package solid.humank.genaidemo.domain.order.model.policy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import solid.humank.genaidemo.domain.common.policy.DomainPolicy;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.order.model.specification.OrderDiscountSpecification;
import solid.humank.genaidemo.domain.common.valueobject.Money;

/**
 * 訂單折扣政策
 * 結合 Specification 和 Policy 模式來實作折扣規則
 */
public class OrderDiscountPolicy implements DomainPolicy<Order, Money> {
    
    private final OrderDiscountSpecification specification;
    private final BigDecimal discountRate;

    public OrderDiscountPolicy(LocalDateTime currentTime, BigDecimal discountRate) {
        this.specification = new OrderDiscountSpecification(
            Money.twd(1000),
            currentTime
        );
        this.discountRate = discountRate;
    }

    @Override
    public Money apply(Order order) {
        if (!isApplicableTo(order)) {
            return order.getTotalAmount();
        }

        BigDecimal discountAmount = order.getTotalAmount().amount()
            .multiply(discountRate);
        
        return new Money(
            order.getTotalAmount().amount().subtract(discountAmount),
            order.getTotalAmount().currency()
        );
    }

    @Override
    public boolean isApplicableTo(Order order) {
        return specification.isSatisfiedBy(order);
    }

    /**
     * 建立一個週末九折優惠政策
     */
    public static OrderDiscountPolicy weekendDiscount() {
        return new OrderDiscountPolicy(
            LocalDateTime.now(),
            new BigDecimal("0.1")  // 10% 折扣
        );
    }
}