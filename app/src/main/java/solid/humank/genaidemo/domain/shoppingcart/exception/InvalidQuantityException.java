package solid.humank.genaidemo.domain.shoppingcart.exception;

import solid.humank.genaidemo.domain.common.exception.DomainException;

/** 無效數量異常 當購物車商品數量無效時拋出 */
public class InvalidQuantityException extends DomainException {

    public InvalidQuantityException(String message) {
        super("INVALID_QUANTITY", message);
    }

    public InvalidQuantityException(String message, Throwable cause) {
        super("INVALID_QUANTITY", message, cause);
    }
}
