package solid.humank.genaidemo.infrastructure.order.external;

import org.springframework.stereotype.Component;
import solid.humank.genaidemo.application.order.port.outgoing.LogisticsServicePort;
import solid.humank.genaidemo.domain.common.valueobject.DeliveryOrder;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;

/** 物流服務適配器 實現應用層的 LogisticsServicePort 接口 使用 ExternalLogisticsAdapter 進行實際的物流處理 */
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
    public void updateDeliveryAddress(OrderId orderId, String address) {
        externalLogisticsAdapter.updateDeliveryAddress(orderId, address);
    }
}
