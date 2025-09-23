---
inclusion: always
---

# Rozanski & Woods Architecture Methodology Steering Rules

## Mandatory Architectural Viewpoint Checks

> **🔗 相關標準**: 
> - [Development Standards](development-standards.md) - 基本開發和架構約束
> - [Domain Events](domain-events.md) - DDD 事件架構實作
> - [Security Standards](security-standards.md) - 安全架構要求
> - [Performance Standards](performance-standards.md) - 效能架構要求

### Each new feature must complete the following viewpoint checks

#### Functional Viewpoint

- [ ] Aggregate boundaries clearly defined
- [ ] Domain service responsibilities clarified
- [ ] Use case implementation follows DDD tactical patterns

#### Information Viewpoint  

- [ ] Domain event design complete
- [ ] Data consistency strategy defined
- [ ] Event sourcing considerations addressed

#### Concurrency Viewpoint

- [ ] Asynchronous processing strategy documented
- [ ] Transaction boundaries clearly defined
- [ ] Concurrency conflict handling mechanisms

#### Development Viewpoint

- [ ] Module dependencies comply with hexagonal architecture
- [ ] Testing strategy covers all layers
- [ ] Build scripts updated

#### Deployment Viewpoint

- [ ] CDK infrastructure updated
- [ ] Environment configuration changes recorded
- [ ] Deployment strategy impact assessed

#### Operational Viewpoint

- [ ] Monitoring metrics defined
- [ ] Log structure designed
- [ ] Failure handling procedures

#### Context Viewpoint

- [ ] External system integration boundaries defined
- [ ] Stakeholder interaction models documented
- [ ] System boundary and external dependencies mapped
- [ ] Integration protocols and data exchange formats specified
- [ ] External service contracts and SLAs defined
- [ ] Organizational and regulatory constraints identified

## Quality Attribute Scenario Requirements

### Each user story must include at least one quality attribute scenario

#### Scenario Format: Source → Stimulus → Environment → Artifact → Response → Response Measure

### QAS Templates by Quality Attribute

#### Performance Scenarios

```
Template:
Source: [User/System/Load Generator]
Stimulus: [Specific request/operation with load characteristics]
Environment: [Normal/Peak/Stress conditions]
Artifact: [System component/service]
Response: [System processes the request]
Response Measure: [Response time ≤ X ms, Throughput ≥ Y req/s, CPU ≤ Z%]

Example:
Source: Web user
Stimulus: Submit order with 3 items during peak shopping hours
Environment: Normal operation with 1000 concurrent users
Artifact: Order processing service
Response: Order is processed and confirmation is returned
Response Measure: Response time ≤ 2000ms, Success rate ≥ 99.5%
```

#### Security Scenarios

```
Template:
Source: [Attacker/Malicious user/System]
Stimulus: [Attack type/unauthorized access attempt]
Environment: [Network/System state]
Artifact: [System component under attack]
Response: [System security response]
Response Measure: [Detection time, Prevention success rate, Recovery time]

Example:
Source: Malicious user
Stimulus: Attempts SQL injection on customer search endpoint
Environment: Production system with normal load
Artifact: Customer API service
Response: System detects and blocks the attack, logs the incident
Response Measure: Attack blocked within 100ms, Incident logged, No data exposure
```

#### Availability Scenarios

```
Template:
Source: [Failure source]
Stimulus: [Failure type]
Environment: [System state during failure]
Artifact: [Affected component]
Response: [System recovery action]
Response Measure: [RTO ≤ X minutes, RPO ≤ Y minutes, Availability ≥ Z%]

Example:
Source: Database server
Stimulus: Primary database server fails
Environment: Production system during business hours
Artifact: Customer data service
Response: System fails over to secondary database
Response Measure: RTO ≤ 5 minutes, RPO ≤ 1 minute, Availability ≥ 99.9%
```

#### Scalability Scenarios

```
Template:
Source: [Load source]
Stimulus: [Load increase pattern]
Environment: [Current system capacity]
Artifact: [System component]
Response: [Scaling action]
Response Measure: [Capacity increase, Performance maintenance, Cost impact]

Example:
Source: Marketing campaign
Stimulus: User load increases from 100 to 1000 concurrent users over 1 hour
Environment: Current system running at 60% capacity
Artifact: Web application tier
Response: System automatically scales out additional instances
Response Measure: Maintains response time ≤ 2s, Scales to handle 1000 users, Cost increase ≤ 50%
```

