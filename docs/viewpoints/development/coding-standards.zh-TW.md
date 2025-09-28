# 編碼標準與規範

## 概述

本文檔定義了專案的編碼標準和最佳實踐指南，確保程式碼的一致性、可讀性和可維護性。這些標準涵蓋 Java 後端、TypeScript/React/Angular 前端、API 設計、資料庫設計等各個方面。

## 🎯 核心原則

### 1. 一致性原則
- 遵循統一的編碼風格和格式
- 使用一致的命名約定
- 保持專案結構和架構模式的一致性
- 統一的錯誤處理和日誌記錄方式

### 2. 可讀性原則
- 編寫自文檔化的程式碼
- 使用有意義且描述性的變數和方法名稱
- 適當添加註釋說明複雜邏輯和業務規則
- 保持程式碼簡潔明瞭，避免過度複雜

### 3. 可維護性原則
- 遵循 SOLID 原則和 DDD 戰術模式
- 保持方法和類別的簡潔，單一職責
- 避免程式碼重複，提取共用邏輯
- 設計易於測試和擴展的程式碼結構

### 4. 安全性原則
- 遵循安全編碼實踐
- 進行嚴格的輸入驗證和輸出編碼
- 保護敏感資料，避免資訊洩露
- 實施適當的認證和授權機制

## 📋 Java 編碼標準

### 命名約定

#### 類別和介面
```java
// ✅ 正確：使用 PascalCase，名稱具有描述性
public class CustomerRegistrationService { }
public interface PaymentGatewayAdapter { }
public class OrderCreatedEvent { }

// ❌ 錯誤：縮寫、不清楚的名稱
public class CustRegSvc { }
public interface PmtGw { }
public class Event1 { }
```

#### 方法和變數
```java
// ✅ 正確：使用 camelCase，動詞-名詞模式
public Customer findCustomerById(String customerId) { }
public boolean isEligibleForDiscount(Customer customer) { }
public void sendWelcomeEmail(String emailAddress) { }

private final CustomerRepository customerRepository;
private final EmailNotificationService emailNotificationService;

// ❌ 錯誤：不清楚的名稱
public Customer get(String id) { }
public boolean check(Customer c) { }
public void send(String addr) { }

private final CustomerRepository repo;
private final EmailNotificationService svc;
```

#### 常數和列舉
```java
// ✅ 正確：使用 UPPER_SNAKE_CASE
public static final String DEFAULT_CURRENCY_CODE = "TWD";
public static final int MAX_RETRY_ATTEMPTS = 3;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}

// ❌ 錯誤：不一致的命名
public static final String defaultCurrency = "TWD";
public static final int maxRetry = 3;
```

### 程式碼結構標準

#### 方法設計
```java
// ✅ 正確：方法簡潔，單一職責
@Service
@Transactional
public class OrderProcessingService {
    
    public Order processOrder(ProcessOrderCommand command) {
        validateOrderCommand(command);
        
        Order order = createOrderFromCommand(command);
        reserveInventory(order);
        processPayment(order);
        
        Order savedOrder = orderRepository.save(order);
        publishOrderCreatedEvent(savedOrder);
        
        return savedOrder;
    }
    
    private void validateOrderCommand(ProcessOrderCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Order command cannot be null");
        }
        if (command.getItems().isEmpty()) {
            throw new BusinessRuleViolationException("Order must contain at least one item");
        }
    }
}

// ❌ 錯誤：方法過長，職責混雜
public Order processOrder(ProcessOrderCommand command) {
    // 50+ 行混合驗證、計算、處理邏輯
    if (command != null && !command.getItems().isEmpty()) {
        // 複雜的驗證邏輯...
        // 複雜的計算邏輯...
        // 複雜的處理邏輯...
        // 複雜的儲存邏輯...
    }
}
```

#### 類別設計
```java
// ✅ 正確：單一職責，清楚的目的
@AggregateRoot(name = "Customer", boundedContext = "Customer")
public class Customer implements AggregateRootInterface {
    
    private final CustomerId id;
    private CustomerName name;
    private Email email;
    private CustomerStatus status;
    
    public void updateProfile(CustomerName newName, Email newEmail) {
        validateProfileUpdate(newName, newEmail);
        
        this.name = newName;
        this.email = newEmail;
        
        collectEvent(CustomerProfileUpdatedEvent.create(this.id, newName, newEmail));
    }
    
    private void validateProfileUpdate(CustomerName name, Email email) {
        if (this.status == CustomerStatus.SUSPENDED) {
            throw new BusinessRuleViolationException("Cannot update profile of suspended customer");
        }
    }
}

// ❌ 錯誤：多重職責，不清楚的目的
@Service
public class CustomerService {
    // 處理客戶、訂單、產品、付款、通知、報告...
    // 500+ 行混合職責
}
```

