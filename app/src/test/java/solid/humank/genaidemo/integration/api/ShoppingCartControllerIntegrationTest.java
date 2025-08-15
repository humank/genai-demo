package solid.humank.genaidemo.integration.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** ShoppingCartController 整合測試 測試購物車API的完整功能 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
@DisplayName("購物車API整合測試")
class ShoppingCartControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    private final String customerId = "660e8400-e29b-41d4-a716-446655440001";

    @Test
    @DisplayName("應該能夠獲取購物車")
    void shouldGetShoppingCart() throws Exception {
        mockMvc.perform(
                        get("/api/consumer/cart/{customerId}", customerId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.customerId").value(customerId))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.totalAmount").exists())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    @DisplayName("應該能夠添加商品到購物車")
    void shouldAddItemToCart() throws Exception {
        Map<String, Object> addItemRequest =
                Map.of("productId", "PROD-001", "quantity", 2, "unitPrice", 1000.00);

        mockMvc.perform(
                        post("/api/consumer/cart/{customerId}/items", customerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(addItemRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("商品已添加到購物車"));
    }

    @Test
    @DisplayName("應該能夠更新購物車商品數量")
    void shouldUpdateCartItemQuantity() throws Exception {
        // 先添加商品
        Map<String, Object> addItemRequest =
                Map.of("productId", "PROD-001", "quantity", 1, "unitPrice", 1000.00);

        mockMvc.perform(
                post("/api/consumer/cart/{customerId}/items", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addItemRequest)));

        // 更新數量
        Map<String, Object> updateRequest = Map.of("quantity", 3);

        mockMvc.perform(
                        put(
                                        "/api/consumer/cart/{customerId}/items/{productId}",
                                        customerId,
                                        "PROD-001")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("商品數量已更新"));
    }

    @Test
    @DisplayName("應該能夠從購物車移除商品")
    void shouldRemoveItemFromCart() throws Exception {
        // 先添加商品
        Map<String, Object> addItemRequest =
                Map.of("productId", "PROD-001", "quantity", 1, "unitPrice", 1000.00);

        mockMvc.perform(
                post("/api/consumer/cart/{customerId}/items", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addItemRequest)));

        // 移除商品
        mockMvc.perform(
                        delete(
                                        "/api/consumer/cart/{customerId}/items/{productId}",
                                        customerId,
                                        "PROD-001")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("商品已從購物車移除"));
    }

    @Test
    @DisplayName("應該能夠清空購物車")
    void shouldClearCart() throws Exception {
        mockMvc.perform(
                        delete("/api/consumer/cart/{customerId}/clear", customerId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("購物車已清空"));
    }

    @Test
    @DisplayName("應該能夠計算結帳金額")
    void shouldCalculateCheckout() throws Exception {
        // 先添加商品
        Map<String, Object> addItemRequest =
                Map.of("productId", "PROD-001", "quantity", 2, "unitPrice", 1000.00);

        mockMvc.perform(
                post("/api/consumer/cart/{customerId}/items", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addItemRequest)));

        // 計算結帳金額
        mockMvc.perform(
                        get("/api/consumer/cart/{customerId}/checkout", customerId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.subtotal").exists())
                .andExpect(jsonPath("$.discount").exists())
                .andExpect(jsonPath("$.total").exists())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    @DisplayName("應該能夠應用優惠券")
    void shouldApplyCoupon() throws Exception {
        Map<String, Object> applyCouponRequest = Map.of("couponCode", "NEWYEAR2024");

        mockMvc.perform(
                        post("/api/consumer/cart/{customerId}/coupons", customerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(applyCouponRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.discountAmount").exists());
    }

    @Test
    @DisplayName("應該能夠移除優惠券")
    void shouldRemoveCoupon() throws Exception {
        String couponCode = "NEWYEAR2024";

        mockMvc.perform(
                        delete(
                                        "/api/consumer/cart/{customerId}/coupons/{couponCode}",
                                        customerId,
                                        couponCode)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("優惠券已移除"));
    }

    @Test
    @DisplayName("添加無效商品應該返回400")
    void shouldReturn400ForInvalidProduct() throws Exception {
        Map<String, Object> invalidRequest =
                Map.of("productId", "", "quantity", 0, "unitPrice", -100.00);

        mockMvc.perform(
                        post("/api/consumer/cart/{customerId}/items", customerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("更新不存在的商品應該返回404")
    void shouldReturn404ForNonExistentItem() throws Exception {
        Map<String, Object> updateRequest = Map.of("quantity", 3);

        mockMvc.perform(
                        put(
                                        "/api/consumer/cart/{customerId}/items/{productId}",
                                        customerId,
                                        "NON-EXISTENT")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("使用無效優惠券應該返回400")
    void shouldReturn400ForInvalidCoupon() throws Exception {
        Map<String, Object> invalidCouponRequest = Map.of("couponCode", "INVALID-COUPON");

        mockMvc.perform(
                        post("/api/consumer/cart/{customerId}/coupons", customerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidCouponRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("應該能夠獲取可用的促銷活動")
    void shouldGetAvailablePromotions() throws Exception {
        mockMvc.perform(
                        get("/api/consumer/cart/{customerId}/promotions", customerId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("應該能夠應用促銷活動")
    void shouldApplyPromotion() throws Exception {
        Map<String, Object> applyPromotionRequest = Map.of("promotionId", "promo-001");

        mockMvc.perform(
                        post("/api/consumer/cart/{customerId}/promotions", customerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(applyPromotionRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.discountAmount").exists());
    }

    @Test
    @DisplayName("應該能夠檢查庫存可用性")
    void shouldCheckInventoryAvailability() throws Exception {
        // 先添加商品
        Map<String, Object> addItemRequest =
                Map.of("productId", "PROD-001", "quantity", 1, "unitPrice", 1000.00);

        mockMvc.perform(
                post("/api/consumer/cart/{customerId}/items", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addItemRequest)));

        // 檢查庫存
        mockMvc.perform(
                        get("/api/consumer/cart/{customerId}/inventory-check", customerId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.available").isBoolean())
                .andExpect(jsonPath("$.items").isArray());
    }
}
