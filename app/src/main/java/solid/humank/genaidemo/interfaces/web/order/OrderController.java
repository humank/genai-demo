package solid.humank.genaidemo.interfaces.web.order;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import solid.humank.genaidemo.application.common.dto.PagedResult;
import solid.humank.genaidemo.application.common.dto.StandardErrorResponse;
import solid.humank.genaidemo.application.order.dto.AddOrderItemCommand;
import solid.humank.genaidemo.application.order.dto.CreateOrderCommand;
import solid.humank.genaidemo.application.order.port.incoming.OrderManagementUseCase;
import solid.humank.genaidemo.interfaces.web.order.dto.AddOrderItemRequest;
import solid.humank.genaidemo.interfaces.web.order.dto.CreateOrderRequest;
import solid.humank.genaidemo.interfaces.web.order.dto.OrderResponse;

/** 訂單控制器 */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "訂單管理", description = "訂單相關的 API 操作，包括創建、查詢、修改和取消訂單")
public class OrderController {

    private final OrderManagementUseCase orderService;

    public OrderController(OrderManagementUseCase orderService) {
        this.orderService = orderService;
    }

    /** 獲取訂單列表 - 符合六角形架構原則 */
    @Operation(summary = "獲取訂單列表", description = "分頁獲取訂單列表，支援分頁參數設定")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "成功獲取訂單列表",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = Map.class))),
                @ApiResponse(
                        responseCode = "500",
                        description = "內部伺服器錯誤",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class)))
            })
    @GetMapping
    public ResponseEntity<Map<String, Object>> getOrders(
            @Parameter(description = "頁碼，從0開始", example = "0") @RequestParam(defaultValue = "0")
                    int page,
            @Parameter(description = "每頁大小", example = "20") @RequestParam(defaultValue = "20")
                    int size) {

        try {
            // 通過應用服務獲取分頁數據
            PagedResult<solid.humank.genaidemo.application.order.dto.response.OrderResponse>
                    pagedResult = orderService.getOrders(page, size);

            // 轉換為介面層響應格式
            Map<String, Object> response = new HashMap<>();
            Map<String, Object> pageInfo = new HashMap<>();

            // 轉換訂單數據
            List<OrderResponse> webOrders =
                    pagedResult.getContent().stream()
                            .map(ResponseFactory::toWebResponse)
                            .collect(Collectors.toList());

            pageInfo.put("content", webOrders);
            pageInfo.put("totalElements", pagedResult.getTotalElements());
            pageInfo.put("totalPages", pagedResult.getTotalPages());
            pageInfo.put("size", pagedResult.getSize());
            pageInfo.put("number", pagedResult.getNumber());
            pageInfo.put("first", pagedResult.isFirst());
            pageInfo.put("last", pagedResult.isLast());

            response.put("success", true);
            response.put("data", pageInfo);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "獲取訂單列表時發生錯誤: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /** 創建訂單 */
    @Operation(summary = "創建新訂單", description = "根據客戶ID和配送地址創建新的訂單")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "201",
                        description = "訂單創建成功",
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
                        description = "內部伺服器錯誤",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class)))
            })
    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(
            @Parameter(description = "創建訂單請求", required = true) @RequestBody
                    CreateOrderRequest request) {
        try {
            // 創建命令對象
            CreateOrderCommand command =
                    CreateOrderCommand.of(request.getCustomerId(), request.getShippingAddress());

            solid.humank.genaidemo.application.order.dto.response.OrderResponse appResponse =
                    orderService.createOrder(command);
            // 將應用層響應轉換為介面層響應
            OrderResponse webResponse = ResponseFactory.toWebResponse(appResponse);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", webResponse);

            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "創建訂單時發生錯誤: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /** 添加訂單項目 */
    @Operation(summary = "添加訂單項目", description = "向指定訂單添加商品項目")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "成功添加訂單項目",
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
                        responseCode = "404",
                        description = "訂單不存在",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "500",
                        description = "內部伺服器錯誤",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class)))
            })
    @PostMapping("/{orderId}/items")
    public ResponseEntity<Map<String, Object>> addOrderItem(
            @Parameter(description = "訂單ID", required = true, example = "order-123") @PathVariable
                    String orderId,
            @Parameter(description = "添加訂單項目請求", required = true) @RequestBody
                    AddOrderItemRequest request) {
        try {
            // 創建命令對象
            AddOrderItemCommand command =
                    AddOrderItemCommand.of(
                            orderId,
                            request.getProductId(),
                            request.getProductName(),
                            request.getQuantity(),
                            request.getPrice().getAmount());

            solid.humank.genaidemo.application.order.dto.response.OrderResponse appResponse =
                    orderService.addOrderItem(command);
            // 將應用層響應轉換為介面層響應
            OrderResponse webResponse = ResponseFactory.toWebResponse(appResponse);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", webResponse);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "添加訂單項目時發生錯誤: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /** 提交訂單 */
    @Operation(summary = "提交訂單", description = "將訂單狀態變更為已提交，準備進行後續處理")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "訂單提交成功",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = Map.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "訂單狀態不允許提交",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "404",
                        description = "訂單不存在",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "500",
                        description = "內部伺服器錯誤",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class)))
            })
    @PostMapping("/{orderId}/submit")
    public ResponseEntity<Map<String, Object>> submitOrder(
            @Parameter(description = "訂單ID", required = true, example = "order-123") @PathVariable
                    String orderId) {
        try {
            solid.humank.genaidemo.application.order.dto.response.OrderResponse appResponse =
                    orderService.submitOrder(orderId);
            // 將應用層響應轉換為介面層響應
            OrderResponse webResponse = ResponseFactory.toWebResponse(appResponse);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", webResponse);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "提交訂單時發生錯誤: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /** 取消訂單 */
    @Operation(summary = "取消訂單", description = "取消指定的訂單，將訂單狀態變更為已取消")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "訂單取消成功",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = Map.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "訂單狀態不允許取消",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "404",
                        description = "訂單不存在",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "500",
                        description = "內部伺服器錯誤",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class)))
            })
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelOrder(
            @Parameter(description = "訂單ID", required = true, example = "order-123") @PathVariable
                    String orderId) {
        try {
            solid.humank.genaidemo.application.order.dto.response.OrderResponse appResponse =
                    orderService.cancelOrder(orderId);
            // 將應用層響應轉換為介面層響應
            OrderResponse webResponse = ResponseFactory.toWebResponse(appResponse);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", webResponse);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "取消訂單時發生錯誤: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /** 獲取訂單 */
    @Operation(summary = "獲取單一訂單", description = "根據訂單ID獲取訂單詳細資訊")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "成功獲取訂單資訊",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = Map.class))),
                @ApiResponse(
                        responseCode = "404",
                        description = "訂單不存在",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class))),
                @ApiResponse(
                        responseCode = "500",
                        description = "內部伺服器錯誤",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                StandardErrorResponse.class)))
            })
    @GetMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrder(
            @Parameter(description = "訂單ID", required = true, example = "order-123") @PathVariable
                    String orderId) {
        try {
            solid.humank.genaidemo.application.order.dto.response.OrderResponse appResponse =
                    orderService.getOrder(orderId);
            // 將應用層響應轉換為介面層響應
            OrderResponse webResponse = ResponseFactory.toWebResponse(appResponse);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", webResponse);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "獲取訂單時發生錯誤: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /** 處理異常 */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleException(RuntimeException ex) {
        if (ex.getMessage().contains("not found")) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
