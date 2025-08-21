package solid.humank.genaidemo.infrastructure.event.handler.customer;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.application.customer.service.CustomerApplicationService;
import solid.humank.genaidemo.domain.customer.model.events.CustomerSpendingUpdatedEvent;
import solid.humank.genaidemo.infrastructure.event.handler.AbstractDomainEventHandler;

/**
 * 客戶消費事件處理器
 * 處理客戶消費更新事件，實現跨聚合根的業務邏輯
 * 
 * 需求 6.2: 建立事件處理器接收和處理事件的機制
 * 需求 6.5: 建立跨聚合根事件通信的業務邏輯執行
 */
@Component
public class CustomerSpendingEventHandler extends AbstractDomainEventHandler<CustomerSpendingUpdatedEvent> {

    private final CustomerApplicationService customerApplicationService;

    public CustomerSpendingEventHandler(CustomerApplicationService customerApplicationService) {
        this.customerApplicationService = customerApplicationService;
    }

    @Override
    public void handle(CustomerSpendingUpdatedEvent event) {
        // 委託給應用服務處理跨聚合根業務邏輯
        customerApplicationService.upgradeCustomerMembershipBasedOnSpending(
                event.getAggregateId(),
                event.totalSpending(),
                event.spendingAmount());
    }

    @Override
    public Class<CustomerSpendingUpdatedEvent> getSupportedEventType() {
        return CustomerSpendingUpdatedEvent.class;
    }
}