package solid.humank.genaidemo.infrastructure.notification.persistence.adapter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import solid.humank.genaidemo.domain.notification.model.aggregate.Notification;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationId;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationStatus;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationType;
import solid.humank.genaidemo.domain.notification.repository.NotificationRepository;
import solid.humank.genaidemo.infrastructure.common.persistence.adapter.BaseRepositoryAdapter;
import solid.humank.genaidemo.infrastructure.notification.persistence.entity.JpaNotificationEntity;
import solid.humank.genaidemo.infrastructure.notification.persistence.mapper.NotificationMapper;
import solid.humank.genaidemo.infrastructure.notification.persistence.repository.JpaNotificationRepository;

/**
 * 通知儲存庫適配器
 * 實現領域儲存庫接口，遵循統一的 Repository Pattern
 */
@Component
@Transactional
public class NotificationRepositoryAdapter
        extends BaseRepositoryAdapter<Notification, NotificationId, JpaNotificationEntity, String>
        implements NotificationRepository {

    private final NotificationMapper mapper;

    public NotificationRepositoryAdapter(JpaNotificationRepository jpaRepository, NotificationMapper mapper) {
        super(jpaRepository);
        this.mapper = mapper;
    }

    @Override
    protected JpaNotificationEntity toJpaEntity(Notification aggregateRoot) {
        return mapper.toJpaEntity(aggregateRoot);
    }

    @Override
    protected Notification toDomainModel(JpaNotificationEntity entity) {
        return mapper.toDomainModel(entity);
    }

    @Override
    protected String convertToJpaId(NotificationId domainId) {
        return domainId.getValue();
    }

    @Override
    protected NotificationId extractId(Notification aggregateRoot) {
        return aggregateRoot.getId();
    }

    @Override
    public List<Notification> findByCustomerId(String customerId) {
        return ((JpaNotificationRepository) jpaRepository).findByCustomerId(customerId)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<Notification> findByType(NotificationType type) {
        return ((JpaNotificationRepository) jpaRepository).findByType(type.name())
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<Notification> findByStatus(NotificationStatus status) {
        return ((JpaNotificationRepository) jpaRepository).findByStatus(status.name())
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<Notification> findPendingNotificationsScheduledBefore(LocalDateTime time) {
        return ((JpaNotificationRepository) jpaRepository).findPendingNotificationsScheduledBefore(time)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<Notification> findFailedNotifications() {
        return ((JpaNotificationRepository) jpaRepository).findFailedNotifications()
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<Notification> findNotificationsForRetry(int maxRetryCount) {
        return ((JpaNotificationRepository) jpaRepository).findNotificationsForRetry(maxRetryCount)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }
}