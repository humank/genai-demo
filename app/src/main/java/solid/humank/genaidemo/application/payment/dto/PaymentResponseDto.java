package solid.humank.genaidemo.application.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import solid.humank.genaidemo.domain.common.valueobject.PaymentStatus;
import solid.humank.genaidemo.domain.payment.model.aggregate.Payment;

/** 支付響應 DTO */
public class PaymentResponseDto {
    private String id;
    private String transactionId;
    private String orderId;
    private BigDecimal amount;
    private String currency;
    private boolean success;
    private String status;
    private String paymentMethod;
    private String errorMessage;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean canRetry;
    private LocalDateTime timestamp;

    public PaymentResponseDto() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * 從領域模型創建 DTO
     *
     * @param payment 支付領域模型
     * @return 支付響應 DTO
     */
    public static PaymentResponseDto fromDomain(Payment payment) {
        PaymentResponseDto dto = new PaymentResponseDto();
        dto.setId(payment.getId().toString());
        dto.setOrderId(payment.getOrderId().toString());
        dto.setAmount(payment.getAmount().getAmount());
        dto.setCurrency(payment.getAmount().getCurrency().getCurrencyCode());
        dto.setStatus(payment.getStatus().name());
        dto.setSuccess(payment.getStatus() == PaymentStatus.COMPLETED);

        if (payment.getPaymentMethod() != null) {
            dto.setPaymentMethod(payment.getPaymentMethod().name());
        }

        dto.setTransactionId(payment.getTransactionId());
        dto.setFailureReason(payment.getFailureReason());
        dto.setErrorMessage(payment.getFailureReason()); // 兼容舊代碼
        dto.setCreatedAt(payment.getCreatedAt());
        dto.setUpdatedAt(payment.getUpdatedAt());
        dto.setCanRetry(payment.canRetry());

        return dto;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isCanRetry() {
        return canRetry;
    }

    public void setCanRetry(boolean canRetry) {
        this.canRetry = canRetry;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
