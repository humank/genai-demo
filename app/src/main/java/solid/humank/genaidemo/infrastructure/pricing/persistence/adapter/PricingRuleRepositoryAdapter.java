package solid.humank.genaidemo.infrastructure.pricing.persistence.adapter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import solid.humank.genaidemo.domain.pricing.model.aggregate.PricingRule;
import solid.humank.genaidemo.domain.pricing.model.valueobject.PriceId;
import solid.humank.genaidemo.domain.pricing.model.valueobject.ProductCategory;
import solid.humank.genaidemo.domain.pricing.repository.PricingRuleRepository;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionId;
import solid.humank.genaidemo.infrastructure.common.persistence.adapter.BaseRepositoryAdapter;
import solid.humank.genaidemo.infrastructure.pricing.persistence.entity.JpaPricingRuleEntity;
import solid.humank.genaidemo.infrastructure.pricing.persistence.mapper.PricingRuleMapper;
import solid.humank.genaidemo.infrastructure.pricing.persistence.repository.JpaPricingRuleRepository;

/** 定價規則儲存庫適配器 - 使用JPA實現，遵循統一的 Repository Pattern */
@Component
@Transactional
public class PricingRuleRepositoryAdapter
        extends BaseRepositoryAdapter<PricingRule, PriceId, JpaPricingRuleEntity, String>
        implements PricingRuleRepository {

    private final PricingRuleMapper mapper;

    public PricingRuleRepositoryAdapter(JpaPricingRuleRepository jpaRepository, PricingRuleMapper mapper) {
        super(jpaRepository);
        this.mapper = mapper;
    }

    @Override
    protected JpaPricingRuleEntity toJpaEntity(PricingRule aggregateRoot) {
        return mapper.toJpaEntity(aggregateRoot);
    }

    @Override
    protected PricingRule toDomainModel(JpaPricingRuleEntity entity) {
        return mapper.toDomainModel(entity);
    }

    @Override
    protected String convertToJpaId(PriceId domainId) {
        return domainId.getValue();
    }

    @Override
    protected PriceId extractId(PricingRule aggregateRoot) {
        return aggregateRoot.getPriceId();
    }

    @Override
    public List<PricingRule> findByProductId(ProductId productId) {
        return ((JpaPricingRuleRepository) jpaRepository).findByProductId(productId.getId())
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<PricingRule> findByPromotionId(PromotionId promotionId) {
        return ((JpaPricingRuleRepository) jpaRepository).findByPromotionId(promotionId.value())
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<PricingRule> findActiveRules() {
        return ((JpaPricingRuleRepository) jpaRepository).findByIsActiveTrue()
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<PricingRule> findRulesValidAt(LocalDateTime dateTime) {
        return ((JpaPricingRuleRepository) jpaRepository).findRulesValidAt(dateTime)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<PricingRule> findByProductCategory(ProductCategory category) {
        return ((JpaPricingRuleRepository) jpaRepository).findByProductCategory(category.name())
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }
}