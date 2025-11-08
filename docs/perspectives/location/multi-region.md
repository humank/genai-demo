---
title: "Multi-Region Deployment Strategy"
type: "perspective-detail"
category: "location"
stakeholders: ["infrastructure-architects", "devops-engineers", "operations"]
last_updated: "2025-10-24"
version: "1.0"
status: "active"
related_docs:

  - "overview.md"
  - "data-residency.md"
  - "latency-optimization.md"
  - "../../viewpoints/deployment/README.md"

tags: ["multi-region", "aws", "deployment", "infrastructure"]
---

# Multi-Region Deployment Strategy

## Overview

This document details the multi-region deployment strategy for the Enterprise E-Commerce Platform, covering regional architecture, deployment procedures, data replication, and operational considerations.

## Regional Architecture

### Primary Regions

#### 1. US East (N. Virginia) - us-east-1

**Role**: Primary Region  
**Traffic**: 50% of global traffic  
**User Base**: North America (US, Canada, Mexico)

**Infrastructure**:

- **Compute**: EKS cluster with 10-50 nodes (auto-scaling)
- **Database**: RDS PostgreSQL Multi-AZ (primary)
- **Cache**: ElastiCache Redis cluster (3 nodes)
- **Storage**: S3 bucket for static assets
- **CDN**: CloudFront distribution origin
- **Message Queue**: MSK Kafka cluster (3 brokers)

**Characteristics**:

- Handles all write operations
- Primary source of truth for data
- Highest resource allocation
- 24/7 operations support

#### 2. EU West (Ireland) - eu-west-1

**Role**: Secondary Active Region  
**Traffic**: 30% of global traffic  
**User Base**: Europe (EU, UK, Middle East, Africa)

**Infrastructure**:

- **Compute**: EKS cluster with 5-30 nodes (auto-scaling)
- **Database**: RDS PostgreSQL read replica
- **Cache**: ElastiCache Redis cluster (3 nodes)
- **Storage**: S3 bucket (replicated from us-east-1)
- **CDN**: CloudFront distribution origin
- **Message Queue**: MSK Kafka cluster (3 brokers)

**Characteristics**:

- Read-heavy workload
- GDPR-compliant data storage
- EU data residency enforcement
- Business hours operations support

#### 3. AP Southeast (Singapore) - ap-southeast-1

**Role**: Tertiary Active Region  
**Traffic**: 20% of global traffic  
**User Base**: Asia Pacific (Singapore, Australia, Japan, India)

**Infrastructure**:

- **Compute**: EKS cluster with 3-20 nodes (auto-scaling)
- **Database**: RDS PostgreSQL read replica
- **Cache**: ElastiCache Redis cluster (2 nodes)
- **Storage**: S3 bucket (replicated from us-east-1)
- **CDN**: CloudFront distribution origin
- **Message Queue**: MSK Kafka cluster (3 brokers)

**Characteristics**:

- Read-heavy workload
- Lower resource allocation
- Business hours operations support
- Potential for future China region expansion

### Regional Deployment Topology

