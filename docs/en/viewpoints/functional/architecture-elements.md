
# Functional ViewpointArchitectural Element

## Overview

本文件詳細描述Functional Viewpoint中的核心Architectural Element，包括領域模型、Aggregate Root、Entity、Value Object、Domain Service和應用服務等關鍵組件。

## 核心Architectural Element

### 1. Aggregate Root (Aggregate Roots)

Aggregate Root是領域模型的核心元素，負責維護業務不變性和協調Aggregate內的操作。

#### 主要Aggregate Root

| Aggregate Root | Bounded Context | 職責 | 關鍵業務規則 |
|--------|------------|------|--------------|
| Customer | Customer Management | Customer生命週期管理 | 唯一電子郵件、會員等級規則 |
| Order | Order Management | 訂單處理和狀態管理 | 訂單狀態轉換、金額計算 |
| Product | Product Catalog | 產品資訊和庫存管理 | 庫存數量、價格有效性 |
| Payment | Payment Processing | 支付處理和記錄 | 支付狀態、退款規則 |
| Inventory | Inventory Management | 庫存控制和預留 | 庫存扣減、預留機制 |
| Promotion | Promotion Engine | 促銷規則和折扣 | 促銷條件、折扣計算 |
| Shipping | Shipping Management | 配送管理和Tracing | 配送狀態、地址驗證 |

#### Design

```java
@AggregateRoot(name = "Customer", description = "CustomerAggregate Root", boundedContext = "Customer", version = "2.0")
public class Customer implements AggregateRootInterface {
    
    // Aggregate Root標識
    private final CustomerId id;
    
    // 業務屬性
    private CustomerName name;
    private Email email;
    private MembershipLevel membershipLevel;
    
    // Aggregate內Entity
    private List<DeliveryAddress> addresses;
    private List<PaymentMethod> paymentMethods;
    
    // 業務方法
    public void updateProfile(CustomerName newName, Email newEmail) {
        validateProfileUpdate(newName, newEmail);
        this.name = newName;
        this.email = newEmail;
        collectEvent(CustomerProfileUpdatedEvent.create(this.id, newName, newEmail));
    }
    
    // 業務規則驗證
    private void validateProfileUpdate(CustomerName newName, Email newEmail) {
        if (newName == null || newEmail == null) {
            throw new InvalidProfileDataException("姓名和電子郵件不能為空");
        }
    }
}
```

### 2. Entity (Entities)

Entity是具有唯一標識和生命週期的領域對象。

#### Design

```java
@Entity
public class DeliveryAddress {
    
    private final DeliveryAddressId id;
    private Address address;
    private AddressType type;
    private boolean isDefault;
    
    public void markAsDefault() {
        this.isDefault = true;
        // 業務邏輯：確保只有一個預設地址
    }
    
    public boolean isValidForDelivery() {
        return address.isComplete() && address.isDeliverable();
    }
}
```

### 3. Value Object (Value Objects)

Value Object是不可變的領域概念，通過屬性值定義相等性。

#### Value Object實現

```java
@ValueObject
public record CustomerId(String value) {
    
    public CustomerId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("CustomerID不能為空");
        }
        if (!value.matches("CUST-\\d{6}")) {
            throw new IllegalArgumentException("CustomerID格式無效");
        }
    }
    
    public static CustomerId generate() {
        return new CustomerId("CUST-" + String.format("%06d", new Random().nextInt(999999)));
    }
    
    public static CustomerId of(String value) {
        return new CustomerId(value);
    }
}

@ValueObject
public record Money(BigDecimal amount, Currency currency) {
    
    public Money {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("金額不能為負數");
        }
        if (currency == null) {
            throw new IllegalArgumentException("貨幣不能為空");
        }
    }
    
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("不同貨幣無法相加");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }
    
    public Money multiply(BigDecimal multiplier) {
        return new Money(this.amount.multiply(multiplier), this.currency);
    }
}
```

### 4. Domain Service (Domain Services)

Domain Service封裝跨Aggregate的業務邏輯。

#### Domain Service實現

