# Data Model Design

## Automated Data Discovery and Governance

### AWS Glue Data Catalog Integration

The GenAI Demo application adopts AWS Glue Data Catalog for automated schema discovery and data governance, ensuring data model consistency and traceability across 13 bounded contexts.

#### Automated Discovery Mechanism
- **Daily Scheduled Scanning**: Automatically scans Aurora database at 2 AM daily
- **Real-time Change Detection**: RDS events trigger immediate schema discovery
- **Intelligent Exclusion**: Automatically excludes system tables and management tables
- **Cross-Region Consistency**: Supports multi-region deployment of Aurora Global Database

#### Data Catalog Structure
```
genai_demo_catalog/
├── customer_tables/     # Customer bounded context
├── order_tables/        # Order bounded context  
├── product_tables/      # Product bounded context
├── inventory_tables/    # Inventory bounded context
├── payment_tables/      # Payment bounded context
├── delivery_tables/     # Delivery bounded context
├── shoppingcart_tables/ # Shopping cart bounded context
├── pricing_tables/      # Pricing bounded context
├── promotion_tables/    # Promotion bounded context
├── seller_tables/       # Seller bounded context
├── review_tables/       # Review bounded context
├── notification_tables/ # Notification bounded context
└── observability_tables/# Observability bounded context
```

For detailed data governance architecture, please refer to [Data Governance Architecture](data-governance-architecture.md).

## Domain Model

### Aggregate Root Design
- Customer Aggregate
- Order Aggregate
- Product Aggregate

### Value Object Design
```java
public record CustomerId(String value) {
    public CustomerId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Customer ID cannot be null or blank");
        }
    }
}
```

## Data Persistence

### JPA Entity Mapping
```java
@Entity
@Table(name = "customers")
public class Customer {
    @Id
    private String id;
    
    @Embedded
    private CustomerName name;
    
    @Embedded
    private Email email;
}
```

### Database Design
- Normalization design principles
- Index strategy planning
- Data integrity constraints

## Data Migration

### Flyway Scripts
```sql
-- V1__Create_customer_table.sql
CREATE TABLE customers (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```ite
ms); }
    public Money getTotalAmount() { return totalAmount; }
    public ShippingAddress getShippingAddress() { return shippingAddress; }
    public PaymentMethodId getPaymentMethodId() { return paymentMethodId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
```

#### Order Value Objects

```java
@ValueObject
public record OrderId(String value) {
    
    public OrderId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }
        if (!value.matches("^ORD-[0-9]{8}$")) {
            throw new IllegalArgumentException("Invalid order ID format: " + value);
        }
    }
    
    public static OrderId of(String value) {
        return new OrderId(value);
    }
    
    public static OrderId generate() {
        return new OrderId("ORD-" + String.format("%08d", 
            ThreadLocalRandom.current().nextInt(1, 100000000)));
    }
}

@ValueObject
public record Money(BigDecimal amount, Currency currency) {
    
    public static final Money ZERO = new Money(BigDecimal.ZERO, Currency.getInstance("USD"));
    
    public Money {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
        if (amount.scale() > currency.getDefaultFractionDigits()) {
            throw new IllegalArgumentException("Amount scale exceeds currency precision");
        }
    }
    
    public static Money of(BigDecimal amount) {
        return new Money(amount, Currency.getInstance("USD"));
    }
    
    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, Currency.getInstance(currencyCode));
    }
    
    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }
    
    public Money subtract(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }
    
    public Money multiply(BigDecimal multiplier) {
        return new Money(this.amount.multiply(multiplier), this.currency);
    }
    
    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }
    
    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    private void validateSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                "Cannot perform operation on different currencies: " + 
                this.currency + " and " + other.currency
            );
        }
    }
}

public class OrderItem {
    
    private final ProductId productId;
    private int quantity;
    private final Money unitPrice;
    
    public OrderItem(ProductId productId, int quantity, Money unitPrice) {
        this.productId = Objects.requireNonNull(productId);
        this.unitPrice = Objects.requireNonNull(unitPrice);
        
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (unitPrice.isNegative()) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }
        
        this.quantity = quantity;
    }
    
    public void updateQuantity(int newQuantity) {
        if (newQuantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.quantity = newQuantity;
    }
    
    public Money getTotalPrice() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
    
    // Getters
    public ProductId getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public Money getUnitPrice() { return unitPrice; }
}

