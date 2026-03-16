package solid.humank.genaidemo.agents.customerservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import solid.humank.genaidemo.agents.common.AgentContext;
import solid.humank.genaidemo.agents.common.AgentResponse;
import solid.humank.genaidemo.agents.common.AgentResponse.AgentAction;
import solid.humank.genaidemo.agents.common.AgentResponse.AgentMetadata;
import solid.humank.genaidemo.agents.common.ToolRegistry;
import solid.humank.genaidemo.agents.common.ToolResult;
import solid.humank.genaidemo.agents.customerservice.client.AgentCoreClient;
import solid.humank.genaidemo.agents.customerservice.client.AgentCoreClient.AgentCoreRequest;
import solid.humank.genaidemo.agents.customerservice.client.AgentCoreClient.AgentCoreResponse;
import solid.humank.genaidemo.agents.customerservice.client.AgentCoreClient.ConversationMessage;
import solid.humank.genaidemo.agents.customerservice.client.AgentCoreClient.ToolCall;
import solid.humank.genaidemo.agents.customerservice.memory.AgentMemoryService;
import solid.humank.genaidemo.agents.customerservice.memory.ConversationMemory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 智能客服 Agent 服務
 * 
 * 核心 Agent 服務，協調 LLM、Tools 和 Memory 來處理客戶請求。
 */
@Service
public class CustomerServiceAgentService {
    
    private static final Logger log = LoggerFactory.getLogger(CustomerServiceAgentService.class);
    
    private final AgentCoreClient agentCoreClient;
    private final AgentMemoryService memoryService;
    private final ToolRegistry toolRegistry;
    
    private static final String SYSTEM_PROMPT = """
        你是電商平台的智能客服助手「小幫手」。
        
        ## 你的職責
        1. 幫助客戶查詢訂單狀態和物流資訊
        2. 處理退款和退貨請求
        3. 解答產品相關問題
        4. 收集客戶反饋和投訴
        
        ## 溝通原則
        - 使用友善、專業的語氣
        - 回答簡潔明瞭，避免冗長
        - 主動提供相關資訊
        - 使用繁體中文回應
        
        ## 工具使用指南
        - 查詢訂單時，使用 get_order_status 工具
        - 如果客戶沒有提供訂單編號，請先詢問
        
        ## 限制
        - 不要編造訂單或產品資訊，必須使用工具查詢
        - 不要承諾無法確認的事項
        - 如果無法解決問題，主動提供人工客服選項
        """;
    
    public CustomerServiceAgentService(
            AgentCoreClient agentCoreClient,
            AgentMemoryService memoryService,
            ToolRegistry toolRegistry) {
        this.agentCoreClient = agentCoreClient;
        this.memoryService = memoryService;
        this.toolRegistry = toolRegistry;
    }
    
    /**
     * 處理客戶請求
     */
    public AgentResponse process(AgentContext context) {
        long startTime = System.currentTimeMillis();
        
        log.info("Processing request for session: {}", context.sessionId());
        
        // 1. 載入對話記憶
        ConversationMemory memory = memoryService.loadMemory(context.sessionId());
        
        // 2. 構建對話歷史
        List<ConversationMessage> conversationHistory = buildConversationHistory(memory);
        
        // 3. 構建 Agent 請求
        AgentCoreRequest request = AgentCoreRequest.builder()
            .systemPrompt(SYSTEM_PROMPT)
            .userMessage(context.message())
            .conversationHistory(conversationHistory)
            .toolDefinitions(toolRegistry.generateToolDefinitions())
            .build();
        
        // 4. 調用 AgentCore
        AgentCoreResponse coreResponse = agentCoreClient.invoke(request);
        
        // 5. 執行 Tool 調用（如果有）
        String finalMessage = coreResponse.message();
        List<AgentAction> actions = new ArrayList<>();
        
        if (coreResponse.hasToolCalls()) {
            ToolExecutionResult toolResult = executeToolCalls(coreResponse.toolCalls());
            finalMessage = toolResult.message();
            actions.addAll(toolResult.actions());
        }
        
        // 6. 更新記憶
        memoryService.updateMemory(context.sessionId(), context.message(), finalMessage);
        
        // 7. 構建回應
        long processingTime = System.currentTimeMillis() - startTime;
        
        return AgentResponse.builder()
            .sessionId(context.sessionId())
            .message(finalMessage)
            .actions(actions)
            .metadata(new AgentMetadata(
                context.sessionId(),
                memory.getTurnCount() + 1,
                detectIntent(context.message()),
                context.language(),
                processingTime
            ))
            .build();
    }
    
    private List<ConversationMessage> buildConversationHistory(ConversationMemory memory) {
        return memory.getHistory().stream()
            .map(msg -> new ConversationMessage(
                msg.role().name().toLowerCase(),
                msg.content()
            ))
            .toList();
    }
    
    private ToolExecutionResult executeToolCalls(List<ToolCall> toolCalls) {
        StringBuilder messageBuilder = new StringBuilder();
        List<AgentAction> actions = new ArrayList<>();
        
        for (ToolCall toolCall : toolCalls) {
            log.info("Executing tool: {} with arguments: {}", toolCall.toolName(), toolCall.arguments());
            
            ToolResult result = toolRegistry.executeTool(toolCall.toolName(), toolCall.arguments());
            
            if (result.success()) {
                messageBuilder.append(result.message());
                
                // 根據工具類型添加對應的 Action
                if ("get_order_status".equals(toolCall.toolName())) {
                    actions.add(AgentAction.showOrder(result.data()));
                }
            } else {
                messageBuilder.append(result.message());
            }
        }
        
        return new ToolExecutionResult(messageBuilder.toString(), actions);
    }
    
    private String detectIntent(String message) {
        String lowerMessage = message.toLowerCase();
        
        if (lowerMessage.contains("訂單") || lowerMessage.contains("order")) {
            return "order_query";
        }
        if (lowerMessage.contains("退款") || lowerMessage.contains("refund")) {
            return "refund_request";
        }
        if (lowerMessage.contains("產品") || lowerMessage.contains("product")) {
            return "product_inquiry";
        }
        if (lowerMessage.contains("投訴") || lowerMessage.contains("complaint")) {
            return "complaint";
        }
        
        return "general";
    }
    
    private record ToolExecutionResult(String message, List<AgentAction> actions) {}
}
