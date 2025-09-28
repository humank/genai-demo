# DDD Domain-Driven Design\n\n## Overview\n\nThis document provides a comprehensive DDD (Domain-Driven Design) guide, including tactical patterns, aggregate roots, value objects, entities, domain services, and domain events design and implementation.\n\n## 🎯 DDD Tactical Patterns\n\n### Design Principles\n\n- **Single Responsibility Principle (SRP)**: Each class has only one reason to change\n- **Open-Closed Principle (OCP)**: Open for extension, closed for modification\n- **Dependency Inversion Principle (DIP)**: Depend on abstractions, not concrete implementations\n\n### Architectural Patterns\n\n- **Hexagonal Architecture**: Clear boundaries and dependency directions\n- **DDD Tactical Patterns**: Aggregate roots, entities, value objects\n- **Event-Driven Architecture**: Loosely coupled component communication\n\n### Code Structure\n\n```\ndomain/\n├── model/          # Aggregate roots, entities, value objects\n├── events/         # Domain events\n└── services/       # Domain services\n\napplication/\n├── commands/       # Command handling\n├── queries/        # Query handling\n└── services/       # Application services\n\ninfrastructure/\n├── persistence/    # Data persistence\n├── messaging/      # Message handling\n└── external/       # External service integration\n```\n\n## @AggregateRoot Aggregate Root\n\n### Annotation Usage\n\nThe `@AggregateRoot` annotation is used to identify aggregate roots, providing aggregate metadata and configuration:\n\n```java\n@AggregateRoot(\n    name = \"Customer\",                    // Aggregate root name\n    description = \"Customer aggregate root\",  // Description\n    boundedContext = \"Customer\",          // Bounded context\n    version = \"2.0\",                     // Version number\n    enableEventCollection = true         // Enable event collection\n)\npublic class Customer implements AggregateRootInterface {\n    \n    private final CustomerId id;\n    private CustomerName name;\n    private Email email;\n    private Phone phone;\n    \n    public Customer(CustomerId id, CustomerName name, Email email, Phone phone) {\n        this.id = id;\n        this.name = name;\n        this.email = email;\n        this.phone = phone;\n        \n        // Publish customer created event\n        collectEvent(CustomerCreatedEvent.create(id, name, email, MembershipLevel.STANDARD));\n    }\n    \n    public void updateProfile(CustomerName newName, Email newEmail, Phone newPhone) {\n        // 1. Execute business logic validation\n        validateProfileUpdate(newName, newEmail, newPhone);\n        \n        // 2. Update state\n        this.name = newName;\n        this.email = newEmail;\n        this.phone = newPhone;\n        \n        // 3. Collect domain event\n        collectEvent(CustomerProfileUpdatedEvent.create(this.id, newName, newEmail, newPhone));\n    }\n    \n    // === Aggregate root event management methods provided by AggregateRootInterface ===\n    // No need to override any methods! All functionality provided by interface default methods:\n    // - collectEvent(DomainEvent event)\n    // - getUncommittedEvents()\n    // - markEventsAsCommitted()\n    // - hasUncommittedEvents()\n    // - getAggregateRootName()\n    // - getBoundedContext()\n    // - getVersion()\n}\n```\n\n### Event Collection Mechanism\n\n```java\npublic interface AggregateRootInterface {\n    \n    default void collectEvent(DomainEvent event) {\n        getEventCollector().collectEvent(event);\n    }\n    \n    default List<DomainEvent> getUncommittedEvents() {\n        return getEventCollector().getUncommittedEvents();\n    }\n    \n    default void markEventsAsCommitted() {\n        getEventCollector().markEventsAsCommitted();\n    }\n    \n    default boolean hasUncommittedEvents() {\n        return getEventCollector().hasUncommittedEvents();\n    }\n}\n```\n\n### Hybrid Approach Advantages\n\n- ✅ **Compile-time Constraints**: Must implement AggregateRootInterface, IDE will prompt\n- ✅ **Zero Override**: All event management methods have default implementations\n- ✅ **Annotation-Driven**: Provides metadata through @AggregateRoot\n- ✅ **Automatic Validation**: Automatically checks annotations in default methods\n\n## @ValueObject Value Objects\n\n### Record Implementation Pattern (Recommended)\n\nThe project extensively uses Java Records to implement value objects, ensuring immutability:\n\n```java\n@ValueObject(name = \"CustomerId\", description = \"Customer unique identifier\")\npublic record CustomerId(String value) {\n    \n    public CustomerId {\n        if (value == null || value.trim().isEmpty()) {\n            throw new IllegalArgumentException(\"Customer ID cannot be null or empty\");\n        }\n    }\n    \n    public static CustomerId generate() {\n        return new CustomerId(UUID.randomUUID().toString());\n    }\n    \n    public static CustomerId of(String value) {\n        return new CustomerId(value);\n    }\n}\n```\n\n### Composite Value Objects\n\n```java\n@ValueObject(name = \"RewardPoints\", description = \"Reward points value object\")\npublic record RewardPoints(int balance, LocalDateTime lastUpdated) {\n    \n    public RewardPoints {\n        if (balance < 0) {\n            throw new IllegalArgumentException(\"Reward points balance cannot be negative\");\n        }\n        if (lastUpdated == null) {\n            lastUpdated = LocalDateTime.now();\n        }\n    }\n    \n    public static RewardPoints empty() {\n        return new RewardPoints(0, LocalDateTime.now());\n    }\n    \n    public RewardPoints add(int points) {\n        return new RewardPoints(balance + points, LocalDateTime.now());\n    }\n    \n    public RewardPoints subtract(int points) {\n        if (points > balance) {\n            throw new IllegalArgumentException(\"Insufficient reward points\");\n        }\n        return new RewardPoints(balance - points, LocalDateTime.now());\n    }\n    \n    public boolean canRedeem(int points) {\n        return balance >= points;\n    }\n}\n```\n\n### Money Value Object\n\n```java\n@ValueObject\npublic record Money(BigDecimal amount, Currency currency) {\n    \n    public Money {\n        Objects.requireNonNull(amount, \"Amount cannot be null\");\n        Objects.requireNonNull(currency, \"Currency cannot be null\");\n        if (amount.compareTo(BigDecimal.ZERO) < 0) {\n            throw new IllegalArgumentException(\"Amount cannot be negative\");\n        }\n    }\n    \n    public static Money twd(double amount) {\n        return new Money(BigDecimal.valueOf(amount), Currency.TWD);\n    }\n    \n    public static Money usd(double amount) {\n        return new Money(BigDecimal.valueOf(amount), Currency.USD);\n    }\n    \n    public Money add(Money other) {\n        if (!currency.equals(other.currency)) {\n            throw new IllegalArgumentException(\"Cannot add different currencies\");\n        }\n        return new Money(amount.add(other.amount), currency);\n    }\n    \n    public Money multiply(BigDecimal multiplier) {\n        return new Money(amount.multiply(multiplier), currency);\n    }\n    \n    public boolean isGreaterThan(Money other) {\n        if (!currency.equals(other.currency)) {\n            throw new IllegalArgumentException(\"Cannot compare different currencies\");\n        }\n        return amount.compareTo(other.amount) > 0;\n    }\n}\n```\n\n## @Entity Entities\n\n### Annotation Usage\n\nThe `@Entity` annotation is used to identify domain entities, distinguishing them from aggregate roots:\n\n```java\n@Entity(name = \"SellerRating\", description = \"Seller rating entity\")\npublic class SellerRating {\n    \n    private final SellerRatingId id;\n    private final CustomerId customerId;\n    private final int rating;\n    private final String comment;\n    private final LocalDateTime ratedAt;\n    private RatingStatus status;\n    \n    public SellerRating(SellerRatingId id, CustomerId customerId, int rating, String comment) {\n        this.id = Objects.requireNonNull(id, \"ID cannot be null\");\n        this.customerId = Objects.requireNonNull(customerId, \"Customer ID cannot be null\");\n        this.rating = validateRating(rating);\n        this.comment = comment;\n        this.ratedAt = LocalDateTime.now();\n        this.status = RatingStatus.ACTIVE;\n    }\n    \n    // Business logic methods\n    public boolean isPositive() {\n        return rating >= 4; // 4 or above considered positive\n    }\n    \n    public boolean isRecent() {\n        return ChronoUnit.DAYS.between(ratedAt, LocalDateTime.now()) <= 30;\n    }\n    \n    public void hide() {\n        this.status = RatingStatus.HIDDEN;\n    }\n    \n    public boolean isVisible() {\n        return status == RatingStatus.ACTIVE;\n    }\n    \n    private int validateRating(int rating) {\n        if (rating < 1 || rating > 5) {\n            throw new IllegalArgumentException(\"Rating must be between 1-5\");\n        }\n        return rating;\n    }\n    \n    // equals and hashCode based on ID\n    @Override\n    public boolean equals(Object obj) {\n        if (this == obj) return true;\n        if (obj == null || getClass() != obj.getClass()) return false;\n        SellerRating that = (SellerRating) obj;\n        return Objects.equals(id, that.id);\n    }\n    \n    @Override\n    public int hashCode() {\n        return Objects.hash(id);\n    }\n}\n```\n\n### Entity Design Principles\n\n1. **Business-Oriented Design**: Entities should focus on domain logic rather than technical abstractions\n2. **Strong-Typed IDs**: Each Entity should have a strong-typed ID Value Object\n3. **State Management**: Use Enum Value Objects to manage Entity states\n\n## @DomainService Domain Services\n\n### Annotation Usage\n\nThe `@DomainService` annotation is used to identify domain services that handle cross-aggregate business logic:\n\n```java\n@DomainService(\n    name = \"CustomerValidationService\",\n    description = \"Customer validation domain service\",\n    boundedContext = \"Customer\"\n)\n@Component\npublic class CustomerValidationService {\n    \n    public boolean isEmailUnique(Email email, CustomerId excludeCustomerId) {\n        // Cross-aggregate uniqueness check logic\n        return customerRepository.findByEmail(email)\n            .map(Customer::getId)\n            .filter(id -> !id.equals(excludeCustomerId))\n            .isEmpty();\n    }\n    \n    public ValidationResult validateCustomerData(CustomerData data) {\n        // Complex cross-domain validation logic\n        return ValidationResult.builder()\n            .addCheck(\"email\", validateEmail(data.email()))\n            .addCheck(\"phone\", validatePhone(data.phone()))\n            .build();\n    }\n}\n```\n\n### Domain Service Design Principles\n\n- **Stateless Design**\n- **Handle cross-aggregate business logic**\n- **Do not include infrastructure concerns**\n- **Use dependency injection to obtain required Repositories**\n\n## 📡 Domain Events - Record Implementation, Event Collection and Publishing\n\n### Record Implementation Pattern\n\nThe project uses Records to implement domain events, ensuring immutability and conciseness:\n\n```java\npublic record CustomerCreatedEvent(\n        CustomerId customerId,\n        CustomerName customerName,\n        Email email,\n        MembershipLevel membershipLevel,\n        UUID eventId,\n        LocalDateTime occurredOn) implements DomainEvent {\n\n    /**\n     * Factory method with automatic eventId and occurredOn generation\n     */\n    public static CustomerCreatedEvent create(\n            CustomerId customerId, \n            CustomerName customerName, \n            Email email,\n            MembershipLevel membershipLevel) {\n        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();\n        return new CustomerCreatedEvent(\n            customerId, customerName, email, membershipLevel,\n            metadata.eventId(), metadata.occurredOn()\n        );\n    }\n\n    @Override\n    public UUID getEventId() {\n        return eventId;\n    }\n\n    @Override\n    public LocalDateTime getOccurredOn() {\n        return occurredOn;\n    }\n\n    @Override\n    public String getEventType() {\n        return DomainEvent.getEventTypeFromClass(this.getClass());\n    }\n\n    @Override\n    public String getAggregateId() {\n        return customerId.getValue();\n    }\n}\n```\n\n### DomainEvent Interface Design\n\n```java\npublic interface DomainEvent extends Serializable {\n    \n    UUID getEventId();\n    LocalDateTime getOccurredOn();\n    String getEventType();\n    String getAggregateId();\n    \n    /**\n     * Automatically derive event type from class name\n     */\n    static String getEventTypeFromClass(Class<? extends DomainEvent> eventClass) {\n        String className = eventClass.getSimpleName();\n        if (className.endsWith(\"Event\")) {\n            return className.substring(0, className.length() - 5);\n        }\n        return className;\n    }\n    \n    /**\n     * Helper method to create event metadata\n     */\n    static EventMetadata createEventMetadata() {\n        return new EventMetadata(UUID.randomUUID(), LocalDateTime.now());\n    }\n    \n    record EventMetadata(UUID eventId, LocalDateTime occurredOn) {}\n}\n```"
### Event
 Processing Mechanism

