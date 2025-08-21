package solid.humank.genaidemo.domain.common.event;

/**
 * 領域事件處理器基礎介面
 * 
 * 定義在領域層，作為處理領域事件的契約
 * 實作類別通常位於基礎設施層，用於跨聚合根的事件處理
 * 
 * 需求 6.2: 建立事件處理器接收和處理事件的機制
 * 需求 6.5: 建立跨聚合根事件通信的業務邏輯執行
 */
public interface DomainEventHandler<T extends DomainEvent> {

    /**
     * 處理領域事件
     * 
     * @param event 要處理的領域事件
     */
    void handle(T event);

    /**
     * 獲取此處理器支援的事件類型
     * 
     * @return 事件類型
     */
    Class<T> getSupportedEventType();

    /**
     * 獲取處理器名稱，用於日誌和監控
     * 
     * @return 處理器名稱
     */
    default String getHandlerName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 檢查是否應該處理此事件
     * 子類可以重寫此方法來添加額外的過濾條件
     * 
     * @param event 事件
     * @return 如果應該處理返回 true
     */
    default boolean shouldHandle(T event) {
        return event != null;
    }
}