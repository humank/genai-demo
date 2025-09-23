# SOLID 原則與設計模式

## 概覽

本文檔涵蓋了軟體開發中的核心設計原則和模式，包括 SOLID 原則和常用設計模式。這些原則和模式是構建可維護、可擴展和高品質軟體的基礎。

## 🎯 SOLID 原則

SOLID 原則是物件導向設計的五個基本原則，由 Robert C. Martin 提出，旨在使軟體設計更加理解、靈活和可維護。

### 📏 單一職責原則 (Single Responsibility Principle, SRP)

**定義**: 一個類別應該只有一個引起它變化的原因，即一個類別應該只有一個職責。

#### ✅ 良好實踐

```java
// ✅ 好的設計：每個類別都有單一職責
@Entity
public class Customer {
    private String id;
    private String name;
    private String email;
    
    // 只負責客戶資料的管理
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
    
    // 只負責客戶業務邏輯
    public Customer createCustomer(CreateCustomerCommand command) {
        Customer customer = new Customer(command.getName(), command.getEmail());
        return customerRepository.save(customer);
    }
}

@Component
public class CustomerNotificationService {
    private final EmailService emailService;
    
    // 只負責客戶通知
    public void sendWelcomeEmail(Customer customer) {
        String subject = "Welcome to our service!";
        String body = "Hello " + customer.getName() + ", welcome!";
        emailService.send(customer.getEmail(), subject, body);
    }
}
```

#### ❌ 不良實踐

```java
// ❌ 壞的設計：一個類別承擔多個職責
@Service
public class CustomerManager {
    
    // 職責1：客戶資料管理
    public Customer createCustomer(String name, String email) {
        Customer customer = new Customer(name, email);
        return saveToDatabase(customer);
    }
    
    // 職責2：資料庫操作
    private Customer saveToDatabase(Customer customer) {
        // 直接處理資料庫邏輯
        return customer;
    }
    
    // 職責3：發送郵件
    public void sendWelcomeEmail(Customer customer) {
        // 直接處理郵件發送邏輯
    }
    
    // 職責4：生成報告
    public String generateCustomerReport(Customer customer) {
        // 直接處理報告生成邏輯
        return "Report for " + customer.getName();
    }
}
```

### 🔓 開放封閉原則 (Open-Closed Principle, OCP)

**定義**: 軟體實體（類別、模組、函數等）應該對擴展開放，對修改封閉。

#### ✅ 良好實踐

```java
// ✅ 好的設計：使用策略模式實現 OCP
public interface DiscountStrategy {
    BigDecimal calculateDiscount(Order order);
}

@Component
public class RegularCustomerDiscount implements DiscountStrategy {
    @Override
    public BigDecimal calculateDiscount(Order order) {
        return order.getTotal().multiply(new BigDecimal("0.05")); // 5% 折扣
    }
}

@Component
public class PremiumCustomerDiscount implements DiscountStrategy {
    @Override
    public BigDecimal calculateDiscount(Order order) {
        return order.getTotal().multiply(new BigDecimal("0.10")); // 10% 折扣
    }
}

@Component
public class VipCustomerDiscount implements DiscountStrategy {
    @Override
    public BigDecimal calculateDiscount(Order order) {
        return order.getTotal().multiply(new BigDecimal("0.15")); // 15% 折扣
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

#### ❌ 不良實踐

```java
// ❌ 壞的設計：每次新增客戶類型都需要修改現有程式碼
@Service
public class OrderService {
    
    public BigDecimal calculateOrderTotal(Order order, CustomerType customerType) {
        BigDecimal total = order.getTotal();
        
        // 每次新增客戶類型都需要修改這個方法
        switch (customerType) {
            case REGULAR:
                return total.multiply(new BigDecimal("0.95")); // 5% 折扣
            case PREMIUM:
                return total.multiply(new BigDecimal("0.90")); // 10% 折扣
            case VIP:
                return total.multiply(new BigDecimal("0.85")); // 15% 折扣
            // 新增 DIAMOND 客戶時需要修改這裡
            default:
                return total;
        }
    }
}
```

### 🔄 里氏替換原則 (Liskov Substitution Principle, LSP)

**定義**: 子類別必須能夠替換其基類別，而不會改變程式的正確性。

#### ✅ 良好實踐

```java
// ✅ 好的設計：子類別可以完全替換父類別
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
        // 信用卡處理邏輯
        return PaymentResult.success(request.getAmount());
    }
}

