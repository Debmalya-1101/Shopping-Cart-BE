package com.demoproject.shoppingcart.service.impl;

import com.demoproject.shoppingcart.dto.AddressDTO;
import com.demoproject.shoppingcart.model.Address;
import com.demoproject.shoppingcart.model.AppUser;
import com.demoproject.shoppingcart.repository.AddressRepository;
import com.demoproject.shoppingcart.repository.UserRepository;
import com.demoproject.shoppingcart.service.AddressService;
import com.demoproject.shoppingcart.service.PincodeValidationService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final PincodeValidationService pincodeValidationService;

    public AddressServiceImpl(AddressRepository addressRepository,
                              UserRepository userRepository,
                              PincodeValidationService pincodeValidationService) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.pincodeValidationService = pincodeValidationService;
    }

    @Override
    public List<AddressDTO> getMyAddresses() {
        AppUser user = getLoggedInUser();
        return addressRepository.findByUser(user).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional
    public AddressDTO addAddress(AddressDTO addressDTO) {
        AppUser user = getLoggedInUser();

        // 1. External API PIN validation & Auto-fill
        pincodeValidationService.validateAndUpdatePincode(addressDTO);

        // 2. Fetch existing user addresses to determine if this is the first address
        List<Address> existingAddresses = addressRepository.findByUser(user);
        boolean isFirstAddress = existingAddresses.isEmpty();

        // If the user wants to set this as default, or if it is their first address
        boolean shouldBeDefault = addressDTO.getIsDefault() || isFirstAddress;

        if (shouldBeDefault) {
            // Unset current default addresses
            for (Address addr : existingAddresses) {
                if (addr.getIsDefault()) {
                    addr.setIsDefault(false);
                    addressRepository.save(addr);
                }
            }
        }

        Address address = new Address();
        address.setUser(user);
        address.setContactName(addressDTO.getContactName());
        address.setMobileNumber(addressDTO.getMobileNumber());
        address.setAddressLine(addressDTO.getAddressLine());
        address.setCity(addressDTO.getCity());
        address.setState(addressDTO.getState());
        address.setPostalCode(addressDTO.getPostalCode());
        address.setCountry(addressDTO.getCountry() != null ? addressDTO.getCountry() : "India");
        address.setIsDefault(shouldBeDefault);

        Address saved = addressRepository.save(address);
        return convertToDTO(saved);
    }

    @Override
    @Transactional
    public AddressDTO updateAddress(Long id, AddressDTO addressDTO) {
        AppUser user = getLoggedInUser();

        // Retrieve existing address with strict ownership validation
        Address existingAddress = addressRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Address not found or unauthorized"));

        // Optimization: Only trigger external API PIN validation if PIN code actually changed
        if (!existingAddress.getPostalCode().equals(addressDTO.getPostalCode())) {
            pincodeValidationService.validateAndUpdatePincode(addressDTO);
            existingAddress.setPostalCode(addressDTO.getPostalCode());
            existingAddress.setCity(addressDTO.getCity());
            existingAddress.setState(addressDTO.getState());
        } else {
            // If PIN didn't change, just update city/state if user manually provided new values
            if (addressDTO.getCity() != null && !addressDTO.getCity().trim().isEmpty()) {
                existingAddress.setCity(addressDTO.getCity());
            }
            if (addressDTO.getState() != null && !addressDTO.getState().trim().isEmpty()) {
                existingAddress.setState(addressDTO.getState());
            }
        }

        existingAddress.setContactName(addressDTO.getContactName());
        existingAddress.setMobileNumber(addressDTO.getMobileNumber());
        existingAddress.setAddressLine(addressDTO.getAddressLine());
        existingAddress.setCountry(addressDTO.getCountry() != null ? addressDTO.getCountry() : "India");

        // Handle default address toggling if changed
        if (addressDTO.getIsDefault() && !existingAddress.getIsDefault()) {
            List<Address> otherAddresses = addressRepository.findByUser(user);
            for (Address addr : otherAddresses) {
                if (!addr.getId().equals(existingAddress.getId()) && addr.getIsDefault()) {
                    addr.setIsDefault(false);
                    addressRepository.save(addr);
                }
            }
            existingAddress.setIsDefault(true);
        }

        Address saved = addressRepository.save(existingAddress);
        return convertToDTO(saved);
    }

    @Override
    @Transactional
    public void deleteAddress(Long id) {
        AppUser user = getLoggedInUser();

        // Retrieve existing address with strict ownership validation
        Address existingAddress = addressRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Address not found or unauthorized"));

        boolean wasDefault = existingAddress.getIsDefault();

        addressRepository.delete(existingAddress);

        // If we deleted the default address, promote another remaining address as default
        if (wasDefault) {
            List<Address> remaining = addressRepository.findByUser(user).stream()
                    .filter(addr -> !addr.getId().equals(id))
                    .toList();
            if (!remaining.isEmpty()) {
                Address newDefault = remaining.get(0);
                newDefault.setIsDefault(true);
                addressRepository.save(newDefault);
            }
        }
    }

    @Override
    @Transactional
    public AddressDTO setDefaultAddress(Long id) {
        AppUser user = getLoggedInUser();

        // Retrieve existing address with strict ownership validation
        Address targetAddress = addressRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Address not found or unauthorized"));

        if (!targetAddress.getIsDefault()) {
            List<Address> allAddresses = addressRepository.findByUser(user);
            for (Address addr : allAddresses) {
                addr.setIsDefault(addr.getId().equals(targetAddress.getId()));
                addressRepository.save(addr);
            }
        }

        return convertToDTO(targetAddress);
    }

    private AppUser getLoggedInUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private AddressDTO convertToDTO(Address address) {
        return new AddressDTO(
                address.getId(),
                address.getContactName(),
                address.getMobileNumber(),
                address.getAddressLine(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry(),
                address.getIsDefault()
        );
    }
}
