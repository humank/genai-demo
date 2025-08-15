# 專案重構和 API 分組優化 - 2025-01-15

## 📋 變更概述

本次更新主要針對專案結構重整和 API 分組策略優化，提升專案的可維護性和 API 文檔的使用體驗。

## 🔄 主要變更

### 1. 專案檔案結構重整

#### 📁 新的目錄結構

```
genai-demo/
├── docker/                 # Docker 相關檔案
│   ├── docker-build.sh    # ARM64 映像構建腳本
│   ├── verify-deployment.sh # 部署驗證腳本
│   └── postgres/           # PostgreSQL 初始化腳本
├── deployment/             # 部署相關檔案
│   ├── k8s/               # Kubernetes 配置
│   ├── deploy-to-eks.sh   # EKS 部署腳本
│   └── aws-eks-architecture.md
├── scripts/                # 各種腳本檔案
│   ├── start-fullstack.sh # 啟動全棧應用
│   ├── stop-fullstack.sh  # 停止所有服務
│   ├── test-api.sh        # API 測試腳本
│   ├── verify-swagger-ui.sh # Swagger UI 驗證
│   └── generate_data.py   # 測試資料生成
├── tools/                  # 開發工具
│   └── plantuml.jar       # UML 圖表生成工具
└── docs/                   # 專案文檔 (擴充)
    ├── api/               # API 相關文檔
    ├── releases/          # 版本發布記錄
    └── ...
```

#### 🗂️ 移動的檔案

- **Docker 相關**: `docker-build.sh`, `verify-deployment.sh` → `docker/`
- **部署相關**: `deploy-to-eks.sh`, `k8s/`, `aws-eks-architecture.md` → `deployment/`
- **腳本檔案**: `start-fullstack.sh`, `stop-fullstack.sh`, `test-api.sh`, `verify-swagger-ui.sh`, `generate_data.py` → `scripts/`
- **工具檔案**: `plantuml.jar` → `tools/`
- **文檔檔案**: 各種 `.md` 檔案 → `docs/`

### 2. API 分組策略重新設計

#### 🎯 基於 DDD 和使用者角色的分組

**舊的分組方式**:

- `public-api`: 所有公開 API
- `internal-api`: 內部 API
- `management`: 管理端點

**新的分組方式**:

- `customer-api`: 客戶端 API (面向終端客戶)
- `operator-api`: 運營管理 API (面向平台運營者)
- `system-api`: 系統管理 API (面向系統管理員)

#### 📊 分組詳細說明

##### 客戶端 API (`customer-api`)

**目標使用者**: 終端客戶 (Customer)
**包含路徑**:

- `/api/products/**` - 商品瀏覽
- `/api/orders/**` - 個人訂單查詢
- `/api/payments/**` - 支付處理
- `/api/consumer/**` - 消費者功能
- `/api/shopping-cart/**` - 購物車
- `/api/promotions/**` - 促銷活動
- `/api/vouchers/**` - 優惠券
- `/api/reviews/**` - 商品評價
- `/api/recommendations/**` - 個人化推薦
- `/api/notifications/**` - 個人通知
- `/api/delivery-tracking/**` - 配送追蹤

##### 運營管理 API (`operator-api`)

**目標使用者**: 平台運營者 (Operator/Admin)
**包含路徑**:

- `/api/customers/**` - 客戶管理
- `/api/orders/**` - 全平台訂單管理
- `/api/products/**` - 商品管理 (CRUD)
- `/api/inventory/**` - 庫存管理
- `/api/pricing/**` - 定價策略
- `/api/payments/**` - 支付管理
- `/api/activities/**` - 系統活動記錄
- `/api/stats/**` - 統計報表
- `/api/admin/**` - 管理員專用功能

##### 系統管理 API (`system-api`)

**目標使用者**: 系統管理員、DevOps
**包含路徑**:

- `/api/internal/**` - 內部系統整合
- `/api/management/**` - 系統管理功能
- `/actuator/**` - Spring Boot Actuator

### 3. OpenAPI 標籤優化

#### 🏷️ 新的標籤體系

**客戶端標籤** (中文命名，更直觀):

