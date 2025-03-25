# 設計指南

## Tell, Don't Ask 原則

### 起源與歷史
"Tell, Don't Ask" 原則最早由 Alec Sharp 在 1997 年提出，後來在 Andy Hunt 和 Dave Thomas 的《程序員修煉之道》(The Pragmatic Programmer) 一書中得到廣泛傳播。這個原則強調應該告訴對象要做什麼，而不是詢問對象的狀態後再決定做什麼。

### 核心概念
- 對象應該負責其內部狀態的處理
- 調用者不應該基於對象的內部狀態做出決策
- 封裝不僅是隱藏數據，更是隱藏行為

### 不好的設計示例
```java
// 違反 Tell, Don't Ask
if (order.getStatus() == OrderStatus.PENDING) {
    order.setStatus(OrderStatus.PROCESSING);
    // 處理訂單...
}
```

### 好的設計示例
```java
// 遵循 Tell, Don't Ask
order.process();  // 讓訂單自己處理狀態轉換
```

### 在我們的專案中的應用

1. 應用服務層
```java
// 好的設計：直接告訴服務處理訂單
return orderApplicationService.processOrder(order);

// 不好的設計：檢查結果後再決定做什麼
OrderProcessingResult result = orderProcessingService.process(order);
if (result.success()) {
    // 做這個
} else {
    // 做那個
}
```

2. 領域服務層
```java
// 好的設計：讓領域服務自己處理完整的業務邏輯
public class OrderProcessingService {
    public OrderProcessingResult process(Order order) {
        // 內部處理所有邏輯
    }
}
```

## 其他重要的設計思考點

### 1. 單一職責原則 (SRP)
- 每個類別都應該有一個明確的職責
- 變化的原因應該只有一個
- 例如：將 HTTP 響應轉換邏輯從領域服務移到應用服務

### 2. 分層架構
- 表現層：處理 HTTP 請求/響應
- 應用層：協調不同服務，處理事務
- 領域層：實現核心業務邏輯
- 基礎設施層：提供技術支持

### 3. 關注點分離
- 業務邏輯與技術細節分離
- 領域邏輯與基礎設施關注點分離
- 例如：將 HTTP 相關邏輯從領域服務中分離出來

### 4. 依賴倒置原則 (DIP)
- 高層模組不應依賴低層模組
- 抽象不應依賴細節
- 例如：使用 DomainEvent 介面而不是具體的事件類別

### 5. 封裝
- 隱藏實現細節
- 提供有意義的介面
- 控制變化的影響範圍

## 在領域驅動設計(DDD)中的應用

### Tell, Don't Ask 與界限上下文

在處理跨界限上下文的溝通時，Tell, Don't Ask 原則特別重要：

1. 事件驅動通信
   ```java
   // 不好的設計
   if (order.getStatus() == OrderStatus.CONFIRMED) {
       Payment payment = new Payment(order.getId(), order.getTotalAmount());
       paymentService.process(payment);
   }

   // 好的設計
   order.confirm();  // 內部發布 OrderConfirmedEvent
   // PaymentService 訂閱並處理 OrderConfirmedEvent
   ```

2. 界限上下文的自治
   - 每個上下文負責自己的決策
   - 通過事件通知其他上下文
   - 避免上下文間的直接查詢

3. 防腐層的應用
   ```java
   // 不好的設計：直接暴露外部系統的細節
   ExternalPaymentSystem.PaymentStatus status = externalSystem.getPaymentStatus(id);
   if (status == ExternalPaymentSystem.PaymentStatus.SUCCESS) {
       // 處理邏輯
   }

   // 好的設計：使用防腐層封裝外部系統
   paymentAntiCorruptionLayer.processPayment(payment);
   ```

### 1. 聚合根
- 維護自身的業務規則
- 保護不變條件
- 提供有意義的業務操作

### 2. 領域事件
- 表達領域中發生的事情
- 解耦不同界限上下文
- 支持異步處理

### 3. 值物件
- 描述領域中的概念
- 保持不可變性
- 封裝相關的行為

## 實踐建議

### 設計思考過程示例

以我們的訂單處理功能重構為例，展示如何運用這些設計原則：

1. 問題識別
   ```java
   // 原始程式碼中的問題
   @PostMapping("/{orderId}/process")
   public ResponseEntity<Object> processOrder(@PathVariable String orderId) {
       OrderProcessingResult result = orderProcessingService.process(order);
       if (result.success()) {
           return ResponseEntity.ok(...);
       } else {
           return ResponseEntity.badRequest(...);
       }
   }
   ```
   - 控制器需要了解處理結果的內部狀態
   - 違反了 Tell, Don't Ask 原則
   - 混合了領域邏輯和 HTTP 響應處理

2. 解決方案設計
   - 引入應用服務層處理轉換邏輯
   - 保持領域服務的純粹性
   - 簡化控制器的職責

