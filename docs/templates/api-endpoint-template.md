# {Resource Name} API

> **Base URL**: `https://api.example.com/api/v1`  
> **Last Updated**: YYYY-MM-DD  
> **API Version**: v1

## Overview

[Brief description of what this API resource does and its purpose in the system]

## Authentication

All endpoints require authentication unless otherwise specified.

```http
Authorization: Bearer {access_token}
```

## Endpoints

---

### Create {Resource}

Create a new {resource} in the system.

#### Request

**Method**: `POST`

**Endpoint**: `/api/v1/{resources}`

**Headers**:

```http
Content-Type: application/json
Authorization: Bearer {access_token}
```

**Request Body**:

```json
{
  "field1": "string",
  "field2": "number",
  "field3": {
    "nestedField1": "string",
    "nestedField2": "boolean"
  },
  "field4": ["array", "of", "strings"]
}
```

**Field Descriptions**:

| Field | Type | Required | Description | Constraints |
|-------|------|----------|-------------|-------------|
| field1 | string | Yes | Description of field1 | Min: 2, Max: 100 characters |
| field2 | number | Yes | Description of field2 | Min: 0, Max: 1000 |
| field3 | object | No | Description of field3 | - |
| field3.nestedField1 | string | Yes | Description | - |
| field3.nestedField2 | boolean | No | Description | Default: false |
| field4 | array | No | Description | Max items: 10 |

#### Response

**Success Response** (201 Created):

```http
HTTP/1.1 201 Created
Content-Type: application/json
Location: /api/v1/{resources}/{id}
```

```json
{
  "id": "resource-123",
  "field1": "string",
  "field2": 42,
  "field3": {
    "nestedField1": "string",
    "nestedField2": true
  },
  "field4": ["array", "of", "strings"],
  "createdAt": "2025-01-17T10:00:00Z",
  "updatedAt": "2025-01-17T10:00:00Z",
  "status": "active"
}
```

**Response Fields**:

| Field | Type | Description |
|-------|------|-------------|
| id | string | Unique identifier for the resource |
| field1 | string | Description |
| field2 | number | Description |
| createdAt | string (ISO 8601) | Timestamp when resource was created |
| updatedAt | string (ISO 8601) | Timestamp when resource was last updated |
| status | string | Current status of the resource |

#### Error Responses

**400 Bad Request** - Invalid input:

```json
{
  "errorCode": "INVALID_INPUT",
  "message": "Validation failed",
  "timestamp": "2025-01-17T10:00:00Z",
  "path": "/api/v1/{resources}",
  "fieldErrors": [
    {
      "field": "field1",
      "message": "Field1 must be between 2 and 100 characters",
      "rejectedValue": "a"
    }
  ]
}
```

**401 Unauthorized** - Missing or invalid authentication:

```json
{
  "errorCode": "UNAUTHORIZED",
  "message": "Authentication required",
  "timestamp": "2025-01-17T10:00:00Z",
  "path": "/api/v1/{resources}"
}
```

**409 Conflict** - Resource already exists:

```json
{
  "errorCode": "RESOURCE_EXISTS",
  "message": "Resource with this identifier already exists",
  "timestamp": "2025-01-17T10:00:00Z",
  "path": "/api/v1/{resources}",
  "context": {
    "existingResourceId": "resource-123"
  }
}
```

**429 Too Many Requests** - Rate limit exceeded:

```json
{
  "errorCode": "RATE_LIMIT_EXCEEDED",
  "message": "Too many requests. Please try again later.",
  "timestamp": "2025-01-17T10:00:00Z",
  "path": "/api/v1/{resources}",
  "retryAfter": 60
}
```

**500 Internal Server Error** - Server error:

```json
{
  "errorCode": "INTERNAL_ERROR",
  "message": "An unexpected error occurred",
  "timestamp": "2025-01-17T10:00:00Z",
  "path": "/api/v1/{resources}"
}
```

#### Examples

**cURL**:

```bash
curl -X POST https://api.example.com/api/v1/{resources} \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "field1": "example value",
    "field2": 42,
    "field3": {
      "nestedField1": "nested value",
      "nestedField2": true
    }
  }'
```

**JavaScript (fetch)**:

```javascript
const response = await fetch('https://api.example.com/api/v1/{resources}', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${accessToken}`
  },
  body: JSON.stringify({
    field1: 'example value',
    field2: 42,
    field3: {
      nestedField1: 'nested value',
      nestedField2: true
    }
  })
});

const data = await response.json();
console.log(data);
```

**Java (Spring RestTemplate)**:

```java
RestTemplate restTemplate = new RestTemplate();
HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.APPLICATION_JSON);
headers.setBearerAuth(accessToken);

CreateResourceRequest request = new CreateResourceRequest(
    "example value",
    42,
    new NestedField("nested value", true)
);

HttpEntity<CreateResourceRequest> entity = new HttpEntity<>(request, headers);
ResponseEntity<ResourceResponse> response = restTemplate.postForEntity(
    "https://api.example.com/api/v1/{resources}",
    entity,
    ResourceResponse.class
);
```

**Python (requests)**:

```python
import requests

