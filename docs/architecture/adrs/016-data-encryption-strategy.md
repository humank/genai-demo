---
adr_number: 016
title: "Data Encryption Strategy (At Rest and In Transit)"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [001, 014, 054]
affected_viewpoints: ["information", "deployment", "operational"]
affected_perspectives: ["security", "performance", "availability"]
---

# ADR-016: Data Encryption Strategy (At Rest and In Transit)

## Status

**Accepted** - 2025-10-25

## Context

### Problem Statement

The Enterprise E-Commerce Platform handles sensitive data including:
- Customer personal information (PII)
- Payment card data (PCI-DSS compliance required)
- Authentication credentials
- Business confidential data

We need a comprehensive encryption strategy that:
- Protects data at rest in databases and file storage
- Secures data in transit between services and clients
- Meets regulatory compliance (GDPR, PCI-DSS, Taiwan Personal Data Protection Act)
- Maintains acceptable performance (< 10ms encryption overhead)
- Enables secure key management and rotation
- Supports data residency requirements

### Business Context

**Business Drivers**:
- PCI-DSS Level 1 compliance required for payment processing
- GDPR compliance for EU customers
- Taiwan Personal Data Protection Act compliance
- Customer trust and brand reputation
- Regulatory penalties avoidance (up to 4% of annual revenue)

**Constraints**:
- Must encrypt payment card data (PCI-DSS Requirement 3)
- Must encrypt PII (GDPR Article 32)
- Performance impact < 10ms per operation
- Key rotation without service downtime
- Budget: $2,000/month for encryption infrastructure

### Technical Context

**Current State**:
- PostgreSQL database on AWS RDS
- S3 for file storage
- Redis for caching
- Kafka for event streaming
- Spring Boot microservices

**Requirements**:
- Encrypt sensitive data at rest
- Encrypt all data in transit (TLS 1.3)
- Secure key management
- Key rotation capability
- Audit trail for encryption operations
- Performance: < 10ms overhead

## Decision Drivers

1. **Compliance**: Meet PCI-DSS, GDPR, local regulations
2. **Security**: Industry-standard encryption algorithms
3. **Performance**: Minimal performance impact
4. **Key Management**: Secure, auditable key lifecycle
5. **Scalability**: Support millions of encrypted records
6. **Operational**: Automated key rotation, monitoring
7. **Cost**: Within budget constraints
8. **Flexibility**: Support multiple encryption methods

## Considered Options

### Option 1: AWS KMS with Envelope Encryption

**Description**: Use AWS KMS for key management, envelope encryption for data

**Pros**:
- ✅ Managed key lifecycle (rotation, auditing)
- ✅ FIPS 140-2 Level 2 validated
- ✅ Integrated with AWS services (RDS, S3, EBS)
- ✅ Automatic key rotation
- ✅ CloudTrail audit logging
- ✅ Envelope encryption reduces KMS API calls
- ✅ Supports customer-managed keys (CMK)
- ✅ Multi-region key replication

**Cons**:
- ⚠️ AWS vendor lock-in
- ⚠️ API rate limits (1200 req/sec per CMK)
- ⚠️ Cost per API call ($0.03 per 10K requests)
- ⚠️ Network latency for KMS calls

**Cost**: $1,500/month (KMS + API calls)

**Risk**: **Low** - Proven AWS service

### Option 2: HashiCorp Vault

**Description**: Self-managed encryption and key management

**Pros**:
- ✅ Cloud-agnostic (no vendor lock-in)
- ✅ Advanced features (dynamic secrets, PKI)
- ✅ Fine-grained access control
- ✅ Audit logging
- ✅ Multi-cloud support

**Cons**:
- ❌ Self-managed infrastructure (operational overhead)
- ❌ High availability setup complex
- ❌ Additional infrastructure costs
- ❌ Team learning curve
- ❌ Maintenance burden

**Cost**: $3,000/month (infrastructure + operations)

**Risk**: **Medium** - Operational complexity

### Option 3: Application-Level Encryption Only

**Description**: Encrypt in application code, manage keys manually

**Pros**:
- ✅ Full control over encryption
- ✅ No external dependencies
- ✅ Low cost

