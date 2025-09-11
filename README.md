# GenAI Demo - 企業級電商平台示範專案

> **Language / 語言選擇**  
> 🇺🇸 **English**: [English Documentation](docs/en/README.md) | [English Project README](docs/en/PROJECT_README.md)  
> 🇹🇼 **繁體中文**: 您正在閱讀繁體中文版本

基於 DDD + 六角形架構的全棧電商平台，整合企業級可觀測性、AI 輔助開發和雲原生部署的現代化應用程式。

## 🌟 專案亮點

### 🏗️ 企業級架構設計

- **DDD + 六角形架構**: 聚合根 + 值對象 + 領域事件 + 規格模式 + 政策模式
- **事件驅動設計**: 完整的事件收集、發布和處理機制
- **Java 21 Record**: 減少 30-40% 樣板代碼，提升類型安全

### �  完整可觀測性系統 (v3.1.0 NEW!)

- **分散式追蹤**: AWS X-Ray + Jaeger 跨服務請求追蹤
- **結構化日誌**: 統一格式 + 關聯 ID + PII 遮罩
- **業務指標**: CloudWatch 自定義指標 + Prometheus 端點
- **成本優化**: 資源右調分析 + 即時成本追蹤

### 🤖 AI 輔助開發 (MCP 整合)

- **AWS 生態**: 文檔查詢 + CDK 指導 + 定價分析 + IAM 管理
- **GitHub 整合**: 程式碼審查 + 問題追蹤 + PR 管理
- **智能助手**: Well-Architected 審查 + 架構決策支援

### 🛒 雙前端業務功能

**消費者端**: 智能購物車 + 個人化推薦 + 會員紅利 + 配送追蹤  
**商務端**: 促銷管理 + 庫存管理 + 訂單處理 + 統計分析

### 🧪 測試與品質保證

- **測試驅動**: BDD + TDD + 架構測試，272 個測試 100% 通過
- **架構合規性**: 9.5/10 (六角形架構) + 9.5/10 (DDD 實踐)
- **雲原生部署**: AWS CDK + Kubernetes + GitOps

## 🚀 快速開始

### 記憶體和效能優化 (v3.0.1 新增)

本專案已針對編譯和測試期間的記憶體使用進行優化：

#### 🔧 優化配置

- **記憶體配置**: 編譯和測試最大堆記憶體增加至 4GB
- **日誌優化**: 測試期間只輸出 ERROR 級別日誌，大幅減少輸出量
- **JVM 優化**: 使用 G1 垃圾收集器和字串去重優化
- **並行處理**: 優化 Gradle 並行執行配置

#### 🛠️ 優化的執行腳本

```bash
# 優化的編譯 (減少日誌輸出，增加記憶體)
./scripts/build-optimized.sh

# 優化的測試執行 (只顯示錯誤日誌)
./scripts/run-tests-optimized.sh

# 記憶體使用監控
./scripts/monitor-memory.sh

# 系統資源檢查
./scripts/check-system-resources.sh
```

#### 🔍 可觀測性端點

```bash
# 應用監控
curl http://localhost:8080/actuator/health     # 健康檢查
curl http://localhost:8080/actuator/metrics    # 應用指標
curl http://localhost:8080/actuator/info       # 應用資訊

# 成本優化 API
curl http://localhost:8080/api/cost-optimization/recommendations  # 成本建議
curl http://localhost:8080/api/cost-optimization/analysis         # 成本分析
```

### 方式一：Docker 容器化部署 (推薦)

```bash
# 構建 ARM64 優化映像
./docker/docker-build.sh

# 啟動容器化環境
docker-compose up -d

# 查看服務狀態
docker-compose ps

# 停止所有服務
docker-compose down
```

**服務端點：**

- 🌐 **API 文檔**: <http://localhost:8080/swagger-ui/index.html>
- 🏥 **健康檢查**: <http://localhost:8080/actuator/health>
- 📊 **應用指標**: <http://localhost:8080/actuator/metrics>
- 💰 **成本優化**: <http://localhost:8080/api/cost-optimization/recommendations>
- 🗄️ **H2 資料庫控制台**: <http://localhost:8080/h2-console>

### 方式二：本地開發環境

```bash
# 啟動完整的前後端應用
./scripts/start-fullstack.sh

# 停止所有服務
./scripts/stop-fullstack.sh
```

### 方式三：單獨啟動服務

```bash
# 僅啟動後端 (Spring Boot)
./gradlew :app:bootRun

# 僅啟動前端 (Next.js)
cd cmc-frontend && npm run dev
```

