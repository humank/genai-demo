# 快速入門指南

## 概覽

歡迎加入我們的開發團隊！本指南將幫助你快速設置開發環境，了解專案結構，並完成你的第一次貢獻。

## 📋 前置需求

在開始之前，請確保你已經具備以下條件：

### 必要工具
- [ ] **Java 21** - OpenJDK 或 Oracle JDK
- [ ] **Node.js 18+** - 前端開發需要
- [ ] **Git** - 版本控制
- [ ] **Docker** - 容器化開發環境
- [ ] **AWS CLI** - 雲端資源管理

### 推薦工具
- [ ] **IntelliJ IDEA** 或 **VS Code** - IDE
- [ ] **Postman** 或 **Insomnia** - API 測試
- [ ] **DBeaver** - 資料庫管理
- [ ] **Kiro IDE** - AI 輔助開發

詳細安裝指南請參考：[前置需求檢查清單](prerequisites.md)

## ⚙️ 環境設置

### 1. 專案克隆
```bash
git clone https://github.com/your-org/genai-demo.git
cd genai-demo
```

### 2. 後端設置
```bash
# 建置專案
./gradlew build

# 執行測試
./gradlew test

# 啟動應用
./gradlew bootRun
```

### 3. 前端設置
```bash
# CMC 管理前端
cd cmc-frontend
npm install
npm run dev

# 消費者前端
cd consumer-frontend
npm install
npm run dev
```

### 4. 資料庫設置
```bash
# 啟動本地資料庫
docker-compose up -d postgres

# 執行資料庫遷移
./gradlew flywayMigrate
```

詳細設置步驟請參考：[環境配置指南](environment-setup.md)

## 🏗️ 專案結構概覽

```
genai-demo/
├── app/                        # Spring Boot 應用
│   ├── src/main/java/         # Java 源碼
│   │   └── solid/humank/genaidemo/
│   │       ├── domain/        # 領域層 (DDD)
│   │       ├── application/   # 應用層
│   │       └── infrastructure/ # 基礎設施層
│   └── src/test/              # 測試代碼
├── cmc-frontend/              # CMC 管理前端 (Next.js)
├── consumer-frontend/         # 消費者前端 (Angular)
├── infrastructure/           # AWS CDK 基礎設施
├── docs/                     # 專案文檔
│   └── viewpoints/           # 架構視點文檔
└── scripts/                  # 自動化腳本
```

## 🎯 第一次貢獻

### 1. 選擇一個簡單的任務
- 查看 [Good First Issues](https://github.com/your-org/genai-demo/labels/good%20first%20issue)
- 或者從文檔改進開始

### 2. 創建功能分支
```bash
git checkout -b feature/your-feature-name
```

### 3. 遵循開發標準
- [Java 編碼標準](../coding-standards/java-standards.md)
- [前端編碼標準](../coding-standards/frontend-standards.md)
- [API 設計規範](../coding-standards/api-design.md)

### 4. 編寫測試
- [TDD 實踐指南](../testing/tdd-practices/red-green-refactor.md)
- [BDD 場景編寫](../testing/bdd-practices/feature-writing.md)

### 5. 提交程式碼
```bash
git add .
git commit -m "feat: add your feature description"
git push origin feature/your-feature-name
```

### 6. 創建 Pull Request
- 使用 [PR 模板](../../../../.github/pull_request_template.md)
- 確保所有檢查都通過
- 請求程式碼審查

詳細貢獻流程請參考：[首次貢獻指南](first-contribution.md)

## 🧪 執行測試

### 單元測試
```bash
./gradlew test
```

### 整合測試
```bash
./gradlew integrationTest
```

### BDD 測試
```bash
./gradlew cucumber
```

### 效能測試
```bash
./gradlew performanceTest
```

## 🔍 常見問題

### Q: 建置失敗怎麼辦？
A: 首先檢查 Java 版本是否為 21，然後清理建置快取：
```bash
./gradlew clean build
```

### Q: 測試失敗怎麼辦？
A: 查看測試報告並檢查：
- 資料庫是否正確啟動
- 環境變數是否正確設置
- 依賴是否正確安裝

### Q: 如何除錯應用？
A: 使用 IDE 的除錯功能，或者查看日誌：
```bash
tail -f logs/application.log
```

## 📚 學習資源

### 必讀文檔
- [DDD 戰術模式](../architecture/ddd-patterns/tactical-patterns.md)
- [六角架構實作](../architecture/hexagonal-architecture/README.md)
- [測試策略](../testing/README.md)

### 推薦學習路徑
1. **第一週**: 熟悉專案結構和基本概念
2. **第二週**: 學習 DDD 和六角架構
3. **第三週**: 掌握測試驅動開發
4. **第四週**: 了解微服務和 Saga 模式

### 外部資源
- [Domain-Driven Design](https://domainlanguage.com/ddd/)
- [Spring Boot Guide](https://spring.io/guides)
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)

### 文檔反饋
如果你發現文檔有任何問題或改進建議，請：
1. 創建 GitHub Issue
2. 直接提交 PR 修正

---

**下一步**: [環境配置詳細指南](environment-setup.md) →

> 💡 **提示**: 不要害怕提問！每個人都是從新手開始的，團隊很樂意幫助你成長。