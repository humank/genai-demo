# Chaos Engineering for Resilience Testing

> **Last Updated**: 2025-11-17  
> **Status**: 🔄 In Progress  
> **Owner**: SRE & Platform Team

## Purpose

This document describes the chaos engineering practices used to proactively identify and address weaknesses in the Enterprise E-Commerce Platform's availability and resilience. By intentionally injecting failures in a controlled manner, we validate that our automated failover mechanisms work as designed and identify areas for improvement.

## Chaos Engineering Principles

### Core Principles

1. **Build a Hypothesis**: Define expected system behavior during failure
2. **Vary Real-World Events**: Simulate realistic failure scenarios
3. **Run Experiments in Production**: Test in production-like environments
4. **Automate Experiments**: Make chaos engineering continuous
5. **Minimize Blast Radius**: Limit impact of experiments

### Our Approach

```text
┌──────────────────────────────────────────────────────────┐
│ 1. Define Steady State                                   │
│    - Normal system behavior and metrics                  │
└────────────────────┬─────────────────────────────────────┘
                     │
┌────────────────────▼─────────────────────────────────────┐
│ 2. Hypothesize Steady State Continues                   │
│    - System should maintain SLOs during failure         │
└────────────────────┬─────────────────────────────────────┘
                     │
┌────────────────────▼─────────────────────────────────────┐
│ 3. Introduce Real-World Variables                       │
│    - Pod failures, network latency, resource exhaustion │
└────────────────────┬─────────────────────────────────────┘
                     │
┌────────────────────▼─────────────────────────────────────┐
│ 4. Disprove Hypothesis                                   │
│    - Identify weaknesses and improvement opportunities  │
└──────────────────────────────────────────────────────────┘
```

## Chaos Engineering Tools

### AWS Fault Injection Simulator (FIS)

#### Setup

```typescript
// infrastructure/lib/stacks/chaos-engineering-stack.ts
export class ChaosEngineeringStack extends Stack {
  constructor(scope: Construct, id: string, props: StackProps) {
    super(scope, id, props);

    // IAM role for FIS
    const fisRole = new iam.Role(this, 'FISRole', {
      assumedBy: new iam.ServicePrincipal('fis.amazonaws.com'),
      managedPolicies: [
        iam.ManagedPolicy.fromAwsManagedPolicyName('AWSFaultInjectionSimulatorEC2Access'),
        iam.ManagedPolicy.fromAwsManagedPolicyName('AWSFaultInjectionSimulatorEKSAccess'),
        iam.ManagedPolicy.fromAwsManagedPolicyName('AWSFaultInjectionSimulatorRDSAccess'),
      ],
    });

    // Experiment template: Terminate random EKS pods
    new fis.CfnExperimentTemplate(this, 'TerminatePodsTemplate', {
      description: 'Terminate random pods to test Kubernetes self-healing',
      roleArn: fisRole.roleArn,
      stopConditions: [{
        source: 'aws:cloudwatch:alarm',
        value: errorRateAlarm.alarmArn,
      }],
      targets: {
        'Pods': {
          resourceType: 'aws:eks:pod',
          selectionMode: 'COUNT(2)',
          resourceTags: {
            'app': 'backend-api'
          },
        }
      },
      actions: {
        'TerminatePods': {
          actionId: 'aws:eks:terminate-nodegroup-instances',
          parameters: {
            'instanceTerminationPercentage': '20'
          },
          targets: {
            'Nodegroups': 'Pods'
          }
        }
      },
      tags: {
        'Name': 'Terminate Pods Experiment',
        'Environment': 'staging'
      }
    });

    // Experiment template: Inject network latency
    new fis.CfnExperimentTemplate(this, 'NetworkLatencyTemplate', {
      description: 'Inject network latency to test timeout handling',
      roleArn: fisRole.roleArn,
      stopConditions: [{
        source: 'aws:cloudwatch:alarm',
        value: latencyAlarm.alarmArn,
      }],
      targets: {
        'Subnets': {
          resourceType: 'aws:ec2:subnet',
          selectionMode: 'ALL',
          resourceTags: {
            'Environment': 'staging'
          },
        }
      },
      actions: {
        'InjectLatency': {
          actionId: 'aws:network:disrupt-connectivity',
          parameters: {
            'duration': 'PT5M',
            'scope': 'all',
            'delayMilliseconds': '500'
          },
          targets: {
            'Subnets': 'Subnets'
          }
        }
      }
    });

    // Experiment template: Throttle RDS connections
    new fis.CfnExperimentTemplate(this, 'RDSThrottleTemplate', {
      description: 'Throttle RDS connections to test connection pool handling',
      roleArn: fisRole.roleArn,
      stopConditions: [{
        source: 'aws:cloudwatch:alarm',
        value: databaseErrorAlarm.alarmArn,
      }],
      targets: {
        'DBClusters': {
          resourceType: 'aws:rds:cluster',
          selectionMode: 'ALL',
          resourceTags: {
            'Environment': 'staging'
          },
        }
      },
      actions: {
        'ThrottleConnections': {
          actionId: 'aws:rds:reboot-db-instances',
          parameters: {
            'forceFailover': 'true'
          },
          targets: {
            'Clusters': 'DBClusters'
          }
        }
      }
    });
  }
}
```

