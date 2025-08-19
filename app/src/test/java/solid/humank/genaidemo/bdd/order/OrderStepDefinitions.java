package solid.humank.genaidemo.bdd.order;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static solid.humank.genaidemo.testutils.matchers.OrderMatchers.hasItemCount;
import static solid.humank.genaidemo.testutils.matchers.OrderMatchers.hasShippingAddress;
import static solid.humank.genaidemo.testutils.matchers.OrderMatchers.hasStatus;

import java.math.BigDecimal;
import java.util.UUID;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import solid.humank.genaidemo.bdd.common.TestContext;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderStatus;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.testutils.annotations.BddTest;
import solid.humank.genaidemo.testutils.assertions.EnhancedAssertions;
import solid.humank.genaidemo.testutils.builders.OrderTestDataBuilder;
import solid.humank.genaidemo.testutils.handlers.TestScenarioHandler;

/**
 * Order Aggregate Root Cucumber Step Definitions - Refactored to remove
 * conditional logic and use
 * test utilities for better readability and maintainability
 */
@BddTest
public class OrderStepDefinitions {

    private final TestContext testContext = TestContext.getInstance();
    private final TestScenarioHandler scenarioHandler = new TestScenarioHandler();

    private Order order;

    @Given("an order has been created with customer ID {string} and shipping address {string}")
    public void createOrder(String customerId, String shippingAddress) {
        String uuid = UUID.randomUUID().toString();
        order = OrderTestDataBuilder.anOrder()
                .withCustomerId(uuid)
                .withShippingAddress(shippingAddress)
                .build();

        EnhancedAssertions.assertOrderCustomerId(order, uuid);
        assertThat(order, hasShippingAddress(shippingAddress));
    }

    @When("create a new order with customer ID {string} and shipping address {string}")
    public void createNewOrder(String customerId, String shippingAddress) {
        String uuid = UUID.randomUUID().toString();
        order = OrderTestDataBuilder.anOrder()
                .withCustomerId(uuid)
                .withShippingAddress(shippingAddress)
                .build();
    }

    @When("add product {string} to order with quantity {int} and unit price {int}")
    public void addItemToOrder(String productName, int quantity, int price) {
        scenarioHandler.handleAddItemScenario(
                order,
                productName,
                quantity,
                price,
                (e) -> testContext.setLastErrorMessage(e.getMessage()));
    }

    @When("submit order")
    public void submitOrder() {
        if (testContext.hasError()) {
            return;
        }

        try {
            order.submit();
        } catch (Exception e) {
            // 確保捕獲完整的錯誤訊息
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = e.getClass().getSimpleName() + ": " + e.toString();
            }
            testContext.setLastErrorMessage(errorMessage);
        }
    }

    @When("cancel order")
    public void cancelOrder() {
        order.cancel();
    }

    @When("apply fixed amount discount {int} to order")
    public void applyFixedDiscount(int discountAmount) {
        order.applyDiscount(Money.twd(discountAmount));
        EnhancedAssertions.assertOrderDiscountAmount(order, BigDecimal.valueOf(discountAmount));
    }

    @Then("order should be successfully created")
    public void orderShouldBeCreated() {
        assertNotNull(order, "Order should not be null");
        assertNotNull(order.getId(), "Order ID should not be null");
    }

    @Then("order status should be {string}")
    public void orderStatusShouldBe(String status) {
        OrderStatus expectedStatus = OrderStatus.valueOf(status);
        assertThat(order, hasStatus(expectedStatus));
    }

    @Then("order total amount should be {int}")
    public void orderTotalAmountShouldBe(int amount) {
        // Check effective amount as discount is applied to effective amount
        // 檢查有效金額，因為折扣是應用在有效金額上的
        EnhancedAssertions.assertOrderEffectiveAmount(order, BigDecimal.valueOf(amount));
    }

    @Then("order item count should be {int}")
    public void orderItemCountShouldBe(int count) {
        assertThat(order, hasItemCount(count));
    }

    @Then("order discount amount should be {int}")
    public void orderDiscountAmountShouldBe(int discountAmount) {
        EnhancedAssertions.assertOrderDiscountAmount(order, BigDecimal.valueOf(discountAmount));
    }

    @Then("should throw exception with error message {string}")
    public void shouldThrowExceptionWithMessage(String errorMessage) {
        assertTrue(testContext.hasError(), "Expected an error to occur");
        assertTrue(
                testContext.getLastErrorMessage().contains(errorMessage),
                "Error message should contain: " + errorMessage);
    }
}
