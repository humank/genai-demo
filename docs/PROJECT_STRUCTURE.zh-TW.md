# GenAI Demo Project Structure

!Infrastructure Status
!Tests
!CDK
!Architecture

## 🏗️ Overall Architecture

This is a full-stack microservices e-commerce platform using Domain-Driven Design (DDD) and hexagonal architecture, with complete cloud infrastructure and production-grade monitoring.

```
genai-demo/
├── 🔧 Build and Configuration
│   ├── build.gradle              # Root-level Gradle configuration (multi-module management)
│   ├── settings.gradle           # Gradle settings
│   ├── gradle.properties         # Gradle properties
│   ├── gradlew / gradlew.bat     # Gradle Wrapper
│   └── gradle/                   # Gradle Wrapper files
│
├── 🚀 Application Modules
│   ├── app/                      # Spring Boot backend (Java 21)
│   │   ├── src/main/java/        # Main source code (DDD architecture)
│   │   ├── src/test/java/        # Test code
│   │   ├── src/main/resources/   # Configuration files
│   │   ├── src/test/resources/   # Test configuration
│   │   └── build.gradle          # Java module build configuration
│   │
│   ├── cmc-frontend/             # Management frontend (Next.js + TypeScript)
│   │   ├── src/                  # React components and pages
│   │   ├── public/               # Static assets
│   │   ├── package.json          # Node.js dependencies
│   │   └── next.config.js        # Next.js configuration
│   │
│   └── consumer-frontend/        # Consumer frontend (Angular + TypeScript)
│       ├── src/                  # Angular components and services
│       ├── public/               # Static assets
│       ├── package.json          # Node.js dependencies
│       └── angular.json          # Angular configuration
│
├── 🏗️ 基礎設施 (已完成整合)
│   ├── infrastructure/           # 統一的 AWS CDK 基礎設施 (TypeScript)
│   │   ├── bin/                  # CDK 應用入口點
│   │   │   └── infrastructure.ts # 主要 CDK 應用 (6 個協調的堆疊)
│   │   ├── src/                  # CDK 源碼
│   │   │   ├── stacks/           # 堆疊定義 (Network, Security, Core, etc.)
│   │   │   ├── constructs/       # 可重用的 CDK 構造
│   │   │   ├── config/           # 環境配置
│   │   │   └── utils/            # 工具函數
│   │   ├── test/                 # Complete test suite (103 tests)
│   │   │   ├── unit/             # Unit tests (26 tests)
│   │   │   ├── integration/      # Integration tests (8 tests)
│   │   │   ├── consolidated-stack.test.ts # Main test suite (18 tests)
│   │   │   └── cdk-nag-suppressions.test.ts # Compliance tests (4 tests)
│   │   ├── docs/                 # Infrastructure documentation
│   │   ├── deploy-consolidated.sh # Unified deployment script
│   │   ├── status-check.sh       # Status check script
│   │   ├── package.json          # Node.js dependencies and scripts
│   │   └── cdk.json              # CDK configuration
│   │
│   └── k8s/                      # Kubernetes configuration files
│       ├── manifests/            # K8s YAML files
│       └── deploy-to-eks.sh      # EKS deployment script
│
├── 📚 Documentation and Tools
│   ├── docs/                     # Project documentation
│   │   ├── architecture/         # Architecture documentation
│   │   ├── api/                  # API documentation
│   │   ├── development/          # Development guides
│   │   └── deployment/           # Deployment guides
│   │
│   ├── scripts/                  # Development and operations scripts
│   │   ├── start-*.sh            # Startup scripts
│   │   ├── test-*.sh             # Testing scripts
│   │   └── setup-*.sh            # Setup scripts
│   │
│   └── logs/                     # Application logs
│       ├── backend.log           # Backend logs
│       ├── cmc-frontend.log      # Management frontend logs
│       └── frontend.log          # Consumer frontend logs
│
├── 🔧 Development Tool Configuration
│   ├── .kiro/                    # Kiro IDE configuration
│   │   ├── steering/             # Development guidance rules
│   │   ├── hooks/                # Automation hooks
│   │   └── specs/                # Feature specifications
│   │
│   ├── .github/                  # GitHub Actions CI/CD
│   │   └── workflows/            # Workflow definitions
│   │
│   ├── .vscode/                  # VS Code configuration
│   ├── docker-compose.yml        # Local development environment
│   └── Dockerfile                # Containerization configuration
│
└── 📄 Project Files
    ├── README.md                 # Project documentation
    ├── CHANGELOG.md              # Change log
    ├── LICENSE                   # License terms
    └── .gitignore                # Git ignore rules
```

