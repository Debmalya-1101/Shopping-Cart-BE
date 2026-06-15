# Frontend Integration Summary

## Base Notes

- Base URL: use your backend host, e.g. `http://localhost:8080`
- CORS: all controllers use `@CrossOrigin(origins = "*")`
- Auth type: JWT bearer token
- Public routes:
  - `/auth/login`
  - `/auth/signup`
  - `/api/products/**`
- Protected routes: everything else
- Admin-only routes: `/api/admin/**`

## Required Headers

- For protected routes: `Authorization: Bearer <token>`
- For JSON request bodies: `Content-Type: application/json`
- No custom headers are required by the codebase

## Authentication Flow

1. `POST /auth/signup` with `emailId`, `userName`, `password`
2. `POST /auth/login` with `usernameOrEmail`, `password`
3. Store `token` from login response
4. Send `Authorization: Bearer <token>` on protected requests
5. Optionally call `GET /auth/me` to fetch the logged-in user's `username`, `emailId`, and `role`

JWT details from code:

- Token subject = username
- Token contains `role` claim like `ROLE_USER` or `ROLE_ADMIN`
- Token type returned by API = `Bearer`
- Expiry configured as `86400000 ms` (24 hours)

## REST Endpoints

### Auth

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/auth/login` | Public | `LoginRequest` | `AuthResponse` |
| POST | `/auth/signup` | Public | `SignupRequest` | `String` |
| GET | `/auth/me` | Bearer | None | `AuthUserInfoDTO` |

### Products

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| GET | `/api/products` | Public | Query params | `PageResponse<ProductListDTO>` |
| GET | `/api/products/{id}` | Public | Path param | `ProductDetailDTO` |

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

### Payments

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/api/payments/initiate/{orderId}` | Bearer | Path param | `PaymentInitiateResponseDTO` |
| POST | `/api/payments/confirm` | Bearer | `PaymentConfirmRequestDTO` | `String` |
| POST | `/api/payments/retry` | Bearer | `RetryPaymentRequest` | `PaymentInitiateResponseDTO` |

> [!WARNING]
> `POST /api/payments/retry` enforces a maximum limit of 3 retries per order. If the limit is exceeded, the endpoint will return an HTTP 500/400 error with the message "Maximum payment retries exceeded. Please create a new order."

### Reviews

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/api/reviews` | Bearer | `CreateReviewRequest` | `ReviewDTO` |
| PUT | `/api/reviews/{id}` | Bearer | `UpdateReviewRequest` | `ReviewDTO` |
| DELETE | `/api/reviews/{id}` | Bearer | Path param | `String` |
| GET | `/api/reviews/product/{productId}?page=0&size=10` | Bearer | Path + query | `PageResponse<ReviewDTO>` |

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
| PUT | `/api/admin/orders/{orderId}/status` | Admin Bearer | `UpdateOrderStatusRequest` | `AdminOrderResponseDTO` |

Supported admin order query params:

- `status` optional enum: `PLACED`, `SHIPPED`, `DELIVERED`, `CANCELLED`
- Spring pageable params such as `page`, `size`, `sort`
- Default pageable: `size=20`, `sort=createdAt,DESC`

### Admin Analytics

| Method | Path | Auth | Request | Response |
|---|---|---|---|
| GET | `/api/admin/analytics/dashboard` | Admin Bearer | None | `DashboardAnalyticsDTO` |

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
  "success": true
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

## Response DTOs

### `AuthResponse`

```json
{
  "token": "jwt",
  "tokenType": "Bearer"
}
```

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
- `orderStatus` is one of: `PLACED`, `SHIPPED`, `DELIVERED`, `CANCELLED`
- `paymentStatus` is one of: `INITIATED`, `COMPLETED`, `FAILED`
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
- Payment failure increments retry count
- Admin order status rules:
  - final states `DELIVERED` and `CANCELLED` cannot change
  - `PLACED -> DELIVERED` is rejected; it must be shipped first

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

- `RuntimeException` -> HTTP `400`
- `MethodArgumentNotValidException` -> HTTP `400`
- generic `Exception` -> HTTP `500` with message `"Something went wrong"`

### Exceptions to the wrapper

- `ResourceNotFoundException` is annotated with HTTP `404`, but it is **not** wrapped by `GlobalExceptionHandler`
- `/auth/signup` duplicate checks return raw plain text `400` responses:
  - `"Email already in use"`
  - `"Username already in use"`
- `/auth/me` may return `401` with an empty body
- Spring Security `401/403` responses are not customized, so protected-route auth failures may return framework-default responses instead of `ApiResponse`

## Frontend Integration Advice

- Centralize bearer token injection for all non-public routes
- Expect mixed success payloads: some endpoints return DTO objects, some return raw strings
- Expect mixed error payloads: handle both `ApiResponse` and plain/default Spring error bodies
- Use `role === "ROLE_ADMIN"` to gate admin UI
- Treat product/order status values as backend enums and avoid hardcoding alternate spellings
- For reviews, do not show write UI unless the user has purchased the product, or be ready to surface the backend rejection message
