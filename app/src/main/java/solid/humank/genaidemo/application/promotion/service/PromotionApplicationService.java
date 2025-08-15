package solid.humank.genaidemo.application.promotion.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import solid.humank.genaidemo.application.promotion.dto.FlashSaleDto;
import solid.humank.genaidemo.application.promotion.dto.PromotionDto;
import solid.humank.genaidemo.domain.promotion.model.aggregate.Promotion;
import solid.humank.genaidemo.domain.promotion.model.factory.PromotionFactory;
import solid.humank.genaidemo.domain.promotion.model.valueobject.FlashSaleRule;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionId;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionType;
import solid.humank.genaidemo.domain.promotion.repository.PromotionRepository;

/** 促銷應用服務 */
@Service
@Transactional
public class PromotionApplicationService {

    private final PromotionRepository promotionRepository;

    public PromotionApplicationService(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    /** 創建閃購促銷 */
    public PromotionDto createFlashSalePromotion(
            String name,
            String description,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String productId,
            double specialPrice,
            int quantityLimit) {

        Promotion promotion =
                PromotionFactory.createFlashSalePromotion(
                        name,
                        description,
                        startDate,
                        endDate,
                        productId,
                        specialPrice,
                        quantityLimit);

        Promotion savedPromotion = promotionRepository.save(promotion);
        return toDto(savedPromotion);
    }

    /** 獲取所有活躍促銷 */
    @Transactional(readOnly = true)
    public List<PromotionDto> getActivePromotions() {
        return promotionRepository.findActivePromotions().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /** 根據類型獲取促銷 */
    @Transactional(readOnly = true)
    public List<PromotionDto> getPromotionsByType(PromotionType type) {
        return promotionRepository.findByType(type).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /** 獲取促銷詳情 */
    @Transactional(readOnly = true)
    public Optional<PromotionDto> getPromotionById(String promotionId) {
        return promotionRepository.findById(PromotionId.of(promotionId)).map(this::toDto);
    }

    /** 獲取閃購促銷列表 */
    @Transactional(readOnly = true)
    public List<FlashSaleDto> getFlashSales() {
        return promotionRepository.findByType(PromotionType.FLASH_SALE).stream()
                .map(this::toFlashSaleDto)
                .collect(Collectors.toList());
    }

    private PromotionDto toDto(Promotion promotion) {
        return new PromotionDto(
                promotion.getId().value(),
                promotion.getName(),
                promotion.getDescription(),
                promotion.getType(),
                promotion.getStatus(),
                promotion.getValidPeriod().startDate(),
                promotion.getValidPeriod().endDate(),
                promotion.getUsageLimit(),
                promotion.getUsageCount(),
                promotion.getCreatedAt(),
                promotion.getUpdatedAt());
    }

    private FlashSaleDto toFlashSaleDto(Promotion promotion) {
        // 從促銷規則中獲取詳細信息
        Optional<FlashSaleRule> ruleOpt = promotion.getFlashSaleRule();
        if (ruleOpt.isPresent()) {
            FlashSaleRule rule = ruleOpt.get();
            return new FlashSaleDto(
                    promotion.getId().value(),
                    rule.targetProductId().getId(),
                    getProductName(rule.targetProductId().getId()),
                    java.math.BigDecimal.valueOf(120), // 預設原價
                    rule.specialPrice().getAmount(),
                    rule.quantityLimit(),
                    getRemainingQuantity(promotion.getId().value()),
                    promotion.getValidPeriod().startDate(),
                    promotion.getValidPeriod().endDate(),
                    promotion.canUse());
        }

        // 如果不是閃購促銷，返回預設值
        return new FlashSaleDto(
                promotion.getId().value(),
                "unknown-product",
                "未知商品",
                java.math.BigDecimal.valueOf(100),
                java.math.BigDecimal.valueOf(80),
                100,
                50,
                promotion.getValidPeriod().startDate(),
                promotion.getValidPeriod().endDate(),
                promotion.canUse());
    }

    private String getProductName(String productId) {
        // 簡化實現：根據產品ID生成商品名稱
        // 實際應該從產品服務獲取
        return "商品 " + productId;
    }

    private int getRemainingQuantity(String promotionId) {
        // 簡化實現：返回預設剩餘數量
        // 實際應該從庫存服務獲取
        return 50;
    }
}