## 🎯 **Module Responsibilities**

### **app/** - Java Backend

- **Tech Stack**: Spring Boot 3.3.5 + Java 21
- **Architecture**: DDD + Hexagonal Architecture + CQRS
- **Functions**: API services, business logic, data persistence
- **Build**: Gradle
- **Testing**: JUnit 5 + Cucumber + ArchUnit

### **cmc-frontend/** - Management Frontend

- **Tech Stack**: Next.js 14 + React 18 + TypeScript
- **Functions**: Content management, order management, user management
- **Build**: npm/yarn
- **Users**: Administrators, customer service staff

### **consumer-frontend/** - Consumer Frontend

- **Tech Stack**: Angular 18 + TypeScript
- **Functions**: Product browsing, shopping cart, order processing
- **Build**: npm/yarn + Angular CLI
- **Users**: End consumers

### **infrastructure/** - Unified Infrastructure ✅

- **Tech Stack**: AWS CDK v2 + TypeScript 5.6+
- **Architecture**: 6 coordinated stacks (Network, Security, Alerting, Core, Observability, Analytics)
- **Functions**: Complete cloud infrastructure, monitoring, security, compliance
- **Build**: npm + CDK CLI
- **Deployment**: Unified CloudFormation deployment
- **Testing**: 103 tests (100% pass rate)
- **Status**: ✅ Production ready

## 🎉 **Infrastructure Integration Completed** (December 2024)

### **Major Milestones**

✅ **Unified Deployment**: Integrated 3 separate CDK applications into 1 unified application  
✅ **Complete Testing**: 103 tests all passed, covering all core functionality  
✅ **CDK v2 Compliance**: Using latest CDK v2.208.0+ and modern patterns  
✅ **Security Validation**: CDK Nag compliance checks passed, meeting AWS security best practices  
✅ **Production Ready**: Complete monitoring, alerting, and observability configuration  

### **Infrastructure Architecture**

```
Unified CDK Application (infrastructure/)
├── NetworkStack        # VPC, subnets, security groups
├── SecurityStack       # KMS 密鑰、IAM 角色
├── AlertingStack       # SNS 主題、通知
├── CoreInfrastructureStack # ALB、計算資源
├── ObservabilityStack  # CloudWatch、監控
└── AnalyticsStack      # 數據湖、分析 (可選)
```

### **測試覆蓋**

- **單元測試**: 26 個 (組件級測試)
- **集成測試**: 8 個 (跨堆疊驗證)
- **主測試套件**: 18 個 (核心功能)
- **合規測試**: 4 個 (安全驗證)
- **其他測試**: 47 個 (堆疊驗證)
- **總計**: **103 個測試，100% 通過率**

## 🚀 **開發命令**

### **後端開發**

```bash
./gradlew :app:bootRun              # 啟動後端服務
./gradlew :app:test                 # 運行所有測試
./gradlew :app:unitTest             # 快速單元測試
./gradlew :app:integrationTest      # 集成測試
./gradlew :app:cucumber             # BDD 測試
```

### **前端開發**

```bash
# CMC 管理前端
cd cmc-frontend
npm install && npm run dev          # 開發模式 (http://localhost:3000)
npm run build                       # 生產構建
npm test                           # 運行測試

# Consumer 消費者前端
cd consumer-frontend
npm install && npm start           # 開發模式 (http://localhost:4200)
npm run build                      # 生產構建
npm test                          # 運行測試
```

