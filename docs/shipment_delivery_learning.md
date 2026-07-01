# Shipment & Delivery Module Learning Guide

This document is a comprehensive developer reference guide for the Shipment & Delivery module. If you are revisiting this codebase, this guide explains exactly what we built, why we built it, and the software engineering principles applied.

---

## 1. Module Overview

The Shipment & Delivery module introduces the post-checkout supply chain logistics into our e-commerce platform. Before this module, our application only tracked products up to the point of a successful payment. With this addition, we now cover the real-world fulfillment process.

It solves the following problems:
* Managing an external workforce of Delivery Partners.
* Tracking physical product movements from warehouse to customer (Shipments).
* Handling delivery failures and customer feedback.
* Maintaining a strict chain of custody and accountability.

---

## 2. Before vs After Architecture

**Before:**
Order Flow: `Cart` → `Checkout` → `Order (PLACED)` → `Payment (SUCCESS)` → Admin manually changes Order Status.
There was no concept of how the item got to the customer, and there were no delivery personnel.

**After:**
Order Flow: `Cart` → `Checkout` → `Order` → `Payment (SUCCESS)` → **`Shipment (CREATED)`** → **`Admin Assigns Partner`** → **`Partner Workflow`** → **`Delivery & Feedback`**
The `Shipment` is now a dedicated bounded context, decoupling order metadata (billing, items) from logistical data (tracking numbers, driver assignments).

---

## 3. Business Flow End-to-End

```text
1. DP Onboarding: Delivery Partner (DP) signs up via `/delivery-partner/signup`.
2. Admin Approval: Admin reviews details and updates status to `APPROVED`.
3. DP Login: DP logs in and receives a JWT with `ROLE_DELIVERY_PARTNER`.
4. Customer Purchase: Customer completes a purchase and confirms Razorpay payment.
5. Shipment Creation: Backend automatically creates a `Shipment` entity tied to the `Order`.
6. Assignment: Admin assigns the `Shipment` to the approved `DeliveryPartner`.
7. Fulfillment: DP updates shipment status: `PICKED_UP` → `OUT_FOR_DELIVERY` → `DELIVERED`.
   * Note: As the shipment moves, the `Order` status is automatically synchronized.
8. Customer Feedback: Customer receives item and submits a 1-5 rating and comment.
9. Dashboard: DP sees their anonymized feedback; Admin sees full global analytics.
```

---

## 4. Delivery Partner Registration & Login Restrictions

### The Theory & Concepts
**`ROLE_DELIVERY_PARTNER`:** A new Spring Security granted authority. It differentiates drivers from standard `ROLE_USER` accounts and guarantees they can access their dashboard, but cannot tamper with generic user resources.

**Delivery partners do not get instant access.** They sign up via a dedicated endpoint and are created in a `PENDING` state. They must be explicitly `APPROVED` by an Admin. This ensures background checks/vetting can occur before they touch inventory.

### Implementation Deep Dive: Registration
* **Problem Solved**: Separating generic user signup from driver signup.
* **Why this design**: We reuse `AppUser` for authentication but keep driver metadata in `DeliveryPartner`.
* **Classes Involved**: `AuthController`, `DeliveryPartnerServiceImpl`, `UserRepository`, `DeliveryPartnerRepository`.
* **Flow**:
  `POST /auth/delivery-partner/signup` (Payload: `DeliveryPartnerSignupRequest`)
  → `AuthController` delegates to `DeliveryPartnerServiceImpl.registerDeliveryPartner()`
  → Validation checks if email or username exists in `UserRepository`.
  → Creates `AppUser` with `Role.ROLE_DELIVERY_PARTNER`.
  → Saves `AppUser`.
  → Creates `DeliveryPartner` entity linked to `AppUser`, setting status to `PENDING`.
  → Saves `DeliveryPartner`.
  → Returns success message.

### Implementation Deep Dive: Login Restriction
* **Problem Solved**: Preventing unapproved drivers from accessing the system.
* **Where it happens**: Inside `AuthController.login()`.
* **Flow**:
  1. User authenticates via `AuthenticationManager`.
  2. If the authenticated principal has `ROLE_DELIVERY_PARTNER`, the code fetches the `DeliveryPartner` profile.
  3. Checks `dp.getStatus()`. If `PENDING`, `REJECTED`, or `SUSPENDED`, it returns a `403 Forbidden` with a custom message.
  4. Only if `APPROVED`, it generates and returns the JWT.

---

## 5. Admin Approval Workflow

