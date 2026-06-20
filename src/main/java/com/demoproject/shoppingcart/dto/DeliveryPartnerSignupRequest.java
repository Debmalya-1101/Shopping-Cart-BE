package com.demoproject.shoppingcart.dto;

import com.demoproject.shoppingcart.model.IdType;
import com.demoproject.shoppingcart.model.VehicleType;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DeliveryPartnerSignupRequest {
    private String fullName;
    private String email;
    private String password;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private String address;
    private VehicleType vehicleType;
    private String vehicleNumber;
    private IdType idType;
    private String idNumber;
}
