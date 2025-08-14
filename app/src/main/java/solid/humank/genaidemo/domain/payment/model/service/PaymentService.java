package solid.humank.genaidemo.domain.payment.model.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.PaymentId;
import solid.humank.genaidemo.domain.payment.model.aggregate.Payment;
import solid.humank.genaidemo.domain.payment.model.valueobject.PaymentMethod;

/** 支付服務 提供支付相關的領域服務 */
public class PaymentService {

    // 模擬支付存儲
    private final Map<PaymentId, Payment> payments = new HashMap<>();

    /** 根據ID查找支付 */
    public Optional<Payment> findById(UUID id) {
        return payments.values().stream()
                .filter(payment -> payment.getIdAsUUID().equals(id))
                .findFirst();
    }

    /** 根據訂單ID查找支付 */
    public Optional<Payment> findByOrderId(UUID orderId) {
        return payments.values().stream()
                .filter(payment -> payment.getOrderIdAsUUID().equals(orderId))
                .findFirst();
    }

    /** 保存支付 */
    public void save(Payment payment) {
        payments.put(payment.getId(), payment);
    }

    /** 請求支付 */
    public void requestPayment(UUID orderId, Money amount) {
        // 創建支付
        Payment payment = new Payment(orderId, amount);

        // 存儲支付
        payments.put(payment.getId(), payment);

        // 模擬處理支付
        processPayment(payment);
    }

    /** 處理支付 */
    private void processPayment(Payment payment) {
        // 模擬支付處理
        try {
            // 模擬支付處理延遲
            Thread.sleep(1000);

            // 模擬支付成功
            payment.complete("TXN-" + System.currentTimeMillis());

            // 發布支付成功事件
            System.out.println(
                    "Payment completed: "
                            + payment.getIdAsUUID()
                            + " for order "
                            + payment.getOrderIdAsUUID());
        } catch (Exception e) {
            // 模擬支付失敗
            payment.fail(e.getMessage());

            // 發布支付失敗事件
            System.out.println(
                    "Payment failed: "
                            + payment.getIdAsUUID()
                            + " for order "
                            + payment.getOrderIdAsUUID()
                            + " reason: "
                            + e.getMessage());
        }
    }

    /** 使用指定支付方式請求支付 */
    public void requestPayment(UUID orderId, Money amount, PaymentMethod paymentMethod) {
        // 創建支付
        Payment payment = new Payment(orderId, amount, paymentMethod);

        // 存儲支付
        payments.put(payment.getId(), payment);

        // 模擬處理支付
        processPayment(payment);
    }
}
