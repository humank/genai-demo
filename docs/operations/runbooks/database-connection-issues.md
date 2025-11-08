# Runbook: Database Connection Issues

## Symptoms

- "Connection refused" errors in logs
- "Too many connections" errors
- Timeout errors when accessing database
- Application unable to start

## Impact

- **Severity**: P0 - Critical
- **Affected Users**: All users
- **Business Impact**: Service unavailable, no data access

## Detection

- **Alert**: `HighDatabaseConnectionUsage` or `DatabaseConnectionFailed`
- **Monitoring Dashboard**: Database Dashboard > Connections
- **Log Patterns**: `SQLException`, `Connection timeout`, `Too many connections`

## Diagnosis

### Step 1: Check Connection Pool Status

```bash
# Check application logs
kubectl logs deployment/ecommerce-backend -n production | grep -i "connection"

# Check connection pool metrics
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
```

### Step 2: Check Database Status

```bash
# Check RDS instance status
aws rds describe-db-instances \
  --db-instance-identifier ecommerce-production \
  --query 'DBInstances[0].DBInstanceStatus'

# Check current connections
kubectl exec -it ${POD_NAME} -n production -- \
  psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} \
  -c "SELECT count(*) FROM pg_stat_activity;"

# Check connection limit
kubectl exec -it ${POD_NAME} -n production -- \
  psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} \
  -c "SHOW max_connections;"
```

### Step 3: Identify Connection Leaks

```bash
# Check long-running connections
kubectl exec -it ${POD_NAME} -n production -- \
  psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} \
  -c "SELECT pid, usename, application_name, state, query_start, query 
      FROM pg_stat_activity 
      WHERE state != 'idle' 
      ORDER BY query_start;"
```

## Resolution

### Immediate Actions

1. **Kill idle connections** (if safe):

```bash
kubectl exec -it ${POD_NAME} -n production -- \
  psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} \
  -c "SELECT pg_terminate_backend(pid) 
      FROM pg_stat_activity 
      WHERE state = 'idle' 
      AND query_start < NOW() - INTERVAL '10 minutes';"
```

1. **Restart application pods**:

```bash
kubectl rollout restart deployment/ecommerce-backend -n production
```

### Root Cause Fixes

#### If connection pool exhausted

1. Increase connection pool size:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30  # Increase from 20
      minimum-idle: 15       # Increase from 10
```

1. Fix connection leaks in code:
   - Ensure proper try-with-resources usage
   - Close connections in finally blocks
   - Use @Transactional properly

#### If database max_connections reached

1. Increase RDS max_connections:

```bash
aws rds modify-db-parameter-group \
  --db-parameter-group-name ecommerce-prod \
  --parameters "ParameterName=max_connections,ParameterValue=200,ApplyMethod=immediate"
```

1. Restart RDS instance if needed

## Verification

- [ ] Application can connect to database
- [ ] Connection pool metrics normal
- [ ] No connection errors in logs
- [ ] API endpoints responding
- [ ] Database queries executing successfully

## Prevention

1. **Proper connection management**:
   - Use connection pooling
   - Set appropriate timeouts
   - Implement connection validation

2. **Monitoring**:
   - Monitor connection pool usage
   - Alert on high connection count
   - Track connection leaks

3. **Code review**:
   - Review database access code
   - Ensure proper resource cleanup
   - Use try-with-resources

## Escalation

- **L1 Support**: DevOps team
- **L2 Support**: Backend engineering team
- **Database DBA**: For RDS-specific issues

## Related

- [Slow Database Queries](slow-queries.md)
- [High Memory Usage](high-memory-usage.md)
- [Service Outage](service-outage.md)

---

**Last Updated**: 2025-10-25  
**Owner**: DevOps Team
