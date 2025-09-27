# 快速入門指南

## 概覽

歡迎加入我們的開發團隊！本指南將幫助你快速設置開發環境，了解專案結構，並完成你的第一次貢獻。無論你是經驗豐富的開發者還是剛開始接觸我們的技術棧，這份指南都會為你提供所需的一切資訊。

## 📋 前置需求檢查清單

在開始之前，請確保你已經具備以下條件：

### 必要工具

#### Java 開發環境
- [ ] **Java 21** - OpenJDK 或 Oracle JDK
  ```bash
  # 檢查 Java 版本
  java -version
  # 應該顯示 Java 21.x.x
  ```

#### 前端開發環境
- [ ] **Node.js 18+** - 前端開發和工具鏈
  ```bash
  # 檢查 Node.js 版本
  node --version
  # 應該顯示 v18.x.x 或更高
  ```

#### 版本控制和容器化
- [ ] **Git** - 版本控制系統
  ```bash
  # 檢查 Git 版本
  git --version
  ```
- [ ] **Docker** - 容器化開發環境
  ```bash
  # 檢查 Docker 版本
  docker --version
  ```

#### 雲端工具
- [ ] **AWS CLI** - 雲端資源管理
  ```bash
  # 安裝 AWS CLI
  curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
  unzip awscliv2.zip
  sudo ./aws/install
  
  # 驗證安裝
  aws --version
  ```

### 推薦工具

#### 開發環境
- [ ] **IntelliJ IDEA Ultimate** - Java 開發 IDE（推薦）
  - 支援 Spring Boot、JPA、Cucumber
  - 內建 Git 整合和資料庫工具
- [ ] **VS Code** - 輕量級編輯器
  - 適合前端開發和文檔編輯
  - 豐富的擴充套件生態系統

#### API 和資料庫工具
- [ ] **Postman** 或 **Insomnia** - API 測試工具
- [ ] **DBeaver** - 資料庫管理工具
- [ ] **Kiro IDE** - AI 輔助開發工具

### 軟體安裝指南

#### 使用 SDKMAN 安裝 Java
```bash
# 安裝 SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# 安裝 Java 21
sdk install java 21.0.1-tem
sdk use java 21.0.1-tem

# 設為預設版本
sdk default java 21.0.1-tem

# 驗證安裝
java -version
javac -version
```

#### 使用 NVM 安裝 Node.js
```bash
# 安裝 NVM
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
source ~/.bashrc

# 安裝 Node.js 18
nvm install 18
nvm use 18
nvm alias default 18

# 驗證安裝
node --version
npm --version
```

#### Docker 安裝
```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install docker.io docker-compose

# macOS (使用 Homebrew)
brew install docker docker-compose

# 啟動 Docker 服務
sudo systemctl start docker
sudo systemctl enable docker

# 驗證安裝
docker --version
docker-compose --version
```

## ⚙️ 環境設置

### 1. 專案克隆和初始設置

```bash
# 克隆專案
git clone https://github.com/your-org/genai-demo.git
cd genai-demo

# 檢查專案結構
ls -la

# 設置 Git 配置
git config user.name "Your Name"
git config user.email "your.email@company.com"

# 安裝 Git hooks
cp scripts/pre-commit .git/hooks/
chmod +x .git/hooks/pre-commit
```

### 2. 後端環境設置

#### Gradle 建置和測試
```bash
# 檢查 Gradle 版本
./gradlew --version

# 清理並建置專案
./gradlew clean build

# 執行所有測試
./gradlew test

# 執行特定類型的測試
./gradlew unitTest           # 單元測試
./gradlew integrationTest    # 整合測試
./gradlew cucumber          # BDD 測試

# 生成測試報告
./gradlew jacocoTestReport

# 檢查程式碼品質
./gradlew checkstyleMain spotbugsMain
```

#### 應用啟動
```bash
# 使用預設 profile 啟動 (開發環境)
./gradlew bootRun

# 使用特定 profile 啟動
./gradlew bootRun --args='--spring.profiles.active=dev'

# 檢查應用是否正常啟動
curl http://localhost:8080/actuator/health
```

### 3. 前端環境設置

