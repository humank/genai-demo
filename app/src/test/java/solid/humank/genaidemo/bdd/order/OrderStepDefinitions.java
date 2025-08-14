package solid.humank.genaidemo.bdd.order;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static solid.humank.genaidemo.testutils.matchers.OrderMatchers.*;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.math.BigDecimal;
import java.util.UUID;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderStatus;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.testutils.annotations.BddTest;
import solid.humank.genaidemo.testutils.assertions.EnhancedAssertions;
import solid.humank.genaidemo.testutils.builders.OrderTestDataBuilder;
import solid.humank.genaidemo.testutils.context.TestContext;
import solid.humank.genaidemo.testutils.handlers.TestScenarioHandler;

/** 訂單聚合根的 Cucumber 步驟定義 重構後移除了條件邏輯，使用測試輔助工具來提高可讀性和維護性 */
@BddTest
public class OrderStepDefinitions {

    private final TestContext testContext = new TestContext();
    private final TestScenarioHandler scenarioHandler = new TestScenarioHandler();

    private Order order;

    @Given("已創建一個訂單，客戶ID為 {string}，配送地址為 {string}")
    public void createOrder(String customerId, String shippingAddress) {
        String uuid = UUID.randomUUID().toString();
        order =
                OrderTestDataBuilder.anOrder()
                        .withCustomerId(uuid)
                        .withShippingAddress(shippingAddress)
                        .build();

        EnhancedAssertions.assertOrderCustomerId(order, uuid);
        assertThat(order, hasShippingAddress(shippingAddress));
    }

    @When("創建一個新訂單，客戶ID為 {string}，配送地址為 {string}")
    public void createNewOrder(String customerId, String shippingAddress) {
        String uuid = UUID.randomUUID().toString();
        order =
                OrderTestDataBuilder.anOrder()
                        .withCustomerId(uuid)
                        .withShippingAddress(shippingAddress)
                        .build();
    }

    @When("添加產品 {string} 到訂單，數量為 {int}，單價為 {int}")
    public void addItemToOrder(String productName, int quantity, int price) {
        scenarioHandler.handleAddItemScenario(
                order,
                productName,
                quantity,
                price,
                testContext.getExceptionHandler()::captureException);
    }

    @When("提交訂單")
    public void submitOrder() {
        if (testContext.getExceptionHandler().hasException()) {
            return;
        }

        try {
            order.submit();
        } catch (Exception e) {
            testContext.getExceptionHandler().captureException(e);
        }
    }

    @When("取消訂單")
    public void cancelOrder() {
        order.cancel();
    }

    @When("應用固定金額折扣 {int} 到訂單")
    public void applyFixedDiscount(int discountAmount) {
        order.applyDiscount(Money.twd(discountAmount));
        EnhancedAssertions.assertOrderDiscountAmount(order, BigDecimal.valueOf(discountAmount));
    }

    @Then("訂單應該被成功創建")
    public void orderShouldBeCreated() {
        assertNotNull(order, "Order should not be null");
        assertNotNull(order.getId(), "Order ID should not be null");
    }

    @Then("訂單狀態應為 {string}")
    public void orderStatusShouldBe(String status) {
        OrderStatus expectedStatus = OrderStatus.valueOf(status);
        assertThat(order, hasStatus(expectedStatus));
    }

    @Then("訂單總金額應為 {int}")
    public void orderTotalAmountShouldBe(int amount) {
        // 檢查有效金額，因為折扣是應用在有效金額上的
        EnhancedAssertions.assertOrderEffectiveAmount(order, BigDecimal.valueOf(amount));
    }

    @Then("訂單項目數量應為 {int}")
    public void orderItemCountShouldBe(int count) {
        assertThat(order, hasItemCount(count));
    }

    @Then("訂單折扣金額應為 {int}")
    public void orderDiscountAmountShouldBe(int discountAmount) {
        EnhancedAssertions.assertOrderDiscountAmount(order, BigDecimal.valueOf(discountAmount));
    }

    @Then("應拋出異常，錯誤信息為 {string}")
    public void shouldThrowExceptionWithMessage(String errorMessage) {
        testContext.getExceptionHandler().expectException(errorMessage);
    }
}
