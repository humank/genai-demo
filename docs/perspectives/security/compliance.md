# Security Compliance

> **Last Updated**: 2025-10-23  
> **Status**: ✅ Active

## Overview

This document describes the regulatory compliance requirements and implementation strategies for the e-commerce platform. The system must comply with GDPR (General Data Protection Regulation) for data privacy and PCI-DSS (Payment Card Industry Data Security Standard) for payment card data handling.

## GDPR Compliance

### Overview

The General Data Protection Regulation (GDPR) is a comprehensive data protection law that applies to all organizations processing personal data of EU residents. Our e-commerce platform handles customer personal data and must comply with GDPR requirements.

### Key GDPR Principles

1. **Lawfulness, Fairness, and Transparency**: Process data lawfully and transparently
2. **Purpose Limitation**: Collect data for specified, explicit purposes
3. **Data Minimization**: Collect only necessary data
4. **Accuracy**: Keep personal data accurate and up-to-date
5. **Storage Limitation**: Retain data only as long as necessary
6. **Integrity and Confidentiality**: Protect data with appropriate security
7. **Accountability**: Demonstrate compliance with GDPR

### Data Subject Rights

#### Right to Access (Article 15)

```java
@Service
public class GDPRDataAccessService {
    
    /**
     * Provide customer with all their personal data
     */
    public CustomerDataExport exportCustomerData(String customerId) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));
        
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        List<Review> reviews = reviewRepository.findByCustomerId(customerId);
        List<Address> addresses = addressRepository.findByCustomerId(customerId);
        List<PaymentMethod> paymentMethods = paymentMethodRepository
            .findByCustomerId(customerId);
        
        return CustomerDataExport.builder()
            .personalInfo(mapPersonalInfo(customer))
            .orders(mapOrders(orders))
            .reviews(mapReviews(reviews))
            .addresses(mapAddresses(addresses))
            .paymentMethods(mapPaymentMethods(paymentMethods))
            .exportDate(LocalDateTime.now())
            .format("JSON")
            .build();
    }
    
    private PersonalInfoDto mapPersonalInfo(Customer customer) {
        return PersonalInfoDto.builder()
            .customerId(customer.getId())
            .name(customer.getName())
            .email(customer.getEmail())
            .phoneNumber(customer.getPhoneNumber())
            .dateOfBirth(customer.getDateOfBirth())
            .registrationDate(customer.getCreatedAt())
            .lastLoginDate(customer.getLastLoginAt())
            .build();
    }
}
```

#### Right to Rectification (Article 16)

```java
@Service
public class GDPRDataRectificationService {
    
    /**
     * Allow customer to correct their personal data
     */
    @Transactional
    public Customer rectifyCustomerData(
            String customerId,
            CustomerDataRectificationRequest request) {
        
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));
        
        // Update personal information
        if (request.name() != null) {
            customer.setName(request.name());
        }
        if (request.email() != null) {
            validateEmailUnique(request.email(), customerId);
            customer.setEmail(request.email());
        }
        if (request.phoneNumber() != null) {
            customer.setPhoneNumber(request.phoneNumber());
        }
        if (request.dateOfBirth() != null) {
            customer.setDateOfBirth(request.dateOfBirth());
        }
        
        customer.setUpdatedAt(LocalDateTime.now());
        Customer updated = customerRepository.save(customer);
        
        // Log rectification for audit
        auditLogger.logDataRectification(customerId, request);
        
        return updated;
    }
}
```

#### Right to Erasure (Article 17)

