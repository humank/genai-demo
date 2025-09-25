# API 交互圖

本文檔展示系統中各種 API 的交互關係和調用流程。

## API 交互架構圖

```mermaid
graph TB
    subgraph "🌐 客戶端"
        WEB_ADMIN[👨‍💼 管理員<br/>CMC Frontend]
        WEB_CONSUMER[🛒 消費者<br/>Consumer Frontend]
        MOBILE[📱 Mobile App]
        THIRD_PARTY[🔗 第三方系統]
    end
    
    subgraph "🔒 API 閘道"
        API_GATEWAY[🚪 API Gateway]
        AUTH[🔐 Authentication]
        RATE_LIMIT[⏱️ Rate Limiting]
        LOAD_BALANCER[⚖️ Load Balancer]
    end
    
    subgraph "🔌 API 層"
        subgraph "📦 Order APIs"
            ORDER_REST[📋 Order REST API]
            ORDER_GRAPHQL[📊 Order GraphQL]
            ORDER_WEBSOCKET[⚡ Order WebSocket]
        end
        
        subgraph "👥 Customer APIs"
            CUSTOMER_REST[👤 Customer REST API]
            CUSTOMER_GRAPHQL[📊 Customer GraphQL]
            LOYALTY_API[🎁 Loyalty API]
        end
        
        subgraph "🏷️ Product APIs"
            PRODUCT_REST[📦 Product REST API]
            CATALOG_API[📚 Catalog API]
            INVENTORY_API[📊 Inventory API]
        end
        
        subgraph "💳 Payment APIs"
            PAYMENT_REST[💰 Payment REST API]
            WEBHOOK_API[🔔 Webhook API]
        end
    end
    
    subgraph "⚙️ 應用服務"
        ORDER_SVC[📦 Order Service]
        CUSTOMER_SVC[👥 Customer Service]
        PRODUCT_SVC[🏷️ Product Service]
        PAYMENT_SVC[💳 Payment Service]
        NOTIFICATION_SVC[📧 Notification Service]
    end
    
    subgraph "🔗 外部服務"
        PAYMENT_GATEWAY[💳 Payment Gateway]
        EMAIL_SERVICE[📧 Email Service]
        SMS_SERVICE[📱 SMS Service]
        LOGISTICS[🚚 Logistics API]
        ANALYTICS[📊 Analytics Service]
    end
    
    subgraph "📊 監控和文檔"
        SWAGGER[📖 Swagger UI]
        ACTUATOR[🔍 Spring Actuator]
        METRICS[📈 Metrics]
        LOGS[📝 Logs]
    end
    
    %% 客戶端到 API 閘道
    WEB_ADMIN --> API_GATEWAY
    WEB_CONSUMER --> API_GATEWAY
    MOBILE --> API_GATEWAY
    THIRD_PARTY --> API_GATEWAY
    
    %% API 閘道處理
    API_GATEWAY --> AUTH
    API_GATEWAY --> RATE_LIMIT
    API_GATEWAY --> LOAD_BALANCER
    
    %% 負載均衡到 API
    LOAD_BALANCER --> ORDER_REST
    LOAD_BALANCER --> CUSTOMER_REST
    LOAD_BALANCER --> PRODUCT_REST
    LOAD_BALANCER --> PAYMENT_REST
    
    %% GraphQL 和 WebSocket
    LOAD_BALANCER --> ORDER_GRAPHQL
    LOAD_BALANCER --> CUSTOMER_GRAPHQL
    LOAD_BALANCER --> ORDER_WEBSOCKET
    
    %% 專用 API
    LOAD_BALANCER --> LOYALTY_API
    LOAD_BALANCER --> CATALOG_API
    LOAD_BALANCER --> INVENTORY_API
    LOAD_BALANCER --> WEBHOOK_API
    
    %% API 到應用服務
    ORDER_REST --> ORDER_SVC
    ORDER_GRAPHQL --> ORDER_SVC
    ORDER_WEBSOCKET --> ORDER_SVC
    
    CUSTOMER_REST --> CUSTOMER_SVC
    CUSTOMER_GRAPHQL --> CUSTOMER_SVC
    LOYALTY_API --> CUSTOMER_SVC
    
    PRODUCT_REST --> PRODUCT_SVC
    CATALOG_API --> PRODUCT_SVC
    INVENTORY_API --> PRODUCT_SVC
    
    PAYMENT_REST --> PAYMENT_SVC
    WEBHOOK_API --> PAYMENT_SVC
    
    %% 應用服務間調用
    ORDER_SVC --> CUSTOMER_SVC
    ORDER_SVC --> PRODUCT_SVC
    ORDER_SVC --> PAYMENT_SVC
    ORDER_SVC --> NOTIFICATION_SVC
    
    %% 外部服務調用
    PAYMENT_SVC --> PAYMENT_GATEWAY
    NOTIFICATION_SVC --> EMAIL_SERVICE
    NOTIFICATION_SVC --> SMS_SERVICE
    ORDER_SVC --> LOGISTICS
    
    %% 監控和分析
    ORDER_SVC --> ANALYTICS
    CUSTOMER_SVC --> ANALYTICS
    
    %% 文檔和監控
    API_GATEWAY --> SWAGGER
    ORDER_REST --> ACTUATOR
    CUSTOMER_REST --> METRICS
    PRODUCT_REST --> LOGS
    
    %% 樣式定義
    classDef client fill:#e3f2fd,stroke:#1565c0,stroke-width:2px
    classDef gateway fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px
    classDef api fill:#e8f5e8,stroke:#2e7d32,stroke-width:2px
    classDef service fill:#fff3e0,stroke:#ef6c00,stroke-width:2px
    classDef external fill:#ffebee,stroke:#c62828,stroke-width:2px
    classDef monitoring fill:#fce4ec,stroke:#c2185b,stroke-width:2px
    
    class WEB_ADMIN,WEB_CONSUMER,MOBILE,THIRD_PARTY client
    class API_GATEWAY,AUTH,RATE_LIMIT,LOAD_BALANCER gateway
    class ORDER_REST,ORDER_GRAPHQL,ORDER_WEBSOCKET,CUSTOMER_REST,CUSTOMER_GRAPHQL,LOYALTY_API,PRODUCT_REST,CATALOG_API,INVENTORY_API,PAYMENT_REST,WEBHOOK_API api
    class ORDER_SVC,CUSTOMER_SVC,PRODUCT_SVC,PAYMENT_SVC,NOTIFICATION_SVC service
    class PAYMENT_GATEWAY,EMAIL_SERVICE,SMS_SERVICE,LOGISTICS,ANALYTICS external
    class SWAGGER,ACTUATOR,METRICS,LOGS monitoring
```