#### CMC 管理前端 (Next.js)
```bash
cd cmc-frontend

# 安裝依賴
npm install

# 啟動開發伺服器
npm run dev

# 建置生產版本
npm run build

# 執行測試
npm test

# 檢查程式碼品質
npm run lint
npm run type-check
```

#### 消費者前端 (Angular)
```bash
cd consumer-frontend

# 安裝依賴
npm install

# 啟動開發伺服器
npm start

# 建置生產版本
npm run build

# 執行測試
npm test

# 執行 E2E 測試
npm run e2e
```

### 4. 資料庫設置

#### 開發環境 (H2 內嵌資料庫)
```bash
# H2 資料庫會自動啟動，無需額外設置
# 可以通過以下 URL 訪問 H2 控制台
# http://localhost:8080/h2-console

# 連接資訊：
# JDBC URL: jdbc:h2:file:./data/devdb
# User Name: sa
# Password: (留空)
```

#### 本地 PostgreSQL (使用 Docker)
```bash
# 啟動 PostgreSQL 容器
docker run --name postgres-dev \
  -e POSTGRES_DB=genaidemo \
  -e POSTGRES_USER=dev \
  -e POSTGRES_PASSWORD=dev123 \
  -p 5432:5432 \
  -d postgres:15

# 執行資料庫遷移
./gradlew flywayMigrate

# 檢查資料庫連接
./gradlew flywayInfo
```

#### 使用 Docker Compose 啟動完整環境
```bash
# 啟動所有服務
docker-compose up -d

# 查看服務狀態
docker-compose ps

# 查看日誌
docker-compose logs -f

# 停止服務
docker-compose down
```

## 🏗️ 專案結構深度解析

### 整體架構
```
genai-demo/
├── app/                        # Spring Boot 主應用
│   ├── src/main/java/         # Java 源碼
│   │   └── solid/humank/genaidemo/
│   │       ├── domain/        # 領域層 (DDD 核心)
│   │       │   ├── customer/  # 客戶聚合
│   │       │   ├── order/     # 訂單聚合
│   │       │   └── shared/    # 共享核心
│   │       ├── application/   # 應用層 (用例實現)
│   │       │   ├── customer/  # 客戶用例
│   │       │   └── order/     # 訂單用例
│   │       └── infrastructure/ # 基礎設施層
│   │           ├── persistence/ # 資料持久化
│   │           ├── web/       # Web 控制器
│   │           └── messaging/ # 訊息處理
│   ├── src/test/              # 測試代碼
│   │   ├── java/             # Java 測試
│   │   └── resources/        # 測試資源
│   │       └── features/     # BDD 特性檔案
│   └── src/main/resources/   # 應用資源
│       ├── application.yml   # 應用配置
│       └── db/migration/     # 資料庫遷移腳本
├── cmc-frontend/              # CMC 管理前端
│   ├── src/                  # 源碼目錄
│   │   ├── app/             # Next.js 應用
│   │   ├── components/      # React 元件
│   │   ├── pages/           # 頁面路由
│   │   └── styles/          # 樣式檔案
│   ├── public/              # 靜態資源
│   └── tests/               # 前端測試
├── consumer-frontend/         # 消費者前端
│   ├── src/                 # Angular 源碼
│   │   ├── app/            # Angular 應用
│   │   ├── assets/         # 靜態資源
│   │   └── environments/   # 環境配置
│   └── e2e/                # E2E 測試
├── infrastructure/           # AWS CDK 基礎設施
│   ├── lib/                # CDK 構造
│   ├── bin/                # CDK 應用入口
│   └── test/               # 基礎設施測試
├── docs/                    # 專案文檔
│   ├── viewpoints/         # 架構視點文檔
│   │   ├── functional/     # 功能視點
│   │   ├── information/    # 資訊視點
│   │   ├── deployment/     # 部署視點
│   │   └── development/    # 開發視點
│   └── diagrams/           # 架構圖表
├── scripts/                # 自動化腳本
│   ├── build/             # 建置腳本
│   ├── deploy/            # 部署腳本
│   └── test/              # 測試腳本
└── .kiro/                 # Kiro IDE 配置
    ├── hooks/             # Git hooks
    └── steering/          # 開發指導原則
```

### 核心模組說明

