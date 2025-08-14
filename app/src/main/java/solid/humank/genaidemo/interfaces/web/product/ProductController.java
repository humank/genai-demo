package solid.humank.genaidemo.interfaces.web.product;

import solid.humank.genaidemo.application.product.service.ProductApplicationService;
import solid.humank.genaidemo.application.product.ProductDto;
import solid.humank.genaidemo.application.product.ProductPageDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 產品控制器
 * 提供產品相關的 API 端點
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductApplicationService productApplicationService;
    
    public ProductController(ProductApplicationService productApplicationService) {
        this.productApplicationService = productApplicationService;
    }

    /**
     * 獲取產品列表
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            ProductPageDto productPage = productApplicationService.getProducts(page, size);
            
            response.put("success", true);
            response.put("data", productPage);
            
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
        
        try {
            Optional<ProductDto> product = productApplicationService.getProduct(productId);
            
            if (product.isPresent()) {
                response.put("success", true);
                response.put("data", product.get());
            } else {
                response.put("success", false);
                response.put("message", "產品不存在");
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "獲取產品時發生錯誤: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 更新產品
     */
    @PutMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> updateProduct(
            @PathVariable String productId,
            @RequestBody solid.humank.genaidemo.interfaces.web.product.dto.UpdateProductRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            solid.humank.genaidemo.application.product.dto.command.UpdateProductCommand command = 
                solid.humank.genaidemo.application.product.dto.command.UpdateProductCommand.of(
                    productId,
                    request.getName(),
                    request.getDescription(),
                    request.getPrice(),
                    request.getCurrency(),
                    request.getCategory()
                );
            
            ProductDto updatedProduct = productApplicationService.updateProduct(command);
            
            response.put("success", true);
            response.put("data", updatedProduct);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "更新產品時發生錯誤: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 刪除產品
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> deleteProduct(@PathVariable String productId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            productApplicationService.deleteProduct(productId);
            
            response.put("success", true);
            response.put("message", "產品已成功刪除");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "刪除產品時發生錯誤: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
        
        return ResponseEntity.ok(response);
    }

}
