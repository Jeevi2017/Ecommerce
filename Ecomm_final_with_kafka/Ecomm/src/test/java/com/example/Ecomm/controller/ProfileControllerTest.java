package com.example.Ecomm.controller;

import com.example.Ecomm.dto.ProfileDTO;
import com.example.Ecomm.dto.UserDTO;
import com.example.Ecomm.exception.ResourceNotFoundException;
import com.example.Ecomm.service.CustomerService;
import com.example.Ecomm.service.ProfileService;
import com.example.Ecomm.service.UserService;
import org.junit.jupiter.api.AfterEach;
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
class ProfileControllerTest {

    @InjectMocks
    private ProfileController profileController;

    @Mock
    private ProfileService profileService;

    @Mock
    private CustomerService customerService;

    @Mock
    private UserService userService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ---------- HELPERS ----------

    private void mockAuthenticatedUser(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser", null)
        );

        UserDTO userDTO = new UserDTO();
        userDTO.setId(userId);
        userDTO.setUsername("testuser");

        // ✅ FIX: lenient to avoid UnnecessaryStubbing
        lenient()
                .when(userService.getUserByUserName("testuser"))
                .thenReturn(userDTO);
    }

    private ProfileDTO mockProfileDTO() {
        ProfileDTO dto = new ProfileDTO();
        dto.setId(10L);
        dto.setCustomerId(1L);
        return dto;
    }

    // ---------- TESTS ----------

    @Test
    void getAuthenticatedCustomerId() {
        mockAuthenticatedUser(1L);

        Long id = profileController.getAuthenticatedCustomerId();

        assertEquals(1L, id);
    }

    @Test
    void getCustomerIdByProfileId() {
        when(profileService.getProfileById(10L))
                .thenReturn(mockProfileDTO());

        Long customerId =
                profileController.getCustomerIdByProfileId(10L);

        assertEquals(1L, customerId);
    }

    @Test
    void getCustomerIdByProfileId_notFound() {
        when(profileService.getProfileById(99L))
                .thenReturn(null);

        assertThrows(
                ResourceNotFoundException.class,
                () -> profileController.getCustomerIdByProfileId(99L)
        );
    }

    @Test
    void saveProfile() {
        mockAuthenticatedUser(1L);

        when(profileService.saveProfile(any()))
                .thenReturn(mockProfileDTO());

        ResponseEntity<ProfileDTO> response =
                profileController.saveProfile(mockProfileDTO());

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void getAllProfiles() {
        when(profileService.getAllProfiles())
                .thenReturn(List.of(mockProfileDTO()));

        ResponseEntity<List<ProfileDTO>> response =
                profileController.getAllProfiles();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getProfileById() {
        mockAuthenticatedUser(1L);

        when(profileService.getProfileById(10L))
                .thenReturn(mockProfileDTO());

        ResponseEntity<ProfileDTO> response =
                profileController.getProfileById(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(10L, response.getBody().getId());
    }

    @Test
    void updateProfile() {
        mockAuthenticatedUser(1L);

        when(profileService.getProfileById(10L))
                .thenReturn(mockProfileDTO());

        when(profileService.updateProfile(eq(10L), any()))
                .thenReturn(mockProfileDTO());

        ResponseEntity<ProfileDTO> response =
                profileController.updateProfile(10L, mockProfileDTO());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void deleteProfile() {
        doNothing().when(profileService).deleteProfile(10L);

        ResponseEntity<Void> response =
                profileController.deleteProfile(10L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
