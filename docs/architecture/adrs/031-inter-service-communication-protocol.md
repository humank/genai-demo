---
adr_number: 031
title: "Inter-Service Communication Protocol"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [2, 3, 5, 9, 25, 30]
affected_viewpoints: ["functional", "concurrency", "deployment"]
affected_perspectives: ["performance", "availability", "evolution"]
decision_makers: ["Architecture Team", "Backend Team"]
---

# ADR-031: Inter-Service Communication Protocol

## Status

**Status**: Accepted

**Date**: 2025-10-25

**Decision Makers**: Architecture Team, Backend Team

## Context

### Problem Statement

The Enterprise E-Commerce Platform consists of 13 bounded contexts that may be deployed as separate microservices. We need to decide on inter-service communication protocols to enable:
- Synchronous request-response communication
- Asynchronous event-driven communication
- Service-to-service data exchange
- Cross-context business process coordination

This decision impacts:
- System performance and latency
- Service coupling and independence
- Data consistency and eventual consistency
- Fault tolerance and resilience
- Development complexity
- Operational overhead

### Business Context

**Business Drivers**:
- Fast response times for user-facing operations
- Reliable order processing across multiple services
- Scalable architecture supporting business growth
- Loose coupling between bounded contexts
- Support for eventual consistency where appropriate
- Enable independent service deployment

**Business Constraints**:
- Order processing requires coordination across multiple services (Order, Inventory, Payment, Shipping)
- Some operations require immediate consistency (payment processing)
- Some operations can tolerate eventual consistency (email notifications, analytics)
- Peak traffic: 10,000 requests/second
- Response time requirement: < 2 seconds for user-facing operations
- 99.9% availability requirement

**Business Requirements**:
- Support synchronous operations for immediate responses
- Support asynchronous operations for long-running processes
- Enable event-driven architecture for loose coupling
- Maintain data consistency across services
- Support service independence and scalability

### Technical Context

**Current Architecture**:
- Backend: Spring Boot microservices (13 bounded contexts)
- Architecture: Hexagonal Architecture with DDD (ADR-002)
- Event System: Domain Events (ADR-003)
- Message Broker: Apache Kafka (ADR-005)
- API Design: RESTful APIs (ADR-009)
- Transaction Management: Saga Pattern (ADR-025)
- API Gateway: Kong Gateway (ADR-030)

**Technical Constraints**:
- Must support Spring Boot ecosystem
- Must integrate with existing Kafka infrastructure
- Must work with Kong API Gateway
- Must support distributed transactions (Saga pattern)
- Must provide low latency for synchronous calls
- Must ensure message delivery for asynchronous calls

**Dependencies**:
- ADR-002: Hexagonal Architecture (service boundaries)
- ADR-003: Domain Events (event-driven communication)
- ADR-005: Apache Kafka (message broker)
- ADR-009: RESTful API Design (API standards)
- ADR-025: Saga Pattern (distributed transactions)
- ADR-030: API Gateway Pattern (external API access)

## Decision Drivers

- **Performance**: Low latency for synchronous operations
- **Reliability**: Guaranteed message delivery for asynchronous operations
- **Scalability**: Support high throughput and horizontal scaling
- **Coupling**: Minimize coupling between services
- **Consistency**: Support both strong and eventual consistency
- **Complexity**: Balance between functionality and simplicity
- **Developer Experience**: Familiar technologies and patterns

## Considered Options

### Option 1: REST for Sync + Kafka for Async (Hybrid Approach)

**Description**:
Use RESTful HTTP APIs for synchronous request-response communication and Apache Kafka for asynchronous event-driven communication.

**Pros** ✅:
- **Clear Separation**: Synchronous vs asynchronous communication clearly separated
- **Familiar Technologies**: REST and Kafka are well-known and widely adopted
- **Flexibility**: Choose appropriate protocol for each use case
- **Existing Infrastructure**: Leverage existing REST APIs and Kafka setup
- **Tooling**: Excellent tooling and ecosystem support
- **Debugging**: Easy to debug and monitor both protocols
- **Spring Integration**: Native Spring support for both REST and Kafka
- **Event-Driven**: Kafka enables true event-driven architecture

**Cons** ❌:
- **Dual Protocol**: Need to manage two different communication protocols
- **Complexity**: More complex than single protocol approach
- **Latency**: REST has higher latency than gRPC for sync calls
- **Payload Size**: REST JSON payloads larger than binary protocols
- **Learning Curve**: Team needs expertise in both REST and Kafka

