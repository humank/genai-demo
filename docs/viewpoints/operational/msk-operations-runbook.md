# MSK Operations Runbook

**Document Version**: 2.0  
**Last Updated**: September 24, 2025 10:20 PM (Taipei Time)  
**Responsible Team**: Operations Team + SRE Team

## 📋 Overview

This operations runbook provides a complete operational guide for MSK (Amazon Managed Streaming for Apache Kafka) clusters, including incident response procedures, escalation paths, monitoring procedures, capacity planning, and troubleshooting guides.

## 🚨 Incident Response Procedures

### Emergency Incident Classification

#### P0 - Emergency 🔴
**Impact**: Complete service unavailability, affecting all users  
**Response Time**: Immediate (< 5 minutes)  
**Notification Method**: Phone + SMS + PagerDuty

**Trigger Conditions**:
- MSK cluster completely offline
- All partitions offline (OfflinePartitionsCount > 0)
- No active controller (ActiveControllerCount = 0)
- Data loss events

**Response Procedure**:
```bash
# 1. Immediately assess impact scope
kubectl get pods -n kafka-system
aws kafka describe-cluster --cluster-arn $MSK_CLUSTER_ARN

# 2. Check cluster status
aws kafka list-clusters --query 'ClusterInfoList[0].State'

# 3. If cluster status is abnormal, initiate disaster recovery
./scripts/disaster-recovery/initiate-failover.sh

# 4. Notify stakeholders
./scripts/notifications/send-emergency-alert.sh "MSK P0 Event"
```

#### P1 - Critical 🟠
**Impact**: Limited service functionality, some users affected  
**Response Time**: Within 15 minutes  
**Notification Method**: PagerDuty + Slack

**Trigger Conditions**:
- Consumer lag > 5 minutes (EstimatedMaxTimeLag > 300000ms)
- Under-replicated partitions > 0 (UnderReplicatedPartitions > 0)
- Producer error rate > 1%
- Broker CPU > 90%

**Response Procedure**:
```bash
# 1. Check consumer lag
aws kafka describe-cluster --cluster-arn $MSK_CLUSTER_ARN \
  --query 'ClusterInfo.CurrentBrokerSoftwareInfo'

# 2. Analyze consumer group status
kafka-consumer-groups.sh --bootstrap-server $BOOTSTRAP_SERVERS \
  --describe --all-groups

# 3. Check partition distribution
kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERVERS \
  --describe --under-replicated-partitions

# 4. If needed, trigger auto-scaling
./scripts/scaling/auto-scale-consumers.sh
```

#### P2 - Warning 🟡
**Impact**: Performance degradation, no user impact  
**Response Time**: Within 1 hour  
**Notification Method**: Slack + Email

**Trigger Conditions**:
- Consumer lag > 1 minute
- Broker CPU > 70%
- Disk usage > 80%
- Producer error rate > 0.1%

### Escalation Paths and Procedures

#### Incident Escalation Matrix
```
Time Elapsed    P0 → P1 → P2
15 minutes      Auto-escalate to P1
30 minutes      Auto-escalate to P0
1 hour          Escalate to management
2 hours         CEO/CTO notification
```

#### Escalation Contacts
```yaml
Primary On-Call:
  - SRE Team Lead: +886-xxx-xxx-xxx
  - Platform Engineer: +886-xxx-xxx-xxx

Secondary On-Call:
  - Engineering Manager: +886-xxx-xxx-xxx
  - Architecture Lead: +886-xxx-xxx-xxx

Executive Escalation:
  - VP Engineering: +886-xxx-xxx-xxx
  - CTO: +886-xxx-xxx-xxx
```

## 📊 Monitoring Procedures

### Monitoring Dashboard Checklist

#### Daily Checks (Daily Health Check)
**Execution Time**: Daily at 9:00 AM  
**Responsible**: On-duty SRE

