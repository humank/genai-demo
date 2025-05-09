package solid.humank.genaidemo.infrastructure.order.external;

import org.springframework.stereotype.Component;
import solid.humank.genaidemo.application.order.port.outgoing.PaymentServicePort;
import solid.humank.genaidemo.domain.order.model.valueobject.Money;
import solid.humank.genaidemo.domain.order.model.valueobject.OrderId;
import solid.humank.genaidemo.domain.order.model.valueobject.PaymentResult;

/**
 * 訂單支付服務適配器
 * 實現應用層的 PaymentServicePort 接口
 * 使用 ExternalPaymentAdapter 進行實際的支付處理
 */
@Component
public class OrderPaymentServiceAdapter implements PaymentServicePort {

    private final ExternalPaymentAdapter externalPaymentAdapter;

    public OrderPaymentServiceAdapter() {
        this.externalPaymentAdapter = new ExternalPaymentAdapter();
    }

    @Override
    public PaymentResult processPayment(OrderId orderId, Money amount) {
        return externalPaymentAdapter.processPayment(orderId, amount);
    }

    @Override
    public PaymentResult cancelPayment(OrderId orderId) {
        return externalPaymentAdapter.cancelPayment(orderId);
    }

    @Override
    public PaymentResult getPaymentStatus(OrderId orderId) {
        return externalPaymentAdapter.getPaymentStatus(orderId);
    }

    @Override
    public PaymentResult processRefund(OrderId orderId, Money amount) {
        return externalPaymentAdapter.processRefund(orderId, amount);
    }
}