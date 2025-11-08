---
title: "Data Residency and Compliance"
type: "perspective-detail"
category: "location"
stakeholders: ["compliance-officers", "legal-team", "data-protection-officers", "infrastructure-architects"]
last_updated: "2025-10-24"
version: "1.0"
status: "active"
related_docs:

  - "overview.md"
  - "multi-region.md"
  - "../../perspectives/security/README.md"
  - "../../viewpoints/information/README.md"

tags: ["compliance", "gdpr", "data-residency", "privacy", "regulations"]
---

# Data Residency and Compliance

## Overview

This document details the data residency requirements, compliance strategies, and implementation approaches for ensuring the Enterprise E-Commerce Platform meets global data protection regulations including GDPR, CCPA, and regional data localization laws.

## Regulatory Landscape

### Global Regulations

#### 1. GDPR (General Data Protection Regulation)

**Scope**: European Union and European Economic Area  
**Effective Date**: May 25, 2018  
**Key Requirements**:

- **Data Residency**: Personal data of EU residents must be stored within the EU
- **Data Transfer**: Transfers outside EU require adequate safeguards
- **Right to be Forgotten**: Users can request data deletion
- **Data Portability**: Users can request data export
- **Consent**: Explicit consent required for data processing
- **Breach Notification**: Must notify within 72 hours

**Penalties**: Up to €20 million or 4% of global annual revenue

#### 2. CCPA (California Consumer Privacy Act)

**Scope**: California residents  
**Effective Date**: January 1, 2020  
**Key Requirements**:

- **Right to Know**: Users can request what data is collected
- **Right to Delete**: Users can request data deletion
- **Right to Opt-Out**: Users can opt-out of data selling
- **Non-Discrimination**: Cannot discriminate against users exercising rights

**Penalties**: Up to $7,500 per intentional violation

#### 3. China Data Localization Law

**Scope**: China  
**Effective Date**: June 1, 2017  
**Key Requirements**:

- **Data Localization**: Personal information and important data must be stored in China
- **Security Assessment**: Cross-border data transfers require security assessment
- **Government Access**: Must provide data access to Chinese authorities

**Penalties**: Business suspension, fines, criminal liability

### Regional Requirements

| Region | Regulation | Key Requirement | Implementation |
|--------|-----------|-----------------|----------------|
| EU/EEA | GDPR | Data must stay in EU | EU-WEST-1 region |
| UK | UK GDPR | Data must stay in UK | EU-WEST-1 region (post-Brexit adequacy) |
| Switzerland | Swiss DPA | Data protection equivalent to GDPR | EU-WEST-1 region |
| Brazil | LGPD | Similar to GDPR | US-EAST-1 (adequacy decision pending) |
| India | PDPB | Data localization for sensitive data | AP-SE-1 region |
| Russia | Federal Law 242-FZ | Personal data must be stored in Russia | Future RU region |
| Australia | Privacy Act | Data protection requirements | AP-SE-1 region |

## Data Classification

### Classification Levels

#### Level 1: Public Data

**Definition**: Data that can be freely shared without restrictions

**Examples**:

- Product catalog
- Public blog posts
- Marketing materials
- Public API documentation

**Storage**: Can be stored in any region  
**Replication**: Replicated globally for performance  
**Retention**: Indefinite

#### Level 2: Internal Data

**Definition**: Data for internal use only, not sensitive

**Examples**:

- Internal documentation
- System logs (non-PII)
- Performance metrics
- Aggregated analytics

**Storage**: Can be stored in any region  
**Replication**: Replicated to operational regions  
**Retention**: 90 days to 1 year

#### Level 3: Confidential Data

**Definition**: Sensitive business data requiring protection

**Examples**:

- Customer orders
- Business contracts
- Financial reports
- Internal communications

**Storage**: Region-specific based on data origin  
**Replication**: Limited replication with encryption  
**Retention**: 7 years (legal requirement)

#### Level 4: Restricted Data (PII)

**Definition**: Personally Identifiable Information requiring strict protection

**Examples**:

- Customer names and addresses
- Email addresses
- Phone numbers
- IP addresses
- User preferences

**Storage**: Must stay in region of origin  
**Replication**: No cross-border replication  
**Retention**: As long as account is active + 30 days

#### Level 5: Highly Restricted Data

**Definition**: Highly sensitive data with legal/regulatory requirements

**Examples**:

- Payment card information (PCI-DSS)
- Health information (HIPAA)
- Biometric data
- Government ID numbers

**Storage**: Must stay in region of origin with encryption  
**Replication**: Prohibited  
**Retention**: Minimal retention, deleted after use

### Data Classification Matrix

