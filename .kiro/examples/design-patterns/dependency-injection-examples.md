# Dependency Injection - Comprehensive Examples

## Principle Overview

**Dependency Injection (DI)** is a design pattern that implements Inversion of Control (IoC) for resolving dependencies. Instead of objects creating their own dependencies, dependencies are provided (injected) from the outside.

**Key Benefits:**
- Loose coupling between components
- Easier testing with mock objects
- Better code reusability
- Clearer dependencies and contracts

---

## Example 1: Order Processing Service

### ❌ Bad: Hard-Coded Dependencies

```java
public class OrderService {
    // Creating dependencies directly - tightly coupled!
    private final OrderValidator validator = new OrderValidator();
    private final PriceCalculator calculator = new PriceCalculator();
    private final PaymentProcessor paymentProcessor = new CreditCardPaymentProcessor();
    private final EmailService emailService = new SmtpEmailService();
    
    public OrderResult processOrder(OrderRequest request) {
        // Cannot test with mocks
        // Cannot change implementations
        // Hard to configure
        return null;
    }
}
```

### ✅ Good: Constructor Injection

```java
@Service
public class OrderService {
    private final OrderValidator validator;
    private final PriceCalculator calculator;
    private final PaymentProcessor paymentProcessor;
    private final EmailService emailService;
    
    // Dependencies injected through constructor
    public OrderService(
        OrderValidator validator,
        PriceCalculator calculator,
        PaymentProcessor paymentProcessor,
        EmailService emailService
    ) {
        this.validator = validator;
        this.calculator = calculator;
        this.paymentProcessor = paymentProcessor;
        this.emailService = emailService;
    }
    
    public OrderResult processOrder(OrderRequest request) {
        // Easy to test with mocks
        // Can swap implementations
        // Configuration handled externally
        validator.validate(request);
        Money total = calculator.calculateTotal(request);
        PaymentResult payment = paymentProcessor.process(total);
        emailService.sendConfirmation(request.getCustomerEmail());
        return OrderResult.success();
    }
}
```

---

## Example 2: Testing with Dependency Injection

### Easy Testing with Mocks

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    
    @Mock
    private OrderValidator validator;
    
    @Mock
    private PriceCalculator calculator;
    
    @Mock
    private PaymentProcessor paymentProcessor;
    
    @Mock
    private EmailService emailService;
    
    @InjectMocks
    private OrderService orderService;
    
    @Test
    void should_process_order_successfully() {
        // Given
        OrderRequest request = createOrderRequest();
        when(calculator.calculateTotal(request)).thenReturn(Money.of(100, "USD"));
        when(paymentProcessor.process(any())).thenReturn(PaymentResult.success());
        
        // When
        OrderResult result = orderService.processOrder(request);
        
        // Then
        assertThat(result.isSuccessful()).isTrue();
        verify(validator).validate(request);
        verify(emailService).sendConfirmation(request.getCustomerEmail());
    }
}
```

---

## Example 3: Configuration with Spring

### Interface-Based Injection

```java
// Define interfaces
public interface PaymentProcessor {
    PaymentResult process(Money amount);
}

public interface EmailService {
    void sendConfirmation(String email);
}

// Multiple implementations
@Component
public class CreditCardPaymentProcessor implements PaymentProcessor {
    @Override
    public PaymentResult process(Money amount) {
        // Credit card processing
        return PaymentResult.success();
    }
}

@Component
public class PayPalPaymentProcessor implements PaymentProcessor {
    @Override
    public PaymentResult process(Money amount) {
        // PayPal processing
        return PaymentResult.success();
    }
}

// Configuration
@Configuration
public class PaymentConfiguration {
    
    @Bean
    @Primary
    public PaymentProcessor paymentProcessor() {
        return new CreditCardPaymentProcessor();
    }
    
    @Bean
    @Qualifier("paypal")
    public PaymentProcessor paypalProcessor() {
        return new PayPalPaymentProcessor();
    }
}

// Usage with qualifier
@Service
public class OrderService {
    private final PaymentProcessor paymentProcessor;
    
    public OrderService(@Qualifier("paypal") PaymentProcessor paymentProcessor) {
        this.paymentProcessor = paymentProcessor;
    }
}
```

---

## Key Principles

### 1. Depend on Abstractions

```java
// ✅ Good: Depend on interface
public class OrderService {
    private final OrderRepository repository;
    
    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }
}

// ❌ Bad: Depend on concrete class
public class OrderService {
    private final JpaOrderRepository repository;
    
    public OrderService(JpaOrderRepository repository) {
        this.repository = repository;
    }
}
```

### 2. Use Constructor Injection

```java
// ✅ Good: Constructor injection
@Service
public class CustomerService {
    private final CustomerRepository repository;
    
    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }
}

// ❌ Bad: Field injection
@Service
public class CustomerService {
    @Autowired
    private CustomerRepository repository;
}
```

### 3. Make Dependencies Explicit

```java
// ✅ Good: Clear dependencies
public class OrderProcessor {
    private final OrderValidator validator;
    private final PriceCalculator calculator;
    private final InventoryService inventory;
    
    public OrderProcessor(
        OrderValidator validator,
        PriceCalculator calculator,
        InventoryService inventory
    ) {
        this.validator = validator;
        this.calculator = calculator;
        this.inventory = inventory;
    }
}
```

---

## Related Patterns

- **Dependency Inversion Principle (DIP)**: Depend on abstractions
- **Inversion of Control (IoC)**: Framework controls object creation
- **Service Locator**: Alternative to DI (not recommended)

## Further Reading

- [Design Principles](../../steering/design-principles.md)
- [SOLID Principles](../../steering/design-principles.md#solid-principles)
