# Design Document

## Overview

This design addresses the profile validation issue in the ProfileConfiguration class that prevents tests from running with the "test" profile. The solution involves updating the validation logic to support test profiles while maintaining security and clarity for production environments.

## Architecture

### Current Architecture Issue

- ProfileConfiguration.validateProfilesOnStartup() only accepts ["dev", "production", "openapi"] profiles
- Test framework automatically activates "test" profile, causing IllegalStateException
- This breaks all Spring Boot tests that rely on application context loading

### Proposed Architecture

- Extend VALID_PROFILES to include "test" profile
- Add test profile detection methods for conditional bean configuration
- Maintain strict validation for production environments
- Ensure test profile configurations don't leak into production

## Components and Interfaces

### 1. ProfileConfiguration Class Updates

#### Updated Constants

```java
private static final Set<String> VALID_PROFILES = Set.of("dev", "production", "test");
private static final Set<String> UTILITY_PROFILES = Set.of("openapi");
```

#### Enhanced Validation Logic

```java
public void validateProfilesOnStartup() {
    // Validate each profile against VALID_PROFILES and UTILITY_PROFILES
    // Provide clear error messages for invalid profiles
    // Log profile validation results
}
```

#### New Profile Detection Methods

```java
public boolean isTestProfile() {
    return Arrays.asList(environment.getActiveProfiles()).contains("test");
}
```

### 2. Test Configuration Support

#### Test Profile Properties

- Ensure test-specific application.yml configurations are properly loaded
- Validate that test profile doesn't conflict with production settings
- Support profile-specific bean configurations

#### Profile Combination Rules

- "test" + "openapi" = Valid (for API documentation tests)
- "test" + "dev" = Invalid (conflicting environments)
- "test" + "production" = Invalid (security risk)

## Data Models

### Profile Validation Model

```java
public class ProfileValidationResult {
    private final boolean valid;
    private final List<String> validProfiles;
    private final List<String> invalidProfiles;
    private final String errorMessage;
}
```

### Profile Configuration Properties

```yaml
# Test profile specific configurations
spring:
  profiles:
    active: test
  datasource:
    url: jdbc:h2:mem:testdb
  jpa:
    hibernate:
      ddl-auto: create-drop
```

## Error Handling

### Profile Validation Errors

1. **Invalid Profile Error**: Clear message listing valid profiles
2. **Profile Conflict Error**: Detect conflicting profile combinations
3. **Missing Configuration Error**: Validate required properties for each profile

### Error Recovery

- Graceful degradation for non-critical profile issues
- Clear logging for debugging profile-related problems
- Fail-fast for security-critical profile violations

## Testing Strategy

### Unit Tests

- Test profile validation logic with various profile combinations
- Verify error messages for invalid profiles
- Test profile detection methods

### Integration Tests

- Verify application context loads with test profile
- Test profile-specific bean configurations
- Validate test profile isolation from production settings

### Test Cases

1. Valid single profiles: "dev", "production", "test"
2. Valid profile combinations: "test" + "openapi"
3. Invalid profiles: "staging", "local", "custom"
4. Invalid combinations: "test" + "production"
5. Default profile behavior when no profiles specified

## Security Considerations

### Profile Isolation

- Ensure test profile cannot be activated in production
- Validate that test configurations don't expose sensitive data
- Prevent test profile from overriding production security settings

### Configuration Validation

- Verify test profile uses secure defaults
- Ensure test databases are properly isolated
- Validate that test profile doesn't enable debug features in production

## Implementation Plan

### Phase 1: Core Profile Support

1. Update VALID_PROFILES constant to include "test"
2. Enhance validation logic to handle utility profiles separately
3. Add isTestProfile() detection method

### Phase 2: Test Configuration

1. Verify test profile application.yml configurations
2. Add profile combination validation
3. Update error messages for clarity

### Phase 3: Testing and Validation

1. Run all existing tests to ensure they pass
2. Add new tests for profile validation logic
3. Verify production profile security is maintained

## Backward Compatibility

### Existing Functionality

- All existing profile validation behavior remains unchanged
- Production and development profiles continue to work as before
- OpenAPI profile integration remains intact

### Migration Path

- No breaking changes for existing deployments
- Test profile support is additive only
- Existing test configurations will work without modification
