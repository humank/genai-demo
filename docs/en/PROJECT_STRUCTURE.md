
# GenAI Demo 項目結構

![Infrastructure Status](https://img.shields.io/badge/Infrastructure-✅%20Production%20Ready-brightgreen)
![Tests](https://img.shields.io/badge/Tests-103%20Passing-brightgreen)
![CDK](https://img.shields.io/badge/CDK-v2.208.0+-blue)
![Architecture](https://img.shields.io/badge/Architecture-DDD%20%2B%20Hexagonal-orange)

## 🏗️ 整體架構

這是一個全棧微服務電商平台，採用Domain-Driven Design (DDD) 和六邊形架構，具備完整的雲端基礎設施和生產級Monitoring。

```
genai-demo/
├── 🔧 構建和配置
│   ├── build.gradle              # 根級 Gradle 配置 (多模組管理)
│   ├── settings.gradle           # Gradle 設置
│   ├── gradle.properties         # Gradle 屬性
│   ├── gradlew / gradlew.bat     # Gradle Wrapper
│   └── gradle/                   # Gradle Wrapper 文件
│
├── 🚀 應用模組
│   ├── app/                      # Spring Boot 後端 (Java 21)
│   │   ├── src/main/java/        # 主要源碼 (DDD 架構)
│   │   ├── src/test/java/        # Testing
│   │   ├── src/main/resources/   # 配置文件
│   │   ├── src/test/resources/   # Testing
│   │   └── build.gradle          # Java 模組構建配置
│   │
│   ├── cmc-frontend/             # 管理前端 (Next.js + TypeScript)
│   │   ├── src/                  # React 組件和頁面
│   │   ├── public/               # Resources
│   │   ├── package.json          # Node.js 依賴
│   │   └── next.config.js        # Next.js 配置
│   │
│   └── consumer-frontend/        # 消費者前端 (Angular + TypeScript)
│       ├── src/                  # Angular 組件和服務
│       ├── public/               # Resources
│       ├── package.json          # Node.js 依賴
│       └── angular.json          # Angular 配置
│
├── 🏗️ 基礎設施 (已完成整合)
│   ├── ../../infrastructure/           # 統一的 AWS CDK 基礎設施 (TypeScript)
│   │   ├── bin/                  # CDK 應用入口點
│   │   │   └── infrastructure.ts # 主要 CDK 應用 (6 個協調的Stack)
│   │   ├── src/                  # CDK 源碼
│   │   │   ├── stacks/           # Stack定義 (Network, Security, Core, etc.)
│   │   │   ├── constructs/       # 可重用的 CDK Construct
│   │   │   ├── config/           # Environment配置
│   │   │   └── utils/            # Tools
│   │   ├── test/                 # Testing
│   │   │   ├── unit/             # Testing
│   │   │   ├── integration/      # Testing
│   │   │   ├── consolidated-stack.test.ts # Testing
│   │   │   └── cdk-nag-suppressions.test.ts # Testing
│   │   ├── docs/                 # 基礎設施文檔
│   │   ├── deploy-consolidated.sh # Deployment
│   │   ├── status-check.sh       # 狀態檢查腳本
│   │   ├── package.json          # Node.js 依賴和腳本
│   │   └── cdk.json              # CDK 配置
│   │
│   └── k8s/                      # Kubernetes 配置文件
│       ├── manifests/            # K8s YAML 文件
│       └── deploy-to-eks.sh      # Deployment
│
├── 📚 文檔和工具
│   ├── docs/                     # 項目文檔
│   │   ├── architecture/         # 架構文檔
│   │   ├── api/                  # API 文檔
│   │   ├── development/          # Guidelines
│   │   └── deployment/           # Deployment
│   │
│   ├── scripts/                  # 開發和運維腳本
│   │   ├── start-*.sh            # 啟動腳本
│   │   ├── test-*.sh             # Testing
│   │   └── setup-*.sh            # 設置腳本
│   │
│   └── logs/                     # 應用Logging
│       ├── backend.log           # 後端Logging
│       ├── cmc-frontend.log      # 管理前端Logging
│       └── frontend.log          # 消費者前端Logging
│
├── 🔧 開發工具配置
│   ├── .kiro/                    # Kiro IDE 配置
│   │   ├── steering/             # 開發指導規則
│   │   ├── hooks/                # 自動化鉤子
│   │   └── specs/                # 功能規格
│   │
│   ├── .github/                  # GitHub Actions CI/CD
│   │   └── workflows/            # 工作流程定義
│   │
│   ├── .vscode/                  # VS Code 配置
│   ├── docker-compose.yml        # 本地開發Environment
│   └── Dockerfile                # Containerization配置
│
└── 📄 項目文件
    ├── README.md                 # 項目說明
    ├── CHANGELOG.md              # 變更Logging
    ├── ../../LICENSE                   # 授權條款
    └── .gitignore                # Git 忽略規則
```

## 🎯 **模組職責**

### **app/** - Java 後端

- **技術棧**: Spring Boot 3.3.5 + Java 21
- **架構**: DDD + 六邊形架構 + Command Query Responsibility Segregation (Command Query Responsibility Segregation (CQRS))
- **功能**: API 服務、業務邏輯、數據持久化
- **構建**: Gradle
- **測試**: JUnit 5 + Cucumber + ArchUnit

### **cmc-frontend/** - 管理前端

- **技術棧**: Next.js 14 + React 18 + TypeScript
- **功能**: 內容管理、訂單管理、用戶管理
- **構建**: npm/yarn
- **用戶**: 管理員、客服人員

### **consumer-frontend/** - 消費者前端

- **技術棧**: Angular 18 + TypeScript
- **功能**: 商品瀏覽、購物車、訂單處理
- **構建**: npm/yarn + Angular CLI
- **用戶**: 終端消費者

### **../../infrastructure/** - 統一基礎設施 ✅

- **技術棧**: AWS CDK v2 + TypeScript 5.6+
- **架構**: 6 個協調的Stack (Network, Security, Alerting, Core, Observability, Analytics)
- **功能**: 完整的雲端基礎設施、Monitoring、安全、合規
- **構建**: npm + CDK CLI
- **Deployment**: 統一 CloudFormation Deployment
- **測試**: 103 個測試 (100% 通過率)
- **狀態**: ✅ 生產就緒

## 🎉 **基礎設施整合完成** (2024年12月)

### **重大Milestone**

✅ **統一Deployment**: 從 3 個分離的 CDK 應用整合為 1 個統一應用  
✅ **完整測試**: 103 個測試全部通過，覆蓋所有核心功能  
✅ **CDK v2 合規**: 使用最新的 CDK v2.208.0+ 和現代化模式  
✅ **安全驗證**: CDK Nag 合規檢查通過，符合 AWS 安全Best Practice  
✅ **生產就緒**: 完整的Monitoring、告警和Observability配置  

### **基礎設施架構**

```
統一 CDK 應用 (../../infrastructure/)
├── NetworkStack        # VPC、子網、安全組
├── SecurityStack       # KMS 密鑰、IAM 角色
├── AlertingStack       # SNS 主題、通知
├── CoreInfrastructureStack # Resources
├── ObservabilityStack  # CloudWatch、Monitoring
└── AnalyticsStack      # 數據湖、分析 (可選)
```

### Testing

- **Unit Test**: 26 個 (組件級測試)
- **集成測試**: 8 個 (跨Stack驗證)
- **主測試套件**: 18 個 (核心功能)
- **合規測試**: 4 個 (安全驗證)
- **其他測試**: 47 個 (Stack驗證)
- **總計**: **103 個測試，100% 通過率**

## 🚀 **開發Command**

### **後端開發**

```bash
./gradlew :app:bootRun              # 啟動後端服務
./gradlew :app:test                 # Testing
./gradlew :app:unitTest             # Testing
./gradlew :app:integrationTest      # Testing
./gradlew :app:cucumber             # Testing
```

### **前端開發**

```bash
# CMC 管理前端
cd cmc-frontend
npm install && npm run dev          # 開發模式 (http://localhost:3000)
npm run build                       # 生產構建
npm test                           # Testing

# Consumer 消費者前端
cd consumer-frontend
npm install && npm start           # 開發模式 (http://localhost:4200)
npm run build                      # 生產構建
npm test                          # Testing
```

### **基礎設施管理** ✅

```bash
cd infrastructure

# 快速狀態檢查
npm run status                     # 檢查Environment和基礎設施狀態

# Testing
npm install                        # 安裝依賴
npm test                          # Testing
npm run test:quick                # Testing
npm run test:unit                 # Testing
npm run test:integration          # Testing
npm run test:compliance           # Testing

# CDK 操作
npm run synth                     # 合成 CloudFormation (6 個Stack)
cdk list                          # 列出所有Stack
cdk diff                          # 查看變更差異

# Deployment
./deploy-consolidated.sh          # Deployment
npm run deploy:dev                # Deployment
npm run deploy:staging            # Deployment
npm run deploy:prod               # Deployment
```

### **全棧開發**

```bash
./gradlew buildAll                 # 構建所有 Java 模組
./gradlew testAll                  # Testing
./gradlew devStart                 # 啟動後端開發Environment
./scripts/start-fullstack.sh      # 啟動完整開發Environment
```

## 📋 **目錄調整recommendations**

### ✅ **已完成的調整**

1. ✅ 刪除根目錄的 `bin/` 和 `build/` 目錄
2. ✅ 移除重複的 Eclipse 配置文件
3. ✅ 移動Logging文件到 `logs/` 目錄
4. ✅ 簡化 Gradle 多模組配置
5. ✅ **基礎設施完全整合** (2024年12月完成)
   - 統一 3 個分離的 CDK 應用為 1 個
   - 103 個測試全部通過
   - CDK v2 完全合規
   - 生產就緒的Deployment腳本

### 🔄 **recommendations的進一步調整**

1. **統一 IDE 配置**: 只在根目錄保留 IDE 配置
2. **標準化構建輸出**: 確保所有構建輸出都在各自的 `build/` 目錄中
3. **Environment配置集中**: 考慮將Environment配置集中管理

## 🎯 **項目狀態summary**

### **架構優勢**

當前的多模組配置是 **正確且高效的**：

- **根目錄 `build.gradle`**: 管理多模組項目，提供全局任務
- **`app/build.gradle`**: 專門處理 Java 後端的詳細配置
- **統一基礎設施**: 單一 CDK 應用管理所有雲端Resource

### **技術成熟度**

| 模組 | 狀態 | 測試覆蓋 | Deployment就緒 |
|------|------|----------|----------|
| Java 後端 | ✅ 穩定 | 高覆蓋 | ✅ 是 |
| CMC 前端 | ✅ 穩定 | 中等覆蓋 | ✅ 是 |
| Consumer 前端 | ✅ 穩定 | 中等覆蓋 | ✅ 是 |
| **基礎設施** | **✅ 完成** | **100% (103 測試)** | **✅ 生產就緒** |

### **架構優勢**

- 🔧 **技術棧分離**: 每種技術使用最適合的構建工具
- 👥 **團隊協作**: 不同技能的Developer可以專注於自己的模組
- 🚀 **獨立Deployment**: 各模組可以獨立構建和Deployment
- 📈 **Scalability**: 未來可以輕鬆添加新的模組
- 🛡️ **安全合規**: CDK Nag 驗證，符合 AWS Best Practice
- 📊 **完整Monitoring**: 內建Monitoring、告警和Observability

### **快速開始**

```bash
# 檢查整體項目狀態
cd infrastructure && npm run status

# 啟動完整開發Environment
./scripts/start-fullstack.sh

# Deployment
cd infrastructure && ./deploy-consolidated.sh
```

這是一個 **現代化、生產就緒** 的全棧Microservices Architecture，目錄結構合理且符合業界Best Practice。基礎設施整合已完成，所有組件都已準備好用於生產Environment。