## 🏗️ 架構設計

### 六角形架構 + DDD 分層

```
interfaces/ → application/ → domain/ ← infrastructure/
```

- **領域層**: 業務邏輯 + 聚合根 + 值對象 + 領域事件 + 規格模式
- **應用層**: 用例協調 + 事件發布 + 跨聚合操作
- **基礎設施層**: 持久化 + 外部系統 + 事件處理
- **介面層**: REST API + OpenAPI 3.0 + Swagger UI

## 📁 專案目錄結構

```
genai-demo/
├── app/                    # 主應用程式
│   ├── src/main/java/      # Java 原始碼
│   └── src/test/java/      # 測試程式碼
├── cmc-frontend/           # Next.js 14.2.30 前端應用 (CMC)
├── consumer-frontend/      # Angular 18.2.0 前端應用 (Consumer)
├── deployment/             # 部署相關檔案
│   ├── k8s/               # Kubernetes 配置
│   └── deploy-to-eks.sh   # EKS 部署腳本
├── docker/                 # Docker 相關檔案
│   ├── docker-build.sh    # 映像構建腳本
│   └── verify-deployment.sh # 部署驗證腳本
├── docs/                   # 專案文檔
│   ├── api/               # API 文檔
│   ├── en/                # 英文文檔
│   ├── architecture/      # 架構文檔
│   ├── diagrams/          # 圖表文檔 (Mermaid + PlantUML)
│   └── reports/           # 專案報告
├── scripts/                # 各種腳本檔案
│   ├── start-fullstack.sh # 啟動全棧應用
│   └── stop-fullstack.sh  # 停止所有服務
├── tools/                  # 開發工具
│   └── plantuml.jar       # UML 圖表生成工具
├── docker-compose.yml      # Docker Compose 配置
├── Dockerfile             # Docker 映像定義
└── README.md              # 專案說明文檔
```

### 雙前端架構

**CMC 管理端 (Next.js 14.2.30)**  
TypeScript + Tailwind CSS + shadcn/ui + Zustand + React Query

**消費者端 (Angular 18.2.0)**  
TypeScript + Tailwind CSS + PrimeNG + RxJS + Jasmine

## 🆕 版本更新 (v3.1.0 - 2025年9月)

### 🚀 主要新增功能

- ✅ **企業級可觀測性**: X-Ray + CloudWatch + Jaeger + 結構化日誌
- ✅ **MCP AI 助手**: AWS 生態 + GitHub 整合 + 智能開發指導
- ✅ **雲原生基礎設施**: AWS CDK + Kubernetes + GitHub Actions CI/CD
- ✅ **成本優化系統**: 資源右調 + 即時成本追蹤 + 優化建議

### 📈 架構與品質提升

- 🏗️ **六角形架構合規性**: 8.5/10 → 9.5/10
- 🔧 **Java Record 重構**: 22 個值對象，減少 30-40% 樣板代碼
- 🧪 **測試穩定性**: 272 個測試 100% 通過率
- �  **專案結構優化**: 檔案重組，清晰分類管理

## 🛠️ 技術棧

### 後端技術

- **核心框架**: Spring Boot 3.4.5
- **程式語言**: Java 21 (啟用預覽功能)
- **構建工具**: Gradle 8.x
- **數據庫**: H2 (開發) + PostgreSQL (生產) + Flyway (遷移管理)
- **API 文檔**: SpringDoc OpenAPI 3 + Swagger UI
- **可觀測性**:
  - Micrometer - 指標收集
  - AWS X-Ray - 分散式追蹤
  - Logback - 結構化日誌
  - Spring Boot Actuator - 健康檢查
- **測試框架**:
  - JUnit 5 - 單元測試
  - Cucumber 7.15.0 - BDD 測試
  - ArchUnit 1.3.0 - 架構測試
  - Mockito 5.8.0 - 模擬對象
  - Allure 2.22.1 - 測試報告與可視化
- **其他工具**:
  - Lombok 1.18.38 - 減少樣板代碼
  - PlantUML - UML 圖表生成

### 前端技術

**CMC 管理端**: Next.js 14.2.30 + TypeScript + Tailwind + shadcn/ui + Zustand + React Query  
**消費者端**: Angular 18.2.0 + TypeScript + Tailwind + PrimeNG + RxJS + Jasmine

## 📊 數據與 API

### 數據庫初始化

專案使用 Flyway 進行數據庫版本管理，包含豐富的業務測試數據：

