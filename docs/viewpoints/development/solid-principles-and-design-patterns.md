# SOLID Principles and Design Patterns

## Overview

This document covers core design principles and patterns in software development, including SOLID principles and commonly used design patterns. These principles and patterns are the foundation for building maintainable, scalable, and high-quality software.

## 🎯 SOLID Principles

SOLID principles are five fundamental principles of object-oriented design proposed by Robert C. Martin, aimed at making software design more understandable, flexible, and maintainable.

### 📏 Single Responsibility Principle (SRP)

**Definition**: A class should have only one reason to change, meaning a class should have only one responsibility.

#### ✅ Good Practice

```java
// ✅ Good design: Each class has a single responsibility
@Entity
public class Customer {
    private String id;
    private String name;
    private String email;
    
    // Only responsible for customer data management
    public void updateProfile(String name, String email) {
        validateName(name);
        validateEmail(email);
        this.name = name;
        this.email = email;
    }
    
    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
    }
    
    private void validateEmail(String email) {
        if (!email.contains("@")) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }
}

@Service
public class CustomerService {
    private final CustomerRepository customerRepository;
    
    // Only responsible for customer business logic
    public Customer createCustomer(CreateCustomerCommand command) {
        Customer customer = new Customer(command.getName(), command.getEmail());
        return customerRepository.save(customer);
    }
}

@Component
public class CustomerNotificationService {
    private final EmailService emailService;
    
    // Only responsible for customer notifications
    public void sendWelcomeEmail(Customer customer) {
        String subject = "Welcome to our service!";
        String body = "Hello " + customer.getName() + ", welcome!";
        emailService.send(customer.getEmail(), subject, body);
    }
}
```

#### ❌ Bad Practice

```java
// ❌ Bad design: One class handles multiple responsibilities
@Service
public class CustomerManager {
    
    // Responsibility 1: Customer data management
    public Customer createCustomer(String name, String email) {
        Customer customer = new Customer(name, email);
        return saveToDatabase(customer);
    }
    
    // Responsibility 2: Database operations
    private Customer saveToDatabase(Customer customer) {
        // Direct database logic handling
        return customer;
    }
    
    // Responsibility 3: Email sending
    public void sendWelcomeEmail(Customer customer) {
        // Direct email sending logic handling
    }
    
    // Responsibility 4: Report generation
    public String generateCustomerReport(Customer customer) {
        // Direct report generation logic handling
        return "Report for " + customer.getName();
    }
}
```

### 🔓 Open-Closed Principle (OCP)

**Definition**: Software entities (classes, modules, functions, etc.) should be open for extension but closed for modification.

#### ✅ Good Practice

```java
// ✅ Good design: Using Strategy pattern to implement OCP
public interface DiscountStrategy {
    BigDecimal calculateDiscount(Order order);
}

@Component
public class RegularCustomerDiscount implements DiscountStrategy {
    @Override
    public BigDecimal calculateDiscount(Order order) {
        return order.getTotal().multiply(new BigDecimal("0.05")); // 5% discount
    }
}

@Component
public class PremiumCustomerDiscount implements DiscountStrategy {
    @Override
    public BigDecimal calculateDiscount(Order order) {
        return order.getTotal().multiply(new BigDecimal("0.10")); // 10% discount
    }
}

@Component
public class VipCustomerDiscount implements DiscountStrategy {
    @Override
    public BigDecimal calculateDiscount(Order order) {
        return order.getTotal().multiply(new BigDecimal("0.15")); // 15% discount
    }
}

@Service
public class OrderService {
    private final Map<CustomerType, DiscountStrategy> discountStrategies;
    
    public OrderService(List<DiscountStrategy> strategies) {
        this.discountStrategies = Map.of(
            CustomerType.REGULAR, strategies.get(0),
            CustomerType.PREMIUM, strategies.get(1),
            CustomerType.VIP, strategies.get(2)
        );
    }
    
    public BigDecimal calculateOrderTotal(Order order, CustomerType customerType) {
        BigDecimal discount = discountStrategies.get(customerType).calculateDiscount(order);
        return order.getTotal().subtract(discount);
    }
}
```

