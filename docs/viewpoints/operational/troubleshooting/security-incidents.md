# Security Incident Troubleshooting

## Overview

This document provides troubleshooting procedures for security-related incidents in the Enterprise E-Commerce Platform, including authentication failures, authorization issues, network security problems, and suspicious activity investigation.

## Authentication Issues

### Issue: JWT Token Expiration

**Symptoms**:

- Users getting logged out unexpectedly
- "Token expired" errors in API responses
- 401 Unauthorized errors after period of inactivity

**Quick Diagnosis**:

```bash
# Check token expiration time in logs
kubectl logs -l app=ecommerce-backend -n production | grep "JWT.*expired"

# Verify JWT configuration
kubectl get configmap jwt-config -n production -o yaml

# Check current token validity
curl -H "Authorization: Bearer ${TOKEN}" \
  http://localhost:8080/api/v1/auth/validate
```

**Common Causes**:

1. Token TTL too short (< 1 hour)
2. Clock skew between services
3. Token refresh mechanism not working
4. Session timeout configuration mismatch

**Quick Fix**:

```bash
# Increase token TTL (temporary)
kubectl set env deployment/ecommerce-backend \
  JWT_EXPIRATION_TIME=3600 -n production

# Restart pods to apply changes
kubectl rollout restart deployment/ecommerce-backend -n production
```

**Permanent Solution**:

1. Review and adjust token TTL in configuration
2. Implement proper token refresh mechanism
3. Add sliding session expiration
4. Configure refresh token rotation

---

### Issue: Invalid Token Signature

**Symptoms**:

- "Invalid signature" errors
- Authentication failures after deployment
- Intermittent 401 errors

**Quick Diagnosis**:

```bash
# Check JWT secret configuration
kubectl get secret jwt-secret -n production -o jsonpath='{.data.secret}' | base64 -d

# Verify token signing algorithm
kubectl logs -l app=ecommerce-backend -n production | grep "JWT.*algorithm"

# Check for multiple JWT secrets (rolling update issue)
kubectl get pods -n production -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.spec.containers[0].env[?(@.name=="JWT_SECRET")].valueFrom.secretKeyRef.name}{"\n"}{end}'
```

**Common Causes**:

1. JWT secret changed without invalidating old tokens
2. Multiple services using different secrets
3. Secret rotation not synchronized
4. Incorrect signing algorithm configuration

**Quick Fix**:

```bash
# Force all users to re-authenticate
kubectl delete secret jwt-secret -n production
kubectl create secret generic jwt-secret \
  --from-literal=secret=$(openssl rand -base64 32) \
  -n production

# Restart all services
kubectl rollout restart deployment/ecommerce-backend -n production
```

**Permanent Solution**:

1. Implement proper secret rotation strategy
2. Use centralized secret management (AWS Secrets Manager)
3. Coordinate secret updates across all services
4. Add grace period for old secrets during rotation

---

### Issue: Authentication Service Unavailable

**Symptoms**:

- All login attempts failing
- "Service unavailable" errors
- Authentication endpoint timing out

**Quick Diagnosis**:

```bash
# Check authentication service health
kubectl get pods -n production -l app=auth-service

# Test authentication endpoint
curl -X POST http://auth-service:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}'

# Check service logs
kubectl logs -l app=auth-service -n production --tail=100
```

**Quick Fix**:

```bash
# Restart authentication service
kubectl rollout restart deployment/auth-service -n production

# Scale up if needed
kubectl scale deployment/auth-service --replicas=3 -n production
```

**Related Runbook**: [Service Outage Runbook](../runbooks/service-outage.md)

---

## Authorization Issues

### Issue: Permission Denied (RBAC)

**Symptoms**:

- 403 Forbidden errors
- "Access denied" messages
- Users unable to access resources they should have access to

**Quick Diagnosis**:

```bash
# Check user roles and permissions
kubectl logs -l app=ecommerce-backend -n production | grep "Access denied.*user=${USER_ID}"

# Verify RBAC configuration
kubectl get configmap rbac-config -n production -o yaml

# Check user's assigned roles
curl -H "Authorization: Bearer ${TOKEN}" \
  http://localhost:8080/api/v1/users/${USER_ID}/roles
```

**Common Causes**:

1. Role not assigned to user
2. Permission not granted to role
3. Resource ownership mismatch
4. RBAC policy cache not updated

**Quick Fix**:

```bash
# Clear RBAC cache
kubectl exec -it ${POD_NAME} -n production -- \
  redis-cli DEL "rbac:cache:*"

# Restart authorization service
kubectl rollout restart deployment/ecommerce-backend -n production
```

**Investigation Steps**:

1. Verify user's roles in database:

```sql
SELECT u.id, u.email, r.name as role
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
WHERE u.id = '${USER_ID}';
```

1. Check role permissions:

```sql
SELECT r.name as role, p.resource, p.action
FROM roles r
JOIN role_permissions rp ON r.id = rp.role_id
JOIN permissions p ON rp.permission_id = p.id
WHERE r.name = '${ROLE_NAME}';
```

1. Review audit logs:

```bash
kubectl logs -l app=ecommerce-backend -n production | \
  grep "Authorization.*${USER_ID}" | tail -50
```

**Permanent Solution**:

1. Review and update RBAC policies
2. Implement proper role hierarchy
3. Add permission inheritance
4. Document role-permission mappings

---

### Issue: Resource Ownership Validation Failure

**Symptoms**:

- Users can't access their own resources
- "Not authorized to access this resource" errors
- Ownership checks failing incorrectly

**Quick Diagnosis**:

```bash
# Check resource ownership in database
psql -c "SELECT id, owner_id, created_by FROM orders WHERE id = '${ORDER_ID}';"

# Verify ownership validation logic in logs
kubectl logs -l app=ecommerce-backend -n production | \
  grep "Ownership validation.*${ORDER_ID}"
```

**Common Causes**:

1. User ID mismatch (string vs UUID)
2. Ownership field not populated
3. Delegation/sharing not working
4. Cache inconsistency

**Quick Fix**:

```bash
# Clear ownership cache
kubectl exec -it ${POD_NAME} -n production -- \
  redis-cli DEL "ownership:*"
```

---

## Network Security Issues

### Issue: Security Group Blocking Traffic

**Symptoms**:

- Connection timeouts to specific services
- Intermittent connectivity issues
- "Connection refused" errors

**Quick Diagnosis**:

```bash
# Check security group rules
aws ec2 describe-security-groups \
  --group-ids ${SECURITY_GROUP_ID} \
  --query 'SecurityGroups[0].IpPermissions'

# Test connectivity from pod
kubectl exec -it ${POD_NAME} -n production -- \
  nc -zv ${TARGET_HOST} ${TARGET_PORT}

# Check VPC flow logs
aws ec2 describe-flow-logs \
  --filter "Name=resource-id,Values=${VPC_ID}"
```

**Common Causes**:

1. Missing ingress/egress rules
2. Wrong CIDR blocks
3. Port mismatch
4. Security group not attached to resource

**Quick Fix**:

```bash
# Add temporary rule (be specific!)
aws ec2 authorize-security-group-ingress \
  --group-id ${SECURITY_GROUP_ID} \
  --protocol tcp \
  --port ${PORT} \
  --cidr ${SOURCE_CIDR}
```

**Investigation Steps**:

1. Review VPC flow logs for rejected connections:

```bash
aws logs filter-log-events \
  --log-group-name /aws/vpc/flowlogs \
  --filter-pattern "[version, account, eni, source, destination, srcport, destport, protocol, packets, bytes, start, end, action=REJECT, status]" \
  --start-time $(date -u -d '1 hour ago' +%s)000
```

1. Verify network ACLs:

```bash
aws ec2 describe-network-acls \
  --filters "Name=vpc-id,Values=${VPC_ID}"
```

**Permanent Solution**:

1. Document required security group rules
2. Use Infrastructure as Code (CDK) for security groups
3. Implement least privilege access
4. Regular security group audits

---

### Issue: Network ACL Blocking Traffic

**Symptoms**:

- Entire subnet connectivity issues
- Both inbound and outbound traffic affected
- Security groups correct but traffic still blocked

**Quick Diagnosis**:

```bash
# Check Network ACL rules
aws ec2 describe-network-acls \
  --filters "Name=association.subnet-id,Values=${SUBNET_ID}"

# Check rule evaluation order
aws ec2 describe-network-acls \
  --network-acl-ids ${NACL_ID} \
  --query 'NetworkAcls[0].Entries' \
  --output table
```

**Common Causes**:

1. Deny rule with lower number (higher priority)
2. Missing allow rules for ephemeral ports
3. Stateless nature of NACLs not considered
4. Rule number conflicts

**Quick Fix**:

```bash
# Add allow rule (use appropriate rule number)
aws ec2 create-network-acl-entry \
  --network-acl-id ${NACL_ID} \
  --rule-number 100 \
  --protocol tcp \
  --port-range From=${PORT},To=${PORT} \
  --cidr-block ${CIDR} \
  --egress \
  --rule-action allow
```

---

### Issue: WAF Blocking Legitimate Traffic

**Symptoms**:

- 403 Forbidden from CloudFront/ALB
- Legitimate requests being blocked
- "Request blocked by WAF" in logs

**Quick Diagnosis**:

```bash
# Check WAF logs
aws wafv2 get-sampled-requests \
  --web-acl-arn ${WEB_ACL_ARN} \
  --rule-metric-name ${RULE_NAME} \
  --scope REGIONAL \
  --time-window StartTime=$(date -u -d '1 hour ago' +%s),EndTime=$(date -u +%s) \
  --max-items 100

# Check which rule is blocking
aws logs filter-log-events \
  --log-group-name aws-waf-logs-${WEB_ACL_NAME} \
  --filter-pattern '{ $.action = "BLOCK" }' \
  --start-time $(date -u -d '1 hour ago' +%s)000
```

**Common Causes**:

1. Rate limiting too aggressive
2. Geo-blocking legitimate users
3. SQL injection rule false positive
4. XSS protection blocking valid input

**Quick Fix**:

```bash
# Temporarily disable problematic rule
aws wafv2 update-web-acl \
  --id ${WEB_ACL_ID} \
  --scope REGIONAL \
  --lock-token ${LOCK_TOKEN} \
  --rules file://updated-rules.json

# Add IP to allowlist
aws wafv2 update-ip-set \
  --id ${IP_SET_ID} \
  --scope REGIONAL \
  --addresses ${IP_ADDRESS}/32 \
  --lock-token ${LOCK_TOKEN}
```

**Investigation Steps**:

1. Analyze blocked requests:

```bash
# Get detailed WAF logs
aws logs get-log-events \
  --log-group-name aws-waf-logs-${WEB_ACL_NAME} \
  --log-stream-name ${LOG_STREAM} \
  --start-time $(date -u -d '1 hour ago' +%s)000 | \
  jq '.events[] | select(.message | contains("BLOCK"))'
```

1. Review rule match details:

```bash
# Check which rule matched
aws wafv2 get-sampled-requests \
  --web-acl-arn ${WEB_ACL_ARN} \
  --rule-metric-name ${RULE_NAME} \
  --scope REGIONAL \
  --time-window StartTime=$(date -u -d '1 hour ago' +%s),EndTime=$(date -u +%s) \
  --max-items 100 | \
  jq '.SampledRequests[] | {uri: .Request.URI, action: .Action, ruleWithinRuleGroup: .RuleNameWithinRuleGroup}'
```

**Permanent Solution**:

1. Fine-tune WAF rules based on traffic patterns
2. Implement custom rules for known false positives
3. Use count mode before blocking
4. Regular review of blocked requests

---

## DDoS Attack Detection and Mitigation

