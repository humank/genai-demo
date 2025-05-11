package solid.humank.genaidemo.infrastructure.payment.persistence;

import org.springframework.stereotype.Repository;
import solid.humank.genaidemo.application.payment.port.outgoing.PaymentPersistencePort;
import solid.humank.genaidemo.domain.payment.model.aggregate.Payment;
import solid.humank.genaidemo.infrastructure.payment.persistence.entity.JpaPaymentEntity;
import solid.humank.genaidemo.infrastructure.payment.persistence.mapper.PaymentMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 支付持久化實現
 * 使用 JPA 實現支付的持久化操作
 */
@Repository
public class JpaPaymentRepositoryImpl implements PaymentPersistencePort {

    private final JpaPaymentRepository jpaPaymentRepository;

    public JpaPaymentRepositoryImpl(JpaPaymentRepository jpaPaymentRepository) {
        this.jpaPaymentRepository = jpaPaymentRepository;
    }

    @Override
    public void save(Payment payment) {
        JpaPaymentEntity jpaEntity = PaymentMapper.toJpaEntity(payment);
        jpaPaymentRepository.save(jpaEntity);
    }

    @Override
    public Optional<Payment> findById(UUID paymentId) {
        return jpaPaymentRepository.findById(paymentId.toString())
                .map(PaymentMapper::toDomainEntity);
    }

    @Override
    public List<Payment> findAll() {
        return jpaPaymentRepository.findAll().stream()
                .map(PaymentMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(UUID paymentId) {
        jpaPaymentRepository.deleteById(paymentId.toString());
    }

    @Override
    public void update(Payment payment) {
        // 在 JPA 中，save 方法會根據 ID 是否存在來決定是插入還是更新
        save(payment);
    }

    @Override
    public boolean exists(UUID paymentId) {
        return jpaPaymentRepository.existsById(paymentId.toString());
    }

    @Override
    public Optional<Payment> findByOrderId(UUID orderId) {
        return jpaPaymentRepository.findByOrderId(orderId.toString())
                .map(PaymentMapper::toDomainEntity);
    }
}