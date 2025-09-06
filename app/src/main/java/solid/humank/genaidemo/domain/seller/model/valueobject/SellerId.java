package solid.humank.genaidemo.domain.seller.model.valueobject;

import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 賣家ID值物件 - 使用 Record 實作 */
@ValueObject
public record SellerId(String id) {

    /**
     * 緊湊建構子 - 驗證參數
     */
    public SellerId {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("賣家ID不能為空");
        }
    }

    /**
     * 生成新的賣家ID
     *
     * @return 新的賣家ID
     */
    public static SellerId generateNew() {
        return new SellerId(UUID.randomUUID().toString());
    }

    /**
     * 從字符串創建賣家ID
     *
     * @param id 字符串ID
     * @return 賣家ID
     */
    public static SellerId of(String id) {
        return new SellerId(id);
    }

    /**
     * 獲取ID（向後相容方法）
     *
     * @return ID
     */
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }
}
