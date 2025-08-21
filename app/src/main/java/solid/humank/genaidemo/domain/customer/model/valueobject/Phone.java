package solid.humank.genaidemo.domain.customer.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 電話號碼值對象 - 使用 Record 實作 */
@ValueObject
public record Phone(String phone) {

    /**
     * 緊湊建構子 - 驗證參數
     */
    public Phone {
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty");
        }
        phone = phone.trim(); // 正規化
    }

    /**
     * 獲取電話號碼（向後相容方法）
     *
     * @return 電話號碼
     */
    public String getPhone() {
        return phone;
    }

    @Override
    public String toString() {
        return phone;
    }
}
