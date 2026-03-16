# Agent 系統整合指南

> **與現有電商平台的整合實作指南**

## 概述

本文檔提供 AI Agent 系統與現有電商平台整合的詳細技術指南，包括 API 整合、事件整合、安全配置等。

## 整合架構

### 整合層次

```
┌─────────────────────────────────────────────────────────────┐
│                    Integration Layer                         │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              AgentCore Gateway Adapter               │   │
│  │  - REST API 轉換                                     │   │
│  │  - WebSocket 管理                                    │   │
│  │  - 認證整合                                          │   │
│  └─────────────────────────────────────────────────────┘   │
│                            │                                 │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Tool Integration Layer                  │   │
│  │  - Application Service 包裝                          │   │
│  │  - 資料轉換                                          │   │
│  │  - 錯誤處理                                          │   │
│  └─────────────────────────────────────────────────────┘   │
│                            │                                 │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Event Integration Layer                 │   │
│  │  - Domain Event 監聽                                 │   │
│  │  - Agent Event 發布                                  │   │
│  │  - 事件轉換                                          │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## API 整合

### REST API 整合

#### Gateway 配置

```yaml
# agent-gateway-config.yaml
gateway:
  base_path: /api/v1/agents
  
  endpoints:
    customer-service:
      path: /customer-service
      methods: [POST]
      auth: jwt
      rate_limit:
        requests: 100
        period: 1m
      timeout: 30s
      
    order-processing:
      path: /order-processing
      methods: [POST]
      auth: api_key
      rate_limit:
        requests: 500
        period: 1m
      timeout: 60s
```

#### Spring Boot 整合

```java
// AgentGatewayController.java
@RestController
@RequestMapping("/api/v1/agents")
public class AgentGatewayController {
    
    private final AgentGatewayService agentGatewayService;
    
    @PostMapping("/customer-service")
    public ResponseEntity<AgentResponse> customerServiceAgent(
            @RequestBody AgentRequest request,
            @AuthenticationPrincipal UserDetails user) {
        
        AgentContext context = AgentContext.builder()
            .userId(user.getUsername())
            .sessionId(request.getSessionId())
            .message(request.getMessage())
            .build();
            
        AgentResponse response = agentGatewayService.invoke(
            "customer-service", context);
            
        return ResponseEntity.ok(response);
    }
}

// AgentRequest.java
public record AgentRequest(
    String sessionId,
    String message,
    Map<String, Object> context
) {}

// AgentResponse.java
public record AgentResponse(
    String sessionId,
    String message,
    List<AgentAction> actions,
    Map<String, Object> metadata
) {}
```

### WebSocket 整合

```java
// AgentWebSocketHandler.java
@Component
public class AgentWebSocketHandler extends TextWebSocketHandler {
    
    private final AgentGatewayService agentGatewayService;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String sessionId = extractSessionId(session);
        sessions.put(sessionId, session);
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        AgentRequest request = parseRequest(message.getPayload());
        
        // 異步處理 Agent 請求
        CompletableFuture.runAsync(() -> {
            AgentResponse response = agentGatewayService.invoke(
                request.getAgentType(), 
                buildContext(session, request)
            );
            
            sendResponse(session, response);
        });
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = extractSessionId(session);
        sessions.remove(sessionId);
    }
}
```

## Tool 整合

### Application Service 包裝

```python
# tools/order_tools.py
from typing import Optional, List
from dataclasses import dataclass
import httpx

@dataclass
class OrderStatus:
    order_id: str
    status: str
    items: List[dict]
    estimated_delivery: Optional[str]
    tracking_number: Optional[str]

class OrderServiceClient:
    """訂單服務客戶端"""
    
    def __init__(self, base_url: str, api_key: str):
        self.base_url = base_url
        self.headers = {"X-API-Key": api_key}
    
    async def get_order(self, order_id: str) -> OrderStatus:
        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{self.base_url}/api/v1/orders/{order_id}",
                headers=self.headers
            )
            response.raise_for_status()
            data = response.json()
            return OrderStatus(**data)

# Tool 定義
order_client = OrderServiceClient(
    base_url=os.getenv("ORDER_SERVICE_URL"),
    api_key=os.getenv("ORDER_SERVICE_API_KEY")
)

@tool
async def get_order_status(order_id: str) -> dict:
    """
    查詢訂單狀態
    
    Args:
        order_id: 訂單 ID
        
    Returns:
        訂單狀態資訊
    """
    try:
        order = await order_client.get_order(order_id)
        return {
            "order_id": order.order_id,
            "status": order.status,
            "items": order.items,
            "estimated_delivery": order.estimated_delivery,
            "tracking_number": order.tracking_number
        }
    except httpx.HTTPStatusError as e:
        if e.response.status_code == 404:
            return {"error": "訂單不存在", "order_id": order_id}
        raise
```

### Java Tool Adapter

```java
// AgentToolAdapter.java
@Component
public class AgentToolAdapter {
    
    private final OrderApplicationService orderService;
    private final CustomerApplicationService customerService;
    private final ObjectMapper objectMapper;
    
