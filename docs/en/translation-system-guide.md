
# Guidelines

## Overview

This project實現了基於 Rozanski & Woods 方法論的自動化翻譯系統，確保中英文文件的一致性和專業術語的準確翻譯。

## 系統組件

### 1. Kiro Hook 配置 (v4.0)

**檔案位置**: `.kiro/hooks/md-docs-translation.kiro.hook`

Hook 會自動Monitoring以下檔案變更：
- `README.md`
- `docs/**/*.md`

**排除檔案**:
- `docs/en/**/*.md` (英文版本)
- `docs/.terminology.json` (術語字典)
- `docs/legacy/**/*.md` (舊版圖表)
- 系統檔案 (`node_modules`, `.git`, `.kiro`)
- 圖表檔案 (`*.png`, `*.jpg`, `*.svg`, `*.puml`, `*.mmd`, `*.excalidraw`)

**新功能**:
- 支援 Rozanski & Woods Viewpoints & Perspectives 新文件結構
- 增強的專業術語翻譯支援
- Stakeholder和設計Policy術語
- Cross-Viewpoint Application和Implementation Technique術語

### 2. 專業術語字典 (v2.0)

**檔案位置**: `docs/.terminology.json`

**增強功能**:
- 支援新的 Viewpoints & Perspectives 文件結構
- 226+ 專業術語，涵蓋 18 個類別
- 增加Stakeholder和設計Policy術語

**術語類別**:
- **Rozanski & Woods 視點** (8 terms): Functional Viewpoint、Information Viewpoint、Concurrency Viewpoint、Development Viewpoint、Deployment Viewpoint、Operational Viewpoint等
- **Rozanski & Woods 觀點** (11 terms): Security Perspective、Performance & Scalability Perspective、Availability & Resilience Perspective、Evolution Perspective等
- **DDD 戰略模式** (10 terms): Bounded Context、Context Mapping、Shared Kernel等
- **DDD 戰術模式** (10 terms): Aggregate Root、Value Object、Domain Event、Domain Service等
- **Event Storming** (12 terms): Big Picture Exploration、Process Level、Design Level、Hotspot等
- **Hexagonal Architecture** (13 terms): Port、Adapter、Application Core、驅動Adapter等
- **Infrastructure as Code** (11 terms): AWS CDK、Multi-Stack Architecture、Construct等
- **測試術語** (12 terms): TDD、BDD、Unit Test、Integration Test等
- **Observability** (12 terms): Monitoring、Logging、Tracing、Metrics等
- **Quality Attribute** (15 terms): Performance、Scalability、Availability、Reliability等
- **Architectural Pattern** (12 terms): Layered Architecture、微服務、事件驅動、Command Query Responsibility Segregation (Command Query Responsibility Segregation (CQRS))等
- **開發實踐** (12 terms): Agile Development、DevOps、Continuous Integration (CI)等
- **Cloud Native** (12 terms): Containerization、Kubernetes、Service Mesh等
- **AI 輔助開發** (9 terms): MCP、Code Generation、智能Refactoring等
- **Stakeholder術語** (18 terms): Architect、Developer、Operations Engineer、Security Engineer等
- **設計Policy** (19 terms): 設計Policy、Architectural Element、Concern、Quality Attribute等
- **Implementation Technique** (18 terms): Implementation Technique、Verification Criteria、Technology Selection等
- **Project Management** (12 terms): Requirements Analysis、Architecture Design、Milestone等

### 3. 翻譯品質檢查機制 (增強版)

**腳本位置**: `scripts/check-translation-quality.sh`

**檢查項目**:
- 翻譯完整性檢查 (支援新文件結構)
- 術語一致性驗證 (80+ 關鍵術語)
- 內部連結有效性檢查
- Viewpoints & Perspectives 目錄結構一致性
- 術語字典驗證 (18 個類別)
- 七大視點和八大觀點結構檢查

**執行方式**:
```bash
./scripts/check-translation-quality.sh
```

**測試腳本**:
```bash
./scripts/test-translation-system.sh  # Testing
```

### 4. 自動化翻譯觸發

**主要機制**: Kiro Hook 自動觸發翻譯

**觸發條件**:
- 修改任何中文 Markdown 檔案
- 自動檢測中文字符並觸發翻譯
- 支援新的 Viewpoints & Perspectives 結構

**自動化功能**:
- 即時翻譯觸發
- 專業術語一致性保證
- 文件結構自動對應
- 內部連結自動更新

## 檔案組織規則

### 中文檔案 → 英文檔案對應

| 中文檔案 | 英文檔案 | 說明 |
|---------|---------|------|
| `README.md` | `docs/en/PROJECT_README.md` | 專案主要說明 |
| `../viewpoints/**/*.md` | `viewpoints/**/*.md` | 七大Architectural Viewpoint |
| `docs/perspectives/**/*.md` | `docs/en/perspectives/**/*.md` | 八大Architectural Perspective |
| `../diagrams/**/*.md` | `docs/en/diagrams/**/*.md` | 圖表文件 |
| `docs/templates/**/*.md` | `docs/en/templates/**/*.md` | 文件模板 |
| `../../api/**/*.md` | `docs/en/api/**/*.md` | API 文件 |
| `docs/mcp/**/*.md` | `docs/en/mcp/**/*.md` | MCP 整合文件 |
| `docs/releases/**/*.md` | `docs/en/releases/**/*.md` | 發布說明 |
| `docs/reports/**/*.md` | `reports/**/*.md` | 專案報告 |

### 目錄結構

