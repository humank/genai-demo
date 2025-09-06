package solid.humank.genaidemo.infrastructure.promotion.persistence.adapter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.promotion.model.aggregate.Voucher;
import solid.humank.genaidemo.domain.promotion.repository.VoucherRepository;
import solid.humank.genaidemo.infrastructure.common.persistence.adapter.BaseRepositoryAdapter;
import solid.humank.genaidemo.infrastructure.promotion.persistence.entity.JpaVoucherEntity;
import solid.humank.genaidemo.infrastructure.promotion.persistence.mapper.VoucherMapper;
import solid.humank.genaidemo.infrastructure.promotion.persistence.repository.JpaVoucherRepository;

/** 優惠券儲存庫適配器 - 準備移至 Promotion Context */
@Component
public class VoucherRepositoryAdapter
        extends BaseRepositoryAdapter<Voucher, String, JpaVoucherEntity, String>
        implements VoucherRepository {

    private final JpaVoucherRepository jpaVoucherRepository;
    private final VoucherMapper mapper;

    public VoucherRepositoryAdapter(JpaVoucherRepository jpaVoucherRepository, VoucherMapper mapper) {
        super(jpaVoucherRepository);
        this.jpaVoucherRepository = jpaVoucherRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Voucher> findByRedemptionCode(String redemptionCode) {
        return jpaVoucherRepository.findByRedemptionCode(redemptionCode)
                .map(mapper::toDomainModel);
    }

    @Override
    public List<Voucher> findByCustomerId(String customerId) {
        return jpaVoucherRepository.findByOwnerId(customerId)
                .stream()
                .map(mapper::toDomainModel)
                .toList();
    }

    @Override
    public List<Voucher> findValidVouchers(LocalDateTime expirationDate) {
        return jpaVoucherRepository.findByExpiresAtAfterAndStatus(expirationDate, "ACTIVE")
                .stream()
                .map(mapper::toDomainModel)
                .toList();
    }

    @Override
    public void delete(String voucherId) {
        deleteById(voucherId);
    }

    // BaseRepositoryAdapter required methods
    @Override
    protected JpaVoucherEntity toJpaEntity(Voucher aggregateRoot) {
        return mapper.toJpaEntity(aggregateRoot);
    }

    @Override
    protected Voucher toDomainModel(JpaVoucherEntity entity) {
        return mapper.toDomainModel(entity);
    }

    @Override
    protected String convertToJpaId(String domainId) {
        return domainId;
    }

    @Override
    protected String extractId(Voucher aggregateRoot) {
        return aggregateRoot.getId().value();
    }
}