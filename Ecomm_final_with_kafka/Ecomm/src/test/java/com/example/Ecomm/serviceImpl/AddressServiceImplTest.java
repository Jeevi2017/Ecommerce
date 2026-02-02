package com.example.Ecomm.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.example.Ecomm.dto.AddressDTO;
import com.example.Ecomm.entitiy.Address;
import com.example.Ecomm.entitiy.Profile;
import com.example.Ecomm.exception.ResourceNotFoundException;
import com.example.Ecomm.repository.AddressRepository;
import com.example.Ecomm.repository.ProfileRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class AddressServiceImplTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private ProfileRepository profileRepository;

    @InjectMocks
    private AddressServiceImpl addressService;

    private Profile profile;
    private Address address;
    private AddressDTO addressDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        profile = new Profile();
        profile.setId(1L);

        address = new Address();
        address.setId(1L);
        address.setProfile(profile);
        address.setCity("New York");
        address.setStreet("123 Main St");

        addressDTO = new AddressDTO();
        addressDTO.setId(1L);
        addressDTO.setProfileId(1L);
        addressDTO.setCity("New York");
        addressDTO.setStreet("123 Main St");
    }

    @Test
    void testSaveAddress() {
        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(addressRepository.save(any(Address.class))).thenReturn(address);

        AddressDTO saved = addressService.saveAddress(addressDTO);

        assertNotNull(saved);
        assertEquals(addressDTO.getCity(), saved.getCity());
        verify(addressRepository, times(1)).save(any(Address.class));
    }

    @Test
    void testGetAddressById() {
        when(addressRepository.findById(1L)).thenReturn(Optional.of(address));

        AddressDTO result = addressService.getAddressById(1L);

        assertNotNull(result);
        assertEquals(address.getCity(), result.getCity());
    }

    @Test
    void testGetAddressesByProfileId() {
        List<Address> addresses = new ArrayList<>();
        addresses.add(address);
        when(addressRepository.findByProfileId(1L)).thenReturn(addresses);

        List<AddressDTO> result = addressService.getAddressesByProfileId(1L);

        assertEquals(1, result.size());
        assertEquals(address.getCity(), result.get(0).getCity());
    }

    @Test
    void testDeleteAddress() {
        when(addressRepository.existsById(1L)).thenReturn(true);
        doNothing().when(addressRepository).deleteById(1L);

        assertDoesNotThrow(() -> addressService.deleteAddress(1L));
        verify(addressRepository, times(1)).deleteById(1L);
    }

    @Test
    void testUpdateAddress() {
        when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(addressRepository.save(any(Address.class))).thenReturn(address);

        AddressDTO updated = addressService.updateAddress(1L, addressDTO);

        assertNotNull(updated);
        assertEquals(addressDTO.getCity(), updated.getCity());
        verify(addressRepository, times(1)).save(any(Address.class));
    }
}
