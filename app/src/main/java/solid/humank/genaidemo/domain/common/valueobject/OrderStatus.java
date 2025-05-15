package solid.humank.genaidemo.domain.common.valueobject;

/**
 * 訂單狀態枚舉
 */
public enum OrderStatus {
    CREATED("已創建"),
    SUBMITTED("已提交"),
    PENDING("待處理"),
    CONFIRMED("已確認"),
    PAID("已支付"),
    PROCESSING("處理中"),
    SHIPPING("配送中"),
    SHIPPED("已發貨"),
    DELIVERED("已送達"),
    COMPLETED("已完成"),
    CANCELLED("已取消"),
    REJECTED("已拒絕"),
    PAYMENT_FAILED("支付失敗");
    
    private final String description;
    
    OrderStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 檢查是否可以轉換到指定狀態
     * 
     * @param targetStatus 目標狀態
     * @return 是否可以轉換
     */
    public boolean canTransitionTo(OrderStatus targetStatus) {
        return switch (this) {
            case CREATED -> targetStatus == SUBMITTED || targetStatus == PENDING || targetStatus == CANCELLED;
            case SUBMITTED -> targetStatus == PENDING || targetStatus == PAID || targetStatus == REJECTED || targetStatus == CANCELLED;
            case PENDING -> targetStatus == CONFIRMED || targetStatus == REJECTED || targetStatus == CANCELLED;
            case CONFIRMED -> targetStatus == PAID || targetStatus == CANCELLED;
            case PAID -> targetStatus == PROCESSING || targetStatus == CANCELLED;
            case PROCESSING -> targetStatus == SHIPPING || targetStatus == SHIPPED || targetStatus == CANCELLED;
            case SHIPPING -> targetStatus == SHIPPED || targetStatus == DELIVERED;
            case SHIPPED -> targetStatus == DELIVERED;
            case DELIVERED -> targetStatus == COMPLETED;
            case COMPLETED, CANCELLED, REJECTED -> false; // 終態
            case PAYMENT_FAILED -> targetStatus == SUBMITTED || targetStatus == PENDING || targetStatus == CANCELLED;
        };
    }
}