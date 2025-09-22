# SOLID 原則應用指南

## 概述

SOLID 原則是物件導向設計的五個基本原則，幫助開發者創建更易維護、擴展和理解的代碼。本指南詳細說明如何在專案中應用這些原則，並提供具體的實作範例。

## 🏗️ SOLID 原則概覽

### 五大原則

1. **S** - Single Responsibility Principle (單一職責原則)
2. **O** - Open/Closed Principle (開放封閉原則)
3. **L** - Liskov Substitution Principle (里氏替換原則)
4. **I** - Interface Segregation Principle (介面隔離原則)
5. **D** - Dependency Inversion Principle (依賴反轉原則)

## 1️⃣ 單一職責原則 (SRP)

### 原則定義

一個類別應該只有一個引起它變化的原因，即一個類別應該只負責一項職責。

### 實作範例

#### ❌ 違反 SRP 的設計

```java
// 違反 SRP：一個類別承擔了多個職責
public class Customer {
    private String id;
    private String name;
    private String email;
    
    // 職責1：客戶資料管理
    public void updateProfile(String name, String email) {
        this.name = name;
        this.email = email;
    }
    
    // 職責2：資料持久化
    public void save() {
        // 直接操作資料庫
        String sql = "UPDATE customers SET name = ?, email = ? WHERE id = ?";
        // JDBC 操作...
    }
    
    // 職責3：郵件發送
    public void sendWelcomeEmail() {
        // 直接發送郵件
        EmailService emailService = new EmailService();
        emailService.send(this.email, "Welcome!", "Welcome to our service!");
    }
    
    // 職責4：資料驗證
    public boolean isValidEmail() {
        return email != null && email.contains("@");
    }
}
```

#### ✅ 遵循 SRP 的設計

```java
// 職責1：客戶領域模型
@Entity
public class Customer {
    @Id
    private String id;
    private String name;
    private String email;
    private LocalDateTime createdAt;
    
    // 只負責客戶資料的業務邏輯
    public void updateProfile(String name, String email) {
        validateProfileUpdate(name, email);
        this.name = name;
        this.email = email;
    }
    
    private void validateProfileUpdate(String name, String email) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidCustomerDataException("Name cannot be empty");
        }
        if (!EmailValidator.isValid(email)) {
            throw new InvalidCustomerDataException("Invalid email format");
        }
    }
    
    // 領域事件收集
    public void collectEvent(DomainEvent event) {
        // 事件收集邏輯
    }
}

// 職責2：資料持久化
@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {
    Optional<Customer> findByEmail(String email);
    List<Customer> findByCreatedAtAfter(LocalDateTime date);
}

// 職責3：郵件服務
@Service
public class CustomerNotificationService {
    
    private final EmailService emailService;
    
    public CustomerNotificationService(EmailService emailService) {
        this.emailService = emailService;
    }
    
    public void sendWelcomeEmail(Customer customer) {
        EmailTemplate template = EmailTemplate.builder()
            .to(customer.getEmail())
            .subject("歡迎加入我們的服務")
            .template("welcome")
            .variable("customerName", customer.getName())
            .build();
            
        emailService.send(template);
    }
}

// 職責4：資料驗證
@Component
public class EmailValidator {
    
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    
    public static boolean isValid(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
}

// 職責5：應用服務協調
@Service
@Transactional
public class CustomerApplicationService {
    
    private final CustomerRepository customerRepository;
    private final CustomerNotificationService notificationService;
    private final DomainEventPublisher eventPublisher;
    
    public Customer createCustomer(CreateCustomerCommand command) {
        // 協調各個服務完成客戶創建
        Customer customer = new Customer(command.getName(), command.getEmail());
        Customer savedCustomer = customerRepository.save(customer);
        
        notificationService.sendWelcomeEmail(savedCustomer);
        eventPublisher.publish(new CustomerCreatedEvent(savedCustomer.getId()));
        
        return savedCustomer;
    }
}
```

## 2️⃣ 開放封閉原則 (OCP)

### 原則定義

軟體實體應該對擴展開放，對修改封閉。即應該通過擴展來實現變化，而不是通過修改現有代碼。

### 實作範例

#### ❌ 違反 OCP 的設計

```java
// 違反 OCP：每次新增折扣類型都需要修改現有代碼
public class DiscountCalculator {
    
    public BigDecimal calculateDiscount(Order order, String discountType) {
        switch (discountType) {
            case "PERCENTAGE":
                return order.getTotal().multiply(new BigDecimal("0.1"));
            case "FIXED_AMOUNT":
                return new BigDecimal("50");
            case "BUY_ONE_GET_ONE":
                // 新增這個類型需要修改這個方法
                return calculateBuyOneGetOneDiscount(order);
            default:
                return BigDecimal.ZERO;
        }
    }
}
```

