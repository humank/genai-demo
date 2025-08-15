# GenAI Demo

> **Language / 語言選擇**  
> 🇺🇸 **English**: [English Documentation](docs/en/README.md)  
> 🇹🇼 **繁體中文**: 您正在閱讀繁體中文版本

這是一個基於領域驅動設計 (DDD) 和六角形架構 (Hexagonal Architecture) 的全棧示範專案，展示了如何構建一個具有良好架構和測試實踐的現代化應用程式。

## 🚀 快速開始

### 全棧應用啟動

```bash
# 啟動完整的前後端應用
./start-fullstack.sh

# 停止所有服務
./stop-fullstack.sh
```

### 單獨啟動服務

```bash
# 僅啟動後端 (Spring Boot)
./gradlew bootRun

# 僅啟動前端 (Next.js)
cd frontend && npm run dev
```

## 🏗️ 專案架構

### 後端架構 (六角形架構 + DDD)

本專案採用六角形架構（又稱端口與適配器架構）和領域驅動設計，將應用程序分為以下幾個主要層次：

1. **領域層 (Domain Layer)**
   - 包含業務核心邏輯和規則
   - 不依賴於其他層
   - 包含聚合根、實體、值對象、領域事件、領域服務和領域異常

2. **應用層 (Application Layer)**
   - 協調領域對象完成用戶用例
   - 只依賴於領域層
   - 包含應用服務、DTO、命令和查詢對象
   - 負責在介面層和領域層之間進行數據轉換

3. **基礎設施層 (Infrastructure Layer)**
   - 提供技術實現
   - 依賴於領域層，實現領域層定義的接口
   - 包含儲存庫實現、外部系統適配器、ORM 映射等
   - 按功能分為 persistence（持久化）和 external（外部系統）等子包

4. **介面層 (Interfaces Layer)**
   - 處理用戶交互
   - 只依賴於應用層，不直接依賴領域層
   - 包含控制器、視圖模型、請求/響應對象等
   - 使用自己的 DTO 與應用層交互

### 前端架構 (現代化 React 生態系統)

- **框架**: Next.js 14 with App Router
- **語言**: TypeScript
- **樣式**: Tailwind CSS + shadcn/ui 組件庫
- **狀態管理**: Zustand (全局狀態) + React Query (服務器狀態)
- **API 集成**: Axios 基於類型安全的 API 調用

## 🛠️ 技術棧

### 後端技術

- **核心框架**: Spring Boot 3.2.0
- **構建工具**: Gradle 8.x
- **數據庫**: H2 (開發) + Flyway (遷移管理)
- **API 文檔**: SpringDoc OpenAPI 3 + Swagger UI
- **測試框架**:
  - JUnit 5 - 單元測試
  - Cucumber 7 - BDD 測試
  - ArchUnit - 架構測試
  - Mockito - 模擬對象
  - Allure 2 - 測試報告與可視化
- **其他工具**:
  - Lombok - 減少樣板代碼
  - PlantUML - UML 圖表生成

### 前端技術

- **框架**: Next.js 14, React 18
- **語言**: TypeScript
- **樣式**: Tailwind CSS, PostCSS
- **UI 組件**: shadcn/ui, Radix UI, Lucide Icons
- **狀態管理**: Zustand, React Query
- **開發工具**: ESLint, Prettier, Hot Reload

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

- **Swagger UI**: <http://localhost:8080/swagger-ui.html>
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

# 健康檢查
GET /actuator/health              # 應用健康狀態

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

專案包含豐富的文檔，位於 `docs` 目錄下：

### 架構文檔

- [系統架構概覽](docs/architecture-overview.md) - 提供系統架構的高層次視圖
- [六角架構實現總結](docs/HexagonalArchitectureSummary.md) - 詳細說明六角形架構的實現
- [六角架構與 Event Storming 整合重構指南](docs/HexagonalRefactoring.MD) - Event Storming 重構指南
- [分層架構設計分析與建議](docs/LayeredArchitectureDesign.MD) - 分層架構分析

### 設計文檔

- [設計指南](docs/DesignGuideline.MD) - Tell, Don't Ask 原則和 DDD 戰術模式
- [系統開發與測試的設計遵循規範](docs/DesignPrinciple.md) - 設計規範
- [軟體設計經典書籍精要](docs/SoftwareDesignClassics.md) - 設計經典總結

### 代碼質量

- [代碼分析報告](docs/CodeAnalysis.md) - 基於《重構》原則的代碼分析
- [重構指南](docs/RefactoringGuidance.md) - 代碼重構技術和實踐

### 全棧應用文檔

- [全棧應用說明](FULLSTACK_README.md) - 前後端整合開發指南

## 🧪 測試

### 運行所有測試

```bash
./gradlew runAllTests                    # 運行所有測試
./gradlew runAllTestsWithReport         # 運行測試並生成 Allure 報告
```

### 運行特定類型測試

