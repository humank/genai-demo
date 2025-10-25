# API Usability

> **Last Updated**: 2025-10-24  
> **Status**: Active  
> **Owner**: API & Backend Team

## Overview

This document defines the API usability standards for the Enterprise E-Commerce Platform. A usable API is one that developers can understand, integrate, and use effectively with minimal friction. Good API usability reduces integration time, decreases support burden, and improves developer satisfaction.

## API Usability Principles

### Core Principles

1. **Consistency**: Predictable patterns across all endpoints
2. **Clarity**: Clear naming and obvious functionality
3. **Simplicity**: Easy to use for common cases, powerful for complex cases
4. **Discoverability**: Self-documenting and easy to explore
5. **Error Transparency**: Clear, actionable error messages
6. **Developer Empathy**: Designed from the developer's perspective

### Developer Experience Goals

| Goal | Target | Measurement |
|------|--------|-------------|
| **Time to First API Call** | < 15 minutes | Onboarding survey |
| **Time to Production Integration** | < 2 hours | Developer feedback |
| **API Documentation Satisfaction** | > 4.5/5.0 | Developer survey |
| **Error Resolution Time** | < 10 minutes | Support ticket analysis |
| **API Adoption Rate** | > 80% of partners | Usage analytics |

---

## RESTful Design Principles

### Resource-Oriented URLs

**Principle**: URLs should represent resources, not actions.

```
✅ GOOD: Resource-oriented URLs
GET    /api/v1/products              # List products
GET    /api/v1/products/{id}         # Get specific product
POST   /api/v1/products              # Create product
PUT    /api/v1/products/{id}         # Update product (full)
PATCH  /api/v1/products/{id}         # Update product (partial)
DELETE /api/v1/products/{id}         # Delete product

GET    /api/v1/customers/{id}/orders # Get customer's orders
POST   /api/v1/orders                # Create order

❌ BAD: Action-oriented URLs
GET    /api/v1/getProducts
POST   /api/v1/createProduct
POST   /api/v1/updateProduct
POST   /api/v1/deleteProduct
GET    /api/v1/getCustomerOrders?customerId=123
```

### HTTP Methods Usage

| Method | Purpose | Idempotent | Safe | Request Body | Response Body |
|--------|---------|------------|------|--------------|---------------|
| **GET** | Retrieve resource(s) | Yes | Yes | No | Yes |
| **POST** | Create resource | No | No | Yes | Yes |
| **PUT** | Replace resource | Yes | No | Yes | Yes |
| **PATCH** | Update resource partially | No | No | Yes | Yes |
| **DELETE** | Remove resource | Yes | No | No | Optional |
| **HEAD** | Get headers only | Yes | Yes | No | No |
| **OPTIONS** | Get allowed methods | Yes | Yes | No | Yes |

### HTTP Status Codes

**Success Codes**:
```
200 OK                  - Successful GET, PUT, PATCH, or DELETE
201 Created             - Successful POST that creates a resource
202 Accepted            - Request accepted for async processing
204 No Content          - Successful DELETE with no response body
```

**Client Error Codes**:
```
400 Bad Request         - Invalid request syntax or validation error
401 Unauthorized        - Authentication required or failed
403 Forbidden           - Authenticated but not authorized
404 Not Found           - Resource doesn't exist
405 Method Not Allowed  - HTTP method not supported for this endpoint
409 Conflict            - Request conflicts with current state
422 Unprocessable Entity - Semantic validation error
429 Too Many Requests   - Rate limit exceeded
```

**Server Error Codes**:
```
500 Internal Server Error - Unexpected server error
502 Bad Gateway          - Invalid response from upstream server
503 Service Unavailable  - Server temporarily unavailable
504 Gateway Timeout      - Upstream server timeout
```

---

## Request Design

### Request Headers

**Required Headers**:
```http
Content-Type: application/json
Accept: application/json
Authorization: Bearer {token}
```

