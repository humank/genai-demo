# 事件溯源實作

## 事件設計

### 領域事件
```java
public record CustomerCreatedEvent(
    CustomerId customerId,
    CustomerName name,
    Email email,
    Instant occurredOn
) implements DomainEvent {}
```

### 事件儲存
- 事件流設計
- 快照機制
- 事件版本管理

## 事件處理

### 事件處理器
```java
@EventHandler
public class CustomerEventHandler {
    
    @TransactionalEventListener
    public void handle(CustomerCreatedEvent event) {
        // 處理客戶創建事件
        updateReadModel(event);
        sendWelcomeEmail(event);
    }
}
```

### 讀取模型更新
- CQRS 模式實作
- 投影更新策略
- 最終一致性處理

## 事件重播

### 重建投影
- 事件重播機制
- 增量更新策略
- 錯誤恢復處理
