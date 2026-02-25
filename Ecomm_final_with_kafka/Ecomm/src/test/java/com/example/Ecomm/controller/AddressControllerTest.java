package com.example.Ecomm.controller;

import com.example.Ecomm.dto.AddressDTO;
import com.example.Ecomm.dto.ProfileDTO;
import com.example.Ecomm.dto.UserDTO;
import com.example.Ecomm.service.AddressService;
import com.example.Ecomm.service.CustomerService;
import com.example.Ecomm.service.ProfileService;
import com.example.Ecomm.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressControllerTest {

    @InjectMocks
    private AddressController addressController;

    @Mock
    private AddressService addressService;

    @Mock
    private ProfileService profileService;

    @Mock
    private CustomerService customerService;

    @Mock
    private UserService userService;

    @BeforeEach
    void setupSecurityContext() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("testuser", null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        UserDTO user = new UserDTO();
        user.setId(1L);
        user.setUsername("testuser");

        // ✅ IMPORTANT FIX: lenient stub
        lenient().when(userService.getUserByUserName("testuser"))
                .thenReturn(user);
    }

    private AddressDTO mockAddressDTO() {
        AddressDTO dto = new AddressDTO();
        dto.setId(1L);
        dto.setStreet("Street");
        dto.setCity("City");
        dto.setState("State");
        dto.setPostalCode("123456");
        dto.setProfileId(10L);
        return dto;
    }

    @Test
    void getAuthenticatedCustomerId() {
        Long id = addressController.getAuthenticatedCustomerId();
        assertEquals(1L, id);
    }

    @Test
    void getCustomerIdByAddressId() {
        ProfileDTO profile = new ProfileDTO();
        profile.setId(10L);
        profile.setCustomerId(1L);

        when(addressService.getAddressById(1L)).thenReturn(mockAddressDTO());
        when(profileService.getProfileById(10L)).thenReturn(profile);

        Long customerId = addressController.getCustomerIdByAddressId(1L);
        assertEquals(1L, customerId);
    }

    @Test
    void getProfileIdForAuthenticatedCustomer() {
        ProfileDTO profile = new ProfileDTO();
        profile.setId(10L);
        profile.setCustomerId(1L);

        when(customerService.getCustomerProfile(1L)).thenReturn(profile);

        Long profileId = addressController.getProfileIdForAuthenticatedCustomer();
        assertEquals(10L, profileId);
    }

    @Test
    void createAddress() {
        when(addressService.saveAddress(any())).thenReturn(mockAddressDTO());

        ResponseEntity<AddressDTO> response =
                addressController.createAddress(mockAddressDTO());

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void getAddressById() {
        when(addressService.getAddressById(1L)).thenReturn(mockAddressDTO());

        ResponseEntity<AddressDTO> response =
                addressController.getAddressById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().getId());
    }

    @Test
    void getAddressesByProfileId() {
        when(addressService.getAddressesByProfileId(10L))
                .thenReturn(List.of(mockAddressDTO()));

        ResponseEntity<List<AddressDTO>> response =
                addressController.getAddressesByProfileId(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void deleteAddress() {
        doNothing().when(addressService).deleteAddress(1L);

        ResponseEntity<Void> response =
                addressController.deleteAddress(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void updateAddress() {
        when(addressService.getAddressById(1L)).thenReturn(mockAddressDTO());
        when(addressService.updateAddress(eq(1L), any()))
                .thenReturn(mockAddressDTO());

        ResponseEntity<AddressDTO> response =
                addressController.updateAddress(1L, mockAddressDTO());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
