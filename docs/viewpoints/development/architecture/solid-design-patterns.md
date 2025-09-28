# SOLID Principles and Design Patterns\n\n## Overview\n\nThis document provides a comprehensive guide to SOLID principles and design patterns, including detailed explanations of the five SOLID principles and implementation examples of common design patterns.\n\n## 🎯 SOLID Principles\n\n### 📏 Single Responsibility Principle (SRP)\n\n#### Definition\n\nA class should have only one reason to change, meaning a class should have only one responsibility.\n\n#### Implementation Example\n\n```java\n// ❌ Violates SRP: One class handles multiple responsibilities\npublic class Customer {\n    private String name;\n    private String email;\n    \n    // Customer data management responsibility\n    public void updateEmail(String email) {\n        this.email = email;\n    }\n    \n    // Data persistence responsibility - violates SRP\n    public void saveToDatabase() {\n        // Database save logic\n    }\n    \n    // Email sending responsibility - violates SRP\n    public void sendWelcomeEmail() {\n        // Email sending logic\n    }\n    \n    // Report generation responsibility - violates SRP\n    public String generateReport() {\n        return \"Customer Report: \" + name;\n    }\n}\n\n// ✅ Follows SRP: Responsibility separation\npublic class Customer {\n    private final CustomerId id;\n    private CustomerName name;\n    private Email email;\n    \n    // Only responsible for customer data management\n    public Customer(CustomerId id, CustomerName name, Email email) {\n        this.id = id;\n        this.name = name;\n        this.email = email;\n    }\n    \n    public void updateEmail(Email newEmail) {\n        this.email = newEmail;\n    }\n    \n    // Getters...\n}\n\n// Separated responsibility classes\n@Repository\npublic class CustomerRepository {\n    public void save(Customer customer) {\n        // Data persistence logic\n    }\n}\n\n@Service\npublic class CustomerEmailService {\n    public void sendWelcomeEmail(Customer customer) {\n        // Email sending logic\n    }\n}\n\n@Service\npublic class CustomerReportService {\n    public String generateReport(Customer customer) {\n        return \"Customer Report: \" + customer.getName();\n    }\n}\n```"###
 🔓 Open-Closed Principle (OCP)

#### Definition

Software entities should be open for extension but closed for modification.

#### Implementation Example

```java
// ❌ Violates OCP: Need to modify existing code every time a new discount type is added
public class DiscountCalculator {
    
    public double calculateDiscount(Customer customer, double amount) {
        if (customer.getType() == CustomerType.REGULAR) {
            return amount * 0.05; // 5% discount
        } else if (customer.getType() == CustomerType.VIP) {
            return amount * 0.10; // 10% discount
        } else if (customer.getType() == CustomerType.PREMIUM) {
            return amount * 0.15; // 15% discount
        }
        // Need to modify here every time a new customer type is added - violates OCP
        return 0;
    }
}

// ✅ Follows OCP: Use strategy pattern, open for extension, closed for modification
public interface DiscountStrategy {
    double calculateDiscount(double amount);
    boolean isApplicable(Customer customer);
}

public class RegularCustomerDiscountStrategy implements DiscountStrategy {
    @Override
    public double calculateDiscount(double amount) {
        return amount * 0.05;
    }
    
    @Override
    public boolean isApplicable(Customer customer) {
        return customer.getType() == CustomerType.REGULAR;
    }
}

public class VipCustomerDiscountStrategy implements DiscountStrategy {
    @Override
    public double calculateDiscount(double amount) {
        return amount * 0.10;
    }
    
    @Override
    public boolean isApplicable(Customer customer) {
        return customer.getType() == CustomerType.VIP;
    }
}

// When adding new customer types, only need to add new strategy classes, no need to modify existing code
public class PremiumCustomerDiscountStrategy implements DiscountStrategy {
    @Override
    public double calculateDiscount(double amount) {
        return amount * 0.15;
    }
    
    @Override
    public boolean isApplicable(Customer customer) {
        return customer.getType() == CustomerType.PREMIUM;
    }
}

@Service
public class DiscountCalculator {
    
    private final List<DiscountStrategy> discountStrategies;
    
    public DiscountCalculator(List<DiscountStrategy> discountStrategies) {
        this.discountStrategies = discountStrategies;
    }
    
    public double calculateDiscount(Customer customer, double amount) {
        return discountStrategies.stream()
            .filter(strategy -> strategy.isApplicable(customer))
            .findFirst()
            .map(strategy -> strategy.calculateDiscount(amount))
            .orElse(0.0);
    }
}
```