```
docs/
├── viewpoints/           # 七大Architectural Viewpoint
│   ├── functional/       # Functional Viewpoint
│   ├── information/      # Information Viewpoint
│   ├── concurrency/      # Concurrency Viewpoint
│   ├── development/      # Development Viewpoint
│   ├── deployment/       # Deployment
│   └── operational/      # Operational Viewpoint
├── perspectives/         # 八大Architectural Perspective
│   ├── security/         # Security Perspective
│   ├── performance/      # Performance & Scalability Perspective
│   ├── availability/     # Availability & Resilience Perspective
│   ├── evolution/        # Evolution Perspective
│   ├── usability/        # Usability Perspective
│   ├── regulation/       # Regulation Perspective
│   ├── location/         # Location Perspective
│   └── cost/            # Cost Perspective
├── diagrams/            # Resources
├── templates/           # Templates
├── .terminology.json    # 術語字典
└── en/                  # 英文版本 (自動生成)
    └── [對應的英文文件結構]
```

## 翻譯規則

### Standards

**Rozanski & Woods 視點**:
- Architectural Viewpoint → Architectural Viewpoint
- Functional Viewpoint → Functional Viewpoint
- Information Viewpoint → Information Viewpoint
- Concurrency Viewpoint → Concurrency Viewpoint
- Development Viewpoint → Development Viewpoint
- Deployment Viewpoint → Deployment Viewpoint
- Operational Viewpoint → Operational Viewpoint

**Rozanski & Woods 觀點**:
- Architectural Perspective → Architectural Perspective
- Security Perspective → Security Perspective
- Performance & Scalability Perspective → Performance & Scalability Perspective
- Availability & Resilience Perspective → Availability & Resilience Perspective
- Evolution Perspective → Evolution Perspective
- Usability Perspective → Usability Perspective
- Regulation Perspective → Regulation Perspective
- Location Perspective → Location Perspective
- Cost Perspective → Cost Perspective

**DDD 術語**:
- Domain-Driven Design → Domain-Driven Design (DDD)
- Bounded Context → Bounded Context
- Aggregate Root → Aggregate Root
- Value Object → Value Object
- Domain Event → Domain Event
- Event Storming → Event Storming

### 格式保留規則

**必須保持不變**:
- URL 連結
- 檔案路徑
- 程式碼區塊
- Command範例
- 版本號碼
- API 端點
- 配置鍵值

**格式保留**:
- Markdown 標題
- 項目符號
- 編號清單
- 表格
- 程式碼圍欄
- 引用區塊
- 強調 (粗體/斜體)
- 連結

## 使用方式

### 自動翻譯觸發

1. **編輯中文檔案**: 修改任何 `docs/**/*.md` 或 `README.md` 檔案
2. **Hook 自動觸發**: Kiro Hook 會自動檢測變更並觸發翻譯
3. **生成英文版本**: 系統會在 `docs/en/` 目錄下生成對應的英文檔案

### 手動品質檢查

```bash
# 執行完整的翻譯品質檢查
./scripts/check-translation-quality.sh

# 檢查特定檔案的翻譯狀態
grep -l "Architectural Viewpoint" docs/**/*.md
```

### 術語字典更新

1. 編輯 `docs/.terminology.json`
2. 新增或修改術語對應關係
3. 執行品質檢查驗證變更
4. 重新翻譯相關檔案

## Quality Assurance

### 自動化檢查

- **CI/CD 整合**: GitHub Actions 自動執行翻譯品質檢查
- **Pull Request 檢查**: 每個 PR 都會檢查翻譯品質
- **術語一致性**: 自動驗證術語使用的一致性
- **連結有效性**: 檢查內部連結是否有效

### 手動檢查

- **專業術語審查**: 定期審查術語翻譯的準確性
- **上下文檢查**: 確保翻譯在特定上下文中的適當性
- **跨文件一致性**: 檢查相同術語在不同文件中的一致性

## Troubleshooting

### 常見問題

**1. Hook 未觸發翻譯**
- 檢查 `.kiro/hooks/md-docs-translation.kiro.hook` 是否存在
- 確認檔案路徑符合 Hook 的Monitoring模式
- 檢查 Hook 是否啟用 (`"enabled": true`)

**2. 術語翻譯不一致**
- 檢查 `docs/.terminology.json` 中的術語定義
- 執行 `./scripts/check-translation-quality.sh` 找出不一致的地方
- 更新英文檔案中的術語使用

**3. 翻譯品質檢查失敗**
- 查看詳細的錯誤報告
- 檢查缺失的翻譯檔案
- 修復損壞的內部連結
- 更新術語使用

### Maintenance

1. **定期更新術語字典**: 隨著專案發展，新增新的專業術語
2. **Monitoring翻譯品質**: 定期執行品質檢查，確保翻譯準確性
3. **文件結構同步**: 確保中英文文件結構保持一致
4. **團隊培訓**: 確保團隊成員了解翻譯系統的使用方式

## 未來改進

### 計劃功能

- **增量翻譯**: 只翻譯變更的部分，提高效率
- **翻譯記憶**: 建立翻譯記憶庫，提高一致性
- **多語言支援**: 支援更多語言的翻譯
- **AI 輔助審查**: 使用 AI 協助翻譯品質審查

### 技術改進

- **Performance優化**: 優化大型文件的翻譯處理
- **錯誤恢復**: 改進翻譯失敗時的錯誤處理
- **批次處理**: 支援批次翻譯多個檔案
- **版本控制**: Tracing翻譯版本和變更歷史

這個自動化翻譯系統確保了專案文件的國際化品質，同時維持了專業術語的一致性和準確性。