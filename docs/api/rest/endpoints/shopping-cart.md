# Shopping Cart API

## Overview

The Shopping Cart API provides endpoints for managing customer shopping carts, including adding items, updating quantities, applying promotions, and checkout preparation.

**Base Path**: `/api/v1/carts`

**Authentication**: Required for all endpoints

## Endpoints

### Get Current Cart

Retrieve the authenticated user's shopping cart.

**Endpoint**: `GET /api/v1/carts/me`

**Authentication**: Required

**Success Response** (200 OK):

```json
{
  "data": {
    "id": "cart-123",
    "customerId": "cust-123",
    "items": [
      {
        "id": "item-1",
        "productId": "prod-456",
        "productName": "Wireless Mouse",
        "quantity": 2,
        "unitPrice": 599.00,
        "subtotal": 1198.00,
        "currency": "TWD"
      },
      {
        "id": "item-2",
        "productId": "prod-789",
        "productName": "USB Cable",
        "quantity": 1,
        "unitPrice": 199.00,
        "subtotal": 199.00,
        "currency": "TWD"
      }
    ],
    "subtotal": 1397.00,
    "discount": 139.70,
    "tax": 0.00,
    "total": 1257.30,
    "currency": "TWD",
    "appliedPromotions": [
      {
        "promotionId": "promo-001",
        "promotionName": "10% Off Electronics",
        "discountAmount": 139.70
      }
    ],
    "itemCount": 3,
    "createdAt": "2025-10-25T10:00:00Z",
    "updatedAt": "2025-10-25T11:30:00Z"
  }
}
```

**Error Responses**:

- `401 Unauthorized`: Missing or invalid token
- `404 Not Found`: Cart not found (empty cart)

**curl Example**:

```bash
curl -X GET https://api.ecommerce.com/api/v1/carts/me \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Add Item to Cart

Add a product to the shopping cart.

**Endpoint**: `POST /api/v1/carts/me/items`

**Authentication**: Required

**Request Body**:

```json
{
  "productId": "prod-456",
  "quantity": 2
}
```

**Validation Rules**:

- `productId`: Required, must exist
- `quantity`: Required, positive integer, max 99

**Success Response** (201 Created):

```json
{
  "data": {
    "id": "cart-123",
    "items": [
      {
        "id": "item-1",
        "productId": "prod-456",
        "productName": "Wireless Mouse",
        "quantity": 2,
        "unitPrice": 599.00,
        "subtotal": 1198.00,
        "currency": "TWD"
      }
    ],
    "subtotal": 1198.00,
    "total": 1198.00,
    "currency": "TWD",
    "itemCount": 2,
    "updatedAt": "2025-10-25T11:30:00Z"
  }
}
```

**Error Responses**:

- `400 Bad Request`: Validation errors
- `404 Not Found`: Product not found
- `409 Conflict`: Insufficient inventory

**curl Example**:

```bash
curl -X POST https://api.ecommerce.com/api/v1/carts/me/items \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "prod-456",
    "quantity": 2
  }'
```

---

### Update Cart Item Quantity

Update the quantity of an item in the cart.

**Endpoint**: `PATCH /api/v1/carts/me/items/{itemId}`

**Authentication**: Required

**Path Parameters**:

- `itemId`: Cart item ID

**Request Body**:

```json
{
  "quantity": 3
}
```

**Validation Rules**:

- `quantity`: Required, positive integer (0 to remove), max 99

**Success Response** (200 OK):

```json
{
  "data": {
    "id": "cart-123",
    "items": [
      {
        "id": "item-1",
        "productId": "prod-456",
        "productName": "Wireless Mouse",
        "quantity": 3,
        "unitPrice": 599.00,
        "subtotal": 1797.00,
        "currency": "TWD"
      }
    ],
    "subtotal": 1797.00,
    "total": 1797.00,
    "currency": "TWD",
    "itemCount": 3,
    "updatedAt": "2025-10-25T11:45:00Z"
  }
}
```

**Error Responses**:

- `400 Bad Request`: Validation errors
- `404 Not Found`: Item not found
- `409 Conflict`: Insufficient inventory

**curl Example**:

```bash
curl -X PATCH https://api.ecommerce.com/api/v1/carts/me/items/item-1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "quantity": 3
  }'
```

---

### Remove Item from Cart

Remove an item from the shopping cart.

**Endpoint**: `DELETE /api/v1/carts/me/items/{itemId}`

**Authentication**: Required

**Path Parameters**:

- `itemId`: Cart item ID

**Success Response** (204 No Content)

**Error Responses**:

- `401 Unauthorized`: Missing or invalid token
- `404 Not Found`: Item not found

**curl Example**:

```bash
curl -X DELETE https://api.ecommerce.com/api/v1/carts/me/items/item-1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Clear Cart

Remove all items from the shopping cart.

**Endpoint**: `DELETE /api/v1/carts/me`

**Authentication**: Required

**Success Response** (204 No Content)

**Error Responses**:

- `401 Unauthorized`: Missing or invalid token

**curl Example**:

```bash
curl -X DELETE https://api.ecommerce.com/api/v1/carts/me \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Apply Promotion Code

Apply a promotion code to the cart.

**Endpoint**: `POST /api/v1/carts/me/promotions`

**Authentication**: Required

**Request Body**:

```json
{
  "promotionCode": "SAVE10"
}
```

**Success Response** (200 OK):

```json
{
  "data": {
    "id": "cart-123",
    "subtotal": 1797.00,
    "discount": 179.70,
    "total": 1617.30,
    "currency": "TWD",
    "appliedPromotions": [
      {
        "promotionId": "promo-001",
        "promotionCode": "SAVE10",
        "promotionName": "10% Off",
        "discountAmount": 179.70
      }
    ],
    "updatedAt": "2025-10-25T12:00:00Z"
  }
}
```

**Error Responses**:

- `400 Bad Request`: Invalid promotion code
- `409 Conflict`: Promotion not applicable or expired

**curl Example**:

```bash
curl -X POST https://api.ecommerce.com/api/v1/carts/me/promotions \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "promotionCode": "SAVE10"
  }'