### 🔄 Liskov Substitution Principle (LSP)

#### Definition

Subtypes must be substitutable for their base types without altering the correctness of the program.

#### Implementation Example

```java
// ❌ Violates LSP: Subclass changes the behavior contract of the base class
public abstract class Bird {
    public abstract void fly();
}

public class Sparrow extends Bird {
    @Override
    public void fly() {
        System.out.println("Sparrow is flying");
    }
}

public class Penguin extends Bird {
    @Override
    public void fly() {
        throw new UnsupportedOperationException("Penguins cannot fly");
        // Violates LSP: Changes the behavior contract of the base class
    }
}

// ✅ Follows LSP: Redesign class hierarchy
public abstract class Bird {
    public abstract void move();
}

public interface Flyable {
    void fly();
}

public class Sparrow extends Bird implements Flyable {
    @Override
    public void move() {
        fly();
    }
    
    @Override
    public void fly() {
        System.out.println("Sparrow is flying");
    }
}

public class Penguin extends Bird {
    @Override
    public void move() {
        swim();
    }
    
    public void swim() {
        System.out.println("Penguin is swimming");
    }
}

// Usage example
public class BirdHandler {
    
    public void handleBird(Bird bird) {
        bird.move(); // All Bird subclasses can execute correctly
    }
    
    public void handleFlyableBird(Flyable flyable) {
        flyable.fly(); // Only flying birds implement this interface
    }
}
```

### 🔌 Interface Segregation Principle (ISP)

#### Definition

Clients should not be forced to depend on interfaces they do not use.

#### Implementation Example

```java
// ❌ Violates ISP: Fat interface forces clients to depend on methods they don't need
public interface Worker {
    void work();
    void eat();
    void sleep();
    void attendMeeting();
    void writeReport();
}

public class Developer implements Worker {
    @Override
    public void work() {
        System.out.println("Writing code");
    }
    
    @Override
    public void eat() {
        System.out.println("Eating lunch");
    }
    
    @Override
    public void sleep() {
        System.out.println("Sleeping");
    }
    
    @Override
    public void attendMeeting() {
        System.out.println("Attending meeting");
    }
    
    @Override
    public void writeReport() {
        // Developers might not need to write reports
        throw new UnsupportedOperationException("Developers don't write reports");
    }
}

// ✅ Follows ISP: Split large interface into small, specialized interfaces
public interface Workable {
    void work();
}

public interface Eatable {
    void eat();
}

public interface Sleepable {
    void sleep();
}

public interface Meetable {
    void attendMeeting();
}

public interface Reportable {
    void writeReport();
}

public class Developer implements Workable, Eatable, Sleepable, Meetable {
    @Override
    public void work() {
        System.out.println("Writing code");
    }
    
    @Override
    public void eat() {
        System.out.println("Eating lunch");
    }
    
    @Override
    public void sleep() {
        System.out.println("Sleeping");
    }
    
    @Override
    public void attendMeeting() {
        System.out.println("Attending meeting");
    }
    // No need to implement Reportable because developers don't write reports
}

public class Manager implements Workable, Eatable, Sleepable, Meetable, Reportable {
    @Override
    public void work() {
        System.out.println("Managing team");
    }
    
    @Override
    public void eat() {
        System.out.println("Eating lunch");
    }
    
    @Override
    public void sleep() {
        System.out.println("Sleeping");
    }
    
    @Override
    public void attendMeeting() {
        System.out.println("Attending meeting");
    }
    
    @Override
    public void writeReport() {
        System.out.println("Writing management report");
    }
}
```

### 🔄 Dependency Inversion Principle (DIP)

#### Definition

High-level modules should not depend on low-level modules. Both should depend on abstractions. Abstractions should not depend on details. Details should depend on abstractions.

#### Implementation Example

