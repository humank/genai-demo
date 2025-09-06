package solid.humank.genaidemo.infrastructure.event.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import solid.humank.genaidemo.application.inventory.service.InventoryApplicationService;
import solid.humank.genaidemo.domain.order.model.events.OrderCreatedEvent;
import solid.humank.genaidemo.infrastructure.event.publisher.DomainEventPublisherAdapter;

/**
 * 庫存事件處理器
 * 
 * 位於基礎設施層，處理來自其他 bounded context 的事件
 * 只調用應用服務，不直接操作領域物件
 * 
 * 需求 8.1: 實現 OrderCreatedEvent 到庫存 bounded context 的事件流轉驗證
 * 需求 8.7: 確保所有事件處理器位於基礎設施層並只調用應用服務或領域服務
 */
@Component
public class InventoryEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryEventHandler.class);

    private final InventoryApplicationService inventoryApplicationService;

    public InventoryEventHandler(InventoryApplicationService inventoryApplicationService) {
        this.inventoryApplicationService = inventoryApplicationService;
    }

    /**
     * 處理訂單創建事件
     * 在事務提交後預留庫存
     * 
     * 需求 8.1: OrderCreatedEvent 到庫存 bounded context 的事件流轉
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(DomainEventPublisherAdapter.DomainEventWrapper wrapper) {
        if (wrapper.getSource() instanceof OrderCreatedEvent) {
            OrderCreatedEvent event = (OrderCreatedEvent) wrapper.getSource();

            LOGGER.info("處理訂單創建事件，開始預留庫存 - 訂單ID: {}, 客戶ID: {}",
                    event.getAggregateId(), event.customerId());

            try {
                // 調用應用服務處理庫存預留
                // 這裡應該根據訂單中的商品項目進行庫存預留
                for (String item : event.items()) {
                    // 解析商品項目並預留庫存
                    // 實際實作中應該有更詳細的商品資訊和數量
                    reserveInventoryForOrderItem(event.getAggregateId(), item);
                }

                LOGGER.info("訂單庫存預留完成 - 訂單ID: {}", event.getAggregateId());

            } catch (Exception e) {
                LOGGER.error("處理訂單創建事件時發生錯誤 - 訂單ID: {}, 錯誤: {}",
                        event.getAggregateId(), e.getMessage(), e);
                // 這裡可以實作補償機制或重試邏輯
            }
        }
    }

    /**
     * 為訂單項目預留庫存
     * 
     * @param orderId 訂單ID
     * @param item    商品項目
     */
    private void reserveInventoryForOrderItem(String orderId, String item) {
        // 這裡應該解析商品項目，獲取商品ID和數量
        // 然後調用庫存應用服務進行預留

        // 模擬實作：假設 item 格式為 "productId:quantity"
        String[] parts = item.split(":");
        if (parts.length == 2) {
            String productId = parts[0];
            int quantity = Integer.parseInt(parts[1]);

            LOGGER.info("預留庫存 - 商品ID: {}, 數量: {}, 訂單ID: {}",
                    productId, quantity, orderId);

            // 實際調用庫存應用服務
            try {
                // 調用庫存應用服務進行預留
                LOGGER.info("調用庫存應用服務預留庫存 - 商品ID: {}, 數量: {}, 訂單ID: {}", productId, quantity, orderId);

                // 實際調用庫存應用服務的預留方法
                inventoryApplicationService.reserveInventory(productId, quantity, orderId);

                // 當前為模擬實現，記錄預留操作
                LOGGER.info("庫存預留操作已記錄 - 商品ID: {}, 數量: {}, 訂單ID: {} (使用應用服務: {})",
                        productId, quantity, orderId, inventoryApplicationService.getClass().getSimpleName());
            } catch (Exception e) {
                LOGGER.error("庫存預留失敗 - 商品ID: {}, 數量: {}, 訂單ID: {}, 錯誤: {}",
                        productId, quantity, orderId, e.getMessage());
                throw e;
            }
        }
    }
}