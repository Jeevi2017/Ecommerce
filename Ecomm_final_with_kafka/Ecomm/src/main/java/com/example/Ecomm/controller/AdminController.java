package com.example.Ecomm.controller;

import com.example.Ecomm.config.SecurityConstants; 

import com.example.Ecomm.dto.UserDTO; 
import com.example.Ecomm.service.UserService; 
import com.example.Ecomm.exception.ResourceNotFoundException; 

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map; 

@RestController
@RequestMapping("/api/admin/users") 
@CrossOrigin(origins = "http://localhost:4200")
public class AdminController {

    @Autowired
    private UserService userService; 

   
    @GetMapping
    @PreAuthorize("hasAuthority('" + SecurityConstants.ROLE_ADMIN + "')")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + SecurityConstants.ROLE_ADMIN + "')")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO user = userService.getUserById(id);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

  
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + SecurityConstants.ROLE_ADMIN + "')")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @Validated @RequestBody UserDTO userDTO) {
        UserDTO updatedUser = userService.updateUser(id, userDTO);
        return new ResponseEntity<>(updatedUser, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + SecurityConstants.ROLE_ADMIN + "')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/{userId}/role")
    @PreAuthorize("hasAuthority('" + SecurityConstants.ROLE_SUPER_ADMIN + "')") 
    public ResponseEntity<UserDTO> updateUserRoles(@PathVariable Long userId, @RequestBody Map<String, List<String>> requestBody) {
        List<String> newRoleNames = requestBody.get("roles");
        if (newRoleNames == null || newRoleNames.isEmpty()) {
            throw new IllegalArgumentException("New roles list cannot be empty.");
        }
        UserDTO updatedUser = userService.updateUserRoles(userId, newRoleNames);
        return new ResponseEntity<>(updatedUser, HttpStatus.OK);
    }
}
