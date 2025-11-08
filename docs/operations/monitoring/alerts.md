# Alert Configuration Guide

## Overview

This document describes all alert configurations, thresholds, and escalation procedures for the Enterprise E-Commerce Platform.

## Alert Severity Levels

| Severity | Response Time | Notification | Escalation |
|----------|---------------|--------------|------------|
| P0 - Critical | Immediate | PagerDuty + SMS + Phone | After 5 minutes |
| P1 - High | < 15 minutes | PagerDuty + Email | After 30 minutes |
| P2 - Medium | < 1 hour | Email + Slack | After 2 hours |
| P3 - Low | < 4 hours | Slack only | Next business day |

## Application Alerts

### API Performance Alerts

#### High Error Rate

```yaml
alert: HighErrorRate
expr: (sum(rate(http_requests_total{status=~"5.."}[5m])) / sum(rate(http_requests_total[5m]))) > 0.05
for: 5m
labels:
  severity: critical
  team: backend
annotations:
  summary: "High error rate detected"
  description: "Error rate is {{ $value | humanizePercentage }} (threshold: 5%)"
  runbook: "https://docs.ecommerce.example.com/runbooks/high-error-rate"
```

**Threshold**: > 5% for 5 minutes  
**Severity**: P0 - Critical  
**Action**: Immediate investigation and potential rollback

#### Slow API Response Time

```yaml
alert: SlowAPIResponseTime
expr: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])) > 2
for: 10m
labels:
  severity: high
  team: backend
annotations:
  summary: "API response time is slow"
  description: "95th percentile response time is {{ $value }}s (threshold: 2s)"
  runbook: "https://docs.ecommerce.example.com/runbooks/slow-api"
```

**Threshold**: > 2s (p95) for 10 minutes  
**Severity**: P1 - High  
**Action**: Investigate performance bottlenecks

#### API Endpoint Down

```yaml
alert: APIEndpointDown
expr: up{job="ecommerce-backend"} == 0
for: 2m
labels:
  severity: critical
  team: backend
annotations:
  summary: "API endpoint is down"
  description: "{{ $labels.instance }} has been down for more than 2 minutes"
  runbook: "https://docs.ecommerce.example.com/runbooks/service-outage"
```

**Threshold**: Down for 2 minutes  
**Severity**: P0 - Critical  
**Action**: Immediate investigation and service restart

### Business Metric Alerts

#### Low Order Rate

```yaml
alert: LowOrderRate
expr: rate(orders_created_total[10m]) < 0.5
for: 15m
labels:
  severity: high
  team: product
annotations:
  summary: "Order creation rate has dropped"
  description: "Order rate is {{ $value }} orders/min (expected: > 0.5)"
  runbook: "https://docs.ecommerce.example.com/runbooks/low-order-rate"
```

**Threshold**: < 0.5 orders/min for 15 minutes  
**Severity**: P1 - High  
**Action**: Check payment gateway, investigate user experience issues

#### High Payment Failure Rate

```yaml
alert: HighPaymentFailureRate
expr: (sum(rate(payments_failed_total[5m])) / sum(rate(payments_total[5m]))) > 0.05
for: 10m
labels:
  severity: critical
  team: backend
annotations:
  summary: "High payment failure rate"
  description: "Payment failure rate is {{ $value | humanizePercentage }} (threshold: 5%)"
  runbook: "https://docs.ecommerce.example.com/runbooks/payment-failures"
```

**Threshold**: > 5% for 10 minutes  
**Severity**: P0 - Critical  
**Action**: Check payment gateway integration, investigate errors

## Infrastructure Alerts

### Compute Alerts

#### High CPU Usage

```yaml
alert: HighCPUUsage
expr: (sum(rate(container_cpu_usage_seconds_total{pod=~"ecommerce-backend.*"}[5m])) by (pod) / sum(container_spec_cpu_quota{pod=~"ecommerce-backend.*"}) by (pod)) > 0.8
for: 10m
labels:
  severity: high
  team: devops
annotations:
  summary: "High CPU usage on {{ $labels.pod }}"
  description: "CPU usage is {{ $value | humanizePercentage }} (threshold: 80%)"
  runbook: "https://docs.ecommerce.example.com/runbooks/high-cpu"
```

**Threshold**: > 80% for 10 minutes  
**Severity**: P1 - High  
**Action**: Investigate CPU-intensive operations, consider scaling

#### High Memory Usage

```yaml
alert: HighMemoryUsage
expr: (sum(container_memory_working_set_bytes{pod=~"ecommerce-backend.*"}) by (pod) / sum(container_spec_memory_limit_bytes{pod=~"ecommerce-backend.*"}) by (pod)) > 0.9
for: 5m
labels:
  severity: critical
  team: devops
annotations:
  summary: "High memory usage on {{ $labels.pod }}"
  description: "Memory usage is {{ $value | humanizePercentage }} (threshold: 90%)"
  runbook: "https://docs.ecommerce.example.com/runbooks/high-memory"
```

