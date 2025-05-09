package solid.humank.genaidemo.examples.order.saga;

import solid.humank.genaidemo.examples.order.model.aggregate.Order;

/**
 * 訂單 Saga 上下文
 * 用於在 Saga 執行過程中傳遞狀態
 */
public class OrderSagaContext {
    private final Order order;
    private String paymentId;
    private String deliveryId;
    
    public OrderSagaContext(Order order) {
        this.order = order;
    }
    
    /**
     * 獲取訂單
     */
    public Order getOrder() {
        return order;
    }
    
    /**
     * 獲取支付ID
     */
    public String getPaymentId() {
        return paymentId;
    }
    
    /**
     * 設置支付ID
     */
    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }
    
    /**
     * 獲取配送ID
     */
    public String getDeliveryId() {
        return deliveryId;
    }
    
    /**
     * 設置配送ID
     */
    public void setDeliveryId(String deliveryId) {
        this.deliveryId = deliveryId;
    }
}