### Chaos Mesh for Kubernetes

#### Installation

```bash
#!/bin/bash
# scripts/install-chaos-mesh.sh

echo "Installing Chaos Mesh..."

# Add Chaos Mesh Helm repository
helm repo add chaos-mesh https://charts.chaos-mesh.org
helm repo update

# Install Chaos Mesh
helm install chaos-mesh chaos-mesh/chaos-mesh \
  --namespace=chaos-mesh \
  --create-namespace \
  --set chaosDaemon.runtime=containerd \
  --set chaosDaemon.socketPath=/run/containerd/containerd.sock \
  --set dashboard.create=true \
  --set dashboard.securityMode=false

# Wait for Chaos Mesh to be ready
kubectl wait --for=condition=Ready pods --all -n chaos-mesh --timeout=300s

echo "Chaos Mesh installed successfully!"
echo "Access dashboard: kubectl port-forward -n chaos-mesh svc/chaos-dashboard 2333:2333"
```

#### Chaos Experiments

```yaml
# k8s/chaos/pod-failure-experiment.yaml
apiVersion: chaos-mesh.org/v1alpha1
kind: PodChaos
metadata:
  name: pod-failure-experiment
  namespace: default
spec:
  action: pod-failure
  mode: one
  duration: '30s'
  selector:
    namespaces:
      - default
    labelSelectors:
      'app': 'backend-api'
  scheduler:
    cron: '@every 1h'

---
# k8s/chaos/network-delay-experiment.yaml
apiVersion: chaos-mesh.org/v1alpha1
kind: NetworkChaos
metadata:
  name: network-delay-experiment
  namespace: default
spec:
  action: delay
  mode: one
  selector:
    namespaces:
      - default
    labelSelectors:
      'app': 'backend-api'
  delay:
    latency: '500ms'
    correlation: '50'
    jitter: '100ms'
  duration: '5m'
  scheduler:
    cron: '@every 2h'

---
# k8s/chaos/cpu-stress-experiment.yaml
apiVersion: chaos-mesh.org/v1alpha1
kind: StressChaos
metadata:
  name: cpu-stress-experiment
  namespace: default
spec:
  mode: one
  selector:
    namespaces:
      - default
    labelSelectors:
      'app': 'backend-api'
  stressors:
    cpu:
      workers: 2
      load: 80
  duration: '3m'
  scheduler:
    cron: '@every 3h'

---
# k8s/chaos/memory-stress-experiment.yaml
apiVersion: chaos-mesh.org/v1alpha1
kind: StressChaos
metadata:
  name: memory-stress-experiment
  namespace: default
spec:
  mode: one
  selector:
    namespaces:
      - default
    labelSelectors:
      'app': 'backend-api'
  stressors:
    memory:
      workers: 1
      size: '512MB'
  duration: '2m'
  scheduler:
    cron: '@every 4h'
```

