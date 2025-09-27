# API Versioning Strategy

## Overview

This document defines the API versioning strategy for the GenAI Demo project, ensuring backward compatibility and smooth API upgrades.

## Version Control Scheme

### 1. Version Number Format

Adopts Semantic Versioning: `MAJOR.MINOR.PATCH`

- **MAJOR**: Incompatible API changes
- **MINOR**: Backward-compatible functionality additions
- **PATCH**: Backward-compatible bug fixes

### 2. Version Control Methods

#### URL Path Versioning (Primary Method)

```
/api/v1/products
/api/v2/products
```

#### HTTP Header Versioning (Alternative Method)

```http
Accept: application/vnd.genaidemo.v1+json
API-Version: v1
```

## API Grouping and Versioning

### Consumer API

- **Base Path**: `/api/consumer/v1/`
- **Target Users**: End consumers
- **Version Strategy**: Strict backward compatibility, long-term support

```
/api/consumer/v1/products          # Product browsing
/api/consumer/v1/shopping-cart     # Shopping cart
/api/consumer/v1/promotions        # Promotional activities
/api/consumer/v1/member            # Member functions
/api/consumer/v1/reviews           # Product reviews
/api/consumer/v1/recommendations   # Recommendation system
/api/consumer/v1/notifications     # Notification system
/api/consumer/v1/delivery-tracking # Delivery tracking
```

### Business API

- **Base Path**: `/api/business/v1/`
- **Target Users**: Business management personnel
- **Version Strategy**: Rapid iteration, regular upgrades

```
/api/business/v1/orders      # Order management
/api/business/v1/products    # Product management
/api/business/v1/customers   # Customer management
/api/business/v1/inventory   # Inventory management
/api/business/v1/pricing     # Pricing management
/api/business/v1/stats       # Statistical analysis
```

### Internal API

- **Base Path**: `/api/internal/v1/`
- **Target Users**: Internal system integration
- **Version Strategy**: Flexible changes, internal coordination

## Version Lifecycle

### 1. Version Support Period

| Version Type | Support Period | Deprecation Notice Period |
|-------------|----------------|---------------------------|
| Consumer API | 24 months | 6 months |
| Business API | 12 months | 3 months |
| Internal API | 6 months | 1 month |

### 2. Version Status

- **CURRENT**: Current version, actively developed and maintained
- **SUPPORTED**: Supported version, only critical issues fixed
- **DEPRECATED**: Deprecated version, planned for removal
- **RETIRED**: Retired version, no longer supported

## Change Management

### 1. Backward Compatible Changes (MINOR/PATCH)

✅ **Allowed Changes**:

- Add new API endpoints
- Add optional parameters
- Add response fields
- Bug fixes
- Performance improvements

### 2. Incompatible Changes (MAJOR)

❌ **Changes Requiring New Version**:

- Remove API endpoints
- Remove request/response fields
- Change field types
- Change error codes
- Change authentication methods

### 3. Change Notification Process

1. **Proposal Stage**: Submit change proposal in GitHub Issues
2. **Assessment Stage**: Technical team assesses impact scope
3. **Notification Stage**: Notify API users in advance
4. **Implementation Stage**: Release new version
5. **Monitoring Stage**: Monitor usage and issues

## Implementation Details

### 1. Spring Boot Configuration

```java
@RestController
@RequestMapping("/api/consumer/v1")
@Tag(name = "Consumer API v1", description = "Consumer API Version 1")
public class ConsumerProductController {
    // Implementation content
}
```

### 2. OpenAPI Documentation Configuration

```yaml
openapi: 3.0.3
info:
  title: GenAI Demo API
  version: 2.0.0
  description: |
    GenAI Demo E-commerce Platform API
    
    ## Version Information
    - Consumer API: v1 (Stable)
    - Business API: v1 (Stable)
    - Internal API: v1 (Development)
```

### 3. Version Detection Middleware

```java
@Component
public class ApiVersionInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, 
                           HttpServletResponse response, 
                           Object handler) {
        // Version detection and routing logic
        return true;
    }
}
```

## Monitoring and Metrics

### 1. Version Usage Statistics

- API call counts per version
- Version distribution statistics
- Deprecated API usage

### 2. Performance Monitoring

- Response times per version
- Error rate statistics
- User satisfaction

### 3. Alert Configuration

- Abnormal deprecated API usage
- High error rates in new versions
- Slow version migration progress

## Best Practices

### 1. API Design Principles

- **Consistency**: Maintain consistent API design style
- **Predictability**: API behavior should be predictable
- **Complete Documentation**: Provide comprehensive API documentation
- **Test Coverage**: Ensure adequate test coverage

### 2. Version Migration Guide

- Provide detailed migration documentation
- Provide code examples
- Provide migration tools
- Provide technical support

### 3. Communication Strategy

- Regularly publish version update notifications
- Maintain change logs
- Provide developer forums
- Host technical sharing sessions

## Related Documentation

- [API Documentation](./README.md)
- [Change Log](../releases/)
- [Frontend Integration Guide](./frontend-integration.md)
- [Observability API Guide](./observability-api.md)
