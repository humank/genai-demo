# 歷史圖表 (Legacy Diagrams)

## 概覽

本目錄保留專案重構前的歷史圖表，作為參考和對比用途。這些圖表記錄了專案的演進歷程，並為新的 Rozanski & Woods 架構文件提供基礎。

## 遷移對應表

### 從歷史圖表到新架構視點的對應關係

| 歷史圖表位置 | 新位置 | 視點/觀點 | 說明 |
|-------------|--------|-----------|------|
| `legacy-uml/domain-model-diagram.puml` | `viewpoints/functional/domain-model.mmd` | 功能視點 | 領域模型重新設計 |
| `legacy-uml/hexagonal-architecture-diagram.puml` | `viewpoints/development/hexagonal-architecture.mmd` | 開發視點 | 六角架構現代化 |
| `legacy-uml/event-storming-diagram.puml` | `viewpoints/information/event-storming-*.puml` | 資訊視點 | 分為三個層次 |
| `legacy-uml/observability-diagram.puml` | `viewpoints/operational/monitoring-architecture.mmd` | 運營視點 | 可觀測性架構 |
| `legacy-uml/security-architecture-diagram.puml` | `perspectives/security/security-architecture.puml` | 安全性觀點 | 安全架構更新 |
| `legacy-uml/deployment-diagram.puml` | `viewpoints/deployment/deployment-diagram.puml` | 部署視點 | 部署架構現代化 |

## 歷史圖表分類

### UML 圖表
- **類圖**: 領域模型和系統設計的類圖
- **時序圖**: 業務流程和系統互動的時序圖
- **組件圖**: 系統組件和依賴關係圖
- **部署圖**: 系統部署和基礎設施圖

### 架構圖表
- **分層架構圖**: 傳統的分層架構設計
- **六角架構圖**: 早期的六角架構實現
- **事件驅動架構圖**: 事件驅動系統設計
- **微服務架構圖**: 微服務拆分和設計

### 業務流程圖
- **Event Storming**: 事件風暴分析結果
- **用例圖**: 系統用例和參與者
- **活動圖**: 業務流程和決策流程
- **狀態圖**: 實體狀態轉換

## 保留原因

### 歷史參考
- **演進軌跡**: 記錄系統架構的演進過程
- **設計決策**: 保留歷史設計決策的背景和理由
- **學習資源**: 作為架構設計的學習和參考資料

### 對比分析
- **改進對比**: 新舊架構的對比分析
- **最佳實踐**: 識別和總結架構設計的最佳實踐
- **避免重複**: 避免重複歷史上的設計錯誤

### 合規要求
- **文件保存**: 滿足專案文件保存的合規要求
- **稽核軌跡**: 提供完整的設計變更稽核軌跡
- **知識管理**: 組織知識資產的管理和傳承

## 使用指南

### 查閱歷史圖表
1. **確定目的**: 明確查閱歷史圖表的目的
2. **找到對應**: 使用對應表找到相關的歷史圖表
3. **對比分析**: 與新的架構圖表進行對比分析
4. **提取價值**: 提取有價值的設計思路和經驗

### 引用歷史圖表
- **明確標註**: 引用時明確標註為歷史版本
- **說明背景**: 說明歷史圖表的時間背景和設計背景
- **對比說明**: 說明與當前版本的差異和改進

### 維護原則
- **只讀保存**: 歷史圖表只作為只讀資料保存
- **不再更新**: 不對歷史圖表進行內容更新
- **定期檢查**: 定期檢查歷史圖表的完整性和可存取性

## 圖表工具演進

### 工具變遷
- **早期**: 主要使用 PlantUML 和手工繪製
- **現在**: Mermaid (GitHub 原生) + PlantUML (詳細設計) + Excalidraw (概念設計)
- **未來**: 考慮引入更多自動化圖表生成工具

### 格式演進
- **標準化**: 從多種格式統一到標準化格式
- **自動化**: 從手工維護到自動化生成
- **整合**: 與開發流程的深度整合

## 存取說明

### 檔案結構
```
legacy/
├── uml/                    # UML 圖表
│   ├── class-diagrams/     # 類圖
│   ├── sequence-diagrams/  # 時序圖
│   ├── component-diagrams/ # 組件圖
│   └── deployment-diagrams/ # 部署圖
├── architecture/           # 架構圖表
│   ├── layered/           # 分層架構
│   ├── hexagonal/         # 六角架構
│   └── event-driven/      # 事件驅動
└── business/              # 業務流程圖
    ├── event-storming/    # 事件風暴
    ├── use-cases/         # 用例圖
    └── workflows/         # 工作流程
```

### 存取權限
- **讀取權限**: 所有團隊成員都有讀取權限
- **修改限制**: 禁止修改歷史圖表內容
- **備份保護**: 定期備份防止意外遺失

## 相關資源

- [新架構視點圖表](../../viewpoints/README.md) - 基於 Rozanski & Woods 的新圖表
- [架構觀點圖表](../perspectives/README.md) - 品質屬性相關圖表
- \1 - 架構演進的詳細記錄

---

**最後更新**: 2025年1月21日  
**維護者**: 架構團隊  
**注意**: 此目錄中的圖表為歷史版本，僅供參考，請使用新的架構視點圖表進行開發工作
