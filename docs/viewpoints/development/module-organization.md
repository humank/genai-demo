# Module Organization

> **Last Updated**: 2025-10-23  
> **Status**: Active  
> **Stakeholders**: Developers, Architects

## Overview

This document provides a detailed description of the module organization, package structure, and bounded context implementation in the Enterprise E-Commerce Platform. Understanding this structure is essential for navigating the codebase and contributing effectively.

## Package Structure Standards

### Root Package

All code resides under the root package:

```text
solid.humank.genaidemo
```

### Layer Packages

The system is organized into four primary layers following Hexagonal Architecture:

```text
solid.humank.genaidemo/
├── domain/              # Domain Layer - Business Logic
├── application/         # Application Layer - Use Case Orchestration
├── infrastructure/      # Infrastructure Layer - Technical Implementations
└── interfaces/          # Interfaces Layer - External Communication
```

## Domain Layer Organization

### Structure

The domain layer is organized by bounded contexts, with each context being completely independent:

```text
domain/
├── customer/           # Customer Bounded Context
├── order/              # Order Bounded Context
├── product/            # Product Bounded Context
├── payment/            # Payment Bounded Context
├── inventory/          # Inventory Bounded Context
├── promotion/          # Promotion Bounded Context
├── logistics/          # Logistics Bounded Context
├── notification/       # Notification Bounded Context
├── review/             # Review Bounded Context
├── shoppingcart/       # Shopping Cart Bounded Context
├── pricing/            # Pricing Bounded Context
├── seller/             # Seller Bounded Context
├── delivery/           # Delivery Bounded Context
└── shared/             # Shared Kernel
```

### Bounded Context Internal Structure

Each bounded context follows a consistent internal structure:

```text
domain/{context}/
├── model/
│   ├── aggregate/      # Aggregate Roots
│   ├── entity/         # Entities (non-aggregate roots)
│   ├── valueobject/    # Value Objects
│   └── specification/  # Business Rule Specifications
├── events/             # Domain Events (Records)
├── repository/         # Repository Interfaces
├── service/            # Domain Services
└── validation/         # Domain Validation Logic
```

### Example: Customer Bounded Context

```text
domain/customer/
├── model/
│   ├── aggregate/
│   │   └── Customer.java                    # Customer Aggregate Root
│   ├── entity/
│   │   ├── CustomerProfile.java             # Profile Entity
│   │   └── CustomerAddress.java             # Address Entity
│   ├── valueobject/
│   │   ├── CustomerId.java                  # Customer ID Value Object
│   │   ├── Email.java                       # Email Value Object
│   │   ├── Phone.java                       # Phone Value Object
│   │   ├── MembershipLevel.java             # Membership Level Enum
│   │   └── CustomerStatus.java              # Status Enum
│   └── specification/
│       ├── CustomerEligibilitySpec.java     # Eligibility Rules
│       └── DiscountEligibilitySpec.java     # Discount Rules
├── events/
│   ├── CustomerCreatedEvent.java            # Customer Created Event
│   ├── CustomerProfileUpdatedEvent.java     # Profile Updated Event
│   ├── CustomerStatusChangedEvent.java      # Status Changed Event
│   └── CustomerDeletedEvent.java            # Customer Deleted Event
├── repository/
│   └── CustomerRepository.java              # Repository Interface
├── service/
│   ├── CustomerDomainService.java           # Domain Service
│   └── CustomerValidationService.java       # Validation Service
└── validation/
    └── CustomerValidator.java               # Validator
```

### Example: Order Bounded Context

