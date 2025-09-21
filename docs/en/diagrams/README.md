
# 架構圖表總覽

> **基於 Rozanski & Woods 方法論的系統化架構視覺化**

## Overview

本目錄包含完整的系統架構圖表，按照 Rozanski & Woods 的七大 Viewpoints 和八大 Perspectives 進行組織。我們使用三種互補的圖表工具來滿足不同的視覺化需求。

## Tools

### 🌊 Mermaid - 主要架構圖表
- **用途**: GitHub 直接顯示的架構概覽
- **格式**: `.mmd` 文件
- **優勢**: 原生 GitHub 支援、版本控制友好
- **適用**: 系統概覽、服務互動、Deployment架構

### 📊 PlantUML - 詳細技術圖表  
- **用途**: 詳細的 UML 和技術設計圖
- **格式**: `.puml` 文件 + 自動生成 `.png/.svg`
- **優勢**: 功能強大、UML 標準、複雜圖表支援
- **適用**: 領域模型、Event Storming、時序圖

### Design
- **用途**: 概念設計和手繪風格圖
- **格式**: `.excalidraw` 文件 + 轉換 `.png`
- **優勢**: 直觀易用、手繪風格、AI 輔助
- **適用**: 概念設計、腦力激盪、Stakeholder圖

## 目錄結構

### 📁 按 Viewpoints 組織

```
docs/diagrams/
├── viewpoints/                      # 七大Architectural Viewpoint
│   ├── functional/                  # Functional Viewpoint
│   │   ├── system-overview.mmd     # Overview
│   │   ├── domain-model-class.puml # 領域模型類圖 (PlantUML)
│   │   ├── bounded-contexts.mmd    # Bounded Context (Mermaid)
│   │   └── [27+ 自動生成的Aggregate Root圖表]
│   ├── information/                 # Information Viewpoint
│   │   ├── event-driven-architecture.mmd # Event-Driven Architecture (Mermaid)
│   │   ├── event-storming-big-picture.puml # Event Storming (PlantUML)
│   │   └── data-flow.mmd           # 資料流圖 (Mermaid)
│   ├── concurrency/                 # Concurrency Viewpoint
│   │   └── async-processing.mmd    # 非同步處理 (Mermaid)
│   ├── development/                 # Development Viewpoint
│   │   ├── hexagonal-architecture.mmd # Hexagonal Architecture (Mermaid)
│   │   ├── ddd-layered-architecture.mmd # DDD Layered Architecture (Mermaid)
│   │   └── module-dependencies.puml # 模組依賴 (PlantUML)
│   ├── deployment/                  # Deployment
│   │   ├── infrastructure-overview.mmd # Overview
│   │   └── deployment-diagram.puml # Deployment
│   └── operational/                 # Operational Viewpoint
│       ├── monitoring-architecture.mmd # Monitoring架構 (Mermaid)
│       └── observability.puml      # Observability (PlantUML)
├── perspectives/                    # 八大Architectural Perspective
│   ├── security/                   # Security Perspective
│   ├── performance/                # Performance & Scalability Perspective
│   ├── availability/               # Availability & Resilience Perspective
│   ├── evolution/                  # Evolution Perspective
│   ├── usability/                  # Usability Perspective
│   ├── regulation/                 # Regulation Perspective
│   ├── location/                   # Location Perspective
│   └── cost/                       # Cost Perspective
├── concepts/                       # Design
├── legacy/                         # 歷史圖表
└── tools/                          # Tools
```

## 🎯 核心架構圖表

### Overview

| 圖表 | 類型 | 描述 | 狀態 |
|------|------|------|------|
| [系統概覽](viewpoints/functional/system-overview.mmd) | Mermaid | 完整系統架構概覽，展示7層架構和組件關係 | ✅ 新增 |
| [Hexagonal Architecture](hexagonal_architecture.mmd) | Mermaid | Port和Adapter架構 | ✅ 已更新 |
| [DDD Layered Architecture](ddd_architecture.mmd) | Mermaid | Domain-Driven Design分層 | ✅ 已更新 |
| [Event-Driven Architecture](event_driven_architecture.mmd) | Mermaid | 事件處理機制 | ✅ 已更新 |

### 領域模型圖表

| 圖表 | 類型 | 描述 | 狀態 |
|------|------|------|------|
| [領域模型概覽](viewpoints/functional/Domain%20Model%20Overview.png) | PlantUML | DDD Aggregate Root總覽 | ✅ 自動生成 |
| [CustomerAggregate](viewpoints/functional/Customer%20Aggregate%20Details.png) | PlantUML | CustomerAggregate Root詳細設計 | ✅ 自動生成 |
| [訂單Aggregate](viewpoints/functional/Order%20Aggregate%20Details.png) | PlantUML | 訂單Aggregate Root詳細設計 | ✅ 自動生成 |
| [支付Aggregate](viewpoints/functional/Payment%20Aggregate%20Details.png) | PlantUML | 支付Aggregate Root詳細設計 | ✅ 自動生成 |

