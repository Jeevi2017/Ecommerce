package com.example.Ecomm.controller;

import com.example.Ecomm.config.JwtUtil;
import com.example.Ecomm.dto.*;
import com.example.Ecomm.entitiy.RefreshToken;
import com.example.Ecomm.entitiy.User;
import com.example.Ecomm.repository.UserRepository;
import com.example.Ecomm.service.CustomerService;
import com.example.Ecomm.service.RefreshTokenService;
import com.example.Ecomm.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @InjectMocks
    private AuthController authController;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserService userService;

    @Mock
    private CustomerService customerService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserRepository userRepository;

    // ---------- HELPERS ----------

    private User mockUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("password");
        user.setIs2faEnabled(false);
        return user;
    }

    // ---------- TESTS ----------

    @Test
    void registerFullUser() {
        CustomerDTO customerDTO = new CustomerDTO();
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("testuser");
        userDTO.setEmail("test@test.com");
        customerDTO.setUserDetails(userDTO);

        CustomerDTO savedCustomer = new CustomerDTO();
        savedCustomer.setId(1L);
        savedCustomer.setUserDetails(userDTO);

        when(customerService.saveCustomer(any())).thenReturn(savedCustomer);

        ResponseEntity<?> response =
                authController.registerFullUser(customerDTO);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void registerAdmin() {
        AdminRegisterRequest request = new AdminRegisterRequest();
        request.setUsername("admin");
        request.setEmail("admin@test.com");
        request.setPassword("password");

        UserDTO admin = new UserDTO();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setEmail("admin@test.com");

        when(userService.registerAdmin(any())).thenReturn(admin);

        ResponseEntity<?> response =
                authController.registerAdmin(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void loginUser_without2FA() {
        AuthRequestDTO request = new AuthRequestDTO();
        request.setUsername("testuser");
        request.setPassword("password");

        User user = mockUser();

        // ✅ IMPORTANT FIX: UserDetails as principal
        UserDetails userDetails =
                new org.springframework.security.core.userdetails.User(
                        "testuser",
                        "password",
                        List.of()
                );

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        when(authenticationManager.authenticate(any()))
                .thenReturn(authentication);

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(user));

        when(jwtUtil.generateToken(userDetails))
                .thenReturn("jwt-token");

        when(refreshTokenService.createRefreshToken(user))
                .thenReturn(new RefreshToken());

        ResponseEntity<?> response =
                authController.loginUser(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void verifyTwoFactorAuth_invalidCode() {
        TwoFactorAuthRequestDTO request = new TwoFactorAuthRequestDTO();
        request.setUsername("testuser");
        request.setTwoFactorCode("123456");

        when(userService.verify2FACode(any(), any()))
                .thenReturn(false);

        ResponseEntity<?> response =
                authController.verifyTwoFactorAuth(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void refreshToken() {
        RefreshToken refreshToken = new RefreshToken();
        User user = mockUser();
        refreshToken.setUser(user);

        when(refreshTokenService.findByToken("refresh"))
                .thenReturn(Optional.of(refreshToken));

        when(refreshTokenService.verifyExpiration(refreshToken))
                .thenReturn(refreshToken);

        when(userService.loadUserByUsername("testuser"))
                .thenReturn(user);

        when(jwtUtil.generateToken(any()))
                .thenReturn("new-jwt");

        when(refreshTokenService.createRefreshToken(user))
                .thenReturn(new RefreshToken());

        ResponseEntity<?> response =
                authController.refreshToken(Map.of("refreshToken", "refresh"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void welcomeAdmin() {
        ResponseEntity<String> response =
                authController.welcomeAdmin();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Admin"));
    }

    @Test
    void welcomeUser() {
        ResponseEntity<String> response =
                authController.welcomeUser();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("User"));
    }

    @Test
    void welcomeSuperAdmin() {
        ResponseEntity<String> response =
                authController.welcomeSuperAdmin();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Super Admin"));
    }
}