### **基礎設施管理** ✅

```bash
cd infrastructure

# 快速狀態檢查
npm run status                     # 檢查環境和基礎設施狀態

# 開發和測試
npm install                        # 安裝依賴
npm test                          # 運行所有測試 (103 個測試)
npm run test:quick                # 快速測試 (44 個核心測試)
npm run test:unit                 # 單元測試 (26 個)
npm run test:integration          # 集成測試 (8 個)
npm run test:compliance           # 合規測試 (4 個)

# CDK 操作
npm run synth                     # 合成 CloudFormation (6 個堆疊)
cdk list                          # 列出所有堆疊
cdk diff                          # 查看變更差異

# 部署選項
./deploy-consolidated.sh          # 統一部署 (推薦)
npm run deploy:dev                # 開發環境部署
npm run deploy:staging            # 預發布環境部署
npm run deploy:prod               # 生產環境部署
```

### **全棧開發**

```bash
./gradlew buildAll                 # 構建所有 Java 模組
./gradlew testAll                  # 運行所有 Java 測試
./gradlew devStart                 # 啟動後端開發環境
./scripts/start-fullstack.sh      # 啟動完整開發環境
```

## 📋 **目錄調整建議**

### ✅ **已完成的調整**

1. ✅ 刪除根目錄的 `bin/` 和 `build/` 目錄
2. ✅ 移除重複的 Eclipse 配置文件
3. ✅ 移動日誌文件到 `logs/` 目錄
4. ✅ 簡化 Gradle 多模組配置
5. ✅ **基礎設施完全整合** (2024年12月完成)
   - 統一 3 個分離的 CDK 應用為 1 個
   - 103 個測試全部通過
   - CDK v2 完全合規
   - 生產就緒的部署腳本

### 🔄 **建議的進一步調整**

1. **統一 IDE 配置**: 只在根目錄保留 IDE 配置
2. **標準化構建輸出**: 確保所有構建輸出都在各自的 `build/` 目錄中
3. **環境配置集中**: 考慮將環境配置集中管理

## 🎯 **項目狀態總結**

### **架構優勢**

當前的多模組配置是 **正確且高效的**：

- **根目錄 `build.gradle`**: 管理多模組項目，提供全局任務
- **`app/build.gradle`**: 專門處理 Java 後端的詳細配置
- **統一基礎設施**: 單一 CDK 應用管理所有雲端資源

### **技術成熟度**

| 模組 | 狀態 | 測試覆蓋 | 部署就緒 |
|------|------|----------|----------|
| Java 後端 | ✅ 穩定 | 高覆蓋 | ✅ 是 |
| CMC 前端 | ✅ 穩定 | 中等覆蓋 | ✅ 是 |
| Consumer 前端 | ✅ 穩定 | 中等覆蓋 | ✅ 是 |
| **基礎設施** | **✅ 完成** | **100% (103 測試)** | **✅ 生產就緒** |

### **架構優勢**

- 🔧 **技術棧分離**: 每種技術使用最適合的構建工具
- 👥 **團隊協作**: 不同技能的開發者可以專注於自己的模組
- 🚀 **獨立部署**: 各模組可以獨立構建和部署
- 📈 **可擴展性**: 未來可以輕鬆添加新的模組
- 🛡️ **安全合規**: CDK Nag 驗證，符合 AWS 最佳實踐
- 📊 **完整監控**: 內建監控、告警和可觀測性

### **快速開始**

```bash
# 檢查整體項目狀態
cd infrastructure && npm run status

# 啟動完整開發環境
./scripts/start-fullstack.sh

# 部署到雲端
cd infrastructure && ./deploy-consolidated.sh
```

這是一個 **現代化、生產就緒** 的全棧微服務架構，目錄結構合理且符合業界最佳實踐。基礎設施整合已完成，所有組件都已準備好用於生產環境。
