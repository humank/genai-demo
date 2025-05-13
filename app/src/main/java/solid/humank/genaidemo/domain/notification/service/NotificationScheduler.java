package solid.humank.genaidemo.domain.notification.service;

import solid.humank.genaidemo.domain.notification.model.aggregate.Notification;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationStatus;
import solid.humank.genaidemo.domain.notification.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知排程器
 * 負責處理通知的排程和重試
 */
public class NotificationScheduler {
    private final NotificationRepository notificationRepository;
    private final NotificationSender notificationSender;
    
    // 最大重試次數
    private static final int MAX_RETRY_COUNT = 3;

    public NotificationScheduler(NotificationRepository notificationRepository, NotificationSender notificationSender) {
        this.notificationRepository = notificationRepository;
        this.notificationSender = notificationSender;
    }

    /**
     * 處理待發送的通知
     */
    public void processPendingNotifications() {
        // 獲取當前時間之前排程的待發送通知
        List<Notification> pendingNotifications = notificationRepository.findPendingNotificationsScheduledBefore(LocalDateTime.now());
        
        for (Notification notification : pendingNotifications) {
            boolean success = notificationSender.send(notification);
            
            if (success) {
                notification.send();
            } else {
                notification.markAsFailed("發送失敗");
            }
            
            notificationRepository.save(notification);
        }
    }

    /**
     * 重試發送失敗的通知
     */
    public void retryFailedNotifications() {
        // 獲取需要重試的通知
        List<Notification> notificationsForRetry = notificationRepository.findNotificationsForRetry(MAX_RETRY_COUNT);
        
        for (Notification notification : notificationsForRetry) {
            boolean success = notificationSender.send(notification);
            
            if (success) {
                notification.send();
            } else {
                // 增加重試次數
                notification.retry();
                // 設置下次重試時間
                LocalDateTime nextRetryTime = LocalDateTime.now().plusHours(1 << notification.getRetryCount());
                notification.schedule(nextRetryTime);
            }
            
            notificationRepository.save(notification);
        }
    }

    /**
     * 清理過期通知
     *
     * @param daysToKeep 保留天數
     */
    public void cleanupOldNotifications(int daysToKeep) {
        // 在實際應用中，這裡會刪除或歸檔舊的通知
        // 這裡只是示例
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        
        // 獲取所有通知
        List<Notification> allNotifications = notificationRepository.findAll();
        
        for (Notification notification : allNotifications) {
            if (notification.getCreatedAt().isBefore(cutoffDate)) {
                // 在實際應用中，這裡可能會將通知歸檔而不是刪除
                notificationRepository.delete(notification);
            }
        }
    }
}