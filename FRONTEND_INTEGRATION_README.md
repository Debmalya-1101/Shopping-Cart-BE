# Frontend Integration Summary

## Base Notes

- Base URL: use your backend host, e.g. `http://localhost:8080`
- CORS: all controllers use `@CrossOrigin(origins = "*")`
- Auth type: JWT bearer token (short-lived) + opaque refresh token (long-lived)
- Public routes:
  - `/auth/login`
  - `/auth/signup`
  - `/auth/refresh`
  - `/oauth2/authorization/google`
  - `/oauth2/authorization/facebook`
  - `/login/oauth2/code/google` *(OAuth2 callback — handled by the backend, not called directly)*
  - `/login/oauth2/code/facebook` *(OAuth2 callback — handled by the backend, not called directly)*
  - `/api/products/**`
- Protected routes: everything else (requires `Authorization: Bearer <accessToken>`)
- Admin-only routes: `/api/admin/**`

## Required Headers

- For protected routes: `Authorization: Bearer <accessToken>`
- For JSON request bodies: `Content-Type: application/json`
- No custom headers are required by the codebase

## Authentication Flow

### Email / Password login

1. `POST /auth/signup` with `emailId`, `userName`, `password`
2. `POST /auth/login` with `usernameOrEmail`, `password`
3. Store **both** `accessToken` and `refreshToken` from the login response
4. Send `Authorization: Bearer <accessToken>` on all protected requests
5. When any protected API returns `401`, call `POST /auth/refresh` with the stored `refreshToken`
6. On a successful refresh, **replace both stored tokens** with the new values returned
7. Retry the original failed request with the new `accessToken`
8. If `/auth/refresh` itself returns `401`, all sessions are dead — redirect the user to the login page
9. On explicit logout, call `POST /auth/logout` (with a valid `accessToken`) to revoke all refresh tokens server-side
10. Optionally call `GET /auth/me` to fetch the logged-in user's `username`, `emailId`, and `role`

### Social login (Google / Facebook)

1. Redirect (or open in a popup) the browser to `GET /oauth2/authorization/google` or `/oauth2/authorization/facebook`
2. The backend handles the full OAuth2 redirect flow with the provider
3. On success, the browser is redirected to:
   ```
   <OAUTH2_REDIRECT_URI>?accessToken=<jwt>&refreshToken=<opaqueToken>
   ```
   The default redirect URI is `http://localhost:4200/oauth2/callback` — override with the `OAUTH2_REDIRECT_URI` environment variable on the server
4. The frontend callback page reads both tokens from the URL query string, stores them, and clears the URL
5. From this point the flow is identical to email/password login (steps 4–10 above)

> [!IMPORTANT]
> **Account linking:** If a user previously signed up with email/password using the same email address as their Google/Facebook account, the backend automatically links the accounts. They can log in via either method and will use the same user record.

JWT details from code:

- Token subject = username
- Token contains `role` claim like `ROLE_USER` or `ROLE_ADMIN`
- Token type returned by API = `Bearer`
- **Access token expiry: `900000 ms` (15 minutes)**
- **Refresh token expiry: `604800000 ms` (7 days)**

> [!WARNING]
> **Breaking change from earlier versions:** The login response field previously named `token` is now `accessToken`. Update any code that reads `response.token` to read `response.accessToken`.

## REST Endpoints

### Auth

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/auth/login` | Public | `LoginRequest` | `AuthResponse` |
| POST | `/auth/signup` | Public | `SignupRequest` | `String` |
| POST | `/auth/refresh` | Public | `TokenRefreshRequest` | `TokenRefreshResponse` |
| POST | `/auth/logout` | Bearer | None | `String` |
| POST | `/auth/delivery-partner/signup` | Public | `DeliveryPartnerSignupRequest` | `String` |
| GET | `/auth/me` | Bearer | None | `AuthUserInfoDTO` |

### Social Login (OAuth2)

These are browser redirect flows, **not** JSON API calls. The frontend initiates them by navigating the browser to the URL.

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/oauth2/authorization/google` | Public | Initiates Google OAuth2 login |
| GET | `/oauth2/authorization/facebook` | Public | Initiates Facebook OAuth2 login |

On success the backend redirects to:
```
<OAUTH2_REDIRECT_URI>?accessToken=<jwt>&refreshToken=<opaqueToken>
```

On failure the backend redirects to:
```
<OAUTH2_REDIRECT_URI>?error=<reason>
```

### Home Page

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| GET | `/api/home/featured-products` | Public | None | `List<ProductListDTO>` |
| GET | `/api/home/new-arrivals` | Public | None | `List<ProductListDTO>` |

### Products

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| GET | `/api/products` | Public | Query params | `PageResponse<ProductListDTO>` |
| GET | `/api/products/{id}` | Public | Path param | `ProductDetailDTO` |
| GET | `/api/products/categories` | Public | None | `List<String>` |
| GET | `/api/products/brands` | Public | None | `List<String>` |

Supported product list query params:

- `page` default `0`
- `size` default `12`
- `category`
- `brand`
- `search`
- `minPrice`
- `maxPrice`
- `sortBy` default `createdAt`, supported mapping: `createdAt`, `price`, `rating`, `name`
- `order` default `desc`

### Cart

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| GET | `/api/cart` | Bearer | None | `CartDTO` |
| POST | `/api/cart/add` | Bearer | `AddToCartRequest` | `CartDTO` |
| PUT | `/api/cart/item/{itemId}?quantity={n}` | Bearer | Path + query | `CartDTO` |
| DELETE | `/api/cart/item/{itemId}` | Bearer | Path param | `CartDTO` |
| DELETE | `/api/cart/clear` | Bearer | None | `String` |

> [!NOTE]
> **Cart Calculations:** The `CartDTO` now includes `subTotal`, `tax`, `shippingFee`, `platformFee`, and `grandTotal`. These are calculated on the backend (e.g., Shipping is free above Rs. 599, else Rs. 50; Platform fee is Rs. 5). The frontend no longer needs to calculate these manually.

### Wishlist

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| GET | `/api/wishlist` | Bearer | None | `WishlistDTO` |
| POST | `/api/wishlist/add` | Bearer | `AddToWishlistRequest` | `WishlistDTO` |
| DELETE | `/api/wishlist/item/{itemId}` | Bearer | Path param | `WishlistDTO` |
| POST | `/api/wishlist/toggle/{productId}` | Bearer | Path param | `WishlistDTO` |

