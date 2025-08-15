package solid.humank.genaidemo.infrastructure.pricing.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.pricing.model.aggregate.PricingRule;
import solid.humank.genaidemo.domain.pricing.model.valueobject.PriceId;
import solid.humank.genaidemo.domain.pricing.model.valueobject.ProductCategory;
import solid.humank.genaidemo.domain.pricing.repository.PricingRuleRepository;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionId;

/** 定價規則倉儲的內存實現 僅用於演示和測試目的 */
@Repository
public class InMemoryPricingRuleRepository implements PricingRuleRepository {

    private final Map<PriceId, PricingRule> storage = new ConcurrentHashMap<>();

    @Override
    public Optional<PricingRule> findById(PriceId priceId) {
        return Optional.ofNullable(storage.get(priceId));
    }

    @Override
    public List<PricingRule> findByProductId(ProductId productId) {
        // 簡化實現：根據產品ID返回模擬定價規則
        List<PricingRule> rules = new ArrayList<>();
        if ("PROD001".equals(productId.getId())) {
            rules.add(createMockPricingRule("RULE001", productId, 100.0));
        }
        return rules;
    }

    @Override
    public List<PricingRule> findByPromotionId(PromotionId promotionId) {
        // 簡化實現：根據促銷ID返回模擬定價規則
        List<PricingRule> rules = new ArrayList<>();
        if ("PROMO001".equals(promotionId.value())) {
            rules.add(createMockPricingRule("RULE002", new ProductId("PROD001"), 80.0));
        }
        return rules;
    }

    @Override
    public List<PricingRule> findActiveRules() {
        // 簡化實現：返回活躍的定價規則
        List<PricingRule> rules = new ArrayList<>();
        rules.add(createMockPricingRule("RULE001", new ProductId("PROD001"), 100.0));
        rules.add(createMockPricingRule("RULE003", new ProductId("PROD002"), 150.0));
        return rules;
    }

    @Override
    public List<PricingRule> findRulesValidAt(LocalDateTime dateTime) {
        // 簡化實現：返回在指定時間有效的定價規則
        List<PricingRule> rules = new ArrayList<>();
        // 假設所有規則在當前時間都有效
        if (dateTime.isBefore(LocalDateTime.now().plusDays(30))) {
            rules.addAll(findActiveRules());
        }
        return rules;
    }

    @Override
    public List<PricingRule> findByProductCategory(ProductCategory category) {
        // 簡化實現：根據產品類別返回定價規則
        List<PricingRule> rules = new ArrayList<>();
        if ("飲品".equals(category.toString())) {
            rules.add(createMockPricingRule("RULE004", new ProductId("PROD001"), 120.0));
        }
        return rules;
    }

    private PricingRule createMockPricingRule(String ruleId, ProductId productId, double price) {
        // 創建模擬的定價規則
        return new PricingRule(
                new PriceId(ruleId),
                productId,
                PromotionId.of("default-promotion"),
                Money.of(price),
                0.0, // discountPercentage
                Money.ZERO, // discountAmount
                LocalDateTime.now(),
                LocalDateTime.now().plusMonths(1),
                ProductCategory.ELECTRONICS);
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
