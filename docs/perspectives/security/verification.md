# Security Verification and Testing

> **Last Updated**: 2025-10-23  
> **Status**: ✅ Active

## Overview

This document describes the security testing and verification strategies used to ensure the e-commerce platform meets security requirements. Security testing is integrated throughout the development lifecycle, from unit tests to penetration testing.

## Security Testing Strategy

### Testing Pyramid for Security

```text
        /\
       /  \      Penetration Testing (Manual, Quarterly)
      /____\
     /      \    Security Integration Tests (Automated, Daily)
    /________\
   /          \  Security Unit Tests (Automated, Every Build)
  /____________\
```

### Test Types

#### Security Unit Tests

- **Frequency**: Every build
- **Scope**: Individual security functions
- **Tools**: JUnit, Mockito
- **Examples**: Password validation, encryption/decryption, input sanitization

#### Security Integration Tests

- **Frequency**: Daily (CI/CD)
- **Scope**: Security controls across components
- **Tools**: Spring Security Test, TestContainers
- **Examples**: Authentication flows, authorization checks, API security

#### Security Scanning

- **Frequency**: Every commit
- **Scope**: Code and dependencies
- **Tools**: SpotBugs, OWASP Dependency-Check, SonarQube
- **Examples**: Vulnerability detection, code quality issues

#### Penetration Testing

- **Frequency**: Quarterly
- **Scope**: Complete system
- **Tools**: OWASP ZAP, Burp Suite, Manual testing
- **Examples**: SQL injection, XSS, authentication bypass

## Authentication Testing

### Password Security Tests

```java
@ExtendWith(MockitoExtension.class)
class PasswordSecurityTest {
    
    private PasswordEncoderService passwordEncoder;
    
    @BeforeEach
    void setUp() {
        passwordEncoder = new PasswordEncoderService();
    }
    
    @Test
    void should_reject_password_shorter_than_8_characters() {
        assertThatThrownBy(() -> passwordEncoder.encodePassword("Short1!"))
            .isInstanceOf(WeakPasswordException.class)
            .hasMessageContaining("at least 8 characters");
    }
    
    @Test
    void should_reject_password_without_uppercase() {
        assertThatThrownBy(() -> passwordEncoder.encodePassword("lowercase123!"))
            .isInstanceOf(WeakPasswordException.class)
            .hasMessageContaining("uppercase letter");
    }
    
    @Test
    void should_reject_password_without_lowercase() {
        assertThatThrownBy(() -> passwordEncoder.encodePassword("UPPERCASE123!"))
            .isInstanceOf(WeakPasswordException.class)
            .hasMessageContaining("lowercase letter");
    }
    
    @Test
    void should_reject_password_without_digit() {
        assertThatThrownBy(() -> passwordEncoder.encodePassword("NoDigits!"))
            .isInstanceOf(WeakPasswordException.class)
            .hasMessageContaining("digit");
    }
    
    @Test
    void should_reject_password_without_special_character() {
        assertThatThrownBy(() -> passwordEncoder.encodePassword("NoSpecial123"))
            .isInstanceOf(WeakPasswordException.class)
            .hasMessageContaining("special character");
    }
    
    @Test
    void should_accept_strong_password() {
        String password = "StrongPass123!";
        String encoded = passwordEncoder.encodePassword(password);
        
        assertThat(encoded).isNotEqualTo(password);
        assertThat(passwordEncoder.matches(password, encoded)).isTrue();
    }
    
    @Test
    void should_produce_different_hashes_for_same_password() {
        String password = "SamePassword123!";
        
        String hash1 = passwordEncoder.encodePassword(password);
        String hash2 = passwordEncoder.encodePassword(password);
        
        // BCrypt uses salt, so hashes should be different
        assertThat(hash1).isNotEqualTo(hash2);
        
        // But both should match the original password
        assertThat(passwordEncoder.matches(password, hash1)).isTrue();
        assertThat(passwordEncoder.matches(password, hash2)).isTrue();
    }
}
```

### JWT Token Tests

```java
@SpringBootTest
class JwtTokenTest {
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Test
    void should_generate_valid_access_token() {
        UserDetails user = createTestUser();
        
        String token = tokenProvider.generateAccessToken(user);
        
        assertThat(token).isNotNull();
        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getUserIdFromToken(token))
            .isEqualTo(user.getUsername());
    }
    
    @Test
    void should_reject_expired_token() throws Exception {
        // Create token with very short expiration
        String expiredToken = createExpiredToken();
        
        assertThat(tokenProvider.validateToken(expiredToken)).isFalse();
    }
    
    @Test
    void should_reject_tampered_token() {
        UserDetails user = createTestUser();
        String token = tokenProvider.generateAccessToken(user);
        
        // Tamper with token
        String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";
        
        assertThat(tokenProvider.validateToken(tamperedToken)).isFalse();
    }
    
    @Test
    void should_reject_token_with_invalid_signature() {
        String tokenWithInvalidSignature = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
            "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ." +
            "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        
        assertThat(tokenProvider.validateToken(tokenWithInvalidSignature)).isFalse();
    }
}
```

