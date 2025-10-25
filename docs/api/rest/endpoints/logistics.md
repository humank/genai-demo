# Logistics API

## Overview

The Logistics API provides endpoints for managing shipping, delivery tracking, and logistics operations. This includes shipment creation, tracking updates, and delivery management.

**Base Path**: `/api/v1/logistics`

**Authentication**: Required for most endpoints

## Endpoints

### Create Shipment

Create a new shipment for an order.

**Endpoint**: `POST /api/v1/logistics/shipments`

**Authentication**: Required

**Authorization**: ADMIN or WAREHOUSE role required

**Request Body**:
```json
{
  "orderId": "order-456",
  "warehouseId": "wh-taipei",
  "shippingMethod": "STANDARD",
  "carrier": "CHUNGHWA_POST",
  "shippingAddress": {
    "recipientName": "John Doe",
    "phone": "+886912345678",
    "street": "123 Main St",
    "city": "Taipei",
    "state": "Taiwan",
    "postalCode": "10001",
    "country": "TW"
  },
  "items": [
    {
      "productId": "prod-456",
      "productName": "Wireless Mouse",
      "quantity": 2,
      "weight": 0.2,
      "dimensions": {
        "length": 15,
        "width": 10,
        "height": 5,
        "unit": "cm"
      }
    }
  ],
  "packageWeight": 0.5,
  "packageDimensions": {
    "length": 20,
    "width": 15,
    "height": 10,
    "unit": "cm"
  }
}
```

**Validation Rules**:
- `orderId`: Required, must exist
- `shippingMethod`: Required, one of: STANDARD, EXPRESS, SAME_DAY
- `carrier`: Required, one of: CHUNGHWA_POST, KERRY_TJ, SF_EXPRESS, BLACK_CAT
- `shippingAddress`: Required with all fields
- `items`: Required, at least one item

**Success Response** (201 Created):
```json
{
  "data": {
    "shipmentId": "ship-123",
    "orderId": "order-456",
    "trackingNumber": "TW1234567890",
    "status": "PENDING",
    "carrier": "CHUNGHWA_POST",
    "shippingMethod": "STANDARD",
    "estimatedDeliveryDate": "2025-10-28T00:00:00Z",
    "shippingAddress": {
      "recipientName": "John Doe",
      "phone": "+886912345678",
      "street": "123 Main St",
      "city": "Taipei",
      "state": "Taiwan",
      "postalCode": "10001",
      "country": "TW"
    },
    "packageWeight": 0.5,
    "packageDimensions": {
      "length": 20,
      "width": 15,
      "height": 10,
      "unit": "cm"
    },
    "shippingCost": 100.00,
    "currency": "TWD",
    "createdAt": "2025-10-25T14:00:00Z"
  }
}
```

**Error Responses**:
- `400 Bad Request`: Validation errors
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Order not found
- `409 Conflict`: Shipment already exists for order

**curl Example**:
```bash
curl -X POST https://api.ecommerce.com/api/v1/logistics/shipments \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "order-456",
    "warehouseId": "wh-taipei",
    "shippingMethod": "STANDARD",
    "carrier": "CHUNGHWA_POST"
  }'
```

---

### Get Shipment by ID

Retrieve shipment details by ID.

**Endpoint**: `GET /api/v1/logistics/shipments/{shipmentId}`

**Authentication**: Required

**Authorization**: User can view own shipments, or ADMIN/WAREHOUSE role required

**Path Parameters**:
- `shipmentId`: Shipment ID

**Success Response** (200 OK):
```json
{
  "data": {
    "shipmentId": "ship-123",
    "orderId": "order-456",
    "trackingNumber": "TW1234567890",
    "status": "IN_TRANSIT",
    "carrier": "CHUNGHWA_POST",
    "shippingMethod": "STANDARD",
    "shippingAddress": {
      "recipientName": "John Doe",
      "phone": "+886912345678",
      "street": "123 Main St",
      "city": "Taipei",
      "state": "Taiwan",
      "postalCode": "10001",
      "country": "TW"
    },
    "packageWeight": 0.5,
    "shippingCost": 100.00,
    "currency": "TWD",
    "estimatedDeliveryDate": "2025-10-28T00:00:00Z",
    "actualDeliveryDate": null,
    "trackingEvents": [
      {
        "status": "PICKED_UP",
        "location": "Taipei Main Warehouse",
        "timestamp": "2025-10-25T15:00:00Z",
        "description": "Package picked up by carrier"
      },
      {
        "status": "IN_TRANSIT",
        "location": "Taipei Distribution Center",
        "timestamp": "2025-10-25T18:00:00Z",
        "description": "Package in transit"
      }
    ],
    "createdAt": "2025-10-25T14:00:00Z",
    "updatedAt": "2025-10-25T18:00:00Z"
  }
}
```

