# Promotion API

## Overview

The Promotion API provides endpoints for managing promotional campaigns, discount codes, and special offers. Promotions can be applied to products, categories, or entire orders.

**Base Path**: `/api/v1/promotions`

**Authentication**: Required for management endpoints, optional for public queries

## Endpoints

### List Active Promotions

Retrieve all currently active promotions.

**Endpoint**: `GET /api/v1/promotions`

**Authentication**: Not required

**Query Parameters**:
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)
- `type`: Filter by type (PERCENTAGE, FIXED_AMOUNT, BUY_X_GET_Y, FREE_SHIPPING)
- `category`: Filter by applicable category
- `active`: Filter by active status (default: true)

**Success Response** (200 OK):
```json
{
  "data": {
    "content": [
      {
        "id": "promo-001",
        "code": "SAVE10",
        "name": "10% Off Electronics",
        "description": "Get 10% off all electronics",
        "type": "PERCENTAGE",
        "discountValue": 10.0,
        "minPurchaseAmount": 1000.00,
        "maxDiscountAmount": 500.00,
        "applicableCategories": ["electronics"],
        "startDate": "2025-10-01T00:00:00Z",
        "endDate": "2025-10-31T23:59:59Z",
        "usageLimit": 1000,
        "usageCount": 245,
        "active": true
      },
      {
        "id": "promo-002",
        "code": "FREESHIP",
        "name": "Free Shipping",
        "description": "Free shipping on orders over $500",
        "type": "FREE_SHIPPING",
        "minPurchaseAmount": 500.00,
        "startDate": "2025-10-01T00:00:00Z",
        "endDate": "2025-12-31T23:59:59Z",
        "active": true
      }
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 15,
      "totalPages": 1
    }
  }
}
```

**curl Example**:
```bash
curl -X GET "https://api.ecommerce.com/api/v1/promotions?type=PERCENTAGE&active=true"
```

---

### Get Promotion by ID

Retrieve a specific promotion by ID.

**Endpoint**: `GET /api/v1/promotions/{id}`

**Authentication**: Not required

**Path Parameters**:
- `id`: Promotion ID

**Success Response** (200 OK):
```json
{
  "data": {
    "id": "promo-001",
    "code": "SAVE10",
    "name": "10% Off Electronics",
    "description": "Get 10% off all electronics",
    "type": "PERCENTAGE",
    "discountValue": 10.0,
    "minPurchaseAmount": 1000.00,
    "maxDiscountAmount": 500.00,
    "applicableCategories": ["electronics"],
    "applicableProducts": [],
    "excludedProducts": [],
    "startDate": "2025-10-01T00:00:00Z",
    "endDate": "2025-10-31T23:59:59Z",
    "usageLimit": 1000,
    "usageCount": 245,
    "usageLimitPerCustomer": 1,
    "stackable": false,
    "active": true,
    "createdAt": "2025-09-25T10:00:00Z",
    "updatedAt": "2025-10-25T12:00:00Z"
  }
}
```

**Error Responses**:
- `404 Not Found`: Promotion not found

**curl Example**:
```bash
curl -X GET https://api.ecommerce.com/api/v1/promotions/promo-001
```

---

### Validate Promotion Code

Validate a promotion code for a specific cart or order.

**Endpoint**: `POST /api/v1/promotions/validate`

**Authentication**: Required

**Request Body**:
```json
{
  "code": "SAVE10",
  "cartId": "cart-123",
  "subtotal": 1500.00,
  "items": [
    {
      "productId": "prod-456",
      "categoryId": "cat-electronics",
      "quantity": 2,
      "unitPrice": 750.00
    }
  ]
}
```

**Success Response** (200 OK):
```json
{
  "data": {
    "valid": true,
    "promotionId": "promo-001",
    "code": "SAVE10",
    "name": "10% Off Electronics",
    "type": "PERCENTAGE",
    "discountValue": 10.0,
    "calculatedDiscount": 150.00,
    "finalAmount": 1350.00,
    "message": "Promotion applied successfully"
  }
}
```

**Validation Failed Response** (200 OK):
```json
{
  "data": {
    "valid": false,
    "code": "SAVE10",
    "reason": "MINIMUM_PURCHASE_NOT_MET",
    "message": "Minimum purchase amount of $1000 required",
    "requiredAmount": 1000.00,
    "currentAmount": 800.00
  }
}
```

**Error Responses**:
- `400 Bad Request`: Invalid request format
- `404 Not Found`: Promotion code not found

