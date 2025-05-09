package solid.humank.genaidemo.interfaces.web.payment.dto;

import java.math.BigDecimal;

/**
 * 支付請求 DTO
 */
public class PaymentRequest {
    private String orderId;
    private BigDecimal amount;
    private String currency;

    public PaymentRequest() {
    }

    public PaymentRequest(String orderId, BigDecimal amount, String currency) {
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
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
}