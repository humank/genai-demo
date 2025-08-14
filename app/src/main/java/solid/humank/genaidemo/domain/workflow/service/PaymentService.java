package solid.humank.genaidemo.domain.workflow.service;

import java.util.Map;
import solid.humank.genaidemo.domain.common.annotations.DomainService;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;

/** 支付服務接口 定義與支付相關的操作 */
@DomainService
public interface PaymentService {

    /**
     * 處理支付
     *
     * @param orderId 訂單ID
     * @param paymentMethod 支付方式
     * @param paymentDetails 支付詳情
     * @return 支付是否成功
     */
    boolean processPayment(
            OrderId orderId, String paymentMethod, Map<String, Object> paymentDetails);

    /**
     * 取消支付
     *
     * @param orderId 訂單ID
     * @return 是否取消成功
     */
    boolean cancelPayment(OrderId orderId);

    /**
     * 退款
     *
     * @param orderId 訂單ID
     * @return 是否退款成功
     */
    boolean refundPayment(OrderId orderId);

    /**
     * 獲取支付狀態
     *
     * @param orderId 訂單ID
     * @return 支付狀態
     */
    String getPaymentStatus(OrderId orderId);

    /**
     * 獲取支付失敗原因
     *
     * @param orderId 訂單ID
     * @return 失敗原因
     */
    String getPaymentFailureReason(OrderId orderId);
}
