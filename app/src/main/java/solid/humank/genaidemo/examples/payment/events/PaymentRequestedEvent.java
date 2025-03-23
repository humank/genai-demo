package solid.humank.genaidemo.examples.payment.events;

import java.time.Instant;
import java.util.UUID;

import solid.humank.genaidemo.ddd.events.DomainEvent;
import solid.humank.genaidemo.examples.order.Money;

public class PaymentRequestedEvent implements DomainEvent {
    private final UUID eventId;
    private final Instant occurredOn;
    private final UUID paymentId;
    private final UUID orderId;
    private final Money amount;

    public PaymentRequestedEvent(UUID paymentId, UUID orderId, Money amount) {
        this.eventId = UUID.randomUUID();
        this.occurredOn = Instant.now();
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public Money getAmount() {
        return amount;
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return "PaymentRequested";
    }

    @Override
    public Instant getOccurredOn() {
        return occurredOn;
    }
}
