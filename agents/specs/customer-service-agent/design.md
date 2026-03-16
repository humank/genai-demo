# 智能客服 Agent 設計文檔

## 概述

本文檔描述智能客服 Agent 的技術設計，包括架構、組件、整合方式和實作細節。

## 架構設計

### 整體架構

```
┌─────────────────────────────────────────────────────────────────┐
│                    Frontend Applications                         │
│              (Consumer App / CMC Management)                     │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Agent Gateway Layer                           │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              AgentGatewayController                      │   │
│  │  - REST: POST /api/v1/agents/customer-service           │   │
│  │  - WebSocket: /api/v1/agents/customer-service/ws        │   │
│  │  - Authentication: JWT Token                             │   │
│  │  - Rate Limiting: 100 req/min per user                  │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Agent Service Layer                           │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │           CustomerServiceAgentService                    │   │
│  │  - 對話管理                                              │   │
│  │  - 意圖識別                                              │   │
│  │  - Tool 調度                                             │   │
│  │  - 回應生成                                              │   │
│  └─────────────────────────────────────────────────────────┘   │
│                            │                                     │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                 AgentCore Runtime                        │   │
│  │  - LLM Integration (Claude 3 Sonnet)                    │   │
│  │  - Tool Execution                                        │   │
│  │  - Memory Management                                     │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Tool Adapter Layer                            │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐               │
│  │ OrderTool   │ │CustomerTool │ │ PaymentTool │               │
│  │ Adapter     │ │ Adapter     │ │ Adapter     │               │
│  └─────────────┘ └─────────────┘ └─────────────┘               │
│  ┌─────────────┐ ┌─────────────┐                               │
│  │ ProductTool │ │Notification │                               │
│  │ Adapter     │ │ Tool Adapter│                               │
│  └─────────────┘ └─────────────┘                               │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                 Existing Application Services                    │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐               │
│  │   Order     │ │  Customer   │ │  Payment    │               │
│  │ Application │ │ Application │ │ Application │               │
│  │  Service    │ │  Service    │ │  Service    │               │
│  └─────────────┘ └─────────────┘ └─────────────┘               │
└─────────────────────────────────────────────────────────────────┘
```

### 組件設計

#### 1. AgentGatewayController

負責處理外部請求，提供 REST 和 WebSocket 端點。

```java
@RestController
@RequestMapping("/api/v1/agents/customer-service")
public class CustomerServiceAgentController {
    
    private final CustomerServiceAgentService agentService;
    private final AgentMetricsCollector metricsCollector;
    
    @PostMapping
    public ResponseEntity<AgentResponse> chat(
            @Valid @RequestBody AgentRequest request,
            @AuthenticationPrincipal UserDetails user) {
        
        Timer.Sample sample = metricsCollector.startLatencyTimer();
        try {
            AgentContext context = buildContext(request, user);
            AgentResponse response = agentService.process(context);
            metricsCollector.recordInvocation("customer-service", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            metricsCollector.recordInvocation("customer-service", "error");
            throw e;
        } finally {
            metricsCollector.recordLatency(sample, "customer-service");
        }
    }
}
```

#### 2. CustomerServiceAgentService

核心 Agent 服務，協調 LLM、Tools 和 Memory。

```java
@Service
public class CustomerServiceAgentService {
    
    private final AgentCoreClient agentCoreClient;
    private final AgentMemoryService memoryService;
    private final AgentToolRegistry toolRegistry;
    
    public AgentResponse process(AgentContext context) {
        // 1. 載入對話記憶
        ConversationMemory memory = memoryService.loadMemory(context.getSessionId());
        
        // 2. 構建 Agent 請求
        AgentCoreRequest request = AgentCoreRequest.builder()
            .systemPrompt(getSystemPrompt())
            .userMessage(context.getMessage())
            .conversationHistory(memory.getHistory())
            .tools(toolRegistry.getAvailableTools())
            .build();
        
        // 3. 調用 AgentCore
        AgentCoreResponse coreResponse = agentCoreClient.invoke(request);
        
        // 4. 執行 Tool 調用（如果有）
        if (coreResponse.hasToolCalls()) {
            coreResponse = executeToolCalls(coreResponse, context);
        }
        
        // 5. 更新記憶
        memoryService.updateMemory(context.getSessionId(), context.getMessage(), coreResponse);
        
        // 6. 構建回應
        return buildResponse(coreResponse, context);
    }
}
```

