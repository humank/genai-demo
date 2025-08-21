package solid.humank.genaidemo.domain.review.model.valueobject;

import java.util.Objects;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 審核記錄ID值對象
 */
@ValueObject(name = "ModerationRecordId", description = "審核記錄ID")
public record ModerationRecordId(UUID value) {

    /**
     * 緊湊建構子 - 驗證參數
     */
    public ModerationRecordId {
        Objects.requireNonNull(value, "ModerationRecord ID cannot be null");
    }

    /**
     * 生成新的審核記錄ID
     */
    public static ModerationRecordId generate() {
        return new ModerationRecordId(UUID.randomUUID());
    }

    /**
     * 從UUID創建審核記錄ID
     */
    public static ModerationRecordId of(UUID uuid) {
        return new ModerationRecordId(uuid);
    }

    /**
     * 從字符串創建審核記錄ID
     */
    public static ModerationRecordId of(String id) {
        return new ModerationRecordId(UUID.fromString(id));
    }

    /**
     * 獲取ID值（向後相容方法）
     */
    public UUID getId() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}