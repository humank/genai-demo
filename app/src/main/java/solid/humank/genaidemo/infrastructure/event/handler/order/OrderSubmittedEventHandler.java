package solid.humank.genaidemo.infrastructure.event.handler.order;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.application.customer.service.CustomerApplicationService;
import solid.humank.genaidemo.domain.order.model.events.OrderSubmittedEvent;
import solid.humank.genaidemo.infrastructure.event.handler.AbstractDomainEventHandler;

/**
 * 訂單提交事件處理器
 * 處理訂單提交事件，更新客戶的消費記錄
 * 
 * 需求 6.2: 建立事件處理器接收和處理事件的機制
 * 需求 6.5: 建立跨聚合根事件通信的業務邏輯執行
 */
@Component
public class OrderSubmittedEventHandler extends AbstractDomainEventHandler<OrderSubmittedEvent> {

    private final CustomerApplicationService customerApplicationService;

    public OrderSubmittedEventHandler(CustomerApplicationService customerApplicationService) {
        this.customerApplicationService = customerApplicationService;
    }

    @Override
    public void handle(OrderSubmittedEvent event) {
        // 委託給應用服務處理跨聚合根業務邏輯
        customerApplicationService.updateCustomerSpending(
                event.customerId(),
                event.totalAmount(),
                event.orderId().getValue(),
                "ORDER_PURCHASE");

        // 根據訂單金額給予基礎紅利點數
        int rewardPoints = calculateRewardPoints(event.totalAmount().getAmount().intValue());
        if (rewardPoints > 0) {
            customerApplicationService.addRewardPoints(
                    event.customerId(),
                    rewardPoints,
                    "Reward points for order: " + event.orderId().getValue());
        }
    }

    @Override
    public Class<OrderSubmittedEvent> getSupportedEventType() {
        return OrderSubmittedEvent.class;
    }

    /**
     * 計算基礎紅利點數
     * 每消費 100 元給 1 點
     */
    private int calculateRewardPoints(int amount) {
        return amount / 100;
    }
}