## Chaos Experiment Scenarios

### Scenario 1: Pod Failure

**Hypothesis**: System maintains 99.9% availability when random pods fail

**Experiment**:
```bash
#!/bin/bash
# scripts/chaos/pod-failure-experiment.sh

echo "Starting Pod Failure Experiment"
echo "================================"

# 1. Record baseline metrics
echo "Recording baseline metrics..."
./scripts/record-baseline-metrics.sh

# 2. Apply chaos experiment
echo "Terminating random pods..."
kubectl apply -f k8s/chaos/pod-failure-experiment.yaml

# 3. Monitor system behavior
echo "Monitoring system for 5 minutes..."
./scripts/monitor-system-health.sh 300

# 4. Collect results
echo "Collecting experiment results..."
./scripts/collect-chaos-results.sh pod-failure

# 5. Cleanup
echo "Cleaning up experiment..."
kubectl delete -f k8s/chaos/pod-failure-experiment.yaml

# 6. Generate report
echo "Generating experiment report..."
./scripts/generate-chaos-report.sh pod-failure
```

**Expected Results**:
- Kubernetes automatically restarts failed pods
- Traffic is routed to healthy pods
- No user-visible impact
- Recovery time < 30 seconds
- Error rate < 0.1%

**Actual Results** (Example):
```json
{
  "experiment": "pod-failure",
  "duration": "5m",
  "pods_terminated": 2,
  "recovery_time_avg": "18s",
  "error_rate": "0.03%",
  "availability": "99.97%",
  "hypothesis_validated": true,
  "improvements_identified": [
    "Increase pod replicas from 5 to 7 for better redundancy"
  ]
}
```

### Scenario 2: Network Latency

**Hypothesis**: System handles 500ms network latency without timeout errors

**Experiment**:
```bash
#!/bin/bash
# scripts/chaos/network-latency-experiment.sh

echo "Starting Network Latency Experiment"
echo "===================================="

# 1. Record baseline
./scripts/record-baseline-metrics.sh

# 2. Inject network latency
echo "Injecting 500ms network latency..."
kubectl apply -f k8s/chaos/network-delay-experiment.yaml

# 3. Monitor for 5 minutes
./scripts/monitor-system-health.sh 300

# 4. Collect results
./scripts/collect-chaos-results.sh network-latency

# 5. Cleanup
kubectl delete -f k8s/chaos/network-delay-experiment.yaml

# 6. Generate report
./scripts/generate-chaos-report.sh network-latency
```

**Expected Results**:
- Request timeouts properly handled
- Circuit breakers activate
- Graceful degradation
- P95 latency < 3 seconds
- Error rate < 1%

### Scenario 3: Database Failover

**Hypothesis**: System recovers from database failover in < 30 seconds

**Experiment**:
```bash
#!/bin/bash
# scripts/chaos/database-failover-experiment.sh

echo "Starting Database Failover Experiment"
echo "======================================"

# 1. Record baseline
./scripts/record-baseline-metrics.sh

# 2. Trigger Aurora failover
echo "Triggering Aurora failover..."
aws rds failover-db-cluster \
  --db-cluster-identifier genai-demo-staging

# 3. Monitor recovery
echo "Monitoring database recovery..."
./scripts/monitor-database-recovery.sh

# 4. Verify application recovery
echo "Verifying application recovery..."
./scripts/verify-application-health.sh

# 5. Collect results
./scripts/collect-chaos-results.sh database-failover

# 6. Generate report
./scripts/generate-chaos-report.sh database-failover
```

