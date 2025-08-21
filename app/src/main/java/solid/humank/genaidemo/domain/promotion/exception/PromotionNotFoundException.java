package solid.humank.genaidemo.domain.promotion.exception;

import solid.humank.genaidemo.domain.common.exception.DomainException;

/** 促銷活動未找到異常 當嘗試操作不存在的促銷活動時拋出 */
public class PromotionNotFoundException extends DomainException {

    public PromotionNotFoundException(String message) {
        super("PROMOTION_NOT_FOUND", message);
    }

    public PromotionNotFoundException(String message, Throwable cause) {
        super("PROMOTION_NOT_FOUND", message, cause);
    }
}
