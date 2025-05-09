package solid.humank.genaidemo.interfaces.web.order.dto;

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