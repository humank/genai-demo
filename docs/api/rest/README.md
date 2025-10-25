# REST API Documentation

## Overview

The Enterprise E-Commerce Platform provides a comprehensive RESTful API following OpenAPI 3.0 specification. The API is designed using Domain-Driven Design (DDD) principles and organized around bounded contexts.

## API Design Principles

### RESTful Architecture

Our API follows REST (Representational State Transfer) architectural principles:

- **Resource-Based URLs**: URLs represent resources, not actions
- **Standard HTTP Methods**: GET, POST, PUT, PATCH, DELETE
- **Stateless Communication**: Each request contains all necessary information
- **Standard HTTP Status Codes**: Consistent use of status codes
- **JSON Format**: All requests and responses use JSON

### Design Guidelines

**URL Naming Conventions**:
- Use nouns, not verbs: `/customers` not `/getCustomers`
- Use plural nouns: `/customers` not `/customer`
- Use kebab-case: `/order-items` not `/orderItems`
- Nest resources logically: `/customers/{id}/orders`
- Keep URLs simple and intuitive

**HTTP Methods**:
- `GET`: Retrieve resource(s) - Safe and idempotent
- `POST`: Create new resource - Not idempotent
- `PUT`: Update entire resource - Idempotent
- `PATCH`: Partial update - Idempotent
- `DELETE`: Remove resource - Idempotent

**HTTP Status Codes**:
- `200 OK`: Successful GET, PUT, PATCH
- `201 Created`: Successful POST
- `204 No Content`: Successful DELETE
- `400 Bad Request`: Validation error, malformed request
- `401 Unauthorized`: Authentication required
- `403 Forbidden`: Authorization failed
- `404 Not Found`: Resource not found
- `409 Conflict`: Business rule violation
- `422 Unprocessable Entity`: Semantic validation error
- `500 Internal Server Error`: System error

## Base URL and Versioning

### Base URLs

**Production**:
```
https://api.ecommerce.com
```

**Staging**:
```
https://api-staging.ecommerce.com
```

**Development**:
```
http://localhost:8080
```

### API Versioning

The API uses URL-based versioning to ensure backward compatibility:

```
/api/v1/customers
/api/v2/customers
```

**Versioning Strategy**:
- Major version in URL path (`v1`, `v2`)
- Backward compatibility maintained for at least 2 versions
- Deprecation warnings provided 6 months before removal
- Breaking changes require new major version

**Deprecation Headers**:
```http
Deprecation: true
Sunset: 2026-12-31T23:59:59Z
Link: </api/v2/customers>; rel="successor-version"
```

## API Structure

### Bounded Contexts

The API is organized around the following bounded contexts:

| Context | Base Path | Description |
|---------|-----------|-------------|
| Customer | `/api/v1/customers` | Customer management and profiles |
| Order | `/api/v1/orders` | Order processing and management |
| Product | `/api/v1/products` | Product catalog and inventory |
| Shopping Cart | `/api/v1/carts` | Shopping cart operations |
| Payment | `/api/v1/payments` | Payment processing |
| Promotion | `/api/v1/promotions` | Promotions and discounts |
| Inventory | `/api/v1/inventory` | Inventory management |
| Logistics | `/api/v1/logistics` | Shipping and delivery |
| Notification | `/api/v1/notifications` | Notifications and alerts |

### Standard Response Format

All API responses follow a consistent format:

**Success Response**:
```json
{
  "data": {
    "id": "cust-123",
    "name": "John Doe",
    "email": "john@example.com"
  },
  "metadata": {
    "requestId": "req-abc-123",
    "timestamp": "2025-10-25T10:30:00Z",
    "version": "v1"
  }
}
```

**Error Response**:
```json
{
  "errors": [
    {
      "code": "VALIDATION_ERROR",
      "message": "Email format is invalid",
      "field": "email",
      "rejectedValue": "invalid-email"
    }
  ],
  "metadata": {
    "requestId": "req-abc-123",
    "timestamp": "2025-10-25T10:30:00Z",
    "version": "v1"
  }
}
```

### Pagination

List endpoints support pagination using query parameters:

**Request**:
```http
GET /api/v1/customers?page=0&size=20&sort=createdAt,desc
```

**Response**:
```json
{
  "data": {
    "content": [
      {
        "id": "cust-123",
        "name": "John Doe"
      }
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 100,
      "totalPages": 5,
      "first": true,
      "last": false
    }
  },
  "metadata": {
    "requestId": "req-abc-123",
    "timestamp": "2025-10-25T10:30:00Z",
    "version": "v1"
  }
}
```

**Pagination Parameters**:
- `page`: Page number (0-based, default: 0)
- `size`: Page size (default: 20, max: 100)
- `sort`: Sort field and direction (e.g., `createdAt,desc`)

## Interactive Documentation

### Swagger UI

Access interactive API documentation at:

**Development**:
```
http://localhost:8080/swagger-ui.html
```

**Production**:
```
https://api.ecommerce.com/swagger-ui.html
```