**Optional Headers**:
```http
X-Request-ID: {uuid}              # For request tracing
X-Idempotency-Key: {uuid}         # For idempotent operations
X-Client-Version: 1.2.3           # Client version for compatibility
Accept-Language: en-US            # Preferred language
```

### Request Body Standards

**Good Request Body Design**:
```json
{
  "customer": {
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "phone": "+1-555-0123",
    "address": {
      "street": "123 Main St",
      "city": "San Francisco",
      "state": "CA",
      "postalCode": "94105",
      "country": "US"
    }
  },
  "preferences": {
    "newsletter": true,
    "notifications": {
      "email": true,
      "sms": false
    }
  }
}
```

**Naming Conventions**:
- Use camelCase for field names
- Use descriptive, self-explanatory names
- Avoid abbreviations unless widely understood
- Use consistent naming across all endpoints

```json
✅ GOOD:
{
  "customerId": "123",
  "orderDate": "2025-10-24T10:00:00Z",
  "totalAmount": 99.99,
  "shippingAddress": {...}
}

❌ BAD:
{
  "cust_id": "123",
  "ord_dt": "2025-10-24",
  "amt": 99.99,
  "ship_addr": {...}
}
```

### Query Parameters

**Filtering**:
```
GET /api/v1/products?category=electronics&minPrice=100&maxPrice=500
GET /api/v1/orders?status=pending&customerId=123
GET /api/v1/products?search=laptop&brand=apple
```

**Sorting**:
```
GET /api/v1/products?sortBy=price&sortOrder=asc
GET /api/v1/orders?sortBy=createdAt&sortOrder=desc
```

**Pagination**:
```
GET /api/v1/products?page=1&pageSize=20
GET /api/v1/products?offset=0&limit=20
GET /api/v1/products?cursor=eyJpZCI6MTIzfQ==
```

**Field Selection**:
```
GET /api/v1/products?fields=id,name,price
GET /api/v1/orders?include=items,customer
```

---

## Response Design

### Response Structure

**Success Response**:
```json
{
  "data": {
    "id": "ord_123456",
    "customerId": "cust_789",
    "status": "PENDING",
    "items": [
      {
        "productId": "prod_001",
        "name": "Laptop Pro",
        "quantity": 1,
        "price": {
          "amount": 1299.99,
          "currency": "USD"
        }
      }
    ],
    "totalAmount": {
      "amount": 1299.99,
      "currency": "USD"
    },
    "createdAt": "2025-10-24T10:00:00Z",
    "updatedAt": "2025-10-24T10:00:00Z"
  },
  "meta": {
    "requestId": "req_abc123",
    "timestamp": "2025-10-24T10:00:00Z"
  }
}
```

**Collection Response with Pagination**:
```json
{
  "data": [
    {
      "id": "prod_001",
      "name": "Laptop Pro",
      "price": {
        "amount": 1299.99,
        "currency": "USD"
      }
    },
    {
      "id": "prod_002",
      "name": "Mouse Wireless",
      "price": {
        "amount": 29.99,
        "currency": "USD"
      }
    }
  ],
  "pagination": {
    "page": 1,
    "pageSize": 20,
    "totalPages": 5,
    "totalItems": 95,
    "hasNext": true,
    "hasPrevious": false,
    "links": {
      "self": "/api/v1/products?page=1&pageSize=20",
      "next": "/api/v1/products?page=2&pageSize=20",
      "first": "/api/v1/products?page=1&pageSize=20",
      "last": "/api/v1/products?page=5&pageSize=20"
    }
  },
  "meta": {
    "requestId": "req_abc123",
    "timestamp": "2025-10-24T10:00:00Z"
  }
}
```

### Response Headers

**Standard Headers**:
```http
Content-Type: application/json; charset=utf-8
X-Request-ID: req_abc123
X-Response-Time: 45ms
Cache-Control: no-cache, no-store, must-revalidate
```