@Component
public class PayPalProcessor extends PaymentProcessor {
    
    @Override
    protected PaymentResult doProcessPayment(PaymentRequest request) {
        // PayPal 處理邏輯
        return PaymentResult.success(request.getAmount());
    }
}

@Service
public class PaymentService {
    
    // 可以使用任何 PaymentProcessor 的子類別
    public PaymentResult processPayment(PaymentProcessor processor, PaymentRequest request) {
        return processor.processPayment(request); // LSP 原則：子類別可以替換父類別
    }
}
```

#### ❌ 不良實踐

```java
// ❌ 壞的設計：子類別改變了父類別的行為契約
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
        // 違反 LSP：現金支付不需要驗證金額？
        // 跳過父類別的驗證邏輯
        return doProcessPayment(request);
    }
    
    @Override
    protected PaymentResult doProcessPayment(PaymentRequest request) {
        return PaymentResult.success(request.getAmount());
    }
}
```

### 🔌 介面隔離原則 (Interface Segregation Principle, ISP)

**定義**: 客戶端不應該被迫依賴它們不使用的介面。

#### ✅ 良好實踐

```java
// ✅ 好的設計：將大介面拆分為多個小介面
public interface Readable {
    String read();
}

public interface Writable {
    void write(String content);
}

public interface Deletable {
    void delete();
}

// 只需要讀取功能的類別
@Component
public class LogReader implements Readable {
    
    @Override
    public String read() {
        return "Log content";
    }
}

// 需要讀寫功能的類別
@Component
public class ConfigurationManager implements Readable, Writable {
    
    @Override
    public String read() {
        return "Configuration content";
    }
    
    @Override
    public void write(String content) {
        // 寫入配置
    }
}

// 需要所有功能的類別
@Component
public class FileManager implements Readable, Writable, Deletable {
    
    @Override
    public String read() {
        return "File content";
    }
    
    @Override
    public void write(String content) {
        // 寫入檔案
    }
    
    @Override
    public void delete() {
        // 刪除檔案
    }
}
```

#### ❌ 不良實踐

```java
// ❌ 壞的設計：強迫客戶端實現不需要的方法
public interface FileOperations {
    String read();
    void write(String content);
    void delete();
    void compress();
    void encrypt();
}

// 只需要讀取功能，但被迫實現所有方法
@Component
public class LogReader implements FileOperations {
    
    @Override
    public String read() {
        return "Log content";
    }
    
