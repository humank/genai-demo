# API Interactions Diagram

This document shows the interaction relationships and call flows of various APIs in the system.

## API Interaction Architecture Diagram

```mermaid
graph TB
    subgraph "🌐 Clients"
        WEB_ADMIN[👨‍💼 Admin<br/>CMC Frontend]
        WEB_CONSUMER[🛒 Consumer<br/>Consumer Frontend]
        MOBILE[📱 Mobile App]
        THIRD_PARTY[🔗 Third-party Systems]
    end
    
    subgraph "🔒 API Gateway"
        API_GATEWAY[🚪 API Gateway]
        AUTH[🔐 Authentication]
        RATE_LIMIT[⏱️ Rate Limiting]
        LOAD_BALANCER[⚖️ Load Balancer]
    end
    
    subgraph "🔌 API Layer"
        subgraph "📦 Order APIs"
            ORDER_REST[📋 Order REST API]
            ORDER_GRAPHQL[📊 Order GraphQL]
            ORDER_WEBSOCKET[⚡ Order WebSocket]
        end
        
        subgraph "👥 Customer APIs"
            CUSTOMER_REST[👤 Customer REST API]
            CUSTOMER_GRAPHQL[📊 Customer GraphQL]
            LOYALTY_API[🎁 Loyalty API]
        end
        
        subgraph "🏷️ Product APIs"
            PRODUCT_REST[📦 Product REST API]
            CATALOG_API[📚 Catalog API]
            INVENTORY_API[📊 Inventory API]
        end
        
        subgraph "💳 Payment APIs"
            PAYMENT_REST[💰 Payment REST API]
            WEBHOOK_API[🔔 Webhook API]
        end
    end
    
    subgraph "⚙️ Application Services"
        ORDER_SVC[📦 Order Service]
        CUSTOMER_SVC[👥 Customer Service]
        PRODUCT_SVC[🏷️ Product Service]
        PAYMENT_SVC[💳 Payment Service]
        NOTIFICATION_SVC[📧 Notification Service]
    end
    
    subgraph "🔗 External Services"
        PAYMENT_GATEWAY[💳 Payment Gateway]
        EMAIL_SERVICE[📧 Email Service]
        SMS_SERVICE[📱 SMS Service]
        LOGISTICS[🚚 Logistics API]
        ANALYTICS[📊 Analytics Service]
    end
    
    subgraph "📊 Monitoring and Documentation"
        SWAGGER[📖 Swagger UI]
        ACTUATOR[🔍 Spring Actuator]
        METRICS[📈 Metrics]
        LOGS[📝 Logs]
    end
    
    %% Clients to API Gateway
    WEB_ADMIN --> API_GATEWAY
    WEB_CONSUMER --> API_GATEWAY
    MOBILE --> API_GATEWAY
    THIRD_PARTY --> API_GATEWAY
    
    %% API Gateway processing
    API_GATEWAY --> AUTH
    API_GATEWAY --> RATE_LIMIT
    API_GATEWAY --> LOAD_BALANCER
    
    %% Load balancer to APIs
    LOAD_BALANCER --> ORDER_REST
    LOAD_BALANCER --> CUSTOMER_REST
    LOAD_BALANCER --> PRODUCT_REST
    LOAD_BALANCER --> PAYMENT_REST
    
    %% GraphQL and WebSocket
    LOAD_BALANCER --> ORDER_GRAPHQL
    LOAD_BALANCER --> CUSTOMER_GRAPHQL
    LOAD_BALANCER --> ORDER_WEBSOCKET
    
    %% Specialized APIs
    LOAD_BALANCER --> LOYALTY_API
    LOAD_BALANCER --> CATALOG_API
    LOAD_BALANCER --> INVENTORY_API
    LOAD_BALANCER --> WEBHOOK_API
    
    %% APIs to application services
    ORDER_REST --> ORDER_SVC
    ORDER_GRAPHQL --> ORDER_SVC
    ORDER_WEBSOCKET --> ORDER_SVC
    
    CUSTOMER_REST --> CUSTOMER_SVC
    CUSTOMER_GRAPHQL --> CUSTOMER_SVC
    LOYALTY_API --> CUSTOMER_SVC
    
    PRODUCT_REST --> PRODUCT_SVC
    CATALOG_API --> PRODUCT_SVC
    INVENTORY_API --> PRODUCT_SVC
    
    PAYMENT_REST --> PAYMENT_SVC
    WEBHOOK_API --> PAYMENT_SVC
    
    %% Inter-service calls
    ORDER_SVC --> CUSTOMER_SVC
    ORDER_SVC --> PRODUCT_SVC
    ORDER_SVC --> PAYMENT_SVC
    ORDER_SVC --> NOTIFICATION_SVC
    
    %% External service calls
    PAYMENT_SVC --> PAYMENT_GATEWAY
    NOTIFICATION_SVC --> EMAIL_SERVICE
    NOTIFICATION_SVC --> SMS_SERVICE
    ORDER_SVC --> LOGISTICS
    
    %% Monitoring and analytics
    ORDER_SVC --> ANALYTICS
    CUSTOMER_SVC --> ANALYTICS
    
    %% Documentation and monitoring
    API_GATEWAY --> SWAGGER
    ORDER_REST --> ACTUATOR
    CUSTOMER_REST --> METRICS
    PRODUCT_REST --> LOGS
    
    %% Style definitions
    classDef client fill:#e3f2fd,stroke:#1565c0,stroke-width:2px
    classDef gateway fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px
    classDef api fill:#e8f5e8,stroke:#2e7d32,stroke-width:2px
    classDef service fill:#fff3e0,stroke:#ef6c00,stroke-width:2px
    classDef external fill:#ffebee,stroke:#c62828,stroke-width:2px
    classDef monitoring fill:#fce4ec,stroke:#c2185b,stroke-width:2px
    
    class WEB_ADMIN,WEB_CONSUMER,MOBILE,THIRD_PARTY client
    class API_GATEWAY,AUTH,RATE_LIMIT,LOAD_BALANCER gateway
    class ORDER_REST,ORDER_GRAPHQL,ORDER_WEBSOCKET,CUSTOMER_REST,CUSTOMER_GRAPHQL,LOYALTY_API,PRODUCT_REST,CATALOG_API,INVENTORY_API,PAYMENT_REST,WEBHOOK_API api
    class ORDER_SVC,CUSTOMER_SVC,PRODUCT_SVC,PAYMENT_SVC,NOTIFICATION_SVC service
    class PAYMENT_GATEWAY,EMAIL_SERVICE,SMS_SERVICE,LOGISTICS,ANALYTICS external
    class SWAGGER,ACTUATOR,METRICS,LOGS monitoring
```