```text
domain/order/
├── model/
│   ├── aggregate/
│   │   └── Order.java                       # Order Aggregate Root
│   ├── entity/
│   │   ├── OrderItem.java                   # Order Item Entity
│   │   └── OrderHistory.java                # Order History Entity
│   ├── valueobject/
│   │   ├── OrderId.java                     # Order ID
│   │   ├── OrderStatus.java                 # Order Status
│   │   ├── Money.java                       # Money Value Object
│   │   ├── Quantity.java                    # Quantity Value Object
│   │   └── ShippingAddress.java             # Shipping Address
│   └── specification/
│       ├── OrderSubmissionSpec.java         # Submission Rules
│       └── OrderCancellationSpec.java       # Cancellation Rules
├── events/
│   ├── OrderCreatedEvent.java               # Order Created
│   ├── OrderSubmittedEvent.java             # Order Submitted
│   ├── OrderConfirmedEvent.java             # Order Confirmed
│   ├── OrderShippedEvent.java               # Order Shipped
│   ├── OrderDeliveredEvent.java             # Order Delivered
│   └── OrderCancelledEvent.java             # Order Cancelled
├── repository/
│   └── OrderRepository.java                 # Repository Interface
├── service/
│   ├── OrderDomainService.java              # Domain Service
│   └── OrderPricingService.java             # Pricing Service
└── validation/
    └── OrderValidator.java                  # Validator
```

### Shared Kernel

The shared kernel contains common value objects and utilities used across multiple bounded contexts:

```text
domain/shared/
├── valueobject/
│   ├── Money.java                           # Money (amount + currency)
│   ├── Address.java                         # Physical Address
│   ├── PhoneNumber.java                     # Phone Number
│   ├── EmailAddress.java                    # Email Address
│   └── DateRange.java                       # Date Range
├── exception/
│   ├── DomainException.java                 # Base Domain Exception
│   ├── BusinessRuleViolationException.java  # Business Rule Violation
│   └── ResourceNotFoundException.java       # Resource Not Found
└── util/
    ├── DomainEventPublisher.java            # Event Publisher Interface
    └── AggregateRoot.java                   # Base Aggregate Root
```

## Application Layer Organization

### Structure

The application layer is organized by bounded contexts, mirroring the domain structure:

```text
application/
├── customer/           # Customer Use Cases
├── order/              # Order Use Cases
├── product/            # Product Use Cases
├── payment/            # Payment Use Cases
├── inventory/          # Inventory Use Cases
├── promotion/          # Promotion Use Cases
├── logistics/          # Logistics Use Cases
├── notification/       # Notification Use Cases
├── review/             # Review Use Cases
├── shoppingcart/       # Shopping Cart Use Cases
├── pricing/            # Pricing Use Cases
├── seller/             # Seller Use Cases
└── delivery/           # Delivery Use Cases
```

### Application Context Internal Structure

```text
application/{context}/
├── {UseCase}ApplicationService.java         # Application Service
├── command/                                  # Command Objects
│   ├── Create{Entity}Command.java
│   ├── Update{Entity}Command.java
│   └── Delete{Entity}Command.java
├── query/                                    # Query Objects
│   ├── {Entity}Query.java
│   └── {Entity}SearchCriteria.java
└── dto/                                      # Data Transfer Objects
    ├── {Entity}Request.java
    ├── {Entity}Response.java
    └── {Entity}Summary.java
```

### Example: Customer Application Layer

```text
application/customer/
├── CustomerApplicationService.java          # Main Application Service
├── command/
│   ├── CreateCustomerCommand.java           # Create Command
│   ├── UpdateCustomerProfileCommand.java    # Update Command
│   ├── ChangeCustomerStatusCommand.java     # Status Change Command
│   └── DeleteCustomerCommand.java           # Delete Command
├── query/
│   ├── CustomerQuery.java                   # Query Object
│   ├── CustomerSearchCriteria.java          # Search Criteria
│   └── CustomerListQuery.java               # List Query
└── dto/
    ├── CustomerDto.java                     # Customer DTO
    ├── CustomerSummaryDto.java              # Summary DTO
    └── CustomerDetailDto.java               # Detail DTO
```

### Example: Order Application Layer