**Rate Limiting Headers**:
```http
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1635091200
Retry-After: 3600
```

**Pagination Headers** (Alternative to body):
```http
Link: </api/v1/products?page=2>; rel="next",
      </api/v1/products?page=1>; rel="first",
      </api/v1/products?page=5>; rel="last"
X-Total-Count: 95
X-Page-Number: 1
X-Page-Size: 20
```

---

## Error Handling

### Error Response Structure

**Standard Error Response**:
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "The request contains invalid data",
    "details": [
      {
        "field": "email",
        "message": "Email format is invalid",
        "value": "invalid-email",
        "constraint": "email"
      },
      {
        "field": "phone",
        "message": "Phone number is required",
        "constraint": "required"
      }
    ],
    "documentation": "https://docs.ecommerce.example.com/errors/validation-error",
    "requestId": "req_abc123",
    "timestamp": "2025-10-24T10:00:00Z"
  }
}
```

### Error Code Catalog

**Validation Errors (400)**:
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "The request contains invalid data",
    "details": [...]
  }
}
```

**Authentication Errors (401)**:
```json
{
  "error": {
    "code": "AUTHENTICATION_REQUIRED",
    "message": "Authentication is required to access this resource",
    "details": {
      "authenticationScheme": "Bearer",
      "realm": "api.ecommerce.example.com"
    },
    "documentation": "https://docs.ecommerce.example.com/authentication"
  }
}
```

**Authorization Errors (403)**:
```json
{
  "error": {
    "code": "INSUFFICIENT_PERMISSIONS",
    "message": "You don't have permission to perform this action",
    "details": {
      "requiredPermission": "orders:write",
      "currentPermissions": ["orders:read", "products:read"]
    },
    "documentation": "https://docs.ecommerce.example.com/authorization"
  }
}
```

**Resource Not Found (404)**:
```json
{
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "The requested resource was not found",
    "details": {
      "resourceType": "Order",
      "resourceId": "ord_123456"
    },
    "suggestions": [
      "Verify the order ID is correct",
      "Check if the order has been deleted",
      "Ensure you have access to this order"
    ]
  }
}
```

**Business Rule Violation (409)**:
```json
{
  "error": {
    "code": "INSUFFICIENT_INVENTORY",
    "message": "Cannot complete order due to insufficient inventory",
    "details": {
      "productId": "prod_001",
      "requestedQuantity": 5,
      "availableQuantity": 2
    },
    "suggestions": [
      "Reduce the quantity to 2 or less",
      "Check back later for restocking",
      "Contact support for bulk orders"
    ]
  }
}
```

**Rate Limiting (429)**:
```json
{
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "Too many requests. Please try again later.",
    "details": {
      "limit": 1000,
      "remaining": 0,
      "resetAt": "2025-10-24T11:00:00Z",
      "retryAfter": 3600
    },
    "documentation": "https://docs.ecommerce.example.com/rate-limiting"
  }
}
```

**Server Error (500)**:
```json
{
  "error": {
    "code": "INTERNAL_SERVER_ERROR",
    "message": "An unexpected error occurred. Our team has been notified.",
    "details": {
      "incidentId": "inc_789xyz"
    },
    "documentation": "https://docs.ecommerce.example.com/errors/server-error"
  }
}
```

### Error Message Guidelines

**Good Error Messages**:
- ✅ Specific and actionable
- ✅ Include what went wrong
- ✅ Suggest how to fix it
- ✅ Provide relevant context
- ✅ Link to documentation

**Bad Error Messages**:
- ❌ Generic: "An error occurred"
- ❌ Technical jargon: "NullPointerException at line 42"
- ❌ No context: "Invalid input"
- ❌ No guidance: "Error 500"