```text
┌─────────────────────────────────────────────────────────────────┐
│                    Data Classification                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Level 1: Public          ┌──────────────────────────────┐     │
│  ├─ Global Storage        │  • Product Catalog           │     │
│  └─ Global Replication    │  • Marketing Content         │     │
│                            └──────────────────────────────┘     │
│                                                                   │
│  Level 2: Internal        ┌──────────────────────────────┐     │
│  ├─ Regional Storage      │  • System Logs               │     │
│  └─ Regional Replication  │  • Metrics                   │     │
│                            └──────────────────────────────┘     │
│                                                                   │
│  Level 3: Confidential    ┌──────────────────────────────┐     │
│  ├─ Regional Storage      │  • Orders                    │     │
│  └─ Limited Replication   │  • Contracts                 │     │
│                            └──────────────────────────────┘     │
│                                                                   │
│  Level 4: Restricted      ┌──────────────────────────────┐     │
│  ├─ Origin Region Only    │  • Customer PII              │     │
│  └─ No Replication        │  • Email, Phone              │     │
│                            └──────────────────────────────┘     │
│                                                                   │
│  Level 5: Highly Restricted ┌────────────────────────────┐     │
│  ├─ Origin Region Only      │  • Payment Cards           │     │
│  ├─ Encrypted at Rest       │  • Health Data             │     │
│  └─ No Replication          │  • Biometrics              │     │
│                              └────────────────────────────┘     │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

## Implementation Strategy

### Data Tagging

**Metadata Tags**:

```json
{
  "dataClassification": "restricted",
  "dataOriginRegion": "eu-west-1",
  "dataSubject": "customer",
  "regulatoryFramework": "GDPR",
  "retentionPeriod": "7years",
  "encryptionRequired": true,
  "crossBorderTransferAllowed": false
}
```

**Database Implementation**:

```sql
CREATE TABLE customers (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    -- Metadata columns
    data_classification VARCHAR(50) DEFAULT 'restricted',
    origin_region VARCHAR(50) NOT NULL,
    regulatory_framework VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL
);

-- Index for compliance queries
CREATE INDEX idx_customers_origin_region ON customers(origin_region);
CREATE INDEX idx_customers_regulatory_framework ON customers(regulatory_framework);
```

### Regional Data Isolation

#### EU Data Isolation

**Architecture**:

```text
┌─────────────────────────────────────────────────────────┐
│                    EU Data Boundary                      │
├─────────────────────────────────────────────────────────┤
│                                                           │
│  ┌──────────────────────────────────────────────────┐  │
│  │  EU-WEST-1 Region                                 │  │
│  │                                                    │  │
│  │  ┌──────────────┐  ┌──────────────┐             │  │
│  │  │  Application │  │  Database    │             │  │
│  │  │  Servers     │  │  (EU Data)   │             │  │
│  │  └──────────────┘  └──────────────┘             │  │
│  │                                                    │  │
│  │  ┌──────────────┐  ┌──────────────┐             │  │
│  │  │  Redis Cache │  │  S3 Bucket   │             │  │
│  │  │  (EU Data)   │  │  (EU Data)   │             │  │
│  │  └──────────────┘  └──────────────┘             │  │
│  │                                                    │  │
│  └──────────────────────────────────────────────────┘  │
│                                                           │
│  ❌ No Data Transfer Outside EU Boundary                │
│                                                           │
└─────────────────────────────────────────────────────────┘
```

**Implementation**:

```java
@Service
public class CustomerService {
    
    @Autowired
    private RegionalDataService regionalDataService;
    
    public Customer createCustomer(CreateCustomerCommand command) {
        // Determine data region based on user location
        String dataRegion = determineDataRegion(command.getCountry());
        
        // Ensure data stays in correct region
        if (isEUCountry(command.getCountry())) {
            // Use EU-specific database
            return regionalDataService.createInRegion(command, "eu-west-1");
        } else {
            // Use US database
            return regionalDataService.createInRegion(command, "us-east-1");
        }
    }
    
    private boolean isEUCountry(String country) {
        Set<String> euCountries = Set.of(
            "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR",
            "DE", "GR", "HU", "IE", "IT", "LV", "LT", "LU", "MT", "NL",
            "PL", "PT", "RO", "SK", "SI", "ES", "SE"
        );
        return euCountries.contains(country);
    }
}
```

### Cross-Border Data Transfer

#### Standard Contractual Clauses (SCCs)

**When Required**: Transferring EU personal data outside EU

**Implementation**:

1. **Legal Agreement**: Execute SCCs with data processor
2. **Technical Measures**: Implement encryption and access controls
3. **Documentation**: Maintain transfer records
4. **Monitoring**: Audit cross-border transfers

**Example SCC Implementation**:

```java
@Service
public class CrossBorderTransferService {
    
