package solid.humank.genaidemo.domain.common.valueobject;

import java.util.Objects;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 支付ID值對象 封裝支付ID的業務規則和行為 */
@ValueObject
public class PaymentId {
    private final UUID id;

    private PaymentId(UUID id) {
        this.id = Objects.requireNonNull(id, "支付ID不能為空");
    }

    /**
     * 創建新的支付ID
     *
     * @return 新的支付ID值對象
     */
    public static PaymentId create() {
        return new PaymentId(UUID.randomUUID());
    }

    /**
     * 從UUID創建支付ID
     *
     * @param id UUID
     * @return 支付ID值對象
     */
    public static PaymentId fromUUID(UUID id) {
        return new PaymentId(id);
    }

    /**
     * 從字符串創建支付ID
     *
     * @param id 字符串形式的UUID
     * @return 支付ID值對象
     */
    public static PaymentId fromString(String id) {
        return new PaymentId(UUID.fromString(id));
    }

    /**
     * 從字符串創建支付ID (工廠方法別名)
     *
     * @param id 字符串形式的UUID
     * @return 支付ID值對象
     */
    public static PaymentId of(String id) {
        return fromString(id);
    }

    /**
     * 生成新的支付ID (別名方法)
     *
     * @return 新的支付ID值對象
     */
    public static PaymentId generate() {
        return create();
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
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PaymentId paymentId = (PaymentId) o;
        return Objects.equals(id, paymentId.id);
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
