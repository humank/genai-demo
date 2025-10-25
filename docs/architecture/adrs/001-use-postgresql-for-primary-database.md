---
adr_number: 001
title: "Use PostgreSQL for Primary Database"
date: 2025-10-24
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [002, 005]
affected_viewpoints: ["information", "deployment"]
affected_perspectives: ["performance", "availability"]
---

# ADR-001: Use PostgreSQL for Primary Database

## Status

**Accepted** - 2025-10-24

## Context

### Problem Statement

The Enterprise E-Commerce Platform requires a robust, scalable, and reliable database system to store and manage critical business data including customers, orders, products, inventory, and transactions. The database must support:

- Complex relational data models with referential integrity
- ACID transactions for financial operations
- High read and write throughput
- Multi-region replication
- Advanced querying capabilities
- Strong consistency guarantees

### Business Context

**Business Drivers**:
- Need for reliable transaction processing (orders, payments)
- Regulatory compliance requirements (GDPR, PCI-DSS)
- Expected growth from 10K to 1M+ users
- 24/7 availability requirement (99.9% SLA)
- Multi-region deployment for global users

**Constraints**:
- Budget: $5,000/month for database infrastructure
- Team expertise: Strong Java/Spring Boot experience
- Timeline: 3 months to production
- Compliance: Must support data encryption and audit trails

### Technical Context

**Current State**:
- New greenfield project
- No existing database infrastructure
- Spring Boot application framework chosen
- AWS cloud infrastructure

**Requirements**:
- Support for complex joins and transactions
- JSON data type support for flexible schemas
- Full-text search capabilities
- Geospatial data support (future requirement)
- Read replica support for scaling
- Point-in-time recovery

## Decision Drivers

1. **Data Integrity**: Need strong ACID guarantees for financial transactions
2. **Scalability**: Must handle 10K → 1M users growth
3. **Team Expertise**: Team has SQL database experience
4. **Ecosystem**: Rich tooling and Spring Boot integration
5. **Cost**: Predictable pricing model
6. **Compliance**: Built-in security and audit features
7. **Performance**: Sub-100ms query response times
8. **Availability**: Multi-AZ and read replica support

## Considered Options

### Option 1: PostgreSQL

**Description**: Open-source relational database with advanced features

**Pros**:
- ✅ Strong ACID compliance and data integrity
- ✅ Excellent Spring Boot/JPA integration
- ✅ Rich feature set (JSON, full-text search, arrays)
- ✅ Active community and extensive documentation
- ✅ AWS RDS managed service available
- ✅ Read replicas for horizontal scaling
- ✅ Advanced indexing (B-tree, GiST, GIN, BRIN)
- ✅ Mature replication (streaming, logical)
- ✅ Cost-effective for our scale

**Cons**:
- ⚠️ Write scaling requires sharding (complex)
- ⚠️ Vertical scaling has limits
- ⚠️ Replication lag in read replicas

**Cost**: 
- Development: $3,000/month (db.r5.xlarge Multi-AZ)
- Production: $5,000/month (db.r5.2xlarge Multi-AZ + 2 read replicas)

**Risk**: **Low** - Proven technology with extensive production use

### Option 2: MySQL

**Description**: Popular open-source relational database

**Pros**:
- ✅ Wide adoption and community
- ✅ Good Spring Boot integration
- ✅ AWS RDS managed service
- ✅ Read replicas support
- ✅ Lower resource usage

**Cons**:
- ❌ Less advanced features than PostgreSQL
- ❌ Weaker JSON support
- ❌ Limited full-text search
- ❌ Less sophisticated query optimizer
- ❌ Replication can be complex

**Cost**: Similar to PostgreSQL

**Risk**: **Low** - Proven technology

### Option 3: MongoDB

**Description**: Document-oriented NoSQL database

**Pros**:
- ✅ Flexible schema
- ✅ Horizontal scaling built-in
- ✅ Good for rapid development
- ✅ Native JSON support

**Cons**:
- ❌ No ACID transactions across documents (until v4.0)
- ❌ Eventual consistency by default
- ❌ Less suitable for complex joins
- ❌ Team lacks NoSQL experience
- ❌ Higher learning curve
- ❌ More expensive at scale