**Cons**:
- ❌ Manual key management (high risk)
- ❌ No automatic key rotation
- ❌ Difficult to audit
- ❌ Key storage security concerns
- ❌ Not compliant with PCI-DSS
- ❌ High operational risk

**Cost**: $0

**Risk**: **High** - Security and compliance risks

### Option 4: Database-Level Encryption Only

**Description**: Rely on RDS encryption, no application-level encryption

**Pros**:
- ✅ Easy to enable
- ✅ Transparent to application
- ✅ Low cost

**Cons**:
- ❌ Coarse-grained (encrypts entire database)
- ❌ No field-level encryption
- ❌ Keys accessible to DBAs
- ❌ Not sufficient for PCI-DSS
- ❌ Cannot selectively encrypt fields

**Cost**: Included in RDS

**Risk**: **High** - Insufficient for compliance

## Decision Outcome

**Chosen Option**: **AWS KMS with Envelope Encryption + Multi-Layer Encryption**

### Rationale

We will implement a multi-layer encryption strategy:

1. **Data in Transit**: TLS 1.3 for all network communication
2. **Data at Rest - Infrastructure Level**: AWS service encryption (RDS, S3, EBS)
3. **Data at Rest - Application Level**: Field-level encryption for sensitive data using AWS KMS
4. **Key Management**: AWS KMS for centralized key management

**Why AWS KMS**:
- Managed service reduces operational overhead
- FIPS 140-2 Level 2 compliance
- Integrated with AWS services
- Automatic key rotation
- Comprehensive audit logging
- Cost-effective for our scale

