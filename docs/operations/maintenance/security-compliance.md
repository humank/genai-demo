# Security and Compliance Maintenance Guide

## Overview

This guide covers security patch management, user access control, audit logging, encryption key management, SSL/TLS certificate management, compliance reporting, vulnerability scanning, and access review procedures.

## Security Patch Management

### Patch Management Strategy

#### Patch Classification

**Critical Patches**
- Security vulnerabilities with CVSS score ≥ 9.0
- Zero-day exploits
- Active exploitation in the wild
- Apply within 24-48 hours

**High Priority Patches**
- Security vulnerabilities with CVSS score 7.0-8.9
- Significant security improvements
- Apply within 1 week

**Medium Priority Patches**
- Security vulnerabilities with CVSS score 4.0-6.9
- Performance improvements
- Apply within 2-4 weeks

**Low Priority Patches**
- Minor bug fixes
- Feature updates
- Apply during regular maintenance windows

### Patch Management Procedures

#### Monthly Patch Review

```bash
# Check for available security updates (AWS RDS)
aws rds describe-db-engine-versions \
  --engine postgres \
  --engine-version 14.10 \
  --query 'DBEngineVersions[0].ValidUpgradeTarget[*].[EngineVersion,Description]'

# Check for pending maintenance
aws rds describe-pending-maintenance-actions \
  --resource-identifier arn:aws:rds:us-east-1:123456789012:db:ecommerce-production

# Review AWS security bulletins
# https://aws.amazon.com/security/security-bulletins/
```

#### Application Dependency Patching

```bash
# Check for security vulnerabilities in Java dependencies
./gradlew dependencyCheckAnalyze

# Review vulnerability report
open build/reports/dependency-check-report.html

# Update vulnerable dependencies in build.gradle
# Example: Update Spring Boot version
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web:3.2.1'
}

# Run tests after updates
./gradlew clean test

# Check for npm vulnerabilities (frontend)
cd cmc-frontend
npm audit

# Fix vulnerabilities automatically
npm audit fix

# For breaking changes, update manually
npm audit fix --force
```

#### Operating System Patching

```bash
# Check for OS updates (EKS nodes)
kubectl get nodes -o wide

# Update EKS node group AMI
aws eks update-nodegroup-version \
  --cluster-name ecommerce-production \
  --nodegroup-name production-nodes \
  --release-version 1.28.5-20240110

# Monitor update progress
aws eks describe-nodegroup \
  --cluster-name ecommerce-production \
  --nodegroup-name production-nodes \
  --query 'nodegroup.status'
```

#### Patch Testing Workflow

```bash
# 1. Create test environment from production snapshot
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier ecommerce-test-patching \
  --db-snapshot-identifier ecommerce-production-latest

# 2. Apply patches to test environment
aws rds modify-db-instance \
  --db-instance-identifier ecommerce-test-patching \
  --engine-version 14.11 \
  --apply-immediately

# 3. Run automated tests
export DATABASE_URL="postgresql://postgres:password@ecommerce-test-patching.xxx.rds.amazonaws.com:5432/ecommerce_production"
./gradlew test integrationTest

# 4. Perform manual testing
# - Test critical user flows
# - Verify API endpoints
# - Check admin functions

# 5. If tests pass, schedule production patching
# 6. If tests fail, investigate and resolve issues before production
```

### Emergency Patch Procedures

#### Critical Security Patch (< 24 hours)

```bash
# 1. Assess impact and urgency
# Review CVE details and exploitation status

# 2. Create emergency backup
aws rds create-db-snapshot \
  --db-instance-identifier ecommerce-production \
  --db-snapshot-identifier emergency-patch-$(date +%Y%m%d-%H%M%S)

# 3. Notify stakeholders
# Send emergency maintenance notification

# 4. Apply patch immediately
aws rds modify-db-instance \
  --db-instance-identifier ecommerce-production \
  --engine-version 14.11 \
  --apply-immediately

# 5. Monitor application health
kubectl get pods -n production
kubectl logs -f deployment/ecommerce-api -n production

# 6. Verify security patch applied
psql -c "SELECT version();"

# 7. Document incident and actions taken
```

## User and Role Management

### User Access Control

#### User Lifecycle Management

**New User Onboarding**

```sql
-- Create new database user
CREATE USER john_doe WITH PASSWORD 'secure_random_password';

-- Grant appropriate role
GRANT developer_role TO john_doe;

-- Set connection limit
ALTER USER john_doe CONNECTION LIMIT 5;

-- Set password expiration
ALTER USER john_doe VALID UNTIL '2025-12-31';

-- Document user creation
INSERT INTO user_audit_log (username, action, performed_by, timestamp)
VALUES ('john_doe', 'USER_CREATED', current_user, NOW());
```

**User Modification**

```sql
-- Change user password
ALTER USER john_doe WITH PASSWORD 'new_secure_password';

-- Update role assignment
REVOKE developer_role FROM john_doe;
GRANT senior_developer_role TO john_doe;

-- Modify connection limit
ALTER USER john_doe CONNECTION LIMIT 10;

-- Extend password validity
ALTER USER john_doe VALID UNTIL '2026-12-31';
```

**User Offboarding**

```sql
-- Revoke all privileges
REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM john_doe;
REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public FROM john_doe;
REVOKE ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public FROM john_doe;

-- Revoke role membership
REVOKE developer_role FROM john_doe;

-- Disable user (don't delete immediately for audit trail)
ALTER USER john_doe WITH NOLOGIN;

-- Terminate active connections
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE usename = 'john_doe';

-- Document user deactivation
INSERT INTO user_audit_log (username, action, performed_by, timestamp)
VALUES ('john_doe', 'USER_DISABLED', current_user, NOW());

-- After 90 days, drop user if no longer needed
-- DROP USER john_doe;
```

#### Role-Based Access Control (RBAC)

**Standard Roles**

```sql
-- Read-only role for analysts
CREATE ROLE analyst_role;
GRANT CONNECT ON DATABASE ecommerce_production TO analyst_role;
GRANT USAGE ON SCHEMA public TO analyst_role;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO analyst_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO analyst_role;

-- Developer role with read/write on specific tables
CREATE ROLE developer_role;
GRANT CONNECT ON DATABASE ecommerce_production TO developer_role;
GRANT USAGE ON SCHEMA public TO developer_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO developer_role;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO developer_role;

-- Admin role with full privileges
CREATE ROLE admin_role;
GRANT ALL PRIVILEGES ON DATABASE ecommerce_production TO admin_role;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO admin_role;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO admin_role;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO admin_role;

-- Application service account (limited privileges)
CREATE ROLE app_service_role;
GRANT CONNECT ON DATABASE ecommerce_production TO app_service_role;
GRANT USAGE ON SCHEMA public TO app_service_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO app_service_role;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_service_role;
-- Exclude sensitive tables
REVOKE ALL ON audit_log FROM app_service_role;
REVOKE ALL ON user_credentials FROM app_service_role;
```

