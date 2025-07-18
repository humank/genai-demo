---
inclusion: manual
---

# 文檔翻譯指南

## 翻譯觸發條件
當 git commit message 包含以下關鍵字時觸發自動翻譯：
- `[translate]` - 翻譯所有變更的 .md 檔案
- `[en]` - 翻譯所有變更的 .md 檔案

## 目錄結構規範

### 原始結構 (繁體中文)
```
├── README.md
├── aws-eks-architecture.md
├── microservices-refactoring-plan.md
├── shared-kernel-refactoring.md
└── docs/
    ├── architecture-overview.md
    ├── DesignPrinciple.md
    ├── releases/
    │   ├── README.md
    │   └── *.md
    ├── uml/
    │   ├── README.md
    │   └── *.md
    └── requirements/
        └── promotion-pricing/
            └── *.md
```

### 翻譯後結構
```
├── README.md (保持雙語或指向各語言版本)
├── docs/
    ├── zh-tw/          # 繁體中文版本
    │   ├── README.md
    │   ├── aws-eks-architecture.md
    │   ├── microservices-refactoring-plan.md
    │   ├── shared-kernel-refactoring.md
    │   ├── architecture-overview.md
    │   ├── DesignPrinciple.md
    │   ├── releases/
    │   ├── uml/
    │   └── requirements/
    └── en/             # 英文版本 (自動生成)
        ├── README.md
        ├── aws-eks-architecture.md
        ├── microservices-refactoring-plan.md
        ├── shared-kernel-refactoring.md
        ├── architecture-overview.md
        ├── DesignPrinciple.md
        ├── releases/
        ├── uml/
        └── requirements/
```

## 連結轉換規則

### 1. 相對路徑連結
```markdown
<!-- 中文版本 -->
[架構概覽](docs/architecture-overview.md)
[設計指南](./DesignGuideline.MD)
[發布說明](releases/README.md)

<!-- 轉換為英文版本 -->
[Architecture Overview](docs/en/architecture-overview.md)
[Design Guidelines](./DesignGuideline.MD)
[Release Notes](releases/README.md)
```

### 2. 錨點連結轉換
```markdown
<!-- 中文版本 -->
[Tell, Don't Ask 原則](DesignGuideline.MD#tell-dont-ask-原則)
[專案架構](#專案架構)

<!-- 轉換為英文版本 -->
[Tell, Don't Ask Principle](DesignGuideline.MD#tell-dont-ask-principle)
[Project Architecture](#project-architecture)
```

### 3. 圖片連結處理
```markdown
<!-- 中文版本 -->
![類別圖](./class-diagram.svg)
![六角形架構圖](../images/hexagonal-architecture.png)

<!-- 英文版本 (路徑保持不變，只翻譯 alt text) -->
![Class Diagram](./class-diagram.svg)
![Hexagonal Architecture Diagram](../images/hexagonal-architecture.png)
```

## 翻譯品質要求

### 1. 技術術語一致性
- Domain-Driven Design (DDD) → 領域驅動設計
- Hexagonal Architecture → 六角形架構
- Aggregate Root → 聚合根
- Value Object → 值對象
- Repository → 儲存庫
- Specification → 規格

### 2. 程式碼區塊
- 保持程式碼不變
- 翻譯註解和字串
- 保持變數名稱的英文形式

### 3. 檔案名稱
- 保持檔案名稱不變 (例如: DesignGuideline.MD)
- 只翻譯內容，不翻譯檔案路徑

## 執行流程

1. **檢測變更檔案**: 使用 `git diff --cached --name-only` 找出本次 commit 的 .md 檔案
2. **建立目錄結構**: 確保 `docs/en/` 目錄結構存在
3. **翻譯內容**: 逐一翻譯每個檔案，同時轉換內部連結
4. **驗證連結**: 確保翻譯後的連結指向正確的英文檔案
5. **加入 commit**: 將翻譯後的檔案自動加入到同一個 commit

## 特殊處理

### 1. PlantUML 檔案
- `.puml` 檔案中的中文註解需要翻譯
- 保持 PlantUML 語法結構不變

### 2. 表格內容
- 翻譯表格中的中文內容
- 保持表格格式不變

### 3. 程式碼註解
```java
// 中文註解 → English comment
/* 多行中文註解 → Multi-line English comment */
```

## 錯誤處理

1. 如果目標檔案已存在且較新，詢問是否覆蓋
2. 如果連結指向不存在的檔案，記錄警告但繼續處理
3. 翻譯失敗時，記錄錯誤並跳過該檔案