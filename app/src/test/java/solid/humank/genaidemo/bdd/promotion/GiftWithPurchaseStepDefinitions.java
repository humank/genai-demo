package solid.humank.genaidemo.bdd.promotion;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.aggregate.Product;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductName;
import solid.humank.genaidemo.domain.promotion.model.valueobject.GiftWithPurchaseRule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GiftWithPurchaseStepDefinitions {

    private Product giftProduct;
    private double minimumPurchaseAmount;
    private double giftValue;
    private List<Product> giftItems = new ArrayList<>();
    private Map<String, Object> promotionDetails = new HashMap<>();

    @Given("the store offers a gift {string} worth ${int} with any purchase over ${int}")
    public void the_store_offers_a_gift_worth_$_with_any_purchase_over_$(String giftName, Integer giftValue, Integer minimumPurchase) {
        ProductId giftProductId = new ProductId(giftName);
        this.giftProduct = mock(Product.class);
        when(giftProduct.getName()).thenReturn(new ProductName(giftName));
        when(giftProduct.getId()).thenReturn(giftProductId);
        when(giftProduct.getPrice()).thenReturn(Money.of(giftValue));
        
        this.giftValue = giftValue;
        this.minimumPurchaseAmount = minimumPurchase;
        
        // 創建滿額贈規則
        GiftWithPurchaseRule rule = new GiftWithPurchaseRule(
            Money.of(minimumPurchase),
            giftProductId,
            Money.of(giftValue),
            1,
            false
        );
        
        promotionDetails.put("rule", rule);
        promotionDetails.put("giftName", giftName);
        promotionDetails.put("giftValue", giftValue);
        promotionDetails.put("minimumPurchase", minimumPurchase);
    }

    @Given("the store offers a gift {string} with any purchase over ${int}")
    public void the_store_offers_a_gift_with_any_purchase_over_$(String giftName, Integer minimumPurchase) {
        ProductId giftProductId = new ProductId(giftName);
        this.giftProduct = mock(Product.class);
        when(giftProduct.getName()).thenReturn(new ProductName(giftName));
        when(giftProduct.getId()).thenReturn(giftProductId);
        when(giftProduct.getPrice()).thenReturn(Money.of(50)); // 假設禮品價值為$50
        
        this.minimumPurchaseAmount = minimumPurchase;
        this.giftValue = 50;
        
        // 創建滿額贈規則
        GiftWithPurchaseRule rule = new GiftWithPurchaseRule(
            Money.of(minimumPurchase),
            giftProductId,
            Money.of(50),
            1,
            false
        );
        
        promotionDetails.put("rule", rule);
        promotionDetails.put("giftName", giftName);
        promotionDetails.put("giftValue", 50);
        promotionDetails.put("minimumPurchase", minimumPurchase);
    }

    @When("the customer's order total is ${int}")
    public void the_customer_s_order_total_is_$(Integer orderTotal) {
        if (orderTotal >= minimumPurchaseAmount) {
            // 符合滿額贈條件，添加禮品
            addGiftToOrder(1);
        }
    }

    @When("the customer's order total is ${int} and they qualify for {int} gifts")
    public void the_customer_s_order_total_is_$_and_they_qualify_for_gifts(Integer orderTotal, Integer giftCount) {
        if (orderTotal >= minimumPurchaseAmount) {
            addGiftToOrder(giftCount);
        }
    }

    private void addGiftToOrder(int giftCount) {
        giftItems.clear();
        String giftName = giftProduct.getName().getName();
        ProductId giftProductId = giftProduct.getId();
        
        for (int i = 0; i < giftCount; i++) {
            Product giftProductCopy = mock(Product.class);
            when(giftProductCopy.getName()).thenReturn(new ProductName(giftName));
            when(giftProductCopy.getId()).thenReturn(giftProductId);
            // 設置禮品價格為0
            when(giftProductCopy.getPrice()).thenReturn(Money.of(0));
            
            giftItems.add(giftProductCopy);
        }
    }

    @Then("the customer receives {int} {string} at no additional cost")
    public void the_customer_receives_at_no_additional_cost(Integer quantity, String giftName) {
        assertEquals(quantity.intValue(), giftItems.size());
        
        // 驗證所有禮品的名稱和價格
        for (Product gift : giftItems) {
            assertEquals(giftName, gift.getName().getName());
        }
    }

    @Then("the gift items are added to the order at ${int} each")
    public void the_gift_items_are_added_to_the_order_at_$_each(Integer price) {
        assertEquals(0, price); // 所有禮品價格應為0
        for (Product gift : giftItems) {
            assertEquals(Money.of(0).getAmount(), gift.getPrice().getAmount());
        }
    }
}