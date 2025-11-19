---
adr_number: 046
title: "Third Region Disaster Recovery (Singapore/Seoul)"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [037, 038, 040, 044]
affected_viewpoints: ["deployment", "operational"]
affected_perspectives: ["availability", "evolution"]
---

# ADR-046: Third Region Disaster Recovery (Singapore/Seoul)

## Status

**Accepted** - 2025-10-25

## Context

### Problem Statement

Current active-active architecture in Taiwan and Tokyo provides high availability but lacks geographic diversity for true disaster recovery:

**Current Limitations**:

- **Geographic Concentration**: Both regions in East Asia
- **Correlated Risks**: Regional disasters (earthquakes, typhoons) could affect both
- **Geopolitical Risks**: Taiwan Strait tensions could impact both regions
- **Limited Failover Options**: No third region for complete primary region loss
- **Recovery Time**: Extended recovery if both regions unavailable
- **Data Sovereignty**: Need alternative region for data residency compliance

**Disaster Scenarios Requiring Third Region**:

1. **Dual Region Failure**: Both Taiwan and Tokyo unavailable
2. **Regional Natural Disaster**: Major earthquake affecting East Asia
3. **Geopolitical Crisis**: Conflict impacting multiple regions
4. **Extended Outage**: Long-term unavailability requiring alternative region
5. **Data Migration**: Need to relocate data due to regulatory changes

**Business Impact Without Third Region**:

- Complete service outage if both primary regions fail
- Extended recovery time (> 4 hours)
- Potential data loss
- Customer trust erosion
- Revenue loss
- Regulatory non-compliance

### Business Context

**Business Drivers**:

- Business continuity (mandatory)
- Geographic risk diversification
- Regulatory compliance (data residency)
- Customer trust and reliability
- Competitive advantage
- Market expansion opportunities

**Constraints**:

- Budget: $50,000/year for third region
- Must maintain 99.9% availability
- Data sovereignty requirements
- Minimize operational complexity
- Cost optimization

### Technical Context

**Current State**:

- Active-active in Taiwan (ap-northeast-1) and Tokyo (ap-northeast-1)
- No third region
- Manual disaster recovery procedures
- Limited geographic diversity
- 4-hour RTO for complete region loss

**Requirements**:

- Third region in different geographic area
- Automated failover capability
- < 2 hour RTO for third region activation
- < 15 minute RPO for critical data
- Data sovereignty compliance
- Cost-effective implementation

## Decision Drivers

1. **Geographic Diversity**: Reduce correlated regional risks
2. **Availability**: Maintain 99.9% SLO even during dual region failure
3. **Recovery Time**: < 2 hour RTO for third region activation
4. **Data Protection**: < 15 minute RPO for critical data
5. **Cost**: Optimize third region costs ($50K/year budget)
6. **Compliance**: Meet data sovereignty requirements
7. **Automation**: Automated failover and recovery
8. **Simplicity**: Minimize operational complexity

## Considered Options

### Option 1: Singapore as Primary Third Region with Seoul Backup (Recommended)

**Description**: Deploy Singapore (ap-southeast-1) as warm standby third region with Seoul (ap-northeast-2) as cold standby backup

**Architecture**:

```text
Primary Regions (Active-Active):
├── Taiwan (ap-northeast-1) - 50% traffic
└── Tokyo (ap-northeast-1) - 50% traffic

Third Region (Warm Standby):
└── Singapore (ap-southeast-1)
    ├── Compute: 20% capacity (ready to scale)
    ├── Database: Read replica (5-minute lag)
    ├── Cache: Empty (ready to populate)
    └── Storage: Replicated from primary

Backup Region (Cold Standby):
└── Seoul (ap-northeast-2)
    ├── Compute: None (deploy from IaC)
    ├── Database: Backup snapshots only
    ├── Cache: None
    └── Storage: Backup snapshots only
```

**Singapore Warm Standby Configuration**:

```typescript
interface SingaporeWarmStandby {
  infrastructure: {
    compute: {
      eks: {
        nodeGroups: 2,
        instanceType: 'm5.large',
        capacity: '20% of primary',
        cost: '$3,000/month'
      }
    },
    
    database: {
      rds: {
        instanceType: 'db.r5.large',
        role: 'Read replica',
        replicationLag: '< 5 minutes',
        cost: '$2,500/month'
      }
    },
    
    cache: {
      elasticache: {
        nodeType: 'cache.r5.large',
        nodes: 2,
        status: 'Empty, ready to populate',
        cost: '$1,000/month'
      }
    },
    
    storage: {
      s3: {
        replication: 'Cross-region replication',
        consistency: 'Eventually consistent',
        cost: '$500/month'
      }
    },
    
    network: {
      vpc: 'Dedicated VPC',
      connectivity: 'VPC peering to Taiwan/Tokyo',
      bandwidth: '1 Gbps',
      cost: '$1,000/month'
    }
  },
  
  totalMonthlyCost: '$8,000',
  totalAnnualCost: '$96,000'
}
```

**Seoul Cold Standby Configuration**:

```typescript
interface SeoulColdStandby {
  infrastructure: {
    compute: {
      status: 'No running infrastructure',
      deployment: 'IaC templates ready',
      activationTime: '< 2 hours'
    },
    
    database: {
      snapshots: 'Daily automated snapshots',
      retention: '30 days',
      restoreTime: '< 1 hour'
    },
    
    storage: {
      s3: 'Backup snapshots only',
      replication: 'Weekly backup replication',
      cost: '$500/month'
    }
  },
  
  totalMonthlyCost: '$500',
  totalAnnualCost: '$6,000'
}
```

**Why Singapore?**:

- ✅ Geographic diversity (Southeast Asia vs East Asia)
- ✅ Low latency to Taiwan/Tokyo (~50-80ms)
- ✅ Stable political environment
- ✅ Strong data protection laws
- ✅ Excellent AWS infrastructure
- ✅ English-speaking support
- ✅ Regional hub for APAC

**Why Seoul as Backup?**:

- ✅ Geographic proximity to Taiwan/Tokyo
- ✅ Lower cost than Singapore
- ✅ Good AWS infrastructure
- ✅ Alternative if Singapore unavailable
- ✅ Cultural and business ties to region

**Activation Procedures**:

**Scenario 1: Activate Singapore (Dual Region Failure)**:

```typescript
interface SingaporeActivation {
  trigger: {
    condition: 'Both Taiwan and Tokyo unavailable',
    detection: 'Automated health checks',
    decision: 'Automatic or manual approval'
  },
  
  steps: [
    {
      phase: 'Immediate Response (0-5 minutes)',
      actions: [
        'Detect dual region failure',
        'Activate incident response team',
        'Notify stakeholders',
        'Initiate Singapore activation'
      ]
    },
    {
      phase: 'Infrastructure Scaling (5-30 minutes)',
      actions: [
        'Scale EKS nodes to 100% capacity',
        'Promote RDS read replica to primary',
        'Populate ElastiCache from database',
        'Update DNS routing to Singapore',
        'Verify health checks'
      ]
    },
    {
      phase: 'Service Validation (30-60 minutes)',
      actions: [
        'Run smoke tests',
        'Verify critical services',
        'Monitor error rates',
        'Validate data integrity',
        'Confirm customer access'
      ]
    },
    {
      phase: 'Stabilization (60-120 minutes)',
      actions: [
        'Monitor system performance',
        'Optimize resource allocation',
        'Update monitoring dashboards',
        'Communicate status to customers',
        'Plan recovery strategy'
      ]
    }
  ],
  
  rto: '< 2 hours',
  rpo: '< 15 minutes',
  
  rollback: {
    trigger: 'Singapore activation fails',
    action: 'Attempt Seoul activation',
    time: '< 1 hour'
  }
}
```

**Scenario 2: Activate Seoul (Singapore Also Unavailable)**:

```typescript
interface SeoulActivation {
  trigger: {
    condition: 'All three regions (Taiwan, Tokyo, Singapore) unavailable',
    detection: 'Manual assessment',
    decision: 'Executive approval required'
  },
  
  steps: [
    {
      phase: 'Infrastructure Deployment (0-60 minutes)',
      actions: [
        'Deploy EKS cluster from IaC',
        'Deploy RDS from latest snapshot',
        'Deploy ElastiCache cluster',
        'Configure networking',
        'Deploy application services'
      ]
    },
    {
      phase: 'Data Restoration (60-120 minutes)',
      actions: [
        'Restore database from snapshot',
        'Verify data integrity',
        'Restore S3 data from backups',
        'Populate caches',
        'Sync with any available region'
      ]
    },
    {
      phase: 'Service Activation (120-180 minutes)',
      actions: [
        'Update DNS routing',
        'Run comprehensive tests',
        'Verify all services',
        'Monitor error rates',
        'Confirm customer access'
      ]
    },
    {
      phase: 'Stabilization (180-240 minutes)',
      actions: [
        'Monitor system performance',
        'Optimize configuration',
        'Update monitoring',
        'Communicate with customers',
        'Plan long-term recovery'
      ]
    }
  ],
  
  rto: '< 4 hours',
  rpo: '< 1 hour',
  
  dataLoss: {
    risk: 'Potential data loss since last snapshot',
    mitigation: 'Hourly snapshots, transaction logs'
  }
}
```

**Data Replication Strategy**:

```typescript
interface DataReplicationStrategy {
  tier1_critical: {
    services: ['Order', 'Payment', 'Authentication'],
    replication: {
      singapore: 'Async replication (< 5 min lag)',
      seoul: 'Hourly snapshots'
    },
    rpo: '< 15 minutes'
  },
  
  tier2_important: {
    services: ['Customer', 'Inventory', 'Product'],
    replication: {
      singapore: 'Async replication (< 15 min lag)',
      seoul: 'Daily snapshots'
    },
    rpo: '< 1 hour'
  },
  
  tier3_standard: {
    services: ['Analytics', 'Reporting', 'Logs'],
    replication: {
      singapore: 'Async replication (< 1 hour lag)',
      seoul: 'Weekly snapshots'
    },
    rpo: '< 24 hours'
  }
}
```

**Cost Breakdown**:

```typescript
const thirdRegionCosts = {
  singapore: {
    compute: 36000,      // $3,000/month
    database: 30000,     // $2,500/month
    cache: 12000,        // $1,000/month
    storage: 6000,       // $500/month
    network: 12000,      // $1,000/month
    subtotal: 96000
  },
  
  seoul: {
    storage: 6000,       // $500/month (snapshots only)
    subtotal: 6000
  },
  
  dataTransfer: {
    taiwanToSingapore: 12000,  // $1,000/month
    tokyoToSingapore: 12000,   // $1,000/month
    subtotal: 24000
  },
  
  total: 126000,  // $126,000/year
  
  note: 'Exceeds $50K budget - requires optimization or budget increase'
}
```

**Cost Optimization Options**:

1. **Reduce Singapore capacity to 10%**: Save $18,000/year
2. **Use Spot instances**: Save $15,000/year
3. **Optimize data transfer**: Save $10,000/year
4. **Use smaller database instance**: Save $12,000/year
5. **Total potential savings**: $55,000/year
6. **Optimized cost**: $71,000/year (still over budget)

**Pros**:

- ✅ True geographic diversity
- ✅ Fast activation (< 2 hours)
- ✅ Low data loss (< 15 minutes RPO)
- ✅ Automated failover capability
- ✅ Two backup options (Singapore + Seoul)
- ✅ Supports data sovereignty
- ✅ Regional expansion opportunity

**Cons**:

- ⚠️ Cost exceeds budget ($126K vs $50K)
- ⚠️ Operational complexity
- ⚠️ Data transfer costs
- ⚠️ Requires cost optimization

**Cost**: $126,000/year (requires optimization to $71K)

**Risk**: **Low** - Comprehensive coverage