#### 領域層 (Domain Layer)
- **聚合根 (Aggregate Roots)**: 業務實體的根，如 `Customer`, `Order`
- **值物件 (Value Objects)**: 不可變的業務概念，如 `Email`, `Money`
- **領域服務 (Domain Services)**: 跨聚合的業務邏輯
- **領域事件 (Domain Events)**: 業務事件的表示

#### 應用層 (Application Layer)
- **應用服務 (Application Services)**: 用例的協調者
- **命令和查詢 (Commands & Queries)**: CQRS 模式實現
- **事件處理器 (Event Handlers)**: 領域事件的處理

#### 基礎設施層 (Infrastructure Layer)
- **資料庫適配器**: JPA 實體和儲存庫實現
- **Web 適配器**: REST 控制器和 DTO
- **訊息適配器**: 事件發布和訂閱

## 🎯 第一次貢獻步驟指南

### 1. 選擇合適的任務

#### 新手友善的任務類型
- **文檔改進**: 修正錯字、更新過時資訊、增加範例
- **測試增強**: 增加測試覆蓋率、修正測試案例
- **程式碼重構**: 改善程式碼可讀性、提取重複邏輯
- **小功能實現**: 簡單的 CRUD 操作、驗證邏輯

#### 尋找任務的方式
```bash
# 查看 GitHub Issues
# 標籤篩選：good-first-issue, documentation, testing, refactoring

# 或者從程式碼品質改進開始
./gradlew checkstyleMain  # 查看程式碼風格問題
./gradlew spotbugsMain    # 查看潛在錯誤
./gradlew jacocoTestReport # 查看測試覆蓋率
```

### 2. 建立開發分支

```bash
# 確保在最新的 main 分支
git checkout main
git pull origin main

# 建立功能分支 (使用描述性名稱)
git checkout -b feature/add-customer-validation
# 或
git checkout -b fix/order-calculation-bug
# 或
git checkout -b docs/update-api-documentation
```

### 3. 遵循開發標準和最佳實踐

#### Java 編碼標準
```java
// ✅ 正確：清楚的類別和方法命名
@Service
@Transactional
public class CustomerRegistrationService {
    
    private final CustomerRepository customerRepository;
    private final EmailNotificationService emailNotificationService;
    
    public Customer registerNewCustomer(CustomerRegistrationRequest request) {
        validateRegistrationRequest(request);
        
        Customer customer = createCustomerFromRequest(request);
        Customer savedCustomer = customerRepository.save(customer);
        
        sendWelcomeEmail(savedCustomer);
        
        return savedCustomer;
    }
    
    private void validateRegistrationRequest(CustomerRegistrationRequest request) {
        if (isEmailAlreadyRegistered(request.getEmail())) {
            throw new EmailAlreadyRegisteredException(request.getEmail());
        }
    }
}

// ❌ 錯誤：不清楚的命名和結構
@Service
public class CustSvc {
    public Cust reg(CustReq req) {
        // 不清楚的實現
    }
}
```

#### API 設計規範
```java
// ✅ 正確：RESTful API 設計
@RestController
@RequestMapping("/../api/v1/customers")
public class CustomerController {
    
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {
        
        Customer customer = customerService.createCustomer(request);
        CustomerResponse response = CustomerResponse.from(customer);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable String id) {
        Customer customer = customerService.findById(id);
        CustomerResponse response = CustomerResponse.from(customer);
        
        return ResponseEntity.ok(response);
    }
}
```

#### 前端編碼標準 (React/TypeScript)
```typescript
// ✅ 正確：型別安全的 React 元件
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
```

### 4. 測試驅動開發 (TDD) 實踐

#### BDD 場景編寫
```gherkin
# src/test/resources/features/customer-registration.feature
Feature: Customer Registration
  As a new user
  I want to register for an account
  So that I can access the system

  Scenario: Successful customer registration
    Given I am a new customer with valid information
      | name          | John Doe           |
      | email         | john@example.com   |
      | password      | SecurePass123!     |
    When I submit the registration form
    Then I should receive a confirmation email
    And my account should be created successfully
    And I should be redirected to the welcome page

  Scenario: Registration with duplicate email
    Given a customer already exists with email "existing@example.com"
    When I try to register with the same email
    Then I should see an error message "Email already registered"
    And my account should not be created
```