**Threshold**: > 90% for 5 minutes  
**Severity**: P0 - Critical  
**Action**: Investigate memory leaks, restart pod if necessary

#### Pod Restart Loop

```yaml
alert: PodRestartLoop
expr: rate(kube_pod_container_status_restarts_total{pod=~"ecommerce-backend.*"}[15m]) > 0.2
for: 5m
labels:
  severity: critical
  team: devops
annotations:
  summary: "Pod {{ $labels.pod }} is in restart loop"
  description: "Pod has restarted {{ $value }} times in 15 minutes"
  runbook: "https://docs.ecommerce.example.com/runbooks/pod-restart-loop"
```

**Threshold**: > 3 restarts in 15 minutes  
**Severity**: P0 - Critical  
**Action**: Check pod logs, investigate crash cause

### Database Alerts

#### High Database Connection Usage

```yaml
alert: HighDatabaseConnectionUsage
expr: (sum(pg_stat_activity_count) / sum(pg_settings_max_connections)) > 0.9
for: 5m
labels:
  severity: critical
  team: backend
annotations:
  summary: "Database connection pool nearly exhausted"
  description: "Connection usage is {{ $value | humanizePercentage }} (threshold: 90%)"
  runbook: "https://docs.ecommerce.example.com/runbooks/database-connections"
```

**Threshold**: > 90% for 5 minutes  
**Severity**: P0 - Critical  
**Action**: Investigate connection leaks, increase pool size if needed

#### Slow Database Queries

```yaml
alert: SlowDatabaseQueries
expr: rate(pg_stat_statements_mean_exec_time_seconds{query!~".*pg_stat.*"}[5m]) > 1
for: 10m
labels:
  severity: high
  team: backend
annotations:
  summary: "Slow database queries detected"
  description: "Average query time is {{ $value }}s (threshold: 1s)"
  runbook: "https://docs.ecommerce.example.com/runbooks/slow-queries"
```

**Threshold**: > 1s average for 10 minutes  
**Severity**: P1 - High  
**Action**: Identify and optimize slow queries

#### Database Replication Lag

```yaml
alert: DatabaseReplicationLag
expr: pg_replication_lag_seconds > 5
for: 5m
labels:
  severity: high
  team: devops
annotations:
  summary: "Database replication lag is high"
  description: "Replication lag is {{ $value }}s (threshold: 5s)"
  runbook: "https://docs.ecommerce.example.com/runbooks/replication-lag"
```

**Threshold**: > 5s for 5 minutes  
**Severity**: P1 - High  
**Action**: Check replica health, investigate network issues

### Cache Alerts

#### Low Cache Hit Rate

```yaml
alert: LowCacheHitRate
expr: (sum(rate(redis_keyspace_hits_total[5m])) / (sum(rate(redis_keyspace_hits_total[5m])) + sum(rate(redis_keyspace_misses_total[5m])))) < 0.7
for: 15m
labels:
  severity: medium
  team: backend
annotations:
  summary: "Cache hit rate is low"
  description: "Hit rate is {{ $value | humanizePercentage }} (threshold: 70%)"
  runbook: "https://docs.ecommerce.example.com/runbooks/low-cache-hit-rate"
```

**Threshold**: < 70% for 15 minutes  
**Severity**: P2 - Medium  
**Action**: Review cache strategy, adjust TTL values

#### High Redis Memory Usage

```yaml
alert: HighRedisMemoryUsage
expr: (redis_memory_used_bytes / redis_memory_max_bytes) > 0.9
for: 5m
labels:
  severity: critical
  team: devops
annotations:
  summary: "Redis memory usage is high"
  description: "Memory usage is {{ $value | humanizePercentage }} (threshold: 90%)"
  runbook: "https://docs.ecommerce.example.com/runbooks/redis-memory"
```

**Threshold**: > 90% for 5 minutes  
**Severity**: P0 - Critical  
**Action**: Increase Redis memory or implement eviction policy

### Message Queue Alerts

#### High Kafka Consumer Lag

```yaml
alert: HighKafkaConsumerLag
expr: kafka_consumergroup_lag > 10000
for: 10m
labels:
  severity: high
  team: backend
annotations:
  summary: "Kafka consumer lag is high"
  description: "Consumer lag is {{ $value }} messages (threshold: 10000)"
  runbook: "https://docs.ecommerce.example.com/runbooks/kafka-lag"
```

**Threshold**: > 10000 messages for 10 minutes  
**Severity**: P1 - High  
**Action**: Scale consumers, investigate processing bottlenecks

#### Kafka Partition Offline