#### ❌ Bad Practice

```java
// ❌ Bad design: Need to modify existing code every time a new customer type is added
@Service
public class OrderService {
    
    public BigDecimal calculateOrderTotal(Order order, CustomerType customerType) {
        BigDecimal total = order.getTotal();
        
        // Need to modify this method every time a new customer type is added
        switch (customerType) {
            case REGULAR:
                return total.multiply(new BigDecimal("0.95")); // 5% discount
            case PREMIUM:
                return total.multiply(new BigDecimal("0.90")); // 10% discount
            case VIP:
                return total.multiply(new BigDecimal("0.85")); // 15% discount
            // Need to modify here when adding DIAMOND customers
            default:
                return total;
        }
    }
}
```

### 🔄 Liskov Substitution Principle (LSP)

**Definition**: Subtypes must be substitutable for their base types without altering the correctness of the program.

#### ✅ Good Practice

```java
// ✅ Good design: Subtypes can completely replace parent types
public abstract class PaymentProcessor {
    
    public final PaymentResult processPayment(PaymentRequest request) {
        validateRequest(request);
        return doProcessPayment(request);
    }
    
    protected void validateRequest(PaymentRequest request) {
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }
    
    protected abstract PaymentResult doProcessPayment(PaymentRequest request);
}

@Component
public class CreditCardProcessor extends PaymentProcessor {
    
    @Override
    protected PaymentResult doProcessPayment(PaymentRequest request) {
        // Credit card processing logic
        return PaymentResult.success(request.getAmount());
    }
}

@Component
public class PayPalProcessor extends PaymentProcessor {
    
    @Override
    protected PaymentResult doProcessPayment(PaymentRequest request) {
        // PayPal processing logic
        return PaymentResult.success(request.getAmount());
    }
}

@Service
public class PaymentService {
    
    // Can use any subclass of PaymentProcessor
    public PaymentResult processPayment(PaymentProcessor processor, PaymentRequest request) {
        return processor.processPayment(request); // LSP principle: subtypes can replace parent types
    }
}
```

#### ❌ Bad Practice

```java
// ❌ Bad design: Subtype changes the behavioral contract of the parent type
public abstract class PaymentProcessor {
    
    public PaymentResult processPayment(PaymentRequest request) {
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        return doProcessPayment(request);
    }
    
    protected abstract PaymentResult doProcessPayment(PaymentRequest request);
}

@Component
public class CashProcessor extends PaymentProcessor {
    
    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        // Violates LSP: Cash payment doesn't need amount validation?
        // Skips parent class validation logic
        return doProcessPayment(request);
    }
    
    @Override
    protected PaymentResult doProcessPayment(PaymentRequest request) {
        return PaymentResult.success(request.getAmount());
    }
}
```

### 🔌 Interface Segregation Principle (ISP)

**Definition**: Clients should not be forced to depend on interfaces they do not use.

#### ✅ Good Practice

```java
// ✅ Good design: Split large interfaces into multiple small interfaces
public interface Readable {
    String read();
}

public interface Writable {
    void write(String content);
}

public interface Deletable {
    void delete();
}

// Class that only needs read functionality
@Component
public class LogReader implements Readable {
    
    @Override
    public String read() {
        return "Log content";
    }
}

// Class that needs read and write functionality
@Component
public class ConfigurationManager implements Readable, Writable {
    
    @Override
    public String read() {
        return "Configuration content";
    }
    
    @Override
    public void write(String content) {
        // Write configuration
    }
}

// Class that needs all functionality
@Component
public class FileManager implements Readable, Writable, Deletable {
    
    @Override
    public String read() {
        return "File content";
    }
    
    @Override
    public void write(String content) {
        // Write file
    }
    
    @Override
    public void delete() {
        // Delete file
    }
}
```

#### ❌ Bad Practice

