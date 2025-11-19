---
adr_number: 054
title: "Data Loss Prevention (DLP) Strategy"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [015, 016, 053, 055]
affected_viewpoints: ["functional", "information", "operational"]
affected_perspectives: ["security", "availability"]
---

# ADR-054: Data Loss Prevention (DLP) Strategy

## Status

**Accepted** - 2025-10-25

## Context

### Problem Statement

The Enterprise E-Commerce Platform handles sensitive customer data that requires comprehensive protection against:

- Data exfiltration by external attackers
- Insider threats and unauthorized access
- Accidental data exposure
- Compliance violations (GDPR, PCI-DSS)
- Data breaches and leaks
- Unauthorized data sharing

Taiwan's regulatory environment and cyber threat landscape require:

- Compliance with Taiwan Personal Data Protection Act
- Protection against state-sponsored data theft
- Secure handling of payment card data (PCI-DSS)
- Audit trails for data access and usage
- Data residency and sovereignty requirements

### Business Context

**Business Drivers**:

- Protect customer trust and platform reputation
- Comply with data protection regulations
- Prevent financial losses from data breaches
- Maintain PCI-DSS compliance for payment processing
- Enable secure business operations

**Constraints**:

- Must not impact system performance significantly
- Cannot block legitimate business operations
- Must support data analytics and reporting
- Budget: $3,000/month for DLP tools and services

### Technical Context

**Current State**:

- Basic encryption at rest and in transit (ADR-016)
- RBAC for access control (ADR-015)
- No data classification system
- No data exfiltration detection
- No data masking for non-production environments
- Manual data access auditing

**Requirements**:

- Sensitive data identification and classification
- Data exfiltration prevention
- Data masking for non-production environments
- Access control and monitoring
- Audit trails for compliance
- Automated policy enforcement

## Decision Drivers

1. **Data Protection**: Prevent unauthorized data access and exfiltration
2. **Compliance**: Meet GDPR, PCI-DSS, Taiwan PDPA requirements
3. **Performance**: Minimal impact on system performance (< 5% overhead)
4. **Usability**: Enable legitimate business operations
5. **Auditability**: Complete audit trails for compliance
6. **Scalability**: Handle growing data volumes
7. **Cost**: Optimize operational costs
8. **Automation**: Automated policy enforcement and monitoring

## Considered Options

### Option 1: Comprehensive DLP Strategy (Recommended)

**Description**: Multi-layered data loss prevention with classification, monitoring, masking, and access control

**Components**:

- **Data Classification**: Identify and tag sensitive data (PII, PCI, confidential)
- **Database Activity Monitoring**: Monitor all database access and queries
- **API Call Auditing**: Track all API calls accessing sensitive data
- **Data Masking**: Mask sensitive data in non-production environments
- **Access Control**: Least privilege principle with periodic reviews
- **Anomaly Detection**: Detect unusual data access patterns
- **Audit Logging**: Comprehensive audit trails for compliance

**Pros**:

- ✅ Defense-in-depth data protection
- ✅ Compliance-ready (GDPR, PCI-DSS, Taiwan PDPA)
- ✅ Automated policy enforcement
- ✅ Comprehensive audit trails
- ✅ Supports legitimate business operations
- ✅ Scalable architecture
- ✅ Cost-effective ($3,000/month)

**Cons**:

- ⚠️ Implementation complexity
- ⚠️ Requires data classification effort
- ⚠️ Potential false positives
- ⚠️ Operational overhead

**Cost**: $3,000/month (AWS Macie $1,500, monitoring tools $1,500)

**Risk**: **Low** - Proven DLP practices

### Option 2: Basic Data Protection (Encryption Only)

**Description**: Rely on encryption and basic access control

**Pros**:

- ✅ Simple to implement
- ✅ Low operational overhead
- ✅ Low cost

**Cons**:

- ❌ No data exfiltration detection
- ❌ No data masking
- ❌ Limited audit capabilities
- ❌ Compliance gaps
- ❌ No anomaly detection

**Cost**: $0 (already implemented)

**Risk**: **High** - Insufficient data protection

### Option 3: Third-Party DLP Solution (Symantec, McAfee)

**Description**: Deploy enterprise DLP solution

**Pros**:

- ✅ Advanced DLP features
- ✅ Proven enterprise solution
- ✅ Professional support

**Cons**:

- ❌ Very high cost ($10,000-20,000/month)
- ❌ Complex deployment
- ❌ Performance overhead
- ❌ Vendor lock-in

**Cost**: $15,000/month

**Risk**: **Medium** - High cost and complexity

