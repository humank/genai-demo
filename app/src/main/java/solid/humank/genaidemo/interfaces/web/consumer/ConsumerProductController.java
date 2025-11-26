package solid.humank.genaidemo.interfaces.web.consumer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 消費者商品控制器
 * 提供消費者端的商品瀏覽、搜索、推薦等功能
 */
@RestController
@RequestMapping("/api/consumer/products")
@Tag(name = "消費者商品", description = "消費者商品瀏覽和搜索功能")
public class ConsumerProductController {

    @GetMapping
    @Operation(summary = "瀏覽商品列表", description = "分頁瀏覽商品，支援分類篩選")
    public ResponseEntity<Map<String, Object>> browseProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) String sort) {

        // 驗證分頁參數
        if (page < 0 || size <= 0) {
            return ResponseEntity.badRequest().build();
        }

        // 模擬商品數據
        List<Map<String, Object>> products = createMockProducts();

        // 應用篩選
        if (category != null) {
            products = products.stream()
                    .filter(p -> category.equals(p.get("category")))
                    .toList();
        }

        if (minPrice != null) {
            products = products.stream()
                    .filter(p -> ((BigDecimal) p.get("price")).compareTo(minPrice) >= 0)
                    .toList();
        }

        if (maxPrice != null) {
            products = products.stream()
                    .filter(p -> ((BigDecimal) p.get("price")).compareTo(maxPrice) <= 0)
                    .toList();
        }

        // 分頁處理
        Pageable pageable = PageRequest.of(page, size);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), products.size());

        List<Map<String, Object>> pageContent = new ArrayList<>(products.subList(start, end));
        Page<Map<String, Object>> productPage = new PageImpl<>(pageContent, pageable, products.size());

        var response = new HashMap<String, Object>();
        response.put("content", productPage.getContent());
        response.put("totalElements", productPage.getTotalElements());
        response.put("totalPages", productPage.getTotalPages());
        response.put("size", productPage.getSize());
        response.put("number", productPage.getNumber());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @Operation(summary = "搜尋商品", description = "根據關鍵字搜尋商品")
    public ResponseEntity<Map<String, Object>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (keyword == null || keyword.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        List<Map<String, Object>> products = createMockProducts();

        // 模擬搜索
        List<Map<String, Object>> searchResults = products.stream()
                .filter(p -> ((String) p.get("name")).toLowerCase().contains(keyword.toLowerCase()))
                .toList();

        // 分頁處理
        Pageable pageable = PageRequest.of(page, size);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), searchResults.size());

        List<Map<String, Object>> pageContent = new ArrayList<>(searchResults.subList(start, end));
        Page<Map<String, Object>> productPage = new PageImpl<>(pageContent, pageable, searchResults.size());

        var response = new HashMap<String, Object>();
        response.put("content", productPage.getContent());
        response.put("totalElements", productPage.getTotalElements());
        response.put("totalPages", productPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{productId}")
    @Operation(summary = "獲取商品詳情", description = "獲取指定商品的詳細信息")
    public ResponseEntity<Map<String, Object>> getProductDetail(@PathVariable String productId) {
        Map<String, Object> product = createMockProducts().stream()
                .filter(p -> productId.equals(p.get("id")))
                .findFirst()
                .orElse(null);

        if (product == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(product);
    }

    @GetMapping("/categories")
    @Operation(summary = "獲取商品分類", description = "獲取所有可用的商品分類")
    public ResponseEntity<List<String>> getProductCategories() {
        List<String> categories = Arrays.asList("ELECTRONICS", "CLOTHING", "BOOKS", "HOME", "SPORTS");
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/recommendations")
    @Operation(summary = "獲取推薦商品", description = "根據客戶ID獲取個人化推薦商品")
    public ResponseEntity<List<Map<String, Object>>> getRecommendedProducts(
            @RequestParam String customerId,
            @RequestParam(defaultValue = "5") int limit) {

        List<Map<String, Object>> recommendations = createMockProducts().subList(0, Math.min(limit, 3));
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/{productId}/related")
    @Operation(summary = "獲取相關商品", description = "獲取與指定商品相關的其他商品")
    public ResponseEntity<List<Map<String, Object>>> getRelatedProducts(
            @PathVariable String productId,
            @RequestParam(defaultValue = "5") int limit) {

        List<Map<String, Object>> related = createMockProducts().subList(0, Math.min(limit, 3));
        return ResponseEntity.ok(related);
    }

    @GetMapping("/trending")
    @Operation(summary = "獲取熱門商品", description = "獲取當前熱門商品")
    public ResponseEntity<List<Map<String, Object>>> getTrendingProducts(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String category) {

        List<Map<String, Object>> trending = createMockProducts().subList(0, Math.min(limit, 5));
        return ResponseEntity.ok(trending);
    }

    @GetMapping("/new")
    @Operation(summary = "獲取新品推薦", description = "獲取最新上架的商品")
    public ResponseEntity<List<Map<String, Object>>> getNewProducts(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "30") int days) {

        List<Map<String, Object>> newProducts = createMockProducts().subList(0, Math.min(limit, 3));
        return ResponseEntity.ok(newProducts);
    }

    @GetMapping("/{productId}/price-history")
    @Operation(summary = "獲取商品價格歷史", description = "獲取指定商品的價格變化歷史")
    public ResponseEntity<Map<String, Object>> getProductPriceHistory(
            @PathVariable String productId,
            @RequestParam(defaultValue = "30") int days) {

        var response = new HashMap<String, Object>();
        response.put("productId", productId);
        response.put("priceHistory", Arrays.asList(
                Map.of("date", "2024-01-01", "price", 1000),
                Map.of("date", "2024-01-15", "price", 950),
                Map.of("date", "2024-02-01", "price", 900)));

        return ResponseEntity.ok(response);
    }

    private List<Map<String, Object>> createMockProducts() {
        var products = new ArrayList<Map<String, Object>>();

        var product1 = new HashMap<String, Object>();
        product1.put("id", "PROD-001");
        product1.put("name", "iPhone 15 Pro");
        product1.put("description", "最新款iPhone");
        product1.put("price", new BigDecimal("35900"));
        product1.put("category", "ELECTRONICS");
        product1.put("inStock", true);
        product1.put("stockQuantity", 50);
        products.add(product1);

        var product2 = new HashMap<String, Object>();
        product2.put("id", "PROD-002");
        product2.put("name", "MacBook Pro");
        product2.put("description", "專業筆記型電腦");
        product2.put("price", new BigDecimal("58000"));
        product2.put("category", "ELECTRONICS");
        product2.put("inStock", true);
        product2.put("stockQuantity", 20);
        products.add(product2);

        var product3 = new HashMap<String, Object>();
        product3.put("id", "PROD-003");
        product3.put("name", "AirPods Pro");
        product3.put("description", "無線耳機");
        product3.put("price", new BigDecimal("8990"));
        product3.put("category", "ELECTRONICS");
        product3.put("inStock", true);
        product3.put("stockQuantity", 100);
        products.add(product3);

        return products;
    }
}