#### Abstract Event Handler

```java
public abstract class AbstractDomainEventHandler<T extends DomainEvent> 
    implements DomainEventHandler<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDomainEventHandler.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Order(100)
    public void onDomainEvent(DomainEventPublisherAdapter.DomainEventWrapper wrapper) {
        DomainEvent event = wrapper.getSource();

        if (getSupportedEventType().isInstance(event)) {
            @SuppressWarnings("unchecked")
            T typedEvent = (T) event;

            if (shouldHandle(typedEvent)) {
                try {
                    logEventProcessingStart(typedEvent);
                    handle(typedEvent);
                    logEventProcessingSuccess(typedEvent);
                } catch (Exception e) {
                    logEventProcessingError(typedEvent, e);
                    throw new DomainEventProcessingException(
                            "Failed to process event: " + event.getClass().getSimpleName(), e);
                }
            }
        }
    }

    protected abstract void handle(T event);
    protected abstract Class<T> getSupportedEventType();
    
    protected boolean shouldHandle(T event) {
        return true; // Default: handle all events
    }
}
```

#### Concrete Event Handler Implementation

```java
@Component
public class CustomerCreatedEventHandler extends AbstractDomainEventHandler<CustomerCreatedEvent> {
    
    private final EmailService emailService;
    private final CustomerStatsService customerStatsService;
    
    public CustomerCreatedEventHandler(EmailService emailService, 
                                     CustomerStatsService customerStatsService) {
        this.emailService = emailService;
        this.customerStatsService = customerStatsService;
    }
    
    @Override
    protected void handle(CustomerCreatedEvent event) {
        // Send welcome email
        emailService.sendWelcomeEmail(event.email(), event.customerName());
        
        // Update customer statistics
        customerStatsService.createStatsRecord(event.customerId());
        
        // Record business metrics
        recordCustomerCreationMetrics(event);
    }
    
    @Override
    protected Class<CustomerCreatedEvent> getSupportedEventType() {
        return CustomerCreatedEvent.class;
    }
    
    private void recordCustomerCreationMetrics(CustomerCreatedEvent event) {
        // Record customer creation metrics
        LOGGER.info("Customer created: {} with membership level: {}", 
                   event.customerId(), event.membershipLevel());
    }
}
```

