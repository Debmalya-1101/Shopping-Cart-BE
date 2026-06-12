package com.demoproject.shoppingcart.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddressDTO {
    private Long id;

    @NotBlank(message = "Contact name is required")
    private String contactName;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be a 10-digit number")
    private String mobileNumber;

    @NotBlank(message = "Address line is required")
    private String addressLine;

    private String city; // Option B: API lookup fills this if left blank

    private String state; // Option B: API lookup fills this if left blank

    @NotBlank(message = "Postal PIN code is required")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "PIN code must be a valid 6-digit Indian PIN code")
    private String postalCode;

    private String country = "India";

    private Boolean isDefault = false;
}
