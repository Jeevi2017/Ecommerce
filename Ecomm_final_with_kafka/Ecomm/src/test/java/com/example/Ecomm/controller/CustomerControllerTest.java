package com.example.Ecomm.controller;

import com.example.Ecomm.dto.CustomerDTO;
import com.example.Ecomm.dto.ProfileDTO;
import com.example.Ecomm.dto.UserDTO;
import com.example.Ecomm.exception.CustomerHasActiveOrdersException;
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
class CustomerControllerTest {

    @InjectMocks
    private CustomerController customerController;

    @Mock
    private CustomerService customerService;

    @Mock
    private ProfileService profileService;

    @Mock
    private UserService userService;

    @BeforeEach
    void setupSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser", null)
        );

        UserDTO userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setUsername("testuser");

        // ✅ FIX: lenient stubbing
        lenient().when(userService.getUserByUserName("testuser"))
                .thenReturn(userDTO);
    }

    // ---------- HELPERS ----------

    private CustomerDTO mockCustomerDTO() {
        CustomerDTO dto = new CustomerDTO();
        dto.setId(1L);
        return dto;
    }

    private ProfileDTO mockProfileDTO() {
        ProfileDTO profile = new ProfileDTO();
        profile.setId(10L);
        profile.setCustomerId(1L);
        return profile;
    }

    // ---------- TESTS ----------

    @Test
    void getAuthenticatedCustomerId() {
        Long id = customerController.getAuthenticatedCustomerId();
        assertEquals(1L, id);
    }

    @Test
    void getCustomerByProfileId() {
        when(profileService.getProfileById(10L))
                .thenReturn(mockProfileDTO());

        Long customerId = customerController.getCustomerByProfileId(10L);

        assertEquals(1L, customerId);
    }

    @Test
    void getAllCustomers() {
        when(customerService.getAllCustomers())
                .thenReturn(List.of(mockCustomerDTO()));

        ResponseEntity<List<CustomerDTO>> response =
                customerController.getAllCustomers();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void registerCustomer() {
        when(customerService.saveCustomer(any()))
                .thenReturn(mockCustomerDTO());

        ResponseEntity<CustomerDTO> response =
                customerController.registerCustomer(mockCustomerDTO());

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void getCustomerById() {
        when(customerService.getCustomerById(1L))
                .thenReturn(mockCustomerDTO());

        ResponseEntity<CustomerDTO> response =
                customerController.getCustomerById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getCustomerByUsername() {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("testuser");

        when(customerService.getCustomerByUsername("testuser"))
                .thenReturn(userDTO);

        ResponseEntity<UserDTO> response =
                customerController.getCustomerByUsername("testuser");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateCustomer() {
        when(customerService.updateCustomer(eq(1L), any()))
                .thenReturn(mockCustomerDTO());

        ResponseEntity<CustomerDTO> response =
                customerController.updateCustomer(1L, mockCustomerDTO());

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void deleteCustomer() {
        doNothing().when(customerService).deleteCustomer(1L);

        ResponseEntity<Void> response =
                customerController.deleteCustomer(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void getCustomerProfile() {
        when(customerService.getCustomerProfile(1L))
                .thenReturn(mockProfileDTO());

        ResponseEntity<ProfileDTO> response =
                customerController.getCustomerProfile(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void createOrUpdateCustomerProfile() {
        when(customerService.createOrUpdateCustomerProfile(eq(1L), any()))
                .thenReturn(mockProfileDTO());

        ResponseEntity<ProfileDTO> response =
                customerController.createOrUpdateCustomerProfile(
                        1L, mockProfileDTO()
                );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void handleCustomerHasActiveOrdersException() {
        CustomerHasActiveOrdersException ex =
                new CustomerHasActiveOrdersException("Active orders exist");

        ResponseEntity<String> response =
                customerController.handleCustomerHasActiveOrdersException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }
}