```java
// ❌ Bad design: Forces clients to implement methods they don't need
public interface FileOperations {
    String read();
    void write(String content);
    void delete();
    void compress();
    void encrypt();
}

// Only needs read functionality, but forced to implement all methods
@Component
public class LogReader implements FileOperations {
    
    @Override
    public String read() {
        return "Log content";
    }
    
    // Forced to implement unneeded methods
    @Override
    public void write(String content) {
        throw new UnsupportedOperationException("Log reader cannot write");
    }
    
    @Override
    public void delete() {
        throw new UnsupportedOperationException("Log reader cannot delete");
    }
    
    @Override
    public void compress() {
        throw new UnsupportedOperationException("Log reader cannot compress");
    }
    
    @Override
    public void encrypt() {
        throw new UnsupportedOperationException("Log reader cannot encrypt");
    }
}
```

### 🔄 Dependency Inversion Principle (DIP)

**Definition**: High-level modules should not depend on low-level modules. Both should depend on abstractions. Abstractions should not depend on details. Details should depend on abstractions.

#### ✅ Good Practice

```java
// ✅ Good design: Depend on abstractions rather than concrete implementations
public interface NotificationService {
    void sendNotification(String recipient, String message);
}

public interface CustomerRepository {
    Customer save(Customer customer);
    Optional<Customer> findById(String id);
}

// High-level module depends on abstractions
@Service
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final NotificationService notificationService;
    
    // Dependency injection of abstract interfaces
    public CustomerService(CustomerRepository customerRepository, 
                          NotificationService notificationService) {
        this.customerRepository = customerRepository;
        this.notificationService = notificationService;
    }
    
    public Customer createCustomer(CreateCustomerCommand command) {
        Customer customer = new Customer(command.getName(), command.getEmail());
        Customer savedCustomer = customerRepository.save(customer);
        
        notificationService.sendNotification(
            savedCustomer.getEmail(), 
            "Welcome to our service!"
        );
        
        return savedCustomer;
    }
}

// Low-level modules implement abstractions
@Repository
public class JpaCustomerRepository implements CustomerRepository {
    
    @Override
    public Customer save(Customer customer) {
        // JPA implementation
        return customer;
    }
    
    @Override
    public Optional<Customer> findById(String id) {
        // JPA implementation
        return Optional.empty();
    }
}

@Component
public class EmailNotificationService implements NotificationService {
    
    @Override
    public void sendNotification(String recipient, String message) {
        // Email sending implementation
    }
}
```

#### ❌ Bad Practice

```java
// ❌ Bad design: High-level module directly depends on concrete implementations of low-level modules
@Service
public class CustomerService {
    private final JpaCustomerRepository customerRepository; // Direct dependency on concrete implementation
    private final EmailService emailService; // Direct dependency on concrete implementation
    
    public CustomerService() {
        this.customerRepository = new JpaCustomerRepository(); // Direct creation of dependency
        this.emailService = new EmailService(); // Direct creation of dependency
    }
    
    public Customer createCustomer(CreateCustomerCommand command) {
        Customer customer = new Customer(command.getName(), command.getEmail());
        Customer savedCustomer = customerRepository.save(customer);
        
        // Direct call to concrete implementation
        emailService.sendEmail(savedCustomer.getEmail(), "Welcome!");
        
        return savedCustomer;
    }
}
```

## 🎨 Design Patterns

Design patterns are reusable solutions to common problems in software design. The following are commonly used design patterns in the project.

### 🏭 Factory Pattern

**Purpose**: Create objects without specifying their concrete classes, encapsulating object creation logic in factory classes.

#### ✅ Implementation Example