```bash
#!/bin/bash
# daily-msk-health-check.sh

echo "=== MSK Daily Health Check $(date) ==="

# 1. Overall cluster health
echo "1. Cluster Health:"
aws kafka describe-cluster --cluster-arn $MSK_CLUSTER_ARN \
  --query 'ClusterInfo.State'

# 2. Broker status
echo "2. Broker Status:"
aws kafka list-nodes --cluster-arn $MSK_CLUSTER_ARN \
  --query 'NodeInfoList[*].[NodeARN,BrokerNodeInfo.BrokerId,BrokerNodeInfo.ClientSubnet]'

# 3. Topic health check
echo "3. Topic Health:"
kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERVERS --list | wc -l
echo "Total topics count"

# 4. Consumer group status
echo "4. Consumer Groups:"
kafka-consumer-groups.sh --bootstrap-server $BOOTSTRAP_SERVERS \
  --list | wc -l
echo "Active consumer groups"

# 5. Performance metrics check
echo "5. Performance Metrics:"
curl -s "http://localhost:8080/actuator/msk-health" | jq '.overall_healthy'

# 6. Generate report
echo "Daily health check completed at $(date)" >> /var/log/msk-health.log
```

#### Weekly Checks (Weekly Review)
**Execution Time**: Weekly Monday at 10:00 AM  
**Responsible**: Platform Team

```bash
#!/bin/bash
# weekly-msk-review.sh

echo "=== MSK Weekly Review $(date) ==="

# 1. Capacity trend analysis
echo "1. Capacity Trends:"
aws cloudwatch get-metric-statistics \
  --namespace AWS/Kafka \
  --metric-name KafkaDataLogsDiskUsed \
  --start-time $(date -d '7 days ago' -u +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 86400 \
  --statistics Average

# 2. Performance trends
echo "2. Performance Trends:"
aws cloudwatch get-metric-statistics \
  --namespace AWS/Kafka \
  --metric-name EstimatedMaxTimeLag \
  --start-time $(date -d '7 days ago' -u +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 3600 \
  --statistics Maximum

# 3. Error rate analysis
echo "3. Error Rate Analysis:"
aws cloudwatch get-metric-statistics \
  --namespace AWS/Kafka \
  --metric-name ProducerRequestErrors \
  --start-time $(date -d '7 days ago' -u +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 3600 \
  --statistics Sum

# 4. Cost analysis
echo "4. Cost Analysis:"
aws ce get-cost-and-usage \
  --time-period Start=$(date -d '7 days ago' +%Y-%m-%d),End=$(date +%Y-%m-%d) \
  --granularity DAILY \
  --metrics BlendedCost \
  --group-by Type=DIMENSION,Key=SERVICE \
  --filter file://msk-cost-filter.json
```

### Alert Threshold Configuration

#### Critical Metric Thresholds
```yaml
Critical Thresholds:
  offline_partitions: 0          # Any offline partition is an emergency
  under_replicated_partitions: 0 # Any under-replicated partition is critical
  consumer_lag_ms: 300000        # 5-minute lag is critical
  broker_cpu_percent: 90         # 90% CPU is critical
  broker_memory_percent: 90      # 90% memory is critical
  disk_usage_percent: 85         # 85% disk is critical

Warning Thresholds:
  consumer_lag_ms: 60000         # 1-minute lag is warning
  broker_cpu_percent: 70         # 70% CPU is warning
  broker_memory_percent: 80      # 80% memory is warning
  disk_usage_percent: 80         # 80% disk is warning
  producer_error_rate: 0.001     # 0.1% error rate is warning
  network_io_percent: 60         # 60% network I/O is warning
```

#### Alert Response Actions
```yaml
Alert Actions:
  Critical:
    - SNS Topic: msk-critical-alerts
    - PagerDuty: High Priority
    - Slack: #ops-critical
    - Auto-scaling: Enabled
    
  Warning:
    - SNS Topic: msk-warning-alerts
    - Slack: #ops-monitoring
    - Email: ops-team@company.com
    - Auto-remediation: Enabled
```

