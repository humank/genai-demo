package solid.humank.genaidemo.domain.order.service;

import java.util.logging.Logger;
import org.springframework.stereotype.Service;
import solid.humank.genaidemo.domain.common.event.EventSubscriber;
import solid.humank.genaidemo.domain.order.model.events.OrderCreatedEvent;
import solid.humank.genaidemo.domain.order.model.events.OrderItemAddedEvent;
import solid.humank.genaidemo.domain.order.model.events.OrderSubmittedEvent;

/** 訂單事件處理服務 處理訂單相關的領域事件 */
@Service
public class OrderEventHandler {
    private static final Logger LOGGER = Logger.getLogger(OrderEventHandler.class.getName());

    /**
     * 處理訂單創建事件
     *
     * @param event 訂單創建事件
     */
    @EventSubscriber(OrderCreatedEvent.class)
    public void handleOrderCreated(OrderCreatedEvent event) {
        LOGGER.info(
                () ->
                        String.format(
                                "處理訂單創建事件: 訂單ID=%s, 客戶ID=%s, 總金額=%s",
                                event.getOrderId(), event.getCustomerId(), event.getTotalAmount()));

        // 這裡可以添加訂單創建後的業務邏輯
        // 例如：發送確認郵件、更新庫存、記錄統計數據等
    }

    /**
     * 處理訂單項添加事件
     *
     * @param event 訂單項添加事件
     */
    @EventSubscriber(OrderItemAddedEvent.class)
    public void handleOrderItemAdded(OrderItemAddedEvent event) {
        LOGGER.info(
                () ->
                        String.format(
                                "處理訂單項添加事件: 訂單ID=%s, 產品ID=%s, 數量=%d, 價格=%s",
                                event.getOrderId(),
                                event.getProductId(),
                                event.getQuantity(),
                                event.getPrice()));

        // 這裡可以添加訂單項添加後的業務邏輯
        // 例如：更新庫存、計算折扣等
    }

    /**
     * 處理訂單提交事件
     *
     * @param event 訂單提交事件
     */
    @EventSubscriber(OrderSubmittedEvent.class)
    public void handleOrderSubmitted(OrderSubmittedEvent event) {
        LOGGER.info(
                () ->
                        String.format(
                                "處理訂單提交事件: 訂單ID=%s, 客戶ID=%s, 總金額=%s, 項目數=%d",
                                event.getOrderId(),
                                event.getCustomerId(),
                                event.getTotalAmount(),
                                event.getItemCount()));

        // 這裡可以添加訂單提交後的業務邏輯
        // 例如：通知庫存系統、準備發貨、發送訂單確認郵件等
    }
}