### Option 2: Seoul Only (Cost-Optimized)

**Description**: Deploy Seoul as single third region (warm standby)

**Pros**:

- ✅ Lower cost ($60K/year)
- ✅ Simpler operations
- ✅ Geographic proximity

**Cons**:

- ❌ Still in East Asia (limited diversity)
- ❌ Single backup option
- ❌ Higher correlated risk

**Cost**: $60,000/year

**Risk**: **Medium** - Limited geographic diversity

### Option 3: Mumbai as Third Region

**Description**: Deploy Mumbai (ap-south-1) as third region

**Pros**:

- ✅ True geographic diversity
- ✅ Lower cost than Singapore
- ✅ Growing AWS region

**Cons**:

- ❌ Higher latency (~150ms)
- ❌ Less mature infrastructure
- ❌ Potential connectivity issues

**Cost**: $80,000/year

**Risk**: **Medium** - Latency and infrastructure concerns

## Decision Outcome

**Chosen Option**: **Singapore as Primary Third Region with Seoul Backup (Option 1)** with cost optimization

### Rationale

Singapore provides optimal geographic diversity with acceptable latency, while Seoul offers cost-effective backup option.

**Cost Optimization Plan**:

1. Reduce Singapore capacity to 15% (save $12K)
2. Use Spot instances for non-critical workloads (save $15K)
3. Optimize data transfer with caching (save $10K)
4. Use smaller database instance initially (save $12K)
5. Implement lifecycle policies for storage (save $6K)

**Optimized Cost**: $71,000/year (requires $21K budget increase or further optimization)

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Executive Team | High | Budget increase required | ROI analysis, phased approach |
| Operations Team | High | Manage third region infrastructure | Training, automation, runbooks |
| Development Team | Medium | Support multi-region testing | Documentation, tooling |
| Finance Team | High | Budget approval for cost increase | Cost-benefit analysis |
| Customers | Low | Transparent failover | Communication plan |

### Impact Radius Assessment

**Selected Impact Radius**: **System**

Affects:

- All services and data
- Infrastructure architecture
- Disaster recovery procedures
- Cost structure
- Operational processes

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Cost overruns | Medium | High | Strict cost monitoring, optimization |
| Activation failure | Low | Critical | Regular testing, automation |
| Data loss during failover | Low | High | Frequent replication, testing |
| Latency increase | Low | Medium | Performance monitoring, optimization |
| Operational complexity | Medium | Medium | Automation, training, documentation |

**Overall Risk Level**: **Low**

## Implementation Plan

### Phase 1: Singapore Infrastructure Setup (Month 1-2)

**Tasks**:

- [ ] Deploy EKS cluster in Singapore
- [ ] Set up RDS read replica
- [ ] Configure ElastiCache
- [ ] Set up S3 cross-region replication
- [ ] Configure VPC and networking
- [ ] Deploy monitoring infrastructure
- [ ] Test data replication
- [ ] Perform initial failover test

**Success Criteria**:

- Singapore infrastructure operational
- Data replication working (< 5 min lag)
- Initial failover test successful

### Phase 2: Seoul Backup Setup (Month 3)

**Tasks**:

- [ ] Create IaC templates for Seoul
- [ ] Set up automated snapshots
- [ ] Configure backup replication
- [ ] Test infrastructure deployment
- [ ] Test data restoration
- [ ] Document activation procedures

**Success Criteria**:

- Seoul can be deployed < 2 hours
- Backup restoration tested
- Procedures documented

### Phase 3: Automation & Testing (Month 4-5)

**Tasks**:

- [ ] Implement automated failover
- [ ] Create activation runbooks
- [ ] Conduct failover drills
- [ ] Test data integrity
- [ ] Optimize performance
- [ ] Train operations team

**Success Criteria**:

- Automated failover working
- RTO < 2 hours achieved
- RPO < 15 minutes achieved
- Team trained

### Phase 4: Cost Optimization (Month 6)

**Tasks**:

- [ ] Implement Spot instances
- [ ] Optimize data transfer
- [ ] Right-size resources
- [ ] Implement lifecycle policies
- [ ] Monitor and adjust

**Success Criteria**:

- Cost reduced to $71K/year
- Performance maintained
- Availability maintained

### Rollback Strategy

**Trigger Conditions**:

- Cost exceeds budget significantly
- Performance degradation
- Activation failures

**Rollback Steps**:

1. Decommission Singapore infrastructure
2. Return to two-region architecture
3. Maintain Seoul snapshots only
4. Re-evaluate strategy

**Rollback Time**: < 1 week

## Monitoring and Success Criteria

### Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Singapore Activation Time | < 2 hours | Drill results |
| Seoul Activation Time | < 4 hours | Drill results |
| Data Replication Lag | < 5 minutes | Monitoring |
| RPO Achievement | < 15 minutes | Actual failover |
| System Availability | > 99.9% | CloudWatch |
| Cost | < $75K/year | Cost reports |
| Drill Success Rate | > 95% | Drill results |

### Review Schedule

- **Monthly**: Cost review, performance monitoring
- **Quarterly**: Failover drill, procedure review
- **Annually**: Comprehensive strategy review

## Consequences

### Positive Consequences

- ✅ **True Geographic Diversity**: Singapore in Southeast Asia
- ✅ **Fast Recovery**: < 2 hour RTO
- ✅ **Low Data Loss**: < 15 minute RPO
- ✅ **Dual Backup Options**: Singapore + Seoul
- ✅ **Automated Failover**: Reduces manual effort
- ✅ **Regional Expansion**: Foundation for APAC growth
- ✅ **Customer Confidence**: Demonstrated resilience

### Negative Consequences

- ⚠️ **Cost Increase**: $71K/year (42% over budget)
- ⚠️ **Operational Complexity**: More regions to manage
- ⚠️ **Data Transfer Costs**: Cross-region replication
- ⚠️ **Testing Overhead**: Regular drills required
- ⚠️ **Latency Considerations**: Singapore slightly higher latency

### Technical Debt

**Identified Debt**:

1. Manual cost optimization
2. Basic failover automation
3. Limited testing scenarios
4. Manual capacity planning

**Debt Repayment Plan**:

- **Q2 2026**: Advanced automation
- **Q3 2026**: AI-powered capacity planning
- **Q4 2026**: Predictive failover

## Related Decisions

- [ADR-040: Network Partition Handling Strategy](040-network-partition-handling-strategy.md)

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-25  
**Next Review**: 2026-01-25 (Quarterly)

## Notes

### Geographic Diversity Analysis

**Current Risk (Taiwan + Tokyo)**:

- Both in East Asia
- Similar seismic risk
- Similar typhoon risk
- Correlated geopolitical risk
- Limited diversity

**With Singapore**:

- Southeast Asia vs East Asia
- Different seismic zones
- Different weather patterns
- Different geopolitical context
- True geographic diversity

### Latency Comparison

| Route | Latency | Impact |
|-------|---------|--------|
| Taiwan ↔ Tokyo | ~30ms | Excellent |
| Taiwan ↔ Singapore | ~50ms | Good |
| Tokyo ↔ Singapore | ~80ms | Acceptable |
| Taiwan ↔ Seoul | ~40ms | Good |
| Tokyo ↔ Seoul | ~30ms | Excellent |

### Alternative Regions Considered

**Hong Kong**: Rejected due to geopolitical concerns
**Sydney**: Rejected due to high latency (~150ms)
**Mumbai**: Considered but higher latency (~150ms)
**Osaka**: Rejected due to proximity to Tokyo

### Budget Considerations

**Original Budget**: $50,000/year
**Actual Cost**: $126,000/year
**Optimized Cost**: $71,000/year
**Budget Gap**: $21,000/year

**Options**:

1. Request budget increase
2. Further cost optimization
3. Phased implementation
4. Reduce scope (Seoul only)

**Recommendation**: Request $25K budget increase with commitment to optimize to $71K