### The Theory & Concepts
To maintain security and trust, the Admin must manually approve, reject, or suspend a driver. An audit trail is kept to know which admin approved a driver and when.

### Implementation Deep Dive
* **API**: `PUT /api/admin/delivery-partners/{id}/status` taking `DeliveryPartnerStatusUpdateRequest`.
* **Classes Involved**: `AdminDeliveryPartnerController`, `DeliveryPartnerServiceImpl`.
* **State Machine Rules**:
  * `PENDING` → `APPROVED` (Sets `approvedBy` and `approvedAt`)
  * `PENDING` → `REJECTED`
  * `REJECTED` → `APPROVED` (Allows fixing a mistake)
  * `APPROVED` → `SUSPENDED` (E.g., bad behavior)
  * `SUSPENDED` → `APPROVED` (Reinstatement)
* **Audit Fields**: When transitioning to `APPROVED`, the service extracts the Admin's username from the JWT and saves it in `dp.setApprovedByAdminUsername()`.

---

## 6. Shipment Creation

### The Theory & Concepts
**Shipment Domain & Lifecycle:** Shipments track the physical movement. Statuses include: `CREATED`, `ASSIGNED`, `PICKED_UP`, `OUT_FOR_DELIVERY`, `DELIVERED`, `FAILED`, and `RETURNED`.
Shipments are generated automatically when a customer successfully pays for an order.

### Implementation Deep Dive
* **Why in `PaymentServiceImpl`?**: If created during Order Placement, unpaid orders would have shipments, causing logistics confusion. Creating it upon payment confirmation ensures only paid goods enter the supply chain.
* **Classes Involved**: `PaymentServiceImpl`, `ShipmentServiceImpl`.
* **Flow**:
  1. `PaymentServiceImpl.confirmPayment()` verifies the Razorpay signature.
  2. Marks payment as `COMPLETED`.
  3. Calls `shipmentService.createShipmentForOrder(order)`.
  4. Transaction boundary ensures that if shipment creation fails, the payment confirmation rolls back.
* **Preventing Duplicates**: `ShipmentServiceImpl.createShipmentForOrder` first checks `shipmentRepository.existsByOrder(order)`. If true, it skips creation. This handles late or duplicate webhooks safely.

---

## 7. Shipment Assignment & Reassignment

### The Theory & Concepts
**Assignment vs Reassignment:** A shipment in `CREATED` state needs an initial driver (`ASSIGNED`). If a driver is unavailable or if a shipment fails delivery (`FAILED`), an Admin can reassign it to a new driver.

### Implementation Deep Dive
* **API Flow**: `POST /api/admin/shipments/{shipmentId}/assign/{partnerId}`
* **Classes Involved**: `AdminShipmentController`, `ShipmentServiceImpl`.
* **Validation Flow**:
  1. Find `Shipment`. Check if status is `CREATED` (initial assignment) or `ASSIGNED`/`FAILED` (reassignment).
  2. Find `DeliveryPartner`. Verify `dp.getStatus() == DeliveryPartnerStatus.APPROVED`. Throws exception if not approved.
  3. Set `shipment.setDeliveryPartner(dp)`, `shipment.setStatus(ShipmentStatus.ASSIGNED)`, and update `assignedAt`.
* **Reassignment Logic**: Explicit business logic permits reassignment of `FAILED` shipments, enabling redelivery without recreating the entire order flow.

---

## 8. Shipment State Machine

### The Theory & Concepts
**Order & Shipment Synchronization**: The customer views the `Order` status, but the driver updates the `Shipment` status. When the shipment hits key milestones, the system must automatically align the parent `Order` status.

### Implementation Deep Dive
The state machine is enforced in `ShipmentServiceImpl`.

* **Enums**: `CREATED`, `ASSIGNED`, `PICKED_UP`, `OUT_FOR_DELIVERY`, `DELIVERED`, `FAILED`, `RETURNED`.
* **Partner Flow** (`updateShipmentStatusByPartner`):
  * Allowed: `ASSIGNED` → `PICKED_UP`
  * Allowed: `PICKED_UP` → `OUT_FOR_DELIVERY`
  * Allowed: `OUT_FOR_DELIVERY` → `DELIVERED` or `FAILED`
  * **Invalid**: Jumping from `ASSIGNED` directly to `DELIVERED`.
* **Admin Flow** (`updateShipmentStatus`):
  * Allowed: `FAILED` → `RETURNED`
