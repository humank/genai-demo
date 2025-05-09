package solid.humank.genaidemo.domain.common.delivery;

/**
 * 物流狀態枚舉
 * 領域層定義的物流狀態，避免依賴基礎設施層
 */
public enum DeliveryStatus {
    PENDING("待處理"),
    PROCESSING("處理中"),
    SHIPPED("已發貨"),
    IN_TRANSIT("運輸中"),
    DELIVERED("已送達"),
    FAILED("配送失敗"),
    RETURNED("已退回"),
    CANCELLED("已取消");

    private final String description;

    DeliveryStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}