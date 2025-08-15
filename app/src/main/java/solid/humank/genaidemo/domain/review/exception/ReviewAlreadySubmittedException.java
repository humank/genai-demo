package solid.humank.genaidemo.domain.review.exception;

import solid.humank.genaidemo.domain.common.exception.DomainException;

/** 評價已提交異常 */
public class ReviewAlreadySubmittedException extends DomainException {

    public ReviewAlreadySubmittedException(String message) {
        super("REVIEW_ALREADY_SUBMITTED", message);
    }
}
