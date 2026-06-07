# Quick API Reference - E-Commerce Backend

## Authentication
All endpoints require JWT token in `Authorization` header:
```
Authorization: Bearer {JWT_TOKEN}
```

---

## MODULE 1: ADMIN PRODUCT MANAGEMENT

### 1️⃣ Create Product
```http
POST /api/admin/products
Content-Type: application/json

{
  "name": "Product Name",
  "price": 99999,
  "description": "Description",
  "brand": "Brand",
  "stock": 50,
  "categoryId": 1,
  "imageUrl": "https://image.jpg",
  "attributes": [
    {"keyId": 1, "value": "128GB"},
    {"keyId": 2, "value": "256GB"}
  ],
  "additionalImageUrls": ["url1", "url2"]
}
```
**Response**: `201 Created` + ProductAdminDTO

---

### 2️⃣ List All Products (Admin View)
```http
GET /api/admin/products?page=0&size=10&category=Electronics&brand=Samsung&active=true&search=phone&sortBy=price&order=asc
```

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Items per page (default: 10)
- `category`: Filter by category name
- `brand`: Filter by brand
- `active`: true/false/null (null = both)
- `search`: Search in product name
- `sortBy`: createdAt, price, rating, stock (default: createdAt)
- `order`: asc or desc (default: desc)

**Response**: `200 OK` + PageResponse<ProductAdminDTO>

---

### 3️⃣ Get Product Details (Admin)
```http
GET /api/admin/products/{id}
```
**Response**: `200 OK` + ProductAdminDTO (includes inactive products)

---

### 4️⃣ Update Product
```http
PUT /api/admin/products/{id}
Content-Type: application/json

{
  "name": "Updated Name",
  "price": 99999,
  "stock": 100,
  "categoryId": 1,
  ...same fields as Create...
}
```
**Response**: `200 OK` + ProductAdminDTO

---

### 5️⃣ Update Stock Only
```http
PATCH /api/admin/products/{id}/stock
Content-Type: application/json

{
  "stock": 50
}
```
**Response**: `200 OK` + "Stock updated successfully"

---

### 6️⃣ Update Product Status
```http
PATCH /api/admin/products/{id}/status
Content-Type: application/json

{
  "active": false
}
```
**Response**: `200 OK` + "Product status updated successfully"

---

### 7️⃣ Delete Product (Soft Delete)
```http
DELETE /api/admin/products/{id}
```
Sets `active = false`. Product still exists in DB but hidden from users.
**Response**: `200 OK` + "Product deleted successfully"

---

## MODULE 2: PRODUCT REVIEWS & RATINGS

### 1️⃣ Create Review
```http
POST /api/reviews
Authorization: Bearer {USER_TOKEN}
Content-Type: application/json

{
  "productId": 1,
  "rating": 5,
  "reviewText": "Excellent product! Highly recommend."
}
```

**Validation:**
- Rating: 1-5
- Review text: 10-500 characters
- User must have purchased product (successful payment order)
- One review per product per user

**Response**: `201 Created` + ReviewDTO

---

### 2️⃣ Update Review
```http
PUT /api/reviews/{id}
Authorization: Bearer {USER_TOKEN}
Content-Type: application/json

{
  "rating": 4,
  "reviewText": "Updated review text"
}
```
**Access**: Only review owner
**Response**: `200 OK` + ReviewDTO

---

### 3️⃣ Delete Review
```http
DELETE /api/reviews/{id}
Authorization: Bearer {USER_TOKEN}
```
**Access**: Only review owner
**Response**: `200 OK` + "Review deleted successfully"

---

### 4️⃣ Get Product Reviews
```http
GET /api/reviews/product/{productId}?page=0&size=10
```

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Items per page (default: 10)

**Response**: `200 OK` + PageResponse<ReviewDTO>

**Auto-Update**: Product rating automatically updates when review is added/edited/deleted

---

## MODULE 3: ADMIN DASHBOARD ANALYTICS

### 📊 Get Dashboard Analytics
```http
GET /api/admin/analytics/dashboard
Authorization: Bearer {ADMIN_TOKEN}
```

**Response**: `200 OK`
```json
{
  "totalUsers": 1500,
  "totalOrders": 850,
  "totalRevenue": 5000000,
  "ordersByStatus": [
    {"status": "PLACED", "count": 50},
    {"status": "SHIPPED", "count": 300},
    {"status": "DELIVERED", "count": 500}
  ],
  "monthlySalesGraph": [
    {"month": 1, "totalSales": 50, "totalOrders": 100, "totalRevenue": 500000},
    {"month": 2, "totalSales": 45, "totalOrders": 95, "totalRevenue": 480000},
    ...12 months...
  ],
  "topSellingProducts": [
    {
      "productId": 1,
      "productName": "iPhone 15",
      "unitsSold": 250,
      "totalRevenue": 2500000,
      "rating": 4.5
    },
    ...top 10 products...
  ]
}
```

---

