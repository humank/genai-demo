package solid.humank.genaidemo.examples.order.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import solid.humank.genaidemo.ddd.annotations.DomainService;
import solid.humank.genaidemo.ddd.events.DomainEventBus;
import solid.humank.genaidemo.examples.order.Money;
import solid.humank.genaidemo.examples.order.Order;
import solid.humank.genaidemo.examples.order.policy.OrderDiscountPolicy;
import solid.humank.genaidemo.examples.order.validation.OrderValidator;
import solid.humank.genaidemo.examples.payment.events.PaymentRequestedEvent;
import solid.humank.genaidemo.exceptions.ValidationException;

/**
 * 訂單處理服務
 * 協調各種領域規則和政策的應用
 */
@DomainService
public class OrderProcessingService {
    private final OrderValidator validator;
    private final OrderDiscountPolicy discountPolicy;
    private final DomainEventBus eventBus;

    public OrderProcessingService(DomainEventBus eventBus) {
        this.validator = new OrderValidator();
        this.discountPolicy = OrderDiscountPolicy.weekendDiscount();
        this.eventBus = eventBus;
    }

    /**
     * 處理訂單
     * 展示如何組合使用各種 DDD 戰術模式：
     * - Domain Invariants（通過 Validator）
     * - Business Rules（通過 Specification）
     * - Domain Policies
     * 
     * @param order 要處理的訂單
     * @return 處理結果，包含可能的錯誤訊息和折扣後金額
     */
    public OrderProcessingResult process(Order order) {
        // 1. 驗證領域不變條件
        try {
            validator.validate(order);
        } catch (ValidationException e) {
            return OrderProcessingResult.failure(e.getErrors());
        }

        // 2. 應用折扣政策（使用了 Specification）
        Money finalAmount = discountPolicy.apply(order);

        // 3. 訂單處理成功，觸發支付流程
        order.setFinalAmount(finalAmount);
        eventBus.publish(new PaymentRequestedEvent(UUID.randomUUID(), order.getId(), finalAmount));

        // 4. 回傳處理結果
        return OrderProcessingResult.success(finalAmount);
    }

    /**
     * 訂單處理結果
     * 使用 record 來確保不可變性
     */
    public record OrderProcessingResult(boolean success, Optional<Money> finalAmount, List<String> errors) {
        public static OrderProcessingResult success(Money finalAmount) {
            return new OrderProcessingResult(true, Optional.of(finalAmount), List.of());
        }

        public static OrderProcessingResult failure(List<String> errors) {
            return new OrderProcessingResult(false, Optional.empty(), errors);
        }
    }
}
