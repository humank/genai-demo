package solid.humank.genaidemo.infrastructure.payment.persistence;

import org.springframework.stereotype.Repository;
import solid.humank.genaidemo.application.payment.port.outgoing.PaymentPersistencePort;
import solid.humank.genaidemo.domain.payment.model.aggregate.Payment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 支付持久化適配器
 * 實作 PaymentPersistencePort 接口
 * 注意：這是一個簡化的內存實現，實際應用中會使用真實的資料庫
 */
@Repository
public class PaymentRepositoryImpl implements PaymentPersistencePort {

    // 模擬數據存儲
    private final Map<UUID, Payment> paymentStore = new HashMap<>();

    @Override
    public void save(Payment payment) {
        paymentStore.put(payment.getId(), payment);
    }

    @Override
    public Optional<Payment> findById(UUID paymentId) {
        return Optional.ofNullable(paymentStore.get(paymentId));
    }

    @Override
    public List<Payment> findAll() {
        return paymentStore.values().stream().collect(Collectors.toList());
    }

    @Override
    public void delete(UUID paymentId) {
        paymentStore.remove(paymentId);
    }

    @Override
    public void update(Payment payment) {
        // 在記憶體實現中，更新和保存是一樣的
        save(payment);
    }

    @Override
    public boolean exists(UUID paymentId) {
        return paymentStore.containsKey(paymentId);
    }
    
    @Override
    public Optional<Payment> findByOrderId(UUID orderId) {
        return paymentStore.values().stream()
                .filter(payment -> payment.getOrderId().equals(orderId))
                .findFirst();
    }
}