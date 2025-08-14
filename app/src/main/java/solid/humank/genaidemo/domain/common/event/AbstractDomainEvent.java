package solid.humank.genaidemo.domain.common.event;

import java.time.LocalDateTime;
import java.util.UUID;

/** 抽象領域事件 提供領域事件的基本實現 */
public abstract class AbstractDomainEvent implements DomainEvent {

    private final UUID eventId;
    private final LocalDateTime occurredOn;
    private final String source;

    protected AbstractDomainEvent(String source) {
        this.eventId = UUID.randomUUID();
        this.occurredOn = LocalDateTime.now();
        this.source = source;
    }

    @Override
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    @Override
    public String getSource() {
        return source;
    }

    /**
     * 獲取事件ID
     *
     * @return 事件ID
     */
    public UUID getEventId() {
        return eventId;
    }
}