```java
@DomainService
public class PricingService {
    
    public Money calculateOrderTotal(Order order, List<Promotion> applicablePromotions) {
        Money subtotal = calculateSubtotal(order);
        Money discount = calculateDiscount(subtotal, applicablePromotions);
        Money tax = calculateTax(subtotal.subtract(discount));
        
        return subtotal.subtract(discount).add(tax);
    }
    
    private Money calculateDiscount(Money subtotal, List<Promotion> promotions) {
        return promotions.stream()
            .map(promotion -> promotion.calculateDiscount(subtotal))
            .reduce(Money.ZERO, Money::add);
    }
}

@DomainService
public class InventoryAllocationService {
    
    public AllocationResult allocateInventory(Order order) {
        List<AllocationItem> allocations = new ArrayList<>();
        
        for (OrderItem item : order.getItems()) {
            AllocationResult itemResult = allocateItem(item);
            if (!itemResult.isSuccessful()) {
                return AllocationResult.failed(itemResult.getReason());
            }
            allocations.add(itemResult.getAllocation());
        }
        
        return AllocationResult.successful(allocations);
    }
}
```

### 5. 應用服務 (Application Services)

應用服務協調用例執行，不包含業務邏輯。

#### 應用服務模式

```java
@Service
@Transactional
public class CustomerApplicationService {
    
    private final CustomerRepository customerRepository;
    private final DomainEventApplicationService domainEventService;
    
    public Customer createCustomer(CreateCustomerCommand command) {
        // 1. 驗證Command
        validateCommand(command);
        
        // 2. 檢查業務規則
        if (customerRepository.existsByEmail(command.email())) {
            throw new EmailAlreadyExistsException(command.email());
        }
        
        // 3. 創建Aggregate Root
        Customer customer = Customer.create(
            command.name(),
            command.email(),
            command.membershipLevel()
        );
        
        // 4. 保存Aggregate Root
        Customer savedCustomer = customerRepository.save(customer);
        
        // 5. 發布Domain Event
        domainEventService.publishEventsFromAggregate(savedCustomer);
        
        return savedCustomer;
    }
}
```

## Architectural Elements關係圖

### 元素層次結構

```
應用服務層
    ├── CustomerApplicationService
    ├── OrderApplicationService
    └── ProductApplicationService
        │
        ▼ 協調
Domain Layer
    ├── Aggregate Root
    │   ├── Customer
    │   ├── Order
    │   └── Product
    ├── Entity
    │   ├── DeliveryAddress
    │   ├── OrderItem
    │   └── ProductVariant
    ├── Value Object
    │   ├── CustomerId
    │   ├── Money
    │   └── Address
    └── Domain Service
        ├── PricingService
        └── InventoryAllocationService
```

### 依賴關係

- **應用服務** → **Aggregate Root** (協調業務操作)
- **Aggregate Root** → **Entity** (包含和管理)
- **Aggregate Root** → **Value Object** (使用和組合)
- **Aggregate Root** → **Domain Service** (委託複雜邏輯)
- **Domain Service** → **多個Aggregate Root** (跨Aggregate操作)

## Design

### Design

1. **Aggregate邊界**: 基於業務不變性和事務一致性
2. **Aggregate大小**: 避免過大的Aggregate，影響Performance
3. **Aggregate引用**: 通過ID引用其他Aggregate，避免直接引用
4. **事務邊界**: 一個事務只能修改一個Aggregate

### Design

1. **唯一標識**: 每個Entity必須有唯一標識
2. **生命週期**: Entity的創建、修改和刪除規則
3. **業務方法**: Entity應包含相關的業務方法
4. **狀態一致性**: Entity狀態變更必須保持一致性

### Design

1. **不可變性**: Value Object創建後不能修改
2. **相等性**: 基於屬性值判斷相等性
3. **驗證**: 創建時進行完整性驗證
4. **業務方法**: 包含相關的業務計算方法

## 實現檢查清單

### Aggregate Root檢查

- [ ] 實現 AggregateRootInterface
- [ ] 使用 @AggregateRoot 註解
- [ ] 包含業務方法和規則驗證
- [ ] 正確收集和發布Domain Event
- [ ] 維護Aggregate內的業務不變性

### Entity檢查

- [ ] 使用 @Entity 註解
- [ ] 具有唯一標識
- [ ] 包含相關業務方法
- [ ] 正確處理狀態轉換
- [ ] 與Aggregate Root的關係清晰

### Value Object檢查

- [ ] 使用 @ValueObject 註解
- [ ] 實現為不可變 Record
- [ ] 包含驗證邏輯
- [ ] 實現業務計算方法
- [ ] 正確處理相等性比較

### 服務檢查

- [ ] Domain Service使用 @DomainService 註解
- [ ] 應用服務使用 @Service 註解
- [ ] 職責分離清晰
- [ ] 依賴注入正確
- [ ] 事務邊界合理

---

**相關文件**:
- [領域模型設計](domain-model.md)
- [Aggregate RootImplementation Guide](aggregates.md)
- [Domain Event設計](../information/domain-events.md)