* **Order Synchronization**:
  * When `Shipment` becomes `OUT_FOR_DELIVERY`, parent `Order` is updated to `SHIPPED`.
  * When `Shipment` becomes `DELIVERED`, parent `Order` is updated to `DELIVERED`.
  * When `Shipment` becomes `RETURNED`, parent `Order` is updated to `RETURNED`.
  * `FAILED` shipment does *not* change the Order status, allowing Admin to reassign.

---

## 9. Delivery Dashboard

### The Theory & Concepts
**Ownership Validation:** Security model where DPs can only read or mutate shipments that are explicitly assigned to their `partnerId`. This prevents drivers from scraping or modifying other drivers' routes.

### Implementation Deep Dive
* **Partner Identity**: `DeliveryPartnerShipmentController` extracts the username using `authentication.getName()`, fetches `AppUser`, and then finds the linked `DeliveryPartner`. This completely prevents ID spoofing in the payload.
* **Ownership Validation**: When updating status, `ShipmentServiceImpl.updateShipmentStatusByPartner` explicitly checks `if (!shipment.getDeliveryPartner().getId().equals(partnerId)) throw new SecurityException()`.
* **Dashboard Stats**: Instead of fetching all shipments into memory, the service uses `shipmentRepository.countByDeliveryPartnerAndStatus()` to efficiently assemble the `DeliveryPartnerDashboardDTO`.

---

## 10. Delivery Feedback

### The Theory & Concepts
**Feedback Privacy & Separation:** We explicitly built a `delivery_feedback` table separate from the existing product `reviews` table. Product reviews describe the item; Delivery feedback describes the driver. Mixing them corrupts metrics.
We also use a dual-DTO model. Drivers view an anonymized version of feedback (no customer PII), while admins view the full details.

### Implementation Deep Dive
* **Submission Flow**: `POST /api/orders/{orderId}/delivery-feedback` by `ROLE_USER`.
* **Validation**:
  * Must be the owner of the Order.
  * Order must be in `DELIVERED` status.
  * Checks if feedback already exists via `DeliveryFeedbackRepository.existsByOrder()`.
* **Database Constraint**: `UNIQUE(order_id)` constraint on the `delivery_feedback` table prevents race condition duplicates.
* **Privacy Enforcement**:
  * Partner views (`GET /api/delivery-partner/feedback`): Returns `DeliveryFeedbackResponseDTO` (rating, comment).
  * Admin views (`GET /api/admin/delivery-partners/{id}/feedback`): Returns `AdminDeliveryFeedbackResponseDTO` (includes customer PII).
* **Rating Aggregation**: `DeliveryFeedbackRepository.getRatingSummary()` uses a SQL `GROUP BY` and `AVG(rating)` to fetch summaries without N+1 queries.

---

## 11. Database Design Deep Dive

### The Theory & Concepts
New tables were added to support the logistics domain without muddying the e-commerce core tables.

### Implementation Deep Dive
1. **`DeliveryPartner`**
   * **Columns**: `vehicleType`, `drivingLicenseNumber`, `status`, `approvedByAdminUsername`, etc.
   * **Relationships**: `@OneToOne(mappedBy = "deliveryPartner")` to `AppUser`. Chosen to separate auth from domain metadata.
2. **`Shipment`**
   * **Columns**: `trackingNumber` (Human readable `SHP-YYYYMMDD-ID`), `failureReason`, `version`.
   * **Relationships**:
     * `@OneToOne` with `Order` (An order has exactly one shipment).
     * `@ManyToOne` with `DeliveryPartner` (A driver handles many shipments).
   * **Optimistic Locking**: `@Version private Long version;` prevents concurrent state updates.
3. **`DeliveryFeedback`**
   * **Columns**: `rating` (1-5), `comment`.
   * **Relationships**: `@OneToOne` with `Order`, `@ManyToOne` with `Customer` and `DeliveryPartner`.

---

## 12. Security Deep Dive

### The Theory & Concepts
Instead of building a separate authentication portal for drivers, we reused the existing `/auth/login` endpoint but wrapped it in strict Role-Based Access Control (RBAC).

### Implementation Deep Dive
* **Role-Based Access**: All endpoints use `@PreAuthorize`.
  * `/api/admin/**` requires `ROLE_ADMIN`.
  * `/api/delivery-partner/**` requires `ROLE_DELIVERY_PARTNER`.
* **Ownership Validation**:
  * Drivers can only fetch shipments assigned to them (`findByDeliveryPartner`).
  * Drivers can only update shipments they own (explicit `SecurityException` if IDs do not match).
  * Customers can only submit feedback for their own orders (validates `order.getUser().getId() == currentUser.getId()`).

