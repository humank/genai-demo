package solid.humank.genaidemo.examples.payment.events;

import java.time.Instant;
import java.util.UUID;

import solid.humank.genaidemo.ddd.events.DomainEvent;

public class PaymentFailedEvent implements DomainEvent {
    private final UUID eventId;
    private final Instant occurredOn;
    private final UUID paymentId;
    private final UUID orderId;
    private final String failureReason;

    public PaymentFailedEvent(UUID paymentId, UUID orderId, String failureReason) {
        this.eventId = UUID.randomUUID();
        this.occurredOn = Instant.now();
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.failureReason = failureReason;
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return "PaymentFailed";
    }

    @Override
    public Instant getOccurredOn() {
        return occurredOn;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