@ValueObject
public record ShippingAddress(
    String street,
    String city,
    String state,
    String postalCode,
    String country
) {
    
    public ShippingAddress {
        if (street == null || street.trim().isEmpty()) {
            throw new IllegalArgumentException("Street cannot be null or empty");
        }
        if (city == null || city.trim().isEmpty()) {
            throw new IllegalArgumentException("City cannot be null or empty");
        }
        if (state == null || state.trim().isEmpty()) {
            throw new IllegalArgumentException("State cannot be null or empty");
        }
        if (postalCode == null || postalCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Postal code cannot be null or empty");
        }
        if (country == null || country.trim().isEmpty()) {
            throw new IllegalArgumentException("Country cannot be null or empty");
        }
    }
    
    public String getFullAddress() {
        return String.join(", ", street, city, state, postalCode, country);
    }
}
```

### 3. Product Domain

#### Product Aggregate Root

```java
@AggregateRoot(name = "Product", boundedContext = "Product", version = "2.0")
public class Product implements AggregateRootInterface {
    
    private final ProductId productId;
    private ProductName productName;
    private ProductDescription description;
    private Money price;
    private ProductCategory category;
    private InventoryLevel inventoryLevel;
    private ProductStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructor
    public Product(
        ProductId productId,
        ProductName productName,
        ProductDescription description,
        Money price,
        ProductCategory category
    ) {
        this.productId = Objects.requireNonNull(productId);
        this.productName = Objects.requireNonNull(productName);
        this.description = Objects.requireNonNull(description);
        this.price = Objects.requireNonNull(price);
        this.category = Objects.requireNonNull(category);
        this.inventoryLevel = InventoryLevel.zero();
        this.status = ProductStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        collectEvent(ProductCreatedEvent.create(
            productId, productName, description, price, category
        ));
    }
    
    // Business methods
    public void updatePrice(Money newPrice) {
        validatePriceUpdate(newPrice);
        
        Money oldPrice = this.price;
        this.price = newPrice;
        this.updatedAt = LocalDateTime.now();
        
        collectEvent(ProductPriceUpdatedEvent.create(
            productId, oldPrice, newPrice
        ));
    }
    
    public void updateInventory(int quantity, InventoryOperation operation) {
        validateInventoryOperation(quantity, operation);
        
        int oldLevel = this.inventoryLevel.getQuantity();
        
        switch (operation) {
            case ADD -> this.inventoryLevel = this.inventoryLevel.add(quantity);
            case SUBTRACT -> this.inventoryLevel = this.inventoryLevel.subtract(quantity);
            case SET -> this.inventoryLevel = InventoryLevel.of(quantity);
        }
        
        this.updatedAt = LocalDateTime.now();
        
        collectEvent(ProductInventoryUpdatedEvent.create(
            productId, oldLevel, this.inventoryLevel.getQuantity(), operation
        ));
        
        // Check for low inventory
        if (this.inventoryLevel.isLow()) {
            collectEvent(ProductLowInventoryEvent.create(
                productId, this.inventoryLevel.getQuantity()
            ));
        }
    }
    
    public void changeStatus(ProductStatus newStatus) {
        if (this.status == newStatus) {
            return;
        }
        
        ProductStatus oldStatus = this.status;
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
        
        collectEvent(ProductStatusChangedEvent.create(
            productId, oldStatus, newStatus
        ));
    }
    
    public boolean isAvailable() {
        return status == ProductStatus.ACTIVE && inventoryLevel.isAvailable();
    }
    
    public boolean canFulfillOrder(int requestedQuantity) {
        return isAvailable() && inventoryLevel.canFulfill(requestedQuantity);
    }
    
    // Validation methods
    private void validatePriceUpdate(Money newPrice) {
        if (newPrice == null || newPrice.isNegative()) {
            throw new IllegalArgumentException("Price must be positive");
        }
    }
    
    private void validateInventoryOperation(int quantity, InventoryOperation operation) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        
        if (operation == InventoryOperation.SUBTRACT && 
            quantity > this.inventoryLevel.getQuantity()) {
            throw new IllegalStateException(
                "Cannot subtract more inventory than available. " +
                "Available: " + this.inventoryLevel.getQuantity() + 
                ", Requested: " + quantity
            );
        }
    }
    
    // Getters
    public ProductId getProductId() { return productId; }
    public ProductName getProductName() { return productName; }
    public ProductDescription getDescription() { return description; }
    public Money getPrice() { return price; }
    public ProductCategory getCategory() { return category; }
    public InventoryLevel getInventoryLevel() { return inventoryLevel; }
    public ProductStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