#### ✅ 遵循 OCP 的設計

```java
// 抽象折扣策略
public interface DiscountStrategy {
    BigDecimal calculateDiscount(Order order);
    boolean isApplicable(Order order);
    String getDiscountType();
}

// 具體折扣策略實作
@Component
public class PercentageDiscountStrategy implements DiscountStrategy {
    
    private final BigDecimal discountRate;
    
    public PercentageDiscountStrategy(@Value("${discount.percentage.rate:0.1}") BigDecimal discountRate) {
        this.discountRate = discountRate;
    }
    
    @Override
    public BigDecimal calculateDiscount(Order order) {
        return order.getTotal().multiply(discountRate);
    }
    
    @Override
    public boolean isApplicable(Order order) {
        return order.getTotal().compareTo(new BigDecimal("100")) >= 0;
    }
    
    @Override
    public String getDiscountType() {
        return "PERCENTAGE";
    }
}

@Component
public class FixedAmountDiscountStrategy implements DiscountStrategy {
    
    private final BigDecimal fixedAmount;
    
    public FixedAmountDiscountStrategy(@Value("${discount.fixed.amount:50}") BigDecimal fixedAmount) {
        this.fixedAmount = fixedAmount;
    }
    
    @Override
    public BigDecimal calculateDiscount(Order order) {
        return fixedAmount;
    }
    
    @Override
    public boolean isApplicable(Order order) {
        return order.getTotal().compareTo(new BigDecimal("200")) >= 0;
    }
    
    @Override
    public String getDiscountType() {
        return "FIXED_AMOUNT";
    }
}

// 新增折扣策略不需要修改現有代碼
@Component
public class BuyOneGetOneDiscountStrategy implements DiscountStrategy {
    
    @Override
    public BigDecimal calculateDiscount(Order order) {
        return order.getItems().stream()
            .filter(item -> item.getQuantity() >= 2)
            .map(item -> item.getUnitPrice().multiply(
                new BigDecimal(item.getQuantity() / 2)))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    @Override
    public boolean isApplicable(Order order) {
        return order.getItems().stream()
            .anyMatch(item -> item.getQuantity() >= 2);
    }
    
    @Override
    public String getDiscountType() {
        return "BUY_ONE_GET_ONE";
    }
}

// 折扣計算器使用策略模式
@Service
public class DiscountCalculator {
    
    private final Map<String, DiscountStrategy> strategies;
    
    public DiscountCalculator(List<DiscountStrategy> strategyList) {
        this.strategies = strategyList.stream()
            .collect(Collectors.toMap(
                DiscountStrategy::getDiscountType,
                Function.identity()
            ));
    }
    
    public BigDecimal calculateDiscount(Order order, String discountType) {
        DiscountStrategy strategy = strategies.get(discountType);
        if (strategy != null && strategy.isApplicable(order)) {
            return strategy.calculateDiscount(order);
        }
        return BigDecimal.ZERO;
    }
    
    public List<String> getApplicableDiscounts(Order order) {
        return strategies.values().stream()
            .filter(strategy -> strategy.isApplicable(order))
            .map(DiscountStrategy::getDiscountType)
            .collect(Collectors.toList());
    }
}
```

## 3️⃣ 里氏替換原則 (LSP)

### 原則定義

子類別必須能夠替換其基類別，而不會改變程式的正確性。

### 實作範例

#### ❌ 違反 LSP 的設計

```java
// 違反 LSP：子類別改變了基類別的行為契約
public abstract class PaymentProcessor {
    
    public abstract PaymentResult processPayment(BigDecimal amount);
    
    // 基類別契約：所有付款處理器都應該支援退款
    public abstract RefundResult processRefund(String transactionId, BigDecimal amount);
}

public class CreditCardProcessor extends PaymentProcessor {
    
    @Override
    public PaymentResult processPayment(BigDecimal amount) {
        // 正常處理信用卡付款
        return new PaymentResult(true, "TXN-001");
    }
    
    @Override
    public RefundResult processRefund(String transactionId, BigDecimal amount) {
        // 正常處理退款
        return new RefundResult(true, "REF-001");
    }
}

public class GiftCardProcessor extends PaymentProcessor {
    
    @Override
    public PaymentResult processPayment(BigDecimal amount) {
        // 正常處理禮品卡付款
        return new PaymentResult(true, "GC-001");
    }
    
    @Override
    public RefundResult processRefund(String transactionId, BigDecimal amount) {
        // 違反 LSP：禮品卡不支援退款，但拋出異常改變了基類別的行為契約
        throw new UnsupportedOperationException("Gift cards do not support refunds");
    }
}
```

