package solid.humank.genaidemo.domain.payment.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 支付方式值對象 */
@ValueObject
public enum PaymentMethod {
    CREDIT_CARD("信用卡"),
    BANK_TRANSFER("銀行轉賬"),
    DIGITAL_WALLET("數位錢包"),
    CASH_ON_DELIVERY("貨到付款");

    private final String description;

    PaymentMethod(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