- **100+ 產品庫存記錄** - 涵蓋電子產品、服裝、家居用品等
- **完整訂單流程數據** - 訂單、訂單項目、支付記錄
- **台灣本地化數據** - 真實地址、繁體中文產品名稱
- **多種支付方式** - 信用卡、數位錢包、銀行轉帳、貨到付款
- **獨立產品表** - 支持完整的產品生命週期管理

### API 文檔與端點

#### 📖 Swagger UI 文檔

- **Swagger UI**: <http://localhost:8080/swagger-ui/index.html>
- **OpenAPI 規範**: <http://localhost:8080/v3/api-docs>
- **API 分組**:
  - 公開 API: `/v3/api-docs/public-api`
  - 內部 API: `/v3/api-docs/internal-api`
  - 管理端點: `/v3/api-docs/management`

#### 🔧 主要 API 端點

```bash
# 產品管理 API
GET /api/products                 # 產品列表 (支持分頁)
GET /api/products/{productId}     # 獲取單個產品
PUT /api/products/{productId}     # 更新產品信息
DELETE /api/products/{productId}  # 刪除產品
POST /api/products                # 創建新產品

# 庫存管理 API
GET /api/inventory/{productId}    # 獲取產品庫存
POST /api/inventory/{productId}/adjust  # 調整庫存
POST /api/inventory/{productId}/reserve # 預留庫存
POST /api/inventory/{productId}/release # 釋放庫存

# 訂單管理 API
GET /api/orders                   # 訂單列表
GET /api/orders/{orderId}         # 獲取單個訂單
POST /api/orders                  # 創建新訂單
PUT /api/orders/{orderId}         # 更新訂單

# 支付管理 API
POST /api/payments                # 創建支付
GET /api/payments/{paymentId}     # 獲取支付詳情
PUT /api/payments/{paymentId}     # 更新支付狀態

# 成本優化 API
GET /api/cost-optimization/recommendations  # 獲取成本優化建議
GET /api/cost-optimization/analysis         # 獲取成本分析報告
POST /api/cost-optimization/right-sizing    # 執行資源右調分析

# 客戶管理 API
GET /api/customers                # 客戶列表
GET /api/customers/{customerId}   # 獲取客戶詳情

# 定價管理 API
GET /api/pricing/rules            # 獲取定價規則
POST /api/pricing/rules           # 創建定價規則
PUT /api/pricing/commission-rates # 更新佣金費率

# 數據統計 API
GET /api/stats                    # 總體數據統計
GET /api/stats/order-status       # 訂單狀態分布
GET /api/stats/payment-methods    # 支付方式分布
GET /api/stats/database           # 數據庫統計

# 活動記錄 API
GET /api/activities               # 系統活動記錄

# 健康檢查與監控
GET /actuator/health              # 應用健康狀態
GET /actuator/metrics             # 應用指標
GET /actuator/info                # 應用資訊
GET /actuator/prometheus          # Prometheus 指標

# H2 數據庫控制台
http://localhost:8080/h2-console  # 數據庫管理界面
```

## 📱 前端功能

### 主要頁面

- **儀表板** (`/`) - 系統概覽和統計數據
- **訂單管理** (`/orders`) - 訂單列表和詳情
  - 訂單詳情頁面 (`/orders/[orderId]`) - 完整訂單信息展示
- **產品管理** (`/products`) - 產品展示和庫存
  - 產品詳情頁面 (`/products/[productId]`) - 產品詳細信息和操作
  - 產品編輯功能 - 支持名稱、描述、價格、分類修改
  - 庫存調整功能 - 支持增加、減少、設定庫存數量
  - 產品刪除功能 - 安全的產品刪除操作
- **客戶管理** (`/customers`) - 客戶信息管理

### 產品管理功能

- ✏️ **產品編輯** - 完整的產品信息編輯界面
  - 產品名稱、描述修改
  - 價格和貨幣設定 (TWD, USD, EUR)
  - 產品分類管理 (電子產品、服飾、食品等)
- 📦 **庫存管理** - 靈活的庫存調整系統
  - 設定庫存 - 直接設定庫存數量
  - 增加庫存 - 進貨補充庫存
  - 減少庫存 - 損耗或退貨處理
  - 調整原因記錄 - 完整的庫存變動追蹤
- 🗑️ **產品刪除** - 安全的產品刪除功能
  - 確認對話框防止誤刪
  - 自動更新產品列表

### UI/UX 特色