### Application Service Integration

#### Event Publishing Flow

```java
@Service
@Transactional
public class CustomerApplicationService {
    
    private final CustomerRepository customerRepository;
    private final DomainEventApplicationService domainEventService;
    
    public void createCustomer(CreateCustomerCommand command) {
        // 1. Create aggregate root (events are collected)
        Customer customer = new Customer(
            CustomerId.generate(),
            command.name(),
            command.email(),
            command.phone()
        );
        
        // 2. Save aggregate root
        customerRepository.save(customer);
        
        // 3. Publish collected events
        domainEventService.publishEventsFromAggregate(customer);
    }
    
    public void updateCustomerProfile(UpdateProfileCommand command) {
        // 1. Load aggregate root
        Customer customer = customerRepository.findById(command.customerId())
            .orElseThrow(() -> new CustomerNotFoundException(command.customerId()));
        
        // 2. Execute business operation (events are collected)
        customer.updateProfile(command.name(), command.email(), command.phone());
        
        // 3. Save aggregate root
        customerRepository.save(customer);
        
        // 4. Publish collected events
        domainEventService.publishEventsFromAggregate(customer);
    }
}
```

## Testing Strategy

### Aggregate Root Testing

```java
@ExtendWith(MockitoExtension.class)
class CustomerTest {
    
    @Test
    void should_collect_customer_created_event_when_customer_is_created() {
        // Given
        CustomerId customerId = CustomerId.generate();
        CustomerName name = new CustomerName("John Doe");
        Email email = new Email("john@example.com");
        Phone phone = new Phone("0912345678");
        
        // When
        Customer customer = new Customer(customerId, name, email, phone);
        
        // Then
        assertThat(customer.hasUncommittedEvents()).isTrue();
        List<DomainEvent> events = customer.getUncommittedEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(CustomerCreatedEvent.class);
        
        CustomerCreatedEvent event = (CustomerCreatedEvent) events.get(0);
        assertThat(event.customerId()).isEqualTo(customerId);
        assertThat(event.customerName()).isEqualTo(name);
        assertThat(event.email()).isEqualTo(email);
    }
}
```

