package solid.humank.genaidemo.infrastructure.event.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import solid.humank.genaidemo.application.notification.service.NotificationApplicationService;
import solid.humank.genaidemo.application.order.service.OrderApplicationService;
import solid.humank.genaidemo.domain.common.event.EventSubscriber;
import solid.humank.genaidemo.domain.order.model.events.OrderCreatedEvent;
import solid.humank.genaidemo.domain.order.model.events.OrderItemAddedEvent;
import solid.humank.genaidemo.domain.order.model.events.OrderSubmittedEvent;
import solid.humank.genaidemo.domain.payment.model.events.PaymentCompletedEvent;
import solid.humank.genaidemo.infrastructure.event.publisher.DomainEventPublisherAdapter;

/**
 * 訂單事件處理器
 * 
 * 位於基礎設施層，處理來自其他 bounded context 的事件
 * 只調用應用服務，不直接操作領域物件
 * 
 * 需求 8.2: 實現 PaymentCompletedEvent 到訂單 bounded context 的事件流轉驗證
 * 需求 8.7: 確保所有事件處理器位於基礎設施層並只調用應用服務或領域服務
 */
@Component
public class OrderEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderEventHandler.class);

    private final OrderApplicationService orderApplicationService;
    private final NotificationApplicationService notificationApplicationService;

    public OrderEventHandler(OrderApplicationService orderApplicationService,
            NotificationApplicationService notificationApplicationService) {
        this.orderApplicationService = orderApplicationService;
        this.notificationApplicationService = notificationApplicationService;
    }

    /**
     * 處理訂單創建事件
     *
     * @param event 訂單創建事件
     */
    @EventSubscriber(OrderCreatedEvent.class)
    public void handleOrderCreated(OrderCreatedEvent event) {
        LOGGER.info("處理訂單創建事件: 訂單ID={}, 客戶ID={}, 總金額={}",
                event.orderId(), event.customerId(), event.totalAmount());

        // 調用應用服務處理訂單創建後的業務邏輯
        notificationApplicationService.sendOrderNotification(
                event.customerId(),
                event.orderId().toString(),
                "CREATED");
    }

    /**
     * 處理訂單項添加事件
     *
     * @param event 訂單項添加事件
     */
    @EventSubscriber(OrderItemAddedEvent.class)
    public void handleOrderItemAdded(OrderItemAddedEvent event) {
        LOGGER.info("處理訂單項添加事件: 訂單ID={}, 產品ID={}, 數量={}, 價格={}",
                event.orderId(), event.productId(), event.quantity(), event.price());

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
        LOGGER.info("處理訂單提交事件: 訂單ID={}, 客戶ID={}, 總金額={}, 項目數={}",
                event.orderId(), event.customerId(), event.totalAmount(), event.itemCount());

        // 調用應用服務處理訂單提交後的業務邏輯
        notificationApplicationService.sendOrderNotification(
                event.customerId(),
                event.orderId().toString(),
                "SUBMITTED");
    }

    /**
     * 處理支付完成事件
     * 在事務提交後更新訂單狀態為已支付
     * 
     * 需求 8.2: PaymentCompletedEvent 到訂單 bounded context 的事件流轉
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCompleted(DomainEventPublisherAdapter.DomainEventWrapper wrapper) {
        if (wrapper.getSource() instanceof PaymentCompletedEvent) {
            PaymentCompletedEvent event = (PaymentCompletedEvent) wrapper.getSource();

            LOGGER.info("處理支付完成事件，更新訂單狀態 - 支付ID: {}, 訂單ID: {}, 金額: {}",
                    event.getAggregateId(), event.orderId(), event.amount());

            try {
                // 調用應用服務更新訂單狀態
                updateOrderStatusToPaid(event.orderId().getValue(), event.transactionId());

                LOGGER.info("訂單狀態更新完成 - 訂單ID: {}, 交易ID: {}",
                        event.orderId().getId(), event.transactionId());

            } catch (Exception e) {
                LOGGER.error("處理支付完成事件時發生錯誤 - 支付ID: {}, 訂單ID: {}, 錯誤: {}",
                        event.getAggregateId(), event.orderId(), e.getMessage(), e);
                // 這裡可以實作補償機制或重試邏輯
            }
        }
    }

    /**
     * 更新訂單狀態為已支付
     * 
     * @param orderId       訂單ID
     * @param transactionId 交易ID
     */
    private void updateOrderStatusToPaid(String orderId, String transactionId) {
        LOGGER.info("更新訂單狀態為已支付 - 訂單ID: {}, 交易ID: {}", orderId, transactionId);

        // 實際調用訂單應用服務
        // orderApplicationService.markOrderAsPaid(orderId, transactionId);

        // 模擬實作：記錄狀態變更
        LOGGER.info("訂單 {} 已標記為已支付，交易ID: {}", orderId, transactionId);
    }
}