```text
┌─────────────────────────────────────────────────────────────────────┐
│                         Global Layer                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌──────────────────┐                                               │
│  │  Route 53        │  ← GeoDNS routing                             │
│  │  Global DNS      │                                               │
│  └────────┬─────────┘                                               │
│           │                                                           │
│  ┌────────▼─────────────────────────────────────────┐               │
│  │  CloudFront CDN (Global Edge Locations)          │               │
│  └────────┬─────────────────────────────────────────┘               │
│           │                                                           │
│           ├──────────────┬──────────────┬──────────────┐            │
│           │              │              │              │            │
├───────────▼──────────────▼──────────────▼──────────────▼───────────┤
│                                                                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐             │
│  │  US-EAST-1   │  │  EU-WEST-1   │  │  AP-SE-1     │             │
│  │  (Primary)   │  │  (Secondary) │  │  (Tertiary)  │             │
│  ├──────────────┤  ├──────────────┤  ├──────────────┤             │
│  │              │  │              │  │              │             │
│  │ ┌──────────┐ │  │ ┌──────────┐ │  │ ┌──────────┐ │             │
│  │ │   EKS    │ │  │ │   EKS    │ │  │ │   EKS    │ │             │
│  │ │ Cluster  │ │  │ │ Cluster  │ │  │ │ Cluster  │ │             │
│  │ └──────────┘ │  │ └──────────┘ │  │ └──────────┘ │             │
│  │              │  │              │  │              │             │
│  │ ┌──────────┐ │  │ ┌──────────┐ │  │ ┌──────────┐ │             │
│  │ │   RDS    │ │  │ │   RDS    │ │  │ │   RDS    │ │             │
│  │ │ Primary  │◄─┼──┼─┤ Replica  │ │  │ │ Replica  │ │             │
│  │ └──────────┘ │  │ └──────────┘ │  │ └──────────┘ │             │
│  │              │  │              │  │              │             │
│  │ ┌──────────┐ │  │ ┌──────────┐ │  │ ┌──────────┐ │             │
│  │ │  Redis   │ │  │ │  Redis   │ │  │ │  Redis   │ │             │
│  │ │  Cache   │ │  │ │  Cache   │ │  │ │  Cache   │ │             │
│  │ └──────────┘ │  │ └──────────┘ │  │ └──────────┘ │             │
│  │              │  │              │  │              │             │
│  │ ┌──────────┐ │  │ ┌──────────┐ │  │ ┌──────────┐ │             │
│  │ │   MSK    │ │  │ │   MSK    │ │  │ │   MSK    │ │             │
│  │ │  Kafka   │ │  │ │  Kafka   │ │  │ │  Kafka   │ │             │
│  │ └──────────┘ │  │ └──────────┘ │  │ └──────────┘ │             │
│  │              │  │              │  │              │             │
│  └──────────────┘  └──────────────┘  └──────────────┘             │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
```

## Deployment Strategy

### Active-Active Configuration (US-EAST-1 and EU-WEST-1)

**Characteristics**:

- Both regions actively serve traffic
- Writes go to US-EAST-1 (primary)
- Reads served from local region
- Automatic failover between regions

**Traffic Distribution**:

```text
User Location → Routing Decision
─────────────────────────────────
North America → US-EAST-1 (primary)
Europe        → EU-WEST-1 (local reads, remote writes)
Asia Pacific  → AP-SE-1 (local reads, remote writes)
```

**Write Path**:

1. User submits write request (e.g., place order)
2. Request routed to nearest region
3. If not US-EAST-1, forward to US-EAST-1
4. US-EAST-1 processes write to primary database
5. Asynchronous replication to read replicas
6. Response returned to user

**Read Path**:

1. User submits read request (e.g., view products)
2. Request routed to nearest region
3. Region serves from local read replica
4. Response returned to user

### Active-Passive Configuration (AP-SE-1)

**Characteristics**:

- AP-SE-1 serves read traffic only
- All writes forwarded to US-EAST-1
- Can be promoted to active if needed
- Lower operational overhead

**Promotion Criteria**:

- Traffic exceeds 30% of global traffic
- Latency requirements demand local writes
- Business expansion in APAC region

## Data Replication Strategy

### Database Replication

**Technology**: PostgreSQL Streaming Replication

**Configuration**:

```text
Primary (US-EAST-1)
    ├─► Read Replica (EU-WEST-1)
    │   └─► Replication Lag Target: < 1 second
    │
    └─► Read Replica (AP-SE-1)
        └─► Replication Lag Target: < 2 seconds
```

**Replication Monitoring**:

- CloudWatch metric: `ReplicaLag`
- Alert threshold: > 5 seconds
- Critical threshold: > 30 seconds

**Failover Procedure**:

1. Detect primary failure
2. Promote read replica to primary
3. Update DNS to point to new primary
4. Reconfigure remaining replicas
5. Estimated RTO: 5 minutes

### Object Storage Replication

**Technology**: S3 Cross-Region Replication (CRR)

**Configuration**:

```text
Source: s3://ecommerce-assets-us-east-1
    ├─► Destination: s3://ecommerce-assets-eu-west-1
    │   └─► Replication Time: < 15 minutes
    │
    └─► Destination: s3://ecommerce-assets-ap-southeast-1
        └─► Replication Time: < 15 minutes
```

**Replication Rules**:

- Replicate all objects
- Replicate delete markers
- Replicate object metadata
- Replicate object tags

### Cache Replication

**Technology**: Redis Global Datastore (for critical data)

**Configuration**:

- Primary cluster: US-EAST-1
- Secondary clusters: EU-WEST-1, AP-SE-1
- Replication lag: < 1 second
- Automatic failover enabled

**Cache Strategy**:

- Session data: Replicated globally
- Product catalog: Replicated globally
- User preferences: Replicated globally
- Shopping cart: Region-specific (no replication)

### Message Queue Replication

**Technology**: MSK MirrorMaker 2.0

**Configuration**:

```text
Source Cluster: US-EAST-1
    ├─► Mirror: EU-WEST-1
    │   └─► Topics: order-events, customer-events
    │
    └─► Mirror: AP-SE-1
        └─► Topics: order-events, customer-events
```

**Replication Strategy**:

- Critical events: Replicated to all regions
- Regional events: Stay in region
- Replication lag: < 5 seconds

## Deployment Procedures

### Standard Deployment (Blue-Green)

**Objective**: Deploy new version with zero downtime

**Steps**:

1. **Preparation** (T-1 hour):

   ```bash
   # Build and test new version
   ./gradlew clean build test
   
   # Build Docker image
   docker build -t ecommerce-app:v2.0.0 .
   
   # Push to ECR in all regions
   ./scripts/push-to-all-regions.sh v2.0.0
   ```

2. **Deploy to US-EAST-1** (T+0):

   ```bash
   # Deploy to blue environment
   kubectl apply -f k8s/blue-deployment.yaml
   
   # Wait for pods to be ready
   kubectl wait --for=condition=ready pod -l version=v2.0.0
   
   # Run smoke tests
   ./scripts/smoke-test.sh us-east-1
   
   # Switch traffic to blue
   kubectl patch service app-service -p '{"spec":{"selector":{"version":"v2.0.0"}}}'
   ```

3. **Monitor US-EAST-1** (T+15 minutes):

   ```bash
   # Monitor error rates
   ./scripts/monitor-deployment.sh us-east-1
   
   # Check key metrics

   - Error rate < 0.1%
   - Latency < 200ms (p95)
   - CPU < 70%
   - Memory < 80%

   ```

4. **Deploy to EU-WEST-1** (T+30 minutes):

   ```bash
   # Repeat steps 2-3 for EU-WEST-1
   ./scripts/deploy-region.sh eu-west-1 v2.0.0
   ```

5. **Deploy to AP-SE-1** (T+60 minutes):

   ```bash
   # Repeat steps 2-3 for AP-SE-1
   ./scripts/deploy-region.sh ap-southeast-1 v2.0.0
   ```

6. **Cleanup** (T+120 minutes):

   ```bash
   # Remove old green deployment
   kubectl delete deployment app-green
   ```

### Emergency Rollback

**Trigger Conditions**:

- Error rate > 1%
- Latency > 500ms (p95)
- Critical functionality broken
- Security vulnerability discovered

**Rollback Steps**:

1. **Immediate** (< 5 minutes):

   ```bash
   # Switch traffic back to green
   kubectl patch service app-service -p '{"spec":{"selector":{"version":"v1.9.0"}}}'
   ```

2. **Verify** (< 10 minutes):

   ```bash
   # Verify metrics return to normal
   ./scripts/verify-rollback.sh
   ```

3. **Investigate** (< 30 minutes):

   ```bash
   # Collect logs and metrics
   ./scripts/collect-diagnostics.sh
   
   # Analyze root cause
   ./scripts/analyze-failure.sh
   ```

### Regional Failover

**Scenario**: US-EAST-1 region becomes unavailable

**Failover Steps**:

1. **Detect Failure** (< 1 minute):

   ```bash
   # Automated health checks detect failure
   # PagerDuty alert triggered
   ```

2. **Promote EU-WEST-1** (< 5 minutes):

   ```bash
   # Promote read replica to primary
   aws rds promote-read-replica \
     --db-instance-identifier ecommerce-db-eu-west-1
   
   # Update Route 53 to point to EU-WEST-1
   aws route53 change-resource-record-sets \
     --hosted-zone-id Z1234567890ABC \
     --change-batch file://failover-to-eu.json
   ```

3. **Reconfigure Replication** (< 10 minutes):

   ```bash
   # Configure AP-SE-1 to replicate from EU-WEST-1
   aws rds modify-db-instance \
     --db-instance-identifier ecommerce-db-ap-southeast-1 \
     --source-db-instance-identifier ecommerce-db-eu-west-1
   ```

4. **Verify** (< 15 minutes):

   ```bash
   # Verify all services operational
   ./scripts/verify-failover.sh
   
   # Check replication lag
   ./scripts/check-replication-lag.sh
   ```

