package com.demoproject.shoppingcart.dto;

import com.demoproject.shoppingcart.model.DeliveryPartnerStatus;
import com.demoproject.shoppingcart.model.IdType;
import com.demoproject.shoppingcart.model.VehicleType;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DeliveryPartnerResponseDTO {
    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private DeliveryPartnerStatus status;
    private LocalDate dateOfBirth;
    private String address;
    private VehicleType vehicleType;
    private String vehicleNumber;
    private IdType idType;
    private String idNumber;
    private Long reviewedByUserId;
    private String reviewedByUsername;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
