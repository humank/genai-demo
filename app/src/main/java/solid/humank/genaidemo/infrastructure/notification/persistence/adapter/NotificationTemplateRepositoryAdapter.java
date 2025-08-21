package solid.humank.genaidemo.infrastructure.notification.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.notification.model.aggregate.NotificationTemplate;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationChannel;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationType;
import solid.humank.genaidemo.domain.notification.repository.NotificationTemplateRepository;
import solid.humank.genaidemo.infrastructure.common.persistence.adapter.BaseRepositoryAdapter;
import solid.humank.genaidemo.infrastructure.notification.persistence.entity.JpaNotificationTemplateEntity;
import solid.humank.genaidemo.infrastructure.notification.persistence.mapper.NotificationTemplateMapper;
import solid.humank.genaidemo.infrastructure.notification.persistence.repository.JpaNotificationTemplateRepository;

/** 通知模板儲存庫適配器 */
@Component
public class NotificationTemplateRepositoryAdapter
        extends BaseRepositoryAdapter<NotificationTemplate, String, JpaNotificationTemplateEntity, String>
        implements NotificationTemplateRepository {

    private final JpaNotificationTemplateRepository jpaNotificationTemplateRepository;
    private final NotificationTemplateMapper mapper;

    public NotificationTemplateRepositoryAdapter(JpaNotificationTemplateRepository jpaNotificationTemplateRepository,
            NotificationTemplateMapper mapper) {
        super(jpaNotificationTemplateRepository);
        this.jpaNotificationTemplateRepository = jpaNotificationTemplateRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<NotificationTemplate> findByTypeAndChannel(NotificationType type, NotificationChannel channel) {
        return jpaNotificationTemplateRepository.findByTypeAndChannel(type.name(), channel.name())
                .map(mapper::toDomainModel);
    }

    @Override
    public List<NotificationTemplate> findByType(NotificationType type) {
        return jpaNotificationTemplateRepository.findByType(type.name())
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationTemplate> findByChannel(NotificationChannel channel) {
        return jpaNotificationTemplateRepository.findByChannel(channel.name())
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationTemplate> findActiveTemplates() {
        return jpaNotificationTemplateRepository.findByIsActiveTrue()
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationTemplate> findByNameContaining(String name) {
        return jpaNotificationTemplateRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    // BaseRepositoryAdapter required methods
    @Override
    protected JpaNotificationTemplateEntity toJpaEntity(NotificationTemplate aggregateRoot) {
        return mapper.toJpaEntity(aggregateRoot);
    }

    @Override
    protected NotificationTemplate toDomainModel(JpaNotificationTemplateEntity entity) {
        return mapper.toDomainModel(entity);
    }

    @Override
    protected String convertToJpaId(String domainId) {
        return domainId;
    }

    @Override
    protected String extractId(NotificationTemplate aggregateRoot) {
        return aggregateRoot.getTemplateId();
    }
}