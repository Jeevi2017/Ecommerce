package com.example.Ecomm.serviceImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Ecomm.config.SecurityConstants;
import com.example.Ecomm.dto.AddressDTO;
import com.example.Ecomm.dto.CustomerDTO;
import com.example.Ecomm.dto.ProfileDTO;
import com.example.Ecomm.dto.ProductDTO;
import com.example.Ecomm.dto.UserDTO;
import com.example.Ecomm.entitiy.Address;
import com.example.Ecomm.entitiy.Cart;
import com.example.Ecomm.entitiy.CartItem;
import com.example.Ecomm.entitiy.Customer;
import com.example.Ecomm.entitiy.Product;
import com.example.Ecomm.entitiy.Profile;
import com.example.Ecomm.entitiy.Role;
import com.example.Ecomm.entitiy.User;
import com.example.Ecomm.exception.CustomerHasActiveOrdersException;
import com.example.Ecomm.exception.ResourceNotFoundException;
import com.example.Ecomm.repository.CartItemRepository;
import com.example.Ecomm.repository.CartRepository;
import com.example.Ecomm.repository.CustomerRepository;
import com.example.Ecomm.repository.OrderRepository;
import com.example.Ecomm.repository.RoleRepository;
import com.example.Ecomm.repository.UserRepository;
import com.example.Ecomm.service.CustomerService;
import com.example.Ecomm.service.ProductService;
import com.example.Ecomm.service.RefreshTokenService;

@Service
public class CustomerServiceImpl implements CustomerService {

    private static final String ENTITY_CUSTOMER = "Customer";

    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductService productService;
    private final OrderRepository orderRepository;

    // ✅ Constructor Injection (SonarQube compliant)
    public CustomerServiceImpl(CustomerRepository customerRepository,
                               RoleRepository roleRepository,
                               PasswordEncoder passwordEncoder,
                               RefreshTokenService refreshTokenService,
                               UserRepository userRepository,
                               CartRepository cartRepository,
                               CartItemRepository cartItemRepository,
                               ProductService productService,
                               OrderRepository orderRepository) {

        this.customerRepository = customerRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productService = productService;
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDTO getCustomerById(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(ENTITY_CUSTOMER, "Id", customerId));
        return mapCustomerToDTO(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerDTO> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(this::mapCustomerToDTO)
                .toList(); // ✅ unmodifiable
    }

    @Override
    @Transactional
    public CustomerDTO saveCustomer(CustomerDTO customerDto) {

        UserDTO userDetails = customerDto.getUserDetails();
        if (userDetails == null) {
            throw new IllegalArgumentException("User details are required for customer registration.");
        }

        if (userRepository.findByUsername(userDetails.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists.");
        }
        if (userRepository.findByEmail(userDetails.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists.");
        }
        if (userDetails.getPhoneNumber() != null &&
                userRepository.findByPhoneNumber(userDetails.getPhoneNumber()).isPresent()) {
            throw new IllegalArgumentException("Phone number already exists.");
        }

        Customer customer = new Customer();
        customer.setUsername(userDetails.getUsername());
        customer.setEmail(userDetails.getEmail());
        customer.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        customer.setPhoneNumber(userDetails.getPhoneNumber());
        customer.setActive(true);

        Role customerRole = roleRepository.findByName(SecurityConstants.ROLE_CUSTOMER)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Role", "name", SecurityConstants.ROLE_CUSTOMER));

        Set<Role> roles = new HashSet<>();
        roles.add(customerRole);
        customer.setRoles(roles);

        Customer savedCustomer = customerRepository.save(customer);

        ProfileDTO profileDetails = customerDto.getProfileDetails();
        if (profileDetails != null) {
            Profile profile = new Profile();
            profile.setFirstName(profileDetails.getFirstName());
            profile.setLastName(profileDetails.getLastName());
            profile.setPhoneNumber(profileDetails.getPhoneNumber());

            savedCustomer.setProfile(profile);

            if (profileDetails.getAddresses() != null) {
                profileDetails.getAddresses()
                        .forEach(a -> profile.addAddress(mapAddressDTOToEntity(a)));
            }
        }

        return mapCustomerToDTO(customerRepository.save(savedCustomer));
    }

    @Override
    @Transactional
    public CustomerDTO updateCustomer(Long customerId, CustomerDTO customerDTO) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(ENTITY_CUSTOMER, "Id", customerId));

        UserDTO userDetails = customerDTO.getUserDetails();
        if (userDetails != null) {
            customer.setEmail(userDetails.getEmail());
            customer.setPhoneNumber(userDetails.getPhoneNumber());
            customer.setActive(userDetails.isActive());
        }

        ProfileDTO profileDTO = customerDTO.getProfileDetails();
        if (profileDTO != null) {
            Profile profile = customer.getProfile() != null
                    ? customer.getProfile()
                    : new Profile();

            profile.setCustomer(customer);
            profile.setFirstName(profileDTO.getFirstName());
            profile.setLastName(profileDTO.getLastName());
            profile.setPhoneNumber(profileDTO.getPhoneNumber());

            profile.setAddresses(new ArrayList<>());
            if (profileDTO.getAddresses() != null) {
                profileDTO.getAddresses()
                        .forEach(a -> profile.addAddress(mapAddressDTOToEntity(a)));
            }

            customer.setProfile(profile);
        }

        return mapCustomerToDTO(customerRepository.save(customer));
    }

    @Override
    @Transactional
    public void deleteCustomer(Long customerId) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(ENTITY_CUSTOMER, "Id", customerId));

