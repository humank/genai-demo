<!-- This document needs manual translation from Chinese to English -->
<!-- 此文檔需要從中文手動翻譯為英文 -->

# 圖表文檔

本目錄包含系統的各種圖表和視覺化文檔。

## 目錄結構

### Mermaid 圖表

- [架構概覽](mermaid/architecture-overview.md) - 系統整體架構圖
- [六角形架構](mermaid/hexagonal-architecture.md) - 六角形架構圖
- [DDD 分層架構](mermaid/ddd-layered-architecture.md) - DDD 分層架構圖
- [事件驅動架構](mermaid/event-driven-architecture.md) - 事件驅動架構圖
- [API 交互圖](mermaid/api-interactions.md) - API 交互關係圖

### PlantUML 圖表

#### 結構圖 (Structural Diagrams)

- [領域模型類圖](plantuml/structural/domain-model-class-diagram.puml)
- [聚合根詳細設計](plantuml/structural/aggregate-root-details.puml)
- [對象圖](plantuml/structural/object-diagrams.puml)
- [組件圖](plantuml/structural/component-diagram.puml)
- [部署圖](plantuml/structural/deployment-diagram.puml)
- [包圖](plantuml/structural/package-diagram.puml)
- [複合結構圖](plantuml/structural/composite-structure-diagram.puml)

#### 行為圖 (Behavioral Diagrams)

- [用例圖](plantuml/behavioral/usecase-diagram.puml)
- [活動圖](plantuml/behavioral/activity-diagrams.puml)
- [狀態圖](plantuml/behavioral/state-diagrams.puml)

#### 交互圖 (Interaction Diagrams)

- [時序圖](plantuml/interaction/sequence-diagrams/)
- [通信圖](plantuml/interaction/communication-diagrams.puml)
- [交互概覽圖](plantuml/interaction/interaction-overview-diagram.puml)
- [時間圖](plantuml/interaction/timing-diagrams.puml)

#### Event Storming 圖表

- [Big Picture Event Storming](plantuml/event-storming/big-picture.puml)
- [Process Level Event Storming](plantuml/event-storming/process-level.puml)
- [Design Level Event Storming](plantuml/event-storming/design-level.puml)

### Legacy UML 圖表

- [Legacy UML 圖表](legacy-uml/) - 舊版本的 UML 圖表，包含豐富的歷史圖表資源

## 圖表生成

使用以下腳本生成圖表：

```bash
# 生成所有 PlantUML 圖表
./scripts/generate-diagrams.sh

# 生成特定圖表
./scripts/generate-diagrams.sh domain-model-class-diagram.puml
```

## 在線編輯器

- [Mermaid Live Editor](https://mermaid.live/)
- [PlantUML Online Server](http://www.plantuml.com/plantuml/uml/)

## 適用對象

- 系統架構師
- 開發者
- 業務分析師
- 專案利益相關者

## 相關連結

- [架構文檔](../architecture/) - 架構說明文檔
- [設計文檔](../design/) - 設計原則和指南
