package solid.humank.genaidemo.domain.notification.model.aggregate;

import java.time.LocalDateTime;
import java.util.Objects;

import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationChannel;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationType;

/** 通知模板聚合根 */
@AggregateRoot(name = "NotificationTemplate", description = "通知模板聚合根，管理通知模板的內容和格式", boundedContext = "Notification", version = "1.0")
public class NotificationTemplate extends solid.humank.genaidemo.domain.common.aggregate.AggregateRoot {

    private final String templateId;
    private final NotificationType type;
    private final NotificationChannel channel;
    private String name;
    private String subject;
    private String content;
    private String variables; // JSON format for template variables
    private boolean isActive;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    public NotificationTemplate(String templateId, NotificationType type, NotificationChannel channel,
            String name, String subject, String content, String createdBy) {
        this.templateId = Objects.requireNonNull(templateId, "模板ID不能為空");
        this.type = Objects.requireNonNull(type, "通知類型不能為空");
        this.channel = Objects.requireNonNull(channel, "通知渠道不能為空");
        this.name = Objects.requireNonNull(name, "模板名稱不能為空");
        this.subject = subject;
        this.content = Objects.requireNonNull(content, "模板內容不能為空");
        this.createdBy = Objects.requireNonNull(createdBy, "創建者不能為空");
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.variables = "{}"; // Default empty JSON
    }

    // Business methods
    public void updateContent(String subject, String content, String updatedBy) {
        this.subject = subject;
        this.content = Objects.requireNonNull(content, "模板內容不能為空");
        this.updatedBy = Objects.requireNonNull(updatedBy, "更新者不能為空");
        this.updatedAt = LocalDateTime.now();
    }

    public void updateVariables(String variables, String updatedBy) {
        this.variables = Objects.requireNonNull(variables, "模板變數不能為空");
        this.updatedBy = Objects.requireNonNull(updatedBy, "更新者不能為空");
        this.updatedAt = LocalDateTime.now();
    }

    public void activate(String updatedBy) {
        this.isActive = true;
        this.updatedBy = Objects.requireNonNull(updatedBy, "更新者不能為空");
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate(String updatedBy) {
        this.isActive = false;
        this.updatedBy = Objects.requireNonNull(updatedBy, "更新者不能為空");
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    public String getTemplateId() {
        return templateId;
    }

    public NotificationType getType() {
        return type;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public String getName() {
        return name;
    }

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }

    public String getVariables() {
        return variables;
    }

    public boolean isActive() {
        return isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        NotificationTemplate that = (NotificationTemplate) o;
        return Objects.equals(templateId, that.templateId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(templateId);
    }
}