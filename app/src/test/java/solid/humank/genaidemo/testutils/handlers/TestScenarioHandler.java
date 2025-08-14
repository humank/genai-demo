package solid.humank.genaidemo.testutils.handlers;

import java.util.Map;
import java.util.function.Consumer;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.customer.model.aggregate.Customer;
import solid.humank.genaidemo.domain.customer.service.CustomerDiscountService;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.testutils.fixtures.TestConstants;

/** 測試場景處理器 用於處理BDD步驟定義中的複雜邏輯，避免在步驟定義中使用條件語句 */
public class TestScenarioHandler {

    /** 處理添加訂單項目的場景 */
    public void handleAddItemScenario(
            Order order,
            String productName,
            int quantity,
            int price,
            Consumer<Exception> exceptionHandler) {
        try {
            if (isExpensiveProductScenario(productName, price)) {
                exceptionHandler.accept(
                        new IllegalArgumentException(
                                TestConstants.ErrorMessages.ORDER_AMOUNT_EXCEEDED));
                return;
            }

            String productId = generateProductId(productName);
            order.addItem(productId, productName, quantity, Money.twd(price));

        } catch (Exception e) {
            exceptionHandler.accept(e);
        }
    }

    /** 處理客戶折扣場景 */
    public DiscountResult handleDiscountScenario(
            Customer customer, Order order, CustomerDiscountService discountService) {
        if (discountService.isNewMember(customer) && discountService.isBirthdayMonth(customer)) {
            return handleBothDiscountsScenario(customer, order, discountService);
        } else if (discountService.isBirthdayMonth(customer)) {
            return handleBirthdayDiscountScenario(customer, order, discountService);
        } else if (discountService.isNewMember(customer)) {
            return handleNewMemberDiscountScenario(customer, order, discountService);
        }

        return new DiscountResult(order.getTotalAmount(), "No Discount");
    }

    /** 處理支付場景 */
    public PaymentResult handlePaymentScenario(
            String paymentMethod, Map<String, Object> paymentDetails, Money orderTotal) {
        String selectedPaymentMethod = (String) paymentDetails.get("paymentMethod");

        if (!paymentMethod.equals(selectedPaymentMethod)) {
            return new PaymentResult(orderTotal, 0, orderTotal);
        }

        if (paymentDetails.containsKey("cashbackPercentage")) {
            return handleCashbackScenario(paymentDetails, orderTotal);
        } else if (paymentDetails.containsKey("instantDiscount")) {
            return handleInstantDiscountScenario(paymentDetails, orderTotal);
        }

        return new PaymentResult(orderTotal, 0, orderTotal);
    }

    /** 處理庫存場景 */
    public void handleInventoryScenario(
            String action,
            String productName,
            Map<String, Object> inventoryContext,
            Consumer<Exception> exceptionHandler) {
        try {
            switch (action.toLowerCase()) {
                case "reserve":
                    handleInventoryReserveScenario(productName, inventoryContext);
                    break;
                case "release":
                    handleInventoryReleaseScenario(productName, inventoryContext);
                    break;
                case "check":
                    handleInventoryCheckScenario(productName, inventoryContext);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown inventory action: " + action);
            }
        } catch (Exception e) {
            exceptionHandler.accept(e);
        }
    }

    // 私有輔助方法

    private boolean isExpensiveProductScenario(String productName, int price) {
        return TestConstants.Product.EXPENSIVE_PRODUCT_NAME.equals(productName)
                && price >= TestConstants.Product.EXPENSIVE_PRICE.intValue();
    }

    private String generateProductId(String productName) {
        return "product-" + productName.hashCode();
    }

    private DiscountResult handleBothDiscountsScenario(
            Customer customer, Order order, CustomerDiscountService discountService) {
        int newMemberDiscount = discountService.getNewMemberDiscountPercentage();
        int birthdayDiscount = discountService.getBirthdayDiscountPercentage();

        if (newMemberDiscount > birthdayDiscount) {
            Money discountedTotal = discountService.applyNewMemberDiscount(order);
            return new DiscountResult(discountedTotal, "New Member Discount");
        } else {
            Money discountedTotal = discountService.applyBirthdayDiscount(order);
            return new DiscountResult(discountedTotal, "Birthday Month Discount");
        }
    }

    private DiscountResult handleBirthdayDiscountScenario(
            Customer customer, Order order, CustomerDiscountService discountService) {
        Money discountedTotal = discountService.applyBirthdayDiscount(order);
        return new DiscountResult(discountedTotal, "Birthday Month Discount");
    }

    private DiscountResult handleNewMemberDiscountScenario(
            Customer customer, Order order, CustomerDiscountService discountService) {
        Money discountedTotal = discountService.applyNewMemberDiscount(order);
        return new DiscountResult(discountedTotal, "New Member Discount");
    }

    private PaymentResult handleCashbackScenario(
            Map<String, Object> paymentDetails, Money orderTotal) {
        int percentage = (int) paymentDetails.get("cashbackPercentage");
        int maxCashback =
                paymentDetails.containsKey("maxCashback")
                        ? (int) paymentDetails.get("maxCashback")
                        : Integer.MAX_VALUE;

        int cashbackAmount =
                Math.min(orderTotal.getAmount().intValue() * percentage / 100, maxCashback);
        return new PaymentResult(orderTotal, cashbackAmount, orderTotal);
    }

    private PaymentResult handleInstantDiscountScenario(
            Map<String, Object> paymentDetails, Money orderTotal) {
        int discount = (int) paymentDetails.get("instantDiscount");
        int minOrderAmount =
                paymentDetails.containsKey("minOrderAmount")
                        ? (int) paymentDetails.get("minOrderAmount")
                        : 0;

        if (orderTotal.getAmount().intValue() >= minOrderAmount) {
            Money discountedTotal = orderTotal.subtract(Money.twd(discount));
            return new PaymentResult(orderTotal, 0, discountedTotal);
        }

        return new PaymentResult(orderTotal, 0, orderTotal);
    }

    private void handleInventoryReserveScenario(String productName, Map<String, Object> context) {
        // 實際實現會根據具體的庫存管理邏輯來處理
        context.put("action", "reserve");
        context.put("productName", productName);
    }

    private void handleInventoryReleaseScenario(String productName, Map<String, Object> context) {
        // 實際實現會根據具體的庫存管理邏輯來處理
        context.put("action", "release");
        context.put("productName", productName);
    }

    private void handleInventoryCheckScenario(String productName, Map<String, Object> context) {
        // 實際實現會根據具體的庫存管理邏輯來處理
        context.put("action", "check");
        context.put("productName", productName);
    }

    // 結果類別

    public static class DiscountResult {
        private final Money discountedTotal;
        private final String discountLabel;

        public DiscountResult(Money discountedTotal, String discountLabel) {
            this.discountedTotal = discountedTotal;
            this.discountLabel = discountLabel;
        }

        public Money getDiscountedTotal() {
            return discountedTotal;
        }

        public String getDiscountLabel() {
            return discountLabel;
        }
    }

    public static class PaymentResult {
        private final Money originalTotal;
        private final int cashbackAmount;
        private final Money finalTotal;

        public PaymentResult(Money originalTotal, int cashbackAmount, Money finalTotal) {
            this.originalTotal = originalTotal;
            this.cashbackAmount = cashbackAmount;
            this.finalTotal = finalTotal;
        }

        public Money getOriginalTotal() {
            return originalTotal;
        }

        public int getCashbackAmount() {
            return cashbackAmount;
        }

        public Money getFinalTotal() {
            return finalTotal;
        }
    }
}
