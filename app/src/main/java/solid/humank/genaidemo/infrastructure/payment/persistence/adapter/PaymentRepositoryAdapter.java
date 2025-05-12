package solid.humank.genaidemo.infrastructure.payment.persistence.adapter;

import org.springframework.stereotype.Component;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.common.valueobject.PaymentId;
import solid.humank.genaidemo.domain.payment.model.aggregate.Payment;
import solid.humank.genaidemo.domain.payment.repository.PaymentRepository;
import solid.humank.genaidemo.infrastructure.payment.persistence.entity.JpaPaymentEntity;
import solid.humank.genaidemo.infrastructure.payment.persistence.mapper.PaymentMapper;
import solid.humank.genaidemo.infrastructure.payment.persistence.repository.JpaPaymentRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 支付儲存庫適配器
 * 實現領域儲存庫接口，並使用基礎設施層的 JPA 儲存庫
 */
@Component
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final JpaPaymentRepository jpaPaymentRepository;

    public PaymentRepositoryAdapter(JpaPaymentRepository jpaPaymentRepository) {
        this.jpaPaymentRepository = jpaPaymentRepository;
    }

    @Override
    public Payment save(Payment payment) {
        // 將領域模型轉換為持久化模型
        JpaPaymentEntity jpaEntity = PaymentMapper.toJpaEntity(payment);
        
        // 保存到資料庫
        JpaPaymentEntity savedEntity = jpaPaymentRepository.save(jpaEntity);
        
        // 將保存後的持久化模型轉換回領域模型
        return PaymentMapper.toDomainEntity(savedEntity);
    }

    @Override
    public Optional<Payment> findById(PaymentId id) {
        return jpaPaymentRepository.findById(id.toString())
                .map(PaymentMapper::toDomainEntity);
    }

    @Override
    public List<Payment> findByOrderId(OrderId orderId) {
        return jpaPaymentRepository.findByOrderId(orderId.toString())
                .stream()
                .map(PaymentMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<Payment> findByOrderId(UUID orderId) {
        return findByOrderId(OrderId.fromUUID(orderId));
    }

    @Override
    public List<Payment> findAll() {
        return jpaPaymentRepository.findAll()
                .stream()
                .map(PaymentMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Payment payment) {
        jpaPaymentRepository.deleteById(payment.getId().toString());
    }

    @Override
    public void deleteById(PaymentId id) {
        jpaPaymentRepository.deleteById(id.toString());
    }

    @Override
    public long count() {
        return jpaPaymentRepository.count();
    }
}