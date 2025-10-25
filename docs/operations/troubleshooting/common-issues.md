# Common Issues and Solutions

## Overview

This document provides quick solutions for common issues encountered in the Enterprise E-Commerce Platform.

## Application Issues

### Issue: Application Won't Start

**Symptoms**: Pods in CrashLoopBackOff, application logs show startup errors

**Quick Checks**:
```bash
kubectl logs ${POD_NAME} -n ${NAMESPACE}
kubectl describe pod ${POD_NAME} -n ${NAMESPACE}
```

**Common Causes**:
1. **Database connection failure**
   - Check database endpoint and credentials
   - Verify security group rules
   - Test connection: `pg_isready -h ${DB_HOST}`

2. **Missing environment variables**
   - Check ConfigMap and Secrets
   - Verify all required vars are set

3. **Port already in use**
   - Check for port conflicts
   - Verify service configuration

**Solution**: See [Service Outage Runbook](../runbooks/service-outage.md)

### Issue: Slow API Responses

**Symptoms**: Response time > 2s, timeout errors

**Quick Checks**:
```bash
curl http://localhost:8080/actuator/metrics/http.server.requests
kubectl top pods -n ${NAMESPACE}
```

**Common Causes**:
1. High CPU/Memory usage
2. Slow database queries
3. Cache misses
4. External API delays