**Role Review and Audit**

```sql
-- List all roles and their members
SELECT r.rolname AS role_name,
       ARRAY_AGG(m.rolname) AS members
FROM pg_roles r
LEFT JOIN pg_auth_members am ON r.oid = am.roleid
LEFT JOIN pg_roles m ON am.member = m.oid
WHERE r.rolname NOT LIKE 'pg_%'
  AND r.rolname != 'rdsadmin'
GROUP BY r.rolname
ORDER BY r.rolname;

-- List all users and their roles
SELECT usename AS username,
       ARRAY_AGG(rolname) AS roles,
       valuntil AS password_expiry
FROM pg_user u
LEFT JOIN pg_auth_members am ON u.usesysid = am.member
LEFT JOIN pg_roles r ON am.roleid = r.oid
WHERE usename NOT LIKE 'pg_%'
  AND usename != 'rdsadmin'
GROUP BY usename, valuntil
ORDER BY usename;

-- Check user privileges on specific table
SELECT grantee, privilege_type
FROM information_schema.role_table_grants
WHERE table_name = 'customers'
ORDER BY grantee, privilege_type;
```

### AWS IAM User and Role Management

#### IAM User Management

```bash
# Create new IAM user
aws iam create-user --user-name john.doe

# Add user to appropriate group
aws iam add-user-to-group \
  --user-name john.doe \
  --group-name Developers

# Create access keys
aws iam create-access-key --user-name john.doe

# Enable MFA for user
aws iam enable-mfa-device \
  --user-name john.doe \
  --serial-number arn:aws:iam::123456789012:mfa/john.doe \
  --authentication-code-1 123456 \
  --authentication-code-2 789012

# Set password policy
aws iam update-account-password-policy \
  --minimum-password-length 14 \
  --require-symbols \
  --require-numbers \
  --require-uppercase-characters \
  --require-lowercase-characters \
  --allow-users-to-change-password \
  --max-password-age 90 \
  --password-reuse-prevention 5
```

#### IAM Role Management

```bash
# Create service role for EKS
aws iam create-role \
  --role-name EKSServiceRole \
  --assume-role-policy-document file://eks-trust-policy.json

# Attach managed policy
aws iam attach-role-policy \
  --role-name EKSServiceRole \
  --policy-arn arn:aws:iam::aws:policy/AmazonEKSClusterPolicy

# Create custom policy for application
aws iam create-policy \
  --policy-name EcommerceAppPolicy \
  --policy-document file://app-policy.json

# List roles and their policies
aws iam list-roles --query 'Roles[?contains(RoleName, `Ecommerce`)].RoleName'

# Review role permissions
aws iam get-role-policy \
  --role-name EKSServiceRole \
  --policy-name EKSPolicy
```

## Audit Log Configuration and Analysis

### Database Audit Logging

#### Enable PostgreSQL Audit Logging

```sql
-- Enable logging (via RDS parameter group)
-- log_statement = 'all'  -- Log all statements (verbose)
-- log_statement = 'ddl'  -- Log DDL statements only
-- log_statement = 'mod'  -- Log DDL and data-modifying statements
-- log_min_duration_statement = 1000  -- Log queries taking > 1 second

-- Create audit log table
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGSERIAL PRIMARY KEY,
    event_time TIMESTAMP DEFAULT NOW(),
    user_name TEXT,
    database_name TEXT,
    client_addr INET,
    application_name TEXT,
    event_type TEXT,
    object_type TEXT,
    object_name TEXT,
    command_text TEXT,
    success BOOLEAN,
    error_message TEXT
);

-- Create audit trigger function
CREATE OR REPLACE FUNCTION audit_trigger_function()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO audit_log (
        user_name, database_name, client_addr, application_name,
        event_type, object_type, object_name, command_text, success
    )
    VALUES (
        current_user,
        current_database(),
        inet_client_addr(),
        current_setting('application_name', true),
        TG_OP,
        TG_TABLE_SCHEMA || '.' || TG_TABLE_NAME,
        TG_TABLE_NAME,
        current_query(),
        true
    );
    
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Apply audit trigger to sensitive tables
CREATE TRIGGER audit_customers_trigger
AFTER INSERT OR UPDATE OR DELETE ON customers
FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

CREATE TRIGGER audit_orders_trigger
AFTER INSERT OR UPDATE OR DELETE ON orders
FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

CREATE TRIGGER audit_payments_trigger
AFTER INSERT OR UPDATE OR DELETE ON payments
FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();
```

#### Audit Log Analysis

```sql
-- Review recent audit events
SELECT event_time, user_name, event_type, object_name, command_text
FROM audit_log
WHERE event_time > NOW() - INTERVAL '24 hours'
ORDER BY event_time DESC
LIMIT 100;

-- Find failed login attempts
SELECT event_time, user_name, client_addr, error_message
FROM audit_log
WHERE event_type = 'LOGIN_FAILED'
  AND event_time > NOW() - INTERVAL '7 days'
ORDER BY event_time DESC;

-- Detect suspicious activity patterns
-- Multiple failed logins from same IP
SELECT client_addr, COUNT(*) AS failed_attempts,
       MIN(event_time) AS first_attempt,
       MAX(event_time) AS last_attempt
FROM audit_log
WHERE event_type = 'LOGIN_FAILED'
  AND event_time > NOW() - INTERVAL '1 hour'
GROUP BY client_addr
HAVING COUNT(*) > 5
ORDER BY failed_attempts DESC;

-- Track privileged operations
SELECT event_time, user_name, event_type, object_name, command_text
FROM audit_log
WHERE event_type IN ('DROP', 'TRUNCATE', 'ALTER')
  AND event_time > NOW() - INTERVAL '30 days'
ORDER BY event_time DESC;

-- Monitor data access patterns
SELECT user_name, object_name, COUNT(*) AS access_count,
       MIN(event_time) AS first_access,
       MAX(event_time) AS last_access
FROM audit_log
WHERE event_type = 'SELECT'
  AND object_name IN ('customers', 'payments', 'user_credentials')
  AND event_time > NOW() - INTERVAL '7 days'
GROUP BY user_name, object_name
ORDER BY access_count DESC;

-- Detect unusual data modifications
SELECT event_time, user_name, event_type, object_name,
       COUNT(*) OVER (PARTITION BY user_name, DATE(event_time)) AS daily_modifications
FROM audit_log
WHERE event_type IN ('INSERT', 'UPDATE', 'DELETE')
  AND event_time > NOW() - INTERVAL '7 days'
ORDER BY daily_modifications DESC, event_time DESC;
```

