package solid.humank.genaidemo.infrastructure.notification.persistence.mapper;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.notification.model.aggregate.NotificationTemplate;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationChannel;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationType;
import solid.humank.genaidemo.infrastructure.notification.persistence.entity.JpaNotificationTemplateEntity;

/** 通知模板映射器 */
@Component
public class NotificationTemplateMapper {

    public JpaNotificationTemplateEntity toJpaEntity(NotificationTemplate template) {
        JpaNotificationTemplateEntity entity = new JpaNotificationTemplateEntity();
        entity.setTemplateId(template.getTemplateId());
        entity.setType(template.getType().name());
        entity.setChannel(template.getChannel().name());
        entity.setName(template.getName());
        entity.setSubject(template.getSubject());
        entity.setContent(template.getContent());
        entity.setVariables(template.getVariables());
        entity.setActive(template.isActive());
        entity.setCreatedAt(template.getCreatedAt());
        entity.setUpdatedAt(template.getUpdatedAt());
        entity.setCreatedBy(template.getCreatedBy());
        entity.setUpdatedBy(template.getUpdatedBy());
        return entity;
    }

    public NotificationTemplate toDomainModel(JpaNotificationTemplateEntity entity) {
        NotificationType type = NotificationType.valueOf(entity.getType());
        NotificationChannel channel = NotificationChannel.valueOf(entity.getChannel());

        return new NotificationTemplate(
                entity.getTemplateId(),
                type,
                channel,
                entity.getName(),
                entity.getSubject(),
                entity.getContent(),
                entity.getCreatedBy());
    }
}