package solid.humank.genaidemo.domain.order.model.service;

import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.PaymentResult;

/**
 * 支付服務介面
 * 處理訂單的支付相關操作
 */
public interface PaymentService {
    /**
     * 處理支付
     * @param orderId 訂單ID
     * @param amount 支付金額
     * @return 支付結果
     */
    PaymentResult processPayment(String orderId, Money amount);

    /**
     * 退款處理
     * @param paymentId 支付ID
     */
    void refundPayment(String paymentId);
}