# Sequence Diagram Example: API Interactions

This example demonstrates API interaction sequence diagrams using Mermaid.

## Simple API Call

```mermaid
sequenceDiagram
    participant Client
    participant API
    participant Database
    
    Client->>API: GET /customers/123
    API->>Database: SELECT * FROM customers WHERE id=123
    Database-->>API: Customer data
    API-->>Client: 200 OK {customer}
```

## Order Submission Flow

```mermaid
sequenceDiagram
    participant Customer
    participant API
    participant OrderService
    participant InventoryService
    participant PaymentService
    participant Database
    
    Customer->>API: POST /orders
    API->>OrderService: createOrder(orderData)
    OrderService->>InventoryService: checkAvailability(items)
    InventoryService-->>OrderService: Available
    OrderService->>PaymentService: processPayment(amount)
    PaymentService-->>OrderService: Payment Success
    OrderService->>Database: Save order
    Database-->>OrderService: Order saved
    OrderService->>OrderService: publishOrderCreatedEvent()
    OrderService-->>API: Order created
    API-->>Customer: 201 Created {order}
```

## Authentication Flow

```mermaid
sequenceDiagram
    participant Client
    participant API
    participant AuthService
    participant TokenService
    participant Database
    
    Client->>API: POST /auth/login
    Note over Client,API: {username, password}
    API->>AuthService: authenticate(credentials)
    AuthService->>Database: findUserByUsername()
    Database-->>AuthService: User data
    AuthService->>AuthService: validatePassword()
    AuthService->>TokenService: generateToken(user)
    TokenService-->>AuthService: JWT token
    AuthService-->>API: Authentication success
    API-->>Client: 200 OK {token}
    Note over Client,API: Client stores token
```

## Error Handling

```mermaid
sequenceDiagram
    participant Client
    participant API
    participant Service
    participant Database
    
    Client->>API: POST /orders
    API->>Service: createOrder(data)
    Service->>Database: Save order
    Database--xService: Connection timeout
    Service--xAPI: DatabaseException
    API-->>Client: 503 Service Unavailable
    Note over Client,API: {error: "Database unavailable"}
```

## Event-Driven Architecture

```mermaid
sequenceDiagram
    participant OrderService
    participant EventBus
    participant InventoryService
    participant NotificationService
    participant EmailService
    
    OrderService->>EventBus: publish(OrderCreatedEvent)
    EventBus->>InventoryService: OrderCreatedEvent
    EventBus->>NotificationService: OrderCreatedEvent
    
    par Parallel Processing
        InventoryService->>InventoryService: reserveInventory()
        InventoryService->>EventBus: publish(InventoryReservedEvent)
    and
        NotificationService->>EmailService: sendOrderConfirmation()
        EmailService-->>NotificationService: Email sent
    end
```

## Retry Mechanism

```mermaid
sequenceDiagram
    participant Client
    participant API
    participant PaymentGateway
    
    Client->>API: POST /payments
    API->>PaymentGateway: processPayment()
    PaymentGateway--xAPI: Timeout
    Note over API: Retry 1
    API->>PaymentGateway: processPayment()
    PaymentGateway--xAPI: Timeout
    Note over API: Retry 2
    API->>PaymentGateway: processPayment()
    PaymentGateway-->>API: Payment Success
    API-->>Client: 200 OK
```

## Circuit Breaker Pattern

```mermaid
sequenceDiagram
    participant Client
    participant API
    participant CircuitBreaker
    participant ExternalService
    
    Client->>API: Request
    API->>CircuitBreaker: call(ExternalService)
    
    alt Circuit Closed (Normal)
        CircuitBreaker->>ExternalService: Forward request
        ExternalService-->>CircuitBreaker: Response
        CircuitBreaker-->>API: Response
    else Circuit Open (Failing)
        CircuitBreaker--xAPI: Circuit Open Error
        Note over CircuitBreaker: Too many failures
    else Circuit Half-Open (Testing)
        CircuitBreaker->>ExternalService: Test request
        ExternalService-->>CircuitBreaker: Success
        Note over CircuitBreaker: Close circuit
    end
    
    API-->>Client: Response
```

## Use Cases

- **API Documentation**: Document API interaction flows
- **System Integration**: Show how systems communicate
- **Event Flows**: Visualize event-driven architecture
- **Error Scenarios**: Document error handling
- **Authentication**: Show auth flows

## Tips

1. Use `participant` to define actors
2. Use `-->>` for return messages
3. Use `--x` for failed calls
4. Use `Note` for additional context
5. Use `alt/else/end` for conditional flows
6. Use `par/and/end` for parallel processing
7. Keep participants to 5-7 for clarity
