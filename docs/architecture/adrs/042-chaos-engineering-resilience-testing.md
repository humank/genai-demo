---
adr_number: 042
title: "Chaos Engineering and Resilience Testing Strategy"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [037, 038, 040, 043]
affected_viewpoints: ["deployment", "operational"]
affected_perspectives: ["availability", "performance"]
---

# ADR-042: Chaos Engineering and Resilience Testing Strategy

## Status

**Accepted** - 2025-10-25

## Context

### Problem Statement

Active-active multi-region architecture requires systematic resilience testing to ensure system reliability:

**Resilience Challenges**:

- **Complex Failure Modes**: Multiple failure scenarios in distributed systems
- **Unknown Weaknesses**: Hidden vulnerabilities in system design
- **Cascading Failures**: Failures that propagate across components
- **Recovery Validation**: Unproven recovery procedures
- **Confidence Gap**: Uncertainty about system behavior under stress

**Testing Gaps**:

- Traditional testing doesn't cover distributed system failures
- Manual testing is time-consuming and incomplete
- Production incidents reveal unknown weaknesses
- Recovery procedures untested until needed
- No systematic approach to resilience validation

**Business Impact**:

- Unexpected downtime (target: 99.9% availability)
- Revenue loss during outages
- Customer trust erosion
- Incident response delays
- Compliance violations

### Business Context

**Business Drivers**:

- Availability SLO: 99.9% (8.76 hours downtime/year)
- RTO: 5 minutes for critical services
- RPO: 1 minute for transactional data
- Customer trust and satisfaction
- Competitive advantage (reliability)

**Constraints**:

- Budget: $50,000/year for chaos engineering tools
- Cannot disrupt production during business hours
- Must maintain customer experience
- Limited team capacity for testing
- Regulatory compliance requirements

### Technical Context

**Current State**:

- Active-active architecture in Taiwan and Tokyo
- Manual failover testing (quarterly)
- No systematic chaos testing
- Limited failure scenario coverage
- Reactive incident response

**Requirements**:

- Automated chaos experiments
- Comprehensive failure scenario coverage
- Production-safe testing
- Continuous resilience validation
- Measurable resilience metrics

## Decision Drivers

1. **Reliability**: Achieve 99.9% availability SLO
2. **Confidence**: Validate system resilience continuously
3. **Safety**: Test without customer impact
4. **Coverage**: Test all critical failure scenarios
5. **Automation**: Reduce manual testing effort
6. **Learning**: Build organizational resilience knowledge
7. **Cost**: Optimize testing infrastructure costs
8. **Compliance**: Meet regulatory requirements

## Considered Options

### Option 1: Comprehensive Chaos Engineering Platform (Recommended)

**Description**: Implement full chaos engineering platform with automated experiments

**Chaos Engineering Approach**:

**1. Chaos Experiment Framework**:

```typescript
// Chaos experiment definition
interface ChaosExperiment {
  id: string;
  name: string;
  description: string;
  hypothesis: string;
  steadyState: SteadyStateDefinition;
  method: ExperimentMethod;
  rollback: RollbackStrategy;
  schedule: ExperimentSchedule;
  safetyChecks: SafetyCheck[];
}

interface SteadyStateDefinition {
  metrics: MetricDefinition[];
  thresholds: Threshold[];
  duration: Duration;
}

interface ExperimentMethod {
  steps: ExperimentStep[];
  duration: Duration;
  blast_radius: BlastRadius;
}

// Example: Region failure experiment
const regionFailureExperiment: ChaosExperiment = {
  id: 'exp-001',
  name: 'Taiwan Region Failure',
  description: 'Simulate complete Taiwan region failure',
  hypothesis: 'System continues operating with Tokyo region only',
  
  steadyState: {
    metrics: [
      { name: 'http_success_rate', threshold: 0.999 },
      { name: 'response_time_p95', threshold: 2000 },
      { name: 'order_completion_rate', threshold: 0.99 }
    ],
    duration: '5m'
  },
  
  method: {
    steps: [
      {
        type: 'network_partition',
        target: 'taiwan_region',
        action: 'isolate',
        duration: '10m'
      }
    ],
    duration: '10m',
    blast_radius: {
      scope: 'region',
      percentage: 50
    }
  },
  
  rollback: {
    automatic: true,
    timeout: '15m',
    steps: [
      { action: 'restore_network', target: 'taiwan_region' }
    ]
  },
  
  schedule: {
    frequency: 'weekly',
    time: '02:00 UTC',
    enabled: true
  },
  
  safetyChecks: [
    { type: 'business_hours', enabled: false },
    { type: 'active_incidents', enabled: true },
    { type: 'deployment_freeze', enabled: true }
  ]
};
```

