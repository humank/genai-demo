package solid.humank.genaidemo.domain.review.model.valueobject;

import java.util.Objects;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 評價回覆ID值對象
 */
@ValueObject(name = "ReviewResponseId", description = "評價回覆ID")
public record ReviewResponseId(UUID value) {

    /**
     * 緊湊建構子 - 驗證參數
     */
    public ReviewResponseId {
        Objects.requireNonNull(value, "ReviewResponse ID cannot be null");
    }

    /**
     * 生成新的評價回覆ID
     */
    public static ReviewResponseId generate() {
        return new ReviewResponseId(UUID.randomUUID());
    }

    /**
     * 從UUID創建評價回覆ID
     */
    public static ReviewResponseId of(UUID uuid) {
        return new ReviewResponseId(uuid);
    }

    /**
     * 從字符串創建評價回覆ID
     */
    public static ReviewResponseId of(String id) {
        return new ReviewResponseId(UUID.fromString(id));
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