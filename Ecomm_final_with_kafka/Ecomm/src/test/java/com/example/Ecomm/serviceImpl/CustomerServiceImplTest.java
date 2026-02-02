package com.example.Ecomm.serviceImpl;

import com.example.Ecomm.dto.CustomerDTO;
import com.example.Ecomm.dto.ProfileDTO;
import com.example.Ecomm.dto.UserDTO;
import com.example.Ecomm.entitiy.Customer;
import com.example.Ecomm.entitiy.Profile;
import com.example.Ecomm.entitiy.Role;
import com.example.Ecomm.entitiy.User;
import com.example.Ecomm.repository.*;
import com.example.Ecomm.service.ProductService;
import com.example.Ecomm.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomerServiceImplTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UserRepository userRepository;
    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductService productService;
    @Mock private OrderRepository orderRepository;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private Customer customer;
    private CustomerDTO customerDTO;
    private UserDTO userDTO;
    private Profile profile;
    private ProfileDTO profileDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        Role role = new Role();
        role.setName("ROLE_CUSTOMER");

        userDTO = new UserDTO();
        userDTO.setUsername("testuser");
        userDTO.setEmail("test@test.com");
        userDTO.setPassword("password");
        userDTO.setActive(true);

        profileDTO = new ProfileDTO();
        profileDTO.setFirstName("John");
        profileDTO.setLastName("Doe");
        profileDTO.setPhoneNumber("1234567890");

        customerDTO = new CustomerDTO();
        customerDTO.setUserDetails(userDTO);
        customerDTO.setProfileDetails(profileDTO);

        profile = new Profile();
        profile.setFirstName("John");

        customer = new Customer();
        customer.setId(1L);
        customer.setUsername("testuser");
        customer.setEmail("test@test.com");
        customer.setActive(true);
        customer.setProfile(profile);
        customer.setRoles(Set.of(role));
    }

    @Test
    void testSaveCustomer() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(roleRepository.findByName(anyString())).thenReturn(Optional.of(new Role()));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        CustomerDTO result = customerService.saveCustomer(customerDTO);

        assertNotNull(result);
        assertEquals("test@test.com", result.getUserDetails().getEmail());
    }

    @Test
    void testGetCustomerById() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        CustomerDTO result = customerService.getCustomerById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void testGetAllCustomers() {
        when(customerRepository.findAll()).thenReturn(List.of(customer));

        List<CustomerDTO> result = customerService.getAllCustomers();

        assertEquals(1, result.size());
    }

    @Test
    void testGetCustomerByEmail() {
        when(customerRepository.findByEmail("test@test.com")).thenReturn(Optional.of(customer));

        CustomerDTO result = customerService.getCustomerByEmail("test@test.com");

        assertNotNull(result);
    }

    @Test
    void testGetCustomerByUsername() {
        User user = new User();
        user.setUsername("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        assertNotNull(customerService.getCustomerByUsername("testuser"));
    }

    @Test
    void testGetCustomerProfile() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        ProfileDTO result = customerService.getCustomerProfile(1L);

        assertNotNull(result);
        assertEquals("John", result.getFirstName());
    }

    @Test
    void testCreateOrUpdateCustomerProfile() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        ProfileDTO result = customerService.createOrUpdateCustomerProfile(1L, profileDTO);

        assertNotNull(result);
        assertEquals("John", result.getFirstName());
    }

    @Test
    void testUpdateCustomer() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        CustomerDTO result = customerService.updateCustomer(1L, customerDTO);

        assertNotNull(result);
    }

    @Test
    void testDeleteCustomer() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(orderRepository.countByCustomer_Id(1L)).thenReturn(0L);

        assertDoesNotThrow(() -> customerService.deleteCustomer(1L));
        verify(customerRepository).delete(customer);
    }
}