## Detailed API Endpoint Design

### 📦 Order API

#### REST API Endpoints

```http
# Order Management
POST   /api/v1/orders                    # Create order
GET    /api/v1/orders/{orderId}          # Get order details
PUT    /api/v1/orders/{orderId}/confirm  # Confirm order
DELETE /api/v1/orders/{orderId}          # Cancel order

# Order Queries
GET    /api/v1/orders                    # Query order list
GET    /api/v1/customers/{customerId}/orders  # Customer orders
```

#### GraphQL Schema

```graphql
type Order {
  id: ID!
  customerId: ID!
  items: [OrderItem!]!
  totalAmount: Money!
  status: OrderStatus!
  createdAt: DateTime!
}

type Query {
  order(id: ID!): Order
  orders(filter: OrderFilter, pagination: Pagination): OrderConnection
}

type Mutation {
  createOrder(input: CreateOrderInput!): CreateOrderPayload!
  confirmOrder(orderId: ID!): ConfirmOrderPayload!
}
```

#### WebSocket Events

```javascript
// Order status update
{
  "type": "ORDER_STATUS_UPDATED",
  "orderId": "ORDER-123",
  "status": "CONFIRMED",
  "timestamp": "2025-01-21T10:30:00Z"
}

// Inventory change notification
{
  "type": "INVENTORY_UPDATED",
  "productId": "PROD-456",
  "availableStock": 50,
  "timestamp": "2025-01-21T10:30:00Z"
}
```

### 👥 Customer API

#### Customer REST API Endpoints

```http
# Customer Management
POST   /api/v1/customers                 # Register customer
GET    /api/v1/customers/{customerId}    # Get customer info
PUT    /api/v1/customers/{customerId}    # Update customer info

# Loyalty System
GET    /api/v1/customers/{customerId}/loyalty  # Get loyalty info
POST   /api/v1/customers/{customerId}/loyalty/redeem  # Redeem points
```

### 🏷️ Product API

#### Product REST API Endpoints