3. 最終實現
   ```java
   // 控制器：簡單且專注
   @PostMapping("/{orderId}/process")
   public ResponseEntity<Object> processOrder(@PathVariable String orderId) {
       return orderApplicationService.processOrder(new Order(orderId));
   }

   // 應用服務：處理轉換
   public ResponseEntity<Object> processOrder(Order order) {
       OrderProcessingResult result = orderProcessingService.process(order);
       return convertToResponse(result, order);
   }

   // 領域服務：純粹的業務邏輯
   public OrderProcessingResult process(Order order) {
       // 處理領域邏輯
   }
   ```

### 一般設計原則

1. 始終思考「誰應該負責這個決策？」
2. 將行為放在數據所在的地方
3. 使用命令式方法名稱，表達意圖
4. 避免取得對象狀態後再進行判斷
5. 時刻警惕過度暴露內部細節

## 《程序員修煉之道》設計原則總結

### DRY (Don't Repeat Yourself)
- 原則：不要重複自己，每一個知識點在系統中都應該有一個單一、明確的表示
- 違反表現：
  - 重複的代碼片段
  - 重複的業務規則
  - 重複的數據結構
  - 文檔和代碼不同步

### 正交性 (Orthogonality)
- 原則：消除不同組件之間的影響
- 特點：
  - 一個組件的改變不應影響其他無關組件
  - 提高代碼的可重用性和可測試性
  - 降低風險和重構成本

### 可逆性 (Reversibility)
- 原則：重要的設計決策應該保持可逆
- 應用：
  - 使用抽象和介面
  - 避免過度耦合
  - 保持技術選擇的靈活性

### 曳光彈 (Tracer Bullets)
- 原則：通過快速實現來驗證設計
- 特點：
  - 端到端的基本功能實現
  - 持續的反饋和調整
  - 增量式開發

### 保持簡單
- KISS (Keep It Simple and Straightforward)
- 避免過度設計
- 優先選擇簡單明確的解決方案

### 關注分離
- 將不同關注點分開處理
- 避免混合不同層次的細節
- 模塊化和組件化

### 設計契約
- 明確定義介面
- 前置條件和後置條件
- 清晰的錯誤處理

### 快速失敗 (Fail Fast)
- 問題發現得越早越好
- 明確的錯誤處理和報告
- 防禦性編程

## 代碼設計模式（標準解決方案）

這些是解決特定問題的標準設計模式，選擇適當的模式能夠讓代碼更易於維護和擴展：

### 1. 創建型模式

這些模式關注於對象的創建機制，幫助創建適合具體情況的對象。

- **工廠方法 (Factory Method)**
  - 定義用於創建對象的接口，讓子類決定實例化哪個類
  - 例如：`OrderFactory.create()` 可以根據不同參數創建不同類型的訂單

- **建造者模式 (Builder)**
  - 將複雜對象的構建與表示分離，使同一構建過程可創建不同表示
  - 例如：`OrderBuilder.withCustomer("123").withItem(...).build()`

- **單例模式 (Singleton)**
  - 確保一個類只有一個實例，並提供全局訪問點
  - 謹慎使用，因為它可能導致全局狀態和測試困難
  - 例如：`ConfigurationManager.getInstance()`

### 2. 結構型模式

這些模式關注於如何組合類和對象形成更大的結構，同時保持結構的靈活和高效。

- **適配器 (Adapter)**
  - 使接口不兼容的類能一起工作
  - 例如：將第三方支付系統適配到我們的接口

- **裝飾器 (Decorator)**
  - 動態地給對象添加職責，是子類化的靈活替代
  - 例如：`LoggingOrderService` 裝飾 `OrderService` 添加日誌功能

- **組合 (Composite)**
  - 將對象組合成樹結構表示"部分-整體"層次結構
  - 例如：構建菜單系統，每個菜單項可以是子菜單或具體操作

### 3. 行為型模式

這些模式關注於對象之間的責任分配和算法封裝。

- **策略 (Strategy)**
  - 定義一系列算法，使它們可互換
  - 例如：不同的折扣計算策略可以互換使用

- **觀察者 (Observer)**
  - 當一個對象狀態改變時，所有依賴它的對象都會收到通知
  - 例如：訂單狀態變更時通知相關服務

- **命令 (Command)**
  - 將請求封裝為對象，從而參數化客戶端和請求
  - 例如：將訂單操作封裝為命令對象，支持撤銷和重做

## 代碼設計規範（可執行標準）

以下規範應該被視為代碼編寫的強制性標準：

### 1. 命名規範

良好的命名是自文檔化代碼的基礎：

- **類名**
  - 使用名詞或名詞短語
  - 採用 PascalCase（首字母大寫駝峰式）
  - 例如：`OrderService`, `PaymentProcessor`

- **方法名**
  - 使用動詞或動詞短語
  - 採用 camelCase（首字母小寫駝峰式）
  - 例如：`processOrder()`, `calculateTotal()`

- **變量名**
  - 明確表達其用途
  - 採用 camelCase
  - 例如：`currentOrder`, `totalAmount`