## 📈 Capacity Planning Guide

### Capacity Monitoring Metrics

#### 1. Storage Capacity Planning
```bash
#!/bin/bash
# storage-capacity-planning.sh

# Current storage usage
CURRENT_USAGE=$(aws cloudwatch get-metric-statistics \
  --namespace AWS/Kafka \
  --metric-name KafkaDataLogsDiskUsed \
  --start-time $(date -d '1 hour ago' -u +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 3600 \
  --statistics Average \
  --query 'Datapoints[0].Average')

echo "Current storage usage: ${CURRENT_USAGE}%"

# Predict future 30-day usage
if (( $(echo "$CURRENT_USAGE > 70" | bc -l) )); then
  echo "WARNING: Storage usage > 70%, consider scaling up"
  
  # Calculate estimated time to full capacity
  GROWTH_RATE=$(calculate_growth_rate.py --days 30)
  DAYS_TO_FULL=$(echo "scale=0; (100 - $CURRENT_USAGE) / $GROWTH_RATE" | bc)
  
  echo "Estimated days to full capacity: $DAYS_TO_FULL"
  
  if (( DAYS_TO_FULL < 30 )); then
    echo "CRITICAL: Need to scale storage within 30 days"
    ./scripts/scaling/request-storage-scaling.sh
  fi
fi
```

#### 2. Capacity Planning Calculations
```python
#!/usr/bin/env python3
# capacity-planning.py

import boto3
from datetime import datetime, timedelta
import numpy as np

def calculate_capacity_requirements():
    cloudwatch = boto3.client('cloudwatch')
    
    # Get metrics for the past 30 days
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(days=30)
    
    metrics = {
        'throughput': get_metric_data('MessagesInPerSec', start_time, end_time),
        'storage': get_metric_data('KafkaDataLogsDiskUsed', start_time, end_time),
        'cpu': get_metric_data('CpuUser', start_time, end_time),
        'memory': get_metric_data('MemoryUsed', start_time, end_time)
    }
    
    # Calculate trends and predictions
    predictions = {}
    for metric_name, data in metrics.items():
        trend = calculate_trend(data)
        prediction = predict_future_usage(data, days=90)
        predictions[metric_name] = {
            'current': data[-1] if data else 0,
            'trend': trend,
            'predicted_90_days': prediction
        }
    
    # Generate capacity recommendations
    recommendations = generate_capacity_recommendations(predictions)
    
    return {
        'predictions': predictions,
        'recommendations': recommendations,
        'timestamp': datetime.utcnow().isoformat()
    }

def generate_capacity_recommendations(predictions):
    recommendations = []
    
    # Storage recommendations
    if predictions['storage']['predicted_90_days'] > 80:
        recommendations.append({
            'type': 'storage_scaling',
            'priority': 'high',
            'action': 'Increase broker storage by 50%',
            'timeline': '30 days'
        })
    
    # CPU recommendations
    if predictions['cpu']['predicted_90_days'] > 70:
        recommendations.append({
            'type': 'compute_scaling',
            'priority': 'medium',
            'action': 'Upgrade to larger instance type',
            'timeline': '60 days'
        })
    
    # Throughput recommendations
    if predictions['throughput']['trend'] > 0.1:  # 10% growth
        recommendations.append({
            'type': 'partition_scaling',
            'priority': 'medium',
            'action': 'Increase partition count for high-traffic topics',
            'timeline': '45 days'
        })
    
    return recommendations

if __name__ == "__main__":
    results = calculate_capacity_requirements()
    print(json.dumps(results, indent=2))
```

### Scaling Triggers

#### Auto-scaling Configuration
```yaml
Auto Scaling Triggers:
  Broker Scaling:
    scale_up_cpu_threshold: 70%
    scale_up_memory_threshold: 80%
    scale_up_network_threshold: 60%
    cooldown_period: 300s
    
  Storage Scaling:
    scale_up_disk_threshold: 80%
    scale_increment: 100GB
    max_storage_per_broker: 1TB
    
  Partition Scaling:
    scale_up_lag_threshold: 10000
    scale_up_throughput_threshold: 80%
    partition_increment: 4
```

