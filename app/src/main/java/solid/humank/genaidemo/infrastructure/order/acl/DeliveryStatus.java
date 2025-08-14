package solid.humank.genaidemo.infrastructure.order.acl;

/** 配送狀態 用於表示物流訂單的當前狀態 */
public enum DeliveryStatus {
    CREATED, // 配送訂單已創建
    IN_TRANSIT, // 運送中
    DELIVERED, // 已送達
    FAILED, // 配送失敗
    UNKNOWN // 未知狀態
}