## Network Configuration

### VPC Peering

**Configuration**:

```text
US-EAST-1 VPC (10.0.0.0/16)
    ├─► Peering Connection ─► EU-WEST-1 VPC (10.1.0.0/16)
    └─► Peering Connection ─► AP-SE-1 VPC (10.2.0.0/16)
```

**Route Tables**:

```text
US-EAST-1:

  - 10.1.0.0/16 → pcx-us-to-eu
  - 10.2.0.0/16 → pcx-us-to-ap

EU-WEST-1:

  - 10.0.0.0/16 → pcx-eu-to-us
  - 10.2.0.0/16 → pcx-eu-to-ap (via US)

AP-SE-1:

  - 10.0.0.0/16 → pcx-ap-to-us
  - 10.1.0.0/16 → pcx-ap-to-eu (via US)

```

### Transit Gateway (Future)

**Planned Configuration**:

```text
┌─────────────────────────────────────────┐
│       AWS Transit Gateway               │
│       (Global Network Hub)              │
├─────────────────────────────────────────┤
│                                          │
│  ┌──────────┐  ┌──────────┐  ┌────────┐│
│  │ US-EAST-1│  │ EU-WEST-1│  │ AP-SE-1││
│  │   VPC    │  │   VPC    │  │  VPC   ││
│  └──────────┘  └──────────┘  └────────┘│
│                                          │
└─────────────────────────────────────────┘
```

**Benefits**:

- Simplified routing
- Centralized network management
- Better scalability for additional regions

### Security Groups

**Cross-Region Access**:

```text
Application Security Group:

  - Inbound: 443 from CloudFront
  - Inbound: 8080 from VPC CIDR (all regions)
  - Outbound: All

Database Security Group:

  - Inbound: 5432 from Application SG (all regions)
  - Outbound: 5432 to Replica SGs

```

## Monitoring and Alerting

### Regional Health Checks

**CloudWatch Synthetics**:

```text
Canary: ecommerce-health-check

  - Frequency: Every 1 minute
  - Regions: US-EAST-1, EU-WEST-1, AP-SE-1
  - Checks:
    - Homepage load time
    - API health endpoint
    - Database connectivity
    - Cache connectivity

```

**Alert Thresholds**:

- Health check failure: 2 consecutive failures
- Latency > 500ms: 5 minutes
- Error rate > 1%: 2 minutes

### Replication Monitoring

**Key Metrics**:

```text
Database Replication:

  - ReplicaLag (seconds)
  - ReplicationSlotDiskUsage (MB)
  - OldestReplicationSlotLag (seconds)

S3 Replication:

  - ReplicationLatency (seconds)
  - BytesPendingReplication (bytes)
  - OperationsPendingReplication (count)

Cache Replication:

  - ReplicationLag (milliseconds)
  - ReplicationBytes (bytes/second)

```

**Alerts**:

- Database lag > 5 seconds: Warning
- Database lag > 30 seconds: Critical
- S3 replication > 1 hour: Warning
- Cache lag > 1 second: Warning

### Regional Performance Dashboard

**Metrics by Region**:

- Request rate (requests/second)
- Error rate (%)
- Latency (p50, p95, p99)
- CPU utilization (%)
- Memory utilization (%)
- Database connections (count)
- Cache hit rate (%)

## Cost Optimization

### Regional Cost Allocation

**Monthly Cost Breakdown** (Estimated):

```text
US-EAST-1 (Primary):

  - Compute (EKS): $5,000
  - Database (RDS): $3,000
  - Cache (Redis): $1,000
  - Storage (S3): $500
  - Network: $1,500
  - Total: $11,000

EU-WEST-1 (Secondary):

  - Compute (EKS): $3,000
  - Database (RDS): $1,500
  - Cache (Redis): $600
  - Storage (S3): $300
  - Network: $1,000
  - Total: $6,400

AP-SE-1 (Tertiary):

  - Compute (EKS): $2,000
  - Database (RDS): $1,000
  - Cache (Redis): $400
  - Storage (S3): $200
  - Network: $800
  - Total: $4,400

Global Services:

  - CloudFront CDN: $2,000
  - Route 53: $100
  - Total: $2,100

Grand Total: $23,900/month
```

### Cost Optimization Strategies

1. **Right-Sizing**:
   - Monitor resource utilization
   - Scale down during off-peak hours
   - Use spot instances for non-critical workloads

