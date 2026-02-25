package com.example.Ecomm.serviceImpl;

import com.example.Ecomm.config.SecurityConstants;
import com.example.Ecomm.dto.AdminRegisterRequest;
import com.example.Ecomm.dto.UserDTO;
import com.example.Ecomm.entitiy.Role;
import com.example.Ecomm.entitiy.User;
import com.example.Ecomm.repository.RoleRepository;
import com.example.Ecomm.repository.UserRepository;
import com.example.Ecomm.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;

    // ---------- HELPERS ----------

    private User mockUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setEmail("admin@test.com");
        user.setActive(true);
        user.setIs2faEnabled(false);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    private Role mockAdminRole() {
        Role role = new Role();
        role.setName(SecurityConstants.ROLE_ADMIN);
        return role;
    }

    // ---------- TESTS ----------

    @Test
    void loadUserByUsername() {
        User user = mockUser();
        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(user));

        assertNotNull(userService.loadUserByUsername("admin"));
    }

    @Test
    void registerAdmin() {
        AdminRegisterRequest req = new AdminRegisterRequest();
        req.setUsername("admin");
        req.setEmail("admin@test.com");
        req.setPassword("password");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(roleRepository.findByName(SecurityConstants.ROLE_ADMIN))
                .thenReturn(Optional.of(mockAdminRole()));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UserDTO dto = userService.registerAdmin(req);

        assertNotNull(dto);
        assertEquals("admin", dto.getUsername());
    }

    @Test
    void getAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(mockUser()));

        List<UserDTO> users = userService.getAllUsers();
        assertEquals(1, users.size());
    }

    @Test
    void getUserById() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser()));

        UserDTO dto = userService.getUserById(1L);
        assertEquals("admin", dto.getUsername());
    }

    @Test
    void updateUser() {
        User user = mockUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        UserDTO update = new UserDTO();
        update.setEmail("new@test.com");
        update.setPhoneNumber("9999999999");
        update.setActive(true);

        UserDTO result = userService.updateUser(1L, update);
        assertNotNull(result);
    }

    @Test
    void deleteUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser()));
        assertDoesNotThrow(() -> userService.deleteUser(1L));
    }

    @Test
    void getUserByUserName() {
        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(mockUser()));

        UserDTO dto = userService.getUserByUserName("admin");
        assertEquals("admin", dto.getUsername());
    }

    @Test
    void updateUserRoles() {
        User user = mockUser();
        Role role = mockAdminRole();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName(SecurityConstants.ROLE_ADMIN))
                .thenReturn(Optional.of(role));
        when(userRepository.save(any())).thenReturn(user);

        UserDTO dto = userService.updateUserRoles(
                1L,
                List.of(SecurityConstants.ROLE_ADMIN)
        );

        assertNotNull(dto);
    }

    @Test
    void enable2FA() {
        User user = mockUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        UserDTO dto = userService.enable2FA(1L);
        assertTrue(dto.getIs2faEnabled());
    }

    @Test
    void disable2FA() {
        User user = mockUser();
        user.setIs2faEnabled(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        UserDTO dto = userService.disable2FA(1L);
        assertFalse(dto.getIs2faEnabled());
    }

    @Test
    void generateAndSend2FACode() {
        User user = mockUser();
        user.setIs2faEnabled(true);

        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        String code = userService.generateAndSend2FACode("admin");

        assertNotNull(code);
        verify(emailService).send2faCode(any(), any(), anyInt());
    }

    @Test
    void verify2FACode() {
        User user = mockUser();
        user.setIs2faEnabled(true);
        user.setTwoFactorCode("123456");
        user.setTwoFactorCodeExpiry(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        boolean result = userService.verify2FACode("admin", "123456");
        assertTrue(result);
    }
}