### Authentication Flow Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AuthenticationFlowTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void should_authenticate_with_valid_credentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "customer@example.com",
                        "password": "ValidPass123!"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andExpect(jsonPath("$.user.email").value("customer@example.com"));
    }
    
    @Test
    void should_reject_invalid_credentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "customer@example.com",
                        "password": "WrongPassword"
                    }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }
    
    @Test
    void should_block_after_multiple_failed_attempts() throws Exception {
        String email = "brute-force@example.com";
        
        // Attempt 5 failed logins
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(String.format("""
                        {
                            "email": "%s",
                            "password": "WrongPassword"
                        }
                        """, email)))
                .andExpect(status().isUnauthorized());
        }
        
        // 6th attempt should be blocked
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("""
                    {
                        "email": "%s",
                        "password": "CorrectPassword123!"
                    }
                    """, email)))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.errorCode").value("ACCOUNT_LOCKED"));
    }
}
```

## Authorization Testing

### Role-Based Access Control Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthorizationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    @WithMockUser(username = "customer-123", roles = "CUSTOMER")
    void customer_can_access_own_data() throws Exception {
        mockMvc.perform(get("/api/v1/customers/customer-123"))
            .andExpect(status().isOk());
    }
    
    @Test
    @WithMockUser(username = "customer-123", roles = "CUSTOMER")
    void customer_cannot_access_other_customer_data() throws Exception {
        mockMvc.perform(get("/api/v1/customers/customer-456"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }
    
    @Test
    @WithMockUser(username = "admin-1", roles = "ADMIN")
    void admin_can_access_any_customer_data() throws Exception {
        mockMvc.perform(get("/api/v1/customers/customer-123"))
            .andExpect(status().isOk());
        
        mockMvc.perform(get("/api/v1/customers/customer-456"))
            .andExpect(status().isOk());
    }
    
    @Test
    @WithAnonymousUser
    void anonymous_user_cannot_access_protected_resources() throws Exception {
        mockMvc.perform(get("/api/v1/customers/customer-123"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    @WithMockUser(username = "seller-1", roles = "SELLER")
    void seller_can_manage_own_products() throws Exception {
        mockMvc.perform(put("/api/v1/products/product-owned-by-seller-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "Updated Product",
                        "price": 99.99
                    }
                    """))
            .andExpect(status().isOk());
    }
    
    @Test
    @WithMockUser(username = "seller-1", roles = "SELLER")
    void seller_cannot_manage_other_seller_products() throws Exception {
        mockMvc.perform(put("/api/v1/products/product-owned-by-seller-2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "Updated Product",
                        "price": 99.99
                    }
                    """))
            .andExpect(status().isForbidden());
    }
}
```

## Input Validation Testing

### SQL Injection Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
class SqlInjectionTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void should_prevent_sql_injection_in_search() throws Exception {
        String maliciousInput = "'; DROP TABLE customers; --";
        
        mockMvc.perform(get("/api/v1/products/search")
                .param("query", maliciousInput))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.items").isEmpty());
        
