package solid.humank.genaidemo.domain.payment.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 支付狀態值對象
 */
@ValueObject
public enum PaymentStatus {
    PENDING("處理中"),
    COMPLETED("已完成"),
    FAILED("失敗"),
    REFUNDED("已退款");
    
    private final String description;
    
    PaymentStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}