### Addresses

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| GET | `/api/addresses` | Bearer | None | `List<AddressDTO>` |
| POST | `/api/addresses` | Bearer | `AddressDTO` | `AddressDTO` |
| PUT | `/api/addresses/{id}` | Bearer | `AddressDTO` | `AddressDTO` |
| DELETE | `/api/addresses/{id}` | Bearer | Path param | `String` |
| PUT | `/api/addresses/{id}/default` | Bearer | Path param | `AddressDTO` |

### Orders

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/api/orders/checkout` | Bearer | `CheckoutRequestDTO` | `OrderResponseDTO` |
| POST | `/api/orders/{orderId}/cancel` | Bearer | None | `OrderResponseDTO` |
| GET | `/api/orders` | Bearer | None | `List<OrderResponseDTO>` |
| GET | `/api/orders/{orderId}` | Bearer | Path param | `OrderDetailDTO` |

> [!NOTE]
> **Order Calculations:** The `OrderResponseDTO` and `OrderDetailDTO` now include `subTotal`, `tax`, `shippingFee`, `platformFee`, and `total` / `grandTotal`. These match the cart calculations and are securely computed on the backend during checkout.

### Payments

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/api/payments/initiate/{orderId}` | Bearer | Path param | `PaymentInitiateResponseDTO` |
| POST | `/api/payments/confirm` | Bearer | `PaymentConfirmRequestDTO` | `String` |
| POST | `/api/payments/retry` | Bearer | `RetryPaymentRequest` | `PaymentInitiateResponseDTO` |

> [!NOTE]
> The backend integrates with **Razorpay**. `POST /api/payments/initiate/{orderId}` returns a Razorpay Order ID in the `paymentToken` field. The frontend should use this token to open the Razorpay Checkout flow. After a successful payment, the frontend must submit the `razorpay_payment_id` and `razorpay_signature` to `/api/payments/confirm` to securely verify the payment.

> [!WARNING]
> `POST /api/payments/retry` enforces a maximum limit of 3 retries per order. If the limit is exceeded, the endpoint will return an HTTP 500/400 error with the message "Maximum payment retries exceeded. Please create a new order."

### Reviews

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/api/reviews` | Bearer | `CreateReviewRequest` | `ReviewDTO` |
| PUT | `/api/reviews/{id}` | Bearer | `UpdateReviewRequest` | `ReviewDTO` |
| DELETE | `/api/reviews/{id}` | Bearer | Path param | `String` |
| GET | `/api/reviews/product/{productId}?page=0&size=10` | Bearer | Path + query | `PageResponse<ReviewDTO>` |

### Admin Users

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| GET | `/api/admin/users` | Admin Bearer | Query params | `PageResponse<AdminUserDTO>` |
| GET | `/api/admin/users/{id}` | Admin Bearer | Path param | `AdminUserDTO` |
| PUT | `/api/admin/users/{id}/roles` | Admin Bearer | `role` enum | `AdminUserDTO` |
| PUT | `/api/admin/users/{id}/status` | Admin Bearer | `active` boolean | `AdminUserDTO` |

Supported `/api/admin/users` query params:
- `role`
- `active`
- `search`
- `page` default `0`
- `size` default `10`

### Admin Inventory

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| GET | `/api/admin/inventory` | Admin Bearer | Query params | `PageResponse<InventoryResponseDTO>` |
| GET | `/api/admin/inventory/product/{productId}` | Admin Bearer | Path param | `InventoryResponseDTO` |
| GET | `/api/admin/inventory/{inventoryId}/transactions` | Admin Bearer | Path + query params | `PageResponse<InventoryTransactionDTO>` |
| POST | `/api/admin/inventory/product/{productId}/adjust` | Admin Bearer | `InventoryAdjustmentRequest` | `InventoryResponseDTO` |
| GET | `/api/admin/inventory/analytics` | Admin Bearer | Query params | `InventoryAnalyticsDashboardDTO` |

Supported `/api/admin/inventory` query params:
- `page`, `size`
- `productId`
- `productName`
- `lowStock` (boolean)
- `outOfStock` (boolean)
- `sortBy` default `updatedAt`, supported mapping: `availableQuantity`, `reservedQuantity`, `reorderLevel`, `updatedAt`
- `order` default `desc`

Supported `/api/admin/inventory/{id}/transactions` query params:
- `page`, `size`
- `transactionType` (enum: RESTOCK, RESERVE, RELEASE, CONSUME, ADJUSTMENT)
- `referenceType`
- `referenceId`
- `startDate` (ISO 8601 Date Time String)
- `endDate` (ISO 8601 Date Time String)

### Admin Products

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/api/admin/products` | Admin Bearer | `CreateProductRequest` | `ProductAdminDTO` |
| PUT | `/api/admin/products/{id}` | Admin Bearer | `UpdateProductRequest` | `ProductAdminDTO` |
| GET | `/api/admin/products/{id}` | Admin Bearer | Path param | `ProductAdminDTO` |
| GET | `/api/admin/products` | Admin Bearer | Query params | `PageResponse<ProductAdminDTO>` |
| PATCH | `/api/admin/products/{id}/stock` | Admin Bearer | `UpdateStockRequest` | `String` |
| PATCH | `/api/admin/products/{id}/status` | Admin Bearer | `UpdateProductStatusRequest` | `String` |
| DELETE | `/api/admin/products/{id}` | Admin Bearer | Path param | `String` |

Supported admin product list query params:

- `category`
- `brand`
- `active`
- `search`
- `sortBy` default `createdAt`, supported mapping: `createdAt`, `price`, `rating`, `name`, `stock`
- `order` default `desc`
- `page` default `0`
- `size` default `10`

### Admin Scraper

