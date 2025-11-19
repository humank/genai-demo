# API Documentation

> **Last Updated**: 2025-01-17

## Overview

This section contains comprehensive API documentation for the GenAI Demo e-commerce platform, including REST API endpoints, domain events, authentication, and integration guides.

## Quick Navigation

### 🌐 REST API

- [REST API Overview](rest/README.md) - API design principles and conventions
- [Authentication](rest/authentication.md) - JWT authentication and authorization
- [Error Handling](rest/error-handling.md) - Error response formats and codes
- [Versioning](rest/versioning.md) - API versioning strategy

### 📡 Domain Events

- [Events Overview](events/README.md) - Event-driven architecture
- [Event Catalog](events/event-catalog.md) - Complete list of domain events
- [Event Patterns](events/event-patterns.md) - Event design patterns
- [Event Contexts](events/contexts/) - Events by bounded context

### 🔌 Integration

- [Integration Guide](integration/README.md) - Integration patterns and best practices
- [Webhooks](integration/webhooks.md) - Webhook configuration
- [Rate Limiting](integration/rate-limiting.md) - API rate limits and quotas

## API Endpoints by Context

### Customer Context

**Base Path**: `/api/v1/customers`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/customers` | Create new customer |
| GET | `/customers/{id}` | Get customer by ID |
| PUT | `/customers/{id}` | Update customer |
| DELETE | `/customers/{id}` | Delete customer |
| GET | `/customers/{id}/orders` | Get customer orders |
| GET | `/customers/{id}/profile` | Get customer profile |
| PUT | `/customers/{id}/profile` | Update customer profile |
| POST | `/customers/{id}/addresses` | Add customer address |

[Full Customer API Documentation](rest/endpoints/customers.md)

### Order Context

**Base Path**: `/api/v1/orders`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/orders` | Create new order |
| GET | `/orders/{id}` | Get order by ID |
| PUT | `/orders/{id}` | Update order |
| POST | `/orders/{id}/submit` | Submit order |
| POST | `/orders/{id}/confirm` | Confirm order |
| POST | `/orders/{id}/ship` | Ship order |
| POST | `/orders/{id}/deliver` | Deliver order |
| POST | `/orders/{id}/cancel` | Cancel order |
| GET | `/orders/{id}/items` | Get order items |
| POST | `/orders/{id}/items` | Add order item |

[Full Order API Documentation](rest/endpoints/orders.md)

### Product Context

**Base Path**: `/api/v1/products`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/products` | Create new product |
| GET | `/products/{id}` | Get product by ID |
| PUT | `/products/{id}` | Update product |
| DELETE | `/products/{id}` | Delete product |
| GET | `/products` | List products |
| GET | `/products/search` | Search products |
| GET | `/products/{id}/inventory` | Get product inventory |

[Full Product API Documentation](rest/endpoints/products.md)

### Inventory Context

**Base Path**: `/api/v1/inventory`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/inventory/{productId}` | Get inventory level |
| POST | `/inventory/{productId}/reserve` | Reserve inventory |
| POST | `/inventory/{productId}/release` | Release inventory |
| POST | `/inventory/{productId}/adjust` | Adjust inventory |
| GET | `/inventory/low-stock` | Get low stock items |
| POST | `/inventory/{productId}/restock` | Restock product |

[Full Inventory API Documentation](rest/endpoints/inventory.md)

### Payment Context

**Base Path**: `/api/v1/payments`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/payments` | Process payment |
| GET | `/payments/{id}` | Get payment by ID |
| POST | `/payments/{id}/refund` | Refund payment |
| GET | `/payments/order/{orderId}` | Get payments for order |
| POST | `/payments/{id}/verify` | Verify payment |

[Full Payment API Documentation](rest/endpoints/payments.md)

### Shipping Context

**Base Path**: `/api/v1/shipping`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/shipping/shipments` | Create shipment |
| GET | `/shipping/shipments/{id}` | Get shipment by ID |
| PUT | `/shipping/shipments/{id}/track` | Update tracking |
| GET | `/shipping/rates` | Calculate shipping rates |

[Full Shipping API Documentation](rest/endpoints/shipping.md)

## Domain Events by Context

### Customer Events

- `CustomerCreatedEvent`
- `CustomerUpdatedEvent`
- `CustomerDeletedEvent`
- `CustomerProfileUpdatedEvent`
- `CustomerAddressAddedEvent`

[Full Customer Events](events/contexts/customer-events.md)

### Order Events

- `OrderCreatedEvent`
- `OrderSubmittedEvent`
- `OrderConfirmedEvent`
- `OrderShippedEvent`
- `OrderDeliveredEvent`
- `OrderCancelledEvent`
- `OrderItemAddedEvent`
- `OrderItemRemovedEvent`

