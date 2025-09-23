# 編碼標準

## 概覽

本目錄包含專案的編碼標準和最佳實踐，涵蓋 Java 後端、前端開發、API 設計和文檔編寫等各個方面。

## 編碼標準概要

### 核心原則
1. **一致性** - 整個專案保持一致的編碼風格
2. **可讀性** - 程式碼應該易於理解和維護
3. **簡潔性** - 避免不必要的複雜性
4. **安全性** - 遵循安全編碼實踐
5. **效能** - 考慮程式碼的效能影響

### 適用範圍
- Java 後端開發
- React/Next.js 前端開發
- Angular 前端開發
- API 設計和文檔
- 資料庫設計
- 測試程式碼

## 核心文檔

- **[API 設計標準](api-design-standards.md)** - REST API 設計規範和最佳實踐

## Java 編碼標準

### 命名規範
```java
// 類別名稱：PascalCase
public class CustomerService {
    
    // 常數：UPPER_SNAKE_CASE
    private static final String DEFAULT_CURRENCY = "TWD";
    
    // 變數和方法：camelCase
    private final CustomerRepository customerRepository;
    
    public Customer createCustomer(CreateCustomerCommand command) {
        // 實作
    }
}
```

### 程式碼組織
```java
// 1. 靜態匯入
import static org.assertj.core.api.Assertions.assertThat;

// 2. 標準庫匯入
import java.time.LocalDateTime;
import java.util.List;

// 3. 第三方庫匯入
import org.springframework.stereotype.Service;

// 4. 專案內部匯入
import solid.humank.genaidemo.domain.Customer;

@Service
public class CustomerService {
    // 1. 靜態變數
    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);
    
    // 2. 實例變數
    private final CustomerRepository customerRepository;
    
    // 3. 建構子
    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }
    
    // 4. 公開方法
    public Customer createCustomer(CreateCustomerCommand command) {
        // 實作
    }
    
    // 5. 私有方法
    private void validateCommand(CreateCustomerCommand command) {
        // 實作
    }
}
```

### 註解使用
```java
// DDD 註解
@AggregateRoot(name = "Customer", description = "客戶聚合根")
public class Customer {
    
    @ValueObject
    public record CustomerId(String value) {
        // 值對象實作
    }
}

// Spring 註解
@Service
@Transactional
public class CustomerApplicationService {
    // 應用服務實作
}

// 測試註解
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    
    @Mock
    private CustomerRepository customerRepository;
    
    @InjectMocks
    private CustomerService customerService;
}
```

## API 設計標準

詳細的 API 設計規範請參考：**[API 設計標準](api-design-standards.md)**

### 核心 API 原則
- RESTful 設計原則
- 統一的錯誤處理
- 版本控制策略
- 安全性最佳實踐

## 前端編碼標準

### React/Next.js 標準
```typescript
// 元件命名：PascalCase
interface CustomerListProps {
  customers: Customer[];
  onCustomerSelect: (customer: Customer) => void;
}

export const CustomerList: React.FC<CustomerListProps> = ({
  customers,
  onCustomerSelect
}) => {
  // Hook 使用
  const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null);
  
  // 事件處理函數：handleXxx
  const handleCustomerClick = useCallback((customer: Customer) => {
    setSelectedCustomer(customer);
    onCustomerSelect(customer);
  }, [onCustomerSelect]);
  
  return (
    <div className="customer-list">
      {customers.map(customer => (
        <CustomerCard
          key={customer.id}
          customer={customer}
          onClick={() => handleCustomerClick(customer)}
        />
      ))}
    </div>
  );
};
```

