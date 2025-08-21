package solid.humank.genaidemo.domain.shoppingcart.model.valueobject;

/** 購物車狀態枚舉 */
public enum ShoppingCartStatus {
    ACTIVE("活躍"),
    ABANDONED("已放棄"),
    CHECKED_OUT("已結帳"),
    EXPIRED("已過期");

    private final String description;

    ShoppingCartStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean canModify() {
        return this == ACTIVE;
    }

    public boolean canCheckout() {
        return this == ACTIVE;
    }
}