### Application Audit Logging

#### Spring Boot Audit Configuration

```java
@Configuration
@EnableJpaAuditing
public class AuditConfiguration {
    
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext())
            .map(SecurityContext::getAuthentication)
            .filter(Authentication::isAuthenticated)
            .map(Authentication::getName);
    }
}

@Entity
@EntityListeners(AuditingEntityListener.class)
public class AuditableEntity {
    
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;
    
    @CreatedDate
    @Column(name = "created_date", updatable = false)
    private Instant createdDate;
    
    @LastModifiedBy
    @Column(name = "last_modified_by")
    private String lastModifiedBy;
    
    @LastModifiedDate
    @Column(name = "last_modified_date")
    private Instant lastModifiedDate;
}

@Component
public class SecurityAuditLogger {
    
    private final Logger auditLogger = LoggerFactory.getLogger("SECURITY_AUDIT");
    
    public void logAuthenticationSuccess(String username, String ipAddress) {
        auditLogger.info("Authentication successful",
            kv("event", "AUTH_SUCCESS"),
            kv("username", username),
            kv("ipAddress", ipAddress),
            kv("timestamp", Instant.now()));
    }
    
    public void logAuthenticationFailure(String username, String ipAddress, String reason) {
        auditLogger.warn("Authentication failed",
            kv("event", "AUTH_FAILURE"),
            kv("username", username),
            kv("ipAddress", ipAddress),
            kv("reason", reason),
            kv("timestamp", Instant.now()));
    }
    
    public void logAuthorizationFailure(String username, String resource, String action) {
        auditLogger.warn("Authorization failed",
            kv("event", "AUTHZ_FAILURE"),
            kv("username", username),
            kv("resource", resource),
            kv("action", action),
            kv("timestamp", Instant.now()));
    }
    
    public void logDataAccess(String username, String resource, String action) {
        auditLogger.info("Data access",
            kv("event", "DATA_ACCESS"),
            kv("username", username),
            kv("resource", resource),
            kv("action", action),
            kv("timestamp", Instant.now()));
    }
}
```

### AWS CloudTrail Audit Logging

#### Enable CloudTrail

```bash
# Create CloudTrail trail
aws cloudtrail create-trail \
  --name ecommerce-audit-trail \
  --s3-bucket-name ecommerce-audit-logs \
  --include-global-service-events \
  --is-multi-region-trail \
  --enable-log-file-validation

# Start logging
aws cloudtrail start-logging --name ecommerce-audit-trail

# Configure CloudWatch Logs integration
aws cloudtrail update-trail \
  --name ecommerce-audit-trail \
  --cloud-watch-logs-log-group-arn arn:aws:logs:us-east-1:123456789012:log-group:CloudTrail/ecommerce \
  --cloud-watch-logs-role-arn arn:aws:iam::123456789012:role/CloudTrailRole

# Enable S3 data events
aws cloudtrail put-event-selectors \
  --trail-name ecommerce-audit-trail \
  --event-selectors '[
    {
      "ReadWriteType": "All",
      "IncludeManagementEvents": true,
      "DataResources": [
        {
          "Type": "AWS::S3::Object",
          "Values": ["arn:aws:s3:::ecommerce-data/*"]
        }
      ]
    }
  ]'
```

#### Query CloudTrail Logs

```bash
# Query recent API calls
aws cloudtrail lookup-events \
  --lookup-attributes AttributeKey=EventName,AttributeValue=DeleteDBInstance \
  --max-results 10

# Query events by user
aws cloudtrail lookup-events \
  --lookup-attributes AttributeKey=Username,AttributeValue=john.doe \
  --start-time $(date -u -d '7 days ago' +%Y-%m-%dT%H:%M:%S) \
  --max-results 50

# Query failed API calls
aws cloudtrail lookup-events \
  --lookup-attributes AttributeKey=EventName,AttributeValue=AssumeRole \
  --start-time $(date -u -d '24 hours ago' +%Y-%m-%dT%H:%M:%S) \
  | jq '.Events[] | select(.CloudTrailEvent | fromjson | .errorCode != null)'
```

## Encryption Key Rotation

### AWS KMS Key Rotation

#### Enable Automatic Key Rotation

```bash
# Enable automatic rotation for KMS key (rotates annually)
aws kms enable-key-rotation --key-id alias/ecommerce-data-key

# Check rotation status
aws kms get-key-rotation-status --key-id alias/ecommerce-data-key

# List key rotation history
aws kms list-key-rotations --key-id alias/ecommerce-data-key
```

#### Manual Key Rotation Procedure

```bash
# 1. Create new KMS key
aws kms create-key \
  --description "Ecommerce data encryption key v2" \
  --key-usage ENCRYPT_DECRYPT \
  --origin AWS_KMS

# 2. Create alias for new key
aws kms create-alias \
  --alias-name alias/ecommerce-data-key-v2 \
  --target-key-id <new-key-id>

# 3. Update application configuration to use new key
# Update environment variables or parameter store

# 4. Re-encrypt data with new key (for RDS)
aws rds modify-db-instance \
  --db-instance-identifier ecommerce-production \
  --kms-key-id alias/ecommerce-data-key-v2 \
  --apply-immediately

# 5. Verify new key is in use
aws rds describe-db-instances \
  --db-instance-identifier ecommerce-production \
  --query 'DBInstances[0].KmsKeyId'

# 6. Update alias to point to new key
aws kms update-alias \
  --alias-name alias/ecommerce-data-key \
  --target-key-id <new-key-id>

# 7. Schedule old key for deletion (after 30-day grace period)
aws kms schedule-key-deletion \
  --key-id <old-key-id> \
  --pending-window-in-days 30
```

### Application-Level Encryption Key Rotation

#### Database Encryption Key Rotation

