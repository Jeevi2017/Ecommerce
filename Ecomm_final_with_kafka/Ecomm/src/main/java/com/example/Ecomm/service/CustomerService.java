package com.example.Ecomm.service;

import java.util.List;

import com.example.Ecomm.dto.CustomerDTO;
import com.example.Ecomm.dto.ProfileDTO;
import com.example.Ecomm.dto.UserDTO;

public interface CustomerService {
    CustomerDTO getCustomerById(Long customerId);
    List<CustomerDTO> getAllCustomers();
    CustomerDTO saveCustomer(CustomerDTO customerDto);
    CustomerDTO updateCustomer(Long customerId, CustomerDTO customerDTO);
    void deleteCustomer(Long customerId);
    CustomerDTO getCustomerByEmail(String email);
    UserDTO getCustomerByUsername(String username);
    ProfileDTO getCustomerProfile(Long customerId);
    ProfileDTO createOrUpdateCustomerProfile(Long customerId, ProfileDTO profileDTO);
}