#### 3. Tool Adapters

將 Agent Tool 調用轉換為 Application Service 調用。

```java
@Component
public class OrderToolAdapter implements AgentTool {
    
    private final OrderApplicationService orderService;
    
    @Override
    public String getName() {
        return "get_order_status";
    }
    
    @Override
    public String getDescription() {
        return "查詢訂單狀態，包括訂單詳情、物流資訊和預計送達時間";
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
        try {
            OrderDto order = orderService.getOrderById(orderId);
            return ToolResult.success(formatOrderResponse(order));
        } catch (OrderNotFoundException e) {
            return ToolResult.error("訂單不存在: " + orderId);
        }
    }
}
```

### Memory 設計

#### 短期記憶 (Conversation Memory)

```java
public class ConversationMemory {
    private String sessionId;
    private List<Message> history;  // 最多 20 輪
    private Map<String, Object> context;  // 當前對話上下文
    private Instant lastActivity;
    private static final Duration TTL = Duration.ofMinutes(30);
}
```

#### 長期記憶 (Customer Memory)

```java
public class CustomerMemory {
    private String customerId;
    private String preferredLanguage;
    private List<String> recentOrderIds;
    private Map<String, Object> preferences;
    private List<InteractionSummary> interactionHistory;
}
```

#### DynamoDB Schema

```
Table: agent-memory-{env}

Partition Key: PK (String)
Sort Key: SK (String)

Items:
- Session Memory: PK=SESSION#{sessionId}, SK=MEMORY
- Customer Memory: PK=CUSTOMER#{customerId}, SK=MEMORY
- Interaction Log: PK=CUSTOMER#{customerId}, SK=INTERACTION#{timestamp}

GSI1: customerId-index
- GSI1PK: customerId
- GSI1SK: timestamp
```

### System Prompt 設計

```
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
- 使用與客戶相同的語言（繁體中文或英文）

## 工具使用指南
- 查詢訂單時，優先使用 get_order_status 工具
- 如果客戶沒有提供訂單編號，使用 get_customer_orders 查詢最近訂單
- 處理退款時，先用 check_refund_eligibility 檢查資格
- 發送通知時，使用 send_notification 工具

## 限制
- 不要編造訂單或產品資訊，必須使用工具查詢
- 不要承諾無法確認的事項
- 涉及敏感操作時需要確認客戶身份
- 如果無法解決問題，主動提供人工客服選項

## 升級條件
當以下情況發生時，提供人工客服選項：
- 客戶明確要求人工服務
- 客戶情緒明顯負面
- 連續 3 次無法解決問題
- 問題超出你的處理範圍
```

### API 設計

#### Request/Response DTOs

```java
// 請求
public record AgentRequest(
    @NotBlank String sessionId,
    @NotBlank String message,
    Map<String, Object> context
) {}

// 回應
public record AgentResponse(
    String sessionId,
    String message,
    List<AgentAction> actions,
    AgentMetadata metadata
) {}

public record AgentAction(
    String type,  // "show_order", "confirm_refund", "escalate"
    Map<String, Object> data
) {}

public record AgentMetadata(
    String conversationId,
    int turnCount,
    String detectedIntent,
    String language,
    long processingTimeMs
) {}
```

#### Error Response

```java
public record AgentErrorResponse(
    String errorCode,
    String message,
    String conversationId,
    Instant timestamp
) {}
```

### 監控設計

#### Metrics

