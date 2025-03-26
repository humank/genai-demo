package solid.humank.genaidemo.examples.order.application.dto;

import solid.humank.genaidemo.examples.order.Money;

/**
 * 添加訂單項命令
 */
public class AddOrderItemCommand {
    private final String orderId;
    private final String productId;
    private final String productName;
    private final int quantity;
    private final Money price;

    public AddOrderItemCommand(String orderId, String productId, String productName, int quantity, Money price) {
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public Money getPrice() {
        return price;
    }
}
