package solid.humank.genaidemo.examples.order.validation;

import java.math.BigDecimal;

import solid.humank.genaidemo.ddd.validation.DomainValidator;
import solid.humank.genaidemo.examples.order.Money;
import solid.humank.genaidemo.examples.order.Order;
import solid.humank.genaidemo.exceptions.ValidationException;

public class OrderValidator extends DomainValidator<Order> {
    private static final int MAX_ITEMS = 100;
    private static final Money MAX_TOTAL = Money.twd(1000000); // 最大金額100萬

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
        if (order.getItems().size() > MAX_ITEMS) {
            addError("訂單商品項目不能超過 " + MAX_ITEMS + " 個");
        }
    }

    private void validateAmount(Order order) {
        Money totalAmount = order.getTotalAmount();
        if (totalAmount.amount().compareTo(BigDecimal.ZERO) < 0) {
            addError("訂單金額不能為負數");
        }
        if (totalAmount.amount().compareTo(MAX_TOTAL.amount()) > 0) {
            addError("訂單金額不能超過 " + MAX_TOTAL);
        }
    }
}