    public void transferData(DataTransferRequest request) {
        // Verify SCC in place
        if (!hasSCCAgreement(request.getSourceRegion(), request.getDestRegion())) {
            throw new ComplianceException("No SCC agreement for this transfer");
        }
        
        // Log transfer for audit
        auditLogger.logCrossBorderTransfer(
            request.getDataType(),
            request.getSourceRegion(),
            request.getDestRegion(),
            request.getLegalBasis()
        );
        
        // Encrypt data before transfer
        byte[] encryptedData = encryptionService.encrypt(request.getData());
        
        // Transfer data
        transferService.transfer(encryptedData, request.getDestRegion());
    }
}
```

#### Adequacy Decisions

**Countries with EU Adequacy Decision**:

- Andorra
- Argentina
- Canada (commercial organizations)
- Faroe Islands
- Guernsey
- Israel
- Isle of Man
- Japan
- Jersey
- New Zealand
- Republic of Korea
- Switzerland
- United Kingdom
- Uruguay

**Implementation**:

```java
public boolean canTransferWithoutSCC(String sourceRegion, String destRegion) {
    Set<String> adequacyCountries = Set.of(
        "CH", "UK", "JP", "NZ", "KR", "IL", "CA", "AR", "UY"
    );
    
    if (sourceRegion.equals("eu-west-1")) {
        String destCountry = getCountryFromRegion(destRegion);
        return adequacyCountries.contains(destCountry);
    }
    
    return false;
}
```

## Compliance Verification

### Automated Compliance Checks

**Daily Checks**:

```bash
#!/bin/bash
# scripts/verify-data-residency.sh

# Check EU data in EU region
echo "Checking EU data residency..."
aws rds describe-db-instances \
  --region eu-west-1 \
  --query 'DBInstances[?contains(DBInstanceIdentifier, `eu-data`)].DBInstanceIdentifier'

# Verify no EU data in US region
echo "Verifying no EU data in US region..."
psql -h us-east-1-db -c "SELECT COUNT(*) FROM customers WHERE origin_region = 'eu-west-1';"
# Expected: 0

# Check data classification tags
echo "Checking data classification..."
aws s3api get-bucket-tagging --bucket ecommerce-data-eu-west-1

# Verify encryption
echo "Verifying encryption..."
aws s3api get-bucket-encryption --bucket ecommerce-data-eu-west-1
```

**Weekly Audits**:

```sql
-- Audit cross-border data transfers
SELECT 
    transfer_date,
    data_type,
    source_region,
    dest_region,
    legal_basis,
    COUNT(*) as transfer_count
FROM data_transfer_audit
WHERE transfer_date >= CURRENT_DATE - INTERVAL '7 days'
GROUP BY transfer_date, data_type, source_region, dest_region, legal_basis
ORDER BY transfer_date DESC;

-- Verify data retention compliance
SELECT 
    data_classification,
    COUNT(*) as record_count,
    MIN(created_at) as oldest_record,
    MAX(created_at) as newest_record
FROM customers
WHERE deleted_at IS NULL
GROUP BY data_classification;
```

### Compliance Reporting

**Monthly Compliance Report**:

```text
Data Residency Compliance Report
Month: October 2025

1. Data Residency Status

   ✅ EU Data: 100% in EU-WEST-1
   ✅ US Data: 100% in US-EAST-1
   ✅ APAC Data: 100% in AP-SE-1

2. Cross-Border Transfers
   - Total Transfers: 1,234
   - With SCC: 1,234 (100%)
   - Without SCC: 0 (0%)
   - Violations: 0

3. Data Subject Requests
   - Access Requests: 45
   - Deletion Requests: 12
   - Portability Requests: 8
   - Average Response Time: 18 hours
   - SLA Compliance: 100%

4. Encryption Status

   ✅ Data at Rest: 100% encrypted
   ✅ Data in Transit: 100% encrypted
   ✅ Backup Encryption: 100% encrypted

5. Audit Findings
   - Critical Issues: 0
   - High Issues: 0
   - Medium Issues: 2
   - Low Issues: 5

6. Recommendations
   - Update data retention policies
   - Review SCC agreements
   - Conduct DPIA for new features

```

## Data Subject Rights

### Right to Access (GDPR Article 15)

**Implementation**:

```java
@RestController
@RequestMapping("/api/v1/data-subject-requests")
public class DataSubjectRequestController {
    