**Cost**: $6,000/month (MongoDB Atlas M30)

**Risk**: **Medium** - Team learning curve, transaction limitations

### Option 4: Amazon Aurora PostgreSQL

**Description**: AWS-native PostgreSQL-compatible database

**Pros**:
- ✅ PostgreSQL compatibility
- ✅ Better performance than standard PostgreSQL
- ✅ Automatic scaling
- ✅ Fast failover (< 30 seconds)
- ✅ Up to 15 read replicas

**Cons**:
- ❌ Vendor lock-in to AWS
- ❌ Higher cost than RDS PostgreSQL
- ❌ Some PostgreSQL extensions not supported
- ❌ More complex pricing model

**Cost**: $7,000/month (db.r5.2xlarge + replicas)

**Risk**: **Low** - AWS managed, but vendor lock-in

## Decision Outcome

**Chosen Option**: **PostgreSQL on AWS RDS**

### Rationale

PostgreSQL was selected as the primary database for the following reasons:

1. **Strong ACID Guarantees**: Critical for financial transactions and order processing
2. **Feature-Rich**: JSON support, full-text search, and advanced indexing meet all current and near-future requirements
3. **Excellent Ecosystem**: Best-in-class Spring Boot/JPA integration with Hibernate
4. **Team Expertise**: Team has SQL experience, minimal learning curve
5. **Cost-Effective**: Meets budget constraints while providing enterprise features
6. **Scalability Path**: Read replicas provide horizontal read scaling; can add sharding later if needed
7. **AWS RDS**: Managed service reduces operational overhead (backups, patching, monitoring)
8. **Compliance**: Built-in encryption, audit logging, and security features

**Why Not Aurora**: While Aurora offers better performance, the cost premium (40% higher) is not justified for our current scale. We can migrate to Aurora later if needed without application changes.

**Why Not MongoDB**: Lack of strong ACID guarantees and team expertise makes it unsuitable for our transaction-heavy workload.

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | Medium | Need to learn PostgreSQL-specific features | Training sessions, documentation |
| Operations Team | Low | Familiar with RDS management | Standard RDS runbooks |
| End Users | None | Transparent to users | N/A |
| Business | Positive | Reliable data storage | N/A |
| Compliance | Positive | Built-in security features | Regular audits |

### Impact Radius

**Selected Impact Radius**: **System**

Affects:
- All bounded contexts (data storage)
- Application layer (JPA configuration)
- Infrastructure layer (RDS setup)
- Deployment processes
- Backup and recovery procedures

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Write scaling limitations | Medium | High | Implement read replicas, caching, eventual sharding plan |
| Replication lag | Medium | Medium | Monitor lag, implement retry logic, use async processing |
| Cost overrun | Low | Medium | Monitor usage, implement auto-scaling policies |
| Data migration complexity | Low | High | Thorough testing, staged rollout, rollback plan |
| Vendor lock-in (AWS RDS) | Low | Medium | Use standard PostgreSQL features, avoid AWS-specific extensions |

**Overall Risk Level**: **Low**

## Implementation Plan

### Phase 1: Setup and Configuration (Week 1-2)

- [x] Provision RDS PostgreSQL instance (db.r5.xlarge, Multi-AZ)
- [x] Configure security groups and network access
- [x] Set up parameter groups for optimization
- [x] Enable automated backups (7-day retention)
- [x] Configure CloudWatch monitoring and alarms
- [x] Set up read replica in secondary region

### Phase 2: Application Integration (Week 3-4)

- [x] Configure Spring Boot data source
- [x] Set up Hibernate/JPA with PostgreSQL dialect
- [x] Implement database migration with Flyway
- [x] Create initial schema and indexes
- [x] Implement connection pooling (HikariCP)
- [x] Add database health checks

### Phase 3: Testing and Optimization (Week 5-6)

- [x] Load testing with realistic data volumes
- [x] Query performance optimization
- [x] Index tuning based on query patterns
- [x] Replication lag monitoring
- [x] Failover testing
- [x] Backup and restore testing

### Rollback Strategy

