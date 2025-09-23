
# Design

This document整理了軟體設計領域中的經典書籍及其核心概念，作為 DesignGuideline.MD 的補充資料。這些書籍涵蓋了從面向對象Design Principle到Architectural Pattern的廣泛知識，為Developer提供了深入理解軟體設計的寶貴Resource。

## 目錄

1. [面向對象Design Principle](#面向對象Design Principle)
2. [Domain-Driven Design](#Domain-Driven Design)
3. [代碼質量與Refactoring](#代碼質量與Refactoring)
4. [Architecture Design](#Architecture Design)
5. [特定Design Principle深入探討](#特定Design Principle深入探討)
6. [實用技術實現](#實用技術實現)

## Design

### Design
**作者**: Erich Gamma, Richard Helm, Ralph Johnson, John Vlissides (Gang of Four)

#### 核心概念
- **Design Pattern分類**：創建型、結構型和行為型模式
- **組合優於繼承**：優先使用對象組合而非繼承來實現代碼復用
- **針對接口編程**：依賴抽象而非具體實現
- **最少知識原則**：一個對象應該對其他對象有最少的了解

#### 關鍵模式
1. **單一職責原則 (SRP)**
   - 一個類應該只有一個變化的理由
   - 實例：將訂單處理和訂單顯示分離為不同類

2. **開放封閉原則 (OCP)**
   - 軟件Entity應該對擴展開放，對修改封閉
   - 實例：使用Policy模式實現不同的折扣計算方法

3. **里氏替換原則 (LSP)**
   - 子類型必須能夠替換其基類型
   - 實例：所有支付方式實現相同的支付接口

4. **接口隔離原則 (ISP)**
   - Customer端不應該依賴它不使用的接口
   - 實例：將大型接口分解為多個特定用途的小接口

5. **依賴倒置原則 (DIP)**
   - 高層模塊不應該依賴低層模塊，兩者都應該依賴抽象
   - 實例：業務邏輯依賴儲存庫接口而非具體實現

### 《敏捷軟件開發：原則、模式與實踐》(Agile Software Development, Principles, Patterns, and Practices)
**作者**: Robert C. Martin

#### 核心概念
- **SOLID 原則**：五個面向對象Design Principle的縮寫
- **包Design Principle**：如何組織和設計包結構
- **組件Aggregate**：如何將類組織成有凝聚力的組件
- **設計圖**：使用 UML 和其他圖表來表達設計意圖

#### 實踐要點
1. **包Design Principle**
   - 發布等價原則 (REP)：重用的粒度就是發布的粒度
   - 共同封閉原則 (CCP)：同一個原因修改的類應該在同一個包中
   - 共同重用原則 (CRP)：不會一起重用的類不應該放在同一個包中

2. **設計實踐**
   - 迭代開發：小步前進，持續改進
   - Test-Driven Development (TDD)：先寫測試，再實現功能
   - Refactoring：持續改進代碼結構

## Design

### Design
**作者**: Eric Evans

#### 核心概念
- **通用語言**：開發團隊和領域專家共享的語言
- **Bounded Context**：模型的應用邊界，在其中特定模型有明確定義
- **領域模型**：對業務概念和規則的抽象表示
- **戰略設計**：關注大局，如何劃分和集成不同的模型

#### 關鍵模式
1. **Entity (Entity)**
   - 具有唯一標識的對象
   - 特點：可變，有生命週期，需要被Tracing
   - 實例：`Order`、`Customer`、`Product`

2. **Value Object (Value Object)**
   - 沒有概念上的標識，用屬性定義的對象
   - 特點：不可變，可替換，無副作用
   - 實例：`Money`、`Address`、`DateRange`

3. **Aggregate (Aggregate)**
   - 一組相關對象的集合，視為一個單元
   - 特點：有一個根Entity，保證一致性邊界
   - 實例：`Order`（根）及其 `OrderItems`

4. **Domain Service (Domain Service)**
   - 表示領域中的操作，而非事物
   - 特點：無狀態，處理跨Entity的業務邏輯
   - 實例：`TransferService`、`PricingService`

5. **Domain Event (Domain Event)**
   - 表示領域中發生的事情
   - 特點：不可變，表達過去發生的事實
   - 實例：`OrderPlacedEvent`、`PaymentReceivedEvent`

### Design
**作者**: Vaughn Vernon

#### 核心概念
- **戰術Design Pattern**：Entity、Value Object、Aggregate等的具體實現
- **Context Mapping**：不同Bounded Context之間的關係
- **Event-Driven Architecture**：使用Domain Event實現系統集成
- **Command Query Responsibility Segregation (Command Query Responsibility Segregation (CQRS))**：Command查詢職責分離模式

#### 實踐要點
1. **AggregateDesign Principle**
   - 根據不變條件設計Aggregate
   - 盡量保持Aggregate小型化
   - 通過標識引用其他Aggregate

2. **Context MappingPolicy**
   - Anti-Corruption Layer (ACL)：隔離External System的影響
   - Open Host Service (OHS)：提供 API 給其他上下文
   - Published Language (PL)：多個上下文共享的通用語言

3. **Event Sourcing**
   - 存儲狀態變化而非最終狀態
   - 通過重放事件重建Aggregate狀態
   - 提供完整的審計和歷史記錄

## 代碼質量與Refactoring

### Design
**作者**: Martin Fowler

#### 核心概念
- **Refactoring定義**：在不改變代碼外部行為的前提下改善內部結構
- **代碼氣味**：表明代碼可能存在問題的徵兆
- **Refactoring技術**：一系列小步驟的代碼轉換
- **測試保障**：確保Refactoring不破壞功能

#### 常見Refactoring手法
1. **提取方法 (Extract Method)**
   - 將代碼片段提取為獨立方法
   - 目的：提高可讀性和復用性
   - 時機：當方法過長或一段代碼有明確意圖

2. **移動方法 (Move Method)**
   - 將方法從一個類移到另一個類
   - 目的：提高內聚性
   - 時機：當方法與其他類的數據交互更多

3. **替換條件表達式 (Replace Conditional with Polymorphism)**
   - 用多態替代條件邏輯
   - 目的：消除 switch/if 語句，增加擴展性
   - 時機：當條件邏輯基於對象類型

4. **提取類 (Extract Class)**
   - 將相關功能移到新類中
   - 目的：提高單一職責
   - 時機：當一個類有多個職責

### 《代碼整潔之道》(Clean Code)
**作者**: Robert C. Martin

#### 核心概念
- **整潔代碼的特徵**：可讀、簡單、直接
- **有意義的命名**：名稱應該揭示意圖
- **函數設計**：小型、單一職責、抽象層級一致
- **註釋使用**：好代碼勝過好註釋

#### 實踐要點
1. **命名原則**
   - 使用描述性名稱
   - 避免誤導性縮寫
   - 使用可搜索的名稱
   - 類名應為名詞，方法名應為動詞

2. **函數設計**
   - 函數應該短小（通常不超過20行）
   - 只做一件事
   - 參數越少越好（理想是0-2個）
   - 避免副作用

3. **錯誤處理**
   - 使用異常而非返回碼
   - 提供有意義的錯誤信息
   - 定義異常類別
   - 不返回或傳遞 null

### 《程序員修煉之道》(The Pragmatic Programmer)
**作者**: Andy Hunt & Dave Thomas

#### 核心概念
- **DRY 原則**：不要重複自己
- **正交性**：消除不必要的依賴
- **可逆性**：保持Design Decision的可逆性
- **曳光彈**：快速構建可工作的系統骨架

#### 實踐要點
1. **破窗理論**
   - 不容忍"破窗"（低質量代碼）
   - 持續維護代碼質量
   - 定期Refactoring和改進

2. **原型與學習**
   - 使用原型探索解決方案
   - 持續學習新技術和方法
   - 批判性思考和質疑Assumption

3. **自動化**
   - 自動化構建和測試
   - 自動化Deployment流程
   - 創建有用的工具

## Design

### 《企業應用Architectural Pattern》(Patterns of Enterprise Application Architecture)
**作者**: Martin Fowler

#### 核心概念
- **Layered Architecture**：表現層、Domain Layer、數據層
- **領域邏輯模式**：事務腳本、領域模型、表模塊
- **數據源架構**：活動記錄、數據映射器
- **對象關係行為**：延遲加載、身份映射

#### 關鍵模式
1. **領域模型 (Domain Model)**
   - 將業務邏輯組織為對象網絡
   - 特點：反映業務概念，封裝業務規則
   - 適用：複雜業務邏輯

2. **數據映射器 (Data Mapper)**
   - 將對象與數據庫表分離
   - 特點：領域對象不知道持久化細節
   - 適用：複雜領域模型

3. **服務層 (Service Layer)**
   - 定義應用的邊界和入口點
   - 特點：協調多個領域對象，處理事務
   - 適用：需要 API 的應用

4. **單元工作 (Unit of Work)**
   - 跟踪業務事務中的變更
   - 特點：延遲更新，批量提交
   - 適用：需要事務一致性的場景

### 《整潔架構》(Clean Architecture)
**作者**: Robert C. Martin

#### 核心概念
- **依賴規則**：依賴方向始終指向內層
- **Entity層**：核心業務規則
- **用例層**：應用特定業務規則
- **接口適配層**：轉換數據格式
- **框架和驅動層**：外部細節

#### Architectural Principle
1. **獨立於框架**
   - 架構不依賴於任何外部庫或框架
   - 框架是工具，而非系統的Constraint

2. **Testability**
   - 業務規則可以在沒有 UI、數據庫等的情況下測試
   - 測試不依賴於外部元素

3. **獨立於 UI**
   - UI 可以輕易更改而不影響系統其他部分
   - 業務邏輯不依賴於 UI

4. **獨立於數據庫**
   - 可以替換數據庫而不影響業務規則
   - 業務Entity不知道持久化細節

### 《軟件架構的藝術》(The Art of Software Architecture)
**作者**: Richard N. Taylor, Nenad Medvidović, Eric M. Dashofy

#### 核心概念
- **架構風格**：系統組織的基本模式
- **架構描述語言**：正式描述架構的語言
- **Architecture Assessment**：評估Architecture Design的方法
- **架構演化**：如何管理架構的變化

#### 架構風格
1. **Pipeline過濾器 (Pipe and Filter)**
   - 數據流經一系列處理組件
   - 適用：數據處理系統

2. **分層系統 (Layered Systems)**
   - 將系統組織為抽象層
   - 適用：大多數企業應用

3. **事件驅動系統 (Event-driven Systems)**
   - 組件通過事件通信
   - 適用：高度解耦的系統

4. **Microservices Architecture (Microservices)**
   - 將系統分解為獨立Deployment的服務
   - 適用：大型、複雜系統

## Design

### 《Tell, Don't Ask: Demeter's Law》
**作者**: Brett L. Schuchert

#### 核心概念
- **迪米特法則**：一個對象應該對其他對象有最少的了解
- **Tell, Don't Ask 原則**：告訴對象做什麼，而不是詢問其狀態後做決定
- **行為封裝**：將行為放在數據所在的地方
- **責任分配**：將責任分配給擁有必要信息的對象

#### 實踐要點
1. **方法鏈問題**
   - 避免 `a.getB().getC().doSomething()`
   - 替代方案：`a.doSomethingWithC()`

2. **行為位置**
   - 行為應該放在數據所在的地方
   - 避免"數據類"和"操作類"的分離

3. **消息傳遞**
   - 面向對象編程是關於消息傳遞
   - 對象應該通過消息協作，而非直接操作其他對象的內部

### 《Object Thinking》
**作者**: David West

#### 核心概念
- **對象思維**：從對象的角度思考問題
- **行為優先**：先考慮對象的責任和行為，再考慮其屬性
- **擬人化**：將對象視為有能力和責任的"人"
- **協作**：系統是對象之間的協作網絡

#### 實踐要點
1. **CRC 卡片**
   - 類-責任-協作者卡片
   - 用於識別對象、其責任和協作者
   - 促進團隊討論和設計

2. **責任驅動設計**
   - 從系統責任開始
   - 將責任分配給適當的對象
   - 識別協作關係

3. **對象自治**
   - 對象應該控制自己的狀態和行為
   - 最小化對象間的依賴
   - 通過明確定義的接口進行協作

### 《Growing Object-Oriented Software, Guided by Tests》
**作者**: Steve Freeman, Nat Pryce

#### 核心概念
- **Test-Driven Development (TDD) (TDD)**：先寫測試，再實現功能
- **對象導向設計**：通過測試驅動良好的對象設計
- **模擬對象**：使用模擬對象測試對象間的交互
- **演進式設計**：通過小步驟逐漸改進設計

#### 實踐要點
1. **外部質量與內部質量**
   - 外部質量：系統的功能和Performance
   - 內部質量：代碼的Maintainability和靈活性
   - 兩者同等重要

2. **聆聽測試**
   - 測試難寫通常表明設計有問題
   - 使用測試困難作為Refactoring的信號

3. **模擬與存根**
   - 模擬對象：驗證交互
   - 存根對象：提供測試數據
   - 何時使用真實對象，何時使用測試替身

## 實用技術實現

### 《Effective Java》
**作者**: Joshua Bloch

#### 核心概念
- **Java Best Practice**：在 Java 中實現Design Principle的具體方法
- **API 設計**：如何設計清晰、一致的 API
- **Performance優化**：如何在不犧牲設計質量的前提下優化Performance
- **並發編程**：如何安全地處理多線程

#### 實踐要點
1. **創建和銷毀對象**
   - 考慮使用靜態Factory方法代替Construct器
   - 使用建造者模式處理多參數
   - 通過私有Construct器強化單例屬性

2. **所有對象的通用方法**
   - 正確覆寫 equals 和 hashCode
   - 始終覆寫 toString
   - 謹慎覆寫 clone

3. **類和接口**
   - 使類和成員的可訪問性最小化
   - 優先使用組合而非繼承
   - 接口優於抽象類

4. **泛型**
   - 不要使用原生態類型
   - 消除未檢查警告
   - 優先考慮泛型方法

### 《Java 8 實戰》(Java 8 in Action)
**作者**: Raoul-Gabriel Urma, Mario Fusco, Alan Mycroft

#### 核心概念
- **函數式編程**：在 Java 中應用函數式思想
- **流處理**：使用 Stream API 處理集合
- **默認方法**：在接口中提供默認實現
- **Optional**：處理可能為空的值

#### 實踐要點
1. **Lambda 表達式**
   - 用於簡化匿名類
   - 提高代碼可讀性
   - 促進函數式風格

2. **Stream API**
   - 聲明式數據處理
   - 支持並行處理
   - 提高代碼表達力

3. **Optional 類型**
   - 明確表達可能缺失的值
   - 避免 NullPointerException
   - 強制Customer端處理空值情況

### 《函數式編程思想》(Functional Thinking)
**作者**: Neal Ford

#### 核心概念
- **函數式範式**：將計算視為函數評估
- **不可變性**：避免狀態變化
- **高階函數**：函數作為參數和返回值
- **組合**：通過組合小函數構建複雜功能

#### 實踐要點
1. **避免副作用**
   - 函數不應修改外部狀態
   - 相同輸入總是產生相同輸出
   - 提高Testability和可推理性

2. **函數組合**
   - 通過組合小函數構建複雜功能
   - 使用Pipeline處理數據轉換
   - 提高代碼復用性

3. **惰性求值**
   - 延遲計算直到需要結果
   - 避免不必要的計算
   - 支持無限數據結構

## summary

這些經典書籍提供了軟體設計的全面視角，從基本原則到具體實現。通過學習和應用這些知識，Developer可以創建更加健壯、可維護和靈活的軟體系統。

關鍵是理解這些原則背後的思想，而不僅僅是機械地應用模式和技術。好的設計來自於深入理解問題領域，並選擇適合特定情境的解決方案。

## 延伸閱讀

1. 《修改代碼的藝術》(Working Effectively with Legacy Code) - Michael Feathers
2. 《持續交付》(Continuous Delivery) - Jez Humble, David Farley
3. 《架構整潔之道》(Clean Architecture) - Robert C. Martin
4. 《Test-Driven Development (TDD)》(Test-Driven Development) - Kent Beck
5. 《領域特定語言》(Domain-Specific Languages) - Martin Fowler
