# API Versioning Strategy

> **Status**: ✅ Active  
> **Last Updated**: 2024-11-19

## Overview

This document defines the API versioning strategy for the GenAI Demo e-commerce platform.

---

## Quick Reference

For complete API documentation, see:
- [API Overview](../README.md) - Complete API documentation
- [REST API Guidelines](README.md) - REST API standards

---

## Versioning Approach

### URL-Based Versioning

We use URL-based versioning for clarity and simplicity:

```
/api/v1/customers
/api/v2/customers
```

### Why URL Versioning?

✅ **Clear and Explicit**: Version is immediately visible in the URL
✅ **Easy to Route**: Simple routing configuration
✅ **Browser-Friendly**: Works with browser testing
✅ **Cache-Friendly**: Different versions have different URLs

---

## Version Lifecycle

### Version States

1. **Current** (v1): Actively maintained, recommended for new integrations
2. **Supported** (v1): Maintained for compatibility, no new features
3. **Deprecated** (v0): Scheduled for removal, migration required
4. **Retired**: No longer available

### Version Timeline

```
v1.0 Released ────────────────────────────────────────────────>
                    v2.0 Released ──────────────────────────>
                                        v1.0 Deprecated ────>
                                                    v1.0 Retired
                                                    
Timeline:        6 months          6 months        6 months
```

---

## Versioning Rules

### When to Create a New Version

Create a new major version when:
- ✅ Breaking changes to existing endpoints
- ✅ Removing fields from responses
- ✅ Changing field types
- ✅ Changing authentication mechanisms
- ✅ Significant behavioral changes

### When NOT to Create a New Version

Minor changes that don't require a new version:
- ✅ Adding new endpoints
- ✅ Adding optional fields to requests
- ✅ Adding new fields to responses
- ✅ Bug fixes
- ✅ Performance improvements

---

## Backward Compatibility

### Maintaining Compatibility

```java
// v1 Response
{
  "id": "CUST-001",
  "name": "John Doe",
  "email": "john@example.com"
}

// v2 Response (backward compatible - adds fields)
{
  "id": "CUST-001",
  "name": "John Doe",
  "email": "john@example.com",
  "phoneNumber": "+1234567890",  // New field
  "membershipLevel": "PREMIUM"    // New field
}
```

### Breaking Changes

```java
// v1 Response
{
  "customerId": "CUST-001",  // String ID
  "fullName": "John Doe"
}

// v2 Response (breaking - changes field names and types)
{
  "id": 12345,              // Changed to numeric ID
  "firstName": "John",      // Split name field
  "lastName": "Doe"
}
```

---

## Implementation

### Controller Versioning

```java
// Version 1
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerControllerV1 {
    
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponseV1> getCustomer(@PathVariable String id) {
        Customer customer = customerService.findById(id);
        return ResponseEntity.ok(CustomerResponseV1.from(customer));
    }
}

// Version 2
@RestController
@RequestMapping("/api/v2/customers")
public class CustomerControllerV2 {
    
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponseV2> getCustomer(@PathVariable String id) {
        Customer customer = customerService.findById(id);
        return ResponseEntity.ok(CustomerResponseV2.from(customer));
    }
}
```

### Response DTOs

```java
// V1 Response
public record CustomerResponseV1(
    String customerId,
    String fullName,
    String email
) {
    public static CustomerResponseV1 from(Customer customer) {
        return new CustomerResponseV1(
            customer.getId(),
            customer.getName(),
            customer.getEmail()
        );
    }
}

// V2 Response
public record CustomerResponseV2(
    Long id,
    String firstName,
    String lastName,
    String email,
    String phoneNumber,
    String membershipLevel
) {
    public static CustomerResponseV2 from(Customer customer) {
        return new CustomerResponseV2(
            customer.getNumericId(),
            customer.getFirstName(),
            customer.getLastName(),
            customer.getEmail(),
            customer.getPhoneNumber(),
            customer.getMembershipLevel().name()
        );
    }
}
```

---

## Deprecation Process

### Deprecation Headers

