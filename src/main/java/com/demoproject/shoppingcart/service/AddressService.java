package com.demoproject.shoppingcart.service;

import com.demoproject.shoppingcart.dto.AddressDTO;
import java.util.List;

public interface AddressService {
    List<AddressDTO> getMyAddresses();
    AddressDTO addAddress(AddressDTO addressDTO);
    AddressDTO updateAddress(Long id, AddressDTO addressDTO);
    void deleteAddress(Long id);
    AddressDTO setDefaultAddress(Long id);
}
