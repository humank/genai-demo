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
     * 遵循 Tell, Don't Ask 原則，讓訂單自己負責大部分業務邏輯
     * 
     * @param order 要處理的訂單
     * @return 處理結果，包含可能的錯誤訊息和折扣後金額
     * @throws IllegalArgumentException 如果訂單為空
     */
    public OrderProcessingResult process(Order order) {
        // 前置條件檢查
        if (order == null) {
            throw new IllegalArgumentException("訂單不能為空");
        }
        
        try {
            // 1. 告訴訂單自己執行驗證和業務邏輯
            order.process();

            // 2. 應用折扣政策（使用了 Specification）
            Money finalAmount = discountPolicy.apply(order);

            // 3. 告訴訂單應用折扣
            order.applyDiscount(finalAmount);

            // 4. 觸發支付流程
            UUID paymentId = UUID.randomUUID();
            eventBus.publish(new PaymentRequestedEvent(paymentId, order.getId(), finalAmount));

            // 5. 回傳處理結果
            return OrderProcessingResult.success(finalAmount);
        } catch (ValidationException e) {
            return OrderProcessingResult.failure(e.getErrors());
        } catch (Exception e) {
            // 統一處理未預期的異常
            return OrderProcessingResult.failure(List.of("處理訂單時發生錯誤: " + e.getMessage()));
        }
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