**Cost**:
- **Implementation Cost**: 2 person-weeks (configure REST clients and Kafka producers/consumers)
- **Infrastructure Cost**: $500/month (Kafka MSK cluster already exists)
- **Maintenance Cost**: 1 person-day/month
- **Total Cost of Ownership (3 years)**: ~$25,000

**Risk**: Low

**Risk Description**: Proven technologies with extensive production usage

**Effort**: Low

**Effort Description**: Leverage existing REST and Kafka infrastructure

### Option 2: gRPC for Sync + Kafka for Async

**Description**:
Use gRPC for synchronous request-response communication and Apache Kafka for asynchronous event-driven communication.

**Pros** ✅:
- **Performance**: gRPC provides lower latency and higher throughput than REST
- **Type Safety**: Protocol Buffers provide strong typing and schema validation
- **Efficient**: Binary serialization reduces payload size
- **Streaming**: Built-in support for bidirectional streaming
- **Code Generation**: Automatic client/server code generation
- **HTTP/2**: Multiplexing, header compression, server push
- **Event-Driven**: Kafka enables event-driven architecture

**Cons** ❌:
- **Learning Curve**: Team needs to learn gRPC and Protocol Buffers
- **Debugging**: Binary protocol harder to debug than JSON
- **Browser Support**: Limited browser support (requires gRPC-Web)
- **Tooling**: Less mature tooling compared to REST
- **API Gateway**: Kong Gateway has limited gRPC support
- **Migration**: Requires migrating existing REST APIs to gRPC
- **Complexity**: More complex than REST for simple use cases

**Cost**:
- **Implementation Cost**: 6 person-weeks (migrate to gRPC, train team)
- **Infrastructure Cost**: $500/month (Kafka MSK)
- **Maintenance Cost**: 1.5 person-days/month
- **Total Cost of Ownership (3 years)**: ~$40,000

**Risk**: Medium

**Risk Description**: Team unfamiliar with gRPC, migration complexity

**Effort**: High

**Effort Description**: Significant migration effort, team training required

### Option 3: Kafka for All Communication (Event-Driven Only)

**Description**:
Use Apache Kafka for all inter-service communication, including both synchronous and asynchronous patterns.

**Pros** ✅:
- **Single Protocol**: Unified communication protocol
- **Event-Driven**: True event-driven architecture
- **Decoupling**: Maximum decoupling between services
- **Scalability**: Kafka scales horizontally
- **Durability**: Messages persisted and replayed
- **Audit Trail**: Complete event history for debugging

**Cons** ❌:
- **Latency**: Higher latency for request-response patterns
- **Complexity**: Request-response over Kafka is complex
- **Correlation**: Need to correlate requests and responses
- **Timeout Handling**: Complex timeout and error handling
- **Debugging**: Harder to debug than direct HTTP calls
- **Not Suitable**: Poor fit for synchronous operations
- **Learning Curve**: Team needs deep Kafka expertise

**Cost**:
- **Implementation Cost**: 8 person-weeks (implement request-response over Kafka)
- **Infrastructure Cost**: $500/month (Kafka MSK)
- **Maintenance Cost**: 2 person-days/month
- **Total Cost of Ownership (3 years)**: ~$50,000

**Risk**: High

**Risk Description**: Complex implementation, not suitable for synchronous operations

**Effort**: High

**Effort Description**: Significant implementation complexity

## Decision Outcome

**Chosen Option**: Option 1 - REST for Sync + Kafka for Async (Hybrid Approach)

**Rationale**:
We chose a hybrid approach using REST for synchronous communication and Kafka for asynchronous communication. This decision balances performance, simplicity, and flexibility:

1. **Clear Use Case Separation**: REST for immediate request-response (query customer, check inventory) and Kafka for event-driven workflows (order processing, notifications). Each protocol optimized for its use case.

2. **Leverage Existing Infrastructure**: We already have RESTful APIs (ADR-009) and Kafka infrastructure (ADR-005). No need to migrate or introduce new technologies.

3. **Team Familiarity**: Team has strong expertise in REST and Kafka. No learning curve for gRPC or complex Kafka request-response patterns.

4. **Proven Pattern**: REST + Kafka is a proven pattern used by major e-commerce platforms (Amazon, eBay, Shopify). Well-documented best practices and troubleshooting guides.

5. **Flexibility**: Choose appropriate protocol for each use case. Synchronous operations (get customer profile) use REST. Asynchronous workflows (order processing) use Kafka events.