### 異常處理標準

#### 自定義異常層次
```java
// 基礎領域異常
public abstract class DomainException extends RuntimeException {
    private final String errorCode;
    private final Map<String, Object> context;
    
    protected DomainException(String errorCode, String message, Map<String, Object> context) {
        super(message);
        this.errorCode = errorCode;
        this.context = context != null ? context : Map.of();
    }
    
    public String getErrorCode() { return errorCode; }
    public Map<String, Object> getContext() { return context; }
}

// 業務規則違反異常
public class BusinessRuleViolationException extends DomainException {
    public BusinessRuleViolationException(String rule, String message) {
        super("BUSINESS_RULE_VIOLATION", message, Map.of("rule", rule));
    }
}

// 資源未找到異常
public class ResourceNotFoundException extends DomainException {
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super("RESOURCE_NOT_FOUND", 
              String.format("%s with id %s not found", resourceType, resourceId),
              Map.of("resourceType", resourceType, "resourceId", resourceId));
    }
}
```

#### 異常處理最佳實踐
```java
// ✅ 正確：具體的異常處理，適當的上下文
@Service
public class CustomerService {
    
    public Customer findCustomerById(String customerId) {
        try {
            return customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
        } catch (DataAccessException e) {
            logger.error("Database error while fetching customer: {}", customerId, e);
            throw new CustomerServiceException("Unable to retrieve customer data", e);
        }
    }
}

// ❌ 錯誤：通用異常，缺乏上下文
public Customer findCustomerById(String customerId) {
    try {
        return customerRepository.findById(customerId).get();
    } catch (Exception e) {
        throw new RuntimeException("Error");
    }
}
```

## 🌐 前端編碼標準

### TypeScript 標準

#### 型別定義
```typescript
// ✅ 正確：明確的型別定義
interface Customer {
  readonly id: string;
  name: string;
  email: string;
  status: CustomerStatus;
  createdAt: Date;
  updatedAt: Date;
}

type CustomerStatus = 'ACTIVE' | 'SUSPENDED' | 'INACTIVE';

interface CreateCustomerRequest {
  name: string;
  email: string;
  initialStatus?: CustomerStatus;
}

// ❌ 錯誤：使用 any，缺乏型別安全
interface Customer {
  id: any;
  name: any;
  email: any;
  status: any;
}
```

#### React 元件標準
```typescript
// ✅ 正確：功能型元件，清楚的 props 型別
interface CustomerListProps {
  customers: Customer[];
  onCustomerSelect: (customer: Customer) => void;
  loading?: boolean;
}

export const CustomerList: React.FC<CustomerListProps> = ({
  customers,
  onCustomerSelect,
  loading = false
}) => {
  const handleCustomerClick = useCallback((customer: Customer) => {
    onCustomerSelect(customer);
  }, [onCustomerSelect]);

  if (loading) {
    return <LoadingSpinner />;
  }

  return (
    <div className="customer-list">
      {customers.map(customer => (
        <CustomerCard
          key={customer.id}
          customer={customer}
          onClick={handleCustomerClick}
        />
      ))}
    </div>
  );
};

// ❌ 錯誤：類別元件，缺乏型別定義
class CustomerList extends React.Component {
  render() {
    return (
      <div>
        {this.props.customers.map(customer => (
          <div key={customer.id} onClick={() => this.props.onSelect(customer)}>
            {customer.name}
          </div>
        ))}
      </div>
    );
  }
}
```

### Angular 標準

#### 服務設計
```typescript
// ✅ 正確：Injectable 服務，明確的型別
@Injectable({
  providedIn: 'root'
})
export class CustomerService {
  private readonly apiUrl = '/../api/v1/customers';

  constructor(private http: HttpClient) {}

  getCustomers(): Observable<Customer[]> {
    return this.http.get<Customer[]>(this.apiUrl).pipe(
      catchError(this.handleError<Customer[]>('getCustomers', []))
    );
  }

  getCustomerById(id: string): Observable<Customer> {
    const url = `${this.apiUrl}/${id}`;
    return this.http.get<Customer>(url).pipe(
      catchError(this.handleError<Customer>(`getCustomer id=${id}`))
    );
  }

  private handleError<T>(operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {
      console.error(`${operation} failed: ${error.message}`);
      return of(result as T);
    };
  }
}
```