```sql
-- Create new encryption key version
INSERT INTO encryption_keys (key_id, key_version, key_material, created_at, status)
VALUES ('data-encryption-key', 2, encode(gen_random_bytes(32), 'base64'), NOW(), 'ACTIVE');

-- Mark old key as deprecated
UPDATE encryption_keys
SET status = 'DEPRECATED', deprecated_at = NOW()
WHERE key_id = 'data-encryption-key' AND key_version = 1;

-- Re-encrypt sensitive data with new key
-- This should be done in batches to avoid long-running transactions
DO $$
DECLARE
    batch_size INT := 1000;
    offset_val INT := 0;
    rows_updated INT;
BEGIN
    LOOP
        UPDATE customers
        SET encrypted_ssn = encrypt_with_key(decrypt_with_old_key(encrypted_ssn), 2)
        WHERE id IN (
            SELECT id FROM customers
            WHERE encryption_key_version = 1
            ORDER BY id
            LIMIT batch_size OFFSET offset_val
        );
        
        GET DIAGNOSTICS rows_updated = ROW_COUNT;
        EXIT WHEN rows_updated = 0;
        
        offset_val := offset_val + batch_size;
        COMMIT;
        
        -- Add delay to avoid overwhelming the database
        PERFORM pg_sleep(1);
    END LOOP;
END $$;

-- Verify all data re-encrypted
SELECT encryption_key_version, COUNT(*)
FROM customers
GROUP BY encryption_key_version;

-- After verification, schedule old key for deletion
UPDATE encryption_keys
SET status = 'SCHEDULED_FOR_DELETION', deletion_date = NOW() + INTERVAL '90 days'
WHERE key_id = 'data-encryption-key' AND key_version = 1;
```

### JWT Secret Rotation

```bash
# 1. Generate new JWT secret
NEW_SECRET=$(openssl rand -base64 64)

# 2. Update secret in AWS Secrets Manager
aws secretsmanager update-secret \
  --secret-id ecommerce/jwt-secret \
  --secret-string "{\"current\":\"$NEW_SECRET\",\"previous\":\"$OLD_SECRET\"}"

# 3. Update application configuration
# Application should support both current and previous secrets during transition

# 4. Deploy updated application
kubectl set env deployment/ecommerce-api \
  JWT_SECRET_VERSION=v2 \
  -n production

# 5. Wait for all pods to restart
kubectl rollout status deployment/ecommerce-api -n production

# 6. After transition period (e.g., 24 hours), remove old secret
aws secretsmanager update-secret \
  --secret-id ecommerce/jwt-secret \
  --secret-string "{\"current\":\"$NEW_SECRET\"}"
```

## SSL/TLS Certificate Management

### Certificate Lifecycle Management

#### Certificate Inventory

```bash
# List ACM certificates
aws acm list-certificates \
  --query 'CertificateSummaryList[*].[DomainName,CertificateArn,Status]' \
  --output table

# Get certificate details
aws acm describe-certificate \
  --certificate-arn arn:aws:acm:us-east-1:123456789012:certificate/xxx \
  --query 'Certificate.[DomainName,NotBefore,NotAfter,Status]'

# Check certificate expiration
aws acm describe-certificate \
  --certificate-arn arn:aws:acm:us-east-1:123456789012:certificate/xxx \
  --query 'Certificate.NotAfter'
```

#### Certificate Renewal (ACM)

```bash
# ACM automatically renews certificates if DNS validation is configured
# Verify DNS validation records are in place

# Check renewal status
aws acm describe-certificate \
  --certificate-arn arn:aws:acm:us-east-1:123456789012:certificate/xxx \
  --query 'Certificate.RenewalSummary'

# If renewal fails, check validation records
aws acm describe-certificate \
  --certificate-arn arn:aws:acm:us-east-1:123456789012:certificate/xxx \
  --query 'Certificate.DomainValidationOptions'

# Manually trigger renewal (if needed)
aws acm renew-certificate \
  --certificate-arn arn:aws:acm:us-east-1:123456789012:certificate/xxx
```

#### Certificate Renewal (Let's Encrypt)

```bash
# For self-managed certificates using certbot

# Check certificate expiration
certbot certificates

# Renew certificates (dry run)
certbot renew --dry-run

# Renew certificates
certbot renew

# Renew specific certificate
certbot renew --cert-name api.ecommerce.com

# Automate renewal with cron
# Add to crontab: 0 0 * * * certbot renew --quiet --post-hook "systemctl reload nginx"
```

#### Certificate Monitoring

```bash
# Create CloudWatch alarm for certificate expiration
aws cloudwatch put-metric-alarm \
  --alarm-name certificate-expiring-soon \
  --alarm-description "Alert when SSL certificate expires in 30 days" \
  --metric-name DaysToExpiry \
  --namespace AWS/CertificateManager \
  --statistic Minimum \
  --period 86400 \
  --evaluation-periods 1 \
  --threshold 30 \
  --comparison-operator LessThanThreshold \
  --dimensions Name=CertificateArn,Value=arn:aws:acm:us-east-1:123456789012:certificate/xxx

# Check certificate expiration dates
openssl s_client -connect api.ecommerce.com:443 -servername api.ecommerce.com < /dev/null 2>/dev/null \
  | openssl x509 -noout -dates
```

## Compliance Reporting

### Compliance Frameworks

#### PCI-DSS Compliance

**Quarterly Compliance Report**

```bash
# Generate PCI-DSS compliance report

# 1. Network segmentation verification
aws ec2 describe-security-groups \
  --filters "Name=group-name,Values=*payment*" \
  --query 'SecurityGroups[*].[GroupName,GroupId,IpPermissions]'

# 2. Encryption verification
aws rds describe-db-instances \
  --query 'DBInstances[*].[DBInstanceIdentifier,StorageEncrypted,KmsKeyId]'

# 3. Access control audit
aws iam get-account-password-policy

# 4. Logging verification
aws cloudtrail describe-trails \
  --query 'trailList[*].[Name,IsMultiRegionTrail,LogFileValidationEnabled]'

# 5. Vulnerability scan results
# Review AWS Inspector findings
aws inspector2 list-findings \
  --filter-criteria '{"severity":[{"comparison":"EQUALS","value":"CRITICAL"}]}' \
  --max-results 100
```

**PCI-DSS Compliance Checklist**

