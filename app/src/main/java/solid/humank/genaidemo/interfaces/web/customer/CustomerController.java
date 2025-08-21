package solid.humank.genaidemo.interfaces.web.customer;

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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import solid.humank.genaidemo.application.common.dto.StandardErrorResponse;
import solid.humank.genaidemo.application.customer.CustomerDto;
import solid.humank.genaidemo.application.customer.CustomerPageDto;
import solid.humank.genaidemo.application.customer.service.CustomerApplicationService;

/** 客戶控制器 提供客戶相關的 API 端點 */
@RestController
@RequestMapping("/api/customers")
@Tag(name = "客戶管理", description = "客戶相關的 API 操作，包括查詢客戶資訊和客戶列表。注意：客戶敏感資訊已進行適當的隱私保護處理")
public class CustomerController {

    private final CustomerApplicationService customerApplicationService;

    public CustomerController(CustomerApplicationService customerApplicationService) {
        this.customerApplicationService = customerApplicationService;
    }

    /** 獲取客戶列表 */
    @Operation(summary = "獲取客戶列表", description = "分頁獲取客戶列表，支援分頁參數設定。返回的客戶資訊已進行隱私保護處理，敏感資訊會被適當遮罩")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "成功獲取客戶列表",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = Map.class))),
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
    @GetMapping
    public ResponseEntity<Map<String, Object>> getCustomers(
            @Parameter(description = "頁碼，從 0 開始", example = "0") @RequestParam(defaultValue = "0")
                    int page,
            @Parameter(description = "每頁大小，最大 100", example = "20")
                    @RequestParam(defaultValue = "20")
                    int size) {

        Map<String, Object> response = new HashMap<>();

        try {
            CustomerPageDto customerPage = customerApplicationService.getCustomers(page, size);

            response.put("success", true);
            response.put("data", customerPage);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "獲取客戶列表時發生錯誤: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    /** 獲取單個客戶 */
    @Operation(summary = "獲取單個客戶資訊", description = "根據客戶ID獲取特定客戶的詳細資訊。返回的客戶資訊已進行隱私保護處理，敏感資訊會被適當遮罩")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "成功獲取客戶資訊",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = Map.class))),
                @ApiResponse(
                        responseCode = "404",
                        description = "客戶不存在",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "客戶ID格式無效",
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
    @GetMapping("/{customerId}")
    public ResponseEntity<Map<String, Object>> getCustomer(
            @Parameter(description = "客戶唯一識別碼", required = true, example = "cust-12345-abcde")
                    @PathVariable
                    String customerId) {
        Map<String, Object> response = new HashMap<>();

        try {
            Optional<CustomerDto> customer = customerApplicationService.getCustomer(customerId);

            if (customer.isPresent()) {
                response.put("success", true);
                response.put("data", customer.get());
            } else {
                response.put("success", false);
                response.put("message", "客戶不存在");
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "獲取客戶時發生錯誤: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }
}