#### ✅ 遵循 LSP 的設計

```java
// 重新設計介面，遵循 LSP
public interface PaymentProcessor {
    PaymentResult processPayment(BigDecimal amount);
    boolean supportsRefund();
}

public interface RefundablePaymentProcessor extends PaymentProcessor {
    RefundResult processRefund(String transactionId, BigDecimal amount);
}

// 信用卡處理器支援退款
@Component
public class CreditCardProcessor implements RefundablePaymentProcessor {
    
    @Override
    public PaymentResult processPayment(BigDecimal amount) {
        // 處理信用卡付款
        return PaymentResult.success("CC-TXN-001");
    }
    
    @Override
    public boolean supportsRefund() {
        return true;
    }
    
    @Override
    public RefundResult processRefund(String transactionId, BigDecimal amount) {
        // 處理信用卡退款
        return RefundResult.success("CC-REF-001");
    }
}

// 禮品卡處理器不支援退款，但不違反契約
@Component
public class GiftCardProcessor implements PaymentProcessor {
    
    @Override
    public PaymentResult processPayment(BigDecimal amount) {
        // 處理禮品卡付款
        return PaymentResult.success("GC-TXN-001");
    }
    
    @Override
    public boolean supportsRefund() {
        return false; // 明確表示不支援退款
    }
}

// 付款服務正確使用 LSP
@Service
public class PaymentService {
    
    private final Map<PaymentMethod, PaymentProcessor> processors;
    
    public PaymentResult processPayment(PaymentMethod method, BigDecimal amount) {
        PaymentProcessor processor = processors.get(method);
        return processor.processPayment(amount);
    }
    
    public RefundResult processRefund(PaymentMethod method, String transactionId, BigDecimal amount) {
        PaymentProcessor processor = processors.get(method);
        
        if (processor instanceof RefundablePaymentProcessor refundableProcessor) {
            return refundableProcessor.processRefund(transactionId, amount);
        } else {
            return RefundResult.failure("Payment method does not support refunds");
        }
    }
    
    public boolean canRefund(PaymentMethod method) {
        PaymentProcessor processor = processors.get(method);
        return processor.supportsRefund();
    }
}
```

## 4️⃣ 介面隔離原則 (ISP)

### 原則定義

客戶端不應該被迫依賴它不使用的介面。應該將大的介面分解為更小、更具體的介面。

### 實作範例

#### ❌ 違反 ISP 的設計

```java
// 違反 ISP：大而全的介面強迫客戶端依賴不需要的方法
public interface OrderService {
    // 訂單管理
    Order createOrder(CreateOrderRequest request);
    Order updateOrder(String orderId, UpdateOrderRequest request);
    void cancelOrder(String orderId);
    
    // 訂單查詢
    Order findById(String orderId);
    List<Order> findByCustomerId(String customerId);
    Page<Order> findAll(Pageable pageable);
    
    // 訂單統計
    OrderStatistics getOrderStatistics(LocalDate from, LocalDate to);
    List<TopSellingProduct> getTopSellingProducts(int limit);
    
    // 訂單匯出
    byte[] exportOrdersToExcel(LocalDate from, LocalDate to);
    byte[] exportOrdersToPdf(String orderId);
    
    // 訂單通知
    void sendOrderConfirmation(String orderId);
    void sendShippingNotification(String orderId);
}

// 客戶端被迫依賴不需要的方法
@RestController
public class OrderController {
    
    private final OrderService orderService; // 只需要基本的 CRUD 操作
    
    @PostMapping("/orders")
    public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest request) {
        Order order = orderService.createOrder(request);
        return ResponseEntity.ok(order);
    }
    
    @GetMapping("/orders/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable String id) {
        Order order = orderService.findById(id);
        return ResponseEntity.ok(order);
    }
    
    // 這個控制器不需要統計、匯出、通知功能，但被迫依賴整個介面
}
```

#### ✅ 遵循 ISP 的設計