**2. Failure Injection Scenarios**:

**Infrastructure Failures**:

- Region failure (complete region unavailable)
- AZ failure (single availability zone down)
- Network partition (split-brain scenarios)
- Network latency (increased latency between regions)
- DNS failure (DNS resolution issues)

**Service Failures**:

- Pod termination (random pod kills)
- Service degradation (CPU/memory pressure)
- Database failure (primary database down)
- Cache failure (Redis cluster down)
- Message queue failure (Kafka broker down)

**Application Failures**:

- API errors (inject 500 errors)
- Timeout injection (slow responses)
- Exception injection (application errors)
- Resource exhaustion (memory leaks)
- Dependency failures (external service down)

**Data Failures**:

- Data corruption (invalid data injection)
- Replication lag (delayed replication)
- Consistency violations (conflicting updates)
- Storage failure (disk full scenarios)

**3. Chaos Tools Integration**:

**Chaos Mesh** (Kubernetes-native):

```yaml
# Network chaos experiment
apiVersion: chaos-mesh.org/v1alpha1
kind: NetworkChaos
metadata:
  name: taiwan-region-partition
  namespace: chaos-testing
spec:
  action: partition
  mode: all
  selector:
    namespaces:

      - production

    labelSelectors:
      region: taiwan
  direction: both
  duration: "10m"
  scheduler:
    cron: "@weekly"
```

**AWS Fault Injection Simulator**:

```yaml
# RDS failover experiment
experiment:
  name: rds-failover-test
  description: Test RDS automatic failover
  actions:

    - name: failover-rds

      actionId: aws:rds:reboot-db-instances
      parameters:
        dbInstanceIdentifier: ecommerce-taiwan-primary
        forceFailover: true
  stopConditions:

    - source: aws:cloudwatch:alarm

      value: HighErrorRate
  targets:

    - resourceType: aws:rds:db

      resourceArns:

        - arn:aws:rds:ap-northeast-1:*:db:ecommerce-taiwan-primary

```

**Litmus Chaos**:

```yaml
# Pod delete chaos
apiVersion: litmuschaos.io/v1alpha1
kind: ChaosEngine
metadata:
  name: order-service-chaos
  namespace: production
spec:
  appinfo:
    appns: production
    applabel: 'app=order-service'
    appkind: deployment
  chaosServiceAccount: litmus-admin
  experiments:

    - name: pod-delete

      spec:
        components:
          env:

            - name: TOTAL_CHAOS_DURATION

              value: '60'

            - name: CHAOS_INTERVAL

              value: '10'

            - name: FORCE

              value: 'false'
```

**4. Observability Integration**:

**Experiment Monitoring**:

```typescript
// Real-time experiment monitoring
class ChaosExperimentMonitor {
  
  async monitorExperiment(experimentId: string): Promise<ExperimentResult> {
    const experiment = await this.getExperiment(experimentId);
    
    // Establish steady state baseline
    const baseline = await this.measureSteadyState(
      experiment.steadyState,
      experiment.steadyState.duration
    );
    
    // Execute chaos injection
    await this.executeExperiment(experiment.method);
    
    // Monitor during chaos
    const duringChaos = await this.measureSteadyState(
      experiment.steadyState,
      experiment.method.duration
    );
    
    // Rollback
    await this.rollback(experiment.rollback);
    
    // Measure recovery
    const afterRecovery = await this.measureSteadyState(
      experiment.steadyState,
      experiment.steadyState.duration
    );
    
    // Analyze results
    return this.analyzeResults(baseline, duringChaos, afterRecovery);
  }
  
  private async measureSteadyState(
    definition: SteadyStateDefinition,
    duration: Duration
  ): Promise<SteadyStateMetrics> {
    const metrics: Record<string, number[]> = {};
    
    const endTime = Date.now() + duration.toMillis();
    
    while (Date.now() < endTime) {
      for (const metric of definition.metrics) {
        const value = await this.queryMetric(metric.name);
        metrics[metric.name] = metrics[metric.name] || [];
        metrics[metric.name].push(value);
      }
      
      await this.sleep(10000); // Sample every 10s
    }
    
    return this.aggregateMetrics(metrics);
  }
  
  private analyzeResults(
    baseline: SteadyStateMetrics,
    duringChaos: SteadyStateMetrics,
    afterRecovery: SteadyStateMetrics
  ): ExperimentResult {
    const deviations = this.calculateDeviations(baseline, duringChaos);
    const recoveryTime = this.calculateRecoveryTime(duringChaos, afterRecovery);
    
    return {
      success: deviations.every(d => d.withinThreshold),
      deviations,
      recoveryTime,
      insights: this.generateInsights(deviations, recoveryTime)
    };
  }
}
```

**Metrics Collection**:

```typescript
const chaosMetrics = {
  // Experiment execution
  'chaos.experiments.total': 'Count',
  'chaos.experiments.success': 'Count',
  'chaos.experiments.failure': 'Count',
  'chaos.experiments.duration': 'Milliseconds',
  
  // System resilience
  'chaos.steady_state.deviation': 'Percentage',
  'chaos.recovery.time': 'Seconds',
  'chaos.availability.during_chaos': 'Percentage',
  
  // Failure scenarios
  'chaos.failures.injected': 'Count',
  'chaos.failures.detected': 'Count',
  'chaos.failures.recovered': 'Count',
};
```

**5. Game Days and Disaster Recovery Drills**:

**Quarterly Game Day Schedule**:

```typescript
interface GameDay {
  id: string;
  name: string;
  date: Date;
  duration: Duration;
  scenarios: GameDayScenario[];
  participants: Participant[];
  objectives: string[];
}

const q1GameDay: GameDay = {
  id: 'gameday-2026-q1',
  name: 'Multi-Region Failover Drill',
  date: new Date('2026-01-15'),
  duration: Duration.ofHours(4),
  
  scenarios: [
    {
      name: 'Taiwan Region Complete Failure',
      description: 'Simulate complete Taiwan region outage',
      duration: Duration.ofMinutes(30),
      expectedActions: [
        'Automatic failover to Tokyo',
        'DNS update to Tokyo endpoints',
        'Customer notification',
        'Incident declaration'
      ],
      successCriteria: [
        'RTO < 5 minutes',
        'No data loss',
        'Customer impact < 1%'
      ]
    },
    {
      name: 'Database Replication Failure',
      description: 'Simulate replication lag and conflict',
      duration: Duration.ofMinutes(45),
      expectedActions: [
        'Detect replication lag',
        'Switch to synchronous replication',
        'Resolve conflicts',
        'Resume normal operations'
      ],
      successCriteria: [
        'Conflicts resolved < 10 minutes',
        'No data inconsistency',
        'Replication restored'
      ]
    }
  ],
  
  participants: [
    { role: 'Incident Commander', team: 'SRE' },
    { role: 'Technical Lead', team: 'Engineering' },
    { role: 'Communications', team: 'Support' },
    { role: 'Observer', team: 'Management' }
  ],
  
  objectives: [
    'Validate failover procedures',
    'Test incident response',
    'Identify gaps in runbooks',
    'Build team confidence'
  ]
};
```

**6. Resilience Scoring**:

**Resilience Score Calculation**:

```typescript
interface ResilienceScore {
  overall: number; // 0-100
  categories: {
    availability: number;
    recovery: number;
    detection: number;
    automation: number;
  };
  trends: {
    improving: boolean;
    changeRate: number;
  };
}

class ResilienceScoreCalculator {
  
  calculateScore(experiments: ExperimentResult[]): ResilienceScore {
    const availability = this.calculateAvailabilityScore(experiments);
    const recovery = this.calculateRecoveryScore(experiments);
    const detection = this.calculateDetectionScore(experiments);
    const automation = this.calculateAutomationScore(experiments);
    
    const overall = (
      availability * 0.4 +
      recovery * 0.3 +
      detection * 0.2 +
      automation * 0.1
    );
    
    return {
      overall,
      categories: { availability, recovery, detection, automation },
      trends: this.calculateTrends(experiments)
    };
  }
  
  private calculateAvailabilityScore(experiments: ExperimentResult[]): number {
    // Score based on availability during chaos
    const availabilities = experiments.map(e => e.availabilityDuringChaos);
    const avgAvailability = this.average(availabilities);
    
    // 99.9% = 100 points, 99% = 90 points, etc.
    return Math.min(100, (avgAvailability - 0.99) * 1000);
  }
  
  private calculateRecoveryScore(experiments: ExperimentResult[]): number {
    // Score based on recovery time
    const recoveryTimes = experiments.map(e => e.recoveryTime);
    const avgRecoveryTime = this.average(recoveryTimes);
    
    // < 1 min = 100 points, < 5 min = 80 points, etc.
    if (avgRecoveryTime < 60) return 100;
    if (avgRecoveryTime < 300) return 80;
    if (avgRecoveryTime < 600) return 60;
    return 40;
  }
}
```

**Pros**:

- ✅ Comprehensive resilience validation
- ✅ Automated continuous testing
- ✅ Production-safe experiments
- ✅ Measurable resilience metrics
- ✅ Organizational learning
- ✅ Proactive weakness discovery

**Cons**:

- ⚠️ Initial setup complexity
- ⚠️ Requires cultural change
- ⚠️ Tool integration effort
- ⚠️ Ongoing maintenance

**Cost**: $50,000/year

**Risk**: **Low** - Industry best practice

### Option 2: Manual Quarterly Testing

**Description**: Manual disaster recovery drills quarterly

**Pros**:

- ✅ Simple to implement
- ✅ Low cost
- ✅ Controlled testing

**Cons**:

- ❌ Infrequent testing
- ❌ Limited coverage
- ❌ Manual effort
- ❌ No continuous validation

**Cost**: $10,000/year

**Risk**: **High** - Insufficient coverage

### Option 3: Production Monitoring Only

**Description**: Rely on production monitoring without chaos testing

**Pros**:

- ✅ No testing overhead
- ✅ Minimal cost

**Cons**:

- ❌ Reactive approach
- ❌ Unknown weaknesses
- ❌ Customer impact
- ❌ No resilience validation

**Cost**: $5,000/year

**Risk**: **Critical** - Unacceptable

## Decision Outcome

**Chosen Option**: **Comprehensive Chaos Engineering Platform (Option 1)**

### Rationale

Comprehensive chaos engineering provides:

1. **Proactive**: Discover weaknesses before customers do
2. **Continuous**: Ongoing resilience validation
3. **Automated**: Reduce manual testing effort
4. **Safe**: Production-safe experiments
5. **Measurable**: Quantifiable resilience metrics
6. **Learning**: Build organizational resilience knowledge

### Implementation Architecture

**Chaos Engineering Stack**:

```text
┌─────────────────────────────────────────────────────────┐
│                  Chaos Control Plane                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   Chaos      │  │  Experiment  │  │   Safety     │  │
│  │  Scheduler   │  │   Monitor    │  │   Guardian   │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
┌───────▼────────┐ ┌──────▼──────┐ ┌───────▼────────┐
│  Chaos Mesh    │ │  AWS FIS    │ │    Litmus      │
│  (K8s Native)  │ │  (AWS Infra)│ │   (Generic)    │
└────────────────┘ └─────────────┘ └────────────────┘
        │                 │                 │
        └─────────────────┼─────────────────┘
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
┌───────▼────────┐ ┌──────▼──────┐ ┌───────▼────────┐
│   Taiwan       │ │    Tokyo    │ │  Observability │
│   Region       │ │   Region    │ │    Stack       │
└────────────────┘ └─────────────┘ └────────────────┘
```

### Experiment Catalog

**Phase 1 Experiments (Month 1-3)**:

1. Pod termination (random pod kills)
2. Network latency injection
3. CPU stress testing
4. Memory pressure testing
5. Database connection exhaustion

**Phase 2 Experiments (Month 4-6)**:

1. AZ failure simulation
2. Database failover testing
3. Cache cluster failure
4. Message queue failure
5. API error injection

**Phase 3 Experiments (Month 7-9)**:

1. Region failure simulation
2. Network partition (split-brain)
3. Data replication lag
4. DNS failure simulation
5. Cross-region latency

**Phase 4 Experiments (Month 10-12)**:

1. Cascading failure scenarios
2. Resource exhaustion
3. Security incident simulation
4. Compliance violation scenarios
5. Full disaster recovery drill

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| SRE Team | High | Implement and maintain chaos platform | Training, automation |
| Development Team | Medium | Fix discovered issues | Prioritization, support |
| Operations Team | Medium | Participate in game days | Training, runbooks |
| Customers | Low | Transparent testing | Non-business hours |
| Business | Low | Improved reliability | ROI analysis |

### Impact Radius

**Selected Impact Radius**: **System**

Affects:

- All services and components
- Infrastructure layer
- Monitoring and alerting
- Incident response procedures
- Team processes

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Customer impact | Low | High | Safety checks, non-business hours |
| False positives | Medium | Low | Baseline validation |
| Tool complexity | Medium | Medium | Training, documentation |
| Cultural resistance | Medium | Medium | Education, gradual adoption |
| Experiment failures | High | Low | Automated rollback |

**Overall Risk Level**: **Low**

## Implementation Plan

### Phase 1: Foundation (Month 1-2)

**Objectives**:

- Set up chaos engineering platform
- Define experiment framework
- Establish safety guardrails

**Tasks**:

- [ ] Deploy Chaos Mesh on EKS
- [ ] Configure AWS FIS
- [ ] Implement experiment scheduler
- [ ] Create safety check system
- [ ] Set up monitoring integration

**Success Criteria**:

- Platform operational
- Safety checks working
- First experiment executed

### Phase 2: Basic Experiments (Month 3-4)

**Objectives**:

- Execute basic failure scenarios
- Validate system resilience
- Build team confidence

**Tasks**:

- [ ] Pod termination experiments
- [ ] Network latency experiments
- [ ] Resource pressure experiments
- [ ] Document findings
- [ ] Fix discovered issues

**Success Criteria**:

- 5 experiments executed
- Issues identified and fixed
- Team trained

### Phase 3: Advanced Experiments (Month 5-6)

**Objectives**:

- Test complex failure scenarios
- Validate recovery procedures
- Improve automation

**Tasks**:

- [ ] AZ failure experiments
- [ ] Database failover experiments
- [ ] Cache failure experiments
- [ ] Automate rollback
- [ ] Update runbooks

**Success Criteria**:

- 10 experiments executed
- Recovery procedures validated
- Automation improved

### Phase 4: Multi-Region Testing (Month 7-8)

**Objectives**:

- Test region-level failures
- Validate cross-region failover
- Conduct game days

**Tasks**:

- [ ] Region failure experiments
- [ ] Network partition experiments
- [ ] First game day
- [ ] Update DR procedures
- [ ] Measure resilience score

**Success Criteria**:

- Region failover validated
- Game day successful
- Resilience score > 80

### Phase 5: Continuous Improvement (Month 9-12)

**Objectives**:

- Establish continuous testing
- Optimize experiments
- Build resilience culture

**Tasks**:

- [ ] Weekly automated experiments
- [ ] Monthly game days
- [ ] Quarterly DR drills
- [ ] Resilience dashboard
- [ ] Team training program

**Success Criteria**:

- Automated testing operational
- Resilience score > 90
- Team confidence high

### Rollback Strategy

**Trigger Conditions**:

- Customer impact detected
- Safety check failures
- Experiment out of control
- Business critical incident

**Rollback Steps**:

1. **Immediate**: Stop all chaos experiments
2. **Restore**: Execute automated rollback
3. **Verify**: Confirm system stability
4. **Investigate**: Root cause analysis
5. **Resume**: Gradual re-enablement

