# Spring Boot 3.4.5 配置指南

## 概述

本指南詳細說明 Spring Boot 3.4.5 在專案中的配置和最佳實踐，包括核心配置、安全配置、資料庫配置和監控配置。

## 🚀 核心配置

### 應用程式配置

```yaml
# application.yml
spring:
  application:
    name: genai-demo
    
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:development}
    
  main:
    lazy-initialization: false
    allow-bean-definition-overriding: false
    
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

### 環境特定配置

```yaml
# application-development.yml
spring:
  devtools:
    restart:
      enabled: true
    livereload:
      enabled: true
      
  h2:
    console:
      enabled: true
      path: /h2-console
      
logging:
  level:
    solid.humank.genaidemo: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
```

```yaml
# application-production.yml
spring:
  devtools:
    restart:
      enabled: false
      
logging:
  level:
    solid.humank.genaidemo: INFO
    org.springframework.web: WARN
    org.hibernate.SQL: WARN
    
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

## 🔒 安全配置

### Spring Security 配置

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/api/auth/**")
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/auth/**", "/actuator/health").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
                .accessDeniedHandler(new JwtAccessDeniedHandler())
            );
            
        return http.build();
    }
    
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        authoritiesConverter.setAuthoritiesClaimName("roles");
        
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }
}
```

### JWT 配置

```java
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
@Data
public class JwtProperties {
    private String secret;
    private long expiration = 86400; // 24 hours
    private long refreshExpiration = 604800; // 7 days
    private String issuer = "genai-demo";
}

@Component
public class JwtTokenProvider {
    
    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;
    
    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }
    
    public String generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Date expiryDate = new Date(System.currentTimeMillis() + jwtProperties.getExpiration() * 1000);
        
        return Jwts.builder()
            .setSubject(userPrincipal.getId())
            .setIssuedAt(new Date())
            .setExpiration(expiryDate)
            .setIssuer(jwtProperties.getIssuer())
            .claim("roles", userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()))
            .signWith(secretKey, SignatureAlgorithm.HS512)
            .compact();
    }
}
```

## 🗄️ 資料庫配置

### JPA 和 Hibernate 配置

```yaml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/genaidemo}
    username: ${DATABASE_USERNAME:genaidemo}
    password: ${DATABASE_PASSWORD:password}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      max-lifetime: 1200000
      connection-timeout: 20000
      leak-detection-threshold: 60000
      
  jpa:
    hibernate:
      ddl-auto: validate
      naming:
        physical-strategy: org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        use_sql_comments: true
        jdbc:
          batch_size: 25
          fetch_size: 50
        order_inserts: true
        order_updates: true
        connection:
          provider_disables_autocommit: true
        cache:
          use_second_level_cache: true
          use_query_cache: true
          region:
            factory_class: org.hibernate.cache.jcache.JCacheRegionFactory
```

### Flyway 配置

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true
    clean-disabled: true
    schemas: public
```

```sql
-- V1__Create_customers_table.sql
CREATE TABLE customers (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_customers_status ON customers(status);
CREATE INDEX idx_customers_created_at ON customers(created_at);
```

## 📊 監控和可觀測性

### Actuator 配置

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,loggers
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized
      show-components: always
    metrics:
      enabled: true
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5, 0.95, 0.99
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active}
```

### 自定義健康檢查

```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    private final DataSource dataSource;
    
    public DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(1)) {
                return Health.up()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("validationQuery", "SELECT 1")
                    .build();
            } else {
                return Health.down()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("error", "Connection validation failed")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("database", "PostgreSQL")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}

@Component
public class CustomHealthIndicator implements HealthIndicator {
    
    private final ExternalService externalService;
    
    @Override
    public Health health() {
        try {
            boolean isHealthy = externalService.isHealthy();
            if (isHealthy) {
                return Health.up()
                    .withDetail("external-service", "Available")
                    .build();
            } else {
                return Health.down()
                    .withDetail("external-service", "Unavailable")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("external-service", "Error")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

## 🔧 自定義配置

### 應用程式屬性

```java
@ConfigurationProperties(prefix = "app")
@Data
@Component
public class AppProperties {
    
    private Cors cors = new Cors();
    private Security security = new Security();
    private Storage storage = new Storage();
    
    @Data
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();
        private List<String> allowedMethods = Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS");
        private List<String> allowedHeaders = Arrays.asList("*");
        private boolean allowCredentials = true;
        private long maxAge = 3600;
    }
    
    @Data
    public static class Security {
        private String jwtSecret;
        private long jwtExpiration = 86400;
        private boolean enableCsrf = true;
    }
    
    @Data
    public static class Storage {
        private String uploadPath = "./uploads";
        private long maxFileSize = 10485760; // 10MB
        private List<String> allowedFileTypes = Arrays.asList("jpg", "jpeg", "png", "pdf");
    }
}
```

### 配置驗證

```java
@Configuration
@EnableConfigurationProperties(AppProperties.class)
@Validated
public class AppConfiguration {
    
    @Bean
    @ConditionalOnProperty(name = "app.cors.enabled", havingValue = "true", matchIfMissing = true)
    public CorsConfigurationSource corsConfigurationSource(AppProperties appProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(appProperties.getCors().getAllowedOrigins());
        configuration.setAllowedMethods(appProperties.getCors().getAllowedMethods());
        configuration.setAllowedHeaders(appProperties.getCors().getAllowedHeaders());
        configuration.setAllowCredentials(appProperties.getCors().isAllowCredentials());
        configuration.setMaxAge(appProperties.getCors().getMaxAge());
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
    
    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();
    }
}
```

## 🚀 性能優化

### 連接池優化

```yaml
spring:
  datasource:
    hikari:
      # 連接池大小配置
      maximum-pool-size: 20
      minimum-idle: 5
      
      # 連接超時配置
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 1200000
      
      # 連接洩漏檢測
      leak-detection-threshold: 60000
      
      # 連接測試
      connection-test-query: SELECT 1
      validation-timeout: 5000
      
      # 性能優化
      auto-commit: false
      read-only: false
      isolation-level: TRANSACTION_READ_COMMITTED
```

### JVM 優化

```bash
# JVM 參數優化
JAVA_OPTS="-Xms2g -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UseStringDeduplication \
  -XX:+OptimizeStringConcat \
  -XX:+UseCompressedOops \
  -XX:+UseCompressedClassPointers \
  -Djava.security.egd=file:/dev/./urandom"
```

## 🧪 測試配置

### 測試專用配置

```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password: 
    
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    
  flyway:
    enabled: false
    
logging:
  level:
    solid.humank.genaidemo: DEBUG
    org.springframework.test: DEBUG
```

### 測試配置類

```java
@TestConfiguration
public class TestConfig {
    
    @Bean
    @Primary
    public Clock testClock() {
        return Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
    }
    
    @Bean
    @Primary
    public EmailService mockEmailService() {
        return Mockito.mock(EmailService.class);
    }
    
    @Bean
    @Primary
    public ExternalApiService mockExternalApiService() {
        return Mockito.mock(ExternalApiService.class);
    }
}
```

## 🔗 相關資源

### 內部文檔
- [技術棧整合指南](README.md)
- [資料庫設計文檔](../../data-management/database-design.md)
- [安全實作指南](../../security/security-implementation.md)

### 外部資源
- [Spring Boot Reference Documentation](https://docs.spring.io/spring-boot/docs/3.4.5/reference/html/)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [Hibernate User Guide](https://docs.jboss.org/hibernate/orm/6.4/userguide/html_single/Hibernate_User_Guide.html)

---

**最後更新**: 2025年1月21日  
**維護者**: Development Team  
**版本**: 1.0