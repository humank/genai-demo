# Product API

## Overview

The Product API provides endpoints for managing the product catalog, including product information, inventory, pricing, and search functionality.

**Base Path**: `/api/v1/products`

**Authentication**:

- Read operations: Not required (public)
- Write operations: ADMIN or SELLER role required

## Endpoints

### List Products

Retrieve a paginated list of products with filtering and search.

**Endpoint**: `GET /api/v1/products`

**Authentication**: Not required

**Query Parameters**:

- `page`: Page number (0-based, default: 0)
- `size`: Page size (default: 20, max: 100)
- `sort`: Sort field and direction (default: `createdAt,desc`)
  - Options: `name,asc`, `price,asc`, `price,desc`, `popularity,desc`
- `search`: Search term (searches name and description)
- `category`: Filter by category ID
- `minPrice`: Minimum price filter
- `maxPrice`: Maximum price filter
- `inStock`: Filter by stock availability (true/false)
- `brand`: Filter by brand name

**Success Response** (200 OK):

```json
{
  "data": {
    "content": [
      {
        "id": "prod-456",
        "name": "Wireless Mouse",
        "description": "Ergonomic wireless mouse with 2.4GHz connection",
        "price": 500.00,
        "currency": "TWD",
        "category": {
          "id": "cat-123",
          "name": "Computer Accessories"
        },
        "brand": "TechBrand",
        "images": [
          {
            "url": "https://cdn.ecommerce.com/products/prod-456-1.jpg",
            "alt": "Wireless Mouse - Front View",
            "isPrimary": true
          }
        ],
        "inStock": true,
        "stockQuantity": 150,
        "rating": 4.5,
        "reviewCount": 234,
        "createdAt": "2025-10-20T10:00:00Z"
      }
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 500,
      "totalPages": 25,
      "first": true,
      "last": false
    }
  }
}
```

**curl Example**:

```bash
# Basic list
curl -X GET "https://api.ecommerce.com/api/v1/products?page=0&size=20"

# With search and filters
curl -X GET "https://api.ecommerce.com/api/v1/products?search=mouse&category=cat-123&minPrice=100&maxPrice=1000&inStock=true"

# Sort by price
curl -X GET "https://api.ecommerce.com/api/v1/products?sort=price,asc"
```

---

### Get Product by ID

Retrieve detailed information about a specific product.

**Endpoint**: `GET /api/v1/products/{id}`

**Authentication**: Not required

**Path Parameters**:

- `id`: Product ID (e.g., `prod-456`)

**Success Response** (200 OK):

```json
{
  "data": {
    "id": "prod-456",
    "sku": "WM-2025-001",
    "name": "Wireless Mouse",
    "description": "Ergonomic wireless mouse with 2.4GHz connection. Features include adjustable DPI, 6 programmable buttons, and long battery life.",
    "price": 500.00,
    "originalPrice": 600.00,
    "discount": 100.00,
    "discountPercentage": 16.67,
    "currency": "TWD",
    "category": {
      "id": "cat-123",
      "name": "Computer Accessories",
      "path": "Electronics > Computer Accessories"
    },
    "brand": "TechBrand",
    "images": [
      {
        "url": "https://cdn.ecommerce.com/products/prod-456-1.jpg",
        "alt": "Wireless Mouse - Front View",
        "isPrimary": true
      },
      {
        "url": "https://cdn.ecommerce.com/products/prod-456-2.jpg",
        "alt": "Wireless Mouse - Side View",
        "isPrimary": false
      }
    ],
    "specifications": {
      "color": "Black",
      "connectivity": "2.4GHz Wireless",
      "dpi": "800-3200",
      "buttons": "6",
      "battery": "AA x 1",
      "weight": "85g"
    },
    "inStock": true,
    "stockQuantity": 150,
    "lowStockThreshold": 20,
    "rating": 4.5,
    "reviewCount": 234,
    "tags": ["wireless", "ergonomic", "gaming"],
    "relatedProducts": ["prod-789", "prod-101"],
    "createdAt": "2025-10-20T10:00:00Z",
    "updatedAt": "2025-10-25T10:00:00Z"
  }
}
```