6. **Debugging and Monitoring**: REST calls easy to debug with HTTP tools. Kafka messages easy to inspect with Kafka tools. Both integrate well with existing observability stack (ADR-008).

7. **API Gateway Integration**: Kong Gateway (ADR-030) has excellent REST support. Frontend applications continue using REST APIs through gateway.

8. **Gradual Evolution**: Can introduce gRPC for specific high-performance use cases in the future without major architectural changes.

**Communication Pattern Guidelines**:

**Use REST (Synchronous) When**:
- Immediate response required (query operations)
- Simple request-response pattern
- External API calls (third-party integrations)
- Frontend-to-backend communication
- Low latency requirement (< 100ms)
- Examples: Get customer profile, check product availability, validate coupon

**Use Kafka (Asynchronous) When**:
- Event notification (something happened)
- Long-running processes (order fulfillment)
- Cross-context workflows (saga pattern)
- Eventual consistency acceptable
- High throughput required
- Examples: Order submitted, payment processed, inventory updated, email sent

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation Strategy |
|-------------|--------------|-------------|-------------------|
| Backend Team | Medium | Need to choose appropriate protocol for each use case | Communication pattern guidelines, code examples |
| Frontend Team | Low | Continue using REST APIs through Kong Gateway | No changes required |
| DevOps Team | Low | Monitor both REST and Kafka communication | Unified observability dashboard |
| QA Team | Medium | Test both synchronous and asynchronous flows | Testing guidelines for both protocols |

### Impact Radius Assessment

**Selected Impact Radius**: System

**Impact Description**:
- **System**: Communication patterns affect all services
  - All services implement REST endpoints for synchronous operations
  - All services publish/consume Kafka events for asynchronous operations
  - All services follow communication pattern guidelines
  - All services integrate with Kong Gateway for external access

### Affected Components

- **All Backend Services**: Implement both REST and Kafka communication
- **Kong Gateway**: Routes REST API calls
- **Kafka Cluster**: Handles asynchronous event communication
- **Observability Stack**: Monitors both REST and Kafka metrics
- **Testing Framework**: Tests both synchronous and asynchronous flows

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy | Owner |
|------|-------------|--------|-------------------|-------|
| Protocol misuse | Medium | Medium | Clear guidelines, code reviews | Architecture Team |
| REST performance issues | Low | Medium | Caching, optimization | Backend Team |
| Kafka message loss | Low | High | Kafka replication, monitoring | DevOps Team |
| Debugging complexity | Medium | Low | Unified observability, tracing | DevOps Team |
| Inconsistent patterns | Medium | Medium | Code templates, reviews | Architecture Team |

**Overall Risk Level**: Low

**Risk Mitigation Plan**:
- Provide clear communication pattern guidelines
- Create code templates for REST clients and Kafka producers/consumers
- Implement distributed tracing across REST and Kafka
- Regular architecture reviews to ensure pattern compliance
- Automated tests for both synchronous and asynchronous flows

## Implementation Plan

### Phase 1: Communication Pattern Guidelines (Timeline: Week 1)

**Objectives**:
- Define when to use REST vs Kafka
- Create communication pattern guidelines
- Document best practices

**Tasks**:
- [ ] Document REST usage guidelines (synchronous operations)
- [ ] Document Kafka usage guidelines (asynchronous operations)
- [ ] Create decision tree for protocol selection
- [ ] Define service-to-service REST API standards
- [ ] Define Kafka event naming conventions
- [ ] Document error handling for both protocols
- [ ] Create code examples for common patterns

**Deliverables**:
- Communication pattern guidelines document
- Protocol selection decision tree
- Code examples and templates

**Success Criteria**:
- Clear guidelines for when to use each protocol
- Team understands protocol selection criteria

### Phase 2: REST Client Implementation (Timeline: Week 1-2)

**Objectives**:
- Implement REST clients for service-to-service calls
- Configure timeouts and retries
- Integrate with observability

**Tasks**:
- [ ] Create RestTemplate/WebClient configuration
- [ ] Configure connection pooling and timeouts
- [ ] Implement circuit breaker for REST calls (Resilience4j)
- [ ] Add distributed tracing (X-Ray) to REST clients
- [ ] Implement retry logic with exponential backoff
- [ ] Create REST client code templates
- [ ] Document REST client usage

**Deliverables**:
- Configured REST clients with resilience patterns
- Code templates for REST communication
- Documentation