        if (orderRepository.countByCustomer_Id(customerId) > 0) {
            throw new CustomerHasActiveOrdersException(
                    "Customer has associated orders. Please resolve them before deletion.");
        }

        refreshTokenService.deleteByUserId(customer.getId());

        Optional<Cart> cartOpt = cartRepository.findByCustomerId(customer.getId());
        if (cartOpt.isPresent()) {
            Cart cart = cartOpt.get();
            for (CartItem item : new ArrayList<>(cart.getCartItems())) {
                Product product = item.getProduct();
                if (product != null && product.getStockQuantity() != null) {
                    product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                    productService.updateProduct(product.getId(), mapProductEntityToDTO(product));
                }
                cartItemRepository.delete(item);
            }
            cartRepository.delete(cart);
        }

        customerRepository.delete(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDTO getCustomerByEmail(String email) {
        return customerRepository.findByEmail(email)
                .map(this::mapCustomerToDTO)
                .orElseThrow(() ->
                        new ResourceNotFoundException(ENTITY_CUSTOMER, "Email", email));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getCustomerByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", "Username", username));
        return mapUserEntityToDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileDTO getCustomerProfile(Long customerId) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(ENTITY_CUSTOMER, "Id", customerId));

        if (customer.getProfile() == null) {
            throw new ResourceNotFoundException("Profile", "Customer ID", customerId);
        }
        return mapProfileToDTO(customer.getProfile());
    }

    @Override
    @Transactional
    public ProfileDTO createOrUpdateCustomerProfile(Long customerId, ProfileDTO profileDTO) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(ENTITY_CUSTOMER, "Id", customerId));

        Profile profile = customer.getProfile() != null
                ? customer.getProfile()
                : new Profile();

        profile.setCustomer(customer);
        profile.setFirstName(profileDTO.getFirstName());
        profile.setLastName(profileDTO.getLastName());
        profile.setPhoneNumber(profileDTO.getPhoneNumber());
        profile.setAddresses(new ArrayList<>());

        if (profileDTO.getAddresses() != null) {
            profileDTO.getAddresses()
                    .forEach(a -> profile.addAddress(mapAddressDTOToEntity(a)));
        }

        customer.setProfile(profile);
        return mapProfileToDTO(customerRepository.save(customer).getProfile());
    }

    // ================= MAPPERS =================

    private CustomerDTO mapCustomerToDTO(Customer customer) {
        CustomerDTO dto = new CustomerDTO();
        dto.setId(customer.getId());
        dto.setUserDetails(mapUserEntityToDTO(customer));
        if (customer.getProfile() != null) {
            dto.setProfileDetails(mapProfileToDTO(customer.getProfile()));
        }
        return dto;
    }

    private UserDTO mapUserEntityToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setActive(user.isActive());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setRoles(user.getRoles().stream().map(Role::getName).toList());
        return dto;
    }

    private ProfileDTO mapProfileToDTO(Profile profile) {
        ProfileDTO dto = new ProfileDTO();
        dto.setId(profile.getId());
        dto.setFirstName(profile.getFirstName());
        dto.setLastName(profile.getLastName());
        dto.setPhoneNumber(profile.getPhoneNumber());
        dto.setCustomerId(profile.getCustomer() != null ? profile.getCustomer().getId() : null);
        dto.setAddresses(profile.getAddresses() != null
                ? profile.getAddresses().stream().map(this::mapAddressToDTO).toList()
                : Collections.emptyList());
        return dto;
    }

    private AddressDTO mapAddressToDTO(Address address) {
        AddressDTO dto = new AddressDTO();
        dto.setId(address.getId());
        dto.setStreet(address.getStreet());
        dto.setCity(address.getCity());
        dto.setState(address.getState());
        dto.setCountry(address.getCountry());
        dto.setPostalCode(address.getPostalCode());
        dto.setType(address.getType());
        dto.setProfileId(address.getProfile() != null ? address.getProfile().getId() : null);
        return dto;
    }

    private Address mapAddressDTOToEntity(AddressDTO dto) {
        Address address = new Address();
        address.setId(dto.getId());
        address.setStreet(dto.getStreet());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setCountry(dto.getCountry());
        address.setPostalCode(dto.getPostalCode());
        address.setType(dto.getType());
        return address;
    }

    private ProductDTO mapProductEntityToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setImages(product.getImages());
        dto.setPrice(product.getPrice());
        dto.setStockQuantity(product.getStockQuantity());
        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getId());
            dto.setCategoryName(product.getCategory().getName());
        }
        return dto;
    }
}
