package solid.humank.genaidemo.examples.payment.service;

import java.util.UUID;

import solid.humank.genaidemo.ddd.annotations.DomainService;
import solid.humank.genaidemo.ddd.events.DomainEvent;
import solid.humank.genaidemo.ddd.events.DomainEventBus;
import solid.humank.genaidemo.examples.order.Money;
import solid.humank.genaidemo.examples.payment.Payment;
import solid.humank.genaidemo.examples.payment.events.PaymentRequestedEvent;

@DomainService
public class PaymentService {
    private final DomainEventBus eventBus;

    public PaymentService(DomainEventBus eventBus) {
        this.eventBus = eventBus;
        // 訂閱支付請求事件
        this.eventBus.subscribe(PaymentRequestedEvent.class, this::handlePaymentRequest);
    }

    private void handlePaymentRequest(DomainEvent event) {
        if (!(event instanceof PaymentRequestedEvent paymentRequestedEvent)) {
            return;
        }

        // 建立新的支付紀錄
        Payment payment = new Payment(
            paymentRequestedEvent.getOrderId(),
            paymentRequestedEvent.getAmount()
        );

        // 在實際應用中，這裡會調用外部支付系統進行處理
        processPaymentWithExternalSystem(payment);
    }

    private void processPaymentWithExternalSystem(Payment payment) {
        // 模擬呼叫外部支付系統
        // 在實際應用中，這裡會整合真實的支付系統，如：信用卡、PayPal等
        
        // 模擬90%的支付成功率
        if (Math.random() < 0.9) {
            payment.markAsCompleted();
        } else {
            payment.markAsFailed("模擬支付失敗");
        }
    }

    // 提供給外部服務調用的方法
    public void requestPayment(UUID orderId, Money amount) {
        Payment payment = new Payment(orderId, amount);
        processPaymentWithExternalSystem(payment);
    }
}
