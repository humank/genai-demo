# Debugging Guide

> **Status**: ✅ Active  
> **Last Updated**: 2024-11-19

## Overview

This guide provides comprehensive debugging strategies and tools for the GenAI Demo platform.

---

## Quick Reference

For related operational guides, see:
- [Troubleshooting Overview](README.md) - Troubleshooting index
- [Monitoring](../monitoring/README.md) - Monitoring and observability
- [Runbooks](../runbooks/README.md) - Operational procedures

---

## Debugging Tools

### Application Logs

```bash
# View application logs
kubectl logs -f deployment/genai-demo-app -n production

# View logs with context
kubectl logs deployment/genai-demo-app -n production --tail=100

# Filter logs by level
kubectl logs deployment/genai-demo-app -n production | grep ERROR

# View logs from specific time
kubectl logs deployment/genai-demo-app -n production --since=1h
```

### Distributed Tracing

```bash
# Access AWS X-Ray console
aws xray get-trace-summaries --start-time $(date -u -d '1 hour ago' +%s) --end-time $(date +%s)

# View trace details
aws xray get-trace-graph --trace-ids <trace-id>
```

### Metrics and Monitoring

```bash
# Access Grafana dashboard
open https://grafana.example.com

# Query Prometheus metrics
curl -G 'http://prometheus:9090/api/v1/query' \
  --data-urlencode 'query=http_requests_total{job="genai-demo"}'
```

---

## Common Issues

### 1. Application Not Starting

#### Symptoms
- Pod in CrashLoopBackOff state
- Application logs show startup errors
- Health checks failing

#### Debugging Steps

```bash
# Check pod status
kubectl get pods -n production

# Describe pod for events
kubectl describe pod <pod-name> -n production

# Check application logs
kubectl logs <pod-name> -n production

# Check previous container logs
kubectl logs <pod-name> -n production --previous
```

#### Common Causes

**Database Connection Issues**:
```bash
# Test database connectivity
kubectl exec -it <pod-name> -n production -- \
  psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c "SELECT 1"
```

**Configuration Issues**:
```bash
# Check ConfigMap
kubectl get configmap genai-demo-config -n production -o yaml

# Check Secrets
kubectl get secret genai-demo-secrets -n production -o yaml
```

**Resource Constraints**:
```bash
# Check resource usage
kubectl top pod <pod-name> -n production

# Check resource limits
kubectl describe pod <pod-name> -n production | grep -A 5 "Limits"
```

---

### 2. High Response Times

#### Symptoms
- API response times > 2s
- Timeout errors
- User complaints about slow performance

#### Debugging Steps

```bash
# Check application metrics
curl http://localhost:8080/actuator/metrics/http.server.requests

# View slow queries
kubectl logs <pod-name> -n production | grep "SlowQuery"

# Check database performance
aws rds describe-db-instances --db-instance-identifier genai-demo-db
```

#### Performance Analysis

**Database Queries**:
```sql
-- Find slow queries
SELECT query, mean_exec_time, calls
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;

-- Check for missing indexes
SELECT schemaname, tablename, attname, n_distinct, correlation
FROM pg_stats
WHERE schemaname = 'public'
ORDER BY correlation;
```

**Application Profiling**:
```bash
# Enable profiling
kubectl exec -it <pod-name> -n production -- \
  curl -X POST http://localhost:8080/actuator/profiler/start

# Download profile
kubectl exec -it <pod-name> -n production -- \
  curl http://localhost:8080/actuator/profiler/download > profile.jfr
```

---

### 3. Memory Leaks

#### Symptoms
- Increasing memory usage over time
- OutOfMemoryError in logs
- Pod restarts due to OOM

#### Debugging Steps

```bash
# Monitor memory usage
kubectl top pod <pod-name> -n production --containers

# Get heap dump
kubectl exec -it <pod-name> -n production -- \
  jcmd 1 GC.heap_dump /tmp/heapdump.hprof

# Copy heap dump locally
kubectl cp <pod-name>:/tmp/heapdump.hprof ./heapdump.hprof -n production
```