## API 端點詳細設計

### 📦 訂單 API

#### REST API 端點

```http
# 訂單管理
POST   /../api/v1/orders                    # 創建訂單
GET    /api/v1/orders/{orderId}          # 獲取訂單詳情
PUT    /api/v1/orders/{orderId}/confirm  # 確認訂單
DELETE /api/v1/orders/{orderId}          # 取消訂單

# 訂單查詢
GET    /api/v1/orders                    # 查詢訂單列表
GET    /api/v1/customers/{customerId}/orders  # 客戶訂單
```

#### GraphQL Schema

```graphql
type Order {
  id: ID!
  customerId: ID!
  items: [OrderItem!]!
  totalAmount: Money!
  status: OrderStatus!
  createdAt: DateTime!
}

type Query {
  order(id: ID!): Order
  orders(filter: OrderFilter, pagination: Pagination): OrderConnection
}

type Mutation {
  createOrder(input: CreateOrderInput!): CreateOrderPayload!
  confirmOrder(orderId: ID!): ConfirmOrderPayload!
}
```

#### WebSocket 事件

```javascript
// 訂單狀態更新
{
  "type": "ORDER_STATUS_UPDATED",
  "orderId": "ORDER-123",
  "status": "CONFIRMED",
  "timestamp": "2025-01-21T10:30:00Z"
}

// 庫存變更通知
{
  "type": "INVENTORY_UPDATED",
  "productId": "PROD-456",
  "availableStock": 50,
  "timestamp": "2025-01-21T10:30:00Z"
}
```

### 👥 客戶 API

#### REST API 端點

```http
# 客戶管理
POST   /../api/v1/customers                 # 註冊客戶
GET    /api/v1/customers/{customerId}    # 獲取客戶資訊
PUT    /api/v1/customers/{customerId}    # 更新客戶資訊

# 忠誠度系統
GET    /api/v1/customers/{customerId}/loyalty  # 獲取忠誠度資訊
POST   /api/v1/customers/{customerId}/loyalty/redeem  # 兌換積分
```

### 🏷️ 產品 API

#### REST API 端點

