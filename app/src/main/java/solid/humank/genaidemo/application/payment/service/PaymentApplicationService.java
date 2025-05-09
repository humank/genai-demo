package solid.humank.genaidemo.application.payment.service;

import org.springframework.stereotype.Service;
import solid.humank.genaidemo.domain.common.events.DomainEventBus;
import solid.humank.genaidemo.domain.order.model.valueobject.Money;
import solid.humank.genaidemo.application.payment.port.incoming.PaymentManagementUseCase;
import solid.humank.genaidemo.application.payment.port.outgoing.PaymentPersistencePort;
import solid.humank.genaidemo.domain.payment.model.aggregate.Payment;
import solid.humank.genaidemo.domain.payment.events.PaymentCompletedEvent;
import solid.humank.genaidemo.domain.payment.events.PaymentFailedEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 支付應用服務
 * 協調領域服務和持久化操作
 */
@Service
public class PaymentApplicationService implements PaymentManagementUseCase {
    
    private final PaymentPersistencePort paymentPersistencePort;
    private final DomainEventBus eventBus;
    
    public PaymentApplicationService(
            PaymentPersistencePort paymentPersistencePort,
            DomainEventBus eventBus) {
        this.paymentPersistencePort = paymentPersistencePort;
        this.eventBus = eventBus;
    }
    
    /**
     * 處理支付
     */
    @Override
    public Payment processPayment(UUID orderId, Money amount) {
        // 創建支付
        Payment payment = new Payment(orderId, amount);
        
        // 保存支付
        paymentPersistencePort.save(payment);
        
        try {
            // 模擬外部支付系統調用
            String transactionId = "TXN-" + System.currentTimeMillis();
            
            // 完成支付
            payment.complete(transactionId);
            
            // 更新支付
            paymentPersistencePort.update(payment);
            
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
            
            // 更新支付
            paymentPersistencePort.update(payment);
            
            // 發布支付失敗事件
            eventBus.publish(new PaymentFailedEvent(
                payment.getId(),
                payment.getOrderId(),
                e.getMessage()
            ));
        }
        
        return payment;
    }
    
    /**
     * 獲取支付
     */
    @Override
    public Optional<Payment> getPayment(UUID paymentId) {
        return paymentPersistencePort.findById(paymentId);
    }
    
    /**
     * 獲取訂單的支付
     */
    @Override
    public Optional<Payment> getPaymentByOrderId(UUID orderId) {
        return paymentPersistencePort.findByOrderId(orderId);
    }
    
    /**
     * 獲取所有支付
     */
    @Override
    public List<Payment> getAllPayments() {
        return paymentPersistencePort.findAll();
    }
    
    /**
     * 退款
     */
    @Override
    public void refundPayment(UUID paymentId) {
        // 獲取支付
        Payment payment = paymentPersistencePort.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        
        // 退款
        payment.refund();
        
        // 更新支付
        paymentPersistencePort.update(payment);
    }
}