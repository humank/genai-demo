package solid.humank.genaidemo.domain.common.event;

import java.time.LocalDateTime;
import java.util.UUID;

/** 抽象領域事件 提供領域事件的基本實現 */
public abstract class AbstractDomainEvent implements DomainEvent {

    private final UUID eventId;
    private final LocalDateTime occurredOn;

    protected AbstractDomainEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredOn = LocalDateTime.now();
    }

    @Override
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    /**
     * 獲取聚合根ID - 子類必須實作
     *
     * @return 聚合根ID
     */
    @Override
    public abstract String getAggregateId();

    /**
     * 獲取事件ID
     *
     * @return 事件ID
     */
    public UUID getEventId() {
        return eventId;
    }
}