### Issue: Suspected DDoS Attack

**Symptoms**:

- Sudden spike in traffic
- High number of requests from specific IPs/regions
- Service degradation or unavailability
- Unusual traffic patterns

**Quick Diagnosis**:

```bash
# Check request rate
kubectl logs -l app=ecommerce-backend -n production | \
  grep "HTTP" | awk '{print $1}' | sort | uniq -c | sort -rn | head -20

# Check CloudWatch metrics
aws cloudwatch get-metric-statistics \
  --namespace AWS/ApplicationELB \
  --metric-name RequestCount \
  --dimensions Name=LoadBalancer,Value=${ALB_ARN} \
  --start-time $(date -u -d '1 hour ago' --iso-8601=seconds) \
  --end-time $(date -u --iso-8601=seconds) \
  --period 60 \
  --statistics Sum

# Check AWS Shield events
aws shield describe-attack \
  --attack-id ${ATTACK_ID}
```

**Immediate Actions**:

1. **Enable AWS Shield Advanced (if not already)**:

```bash
# Check Shield status
aws shield describe-subscription

# Enable DDoS Response Team (DRT) access if needed
aws shield associate-drt-role --role-arn ${DRT_ROLE_ARN}
```

1. **Activate rate limiting**:

```bash
# Update WAF rate limit rule
aws wafv2 update-web-acl \
  --id ${WEB_ACL_ID} \
  --scope REGIONAL \
  --lock-token ${LOCK_TOKEN} \
  --rules file://rate-limit-rules.json
```

1. **Block malicious IPs**:

```bash
# Add IPs to block list
aws wafv2 update-ip-set \
  --id ${BLOCK_IP_SET_ID} \
  --scope REGIONAL \
  --addresses ${MALICIOUS_IP}/32 \
  --lock-token ${LOCK_TOKEN}
```

1. **Enable CloudFront geo-blocking** (if applicable):

```bash
aws cloudfront update-distribution \
  --id ${DISTRIBUTION_ID} \
  --distribution-config file://geo-restriction-config.json
```

**Investigation Steps**:

1. Analyze traffic patterns:

```bash
# Top source IPs
aws logs filter-log-events \
  --log-group-name /aws/elasticloadbalancing/${ALB_NAME} \
  --start-time $(date -u -d '1 hour ago' +%s)000 | \
  jq -r '.events[].message' | \
  awk '{print $3}' | sort | uniq -c | sort -rn | head -20

# Request distribution by endpoint
kubectl logs -l app=ecommerce-backend -n production | \
  grep "HTTP" | awk '{print $7}' | sort | uniq -c | sort -rn
```

1. Check for attack signatures:

```bash
# Look for common DDoS patterns
kubectl logs -l app=ecommerce-backend -n production | \
  grep -E "(slowloris|RUDY|HTTP flood)" | wc -l

# Check for SYN flood
aws ec2 describe-flow-logs \
  --filter "Name=resource-id,Values=${VPC_ID}" | \
  grep "SYN"
```

**Mitigation Strategy**:

1. **Layer 7 (Application Layer)**:
   - Enable WAF rate limiting
   - Implement CAPTCHA challenges
   - Use CloudFront with AWS Shield
   - Enable request throttling

2. **Layer 4 (Transport Layer)**:
   - Enable AWS Shield Advanced
   - Use Network Load Balancer with Shield
   - Implement connection limits

3. **Layer 3 (Network Layer)**:
   - AWS Shield Standard (automatic)
   - VPC flow logs analysis
   - Network ACL rules

**Post-Incident**:

1. Review attack patterns and update defenses
2. Document attack characteristics
3. Update incident response procedures
4. Conduct post-mortem analysis

---

## Suspicious Activity Investigation

### Issue: Unusual User Behavior

**Symptoms**:

- Multiple failed login attempts
- Access from unusual locations
- Unusual API usage patterns
- Privilege escalation attempts

**Investigation Procedure**:

1. **Gather user activity data**:

```bash
# Check authentication logs
kubectl logs -l app=ecommerce-backend -n production | \
  grep "Authentication.*${USER_ID}" | tail -100

# Check authorization failures
kubectl logs -l app=ecommerce-backend -n production | \
  grep "Authorization failed.*${USER_ID}" | tail -100

# Check API access patterns
kubectl logs -l app=ecommerce-backend -n production | \
  grep "user=${USER_ID}" | \
  awk '{print $7}' | sort | uniq -c | sort -rn
```

1. **Analyze login patterns**:

```sql
-- Check recent login attempts
SELECT 
  user_id,
  ip_address,
  user_agent,
  success,
  created_at
FROM authentication_logs
WHERE user_id = '${USER_ID}'
  AND created_at > NOW() - INTERVAL '24 hours'
ORDER BY created_at DESC;

-- Check failed login attempts
SELECT 
  ip_address,
  COUNT(*) as attempts,
  MAX(created_at) as last_attempt
FROM authentication_logs
WHERE user_id = '${USER_ID}'
  AND success = false
  AND created_at > NOW() - INTERVAL '1 hour'
GROUP BY ip_address
HAVING COUNT(*) > 5;
```

1. **Check for privilege escalation**:

```sql
-- Check role changes
SELECT 
  user_id,
  old_role,
  new_role,
  changed_by,
  changed_at
FROM role_change_audit
WHERE user_id = '${USER_ID}'
  AND changed_at > NOW() - INTERVAL '7 days'
ORDER BY changed_at DESC;

-- Check permission grants
SELECT 
  user_id,
  permission,
  granted_by,
  granted_at
FROM permission_audit
WHERE user_id = '${USER_ID}'
  AND granted_at > NOW() - INTERVAL '7 days'
ORDER BY granted_at DESC;
```

1. **Analyze data access patterns**:

```sql
-- Check sensitive data access
SELECT 
  user_id,
  resource_type,
  resource_id,
  action,
  accessed_at
FROM data_access_audit
WHERE user_id = '${USER_ID}'
  AND resource_type IN ('customer_pii', 'payment_info', 'admin_panel')
  AND accessed_at > NOW() - INTERVAL '24 hours'
ORDER BY accessed_at DESC;
```

**Response Actions**:

1. **If account compromised**:

```bash
# Immediately disable account
curl -X POST http://localhost:8080/api/v1/admin/users/${USER_ID}/disable \
  -H "Authorization: Bearer ${ADMIN_TOKEN}"

# Invalidate all sessions
kubectl exec -it ${POD_NAME} -n production -- \
  redis-cli DEL "session:${USER_ID}:*"

# Force password reset
curl -X POST http://localhost:8080/api/v1/admin/users/${USER_ID}/force-password-reset \
  -H "Authorization: Bearer ${ADMIN_TOKEN}"
```

1. **If suspicious but not confirmed**:

```bash
# Enable enhanced monitoring
curl -X POST http://localhost:8080/api/v1/admin/users/${USER_ID}/enable-monitoring \
  -H "Authorization: Bearer ${ADMIN_TOKEN}"

# Require MFA for next login
curl -X POST http://localhost:8080/api/v1/admin/users/${USER_ID}/require-mfa \
  -H "Authorization: Bearer ${ADMIN_TOKEN}"
```

---

### Issue: Data Exfiltration Attempt

**Symptoms**:

- Large number of API requests
- Bulk data downloads
- Unusual database queries
- High data transfer volumes

**Investigation Procedure**:

1. **Check API usage**:

```bash
# Count API requests by user
kubectl logs -l app=ecommerce-backend -n production | \
  grep "user=${USER_ID}" | wc -l

# Check data volume transferred
kubectl logs -l app=ecommerce-backend -n production | \
  grep "user=${USER_ID}" | \
  awk '{sum+=$10} END {print "Total bytes:", sum}'
```

1. **Analyze database queries**:

