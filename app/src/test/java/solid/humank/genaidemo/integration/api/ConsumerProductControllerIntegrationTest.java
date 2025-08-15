package solid.humank.genaidemo.integration.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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

/** ConsumerProductController 整合測試 測試消費者商品API的完整功能 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
@DisplayName("消費者商品API整合測試")
class ConsumerProductControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @Test
    @DisplayName("應該能夠瀏覽商品列表")
    void shouldBrowseProducts() throws Exception {
        mockMvc.perform(
                        get("/api/consumer/products")
                                .param("page", "0")
                                .param("size", "10")
                                .param("category", "ELECTRONICS")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());
    }

    @Test
    @DisplayName("應該能夠搜尋商品")
    void shouldSearchProducts() throws Exception {
        mockMvc.perform(
                        get("/api/consumer/products/search")
                                .param("keyword", "iPhone")
                                .param("page", "0")
                                .param("size", "10")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("應該能夠獲取商品詳情")
    void shouldGetProductDetail() throws Exception {
        String productId = "PROD-001";

        mockMvc.perform(
                        get("/api/consumer/products/{productId}", productId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.price").exists())
                .andExpect(jsonPath("$.description").exists());
    }

    @Test
    @DisplayName("獲取不存在的商品應該返回404")
    void shouldReturn404ForNonExistentProduct() throws Exception {
        String nonExistentProductId = "PROD-999";

        mockMvc.perform(
                        get("/api/consumer/products/{productId}", nonExistentProductId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("應該能夠獲取商品分類")
    void shouldGetProductCategories() throws Exception {
        mockMvc.perform(
                        get("/api/consumer/products/categories")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("應該能夠獲取推薦商品")
    void shouldGetRecommendedProducts() throws Exception {
        String customerId = "660e8400-e29b-41d4-a716-446655440001";

        mockMvc.perform(
                        get("/api/consumer/products/recommendations")
                                .param("customerId", customerId)
                                .param("limit", "5")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("應該能夠獲取相關商品")
    void shouldGetRelatedProducts() throws Exception {
        String productId = "PROD-001";

        mockMvc.perform(
                        get("/api/consumer/products/{productId}/related", productId)
                                .param("limit", "5")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("應該能夠獲取熱門商品")
    void shouldGetTrendingProducts() throws Exception {
        mockMvc.perform(
                        get("/api/consumer/products/trending")
                                .param("limit", "10")
                                .param("category", "ELECTRONICS")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("應該能夠獲取新品推薦")
    void shouldGetNewProducts() throws Exception {
        mockMvc.perform(
                        get("/api/consumer/products/new")
                                .param("limit", "10")
                                .param("days", "30")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("應該能夠獲取商品價格歷史")
    void shouldGetProductPriceHistory() throws Exception {
        String productId = "PROD-001";

        mockMvc.perform(
                        get("/api/consumer/products/{productId}/price-history", productId)
                                .param("days", "30")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.priceHistory").isArray());
    }

    @Test
    @DisplayName("無效的分頁參數應該返回400")
    void shouldReturn400ForInvalidPaginationParams() throws Exception {
        mockMvc.perform(
                        get("/api/consumer/products")
                                .param("page", "-1")
                                .param("size", "0")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("空的搜尋關鍵字應該返回400")
    void shouldReturn400ForEmptySearchKeyword() throws Exception {
        mockMvc.perform(
                        get("/api/consumer/products/search")
                                .param("keyword", "")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("應該能夠按價格範圍篩選商品")
    void shouldFilterProductsByPriceRange() throws Exception {
        mockMvc.perform(
                        get("/api/consumer/products")
                                .param("minPrice", "1000")
                                .param("maxPrice", "5000")
                                .param("page", "0")
                                .param("size", "10")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("應該能夠按評分篩選商品")
    void shouldFilterProductsByRating() throws Exception {
        mockMvc.perform(
                        get("/api/consumer/products")
                                .param("minRating", "4")
                                .param("page", "0")
                                .param("size", "10")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("應該能夠排序商品")
    void shouldSortProducts() throws Exception {
        mockMvc.perform(
                        get("/api/consumer/products")
                                .param("sort", "price,desc")
                                .param("page", "0")
                                .param("size", "10")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray());
    }
}
