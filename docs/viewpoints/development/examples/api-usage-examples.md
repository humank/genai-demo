# API Usage Examples

> **Purpose**: Practical, runnable examples for API integration  
> **Last Updated**: 2024-11-19  
> **Owner**: API Team

---

## Overview

This document provides practical examples for integrating with the E-Commerce Platform APIs, including authentication, common operations, and error handling.

---

## Example 1: Authentication Flow

### Scenario

Authenticate a user and obtain a JWT token for API access.

### Prerequisites

- Valid user credentials
- API endpoint URL
- HTTP client (curl, Postman, or programming language)

### Steps

**1. Login and Get JWT Token**

```bash
# Set variables
export API_URL="https://api.example.com"
export USERNAME="user@example.com"
export PASSWORD="SecurePassword123!"

# Login request
curl -X POST "${API_URL}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "'"${USERNAME}"'",
    "password": "'"${PASSWORD}"'"
  }' | jq '.'
```

**Expected Response**:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": "user-123",
    "email": "user@example.com",
    "name": "John Doe",
    "roles": ["CUSTOMER"]
  }
}
```

**2. Use Access Token**

```bash
# Store token
export ACCESS_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Make authenticated request
curl -X GET "${API_URL}/api/v1/customers/me" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" | jq '.'
```

**3. Refresh Token**

```bash
# When access token expires, use refresh token
export REFRESH_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

curl -X POST "${API_URL}/api/v1/auth/refresh" \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "'"${REFRESH_TOKEN}"'"
  }' | jq '.'
```

### Java Example

```java
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

public class AuthenticationExample {
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final String apiUrl = "https://api.example.com";
    
    public AuthResponse login(String email, String password) {
        String url = apiUrl + "/api/v1/auth/login";
        
        LoginRequest request = new LoginRequest(email, password);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<LoginRequest> entity = new HttpEntity<>(request, headers);
        
        ResponseEntity<AuthResponse> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            entity,
            AuthResponse.class
        );
        
        return response.getBody();
    }
    
    public CustomerResponse getCustomerProfile(String accessToken) {
        String url = apiUrl + "/api/v1/customers/me";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        ResponseEntity<CustomerResponse> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            CustomerResponse.class
        );
        
        return response.getBody();
    }
}

record LoginRequest(String email, String password) {}
record AuthResponse(String accessToken, String refreshToken, String tokenType, int expiresIn, UserInfo user) {}
record UserInfo(String id, String email, String name, List<String> roles) {}
record CustomerResponse(String id, String name, String email, String phone) {}
```

### TypeScript Example

```typescript
import axios, { AxiosInstance } from 'axios';

interface LoginRequest {
  email: string;
  password: string;
}

interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: {
    id: string;
    email: string;
    name: string;
    roles: string[];
  };
}

class ApiClient {
  private client: AxiosInstance;
  private accessToken?: string;
  
  constructor(baseURL: string) {
    this.client = axios.create({
      baseURL,
      headers: {
        'Content-Type': 'application/json',
      },
    });
    
    // Add request interceptor for auth
    this.client.interceptors.request.use((config) => {
      if (this.accessToken) {
        config.headers.Authorization = `Bearer ${this.accessToken}`;
      }
      return config;
    });
  }
  
  async login(email: string, password: string): Promise<AuthResponse> {
    const response = await this.client.post<AuthResponse>('/api/v1/auth/login', {
      email,
      password,
    });
    
    this.accessToken = response.data.accessToken;
    return response.data;
  }
  
  async getCustomerProfile() {
    const response = await this.client.get('/api/v1/customers/me');
    return response.data;
  }
}

// Usage
const client = new ApiClient('https://api.example.com');
await client.login('user@example.com', 'SecurePassword123!');
const profile = await client.getCustomerProfile();
```

---

## Example 2: Create Order

### Scenario

Create a new order with multiple items.

### Prerequisites

- Valid authentication token
- Customer account
- Products in catalog

### Steps

**1. Create Order (cURL)**

```bash
# Create order
curl -X POST "${API_URL}/api/v1/orders" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-123",
    "items": [
      {
        "productId": "product-456",
        "quantity": 2,
        "price": 29.99
      },
      {
        "productId": "product-789",
        "quantity": 1,
        "price": 49.99
      }
    ],
    "shippingAddress": {
      "street": "123 Main St",
      "city": "San Francisco",
      "state": "CA",
      "zipCode": "94102",
      "country": "US"
    },
    "paymentMethod": {
      "type": "CREDIT_CARD",
      "cardToken": "tok_visa_4242"
    }
  }' | jq '.'
