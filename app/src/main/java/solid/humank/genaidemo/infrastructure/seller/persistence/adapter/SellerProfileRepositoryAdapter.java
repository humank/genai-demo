package solid.humank.genaidemo.infrastructure.seller.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.seller.model.aggregate.SellerProfile;
import solid.humank.genaidemo.domain.seller.model.valueobject.SellerId;
import solid.humank.genaidemo.domain.seller.repository.SellerProfileRepository;
import solid.humank.genaidemo.infrastructure.common.persistence.adapter.BaseRepositoryAdapter;
import solid.humank.genaidemo.infrastructure.seller.persistence.entity.JpaSellerProfileEntity;
import solid.humank.genaidemo.infrastructure.seller.persistence.mapper.SellerProfileMapper;
import solid.humank.genaidemo.infrastructure.seller.persistence.repository.JpaSellerProfileRepository;

/** 賣家檔案儲存庫適配器 */
@Component
public class SellerProfileRepositoryAdapter
        extends BaseRepositoryAdapter<SellerProfile, SellerId, JpaSellerProfileEntity, String>
        implements SellerProfileRepository {

    private final JpaSellerProfileRepository jpaSellerProfileRepository;
    private final SellerProfileMapper mapper;

    public SellerProfileRepositoryAdapter(JpaSellerProfileRepository jpaSellerProfileRepository,
            SellerProfileMapper mapper) {
        super(jpaSellerProfileRepository);
        this.jpaSellerProfileRepository = jpaSellerProfileRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<SellerProfile> findBySellerId(SellerId sellerId) {
        return jpaSellerProfileRepository.findBySellerId(sellerId.getId())
                .map(mapper::toDomainModel);
    }

    @Override
    public List<SellerProfile> findVerifiedProfiles() {
        return jpaSellerProfileRepository.findByIsVerifiedTrue()
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<SellerProfile> findByRatingBetween(double minRating, double maxRating) {
        return jpaSellerProfileRepository.findByRatingBetween(minRating, maxRating)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<SellerProfile> findByVerificationStatus(String verificationStatus) {
        return jpaSellerProfileRepository.findByVerificationStatusContaining(verificationStatus)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    // BaseRepositoryAdapter required methods
    @Override
    protected JpaSellerProfileEntity toJpaEntity(SellerProfile aggregateRoot) {
        return mapper.toJpaEntity(aggregateRoot);
    }

    @Override
    protected SellerProfile toDomainModel(JpaSellerProfileEntity entity) {
        return mapper.toDomainModel(entity);
    }

    @Override
    protected String convertToJpaId(SellerId domainId) {
        return domainId.getId();
    }

    @Override
    protected SellerId extractId(SellerProfile aggregateRoot) {
        return aggregateRoot.getSellerId();
    }
}