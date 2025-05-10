package solid.humank.genaidemo.infrastructure.order.acl;

import java.time.LocalDateTime;
import java.util.Map;

import solid.humank.genaidemo.domain.common.delivery.DeliveryStatus;
import solid.humank.genaidemo.domain.common.valueobject.DeliveryOrder;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.order.model.valueobject.OrderId;

/**
 * 物流防腐層
 * 隔離外部物流系統與領域模型
 */
public class LogisticsAntiCorruptionLayer {
    
    private final ExternalLogisticsSystem externalLogisticsSystem;
    
    public LogisticsAntiCorruptionLayer(ExternalLogisticsSystem externalLogisticsSystem) {
        this.externalLogisticsSystem = externalLogisticsSystem;
    }
    
    /**
     * 創建物流訂單
     * 
     * @param order 訂單
     * @return 物流訂單
     */
    public DeliveryOrder createDeliveryOrder(Order order) {
        // 將領域模型轉換為外部系統所需的格式
        Map<String, String> externalRequest = Map.of(
            "orderId", order.getId().toString(),
            "customerId", order.getCustomerId(),
            "address", order.getShippingAddress(),
            "items", String.valueOf(order.getItems().size()),
            "totalAmount", order.getTotalAmount().getAmount().toString()
        );
        
        // 調用外部系統
        String trackingNumber = externalLogisticsSystem.createDelivery(externalRequest);
        
        // 將外部系統的響應轉換為領域模型
        return new DeliveryOrder(
            order.getId(),
            DeliveryStatus.PENDING,
            trackingNumber,
            LocalDateTime.now().plusDays(3)
        );
    }
    
    /**
     * 獲取物流狀態
     * 
     * @param orderId 訂單ID
     * @return 物流狀態
     */
    public DeliveryStatus getDeliveryStatus(OrderId orderId) {
        // 調用外部系統
        String externalStatus = externalLogisticsSystem.getDeliveryStatus(orderId.toString());
        
        // 將外部系統的響應轉換為領域模型
        return mapExternalStatus(externalStatus);
    }
    
    /**
     * 將外部系統的狀態映射為領域模型的狀態
     * 
     * @param externalStatus 外部系統的狀態
     * @return 領域模型的狀態
     */
    private DeliveryStatus mapExternalStatus(String externalStatus) {
        return switch (externalStatus.toUpperCase()) {
            case "CREATED", "PENDING" -> DeliveryStatus.PENDING;
            case "SHIPPED" -> DeliveryStatus.SHIPPED;
            case "DELIVERED" -> DeliveryStatus.DELIVERED;
            case "FAILED" -> DeliveryStatus.FAILED;
            case "CANCELLED" -> DeliveryStatus.CANCELLED;
            default -> DeliveryStatus.UNKNOWN;
        };
    }
}