```java
// Product interface
public interface PaymentProcessor {
    PaymentResult process(PaymentRequest request);
}

// Concrete products
@Component
public class CreditCardProcessor implements PaymentProcessor {
    
    @Override
    public PaymentResult process(PaymentRequest request) {
        // Credit card processing logic
        return PaymentResult.success("Credit card payment processed");
    }
}

@Component
public class PayPalProcessor implements PaymentProcessor {
    
    @Override
    public PaymentResult process(PaymentRequest request) {
        // PayPal processing logic
        return PaymentResult.success("PayPal payment processed");
    }
}

@Component
public class BankTransferProcessor implements PaymentProcessor {
    
    @Override
    public PaymentResult process(PaymentRequest request) {
        // Bank transfer processing logic
        return PaymentResult.success("Bank transfer processed");
    }
}

// Factory class
@Component
public class PaymentProcessorFactory {
    private final Map<PaymentType, PaymentProcessor> processors;
    
    public PaymentProcessorFactory(List<PaymentProcessor> processorList) {
        this.processors = Map.of(
            PaymentType.CREDIT_CARD, processorList.stream()
                .filter(p -> p instanceof CreditCardProcessor)
                .findFirst().orElseThrow(),
            PaymentType.PAYPAL, processorList.stream()
                .filter(p -> p instanceof PayPalProcessor)
                .findFirst().orElseThrow(),
            PaymentType.BANK_TRANSFER, processorList.stream()
                .filter(p -> p instanceof BankTransferProcessor)
                .findFirst().orElseThrow()
        );
    }
    
    public PaymentProcessor createProcessor(PaymentType type) {
        PaymentProcessor processor = processors.get(type);
        if (processor == null) {
            throw new IllegalArgumentException("Unsupported payment type: " + type);
        }
        return processor;
    }
}

// Using the factory
@Service
public class PaymentService {
    private final PaymentProcessorFactory processorFactory;
    
    public PaymentService(PaymentProcessorFactory processorFactory) {
        this.processorFactory = processorFactory;
    }
    
    public PaymentResult processPayment(PaymentRequest request) {
        PaymentProcessor processor = processorFactory.createProcessor(request.getType());
        return processor.process(request);
    }
}
```

### 🔨 Builder Pattern

**Purpose**: Construct complex objects step by step, allowing creation of different representations of the same object.

#### ✅ Implementation Example

```java
// Complex object
public class Order {
    private final String id;
    private final String customerId;
    private final List<OrderItem> items;
    private final BigDecimal totalAmount;
    private final LocalDateTime orderDate;
    private final Address shippingAddress;
    private final Address billingAddress;
    private final PaymentMethod paymentMethod;
    private final String notes;
    
    // Private constructor, can only be created through Builder
    private Order(Builder builder) {
        this.id = builder.id;
        this.customerId = builder.customerId;
        this.items = List.copyOf(builder.items);
        this.totalAmount = builder.totalAmount;
        this.orderDate = builder.orderDate;
        this.shippingAddress = builder.shippingAddress;
        this.billingAddress = builder.billingAddress;
        this.paymentMethod = builder.paymentMethod;
        this.notes = builder.notes;
    }
    
    // Builder class
    public static class Builder {
        private String id;
        private String customerId;
        private List<OrderItem> items = new ArrayList<>();
        private BigDecimal totalAmount;
        private LocalDateTime orderDate;
        private Address shippingAddress;
        private Address billingAddress;
        private PaymentMethod paymentMethod;
        private String notes;
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }
        
        public Builder addItem(OrderItem item) {
            this.items.add(item);
            return this;
        }
        
        public Builder items(List<OrderItem> items) {
            this.items = new ArrayList<>(items);
            return this;
        }
        
        public Builder totalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }
        
        public Builder orderDate(LocalDateTime orderDate) {
            this.orderDate = orderDate;
            return this;
        }
        
        public Builder shippingAddress(Address shippingAddress) {
            this.shippingAddress = shippingAddress;
            return this;
        }
        
        public Builder billingAddress(Address billingAddress) {
            this.billingAddress = billingAddress;
            return this;
        }
        
        public Builder paymentMethod(PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }
        
        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }
        
        public Order build() {
            validateBuilder();
            return new Order(this);
        }
        
        private void validateBuilder() {
            if (customerId == null) {
                throw new IllegalStateException("Customer ID is required");
            }
            if (items.isEmpty()) {
                throw new IllegalStateException("Order must have at least one item");
            }
            if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("Total amount must be positive");
            }
        }
    }
    
    // Static factory method
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public List<OrderItem> getItems() { return items; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public LocalDateTime getOrderDate() { return orderDate; }
    public Address getShippingAddress() { return shippingAddress; }
    public Address getBillingAddress() { return billingAddress; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public String getNotes() { return notes; }
}

// Using Builder
@Service
public class OrderService {
    
    public Order createOrder(CreateOrderCommand command) {
        return Order.builder()
            .id(generateOrderId())
            .customerId(command.getCustomerId())
            .items(command.getItems())
            .totalAmount(calculateTotal(command.getItems()))
            .orderDate(LocalDateTime.now())
            .shippingAddress(command.getShippingAddress())
            .billingAddress(command.getBillingAddress())
            .paymentMethod(command.getPaymentMethod())
            .notes(command.getNotes())
            .build();
    }
}
```

