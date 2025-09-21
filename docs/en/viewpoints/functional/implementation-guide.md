
# Guidelines

## Overview

本指南提供Functional Viewpoint實現的詳細步驟、Best Practice和具體範例，幫助開發團隊正確實現Domain-Driven Design和功能需求。

## 實現流程

### 階段一：領域分析和建模

#### Requirements

```markdown
## Requirements

### Requirements
- Customer可以使用電子郵件註冊帳戶
- 系統驗證電子郵件唯一性
- 註冊成功後發送歡迎郵件
- Customer獲得預設會員等級

### 業務規則
- 電子郵件必須唯一
- 密碼必須符合安全要求
- 新Customer預設為一般會員
- 註冊後24小時內必須驗證電子郵件
```

#### 2. 領域建模工作坊

```markdown
## Event Storming 工作坊結果

### Domain Event
- CustomerRegistered (Customer已註冊)
- EmailVerificationSent (電子郵件驗證已發送)
- EmailVerified (電子郵件已驗證)
- WelcomeEmailSent (歡迎郵件已發送)

### Command
- RegisterCustomer (註冊Customer)
- VerifyEmail (驗證電子郵件)
- ResendVerificationEmail (重新發送驗證郵件)

### Aggregate Root
- Customer (Customer)

### External System
- EmailService (郵件服務)
- IdentityProvider (身份提供者)
```

#### 3. Bounded Context識別

```java
// Bounded Context對應表
public enum BoundedContext {
    CUSTOMER_MANAGEMENT("Customer管理", "管理Customer生命週期和基本資訊"),
    IDENTITY_ACCESS("身份存取", "處理認證和授權"),
    NOTIFICATION("通知服務", "處理各種通知和郵件"),
    MEMBERSHIP("會員管理", "管理會員等級和權益");
}
```

### Design

#### 1. Aggregate Root識別原則

```java
// Aggregate Root識別檢查清單
public class AggregateRootIdentificationGuide {
    
    /**
     * Aggregate Root識別標準：
     * 1. 具有全域唯一標識
     * 2. 有獨立的生命週期
     * 3. 維護業務不變性
     * 4. 是業務概念的根Entity
     * 5. 控制對Aggregate內其他Entity的存取
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

#### Templates

```java
@AggregateRoot(name = "Customer", description = "CustomerAggregate Root", boundedContext = "CustomerManagement", version = "1.0")
public class Customer implements AggregateRootInterface {
    
    // === Aggregate Root標識 ===
    private final CustomerId id;
    
    // === 核心業務屬性 ===
    private CustomerName name;
    private Email email;
    private MembershipLevel membershipLevel;
    private CustomerStatus status;
    private LocalDateTime registrationDate;
    private LocalDateTime lastLoginDate;
    
    // === Aggregate內Entity集合 ===
    private final List<DeliveryAddress> addresses = new ArrayList<>();
    private final List<PaymentMethod> paymentMethods = new ArrayList<>();
    private CustomerPreferences preferences;
    
    // === 建構子 ===
    private Customer(CustomerId id, CustomerName name, Email email) {
        this.id = requireNonNull(id, "CustomerID不能為空");
        this.name = requireNonNull(name, "Customer姓名不能為空");
        this.email = requireNonNull(email, "電子郵件不能為空");
        this.membershipLevel = MembershipLevel.STANDARD;
        this.status = CustomerStatus.PENDING_VERIFICATION;
        this.registrationDate = LocalDateTime.now();
        this.preferences = CustomerPreferences.defaultPreferences();
    }
    
    // === Factory方法 ===
    public static Customer register(CustomerName name, Email email) {
        CustomerId id = CustomerId.generate();
        Customer customer = new Customer(id, name, email);
        
        // 收集Domain Event
        customer.collectEvent(CustomerRegisteredEvent.create(id, name, email));
        
        return customer;
    }
    
