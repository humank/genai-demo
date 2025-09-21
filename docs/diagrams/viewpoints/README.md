# 架構視點圖表 (Viewpoint Diagrams)

## 概覽

本目錄包含基於 Rozanski & Woods 七大架構視點的圖表資源，使用不同的圖表工具來表達不同層次的架構資訊。

## 圖表工具策略

### 工具選擇原則

| 工具 | 最佳使用場景 | GitHub 顯示 | 優勢 | 使用時機 |
|------|-------------|-------------|------|----------|
| **Mermaid** | 系統架構圖、流程圖、時序圖 | ✅ 直接顯示 | 語法簡潔、版本控制友好 | 概覽圖、流程圖 |
| **PlantUML** | 詳細 UML 圖、複雜類圖 | ❌ 需轉換 PNG | 功能強大、UML 標準 | 詳細設計圖 |
| **Excalidraw** | 概念設計、腦力激盪 | ❌ 需轉換 PNG | 直觀易用、手繪風格 | 概念圖、草圖 |

### 圖表層次結構

1. **概覽層 (Mermaid)**: 系統整體架構和主要組件關係
2. **詳細層 (PlantUML)**: 具體的類圖、時序圖、組件圖
3. **概念層 (Excalidraw)**: 概念設計和利害關係人溝通

## 視點圖表目錄

### [功能視點圖表](functional/README.md)
- **domain-model.mmd**: 領域模型概覽圖 (Mermaid)
- **bounded-contexts.puml**: 界限上下文詳細圖 (PlantUML)
- **use-cases.puml**: 用例圖 (PlantUML)
- **aggregates.puml**: 聚合根設計圖 (PlantUML)

### [資訊視點圖表](information/README.md)
- **data-model.puml**: 資料模型圖 (PlantUML)
- **event-storming-big-picture.puml**: Event Storming Big Picture (PlantUML)
- **event-storming-process-level.puml**: Event Storming Process Level (PlantUML)
- **event-storming-design-level.puml**: Event Storming Design Level (PlantUML)
- **information-flow.mmd**: 資訊流圖 (Mermaid)

### [並發視點圖表](concurrency/README.md)
- **event-driven-architecture.mmd**: 事件驅動架構圖 (Mermaid)
- **async-processing.puml**: 非同步處理圖 (PlantUML)
- **concurrency-patterns.puml**: 並發模式圖 (PlantUML)

### [開發視點圖表](development/README.md)
- **hexagonal-architecture.mmd**: 六角架構圖 (Mermaid)
- **module-dependencies.puml**: 模組依賴圖 (PlantUML)
- **build-pipeline.mmd**: 建置流程圖 (Mermaid)

### [部署視點圖表](deployment/README.md)
- **infrastructure.mmd**: 基礎設施概覽圖 (Mermaid)
- **deployment-diagram.puml**: 部署圖 (PlantUML)
- **network-topology.puml**: 網路拓撲圖 (PlantUML)

### [運營視點圖表](operational/README.md)
- **monitoring-architecture.mmd**: 監控架構圖 (Mermaid)
- **observability.puml**: 可觀測性架構圖 (PlantUML)
- **incident-response.mmd**: 事件響應流程圖 (Mermaid)

## 圖表命名規範

### 檔案命名格式
```
{視點名稱}/{圖表類型}-{具體內容}.{副檔名}
```

### 範例
- `functional/domain-model.mmd` - 功能視點的領域模型 Mermaid 圖
- `information/event-storming-big-picture.puml` - 資訊視點的 Event Storming PlantUML 圖
- `deployment/infrastructure.mmd` - 部署視點的基礎設施 Mermaid 圖

## 圖表維護指南

### 自動化生成
- **PlantUML**: 使用 GitHub Actions 自動生成 PNG
- **Excalidraw**: 使用 MCP 服務生成和轉換
- **Mermaid**: GitHub 原生支援，無需額外處理

### 版本控制
- 圖表原始檔案 (`.mmd`, `.puml`, `.excalidraw`) 納入版本控制
- 生成的 PNG 檔案也納入版本控制，方便離線查看
- 使用有意義的提交訊息描述圖表變更

### 品質標準
- 圖表必須有清晰的標題和說明
- 使用一致的顏色和樣式
- 確保圖表在不同尺寸下的可讀性
- 定期檢查圖表與實際系統的一致性

## 相關資源

- [架構觀點圖表](../perspectives/README.md) - 跨視點的品質屬性圖表
- [歷史圖表](../legacy/README.md) - 保留的歷史圖表
- [圖表工具指南](../../tools/diagram-tools.md) - 圖表工具使用指南

---

**最後更新**: 2025年1月21日  
**維護者**: 架構團隊