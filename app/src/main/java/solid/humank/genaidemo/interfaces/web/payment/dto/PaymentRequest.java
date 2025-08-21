package solid.humank.genaidemo.interfaces.web.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.Map;

/** 支付請求DTO 用於接收前端的支付請求 包含支付所需的基本資訊，包括訂單ID、金額、貨幣、支付方式等 */
@Schema(
        description = "支付請求資料，用於發起新的支付交易",
        example =
                """
        {
          "orderId": "123e4567-e89b-12d3-a456-426614174000",
          "amount": 1299.99,
          "currency": "TWD",
          "paymentMethod": "CREDIT_CARD",
          "paymentDetails": {
            "cardNumber": "****-****-****-1234",
            "expiryDate": "12/25",
            "cvv": "***"
          }
        }
        """)
public class PaymentRequest {
    @Schema(
            description = "關聯的訂單唯一識別碼，必須是有效的UUID格式",
            example = "123e4567-e89b-12d3-a456-426614174000",
            required = true,
            pattern =
                    "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    @NotBlank(message = "訂單ID不能為空")
    @Pattern(
            regexp =
                    "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
            message = "訂單ID必須是有效的UUID格式")
    private String orderId;

    @Schema(
            description = "支付金額，使用BigDecimal確保精度，最小值為0.01",
            example = "1299.99",
            minimum = "0.01",
            required = true,
            type = "number",
            format = "decimal")
    @NotNull(message = "支付金額不能為空")
    @DecimalMin(value = "0.01", message = "支付金額必須大於0")
    private BigDecimal amount;

    @Schema(
            description = "ISO 4217貨幣代碼，支援主要國際貨幣",
            example = "TWD",
            allowableValues = {"TWD", "USD", "EUR", "JPY"},
            defaultValue = "TWD",
            pattern = "^[A-Z]{3}$")
    @Pattern(regexp = "^[A-Z]{3}$", message = "貨幣代碼必須是3位大寫字母")
    private String currency;

    @Schema(
            description = "支付方式，決定使用何種支付管道進行交易",
            example = "CREDIT_CARD",
            allowableValues = {
                "CREDIT_CARD", // 信用卡
                "BANK_TRANSFER", // 銀行轉賬
                "DIGITAL_WALLET", // 數位錢包
                "CASH_ON_DELIVERY" // 貨到付款
            },
            defaultValue = "CREDIT_CARD")
    private String paymentMethod;

    @Schema(
            description = "支付詳細資訊，包含支付方式特定的額外資料。注意：敏感資訊如完整卡號、CVV等不應包含在此欄位中",
            example =
                    """
            {
              "cardNumber": "****-****-****-1234",
              "expiryDate": "12/25",
              "cardHolderName": "王小明",
              "bankCode": "012"
            }
            """,
            nullable = true,
            accessMode = Schema.AccessMode.WRITE_ONLY)
    private Map<String, Object> paymentDetails;

    // 默認構造函數
    public PaymentRequest() {}

    // 全參數構造函數
    public PaymentRequest(
            String orderId,
            BigDecimal amount,
            String currency,
            String paymentMethod,
            Map<String, Object> paymentDetails) {
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
        this.paymentMethod = paymentMethod;
        this.paymentDetails = paymentDetails;
    }

    // Getters and Setters
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

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Map<String, Object> getPaymentDetails() {
        return paymentDetails;
    }

    public void setPaymentDetails(Map<String, Object> paymentDetails) {
        this.paymentDetails = paymentDetails;
    }
}
