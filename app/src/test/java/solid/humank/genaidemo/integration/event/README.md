# 領域事件整合測試

本目錄包含領域事件處理機制的整合測試，用於驗證事件發布、訂閱和處理的完整流程。

## 測試類說明

### 1. DomainEventPublishingIntegrationTest

測試領域事件發布機制，包括：

- 直接發布單個事件
- 發布多個事件
- 事件處理的順序性

### 2. EventSubscriptionIntegrationTest

測試事件訂閱機制，包括：

- 事件訂閱註解的正確註冊
- 事件訂閱和處理的匹配
- 事件訂閱的特定性（確保事件只被相應的處理器處理）

### 3. BusinessFlowEventIntegrationTest

測試業務流程中的事件處理，包括：

- 訂單創建流程中的事件發布和處理
- 訂單狀態變更是否正確反映事件處理結果
- 異常情況下的事件處理

### 4. EventHandlingPerformanceTest

測試事件處理的性能和穩定性，包括：

- 批量事件處理性能
- 事件處理的並發性

## 運行測試

可以通過以下方式運行測試：

1. 使用IDE直接運行測試類
2. 使用Gradle命令運行：

```bash
./gradlew test --tests "solid.humank.genaidemo.integration.event.*"
```

## 測試策略

這些測試採用了以下策略：

1. **使用@SpyBean監控事件處理器**：保留原始實現，同時允許驗證方法調用
2. **事件捕獲**：通過替換eventPublisherAdapter的publish方法來捕獲發布的事件
3. **超時驗證**：使用timeout參數處理異步事件
4. **業務流程驗證**：通過執行完整的業務操作來觸發事件流
5. **性能測試**：使用CountDownLatch等待異步事件處理完成

## 注意事項

1. 這些測試需要啟動完整的Spring上下文，因此運行時間較長
2. 性能測試中的參數（如事件數量）可能需要根據實際環境調整
3. 測試可能會受到系統資源和環境因素的影響