**Rollback Time**: < 5 minutes

## Monitoring and Success Criteria

### Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Availability During Chaos | > 99.9% | Experiment monitoring |
| Recovery Time | < 5 minutes | Experiment results |
| Experiments Per Month | > 20 | Experiment logs |
| Issues Discovered | > 10/quarter | Issue tracking |
| Resilience Score | > 90 | Score calculation |
| Game Days Per Quarter | 1 | Schedule |

### Key Metrics

```typescript
const resilienceMetrics = {
  // Experiments
  'resilience.experiments.total': 'Count',
  'resilience.experiments.success_rate': 'Percentage',
  'resilience.experiments.duration': 'Minutes',
  
  // System behavior
  'resilience.availability.during_chaos': 'Percentage',
  'resilience.recovery.time': 'Seconds',
  'resilience.steady_state.deviation': 'Percentage',
  
  // Issues
  'resilience.issues.discovered': 'Count',
  'resilience.issues.fixed': 'Count',
  'resilience.issues.severity': 'Distribution',
  
  // Score
  'resilience.score.overall': 'Score',
  'resilience.score.availability': 'Score',
  'resilience.score.recovery': 'Score',
};
```

### Review Schedule

- **Daily**: Experiment results review
- **Weekly**: Resilience metrics review
- **Monthly**: Game day planning
- **Quarterly**: DR drill, resilience assessment
- **Annually**: Strategy review

## Consequences

### Positive Consequences

- ✅ **Proactive**: Discover issues before customers
- ✅ **Confidence**: Validated system resilience
- ✅ **Automation**: Reduced manual testing
- ✅ **Learning**: Organizational resilience knowledge
- ✅ **Metrics**: Quantifiable resilience
- ✅ **Culture**: Resilience-focused mindset

### Negative Consequences

- ⚠️ **Complexity**: Additional operational complexity
- ⚠️ **Cost**: $50,000/year platform costs
- ⚠️ **Effort**: Team time for experiments
- ⚠️ **Risk**: Potential customer impact
- ⚠️ **Cultural**: Requires mindset change

### Technical Debt

**Identified Debt**:

1. Manual experiment creation
2. Limited blast radius control
3. Basic safety checks
4. Manual result analysis

**Debt Repayment Plan**:

- **Q2 2026**: Automated experiment generation
- **Q3 2026**: Advanced blast radius control
- **Q4 2026**: AI-powered safety checks
- **2027**: Automated result analysis and recommendations

## Related Decisions

- [ADR-037: Active-Active Multi-Region Architecture](037-active-active-multi-region-architecture.md) - Architecture foundation
- [ADR-038: Cross-Region Data Replication Strategy](038-cross-region-data-replication-strategy.md) - Replication resilience
- [ADR-040: Network Partition Handling Strategy](040-network-partition-handling-strategy.md) - Partition resilience
- [ADR-043: Observability for Multi-Region Operations](043-observability-multi-region-operations.md) - Monitoring integration

## Notes

### Chaos Engineering Principles

1. **Build a Hypothesis**: Define expected steady state
2. **Vary Real-World Events**: Inject realistic failures
3. **Run Experiments in Production**: Test real system
4. **Automate Experiments**: Continuous validation
5. **Minimize Blast Radius**: Limit experiment scope

### Safety Guidelines

**Pre-Experiment Checks**:

- No active incidents
- No deployment freeze
- Outside business hours (for high-risk experiments)
- Monitoring systems operational
- Rollback procedures ready

**During Experiment**:

- Real-time monitoring
- Automatic safety checks
- Manual abort capability
- Customer impact monitoring
- Team availability

**Post-Experiment**:

- Result analysis
- Issue documentation
- Runbook updates
- Team retrospective
- Continuous improvement

### Experiment Design Best Practices

**Good Experiment**:

- Clear hypothesis
- Measurable steady state
- Realistic failure scenario
- Automated rollback
- Safety guardrails

**Bad Experiment**:

- Vague hypothesis
- Unmeasurable outcomes
- Unrealistic scenario
- Manual rollback
- No safety checks

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-25  
**Next Review**: 2026-01-25 (Quarterly)