#### Usability Scenarios

```
Template:
Source: [User type]
Stimulus: [User task/goal]
Environment: [Usage context]
Artifact: [User interface/system]
Response: [System provides interface/feedback]
Response Measure: [Task completion time, Error rate, User satisfaction]

Example:
Source: New customer
Stimulus: Wants to complete first purchase
Environment: Using mobile device during commute
Artifact: Mobile checkout interface
Response: System guides user through streamlined checkout process
Response Measure: Checkout completion ≤ 3 minutes, Error rate ≤ 2%, Abandonment rate ≤ 10%
```

### Quantitative Metrics Requirements

#### Performance Metrics

- **Response Time**: API endpoints ≤ 2s (95th percentile)
- **Throughput**: System handles ≥ 1000 req/s peak load
- **Resource Usage**: CPU ≤ 70%, Memory ≤ 80%, Disk I/O ≤ 80%
- **Database**: Query response ≤ 100ms (95th percentile)

#### Security Metrics

- **Authentication**: Login success rate ≥ 99.9%
- **Authorization**: Access control violations = 0
- **Encryption**: All data encrypted in transit (TLS 1.3) and at rest (AES-256)
- **Vulnerability**: Zero critical/high severity vulnerabilities in production

#### Availability Metrics

- **Uptime**: System availability ≥ 99.9% (8.76 hours downtime/year)
- **RTO**: Recovery Time Objective ≤ 5 minutes
- **RPO**: Recovery Point Objective ≤ 1 minute
- **MTTR**: Mean Time To Recovery ≤ 15 minutes

#### Scalability Metrics

- **Horizontal Scaling**: Auto-scale from 2 to 20 instances
- **Load Handling**: Support 10x traffic increase within 10 minutes
- **Database**: Read replicas scale automatically based on load
- **Storage**: Auto-scaling storage with 99.999% durability

## Architecture Compliance Rules

### Mandatory ArchUnit Rules

```java
// Domain layer dependency restrictions
@ArchTest
static final ArchRule domainLayerRules = classes()
    .that().resideInAPackage("..domain..")
    .should().onlyDependOnClassesThat()
    .resideInAnyPackage("..domain..", "java..", "org.springframework..");

// Aggregate root rules
@ArchTest  
static final ArchRule aggregateRootRules = classes()
    .that().areAnnotatedWith(AggregateRoot.class)
    .should().implement(AggregateRootInterface.class);

// Event handler rules
@ArchTest
static final ArchRule eventHandlerRules = classes()
    .that().areAnnotatedWith(Component.class)
    .and().haveSimpleNameEndingWith("EventHandler")
    .should().beAnnotatedWith(TransactionalEventListener.class);

// Value object rules
@ArchTest
static final ArchRule valueObjectRules = classes()
    .that().areAnnotatedWith(ValueObject.class)
    .should().beRecords();
```

## ADR Required Content

### ADR Template Structure