```

#### Product Value Objects

```java
@ValueObject
public record ProductId(String value) {
    
    public ProductId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be null or empty");
        }
        if (!value.matches("^PROD-[A-Z0-9]{6}$")) {
            throw new IllegalArgumentException("Invalid product ID format: " + value);
        }
    }
    
    public static ProductId of(String value) {
        return new ProductId(value);
    }
    
    public static ProductId generate() {
        String randomPart = ThreadLocalRandom.current()
            .ints(6, 0, 36)
            .mapToObj(i -> i < 10 ? String.valueOf(i) : String.valueOf((char)('A' + i - 10)))
            .collect(Collectors.joining());
        return new ProductId("PROD-" + randomPart);
    }
}

@ValueObject
public record ProductName(String value) {
    
    public ProductName {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be null or empty");
        }
        if (value.length() > 100) {
            throw new IllegalArgumentException("Product name cannot exceed 100 characters");
        }
    }
    
    public static ProductName of(String value) {
        return new ProductName(value.trim());
    }
}

@ValueObject
public record ProductDescription(String value) {
    
    public ProductDescription {
        if (value == null) {
            value = "";
        }
        if (value.length() > 1000) {
            throw new IllegalArgumentException("Product description cannot exceed 1000 characters");
        }
    }
    
    public static ProductDescription of(String value) {
        return new ProductDescription(value != null ? value.trim() : "");
    }
    
    public static ProductDescription empty() {
        return new ProductDescription("");
    }
}

@ValueObject
public record InventoryLevel(int quantity) {
    
    private static final int LOW_INVENTORY_THRESHOLD = 10;
    
    public InventoryLevel {
        if (quantity < 0) {
            throw new IllegalArgumentException("Inventory quantity cannot be negative");
        }
    }
    
    public static InventoryLevel zero() {
        return new InventoryLevel(0);
    }
    
    public static InventoryLevel of(int quantity) {
        return new InventoryLevel(quantity);
    }
    
    public InventoryLevel add(int amount) {
        return new InventoryLevel(this.quantity + amount);
    }
    
    public InventoryLevel subtract(int amount) {
        if (amount > this.quantity) {
            throw new IllegalArgumentException("Cannot subtract more than available quantity");
        }
        return new InventoryLevel(this.quantity - amount);
    }
    
    public boolean isAvailable() {
        return quantity > 0;
    }
    
    public boolean isLow() {
        return quantity <= LOW_INVENTORY_THRESHOLD && quantity > 0;
    }
    
    public boolean canFulfill(int requestedQuantity) {
        return quantity >= requestedQuantity;
    }
    
    public int getQuantity() {
        return quantity;
    }
}
```

## Enumerations

### Domain Enumerations

```java
public enum CustomerStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    SUSPENDED("Suspended"),
    DELETED("Deleted");
    
    private final String displayName;
    
    CustomerStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

public enum MembershipLevel {
    BRONZE("Bronze", 1.0),
    SILVER("Silver", 1.05),
    GOLD("Gold", 1.10),
    PLATINUM("Platinum", 1.15);
    
    private final String displayName;
    private final double discountMultiplier;
    
    MembershipLevel(String displayName, double discountMultiplier) {
        this.displayName = displayName;
        this.discountMultiplier = discountMultiplier;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public double getDiscountMultiplier() {
        return discountMultiplier;
    }
}

public enum OrderStatus {
    CREATED("Created"),
    SUBMITTED("Submitted"),
    CONFIRMED("Confirmed"),
    PROCESSING("Processing"),
    SHIPPED("Shipped"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled"),
    RETURNED("Returned");
    
    private final String displayName;
    
    OrderStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED || this == RETURNED;
    }
    
    public boolean canTransitionTo(OrderStatus newStatus) {
        return switch (this) {
            case CREATED -> newStatus == SUBMITTED || newStatus == CANCELLED;
            case SUBMITTED -> newStatus == CONFIRMED || newStatus == CANCELLED;
            case CONFIRMED -> newStatus == PROCESSING || newStatus == CANCELLED;
            case PROCESSING -> newStatus == SHIPPED || newStatus == CANCELLED;
            case SHIPPED -> newStatus == DELIVERED;
            case DELIVERED -> newStatus == RETURNED;
            case CANCELLED, RETURNED -> false;
        };
    }
}

public enum ProductStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    DISCONTINUED("Discontinued"),
    OUT_OF_STOCK("Out of Stock");
    
