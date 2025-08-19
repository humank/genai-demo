package solid.humank.genaidemo.infrastructure.payment.persistence.adapter;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.common.valueobject.PaymentId;
import solid.humank.genaidemo.domain.payment.model.aggregate.Payment;
import solid.humank.genaidemo.domain.payment.repository.PaymentRepository;
import solid.humank.genaidemo.infrastructure.common.persistence.adapter.BaseRepositoryAdapter;
import solid.humank.genaidemo.infrastructure.payment.persistence.entity.JpaPaymentEntity;
import solid.humank.genaidemo.infrastructure.payment.persistence.mapper.PaymentMapper;
import solid.humank.genaidemo.infrastructure.payment.persistence.repository.JpaPaymentRepository;

/** 支付儲存庫適配器 實現領域儲存庫接口，並使用基礎設施層的 JPA 儲存庫 嚴格遵循 Repository Pattern，只接受和返回聚合根 */
@Component
public class PaymentRepositoryAdapter extends BaseRepositoryAdapter<Payment, PaymentId, JpaPaymentEntity, String>
        implements PaymentRepository {

    private final JpaPaymentRepository jpaPaymentRepository;
    private final PaymentMapper mapper;

    public PaymentRepositoryAdapter(JpaPaymentRepository jpaPaymentRepository, PaymentMapper mapper) {
        super(jpaPaymentRepository);
        this.jpaPaymentRepository = jpaPaymentRepository;
        this.mapper = mapper;
    }

    @Override
    public List<Payment> findByOrderId(OrderId orderId) {
        return jpaPaymentRepository.findByOrderId(orderId.toString()).stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<Payment> findByOrderId(UUID orderId) {
        return findByOrderId(OrderId.fromUUID(orderId));
    }

    // BaseRepositoryAdapter required methods
    @Override
    protected JpaPaymentEntity toJpaEntity(Payment aggregateRoot) {
        return mapper.toJpaEntity(aggregateRoot);
    }

    @Override
    protected Payment toDomainModel(JpaPaymentEntity entity) {
        return mapper.toDomainModel(entity);
    }

    @Override
    protected String convertToJpaId(PaymentId domainId) {
        return domainId.toString();
    }

    @Override
    protected PaymentId extractId(Payment aggregateRoot) {
        return aggregateRoot.getId();
    }
}
