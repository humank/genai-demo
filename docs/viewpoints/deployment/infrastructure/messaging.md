---
title: "Messaging Infrastructure"
type: "deployment-viewpoint"
category: "infrastructure"
status: "active"
last_updated: "2025-10-23"
owner: "DevOps Team"
---

# Messaging Infrastructure

> **Service**: Amazon MSK (Managed Streaming for Apache Kafka)

## Cluster Configuration

**Cluster Details**:
```yaml
Cluster Name: ecommerce-platform-prod-kafka
Kafka Version: 3.5.1
Broker Type: kafka.m5.large
  - vCPU: 2
  - Memory: 8 GiB
  - Network: Up to 10 Gbps

Brokers: 3 (one per AZ)
Availability Zones:
  - us-east-1a
  - us-east-1b
  - us-east-1c
```

**Storage Configuration**:
- Storage per Broker: 1000 GB
- Storage Type: EBS (gp3)
- IOPS: 3000
- Throughput: 250 MB/s
- Auto-scaling: Enabled (up to 2000 GB)

## Kafka Configuration

**Broker Settings**:
```properties
# Replication
default.replication.factor=3
min.insync.replicas=2
unclean.leader.election.enable=false

# Performance
num.network.threads=8
num.io.threads=16
socket.send.buffer.bytes=102400
socket.receive.buffer.bytes=102400
socket.request.max.bytes=104857600

# Log Retention
log.retention.hours=168  # 7 days
log.retention.bytes=-1   # No size limit
log.segment.bytes=1073741824  # 1 GB
log.cleanup.policy=delete

# Compression
compression.type=snappy
```

## Topic Configuration

**Standard Topics**:
- **order-events**: 12 partitions, 3 replicas, 7 days retention
- **customer-events**: 6 partitions, 3 replicas, 7 days retention
- **payment-events**: 6 partitions, 3 replicas, 7 days retention
- **notification-events**: 12 partitions, 3 replicas, 3 days retention

## Monitoring and Security

**Monitoring**:
- CloudWatch Metrics: KafkaBytesInPerSec, KafkaBytesOutPerSec, KafkaMessagesInPerSec, PartitionCount, UnderReplicatedPartitions, OfflinePartitionsCount
- Alarms:
  - UnderReplicatedPartitions > 0
  - OfflinePartitionsCount > 0
  - CPU > 75%
  - Disk Usage > 80%

**Security**:
- Encryption: TLS 1.2 (In Transit), AWS KMS (At Rest)
- Authentication: SASL/SCRAM
- Authorization: ACLs Enabled
