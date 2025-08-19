// 比較分析：Abstract Class vs Interface Default Methods

// ========== 現有設計 (Abstract Class) ==========

// 1. 現有的 AggregateRoot 抽象類別
public abstract class AggregateRoot {
    private final DomainEventCollector eventCollector = new SimpleDomainEventCollector();
    
    // 所有方法都是具體實作，沒有抽象方法需要 override
    protected void collectEvent(DomainEvent event) { /* 實作 */ }
    public List<DomainEvent> getUncommittedEvents() { /* 實作 */ }
    public void markEventsAsCommitted() { /* 實作 */ }
    public boolean hasUncommittedEvents() { /* 實作 */ }
    public void clearEvents() { /* 實作 */ }
}

// 2. 現有的 AbstractDomainEvent（這個才有抽象方法）
public abstract class AbstractDomainEvent implements DomainEvent {
    private final UUID eventId;
    private final LocalDateTime occurredOn;
    
    // 這個方法需要子類 override
    @Override
    public abstract String getAggregateId();  // <-- 這裡需要實作者 override
    
    @Override
    public abstract String getEventType();    // <-- 這裡也需要實作者 override
}

// 3. 現有聚合根的使用方式
@AggregateRoot(name = "Customer")
public class Customer extends AggregateRoot {  // 繼承抽象類別
    private final CustomerId id;
    
    public void updateProfile() {
        // 直接調用父類方法
        collectEvent(new CustomerProfileUpdatedEvent(...));
    }
}

// 4. 現有事件的使用方式
public class CustomerCreatedEvent extends AbstractDomainEvent {  // 繼承抽象類別
    private final CustomerId customerId;
    
    // 必須實作抽象方法
    @Override
    public String getAggregateId() {
        return customerId.toString();
    }
    
    @Override
    public String getEventType() {
        return "CustomerCreated";
    }
}

// ========== 新設計 (Interface Default Methods) ==========

// 1. 新的 AggregateRoot 介面
public interface AggregateRoot {
    // 需要實作者提供事件收集器
    DomainEventCollector getEventCollector();  // <-- 這裡需要實作者提供
    
    // 預設實作，不需要 override
    default void collectEvent(DomainEvent event) {
        getEventCollector().collectEvent(event);
    }
    
    default List<DomainEvent> getUncommittedEvents() {
        return getEventCollector().getUncommittedEvents();
    }
    
    default void markEventsAsCommitted() {
        getEventCollector().markEventsAsCommitted();
    }
    
    default boolean hasUncommittedEvents() {
        return getEventCollector().hasUncommittedEvents();
    }
    
    default void clearEvents() {
        getEventCollector().markEventsAsCommitted();
    }
}

// 2. 新的 DomainEvent 介面（取代 AbstractDomainEvent）
public interface DomainEvent extends Serializable {
    LocalDateTime getOccurredOn();
    String getEventType();
    String getAggregateId();
    
    // 可以提供預設的 eventId 實作
    default UUID getEventId() {
        return UUID.randomUUID();  // 或者使用其他策略
    }
}

// 3. 新聚合根的使用方式
@AggregateRoot(name = "Customer")
public class Customer implements AggregateRoot {  // 實作介面
    private final CustomerId id;
    private final DomainEventCollector eventCollector = new SimpleDomainEventCollector();
    
    // 必須實作介面方法
    @Override
    public DomainEventCollector getEventCollector() {  // <-- 需要實作
        return eventCollector;
    }
    
    public void updateProfile() {
        // 調用介面預設方法
        collectEvent(new CustomerProfileUpdatedEvent(...));
    }
}

// 4. 新事件的使用方式
public class CustomerCreatedEvent implements DomainEvent {  // 實作介面
    private final UUID eventId;
    private final LocalDateTime occurredOn;
    private final CustomerId customerId;
    
    public CustomerCreatedEvent(CustomerId customerId) {
        this.eventId = UUID.randomUUID();
        this.occurredOn = LocalDateTime.now();
        this.customerId = customerId;
    }
    
    // 必須實作介面方法
    @Override
    public String getAggregateId() {  // <-- 仍然需要實作
        return customerId.toString();
    }
    
    @Override
    public String getEventType() {  // <-- 仍然需要實作
        return "CustomerCreated";
    }
    
    @Override
    public LocalDateTime getOccurredOn() {  // <-- 需要實作
        return occurredOn;
    }
    
    @Override
    public UUID getEventId() {  // <-- 可以使用預設實作或自己實作
        return eventId;
    }
}

// ========== 主要差異分析 ==========

/*
1. 需要實作的方法數量變化：

現有設計：
- AggregateRoot: 0 個抽象方法（無需 override）
- AbstractDomainEvent: 2 個抽象方法（getAggregateId, getEventType）

新設計：
- AggregateRoot: 1 個方法（getEventCollector）
- DomainEvent: 3 個方法（getAggregateId, getEventType, getOccurredOn）

2. 實作複雜度：

現有設計：
- 聚合根：繼承即可，無需額外實作
- 事件：需要實作 2 個方法

新設計：
- 聚合根：需要實作 1 個方法（提供事件收集器）
- 事件：需要實作 3 個方法（但可以用 record 簡化）

3. 靈活性：

現有設計：
- 聚合根被鎖定在繼承關係中
- 無法繼承其他類別

新設計：
- 聚合根可以實作多個介面
- 可以繼承其他類別（如果需要）
- 更符合「組合優於繼承」原則

4. 事件收集器管理：

現有設計：
- 事件收集器在基礎類別中自動管理
- 實作者無需關心

新設計：
- 實作者需要自己管理事件收集器實例
- 可以選擇不同的事件收集器實作
- 更靈活但需要更多樣板代碼

5. 使用 Record 簡化事件（Java 14+）：

// 新設計可以使用 record 大幅簡化事件實作
public record CustomerCreatedEvent(
    UUID eventId,
    LocalDateTime occurredOn,
    CustomerId customerId
) implements DomainEvent {
    
    public CustomerCreatedEvent(CustomerId customerId) {
        this(UUID.randomUUID(), LocalDateTime.now(), customerId);
    }
    
    @Override
    public String getAggregateId() {
        return customerId.toString();
    }
    
    @Override
    public String getEventType() {
        return "CustomerCreated";
    }
}
*/

// ========== 遷移策略建議 ==========

/*
漸進式遷移方案：

階段 1：保持現有設計，修復一致性問題
- 讓所有聚合根都繼承 AggregateRoot 基礎類別
- 讓所有事件都繼承 AbstractDomainEvent
- 修復 TODO 註解，實作真正的事件發布

階段 2：引入新介面，並行支援
- 創建新的 AggregateRoot 介面
- 讓現有的抽象類別也實作這個介面
- 新的聚合根可以選擇使用介面或繼承

階段 3：逐步遷移到介面
- 將現有聚合根逐一遷移到介面實作
- 使用 record 重寫事件類別
- 移除舊的抽象類別

這樣可以避免一次性大規模重構的風險。
*/