### Event Handler Testing

```java
@ExtendWith(MockitoExtension.class)
class CustomerCreatedEventHandlerTest {
    
    @Mock
    private EmailService emailService;
    
    @Mock
    private CustomerStatsService customerStatsService;
    
    @InjectMocks
    private CustomerCreatedEventHandler handler;
    
    @Test
    void should_send_welcome_email_when_customer_created() {
        // Given
        CustomerCreatedEvent event = CustomerCreatedEvent.create(
            CustomerId.of("CUST-001"),
            new CustomerName("John Doe"),
            new Email("john@example.com"),
            MembershipLevel.STANDARD
        );
        
        // When
        handler.handle(event);
        
        // Then
        verify(emailService).sendWelcomeEmail(event.email(), event.customerName());
        verify(customerStatsService).createStatsRecord(event.customerId());
    }
}
```

## Architecture Testing

### ArchUnit Rules

```java
@ArchTest
static final ArchRule aggregateRootRules = classes()
    .that().areAnnotatedWith(AggregateRoot.class)
    .should().implement(AggregateRootInterface.class)
    .because("Aggregate roots must implement AggregateRootInterface");

@ArchTest
static final ArchRule valueObjectRules = classes()
    .that().areAnnotatedWith(ValueObject.class)
    .should().beRecords()
    .because("Value objects should be implemented using Records");

@ArchTest
static final ArchRule domainEventRules = classes()
    .that().implement(DomainEvent.class)
    .should().beRecords()
    .and().haveSimpleNameEndingWith("Event")
    .because("Domain events should be implemented using Records and end with Event");

@ArchTest
static final ArchRule eventHandlerRules = classes()
    .that().areAnnotatedWith(Component.class)
    .and().haveSimpleNameEndingWith("EventHandler")
    .should().beAssignableTo(DomainEventHandler.class)
    .because("Event handlers must implement DomainEventHandler interface");
```