```sql
-- Check for bulk queries
SELECT 
  user_id,
  query,
  rows_returned,
  execution_time,
  executed_at
FROM query_audit
WHERE user_id = '${USER_ID}'
  AND rows_returned > 1000
  AND executed_at > NOW() - INTERVAL '1 hour'
ORDER BY rows_returned DESC;
```

1. **Check export operations**:

```sql
-- Check data exports
SELECT 
  user_id,
  export_type,
  record_count,
  file_size,
  created_at
FROM export_audit
WHERE user_id = '${USER_ID}'
  AND created_at > NOW() - INTERVAL '24 hours'
ORDER BY created_at DESC;
```

**Response Actions**:

```bash
# Block user immediately
curl -X POST http://localhost:8080/api/v1/admin/users/${USER_ID}/block \
  -H "Authorization: Bearer ${ADMIN_TOKEN}"

# Revoke API keys
curl -X DELETE http://localhost:8080/api/v1/admin/users/${USER_ID}/api-keys \
  -H "Authorization: Bearer ${ADMIN_TOKEN}"

# Alert security team
# Send notification with evidence
```

---

## Audit Log Analysis

### Analyzing Security Events

**Common Queries**:

1. **Failed authentication attempts**:

```sql
SELECT 
  ip_address,
  user_agent,
  COUNT(*) as attempts,
  MIN(created_at) as first_attempt,
  MAX(created_at) as last_attempt
FROM authentication_logs
WHERE success = false
  AND created_at > NOW() - INTERVAL '1 hour'
GROUP BY ip_address, user_agent
HAVING COUNT(*) > 10
ORDER BY attempts DESC;
```

1. **Privilege escalation events**:

```sql
SELECT 
  user_id,
  action,
  resource,
  old_value,
  new_value,
  performed_by,
  created_at
FROM security_audit
WHERE action IN ('ROLE_CHANGE', 'PERMISSION_GRANT', 'ADMIN_ACCESS')
  AND created_at > NOW() - INTERVAL '24 hours'
ORDER BY created_at DESC;
```

1. **Sensitive data access**:

```sql
SELECT 
  user_id,
  resource_type,
  COUNT(*) as access_count,
  MIN(accessed_at) as first_access,
  MAX(accessed_at) as last_access
FROM data_access_audit
WHERE resource_type IN ('customer_pii', 'payment_info', 'financial_data')
  AND accessed_at > NOW() - INTERVAL '24 hours'
GROUP BY user_id, resource_type
HAVING COUNT(*) > 100
ORDER BY access_count DESC;
```

1. **Configuration changes**:

```sql
SELECT 
  user_id,
  config_key,
  old_value,
  new_value,
  changed_at
FROM config_change_audit
WHERE config_key LIKE '%security%'
  OR config_key LIKE '%auth%'
  OR config_key LIKE '%permission%'
ORDER BY changed_at DESC
LIMIT 50;
```

**CloudWatch Insights Queries**:

1. **Authentication failures by IP**:

```text
fields @timestamp, ip_address, user_id, error_message
| filter event_type = "authentication_failure"
| stats count() as failure_count by ip_address
| sort failure_count desc
| limit 20
```

1. **Authorization failures**:

```text
fields @timestamp, user_id, resource, action, reason
| filter event_type = "authorization_failure"
| stats count() as denial_count by user_id, resource
| sort denial_count desc
```

1. **Suspicious API patterns**:

```text
fields @timestamp, user_id, endpoint, response_code
| filter response_code = 403 or response_code = 401
| stats count() as error_count by user_id, endpoint
| filter error_count > 50
| sort error_count desc
```

---

## Security Incident Response Checklist

### Immediate Response (0-15 minutes)

- [ ] Identify and confirm the security incident
- [ ] Assess severity and impact
- [ ] Notify security team and on-call engineer
- [ ] Begin evidence collection
- [ ] Implement immediate containment measures

### Investigation (15-60 minutes)

