# 微服務重構計劃

## 1. 事件驅動架構改造

### 當前問題
- 使用 Spring ApplicationEvent，僅限於單體應用內部
- 缺乏事件持久化和重試機制
- 沒有跨服務的事件通信

### 改造方案

#### A. 引入消息中間件
```yaml
# docker-compose.yml 或 Kubernetes manifest
services:
  kafka:
    image: confluentinc/cp-kafka:latest
  redis:
    image: redis:alpine
```

#### B. 事件基礎設施改造
```java
// 新增：分散式事件發布器
@Component
public class DistributedEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public void publish(DomainEvent event) {
        String topic = event.getEventType().toLowerCase() + "-events";
        kafkaTemplate.send(topic, event);
    }
}

// 新增：事件存儲
@Entity
public class EventStore {
    private String eventId;
    private String eventType;
    private String aggregateId;
    private String eventData;
    private LocalDateTime occurredAt;
    private boolean published;
}
```

## 2. 服務間通信改造

### 當前問題
- 直接依賴其他領域的 Repository
- 缺乏服務邊界隔離
- 沒有熔斷和重試機制

### 改造方案

#### A. API Gateway 模式
```java
// 新增：服務發現和負載均衡
@Component
public class ServiceDiscovery {
    @Value("${services.inventory.url}")
    private String inventoryServiceUrl;
    
    @Value("${services.payment.url}")
    private String paymentServiceUrl;
}
```

#### B. 外部服務適配器
```java
// 改造：PaymentServicePort 實現
@Component
public class PaymentServiceAdapter implements PaymentServicePort {
    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;
    
    @Override
    public PaymentResult processPayment(UUID orderId, Money amount) {
        return circuitBreaker.executeSupplier(() -> 
            webClient.post()
                .uri("/payments")
                .bodyValue(new PaymentRequest(orderId, amount))
                .retrieve()
                .bodyToMono(PaymentResult.class)
                .block()
        );
    }
}
```

## 3. 數據一致性改造

### 當前問題
- 使用單一數據庫
- 缺乏分散式事務處理
- SAGA 模式實現不完整

### 改造方案

#### A. 每個服務獨立數據庫
```yaml
# Order Service
spring:
  datasource:
    url: jdbc:postgresql://order-db:5432/orderdb

# Inventory Service  
spring:
  datasource:
    url: jdbc:postgresql://inventory-db:5432/inventorydb
```

#### B. SAGA 模式完整實現
```java
// 改造：分散式 SAGA 協調器
@Component
public class DistributedSagaOrchestrator {
    
    @SagaOrchestrationStart
    public void handleOrderCreated(OrderCreatedEvent event) {
        SagaTransaction saga = SagaTransaction.builder()
            .sagaId(UUID.randomUUID())
            .addStep(new ReserveInventoryStep(event.getOrderId()))
            .addStep(new ProcessPaymentStep(event.getOrderId()))
            .addStep(new ArrangeDeliveryStep(event.getOrderId()))
            .build();
            
        sagaManager.execute(saga);
    }
}
```

## 4. AWS EKS 原生架構改造

### A. 配置管理 - AWS Systems Manager Parameter Store
```java
// 使用 AWS Parameter Store 替代 Spring Cloud Config
@Configuration
public class AwsParameterStoreConfig {
    
    @Bean
    public SsmClient ssmClient() {
        return SsmClient.builder()
            .region(Region.AP_NORTHEAST_1)
            .build();
    }
    
    @Value("${aws.parameterstore.prefix:/genai-demo}")
    private String parameterPrefix;
    
    @PostConstruct
    public void loadParameters() {
        // 從 Parameter Store 載入配置
        GetParametersByPathRequest request = GetParametersByPathRequest.builder()
            .path(parameterPrefix)
            .recursive(true)
            .withDecryption(true)
            .build();
            
        GetParametersByPathResponse response = ssmClient.getParametersByPath(request);
        // 設置系統屬性
    }
}
```

### B. EKS 部署配置
```yaml
# order-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
  namespace: genai-demo
spec:
  replicas: 3
  selector:
    matchLabels:
      app: order-service
  template:
    metadata:
      labels:
        app: order-service
      annotations:
        # AWS Load Balancer Controller 註解
        service.beta.kubernetes.io/aws-load-balancer-type: "nlb"
    spec:
      serviceAccountName: order-service-sa
      containers:
      - name: order-service
        image: 123456789012.dkr.ecr.ap-northeast-1.amazonaws.com/genai-demo/order-service:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "aws"
        - name: AWS_REGION
          value: "ap-northeast-1"
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10

---
# Service Account with IRSA (IAM Roles for Service Accounts)
apiVersion: v1
kind: ServiceAccount
metadata:
  name: order-service-sa
  namespace: genai-demo
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::123456789012:role/OrderServiceRole

---
# Service
apiVersion: v1
kind: Service
metadata:
  name: order-service
  namespace: genai-demo
  annotations:
    service.beta.kubernetes.io/aws-load-balancer-type: "nlb"
    service.beta.kubernetes.io/aws-load-balancer-scheme: "internal"
spec:
  selector:
    app: order-service
  ports:
  - port: 80
    targetPort: 8080
  type: LoadBalancer
```

## 5. 監控和可觀測性

### A. 分散式追蹤
```java
// 新增：追蹤配置
@Configuration
public class TracingConfiguration {
    
    @Bean
    public Sender sender() {
        return OkHttpSender.create("http://jaeger:14268/api/traces");
    }
    
    @Bean
    public AsyncReporter<Span> spanReporter() {
        return AsyncReporter.create(sender());
    }
}
```

### B. 健康檢查和指標
```java
// 新增：自定義健康檢查
@Component
public class OrderServiceHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        // 檢查數據庫連接、外部服務等
        return Health.up()
            .withDetail("database", "UP")
            .withDetail("kafka", "UP")
            .build();
    }
}
```

## 6. 安全性改造

### A. 服務間認證
```java
// 新增：JWT 令牌驗證
@Component
public class ServiceAuthenticationFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        // 驗證服務間調用的 JWT 令牌
        String token = extractToken(request);
        if (isValidServiceToken(token)) {
            chain.doFilter(request, response);
        } else {
            throw new UnauthorizedException("Invalid service token");
        }
    }
}
```

## 7. 重構步驟建議

### Phase 1: 基礎設施準備 (2-3 週)
1. 設置 Kafka/Redis 消息中間件
2. 實現分散式事件發布機制
3. 建立服務發現和配置中心

### Phase 2: 服務拆分 (4-6 週)
1. 拆分 Order Service (最核心)
2. 拆分 Payment Service
3. 拆分 Inventory Service
4. 建立 API Gateway

### Phase 3: 數據遷移 (2-3 週)
1. 為每個服務建立獨立數據庫
2. 實現數據遷移腳本
3. 建立數據同步機制

### Phase 4: 部署和監控 (2-3 週)
1. Kubernetes 部署配置
2. 監控和日誌系統
3. 性能測試和調優

### Phase 5: 生產部署 (1-2 週)
1. 藍綠部署策略
2. 回滾計劃
3. 生產監控

## 8. 風險評估和緩解

### 高風險項目
1. **數據一致性**：使用 SAGA 模式和最終一致性
2. **服務間依賴**：實現熔斷器和降級策略
3. **性能影響**：網絡延遲增加，需要優化調用鏈

### 緩解策略
1. 漸進式重構，保持向後兼容
2. 完整的測試覆蓋，包括集成測試
3. 監控和告警系統完善
4. 回滾計劃和應急預案