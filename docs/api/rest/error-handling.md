# Error Handling

## Overview

The Enterprise E-Commerce Platform API uses standard HTTP status codes and provides detailed error information in a consistent format. This document describes error response formats, error codes, and troubleshooting guidance.

## Error Response Format

### Standard Error Response

All error responses follow this consistent structure:

```json
{
  "errors": [
    {
      "code": "ERROR_CODE",
      "message": "Human-readable error message",
      "field": "fieldName",
      "rejectedValue": "invalid-value"
    }
  ],
  "metadata": {
    "requestId": "req-abc-123",
    "timestamp": "2025-10-25T10:30:00Z",
    "version": "v1"
  }
}
```

**Fields**:

- `errors`: Array of error objects
- `code`: Machine-readable error code
- `message`: Human-readable error description
- `field`: Field name (for validation errors)
- `rejectedValue`: The invalid value that was rejected
- `metadata.requestId`: Unique request identifier for tracking
- `metadata.timestamp`: Error occurrence timestamp
- `metadata.version`: API version

### Multiple Errors

When multiple validation errors occur, all errors are returned:

```json
{
  "errors": [
    {
      "code": "VALIDATION_ERROR",
      "message": "Email format is invalid",
      "field": "email",
      "rejectedValue": "invalid-email"
    },
    {
      "code": "VALIDATION_ERROR",
      "message": "Password must be at least 8 characters",
      "field": "password",
      "rejectedValue": "short"
    }
  ],
  "metadata": {
    "requestId": "req-abc-123",
    "timestamp": "2025-10-25T10:30:00Z",
    "version": "v1"
  }
}
```

## HTTP Status Codes

### Success Codes (2xx)

| Code | Status | Description | Usage |
|------|--------|-------------|-------|
| 200 | OK | Request successful | GET, PUT, PATCH |
| 201 | Created | Resource created | POST |
| 204 | No Content | Success with no response body | DELETE |

### Client Error Codes (4xx)

| Code | Status | Description | Usage |
|------|--------|-------------|-------|
| 400 | Bad Request | Invalid request format or validation error | Malformed JSON, validation failures |
| 401 | Unauthorized | Authentication required or failed | Missing/invalid token |
| 403 | Forbidden | Insufficient permissions | Authorization failure |
| 404 | Not Found | Resource not found | Invalid resource ID |
| 405 | Method Not Allowed | HTTP method not supported | Wrong HTTP method |
| 409 | Conflict | Business rule violation | Duplicate resource, state conflict |
| 422 | Unprocessable Entity | Semantic validation error | Business logic validation |
| 429 | Too Many Requests | Rate limit exceeded | Too many requests |

### Server Error Codes (5xx)

| Code | Status | Description | Usage |
|------|--------|-------------|-------|
| 500 | Internal Server Error | Unexpected server error | System failures |
| 502 | Bad Gateway | Upstream service error | External service failure |
| 503 | Service Unavailable | Service temporarily unavailable | Maintenance, overload |
| 504 | Gateway Timeout | Upstream service timeout | External service timeout |

## Error Codes

### Authentication Errors (AUTH_*)

| Code | HTTP Status | Description | Solution |
|------|-------------|-------------|----------|
| `AUTH_INVALID_CREDENTIALS` | 401 | Invalid email or password | Check credentials |
| `AUTH_TOKEN_EXPIRED` | 401 | Access token expired | Refresh token |
| `AUTH_TOKEN_INVALID` | 401 | Malformed or invalid token | Re-authenticate |
| `AUTH_REFRESH_TOKEN_EXPIRED` | 401 | Refresh token expired | Re-authenticate |
| `AUTH_REFRESH_TOKEN_INVALID` | 401 | Invalid refresh token | Re-authenticate |
| `AUTH_ACCOUNT_LOCKED` | 401 | Account locked due to failed attempts | Wait or contact support |
| `AUTH_ACCOUNT_DISABLED` | 401 | Account has been disabled | Contact support |
| `AUTH_EMAIL_NOT_VERIFIED` | 401 | Email not verified | Verify email |

**Example**:

```json
{
  "errors": [
    {
      "code": "AUTH_INVALID_CREDENTIALS",
      "message": "Invalid email or password"
    }
  ]
}
```

### Authorization Errors (AUTHZ_*)

| Code | HTTP Status | Description | Solution |
|------|-------------|-------------|----------|
| `AUTHZ_INSUFFICIENT_PERMISSIONS` | 403 | Missing required permissions | Check required roles |
| `AUTHZ_RESOURCE_ACCESS_DENIED` | 403 | Cannot access this resource | Check resource ownership |
| `AUTHZ_ROLE_REQUIRED` | 403 | Specific role required | Contact administrator |

**Example**:

```json
{
  "errors": [
    {
      "code": "AUTHZ_INSUFFICIENT_PERMISSIONS",
      "message": "Insufficient permissions. Required role: ADMIN"
    }
  ]
}
```

### Validation Errors (VALIDATION_*)

| Code | HTTP Status | Description | Solution |
|------|-------------|-------------|----------|
| `VALIDATION_ERROR` | 400 | Field validation failed | Check field requirements |
| `VALIDATION_REQUIRED_FIELD` | 400 | Required field missing | Provide required field |
| `VALIDATION_INVALID_FORMAT` | 400 | Invalid field format | Check format requirements |
| `VALIDATION_OUT_OF_RANGE` | 400 | Value out of acceptable range | Check min/max values |
| `VALIDATION_INVALID_LENGTH` | 400 | String length invalid | Check length requirements |
| `VALIDATION_INVALID_EMAIL` | 400 | Invalid email format | Provide valid email |
| `VALIDATION_INVALID_PHONE` | 400 | Invalid phone format | Provide valid phone |
| `VALIDATION_INVALID_DATE` | 400 | Invalid date format | Use ISO 8601 format |

**Example**:

```json
{
  "errors": [
    {
      "code": "VALIDATION_INVALID_EMAIL",
      "message": "Email format is invalid",
      "field": "email",
      "rejectedValue": "invalid-email"
    }
  ]
}
```

### Business Rule Errors (BUSINESS_*)

| Code | HTTP Status | Description | Solution |
|------|-------------|-------------|----------|
| `BUSINESS_RULE_VIOLATION` | 409 | Business rule violated | Check business constraints |
| `BUSINESS_DUPLICATE_RESOURCE` | 409 | Resource already exists | Use existing resource |
| `BUSINESS_INVALID_STATE` | 409 | Invalid state transition | Check current state |
| `BUSINESS_INSUFFICIENT_INVENTORY` | 409 | Not enough inventory | Reduce quantity |
| `BUSINESS_PAYMENT_FAILED` | 422 | Payment processing failed | Check payment details |
| `BUSINESS_ORDER_CANNOT_BE_CANCELLED` | 409 | Order cannot be cancelled | Check order status |

**Example**:

```json
{
  "errors": [
    {
      "code": "BUSINESS_INSUFFICIENT_INVENTORY",
      "message": "Insufficient inventory for product. Available: 5, Requested: 10",
      "field": "quantity",
      "rejectedValue": 10
    }
  ]
}
```

### Resource Errors (RESOURCE_*)

| Code | HTTP Status | Description | Solution |
|------|-------------|-------------|----------|
| `RESOURCE_NOT_FOUND` | 404 | Resource not found | Check resource ID |
| `RESOURCE_ALREADY_EXISTS` | 409 | Resource already exists | Use different identifier |
| `RESOURCE_DELETED` | 410 | Resource has been deleted | Cannot be recovered |

**Example**:

```json
{
  "errors": [
    {
      "code": "RESOURCE_NOT_FOUND",
      "message": "Customer with ID 'cust-999' not found"
    }
  ]
}
```

### Rate Limiting Errors (RATE_LIMIT_*)

| Code | HTTP Status | Description | Solution |
|------|-------------|-------------|----------|
| `RATE_LIMIT_EXCEEDED` | 429 | Too many requests | Wait and retry |
| `RATE_LIMIT_QUOTA_EXCEEDED` | 429 | Daily quota exceeded | Wait until reset |

**Example**:

```json
{
  "errors": [
    {
      "code": "RATE_LIMIT_EXCEEDED",
      "message": "Rate limit exceeded. Please try again in 3600 seconds."
    }
  ]
}
```

**Response Headers**:

```http
HTTP/1.1 429 Too Many Requests
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1635793200
Retry-After: 3600
```

### System Errors (SYSTEM_*)

| Code | HTTP Status | Description | Solution |
|------|-------------|-------------|----------|
| `SYSTEM_INTERNAL_ERROR` | 500 | Unexpected system error | Contact support |
| `SYSTEM_SERVICE_UNAVAILABLE` | 503 | Service temporarily unavailable | Retry later |
| `SYSTEM_DATABASE_ERROR` | 500 | Database operation failed | Contact support |
| `SYSTEM_EXTERNAL_SERVICE_ERROR` | 502 | External service error | Retry or contact support |
| `SYSTEM_TIMEOUT` | 504 | Request timeout | Retry with smaller payload |

**Example**:

```json
{
  "errors": [
    {
      "code": "SYSTEM_INTERNAL_ERROR",
      "message": "An unexpected error occurred. Please contact support with request ID: req-abc-123"
    }
  ],
  "metadata": {
    "requestId": "req-abc-123",
    "timestamp": "2025-10-25T10:30:00Z"
  }
}
```

## Error Handling Best Practices

### Client-Side Error Handling

