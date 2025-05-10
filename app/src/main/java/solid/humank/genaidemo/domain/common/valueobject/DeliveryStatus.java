package solid.humank.genaidemo.domain.common.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 物流狀態值對象
 * 表示物流訂單的不同狀態
 */
@ValueObject
public enum DeliveryStatus {
    PENDING("待處理"),
    SHIPPED("已發貨"),
    DELIVERED("已送達"),
    FAILED("失敗"),
    CANCELLED("已取消"),
    UNKNOWN("未知");
    
    private final String displayName;
    
    DeliveryStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}