```java
// ❌ Violates DIP: High-level module directly depends on low-level module
public class EmailService {
    public void sendEmail(String to, String subject, String body) {
        // Direct email sending implementation
        System.out.println("Sending email to: " + to);
    }
}

public class OrderService {
    private EmailService emailService; // Direct dependency on concrete implementation
    
    public OrderService() {
        this.emailService = new EmailService(); // Tight coupling
    }
    
    public void processOrder(Order order) {
        // Order processing logic
        
        // Send confirmation email
        emailService.sendEmail(
            order.getCustomerEmail(),
            "Order Confirmation",
            "Your order has been processed"
        );
    }
}

// ✅ Follows DIP: Depend on abstractions, not concrete implementations
public interface NotificationService {
    void sendNotification(String to, String subject, String message);
}

public class EmailNotificationService implements NotificationService {
    @Override
    public void sendNotification(String to, String subject, String message) {
        System.out.println("Sending email to: " + to);
        System.out.println("Subject: " + subject);
        System.out.println("Message: " + message);
    }
}

public class SmsNotificationService implements NotificationService {
    @Override
    public void sendNotification(String to, String subject, String message) {
        System.out.println("Sending SMS to: " + to);
        System.out.println("Message: " + subject + " - " + message);
    }
}

@Service
public class OrderService {
    
    private final NotificationService notificationService; // Depend on abstraction
    
    public OrderService(NotificationService notificationService) {
        this.notificationService = notificationService; // Dependency injection
    }
    
    public void processOrder(Order order) {
        // Order processing logic
        
        // Send confirmation notification (can be email or SMS)
        notificationService.sendNotification(
            order.getCustomerContact(),
            "Order Confirmation",
            "Your order has been processed"
        );
    }
}

// Spring configuration
@Configuration
public class NotificationConfiguration {
    
    @Bean
    @Primary
    public NotificationService emailNotificationService() {
        return new EmailNotificationService();
    }
    
    @Bean
    public NotificationService smsNotificationService() {
        return new SmsNotificationService();
    }
}
```## 🎨 Desig
n Patterns

### 🏭 Factory Pattern

#### Factory Method Pattern

```java
// Product interface
public interface Customer {
    void register();
    CustomerType getType();
}

// Concrete products
public class RegularCustomer implements Customer {
    private final String name;
    private final String email;
    
    public RegularCustomer(String name, String email) {
        this.name = name;
        this.email = email;
    }
    
    @Override
    public void register() {
        System.out.println("Registering regular customer: " + name);
    }
    
    @Override
    public CustomerType getType() {
        return CustomerType.REGULAR;
    }
}

public class VipCustomer implements Customer {
    private final String name;
    private final String email;
    private final String vipCode;
    
    public VipCustomer(String name, String email, String vipCode) {
        this.name = name;
        this.email = email;
        this.vipCode = vipCode;
    }
    
    @Override
    public void register() {
        System.out.println("Registering VIP customer: " + name + " with code: " + vipCode);
    }
    
    @Override
    public CustomerType getType() {
        return CustomerType.VIP;
    }
}

// Abstract factory
public abstract class CustomerFactory {
    
    public abstract Customer createCustomer(CustomerRegistrationData data);
    
    // Template method
    public Customer registerCustomer(CustomerRegistrationData data) {
        Customer customer = createCustomer(data);
        customer.register();
        return customer;
    }
}

// Concrete factories
@Component
public class RegularCustomerFactory extends CustomerFactory {
    
    @Override
    public Customer createCustomer(CustomerRegistrationData data) {
        return new RegularCustomer(data.getName(), data.getEmail());
    }
}

@Component
public class VipCustomerFactory extends CustomerFactory {
    
    @Override
    public Customer createCustomer(CustomerRegistrationData data) {
        return new VipCustomer(
            data.getName(), 
            data.getEmail(), 
            data.getVipCode()
        );
    }
}

// Factory selector
@Service
public class CustomerFactorySelector {
    
    private final Map<CustomerType, CustomerFactory> factories;
    
    public CustomerFactorySelector(List<CustomerFactory> factoryList) {
        this.factories = factoryList.stream()
            .collect(Collectors.toMap(
                factory -> determineType(factory),
                Function.identity()
            ));
    }
    
    public CustomerFactory getFactory(CustomerType type) {
        CustomerFactory factory = factories.get(type);
        if (factory == null) {
            throw new IllegalArgumentException("No factory found for type: " + type);
        }
        return factory;
    }
    
    private CustomerType determineType(CustomerFactory factory) {
        if (factory instanceof RegularCustomerFactory) {
            return CustomerType.REGULAR;
        } else if (factory instanceof VipCustomerFactory) {
            return CustomerType.VIP;
        }
        throw new IllegalArgumentException("Unknown factory type");
    }
}
```

### 🔨 Builder Pattern

