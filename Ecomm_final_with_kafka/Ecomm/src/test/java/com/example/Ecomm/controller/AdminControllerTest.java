package com.example.Ecomm.controller;

import com.example.Ecomm.dto.UserDTO;
import com.example.Ecomm.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @InjectMocks
    private AdminController adminController;

    @Mock
    private UserService userService;

    private UserDTO mockUserDTO() {
        UserDTO dto = new UserDTO();
        dto.setId(1L);
        dto.setUsername("testuser");
        dto.setEmail("test@test.com");
        dto.setActive(true);
        return dto;
    }

    // ---------- TESTS ----------

    @Test
    void getAllUsers() {
        when(userService.getAllUsers()).thenReturn(List.of(mockUserDTO()));

        ResponseEntity<List<UserDTO>> response =
                adminController.getAllUsers();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getUserById() {
        when(userService.getUserById(1L)).thenReturn(mockUserDTO());

        ResponseEntity<UserDTO> response =
                adminController.getUserById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().getId());
    }

    @Test
    void updateUser() {
        when(userService.updateUser(eq(1L), any()))
                .thenReturn(mockUserDTO());

        ResponseEntity<UserDTO> response =
                adminController.updateUser(1L, mockUserDTO());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void deleteUser() {
        doNothing().when(userService).deleteUser(1L);

        ResponseEntity<Void> response =
                adminController.deleteUser(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(userService).deleteUser(1L);
    }

    @Test
    void updateUserRoles() {
        when(userService.updateUserRoles(eq(1L), any()))
                .thenReturn(mockUserDTO());

        Map<String, List<String>> request =
                Map.of("roles", List.of("ROLE_ADMIN"));

        ResponseEntity<UserDTO> response =
                adminController.updateUserRoles(1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