### 📋 Strategy Pattern

**Purpose**: Define a family of algorithms, encapsulate each one, and make them interchangeable.

#### ✅ Implementation Example

```java
// Strategy interface
public interface PricingStrategy {
    BigDecimal calculatePrice(Product product, int quantity);
    String getStrategyName();
}

// Concrete strategies
@Component
public class RegularPricingStrategy implements PricingStrategy {
    
    @Override
    public BigDecimal calculatePrice(Product product, int quantity) {
        return product.getPrice().multiply(BigDecimal.valueOf(quantity));
    }
    
    @Override
    public String getStrategyName() {
        return "Regular Pricing";
    }
}

@Component
public class BulkDiscountStrategy implements PricingStrategy {
    
    @Override
    public BigDecimal calculatePrice(Product product, int quantity) {
        BigDecimal basePrice = product.getPrice().multiply(BigDecimal.valueOf(quantity));
        
        if (quantity >= 100) {
            return basePrice.multiply(new BigDecimal("0.8")); // 20% discount
        } else if (quantity >= 50) {
            return basePrice.multiply(new BigDecimal("0.9")); // 10% discount
        } else if (quantity >= 10) {
            return basePrice.multiply(new BigDecimal("0.95")); // 5% discount
        }
        
        return basePrice;
    }
    
    @Override
    public String getStrategyName() {
        return "Bulk Discount";
    }
}

@Component
public class SeasonalDiscountStrategy implements PricingStrategy {
    
    @Override
    public BigDecimal calculatePrice(Product product, int quantity) {
        BigDecimal basePrice = product.getPrice().multiply(BigDecimal.valueOf(quantity));
        
        // Check if it's promotional season
        if (isPromotionalSeason()) {
            return basePrice.multiply(new BigDecimal("0.85")); // 15% seasonal discount
        }
        
        return basePrice;
    }
    
    @Override
    public String getStrategyName() {
        return "Seasonal Discount";
    }
    
    private boolean isPromotionalSeason() {
        Month currentMonth = LocalDate.now().getMonth();
        return currentMonth == Month.NOVEMBER || currentMonth == Month.DECEMBER;
    }
}

// Context class
@Service
public class PricingService {
    private final Map<String, PricingStrategy> strategies;
    
    public PricingService(List<PricingStrategy> strategyList) {
        this.strategies = strategyList.stream()
            .collect(Collectors.toMap(
                PricingStrategy::getStrategyName,
                Function.identity()
            ));
    }
    
    public BigDecimal calculatePrice(Product product, int quantity, String strategyName) {
        PricingStrategy strategy = strategies.get(strategyName);
        if (strategy == null) {
            throw new IllegalArgumentException("Unknown pricing strategy: " + strategyName);
        }
        
        return strategy.calculatePrice(product, quantity);
    }
    
    public List<String> getAvailableStrategies() {
        return new ArrayList<>(strategies.keySet());
    }
}
```

### 👁️ Observer Pattern

**Purpose**: Define a one-to-many dependency between objects so that when one object changes state, all its dependents are notified automatically.

#### ✅ Implementation Example

