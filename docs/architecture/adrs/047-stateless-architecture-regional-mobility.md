---
adr_number: 047
title: "Stateless Architecture for Regional Mobility"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [037, 038, 040, 046]
affected_viewpoints: ["deployment", "development", "concurrency"]
affected_perspectives: ["availability", "performance", "evolution"]
---

# ADR-047: Stateless Architecture for Regional Mobility

## Status

**Accepted** - 2025-10-25

## Context

### Problem Statement

Multi-region active-active architecture requires seamless regional mobility, but stateful components create barriers:

**Stateful Architecture Challenges**:

- **Session Affinity**: Users tied to specific regions
- **In-Memory State**: Lost during failover
- **Local Caching**: Inconsistent across regions
- **File Storage**: Region-specific file systems
- **Connection Pooling**: Region-bound connections
- **Scheduled Jobs**: Duplicate execution across regions

**Impact on Regional Mobility**:

- Slow failover (need to migrate state)
- User session loss during failover
- Inconsistent user experience
- Complex failover procedures
- Limited load balancing flexibility
- Difficult testing and deployment

**Current State Issues**:

- Some services maintain in-memory session state
- Local file storage for uploads
- Region-specific caching
- Stateful WebSocket connections
- Background jobs with local state

### Business Context

**Business Drivers**:

- Seamless failover (< 5 minutes)
- Consistent user experience
- Global load balancing
- Simplified operations
- Cost optimization
- Scalability

**Constraints**:

- Cannot break existing functionality
- Must maintain performance
- Minimize refactoring effort
- Support gradual migration
- Budget: $30,000 for implementation

### Technical Context

**Current State**:

- Mix of stateful and stateless services
- Session state in application memory
- Local file storage
- Region-specific caches
- Manual failover procedures

**Requirements**:

- All services must be stateless
- Externalized session management
- Distributed caching
- Shared file storage
- Idempotent operations
- Automated failover

## Decision Drivers

1. **Regional Mobility**: Enable seamless cross-region failover
2. **Scalability**: Support horizontal scaling
3. **Availability**: Improve failover speed (< 5 minutes)
4. **Consistency**: Ensure consistent user experience
5. **Simplicity**: Simplify operations and deployment
6. **Performance**: Maintain or improve performance
7. **Cost**: Optimize infrastructure costs
8. **Evolution**: Support future architectural changes

## Considered Options

### Option 1: Comprehensive Stateless Architecture (Recommended)

**Description**: Implement fully stateless architecture with externalized state management

**Architecture Principles**:

**1. Stateless Application Services**:

```typescript
// ❌ BAD: Stateful service with in-memory session
@Service
class OrderService {
  private userSessions: Map<string, UserSession> = new Map();
  
  processOrder(userId: string, order: Order) {
    const session = this.userSessions.get(userId);  // State in memory!
    // Process order using session
  }
}

// ✅ GOOD: Stateless service with externalized session
@Service
class OrderService {
  constructor(
    private sessionStore: RedisSessionStore,
    private orderRepository: OrderRepository
  ) {}
  
  async processOrder(sessionId: string, order: Order) {
    const session = await this.sessionStore.get(sessionId);  // External state
    // Process order using session
  }
}
```

**2. Externalized Session Management**:

```typescript
interface SessionManagementStrategy {
  storage: {
    primary: 'Redis (ElastiCache Global Datastore)',
    backup: 'DynamoDB Global Tables',
    ttl: '24 hours',
    replication: 'Cross-region automatic'
  },
  
  sessionData: {
    userId: 'string',
    authToken: 'JWT',
    preferences: 'object',
    cart: 'reference to cart service',
    lastActivity: 'timestamp'
  },
  
  implementation: {
    library: 'Spring Session with Redis',
    serialization: 'JSON',
    compression: 'gzip',
    encryption: 'AES-256'
  }
}

// Spring Session configuration
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 86400)
class SessionConfiguration {
  
  @Bean
  fun redisConnectionFactory(): RedisConnectionFactory {
    return LettuceConnectionFactory(
      RedisStandaloneConfiguration(
        redisHost,
        redisPort
      )
    )
  }
  
  @Bean
  fun cookieSerializer(): CookieSerializer {
    val serializer = DefaultCookieSerializer()
    serializer.setCookieName("SESSION")
    serializer.setCookiePath("/")
    serializer.setDomainNamePattern("^.+?\\.(\\w+\\.[a-z]+)$")
    serializer.setSameSite("Lax")
    serializer.setUseSecureCookie(true)
    serializer.setUseHttpOnlyCookie(true)
    return serializer
  }
}
```