```java
// 將大介面分解為多個小介面
public interface OrderManagementService {
    Order createOrder(CreateOrderRequest request);
    Order updateOrder(String orderId, UpdateOrderRequest request);
    void cancelOrder(String orderId);
}

public interface OrderQueryService {
    Order findById(String orderId);
    List<Order> findByCustomerId(String customerId);
    Page<Order> findAll(Pageable pageable);
}

public interface OrderStatisticsService {
    OrderStatistics getOrderStatistics(LocalDate from, LocalDate to);
    List<TopSellingProduct> getTopSellingProducts(int limit);
}

public interface OrderExportService {
    byte[] exportOrdersToExcel(LocalDate from, LocalDate to);
    byte[] exportOrdersToPdf(String orderId);
}

public interface OrderNotificationService {
    void sendOrderConfirmation(String orderId);
    void sendShippingNotification(String orderId);
}

// 客戶端只依賴需要的介面
@RestController
public class OrderController {
    
    private final OrderManagementService orderManagementService;
    private final OrderQueryService orderQueryService;
    
    public OrderController(OrderManagementService orderManagementService,
                          OrderQueryService orderQueryService) {
        this.orderManagementService = orderManagementService;
        this.orderQueryService = orderQueryService;
    }
    
    @PostMapping("/orders")
    public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest request) {
        Order order = orderManagementService.createOrder(request);
        return ResponseEntity.ok(order);
    }
    
    @GetMapping("/orders/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable String id) {
        Order order = orderQueryService.findById(id);
        return ResponseEntity.ok(order);
    }
}

@RestController
public class OrderReportController {
    
    private final OrderStatisticsService statisticsService;
    private final OrderExportService exportService;
    
    // 只依賴統計和匯出相關的介面
    
    @GetMapping("/orders/statistics")
    public ResponseEntity<OrderStatistics> getStatistics(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        OrderStatistics stats = statisticsService.getOrderStatistics(from, to);
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/orders/export/excel")
    public ResponseEntity<byte[]> exportToExcel(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        byte[] excel = exportService.exportOrdersToExcel(from, to);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=orders.xlsx")
            .body(excel);
    }
}

// 實作類別可以實作多個介面
@Service
@Transactional
public class OrderServiceImpl implements OrderManagementService, OrderQueryService {
    
    private final OrderRepository orderRepository;
    
    @Override
    public Order createOrder(CreateOrderRequest request) {
        // 實作訂單創建邏輯
        return new Order();
    }
    
    @Override
    public Order findById(String orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
    
    // 其他方法實作...
}
```

## 5️⃣ 依賴反轉原則 (DIP)

### 原則定義

高層模組不應該依賴低層模組，兩者都應該依賴抽象。抽象不應該依賴細節，細節應該依賴抽象。

### 實作範例

#### ❌ 違反 DIP 的設計

```java
// 違反 DIP：高層模組直接依賴低層模組的具體實作
@Service
public class OrderService {
    
    // 直接依賴具體的實作類別
    private final MySQLOrderRepository orderRepository;
    private final SMTPEmailService emailService;
    private final StripePaymentGateway paymentGateway;
    
    public OrderService() {
        // 在構造函數中直接創建依賴
        this.orderRepository = new MySQLOrderRepository();
        this.emailService = new SMTPEmailService();
        this.paymentGateway = new StripePaymentGateway();
    }
    
    public Order processOrder(CreateOrderRequest request) {
        // 業務邏輯與具體實作緊密耦合
        Order order = new Order(request);
        orderRepository.save(order);
        
        PaymentResult result = paymentGateway.processPayment(order.getTotal());
        if (result.isSuccess()) {
            emailService.sendConfirmationEmail(order.getCustomerEmail());
        }
        
        return order;
    }
}

// 具體實作類別
public class MySQLOrderRepository {
    public void save(Order order) {
        // MySQL 特定的實作
    }
}

public class SMTPEmailService {
    public void sendConfirmationEmail(String email) {
        // SMTP 特定的實作
    }
}

public class StripePaymentGateway {
    public PaymentResult processPayment(BigDecimal amount) {
        // Stripe 特定的實作
        return new PaymentResult();
    }
}
```

#### ✅ 遵循 DIP 的設計