```http
HTTP/1.1 200 OK
Deprecation: true
Sunset: Wed, 31 Dec 2025 23:59:59 GMT
Link: </api/v2/customers>; rel="successor-version"
Content-Type: application/json

{
  "customerId": "CUST-001",
  "fullName": "John Doe"
}
```

### Implementation

```java
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerControllerV1 {
    
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponseV1> getCustomer(@PathVariable String id) {
        Customer customer = customerService.findById(id);
        
        return ResponseEntity.ok()
            .header("Deprecation", "true")
            .header("Sunset", "Wed, 31 Dec 2025 23:59:59 GMT")
            .header("Link", "</api/v2/customers>; rel=\"successor-version\"")
            .body(CustomerResponseV1.from(customer));
    }
}
```

---

## Migration Guide

### For API Consumers

#### Step 1: Review Changes

```bash
# Check API documentation
curl https://api.example.com/api/v2/docs

# Compare versions
diff v1-response.json v2-response.json
```

#### Step 2: Update Client Code

```java
// Old V1 client
CustomerResponseV1 customer = apiClient.getCustomer("CUST-001");
String name = customer.fullName();

// New V2 client
CustomerResponseV2 customer = apiClient.getCustomer("CUST-001");
String name = customer.firstName() + " " + customer.lastName();
```

#### Step 3: Test Migration

```java
@Test
void should_migrate_from_v1_to_v2() {
    // Test with V1
    CustomerResponseV1 v1Response = v1Client.getCustomer("CUST-001");
    
    // Test with V2
    CustomerResponseV2 v2Response = v2Client.getCustomer("CUST-001");
    
    // Verify data consistency
    assertThat(v2Response.firstName() + " " + v2Response.lastName())
        .isEqualTo(v1Response.fullName());
}
```

#### Step 4: Deploy and Monitor

```bash
# Deploy new version
./deploy.sh --version=v2

# Monitor error rates
./monitor.sh --api-version=v2 --metric=error-rate
```

---

## Version Documentation

### OpenAPI Specification

```yaml
openapi: 3.0.0
info:
  title: GenAI Demo API
  version: 2.0.0
  description: |
    ## Version History
    
    ### v2.0.0 (Current)
    - Split customer name into firstName and lastName
    - Changed customer ID to numeric format
    - Added phoneNumber and membershipLevel fields
    
    ### v1.0.0 (Deprecated - Sunset: 2025-12-31)
    - Original API version
    - String-based customer IDs
    - Combined fullName field

servers:
  - url: https://api.example.com/api/v2
    description: Version 2 (Current)
  - url: https://api.example.com/api/v1
    description: Version 1 (Deprecated)
```

---

## Monitoring and Metrics

### Version Usage Tracking

```java
@Component
public class ApiVersionMetrics {
    
    private final MeterRegistry meterRegistry;
    
    @EventListener
    public void trackApiVersion(ApiRequestEvent event) {
        Counter.builder("api.requests")
            .tag("version", event.getVersion())
            .tag("endpoint", event.getEndpoint())
            .register(meterRegistry)
            .increment();
    }
}
```

### Deprecation Warnings

```java
@Component
public class DeprecationWarningService {
    
    public void logDeprecationWarning(String version, String endpoint) {
        logger.warn("Deprecated API version used",
            kv("version", version),
            kv("endpoint", endpoint),
            kv("sunsetDate", getSunsetDate(version))
        );
    }
}
```

---

## Best Practices

### Do's ✅

1. **Maintain Multiple Versions**: Support at least 2 versions simultaneously
2. **Document Changes**: Clear migration guides for each version
3. **Communicate Early**: Announce deprecations well in advance
4. **Monitor Usage**: Track version adoption rates
5. **Test Thoroughly**: Ensure backward compatibility

### Don'ts ❌

1. **Don't Break Suddenly**: Always provide migration period
2. **Don't Version Everything**: Minor changes don't need new versions
3. **Don't Forget Headers**: Always include deprecation headers
4. **Don't Remove Immediately**: Give consumers time to migrate
5. **Don't Ignore Feedback**: Listen to API consumer concerns

---


**Document Version**: 1.0  
**Last Updated**: 2024-11-19  
**Owner**: API Team