```text
application/order/
├── OrderApplicationService.java             # Main Application Service
├── command/
│   ├── CreateOrderCommand.java              # Create Order
│   ├── SubmitOrderCommand.java              # Submit Order
│   ├── ConfirmOrderCommand.java             # Confirm Order
│   ├── ShipOrderCommand.java                # Ship Order
│   └── CancelOrderCommand.java              # Cancel Order
├── query/
│   ├── OrderQuery.java                      # Order Query
│   ├── OrderSearchCriteria.java             # Search Criteria
│   └── OrderHistoryQuery.java               # History Query
└── dto/
    ├── OrderDto.java                        # Order DTO
    ├── OrderSummaryDto.java                 # Summary DTO
    ├── OrderItemDto.java                    # Order Item DTO
    └── OrderStatusDto.java                  # Status DTO
```

## Infrastructure Layer Organization

### Structure

The infrastructure layer implements technical concerns and adapters:

```text
infrastructure/
├── customer/           # Customer Infrastructure
├── order/              # Order Infrastructure
├── product/            # Product Infrastructure
├── payment/            # Payment Infrastructure
├── inventory/          # Inventory Infrastructure
├── promotion/          # Promotion Infrastructure
├── logistics/          # Logistics Infrastructure
├── notification/       # Notification Infrastructure
├── review/             # Review Infrastructure
├── shoppingcart/       # Shopping Cart Infrastructure
├── pricing/            # Pricing Infrastructure
├── seller/             # Seller Infrastructure
├── delivery/           # Delivery Infrastructure
├── config/             # Configuration
├── event/              # Event Publishing
├── messaging/          # Message Broker Integration
├── cache/              # Caching Implementation
└── external/           # External Service Adapters
```

### Infrastructure Context Internal Structure

```text
infrastructure/{context}/
├── persistence/
│   ├── entity/                              # JPA Entities
│   │   ├── {Entity}Entity.java
│   │   └── {Entity}ItemEntity.java
│   ├── mapper/                              # Domain ↔ Entity Mappers
│   │   └── {Entity}Mapper.java
│   └── repository/                          # Repository Implementations
│       ├── Jpa{Entity}Repository.java       # Spring Data JPA Repository
│       └── {Entity}RepositoryImpl.java      # Custom Implementation
├── messaging/
│   ├── publisher/                           # Event Publishers
│   │   └── {Entity}EventPublisher.java
│   └── consumer/                            # Event Consumers
│       └── {Entity}EventConsumer.java
└── adapter/                                 # External Service Adapters
    └── {Service}Adapter.java
```

### Example: Customer Infrastructure

```text
infrastructure/customer/
├── persistence/
│   ├── entity/
│   │   ├── CustomerEntity.java              # JPA Entity
│   │   ├── CustomerProfileEntity.java       # Profile Entity
│   │   └── CustomerAddressEntity.java       # Address Entity
│   ├── mapper/
│   │   ├── CustomerMapper.java              # Customer Mapper
│   │   └── CustomerProfileMapper.java       # Profile Mapper
│   └── repository/
│       ├── JpaCustomerRepository.java       # Spring Data Repository
│       └── CustomerRepositoryImpl.java      # Implementation
├── messaging/
│   ├── publisher/
│   │   └── CustomerEventPublisher.java      # Event Publisher
│   └── consumer/
│       └── CustomerEventConsumer.java       # Event Consumer
└── adapter/
    └── EmailServiceAdapter.java            # Email Service Adapter
```

### Example: Order Infrastructure

```text
infrastructure/order/
├── persistence/
│   ├── entity/
│   │   ├── OrderEntity.java                 # Order JPA Entity
│   │   ├── OrderItemEntity.java             # Order Item Entity
│   │   └── OrderHistoryEntity.java          # History Entity
│   ├── mapper/
│   │   ├── OrderMapper.java                 # Order Mapper
│   │   └── OrderItemMapper.java             # Item Mapper
│   └── repository/
│       ├── JpaOrderRepository.java          # Spring Data Repository
│       └── OrderRepositoryImpl.java         # Implementation
├── messaging/
│   ├── publisher/
│   │   └── OrderEventPublisher.java         # Event Publisher
│   └── consumer/
│       ├── OrderEventConsumer.java          # Event Consumer
│       └── PaymentEventConsumer.java        # Payment Event Handler
└── adapter/
    ├── InventoryServiceAdapter.java         # Inventory Adapter
    └── PaymentServiceAdapter.java           # Payment Adapter
```