**Error Responses**:

- `404 Not Found`: Product not found

**curl Example**:

```bash
curl -X GET https://api.ecommerce.com/api/v1/products/prod-456
```

---

### Search Products

Advanced product search with full-text search capabilities.

**Endpoint**: `GET /api/v1/products/search`

**Authentication**: Not required

**Query Parameters**:

- `q`: Search query (required)
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)
- `filters`: JSON string of filters
  - Example: `{"category": "cat-123", "brand": "TechBrand"}`

**Success Response** (200 OK):

```json
{
  "data": {
    "query": "wireless mouse",
    "results": [
      {
        "id": "prod-456",
        "name": "Wireless Mouse",
        "description": "Ergonomic wireless mouse...",
        "price": 500.00,
        "images": ["https://cdn.ecommerce.com/products/prod-456-1.jpg"],
        "rating": 4.5,
        "inStock": true,
        "relevanceScore": 0.95
      }
    ],
    "suggestions": ["wireless keyboard", "bluetooth mouse"],
    "facets": {
      "categories": [
        {"id": "cat-123", "name": "Computer Accessories", "count": 45}
      ],
      "brands": [
        {"name": "TechBrand", "count": 23},
        {"name": "OfficePro", "count": 15}
      ],
      "priceRanges": [
        {"min": 0, "max": 500, "count": 30},
        {"min": 500, "max": 1000, "count": 15}
      ]
    },
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 45
    }
  }
}
```

**curl Example**:

```bash
curl -X GET "https://api.ecommerce.com/api/v1/products/search?q=wireless+mouse&page=0&size=20"
```

---

### Get Products by Category

Retrieve products in a specific category.

**Endpoint**: `GET /api/v1/products/category/{categoryId}`

**Authentication**: Not required

**Path Parameters**:

- `categoryId`: Category ID

**Query Parameters**:

- `page`, `size`, `sort`: Standard pagination parameters

**Success Response** (200 OK):

```json
{
  "data": {
    "category": {
      "id": "cat-123",
      "name": "Computer Accessories",
      "description": "Accessories for computers and laptops"
    },
    "products": {
      "content": [
        {
          "id": "prod-456",
          "name": "Wireless Mouse",
          "price": 500.00,
          "inStock": true
        }
      ],
      "page": {
        "number": 0,
        "size": 20,
        "totalElements": 45
      }
    }
  }
}
```

**curl Example**:

```bash
curl -X GET "https://api.ecommerce.com/api/v1/products/category/cat-123?page=0&size=20"
```

---

### Create Product

Create a new product (Admin/Seller only).

**Endpoint**: `POST /api/v1/products`

**Authentication**: Required

**Authorization**: ADMIN or SELLER role required

**Request Body**:

```json
{
  "sku": "WM-2025-002",
  "name": "Gaming Mouse Pro",
  "description": "Professional gaming mouse with RGB lighting",
  "price": 1200.00,
  "categoryId": "cat-123",
  "brand": "GamerTech",
  "specifications": {
    "color": "Black",
    "connectivity": "USB Wired",
    "dpi": "100-16000",
    "buttons": "8"
  },
  "images": [
    {
      "url": "https://cdn.ecommerce.com/products/new-product-1.jpg",
      "alt": "Gaming Mouse Pro",
      "isPrimary": true
    }
  ],
  "stockQuantity": 100,
  "lowStockThreshold": 10,
  "tags": ["gaming", "rgb", "wired"]
}
```

**Validation Rules**:

- `sku`: Required, unique
- `name`: Required, 2-200 characters
- `description`: Required, 10-5000 characters
- `price`: Required, must be > 0
- `categoryId`: Required, must exist
- `stockQuantity`: Required, must be >= 0

**Success Response** (201 Created):