```json
✅ GOOD:
{
  "error": {
    "code": "INVALID_EMAIL_FORMAT",
    "message": "The email address 'john@' is not valid",
    "details": {
      "field": "email",
      "value": "john@",
      "expectedFormat": "user@domain.com"
    },
    "suggestions": [
      "Ensure the email includes a domain (e.g., john@example.com)",
      "Check for typos in the email address"
    ]
  }
}

❌ BAD:
{
  "error": {
    "message": "Invalid input"
  }
}
```

---

## API Documentation

### OpenAPI Specification

**Complete Endpoint Documentation**:
```yaml
openapi: 3.0.0
info:
  title: E-Commerce API
  version: 1.0.0
  description: |
    The E-Commerce API allows you to manage products, orders, and customers.
    
    ## Authentication
    All API requests require authentication using Bearer tokens.
    
    ## Rate Limiting
    API requests are limited to 1000 requests per hour per API key.
    
  contact:
    name: API Support
    email: api-support@ecommerce.example.com
    url: https://support.ecommerce.example.com

servers:
  - url: https://api.ecommerce.example.com/api/v1
    description: Production server
  - url: https://sandbox-api.ecommerce.example.com/api/v1
    description: Sandbox server

paths:
  /orders:
    post:
      summary: Create a new order
      description: |
        Creates a new order for the authenticated customer.
        
        ## Business Rules
        - Customer must have a valid payment method
        - All products must be in stock
        - Order total must be greater than $0
        
        ## Idempotency
        This endpoint supports idempotency using the `X-Idempotency-Key` header.
        
      operationId: createOrder
      tags:
        - Orders
      security:
        - bearerAuth: []
      parameters:
        - name: X-Idempotency-Key
          in: header
          description: Unique key to ensure idempotent requests
          required: false
          schema:
            type: string
            format: uuid
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateOrderRequest'
            examples:
              simple:
                summary: Simple order with one item
                value:
                  customerId: "cust_123"
                  items:
                    - productId: "prod_001"
                      quantity: 1
                  shippingAddress:
                    street: "123 Main St"
                    city: "San Francisco"
                    state: "CA"
                    postalCode: "94105"
                    country: "US"
              complex:
                summary: Order with multiple items and promo code
                value:
                  customerId: "cust_123"
                  items:
                    - productId: "prod_001"
                      quantity: 2
                    - productId: "prod_002"
                      quantity: 1
                  promoCode: "SAVE10"
                  shippingAddress:
                    street: "123 Main St"
                    city: "San Francisco"
                    state: "CA"
                    postalCode: "94105"
                    country: "US"
      responses:
        '201':
          description: Order created successfully
          headers:
            X-Request-ID:
              schema:
                type: string
              description: Unique request identifier
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/OrderResponse'
              examples:
                success:
                  summary: Successful order creation
                  value:
                    data:
                      id: "ord_123456"
                      customerId: "cust_123"
                      status: "PENDING"
                      totalAmount:
                        amount: 1299.99
                        currency: "USD"
                      createdAt: "2025-10-24T10:00:00Z"
        '400':
          description: Invalid request
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
              examples:
                validation:
                  summary: Validation error
                  value:
                    error:
                      code: "VALIDATION_ERROR"
                      message: "The request contains invalid data"
                      details:
                        - field: "items"
                          message: "At least one item is required"
        '401':
          $ref: '#/components/responses/Unauthorized'
        '409':
          description: Business rule violation
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
              examples:
                insufficient_inventory:
                  summary: Insufficient inventory
                  value:
                    error:
                      code: "INSUFFICIENT_INVENTORY"
                      message: "Cannot complete order due to insufficient inventory"
                      details:
                        productId: "prod_001"
                        requestedQuantity: 5
                        availableQuantity: 2

components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
      description: |
        JWT token obtained from the authentication endpoint.
        
        Example: `Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`
  
  schemas:
    CreateOrderRequest:
      type: object
      required:
        - customerId
        - items
        - shippingAddress
      properties:
        customerId:
          type: string
          description: Unique identifier of the customer
          example: "cust_123"
        items:
          type: array
          description: List of items to order
          minItems: 1
          items:
            $ref: '#/components/schemas/OrderItem'
        shippingAddress:
          $ref: '#/components/schemas/Address'
        promoCode:
          type: string
          description: Optional promotional code
          example: "SAVE10"
    
    OrderResponse:
      type: object
      properties:
        data:
          $ref: '#/components/schemas/Order'
        meta:
          $ref: '#/components/schemas/ResponseMeta'
    
    ErrorResponse:
      type: object
      properties:
        error:
          type: object
          properties:
            code:
              type: string
              description: Machine-readable error code
            message:
              type: string
              description: Human-readable error message
            details:
              type: object
              description: Additional error details
            documentation:
              type: string
              format: uri
              description: Link to error documentation
```

