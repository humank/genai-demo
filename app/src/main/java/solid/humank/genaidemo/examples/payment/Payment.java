package solid.humank.genaidemo.examples.payment;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import solid.humank.genaidemo.ddd.annotations.AggregateRoot;
import solid.humank.genaidemo.ddd.annotations.Entity;
import solid.humank.genaidemo.examples.order.model.valueobject.Money;

/**
 * 支付聚合根
 */
@AggregateRoot
@Entity
public class Payment {
    private final UUID id;
    private final UUID orderId;
    private final Money amount;
    private PaymentStatus status;
    private String transactionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * 建立支付
     */
    public Payment(UUID orderId, Money amount) {
        this.id = UUID.randomUUID();
        this.orderId = Objects.requireNonNull(orderId, "Order ID cannot be null");
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null");
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }
    
    /**
     * 完成支付
     */
    public void complete(String transactionId) {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Payment must be in PENDING state to complete");
        }
        
        this.transactionId = Objects.requireNonNull(transactionId, "Transaction ID cannot be null");
        this.status = PaymentStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 失敗支付
     */
    public void fail(String reason) {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Payment must be in PENDING state to fail");
        }
        
        this.status = PaymentStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 退款
     */
    public void refund() {
        if (status != PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Payment must be in COMPLETED state to refund");
        }
        
        this.status = PaymentStatus.REFUNDED;
        this.updatedAt = LocalDateTime.now();
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
    
    /**
     * 支付狀態枚舉
     */
    public enum PaymentStatus {
        PENDING("處理中"),
        COMPLETED("已完成"),
        FAILED("失敗"),
        REFUNDED("已退款");
        
        private final String description;
        
        PaymentStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}