```sql
-- Database compliance checks

-- 1. Verify encryption at rest
SELECT datname, 
       CASE WHEN EXISTS (
         SELECT 1 FROM pg_settings 
         WHERE name = 'ssl' AND setting = 'on'
       ) THEN 'ENABLED' ELSE 'DISABLED' END AS ssl_status
FROM pg_database
WHERE datname = current_database();

-- 2. Check password policies
SELECT usename, valuntil
FROM pg_user
WHERE valuntil IS NULL OR valuntil < NOW() + INTERVAL '90 days'
ORDER BY valuntil;

-- 3. Review user access
SELECT usename, usesuper, usecreatedb, usecreaterole
FROM pg_user
WHERE usename NOT IN ('postgres', 'rdsadmin')
ORDER BY usename;

-- 4. Audit log review
SELECT COUNT(*) AS total_audit_events,
       COUNT(CASE WHEN success = false THEN 1 END) AS failed_events,
       COUNT(CASE WHEN event_type IN ('DROP', 'TRUNCATE') THEN 1 END) AS destructive_events
FROM audit_log
WHERE event_time > NOW() - INTERVAL '90 days';
```

#### GDPR Compliance

**Data Subject Rights Management**

```sql
-- Right to Access: Export all personal data for a customer
SELECT 
    'customers' AS table_name,
    json_build_object(
        'id', id,
        'name', name,
        'email', email,
        'phone', phone,
        'created_at', created_at
    ) AS data
FROM customers WHERE id = 'CUST-123'
UNION ALL
SELECT 
    'orders' AS table_name,
    json_agg(json_build_object(
        'order_id', id,
        'order_date', order_date,
        'total_amount', total_amount,
        'status', status
    )) AS data
FROM orders WHERE customer_id = 'CUST-123'
UNION ALL
SELECT 
    'addresses' AS table_name,
    json_agg(json_build_object(
        'address_id', id,
        'street', street,
        'city', city,
        'postal_code', postal_code
    )) AS data
FROM addresses WHERE customer_id = 'CUST-123';

-- Right to Erasure: Anonymize customer data
BEGIN;

-- Anonymize customer record
UPDATE customers
SET name = 'ANONYMIZED_' || id,
    email = 'anonymized_' || id || '@deleted.local',
    phone = NULL,
    date_of_birth = NULL,
    anonymized_at = NOW()
WHERE id = 'CUST-123';

-- Anonymize related data
UPDATE orders
SET shipping_address = 'ANONYMIZED',
    billing_address = 'ANONYMIZED'
WHERE customer_id = 'CUST-123';

-- Log erasure request
INSERT INTO gdpr_requests (customer_id, request_type, processed_at)
VALUES ('CUST-123', 'ERASURE', NOW());

COMMIT;

-- Right to Portability: Generate data export
COPY (
    SELECT * FROM customers WHERE id = 'CUST-123'
) TO '/tmp/customer_data_export.csv' WITH CSV HEADER;
```

**GDPR Compliance Reporting**

```sql
-- Generate GDPR compliance report

-- 1. Data retention compliance
SELECT 
    'customers' AS table_name,
    COUNT(*) AS total_records,
    COUNT(CASE WHEN created_at < NOW() - INTERVAL '7 years' THEN 1 END) AS records_exceeding_retention,
    COUNT(CASE WHEN anonymized_at IS NOT NULL THEN 1 END) AS anonymized_records
FROM customers
UNION ALL
SELECT 
    'orders' AS table_name,
    COUNT(*) AS total_records,
    COUNT(CASE WHEN order_date < NOW() - INTERVAL '7 years' THEN 1 END) AS records_exceeding_retention,
    0 AS anonymized_records
FROM orders;

-- 2. Data subject requests tracking
SELECT request_type, 
       COUNT(*) AS total_requests,
       COUNT(CASE WHEN processed_at IS NOT NULL THEN 1 END) AS processed,
       COUNT(CASE WHEN processed_at IS NULL AND requested_at < NOW() - INTERVAL '30 days' THEN 1 END) AS overdue
FROM gdpr_requests
WHERE requested_at > NOW() - INTERVAL '1 year'
GROUP BY request_type;

-- 3. Consent tracking
SELECT consent_type,
       COUNT(*) AS total_consents,
       COUNT(CASE WHEN consent_given = true THEN 1 END) AS consents_given,
       COUNT(CASE WHEN consent_given = false THEN 1 END) AS consents_withdrawn
FROM customer_consents
WHERE created_at > NOW() - INTERVAL '1 year'
GROUP BY consent_type;

-- 4. Data breach incidents
SELECT incident_date, incident_type, affected_records, reported_to_authority
FROM data_breach_incidents
WHERE incident_date > NOW() - INTERVAL '1 year'
ORDER BY incident_date DESC;
```

#### SOC 2 Compliance

**Control Evidence Collection**

```bash
# Security controls evidence

# 1. Access control evidence
aws iam get-account-summary
aws iam list-users --query 'Users[*].[UserName,CreateDate,PasswordLastUsed]'

# 2. Change management evidence
git log --since="3 months ago" --pretty=format:"%h - %an, %ar : %s" > change_log.txt

# 3. Monitoring evidence
aws cloudwatch describe-alarms --query 'MetricAlarms[*].[AlarmName,StateValue]'

# 4. Backup evidence
aws rds describe-db-snapshots \
  --query 'DBSnapshots[*].[DBSnapshotIdentifier,SnapshotCreateTime,Status]' \
  --output table

# 5. Incident response evidence
# Export incident tickets from ticketing system
```

**SOC 2 Compliance Report**

```sql
-- Generate SOC 2 compliance metrics

-- 1. System availability
SELECT 
    DATE(timestamp) AS date,
    COUNT(*) AS total_checks,
    COUNT(CASE WHEN status = 'UP' THEN 1 END) AS successful_checks,
    ROUND(100.0 * COUNT(CASE WHEN status = 'UP' THEN 1 END) / COUNT(*), 2) AS availability_percent
FROM health_checks
WHERE timestamp > NOW() - INTERVAL '90 days'
GROUP BY DATE(timestamp)
ORDER BY date DESC;

-- 2. Security incident tracking
SELECT 
    incident_type,
    COUNT(*) AS total_incidents,
    AVG(EXTRACT(EPOCH FROM (resolved_at - detected_at))/3600) AS avg_resolution_hours,
    COUNT(CASE WHEN severity = 'CRITICAL' THEN 1 END) AS critical_incidents
FROM security_incidents
WHERE detected_at > NOW() - INTERVAL '90 days'
GROUP BY incident_type;

-- 3. Access review compliance
SELECT 
    review_period,
    COUNT(*) AS total_users_reviewed,
    COUNT(CASE WHEN access_modified THEN 1 END) AS access_changes,
    COUNT(CASE WHEN access_revoked THEN 1 END) AS access_revocations
FROM access_reviews
WHERE review_date > NOW() - INTERVAL '1 year'
GROUP BY review_period
ORDER BY review_period DESC;
```

