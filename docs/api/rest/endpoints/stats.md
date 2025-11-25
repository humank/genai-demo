# Stats API

> **Package**: `solid.humank.genaidemo.interfaces.web.stats`
> **Base URL**: `/api/stats`

The Stats API provides system-wide statistics and reports.

## 1. General Statistics

### Get System Stats
**GET** `/api/stats`

Retrieves overall system statistics including counts of customers, orders, products, etc.

**Response Example**:
```json
{
  "customers": 150,
  "orders": 1250,
  "products": 85,
  "payments": 1180,
  "status": "success"
}
```

## 2. Order Statistics

### Get Order Status Distribution
**GET** `/api/stats/order-status`

Retrieves the distribution of orders by status (PENDING, SHIPPED, etc.).

**Response Example**:
```json
{
  "statusDistribution": {
    "PENDING": 45,
    "SHIPPED": 200,
    "DELIVERED": 350
  }
}
```

## 3. Payment Statistics

### Get Payment Method Distribution
**GET** `/api/stats/payment-methods`

Retrieves the distribution of payments by method (CREDIT_CARD, PAYPAL, etc.).

**Response Example**:
```json
{
  "paymentMethodDistribution": {
    "CREDIT_CARD": 450,
    "DIGITAL_WALLET": 320
  }
}
```
