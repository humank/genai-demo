package solid.humank.genaidemo.interfaces.web.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import solid.humank.genaidemo.application.payment.dto.PaymentResponseDto;

/** 支付響應DTO 用於返回給前端的數據 包含支付的完整狀態資訊，包括支付ID、狀態、交易詳情等 */
@Schema(
        description = "支付回應資料，包含支付交易的完整狀態和詳細資訊",
        example =
                """
        {
          "id": "123e4567-e89b-12d3-a456-426614174000",
          "orderId": "456e7890-e89b-12d3-a456-426614174001",
          "amount": 1299.99,
          "currency": "TWD",
          "status": "COMPLETED",
          "paymentMethod": "CREDIT_CARD",
          "transactionId": "txn_1234567890abcdef",
          "createdAt": "2024-01-15T10:30:00",
          "updatedAt": "2024-01-15T10:35:00",
          "canRetry": false
        }
        """)
public class PaymentResponse {
    @Schema(
            description = "支付交易的唯一識別碼，UUID格式",
            example = "123e4567-e89b-12d3-a456-426614174000",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(
            description = "關聯的訂單唯一識別碼，UUID格式",
            example = "456e7890-e89b-12d3-a456-426614174001",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String orderId;

    @Schema(
            description = "支付金額，使用BigDecimal確保精度",
            example = "1299.99",
            requiredMode = Schema.RequiredMode.REQUIRED,
            type = "number",
            format = "decimal")
    private BigDecimal amount;

    @Schema(description = "ISO 4217貨幣代碼", example = "TWD", requiredMode = Schema.RequiredMode.REQUIRED, pattern = "^[A-Z]{3}$")
    private String currency;

    @Schema(
            description = "支付狀態，反映當前支付交易的處理狀態",
            example = "COMPLETED",
            requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {
                "PENDING", // 待支付
                "PROCESSING", // 處理中
                "COMPLETED", // 已完成
                "FAILED", // 失敗
                "CANCELLED", // 已取消
                "REFUNDED" // 已退款
            })
    private String status;

    @Schema(
            description = "使用的支付方式",
            example = "CREDIT_CARD",
            allowableValues = {
                "CREDIT_CARD", // 信用卡
                "BANK_TRANSFER", // 銀行轉賬
                "DIGITAL_WALLET", // 數位錢包
                "CASH_ON_DELIVERY" // 貨到付款
            })
    private String paymentMethod;

    @Schema(
            description = "第三方支付服務提供商的交易識別碼，僅在支付成功時提供",
            example = "txn_1234567890abcdef",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY)
    private String transactionId;

    @Schema(
            description = "支付失敗的詳細原因，僅在支付狀態為FAILED時提供",
            example = "信用卡餘額不足",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY)
    private String failureReason;

    @Schema(
            description = "支付記錄的創建時間，ISO 8601格式",
            example = "2024-01-15T10:30:00",
            requiredMode = Schema.RequiredMode.REQUIRED,
            type = "string",
            format = "date-time")
    private LocalDateTime createdAt;

    @Schema(
            description = "支付記錄的最後更新時間，ISO 8601格式",
            example = "2024-01-15T10:35:00",
            requiredMode = Schema.RequiredMode.REQUIRED,
            type = "string",
            format = "date-time")
    private LocalDateTime updatedAt;

    @Schema(description = "指示此支付是否允許重試，通常在支付失敗時為true", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean canRetry;

    // 默認構造函數
    public PaymentResponse() {}

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