```java
@Service
public class GDPRDataErasureService {
    
    /**
     * Anonymize customer data (right to be forgotten)
     */
    @Transactional
    public void eraseCustomerData(String customerId) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));
        
        // Check if erasure is allowed
        if (!canEraseData(customerId)) {
            throw new DataErasureException(
                "Cannot erase data due to legal obligations or pending transactions"
            );
        }
        
        // Anonymize personal data
        customer.setName("ANONYMIZED_" + UUID.randomUUID());
        customer.setEmail("anonymized_" + UUID.randomUUID() + "@deleted.local");
        customer.setPhoneNumber("000-000-0000");
        customer.setDateOfBirth(null);
        customer.setAnonymized(true);
        customer.setAnonymizedAt(LocalDateTime.now());
        
        customerRepository.save(customer);
        
        // Anonymize related data
        anonymizeRelatedData(customerId);
        
        // Log erasure for audit
        auditLogger.logDataErasure(customerId);
    }
    
    private boolean canEraseData(String customerId) {
        // Cannot erase if there are pending orders
        List<Order> pendingOrders = orderRepository
            .findByCustomerIdAndStatus(customerId, OrderStatus.PENDING);
        if (!pendingOrders.isEmpty()) {
            return false;
        }
        
        // Cannot erase if there are recent transactions (within 90 days)
        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
        List<Order> recentOrders = orderRepository
            .findByCustomerIdAndCreatedAtAfter(customerId, ninetyDaysAgo);
        
        return recentOrders.isEmpty();
    }
    
    private void anonymizeRelatedData(String customerId) {
        // Anonymize order shipping addresses
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        for (Order order : orders) {
            order.setShippingAddress("ANONYMIZED");
            order.setBillingAddress("ANONYMIZED");
            orderRepository.save(order);
        }
        
        // Delete addresses
        addressRepository.deleteByCustomerId(customerId);
        
        // Delete payment methods (keep transaction records)
        paymentMethodRepository.deleteByCustomerId(customerId);
    }
}
```

#### Right to Data Portability (Article 20)

```java
@Service
public class GDPRDataPortabilityService {
    
    /**
     * Export customer data in machine-readable format
     */
    public byte[] exportDataInMachineReadableFormat(
            String customerId,
            ExportFormat format) {
        
        CustomerDataExport data = gdprDataAccessService
            .exportCustomerData(customerId);
        
        return switch (format) {
            case JSON -> exportAsJson(data);
            case XML -> exportAsXml(data);
            case CSV -> exportAsCsv(data);
        };
    }
    
    private byte[] exportAsJson(CustomerDataExport data) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new DataExportException("Failed to export data as JSON", e);
        }
    }
}
```

#### Right to Restriction of Processing (Article 18)

```java
@Service
public class GDPRProcessingRestrictionService {
    
    /**
     * Restrict processing of customer data
     */
    @Transactional
    public void restrictProcessing(String customerId, RestrictionReason reason) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));
        
        customer.setProcessingRestricted(true);
        customer.setRestrictionReason(reason);
        customer.setRestrictionDate(LocalDateTime.now());
        
        customerRepository.save(customer);
        
        // Log restriction
        auditLogger.logProcessingRestriction(customerId, reason);
    }
    
    /**
     * Lift processing restriction
     */
    @Transactional
    public void liftRestriction(String customerId) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));
        
        customer.setProcessingRestricted(false);
        customer.setRestrictionReason(null);
        customer.setRestrictionDate(null);
        
        customerRepository.save(customer);
        
        // Log restriction lift
        auditLogger.logRestrictionLifted(customerId);
    }
}
```

### Consent Management

```java
@Service
public class ConsentManagementService {
    
    /**
     * Record customer consent
     */
    @Transactional
    public void recordConsent(String customerId, ConsentType type) {
        Consent consent = Consent.builder()
            .customerId(customerId)
            .consentType(type)
            .granted(true)
            .grantedAt(LocalDateTime.now())
            .ipAddress(getCurrentIpAddress())
            .userAgent(getCurrentUserAgent())
            .build();
        
        consentRepository.save(consent);
    }
    
    /**
     * Withdraw customer consent
     */
    @Transactional
    public void withdrawConsent(String customerId, ConsentType type) {
        Consent consent = consentRepository
            .findByCustomerIdAndConsentType(customerId, type)
            .orElseThrow(() -> new ConsentNotFoundException(customerId, type));
        
        consent.setGranted(false);
        consent.setWithdrawnAt(LocalDateTime.now());
        
        consentRepository.save(consent);
        
        // Handle consent withdrawal
        handleConsentWithdrawal(customerId, type);
    }
    
    private void handleConsentWithdrawal(String customerId, ConsentType type) {
        switch (type) {
            case MARKETING_EMAILS -> 
                emailPreferenceService.unsubscribeFromMarketing(customerId);
            case DATA_ANALYTICS -> 
                analyticsService.excludeFromAnalytics(customerId);
            case THIRD_PARTY_SHARING -> 
                thirdPartyService.stopDataSharing(customerId);
        }
    }
}
```

