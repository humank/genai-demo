package solid.humank.genaidemo.interfaces.web.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import solid.humank.genaidemo.application.common.dto.StandardErrorResponse;
import solid.humank.genaidemo.application.product.ProductDto;
import solid.humank.genaidemo.application.product.ProductPageDto;
import solid.humank.genaidemo.application.product.service.ProductApplicationService;

/** 產品控制器 提供產品相關的 API 端點 */
@RestController
@RequestMapping("/api/products")
@Tag(name = "產品管理", description = "提供產品的查詢、更新和刪除功能，包括產品資訊管理、庫存狀態查詢等操作")
public class ProductController {

    private final ProductApplicationService productApplicationService;

    public ProductController(ProductApplicationService productApplicationService) {
        this.productApplicationService = productApplicationService;
    }

    /** 獲取產品列表 */
    @GetMapping
    @Operation(summary = "獲取產品列表", description = "分頁查詢產品列表，支援自訂頁碼和每頁數量。回傳產品基本資訊、價格、庫存狀態等資料。")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "成功獲取產品列表",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        example =
                                                                "{\"success\": true, \"data\":"
                                                                    + " {\"content\": [{\"id\":"
                                                                    + " \"PROD-001\", \"name\":"
                                                                    + " \"產品名稱\", \"description\":"
                                                                    + " \"產品描述\", \"price\":"
                                                                    + " {\"amount\": 100.00,"
                                                                    + " \"currency\": \"TWD\"},"
                                                                    + " \"category\": \"電子產品\","
                                                                    + " \"inStock\": true,"
                                                                    + " \"stockQuantity\": 50}],"
                                                                    + " \"totalElements\": 100,"
                                                                    + " \"totalPages\": 5,"
                                                                    + " \"size\": 20, \"number\":"
                                                                    + " 0, \"first\": true,"
                                                                    + " \"last\": false}}"))),
                @ApiResponse(
                        responseCode = "400",
                        description = "請求參數無效",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "500",
                        description = "系統內部錯誤",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class)))
            })
    public ResponseEntity<Map<String, Object>> getProducts(
            @Parameter(description = "頁碼，從0開始", example = "0") @RequestParam(defaultValue = "0")
                    int page,
            @Parameter(description = "每頁數量，最大100", example = "20")
                    @RequestParam(defaultValue = "20")
                    int size) {

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

    /** 獲取單個產品 */
    @GetMapping("/{productId}")
    @Operation(summary = "獲取單個產品", description = "根據產品ID獲取產品詳細資訊，包括產品名稱、描述、價格、分類、庫存狀態等完整資料。")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "成功獲取產品資訊",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        example =
                                                                "{\"success\": true, \"data\":"
                                                                    + " {\"id\": \"PROD-001\","
                                                                    + " \"name\": \"產品名稱\","
                                                                    + " \"description\": \"產品描述\","
                                                                    + " \"price\": {\"amount\":"
                                                                    + " 100.00, \"currency\":"
                                                                    + " \"TWD\"}, \"category\":"
                                                                    + " \"電子產品\", \"inStock\":"
                                                                    + " true, \"stockQuantity\":"
                                                                    + " 50}}"))),
                @ApiResponse(
                        responseCode = "404",
                        description = "產品不存在",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "500",
                        description = "系統內部錯誤",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class)))
            })
    public ResponseEntity<Map<String, Object>> getProduct(
            @Parameter(description = "產品唯一識別碼", required = true, example = "PROD-001") @PathVariable
                    String productId) {
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

    /** 更新產品 */
    @PutMapping("/{productId}")
    @Operation(
            summary = "更新產品資訊",
            description = "更新指定產品的基本資訊，包括產品名稱、描述、價格、貨幣和分類。所有欄位都是可選的，只更新提供的欄位。")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "產品更新成功",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        example =
                                                                "{\"success\": true, \"data\":"
                                                                    + " {\"id\": \"PROD-001\","
                                                                    + " \"name\": \"產品名稱\","
                                                                    + " \"description\": \"產品描述\","
                                                                    + " \"price\": {\"amount\":"
                                                                    + " 100.00, \"currency\":"
                                                                    + " \"TWD\"}, \"category\":"
                                                                    + " \"電子產品\", \"inStock\":"
                                                                    + " true, \"stockQuantity\":"
                                                                    + " 50}}"))),
                @ApiResponse(
                        responseCode = "400",
                        description = "請求參數無效",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "404",
                        description = "產品不存在",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "422",
                        description = "業務規則驗證失敗",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "500",
                        description = "系統內部錯誤",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class)))
            })
    public ResponseEntity<Map<String, Object>> updateProduct(
            @Parameter(description = "產品唯一識別碼", required = true, example = "PROD-001") @PathVariable
                    String productId,
            @Parameter(description = "產品更新請求資料", required = true) @RequestBody
                    solid.humank.genaidemo.interfaces.web.product.dto.UpdateProductRequest
                            request) {

        Map<String, Object> response = new HashMap<>();

        try {
            solid.humank.genaidemo.application.product.dto.command.UpdateProductCommand command =
                    solid.humank.genaidemo.application.product.dto.command.UpdateProductCommand.of(
                            productId,
                            request.getName(),
                            request.getDescription(),
                            request.getPrice(),
                            request.getCurrency(),
                            request.getCategory());

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

    /** 刪除產品 */
    @DeleteMapping("/{productId}")
    @Operation(summary = "刪除產品", description = "根據產品ID刪除指定產品。此操作不可逆，請謹慎使用。刪除前會檢查產品是否存在相關訂單。")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "產品刪除成功",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        example =
                                                                "{\"success\": true, \"message\":"
                                                                        + " \"產品已成功刪除\"}"))),
                @ApiResponse(
                        responseCode = "404",
                        description = "產品不存在",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "422",
                        description = "產品無法刪除（存在相關訂單）",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "500",
                        description = "系統內部錯誤",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class)))
            })
    public ResponseEntity<Map<String, Object>> deleteProduct(
            @Parameter(description = "產品唯一識別碼", required = true, example = "PROD-001") @PathVariable
                    String productId) {
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