**3. Distributed Caching Strategy**:

```typescript
interface DistributedCachingStrategy {
  architecture: {
    global: 'ElastiCache Global Datastore',
    regions: ['Taiwan', 'Tokyo', 'Singapore'],
    replication: 'Automatic cross-region',
    consistency: 'Eventually consistent (< 1 second)'
  },
  
  cachePatterns: {
    readThrough: {
      description: 'Cache miss loads from database',
      implementation: '@Cacheable annotation',
      ttl: 'Varies by data type'
    },
    
    writeThrough: {
      description: 'Write to cache and database',
      implementation: '@CachePut annotation',
      consistency: 'Strong'
    },
    
    cacheAside: {
      description: 'Application manages cache',
      implementation: 'Manual cache operations',
      flexibility: 'High'
    }
  },
  
  cacheKeys: {
    format: '{service}:{entity}:{id}',
    examples: [
      'product:item:12345',
      'customer:profile:user-789',
      'order:summary:order-456'
    ]
  }
}

// Cache configuration
@Configuration
@EnableCaching
class CacheConfiguration {
  
  @Bean
  fun cacheManager(
    connectionFactory: RedisConnectionFactory
  ): CacheManager {
    val config = RedisCacheConfiguration.defaultCacheConfig()
      .entryTtl(Duration.ofHours(1))
      .serializeKeysWith(
        RedisSerializationContext.SerializationPair
          .fromSerializer(StringRedisSerializer())
      )
      .serializeValuesWith(
        RedisSerializationContext.SerializationPair
          .fromSerializer(GenericJackson2JsonRedisSerializer())
      )
    
    return RedisCacheManager.builder(connectionFactory)
      .cacheDefaults(config)
      .withCacheConfiguration("products", 
        config.entryTtl(Duration.ofMinutes(30)))
      .withCacheConfiguration("customers",
        config.entryTtl(Duration.ofHours(2)))
      .build()
  }
}
```

**4. Shared File Storage**:

```typescript
interface SharedFileStorageStrategy {
  storage: {
    service: 'Amazon S3',
    replication: 'Cross-region replication',
    consistency: 'Strong read-after-write',
    availability: '99.99%'
  },
  
  structure: {
    buckets: {
      uploads: 's3://ecommerce-uploads-{region}',
      assets: 's3://ecommerce-assets-{region}',
      backups: 's3://ecommerce-backups-{region}'
    },
    
    replication: {
      source: 'Taiwan',
      destinations: ['Tokyo', 'Singapore'],
      mode: 'Automatic',
      time: '< 15 minutes'
    }
  },
  
  access: {
    method: 'Pre-signed URLs',
    expiration: '1 hour',
    permissions: 'Least privilege',
    cdn: 'CloudFront for public assets'
  }
}

// S3 service implementation
@Service
class FileStorageService(
  private val s3Client: S3Client,
  private val bucketName: String
) {
  
  fun uploadFile(
    key: String,
    file: MultipartFile
  ): String {
    val putRequest = PutObjectRequest.builder()
      .bucket(bucketName)
      .key(key)
      .contentType(file.contentType)
      .build()
    
    s3Client.putObject(
      putRequest,
      RequestBody.fromInputStream(
        file.inputStream,
        file.size
      )
    )
    
    return generatePresignedUrl(key)
  }
  
  fun generatePresignedUrl(key: String): String {
    val getRequest = GetObjectRequest.builder()
      .bucket(bucketName)
      .key(key)
      .build()
    
    val presignRequest = GetObjectPresignRequest.builder()
      .signatureDuration(Duration.ofHours(1))
      .getObjectRequest(getRequest)
      .build()
    
    return s3Presigner.presignGetObject(presignRequest)
      .url()
      .toString()
  }
}
```

**5. Idempotent Operations**:

