# From Chaos to Clean: A DDD and Hexagonal Architecture Refactoring Journey

This document records the process of gradually refactoring a chaotic code structure to comply with Domain-Driven Design (DDD) and Hexagonal Architecture. This refactoring process not only improved the code organization but also enhanced the system's maintainability, testability, and flexibility.

## Initial State: Chaotic Code Structure

Initially, the project had the following problems:

1. **Chaotic Dependencies Between Layers**:
   - Application layer directly depends on infrastructure layer classes
   - Domain logic scattered across different layers
   - Lack of clear architectural boundaries

2. **Inconsistent Package Structure**:
   - Classes with similar functionality scattered across different packages
   - Inconsistent naming, making it difficult to understand class responsibilities
   - Lack of modularity and cohesion

3. **Business Logic Mixed with Technical Details**:
   - Domain models directly depend on persistence frameworks
   - Business rules mixed with UI logic
   - Difficult to test core business logic independently

## Refactoring Goals

Our refactoring goals were to establish an architecture that follows these principles:

1. **Clear Layered Architecture**:
   - Domain Layer: Contains core business logic and rules
   - Application Layer: Coordinates domain objects to complete user use cases
   - Infrastructure Layer: Provides technical implementations
   - Interface Layer: Handles user interactions

2. **Dependency Direction Control**:
   - Inner layers don't depend on outer layers
   - Use dependency inversion principle to resolve cross-layer dependencies

3. **DDD Tactical Patterns**:
   - Proper use of aggregate roots, entities, value objects
   - Application of patterns like domain events, domain services, repositories
   - Anti-corruption layers to isolate external systems

## Refactoring Steps

### Phase 1: Establish Basic Architecture

1. **Define Clear Package Structure**:
   ```
   solid.humank.genaidemo/
   ├── domain/                           # Domain Layer
   │   ├── common/                       # Shared domain concepts
   │   │   ├── annotations/              # DDD related annotations
   │   │   ├── events/                   # Domain event infrastructure
   │   │   ├── specification/            # Specification pattern
   │   │   ├── repository/               # Repository interfaces
   │   │   └── valueobject/              # Shared value objects
   │   ├── order/                        # Order domain
   │   │   ├── model/                    # Order domain model
   │   │   │   ├── aggregate/            # Aggregate root (Order)
   │   │   │   ├── events/               # Domain events
   │   │   │   ├── factory/              # Factories
   │   │   │   ├── policy/               # Policies
   │   │   │   ├── service/              # Domain services
   │   │   │   └── specification/        # Specifications
   │   │   ├── repository/               # Repository interfaces
   │   │   └── validation/               # Domain validation
   │   ├── payment/                      # Payment domain
   │   │   ├── model/                    # Payment domain model
   │   │   │   ├── aggregate/            # Aggregate root (Payment)
   │   │   │   ├── events/               # Domain events
   │   │   │   ├── service/              # Domain services
   │   │   │   └── valueobject/          # Value objects
   │   │   ├── events/                   # Payment domain events
   │   │   └── repository/               # Repository interfaces
   │   ├── inventory/                    # Inventory domain
   │   │   └── model/                    # Inventory domain model
   │   │       ├── aggregate/            # Aggregate root (Inventory)
   │   │       └── valueobject/          # Value objects
   │   ├── delivery/                     # Delivery domain
   │   │   └── model/                    # Delivery domain model
   │   │       ├── aggregate/            # Aggregate root (Delivery)
   │   │       └── valueobject/          # Value objects
   │   ├── notification/                 # Notification domain
   │   │   └── model/                    # Notification domain model
   │   │       ├── aggregate/            # Aggregate root (Notification)
   │   │       └── valueobject/          # Value objects
   │   └── workflow/                     # Workflow domain
   │       └── model/                    # Workflow domain model
   │           ├── aggregate/            # Aggregate root (OrderWorkflow)
   │           └── valueobject/          # Value objects
   ├── application/                      # Application Layer
   │   ├── common/                       # Shared application services
   │   │   └── valueobject/              # Application layer value objects
   │   ├── order/                        # Order application services
   │   │   ├── dto/                      # DTOs
   │   │   │   ├── response/             # Response DTOs
   │   │   │   └── Various command and request DTOs
   │   │   ├── port/                     # Ports
   │   │   │   ├── incoming/             # Inbound ports (OrderManagementUseCase)
   │   │   │   └── outgoing/             # Outbound ports
   │   │   └── service/                  # Application services (OrderApplicationService)
   │   ├── payment/                      # Payment application services
   │   │   ├── dto/                      # DTOs
   │   │   ├── port/                     # Ports
   │   │   │   ├── incoming/             # Inbound ports
   │   │   │   └── outgoing/             # Outbound ports
   │   │   └── service/                  # Application services
   │   ├── inventory/                    # Inventory application services
   │   │   └── port/                     # Ports
   │   │       └── incoming/             # Inbound ports
   │   ├── logistics/                    # Logistics application services
   │   │   └── port/                     # Ports
   │   │       └── incoming/             # Inbound ports
   │   └── notification/                 # Notification application services
   │       └── port/                     # Ports
   │           └── incoming/             # Inbound ports
   ├── infrastructure/                   # Infrastructure Layer
   │   ├── common/                       # Shared infrastructure
   │   │   └── event/                    # Event infrastructure
   │   ├── order/                        # Order infrastructure
   │   │   ├── acl/                      # Anti-corruption layer
   │   │   ├── config/                   # Configuration
   │   │   ├── external/                 # External service adapters
   │   │   └── persistence/              # Persistence
   │   │       ├── adapter/              # Repository adapters
   │   │       ├── entity/               # JPA entities
   │   │       ├── mapper/               # Mappers
   │   │       └── repository/           # JPA repositories
   │   ├── payment/                      # Payment infrastructure
   │   │   ├── config/                   # Configuration
   │   │   ├── external/                 # External service adapters
   │   │   └── persistence/              # Persistence
   │   │       ├── adapter/              # Repository adapters
   │   │       ├── entity/               # JPA entities
   │   │       ├── mapper/               # Mappers
   │   │       └── repository/           # JPA repositories
   │   ├── config/                       # Global configuration
   │   ├── external/                     # Shared external services
   │   └── saga/                         # Saga coordinators
   │       └── definition/               # Saga definitions
   ├── interfaces/                       # Interface Layer
   │   └── web/                          # Web interfaces
   │       ├── order/                    # Order controllers
   │       │   ├── dto/                  # Web DTOs
   │       │   └── OrderController.java
   │       └── payment/                  # Payment controllers
   │           ├── dto/                  # Web DTOs
   │           └── PaymentController.java
   ├── exceptions/                       # Global exception handling
   ├── utils/                            # Utility classes
   └── GenAiDemoApplication.java         # Application entry point
   ```