### Event Storming 圖表

| 圖表 | 類型 | 描述 | 狀態 |
|------|------|------|------|
| [Big Picture](viewpoints/functional/Event%20Storming%20Big%20Picture.png) | PlantUML | Event Storming全景圖 | ✅ 自動生成 |
| [Process Level](viewpoints/functional/Event%20Storming%20Process%20Level.png) | PlantUML | Process Level事件圖 | ✅ 自動生成 |
| [業務流程](viewpoints/functional/Business%20Process%20Flows.png) | PlantUML | 業務流程詳細圖 | ✅ 自動生成 |

### 基礎設施圖表

| 圖表 | 類型 | 描述 | 狀態 |
|------|------|------|------|
| **[AWS 基礎設施架構](aws-infrastructure.md)** | **Mermaid** | **完整 AWS CDK 基礎設施架構文檔** | **✅ 新增** |
| [AWS 基礎設施圖表](aws_infrastructure.mmd) | Mermaid | AWS 服務架構圖 | ✅ 已更新 |
| [多Environment架構](multi_environment.mmd) | Mermaid | 開發/測試/生產Environment | ✅ 已更新 |
| [Observability架構](observability_architecture.mmd) | Mermaid | Monitoring、Logging、Tracing系統 | ✅ 已更新 |
| [基礎設施概覽](viewpoints/deployment/infrastructure-overview.mmd) | Mermaid | 雲端基礎設施架構 | ✅ 已更新 |
| [Monitoring架構](viewpoints/operational/monitoring-architecture.mmd) | Mermaid | Observability平台 | ✅ 已更新 |
| [非同步處理](viewpoints/concurrency/async-processing.mmd) | Mermaid | 並發和非同步架構 | ✅ 已更新 |

## 🔄 自動化圖表生成

### 生成所有圖表

```bash
# 生成所有類型的圖表
./scripts/generate-all-diagrams.sh

# 只生成特定類型
./scripts/generate-all-diagrams.sh --plantuml
./scripts/generate-all-diagrams.sh --mermaid
./scripts/generate-all-diagrams.sh --excalidraw

# 清理後重新生成
./scripts/generate-all-diagrams.sh --clean
```

### Kiro Hook 自動化

系統已配置 Kiro Hook 來自動Monitoring程式碼變更並更新相關圖表：

- **DDD 註解Monitoring**: Monitoring `@AggregateRoot`、`@ValueObject`、`@Entity` 變更
- **BDD Feature Monitoring**: Monitoring `.feature` 檔案變更
- **自動圖表生成**: 程式碼變更時自動更新 PlantUML 圖表

## 📊 圖表統計

### 當前圖表數量

- **Mermaid 圖表**: 6 個主要架構圖
- **PlantUML 圖表**: 27+ 個自動生成的詳細圖表
- **Excalidraw 圖表**: 概念設計圖 (按需創建)
- **PNG/SVG 輸出**: 自動生成的圖片文件

### 覆蓋範圍

- ✅ **Functional Viewpoint**: 系統概覽、領域模型、Aggregate Root設計
- ✅ **Information Viewpoint**: Event-Driven Architecture、Event Storming 分析
- ✅ **Concurrency Viewpoint**: 非同步處理、並發控制
- ✅ **Development Viewpoint**: Hexagonal Architecture、DDD Layered Architecture
- ✅ **Deployment Viewpoint**: 基礎設施、Containerization、CI/CD
- ✅ **Operational Viewpoint**: Monitoring、Observability、告警

## Maintenance

### 更新圖表

1. **Mermaid 圖表**: 直接編輯 `.mmd` 文件
2. **PlantUML 圖表**: 編輯 `.puml` 文件，運行生成腳本
3. **Excalidraw 圖表**: 使用 Excalidraw 編輯器或 MCP 整合

### 品質檢查

```bash
# 驗證現有圖表
./scripts/generate-all-diagrams.sh --validate

# 生成圖表報告
./scripts/generate-all-diagrams.sh --report
```

### Best Practices

1. **命名規範**: 使用 `kebab-case` 命名
2. **目錄組織**: 按 Viewpoint 分類存放
3. **版本控制**: 源文件納入 Git，PNG 文件可選
4. **文檔關聯**: 在 Markdown 中引用圖表
5. **定期更新**: 保持圖表與實際實現同步

## Resources

- **[圖表工具使用指南](diagram-tools-guide.md)**: 詳細的工具使用說明
- **[Viewpoints 總覽](../viewpoints/README.md)**: 七大Architectural Viewpoint文檔
- **[Perspectives 總覽](../perspectives/README.md)**: 八大Architectural Perspective文檔
- **[自動化腳本](../../scripts/README.md)**: 圖表生成和維護腳本

---

**維護者**: 架構團隊  
**最後更新**: 2025年1月21日  
**圖表工具**: Mermaid + PlantUML + Excalidraw  
**自動化**: Kiro Hook + GitHub Actions