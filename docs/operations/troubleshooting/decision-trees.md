# Troubleshooting Decision Trees

## Overview

This document provides systematic decision trees for diagnosing and resolving common issues in the Enterprise E-Commerce Platform. Each decision tree follows an if-then-else diagnostic workflow with clear escalation criteria and procedures.

**Purpose**: Provide structured troubleshooting workflows for rapid issue resolution  
**Audience**: DevOps engineers, SREs, on-call engineers, support teams  
**Usage**: Follow the decision tree from top to bottom, answering each question to reach the appropriate resolution path

---

## How to Use This Guide

### Decision Tree Structure

Each decision tree follows this format:

1. **Initial Symptom**: The observable problem or alert
2. **Diagnostic Questions**: Yes/No questions to narrow down the issue
3. **Action Steps**: Specific commands or procedures to execute
4. **Escalation Criteria**: When to escalate to the next level
5. **Resolution Verification**: How to confirm the issue is resolved

### Severity Classification

| Severity | Response Time | Escalation | Examples |
|----------|---------------|------------|----------|
| **P0 - Critical** | Immediate | CTO + Security Team | System down, data breach, active attack |
| **P1 - High** | 15 minutes | Engineering Manager | Service degradation, authentication failure |
| **P2 - Medium** | 1 hour | On-call Engineer | Performance issues, partial outage |
| **P3 - Low** | 4 hours | Team Lead | Minor issues, warnings |
| **P4 - Informational** | 24 hours | Regular Sprint | Optimization opportunities |

### Escalation Contacts