**1. Check HTTP Status Code**:

```javascript
async function makeRequest() {
  const response = await fetch('/api/v1/customers');
  
  if (!response.ok) {
    const error = await response.json();
    handleError(response.status, error);
    return;
  }
  
  return response.json();
}

function handleError(status, error) {
  switch (status) {
    case 400:
      // Validation error - show field errors
      showValidationErrors(error.errors);
      break;
    case 401:
      // Authentication error - redirect to login
      redirectToLogin();
      break;
    case 403:
      // Authorization error - show access denied
      showAccessDenied();
      break;
    case 404:
      // Not found - show not found page
      showNotFound();
      break;
    case 429:
      // Rate limit - show retry message
      showRateLimitError(error);
      break;
    case 500:
      // Server error - show error message with request ID
      showServerError(error.metadata.requestId);
      break;
    default:
      showGenericError();
  }
}
```

**2. Display User-Friendly Messages**:

```javascript
function showValidationErrors(errors) {
  errors.forEach(error => {
    const field = document.querySelector(`[name="${error.field}"]`);
    if (field) {
      field.classList.add('error');
      const errorMessage = document.createElement('span');
      errorMessage.className = 'error-message';
      errorMessage.textContent = error.message;
      field.parentNode.appendChild(errorMessage);
    }
  });
}
```

**3. Implement Retry Logic**:

```javascript
async function fetchWithRetry(url, options, maxRetries = 3) {
  for (let i = 0; i < maxRetries; i++) {
    try {
      const response = await fetch(url, options);
      
      if (response.status === 429) {
        // Rate limited - wait and retry
        const retryAfter = response.headers.get('Retry-After') || 60;
        await sleep(retryAfter * 1000);
        continue;
      }
      
      if (response.status >= 500) {
        // Server error - exponential backoff
        await sleep(Math.pow(2, i) * 1000);
        continue;
      }
      
      return response;
    } catch (error) {
      if (i === maxRetries - 1) throw error;
      await sleep(Math.pow(2, i) * 1000);
    }
  }
}
```

**4. Log Errors with Request ID**:

```javascript
function logError(error, requestId) {
  console.error('API Error:', {
    requestId,
    code: error.code,
    message: error.message,
    timestamp: new Date().toISOString()
  });
  
  // Send to error tracking service
  errorTracker.captureException(error, {
    extra: { requestId }
  });
}
```

### Server-Side Error Handling

**Java Example**:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
        ValidationException ex) {
        
        List<ApiError> errors = ex.getErrors().stream()
            .map(error -> new ApiError(
                "VALIDATION_ERROR",
                error.getMessage(),
                error.getField(),
                error.getRejectedValue()
            ))
            .toList();
        
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.error(errors));
    }
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(
        ResourceNotFoundException ex) {
        
        ApiError error = new ApiError(
            "RESOURCE_NOT_FOUND",
            ex.getMessage(),
            null,
            null
        );
        
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(List.of(error)));
    }
    
    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessRule(
        BusinessRuleViolationException ex) {
        
        ApiError error = new ApiError(
            "BUSINESS_RULE_VIOLATION",
            ex.getMessage(),
            null,
            null
        );
        
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(List.of(error)));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(
        Exception ex, HttpServletRequest request) {
        
        String requestId = request.getHeader("X-Request-ID");
        
        logger.error("Unexpected error for request {}", requestId, ex);
        
        ApiError error = new ApiError(
            "SYSTEM_INTERNAL_ERROR",
            "An unexpected error occurred. Request ID: " + requestId,
            null,
            null
        );
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(List.of(error)));
    }
}
```

## Troubleshooting Guide

### Common Scenarios

#### Scenario 1: 401 Unauthorized

**Symptoms**:

- API returns 401 status code
- Error code: `AUTH_TOKEN_EXPIRED` or `AUTH_TOKEN_INVALID`

**Diagnosis**:

1. Check if token is included in Authorization header
2. Verify token format: `Bearer <token>`
3. Check token expiration time
4. Verify token signature

**Solution**:

```javascript
// Check token expiration
function isTokenExpired(token) {
  const payload = JSON.parse(atob(token.split('.')[1]));
  return payload.exp * 1000 < Date.now();
}

