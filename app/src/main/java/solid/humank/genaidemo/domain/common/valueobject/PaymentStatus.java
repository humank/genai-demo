package solid.humank.genaidemo.domain.common.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 支付狀態值對象
 * 
 * 表示支付在生命週期中的不同狀態。
 * 作為值對象，它是不可變的，所有屬性在創建後不能被修改。
 */
@ValueObject
public enum PaymentStatus {
    PENDING("處理中"),
    COMPLETED("已完成"),
    FAILED("失敗"),
    REFUNDED("已退款");
    
    private final String description;
    
    /**
     * 建立支付狀態
     * 
     * @param description 狀態描述
     */
    PaymentStatus(String description) {
        this.description = description;
    }
    
    /**
     * 獲取狀態描述
     * 
     * @return 狀態描述
     */
    public String getDescription() {
        return description;
    }
}