package com.example.Ecomm.service;

import java.util.List;

import com.example.Ecomm.dto.AddressDTO;

public interface AddressService {

	AddressDTO saveAddress(AddressDTO addressDTO);

	AddressDTO getAddressById(Long id);



	void deleteAddress(Long id);

	List<AddressDTO> getAddressesByProfileId(Long profileId);

	

	AddressDTO updateAddress(Long id, AddressDTO addressDTO);

}