**Trigger Conditions**:
- Performance degradation > 50%
- Data corruption or loss
- Unrecoverable errors
- Cost exceeds budget by > 50%

**Rollback Steps**:
1. If in development: Switch to H2 in-memory database temporarily
2. If in production: Restore from latest backup
3. Investigate root cause
4. Re-evaluate database choice if fundamental issues found

**Rollback Time**: < 1 hour for development, < 4 hours for production

## Monitoring and Success Criteria

### Success Metrics

- ✅ Query response time < 100ms (95th percentile)
- ✅ Write throughput > 1000 TPS
- ✅ Read throughput > 5000 TPS
- ✅ Replication lag < 1 second
- ✅ Availability > 99.9%
- ✅ Zero data loss incidents
- ✅ Cost within budget ($5,000/month)

### Monitoring Plan

**CloudWatch Metrics**:
- DatabaseConnections
- CPUUtilization
- FreeableMemory
- ReadLatency / WriteLatency
- ReplicaLag
- DiskQueueDepth

**Alerts**:
- CPU > 80% for 5 minutes
- Connections > 80% of max
- Replication lag > 5 seconds
- Disk space < 20%
- Failed connections > 10/minute

**Review Schedule**:
- Daily: Check CloudWatch dashboard
- Weekly: Review slow query log
- Monthly: Capacity planning review
- Quarterly: Cost optimization review

## Consequences

### Positive Consequences

- ✅ **Strong Data Integrity**: ACID guarantees protect financial data
- ✅ **Rich Feature Set**: JSON, full-text search, arrays support complex use cases
- ✅ **Excellent Tooling**: pgAdmin, pg_stat_statements, extensive monitoring
- ✅ **Spring Boot Integration**: Seamless JPA/Hibernate support
- ✅ **Scalability**: Read replicas provide horizontal read scaling
- ✅ **Community Support**: Large community, extensive documentation
- ✅ **Cost-Effective**: Meets requirements within budget
- ✅ **Compliance Ready**: Built-in encryption, audit logging

### Negative Consequences

- ⚠️ **Write Scaling**: Vertical scaling only, sharding required for massive scale
- ⚠️ **Replication Lag**: Read replicas have eventual consistency
- ⚠️ **Operational Overhead**: Need to monitor and tune performance
- ⚠️ **Migration Complexity**: Future migration to Aurora or sharding will be complex

### Technical Debt

**Identified Debt**:
1. No sharding strategy implemented (acceptable for current scale)
2. Single-region write master (acceptable for current requirements)
3. Manual query optimization needed (ongoing process)

**Debt Repayment Plan**:
- **Q2 2026**: Evaluate need for sharding based on growth
- **Q3 2026**: Implement caching layer to reduce database load
- **Q4 2026**: Consider Aurora migration if performance requirements increase

## Related Decisions

- [ADR-002: Adopt Hexagonal Architecture](002-adopt-hexagonal-architecture.md) - Repository pattern implementation
- [ADR-004: Use Redis for Distributed Caching](004-use-redis-for-distributed-caching.md) - Caching strategy to reduce database load
- [ADR-005: Use Apache Kafka for Event Streaming](005-use-kafka-for-event-streaming.md) - Event sourcing considerations
- [ADR-007: Use AWS CDK for Infrastructure](007-use-aws-cdk-for-infrastructure.md) - Database provisioning

## Notes

### PostgreSQL Configuration

```yaml
# Key RDS Parameters
max_connections: 200
shared_buffers: 8GB
effective_cache_size: 24GB
maintenance_work_mem: 2GB
checkpoint_completion_target: 0.9
wal_buffers: 16MB
default_statistics_target: 100
random_page_cost: 1.1
effective_io_concurrency: 200
work_mem: 20MB
min_wal_size: 1GB
max_wal_size: 4GB
```

### Migration Path to Aurora

If future requirements demand Aurora:
1. Create Aurora cluster from RDS snapshot
2. Update application connection string
3. Test thoroughly in staging
4. Blue-green deployment to production
5. Monitor for 48 hours
6. Decommission RDS instance

**Estimated Migration Time**: 1 week  
**Estimated Downtime**: < 1 hour

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-24  
**Next Review**: 2026-01-24 (Quarterly)