// Refresh if expired
if (isTokenExpired(accessToken)) {
  await refreshToken();
}
```

#### Scenario 2: 400 Bad Request with Validation Errors

**Symptoms**:

- API returns 400 status code
- Multiple validation errors in response

**Diagnosis**:

1. Check all required fields are provided
2. Verify field formats (email, phone, date)
3. Check field length constraints
4. Verify data types

**Solution**:

```javascript
// Validate before sending
function validateCustomer(customer) {
  const errors = [];
  
  if (!customer.email || !isValidEmail(customer.email)) {
    errors.push({ field: 'email', message: 'Invalid email format' });
  }
  
  if (!customer.name || customer.name.length < 2) {
    errors.push({ field: 'name', message: 'Name must be at least 2 characters' });
  }
  
  return errors;
}
```

#### Scenario 3: 409 Conflict - Business Rule Violation

**Symptoms**:

- API returns 409 status code
- Error code: `BUSINESS_RULE_VIOLATION`

**Diagnosis**:

1. Check current resource state
2. Verify business rule constraints
3. Check for duplicate resources

**Solution**:

- Read error message for specific constraint
- Adjust request to satisfy business rules
- Check resource state before operation

#### Scenario 4: 429 Too Many Requests

**Symptoms**:

- API returns 429 status code
- Error code: `RATE_LIMIT_EXCEEDED`

**Diagnosis**:

1. Check rate limit headers
2. Count recent requests
3. Verify rate limit tier

**Solution**:

```javascript
// Implement exponential backoff
async function retryWithBackoff(fn, maxRetries = 3) {
  for (let i = 0; i < maxRetries; i++) {
    try {
      return await fn();
    } catch (error) {
      if (error.status === 429 && i < maxRetries - 1) {
        const delay = Math.pow(2, i) * 1000;
        await sleep(delay);
        continue;
      }
      throw error;
    }
  }
}
```

#### Scenario 5: 500 Internal Server Error

**Symptoms**:

- API returns 500 status code
- Error code: `SYSTEM_INTERNAL_ERROR`

**Diagnosis**:

1. Note the request ID from error response
2. Check if error is reproducible
3. Verify request payload

**Solution**:

- Contact support with request ID
- Retry request after some time
- Check API status page

### Debugging Tips

**1. Enable Request/Response Logging**:

```javascript
// Log all API requests
fetch = new Proxy(fetch, {
  apply(target, thisArg, args) {
    const [url, options] = args;
    console.log('API Request:', { url, options });
    
    return Reflect.apply(target, thisArg, args)
      .then(response => {
        console.log('API Response:', {
          url,
          status: response.status,
          headers: Object.fromEntries(response.headers)
        });
        return response;
      });
  }
});
```

**2. Include Request ID in All Logs**:

```javascript
function generateRequestId() {
  return `req-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
}

async function apiCall(url, options = {}) {
  const requestId = generateRequestId();
  
  options.headers = {
    ...options.headers,
    'X-Request-ID': requestId
  };
  
  console.log(`[${requestId}] Request:`, url);
  
  try {
    const response = await fetch(url, options);
    console.log(`[${requestId}] Response:`, response.status);
    return response;
  } catch (error) {
    console.error(`[${requestId}] Error:`, error);
    throw error;
  }
}
```

**3. Use Browser DevTools**:

- Network tab: Inspect request/response
- Console: Check for JavaScript errors
- Application tab: Verify token storage

**4. Test with curl**:

```bash
# Add verbose output
curl -v -X GET https://api.ecommerce.com/api/v1/customers/me \
  -H "Authorization: Bearer $TOKEN"

# Save response to file
curl -X GET https://api.ecommerce.com/api/v1/customers/me \
  -H "Authorization: Bearer $TOKEN" \
  -o response.json

# Show only HTTP status
curl -s -o /dev/null -w "%{http_code}" \
  https://api.ecommerce.com/api/v1/customers/me
```

## Support

### Getting Help

**1. Check Documentation**:

- [API Reference](endpoints/) - Endpoint documentation
- [Authentication Guide](authentication.md) - Authentication issues
- [FAQ](#frequently-asked-questions) - Common questions

**2. Check API Status**:

- Status Page: <https://status.ecommerce.com>
- Incident History: <https://status.ecommerce.com/history>

**3. Contact Support**:

- Email: <api-support@ecommerce.com>
- Include: Request ID, timestamp, error code
- Response Time: 24 hours

### Frequently Asked Questions

**Q: Why am I getting 401 Unauthorized?**  
A: Check if your token is valid and not expired. Try refreshing your token.

**Q: How do I handle rate limiting?**  
A: Implement exponential backoff and respect the `Retry-After` header.

**Q: What should I do with a 500 error?**  
A: Note the request ID and contact support. The issue is on our end.

**Q: Can I retry failed requests?**  
A: Yes, but only for idempotent operations (GET, PUT, DELETE). Use exponential backoff.

**Q: How long are error logs retained?**  
A: Error logs are retained for 30 days for troubleshooting.

## Related Documentation

- [API Overview](README.md) - API design principles
- [Authentication](authentication.md) - Authentication and authorization
- [Customer API](endpoints/customers.md) - Customer endpoints
- [ADR-009: RESTful API Design](../../architecture/adrs/009-restful-api-design-with-openapi.md) - API design decisions

---

**Last Updated**: 2025-10-25  
**API Version**: v1