#### 單元測試實現
```java
@ExtendWith(MockitoExtension.class)
class CustomerRegistrationServiceTest {
    
    @Mock
    private CustomerRepository customerRepository;
    
    @Mock
    private EmailNotificationService emailNotificationService;
    
    @InjectMocks
    private CustomerRegistrationService customerRegistrationService;
    
    @Test
    void should_create_customer_and_send_welcome_email_when_valid_request_provided() {
        // Given
        CustomerRegistrationRequest request = new CustomerRegistrationRequest(
            "John Doe",
            "john@example.com",
            "SecurePass123!"
        );
        
        Customer expectedCustomer = Customer.builder()
            .id("customer-123")
            .name("John Doe")
            .email("john@example.com")
            .build();
        
        when(customerRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(expectedCustomer);
        
        // When
        Customer result = customerRegistrationService.registerNewCustomer(request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("John Doe");
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        
        verify(customerRepository).save(any(Customer.class));
        verify(emailNotificationService).sendWelcomeEmail("john@example.com", "John Doe");
    }
    
    @Test
    void should_throw_exception_when_email_already_exists() {
        // Given
        CustomerRegistrationRequest request = new CustomerRegistrationRequest(
            "John Doe",
            "existing@example.com",
            "SecurePass123!"
        );
        
        when(customerRepository.existsByEmail("existing@example.com")).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> customerRegistrationService.registerNewCustomer(request))
            .isInstanceOf(EmailAlreadyRegisteredException.class)
            .hasMessage("Email already registered: existing@example.com");
        
        verify(customerRepository, never()).save(any(Customer.class));
        verify(emailNotificationService, never()).sendWelcomeEmail(anyString(), anyString());
    }
}
```

#### 整合測試
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class CustomerRegistrationIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Test
    void should_register_customer_successfully() {
        // Given
        CreateCustomerRequest request = new CreateCustomerRequest(
            "John Doe",
            "john@example.com",
            "SecurePass123!"
        );
        
        // When
        ResponseEntity<CustomerResponse> response = restTemplate.postForEntity(
            "/../api/v1/customers",
            request,
            CustomerResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("John Doe");
        
        // 驗證資料庫中的資料
        Optional<Customer> savedCustomer = customerRepository.findByEmail("john@example.com");
        assertThat(savedCustomer).isPresent();
        assertThat(savedCustomer.get().getName()).isEqualTo("John Doe");
    }
}
```

### 5. 程式碼提交和推送

#### 提交訊息規範
```bash
# 使用 Conventional Commits 格式
git add .

# 功能新增
git commit -m "feat(customer): add customer registration validation"

# 錯誤修正
git commit -m "fix(order): correct order total calculation logic"

# 文檔更新
git commit -m "docs(api): update customer API documentation"

# 測試增加
git commit -m "test(customer): add unit tests for customer service"

# 重構
git commit -m "refactor(order): extract order calculation logic"

# 推送到遠端分支
git push origin feature/add-customer-validation
```

### 6. 建立 Pull Request

#### PR 標題和描述範本
```markdown
## 📋 Pull Request 標題
[FEAT] Add customer registration validation

## 📝 描述
### 變更內容
- 新增客戶註冊時的電子郵件驗證
- 實作密碼強度檢查
- 增加重複電子郵件檢查

### 變更原因
- 提升系統安全性
- 防止無效資料進入系統
- 改善使用者體驗

### 測試
- [x] 單元測試已通過
- [x] 整合測試已通過
- [x] BDD 場景已驗證
- [x] 手動測試已完成

### 檢查清單
- [x] 程式碼遵循編碼標準
- [x] 所有測試都通過
- [x] 文檔已更新
- [x] 無破壞性變更
- [x] 已自我審查程式碼

### 相關 Issues
Closes #123
Related to #456

### 截圖 (如適用)
[包含 UI 變更的前後對比截圖]
```

#### PR 檢查清單
- [ ] **程式碼品質**: 通過所有靜態分析檢查
- [ ] **測試覆蓋**: 新程式碼有適當的測試覆蓋
- [ ] **文檔更新**: 相關文檔已更新
- [ ] **向後相容**: 沒有破壞現有功能
- [ ] **效能影響**: 評估對系統效能的影響
- [ ] **安全考量**: 檢查潛在的安全問題

## 🧪 測試執行指南

### 測試分層策略

#### 單元測試 (80% 覆蓋目標)
```bash
# 執行所有單元測試
./gradlew unitTest

