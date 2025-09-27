# MSK Operations Runbook

**文檔版本**: 2.0  
**最後更新**: 2025年9月24日 下午10:20 (台北時間)  
**負責團隊**: 運營團隊 + SRE 團隊

## 📋 概述

本運營手冊提供 MSK (Amazon Managed Streaming for Apache Kafka) 集群的完整運營指南，包括事件響應程序、升級路徑、監控程序、容量規劃和故障排除指南。

## 🚨 事件響應程序

### 緊急事件分級

#### P0 - 緊急 (Emergency) 🔴
**影響**: 服務完全不可用，影響所有用戶  
**響應時間**: 立即 (< 5 分鐘)  
**通知方式**: 電話 + SMS + PagerDuty

**觸發條件**:
- MSK 集群完全離線
- 所有分區離線 (OfflinePartitionsCount > 0)
- 無活躍控制器 (ActiveControllerCount = 0)
- 資料遺失事件

**響應程序**:
```bash
# 1. 立即評估影響範圍
kubectl get pods -n kafka-system
aws kafka describe-cluster --cluster-arn $MSK_CLUSTER_ARN

# 2. 檢查集群狀態
aws kafka list-clusters --query 'ClusterInfoList[0].State'

# 3. 如果集群狀態異常，啟動災難恢復
./scripts/disaster-recovery/initiate-failover.sh

# 4. 通知利害關係人
./scripts/notifications/send-emergency-alert.sh "MSK P0 Event"
```

#### P1 - 嚴重 (Critical) 🟠
**影響**: 服務功能受限，部分用戶受影響  
**響應時間**: 15 分鐘內  
**通知方式**: PagerDuty + Slack

**觸發條件**:
- 消費者延遲 > 5 分鐘 (EstimatedMaxTimeLag > 300000ms)
- 未複製分區 > 0 (UnderReplicatedPartitions > 0)
- 生產者錯誤率 > 1%
- Broker CPU > 90%

**響應程序**:
```bash
# 1. 檢查消費者延遲
aws kafka describe-cluster --cluster-arn $MSK_CLUSTER_ARN \
  --query 'ClusterInfo.CurrentBrokerSoftwareInfo'

# 2. 分析消費者群組狀態
kafka-consumer-groups.sh --bootstrap-server $BOOTSTRAP_SERVERS \
  --describe --all-groups

# 3. 檢查分區分佈
kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERVERS \
  --describe --under-replicated-partitions

# 4. 如需要，觸發自動擴展
./scripts/scaling/auto-scale-consumers.sh
```

#### P2 - 警告 (Warning) 🟡
**影響**: 效能下降，無用戶影響  
**響應時間**: 1 小時內  
**通知方式**: Slack + Email

**觸發條件**:
- 消費者延遲 > 1 分鐘
- Broker CPU > 70%
- 磁碟使用率 > 80%
- 生產者錯誤率 > 0.1%

### 升級路徑和程序

#### 事件升級矩陣
```
時間經過    P0 → P1 → P2
15 分鐘     自動升級到 P1
30 分鐘     自動升級到 P0
1 小時      升級到管理層
2 小時      CEO/CTO 通知
```

#### 升級聯絡人
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

## 📊 監控程序

### 監控儀表板檢查清單

#### 每日檢查 (Daily Health Check)
**執行時間**: 每日上午 9:00  
**負責人**: 值班 SRE

```bash
#!/bin/bash
# daily-msk-health-check.sh

echo "=== MSK Daily Health Check $(date) ==="

# 1. 集群整體健康
echo "1. Cluster Health:"
aws kafka describe-cluster --cluster-arn $MSK_CLUSTER_ARN \
  --query 'ClusterInfo.State'

# 2. Broker 狀態
echo "2. Broker Status:"
aws kafka list-nodes --cluster-arn $MSK_CLUSTER_ARN \
  --query 'NodeInfoList[*].[NodeARN,BrokerNodeInfo.BrokerId,BrokerNodeInfo.ClientSubnet]'

# 3. 主題健康檢查
echo "3. Topic Health:"
kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERVERS --list | wc -l
echo "Total topics count"

# 4. 消費者群組狀態
echo "4. Consumer Groups:"
kafka-consumer-groups.sh --bootstrap-server $BOOTSTRAP_SERVERS \
  --list | wc -l
echo "Active consumer groups"

# 5. 效能指標檢查
echo "5. Performance Metrics:"
curl -s "http://localhost:8080/actuator/msk-health" | jq '.overall_healthy'

# 6. 生成報告
echo "Daily health check completed at $(date)" >> /var/log/msk-health.log
```

