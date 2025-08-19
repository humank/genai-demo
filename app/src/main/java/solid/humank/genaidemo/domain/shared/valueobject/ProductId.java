package solid.humank.genaidemo.domain.shared.valueobject;

import java.util.Objects;
import java.util.UUID;
import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 產品ID值對象 - Shared Kernel 中的統一產品標識符 */
@ValueObject(name = "ProductId", description = "產品唯一標識符")
public class ProductId {
    private final String id;

    /**
     * 從字符串創建產品ID
     *
     * @param id 字符串ID
     */
    public ProductId(String id) {
        this.id = Objects.requireNonNull(id, "Product ID cannot be null");
    }

    /**
     * 從UUID創建產品ID
     *
     * @param uuid UUID
     */
    public ProductId(UUID uuid) {
        this.id = Objects.requireNonNull(uuid, "Product UUID cannot be null").toString();
    }

    /**
     * 生成新的產品ID
     *
     * @return 新的產品ID
     */
    public static ProductId generate() {
        return new ProductId(UUID.randomUUID().toString());
    }

    /**
     * 從字符串創建產品ID
     *
     * @param id 字符串ID
     * @return 產品ID
     */
    public static ProductId of(String id) {
        return new ProductId(id);
    }

    /**
     * 從UUID創建產品ID
     *
     * @param uuid UUID
     * @return 產品ID
     */
    public static ProductId of(UUID uuid) {
        return new ProductId(uuid);
    }

    /**
     * 獲取ID字符串
     *
     * @return ID字符串
     */
    public String getId() {
        return id;
    }

    /**
     * 獲取ID值（別名）
     *
     * @return ID值
     */
    public String getValue() {
        return id;
    }

    /**
     * 嘗試轉換為UUID（如果格式正確）
     *
     * @return UUID，如果格式不正確則拋出異常
     */
    public UUID toUUID() {
        return UUID.fromString(id);
    }

    /**
     * 檢查是否為有效的UUID格式
     *
     * @return 如果是有效UUID格式返回true
     */
    public boolean isUUIDFormat() {
        try {
            UUID.fromString(id);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductId productId = (ProductId) o;
        return Objects.equals(id, productId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id;
    }
}
