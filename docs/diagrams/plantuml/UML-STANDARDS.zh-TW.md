# UML 2.5 標準化指南

本文檔定義了專案中所有 UML 圖表的標準化規範，基於 UML 2.5 標準和 PlantUML 最新語法。

## 標準化原則

### 1. 配色標準

所有 UML 圖表使用統一的配色方案，定義在 `uml-2.5-colors.puml` 中：

| 元素類型 | 顏色 | 用途 |
|---------|------|------|
| 聚合根 | #LightSalmon | DDD 聚合根實體 |
| 實體 | #LightBlue | 具有唯一標識的對象 |
| 值對象 | #LightGreen | 不可變的值對象 |
| 領域事件 | #LightYellow | 業務事件 |
| 服務 | #LightGray | 應用服務和領域服務 |
| 儲存庫 | #LightCyan | 資料存取介面 |
| 工廠 | #LightPink | 對象創建工廠 |
| 策略 | #LightGoldenRodYellow | 業務策略模式 |
| 規格 | #LightSkyBlue | 業務規格模式 |

### 2. 語法標準

#### 類圖標準語法

```plantuml
@startuml 類圖標題 - UML 2.5 標準化版本
!theme plain
!include uml-2.5-colors.puml

package "package.name" <<Rectangle>> {
  class ClassName <<Stereotype>> {
    -privateField: Type
    #protectedField: Type
    +publicField: Type
    --
    +publicMethod(param: Type): ReturnType
    -privateMethod(): void
    --
    +{static} staticMethod(): Type
  }
}
@enduml
```

#### 時序圖標準語法

```plantuml
@startuml 時序圖標題 - UML 2.5 標準化版本
!theme plain
!include uml-2.5-colors.puml

actor "參與者" as Actor
participant "控制器" as Controller
participant "服務" as Service
participant "儲存庫" as Repository

Actor -> Controller: 請求(參數: Type)
activate Controller

Controller -> Service: 處理(命令: Command)
activate Service

Service -> Repository: 保存(實體: Entity)
activate Repository
Repository --> Service: 結果: Result
deactivate Repository

Service --> Controller: 響應: Response
deactivate Service

Controller --> Actor: 結果: Result
deactivate Controller
@enduml
```

### 3. 關係標準

| 關係類型 | UML 符號 | PlantUML 語法 | 說明 |
|---------|----------|---------------|------|
| 組合 | ♦—— | `*--` | 強擁有關係，生命週期依賴 |
| 聚合 | ◊—— | `o--` | 弱擁有關係，可獨立存在 |
| 關聯 | ——> | `-->` | 引用關係，知道對方存在 |
| 依賴 | ····> | `..>` | 使用關係，臨時性依賴 |
| 泛化 | ——▷ | `--|>` | 繼承關係 |
| 實現 | ····▷ | `..|>` | 介面實現關係 |

### 4. 多重性標記

```plantuml
' 標準多重性標記
ClassA "1" *-- "*" ClassB : contains
ClassC "0..1" --> "1..*" ClassD : references
ClassE "1" o-- "0..5" ClassF : aggregates
```

## 圖表類型標準

### 1. 類圖 (Class Diagram)

**用途**: 展示系統的靜態結構，包括類、介面、關係

**標準元素**:
- 使用 `<<AggregateRoot>>` 標記聚合根
- 使用 `<<ValueObject>>` 標記值對象
- 使用 `<<DomainEvent>>` 標記領域事件
- 使用 `<<Service>>` 標記服務類
- 使用 `<<Repository>>` 標記儲存庫介面

**範例**: `class-diagram.puml`

### 2. 時序圖 (Sequence Diagram)

**用途**: 展示對象間的互動時序

**標準元素**:
- 使用 `actor` 表示外部參與者
- 使用 `participant` 表示系統組件
- 使用 `activate`/`deactivate` 表示生命線
- 使用 `note` 添加重要說明

**範例**: `sequence-diagram.puml`

### 3. 領域模型圖 (Domain Model Diagram)

**用途**: 展示完整的領域模型結構