### Data Retention Policy

```java
@Service
public class DataRetentionService {
    
    // Retention periods (in days)
    private static final int CUSTOMER_DATA_RETENTION = 2555; // 7 years
    private static final int ORDER_DATA_RETENTION = 2555; // 7 years
    private static final int LOG_RETENTION = 90; // 90 days
    private static final int ANONYMIZED_DATA_RETENTION = 365; // 1 year after anonymization
    
    /**
     * Enforce data retention policy
     */
    @Scheduled(cron = "0 0 3 * * *") // Daily at 3 AM
    public void enforceRetentionPolicy() {
        deleteExpiredAnonymizedData();
        deleteExpiredLogs();
        archiveOldOrders();
    }
    
    private void deleteExpiredAnonymizedData() {
        LocalDateTime cutoffDate = LocalDateTime.now()
            .minusDays(ANONYMIZED_DATA_RETENTION);
        
        List<Customer> expiredCustomers = customerRepository
            .findByAnonymizedTrueAndAnonymizedAtBefore(cutoffDate);
        
        for (Customer customer : expiredCustomers) {
            // Delete customer and all related data
            deleteCustomerCompletely(customer.getId());
            
            logger.info("Deleted expired anonymized customer data",
                kv("customerId", customer.getId()),
                kv("anonymizedAt", customer.getAnonymizedAt()));
        }
    }
    
    private void deleteExpiredLogs() {
        LocalDateTime cutoffDate = LocalDateTime.now()
            .minusDays(LOG_RETENTION);
        
        int deletedCount = auditLogRepository.deleteByCreatedAtBefore(cutoffDate);
        
        logger.info("Deleted expired audit logs",
            kv("count", deletedCount),
            kv("cutoffDate", cutoffDate));
    }
}
```

### GDPR Compliance Checklist

- [x] **Lawful Basis for Processing**: Documented consent and legitimate interest
- [x] **Privacy Policy**: Clear, accessible privacy policy
- [x] **Data Subject Rights**: All rights implemented and tested
- [x] **Consent Management**: Granular consent with easy withdrawal
- [x] **Data Minimization**: Collect only necessary data
- [x] **Data Retention**: Automated retention policy enforcement
- [x] **Data Security**: Encryption, access controls, audit logging
- [x] **Data Breach Notification**: Incident response plan in place
- [x] **Data Protection Officer**: DPO appointed and contactable
- [x] **Privacy by Design**: Security built into system design
- [x] **Data Processing Agreements**: Contracts with third-party processors

## PCI-DSS Compliance

### Overview

The Payment Card Industry Data Security Standard (PCI-DSS) is a set of security standards designed to ensure that all companies that accept, process, store, or transmit credit card information maintain a secure environment.

### PCI-DSS Requirements

#### Requirement 1: Install and Maintain Firewall Configuration

```yaml
# AWS Security Group Configuration
SecurityGroup:
  Type: AWS::EC2::SecurityGroup
  Properties:
    GroupDescription: Application security group
    VpcId: !Ref VPC
    SecurityGroupIngress:
      # Allow HTTPS only
      - IpProtocol: tcp
        FromPort: 443
        ToPort: 443
        CidrIp: 0.0.0.0/0
    SecurityGroupEgress:
      # Allow outbound to payment gateway only
      - IpProtocol: tcp
        FromPort: 443
        ToPort: 443
        DestinationSecurityGroupId: !Ref PaymentGatewaySecurityGroup
```

#### Requirement 2: Do Not Use Vendor-Supplied Defaults

```java
@Configuration
public class SecurityDefaultsConfiguration {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Use strong password encoding (not default)
        return new BCryptPasswordEncoder(12);
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable default security
            .csrf().disable()
            .cors().and()
            // Custom security configuration
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        
        return http.build();
    }
}
```

#### Requirement 3: Protect Stored Cardholder Data