#### Manual Scaling Procedures
```bash
#!/bin/bash
# manual-scaling-procedure.sh

function scale_brokers() {
  local target_count=$1
  
  echo "Scaling MSK cluster to $target_count brokers..."
  
  # 1. Update CDK configuration
  sed -i "s/numberOfBrokerNodes: [0-9]*/numberOfBrokerNodes: $target_count/" \
    infrastructure/src/stacks/msk-stack.ts
  
  # 2. Deploy update
  cd infrastructure
  cdk deploy MSKStack --require-approval never
  
  # 3. Wait for scaling completion
  aws kafka describe-cluster --cluster-arn $MSK_CLUSTER_ARN \
    --query 'ClusterInfo.NumberOfBrokerNodes'
  
  # 4. Verify cluster health
  ./scripts/health-check/verify-cluster-health.sh
  
  echo "Broker scaling completed"
}

function scale_storage() {
  local new_size_gb=$1
  
  echo "Scaling storage to ${new_size_gb}GB per broker..."
  
  # 1. Create scaling request
  aws kafka update-broker-storage \
    --cluster-arn $MSK_CLUSTER_ARN \
    --target-broker-ebs-volume-info VolumeSize=$new_size_gb
  
  # 2. Monitor scaling progress
  while true; do
    status=$(aws kafka describe-cluster --cluster-arn $MSK_CLUSTER_ARN \
      --query 'ClusterInfo.State' --output text)
    
    if [ "$status" = "ACTIVE" ]; then
      echo "Storage scaling completed"
      break
    fi
    
    echo "Scaling in progress... Status: $status"
    sleep 30
  done
}
```

## 🔧 Troubleshooting Guide

### Common Issue Diagnosis

#### 1. Consumer Lag Issues
```bash
#!/bin/bash
# diagnose-consumer-lag.sh

echo "=== Consumer Lag Diagnosis ==="

# Check all consumer groups
echo "1. Consumer Groups Overview:"
kafka-consumer-groups.sh --bootstrap-server $BOOTSTRAP_SERVERS \
  --describe --all-groups | grep -E "(GROUP|LAG)"

# Identify most lagging groups
echo "2. Top Lagging Consumer Groups:"
kafka-consumer-groups.sh --bootstrap-server $BOOTSTRAP_SERVERS \
  --describe --all-groups | sort -k5 -nr | head -10

# Check specific group details
read -p "Enter consumer group to analyze: " GROUP_ID
echo "3. Detailed Analysis for $GROUP_ID:"

kafka-consumer-groups.sh --bootstrap-server $BOOTSTRAP_SERVERS \
  --describe --group $GROUP_ID

# Check consumer instances
echo "4. Consumer Instances:"
kafka-consumer-groups.sh --bootstrap-server $BOOTSTRAP_SERVERS \
  --describe --group $GROUP_ID --members

# Analyze partition distribution
echo "5. Partition Distribution:"
kafka-consumer-groups.sh --bootstrap-server $BOOTSTRAP_SERVERS \
  --describe --group $GROUP_ID --members --verbose

# Suggest solutions
echo "6. Recommendations:"
LAG=$(kafka-consumer-groups.sh --bootstrap-server $BOOTSTRAP_SERVERS \
  --describe --group $GROUP_ID | awk '{sum += $5} END {print sum}')

if (( LAG > 10000 )); then
  echo "- HIGH LAG DETECTED: Consider scaling consumers"
  echo "- Check consumer processing logic for bottlenecks"
  echo "- Verify network connectivity and DNS resolution"
fi
```

