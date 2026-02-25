package com.example.Ecomm.serviceImpl;

import com.example.Ecomm.dto.AddressDTO;
import com.example.Ecomm.dto.ProfileDTO;
import com.example.Ecomm.entitiy.Address;
import com.example.Ecomm.entitiy.Customer;
import com.example.Ecomm.entitiy.Profile;
import com.example.Ecomm.repository.CustomerRepository;
import com.example.Ecomm.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    @InjectMocks
    private ProfileServiceImpl profileService;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private CustomerRepository customerRepository;

    // ---------- HELPERS ----------

    private Customer mockCustomer() {
        Customer c = new Customer();
        c.setId(1L);
        return c;
    }

    private Profile mockProfile(Customer customer) {
        Profile p = new Profile();
        p.setId(1L);
        p.setFirstName("John");
        p.setLastName("Doe");
        p.setPhoneNumber("9999999999");
        p.setCustomer(customer);
        return p;
    }

    private ProfileDTO mockProfileDTO() {
        ProfileDTO dto = new ProfileDTO();
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setPhoneNumber("9999999999");
        dto.setCustomerId(1L);

        AddressDTO addressDTO = new AddressDTO();
        addressDTO.setStreet("Street");
        addressDTO.setCity("City");
        addressDTO.setState("State");
        addressDTO.setCountry("Country");
        addressDTO.setPostalCode("123456");
        addressDTO.setType("HOME");

        dto.setAddresses(List.of(addressDTO));
        return dto;
    }

    // ---------- TESTS ----------

    @Test
    void saveProfile() {
        Customer customer = mockCustomer();
        Profile profile = mockProfile(customer);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(profileRepository.save(any(Profile.class))).thenReturn(profile);

        ProfileDTO result = profileService.saveProfile(mockProfileDTO());

        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        verify(profileRepository).save(any(Profile.class));
    }

    @Test
    void getAllProfiles() {
        Customer customer = mockCustomer();
        Profile profile = mockProfile(customer);

        when(profileRepository.findAll()).thenReturn(List.of(profile));

        List<ProfileDTO> result = profileService.getAllProfiles();

        assertEquals(1, result.size());
        assertEquals("John", result.get(0).getFirstName());
    }

    @Test
    void getProfileById() {
        Customer customer = mockCustomer();
        Profile profile = mockProfile(customer);

        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));

        ProfileDTO result = profileService.getProfileById(1L);

        assertNotNull(result);
        assertEquals("John", result.getFirstName());
    }

    @Test
    void updateProfile() {
        Customer customer = mockCustomer();
        Profile profile = mockProfile(customer);

        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(Profile.class))).thenReturn(profile);

        ProfileDTO updated = mockProfileDTO();
        updated.setFirstName("Jane");

        ProfileDTO result = profileService.updateProfile(1L, updated);

        assertNotNull(result);
        assertEquals("Jane", result.getFirstName());
    }

    @Test
    void deleteProfile() {
        Customer customer = mockCustomer();
        Profile profile = mockProfile(customer);

        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));

        assertDoesNotThrow(() -> profileService.deleteProfile(1L));
        verify(profileRepository).delete(profile);
    }
}
