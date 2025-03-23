package solid.humank.genaidemo.examples.order.saga;

import solid.humank.genaidemo.examples.order.Order;

/**
 * 訂單 Saga 上下文
 * 包含整個訂單處理流程需要的所有資訊
 */
public class OrderSagaContext {
    private final Order order;
    private String paymentId;
    private String deliveryId;

    public OrderSagaContext(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("訂單不能為空");
        }
        this.order = order;
    }

    public Order getOrder() {
        return order;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(String deliveryId) {
        this.deliveryId = deliveryId;
    }

    /**
     * 檢查支付狀態
     */
    public boolean isPaymentProcessed() {
        return paymentId != null && !paymentId.isEmpty();
    }

    /**
     * 檢查物流狀態
     */
    public boolean isDeliveryCreated() {
        return deliveryId != null && !deliveryId.isEmpty();
    }
}