    // === 業務方法 ===
    public void verifyEmail() {
        if (this.status != CustomerStatus.PENDING_VERIFICATION) {
            throw new InvalidCustomerStatusException("Customer狀態不允許驗證電子郵件");
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
        
        // 如果是第一個地址，設為預設
        if (addresses.isEmpty()) {
            deliveryAddress.markAsDefault();
        }
        
        addresses.add(deliveryAddress);
        collectEvent(CustomerAddressAddedEvent.create(this.id, addressId, address, type));
    }
    
    public void promoteToMembership(MembershipLevel newLevel) {
        if (newLevel.ordinal() <= this.membershipLevel.ordinal()) {
            throw new InvalidMembershipPromotionException("不能降級或平級調整會員等級");
        }
        
        MembershipLevel oldLevel = this.membershipLevel;
        this.membershipLevel = newLevel;
        
        collectEvent(CustomerMembershipPromotedEvent.create(this.id, oldLevel, newLevel));
    }
    
    // === 業務規則驗證 ===
    private void validateProfileUpdate(CustomerName newName, Email newEmail) {
        if (newName == null || newEmail == null) {
            throw new InvalidProfileDataException("姓名和電子郵件不能為空");
        }
        
        if (this.status == CustomerStatus.SUSPENDED) {
            throw new InvalidCustomerStatusException("已暫停的Customer無法更新資料");
        }
    }
    
    private void validateAddress(Address address) {
        if (address == null || !address.isValid()) {
            throw new InvalidAddressException("地址資訊無效");
        }
        
        if (addresses.size() >= 5) {
            throw new TooManyAddressesException("Customer最多只能有5個配送地址");
        }
    }
    
    // === 查詢方法 ===
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

#### Templates

```java
@ValueObject
public record CustomerId(String value) {
    
    private static final String PREFIX = "CUST-";
    private static final Pattern PATTERN = Pattern.compile("^CUST-\\d{8}$");
    
    public CustomerId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("CustomerID不能為空");
        }
        
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("CustomerID格式無效，應為：CUST-xxxxxxxx");
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
            throw new IllegalArgumentException("電子郵件不能為空");
        }
        
        String normalizedValue = value.trim().toLowerCase();
        
        if (!EMAIL_PATTERN.matcher(normalizedValue).matches()) {
            throw new IllegalArgumentException("電子郵件格式無效");
        }
        
        // 重新賦值為標準化後的值
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

### 階段三：應用服務實現

#### Templates

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
    
    // === Command處理方法 ===
    
    public Customer registerCustomer(RegisterCustomerCommand command) {
        // 1. 驗證Command
        validateCommand(command);
        
        // 2. 檢查業務規則
        if (customerRepository.existsByEmail(command.email())) {
            throw new EmailAlreadyExistsException(command.email());
        }
        
        // 3. 創建Aggregate Root
        Customer customer = Customer.register(command.name(), command.email());
        
        // 4. 保存Aggregate Root
        Customer savedCustomer = customerRepository.save(customer);
        
        // 5. 發布Domain Event
        domainEventService.publishEventsFromAggregate(savedCustomer);
        
        return savedCustomer;
    }
    
    public void verifyCustomerEmail(VerifyEmailCommand command) {
        // 1. 載入Aggregate Root
        Customer customer = customerRepository.findById(command.customerId())
            .orElseThrow(() -> new CustomerNotFoundException(command.customerId()));
        
        // 2. 執行業務操作
        customer.verifyEmail();
        
        // 3. 保存變更
        customerRepository.save(customer);
        
        // 4. 發布事件
        domainEventService.publishEventsFromAggregate(customer);
    }
    
    public Customer updateCustomerProfile(UpdateProfileCommand command) {
        // 1. 載入Aggregate Root
        Customer customer = customerRepository.findById(command.customerId())
            .orElseThrow(() -> new CustomerNotFoundException(command.customerId()));
        
        // 2. 執行業務操作
        customer.updateProfile(command.name(), command.email());
        
        // 3. 保存變更
        Customer updatedCustomer = customerRepository.save(customer);
        
        // 4. 發布事件
        domainEventService.publishEventsFromAggregate(updatedCustomer);
        
        return updatedCustomer;
    }
    
    // === 查詢方法 ===
    
    @Transactional(readOnly = true)
    public Customer getCustomer(CustomerId customerId) {
        return customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }
    
    @Transactional(readOnly = true)
    public Page<Customer> getCustomers(CustomerSearchCriteria criteria, Pageable pageable) {
        return customerRepository.findByCriteria(criteria, pageable);
    }
    
    // === 私有輔助方法 ===
    
    private void validateCommand(RegisterCustomerCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("註冊Command不能為空");
        }
        
        if (command.name() == null || command.email() == null) {
            throw new IllegalArgumentException("Customer姓名和電子郵件不能為空");
        }
    }
}
```

#### Design

```java
// Command基礎介面
public interface Command {
    UUID getCommandId();
    LocalDateTime getTimestamp();
    String getInitiatedBy();
}

// 抽象Command基類
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

// 具體Command實現
public record RegisterCustomerCommand(
    CustomerName name,
    Email email,
    String initiatedBy
) implements Command {
    
    private static final UUID commandId = UUID.randomUUID();
    private static final LocalDateTime timestamp = LocalDateTime.now();
    
    public RegisterCustomerCommand {
        if (name == null || email == null) {
            throw new IllegalArgumentException("Customer姓名和電子郵件不能為空");
        }
        if (initiatedBy == null || initiatedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Command發起者不能為空");
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

### 階段四：Domain Event實現

#### Design

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

#### 2. 事件處理器實現

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
            // 1. 發送歡迎郵件
            sendWelcomeEmail(event);
            
            // 2. 更新統計資料
            updateCustomerStatistics(event);
            
            // 3. 準備歡迎禮包
            prepareWelcomePackage(event);
            
            // 4. 記錄處理成功
            markEventAsProcessed(event.getEventId());
            
        } catch (Exception e) {
            logger.error("處理Customer註冊事件失敗", e);
            throw new DomainEventProcessingException("Customer註冊後續處理失敗", e);
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

### 階段五：Infrastructure Layer實現

#### 1. 儲存庫實現

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
        
        // 標記事件為已提交
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

#### 2. Entity映射器

```java
@Component
public class CustomerMapper {
    
    public Customer toDomain(CustomerEntity entity) {
        if (entity == null) return null;
        
        // 使用反射或建構子創建領域對象
        Customer customer = Customer.reconstitute(
            CustomerId.of(entity.getId()),
            new CustomerName(entity.getName()),
            new Email(entity.getEmail()),
            entity.getMembershipLevel(),
            entity.getStatus(),
            entity.getRegistrationDate(),
            entity.getLastLoginDate()
        );
        
        // 映射Aggregate內Entity
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
        
        // 映射Aggregate內Entity
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

## Testing

### Testing

```java
@ExtendWith(MockitoExtension.class)
class CustomerTest {
    
    @Test
    void should_create_customer_with_correct_initial_state() {
        // Given
        CustomerName name = new CustomerName("張三");
        Email email = new Email("zhang.san@example.com");
        
        // When
        Customer customer = Customer.register(name, email);
        
        // Then
        assertThat(customer.getId()).isNotNull();
        assertThat(customer.getName()).isEqualTo(name);
        assertThat(customer.getEmail()).isEqualTo(email);
        assertThat(customer.getMembershipLevel()).isEqualTo(MembershipLevel.STANDARD);
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.PENDING_VERIFICATION);
        
        // 驗證Domain Event
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
            .hasMessage("Customer狀態不允許驗證電子郵件");
    }
}
```

### Testing

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
            new CustomerName("李四"),
            new Email("li.si@example.com"),
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
            new CustomerName("王五"),
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

### Testing

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
            new CustomerName("Integration TestCustomer"),
            new Email("integration.test@example.com"),
            "test-system"
        );
        
        // When - 註冊Customer
        Customer registeredCustomer = customerApplicationService.registerCustomer(registerCommand);
        
        // Then - 驗證註冊結果
        assertThat(registeredCustomer).isNotNull();
        assertThat(registeredCustomer.getStatus()).isEqualTo(CustomerStatus.PENDING_VERIFICATION);
        
        // When - 驗證電子郵件
        VerifyEmailCommand verifyCommand = new VerifyEmailCommand(
            registeredCustomer.getId(),
            "test-system"
        );
        customerApplicationService.verifyCustomerEmail(verifyCommand);
        
        // Then - 驗證最終狀態
        Customer verifiedCustomer = customerRepository.findById(registeredCustomer.getId()).orElseThrow();
        assertThat(verifiedCustomer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
    }
}
```

## Best Practices

### Design

- [ ] 完成 Event Storming 工作坊
- [ ] 識別核心Aggregate Root和Bounded Context
- [ ] 定義清晰的業務規則和不變性
- [ ] 設計適當的Value Object和Entity
- [ ] 規劃Domain Event和Command

### 實現階段

- [ ] Aggregate Root實現 AggregateRootInterface
- [ ] Value Object使用不可變 Record
- [ ] 應用服務只協調，不包含業務邏輯
- [ ] 正確實現Domain Event收集和發布
- [ ] 儲存庫介面在Domain Layer定義

### Testing

- [ ] Aggregate RootUnit Test覆蓋所有業務方法
- [ ] 應用服務測試驗證協調邏輯
- [ ] Integration Test驗證完整業務流程
- [ ] 事件處理器測試確保正確處理
- [ ] Performance Test滿足響應時間要求

### Quality Assurance

- [ ] 所有業務規則在Domain Layer實現
- [ ] Aggregate邊界設計合理
- [ ] 事務邊界與Aggregate邊界一致
- [ ] Domain Event正確建模業務概念
- [ ] 錯誤處理完整且User友好

---

**相關文件**:
- [Architectural Element詳解](architecture-elements.md)
- [品質考量指南](quality-considerations.md)
- [Domain Event實現](../information/domain-events.md)
- [測試Policy](../development/testing-strategy.md)