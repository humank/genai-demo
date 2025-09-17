# GenAI Demo 文檔中心

歡迎來到 GenAI Demo 專案的文檔中心！這裡包含了專案的完整文檔，按功能和用途分類組織。

## 🌐 語言版本

- **中文版本** (當前): [docs/](.)
- **English Version**: [docs/en/](en/)

## 📚 文檔分類

### 🏗️ [架構文檔](architecture/)

系統架構相關的文檔，適合架構師和高級開發者。

- **[架構決策記錄 (ADR)](architecture/adr/)** - 完整記錄所有重要的架構決策
- [架構概覽](architecture/overview.md) - 系統整體架構介紹
- [六角形架構](architecture/hexagonal-architecture.md) - 六角形架構實現詳解
- [分層架構設計](architecture/layered-architecture-design.md) - 分層架構設計指南
- [2025年架構改進](architecture/improvements-2025.md) - 最新架構改進記錄

### 🔌 [API 文檔](api/)

API 相關的文檔，適合 API 使用者和前端開發者。

- [API 版本策略](api/versioning-strategy.md) - API 版本管理策略
- [OpenAPI 規範](api/openapi-spec.md) - OpenAPI 3.0 規範文檔
- [前端 API 整合](api/frontend-integration.md) - 前端 API 整合指南

### 🤖 [MCP 整合文檔](mcp/) - NEW

Model Context Protocol 整合相關文檔，適合 AI 輔助開發。

- **[MCP 整合指南](mcp/README.md)** - 完整的 MCP 整合指南和使用說明
- MCP Servers 配置和管理
- AI 輔助開發最佳實踐
- 故障排除和性能優化

### 🧪 [測試文檔](testing/) - NEW

測試相關的文檔，適合 QA 工程師和開發者。

- **[測試性能監控](testing/test-performance-monitoring.md)** - 測試性能監控框架完整指南
- [測試配置指南](testing/test-configuration-examples.md) - 測試配置範例和最佳實踐
- [HTTP 客戶端配置](testing/http-client-configuration-guide.md) - 測試 HTTP 客戶端配置
- [故障排除指南](testing/testresttemplate-troubleshooting-guide.md) - 測試問題排除
- [新開發者指南](testing/new-developer-onboarding-guide.md) - 新開發者測試入門

### 📊 [圖表文檔](diagrams/)

系統的各種圖表和視覺化文檔，適合所有角色。

#### Mermaid 圖表 (GitHub 直接顯示)

- [架構概覽](diagrams/mermaid/architecture-overview.md) - 系統整體架構圖
- [六角形架構](diagrams/mermaid/hexagonal-architecture.md) - 六角形架構圖
- [DDD 分層架構](diagrams/mermaid/ddd-layered-architecture.md) - DDD 分層架構圖
- [事件驅動架構](diagrams/mermaid/event-driven-architecture.md) - 事件驅動架構圖
- [API 交互圖](diagrams/mermaid/api-interactions.md) - API 交互關係圖

#### PlantUML 圖表 (詳細 UML 圖表)

- **結構圖**: 類圖、對象圖、組件圖、部署圖、包圖、複合結構圖
- **行為圖**: 用例圖、活動圖、狀態圖
- **交互圖**: 時序圖、通信圖、交互概覽圖、時間圖
- **Event Storming**: Big Picture、Process Level、Design Level

### 💻 [開發指南](development/)

開發相關的文檔，適合開發者和新加入的團隊成員。

- [快速入門](development/getting-started.md) - 專案快速入門指南
- [編碼標準](development/coding-standards.md) - 編碼規範和最佳實踐
- [開發說明](development/instructions.md) - 開發流程和說明
- [文檔維護指南](development/documentation-guide.md) - 文檔創建和維護指南

### 🚀 [部署文檔](deployment/)

部署相關的文檔，適合 DevOps 工程師和運維人員。

- [Docker 指南](deployment/docker-guide.md) - Docker 容器化部署
- [Kubernetes 指南](deployment/kubernetes-guide.md) - Kubernetes 集群部署

### 🎨 [設計文檔](design/)

設計相關的文檔，適合軟體架構師和設計決策者。

- [DDD 指南](design/ddd-guide.md) - 領域驅動設計指南
- [設計原則](design/design-principles.md) - 軟體設計原則
- [重構指南](design/refactoring-guide.md) - 代碼重構指南

### 📋 [發布說明](releases/)

版本發布和變更記錄，適合所有利益相關者。

- [發布記錄](releases/) - 版本發布歷史

