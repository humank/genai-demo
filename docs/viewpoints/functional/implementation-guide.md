# Functional Viewpoint Implementation Guide

## Overview

This guide provides detailed steps, best practices, and concrete examples for implementing the functional viewpoint, helping development teams correctly implement domain-driven design and functional requirements.

## Implementation Process

### Phase 1: Domain Analysis and Modeling

#### 1. Business Requirements Analysis

```markdown
## Business Requirements Example: Customer Registration Process

### Functional Requirements
- Customers can register accounts using email
- System validates email uniqueness
- Send welcome email after successful registration
- Customers receive default membership level

### Business Rules
- Email must be unique
- Password must meet security requirements
- New customers default to standard membership
- Email must be verified within 24 hours of registration
```

#### 2. Domain Modeling Workshop

```markdown
## Event Storming Workshop Results

### Domain Events
- CustomerRegistered
- EmailVerificationSent
- EmailVerified
- WelcomeEmailSent

### Commands
- RegisterCustomer
- VerifyEmail
- ResendVerificationEmail

### Aggregate Roots
- Customer

### External Systems
- EmailService
- IdentityProvider
```

#### 3. Bounded Context Identification

```java
// Bounded context mapping
public enum BoundedContext {
    CUSTOMER_MANAGEMENT("Customer Management", "Manages customer lifecycle and basic information"),
    IDENTITY_ACCESS("Identity & Access", "Handles authentication and authorization"),
    NOTIFICATION("Notification Service", "Handles various notifications and emails"),
    MEMBERSHIP("Membership Management", "Manages membership levels and benefits");
}
```

### Phase 2: Aggregate Root Design and Implementation

#### 1. Aggregate Root Identification Principles

```java
// Aggregate root identification checklist
public class AggregateRootIdentificationGuide {
    
    /**
     * Aggregate root identification criteria:
     * 1. Has global unique identity
     * 2. Has independent lifecycle
     * 3. Maintains business invariants
     * 4. Is the root entity of business concept
     * 5. Controls access to other entities within aggregate
     */
    
    public boolean isAggregateRoot(DomainEntity entity) {
        return hasGlobalIdentity(entity) &&
               hasIndependentLifecycle(entity) &&
               maintainsBusinessInvariants(entity) &&
               isBusinessConceptRoot(entity) &&
               controlsInternalAccess(entity);
    }
}
```

#### 2. Aggregate Root Implementation Template

