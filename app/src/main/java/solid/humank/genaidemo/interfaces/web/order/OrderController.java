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

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
    
    @Autowired
    private DataSource dataSource;

    public OrderController(OrderManagementUseCase orderService) {
        this.orderService = orderService;
    }

    /**
     * 獲取訂單列表
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> orders = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection()) {
            // 計算總數
            int totalElements = 0;
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM orders")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    totalElements = rs.getInt(1);
                }
            }
            
            // 獲取分頁數據
            String sql = "SELECT id, customer_id, shipping_address, status, total_amount, created_at " +
                        "FROM orders ORDER BY created_at DESC LIMIT ? OFFSET ?";
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, size);
                ps.setInt(2, page * size);
                ResultSet rs = ps.executeQuery();
                
                while (rs.next()) {
                    Map<String, Object> order = new HashMap<>();
                    String orderId = rs.getString("id");
                    double totalAmount = rs.getDouble("total_amount");
                    
                    order.put("id", orderId);
                    order.put("customerId", rs.getString("customer_id"));
                    order.put("shippingAddress", rs.getString("shipping_address"));
                    order.put("status", rs.getString("status"));
                    order.put("createdAt", rs.getString("created_at"));
                    
                    // 格式化金額為 Money 對象
                    Map<String, Object> totalAmountMoney = new HashMap<>();
                    totalAmountMoney.put("amount", totalAmount);
                    totalAmountMoney.put("currency", "TWD");
                    order.put("totalAmount", totalAmountMoney);
                    
                    // 添加 effectiveAmount（暫時與 totalAmount 相同）
                    Map<String, Object> effectiveAmountMoney = new HashMap<>();
                    effectiveAmountMoney.put("amount", totalAmount);
                    effectiveAmountMoney.put("currency", "TWD");
                    order.put("effectiveAmount", effectiveAmountMoney);
                    
                    // 獲取訂單項目
                    List<Map<String, Object>> items = new ArrayList<>();
                    String itemSql = "SELECT oi.product_id, oi.product_name, oi.quantity, oi.price, oi.currency " +
                                   "FROM order_items oi " +
                                   "WHERE oi.order_id = ?";
                    
                    try (PreparedStatement itemPs = conn.prepareStatement(itemSql)) {
                        itemPs.setString(1, orderId);
                        ResultSet itemRs = itemPs.executeQuery();
                        
                        while (itemRs.next()) {
                            Map<String, Object> item = new HashMap<>();
                            item.put("productId", itemRs.getString("product_id"));
                            item.put("productName", itemRs.getString("product_name") != null ? 
                                    itemRs.getString("product_name") : "未知商品");
                            item.put("quantity", itemRs.getInt("quantity"));
                            
                            double price = itemRs.getDouble("price");
                            int quantity = itemRs.getInt("quantity");
                            String currency = itemRs.getString("currency");
                            
                            // 格式化單價為 Money 對象
                            Map<String, Object> unitPrice = new HashMap<>();
                            unitPrice.put("amount", price);
                            unitPrice.put("currency", currency != null ? currency : "TWD");
                            item.put("unitPrice", unitPrice);
                            
                            // 格式化總價為 Money 對象（單價 * 數量）
                            Map<String, Object> totalPrice = new HashMap<>();
                            totalPrice.put("amount", price * quantity);
                            totalPrice.put("currency", currency != null ? currency : "TWD");
                            item.put("totalPrice", totalPrice);
                            
                            items.add(item);
                        }
                    }
                    
                    order.put("items", items);
                    orders.add(order);
                }
            }
            
            // 構建分頁響應
            int totalPages = (int) Math.ceil((double) totalElements / size);
            
            Map<String, Object> pageInfo = new HashMap<>();
            pageInfo.put("content", orders);
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
            response.put("message", "獲取訂單列表時發生錯誤: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 創建訂單
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        // 創建命令對象
        CreateOrderCommand command = CreateOrderCommand.of(
            request.getCustomerId(),
            request.getShippingAddress()
        );
        
        solid.humank.genaidemo.application.order.dto.response.OrderResponse appResponse = orderService.createOrder(command);
        // 將應用層響應轉換為介面層響應
        OrderResponse webResponse = ResponseFactory.toWebResponse(appResponse);
        return new ResponseEntity<>(webResponse, HttpStatus.CREATED);
    }

    /**
     * 添加訂單項目
     */
    @PostMapping("/{orderId}/items")
    public ResponseEntity<OrderResponse> addOrderItem(
            @PathVariable String orderId,
            @RequestBody AddOrderItemRequest request) {
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
        return ResponseEntity.ok(webResponse);
    }

    /**
     * 提交訂單
     */
    @PostMapping("/{orderId}/submit")
    public ResponseEntity<OrderResponse> submitOrder(@PathVariable String orderId) {
        solid.humank.genaidemo.application.order.dto.response.OrderResponse appResponse = orderService.submitOrder(orderId);
        // 將應用層響應轉換為介面層響應
        OrderResponse webResponse = ResponseFactory.toWebResponse(appResponse);
        return ResponseEntity.ok(webResponse);
    }

    /**
     * 取消訂單
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable String orderId) {
        solid.humank.genaidemo.application.order.dto.response.OrderResponse appResponse = orderService.cancelOrder(orderId);
        // 將應用層響應轉換為介面層響應
        OrderResponse webResponse = ResponseFactory.toWebResponse(appResponse);
        return ResponseEntity.ok(webResponse);
    }

    /**
     * 獲取訂單
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        solid.humank.genaidemo.application.order.dto.response.OrderResponse appResponse = orderService.getOrder(orderId);
        // 將應用層響應轉換為介面層響應
        OrderResponse webResponse = ResponseFactory.toWebResponse(appResponse);
        return ResponseEntity.ok(webResponse);
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