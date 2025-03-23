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
