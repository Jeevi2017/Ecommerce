package com.example.Ecomm.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
import com.example.Ecomm.dto.AddressDTO;
import com.example.Ecomm.dto.ProfileDTO;
import com.example.Ecomm.dto.UserDTO;
import com.example.Ecomm.exception.ResourceNotFoundException;
import com.example.Ecomm.service.AddressService;
import com.example.Ecomm.service.CustomerService;
import com.example.Ecomm.service.ProfileService;
import com.example.Ecomm.service.UserService;

@RestController
@RequestMapping("/api/addresses")
@CrossOrigin(origins = "http://localhost:4200")
public class AddressController {

    private final AddressService addressService;
    private final ProfileService profileService;
    private final CustomerService customerService;
    private final UserService userService;

    // ✅ Constructor Injection (SonarQube compliant)
    public AddressController(AddressService addressService,
                             ProfileService profileService,
                             CustomerService customerService,
                             UserService userService) {
        this.addressService = addressService;
        this.profileService = profileService;
        this.customerService = customerService;
        this.userService = userService;
    }

    public Long getAuthenticatedCustomerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authenticatedUsername = authentication.getName();
        UserDTO userDTO = userService.getUserByUserName(authenticatedUsername);
        return userDTO.getId();
    }

    public Long getCustomerIdByAddressId(Long addressId) {
        AddressDTO addressDTO = addressService.getAddressById(addressId);
        if (addressDTO != null && addressDTO.getProfileId() != null) {
            ProfileDTO profileDTO = profileService.getProfileById(addressDTO.getProfileId());
            if (profileDTO != null && profileDTO.getCustomerId() != null) {
                return profileDTO.getCustomerId();
            }
        }
        throw new ResourceNotFoundException("Address or associated Profile", "Id", addressId);
    }

    public Long getProfileIdForAuthenticatedCustomer() {
        Long authenticatedCustomerId = getAuthenticatedCustomerId();
        ProfileDTO profileDTO = customerService.getCustomerProfile(authenticatedCustomerId);
        if (profileDTO != null) {
            return profileDTO.getId();
        }
        throw new ResourceNotFoundException("Profile", "Customer ID", authenticatedCustomerId);
    }

    @PostMapping
    @PreAuthorize(
            "hasAuthority('" + SecurityConstants.ROLE_ADMIN + "') or " +
                    "#addressDTO.profileId == @addressController.getProfileIdForAuthenticatedCustomer()"
    )
    public ResponseEntity<AddressDTO> createAddress(
            @Validated @RequestBody AddressDTO addressDTO) {

        AddressDTO savedAddress = addressService.saveAddress(addressDTO);
        return new ResponseEntity<>(savedAddress, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('" + SecurityConstants.ROLE_ADMIN + "') or " +
                    "@addressController.getAuthenticatedCustomerId() == " +
                    "@addressController.getCustomerIdByAddressId(#id)"
    )
    public ResponseEntity<AddressDTO> getAddressById(@PathVariable Long id) {
        AddressDTO address = addressService.getAddressById(id);
        return new ResponseEntity<>(address, HttpStatus.OK);
    }

    @GetMapping("/profile/{profileId}")
    @PreAuthorize(
            "hasAuthority('" + SecurityConstants.ROLE_ADMIN + "') or " +
                    "#profileId == @addressController.getProfileIdForAuthenticatedCustomer()"
    )
    public ResponseEntity<List<AddressDTO>> getAddressesByProfileId(
            @PathVariable Long profileId) {

        List<AddressDTO> addresses = addressService.getAddressesByProfileId(profileId);
        return new ResponseEntity<>(addresses, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('" + SecurityConstants.ROLE_ADMIN + "') or " +
                    "@addressController.getAuthenticatedCustomerId() == " +
                    "@addressController.getCustomerIdByAddressId(#id)"
    )
    public ResponseEntity<Void> deleteAddress(@PathVariable Long id) {
        addressService.deleteAddress(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('" + SecurityConstants.ROLE_ADMIN + "') or " +
                    "@addressController.getAuthenticatedCustomerId() == " +
                    "@addressController.getCustomerIdByAddressId(#id)"
    )
    public ResponseEntity<AddressDTO> updateAddress(
            @PathVariable Long id,
            @Validated @RequestBody AddressDTO addressDTO) {

        Long existingProfileId = addressService.getAddressById(id).getProfileId();
        if (addressDTO.getProfileId() != null &&
                !addressDTO.getProfileId().equals(existingProfileId)) {
            throw new AccessDeniedException(
                    "Cannot change the profile association of an existing address."
            );
        }

        AddressDTO updatedAddress = addressService.updateAddress(id, addressDTO);
        return new ResponseEntity<>(updatedAddress, HttpStatus.OK);
    }
}