```java
@AggregateRoot(name = "Customer", description = "Customer aggregate root", boundedContext = "CustomerManagement", version = "1.0")
public class Customer implements AggregateRootInterface {
    
    // === Aggregate root identity ===
    private final CustomerId id;
    
    // === Core business attributes ===
    private CustomerName name;
    private Email email;
    private MembershipLevel membershipLevel;
    private CustomerStatus status;
    private LocalDateTime registrationDate;
    private LocalDateTime lastLoginDate;
    
    // === Aggregate internal entity collections ===
    private final List<DeliveryAddress> addresses = new ArrayList<>();
    private final List<PaymentMethod> paymentMethods = new ArrayList<>();
    private CustomerPreferences preferences;
    
    // === Constructor ===
    private Customer(CustomerId id, CustomerName name, Email email) {
        this.id = requireNonNull(id, "Customer ID cannot be null");
        this.name = requireNonNull(name, "Customer name cannot be null");
        this.email = requireNonNull(email, "Email cannot be null");
        this.membershipLevel = MembershipLevel.STANDARD;
        this.status = CustomerStatus.PENDING_VERIFICATION;
        this.registrationDate = LocalDateTime.now();
        this.preferences = CustomerPreferences.defaultPreferences();
    }
    
    // === Factory methods ===
    public static Customer register(CustomerName name, Email email) {
        CustomerId id = CustomerId.generate();
        Customer customer = new Customer(id, name, email);
        
        // Collect domain event
        customer.collectEvent(CustomerRegisteredEvent.create(id, name, email));
        
        return customer;
    }
    
    // === Business methods ===
    public void verifyEmail() {
        if (this.status != CustomerStatus.PENDING_VERIFICATION) {
            throw new InvalidCustomerStatusException("Customer status does not allow email verification");
        }
        
        this.status = CustomerStatus.ACTIVE;
        collectEvent(CustomerEmailVerifiedEvent.create(this.id, this.email));
    }
    
    public void updateProfile(CustomerName newName, Email newEmail) {
        validateProfileUpdate(newName, newEmail);
        
        CustomerName oldName = this.name;
        Email oldEmail = this.email;
        
        this.name = newName;
        this.email = newEmail;
        
        collectEvent(CustomerProfileUpdatedEvent.create(this.id, oldName, newName, oldEmail, newEmail));
    }
    
    public void addDeliveryAddress(Address address, AddressType type) {
        validateAddress(address);
        
        DeliveryAddressId addressId = DeliveryAddressId.generate();
        DeliveryAddress deliveryAddress = new DeliveryAddress(addressId, address, type);
        
        // If this is the first address, mark as default
        if (addresses.isEmpty()) {
            deliveryAddress.markAsDefault();
        }
        
        addresses.add(deliveryAddress);
        collectEvent(CustomerAddressAddedEvent.create(this.id, addressId, address, type));
    }
    
    public void promoteToMembership(MembershipLevel newLevel) {
        if (newLevel.ordinal() <= this.membershipLevel.ordinal()) {
            throw new InvalidMembershipPromotionException("Cannot downgrade or maintain same membership level");
        }
        
        MembershipLevel oldLevel = this.membershipLevel;
        this.membershipLevel = newLevel;
        
        collectEvent(CustomerMembershipPromotedEvent.create(this.id, oldLevel, newLevel));
    }
    
    // === Business rule validation ===
    private void validateProfileUpdate(CustomerName newName, Email newEmail) {
        if (newName == null || newEmail == null) {
            throw new InvalidProfileDataException("Name and email cannot be null");
        }
        
        if (this.status == CustomerStatus.SUSPENDED) {
            throw new InvalidCustomerStatusException("Suspended customers cannot update profile");
        }
    }
    
    private void validateAddress(Address address) {
        if (address == null || !address.isValid()) {
            throw new InvalidAddressException("Invalid address information");
        }
        
        if (addresses.size() >= 5) {
            throw new TooManyAddressesException("Customer can have maximum 5 delivery addresses");
        }
    }
    
    // === Query methods ===
    public boolean isActive() {
        return this.status == CustomerStatus.ACTIVE;
    }
    
    public boolean isPremiumMember() {
        return this.membershipLevel.ordinal() >= MembershipLevel.PREMIUM.ordinal();
    }
    
    public Optional<DeliveryAddress> getDefaultAddress() {
        return addresses.stream()
            .filter(DeliveryAddress::isDefault)
            .findFirst();
    }
    
    public List<DeliveryAddress> getAddresses() {
        return Collections.unmodifiableList(addresses);
    }
    
    // === Getters ===
    public CustomerId getId() { return id; }
    public CustomerName getName() { return name; }
    public Email getEmail() { return email; }
    public MembershipLevel getMembershipLevel() { return membershipLevel; }
    public CustomerStatus getStatus() { return status; }
    public LocalDateTime getRegistrationDate() { return registrationDate; }
    public LocalDateTime getLastLoginDate() { return lastLoginDate; }
}
```

#### 3. Value Object Implementation Template

```java
@ValueObject
public record CustomerId(String value) {
    
    private static final String PREFIX = "CUST-";
    private static final Pattern PATTERN = Pattern.compile("^CUST-\\d{8}$");
    
    public CustomerId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid customer ID format, should be: CUST-xxxxxxxx");
        }
    }
    
    public static CustomerId generate() {
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(5);
        String random = String.format("%03d", new Random().nextInt(1000));
        return new CustomerId(PREFIX + timestamp + random);
    }
    
    public static CustomerId of(String value) {
        return new CustomerId(value);
    }
    
    public String getShortId() {
        return value.substring(PREFIX.length());
    }
}

@ValueObject
public record Email(String value) {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    public Email {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        
        String normalizedValue = value.trim().toLowerCase();
        
        if (!EMAIL_PATTERN.matcher(normalizedValue).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }
        
        // Reassign to normalized value
        value = normalizedValue;
    }
    
    public String getDomain() {
        return value.substring(value.indexOf('@') + 1);
    }
    
    public String getLocalPart() {
        return value.substring(0, value.indexOf('@'));
    }
    
    public boolean isFromDomain(String domain) {
        return getDomain().equalsIgnoreCase(domain);
    }
}
```

