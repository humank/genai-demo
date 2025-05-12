package solid.humank.genaidemo.application.payment.dto;

import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.payment.model.valueobject.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 處理支付命令
 * 用於封裝處理支付的請求數據
 */
public class ProcessPaymentCommand {
    private final UUID orderId;
    private final Money amount;
    private final PaymentMethod paymentMethod;
    private final String paymentDetails;

    public ProcessPaymentCommand(UUID orderId, Money amount, PaymentMethod paymentMethod) {
        this(orderId, amount, paymentMethod, null);
    }

    public ProcessPaymentCommand(UUID orderId, Money amount, PaymentMethod paymentMethod, String paymentDetails) {
        this.orderId = orderId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.paymentDetails = paymentDetails;
    }

    /**
     * 創建處理支付命令
     */
    public static ProcessPaymentCommand of(String orderId, BigDecimal amount, String currency, String paymentMethod) {
        return new ProcessPaymentCommand(
                UUID.fromString(orderId),
                Money.of(amount, currency),
                PaymentMethod.valueOf(paymentMethod)
        );
    }

    /**
     * 創建處理支付命令（帶支付詳情）
     */
    public static ProcessPaymentCommand of(String orderId, BigDecimal amount, String currency, String paymentMethod, String paymentDetails) {
        return new ProcessPaymentCommand(
                UUID.fromString(orderId),
                Money.of(amount, currency),
                PaymentMethod.valueOf(paymentMethod),
                paymentDetails
        );
    }

    public UUID getOrderId() {
        return orderId;
    }

    public Money getAmount() {
        return amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getPaymentDetails() {
        return paymentDetails;
    }
}