### Angular 標準
```typescript
// 服務命名：XxxService
@Injectable({
  providedIn: 'root'
})
export class CustomerService {
  private readonly apiUrl = '/api/v1/customers';
  
  constructor(private http: HttpClient) {}
  
  // 方法命名：動詞開頭
  getCustomers(): Observable<Customer[]> {
    return this.http.get<Customer[]>(this.apiUrl);
  }
  
  createCustomer(customer: CreateCustomerRequest): Observable<Customer> {
    return this.http.post<Customer>(this.apiUrl, customer);
  }
}

// 元件命名：XxxComponent
@Component({
  selector: 'app-customer-list',
  templateUrl: './customer-list.component.html',
  styleUrls: ['./customer-list.component.scss']
})
export class CustomerListComponent implements OnInit {
  customers: Customer[] = [];
  
  constructor(private customerService: CustomerService) {}
  
  ngOnInit(): void {
    this.loadCustomers();
  }
  
  private loadCustomers(): void {
    this.customerService.getCustomers().subscribe({
      next: (customers) => this.customers = customers,
      error: (error) => console.error('Failed to load customers', error)
    });
  }
}
```

## API 設計標準

### REST API 規範
```java
@RestController
@RequestMapping("/api/v1/customers")
@Validated
public class CustomerController {
    
    // GET 取得資源列表
    @GetMapping
    public ResponseEntity<Page<CustomerResponse>> getCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // 實作
    }
    
    // GET 取得單一資源
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable String id) {
        // 實作
    }
    
    // POST 創建資源
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {
        // 實作
    }
    
    // PUT 更新資源
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable String id,
            @Valid @RequestBody UpdateCustomerRequest request) {
        // 實作
    }
    
    // DELETE 刪除資源
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable String id) {
        // 實作
    }
}
```

### URL 設計規範
```
# 資源命名：複數名詞
GET    /api/v1/customers           # 取得客戶列表
GET    /api/v1/customers/{id}      # 取得特定客戶
POST   /api/v1/customers           # 創建客戶
PUT    /api/v1/customers/{id}      # 更新客戶
DELETE /api/v1/customers/{id}      # 刪除客戶

# 巢狀資源
GET    /api/v1/customers/{id}/orders    # 取得客戶的訂單
POST   /api/v1/customers/{id}/orders    # 為客戶創建訂單

# 動作資源
POST   /api/v1/orders/{id}/cancel       # 取消訂單
POST   /api/v1/orders/{id}/ship         # 出貨訂單
```

## 測試編碼標準

### 測試命名
```java
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    
    // 測試方法命名：should_expectedBehavior_when_condition
    @Test
    void should_create_customer_when_valid_command_provided() {
        // Given
        CreateCustomerCommand command = new CreateCustomerCommand("John Doe", "john@example.com");
        
        // When
        Customer result = customerService.createCustomer(command);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("John Doe");
    }
    
    @Test
    void should_throw_exception_when_email_already_exists() {
        // Given
        CreateCustomerCommand command = new CreateCustomerCommand("John Doe", "existing@example.com");
        when(customerRepository.existsByEmail("existing@example.com")).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> customerService.createCustomer(command))
            .isInstanceOf(EmailAlreadyExistsException.class)
            .hasMessage("Email already exists: existing@example.com");
    }
}
```

## 程式碼品質工具

### 靜態分析工具
- **Checkstyle** - 程式碼風格檢查
- **SpotBugs** - 潛在錯誤檢測
- **SonarQube** - 程式碼品質分析
- **ESLint** - JavaScript/TypeScript 程式碼檢查

### 格式化工具
- **Spotless** - Java 程式碼格式化
- **Prettier** - 前端程式碼格式化
- **EditorConfig** - 編輯器配置統一

### 配置範例
```gradle
// build.gradle
spotless {
    java {
        googleJavaFormat('1.15.0')
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

checkstyle {
    toolVersion = '10.3'
    configFile = file('config/checkstyle/checkstyle.xml')
}
```

## 相關資源

### 內部文檔
- [開發標準](../../../../.kiro/steering/development-standards.md)
- [程式碼審查標準](../../../../.kiro/steering/code-review-standards.md)
- [測試標準](../testing/README.md)

### 外部資源
- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- [Airbnb JavaScript Style Guide](https://github.com/airbnb/javascript)
- [Angular Style Guide](https://angular.io/guide/styleguide)

---

**維護者**: 開發團隊  
**最後更新**: 2025年1月21日  
**版本**: 1.0