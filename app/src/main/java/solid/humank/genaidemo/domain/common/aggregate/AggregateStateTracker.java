package solid.humank.genaidemo.domain.common.aggregate;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

import solid.humank.genaidemo.domain.common.event.DomainEvent;

/**
 * 聚合根狀態追蹤器
 * 
 * 用於追蹤聚合根的狀態變化並自動產生相應的領域事件
 * 
 * 設計理念：
 * 1. 自動化事件產生：當狀態改變時自動產生事件
 * 2. 類型安全：使用泛型確保類型安全
 * 3. 可配置：允許自定義事件產生邏輯
 * 4. 純領域：不依賴任何基礎設施框架
 * 
 * 使用方式：
 * 
 * public class Customer extends AggregateRoot {
 * private final AggregateStateTracker<Customer> stateTracker =
 * new AggregateStateTracker<>(this);
 * 
 * public void updateEmail(Email newEmail) {
 * stateTracker.trackChange("email", this.email, newEmail,
 * (oldValue, newValue) -> new CustomerEmailUpdatedEvent(id, oldValue,
 * newValue));
 * this.email = newEmail;
 * }
 * }
 */
public class AggregateStateTracker<T extends AggregateRootInterface> {

    private final T aggregateRoot;
    private final Map<String, Object> previousState;
    private final Map<String, BiFunction<Object, Object, DomainEvent>> eventGenerators;

    public AggregateStateTracker(T aggregateRoot) {
        this.aggregateRoot = Objects.requireNonNull(aggregateRoot, "聚合根不能為空");
        this.previousState = new HashMap<>();
        this.eventGenerators = new HashMap<>();
    }

    /**
     * 追蹤狀態變化並產生事件
     * 
     * @param fieldName      欄位名稱
     * @param oldValue       舊值
     * @param newValue       新值
     * @param eventGenerator 事件產生器
     * @param <V>            值類型
     */
    public <V> void trackChange(String fieldName, V oldValue, V newValue,
            BiFunction<V, V, DomainEvent> eventGenerator) {
        if (!Objects.equals(oldValue, newValue)) {
            // 記錄狀態變化
            previousState.put(fieldName, oldValue);

            // 產生並收集事件
            DomainEvent event = eventGenerator.apply(oldValue, newValue);
            aggregateRoot.collectEvent(event);
        }
    }

    /**
     * 追蹤狀態變化（無事件產生）
     * 
     * @param fieldName 欄位名稱
     * @param oldValue  舊值
     * @param newValue  新值
     * @param <V>       值類型
     */
    public <V> void trackChange(String fieldName, V oldValue, V newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            previousState.put(fieldName, oldValue);
        }
    }

    /**
     * 註冊事件產生器
     * 
     * @param fieldName      欄位名稱
     * @param eventGenerator 事件產生器
     */
    public void registerEventGenerator(String fieldName,
            BiFunction<Object, Object, DomainEvent> eventGenerator) {
        eventGenerators.put(fieldName, eventGenerator);
    }

    /**
     * 自動追蹤狀態變化（使用預註冊的事件產生器）
     * 
     * @param fieldName 欄位名稱
     * @param oldValue  舊值
     * @param newValue  新值
     * @param <V>       值類型
     */
    public <V> void autoTrackChange(String fieldName, V oldValue, V newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            previousState.put(fieldName, oldValue);

            BiFunction<Object, Object, DomainEvent> eventGenerator = eventGenerators.get(fieldName);
            if (eventGenerator != null) {
                DomainEvent event = eventGenerator.apply(oldValue, newValue);
                aggregateRoot.collectEvent(event);
            }
        }
    }

    /**
     * 獲取欄位的前一個值
     * 
     * @param fieldName 欄位名稱
     * @param <V>       值類型
     * @return 前一個值
     */
    @SuppressWarnings("unchecked")
    public <V> V getPreviousValue(String fieldName) {
        return (V) previousState.get(fieldName);
    }

    /**
     * 檢查欄位是否有變化
     * 
     * @param fieldName 欄位名稱
     * @return 是否有變化
     */
    public boolean hasChanged(String fieldName) {
        return previousState.containsKey(fieldName);
    }

    /**
     * 清除狀態追蹤記錄
     */
    public void clearTracking() {
        previousState.clear();
    }

    /**
     * 獲取所有變化的欄位名稱
     * 
     * @return 變化的欄位名稱集合
     */
    public java.util.Set<String> getChangedFields() {
        return java.util.Set.copyOf(previousState.keySet());
    }
}