### Phase 3: Application Service Implementation

#### 1. Application Service Template

```java
@Service
@Transactional
public class CustomerApplicationService {
    
    private final CustomerRepository customerRepository;
    private final DomainEventApplicationService domainEventService;
    private final EmailService emailService;
    
    public CustomerApplicationService(
            CustomerRepository customerRepository,
            DomainEventApplicationService domainEventService,
            EmailService emailService) {
        this.customerRepository = customerRepository;
        this.domainEventService = domainEventService;
        this.emailService = emailService;
    }
    
    // === Command handling methods ===
    
    public Customer registerCustomer(RegisterCustomerCommand command) {
        // 1. Validate command
        validateCommand(command);
        
        // 2. Check business rules
        if (customerRepository.existsByEmail(command.email())) {
            throw new EmailAlreadyExistsException(command.email());
        }
        
        // 3. Create aggregate root
        Customer customer = Customer.register(command.name(), command.email());
        
        // 4. Save aggregate root
        Customer savedCustomer = customerRepository.save(customer);
        
        // 5. Publish domain events
        domainEventService.publishEventsFromAggregate(savedCustomer);
        
        return savedCustomer;
    }
    
    public void verifyCustomerEmail(VerifyEmailCommand command) {
        // 1. Load aggregate root
        Customer customer = customerRepository.findById(command.customerId())
            .orElseThrow(() -> new CustomerNotFoundException(command.customerId()));
        
        // 2. Execute business operation
        customer.verifyEmail();
        
        // 3. Save changes
        customerRepository.save(customer);
        
        // 4. Publish events
        domainEventService.publishEventsFromAggregate(customer);
    }
    
    public Customer updateCustomerProfile(UpdateProfileCommand command) {
        // 1. Load aggregate root
        Customer customer = customerRepository.findById(command.customerId())
            .orElseThrow(() -> new CustomerNotFoundException(command.customerId()));
        
        // 2. Execute business operation
        customer.updateProfile(command.name(), command.email());
        
        // 3. Save changes
        Customer updatedCustomer = customerRepository.save(customer);
        
        // 4. Publish events
        domainEventService.publishEventsFromAggregate(updatedCustomer);
        
        return updatedCustomer;
    }
    
    // === Query methods ===
    
    @Transactional(readOnly = true)
    public Customer getCustomer(CustomerId customerId) {
        return customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }
    
    @Transactional(readOnly = true)
    public Page<Customer> getCustomers(CustomerSearchCriteria criteria, Pageable pageable) {
        return customerRepository.findByCriteria(criteria, pageable);
    }
    
    // === Private helper methods ===
    
    private void validateCommand(RegisterCustomerCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Register command cannot be null");
        }
        
        if (command.name() == null || command.email() == null) {
            throw new IllegalArgumentException("Customer name and email cannot be null");
        }
    }
}
```

#### 2. Command Object Design

```java
// Command base interface
public interface Command {
    UUID getCommandId();
    LocalDateTime getTimestamp();
    String getInitiatedBy();
}

// Abstract command base class
public abstract class AbstractCommand implements Command {
    private final UUID commandId;
    private final LocalDateTime timestamp;
    private final String initiatedBy;
    
    protected AbstractCommand(String initiatedBy) {
        this.commandId = UUID.randomUUID();
        this.timestamp = LocalDateTime.now();
        this.initiatedBy = initiatedBy;
    }
    
    @Override
    public UUID getCommandId() { return commandId; }
    
    @Override
    public LocalDateTime getTimestamp() { return timestamp; }
    
    @Override
    public String getInitiatedBy() { return initiatedBy; }
}

// Concrete command implementation
public record RegisterCustomerCommand(
    CustomerName name,
    Email email,
    String initiatedBy
) implements Command {
    
    private static final UUID commandId = UUID.randomUUID();
    private static final LocalDateTime timestamp = LocalDateTime.now();
    
    public RegisterCustomerCommand {
        if (name == null || email == null) {
            throw new IllegalArgumentException("Customer name and email cannot be null");
        }
        if (initiatedBy == null || initiatedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Command initiator cannot be null or empty");
        }
    }
    
    @Override
    public UUID getCommandId() { return commandId; }
    
    @Override
    public LocalDateTime getTimestamp() { return timestamp; }
    
    @Override
    public String getInitiatedBy() { return initiatedBy; }
}
```

