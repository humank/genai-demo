package solid.humank.genaidemo.interfaces.web.order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import solid.humank.genaidemo.application.order.dto.AddOrderItemCommand;
import solid.humank.genaidemo.application.order.dto.CreateOrderCommand;
import solid.humank.genaidemo.application.order.port.incoming.OrderManagementUseCase;
import solid.humank.genaidemo.interfaces.web.order.dto.AddOrderItemRequest;
import solid.humank.genaidemo.interfaces.web.order.dto.CreateOrderRequest;
import solid.humank.genaidemo.interfaces.web.order.dto.OrderResponse;
import solid.humank.genaidemo.application.common.dto.PagedResult;

import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 訂單控制器
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderManagementUseCase orderService;

    public OrderController(OrderManagementUseCase orderService) {
        this.orderService = orderService;
    }

    /**
     * 獲取訂單列表 - 符合六角形架構原則
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            // 通過應用服務獲取分頁數據
            PagedResult<solid.humank.genaidemo.application.order.dto.response.OrderResponse> pagedResult = 
                orderService.getOrders(page, size);
            
            // 轉換為介面層響應格式
            Map<String, Object> response = new HashMap<>();
            Map<String, Object> pageInfo = new HashMap<>();
            
            // 轉換訂單數據
            List<OrderResponse> webOrders = pagedResult.getContent().stream()
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

    /**
     * 創建訂單
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody CreateOrderRequest request) {
        try {
            // 創建命令對象
            CreateOrderCommand command = CreateOrderCommand.of(
                request.getCustomerId(),
                request.getShippingAddress()
            );
            
            solid.humank.genaidemo.application.order.dto.response.OrderResponse appResponse = orderService.createOrder(command);
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

    /**
     * 添加訂單項目
     */
    @PostMapping("/{orderId}/items")
    public ResponseEntity<Map<String, Object>> addOrderItem(
            @PathVariable String orderId,
            @RequestBody AddOrderItemRequest request) {
        try {
            // 創建命令對象
            AddOrderItemCommand command = AddOrderItemCommand.of(
                orderId,
                request.getProductId(),
                request.getProductName(),
                request.getQuantity(),
                request.getPrice().getAmount()
            );
            
            solid.humank.genaidemo.application.order.dto.response.OrderResponse appResponse = orderService.addOrderItem(command);
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

    /**
     * 提交訂單
     */
    @PostMapping("/{orderId}/submit")
    public ResponseEntity<Map<String, Object>> submitOrder(@PathVariable String orderId) {
        try {
            solid.humank.genaidemo.application.order.dto.response.OrderResponse appResponse = orderService.submitOrder(orderId);
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

    /**
     * 取消訂單
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelOrder(@PathVariable String orderId) {
        try {
            solid.humank.genaidemo.application.order.dto.response.OrderResponse appResponse = orderService.cancelOrder(orderId);
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

    /**
     * 獲取訂單
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable String orderId) {
        try {
            solid.humank.genaidemo.application.order.dto.response.OrderResponse appResponse = orderService.getOrder(orderId);
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

    /**
     * 處理異常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleException(RuntimeException ex) {
        if (ex.getMessage().contains("not found")) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}