**Expected Results**:
- Aurora fails over to standby
- Application reconnects automatically
- Connection pool refreshes
- Recovery time < 30 seconds
- Zero data loss

### Scenario 4: Region Failure

**Hypothesis**: System fails over to backup region in < 60 seconds

**Experiment**:
```bash
#!/bin/bash
# scripts/chaos/region-failure-experiment.sh

echo "Starting Region Failure Experiment"
echo "==================================="

# 1. Record baseline
./scripts/record-baseline-metrics.sh

# 2. Simulate Taiwan region failure
echo "Simulating Taiwan region failure..."
./scripts/simulate-region-failure.sh taiwan

# 3. Monitor failover
echo "Monitoring automatic failover..."
./scripts/monitor-region-failover.sh

# 4. Verify Japan region handling traffic
echo "Verifying Japan region capacity..."
./scripts/verify-region-capacity.sh japan

# 5. Test critical user journeys
echo "Testing critical user journeys..."
./scripts/test-user-journeys.sh

# 6. Restore Taiwan region
echo "Restoring Taiwan region..."
./scripts/restore-region.sh taiwan

# 7. Collect results
./scripts/collect-chaos-results.sh region-failure

# 8. Generate report
./scripts/generate-chaos-report.sh region-failure
```

**Expected Results**:
- Route 53 detects failure in 60 seconds
- Traffic automatically routes to Japan
- Application-layer routing fails over in 5 seconds
- Zero transaction loss
- RTO < 60 seconds

### Scenario 5: Resource Exhaustion

**Hypothesis**: System handles CPU/memory stress without crashing

**Experiment**:
```bash
#!/bin/bash
# scripts/chaos/resource-exhaustion-experiment.sh

echo "Starting Resource Exhaustion Experiment"
echo "========================================"

# 1. Record baseline
./scripts/record-baseline-metrics.sh

# 2. Apply CPU stress
echo "Applying CPU stress..."
kubectl apply -f k8s/chaos/cpu-stress-experiment.yaml

# 3. Monitor for 3 minutes
./scripts/monitor-system-health.sh 180

# 4. Apply memory stress
echo "Applying memory stress..."
kubectl apply -f k8s/chaos/memory-stress-experiment.yaml

# 5. Monitor for 2 minutes
./scripts/monitor-system-health.sh 120

# 6. Collect results
./scripts/collect-chaos-results.sh resource-exhaustion

# 7. Cleanup
kubectl delete -f k8s/chaos/cpu-stress-experiment.yaml
kubectl delete -f k8s/chaos/memory-stress-experiment.yaml

# 8. Generate report
./scripts/generate-chaos-report.sh resource-exhaustion
```

**Expected Results**:
- HPA scales out additional pods
- Cluster Autoscaler adds nodes if needed
- No pod OOM kills
- Latency increases but stays < 5 seconds
- Error rate < 2%

## Automated Chaos Testing Schedule

### Monthly Schedule

```yaml
# .github/workflows/chaos-engineering.yml
name: Monthly Chaos Engineering Tests

on:
  schedule:
    - cron: '0 2 1 * *'  # 1st of every month at 2 AM
  workflow_dispatch:

jobs:
  pod-failure-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run Pod Failure Experiment
        run: ./scripts/chaos/pod-failure-experiment.sh
      - name: Upload Results
        uses: actions/upload-artifact@v3
        with:
          name: pod-failure-results
          path: reports/chaos/pod-failure-*.json

  network-latency-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run Network Latency Experiment
        run: ./scripts/chaos/network-latency-experiment.sh
      - name: Upload Results
        uses: actions/upload-artifact@v3
        with:
          name: network-latency-results
          path: reports/chaos/network-latency-*.json

  database-failover-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run Database Failover Experiment
        run: ./scripts/chaos/database-failover-experiment.sh
      - name: Upload Results
        uses: actions/upload-artifact@v3
        with:
          name: database-failover-results
          path: reports/chaos/database-failover-*.json

  region-failure-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run Region Failure Experiment
        run: ./scripts/chaos/region-failure-experiment.sh
      - name: Upload Results
        uses: actions/upload-artifact@v3
        with:
          name: region-failure-results
          path: reports/chaos/region-failure-*.json

  generate-report:
    needs: [pod-failure-test, network-latency-test, database-failover-test, region-failure-test]
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Download All Results
        uses: actions/download-artifact@v3
      - name: Generate Comprehensive Report
        run: ./scripts/generate-monthly-chaos-report.sh
      - name: Upload Report
        uses: actions/upload-artifact@v3
        with:
          name: monthly-chaos-report
          path: reports/chaos/monthly-report-*.md
```

