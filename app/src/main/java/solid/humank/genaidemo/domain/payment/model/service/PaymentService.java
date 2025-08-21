package solid.humank.genaidemo.domain.payment.model.service;

import java.util.UUID;
import solid.humank.genaidemo.domain.common.annotations.DomainService;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.payment.model.aggregate.Payment;
import solid.humank.genaidemo.domain.payment.model.valueobject.PaymentMethod;

/** 支付服務 提供支付相關的領域服務 - 無狀態領域服務 */
@DomainService
public class PaymentService {

    /** 驗證支付金額是否有效 */
    public boolean isValidPaymentAmount(Money amount) {
        return amount != null && amount.getAmount().compareTo(java.math.BigDecimal.ZERO) > 0;
    }

    /** 驗證支付方式是否支援 */
    public boolean isPaymentMethodSupported(PaymentMethod paymentMethod) {
        return paymentMethod != null;
    }

    /** 計算支付手續費 */
    public Money calculateProcessingFee(Money amount, PaymentMethod paymentMethod) {
        if (!isValidPaymentAmount(amount) || !isPaymentMethodSupported(paymentMethod)) {
            return Money.twd(0);
        }

        // 根據支付方式計算手續費
        return switch (paymentMethod) {
            case CREDIT_CARD -> amount.multiply(0.03); // 3% 手續費
            case BANK_TRANSFER -> Money.twd(15); // 固定手續費
            case CASH_ON_DELIVERY -> Money.twd(30); // 貨到付款手續費
            default -> Money.twd(0);
        };
    }

    /** 驗證支付請求是否有效 */
    public boolean isValidPaymentRequest(UUID orderId, Money amount, PaymentMethod paymentMethod) {
        return orderId != null
                && isValidPaymentAmount(amount)
                && isPaymentMethodSupported(paymentMethod);
    }

    /** 創建支付聚合根 */
    public Payment createPayment(UUID orderId, Money amount) {
        if (!isValidPaymentRequest(orderId, amount, PaymentMethod.CREDIT_CARD)) {
            throw new IllegalArgumentException("無效的支付請求");
        }
        return new Payment(orderId, amount);
    }

    /** 創建支付聚合根（指定支付方式） */
    public Payment createPayment(UUID orderId, Money amount, PaymentMethod paymentMethod) {
        if (!isValidPaymentRequest(orderId, amount, paymentMethod)) {
            throw new IllegalArgumentException("無效的支付請求");
        }
        return new Payment(orderId, amount, paymentMethod);
    }
}
