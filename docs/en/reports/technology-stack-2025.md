<!-- This document needs manual translation from Chinese to English -->
<!-- 此文檔需要從中文手動翻譯為英文 -->

# 技術棧詳細說明 (2025年8月)

## 🚀 技術棧概覽

GenAI Demo 專案採用現代化的技術棧，結合了最新的 Java 生態系統、現代前端框架和企業級開發工具。

## 🔧 後端技術棧

### 核心框架

#### Spring Boot 3.4.5

- **最新穩定版本**: 使用 Spring Boot 3.x 系列最新穩定版
- **Spring Framework 6.x**: 基於 Spring Framework 6.x
- **原生編譯支持**: 支援 GraalVM 原生映像編譯
- **可觀測性增強**: 內建 Micrometer 和 OpenTelemetry 支持

```gradle
implementation 'org.springframework.boot:spring-boot-starter-web'
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
implementation 'org.springframework.boot:spring-boot-starter-validation'
implementation 'org.springframework.boot:spring-boot-starter-actuator'
```

#### Java 21 LTS

- **最新 LTS 版本**: 長期支持版本，穩定可靠
- **預覽功能啟用**: 使用最新語言特性
- **Record 模式**: 大量使用 Java Record 減少樣板代碼
- **Pattern Matching**: 現代化的模式匹配語法
- **Virtual Threads**: 輕量級並發處理 (預覽功能)

```java
// Java 21 Record 範例
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        Objects.requireNonNull(amount, "金額不能為空");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("金額不能為負數");
        }
    }
}
```

### 構建工具

#### Gradle 8.x

- **現代化構建系統**: 比 Maven 更靈活和高效
- **Kotlin DSL 支持**: 類型安全的構建腳本
- **增量編譯**: 提升構建速度
- **依賴管理**: 強大的依賴解析和管理

```gradle
java {
    sourceCompatibility = '21'
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType(JavaCompile).configureEach {
    options.compilerArgs += '--enable-preview'
    options.release = 21
}
```

### 數據持久化

#### H2 Database

- **內存數據庫**: 快速開發和測試
- **SQL 兼容**: 標準 SQL 語法支持
- **Web 控制台**: 內建數據庫管理界面
- **零配置**: 無需額外安裝和配置

#### Flyway

- **數據庫版本管理**: 自動化數據庫遷移
- **版本控制**: 數據庫結構變更追蹤
- **團隊協作**: 確保數據庫結構一致性
- **生產就緒**: 支持生產環境部署

```sql
-- V1__Create_customer_table.sql
CREATE TABLE customers (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### API 文檔

#### SpringDoc OpenAPI 3

- **OpenAPI 3.0 規範**: 業界標準的 API 文檔格式
- **自動生成**: 基於註解自動生成文檔
- **Swagger UI 整合**: 互動式 API 文檔界面
- **API 分組**: 支持多組 API 文檔管理

```java
@RestController
@RequestMapping("/api/orders")
@Tag(name = "訂單管理", description = "訂單相關的 API 端點")
public class OrderController {
    
    @PostMapping
    @Operation(summary = "創建訂單", description = "創建新的訂單")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "訂單創建成功"),
        @ApiResponse(responseCode = "400", description = "請求參數錯誤")
    })
    public ResponseEntity<CreateOrderResponse> createOrder(
        @RequestBody @Valid CreateOrderRequest request) {
        // 實現邏輯
    }
}
```

### 測試框架

#### JUnit 5

- **現代化測試框架**: 比 JUnit 4 更強大和靈活
- **參數化測試**: 支持多種參數化測試方式
- **動態測試**: 運行時生成測試用例
- **擴展模型**: 靈活的擴展機制

```java
@Test
@DisplayName("應該在創建訂單時收集領域事件")
void should_collect_domain_event_when_creating_order() {
    // Given
    CustomerId customerId = CustomerId.of("CUST-001");
    List<OrderItem> items = List.of(
        new OrderItem(ProductId.of("PROD-001"), 1, Money.twd(999))
    );
    
    // When
    Order order = new Order(customerId, items);
    
    // Then
    assertThat(order.hasUncommittedEvents()).isTrue();
}
```

#### Cucumber 7

- **行為驅動開發**: BDD 測試框架
- **Gherkin 語法**: 業務可讀的測試場景
- **多語言支持**: 支持中文測試場景
- **豐富的報告**: HTML 和 JSON 格式報告

```gherkin
Feature: 訂單處理
  作為一個客戶
  我想要下訂單
  以便購買商品

  Scenario: 成功創建訂單
    Given 我是註冊客戶 "CUST-001"
    When 我下訂單包含商品 "PROD-001" 數量 1
    Then 訂單應該成功創建
    And 訂單總額應該是 999