### Phase 4: Domain Event Implementation

#### 1. Domain Event Design

```java
public record CustomerRegisteredEvent(
    CustomerId customerId,
    CustomerName customerName,
    Email email,
    MembershipLevel membershipLevel,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {
    
    public static CustomerRegisteredEvent create(
        CustomerId customerId,
        CustomerName customerName,
        Email email
    ) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new CustomerRegisteredEvent(
            customerId,
            customerName,
            email,
            MembershipLevel.STANDARD,
            metadata.eventId(),
            metadata.occurredOn()
        );
    }
    
    @Override
    public UUID getEventId() { return eventId; }
    
    @Override
    public LocalDateTime getOccurredOn() { return occurredOn; }
    
    @Override
    public String getEventType() {
        return DomainEvent.getEventTypeFromClass(this.getClass());
    }
    
    @Override
    public String getAggregateId() { return customerId.value(); }
}
```

#### 2. Event Handler Implementation

```java
@Component
public class CustomerRegisteredEventHandler extends AbstractDomainEventHandler<CustomerRegisteredEvent> {
    
    private final EmailService emailService;
    private final CustomerStatisticsService statisticsService;
    private final WelcomePackageService welcomePackageService;
    
    @Override
    @Transactional
    public void handle(CustomerRegisteredEvent event) {
        try {
            // 1. Send welcome email
            sendWelcomeEmail(event);
            
            // 2. Update statistics
            updateCustomerStatistics(event);
            
            // 3. Prepare welcome package
            prepareWelcomePackage(event);
            
            // 4. Mark event as processed
            markEventAsProcessed(event.getEventId());
            
        } catch (Exception e) {
            logger.error("Failed to process customer registration event", e);
            throw new DomainEventProcessingException("Customer registration follow-up processing failed", e);
        }
    }
    
    @Override
    public Class<CustomerRegisteredEvent> getSupportedEventType() {
        return CustomerRegisteredEvent.class;
    }
    
    private void sendWelcomeEmail(CustomerRegisteredEvent event) {
        WelcomeEmailRequest request = WelcomeEmailRequest.builder()
            .recipientEmail(event.email().value())
            .customerName(event.customerName().value())
            .membershipLevel(event.membershipLevel())
            .build();
            
        emailService.sendWelcomeEmail(request);
    }
    
    private void updateCustomerStatistics(CustomerRegisteredEvent event) {
        statisticsService.recordNewCustomerRegistration(
            event.customerId(),
            event.occurredOn().toLocalDate()
        );
    }
    
    private void prepareWelcomePackage(CustomerRegisteredEvent event) {
        welcomePackageService.preparePackage(
            event.customerId(),
            event.membershipLevel()
        );
    }
}
```

### Phase 5: Infrastructure Layer Implementation

#### 1. Repository Implementation

```java
@Repository
public class JpaCustomerRepository implements CustomerRepository {
    
    private final CustomerJpaRepository jpaRepository;
    private final CustomerMapper mapper;
    
    public JpaCustomerRepository(CustomerJpaRepository jpaRepository, CustomerMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    public Optional<Customer> findById(CustomerId customerId) {
        return jpaRepository.findById(customerId.value())
            .map(mapper::toDomain);
    }
    
    @Override
    public Customer save(Customer customer) {
        CustomerEntity entity = mapper.toEntity(customer);
        CustomerEntity savedEntity = jpaRepository.save(entity);
        
        // Mark events as committed
        customer.markEventsAsCommitted();
        
        return mapper.toDomain(savedEntity);
    }
    
    @Override
    public boolean existsByEmail(Email email) {
        return jpaRepository.existsByEmail(email.value());
    }
    
    @Override
    public Page<Customer> findByCriteria(CustomerSearchCriteria criteria, Pageable pageable) {
        Specification<CustomerEntity> spec = CustomerSpecifications.fromCriteria(criteria);
        Page<CustomerEntity> entityPage = jpaRepository.findAll(spec, pageable);
        
        return entityPage.map(mapper::toDomain);
    }
}
```

