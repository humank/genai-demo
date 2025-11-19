# API Security

> **Status**: ✅ Active  
> **Last Updated**: 2024-11-19

## Overview

This document provides comprehensive API security guidance for the GenAI Demo platform.

---

## Quick Reference

For complete security documentation, see:
- [Security Overview](../../perspectives/security/overview.md) - Complete security architecture
- [Security Standards](.kiro/steering/security-standards.md) - Security development standards
- [API Overview](../README.md) - API documentation

---

## Contents

### Authentication
- [JWT Authentication](#jwt-authentication)
- [API Keys](#api-keys)
- [OAuth 2.0](#oauth-20)

### Authorization
- [Role-Based Access Control](#role-based-access-control)
- [Resource-Level Permissions](#resource-level-permissions)

### Security Best Practices
- [Input Validation](#input-validation)
- [Rate Limiting](#rate-limiting)
- [HTTPS/TLS](#httpstls)

---

## JWT Authentication

### Token Structure

```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "user123",
    "name": "John Doe",
    "email": "john@example.com",
    "roles": ["USER", "ADMIN"],
    "iat": 1700000000,
    "exp": 1700003600
  }
}
```

### Implementation

```java
@Component
public class JwtTokenProvider {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration}")
    private int jwtExpiration;
    
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList()));
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(userDetails.getUsername())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration * 1000))
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

### Usage

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "password123"
}

Response:
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600,
  "tokenType": "Bearer"
}

# Use token in subsequent requests
GET /api/v1/customers/123
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## API Keys

### Generation

```java
@Service
public class ApiKeyService {
    
    public ApiKey generateApiKey(String userId, String description) {
        String key = "sk_" + generateSecureRandomString(32);
        String hashedKey = hashApiKey(key);
        
        ApiKey apiKey = ApiKey.builder()
            .id(UUID.randomUUID().toString())
            .userId(userId)
            .keyHash(hashedKey)
            .description(description)
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plus(365, ChronoUnit.DAYS))
            .build();
        
        apiKeyRepository.save(apiKey);
        
        // Return plain key only once
        return apiKey.withPlainKey(key);
    }
    
    private String hashApiKey(String key) {
        return BCrypt.hashpw(key, BCrypt.gensalt(12));
    }
}
```

### Usage

```http
GET /api/v1/products
X-API-Key: sk_abc123def456ghi789jkl012mno345pq
```

---

## OAuth 2.0

### Authorization Code Flow

```java
@Configuration
@EnableAuthorizationServer
public class OAuth2Configuration extends AuthorizationServerConfigurerAdapter {
    
    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients.inMemory()
            .withClient("web-client")
            .secret(passwordEncoder.encode("secret"))
            .authorizedGrantTypes("authorization_code", "refresh_token")
            .scopes("read", "write")
            .redirectUris("https://example.com/callback");
    }
}
```

### Flow

```
1. Client redirects to authorization endpoint
   GET /oauth/authorize?
       response_type=code&
       client_id=web-client&
       redirect_uri=https://example.com/callback&
       scope=read+write

2. User authenticates and authorizes

3. Authorization server redirects back with code
   https://example.com/callback?code=abc123

4. Client exchanges code for token
   POST /oauth/token
   Content-Type: application/x-www-form-urlencoded
   
   grant_type=authorization_code&
   code=abc123&
   client_id=web-client&
   client_secret=secret&
   redirect_uri=https://example.com/callback

5. Response with access token
   {
     "access_token": "eyJhbGc...",
     "token_type": "Bearer",
     "expires_in": 3600,
     "refresh_token": "def456..."
   }
```

---

## Role-Based Access Control

### Implementation

```java
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/public/**").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/customers/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt);
        
        return http.build();
    }
}

// Method-level security
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public CustomerResponse getCustomer(@PathVariable String id) {
        return customerService.findById(id);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteCustomer(@PathVariable String id) {
        customerService.delete(id);
    }
    
    @GetMapping("/{id}/orders")
    @PreAuthorize("@securityService.canAccessCustomerOrders(#id)")
    public List<OrderResponse> getCustomerOrders(@PathVariable String id) {
        return orderService.findByCustomerId(id);
    }
}
```

---

## Resource-Level Permissions

### Custom Permission Evaluator

```java
@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {
    
    @Override
    public boolean hasPermission(
            Authentication authentication,
            Object targetDomainObject,
            Object permission) {
        
        if (targetDomainObject instanceof Order order) {
            return evaluateOrderPermission(authentication, order, permission.toString());
        }
        
        return false;
    }
    
    private boolean evaluateOrderPermission(
            Authentication auth,
            Order order,
            String permission) {
        
        String userId = auth.getName();
        
        return switch (permission) {
            case "READ" -> order.getCustomerId().equals(userId) || hasRole(auth, "ADMIN");
            case "WRITE" -> order.getCustomerId().equals(userId) && order.isModifiable();
            case "DELETE" -> hasRole(auth, "ADMIN");
            default -> false;
        };
    }
}

// Usage
@PreAuthorize("hasPermission(#order, 'READ')")
public Order getOrder(Order order) {
    return order;
}
```

---

## Input Validation

### Request Validation

```java
public record CreateCustomerRequest(
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100)
    String name,
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128)
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
             message = "Password must contain uppercase, lowercase, and number")
    String password
) {}

@PostMapping("/customers")
public ResponseEntity<CustomerResponse> createCustomer(
        @Valid @RequestBody CreateCustomerRequest request) {
    // Request is automatically validated
    Customer customer = customerService.createCustomer(request);
    return ResponseEntity.ok(CustomerResponse.from(customer));
}
```

### SQL Injection Prevention

```java
// ✅ Good: Parameterized query
@Query("SELECT c FROM Customer c WHERE c.email = :email")
Optional<Customer> findByEmail(@Param("email") String email);

// ❌ Bad: String concatenation
String query = "SELECT * FROM customers WHERE email = '" + email + "'";
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
```

---

## Rate Limiting

See [Rate Limiting Guide](../integration/rate-limiting.md) for complete documentation.

### Quick Example

```java
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        String clientId = extractClientId(request);
        
        if (!rateLimiter.allowRequest(clientId, 60, Duration.ofMinutes(1))) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
}
```

---

## HTTPS/TLS

### Configuration

```yaml
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

### Security Headers

```java
@Configuration
public class SecurityHeadersConfiguration {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.headers(headers -> headers
            .contentTypeOptions(Customizer.withDefaults())
            .xssProtection(Customizer.withDefaults())
            .frameOptions(frame -> frame.deny())
            .httpStrictTransportSecurity(hsts -> hsts
                .maxAgeInSeconds(31536000)
                .includeSubdomains(true)
                .preload(true)
            )
        );
        
        return http.build();
    }
}
```

---

## Security Testing

### Authentication Tests

```java
@Test
void should_reject_request_without_token() throws Exception {
    mockMvc.perform(get("/api/v1/customers/123"))
        .andExpect(status().isUnauthorized());
}

@Test
void should_accept_request_with_valid_token() throws Exception {
    String token = generateValidToken();
    
    mockMvc.perform(get("/api/v1/customers/123")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
}
```

### Authorization Tests

```java
@Test
void should_allow_admin_to_delete_customer() throws Exception {
    String adminToken = generateAdminToken();
    
    mockMvc.perform(delete("/api/v1/customers/123")
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());
}

@Test
void should_deny_user_to_delete_customer() throws Exception {
    String userToken = generateUserToken();
    
    mockMvc.perform(delete("/api/v1/customers/123")
            .header("Authorization", "Bearer " + userToken))
        .andExpect(status().isForbidden());
}
```

---

## Related Documentation

- [Security Overview](../../perspectives/security/overview.md)
- [Security Standards](.kiro/steering/security-standards.md)
- [API Overview](../README.md)
- [Rate Limiting](../integration/rate-limiting.md)

---

**Document Version**: 1.0  
**Last Updated**: 2024-11-19  
**Owner**: Security Team