    /**
     * 執行 Tool 調用
     */
    public ToolResult executeTool(String toolName, Map<String, Object> arguments) {
        return switch (toolName) {
            case "get_order_status" -> getOrderStatus(arguments);
            case "get_customer_orders" -> getCustomerOrders(arguments);
            case "request_refund" -> requestRefund(arguments);
            default -> ToolResult.error("Unknown tool: " + toolName);
        };
    }
    
    private ToolResult getOrderStatus(Map<String, Object> args) {
        try {
            String orderId = (String) args.get("order_id");
            OrderResponse order = orderService.getOrderById(orderId);
            return ToolResult.success(objectMapper.writeValueAsString(order));
        } catch (OrderNotFoundException e) {
            return ToolResult.error("訂單不存在: " + args.get("order_id"));
        } catch (Exception e) {
            return ToolResult.error("查詢失敗: " + e.getMessage());
        }
    }
    
    private ToolResult getCustomerOrders(Map<String, Object> args) {
        try {
            String customerId = (String) args.get("customer_id");
            int limit = (int) args.getOrDefault("limit", 5);
            List<OrderSummary> orders = orderService.getCustomerOrders(customerId, limit);
            return ToolResult.success(objectMapper.writeValueAsString(orders));
        } catch (Exception e) {
            return ToolResult.error("查詢失敗: " + e.getMessage());
        }
    }
    
    private ToolResult requestRefund(Map<String, Object> args) {
        try {
            String orderId = (String) args.get("order_id");
            String reason = (String) args.get("reason");
            RefundRequest request = new RefundRequest(orderId, reason);
            RefundResponse response = orderService.requestRefund(request);
            return ToolResult.success(objectMapper.writeValueAsString(response));
        } catch (BusinessRuleViolationException e) {
            return ToolResult.error("無法退款: " + e.getMessage());
        } catch (Exception e) {
            return ToolResult.error("退款請求失敗: " + e.getMessage());
        }
    }
}

// ToolResult.java
public record ToolResult(
    boolean success,
    String data,
    String error
) {
    public static ToolResult success(String data) {
        return new ToolResult(true, data, null);
    }
    
    public static ToolResult error(String error) {
        return new ToolResult(false, null, error);
    }
}
```

## 事件整合

### Domain Event 監聽

```java
// AgentEventListener.java
@Component
public class AgentEventListener {
    
    private final AgentEventPublisher agentEventPublisher;
    
