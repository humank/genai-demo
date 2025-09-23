<!-- 
此文件需要手動翻譯
原文件: tools-and-environment/README.md
翻譯日期: Thu Aug 21 22:02:37 CST 2025

請將以下中文內容翻譯為英文，保持 Markdown 格式不變
-->

# Tools 目錄

此目錄包含專案使用的工具和實用程式。

## 檔案說明

- `plantuml.jar` - PlantUML 工具，用於生成 UML 圖表

## 使用方式

```bash
# 生成 UML 圖表
java -jar tools/plantuml.jar docs/uml/*.puml
```

## PlantUML 使用說明

PlantUML 是一個開源工具，可以從文字描述生成 UML 圖表。

### 支援的圖表類型

- 序列圖 (Sequence Diagram)
- 用例圖 (Use Case Diagram)
- 類別圖 (Class Diagram)
- 活動圖 (Activity Diagram)
- 組件圖 (Component Diagram)
- 狀態圖 (State Diagram)

### Examples

```bash
# 生成所有 UML 圖表
java -jar tools-and-environment/plantuml.jar docs/uml/*.puml

# 生成特定圖表
java -jar tools/plantuml.jar docs/uml/architecture.puml
```


<!-- 翻譯完成後請刪除此註釋 -->