```java
// 定義抽象介面
public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(String orderId);
    List<Order> findByCustomerId(String customerId);
}

public interface EmailService {
    void sendConfirmationEmail(String email, String orderNumber);
    void sendShippingNotification(String email, String trackingNumber);
}

public interface PaymentGateway {
    PaymentResult processPayment(BigDecimal amount, PaymentInfo paymentInfo);
    RefundResult processRefund(String transactionId, BigDecimal amount);
}

// 高層模組依賴抽象
@Service
@Transactional
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final EmailService emailService;
    private final PaymentGateway paymentGateway;
    private final DomainEventPublisher eventPublisher;
    
    // 通過構造函數注入依賴抽象
    public OrderService(OrderRepository orderRepository,
                       EmailService emailService,
                       PaymentGateway paymentGateway,
                       DomainEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.emailService = emailService;
        this.paymentGateway = paymentGateway;
        this.eventPublisher = eventPublisher;
    }
    
    public Order processOrder(CreateOrderRequest request) {
        // 業務邏輯與具體實作解耦
        Order order = Order.create(request);
        Order savedOrder = orderRepository.save(order);
        
        PaymentResult result = paymentGateway.processPayment(
            order.getTotal(), 
            request.getPaymentInfo()
        );
        
        if (result.isSuccess()) {
            order.markAsPaid(result.getTransactionId());
            emailService.sendConfirmationEmail(
                order.getCustomerEmail(), 
                order.getOrderNumber()
            );
            
            eventPublisher.publish(new OrderProcessedEvent(order.getId()));
        }
        
        return savedOrder;
    }
}

// 具體實作類別實作抽象介面
@Repository
public class JpaOrderRepository implements OrderRepository {
    
    private final OrderJpaRepository jpaRepository;
    
    public JpaOrderRepository(OrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public Order save(Order order) {
        OrderEntity entity = OrderEntity.from(order);
        OrderEntity savedEntity = jpaRepository.save(entity);
        return savedEntity.toDomain();
    }
    
    @Override
    public Optional<Order> findById(String orderId) {
        return jpaRepository.findById(orderId)
            .map(OrderEntity::toDomain);
    }
    
    @Override
    public List<Order> findByCustomerId(String customerId) {
        return jpaRepository.findByCustomerId(customerId)
            .stream()
            .map(OrderEntity::toDomain)
            .collect(Collectors.toList());
    }
}

@Service
public class AsyncEmailService implements EmailService {
    
    private final JavaMailSender mailSender;
    private final EmailTemplateService templateService;
    
    @Async
    @Override
    public void sendConfirmationEmail(String email, String orderNumber) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setTo(email);
            helper.setSubject("訂單確認 - " + orderNumber);
            helper.setText(templateService.generateConfirmationEmail(orderNumber), true);
            
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send confirmation email", e);
        }
    }
    
    @Async
    @Override
    public void sendShippingNotification(String email, String trackingNumber) {
        // 實作配送通知邏輯
    }
}

@Service
public class StripePaymentGateway implements PaymentGateway {
    
    private final StripeClient stripeClient;
    
    @Override
    public PaymentResult processPayment(BigDecimal amount, PaymentInfo paymentInfo) {
        try {
            // Stripe 特定的付款處理邏輯
            PaymentIntent intent = stripeClient.createPaymentIntent(
                amount.multiply(new BigDecimal("100")).longValue(), // 轉換為分
                paymentInfo.getCurrency(),
                paymentInfo.getPaymentMethodId()
            );
            
            return PaymentResult.success(intent.getId());
        } catch (StripeException e) {
            return PaymentResult.failure(e.getMessage());
        }
    }
    
    @Override
    public RefundResult processRefund(String transactionId, BigDecimal amount) {
        // 實作退款邏輯
        return RefundResult.success("refund_id");
    }
}

// 配置類別負責依賴注入的配置
@Configuration
public class ServiceConfiguration {
    
    @Bean
    @ConditionalOnProperty(name = "payment.provider", havingValue = "stripe")
    public PaymentGateway stripePaymentGateway(StripeClient stripeClient) {
        return new StripePaymentGateway(stripeClient);
    }
    
    @Bean
    @ConditionalOnProperty(name = "payment.provider", havingValue = "paypal")
    public PaymentGateway paypalPaymentGateway(PayPalClient paypalClient) {
        return new PayPalPaymentGateway(paypalClient);
    }
    
    @Bean
    @ConditionalOnProperty(name = "email.provider", havingValue = "smtp")
    public EmailService smtpEmailService(JavaMailSender mailSender) {
        return new AsyncEmailService(mailSender);
    }
    
    @Bean
    @ConditionalOnProperty(name = "email.provider", havingValue = "ses")
    public EmailService sesEmailService(AmazonSimpleEmailService sesClient) {
        return new SESEmailService(sesClient);
    }
}
```

## 🎯 設計模式應用

### Factory Pattern

```java
// 抽象工廠介面
public interface PaymentProcessorFactory {
    PaymentProcessor createProcessor(PaymentMethod method);
}

// 具體工廠實作
@Component
public class PaymentProcessorFactoryImpl implements PaymentProcessorFactory {
    
    private final Map<PaymentMethod, Supplier<PaymentProcessor>> processorSuppliers;
    
    public PaymentProcessorFactoryImpl(ApplicationContext context) {
        this.processorSuppliers = Map.of(
            PaymentMethod.CREDIT_CARD, () -> context.getBean(CreditCardProcessor.class),
            PaymentMethod.PAYPAL, () -> context.getBean(PayPalProcessor.class),
            PaymentMethod.BANK_TRANSFER, () -> context.getBean(BankTransferProcessor.class)
        );
    }
    
    @Override
    public PaymentProcessor createProcessor(PaymentMethod method) {
        Supplier<PaymentProcessor> supplier = processorSuppliers.get(method);
        if (supplier == null) {
            throw new UnsupportedPaymentMethodException("Unsupported payment method: " + method);
        }
        return supplier.get();
    }
}

// 使用工廠模式
@Service
public class PaymentService {
    
    private final PaymentProcessorFactory processorFactory;
    
    public PaymentResult processPayment(PaymentRequest request) {
        PaymentProcessor processor = processorFactory.createProcessor(request.getMethod());
        return processor.processPayment(request.getAmount(), request.getPaymentInfo());
    }
}
```