```typescript
interface IdempotencyStrategy {
  implementation: {
    method: 'Idempotency keys',
    storage: 'Redis with 24-hour TTL',
    header: 'Idempotency-Key',
    format: 'UUID v4'
  },
  
  operations: {
    payments: 'Required',
    orders: 'Required',
    inventory: 'Required',
    notifications: 'Recommended',
    queries: 'Not needed'
  }
}

// Idempotency implementation
@Service
class IdempotentOrderService(
  private val orderRepository: OrderRepository,
  private val idempotencyStore: RedisTemplate<String, String>
) {
  
  @Transactional
  fun createOrder(
    idempotencyKey: String,
    orderRequest: CreateOrderRequest
  ): Order {
    // Check if already processed
    val existingOrderId = idempotencyStore
      .opsForValue()
      .get("idempotency:$idempotencyKey")
    
    if (existingOrderId != null) {
      return orderRepository.findById(existingOrderId)
        .orElseThrow()
    }
    
    // Process order
    val order = Order.create(orderRequest)
    val savedOrder = orderRepository.save(order)
    
    // Store idempotency key
    idempotencyStore.opsForValue().set(
      "idempotency:$idempotencyKey",
      savedOrder.id,
      24,
      TimeUnit.HOURS
    )
    
    return savedOrder
  }
}

// API endpoint with idempotency
@PostMapping("/orders")
fun createOrder(
  @RequestHeader("Idempotency-Key") idempotencyKey: String,
  @RequestBody request: CreateOrderRequest
): ResponseEntity<OrderResponse> {
  val order = orderService.createOrder(idempotencyKey, request)
  return ResponseEntity.ok(OrderResponse.from(order))
}
```

**6. Stateless Background Jobs**:

```typescript
interface StatelessJobStrategy {
  scheduler: {
    service: 'AWS EventBridge',
    distribution: 'Single execution per schedule',
    failover: 'Automatic region failover'
  },
  
  execution: {
    method: 'Lambda or ECS Fargate',
    state: 'No local state',
    coordination: 'DynamoDB for distributed locks',
    idempotency: 'Required for all jobs'
  },
  
  examples: {
    dailyReport: {
      schedule: 'cron(0 2 * * ? *)',
      execution: 'Lambda',
      state: 'Query from database',
      output: 'S3'
    },
    
    orderCleanup: {
      schedule: 'rate(1 hour)',
      execution: 'ECS Fargate',
      state: 'Query from database',
      lock: 'DynamoDB distributed lock'
    }
  }
}

// Distributed lock for job coordination
@Service
class DistributedLockService(
  private val dynamoDb: DynamoDbClient
) {
  
  fun acquireLock(
    lockName: String,
    ttl: Duration
  ): Boolean {
    try {
      val item = mapOf(
        "lockName" to AttributeValue.builder().s(lockName).build(),
        "owner" to AttributeValue.builder().s(instanceId).build(),
        "expiresAt" to AttributeValue.builder()
          .n(Instant.now().plus(ttl).epochSecond.toString())
          .build()
      )
      
      dynamoDb.putItem(
        PutItemRequest.builder()
          .tableName("distributed-locks")
          .item(item)
          .conditionExpression("attribute_not_exists(lockName)")
          .build()
      )
      
      return true
    } catch (e: ConditionalCheckFailedException) {
      return false
    }
  }
  
  fun releaseLock(lockName: String) {
    dynamoDb.deleteItem(
      DeleteItemRequest.builder()
        .tableName("distributed-locks")
        .key(mapOf(
          "lockName" to AttributeValue.builder().s(lockName).build()
        ))
        .conditionExpression("owner = :owner")
        .expressionAttributeValues(mapOf(
          ":owner" to AttributeValue.builder().s(instanceId).build()
        ))
        .build()
    )
  }
}
```

**7. Stateless WebSocket Connections**:

```typescript
interface StatelessWebSocketStrategy {
  architecture: {
    gateway: 'AWS API Gateway WebSocket',
    backend: 'Lambda or ECS',
    state: 'DynamoDB for connection tracking',
    messaging: 'SNS/SQS for pub/sub'
  },
  
  connectionManagement: {
    storage: 'DynamoDB',
    schema: {
      connectionId: 'string (partition key)',
      userId: 'string (GSI)',
      region: 'string',
      connectedAt: 'timestamp',
      lastActivity: 'timestamp'
    }
  },
  
  messageRouting: {
    method: 'SNS topic per message type',
    fanout: 'All regions receive messages',
    delivery: 'At-least-once',
    deduplication: 'Message ID tracking'
  }
}

// WebSocket connection handler
@Service
class WebSocketConnectionService(
  private val dynamoDb: DynamoDbClient,
  private val apiGatewayClient: ApiGatewayManagementApiClient
) {
  
  fun handleConnect(connectionId: String, userId: String) {
    val item = mapOf(
      "connectionId" to AttributeValue.builder().s(connectionId).build(),
      "userId" to AttributeValue.builder().s(userId).build(),
      "region" to AttributeValue.builder().s(currentRegion).build(),
      "connectedAt" to AttributeValue.builder()
        .n(Instant.now().epochSecond.toString())
        .build()
    )
    
    dynamoDb.putItem(
      PutItemRequest.builder()
        .tableName("websocket-connections")
        .item(item)
        .build()
    )
  }
  
  fun sendMessage(userId: String, message: String) {
    // Query all connections for user
    val connections = queryConnectionsByUserId(userId)
    
    // Send to all connections (may be in different regions)
    connections.forEach { connection ->
      try {
        apiGatewayClient.postToConnection(
          PostToConnectionRequest.builder()
            .connectionId(connection.connectionId)
            .data(SdkBytes.fromUtf8String(message))
            .build()
        )
      } catch (e: GoneException) {
        // Connection closed, remove from DynamoDB
        removeConnection(connection.connectionId)
      }
    }
  }
}
```

**Benefits of Stateless Architecture**:

```typescript
const statelessBenefits = {
  regionalMobility: {
    failoverTime: '< 5 minutes (vs 30 minutes stateful)',
    userImpact: 'Transparent (vs session loss)',
    complexity: 'Low (vs high)',
    automation: 'Fully automated (vs manual)'
  },
  
  scalability: {
    horizontal: 'Unlimited (vs limited)',
    autoScaling: 'Instant (vs slow)',
    loadBalancing: 'Any instance (vs sticky sessions)',
    efficiency: 'High (vs low)'
  },
  
  operations: {
    deployment: 'Rolling updates (vs blue-green)',
    testing: 'Simple (vs complex)',
    debugging: 'Easier (vs harder)',
    monitoring: 'Straightforward (vs complex)'
  },
  
  cost: {
    infrastructure: 'Optimized (vs over-provisioned)',
    operations: 'Lower (vs higher)',
    development: 'Faster (vs slower)'
  }
}
```

**Migration Strategy**:

```typescript
interface MigrationStrategy {
  phases: {
    phase1: {
      name: 'Assessment',
      duration: '2 weeks',
      activities: [
        'Identify stateful components',
        'Analyze dependencies',
        'Plan migration order',
        'Estimate effort'
      ]
    },
    
    phase2: {
      name: 'Infrastructure Setup',
      duration: '2 weeks',
      activities: [
        'Deploy Redis Global Datastore',
        'Configure S3 replication',
        'Set up DynamoDB Global Tables',
        'Test connectivity'
      ]
    },
    
    phase3: {
      name: 'Service Migration',
      duration: '8 weeks',
      activities: [
        'Migrate session management',
        'Externalize caching',
        'Move file storage to S3',
        'Implement idempotency',
        'Refactor background jobs',
        'Update WebSocket handling'
      ]
    },
    
    phase4: {
      name: 'Testing & Validation',
      duration: '2 weeks',
      activities: [
        'Integration testing',
        'Failover testing',
        'Performance testing',
        'Load testing'
      ]
    },
    
    phase5: {
      name: 'Production Rollout',
      duration: '2 weeks',
      activities: [
        'Gradual rollout',
        'Monitor performance',
        'Validate failover',
        'Optimize configuration'
      ]
    }
  },
  
  totalDuration: '16 weeks',
  
  prioritization: {
    high: ['Session management', 'File storage'],
    medium: ['Caching', 'Background jobs'],
    low: ['WebSocket', 'Optimization']
  }
}
```

**Pros**:

- ✅ Seamless regional failover (< 5 minutes)
- ✅ Unlimited horizontal scalability
- ✅ Simplified operations
- ✅ Consistent user experience
- ✅ Automated failover
- ✅ Cost optimization
- ✅ Easier testing and deployment
- ✅ Better resource utilization

**Cons**:

- ⚠️ Refactoring effort (16 weeks)
- ⚠️ External dependencies (Redis, S3, DynamoDB)
- ⚠️ Network latency for state access
- ⚠️ Complexity in distributed state management

**Cost**: $30,000 implementation + $15,000/year operational

**Risk**: **Low** - Proven architecture pattern

### Option 2: Hybrid Stateful/Stateless

**Description**: Keep some stateful components, externalize critical state only

