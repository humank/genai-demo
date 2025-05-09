package solid.humank.genaidemo.domain.order.model.specification;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

import solid.humank.genaidemo.domain.common.specification.Specification;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.order.model.valueobject.Money;

/**
 * 訂單折扣規格
 * 展示如何使用 Specification 模式來實作複雜的業務規則
 */
public class OrderDiscountSpecification implements Specification<Order> {
    
    private final Money minimumAmount;
    private final LocalDateTime currentTime;

    public OrderDiscountSpecification(Money minimumAmount, LocalDateTime currentTime) {
        this.minimumAmount = minimumAmount;
        this.currentTime = currentTime;
    }

    @Override
    public boolean isSatisfiedBy(Order order) {
        return isMinimumAmountMet(order) && 
               isWeekend() && 
               hasMultipleItems(order);
    }

    private boolean isMinimumAmountMet(Order order) {
        return order.getTotalAmount().amount()
                   .compareTo(minimumAmount.amount()) >= 0;
    }

    private boolean isWeekend() {
        DayOfWeek dayOfWeek = currentTime.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || 
               dayOfWeek == DayOfWeek.SUNDAY;
    }

    private boolean hasMultipleItems(Order order) {
        return order.getItems().size() >= 2;
    }

    /**
     * 建立一個週末特價規格
     * 訂單滿 1000 元且購買 2 件以上商品，在週末可以享有折扣
     */
    public static OrderDiscountSpecification weekendSpecial() {
        return new OrderDiscountSpecification(
            Money.twd(1000),
            LocalDateTime.now()
        );
    }
}