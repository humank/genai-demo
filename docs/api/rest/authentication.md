# Authentication

## Overview

The Enterprise E-Commerce Platform API uses JWT (JSON Web Token) based authentication to secure API endpoints. This document describes the authentication flow, token format, and best practices.

## Authentication Flow

### 1. User Login

**Endpoint**: `POST /api/v1/auth/login`

**Request**:

```http
POST /api/v1/auth/login HTTP/1.1
Host: api.ecommerce.com
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

**Response**:

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwicm9sZXMiOlsiVVNFUiJdLCJpYXQiOjE2MzU3ODk2MDAsImV4cCI6MTYzNTc5MzIwMH0.signature",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwidHlwZSI6InJlZnJlc2giLCJpYXQiOjE2MzU3ODk2MDAsImV4cCI6MTYzNjM5NDQwMH0.signature",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": "user-123",
      "email": "user@example.com",
      "name": "John Doe",
      "roles": ["USER"]
    }
  },
  "metadata": {
    "requestId": "req-abc-123",
    "timestamp": "2025-10-25T10:30:00Z",
    "version": "v1"
  }
}
```

### 2. Using Access Token

Include the access token in the `Authorization` header for all authenticated requests:

```http
GET /api/v1/customers/me HTTP/1.1
Host: api.ecommerce.com
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

### 3. Token Refresh

When the access token expires, use the refresh token to obtain a new access token:

**Endpoint**: `POST /api/v1/auth/refresh`

**Request**:

```http
POST /api/v1/auth/refresh HTTP/1.1
Host: api.ecommerce.com
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**Response**:

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9.new_token...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9.new_refresh_token...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

### 4. Logout

**Endpoint**: `POST /api/v1/auth/logout`

**Request**:

```http
POST /api/v1/auth/logout HTTP/1.1
Host: api.ecommerce.com
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

**Response**:

```http
HTTP/1.1 204 No Content
```

## JWT Token Format

### Access Token Structure

The access token is a JWT with the following structure:

**Header**:

```json
{
  "alg": "HS512",
  "typ": "JWT"
}
```

**Payload**:

```json
{
  "sub": "user@example.com",
  "userId": "user-123",
  "roles": ["USER", "CUSTOMER"],
  "permissions": ["read:orders", "write:orders"],
  "iat": 1635789600,
  "exp": 1635793200,
  "jti": "token-unique-id"
}
```

**Claims Explanation**:

- `sub`: Subject (user email)
- `userId`: User unique identifier
- `roles`: User roles (e.g., USER, ADMIN, CUSTOMER)
- `permissions`: Specific permissions
- `iat`: Issued at timestamp
- `exp`: Expiration timestamp
- `jti`: JWT ID (unique token identifier)

### Refresh Token Structure

The refresh token has a longer expiration time and is used only for obtaining new access tokens:

**Payload**:

```json
{
  "sub": "user@example.com",
  "userId": "user-123",
  "type": "refresh",
  "iat": 1635789600,
  "exp": 1636394400,
  "jti": "refresh-token-unique-id"
}
```

### Token Expiration

| Token Type | Expiration | Purpose |
|------------|------------|---------|
| Access Token | 1 hour | API authentication |
| Refresh Token | 7 days | Token renewal |

## Authorization

### Role-Based Access Control (RBAC)

The API uses role-based access control to manage permissions:

**Roles**:

- `USER`: Basic authenticated user
- `CUSTOMER`: Customer with shopping privileges
- `ADMIN`: Administrative access
- `SELLER`: Seller/vendor access
- `SUPPORT`: Customer support access

**Role Hierarchy**:

```text
ADMIN > SELLER > SUPPORT > CUSTOMER > USER
```

### Permission Checks

Endpoints may require specific roles or permissions:

**Example - Admin Only**:

```http
GET /api/v1/admin/users
Authorization: Bearer <admin-token>
```

**Response (Forbidden)**:

```http
HTTP/1.1 403 Forbidden

{
  "errors": [
    {
      "code": "FORBIDDEN",
      "message": "Insufficient permissions. Required role: ADMIN"
    }
  ]
}
```

### Resource-Level Authorization

Some endpoints enforce resource-level authorization:

**Example - Own Resource Access**:

```http
GET /api/v1/customers/cust-123
Authorization: Bearer <user-token>
```

- ✅ Allowed if token belongs to `cust-123` or user has `ADMIN` role
- ❌ Forbidden if token belongs to different user without `ADMIN` role

## Authentication Endpoints

### Login

**Endpoint**: `POST /api/v1/auth/login`

**Request Body**:

```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

**Validation Rules**:

- Email: Required, valid email format
- Password: Required, minimum 8 characters