```bash
./gradlew test                          # 單元測試
./gradlew cucumber                      # BDD 測試
./gradlew testArchitecture             # 架構測試
```

### 測試報告

- **Cucumber HTML 報告**: `app/build/reports/cucumber/cucumber-report.html`
- **JUnit HTML 報告**: `app/build/reports/tests/test/index.html`
- **Allure 報告**: `app/build/reports/allure-report/allureReport/index.html`

### 架構測試

使用 ArchUnit 確保代碼遵循預定的架構規則：

- **DddArchitectureTest** - 確保遵循 DDD 分層架構
- **DddTacticalPatternsTest** - 確保正確使用 DDD 戰術模式
- **PackageStructureTest** - 確保包結構符合規範

### BDD 測試

使用 Cucumber 進行行為驅動開發測試，覆蓋：

- 訂單管理 (Order)
- 庫存管理 (Inventory)
- 支付處理 (Payment)
- 物流配送 (Delivery)
- 通知服務 (Notification)
- 完整訂單工作流 (Workflow)

## 🔧 開發工具

### 數據生成

```bash
python3 generate_data.py               # 生成大量測試數據
```

### 服務管理

```bash
./start-fullstack.sh                   # 啟動全棧應用
./stop-fullstack.sh                    # 停止所有服務
```

### 前端開發

```bash
cd frontend
npm install                             # 安裝依賴
npm run dev                            # 開發模式
npm run build                          # 生產構建
npm run lint                           # 代碼檢查
```

## 🎯 UML 圖表

本專案使用 PlantUML 生成各種 UML 圖表：

- 類別圖、對象圖、組件圖、部署圖
- 時序圖（訂單處理、定價處理、配送處理）
- 狀態圖、活動圖
- 領域模型圖、六角形架構圖、DDD分層架構圖

查看 [UML 文檔說明](docs/uml/README.md) 獲取更多信息。

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
cd frontend
rm -rf node_modules package-lock.json
npm install
```

## 🤝 貢獻

歡迎提交 Pull Request 或開 Issue 討論改進建議。

## 📄 授權

本專案採用 MIT 授權協議 - 詳見 [LICENSE](LICENSE) 文件。

## 🔗 相關連結

- **DeepWiki 整合**: [![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/humank/genai-demo)
- **前端應用**: <http://localhost:3000> (開發模式)
- **後端 API**: <http://localhost:8080>
- **Swagger UI**: <http://localhost:8080/swagger-ui.html>
- **OpenAPI 規範**: <http://localhost:8080/v3/api-docs>
- **H2 控制台**: <http://localhost:8080/h2-console>

---

## 🆕 最新更新 (2025年8月)

### OpenAPI 文檔系統完整實現

- ✅ **完整的 OpenAPI 3.0 規範** - 符合業界標準的 API 文檔
- ✅ **Swagger UI 整合** - 互動式 API 文檔界面
- ✅ **API 分組管理** - 公開 API、內部 API、管理端點分組
- ✅ **標準化錯誤回應** - 統一的錯誤處理格式
- ✅ **完整的 Schema 註解** - 詳細的請求/回應模型文檔
- ✅ **多環境配置** - 開發、測試、預發布、生產環境配置

### 產品管理系統完整實現

- ✅ **完整的產品 CRUD 操作** - 創建、讀取、更新、刪除
- ✅ **獨立的產品數據表** - 從庫存系統中分離產品管理
- ✅ **庫存調整功能** - 支持增加、減少、設定庫存
- ✅ **前後端完整整合** - React + Spring Boot 無縫對接
- ✅ **DDD 架構實現** - 遵循領域驅動設計原則
- ✅ **六角形架構** - 清晰的端口與適配器分離

### 技術改進

- 🔧 **JPA 實體重構** - 優化數據庫映射和查詢性能
- 🔧 **API 錯誤處理** - 完善的錯誤處理和用戶反饋
- 🔧 **前端狀態管理** - React Query 實現數據同步
- 🔧 **類型安全** - TypeScript 完整類型定義
- 🔧 **API 文檔自動化** - SpringDoc 自動生成 OpenAPI 規範

## 📈 專案統計

- **總代碼行數**: 20,000+ 行 (新增產品管理功能)
- **測試覆蓋率**: 高覆蓋率的單元測試和整合測試
- **業務數據**: 131 筆完整的業務記錄
- **API 端點**: 30+ 個 RESTful API (完整的業務功能覆蓋)
- **UI 組件**: 25+ 個可重用組件 (新增產品管理組件)
- **文檔頁面**: 20+ 個詳細文檔
- **數據庫遷移**: 14 個 Flyway 遷移腳本

這個專案展示了現代化全棧應用開發的最佳實踐，結合了 DDD、六角形架構、現代前端技術和完整的測試策略。最新的 OpenAPI 文檔系統和產品管理功能展示了如何在複雜業務場景下實現清晰的架構分離、完整的 API 文檔和優秀的用戶體驗。
