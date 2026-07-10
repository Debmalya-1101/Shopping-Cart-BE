package com.demoproject.shoppingcart.model;

/**
 * Internal lifecycle status of a Shipment, tracked by the fulfillment team
 * and the delivery partner.
 *
 * State transitions:
 *   CREATED → ASSIGNED → PICKED_UP → OUT_FOR_DELIVERY → DELIVERED   (happy path)
 *                                                      → DELIVERY_FAILED → ASSIGNED (retry)
 *                                                                        → RETURNED  (admin gives up)
 */
public enum ShipmentStatus {
    /** Shipment record created after payment; no delivery partner assigned yet. */
    CREATED,

    /** Delivery partner has been assigned by admin. */
    ASSIGNED,

    /** Delivery partner has physically picked up the parcel. */
    PICKED_UP,

    /** Delivery partner is on the way to deliver to the customer. */
    OUT_FOR_DELIVERY,

    /** Parcel successfully delivered to the customer. Terminal state. */
    DELIVERED,

    /** Delivery attempt failed (customer unavailable, address issue, etc.). */
    DELIVERY_FAILED,

    /** Parcel returned to warehouse after irrecoverable delivery failure. Terminal state. */
    RETURNED
}