**Pros**:

- ✅ Lower refactoring effort
- ✅ Faster implementation

**Cons**:

- ❌ Limited regional mobility
- ❌ Complex failover
- ❌ Inconsistent architecture

**Cost**: $15,000 implementation

**Risk**: **Medium** - Partial solution

### Option 3: Sticky Sessions with State Replication

**Description**: Use sticky sessions with background state replication

**Pros**:

- ✅ Minimal refactoring
- ✅ Familiar pattern

**Cons**:

- ❌ Slow failover
- ❌ Session loss on failure
- ❌ Limited scalability
- ❌ Complex operations

**Cost**: $10,000 implementation

**Risk**: **High** - Does not solve core problems

## Decision Outcome

**Chosen Option**: **Comprehensive Stateless Architecture (Option 1)**

### Rationale

Fully stateless architecture provides optimal regional mobility, scalability, and operational simplicity, justifying the refactoring investment.

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | High | Significant refactoring required | Training, documentation, phased approach |
| Operations Team | Medium | New infrastructure to manage | Automation, training, runbooks |
| QA Team | High | Extensive testing required | Test automation, clear test plans |
| Customers | Low | Transparent changes | Gradual rollout, monitoring |
| Management | Medium | Investment approval | ROI analysis, phased approach |

### Impact Radius Assessment

**Selected Impact Radius**: **System**

Affects:

- All application services
- Session management
- Caching layer
- File storage
- Background jobs
- WebSocket connections
- Deployment processes

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Performance degradation | Medium | High | Performance testing, optimization |
| Migration complexity | High | Medium | Phased approach, thorough testing |
| External dependency failures | Low | High | Redundancy, fallback mechanisms |
| Data consistency issues | Low | Critical | Strong consistency guarantees, testing |
| Cost overruns | Medium | Medium | Budget monitoring, phased approach |

**Overall Risk Level**: **Low**

## Implementation Plan

### Phase 1: Assessment & Planning (Week 1-2)

**Tasks**:

- [ ] Audit all services for stateful components
- [ ] Identify dependencies and migration order
- [ ] Create detailed migration plan
- [ ] Set up project tracking
- [ ] Allocate resources

**Success Criteria**:

- Complete inventory of stateful components
- Migration plan approved
- Resources allocated

### Phase 2: Infrastructure Setup (Week 3-4)

**Tasks**:

- [ ] Deploy Redis Global Datastore
- [ ] Configure S3 cross-region replication
- [ ] Set up DynamoDB Global Tables
- [ ] Configure networking
- [ ] Test connectivity and replication
- [ ] Set up monitoring

**Success Criteria**:

- All infrastructure operational
- Replication working
- Monitoring configured

### Phase 3: Session Management Migration (Week 5-6)

**Tasks**:

- [ ] Implement Spring Session with Redis
- [ ] Migrate session data structure
- [ ] Update authentication flow
- [ ] Test session persistence
- [ ] Deploy to staging
- [ ] Gradual production rollout

**Success Criteria**:

- Sessions externalized
- No session loss during failover
- Performance maintained

### Phase 4: Caching Migration (Week 7-8)

**Tasks**:

- [ ] Configure distributed caching
- [ ] Migrate cache keys
- [ ] Update cache access patterns
- [ ] Test cache consistency
- [ ] Deploy to production

**Success Criteria**:

- Distributed caching operational
- Cache hit rates maintained
- Cross-region consistency

### Phase 5: File Storage Migration (Week 9-10)

**Tasks**:

- [ ] Set up S3 buckets with replication
- [ ] Migrate existing files
- [ ] Update file upload/download logic
- [ ] Implement pre-signed URLs
- [ ] Test file access
- [ ] Deploy to production

**Success Criteria**:

- All files in S3
- Cross-region replication working
- File access performance acceptable

### Phase 6: Idempotency Implementation (Week 11-12)

**Tasks**:

- [ ] Implement idempotency framework
- [ ] Add idempotency to critical operations
- [ ] Test duplicate request handling
- [ ] Update API documentation
- [ ] Deploy to production

**Success Criteria**:

- Idempotency working for all critical operations
- No duplicate processing
- API clients updated

### Phase 7: Background Jobs Migration (Week 13-14)

**Tasks**:

- [ ] Migrate to EventBridge scheduling
- [ ] Implement distributed locks
- [ ] Update job implementations
- [ ] Test job execution
- [ ] Deploy to production

