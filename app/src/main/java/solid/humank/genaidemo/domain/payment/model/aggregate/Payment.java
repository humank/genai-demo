package solid.humank.genaidemo.domain.payment.model.aggregate;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.aggregate.AggregateRootInterface;
import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.common.valueobject.PaymentId;
import solid.humank.genaidemo.domain.common.valueobject.PaymentStatus;
import solid.humank.genaidemo.domain.payment.model.events.PaymentCompletedEvent;
import solid.humank.genaidemo.domain.payment.model.events.PaymentCreatedEvent;
import solid.humank.genaidemo.domain.payment.model.events.PaymentFailedEvent;
import solid.humank.genaidemo.domain.payment.model.valueobject.PaymentMethod;

/** 支付聚合根 */
@AggregateRoot(name = "Payment", description = "支付聚合根，管理支付流程和狀態", boundedContext = "Payment", version = "1.0")
public class Payment implements AggregateRootInterface {
    private final PaymentId id;
    private final OrderId orderId;
    private final Money amount;
    private PaymentStatus status;
    private PaymentMethod paymentMethod;
    private String transactionId;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean canRetry;

    /** 建立支付 */
    public Payment(UUID orderId, Money amount) {
        this.id = PaymentId.create();
        this.orderId = OrderId.fromUUID(Objects.requireNonNull(orderId, "Order ID cannot be null"));
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null");
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.canRetry = true;
    }

    /** 建立支付（指定支付方式） */
    public Payment(UUID orderId, Money amount, PaymentMethod paymentMethod) {
        this(orderId, amount);
        this.paymentMethod = Objects.requireNonNull(paymentMethod, "Payment method cannot be null");
    }

    /** 建立支付（使用OrderId值對象） */
    public Payment(OrderId orderId, Money amount) {
        this.id = PaymentId.create();
        this.orderId = Objects.requireNonNull(orderId, "Order ID cannot be null");
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null");
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.canRetry = true;

        // 發布支付創建事件，處理paymentMethod可能為null的情況
        collectEvent(
                PaymentCreatedEvent.create(
                        this.id, this.orderId, this.amount, null // paymentMethod可能為null
                ));
    }

    /** 建立支付（使用OrderId值對象和指定支付方式） */
    public Payment(OrderId orderId, Money amount, PaymentMethod paymentMethod) {
        this(orderId, amount);
        this.paymentMethod = Objects.requireNonNull(paymentMethod, "Payment method cannot be null");
    }

    /** 用於重建聚合根的完整建構子（僅供Repository使用） */
    protected Payment(
            PaymentId id,
            OrderId orderId,
            Money amount,
            PaymentStatus status,
            PaymentMethod paymentMethod,
            String transactionId,
            String failureReason,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            boolean canRetry) {
        this.id = Objects.requireNonNull(id, "Payment ID cannot be null");
        this.orderId = Objects.requireNonNull(orderId, "Order ID cannot be null");
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.paymentMethod = paymentMethod;
        this.transactionId = transactionId;
        this.failureReason = failureReason;
        this.createdAt = Objects.requireNonNull(createdAt, "Created at cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated at cannot be null");
        this.canRetry = canRetry;
    }

    /** 完成支付 */
    public void complete(String transactionId) {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Payment must be in PENDING state to complete");
        }

        this.transactionId = Objects.requireNonNull(transactionId, "Transaction ID cannot be null");
        this.status = PaymentStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();

        // 發布支付完成事件
        collectEvent(
                PaymentCompletedEvent.create(this.id, this.orderId, this.amount, this.transactionId));
    }

    /** 失敗支付 */
    public void fail(String reason) {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Payment must be in PENDING state to fail");
        }

        this.failureReason = reason;
        this.status = PaymentStatus.FAILED;
        this.updatedAt = LocalDateTime.now();

        // 發布支付失敗事件
        collectEvent(
                PaymentFailedEvent.create(
                        this.id, this.orderId, this.amount, this.failureReason, this.canRetry));
    }

    /** 退款 */
    public void refund() {
        if (status != PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Payment must be in COMPLETED state to refund");
        }

        this.status = PaymentStatus.REFUNDED;
        this.updatedAt = LocalDateTime.now();
    }

    /** 處理支付超時 */
    public void timeout() {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Payment must be in PENDING state to timeout");
        }

        this.failureReason = "Payment gateway timeout";
        this.status = PaymentStatus.FAILED;
        this.canRetry = true;
        this.updatedAt = LocalDateTime.now();
    }

    /** 設置支付方式 */
    public void setPaymentMethod(PaymentMethod paymentMethod) {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Payment method can only be set when payment is in PENDING state");
        }

        this.paymentMethod = Objects.requireNonNull(paymentMethod, "Payment method cannot be null");
        this.updatedAt = LocalDateTime.now();
    }

    /** 重試支付 */
    public void retry() {
        if (!canRetry) {
            throw new IllegalStateException("This payment cannot be retried");
        }

        if (status != PaymentStatus.FAILED) {
            throw new IllegalStateException("Only failed payments can be retried");
        }

        this.status = PaymentStatus.PENDING;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    public PaymentId getId() {
        return id;
    }

    public UUID getIdAsUUID() {
        return id.getId();
    }

    public OrderId getOrderId() {
        return orderId;
    }

    public UUID getOrderIdAsUUID() {
        return orderId.getId();
    }

    public Money getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean canRetry() {
        return canRetry;
    }
}