## Security Vulnerability Scanning

### Infrastructure Vulnerability Scanning

#### AWS Inspector

```bash
# Enable AWS Inspector
aws inspector2 enable \
  --resource-types EC2 ECR LAMBDA

# Create assessment target
aws inspector create-assessment-target \
  --assessment-target-name ecommerce-production \
  --resource-group-arn arn:aws:inspector:us-east-1:123456789012:resourcegroup/xxx

# Create assessment template
aws inspector create-assessment-template \
  --assessment-target-arn arn:aws:inspector:us-east-1:123456789012:target/xxx \
  --assessment-template-name weekly-security-scan \
  --duration-in-seconds 3600 \
  --rules-package-arns \
    arn:aws:inspector:us-east-1:316112463485:rulespackage/0-gEjTy7T7 \
    arn:aws:inspector:us-east-1:316112463485:rulespackage/0-rExsr2X8

# Run assessment
aws inspector start-assessment-run \
  --assessment-template-arn arn:aws:inspector:us-east-1:123456789012:template/xxx

# Get findings
aws inspector2 list-findings \
  --filter-criteria '{"severity":[{"comparison":"EQUALS","value":"CRITICAL"}]}' \
  --max-results 100

# Generate report
aws inspector get-assessment-report \
  --assessment-run-arn arn:aws:inspector:us-east-1:123456789012:run/xxx \
  --report-file-format PDF \
  --report-type FULL
```

#### Container Image Scanning

```bash
# Scan Docker images with Trivy
trivy image --severity HIGH,CRITICAL ecommerce-api:latest

# Scan with detailed output
trivy image --format json --output trivy-report.json ecommerce-api:latest

# Scan ECR images
aws ecr start-image-scan \
  --repository-name ecommerce-api \
  --image-id imageTag=latest

# Get scan results
aws ecr describe-image-scan-findings \
  --repository-name ecommerce-api \
  --image-id imageTag=latest
```

### Application Vulnerability Scanning

#### Dependency Scanning

```bash
# Scan Java dependencies with OWASP Dependency Check
./gradlew dependencyCheckAnalyze

# Review report
open build/reports/dependency-check-report.html

# Scan npm dependencies
cd cmc-frontend
npm audit

# Generate detailed report
npm audit --json > npm-audit-report.json

# Scan with Snyk
snyk test --severity-threshold=high

# Monitor dependencies continuously
snyk monitor
```

#### Static Application Security Testing (SAST)

```bash
# Run SonarQube analysis
./gradlew sonarqube \
  -Dsonar.projectKey=ecommerce-platform \
  -Dsonar.host.url=https://sonarqube.example.com \
  -Dsonar.login=$SONAR_TOKEN

# Run SpotBugs security checks
./gradlew spotbugsMain

# Review security findings
open build/reports/spotbugs/main.html

# Run PMD security rules
./gradlew pmdMain

# Check for hardcoded secrets
trufflehog git file://. --only-verified
```

#### Dynamic Application Security Testing (DAST)

```bash
# Run OWASP ZAP scan
docker run -t owasp/zap2docker-stable zap-baseline.py \
  -t https://api.ecommerce.com \
  -r zap-report.html

# Run Burp Suite scan (if available)
# Configure and run through Burp Suite Professional

# Perform penetration testing
# Schedule regular penetration tests with security team
```

### Vulnerability Remediation

#### Vulnerability Triage Process

```bash
# 1. Categorize vulnerabilities by severity
# CRITICAL: CVSS 9.0-10.0 - Fix within 24-48 hours
# HIGH: CVSS 7.0-8.9 - Fix within 1 week
# MEDIUM: CVSS 4.0-6.9 - Fix within 2-4 weeks
# LOW: CVSS 0.1-3.9 - Fix during regular maintenance

# 2. Assess exploitability
# - Is there a known exploit?
# - Is it being actively exploited?
# - What is the attack vector?

# 3. Determine impact
# - What systems are affected?
# - What data is at risk?
# - What is the business impact?

# 4. Prioritize remediation
# Priority = Severity × Exploitability × Impact

# 5. Create remediation plan
# - Identify fix or workaround
# - Test in non-production environment
# - Schedule deployment
# - Verify fix effectiveness
```

#### Vulnerability Tracking

```sql
-- Create vulnerability tracking table
CREATE TABLE IF NOT EXISTS vulnerabilities (
    id SERIAL PRIMARY KEY,
    cve_id TEXT,
    severity TEXT,
    cvss_score NUMERIC(3,1),
    affected_component TEXT,
    description TEXT,
    discovered_date TIMESTAMP DEFAULT NOW(),
    remediation_status TEXT,
    remediation_date TIMESTAMP,
    assigned_to TEXT,
    notes TEXT
);

-- Track vulnerability remediation
INSERT INTO vulnerabilities (cve_id, severity, cvss_score, affected_component, description, remediation_status, assigned_to)
VALUES ('CVE-2024-1234', 'HIGH', 8.5, 'spring-boot-starter-web:3.1.0', 'Remote code execution vulnerability', 'IN_PROGRESS', 'security-team');

-- Query open vulnerabilities
SELECT cve_id, severity, affected_component, 
       NOW() - discovered_date AS days_open,
       assigned_to
FROM vulnerabilities
WHERE remediation_status != 'RESOLVED'
ORDER BY cvss_score DESC, discovered_date ASC;

-- Generate vulnerability report
SELECT severity,
       COUNT(*) AS total,
       COUNT(CASE WHEN remediation_status = 'RESOLVED' THEN 1 END) AS resolved,
       COUNT(CASE WHEN remediation_status != 'RESOLVED' THEN 1 END) AS open,
       AVG(EXTRACT(EPOCH FROM (remediation_date - discovered_date))/86400) AS avg_days_to_resolve
FROM vulnerabilities
WHERE discovered_date > NOW() - INTERVAL '90 days'
GROUP BY severity
ORDER BY 
    CASE severity
        WHEN 'CRITICAL' THEN 1
        WHEN 'HIGH' THEN 2
        WHEN 'MEDIUM' THEN 3
        WHEN 'LOW' THEN 4
    END;
```

## Access Review and Cleanup

### Quarterly Access Review

#### Database Access Review