```

**Expected Response**:

```json
{
  "orderId": "order-abc123",
  "customerId": "customer-123",
  "status": "PENDING",
  "items": [
    {
      "productId": "product-456",
      "quantity": 2,
      "price": 29.99,
      "subtotal": 59.98
    },
    {
      "productId": "product-789",
      "quantity": 1,
      "price": 49.99,
      "subtotal": 49.99
    }
  ],
  "subtotal": 109.97,
  "tax": 9.90,
  "shipping": 5.99,
  "total": 125.86,
  "createdAt": "2024-11-19T14:30:00Z",
  "estimatedDelivery": "2024-11-22T14:30:00Z"
}
```

**2. Java Example**

```java
public class OrderExample {
    
    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String accessToken;
    
    public OrderResponse createOrder(CreateOrderRequest request) {
        String url = apiUrl + "/api/v1/orders";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<CreateOrderRequest> entity = new HttpEntity<>(request, headers);
        
        ResponseEntity<OrderResponse> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            entity,
            OrderResponse.class
        );
        
        return response.getBody();
    }
    
    public OrderResponse getOrder(String orderId) {
        String url = apiUrl + "/api/v1/orders/" + orderId;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        ResponseEntity<OrderResponse> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            OrderResponse.class
        );
        
        return response.getBody();
    }
}

record CreateOrderRequest(
    String customerId,
    List<OrderItem> items,
    Address shippingAddress,
    PaymentMethod paymentMethod
) {}

record OrderItem(String productId, int quantity, BigDecimal price) {}
record Address(String street, String city, String state, String zipCode, String country) {}
record PaymentMethod(String type, String cardToken) {}
record OrderResponse(String orderId, String customerId, String status, List<OrderItem> items, 
                    BigDecimal subtotal, BigDecimal tax, BigDecimal shipping, BigDecimal total,
                    String createdAt, String estimatedDelivery) {}
```

**3. TypeScript Example**

```typescript
interface CreateOrderRequest {
  customerId: string;
  items: OrderItem[];
  shippingAddress: Address;
  paymentMethod: PaymentMethod;
}

interface OrderItem {
  productId: string;
  quantity: number;
  price: number;
}

interface Address {
  street: string;
  city: string;
  state: string;
  zipCode: string;
  country: string;
}

interface PaymentMethod {
  type: string;
  cardToken: string;
}

class OrderService {
  constructor(private client: ApiClient) {}
  
  async createOrder(request: CreateOrderRequest) {
    const response = await this.client.post('/api/v1/orders', request);
    return response.data;
  }
  
  async getOrder(orderId: string) {
    const response = await this.client.get(`/api/v1/orders/${orderId}`);
    return response.data;
  }
  
  async cancelOrder(orderId: string) {
    const response = await this.client.post(`/api/v1/orders/${orderId}/cancel`);
    return response.data;
  }
}
```

---

## Example 3: Search Products

### Scenario

Search for products with filters and pagination.

### Steps

**1. Basic Search**

```bash
# Search products by keyword
curl -X GET "${API_URL}/api/v1/products/search?q=laptop&page=0&size=20" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" | jq '.'
```

**2. Advanced Search with Filters**

```bash
# Search with category, price range, and sorting
curl -X GET "${API_URL}/api/v1/products/search" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -G \
  --data-urlencode "q=laptop" \
  --data-urlencode "category=electronics" \
  --data-urlencode "minPrice=500" \
  --data-urlencode "maxPrice=2000" \
  --data-urlencode "sort=price,asc" \
  --data-urlencode "page=0" \
  --data-urlencode "size=20" | jq '.'