**curl Example**:
```bash
curl -X POST https://api.ecommerce.com/api/v1/promotions/validate \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "SAVE10",
    "cartId": "cart-123",
    "subtotal": 1500.00
  }'
```

---

### Create Promotion (Admin)

Create a new promotion.

**Endpoint**: `POST /api/v1/promotions`

**Authentication**: Required

**Authorization**: ADMIN or MARKETING role required

**Request Body**:
```json
{
  "code": "SUMMER2025",
  "name": "Summer Sale 2025",
  "description": "20% off summer collection",
  "type": "PERCENTAGE",
  "discountValue": 20.0,
  "minPurchaseAmount": 500.00,
  "maxDiscountAmount": 1000.00,
  "applicableCategories": ["clothing", "accessories"],
  "startDate": "2025-06-01T00:00:00Z",
  "endDate": "2025-08-31T23:59:59Z",
  "usageLimit": 5000,
  "usageLimitPerCustomer": 3,
  "stackable": false,
  "active": true
}
```

**Validation Rules**:
- `code`: Required, unique, 4-20 characters, alphanumeric
- `name`: Required, 3-100 characters
- `type`: Required, one of: PERCENTAGE, FIXED_AMOUNT, BUY_X_GET_Y, FREE_SHIPPING
- `discountValue`: Required for PERCENTAGE and FIXED_AMOUNT
- `startDate`: Required, must be future date
- `endDate`: Required, must be after startDate

**Success Response** (201 Created):
```json
{
  "data": {
    "id": "promo-003",
    "code": "SUMMER2025",
    "name": "Summer Sale 2025",
    "description": "20% off summer collection",
    "type": "PERCENTAGE",
    "discountValue": 20.0,
    "minPurchaseAmount": 500.00,
    "maxDiscountAmount": 1000.00,
    "applicableCategories": ["clothing", "accessories"],
    "startDate": "2025-06-01T00:00:00Z",
    "endDate": "2025-08-31T23:59:59Z",
    "usageLimit": 5000,
    "usageLimitPerCustomer": 3,
    "stackable": false,
    "active": true,
    "createdAt": "2025-10-25T13:00:00Z"
  }
}
```

**Error Responses**:
- `400 Bad Request`: Validation errors
- `403 Forbidden`: Insufficient permissions
- `409 Conflict`: Promotion code already exists

**curl Example**:
```bash
curl -X POST https://api.ecommerce.com/api/v1/promotions \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "SUMMER2025",
    "name": "Summer Sale 2025",
    "type": "PERCENTAGE",
    "discountValue": 20.0,
    "startDate": "2025-06-01T00:00:00Z",
    "endDate": "2025-08-31T23:59:59Z"
  }'
```

---

### Update Promotion (Admin)

Update an existing promotion.

**Endpoint**: `PUT /api/v1/promotions/{id}`

**Authentication**: Required

**Authorization**: ADMIN or MARKETING role required

**Path Parameters**:
- `id`: Promotion ID

**Request Body**:
```json
{
  "name": "Summer Sale 2025 - Extended",
  "description": "20% off summer collection - Extended!",
  "endDate": "2025-09-30T23:59:59Z",
  "usageLimit": 10000,
  "active": true
}
```

**Success Response** (200 OK):
```json
{
  "data": {
    "id": "promo-003",
    "code": "SUMMER2025",
    "name": "Summer Sale 2025 - Extended",
    "description": "20% off summer collection - Extended!",
    "endDate": "2025-09-30T23:59:59Z",
    "usageLimit": 10000,
    "updatedAt": "2025-10-25T14:00:00Z"
  }
}
```

**Business Rules**:
- Cannot change promotion code after creation
- Cannot change type after creation
- Cannot reduce usage limit below current usage count

**Error Responses**:
- `400 Bad Request`: Validation errors
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Promotion not found

**curl Example**:
```bash
curl -X PUT https://api.ecommerce.com/api/v1/promotions/promo-003 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "endDate": "2025-09-30T23:59:59Z",
    "usageLimit": 10000
  }'
```

---

### Deactivate Promotion (Admin)

Deactivate a promotion (soft delete).

**Endpoint**: `DELETE /api/v1/promotions/{id}`

**Authentication**: Required

**Authorization**: ADMIN or MARKETING role required

**Path Parameters**:
- `id`: Promotion ID

**Success Response** (204 No Content)

**Error Responses**:
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Promotion not found