```yaml
alert: KafkaPartitionOffline
expr: kafka_topic_partition_in_sync_replica < kafka_topic_partition_replicas
for: 5m
labels:
  severity: critical
  team: devops
annotations:
  summary: "Kafka partition has offline replicas"
  description: "Topic {{ $labels.topic }} partition {{ $labels.partition }} has offline replicas"
  runbook: "https://docs.ecommerce.example.com/runbooks/kafka-partition-offline"
```

**Threshold**: Any partition offline for 5 minutes  
**Severity**: P0 - Critical  
**Action**: Check Kafka broker health, restart if necessary

## Alert Escalation

### Escalation Policy

```yaml
escalation_policy:

  - level: 1

    delay: 0m
    notify:

      - on-call-engineer

    channels:

      - pagerduty
      - sms
  
  - level: 2

    delay: 5m
    notify:

      - on-call-engineer
      - team-lead

    channels:

      - pagerduty
      - sms
      - phone
  
  - level: 3

    delay: 15m
    notify:

      - on-call-engineer
      - team-lead
      - engineering-manager

    channels:

      - pagerduty
      - sms
      - phone
      - email

```

### On-Call Schedule

| Time | Primary | Secondary | Manager |
|------|---------|-----------|---------|
| Weekdays 9am-6pm | Engineer A | Engineer B | Manager X |
| Weekdays 6pm-9am | Engineer C | Engineer D | Manager X |
| Weekends | Engineer E | Engineer F | Manager Y |

## Alert Notification Channels

### PagerDuty

```yaml
pagerduty:
  service_key: ${PAGERDUTY_SERVICE_KEY}
  severity_mapping:
    critical: high
    high: high
    medium: low
    low: info
```

### Slack

```yaml
slack:
  webhook_url: ${SLACK_WEBHOOK_URL}
  channels:
    critical: "#alerts-critical"
    high: "#alerts-high"
    medium: "#alerts-medium"
    low: "#alerts-low"
```

### Email

```yaml
email:
  smtp_server: smtp.example.com
  from: alerts@ecommerce.example.com
  recipients:
    critical:

      - oncall@ecommerce.example.com
      - manager@ecommerce.example.com

    high:

      - oncall@ecommerce.example.com

    medium:

      - team@ecommerce.example.com

```

## Alert Response Procedures

### P0 - Critical Alerts

1. **Acknowledge** alert within 2 minutes
2. **Assess** impact and severity
3. **Notify** team lead immediately
4. **Investigate** root cause
5. **Mitigate** issue (rollback, restart, scale)
6. **Verify** resolution
7. **Document** incident
8. **Follow up** with RCA

### P1 - High Alerts

1. **Acknowledge** alert within 15 minutes
2. **Assess** impact
3. **Investigate** root cause
4. **Implement** fix
5. **Verify** resolution
6. **Document** findings

### P2 - Medium Alerts

1. **Acknowledge** alert within 1 hour
2. **Investigate** when available
3. **Create** ticket for tracking
4. **Schedule** fix
5. **Verify** resolution

### P3 - Low Alerts

1. **Review** during business hours
2. **Create** ticket if action needed
3. **Schedule** fix in next sprint

## Alert Tuning

### Reducing False Positives

- **Adjust thresholds** based on historical data
- **Increase evaluation period** for transient issues
- **Add context** with multiple conditions
- **Use rate of change** instead of absolute values

### Example: Tuned Alert

```yaml
# Before: Too sensitive
alert: HighCPU
expr: cpu_usage > 0.7
for: 1m

# After: Better tuned
alert: HighCPU
expr: cpu_usage > 0.8 and rate(cpu_usage[5m]) > 0.1
for: 10m
```

## Alert Testing

### Test Procedures

```bash
# Test alert firing
curl -X POST http://prometheus:9090/api/v1/alerts \
  -d 'alert=HighErrorRate&value=0.1'

# Test notification delivery
curl -X POST ${SLACK_WEBHOOK_URL} \
  -d '{"text":"Test alert notification"}'

# Test PagerDuty integration
curl -X POST https://events.pagerduty.com/v2/enqueue \
  -H "Content-Type: application/json" \
  -d '{
    "routing_key": "${PAGERDUTY_KEY}",
    "event_action": "trigger",
    "payload": {
      "summary": "Test alert",
      "severity": "critical",
      "source": "test"
    }
  }'
```

## Alert Maintenance

### Weekly Tasks

- [ ] Review alert effectiveness
- [ ] Check for alert fatigue
- [ ] Update thresholds if needed
- [ ] Test notification channels

### Monthly Tasks

- [ ] Review alert response times
- [ ] Analyze false positive rate
- [ ] Update escalation policies
- [ ] Conduct alert drills

## Related Documentation

- [Monitoring Strategy](monitoring-strategy.md)
- [Runbooks](../runbooks/README.md)
- [Incident Response](../runbooks/service-outage.md)
- [Troubleshooting Guide](../troubleshooting/common-issues.md)

---

**Last Updated**: 2025-10-25  
**Owner**: DevOps Team  
**Review Cycle**: Monthly
