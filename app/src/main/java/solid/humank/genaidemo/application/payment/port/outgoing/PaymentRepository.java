package solid.humank.genaidemo.application.payment.port.outgoing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import solid.humank.genaidemo.domain.payment.model.aggregate.Payment;

/** 支付倉儲接口 */
public interface PaymentRepository {
    /** 保存支付 */
    Payment save(Payment payment);

    /** 根據ID查詢支付 */
    Optional<Payment> findById(UUID id);

    /** 根據訂單ID查詢支付 */
    Optional<Payment> findByOrderId(UUID orderId);

    /** 查詢所有支付 */
    List<Payment> findAll();

    /** 更新支付 */
    Payment update(Payment payment);
}
