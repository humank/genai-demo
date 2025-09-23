

<!-- 
注意：Mermaid 圖表格式更新
- 舊格式：.mmd 文件引用
- 新格式：.md 文件中的 ```mermaid 代碼塊
- 原因：GitHub 原生支援，更好的可讀性和維護性
-->

# [Viewpoint Name] 視點

---
title: "[Viewpoint Name] 視點"
viewpoint: "functional|information|concurrency|development|deployment|operational|context"
perspective: ["security", "performance", "availability", "evolution", "usability", "regulation", "location", "cost"]
stakeholders: ["architect", "developer", "operator", "security-engineer", "business-analyst"]
related_viewpoints: ["viewpoint1", "viewpoint2"]
related_documents: ["doc1.md", "doc2.md"]
diagrams: ["diagram1.mmd"  # 注意：現在使用包含 Mermaid 代碼塊的 .md 文件, "diagram2.puml"]
last_updated: "YYYY-MM-DD"
version: "1.0"
author: "Architecture Team"
review_status: "draft|reviewed|approved"
---

## Overview

[視點的定義和目標]

### 視點目的
- [目的1：描述此視點要解決的核心問題]
- [目的2：說明此視點的價值和重要性]
- [目的3：定義此視點的範圍和邊界]

### 適用場景
- [場景1：何時需要使用此視點]
- [場景2：此視點最有價值的情況]
- [場景3：此視點的限制和不適用場景]

## Stakeholders

### Primary Stakeholders
- **[角色1]**: [Concern和期望]
- **[角色2]**: [Concern和期望]
- **[角色3]**: [Concern和期望]

### Secondary Stakeholders
- **[角色4]**: [Concern和期望]
- **[角色5]**: [Concern和期望]

### Stakeholders對應表

| Stakeholder | 主要Concern | 期望結果 | 影響程度 |
|-----------|-----------|---------|---------|
| [角色1] | [Concern] | [期望] | 高/中/低 |
| [角色2] | [Concern] | [期望] | 高/中/低 |

## Concerns

### 核心Concern
1. **[Concern1]**: [詳細描述]
2. **[Concern2]**: [詳細描述]
3. **[Concern3]**: [詳細描述]

### 次要Concern
1. **[Concern4]**: [詳細描述]
2. **[Concern5]**: [詳細描述]

### Concerns優先級矩陣

| Concern | 重要性 | 緊急性 | 優先級 | 負責角色 |
|-------|-------|-------|-------|---------|
| [Concern1] | 高/中/低 | 高/中/低 | P1/P2/P3 | [角色] |
| [Concern2] | 高/中/低 | 高/中/低 | P1/P2/P3 | [角色] |

## Architectural Elements

### [元素類型1]
**定義**: [元素的定義和作用]

**特徵**:
- [特徵1]
- [特徵2]
- [特徵3]

**實現方式**:
- [實現方式1]
- [實現方式2]

**範例**:
```[language]
// 程式碼範例或配置範例
[範例內容]
```

### [元素類型2]
**定義**: [元素的定義和作用]

**特徵**:
- [特徵1]
- [特徵2]

**實現方式**:
- [實現方式1]
- [實現方式2]

## Quality Attributes考量

### Security Perspective
**影響程度**: 高/中/低

**考量要點**:
- [Security在此視點的具體考量]
- [相關的安全措施和控制]
- [安全風險和緩解Policy]

**Implementation Guide**:
- [具體的安全實現recommendations]

### Performance & Scalability Perspective
**影響程度**: 高/中/低

**考量要點**:
- [Performance在此視點的具體考量]
- [Scalability的設計要求]
- [Performance瓶頸和優化Policy]

**Implementation Guide**:
- [具體的Performance優化recommendations]

### Availability & Resilience Perspective
**影響程度**: 高/中/低

**考量要點**:
- [Availability在此視點的具體考量]
- [Resilience和容錯的設計要求]
- [故障恢復和災難準備]

**Implementation Guide**:
- [具體的Resilience設計recommendations]

### Evolution Perspective
**影響程度**: 高/中/低

**考量要點**:
- [演進性在此視點的具體考量]
- [Maintainability和Scalability要求]
- [Technical Debt和RefactoringPolicy]

**Implementation Guide**:
- [具體的演進性設計recommendations]

### Usability Perspective
**影響程度**: 高/中/低

**考量要點**:
- [使用性在此視點的具體考量]
- [User體驗和介面設計]
- [可訪問性和國際化要求]

**Implementation Guide**:
- [具體的使用性設計recommendations]

### Regulation Perspective
**影響程度**: 高/中/低

**考量要點**:
- [法規合規在此視點的具體考量]
- [資料治理和隱私保護]
- [稽核和報告要求]

**Implementation Guide**:
- [具體的合規實現recommendations]

### Location Perspective
**影響程度**: 高/中/低

**考量要點**:
- [地理分佈在此視點的具體考量]
- [資料本地化和網路拓撲]
- [邊緣運算和延遲優化]

**Implementation Guide**:
- [具體的位置相關設計recommendations]

### Cost Perspective
**影響程度**: 高/中/低

**考量要點**:
- [成本控制在此視點的具體考量]
- [Resource效率和預算管理]
- [成本優化Policy]

**Implementation Guide**:
- [具體的成本優化recommendations]

## Related Diagrams

### Overview
- **[圖表1名稱]**: [圖表描述和用途]
  - 檔案: [../diagrams/viewpoints/[viewpoint]/[diagram1].mmd](../diagrams/viewpoints/[viewpoint]/[diagram1].mmd)
  - 類型: Mermaid
  - 更新頻率: [頻率]

- **[圖表2名稱]**: [圖表描述和用途]
  - 檔案: [../diagrams/viewpoints/[viewpoint]/[diagram2].puml](../diagrams/viewpoints/[viewpoint]/[diagram2].puml)
  - 類型: PlantUML
  - 更新頻率: [頻率]

### 詳細圖表
- **[圖表3名稱]**: [圖表描述和用途]
  - 檔案: [../diagrams/viewpoints/[viewpoint]/[diagram3].excalidraw](../diagrams/viewpoints/[viewpoint]/[diagram3].excalidraw)
  - 類型: Excalidraw
  - 更新頻率: [頻率]

### 圖表對應表

| 圖表名稱 | 類型 | 詳細程度 | 目標受眾 | 維護責任 |
|---------|------|---------|---------|---------|
| [圖表1] | Mermaid | 概覽 | [受眾] | [責任人] |
| [圖表2] | PlantUML | 詳細 | [受眾] | [責任人] |

## Relationships with Other Viewpoints

### 直接關聯
- **[其他視點1]**: [關聯描述和依賴關係]
  - 依賴類型: 強依賴/弱依賴/互相依賴
  - 影響程度: 高/中/低
  - 協調機制: [如何協調和同步]

- **[其他視點2]**: [關聯描述和依賴關係]
  - 依賴類型: 強依賴/弱依賴/互相依賴
  - 影響程度: 高/中/低
  - 協調機制: [如何協調和同步]

### 間接關聯
- **[其他視點3]**: [間接影響和考量]
- **[其他視點4]**: [間接影響和考量]

### 視點整合Policy
1. **整合原則**: [如何與其他視點整合]
2. **衝突解決**: [當視點間出現衝突時的解決Policy]
3. **協調機制**: [跨視點協調的具體機制]

## Guidelines

### Design
1. **[原則1]**: [具體描述和應用方式]
2. **[原則2]**: [具體描述和應用方式]
3. **[原則3]**: [具體描述和應用方式]

### Best Practices
1. **[實踐1]**: [詳細說明和範例]
2. **[實踐2]**: [詳細說明和範例]
3. **[實踐3]**: [詳細說明和範例]

### 常見陷阱
1. **[陷阱1]**: [描述和避免方法]
2. **[陷阱2]**: [描述和避免方法]
3. **[陷阱3]**: [描述和避免方法]

### 實現檢查清單
- [ ] [檢查項目1]
- [ ] [檢查項目2]
- [ ] [檢查項目3]
- [ ] [檢查項目4]
- [ ] [檢查項目5]

## Standards

### 完整性驗證
- [ ] 所有Architectural Element都已定義
- [ ] StakeholderConcern都已涵蓋
- [ ] Quality Attribute考量都已評估
- [ ] 相關圖表都已建立

### 一致性驗證
- [ ] 與其他視點保持一致
- [ ] 架構決策相互支持
- [ ] 沒有矛盾或衝突

### 品質驗證
- [ ] 文件清晰易懂
- [ ] 圖表準確反映設計
- [ ] Implementation Guide具體可行
- [ ] Verification Criteria可測量

### Tracing性驗證
- [ ] 需求可Tracing到Architectural Element
- [ ] 架構決策有明確理由
- [ ] 變更影響可評估

## Maintenance

### 更新觸發條件
- [條件1：何時需要更新此視點]
- [條件2：相關變更的影響]
- [條件3：定期審查週期]

### Maintenance
- **主要負責人**: [角色/姓名]
- **審查者**: [角色/姓名]
- **批准者**: [角色/姓名]

### 版本控制
- **版本號規則**: [版本號命名規則]
- **變更記錄**: [如何記錄變更]
- **發布流程**: [文件發布和通知流程]

## Reference

### 相關文件
- \1
- \1
- \1

### Reference
- [參考資料1]
- [參考資料2]
- [參考資料3]

### Standards
- [標準1]
- [標準2]
- [標準3]

---

**文件狀態**: [草稿/審查中/已批准]  
**最後更新**: [YYYY-MM-DD]  
**下次審查**: [YYYY-MM-DD]  
**版本**: [版本號]