    private final String displayName;
    
    ProductStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

public enum ProductCategory {
    ELECTRONICS("Electronics"),
    CLOTHING("Clothing"),
    BOOKS("Books"),
    HOME_GARDEN("Home & Garden"),
    SPORTS_OUTDOORS("Sports & Outdoors"),
    TOYS_GAMES("Toys & Games"),
    HEALTH_BEAUTY("Health & Beauty"),
    AUTOMOTIVE("Automotive");
    
    private final String displayName;
    
    ProductCategory(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

public enum InventoryOperation {
    ADD("Add"),
    SUBTRACT("Subtract"),
    SET("Set");
    
    private final String displayName;
    
    InventoryOperation(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
```

## JPA Entity Mappings

### 1. Customer JPA Entity

```java
@Entity
@Table(name = "customers")
public class CustomerJpaEntity {
    
    @Id
    @Column(name = "customer_id")
    private String customerId;
    
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;
    
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;
    
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;
    
    @Column(name = "phone_country_code", length = 5)
    private String phoneCountryCode;
    
    @Column(name = "phone_number", length = 15)
    private String phoneNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "membership_level", nullable = false)
    private MembershipLevel membershipLevel;
    
    @Column(name = "reward_points", nullable = false)
    private Integer rewardPoints;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CustomerStatus status;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Constructors
    protected CustomerJpaEntity() {}
    
    public CustomerJpaEntity(Customer customer) {
        this.customerId = customer.getCustomerId().value();
        this.firstName = customer.getCustomerName().firstName();
        this.lastName = customer.getCustomerName().lastName();
        this.email = customer.getEmail().value();
        
        if (customer.getPhone() != null) {
            this.phoneCountryCode = customer.getPhone().countryCode();
            this.phoneNumber = customer.getPhone().number();
        }
        
        this.membershipLevel = customer.getMembershipLevel();
        this.rewardPoints = customer.getRewardPoints().value();
        this.status = customer.getStatus();
        this.createdAt = customer.getCreatedAt();
        this.updatedAt = customer.getUpdatedAt();
    }
    
    // Convert to domain object
    public Customer toDomainObject() {
        Customer customer = new Customer(
            CustomerId.of(customerId),
            CustomerName.of(firstName, lastName),
            Email.of(email),
            membershipLevel
        );
        
        if (phoneCountryCode != null && phoneNumber != null) {
            customer.updateProfile(
                customer.getCustomerName(),
                customer.getEmail(),
                Phone.of(phoneCountryCode, phoneNumber)
            );
        }
        
        // Set reward points and status through reflection or package-private methods
        // This is a simplified example - in practice, you'd need proper domain reconstruction
        
        return customer;
    }
    
    // Update from domain object
    public void updateFromDomainObject(Customer customer) {
        this.firstName = customer.getCustomerName().firstName();
        this.lastName = customer.getCustomerName().lastName();
        this.email = customer.getEmail().value();
        
        if (customer.getPhone() != null) {
            this.phoneCountryCode = customer.getPhone().countryCode();
            this.phoneNumber = customer.getPhone().number();
        }
        
        this.membershipLevel = customer.getMembershipLevel();
        this.rewardPoints = customer.getRewardPoints().value();
        this.status = customer.getStatus();
        this.updatedAt = customer.getUpdatedAt();
    }
    
    // Getters and setters
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhoneCountryCode() { return phoneCountryCode; }
    public void setPhoneCountryCode(String phoneCountryCode) { this.phoneCountryCode = phoneCountryCode; }
    
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    
    public MembershipLevel getMembershipLevel() { return membershipLevel; }
    public void setMembershipLevel(MembershipLevel membershipLevel) { this.membershipLevel = membershipLevel; }
    
    public Integer getRewardPoints() { return rewardPoints; }
    public void setRewardPoints(Integer rewardPoints) { this.rewardPoints = rewardPoints; }
    
    public CustomerStatus getStatus() { return status; }
    public void setStatus(CustomerStatus status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

### 2. Order JPA Entity

```java
@Entity
@Table(name = "orders")
public class OrderJpaEntity {
    
    @Id
    @Column(name = "order_id")
    private String orderId;
    
    @Column(name = "customer_id", nullable = false)
    private String customerId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;
    
    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    
    @Column(name = "shipping_street")
    private String shippingStreet;
    
    @Column(name = "shipping_city")
    private String shippingCity;
    
    @Column(name = "shipping_state")
    private String shippingState;
    
    @Column(name = "shipping_postal_code")
    private String shippingPostalCode;
    
    @Column(name = "shipping_country")
    private String shippingCountry;
    
    @Column(name = "payment_method_id")
    private String paymentMethodId;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItemJpaEntity> items = new ArrayList<>();
    
    // Constructors
    protected OrderJpaEntity() {}
    
    public OrderJpaEntity(Order order) {
        this.orderId = order.getOrderId().value();
        this.customerId = order.getCustomerId().value();
        this.status = order.getStatus();
        this.totalAmount = order.getTotalAmount().amount();
        this.currency = order.getTotalAmount().currency().getCurrencyCode();
        
        if (order.getShippingAddress() != null) {
            ShippingAddress address = order.getShippingAddress();
            this.shippingStreet = address.street();
            this.shippingCity = address.city();
            this.shippingState = address.state();
            this.shippingPostalCode = address.postalCode();
            this.shippingCountry = address.country();
        }
        
        if (order.getPaymentMethodId() != null) {
            this.paymentMethodId = order.getPaymentMethodId().value();
        }
        
        this.createdAt = order.getCreatedAt();
        this.updatedAt = order.getUpdatedAt();
        
        // Convert order items
        this.items = order.getItems().stream()
            .map(item -> new OrderItemJpaEntity(this, item))
            .collect(Collectors.toList());
    }
    
    // Convert to domain object
    public Order toDomainObject() {
        Order order = new Order(
            OrderId.of(orderId),
            CustomerId.of(customerId)
        );
        
        // Reconstruct order items
        for (OrderItemJpaEntity itemEntity : items) {
            order.addItem(
                ProductId.of(itemEntity.getProductId()),
                itemEntity.getQuantity(),
                Money.of(itemEntity.getUnitPrice(), itemEntity.getCurrency())
            );
        }
        
        // Set shipping address if available
        if (shippingStreet != null) {
            order.setShippingAddress(new ShippingAddress(
                shippingStreet, shippingCity, shippingState,
                shippingPostalCode, shippingCountry
            ));
        }
        
        // Set payment method if available
        if (paymentMethodId != null) {
            order.setPaymentMethod(PaymentMethodId.of(paymentMethodId));
        }
        
        return order;
    }
    
    // Getters and setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public List<OrderItemJpaEntity> getItems() { return items; }
    public void setItems(List<OrderItemJpaEntity> items) { this.items = items; }
    
    // Additional getters and setters for shipping and payment information...
}

@Entity
@Table(name = "order_items")
public class OrderItemJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderJpaEntity order;
    
    @Column(name = "product_id", nullable = false)
    private String productId;
    
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;
    
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    
    // Constructors
    protected OrderItemJpaEntity() {}
    
    public OrderItemJpaEntity(OrderJpaEntity order, OrderItem orderItem) {
        this.order = order;
        this.productId = orderItem.getProductId().value();
        this.quantity = orderItem.getQuantity();
        this.unitPrice = orderItem.getUnitPrice().amount();
        this.currency = orderItem.getUnitPrice().currency().getCurrencyCode();
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public OrderJpaEntity getOrder() { return order; }
    public void setOrder(OrderJpaEntity order) { this.order = order; }
    
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
```

## Repository Implementations

### 1. Customer Repository Implementation

```java
@Repository
@Transactional
public class CustomerRepositoryImpl implements CustomerRepository {
    
    private final CustomerJpaRepository customerJpaRepository;
    
    public CustomerRepositoryImpl(CustomerJpaRepository customerJpaRepository) {
        this.customerJpaRepository = customerJpaRepository;
    }
    
    @Override
    public void save(Customer customer) {
        CustomerJpaEntity entity = customerJpaRepository
            .findById(customer.getCustomerId().value())
            .map(existing -> {
                existing.updateFromDomainObject(customer);
                return existing;
            })
            .orElse(new CustomerJpaEntity(customer));
        
        customerJpaRepository.save(entity);
    }
    
    @Override
    public Optional<Customer> findById(CustomerId customerId) {
        return customerJpaRepository.findById(customerId.value())
            .map(CustomerJpaEntity::toDomainObject);
    }
    
    @Override
    public Optional<Customer> findByEmail(Email email) {
        return customerJpaRepository.findByEmail(email.value())
            .map(CustomerJpaEntity::toDomainObject);
    }
    
    @Override
    public List<Customer> findByMembershipLevel(MembershipLevel membershipLevel) {
        return customerJpaRepository.findByMembershipLevel(membershipLevel)
            .stream()
            .map(CustomerJpaEntity::toDomainObject)
            .collect(Collectors.toList());
    }
    
    @Override
    public boolean existsByEmail(Email email) {
        return customerJpaRepository.existsByEmail(email.value());
    }
    
    @Override
    public void delete(Customer customer) {
        customerJpaRepository.deleteById(customer.getCustomerId().value());
    }
}

@Repository
public interface CustomerJpaRepository extends JpaRepository<CustomerJpaEntity, String> {
    
    Optional<CustomerJpaEntity> findByEmail(String email);
    
    List<CustomerJpaEntity> findByMembershipLevel(MembershipLevel membershipLevel);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT c FROM CustomerJpaEntity c WHERE c.status = :status ORDER BY c.createdAt DESC")
    List<CustomerJpaEntity> findByStatusOrderByCreatedAtDesc(@Param("status") CustomerStatus status);
    
    @Query("SELECT c FROM CustomerJpaEntity c WHERE c.rewardPoints >= :minPoints")
    List<CustomerJpaEntity> findByRewardPointsGreaterThanEqual(@Param("minPoints") Integer minPoints);
}
```

### 2. Order Repository Implementation

```java
@Repository
@Transactional
public class OrderRepositoryImpl implements OrderRepository {
    
    private final OrderJpaRepository orderJpaRepository;
    
    public OrderRepositoryImpl(OrderJpaRepository orderJpaRepository) {
        this.orderJpaRepository = orderJpaRepository;
    }
    
    @Override
    public void save(Order order) {
        OrderJpaEntity entity = orderJpaRepository
            .findById(order.getOrderId().value())
            .map(existing -> {
                existing.updateFromDomainObject(order);
                return existing;
            })
            .orElse(new OrderJpaEntity(order));
        
        orderJpaRepository.save(entity);
    }
    
    @Override
    public Optional<Order> findById(OrderId orderId) {
        return orderJpaRepository.findById(orderId.value())
            .map(OrderJpaEntity::toDomainObject);
    }
    
    @Override
    public List<Order> findByCustomerId(CustomerId customerId) {
        return orderJpaRepository.findByCustomerIdOrderByCreatedAtDesc(customerId.value())
            .stream()
            .map(OrderJpaEntity::toDomainObject)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Order> findByStatus(OrderStatus status) {
        return orderJpaRepository.findByStatusOrderByCreatedAtDesc(status)
            .stream()
            .map(OrderJpaEntity::toDomainObject)
            .collect(Collectors.toList());
    }
    
    @Override
    public void delete(Order order) {
        orderJpaRepository.deleteById(order.getOrderId().value());
    }
}

@Repository
public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, String> {
    
    List<OrderJpaEntity> findByCustomerIdOrderByCreatedAtDesc(String customerId);
    
    List<OrderJpaEntity> findByStatusOrderByCreatedAtDesc(OrderStatus status);
    
    @Query("SELECT o FROM OrderJpaEntity o WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate")
    List<OrderJpaEntity> findByCreatedAtBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT COUNT(o) FROM OrderJpaEntity o WHERE o.customerId = :customerId")
    Long countByCustomerId(@Param("customerId") String customerId);
    
    @Query("SELECT SUM(o.totalAmount) FROM OrderJpaEntity o WHERE o.customerId = :customerId AND o.status = :status")
    BigDecimal sumTotalAmountByCustomerIdAndStatus(
        @Param("customerId") String customerId,
        @Param("status") OrderStatus status
    );
}
```

## Database Schema

### 1. DDL Scripts

```sql
-- Customer table
CREATE TABLE customers (
    customer_id VARCHAR(50) PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone_country_code VARCHAR(5),
    phone_number VARCHAR(15),
    membership_level VARCHAR(20) NOT NULL DEFAULT 'BRONZE',
    reward_points INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Orders table
CREATE TABLE orders (
    order_id VARCHAR(50) PRIMARY KEY,
    customer_id VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    total_amount DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    shipping_street VARCHAR(255),
    shipping_city VARCHAR(100),
    shipping_state VARCHAR(100),
    shipping_postal_code VARCHAR(20),
    shipping_country VARCHAR(100),
    payment_method_id VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
);

-- Order items table
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL,
    product_id VARCHAR(50) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE
);

-- Products table
CREATE TABLE products (
    product_id VARCHAR(50) PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    category VARCHAR(50) NOT NULL,
    inventory_quantity INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 2. Indexes for Performance

```sql
-- Customer indexes
CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_customers_membership_level ON customers(membership_level);
CREATE INDEX idx_customers_status ON customers(status);
CREATE INDEX idx_customers_created_at ON customers(created_at);

-- Order indexes
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_orders_customer_status ON orders(customer_id, status);

-- Order item indexes
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);

-- Product indexes
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_status ON products(status);
CREATE INDEX idx_products_name ON products(product_name);
CREATE INDEX idx_products_price ON products(price);
```

## Data Migration Strategies

### 1. Flyway Migration Scripts

```sql
-- V1__Create_initial_schema.sql
CREATE TABLE customers (
    customer_id VARCHAR(50) PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    membership_level VARCHAR(20) NOT NULL DEFAULT 'BRONZE',
    reward_points INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- V2__Add_phone_to_customers.sql
ALTER TABLE customers 
ADD COLUMN phone_country_code VARCHAR(5),
ADD COLUMN phone_number VARCHAR(15);

-- V3__Create_orders_table.sql
CREATE TABLE orders (
    order_id VARCHAR(50) PRIMARY KEY,
    customer_id VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    total_amount DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
);

-- V4__Add_shipping_to_orders.sql
ALTER TABLE orders 
ADD COLUMN shipping_street VARCHAR(255),
ADD COLUMN shipping_city VARCHAR(100),
ADD COLUMN shipping_state VARCHAR(100),
ADD COLUMN shipping_postal_code VARCHAR(20),
ADD COLUMN shipping_country VARCHAR(100),
ADD COLUMN payment_method_id VARCHAR(50);
```

### 2. Data Validation and Constraints

```sql
-- Add check constraints for data integrity
ALTER TABLE customers 
ADD CONSTRAINT chk_customers_email_format 
CHECK (email ~* '^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');

ALTER TABLE customers 
ADD CONSTRAINT chk_customers_reward_points_positive 
CHECK (reward_points >= 0);

ALTER TABLE orders 
ADD CONSTRAINT chk_orders_total_amount_positive 
CHECK (total_amount >= 0);

ALTER TABLE order_items 
ADD CONSTRAINT chk_order_items_quantity_positive 
CHECK (quantity > 0);

ALTER TABLE order_items 
ADD CONSTRAINT chk_order_items_unit_price_positive 
CHECK (unit_price >= 0);

ALTER TABLE products 
ADD CONSTRAINT chk_products_price_positive 
CHECK (price >= 0);

ALTER TABLE products 
ADD CONSTRAINT chk_products_inventory_non_negative 
CHECK (inventory_quantity >= 0);
```

## Best Practices

### 1. Domain Model Design

- **Rich Domain Models**: Use behavior-rich domain objects instead of anemic data models
- **Value Object Immutability**: Ensure all value objects are immutable
- **Aggregate Boundaries**: Keep aggregates small and focused on business invariants
- **Domain Events**: Use domain events for cross-aggregate communication

### 2. Data Persistence

- **Separation of Concerns**: Keep domain models separate from persistence models
- **Repository Pattern**: Use repositories to abstract data access
- **Transaction Boundaries**: Align transactions with aggregate boundaries
- **Optimistic Locking**: Use version fields for concurrency control

### 3. Performance Considerations

- **Lazy Loading**: Use lazy loading for large collections
- **Query Optimization**: Create appropriate indexes for common queries
- **Batch Operations**: Use batch operations for bulk data processing
- **Connection Pooling**: Configure proper connection pool settings

### 4. Data Integrity

- **Validation**: Implement validation at both domain and database levels
- **Constraints**: Use database constraints to enforce data integrity
- **Referential Integrity**: Maintain proper foreign key relationships
- **Audit Trails**: Implement audit logging for sensitive operations

This comprehensive data model design provides a solid foundation for the e-commerce platform while following Domain-Driven Design principles and ensuring data integrity, performance, and maintainability.
