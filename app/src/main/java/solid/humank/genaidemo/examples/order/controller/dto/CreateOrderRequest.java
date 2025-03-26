package solid.humank.genaidemo.examples.order.controller.dto;

/**
 * 創建訂單請求 DTO
 */
public record CreateOrderRequest(
    String customerId,
    String shippingAddress
) {
    public String getCustomerId() {
        return customerId;
    }
    
    public String getShippingAddress() {
        return shippingAddress;
    }
}
