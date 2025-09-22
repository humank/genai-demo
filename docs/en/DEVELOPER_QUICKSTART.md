
# Guidelines

## 🎯 5 分鐘快速啟動

### 1. Environment檢查

```bash
# Tools
java --version    # 需要 21+
node --version    # 需要 18+
npm --version
git --version
```

### 2. 專案設置

```bash
# 克隆專案
git clone https://github.com/humank/genai-demo.git
cd genai-demo

# 安裝根目錄依賴
npm install

# 後端設置
cd app
./gradlew build

# 前端設置
cd ../consumer-frontend
npm install

cd ../cmc-frontend
npm install
```

### 3. 啟動開發Environment

```bash
# 終端 1: 後端
cd app
./gradlew bootRun

# 終端 2: 消費者前端
cd consumer-frontend
npm start

# 終端 3: 管理前端 (可選)
cd cmc-frontend
npm run dev
```

### 4. 驗證安裝

- 後端 API: <http://localhost:8080/actuator/health>
- 消費者前端: <http://localhost:4200>
- 管理前端: <http://localhost:3000>
- API 文檔: <http://localhost:8080/swagger-ui.html>

## 🏗️ 開發工作流程

### 新功能開發

1. **創建功能分支**

   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **遵循 DDD 架構**
   - Domain Layer: 業務邏輯和規則
   - Application Layer: 用例協調
   - Infrastructure Layer: 技術實現
   - Interface Layer: API 和 UI

3. **Test-Driven Development (TDD)**

   ```bash
   # Testing
   ./gradlew unitTest
   
   # Testing
   ./gradlew integrationTest
   ```

4. **代碼品質檢查**

   ```bash
   # 後端代碼檢查
   ./gradlew check
   
   # 前端代碼檢查
   npm run lint
   ```

### 常用開發Command

```bash
# 後端開發
./gradlew bootRun                    # 啟動應用
./gradlew test                       # Testing
./gradlew build                      # 構建專案
./gradlew clean build               # 清理並重新構建

# 前端開發 (Angular)
npm start                           # 開發服務器
npm run build                       # 生產構建
npm run test                        # Testing
npm run lint                        # 代碼檢查

# 前端開發 (Next.js)
npm run dev                         # 開發服務器
npm run build                       # 生產構建
npm run start                       # 生產服務器
```

## Testing

### Testing

- **Unit Test (80%)**: 快速，隔離的業務邏輯測試
- **Integration Test (15%)**: 組件間交互測試
- **End-to-End Test (5%)**: 完整用戶流程測試

### Testing

```bash
# 按速度分類
./gradlew quickTest              # < 2 分鐘，日常開發
./gradlew preCommitTest          # < 5 分鐘，提交前
./gradlew fullTest               # < 30 分鐘，發布前

# 按類型分類
./gradlew unitTest               # Testing
./gradlew integrationTest        # Testing
./gradlew e2eTest               # Testing
```

## 📊 Observability開發

### 當前狀態

- ✅ **基礎Monitoring**: Spring Boot Actuator
- ✅ **結構化Logging**: 統一格式和關聯 ID
- ✅ **前端Tracing**: 用戶行為分析
- 🚧 **WebSocket**: 前端就緒，後端計劃中
- 🚧 **Analytics**: 部分 API 可用

### 添加MonitoringMetrics

```java
// 業務Metrics範例
@Component
public class OrderMetrics {
    private final Counter ordersCreated;
    
    public OrderMetrics(MeterRegistry registry) {
        this.ordersCreated = Counter.builder("orders.created")
            .description("Total orders created")
            .register(registry);
    }
    
    public void recordOrderCreated() {
        ordersCreated.increment();
    }
}
```

### 結構化Logging

```java
// 使用結構化Logging
log.info("Order processed successfully", 
    kv("orderId", order.getId()),
    kv("customerId", order.getCustomerId()),
    kv("amount", order.getTotalAmount()));
```

## Tools

### IDE 設置 (推薦)

- **IntelliJ IDEA**: 完整的 Java 和 Spring Boot 支援
- **VS Code**: 輕量級，適合前端開發
- **Kiro IDE**: AI 輔助開發和代碼審查

### 有用的插件

- **SonarLint**: 代碼品質檢查
- **GitLens**: Git 歷史和責任Tracing
- **Spring Boot Tools**: Spring Boot 開發支援
- **Angular Language Service**: Angular 開發支援

## 🐛 常見問題解決

### 後端問題

1. **Port衝突**

   ```bash
   # 查找佔用Port的進程
   lsof -i :8080
   # 或更改Port
   ./gradlew bootRun --args='--server.port=8081'
   ```

2. **Repository連接問題**

   ```bash
   # 檢查 H2 控制台
   http://localhost:8080/h2-console
   ```

### 前端問題

1. **依賴衝突**

   ```bash
   rm -rf node_modules package-lock.json
   npm install
   ```

2. **編譯錯誤**

   ```bash
   # Angular
   ng build --verbose
   
   # Next.js
   npm run build -- --debug
   ```

## Resources

### Design

- [DDD 實踐指南](../design/ddd-guide.md)
- [Hexagonal Architecture說明](../diagrams/hexagonal-architecture.md)
- \1

### Standards

- [代碼審查標準](../../.kiro/steering/code-review-standards.md)
- [開發標準](../../.kiro/steering/development-standards.md)
- [安全標準](../../.kiro/steering/security-standards.md)

### API 文檔

- [後端 API](http://localhost:8080/swagger-ui.html)
- [前端組件庫](http://localhost:4200/storybook)

---

**快速求助**: 檢查 [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) 或 \1
