package solid.humank.genaidemo.infrastructure.pricing.repository;

import org.springframework.stereotype.Repository;
import solid.humank.genaidemo.domain.pricing.model.aggregate.PricingRule;
import solid.humank.genaidemo.domain.pricing.model.valueobject.PriceId;
import solid.humank.genaidemo.domain.pricing.model.valueobject.ProductCategory;
import solid.humank.genaidemo.domain.pricing.repository.PricingRuleRepository;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionId;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 定價規則倉儲的內存實現
 * 僅用於演示和測試目的
 */
@Repository
public class InMemoryPricingRuleRepository implements PricingRuleRepository {

    private final Map<PriceId, PricingRule> storage = new ConcurrentHashMap<>();

    @Override
    public Optional<PricingRule> findById(PriceId priceId) {
        return Optional.ofNullable(storage.get(priceId));
    }

    @Override
    public List<PricingRule> findByProductId(ProductId productId) {
        return new ArrayList<>(); // 簡化實現
    }

    @Override
    public List<PricingRule> findByPromotionId(PromotionId promotionId) {
        return new ArrayList<>(); // 簡化實現
    }

    @Override
    public List<PricingRule> findActiveRules() {
        return new ArrayList<>(); // 簡化實現
    }

    @Override
    public List<PricingRule> findRulesValidAt(LocalDateTime dateTime) {
        return new ArrayList<>(); // 簡化實現
    }

    @Override
    public List<PricingRule> findByProductCategory(ProductCategory category) {
        return new ArrayList<>(); // 簡化實現
    }

    @Override
    public PricingRule save(PricingRule pricingRule) {
        storage.put(pricingRule.getPriceId(), pricingRule);
        return pricingRule;
    }

    @Override
    public void delete(PriceId priceId) {
        storage.remove(priceId);
    }

    @Override
    public List<PricingRule> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public void delete(PricingRule aggregateRoot) {
        storage.remove(aggregateRoot.getPriceId());
    }

    @Override
    public void deleteById(PriceId priceId) {
        storage.remove(priceId);
    }

    @Override
    public long count() {
        return storage.size();
    }
    
    @Override
    public boolean existsById(PriceId priceId) {
        return storage.containsKey(priceId);
    }
}
