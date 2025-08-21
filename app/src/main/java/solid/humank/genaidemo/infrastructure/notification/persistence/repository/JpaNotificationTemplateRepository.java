package solid.humank.genaidemo.infrastructure.notification.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import solid.humank.genaidemo.infrastructure.notification.persistence.entity.JpaNotificationTemplateEntity;

/** 通知模板JPA儲存庫 */
@Repository
public interface JpaNotificationTemplateRepository extends JpaRepository<JpaNotificationTemplateEntity, String> {

    Optional<JpaNotificationTemplateEntity> findByTypeAndChannel(String type, String channel);

    List<JpaNotificationTemplateEntity> findByType(String type);

    List<JpaNotificationTemplateEntity> findByChannel(String channel);

    List<JpaNotificationTemplateEntity> findByIsActiveTrue();

    List<JpaNotificationTemplateEntity> findByNameContainingIgnoreCase(String name);
}