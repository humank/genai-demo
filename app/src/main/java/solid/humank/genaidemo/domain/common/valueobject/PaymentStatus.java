package solid.humank.genaidemo.domain.common.valueobject;

/**
 * 支付狀態枚舉
 */
public enum PaymentStatus {
    PENDING("待支付"),
    PROCESSING("處理中"),
    COMPLETED("已完成"),
    FAILED("失敗"),
    REFUNDED("已退款"),
    CANCELLED("已取消");
    
    private final String description;
    
    PaymentStatus(String description) {
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
    public boolean canTransitionTo(PaymentStatus targetStatus) {
        switch (this) {
            case PENDING:
                return targetStatus == PROCESSING || targetStatus == CANCELLED;
            case PROCESSING:
                return targetStatus == COMPLETED || targetStatus == FAILED;
            case COMPLETED:
                return targetStatus == REFUNDED;
            case FAILED:
                return targetStatus == PENDING;
            case REFUNDED:
                return false; // 終態
            case CANCELLED:
                return false; // 終態
            default:
                return false;
        }
    }
}