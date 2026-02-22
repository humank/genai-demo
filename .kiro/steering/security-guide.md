---
inclusion: always
last_updated: 2026-02-21
---

# Security Guide

Complete guide for security implementation, authentication, and best practices.

## Quick Reference

### Security Checklist
- [ ] Authentication: JWT with proper expiration
- [ ] Authorization: Role-based access control
- [ ] Input validation: All user inputs validated
- [ ] SQL injection: Use parameterized queries
- [ ] XSS prevention: Sanitize all outputs
- [ ] Encryption: TLS 1.3 in transit, AES-256 at rest

### Common Commands
```bash
./gradlew checkstyleMain          # Check security patterns
./gradlew spotbugsMain            # Find security bugs
./gradlew dependencyCheckAnalyze  # Check vulnerable dependencies
```

---

## Authentication

### JWT Implementation

**Must Follow:**
- Token expiration: 1 hour (access), 24 hours (refresh)
- Use HS512 or RS256 algorithm
- Include user roles in claims
- Validate signature on every request

**Example:**
```java
@Component
public class JwtTokenProvider {
    
    private static final int TOKEN_VALIDITY = 3600; // 1 hour
    
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList()));
        
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

### Password Security

**Must Follow:**
- Minimum 8 characters
- Require uppercase, lowercase, number, special char
- Use BCrypt with strength 12
- Never log passwords

**Example:**
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

---

## Authorization

### Role-Based Access Control

**Must Follow:**
- Use `@PreAuthorize` for method-level security
- Implement custom permission evaluator
- Check ownership for user-specific resources

**Example:**
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

### Custom Permission Evaluator

```java
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

---

## Input Validation

### Request Validation

**Must Follow:**
- Use `@Valid` on all request objects
- Validate all fields with appropriate constraints
- Use custom validators for complex rules

**Example:**
```java
public record CreateCustomerRequest(
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100)
    @Pattern(regexp = "^[a-zA-Z\\s]+$")
    String name,
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    String email,
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128)
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$")
    String password
) {}
```

### SQL Injection Prevention

**Must Follow:**
- Always use parameterized queries
- Never concatenate user input into SQL
- Use JPA/Hibernate for database access

**Example:**
```java
@Repository
public class CustomerRepository {
    
    // ✅ Good: Parameterized query
    @Query("SELECT c FROM Customer c WHERE c.email = :email AND c.status = :status")
    Optional<Customer> findByEmailAndStatus(@Param("email") String email, @Param("status") String status);
    
    // ❌ Bad: String concatenation (vulnerable to SQL injection)
    // String query = "SELECT * FROM customers WHERE email = '" + email + "'";
}
```

### XSS Prevention

**Must Follow:**
- Sanitize all HTML input
- Escape output in templates
- Use Content Security Policy headers

**Example:**
```java
@Component
public class XSSProtectionService {
    
    private final PolicyFactory policy = Sanitizers.FORMATTING
        .and(Sanitizers.LINKS)
        .and(Sanitizers.BLOCKS);
    
    public String sanitizeHtml(String input) {
        if (input == null) return null;
        return policy.sanitize(input);
    }
}

@RestController
public class CustomerController {
    
    @PostMapping("/customers")
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {
        
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

---

## Data Protection

### Encryption at Rest

**Must Follow:**
- Encrypt sensitive fields in database
- Use AES-256 encryption
- Store encryption keys securely (AWS KMS)

**Example:**
```java
@Entity
public class Customer {
    
    @Id
    private String id;
    
    @Column
    private String name;
    
    @Column
    @Convert(converter = EncryptedStringConverter.class)
    private String email; // Encrypted
    
    @Column
    @Convert(converter = EncryptedStringConverter.class)
    private String phoneNumber; // Encrypted
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

### Encryption in Transit

**Must Follow:**
- Use TLS 1.3 for all connections
- Enforce HTTPS in production
- Use HSTS headers

```yaml
# application.yml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
  port: 8443

security:
  require-ssl: true
```

---

## Security Headers

### Required Headers

**Must Follow:**
- Content-Security-Policy
- X-Content-Type-Options: nosniff
- X-Frame-Options: DENY
- Strict-Transport-Security

**Example:**
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
                .includeSubdomains(true))
            .and()
            .addHeaderWriter(new StaticHeadersWriter("X-Content-Type-Options", "nosniff"))
            .addHeaderWriter(new StaticHeadersWriter("X-Frame-Options", "DENY"))
        );
        
        return http.build();
    }
}
```

---

## Security Testing

### Authentication Tests

```java
@SpringBootTest
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
}
```

### Authorization Tests

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

---

## Security Checklist

### Pre-Deployment

- [ ] All endpoints require authentication
- [ ] Authorization rules implemented
- [ ] Input validation on all endpoints
- [ ] SQL injection prevention verified
- [ ] XSS prevention implemented
- [ ] Sensitive data encrypted
- [ ] Security headers configured
- [ ] HTTPS enforced
- [ ] Security tests passing

### Post-Deployment

- [ ] Monitor authentication failures
- [ ] Track authorization violations
- [ ] Alert on suspicious activities
- [ ] Regular security audits
- [ ] Dependency vulnerability scans

---

## Related Guides

- **Development**: See `development-guide.md` for testing
- **Architecture**: See `architecture-guide.md` for design patterns
- **Performance**: See `performance-guide.md` for optimization

---

**Last Updated**: 2026-02-21
**Owner**: Security Team