- **L1 Support**: DevOps Team (Slack: #devops-support)
- **L2 Support**: Backend Engineering Team (Slack: #backend-eng)
- **L3 Support**: Architecture Team (Slack: #architecture)
- **Security Team**: <security@company.com> (PagerDuty: Security On-Call)
- **On-Call Engineer**: PagerDuty rotation
- **AWS Support**: Premium support portal

---

## Decision Tree Index

1. [Service Availability Issues](#1-service-availability-decision-tree)
2. [Performance Degradation](#2-performance-degradation-decision-tree)
3. [Authentication & Authorization](#3-authentication--authorization-decision-tree)
4. [Database Issues](#4-database-issues-decision-tree)
5. [Network Connectivity](#5-network-connectivity-decision-tree)
6. [Security Incidents](#6-security-incident-decision-tree)
7. [Deployment Failures](#7-deployment-failure-decision-tree)
8. [Cache Issues](#8-cache-issues-decision-tree)
9. [Message Queue Issues](#9-message-queue-issues-decision-tree)
10. [Resource Exhaustion](#10-resource-exhaustion-decision-tree)

---

## 1. Service Availability Decision Tree

### Initial Symptom: Service Unreachable or Down

```text
Service Unreachable/Down Alert
│
├─ Can you access the service endpoint?
│  │
│  ├─ NO → Service is down
│  │  │
│  │  ├─ Are pods running?
│  │  │  │
│  │  │  ├─ NO → Pod scheduling issue
│  │  │  │  ├─ Check: kubectl get pods -n production
│  │  │  │  ├─ Status: Pending/CrashLoopBackOff/ImagePullBackOff?
│  │  │  │  │  ├─ Pending → Resource constraints
│  │  │  │  │  │  ├─ Action: Check node resources
│  │  │  │  │  │  ├─ Command: kubectl describe pod ${POD_NAME}
│  │  │  │  │  │  ├─ Fix: Scale cluster or adjust resource requests
│  │  │  │  │  │  └─ Escalate: If nodes at capacity → L2 Support
│  │  │  │  │  │
│  │  │  │  │  ├─ CrashLoopBackOff → Application startup failure
│  │  │  │  │  │  ├─ Action: Check application logs
│  │  │  │  │  │  ├─ Command: kubectl logs ${POD_NAME} --previous
│  │  │  │  │  │  ├─ Common causes:
│  │  │  │  │  │  │  ├─ Database connection failure
│  │  │  │  │  │  │  ├─ Missing environment variables
│  │  │  │  │  │  │  ├─ Configuration errors
│  │  │  │  │  │  │  └─ Dependency unavailable
│  │  │  │  │  │  ├─ Fix: Correct configuration or rollback
│  │  │  │  │  │  └─ Escalate: If unknown error → L2 Support
│  │  │  │  │  │
│  │  │  │  │  └─ ImagePullBackOff → Image pull failure
│  │  │  │  │     ├─ Action: Check image registry access
│  │  │  │  │     ├─ Command: kubectl describe pod ${POD_NAME}
│  │  │  │  │     ├─ Fix: Update image pull secret or image tag
│  │  │  │  │     └─ Escalate: If registry issue → L1 Support
│  │  │  │  │
│  │  │  │  └─ Severity: P1 - High
│  │  │  │
│  │  │  └─ YES → Pods running but service unavailable
│  │  │     │
│  │  │     ├─ Is the service endpoint configured?
│  │  │     │  ├─ Check: kubectl get svc -n production
│  │  │     │  ├─ Check: kubectl get endpoints -n production
│  │  │     │  ├─ NO → Service misconfiguration
│  │  │     │  │  ├─ Fix: Verify service selector matches pod labels
│  │  │     │  │  └─ Escalate: If configuration correct → L2 Support
│  │  │     │  │
│  │  │     │  └─ YES → Network or health check issue
│  │  │     │     ├─ Are pods healthy?
│  │  │     │     │  ├─ Check: kubectl get pods -n production
│  │  │     │     │  ├─ Check: curl http://${POD_IP}:8080/actuator/health
│  │  │     │     │  ├─ NO → Health check failing
│  │  │     │     │  │  ├─ Action: Check application health
│  │  │     │     │  │  ├─ Common causes:
│  │  │     │     │  │  │  ├─ Database connectivity
│  │  │     │     │  │  │  ├─ Dependency service down
│  │  │     │     │  │  │  └─ Resource exhaustion
│  │  │     │     │  │  └─ Fix: Resolve dependency or restart
│  │  │     │     │  │
│  │  │     │     │  └─ YES → Network policy or security group issue
│  │  │     │     │     ├─ Action: Check network policies
│  │  │     │     │     ├─ Command: kubectl get networkpolicies
│  │  │     │     │     ├─ Check: AWS security groups
│  │  │     │     │     └─ Escalate: L1 Support for network issues
│  │  │     │
│  │  │     └─ Severity: P0 - Critical
│  │  │
│  │  └─ Immediate Actions:
│  │     ├─ 1. Alert on-call engineer
│  │     ├─ 2. Check recent deployments
│  │     ├─ 3. Consider rollback if recent change
│  │     └─ 4. Scale up healthy pods if partial outage
│  │
│  └─ YES → Service accessible but degraded
│     │
│     ├─ Response time > 5s?
│     │  ├─ YES → Performance issue (See Decision Tree #2)
│     │  └─ NO → Intermittent issue
│     │     ├─ Check error rate
│     │     ├─ Monitor for patterns
│     │     └─ Severity: P2 - Medium
│     │
│     └─ Error rate > 5%?
│        ├─ YES → Application errors (Check logs)
│        │  └─ Escalate: L2 Support
│        └─ NO → Monitor and investigate
│           └─ Severity: P3 - Low
│
└─ Resolution Verification:
   ├─ Service endpoint responds with 200 OK
   ├─ All pods in Running state
   ├─ Health checks passing
   ├─ Response time < 2s
   └─ Error rate < 1%
```

### Quick Commands

```bash
# Check service status
kubectl get pods,svc,endpoints -n production -l app=ecommerce-backend

# Check pod health
kubectl exec -it ${POD_NAME} -n production -- \
  curl http://localhost:8080/actuator/health

# Check recent events
kubectl get events -n production --sort-by='.lastTimestamp' | tail -20

# Rollback if needed
kubectl rollout undo deployment/ecommerce-backend -n production
```

### Escalation Criteria

- **Immediate (P0)**: Complete service outage affecting all users
- **15 minutes (P1)**: Partial outage or critical functionality unavailable
- **1 hour (P2)**: Degraded performance but service functional
- **4 hours (P3)**: Minor issues or warnings

---

## 2. Performance Degradation Decision Tree

### Initial Symptom: Slow Response Times or High Latency

```text
Performance Degradation Alert
│
├─ What is the response time?
│  │
│  ├─ > 5s (95th percentile) → P1 Critical
│  │  ├─ Immediate Actions:
│  │  │  ├─ Scale up replicas immediately
│  │  │  ├─ Check for system-wide outages
│  │  │  └─ Engage on-call engineer
│  │  └─ Continue diagnosis in parallel
│  │
│  ├─ 3-5s → P2 High
│  ├─ 2-3s → P3 Medium
│  └─ 1.5-2s → P4 Low
│
├─ Is it affecting all endpoints?
│  │
│  ├─ YES → System-wide performance issue
│  │  │
│  │  ├─ Check resource utilization
│  │  │  ├─ CPU > 80%?
│  │  │  │  ├─ YES → CPU bottleneck
│  │  │  │  │  ├─ Action: Check CPU-intensive processes
│  │  │  │  │  ├─ Command: kubectl top pods -n production
│  │  │  │  │  ├─ Check: Thread dumps for hot spots
│  │  │  │  │  ├─ Fix: Scale horizontally or optimize code
│  │  │  │  │  └─ Escalate: If optimization needed → L2 Support
│  │  │  │  │
│  │  │  │  └─ NO → Check memory
│  │  │  │
│  │  │  ├─ Memory > 80%?
│  │  │  │  ├─ YES → Memory pressure
│  │  │  │  │  ├─ Action: Check for memory leaks
│  │  │  │  │  ├─ Command: Check heap usage
│  │  │  │  │  ├─ Check: GC frequency and duration
│  │  │  │  │  ├─ Fix: Increase memory or fix leak
│  │  │  │  │  └─ Escalate: If leak suspected → L2 Support
│  │  │  │  │
│  │  │  │  └─ NO → Check I/O
│  │  │  │
│  │  │  └─ I/O wait > 20%?
│  │  │     ├─ YES → I/O bottleneck
│  │  │     │  ├─ Check: Database performance
│  │  │     │  ├─ Check: Disk I/O metrics
│  │  │     │  └─ See: Database Decision Tree (#4)
│  │  │     │
│  │  │     └─ NO → Check external dependencies
│  │  │
│  │  ├─ Check database performance
│  │  │  ├─ Query time > 100ms?
│  │  │  │  ├─ YES → Database bottleneck
│  │  │  │  │  ├─ Action: Identify slow queries
│  │  │  │  │  ├─ Command: Check pg_stat_statements
│  │  │  │  │  ├─ Check: Connection pool utilization
│  │  │  │  │  ├─ Fix: Add indexes or optimize queries
│  │  │  │  │  └─ See: Database Decision Tree (#4)
│  │  │  │  │
│  │  │  │  └─ NO → Database performing normally
│  │  │  │
│  │  │  └─ Connection pool > 80% utilized?
│  │  │     ├─ YES → Connection pool exhaustion
│  │  │     │  ├─ Action: Increase pool size
│  │  │     │  ├─ Check: For connection leaks
│  │  │     │  └─ Fix: Tune pool configuration
│  │  │     │
│  │  │     └─ NO → Pool healthy
│  │  │
│  │  └─ Check external dependencies
│  │     ├─ External API timeout?
│  │     │  ├─ YES → External dependency issue
│  │     │  │  ├─ Action: Check circuit breaker status
│  │     │  │  ├─ Check: External service health
│  │     │  │  ├─ Fix: Implement fallback or contact vendor
│  │     │  │  └─ Escalate: If vendor issue → L3 Support
│  │     │  │
│  │     │  └─ NO → External services healthy
│  │     │
│  │     └─ Network latency high?
│  │        ├─ YES → Network issue
│  │        │  ├─ Check: DNS resolution time
│  │        │  ├─ Check: Network policies
│  │        │  └─ See: Network Decision Tree (#5)
│  │        │
│  │        └─ NO → Continue investigation
│  │
│  └─ NO → Endpoint-specific performance issue
│     │
│     ├─ Identify affected endpoint
│     │  ├─ Action: Check endpoint metrics
│     │  ├─ Command: Review application logs
│     │  └─ Analyze: Request patterns
│     │
│     ├─ Check endpoint-specific issues
│     │  ├─ N+1 query problem?
│     │  │  ├─ YES → Query optimization needed
│     │  │  │  ├─ Action: Review query patterns
│     │  │  │  ├─ Fix: Use JOIN FETCH or batch loading
│     │  │  │  └─ Escalate: L2 Support for code changes
│     │  │  │
│     │  │  └─ NO → Continue
│     │  │
│     │  ├─ Large payload size?
│     │  │  ├─ YES → Payload optimization needed
│     │  │  │  ├─ Action: Implement pagination
│     │  │  │  ├─ Fix: Use projections or DTOs
│     │  │  │  └─ Escalate: L2 Support for API changes
│     │  │  │
│     │  │  └─ NO → Continue
│     │  │
│     │  └─ Recent code changes?
│     │     ├─ YES → Code regression
│     │     │  ├─ Action: Review recent commits
│     │     │  ├─ Fix: Rollback or hotfix
│     │     │  └─ Escalate: L2 Support immediately
│     │     │
│     │     └─ NO → Deep analysis required
│     │        └─ Escalate: L2 Support
│     │
│     └─ Severity: P2-P3 depending on impact
│
├─ Check cache performance
│  ├─ Cache hit rate < 70%?
│  │  ├─ YES → Cache issue
│  │  │  ├─ Action: Analyze cache patterns
│  │  │  ├─ Check: Cache eviction rate
│  │  │  ├─ Fix: Warm cache or adjust TTL
│  │  │  └─ See: Cache Decision Tree (#8)
│  │  │
│  │  └─ NO → Cache performing well
│  │
│  └─ Cache size appropriate?
│     ├─ Check: Memory usage
│     └─ Adjust: If needed
│
└─ Resolution Verification:
   ├─ Response time < 2s (95th percentile)
   ├─ Resource utilization < 70%
   ├─ Cache hit rate > 80%
   ├─ Database query time < 100ms
   └─ No external API timeouts
```

### Quick Commands

```bash
# Check response times
curl http://localhost:8080/actuator/metrics/http.server.requests | \
  jq '.measurements[] | select(.statistic == "MAX")'

# Check resource usage
kubectl top pods -n production -l app=ecommerce-backend

# Check database performance
kubectl exec -it ${POD_NAME} -- \
  psql -c "SELECT query, mean_time FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 10;"

# Check cache hit rate
curl http://localhost:8080/actuator/metrics/cache.gets | jq
```

### Escalation Criteria

- **Immediate (P1)**: Response time > 5s affecting all users
- **15 minutes (P2)**: Response time > 3s or affecting critical endpoints
- **1 hour (P3)**: Response time > 2s or affecting non-critical endpoints
- **4 hours (P4)**: Minor performance degradation

---

## 3. Authentication & Authorization Decision Tree

### Initial Symptom: Users Cannot Login or Access Resources

```text
Authentication/Authorization Failure
│
├─ What type of failure?
│  │
│  ├─ Authentication Failure (Cannot login)
│  │  │
│  │  ├─ Is authentication service available?
│  │  │  ├─ NO → Service outage
│  │  │  │  ├─ Action: Check auth service health
│  │  │  │  ├─ Command: kubectl get pods -l app=auth-service
│  │  │  │  ├─ Fix: Restart service or scale up
│  │  │  │  ├─ Severity: P0 - Critical
│  │  │  │  └─ Escalate: Immediate → On-call
│  │  │  │
│  │  │  └─ YES → Authentication logic issue
│  │  │     │
│  │  │     ├─ JWT token expired?
│  │  │     │  ├─ YES → Token expiration issue
│  │  │     │  │  ├─ Check: Token TTL configuration
│  │  │     │  │  ├─ Check: Clock skew between services
│  │  │     │  │  ├─ Fix: Adjust TTL or implement refresh
│  │  │     │  │  ├─ Severity: P2 - Medium
│  │  │     │  │  └─ Escalate: If widespread → L2 Support
│  │  │     │  │
│  │  │     │  └─ NO → Continue
│  │  │     │
│  │  │     ├─ Invalid token signature?
│  │  │     │  ├─ YES → Secret mismatch
│  │  │     │  │  ├─ Check: JWT secret configuration
│  │  │     │  │  ├─ Check: Recent secret rotation
│  │  │     │  │  ├─ Fix: Synchronize secrets across services
│  │  │     │  │  ├─ Severity: P1 - High
│  │  │     │  │  └─ Escalate: Immediate → L2 Support
│  │  │     │  │
│  │  │     │  └─ NO → Continue
│  │  │     │
│  │  │     ├─ Database connectivity issue?
│  │  │     │  ├─ YES → Database problem
│  │  │     │  │  ├─ See: Database Decision Tree (#4)
│  │  │     │  │  └─ Severity: P1 - High
│  │  │     │  │
│  │  │     │  └─ NO → Continue
│  │  │     │
│  │  │     └─ Invalid credentials?
│  │  │        ├─ YES → User error or account issue
│  │  │        │  ├─ Check: Account status (locked/disabled)
│  │  │        │  ├─ Check: Failed login attempts
│  │  │        │  ├─ Action: Reset password if needed
│  │  │        │  └─ Severity: P4 - Low (individual user)
│  │  │        │
│  │  │        └─ NO → Unknown authentication issue
│  │  │           ├─ Action: Review authentication logs
│  │  │           ├─ Check: For error patterns
│  │  │           └─ Escalate: L2 Support
│  │
│  └─ Authorization Failure (Cannot access resource)
│     │
│     ├─ Is user authenticated?
│     │  ├─ NO → Authentication required first
│     │  │  └─ See: Authentication branch above
│     │  │
│     │  └─ YES → Authorization logic issue
│     │     │
│     │     ├─ Does user have required role?
│     │     │  ├─ NO → Missing role assignment
│     │     │  │  ├─ Check: User roles in database
│     │     │  │  ├─ Action: Assign appropriate role
│     │     │  │  ├─ Severity: P3 - Medium (individual)
│     │     │  │  └─ Escalate: If role system broken → L2
│     │     │  │
│     │     │  └─ YES → Continue
│     │     │
│     │     ├─ Does role have required permission?
│     │     │  ├─ NO → Missing permission
│     │     │  │  ├─ Check: Role permissions in database
│     │     │  │  ├─ Action: Grant permission to role
│     │     │  │  ├─ Severity: P3 - Medium
│     │     │  │  └─ Escalate: If RBAC broken → L2
│     │     │  │
│     │     │  └─ YES → Continue
│     │     │
│     │     ├─ Resource ownership validation failing?
│     │     │  ├─ YES → Ownership check issue
│     │     │  │  ├─ Check: Resource owner_id field
│     │     │  │  ├─ Check: User ID format (string vs UUID)
│     │     │  │  ├─ Fix: Clear ownership cache
│     │     │  │  ├─ Severity: P2 - Medium
│     │     │  │  └─ Escalate: If logic error → L2
│     │     │  │
│     │     │  └─ NO → Continue
│     │     │
│     │     └─ RBAC cache stale?
│     │        ├─ YES → Cache inconsistency
│     │        │  ├─ Action: Clear RBAC cache
│     │        │  ├─ Command: redis-cli DEL "rbac:cache:*"
│     │        │  ├─ Fix: Restart authorization service
│     │        │  └─ Severity: P3 - Medium
│     │        │
│     │        └─ NO → Unknown authorization issue
│     │           ├─ Action: Review authorization logs
│     │           ├─ Check: Audit trail
│     │           └─ Escalate: L2 Support
│     │
│     └─ Severity based on impact:
│        ├─ All users affected → P0/P1
│        ├─ Multiple users affected → P2
│        └─ Single user affected → P3/P4
│
└─ Resolution Verification:
   ├─ Users can successfully authenticate
   ├─ Users can access authorized resources
   ├─ No authentication errors in logs
   ├─ RBAC cache functioning correctly
   └─ Token validation working properly
```

### Quick Commands

```bash
# Check authentication service
kubectl get pods -l app=auth-service -n production
curl http://auth-service:8080/actuator/health

# Validate JWT token
curl -H "Authorization: Bearer ${TOKEN}" \
  http://localhost:8080/api/v1/auth/validate

# Check user roles
curl -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  http://localhost:8080/api/v1/users/${USER_ID}/roles

# Clear RBAC cache
kubectl exec -it ${POD_NAME} -n production -- \
  redis-cli DEL "rbac:cache:*"

# Check authentication logs
kubectl logs -l app=ecommerce-backend -n production | \
  grep "Authentication" | tail -50
```

### Escalation Criteria

- **Immediate (P0)**: All users cannot authenticate
- **15 minutes (P1)**: Authentication service down or widespread failures
- **1 hour (P2)**: Authorization issues affecting multiple users
- **4 hours (P3)**: Individual user access issues

---

## 4. Database Issues Decision Tree

### Initial Symptom: Database Errors or Slow Queries

```text
Database Issue Detected
│
├─ Can you connect to the database?
│  │
│  ├─ NO → Connection failure
│  │  │
│  │  ├─ Is database instance running?
│  │  │  ├─ NO → Database down
│  │  │  │  ├─ Check: RDS instance status
│  │  │  │  ├─ Command: aws rds describe-db-instances
│  │  │  │  ├─ Action: Start instance or failover
│  │  │  │  ├─ Severity: P0 - Critical
│  │  │  │  └─ Escalate: Immediate → On-call + AWS Support
│  │  │  │
│  │  │  └─ YES → Network or authentication issue
│  │  │     │
│  │  │     ├─ Security group blocking?
│  │  │     │  ├─ YES → Network security issue
│  │  │     │  │  ├─ Check: Security group rules
│  │  │     │  │  ├─ Fix: Add ingress rule for application
│  │  │     │  │  └─ Severity: P1 - High
│  │  │     │  │
│  │  │     │  └─ NO → Continue
│  │  │     │
│  │  │     ├─ Credentials incorrect?
│  │  │     │  ├─ YES → Authentication issue
│  │  │     │  │  ├─ Check: Database credentials in secrets
│  │  │     │  │  ├─ Fix: Update credentials
│  │  │     │  │  └─ Severity: P1 - High
│  │  │     │  │
│  │  │     │  └─ NO → Continue
│  │  │     │
│  │  │     └─ DNS resolution failing?
│  │  │        ├─ YES → DNS issue
│  │  │        │  ├─ See: Network Decision Tree (#5)
│  │  │        │  └─ Severity: P1 - High
│  │  │        │
│  │  │        └─ NO → Unknown connection issue
│  │  │           └─ Escalate: L2 Support
│  │  │
│  │  └─ Immediate Actions:
│  │     ├─ Alert on-call engineer
│  │     ├─ Check database health dashboard
│  │     └─ Prepare for failover if needed
│  │
│  └─ YES → Database accessible but having issues
│     │
│     ├─ What type of issue?
│     │  │
│     │  ├─ Connection pool exhausted
│     │  │  ├─ Symptoms: "Too many connections" error
│     │  │  ├─ Check: Current connections
│     │  │  │  ├─ Command: SELECT count(*) FROM pg_stat_activity;
│     │  │  │  └─ Check: Connection pool metrics
│     │  │  │
│     │  │  ├─ Are there idle connections?
│     │  │  │  ├─ YES → Connection leak
│     │  │  │  │  ├─ Action: Kill idle connections
│     │  │  │  │  ├─ Command: Terminate idle > 10 min
│     │  │  │  │  ├─ Fix: Increase pool size temporarily
│     │  │  │  │  ├─ Severity: P2 - Medium
│     │  │  │  │  └─ Escalate: If leak persists → L2
│     │  │  │  │
│     │  │  │  └─ NO → Genuine high load
│     │  │  │     ├─ Action: Increase max connections
│     │  │  │     ├─ Fix: Scale application or database
│     │  │  │     └─ Severity: P2 - Medium
│     │  │  │
│     │  │  └─ Resolution:
│     │  │     ├─ Connection pool < 80% utilized
│     │  │     └─ No connection errors
│     │  │
│     │  ├─ Slow queries
│     │  │  ├─ Symptoms: Query time > 100ms
│     │  │  ├─ Identify slow queries
│     │  │  │  ├─ Command: Check pg_stat_statements
│     │  │  │  └─ Analyze: Query execution plans
│     │  │  │
│     │  │  ├─ Missing indexes?
│     │  │  │  ├─ YES → Index optimization needed
│     │  │  │  │  ├─ Action: Analyze query patterns
│     │  │  │  │  ├─ Fix: CREATE INDEX CONCURRENTLY
│     │  │  │  │  ├─ Severity: P2 - Medium
│     │  │  │  │  └─ Escalate: If complex → L2
│     │  │  │  │
│     │  │  │  └─ NO → Continue
│     │  │  │
│     │  │  ├─ Table bloat?
│     │  │  │  ├─ YES → Maintenance needed
│     │  │  │  │  ├─ Action: Run VACUUM ANALYZE
│     │  │  │  │  ├─ Fix: Schedule regular maintenance
│     │  │  │  │  └─ Severity: P3 - Medium
│     │  │  │  │
│     │  │  │  └─ NO → Continue
│     │  │  │
│     │  │  ├─ Lock contention?
│     │  │  │  ├─ YES → Locking issue
│     │  │  │  │  ├─ Check: pg_locks for blocking queries
│     │  │  │  │  ├─ Action: Identify blocking query
│     │  │  │  │  ├─ Fix: Kill blocking query if needed
│     │  │  │  │  ├─ Severity: P2 - Medium
│     │  │  │  │  └─ Escalate: If deadlock → L2
│     │  │  │  │
│     │  │  │  └─ NO → Query optimization needed
│     │  │  │     ├─ Action: Rewrite query
│     │  │  │     └─ Escalate: L2 Support
│     │  │  │
│     │  │  └─ Resolution:
│     │  │     ├─ Query time < 100ms (95th percentile)
│     │  │     └─ No slow query alerts
│     │  │
│     │  ├─ High CPU usage
│     │  │  ├─ Symptoms: Database CPU > 80%
│     │  │  ├─ Check: Running queries
│     │  │  │  ├─ Command: SELECT * FROM pg_stat_activity
│     │  │  │  └─ Identify: CPU-intensive queries
│     │  │  │
│     │  │  ├─ Expensive queries running?
│     │  │  │  ├─ YES → Query optimization
│     │  │  │  │  ├─ Action: Optimize or kill query
│     │  │  │  │  ├─ Fix: Add indexes or rewrite
│     │  │  │  │  └─ Severity: P2 - Medium
│     │  │  │  │
│     │  │  │  └─ NO → High load
│     │  │  │     ├─ Action: Scale database instance
│     │  │  │     ├─ Fix: Add read replicas
│     │  │  │     └─ Severity: P2 - Medium
│     │  │  │
│     │  │  └─ Resolution:
│     │  │     ├─ CPU usage < 70%
│     │  │     └─ Query performance acceptable
│     │  │
│     │  ├─ Storage full
│     │  │  ├─ Symptoms: "Disk full" errors
│     │  │  ├─ Check: Storage metrics
│     │  │  │  ├─ Command: Check RDS storage
│     │  │  │  └─ Identify: Large tables
│     │  │  │
│     │  │  ├─ Can storage be increased?
│     │  │  │  ├─ YES → Increase storage
│     │  │  │  │  ├─ Action: Modify RDS instance
│     │  │  │  │  ├─ Severity: P1 - High
│     │  │  │  │  └─ Escalate: If at limit → L3
│     │  │  │  │
│     │  │  │  └─ NO → Data cleanup needed
│     │  │  │     ├─ Action: Archive old data
│     │  │  │     ├─ Fix: Implement data retention
│     │  │  │     └─ Escalate: L2 Support
│     │  │  │
│     │  │  └─ Resolution:
│     │  │     ├─ Storage usage < 80%
│     │  │     └─ No storage alerts
│     │  │
│     │  └─ Replication lag
│     │     ├─ Symptoms: Read replica behind primary
│     │     ├─ Check: Replication lag metrics
│     │     │  ├─ Command: Check RDS replication status
│     │     │  └─ Identify: Lag duration
│     │     │
│     │     ├─ Lag > 5 minutes?
│     │     │  ├─ YES → Significant lag
│     │     │  │  ├─ Check: Network between AZs
│     │     │  │  ├─ Check: Replica instance size
│     │     │  │  ├─ Fix: Scale replica or reduce load
│     │     │  │  ├─ Severity: P2 - Medium
│     │     │  │  └─ Escalate: If persistent → AWS Support
│     │     │  │
│     │     │  └─ NO → Acceptable lag
│     │     │     └─ Monitor for trends
│     │     │
│     │     └─ Resolution:
│     │        ├─ Replication lag < 1 minute
│     │        └─ Replica in sync
│     │
│     └─ Severity based on impact:
│        ├─ Database down → P0
│        ├─ Connection failures → P1
│        ├─ Slow queries affecting users → P2
│        └─ Minor performance issues → P3
│
└─ Resolution Verification:
   ├─ Database accessible and responsive
   ├─ Connection pool healthy (< 80%)
   ├─ Query performance acceptable (< 100ms)
   ├─ No lock contention
   └─ Resource utilization normal (< 70%)
```

### Quick Commands

```bash
# Check database connectivity
pg_isready -h ${DB_HOST}

# Check active connections
psql -c "SELECT count(*), state FROM pg_stat_activity GROUP BY state;"

# Check slow queries
psql -c "SELECT query, mean_time FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 10;"

# Check for locks
psql -c "SELECT * FROM pg_locks WHERE NOT granted;"

# Kill idle connections
psql -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE state = 'idle' AND query_start < NOW() - INTERVAL '10 minutes';"
```

### Escalation Criteria

- **Immediate (P0)**: Database completely unavailable
- **15 minutes (P1)**: Connection failures or critical query failures
- **1 hour (P2)**: Performance degradation affecting users
- **4 hours (P3)**: Minor performance issues or warnings

---

## 5. Network Connectivity Decision Tree

### Initial Symptom: Connection Timeouts or Network Errors

```text
Network Connectivity Issue
│
├─ What is failing to connect?
│  │
│  ├─ Pod to Pod communication
│  │  │
│  │  ├─ Can you ping the target pod?
│  │  │  ├─ NO → Network policy or routing issue
│  │  │  │  ├─ Check: Network policies
│  │  │  │  │  ├─ Command: kubectl get networkpolicies
│  │  │  │  │  └─ Review: Policy rules
│  │  │  │  │
│  │  │  │  ├─ Network policy blocking?
│  │  │  │  │  ├─ YES → Policy misconfiguration
│  │  │  │  │  │  ├─ Action: Review policy rules
│  │  │  │  │  │  ├─ Fix: Update network policy
│  │  │  │  │  │  ├─ Severity: P1 - High
│  │  │  │  │  │  └─ Escalate: If complex → L2
│  │  │  │  │  │
│  │  │  │  │  └─ NO → Routing issue
│  │  │  │  │     ├─ Check: Pod network configuration
│  │  │  │  │     ├─ Check: CNI plugin status
│  │  │  │  │     └─ Escalate: L1 Support (Infrastructure)
│  │  │  │  │
│  │  │  │  └─ Severity: P1 - High
│  │  │  │
│  │  │  └─ YES → Service or port issue
│  │  │     │
│  │  │     ├─ Is the service endpoint correct?
│  │  │     │  ├─ NO → Service misconfiguration
│  │  │     │  │  ├─ Check: Service definition
│  │  │     │  │  ├─ Check: Endpoint objects
│  │  │     │  │  ├─ Fix: Correct service selector
│  │  │     │  │  └─ Severity: P2 - Medium
│  │  │     │  │
│  │  │     │  └─ YES → Port or protocol issue
│  │  │     │     ├─ Check: Target port configuration
│  │  │     │     ├─ Check: Container port exposed
│  │  │     │     └─ Fix: Correct port mapping
│  │  │     │
│  │  │     └─ Resolution:
│  │  │        └─ Pod-to-pod communication working
│  │  │
│  │  └─ Immediate Actions:
│  │     ├─ Test with nc or curl
│  │     └─ Check service endpoints
│  │
│  ├─ Pod to External Service
│  │  │
│  │  ├─ Can you resolve DNS?
│  │  │  ├─ NO → DNS resolution failure
│  │  │  │  ├─ Check: CoreDNS status
│  │  │  │  │  ├─ Command: kubectl get pods -n kube-system -l k8s-app=kube-dns
│  │  │  │  │  └─ Check: CoreDNS logs
│  │  │  │  │
│  │  │  │  ├─ CoreDNS healthy?
│  │  │  │  │  ├─ NO → CoreDNS issue
│  │  │  │  │  │  ├─ Action: Restart CoreDNS
│  │  │  │  │  │  ├─ Command: kubectl rollout restart -n kube-system deployment/coredns
│  │  │  │  │  │  ├─ Severity: P1 - High
│  │  │  │  │  │  └─ Escalate: If persists → L1
│  │  │  │  │  │
│  │  │  │  │  └─ YES → DNS configuration issue
│  │  │  │  │     ├─ Check: /etc/resolv.conf in pod
│  │  │  │  │     ├─ Check: DNS service IP
│  │  │  │  │     └─ Escalate: L1 Support
│  │  │  │  │
│  │  │  │  └─ Severity: P1 - High
│  │  │  │
│  │  │  └─ YES → Network connectivity issue
│  │  │     │
│  │  │     ├─ Can you reach the IP directly?
│  │  │     │  ├─ NO → Network routing issue
│  │  │     │  │  ├─ Check: Security groups
│  │  │     │  │  │  ├─ Egress rules allow traffic?
│  │  │     │  │  │  │  ├─ NO → Security group blocking
│  │  │     │  │  │  │  │  ├─ Action: Add egress rule
│  │  │     │  │  │  │  │  ├─ Severity: P1 - High
│  │  │     │  │  │  │  │  └─ Escalate: If policy → L3
│  │  │     │  │  │  │  │
│  │  │     │  │  │  │  └─ YES → Continue
│  │  │     │  │  │  │
│  │  │     │  │  │  └─ Check: Network ACLs
│  │  │     │  │  │     ├─ ACLs blocking?
│  │  │     │  │  │     │  ├─ YES → NACL issue
│  │  │     │  │  │     │  │  ├─ Action: Update NACL rules
│  │  │     │  │  │     │  │  └─ Escalate: L1 Support
│  │  │     │  │  │     │  │
│  │  │     │  │  │     │  └─ NO → Routing issue
│  │  │     │  │  │     │     └─ Escalate: L1 Support
│  │  │     │  │  │     │
│  │  │     │  │  │     └─ Check: NAT Gateway
│  │  │     │  │  │        ├─ NAT Gateway healthy?
│  │  │     │  │  │        └─ Check: Route tables
│  │  │     │  │  │
│  │  │     │  │  └─ Severity: P1 - High
│  │  │     │  │
│  │  │     │  └─ YES → Port or firewall issue
│  │  │     │     ├─ Check: Target service port
│  │  │     │     ├─ Check: External firewall rules
│  │  │     │     └─ Escalate: Vendor support
│  │  │     │
│  │  │     └─ Resolution:
│  │  │        ├─ DNS resolution working
│  │  │        └─ External service accessible
│  │  │
│  │  └─ Immediate Actions:
│  │     ├─ Test DNS: nslookup <hostname>
│  │     ├─ Test connectivity: curl -v <url>
│  │     └─ Check security groups
│  │
│  ├─ External to Pod (Ingress)
│  │  │
│  │  ├─ Can you reach the load balancer?
│  │  │  ├─ NO → Load balancer issue
│  │  │  │  ├─ Check: ALB/NLB status
│  │  │  │  │  ├─ Command: aws elbv2 describe-load-balancers
│  │  │  │  │  └─ Check: Target health
│  │  │  │  │
│  │  │  │  ├─ Load balancer healthy?
│  │  │  │  │  ├─ NO → LB configuration issue
│  │  │  │  │  │  ├─ Check: Listener rules
│  │  │  │  │  │  ├─ Check: Target groups
│  │  │  │  │  │  ├─ Severity: P0 - Critical
│  │  │  │  │  │  └─ Escalate: Immediate → L1
│  │  │  │  │  │
│  │  │  │  │  └─ YES → Target health issue
│  │  │  │  │     ├─ Check: Target health status
│  │  │  │  │     ├─ Check: Health check configuration
│  │  │  │  │     └─ See: Service Availability (#1)
│  │  │  │  │
│  │  │  │  └─ Severity: P0 - Critical
│  │  │  │
│  │  │  └─ YES → Ingress routing issue
│  │  │     │
│  │  │     ├─ Is ingress configured correctly?
│  │  │     │  ├─ NO → Ingress misconfiguration
│  │  │     │  │  ├─ Check: Ingress rules
│  │  │     │  │  ├─ Check: Service backend
│  │  │     │  │  ├─ Fix: Correct ingress configuration
│  │  │     │  │  └─ Severity: P1 - High
│  │  │     │  │
│  │  │     │  └─ YES → WAF or security blocking
│  │  │     │     ├─ Check: WAF logs
│  │  │     │     ├─ Check: Security group ingress
│  │  │     │     └─ Fix: Adjust WAF rules or SG
│  │  │     │
│  │  │     └─ Resolution:
│  │  │        ├─ External access working
│  │  │        └─ Load balancer healthy
│  │  │
│  │  └─ Immediate Actions:
│  │     ├─ Check load balancer status
│  │     ├─ Check target health
│  │     └─ Review WAF logs
│  │
│  └─ TLS/SSL Issues
│     │
│     ├─ Certificate expired?
│     │  ├─ YES → Certificate renewal needed
│     │  │  ├─ Action: Renew certificate
│     │  │  ├─ Check: Certificate expiration date
│     │  │  ├─ Fix: Update certificate in ACM/ALB
│     │  │  ├─ Severity: P0 - Critical
│     │  │  └─ Escalate: Immediate → L1
│     │  │
│     │  └─ NO → Continue
│     │
│     ├─ Certificate mismatch?
│     │  ├─ YES → Wrong certificate
│     │  │  ├─ Check: Certificate domain
│     │  │  ├─ Fix: Use correct certificate
│     │  │  └─ Severity: P1 - High
│     │  │
│     │  └─ NO → Continue
│     │
│     └─ TLS version incompatible?
│        ├─ YES → TLS configuration issue
│        │  ├─ Check: Supported TLS versions
│        │  ├─ Fix: Update TLS policy
│        │  └─ Severity: P2 - Medium
│        │
│        └─ NO → Unknown TLS issue
│           └─ Escalate: L2 Support
│
└─ Resolution Verification:
   ├─ All network paths functional
   ├─ DNS resolution working
   ├─ No connection timeouts
   ├─ TLS/SSL certificates valid
   └─ Security groups configured correctly
```

### Quick Commands

```bash
# Test pod-to-pod connectivity
kubectl exec -it ${SOURCE_POD} -- nc -zv ${TARGET_POD_IP} ${PORT}

# Test DNS resolution
kubectl exec -it ${POD_NAME} -- nslookup ${HOSTNAME}

# Check network policies
kubectl get networkpolicies -n production

# Check service endpoints
kubectl get endpoints -n production

# Test external connectivity
kubectl exec -it ${POD_NAME} -- curl -v https://external-service.com

# Check CoreDNS
kubectl get pods -n kube-system -l k8s-app=kube-dns
kubectl logs -n kube-system -l k8s-app=kube-dns

# Check load balancer
aws elbv2 describe-load-balancers
aws elbv2 describe-target-health --target-group-arn ${TG_ARN}
```

### Escalation Criteria

- **Immediate (P0)**: Complete network outage or external access down
- **15 minutes (P1)**: Critical service connectivity issues
- **1 hour (P2)**: Intermittent connectivity or performance issues
- **4 hours (P3)**: Minor connectivity issues or warnings

---

## 6. Security Incident Decision Tree

### Initial Symptom: Security Alert or Suspicious Activity

```text
Security Incident Detected
│
├─ What type of security event?
│  │
│  ├─ Unauthorized Access Attempt
│  │  │
│  │  ├─ Multiple failed login attempts?
│  │  │  ├─ YES → Potential brute force attack
│  │  │  │  ├─ Check: Failed login count and pattern
│  │  │  │  │  ├─ Command: Check authentication logs
│  │  │  │  │  └─ Analyze: Source IPs and timing
│  │  │  │  │
│  │  │  │  ├─ From single IP?
│  │  │  │  │  ├─ YES → Targeted attack
│  │  │  │  │  │  ├─ Action: Block IP immediately
│  │  │  │  │  │  ├─ Command: Update WAF IP blocklist
│  │  │  │  │  │  ├─ Severity: P1 - High
│  │  │  │  │  │  └─ Escalate: Security Team
│  │  │  │  │  │
│  │  │  │  │  └─ NO → Distributed attack
│  │  │  │  │     ├─ Action: Enable rate limiting
│  │  │  │  │     ├─ Action: Implement CAPTCHA
│  │  │  │  │     ├─ Severity: P0 - Critical
│  │  │  │  │     └─ Escalate: Immediate → Security Team
│  │  │  │  │
│  │  │  │  └─ Immediate Actions:
│  │  │  │     ├─ Block malicious IPs
│  │  │  │     ├─ Enable account lockout
│  │  │  │     ├─ Alert security team
│  │  │  │     └─ Document incident
│  │  │  │
│  │  │  └─ NO → Continue investigation
│  │  │
│  │  ├─ Privilege escalation attempt?
│  │  │  ├─ YES → Critical security incident
│  │  │  │  ├─ Check: Role change audit logs
│  │  │  │  ├─ Check: Permission grant logs
│  │  │  │  ├─ Action: Disable affected account
│  │  │  │  ├─ Action: Revoke elevated permissions
│  │  │  │  ├─ Severity: P0 - Critical
│  │  │  │  └─ Escalate: Immediate → Security Team + CTO
│  │  │  │
│  │  │  └─ NO → Continue
│  │  │
│  │  └─ Unusual access patterns?
│  │     ├─ YES → Potential account compromise
│  │     │  ├─ Check: Access from unusual location
│  │     │  ├─ Check: Access at unusual time
│  │     │  ├─ Check: Unusual API usage pattern
│  │     │  │
│  │     │  ├─ Account likely compromised?
│  │     │  │  ├─ YES → Account compromise
│  │     │  │  │  ├─ Action: Disable account immediately
│  │     │  │  │  ├─ Action: Invalidate all sessions
│  │     │  │  │  ├─ Action: Force password reset
│  │     │  │  │  ├─ Action: Review access logs
│  │     │  │  │  ├─ Severity: P0 - Critical
│  │     │  │  │  └─ Escalate: Immediate → Security Team
│  │     │  │  │
│  │     │  │  └─ NO → Enhanced monitoring
│  │     │  │     ├─ Action: Enable detailed logging
│  │     │  │     ├─ Action: Require MFA
│  │     │  │     └─ Severity: P2 - Medium
│  │     │  │
│  │     │  └─ Resolution:
│  │     │     ├─ Account secured
│  │     │     ├─ Credentials reset
│  │     │     └─ Monitoring enabled
│  │     │
│  │     └─ NO → False alarm
│  │        └─ Document and close
│  │
│  ├─ Data Exfiltration Attempt
│  │  │
│  │  ├─ Large data transfer detected?
│  │  │  ├─ YES → Potential data breach
│  │  │  │  ├─ Check: Data volume transferred
│  │  │  │  ├─ Check: Destination of transfer
│  │  │  │  ├─ Check: User authorization level
│  │  │  │  │
│  │  │  │  ├─ Unauthorized transfer?
│  │  │  │  │  ├─ YES → Active data breach
│  │  │  │  │  │  ├─ Action: Block user immediately
│  │  │  │  │  │  ├─ Action: Revoke API keys
│  │  │  │  │  │  ├─ Action: Isolate affected systems
│  │  │  │  │  │  ├─ Action: Preserve evidence
│  │  │  │  │  │  ├─ Severity: P0 - Critical
│  │  │  │  │  │  └─ Escalate: Immediate → Security Team + CTO + Legal
│  │  │  │  │  │
│  │  │  │  │  └─ NO → Legitimate bulk operation
│  │  │  │  │     ├─ Action: Verify with user
│  │  │  │  │     └─ Severity: P3 - Low
│  │  │  │  │
│  │  │  │  └─ Immediate Actions:
│  │  │  │     ├─ Preserve all logs
│  │  │  │     ├─ Document timeline
│  │  │  │     ├─ Identify affected data
│  │  │  │     └─ Notify stakeholders
│  │  │  │
│  │  │  └─ NO → Continue monitoring
│  │  │
│  │  ├─ Bulk database queries?
│  │  │  ├─ YES → Potential data scraping
│  │  │  │  ├─ Check: Query patterns
│  │  │  │  ├─ Check: Rows returned
│  │  │  │  ├─ Action: Rate limit queries
│  │  │  │  ├─ Severity: P1 - High
│  │  │  │  └─ Escalate: Security Team
│  │  │  │
│  │  │  └─ NO → Continue
│  │  │
│  │  └─ Unusual export operations?
│  │     ├─ YES → Investigate exports
│  │     │  ├─ Check: Export audit logs
│  │     │  ├─ Check: User authorization
│  │     │  └─ Action: Review and block if needed
│  │     │
│  │     └─ NO → Normal operations
│  │
│  ├─ DDoS Attack
│  │  │
│  │  ├─ Sudden traffic spike?
│  │  │  ├─ YES → Potential DDoS
│  │  │  │  ├─ Check: Request rate
│  │  │  │  ├─ Check: Source distribution
│  │  │  │  ├─ Check: Request patterns
│  │  │  │  │
│  │  │  │  ├─ Distributed sources?
│  │  │  │  │  ├─ YES → DDoS attack
│  │  │  │  │  │  ├─ Action: Enable AWS Shield
│  │  │  │  │  │  ├─ Action: Activate WAF rate limiting
│  │  │  │  │  │  ├─ Action: Enable geo-blocking if needed
│  │  │  │  │  │  ├─ Action: Contact AWS Support
│  │  │  │  │  │  ├─ Severity: P0 - Critical
│  │  │  │  │  │  └─ Escalate: Immediate → Security + AWS Support
│  │  │  │  │  │
│  │  │  │  │  └─ NO → Single source attack
│  │  │  │  │     ├─ Action: Block source IP
│  │  │  │  │     ├─ Severity: P1 - High
│  │  │  │  │     └─ Escalate: Security Team
│  │  │  │  │
│  │  │  │  └─ Immediate Actions:
│  │  │  │     ├─ Enable rate limiting
│  │  │  │     ├─ Scale infrastructure
│  │  │  │     ├─ Monitor service health
│  │  │  │     └─ Document attack patterns
│  │  │  │
│  │  │  └─ NO → Legitimate traffic spike
│  │  │     └─ Monitor and scale as needed
│  │  │
│  │  └─ Service degradation?
│  │     ├─ YES → See Performance Tree (#2)
│  │     └─ NO → Continue monitoring
│  │
│  ├─ Malware or Injection Attack
│  │  │
│  │  ├─ SQL injection attempt?
│  │  │  ├─ YES → Injection attack
│  │  │  │  ├─ Check: WAF logs for patterns
│  │  │  │  ├─ Action: Block source IP
│  │  │  │  ├─ Action: Review query logs
│  │  │  │  ├─ Action: Verify no data breach
│  │  │  │  ├─ Severity: P1 - High
│  │  │  │  └─ Escalate: Security Team
│  │  │  │
│  │  │  └─ NO → Continue
│  │  │
│  │  ├─ XSS attempt detected?
│  │  │  ├─ YES → XSS attack
│  │  │  │  ├─ Check: Input validation logs
│  │  │  │  ├─ Action: Block malicious input
│  │  │  │  ├─ Action: Review affected pages
│  │  │  │  ├─ Severity: P1 - High
│  │  │  │  └─ Escalate: Security Team
│  │  │  │
│  │  │  └─ NO → Continue
│  │  │
│  │  └─ Malicious file upload?
│  │     ├─ YES → Malware upload attempt
│  │     │  ├─ Action: Quarantine file
│  │     │  ├─ Action: Scan with antivirus
│  │     │  ├─ Action: Block user
│  │     │  ├─ Severity: P0 - Critical
│  │     │  └─ Escalate: Immediate → Security Team
│  │     │
│  │     └─ NO → Continue
│  │
│  └─ Configuration Vulnerability
│     │
│     ├─ Exposed credentials?
│     │  ├─ YES → Credential exposure
│     │  │  ├─ Action: Rotate credentials immediately
│     │  │  ├─ Action: Revoke exposed credentials
│     │  │  ├─ Action: Audit access logs
│     │  │  ├─ Severity: P0 - Critical
│     │  │  └─ Escalate: Immediate → Security Team
│     │  │
│     │  └─ NO → Continue
│     │
│     ├─ Open security group?
│     │  ├─ YES → Security misconfiguration
│     │  │  ├─ Action: Restrict security group
│     │  │  ├─ Action: Review access logs
│     │  │  ├─ Severity: P1 - High
│     │  │  └─ Escalate: Security Team
│     │  │
│     │  └─ NO → Continue
│     │
│     └─ Unpatched vulnerability?
│        ├─ YES → Security patch needed
│        │  ├─ Action: Apply security patch
│        │  ├─ Action: Scan for exploitation
│        │  ├─ Severity: P1 - High
│        │  └─ Escalate: Security Team
│        │
│        └─ NO → Continue monitoring
│
└─ Resolution Verification:
   ├─ Threat neutralized
   ├─ Affected accounts secured
   ├─ Credentials rotated if needed
   ├─ Security controls updated
   ├─ Incident documented
   └─ Post-incident review scheduled
```

### Quick Commands

```bash
# Check authentication failures
kubectl logs -l app=ecommerce-backend -n production | \
  grep "Authentication failed" | tail -100

# Check for suspicious IPs
kubectl logs -l app=ecommerce-backend -n production | \
  grep "403\|401" | awk '{print $3}' | sort | uniq -c | sort -rn

# Block IP in WAF
aws wafv2 update-ip-set \
  --id ${IP_SET_ID} \
  --addresses ${MALICIOUS_IP}/32

# Disable user account
curl -X POST http://localhost:8080/api/v1/admin/users/${USER_ID}/disable

# Check security audit logs
kubectl logs -l app=ecommerce-backend -n production | \
  grep "SECURITY" | tail -100
```

### Escalation Criteria

- **Immediate (P0)**: Active breach, data exfiltration, DDoS attack
- **15 minutes (P1)**: Unauthorized access, injection attempts, credential exposure
- **1 hour (P2)**: Suspicious activity, failed attacks, security warnings
- **4 hours (P3)**: Minor security events, false positives

### Incident Response Team

- **Security Team**: <security@company.com> (PagerDuty: Security On-Call)
- **CTO**: For P0 incidents
- **Legal**: For data breach incidents
- **AWS Support**: For infrastructure attacks

---

## 7. Deployment Failure Decision Tree

### Initial Symptom: Deployment Not Completing Successfully

```text
Deployment Failure
│
├─ What is the deployment status?
│  │
│  ├─ Pods not starting (Pending/CrashLoopBackOff)
│  │  └─ See: Service Availability Tree (#1)
│  │
│  ├─ Rollout stuck/progressing slowly
│  │  │
│  │  ├─ Check rollout status
│  │  │  ├─ Command: kubectl rollout status deployment/${NAME}
│  │  │  └─ Check: Recent events
│  │  │
│  │  ├─ New pods failing health checks?
│  │  │  ├─ YES → Health check failure
│  │  │  │  ├─ Check: Readiness probe configuration
│  │  │  │  ├─ Check: Application startup time
│  │  │  │  ├─ Fix: Adjust probe timing or fix app
│  │  │  │  ├─ Severity: P1 - High
│  │  │  │  └─ Escalate: If app issue → L2
│  │  │  │
│  │  │  └─ NO → Continue
│  │  │
│  │  ├─ Insufficient resources?
│  │  │  ├─ YES → Resource constraints
│  │  │  │  ├─ Check: Node capacity
│  │  │  │  ├─ Fix: Scale cluster or adjust requests
│  │  │  │  └─ Severity: P2 - Medium
│  │  │  │
│  │  │  └─ NO → Continue
│  │  │
│  │  └─ Rollout strategy issue?
│  │     ├─ Check: Max unavailable setting
│  │     ├─ Check: Max surge setting
│  │     └─ Fix: Adjust rollout parameters
│  │
│  ├─ Image pull failures
│  │  │
│  │  ├─ Image exists in registry?
│  │  │  ├─ NO → Wrong image tag
│  │  │  │  ├─ Check: Image tag in deployment
│  │  │  │  ├─ Fix: Use correct tag
│  │  │  │  └─ Severity: P1 - High
│  │  │  │
│  │  │  └─ YES → Authentication issue
│  │  │     ├─ Check: Image pull secret
│  │  │     ├─ Fix: Update secret
│  │  │     └─ Severity: P1 - High
│  │  │
│  │  └─ Immediate Action:
│  │     └─ Rollback: kubectl rollout undo
│  │
│  └─ Configuration errors
│     │
│     ├─ ConfigMap/Secret missing?
│     │  ├─ YES → Missing configuration
│     │  │  ├─ Check: Required configs
│     │  │  ├─ Fix: Create missing resources
│     │  │  └─ Severity: P1 - High
│     │  │
│     │  └─ NO → Continue
│     │
│     └─ Environment variable errors?
│        ├─ YES → Configuration issue
│        │  ├─ Check: Application logs
│        │  ├─ Fix: Correct configuration
│        │  └─ Severity: P1 - High
│        │
│        └─ NO → Unknown issue
│           └─ Escalate: L2 Support
│
└─ Resolution Verification:
   ├─ All pods running and ready
   ├─ Rollout completed successfully
   ├─ Health checks passing
   └─ No errors in logs
```

### Quick Commands

```bash
# Check deployment status
kubectl rollout status deployment/${NAME} -n production

# Check pod events
kubectl describe pod ${POD_NAME} -n production

# Rollback deployment
kubectl rollout undo deployment/${NAME} -n production

# Check rollout history
kubectl rollout history deployment/${NAME} -n production
```

### Escalation Criteria

- **Immediate (P1)**: Production deployment failed, service down
- **15 minutes (P2)**: Deployment stuck, partial rollout
- **1 hour (P3)**: Slow deployment, minor issues

---

## 8. Cache Issues Decision Tree

### Initial Symptom: Cache Performance Problems

```text
Cache Issue Detected
│
├─ What is the cache hit rate?
│  │
│  ├─ < 50% → Critical cache problem
│  │  ├─ Severity: P1 - High
│  │  └─ Immediate investigation needed
│  │
│  ├─ 50-70% → Poor cache performance
│  │  ├─ Severity: P2 - Medium
│  │  └─ Optimization needed
│  │
│  └─ > 70% → Acceptable (but investigate if degrading)
│     └─ Monitor trends
│
├─ Is Redis/cache service available?
│  │
│  ├─ NO → Cache service down
│  │  ├─ Check: Redis pod status
│  │  ├─ Action: Restart Redis
│  │  ├─ Severity: P1 - High
│  │  └─ Escalate: If persists → L1
│  │
│  └─ YES → Cache logic issue
│     │
│     ├─ High eviction rate?
│     │  ├─ YES → Memory pressure
│     │  │  ├─ Check: Redis memory usage
│     │  │  ├─ Check: Eviction policy
│     │  │  ├─ Fix: Increase memory or adjust TTL
│     │  │  └─ Severity: P2 - Medium
│     │  │
│     │  └─ NO → Continue
│     │
│     ├─ Cache keys not found?
│     │  ├─ YES → Cache warming needed
│     │  │  ├─ Action: Warm cache with common data
│     │  │  ├─ Check: Cache key patterns
│     │  │  └─ Severity: P3 - Medium
│     │  │
│     │  └─ NO → Continue
│     │
│     └─ TTL too short?
│        ├─ YES → Configuration issue
│        │  ├─ Action: Adjust TTL values
│        │  └─ Severity: P3 - Low
│        │
│        └─ NO → Cache strategy review needed
│           └─ Escalate: L2 Support
│
└─ Resolution Verification:
   ├─ Cache hit rate > 80%
   ├─ Eviction rate acceptable
   ├─ Redis healthy and responsive
   └─ Application performance improved
```

### Quick Commands

```bash
# Check cache hit rate
curl http://localhost:8080/actuator/metrics/cache.gets | jq

# Check Redis status
kubectl exec -it redis-0 -n production -- redis-cli INFO stats

# Warm cache
curl -X POST http://localhost:8080/admin/cache/warm

# Clear cache
kubectl exec -it redis-0 -n production -- redis-cli FLUSHDB
```

---

## 9. Message Queue Issues Decision Tree

### Initial Symptom: Message Processing Problems

```text
Message Queue Issue
│
├─ Is Kafka/MSK accessible?
│  │
│  ├─ NO → Broker unavailable
│  │  ├─ Check: Kafka broker status
│  │  ├─ Check: MSK cluster health
│  │  ├─ Severity: P0 - Critical
│  │  └─ Escalate: Immediate → L1 + AWS Support
│  │
│  └─ YES → Message processing issue
│     │
│     ├─ High consumer lag?
│     │  ├─ YES → Processing bottleneck
│     │  │  ├─ Check: Consumer group lag
│     │  │  ├─ Check: Processing rate
│     │  │  │
│     │  │  ├─ Consumers slow?
│     │  │  │  ├─ YES → Performance issue
│     │  │  │  │  ├─ Action: Scale consumers
│     │  │  │  │  ├─ Check: Consumer errors
│     │  │  │  │  └─ Severity: P2 - Medium
│     │  │  │  │
│     │  │  │  └─ NO → High message volume
│     │  │  │     ├─ Action: Scale consumers
│     │  │  │     └─ Severity: P2 - Medium
│     │  │  │
│     │  │  └─ Resolution:
│     │  │     └─ Lag < 1000 messages
│     │  │
│     │  └─ NO → Continue
│     │
│     ├─ Messages not being consumed?
│     │  ├─ YES → Consumer issue
│     │  │  ├─ Check: Consumer status
│     │  │  ├─ Check: Consumer errors
│     │  │  ├─ Fix: Restart consumers
│     │  │  └─ Severity: P1 - High
│     │  │
│     │  └─ NO → Continue
│     │
│     └─ Dead letter queue filling?
│        ├─ YES → Message processing failures
│        │  ├─ Check: Error patterns
│        │  ├─ Fix: Resolve processing errors
│        │  └─ Severity: P2 - Medium
│        │
│        └─ NO → Normal operations
│
└─ Resolution Verification:
   ├─ Consumer lag acceptable
   ├─ Messages processing successfully
   ├─ No errors in consumer logs
   └─ DLQ not accumulating
```

### Quick Commands

```bash
# Check consumer lag
kubectl exec -it kafka-0 -- \
  kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --group ${GROUP_NAME}

# Check topic status
kubectl exec -it kafka-0 -- \
  kafka-topics --bootstrap-server localhost:9092 --describe

# Scale consumers
kubectl scale deployment/consumer --replicas=5
```

---

## 10. Resource Exhaustion Decision Tree

### Initial Symptom: Resource Limits Reached

```text
Resource Exhaustion
│
├─ What resource is exhausted?
│  │
│  ├─ CPU > 90%
│  │  ├─ Check: CPU-intensive processes
│  │  ├─ Action: Scale horizontally
│  │  ├─ Severity: P1 - High
│  │  └─ See: Performance Tree (#2)
│  │
│  ├─ Memory > 90%
│  │  ├─ Check: Memory leaks
│  │  ├─ Action: Restart or scale
│  │  ├─ Severity: P1 - High
│  │  └─ See: Performance Tree (#2)
│  │
│  ├─ Disk > 85%
│  │  ├─ Check: Log files
│  │  ├─ Action: Clean up or expand
│  │  ├─ Severity: P1 - High
│  │  └─ Escalate: L1 Support
│  │
│  ├─ File descriptors exhausted
│  │  ├─ Check: Open file count
│  │  ├─ Action: Increase limits
│  │  ├─ Severity: P1 - High
│  │  └─ Escalate: L1 Support
│  │
│  └─ Connection pool exhausted
│     ├─ See: Database Tree (#4)
│     └─ Severity: P1 - High
│
└─ Resolution Verification:
   ├─ Resource utilization < 70%
   ├─ No resource warnings
   └─ System stable
```

---

## Summary and Best Practices

### Using Decision Trees Effectively

1. **Start at the top**: Always begin with the initial symptom
2. **Answer honestly**: Base decisions on actual data, not assumptions
3. **Document findings**: Record what you checked and found
4. **Follow escalation**: Don't hesitate to escalate when criteria met
5. **Verify resolution**: Always confirm the issue is resolved

### Common Patterns

- **Check availability first**: Is the service/component accessible?
- **Check recent changes**: Did a deployment or configuration change trigger this?
- **Check resources**: Are CPU/memory/disk/network adequate?
- **Check dependencies**: Are external services healthy?
- **Check logs**: What do the logs say about the issue?

### Escalation Guidelines

- **P0 (Critical)**: Escalate immediately, engage multiple teams
- **P1 (High)**: Escalate within 15 minutes if not resolved
- **P2 (Medium)**: Escalate within 1 hour if not progressing
- **P3 (Low)**: Escalate within 4 hours if needed
- **P4 (Info)**: Handle during regular sprint planning

### Documentation Requirements

For every incident, document:

- Initial symptom and alert
- Decision tree path followed
- Commands executed and results
- Actions taken
- Resolution steps
- Root cause (if identified)
- Prevention measures

---

## Related Documentation

- [Common Issues Guide](common-issues.md) - Quick fixes for common problems
- [Performance Degradation Guide](performance-degradation.md) - Detailed performance troubleshooting
- [Security Incidents Guide](security-incidents.md) - Security incident procedures
- [Database Issues Guide](database-issues.md) - Database-specific troubleshooting
- [Network Connectivity Guide](network-connectivity.md) - Network troubleshooting
- [Runbooks](../runbooks/README.md) - Step-by-step operational procedures
- [Monitoring Strategy](../monitoring/monitoring-strategy.md) - Monitoring and alerting setup

---

**Last Updated**: 2025-10-26  
**Owner**: DevOps Team  
**Review Cycle**: Quarterly  
**Feedback**: <devops@company.com>
