# Authentication

> **Last Updated**: 2025-10-23  
> **Status**: ✅ Active

## Overview

This document describes the authentication mechanisms used in the e-commerce platform. Authentication is the process of verifying the identity of users and systems before granting access to resources. The system uses JWT (JSON Web Tokens) as the primary authentication mechanism, providing stateless, scalable authentication across all services.

## Authentication Strategy

### JWT-Based Authentication

The system implements JWT-based authentication with the following characteristics:

- **Stateless**: No server-side session storage required
- **Scalable**: Works seamlessly across multiple service instances
- **Secure**: Cryptographically signed tokens prevent tampering
- **Efficient**: Fast token validation with minimal overhead

### Token Types

#### Access Token
- **Purpose**: Short-lived token for API access
- **Validity**: 1 hour
- **Contains**: User ID, roles, permissions
- **Usage**: Included in Authorization header for all API requests

#### Refresh Token
- **Purpose**: Long-lived token for obtaining new access tokens
- **Validity**: 24 hours
- **Contains**: User ID, token family ID
- **Usage**: Used to obtain new access token when current one expires

## Authentication Flow

### Initial Login

```
1. User submits credentials (email + password)
2. System validates credentials against database
3. System generates access token and refresh token
4. Tokens returned to client
5. Client stores tokens securely
```

### Subsequent Requests

```
1. Client includes access token in Authorization header
2. System validates token signature and expiration
3. System extracts user identity and permissions
4. Request processed with user context
```

### Token Refresh

```
1. Client detects access token expiration
2. Client sends refresh token to /auth/refresh endpoint
3. System validates refresh token
4. System generates new access token
5. New access token returned to client
```

## JWT Token Structure

### Access Token Claims

```json
{
  "sub": "user-123",
  "userId": "user-123",
  "email": "customer@example.com",
  "roles": ["CUSTOMER"],
  "permissions": ["order:read", "order:create"],
  "iat": 1698765432,
  "exp": 1698769032,
  "iss": "ecommerce-platform",
  "aud": "ecommerce-api"
}
```

### Refresh Token Claims

```json
{
  "sub": "user-123",
  "userId": "user-123",
  "tokenFamily": "family-456",
  "iat": 1698765432,
  "exp": 1698851832,
  "iss": "ecommerce-platform",
  "type": "refresh"
}
```

## Implementation

### JWT Token Provider

```java
@Component
public class JwtTokenProvider {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.access-token-validity:3600}")
    private int accessTokenValidity; // 1 hour
    
    @Value("${jwt.refresh-token-validity:86400}")
    private int refreshTokenValidity; // 24 hours
    
    /**
     * Generate access token for authenticated user
     */
    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userDetails.getUsername());
        claims.put("email", userDetails.getEmail());
        claims.put("roles", userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList()));
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(userDetails.getUsername())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + accessTokenValidity * 1000))
            .setIssuer("ecommerce-platform")
            .setAudience("ecommerce-api")
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }
    
    /**
     * Generate refresh token for user
     */
    public String generateRefreshToken(String userId) {
        String tokenFamily = UUID.randomUUID().toString();
        
        return Jwts.builder()
            .setSubject(userId)
            .claim("userId", userId)
            .claim("tokenFamily", tokenFamily)
            .claim("type", "refresh")
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + refreshTokenValidity * 1000))
            .setIssuer("ecommerce-platform")
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }
    
    /**
     * Validate token signature and expiration
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            logger.debug("JWT token expired: {}", e.getMessage());
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Extract user ID from token
     */
    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();
        return claims.get("userId", String.class);
    }
    
    /**
     * Extract roles from token
     */
    public List<String> getRolesFromToken(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();
        return claims.get("roles", List.class);
    }
}
```

