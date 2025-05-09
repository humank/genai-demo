package solid.humank.genaidemo.examples.order.saga;

import org.springframework.stereotype.Component;
import solid.humank.genaidemo.examples.order.model.aggregate.Order;
import solid.humank.genaidemo.ddd.saga.SagaDefinition;
import solid.humank.genaidemo.examples.order.model.valueobject.PaymentResult;
import solid.humank.genaidemo.examples.order.model.service.PaymentService;

/**
 * 訂單處理 Saga
 * 協調訂單處理的各個步驟
 */
@Component
public class OrderProcessingSaga implements SagaDefinition<OrderSagaContext> {
    private final PaymentService paymentService;
    
    /**
     * 建立訂單處理 Saga
     */
    public OrderProcessingSaga(
        PaymentService paymentService
    ) {
        this.paymentService = paymentService;
    }
    
    @Override
    public void execute(OrderSagaContext context) {
        // 處理支付
        processPayment(context);
        
        // 處理物流
        processLogistics(context);
        
        // 完成訂單
        completeOrder(context);
    }
    
    private void processPayment(OrderSagaContext context) {
        Order order = context.getOrder();
        
        // 處理支付
        PaymentResult result = paymentService.processPayment(
            order.getId().toString(),
            order.getTotalAmount()
        );
        
        if (!result.success()) {
            throw new RuntimeException("Payment failed: " + result.message());
        }
        
        // 保存支付ID
        context.setPaymentId(result.paymentId());
    }
    
    private void processLogistics(OrderSagaContext context) {
        // 物流處理邏輯
        // 實際應用中會調用物流服務
        context.setDeliveryId("DEL-" + System.currentTimeMillis());
    }
    
    private void completeOrder(OrderSagaContext context) {
        // 完成訂單處理
        Order order = context.getOrder();
        order.markAsPaid();
    }
    
    @Override
    public void compensate(OrderSagaContext context, Exception exception) {
        // 補償邏輯
        if (context.getPaymentId() != null) {
            paymentService.refundPayment(context.getPaymentId());
        }
        
        // 取消訂單
        context.getOrder().cancel();
    }
}