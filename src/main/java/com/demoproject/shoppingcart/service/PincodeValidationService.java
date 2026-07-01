package com.demoproject.shoppingcart.service;

import com.demoproject.shoppingcart.dto.AddressDTO;

public interface PincodeValidationService {
    void validateAndUpdatePincode(AddressDTO addressDTO);
}