### Builder Pattern

```java
// 使用 Builder 模式構建複雜物件
public class Order {
    private final String id;
    private final String customerId;
    private final List<OrderItem> items;
    private final ShippingAddress shippingAddress;
    private final PaymentInfo paymentInfo;
    private final OrderStatus status;
    private final LocalDateTime createdAt;
    
    private Order(Builder builder) {
        this.id = builder.id;
        this.customerId = builder.customerId;
        this.items = List.copyOf(builder.items);
        this.shippingAddress = builder.shippingAddress;
        this.paymentInfo = builder.paymentInfo;
        this.status = builder.status;
        this.createdAt = builder.createdAt;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String id;
        private String customerId;
        private List<OrderItem> items = new ArrayList<>();
        private ShippingAddress shippingAddress;
        private PaymentInfo paymentInfo;
        private OrderStatus status = OrderStatus.PENDING;
        private LocalDateTime createdAt = LocalDateTime.now();
        
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
        
        public Builder shippingAddress(ShippingAddress address) {
            this.shippingAddress = address;
            return this;
        }
        
        public Builder paymentInfo(PaymentInfo paymentInfo) {
            this.paymentInfo = paymentInfo;
            return this;
        }
        
        public Builder status(OrderStatus status) {
            this.status = status;
            return this;
        }
        
        public Order build() {
            validate();
            return new Order(this);
        }
        
        private void validate() {
            if (customerId == null || customerId.trim().isEmpty()) {
                throw new IllegalArgumentException("Customer ID is required");
            }
            if (items.isEmpty()) {
                throw new IllegalArgumentException("Order must contain at least one item");
            }
            if (shippingAddress == null) {
                throw new IllegalArgumentException("Shipping address is required");
            }
        }
    }
}

// 使用 Builder 模式
@Service
public class OrderFactory {
    
    public Order createOrder(CreateOrderRequest request) {
        return Order.builder()
            .id(generateOrderId())
            .customerId(request.getCustomerId())
            .items(request.getItems())
            .shippingAddress(request.getShippingAddress())
            .paymentInfo(request.getPaymentInfo())
            .build();
    }
}
```

### Strategy Pattern

```java
// 策略介面
public interface PricingStrategy {
    BigDecimal calculatePrice(Product product, int quantity);
    boolean isApplicable(Customer customer, Product product);
}

// 具體策略實作
@Component
public class RegularPricingStrategy implements PricingStrategy {
    
    @Override
    public BigDecimal calculatePrice(Product product, int quantity) {
        return product.getPrice().multiply(new BigDecimal(quantity));
    }
    
    @Override
    public boolean isApplicable(Customer customer, Product product) {
        return true; // 預設策略，總是適用
    }
}

@Component
public class VipPricingStrategy implements PricingStrategy {
    
    private static final BigDecimal VIP_DISCOUNT = new BigDecimal("0.9");
    
    @Override
    public BigDecimal calculatePrice(Product product, int quantity) {
        BigDecimal regularPrice = product.getPrice().multiply(new BigDecimal(quantity));
        return regularPrice.multiply(VIP_DISCOUNT);
    }
    
    @Override
    public boolean isApplicable(Customer customer, Product product) {
        return customer.getMembershipLevel() == MembershipLevel.VIP;
    }
}

@Component
public class BulkPricingStrategy implements PricingStrategy {
    
    private static final int BULK_THRESHOLD = 10;
    private static final BigDecimal BULK_DISCOUNT = new BigDecimal("0.85");
    
    @Override
    public BigDecimal calculatePrice(Product product, int quantity) {
        BigDecimal regularPrice = product.getPrice().multiply(new BigDecimal(quantity));
        return quantity >= BULK_THRESHOLD ? regularPrice.multiply(BULK_DISCOUNT) : regularPrice;
    }
    
    @Override
    public boolean isApplicable(Customer customer, Product product) {
        return true; // 適用於所有客戶
    }
}

// 策略上下文
@Service
public class PricingService {
    
    private final List<PricingStrategy> strategies;
    
    public PricingService(List<PricingStrategy> strategies) {
        // Spring 會自動注入所有 PricingStrategy 實作
        this.strategies = strategies;
    }
    
    public BigDecimal calculatePrice(Customer customer, Product product, int quantity) {
        return strategies.stream()
            .filter(strategy -> strategy.isApplicable(customer, product))
            .map(strategy -> strategy.calculatePrice(product, quantity))
            .min(BigDecimal::compareTo) // 選擇最低價格
            .orElse(product.getPrice().multiply(new BigDecimal(quantity)));
    }
}
```