```

---

### Remove Promotion Code

Remove a promotion code from the cart.

**Endpoint**: `DELETE /api/v1/carts/me/promotions/{promotionId}`

**Authentication**: Required

**Path Parameters**:

- `promotionId`: Promotion ID

**Success Response** (200 OK):

```json
{
  "data": {
    "id": "cart-123",
    "subtotal": 1797.00,
    "discount": 0.00,
    "total": 1797.00,
    "currency": "TWD",
    "appliedPromotions": [],
    "updatedAt": "2025-10-25T12:15:00Z"
  }
}
```

**curl Example**:

```bash
curl -X DELETE https://api.ecommerce.com/api/v1/carts/me/promotions/promo-001 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Merge Carts

Merge anonymous cart with authenticated user's cart (used after login).

**Endpoint**: `POST /api/v1/carts/me/merge`

**Authentication**: Required

**Request Body**:

```json
{
  "anonymousCartId": "cart-anon-456"
}
```

**Success Response** (200 OK):

```json
{
  "data": {
    "id": "cart-123",
    "items": [
      {
        "id": "item-1",
        "productId": "prod-456",
        "quantity": 5,
        "unitPrice": 599.00,
        "subtotal": 2995.00
      }
    ],
    "subtotal": 2995.00,
    "total": 2995.00,
    "currency": "TWD",
    "itemCount": 5,
    "mergedFrom": "cart-anon-456",
    "updatedAt": "2025-10-25T12:30:00Z"
  }
}
```

**Business Rules**:

- Duplicate items are merged (quantities added)
- Anonymous cart is deleted after merge
- Promotions from anonymous cart are validated

**curl Example**:

```bash
curl -X POST https://api.ecommerce.com/api/v1/carts/me/merge \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "anonymousCartId": "cart-anon-456"
  }'
```

---

### Validate Cart for Checkout

Validate cart items and calculate final totals before checkout.

**Endpoint**: `POST /api/v1/carts/me/validate`

**Authentication**: Required

**Success Response** (200 OK):

```json
{
  "data": {
    "valid": true,
    "items": [
      {
        "productId": "prod-456",
        "available": true,
        "stockQuantity": 50,
        "requestedQuantity": 2
      }
    ],
    "subtotal": 1198.00,
    "discount": 119.80,
    "shippingFee": 100.00,
    "tax": 0.00,
    "total": 1178.20,
    "currency": "TWD",
    "warnings": [],
    "errors": []
  }
}
```

**Validation Response with Issues** (200 OK):

```json
{
  "data": {
    "valid": false,
    "items": [
      {
        "productId": "prod-456",
        "available": true,
        "stockQuantity": 1,
        "requestedQuantity": 2
      }
    ],
    "warnings": [
      {
        "code": "INSUFFICIENT_STOCK",
        "message": "Only 1 unit available for Wireless Mouse",
        "productId": "prod-456"
      }
    ],
    "errors": []
  }
}
```

**curl Example**:

```bash
curl -X POST https://api.ecommerce.com/api/v1/carts/me/validate \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## Data Models

### Cart Object

```json
{
  "id": "string",
  "customerId": "string",
  "items": [
    {
      "id": "string",
      "productId": "string",
      "productName": "string",
      "quantity": "number",
      "unitPrice": "number",
      "subtotal": "number",
      "currency": "string"
    }
  ],
  "subtotal": "number",
  "discount": "number",
  "shippingFee": "number",
  "tax": "number",
  "total": "number",
  "currency": "string",
  "appliedPromotions": [
    {
      "promotionId": "string",
      "promotionCode": "string",
      "promotionName": "string",
      "discountAmount": "number"
    }
  ],
  "itemCount": "number",
  "createdAt": "string (ISO 8601)",
  "updatedAt": "string (ISO 8601)"
}
```

## Business Rules

1. **Cart Expiration**: Anonymous carts expire after 30 days
2. **Authenticated Cart**: Persists until explicitly cleared
3. **Quantity Limits**: Maximum 99 units per item
4. **Stock Validation**: Real-time inventory check on add/update
5. **Promotion Stacking**: Multiple promotions can be applied based on rules
6. **Price Updates**: Prices are recalculated on each cart retrieval
7. **Merge Logic**: Duplicate items have quantities summed

## Error Codes

| Code | Description | Solution |
|------|-------------|----------|
| `CART_ITEM_NOT_FOUND` | Cart item not found | Check item ID |
| `CART_INSUFFICIENT_STOCK` | Not enough inventory | Reduce quantity |
| `CART_INVALID_QUANTITY` | Invalid quantity value | Use positive integer 1-99 |
| `CART_PRODUCT_NOT_FOUND` | Product not found | Check product ID |
| `CART_PROMOTION_INVALID` | Invalid promotion code | Check code and expiration |
| `CART_PROMOTION_NOT_APPLICABLE` | Promotion not applicable | Check promotion conditions |

## Related Documentation

- [Product API](products.md) - Product information
- [Promotion API](promotions.md) - Promotion management
- [Order API](orders.md) - Checkout and order creation
- [Inventory API](inventory.md) - Stock availability

---

**Last Updated**: 2025-10-25  
**API Version**: v1
