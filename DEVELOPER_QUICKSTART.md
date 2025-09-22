# 開發者快速入門指南

## 🎯 5 分鐘快速啟動

### 1. 環境檢查

```bash
# 檢查必要工具
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

### 3. 啟動開發環境

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

3. **測試驅動開發**

   ```bash
   # 單元測試 (快速反饋)
   ./gradlew unitTest
   
   # 整合測試 (提交前)
   ./gradlew integrationTest
   ```

4. **代碼品質檢查**

   ```bash
   # 後端代碼檢查
   ./gradlew check
   
   # 前端代碼檢查
   npm run lint
   ```

### 常用開發命令

```bash
# 後端開發
./gradlew bootRun                    # 啟動應用
./gradlew test                       # 運行測試
./gradlew build                      # 構建專案
./gradlew clean build               # 清理並重新構建

# 前端開發 (Angular)
npm start                           # 開發服務器
npm run build                       # 生產構建
npm run test                        # 運行測試
npm run lint                        # 代碼檢查

# 前端開發 (Next.js)
npm run dev                         # 開發服務器
npm run build                       # 生產構建
npm run start                       # 生產服務器
```

## 🧪 測試策略

### 測試金字塔

- **單元測試 (80%)**: 快速，隔離的業務邏輯測試
- **整合測試 (15%)**: 組件間交互測試
- **端到端測試 (5%)**: 完整用戶流程測試

### 測試分類

```bash
# 按速度分類
./gradlew quickTest              # < 2 分鐘，日常開發
./gradlew preCommitTest          # < 5 分鐘，提交前
./gradlew fullTest               # < 30 分鐘，發布前

# 按類型分類
./gradlew unitTest               # 單元測試
./gradlew integrationTest        # 整合測試
./gradlew e2eTest               # 端到端測試
```

## 📊 可觀測性開發

### 當前狀態

- ✅ **基礎監控**: Spring Boot Actuator
- ✅ **結構化日誌**: 統一格式和關聯 ID
- ✅ **前端追蹤**: 用戶行為分析
- 🚧 **WebSocket**: 前端就緒，後端計劃中
- 🚧 **Analytics**: 部分 API 可用

### 添加監控指標

```java
// 業務指標範例
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

### 結構化日誌

```java
// 使用結構化日誌
log.info("Order processed successfully", 
    kv("orderId", order.getId()),
    kv("customerId", order.getCustomerId()),
    kv("amount", order.getTotalAmount()));
```

## 🔧 開發工具配置

### IDE 設置 (推薦)

- **IntelliJ IDEA**: 完整的 Java 和 Spring Boot 支援
- **VS Code**: 輕量級，適合前端開發
- **Kiro IDE**: AI 輔助開發和代碼審查

### 有用的插件

- **SonarLint**: 代碼品質檢查
- **GitLens**: Git 歷史和責任追蹤
- **Spring Boot Tools**: Spring Boot 開發支援
- **Angular Language Service**: Angular 開發支援

## 🐛 常見問題解決

### 後端問題

1. **端口衝突**

   ```bash
   # 查找佔用端口的進程
   lsof -i :8080
   # 或更改端口
   ./gradlew bootRun --args='--server.port=8081'
   ```

2. **資料庫連接問題**

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

## 📚 學習資源

### 架構和設計

- [DDD 實踐指南](docs/viewpoints/development/architecture/ddd-domain-driven-design.md)
- [六角形架構說明](docs/viewpoints/development/architecture/hexagonal-architecture.md)
- [事件驅動設計](docs/architecture/event-driven-design.md)

### 開發標準

- <!-- Kiro 配置連結: <!-- Kiro 配置連結: **代碼審查標準** (請參考專案內部文檔) --> -->
- <!-- Kiro 配置連結: <!-- Kiro 配置連結: **開發標準** (請參考專案內部文檔) --> -->
- <!-- Kiro 配置連結: <!-- Kiro 配置連結: **安全標準** (請參考專案內部文檔) --> -->

### API 文檔

- [後端 API](http://localhost:8080/swagger-ui.html)
- [前端組件庫](http://localhost:4200/storybook)

---

**快速求助**: 檢查 [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) 或 [故障排除文檔](docs/troubleshooting/README.md)