## Best Practices Summary

### Aggregate Root Design
1. **Use Hybrid Approach**: Annotation + Interface provides the best development experience
2. **Event Collection**: Collect events in business operations, publish by application services
3. **Clear Boundaries**: One aggregate root manages one business invariant boundary

### Value Object Design
1. **Record First**: Use Java Records to ensure immutability
2. **Business Validation**: Perform business rule validation in constructors
3. **Factory Methods**: Provide semantically clear creation methods

### Domain Event Design
1. **Record Implementation**: Ensure event immutability
2. **Factory Methods**: Automatically set event metadata
3. **Business Logic**: Include business judgment methods in events

### Event Processing
1. **Transaction Aware**: Use @TransactionalEventListener to ensure consistency
2. **Type Safety**: Abstract base class provides type-safe event processing
3. **Error Handling**: Complete error handling and logging

---

**Related Documents**
- [Hexagonal Architecture](hexagonal-architecture.md)
- Microservices Architecture
- Saga Patterns

# Domain Event Design Guide

### Overview

Domain events are important tactical patterns in DDD, used to represent significant business events that occur in the domain. Event-driven architecture enables loose coupling communication between aggregates and supports complex business process coordination.

### Event Design Principles

#### 1. Event Naming Conventions
- Use past tense verbs: `CustomerRegistered`, `OrderPlaced`, `PaymentCompleted`
- Include aggregate name: `Customer*Event`, `Order*Event`
- Specifically describe what happened: `OrderStatusChanged` rather than `OrderUpdated`

#### 2. Event Content Design
- Include aggregate ID for event routing
- Include all data needed for event processing
- Avoid including sensitive information that shouldn't be shared
- Include event metadata (eventId, occurredOn, eventType)