```markdown
# ADR-{NUMBER}: {TITLE}

## Status
[Proposed | Accepted | Deprecated | Superseded by ADR-XXX]

## Context
### Problem Statement
[Describe the problem that needs to be solved]

### Business Context
[Business drivers, constraints, and requirements]

### Technical Context
[Current architecture, technical constraints, and dependencies]

## Decision Drivers
- [Driver 1: e.g., Performance requirements]
- [Driver 2: e.g., Cost constraints]
- [Driver 3: e.g., Team expertise]
- [Driver 4: e.g., Time to market]

## Considered Options
### Option 1: [Name]
**Pros:**
- [Advantage 1]
- [Advantage 2]

**Cons:**
- [Disadvantage 1]
- [Disadvantage 2]

**Cost:** [Implementation cost, maintenance cost]
**Risk:** [High/Medium/Low] - [Risk description]

### Option 2: [Name]
[Same structure as Option 1]

### Option 3: [Name]
[Same structure as Option 1]

## Decision Outcome
**Chosen Option:** [Selected option with rationale]

**Rationale:**
[Detailed explanation of why this option was chosen]

## Impact Analysis

### Stakeholder Impact
| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | High | Need to learn new technology | Training plan, documentation |
| Operations Team | Medium | New monitoring requirements | Update runbooks, training |
| End Users | Low | No visible changes | N/A |
| Business | Medium | Cost increase | Budget approval obtained |

### Impact Radius Assessment
- **Local**: [Changes within single component/service]
- **Bounded Context**: [Changes across related services]
- **System**: [Changes across multiple bounded contexts]
- **Enterprise**: [Changes affecting multiple systems]

**Selected Impact Radius:** [Local/Bounded Context/System/Enterprise]

### Risk Assessment
| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|-------------------|
| Technology learning curve | Medium | High | Provide training, pair programming |
| Performance degradation | Low | High | Load testing, performance monitoring |
| Integration complexity | High | Medium | Proof of concept, incremental rollout |

**Overall Risk Level:** [High/Medium/Low]

## Implementation Plan

### Migration Path
**Phase 1: Preparation (Week 1-2)**
- [ ] Team training
- [ ] Environment setup
- [ ] Proof of concept

**Phase 2: Implementation (Week 3-6)**
- [ ] Core functionality implementation
- [ ] Unit and integration tests
- [ ] Documentation updates

**Phase 3: Deployment (Week 7-8)**
- [ ] Staging environment deployment
- [ ] Performance testing
- [ ] Production deployment
- [ ] Monitoring setup

### Rollback Strategy
**Trigger Conditions:**
- Performance degradation > 20%
- Error rate > 1%
- Critical functionality unavailable > 5 minutes

**Rollback Steps:**
1. [Immediate action - e.g., traffic routing]
2. [Database rollback if needed]
3. [Service rollback procedure]
4. [Verification steps]

**Rollback Time:** [Target time to complete rollback]

## Monitoring and Success Criteria

### Success Metrics
- [Metric 1: e.g., Response time < 2s]
- [Metric 2: e.g., Error rate < 0.1%]
- [Metric 3: e.g., Cost reduction of 20%]

### Monitoring Plan
- [Dashboard/Alert 1]
- [Dashboard/Alert 2]
- [Review schedule]

## Consequences

### Positive Consequences
- [Benefit 1]
- [Benefit 2]

### Negative Consequences
- [Trade-off 1]
- [Trade-off 2]

### Technical Debt
- [Any technical debt introduced]
- [Plan to address technical debt]

## Related Decisions
- [ADR-XXX: Related decision]
- [Link to relevant documentation]

## Notes
[Any additional notes, assumptions, or constraints]
```

### ADR Quality Checklist

Before accepting an ADR, ensure:

- [ ] Problem statement is clear and specific
- [ ] At least 3 options were considered
- [ ] Each option includes pros, cons, costs, and risks
- [ ] Decision rationale is well-documented
- [ ] Stakeholder impact analysis is complete
- [ ] Risk assessment includes mitigation strategies
- [ ] Implementation plan has clear phases and timelines
- [ ] Rollback strategy is detailed and testable
- [ ] Success criteria are measurable
- [ ] Monitoring plan is specific

### ADR Review Process

1. **Author** creates ADR in "Proposed" status
2. **Architecture Team** reviews technical aspects
3. **Stakeholders** review impact analysis
4. **Team Lead** approves implementation plan
5. **ADR** status changes to "Accepted"
6. **Implementation** begins according to plan
7. **Review** success criteria after implementation

## Observability Requirements

### Mandatory for new features

- Each aggregate root must have corresponding business metrics
- Each use case must have execution tracing and performance metrics
- Each domain event must have publication and processing metrics
- Critical paths must have monitoring and alerting

### Monitoring Implementation Standards

#### Business Metrics (Required for each Aggregate Root)

