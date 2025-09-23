# 快速入門指南

## 概覽

歡迎加入我們的開發團隊！本指南將幫助你快速了解專案架構、設置開發環境，並進行第一次程式碼貢獻。

## 前置需求

### 必要軟體
- **Java 21** - OpenJDK 或 Oracle JDK
- **Node.js 18+** - 前端開發需要
- **Docker Desktop** - 容器化開發環境
- **Git** - 版本控制

### 推薦工具
- **IntelliJ IDEA Ultimate** - Java 開發 IDE
- **VS Code** - 前端開發和通用編輯器
- **Postman** - API 測試工具
- **AWS CLI** - AWS 服務管理

## 環境設置

### 1. 克隆專案
```bash
git clone https://github.com/your-org/genai-demo.git
cd genai-demo
```

### 2. 後端設置
```bash
# 檢查 Java 版本
java -version

# 建構專案
./gradlew build

# 執行測試
./gradlew test

# 啟動應用程式
./gradlew bootRun
```

### 3. 前端設置

#### CMC 管理系統
```bash
cd cmc-frontend
npm install
npm run dev
```

#### 消費者應用
```bash
cd consumer-frontend
npm install
npm run dev
```

### 4. 資料庫設置
```bash
# 開發環境使用 H2 記憶體資料庫，無需額外設置
# 如需使用 PostgreSQL，請參考 docker-compose.yml

docker-compose up -d postgres
```

## 專案結構

### 後端結構
```
app/
├── src/main/java/solid/humank/genaidemo/
│   ├── application/          # 應用服務層
│   ├── domain/              # 領域層
│   ├── infrastructure/      # 基礎設施層
│   └── interfaces/          # 介面層
├── src/test/               # 測試程式碼
└── build.gradle           # 建構配置
```

### 前端結構
```
cmc-frontend/               # CMC 管理系統
├── src/
│   ├── components/        # React 元件
│   ├── pages/            # Next.js 頁面
│   └── styles/           # 樣式檔案
└── package.json

consumer-frontend/          # 消費者應用
├── src/
│   ├── app/              # Angular 應用
│   ├── components/       # Angular 元件
│   └── services/         # Angular 服務
└── package.json
```

## 第一次貢獻

### 1. 創建功能分支
```bash
git checkout -b feature/your-feature-name
```

### 2. 編寫程式碼
- 遵循 [編碼標準](../coding-standards.md)
- 編寫單元測試
- 更新相關文檔

### 3. 執行品質檢查
```bash
# 執行所有測試
./gradlew test

# 程式碼品質檢查
./gradlew check

# 格式化程式碼
./gradlew spotlessApply
```

### 4. 提交變更
```bash
git add .
git commit -m "feat: add new feature description"
git push origin feature/your-feature-name
```

### 5. 創建 Pull Request
- 使用清晰的標題和描述
- 連結相關的 Issue
- 確保所有檢查通過
- 請求程式碼審查

## 開發工作流程

### 日常開發
1. **拉取最新程式碼**: `git pull origin main`
2. **創建功能分支**: `git checkout -b feature/xxx`
3. **TDD 開發**: 先寫測試，再寫實作
4. **程式碼審查**: 提交 PR 並請求審查
5. **合併程式碼**: 審查通過後合併到主分支

### 測試策略
- **單元測試**: 測試業務邏輯
- **整合測試**: 測試元件互動
- **端到端測試**: 測試完整流程

## 常見問題

### Q: 如何解決建構失敗？
A: 檢查 Java 版本是否為 21，清理建構快取：`./gradlew clean build`

### Q: 如何設置 IDE？
A: 參考 [開發工具配置](../tools-and-environment/README.md)

### Q: 如何執行特定測試？
A: 使用 `./gradlew test --tests "ClassName.methodName"`

### Q: 如何連接到開發資料庫？
A: 開發環境預設使用 H2，可在 `http://localhost:8080/h2-console` 查看

## 學習資源

### 內部文檔
- [架構設計](../architecture/README.md)
- [測試指南](../testing/README.md)
- [API 設計](../coding-standards/api-design-standards.md)

### 外部資源
- [Spring Boot 官方文檔](https://spring.io/projects/spring-boot)
- [React 官方文檔](https://react.dev/)
- [Angular 官方文檔](https://angular.io/)

## 獲得幫助

### 團隊聯繫
- **技術問題**: 在 Slack #dev-help 頻道提問
- **架構討論**: 聯繫架構師團隊
- **緊急問題**: 聯繫技術負責人

### 文檔貢獻
如果你發現文檔有誤或需要改進，歡迎：
1. 創建 Issue 報告問題
2. 提交 PR 改進文檔
3. 在團隊會議中提出建議

---

**維護者**: 開發團隊  
**最後更新**: 2025年1月21日  
**版本**: 1.0

> 🎉 **歡迎加入團隊！** 如果你在設置過程中遇到任何問題，請隨時尋求幫助。我們致力於為每位開發者提供良好的入職體驗。