<!-- This document needs manual translation from Chinese to English -->
<!-- 此文檔需要從中文手動翻譯為英文 -->

# 重構指南：基於《重構：改善既有代碼的設計》

本文檔整理自 Martin Fowler 的經典著作《重構：改善既有代碼的設計》(Refactoring: Improving the Design of Existing Code)，提供了代碼壞味道、重構技術以及相關設計原則的概述，幫助開發者提升代碼質量。

## 目錄

- [代碼壞味道 (Code Smells)](#代碼壞味道-code-smells)
- [重構技術 (Refactoring Techniques)](#重構技術-refactoring-techniques)
- [設計原則 (Design Principles)](#設計原則-design-principles)
- [重構流程與最佳實踐](#重構流程與最佳實踐)

## 代碼壞味道 (Code Smells)

代碼壞味道是代碼中可能存在問題的跡象，它們通常表明代碼需要重構。

### 代碼的大方向問題

- **重複代碼 (Duplicated Code)**
  - 相同或相似的代碼段出現在多個地方
  - 違反 DRY (Don't Repeat Yourself) 原則
  - 解決方法：Extract Method, Pull Up Method, Form Template Method

- **過長函式 (Long Method)**
  - 函式過於冗長，難以理解和維護
  - 解決方法：Extract Method, Replace Temp with Query, Introduce Parameter Object

- **過大的類 (Large Class)**
  - 一個類承擔過多職責，擁有太多字段和方法
  - 解決方法：Extract Class, Extract Subclass, Extract Interface

- **過長參數列 (Long Parameter List)**
  - 函式參數過多，導致調用和理解困難
  - 解決方法：Introduce Parameter Object, Preserve Whole Object, Replace Parameter with Method

- **發散式修改 (Divergent Change)**
  - 每當發生某種變化，就需要更改同一個類的多個部分
  - 解決方法：Extract Class

- **散彈式修改 (Shotgun Surgery)**
  - 一個變化導致需要修改多個不同的類
  - 解決方法：Move Method, Move Field, Inline Class

- **依戀情結 (Feature Envy)**
  - 一個方法對其他類的興趣超過自己所屬的類
  - 解決方法：Move Method, Extract Method

- **資料泥團 (Data Clumps)**
  - 多個地方出現相同的數據項組合
  - 解決方法：Extract Class, Introduce Parameter Object, Preserve Whole Object

- **基本型別偏執 (Primitive Obsession)**
  - 過度使用基本類型而非小型對象來表示簡單概念
  - 解決方法：Replace Data Value with Object, Replace Type Code with Class, Replace Type Code with Subclasses

- **Switch 陳述式 (Switch Statements)**
  - 同一個 switch 語句散布在多個地方
  - 解決方法：Replace Conditional with Polymorphism, Replace Type Code with Subclasses, Replace Type Code with State/Strategy

- **平行繼承體系 (Parallel Inheritance Hierarchies)**
  - 每當為一個繼承體系添加子類，也必須為另一個繼承體系添加相應子類
  - 解決方法：Move Method, Move Field

- **冗余類 (Lazy Class)**
  - 類的用處不足以證明其存在的必要性
  - 解決方法：Inline Class, Collapse Hierarchy

- **夸夸其談未來性 (Speculative Generality)**
  - 為未來可能出現的需求過早設計的抽象和靈活性
  - 解決方法：Collapse Hierarchy, Inline Class, Remove Parameter, Rename Method

### 代碼具體問題

- **臨時字段 (Temporary Field)**
  - 某個字段僅在特定情況下才有意義
  - 解決方法：Extract Class, Introduce Special Case

- **過度耦合的消息鏈 (Message Chains)**
  - 客戶端請求一個對象，再請求另一個對象，如此形成一連串的關聯
  - 解決方法：Hide Delegate, Extract Method, Move Method

- **中間人 (Middle Man)**
  - 一個類的主要功能是委託給其他類
  - 解決方法：Remove Middle Man, Inline Method, Replace Delegation with Inheritance

- **不當的親密關係 (Inappropriate Intimacy)**
  - 一個類對另一個類的內部細節過於了解
  - 解決方法：Move Method, Move Field, Change Bidirectional Association to Unidirectional, Extract Class, Hide Delegate

- **異曲同工的類 (Alternative Classes with Different Interfaces)**
  - 不同類的方法執行相似功能但有不同簽名
  - 解決方法：Rename Method, Move Method, Extract Superclass

- **不完美的程式庫類 (Incomplete Library Class)**
  - 程式庫類沒有提供所需功能
  - 解決方法：Introduce Foreign Method, Introduce Local Extension

- **純資料類 (Data Class)**
  - 僅包含字段和訪問方法的類，沒有行為
  - 解決方法：Move Method, Encapsulate Field, Encapsulate Collection

- **被拒絕的遺贈 (Refused Bequest)**
  - 子類不需要或不想繼承父類的部分方法或屬性
  - 解決方法：Replace Inheritance with Delegation, Extract Superclass, Extract Interface

- **註釋 (Comments)**
  - 過多註釋可能暗示代碼設計不足夠清晰
  - 解決方法：Extract Method, Rename Method, Introduce Assertion

## 重構技術 (Refactoring Techniques)

### 組合重構

- **提煉函數 (Extract Method)**
  - 將代碼片段提取為可復用的函數
  - 改善可讀性和避免重複

- **內聯函數 (Inline Method)**
  - 將函數的調用替換為函數的實際內容
  - 當函數體比函數名更清晰時使用

- **提煉變數 (Extract Variable)**
  - 將表達式的結果存入變數
  - 使複雜表達式更易理解

- **內聯變數 (Inline Temp)**
  - 將臨時變數替換為其值
  - 簡化不必要的間接引用

- **以查詢取代臨時變數 (Replace Temp with Query)**
  - 將表達式提取到單獨的方法中
  - 使代碼更具可讀性和可復用性

- **分解臨時變數 (Split Temporary Variable)**
  - 為每次賦值創建獨立變數
  - 避免變數承擔多種職責

- **移除對參數的賦值 (Remove Assignments to Parameters)**
  - 使用本地變數而不是修改參數
  - 避免副作用和混淆

- **以函數對象取代函數 (Replace Method with Method Object)**
  - 將複雜方法轉化為一個新的類
  - 便於分解長方法

- **替換算法 (Substitute Algorithm)**
  - 用更清晰的算法替換現有實現
  - 改善可讀性和性能

### 移動特性

- **搬移函數 (Move Method)**
  - 將方法移至更適合的類
  - 改善內聚性和減少耦合

- **搬移字段 (Move Field)**
  - 將字段移至更適合的類
  - 改善數據與行為的配置

- **提煉類 (Extract Class)**
  - 從一個類中分離出職責
  - 創建兩個更內聚的類

- **內聚類 (Inline Class)**
  - 將類的內容併入另一個類
  - 當類失去了存在的意義

- **隱藏委託關係 (Hide Delegate)**
  - 在委託類中創建方法
  - 減少客戶對委託關係的了解

- **移除中間人 (Remove Middle Man)**
  - 直接與真正的對象交互
  - 當委託關係過度簡單時

- **引入外加函數 (Introduce Foreign Method)**
  - 在客戶類中新增函數並傳入服務類實例
  - 當無法修改服務類時

- **引入本地擴展 (Introduce Local Extension)**
  - 創建服務類的子類或包裝類
  - 添加所需功能

### 重新組織數據

- **自封裝字段 (Self Encapsulate Field)**
  - 通過訪問器方法而非直接訪問字段
  - 增加子類的靈活性

- **以對象取代數據值 (Replace Data Value with Object)**
  - 將簡單數據轉化為對象
  - 添加行為或更多結構

- **將值對象改為引用對象 (Change Value to Reference)**
  - 將多個相同對象的副本轉化為引用
  - 確保一致性

- **將引用對象改為值對象 (Change Reference to Value)**
  - 將引用對象轉化為不可變的值對象
  - 簡化代碼並避免副作用

- **以對象取代陣列 (Replace Array with Object)**
  - 用對象替代不同類型元素的陣列
  - 提高代碼的明確性

- **複製被監視數據 (Duplicate Observed Data)**
  - 將領域數據從UI對象中分離出來
  - 改善代碼結構和可測試性

- **將單向關聯改為雙向關聯 (Change Unidirectional Association to Bidirectional)**
  - 添加反向引用
  - 方便雙向導航

- **將雙向關聯改為單向關聯 (Change Bidirectional Association to Unidirectional)**
  - 移除不必要的關聯方向
  - 減少耦合

- **以符號常量取代魔法數字 (Replace Magic Number with Symbolic Constant)**
  - 將硬編碼數值替換為命名常量
  - 提高可讀性和可維護性

- **封裝字段 (Encapsulate Field)**
  - 將公共字段設為私有並提供訪問器
  - 控制訪問並添加行為

- **封裝集合 (Encapsulate Collection)**
  - 提供添加/刪除方法而不直接返回集合
  - 控制集合修改

- **以類取代類型代碼 (Replace Type Code with Class)**
  - 用類替代枚舉或常量
  - 增加類型安全和可擴展性

- **以子類取代類型代碼 (Replace Type Code with Subclasses)**
  - 為每種類型創建子類
  - 利用多態性

- **以狀態/策略取代類型代碼 (Replace Type Code with State/Strategy)**
  - 將類型相關行為移至狀態或策略類
  - 更靈活地改變行為

- **以字段取代子類 (Replace Subclass with Fields)**
  - 將子類特有行為移至父類的字段
  - 簡化不必要的繼承

### 簡化條件表達式

- **分解條件表達式 (Decompose Conditional)**
  - 提取複雜條件為命名方法
  - 使條件更易理解

- **合併條件表達式 (Consolidate Conditional Expression)**
  - 將多個相同結果的檢查合併
  - 突顯檢查的意圖

- **合併重複的條件片段 (Consolidate Duplicate Conditional Fragments)**
  - 將條件分支中的重複代碼移至條件外
  - 減少重複

- **移除控制標記 (Remove Control Flag)**
  - 使用 break 或 return 替代控制標記
  - 簡化循環或條件結構

- **以衛語句取代巢狀條件表達式 (Replace Nested Conditional with Guard Clauses)**
  - 使用提前返回處理特殊情況
  - 減少嵌套和突顯主邏輯

- **以多態取代條件表達式 (Replace Conditional with Polymorphism)**
  - 將條件邏輯移至類層次結構
  - 利用多態實現動態行為

- **引入 Null 對象 (Introduce Null Object)**
  - 用特殊對象替代 null 檢查
  - 避免 null 檢查的擴散

- **引入斷言 (Introduce Assertion)**
  - 添加斷言說明假設
  - 明確表達代碼假設

### 簡化方法調用

- **重新命名方法 (Rename Method)**
  - 使方法名清晰地表達其意圖
  - 改善自文檔化程度

- **添加參數 (Add Parameter)**
  - 在方法簽名中添加參數
  - 提供更多信息

- **移除參數 (Remove Parameter)**
  - 刪除不再使用的參數
  - 簡化方法簽名

- **將查詢函數和修改函數分離 (Separate Query from Modifier)**
  - 分離讀取和修改操作
  - 提高安全性

- **參數化方法 (Parameterize Method)**
  - 將多個類似方法合併為帶參數的單一方法
  - 減少重複代碼

- **引入參數對象 (Introduce Parameter Object)**
  - 將多個參數組織為一個對象
  - 簡化參數列表

- **保持對象完整 (Preserve Whole Object)**
  - 傳遞整個對象而非多個單獨屬性
  - 簡化參數傳遞

- **以明確函數取代參數 (Replace Parameter with Explicit Methods)**
  - 為不同行為創建獨立方法
  - 簡化客戶端代碼

- **引入命名參數 (Introduce Named Parameter)**
  - 使參數的意義更明確
  - 提高可讀性

- **隱藏方法 (Hide Method)**
  - 將不需要被外部調用的方法設為私有
  - 減少公共接口

- **以工廠函數取代構造函數 (Replace Constructor with Factory Method)**
  - 使用工廠方法創建對象
  - 提供更多靈活性

- **封裝向下轉型 (Encapsulate Downcast)**
  - 將轉型移至方法內部
  - 由方法返回正確類型

- **以異常取代錯誤碼 (Replace Error Code with Exception)**
  - 使用異常而非返回錯誤碼
  - 更明確地處理錯誤情況

- **以測試取代異常 (Replace Exception with Test)**
  - 在可能的情況下用條件測試替代異常
  - 避免異常處理的性能開銷

### 處理概括關係

- **字段上移 (Pull Up Field)**
  - 將相同字段移至父類
  - 消除重複

- **方法上移 (Pull Up Method)**
  - 將相同方法移至父類
  - 消除重複

- **構造函數本體上移 (Pull Up Constructor Body)**
  - 將子類構造函數共同部分移至父類
  - 重用初始化代碼

- **方法下移 (Push Down Method)**
  - 將方法移至特定子類
  - 當方法只與某些子類相關

- **字段下移 (Push Down Field)**
  - 將字段移至特定子類
  - 當字段只與某些子類相關

- **提煉子類 (Extract Subclass)**
  - 為特殊行為創建子類
  - 將特殊行為與主類分離

- **提煉父類 (Extract Superclass)**
  - 從類中提取共同特性創建父類
  - 減少重複

- **提煉介面 (Extract Interface)**
  - 將共同的方法簽名提取為介面
  - 明確類的能力

- **折疊繼承體系 (Collapse Hierarchy)**
  - 合併子類和父類
  - 當它們差異不大時

- **塑造模板函數 (Form Template Method)**
  - 在父類中定義算法骨架，在子類中實現特定步驟
  - 重用算法結構

- **以委託取代繼承 (Replace Inheritance with Delegation)**
  - 使用組合代替繼承關係
  - 避免不當繼承造成的問題

- **以繼承取代委託 (Replace Delegation with Inheritance)**
  - 當一個類完全委託給另一個類時使用繼承
  - 簡化代碼

## 設計原則 (Design Principles)

### SOLID 原則

- **單一職責原則 (Single Responsibility Principle, SRP)**
  - 一個類應該只有一個變化的理由
  - 每個類只負責一項職責

- **開放封閉原則 (Open-Closed Principle, OCP)**
  - 對擴展開放，對修改封閉
  - 新功能應該通過擴展實現，而非修改現有代碼

- **里氏替換原則 (Liskov Substitution Principle, LSP)**
  - 子類應該能替換其父類使用
  - 確保繼承正確使用

- **介面隔離原則 (Interface Segregation Principle, ISP)**
  - 客戶端不應被迫依賴於它們不使用的方法
  - 接口應該小而專注

- **依賴倒置原則 (Dependency Inversion Principle, DIP)**
  - 高層模塊不應依賴低層模塊，兩者都應依賴於抽象
  - 抽象不應依賴於細節，細節應依賴於抽象

### 其他重要原則

- **最少知識原則 (Law of Demeter/Principle of Least Knowledge)**
  - 一個對象應該對其他對象了解得最少
  - 減少對象之間的耦合

- **組合優於繼承 (Composition Over Inheritance)**
  - 優先使用對象組合而非繼承
  - 提高靈活性和減少耦合

- **高內聚，低耦合 (High Cohesion, Low Coupling)**
  - 相關功能應集中在同一模塊
  - 不同模塊之間的依賴應最小化

- **DRY原則 (Don't Repeat Yourself)**
  - 避免重複代碼和知識
  - 每一塊知識應在系統中有一個明確的表示

- **YAGNI原則 (You Aren't Gonna Need It)**
  - 不要為未來可能的需求開發功能
  - 專注於當前確定的需求

- **关注点分离 (Separation of Concerns)**
  - 不同功能和責任應該被分離
  - 一個組件應只關注其主要職責

## 重構流程與最佳實踐

### 重構流程

- **確保測試覆蓋**
  - 重構前確保有充分的測試
  - 每次小步重構後運行測試

- **小步前進**
  - 進行小而安全的變更
  - 頻繁測試，確保不引入錯誤

- **一次只解決一個問題**
  - 避免同時進行多項重構
  - 保持變更的可管理性

- **重構與新功能開發分離**
  - 不要在添加新功能的同時重構
  - 清晰區分這兩種活動

### 重構的時機

- **添加功能時**
  - 使代碼更容易理解和修改
  - 為新功能做好準備

- **修復錯誤時**
  - 改善代碼結構使問題更明顯
  - 防止未來類似問題

- **代碼審查時**
  - 基於團隊反饋進行改進
  - 統一代碼風格和結構

- **定期的維護活動**
  - 專門的重構時段
  - 處理技術債務

### 實施注意事項

- **團隊合作**
  - 與團隊成員討論重要重構
  - 分享重構策略和學習

- **文檔與溝通**
  - 記錄重大重構的原因和方法
  - 告知相關團隊成員

- **版本控制**
  - 頻繁提交小的變更
  - 使用有意義的提交信息

- **監控性能與資源使用**
  - 確保重構不會引入性能問題
  - 測試不同場景下的效能

## 總結

重構是軟體開發的重要實踐，它幫助我們持續改進代碼質量，使系統更容易理解、更容易維護和更能應對變化。通過識別代碼壞味道，應用適當的重構技術，並遵循良好的設計原則，我們可以創建健壯、靈活且可持續發展的軟體系統。

重構不是一次性活動，而是開發過程中的持續實踐。合理的重構可以降低技術債務，提高團隊效率，並為業務需求的快速實現奠定基礎。