### Observer Pattern

```java
// 觀察者介面
public interface OrderEventListener {
    void onOrderCreated(OrderCreatedEvent event);
    void onOrderPaid(OrderPaidEvent event);
    void onOrderShipped(OrderShippedEvent event);
}

// 具體觀察者實作
@Component
public class InventoryUpdateListener implements OrderEventListener {
    
    private final InventoryService inventoryService;
    
    @Override
    public void onOrderCreated(OrderCreatedEvent event) {
        // 預留庫存
        inventoryService.reserveItems(event.getOrderItems());
    }
    
    @Override
    public void onOrderPaid(OrderPaidEvent event) {
        // 確認庫存分配
        inventoryService.confirmReservation(event.getOrderId());
    }
    
    @Override
    public void onOrderShipped(OrderShippedEvent event) {
        // 更新庫存數量
        inventoryService.updateStock(event.getOrderItems());
    }
}

@Component
public class NotificationListener implements OrderEventListener {
    
    private final EmailService emailService;
    private final SmsService smsService;
    
    @Override
    public void onOrderCreated(OrderCreatedEvent event) {
        emailService.sendOrderConfirmation(event.getCustomerEmail(), event.getOrderNumber());
    }
    
    @Override
    public void onOrderPaid(OrderPaidEvent event) {
        emailService.sendPaymentConfirmation(event.getCustomerEmail(), event.getOrderNumber());
    }
    
    @Override
    public void onOrderShipped(OrderShippedEvent event) {
        smsService.sendShippingNotification(event.getCustomerPhone(), event.getTrackingNumber());
    }
}

// 事件發布者
@Service
public class OrderEventPublisher {
    
    private final List<OrderEventListener> listeners;
    
    public OrderEventPublisher(List<OrderEventListener> listeners) {
        this.listeners = listeners;
    }
    
    public void publishOrderCreated(Order order) {
        OrderCreatedEvent event = new OrderCreatedEvent(order);
        listeners.forEach(listener -> {
            try {
                listener.onOrderCreated(event);
            } catch (Exception e) {
                log.error("Error processing order created event", e);
            }
        });
    }
    
    public void publishOrderPaid(Order order, String transactionId) {
        OrderPaidEvent event = new OrderPaidEvent(order, transactionId);
        listeners.forEach(listener -> {
            try {
                listener.onOrderPaid(event);
            } catch (Exception e) {
                log.error("Error processing order paid event", e);
            }
        });
    }
}
```

## 📏 Show Don't Ask 原則

### 原則定義

不要詢問物件的狀態然後根據狀態執行動作，而是直接告訴物件執行動作。

### 實作範例

#### ❌ 違反 Show Don't Ask 的設計

```java
// 違反原則：詢問物件狀態然後執行動作
public class OrderProcessor {
    
    public void processOrder(Order order) {
        // Ask：詢問訂單狀態
        if (order.getStatus() == OrderStatus.PENDING) {
            if (order.getPaymentStatus() == PaymentStatus.UNPAID) {
                // 外部邏輯決定如何處理
                order.setStatus(OrderStatus.AWAITING_PAYMENT);
                order.setLastUpdated(LocalDateTime.now());
            } else if (order.getPaymentStatus() == PaymentStatus.PAID) {
                if (order.hasInStockItems()) {
                    order.setStatus(OrderStatus.PROCESSING);
                    order.setLastUpdated(LocalDateTime.now());
                } else {
                    order.setStatus(OrderStatus.BACKORDERED);
                    order.setLastUpdated(LocalDateTime.now());
                }
            }
        }
    }
    
    public void cancelOrder(Order order) {
        // Ask：詢問是否可以取消
        if (order.getStatus() == OrderStatus.PENDING || 
            order.getStatus() == OrderStatus.AWAITING_PAYMENT) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setCancelledAt(LocalDateTime.now());
            order.setLastUpdated(LocalDateTime.now());
        } else {
            throw new IllegalStateException("Cannot cancel order in status: " + order.getStatus());
        }
    }
}
```

#### ✅ 遵循 Show Don't Ask 的設計

