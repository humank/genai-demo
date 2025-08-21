package solid.humank.genaidemo.application.order.dto;

/** 創建訂單命令 */
public class CreateOrderCommand {
    private final String customerId;
    private final String shippingAddress;

    public CreateOrderCommand(String customerId, String shippingAddress) {
        this.customerId = customerId;
        this.shippingAddress = shippingAddress;
    }

    /** 創建訂單命令 */
    public static CreateOrderCommand of(String customerId, String shippingAddress) {
        return new CreateOrderCommand(customerId, shippingAddress);
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }
}
