package com.example.Ecomm.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.Ecomm.config.JwtUtil;
import com.example.Ecomm.config.SecurityConstants;
import com.example.Ecomm.dto.AdminRegisterRequest;
import com.example.Ecomm.dto.AuthRequestDTO;
import com.example.Ecomm.dto.AuthResponseDTO;
import com.example.Ecomm.dto.CustomerDTO;
import com.example.Ecomm.dto.TwoFactorAuthRequestDTO;
import com.example.Ecomm.dto.UserDTO;
import com.example.Ecomm.entitiy.RefreshToken;
import com.example.Ecomm.entitiy.User;
import com.example.Ecomm.repository.UserRepository;
import com.example.Ecomm.service.CustomerService;
import com.example.Ecomm.service.RefreshTokenService;
import com.example.Ecomm.service.UserService;




@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private JwtUtil jwtUtil;

	@Autowired
	private UserService userService;

	@Autowired
	private CustomerService customerService;

	@Autowired
	private RefreshTokenService refreshTokenService;

	@Autowired
	private UserRepository userRepository;

	@PostMapping("/register")
	public ResponseEntity<?> registerFullUser(@Validated @RequestBody CustomerDTO customerDto) {
		try {
			if (customerDto.getUserDetails() != null) {
				customerDto.getUserDetails().setIs2faEnabled(false);
			}
			CustomerDTO registeredCustomer = customerService.saveCustomer(customerDto);

			Map<String, Object> response = new HashMap<>();
			response.put("message", "User registered successfully.");
			response.put("userId", registeredCustomer.getId());
			response.put("username", registeredCustomer.getUserDetails().getUsername());
			response.put("email", registeredCustomer.getUserDetails().getEmail());

			return new ResponseEntity<>(response, HttpStatus.CREATED);
		} catch (DataIntegrityViolationException e) {
			return new ResponseEntity<>("Registration failed: Username or email already exists.", HttpStatus.CONFLICT);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>("Registration failed: " + e.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			System.err.println("Error during user registration: " + e.getMessage());
			return new ResponseEntity<>("Registration failed due to an unexpected error.",
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/register-admin")
	public ResponseEntity<?> registerAdmin(@Validated @RequestBody AdminRegisterRequest adminRegisterRequest) {
		try {
			UserDTO registeredAdmin = userService.registerAdmin(adminRegisterRequest);

			Map<String, Object> response = new HashMap<>();
			response.put("message", "Admin registered successfully.");
			response.put("userId", registeredAdmin.getId());
			response.put("username", registeredAdmin.getUsername());
			response.put("email", registeredAdmin.getEmail());

			return new ResponseEntity<>(response, HttpStatus.CREATED);
		} catch (DataIntegrityViolationException e) {
			return new ResponseEntity<>("Admin registration failed: Username or email already exists.",
					HttpStatus.CONFLICT);
		} catch (UsernameNotFoundException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>("Admin registration failed: " + e.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			System.err.println("Error during admin registration: " + e.getMessage());
			return new ResponseEntity<>("Admin registration failed due to an unexpected error.",
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/login")
	public ResponseEntity<?> loginUser(@Validated @RequestBody AuthRequestDTO authRequestDTO)
			throws UsernameNotFoundException {
		try {
			Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
					authRequestDTO.getUsername(), authRequestDTO.getPassword()));

			User user = userRepository.findByUsername(authentication.getName())
					.orElseThrow(() -> new UsernameNotFoundException("User not found after authentication."));

			if (user.isIs2faEnabled()) {
				userService.generateAndSend2FACode(user.getUsername());
				Map<String, String> response = new HashMap<>();
				response.put("message", "2FA required. A verification code has been sent to your email.");
				response.put("username", user.getUsername());
				return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
			} else {
				SecurityContextHolder.getContext().setAuthentication(authentication);
				UserDetails userDetails = (UserDetails) authentication.getPrincipal();
				String accessToken = jwtUtil.generateToken(userDetails);
				RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
				return new ResponseEntity<>(
						new AuthResponseDTO(accessToken, refreshToken.getToken(), "Login successful!"), HttpStatus.OK);
			}

		} catch (AuthenticationException e) {
			return new ResponseEntity<>("Invalid username or password.", HttpStatus.UNAUTHORIZED);
		} catch (IllegalStateException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			System.err.println("Error during user login: " + e.getMessage());
			return new ResponseEntity<>("An internal error occurred during login.", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/verify-2fa")
	public ResponseEntity<?> verifyTwoFactorAuth(
			@Validated @RequestBody TwoFactorAuthRequestDTO twoFactorAuthRequestDTO) {
		try {
			boolean isCodeValid = userService.verify2FACode(twoFactorAuthRequestDTO.getUsername(),
					twoFactorAuthRequestDTO.getTwoFactorCode());

			if (isCodeValid) {
				User user = userRepository.findByUsername(twoFactorAuthRequestDTO.getUsername())
						.orElseThrow(() -> new UsernameNotFoundException("User not found during 2FA verification."));

				Authentication authentication = new UsernamePasswordAuthenticationToken(user.getUsername(),
						user.getPassword(), user.getAuthorities());
				SecurityContextHolder.getContext().setAuthentication(authentication);

				String accessToken = jwtUtil.generateToken(user);
				RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

				return new ResponseEntity<>(
						new AuthResponseDTO(accessToken, refreshToken.getToken(), "2FA verification successful!"),
						HttpStatus.OK);
			} else {
				return new ResponseEntity<>("Invalid or expired 2FA code.", HttpStatus.UNAUTHORIZED);
			}
		} catch (UsernameNotFoundException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			System.err.println("Error during 2FA verification: " + e.getMessage());
			return new ResponseEntity<>("An internal error occurred during 2FA verification.",
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/refresh-token")
	public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
		String requestRefreshToken = request.get("refreshToken");

		if (requestRefreshToken == null || requestRefreshToken.isEmpty()) {
			return new ResponseEntity<>("Refresh token is missing from request body.", HttpStatus.BAD_REQUEST);
		}

		return refreshTokenService.findByToken(requestRefreshToken).map(refreshTokenService::verifyExpiration)
				.map(refreshToken -> {
					User user = refreshToken.getUser();
					UserDetails userDetails = userService.loadUserByUsername(user.getUsername());
					String newAccessToken = jwtUtil.generateToken(userDetails);

					refreshTokenService.deleteRefreshToken(refreshToken);
					RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

					return new ResponseEntity<>(new AuthResponseDTO(newAccessToken, newRefreshToken.getToken(),
							"Token refreshed successfully!"), HttpStatus.OK);
				}).orElseThrow(() -> new RuntimeException("Refresh token not found or invalid. Please re-login."));
	}

	@GetMapping("/admin/welcome")
	@PreAuthorize("hasAuthority('" + SecurityConstants.ROLE_ADMIN + "')")
	public ResponseEntity<String> welcomeAdmin() {
		return new ResponseEntity<>("Welcome, Admin! You have access to administrative resources.", HttpStatus.OK);
	}

	@GetMapping("/customer/welcome")
	@PreAuthorize("hasAuthority('" + SecurityConstants.ROLE_CUSTOMER + "')")
	public ResponseEntity<String> welcomeUser() {
		return new ResponseEntity<>("Welcome, User! You have access to general user resources.", HttpStatus.OK);
	}

	@GetMapping("/super-admin/welcome")
	@PreAuthorize("hasAuthority('" + SecurityConstants.ROLE_SUPER_ADMIN + "')")
	public ResponseEntity<String> welcomeSuperAdmin() {
		return new ResponseEntity<>("Welcome, Super Admin! You have access to super administrative resources.",
				HttpStatus.OK);
	}
}