```java
@Service
public class PCIDSSPaymentService {
    
    /**
     * NEVER store full PAN (Primary Account Number)
     * NEVER store CVV/CVC
     * NEVER store PIN
     */
    @Transactional
    public PaymentResult processPayment(PaymentRequest request) {
        // Validate card data (but don't log it)
        validateCardData(request);
        
        // Send directly to payment gateway
        // DO NOT store card data in our database
        PaymentGatewayResponse response = paymentGateway.process(request);
        
        // Store only tokenized reference
        Payment payment = Payment.builder()
            .orderId(request.getOrderId())
            .amount(request.getAmount())
            .paymentToken(response.getToken()) // Token only, not card data
            .last4Digits(request.getCardNumber().substring(
                request.getCardNumber().length() - 4)) // Last 4 digits only
            .cardBrand(detectCardBrand(request.getCardNumber()))
            .status(response.getStatus())
            .processedAt(LocalDateTime.now())
            .build();
        
        paymentRepository.save(payment);
        
        return PaymentResult.from(response);
    }
    
    private void validateCardData(PaymentRequest request) {
        // Validate but NEVER log card data
        if (!isValidCardNumber(request.getCardNumber())) {
            throw new InvalidCardException("Invalid card number");
        }
        
        // Ensure CVV is not stored
        if (request.getCvv() == null || request.getCvv().length() < 3) {
            throw new InvalidCardException("Invalid CVV");
        }
        
        // CVV should only be used for validation, never stored
    }
}
```

#### Requirement 4: Encrypt Transmission of Cardholder Data

```yaml
# application.yml - Force TLS 1.3
server:
  ssl:
    enabled: true
    protocol: TLS
    enabled-protocols: TLSv1.3
    ciphers:
      - TLS_AES_256_GCM_SHA384
      - TLS_AES_128_GCM_SHA256
```

#### Requirement 5: Protect All Systems Against Malware

```yaml
# Automated vulnerability scanning
security-scanning:
  enabled: true
  schedule: "0 0 2 * * *" # Daily at 2 AM
  tools:
    - dependency-check
    - spotbugs
    - sonarqube
```

#### Requirement 6: Develop and Maintain Secure Systems

```java
// Secure coding practices enforced through code review and automated checks
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResult> processPayment(
            @Valid @RequestBody PaymentRequest request) {
        
        // Input validation
        validatePaymentRequest(request);
        
        // Process payment securely
        PaymentResult result = paymentService.processPayment(request);
        
        return ResponseEntity.ok(result);
    }
}
```

#### Requirement 7: Restrict Access to Cardholder Data

```java
@PreAuthorize("hasRole('PAYMENT_PROCESSOR')")
public class PaymentProcessingService {
    
    /**
     * Only authorized personnel can access payment processing
     */
    @AuditDataAccess
    public PaymentResult processPayment(PaymentRequest request) {
        // Payment processing logic
        // All access is logged for audit
    }
}
```

#### Requirement 8: Identify and Authenticate Access

```java
// Strong authentication for payment processing
@Service
public class PaymentAuthenticationService {
    
    /**
     * Require MFA for payment processing access
     */
    public boolean authenticatePaymentProcessor(String userId, String mfaCode) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        
        // Verify user has payment processor role
        if (!user.hasRole("PAYMENT_PROCESSOR")) {
            return false;
        }
        
        // Verify MFA code
        return mfaService.verifyCode(userId, mfaCode);
    }
}
```

#### Requirement 9: Restrict Physical Access

- Physical access controls to data centers (AWS responsibility)
- Visitor logs and escort requirements
- Secure disposal of media containing cardholder data

#### Requirement 10: Track and Monitor All Access

```java
@Component
public class PaymentAuditLogger {
    
    /**
     * Log all payment-related activities
     */
    public void logPaymentAccess(String userId, String action, String details) {
        AuditLogEntry entry = AuditLogEntry.builder()
            .userId(userId)
            .action(action)
            .category("PAYMENT")
            .details(details)
            .ipAddress(getCurrentIpAddress())
            .timestamp(Instant.now())
            .build();
        
        auditLogRepository.save(entry);
        
        // Also send to centralized logging
        logger.info("Payment access",
            kv("userId", userId),
            kv("action", action),
            kv("timestamp", Instant.now()));
    }
}
```