**Error Responses**:
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Shipment not found

**curl Example**:
```bash
curl -X GET https://api.ecommerce.com/api/v1/logistics/shipments/ship-123 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Track Shipment

Track a shipment by tracking number (public endpoint).

**Endpoint**: `GET /api/v1/logistics/track/{trackingNumber}`

**Authentication**: Not required

**Path Parameters**:
- `trackingNumber`: Tracking number

**Success Response** (200 OK):
```json
{
  "data": {
    "trackingNumber": "TW1234567890",
    "status": "IN_TRANSIT",
    "carrier": "CHUNGHWA_POST",
    "estimatedDeliveryDate": "2025-10-28T00:00:00Z",
    "trackingEvents": [
      {
        "status": "PICKED_UP",
        "location": "Taipei Main Warehouse",
        "timestamp": "2025-10-25T15:00:00Z",
        "description": "Package picked up by carrier"
      },
      {
        "status": "IN_TRANSIT",
        "location": "Taipei Distribution Center",
        "timestamp": "2025-10-25T18:00:00Z",
        "description": "Package in transit"
      },
      {
        "status": "OUT_FOR_DELIVERY",
        "location": "Taipei Delivery Station",
        "timestamp": "2025-10-26T08:00:00Z",
        "description": "Out for delivery"
      }
    ],
    "lastUpdate": "2025-10-26T08:00:00Z"
  }
}
```

**Error Responses**:
- `404 Not Found`: Tracking number not found

**curl Example**:
```bash
curl -X GET https://api.ecommerce.com/api/v1/logistics/track/TW1234567890
```

---

### Update Shipment Status

Update shipment status and add tracking event.

**Endpoint**: `POST /api/v1/logistics/shipments/{shipmentId}/events`

**Authentication**: Required

**Authorization**: ADMIN, WAREHOUSE, or CARRIER role required

**Path Parameters**:
- `shipmentId`: Shipment ID

**Request Body**:
```json
{
  "status": "DELIVERED",
  "location": "Customer Address",
  "description": "Package delivered successfully",
  "recipientName": "John Doe",
  "signature": "base64_encoded_signature_image"
}
```

**Status Values**:
- `PENDING`: Shipment created, awaiting pickup
- `PICKED_UP`: Picked up by carrier
- `IN_TRANSIT`: In transit to destination
- `OUT_FOR_DELIVERY`: Out for delivery
- `DELIVERED`: Successfully delivered
- `FAILED_DELIVERY`: Delivery attempt failed
- `RETURNED`: Returned to sender
- `CANCELLED`: Shipment cancelled

**Success Response** (201 Created):
```json
{
  "data": {
    "shipmentId": "ship-123",
    "status": "DELIVERED",
    "trackingEvent": {
      "status": "DELIVERED",
      "location": "Customer Address",
      "timestamp": "2025-10-26T14:30:00Z",
      "description": "Package delivered successfully",
      "recipientName": "John Doe"
    },
    "actualDeliveryDate": "2025-10-26T14:30:00Z",
    "updatedAt": "2025-10-26T14:30:00Z"
  }
}
```

**Error Responses**:
- `400 Bad Request`: Invalid status transition
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Shipment not found

**curl Example**:
```bash
curl -X POST https://api.ecommerce.com/api/v1/logistics/shipments/ship-123/events \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "DELIVERED",
    "location": "Customer Address",
    "description": "Package delivered successfully"
  }'
```

---

### Get Shipments by Order

Get all shipments for an order.

**Endpoint**: `GET /api/v1/logistics/orders/{orderId}/shipments`

**Authentication**: Required

**Authorization**: User can view own order shipments, or ADMIN role required

**Path Parameters**:
- `orderId`: Order ID

**Success Response** (200 OK):
```json
{
  "data": [
    {
      "shipmentId": "ship-123",
      "trackingNumber": "TW1234567890",
      "status": "DELIVERED",
      "carrier": "CHUNGHWA_POST",
      "shippingMethod": "STANDARD",
      "estimatedDeliveryDate": "2025-10-28T00:00:00Z",
      "actualDeliveryDate": "2025-10-26T14:30:00Z",
      "createdAt": "2025-10-25T14:00:00Z"
    }
  ]
}
```

**curl Example**:
```bash
curl -X GET https://api.ecommerce.com/api/v1/logistics/orders/order-456/shipments \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### List Shipments

