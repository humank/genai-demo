package solid.humank.genaidemo.examples.payment;

import java.time.LocalDateTime;
import java.util.UUID;

import solid.humank.genaidemo.ddd.annotations.AggregateRoot;
import solid.humank.genaidemo.ddd.events.DomainEventPublisherService;
import solid.humank.genaidemo.examples.order.Money;
import solid.humank.genaidemo.examples.payment.events.PaymentFailedEvent;
import solid.humank.genaidemo.examples.payment.events.PaymentRequestedEvent;

@AggregateRoot
public class Payment {
    private final UUID id;
    private final UUID orderId;
    private final Money amount;
    private PaymentStatus status;
    private String failureReason;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    
    public Payment(UUID orderId, Money amount) {
        this.id = UUID.randomUUID();
        this.orderId = orderId;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        // 使用 DomainEventPublisherService 發布事件
        DomainEventPublisherService.publishEvent(
            new PaymentRequestedEvent(this.id, this.orderId, this.amount)
        );
    }

    public void markAsCompleted() {
        this.status = PaymentStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void markAsFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = LocalDateTime.now();
        
        // 使用 DomainEventPublisherService 發布事件
        DomainEventPublisherService.publishEvent(
            new PaymentFailedEvent(this.id, this.orderId, reason)
        );
    }

    public String getFailureReason() {
        return failureReason;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public Money getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