#### 3. Event Immutability
- Events should not be modified once published
- Use Java Records to implement immutable events
- Avoid including references to mutable objects in events

### Event Implementation Patterns

#### Event Definition
```java
// Domain event as Record - following project style
public record CustomerRegisteredEvent(
    CustomerId customerId,
    CustomerName customerName,
    Email email,
    MembershipLevel membershipLevel,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {
    
    /**
     * Factory method with automatic eventId and occurredOn generation
     */
    public static CustomerRegisteredEvent create(
        CustomerId customerId, 
        CustomerName customerName, 
        Email email,
        MembershipLevel membershipLevel
    ) {
        return new CustomerRegisteredEvent(
            customerId, customerName, email, membershipLevel,
            UUID.randomUUID(), LocalDateTime.now()
        );
    }
    
    @Override
    public String getEventType() {
        return "CustomerRegistered";
    }
    
    @Override
    public String getAggregateId() {
        return customerId.getValue();
    }
}
```

#### Event Collection in Aggregate Roots
```java
@AggregateRoot(name = "Customer", description = "Customer aggregate root")
public class Customer implements AggregateRootInterface {
    
    public void register(CustomerName name, Email email, MembershipLevel level) {
        // 1. Execute business logic
        validateRegistration(name, email);
        
        // 2. Update state
        this.name = name;
        this.email = email;
        this.membershipLevel = level;
        this.status = CustomerStatus.ACTIVE;
        
        // 3. Collect domain event
        collectEvent(CustomerRegisteredEvent.create(this.id, name, email, level));
    }
    
    // Event management methods provided by AggregateRootInterface:
    // - collectEvent(DomainEvent event)
    // - getUncommittedEvents()
    // - markEventsAsCommitted()
    // - hasUncommittedEvents()
}
```

#### Event Publishing in Application Services
```java
@Service
@Transactional
public class CustomerApplicationService {
    
    private final CustomerRepository customerRepository;
    private final DomainEventPublisher eventPublisher;
    
    public void registerCustomer(RegisterCustomerCommand command) {
        // 1. Load or create aggregate
        Customer customer = Customer.register(
            command.name(), 
            command.email(), 
            command.membershipLevel()
        );
        
        // 2. Save aggregate
        customerRepository.save(customer);
        
        // 3. Publish collected events
        eventPublisher.publishEvents(customer.getUncommittedEvents());
        customer.markEventsAsCommitted();
    }
}
```

#### Event Handlers
```java
@Component
public class CustomerRegisteredEventHandler {
    
    private final EmailService emailService;
    private final LoyaltyService loyaltyService;
    
    @EventListener
    @Transactional
    public void handle(CustomerRegisteredEvent event) {
        // Implement idempotency check
        if (isEventAlreadyProcessed(event.eventId())) {
            return;
        }
        
        try {
            // Execute cross-aggregate business logic
            sendWelcomeEmail(event);
            createLoyaltyAccount(event);
            
            // Mark event as processed
            markEventAsProcessed(event.eventId());
            
        } catch (Exception e) {
            // Log error and potentially retry
            logEventProcessingError(event, e);
            throw new DomainEventProcessingException("Failed to process customer registration", e);
        }
    }
    
    private void sendWelcomeEmail(CustomerRegisteredEvent event) {
        emailService.sendWelcomeEmail(
            event.email().getValue(),
            event.customerName().getValue()
        );
    }
    
    private void createLoyaltyAccount(CustomerRegisteredEvent event) {
        loyaltyService.createAccount(
            event.customerId(),
            event.membershipLevel()
        );
    }
}
```

### Event Versioning

