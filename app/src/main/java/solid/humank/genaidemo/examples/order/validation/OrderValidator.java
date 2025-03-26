package solid.humank.genaidemo.examples.order.validation;

import java.math.BigDecimal;

import solid.humank.genaidemo.ddd.validation.DomainValidator;
import solid.humank.genaidemo.examples.order.Money;
import solid.humank.genaidemo.examples.order.Order;
import solid.humank.genaidemo.exceptions.ValidationException;
import solid.humank.genaidemo.utils.Preconditions;

public class OrderValidator extends DomainValidator<Order> {
    private final int maxItems;
    private final Money maxTotal;

    /**
     * 使用預設參數創建驗證器
     */
    public OrderValidator() {
        this(100, Money.twd(1000000)); // 預設: 最多100項，最大金額100萬
    }
    
    /**
     * 使用指定參數創建驗證器
     *
     * @param maxItems 最大項目數
     * @param maxTotal 最大訂單金額
     */
    public OrderValidator(int maxItems, Money maxTotal) {
        Preconditions.requirePositive(maxItems, "最大項目數必須大於零");
        Preconditions.requireNonNull(maxTotal, "最大訂單金額不能為空");
        
        this.maxItems = maxItems;
        this.maxTotal = maxTotal;
    }

    @Override
    protected void doValidate(Order order) {
        validateBasicInfo(order);
        validateItems(order);
        validateAmount(order);

        if (hasErrors()) {
            throw new ValidationException(getErrors());
        }
    }

    private void validateBasicInfo(Order order) {
        if (order.getCustomerId() == null || order.getCustomerId().isBlank()) {
            addError("客戶ID不能為空");
        }
    }

    private void validateItems(Order order) {
        if (order.getItems().isEmpty()) {
            addError("訂單必須至少包含一個商品項目");
        }
        if (order.getItems().size() > maxItems) {
            addError("訂單商品項目不能超過 " + maxItems + " 個");
        }
    }

    private void validateAmount(Order order) {
        Money totalAmount = order.getTotalAmount();
        if (totalAmount.amount().compareTo(BigDecimal.ZERO) < 0) {
            addError("訂單金額不能為負數");
        }
        if (totalAmount.amount().compareTo(maxTotal.amount()) > 0) {
            addError("訂單金額不能超過 " + maxTotal);
        }
    }
}
