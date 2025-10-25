# Inventory API

## Overview

The Inventory API provides endpoints for managing product stock levels, reservations, and warehouse operations. This API is primarily used by internal systems and administrators.

**Base Path**: `/api/v1/inventory`

**Authentication**: Required for all endpoints

**Authorization**: Most endpoints require ADMIN or WAREHOUSE role

## Endpoints

### Check Product Availability

Check stock availability for a product.

**Endpoint**: `GET /api/v1/inventory/products/{productId}/availability`

**Authentication**: Not required (public endpoint)

**Path Parameters**:
- `productId`: Product ID

**Query Parameters**:
- `quantity`: Requested quantity (default: 1)
- `warehouseId`: Specific warehouse (optional)

**Success Response** (200 OK):
```json
{
  "data": {
    "productId": "prod-456",
    "available": true,
    "totalStock": 150,
    "availableStock": 145,
    "reservedStock": 5,
    "requestedQuantity": 2,
    "warehouses": [
      {
        "warehouseId": "wh-taipei",
        "warehouseName": "Taipei Main Warehouse",
        "stock": 100,
        "available": 95
      },
      {
        "warehouseId": "wh-taichung",
        "warehouseName": "Taichung Warehouse",
        "stock": 50,
        "available": 50
      }
    ],
    "estimatedRestockDate": null
  }
}
```

**Out of Stock Response** (200 OK):
```json
{
  "data": {
    "productId": "prod-789",
    "available": false,
    "totalStock": 0,
    "availableStock": 0,
    "reservedStock": 0,
    "requestedQuantity": 1,
    "estimatedRestockDate": "2025-11-01T00:00:00Z"
  }
}
```

**Error Responses**:
- `404 Not Found`: Product not found

**curl Example**:
```bash
curl -X GET "https://api.ecommerce.com/api/v1/inventory/products/prod-456/availability?quantity=2"
```

---

### Get Inventory by Product

Get detailed inventory information for a product.

**Endpoint**: `GET /api/v1/inventory/products/{productId}`

**Authentication**: Required

**Authorization**: ADMIN or WAREHOUSE role required

**Path Parameters**:
- `productId`: Product ID

**Success Response** (200 OK):
```json
{
  "data": {
    "productId": "prod-456",
    "productName": "Wireless Mouse",
    "sku": "WM-001",
    "totalStock": 150,
    "availableStock": 145,
    "reservedStock": 5,
    "inTransitStock": 50,
    "reorderPoint": 20,
    "reorderQuantity": 100,
    "warehouses": [
      {
        "warehouseId": "wh-taipei",
        "warehouseName": "Taipei Main Warehouse",
        "location": "A-12-03",
        "stock": 100,
        "reserved": 3,
        "available": 97
      },
      {
        "warehouseId": "wh-taichung",
        "warehouseName": "Taichung Warehouse",
        "location": "B-05-12",
        "stock": 50,
        "reserved": 2,
        "available": 48
      }
    ],
    "lastStockUpdate": "2025-10-25T10:00:00Z",
    "lastRestockDate": "2025-10-20T14:00:00Z",
    "nextRestockDate": "2025-11-05T00:00:00Z"
  }
}
```

**Error Responses**:
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Product not found

**curl Example**:
```bash
curl -X GET https://api.ecommerce.com/api/v1/inventory/products/prod-456 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Reserve Inventory

Reserve inventory for an order (internal use).

**Endpoint**: `POST /api/v1/inventory/reservations`

**Authentication**: Required

**Authorization**: SYSTEM or ADMIN role required

**Request Body**:
```json
{
  "orderId": "order-456",
  "items": [
    {
      "productId": "prod-456",
      "quantity": 2,
      "warehouseId": "wh-taipei"
    },
    {
      "productId": "prod-789",
      "quantity": 1,
      "warehouseId": "wh-taipei"
    }
  ],
  "expiresAt": "2025-10-25T15:00:00Z"
}
```

**Validation Rules**:
- `orderId`: Required, unique
- `items`: Required, at least one item
- `quantity`: Required, positive integer
- `expiresAt`: Optional, default 15 minutes from now

**Success Response** (201 Created):
```json
{
  "data": {
    "reservationId": "res-123",
    "orderId": "order-456",
    "status": "RESERVED",
    "items": [
      {
        "productId": "prod-456",
        "quantity": 2,
        "warehouseId": "wh-taipei",
        "reserved": true
      },
      {
        "productId": "prod-789",
        "quantity": 1,
        "warehouseId": "wh-taipei",
        "reserved": true
      }
    ],
    "createdAt": "2025-10-25T14:00:00Z",
    "expiresAt": "2025-10-25T15:00:00Z"
  }
}
```

**Partial Reservation Response** (207 Multi-Status):
```json
{
  "data": {
    "reservationId": "res-123",
    "orderId": "order-456",
    "status": "PARTIAL",
    "items": [
      {
        "productId": "prod-456",
        "quantity": 2,
        "warehouseId": "wh-taipei",
        "reserved": true
      },
      {
        "productId": "prod-789",
        "quantity": 1,
        "warehouseId": "wh-taipei",
        "reserved": false,
        "reason": "INSUFFICIENT_STOCK",
        "availableQuantity": 0
      }
    ],
    "createdAt": "2025-10-25T14:00:00Z"
  }
}
```

**Error Responses**:
- `400 Bad Request`: Validation errors
- `403 Forbidden`: Insufficient permissions
- `409 Conflict`: Insufficient stock for all items

**curl Example**:
```bash
curl -X POST https://api.ecommerce.com/api/v1/inventory/reservations \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "order-456",
    "items": [
      {
        "productId": "prod-456",
        "quantity": 2,
        "warehouseId": "wh-taipei"
      }
    ]
  }'
