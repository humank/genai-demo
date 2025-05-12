package solid.humank.genaidemo.domain.order.validation;

import java.util.ArrayList;
import java.util.List;

import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.common.validation.DomainValidator;

/**
 * 訂單驗證器
 */
public class OrderValidator extends DomainValidator<Order> {
    private final int maxItems;
    private final Money maxTotal;
    
    /**
     * 建立訂單驗證器
     * 
     * @param maxItems 最大訂單項數量
     */
    public OrderValidator(int maxItems) {
        this(maxItems, Money.twd(100000));
    }
    
    /**
     * 建立訂單驗證器
     * 
     * @param maxItems 最大訂單項數量
     * @param maxTotal 最大訂單金額
     */
    public OrderValidator(int maxItems, Money maxTotal) {
        this.maxItems = maxItems;
        this.maxTotal = maxTotal;
    }

    @Override
    protected void doValidate(Order order) {
        List<String> errors = new ArrayList<>();
        
        validateBasicInfo(order);
        validateItems(order);
        validateAmount(order);
        
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }
    
    private void validateBasicInfo(Order order) {
        if (order.getCustomerId() == null) {
            addError("Customer ID cannot be empty");
        }
    }
    
    private void validateItems(Order order) {
        if (order.getItems().isEmpty()) {
            addError("Order must have at least one item");
        }
        
        if (order.getItems().size() > maxItems) {
            addError("Order cannot have more than " + maxItems + " items");
        }
    }
    
    private void validateAmount(Order order) {
        if (order.getTotalAmount().isGreaterThan(maxTotal)) {
            addError("Order total amount cannot exceed " + maxTotal);
        }
    }
}