package com.demoproject.shoppingcart.service.impl;

import com.demoproject.shoppingcart.dto.DeliveryPartnerResponseDTO;
import com.demoproject.shoppingcart.dto.DeliveryPartnerSignupRequest;
import com.demoproject.shoppingcart.dto.DeliveryPartnerStatusUpdateRequest;
import com.demoproject.shoppingcart.model.AppUser;
import com.demoproject.shoppingcart.model.DeliveryPartner;
import com.demoproject.shoppingcart.model.DeliveryPartnerStatus;
import com.demoproject.shoppingcart.model.Role;
import com.demoproject.shoppingcart.repository.DeliveryPartnerRepository;
import com.demoproject.shoppingcart.repository.UserRepository;
import com.demoproject.shoppingcart.service.DeliveryPartnerService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeliveryPartnerServiceImpl implements DeliveryPartnerService {

    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DeliveryPartnerServiceImpl(DeliveryPartnerRepository deliveryPartnerRepository,
                                      UserRepository userRepository,
                                      PasswordEncoder passwordEncoder) {
        this.deliveryPartnerRepository = deliveryPartnerRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void registerDeliveryPartner(DeliveryPartnerSignupRequest request) {
        String email = request.getEmail();
        if (email == null) {
            throw new RuntimeException("Email is required");
        }
        if (userRepository.findByEmailId(email).isPresent()) {
            throw new RuntimeException("Email already in use");
        }

        String username = email;
        if (userRepository.findByUserName(username).isPresent()) {
            throw new RuntimeException("Username already in use");
        }

        AppUser user = new AppUser();
        user.setEmailId(email);
        user.setUserName(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.ROLE_DELIVERY_PARTNER);

        user = userRepository.save(user);

        DeliveryPartner dp = new DeliveryPartner();
        dp.setUser(user);
        dp.setFullName(request.getFullName());
        dp.setPhoneNumber(request.getPhoneNumber());
        dp.setDateOfBirth(request.getDateOfBirth() != null ? request.getDateOfBirth() : java.time.LocalDate.of(2000, 1, 1));
        dp.setAddress(request.getAddress() != null ? request.getAddress() : "Not Provided");
        dp.setVehicleType(request.getVehicleType());
        
        String vehicleNumber = request.getVehicleNumber();
        dp.setVehicleNumber(vehicleNumber);
        
        com.demoproject.shoppingcart.model.IdType idType = request.getIdType() != null ? request.getIdType() : com.demoproject.shoppingcart.model.IdType.DRIVING_LICENSE;
        dp.setIdType(idType);
        
        String idNumber = request.getIdNumber();
        dp.setIdNumber(idNumber);
        
        dp.setStatus(DeliveryPartnerStatus.PENDING);

        deliveryPartnerRepository.save(dp);
    }

    @Override
    public DeliveryPartnerResponseDTO getDeliveryPartnerById(Long id) {
        DeliveryPartner dp = deliveryPartnerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Delivery Partner not found"));
        return mapToDTO(dp);
    }

    @Override
    public DeliveryPartnerResponseDTO getDeliveryPartnerByUserId(Long userId) {
        DeliveryPartner dp = deliveryPartnerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Delivery Partner not found for user"));
        return mapToDTO(dp);
    }

    @Override
    public List<DeliveryPartnerResponseDTO> getAllDeliveryPartners(DeliveryPartnerStatus status) {
        List<DeliveryPartner> list;
        if (status != null) {
            list = deliveryPartnerRepository.findByStatus(status);
        } else {
            list = deliveryPartnerRepository.findAll();
        }
        return list.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DeliveryPartnerResponseDTO updateDeliveryPartnerStatus(Long id, DeliveryPartnerStatusUpdateRequest request, Long adminUserId, String adminUsername) {
        DeliveryPartner dp = deliveryPartnerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Delivery Partner not found"));

        DeliveryPartnerStatus currentStatus = dp.getStatus();
        DeliveryPartnerStatus newStatus = request.getStatus();

        if (currentStatus == newStatus) {
            throw new IllegalStateException("Delivery Partner is already in " + currentStatus + " status.");
        }

        boolean isValid = false;
        if (currentStatus == DeliveryPartnerStatus.PENDING) {
            isValid = (newStatus == DeliveryPartnerStatus.APPROVED || newStatus == DeliveryPartnerStatus.REJECTED);
        } else if (currentStatus == DeliveryPartnerStatus.APPROVED) {
            isValid = (newStatus == DeliveryPartnerStatus.SUSPENDED);
        } else if (currentStatus == DeliveryPartnerStatus.SUSPENDED) {
            isValid = (newStatus == DeliveryPartnerStatus.APPROVED);
        } else if (currentStatus == DeliveryPartnerStatus.REJECTED) {
            isValid = (newStatus == DeliveryPartnerStatus.APPROVED);
        }

        if (!isValid) {
            throw new IllegalStateException("Invalid status transition from " + currentStatus + " to " + newStatus);
        }

        dp.setStatus(newStatus);
        dp.setReviewedByUserId(adminUserId);
        dp.setReviewedByUsername(adminUsername);
        dp.setReviewedAt(java.time.LocalDateTime.now());

        dp = deliveryPartnerRepository.save(dp);
        return mapToDTO(dp);
    }

    private DeliveryPartnerResponseDTO mapToDTO(DeliveryPartner dp) {
        DeliveryPartnerResponseDTO dto = new DeliveryPartnerResponseDTO();
        dto.setId(dp.getId());
        dto.setUserId(dp.getUser().getId());
        dto.setFullName(dp.getFullName());
        dto.setEmail(dp.getUser().getEmailId());
        dto.setPhoneNumber(dp.getPhoneNumber());
        dto.setStatus(dp.getStatus());
        dto.setDateOfBirth(dp.getDateOfBirth());
        dto.setAddress(dp.getAddress());
        dto.setVehicleType(dp.getVehicleType());
        dto.setVehicleNumber(dp.getVehicleNumber());
        dto.setIdType(dp.getIdType());
        dto.setIdNumber(dp.getIdNumber());
        dto.setReviewedByUserId(dp.getReviewedByUserId());
        dto.setReviewedByUsername(dp.getReviewedByUsername());
        dto.setReviewedAt(dp.getReviewedAt());
        dto.setCreatedAt(dp.getCreatedAt());
        dto.setUpdatedAt(dp.getUpdatedAt());
        return dto;
    }
}
