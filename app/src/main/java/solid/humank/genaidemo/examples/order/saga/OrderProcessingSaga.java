package solid.humank.genaidemo.examples.order.saga;

import solid.humank.genaidemo.ddd.saga.SagaDefinition;
import solid.humank.genaidemo.examples.order.Order;
import solid.humank.genaidemo.examples.order.acl.LogisticsAntiCorruptionLayer;
import solid.humank.genaidemo.examples.order.service.PaymentResult;
import solid.humank.genaidemo.examples.order.service.PaymentService;

/**
 * 訂單處理 Saga
 * 協調訂單創建、付款和物流的完整流程
 */
public class OrderProcessingSaga {
    private final LogisticsAntiCorruptionLayer logisticsAcl;
    private final PaymentService paymentService;

    public OrderProcessingSaga(
        LogisticsAntiCorruptionLayer logisticsAcl,
        PaymentService paymentService
    ) {
        this.logisticsAcl = logisticsAcl;
        this.paymentService = paymentService;
    }

    public void process(OrderSagaContext context) {
        new SagaDefinition<>(context)
            // 1. 驗證訂單
            .step(
                "驗證訂單",
                this::validateOrder,
                null  // 驗證步驟不需要補償
            )
            // 2. 處理付款
            .step(
                "處理付款",
                this::processPayment,
                this::refundPayment
            )
            // 3. 建立物流訂單
            .step(
                "建立物流訂單",
                this::createDeliveryOrder,
                this::cancelDeliveryOrder
            )
            .execute();
    }

    private void validateOrder(OrderSagaContext context) {
        Order order = context.getOrder();
        if (order.getItems().isEmpty()) {
            throw new IllegalStateException("訂單必須包含至少一個商品");
        }
    }

    private void processPayment(OrderSagaContext context) {
        Order order = context.getOrder();
        PaymentResult result = paymentService.processPayment(
            order.getOrderId().toString(),
            order.getTotalAmount()
        );
        
        if (!result.success()) {
            throw new IllegalStateException("付款失敗: " + result.message());
        }
        
        context.setPaymentId(result.paymentId());
    }

    private void refundPayment(OrderSagaContext context) {
        if (context.getPaymentId() != null) {
            paymentService.refundPayment(context.getPaymentId());
        }
    }

    private void createDeliveryOrder(OrderSagaContext context) {
        var deliveryOrder = logisticsAcl.createDeliveryOrder(context.getOrder());
        context.setDeliveryId(deliveryOrder.orderId().toString());
    }

    private void cancelDeliveryOrder(OrderSagaContext context) {
        if (context.getDeliveryId() != null) {
            // 通過 ACL 取消物流訂單
            // 實際實現中需要處理取消失敗的情況
        }
    }
}