```java
@Component
public class CustomerMetrics {
    private final MeterRegistry meterRegistry;
    private final Counter customersCreated;
    private final Timer customerCreationTime;
    private final Gauge activeCustomers;
    
    public CustomerMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.customersCreated = Counter.builder("customers.created")
            .description("Total number of customers created")
            .register(meterRegistry);
        this.customerCreationTime = Timer.builder("customers.creation.time")
            .description("Time taken to create a customer")
            .register(meterRegistry);
        this.activeCustomers = Gauge.builder("customers.active")
            .description("Number of active customers")
            .register(meterRegistry, this, CustomerMetrics::getActiveCustomerCount);
    }
    
    public void recordCustomerCreated() {
        customersCreated.increment();
    }
    
    public Timer.Sample startCustomerCreation() {
        return Timer.start(meterRegistry);
    }
    
    private double getActiveCustomerCount() {
        // Implementation to get active customer count
        return customerRepository.countActiveCustomers();
    }
}
```

#### Use Case Tracing (Required for each Application Service)

```java
@Service
@Transactional
public class CustomerApplicationService {
    
    @TraceAsync
    @Timed(name = "customer.creation", description = "Time taken to create customer")
    public void createCustomer(CreateCustomerCommand command) {
        Span span = tracer.nextSpan()
            .name("create-customer")
            .tag("customer.type", command.getType())
            .tag("customer.source", command.getSource())
            .start();
            
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            // Business logic implementation
            Customer customer = customerFactory.create(command);
            customerRepository.save(customer);
            
            // Add business context to trace
            span.tag("customer.id", customer.getId())
                .tag("customer.segment", customer.getSegment())
                .event("customer.created");
                
            domainEventService.publishEventsFromAggregate(customer);
            
        } catch (Exception e) {
            span.tag("error", e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
```

#### Domain Event Metrics (Required for each Event Type)

```java
@Component
public class DomainEventMetrics {
    
    @EventListener
    public void recordEventPublished(DomainEventPublishedEvent event) {
        Counter.builder("domain.events.published")
            .tag("event.type", event.getEventType())
            .tag("aggregate.type", event.getAggregateType())
            .register(meterRegistry)
            .increment();
    }
    
    @EventListener
    public void recordEventProcessed(DomainEventProcessedEvent event) {
        Timer.builder("domain.events.processing.time")
            .tag("event.type", event.getEventType())
            .tag("handler", event.getHandlerName())
            .register(meterRegistry)
            .record(event.getProcessingTime(), TimeUnit.MILLISECONDS);
    }
    
    @EventListener
    public void recordEventFailed(DomainEventFailedEvent event) {
        Counter.builder("domain.events.failed")
            .tag("event.type", event.getEventType())
            .tag("error.type", event.getErrorType())
            .register(meterRegistry)
            .increment();
    }
}
```

### Logging Structure Standards

#### Structured Logging Format

```java
// Use consistent structured logging
public class StructuredLogger {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    public void logBusinessEvent(String event, Object... keyValues) {
        logger.info("Business event: {}", event, 
            StructuredArguments.kv("timestamp", Instant.now()),
            StructuredArguments.kv("traceId", getCurrentTraceId()),
            StructuredArguments.kv("userId", getCurrentUserId()),
            keyValues);
    }
    
    public void logError(String message, Exception e, Object... keyValues) {
        logger.error("Error occurred: {}", message,
            StructuredArguments.kv("timestamp", Instant.now()),
            StructuredArguments.kv("traceId", getCurrentTraceId()),
            StructuredArguments.kv("errorType", e.getClass().getSimpleName()),
            keyValues,
            e);
    }
}

// Usage example
structuredLogger.logBusinessEvent("Customer created",
    kv("customerId", customer.getId()),
    kv("customerType", customer.getType()),
    kv("registrationSource", "web"));
```

#### Log Correlation Standards

```java
@Component
public class TraceContextFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        String traceId = extractOrGenerateTraceId(request);
        String sessionId = extractSessionId(request);
        
        try (MDCCloseable mdcCloseable = MDC.putCloseable("traceId", traceId)) {
            MDC.put("sessionId", sessionId);
            MDC.put("userId", getCurrentUserId());
            
            chain.doFilter(request, response);
        }
    }
}
```

### Alert Configuration Standards

#### Critical Path Alerts (Required)

