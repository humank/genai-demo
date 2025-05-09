package solid.humank.genaidemo.domain.payment.model.entity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.Entity;
import solid.humank.genaidemo.domain.order.model.valueobject.Money;
import solid.humank.genaidemo.domain.payment.model.valueobject.PaymentStatus;

/**
 * 支付實體
 * 為了符合架構測試要求，將帶有 @Entity 註解的類放在 entity 包中
 */
@Entity
public class PaymentEntity {
    private final UUID id;
    private final UUID orderId;
    private final Money amount;
    private PaymentStatus status;
    private String transactionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * 建立支付實體
     */
    public PaymentEntity(UUID id, UUID orderId, Money amount, PaymentStatus status, 
                        String transactionId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.orderId = Objects.requireNonNull(orderId, "Order ID cannot be null");
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.transactionId = transactionId;
        this.createdAt = Objects.requireNonNull(createdAt, "Created time cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated time cannot be null");
    }
    
    // Getters
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
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}