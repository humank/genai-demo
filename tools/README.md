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

### 範例

```bash
# 生成所有 UML 圖表
java -jar tools/plantuml.jar docs/uml/*.puml

# 生成特定圖表
java -jar tools/plantuml.jar docs/uml/architecture.puml
```