```yaml
# Prometheus Alert Rules
groups:
  - name: customer-service-alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.01
        for: 2m
        labels:
          severity: critical
          service: customer-service
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value }} errors per second"
          
      - alert: HighResponseTime
        expr: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])) > 2
        for: 5m
        labels:
          severity: warning
          service: customer-service
        annotations:
          summary: "High response time detected"
          description: "95th percentile response time is {{ $value }} seconds"
          
      - alert: DatabaseConnectionPoolExhausted
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
        for: 1m
        labels:
          severity: critical
          service: customer-service
        annotations:
          summary: "Database connection pool nearly exhausted"
          description: "Connection pool usage is {{ $value }}%"
```

#### Business Metrics Alerts

```yaml
  - name: business-metrics-alerts
    rules:
      - alert: CustomerCreationRateDropped
        expr: rate(customers_created_total[10m]) < 0.1
        for: 5m
        labels:
          severity: warning
          team: product
        annotations:
          summary: "Customer creation rate has dropped significantly"
          
      - alert: HighCustomerChurnRate
        expr: rate(customers_churned_total[1h]) / rate(customers_created_total[1h]) > 0.1
        for: 10m
        labels:
          severity: critical
          team: product
        annotations:
          summary: "Customer churn rate is unusually high"
```

### Dashboard Requirements

#### Technical Dashboard (Required for each service)

- **Response Time**: 95th percentile over time
- **Error Rate**: 4xx and 5xx errors per minute
- **Throughput**: Requests per second
- **Resource Usage**: CPU, Memory, Database connections
- **Dependency Health**: External service response times

#### Business Dashboard (Required for each bounded context)

- **Key Business Metrics**: Orders, customers, revenue
- **Conversion Rates**: Funnel analysis
- **User Behavior**: Page views, session duration
- **Business Process Health**: Success rates, completion times

### Health Check Standards

#### Application Health Checks

```java
@Component
public class CustomerServiceHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        try {
            // Check database connectivity
            customerRepository.count();
            
            // Check external dependencies
            paymentService.healthCheck();
            
            // Check business logic health
            validateBusinessRules();
            
            return Health.up()
                .withDetail("database", "UP")
                .withDetail("payment-service", "UP")
                .withDetail("business-rules", "VALID")
                .build();
                
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

#### Infrastructure Health Checks

```yaml
# Kubernetes Liveness and Readiness Probes
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
  timeoutSeconds: 3
  failureThreshold: 3
```

## Four Perspectives Checklist

### Security Perspective

- [ ] API endpoints pass CDK Nag checks
- [ ] Sensitive data encrypted in storage and transit
- [ ] Authentication and authorization mechanisms
- [ ] Security event logging and monitoring

### Performance & Scalability Perspective

- [ ] Critical path performance benchmarks (< 2s)
- [ ] Database query optimization and indexing strategy
- [ ] Caching strategy implementation
- [ ] Horizontal scaling capability verification

### Availability & Resilience Perspective

- [ ] Health check endpoints implemented
- [ ] Failure recovery and retry mechanisms
- [ ] Circuit breaker pattern implementation
- [ ] Disaster recovery plan and testing

### Evolution Perspective

- [ ] Interface backward compatibility guaranteed
- [ ] Version management strategy implemented
- [ ] Modular and loosely coupled design
- [ ] Refactoring safety guaranteed (test coverage)

## Concurrency Strategy Requirements

### Asynchronous processing design must specify

- Event processing order dependencies
- Transaction boundaries and consistency guarantees
- Concurrency conflict detection and handling mechanisms
- Deadlock prevention and detection strategies

## Mandatory Resilience Patterns

### External service calls must implement

- Circuit breaker pattern
- Retry mechanism (max 3 attempts, exponential backoff)
- Fallback strategy
- Dead letter queue handling

### Critical business processes must have

- Failure recovery time testing
- Monitoring and alerting configuration
- Operations manual updates

## Technology Evolution Standards

### New technology introduction must satisfy

- [ ] Technology maturity reaches "Growth" stage or above
- [ ] Complete documentation and community support
- [ ] Team learning and maintenance capability
- [ ] Migration risk controllable with rollback plan

### Version upgrade requirements

- Critical dependency upgrades must have automated test coverage
- Major version upgrades must be verified in test environment
- Legacy technology retirement must have clear timeline

## Compliance Monitoring Metrics

- Viewpoint coverage rate: 100%
- Quality attribute scenario coverage rate: 100%
- ArchUnit test pass rate: 100%
- Architecture debt trend: Continuously decreasing