List shipments with filters (admin).

**Endpoint**: `GET /api/v1/logistics/shipments`

**Authentication**: Required

**Authorization**: ADMIN or WAREHOUSE role required

**Query Parameters**:
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)
- `status`: Filter by status
- `carrier`: Filter by carrier
- `warehouseId`: Filter by warehouse
- `startDate`: Filter from date (ISO 8601)
- `endDate`: Filter to date (ISO 8601)

**Success Response** (200 OK):
```json
{
  "data": {
    "content": [
      {
        "shipmentId": "ship-123",
        "orderId": "order-456",
        "trackingNumber": "TW1234567890",
        "status": "DELIVERED",
        "carrier": "CHUNGHWA_POST",
        "recipientName": "John Doe",
        "estimatedDeliveryDate": "2025-10-28T00:00:00Z",
        "actualDeliveryDate": "2025-10-26T14:30:00Z",
        "createdAt": "2025-10-25T14:00:00Z"
      }
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 150,
      "totalPages": 8
    }
  }
}
```

**curl Example**:
```bash
curl -X GET "https://api.ecommerce.com/api/v1/logistics/shipments?status=IN_TRANSIT&carrier=CHUNGHWA_POST" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Calculate Shipping Cost

Calculate shipping cost for an order.

**Endpoint**: `POST /api/v1/logistics/calculate-shipping`

**Authentication**: Required

**Request Body**:
```json
{
  "shippingMethod": "STANDARD",
  "carrier": "CHUNGHWA_POST",
  "destinationAddress": {
    "city": "Taipei",
    "state": "Taiwan",
    "postalCode": "10001",
    "country": "TW"
  },
  "packageWeight": 0.5,
  "packageDimensions": {
    "length": 20,
    "width": 15,
    "height": 10,
    "unit": "cm"
  },
  "declaredValue": 1500.00
}
```

**Success Response** (200 OK):
```json
{
  "data": {
    "shippingMethod": "STANDARD",
    "carrier": "CHUNGHWA_POST",
    "baseCost": 80.00,
    "weightSurcharge": 10.00,
    "dimensionSurcharge": 5.00,
    "insuranceFee": 5.00,
    "totalCost": 100.00,
    "currency": "TWD",
    "estimatedDeliveryDays": 3,
    "estimatedDeliveryDate": "2025-10-28T00:00:00Z"
  }
}
```

**curl Example**:
```bash
curl -X POST https://api.ecommerce.com/api/v1/logistics/calculate-shipping \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "shippingMethod": "STANDARD",
    "carrier": "CHUNGHWA_POST",
    "packageWeight": 0.5
  }'
```

---

### Get Available Carriers

Get available carriers for a destination.

**Endpoint**: `GET /api/v1/logistics/carriers`

**Authentication**: Not required

**Query Parameters**:
- `country`: Destination country code (e.g., TW)
- `city`: Destination city (optional)

**Success Response** (200 OK):
```json
{
  "data": [
    {
      "code": "CHUNGHWA_POST",
      "name": "Chunghwa Post",
      "description": "Taiwan national postal service",
      "shippingMethods": [
        {
          "code": "STANDARD",
          "name": "Standard Shipping",
          "estimatedDays": 3,
          "baseCost": 80.00
        },
        {
          "code": "EXPRESS",
          "name": "Express Shipping",
          "estimatedDays": 1,
          "baseCost": 150.00
        }
      ],
      "trackingSupported": true,
      "insuranceAvailable": true
    },
    {
      "code": "BLACK_CAT",
      "name": "Black Cat Delivery",
      "description": "Taiwan courier service",
      "shippingMethods": [
        {
          "code": "SAME_DAY",
          "name": "Same Day Delivery",
          "estimatedDays": 0,
          "baseCost": 200.00
        }
      ],
      "trackingSupported": true,
      "insuranceAvailable": true
    }
  ]
}
```

**curl Example**:
```bash
curl -X GET "https://api.ecommerce.com/api/v1/logistics/carriers?country=TW"
```

---

### Request Return Shipment

Request a return shipment for an order.

**Endpoint**: `POST /api/v1/logistics/returns`

**Authentication**: Required

**Request Body**:
```json
{
  "orderId": "order-456",
  "originalShipmentId": "ship-123",
  "reason": "DEFECTIVE",
  "description": "Product not working properly",
  "items": [
    {
      "productId": "prod-456",
      "quantity": 1
    }
  ]
}
```

**Reason Codes**:
- `DEFECTIVE`: Product defective
- `WRONG_ITEM`: Wrong item received
- `NOT_AS_DESCRIBED`: Not as described
- `CHANGED_MIND`: Customer changed mind
- `OTHER`: Other reason

**Success Response** (201 Created):
```json
{
  "data": {
    "returnId": "ret-123",
    "orderId": "order-456",
    "status": "PENDING_APPROVAL",
    "reason": "DEFECTIVE",
    "description": "Product not working properly",
    "returnShipment": {
      "shipmentId": "ship-ret-123",
      "trackingNumber": "TW-RET-1234567890",
      "carrier": "CHUNGHWA_POST",
      "pickupAddress": {
        "recipientName": "John Doe",
        "phone": "+886912345678",
        "street": "123 Main St",
        "city": "Taipei",
        "postalCode": "10001"
      },
      "estimatedPickupDate": "2025-10-27T00:00:00Z"
    },
    "createdAt": "2025-10-26T15:00:00Z"
  }
}
```

**curl Example**:
```bash
curl -X POST https://api.ecommerce.com/api/v1/logistics/returns \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "order-456",
    "reason": "DEFECTIVE",
    "description": "Product not working properly"
  }'