#### 2. Producer Performance Issues
```bash
#!/bin/bash
# diagnose-producer-performance.sh

echo "=== Producer Performance Diagnosis ==="

# Check producer metrics
echo "1. Producer Metrics:"
curl -s "http://localhost:8080/actuator/msk-metrics" | \
  jq '.throughput'

# Check batch configuration
echo "2. Producer Configuration:"
curl -s "http://localhost:8080/actuator/configprops" | \
  jq '.kafka.producer'

# Analyze error patterns
echo "3. Error Analysis:"
curl -s "http://localhost:8080/actuator/msk-errors" | \
  jq '.error_stats'

# Check network latency
echo "4. Network Latency Test:"
for broker in $(echo $BOOTSTRAP_SERVERS | tr ',' ' '); do
  echo "Testing $broker:"
  nc -zv ${broker%:*} ${broker#*:}
done

# Suggest optimizations
echo "5. Optimization Recommendations:"
echo "- Check batch.size and linger.ms configuration"
echo "- Verify compression.type setting"
echo "- Monitor buffer.memory usage"
echo "- Check for DNS resolution issues"
```

#### 3. Partition Imbalance Issues
```bash
#!/bin/bash
# diagnose-partition-imbalance.sh

echo "=== Partition Imbalance Diagnosis ==="

# Check topic partition distribution
echo "1. Topic Partition Distribution:"
kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERVERS \
  --describe | grep -E "(Topic:|Leader:)" | \
  awk '/Leader:/ {print $2, $4}' | sort | uniq -c

# Check broker load distribution
echo "2. Broker Load Distribution:"
kafka-log-dirs.sh --bootstrap-server $BOOTSTRAP_SERVERS \
  --describe --json | jq -r '.brokers[].logDirs[].partitions | length'

# Analyze partition sizes
echo "3. Partition Size Analysis:"
for topic in $(kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERVERS --list); do
  echo "Topic: $topic"
  kafka-log-dirs.sh --bootstrap-server $BOOTSTRAP_SERVERS \
    --topic-list $topic --describe --json | \
    jq -r '.brokers[].logDirs[].partitions[] | "\(.partition): \(.size) bytes"'
done

# Rebalancing recommendations
echo "4. Rebalancing Recommendations:"
echo "- Use kafka-reassign-partitions.sh for manual rebalancing"
echo "- Consider using Cruise Control for automated rebalancing"
echo "- Monitor rebalancing impact on performance"
```

### Performance Tuning Guide

#### 1. Broker Tuning
```bash
#!/bin/bash
# broker-tuning.sh

echo "=== Broker Performance Tuning ==="

# JVM tuning recommendations
echo "1. JVM Tuning Recommendations:"
cat << EOF
# Kafka Broker JVM Settings
export KAFKA_HEAP_OPTS="-Xmx6g -Xms6g"
export KAFKA_JVM_PERFORMANCE_OPTS="-server -XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:InitiatingHeapOccupancyPercent=35 -XX:+ExplicitGCInvokesConcurrent -Djava.awt.headless=true"
EOF

# OS tuning recommendations
echo "2. OS Tuning Recommendations:"
cat << EOF
# File descriptor limits
echo "* soft nofile 100000" >> /etc/security/limits.conf
echo "* hard nofile 100000" >> /etc/security/limits.conf

# VM settings
echo "vm.swappiness=1" >> /etc/sysctl.conf
echo "vm.dirty_background_ratio=5" >> /etc/sysctl.conf
echo "vm.dirty_ratio=60" >> /etc/sysctl.conf
echo "vm.dirty_expire_centisecs=12000" >> /etc/sysctl.conf
EOF

# Network tuning
echo "3. Network Tuning:"
cat << EOF
# TCP settings
echo "net.core.wmem_default = 131072" >> /etc/sysctl.conf
echo "net.core.rmem_default = 131072" >> /etc/sysctl.conf
echo "net.core.rmem_max = 16777216" >> /etc/sysctl.conf
echo "net.core.wmem_max = 16777216" >> /etc/sysctl.conf
EOF
```

