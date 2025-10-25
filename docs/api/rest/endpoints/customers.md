# Customer API

## Overview

The Customer API provides endpoints for managing customer profiles, including creation, retrieval, updates, and deletion. Customers are the primary users of the e-commerce platform.

**Base Path**: `/api/v1/customers`

**Authentication**: Required for all endpoints except registration

## Endpoints

### Create Customer (Register)

Create a new customer account.

**Endpoint**: `POST /api/v1/customers`

**Authentication**: Not required

**Request Body**:
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "SecurePassword123!",
  "phone": "+886912345678",
  "address": {
    "street": "123 Main St",
    "city": "Taipei",
    "state": "Taiwan",
    "postalCode": "10001",
    "country": "TW"
  }
}
```

**Validation Rules**:
- `name`: Required, 2-100 characters
- `email`: Required, valid email format, unique
- `password`: Required, min 8 characters, must contain uppercase, lowercase, and number
- `phone`: Optional, valid phone format
- `address`: Required object with all fields

**Success Response** (201 Created):
```json
{
  "data": {
    "id": "cust-123",
    "name": "John Doe",
    "email": "john@example.com",
    "phone": "+886912345678",
    "address": {
      "street": "123 Main St",
      "city": "Taipei",
      "state": "Taiwan",
      "postalCode": "10001",
      "country": "TW"
    },
    "membershipLevel": "STANDARD",
    "createdAt": "2025-10-25T10:30:00Z",
    "updatedAt": "2025-10-25T10:30:00Z"
  },
  "metadata": {
    "requestId": "req-abc-123",
    "timestamp": "2025-10-25T10:30:00Z",
    "version": "v1"
  }
}
```

**Error Responses**:
- `400 Bad Request`: Validation errors
- `409 Conflict`: Email already registered

**curl Example**:
```bash
curl -X POST https://api.ecommerce.com/api/v1/customers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "password": "SecurePassword123!",
    "phone": "+886912345678",
    "address": {
      "street": "123 Main St",
      "city": "Taipei",
      "state": "Taiwan",
      "postalCode": "10001",
      "country": "TW"
    }
  }'
```

---

### Get Customer by ID

Retrieve a specific customer by their ID.

**Endpoint**: `GET /api/v1/customers/{id}`

**Authentication**: Required

**Authorization**: User can access own profile, or ADMIN role required

**Path Parameters**:
- `id`: Customer ID (e.g., `cust-123`)

**Success Response** (200 OK):
```json
{
  "data": {
    "id": "cust-123",
    "name": "John Doe",
    "email": "john@example.com",
    "phone": "+886912345678",
    "address": {
      "street": "123 Main St",
      "city": "Taipei",
      "state": "Taiwan",
      "postalCode": "10001",
      "country": "TW"
    },
    "membershipLevel": "PREMIUM",
    "loyaltyPoints": 1500,
    "createdAt": "2025-10-25T10:30:00Z",
    "updatedAt": "2025-10-25T11:00:00Z"
  },
  "metadata": {
    "requestId": "req-abc-123",
    "timestamp": "2025-10-25T11:00:00Z",
    "version": "v1"
  }
}
```

**Error Responses**:
- `401 Unauthorized`: Missing or invalid token
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Customer not found

**curl Example**:
```bash
curl -X GET https://api.ecommerce.com/api/v1/customers/cust-123 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Get Current Customer Profile

Retrieve the authenticated user's profile.

**Endpoint**: `GET /api/v1/customers/me`

**Authentication**: Required

**Success Response** (200 OK):
```json
{
  "data": {
    "id": "cust-123",
    "name": "John Doe",
    "email": "john@example.com",
    "phone": "+886912345678",
    "address": {
      "street": "123 Main St",
      "city": "Taipei",
      "state": "Taiwan",
      "postalCode": "10001",
      "country": "TW"
    },
    "membershipLevel": "PREMIUM",
    "loyaltyPoints": 1500,
    "preferences": {
      "newsletter": true,
      "notifications": true,
      "language": "zh-TW"
    },
    "createdAt": "2025-10-25T10:30:00Z",
    "updatedAt": "2025-10-25T11:00:00Z"
  }
}
```

**Error Responses**:
- `401 Unauthorized`: Missing or invalid token