## Decision Outcome

**Chosen Option**: **Comprehensive DLP Strategy (Option 1)**

### Rationale

Comprehensive DLP strategy was selected for the following reasons:

1. **Compliance**: Meets GDPR, PCI-DSS, and Taiwan PDPA requirements
2. **Cost-Effective**: $3,000/month vs $15,000+ for enterprise DLP
3. **AWS Native**: Leverages AWS services (Macie, CloudTrail, GuardDuty)
4. **Scalable**: Handles platform growth automatically
5. **Automated**: Policy enforcement and monitoring with minimal overhead
6. **Flexible**: Supports legitimate business operations
7. **Comprehensive**: Multi-layered protection against data loss

### Data Classification

**Sensitivity Tiers**:

**Tier 1: Highly Sensitive (PII, PCI)**

- Customer names, addresses, phone numbers
- Email addresses
- Payment card numbers (PAN)
- Bank account information
- Government ID numbers
- Passwords and authentication credentials

**Tier 2: Sensitive (Business Data)**

- Order details and history
- Inventory levels and pricing
- Customer purchase patterns
- Business analytics data
- Internal communications

**Tier 3: Public (Non-Sensitive)**

- Product catalog
- Public marketing content
- General system information

**Classification Implementation**:

```java
@Entity
@Table(name = "customers")
public class Customer {
    
    @Id
    private String id;
    
    @Column
    @DataClassification(tier = DataTier.TIER1, type = DataType.PII)
    private String name;
    
    @Column
    @DataClassification(tier = DataTier.TIER1, type = DataType.PII)
    @Encrypted
    private String email;
    
    @Column
    @DataClassification(tier = DataTier.TIER1, type = DataType.PII)
    @Encrypted
    private String phoneNumber;
    
    @Column
    @DataClassification(tier = DataTier.TIER1, type = DataType.PCI)
    @Encrypted
    @Masked
    private String paymentCardLast4;
}
```

### Database Activity Monitoring

**Monitoring Approach**:

- Enable PostgreSQL audit logging
- Monitor all SELECT, INSERT, UPDATE, DELETE operations
- Track query patterns and data volumes
- Detect anomalous queries (large result sets, unusual times)
- Alert on suspicious activity

**Implementation**:

```sql
-- Enable PostgreSQL audit logging
ALTER SYSTEM SET log_statement = 'all';
ALTER SYSTEM SET log_connections = 'on';
ALTER SYSTEM SET log_disconnections = 'on';
ALTER SYSTEM SET log_duration = 'on';

-- Create audit table
CREATE TABLE audit_log (
    id SERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    user_id VARCHAR(255),
    session_id VARCHAR(255),
    query_text TEXT,
    rows_affected INTEGER,
    execution_time_ms INTEGER,
    source_ip VARCHAR(45),
    application_name VARCHAR(255)
);

-- Audit trigger for sensitive tables
CREATE OR REPLACE FUNCTION audit_sensitive_data()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO audit_log (user_id, query_text, rows_affected)
    VALUES (current_user, current_query(), 1);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER customer_audit_trigger
AFTER INSERT OR UPDATE OR DELETE ON customers
FOR EACH ROW EXECUTE FUNCTION audit_sensitive_data();
```

**Anomaly Detection**:

```python
# Detect anomalous database queries
def detect_anomalous_queries():
    # Large result set (> 10,000 rows)
    large_queries = """
    SELECT user_id, query_text, rows_affected
    FROM audit_log
    WHERE rows_affected > 10000
    AND timestamp > NOW() - INTERVAL '1 hour'
    """
    
    # Unusual query time (3 AM - 6 AM)
    unusual_time_queries = """
    SELECT user_id, query_text, timestamp
    FROM audit_log
    WHERE EXTRACT(HOUR FROM timestamp) BETWEEN 3 AND 6
    AND timestamp > NOW() - INTERVAL '24 hours'
    """
    
    # Bulk data export
    bulk_export_queries = """
    SELECT user_id, COUNT(*) as query_count, SUM(rows_affected) as total_rows
    FROM audit_log
    WHERE timestamp > NOW() - INTERVAL '1 hour'
    GROUP BY user_id
    HAVING SUM(rows_affected) > 50000
    """
    
    # Send alerts for anomalies
    for query in execute_queries([large_queries, unusual_time_queries, bulk_export_queries]):
        send_security_alert(query)
```

### API Call Auditing

**Monitoring Approach**:

- Log all API calls accessing sensitive data
- Track request/response payloads (sanitized)
- Monitor data volume per user/IP
- Detect unusual access patterns
- Alert on suspicious activity

