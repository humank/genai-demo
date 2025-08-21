package solid.humank.genaidemo.domain.delivery.model.valueobject;

import java.util.Objects;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 配送ID值對象 - 使用 Record 實作 */
@ValueObject
public record DeliveryId(UUID id) {

    /**
     * 緊湊建構子 - 驗證參數
     */
    public DeliveryId {
        Objects.requireNonNull(id, "ID cannot be null");
    }

    /**
     * 生成新的配送ID
     *
     * @return 新的配送ID
     */
    public static DeliveryId generate() {
        return new DeliveryId(UUID.randomUUID());
    }

    /**
     * 從UUID創建配送ID
     *
     * @param id UUID
     * @return 配送ID
     */
    public static DeliveryId fromUUID(UUID id) {
        return new DeliveryId(id);
    }

    /**
     * 從字符串創建配送ID
     *
     * @param id 字符串ID
     * @return 配送ID
     */
    public static DeliveryId fromString(String id) {
        return new DeliveryId(UUID.fromString(id));
    }

    /**
     * 從字符串創建配送ID
     *
     * @param id 字符串ID
     * @return 配送ID
     */
    public static DeliveryId of(String id) {
        return new DeliveryId(UUID.fromString(id));
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