## 🔌 API 設計規範

### REST API 約定

#### URL 命名標準

遵循 RESTful 設計原則：
- 使用複數名詞表示資源
- 使用 HTTP 動詞表示操作
- 巢狀資源表示關聯關係
- 動作端點用於非 CRUD 操作

詳細的 API 設計規範請參考：[API 設計標準](coding-standards/api-design-standards.md)

#### HTTP 狀態碼標準
- **200 OK**: 成功的 GET、PUT、PATCH
- **201 Created**: 成功的 POST
- **204 No Content**: 成功的 DELETE
- **400 Bad Request**: 驗證錯誤、格式錯誤的請求
- **401 Unauthorized**: 需要認證
- **403 Forbidden**: 授權失敗
- **404 Not Found**: 資源未找到
- **409 Conflict**: 業務規則違反
- **422 Unprocessable Entity**: 語義驗證錯誤
- **500 Internal Server Error**: 系統錯誤

#### 請求/回應格式標準
```java
// 請求 DTO
public record CreateCustomerRequest(
    @NotBlank String name,
    @Email String email,
    @Valid AddressDto address
) {}

// 回應 DTO
public record CustomerResponse(
    String id,
    String name,
    String email,
    AddressDto address,
    Instant createdAt,
    Instant updatedAt
) {}

// 錯誤回應
public record ErrorResponse(
    String errorCode,
    String message,
    Map<String, Object> context,
    Instant timestamp,
    List<FieldError> fieldErrors
) {}
```

## 🗄️ 資料庫設計規範

### 表格命名約定
```sql
-- ✅ 正確：複數形式，snake_case
CREATE TABLE customers (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE customer_orders (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customers(id),
    order_date TIMESTAMP NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL
);

-- ❌ 錯誤：不一致的命名
CREATE TABLE Customer (
    ID UUID PRIMARY KEY,
    CustomerName VARCHAR(100),
    Email VARCHAR(255)
);
```

### 索引策略
```sql
-- 主鍵索引（自動建立）
-- 外鍵索引
CREATE INDEX idx_customer_orders_customer_id ON customer_orders(customer_id);

-- 查詢優化索引
CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_customers_status_created ON customers(status, created_at);

-- 複合索引用於複雜查詢
CREATE INDEX idx_orders_customer_date ON customer_orders(customer_id, order_date);
```

### JPA 實體設計
```java
// ✅ 正確：清楚的實體映射
@Entity
@Table(name = "customers")
public class Customer {
    
    @Id
    @Column(name = "id")
    private String id;
    
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CustomerStatus status;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    // 懶載入關聯
    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    private List<Order> orders = new ArrayList<>();
}
```

## 📝 文檔編寫規範

### 程式碼註釋標準

#### JavaDoc 標準
```java
/**
 * 管理客戶生命週期操作的服務。
 * 
 * 此服務處理客戶註冊、個人資料更新和帳戶管理。
 * 它與電子郵件服務整合以發送通知，並維護所有客戶操作的稽核軌跡。
 * 
 * @author Development Team
 * @since 1.0
 */
@Service
@Transactional
public class CustomerService {
    
    /**
     * 使用提供的資訊建立新的客戶帳戶。
     * 
     * 此方法執行以下操作：
     * 1. 驗證客戶資訊
     * 2. 檢查重複的電子郵件地址
     * 3. 建立客戶記錄
     * 4. 發送歡迎電子郵件
     * 5. 記錄註冊事件
     * 
     * @param command 包含所有必要資訊的客戶建立命令
     * @return 建立的客戶，包含生成的 ID 和時間戳
     * @throws EmailAlreadyExistsException 如果電子郵件已註冊
     * @throws ValidationException 如果客戶資訊無效
     */
    public Customer createCustomer(CreateCustomerCommand command) {
        // 實作邏輯...
    }
}
```

