package solid.humank.genaidemo.domain.review.model.valueobject;

import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 評價ID值對象 - 使用 Record 實作 */
@ValueObject
public record ReviewId(String id) {

    /**
     * 緊湊建構子 - 驗證參數
     */
    public ReviewId {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("評價ID不能為空");
        }
    }

    /**
     * 生成新的評價ID
     *
     * @return 新的評價ID
     */
    public static ReviewId generateNew() {
        return new ReviewId(UUID.randomUUID().toString());
    }

    /**
     * 從字符串創建評價ID
     *
     * @param id 字符串ID
     * @return 評價ID
     */
    public static ReviewId of(String id) {
        return new ReviewId(id);
    }

    /**
     * 獲取ID值
     *
     * @return ID值
     */
    public String value() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }
}
