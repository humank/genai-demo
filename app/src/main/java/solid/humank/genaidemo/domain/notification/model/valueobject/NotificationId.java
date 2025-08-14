package solid.humank.genaidemo.domain.notification.model.valueobject;

import java.util.Objects;
import java.util.UUID;
import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 通知ID值對象 */
@ValueObject
public class NotificationId {
    private final UUID id;

    private NotificationId(UUID id) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
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
     * 獲取UUID
     *
     * @return UUID
     */
    public UUID getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotificationId that = (NotificationId) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