```java
// Complex object
public class Order {
    private final String orderId;
    private final String customerId;
    private final List<OrderItem> items;
    private final Address shippingAddress;
    private final Address billingAddress;
    private final PaymentMethod paymentMethod;
    private final LocalDateTime orderDate;
    private final String notes;
    private final boolean expressDelivery;
    private final boolean giftWrap;
    
    // Private constructor, can only be created through Builder
    private Order(Builder builder) {
        this.orderId = builder.orderId;
        this.customerId = builder.customerId;
        this.items = Collections.unmodifiableList(new ArrayList<>(builder.items));
        this.shippingAddress = builder.shippingAddress;
        this.billingAddress = builder.billingAddress;
        this.paymentMethod = builder.paymentMethod;
        this.orderDate = builder.orderDate;
        this.notes = builder.notes;
        this.expressDelivery = builder.expressDelivery;
        this.giftWrap = builder.giftWrap;
    }
    
    // Builder class
    public static class Builder {
        // Required parameters
        private final String customerId;
        private final List<OrderItem> items = new ArrayList<>();
        
        // Optional parameters - initialized with default values
        private String orderId;
        private Address shippingAddress;
        private Address billingAddress;
        private PaymentMethod paymentMethod;
        private LocalDateTime orderDate = LocalDateTime.now();
        private String notes = "";
        private boolean expressDelivery = false;
        private boolean giftWrap = false;
        
        public Builder(String customerId) {
            this.customerId = customerId;
            this.orderId = generateOrderId();
        }
        
        public Builder addItem(OrderItem item) {
            this.items.add(item);
            return this;
        }
        
        public Builder addItems(List<OrderItem> items) {
            this.items.addAll(items);
            return this;
        }
        
        public Builder shippingAddress(Address address) {
            this.shippingAddress = address;
            return this;
        }
        
        public Builder billingAddress(Address address) {
            this.billingAddress = address;
            return this;
        }
        
        public Builder paymentMethod(PaymentMethod method) {
            this.paymentMethod = method;
            return this;
        }
        
        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }
        
        public Builder expressDelivery(boolean express) {
            this.expressDelivery = express;
            return this;
        }
        
        public Builder giftWrap(boolean wrap) {
            this.giftWrap = wrap;
            return this;
        }
        
        public Order build() {
            validate();
            return new Order(this);
        }
        
        private void validate() {
            if (items.isEmpty()) {
                throw new IllegalStateException("Order must have at least one item");
            }
            if (shippingAddress == null) {
                throw new IllegalStateException("Shipping address is required");
            }
            if (paymentMethod == null) {
                throw new IllegalStateException("Payment method is required");
            }
        }
        
        private String generateOrderId() {
            return "ORD-" + System.currentTimeMillis();
        }
    }
    
    // Getters...
}

// Usage example
public class OrderService {
    
    public Order createOrder(CreateOrderRequest request) {
        return new Order.Builder(request.getCustomerId())
            .addItems(request.getItems())
            .shippingAddress(request.getShippingAddress())
            .billingAddress(request.getBillingAddress())
            .paymentMethod(request.getPaymentMethod())
            .notes(request.getNotes())
            .expressDelivery(request.isExpressDelivery())
            .giftWrap(request.isGiftWrap())
            .build();
    }
}
```### 📋 St
rategy Pattern

```java
// Strategy interface
public interface PaymentStrategy {
    PaymentResult processPayment(PaymentRequest request);
    boolean supports(PaymentMethod method);
}

// Concrete strategies
@Component
public class CreditCardPaymentStrategy implements PaymentStrategy {
    
    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        // Credit card payment logic
        System.out.println("Processing credit card payment: " + request.getAmount());
        
        // Simulate payment processing
        if (validateCreditCard(request.getCreditCardInfo())) {
            return PaymentResult.success(generateTransactionId());
        } else {
            return PaymentResult.failure("Invalid credit card information");
        }
    }
    
    @Override
    public boolean supports(PaymentMethod method) {
        return method == PaymentMethod.CREDIT_CARD;
    }
    
    private boolean validateCreditCard(CreditCardInfo cardInfo) {
        // Credit card validation logic
        return cardInfo != null && cardInfo.getCardNumber().length() == 16;
    }
    
    private String generateTransactionId() {
        return "CC-" + System.currentTimeMillis();
    }
}

@Component
public class PayPalPaymentStrategy implements PaymentStrategy {
    
    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        // PayPal payment logic
        System.out.println("Processing PayPal payment: " + request.getAmount());
        
        if (validatePayPalAccount(request.getPayPalInfo())) {
            return PaymentResult.success(generateTransactionId());
        } else {
            return PaymentResult.failure("Invalid PayPal account");
        }
    }
    
    @Override
    public boolean supports(PaymentMethod method) {
        return method == PaymentMethod.PAYPAL;
    }
    
    private boolean validatePayPalAccount(PayPalInfo payPalInfo) {
        // PayPal account validation logic
        return payPalInfo != null && payPalInfo.getEmail().contains("@");
    }
    
    private String generateTransactionId() {
        return "PP-" + System.currentTimeMillis();
    }
}

// Context class
@Service
public class PaymentProcessor {
    
    private final List<PaymentStrategy> paymentStrategies;
    
    public PaymentProcessor(List<PaymentStrategy> paymentStrategies) {
        this.paymentStrategies = paymentStrategies;
    }
    
    public PaymentResult processPayment(PaymentRequest request) {
        PaymentStrategy strategy = findStrategy(request.getPaymentMethod());
        
        if (strategy == null) {
            return PaymentResult.failure("Unsupported payment method: " + request.getPaymentMethod());
        }
        
        return strategy.processPayment(request);
    }
    
    private PaymentStrategy findStrategy(PaymentMethod method) {
        return paymentStrategies.stream()
            .filter(strategy -> strategy.supports(method))
            .findFirst()
            .orElse(null);
    }
}
```