    @PostMapping("/access")
    public ResponseEntity<DataAccessResponse> handleAccessRequest(
            @Valid @RequestBody DataAccessRequest request) {
        
        // Verify identity
        if (!identityVerificationService.verify(request)) {
            throw new UnauthorizedException("Identity verification failed");
        }
        
        // Collect all personal data
        PersonalDataPackage dataPackage = dataCollectionService.collectAllData(
            request.getCustomerId()
        );
        
        // Log request for audit
        auditLogger.logDataAccessRequest(request.getCustomerId());
        
        // Return data in machine-readable format
        return ResponseEntity.ok(new DataAccessResponse(dataPackage));
    }
}
```

### Right to Erasure (GDPR Article 17)

**Implementation**:

```java
@Service
public class DataErasureService {
    
    @Transactional
    public void eraseCustomerData(String customerId) {
        // Verify erasure is allowed
        if (!canEraseData(customerId)) {
            throw new ErasureNotAllowedException("Cannot erase data due to legal obligations");
        }
        
        // Soft delete customer record
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));
        customer.setDeletedAt(Instant.now());
        customerRepository.save(customer);
        
        // Anonymize related data
        orderRepository.anonymizeCustomerOrders(customerId);
        reviewRepository.anonymizeCustomerReviews(customerId);
        
        // Schedule hard delete after retention period
        scheduleHardDelete(customerId, 30); // 30 days
        
        // Log erasure for audit
        auditLogger.logDataErasure(customerId);
    }
    
    private boolean canEraseData(String customerId) {
        // Cannot erase if:
        // - Active orders exist
        // - Legal hold in place
        // - Regulatory retention period not met
        return !hasActiveOrders(customerId) 
            && !hasLegalHold(customerId)
            && retentionPeriodMet(customerId);
    }
}
```

### Right to Data Portability (GDPR Article 20)

**Implementation**:

```java
@Service
public class DataPortabilityService {
    
    public byte[] exportCustomerData(String customerId, ExportFormat format) {
        // Collect all personal data
        PersonalDataPackage dataPackage = dataCollectionService.collectAllData(customerId);
        
        // Export in requested format
        return switch (format) {
            case JSON -> jsonExporter.export(dataPackage);
            case XML -> xmlExporter.export(dataPackage);
            case CSV -> csvExporter.export(dataPackage);
        };
    }
}
```

## Breach Notification

### GDPR Breach Notification (72 hours)

**Procedure**:

1. **Detection** (T+0):

   ```bash
   # Automated detection

   - Security monitoring alerts
   - Anomaly detection
   - User reports

   ```

2. **Assessment** (T+1 hour):

   ```bash
   # Determine severity

   - Number of affected users
   - Type of data exposed
   - Potential harm
   - Likelihood of misuse

   ```

3. **Containment** (T+2 hours):

   ```bash
   # Stop the breach

   - Isolate affected systems
   - Revoke compromised credentials
   - Block unauthorized access

   ```

4. **Notification** (T+72 hours):

   ```bash
   # Notify authorities

   - Notify supervisory authority (DPA)
   - Notify affected data subjects
   - Document notification

   ```

**Notification Template**:

```text
To: [Data Protection Authority]
Subject: Personal Data Breach Notification

1. Nature of Breach:
   - Date/Time: [timestamp]
   - Type: [unauthorized access/data loss/etc.]
   - Affected Systems: [list]

2. Categories of Data:
   - Personal Data: [types]
   - Number of Records: [count]
   - Number of Data Subjects: [count]

3. Likely Consequences:
   - [description of potential harm]

4. Measures Taken:
   - [containment actions]
   - [remediation steps]
   - [preventive measures]

5. Contact Information:
   - DPO: [name, email, phone]
   - Organization: [details]

```

## Best Practices

### Development Best Practices

1. **Data Minimization**:
   - Collect only necessary data
   - Delete data when no longer needed
   - Anonymize data when possible

2. **Privacy by Design**:
   - Consider privacy from the start
   - Implement privacy controls
   - Document privacy decisions

3. **Encryption**:
   - Encrypt data at rest (AES-256)
   - Encrypt data in transit (TLS 1.3)
   - Manage encryption keys securely

### Operational Best Practices

1. **Regular Audits**:
   - Monthly compliance audits
   - Quarterly security audits
   - Annual third-party audits

2. **Training**:
   - Annual privacy training for all staff
   - Specialized training for developers
   - Regular updates on regulations

3. **Documentation**:
   - Maintain data processing records
   - Document data flows
   - Keep audit trails

## Related Documentation

- [Overview](overview.md) - Location Perspective overview
- [Multi-Region Deployment](multi-region.md) - Regional architecture
- [Security Perspective](../../perspectives/security/README.md) - Security controls
- [Information Viewpoint](../../viewpoints/information/README.md) - Data models

---

**Document Status**: ✅ Complete  
**Review Date**: 2025-10-24  
**Next Review**: 2026-01-24 (Quarterly)