**curl Example**:
```bash
curl -X GET https://api.ecommerce.com/api/v1/customers/me \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### List Customers

Retrieve a paginated list of customers.

**Endpoint**: `GET /api/v1/customers`

**Authentication**: Required

**Authorization**: ADMIN role required

**Query Parameters**:
- `page`: Page number (0-based, default: 0)
- `size`: Page size (default: 20, max: 100)
- `sort`: Sort field and direction (default: `createdAt,desc`)
  - Examples: `name,asc`, `email,desc`, `createdAt,desc`
- `search`: Search term (searches name and email)
- `membershipLevel`: Filter by membership level (STANDARD, PREMIUM, VIP)
- `status`: Filter by status (ACTIVE, INACTIVE, SUSPENDED)

**Success Response** (200 OK):
```json
{
  "data": {
    "content": [
      {
        "id": "cust-123",
        "name": "John Doe",
        "email": "john@example.com",
        "membershipLevel": "PREMIUM",
        "status": "ACTIVE",
        "createdAt": "2025-10-25T10:30:00Z"
      },
      {
        "id": "cust-124",
        "name": "Jane Smith",
        "email": "jane@example.com",
        "membershipLevel": "STANDARD",
        "status": "ACTIVE",
        "createdAt": "2025-10-24T09:15:00Z"
      }
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 150,
      "totalPages": 8,
      "first": true,
      "last": false
    }
  }
}
```

**Error Responses**:
- `401 Unauthorized`: Missing or invalid token
- `403 Forbidden`: Insufficient permissions (ADMIN required)

**curl Example**:
```bash
# Basic list
curl -X GET "https://api.ecommerce.com/api/v1/customers?page=0&size=20" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# With search and filters
curl -X GET "https://api.ecommerce.com/api/v1/customers?search=john&membershipLevel=PREMIUM&sort=name,asc" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Update Customer

Update customer information.

**Endpoint**: `PUT /api/v1/customers/{id}`

**Authentication**: Required

**Authorization**: User can update own profile, or ADMIN role required

**Path Parameters**:
- `id`: Customer ID

**Request Body**:
```json
{
  "name": "John Doe Updated",
  "phone": "+886987654321",
  "address": {
    "street": "456 New St",
    "city": "Taipei",
    "state": "Taiwan",
    "postalCode": "10002",
    "country": "TW"
  }
}
```

**Validation Rules**:
- `name`: Optional, 2-100 characters if provided
- `phone`: Optional, valid phone format if provided
- `address`: Optional, all fields required if provided
- `email`: Cannot be updated (use separate endpoint)

**Success Response** (200 OK):
```json
{
  "data": {
    "id": "cust-123",
    "name": "John Doe Updated",
    "email": "john@example.com",
    "phone": "+886987654321",
    "address": {
      "street": "456 New St",
      "city": "Taipei",
      "state": "Taiwan",
      "postalCode": "10002",
      "country": "TW"
    },
    "membershipLevel": "PREMIUM",
    "updatedAt": "2025-10-25T12:00:00Z"
  }
}
```

**Error Responses**:
- `400 Bad Request`: Validation errors
- `401 Unauthorized`: Missing or invalid token
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Customer not found

**curl Example**:
```bash
curl -X PUT https://api.ecommerce.com/api/v1/customers/cust-123 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe Updated",
    "phone": "+886987654321",
    "address": {
      "street": "456 New St",
      "city": "Taipei",
      "state": "Taiwan",
      "postalCode": "10002",
      "country": "TW"
    }
  }'
```

---

### Partial Update Customer

Partially update customer information (only specified fields).

**Endpoint**: `PATCH /api/v1/customers/{id}`

**Authentication**: Required

**Authorization**: User can update own profile, or ADMIN role required

**Path Parameters**:
- `id`: Customer ID

**Request Body** (all fields optional):
```json
{
  "name": "John Doe Updated",
  "phone": "+886987654321"
}
```

**Success Response** (200 OK):
```json
{
  "data": {
    "id": "cust-123",
    "name": "John Doe Updated",
    "email": "john@example.com",
    "phone": "+886987654321",
    "address": {
      "street": "123 Main St",
      "city": "Taipei",
      "state": "Taiwan",
      "postalCode": "10001",
      "country": "TW"
    },
    "membershipLevel": "PREMIUM",
    "updatedAt": "2025-10-25T12:30:00Z"
  }
}
```

**Error Responses**:
- `400 Bad Request`: Validation errors
- `401 Unauthorized`: Missing or invalid token
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Customer not found

**curl Example**:
```bash
curl -X PATCH https://api.ecommerce.com/api/v1/customers/cust-123 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "+886987654321"
  }'
```

---

### Delete Customer

Delete a customer account.

**Endpoint**: `DELETE /api/v1/customers/{id}`

**Authentication**: Required

**Authorization**: User can delete own account, or ADMIN role required

**Path Parameters**:
- `id`: Customer ID

**Success Response** (204 No Content)

**Error Responses**:
- `401 Unauthorized`: Missing or invalid token
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Customer not found
- `409 Conflict`: Cannot delete customer with active orders