### Cross-Cutting Infrastructure

```text
infrastructure/
├── config/
│   ├── DatabaseConfiguration.java           # Database Config
│   ├── RedisConfiguration.java              # Redis Config
│   ├── KafkaConfiguration.java              # Kafka Config
│   ├── SecurityConfiguration.java           # Security Config
│   └── ObservabilityConfiguration.java      # Observability Config
├── event/
│   ├── DomainEventPublisherAdapter.java     # Event Publisher
│   ├── DomainEventApplicationService.java   # Event Service
│   └── EventStore.java                      # Event Store
├── messaging/
│   ├── KafkaProducer.java                   # Kafka Producer
│   ├── KafkaConsumer.java                   # Kafka Consumer
│   └── MessageConverter.java                # Message Converter
├── cache/
│   ├── RedisCacheManager.java               # Cache Manager
│   └── CacheConfiguration.java              # Cache Config
└── external/
    ├── PaymentGatewayAdapter.java           # Payment Gateway
    ├── EmailServiceAdapter.java             # Email Service
    └── ShippingProviderAdapter.java         # Shipping Provider
```

## Interfaces Layer Organization

### Structure

The interfaces layer exposes the system to external clients:

```text
interfaces/
├── rest/               # REST API Controllers
│   ├── customer/      # Customer API
│   ├── order/         # Order API
│   ├── product/       # Product API
│   ├── payment/       # Payment API
│   ├── inventory/     # Inventory API
│   ├── promotion/     # Promotion API
│   ├── logistics/     # Logistics API
│   ├── notification/  # Notification API
│   ├── review/        # Review API
│   ├── shoppingcart/  # Shopping Cart API
│   ├── pricing/       # Pricing API
│   ├── seller/        # Seller API
│   └── delivery/      # Delivery API
└── web/               # Web UI Controllers (if applicable)
```

### REST API Context Structure

```text
interfaces/rest/{context}/
├── controller/
│   └── {Entity}Controller.java              # REST Controller
├── dto/
│   ├── request/                             # Request DTOs
│   │   ├── Create{Entity}Request.java
│   │   ├── Update{Entity}Request.java
│   │   └── {Entity}SearchRequest.java
│   └── response/                            # Response DTOs
│       ├── {Entity}Response.java
│       ├── {Entity}ListResponse.java
│       └── {Entity}DetailResponse.java
└── mapper/
    └── {Entity}DtoMapper.java               # DTO ↔ Domain Mapper
```

### Example: Customer REST API

```text
interfaces/rest/customer/
├── controller/
│   └── CustomerController.java              # Customer API Controller
├── dto/
│   ├── request/
│   │   ├── CreateCustomerRequest.java       # Create Request
│   │   ├── UpdateCustomerRequest.java       # Update Request
│   │   └── CustomerSearchRequest.java       # Search Request
│   └── response/
│       ├── CustomerResponse.java            # Customer Response
│       ├── CustomerListResponse.java        # List Response
│       └── CustomerDetailResponse.java      # Detail Response
└── mapper/
    └── CustomerDtoMapper.java               # DTO Mapper
```

### Example: Order REST API

```text
interfaces/rest/order/
├── controller/
│   └── OrderController.java                 # Order API Controller
├── dto/
│   ├── request/
│   │   ├── CreateOrderRequest.java          # Create Request
│   │   ├── SubmitOrderRequest.java          # Submit Request
│   │   ├── UpdateOrderRequest.java          # Update Request
│   │   └── OrderSearchRequest.java          # Search Request
│   └── response/
│       ├── OrderResponse.java               # Order Response
│       ├── OrderListResponse.java           # List Response
│       ├── OrderDetailResponse.java         # Detail Response
│       └── OrderStatusResponse.java         # Status Response
└── mapper/
    └── OrderDtoMapper.java                  # DTO Mapper
```

## Naming Conventions