```

---

### Confirm Reservation

Confirm a reservation and deduct from available stock.

**Endpoint**: `POST /api/v1/inventory/reservations/{reservationId}/confirm`

**Authentication**: Required

**Authorization**: SYSTEM or ADMIN role required

**Path Parameters**:
- `reservationId`: Reservation ID

**Success Response** (200 OK):
```json
{
  "data": {
    "reservationId": "res-123",
    "orderId": "order-456",
    "status": "CONFIRMED",
    "items": [
      {
        "productId": "prod-456",
        "quantity": 2,
        "warehouseId": "wh-taipei",
        "deducted": true
      }
    ],
    "confirmedAt": "2025-10-25T14:30:00Z"
  }
}
```

**Error Responses**:
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Reservation not found
- `409 Conflict`: Reservation expired or already confirmed

**curl Example**:
```bash
curl -X POST https://api.ecommerce.com/api/v1/inventory/reservations/res-123/confirm \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Cancel Reservation

Cancel a reservation and release reserved stock.

**Endpoint**: `POST /api/v1/inventory/reservations/{reservationId}/cancel`

**Authentication**: Required

**Authorization**: SYSTEM or ADMIN role required

**Path Parameters**:
- `reservationId`: Reservation ID

**Success Response** (200 OK):
```json
{
  "data": {
    "reservationId": "res-123",
    "orderId": "order-456",
    "status": "CANCELLED",
    "items": [
      {
        "productId": "prod-456",
        "quantity": 2,
        "warehouseId": "wh-taipei",
        "released": true
      }
    ],
    "cancelledAt": "2025-10-25T14:45:00Z"
  }
}
```

**curl Example**:
```bash
curl -X POST https://api.ecommerce.com/api/v1/inventory/reservations/res-123/cancel \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Update Stock Level

Update stock level for a product in a warehouse.

**Endpoint**: `PUT /api/v1/inventory/products/{productId}/warehouses/{warehouseId}`

**Authentication**: Required

**Authorization**: ADMIN or WAREHOUSE role required

**Path Parameters**:
- `productId`: Product ID
- `warehouseId`: Warehouse ID

**Request Body**:
```json
{
  "quantity": 150,
  "operation": "SET",
  "reason": "RESTOCK",
  "notes": "Received shipment from supplier"
}
```

**Operation Types**:
- `SET`: Set absolute quantity
- `ADD`: Add to current quantity
- `SUBTRACT`: Subtract from current quantity

**Reason Codes**:
- `RESTOCK`: Inventory replenishment
- `ADJUSTMENT`: Inventory adjustment
- `DAMAGE`: Damaged goods
- `RETURN`: Customer return
- `TRANSFER`: Warehouse transfer

**Success Response** (200 OK):
```json
{
  "data": {
    "productId": "prod-456",
    "warehouseId": "wh-taipei",
    "previousQuantity": 100,
    "newQuantity": 150,
    "operation": "SET",
    "reason": "RESTOCK",
    "updatedBy": "admin-user-123",
    "updatedAt": "2025-10-25T15:00:00Z"
  }
}
```

**Error Responses**:
- `400 Bad Request`: Invalid operation or quantity
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Product or warehouse not found

**curl Example**:
```bash
curl -X PUT https://api.ecommerce.com/api/v1/inventory/products/prod-456/warehouses/wh-taipei \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "quantity": 150,
    "operation": "SET",
    "reason": "RESTOCK"
  }'
```

---

### Get Low Stock Products

Get products with stock below reorder point.

**Endpoint**: `GET /api/v1/inventory/low-stock`

**Authentication**: Required

**Authorization**: ADMIN or WAREHOUSE role required

**Query Parameters**:
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)
- `warehouseId`: Filter by warehouse (optional)

**Success Response** (200 OK):
```json
{
  "data": {
    "content": [
      {
        "productId": "prod-789",
        "productName": "USB Cable",
        "sku": "UC-001",
        "currentStock": 15,
        "reorderPoint": 20,
        "reorderQuantity": 100,
        "status": "LOW_STOCK",
        "warehouseId": "wh-taipei",
        "lastRestockDate": "2025-10-15T00:00:00Z"
      },
      {
        "productId": "prod-890",
        "productName": "HDMI Cable",
        "sku": "HC-001",
        "currentStock": 0,
        "reorderPoint": 10,
        "reorderQuantity": 50,
        "status": "OUT_OF_STOCK",
        "warehouseId": "wh-taipei",
        "lastRestockDate": "2025-10-10T00:00:00Z"
      }
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 12,
      "totalPages": 1
    }
  }
}
```

**curl Example**:
```bash
curl -X GET "https://api.ecommerce.com/api/v1/inventory/low-stock?warehouseId=wh-taipei" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Get Inventory Movement History