#### 內聯註釋標準
```java
public void processComplexBusinessLogic(Order order) {
    // 檢查高風險訂單需要額外驗證
    // 這包括來自特定地區或具有特定模式的訂單
    if (isHighRiskOrder(order)) {
        scheduleAdditionalVerification(order);
    }
    
    // 計算折扣時需要考慮客戶等級和促銷活動
    BigDecimal discount = calculateDiscount(order);
    order.applyDiscount(discount);
    
    // TODO: 實作動態定價邏輯 (JIRA-123)
    // FIXME: 處理庫存不足的情況 (BUG-456)
}
```

### Markdown 文檔標準

#### 文檔結構
```markdown
# 文檔標題

## 概述
簡要說明文檔的目的和範圍。

## 目錄
- 章節 1
- 章節 2

## 章節 1
詳細內容...

### 子章節 1.1
更詳細的內容...

## 程式碼範例
```java
// 程式碼範例
public class Example {
    // 實作...
}
```

## 相關資源
- 相關文檔 1
- 相關文檔 2

---
**最後更新**: 2025年1月21日  
**維護者**: Development Team  
**版本**: 1.0
```

## 🔍 程式碼審查指南

### 審查流程

#### Pull Request 要求
- [ ] **標題**: 清楚、描述性的標題，遵循格式：`[TYPE] 簡要描述`
  - 類型：`FEAT`, `FIX`, `REFACTOR`, `DOCS`, `TEST`, `CHORE`
- [ ] **描述**: 詳細說明變更內容和原因
- [ ] **連結問題**: 參考相關的 issues 或 user stories
- [ ] **測試**: 測試證據（單元測試、手動測試結果）
- [ ] **破壞性變更**: 如有任何破壞性變更，需清楚記錄
- [ ] **截圖**: 對於 UI 變更，包含前後對比截圖

#### 審查分配規則
- **最少審查者**: 需要 2 位審查者
- **必要審查者**:
  - 至少 1 位資深開發者
  - 受影響領域的領域專家
  - 安全相關變更需要安全審查者
- **審查時限**: 審查必須在 24 小時內完成
- **自我審查**: 作者必須先自我審查 PR

### 審查檢查清單

#### 功能需求
- [ ] **業務邏輯**: 程式碼正確實作需求
- [ ] **邊界情況**: 適當處理邊界情況和錯誤條件
- [ ] **輸入驗證**: 所有輸入都經過適當驗證
- [ ] **輸出正確性**: 輸出符合預期格式和內容
- [ ] **整合**: 與現有系統適當整合

#### 程式碼品質
- [ ] **可讀性**: 程式碼清楚且自文檔化
- [ ] **可維護性**: 程式碼易於修改和擴展
- [ ] **複雜度**: 方法和類別不過度複雜
- [ ] **命名**: 變數、方法和類別有意義的名稱
- [ ] **註釋**: 複雜邏輯有適當註釋

#### 架構和設計
- [ ] **設計模式**: 使用適當的設計模式
- [ ] **SOLID 原則**: 程式碼遵循 SOLID 原則
- [ ] **DDD 合規**: 遵循領域驅動設計原則
- [ ] **層次分離**: 跨層次適當的關注點分離
- [ ] **依賴**: 依賴適當管理和注入

### 回饋指南

#### 回饋分類
- **Must Fix**: 阻止合併的關鍵問題
- **Should Fix**: 應該解決的重要問題
- **Consider**: 改進建議
- **Nitpick**: 次要的風格或偏好問題
- **Praise**: 對良好實踐的正面回饋

#### 回饋範例
```markdown
## Must Fix
- **安全問題**: 第 45 行存在 SQL 注入漏洞。使用參數化查詢。
- **錯誤**: 第 23 行可能出現空指標異常。添加空值檢查。

## Should Fix
- **效能**: `getOrderSummaries()` 中的 N+1 查詢問題。考慮使用 JOIN FETCH。
- **錯誤處理**: 第 67 行的通用異常處理。使用具體異常。

## Consider
- **設計**: 考慮將此邏輯提取到單獨的服務中，以更好地分離關注點。
- **可讀性**: 此方法相當長。考慮分解為較小的方法。

## Nitpick
- **風格**: 考慮使用更描述性的變數名稱（例如 `customerList` 而不是 `list`）。

## Praise
- **良好實踐**: 測試資料建立使用建造者模式的優秀做法。
- **乾淨程式碼**: 結構良好的方法，具有清楚的單一職責。
```

## 🛠️ 工具和自動化

### 程式碼格式化工具