- **常量名**
  - 全大寫，下劃線分隔
  - 例如：`MAX_RETRY_COUNT`, `DEFAULT_TIMEOUT`

### 2. 方法設計規範

方法是代碼的基本單位，應遵循以下規範：

- **單一抽象層級原則 (SLAP)**
  - 方法內的所有操作應該處於相同的抽象層級
  - 如果發現混合了不同層級的操作，應提取子方法

- **命令查詢分離 (CQS)**
  - 方法要麼執行動作（命令），要麼返回數據（查詢），不應同時做這兩件事
  - 例如：`saveOrder()` 應只保存訂單而不返回數據，`getOrder()` 應只返回訂單而不修改狀態

- **參數數量限制**
  - 方法參數通常不應超過 3 個
  - 如果需要更多參數，考慮使用參數對象或建造者模式

- **方法長度限制**
  - 方法通常不應超過 20-30 行代碼
  - 過長的方法通常表明需要提取子方法

### 3. 類設計規範

類是面向對象設計的基本單位：

- **單一職責**
  - 每個類應只有一個職責，即一個變化的理由
  - 如果一個類有多個職責，考慮拆分

- **封裝完整性**
  - 盡可能減少公共方法和屬性
  - 所有屬性應該是私有的，通過方法控制訪問

- **類大小限制**
  - 類通常不應超過 300-500 行代碼
  - 過大的類通常表明需要分解為多個類

### 4. 錯誤處理規範

健壯的錯誤處理對系統穩定性至關重要：

- **使用具體異常**
  - 拋出特定業務異常而非通用異常
  - 例如：`OrderNotFoundException` 而非通用 `RuntimeException`

- **異常文檔化**
  - 所有公共方法的可能異常應在文檔中說明
  - 使用 `@throws` 標記說明異常條件

- **統一處理策略**
  - 在應用層統一處理異常，轉換為適當的響應
  - 使用 AOP 或全局異常處理器簡化代碼

## 防禦性編程規範（保護措施）

防禦性編程能夠提高系統的健壯性和安全性：

### 1. 輸入驗證

所有外部輸入都是潛在的威脅：

- **前置條件檢查**
  - 方法開始時驗證所有輸入參數
  - 使用 `Objects.requireNonNull()` 檢查非空參數
  - 例如：`Objects.requireNonNull(order, "Order must not be null");`

- **參數範圍驗證**
  - 檢查數值參數是否在合理範圍內
  - 例如：`if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");`

- **專用驗證組件**
  - 將複雜驗證邏輯提取到專用驗證器中
  - 例如：`orderValidator.validate(order);`

### 2. 不變量保護

保護對象的一致性狀態：

- **不可變對象**
  - 盡可能使用不可變對象
  - 不可變對象本質上是線程安全的
  - 例如：`Money` 類應該是不可變的

- **狀態轉換控制**
  - 通過封裝控制對象狀態的變化
  - 明確定義狀態轉換規則
  - 例如：訂單狀態只能按特定流程轉換

- **後置條件驗證**
  - 方法結束前檢查結果的正確性
  - 確保方法的合約得到滿足

### 3. 防空措施

空引用是常見的錯誤源：

- **使用 Optional**
  - 對可能不存在的值使用 `Optional<T>`
  - 例如：`Optional<Order> findOrder(String id);`

- **空對象模式**
  - 返回特殊的"空"實現而非 null
  - 例如：`NullOrder` 實現 `Order` 接口但不執行實際操作

- **默認值處理**
  - 提供安全的默認值處理機制
  - 例如：`String name = Optional.ofNullable(customerName).orElse("Guest");`

### 4. 並發安全

多線程環境下的防禦措施：

- **不可變設計**
  - 盡可能使用不可變對象避免並發問題
  - 讀多寫少的場景特別適合

- **顯式同步**
  - 當需要修改共享狀態時，使用明確的同步機制
  - 考慮使用 `java.util.concurrent` 包中的工具

- **避免共享狀態**
  - 盡量避免線程間共享可變狀態
  - 使用消息傳遞而非共享內存進行通信

## 專案代碼分析

以下是當前專案中需要注意的幾個地方：

1. DRY 原則違反
   - OrderController 中的錯誤處理邏輯重複
   - 缺少統一的錯誤處理機制

2. 正交性問題
   - 領域服務與 HTTP 響應格式耦合
   - 需要進一步分離關注點

3. 可逆性考慮
   - 外部系統集成缺少抽象層
   - 支付系統集成過於緊密

4. 改進建議
   - 引入統一的錯誤處理機制
   - 增加抽象層來處理外部依賴
   - 完善文檔和注釋
   - 增強防禦性編程

## 參考資源

1. 《程序員修煉之道》(The Pragmatic Programmer) - Andy Hunt & Dave Thomas
2. 《重構》(Refactoring) - Martin Fowler
3. 《領域驅動設計》(Domain-Driven Design) - Eric Evans
4. [Tell, Don't Ask by Alec Sharp](http://pragprog.com/articles/tell-dont-ask)
