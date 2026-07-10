package com.demoproject.shoppingcart.model;

/**
 * Per-item status used for the return workflow.
 * These values are set on individual OrderItems — not on the parent Order.
 *
 * Note: The full return workflow (approval, pickup, restock) is planned for a
 * future iteration. This enum is defined in full now so the data model is
 * ready for that integration without a schema change.
 *
 * Happy-path return flow (future):
 *   RETURN_REQUESTED → RETURN_APPROVED → RETURN_IN_TRANSIT → RETURNED
 *
 * Rejection path:
 *   RETURN_REQUESTED → RETURN_REJECTED
 */
public enum OrderItemStatus {
    /** User has requested a return for this item. Awaiting admin review. */
    RETURN_REQUESTED,

    /** Admin approved the return request. Pickup/drop-off to be arranged. */
    RETURN_APPROVED,

    /** Item is in transit back to the warehouse. */
    RETURN_IN_TRANSIT,

    /** Item has been received at the warehouse. Refund will be initiated. */
    RETURNED,

    /** Admin rejected the return request (e.g. outside return window, damaged by user). */
    RETURN_REJECTED
}