### OpenAPI Specification

Download the OpenAPI 3.0 specification:

**JSON Format**:
```
http://localhost:8080/v3/api-docs
```

**YAML Format**:
```
http://localhost:8080/v3/api-docs.yaml
```

## API Endpoints

### Core APIs

- [Authentication](authentication.md) - JWT-based authentication
- [Error Handling](error-handling.md) - Error codes and troubleshooting
- [Customer API](endpoints/customers.md) - Customer management
- [Order API](endpoints/orders.md) - Order processing
- [Product API](endpoints/products.md) - Product catalog
- [Payment API](endpoints/payments.md) - Payment processing

### Additional APIs

- [Shopping Cart API](endpoints/shopping-cart.md) - Cart operations
- [Promotion API](endpoints/promotions.md) - Promotions and discounts
- [Inventory API](endpoints/inventory.md) - Inventory management
- [Logistics API](endpoints/logistics.md) - Shipping and delivery
- [Notification API](endpoints/notifications.md) - Notifications

## Getting Started

### Prerequisites

- Valid API credentials (API key or JWT token)
- HTTP client (curl, Postman, or programming language HTTP library)
- Understanding of REST principles

### Quick Start

1. **Obtain Authentication Token**:
   ```bash
   curl -X POST https://api.ecommerce.com/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{
       "email": "user@example.com",
       "password": "your-password"
     }'
   ```

2. **Make Authenticated Request**:
   ```bash
   curl -X GET https://api.ecommerce.com/api/v1/customers/me \
     -H "Authorization: Bearer YOUR_JWT_TOKEN"
   ```

3. **Create a Resource**:
   ```bash
   curl -X POST https://api.ecommerce.com/api/v1/customers \
     -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{
       "name": "John Doe",
       "email": "john@example.com",
       "address": {
         "street": "123 Main St",
         "city": "Taipei",
         "postalCode": "10001"
       }
     }'
   ```

### Testing Tools

**Postman Collection**:
- Download: [Postman Collection](postman/ecommerce-api.json)
- Import into Postman for easy testing

**curl Examples**:
- See individual endpoint documentation for curl examples

**Client SDKs**:
- Generate client SDKs from OpenAPI specification
- Supported languages: Java, JavaScript, Python, Go

## Rate Limiting

API requests are rate-limited to ensure fair usage:

**Limits**:
- Authenticated users: 1000 requests per hour
- Unauthenticated users: 100 requests per hour
- Burst limit: 20 requests per second

**Rate Limit Headers**:
```http
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1635789600
```

**Rate Limit Exceeded Response**:
```http
HTTP/1.1 429 Too Many Requests
Retry-After: 3600

{
  "errors": [
    {
      "code": "RATE_LIMIT_EXCEEDED",
      "message": "Rate limit exceeded. Please try again later."
    }
  ]
}
```

## CORS Configuration

Cross-Origin Resource Sharing (CORS) is configured to allow requests from approved origins:

**Allowed Origins**:
- `https://www.ecommerce.com`
- `https://admin.ecommerce.com`
- `http://localhost:3000` (development)

**Allowed Methods**:
- GET, POST, PUT, PATCH, DELETE, OPTIONS

**Allowed Headers**:
- Authorization, Content-Type, X-Request-ID

## Best Practices

### Request Best Practices

1. **Use Appropriate HTTP Methods**: Use GET for reads, POST for creates, PUT/PATCH for updates
2. **Include Request ID**: Add `X-Request-ID` header for tracing
3. **Set Content-Type**: Always set `Content-Type: application/json`
4. **Handle Errors Gracefully**: Check status codes and handle errors appropriately
5. **Implement Retry Logic**: Use exponential backoff for retries
6. **Cache Responses**: Cache GET responses when appropriate

### Security Best Practices

1. **Use HTTPS**: Always use HTTPS in production
2. **Secure Tokens**: Store JWT tokens securely (not in localStorage)
3. **Validate Input**: Validate all input on client side
4. **Handle Sensitive Data**: Never log sensitive data
5. **Implement Timeout**: Set reasonable request timeouts
6. **Monitor API Usage**: Track API usage and errors

## Support and Resources

### Documentation

- [API Reference](endpoints/) - Detailed endpoint documentation
- [Authentication Guide](authentication.md) - Authentication and authorization
- [Error Handling](error-handling.md) - Error codes and troubleshooting
- [Postman Collection](postman/) - Ready-to-use API collection

### Support Channels

- **Email**: api-support@ecommerce.com
- **Documentation**: https://docs.ecommerce.com
- **Status Page**: https://status.ecommerce.com
- **GitHub Issues**: https://github.com/ecommerce/api/issues

### Related Documentation

- [Architecture Decision Records](../../architecture/adrs/) - API design decisions
- [Domain Events](../events/) - Event-driven architecture
- [Development Guide](../../development/) - Developer resources

---

**API Version**: v1  
**Last Updated**: 2025-10-25  
**OpenAPI Specification**: [Download](http://localhost:8080/v3/api-docs)
