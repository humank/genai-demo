package solid.humank.genaidemo.agents.customerservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import solid.humank.genaidemo.agents.common.AgentContext;
import solid.humank.genaidemo.agents.common.AgentResponse;
import solid.humank.genaidemo.agents.common.ToolRegistry;
import solid.humank.genaidemo.agents.customerservice.client.AgentCoreClient;
import solid.humank.genaidemo.agents.customerservice.client.AgentCoreClient.AgentCoreResponse;
import solid.humank.genaidemo.agents.customerservice.memory.AgentMemoryService;
import solid.humank.genaidemo.agents.customerservice.memory.ConversationMemory;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 智能客服 Agent 服務單元測試
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerServiceAgentService 單元測試")
class CustomerServiceAgentServiceTest {

    @Mock
    private AgentCoreClient agentCoreClient;

    @Mock
    private AgentMemoryService memoryService;

    @Mock
    private ToolRegistry toolRegistry;

    private CustomerServiceAgentService agentService;

    @BeforeEach
    void setUp() {
        agentService = new CustomerServiceAgentService(
            agentCoreClient,
            memoryService,
            toolRegistry
        );
    }

    @Test
    @DisplayName("應該成功處理用戶訊息並返回回應")
    void should_process_user_message_and_return_response() {
        // Given
        String sessionId = "test-session-123";
        String userMessage = "我想查詢訂單狀態";
        String expectedResponse = "好的，請提供您的訂單編號，我來幫您查詢。";

        AgentContext context = new AgentContext(
            sessionId,
            "customer-001",
            userMessage,
            "zh-TW",
            Map.of()
        );

        ConversationMemory memory = new ConversationMemory(sessionId);
        when(memoryService.loadMemory(sessionId)).thenReturn(memory);
        when(toolRegistry.generateToolDefinitions()).thenReturn("[]");
        when(agentCoreClient.invoke(any()))
            .thenReturn(new AgentCoreResponse(expectedResponse, Collections.emptyList(), "end_turn"));

        // When
        AgentResponse response = agentService.process(context);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.message()).isEqualTo(expectedResponse);
        assertThat(response.sessionId()).isEqualTo(sessionId);
    }

    @Test
    @DisplayName("應該正確識別訂單查詢意圖")
    void should_detect_order_query_intent() {
        // Given
        String sessionId = "test-session-456";
        String userMessage = "我的訂單到哪了？";

        AgentContext context = new AgentContext(
            sessionId,
            "customer-002",
            userMessage,
            "zh-TW",
            Map.of()
        );

        ConversationMemory memory = new ConversationMemory(sessionId);
        when(memoryService.loadMemory(sessionId)).thenReturn(memory);
        when(toolRegistry.generateToolDefinitions()).thenReturn("[]");
        when(agentCoreClient.invoke(any()))
            .thenReturn(new AgentCoreResponse("正在查詢您的訂單...", Collections.emptyList(), "end_turn"));

        // When
        AgentResponse response = agentService.process(context);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.metadata().detectedIntent()).isEqualTo("order_query");
    }

    @Test
    @DisplayName("應該更新對話記憶")
    void should_update_conversation_memory() {
        // Given
        String sessionId = "test-session-789";
        String userMessage = "查詢訂單";
        String agentResponse = "請提供訂單編號";

        AgentContext context = new AgentContext(
            sessionId,
            "customer-003",
            userMessage,
            "zh-TW",
            Map.of()
        );

        ConversationMemory memory = new ConversationMemory(sessionId);
        when(memoryService.loadMemory(sessionId)).thenReturn(memory);
        when(toolRegistry.generateToolDefinitions()).thenReturn("[]");
        when(agentCoreClient.invoke(any()))
            .thenReturn(new AgentCoreResponse(agentResponse, Collections.emptyList(), "end_turn"));

        // When
        agentService.process(context);

        // Then
        verify(memoryService).updateMemory(sessionId, userMessage, agentResponse);
    }
}
