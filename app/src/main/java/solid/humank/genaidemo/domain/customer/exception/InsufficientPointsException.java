package solid.humank.genaidemo.domain.customer.exception;

import solid.humank.genaidemo.domain.common.exception.DomainException;

/** 紅利點數不足異常 */
public class InsufficientPointsException extends DomainException {

    public InsufficientPointsException(String message) {
        super("INSUFFICIENT_POINTS", message);
    }

    public InsufficientPointsException() {
        super("INSUFFICIENT_POINTS", "消費者紅利點數不足");
    }
}