### Code Examples

**Provide examples in multiple languages**:

```javascript
// JavaScript/Node.js
const axios = require('axios');

async function createOrder() {
  try {
    const response = await axios.post(
      'https://api.ecommerce.example.com/api/v1/orders',
      {
        customerId: 'cust_123',
        items: [
          {
            productId: 'prod_001',
            quantity: 1
          }
        ],
        shippingAddress: {
          street: '123 Main St',
          city: 'San Francisco',
          state: 'CA',
          postalCode: '94105',
          country: 'US'
        }
      },
      {
        headers: {
          'Authorization': 'Bearer YOUR_API_TOKEN',
          'Content-Type': 'application/json'
        }
      }
    );
    
    console.log('Order created:', response.data);
  } catch (error) {
    if (error.response) {
      console.error('Error:', error.response.data.error);
    }
  }
}
```

```python
# Python
import requests

def create_order():
    url = 'https://api.ecommerce.example.com/api/v1/orders'
    headers = {
        'Authorization': 'Bearer YOUR_API_TOKEN',
        'Content-Type': 'application/json'
    }
    data = {
        'customerId': 'cust_123',
        'items': [
            {
                'productId': 'prod_001',
                'quantity': 1
            }
        ],
        'shippingAddress': {
            'street': '123 Main St',
            'city': 'San Francisco',
            'state': 'CA',
            'postalCode': '94105',
            'country': 'US'
        }
    }
    
    try:
        response = requests.post(url, json=data, headers=headers)
        response.raise_for_status()
        print('Order created:', response.json())
    except requests.exceptions.HTTPError as error:
        print('Error:', error.response.json()['error'])
```

```java
// Java
import java.net.http.*;
import java.net.URI;

public class OrderClient {
    public void createOrder() {
        HttpClient client = HttpClient.newHttpClient();
        
        String requestBody = """
            {
                "customerId": "cust_123",
                "items": [
                    {
                        "productId": "prod_001",
                        "quantity": 1
                    }
                ],
                "shippingAddress": {
                    "street": "123 Main St",
                    "city": "San Francisco",
                    "state": "CA",
                    "postalCode": "94105",
                    "country": "US"
                }
            }
            """;
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.ecommerce.example.com/api/v1/orders"))
            .header("Authorization", "Bearer YOUR_API_TOKEN")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
        
        try {
            HttpResponse<String> response = client.send(request, 
                HttpResponse.BodyHandlers.ofString());
            System.out.println("Order created: " + response.body());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
```

```bash
# cURL
curl -X POST https://api.ecommerce.example.com/api/v1/orders \
  -H "Authorization: Bearer YOUR_API_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "cust_123",
    "items": [
      {
        "productId": "prod_001",
        "quantity": 1
      }
    ],
    "shippingAddress": {
      "street": "123 Main St",
      "city": "San Francisco",
      "state": "CA",
      "postalCode": "94105",
      "country": "US"
    }
  }'
```

---

## API Versioning

### Version Communication

**In URL**:
```
https://api.ecommerce.example.com/api/v1/orders
https://api.ecommerce.example.com/api/v2/orders
```

**Deprecation Headers**:
```http
Deprecation: true
Sunset: 2025-12-31T23:59:59Z
Link: </api/v2/orders>; rel="successor-version"
```

