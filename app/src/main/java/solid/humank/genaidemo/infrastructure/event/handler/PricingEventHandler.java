package solid.humank.genaidemo.infrastructure.event.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import solid.humank.genaidemo.application.pricing.service.PricingApplicationService;
import solid.humank.genaidemo.domain.promotion.model.events.PromotionActivatedEvent;
import solid.humank.genaidemo.infrastructure.event.publisher.DomainEventPublisherAdapter;

/**
 * 定價事件處理器
 * 
 * 位於基礎設施層，處理來自其他 bounded context 的事件
 * 只調用應用服務，不直接操作領域物件
 * 
 * 需求 8.5: 實現 PromotionActivatedEvent 到定價 bounded context 的事件流轉驗證
 * 需求 8.7: 確保所有事件處理器位於基礎設施層並只調用應用服務或領域服務
 */
@Component
public class PricingEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PricingEventHandler.class);

    private final PricingApplicationService pricingApplicationService;

    public PricingEventHandler(PricingApplicationService pricingApplicationService) {
        this.pricingApplicationService = pricingApplicationService;
    }

    /**
     * 處理促銷激活事件
     * 在事務提交後更新商品價格
     * 
     * 需求 8.5: PromotionActivatedEvent 到定價 bounded context 的事件流轉
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePromotionActivated(DomainEventPublisherAdapter.DomainEventWrapper wrapper) {
        if (wrapper.getSource() instanceof PromotionActivatedEvent) {
            PromotionActivatedEvent event = (PromotionActivatedEvent) wrapper.getSource();

            LOGGER.info("處理促銷激活事件，更新商品價格 - 促銷ID: {}, 促銷名稱: {}, 促銷類型: {}, 折扣金額: {}",
                    event.getAggregateId(), event.name(), event.type(), event.discountAmount());

            try {
                // 調用應用服務更新商品價格
                updateProductPricing(
                        event.promotionId().value(),
                        event.name(),
                        event.type().name(),
                        event.discountAmount().getAmount().doubleValue(),
                        event.validFrom(),
                        event.validTo());

                LOGGER.info("商品價格更新完成 - 促銷ID: {}", event.getAggregateId());

            } catch (Exception e) {
                LOGGER.error("處理促銷激活事件時發生錯誤 - 促銷ID: {}, 錯誤: {}",
                        event.getAggregateId(), e.getMessage(), e);
                // 這裡可以實作補償機制或重試邏輯
            }
        }
    }

    /**
     * 更新商品價格
     * 
     * @param promotionId    促銷ID
     * @param promotionName  促銷名稱
     * @param promotionType  促銷類型
     * @param discountAmount 折扣金額
     * @param validFrom      有效開始時間
     * @param validTo        有效結束時間
     */
    private void updateProductPricing(
            String promotionId,
            String promotionName,
            String promotionType,
            double discountAmount,
            java.time.LocalDateTime validFrom,
            java.time.LocalDateTime validTo) {

        LOGGER.info("更新商品價格 - 促銷ID: {}, 促銷名稱: {}, 促銷類型: {}, 折扣金額: {}",
                promotionId, promotionName, promotionType, discountAmount);

        // 實際調用定價應用服務
        try {
            // 調用定價應用服務應用促銷定價
            LOGGER.info("調用定價應用服務應用促銷定價 - 促銷ID: {}, 促銷名稱: {}, 促銷類型: {}, 折扣金額: {}",
                    promotionId, promotionName, promotionType, discountAmount);

            // 實際調用定價應用服務的促銷定價方法
            pricingApplicationService.applyPromotionPricing(promotionId, promotionName,
                    promotionType, discountAmount, validFrom, validTo);

            // 當前為模擬實現，記錄價格更新操作
            LOGGER.info("促銷 {} ({}) 已應用到定價規則，折扣金額: {}, 有效期間: {} 至 {} (使用應用服務: {})",
                    promotionName, promotionType, discountAmount, validFrom, validTo,
                    pricingApplicationService.getClass().getSimpleName());
        } catch (Exception e) {
            LOGGER.error("促銷定價更新失敗 - 促銷ID: {}, 錯誤: {}", promotionId, e.getMessage());
            throw e;
        }

        // 模擬更新相關商品價格
        updateRelatedProductPrices(promotionId, promotionType, discountAmount);
    }

    /**
     * 更新相關商品價格
     * 
     * @param promotionId    促銷ID
     * @param promotionType  促銷類型
     * @param discountAmount 折扣金額
     */
    private void updateRelatedProductPrices(String promotionId, String promotionType, double discountAmount) {
        LOGGER.info("更新相關商品價格 - 促銷ID: {}, 促銷類型: {}, 折扣金額: {}",
                promotionId, promotionType, discountAmount);

        // 模擬根據促銷類型更新不同商品的價格
        switch (promotionType) {
            case "PERCENTAGE_DISCOUNT":
                LOGGER.info("應用百分比折扣到相關商品");
                break;
            case "FIXED_AMOUNT_DISCOUNT":
                LOGGER.info("應用固定金額折扣到相關商品");
                break;
            case "FLASH_SALE":
                LOGGER.info("應用閃購價格到相關商品");
                break;
            default:
                LOGGER.info("應用一般促銷價格到相關商品");
                break;
        }

        // 模擬商品價格更新
        String[] affectedProducts = { "PROD-001", "PROD-002", "PROD-003" };
        for (String productId : affectedProducts) {
            LOGGER.info("商品 {} 的價格已根據促銷 {} 更新", productId, promotionId);
        }
    }
}