```http
# Product Catalog
GET    /api/v1/products                  # Product list
GET    /api/v1/products/{productId}      # Product details
GET    /api/v1/categories                # Product categories

# Inventory Management
GET    /api/v1/products/{productId}/inventory  # Inventory query
POST   /api/v1/products/{productId}/inventory/reserve  # Reserve inventory
```

### 💳 Payment API

#### Payment REST API Endpoints

```http
# Payment Processing
POST   /api/v1/payments                  # Create payment
GET    /api/v1/payments/{paymentId}      # Payment status
POST   /api/v1/payments/{paymentId}/refund  # Refund

# Webhook Endpoints
POST   /api/v1/webhooks/payment-gateway  # Payment gateway callback
```

## API Call Flow

### 🛒 Complete Shopping Flow

```mermaid
sequenceDiagram
    participant Consumer as 🛒 Consumer Frontend
    participant Gateway as 🚪 API Gateway
    participant OrderAPI as 📦 Order API
    participant CustomerAPI as 👥 Customer API
    participant ProductAPI as 🏷️ Product API
    participant PaymentAPI as 💳 Payment API
    participant PaymentGW as 💳 Payment Gateway
    
    Consumer->>Gateway: 1. Browse products
    Gateway->>ProductAPI: GET /api/v1/products
    ProductAPI-->>Gateway: Product list
    Gateway-->>Consumer: Product info
    
    Consumer->>Gateway: 2. Add to cart
    Gateway->>ProductAPI: GET /api/v1/products/{id}/inventory
    ProductAPI-->>Gateway: Inventory info
    Gateway-->>Consumer: Inventory confirmation
    
    Consumer->>Gateway: 3. Create order
    Gateway->>OrderAPI: POST /api/v1/orders
    OrderAPI->>CustomerAPI: Validate customer info
    CustomerAPI-->>OrderAPI: Customer validation result
    OrderAPI->>ProductAPI: Reserve inventory
    ProductAPI-->>OrderAPI: Inventory reservation result
    OrderAPI-->>Gateway: Order created successfully
    Gateway-->>Consumer: Order info
    
    Consumer->>Gateway: 4. Process payment
    Gateway->>PaymentAPI: POST /api/v1/payments
    PaymentAPI->>PaymentGW: Payment request
    PaymentGW-->>PaymentAPI: Payment result
    PaymentAPI-->>Gateway: Payment status
    Gateway-->>Consumer: Payment confirmation
    
    PaymentGW->>Gateway: 5. Webhook notification
    Gateway->>PaymentAPI: POST /api/v1/webhooks/payment-gateway
    PaymentAPI->>OrderAPI: Update order status
    OrderAPI-->>PaymentAPI: Status update confirmation
```

## API Security Design

### 🔐 Authentication and Authorization

```http
# JWT Token Authentication
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

# API Key Authentication (Third-party systems)
X-API-Key: your-api-key-here

# OAuth 2.0 (External integrations)
Authorization: Bearer oauth-access-token
```

### 🛡️ Security Headers

```http
# CORS Configuration
Access-Control-Allow-Origin: https://your-frontend.com
Access-Control-Allow-Methods: GET, POST, PUT, DELETE
Access-Control-Allow-Headers: Content-Type, Authorization

# Security Headers
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
```

### ⏱️ Rate Limiting

```http
# Rate limiting headers
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1642781400
```

## API Version Management

### 📋 Versioning Strategy

- **URL Versioning**: `/api/v1/orders`, `/api/v2/orders`
- **Header Versioning**: `Accept: application/vnd.api+json;version=1`
- **Backward Compatibility**: Support old versions for at least 6 months

### 🔄 Version Migration

```http
# Version deprecation warning
Deprecation: true
Sunset: Wed, 11 Nov 2025 23:59:59 GMT
Link: </api/v2/orders>; rel="successor-version"
```

## API Monitoring and Observability

### 📊 Key Metrics

- **Response Time**: P50, P95, P99
- **Error Rate**: 4xx, 5xx error percentage
- **Throughput**: Requests per second (RPS)
- **Availability**: Uptime percentage

### 🔍 Distributed Tracing

```http
# Tracing headers
X-Trace-Id: 550e8400-e29b-41d4-a716-446655440000
X-Span-Id: 6e0c63257de34c92
X-Parent-Span-Id: 05e3ac9a4f6e3b90
```

## Related Documentation

- [Architecture Overview](architecture-overview.md) - Overall system architecture
- [Event-Driven Architecture](event-driven-architecture.md) - Event handling mechanisms
- [API Documentation](../../api/) - Detailed API specifications