#### 2. Entity Mapper

```java
@Component
public class CustomerMapper {
    
    public Customer toDomain(CustomerEntity entity) {
        if (entity == null) return null;
        
        // Use reflection or constructor to create domain object
        Customer customer = Customer.reconstitute(
            CustomerId.of(entity.getId()),
            new CustomerName(entity.getName()),
            new Email(entity.getEmail()),
            entity.getMembershipLevel(),
            entity.getStatus(),
            entity.getRegistrationDate(),
            entity.getLastLoginDate()
        );
        
        // Map aggregate internal entities
        List<DeliveryAddress> addresses = entity.getAddresses().stream()
            .map(this::toDeliveryAddress)
            .toList();
        customer.setAddresses(addresses);
        
        return customer;
    }
    
    public CustomerEntity toEntity(Customer customer) {
        if (customer == null) return null;
        
        CustomerEntity entity = new CustomerEntity();
        entity.setId(customer.getId().value());
        entity.setName(customer.getName().value());
        entity.setEmail(customer.getEmail().value());
        entity.setMembershipLevel(customer.getMembershipLevel());
        entity.setStatus(customer.getStatus());
        entity.setRegistrationDate(customer.getRegistrationDate());
        entity.setLastLoginDate(customer.getLastLoginDate());
        
        // Map aggregate internal entities
        List<DeliveryAddressEntity> addressEntities = customer.getAddresses().stream()
            .map(this::toAddressEntity)
            .toList();
        entity.setAddresses(addressEntities);
        
        return entity;
    }
    
    private DeliveryAddress toDeliveryAddress(DeliveryAddressEntity entity) {
        return new DeliveryAddress(
            DeliveryAddressId.of(entity.getId()),
            new Address(
                entity.getStreet(),
                entity.getCity(),
                entity.getPostalCode(),
                entity.getCountry()
            ),
            entity.getType(),
            entity.isDefault()
        );
    }
    
    private DeliveryAddressEntity toAddressEntity(DeliveryAddress address) {
        DeliveryAddressEntity entity = new DeliveryAddressEntity();
        entity.setId(address.getId().value());
        entity.setStreet(address.getAddress().getStreet());
        entity.setCity(address.getAddress().getCity());
        entity.setPostalCode(address.getAddress().getPostalCode());
        entity.setCountry(address.getAddress().getCountry());
        entity.setType(address.getType());
        entity.setDefault(address.isDefault());
        return entity;
    }
}
```

## Testing Implementation Guide

### 1. Aggregate Root Testing

```java
@ExtendWith(MockitoExtension.class)
class CustomerTest {
    
    @Test
    void should_create_customer_with_correct_initial_state() {
        // Given
        CustomerName name = new CustomerName("John Doe");
        Email email = new Email("john.doe@example.com");
        
        // When
        Customer customer = Customer.register(name, email);
        
        // Then
        assertThat(customer.getId()).isNotNull();
        assertThat(customer.getName()).isEqualTo(name);
        assertThat(customer.getEmail()).isEqualTo(email);
        assertThat(customer.getMembershipLevel()).isEqualTo(MembershipLevel.STANDARD);
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.PENDING_VERIFICATION);
        
        // Verify domain events
        assertThat(customer.hasUncommittedEvents()).isTrue();
        List<DomainEvent> events = customer.getUncommittedEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(CustomerRegisteredEvent.class);
    }
    
    @Test
    void should_verify_email_when_status_is_pending() {
        // Given
        Customer customer = createPendingCustomer();
        
        // When
        customer.verifyEmail();
        
        // Then
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(customer.hasUncommittedEvents()).isTrue();
        
        List<DomainEvent> events = customer.getUncommittedEvents();
        assertThat(events.stream()
            .anyMatch(event -> event instanceof CustomerEmailVerifiedEvent))
            .isTrue();
    }
    
    @Test
    void should_throw_exception_when_verifying_email_with_invalid_status() {
        // Given
        Customer customer = createActiveCustomer();
        
        // When & Then
        assertThatThrownBy(() -> customer.verifyEmail())
            .isInstanceOf(InvalidCustomerStatusException.class)
            .hasMessage("Customer status does not allow email verification");
    }
}
```

### 2. Application Service Testing

