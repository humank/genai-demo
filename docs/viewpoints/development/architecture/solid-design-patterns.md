# SOLID 原則與設計模式

## 概述

本文檔提供完整的 SOLID 原則與設計模式指南，包含五大 SOLID 原則的詳細說明和常用設計模式的實作範例。

## 🎯 SOLID 原則

### 📏 單一職責原則 (SRP)

#### 定義
一個類別應該只有一個引起它變化的原因，即一個類別只應該有一個職責。

#### 實作範例

```java
// ❌ 違反 SRP：一個類別承擔多個職責
public class Customer {
    private String name;
    private String email;
    
    // 客戶資料管理職責
    public void updateEmail(String email) {
        this.email = email;
    }
    
    // 資料持久化職責 - 違反 SRP
    public void saveToDatabase() {
        // 資料庫保存邏輯
    }
    
    // 郵件發送職責 - 違反 SRP
    public void sendWelcomeEmail() {
        // 郵件發送邏輯
    }
    
    // 報告生成職責 - 違反 SRP
    public String generateReport() {
        return "Customer Report: " + name;
    }
}

// ✅ 遵循 SRP：職責分離
public class Customer {
    private final CustomerId id;
    private CustomerName name;
    private Email email;
    
    // 只負責客戶資料管理
    public Customer(CustomerId id, CustomerName name, Email email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }
    
    public void updateEmail(Email newEmail) {
        this.email = newEmail;
    }
    
    // Getters...
}

// 分離的職責類別
@Repository
public class CustomerRepository {
    public void save(Customer customer) {
        // 資料持久化邏輯
    }
}

@Service
public class CustomerEmailService {
    public void sendWelcomeEmail(Customer customer) {
        // 郵件發送邏輯
    }
}

@Service
public class CustomerReportService {
    public String generateReport(Customer customer) {
        return "Customer Report: " + customer.getName();
    }
}
```###
 🔓 開放封閉原則 (OCP)

#### 定義
軟體實體應該對擴展開放，對修改封閉。

#### 實作範例

```java
// ❌ 違反 OCP：每次新增折扣類型都需要修改現有程式碼
public class DiscountCalculator {
    
    public double calculateDiscount(Customer customer, double amount) {
        if (customer.getType() == CustomerType.REGULAR) {
            return amount * 0.05; // 5% 折扣
        } else if (customer.getType() == CustomerType.VIP) {
            return amount * 0.10; // 10% 折扣
        } else if (customer.getType() == CustomerType.PREMIUM) {
            return amount * 0.15; // 15% 折扣
        }
        // 每次新增客戶類型都需要修改這裡 - 違反 OCP
        return 0;
    }
}

// ✅ 遵循 OCP：使用策略模式，對擴展開放，對修改封閉
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

// 新增客戶類型時，只需要新增策略類別，不需要修改現有程式碼
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

### 🔄 里氏替換原則 (LSP)

#### 定義
子類別必須能夠替換其基類別，而不會改變程式的正確性。

#### 實作範例

```java
// ❌ 違反 LSP：子類別改變了基類別的行為契約
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
        // 違反 LSP：改變了基類別的行為契約
    }
}

// ✅ 遵循 LSP：重新設計類別層次結構
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

// 使用範例
public class BirdHandler {
    
    public void handleBird(Bird bird) {
        bird.move(); // 所有 Bird 子類別都能正確執行
    }
    
    public void handleFlyableBird(Flyable flyable) {
        flyable.fly(); // 只有會飛的鳥類才會實作這個介面
    }
}
```

### 🔌 介面隔離原則 (ISP)

#### 定義
客戶端不應該被迫依賴它們不使用的介面。

#### 實作範例

```java
// ❌ 違反 ISP：胖介面強迫客戶端依賴不需要的方法
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
        // 開發者可能不需要寫報告
        throw new UnsupportedOperationException("Developers don't write reports");
    }
}

// ✅ 遵循 ISP：將大介面拆分為小的、專門的介面
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
    // 不需要實作 Reportable，因為開發者不寫報告
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

### 🔄 依賴反轉原則 (DIP)

#### 定義
高層模組不應該依賴低層模組，兩者都應該依賴抽象。抽象不應該依賴細節，細節應該依賴抽象。

#### 實作範例

```java
// ❌ 違反 DIP：高層模組直接依賴低層模組
public class EmailService {
    public void sendEmail(String to, String subject, String body) {
        // 直接發送郵件的實作
        System.out.println("Sending email to: " + to);
    }
}

public class OrderService {
    private EmailService emailService; // 直接依賴具體實作
    
    public OrderService() {
        this.emailService = new EmailService(); // 緊耦合
    }
    
    public void processOrder(Order order) {
        // 處理訂單邏輯
        
        // 發送確認郵件
        emailService.sendEmail(
            order.getCustomerEmail(),
            "Order Confirmation",
            "Your order has been processed"
        );
    }
}

// ✅ 遵循 DIP：依賴抽象而非具體實作
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
    
    private final NotificationService notificationService; // 依賴抽象
    
    public OrderService(NotificationService notificationService) {
        this.notificationService = notificationService; // 依賴注入
    }
    
    public void processOrder(Order order) {
        // 處理訂單邏輯
        
        // 發送確認通知（可以是郵件或簡訊）
        notificationService.sendNotification(
            order.getCustomerContact(),
            "Order Confirmation",
            "Your order has been processed"
        );
    }
}

// Spring 配置
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
```

## 🎨 設計模式

### 🏭 Factory 模式

#### 工廠方法模式