Get inventory movement history for a product.

**Endpoint**: `GET /api/v1/inventory/products/{productId}/movements`

**Authentication**: Required

**Authorization**: ADMIN or WAREHOUSE role required

**Path Parameters**:
- `productId`: Product ID

**Query Parameters**:
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)
- `startDate`: Filter from date (ISO 8601)
- `endDate`: Filter to date (ISO 8601)
- `warehouseId`: Filter by warehouse (optional)
- `type`: Filter by movement type (optional)

**Success Response** (200 OK):
```json
{
  "data": {
    "content": [
      {
        "id": "mov-123",
        "productId": "prod-456",
        "warehouseId": "wh-taipei",
        "type": "RESTOCK",
        "quantity": 50,
        "previousQuantity": 100,
        "newQuantity": 150,
        "reason": "RESTOCK",
        "notes": "Received shipment from supplier",
        "performedBy": "admin-user-123",
        "createdAt": "2025-10-25T15:00:00Z"
      },
      {
        "id": "mov-124",
        "productId": "prod-456",
        "warehouseId": "wh-taipei",
        "type": "SALE",
        "quantity": -2,
        "previousQuantity": 150,
        "newQuantity": 148,
        "orderId": "order-456",
        "createdAt": "2025-10-25T14:30:00Z"
      }
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 45,
      "totalPages": 3
    }
  }
}
```

**Movement Types**:
- `RESTOCK`: Inventory replenishment
- `SALE`: Order fulfillment
- `RETURN`: Customer return
- `ADJUSTMENT`: Manual adjustment
- `DAMAGE`: Damaged goods write-off
- `TRANSFER_IN`: Transfer from another warehouse
- `TRANSFER_OUT`: Transfer to another warehouse

**curl Example**:
```bash
curl -X GET "https://api.ecommerce.com/api/v1/inventory/products/prod-456/movements?startDate=2025-10-01T00:00:00Z" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## Data Models

### Inventory Object

```json
{
  "productId": "string",
  "productName": "string",
  "sku": "string",
  "totalStock": "number",
  "availableStock": "number",
  "reservedStock": "number",
  "inTransitStock": "number",
  "reorderPoint": "number",
  "reorderQuantity": "number",
  "warehouses": [
    {
      "warehouseId": "string",
      "warehouseName": "string",
      "location": "string",
      "stock": "number",
      "reserved": "number",
      "available": "number"
    }
  ],
  "lastStockUpdate": "string (ISO 8601)",
  "lastRestockDate": "string (ISO 8601)",
  "nextRestockDate": "string (ISO 8601)"
}
```

### Reservation Object

```json
{
  "reservationId": "string",
  "orderId": "string",
  "status": "RESERVED | CONFIRMED | CANCELLED | EXPIRED",
  "items": [
    {
      "productId": "string",
      "quantity": "number",
      "warehouseId": "string",
      "reserved": "boolean"
    }
  ],
  "createdAt": "string (ISO 8601)",
  "expiresAt": "string (ISO 8601)",
  "confirmedAt": "string (ISO 8601)",
  "cancelledAt": "string (ISO 8601)"
}
```

## Business Rules

1. **Stock Calculation**: Available Stock = Total Stock - Reserved Stock
2. **Reservation Expiry**: Reservations expire after 15 minutes if not confirmed
3. **Reorder Point**: Automatic alerts when stock falls below reorder point
4. **Multi-Warehouse**: Stock is tracked per warehouse
5. **Negative Stock**: Not allowed, operations fail if result would be negative
6. **Concurrent Updates**: Optimistic locking prevents race conditions
7. **Audit Trail**: All stock movements are logged

## Error Codes

| Code | Description | Solution |
|------|-------------|----------|
| `INVENTORY_INSUFFICIENT_STOCK` | Not enough stock available | Reduce quantity or wait for restock |
| `INVENTORY_RESERVATION_EXPIRED` | Reservation has expired | Create new reservation |
| `INVENTORY_RESERVATION_NOT_FOUND` | Reservation not found | Check reservation ID |
| `INVENTORY_NEGATIVE_STOCK` | Operation would result in negative stock | Check current stock level |
| `INVENTORY_PRODUCT_NOT_FOUND` | Product not found in inventory | Check product ID |
| `INVENTORY_WAREHOUSE_NOT_FOUND` | Warehouse not found | Check warehouse ID |

## Related Documentation

- [Product API](products.md) - Product information
- [Order API](orders.md) - Order processing
- [Shopping Cart API](shopping-cart.md) - Cart stock validation

---

**Last Updated**: 2025-10-25  
**API Version**: v1