**Version Information in Response**:
```json
{
  "data": {...},
  "meta": {
    "apiVersion": "1.0.0",
    "deprecationNotice": {
      "deprecated": true,
      "sunsetDate": "2025-12-31T23:59:59Z",
      "migrationGuide": "https://docs.ecommerce.example.com/migration/v1-to-v2"
    }
  }
}
```

---

## Rate Limiting

### Rate Limit Information

**Headers**:
```http
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1635091200
```

**Rate Limit Exceeded Response**:
```json
{
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "Too many requests. Please try again later.",
    "details": {
      "limit": 1000,
      "remaining": 0,
      "resetAt": "2025-10-24T11:00:00Z",
      "retryAfter": 3600
    },
    "suggestions": [
      "Wait 1 hour before making more requests",
      "Implement exponential backoff in your client",
      "Contact support to increase your rate limit"
    ],
    "documentation": "https://docs.ecommerce.example.com/rate-limiting"
  }
}
```

---

## SDK and Client Libraries

### Official SDKs

Provide official SDKs for popular languages:

```javascript
// JavaScript SDK
const EcommerceAPI = require('@ecommerce/api-client');

const client = new EcommerceAPI({
  apiKey: 'YOUR_API_KEY',
  environment: 'production' // or 'sandbox'
});

// Simple, intuitive API
const order = await client.orders.create({
  customerId: 'cust_123',
  items: [
    { productId: 'prod_001', quantity: 1 }
  ],
  shippingAddress: {
    street: '123 Main St',
    city: 'San Francisco',
    state: 'CA',
    postalCode: '94105',
    country: 'US'
  }
});

console.log('Order created:', order.id);
```

### SDK Features

- ✅ Automatic authentication
- ✅ Automatic retry with exponential backoff
- ✅ Built-in error handling
- ✅ Type definitions (TypeScript)
- ✅ Request/response logging
- ✅ Idempotency support
- ✅ Rate limit handling

---

## Testing and Sandbox

### Sandbox Environment

```
Production: https://api.ecommerce.example.com
Sandbox:    https://sandbox-api.ecommerce.example.com
```

**Sandbox Features**:
- Isolated test environment
- Test credit cards accepted
- No real charges
- Reset data daily
- Same API as production

### Test Data

**Test Credit Cards**:
```
Success: 4242 4242 4242 4242
Decline: 4000 0000 0000 0002
Insufficient Funds: 4000 0000 0000 9995
```

**Test Customers**:
```
test_customer_success@example.com
test_customer_decline@example.com
```

---

## API Usability Checklist

### Design Checklist

- [ ] RESTful URL structure
- [ ] Consistent naming conventions
- [ ] Proper HTTP methods and status codes
- [ ] Clear, actionable error messages
- [ ] Comprehensive documentation
- [ ] Code examples in multiple languages
- [ ] Sandbox environment available
- [ ] Rate limiting with clear headers
- [ ] API versioning strategy
- [ ] Idempotency support for mutations

### Documentation Checklist

- [ ] OpenAPI specification
- [ ] Getting started guide
- [ ] Authentication guide
- [ ] Error handling guide
- [ ] Rate limiting documentation
- [ ] Changelog
- [ ] Migration guides
- [ ] API reference
- [ ] Code examples
- [ ] Postman collection

### Developer Experience Checklist

- [ ] < 15 minutes to first API call
- [ ] Official SDKs available
- [ ] Interactive API explorer
- [ ] Webhook support
- [ ] Developer dashboard
- [ ] API key management
- [ ] Usage analytics
- [ ] Support channels
- [ ] Community forum
- [ ] Status page

---

**Related Documents**:
- [Overview](overview.md) - Accessibility perspective introduction
- [UI Accessibility](ui-accessibility.md) - User interface accessibility
- [Documentation](documentation.md) - Documentation clarity standards
