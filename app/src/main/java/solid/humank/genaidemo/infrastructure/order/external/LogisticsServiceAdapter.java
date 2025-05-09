package solid.humank.genaidemo.infrastructure.order.external;

import org.springframework.stereotype.Component;
import solid.humank.genaidemo.application.order.port.outgoing.LogisticsServicePort;
import solid.humank.genaidemo.domain.common.delivery.DeliveryOrder;
import solid.humank.genaidemo.domain.common.delivery.DeliveryStatus;
import solid.humank.genaidemo.domain.order.model.valueobject.OrderId;

/**
 * 物流服務適配器
 * 實現應用層的 LogisticsServicePort 接口
 * 使用 ExternalLogisticsAdapter 進行實際的物流處理
 */
@Component
public class LogisticsServiceAdapter implements LogisticsServicePort {

    private final ExternalLogisticsAdapter externalLogisticsAdapter;

    public LogisticsServiceAdapter() {
        this.externalLogisticsAdapter = new ExternalLogisticsAdapter();
    }

    @Override
    public DeliveryOrder createDeliveryOrder(OrderId orderId) {
        return externalLogisticsAdapter.createDeliveryOrder(orderId);
    }

    @Override
    public boolean cancelDeliveryOrder(OrderId orderId) {
        return externalLogisticsAdapter.cancelDeliveryOrder(orderId);
    }

    @Override
    public DeliveryStatus getDeliveryStatus(OrderId orderId) {
        return externalLogisticsAdapter.getDeliveryStatus(orderId);
    }

    @Override
    public boolean updateDeliveryAddress(OrderId orderId, String address) {
        return externalLogisticsAdapter.updateDeliveryAddress(orderId, address);
    }

    @Override
    public boolean confirmDelivery(OrderId orderId) {
        return externalLogisticsAdapter.confirmDelivery(orderId);
    }
}