2. **Establish Architecture Tests**:
   - Create `DddArchitectureTest` to ensure correct layered architecture dependencies
   - Create `DddTacticalPatternsTest` to ensure proper implementation of DDD tactical patterns
   - Create `PackageStructureTest` to ensure package structure compliance

### Phase 2: Domain Model Refactoring

1. **Identify Aggregate Roots and Boundaries**:
   - Refactor `Order` as aggregate root, controlling access to `OrderItem`
   - Refactor `Payment` as independent aggregate root
   - Identify other aggregate roots like `Inventory`, `Delivery`, `Notification`, and `OrderWorkflow`

2. **Implement Value Objects**:
   - Create immutable value objects like `OrderId`, `Money`, `Address`
   - Ensure value objects encapsulate related business rules
   - Place shared value objects in `domain.common.valueobject` package

3. **Define Domain Events**:
   - Create domain events like `OrderCreatedEvent`, `PaymentProcessedEvent`
   - Implement `DomainEvent` interface
   - Use events to decouple interactions between different domains

4. **Design Repository Interfaces**:
   - Define repository interfaces in domain layer, like `OrderRepository`
   - Ensure repository operations work with aggregate roots
   - Place repository interfaces in corresponding domain `repository` packages

### Phase 3: Application Layer Refactoring

1. **Implement Application Services**:
   - Create `OrderApplicationService` and `PaymentApplicationService`
   - Application services coordinate domain objects to complete use cases
   - Ensure application services don't contain business logic

2. **Define Commands and Queries**:
   - Create command objects like `CreateOrderCommand`, `ProcessPaymentCommand`
   - Create query objects and response DTOs
   - Place DTOs in corresponding application service `dto` packages

