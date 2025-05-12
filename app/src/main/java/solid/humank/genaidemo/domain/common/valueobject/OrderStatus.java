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
        switch (this) {
            case CREATED:
                return targetStatus == SUBMITTED || targetStatus == PENDING || targetStatus == CANCELLED;
            case SUBMITTED:
                return targetStatus == PENDING || targetStatus == PAID || targetStatus == REJECTED || targetStatus == CANCELLED;
            case PENDING:
                return targetStatus == CONFIRMED || targetStatus == REJECTED || targetStatus == CANCELLED;
            case CONFIRMED:
                return targetStatus == PAID || targetStatus == CANCELLED;
            case PAID:
                return targetStatus == PROCESSING || targetStatus == CANCELLED;
            case PROCESSING:
                return targetStatus == SHIPPING || targetStatus == SHIPPED || targetStatus == CANCELLED;
            case SHIPPING:
                return targetStatus == SHIPPED || targetStatus == DELIVERED;
            case SHIPPED:
                return targetStatus == DELIVERED;
            case DELIVERED:
                return targetStatus == COMPLETED;
            case COMPLETED:
                return false; // 終態
            case CANCELLED:
                return false; // 終態
            case REJECTED:
                return false; // 終態
            case PAYMENT_FAILED:
                return targetStatus == SUBMITTED || targetStatus == PENDING || targetStatus == CANCELLED;
            default:
                return false;
        }
    }
}