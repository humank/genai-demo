# 01: Introduction to Domain-Driven Design (DDD)

Domain-Driven Design (DDD) is an approach to software development that emphasizes a deep understanding of the domain, or the business area, the software is being built for. It was first introduced by Eric Evans in his book "Domain-Driven Design: Tackling Complexity in the Heart of Software".

The core idea of DDD is to model the software to reflect the real-world domain, making the software more intuitive, maintainable, and aligned with business needs.

## Key Concepts in DDD

DDD introduces several key concepts and patterns to help manage complexity:

*   **Ubiquitous Language:** A common language shared by domain experts, developers, and other stakeholders. This language is used in all forms of communication, including code, diagrams, and documentation, to eliminate ambiguity.
*   **Bounded Context:** A specific boundary within which a particular domain model is defined and applicable. Different bounded contexts may have their own ubiquitous language and models for the same concepts. For example, the concept of a "Product" might be different in a "Sales" context versus a "Shipping" context.
*   **Entities:** Objects that have a distinct identity that runs through time and different states. Entities are not defined by their attributes but by their thread of continuity. For example, a `User` entity is unique even if their name or address changes.
*   **Value Objects:** Objects that represent descriptive aspects of the domain with no conceptual identity. They are defined by their attributes. For example, an `Address` (street, city, zip code) can be a value object. Two value objects are considered equal if all their attributes are equal. They are typically immutable.
*   **Aggregates:** A cluster of associated entities and value objects treated as a single unit for data changes. Each aggregate has a root entity, known as the Aggregate Root. The aggregate root is the only member of the aggregate that outside objects are allowed to hold references to. This ensures the consistency of the aggregate.
*   **Repositories:** Provide an abstraction for accessing and persisting aggregates. They mediate between the domain and data mapping layers, behaving like in-memory collections of domain objects.
*   **Domain Services:** Operations or logic that don't naturally fit within an entity or value object. These services typically involve multiple domain objects.
*   **Domain Events:** Significant occurrences within the domain that domain experts care about. For example, `OrderPlaced` or `InventoryUpdated`. These are often used to communicate changes between different parts of the system or between bounded contexts.

## DDD vs. Anemic Domain Model

A common pitfall in software development is the **Anemic Domain Model**, where domain objects are primarily data containers with little to no behavior. In this approach, business logic is often placed in service layers or transaction scripts, leading to a procedural style of programming.

| Feature          | Domain-Driven Design (Rich Domain Model)             | Anemic Domain Model                               |
| ---------------- | ---------------------------------------------------- | ------------------------------------------------- |
| **Behavior**     | Domain objects encapsulate both data and behavior.   | Domain objects are primarily data holders (getters/setters). |
| **Logic**        | Business logic resides within the domain objects.    | Business logic is external to domain objects (e.g., in services). |
| **Encapsulation**| Strong encapsulation, protecting invariants.         | Weak encapsulation, often exposing internal state. |
| **Complexity**   | Manages complexity by aligning with the domain.      | Can lead to scattered logic and increased complexity. |
| **Maintainability**| Easier to maintain and evolve as domain logic is co-located with data. | Harder to maintain due to dispersed logic.       |
| **Testability**  | Domain logic is easier to unit test in isolation.    | Business logic is harder to test without involving multiple classes. |

**In DDD, the domain model is rich with behavior.** Entities and value objects are not just bags of data; they contain methods that implement business rules and invariants related to the data they hold. This makes the model more expressive and closer to the mental model of domain experts.

## Visualizing DDD Concepts

We can use diagrams to visualize the relationships between DDD concepts. Below is a Mermaid diagram illustrating a simple `Order` aggregate:

```mermaid
graph TD
    subgraph "Order Aggregate"
        direction LR
        AR[Order (Aggregate Root)]
        Entity1[OrderItem (Entity)]
        VO1[Money (Value Object)]
        VO2[Address (Value Object) - Shipping]
        VO3[Address (Value Object) - Billing]
    end

    AR --> Entity1
    AR --> VO2
    AR --> VO3
    Entity1 --> VO1

    subgraph "Customer Aggregate"
        direction LR
        AR_Cust[Customer (Aggregate Root)]
        VO_Cust_Details[CustomerDetails (Value Object)]
    end

    AR --- AR_Cust

    classDef default fill:#f9f9f9,stroke:#333,stroke-width:2px;
    classDef ar fill:#lightcoral,stroke:#A00,stroke-width:2px;
    classDef entity fill:#lightgoldenrodyellow,stroke:#B8860B,stroke-width:2px;
    classDef vo fill:#lightblue,stroke:#0000CD,stroke-width:2px;

    class AR,AR_Cust ar;
    class Entity1 entity;
    class VO1,VO2,VO3,VO_Cust_Details vo;
```

This diagram shows an `Order` aggregate with `Order` as the aggregate root. It contains `OrderItem` entities and `Money` and `Address` value objects. The `Order` might also reference a `Customer` aggregate root.

## Examples from This Project's Codebase

This project, `genaidemo`, demonstrates several DDD concepts, particularly within its `domain` layer (`app/src/main/java/solid/humank/genaidemo/domain/`).

Let's look at some specific examples:

### Aggregate Root: `Order`

