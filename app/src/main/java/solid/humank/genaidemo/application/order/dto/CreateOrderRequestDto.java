package solid.humank.genaidemo.application.order.dto;

/** 創建訂單請求DTO 應用層使用的數據傳輸對象，避免直接依賴介面層 */
public class CreateOrderRequestDto {
    private String customerId;
    private String shippingAddress;

    public CreateOrderRequestDto() {}

    public CreateOrderRequestDto(String customerId, String shippingAddress) {
        this.customerId = customerId;
        this.shippingAddress = shippingAddress;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    /** 從通用參數創建應用層DTO */
    public static CreateOrderRequestDto from(String customerId, String shippingAddress) {
        return new CreateOrderRequestDto(customerId, shippingAddress);
    }
}