- 🎨 現代化設計系統 (shadcn/ui + Tailwind CSS)
- 📱 完全響應式設計
- 🌙 深色/淺色主題支持
- ⚡ 實時數據更新 (React Query)
- 🔄 加載狀態和錯誤處理
- 📊 數據可視化圖表
- 🎯 直觀的操作界面
- 📝 表單驗證和用戶反饋
- 🔔 Toast 通知系統

## 📚 文檔

> **文檔中心**: [docs/README.md](docs/README.md) - 完整的文檔導航和分類

專案包含豐富的文檔，按功能分類組織：

### 🎯 快速導航

- **👨‍💼 專案經理**: [專案總結 2025](docs/reports/project-summary-2025.md) | [架構概覽](docs/diagrams/mermaid/architecture-overview.md)
- **🏗️ 架構師**: [架構文檔](docs/architecture/) | [圖表文檔](docs/diagrams/) | [設計文檔](docs/design/)
- **👨‍💻 開發者**: [開發指南](docs/development/) | [API 文檔](docs/api/) | [開發說明](docs/development/instructions.md)
- **🚀 DevOps**: [部署文檔](docs/deployment/) | [Docker 指南](docs/deployment/docker-guide.md)
- **🔍 可觀測性**: [可觀測性系統](docs/observability/) | [監控指南](docs/observability/README.md)
- **🤖 MCP 整合**: [MCP 指南](docs/mcp/) | [AI 輔助開發](docs/mcp/README.md)

### 📊 核心圖表 (Mermaid - GitHub 直接顯示)

- [🏗️ 系統架構概覽](docs/diagrams/mermaid/architecture-overview.md) - 完整的系統架構圖
- [🔵 六角形架構](docs/diagrams/mermaid/hexagonal-architecture.md) - 端口與適配器架構
- [🏛️ DDD 分層架構](docs/diagrams/mermaid/ddd-layered-architecture.md) - 領域驅動設計分層
- [⚡ 事件驅動架構](docs/diagrams/mermaid/event-driven-architecture.md) - 事件處理機制
- [🔌 API 交互圖](docs/diagrams/mermaid/api-interactions.md) - API 調用關係

### 📋 詳細 UML 圖表 (PlantUML)

- **結構圖**: 類圖、對象圖、組件圖、部署圖、包圖、複合結構圖
- **行為圖**: 用例圖、活動圖、狀態圖
- **交互圖**: 時序圖、通信圖、交互概覽圖、時間圖
- **Event Storming**: Big Picture、Process Level、Design Level

### 🏆 核心報告 (2025年9月更新)

- [📋 專案總結報告 2025](docs/reports/project-summary-2025.md) - 完整的專案成果和技術亮點總結
- [🏗️ 架構卓越性報告 2025](docs/reports/architecture-excellence-2025.md) - 詳細的架構評估和最佳實踐分析
- [🚀 技術棧詳細說明 2025](docs/reports/technology-stack-2025.md) - 完整的技術選型和實現細節
- [📝 文檔清理報告 2025](docs/reports/documentation-cleanup-2025.md) - 文檔重整和優化記錄

### 🛠️ 圖表生成工具

```bash
# 生成所有 PlantUML 圖表
./scripts/generate-diagrams.sh

# 生成特定圖表
./scripts/generate-diagrams.sh domain-model-class-diagram.puml

# 驗證圖表語法
./scripts/generate-diagrams.sh --validate
```

## 🧪 測試

### 運行所有測試

```bash
./gradlew runAllTests                    # 運行所有測試 (272 個測試)
./gradlew runAllTestsWithReport         # 運行測試並生成 Allure 報告
./gradlew runAllTestsComplete           # 運行完整測試套件
```

### 運行特定類型測試

```bash
./gradlew test                          # 單元測試 (JUnit 5)
./gradlew cucumber                      # BDD 測試 (Cucumber 7.15.0)
./gradlew testArchitecture             # 架構測試 (ArchUnit 1.3.0)
```

### 測試報告

- **Cucumber HTML 報告**: `app/build/reports/cucumber/cucumber-report.html`
- **JUnit HTML 報告**: `app/build/reports/tests/test/index.html`
- **Allure 報告**: `app/build/reports/allure-report/allureReport/index.html`
- **Allure 結果目錄**: `app/build/allure-results/`

### 架構測試

使用 ArchUnit 1.3.0 確保代碼遵循預定的架構規則：