### 👁️ Observer Pattern

```java
// Event interface
public interface DomainEvent {
    UUID getEventId();
    LocalDateTime getOccurredOn();
    String getEventType();
}

// Concrete event
public record OrderCreatedEvent(
    UUID eventId,
    LocalDateTime occurredOn,
    String orderId,
    String customerId,
    BigDecimal totalAmount
) implements DomainEvent {
    
    public static OrderCreatedEvent create(String orderId, String customerId, BigDecimal totalAmount) {
        return new OrderCreatedEvent(
            UUID.randomUUID(),
            LocalDateTime.now(),
            orderId,
            customerId,
            totalAmount
        );
    }
    
    @Override
    public String getEventType() {
        return "OrderCreated";
    }
}

// Observer interface
public interface DomainEventHandler<T extends DomainEvent> {
    void handle(T event);
    Class<T> getSupportedEventType();
}

// Concrete observers
@Component
public class OrderCreatedEmailHandler implements DomainEventHandler<OrderCreatedEvent> {
    
    private final EmailService emailService;
    private final CustomerService customerService;
    
    public OrderCreatedEmailHandler(EmailService emailService, CustomerService customerService) {
        this.emailService = emailService;
        this.customerService = customerService;
    }
    
    @Override
    public void handle(OrderCreatedEvent event) {
        Customer customer = customerService.findById(event.customerId());
        
        emailService.sendOrderConfirmationEmail(
            customer.getEmail(),
            event.orderId(),
            event.totalAmount()
        );
        
        System.out.println("Order confirmation email sent for order: " + event.orderId());
    }
    
    @Override
    public Class<OrderCreatedEvent> getSupportedEventType() {
        return OrderCreatedEvent.class;
    }
}

@Component
public class OrderCreatedInventoryHandler implements DomainEventHandler<OrderCreatedEvent> {
    
    private final InventoryService inventoryService;
    
    public OrderCreatedInventoryHandler(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }
    
    @Override
    public void handle(OrderCreatedEvent event) {
        inventoryService.reserveItems(event.orderId());
        System.out.println("Inventory reserved for order: " + event.orderId());
    }
    
    @Override
    public Class<OrderCreatedEvent> getSupportedEventType() {
        return OrderCreatedEvent.class;
    }
}

// Event publisher
@Service
public class DomainEventPublisher {
    
    private final List<DomainEventHandler<?>> eventHandlers;
    
    public DomainEventPublisher(List<DomainEventHandler<?>> eventHandlers) {
        this.eventHandlers = eventHandlers;
    }
    
    @SuppressWarnings("unchecked")
    public <T extends DomainEvent> void publish(T event) {
        eventHandlers.stream()
            .filter(handler -> handler.getSupportedEventType().isInstance(event))
            .forEach(handler -> {
                try {
                    ((DomainEventHandler<T>) handler).handle(event);
                } catch (Exception e) {
                    System.err.println("Error handling event: " + e.getMessage());
                }
            });
    }
}

// Usage example
@Service
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final DomainEventPublisher eventPublisher;
    
    public OrderService(OrderRepository orderRepository, DomainEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }
    
    public Order createOrder(CreateOrderRequest request) {
        Order order = new Order.Builder(request.getCustomerId())
            .addItems(request.getItems())
            .shippingAddress(request.getShippingAddress())
            .paymentMethod(request.getPaymentMethod())
            .build();
        
        Order savedOrder = orderRepository.save(order);
        
        // Publish event to notify all observers
        OrderCreatedEvent event = OrderCreatedEvent.create(
            savedOrder.getOrderId(),
            savedOrder.getCustomerId(),
            savedOrder.getTotalAmount()
        );
        
        eventPublisher.publish(event);
        
        return savedOrder;
    }
}
```### 🙈 Tell
 Don't Ask Principle