#### 每週檢查 (Weekly Review)
**執行時間**: 每週一上午 10:00  
**負責人**: 平台團隊

```bash
#!/bin/bash
# weekly-msk-review.sh

echo "=== MSK Weekly Review $(date) ==="

# 1. 容量趨勢分析
echo "1. Capacity Trends:"
aws cloudwatch get-metric-statistics \
  --namespace AWS/Kafka \
  --metric-name KafkaDataLogsDiskUsed \
  --start-time $(date -d '7 days ago' -u +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 86400 \
  --statistics Average

# 2. 效能趨勢
echo "2. Performance Trends:"
aws cloudwatch get-metric-statistics \
  --namespace AWS/Kafka \
  --metric-name EstimatedMaxTimeLag \
  --start-time $(date -d '7 days ago' -u +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 3600 \
  --statistics Maximum

# 3. 錯誤率分析
echo "3. Error Rate Analysis:"
aws cloudwatch get-metric-statistics \
  --namespace AWS/Kafka \
  --metric-name ProducerRequestErrors \
  --start-time $(date -d '7 days ago' -u +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 3600 \
  --statistics Sum

# 4. 成本分析
echo "4. Cost Analysis:"
aws ce get-cost-and-usage \
  --time-period Start=$(date -d '7 days ago' +%Y-%m-%d),End=$(date +%Y-%m-%d) \
  --granularity DAILY \
  --metrics BlendedCost \
  --group-by Type=DIMENSION,Key=SERVICE \
  --filter file://msk-cost-filter.json
```

### 警報閾值配置

#### 關鍵指標閾值
```yaml
Critical Thresholds:
  offline_partitions: 0          # 任何離線分區都是緊急事件
  under_replicated_partitions: 0 # 任何未複製分區都是嚴重事件
  consumer_lag_ms: 300000        # 5 分鐘延遲為嚴重事件
  broker_cpu_percent: 90         # CPU 90% 為嚴重事件
  broker_memory_percent: 90      # 記憶體 90% 為嚴重事件
  disk_usage_percent: 85         # 磁碟 85% 為嚴重事件

Warning Thresholds:
  consumer_lag_ms: 60000         # 1 分鐘延遲為警告
  broker_cpu_percent: 70         # CPU 70% 為警告
  broker_memory_percent: 80      # 記憶體 80% 為警告
  disk_usage_percent: 80         # 磁碟 80% 為警告
  producer_error_rate: 0.001     # 0.1% 錯誤率為警告
  network_io_percent: 60         # 網路 I/O 60% 為警告
```

#### 警報響應動作
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

## 📈 容量規劃指南

### 容量監控指標

#### 1. 儲存容量規劃
```bash
#!/bin/bash
# storage-capacity-planning.sh

# 當前儲存使用率
CURRENT_USAGE=$(aws cloudwatch get-metric-statistics \
  --namespace AWS/Kafka \
  --metric-name KafkaDataLogsDiskUsed \
  --start-time $(date -d '1 hour ago' -u +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 3600 \
  --statistics Average \
  --query 'Datapoints[0].Average')

echo "Current storage usage: ${CURRENT_USAGE}%"

# 預測未來 30 天使用量
if (( $(echo "$CURRENT_USAGE > 70" | bc -l) )); then
  echo "WARNING: Storage usage > 70%, consider scaling up"
  
  # 計算預計滿載時間
  GROWTH_RATE=$(calculate_growth_rate.py --days 30)
  DAYS_TO_FULL=$(echo "scale=0; (100 - $CURRENT_USAGE) / $GROWTH_RATE" | bc)
  
  echo "Estimated days to full capacity: $DAYS_TO_FULL"
  
  if (( DAYS_TO_FULL < 30 )); then
    echo "CRITICAL: Need to scale storage within 30 days"
    ./scripts/scaling/request-storage-scaling.sh
  fi
fi
```