### 📊 [報告文檔](reports/)

專案報告和分析文檔，適合專案經理和技術負責人。

- [專案總結 2025](reports/project-summary-2025.md) - 2025年專案總結報告
- [架構卓越性 2025](reports/architecture-excellence-2025.md) - 架構卓越性評估
- [技術棧 2025](reports/technology-stack-2025.md) - 技術棧分析報告
- [文檔清理 2025](reports/documentation-cleanup-2025.md) - 文檔清理報告

## 🎯 快速導航

### 👨‍💼 我是專案經理

- [專案總結 2025](reports/project-summary-2025.md) - 了解專案現狀
- [架構概覽](diagrams/mermaid/architecture-overview.md) - 系統整體架構
- [發布記錄](releases/) - 版本發布歷史

### 🏗️ 我是架構師

- [架構文檔](architecture/) - 完整架構設計
- [圖表文檔](diagrams/) - 視覺化架構圖
- [設計文檔](design/) - 設計原則和指南

### 👨‍💻 我是開發者

- [開發指南](development/) - 開發環境和規範
- [API 文檔](api/) - API 使用指南
- [開發說明](development/instructions.md) - 開發流程和說明

### 🚀 我是 DevOps 工程師

- [部署文檔](deployment/) - 部署指南
- [Docker 指南](deployment/docker-guide.md) - 容器化部署
- [Kubernetes 指南](deployment/kubernetes-guide.md) - 集群部署

### 🔍 我是 SRE/可觀測性工程師

- **[生產環境可觀測性測試指南](observability/production-observability-testing-guide.md)** - 67頁完整的生產環境測試策略
- [可觀測性系統](observability/) - 監控、日誌、追蹤系統
- [MCP 整合](mcp/) - AI 輔助開發和監控

### 🔍 我是業務分析師

- [Event Storming 圖表](diagrams/plantuml/event-storming/) - 業務流程分析
- [用例圖](diagrams/plantuml/behavioral/) - 系統功能概覽
- [API 交互圖](diagrams/mermaid/api-interactions.md) - 系統交互

## 🛠️ 工具和腳本

### 圖表生成

```bash
# 生成所有 PlantUML 圖表
./scripts/generate-diagrams.sh

# 生成特定圖表
./scripts/generate-diagrams.sh domain-model-class-diagram.puml

# 驗證圖表語法
./scripts/generate-diagrams.sh --validate
```

### 文檔同步

```bash
# 同步中英文文檔
./scripts/sync-docs.sh

# 驗證文檔品質
./scripts/validate-docs.sh
```

## 📈 專案統計

- **總文檔數**: 50+ 個文檔
- **圖表數量**: 20+ 個圖表
- **支援語言**: 中文、英文
- **架構模式**: DDD + 六角形架構 + 事件驅動
- **技術棧**: Java 21 + Spring Boot 3.5.5 + Next.js 14.2.30 + Angular 18.2.0
- **測試覆蓋**: 272 個測試，100% 通過率
- **代碼品質**: ArchUnit 架構測試確保 DDD 合規性

## 🔗 外部連結

### 在線編輯器

- [Mermaid Live Editor](https://mermaid.live/) - Mermaid 圖表在線編輯
- [PlantUML Online Server](http://www.plantuml.com/plantuml/uml/) - PlantUML 圖表在線編輯

### API 端點

- **後端 API**: <http://localhost:8080>
- **Swagger UI**: <http://localhost:8080/swagger-ui/index.html>
- **健康檢查**: <http://localhost:8080/actuator/health>
- **CMC 前端**: <http://localhost:3002>
- **Consumer 前端**: <http://localhost:3001>

## 📝 貢獻指南

### 文檔更新流程

1. 更新中文文檔
2. Kiro Hook 自動生成英文版本
3. 人工審核翻譯品質
4. 提交變更

### 圖表更新流程

1. 修改 PlantUML 源文件
2. 運行 `./scripts/generate-diagrams.sh`
3. 檢查生成的圖片
4. 提交源文件和生成的圖片

## 📞 支援

如有問題或建議，請：

1. 查看相關文檔
2. 檢查 [Issues](../../issues)
3. 創建新的 Issue

---

**最後更新**: 2025年1月21日  
**文檔版本**: v3.0.0  
**維護者**: GenAI Demo 團隊  
**技術棧**: Java 21 + Spring Boot 3.5.5 + Next.js 14.2.30 + Angular 18.2.0  
**Hook 測試**: 2025年1月21日 - 測試自動翻譯功能
