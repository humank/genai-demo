package solid.humank.genaidemo.infrastructure.promotion.persistence.adapter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.promotion.model.aggregate.Promotion;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionId;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionType;
import solid.humank.genaidemo.domain.promotion.repository.PromotionRepository;
import solid.humank.genaidemo.infrastructure.common.persistence.adapter.BaseRepositoryAdapter;
import solid.humank.genaidemo.infrastructure.promotion.persistence.entity.JpaPromotionEntity;
import solid.humank.genaidemo.infrastructure.promotion.persistence.mapper.PromotionMapper;
import solid.humank.genaidemo.infrastructure.promotion.persistence.repository.JpaPromotionRepository;

/** 促銷Repository適配器 */
@Component
public class PromotionRepositoryAdapter
        extends BaseRepositoryAdapter<Promotion, PromotionId, JpaPromotionEntity, String>
        implements PromotionRepository {

    private final JpaPromotionRepository jpaRepository;
    private final PromotionMapper promotionMapper;

    public PromotionRepositoryAdapter(
            JpaPromotionRepository jpaRepository, PromotionMapper promotionMapper) {
        super(jpaRepository);
        this.jpaRepository = jpaRepository;
        this.promotionMapper = promotionMapper;
    }

    @Override
    public Promotion save(Promotion promotion) {
        JpaPromotionEntity entity = toEntity(promotion);
        JpaPromotionEntity savedEntity = jpaRepository.save(entity);
        return promotion; // Return original aggregate root to maintain consistency
    }

    @Override
    public List<Promotion> findByType(PromotionType type) {
        return jpaRepository.findByType(type.name()).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Promotion> findActivePromotions() {
        return jpaRepository.findActivePromotions(LocalDateTime.now()).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(PromotionId promotionId) {
        jpaRepository.deleteById(promotionId.value());
    }

    @Override
    public List<Promotion> findPromotionsValidAt(LocalDateTime dateTime) {
        return jpaRepository.findActivePromotions(dateTime).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private JpaPromotionEntity toEntity(Promotion promotion) {
        return promotionMapper.toJpaEntity(promotion);
    }

    private Promotion toDomain(JpaPromotionEntity entity) {
        return promotionMapper.toDomainModel(entity);
    }

    // BaseRepositoryAdapter required methods
    @Override
    protected JpaPromotionEntity toJpaEntity(Promotion aggregateRoot) {
        return promotionMapper.toJpaEntity(aggregateRoot);
    }

    @Override
    protected Promotion toDomainModel(JpaPromotionEntity entity) {
        return promotionMapper.toDomainModel(entity);
    }

    @Override
    protected String convertToJpaId(PromotionId domainId) {
        return domainId.value();
    }

    @Override
    protected PromotionId extractId(Promotion aggregateRoot) {
        return aggregateRoot.getId();
    }
}