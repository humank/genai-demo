package solid.humank.genaidemo.agents.common;

import java.util.List;
import java.util.Map;

/**
 * Agent 回應
 * 
 * 封裝 Agent 處理結果，包括回應訊息、執行的動作和元資料。
 */
public record AgentResponse(
    String sessionId,
    String message,
    List<AgentAction> actions,
    AgentMetadata metadata
) {
    
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Agent 執行的動作
     */
    public record AgentAction(
        String type,
        Map<String, Object> data
    ) {
        public static AgentAction showOrder(Map<String, Object> orderData) {
            return new AgentAction("show_order", orderData);
        }
        
        public static AgentAction confirmRefund(Map<String, Object> refundData) {
            return new AgentAction("confirm_refund", refundData);
        }
        
        public static AgentAction escalate(String reason) {
            return new AgentAction("escalate", Map.of("reason", reason));
        }
    }
    
    /**
     * Agent 回應元資料
     */
    public record AgentMetadata(
        String conversationId,
        int turnCount,
        String detectedIntent,
        String language,
        long processingTimeMs
    ) {}
    
    public static class Builder {
        private String sessionId;
        private String message;
        private List<AgentAction> actions = List.of();
        private AgentMetadata metadata;
        
        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public Builder actions(List<AgentAction> actions) {
            this.actions = actions;
            return this;
        }
        
        public Builder metadata(AgentMetadata metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public AgentResponse build() {
            return new AgentResponse(sessionId, message, actions, metadata);
        }
    }
}