**Success Response** (200 OK):

```json
{
  "data": {
    "accessToken": "...",
    "refreshToken": "...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": "user-123",
      "email": "user@example.com",
      "name": "John Doe",
      "roles": ["USER", "CUSTOMER"]
    }
  }
}
```

**Error Responses**:

- `400 Bad Request`: Invalid email or password format
- `401 Unauthorized`: Invalid credentials
- `429 Too Many Requests`: Too many failed login attempts

### Register

**Endpoint**: `POST /api/v1/auth/register`

**Request Body**:

```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "SecurePassword123!",
  "confirmPassword": "SecurePassword123!"
}
```

**Validation Rules**:

- Name: Required, 2-100 characters
- Email: Required, valid email format, unique
- Password: Required, minimum 8 characters, must contain uppercase, lowercase, and number
- Confirm Password: Must match password

**Success Response** (201 Created):

```json
{
  "data": {
    "accessToken": "...",
    "refreshToken": "...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": "user-123",
      "email": "john@example.com",
      "name": "John Doe",
      "roles": ["USER", "CUSTOMER"]
    }
  }
}
```

**Error Responses**:

- `400 Bad Request`: Validation errors
- `409 Conflict`: Email already registered

### Refresh Token

**Endpoint**: `POST /api/v1/auth/refresh`

**Request Body**:

```json
{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**Success Response** (200 OK):

```json
{
  "data": {
    "accessToken": "...",
    "refreshToken": "...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

**Error Responses**:

- `400 Bad Request`: Missing refresh token
- `401 Unauthorized`: Invalid or expired refresh token

### Logout

**Endpoint**: `POST /api/v1/auth/logout`

**Headers**:

```http
Authorization: Bearer <access-token>
```

**Success Response** (204 No Content)

**Error Responses**:

- `401 Unauthorized`: Invalid or missing token

### Password Reset Request

**Endpoint**: `POST /api/v1/auth/password-reset/request`

**Request Body**:

```json
{
  "email": "user@example.com"
}
```

**Success Response** (200 OK):

```json
{
  "data": {
    "message": "Password reset email sent if account exists"
  }
}
```

**Note**: Always returns success to prevent email enumeration

### Password Reset Confirm

**Endpoint**: `POST /api/v1/auth/password-reset/confirm`

**Request Body**:

```json
{
  "token": "reset-token-from-email",
  "newPassword": "NewSecurePassword123!",
  "confirmPassword": "NewSecurePassword123!"
}
```

**Success Response** (200 OK):

```json
{
  "data": {
    "message": "Password reset successful"
  }
}
```

**Error Responses**:

- `400 Bad Request`: Invalid token or password validation failed
- `401 Unauthorized`: Expired reset token

## Code Examples

### JavaScript (Fetch API)

```javascript
// Login
async function login(email, password) {
  const response = await fetch('https://api.ecommerce.com/api/v1/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ email, password })
  });
  
  if (!response.ok) {
    throw new Error('Login failed');
  }
  
  const data = await response.json();
  
  // Store tokens securely
  sessionStorage.setItem('accessToken', data.data.accessToken);
  sessionStorage.setItem('refreshToken', data.data.refreshToken);
  
  return data.data.user;
}

// Authenticated request
async function getProfile() {
  const accessToken = sessionStorage.getItem('accessToken');
  
  const response = await fetch('https://api.ecommerce.com/api/v1/customers/me', {
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  });
  
  if (response.status === 401) {
    // Token expired, try refresh
    await refreshToken();
    return getProfile(); // Retry
  }
  
  return response.json();
}

// Refresh token
async function refreshToken() {
  const refreshToken = sessionStorage.getItem('refreshToken');
  
  const response = await fetch('https://api.ecommerce.com/api/v1/auth/refresh', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ refreshToken })
  });
  
  if (!response.ok) {
    // Refresh failed, redirect to login
    window.location.href = '/login';
    return;
  }
  
  const data = await response.json();
  sessionStorage.setItem('accessToken', data.data.accessToken);
  sessionStorage.setItem('refreshToken', data.data.refreshToken);
}
```

### Java (Spring RestTemplate)

```java
// Login
public class AuthService {
    private final RestTemplate restTemplate;
    private final String apiBaseUrl = "https://api.ecommerce.com";
    
    public LoginResponse login(String email, String password) {
        LoginRequest request = new LoginRequest(email, password);
        
        ResponseEntity<ApiResponse<LoginResponse>> response = restTemplate.postForEntity(
            apiBaseUrl + "/api/v1/auth/login",
            request,
            new ParameterizedTypeReference<ApiResponse<LoginResponse>>() {}
        );
        
        return response.getBody().getData();
    }
    
    // Authenticated request
    public CustomerResponse getProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        ResponseEntity<ApiResponse<CustomerResponse>> response = restTemplate.exchange(
            apiBaseUrl + "/api/v1/customers/me",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<ApiResponse<CustomerResponse>>() {}
        );
        
        return response.getBody().getData();
    }
}
```

### Python (Requests)

```python
import requests

class AuthClient:
    def __init__(self, base_url="https://api.ecommerce.com"):
        self.base_url = base_url
        self.access_token = None
        self.refresh_token = None
    
    def login(self, email, password):
        response = requests.post(
            f"{self.base_url}/api/v1/auth/login",
            json={"email": email, "password": password}
        )
        response.raise_for_status()
        
        data = response.json()["data"]
        self.access_token = data["accessToken"]
        self.refresh_token = data["refreshToken"]
        
        return data["user"]
    
    def get_profile(self):
        headers = {"Authorization": f"Bearer {self.access_token}"}
        
        response = requests.get(
            f"{self.base_url}/api/v1/customers/me",
            headers=headers
        )
        
        if response.status_code == 401:
            # Token expired, refresh
            self.refresh_access_token()
            return self.get_profile()  # Retry
        
        response.raise_for_status()
        return response.json()["data"]
    
    def refresh_access_token(self):
        response = requests.post(
            f"{self.base_url}/api/v1/auth/refresh",
            json={"refreshToken": self.refresh_token}
        )
        response.raise_for_status()
        
        data = response.json()["data"]
        self.access_token = data["accessToken"]
        self.refresh_token = data["refreshToken"]
```

### curl

```bash
# Login
curl -X POST https://api.ecommerce.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePassword123!"
  }'

# Save token to variable
TOKEN="eyJhbGciOiJIUzUxMiJ9..."

# Authenticated request
curl -X GET https://api.ecommerce.com/api/v1/customers/me \
  -H "Authorization: Bearer $TOKEN"

# Refresh token
curl -X POST https://api.ecommerce.com/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
  }'
```

## Security Best Practices

### Token Storage

**✅ Recommended**:

- Store tokens in memory (JavaScript variables)
- Use HttpOnly cookies for web applications
- Use secure storage APIs on mobile (Keychain, KeyStore)
- Use session storage for single-tab applications

**❌ Not Recommended**:

- localStorage (vulnerable to XSS attacks)
- Cookies without HttpOnly flag
- Plain text files
- URL parameters

### Token Transmission

**✅ Always**:

- Use HTTPS in production
- Include tokens in Authorization header
- Validate SSL certificates

**❌ Never**:

- Send tokens in URL parameters
- Send tokens over HTTP
- Log tokens in application logs
- Share tokens between users

### Token Lifecycle

**Best Practices**:

1. **Short-Lived Access Tokens**: 1 hour expiration
2. **Longer Refresh Tokens**: 7 days expiration
3. **Token Rotation**: Issue new refresh token on refresh
4. **Revocation**: Implement token blacklist for logout
5. **Monitoring**: Track token usage and anomalies

### Password Security

**Requirements**:

- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one number
- At least one special character
- No common passwords (checked against dictionary)

**Best Practices**:

- Use password managers
- Enable two-factor authentication (2FA)
- Change passwords regularly
- Never reuse passwords

## Troubleshooting

### Common Issues

**401 Unauthorized**:

- **Cause**: Invalid or expired token
- **Solution**: Refresh token or re-authenticate

**403 Forbidden**:

- **Cause**: Insufficient permissions
- **Solution**: Check required roles/permissions

**429 Too Many Requests**:

- **Cause**: Rate limit exceeded
- **Solution**: Implement exponential backoff

### Error Codes

| Code | Description | Action |
|------|-------------|--------|
| `INVALID_CREDENTIALS` | Wrong email or password | Check credentials |
| `TOKEN_EXPIRED` | Access token expired | Refresh token |
| `TOKEN_INVALID` | Malformed or invalid token | Re-authenticate |
| `REFRESH_TOKEN_EXPIRED` | Refresh token expired | Re-authenticate |
| `INSUFFICIENT_PERMISSIONS` | Missing required role | Contact administrator |
| `ACCOUNT_LOCKED` | Too many failed attempts | Wait or contact support |

## Related Documentation

- [Error Handling](error-handling.md) - Complete error code reference
- [Customer API](endpoints/customers.md) - Customer profile management
- [Security Perspective](../../perspectives/security/) - Security architecture
- [ADR-014: JWT Authentication](../../architecture/adrs/014-jwt-authentication-strategy.md) - Authentication design decisions

---

**Last Updated**: 2025-10-25  
**API Version**: v1