#### Java 工具配置
```xml
<!-- Checkstyle 配置 -->
<checkstyle>
    <module name="Checker">
        <module name="TreeWalker">
            <module name="NamingConventions"/>
            <module name="LineLength">
                <property name="max" value="120"/>
            </module>
            <module name="MethodLength">
                <property name="max" value="20"/>
            </module>
        </module>
    </module>
</checkstyle>
```

#### TypeScript 工具配置
```json
// .eslintrc.json
{
  "extends": [
    "@typescript-eslint/recommended",
    "prettier"
  ],
  "rules": {
    "@typescript-eslint/no-unused-vars": "error",
    "@typescript-eslint/explicit-function-return-type": "warn",
    "prefer-const": "error",
    "no-var": "error"
  }
}

// prettier.config.js
module.exports = {
  semi: true,
  trailingComma: 'es5',
  singleQuote: true,
  printWidth: 100,
  tabWidth: 2
};
```

### IDE 配置

#### IntelliJ IDEA 設定
```xml
<!-- .idea/codeStyles/Project.xml -->
<component name="ProjectCodeStyleConfiguration">
  <code_scheme name="Project">
    <JavaCodeStyleSettings>
      <option name="IMPORT_LAYOUT_TABLE">
        <value>
          <package name="java" withSubpackages="true" static="false"/>
          <package name="javax" withSubpackages="true" static="false"/>
          <emptyLine/>
          <package name="org" withSubpackages="true" static="false"/>
          <emptyLine/>
          <package name="com" withSubpackages="true" static="false"/>
          <emptyLine/>
          <package name="" withSubpackages="true" static="false"/>
        </value>
      </option>
    </JavaCodeStyleSettings>
  </code_scheme>
</component>
```

#### VS Code 設定
```json
// .vscode/settings.json
{
  "editor.formatOnSave": true,
  "editor.codeActionsOnSave": {
    "source.fixAll.eslint": true,
    "source.organizeImports": true
  },
  "typescript.preferences.importModuleSpecifier": "relative",
  "typescript.suggest.autoImports": true
}
```

### 自動化檢查

#### Pre-commit Hooks
```yaml
# .pre-commit-config.yaml
repos:
  - repo: https://github.com/pre-commit/pre-commit-hooks
    rev: v4.4.0
    hooks:
      - id: trailing-whitespace
      - id: end-of-file-fixer
      - id: check-yaml
      - id: check-json

  - repo: https://github.com/psf/black
    rev: 22.10.0
    hooks:
      - id: black
        language_version: python3

  - repo: local
    hooks:
      - id: checkstyle
        name: Checkstyle
        entry: ./gradlew checkstyleMain
        language: system
        pass_filenames: false
```

#### CI/CD 管道檢查
```yaml
# .github/workflows/code-quality.yml
name: Code Quality

on:
  pull_request:
    branches: [ main, develop ]

jobs:
  code-quality:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      
      - name: Run Checkstyle
        run: ./gradlew checkstyleMain
      
      - name: Run SpotBugs
        run: ./gradlew spotbugsMain
      
      - name: Run Tests
        run: ./gradlew test
      
      - name: Generate Test Report
        run: ./gradlew jacocoTestReport
      
      - name: Check Coverage
        run: ./gradlew jacocoTestCoverageVerification
```

## 📊 品質指標和門檻

### 品質門檻
- **程式碼覆蓋率**: 新程式碼最少 80% 行覆蓋率
- **複雜度**: 每個方法的循環複雜度 ≤ 10
- **重複**: 不允許 > 5 行的程式碼重複
- **安全**: 無高或關鍵安全漏洞
- **效能**: 無效能回歸

### 審查指標
- **審查時間**: 完成審查的平均時間
- **回饋品質**: 每次審查發現的問題數量
- **返工率**: 需要重大返工的 PR 百分比
- **批准率**: 首次審查即批准的 PR 百分比

## 🔗 相關資源

### 內部文檔
- [開發視點總覽](README.md)
- [架構設計標準](architecture/)
- [測試標準](testing/)
- [建置和部署](build-system/)

### 外部參考
- Google Java Style Guide
- Airbnb JavaScript Style Guide
- Clean Code
- Effective Java

---

**最後更新**: 2025年1月21日  
**維護者**: Development Team  
**版本**: 1.0

> 💡 **提示**: 編碼標準不是束縛，而是團隊協作的基礎。遵循這些標準能讓我們更高效地協作和維護程式碼。
