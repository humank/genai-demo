# Spring Boot Profile Configuration Foundation

This document describes the Spring Boot profile configuration system implemented for the GenAI Demo application with observability integration.

## Overview

The profile configuration system provides:

- Environment-specific configurations (dev, production, test)
- Profile validation and error handling
- Clear configuration precedence rules
- Comprehensive error messages and troubleshooting guidance

## Configuration Files

### Base Configuration

- `application.yml` - Base configuration with defaults
- Contains common settings shared across all profiles

### Profile-Specific Configurations

- `application-dev.yml` - Development environment (H2 database, debug logging)
- `application-production.yml` - Production environment (PostgreSQL, Kafka, optimized settings)
- `application-test.yml` - Test environment (H2 in-memory, minimal logging)

## Profile Activation

### Environment Variable (Recommended)

```bash
# Development
export SPRING_PROFILES_ACTIVE=dev

# Production
export SPRING_PROFILES_ACTIVE=prod

# Testing
export SPRING_PROFILES_ACTIVE=test
```

### Command Line

```bash
# Development
./gradlew bootRun --args='--spring.profiles.active=dev'

# Production
java -jar app.jar --spring.profiles.active=production
```

### Docker

```bash
# Development
docker run -e SPRING_PROFILES_ACTIVE=dev genai-demo:latest

# Production
docker run -e SPRING_PROFILES_ACTIVE=prod genai-demo:latest
```

## Configuration Precedence Rules

The configuration follows Spring Boot's standard precedence rules:

1. **Environment Variables** (highest priority)
2. **Profile-specific configuration files** (`application-{profile}.yml`)
3. **Base configuration file** (`application.yml`)
4. **Default values** (lowest priority)

## Profile Features

### Development Profile (`dev`)

- **Database**: H2 in-memory database
- **H2 Console**: Enabled at `/h2-console`
- **Logging**: Debug level for application packages
- **Events**: In-memory event processing
- **Flyway**: H2-specific migration scripts
- **Error Handling**: Detailed stack traces

### Production Profile (`production`)

- **Database**: PostgreSQL with connection pooling
- **Logging**: INFO level with structured JSON format
- **Events**: Kafka-based event processing
- **Flyway**: PostgreSQL-specific migration scripts
- **Security**: SSL required, minimal error details
- **Monitoring**: Full actuator endpoints with authentication

### Test Profile (`test`)

- **Database**: H2 in-memory (clean slate for each test)
- **Logging**: Minimal logging to reduce test noise
- **Events**: In-memory event processing
- **Flyway**: H2-specific migrations with flexible validation
- **Server**: Random port assignment

## Environment Variables

### Required for Production

```bash
# Database Configuration
DB_HOST=your-database-host
DB_NAME=your-database-name
DB_USERNAME=your-database-username
DB_PASSWORD=your-database-password

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=your-kafka-servers

# Profile Activation
SPRING_PROFILES_ACTIVE=prod
```

### Optional Configuration

```bash
# Strict validation (default: enabled in production)
APP_PROFILE_VALIDATION_STRICT=true

# Custom database port
DB_PORT=5432
```

## Profile Validation

The system includes comprehensive profile validation:

### Valid Profiles

- `dev`, `development` - Development environment
- `production`, `prod` - Production environment
- `test` - Testing environment
- `openapi` - Utility profile for API documentation

### Invalid Profile Combinations

- ❌ `test` + `production` (Security violation)
- ❌ `test` + `development` (Configuration conflict)
- ❌ `production` + `development` (Environment conflict)
- ✅ `test` + `openapi` (Valid for API testing)
- ✅ `dev` + `openapi` (Valid for development)

### Validation Modes

- **Strict Mode**: Enabled in production, throws exceptions for invalid configurations
- **Flexible Mode**: Enabled in development/test, logs warnings but continues

## Error Handling

### Common Errors and Solutions

#### 1. Invalid Profile Error

```
Error: Invalid profile 'invalid-profile' detected
Solution: Use one of the valid profiles: dev, production, test
```

#### 2. Profile Combination Error

```
Error: SECURITY VIOLATION: 'test' and 'production' profiles must not be active together
Solution: Use either 'test' OR 'production', never both
```

#### 3. Missing Database Configuration

```
Error: Production profile requires PostgreSQL datasource URL
Solution: Set DB_HOST, DB_NAME, DB_USERNAME, DB_PASSWORD environment variables
```

#### 4. Missing Kafka Configuration

```
Warning: Kafka bootstrap servers not configured - event publishing may fail
Solution: Set KAFKA_BOOTSTRAP_SERVERS environment variable
```

## Configuration Properties

### Profile Configuration Properties

```yaml
genai-demo:
  profile:
    name: development
    description: "Local development environment with H2 database"
    features:
      h2-console: true
      debug-logging: true
      in-memory-events: true
      kafka-events: false
```

### Feature Flags

- `h2-console`: Enable/disable H2 database console
- `debug-logging`: Enable/disable debug level logging
- `in-memory-events`: Use in-memory event processing
- `kafka-events`: Use Kafka-based event processing

## Testing

### Unit Tests

```bash
# Run profile configuration tests
./gradlew test --tests="*ProfileConfiguration*"
```

### Integration Tests

```bash
# Run with specific profile
./gradlew test -Dspring.profiles.active=test
```

### Profile-Specific Testing

```java
@SpringBootTest
@ActiveProfiles("dev")
class DevelopmentProfileTest {
    // Test development-specific configuration
}

@SpringBootTest
@ActiveProfiles("production")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/testdb"
})
class ProductionProfileTest {
    // Test production-specific configuration
}
```

## Troubleshooting

### Debug Configuration Loading

1. Enable debug logging for configuration:

   ```yaml
   logging:
     level:
       org.springframework.boot.context.config: DEBUG
   ```

2. Check active profiles:

   ```bash
   curl http://localhost:8080/actuator/info
   ```

3. Verify configuration properties:

   ```bash
   curl http://localhost:8080/actuator/configprops
   ```

### Common Issues

#### Profile Not Activated

- Check `SPRING_PROFILES_ACTIVE` environment variable
- Verify profile name spelling
- Check for typos in configuration files

#### Configuration Not Loading

- Verify file names match pattern: `application-{profile}.yml`
- Check YAML syntax and indentation
- Ensure files are in `src/main/resources/`

#### Database Connection Issues

- Verify database is running and accessible
- Check connection parameters (host, port, database name)
- Verify credentials and permissions

## Best Practices

1. **Use Environment Variables**: Set `SPRING_PROFILES_ACTIVE` via environment variables
2. **Validate Early**: Profile validation runs at startup to catch issues early
3. **Clear Error Messages**: Comprehensive error handling provides actionable guidance
4. **Test All Profiles**: Write tests for each profile configuration
5. **Document Changes**: Update this documentation when adding new profiles or features
6. **Security First**: Never combine test and production profiles
7. **Fail Fast**: Use strict validation in production environments

## Integration with Observability

This profile configuration system integrates with the observability features:

- **Logging**: Profile-specific log levels and formats
- **Metrics**: Profile information exposed via actuator endpoints
- **Tracing**: Profile-aware trace configuration
- **Health Checks**: Profile-specific health indicators

## Next Steps

After completing the profile configuration foundation:

1. Implement multi-environment database configuration (Task 2)
2. Add domain events publishing strategy (Task 3)
3. Set up development observability stack (Task 4)
4. Create AWS CDK infrastructure foundation (Task 5)
