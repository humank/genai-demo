package solid.humank.genaidemo.domain.common.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 訂單狀態值對象
 * 
 * 表示訂單在生命週期中的不同狀態。
 * 提供了狀態轉換的規則，確保訂單狀態的變更符合業務邏輯。
 */
@ValueObject
public enum OrderStatus {
    CREATED("已建立"),
    PENDING("處理中"),
    CONFIRMED("已確認"),
    PAID("已付款"),
    SHIPPING("配送中"),
    DELIVERED("已送達"),
    CANCELLED("已取消");

    private final String description;

    /**
     * 建立訂單狀態
     * 
     * @param description 狀態描述
     */
    OrderStatus(String description) {
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

    /**
     * 檢查是否可以轉換到下一個狀態
     * 
     * @param nextStatus 下一個狀態
     * @return 是否可以轉換
     */
    public boolean canTransitionTo(OrderStatus nextStatus) {
        switch (this) {
            case CREATED:
                return nextStatus == PENDING || nextStatus == CANCELLED;
            case PENDING:
                return nextStatus == CONFIRMED || nextStatus == CANCELLED;
            case CONFIRMED:
                return nextStatus == PAID || nextStatus == CANCELLED;
            case PAID:
                return nextStatus == SHIPPING || nextStatus == CANCELLED;
            case SHIPPING:
                return nextStatus == DELIVERED || nextStatus == CANCELLED;
            case DELIVERED:
            case CANCELLED:
                return false;
            default:
                throw new IllegalStateException("Unexpected value: " + this);
        }
    }
}