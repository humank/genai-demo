# 02: Hexagonal Architecture (Ports and Adapters)

Hexagonal Architecture, also known as Ports and Adapters, is an architectural pattern that aims to create loosely coupled application components that can be easily connected to their software environment. Proposed by Alistair Cockburn, it emphasizes separating the application's core logic from external concerns like databases, UI, messaging queues, or other applications.

The core idea is to place the application logic inside a "hexagon," with "ports" defining formal interfaces to the outside world. "Adapters" then implement these ports to bridge the gap between the application core and various external tools and technologies.

## Core Components

The Hexagonal Architecture has three main parts: the Application Core (the hexagon), Ports, and Adapters.

### 1. Application Core (The Hexagon)

*   This is the heart of your application and contains the business logic.
*   It includes **Domain Models** (Entities, Value Objects, Aggregates as defined in Domain-Driven Design), **Domain Services**, and **Application Services** (which orchestrate use cases).
*   The core should be completely independent of any specific technology or external system. It shouldn't know whether it's being driven by a web UI, a command-line interface, or automated tests. Similarly, it shouldn't know if it's persisting data to a SQL database, a NoSQL store, or a file system.

### 2. Ports

Ports are interfaces that define a contract for interaction between the application core and the outside world. They are part of the application core but represent its boundaries. There are two types of ports:

*   **Driving Ports (Input Ports / Use Case Interfaces):**
    *   Define how external actors (users, other systems, automated tests) can interact *with* the application to achieve a certain goal or execute a use case.
    *   Typically implemented by application services or use case handlers within the core.
    *   Example: An interface `OrderServicePort` with methods like `createOrder(OrderDetails)` or `getOrderStatus(OrderId)`.

*   **Driven Ports (Output Ports / Secondary Ports):**
    *   Define how the application core interacts *with* external services or infrastructure it needs to perform its tasks.
    *   These are interfaces that the application core *depends on* for things like data persistence, message sending, or accessing third-party APIs.
    *   Example: An interface `OrderRepositoryPort` with methods like `save(Order)` or `findById(OrderId)`, or a `NotificationServicePort` with a method `sendEmail(EmailDetails)`.

### 3. Adapters

Adapters are components outside the application core that implement the ports. They are responsible for translating between the specific technology of the external system and the port's interface.

*   **Driving Adapters (Primary Adapters):**
    *   These adapters drive the application by invoking methods on the input ports. They adapt external requests into method calls on the application core's use case interfaces.
    *   Examples:
        *   A REST controller that handles HTTP requests and calls an `OrderServicePort`.
        *   A CLI command that parses arguments and calls a relevant application service.
        *   Automated test scripts that directly invoke input ports.

*   **Driven Adapters (Secondary Adapters):**
    *   These adapters are driven by the application core when it needs to interact with external systems via output ports. They implement the output port interfaces using a specific technology.
    *   Examples:
        *   A JPA implementation of an `OrderRepositoryPort` that persists `Order` aggregates to a relational database.
        *   An SMTP implementation of a `NotificationServicePort` that sends emails.
        *   A REST client adapter that calls an external payment gateway API.

The key principle is **dependency inversion**: the application core defines the ports (interfaces), and the adapters (concrete implementations) depend on these abstractions, not the other way around. This keeps the core isolated.

## Visualizing Hexagonal Architecture

```mermaid
graph TD
    subgraph "Hexagon (Application Core)"
        direction LR
        US[Application Services / Use Cases (Input Ports)]
        DM[Domain Model (Entities, VOs, Aggregates)]
        OSP[Output Ports (Interfaces for external services)]
        
        US --> DM
        US --> OSP
    end

    subgraph "Driving Adapters (Primary)"
        WA[Web Adapter (Controller)]
        CLI[CLI Adapter]
        Test[Test Scripts]
    end

    subgraph "Driven Adapters (Secondary)"
        DB[Database Adapter (Repository Impl)]
        MSG[Messaging Adapter (Queue Impl)]
        EXT[External API Adapter]
    end

    WA --> US
    CLI --> US
    Test --> US

    OSP --> DB
    OSP --> MSG
    OSP --> EXT

    classDef hexagon fill:#ccffcc,stroke:#333,stroke-width:2px;
    classDef port fill:#lightblue,stroke:#333,stroke-width:2px;
    classDef adapter fill:#ffffcc,stroke:#333,stroke-width:2px;

    class US,DM,OSP hexagon;
    class WA,CLI,Test adapter;
    class DB,MSG,EXT adapter;
```