#### Backward Compatible Event Evolution
```java
// Recommended: Use Optional fields for backward compatibility
public record CustomerRegisteredEvent(
    CustomerId customerId,
    CustomerName customerName,
    Email email,
    MembershipLevel membershipLevel,
    // V2 fields using Optional for backward compatibility
    Optional<LocalDate> birthDate,
    Optional<Address> address,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {
    
    // Primary factory method - latest version
    public static CustomerRegisteredEvent create(
        CustomerId customerId, 
        CustomerName customerName, 
        Email email,
        MembershipLevel membershipLevel,
        LocalDate birthDate,
        Address address
    ) {
        return new CustomerRegisteredEvent(
            customerId, customerName, email, membershipLevel,
            Optional.ofNullable(birthDate),
            Optional.ofNullable(address),
            UUID.randomUUID(), LocalDateTime.now()
        );
    }
    
    // Backward compatible factory method
    public static CustomerRegisteredEvent createLegacy(
        CustomerId customerId, 
        CustomerName customerName, 
        Email email,
        MembershipLevel membershipLevel
    ) {
        return new CustomerRegisteredEvent(
            customerId, customerName, email, membershipLevel,
            Optional.empty(), // Legacy version has no birth date
            Optional.empty(), // Legacy version has no address
            UUID.randomUUID(), LocalDateTime.now()
        );
    }
    
    @Override
    public String getEventType() {
        return "CustomerRegistered"; // Keep same event type across versions
    }
}
```

### Event Storage Strategies

#### Option 1: EventStore DB (Recommended for Production)
```yaml
# docker-compose.yml
version: '3.8'
services:
  eventstore:
    image: eventstore/eventstore:23.10.0-bookworm-slim
    container_name: eventstore
    environment:
      - EVENTSTORE_CLUSTER_SIZE=1
      - EVENTSTORE_RUN_PROJECTIONS=All
      - EVENTSTORE_START_STANDARD_PROJECTIONS=true
      - EVENTSTORE_EXT_TCP_PORT=1113
      - EVENTSTORE_HTTP_PORT=2113
      - EVENTSTORE_INSECURE=true
    ports:
      - "1113:1113"
      - "2113:2113"
```

#### Option 2: JPA Event Store (Recommended for Development)
```java
@Entity
@Table(name = "domain_events")
public class StoredDomainEvent {
    @Id
    private String eventId;
    
    @Column(name = "event_type")
    private String eventType;
    
    @Column(name = "aggregate_id")
    private String aggregateId;
    
    @Column(name = "event_data", columnDefinition = "TEXT")
    private String eventData;
    
    @Column(name = "occurred_on")
    private LocalDateTime occurredOn;
    
    @Column(name = "version")
    private Long version;
}

@Component
@Profile("development")
public class JpaEventStore implements EventStore {
    
    private final StoredDomainEventRepository repository;
    private final ObjectMapper objectMapper;
    
    @Override
    @Transactional
    public void store(DomainEvent event) {
        StoredDomainEvent storedEvent = new StoredDomainEvent(
            event.getEventId().toString(),
            event.getEventType(),
            event.getAggregateId(),
            serializeEvent(event),
            event.getOccurredOn(),
            getNextVersion(event.getAggregateId())
        );
        
        repository.save(storedEvent);
    }
}
```

### Event-Driven Saga Pattern

#### Order Processing Saga
```java
@Component
public class OrderProcessingSaga {
    
    @EventListener
    @Order(1)
    public void on(OrderCreatedEvent event) {
        // Step 1: Reserve inventory
        inventoryService.reserveItems(event.orderItems());
    }
    
    @EventListener
    @Order(2)
    public void on(InventoryReservedEvent event) {
        // Step 2: Process payment
        paymentService.processPayment(event.orderId(), event.amount());
    }
    
    @EventListener
    @Order(3)
    public void on(PaymentProcessedEvent event) {
        // Step 3: Confirm order
        orderService.confirmOrder(event.orderId());
    }
    
    @EventListener
    public void on(PaymentFailedEvent event) {
        // Compensation: Release inventory
        inventoryService.releaseReservation(event.orderId());
        orderService.cancelOrder(event.orderId());
    }
}
```

### Error Handling and Resilience

#### Retry Mechanism
```java
@Component
public class ResilientEventHandler {
    
    @Retryable(
        value = {TransientException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @EventListener
    public void handle(CustomerRegisteredEvent event) {
        // Event processing logic with retry capability
    }
    
    @Recover
    public void recover(TransientException ex, CustomerRegisteredEvent event) {
        // Final failure handling after all retries
        deadLetterService.send(event, ex);
    }
}
```