#### 2. Topic Configuration Tuning
```bash
#!/bin/bash
# topic-tuning.sh

function optimize_topic() {
  local topic=$1
  local throughput_requirement=$2
  
  echo "Optimizing topic: $topic for $throughput_requirement msgs/sec"
  
  # Calculate recommended partition count
  local partitions=$(( throughput_requirement / 1000 + 1 ))
  if (( partitions < 3 )); then
    partitions=3
  fi
  
  # Update topic configuration
  kafka-configs.sh --bootstrap-server $BOOTSTRAP_SERVERS \
    --entity-type topics --entity-name $topic --alter \
    --add-config "segment.ms=604800000,retention.ms=604800000,compression.type=gzip"
  
  # Increase partitions (if needed)
  current_partitions=$(kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERVERS \
    --describe --topic $topic | grep "PartitionCount" | awk '{print $4}')
  
  if (( partitions > current_partitions )); then
    kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERVERS \
      --alter --topic $topic --partitions $partitions
    echo "Increased partitions from $current_partitions to $partitions"
  fi
}

# Batch optimize all business topics
for topic in $(kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERVERS \
  --list | grep "business-events"); do
  optimize_topic $topic 5000
done
```

## 🔄 Backup and Disaster Recovery

### Backup Strategy

#### 1. Automated Backup Configuration
```yaml
Backup Configuration:
  type: "cross_region_replication"
  primary_region: "ap-northeast-1"
  backup_region: "ap-southeast-1"
  
  replication_settings:
    topics: "all_business_events"
    consumer_group_offsets: true
    acls: true
    
  retention:
    operational_backup: "7_days"
    compliance_backup: "90_days"
    disaster_recovery: "continuous"
```

#### 2. Backup Verification Procedures
```bash
#!/bin/bash
# backup-verification.sh

echo "=== Backup Verification $(date) ==="

# 1. Check cross-region replication status
echo "1. Cross-Region Replication Status:"
aws kafka describe-replication \
  --replication-arn $REPLICATION_ARN \
  --query 'ReplicationInfo.ReplicationState'

# 2. Verify topic synchronization
echo "2. Topic Synchronization:"
PRIMARY_TOPICS=$(kafka-topics.sh --bootstrap-server $PRIMARY_BOOTSTRAP \
  --list | sort)
BACKUP_TOPICS=$(kafka-topics.sh --bootstrap-server $BACKUP_BOOTSTRAP \
  --list | sort)

diff <(echo "$PRIMARY_TOPICS") <(echo "$BACKUP_TOPICS")

# 3. Check consumer group offsets
echo "3. Consumer Group Offsets:"
for group in $(kafka-consumer-groups.sh --bootstrap-server $PRIMARY_BOOTSTRAP \
  --list); do
  
  PRIMARY_OFFSET=$(kafka-consumer-groups.sh \
    --bootstrap-server $PRIMARY_BOOTSTRAP \
    --describe --group $group | tail -n +3 | awk '{sum += $3} END {print sum}')
  
  BACKUP_OFFSET=$(kafka-consumer-groups.sh \
    --bootstrap-server $BACKUP_BOOTSTRAP \
    --describe --group $group | tail -n +3 | awk '{sum += $3} END {print sum}')
  
  LAG=$((PRIMARY_OFFSET - BACKUP_OFFSET))
  echo "Group $group: Primary=$PRIMARY_OFFSET, Backup=$BACKUP_OFFSET, Lag=$LAG"
  
  if (( LAG > 1000 )); then
    echo "WARNING: High replication lag for group $group"
  fi
done
```

### Disaster Recovery Procedures

#### RTO/RPO Targets
```yaml
Recovery Objectives:
  RTO: "< 5 minutes"    # Recovery Time Objective
  RPO: "< 1 minute"     # Recovery Point Objective
  
Service Level Targets:
  availability: "99.9%"
  data_loss_tolerance: "0%"
  max_downtime_per_month: "43.2 minutes"
```

