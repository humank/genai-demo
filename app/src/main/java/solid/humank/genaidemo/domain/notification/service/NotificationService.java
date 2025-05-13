package solid.humank.genaidemo.domain.notification.service;

import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.notification.model.aggregate.Notification;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationChannel;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationId;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationType;
import solid.humank.genaidemo.domain.notification.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 通知服務
 * 負責處理通知的創建、發送和管理
 */
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationSender notificationSender;

    public NotificationService(NotificationRepository notificationRepository, NotificationSender notificationSender) {
        this.notificationRepository = notificationRepository;
        this.notificationSender = notificationSender;
    }

    /**
     * 發送訂單創建通知
     *
     * @param customerId 客戶ID
     * @param orderId 訂單ID
     * @param orderCreationTime 訂單創建時間
     * @param channels 通知渠道
     * @return 通知ID
     */
    public NotificationId sendOrderCreationNotification(String customerId, OrderId orderId, LocalDateTime orderCreationTime, List<NotificationChannel> channels) {
        String subject = "訂單創建通知";
        String content = String.format("您的訂單 %s 已於 %s 創建成功。", orderId.toString(), orderCreationTime);
        
        Notification notification = new Notification(
                customerId,
                NotificationType.ORDER_CREATED,
                subject,
                content,
                channels
        );
        
        notificationRepository.save(notification);
        notificationSender.send(notification);
        
        return notification.getId();
    }

    /**
     * 發送訂單確認通知
     *
     * @param customerId 客戶ID
     * @param orderId 訂單ID
     * @param orderDetails 訂單詳情
     * @param estimatedDeliveryTime 預計配送時間
     * @param channels 通知渠道
     * @return 通知ID
     */
    public NotificationId sendOrderConfirmationNotification(String customerId, OrderId orderId, Map<String, Object> orderDetails, LocalDateTime estimatedDeliveryTime, List<NotificationChannel> channels) {
        String subject = "訂單確認通知";
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append(String.format("您的訂單 %s 已確認。\n", orderId.toString()));
        contentBuilder.append("訂單詳情：\n");
        
        for (Map.Entry<String, Object> entry : orderDetails.entrySet()) {
            contentBuilder.append(String.format("%s: %s\n", entry.getKey(), entry.getValue()));
        }
        
        contentBuilder.append(String.format("預計配送時間：%s", estimatedDeliveryTime));
        
        Notification notification = new Notification(
                customerId,
                NotificationType.ORDER_CONFIRMED,
                subject,
                contentBuilder.toString(),
                channels
        );
        
        notificationRepository.save(notification);
        notificationSender.send(notification);
        
        return notification.getId();
    }

    /**
     * 發送支付失敗通知
     *
     * @param customerId 客戶ID
     * @param orderId 訂單ID
     * @param failureReason 失敗原因
     * @param retryPaymentLink 重新支付鏈接
     * @param channels 通知渠道
     * @return 通知ID
     */
    public NotificationId sendPaymentFailureNotification(String customerId, OrderId orderId, String failureReason, String retryPaymentLink, List<NotificationChannel> channels) {
        String subject = "支付失敗通知";
        String content = String.format("您的訂單 %s 支付失敗。\n失敗原因：%s\n點擊以下鏈接重新支付：%s", 
                orderId.toString(), failureReason, retryPaymentLink);
        
        Notification notification = new Notification(
                customerId,
                NotificationType.PAYMENT_FAILED,
                subject,
                content,
                channels
        );
        
        notificationRepository.save(notification);
        notificationSender.send(notification);
        
        return notification.getId();
    }

    /**
     * 發送庫存不足通知
     *
     * @param customerId 客戶ID
     * @param orderId 訂單ID
     * @param outOfStockProducts 庫存不足的商品
     * @param alternativeProducts 替代商品建議
     * @param channels 通知渠道
     * @return 通知ID
     */
    public NotificationId sendInsufficientInventoryNotification(String customerId, OrderId orderId, List<String> outOfStockProducts, Map<String, String> alternativeProducts, List<NotificationChannel> channels) {
        String subject = "庫存不足通知";
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append(String.format("很抱歉，您的訂單 %s 中的以下商品庫存不足：\n", orderId.toString()));
        
        for (String product : outOfStockProducts) {
            contentBuilder.append(String.format("- %s\n", product));
        }
        
        if (!alternativeProducts.isEmpty()) {
            contentBuilder.append("\n您可以考慮以下替代商品：\n");
            for (Map.Entry<String, String> entry : alternativeProducts.entrySet()) {
                contentBuilder.append(String.format("- %s 替代 %s\n", entry.getValue(), entry.getKey()));
            }
        }
        
        Notification notification = new Notification(
                customerId,
                NotificationType.INVENTORY_INSUFFICIENT,
                subject,
                contentBuilder.toString(),
                channels
        );
        
        notificationRepository.save(notification);
        notificationSender.send(notification);
        
        return notification.getId();
    }

    /**
     * 發送配送狀態更新通知
     *
     * @param customerId 客戶ID
     * @param orderId 訂單ID
     * @param deliveryStatus 配送狀態
     * @param estimatedDeliveryTime 預計配送時間
     * @param trackingLink 配送追蹤鏈接
     * @param channels 通知渠道
     * @return 通知ID
     */
    public NotificationId sendDeliveryStatusUpdateNotification(String customerId, OrderId orderId, String deliveryStatus, LocalDateTime estimatedDeliveryTime, String trackingLink, List<NotificationChannel> channels) {
        String subject = "配送狀態更新通知";
        String content = String.format("您的訂單 %s 配送狀態已更新為：%s\n預計配送時間：%s\n點擊以下鏈接追蹤配送：%s", 
                orderId.toString(), deliveryStatus, estimatedDeliveryTime, trackingLink);
        
        Notification notification = new Notification(
                customerId,
                NotificationType.DELIVERY_STATUS_UPDATE,
                subject,
                content,
                channels
        );
        
        notificationRepository.save(notification);
        notificationSender.send(notification);
        
        return notification.getId();
    }

    /**
     * 發送訂單完成通知
     *
     * @param customerId 客戶ID
     * @param orderId 訂單ID
     * @param ratingLink 評價鏈接
     * @param recommendations 相關商品推薦
     * @param channels 通知渠道
     * @return 通知ID
     */
    public NotificationId sendOrderCompletionNotification(String customerId, OrderId orderId, String ratingLink, List<String> recommendations, List<NotificationChannel> channels) {
        String subject = "訂單完成通知";
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append(String.format("您的訂單 %s 已完成。\n", orderId.toString()));
        contentBuilder.append(String.format("點擊以下鏈接評價訂單：%s\n\n", ratingLink));
        
        if (!recommendations.isEmpty()) {
            contentBuilder.append("您可能還會喜歡：\n");
            for (String recommendation : recommendations) {
                contentBuilder.append(String.format("- %s\n", recommendation));
            }
        }
        
        Notification notification = new Notification(
                customerId,
                NotificationType.ORDER_COMPLETED,
                subject,
                contentBuilder.toString(),
                channels
        );
        
        notificationRepository.save(notification);
        notificationSender.send(notification);
        
        return notification.getId();
    }

    /**
     * 發送訂單取消通知
     *
     * @param customerId 客戶ID
     * @param orderId 訂單ID
     * @param cancellationReason 取消原因
     * @param channels 通知渠道
     * @return 通知ID
     */
    public NotificationId sendOrderCancellationNotification(String customerId, OrderId orderId, String cancellationReason, List<NotificationChannel> channels) {
        String subject = "訂單取消通知";
        String content = String.format("您的訂單 %s 已取消。\n取消原因：%s", 
                orderId.toString(), cancellationReason);
        
        Notification notification = new Notification(
                customerId,
                NotificationType.ORDER_CANCELLED,
                subject,
                content,
                channels
        );
        
        notificationRepository.save(notification);
        notificationSender.send(notification);
        
        return notification.getId();
    }

    /**
     * 處理通知發送失敗
     *
     * @param notificationId 通知ID
     * @param failureReason 失敗原因
     * @return 是否處理成功
     */
    public boolean handleNotificationFailure(NotificationId notificationId, String failureReason) {
        Optional<Notification> optionalNotification = notificationRepository.findById(notificationId);
        if (optionalNotification.isEmpty()) {
            return false;
        }
        
        Notification notification = optionalNotification.get();
        notification.markAsFailed(failureReason);
        notificationRepository.save(notification);
        
        // 嘗試通過其他渠道發送
        if (notification.getChannels().size() > 1) {
            notificationSender.sendThroughAlternativeChannels(notification);
        }
        
        // 排程重試
        scheduleRetry(notification);
        
        return true;
    }

    /**
     * 排程重試發送通知
     *
     * @param notification 通知
     */
    private void scheduleRetry(Notification notification) {
        if (notification.getRetryCount() < 3) {
            // 設置重試時間，每次重試間隔增加
            LocalDateTime retryTime = LocalDateTime.now().plusHours(1 << notification.getRetryCount());
            notification.retry();
            notification.schedule(retryTime);
            notificationRepository.save(notification);
        }
    }

    /**
     * 更新客戶通知偏好設置
     *
     * @param customerId 客戶ID
     * @param selectedTypes 選擇的通知類型
     * @param selectedChannels 選擇的通知渠道
     * @return 是否更新成功
     */
    public boolean updateCustomerNotificationPreferences(String customerId, List<NotificationType> selectedTypes, List<NotificationChannel> selectedChannels) {
        // 在實際應用中，這裡會與客戶資料庫交互
        // 這裡只是示例，返回成功
        return true;
    }
}