```

#### ArchUnit

- **架構測試**: 確保代碼遵循架構規則
- **依賴檢查**: 驗證層間依賴關係
- **命名約定**: 檢查類和方法命名規範
- **DDD 合規性**: 驗證 DDD 模式實現

```java
@Test
@DisplayName("領域層不應依賴基礎設施層")
void domain_should_not_depend_on_infrastructure() {
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
        .check(classes);
}
```

#### Mockito

- **模擬對象框架**: 創建和管理模擬對象
- **行為驗證**: 驗證方法調用和參數
- **Stubbing**: 定義模擬對象行為
- **Spy 支持**: 部分模擬真實對象

#### Allure 2

- **測試報告**: 美觀的測試報告生成
- **多格式支持**: HTML、JSON 等格式
- **歷史趨勢**: 測試結果歷史追蹤
- **豐富的註解**: 詳細的測試描述

### 開發工具

#### Lombok

- **樣板代碼減少**: 自動生成 getter、setter 等
- **註解驅動**: 基於註解的代碼生成
- **IDE 支持**: 主流 IDE 都有插件支持
- **編譯時處理**: 不影響運行時性能

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerJpaEntity {
    @Id
    private String id;
    private String name;
    private String email;
}
```

#### PlantUML

- **UML 圖表生成**: 基於文本的 UML 圖表
- **多種圖表類型**: 類圖、時序圖、活動圖等
- **版本控制友好**: 文本格式便於版本控制
- **自動化生成**: 可集成到構建流程

## 🌐 前端技術棧

### 核心框架

#### Next.js 14

- **React 框架**: 基於 React 的全棧框架
- **App Router**: 新一代路由系統
- **Server Components**: 服務器端組件支持
- **自動優化**: 自動代碼分割和優化
- **TypeScript 支持**: 原生 TypeScript 支持

```typescript
// app/orders/page.tsx
export default async function OrdersPage() {
  const orders = await getOrders();
  
  return (
    <div className="container mx-auto p-4">
      <h1 className="text-2xl font-bold mb-4">訂單管理</h1>
      <OrderList orders={orders} />
    </div>
  );
}
```

#### React 18

- **並發功能**: Concurrent Features 支持
- **Suspense**: 數據獲取和代碼分割
- **自動批處理**: 性能優化
- **Hooks**: 現代化的狀態管理

#### TypeScript

- **類型安全**: 編譯時類型檢查
- **IDE 支持**: 優秀的開發體驗
- **重構友好**: 安全的代碼重構
- **團隊協作**: 提升代碼可維護性

```typescript
interface Order {
  id: string;
  customerId: string;
  totalAmount: number;
  status: OrderStatus;
  items: OrderItem[];
}

type OrderStatus = 'PENDING' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED';
```

### 樣式和 UI

#### Tailwind CSS

- **實用優先**: Utility-first CSS 框架
- **響應式設計**: 內建響應式支持
- **自定義主題**: 靈活的主題配置
- **生產優化**: 自動移除未使用的樣式

```tsx
<div className="bg-white shadow-md rounded-lg p-6 mb-4">
  <h2 className="text-xl font-semibold text-gray-800 mb-2">
    訂單 #{order.id}
  </h2>
  <p className="text-gray-600">
    總額: <span className="font-bold text-green-600">${order.totalAmount}</span>
  </p>
</div>
```

#### shadcn/ui

- **現代化組件庫**: 基於 Radix UI 的組件庫
- **可定制**: 完全可定制的組件
- **無障礙支持**: 內建無障礙功能
- **TypeScript**: 完整的 TypeScript 支持

```tsx
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export function OrderCard({ order }: { order: Order }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>訂單 #{order.id}</CardTitle>
      </CardHeader>
      <CardContent>
        <Button onClick={() => handleConfirm(order.id)}>
          確認訂單
        </Button>
      </CardContent>
    </Card>
  );
}
```

### 狀態管理

#### React Query (@tanstack/react-query)

- **服務器狀態管理**: 專門處理服務器狀態
- **緩存機制**: 智能的數據緩存
- **背景更新**: 自動背景數據更新
- **錯誤處理**: 內建錯誤處理機制

```typescript
function useOrders() {
  return useQuery({
    queryKey: ['orders'],
    queryFn: async () => {
      const response = await fetch('/api/orders');
      return response.json();
    },
    staleTime: 5 * 60 * 1000, // 5 分鐘
  });
}
```

#### Zustand

- **輕量級狀態管理**: 簡單的全局狀態管理
- **TypeScript 支持**: 完整的類型支持
- **中間件支持**: 豐富的中間件生態
- **DevTools**: 開發者工具支持

```typescript
interface AppState {
  user: User | null;
  setUser: (user: User | null) => void;
  theme: 'light' | 'dark';
  toggleTheme: () => void;
}

const useAppStore = create<AppState>((set) => ({
  user: null,
  setUser: (user) => set({ user }),
  theme: 'light',
  toggleTheme: () => set((state) => ({ 
    theme: state.theme === 'light' ? 'dark' : 'light' 
  })),
}));
```

### 表單處理

#### React Hook Form

- **高性能表單**: 最小重渲染
- **驗證支持**: 內建和自定義驗證
- **TypeScript**: 完整的類型支持
- **易於使用**: 簡潔的 API