```sql
-- Generate access review report
SELECT 
    u.usename AS username,
    u.valuntil AS password_expiry,
    ARRAY_AGG(DISTINCT r.rolname) AS roles,
    MAX(a.query_start) AS last_activity,
    NOW() - MAX(a.query_start) AS inactive_duration
FROM pg_user u
LEFT JOIN pg_auth_members am ON u.usesysid = am.member
LEFT JOIN pg_roles r ON am.roleid = r.oid
LEFT JOIN pg_stat_activity a ON u.usename = a.usename
WHERE u.usename NOT IN ('postgres', 'rdsadmin')
GROUP BY u.usename, u.valuntil
ORDER BY inactive_duration DESC NULLS FIRST;

-- Identify inactive users (no activity in 90 days)
SELECT usename, valuntil, 
       NOW() - MAX(query_start) AS days_inactive
FROM pg_user u
LEFT JOIN pg_stat_activity a ON u.usename = a.usename
WHERE u.usename NOT IN ('postgres', 'rdsadmin')
GROUP BY usename, valuntil
HAVING MAX(query_start) < NOW() - INTERVAL '90 days' OR MAX(query_start) IS NULL;

-- Review privileged users
SELECT usename, usesuper, usecreatedb, usecreaterole, usebypassrls
FROM pg_user
WHERE (usesuper OR usecreatedb OR usecreaterole OR usebypassrls)
  AND usename NOT IN ('postgres', 'rdsadmin')
ORDER BY usename;

-- Check for users with expired passwords
SELECT usename, valuntil,
       CASE 
           WHEN valuntil IS NULL THEN 'NO_EXPIRY'
           WHEN valuntil < NOW() THEN 'EXPIRED'
           WHEN valuntil < NOW() + INTERVAL '30 days' THEN 'EXPIRING_SOON'
           ELSE 'VALID'
       END AS password_status
FROM pg_user
WHERE usename NOT IN ('postgres', 'rdsadmin')
ORDER BY valuntil NULLS LAST;
```

#### AWS IAM Access Review

```bash
# List all IAM users and their last activity
aws iam generate-credential-report
sleep 10
aws iam get-credential-report --query 'Content' --output text | base64 -d > iam-credential-report.csv

# Analyze credential report
cat iam-credential-report.csv | awk -F',' '{
    if ($5 == "true" && $11 == "N/A") {
        print "Inactive user: " $1 " (password enabled, never used)"
    }
}'

# List users with old access keys (> 90 days)
aws iam list-users --query 'Users[*].UserName' --output text | while read user; do
    aws iam list-access-keys --user-name $user --query 'AccessKeyMetadata[*].[AccessKeyId,CreateDate]' --output text | while read key date; do
        age_days=$(( ($(date +%s) - $(date -d "$date" +%s)) / 86400 ))
        if [ $age_days -gt 90 ]; then
            echo "Old access key: $user - $key ($age_days days old)"
        fi
    done
done

# List users with MFA disabled
aws iam list-users --query 'Users[*].UserName' --output text | while read user; do
    mfa=$(aws iam list-mfa-devices --user-name $user --query 'MFADevices[*].SerialNumber' --output text)
    if [ -z "$mfa" ]; then
        echo "MFA not enabled: $user"
    fi
done

# Review unused roles
aws iam list-roles --query 'Roles[*].[RoleName,CreateDate]' --output text | while read role date; do
    last_used=$(aws iam get-role --role-name $role --query 'Role.RoleLastUsed.LastUsedDate' --output text)
    if [ "$last_used" == "None" ]; then
        echo "Never used role: $role (created $date)"
    fi
done
```

#### Access Cleanup Procedures

```sql
-- Disable inactive database users
DO $$
DECLARE
    inactive_user RECORD;
BEGIN
    FOR inactive_user IN 
        SELECT usename
        FROM pg_user u
        LEFT JOIN pg_stat_activity a ON u.usename = a.usename
        WHERE u.usename NOT IN ('postgres', 'rdsadmin')
        GROUP BY usename
        HAVING MAX(query_start) < NOW() - INTERVAL '90 days' OR MAX(query_start) IS NULL
    LOOP
        EXECUTE format('ALTER USER %I WITH NOLOGIN', inactive_user.usename);
        RAISE NOTICE 'Disabled user: %', inactive_user.usename;
        
        -- Log the action
        INSERT INTO user_audit_log (username, action, performed_by, timestamp)
        VALUES (inactive_user.usename, 'USER_DISABLED_INACTIVE', current_user, NOW());
    END LOOP;
END $$;

-- Revoke excessive privileges
-- Review and revoke superuser privileges if not needed
SELECT usename FROM pg_user 
WHERE usesuper = true 
  AND usename NOT IN ('postgres', 'rdsadmin', 'admin');

-- Force password reset for users with expired passwords
UPDATE pg_user
SET valuntil = NOW()
WHERE valuntil < NOW()
  AND usename NOT IN ('postgres', 'rdsadmin');
```

```bash
# AWS IAM cleanup

# Deactivate old access keys
aws iam list-users --query 'Users[*].UserName' --output text | while read user; do
    aws iam list-access-keys --user-name $user --query 'AccessKeyMetadata[*].[AccessKeyId,CreateDate]' --output text | while read key date; do
        age_days=$(( ($(date +%s) - $(date -d "$date" +%s)) / 86400 ))
        if [ $age_days -gt 90 ]; then
            echo "Deactivating old access key: $user - $key"
            aws iam update-access-key --user-name $user --access-key-id $key --status Inactive
        fi
    done
done

# Remove unused roles
aws iam list-roles --query 'Roles[?contains(RoleName, `Unused`)].RoleName' --output text | while read role; do
    echo "Deleting unused role: $role"
    # Detach policies first
    aws iam list-attached-role-policies --role-name $role --query 'AttachedPolicies[*].PolicyArn' --output text | while read policy; do
        aws iam detach-role-policy --role-name $role --policy-arn $policy
    done
    # Delete role
    aws iam delete-role --role-name $role
done
```

### Access Review Documentation

#### Access Review Template