```

---

## Data Models

### Shipment Object

```json
{
  "shipmentId": "string",
  "orderId": "string",
  "trackingNumber": "string",
  "status": "PENDING | PICKED_UP | IN_TRANSIT | OUT_FOR_DELIVERY | DELIVERED | FAILED_DELIVERY | RETURNED | CANCELLED",
  "carrier": "string",
  "shippingMethod": "STANDARD | EXPRESS | SAME_DAY",
  "shippingAddress": {
    "recipientName": "string",
    "phone": "string",
    "street": "string",
    "city": "string",
    "state": "string",
    "postalCode": "string",
    "country": "string"
  },
  "packageWeight": "number",
  "packageDimensions": {
    "length": "number",
    "width": "number",
    "height": "number",
    "unit": "string"
  },
  "shippingCost": "number",
  "currency": "string",
  "estimatedDeliveryDate": "string (ISO 8601)",
  "actualDeliveryDate": "string (ISO 8601)",
  "trackingEvents": [
    {
      "status": "string",
      "location": "string",
      "timestamp": "string (ISO 8601)",
      "description": "string"
    }
  ],
  "createdAt": "string (ISO 8601)",
  "updatedAt": "string (ISO 8601)"
}
```

### Supported Carriers

| Code | Name | Coverage | Tracking |
|------|------|----------|----------|
| CHUNGHWA_POST | Chunghwa Post | Taiwan nationwide | Yes |
| BLACK_CAT | Black Cat Delivery | Taiwan nationwide | Yes |
| KERRY_TJ | Kerry TJ Logistics | Taiwan nationwide | Yes |
| SF_EXPRESS | SF Express | Taiwan + International | Yes |

## Business Rules

1. **Shipment Creation**: One shipment per order (or multiple for partial shipments)
2. **Tracking Updates**: Real-time updates from carrier APIs
3. **Delivery Confirmation**: Requires recipient signature or photo proof
4. **Failed Delivery**: Maximum 3 delivery attempts
5. **Return Window**: 7 days from delivery date
6. **Shipping Cost**: Calculated based on weight, dimensions, and destination
7. **Insurance**: Optional, based on declared value

## Error Codes

| Code | Description | Solution |
|------|-------------|----------|
| `LOGISTICS_SHIPMENT_NOT_FOUND` | Shipment not found | Check shipment ID |
| `LOGISTICS_TRACKING_NOT_FOUND` | Tracking number not found | Check tracking number |
| `LOGISTICS_INVALID_STATUS_TRANSITION` | Invalid status change | Check current status |
| `LOGISTICS_CARRIER_NOT_AVAILABLE` | Carrier not available for destination | Choose different carrier |
| `LOGISTICS_SHIPMENT_ALREADY_EXISTS` | Shipment already exists for order | Use existing shipment |
| `LOGISTICS_RETURN_WINDOW_EXPIRED` | Return window has expired | Contact customer service |

## Related Documentation

- [Order API](orders.md) - Order management
- [Customer API](customers.md) - Customer addresses
- [Notification API](notifications.md) - Shipping notifications

---

**Last Updated**: 2025-10-25  
**API Version**: v1