**Solution**: 
- Quick fix: See [Slow API Responses Runbook](../runbooks/slow-api-responses.md)
- Detailed investigation: See [Application Debugging Guide](application-debugging.md#performance-issues)

### Issue: Memory Leaks

**Symptoms**: Memory usage continuously increasing, OOMKilled pods

**Quick Checks**:
```bash
kubectl top pod ${POD_NAME} -n ${NAMESPACE}
jmap -histo:live 1
```

**Common Causes**:
1. Unclosed database connections
2. Unbounded caches
3. Static collections growing
4. ThreadLocal not cleaned

**Solution**: 
- Quick fix: See [High Memory Usage Runbook](../runbooks/high-memory-usage.md)
- Detailed investigation: See [Application Debugging Guide](application-debugging.md#memory-issues)

## Database Issues

### Issue: Connection Pool Exhausted

**Symptoms**: "Too many connections" errors

**Quick Fix**:
```bash
# Kill idle connections
psql -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE state = 'idle' AND query_start < NOW() - INTERVAL '10 minutes';"

# Increase pool size temporarily
kubectl set env deployment/ecommerce-backend HIKARI_MAX_POOL_SIZE=30
```

**Solution**: See [Database Connection Issues Runbook](../runbooks/database-connection-issues.md)

### Issue: Slow Queries

**Symptoms**: Query time > 1s, database CPU high

**Quick Diagnosis**:
```sql
SELECT query, calls, mean_time, max_time
FROM pg_stat_statements
ORDER BY mean_time DESC LIMIT 10;
```

**Quick Fix**:
- Add missing indexes
- Optimize query
- Use query caching

**Solution**: See [Slow API Responses Runbook](../runbooks/slow-api-responses.md)

## Deployment Issues

### Issue: Deployment Stuck

**Symptoms**: Rollout not progressing, pods pending

**Quick Checks**:
```bash
kubectl rollout status deployment/ecommerce-backend -n ${NAMESPACE}
kubectl get events -n ${NAMESPACE} --sort-by='.lastTimestamp'
```

**Quick Fix**:
```bash
# Rollback if needed
kubectl rollout undo deployment/ecommerce-backend -n ${NAMESPACE}
```

**Solution**: See [Failed Deployment Runbook](../runbooks/failed-deployment.md)

### Issue: Image Pull Errors

**Symptoms**: ImagePullBackOff, ErrImagePull

**Quick Fix**:
```bash
# Update image pull secret
TOKEN=$(aws ecr get-login-password)
kubectl create secret docker-registry ecr-secret \
  --docker-server=${ECR_REGISTRY} \
  --docker-username=AWS \
  --docker-password=${TOKEN} \
  --dry-run=client -o yaml | kubectl apply -f -
```

## Network Issues

### Issue: Service Unreachable

**Symptoms**: Connection refused, timeout errors

**Quick Checks**:
```bash
kubectl get svc -n ${NAMESPACE}
kubectl get endpoints -n ${NAMESPACE}
kubectl describe svc ecommerce-backend -n ${NAMESPACE}
```

**Common Causes**:
1. Service selector mismatch
2. Pod not ready
3. Network policy blocking
4. Security group rules

**Solution**: See [Network and Connectivity Troubleshooting Guide](network-connectivity.md)

### Issue: DNS Resolution Failures

**Symptoms**: "Name or service not known" errors

**Quick Fix**:
```bash
# Test DNS resolution
kubectl exec -it ${POD_NAME} -- nslookup kubernetes.default

# Restart CoreDNS
kubectl rollout restart deployment/coredns -n kube-system
```

**Solution**: 
- Quick fix: Restart CoreDNS
- Detailed investigation: See [Network and Connectivity Troubleshooting Guide](network-connectivity.md#dns-resolution-troubleshooting)

## Cache Issues

### Issue: Low Cache Hit Rate

**Symptoms**: Cache hit rate < 70%, slow responses

**Quick Diagnosis**:
```bash
redis-cli INFO stats | grep keyspace
```

**Quick Fix**:
- Increase cache TTL
- Warm cache with frequently accessed data
- Review cache key strategy

### Issue: Redis Connection Failures

**Symptoms**: "Connection refused" to Redis

**Quick Checks**:
```bash
redis-cli ping
kubectl get pods -n ${NAMESPACE} -l app=redis
```

**Quick Fix**:
```bash
# Restart Redis
kubectl rollout restart statefulset/redis -n ${NAMESPACE}
```

## Monitoring Issues

### Issue: Metrics Not Showing

**Symptoms**: Grafana dashboards empty, no metrics

**Quick Checks**:
```bash
# Check metrics endpoint
curl http://localhost:8080/actuator/metrics

# Check Prometheus targets
curl http://prometheus:9090/api/v1/targets
```

**Quick Fix**:
- Verify Prometheus scrape config
- Check service monitor
- Restart Prometheus

### Issue: Alerts Not Firing

**Symptoms**: No alerts despite issues

**Quick Checks**:
```bash
# Check alert rules
curl http://prometheus:9090/api/v1/rules

# Check Alertmanager
curl http://alertmanager:9093/api/v1/alerts
```

## Quick Reference Commands

### Health Checks
```bash
# Application health
curl http://localhost:8080/actuator/health

# Database health
pg_isready -h ${DB_HOST}

# Redis health
redis-cli ping

# Kafka health
kafka-broker-api-versions --bootstrap-server ${KAFKA_BOOTSTRAP}
```

### Resource Checks
```bash
# Pod resources
kubectl top pods -n ${NAMESPACE}

# Node resources
kubectl top nodes

# Database connections
psql -c "SELECT count(*) FROM pg_stat_activity;"
```

### Log Checks
```bash
# Application logs
kubectl logs -f deployment/ecommerce-backend -n ${NAMESPACE}

# Previous container logs
kubectl logs ${POD_NAME} --previous -n ${NAMESPACE}

# All pods logs
kubectl logs -l app=ecommerce-backend -n ${NAMESPACE} --tail=100
```

## Getting Help

### Internal Resources
- **Runbooks**: `/docs/operations/runbooks/`
- **Monitoring**: Grafana dashboards
- **Logs**: CloudWatch Logs, Kibana

### Escalation
- **L1 Support**: DevOps team
- **L2 Support**: Backend engineering team
- **On-Call**: Check PagerDuty schedule

### External Resources
- **AWS Support**: Premium support available
- **Kubernetes Docs**: https://kubernetes.io/docs/
- **Spring Boot Docs**: https://spring.io/projects/spring-boot

## Related Documentation

- [Application Debugging Guide](application-debugging.md) - Detailed debugging workflows and analysis techniques
- [Database Issues Guide](database-issues.md) - Comprehensive database troubleshooting
- [Network and Connectivity Guide](network-connectivity.md) - Network, DNS, TLS, and connectivity troubleshooting
- [Security Incidents Guide](security-incidents.md) - Security troubleshooting and incident response
- [Runbooks](../runbooks/README.md) - Step-by-step operational procedures
- [Monitoring Strategy](../monitoring/monitoring-strategy.md) - Monitoring and alerting setup
- [Deployment Process](../deployment/deployment-process.md) - Deployment procedures

---

**Last Updated**: 2025-10-25  
**Owner**: DevOps Team  
**Review Cycle**: Monthly
