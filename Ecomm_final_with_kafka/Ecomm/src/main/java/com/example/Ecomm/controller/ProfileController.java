package com.example.Ecomm.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Ecomm.config.SecurityConstants;
import com.example.Ecomm.dto.CustomerDTO;
import com.example.Ecomm.dto.ProfileDTO;
import com.example.Ecomm.dto.UserDTO;
import com.example.Ecomm.exception.ResourceNotFoundException;
import com.example.Ecomm.service.CustomerService;
import com.example.Ecomm.service.ProfileService;
import com.example.Ecomm.service.UserService;

@RestController
@RequestMapping("/api/profiles")
@CrossOrigin(origins = "http://localhost:4200")
public class ProfileController {

	@Autowired
	private ProfileService profileService;

	@Autowired
	private CustomerService customerService;

	@Autowired
	private UserService userService;

	public Long getAuthenticatedCustomerId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String authenticatedUsername = authentication.getName();
		UserDTO userDTO = userService.getUserByUserName(authenticatedUsername);
		return userDTO.getId();
	}

	public Long getCustomerIdByProfileId(Long profileId) {
		ProfileDTO profileDTO = profileService.getProfileById(profileId);
		if (profileDTO != null && profileDTO.getCustomerId() != null) {
			return profileDTO.getCustomerId();
		}
		throw new ResourceNotFoundException("Profile", "Id", profileId);
	}

	@PostMapping
	@PreAuthorize("hasAuthority('" + SecurityConstants.ROLE_ADMIN
			+ "') or #profileDTO.customerId == @profileController.getAuthenticatedCustomerId()")
	public ResponseEntity<ProfileDTO> saveProfile(@Validated @RequestBody ProfileDTO profileDTO) {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		boolean isAdminOrSuperAdmin = authentication.getAuthorities()
				.contains(new SimpleGrantedAuthority(SecurityConstants.ROLE_ADMIN))
				|| authentication.getAuthorities()
						.contains(new SimpleGrantedAuthority(SecurityConstants.ROLE_SUPER_ADMIN));

		if (!isAdminOrSuperAdmin) {
			if (profileDTO.getCustomerId() == null
					|| !profileDTO.getCustomerId().equals(getAuthenticatedCustomerId())) {
				throw new org.springframework.security.access.AccessDeniedException(
						"Customers can only create profiles for their own account.");
			}
		}
		ProfileDTO savedProfile = profileService.saveProfile(profileDTO);
		return new ResponseEntity<>(savedProfile, HttpStatus.CREATED);
	}

	@GetMapping
	@PreAuthorize("hasAuthority('" + SecurityConstants.ROLE_ADMIN + "')")
	public ResponseEntity<List<ProfileDTO>> getAllProfiles() {
		List<ProfileDTO> profiles = profileService.getAllProfiles();
		return new ResponseEntity<>(profiles, HttpStatus.OK);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('" + SecurityConstants.ROLE_ADMIN
			+ "') or @profileController.getAuthenticatedCustomerId() == @profileController.getCustomerIdByProfileId(#id)")
	public ResponseEntity<ProfileDTO> getProfileById(@PathVariable Long id) {
		ProfileDTO profile = profileService.getProfileById(id);
		return new ResponseEntity<>(profile, HttpStatus.OK);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('" + SecurityConstants.ROLE_ADMIN
			+ "') or @profileController.getAuthenticatedCustomerId() == @profileController.getCustomerIdByProfileId(#id)")
	public ResponseEntity<ProfileDTO> updateProfile(@PathVariable Long id,
			@Validated @RequestBody ProfileDTO profileDTO) {

		if (profileDTO.getCustomerId() != null && !profileDTO.getCustomerId().equals(getCustomerIdByProfileId(id))) {
			throw new org.springframework.security.access.AccessDeniedException(
					"Cannot change customer ID of an existing profile.");
		}

		ProfileDTO updatedProfile = profileService.updateProfile(id, profileDTO);
		return new ResponseEntity<>(updatedProfile, HttpStatus.OK);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('" + SecurityConstants.ROLE_ADMIN + "')")
	public ResponseEntity<Void> deleteProfile(@PathVariable Long id) {
		profileService.deleteProfile(id);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}
}