```json
{
  "data": {
    "id": "prod-999",
    "sku": "WM-2025-002",
    "name": "Gaming Mouse Pro",
    "description": "Professional gaming mouse with RGB lighting",
    "price": 1200.00,
    "currency": "TWD",
    "category": {
      "id": "cat-123",
      "name": "Computer Accessories"
    },
    "brand": "GamerTech",
    "inStock": true,
    "stockQuantity": 100,
    "createdAt": "2025-10-25T14:00:00Z"
  }
}
```

**Error Responses**:

- `400 Bad Request`: Validation errors
- `401 Unauthorized`: Missing or invalid token
- `403 Forbidden`: Insufficient permissions
- `409 Conflict`: SKU already exists

**curl Example**:

```bash
curl -X POST https://api.ecommerce.com/api/v1/products \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "WM-2025-002",
    "name": "Gaming Mouse Pro",
    "description": "Professional gaming mouse with RGB lighting",
    "price": 1200.00,
    "categoryId": "cat-123",
    "brand": "GamerTech",
    "stockQuantity": 100
  }'
```

---

### Update Product

Update product information (Admin/Seller only).

**Endpoint**: `PUT /api/v1/products/{id}`

**Authentication**: Required

**Authorization**: ADMIN or SELLER role required

**Path Parameters**:

- `id`: Product ID

**Request Body**: Same as Create Product

**Success Response** (200 OK):

```json
{
  "data": {
    "id": "prod-456",
    "name": "Wireless Mouse Updated",
    "price": 550.00,
    "updatedAt": "2025-10-25T15:00:00Z"
  }
}
```

**curl Example**:

```bash
curl -X PUT https://api.ecommerce.com/api/v1/products/prod-456 \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Wireless Mouse Updated",
    "price": 550.00
  }'
```

---

### Update Product Stock

Update product inventory (Admin/Seller only).

**Endpoint**: `PATCH /api/v1/products/{id}/stock`

**Authentication**: Required

**Authorization**: ADMIN or SELLER role required

**Path Parameters**:

- `id`: Product ID

**Request Body**:

```json
{
  "quantity": 200,
  "operation": "SET"
}
```

**Operations**:

- `SET`: Set absolute quantity
- `ADD`: Add to current quantity
- `SUBTRACT`: Subtract from current quantity

**Success Response** (200 OK):

```json
{
  "data": {
    "id": "prod-456",
    "stockQuantity": 200,
    "inStock": true,
    "updatedAt": "2025-10-25T15:30:00Z"
  }
}
```

**curl Example**:

```bash
curl -X PATCH https://api.ecommerce.com/api/v1/products/prod-456/stock \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "quantity": 200,
    "operation": "SET"
  }'
```

---

### Delete Product

Delete a product (Admin only).

**Endpoint**: `DELETE /api/v1/products/{id}`

**Authentication**: Required

**Authorization**: ADMIN role required

**Path Parameters**:

- `id`: Product ID

**Success Response** (204 No Content)

**Error Responses**:

- `401 Unauthorized`: Missing or invalid token
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Product not found
- `409 Conflict`: Product has active orders

**curl Example**:

```bash
curl -X DELETE https://api.ecommerce.com/api/v1/products/prod-456 \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN"
```

---

### Get Product Reviews

Retrieve reviews for a product.

**Endpoint**: `GET /api/v1/products/{id}/reviews`

**Authentication**: Not required

**Path Parameters**:

- `id`: Product ID

**Query Parameters**:

- `page`, `size`: Standard pagination
- `sort`: Sort by `rating,desc`, `createdAt,desc`, `helpful,desc`
- `rating`: Filter by rating (1-5)

**Success Response** (200 OK):

```json
{
  "data": {
    "productId": "prod-456",
    "averageRating": 4.5,
    "totalReviews": 234,
    "ratingDistribution": {
      "5": 150,
      "4": 60,
      "3": 15,
      "2": 5,
      "1": 4
    },
    "reviews": {
      "content": [
        {
          "id": "review-123",
          "customerId": "cust-789",
          "customerName": "John D.",
          "rating": 5,
          "title": "Excellent mouse!",
          "comment": "Very comfortable and responsive. Highly recommended!",
          "verified": true,
          "helpful": 45,
          "createdAt": "2025-10-20T10:00:00Z"
        }
      ],
      "page": {
        "number": 0,
        "size": 20,
        "totalElements": 234
      }
    }
  }
}
```

