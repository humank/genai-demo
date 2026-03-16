package solid.humank.genaidemo.agents.customerservice.client;

import java.util.List;

/**
 * AgentCore 客戶端介面
 * 
 * 定義與 AWS Bedrock AgentCore 互動的標準介面。
 */
public interface AgentCoreClient {
    
    /**
     * 調用 Agent
     * 
     * @param request Agent 請求
     * @return Agent 回應
     */
    AgentCoreResponse invoke(AgentCoreRequest request);
    
    /**
     * Agent 請求
     */
    record AgentCoreRequest(
        String systemPrompt,
        String userMessage,
        List<ConversationMessage> conversationHistory,
        String toolDefinitions
    ) {
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private String systemPrompt;
            private String userMessage;
            private List<ConversationMessage> conversationHistory = List.of();
            private String toolDefinitions;
            
            public Builder systemPrompt(String systemPrompt) {
                this.systemPrompt = systemPrompt;
                return this;
            }
            
            public Builder userMessage(String userMessage) {
                this.userMessage = userMessage;
                return this;
            }
            
            public Builder conversationHistory(List<ConversationMessage> history) {
                this.conversationHistory = history;
                return this;
            }
            
            public Builder toolDefinitions(String toolDefinitions) {
                this.toolDefinitions = toolDefinitions;
                return this;
            }
            
            public AgentCoreRequest build() {
                return new AgentCoreRequest(systemPrompt, userMessage, conversationHistory, toolDefinitions);
            }
        }
    }
    
    /**
     * Agent 回應
     */
    record AgentCoreResponse(
        String message,
        List<ToolCall> toolCalls,
        String stopReason
    ) {
        public boolean hasToolCalls() {
            return toolCalls != null && !toolCalls.isEmpty();
        }
    }
    
    /**
     * 對話訊息
     */
    record ConversationMessage(String role, String content) {}
    
    /**
     * Tool 調用
     */
    record ToolCall(String toolName, java.util.Map<String, Object> arguments) {}
}
