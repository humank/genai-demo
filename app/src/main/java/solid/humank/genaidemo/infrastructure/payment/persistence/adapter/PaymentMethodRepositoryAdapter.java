package solid.humank.genaidemo.infrastructure.payment.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.payment.model.aggregate.PaymentMethod;
import solid.humank.genaidemo.domain.payment.repository.PaymentMethodRepository;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;
import solid.humank.genaidemo.infrastructure.common.persistence.adapter.BaseRepositoryAdapter;
import solid.humank.genaidemo.infrastructure.payment.persistence.entity.JpaPaymentMethodEntity;
import solid.humank.genaidemo.infrastructure.payment.persistence.mapper.PaymentMethodMapper;
import solid.humank.genaidemo.infrastructure.payment.persistence.repository.JpaPaymentMethodRepository;

/** 支付方式儲存庫適配器 */
@Component
public class PaymentMethodRepositoryAdapter
        extends BaseRepositoryAdapter<PaymentMethod, String, JpaPaymentMethodEntity, String>
        implements PaymentMethodRepository {

    private final JpaPaymentMethodRepository jpaPaymentMethodRepository;
    private final PaymentMethodMapper mapper;

    public PaymentMethodRepositoryAdapter(JpaPaymentMethodRepository jpaPaymentMethodRepository,
            PaymentMethodMapper mapper) {
        super(jpaPaymentMethodRepository);
        this.jpaPaymentMethodRepository = jpaPaymentMethodRepository;
        this.mapper = mapper;
    }

    @Override
    public List<PaymentMethod> findByCustomerId(CustomerId customerId) {
        return jpaPaymentMethodRepository.findByCustomerId(customerId.getValue())
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<PaymentMethod> findDefaultByCustomerId(CustomerId customerId) {
        return jpaPaymentMethodRepository.findByCustomerIdAndIsDefaultTrue(customerId.getValue())
                .map(mapper::toDomainModel);
    }

    @Override
    public List<PaymentMethod> findByCustomerIdAndType(CustomerId customerId, String type) {
        return jpaPaymentMethodRepository.findByCustomerIdAndType(customerId.getValue(), type)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentMethod> findActiveByCustomerId(CustomerId customerId) {
        return jpaPaymentMethodRepository.findByCustomerIdAndIsActiveTrue(customerId.getValue())
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentMethod> findByProvider(String provider) {
        return jpaPaymentMethodRepository.findByProvider(provider)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    // BaseRepositoryAdapter required methods
    @Override
    protected JpaPaymentMethodEntity toJpaEntity(PaymentMethod aggregateRoot) {
        return mapper.toJpaEntity(aggregateRoot);
    }

    @Override
    protected PaymentMethod toDomainModel(JpaPaymentMethodEntity entity) {
        return mapper.toDomainModel(entity);
    }

    @Override
    protected String convertToJpaId(String domainId) {
        return domainId;
    }

    @Override
    protected String extractId(PaymentMethod aggregateRoot) {
        return aggregateRoot.getPaymentMethodId();
    }
}