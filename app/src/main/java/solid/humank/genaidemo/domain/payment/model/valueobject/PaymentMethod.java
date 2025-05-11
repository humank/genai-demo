package solid.humank.genaidemo.domain.payment.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 支付方式值對象
 * 
 * 表示支付的不同方式，如信用卡、銀行轉賬等。
 * 作為值對象，它是不可變的，所有屬性在創建後不能被修改。
 */
@ValueObject
public enum PaymentMethod {
    CREDIT_CARD("信用卡"),
    BANK_TRANSFER("銀行轉賬");
    
    private final String description;
    
    /**
     * 建立支付方式
     * 
     * @param description 方式描述
     */
    PaymentMethod(String description) {
        this.description = description;
    }
    
    /**
     * 獲取方式描述
     * 
     * @return 方式描述
     */
    public String getDescription() {
        return description;
    }
}