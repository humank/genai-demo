package solid.humank.genaidemo.domain.voucher.model.valueobject;

/** 優惠券類型枚舉 */
public enum VoucherType {
    MEAL_COMBO("超值餐組合"),
    BEVERAGE_COMBO("飲品組合"),
    COFFEE_COMBO("咖啡組合"),
    SNACK_COMBO("零食組合"),
    BREAKFAST_COMBO("早餐組合"),
    LUNCH_COMBO("午餐組合"),
    GENERAL_VOUCHER("通用優惠券");

    private final String description;

    VoucherType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCombo() {
        return this != GENERAL_VOUCHER;
    }
}
