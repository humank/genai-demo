package solid.humank.genaidemo.infrastructure.promotion.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import solid.humank.genaidemo.domain.promotion.model.aggregate.Promotion;
import solid.humank.genaidemo.domain.promotion.model.valueobject.DateRange;
import solid.humank.genaidemo.domain.promotion.model.valueobject.FlashSaleRule;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionId;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionStatus;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionType;
import solid.humank.genaidemo.domain.promotion.repository.PromotionRepository;
import solid.humank.genaidemo.infrastructure.entity.JpaPromotionEntity;

/** 促銷Repository適配器 */
@Component
public class PromotionRepositoryAdapter implements PromotionRepository {

    private final JpaPromotionRepository jpaRepository;
    private final ObjectMapper objectMapper;

    public PromotionRepositoryAdapter(
            JpaPromotionRepository jpaRepository, ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Promotion save(Promotion promotion) {
        JpaPromotionEntity entity = toEntity(promotion);
        JpaPromotionEntity savedEntity = jpaRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<Promotion> findById(PromotionId id) {
        return jpaRepository.findById(id.value()).map(this::toDomain);
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
    public void deleteById(PromotionId promotionId) {
        jpaRepository.deleteById(promotionId.value());
    }

    @Override
    public List<Promotion> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Promotion> findPromotionsValidAt(LocalDateTime dateTime) {
        return jpaRepository.findActivePromotions(dateTime).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(PromotionId promotionId) {
        return jpaRepository.existsById(promotionId.value());
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public void delete(Promotion promotion) {
        jpaRepository.deleteById(promotion.getId().value());
    }

    private JpaPromotionEntity toEntity(Promotion promotion) {
        JpaPromotionEntity entity = new JpaPromotionEntity();
        entity.setId(promotion.getId().value());
        entity.setName(promotion.getName());
        entity.setDescription(promotion.getDescription());
        entity.setType(promotion.getType().name());
        entity.setStatus(promotion.getStatus().name());
        entity.setStartDate(promotion.getValidPeriod().startDate());
        entity.setEndDate(promotion.getValidPeriod().endDate());
        entity.setUsageLimit(promotion.getUsageLimit());
        entity.setUsageCount(promotion.getUsageCount());
        entity.setCreatedAt(promotion.getCreatedAt());
        entity.setUpdatedAt(promotion.getUpdatedAt());

        // 序列化規則數據為JSON
        try {
            entity.setRuleData(objectMapper.writeValueAsString(promotion.getRule()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize promotion rule", e);
        }

        return entity;
    }

    private Promotion toDomain(JpaPromotionEntity entity) {
        PromotionId id = PromotionId.of(entity.getId());
        PromotionType type = PromotionType.valueOf(entity.getType());
        DateRange validPeriod = new DateRange(entity.getStartDate(), entity.getEndDate());

        // 根據促銷類型和ruleData反序列化正確的規則
        solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionRule rule =
                deserializeRule(type, entity.getRuleData(), validPeriod);

        Promotion promotion =
                new Promotion(
                        id, entity.getName(), entity.getDescription(), type, rule, validPeriod);

        // 設置其他屬性
        promotion.updateStatus(PromotionStatus.valueOf(entity.getStatus()));
        if (entity.getUsageLimit() != null) {
            promotion.setUsageLimit(entity.getUsageLimit());
        }

        return promotion;
    }

    private solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionRule deserializeRule(
            PromotionType type, String ruleData, DateRange validPeriod) {
        try {
            switch (type) {
                case FLASH_SALE:
                    if (ruleData != null && !ruleData.isEmpty()) {
                        // 嘗試從JSON反序列化
                        return objectMapper.readValue(ruleData, FlashSaleRule.class);
                    }
                    // 預設FlashSaleRule
                    return new FlashSaleRule(
                            new solid.humank.genaidemo.domain.product.model.valueobject.ProductId(
                                    "default"),
                            solid.humank.genaidemo.domain.common.valueobject.Money.twd(100),
                            100,
                            validPeriod);

                case ADD_ON_PURCHASE:
                    if (ruleData != null && !ruleData.isEmpty()) {
                        return objectMapper.readValue(
                                ruleData,
                                solid.humank.genaidemo.domain.promotion.model.valueobject
                                        .AddOnPurchaseRule.class);
                    }
                    // 預設AddOnPurchaseRule
                    return new solid.humank.genaidemo.domain.promotion.model.valueobject
                            .AddOnPurchaseRule(
                            new solid.humank.genaidemo.domain.product.model.valueobject.ProductId(
                                    "main"),
                            new solid.humank.genaidemo.domain.product.model.valueobject.ProductId(
                                    "addon"),
                            solid.humank.genaidemo.domain.common.valueobject.Money.twd(50),
                            solid.humank.genaidemo.domain.common.valueobject.Money.twd(100));

                case LIMITED_QUANTITY:
                    if (ruleData != null && !ruleData.isEmpty()) {
                        return objectMapper.readValue(
                                ruleData,
                                solid.humank.genaidemo.domain.promotion.model.valueobject
                                        .LimitedQuantityRule.class);
                    }
                    // 預設LimitedQuantityRule
                    return new solid.humank.genaidemo.domain.promotion.model.valueobject
                            .LimitedQuantityRule(
                            new solid.humank.genaidemo.domain.product.model.valueobject.ProductId(
                                    "limited"),
                            solid.humank.genaidemo.domain.common.valueobject.Money.twd(80),
                            solid.humank.genaidemo.domain.common.valueobject.Money.twd(120),
                            50,
                            "promo-001");

                case GIFT_WITH_PURCHASE:
                    if (ruleData != null && !ruleData.isEmpty()) {
                        return objectMapper.readValue(
                                ruleData,
                                solid.humank.genaidemo.domain.promotion.model.valueobject
                                        .GiftWithPurchaseRule.class);
                    }
                    // 預設GiftWithPurchaseRule
                    return new solid.humank.genaidemo.domain.promotion.model.valueobject
                            .GiftWithPurchaseRule(
                            solid.humank.genaidemo.domain.common.valueobject.Money.twd(500),
                            new solid.humank.genaidemo.domain.product.model.valueobject.ProductId(
                                    "gift"),
                            solid.humank.genaidemo.domain.common.valueobject.Money.twd(50),
                            1,
                            false);

                default:
                    // 預設返回FlashSaleRule
                    return new FlashSaleRule(
                            new solid.humank.genaidemo.domain.product.model.valueobject.ProductId(
                                    "default"),
                            solid.humank.genaidemo.domain.common.valueobject.Money.twd(100),
                            100,
                            validPeriod);
            }
        } catch (Exception e) {
            // 反序列化失敗時返回預設規則
            return new FlashSaleRule(
                    new solid.humank.genaidemo.domain.product.model.valueobject.ProductId(
                            "default"),
                    solid.humank.genaidemo.domain.common.valueobject.Money.twd(100),
                    100,
                    validPeriod);
        }
    }
}