**curl Example**:

```bash
curl -X GET "https://api.ecommerce.com/api/v1/products/prod-456/reviews?page=0&size=20&sort=helpful,desc"
```

---

### Get Related Products

Get products related to a specific product.

**Endpoint**: `GET /api/v1/products/{id}/related`

**Authentication**: Not required

**Path Parameters**:

- `id`: Product ID

**Query Parameters**:

- `limit`: Number of related products (default: 10, max: 20)

**Success Response** (200 OK):

```json
{
  "data": {
    "products": [
      {
        "id": "prod-789",
        "name": "Mechanical Keyboard",
        "price": 1500.00,
        "images": ["https://cdn.ecommerce.com/products/prod-789-1.jpg"],
        "rating": 4.7,
        "inStock": true
      }
    ]
  }
}
```

**curl Example**:

```bash
curl -X GET "https://api.ecommerce.com/api/v1/products/prod-456/related?limit=10"
```

---

## Data Models

### Product Object

```json
{
  "id": "string",
  "sku": "string",
  "name": "string",
  "description": "string",
  "price": "number",
  "originalPrice": "number",
  "discount": "number",
  "discountPercentage": "number",
  "currency": "string",
  "category": {
    "id": "string",
    "name": "string",
    "path": "string"
  },
  "brand": "string",
  "images": [
    {
      "url": "string",
      "alt": "string",
      "isPrimary": "boolean"
    }
  ],
  "specifications": "object",
  "inStock": "boolean",
  "stockQuantity": "number",
  "lowStockThreshold": "number",
  "rating": "number",
  "reviewCount": "number",
  "tags": ["string"],
  "relatedProducts": ["string"],
  "createdAt": "string (ISO 8601)",
  "updatedAt": "string (ISO 8601)"
}
```

### Product Status

| Status | Description |
|--------|-------------|
| In Stock | Available for purchase |
| Low Stock | Stock below threshold |
| Out of Stock | Not available |
| Discontinued | No longer sold |

## Search and Filtering

### Search Capabilities

1. **Full-Text Search**: Searches product name and description
2. **Faceted Search**: Filter by category, brand, price range
3. **Auto-Suggestions**: Real-time search suggestions
4. **Relevance Ranking**: Results sorted by relevance score

### Filter Options

- **Category**: Filter by product category
- **Brand**: Filter by brand name
- **Price Range**: Min and max price filters
- **Rating**: Filter by minimum rating
- **Availability**: In stock / out of stock
- **Tags**: Filter by product tags

## Business Rules

1. **Stock Management**: Automatic low stock alerts
2. **Price Updates**: Price history tracked
3. **Product Visibility**: Out of stock products still visible
4. **Review Verification**: Only verified purchases can review
5. **Image Requirements**: At least one primary image required
6. **SKU Uniqueness**: SKU must be unique across all products

## Error Codes

| Code | Description | Solution |
|------|-------------|----------|
| `PRODUCT_NOT_FOUND` | Product ID not found | Check product ID |
| `PRODUCT_SKU_EXISTS` | SKU already exists | Use different SKU |
| `PRODUCT_OUT_OF_STOCK` | Product not available | Check stock or wait |
| `PRODUCT_INVALID_PRICE` | Price must be positive | Provide valid price |
| `PRODUCT_INVALID_CATEGORY` | Category not found | Check category ID |

## Related Documentation

- [Order API](orders.md) - Order management
- [Inventory API](inventory.md) - Inventory management
- [Shopping Cart API](shopping-cart.md) - Cart operations
- [Authentication](../authentication.md) - Authentication and authorization

---

**Last Updated**: 2025-10-25  
**API Version**: v1
