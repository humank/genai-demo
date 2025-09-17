---
inclusion: always
---

# Security Standards and Guidelines

## Authentication and Authorization

### Authentication Standards

#### JWT Token Implementation

```java
@Component
public class JwtTokenProvider {
    
    private static final int TOKEN_VALIDITY = 3600; // 1 hour
    private static final int REFRESH_TOKEN_VALIDITY = 86400; // 24 hours
    
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList()));
        claims.put("userId", userDetails.getUsername());
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(userDetails.getUsername())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + TOKEN_VALIDITY * 1000))
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
}
```

#### Password Security Standards

```java
@Component
public class PasswordEncoder {
    
    private static final int BCRYPT_STRENGTH = 12;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    
    public String encode(String rawPassword) {
        validatePasswordStrength(rawPassword);
        return encoder.encode(rawPassword);
    }
    
    private void validatePasswordStrength(String password) {
        if (password.length() < 8) {
            throw new WeakPasswordException("Password must be at least 8 characters");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new WeakPasswordException("Password must contain uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new WeakPasswordException("Password must contain lowercase letter");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new WeakPasswordException("Password must contain number");
        }
        if (!password.matches(".*[!@#$%^&*()].*")) {
            throw new WeakPasswordException("Password must contain special character");
        }
    }
}
```

### Authorization Standards

#### Role-Based Access Control (RBAC)

```java
@PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and #customerId == authentication.principal.customerId)")
public Customer getCustomer(@PathVariable String customerId) {
    return customerService.findById(customerId);
}

@PreAuthorize("hasPermission(#order, 'READ')")
public Order getOrder(@PathVariable String orderId) {
    return orderService.findById(orderId);
}
```

#### Method-Level Security

```java
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfiguration {
    
    @Bean
    public PermissionEvaluator permissionEvaluator() {
        return new CustomPermissionEvaluator();
    }
}

public class CustomPermissionEvaluator implements PermissionEvaluator {
    
    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (targetDomainObject instanceof Order order) {
            return evaluateOrderPermission(authentication, order, permission.toString());
        }
        return false;
    }
    
    private boolean evaluateOrderPermission(Authentication auth, Order order, String permission) {
        String userId = auth.getName();
        
        return switch (permission) {
            case "READ" -> order.getCustomerId().equals(userId) || hasRole(auth, "ADMIN");
            case "WRITE" -> order.getCustomerId().equals(userId) && order.isModifiable();
            case "DELETE" -> hasRole(auth, "ADMIN");
            default -> false;
        };
    }
}
```

## Data Protection

### Encryption Standards

#### Data at Rest Encryption

```java
@Entity
public class Customer {
    
    @Id
    private String id;
    
    @Column
    private String name;
    
    @Column
    @Convert(converter = EncryptedStringConverter.class)
    private String email; // Encrypted in database
    
    @Column
    @Convert(converter = EncryptedStringConverter.class)
    private String phoneNumber; // Encrypted in database
    
    @Column
    private String hashedPassword; // Already hashed, no additional encryption needed
}

@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {
    
    private final AESUtil aesUtil;
    
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        return aesUtil.encrypt(attribute);
    }
    
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return aesUtil.decrypt(dbData);
    }
}
```

#### Data in Transit Encryption

```yaml
# application.yml - Force HTTPS
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: tomcat
  port: 8443

# Redirect HTTP to HTTPS
security:
  require-ssl: true
```

### Data Masking and Anonymization

```java
@Component
public class DataMaskingService {
    
    public String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];
        
        if (localPart.length() <= 2) {
            return "*".repeat(localPart.length()) + "@" + domain;
        }
        
        return localPart.charAt(0) + "*".repeat(localPart.length() - 2) + 
               localPart.charAt(localPart.length() - 1) + "@" + domain;
    }
    
    public String maskCreditCard(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) return cardNumber;
        return "*".repeat(cardNumber.length() - 4) + cardNumber.substring(cardNumber.length() - 4);
    }
    
    public CustomerDto maskSensitiveData(Customer customer, String requesterRole) {
        CustomerDto dto = CustomerDto.from(customer);
        
        if (!"ADMIN".equals(requesterRole)) {
            dto.setEmail(maskEmail(dto.getEmail()));
            dto.setPhoneNumber(maskPhoneNumber(dto.getPhoneNumber()));
        }
        
        return dto;
    }
}
```

## Input Validation and Sanitization

### Request Validation Standards

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
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$", 
             message = "Password must contain uppercase, lowercase, number and special character")
    String password,
    
    @Valid
    @NotNull(message = "Address is required")
    AddressDto address
) {}
```

### SQL Injection Prevention

```java
@Repository
public class CustomerRepository {
    
    // ✅ Good: Using parameterized queries
    @Query("SELECT c FROM Customer c WHERE c.email = :email AND c.status = :status")
    Optional<Customer> findByEmailAndStatus(@Param("email") String email, @Param("status") String status);
    
