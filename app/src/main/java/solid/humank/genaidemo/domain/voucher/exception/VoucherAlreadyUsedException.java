package solid.humank.genaidemo.domain.voucher.exception;

import solid.humank.genaidemo.domain.common.exception.DomainException;

/** 優惠券已使用異常 */
public class VoucherAlreadyUsedException extends DomainException {

    public VoucherAlreadyUsedException(String message) {
        super("VOUCHER_ALREADY_USED", message);
    }
}
