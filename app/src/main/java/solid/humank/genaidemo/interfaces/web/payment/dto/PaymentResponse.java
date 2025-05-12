package solid.humank.genaidemo.interfaces.web.payment.dto;

import solid.humank.genaidemo.application.payment.dto.PaymentResponseDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付響應DTO
 * 用於返回給前端的數據
 */
public class PaymentResponse {
    private String id;
    private String orderId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String paymentMethod;
    private String transactionId;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean canRetry;

    // 默認構造函數
    public PaymentResponse() {
    }

    // 從應用層DTO創建
    public static PaymentResponse fromDto(PaymentResponseDto dto) {
        PaymentResponse response = new PaymentResponse();
        response.setId(dto.getId());
        response.setOrderId(dto.getOrderId());
        response.setAmount(dto.getAmount());
        response.setCurrency(dto.getCurrency());
        response.setStatus(dto.getStatus());
        response.setPaymentMethod(dto.getPaymentMethod());
        response.setTransactionId(dto.getTransactionId());
        response.setFailureReason(dto.getFailureReason());
        response.setCreatedAt(dto.getCreatedAt());
        response.setUpdatedAt(dto.getUpdatedAt());
        response.setCanRetry(dto.isCanRetry());
        return response;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
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
}