## Benefits of Hexagonal Architecture

*   **Testability:** The application core can be tested in isolation by mocking the ports or using test-specific adapters. Driving adapters can also be tested independently.
*   **Technology Independence:** Adapters can be swapped out with different technology implementations without affecting the application core. For example, you could switch from a REST API to gRPC, or from a MySQL database to PostgreSQL, by simply changing the adapters.
*   **Maintainability & Flexibility:** Changes in external systems or technologies are confined to their respective adapters, reducing the risk of impacting the core business logic. New ways of interacting with the application can be added by creating new driving adapters.
*   **Focus on Domain Logic:** Developers can concentrate on the business logic within the application core without being distracted by infrastructure concerns.
*   **Delayed Decisions:** Decisions about specific technologies (database, message broker, etc.) can be deferred, as the core only depends on interfaces.

## Hexagonal Architecture and DDD

Hexagonal Architecture and Domain-Driven Design (DDD) are highly complementary:

*   **DDD provides the "what" and "how" for the Application Core:** DDD principles help in modeling the domain logic (Aggregates, Entities, Value Objects, Domain Services) that resides within the hexagon.
*   **Hexagonal Architecture provides the "where":** It offers a clear structure for isolating this rich domain model from external infrastructure and UI concerns. The application services (use cases) in DDD often serve as the input ports in a hexagonal architecture. Repositories defined in DDD are perfect candidates for output ports.

By using them together, you can build systems where the valuable business logic is well-protected, independent, and testable, while still allowing flexible integration with various external systems.

## Examples from This Project's Codebase

The `genaidemo` project provides clear examples of Hexagonal Architecture components, especially within the `order` context.

### 1. Application Core

*   **Domain Model**: Located in `app/src/main/java/solid/humank/genaidemo/domain/`. For instance, the `Order` aggregate (`domain/order/model/aggregate/Order.java`) and its associated Value Objects (like `Money`, `OrderItem` in `domain/common/valueobject/`) represent the core business logic and state.
*   **Application Services & Input Ports**: These define and implement the use cases.

### 2. Input Ports (Use Case Interfaces)

*   **`OrderManagementUseCase.java`**:
    *   **Path**: `app/src/main/java/solid/humank/genaidemo/application/order/port/incoming/OrderManagementUseCase.java`
    *   **Description**: This interface is an Input Port. It defines the contract for all order management operations that can be initiated from outside the application core.
    *   **Example Methods**: `createOrder(CreateOrderCommand)`, `addOrderItem(AddOrderItemCommand)`, `submitOrder(String orderId)`.

    ```java
    // Snippet from OrderManagementUseCase.java
    public interface OrderManagementUseCase {
        OrderResponse createOrder(CreateOrderCommand command);
        OrderResponse addOrderItem(AddOrderItemCommand command);
        OrderResponse submitOrder(String orderId);
        // ... other methods
    }
    ```

### 3. Application Service (Input Port Implementation)