```java
@ExtendWith(MockitoExtension.class)
class CustomerApplicationServiceTest {
    
    @Mock
    private CustomerRepository customerRepository;
    
    @Mock
    private DomainEventApplicationService domainEventService;
    
    @InjectMocks
    private CustomerApplicationService customerApplicationService;
    
    @Test
    void should_register_customer_successfully() {
        // Given
        RegisterCustomerCommand command = new RegisterCustomerCommand(
            new CustomerName("Jane Smith"),
            new Email("jane.smith@example.com"),
            "system"
        );
        
        when(customerRepository.existsByEmail(command.email())).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        Customer result = customerApplicationService.registerCustomer(command);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(command.name());
        assertThat(result.getEmail()).isEqualTo(command.email());
        
        verify(customerRepository).existsByEmail(command.email());
        verify(customerRepository).save(any(Customer.class));
        verify(domainEventService).publishEventsFromAggregate(any(Customer.class));
    }
    
    @Test
    void should_throw_exception_when_email_already_exists() {
        // Given
        RegisterCustomerCommand command = new RegisterCustomerCommand(
            new CustomerName("Bob Wilson"),
            new Email("existing@example.com"),
            "system"
        );
        
        when(customerRepository.existsByEmail(command.email())).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> customerApplicationService.registerCustomer(command))
            .isInstanceOf(EmailAlreadyExistsException.class);
        
        verify(customerRepository).existsByEmail(command.email());
        verify(customerRepository, never()).save(any(Customer.class));
        verify(domainEventService, never()).publishEventsFromAggregate(any(Customer.class));
    }
}
```

### 3. Integration Testing

```java
@SpringBootTest
@Transactional
@TestPerformanceExtension(maxExecutionTimeMs = 10000, maxMemoryIncreaseMB = 100)
class CustomerIntegrationTest extends BaseIntegrationTest {
    
    @Autowired
    private CustomerApplicationService customerApplicationService;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Test
    void should_complete_customer_registration_flow() {
        // Given
        RegisterCustomerCommand registerCommand = new RegisterCustomerCommand(
            new CustomerName("Integration Test Customer"),
            new Email("integration.test@example.com"),
            "test-system"
        );
        
        // When - Register customer
        Customer registeredCustomer = customerApplicationService.registerCustomer(registerCommand);
        
        // Then - Verify registration result
        assertThat(registeredCustomer).isNotNull();
        assertThat(registeredCustomer.getStatus()).isEqualTo(CustomerStatus.PENDING_VERIFICATION);
        
        // When - Verify email
        VerifyEmailCommand verifyCommand = new VerifyEmailCommand(
            registeredCustomer.getId(),
            "test-system"
        );
        customerApplicationService.verifyCustomerEmail(verifyCommand);
        
        // Then - Verify final state
        Customer verifiedCustomer = customerRepository.findById(registeredCustomer.getId()).orElseThrow();
        assertThat(verifiedCustomer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
    }
}
```

## Best Practices Checklist

### Design Phase

- [ ] Complete Event Storming workshop
- [ ] Identify core aggregate roots and bounded contexts
- [ ] Define clear business rules and invariants
- [ ] Design appropriate value objects and entities
- [ ] Plan domain events and commands

### Implementation Phase

- [ ] Aggregate roots implement AggregateRootInterface
- [ ] Value objects use immutable Records
- [ ] Application services only coordinate, no business logic
- [ ] Correctly implement domain event collection and publishing
- [ ] Repository interfaces defined in domain layer

### Testing Phase

- [ ] Aggregate root unit tests cover all business methods
- [ ] Application service tests verify coordination logic
- [ ] Integration tests verify complete business processes
- [ ] Event handler tests ensure correct processing
- [ ] Performance tests meet response time requirements

### Quality Assurance

- [ ] All business rules implemented in domain layer
- [ ] Aggregate boundaries designed reasonably
- [ ] Transaction boundaries align with aggregate boundaries
- [ ] Domain events correctly model business concepts
- [ ] Error handling is complete and user-friendly

---

**Related Documents**:
- [Architecture Elements Details](architecture-elements.md)
- [Quality Considerations Guide](quality-considerations.md)
- [Domain Events Implementation](../information/domain-events.md)
- [Development Standards](../../development/README.md)