- **DddEntityRefactoringArchitectureTest** - 確保 DDD 實體重構符合架構規範
- **六角形架構合規性** - 確保端口與適配器分離
- **DDD 戰術模式** - 確保正確使用聚合根、值對象、領域事件、規格模式、政策模式
- **包結構規範** - 確保包結構符合 DDD 分層架構
- **註解驗證** - 確保正確使用 `@AggregateRoot`、`@ValueObject`、`@Specification`、`@Policy` 等註解

### BDD 測試

使用 Cucumber 7.15.0 進行行為驅動開發測試，覆蓋：

- **消費者功能** (Consumer) - 購物旅程、購物車管理
- **客戶管理** (Customer) - 會員系統、紅利點數、會員折扣
- **訂單管理** (Order) - 訂單聚合根、訂單工作流
- **庫存管理** (Inventory) - 庫存管理
- **支付處理** (Payment) - 支付聚合根、支付折扣
- **物流配送** (Logistics) - 配送管理、配送系統
- **通知服務** (Notification) - 通知管理、通知服務
- **促銷活動** (Promotion) - 優惠券系統、閃購活動、超商優惠券、加購活動、贈品活動
- **產品管理** (Product) - 產品搜尋、產品組合
- **定價管理** (Pricing) - 佣金費率
- **完整工作流** (Workflow) - 端到端業務流程

## 🔧 開發工具

### 數據生成

```bash
python3 scripts/generate_data.py       # 生成大量測試數據
```

### 服務管理

```bash
./scripts/start-fullstack.sh           # 啟動全棧應用
./scripts/stop-fullstack.sh            # 停止所有服務
```

### 前端開發

#### CMC Frontend (Next.js)

```bash
cd cmc-frontend
npm install                             # 安裝依賴
npm run dev                            # 開發模式 (http://localhost:3002)
npm run build                          # 生產構建
npm run lint                           # 代碼檢查
npm run type-check                     # TypeScript 類型檢查
npm test                               # 運行測試
npm run test:e2e                       # E2E 測試 (Playwright)
```

#### Consumer Frontend (Angular)

```bash
cd consumer-frontend
npm install                             # 安裝依賴
npm start                              # 開發模式 (http://localhost:3001)
npm run build                          # 生產構建
npm test                               # 運行測試 (Jasmine + Karma)
```

## 🎯 UML 圖表

本專案使用 PlantUML 生成各種 UML 圖表：

- 類別圖、對象圖、組件圖、部署圖
- 時序圖（訂單處理、定價處理、配送處理）
- 狀態圖、活動圖
- 領域模型圖、六角形架構圖、DDD分層架構圖

查看 [圖表文檔說明](docs/diagrams/README.md) 獲取更多信息。

## 🚨 常見問題

### 配置緩存問題

```bash
./gradlew --no-configuration-cache <task>
```

### Allure 報告問題

```bash
./gradlew clean
./gradlew runAllTestsWithReport
```

### 前端依賴問題

```bash
cd cmc-frontend
rm -rf node_modules package-lock.json
npm install
```

## 🤝 貢獻

歡迎提交 Pull Request 或開 Issue 討論改進建議。

## 📄 授權

本專案採用 MIT 授權協議 - 詳見 [LICENSE](LICENSE) 文件。

## 🔗 相關連結

- **DeepWiki 整合**: [![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/humank/genai-demo)
- **Consumer 前端**: <http://localhost:3001> (開發模式)
- **CMC 前端**: <http://localhost:3002> (開發模式)
- **後端 API**: <http://localhost:8080>
- **Swagger UI**: <http://localhost:8080/swagger-ui/index.html>
- **OpenAPI 規範**: <http://localhost:8080/v3/api-docs>
- **H2 控制台**: <http://localhost:8080/h2-console>

---

## � 專案統計與價值

### 📈 核心數據

- **代碼規模**: 200,000+ 行高品質代碼
- **測試品質**: 272 個測試，100% 通過率
- **API 覆蓋**: 35+ 個 RESTful API 端點
- **UI 組件**: 25+ 個可重用組件 (React + Angular)
- **文檔完整性**: 80+ 個詳細文檔頁面
- **數據庫**: 131 筆業務記錄 + 22 個 Flyway 遷移腳本

### 🏆 技術價值

- **架構卓越**: DDD + 六角形架構 + 事件驅動設計
- **品質保證**: BDD + TDD + 架構測試完整覆蓋
- **現代技術棧**: Java 21 + Spring Boot 3.4.5 + Next.js 14.2.30 + Angular 18.2.0
- **企業級特性**: 可觀測性 + AI 輔助開發 + 雲原生部署
- **最佳實踐**: 適合學習現代化企業級應用開發的理想參考專案