**curl Example**:
```bash
curl -X DELETE https://api.ecommerce.com/api/v1/customers/cust-123 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Update Customer Email

Update customer email address (requires verification).

**Endpoint**: `POST /api/v1/customers/{id}/email`

**Authentication**: Required

**Authorization**: User can update own email, or ADMIN role required

**Path Parameters**:
- `id`: Customer ID

**Request Body**:
```json
{
  "newEmail": "newemail@example.com",
  "password": "CurrentPassword123!"
}
```

**Validation Rules**:
- `newEmail`: Required, valid email format, unique
- `password`: Required for verification

**Success Response** (200 OK):
```json
{
  "data": {
    "message": "Verification email sent to newemail@example.com",
    "verificationRequired": true
  }
}
```

**Error Responses**:
- `400 Bad Request`: Validation errors
- `401 Unauthorized`: Invalid password
- `409 Conflict`: Email already in use

**curl Example**:
```bash
curl -X POST https://api.ecommerce.com/api/v1/customers/cust-123/email \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "newEmail": "newemail@example.com",
    "password": "CurrentPassword123!"
  }'
```

---

### Get Customer Orders

Retrieve orders for a specific customer.

**Endpoint**: `GET /api/v1/customers/{id}/orders`

**Authentication**: Required

**Authorization**: User can access own orders, or ADMIN role required

**Path Parameters**:
- `id`: Customer ID

**Query Parameters**:
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)
- `status`: Filter by order status (PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)

**Success Response** (200 OK):
```json
{
  "data": {
    "content": [
      {
        "id": "order-456",
        "orderNumber": "ORD-2025-001",
        "status": "DELIVERED",
        "totalAmount": 1500.00,
        "currency": "TWD",
        "itemCount": 3,
        "createdAt": "2025-10-20T10:00:00Z",
        "deliveredAt": "2025-10-23T14:30:00Z"
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
curl -X GET "https://api.ecommerce.com/api/v1/customers/cust-123/orders?status=DELIVERED" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Update Customer Preferences

Update customer preferences and settings.

**Endpoint**: `PATCH /api/v1/customers/{id}/preferences`

**Authentication**: Required

**Authorization**: User can update own preferences

**Path Parameters**:
- `id`: Customer ID

**Request Body**:
```json
{
  "newsletter": true,
  "notifications": true,
  "language": "zh-TW",
  "currency": "TWD"
}
```

**Success Response** (200 OK):
```json
{
  "data": {
    "preferences": {
      "newsletter": true,
      "notifications": true,
      "language": "zh-TW",
      "currency": "TWD"
    },
    "updatedAt": "2025-10-25T13:00:00Z"
  }
}
```

**curl Example**:
```bash
curl -X PATCH https://api.ecommerce.com/api/v1/customers/cust-123/preferences \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "newsletter": true,
    "language": "zh-TW"
  }'
```

---

## Data Models

### Customer Object

```json
{
  "id": "string",
  "name": "string",
  "email": "string",
  "phone": "string",
  "address": {
    "street": "string",
    "city": "string",
    "state": "string",
    "postalCode": "string",
    "country": "string"
  },
  "membershipLevel": "STANDARD | PREMIUM | VIP",
  "status": "ACTIVE | INACTIVE | SUSPENDED",
  "loyaltyPoints": "number",
  "preferences": {
    "newsletter": "boolean",
    "notifications": "boolean",
    "language": "string",
    "currency": "string"
  },
  "createdAt": "string (ISO 8601)",
  "updatedAt": "string (ISO 8601)"
}
```

### Membership Levels

| Level | Description | Benefits |
|-------|-------------|----------|
| STANDARD | Default membership | Basic features |
| PREMIUM | Upgraded membership | 5% discount, priority support |
| VIP | Top-tier membership | 10% discount, free shipping, exclusive access |

## Business Rules

1. **Email Uniqueness**: Each email can only be registered once
2. **Membership Upgrade**: Automatic upgrade based on purchase history
3. **Loyalty Points**: Earned on purchases, 1 point per $10 spent
4. **Account Deletion**: Cannot delete account with active orders
5. **Email Change**: Requires verification via email
6. **Password Change**: Requires current password verification

## Error Codes

| Code | Description | Solution |
|------|-------------|----------|
| `CUSTOMER_EMAIL_EXISTS` | Email already registered | Use different email or login |
| `CUSTOMER_NOT_FOUND` | Customer ID not found | Check customer ID |
| `CUSTOMER_INVALID_PASSWORD` | Password verification failed | Check current password |
| `CUSTOMER_HAS_ACTIVE_ORDERS` | Cannot delete with active orders | Cancel orders first |
| `CUSTOMER_EMAIL_VERIFICATION_REQUIRED` | Email not verified | Check email for verification link |

## Related Documentation

- [Authentication](../authentication.md) - Authentication and authorization
- [Order API](orders.md) - Order management
- [Error Handling](../error-handling.md) - Error codes and troubleshooting

---

**Last Updated**: 2025-10-25  
**API Version**: v1