**Success Criteria**:
- REST clients working with circuit breaker and retries
- Distributed tracing working across services

### Phase 3: Kafka Producer/Consumer Implementation (Timeline: Week 2-3)

**Objectives**:
- Implement Kafka producers for event publishing
- Implement Kafka consumers for event handling
- Ensure reliable message delivery

**Tasks**:
- [ ] Configure Kafka producers with idempotency
- [ ] Configure Kafka consumers with error handling
- [ ] Implement dead letter queue for failed messages
- [ ] Add distributed tracing (X-Ray) to Kafka messages
- [ ] Implement event versioning strategy
- [ ] Create Kafka producer/consumer code templates
- [ ] Document Kafka usage patterns

**Deliverables**:
- Configured Kafka producers and consumers
- Dead letter queue implementation
- Code templates and documentation

**Success Criteria**:
- Kafka messages delivered reliably
- Failed messages routed to dead letter queue
- Distributed tracing working for Kafka events

### Phase 4: Testing and Validation (Timeline: Week 3-4)

**Objectives**:
- Test synchronous and asynchronous flows
- Validate performance and reliability
- Create testing guidelines

**Tasks**:
- [ ] Create integration tests for REST communication
- [ ] Create integration tests for Kafka communication
- [ ] Test circuit breaker and retry behavior
- [ ] Test Kafka message delivery and error handling
- [ ] Perform load testing for both protocols
- [ ] Validate distributed tracing end-to-end
- [ ] Create testing guidelines and examples
- [ ] Document troubleshooting procedures

**Deliverables**:
- Comprehensive integration tests
- Load testing results
- Testing guidelines

**Success Criteria**:
- All tests passing
- Performance targets met (REST < 100ms, Kafka throughput > 10,000 msg/s)
- Distributed tracing working end-to-end

### Rollback Strategy

**Trigger Conditions**:
- Critical performance degradation (> 50% latency increase)
- Message loss or data inconsistency
- Service communication failures
- Team unable to implement patterns correctly

**Rollback Steps**:
1. **Immediate Action**: Revert to previous communication patterns
2. **Service Rollback**: Deploy previous service versions
3. **Configuration Rollback**: Restore previous REST/Kafka configurations
4. **Validation**: Verify services communicating correctly
5. **Root Cause Analysis**: Investigate issues and plan remediation

**Rollback Time**: 15-30 minutes

**Rollback Testing**: Test rollback procedure in staging environment

## Monitoring and Success Criteria

### Success Metrics

| Metric | Target | Measurement Method | Review Frequency |
|--------|--------|-------------------|------------------|
| REST Latency (p95) | < 100ms | Spring Boot Actuator metrics | Real-time |
| REST Success Rate | > 99.5% | HTTP status codes | Daily |
| Kafka Throughput | > 10,000 msg/s | Kafka metrics | Real-time |
| Kafka Consumer Lag | < 1000 messages | Kafka consumer metrics | Real-time |
| Message Delivery Rate | 100% | Kafka producer metrics | Daily |

### Monitoring Plan

**Dashboards**:
- **REST Communication Dashboard**: Latency, success rate, circuit breaker status
- **Kafka Communication Dashboard**: Throughput, consumer lag, message delivery
- **Service Communication Map**: Visualize service dependencies and communication patterns

**Alerts**:
- **Critical**: REST success rate < 95% (PagerDuty)
- **Critical**: Kafka consumer lag > 10,000 messages (PagerDuty)
- **Warning**: REST latency > 200ms p95 (Slack)
- **Warning**: Kafka message delivery failure (Slack)

**Review Schedule**:
- **Real-time**: Automated monitoring and alerting
- **Daily**: Review communication metrics and errors
- **Weekly**: Analyze communication patterns and optimize
- **Monthly**: Comprehensive performance review

### Key Performance Indicators (KPIs)

- **Performance KPI**: REST latency < 100ms (p95), Kafka throughput > 10,000 msg/s
- **Reliability KPI**: REST success rate > 99.5%, Kafka delivery rate 100%
- **Scalability KPI**: Support 10,000 req/s peak load
- **Observability KPI**: 100% distributed tracing coverage

## Consequences

### Positive Consequences ✅

- **Flexibility**: Choose appropriate protocol for each use case
- **Performance**: REST provides low latency for synchronous operations
- **Scalability**: Kafka provides high throughput for asynchronous operations
- **Decoupling**: Kafka events enable loose coupling between services
- **Familiarity**: Team already knows REST and Kafka
- **Tooling**: Excellent tooling and ecosystem support
- **Debugging**: Easy to debug and monitor both protocols
- **Evolution**: Can introduce gRPC for specific use cases in future