    /**
     * 監聽訂單創建事件，觸發訂單處理 Agent
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {
        AgentTriggerEvent triggerEvent = AgentTriggerEvent.builder()
            .agentType("order-processing")
            .eventType("ORDER_CREATED")
            .payload(Map.of(
                "orderId", event.orderId().getValue(),
                "customerId", event.customerId().getValue(),
                "totalAmount", event.totalAmount().toString()
            ))
            .build();
            
        agentEventPublisher.publish(triggerEvent);
    }
    
    /**
     * 監聽支付失敗事件，觸發異常處理
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentFailed(PaymentFailedEvent event) {
        AgentTriggerEvent triggerEvent = AgentTriggerEvent.builder()
            .agentType("order-processing")
            .eventType("PAYMENT_FAILED")
            .payload(Map.of(
                "orderId", event.orderId().getValue(),
                "reason", event.failureReason(),
                "retryCount", event.retryCount()
            ))
            .priority("HIGH")
            .build();
            
        agentEventPublisher.publish(triggerEvent);
    }
    
    /**
     * 監聯新評價事件，觸發評價分析
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReviewCreated(ReviewCreatedEvent event) {
        AgentTriggerEvent triggerEvent = AgentTriggerEvent.builder()
            .agentType("review-analysis")
            .eventType("REVIEW_CREATED")
            .payload(Map.of(
                "reviewId", event.reviewId().getValue(),
                "productId", event.productId().getValue(),
                "rating", event.rating()
            ))
            .build();
            
        agentEventPublisher.publish(triggerEvent);
    }
}
```

### Agent Event 發布

```java
// AgentEventPublisher.java
@Component
public class AgentEventPublisher {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String AGENT_EVENTS_TOPIC = "agent-events";
    
    public void publish(AgentTriggerEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(AGENT_EVENTS_TOPIC, event.agentType(), payload);
        } catch (JsonProcessingException e) {
            throw new AgentEventPublishException("Failed to publish agent event", e);
        }
    }
}

// AgentTriggerEvent.java
@Builder
public record AgentTriggerEvent(
    String agentType,
    String eventType,
    Map<String, Object> payload,
    String priority,
    Instant timestamp
) {
    public AgentTriggerEvent {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        if (priority == null) {
            priority = "NORMAL";
        }
    }
}
```

## 安全配置

### 認證整合

```java
// AgentSecurityConfig.java
@Configuration
@EnableWebSecurity
public class AgentSecurityConfig {
    
    @Bean
    public SecurityFilterChain agentSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/v1/agents/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/agents/customer-service/**")
                    .hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/v1/agents/order-processing/**")
                    .hasRole("SYSTEM")
                .requestMatchers("/api/v1/agents/operations/**")
                    .hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt())
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
            
        return http.build();
    }
}
```

### API Key 驗證

```java
// ApiKeyAuthenticationFilter.java
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    
    private final ApiKeyService apiKeyService;
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        String apiKey = request.getHeader("X-API-Key");
        
        if (apiKey != null && apiKeyService.isValid(apiKey)) {
            ApiKeyAuthentication auth = new ApiKeyAuthentication(apiKey);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        
        filterChain.doFilter(request, response);
    }
}
```

## 監控整合

### 指標收集

```java
// AgentMetricsCollector.java
@Component
public class AgentMetricsCollector {
    
    private final MeterRegistry meterRegistry;
    
    private final Counter agentInvocations;
    private final Timer agentLatency;
    private final Counter toolExecutions;
    
    public AgentMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        this.agentInvocations = Counter.builder("agent.invocations")
            .description("Agent invocation count")
            .register(meterRegistry);
            
        this.agentLatency = Timer.builder("agent.latency")
            .description("Agent response latency")
            .register(meterRegistry);
            
        this.toolExecutions = Counter.builder("agent.tool.executions")
            .description("Tool execution count")
            .register(meterRegistry);
    }
    
    public void recordInvocation(String agentType, String status) {
        agentInvocations.increment(Tags.of(
            "agent_type", agentType,
            "status", status
        ));
    }
    
    public Timer.Sample startLatencyTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordLatency(Timer.Sample sample, String agentType) {
        sample.stop(agentLatency.withTag("agent_type", agentType));
    }
    
    public void recordToolExecution(String toolName, boolean success) {
        toolExecutions.increment(Tags.of(
            "tool_name", toolName,
            "success", String.valueOf(success)
        ));
    }
}
```

### 追蹤整合

```java
// AgentTracingAspect.java
@Aspect
@Component
public class AgentTracingAspect {
    
    private final Tracer tracer;
    
    @Around("@annotation(AgentOperation)")
    public Object traceAgentOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        AgentOperation annotation = getAnnotation(joinPoint);
        
        Span span = tracer.nextSpan()
            .name("agent-" + annotation.value())
            .tag("agent.type", annotation.value())
            .start();
            
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            Object result = joinPoint.proceed();
            span.tag("agent.status", "success");
            return result;
        } catch (Exception e) {
            span.tag("agent.status", "error");
            span.tag("agent.error", e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
```

## 錯誤處理

### 統一錯誤處理

```java
// AgentExceptionHandler.java
@RestControllerAdvice(basePackages = "solid.humank.genaidemo.interfaces.rest.agent")
public class AgentExceptionHandler {
    
    @ExceptionHandler(AgentTimeoutException.class)
    public ResponseEntity<AgentErrorResponse> handleTimeout(AgentTimeoutException e) {
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
            .body(new AgentErrorResponse(
                "AGENT_TIMEOUT",
                "Agent 回應超時，請稍後重試",
                e.getAgentType()
            ));
    }
    
    @ExceptionHandler(AgentUnavailableException.class)
    public ResponseEntity<AgentErrorResponse> handleUnavailable(AgentUnavailableException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new AgentErrorResponse(
                "AGENT_UNAVAILABLE",
                "Agent 服務暫時不可用",
                e.getAgentType()
            ));
    }
    
    @ExceptionHandler(ToolExecutionException.class)
    public ResponseEntity<AgentErrorResponse> handleToolError(ToolExecutionException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new AgentErrorResponse(
                "TOOL_EXECUTION_ERROR",
                "工具執行失敗: " + e.getMessage(),
                e.getToolName()
            ));
    }
}

// AgentErrorResponse.java
public record AgentErrorResponse(
    String errorCode,
    String message,
    String context,
    Instant timestamp
) {
    public AgentErrorResponse(String errorCode, String message, String context) {
        this(errorCode, message, context, Instant.now());
    }
}
```

## 配置管理

### 環境配置

```yaml
# application-agent.yml
agent:
  gateway:
    base-url: ${AGENT_GATEWAY_URL:http://localhost:8081}
    timeout: 30s
    retry:
      max-attempts: 3
      backoff: 1s
      
  customer-service:
    enabled: true
    model: anthropic.claude-3-sonnet
    max-tokens: 2000
    temperature: 0.7
    
  order-processing:
    enabled: true
    model: anthropic.claude-3-haiku
    max-tokens: 1000
    
  memory:
    type: dynamodb
    table-prefix: ${ENVIRONMENT}-agent-memory
    ttl: 24h
    
  tools:
    order-service:
      url: ${ORDER_SERVICE_URL}
      api-key: ${ORDER_SERVICE_API_KEY}
    customer-service:
      url: ${CUSTOMER_SERVICE_URL}
      api-key: ${CUSTOMER_SERVICE_API_KEY}
```

---

**文件版本**: 1.0  
**建立日期**: 2026-01-06  
**維護者**: Architecture Team
