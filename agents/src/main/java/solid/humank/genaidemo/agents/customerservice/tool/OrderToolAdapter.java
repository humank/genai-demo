package solid.humank.genaidemo.agents.customerservice.tool;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.agents.common.AgentTool;
import solid.humank.genaidemo.agents.common.ToolResult;
import solid.humank.genaidemo.agents.common.ToolSchema;
import solid.humank.genaidemo.application.order.dto.response.OrderResponse;
import solid.humank.genaidemo.application.order.service.OrderApplicationService;

import java.util.Map;

/**
 * 訂單查詢 Tool Adapter
 * 
 * 將 Agent 的訂單查詢請求轉換為 OrderApplicationService 調用。
 */
@Component
public class OrderToolAdapter implements AgentTool {
    
    private final OrderApplicationService orderService;
    
    public OrderToolAdapter(OrderApplicationService orderService) {
        this.orderService = orderService;
    }
    
    @Override
    public String getName() {
        return "get_order_status";
    }
    
    @Override
    public String getDescription() {
        return "查詢訂單狀態，包括訂單詳情、商品清單、金額和目前狀態。" +
               "當客戶詢問訂單相關問題時使用此工具。";
    }
    
    @Override
    public ToolSchema getSchema() {
        return ToolSchema.builder()
            .addParameter("order_id", "string", "訂單編號", true)
            .build();
    }
    
    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String orderId = (String) arguments.get("order_id");
        
        if (orderId == null || orderId.isBlank()) {
            return ToolResult.error("請提供訂單編號");
        }
        
        try {
            OrderResponse order = orderService.getOrder(orderId);
            return ToolResult.success(
                formatOrderResponse(order),
                Map.of(
                    "orderId", order.getId(),
                    "status", order.getStatus(),
                    "totalAmount", order.getTotalAmount(),
                    "itemCount", order.getItems().size()
                )
            );
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ToolResult.error("找不到訂單編號: " + orderId + "，請確認訂單編號是否正確。");
            }
            return ToolResult.error("查詢訂單時發生錯誤: " + e.getMessage());
        }
    }
    
    private String formatOrderResponse(OrderResponse order) {
        StringBuilder sb = new StringBuilder();
        sb.append("訂單編號: ").append(order.getId()).append("\n");
        sb.append("訂單狀態: ").append(translateStatus(order.getStatus())).append("\n");
        sb.append("收件地址: ").append(order.getShippingAddress()).append("\n");
        sb.append("訂單金額: NT$ ").append(order.getTotalAmount()).append("\n");
        sb.append("商品數量: ").append(order.getItems().size()).append(" 項\n");
        
        if (!order.getItems().isEmpty()) {
            sb.append("\n商品清單:\n");
            order.getItems().forEach(item -> {
                sb.append("  - ").append(item.getProductName())
                  .append(" x ").append(item.getQuantity())
                  .append(" = NT$ ").append(item.getSubtotal())
                  .append("\n");
            });
        }
        
        return sb.toString();
    }
    
    private String translateStatus(String status) {
        return switch (status) {
            case "CREATED" -> "已建立";
            case "PENDING" -> "待處理";
            case "CONFIRMED" -> "已確認";
            case "SHIPPED" -> "已出貨";
            case "DELIVERED" -> "已送達";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }
}