**Implementation**:

```java
@Aspect
@Component
public class DataAccessAuditAspect {
    
    @Around("@annotation(DataAccess)")
    public Object auditDataAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        DataAccess annotation = getAnnotation(joinPoint);
        String userId = getCurrentUserId();
        String ipAddress = getCurrentIpAddress();
        
        // Log access attempt
        AuditLog auditLog = AuditLog.builder()
            .timestamp(Instant.now())
            .userId(userId)
            .ipAddress(ipAddress)
            .dataType(annotation.dataType())
            .operation(annotation.operation())
            .resourceId(getResourceId(joinPoint))
            .build();
        
        try {
            Object result = joinPoint.proceed();
            
            // Log successful access
            auditLog.setStatus("SUCCESS");
            auditLog.setRowsAccessed(getRowCount(result));
            
            // Check for anomalous access
            if (isAnomalousAccess(auditLog)) {
                sendSecurityAlert(auditLog);
            }
            
            return result;
        } catch (Exception e) {
            // Log failed access
            auditLog.setStatus("FAILED");
            auditLog.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            auditLogRepository.save(auditLog);
        }
    }
    
    private boolean isAnomalousAccess(AuditLog auditLog) {
        // Check for large data access
        if (auditLog.getRowsAccessed() > 1000) {
            return true;
        }
        
        // Check for unusual time
        int hour = LocalDateTime.now().getHour();
        if (hour >= 3 && hour <= 6) {
            return true;
        }
        
        // Check for velocity (multiple accesses in short time)
        long recentAccesses = countRecentAccesses(auditLog.getUserId(), Duration.ofMinutes(5));
        if (recentAccesses > 100) {
            return true;
        }
        
        return false;
    }
}
```

### Data Masking

**Masking Strategy**:

**Production Environment**: No masking (encrypted data)

**Staging Environment**: Partial masking

- Email: `j***@example.com`
- Phone: `+886-9**-***-123`
- Credit Card: `****-****-****-1234`
- Name: `John D***`

**Development Environment**: Full masking

- Email: `user{id}@example.com`
- Phone: `+886-900-000-{id}`
- Credit Card: `4111-1111-1111-{id}`
- Name: `Test User {id}`

**Test Environment**: Synthetic data

- Generated fake data using Faker library
- Realistic but not real customer data

**Implementation**:

```java
@Service
public class DataMaskingService {
    
    public String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];
        
        if (localPart.length() <= 2) {
            return "*".repeat(localPart.length()) + "@" + domain;
        }
        
        return localPart.charAt(0) + "*".repeat(localPart.length() - 2) + 
               localPart.charAt(localPart.length() - 1) + "@" + domain;
    }
    
    public String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return phone;
        }
        
        String last4 = phone.substring(phone.length() - 4);
        return "*".repeat(phone.length() - 4) + last4;
    }
    
    public String maskCreditCard(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return cardNumber;
        }
        
        String last4 = cardNumber.substring(cardNumber.length() - 4);
        return "****-****-****-" + last4;
    }
    
    public Customer maskCustomer(Customer customer, Environment environment) {
        if (environment == Environment.PRODUCTION) {
            return customer; // No masking in production
        }
        
        Customer masked = customer.clone();
        
        if (environment == Environment.STAGING) {
            // Partial masking
            masked.setEmail(maskEmail(customer.getEmail()));
            masked.setPhone(maskPhone(customer.getPhone()));
        } else {
            // Full masking
            masked.setEmail("user" + customer.getId() + "@example.com");
            masked.setPhone("+886-900-000-" + customer.getId());
            masked.setName("Test User " + customer.getId());
        }
        
        return masked;
    }
}
```

### Access Control

**Least Privilege Principle**:

- Users have minimum permissions required for their role
- Temporary elevated access for specific tasks
- Automatic permission expiration
- Regular permission reviews (quarterly)

**Access Control Matrix**:

| Role | Customer PII | Payment Data | Order Data | Product Data |
|------|--------------|--------------|------------|--------------|
| Customer | Own data only | Own data only | Own data only | Read all |
| Customer Support | Read all | Read last 4 digits | Read all | Read all |
| Admin | Read/Write all | Read last 4 digits | Read/Write all | Read/Write all |
| Developer | No access | No access | Read staging | Read/Write all |
| Analyst | Masked data | No access | Read all | Read all |

**Implementation**:

```java
@Service
public class DataAccessControlService {
    
    public boolean canAccessCustomerData(String userId, String customerId, AccessType accessType) {
        User user = userRepository.findById(userId).orElseThrow();
        
        // Customer can only access own data
        if (user.hasRole("CUSTOMER")) {
            return userId.equals(customerId) && accessType == AccessType.READ;
        }
        
        // Customer support can read all customer data
        if (user.hasRole("CUSTOMER_SUPPORT")) {
            return accessType == AccessType.READ;
        }
        
        // Admin can read/write all customer data
        if (user.hasRole("ADMIN")) {
            return true;
        }
        
        // Developer has no access to production customer data
        if (user.hasRole("DEVELOPER")) {
            return false;
        }
        
        // Analyst can read masked data
        if (user.hasRole("ANALYST")) {
            return accessType == AccessType.READ_MASKED;
        }
        
        return false;
    }
    
    public void requestTemporaryAccess(String userId, String resourceType, Duration duration, String justification) {
        // Create temporary access request
        TemporaryAccessRequest request = TemporaryAccessRequest.builder()
            .userId(userId)
            .resourceType(resourceType)
            .duration(duration)
            .justification(justification)
            .status(RequestStatus.PENDING)
            .build();
        
        temporaryAccessRepository.save(request);
        
        // Notify approver
        notificationService.sendAccessRequestNotification(request);
    }
    
    public void reviewPermissions() {
        // Quarterly permission review
        List<User> users = userRepository.findAll();
        
        for (User user : users) {
            // Check for unused permissions
            List<Permission> unusedPermissions = findUnusedPermissions(user, Duration.ofDays(90));
            
            if (!unusedPermissions.isEmpty()) {
                // Notify manager for review
                notificationService.sendPermissionReviewNotification(user, unusedPermissions);
            }
            
            // Check for expired temporary access
            List<TemporaryAccess> expiredAccess = findExpiredTemporaryAccess(user);
            
            for (TemporaryAccess access : expiredAccess) {
                revokeTemporaryAccess(access);
            }
        }
    }
}
```

### AWS Macie Integration

**Sensitive Data Discovery**:

- Automatically scan S3 buckets for sensitive data
- Identify PII, PCI, and confidential data
- Generate data classification reports
- Alert on unencrypted sensitive data

**Implementation**:

```python
# Enable Macie via CDK
macie = aws_macie.CfnSession(
    self, "Macie",
    status="ENABLED",
    finding_publishing_frequency="FIFTEEN_MINUTES"
)

# Create classification job
classification_job = aws_macie.CfnClassificationJob(
    self, "ClassificationJob",
    job_type="SCHEDULED",
    name="SensitiveDataDiscovery",
    s3_job_definition=aws_macie.CfnClassificationJob.S3JobDefinitionProperty(
        bucket_definitions=[
            aws_macie.CfnClassificationJob.S3BucketDefinitionProperty(
                account_id=account_id,
                buckets=[bucket_name]
            )
        ]
    ),
    schedule_frequency=aws_macie.CfnClassificationJob.JobScheduleFrequencyProperty(
        daily_schedule={}
    )
)
```

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | Medium | Data masking in non-prod environments | Automated masking, synthetic data |
| Data Analysts | Medium | Access to masked data only | Provide aggregated data, masked datasets |
| Customer Support | Low | Audit logging of data access | Clear policies, training |
| Security Team | Positive | Enhanced data protection | Regular reviews, automated monitoring |
| Compliance Team | Positive | Audit trails for compliance | Automated reporting |

### Impact Radius

**Selected Impact Radius**: **System**

Affects:

- All databases and data stores
- All API endpoints accessing sensitive data
- All non-production environments
- Data analytics and reporting
- Access control policies

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| False positive alerts | Medium | Low | Alert tuning, feedback loop |
| Performance overhead | Low | Medium | Optimize queries, caching |
| Data masking errors | Low | High | Comprehensive testing, validation |
| Access control bypass | Low | Critical | Regular audits, penetration testing |
| Compliance violations | Low | Critical | Automated compliance checks, regular reviews |

**Overall Risk Level**: **Low**

## Implementation Plan

### Phase 1: Data Classification (Week 1-2)

- [ ] Define data classification tiers
- [ ] Annotate database schemas
- [ ] Implement classification metadata
- [ ] Create data inventory
- [ ] Document data flows

### Phase 2: Monitoring and Auditing (Week 3-4)

- [ ] Enable database audit logging
- [ ] Implement API call auditing
- [ ] Create audit log storage
- [ ] Set up anomaly detection
- [ ] Configure alerts

### Phase 3: Data Masking (Week 5-6)