**標準元素**:
- 按界限上下文分組
- 清晰標記 DDD 戰術模式
- 包含完整的關係和多重性
- 添加業務規則說明

**範例**: `domain-model-diagram.puml`

## 命名規範

### 1. 檔案命名

```
{diagram-type}-{context}-{version}.puml
```

範例:
- `class-diagram-order-context.puml`
- `sequence-diagram-payment-flow.puml`
- `domain-model-complete.puml`

### 2. 類別命名

```plantuml
' 聚合根
class OrderAggregate <<AggregateRoot>>

' 值對象
class OrderId <<ValueObject>>

' 領域事件
class OrderCreatedEvent <<DomainEvent>>

' 服務
class OrderApplicationService <<Service>>

' 儲存庫
interface OrderRepository <<Repository>>
```

### 3. 方法命名

```plantuml
class Order {
  ' 查詢方法
  +getId(): OrderId
  +getStatus(): OrderStatus
  
  ' 命令方法
  +addItem(item: OrderItem): void
  +submit(): void
  +cancel(): void
  
  ' 工廠方法
  +{static} create(customerId: String): Order
}
```

## 最佳實踐

### 1. 圖表組織

- **單一職責**: 每個圖表專注於一個特定的視角或用例
- **適當大小**: 避免過於複雜的圖表，必要時拆分為多個圖表
- **一致性**: 在所有圖表中使用相同的命名和樣式

### 2. 註解和文檔

```plantuml
note right of Order
  <b>訂單聚合根</b>
  - 維護訂單一致性
  - 發布領域事件
  - 實現業務規則
end note
```

### 3. 圖例說明

每個圖表都應包含圖例，說明使用的符號和顏色：

```plantuml
legend right
  |= 元素類型 |= 顏色 |= 說明 |
  | <back:#LightSalmon>   </back> | 聚合根 | DDD 聚合根實體 |
  | <back:#LightBlue>   </back> | 實體 | 具有唯一標識的對象 |
  | <back:#LightGreen>   </back> | 值對象 | 不可變的值對象 |
endlegend
```

## 工具和自動化

### 1. PlantUML 配置

```bash
# 生成 PNG 圖片
java -jar tools-and-environment/plantuml.jar -tpng docs/diagrams/plantuml/*.puml

# 生成 SVG 圖片
java -jar tools/plantuml.jar -tsvg docs/diagrams/plantuml/*.puml

# 檢查語法
java -jar tools/plantuml.jar -checkonly docs/diagrams/plantuml/*.puml
```

### 2. 自動化腳本

```bash
#!/bin/bash
# scripts/generate-diagrams.sh

echo "生成標準化 UML 圖表..."

# 生成所有 PlantUML 圖表
find docs/diagrams/plantuml -name "*.puml" -exec java -jar tools/plantuml.jar -tpng {} \;

echo "圖表生成完成！"
```

### 3. Git Hook 整合

```bash
#!/bin/bash
# .git/hooks/pre-commit

# 檢查 PlantUML 語法
java -jar tools/plantuml.jar -checkonly docs/diagrams/plantuml/*.puml

if [ $? -ne 0 ]; then
    echo "PlantUML 語法錯誤，請修正後再提交"
    exit 1
fi
```

## 品質檢查清單

### 圖表品質檢查

- [ ] 使用標準配色方案
- [ ] 包含適當的圖例說明
- [ ] 關係標記正確且完整
- [ ] 命名規範一致
- [ ] 包含必要的註解說明
- [ ] 圖表大小適中，不過於複雜
- [ ] 語法符合 UML 2.5 標準

### 技術品質檢查

- [ ] PlantUML 語法正確
- [ ] 可以成功生成 PNG/SVG
- [ ] 檔案命名符合規範
- [ ] 包含版本和更新日期
- [ ] 相關文檔已更新

## 相關資源

- [PlantUML 官方文檔](https://plantuml.com/)
- [UML 2.5 規範](https://www.omg.org/spec/UML/2.5/)
- [DDD 戰術模式](https://martinfowler.com/bliki/DomainDrivenDesign.html)
- [專案 DDD 指南](../../design/ddd-guide.md)
- [架構決策記錄](../../architecture/adr/)