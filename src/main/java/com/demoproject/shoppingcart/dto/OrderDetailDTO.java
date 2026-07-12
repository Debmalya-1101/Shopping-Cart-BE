package com.demoproject.shoppingcart.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Rich DTO returned by GET /api/orders/{orderId}.
 * Provides everything a customer needs to render a full Order Details page:
 * order metadata, shipping snapshot, enriched line items, and order summary.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDetailDTO {

    // ── Order Information ────────────────────────────────────────────────────
    private Long orderId;
    private String orderStatus;
    private String paymentStatus;
    private Long totalAmount;
    private Long subTotal;
    private Long tax;
    private Long shippingFee;
    private Long platformFee;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Shipping Information (snapshot captured at checkout) ─────────────────
    private String recipientName;
    private String email;
    private Long phoneNo;
    private String address;

    // ── Delivery Partner Information ──────────────────────────────────────────
    private Long deliveryPartnerId;
    private String deliveryPartnerName;
    private String deliveryPartnerPhone;

    // ── Order Items ───────────────────────────────────────────────────────────
    private List<OrderDetailItemDTO> items;

    // ── Order Summary ─────────────────────────────────────────────────────────
    private int totalItems;     // total number of distinct line items
    private Long grandTotal;    // same as totalAmount; exposed for UI convenience
}
