package solid.humank.genaidemo.domain.order.model.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import solid.humank.genaidemo.domain.common.annotations.DomainService;
import solid.humank.genaidemo.domain.common.events.DomainEventBus;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.order.model.policy.OrderDiscountPolicy;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.payment.events.PaymentRequestedEvent;
import solid.humank.genaidemo.exceptions.ValidationException;
import solid.humank.genaidemo.utils.Preconditions;

/**
 * 訂單處理服務
 * 協調各種領域規則和政策的應用
 */
@Service
@DomainService(description = "處理訂單相關的領域邏輯")
public class OrderProcessingService {
    private final OrderDiscountPolicy discountPolicy;
    private final DomainEventBus eventBus;

    /**
     * 建立訂單處理服務
     * 
     * @param discountPolicy 折扣政策
     * @param eventBus 領域事件總線
     */
    public OrderProcessingService(
            OrderDiscountPolicy discountPolicy,
            DomainEventBus eventBus) {
        Preconditions.requireNonNull(discountPolicy, "折扣政策不能為空");
        Preconditions.requireNonNull(eventBus, "事件總線不能為空");
        
        this.discountPolicy = discountPolicy;
        this.eventBus = eventBus;
    }

    /**
     * 處理訂單並應用相關業務規則
     * 
     * @param order 要處理的訂單
     * @return 處理結果，包含可能的錯誤訊息和最終金額
     * @throws IllegalArgumentException 如果訂單為空
     */
    public OrderProcessingResult process(Order order) {
        // 前置條件檢查
        Preconditions.requireNonNull(order, "訂單不能為空");
        
        try {
            // 驗證訂單
            order.process();
            
            // 計算折扣
            Money finalAmount = discountPolicy.apply(order);
            order.applyDiscount(finalAmount);
            
            // 處理支付
            Money effectiveAmount = order.getEffectiveAmount();
            UUID paymentId = UUID.randomUUID();
            eventBus.publish(new PaymentRequestedEvent(paymentId, order.getId().getValue(), effectiveAmount));
            
            // 返回結果
            return OrderProcessingResult.success(effectiveAmount);
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