# 執行特定類別的測試
./gradlew test --tests "CustomerServiceTest"

# 執行特定方法的測試
./gradlew test --tests "CustomerServiceTest.should_create_customer_successfully"

# 生成測試報告
./gradlew jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

#### 整合測試 (15% 覆蓋目標)
```bash
# 執行整合測試
./gradlew integrationTest

# 執行資料庫整合測試
./gradlew test --tests "*IntegrationTest"

# 執行 Web 層整合測試
./gradlew test --tests "*ControllerTest"
```

#### BDD 測試 (5% 覆蓋目標)
```bash
# 執行所有 BDD 測試
./gradlew cucumber

# 執行特定功能的 BDD 測試
./gradlew cucumber --tests "*CustomerRegistration*"

# 生成 BDD 報告
open build/reports/cucumber/index.html
```

#### 效能測試
```bash
# 執行效能測試
./gradlew performanceTest

# 生成效能報告
./gradlew generatePerformanceReport
open build/reports/performance/index.html
```

### 前端測試

#### React 測試 (Jest + Testing Library)
```bash
cd cmc-frontend

# 執行所有測試
npm test

# 執行特定測試檔案
npm test CustomerList.test.tsx

# 執行測試並生成覆蓋率報告
npm test -- --coverage

# 執行 E2E 測試
npm run e2e
```

#### Angular 測試 (Jasmine + Karma)
```bash
cd consumer-frontend

# 執行單元測試
npm test

# 執行 E2E 測試
npm run e2e

# 生成測試覆蓋率報告
npm run test:coverage
```

## 🔍 常見問題和故障排除

### 建置問題

#### Java 版本不符
```bash
# 問題：Java 版本不是 21
# 解決方案：
sdk list java
sdk use java 21.0.1-tem

# 驗證
java -version
./gradlew --version
```

#### Gradle 建置失敗
```bash
# 清理建置快取
./gradlew clean

# 重新整理依賴
./gradlew --refresh-dependencies

# 檢查依賴衝突
./gradlew dependencies

# 完整重建
./gradlew clean build
```

#### 記憶體不足問題
```bash
# 增加 Gradle 記憶體
export GRADLE_OPTS="-Xmx4g -XX:+UseG1GC"

# 或在 gradle.properties 中設置
echo "org.gradle.jvmargs=-Xmx4g -XX:+UseG1GC" >> gradle.properties
```

### 測試問題

#### 測試資料庫連接失敗
```bash
# 檢查 H2 資料庫檔案
ls -la data/

# 重置測試資料庫
rm -rf data/testdb*
./gradlew test
```

#### 測試間相互影響
```java
// 確保測試隔離
@Transactional
@Rollback
class CustomerServiceTest {
    
    @BeforeEach
    void setUp() {
        // 清理測試資料
        customerRepository.deleteAll();
    }
}
```

### 前端問題

#### Node.js 依賴衝突
```bash
# 清理 node_modules
rm -rf node_modules package-lock.json
npm install

# 或使用 npm ci 進行乾淨安裝
npm ci
```

#### 連接埠衝突
```bash
# 檢查連接埠使用情況
lsof -i :8080  # 後端
lsof -i :3000  # React
lsof -i :4200  # Angular

# 終止佔用連接埠的程序
kill -9 <PID>

# 或使用不同連接埠啟動
npm start -- --port 3001
```

### Docker 問題

#### 容器啟動失敗
```bash
# 檢查 Docker 服務狀態
sudo systemctl status docker

# 重啟 Docker 服務
sudo systemctl restart docker

# 清理 Docker 資源
docker system prune -a
```

#### 資料庫容器連接問題
```bash
# 檢查容器狀態
docker ps -a

# 查看容器日誌
docker logs postgres-dev

# 重新啟動容器
docker restart postgres-dev
```

## 📚 學習資源和進階指南

### 必讀文檔

#### 架構和設計
- DDD 領域驅動設計
- 六角架構實作
- SOLID 設計原則