#### Dead Letter Queue
```java
@Component
public class DeadLetterService {
    
    public void send(DomainEvent event, Exception cause) {
        DeadLetterEvent deadLetter = new DeadLetterEvent(
            event.getEventId(),
            event.getClass().getSimpleName(),
            serializeEvent(event),
            cause.getMessage(),
            Instant.now()
        );
        
        deadLetterRepository.save(deadLetter);
        
        // Optionally send to external dead letter queue
        messageQueue.send("dead-letter-queue", deadLetter);
    }
}
```

### Testing Strategies

#### Event Collection Testing
```java
@Test
void should_collect_customer_registered_event_when_customer_registers() {
    // Given
    CustomerId customerId = CustomerId.generate();
    CustomerName name = new CustomerName("John Doe");
    Email email = new Email("john@example.com");
    
    // When
    Customer customer = Customer.register(customerId, name, email, MembershipLevel.STANDARD);
    
    // Then
    assertThat(customer.hasUncommittedEvents()).isTrue();
    List<DomainEvent> events = customer.getUncommittedEvents();
    assertThat(events).hasSize(1);
    assertThat(events.get(0)).isInstanceOf(CustomerRegisteredEvent.class);
    
    CustomerRegisteredEvent event = (CustomerRegisteredEvent) events.get(0);
    assertThat(event.customerId()).isEqualTo(customerId);
    assertThat(event.customerName()).isEqualTo(name);
    assertThat(event.email()).isEqualTo(email);
}
```

#### Event Handler Testing
```java
@Test
void should_send_welcome_email_when_customer_registered() {
    // Given
    CustomerRegisteredEvent event = CustomerRegisteredEvent.create(
        CustomerId.of("CUST-001"),
        new CustomerName("John Doe"),
        new Email("john@example.com"),
        MembershipLevel.STANDARD
    );
    
    // When
    customerRegisteredEventHandler.handle(event);
    
    // Then
    verify(emailService).sendWelcomeEmail("john@example.com", "John Doe");
    verify(loyaltyService).createAccount(CustomerId.of("CUST-001"), MembershipLevel.STANDARD);
}
```

### Monitoring and Observability

#### Event Metrics
```java
@Component
public class EventMetricsCollector {
    
    private final MeterRegistry meterRegistry;
    
    @EventListener
    public void collectMetrics(DomainEvent event) {
        // Count events by type
        Counter.builder("domain.events.published")
            .tag("event.type", event.getEventType())
            .tag("aggregate.type", getAggregateType(event))
            .register(meterRegistry)
            .increment();
    }
}
```

#### Event Tracing
```java
@Component
public class EventTracingHandler {
    
    @EventListener
    public void trace(DomainEvent event) {
        Span span = tracer.nextSpan()
            .name("domain-event-processing")
            .tag("event.type", event.getEventType())
            .tag("event.id", event.getEventId().toString())
            .tag("aggregate.id", event.getAggregateId())
            .start();
            
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            // Event processing is traced
        } finally {
            span.end();
        }
    }
}
```

This comprehensive guide ensures consistent, reliable, and maintainable domain event implementation across the entire project.

## E-commerce Platform Epic Implementation Case Study

### Overview

This case study demonstrates a complete e-commerce platform Epic implementation, covering the entire business process from customer browsing products to order completion. The system adopts Domain-Driven Design (DDD) and hexagonal architecture, providing a highly scalable and maintainable solution.

### Implementation Results

✅ **All Features Completed**

- **68 scenarios** all passed testing
- **452 steps** all implemented and verified
- **15 Features** complete coverage of all business requirements
- **100% BDD test coverage**

### Implemented Feature Modules

| Module | Feature Count | Scenario Count | Status |
|--------|---------------|----------------|--------|
| Customer Management | 2 | 6 | ✅ Complete |
| Order Management | 1 | 6 | ✅ Complete |
| Payment Processing | 2 | 11 | ✅ Complete |
| Inventory Management | 1 | 7 | ✅ Complete |
| Logistics & Shipping | 1 | 7 | ✅ Complete |
| Notification Service | 1 | 7 | ✅ Complete |
| Promotion Activities | 4 | 10 | ✅ Complete |
| Pricing Management | 1 | 2 | ✅ Complete |
| Product Management | 1 | 3 | ✅ Complete |
| Workflow | 1 | 9 | ✅ Complete |