        // Verify table still exists
        mockMvc.perform(get("/api/v1/customers"))
            .andExpect(status().isOk());
    }
    
    @Test
    @WithMockUser(roles = "CUSTOMER")
    void should_sanitize_special_characters_in_input() throws Exception {
        String inputWithSpecialChars = "test' OR '1'='1";
        
        mockMvc.perform(post("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("""
                    {
                        "name": "%s",
                        "email": "test@example.com",
                        "password": "ValidPass123!"
                    }
                    """, inputWithSpecialChars)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }
}
```

### XSS Prevention Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
class XSSPreventionTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    @WithMockUser(roles = "CUSTOMER")
    void should_sanitize_html_in_review_content() throws Exception {
        String xssPayload = "<script>alert('XSS')</script>";
        
        MvcResult result = mockMvc.perform(post("/api/v1/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("""
                    {
                        "productId": "product-123",
                        "rating": 5,
                        "content": "%s"
                    }
                    """, xssPayload)))
            .andExpect(status().isCreated())
            .andReturn();
        
        String reviewId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        
        // Verify content is sanitized
        mockMvc.perform(get("/api/v1/reviews/" + reviewId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value(not(containsString("<script>"))));
    }
    
    @Test
    void should_set_security_headers_to_prevent_xss() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("X-Frame-Options", "DENY"))
            .andExpect(header().exists("Content-Security-Policy"));
    }
}
```

## Encryption Testing

### Data Encryption Tests

```java
@SpringBootTest
class EncryptionTest {
    
    @Autowired
    private AESEncryptionService encryptionService;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Test
    void should_encrypt_sensitive_fields_in_database() {
        // Create customer with sensitive data
        Customer customer = new Customer();
        customer.setId("test-customer");
        customer.setName("John Doe");
        customer.setEmail("john@example.com");
        customer.setPhoneNumber("+1-555-123-4567");
        
        customerRepository.save(customer);
        
        // Verify data is encrypted in database
        String encryptedEmail = jdbcTemplate.queryForObject(
            "SELECT email FROM customers WHERE id = ?",
            String.class,
            "test-customer"
        );
        
        assertThat(encryptedEmail).isNotEqualTo("john@example.com");
        assertThat(encryptedEmail).startsWith("encrypted:");
        
        // Verify data is decrypted when retrieved
        Customer retrieved = customerRepository.findById("test-customer").orElseThrow();
        assertThat(retrieved.getEmail()).isEqualTo("john@example.com");
    }
    
    @Test
    void should_use_different_iv_for_each_encryption() throws Exception {
        String plaintext = "sensitive-data";
        
        String encrypted1 = encryptionService.encrypt(plaintext);
        String encrypted2 = encryptionService.encrypt(plaintext);
        
        // Different IV should produce different ciphertext
        assertThat(encrypted1).isNotEqualTo(encrypted2);
        
        // But both should decrypt to same plaintext
        assertThat(encryptionService.decrypt(encrypted1)).isEqualTo(plaintext);
        assertThat(encryptionService.decrypt(encrypted2)).isEqualTo(plaintext);
    }
}
```

### TLS Configuration Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TLSConfigurationTest {
    
    @LocalServerPort
    private int port;
    
    @Test
    void should_enforce_https() {
        RestTemplate restTemplate = new RestTemplate();
        
        // HTTP request should be redirected to HTTPS
        assertThatThrownBy(() -> 
            restTemplate.getForEntity(
                "http://localhost:" + port + "/api/v1/products",
                String.class
            )
        ).isInstanceOf(ResourceAccessException.class);
    }
    
    @Test
    void should_use_tls_1_3() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
        sslContext.init(null, null, null);
        
        SSLSocketFactory socketFactory = sslContext.getSocketFactory();
        
        try (SSLSocket socket = (SSLSocket) socketFactory.createSocket("localhost", 8443)) {
            socket.startHandshake();
            
            String protocol = socket.getSession().getProtocol();
            assertThat(protocol).isEqualTo("TLSv1.3");
        }
    }
}
```

## Automated Security Scanning

### Dependency Vulnerability Scanning

```gradle
// build.gradle
plugins {
    id 'org.owasp.dependencycheck' version '8.4.0'
}

dependencyCheck {
    format = 'ALL'
    failBuildOnCVSS = 7
    suppressionFile = 'config/dependency-check-suppressions.xml'
    
    analyzers {
        assemblyEnabled = false
        nugetconfEnabled = false
        nodeEnabled = false
    }
}
```

### Static Code Analysis

```gradle
// build.gradle
plugins {
    id 'com.github.spotbugs' version '5.0.14'
}

spotbugs {
    effort = 'max'
    reportLevel = 'low'
    includeFilter = file('config/spotbugs-security-include.xml')
}

spotbugsMain {
    reports {
        html.enabled = true
        xml.enabled = false
    }
}
```

### SonarQube Integration

```yaml
# .github/workflows/security-scan.yml
name: Security Scan

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  security-scan:
    runs-on: ubuntu-latest
    
    steps:

      - uses: actions/checkout@v3
      
      - name: Set up JDK 21

        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      
      - name: Run OWASP Dependency Check

        run: ./gradlew dependencyCheckAnalyze
      
      - name: Run SpotBugs

        run: ./gradlew spotbugsMain
      
      - name: SonarQube Scan

        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: ./gradlew sonarqube
      
      - name: Upload Security Reports

        uses: actions/upload-artifact@v3
        with:
          name: security-reports
          path: |
            build/reports/dependency-check-report.html
            build/reports/spotbugs/main.html
```

## Penetration Testing

### OWASP ZAP Integration

```yaml
# docker-compose.zap.yml
version: '3.8'

services:
  zap:
    image: owasp/zap2docker-stable
    command: zap-baseline.py -t http://app:8080 -r zap-report.html
    volumes:

      - ./zap-reports:/zap/wrk

    depends_on:

      - app
    
  app:
    image: ecommerce-platform:latest
    ports:

      - "8080:8080"

    environment:

      - SPRING_PROFILES_ACTIVE=test

```

### Manual Penetration Testing Checklist

#### Authentication & Session Management

- [ ] Test for weak password policy
- [ ] Test for brute force protection
- [ ] Test for session fixation
- [ ] Test for session timeout
- [ ] Test for concurrent session handling
- [ ] Test for logout functionality
- [ ] Test for password reset mechanism

#### Authorization

- [ ] Test for privilege escalation
- [ ] Test for horizontal access control
- [ ] Test for vertical access control
- [ ] Test for forced browsing
- [ ] Test for missing function level access control

#### Input Validation

- [ ] Test for SQL injection
- [ ] Test for XSS (reflected, stored, DOM-based)
- [ ] Test for command injection
- [ ] Test for path traversal
- [ ] Test for XML injection
- [ ] Test for LDAP injection

#### Data Protection

- [ ] Test for sensitive data exposure
- [ ] Test for insecure cryptographic storage
- [ ] Test for insufficient transport layer protection
- [ ] Test for unencrypted communications

#### Business Logic

- [ ] Test for business logic flaws
- [ ] Test for race conditions
- [ ] Test for price manipulation
- [ ] Test for inventory manipulation

## Security Metrics

### Key Security Metrics

| Metric | Target | Measurement | Alert Threshold |
|--------|--------|-------------|-----------------|
| Critical Vulnerabilities | 0 | Dependency scan | Any critical |
| High Vulnerabilities | 0 | Dependency scan | > 0 |
| Failed Login Attempts | < 1% | Authentication logs | > 5% in 5 min |
| Authorization Failures | < 0.1% | Access logs | > 1% in 5 min |
| Security Test Coverage | > 80% | Code coverage | < 80% |
| Penetration Test Findings | 0 critical | Pen test report | Any critical |
| Security Incidents | 0 | Incident tracking | Any incident |

### Security Dashboard

```java
@RestController
@RequestMapping("/api/v1/admin/security")
@PreAuthorize("hasRole('ADMIN')")
public class SecurityMetricsController {
    
    @GetMapping("/metrics")
    public ResponseEntity<SecurityMetrics> getSecurityMetrics() {
        SecurityMetrics metrics = SecurityMetrics.builder()
            .authenticationFailureRate(calculateAuthFailureRate())
            .authorizationFailureRate(calculateAuthzFailureRate())
            .vulnerabilityCount(getVulnerabilityCount())
            .securityTestCoverage(getSecurityTestCoverage())
            .lastPenetrationTest(getLastPenTestDate())
            .openSecurityIncidents(getOpenIncidentCount())
            .build();
        
        return ResponseEntity.ok(metrics);
    }
}
```

## Continuous Security Testing

### CI/CD Integration

```yaml
# .github/workflows/ci.yml
name: CI Pipeline

on: [push, pull_request]

jobs:
  security-tests:
    runs-on: ubuntu-latest
    
    steps:

      - uses: actions/checkout@v3
      
      - name: Run Security Unit Tests

        run: ./gradlew test --tests '*Security*'
      
      - name: Run Security Integration Tests

        run: ./gradlew integrationTest --tests '*Security*'
      
      - name: Dependency Vulnerability Scan

        run: ./gradlew dependencyCheckAnalyze
      
      - name: Static Security Analysis

        run: ./gradlew spotbugsMain
      
      - name: Fail on Security Issues

        run: |
          if [ -f build/reports/dependency-check-report.xml ]; then
            if grep -q 'severity="CRITICAL"' build/reports/dependency-check-report.xml; then
              echo "Critical vulnerabilities found!"
              exit 1
            fi
          fi
```

## Best Practices

### ✅ Do

- Write security tests for all authentication and authorization logic
- Include security tests in CI/CD pipeline
- Run automated security scans on every commit
- Conduct regular penetration testing (quarterly)
- Monitor security metrics continuously
- Test with realistic attack scenarios
- Keep security testing tools updated
- Document all security test results

### ❌ Don't

- Don't skip security tests to save time
- Don't ignore security scan warnings
- Don't test only happy paths
- Don't forget to test error handling
- Don't use production data in security tests
- Don't disable security features in tests
- Don't forget to test third-party integrations

## Related Documentation

- [Security Overview](overview.md) - Overall security perspective
- [Authentication](authentication.md) - Authentication mechanisms
- [Authorization](authorization.md) - Authorization model
- [Data Protection](data-protection.md) - Data protection strategies

## References

- OWASP Testing Guide: <https://owasp.org/www-project-web-security-testing-guide/>
- OWASP ZAP: <https://www.zaproxy.org/>
- Spring Security Testing: <https://docs.spring.io/spring-security/reference/servlet/test/index.html>
- OWASP Dependency-Check: <https://owasp.org/www-project-dependency-check/>