Provides capability to scrape and seed products directly from Amazon India (amazon.in) or Flipkart India (flipkart.com) using ASINs/FSNs.

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/api/admin/scraper/amazon` | Admin Bearer | `AmazonScrapeRequest` | `List<AmazonScrapeResultDTO>` |
| POST | `/api/admin/scraper/flipkart` | Admin Bearer | `FlipkartScrapeRequest` | `List<FlipkartScrapeResultDTO>` |

### Admin Orders

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| GET | `/api/admin/orders` | Admin Bearer | Query params | `Page<AdminOrderResponseDTO>` |
| GET | `/api/admin/orders/{orderId}` | Admin Bearer | Path param | `AdminOrderResponseDTO` |
| POST | `/api/admin/orders/{orderId}/cancel` | Admin Bearer | `AdminCancelOrderRequest` | `AdminOrderResponseDTO` |
| POST | `/api/admin/orders/{orderId}/items/return` | Admin Bearer | `OrderItemReturnRequestDTO` | `OrderResponseDTO` |

Supported admin order query params:

- `status` optional enum — any value from the `OrderStatus` enum (see [Enum Reference](#enum-reference) section)
- Spring pageable params such as `page`, `size`, `sort`
- Default pageable: `size=20`, `sort=createdAt,DESC`


### Admin Analytics

| Method | Path | Auth | Request | Response |
|---|---|---|---|
| GET | `/api/admin/analytics/dashboard` | Admin Bearer | None | `DashboardAnalyticsDTO` |

> [!NOTE]
> **Dashboard Metrics:** The backend returns pre-calculated top-level metrics and aggregated chart data. The `totalUsers` metric has been optimized to exclusively count active user accounts (`active = true` and `role = ROLE_USER`). This prevents the frontend from needing to fetch all users and calculate aggregations on the client side.

### Admin Notifications

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| GET | `/api/admin/notifications/logs` | Admin Bearer | `?page=&size=&status=` (optional) | `PageResponse<NotificationLogDTO>` |

### Admin Categories

Full CRUD for product categories. Each category response includes all its defined attribute keys.

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| GET | `/api/admin/categories` | Admin Bearer | None | `List<CategoryDTO>` |
| GET | `/api/admin/categories/{id}` | Admin Bearer | Path param | `CategoryDTO` |
| POST | `/api/admin/categories` | Admin Bearer | `CreateCategoryRequest` | `CategoryDTO` |
| PUT | `/api/admin/categories/{id}` | Admin Bearer | `CreateCategoryRequest` | `CategoryDTO` |
| DELETE | `/api/admin/categories/{id}` | Admin Bearer | Path param | `String` |

> [!WARNING]
> Deleting a category fails with `400` if any products are still linked to it.

### Admin Attribute Keys

Full CRUD for attribute keys (RAM, ROM, Camera, Storage, etc.) under a category.
Used to build the attributes section of the product add/edit form.

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| GET | `/api/admin/attribute-keys` | Admin Bearer | `?categoryId=` (optional) | `List<AdminAttributeKeyDTO>` |
| GET | `/api/admin/attribute-keys/{id}` | Admin Bearer | Path param | `AdminAttributeKeyDTO` |
| POST | `/api/admin/attribute-keys` | Admin Bearer | `CreateAttributeKeyRequest` | `AdminAttributeKeyDTO` |
| PUT | `/api/admin/attribute-keys/{id}` | Admin Bearer | `CreateAttributeKeyRequest` | `AdminAttributeKeyDTO` |
| DELETE | `/api/admin/attribute-keys/{id}` | Admin Bearer | Path param | `String` |

> [!WARNING]
> Deleting an attribute key fails with `400` if any product currently has a value for that key.

### Delivery Partner Auth

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/auth/delivery-partner/signup` | Public | `DeliveryPartnerSignupRequest` | `String` |

> [!NOTE]
> The delivery partner signup endpoint is also listed under the Auth section above.

### Admin Delivery Partner Management

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| GET | `/api/admin/delivery-partners?status=PENDING&page=0&size=10` | Admin Bearer | None | `PageResponse<DeliveryPartnerResponseDTO>` |
| GET | `/api/admin/delivery-partners/{id}` | Admin Bearer | None | `DeliveryPartnerResponseDTO` |
| PUT | `/api/admin/delivery-partners/{id}/status` | Admin Bearer | `DeliveryPartnerStatusUpdateRequest` | `DeliveryPartnerResponseDTO` |
| GET | `/api/admin/delivery-partners/{id}/feedback?page=0&size=10` | Admin Bearer | None | `PageResponse<AdminDeliveryFeedbackResponseDTO>` |

### Admin Shipments

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| GET | `/api/admin/shipments/unassigned?page=0&size=10` | Admin Bearer | None | `PageResponse<ShipmentResponseDTO>` |
| POST | `/api/admin/shipments/{shipmentId}/assign/{partnerId}` | Admin Bearer | Path params | `ShipmentResponseDTO` |

### Delivery Partner Dashboard

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| GET | `/api/delivery-partner/shipments/dashboard` | DP Bearer | None | `DeliveryPartnerDashboardDTO` |
| GET | `/api/delivery-partner/shipments/active?page=0&size=10` | DP Bearer | None | `PageResponse<ShipmentResponseDTO>` |
| GET | `/api/delivery-partner/shipments/history?page=0&size=10` | DP Bearer | None | `PageResponse<ShipmentResponseDTO>` |
| GET | `/api/delivery-partner/shipments/{id}` | DP Bearer | Path param | `ShipmentResponseDTO` |
| PUT | `/api/delivery-partner/shipments/{id}/status` | DP Bearer | `ShipmentStatusUpdateRequest` | `ShipmentResponseDTO` |