#### Disaster Recovery Execution Procedures
```bash
#!/bin/bash
# disaster-recovery-execution.sh

function execute_disaster_recovery() {
  local scenario=$1  # "primary_failure" | "region_failure" | "complete_failure"
  
  echo "=== Executing Disaster Recovery: $scenario ==="
  
  case $scenario in
    "primary_failure")
      execute_primary_cluster_failover
      ;;
    "region_failure")
      execute_cross_region_failover
      ;;
    "complete_failure")
      execute_complete_rebuild
      ;;
    *)
      echo "Unknown disaster scenario: $scenario"
      exit 1
      ;;
  esac
}

function execute_primary_cluster_failover() {
  echo "1. Stopping primary cluster traffic..."
  
  # Update DNS records to point to backup cluster
  aws route53 change-resource-record-sets \
    --hosted-zone-id $HOSTED_ZONE_ID \
    --change-batch file://failover-dns-change.json
  
  # Update application configuration
  kubectl patch configmap kafka-config \
    --patch '{"data":{"bootstrap.servers":"'$BACKUP_BOOTSTRAP_SERVERS'"}}'
  
  # Restart application pods
  kubectl rollout restart deployment/genai-demo-app
  
  echo "2. Verifying failover..."
  sleep 30
  
  # Verify application connected to backup cluster
  kubectl logs -l app=genai-demo-app | grep "Connected to backup cluster"
  
  echo "Primary cluster failover completed"
}

function execute_cross_region_failover() {
  echo "1. Activating backup region..."
  
  # Deploy complete infrastructure in backup region
  cd infrastructure
  AWS_REGION=$BACKUP_REGION cdk deploy --all
  
  # Update global load balancer
  aws globalaccelerator update-listener \
    --listener-arn $LISTENER_ARN \
    --port-ranges FromPort=443,ToPort=443,Protocol=TCP
  
  echo "2. Migrating traffic..."
  
  # Gradually shift traffic to backup region
  for weight in 25 50 75 100; do
    aws route53 change-resource-record-sets \
      --hosted-zone-id $HOSTED_ZONE_ID \
      --change-batch file://traffic-shift-${weight}.json
    
    echo "Traffic shifted to ${weight}% backup region"
    sleep 60
    
    # Monitor error rate
    error_rate=$(get_error_rate.sh)
    if (( $(echo "$error_rate > 0.01" | bc -l) )); then
      echo "High error rate detected, rolling back..."
      rollback_traffic_shift.sh
      exit 1
    fi
  done
  
  echo "Cross-region failover completed"
}
```

## 📋 Maintenance Procedures

### Regular Maintenance Tasks

#### Daily Maintenance
```bash
#!/bin/bash
# daily-maintenance.sh

echo "=== Daily MSK Maintenance $(date) ==="

# 1. Health check
./scripts/health-check/daily-health-check.sh

# 2. Log rotation
find /var/log/kafka -name "*.log" -mtime +7 -delete

# 3. Metrics collection
./scripts/monitoring/collect-daily-metrics.sh

# 4. Backup verification
./scripts/backup/verify-backup-status.sh

# 5. Capacity check
./scripts/capacity/check-capacity-usage.sh

echo "Daily maintenance completed"
```

#### Weekly Maintenance
```bash
#!/bin/bash
# weekly-maintenance.sh

echo "=== Weekly MSK Maintenance $(date) ==="

# 1. Performance analysis
./scripts/performance/weekly-performance-analysis.sh

# 2. Capacity planning
./scripts/capacity/weekly-capacity-planning.sh

# 3. Security scan
./scripts/security/weekly-security-scan.sh

# 4. Configuration audit
./scripts/audit/weekly-config-audit.sh

# 5. Documentation update
./scripts/documentation/update-runbook.sh

echo "Weekly maintenance completed"
```