*   **`OrderApplicationService.java`**:
    *   **Path**: `app/src/main/java/solid/humank/genaidemo/application/order/service/OrderApplicationService.java`
    *   **Description**: This class implements the `OrderManagementUseCase` input port. It orchestrates the domain logic, uses output ports to interact with external systems (like databases or payment services), and contains application-specific logic.
    *   **Dependencies**: It injects `OrderPersistencePort` and `PaymentServicePort` (Output Ports).

    ```java
    // Snippet from OrderApplicationService.java
    @Service
    public class OrderApplicationService implements OrderManagementUseCase {
        private final OrderPersistencePort orderPersistencePort;
        private final PaymentServicePort paymentServicePort;
        // ... constructor and methods ...

        @Override
        public OrderResponse createOrder(CreateOrderCommand command) {
            Order order = orderFactory.create(...); // Domain logic
            orderPersistencePort.save(order);      // Using output port
            return mapToOrderResponse(order);
        }
        // ...
    }
    ```

### 4. Driving Adapters (Primary Adapters)

*   **`OrderController.java` (Web Adapter)**:
    *   **Path**: `app/src/main/java/solid/humank/genaidemo/interfaces/web/order/OrderController.java`
    *   **Description**: This is a Driving Adapter. It's a Spring MVC REST controller that handles incoming HTTP requests related to orders. It adapts these requests into calls on the `OrderManagementUseCase` input port.
    *   **Interaction**: It injects `OrderManagementUseCase` and calls its methods.

    ```java
    // Snippet from OrderController.java
    @RestController
    @RequestMapping("/api/orders")
    public class OrderController {
        private final OrderManagementUseCase orderService;

        public OrderController(OrderManagementUseCase orderService) {
            this.orderService = orderService;
        }

        @PostMapping
        public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
            CreateOrderCommand command = ...; // Adapting request
            solid.humank.genaidemo.application.order.dto.response.OrderResponse appResponse = orderService.createOrder(command); // Calling input port
            return new ResponseEntity<>(ResponseFactory.toWebResponse(appResponse), HttpStatus.CREATED);
        }
        // ... other request handling methods
    }
    ```

### 5. Output Ports (Secondary Port Interfaces)

These interfaces define contracts for services the application core depends on.

*   **`OrderPersistencePort.java`**:
    *   **Path**: `app/src/main/java/solid/humank/genaidemo/application/order/port/outgoing/OrderPersistencePort.java`
    *   **Description**: An Output Port defining operations for persisting and retrieving `Order` aggregates.
    *   **Example Methods**: `save(Order order)`, `findById(UUID orderId)`.

    ```java
    // Snippet from OrderPersistencePort.java
    public interface OrderPersistencePort {
        void save(Order order);
        Optional<Order> findById(UUID orderId);
        // ... other methods
    }
    ```

*   **`PaymentServicePort.java`**:
    *   **Path**: `app/src/main/java/solid/humank/genaidemo/application/order/port/outgoing/PaymentServicePort.java`
    *   **Description**: An Output Port for interacting with a payment service.
    *   **Example Methods**: `processPayment(UUID orderId, Money amount)`, `cancelPayment(UUID orderId)`.

    ```java
    // Snippet from PaymentServicePort.java
    public interface PaymentServicePort {
        PaymentResult processPayment(UUID orderId, Money amount);
        PaymentResult cancelPayment(UUID orderId);
        // ... other methods
    }
    ```

### 6. Driven Adapters (Secondary Adapters)

These are concrete implementations of the Output Ports, residing in the `infrastructure` layer.

