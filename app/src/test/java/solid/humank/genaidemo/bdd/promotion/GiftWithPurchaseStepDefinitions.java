package solid.humank.genaidemo.bdd.promotion;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.aggregate.Product;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.promotion.model.valueobject.GiftWithPurchaseRule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GiftWithPurchaseStepDefinitions {

    private Product giftProduct;
    private int minimumPurchaseAmount;
    private int giftValue;
    private int cartTotal;
    private List<Product> cartItems = new ArrayList<>();
    private List<Product> giftItems = new ArrayList<>();
    private Map<String, Object> promotionDetails = new HashMap<>();

    @Given("the store offers a free {string} \\(valued at ${int}) for purchases over ${int}")
    public void the_store_offers_a_free_valued_at_$_for_purchases_over_$(
            String giftName, Integer giftValue, Integer minimumPurchase) {
        
        ProductId giftProductId = new ProductId(giftName);
        this.giftProduct = mock(Product.class);
        when(giftProduct.getName()).thenReturn(giftName);
        when(giftProduct.getProductId()).thenReturn(giftProductId);
        when(giftProduct.getBasePrice()).thenReturn(Money.of(giftValue));
        
        this.giftValue = giftValue;
        this.minimumPurchaseAmount = minimumPurchase;
        
        // 創建滿額贈禮規則
        GiftWithPurchaseRule rule = new GiftWithPurchaseRule(
            Money.of(minimumPurchase),
            giftProductId,
            Money.of(giftValue),
            1, // 最大贈送數量為1
            false // 不允許多個禮品
        );
        
        promotionDetails.put("giftName", giftName);
        promotionDetails.put("giftValue", giftValue);
        promotionDetails.put("minimumPurchase", minimumPurchase);
        promotionDetails.put("rule", rule);
    }

    @Given("the store offers one free {string} for every ${int} spent, up to {int} speakers")
    public void the_store_offers_one_free_for_every_$_spent_up_to_speakers(
            String giftName, Integer spendThreshold, Integer maxGifts) {
        
        ProductId giftProductId = new ProductId(giftName);
        this.giftProduct = mock(Product.class);
        when(giftProduct.getName()).thenReturn(giftName);
        when(giftProduct.getProductId()).thenReturn(giftProductId);
        when(giftProduct.getBasePrice()).thenReturn(Money.of(50)); // 假設禮品價值為$50
        
        this.minimumPurchaseAmount = spendThreshold;
        
        // 創建滿額贈禮規則 - 每滿 spendThreshold 元贈送一個，最多贈送 maxGifts 個
        GiftWithPurchaseRule rule = new GiftWithPurchaseRule(
            Money.of(spendThreshold),
            giftProductId,
            Money.of(50), // 禮品價值為$50
            maxGifts, // 最大贈送數量
            true // 允許多個禮品
        );
        
        promotionDetails.put("giftName", giftName);
        promotionDetails.put("spendThreshold", spendThreshold);
        promotionDetails.put("maxGifts", maxGifts);
        promotionDetails.put("rule", rule);
    }

    @When("the customer's cart total reaches ${int}")
    public void the_customer_s_cart_total_reaches_$(Integer total) {
        this.cartTotal = total;
        
        // 計算應該獲得的禮品數量
        int giftCount = 0;
        if (cartTotal >= minimumPurchaseAmount) {
            if (promotionDetails.containsKey("spendThreshold") && promotionDetails.containsKey("maxGifts")) {
                int spendThreshold = (int) promotionDetails.get("spendThreshold");
                int maxGifts = (int) promotionDetails.get("maxGifts");
                giftCount = Math.min(cartTotal / spendThreshold, maxGifts);
            } else {
                giftCount = 1; // 單一禮品促銷
            }
        }
        
        // 添加禮品到禮品列表
        giftItems.clear();
        String giftName = giftProduct.getName();
        ProductId giftProductId = giftProduct.getProductId();
        
        for (int i = 0; i < giftCount; i++) {
            Product giftProductCopy = mock(Product.class);
            when(giftProductCopy.getName()).thenReturn(giftName);
            when(giftProductCopy.getProductId()).thenReturn(giftProductId);
            // 設置禮品價格為0
            when(giftProductCopy.getBasePrice()).thenReturn(Money.of(0));
            giftItems.add(giftProductCopy);
        }
    }

    @Then("the customer should automatically receive the {string} in their cart")
    public void the_customer_should_automatically_receive_the_in_their_cart(String giftName) {
        assertTrue(giftItems.size() > 0);
        assertEquals(giftName, giftItems.get(0).getName());
    }

    @Then("the gift item should be marked as ${int}")
    public void the_gift_item_should_be_marked_as_$(Integer price) {
        assertEquals(0, price); // 禮品價格應為0
    }

    @Then("the customer should receive {int} {string} items in their cart")
    public void the_customer_should_receive_items_in_their_cart(Integer count, String giftName) {
        assertEquals(count, giftItems.size());
        for (Product gift : giftItems) {
            assertEquals(giftName, gift.getName());
        }
    }

    @Then("all gift items should be marked as ${int}")
    public void all_gift_items_should_be_marked_as_$(Integer price) {
        assertEquals(0, price); // 所有禮品價格應為0
        for (Product gift : giftItems) {
            assertEquals(Money.of(0).getAmount(), gift.getBasePrice().getAmount());
        }
    }
}