```java
// 產品介面
public interface Customer {
    void register();
    CustomerType getType();
}

// 具體產品
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

// 抽象工廠
public abstract class CustomerFactory {
    
    public abstract Customer createCustomer(CustomerRegistrationData data);
    
    // 模板方法
    public Customer registerCustomer(CustomerRegistrationData data) {
        Customer customer = createCustomer(data);
        customer.register();
        return customer;
    }
}

// 具體工廠
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

// 工廠選擇器
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

### 🔨 Builder 模式

```java
// 複雜對象
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
    
    // 私有建構子，只能通過 Builder 創建
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
    
    // Builder 類別
    public static class Builder {
        // 必要參數
        private final String customerId;
        private final List<OrderItem> items = new ArrayList<>();
        
        // 可選參數 - 初始化為預設值
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

// 使用範例
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
```

### 📋 Strategy 模式

```java
// 策略介面
public interface PaymentStrategy {
    PaymentResult processPayment(PaymentRequest request);
    boolean supports(PaymentMethod method);
}

// 具體策略
@Component
public class CreditCardPaymentStrategy implements PaymentStrategy {
    
    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        // 信用卡支付邏輯
        System.out.println("Processing credit card payment: " + request.getAmount());
        
        // 模擬支付處理
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
        // 信用卡驗證邏輯
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
        // PayPal 支付邏輯
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
        // PayPal 帳戶驗證邏輯
        return payPalInfo != null && payPalInfo.getEmail().contains("@");
    }
    
    private String generateTransactionId() {
        return "PP-" + System.currentTimeMillis();
    }
}

// 上下文類別
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

### 👁️ Observer 模式

```java
// 事件介面
public interface DomainEvent {
    UUID getEventId();
    LocalDateTime getOccurredOn();
    String getEventType();
}

// 具體事件
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

// 觀察者介面
public interface DomainEventHandler<T extends DomainEvent> {
    void handle(T event);
    Class<T> getSupportedEventType();
}

// 具體觀察者
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

// 事件發布者
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

// 使用範例
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
        
        // 發布事件，通知所有觀察者
        OrderCreatedEvent event = OrderCreatedEvent.create(
            savedOrder.getOrderId(),
            savedOrder.getCustomerId(),
            savedOrder.getTotalAmount()
        );
        
        eventPublisher.publish(event);
        
        return savedOrder;
    }
}
```

### 🙈 Tell Don't Ask 原則

```java
// ❌ Ask：詢問對象狀態然後做決定
public class OrderProcessor {
    
    public void processOrder(Order order) {
        // 詢問訂單狀態
        if (order.getStatus() == OrderStatus.PENDING) {
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                if (order.getItems().size() > 0) {
                    // 外部邏輯決定如何處理
                    order.setStatus(OrderStatus.CONFIRMED);
                    order.setConfirmedAt(LocalDateTime.now());
                    
                    // 更多外部邏輯
                    for (OrderItem item : order.getItems()) {
                        if (item.getQuantity() > 0) {
                            // 處理每個項目
                        }
                    }
                }
            }
        }
    }
}

// ✅ Tell：告訴對象做什麼，讓對象自己決定如何做
public class Order {
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private List<OrderItem> items;
    private LocalDateTime confirmedAt;
    
    // Tell：告訴訂單確認自己
    public void confirm() {
        validateCanBeConfirmed();
        
        this.status = OrderStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        
        // 訂單自己知道如何處理確認邏輯
        notifyItemsOfConfirmation();
    }
    
    // Tell：告訴訂單處理付款
    public void markAsPaid() {
        validateCanBePaid();
        
        this.paymentStatus = PaymentStatus.PAID;
        
        // 如果滿足條件，自動確認訂單
        if (canBeAutoConfirmed()) {
            confirm();
        }
    }
    
    // 內部邏輯，外部不需要知道
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
        // Tell：直接告訴訂單做什麼
        order.confirm();
        
        // 簡潔明瞭，不需要知道內部邏輯
    }
    
    public void processPayment(Order order) {
        // Tell：告訴訂單標記為已付款
        order.markAsPaid();
        
        // 訂單會自動處理後續邏輯
    }
}
```

## 最佳實踐總結

### SOLID 原則應用

1. **SRP**: 每個類別只負責一個職責，職責變化時只影響一個類別
2. **OCP**: 使用抽象和多型實現擴展性，避免修改現有程式碼
3. **LSP**: 確保子類別能完全替換父類別，不破壞程式正確性
4. **ISP**: 設計小而專門的介面，避免強迫客戶端依賴不需要的方法
5. **DIP**: 依賴抽象而非具體實作，使用依賴注入實現鬆耦合

### 設計模式選擇

1. **Factory**: 當對象創建邏輯複雜或需要根據條件創建不同類型對象時
2. **Builder**: 當對象有多個可選參數或創建過程複雜時
3. **Strategy**: 當有多種算法或行為需要在運行時選擇時
4. **Observer**: 當對象狀態變化需要通知多個依賴對象時
5. **Tell Don't Ask**: 讓對象自己管理狀態和行為，提高封裝性

### 實作建議

1. **漸進式重構**: 不要一次性重構所有程式碼，逐步應用原則
2. **測試保護**: 在重構前確保有充分的測試覆蓋
3. **團隊共識**: 確保團隊對設計原則和模式有共同理解
4. **適度應用**: 不要過度設計，根據實際需求選擇合適的模式

---

**相關文檔**
- [DDD 領域驅動設計](ddd-domain-driven-design.md)
- [六角架構](hexagonal-architecture.md)
- 微服務架構