---
adr_number: 051
title: "Input Validation and Sanitization Strategy"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [009, 014, 049, 050]
affected_viewpoints: ["functional", "development"]
affected_perspectives: ["security"]
---

# ADR-051: Input Validation and Sanitization Strategy

## Status

**Accepted** - 2025-10-25

## Context

### Problem Statement

The Enterprise E-Commerce Platform must protect against injection attacks and malicious input through comprehensive validation and sanitization:

- **SQL Injection**: Malicious SQL queries targeting database
- **Cross-Site Scripting (XSS)**: Injection of malicious scripts
- **Cross-Site Request Forgery (CSRF)**: Unauthorized state-changing operations
- **Command Injection**: OS command execution attempts
- **Path Traversal**: Unauthorized file system access
- **XML/JSON Injection**: Malicious data in structured formats
- **Business Logic Bypass**: Invalid data causing unexpected behavior

The platform requires defense-in-depth validation strategy with multiple layers:

- Frontend validation for user experience
- API Gateway validation for early rejection
- Application validation for business rules
- Database validation as final defense

### Business Context

**Business Drivers**:

- **Data Integrity**: Ensure data quality and consistency
- **Security Compliance**: PCI-DSS, GDPR requirements
- **User Trust**: Protect customer data from breaches
- **Regulatory Requirements**: Must prevent data manipulation

**Taiwan-Specific Context**:

- **High Attack Volume**: Frequent injection attack attempts
- **E-Commerce Targeting**: Payment and customer data are prime targets
- **Regulatory Scrutiny**: Taiwan Personal Data Protection Act compliance

**Constraints**:

- Must not impact user experience (< 10ms validation overhead)
- Must support internationalization (Unicode, multi-language)
- Must integrate with existing Spring Boot application
- Budget: No additional infrastructure cost

### Technical Context

**Current State**:

- Spring Boot 3.4.5 with Spring Validation
- Basic @Valid annotation usage
- No comprehensive sanitization
- No CSRF protection
- Limited input validation

**Attack Vectors**:

- **Login Form**: SQL injection, XSS in username/password
- **Search**: SQL injection, XSS in search terms
- **Product Reviews**: XSS in review content
- **User Profile**: XSS in name, address, bio
- **File Upload**: Malicious file uploads
- **API Endpoints**: JSON injection, parameter tampering

## Decision Drivers

1. **Security**: Prevent all injection attacks (SQL, XSS, CSRF)
2. **Performance**: < 10ms validation overhead
3. **User Experience**: Clear, helpful error messages
4. **Maintainability**: Centralized validation logic
5. **Compliance**: Meet PCI-DSS and GDPR requirements
6. **Flexibility**: Support custom validation rules
7. **Integration**: Seamless Spring Boot integration
8. **Cost**: No additional infrastructure cost

## Considered Options

### Option 1: Multi-Layer Validation (Frontend + API Gateway + Application + Database) - Recommended

**Description**: Defense-in-depth with validation at every layer

**Validation Layers**:

1. **Frontend**: UX validation (immediate feedback)
2. **API Gateway**: Schema validation (early rejection)
3. **Application**: Business logic validation (comprehensive)
4. **Database**: Final defense (constraints, triggers)

**Pros**:

- ✅ **Defense in Depth**: Multiple layers of protection
- ✅ **Early Rejection**: Invalid requests rejected at gateway
- ✅ **User Experience**: Immediate frontend feedback
- ✅ **Security**: Comprehensive protection against all injection types
- ✅ **Performance**: < 10ms overhead per layer
- ✅ **Maintainability**: Clear separation of concerns

**Cons**:

- ⚠️ **Complexity**: Multiple validation layers to maintain
- ⚠️ **Duplication**: Some validation logic duplicated across layers

**Cost**: $0 (using existing infrastructure)

**Risk**: **Low** - Industry best practice

### Option 2: Application-Only Validation (Basic)

**Description**: Validation only at application layer

**Pros**:

- ✅ **Simple**: Single validation layer
- ✅ **Low Maintenance**: One place to update

**Cons**:

- ❌ **No Early Rejection**: Invalid requests reach application
- ❌ **Performance**: Wasted processing on invalid requests
- ❌ **Security**: Single point of failure

**Cost**: $0

**Risk**: **Medium** - Insufficient defense in depth

### Option 3: Third-Party Validation Service

**Description**: Use external validation service

**Pros**:

- ✅ **Managed Service**: Less operational overhead
- ✅ **Advanced Features**: ML-based validation

**Cons**:

- ❌ **High Cost**: $500-2,000/month
- ❌ **Latency**: Additional network hop
- ❌ **Vendor Lock-In**: Difficult to migrate

**Cost**: $500-2,000/month

**Risk**: **Medium** - Vendor dependency

## Decision Outcome

**Chosen Option**: **Multi-Layer Validation (Frontend + API Gateway + Application + Database)**

### Rationale

Multi-layer validation was selected for the following reasons:

1. **Defense in Depth**: Multiple layers provide comprehensive protection
2. **Early Rejection**: Invalid requests rejected at gateway (saves resources)
3. **User Experience**: Immediate frontend feedback
4. **Security**: Protection against all injection types
5. **Performance**: < 10ms overhead per layer
6. **Cost-Effective**: Uses existing infrastructure
7. **Compliance**: Meets PCI-DSS and GDPR requirements

**Validation Strategy by Layer**:

**Layer 1 - Frontend (UX Validation)**:

- **Purpose**: Immediate user feedback, reduce server load
- **Validation**: Format, length, required fields
- **Technology**: React Hook Form, Angular Forms
- **Example**: Email format, password strength, required fields

**Layer 2 - API Gateway (Schema Validation)**:

- **Purpose**: Early rejection of malformed requests
- **Validation**: JSON schema, request structure
- **Technology**: OpenAPI 3.0 schema validation
- **Example**: Required fields, data types, enum values

**Layer 3 - Application (Business Logic Validation)**:

- **Purpose**: Comprehensive validation and sanitization
- **Validation**: Business rules, cross-field validation, sanitization
- **Technology**: Spring Validation, Hibernate Validator, OWASP Java Encoder
- **Example**: Business rules, SQL injection prevention, XSS prevention

**Layer 4 - Database (Final Defense)**:

- **Purpose**: Data integrity constraints
- **Validation**: Database constraints, triggers
- **Technology**: PostgreSQL constraints, check constraints
- **Example**: Unique constraints, foreign keys, check constraints

**SQL Injection Prevention**:

- **Mandatory**: Use parameterized queries (JPA, JDBC PreparedStatement)
- **Prohibited**: String concatenation for SQL queries
- **ORM Usage**: Prefer JPA/Hibernate over native SQL
- **Code Review**: Automated checks for SQL injection vulnerabilities

**XSS Prevention**:

- **Output Encoding**: Encode all user-generated content
- **HTML Sanitization**: Use OWASP Java HTML Sanitizer
- **CSP Headers**: Content Security Policy headers
- **HTTPOnly Cookies**: Prevent JavaScript access to cookies
- **Secure Cookies**: HTTPS-only cookies

**CSRF Prevention**:

- **CSRF Tokens**: Required for all state-changing operations
- **SameSite Cookies**: SameSite=Strict or Lax
- **Double-Submit Cookie**: Additional CSRF protection
- **Referer Validation**: Validate Referer header

**Why Not Application-Only**: Insufficient defense in depth, no early rejection of invalid requests.

**Why Not Third-Party**: High cost ($500-2K/month) not justified when we can implement comprehensive validation using existing infrastructure.

## Implementation Plan

### Phase 1: Application-Level Validation (Week 1)

- [x] Configure Spring Validation
- [x] Implement custom validators for business rules
- [x] Add @Valid annotations to all DTOs
- [x] Implement validation error handling
- [x] Add validation unit tests

### Phase 2: SQL Injection Prevention (Week 2)

- [x] Audit all SQL queries for injection vulnerabilities
- [x] Convert string concatenation to parameterized queries
- [x] Implement JPA query validation
- [x] Add ArchUnit rules to prevent SQL injection
- [x] Conduct security testing

### Phase 3: XSS Prevention (Week 3)

- [x] Implement output encoding for all user-generated content
- [x] Configure OWASP Java HTML Sanitizer
- [x] Add CSP headers
- [x] Configure HTTPOnly and Secure cookies
- [x] Test XSS prevention with OWASP ZAP

### Phase 4: CSRF Protection (Week 4)

- [x] Enable Spring Security CSRF protection
- [x] Configure CSRF tokens for all state-changing operations
- [x] Implement SameSite cookie attribute
- [x] Add double-submit cookie pattern
- [x] Test CSRF protection

### Phase 5: API Gateway Validation (Week 5)

- [x] Configure OpenAPI 3.0 schema validation
- [x] Implement request validation at API Gateway
- [x] Add validation error responses
- [x] Test with invalid requests
- [x] Monitor validation metrics

### Rollback Strategy

**Trigger Conditions**:

- Validation causing service outage
- False positive rate > 1% (legitimate requests rejected)
- Performance degradation > 20ms

**Rollback Steps**:

1. Disable specific validation rules causing issues
2. Revert to previous validation configuration
3. Investigate and fix issues
4. Re-deploy with corrections

**Rollback Time**: < 15 minutes

## Monitoring and Success Criteria

### Success Metrics

- ✅ **Injection Prevention**: 100% of injection attempts blocked
- ✅ **False Positive Rate**: < 0.1% legitimate requests rejected
- ✅ **Performance**: < 10ms validation overhead
- ✅ **User Experience**: Clear, helpful error messages
- ✅ **Compliance**: Pass PCI-DSS and GDPR audits
- ✅ **Security**: Zero successful injection attacks

### Monitoring Plan

**CloudWatch Metrics**:

- `validation.errors` (count by field)
- `validation.sql_injection_attempts` (count)
- `validation.xss_attempts` (count)
- `validation.csrf_failures` (count)
- `validation.latency` (histogram)