- [ ] Implement masking service
- [ ] Create masked datasets for staging
- [ ] Generate synthetic data for dev/test
- [ ] Test masking accuracy
- [ ] Deploy to non-production environments

### Phase 4: Access Control (Week 7-8)

- [ ] Implement access control service
- [ ] Create access control matrix
- [ ] Set up temporary access workflow
- [ ] Implement permission reviews
- [ ] Enable AWS Macie
- [ ] Test and validate

### Rollback Strategy

**Trigger Conditions**:

- Critical performance degradation (> 10% overhead)
- Data masking errors exposing real data
- Access control blocking legitimate operations
- Excessive false positive alerts

**Rollback Steps**:

1. Disable anomaly detection alerts
2. Relax access control policies
3. Disable data masking temporarily
4. Investigate and fix issues
5. Gradually re-enable features

**Rollback Time**: < 2 hours

## Monitoring and Success Criteria

### Success Metrics

- ✅ Data classification coverage: 100% of sensitive data
- ✅ Audit log coverage: 100% of sensitive data access
- ✅ Data masking accuracy: 100% (no real data in non-prod)
- ✅ Access control compliance: 100%
- ✅ Anomaly detection accuracy: > 90%
- ✅ Performance overhead: < 5%
- ✅ Zero data breach incidents

### Monitoring Plan

**CloudWatch Metrics**:

- `dlp.data.access.count` (count by data tier)
- `dlp.anomaly.detected` (count by type)
- `dlp.access.denied` (count by reason)
- `dlp.audit.log.size` (bytes)
- `dlp.masking.errors` (count)

**Alerts**:

- Anomalous data access detected
- Large data export (> 10,000 rows)
- Unusual access time (3-6 AM)
- Access control violation
- Data masking error

**Review Schedule**:

- Daily: Review anomalous access alerts
- Weekly: Analyze data access patterns
- Monthly: Permission review
- Quarterly: Compliance audit

## Consequences

### Positive Consequences

- ✅ **Data Protection**: Comprehensive protection against data loss
- ✅ **Compliance**: Meets GDPR, PCI-DSS, Taiwan PDPA requirements
- ✅ **Auditability**: Complete audit trails for compliance
- ✅ **Insider Threat Protection**: Detect and prevent insider threats
- ✅ **Cost-Effective**: $3,000/month for enterprise-grade DLP
- ✅ **Automated**: Policy enforcement with minimal overhead

### Negative Consequences

- ⚠️ **Implementation Complexity**: Multiple components to implement
- ⚠️ **Operational Overhead**: Monitoring and alert management
- ⚠️ **Performance Impact**: 3-5% overhead for auditing
- ⚠️ **False Positives**: Potential for legitimate operations to trigger alerts
- ⚠️ **Data Masking Effort**: Initial effort to create masked datasets

### Technical Debt

**Identified Debt**:

1. Manual data classification (acceptable initially)
2. Basic anomaly detection (rule-based)
3. No ML-powered threat detection
4. Limited data lineage tracking

**Debt Repayment Plan**:

- **Q2 2026**: Implement ML-powered anomaly detection
- **Q3 2026**: Automate data classification with Macie
- **Q4 2026**: Implement data lineage tracking
- **2027**: Integrate with CASB for cloud data protection

## Related Decisions

- [ADR-016: Data Encryption Strategy](016-data-encryption-strategy.md) - Data encryption at rest and in transit
- [ADR-053: Security Monitoring and Incident Response](053-security-monitoring-incident-response.md) - Security monitoring integration
- [ADR-055: Vulnerability Management and Patching Strategy](055-vulnerability-management-patching-strategy.md) - Vulnerability management

## Notes

### Data Classification Tags

```java
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface DataClassification {
    DataTier tier();
    DataType type();
    boolean encrypted() default false;
    boolean masked() default false;
}

public enum DataTier {
    TIER1, // Highly Sensitive (PII, PCI)
    TIER2, // Sensitive (Business Data)
    TIER3  // Public (Non-Sensitive)
}

public enum DataType {
    PII,           // Personally Identifiable Information
    PCI,           // Payment Card Industry data
    CONFIDENTIAL,  // Business confidential
    PUBLIC         // Public information
}
```

### Compliance Mapping

- **GDPR Article 32**: Security of processing (encryption, audit logs)
- **PCI-DSS Requirement 10**: Track and monitor all access to network resources and cardholder data
- **Taiwan PDPA Article 27**: Security measures for personal data protection

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-25  
**Next Review**: 2026-01-25 (Quarterly)
