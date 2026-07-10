package com.demoproject.shoppingcart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for admin-initiated order cancellation.
 * Requires a mandatory reason which is shared with the customer in
 * the apology/refund notification.
 */
@Data
@NoArgsConstructor
public class AdminCancelOrderRequest {

    @NotBlank(message = "Cancellation reason is required")
    @Size(min = 10, max = 1000, message = "Reason must be between 10 and 1000 characters")
    private String reason;
}
