package solid.humank.genaidemo.examples.order.application.port.outgoing;

import solid.humank.genaidemo.examples.order.model.valueobject.Money;
import solid.humank.genaidemo.examples.order.model.valueobject.OrderId;
import solid.humank.genaidemo.examples.order.model.valueobject.PaymentResult;

/**
 * 支付服務端口 - 次要輸出端口
 * 定義系統與支付服務的交互方式
 */
public interface PaymentServicePort {
    /**
     * 處理訂單支付
     * @param orderId 訂單ID
     * @param amount 支付金額
     * @return 支付結果
     */
    PaymentResult processPayment(OrderId orderId, Money amount);

    /**
     * 取消支付
     * @param orderId 訂單ID
     * @return 取消結果
     */
    PaymentResult cancelPayment(OrderId orderId);

    /**
     * 查詢支付狀態
     * @param orderId 訂單ID
     * @return 支付狀態
     */
    PaymentResult getPaymentStatus(OrderId orderId);

    /**
     * 退款處理
     * @param orderId 訂單ID
     * @param amount 退款金額
     * @return 退款結果
     */
    PaymentResult processRefund(OrderId orderId, Money amount);
}