**Alerts**:

- **P0 Critical**: SQL injection attempts > 100/min
- **P1 High**: XSS attempts > 50/min
- **P2 Medium**: Validation error rate > 10%
- **P3 Low**: Unusual validation patterns

**Review Schedule**:

- **Real-Time**: 24/7 monitoring dashboard
- **Daily**: Review validation errors
- **Weekly**: Analyze attack patterns
- **Monthly**: Security review and validation optimization
- **Quarterly**: Penetration testing

## Consequences

### Positive Consequences

- ✅ **Enhanced Security**: Protection against all injection attacks
- ✅ **Data Integrity**: Ensure data quality and consistency
- ✅ **User Experience**: Clear, helpful error messages
- ✅ **Compliance**: Meet PCI-DSS and GDPR requirements
- ✅ **Performance**: < 10ms validation overhead
- ✅ **Maintainability**: Centralized validation logic
- ✅ **Cost-Effective**: No additional infrastructure cost

### Negative Consequences

- ⚠️ **Complexity**: Multiple validation layers to maintain
- ⚠️ **Duplication**: Some validation logic duplicated across layers
- ⚠️ **Development Overhead**: More code to write and test

### Technical Debt

**Identified Debt**:

1. Manual validation rule updates (acceptable initially)
2. Limited internationalization support for error messages
3. No automated validation testing

**Debt Repayment Plan**:

- **Q2 2026**: Implement automated validation rule generation
- **Q3 2026**: Enhance internationalization support
- **Q4 2026**: Implement automated validation testing

## Related Decisions

- [ADR-009: RESTful API Design with OpenAPI 3.0](009-restful-api-design-with-openapi.md) - API schema validation
- [ADR-014: JWT-Based Authentication Strategy](014-jwt-based-authentication-strategy.md) - Authentication
- [ADR-049: Web Application Firewall (WAF) Rules and Policies](049-web-application-firewall-rules-and-policies.md) - WAF protection
- [ADR-050: API Security and Rate Limiting Strategy](050-api-security-and-rate-limiting-strategy.md) - API security

## Notes

### Validation Examples

**Spring Validation (Application Layer)**:

```java
public record CreateCustomerRequest(
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Name can only contain letters and spaces")
    String name,
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    @Size(max = 255, message = "Email is too long")
    String email,
    
    @NotBlank(message = "Password is required")
    @Size(min = 12, max = 128, message = "Password must be between 12 and 128 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$", 
             message = "Password must contain uppercase, lowercase, number and special character")
    String password
) {}
```

**SQL Injection Prevention**:

```java
// ✅ GOOD: Parameterized query
@Query("SELECT c FROM Customer c WHERE c.email = :email AND c.status = :status")
Optional<Customer> findByEmailAndStatus(@Param("email") String email, @Param("status") String status);

// ❌ BAD: String concatenation (SQL injection risk)
String query = "SELECT * FROM customers WHERE email = '" + email + "'";
```

**XSS Prevention**:

```java
@Component
public class XSSProtectionService {
    
    private final PolicyFactory policy = Sanitizers.FORMATTING
        .and(Sanitizers.LINKS)
        .and(Sanitizers.BLOCKS)
        .and(Sanitizers.IMAGES);
    
    public String sanitizeHtml(String input) {
        if (input == null) return null;
        return policy.sanitize(input);
    }
    
    public String escapeHtml(String input) {
        if (input == null) return null;
        return StringEscapeUtils.escapeHtml4(input);
    }
}
```

**CSRF Protection**:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .ignoringRequestMatchers("/api/v1/public/**")
        );
        
        return http.build();
    }
}
```

### Database Constraints

**PostgreSQL Constraints**:

```sql
-- Unique constraint
ALTER TABLE customers ADD CONSTRAINT uk_customer_email UNIQUE (email);

-- Check constraint
ALTER TABLE orders ADD CONSTRAINT chk_order_total_positive CHECK (total_amount > 0);

-- Foreign key constraint
ALTER TABLE order_items ADD CONSTRAINT fk_order_items_order 
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE;

-- Not null constraint
ALTER TABLE customers ALTER COLUMN email SET NOT NULL;
```

### Security Headers

**Spring Security Headers**:

```java
@Configuration
public class SecurityHeadersConfiguration {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.headers(headers -> headers
            .contentSecurityPolicy("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'")
            .frameOptions().deny()
            .xssProtection().and()
            .contentTypeOptions().and()
            .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                .maxAgeInSeconds(31536000)
                .includeSubdomains(true)
                .preload(true))
        );
        
        return http.build();
    }
}
```

### Cost Breakdown

**Monthly Costs**:

- Spring Validation: $0 (included in Spring Boot)
- OWASP Java HTML Sanitizer: $0 (open source)
- Database Constraints: $0 (included in PostgreSQL)
- API Gateway Validation: $0 (included in API Gateway)
- **Total**: $0 (no additional cost)

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-25  
**Next Review**: 2026-01-25 (Quarterly)