2. **Reserved Instances**:
   - Purchase 1-year RIs for baseline capacity
   - Save 30-40% on compute costs

3. **Data Transfer Optimization**:
   - Use CloudFront to reduce origin requests
   - Compress data before transfer
   - Use VPC endpoints to avoid NAT gateway costs

4. **Storage Optimization**:
   - Use S3 Intelligent-Tiering
   - Implement lifecycle policies
   - Delete old logs and backups

## Operational Procedures

### Adding a New Region

**Prerequisites**:

- Business justification (traffic > 15% from region)
- Compliance requirements identified
- Budget approved

**Steps**:

1. **Infrastructure Setup** (Week 1):

   ```bash
   # Deploy CDK stack to new region
   cdk deploy --region ap-northeast-1 --all
   ```

2. **Data Replication** (Week 2):

   ```bash
   # Configure database replication
   aws rds create-db-instance-read-replica \
     --db-instance-identifier ecommerce-db-ap-northeast-1 \
     --source-db-instance-identifier ecommerce-db-us-east-1
   
   # Configure S3 replication
   aws s3api put-bucket-replication \
     --bucket ecommerce-assets-us-east-1 \
     --replication-configuration file://replication-ap-northeast-1.json
   ```

3. **Application Deployment** (Week 3):

   ```bash
   # Deploy application to new region
   ./scripts/deploy-region.sh ap-northeast-1 v2.0.0
   ```

4. **Testing** (Week 4):

   ```bash
   # Run integration tests
   ./scripts/test-region.sh ap-northeast-1
   
   # Run load tests
   ./scripts/load-test-region.sh ap-northeast-1
   ```

5. **Traffic Migration** (Week 5):

   ```bash
   # Update Route 53 to include new region
   aws route53 change-resource-record-sets \
     --hosted-zone-id Z1234567890ABC \
     --change-batch file://add-ap-northeast-1.json
   
   # Gradually increase traffic (10% per day)
   ./scripts/gradual-traffic-shift.sh ap-northeast-1
   ```

### Removing a Region

**Trigger Conditions**:

- Traffic < 5% from region
- Cost exceeds benefit
- Compliance requirements change

**Steps**:

1. **Traffic Migration** (Week 1):

   ```bash
   # Redirect traffic to other regions
   aws route53 change-resource-record-sets \
     --hosted-zone-id Z1234567890ABC \
     --change-batch file://remove-ap-northeast-1.json
   ```

2. **Data Migration** (Week 2):

   ```bash
   # Stop replication
   aws rds delete-db-instance-read-replica \
     --db-instance-identifier ecommerce-db-ap-northeast-1
   
   # Archive data if needed
   ./scripts/archive-regional-data.sh ap-northeast-1
   ```

3. **Infrastructure Teardown** (Week 3):

   ```bash
   # Destroy CDK stack
   cdk destroy --region ap-northeast-1 --all
   ```

## Best Practices

### Deployment Best Practices

1. **Always deploy to one region at a time**
2. **Monitor each deployment for 30 minutes before proceeding**
3. **Have rollback plan ready before deployment**
4. **Test in staging environment first**
5. **Deploy during low-traffic hours**

### Operational Best Practices

1. **Maintain regional parity** - Keep all regions at same version
2. **Monitor replication lag** - Alert on lag > 5 seconds
3. **Test failover regularly** - Quarterly failover drills
4. **Document regional differences** - Track any region-specific configurations
5. **Optimize costs continuously** - Monthly cost review

### Security Best Practices

1. **Encrypt data in transit** - TLS 1.3 for all cross-region traffic
2. **Encrypt data at rest** - KMS encryption for all storage
3. **Implement least privilege** - Region-specific IAM roles
4. **Audit cross-region access** - CloudTrail logging enabled
5. **Regular security reviews** - Quarterly security audits

## Related Documentation

- [Overview](overview.md) - Location Perspective overview
- [Data Residency](data-residency.md) - Compliance requirements
- [Latency Optimization](latency-optimization.md) - Performance tuning
- [Deployment Viewpoint](../../viewpoints/deployment/README.md) - Infrastructure details
- [Operational Viewpoint](../../viewpoints/operational/README.md) - Operations procedures

---

**Document Status**: ✅ Complete  
**Review Date**: 2025-10-24  
**Next Review**: 2026-01-24 (Quarterly)