    // 被迫實現不需要的方法
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

### 🔄 依賴反轉原則 (Dependency Inversion Principle, DIP)

**定義**: 高層模組不應該依賴低層模組，兩者都應該依賴抽象。抽象不應該依賴細節，細節應該依賴抽象。

#### ✅ 良好實踐

```java
// ✅ 好的設計：依賴抽象而不是具體實現
public interface NotificationService {
    void sendNotification(String recipient, String message);
}

public interface CustomerRepository {
    Customer save(Customer customer);
    Optional<Customer> findById(String id);
}

// 高層模組依賴抽象
@Service
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final NotificationService notificationService;
    
    // 依賴注入抽象介面
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

// 低層模組實現抽象
@Repository
public class JpaCustomerRepository implements CustomerRepository {
    
    @Override
    public Customer save(Customer customer) {
        // JPA 實現
        return customer;
    }
    
    @Override
    public Optional<Customer> findById(String id) {
        // JPA 實現
        return Optional.empty();
    }
}

@Component
public class EmailNotificationService implements NotificationService {
    
    @Override
    public void sendNotification(String recipient, String message) {
        // 郵件發送實現
    }
}
```

#### ❌ 不良實踐

```java
// ❌ 壞的設計：高層模組直接依賴低層模組的具體實現
@Service
public class CustomerService {
    private final JpaCustomerRepository customerRepository; // 直接依賴具體實現
    private final EmailService emailService; // 直接依賴具體實現
    
    public CustomerService() {
        this.customerRepository = new JpaCustomerRepository(); // 直接創建依賴
        this.emailService = new EmailService(); // 直接創建依賴
    }
    
    public Customer createCustomer(CreateCustomerCommand command) {
        Customer customer = new Customer(command.getName(), command.getEmail());
        Customer savedCustomer = customerRepository.save(customer);
        
        // 直接調用具體實現
        emailService.sendEmail(savedCustomer.getEmail(), "Welcome!");
        
        return savedCustomer;
    }
}
```

## 🎨 設計模式

設計模式是解決軟體設計中常見問題的可重用解決方案。以下是專案中常用的設計模式。

### 🏭 Factory 模式

**目的**: 創建對象而不指定其具體類別，將對象創建邏輯封裝在工廠類別中。

#### ✅ 實現範例

```java
// 產品介面
public interface PaymentProcessor {
    PaymentResult process(PaymentRequest request);
}

// 具體產品
@Component
public class CreditCardProcessor implements PaymentProcessor {
    
    @Override
    public PaymentResult process(PaymentRequest request) {
        // 信用卡處理邏輯
        return PaymentResult.success("Credit card payment processed");
    }
}

@Component
public class PayPalProcessor implements PaymentProcessor {
    
    @Override
    public PaymentResult process(PaymentRequest request) {
        // PayPal 處理邏輯
        return PaymentResult.success("PayPal payment processed");
    }
}

@Component
public class BankTransferProcessor implements PaymentProcessor {
    
    @Override
    public PaymentResult process(PaymentRequest request) {
        // 銀行轉帳處理邏輯
        return PaymentResult.success("Bank transfer processed");
    }
}

// 工廠類別
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

// 使用工廠
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

### 🔨 Builder 模式

**目的**: 逐步構建複雜對象，允許創建不同表示的同一對象。

#### ✅ 實現範例

```java
// 複雜對象
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
    
    // 私有建構子，只能通過 Builder 創建
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
    
    // Builder 類別
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
    