[Full Order Events](events/contexts/order-events.md)

### Product Events

- `ProductCreatedEvent`
- `ProductUpdatedEvent`
- `ProductDeletedEvent`
- `ProductPriceChangedEvent`

[Full Product Events](events/contexts/product-events.md)

### Inventory Events

- `InventoryReservedEvent`
- `InventoryReleasedEvent`
- `InventoryAdjustedEvent`
- `LowStockAlertEvent`

[Full Inventory Events](events/contexts/inventory-events.md)

### Payment Events

- `PaymentProcessedEvent`
- `PaymentFailedEvent`
- `PaymentRefundedEvent`
- `PaymentVerifiedEvent`

[Full Payment Events](events/contexts/payment-events.md)

### Shipping Events

- `ShipmentCreatedEvent`
- `ShipmentShippedEvent`
- `ShipmentDeliveredEvent`
- `TrackingUpdatedEvent`

[Full Shipping Events](events/contexts/shipping-events.md)

## Authentication & Authorization

### JWT Authentication

All API requests require JWT authentication:

```http
Authorization: Bearer <jwt_token>
```

[Authentication Guide](rest/authentication.md)

### Role-Based Access Control

- **Admin**: Full access to all endpoints
- **Customer**: Access to own data only
- **Seller**: Access to own products and orders
- **Guest**: Read-only access to public data

[Authorization Guide](rest/authentication.md#authorization)

## Error Handling

### Standard Error Response

```json
{
  "errorCode": "RESOURCE_NOT_FOUND",
  "message": "Customer with id 123 not found",
  "timestamp": "2025-01-17T10:30:00Z",
  "path": "/api/v1/customers/123",
  "fieldErrors": []
}
```

[Error Handling Guide](rest/error-handling.md)

### HTTP Status Codes

- `200 OK`: Successful GET, PUT, PATCH
- `201 Created`: Successful POST
- `204 No Content`: Successful DELETE
- `400 Bad Request`: Validation errors
- `401 Unauthorized`: Authentication required
- `403 Forbidden`: Authorization failed
- `404 Not Found`: Resource not found
- `409 Conflict`: Business rule violation
- `500 Internal Server Error`: System error

## API Versioning

Current API version: **v1**

- **URL Versioning**: `/api/v1/`
- **Backward Compatibility**: Maintained for 2 versions
- **Deprecation Notice**: 6 months before removal

[Versioning Strategy](rest/versioning.md)

## Rate Limiting

- **Default**: 1000 requests per hour per API key
- **Burst**: 100 requests per minute
- **Headers**: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`

[Rate Limiting Guide](integration/rate-limiting.md)

## Getting Started

### For API Consumers

1. **Get API Key**: Register for API access
2. **Read Authentication Guide**: Understand JWT authentication
3. **Explore Endpoints**: Review endpoint documentation
4. **Test with Postman**: Use provided Postman collection
5. **Handle Errors**: Implement proper error handling

### For Developers

1. **Understand Architecture**: Review [Functional Viewpoint](../viewpoints/functional/README.md)
2. **Learn Event Patterns**: Study [Domain Events](events/README.md)
3. **Follow Standards**: Use [API Design Standards](rest/README.md)
4. **Implement Endpoints**: Follow [Development Guide](../development/README.md)

## Tools and Resources

### Postman Collection

Download the complete Postman collection:

### OpenAPI Specification

- [Swagger UI](http://localhost:8080/swagger-ui.html) (when running locally)

### Code Examples


## Related Documentation

### Architecture Documentation

- [Functional Viewpoint](../viewpoints/functional/README.md) - Business capabilities
- [Context Viewpoint](../viewpoints/context/README.md) - System context
- [Information Viewpoint](../viewpoints/information/README.md) - Data models

### Development Documentation

- [API Development Guide](../development/api-development.md)
- [Testing API Endpoints](../development/testing/api-testing.md)
- [API Security](../perspectives/security/api-security.md)

### Architecture Decisions


## Support

### API Support

- **Email**: api-support@company.com
- **Slack**: #api-support
- **Documentation**: This site
- **Status Page**: https://status.company.com

### Reporting Issues

1. Check [troubleshooting guide](../operations/troubleshooting/README.md)
2. Search existing issues
3. Create new issue with details
4. Include API request/response examples

## Contributing

### Updating API Documentation

1. Follow [API documentation standards](../STYLE-GUIDE.md#api-documentation)
2. Update OpenAPI specification
3. Add code examples
4. Submit PR for review

### Adding New Endpoints

1. Design endpoint following REST principles
2. Document in OpenAPI spec
3. Add to relevant context documentation
4. Include authentication/authorization requirements
5. Add code examples

---

**Document Owner**: API Team
**Last Review**: 2025-01-17
**Next Review**: 2025-04-17
**Status**: Active