#### 2. 計算容量規劃
```python
#!/usr/bin/env python3
# capacity-planning.py

import boto3
from datetime import datetime, timedelta
import numpy as np

def calculate_capacity_requirements():
    cloudwatch = boto3.client('cloudwatch')
    
    # 獲取過去 30 天的指標
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(days=30)
    
    metrics = {
        'throughput': get_metric_data('MessagesInPerSec', start_time, end_time),
        'storage': get_metric_data('KafkaDataLogsDiskUsed', start_time, end_time),
        'cpu': get_metric_data('CpuUser', start_time, end_time),
        'memory': get_metric_data('MemoryUsed', start_time, end_time)
    }
    
    # 計算趨勢和預測
    predictions = {}
    for metric_name, data in metrics.items():
        trend = calculate_trend(data)
        prediction = predict_future_usage(data, days=90)
        predictions[metric_name] = {
            'current': data[-1] if data else 0,
            'trend': trend,
            'predicted_90_days': prediction
        }
    
    # 生成容量建議
    recommendations = generate_capacity_recommendations(predictions)
    
    return {
        'predictions': predictions,
        'recommendations': recommendations,
        'timestamp': datetime.utcnow().isoformat()
    }

def generate_capacity_recommendations(predictions):
    recommendations = []
    
    # 儲存建議
    if predictions['storage']['predicted_90_days'] > 80:
        recommendations.append({
            'type': 'storage_scaling',
            'priority': 'high',
            'action': 'Increase broker storage by 50%',
            'timeline': '30 days'
        })
    
    # CPU 建議
    if predictions['cpu']['predicted_90_days'] > 70:
        recommendations.append({
            'type': 'compute_scaling',
            'priority': 'medium',
            'action': 'Upgrade to larger instance type',
            'timeline': '60 days'
        })
    
    # 吞吐量建議
    if predictions['throughput']['trend'] > 0.1:  # 10% 增長
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

### 擴展觸發器

#### 自動擴展配置
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

#### 手動擴展程序
```bash
#!/bin/bash
# manual-scaling-procedure.sh

function scale_brokers() {
  local target_count=$1
  
  echo "Scaling MSK cluster to $target_count brokers..."
  
  # 1. 更新 CDK 配置
  sed -i "s/numberOfBrokerNodes: [0-9]*/numberOfBrokerNodes: $target_count/" \
    infrastructure/src/stacks/msk-stack.ts
  
  # 2. 部署更新
  cd infrastructure
  cdk deploy MSKStack --require-approval never
  
  # 3. 等待擴展完成
  aws kafka describe-cluster --cluster-arn $MSK_CLUSTER_ARN \
    --query 'ClusterInfo.NumberOfBrokerNodes'
  
  # 4. 驗證集群健康
  ./scripts/health-check/verify-cluster-health.sh
  
  echo "Broker scaling completed"
}