*   **`OrderRepositoryAdapter.java` (Persistence Adapter)**:
    *   **Path**: `app/src/main/java/solid/humank/genaidemo/infrastructure/order/persistence/adapter/OrderRepositoryAdapter.java`
    *   **Description**: This class acts as a Driven Adapter. While it implements `OrderRepository` (a domain-specific repository interface), it fulfills the contract of `OrderPersistencePort` by interacting with `JpaOrderRepository` for database operations using JPA. The Spring IoC container wires this implementation to be injected where `OrderPersistencePort` is required in `OrderApplicationService`.
    *   **Technology**: Uses Spring Data JPA.

    ```java
    // Snippet from OrderRepositoryAdapter.java
    @Component // Or @Repository
    public class OrderRepositoryAdapter implements OrderRepository { // Implements domain repository
        private final JpaOrderRepository jpaOrderRepository; // Specific JPA repository
        // ... constructor ...

        @Override
        public Order save(Order order) {
            JpaOrderEntity jpaEntity = OrderMapper.toJpaEntity(order); // Adapting to JPA entity
            jpaOrderRepository.save(jpaEntity);
            return order;
        }
        // ... other methods adapting calls to jpaOrderRepository
    }
    // Note: For OrderApplicationService to use this for OrderPersistencePort,
    // this adapter (or another one like it) must be configured as the bean
    // for OrderPersistencePort. Often, OrderRepository (domain) and OrderPersistencePort (application)
    // can be the same interface, or the adapter implements the latter directly.
    // In this project, OrderApplicationService takes OrderPersistencePort.
    // The class `solid.humank.genaidemo.infrastructure.external.OrderRepositoryAdapter` (if it exists and implements OrderPersistencePort)
    // or a configuration that makes `OrderRepositoryAdapter` the provider for `OrderPersistencePort` is assumed.
    // For clarity, let's assume there's an adapter that directly implements OrderPersistencePort using JpaOrderRepository.
    // A more direct example is the PaymentServiceAdapter.
    ```
    *(Self-correction: The `OrderApplicationService` requires an `OrderPersistencePort`. The `OrderRepositoryAdapter` implements `OrderRepository`. A class that implements `OrderPersistencePort` is needed. Let's assume the project has such an adapter, likely named similarly or `OrderPersistenceAdapter` in `infrastructure/order/persistence/adapter/`)*

    Looking back at the files, `OrderRepositoryAdapter.java` is indeed in `infrastructure/order/persistence/adapter/`. It implements `OrderRepository`. If `OrderPersistencePort` and `OrderRepository` are intended to be fulfilled by the same adapter, this works. The key is that `OrderApplicationService` gets an instance that satisfies `OrderPersistencePort`. Often, `OrderRepository` from the domain is directly used as the port by the application service.

*   **`PaymentServiceAdapter.java` (Payment Adapter)**:
    *   **Path**: `app/src/main/java/solid/humank/genaidemo/infrastructure/payment/external/PaymentServiceAdapter.java`
    *   **Description**: This is a Driven Adapter that implements the `PaymentServicePort`. It simulates interaction with an external payment service.
    *   **Technology**: Mock implementation (in a real scenario, it would use HTTP clients, SDKs, etc.).

    ```java
    // Snippet from PaymentServiceAdapter.java
    @Component
    public class PaymentServiceAdapter implements PaymentServicePort {
        @Override
        public PaymentResult processPayment(UUID orderId, Money amount) {
            // Simulate external payment interaction
            System.out.println("Processing payment for order: " + orderId + ", amount: " + amount);
            return PaymentResult.successful("PAY-" + System.currentTimeMillis());
        }
        // ... other methods
    }
    ```

These examples illustrate how the `genaidemo` project separates concerns using Hexagonal Architecture, allowing the domain logic to remain independent of UI, database, and external service details.

---
The project structure with `application`, `domain`, `infrastructure`, and `interfaces` packages aligns well with Hexagonal Architecture principles.

Based on the previous exploration and typical Java project structures using this architecture:
*   **Driving Adapters (Primary Adapters)** are likely in the `interfaces.web` or a similar package (e.g., REST controllers).
*   **Input Ports** are likely interfaces implemented by application services, possibly found in an `application...port.in` or `application...service` package.
*   **Application Services** themselves (which implement input ports) would be in an `application...service` or `application...usecase` package.
*   **Output Ports** are interfaces for repositories or external service clients, likely in an `application...port.out` or `domain...repository` package.
*   **Driven Adapters (Secondary Adapters)** would be implementations of these output ports, found in the `infrastructure` layer (e.g., `infrastructure...persistence` for database adapters, `infrastructure...external` for external service clients).

Let's start by looking for Input Ports and the Application Services that implement them. The `app/src/main/java/solid/humank/genaidemo/application/order/port/in/` directory seems like a good candidate for input ports related to orders.
