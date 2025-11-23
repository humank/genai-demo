package solid.humank.genaidemo.infrastructure.event.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.shoppingcart.model.events.CartItemAddedEvent;
import solid.humank.genaidemo.infrastructure.event.publisher.DomainEventPublisherAdapter;

/**
 * 推薦事件處理器
 * 
 * 位於基礎設施層，處理來自其他 bounded context 的事件
 * 只調用應用服務，不直接操作領域物件
 * 
 * 需求 8.4: 實現 CartItemAddedEvent 到推薦 bounded context 的事件流轉驗證
 * 需求 8.7: 確保所有事件處理器位於基礎設施層並只調用應用服務或領域服務
 * 
 * 注意：推薦系統通常需要即時響應，所以使用 @EventListener 而非 @TransactionalEventListener
 */
@Component
public class RecommendationEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecommendationEventHandler.class);

    // 注意：這裡應該注入推薦應用服務，但目前專案中沒有推薦 bounded context
    // recommendationApplicationService;

    public RecommendationEventHandler() {
        // 暫時不注入推薦應用服務，因為專案中沒有實作推薦 bounded context
    }

    /**
     * 處理購物車商品添加事件
     * 即時更新推薦算法
     * 
     * 需求 8.4: CartItemAddedEvent 到推薦 bounded context 的事件流轉
     */
    @EventListener
    public void handleCartItemAdded(DomainEventPublisherAdapter.DomainEventWrapper wrapper) {
        if (wrapper.getSource() instanceof CartItemAddedEvent) {
            CartItemAddedEvent event = (CartItemAddedEvent) wrapper.getSource();

            LOGGER.info("處理購物車商品添加事件，更新推薦算法 - 購物車ID: {}, 客戶ID: {}, 商品ID: {}, 數量: {}",
                    event.cartId().value(), event.customerId().getId(),
                    event.productId().getId(), event.quantity());

            try {
                // 調用應用服務更新推薦算法
                updateRecommendationAlgorithm(
                        event.customerId().getId(),
                        event.productId().getId(),
                        event.quantity(),
                        event.unitPrice().getAmount().doubleValue());

                LOGGER.info("推薦算法更新完成 - 客戶ID: {}, 商品ID: {}",
                        event.customerId().getId(), event.productId().getId());

            } catch (Exception e) {
                LOGGER.error("處理購物車商品添加事件時發生錯誤 - 購物車ID: {}, 錯誤: {}",
                        event.cartId().value(), e.getMessage(), e);
                // 推薦系統的錯誤通常不需要回滾主要業務流程
            }
        }
    }

    /**
     * 更新推薦算法
     * 
     * @param customerId 客戶ID
     * @param productId  商品ID
     * @param quantity   數量
     * @param unitPrice  單價
     */
    private void updateRecommendationAlgorithm(String customerId, String productId, int quantity, double unitPrice) {
        LOGGER.info("更新推薦算法 - 客戶ID: {}, 商品ID: {}, 數量: {}, 單價: {}",
                customerId, productId, quantity, unitPrice);

        // 實際調用推薦應用服務
        // productId, quantity, unitPrice);

        // 模擬實作：記錄推薦更新
        LOGGER.info("客戶 {} 對商品 {} 的偏好已更新，購買數量: {}, 價格敏感度: {}",
                customerId, productId, quantity, unitPrice);

        // 模擬生成相關推薦
        generateRelatedRecommendations(customerId, productId);
    }

    /**
     * 生成相關推薦
     * 
     * @param customerId 客戶ID
     * @param productId  商品ID
     */
    private void generateRelatedRecommendations(String customerId, String productId) {
        LOGGER.info("為客戶 {} 基於商品 {} 生成相關推薦", customerId, productId);

        // 模擬推薦邏輯
        String[] relatedProducts = { "PROD-001", "PROD-002", "PROD-003" };

        for (String relatedProduct : relatedProducts) {
            LOGGER.info("推薦商品: {} 給客戶: {}", relatedProduct, customerId);
        }
    }
}