```markdown
# Quarterly Access Review - Q1 2025

## Review Date
2025-01-15

## Reviewer
Security Team

## Scope
- Database users and roles
- AWS IAM users and roles
- Application service accounts
- Third-party integrations

## Findings

### Database Access
- Total users reviewed: 45
- Inactive users identified: 8
- Excessive privileges identified: 3
- Password expiry issues: 5

### AWS IAM Access
- Total users reviewed: 32
- MFA not enabled: 4
- Old access keys (>90 days): 7
- Unused roles: 5

### Actions Taken
1. Disabled 8 inactive database users
2. Revoked superuser privilege from 2 users
3. Forced password reset for 5 users
4. Deactivated 7 old access keys
5. Enabled MFA for 4 users
6. Deleted 5 unused IAM roles

### Recommendations
1. Implement automated access key rotation
2. Enforce MFA for all users
3. Review and update password policy
4. Implement just-in-time access for privileged operations

## Sign-off
- Reviewed by: [Security Manager]
- Approved by: [CISO]
- Date: 2025-01-15
```

## Security Maintenance Schedule

### Daily Tasks

- [ ] Review security alerts and notifications
- [ ] Monitor failed authentication attempts
- [ ] Check CloudTrail for suspicious API calls
- [ ] Review application security logs
- [ ] Verify backup completion

### Weekly Tasks

- [ ] Review audit logs for anomalies
- [ ] Check for security patches and updates
- [ ] Review vulnerability scan results
- [ ] Verify SSL/TLS certificate status
- [ ] Review access logs for unusual patterns

### Monthly Tasks

- [ ] Conduct access review for new/modified users
- [ ] Review and update security group rules
- [ ] Analyze security metrics and trends
- [ ] Test incident response procedures
- [ ] Review and update security documentation

### Quarterly Tasks

- [ ] Comprehensive access review and cleanup
- [ ] Security vulnerability assessment
- [ ] Penetration testing
- [ ] Compliance audit (PCI-DSS, GDPR, SOC 2)
- [ ] Review and update security policies
- [ ] Disaster recovery testing
- [ ] Security awareness training

### Annual Tasks

- [ ] Comprehensive security audit
- [ ] Review and update incident response plan
- [ ] Review and update business continuity plan
- [ ] Encryption key rotation (if not automated)
- [ ] Third-party security assessment
- [ ] Compliance certification renewal

## Security Metrics and KPIs

### Key Performance Indicators

```sql
-- Security KPI dashboard

-- 1. Mean Time to Detect (MTTD)
SELECT AVG(EXTRACT(EPOCH FROM (detected_at - occurred_at))/3600) AS mttd_hours
FROM security_incidents
WHERE detected_at > NOW() - INTERVAL '90 days';

-- 2. Mean Time to Respond (MTTR)
SELECT AVG(EXTRACT(EPOCH FROM (resolved_at - detected_at))/3600) AS mttr_hours
FROM security_incidents
WHERE resolved_at > NOW() - INTERVAL '90 days';

-- 3. Vulnerability remediation rate
SELECT 
    COUNT(CASE WHEN remediation_status = 'RESOLVED' THEN 1 END)::FLOAT / COUNT(*) * 100 AS remediation_rate
FROM vulnerabilities
WHERE discovered_date > NOW() - INTERVAL '90 days';

-- 4. Failed authentication rate
SELECT 
    COUNT(CASE WHEN event_type = 'LOGIN_FAILED' THEN 1 END)::FLOAT / 
    COUNT(*) * 100 AS failed_auth_rate
FROM audit_log
WHERE event_time > NOW() - INTERVAL '30 days';

-- 5. Compliance score
SELECT 
    COUNT(CASE WHEN status = 'COMPLIANT' THEN 1 END)::FLOAT / 
    COUNT(*) * 100 AS compliance_score
FROM compliance_checks
WHERE check_date > NOW() - INTERVAL '30 days';
```

## Incident Response Procedures

### Security Incident Classification

**Severity Levels**

- **Critical (P1)**: Active breach, data exfiltration, ransomware
  - Response time: Immediate (< 15 minutes)
  - Escalation: CISO, Executive team
  
- **High (P2)**: Attempted breach, vulnerability exploitation, DDoS
  - Response time: < 1 hour
  - Escalation: Security team, IT management
  
- **Medium (P3)**: Suspicious activity, policy violations, failed attacks
  - Response time: < 4 hours
  - Escalation: Security team
  
- **Low (P4)**: Security warnings, minor policy violations
  - Response time: < 24 hours
  - Escalation: Security analyst

### Incident Response Workflow

```bash
# 1. Detection and Analysis
# - Review alerts and logs
# - Determine incident severity
# - Gather initial evidence

# 2. Containment
# Isolate affected systems
aws ec2 modify-instance-attribute \
  --instance-id i-1234567890abcdef0 \
  --groups sg-isolated

# Disable compromised user accounts
aws iam update-access-key \
  --user-name compromised-user \
  --access-key-id AKIAIOSFODNN7EXAMPLE \
  --status Inactive

# 3. Eradication
# - Remove malware
# - Patch vulnerabilities
# - Reset credentials

# 4. Recovery
# - Restore from clean backups
# - Verify system integrity
# - Monitor for reinfection

# 5. Post-Incident
# - Document incident
# - Conduct lessons learned
# - Update security controls
```

### Incident Documentation Template

```markdown
# Security Incident Report

## Incident ID
INC-2025-001

## Incident Summary
- **Date/Time**: 2025-01-15 14:30 UTC
- **Severity**: High (P2)
- **Status**: Resolved
- **Incident Type**: Unauthorized access attempt

## Timeline
- 14:30 - Suspicious login attempts detected
- 14:35 - Security team notified
- 14:40 - Account disabled, investigation started
- 15:15 - Root cause identified
- 16:00 - Remediation completed
- 16:30 - Monitoring confirmed no further activity

## Impact Assessment
- **Systems Affected**: Production database
- **Data Compromised**: None
- **Users Affected**: 0
- **Business Impact**: Minimal

## Root Cause
Compromised credentials from phishing attack

## Actions Taken
1. Disabled compromised account
2. Forced password reset for all users
3. Enabled MFA requirement
4. Updated security awareness training

## Lessons Learned
- Need for mandatory MFA
- Improved phishing detection
- Enhanced monitoring alerts

## Follow-up Actions
- [ ] Implement mandatory MFA (Due: 2025-01-20)
- [ ] Conduct security awareness training (Due: 2025-01-25)
- [ ] Review and update access policies (Due: 2025-01-30)
```

## Related Documentation

- [Database Maintenance](database-maintenance.md)
- [Backup and Recovery](backup-recovery.md)
- [Monitoring and Alerting](../monitoring/monitoring-alerting.md)
- [Disaster Recovery](../deployment/disaster-recovery.md)

---

**Last Updated**: 2025-01-15
**Document Owner**: Security Team
**Review Frequency**: Quarterly