### Authentication Filter

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        try {
            String token = extractTokenFromRequest(request);
            
            if (token != null && tokenProvider.validateToken(token)) {
                String userId = tokenProvider.getUserIdFromToken(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(userId);
                
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );
                
                authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                );
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

### Authentication Controller

```java
@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {
    
    private final AuthenticationService authenticationService;
    
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @Valid @RequestBody LoginRequest request) {
        
        AuthenticationResponse response = authenticationService.authenticate(
            request.email(),
            request.password()
        );
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request) {
        
        TokenRefreshResponse response = authenticationService.refreshToken(
            request.refreshToken()
        );
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String token) {
        
        authenticationService.logout(token);
        return ResponseEntity.noContent().build();
    }
}
```

## Password Security

### Password Requirements

- **Minimum Length**: 8 characters
- **Complexity**: Must contain:
  - At least one uppercase letter (A-Z)
  - At least one lowercase letter (a-z)
  - At least one digit (0-9)
  - At least one special character (!@#$%^&*())

### Password Hashing

```java
@Component
public class PasswordEncoderService {
    
    private static final int BCRYPT_STRENGTH = 12;
    private final BCryptPasswordEncoder encoder = 
        new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    
    /**
     * Hash password using BCrypt with strength factor 12
     */
    public String encodePassword(String rawPassword) {
        validatePasswordStrength(rawPassword);
        return encoder.encode(rawPassword);
    }
    
    /**
     * Verify password matches hash
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
    
    /**
     * Validate password meets strength requirements
     */
    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new WeakPasswordException(
                "Password must be at least 8 characters"
            );
        }
        
        if (!password.matches(".*[A-Z].*")) {
            throw new WeakPasswordException(
                "Password must contain at least one uppercase letter"
            );
        }
        
        if (!password.matches(".*[a-z].*")) {
            throw new WeakPasswordException(
                "Password must contain at least one lowercase letter"
            );
        }
        
        if (!password.matches(".*[0-9].*")) {
            throw new WeakPasswordException(
                "Password must contain at least one digit"
            );
        }
        
        if (!password.matches(".*[!@#$%^&*()].*")) {
            throw new WeakPasswordException(
                "Password must contain at least one special character"
            );
        }
    }
}
```

## Security Configuration

### Spring Security Configuration

```java
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .cors().and()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/public/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling()
                .authenticationEntryPoint(authenticationEntryPoint)
            .and()
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );
        
        return http.build();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
```

## API Usage Examples

### Login Request

```bash
curl -X POST https://api.example.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "customer@example.com",
    "password": "SecurePass123!"
  }'
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": "user-123",
    "email": "customer@example.com",
    "roles": ["CUSTOMER"]
  }
}
```

### Authenticated Request

```bash
curl -X GET https://api.example.com/api/v1/customers/user-123 \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

### Token Refresh Request

```bash
curl -X POST https://api.example.com/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
  }'
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

## Security Considerations

### Token Storage

**Client-Side Storage:**
- ✅ **Recommended**: Store in memory (JavaScript variable)
- ✅ **Acceptable**: HttpOnly, Secure cookies
- ❌ **Not Recommended**: localStorage (vulnerable to XSS)
- ❌ **Never**: sessionStorage without additional protection

### Token Transmission

- Always use HTTPS for token transmission
- Include tokens in Authorization header (not URL parameters)
- Use Bearer token scheme: `Authorization: Bearer <token>`

### Token Validation

- Validate token signature on every request
- Check token expiration
- Verify token issuer and audience
- Validate token has not been revoked (if revocation list maintained)

### Brute Force Protection

```java
@Component
public class LoginAttemptService {
    
    private final LoadingCache<String, Integer> attemptsCache;
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 15;
    
    public LoginAttemptService() {
        attemptsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(LOCKOUT_DURATION_MINUTES, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                @Override
                public Integer load(String key) {
                    return 0;
                }
            });
    }
    
    public void loginSucceeded(String email) {
        attemptsCache.invalidate(email);
    }
    
    public void loginFailed(String email) {
        int attempts = attemptsCache.getUnchecked(email);
        attemptsCache.put(email, attempts + 1);
    }
    
    public boolean isBlocked(String email) {
        return attemptsCache.getUnchecked(email) >= MAX_ATTEMPTS;
    }
}
```

## Monitoring and Logging

### Authentication Events to Log

- ✅ Successful login attempts
- ✅ Failed login attempts
- ✅ Account lockouts
- ✅ Token refresh requests
- ✅ Logout events
- ✅ Password changes
- ✅ Suspicious activity (multiple failures, unusual locations)

### Log Format

```java
logger.info("Authentication successful",
    kv("event", "AUTH_SUCCESS"),
    kv("userId", userId),
    kv("email", email),
    kv("ipAddress", ipAddress),
    kv("userAgent", userAgent),
    kv("timestamp", Instant.now()));

logger.warn("Authentication failed",
    kv("event", "AUTH_FAILURE"),
    kv("email", email),
    kv("reason", "INVALID_CREDENTIALS"),
    kv("ipAddress", ipAddress),
    kv("timestamp", Instant.now()));
```

## Testing

### Authentication Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void should_return_tokens_when_valid_credentials_provided() throws Exception {
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
    void should_return_401_when_invalid_credentials_provided() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "customer@example.com",
                        "password": "WrongPassword"
                    }
                    """))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    void should_reject_request_without_token() throws Exception {
        mockMvc.perform(get("/api/v1/customers/user-123"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    void should_accept_request_with_valid_token() throws Exception {
        String token = generateValidToken();
        
        mockMvc.perform(get("/api/v1/customers/user-123")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }
}
```

## Related Documentation

- [Authorization](authorization.md) - Role-based access control
- [Security Overview](overview.md) - Overall security perspective
- [Security Standards](../../.kiro/steering/security-standards.md) - Detailed security standards

## References

- JWT Specification: https://tools.ietf.org/html/rfc7519
- OWASP Authentication Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html
- Spring Security Documentation: https://spring.io/projects/spring-security