| Metric Name | Type | Labels | Description |
|-------------|------|--------|-------------|
| agent.invocations | Counter | agent_type, status | Agent 調用次數 |
| agent.latency | Timer | agent_type | Agent 回應延遲 |
| agent.tool.executions | Counter | tool_name, success | Tool 執行次數 |
| agent.memory.operations | Counter | operation, success | Memory 操作次數 |
| agent.escalations | Counter | reason | 升級到人工次數 |

#### Tracing

```java
@Aspect
@Component
public class AgentTracingAspect {
    
    @Around("@annotation(AgentOperation)")
    public Object trace(ProceedingJoinPoint joinPoint) throws Throwable {
        Span span = tracer.nextSpan()
            .name("agent-operation")
            .tag("agent.type", "customer-service")
            .start();
        
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            return joinPoint.proceed();
        } finally {
            span.end();
        }
    }
}
```

### 安全設計

#### 認證流程

```
Client → JWT Token → AgentGatewayController → Validate Token
                                                    ↓
                                            Extract User Info
                                                    ↓
                                            Build AgentContext
                                                    ↓
                                            Process Request
```

#### 敏感資料處理

```java
@Component
public class AgentPiiMaskingService {
    
    public String maskSensitiveData(String text) {
        // 遮蔽信用卡號
        text = text.replaceAll("\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}", "****-****-****-****");
        // 遮蔽電話號碼
        text = text.replaceAll("09\\d{8}", "09********");
        // 遮蔽 Email
        text = text.replaceAll("([a-zA-Z0-9._-]+)@([a-zA-Z0-9._-]+)", "***@***");
        return text;
    }
}
```

### 錯誤處理設計

#### 錯誤類型

| Error Code | HTTP Status | Description | Recovery |
|------------|-------------|-------------|----------|
| AGENT_TIMEOUT | 504 | Agent 回應超時 | 重試或降級 |
| TOOL_EXECUTION_ERROR | 500 | Tool 執行失敗 | 重試或跳過 |
| MEMORY_ERROR | 500 | Memory 操作失敗 | 降級到無狀態 |
| RATE_LIMITED | 429 | 超過速率限制 | 等待後重試 |
| INVALID_REQUEST | 400 | 請求格式錯誤 | 修正請求 |

#### 降級策略

```java
@Component
public class AgentFallbackService {
    
    public AgentResponse getFallbackResponse(AgentContext context, Exception e) {
        if (e instanceof AgentTimeoutException) {
            return AgentResponse.builder()
                .message("抱歉，系統目前較忙，請稍後再試或聯繫人工客服。")
                .actions(List.of(new AgentAction("escalate", Map.of())))
                .build();
        }
        // ... 其他降級邏輯
    }
}
```

## 技術選型

| 組件 | 技術選擇 | 理由 |
|------|----------|------|
| LLM | Claude 3 Sonnet | 平衡效能和成本 |
| Memory Storage | DynamoDB | 低延遲、高可用 |
| API Framework | Spring Boot 3.x | 與現有系統一致 |
| Metrics | Micrometer + CloudWatch | 整合現有監控 |
| Tracing | AWS X-Ray | 整合現有追蹤 |

## 部署架構

### 開發環境

- Agent Service 作為 Spring Boot 模組運行
- Memory 使用本地 DynamoDB Local
- LLM 使用 Mock 或低成本模型

### 生產環境

- Agent Service 部署到 EKS
- Memory 使用 DynamoDB (On-Demand)
- LLM 使用 Bedrock Claude 3 Sonnet
- 整合現有的 ALB 和 Route 53

## 測試策略

### 單元測試

- Tool Adapter 邏輯測試
- Memory Service 測試
- Response Builder 測試

### 整合測試

- Agent Service 與 Application Services 整合
- Memory 讀寫測試
- API 端點測試

### E2E 測試

- 完整對話流程測試
- 多輪對話測試
- 升級流程測試

### 效能測試

- 延遲基準測試
- 並發測試
- Memory 效能測試

