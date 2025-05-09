package solid.humank.genaidemo.infrastructure.payment.persistence;

import org.springframework.stereotype.Component;
import solid.humank.genaidemo.application.payment.port.outgoing.PaymentPersistencePort;
import solid.humank.genaidemo.domain.payment.model.aggregate.Payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 支付持久化適配器
 * 實現應用層的 PaymentPersistencePort 接口
 * 使用 PaymentRepositoryImpl 進行實際的持久化操作
 */
@Component
public class PaymentPersistenceAdapter implements PaymentPersistencePort {

    private final PaymentRepositoryImpl paymentRepository;

    public PaymentPersistenceAdapter(PaymentRepositoryImpl paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public void save(Payment payment) {
        paymentRepository.save(payment);
    }

    @Override
    public Optional<Payment> findById(UUID paymentId) {
        return paymentRepository.findById(paymentId);
    }

    @Override
    public List<Payment> findAll() {
        return paymentRepository.findAll();
    }

    @Override
    public void delete(UUID paymentId) {
        paymentRepository.delete(paymentId);
    }

    @Override
    public void update(Payment payment) {
        paymentRepository.update(payment);
    }

    @Override
    public boolean exists(UUID paymentId) {
        return paymentRepository.exists(paymentId);
    }

    @Override
    public Optional<Payment> findByOrderId(UUID orderId) {
        return paymentRepository.findByOrderId(orderId);
    }
}