#### Monthly Maintenance
```bash
#!/bin/bash
# monthly-maintenance.sh

echo "=== Monthly MSK Maintenance $(date) ==="

# 1. Disaster recovery test
./scripts/dr/monthly-dr-test.sh

# 2. Performance benchmark
./scripts/performance/monthly-benchmark.sh

# 3. Cost optimization analysis
./scripts/cost/monthly-cost-analysis.sh

# 4. Security compliance check
./scripts/compliance/monthly-compliance-check.sh

# 5. Architecture review
./scripts/architecture/monthly-architecture-review.sh

echo "Monthly maintenance completed"
```

### Maintenance Window Management

#### Maintenance Window Schedule
```yaml
Maintenance Windows:
  daily:
    time: "02:00-04:00 UTC"
    duration: "2 hours"
    impact: "minimal"
    
  weekly:
    time: "Sunday 01:00-05:00 UTC"
    duration: "4 hours"
    impact: "low"
    
  monthly:
    time: "First Sunday 00:00-06:00 UTC"
    duration: "6 hours"
    impact: "medium"
    
  emergency:
    time: "as_needed"
    duration: "variable"
    impact: "high"
```

#### Maintenance Notification Procedures
```bash
#!/bin/bash
# maintenance-notification.sh

function send_maintenance_notification() {
  local type=$1      # "scheduled" | "emergency"
  local start_time=$2
  local duration=$3
  local impact=$4
  
  # Prepare notification content
  cat << EOF > maintenance-notice.json
{
  "type": "$type",
  "start_time": "$start_time",
  "duration": "$duration",
  "impact": "$impact",
  "services_affected": ["MSK Cluster", "Event Processing", "Real-time Analytics"],
  "contact": "ops-team@company.com"
}
EOF

  # Send notifications
  case $type in
    "scheduled")
      # Notify 24 hours in advance
      aws sns publish \
        --topic-arn $MAINTENANCE_TOPIC_ARN \
        --message file://maintenance-notice.json \
        --subject "Scheduled MSK Maintenance - $start_time"
      ;;
    "emergency")
      # Immediate notification
      aws sns publish \
        --topic-arn $EMERGENCY_TOPIC_ARN \
        --message file://maintenance-notice.json \
        --subject "Emergency MSK Maintenance - Starting Now"
      ;;
  esac
}
```

## 📞 Contact Information and Escalation Paths

### Team Contact Information
```yaml
Primary Contacts:
  SRE Team Lead:
    name: "John Smith"
    phone: "+886-912-345-678"
    email: "sre-lead@company.com"
    slack: "@sre-lead"
    
  Platform Engineer:
    name: "Jane Doe"
    phone: "+886-987-654-321"
    email: "platform-eng@company.com"
    slack: "@platform-eng"

Secondary Contacts:
  Engineering Manager:
    name: "Mike Johnson"
    phone: "+886-955-123-456"
    email: "eng-manager@company.com"
    
  Architecture Lead:
    name: "Sarah Wilson"
    phone: "+886-933-789-012"
    email: "arch-lead@company.com"

Executive Escalation:
  VP Engineering:
    name: "Director Lin"
    phone: "+886-911-111-111"
    email: "vp-eng@company.com"
    
  CTO:
    name: "CTO Huang"
    phone: "+886-922-222-222"
    email: "cto@company.com"
```

### External Vendor Contacts
```yaml
AWS Support:
  support_level: "Enterprise"
  case_priority: "Critical"
  phone: "+1-206-266-4064"
  web: "https://console.aws.amazon.com/support/"
  
Kafka Consulting:
  vendor: "Confluent Professional Services"
  contact: "support@confluent.io"
  phone: "+1-855-899-0121"
  
Monitoring Vendor:
  vendor: "Datadog"
  contact: "support@datadoghq.com"
  phone: "+1-866-329-4466"
```

---

**Document Maintenance**: This operations runbook is updated monthly  
**Next Review**: October 24, 2025  
**Emergency Contact**: ops-team@company.com | +886-911-MSK-OPS (911-675-677)