```

**Expected Response**:

```json
{
  "content": [
    {
      "productId": "product-123",
      "name": "Dell XPS 13 Laptop",
      "description": "13-inch ultrabook with Intel i7",
      "price": 1299.99,
      "category": "electronics",
      "inStock": true,
      "rating": 4.5,
      "reviewCount": 234
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "sorted": true,
      "unsorted": false
    }
  },
  "totalElements": 45,
  "totalPages": 3,
  "last": false,
  "first": true
}
```

**3. Java Example**

```java
public class ProductSearchExample {
    
    public Page<Product> searchProducts(ProductSearchCriteria criteria) {
        UriComponentsBuilder builder = UriComponentsBuilder
            .fromHttpUrl(apiUrl + "/api/v1/products/search")
            .queryParam("q", criteria.query())
            .queryParam("category", criteria.category())
            .queryParam("minPrice", criteria.minPrice())
            .queryParam("maxPrice", criteria.maxPrice())
            .queryParam("sort", criteria.sort())
            .queryParam("page", criteria.page())
            .queryParam("size", criteria.size());
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        ResponseEntity<ProductPage> response = restTemplate.exchange(
            builder.toUriString(),
            HttpMethod.GET,
            entity,
            ProductPage.class
        );
        
        return response.getBody();
    }
}

record ProductSearchCriteria(
    String query,
    String category,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    String sort,
    int page,
    int size
) {}
```

---

## Example 4: Error Handling

### Scenario

Handle common API errors gracefully.

### Common Error Responses

**1. Validation Error (400)**

```json
{
  "timestamp": "2024-11-19T14:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/orders",
  "fieldErrors": [
    {
      "field": "items",
      "message": "Order must contain at least one item"
    },
    {
      "field": "shippingAddress.zipCode",
      "message": "Invalid zip code format"
    }
  ]
}
```

**2. Authentication Error (401)**

```json
{
  "timestamp": "2024-11-19T14:30:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired token",
  "path": "/api/v1/customers/me"
}
```

**3. Authorization Error (403)**

```json
{
  "timestamp": "2024-11-19T14:30:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Insufficient permissions to access this resource",
  "path": "/api/v1/admin/users"
}
```

**4. Not Found Error (404)**

```json
{
  "timestamp": "2024-11-19T14:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Order not found with id: order-xyz",
  "path": "/api/v1/orders/order-xyz"
}
```

**5. Business Rule Violation (409)**

```json
{
  "timestamp": "2024-11-19T14:30:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Insufficient inventory for product",
  "path": "/api/v1/orders",
  "details": {
    "productId": "product-123",
    "requested": 10,
    "available": 5
  }
}
```

### Error Handling Implementation

**Java Example**:

```java
public class ApiErrorHandler {
    