## Monitoring and Metrics

### Chaos Engineering Metrics

```yaml
# Prometheus metrics for chaos engineering
chaos_experiments_total:
  type: counter
  labels: [experiment_type, result]
  description: Total number of chaos experiments run

chaos_experiment_duration_seconds:
  type: histogram
  labels: [experiment_type]
  description: Duration of chaos experiments

chaos_hypothesis_validated:
  type: gauge
  labels: [experiment_type]
  description: Whether hypothesis was validated (1=yes, 0=no)

chaos_improvements_identified:
  type: counter
  labels: [experiment_type, improvement_category]
  description: Number of improvements identified
```

### Grafana Dashboard

**Chaos Engineering Overview**:
- Experiment execution timeline
- Hypothesis validation rate
- System behavior during experiments
- Improvements identified and implemented
- Monthly chaos testing results

## Safety Measures

### Stop Conditions

```typescript
// Automatic experiment termination conditions
const stopConditions = [
  {
    name: 'High Error Rate',
    metric: 'error_rate',
    threshold: 5.0,  // 5% error rate
    action: 'STOP_EXPERIMENT'
  },
  {
    name: 'Low Availability',
    metric: 'availability',
    threshold: 99.0,  // Below 99% availability
    action: 'STOP_EXPERIMENT'
  },
  {
    name: 'High Latency',
    metric: 'p95_latency',
    threshold: 5000,  // 5 seconds
    action: 'STOP_EXPERIMENT'
  }
];
```

### Blast Radius Limitation

1. **Environment Isolation**: Run experiments in staging first
2. **Gradual Rollout**: Start with 10% of pods, increase gradually
3. **Time Limits**: Maximum experiment duration: 10 minutes
4. **Automatic Rollback**: Immediate rollback on stop condition
5. **Business Hours**: Avoid experiments during peak traffic

## Continuous Improvement

### Post-Experiment Process

1. **Analyze Results**: Review experiment outcomes
2. **Identify Weaknesses**: Document system weaknesses found
3. **Create Action Items**: Generate improvement tasks
4. **Implement Fixes**: Address identified issues
5. **Re-test**: Validate fixes with follow-up experiments
6. **Document Learnings**: Update runbooks and procedures

### Example Improvements

**From Pod Failure Experiments**:
- Increased pod replicas from 5 to 7
- Reduced readiness probe failure threshold from 5 to 3
- Implemented pod disruption budgets

**From Network Latency Experiments**:
- Increased connection timeouts from 5s to 10s
- Implemented circuit breakers with 3-failure threshold
- Added retry logic with exponential backoff

**From Database Failover Experiments**:
- Reduced connection pool max lifetime from 30m to 15m
- Implemented connection validation on borrow
- Added automatic connection pool refresh on failover

## Best Practices

1. **Start Small**: Begin with low-impact experiments
2. **Automate**: Make chaos engineering continuous
3. **Document**: Record all experiments and results
4. **Learn**: Use findings to improve system resilience
5. **Communicate**: Share results with entire team


**Next Steps**:
1. Install Chaos Mesh in staging environment
2. Run initial pod failure experiment
3. Schedule monthly automated chaos tests
4. Review and implement improvements from experiments