```java
// 遵循原則：告訴物件執行動作，讓物件自己決定如何處理
public class Order {
    private String id;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private List<OrderItem> items;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
    private LocalDateTime cancelledAt;
    
    // Don't Ask, Show：直接告訴訂單處理自己
    public void process() {
        switch (status) {
            case PENDING -> processPendingOrder();
            case AWAITING_PAYMENT -> processAwaitingPaymentOrder();
            default -> throw new IllegalStateException("Cannot process order in status: " + status);
        }
    }
    
    private void processPendingOrder() {
        if (paymentStatus == PaymentStatus.UNPAID) {
            transitionTo(OrderStatus.AWAITING_PAYMENT);
        } else if (paymentStatus == PaymentStatus.PAID) {
            if (hasInStockItems()) {
                transitionTo(OrderStatus.PROCESSING);
            } else {
                transitionTo(OrderStatus.BACKORDERED);
            }
        }
    }
    
    private void processAwaitingPaymentOrder() {
        if (paymentStatus == PaymentStatus.PAID) {
            transitionTo(OrderStatus.PROCESSING);
        }
    }
    
    // Don't Ask, Show：直接告訴訂單取消自己
    public void cancel() {
        if (!canBeCancelled()) {
            throw new IllegalStateException("Cannot cancel order in status: " + status);
        }
        
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
        
        // 收集領域事件
        collectEvent(new OrderCancelledEvent(this.id, this.cancelledAt));
    }
    
    // Don't Ask, Show：直接告訴訂單標記為已付款
    public void markAsPaid(String transactionId) {
        if (paymentStatus == PaymentStatus.PAID) {
            throw new IllegalStateException("Order is already paid");
        }
        
        this.paymentStatus = PaymentStatus.PAID;
        this.lastUpdated = LocalDateTime.now();
        
        // 付款後自動處理訂單
        if (status == OrderStatus.AWAITING_PAYMENT) {
            process();
        }
        
        collectEvent(new OrderPaidEvent(this.id, transactionId));
    }
    
    // Don't Ask, Show：直接告訴訂單發貨
    public void ship(String trackingNumber) {
        if (!canBeShipped()) {
            throw new IllegalStateException("Cannot ship order in status: " + status);
        }
        
        transitionTo(OrderStatus.SHIPPED);
        collectEvent(new OrderShippedEvent(this.id, trackingNumber));
    }
    
    // 內部方法封裝業務規則
    private boolean canBeCancelled() {
        return status == OrderStatus.PENDING || 
               status == OrderStatus.AWAITING_PAYMENT ||
               status == OrderStatus.PROCESSING;
    }
    
    private boolean canBeShipped() {
        return status == OrderStatus.PROCESSING;
    }
    
    private boolean hasInStockItems() {
        return items.stream().allMatch(OrderItem::isInStock);
    }
    
    private void transitionTo(OrderStatus newStatus) {
        OrderStatus oldStatus = this.status;
        this.status = newStatus;
        this.lastUpdated = LocalDateTime.now();
        
        collectEvent(new OrderStatusChangedEvent(this.id, oldStatus, newStatus));
    }
}

// 簡化的處理器
public class OrderProcessor {
    
    public void processOrder(Order order) {
        // Show：直接告訴訂單處理自己
        order.process();
    }
    
    public void cancelOrder(Order order) {
        // Show：直接告訴訂單取消自己
        order.cancel();
    }
    
    public void markOrderAsPaid(Order order, String transactionId) {
        // Show：直接告訴訂單標記為已付款
        order.markAsPaid(transactionId);
    }
}
```

## 🔗 相關資源

### 內部文檔
- [技術棧整合指南](../tools-and-environment/technology-stack/README.md)
- [DDD 戰術模式](ddd-patterns/tactical-patterns.md)
- [架構決策記錄](../architecture-decisions/)

### 外部資源
- [Clean Code by Robert C. Martin](https://www.amazon.com/Clean-Code-Handbook-Software-Craftsmanship/dp/0132350884)
- [SOLID Principles Explained](https://www.digitalocean.com/community/conceptual_articles/s-o-l-i-d-the-first-five-principles-of-object-oriented-design)
- [Design Patterns: Elements of Reusable Object-Oriented Software](https://www.amazon.com/Design-Patterns-Elements-Reusable-Object-Oriented/dp/0201633612)

---

**最後更新**: 2025年1月21日  
**維護者**: Architecture Team  
**版本**: 1.0

> 💡 **提示**: SOLID 原則是編寫高品質代碼的基礎。在實際開發中，要平衡原則的應用與實用性，避免過度設計。重點是讓代碼更易於理解、測試和維護。