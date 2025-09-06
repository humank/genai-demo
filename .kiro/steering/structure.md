# Project Structure

## Root Directory Organization

```
genai-demo/
├── app/                    # Main Spring Boot application
├── cmc-frontend/           # Commerce Management Center (Next.js)
├── consumer-frontend/      # Consumer-facing application (Angular)
├── deployment/             # Kubernetes and cloud deployment configs
├── docker/                 # Docker build scripts and configurations
├── docs/                   # Project documentation
├── scripts/                # Development and deployment scripts
├── tools/                  # Development tools (PlantUML, etc.)
├── docker-compose.yml      # Multi-container orchestration
└── README.md              # Project overview and setup guide
```

## Backend Structure (DDD + Hexagonal Architecture)

The backend follows strict DDD layering with hexagonal architecture principles:

```
app/src/main/java/solid/humank/genaidemo/
├── application/            # Application Layer (Use Cases)
│   ├── common/            # Shared application services
│   ├── customer/          # Customer management use cases
│   ├── inventory/         # Inventory management use cases
│   ├── order/             # Order processing use cases
│   ├── payment/           # Payment processing use cases
│   ├── product/           # Product management use cases
│   └── [other domains]/   # Other bounded contexts
├── domain/                # Domain Layer (Business Logic)
│   ├── common/            # Shared domain concepts
│   ├── customer/          # Customer aggregate and domain logic
│   │   ├── model/         # Aggregates, entities, value objects
│   │   ├── events/        # Domain events
│   │   ├── repository/    # Repository interfaces
│   │   └── service/       # Domain services
│   ├── order/             # Order aggregate and domain logic
│   ├── payment/           # Payment aggregate and domain logic
│   └── [other domains]/   # Other bounded contexts
├── infrastructure/        # Infrastructure Layer (Technical Implementation)
│   ├── customer/
│   │   └── persistence/   # JPA entities, repository implementations
│   ├── order/
│   │   └── persistence/   # Order persistence adapters
│   ├── event/             # Event handling infrastructure
│   └── config/            # Spring configuration
├── interfaces/            # Interface Layer (Controllers)
│   └── web/               # REST controllers and DTOs
├── config/                # Application configuration
├── exceptions/            # Global exception handling
└── utils/                 # Utility classes
```

## DDD Package Conventions

### Domain Layer Structure
Each bounded context follows this pattern:
```
domain/{context}/
├── model/
│   ├── aggregate/         # Aggregate roots (@AggregateRoot)
│   ├── entity/           # Entities (@Entity)
│   ├── valueobject/      # Value objects (@ValueObject)
│   ├── specification/    # Business rules (@Specification)
│   └── policy/           # Domain policies (@Policy)
├── events/               # Domain events (implements DomainEvent)
├── repository/           # Repository interfaces
└── service/              # Domain services (@DomainService)
```

#### Specification Pattern
- **Purpose**: Encapsulate business rules and validation logic
- **Location**: `domain/{context}/model/specification/`
- **Annotation**: `@Specification`
- **Interface**: Implements `Specification<T>`
- **Usage**: Complex business rule validation, query criteria

#### Policy Pattern  
- **Purpose**: Encapsulate business policies that may change
- **Location**: `domain/{context}/model/policy/`
- **Annotation**: `@Policy`
- **Interface**: Implements `DomainPolicy<T, R>`
- **Usage**: Business calculations, decision making, rule application

### Infrastructure Layer Structure
```
infrastructure/{context}/
├── persistence/
│   ├── entity/           # JPA entities
│   ├── repository/       # Repository implementations
│   └── mapper/           # Domain ↔ JPA mapping
└── acl/                  # Anti-corruption layer for external systems
```

## Frontend Structure

### CMC Frontend (Next.js 14)
```
cmc-frontend/
├── src/
│   ├── app/              # App Router pages
│   │   ├── customers/    # Customer management pages
│   │   ├── orders/       # Order management pages
│   │   ├── products/     # Product management pages
│   │   └── api-test/     # API testing utilities
│   ├── components/       # Reusable UI components
│   │   ├── ui/           # shadcn/ui components
│   │   ├── customer/     # Customer-specific components
│   │   ├── order/        # Order-specific components
│   │   └── product/      # Product-specific components
│   ├── hooks/            # Custom React hooks
│   ├── lib/              # Utility libraries and store
│   ├── services/         # API service layer
│   └── types/            # TypeScript type definitions
├── public/               # Static assets
└── package.json          # Dependencies and scripts
```

### Consumer Frontend (Angular 18)
```
consumer-frontend/
├── src/
│   ├── app/
│   │   ├── core/         # Core services and models
│   │   │   ├── models/   # Domain models
│   │   │   └── services/ # API services
│   │   ├── features/     # Feature modules
│   │   └── layout/       # Layout components
│   └── environments/     # Environment configurations
└── package.json          # Dependencies and scripts
```

## Testing Structure

```
app/src/test/java/solid/humank/genaidemo/
├── architecture/         # ArchUnit tests for DDD compliance
├── bdd/                  # Cucumber step definitions
│   ├── customer/         # Customer BDD tests
│   ├── inventory/        # Inventory BDD tests
│   └── order/            # Order BDD tests
├── domain/               # Domain unit tests
├── infrastructure/       # Infrastructure integration tests
├── testutils/            # Test utilities and builders
│   ├── builders/         # Test data builders
│   ├── handlers/         # Scenario handlers
│   └── matchers/         # Custom test matchers
└── ApplicationContextTest.java # Spring context test
```