    // 靜態工廠方法
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

// 使用 Builder
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

### 📋 Strategy 模式

**目的**: 定義一系列算法，將每個算法封裝起來，並使它們可以互換。

#### ✅ 實現範例

```java
// 策略介面
public interface PricingStrategy {
    BigDecimal calculatePrice(Product product, int quantity);
    String getStrategyName();
}

// 具體策略
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
            return basePrice.multiply(new BigDecimal("0.8")); // 20% 折扣
        } else if (quantity >= 50) {
            return basePrice.multiply(new BigDecimal("0.9")); // 10% 折扣
        } else if (quantity >= 10) {
            return basePrice.multiply(new BigDecimal("0.95")); // 5% 折扣
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
        
        // 檢查是否為促銷季節
        if (isPromotionalSeason()) {
            return basePrice.multiply(new BigDecimal("0.85")); // 15% 季節性折扣
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

// 上下文類別
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

### 👁️ Observer 模式

**目的**: 定義對象間的一對多依賴關係，當一個對象的狀態發生改變時，所有依賴於它的對象都會得到通知。

#### ✅ 實現範例

```java
// 事件（主題）
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

// 觀察者介面
public interface OrderStatusObserver {
    void onOrderStatusChanged(OrderStatusChangedEvent event);
}

// 具體觀察者
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
            // 訂單取消時釋放庫存
            inventoryService.releaseReservedItems(event.orderId());
        } else if (event.newStatus() == OrderStatus.SHIPPED) {
            // 訂單出貨時確認庫存扣除
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

// 主題（發布者）
@Entity
public class Order {
    private String id;
    private OrderStatus status;
    
    @Transient
    private ApplicationEventPublisher eventPublisher;
    
    public void updateStatus(OrderStatus newStatus) {
        OrderStatus oldStatus = this.status;
        this.status = newStatus;
        
        // 發布事件通知所有觀察者
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

### 🙈 Tell, Don't Ask 原則

**目的**: 不要詢問對象的狀態然後基於狀態做決定，而是直接告訴對象該做什麼。

#### ✅ 良好實踐

```java
// ✅ 好的設計：Tell, Don't Ask
@Entity
public class BankAccount {
    private String accountNumber;
    private BigDecimal balance;
    private AccountStatus status;
    
    // 告訴對象執行操作，而不是詢問狀態
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
    
    // 內部驗證邏輯
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
    
    // 直接告訴對象執行操作
    public void transferMoney(String fromAccountId, String toAccountId, BigDecimal amount) {
        BankAccount fromAccount = accountRepository.findById(fromAccountId)
            .orElseThrow(() -> new AccountNotFoundException(fromAccountId));
        BankAccount toAccount = accountRepository.findById(toAccountId)
            .orElseThrow(() -> new AccountNotFoundException(toAccountId));
        
        // Tell, Don't Ask：直接執行操作
        fromAccount.withdraw(amount);
        toAccount.deposit(amount);
        
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
    }
}
```

#### ❌ 不良實踐

```java
// ❌ 壞的設計：Ask, Then Tell（詢問然後告訴）
@Entity
public class BankAccount {
    private String accountNumber;
    private BigDecimal balance;
    private AccountStatus status;
    
    // 暴露內部狀態供外部查詢
    public BigDecimal getBalance() { return balance; }
    public AccountStatus getStatus() { return status; }
    
    // 簡單的 setter，沒有業務邏輯
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public void setStatus(AccountStatus status) { this.status = status; }
}

@Service
public class BankingService {
    
    // 詢問對象狀態，然後基於狀態做決定
    public void transferMoney(String fromAccountId, String toAccountId, BigDecimal amount) {
        BankAccount fromAccount = accountRepository.findById(fromAccountId)
            .orElseThrow(() -> new AccountNotFoundException(fromAccountId));
        BankAccount toAccount = accountRepository.findById(toAccountId)
            .orElseThrow(() -> new AccountNotFoundException(toAccountId));
        
        // Ask, Then Tell：詢問狀態然後做決定
        if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException("From account is not active");
        }
        
        if (toAccount.getStatus() == AccountStatus.CLOSED) {
            throw new AccountClosedException("To account is closed");
        }
        
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }
        
        // 直接操作內部狀態
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));
        
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
    }
}
```

## 🎯 最佳實踐總結

### SOLID 原則應用指南

1. **SRP**: 每個類別只負責一個業務概念
2. **OCP**: 使用策略模式、工廠模式等支持擴展
3. **LSP**: 確保子類別行為與父類別一致
4. **ISP**: 創建小而專注的介面
5. **DIP**: 依賴抽象，使用依賴注入

### 設計模式選擇指南

1. **Factory**: 當需要創建複雜對象或支持多種類型時
2. **Builder**: 當對象有多個可選參數時
3. **Strategy**: 當有多種算法或業務規則時
4. **Observer**: 當需要解耦事件發布者和訂閱者時
5. **Tell, Don't Ask**: 始終優先考慮的設計原則

### 程式碼品質檢查清單

- [ ] 每個類別都遵循單一職責原則
- [ ] 使用介面而不是具體實現
- [ ] 避免過長的方法和類別
- [ ] 使用有意義的命名
- [ ] 適當使用設計模式
- [ ] 遵循 Tell, Don't Ask 原則
- [ ] 編寫單元測試驗證設計

## 🔗 相關資源

### 內部連結
- [編碼標準](coding-standards.md)
- [架構設計](architecture/)
- [測試策略](testing/)

### 外部資源
- [Clean Code by Robert C. Martin](https://www.amazon.com/Clean-Code-Handbook-Software-Craftsmanship/dp/0132350884)
- [Design Patterns by Gang of Four](https://www.amazon.com/Design-Patterns-Elements-Reusable-Object-Oriented/dp/0201633612)
- [SOLID Principles](https://en.wikipedia.org/wiki/SOLID)

---

**最後更新**: 2025年1月21日  
**維護者**: Development Team  
**版本**: 1.0  
**狀態**: Active