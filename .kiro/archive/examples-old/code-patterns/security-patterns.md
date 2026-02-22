# Security Patterns

## Overview

Security implementation patterns and best practices for our project.

**Related Standards**: [Security Standards](../../steering/security-standards.md)

---

## Input Validation

### Bean Validation

```java
// ✅ GOOD: Comprehensive validation
public record CreateCustomerRequest(
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100)
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Name can only contain letters")
    String name,
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128)
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
             message = "Password must contain uppercase, lowercase, number and special character")
    String password
) {}
```

### SQL Injection Prevention

```java
// ✅ GOOD: Parameterized queries
@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {
    
    @Query("SELECT c FROM Customer c WHERE c.email = :email AND c.status = :status")
    Optional<Customer> findByEmailAndStatus(
        @Param("email") String email, 
        @Param("status") String status
    );
}

// ❌ BAD: String concatenation
@Query(value = "SELECT * FROM customers WHERE email = '" + email + "'", nativeQuery = true)
List<Customer> findByEmail(String email); // SQL injection risk!
```

### XSS Prevention

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
        
        // Sanitize user input
        String sanitizedName = xssProtectionService.sanitizeHtml(request.name());
        
        Customer customer = customerService.createCustomer(sanitizedName, request.email());
        return ResponseEntity.ok(CustomerResponse.from(customer));
    }
}
```

---

## Authentication

### JWT Implementation

```java
@Component
public class JwtTokenProvider {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
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
    
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();
        return claims.getSubject();
    }
}
```

### Password Security

```java
@Component
public class PasswordService {
    
    private static final int BCRYPT_STRENGTH = 12;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    
    public String encodePassword(String rawPassword) {
        validatePasswordStrength(rawPassword);
        return encoder.encode(rawPassword);
    }
    
    public boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
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

### Method-Level Security

```java
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfiguration {
    
    @Bean
    public PermissionEvaluator permissionEvaluator() {
        return new CustomPermissionEvaluator();
    }
}

@Service
public class OrderService {
    
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and #customerId == authentication.principal.customerId)")
    public List<Order> getCustomerOrders(String customerId) {
        return orderRepository.findByCustomerId(customerId);
    }
    
    @PreAuthorize("hasPermission(#order, 'READ')")
    public Order getOrder(Order order) {
        return order;
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteOrder(String orderId) {
        orderRepository.deleteById(orderId);
    }
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
    
    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
}
```

---

## Data Protection

### Encryption at Rest

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

### Data Masking

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

---

## Security Headers

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

---

## CORS Configuration

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

---

## Security Testing

```java
@SpringBootTest
@AutoConfigureTestDatabase
class SecurityTest {
    
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
            .andExpect(status().isBadRequest());
    }
}
```

---

## Summary

Key security practices:

1. **Validate all input** - Never trust user input
2. **Use parameterized queries** - Prevent SQL injection
3. **Sanitize output** - Prevent XSS attacks
4. **Encrypt sensitive data** - At rest and in transit
5. **Implement proper authentication** - JWT with secure secrets
6. **Enforce authorization** - Method-level security
7. **Set security headers** - HSTS, CSP, X-Frame-Options
8. **Test security** - Automated security tests

---

**Related Documentation**:
- [Security Standards](../../steering/security-standards.md)
- [Error Handling](error-handling.md)
- [Code Quality Checklist](../../steering/code-quality-checklist.md)
