package solid.humank.genaidemo.domain.payment.model.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import solid.humank.genaidemo.domain.common.annotations.DomainService;
import solid.humank.genaidemo.domain.common.events.DomainEvent;
import solid.humank.genaidemo.domain.common.events.DomainEventBus;
import solid.humank.genaidemo.domain.order.model.valueobject.Money;
import solid.humank.genaidemo.domain.payment.model.aggregate.Payment;
import solid.humank.genaidemo.domain.payment.events.PaymentCompletedEvent;
import solid.humank.genaidemo.domain.payment.events.PaymentFailedEvent;
import solid.humank.genaidemo.domain.payment.events.PaymentRequestedEvent;

/**
 * 支付服務
 * 處理支付相關的業務邏輯
 */
@Service
@DomainService(description = "處理支付相關的領域邏輯")
public class PaymentService {
    private final DomainEventBus eventBus;
    
    // 模擬支付存儲
    private final Map<UUID, Payment> payments = new HashMap<>();
    
    /**
     * 建立支付服務
     */
    public PaymentService(DomainEventBus eventBus) {
        this.eventBus = eventBus;
        
        // 訂閱支付請求事件
        eventBus.subscribe(PaymentRequestedEvent.class, this::handlePaymentRequestedEvent);
    }
    
    /**
     * 處理支付請求事件
     */
    private void handlePaymentRequestedEvent(DomainEvent event) {
        if (event instanceof PaymentRequestedEvent) {
            PaymentRequestedEvent paymentEvent = (PaymentRequestedEvent) event;
            UUID orderId = UUID.fromString(paymentEvent.getOrderId());
            requestPayment(orderId, paymentEvent.getAmount());
        }
    }
    
    /**
     * 請求支付
     */
    public void requestPayment(UUID orderId, Money amount) {
        // 創建支付
        Payment payment = new Payment(orderId, amount);
        
        // 存儲支付
        payments.put(payment.getId(), payment);
        
        // 模擬處理支付
        processPayment(payment);
    }
    
    /**
     * 處理支付
     */
    private void processPayment(Payment payment) {
        // 模擬支付處理
        try {
            // 模擬外部支付系統調用
            String transactionId = "TXN-" + System.currentTimeMillis();
            
            // 完成支付
            payment.complete(transactionId);
            
            // 發布支付完成事件
            eventBus.publish(new PaymentCompletedEvent(
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount(),
                transactionId
            ));
        } catch (Exception e) {
            // 支付失敗
            payment.fail(e.getMessage());
            
            // 發布支付失敗事件
            eventBus.publish(new PaymentFailedEvent(
                payment.getId(),
                payment.getOrderId(),
                e.getMessage()
            ));
        }
    }
    
    /**
     * 獲取支付
     */
    public Optional<Payment> getPayment(UUID paymentId) {
        return Optional.ofNullable(payments.get(paymentId));
    }
    
    /**
     * 獲取訂單的支付
     */
    public Optional<Payment> getPaymentByOrderId(UUID orderId) {
        return payments.values().stream()
                .filter(payment -> payment.getOrderId().equals(orderId))
                .findFirst();
    }
    
    /**
     * 退款
     */
    public void refundPayment(UUID paymentId) {
        // 獲取支付
        Payment payment = payments.get(paymentId);
        if (payment == null) {
            throw new IllegalArgumentException("Payment not found: " + paymentId);
        }
        
        // 退款
        payment.refund();
    }
}