```java
// Event (Subject)
public record OrderStatusChangedEvent(
    String orderId,
    OrderStatus oldStatus,
    OrderStatus newStatus,
    LocalDateTime timestamp
) implements DomainEvent {
    
    public static OrderStatusChangedEvent create(String orderId, OrderStatus oldStatus, OrderStatus newStatus) {
        return new OrderStatusChangedEvent(orderId, oldStatus, newStatus, LocalDateTime.now());
    }
    
    @Override
    public UUID getEventId() {
        return UUID.randomUUID();
    }
    
    @Override
    public LocalDateTime getOccurredOn() {
        return timestamp;
    }
    
    @Override
    public String getEventType() {
        return "OrderStatusChanged";
    }
    
    @Override
    public String getAggregateId() {
        return orderId;
    }
}

// Observer interface
public interface OrderStatusObserver {
    void onOrderStatusChanged(OrderStatusChangedEvent event);
}

// Concrete observers
@Component
public class EmailNotificationObserver implements OrderStatusObserver {
    private final EmailService emailService;
    private final CustomerService customerService;
    
    public EmailNotificationObserver(EmailService emailService, CustomerService customerService) {
        this.emailService = emailService;
        this.customerService = customerService;
    }
    
    @Override
    @EventListener
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        Customer customer = customerService.findByOrderId(event.orderId());
        
        String subject = "Order Status Update";
        String message = String.format(
            "Your order %s status has changed from %s to %s",
            event.orderId(),
            event.oldStatus(),
            event.newStatus()
        );
        
        emailService.sendEmail(customer.getEmail(), subject, message);
    }
}

@Component
public class InventoryUpdateObserver implements OrderStatusObserver {
    private final InventoryService inventoryService;
    
    public InventoryUpdateObserver(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }
    
    @Override
    @EventListener
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        if (event.newStatus() == OrderStatus.CANCELLED) {
            // Release inventory when order is cancelled
            inventoryService.releaseReservedItems(event.orderId());
        } else if (event.newStatus() == OrderStatus.SHIPPED) {
            // Confirm inventory deduction when order is shipped
            inventoryService.confirmItemsShipped(event.orderId());
        }
    }
}

@Component
public class AuditLogObserver implements OrderStatusObserver {
    private final AuditService auditService;
    
    public AuditLogObserver(AuditService auditService) {
        this.auditService = auditService;
    }
    
    @Override
    @EventListener
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        AuditLog auditLog = AuditLog.builder()
            .entityType("Order")
            .entityId(event.orderId())
            .action("STATUS_CHANGED")
            .oldValue(event.oldStatus().toString())
            .newValue(event.newStatus().toString())
            .timestamp(event.timestamp())
            .build();
            
        auditService.log(auditLog);
    }
}

// Subject (Publisher)
@Entity
public class Order {
    private String id;
    private OrderStatus status;
    
    @Transient
    private ApplicationEventPublisher eventPublisher;
    
    public void updateStatus(OrderStatus newStatus) {
        OrderStatus oldStatus = this.status;
        this.status = newStatus;
        
        // Publish event to notify all observers
        if (eventPublisher != null) {
            OrderStatusChangedEvent event = OrderStatusChangedEvent.create(id, oldStatus, newStatus);
            eventPublisher.publishEvent(event);
        }
    }
    
    @PostLoad
    @PostPersist
    public void setEventPublisher() {
        this.eventPublisher = ApplicationContextProvider.getApplicationContext()
            .getBean(ApplicationEventPublisher.class);
    }
}
```

### 🙈 Tell, Don't Ask Principle

**Purpose**: Don't ask an object for its state and then make decisions based on that state; instead, tell the object what to do directly.

#### ✅ Good Practice