### Package Names

- **Lowercase**: All package names use lowercase
- **Singular**: Use singular nouns (e.g., `customer`, not `customers`)
- **Descriptive**: Clear, descriptive names (e.g., `valueobject`, not `vo`)

### Class Names

- **PascalCase**: All class names use PascalCase
- **Suffixes**: Use consistent suffixes:
  - Aggregate Roots: No suffix (e.g., `Customer`, `Order`)
  - Entities: No suffix (e.g., `OrderItem`, `CustomerProfile`)
  - Value Objects: Descriptive name (e.g., `Email`, `Money`, `CustomerId`)
  - Domain Events: `Event` suffix (e.g., `CustomerCreatedEvent`)
  - Repositories: `Repository` suffix (e.g., `CustomerRepository`)
  - Services: `Service` suffix (e.g., `CustomerDomainService`)
  - Controllers: `Controller` suffix (e.g., `CustomerController`)
  - DTOs: `Request`/`Response`/`Dto` suffix

### File Organization

- **One Class Per File**: Each class in its own file
- **File Name = Class Name**: File name matches class name exactly
- **Package = Directory**: Package structure mirrors directory structure

## Module Dependencies

### Allowed Dependencies

```text
Domain Layer:
  ✅ Java Standard Library
  ✅ Domain-specific libraries (e.g., Money API)
  ✅ Other domain packages within same context
  ✅ Shared kernel
  ❌ NO other layers
  ❌ NO Spring Framework (except @Component for services)
  ❌ NO JPA annotations

Application Layer:
  ✅ Domain Layer
  ✅ Spring Framework (for transactions)
  ❌ NO Infrastructure Layer
  ❌ NO Interfaces Layer

Infrastructure Layer:
  ✅ Domain Layer (interfaces only)
  ✅ Spring Framework
  ✅ JPA/Hibernate
  ✅ External libraries
  ❌ NO Application Layer
  ❌ NO Interfaces Layer

Interfaces Layer:
  ✅ Application Layer
  ✅ Domain Layer
  ✅ Spring Web
  ✅ Validation libraries
  ❌ NO Infrastructure Layer (except via dependency injection)
```

### Dependency Enforcement

Dependencies are enforced through:

1. **ArchUnit Tests**: Automated architecture tests
2. **Code Reviews**: Manual verification
3. **Package Visibility**: Package-private classes where appropriate
4. **Gradle Modules**: Separate Gradle modules for each layer (future enhancement)

## Best Practices

### Package Organization

1. **Keep Contexts Independent**: No direct dependencies between bounded contexts
2. **Use Shared Kernel Sparingly**: Only for truly shared concepts
3. **Consistent Structure**: All contexts follow the same internal structure
4. **Clear Boundaries**: Each package has a clear, single responsibility

### Class Organization

1. **Small Classes**: Keep classes focused and small (< 200 lines)
2. **Single Responsibility**: Each class has one clear purpose
3. **Cohesion**: Related classes in the same package
4. **Loose Coupling**: Minimize dependencies between packages

### File Organization

1. **Logical Grouping**: Group related files together
2. **Flat Structure**: Avoid deep nesting (max 3-4 levels)
3. **Clear Names**: Use descriptive, unambiguous names
4. **Consistent Naming**: Follow naming conventions consistently

## Navigation

### Related Documents

- [← Overview](overview.md) - Development Viewpoint overview
- [Dependency Rules](dependency-rules.md) - Architecture constraints →
- [Build Process](build-process.md) - Build and deployment

### Related Viewpoints

- [Functional Viewpoint](../functional/bounded-contexts.md) - Bounded context descriptions
- [Information Viewpoint](../information/domain-models.md) - Domain models

### Development Guides

- [Coding Standards](../../development/coding-standards/java-standards.md)
- [Creating Aggregates](../../development/examples/creating-aggregate.md)
- [Adding Endpoints](../../development/examples/adding-endpoint.md)

---

**Previous**: [← Overview](overview.md) | **Next**: [Dependency Rules →](dependency-rules.md)
