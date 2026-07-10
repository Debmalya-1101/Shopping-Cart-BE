package com.demoproject.shoppingcart.model;

/**
 * Represents the lifecycle of an Order from a user's perspective.
 *
 * State transitions (happy path):
 *   PENDING_PAYMENT → CONFIRMED → PROCESSING → SHIPPED → OUT_FOR_DELIVERY → DELIVERED
 *
 * Payment failure path:
 *   PENDING_PAYMENT → PAYMENT_FAILED  (user may retry, up to 3 times)
 *
 * Cancellation paths:
 *   PENDING_PAYMENT → CANCELLED  (user cancels before paying)
 *   CONFIRMED       → CANCELLED  (user or admin cancels after payment; triggers refund)
 *
 * Delivery failure path:
 *   OUT_FOR_DELIVERY → DELIVERY_FAILED → CANCELLED (undeliverable, triggers refund)
 *
 * Return path (future iteration):
 *   DELIVERED → (return items requested/approved by admin)
 */
public enum OrderStatus {
    /** Order created; awaiting successful payment. */
    PENDING_PAYMENT,

    /** Payment failed; user may retry (up to 3 times). */
    PAYMENT_FAILED,

    /** Payment succeeded; shipment is being prepared. */
    CONFIRMED,

    /** Shipment has been assigned to a delivery partner and picked up from warehouse. */
    PROCESSING,

    /** Parcel is in transit (picked up by delivery partner). */
    SHIPPED,

    /** Delivery partner is out delivering the parcel today. */
    OUT_FOR_DELIVERY,

    /** Order successfully delivered to customer. */
    DELIVERED,

    /** Last-mile delivery attempt failed (e.g. customer unavailable, wrong address). */
    DELIVERY_FAILED,

    /** Order cancelled (before shipping). If payment was made, a refund is triggered. */
    CANCELLED,

    /** Order returned after delivery (future return flow). */
    RETURNED
}