url = "https://api.example.com/api/v1/{resources}"
headers = {
    "Content-Type": "application/json",
    "Authorization": f"Bearer {access_token}"
}
payload = {
    "field1": "example value",
    "field2": 42,
    "field3": {
        "nestedField1": "nested value",
        "nestedField2": True
    }
}

response = requests.post(url, json=payload, headers=headers)
data = response.json()
print(data)
```

---

### Get {Resource}

Retrieve a specific {resource} by ID.

#### Request

**Method**: `GET`

**Endpoint**: `/api/v1/{resources}/{id}`

**Headers**:

```http
Authorization: Bearer {access_token}
```

**Path Parameters**:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| id | string | Yes | Unique identifier of the resource |

#### Response

**Success Response** (200 OK):

```http
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
{
  "id": "resource-123",
  "field1": "string",
  "field2": 42,
  "createdAt": "2025-01-17T10:00:00Z",
  "updatedAt": "2025-01-17T10:00:00Z"
}
```

#### Error Responses

**404 Not Found** - Resource not found:

```json
{
  "errorCode": "RESOURCE_NOT_FOUND",
  "message": "Resource with ID 'resource-123' not found",
  "timestamp": "2025-01-17T10:00:00Z",
  "path": "/api/v1/{resources}/resource-123"
}
```

#### Examples

**cURL**:

```bash
curl -X GET https://api.example.com/api/v1/{resources}/resource-123 \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

### List {Resources}

Retrieve a paginated list of {resources}.

#### Request

**Method**: `GET`

**Endpoint**: `/api/v1/{resources}`

**Headers**:

```http
Authorization: Bearer {access_token}
```

**Query Parameters**:

| Parameter | Type | Required | Description | Default |
|-----------|------|----------|-------------|---------|
| page | number | No | Page number (0-indexed) | 0 |
| size | number | No | Number of items per page | 20 |
| sort | string | No | Sort field and direction (e.g., "createdAt,desc") | "createdAt,desc" |
| filter | string | No | Filter criteria | - |

#### Response

**Success Response** (200 OK):

```json
{
  "content": [
    {
      "id": "resource-123",
      "field1": "string",
      "field2": 42
    }
  ],
  "page": {
    "size": 20,
    "number": 0,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

#### Examples

**cURL**:

```bash
curl -X GET "https://api.example.com/api/v1/{resources}?page=0&size=20&sort=createdAt,desc" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

### Update {Resource}

Update an existing {resource}.

#### Request

**Method**: `PUT`

**Endpoint**: `/api/v1/{resources}/{id}`

**Headers**:

```http
Content-Type: application/json
Authorization: Bearer {access_token}
```

**Request Body**: [Same as Create]

#### Response

**Success Response** (200 OK): [Same structure as Get]

#### Error Responses

[Same as Create, plus 404 Not Found]

---

### Partial Update {Resource}

Partially update an existing {resource}.

#### Request

**Method**: `PATCH`

**Endpoint**: `/api/v1/{resources}/{id}`

**Headers**:

```http
Content-Type: application/json
Authorization: Bearer {access_token}
```

**Request Body**:

```json
{
  "field1": "updated value"
}
```

#### Response

**Success Response** (200 OK): [Same structure as Get]

---

### Delete {Resource}

Delete a {resource}.

#### Request

**Method**: `DELETE`

**Endpoint**: `/api/v1/{resources}/{id}`

**Headers**:

```http
Authorization: Bearer {access_token}
```

#### Response

**Success Response** (204 No Content):

```http
HTTP/1.1 204 No Content
```

#### Error Responses

**404 Not Found**: Resource not found

**409 Conflict**: Resource cannot be deleted (e.g., has dependencies)

---

## Rate Limiting

- **Rate Limit**: 1000 requests per hour per user
- **Headers**:
  - `X-RateLimit-Limit`: Total requests allowed
  - `X-RateLimit-Remaining`: Requests remaining
  - `X-RateLimit-Reset`: Time when limit resets (Unix timestamp)

## Pagination

All list endpoints support pagination:

- Default page size: 20
- Maximum page size: 100
- Page numbers are 0-indexed

## Filtering and Sorting

### Sorting

Use the `sort` parameter: `sort=field,direction`

- Example: `sort=createdAt,desc`
- Multiple sorts: `sort=field1,asc&sort=field2,desc`

### Filtering

Use the `filter` parameter with format: `field:operator:value`

- Operators: `eq`, `ne`, `gt`, `lt`, `gte`, `lte`, `like`, `in`
- Example: `filter=status:eq:active`
- Multiple filters: `filter=status:eq:active&filter=createdAt:gt:2025-01-01`

## Versioning

This API uses URL versioning. The current version is `v1`.

When breaking changes are introduced, a new version will be released (e.g., `v2`).

## Changelog

| Date | Version | Changes |
|------|---------|---------|
| YYYY-MM-DD | 1.0 | Initial API version |

---

**API Template Version**: 1.0  
**Last Template Update**: 2025-01-17