### Feature Files Structure
```
app/src/test/resources/features/
├── customer/             # Customer-related scenarios
├── inventory/            # Inventory management scenarios
├── order/                # Order processing scenarios
├── payment/              # Payment processing scenarios
└── workflow/             # End-to-end workflow scenarios
```

## Documentation Structure

```
docs/
├── api/                  # API documentation
├── en/                   # English documentation
├── uml/                  # UML diagrams and PlantUML sources
├── releases/             # Release notes and changelogs
└── requirements/         # Business requirements and analysis
```

## Configuration Files

### Root Level
- `settings.gradle` - Multi-module Gradle configuration
- `gradle.properties` - Gradle build properties
- `docker-compose.yml` - Multi-container orchestration
- `lombok.config` - Lombok configuration

### Application Level
- `app/build.gradle` - Main application build configuration
- `app/lombok.config` - Application-specific Lombok settings

## Key Architectural Rules

### Layer Dependencies

#### Dependency Flow Diagram
```
┌─────────────┐    depends on    ┌─────────────┐    depends on    ┌─────────────┐
│ Interfaces  │ ───────────────→ │ Application │ ───────────────→ │   Domain    │
│   Layer     │                  │    Layer    │                  │    Layer    │
└─────────────┘                  └─────────────┘                  └─────────────┘
                                                                          ↑
                                                                          │
                                                                   implements
                                                                   interfaces
                                                                          │
                                                                  ┌─────────────┐
                                                                  │Infrastructure│
                                                                  │    Layer    │
                                                                  └─────────────┘
```

#### Detailed Explanation

1. **Interfaces → Application → Domain**
   - **Interfaces Layer** (REST Controllers) depends on **Application Layer** (Use Cases)
   - **Application Layer** (Use Cases) depends on **Domain Layer** (Business Logic)
   - This forms a unidirectional dependency chain, ensuring outer layers depend on inner layers

2. **Domain ← Infrastructure (Reverse Implementation)**
   - **Infrastructure Layer** implements interfaces defined in **Domain Layer**
   - Domain Layer defines abstract interfaces (e.g., `OrderRepository`)
   - Infrastructure Layer provides concrete implementations (e.g., `JpaOrderRepositoryAdapter`)
   - This embodies the Dependency Inversion Principle (DIP)

3. **Domain Layer Independence**
   - Domain Layer is the system's core, with no dependencies on other layers
   - Contains pure business logic, aggregate roots, entities, value objects
   - Only defines interfaces, doesn't care about technical implementation details

4. **Application Layer Coordination Role**
   - Coordinates multiple Domain objects to complete business use cases
   - Calls Domain services and aggregate root methods
   - Interacts with Infrastructure through Domain interfaces (dependency injection)

#### Practical Example

```java
// Domain Layer - Define interfaces
public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(OrderId id);
}

// Application Layer - Use interfaces
@Service
public class OrderApplicationService {
    private final OrderRepository orderRepository;
    
    public void createOrder(CreateOrderCommand command) {
        Order order = new Order(command.getCustomerId(), command.getItems());
        orderRepository.save(order);
    }
}

// Infrastructure Layer - Implement interfaces
@Repository
public class JpaOrderRepositoryAdapter implements OrderRepository {
    // Concrete JPA implementation
}
```

#### Dependency Injection Configuration

The real repository implementations are injected at runtime through Spring's dependency injection:

```java
// Spring Configuration - Infrastructure Layer
@Configuration
@EnableJpaRepositories(basePackages = "solid.humank.genaidemo.infrastructure")
public class PersistenceConfig {
    
    @Bean
    public OrderRepository orderRepository(JpaOrderRepository jpaRepository) {
        return new JpaOrderRepositoryAdapter(jpaRepository);
    }
}

// Application Service receives the real implementation
@Service
public class OrderApplicationService {
    // Spring injects JpaOrderRepositoryAdapter at runtime
    private final OrderRepository orderRepository;
    
    public OrderApplicationService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
}
```

**When Real Implementation is Injected:**
- **Production Runtime**: Spring injects `JpaOrderRepositoryAdapter` (real database)
- **Integration Tests**: Spring injects `JpaOrderRepositoryAdapter` (test database)
- **Unit Tests**: Manually inject mock implementations (`Mockito.mock(OrderRepository.class)`)
- **BDD Tests**: Use test doubles or in-memory implementations for isolated testing

#### Benefits
- **Testability**: Domain logic can be tested independently without real databases
- **Maintainability**: Business logic is separated from technical implementation
- **Extensibility**: Technical implementations can be easily replaced (e.g., JPA to MongoDB)
- **Clear Responsibilities**: Each layer has well-defined responsibility boundaries
- **Runtime Flexibility**: Different implementations can be injected based on context (production, testing, etc.)

### Package Naming Conventions
- Domain packages: `domain.{context}.{pattern}`
- Application packages: `application.{context}`
- Infrastructure packages: `infrastructure.{context}.{technology}`
- Interface packages: `interfaces.{protocol}`

### File Organization Principles
- Group by feature/bounded context, not by technical layer
- Keep related domain concepts together
- Separate technical concerns in infrastructure
- Use consistent naming patterns across contexts

## Development Workflow

### Adding New Features
1. Start with domain modeling in `domain/{context}/`
2. Define application services in `application/{context}/`
3. Implement infrastructure adapters in `infrastructure/{context}/`
4. Add REST endpoints in `interfaces/web/`
5. Write BDD tests in `src/test/resources/features/`
6. Implement step definitions in `src/test/java/.../bdd/`

### Testing Strategy
- Domain logic: Unit tests with Cucumber BDD
- Application services: Integration tests
- Infrastructure: Repository and adapter tests
- Interfaces: Controller and API tests
- Architecture: ArchUnit tests for compliance