## MODULE 4: ENHANCED PAYMENT SYSTEM

### 1️⃣ Initiate Payment
```http
POST /api/payments/initiate/{orderId}
Authorization: Bearer {USER_TOKEN}
```

**Response**: `200 OK`
```json
{
  "orderId": 1,
  "amount": 99999,
  "currency": "INR",
  "paymentToken": "PAY_1234567890",
  "paymentReferenceId": "REF_XYZ123",
  "paymentInitiatedAt": "2026-05-10T10:30:45"
}
```

**Features:**
- Generates unique payment reference ID
- Records payment initiation time
- Prevents duplicate initiations for successfully paid orders

---

### 2️⃣ Confirm Payment
```http
POST /api/payments/confirm
Authorization: Bearer {USER_TOKEN}
Content-Type: application/json

{
  "orderId": 1,
  "paymentToken": "PAY_1234567890",
  "paymentReferenceId": "REF_XYZ123",
  "success": true
}
```

**On Success:**
- Reduces product inventory
- Sets payment status to SUCCESS
- Records payment completion time
- Resets retry count
- Triggers PaymentSuccessEvent

**On Failure:**
- Sets payment status to FAILED
- Increments retry count
- Allows retry up to 3 times

**Response**: `200 OK` + "Payment successful" or error message

---

### 3️⃣ Retry Payment
```http
POST /api/payments/retry
Authorization: Bearer {USER_TOKEN}
Content-Type: application/json

{
  "orderId": 1
}
```

**Features:**
- Generates new payment reference ID
- Maximum 3 retry attempts
- Useful for network failures or user-initiated retries

**Response**: `200 OK` + PaymentInitiateResponseDTO

---

## MODULE 5: KAFKA EVENT ARCHITECTURE

### 📤 Event Types (Auto-Fired)

#### 1. OrderPlacedEvent
Fired when: Order successfully confirmed
Fields: orderId, userId, userEmail, totalAmount, orderedAt, items[]

#### 2. PaymentSuccessEvent
Fired when: Payment confirmed successfully
Fields: orderId, userId, paymentReferenceId, amount, paymentCompletedAt

#### 3. ProductStockUpdatedEvent
Fired when: Product inventory changes
Fields: productId, productName, previousStock, currentStock, reason, updatedAt, isLowStock

#### 4. WishlistAddedEvent
Fired when: Item added to wishlist
Fields: wishlistItemId, userId, productId, productName, productPrice, addedAt

**Current Status**: Mock implementations (log to console)
**Future**: Replace with Kafka KafkaTemplate

---

## Common Response Codes

| Code | Meaning |
|------|---------|
| 200 | OK - Request successful |
| 201 | Created - Resource created successfully |
| 400 | Bad Request - Invalid input data |
| 401 | Unauthorized - Missing or invalid token |
| 403 | Forbidden - Insufficient permissions |
| 404 | Not Found - Resource not found |
| 409 | Conflict - Constraint violation (e.g., duplicate review) |
| 500 | Server Error - Internal server error |

---

## Error Response Format

```json
{
  "timestamp": "2026-05-10T10:30:45.123456",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed: Price must be positive"
}
```

---

## Validation Rules

### Products
- Name: 3-100 characters
- Price: Must be positive
- Stock: Non-negative (0+)
- Description: Max 1000 characters
- Brand: Max 50 characters

### Reviews
- Rating: 1-5 only
- Review text: 10-500 characters
- One per product per user

### Payments
- Order must be in INITIATED or FAILED state
- Maximum 3 retries for failed payments
- Reference ID must match

---

## Example cURL Commands

### Admin Create Product
```bash
curl -X POST http://localhost:8080/api/admin/products \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Samsung S24",
    "price": 79999,
    "stock": 100,
    "categoryId": 1,
    "brand": "Samsung"
  }'
```

### User Create Review
```bash
curl -X POST http://localhost:8080/api/reviews \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "rating": 5,
    "reviewText": "Excellent product, fast delivery!"
  }'
```

### Get Dashboard Analytics
```bash
curl -X GET http://localhost:8080/api/admin/analytics/dashboard \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### Initiate Payment
```bash
curl -X POST http://localhost:8080/api/payments/initiate/1 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### Confirm Payment
```bash
curl -X POST http://localhost:8080/api/payments/confirm \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1,
    "paymentToken": "PAY_1234567890",
    "paymentReferenceId": "REF_XYZ123",
    "success": true
  }'
```

---

## Tips & Best Practices

1. **Always include** Authorization header with valid JWT token
2. **Use pagination** for list endpoints (page=0, size=10)
3. **Validate response** status codes before processing data
4. **Handle errors** with meaningful messages to users
5. **Use filters** to reduce unnecessary data transfer
6. **Test with** both admin and user accounts
7. **Cache** dashboard analytics (expensive queries)
8. **Monitor** payment reference IDs to prevent duplicates

---

**Last Updated**: May 10, 2026
**API Version**: v1
**Status**: Production Ready ✅