    // ✅ Good: Using Criteria API for dynamic queries
    public List<Customer> findCustomersWithCriteria(CustomerSearchCriteria criteria) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Customer> query = cb.createQuery(Customer.class);
        Root<Customer> root = query.from(Customer.class);
        
        List<Predicate> predicates = new ArrayList<>();
        
        if (criteria.getName() != null) {
            predicates.add(cb.like(root.get("name"), "%" + criteria.getName() + "%"));
        }
        
        if (criteria.getEmail() != null) {
            predicates.add(cb.equal(root.get("email"), criteria.getEmail()));
        }
        
        query.where(predicates.toArray(new Predicate[0]));
        return entityManager.createQuery(query).getResultList();
    }
}
```

### XSS Prevention

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

@RestController
public class CustomerController {
    
    @PostMapping("/customers")
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {
        
        // Sanitize input to prevent XSS
        String sanitizedName = xssProtectionService.sanitizeHtml(request.name());
        
        CreateCustomerCommand command = new CreateCustomerCommand(
            sanitizedName,
            request.email(),
            request.password()
        );
        
        Customer customer = customerService.createCustomer(command);
        return ResponseEntity.ok(CustomerResponse.from(customer));
    }
}
```

## Security Headers and CORS

### Security Headers Configuration

```java
@Configuration
@EnableWebSecurity
public class SecurityHeadersConfiguration {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.headers(headers -> headers
            .frameOptions().deny()
            .contentTypeOptions().and()
            .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                .maxAgeInSeconds(31536000)
                .includeSubdomains(true)
                .preload(true))
            .and()
            .addHeaderWriter(new StaticHeadersWriter("X-Content-Type-Options", "nosniff"))
            .addHeaderWriter(new StaticHeadersWriter("X-Frame-Options", "DENY"))
            .addHeaderWriter(new StaticHeadersWriter("X-XSS-Protection", "1; mode=block"))
            .addHeaderWriter(new StaticHeadersWriter("Referrer-Policy", "strict-origin-when-cross-origin"))
            .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy", 
                "geolocation=(), microphone=(), camera=()"))
        );
        
        return http.build();
    }
}
```

### CORS Configuration

```java
@Configuration
public class CorsConfiguration {
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Only allow specific origins in production
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "https://*.yourdomain.com",
            "https://yourdomain.com"
        ));
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        
        return source;
    }
}
```

## Security Testing Standards

### Security Test Categories

#### Authentication Tests

```java
@SpringBootTest
@AutoConfigureTestDatabase
class AuthenticationSecurityTest {
    
    @Test
    void should_reject_request_without_token() throws Exception {
        mockMvc.perform(get("/api/v1/customers/123"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    void should_reject_request_with_expired_token() throws Exception {
        String expiredToken = createExpiredToken();
        
        mockMvc.perform(get("/api/v1/customers/123")
                .header("Authorization", "Bearer " + expiredToken))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    void should_reject_request_with_invalid_signature() throws Exception {
        String tamperedToken = createTamperedToken();
        
        mockMvc.perform(get("/api/v1/customers/123")
                .header("Authorization", "Bearer " + tamperedToken))
            .andExpected(status().isUnauthorized());
    }
}
```

#### Authorization Tests

```java
@Test
void should_allow_user_to_access_own_data() throws Exception {
    String userToken = createTokenForUser("user123");
    
    mockMvc.perform(get("/api/v1/customers/user123")
            .header("Authorization", "Bearer " + userToken))
        .andExpect(status().isOk());
}

@Test
void should_deny_user_access_to_other_user_data() throws Exception {
    String userToken = createTokenForUser("user123");
    
    mockMvc.perform(get("/api/v1/customers/user456")
            .header("Authorization", "Bearer " + userToken))
        .andExpect(status().isForbidden());
}
```

#### Input Validation Tests

```java
@Test
void should_reject_sql_injection_attempt() throws Exception {
    String maliciousInput = "'; DROP TABLE customers; --";
    
    CreateCustomerRequest request = new CreateCustomerRequest(
        maliciousInput,
        "test@example.com",
        "ValidPassword123!"
    );
    
    mockMvc.perform(post("/api/v1/customers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
}

@Test
void should_sanitize_xss_attempt() throws Exception {
    String xssPayload = "<script>alert('XSS')</script>";
    
    CreateCustomerRequest request = new CreateCustomerRequest(
        xssPayload,
        "test@example.com",
        "ValidPassword123!"
    );
    
    mockMvc.perform(post("/api/v1/customers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
}
```

## Security Monitoring and Incident Response

### Security Event Logging

