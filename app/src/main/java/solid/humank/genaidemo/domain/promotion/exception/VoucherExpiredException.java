package solid.humank.genaidemo.domain.promotion.exception;

import solid.humank.genaidemo.domain.common.exception.DomainException;

/** 優惠券已過期異常 */
public class VoucherExpiredException extends DomainException {

    public VoucherExpiredException(String message) {
        super("VOUCHER_EXPIRED", message);
    }
}