```typescript
const { register, handleSubmit, formState: { errors } } = useForm<OrderForm>();

const onSubmit = (data: OrderForm) => {
  createOrder(data);
};

return (
  <form onSubmit={handleSubmit(onSubmit)}>
    <input
      {...register('customerName', { required: '客戶姓名為必填' })}
      placeholder="客戶姓名"
    />
    {errors.customerName && (
      <span className="text-red-500">{errors.customerName.message}</span>
    )}
  </form>
);
```

#### Zod

- **Schema 驗證**: TypeScript-first 的驗證庫
- **類型推導**: 自動類型推導
- **組合式驗證**: 靈活的驗證規則組合
- **錯誤處理**: 詳細的錯誤信息

```typescript
const orderSchema = z.object({
  customerName: z.string().min(1, '客戶姓名為必填'),
  email: z.string().email('請輸入有效的電子郵件'),
  items: z.array(z.object({
    productId: z.string(),
    quantity: z.number().min(1, '數量必須大於 0'),
  })).min(1, '至少需要一個商品'),
});

type OrderForm = z.infer<typeof orderSchema>;
```

## 🐳 容器化和部署

### Docker

- **容器化部署**: 一致的運行環境
- **ARM64 優化**: 支持 Apple Silicon 和 AWS Graviton
- **多階段構建**: 優化映像大小
- **健康檢查**: 內建健康檢查機制

```dockerfile
FROM openjdk:21-jdk-slim as builder
WORKDIR /app
COPY . .
RUN ./gradlew build -x test

FROM openjdk:21-jre-slim
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Compose

- **多容器編排**: 管理多個相關容器
- **網絡配置**: 自動網絡配置
- **卷管理**: 數據持久化
- **環境變量**: 靈活的配置管理

```yaml
version: '3.8'
services:
  genai-demo:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - JAVA_OPTS=-Xms256m -Xmx512m
    healthcheck:
      test: ["CMD-SHELL", "wget --spider http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
```

## 🔧 開發工具和流程

### 版本控制

- **Git**: 分散式版本控制
- **GitHub**: 代碼託管和協作
- **分支策略**: GitFlow 或 GitHub Flow

### IDE 支持

- **IntelliJ IDEA**: 推薦的 Java IDE
- **VS Code**: 輕量級編輯器，適合前端開發
- **插件支持**: Lombok、Spring Boot、React 等插件

### 代碼品質

- **ESLint**: JavaScript/TypeScript 代碼檢查
- **Prettier**: 代碼格式化
- **SonarQube**: 代碼品質分析 (可選)

### CI/CD

- **GitHub Actions**: 自動化構建和部署
- **Docker Hub**: 容器映像倉庫
- **自動化測試**: 每次提交自動運行測試

## 📊 技術選型理由

### 後端技術選型

| 技術 | 選擇理由 | 替代方案 |
|------|----------|----------|
| Java 21 | 最新 LTS，Record 支持 | Java 17, Kotlin |
| Spring Boot 3.4.5 | 成熟生態，企業級 | Quarkus, Micronaut |
| H2 Database | 快速開發，零配置 | PostgreSQL, MySQL |
| Gradle | 靈活構建，性能好 | Maven |
| JUnit 5 | 現代化測試框架 | TestNG |

### 前端技術選型

| 技術 | 選擇理由 | 替代方案 |
|------|----------|----------|
| Next.js 14 | 全棧框架，SEO 友好 | Create React App, Vite |
| TypeScript | 類型安全，開發體驗 | JavaScript |
| Tailwind CSS | 實用優先，快速開發 | Bootstrap, Material-UI |
| React Query | 服務器狀態管理專家 | SWR, Apollo Client |
| Zustand | 輕量級，簡單易用 | Redux, Context API |

## 🚀 性能優化

### 後端優化

- **JVM 調優**: 內存和垃圾回收優化
- **數據庫優化**: 索引和查詢優化
- **緩存策略**: Redis 緩存 (未來計劃)
- **異步處理**: 事件異步處理

### 前端優化

- **代碼分割**: 自動代碼分割
- **圖片優化**: Next.js 圖片優化
- **緩存策略**: React Query 緩存
- **Bundle 分析**: 包大小分析和優化

## 🔮 技術發展規劃

### 短期計劃 (1-3 個月)

- **Spring Boot 升級**: 升級到最新版本
- **性能監控**: 添加 APM 監控
- **安全增強**: OAuth2 認證授權

### 中期計劃 (3-6 個月)

- **微服務拆分**: 基於 DDD 邊界拆分
- **事件驅動**: 完整的事件驅動架構
- **API Gateway**: 統一 API 網關

### 長期計劃 (6-12 個月)

- **雲原生**: Kubernetes 部署
- **服務網格**: Istio 服務網格
- **AI 集成**: 機器學習和 AI 功能

這個技術棧展示了現代化企業級應用開發的最佳實踐，結合了穩定性、性能和開發效率的平衡。
