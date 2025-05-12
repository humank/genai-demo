package solid.humank.genaidemo.domain.payment.repository;

import solid.humank.genaidemo.domain.common.repository.Repository;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.common.valueobject.PaymentId;
import solid.humank.genaidemo.domain.payment.model.aggregate.Payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 支付儲存庫接口
 */
public interface PaymentRepository extends Repository<Payment, PaymentId> {
    
    /**
     * 保存支付
     * 
     * @param payment 支付
     * @return 保存後的支付
     */
    @Override
    Payment save(Payment payment);
    
    /**
     * 根據ID查詢支付
     * 
     * @param id 支付ID
     * @return 支付
     */
    Optional<Payment> findById(PaymentId id);
    
    /**
     * 根據訂單ID查詢支付列表
     * 
     * @param orderId 訂單ID
     * @return 支付列表
     */
    List<Payment> findByOrderId(OrderId orderId);
    
    /**
     * 根據訂單ID查詢支付列表
     * 
     * @param orderId 訂單ID
     * @return 支付列表
     */
    List<Payment> findByOrderId(UUID orderId);
}