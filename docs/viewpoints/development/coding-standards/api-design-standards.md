# REST API Design Guidelines

## API Design Principles

### RESTful Design
- Use HTTP verbs (GET, POST, PUT, DELETE)
- Resource-oriented URL design
- Unified response format
- Appropriate HTTP status codes

### URL Design Specifications

#### Basic CRUD Operations
```
GET    /api/v1/customers                    # Get customer list
GET    /api/v1/customers/{id}               # Get specific customer
POST   /api/v1/customers                    # Create new customer
PUT    /api/v1/customers/{id}               # Update customer (complete)
PATCH  /api/v1/customers/{id}               # Update customer (partial)
DELETE /api/v1/customers/{id}               # Delete customer
```

#### Nested Resources
```
GET    /api/v1/customers/{id}/orders        # Get customer's orders
POST   /api/v1/customers/{id}/orders        # Create order for customer
GET    /api/v1/customers/{id}/addresses     # Get customer's addresses
POST   /api/v1/customers/{id}/addresses     # Add address for customer
```

#### Action Endpoints (Non-CRUD Operations)
```
POST   /api/v1/orders/{id}/cancel           # Cancel order
POST   /api/v1/orders/{id}/ship             # Ship order
POST   /api/v1/customers/{id}/activate      # Activate customer
POST   /api/v1/customers/{id}/suspend       # Suspend customer
```

## Response Format

### Success Response
```json
{
  "status": "success",
  "data": {
    "id": "123",
    "name": "Customer Name"
  },
  "meta": {
    "timestamp": "2025-01-21T10:00:00Z"
  }
}
```

### Error Response
```json
{
  "status": "error",
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Input data validation failed",
    "details": [
      {
        "field": "email",
        "message": "Email format is incorrect"
      }
    ]
  }
}
```

## Version Management

### URL Versioning
- Use `/api/v1/` format
- Backward compatibility guarantee
- Version deprecation notification mechanism

### Header Versioning
```http
Accept: application/vnd.api+json;version=1
API-Version: 1.0
```