**curl Example**:
```bash
curl -X DELETE https://api.ecommerce.com/api/v1/promotions/promo-003 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Get Promotion Usage Statistics (Admin)

Get usage statistics for a promotion.

**Endpoint**: `GET /api/v1/promotions/{id}/statistics`

**Authentication**: Required

**Authorization**: ADMIN or MARKETING role required

**Path Parameters**:
- `id`: Promotion ID

**Success Response** (200 OK):
```json
{
  "data": {
    "promotionId": "promo-001",
    "code": "SAVE10",
    "usageCount": 245,
    "usageLimit": 1000,
    "usagePercentage": 24.5,
    "totalDiscountAmount": 12250.00,
    "totalOrderValue": 122500.00,
    "averageDiscountPerOrder": 50.00,
    "uniqueCustomers": 230,
    "topCustomers": [
      {
        "customerId": "cust-123",
        "usageCount": 3,
        "totalDiscount": 450.00
      }
    ],
    "usageByDate": [
      {
        "date": "2025-10-25",
        "usageCount": 15,
        "totalDiscount": 750.00
      }
    ]
  }
}
```

**curl Example**:
```bash
curl -X GET https://api.ecommerce.com/api/v1/promotions/promo-001/statistics \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Get Customer Promotion History

Get promotion usage history for the authenticated customer.

**Endpoint**: `GET /api/v1/promotions/me/history`

**Authentication**: Required

**Query Parameters**:
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)

**Success Response** (200 OK):
```json
{
  "data": {
    "content": [
      {
        "promotionId": "promo-001",
        "code": "SAVE10",
        "name": "10% Off Electronics",
        "orderId": "order-456",
        "discountAmount": 150.00,
        "usedAt": "2025-10-20T14:30:00Z"
      }
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 5,
      "totalPages": 1
    }
  }
}
```

**curl Example**:
```bash
curl -X GET https://api.ecommerce.com/api/v1/promotions/me/history \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## Data Models

### Promotion Object

```json
{
  "id": "string",
  "code": "string",
  "name": "string",
  "description": "string",
  "type": "PERCENTAGE | FIXED_AMOUNT | BUY_X_GET_Y | FREE_SHIPPING",
  "discountValue": "number",
  "minPurchaseAmount": "number",
  "maxDiscountAmount": "number",
  "applicableCategories": ["string"],
  "applicableProducts": ["string"],
  "excludedProducts": ["string"],
  "startDate": "string (ISO 8601)",
  "endDate": "string (ISO 8601)",
  "usageLimit": "number",
  "usageCount": "number",
  "usageLimitPerCustomer": "number",
  "stackable": "boolean",
  "active": "boolean",
  "createdAt": "string (ISO 8601)",
  "updatedAt": "string (ISO 8601)"
}
```

### Promotion Types

| Type | Description | Example |
|------|-------------|---------|
| PERCENTAGE | Percentage discount | 10% off |
| FIXED_AMOUNT | Fixed amount discount | $50 off |
| BUY_X_GET_Y | Buy X get Y free | Buy 2 get 1 free |
| FREE_SHIPPING | Free shipping | Free shipping over $500 |

## Business Rules

1. **Code Uniqueness**: Promotion codes must be unique
2. **Date Validation**: End date must be after start date
3. **Usage Limits**: Cannot exceed global or per-customer limits
4. **Stacking**: Non-stackable promotions cannot be combined
5. **Category/Product Rules**: Promotion applies only to specified items
6. **Minimum Purchase**: Order must meet minimum amount
7. **Maximum Discount**: Discount cannot exceed maximum cap
8. **Expiration**: Promotions automatically deactivate after end date

## Error Codes

| Code | Description | Solution |
|------|-------------|----------|
| `PROMOTION_NOT_FOUND` | Promotion code not found | Check code spelling |
| `PROMOTION_EXPIRED` | Promotion has expired | Use current promotion |
| `PROMOTION_NOT_STARTED` | Promotion not yet active | Wait for start date |
| `PROMOTION_USAGE_LIMIT_REACHED` | Usage limit reached | Promotion no longer available |
| `PROMOTION_CUSTOMER_LIMIT_REACHED` | Customer usage limit reached | Cannot use again |
| `PROMOTION_MINIMUM_NOT_MET` | Minimum purchase not met | Add more items |
| `PROMOTION_NOT_APPLICABLE` | Not applicable to cart items | Check eligible products |
| `PROMOTION_NOT_STACKABLE` | Cannot combine with other promotions | Remove other promotions |

## Related Documentation

- [Shopping Cart API](shopping-cart.md) - Apply promotions to cart
- [Order API](orders.md) - Promotions in orders
- [Product API](products.md) - Product categories

---

**Last Updated**: 2025-10-25  
**API Version**: v1