function scale_storage() {
  local new_size_gb=$1
  
  echo "Scaling storage to ${new_size_gb}GB per broker..."
  
  # 1. 創建擴展請求
  aws kafka update-broker-storage \
    --cluster-arn $MSK_CLUSTER_ARN \
    --target-broker-ebs-volume-info VolumeSize=$new_size_gb
  
  # 2. 監控擴展進度
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

## 🔧 故障排除指南

### 常見問題診斷

#### 1. 消費者延遲問題
```bash
#!/bin/bash
# diagnose-consumer-lag.sh

echo "=== Consumer Lag Diagnosis ==="

# 檢查所有消費者群組
echo "1. Consumer Groups Overview:"
kafka-consumer-groups.sh --bootstrap-server $BOOTSTRAP_SERVERS \
  --describe --all-groups | grep -E "(GROUP|LAG)"

# 識別延遲最嚴重的群組
echo "2. Top Lagging Consumer Groups:"
kafka-consumer-groups.sh --bootstrap-server $BOOTSTRAP_SERVERS \
  --describe --all-groups | sort -k5 -nr | head -10

# 檢查特定群組詳細資訊
read -p "Enter consumer group to analyze: " GROUP_ID
echo "3. Detailed Analysis for $GROUP_ID:"

kafka-consumer-groups.sh --bootstrap-server $BOOTSTRAP_SERVERS \
  --describe --group $GROUP_ID

# 檢查消費者實例
echo "4. Consumer Instances:"
kafka-consumer-groups.sh --bootstrap-server $BOOTSTRAP_SERVERS \
  --describe --group $GROUP_ID --members

# 分析分區分佈
echo "5. Partition Distribution:"
kafka-consumer-groups.sh --bootstrap-server $BOOTSTRAP_SERVERS \
  --describe --group $GROUP_ID --members --verbose

# 建議解決方案
echo "6. Recommendations:"
LAG=$(kafka-consumer-groups.sh --bootstrap-server $BOOTSTRAP_SERVERS \
  --describe --group $GROUP_ID | awk '{sum += $5} END {print sum}')

if (( LAG > 10000 )); then
  echo "- HIGH LAG DETECTED: Consider scaling consumers"
  echo "- Check consumer processing logic for bottlenecks"
  echo "- Verify network connectivity and DNS resolution"
fi
```

#### 2. 生產者效能問題
```bash
#!/bin/bash
# diagnose-producer-performance.sh

echo "=== Producer Performance Diagnosis ==="

# 檢查生產者指標
echo "1. Producer Metrics:"
curl -s "http://localhost:8080/actuator/msk-metrics" | \
  jq '.throughput'

# 檢查批次配置
echo "2. Producer Configuration:"
curl -s "http://localhost:8080/actuator/configprops" | \
  jq '.kafka.producer'

# 分析錯誤模式
echo "3. Error Analysis:"
curl -s "http://localhost:8080/actuator/msk-errors" | \
  jq '.error_stats'

# 檢查網路延遲
echo "4. Network Latency Test:"
for broker in $(echo $BOOTSTRAP_SERVERS | tr ',' ' '); do
  echo "Testing $broker:"
  nc -zv ${broker%:*} ${broker#*:}
done

# 建議優化
echo "5. Optimization Recommendations:"
echo "- Check batch.size and linger.ms configuration"
echo "- Verify compression.type setting"
echo "- Monitor buffer.memory usage"
echo "- Check for DNS resolution issues"
```

#### 3. 分區不平衡問題
```bash
#!/bin/bash
# diagnose-partition-imbalance.sh

echo "=== Partition Imbalance Diagnosis ==="

# 檢查主題分區分佈
echo "1. Topic Partition Distribution:"
kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERVERS \
  --describe | grep -E "(Topic:|Leader:)" | \
  awk '/Leader:/ {print $2, $4}' | sort | uniq -c

# 檢查 Broker 負載分佈
echo "2. Broker Load Distribution:"
kafka-log-dirs.sh --bootstrap-server $BOOTSTRAP_SERVERS \
  --describe --json | jq -r '.brokers[].logDirs[].partitions | length'

# 分析分區大小
echo "3. Partition Size Analysis:"
for topic in $(kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERVERS --list); do
  echo "Topic: $topic"
  kafka-log-dirs.sh --bootstrap-server $BOOTSTRAP_SERVERS \
    --topic-list $topic --describe --json | \
    jq -r '.brokers[].logDirs[].partitions[] | "\(.partition): \(.size) bytes"'
done

# 重新平衡建議
echo "4. Rebalancing Recommendations:"
echo "- Use kafka-reassign-partitions.sh for manual rebalancing"
echo "- Consider using Cruise Control for automated rebalancing"
echo "- Monitor rebalancing impact on performance"
```

### 效能調優指南

#### 1. Broker 調優
```bash
#!/bin/bash
# broker-tuning.sh

echo "=== Broker Performance Tuning ==="

# JVM 調優建議
echo "1. JVM Tuning Recommendations:"
cat << EOF
# Kafka Broker JVM Settings
export KAFKA_HEAP_OPTS="-Xmx6g -Xms6g"
export KAFKA_JVM_PERFORMANCE_OPTS="-server -XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:InitiatingHeapOccupancyPercent=35 -XX:+ExplicitGCInvokesConcurrent -Djava.awt.headless=true"
EOF

# OS 調優建議
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

# 網路調優
echo "3. Network Tuning:"
cat << EOF
# TCP settings
echo "net.core.wmem_default = 131072" >> /etc/sysctl.conf
echo "net.core.rmem_default = 131072" >> /etc/sysctl.conf
echo "net.core.rmem_max = 16777216" >> /etc/sysctl.conf
echo "net.core.wmem_max = 16777216" >> /etc/sysctl.conf
EOF
```

#### 2. 主題配置調優
```bash
#!/bin/bash
# topic-tuning.sh

function optimize_topic() {
  local topic=$1
  local throughput_requirement=$2
  
  echo "Optimizing topic: $topic for $throughput_requirement msgs/sec"
  
  # 計算建議分區數
  local partitions=$(( throughput_requirement / 1000 + 1 ))
  if (( partitions < 3 )); then
    partitions=3
  fi
  
  # 更新主題配置
  kafka-configs.sh --bootstrap-server $BOOTSTRAP_SERVERS \
    --entity-type topics --entity-name $topic --alter \
    --add-config "segment.ms=604800000,retention.ms=604800000,compression.type=gzip"
  
  # 增加分區（如果需要）
  current_partitions=$(kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERVERS \
    --describe --topic $topic | grep "PartitionCount" | awk '{print $4}')
  
  if (( partitions > current_partitions )); then
    kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERVERS \
      --alter --topic $topic --partitions $partitions
    echo "Increased partitions from $current_partitions to $partitions"
  fi
}

# 批次優化所有業務主題
for topic in $(kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERVERS \
  --list | grep "business-events"); do
  optimize_topic $topic 5000
done
```

## 🔄 備份和災難恢復

### 備份策略

#### 1. 自動備份配置
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

#### 2. 備份驗證程序
```bash
#!/bin/bash
# backup-verification.sh

echo "=== Backup Verification $(date) ==="

# 1. 檢查跨區域複製狀態
echo "1. Cross-Region Replication Status:"
aws kafka describe-replication \
  --replication-arn $REPLICATION_ARN \
  --query 'ReplicationInfo.ReplicationState'

# 2. 驗證主題同步
echo "2. Topic Synchronization:"
PRIMARY_TOPICS=$(kafka-topics.sh --bootstrap-server $PRIMARY_BOOTSTRAP \
  --list | sort)
BACKUP_TOPICS=$(kafka-topics.sh --bootstrap-server $BACKUP_BOOTSTRAP \
  --list | sort)

diff <(echo "$PRIMARY_TOPICS") <(echo "$BACKUP_TOPICS")

# 3. 檢查消費者群組偏移量
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

### 災難恢復程序

#### RTO/RPO 目標
```yaml
Recovery Objectives:
  RTO: "< 5 minutes"    # Recovery Time Objective
  RPO: "< 1 minute"     # Recovery Point Objective
  
Service Level Targets:
  availability: "99.9%"
  data_loss_tolerance: "0%"
  max_downtime_per_month: "43.2 minutes"
```

#### 災難恢復執行程序
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
  
  # 更新 DNS 記錄指向備用集群
  aws route53 change-resource-record-sets \
    --hosted-zone-id $HOSTED_ZONE_ID \
    --change-batch file://failover-dns-change.json
  
  # 更新應用程式配置
  kubectl patch configmap kafka-config \
    --patch '{"data":{"bootstrap.servers":"'$BACKUP_BOOTSTRAP_SERVERS'"}}'
  
  # 重啟應用程式 Pod
  kubectl rollout restart deployment/genai-demo-app
  
  echo "2. Verifying failover..."
  sleep 30
  
  # 驗證應用程式連接到備用集群
  kubectl logs -l app=genai-demo-app | grep "Connected to backup cluster"
  
  echo "Primary cluster failover completed"
}

function execute_cross_region_failover() {
  echo "1. Activating backup region..."
  
  # 在備用區域部署完整基礎設施
  cd infrastructure
  AWS_REGION=$BACKUP_REGION cdk deploy --all
  
  # 更新全域負載平衡器
  aws globalaccelerator update-listener \
    --listener-arn $LISTENER_ARN \
    --port-ranges FromPort=443,ToPort=443,Protocol=TCP
  
  echo "2. Migrating traffic..."
  
  # 逐步將流量切換到備用區域
  for weight in 25 50 75 100; do
    aws route53 change-resource-record-sets \
      --hosted-zone-id $HOSTED_ZONE_ID \
      --change-batch file://traffic-shift-${weight}.json
    
    echo "Traffic shifted to ${weight}% backup region"
    sleep 60
    
    # 監控錯誤率
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

## 📋 維護程序

### 定期維護任務

#### 每日維護 (Daily Maintenance)
```bash
#!/bin/bash
# daily-maintenance.sh

echo "=== Daily MSK Maintenance $(date) ==="

# 1. 健康檢查
./scripts/health-check/daily-health-check.sh

# 2. 日誌輪轉
find /var/log/kafka -name "*.log" -mtime +7 -delete

# 3. 指標收集
./scripts/monitoring/collect-daily-metrics.sh

# 4. 備份驗證
./scripts/backup/verify-backup-status.sh

# 5. 容量檢查
./scripts/capacity/check-capacity-usage.sh

echo "Daily maintenance completed"
```

#### 每週維護 (Weekly Maintenance)
```bash
#!/bin/bash
# weekly-maintenance.sh

echo "=== Weekly MSK Maintenance $(date) ==="

# 1. 效能分析
./scripts/performance/weekly-performance-analysis.sh

# 2. 容量規劃
./scripts/capacity/weekly-capacity-planning.sh

# 3. 安全掃描
./scripts/security/weekly-security-scan.sh

# 4. 配置審核
./scripts/audit/weekly-config-audit.sh

# 5. 文檔更新
./scripts/documentation/update-runbook.sh

echo "Weekly maintenance completed"
```

#### 每月維護 (Monthly Maintenance)
```bash
#!/bin/bash
# monthly-maintenance.sh

echo "=== Monthly MSK Maintenance $(date) ==="

# 1. 災難恢復測試
./scripts/dr/monthly-dr-test.sh

# 2. 效能基準測試
./scripts/performance/monthly-benchmark.sh

# 3. 成本優化分析
./scripts/cost/monthly-cost-analysis.sh

# 4. 安全合規檢查
./scripts/compliance/monthly-compliance-check.sh

# 5. 架構審核
./scripts/architecture/monthly-architecture-review.sh

echo "Monthly maintenance completed"
```

### 維護窗口管理

#### 維護窗口排程
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

#### 維護通知程序
```bash
#!/bin/bash
# maintenance-notification.sh

function send_maintenance_notification() {
  local type=$1      # "scheduled" | "emergency"
  local start_time=$2
  local duration=$3
  local impact=$4
  
  # 準備通知內容
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

  # 發送通知
  case $type in
    "scheduled")
      # 提前 24 小時通知
      aws sns publish \
        --topic-arn $MAINTENANCE_TOPIC_ARN \
        --message file://maintenance-notice.json \
        --subject "Scheduled MSK Maintenance - $start_time"
      ;;
    "emergency")
      # 立即通知
      aws sns publish \
        --topic-arn $EMERGENCY_TOPIC_ARN \
        --message file://maintenance-notice.json \
        --subject "Emergency MSK Maintenance - Starting Now"
      ;;
  esac
}
```

## 📞 聯絡資訊和升級路徑

### 團隊聯絡資訊
```yaml
Primary Contacts:
  SRE Team Lead:
    name: "張小明"
    phone: "+886-912-345-678"
    email: "sre-lead@company.com"
    slack: "@sre-lead"
    
  Platform Engineer:
    name: "李小華"
    phone: "+886-987-654-321"
    email: "platform-eng@company.com"
    slack: "@platform-eng"

Secondary Contacts:
  Engineering Manager:
    name: "王大明"
    phone: "+886-955-123-456"
    email: "eng-manager@company.com"
    
  Architecture Lead:
    name: "陳小美"
    phone: "+886-933-789-012"
    email: "arch-lead@company.com"

Executive Escalation:
  VP Engineering:
    name: "林總監"
    phone: "+886-911-111-111"
    email: "vp-eng@company.com"
    
  CTO:
    name: "黃技術長"
    phone: "+886-922-222-222"
    email: "cto@company.com"
```

### 外部供應商聯絡
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

**文檔維護**: 本運營手冊每月更新一次  
**下次審核**: 2025年10月24日  
**緊急聯絡**: ops-team@company.com | +886-911-MSK-OPS (911-675-677)