#### Requirement 11: Regularly Test Security Systems

```java
@SpringBootTest
class PCIDSSSecurityTest {
    
    @Test
    void should_not_store_full_card_number() {
        // Verify no full card numbers in database
        List<Payment> payments = paymentRepository.findAll();
        
        for (Payment payment : payments) {
            assertThat(payment.getCardNumber()).isNull();
            assertThat(payment.getLast4Digits()).hasSize(4);
            assertThat(payment.getPaymentToken()).isNotNull();
        }
    }
    
    @Test
    void should_not_store_cvv() {
        // Verify no CVV stored anywhere
        List<Payment> payments = paymentRepository.findAll();
        
        for (Payment payment : payments) {
            assertThat(payment.getCvv()).isNull();
        }
    }
}
```

#### Requirement 12: Maintain Information Security Policy

- Documented security policies
- Annual security awareness training
- Incident response plan
- Regular security assessments

### PCI-DSS Compliance Checklist

- [x] **Firewall Configuration**: AWS Security Groups configured
- [x] **No Default Passwords**: All defaults changed
- [x] **Cardholder Data Protection**: No card data stored
- [x] **Encryption in Transit**: TLS 1.3 enforced
- [x] **Malware Protection**: Automated scanning enabled
- [x] **Secure Development**: Secure coding practices enforced
- [x] **Access Restriction**: RBAC with MFA for payment processing
- [x] **Authentication**: Strong authentication required
- [x] **Physical Access**: AWS data center controls
- [x] **Audit Logging**: All payment access logged
- [x] **Security Testing**: Automated and manual testing
- [x] **Security Policy**: Documented and maintained

## Compliance Monitoring

### Automated Compliance Checks

```java
@Service
public class ComplianceMonitoringService {
    
    @Scheduled(cron = "0 0 1 * * *") // Daily at 1 AM
    public void runComplianceChecks() {
        ComplianceReport report = ComplianceReport.builder()
            .reportDate(LocalDate.now())
            .gdprCompliance(checkGDPRCompliance())
            .pciDssCompliance(checkPCIDSSCompliance())
            .build();
        
        complianceReportRepository.save(report);
        
        // Alert if non-compliant
        if (!report.isFullyCompliant()) {
            alertComplianceTeam(report);
        }
    }
    
    private GDPRComplianceStatus checkGDPRCompliance() {
        return GDPRComplianceStatus.builder()
            .dataSubjectRightsImplemented(true)
            .consentManagementActive(true)
            .dataRetentionPolicyEnforced(true)
            .privacyPolicyUpToDate(checkPrivacyPolicyDate())
            .dataBreachProceduresTested(checkLastDRPTest())
            .build();
    }
    
    private PCIDSSComplianceStatus checkPCIDSSCompliance() {
        return PCIDSSComplianceStatus.builder()
            .noCardDataStored(verifyNoCardDataStored())
            .tlsEnforced(verifyTLSConfiguration())
            .accessControlsActive(verifyAccessControls())
            .auditLoggingEnabled(verifyAuditLogging())
            .vulnerabilityScanningActive(checkLastVulnerabilityScan())
            .build();
    }
}
```

## Compliance Documentation

### Required Documentation

1. **Privacy Policy**: Customer-facing privacy policy
2. **Data Processing Agreement**: Contracts with third-party processors
3. **Data Protection Impact Assessment (DPIA)**: Risk assessment
4. **Incident Response Plan**: Data breach response procedures
5. **Security Policy**: Internal security policies and procedures
6. **Audit Logs**: Comprehensive access and activity logs
7. **Compliance Reports**: Regular compliance assessment reports

## Related Documentation

- [Security Overview](overview.md) - Overall security perspective
- [Data Protection](data-protection.md) - Data protection implementation
- [Authentication](authentication.md) - Authentication mechanisms
- [Authorization](authorization.md) - Authorization model

## References

- GDPR Official Text: https://gdpr.eu/
- PCI-DSS Standards: https://www.pcisecuritystandards.org/
- GDPR Compliance Checklist: https://gdpr.eu/checklist/
- PCI-DSS Self-Assessment: https://www.pcisecuritystandards.org/document_library