- [ ] Collect and preserve logs
- [ ] Analyze attack vectors
- [ ] Identify affected systems and data
- [ ] Determine root cause
- [ ] Document timeline of events

### Containment (30-120 minutes)

- [ ] Block malicious IPs/users
- [ ] Revoke compromised credentials
- [ ] Isolate affected systems
- [ ] Apply emergency patches
- [ ] Enable enhanced monitoring

### Recovery (1-24 hours)

- [ ] Restore affected services
- [ ] Verify system integrity
- [ ] Reset compromised credentials
- [ ] Update security controls
- [ ] Conduct security scan

### Post-Incident (24-72 hours)

- [ ] Complete incident report
- [ ] Conduct post-mortem analysis
- [ ] Update security procedures
- [ ] Implement preventive measures
- [ ] Brief stakeholders

---

## Quick Reference Commands

### Authentication Checks

```bash
# Validate JWT token
curl -H "Authorization: Bearer ${TOKEN}" \
  http://localhost:8080/api/v1/auth/validate

# Check user sessions
kubectl exec -it ${POD_NAME} -n production -- \
  redis-cli KEYS "session:${USER_ID}:*"

# View authentication logs
kubectl logs -l app=ecommerce-backend -n production | \
  grep "Authentication"
```

### Authorization Checks

```bash
# Check user roles
curl -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  http://localhost:8080/api/v1/users/${USER_ID}/roles

# Verify permissions
curl -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  http://localhost:8080/api/v1/users/${USER_ID}/permissions

# Clear RBAC cache
kubectl exec -it ${POD_NAME} -n production -- \
  redis-cli DEL "rbac:cache:*"
```

### Network Security Checks

```bash
# Check security groups
aws ec2 describe-security-groups --group-ids ${SG_ID}

# Check Network ACLs
aws ec2 describe-network-acls --network-acl-ids ${NACL_ID}

# Check WAF rules
aws wafv2 get-web-acl --id ${WEB_ACL_ID} --scope REGIONAL

# Test connectivity
kubectl exec -it ${POD_NAME} -n production -- \
  nc -zv ${HOST} ${PORT}
```

### Audit Log Queries

```bash
# CloudWatch Logs
aws logs filter-log-events \
  --log-group-name /aws/application/security \
  --filter-pattern "{ $.event_type = \"security_incident\" }" \
  --start-time $(date -u -d '1 hour ago' +%s)000

# Application logs
kubectl logs -l app=ecommerce-backend -n production | \
  grep -E "(Authentication|Authorization|Security)" | tail -100
```

---

## Escalation Procedures

### Severity Levels

**P0 - Critical**:

- Active security breach
- Data exfiltration in progress
- System-wide compromise
- **Response Time**: Immediate
- **Escalation**: Security team + CTO

**P1 - High**:

- Suspected account compromise
- DDoS attack
- Multiple authentication failures
- **Response Time**: 15 minutes
- **Escalation**: Security team + Engineering manager

**P2 - Medium**:

- Authorization issues
- WAF blocking legitimate traffic
- Security configuration issues
- **Response Time**: 1 hour
- **Escalation**: On-call engineer

**P3 - Low**:

- Minor security warnings
- Audit log anomalies
- Non-critical security updates
- **Response Time**: 4 hours
- **Escalation**: Security team (next business day)

### Contact Information

- **Security Team**: <security@company.com>
- **On-Call Engineer**: PagerDuty
- **AWS Support**: Premium support portal
- **Incident Commander**: See on-call schedule

---

## Related Documentation

- [Security Perspective](../../perspectives/security/README.md) - Security architecture and design
- [Authentication Guide](../../perspectives/security/authentication.md) - Authentication mechanisms
- [Authorization Guide](../../perspectives/security/authorization.md) - RBAC and permissions
- [Monitoring Strategy](../monitoring/monitoring-strategy.md) - Security monitoring setup

---

**Last Updated**: 2025-10-25  
**Owner**: Security Team  
**Review Cycle**: Quarterly  
**Emergency Contact**: <security@company.com>
