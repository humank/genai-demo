package solid.humank.genaidemo.domain.review.model.valueobject;

import java.util.Objects;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 評價圖片ID值對象
 */
@ValueObject(name = "ReviewImageId", description = "評價圖片ID")
public record ReviewImageId(UUID value) {

    /**
     * 緊湊建構子 - 驗證參數
     */
    public ReviewImageId {
        Objects.requireNonNull(value, "ReviewImage ID cannot be null");
    }

    /**
     * 生成新的評價圖片ID
     */
    public static ReviewImageId generate() {
        return new ReviewImageId(UUID.randomUUID());
    }

    /**
     * 從UUID創建評價圖片ID
     */
    public static ReviewImageId of(UUID uuid) {
        return new ReviewImageId(uuid);
    }

    /**
     * 從字符串創建評價圖片ID
     */
    public static ReviewImageId of(String id) {
        return new ReviewImageId(UUID.fromString(id));
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