```http
# 產品目錄
GET    /api/v1/products                  # 產品列表
GET    /api/v1/products/{productId}      # 產品詳情
GET    /api/v1/categories                # 產品分類

# 庫存管理
GET    /api/v1/products/{productId}/inventory  # 庫存查詢
POST   /api/v1/products/{productId}/inventory/reserve  # 預留庫存
```

### 💳 支付 API

#### REST API 端點

```http
# 支付處理
POST   /api/v1/payments                  # 創建支付
GET    /api/v1/payments/{paymentId}      # 支付狀態
POST   /api/v1/payments/{paymentId}/refund  # 退款

# Webhook 端點
POST   /api/v1/webhooks/payment-gateway  # 支付閘道回調
```

## API 調用流程

### 🛒 完整購物流程

```mermaid
sequenceDiagram
    participant Consumer as 🛒 消費者前端
    participant Gateway as 🚪 API Gateway
    participant OrderAPI as 📦 Order API
    participant CustomerAPI as 👥 Customer API
    participant ProductAPI as 🏷️ Product API
    participant PaymentAPI as 💳 Payment API
    participant PaymentGW as 💳 Payment Gateway
    
    Consumer->>Gateway: 1. 瀏覽產品
    Gateway->>ProductAPI: GET /api/v1/products
    ProductAPI-->>Gateway: 產品列表
    Gateway-->>Consumer: 產品資訊
    
    Consumer->>Gateway: 2. 添加到購物車
    Gateway->>ProductAPI: GET /api/v1/products/{id}/inventory
    ProductAPI-->>Gateway: 庫存資訊
    Gateway-->>Consumer: 庫存確認
    
    Consumer->>Gateway: 3. 創建訂單
    Gateway->>OrderAPI: POST /api/v1/orders
    OrderAPI->>CustomerAPI: 驗證客戶資訊
    CustomerAPI-->>OrderAPI: 客戶驗證結果
    OrderAPI->>ProductAPI: 預留庫存
    ProductAPI-->>OrderAPI: 庫存預留結果
    OrderAPI-->>Gateway: 訂單創建成功
    Gateway-->>Consumer: 訂單資訊
    
    Consumer->>Gateway: 4. 處理支付
    Gateway->>PaymentAPI: POST /api/v1/payments
    PaymentAPI->>PaymentGW: 支付請求
    PaymentGW-->>PaymentAPI: 支付結果
    PaymentAPI-->>Gateway: 支付狀態
    Gateway-->>Consumer: 支付確認
    
    PaymentGW->>Gateway: 5. Webhook 通知
    Gateway->>PaymentAPI: POST /api/v1/webhooks/payment-gateway
    PaymentAPI->>OrderAPI: 更新訂單狀態
    OrderAPI-->>PaymentAPI: 狀態更新確認
```

## API 安全設計

### 🔐 認證和授權

```http
# JWT Token 認證
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

# API Key 認證 (第三方系統)
X-API-Key: your-api-key-here

# OAuth 2.0 (外部整合)
Authorization: Bearer oauth-access-token
```

### 🛡️ 安全標頭

```http
# CORS 設定
Access-Control-Allow-Origin: https://your-frontend.com
Access-Control-Allow-Methods: GET, POST, PUT, DELETE
Access-Control-Allow-Headers: Content-Type, Authorization

# 安全標頭
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
```

### ⏱️ 速率限制

```http
# 速率限制標頭
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1642781400
```

## API 版本管理

### 📋 版本策略

- **URL 版本**: `/../api/v1/orders`, `/api/v2/orders`
- **標頭版本**: `Accept: application/vnd.api+json;version=1`
- **向後相容**: 支援舊版本至少 6 個月

### 🔄 版本遷移

```http
# 版本棄用警告
Deprecation: true
Sunset: Wed, 11 Nov 2025 23:59:59 GMT
Link: </api/v2/orders>; rel="successor-version"
```

## API 監控和可觀測性

### 📊 關鍵指標

- **回應時間**: P50, P95, P99
- **錯誤率**: 4xx, 5xx 錯誤百分比
- **吞吐量**: 每秒請求數 (RPS)
- **可用性**: 正常運行時間百分比

### 🔍 分散式追蹤

```http
# 追蹤標頭
X-Trace-Id: 550e8400-e29b-41d4-a716-446655440000
X-Span-Id: 6e0c63257de34c92
X-Parent-Span-Id: 05e3ac9a4f6e3b90
```

## 相關文檔

- [架構概覽](architecture-overview.md) - 整體系統架構
- [事件驅動架構](event-driven-architecture.md) - 事件處理機制
- [API 文檔](../../api/) - 詳細 API 規範
