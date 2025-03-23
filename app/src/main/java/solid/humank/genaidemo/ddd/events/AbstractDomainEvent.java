package solid.humank.genaidemo.ddd.events;

import java.time.Instant;
import java.util.UUID;

public record AbstractDomainEvent(
    UUID eventId,
    Instant occurredOn,
    String eventType
) implements DomainEvent {
    public AbstractDomainEvent(String eventType) {
        this(UUID.randomUUID(), Instant.now(), eventType);
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public Instant getOccurredOn() {
        return occurredOn;
    }

    @Override
    public String getEventType() {
        return eventType;
    }
}
