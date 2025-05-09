package solid.humank.genaidemo.examples.order.acl;

import java.util.Map;

import solid.humank.genaidemo.examples.order.model.aggregate.Order;
import solid.humank.genaidemo.examples.order.model.valueobject.OrderId;

/**
 * 物流防腐層
 * 用於隔離外部物流系統的資料結構和協議，保護我們的領域模型
 */
public class LogisticsAntiCorruptionLayer {
    private final ExternalLogisticsSystem legacySystem;

    public LogisticsAntiCorruptionLayer(ExternalLogisticsSystem legacySystem) {
        this.legacySystem = legacySystem;
    }

    /**
     * 將訂單轉換為外部物流系統可以理解的格式並創建配送單
     */
    public DeliveryOrder createDeliveryOrder(Order order) {
        // 將我們的領域模型轉換為外部系統期望的格式
        Map<String, String> externalFormat = Map.of(
            "reference_no", order.getOrderId().toString(),
            "delivery_address", "從訂單中獲取地址", // 實際應用中會從Order中獲取
            "customer_contact", order.getCustomerId(),
            "items_count", String.valueOf(order.getItems().size()),
            "total_amount", order.getTotalAmount().toString()
        );

        // 調用外部系統並轉換回我們的模型
        String deliveryId = legacySystem.createDelivery(externalFormat);
        return new DeliveryOrder(
            OrderId.of(deliveryId),
            DeliveryStatus.CREATED
        );
    }

    /**
     * 查詢配送狀態並轉換為我們的領域模型能理解的格式
     */
    public DeliveryStatus getDeliveryStatus(OrderId orderId) {
        String externalStatus = legacySystem.getDeliveryStatus(orderId.toString());
        return mapExternalStatus(externalStatus);
    }

    private DeliveryStatus mapExternalStatus(String externalStatus) {
        return switch (externalStatus.toUpperCase()) {
            case "INIT", "PENDING" -> DeliveryStatus.CREATED;
            case "IN_TRANSIT" -> DeliveryStatus.IN_TRANSIT;
            case "DELIVERED" -> DeliveryStatus.DELIVERED;
            case "FAILED" -> DeliveryStatus.FAILED;
            default -> DeliveryStatus.UNKNOWN;
        };
    }
}