```java
// ❌ Ask: Query object state then make decisions
public class OrderProcessor {
    
    public void processOrder(Order order) {
        // Query order state
        if (order.getStatus() == OrderStatus.PENDING) {
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                if (order.getItems().size() > 0) {
                    // External logic decides how to handle
                    order.setStatus(OrderStatus.CONFIRMED);
                    order.setConfirmedAt(LocalDateTime.now());
                    
                    // More external logic
                    for (OrderItem item : order.getItems()) {
                        if (item.getQuantity() > 0) {
                            // Process each item
                        }
                    }
                }
            }
        }
    }
}

// ✅ Tell: Tell object what to do, let object decide how to do it
public class Order {
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private List<OrderItem> items;
    private LocalDateTime confirmedAt;
    
    // Tell: Tell order to confirm itself
    public void confirm() {
        validateCanBeConfirmed();
        
        this.status = OrderStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        
        // Order knows how to handle confirmation logic
        notifyItemsOfConfirmation();
    }
    
    // Tell: Tell order to handle payment
    public void markAsPaid() {
        validateCanBePaid();
        
        this.paymentStatus = PaymentStatus.PAID;
        
        // If conditions are met, automatically confirm order
        if (canBeAutoConfirmed()) {
            confirm();
        }
    }
    
    // Internal logic, external doesn't need to know
    private void validateCanBeConfirmed() {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be confirmed");
        }
        if (paymentStatus != PaymentStatus.PAID) {
            throw new IllegalStateException("Order must be paid before confirmation");
        }
        if (items.isEmpty()) {
            throw new IllegalStateException("Order must have items to be confirmed");
        }
    }
    
    private void validateCanBePaid() {
        if (status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cancelled orders cannot be paid");
        }
    }
    
    private boolean canBeAutoConfirmed() {
        return status == OrderStatus.PENDING && 
               paymentStatus == PaymentStatus.PAID && 
               !items.isEmpty();
    }
    
    private void notifyItemsOfConfirmation() {
        items.forEach(OrderItem::reserve);
    }
}

public class OrderProcessor {
    
    public void processOrder(Order order) {
        // Tell: Directly tell order what to do
        order.confirm();
        
        // Clean and clear, no need to know internal logic
    }
    
    public void processPayment(Order order) {
        // Tell: Tell order to mark as paid
        order.markAsPaid();
        
        // Order will automatically handle subsequent logic
    }
}
```

## Best Practices Summary

### SOLID Principles Application

1. **SRP**: Each class is responsible for only one responsibility, changes only affect one class when responsibility changes
2. **OCP**: Use abstraction and polymorphism to achieve extensibility, avoid modifying existing code
3. **LSP**: Ensure subclasses can completely replace parent classes without breaking program correctness
4. **ISP**: Design small, specialized interfaces, avoid forcing clients to depend on methods they don't need
5. **DIP**: Depend on abstractions rather than concrete implementations, use dependency injection to achieve loose coupling

### Design Pattern Selection

1. **Factory**: When object creation logic is complex or need to create different types of objects based on conditions
2. **Builder**: When objects have multiple optional parameters or complex creation process
3. **Strategy**: When there are multiple algorithms or behaviors that need to be selected at runtime
4. **Observer**: When object state changes need to notify multiple dependent objects
5. **Tell Don't Ask**: Let objects manage their own state and behavior, improve encapsulation

### Implementation Recommendations

1. **Gradual Refactoring**: Don't refactor all code at once, gradually apply principles
2. **Test Protection**: Ensure adequate test coverage before refactoring
3. **Team Consensus**: Ensure team has common understanding of design principles and patterns
4. **Moderate Application**: Don't over-design, choose appropriate patterns based on actual needs

---

**Related Documents**

- [DDD Domain-Driven Design](ddd-domain-driven-design.md)
- [Hexagonal Architecture](hexagonal-architecture.md)
- Microservices Architecture