```java
// ✅ Good design: Tell, Don't Ask
@Entity
public class BankAccount {
    private String accountNumber;
    private BigDecimal balance;
    private AccountStatus status;
    
    // Tell the object to perform operations, don't ask for state
    public void withdraw(BigDecimal amount) {
        validateWithdrawal(amount);
        this.balance = this.balance.subtract(amount);
    }
    
    public void deposit(BigDecimal amount) {
        validateDeposit(amount);
        this.balance = this.balance.add(amount);
    }
    
    public void freeze() {
        if (status != AccountStatus.FROZEN) {
            this.status = AccountStatus.FROZEN;
        }
    }
    
    public void activate() {
        if (status == AccountStatus.FROZEN || status == AccountStatus.INACTIVE) {
            this.status = AccountStatus.ACTIVE;
        }
    }
    
    // Internal validation logic
    private void validateWithdrawal(BigDecimal amount) {
        if (status != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException("Account is not active");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }
    }
    
    private void validateDeposit(BigDecimal amount) {
        if (status == AccountStatus.CLOSED) {
            throw new AccountClosedException("Cannot deposit to closed account");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
    }
}

@Service
public class BankingService {
    
    // Directly tell objects to perform operations
    public void transferMoney(String fromAccountId, String toAccountId, BigDecimal amount) {
        BankAccount fromAccount = accountRepository.findById(fromAccountId)
            .orElseThrow(() -> new AccountNotFoundException(fromAccountId));
        BankAccount toAccount = accountRepository.findById(toAccountId)
            .orElseThrow(() -> new AccountNotFoundException(toAccountId));
        
        // Tell, Don't Ask: directly perform operations
        fromAccount.withdraw(amount);
        toAccount.deposit(amount);
        
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
    }
}
```

#### ❌ Bad Practice

```java
// ❌ Bad design: Ask, Then Tell (ask then tell)
@Entity
public class BankAccount {
    private String accountNumber;
    private BigDecimal balance;
    private AccountStatus status;
    
    // Expose internal state for external queries
    public BigDecimal getBalance() { return balance; }
    public AccountStatus getStatus() { return status; }
    
    // Simple setters without business logic
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public void setStatus(AccountStatus status) { this.status = status; }
}

@Service
public class BankingService {
    
    // Ask object state, then make decisions based on state
    public void transferMoney(String fromAccountId, String toAccountId, BigDecimal amount) {
        BankAccount fromAccount = accountRepository.findById(fromAccountId)
            .orElseThrow(() -> new AccountNotFoundException(fromAccountId));
        BankAccount toAccount = accountRepository.findById(toAccountId)
            .orElseThrow(() -> new AccountNotFoundException(toAccountId));
        
        // Ask, Then Tell: ask state then make decisions
        if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException("From account is not active");
        }
        
        if (toAccount.getStatus() == AccountStatus.CLOSED) {
            throw new AccountClosedException("To account is closed");
        }
        
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }
        
        // Directly manipulate internal state
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));
        
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
    }
}
```

## 🎯 Best Practices Summary

### SOLID Principles Application Guide

1. **SRP**: Each class should be responsible for only one business concept
2. **OCP**: Use strategy pattern, factory pattern, etc. to support extension
3. **LSP**: Ensure subclass behavior is consistent with parent class
4. **ISP**: Create small and focused interfaces
5. **DIP**: Depend on abstractions, use dependency injection

### Design Pattern Selection Guide

1. **Factory**: When you need to create complex objects or support multiple types
2. **Builder**: When objects have multiple optional parameters
3. **Strategy**: When there are multiple algorithms or business rules
4. **Observer**: When you need to decouple event publishers and subscribers
5. **Tell, Don't Ask**: A design principle that should always be prioritized

### Code Quality Checklist

- [ ] Each class has a single, clear responsibility
- [ ] Dependencies are injected through interfaces
- [ ] Business logic is encapsulated within domain objects
- [ ] Complex object creation uses Builder pattern
- [ ] Multiple algorithms use Strategy pattern
- [ ] Event handling uses Observer pattern
- [ ] Follow Tell, Don't Ask principle
- [ ] Code is easy to test and maintain
- [ ] Design patterns are used appropriately, not over-engineered

## 🔗 Related Documentation

- [Development Standards](../../../.kiro/steering/development-standards.md) - Basic development and code standards
- [Code Review Standards](../../../.kiro/steering/code-review-standards.md) - Code review process and quality standards
- [Domain Events](../../../.kiro/steering/domain-events.md) - Domain event design and implementation
- [Architecture Overview](../../architecture/overview.md) - Overall system architecture design

---

**Note**: This document provides detailed guidance on SOLID principles and design patterns. These principles and patterns should be applied consistently throughout the project to ensure code quality and maintainability.