```java
@Component
public class SecurityEventLogger {
    
    private final Logger securityLogger = LoggerFactory.getLogger("SECURITY");
    
    public void logAuthenticationFailure(String username, String ipAddress, String reason) {
        securityLogger.warn("Authentication failure",
            kv("event", "AUTH_FAILURE"),
            kv("username", username),
            kv("ipAddress", ipAddress),
            kv("reason", reason),
            kv("timestamp", Instant.now()));
    }
    
    public void logAuthorizationFailure(String username, String resource, String action) {
        securityLogger.warn("Authorization failure",
            kv("event", "AUTHZ_FAILURE"),
            kv("username", username),
            kv("resource", resource),
            kv("action", action),
            kv("timestamp", Instant.now()));
    }
    
    public void logSuspiciousActivity(String username, String activity, Map<String, Object> context) {
        securityLogger.error("Suspicious activity detected",
            kv("event", "SUSPICIOUS_ACTIVITY"),
            kv("username", username),
            kv("activity", activity),
            kv("context", context),
            kv("timestamp", Instant.now()));
    }
}
```

### Security Metrics and Alerts

```java
@Component
public class SecurityMetrics {
    
    private final Counter authenticationFailures;
    private final Counter authorizationFailures;
    private final Counter suspiciousActivities;
    
    public SecurityMetrics(MeterRegistry meterRegistry) {
        this.authenticationFailures = Counter.builder("security.authentication.failures")
            .description("Number of authentication failures")
            .register(meterRegistry);
            
        this.authorizationFailures = Counter.builder("security.authorization.failures")
            .description("Number of authorization failures")
            .register(meterRegistry);
            
        this.suspiciousActivities = Counter.builder("security.suspicious.activities")
            .description("Number of suspicious activities detected")
            .register(meterRegistry);
    }
    
    public void recordAuthenticationFailure(String reason) {
        authenticationFailures.increment(Tags.of("reason", reason));
    }
    
    public void recordAuthorizationFailure(String resource) {
        authorizationFailures.increment(Tags.of("resource", resource));
    }
    
    public void recordSuspiciousActivity(String activityType) {
        suspiciousActivities.increment(Tags.of("type", activityType));
    }
}
```

## Compliance and Audit Requirements

### GDPR Compliance

```java
@Service
public class GDPRComplianceService {
    
    public void handleDataSubjectRequest(DataSubjectRequest request) {
        switch (request.getType()) {
            case ACCESS -> provideDataAccess(request.getSubjectId());
            case RECTIFICATION -> updatePersonalData(request.getSubjectId(), request.getData());
            case ERASURE -> deletePersonalData(request.getSubjectId());
            case PORTABILITY -> exportPersonalData(request.getSubjectId());
            case RESTRICTION -> restrictProcessing(request.getSubjectId());
        }
        
        auditLogger.logDataSubjectRequest(request);
    }
    
    private void deletePersonalData(String subjectId) {
        // Anonymize rather than delete to maintain referential integrity
        Customer customer = customerRepository.findById(subjectId)
            .orElseThrow(() -> new CustomerNotFoundException(subjectId));
            
        customer.anonymize(); // Replace PII with anonymous values
        customerRepository.save(customer);
        
        // Also handle related data
        orderRepository.anonymizeCustomerOrders(subjectId);
        paymentRepository.anonymizeCustomerPayments(subjectId);
    }
}
```

### Audit Trail Implementation

```java
@Entity
@Table(name = "audit_log")
public class AuditLogEntry {
    
    @Id
    private String id;
    
    @Column(name = "entity_type")
    private String entityType;
    
    @Column(name = "entity_id")
    private String entityId;
    
    @Column(name = "action")
    private String action; // CREATE, UPDATE, DELETE, ACCESS
    
    @Column(name = "user_id")
    private String userId;
    
    @Column(name = "timestamp")
    private Instant timestamp;
    
    @Column(name = "old_values", columnDefinition = "TEXT")
    private String oldValues;
    
    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValues;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "user_agent")
    private String userAgent;
}

@Component
public class AuditService {
    
    @EventListener
    public void handleEntityCreated(EntityCreatedEvent event) {
        AuditLogEntry entry = AuditLogEntry.builder()
            .entityType(event.getEntityType())
            .entityId(event.getEntityId())
            .action("CREATE")
            .userId(getCurrentUserId())
            .timestamp(Instant.now())
            .newValues(serializeEntity(event.getEntity()))
            .ipAddress(getCurrentIpAddress())
            .userAgent(getCurrentUserAgent())
            .build();
            
        auditLogRepository.save(entry);
    }
}
```

## Security Checklist for New Features

### Pre-Development Security Review

- [ ] Threat modeling completed
- [ ] Security requirements defined
- [ ] Data classification performed
- [ ] Privacy impact assessment conducted

### Development Security Checklist

- [ ] Input validation implemented
- [ ] Output encoding applied
- [ ] Authentication and authorization implemented
- [ ] Sensitive data encrypted
- [ ] Security headers configured
- [ ] Error handling doesn't leak information
- [ ] Logging includes security events

### Testing Security Checklist

- [ ] Authentication tests written
- [ ] Authorization tests written
- [ ] Input validation tests written
- [ ] XSS prevention tests written
- [ ] SQL injection prevention tests written
- [ ] Security headers tests written

### Deployment Security Checklist

- [ ] Security configuration reviewed
- [ ] Secrets management implemented
- [ ] Network security configured
- [ ] Monitoring and alerting configured
- [ ] Incident response plan updated
- [ ] Security documentation updated
