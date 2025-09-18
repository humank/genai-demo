# GenAI Demo 項目結構

## 🏗️ 整體架構

這是一個全棧微服務電商平台，採用領域驅動設計 (DDD) 和六邊形架構。

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
│   │   ├── src/test/java/        # 測試代碼
│   │   ├── src/main/resources/   # 配置文件
│   │   ├── src/test/resources/   # 測試配置
│   │   └── build.gradle          # Java 模組構建配置
│   │
│   ├── cmc-frontend/             # 管理前端 (Next.js + TypeScript)
│   │   ├── src/                  # React 組件和頁面
│   │   ├── public/               # 靜態資源
│   │   ├── package.json          # Node.js 依賴
│   │   └── next.config.js        # Next.js 配置
│   │
│   └── consumer-frontend/        # 消費者前端 (Angular + TypeScript)
│       ├── src/                  # Angular 組件和服務
│       ├── public/               # 靜態資源
│       ├── package.json          # Node.js 依賴
│       └── angular.json          # Angular 配置
│
├── 🏗️ 基礎設施
│   ├── infrastructure/           # AWS CDK 基礎設施即代碼 (TypeScript)
│   │   ├── lib/                  # CDK 堆疊定義
│   │   ├── test/                 # 基礎設施測試
│   │   ├── package.json          # Node.js 依賴
│   │   └── cdk.json              # CDK 配置
│   │
│   └── deployment/               # 部署腳本和 K8s 配置
│       ├── k8s/                  # Kubernetes YAML 文件
│       └── deploy-to-eks.sh      # EKS 部署腳本
│
├── 📚 文檔和工具
│   ├── docs/                     # 項目文檔
│   │   ├── architecture/         # 架構文檔
│   │   ├── api/                  # API 文檔
│   │   ├── development/          # 開發指南
│   │   └── deployment/           # 部署指南
│   │
│   ├── scripts/                  # 開發和運維腳本
│   │   ├── start-*.sh            # 啟動腳本
│   │   ├── test-*.sh             # 測試腳本
│   │   └── setup-*.sh            # 設置腳本
│   │
│   └── logs/                     # 應用日誌
│       ├── backend.log           # 後端日誌
│       ├── cmc-frontend.log      # 管理前端日誌
│       └── frontend.log          # 消費者前端日誌
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
│   ├── docker-compose.yml        # 本地開發環境
│   └── Dockerfile                # 容器化配置
│
└── 📄 項目文件
    ├── README.md                 # 項目說明
    ├── CHANGELOG.md              # 變更日誌
    ├── LICENSE                   # 授權條款
    └── .gitignore                # Git 忽略規則
```

## 🎯 **模組職責**

### **app/** - Java 後端

- **技術棧**: Spring Boot 3.3.5 + Java 21
- **架構**: DDD + 六邊形架構 + CQRS
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

### **infrastructure/** - 基礎設施

- **技術棧**: AWS CDK + TypeScript
- **功能**: 雲端資源定義、監控、安全配置
- **構建**: npm + CDK CLI
- **部署**: AWS CloudFormation

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

### **基礎設施管理**

```bash
cd infrastructure
npm install                        # 安裝依賴
cdk synth                         # 合成 CloudFormation
cdk deploy                        # 部署到 AWS
npm test                          # 運行基礎設施測試
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

### 🔄 **建議的進一步調整**

1. **統一 IDE 配置**: 只在根目錄保留 IDE 配置
2. **標準化構建輸出**: 確保所有構建輸出都在各自的 `build/` 目錄中
3. **環境配置集中**: 考慮將環境配置集中管理

## 🎯 **最終建議**

當前的雙 `build.gradle` 配置是 **正確且必要的**：

- **根目錄 `build.gradle`**: 管理多模組項目，提供全局任務
- **`app/build.gradle`**: 專門處理 Java 後端的詳細配置

這種結構支持：

- 🔧 **技術棧分離**: 每種技術使用最適合的構建工具
- 👥 **團隊協作**: 不同技能的開發者可以專注於自己的模組
- 🚀 **獨立部署**: 各模組可以獨立構建和部署
- 📈 **可擴展性**: 未來可以輕鬆添加新的模組

這是一個典型的現代全棧微服務架構，目錄結構合理且符合業界最佳實踐。
