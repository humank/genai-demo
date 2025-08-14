package solid.humank.genaidemo.domain.notification.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import solid.humank.genaidemo.domain.common.repository.Repository;
import solid.humank.genaidemo.domain.notification.model.aggregate.Notification;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationId;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationStatus;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationType;

/** 通知儲存庫接口 */
public interface NotificationRepository extends Repository<Notification, NotificationId> {

    /**
     * 保存通知
     *
     * @param notification 通知
     * @return 保存後的通知
     */
    @Override
    Notification save(Notification notification);

    /**
     * 根據ID查詢通知
     *
     * @param id 通知ID
     * @return 通知
     */
    Optional<Notification> findById(NotificationId id);

    /**
     * 根據客戶ID查詢通知列表
     *
     * @param customerId 客戶ID
     * @return 通知列表
     */
    List<Notification> findByCustomerId(String customerId);

    /**
     * 根據通知類型查詢通知列表
     *
     * @param type 通知類型
     * @return 通知列表
     */
    List<Notification> findByType(NotificationType type);

    /**
     * 根據通知狀態查詢通知列表
     *
     * @param status 通知狀態
     * @return 通知列表
     */
    List<Notification> findByStatus(NotificationStatus status);

    /**
     * 查詢指定時間之前的待發送通知
     *
     * @param time 時間
     * @return 通知列表
     */
    List<Notification> findPendingNotificationsScheduledBefore(LocalDateTime time);

    /**
     * 查詢發送失敗的通知
     *
     * @return 通知列表
     */
    List<Notification> findFailedNotifications();

    /**
     * 查詢需要重試的通知
     *
     * @param maxRetryCount 最大重試次數
     * @return 通知列表
     */
    List<Notification> findNotificationsForRetry(int maxRetryCount);
}