**Success Criteria**:

- Jobs running stateless
- No duplicate execution
- Proper failover

### Phase 8: Testing & Validation (Week 15-16)

**Tasks**:

- [ ] Integration testing
- [ ] Failover testing
- [ ] Performance testing
- [ ] Load testing
- [ ] Security testing
- [ ] Documentation updates

**Success Criteria**:

- All tests passing
- Failover < 5 minutes
- Performance maintained
- Documentation complete

### Rollback Strategy

**Trigger Conditions**:

- Critical performance degradation
- Data consistency issues
- Failover failures

**Rollback Steps**:

1. Revert to previous version
2. Restore stateful components
3. Investigate issues
4. Fix and retry

**Rollback Time**: < 4 hours

## Monitoring and Success Criteria

### Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Failover Time | < 5 minutes | Drill results |
| Session Persistence | 100% | Monitoring |
| Cache Hit Rate | > 80% | Redis metrics |
| File Access Latency | < 200ms | CloudWatch |
| Idempotency Success | 100% | Application logs |
| System Availability | > 99.9% | CloudWatch |

### Review Schedule

- **Weekly**: Progress review during implementation
- **Monthly**: Performance and cost review
- **Quarterly**: Architecture review

## Consequences

### Positive Consequences

- ✅ **Seamless Failover**: < 5 minute regional failover
- ✅ **Unlimited Scalability**: Horizontal scaling without limits
- ✅ **Simplified Operations**: Easier deployment and management
- ✅ **Consistent Experience**: No session loss during failover
- ✅ **Cost Optimization**: Better resource utilization
- ✅ **Easier Testing**: Simplified testing and debugging
- ✅ **Future-Proof**: Foundation for further evolution

### Negative Consequences

- ⚠️ **Refactoring Effort**: 16 weeks of development
- ⚠️ **External Dependencies**: Reliance on Redis, S3, DynamoDB
- ⚠️ **Network Latency**: Slight increase for state access
- ⚠️ **Operational Cost**: $15K/year for external services
- ⚠️ **Complexity**: Distributed state management complexity

### Technical Debt

**Identified Debt**:

1. Some legacy components still stateful
2. Manual state migration scripts
3. Basic monitoring
4. Limited automation

**Debt Repayment Plan**:

- **Q2 2026**: Complete legacy migration
- **Q3 2026**: Advanced monitoring
- **Q4 2026**: Full automation

## Related Decisions

- [ADR-040: Network Partition Handling Strategy](040-network-partition-handling-strategy.md)
- [ADR-046: Third Region Disaster Recovery](046-third-region-disaster-recovery-singapore-seoul.md)

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-25  
**Next Review**: 2026-01-25 (Quarterly)

## Notes

### Stateless vs Stateful Comparison

| Aspect | Stateful | Stateless |
|--------|----------|-----------|
| Failover Time | 30+ minutes | < 5 minutes |
| Session Loss | Yes | No |
| Scalability | Limited | Unlimited |
| Load Balancing | Sticky sessions | Any instance |
| Deployment | Complex | Simple |
| Testing | Difficult | Easy |
| Cost | Higher | Lower |

### External State Storage Comparison

| Service | Use Case | Latency | Cost | Availability |
|---------|----------|---------|------|--------------|
| Redis | Sessions, Cache | < 1ms | Medium | 99.99% |
| DynamoDB | Locks, Connections | < 10ms | Low | 99.99% |
| S3 | Files, Assets | < 50ms | Very Low | 99.99% |

### Best Practices

**Session Management**:

- Use short TTL (24 hours)
- Compress session data
- Encrypt sensitive data
- Monitor session count

**Caching**:

- Use appropriate TTL
- Implement cache warming
- Monitor hit rates
- Handle cache failures gracefully

**File Storage**:

- Use pre-signed URLs
- Implement CDN for public files
- Monitor storage costs
- Implement lifecycle policies

**Idempotency**:

- Use UUID v4 for keys
- Store for 24 hours
- Return same result for duplicate requests
- Log duplicate attempts

### Migration Checklist

- [ ] All services stateless
- [ ] Sessions externalized
- [ ] Caching distributed
- [ ] Files in S3
- [ ] Idempotency implemented
- [ ] Background jobs stateless
- [ ] WebSockets stateless
- [ ] Failover tested
- [ ] Performance validated
- [ ] Documentation updated
