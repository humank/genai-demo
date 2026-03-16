package solid.humank.genaidemo.agents.common;

import java.util.Map;

/**
 * Agent 執行上下文
 * 
 * 封裝 Agent 處理請求所需的所有上下文資訊。
 */
public record AgentContext(
    String sessionId,
    String customerId,
    String message,
    String language,
    Map<String, Object> metadata
) {
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String sessionId;
        private String customerId;
        private String message;
        private String language = "zh-TW";
        private Map<String, Object> metadata = Map.of();
        
        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }
        
        public Builder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public Builder language(String language) {
            this.language = language;
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public AgentContext build() {
            return new AgentContext(sessionId, customerId, message, language, metadata);
        }
    }
}
