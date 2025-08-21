package solid.humank.genaidemo.infrastructure.notification.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import solid.humank.genaidemo.infrastructure.notification.persistence.entity.JpaNotificationEntity;

/**
 * 通知JPA儲存庫
 */
@Repository
public interface JpaNotificationRepository extends JpaRepository<JpaNotificationEntity, String> {

    /**
     * 根據客戶ID查詢通知列表
     */
    List<JpaNotificationEntity> findByCustomerId(String customerId);

    /**
     * 根據通知類型查詢通知列表
     */
    List<JpaNotificationEntity> findByType(String type);

    /**
     * 根據通知狀態查詢通知列表
     */
    List<JpaNotificationEntity> findByStatus(String status);

    /**
     * 查詢指定時間之前的待發送通知
     */
    @Query("SELECT n FROM JpaNotificationEntity n WHERE n.status = 'PENDING' AND n.scheduledTime <= :time")
    List<JpaNotificationEntity> findPendingNotificationsScheduledBefore(@Param("time") LocalDateTime time);

    /**
     * 查詢發送失敗的通知
     */
    @Query("SELECT n FROM JpaNotificationEntity n WHERE n.status = 'FAILED'")
    List<JpaNotificationEntity> findFailedNotifications();

    /**
     * 查詢需要重試的通知
     */
    @Query("SELECT n FROM JpaNotificationEntity n WHERE n.status = 'FAILED' AND n.retryCount < :maxRetryCount")
    List<JpaNotificationEntity> findNotificationsForRetry(@Param("maxRetryCount") int maxRetryCount);
}