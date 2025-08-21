package solid.humank.genaidemo.domain.notification.model.valueobject;

import java.util.Objects;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 通知ID值對象 - 使用 Record 實作 */
@ValueObject
public record NotificationId(UUID id) {

    /**
     * 緊湊建構子 - 驗證參數
     */
    public NotificationId {
        Objects.requireNonNull(id, "ID cannot be null");
    }

    /**
     * 生成新的通知ID
     *
     * @return 新的通知ID
     */
    public static NotificationId generate() {
        return new NotificationId(UUID.randomUUID());
    }

    /**
     * 從UUID創建通知ID
     *
     * @param id UUID
     * @return 通知ID
     */
    public static NotificationId fromUUID(UUID id) {
        return new NotificationId(id);
    }

    /**
     * 從字符串創建通知ID
     *
     * @param id 字符串ID
     * @return 通知ID
     */
    public static NotificationId fromString(String id) {
        return new NotificationId(UUID.fromString(id));
    }

    /**
     * 從字符串創建通知ID
     *
     * @param id 字符串ID
     * @return 通知ID
     */
    public static NotificationId of(String id) {
        return new NotificationId(UUID.fromString(id));
    }

    /**
     * 獲取UUID（向後相容方法）
     *
     * @return UUID
     */
    public UUID getId() {
        return id;
    }

    /**
     * 獲取ID值
     *
     * @return ID值
     */
    public String getValue() {
        return id.toString();
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
