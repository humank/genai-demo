package solid.humank.genaidemo.domain.pricing.model.valueobject;

import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 價格ID值對象 - 使用 Record 實作 */
@ValueObject
public record PriceId(String id) {

    /**
     * 緊湊建構子 - 驗證參數
     */
    public PriceId {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("價格ID不能為空");
        }
    }

    /**
     * 生成新的價格ID
     *
     * @return 新的價格ID
     */
    public static PriceId generate() {
        return new PriceId(UUID.randomUUID().toString());
    }

    /**
     * 從字符串創建價格ID
     *
     * @param id 字符串ID
     * @return 價格ID
     */
    public static PriceId of(String id) {
        return new PriceId(id);
    }

    /**
     * 獲取ID（向後相容方法）
     *
     * @return ID
     */
    public String getId() {
        return id;
    }

    /**
     * 獲取ID值
     *
     * @return ID值
     */
    public String getValue() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }
}
