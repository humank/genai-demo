package solid.humank.genaidemo.agents.customerservice.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import solid.humank.genaidemo.agents.common.AgentContext;
import solid.humank.genaidemo.agents.common.AgentResponse;
import solid.humank.genaidemo.agents.customerservice.service.CustomerServiceAgentService;

import java.util.Map;

/**
 * 智能客服 Agent REST API Controller
 * 
 * 提供客服 Agent 的 HTTP 端點。
 */
@RestController
@RequestMapping("/api/v1/agents/customer-service")
public class CustomerServiceAgentController {
    
    private static final Logger log = LoggerFactory.getLogger(CustomerServiceAgentController.class);
    
    private final CustomerServiceAgentService agentService;
    
    public CustomerServiceAgentController(CustomerServiceAgentService agentService) {
        this.agentService = agentService;
    }
    
    /**
     * 發送訊息給客服 Agent
     */
    @PostMapping("/chat")
    public ResponseEntity<AgentResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Received chat request for session: {}", request.sessionId());
        
        AgentContext context = AgentContext.builder()
            .sessionId(request.sessionId())
            .customerId(request.customerId())
            .message(request.message())
            .language(request.language() != null ? request.language() : "zh-TW")
            .metadata(request.metadata() != null ? request.metadata() : Map.of())
            .build();
        
        AgentResponse response = agentService.process(context);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 健康檢查端點
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse("healthy", "Customer Service Agent is running"));
    }
    
    /**
     * 聊天請求 DTO
     */
    public record ChatRequest(
        @NotBlank(message = "Session ID is required")
        String sessionId,
        
        String customerId,
        
        @NotBlank(message = "Message is required")
        String message,
        
        String language,
        
        Map<String, Object> metadata
    ) {}
    
    /**
     * 健康檢查回應 DTO
     */
    public record HealthResponse(String status, String message) {}
}
