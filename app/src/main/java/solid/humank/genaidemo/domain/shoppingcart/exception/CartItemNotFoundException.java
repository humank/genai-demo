package solid.humank.genaidemo.domain.shoppingcart.exception;

import solid.humank.genaidemo.domain.common.exception.DomainException;

/** 購物車項目未找到異常 當嘗試操作不存在的購物車項目時拋出 */
public class CartItemNotFoundException extends DomainException {

    public CartItemNotFoundException(String message) {
        super("CART_ITEM_NOT_FOUND", message);
    }

    public CartItemNotFoundException(String message, Throwable cause) {
        super("CART_ITEM_NOT_FOUND", message, cause);
    }
}
