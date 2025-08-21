package solid.humank.genaidemo.domain.notification.repository;

import java.util.List;
import java.util.Optional;

import solid.humank.genaidemo.domain.notification.model.aggregate.NotificationTemplate;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationChannel;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationType;

/** 通知模板儲存庫接口 */
@solid.humank.genaidemo.domain.common.annotations.Repository(name = "NotificationTemplateRepository", description = "通知模板聚合根儲存庫")
public interface NotificationTemplateRepository
        extends solid.humank.genaidemo.domain.common.repository.BaseRepository<NotificationTemplate, String> {

    /**
     * 根據類型和渠道查詢模板
     * 
     * @param type    通知類型
     * @param channel 通知渠道
     * @return 模板（如果存在）
     */
    Optional<NotificationTemplate> findByTypeAndChannel(NotificationType type, NotificationChannel channel);

    /**
     * 根據類型查詢模板
     * 
     * @param type 通知類型
     * @return 模板列表
     */
    List<NotificationTemplate> findByType(NotificationType type);

    /**
     * 根據渠道查詢模板
     * 
     * @param channel 通知渠道
     * @return 模板列表
     */
    List<NotificationTemplate> findByChannel(NotificationChannel channel);

    /**
     * 查詢活躍的模板
     * 
     * @return 活躍模板列表
     */
    List<NotificationTemplate> findActiveTemplates();

    /**
     * 根據名稱模糊查詢模板
     * 
     * @param name 模板名稱
     * @return 模板列表
     */
    List<NotificationTemplate> findByNameContaining(String name);
}