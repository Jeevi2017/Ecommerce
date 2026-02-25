package com.example.Ecomm.serviceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Ecomm.dto.AddressDTO;
import com.example.Ecomm.dto.ProfileDTO;
import com.example.Ecomm.entitiy.Address;
import com.example.Ecomm.entitiy.Customer;
import com.example.Ecomm.entitiy.Profile;
import com.example.Ecomm.exception.ResourceNotFoundException;
import com.example.Ecomm.repository.CustomerRepository;
import com.example.Ecomm.repository.ProfileRepository;
import com.example.Ecomm.service.ProfileService;

@Service
public class ProfileServiceImpl implements ProfileService {

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private CustomerRepository customerRepository;

    // ================= SAVE =================

    @Override
    @Transactional
    public ProfileDTO saveProfile(ProfileDTO profileDTO) {

        if (profileDTO.getCustomerId() == null) {
            throw new IllegalArgumentException("Customer ID is required.");
        }

        Customer customer = customerRepository.findById(profileDTO.getCustomerId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Customer", "Id", profileDTO.getCustomerId()));

        Profile profile = new Profile();
        profile.setCustomer(customer);
        profile.setFirstName(profileDTO.getFirstName());
        profile.setLastName(profileDTO.getLastName());
        profile.setPhoneNumber(profileDTO.getPhoneNumber());

        if (profileDTO.getAddresses() != null) {
            for (AddressDTO addressDto : profileDTO.getAddresses()) {

                Address address = new Address();
                address.setStreet(addressDto.getStreet());
                address.setCity(addressDto.getCity());
                address.setState(addressDto.getState());
                address.setCountry(addressDto.getCountry());
                address.setPostalCode(addressDto.getPostalCode());
                address.setType(addressDto.getType());

                profile.addAddress(address);
            }
        }

        Profile saved = profileRepository.save(profile);
        return convertToProfileDTO(saved);
    }

    // ================= UPDATE =================

    @Override
    @Transactional
    public ProfileDTO updateProfile(Long profileId, ProfileDTO profileDTO) {

        Profile existingProfile = profileRepository.findById(profileId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Profile", "Id", profileId));

        existingProfile.setFirstName(profileDTO.getFirstName());
        existingProfile.setLastName(profileDTO.getLastName());
        existingProfile.setPhoneNumber(profileDTO.getPhoneNumber());

        // ✅ SAFE REMOVE (important for orphanRemoval)
        List<Address> existingAddresses = new ArrayList<>(existingProfile.getAddresses());
        for (Address address : existingAddresses) {
            existingProfile.removeAddress(address);
        }

        // ✅ ADD NEW ADDRESSES
        if (profileDTO.getAddresses() != null) {
            for (AddressDTO addressDTO : profileDTO.getAddresses()) {

                Address address = new Address();
                address.setStreet(addressDTO.getStreet());
                address.setCity(addressDTO.getCity());
                address.setState(addressDTO.getState());
                address.setCountry(addressDTO.getCountry());
                address.setPostalCode(addressDTO.getPostalCode());
                address.setType(addressDTO.getType());

                existingProfile.addAddress(address);
            }
        }

        Profile updated = profileRepository.save(existingProfile);
        return convertToProfileDTO(updated);
    }

    // ================= DELETE PROFILE =================

    @Override
    @Transactional
    public void deleteProfile(Long profileId) {

        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Profile", "Id", profileId));

        profileRepository.delete(profile);
    }

    // ================= GET ALL =================

    @Override
    @Transactional(readOnly = true)
    public List<ProfileDTO> getAllProfiles() {
        return profileRepository.findAll()
                .stream()
                .map(this::convertToProfileDTO)
                .collect(Collectors.toList());
    }

    // ================= GET BY ID =================

    @Override
    @Transactional(readOnly = true)
    public ProfileDTO getProfileById(Long profileId) {

        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Profile", "Id", profileId));

        return convertToProfileDTO(profile);
    }

    // ================= CONVERTERS =================

    private ProfileDTO convertToProfileDTO(Profile profile) {

        ProfileDTO dto = new ProfileDTO();
        dto.setId(profile.getId());
        dto.setFirstName(profile.getFirstName());
        dto.setLastName(profile.getLastName());
        dto.setPhoneNumber(profile.getPhoneNumber());
        dto.setCustomerId(profile.getCustomer() != null
                ? profile.getCustomer().getId()
                : null);

        if (profile.getAddresses() != null) {
            dto.setAddresses(profile.getAddresses()
                    .stream()
                    .map(this::convertToAddressDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private AddressDTO convertToAddressDTO(Address address) {

        AddressDTO dto = new AddressDTO();
        dto.setId(address.getId());
        dto.setStreet(address.getStreet());
        dto.setCity(address.getCity());
        dto.setState(address.getState());
        dto.setCountry(address.getCountry());
        dto.setPostalCode(address.getPostalCode());
        dto.setType(address.getType());

        return dto;
    }
}
