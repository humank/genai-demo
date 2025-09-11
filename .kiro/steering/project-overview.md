# 項目概覽與技術棧

## 項目簡介

GenAI Demo - 基於 DDD + 六角架構的全棧電商平台演示項目，整合企業級可觀測性和 AI 輔助開發

## 核心特性

- **智能購物車**: 多重促銷計算引擎
- **個性化推薦**: 基於購買歷史的產品推薦
- **會員積分系統**: 完整的積分累積與兌換機制
- **即時配送追蹤**: 實時配送狀態更新
- **訂單生命週期管理**: 完整的訂單處理流程
- **成本優化**: 即時成本追蹤和資源右調建議

## 技術架構亮點

- **領域驅動設計**: 完整的 DDD 戰術模式實現
- **六角架構**: 清晰的關注點分離
- **事件驅動架構**: 鬆耦合的領域事件機制
- **CQRS 模式**: 命令查詢職責分離
- **全面測試覆蓋**: BDD + TDD + 架構測試
- **企業級可觀測性**: 分散式追蹤 + 結構化日誌 + 業務指標
- **AI 輔助開發**: MCP 整合提供智能開發指導

## 技術棧

### 後端技術

- **Spring Boot 3.4.5** + **Java 21** + **Gradle 8.x**
- **Spring Data JPA** + **Hibernate** + **Flyway**
- **H2** (開發/測試) + **PostgreSQL** (生產)
- **SpringDoc OpenAPI 3** + **Swagger UI**
- **Spring Boot Actuator** (監控)
- **AWS X-Ray** (分散式追蹤) + **Micrometer** (指標)
- **Logback** (結構化日誌) + **CloudWatch** (日誌聚合)

### 前端技術

- **CMC 管理端**: Next.js 14 + React 18 + TypeScript + Tailwind CSS
- **消費者端**: Angular 18 + TypeScript + Tailwind CSS
- **UI 組件**: shadcn/ui + Radix UI
- **狀態管理**: React Query + Zustand

### 測試框架

- **JUnit 5** + **Mockito** + **AssertJ**
- **Cucumber 7** (BDD) + **Gherkin**
- **ArchUnit** (架構測試)
- **Allure 2** (測試報告)

### 開發工具

- **Lombok** (減少樣板代碼)
- **PlantUML** (UML 圖表生成)
- **Docker** + **Docker Compose**
- **AWS CDK** (基礎設施即程式碼)
- **GitHub Actions** (CI/CD 管道)
- **MCP 伺服器** (AI 輔助開發)

## 項目結構

```
genai-demo/
├── app/                    # Spring Boot 主應用
├── cmc-frontend/           # 商務管理中心 (Next.js)
├── consumer-frontend/      # 消費者應用 (Angular)
├── infrastructure/         # AWS CDK 基礎設施程式碼
├── deployment/             # Kubernetes 部署配置
├── docker/                 # Docker 構建腳本
├── docs/                   # 項目文檔
├── scripts/                # 開發部署腳本
└── .github/                # GitHub Actions CI/CD
```

## 開發環境端口

- **後端 API**: <http://localhost:8080>
- **Swagger UI**: <http://localhost:8080/swagger-ui/index.html>
- **H2 控制台**: <http://localhost:8080/h2-console>
- **健康檢查**: <http://localhost:8080/actuator/health>
- **應用指標**: <http://localhost:8080/actuator/metrics>
- **成本優化**: <http://localhost:8080/api/cost-optimization/recommendations>
- **CMC 前端**: <http://localhost:3002>
- **消費者前端**: <http://localhost:3001>

## 快速開始

```bash
# 啟動全棧服務
./scripts/start-fullstack.sh

# 僅啟動後端
./gradlew bootRun

# 運行測試
./gradlew runAllTests

# 構建 Docker 鏡像
./docker/docker-build.sh
```

詳細信息請參考：

- [架構設計](../../docs/architecture/)
- [開發指南](../../docs/development/)
- [部署文檔](../../docs/deployment/)