- 產品瀏覽、購物車、訂單查詢、支付處理
- 促銷活動、商品評價、個人化推薦
- 通知中心、配送追蹤

**運營管理標籤**:

- 客戶管理、訂單管理、商品管理
- 庫存管理、定價管理、支付管理
- 統計報表、活動記錄

**系統管理標籤**:

- 系統監控、內部整合

### 4. 領域模型完善

#### 🏗️ DDD 架構增強

- **促銷規則引擎**: 完整實作 sealed interface 的促銷規則體系
- **購物車聚合**: 實作完整的購物車業務邏輯和優惠計算
- **客戶聚合**: 增強客戶模型，支援紅利點數和通知偏好
- **訂單聚合**: 完善訂單生命週期管理和狀態轉換

#### 🧪 測試體系建立

- **BDD 測試**: 實作消費者購物流程的行為驅動測試
- **架構測試**: 使用 ArchUnit 確保 DDD 架構合規性
- **整合測試**: 完整的 API 端點測試覆蓋
- **單元測試**: 領域邏輯和業務規則的單元測試

## 🔧 技術改進

### Docker 優化

- **ARM64 原生支援**: 針對 Apple Silicon 和 ARM64 伺服器優化
- **多階段構建**: 最小化最終映像大小
- **JVM 調優**: 容器環境專用的 JVM 參數
- **健康檢查**: 完整的應用程式監控機制

### 開發體驗提升

- **目錄說明文檔**: 每個新目錄都包含 README.md 說明
- **腳本執行權限**: 自動設定所有腳本的執行權限
- **路徑引用更新**: 更新所有文檔中的檔案路徑引用

## 📱 使用方式更新

### 新的腳本路徑

```bash
# Docker 相關
./docker/docker-build.sh
./docker/verify-deployment.sh

# 全棧應用
./scripts/start-fullstack.sh
./scripts/stop-fullstack.sh

# 測試和驗證
./scripts/test-api.sh
./scripts/verify-swagger-ui.sh

# 資料生成
python3 scripts/generate_data.py

# 部署
./deployment/deploy-to-eks.sh

# 工具
java -jar tools/plantuml.jar docs/uml/*.puml
```

### API 文檔訪問

```bash
# 客戶端 API 文檔
http://localhost:8080/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config#/customer-api

# 運營管理 API 文檔  
http://localhost:8080/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config#/operator-api

# 系統管理 API 文檔
http://localhost:8080/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config#/system-api
```

## 🎯 影響和效益

### 開發效率提升

- **清晰的目錄結構**: 開發者能快速找到相關檔案
- **功能分組**: 相關功能集中管理，降低維護成本
- **文檔完整性**: 每個目錄都有詳細的使用說明

### API 使用體驗改善

- **角色導向分組**: 不同使用者只看到相關的 API
- **中文標籤**: 更直觀的 API 分類和描述
- **智能路徑匹配**: 自動將 API 歸類到正確的分組

### 架構品質提升

- **DDD 合規性**: 通過架構測試確保設計原則
- **測試覆蓋**: 多層次的測試策略保證程式碼品質
- **容器化優化**: ARM64 原生支援和效能調優

## 🔄 遷移指南

### 對現有使用者的影響

1. **腳本路徑變更**: 需要更新 CI/CD 腳本中的路徑引用
2. **API 文檔訪問**: 可以使用新的分組 URL 獲得更好的體驗
3. **開發工具**: PlantUML 等工具路徑已變更

### 建議的遷移步驟

1. 更新本地腳本中的路徑引用
2. 重新構建 Docker 映像以獲得最新優化
3. 使用新的 API 分組 URL 訪問文檔
4. 檢查 CI/CD 流程中的路徑引用

## 📚 相關文檔

- [Docker 部署指南](../DOCKER_GUIDE.md)
- [API 版本管理策略](../api/API_VERSIONING_STRATEGY.md)
- [專案目錄結構說明](../../README.md#專案目錄結構)
- [SpringDoc 分組配置指南](../api/SPRINGDOC_GROUPING_GUIDE.md)

---

**發布日期**: 2025-01-15  
**版本**: v2.0.0  
**影響範圍**: 專案結構、API 文檔、開發工具