#### Analysis

```bash
# Analyze with jhat
jhat heapdump.hprof

# Or use Eclipse MAT
# Download from: https://www.eclipse.org/mat/
```

---

### 4. Database Connection Pool Exhausted

#### Symptoms
- "Unable to acquire JDBC Connection" errors
- Timeouts on database operations
- High connection wait times

#### Debugging Steps

```bash
# Check connection pool metrics
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active

# View pool configuration
kubectl exec -it <pod-name> -n production -- \
  env | grep SPRING_DATASOURCE
```

#### Solutions

**Increase Pool Size**:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
```

**Check for Connection Leaks**:
```bash
# Enable leak detection
spring.datasource.hikari.leak-detection-threshold=60000
```

---

### 5. Event Processing Failures

#### Symptoms
- Events stuck in queue
- Dead letter queue growing
- Event processing errors in logs

#### Debugging Steps

```bash
# Check Kafka consumer lag
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --group genai-demo-consumer

# View dead letter queue
aws sqs get-queue-attributes \
  --queue-url https://sqs.region.amazonaws.com/account/genai-demo-dlq \
  --attribute-names ApproximateNumberOfMessages
```

#### Analysis

```bash
# View failed events
aws sqs receive-message \
  --queue-url https://sqs.region.amazonaws.com/account/genai-demo-dlq \
  --max-number-of-messages 10

# Replay events
kubectl exec -it <pod-name> -n production -- \
  java -jar event-replayer.jar --event-id <event-id>
```

---

## Debugging Techniques

### 1. Enable Debug Logging

```yaml
# application.yml
logging:
  level:
    solid.humank.genaidemo: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
```

### 2. Remote Debugging

```bash
# Enable remote debugging
kubectl port-forward <pod-name> 5005:5005 -n production

# Connect with IDE
# IntelliJ: Run > Edit Configurations > Remote JVM Debug
# Host: localhost, Port: 5005
```

### 3. Thread Dump Analysis

```bash
# Get thread dump
kubectl exec -it <pod-name> -n production -- \
  jstack 1 > threaddump.txt

# Analyze for deadlocks
grep -A 10 "Found one Java-level deadlock" threaddump.txt
```

### 4. Network Debugging

```bash
# Test connectivity
kubectl exec -it <pod-name> -n production -- \
  curl -v http://service-name:8080/health

# Check DNS resolution
kubectl exec -it <pod-name> -n production -- \
  nslookup service-name

# Trace network path
kubectl exec -it <pod-name> -n production -- \
  traceroute service-name
```

---

## Debugging Checklist

### Initial Investigation

- [ ] Check application logs
- [ ] Review recent deployments
- [ ] Check system metrics
- [ ] Verify external dependencies
- [ ] Review error patterns

### Deep Dive

- [ ] Enable debug logging
- [ ] Collect thread dumps
- [ ] Analyze heap dumps
- [ ] Review database queries
- [ ] Check network connectivity

### Resolution

- [ ] Identify root cause
- [ ] Implement fix
- [ ] Test in staging
- [ ] Deploy to production
- [ ] Monitor for recurrence

---

## Tools and Resources

### Monitoring Tools

- **Grafana**: Metrics visualization
- **Prometheus**: Metrics collection
- **AWS X-Ray**: Distributed tracing
- **CloudWatch**: AWS service logs

### Debugging Tools

- **kubectl**: Kubernetes debugging
- **jstack**: Thread dump analysis
- **jmap**: Heap dump generation
- **Eclipse MAT**: Memory analysis

### Performance Tools

- **JProfiler**: Java profiling
- **VisualVM**: JVM monitoring
- **pgAdmin**: PostgreSQL analysis
- **Redis CLI**: Cache debugging

---

## Related Documentation

- [Troubleshooting Overview](README.md)
- [Monitoring Guide](../monitoring/README.md)
- [Performance Tuning](../../perspectives/performance/overview.md)
- [Runbooks](../runbooks/README.md)

---

**Document Version**: 1.0  
**Last Updated**: 2024-11-19  
**Owner**: Operations Team