---

## 13. Engineering Decisions

| Problem | Options Considered | Final Decision & Why |
|---------|--------------------|----------------------|
| **Driver Auth Model** | A) Separate Auth Table<br>B) Reuse `AppUser` | **B**. Reuse `AppUser` with `ROLE_DELIVERY_PARTNER`. Simplifies JWT handling and avoids duplicate authentication filters. |
| **Login Block vs API Block** | A) Allow login but block APIs<br>B) Block login completely | **B**. Checked in `AuthController`. Prevent unapproved drivers from even obtaining a JWT, reducing attack surface. |
| **Tracking Number Gen** | A) UUID<br>B) Auto-increment<br>C) Custom Format | **C**. `SHP-YYYYMMDD-ORDERID`. Human-readable for support agents and customers. |
| **Feedback Separation** | A) Add driver rating to product reviews<br>B) Separate table | **B**. Separate `delivery_feedback` table. Mixing them corrupts product ratings based on delivery experience. |
| **Concurrency Risks** | Admin assigns shipment exactly when Driver updates it. | Added `@Version` on `Shipment` and `@Retryable` on service methods. Throws `ObjectOptimisticLockingFailureException`. |
| **Shipment Trigger** | A) On Order placement<br>B) On Payment success | **B**. Creating a shipment for an unpaid order risks shipping unpaid goods. Triggered inside `confirmPayment()`. |
| **Duplicate Webhooks** | Razorpay might send late webhooks after a timeout failure. | **Safe Idempotency**. If a late success webhook hits a FAILED order, the system resurrects the order, re-consumes inventory, and creates a shipment dynamically. |
| **Admin N+1 Query** | Java `stream().map()` vs SQL `GROUP BY` | **SQL GROUP BY**. Calculating admin dashboards in Java memory would crash the server at high volumes. |

---

## 14. Code Walkthrough Section

This section maps features to their exact file locations.

**Delivery Partner Onboarding & Login**
* **API**: `POST /auth/delivery-partner/signup`, `POST /auth/login`
* **Controller**: `AuthController.java`
* **Service**: `DeliveryPartnerServiceImpl.java` (signup logic)
* **Entities**: `AppUser.java`, `DeliveryPartner.java`

**Admin Partner Management**
* **API**: `PUT /api/admin/delivery-partners/{id}/status`
* **Controller**: `AdminDeliveryPartnerController.java`
* **Service**: `DeliveryPartnerServiceImpl.updateStatus()`

**Shipment Assignment**
* **API**: `POST /api/admin/shipments/{id}/assign/{partnerId}`
* **Controller**: `AdminShipmentController.java`
* **Service**: `ShipmentServiceImpl.assignShipment()`

**Shipment State Machine**
* **API**: `PUT /api/delivery-partner/shipments/{id}/status`
* **Controller**: `DeliveryPartnerShipmentController.java`
* **Service**: `ShipmentServiceImpl.updateShipmentStatusByPartner()`
* **Entity**: `Shipment.java`

**Delivery Feedback**
* **APIs**:
  * `POST /api/orders/{id}/delivery-feedback` (Customer - `DeliveryFeedbackController.java`)
  * `GET /api/delivery-partner/feedback` (Driver - `DeliveryPartnerFeedbackController.java`)
  * `GET /api/admin/delivery-partners/{id}/feedback` (Admin - `AdminDeliveryPartnerController.java`)
* **Service**: `DeliveryFeedbackServiceImpl.java`
* **Repository**: `DeliveryFeedbackRepository.java` (Contains custom SQL aggregations).

---

## 15. Future Enhancements

* **OTP-based Delivery Confirmation:** Require the customer to provide a 4-digit PIN to the driver to transition to `DELIVERED`.
* **Live GPS Tracking:** Add WebSockets and a `location_history` table for live map tracking.
* **Route Optimization:** Implement traveling salesperson algorithms for the DP Dashboard to suggest the fastest delivery sequence.
* **Auto-Assignment:** Dispatch shipments based on driver proximity/zones instead of manual Admin assignment.

---

## 16. Key Takeaways

1. **Protect the Boundary:** Always validate ownership by extracting user ID from the secure JWT, never trust client-provided IDs.
2. **Push Math to the Database:** Use SQL aggregations (`COUNT`, `AVG`, `GROUP BY`) instead of pulling thousands of records into Java memory to count them.
3. **Decouple Logistics from Billing:** The `Shipment` and `Order` domains must remain distinct to handle the complexities of real-world fulfillment cleanly.
