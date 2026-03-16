package solid.humank.genaidemo.agents.customerservice.client;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mock AgentCore 客戶端
 * 
 * 用於開發和測試環境的 Mock 實作。
 * 模擬 LLM 的意圖識別和 Tool 調用行為。
 */
@Component
@Profile({"local", "test"})
public class MockAgentCoreClient implements AgentCoreClient {
    
    private static final Pattern ORDER_ID_PATTERN = Pattern.compile("(?:訂單|order)[\\s#:：]*([A-Za-z0-9-]+)", Pattern.CASE_INSENSITIVE);
    
    @Override
    public AgentCoreResponse invoke(AgentCoreRequest request) {
        String userMessage = request.userMessage().toLowerCase();
        
        // 模擬意圖識別
        if (containsOrderQuery(userMessage)) {
            String orderId = extractOrderId(request.userMessage());
            if (orderId != null) {
                // 模擬 Tool 調用
                return new AgentCoreResponse(
                    null,
                    List.of(new ToolCall("get_order_status", Map.of("order_id", orderId))),
                    "tool_use"
                );
            } else {
                return new AgentCoreResponse(
                    "請問您要查詢哪一筆訂單呢？請提供訂單編號，我可以幫您查詢訂單狀態。",
                    List.of(),
                    "end_turn"
                );
            }
        }
        
        if (containsRefundQuery(userMessage)) {
            return new AgentCoreResponse(
                "關於退款申請，請問您要退款的訂單編號是什麼？我需要先查詢訂單狀態，確認是否符合退款條件。",
                List.of(),
                "end_turn"
            );
        }
        
        if (containsHumanRequest(userMessage)) {
            return new AgentCoreResponse(
                "好的，我將為您轉接人工客服。請稍候，客服人員將盡快為您服務。",
                List.of(),
                "end_turn"
            );
        }
        
        // 預設回應
        return new AgentCoreResponse(
            "您好！我是智能客服小幫手，很高興為您服務。我可以幫您：\n" +
            "1. 查詢訂單狀態\n" +
            "2. 處理退款申請\n" +
            "3. 解答產品問題\n\n" +
            "請問有什麼可以幫您的嗎？",
            List.of(),
            "end_turn"
        );
    }
    
    private boolean containsOrderQuery(String message) {
        return message.contains("訂單") || message.contains("order") ||
               message.contains("查詢") || message.contains("到哪") ||
               message.contains("狀態") || message.contains("status");
    }
    
    private boolean containsRefundQuery(String message) {
        return message.contains("退款") || message.contains("refund") ||
               message.contains("退貨") || message.contains("return");
    }
    
    private boolean containsHumanRequest(String message) {
        return message.contains("人工") || message.contains("客服") ||
               message.contains("真人") || message.contains("human");
    }
    
    private String extractOrderId(String message) {
        Matcher matcher = ORDER_ID_PATTERN.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // 嘗試找任何看起來像 ID 的字串
        Pattern idPattern = Pattern.compile("([A-Za-z0-9]{8,})");
        matcher = idPattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
}