### Delivery Feedback

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/api/orders/{orderId}/delivery-feedback` | Bearer (User) | `DeliveryFeedbackRequestDTO` | `DeliveryFeedbackResponseDTO` |
| GET | `/api/delivery-partner/feedback/summary` | DP Bearer | None | `DeliveryPartnerRatingSummaryDTO` |
| GET | `/api/delivery-partner/feedback?page=0&size=10` | DP Bearer | None | `PageResponse<DeliveryFeedbackResponseDTO>` |
| GET | `/api/admin/delivery-partners/ratings` | Admin Bearer | None | `List<DeliveryPartnerRatingSummaryDTO>` |

## Email Notifications (Backend Driven)

The backend features an event-driven Notification System. This is completely transparent to the frontend — **no new API calls are required** to trigger emails.

Events are published at each major lifecycle milestone. Future notification listeners will send emails/push notifications based on these events:

| Event | Fired When | Who Gets Notified |
|---|---|---|
| `OrderPlacedEvent` | Checkout succeeds (payment pending) | User |
| `PaymentSuccessEvent` | Payment verified by Razorpay | User |
| `PaymentFailedEvent` 🆕 | Payment attempt fails (retry count included) | User |
| `OrderConfirmedEvent` 🆕 | Payment success + shipment created | User (tracking number + ETA) |
| `OrderCancelledEvent` 🆕 | User or admin cancels order | User (admin cancel: apology + refund notice) |
| `ShipmentCreatedEvent` 🆕 | Shipment record created after payment | Admin/Fulfillment team |
| `ShipmentAssignedEvent` 🆕 | Admin assigns delivery partner | Delivery Partner (new job alert) |
| `ShipmentPickedUpEvent` 🆕 | Partner marks parcel picked up | User ("your order is on its way!") |
| `ShipmentOutForDeliveryEvent` 🆕 | Partner starts last-mile delivery | User ("arriving today!") |
| `OrderDeliveredEvent` 🆕 | Partner confirms delivery | User (confirmation + review prompt) |
| `DeliveryFailedEvent` 🆕 | Partner marks delivery failed | User + Admin |
| `ReturnRequestedEvent` 🆕 | User requests item return (future) | Admin |
| `ReturnApprovedEvent` 🆕 | Admin approves return (future) | User |
| `ReturnRejectedEvent` 🆕 | Admin rejects return (future) | User |

> [!NOTE]
> For local testing, ensure the backend is running with valid SMTP credentials injected via environment variables (`SMTP_HOST`, `SMTP_USERNAME`, `SMTP_PASSWORD`). Email failures are logged but do not fail the main request.



## Request DTOs

### `LoginRequest`

```json
{
  "usernameOrEmail": "string",
  "password": "string"
}
```

### `SignupRequest`

```json
{
  "emailId": "string",
  "userName": "string",
  "password": "string"
}
```

### `AddToCartRequest`

```json
{
  "productId": 1,
  "quantity": 1
}
```

### `AddToWishlistRequest`

```json
{
  "productId": 1
}
```

### `AddressDTO`

```json
{
  "id": 1,
  "contactName": "string",
  "mobileNumber": "9876543210",
  "addressLine": "string",
  "city": "string",
  "state": "string",
  "postalCode": "700001",
  "country": "India",
  "isDefault": true
}
```

### `CheckoutRequestDTO`

```json
{
  "name": "string",
  "phoneNo": 9876543210,
  "email": "string",
  "address": "string",
  "addressId": 1
}
```

> [!NOTE]
> If `addressId` is provided, it takes precedence. If no `addressId` is provided, the backend falls back to the user's default saved address, and finally to the manual fields.

### `PaymentConfirmRequestDTO`

```json
{
  "orderId": 1,
  "paymentToken": "string",
  "paymentReferenceId": "string",
  "success": true,
  "razorpayPaymentId": "string",
  "razorpaySignature": "string"
}
```

### `RetryPaymentRequest`

```json
{
  "orderId": 1
}
```

### `CreateReviewRequest`

```json
{
  "productId": 1,
  "rating": 5,
  "reviewText": "string"
}
```

### `UpdateReviewRequest`

```json
{
  "rating": 5,
  "reviewText": "string"
}
```

### `CreateProductRequest` and `UpdateProductRequest`

Both `CreateProductRequest` and `UpdateProductRequest` support the `active` boolean field.
For `CreateProductRequest`, `active` defaults to `true` if not provided.

```json
{
  "name": "string",
  "price": 1000,
  "description": "string",
  "brand": "string",
  "stock": 10,
  "categoryId": 1,
  "imageUrl": "string",
  "active": true,
  "attributes": [
    {
      "keyId": 1,
      "value": "string"
    }
  ],
  "additionalImageUrls": ["string"]
}
```

### `UpdateStockRequest`

```json
{
  "stock": 10
}
```

### `InventoryAdjustmentRequest`

```json
{
  "quantityDelta": -5,
  "referenceType": "MANUAL_ADJUSTMENT",
  "referenceId": "INCIDENT-992",
  "notes": "Damaged items"
}
```

### `CreateCategoryRequest`

```json
{
  "name": "Electronics"
}
```

### `CreateAttributeKeyRequest`

```json
{
  "keyName": "RAM",
  "type": "TEXT",
  "categoryId": 1
}
```

`type` must be `"TEXT"` or `"NUMBER"`.

### `UpdateProductStatusRequest`

```json
{
  "active": true
}
```

### `UpdateOrderStatusRequest`

```json
{
  "status": "SHIPPED"
}
```

### `AmazonScrapeRequest`

```json
{
  "asins": ["B09G93C5DK", "B0BZM6985C"],
  "categoryName": "Smartphones",
  "simulatedReviews": 3,
  "simulatedOrders": 5
}
```

* `asins` (List<String>, required): A list of 10-character Amazon Standard Identification Numbers (ASINs).
* `categoryName` (String, required): Category name to assign the scraped product(s) to (automatically created if not found).
* `simulatedReviews` (int, optional): Number of fake/mock reviews to automatically generate and link for each product (default is `3`).
* `simulatedOrders` (int, optional): Number of fake/mock orders to automatically generate and link for each product (default is `5`).

> [!NOTE]
> When multiple ASINs are supplied, each scrape task will run sequentially with a **5-second gap** in between to simulate human-like behavior.

### `FlipkartScrapeRequest`

```json
{
  "fsns": ["MOBGTAGMG5GB3BD3"],
  "categoryName": "Smartphones",
  "simulatedReviews": 3,
  "simulatedOrders": 5
}
```

* `fsns` (List<String>, required): A list of Flipkart Serial Numbers (FSNs / PIDs).
* `categoryName` (String, required): Category name to assign the scraped product(s) to (automatically created if not found).
* `simulatedReviews` (int, optional): Number of fake/mock reviews to automatically generate and link for each product (default is `3`).
* `simulatedOrders` (int, optional): Number of fake/mock orders to automatically generate and link for each product (default is `5`).

> [!NOTE]
> Like ASIN scraping, multiple FSNs are processed sequentially with a **5-second delay** in between.

### `DeliveryPartnerSignupRequest`

```json
{
  "fullName": "string",
  "email": "string",
  "password": "string",
  "phoneNumber": "9876543210",
  "dateOfBirth": "2000-01-01",
  "address": "string",
  "vehicleType": "BIKE",
  "vehicleNumber": "string",
  "idType": "DRIVING_LICENSE",
  "idNumber": "string"
}
```

### `DeliveryPartnerStatusUpdateRequest`

```json
{
  "status": "APPROVED"
}
```

### `ShipmentStatusUpdateRequest`

```json
{
  "status": "PICKED_UP",
  "failureReason": "string"
}
```

### `DeliveryFeedbackRequestDTO`

```json
{
  "rating": 5,
  "comment": "string"
}
```

## Response DTOs

### `AuthResponse`

Returned by `POST /auth/login` on success.

> [!WARNING]
> The field previously named `token` is now `accessToken`. Update any code that reads `response.token`.

```json
{
  "accessToken": "<short-lived JWT — 15 min>",
  "refreshToken": "<long-lived opaque token — 7 days>",
  "tokenType": "Bearer"
}
```

### `TokenRefreshRequest`

Sent to `POST /auth/refresh`.

```json
{
  "refreshToken": "<opaque refresh token string>"
}
```

### `TokenRefreshResponse`

Returned by `POST /auth/refresh`. **Both** tokens are brand-new due to refresh token rotation.

```json
{
  "accessToken": "<new JWT — 15 min>",
  "refreshToken": "<new rotated opaque token — 7 days>",
  "tokenType": "Bearer"
}
```

> [!IMPORTANT]
> **Refresh token rotation:** every call to `/auth/refresh` invalidates the submitted refresh token and issues a completely new one. Always replace **both** stored tokens with the values from the response.

> [!CAUTION]
> **Reuse detection:** if a previously-used (invalidated) refresh token is submitted, the server detects a possible token-theft attack, immediately revokes **all** active sessions for that user, and returns `401`. The user must log in again from scratch.

### `AuthUserInfoDTO`

```json
{
  "username": "string",
  "emailId": "string",
  "role": "ROLE_USER"
}
```

### `PageResponse<T>`

```json
{
  "content": [],
  "pageNumber": 0,
  "pageSize": 12,
  "totalElements": 0,
  "totalPages": 0,
  "last": true
}
```

### `InventoryResponseDTO`

```json
{
  "id": 1,
  "productId": 50,
  "productName": "iPhone 15",
  "availableQuantity": 8,
  "reservedQuantity": 2,
  "reorderLevel": 5,
  "totalQuantity": 10,
  "version": 3,
  "updatedAt": "2026-06-13T10:00:00"
}
```

### `InventoryTransactionDTO`

```json
{
  "id": 101,
  "transactionType": "RESERVE",
  "referenceType": "ORDER",
  "referenceId": "ORD-12345",
  "quantity": 2,
  "notes": "User checkout",
  "createdAt": "2026-06-13T09:30:00"
}
```

### `InventoryAnalyticsDashboardDTO`

```json
{
  "valuation": {
    "totalValue": 150000.0,
    "reservedValue": 5000.0,
    "damagedValue": 200.0
  },
  "lowStockCount": 12,
  "outOfStockCount": 3,
  "fastMovingProducts": [
    {
      "productId": 50,
      "productName": "iPhone 15",
      "unitsConsumed": 45,
      "netUnitsSold": 42
    }
  ],
  "slowMovingProducts": [
    {
      "productId": 12,
      "productName": "Old Phone Case",
      "unitsConsumed": 0,
      "netUnitsSold": 0
    }
  ],
  "transactionSummaries": [
    {
      "transactionType": "CONSUME",
      "totalQuantity": 150
    }
  ]
}
```

### `CategoryDTO`

Returned by all `/api/admin/categories` endpoints.
Includes the attribute keys defined for the category so the product edit form can build the attributes panel without a second call.

```json
{
  "id": 1,
  "name": "Electronics",
  "attributeKeys": [
    {
      "id": 1,
      "keyName": "RAM",
      "type": "TEXT",
      "categoryId": 1,
      "categoryName": "Electronics"
    },
    {
      "id": 2,
      "keyName": "Battery",
      "type": "NUMBER",
      "categoryId": 1,
      "categoryName": "Electronics"
    }
  ]
}
```

### `AdminAttributeKeyDTO`

Returned by all `/api/admin/attribute-keys` endpoints.

```json
{
  "id": 1,
  "keyName": "RAM",
  "type": "TEXT",
  "categoryId": 1,
  "categoryName": "Electronics"
}
```

### `ProductListDTO`

```json
{
  "id": 1,
  "name": "string",
  "price": 1000,
  "imageUrl": "string",
  "rating": 4.5,
  "ratingCount": 0,
  "active": true,
  "brand": "string",
  "categoryName": "string"
}
```

### `ProductDetailDTO`

```json
{
  "id": 1,
  "name": "string",
  "fullName": "string",
  "description": "string",
  "price": 1000,
  "imageUrl": "string",
  "rating": 4.5,
  "ratingCount": 0,
  "active": true,
  "brand": "string",
  "categoryName": "string",
  "imageGallery": ["string"],
  "specifications": [
    {
      "key": "string",
      "value": "string"
    }
  ]
}
```

### `CartDTO`

```json
{
  "items": [
    {
      "itemId": 1,
      "productId": 1,
      "productName": "string",
      "imageUrl": "string",
      "price": 1000,
      "quantity": 2,
      "total": 2000
    }
  ],
  "cartTotal": 2000
}
```

### `WishlistDTO`

```json
{
  "items": [
    {
      "itemId": 1,
      "productId": 1,
      "productName": "string",
      "imageUrl": "string",
      "price": 1000,
      "rating": 4.2
    }
  ]
}
```

### `OrderResponseDTO`

Returned by `POST /api/orders/checkout` and `GET /api/orders` (list view). Intentionally lightweight.

```json
{
  "orderId": 1,
  "total": 2000,
  "status": "PENDING_PAYMENT",
  "paymentStatus": "INITIATED",
  "deliveryStatus": "PENDING",
  "createdAt": "2026-05-11T12:00:00",
  "items": [
    {
      "productId": 12,
      "productName": "string",
      "productImageUrl": "https://example.com/main.jpg",
      "price": 1000,
      "quantity": 2,
      "total": 2000
    }
  ]
}
```

### `OrderDetailDTO`

Returned **only** by `GET /api/orders/{orderId}`. Provides everything needed to render a full Order Details page. Only the order owner can access this endpoint; any other user receives a `400` error.

```json
{
  "orderId": 42,
  "orderStatus": "SHIPPED",
  "paymentStatus": "COMPLETED",
  "totalAmount": 3500,
  "createdAt": "2026-06-01T10:30:00",
  "updatedAt": "2026-06-02T08:15:00",
  "recipientName": "Riya Sharma",
  "email": "riya@example.com",
  "phoneNo": 9876543210,
  "address": "12B, MG Road, Bengaluru, Karnataka 560001",
  "deliveryPartnerId": 105,
  "deliveryPartnerName": "Arjun Kumar",
  "deliveryPartnerPhone": "9876500123",
  "items": [
    {
      "productId": 7,
      "productName": "Wireless Noise-Cancelling Headphones",
      "productImageUrl": "https://example.com/images/headphones.jpg",
      "categoryName": "Electronics",
      "quantity": 1,
      "price": 2500,
      "lineTotal": 2500
    },
    {
      "productId": 14,
      "productName": "USB-C Charging Cable",
      "productImageUrl": "https://example.com/images/cable.jpg",
      "categoryName": "Accessories",
      "quantity": 2,
      "price": 500,
      "lineTotal": 1000
    }
  ],
  "totalItems": 2,
  "grandTotal": 3500
}
```

**Notes:**
- `orderStatus` is one of: `PENDING_PAYMENT`, `PAYMENT_FAILED`, `CONFIRMED`, `PROCESSING`, `SHIPPED`, `OUT_FOR_DELIVERY`, `DELIVERED`, `DELIVERY_FAILED`, `CANCELLED`, `RETURNED`
- `paymentStatus` is one of: `INITIATED`, `SUCCESS`, `FAILED`, `SUCCESS_REQUIRES_REFUND`, `REFUNDED`
- `categoryName` on each item is `null` if the product has no category assigned
- `price` on each item is the **snapshot price captured at checkout**, not the current product price
- `grandTotal` equals `totalAmount`; both are included for frontend convenience

### `OrderDetailItemDTO`

Used inside `OrderDetailDTO.items`.

```json
{
  "productId": 7,
  "productName": "string",
  "productImageUrl": "string",
  "categoryName": "string",
  "quantity": 1,
  "price": 2500,
  "lineTotal": 2500
}
```

### `PaymentInitiateResponseDTO`

```json
{
  "orderId": 1,
  "amount": 2000,
  "currency": "INR",
  "paymentToken": "PAY_xxx",
  "paymentReferenceId": "REF_xxx",
  "paymentInitiatedAt": "2026-05-11T12:00:00"
}
```

### `ReviewDTO`

```json
{
  "id": 1,
  "productId": 1,
  "userId": 1,
  "userName": "string",
  "rating": 5,
  "reviewText": "string",
  "createdAt": "2026-05-11T12:00:00",
  "updatedAt": "2026-05-11T12:00:00"
}
```

### `ProductAdminDTO`

Returned by all admin product GET, POST, and PUT endpoints.
The `attributes` array includes `keyName` so the frontend edit form can display the attribute label without a separate lookup.

```json
{
  "id": 1,
  "name": "string",
  "description": "string",
  "price": 1000,
  "stock": 10,
  "active": true,
  "brand": "string",
  "categoryId": 1,
  "categoryName": "string",
  "rating": 4.5,
  "imageUrl": "string",
  "imageUrls": ["string"],
  "attributes": [
    {
      "keyId": 1,
      "keyName": "RAM",
      "value": "8GB"
    }
  ],
  "createdAt": "2026-05-11T12:00:00",
  "updatedAt": "2026-05-11T12:00:00"
}
```

### `AdminOrderResponseDTO`

```json
{
  "orderId": 1,
  "userName": "string",
  "email": "string",
  "address": "string",
  "phoneNo": 9876543210,
  "total": 2000,
  "status": "PLACED",
  "createdAt": "2026-05-11T12:00:00",
  "items": [
    {
      "productId": 12,
      "productName": "string",
      "productImageUrl": "https://example.com/main.jpg",
      "price": 1000,
      "quantity": 2,
      "total": 2000
    }
  ]
}
```

### `DashboardAnalyticsDTO`

```json
{
  "totalUsers": 0,
  "totalOrders": 0,
  "totalRevenue": 0,
  "totalEmailsSent": 0,
  "totalEmailsFailed": 0,
  "ordersByStatus": [
    {
      "status": "PLACED",
      "count": 0
    }
  ],
  "monthlySalesGraph": [
    {
      "month": 1,
      "totalSales": 0,
      "totalOrders": 0,
      "totalRevenue": 0
    }
  ],
  "topSellingProducts": [
    {
      "productId": 1,
      "productName": "string",
      "unitsSold": 0,
      "totalRevenue": 0,
      "rating": 0.0
    }
  ]
}
```

### `AmazonScrapeResultDTO`

```json
{
  "status": "SUCCESS",
  "asin": "B09G93C5DK",
  "message": "Product successfully scraped from Amazon India and saved to database.",
  "productId": 42,
  "productName": "Apple iPhone 13",
  "fullProductName": "Apple iPhone 13 (128GB) - Midnight",
  "priceInr": 52999,
  "category": "Smartphones",
  "mainImageUrl": "https://m.media-amazon.com/images/I/...",
  "imagesInserted": 6,
  "attributesInserted": 12,
  "reviewsSimulated": 3,
  "ordersSimulated": 5,
  "galleryImageUrls": [
    "https://m.media-amazon.com/images/I/..."
  ]
}
```

### `FlipkartScrapeResultDTO`

```json
{
  "status": "SUCCESS",
  "fsn": "MOBGTAGMG5GB3BD3",
  "message": "Product successfully scraped from Flipkart India and saved to database.",
  "productId": 42,
  "productName": "SAMSUNG Galaxy S24",
  "fullProductName": "SAMSUNG Galaxy S24 5G (Amber Yellow, 256 GB)  (8 GB RAM)",
  "priceInr": 79999,
  "category": "Smartphones",
  "mainImageUrl": "https://rukminim2.flixcart.com/image/...",
  "imagesInserted": 5,
  "attributesInserted": 10,
  "reviewsSimulated": 3,
  "ordersSimulated": 5,
  "galleryImageUrls": [
    "https://rukminim2.flixcart.com/image/..."
  ]
}
```

### `DeliveryPartnerResponseDTO`

```json
{
  "id": 1,
  "fullName": "string",
  "phoneNumber": "string",
  "vehicleType": "BIKE",
  "vehicleRegistrationNumber": "string",
  "drivingLicenseNumber": "string",
  "status": "APPROVED",
  "createdAt": "2026-06-19T10:00:00",
  "updatedAt": "2026-06-19T10:00:00",
  "approvedByAdminUsername": "admin1",
  "approvedAt": "2026-06-19T11:00:00"
}
```

### `ShipmentResponseDTO`

```json
{
  "id": 1,
  "orderId": 12,
  "deliveryPartnerId": 5,
  "status": "OUT_FOR_DELIVERY",
  "trackingNumber": "SHP-20260619-000012",
  "expectedDeliveryDate": "2026-06-22",
  "failureReason": "string",
  "createdAt": "2026-06-19T10:00:00",
  "updatedAt": "2026-06-19T10:00:00"
}
```

### `DeliveryPartnerDashboardDTO`

```json
{
  "totalAssigned": 5,
  "totalPickedUp": 2,
  "totalOutForDelivery": 3,
  "totalDelivered": 150,
  "totalFailed": 2
}
```

### `DeliveryFeedbackResponseDTO`

```json
{
  "id": 1,
  "orderId": 12,
  "rating": 5,
  "comment": "string",
  "createdAt": "2026-06-19T12:00:00"
}
```

### `AdminDeliveryFeedbackResponseDTO`

```json
{
  "id": 1,
  "orderId": 12,
  "customerId": 1,
  "customerUsername": "string",
  "customerEmail": "string",
  "deliveryPartnerId": 5,
  "deliveryPartnerName": "string",
  "rating": 5,
  "comment": "string",
  "createdAt": "2026-06-19T12:00:00"
}
```

### `DeliveryPartnerRatingSummaryDTO`

```json
{
  "deliveryPartnerId": 5,
  "averageRating": 4.8,
  "totalReviews": 100
}
```

## Enums

### `DeliveryPartnerStatus`
- `PENDING`: Initial state after registration.
- `APPROVED`: Admin approved. Partner can login and receive shipments.
- `REJECTED`: Admin rejected. Login blocked.
- `SUSPENDED`: Admin suspended. Login blocked.

### `OrderStatus`

The full lifecycle of an order — what the user sees:

| Value | User-Facing Label | Description |
|---|---|---|
| `PENDING_PAYMENT` | "Awaiting Payment" | Order created; payment not yet completed |
| `PAYMENT_FAILED` | "Payment Failed" | Payment attempt failed; user may retry (max 3×) |
| `CONFIRMED` | "Order Confirmed" | Payment succeeded; shipment being prepared |
| `PROCESSING` | "Being Prepared" | Delivery partner assigned; parcel being picked up |
| `SHIPPED` | "Shipped" | Parcel picked up and in transit |
| `OUT_FOR_DELIVERY` | "Out for Delivery" | Delivery partner is en route to customer |
| `DELIVERED` | "Delivered ✅" | Successfully delivered |
| `DELIVERY_FAILED` | "Delivery Failed" | Last-mile delivery attempt failed |
| `CANCELLED` | "Cancelled" | Cancelled by user (pre-PROCESSING) or admin (CONFIRMED only) |
| `RETURNED` | "Returned" | Parcel returned to warehouse |

> **Cancellation rules:**
> - User can cancel from `PENDING_PAYMENT` (no refund) or `CONFIRMED` (refund triggered)
> - Admin can cancel from `CONFIRMED` only (requires non-empty reason; refund triggered)
> - `PENDING_PAYMENT` orders auto-expire after 30 minutes

### `ShipmentStatus`
- `CREATED`: Initial state (unassigned to a partner).
- `ASSIGNED`: Assigned to a delivery partner. Order becomes `PROCESSING`.
- `PICKED_UP`: Partner picked up the shipment. Order becomes `SHIPPED`.
- `OUT_FOR_DELIVERY`: Shipment is on the way to the customer. Order becomes `OUT_FOR_DELIVERY`.
- `DELIVERED`: Successfully delivered. Order becomes `DELIVERED`. ✅ Terminal state.
- `DELIVERY_FAILED`: Delivery attempt failed (renamed from `FAILED`). Order becomes `DELIVERY_FAILED`.
- `RETURNED`: Shipment returned to warehouse. Order becomes `RETURNED`. ✅ Terminal state.


### `VehicleType`
- `BIKE`
- `VAN`
- `TRUCK`

## Validation Rules

### Bean validation actually enforced with `@Valid`

- `CreateReviewRequest`
  - `productId`: required, positive
  - `rating`: required, `1` to `5`
  - `reviewText`: required, `10` to `500` chars
- `UpdateReviewRequest`
  - `rating`: required, `1` to `5`
  - `reviewText`: required, `10` to `500` chars
- `CreateProductRequest`
  - `name`: required, `3` to `100` chars
  - `price`: required, positive
  - `description`: max `1000` chars
  - `brand`: max `50` chars
  - `stock`: required, `>= 0`
  - `categoryId`: required, positive
- `UpdateProductRequest`
  - same as `CreateProductRequest`
- `UpdateStockRequest`
  - `stock`: required, `>= 0`
- `UpdateProductStatusRequest`
  - `active`: required
- `RetryPaymentRequest`
  - `orderId`: required, positive
- `AmazonScrapeRequest`
  - `asins`: required, list cannot be empty
  - `categoryName`: required
- `FlipkartScrapeRequest`
  - `fsns`: required, list cannot be empty
  - `categoryName`: required
- `AddressDTO`
  - `contactName`: required
  - `mobileNumber`: required, exactly 10 digits
  - `addressLine`: required
  - `postalCode`: required, exactly 6 digits (Indian PIN code format)

### Important gaps

- `LoginRequest`, `SignupRequest`, `AddToCartRequest`, `AddToWishlistRequest`, `CheckoutRequestDTO`, `PaymentConfirmRequestDTO`, and `UpdateOrderStatusRequest` have no field-level bean validation
- `PaymentConfirmRequestDTO` is annotated with `@Valid` in controller, but its fields themselves have no validation annotations

### Business rules enforced in services

- Cart checkout fails if cart is empty
- Cart checkout fails if any item stock is insufficient
- Review creation allowed only if user has successfully purchased that product
- One user can review a product only once
- Users can edit/delete only their own reviews
- Wishlist add rejects duplicate items
- Cart and wishlist item modification is ownership-checked
- Payment initiation/confirmation is ownership-checked
- Payment cannot be re-completed after success
- Payment failure increments retry count; max 3 retries then order must be recreated
- **Order status rules (new state machine):**
  - Order starts at `PENDING_PAYMENT` on checkout
  - Moves to `CONFIRMED` only after successful payment verification
  - User can cancel from `PENDING_PAYMENT` (no refund) or `CONFIRMED` (refund triggered)
  - Admin can cancel from `CONFIRMED` only — requires a non-empty reason via `AdminCancelOrderRequest`
  - Admin cancel of `CONFIRMED` order sets `PaymentStatus → SUCCESS_REQUIRES_REFUND`
  - `PENDING_PAYMENT` orders auto-expire and are cancelled after 30 minutes
- **Shipment → Order status sync (fully automated — no admin action needed):**
  - `ASSIGNED` → Order becomes `PROCESSING`
  - `PICKED_UP` → Order becomes `SHIPPED`
  - `OUT_FOR_DELIVERY` → Order becomes `OUT_FOR_DELIVERY`
  - `DELIVERED` → Order becomes `DELIVERED`
  - `DELIVERY_FAILED` → Order becomes `DELIVERY_FAILED`
  - `RETURNED` → Order becomes `RETURNED`
- Delivery partner login fails with `403 Forbidden` if status is `PENDING`, `REJECTED`, or `SUSPENDED`
- Delivery partners can only access and update shipments assigned to them
- **Shipment status transitions (strictly enforced):**
  - Admin: `CREATED` → `ASSIGNED` (via assign endpoint)
  - Partner: `ASSIGNED` → `PICKED_UP` → `OUT_FOR_DELIVERY` → `DELIVERED` | `DELIVERY_FAILED`
  - Admin reassignment: `ASSIGNED` or `DELIVERY_FAILED` → `ASSIGNED` (to a new or different partner)
  - Admin: `DELIVERY_FAILED` → `RETURNED`
- `DELIVERY_FAILED` shipments must include a `failureReason` when reported by partner
- Customers can only submit delivery feedback for `DELIVERED` shipments
- Delivery feedback is limited to one per order (duplicate check)
- Delivery partners view an anonymized version of their feedback (no customer info); Admins view full details


## Error Response Structure

This backend is **not fully consistent** in error formatting.

### Global exception wrapper

Many runtime and validation failures return:

```json
{
  "success": false,
  "message": "error message",
  "data": null,
  "timestamp": "2026-05-11T12:00:00"
}
```

Used by `GlobalExceptionHandler` for:

- `TokenRefreshException` -> HTTP `401` (expired, not found, or reuse-attack detected)
- `RuntimeException` -> HTTP `400`
- `MethodArgumentNotValidException` -> HTTP `400`
- generic `Exception` -> HTTP `500` with message `"Something went wrong"`

### Exceptions to the wrapper

- `ResourceNotFoundException` is annotated with HTTP `404`, but it is **not** wrapped by `GlobalExceptionHandler`
- `/auth/signup` duplicate checks return raw plain text `400` responses:
  - `"Email already in use"`
  - `"Username already in use"`
- `/auth/me` may return `401` with an empty body
- `/auth/refresh` returns `401` wrapped in `ApiResponse` for all failure cases (expired, invalid, reuse detected)
- Spring Security `401/403` responses are not customized, so protected-route auth failures may return framework-default responses instead of `ApiResponse`

## Frontend Integration Advice

- Centralize bearer token injection for all non-public routes
- Expect mixed success payloads: some endpoints return DTO objects, some return raw strings
- Expect mixed error payloads: handle both `ApiResponse` and plain/default Spring error bodies
- Use `role === "ROLE_ADMIN"` to gate admin UI
- Treat product/order status values as backend enums and avoid hardcoding alternate spellings
- For reviews, do not show write UI unless the user has purchased the product, or be ready to surface the backend rejection message

### `NotificationLogDTO`

```json
{
  "id": 1,
  "userId": 5,
  "referenceId": "ORD-102",
  "type": "ORDER_PLACED",
  "channel": "EMAIL",
  "recipient": "user@example.com",
  "subject": "Order Confirmation #102",
  "status": "SENT",
  "retryCount": 0,
  "failureReason": null,
  "createdAt": "2026-07-05T14:30:00",
  "sentAt": "2026-07-05T14:30:05"
}
```

### Token management (refresh token pattern)

Implement a single HTTP interceptor (e.g. Axios `response` interceptor) that:

1. Attaches `Authorization: Bearer <accessToken>` to every request
2. On a `401` response from **any** endpoint except `/auth/refresh`:
   - Pauses the failed request
   - Calls `POST /auth/refresh` with the stored `refreshToken`
   - If refresh succeeds → stores both new tokens and retries the original request
   - If refresh returns `401` → clears all stored tokens and redirects to `/login`
3. On `POST /auth/logout` success → clears all stored tokens and redirects to `/login`

```js
// Pseudocode — adapt to your HTTP client
api.interceptors.response.use(
  response => response,
  async error => {
    const original = error.config;
    if (error.response?.status === 401 && !original._retry
        && !original.url.includes('/auth/refresh')) {
      original._retry = true;
      try {
        const { data } = await axios.post('/auth/refresh', {
          refreshToken: getStoredRefreshToken()
        });
        storeTokens(data.accessToken, data.refreshToken); // always replace BOTH
        original.headers['Authorization'] = `Bearer ${data.accessToken}`;
        return api(original); // retry
      } catch {
        clearTokens();
        redirectToLogin();
      }
    }
    return Promise.reject(error);
  }
);
```

### OAuth2 social login callback page

Create a dedicated frontend route (e.g. `/oauth2/callback`) that:

1. Reads `accessToken` and `refreshToken` from URL query params
2. Stores both tokens in the same secure location used for email/password login
3. Clears the query string from the browser URL bar (use `history.replaceState`)
4. If an `error` query param is present instead, show a user-friendly error message
5. Redirects the user to the intended page (e.g. home or a stored `returnTo` path)

```js
// /oauth2/callback page — pseudocode
const params = new URLSearchParams(window.location.search);
const accessToken  = params.get('accessToken');
const refreshToken = params.get('refreshToken');
const error        = params.get('error');

if (error) {
  showError(error);
} else if (accessToken && refreshToken) {
  storeTokens(accessToken, refreshToken);
  history.replaceState({}, '', window.location.pathname); // clean URL
  redirectToHome();
}
```