**Why Multi-Layer**:
- Defense in depth
- Compliance requirements (PCI-DSS requires field-level encryption)
- Granular control over sensitive data
- Performance optimization (encrypt only what's necessary)

**Encryption Strategy by Data Type**:

| Data Type | Encryption Method | Key Type | Rotation |
|-----------|------------------|----------|----------|
| Payment Card Data | AES-256 field-level | AWS KMS CMK | 90 days |
| PII (email, phone) | AES-256 field-level | AWS KMS CMK | 90 days |
| Passwords | BCrypt (cost 12) | N/A | N/A |
| Session Tokens | N/A (short-lived) | N/A | N/A |
| Business Data | RDS encryption | AWS-managed | Annual |
| File Storage | S3 SSE-KMS | AWS KMS CMK | 90 days |
| Backups | EBS/S3 encryption | AWS KMS CMK | 90 days |

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | High | Implement encryption logic | Training, libraries, code examples |
| Operations Team | Medium | Monitor encryption, key rotation | Automation, runbooks, alerts |
| Security Team | Positive | Enhanced security posture | Regular audits |
| End Users | None | Transparent to users | N/A |
| Compliance | Positive | Meet regulatory requirements | Compliance documentation |
| Performance | Low | < 10ms overhead | Caching, optimization |

### Impact Radius

**Selected Impact Radius**: **System**

Affects:
- All services handling sensitive data
- Database schema (encrypted columns)
- File storage (S3 encryption)
- Infrastructure (KMS setup)
- Deployment (key management)
- Monitoring (encryption metrics)

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Key loss/corruption | Low | Critical | Multi-region key backup, AWS KMS durability |
| Performance degradation | Medium | Medium | Caching, selective encryption, optimization |
| KMS API rate limits | Low | Medium | Envelope encryption, caching, request batching |
| Key rotation downtime | Low | High | Zero-downtime rotation process, dual-key period |
| Compliance audit failure | Low | High | Regular audits, automated compliance checks |

**Overall Risk Level**: **Low**

## Implementation Plan

### Phase 1: Infrastructure Setup (Week 1)

- [x] Create AWS KMS Customer Managed Keys (CMKs)
- [x] Configure key policies and IAM roles
- [x] Enable CloudTrail logging for KMS
- [x] Set up key rotation (90-day schedule)
- [x] Configure multi-region key replication
- [x] Create key aliases for different data types

### Phase 2: Data in Transit Encryption (Week 2)

- [x] Enable TLS 1.3 for all API endpoints
- [x] Configure HTTPS-only (HSTS headers)
- [x] Enable TLS for database connections
- [x] Enable TLS for Redis connections
- [x] Enable TLS for Kafka connections
- [x] Configure certificate management (ACM)

### Phase 3: Data at Rest - Infrastructure Level (Week 3)

- [x] Enable RDS encryption with KMS
- [x] Enable S3 bucket encryption (SSE-KMS)
- [x] Enable EBS volume encryption
- [x] Enable ElastiCache encryption
- [x] Enable MSK encryption
- [x] Verify encryption status

### Phase 4: Data at Rest - Application Level (Week 4-5)

- [x] Implement encryption service using AWS KMS
- [x] Implement envelope encryption for performance
- [x] Add JPA AttributeConverter for encrypted fields
- [x] Encrypt payment card data fields
- [x] Encrypt PII fields (email, phone, address)
- [x] Implement key caching (5-minute TTL)
- [x] Add encryption audit logging

### Phase 5: Testing and Validation (Week 6)

- [x] Unit tests for encryption/decryption
- [x] Integration tests with KMS
- [x] Performance testing (< 10ms overhead)
- [x] Key rotation testing
- [x] Disaster recovery testing
- [x] Compliance validation (PCI-DSS, GDPR)

### Rollback Strategy

**Trigger Conditions**:
- Performance degradation > 20ms per operation
- KMS availability issues
- Data corruption during encryption
- Compliance audit failures

**Rollback Steps**:
1. Disable application-level encryption
2. Revert to infrastructure-level encryption only
3. Investigate and fix issues
4. Re-enable with fixes

**Rollback Time**: < 2 hours

## Monitoring and Success Criteria

### Success Metrics

- ✅ Encryption overhead < 10ms (95th percentile)
- ✅ Zero data breaches
- ✅ 100% sensitive data encrypted
- ✅ Key rotation success rate 100%
- ✅ PCI-DSS compliance achieved
- ✅ GDPR compliance achieved
- ✅ KMS API error rate < 0.1%

### Monitoring Plan

**CloudWatch Metrics**:
- `kms.encrypt.time` (histogram)
- `kms.decrypt.time` (histogram)
- `kms.api.errors` (count)
- `kms.api.throttles` (count)
- `encryption.cache.hit_rate` (gauge)
- `key.rotation.success` (count)

**Alerts**:
- KMS API error rate > 1% for 5 minutes
- Encryption latency > 20ms for 5 minutes
- Key rotation failures
- KMS API throttling
- Encryption cache hit rate < 80%

**Security Monitoring**:
- KMS key usage patterns
- Unauthorized key access attempts
- Key policy changes
- Encryption/decryption failures

**Review Schedule**:
- Daily: Check encryption metrics
- Weekly: Review KMS audit logs
- Monthly: Key rotation verification
- Quarterly: Compliance audit

## Consequences

### Positive Consequences

- ✅ **Compliance**: Meet PCI-DSS, GDPR, local regulations
- ✅ **Security**: Defense in depth with multi-layer encryption
- ✅ **Key Management**: Automated, auditable key lifecycle
- ✅ **Performance**: < 10ms overhead with caching
- ✅ **Scalability**: Envelope encryption reduces KMS calls
- ✅ **Operational**: Managed service reduces overhead
- ✅ **Audit Trail**: Complete encryption operation history
- ✅ **Disaster Recovery**: Multi-region key replication

### Negative Consequences

- ⚠️ **Complexity**: Additional encryption layer to manage
- ⚠️ **Cost**: $1,500/month for KMS and API calls
- ⚠️ **Vendor Lock-in**: AWS KMS dependency
- ⚠️ **Performance**: Small overhead for encryption operations
- ⚠️ **Development**: Additional code for encryption logic

### Technical Debt

**Identified Debt**:
1. No client-side encryption for highly sensitive data (acceptable for now)
2. Manual encryption key selection (acceptable with clear guidelines)
3. No homomorphic encryption for searchable encrypted data (not needed yet)

**Debt Repayment Plan**:
- **Q2 2026**: Evaluate client-side encryption for payment data
- **Q3 2026**: Implement automated encryption key selection
- **Q4 2026**: Evaluate searchable encryption if needed

## Related Decisions

- [ADR-001: Use PostgreSQL for Primary Database](001-use-postgresql-for-primary-database.md) - Database encryption
- [ADR-014: JWT-Based Authentication Strategy](014-jwt-based-authentication-strategy.md) - Token security
- [ADR-054: Data Loss Prevention (DLP) Strategy](054-data-loss-prevention-strategy.md) - Data protection integration

## Notes

### Encryption Implementation

```java
@Service
public class EncryptionService {
    
    private final AWSKMS kmsClient;
    private final Cache<String, DataKey> keyCache;
    
    public String encrypt(String plaintext, String keyId) {
        // 1. Get data key from cache or generate new
        DataKey dataKey = keyCache.get(keyId, () -> generateDataKey(keyId));
        
        // 2. Encrypt plaintext with data key (AES-256-GCM)
        byte[] ciphertext = aesEncrypt(plaintext, dataKey.getPlaintext());
        
        // 3. Return encrypted data key + ciphertext
        return Base64.encode(dataKey.getEncrypted() + ciphertext);
    }
    
    public String decrypt(String encrypted, String keyId) {
        // 1. Extract encrypted data key and ciphertext
        byte[] encryptedDataKey = extractDataKey(encrypted);
        byte[] ciphertext = extractCiphertext(encrypted);
        
        // 2. Decrypt data key with KMS
        byte[] plaintextDataKey = kmsClient.decrypt(encryptedDataKey);
        
        // 3. Decrypt ciphertext with data key
        return aesDecrypt(ciphertext, plaintextDataKey);
    }
}

// JPA AttributeConverter for transparent encryption
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {
    
    @Autowired
    private EncryptionService encryptionService;
    
    @Override
    public String convertToDatabaseColumn(String attribute) {
        return encryptionService.encrypt(attribute, "customer-data-key");
    }
    
    @Override
    public String convertToEntityAttribute(String dbData) {
        return encryptionService.decrypt(dbData, "customer-data-key");
    }
}

// Usage in entities
@Entity
public class Customer {
    
    @Id
    private String id;
    
    @Column
    @Convert(converter = EncryptedStringConverter.class)
    private String email; // Encrypted at rest
    
    @Column
    @Convert(converter = EncryptedStringConverter.class)
    private String phone; // Encrypted at rest
    
    @Column
    private String hashedPassword; // BCrypt hashed, not encrypted
}
```

### TLS Configuration

```yaml
# application.yml
server:
  ssl:
    enabled: true
    protocol: TLS
    enabled-protocols: TLSv1.3
    ciphers:
      - TLS_AES_256_GCM_SHA384
      - TLS_AES_128_GCM_SHA256
    key-store: ${SSL_KEYSTORE_PATH}
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12

# Force HTTPS
security:
  require-ssl: true
  
# HSTS headers
spring:
  security:
    headers:
      hsts:
        enabled: true
        max-age: 31536000
        include-subdomains: true
```

### Key Rotation Process

1. **Automatic Rotation** (AWS KMS):
   - KMS automatically rotates CMKs every 90 days
   - Old key versions retained for decryption
   - No application changes needed

2. **Manual Rotation** (if needed):
   - Create new CMK
   - Update application configuration
   - Re-encrypt data with new key (background job)
   - Deprecate old key after re-encryption complete

3. **Zero-Downtime Rotation**:
   - Dual-key period (7 days)
   - Both old and new keys active
   - Gradual migration to new key
   - Verify all data re-encrypted
   - Deactivate old key

### Compliance Mapping

**PCI-DSS Requirements**:
- Requirement 3.4: Render PAN unreadable ✅ (AES-256 encryption)
- Requirement 3.5: Protect keys ✅ (AWS KMS)
- Requirement 3.6: Key management ✅ (Automated rotation)
- Requirement 4.1: Encrypt transmission ✅ (TLS 1.3)

**GDPR Requirements**:
- Article 32: Security of processing ✅ (Encryption at rest and in transit)
- Article 33: Breach notification ✅ (Monitoring and alerting)

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-25  
**Next Review**: 2026-01-25 (Quarterly)
