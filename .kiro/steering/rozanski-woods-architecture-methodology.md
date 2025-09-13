---
inclusion: always
---

# Rozanski & Woods Architecture Methodology Steering Rules

## Mandatory Architectural Viewpoint Checks

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

## Quality Attribute Scenario Requirements

### Each user story must include at least one quality attribute scenario

#### Scenario Format: Source → Stimulus → Environment → Artifact → Response → Response Measure

#### Quantitative Metrics Requirements

- Performance scenarios: Specific time, throughput, or resource usage metrics
- Security scenarios: Must pass CDK Nag rule validation
- Availability scenarios: Must include RTO and RPO
- Scalability scenarios: Define load growth and scaling strategies

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

### Each ADR must include

- Stakeholder impact analysis
- Impact radius assessment (Local/Bounded Context/System/Enterprise)
- Risk assessment (High/Medium/Low)
- Rollback strategy and trigger conditions
- Migration path (Phase 1/2/3)

## Observability Requirements

### Mandatory for new features

- Each aggregate root must have corresponding business metrics
- Each use case must have execution tracing and performance metrics
- Each domain event must have publication and processing metrics
- Critical paths must have monitoring and alerting

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
