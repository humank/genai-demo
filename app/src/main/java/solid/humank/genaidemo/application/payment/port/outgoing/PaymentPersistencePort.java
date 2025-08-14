package solid.humank.genaidemo.application.payment.port.outgoing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import solid.humank.genaidemo.domain.payment.model.aggregate.Payment;

/** 支付持久化端口 定義支付持久化的業務操作 */
public interface PaymentPersistencePort {

    /** 保存支付 */
    void save(Payment payment);

    /** 根據ID查詢支付 */
    Optional<Payment> findById(UUID paymentId);

    /** 查詢所有支付 */
    List<Payment> findAll();

    /** 刪除支付 */
    void delete(UUID paymentId);

    /** 更新支付 */
    void update(Payment payment);

    /** 檢查支付是否存在 */
    boolean exists(UUID paymentId);

    /** 根據訂單ID查詢支付 */
    Optional<Payment> findByOrderId(UUID orderId);
}