### Negative Consequences ❌

- **Dual Protocol**: Need to manage two communication protocols (Mitigation: Clear guidelines and templates)
- **Complexity**: More complex than single protocol (Mitigation: Documentation and training)
- **Protocol Selection**: Need to choose correct protocol for each use case (Mitigation: Decision tree and guidelines)

### Technical Debt

**Debt Introduced**:
- **Protocol Expertise**: Team needs expertise in both REST and Kafka
- **Monitoring Complexity**: Need to monitor two different protocols
- **Testing Complexity**: Need to test both synchronous and asynchronous flows

**Debt Repayment Plan**:
- **Training**: Regular training on REST and Kafka best practices
- **Documentation**: Maintain comprehensive communication pattern guidelines
- **Automation**: Automate testing for both protocols
- **Monitoring**: Unified observability dashboard for both protocols

### Long-term Implications

This decision establishes REST + Kafka as our inter-service communication strategy for the next 3-5 years. As the platform evolves:
- Consider gRPC for specific high-performance use cases
- Evaluate GraphQL for flexible client queries
- Monitor communication patterns and optimize
- Keep REST and Kafka libraries updated
- Reassess if performance requirements change significantly

The hybrid approach provides flexibility to evolve communication patterns while maintaining simplicity and team productivity.

## Related Decisions

### Related ADRs
- [ADR-002: Hexagonal Architecture](002-adopt-hexagonal-architecture.md) - Service boundaries
- [ADR-003: Domain Events](003-use-domain-events-for-cross-context-communication.md) - Event-driven communication
- [ADR-005: Apache Kafka](005-use-apache-kafka-for-event-streaming.md) - Message broker
- [ADR-009: RESTful API Design](009-restful-api-design-with-openapi.md) - API standards
- [ADR-025: Saga Pattern](025-saga-pattern-distributed-transactions.md) - Distributed transactions
- [ADR-030: API Gateway Pattern](030-api-gateway-pattern.md) - External API access

### Affected Viewpoints
- [Functional Viewpoint](../../viewpoints/functional/README.md) - Service interactions
- [Concurrency Viewpoint](../../viewpoints/concurrency/README.md) - Synchronous vs asynchronous
- [Deployment Viewpoint](../../viewpoints/deployment/README.md) - Service deployment

### Affected Perspectives
- [Performance Perspective](../../perspectives/performance/README.md) - Communication latency
- [Availability Perspective](../../perspectives/availability/README.md) - Fault tolerance
- [Evolution Perspective](../../perspectives/evolution/README.md) - Protocol evolution

## Notes

### Assumptions
- Team has REST and Kafka expertise
- Kong Gateway supports REST routing
- Kafka cluster available and reliable
- Services deployed on AWS EKS
- Distributed tracing infrastructure available

### Constraints
- Must support Spring Boot ecosystem
- Must integrate with Kong API Gateway
- Must work with existing Kafka infrastructure
- Must provide low latency for synchronous calls
- Must ensure message delivery for asynchronous calls

### Open Questions
- Should we introduce gRPC for specific high-performance use cases?
- What is optimal timeout for REST calls between services?
- How to handle Kafka schema evolution?
- Should we use Kafka Streams for complex event processing?

### Follow-up Actions
- [ ] Create communication pattern guidelines - Architecture Team
- [ ] Implement REST client templates - Backend Team
- [ ] Implement Kafka producer/consumer templates - Backend Team
- [ ] Create integration tests for both protocols - QA Team
- [ ] Set up monitoring dashboards - DevOps Team
- [ ] Conduct training on communication patterns - Tech Lead

### References
- [Microservices Communication Patterns](https://microservices.io/patterns/communication-style/messaging.html)
- [REST vs gRPC Comparison](https://cloud.google.com/blog/products/api-management/understanding-grpc-openapi-and-rest)
- [Kafka for Microservices](https://www.confluent.io/blog/apache-kafka-for-service-architectures/)
- [Spring Cloud OpenFeign](https://spring.io/projects/spring-cloud-openfeign)
- [Spring Kafka](https://spring.io/projects/spring-kafka)
- [Resilience4j Circuit Breaker](https://resilience4j.readme.io/docs/circuitbreaker)

---

**ADR Template Version**: 1.0  
**Last Template Update**: 2025-01-17