    public <T> T executeWithRetry(Supplier<T> apiCall, int maxRetries) {
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                return apiCall.get();
            } catch (HttpClientErrorException e) {
                handleClientError(e);
                throw e; // Don't retry client errors
            } catch (HttpServerErrorException e) {
                attempt++;
                if (attempt >= maxRetries) {
                    throw e;
                }
                waitBeforeRetry(attempt);
            }
        }
        throw new RuntimeException("Max retries exceeded");
    }
    
    private void handleClientError(HttpClientErrorException e) {
        switch (e.getStatusCode().value()) {
            case 400 -> handleValidationError(e);
            case 401 -> handleAuthenticationError(e);
            case 403 -> handleAuthorizationError(e);
            case 404 -> handleNotFoundError(e);
            case 409 -> handleConflictError(e);
            default -> throw e;
        }
    }
    
    private void waitBeforeRetry(int attempt) {
        try {
            Thread.sleep((long) Math.pow(2, attempt) * 1000); // Exponential backoff
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

**TypeScript Example**:

```typescript
class ApiErrorHandler {
  async executeWithRetry<T>(
    apiCall: () => Promise<T>,
    maxRetries: number = 3
  ): Promise<T> {
    let attempt = 0;
    
    while (attempt < maxRetries) {
      try {
        return await apiCall();
      } catch (error) {
        if (axios.isAxiosError(error)) {
          const status = error.response?.status;
          
          // Don't retry client errors (4xx)
          if (status && status >= 400 && status < 500) {
            this.handleClientError(error);
            throw error;
          }
          
          // Retry server errors (5xx)
          attempt++;
          if (attempt >= maxRetries) {
            throw error;
          }
          
          await this.waitBeforeRetry(attempt);
        } else {
          throw error;
        }
      }
    }
    
    throw new Error('Max retries exceeded');
  }
  
  private handleClientError(error: AxiosError) {
    const status = error.response?.status;
    const data = error.response?.data;
    
    switch (status) {
      case 400:
        console.error('Validation error:', data);
        break;
      case 401:
        console.error('Authentication error - token may be expired');
        // Trigger re-authentication
        break;
      case 403:
        console.error('Authorization error - insufficient permissions');
        break;
      case 404:
        console.error('Resource not found');
        break;
      case 409:
        console.error('Business rule violation:', data);
        break;
    }
  }
  
  private async waitBeforeRetry(attempt: number): Promise<void> {
    const delay = Math.pow(2, attempt) * 1000; // Exponential backoff
    await new Promise(resolve => setTimeout(resolve, delay));
  }
}
```

---

## Example 5: Webhook Integration

### Scenario

Receive and process webhook events from the platform.

### Steps

**1. Register Webhook Endpoint**

```bash
# Register webhook
curl -X POST "${API_URL}/api/v1/webhooks" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://your-app.com/webhooks/ecommerce",
    "events": [
      "order.created",
      "order.completed",
      "payment.succeeded",
      "payment.failed"
    ],
    "secret": "your-webhook-secret"
  }' | jq '.'
```

**2. Webhook Handler Implementation (Java)**

```java
@RestController
@RequestMapping("/webhooks/ecommerce")
public class WebhookController {
    
    private final String webhookSecret = "your-webhook-secret";
    
    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader("X-Webhook-Signature") String signature,
            @RequestBody String payload) {
        
        // Verify signature
        if (!verifySignature(payload, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Parse event
        WebhookEvent event = parseEvent(payload);
        
        // Process event
        switch (event.type()) {
            case "order.created" -> handleOrderCreated(event);
            case "order.completed" -> handleOrderCompleted(event);
            case "payment.succeeded" -> handlePaymentSucceeded(event);
            case "payment.failed" -> handlePaymentFailed(event);
            default -> log.warn("Unknown event type: {}", event.type());
        }
        
        return ResponseEntity.ok().build();
    }
    
    private boolean verifySignature(String payload, String signature) {
        String computed = HmacUtils.hmacSha256Hex(webhookSecret, payload);
        return MessageDigest.isEqual(
            computed.getBytes(),
            signature.getBytes()
        );
    }
}

record WebhookEvent(String id, String type, String timestamp, Map<String, Object> data) {}
```

**3. Webhook Handler Implementation (TypeScript)**

```typescript
import express from 'express';
import crypto from 'crypto';

const app = express();
const webhookSecret = 'your-webhook-secret';

app.post('/webhooks/ecommerce', express.raw({ type: 'application/json' }), (req, res) => {
  const signature = req.headers['x-webhook-signature'] as string;
  const payload = req.body.toString();
  
  // Verify signature
  if (!verifySignature(payload, signature)) {
    return res.status(401).send('Invalid signature');
  }
  
  // Parse event
  const event = JSON.parse(payload);
  
  // Process event
  switch (event.type) {
    case 'order.created':
      handleOrderCreated(event);
      break;
    case 'order.completed':
      handleOrderCompleted(event);
      break;
    case 'payment.succeeded':
      handlePaymentSucceeded(event);
      break;
    case 'payment.failed':
      handlePaymentFailed(event);
      break;
    default:
      console.warn('Unknown event type:', event.type);
  }
  
  res.status(200).send('OK');
});

function verifySignature(payload: string, signature: string): boolean {
  const computed = crypto
    .createHmac('sha256', webhookSecret)
    .update(payload)
    .digest('hex');
  
  return crypto.timingSafeEqual(
    Buffer.from(computed),
    Buffer.from(signature)
  );
}
```

---


**Document Version**: 1.0  
**Last Updated**: 2024-11-19  
**Owner**: API Team
