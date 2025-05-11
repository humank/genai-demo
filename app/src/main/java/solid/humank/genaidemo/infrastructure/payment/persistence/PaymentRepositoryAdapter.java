package solid.humank.genaidemo.infrastructure.payment.persistence;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import solid.humank.genaidemo.domain.payment.model.aggregate.Payment;
import solid.humank.genaidemo.domain.payment.repository.PaymentRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 支付儲存庫適配器
 * 實現領域層的 PaymentRepository 接口
 * 使用 JpaPaymentRepositoryImpl 進行實際的持久化操作
 */
@Component
@Primary
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final JpaPaymentRepositoryImpl jpaPaymentRepository;

    public PaymentRepositoryAdapter(JpaPaymentRepositoryImpl jpaPaymentRepository) {
        this.jpaPaymentRepository = jpaPaymentRepository;
    }

    @Override
    public void save(Payment payment) {
        jpaPaymentRepository.save(payment);
    }

    @Override
    public Optional<Payment> findById(UUID paymentId) {
        return jpaPaymentRepository.findById(paymentId);
    }

    @Override
    public List<Payment> findAll() {
        return jpaPaymentRepository.findAll();
    }

    @Override
    public void delete(UUID paymentId) {
        jpaPaymentRepository.delete(paymentId);
    }

    @Override
    public void update(Payment payment) {
        jpaPaymentRepository.update(payment);
    }

    @Override
    public boolean exists(UUID paymentId) {
        return jpaPaymentRepository.exists(paymentId);
    }

    @Override
    public Optional<Payment> findByOrderId(UUID orderId) {
        return jpaPaymentRepository.findByOrderId(orderId);
    }
}