3. **Define Ports**:
   - Create inbound ports (use case interfaces) like `OrderManagementUseCase`
   - Create outbound ports like `OrderPersistencePort`, `PaymentServicePort`
   - Place ports in corresponding application service `port` packages

### Phase 4: Infrastructure Layer Refactoring

1. **Implement Repositories**:
   - Create repository implementations like `OrderRepositoryImpl`
   - Use JPA for persistence implementation
   - Create mappers like `OrderMapper` to convert between domain models and JPA entities

2. **Implement Adapters**:
   - Create external system adapters like `ExternalPaymentAdapter`, `LogisticsServiceAdapter`
   - Implement anti-corruption layers like `LogisticsAntiCorruptionLayer`
   - Place adapters in corresponding domain `infrastructure` packages

3. **Configure Dependency Injection**:
   - Set up Spring configuration to bind interfaces with implementations
   - Ensure dependency injection follows dependency inversion principle
   - Create configuration classes like `OrderJpaConfig`, `PaymentJpaConfig`

### Phase 5: Interface Layer Refactoring

1. **Implement Controllers**:
   - Create REST controllers like `OrderController`, `PaymentController`
   - Controllers only handle HTTP requests and responses
   - Place controllers in `interfaces.web` package

2. **Define API Models**:
   - Create request and response models like `CreateOrderRequest`, `OrderResponse`
   - Use mappers to convert between API models and application layer DTOs
   - Place API models in corresponding controller `dto` packages

## Post-Refactoring Architecture

After refactoring, we achieved a system that complies with DDD and Hexagonal Architecture:

1. **Domain Layer**:
   - Contains pure business logic, independent of external technologies
   - Uses patterns like aggregate roots, entities, value objects to express business concepts
   - Implements loose coupling communication between domains through domain events

2. **Application Layer**:
   - Coordinates domain objects to complete use cases
   - Defines ports (interfaces) to interact with external world
   - Contains no business rules, only responsible for coordination

3. **Infrastructure Layer**:
   - Implements repository interfaces defined by domain layer
   - Provides integration with external systems
   - Isolates external system impact through anti-corruption layers

4. **Interface Layer**:
   - Handles user interactions like HTTP requests
   - Converts requests to commands or queries that application layer can process
   - Converts application layer responses to formats suitable for UI

## Architecture Testing

To ensure architectural integrity, we implemented three types of architecture tests:

1. **DddArchitectureTest**:
   - Ensures correct layered architecture dependencies
   - Verifies inner layers don't depend on outer layers
   - Checks responsibility boundaries of each layer

2. **DddTacticalPatternsTest**:
   - Ensures proper implementation of DDD tactical patterns
   - Verifies immutability of value objects
   - Checks correct usage of aggregate roots, entities, domain events, etc.

3. **PackageStructureTest**:
   - Ensures package structure compliance
   - Verifies classes are in correct packages
   - Checks naming consistency

## Refactoring Benefits

This refactoring brought the following benefits:

1. **Improved Maintainability**:
   - Clear layered architecture makes code easier to understand and maintain
   - Clear responsibility allocation reduces code chaos

2. **Enhanced Testability**:
   - Separation of business logic from technical details facilitates unit testing
   - Use of dependency inversion principle facilitates mocking external dependencies

3. **Increased Flexibility**:
   - Easy to replace technical implementations like databases or UI frameworks
   - Domain model independence makes it easier to adapt to business changes

4. **Improved Team Collaboration**:
   - Clear architectural boundaries allow different teams to work in parallel
   - Shared domain language improves communication efficiency

## Continuous Improvement

Refactoring is a continuous process. Future improvements could consider:

1. **Introduce CQRS Pattern**:
   - Separate command and query responsibilities
   - Optimize read/write performance

2. **Implement Event Sourcing**:
   - Store events instead of state
   - Provide complete audit and historical records

3. **Microservice Decomposition**:
   - Split into microservices based on domain boundaries
   - Use event-driven architecture for inter-service communication

4. **Enhanced Observability**:
   - Implement distributed tracing
   - Add more detailed monitoring and logging

## Conclusion

Through this refactoring, we transformed a chaotic code structure into a system that complies with DDD and Hexagonal Architecture. This not only improved code quality but also made the system more flexible, maintainable, and testable. The experiences and lessons learned during the refactoring process also provided valuable learning opportunities for the team, helping us better apply these architectural principles in future projects.