#### 測試策略
- TDD 和 BDD 實踐
- 測試金字塔策略
- 效能測試指南

#### 技術棧
- Spring Boot 最佳實踐
- React 開發指南
- Angular 開發指南

### 推薦學習路徑

#### 第一週：基礎概念和環境熟悉
- [ ] 完成環境設置
- [ ] 熟悉專案結構
- [ ] 閱讀核心架構文檔
- [ ] 執行第一個測試
- [ ] 完成簡單的文檔修正

#### 第二週：領域驅動設計和架構模式
- [ ] 學習 DDD 戰術模式
- [ ] 理解六角架構原則
- [ ] 實作簡單的聚合根
- [ ] 編寫領域事件
- [ ] 完成小功能開發

#### 第三週：測試驅動開發
- [ ] 掌握 TDD 紅綠重構循環
- [ ] 編寫 BDD 場景
- [ ] 實作整合測試
- [ ] 學習測試替身使用
- [ ] 提升測試覆蓋率

#### 第四週：進階主題
- [ ] 了解微服務架構
- [ ] 學習 Saga 模式
- [ ] 實作 CQRS 模式
- [ ] 掌握事件溯源
- [ ] 參與程式碼審查

### 外部學習資源

#### 書籍推薦
- **Domain-Driven Design** by Eric Evans
- **Clean Architecture** by Robert C. Martin
- **Microservices Patterns** by Chris Richardson
- **Test Driven Development** by Kent Beck
- **Refactoring** by Martin Fowler

#### 線上課程
- [Spring Boot 官方指南](https://spring.io/guides)
- [React 官方教學](https://reactjs.org/tutorial/tutorial.html)
- [Angular 官方教學](https://angular.io/tutorial)
- [AWS 開發者指南](https://docs.aws.amazon.com/)

#### 社群資源
- [DDD Community](https://github.com/ddd-crew)
- [Spring Boot GitHub](https://github.com/spring-projects/spring-boot)
- [React GitHub](https://github.com/facebook/react)
- [Angular GitHub](https://github.com/angular/angular)

### 團隊協作和溝通

#### 溝通管道
- **Slack/Teams**: 日常溝通和快速問題
- **GitHub Issues**: 功能需求和錯誤報告
- **Pull Request**: 程式碼審查和討論
- **定期會議**: Sprint 規劃和回顧

#### 尋求幫助的最佳實踐
1. **先自己嘗試解決**: 查閱文檔、搜尋相關資源
2. **準備具體問題**: 包含錯誤訊息、重現步驟、預期結果
3. **選擇合適管道**: 緊急問題用即時通訊，複雜問題建立 Issue
4. **分享解決方案**: 將學到的知識回饋給團隊

#### 知識分享
- **技術分享會**: 定期分享新技術和最佳實踐
- **程式碼審查**: 透過審查學習和教學
- **文檔貢獻**: 改進和更新專案文檔
- **導師制度**: 資深開發者指導新成員

## 🎉 完成第一次貢獻後的下一步

### 慶祝成就
恭喜你完成了第一次貢獻！這是一個重要的里程碑。

### 持續改進
- **反思學習**: 回顧開發過程中的挑戰和收穫
- **收集回饋**: 從程式碼審查中學習改進點
- **設定目標**: 為下一個貢獻設定更具挑戰性的目標

### 進階貢獻機會
- **功能開發**: 參與更複雜的功能實作
- **架構改進**: 提出和實作架構優化
- **效能優化**: 識別和解決效能瓶頸
- **導師角色**: 幫助其他新成員入門

### 專業發展
- **技能提升**: 深入學習特定技術領域
- **認證考試**: 考慮相關的技術認證
- **會議參與**: 參加技術會議和研討會
- **開源貢獻**: 參與其他開源專案

---

**下一步**: [編碼標準與規範](coding-standards.md) →

> 💡 **提示**: 記住，每個專家都曾經是初學者。不要害怕提問，團隊很樂意幫助你成長。持續學習和實踐是成為優秀開發者的關鍵！

> 🎯 **目標**: 通過這份指南，你應該能夠獨立設置開發環境、理解專案結構、遵循開發標準，並成功完成你的第一次程式碼貢獻。
