package solid.humank.genaidemo.domain.promotion.model.valueobject;

/** 促銷類型枚舉 */
public enum PromotionType {
    PERCENTAGE_DISCOUNT("百分比折扣"),
    FIXED_AMOUNT_DISCOUNT("固定金額折扣"),
    BUY_ONE_GET_ONE("買一送一"),
    FLASH_SALE("閃購特價"),
    GIFT_WITH_PURCHASE("滿額贈禮"),
    ADD_ON_PURCHASE("加購優惠"),
    BUNDLE_PRICING("組合優惠"),
    MEMBER_EXCLUSIVE("會員專屬"),
    TIME_LIMITED("限時優惠"),
    LIMITED_QUANTITY("限量優惠");

    private final String description;

    PromotionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
