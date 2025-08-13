package solid.humank.genaidemo.infrastructure.payment.persistence.adapter;

import org.springframework.stereotype.Component;
import solid.humank.genaidemo.application.payment.port.outgoing.PaymentPersistencePort;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.common.valueobject.PaymentId;
import solid.humank.genaidemo.domain.payment.model.aggregate.Payment;
import solid.humank.genaidemo.domain.payment.repository.PaymentRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 支付持久化適配器
 * 實現應用層的 PaymentPersistencePort 接口
 * 使用領域層的 PaymentRepository 進行實際的持久化操作
 */
@Component
public class PaymentPersistenceAdapter implements PaymentPersistencePort {

    private final PaymentRepository paymentRepository;

    public PaymentPersistenceAdapter(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public void save(Payment payment) {
        paymentRepository.save(payment);
    }

    @Override
    public Optional<Payment> findById(UUID paymentId) {
        return paymentRepository.findById(PaymentId.fromUUID(paymentId));
    }

    @Override
    public List<Payment> findAll() {
        return paymentRepository.findAll();
    }

    @Override
    public void delete(UUID paymentId) {
        paymentRepository.deleteById(PaymentId.fromUUID(paymentId));
    }

    @Override
    public void update(Payment payment) {
        paymentRepository.save(payment);
    }

    @Override
    public boolean exists(UUID paymentId) {
        return paymentRepository.findById(PaymentId.fromUUID(paymentId)).isPresent();
    }

    @Override
    public Optional<Payment> findByOrderId(UUID orderId) {
        List<Payment> payments = paymentRepository.findByOrderId(OrderId.fromUUID(orderId));
        return payments.isEmpty() ? Optional.empty() : Optional.of(payments.get(0));
    }
}
