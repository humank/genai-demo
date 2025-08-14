package solid.humank.genaidemo.interfaces.web.product;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 產品控制器
 * 提供產品相關的 API 端點
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private DataSource dataSource;

    /**
     * 獲取產品列表
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> products = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection()) {
            // 計算總數
            int totalElements = 0;
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM inventories WHERE status = 'ACTIVE'")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    totalElements = rs.getInt(1);
                }
            }
            
            // 獲取分頁數據
            String sql = "SELECT product_id, available_quantity, reserved_quantity, status " +
                        "FROM inventories WHERE status = 'ACTIVE' " +
                        "ORDER BY product_id LIMIT ? OFFSET ?";
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, size);
                ps.setInt(2, page * size);
                ResultSet rs = ps.executeQuery();
                
                while (rs.next()) {
                    Map<String, Object> product = new HashMap<>();
                    String productId = rs.getString("product_id");
                    int availableQuantity = rs.getInt("available_quantity");
                    int reservedQuantity = rs.getInt("reserved_quantity");
                    
                    product.put("id", productId);
                    product.put("name", generateProductName(productId));
                    product.put("description", generateProductDescription(productId));
                    product.put("price", Map.of(
                        "amount", generatePrice(productId),
                        "currency", "TWD"
                    ));
                    product.put("category", generateCategory(productId));
                    product.put("inStock", availableQuantity > 0);
                    product.put("stockQuantity", availableQuantity);
                    
                    products.add(product);
                }
            }
            
            // 構建分頁響應
            int totalPages = (int) Math.ceil((double) totalElements / size);
            
            Map<String, Object> pageInfo = new HashMap<>();
            pageInfo.put("content", products);
            pageInfo.put("totalElements", totalElements);
            pageInfo.put("totalPages", totalPages);
            pageInfo.put("size", size);
            pageInfo.put("number", page);
            pageInfo.put("first", page == 0);
            pageInfo.put("last", page >= totalPages - 1);
            
            response.put("success", true);
            response.put("data", pageInfo);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "獲取產品列表時發生錯誤: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 獲取單個產品
     */
    @GetMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> getProduct(@PathVariable String productId) {
        Map<String, Object> response = new HashMap<>();
        
        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT product_id, available_quantity, reserved_quantity, status " +
                        "FROM inventories WHERE product_id = ? AND status = 'ACTIVE'";
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, productId);
                ResultSet rs = ps.executeQuery();
                
                if (rs.next()) {
                    int availableQuantity = rs.getInt("available_quantity");
                    
                    Map<String, Object> product = new HashMap<>();
                    product.put("id", productId);
                    product.put("name", generateProductName(productId));
                    product.put("description", generateProductDescription(productId));
                    product.put("price", Map.of(
                        "amount", generatePrice(productId),
                        "currency", "TWD"
                    ));
                    product.put("category", generateCategory(productId));
                    product.put("inStock", availableQuantity > 0);
                    product.put("stockQuantity", availableQuantity);
                    
                    response.put("success", true);
                    response.put("data", product);
                } else {
                    response.put("success", false);
                    response.put("message", "產品不存在");
                    return ResponseEntity.notFound().build();
                }
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "獲取產品時發生錯誤: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    // 輔助方法：根據產品ID生成產品名稱
    private String generateProductName(String productId) {
        String[] names = {
            "iPhone 15 Pro", "MacBook Pro 14\"", "AirPods Pro", "iPad Air",
            "Apple Watch Series 9", "Magic Keyboard", "Samsung Galaxy S24",
            "Dell XPS 13", "Sony WH-1000XM5", "Nintendo Switch"
        };
        return names[Math.abs(productId.hashCode()) % names.length];
    }

    // 輔助方法：根據產品ID生成產品描述
    private String generateProductDescription(String productId) {
        String[] descriptions = {
            "最新款智慧型手機，配備先進處理器",
            "專業級筆記型電腦，適合創作者使用",
            "主動降噪耳機，提供卓越音質體驗",
            "輕薄平板電腦，娛樂辦公兩相宜",
            "智慧手錶，健康監測功能完備",
            "無線鍵盤，提升工作效率",
            "旗艦級智慧型手機，攝影功能強大",
            "輕薄筆電，商務人士首選",
            "頂級降噪耳機，音樂愛好者必備",
            "掌上遊戲機，隨時隨地享受遊戲"
        };
        return descriptions[Math.abs(productId.hashCode()) % descriptions.length];
    }

    // 輔助方法：根據產品ID生成價格
    private int generatePrice(String productId) {
        int[] prices = {
            35900, 65900, 7490, 19900, 13900, 10900,
            28900, 45900, 12900, 9990
        };
        return prices[Math.abs(productId.hashCode()) % prices.length];
    }

    // 輔助方法：根據產品ID生成分類
    private String generateCategory(String productId) {
        String[] categories = {
            "智慧型手機", "筆記型電腦", "音響設備", "平板電腦",
            "穿戴裝置", "配件", "智慧型手機", "筆記型電腦",
            "音響設備", "遊戲設備"
        };
        return categories[Math.abs(productId.hashCode()) % categories.length];
    }
}
