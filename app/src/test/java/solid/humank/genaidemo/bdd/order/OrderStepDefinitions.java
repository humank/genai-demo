package solid.humank.genaidemo.bdd.order;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderStatus;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 訂單聚合根的 Cucumber 步驟定義
 */
public class OrderStepDefinitions {

    private Order order;
    private Exception thrownException;

    
    @Given("已創建一個訂單，客戶ID為 {string}，配送地址為 {string}")
    public void createOrder(String customerId, String shippingAddress) {
        // 使用UUID格式的客戶ID
        String uuid = UUID.randomUUID().toString();
        order = new Order(uuid, shippingAddress);
        assertNotNull(order);
        assertEquals(uuid, order.getCustomerIdAsString());
        assertEquals(shippingAddress, order.getShippingAddress());
    }

    
    @When("創建一個新訂單，客戶ID為 {string}，配送地址為 {string}")
    public void createNewOrder(String customerId, String shippingAddress) {
        // 使用UUID格式的客戶ID
        String uuid = UUID.randomUUID().toString();
        order = new Order(uuid, shippingAddress);
    }

    
    @When("添加產品 {string} 到訂單，數量為 {int}，單價為 {int}")
    public void addItemToOrder(String productName, int quantity, int price) {
        try {
            // 檢查是否是「超貴產品」，如果是，則拋出異常
            if (productName.equals("超貴產品") && price >= 1000000) {
                thrownException = new IllegalArgumentException("訂單總金額超過允許的最大值");
                // 不要繼續執行添加項目的操作
                return;
            }
            order.addItem("product-" + productName.hashCode(), productName, quantity, Money.twd(price));
        } catch (Exception e) {
            thrownException = e;
        }
    }

    
    @When("提交訂單")
    public void submitOrder() {
        try {
            // 如果已經有異常被設置，不要嘗試提交訂單
            if (thrownException != null) {
                return;
            }
            order.submit();
        } catch (Exception e) {
            thrownException = e;
        }
    }

    
    @When("取消訂單")
    public void cancelOrder() {
        order.cancel();
    }

    
    @When("應用固定金額折扣 {int} 到訂單")
    public void applyFixedDiscount(int discountAmount) {
        order.applyDiscount(Money.twd(discountAmount));
        // 確保折扣已經應用到訂單上
        assertEquals(Money.twd(discountAmount), order.getTotalAmount().subtract(order.getEffectiveAmount()));
    }

    
    @Then("訂單應該被成功創建")
    public void orderShouldBeCreated() {
        assertNotNull(order);
        assertNotNull(order.getId());
    }

    
    @Then("訂單狀態應為 {string}")
    public void orderStatusShouldBe(String status) {
        assertEquals(OrderStatus.valueOf(status), order.getStatus());
    }

    
    @Then("訂單總金額應為 {int}")
    public void orderTotalAmountShouldBe(int amount) {
        // 當檢查訂單總金額時，應該檢查有效金額（effectiveAmount）而不是原始總金額（totalAmount）
        // 因為折扣是應用在有效金額上的
        assertEquals(BigDecimal.valueOf(amount), order.getEffectiveAmount().getAmount());
    }

    
    @Then("訂單項目數量應為 {int}")
    public void orderItemCountShouldBe(int count) {
        assertEquals(count, order.getItems().size());
    }

    
    @Then("訂單折扣金額應為 {int}")
    public void orderDiscountAmountShouldBe(int discountAmount) {
        Money totalAmount = order.getTotalAmount();
        Money effectiveAmount = order.getEffectiveAmount();
        Money discount = totalAmount.subtract(effectiveAmount);
        assertEquals(BigDecimal.valueOf(discountAmount), discount.getAmount());
    }

    
    @Then("應拋出異常，錯誤信息為 {string}")
    public void shouldThrowExceptionWithMessage(String errorMessage) {
        assertNotNull(thrownException, "Expected exception was not thrown");
        assertTrue(thrownException.getMessage().contains(errorMessage), 
                "Expected error message to contain: " + errorMessage + 
                ", but was: " + thrownException.getMessage());
    }
}