package solid.humank.genaidemo.domain.review.exception;

import solid.humank.genaidemo.domain.common.exception.DomainException;

/** 評價修改不被允許異常 */
public class ReviewModificationNotAllowedException extends DomainException {

    public ReviewModificationNotAllowedException(String message) {
        super("REVIEW_MODIFICATION_NOT_ALLOWED", message);
    }
}