*   **Path**: `app/src/main/java/solid/humank/genaidemo/domain/order/model/aggregate/Order.java`
*   **Description**: The `Order` class is explicitly annotated with `@AggregateRoot`. It encapsulates the core logic and state related to an order.
*   **Responsibilities**:
    *   Manages a list of `OrderItem` value objects.
    *   Maintains the order's `status` (e.g., `CREATED`, `PAID`, `SHIPPED`).
    *   Calculates `totalAmount` and `effectiveAmount`.
    *   Handles state transitions (e.g., `submit()`, `confirm()`, `cancel()`).
    *   Ensures invariants, such as not allowing items to be added to an order that is not in the `CREATED` state.
    *   Publishes domain events like `OrderCreatedEvent` and `OrderItemAddedEvent`.

    ```java
    // Snippet from Order.java
    @AggregateRoot
    public class Order {
        private final OrderId id;
        private final CustomerId customerId;
        private final List<OrderItem> items;
        private OrderStatus status;
        private Money totalAmount;

        public void addItem(String productId, String productName, int quantity, Money price) {
            if (status != OrderStatus.CREATED) {
                throw new IllegalStateException("Cannot add items to an order that is not in CREATED state");
            }
            OrderItem item = new OrderItem(productId, productName, quantity, price);
            items.add(item);
            totalAmount = totalAmount.add(item.getSubtotal());
            // ... publish OrderItemAddedEvent
        }

        public void submit() {
            if (items.isEmpty()) {
                throw new IllegalStateException("Cannot submit an order with no items");
            }
            status = OrderStatus.PENDING;
            // ...
        }
        // ... other methods and getters
    }
    ```

### Value Objects

The project makes extensive use of Value Objects, primarily located in `app/src/main/java/solid/humank/genaidemo/domain/common/valueobject/`.

1.  **`Money.java`**:
    *   **Path**: `app/src/main/java/solid/humank/genaidemo/domain/common/valueobject/Money.java`
    *   **Description**: Represents a monetary value with an amount and currency. It's immutable and provides methods for arithmetic operations (`add`, `subtract`, `multiply`), ensuring that operations between different currencies are handled correctly (or prevented).
    *   **Characteristics**: Immutability, equality based on attribute values, self-validation (e.g., currency matching).

    ```java
    // Snippet from Money.java
    @ValueObject
    public class Money {
        private final BigDecimal amount;
        private final Currency currency;

        public Money(BigDecimal amount, Currency currency) {
            this.amount = amount;
            this.currency = currency;
        }

        public Money add(Money money) {
            if (!this.currency.equals(money.currency)) {
                throw new IllegalArgumentException("Cannot add money with different currencies");
            }
            return new Money(this.amount.add(money.amount), this.currency);
        }
        // ... other methods
    }
    ```

2.  **`OrderItem.java`**:
    *   **Path**: `app/src/main/java/solid/humank/genaidemo/domain/common/valueobject/OrderItem.java`
    *   **Description**: Represents an item within an order, including product details, quantity, and price. It's immutable and calculates its subtotal. In this design, `OrderItem` is treated as a Value Object because its identity is primarily defined by its attributes, and it's fully owned by the `Order` aggregate.
    *   **Characteristics**: Immutability, equality based on attributes, contains logic relevant to its attributes (e.g., `getSubtotal()`).

    ```java
    // Snippet from OrderItem.java
    @ValueObject
    public class OrderItem {
        private final String productId;
        private final String productName;
        private final int quantity;
        private final Money price;

        public OrderItem(String productId, String productName, int quantity, Money price) {
            // ... validation ...
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.price = price;
        }

        public Money getSubtotal() {
            return price.multiply(quantity);
        }
        // ... other getters, equals, hashCode
    }
    ```

3.  **Identifier Value Objects (`OrderId.java`, `CustomerId.java`)**:
    *   **Path**: `.../common/valueobject/OrderId.java`, `.../common/valueobject/CustomerId.java`
    *   **Description**: These classes wrap primitive types (like `UUID` or `String`) to create strongly-typed identifiers. This prevents errors like accidentally passing a `ProductId` where an `OrderId` is expected.
    *   **Characteristics**: Immutability, strong typing, often generated by factories or the VO itself (e.g., `OrderId.generate()`).

    ```java
    // Snippet from OrderId.java
    @ValueObject
    public class OrderId {
        private final UUID id;

        public OrderId(UUID id) {
            this.id = Objects.requireNonNull(id, "Order ID cannot be null");
        }

        public static OrderId generate() {
            return new OrderId(UUID.randomUUID());
        }
        // ...
    }
    ```

4.  **`OrderStatus.java` (Enum)**:
    *   **Path**: `app/src/main/java/solid/humank/genaidemo/domain/common/valueobject/OrderStatus.java`
    *   **Description**: An enum representing the various states an order can be in. Enums are inherently Value Objects as they represent a fixed set of named values. This enum also includes behavior (`canTransitionTo`) to manage valid state transitions.
    *   **Characteristics**: Fixed set of instances, type safety, can encapsulate state-related logic.

### Domain Events

The `Order` aggregate publishes domain events such as `OrderCreatedEvent` and `OrderItemAddedEvent` (found in `app/src/main/java/solid/humank/genaidemo/domain/order/model/events/`). These events signify important occurrences within the domain.

```java
// Example of an event from OrderCreatedEvent.java
public class OrderCreatedEvent extends DomainEvent {
    private final OrderId orderId;
    private final String customerId;
    // ... other fields, constructor, getters
}
```

These examples show how the `genaidemo` project applies DDD principles to structure its domain logic, making the code more aligned with the business domain, more maintainable, and easier to understand.

---
The project structure clearly separates concerns, with `application`, `domain`, and `infrastructure` layers, which is common in DDD-influenced architectures. The `domain` layer is rich with behavior and uses DDD patterns effectively.
