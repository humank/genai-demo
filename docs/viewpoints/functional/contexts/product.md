---
title: "Product Context"
type: "functional-viewpoint"
category: "functional"
status: "active"
last_updated: "2025-10-22"
owner: "Architecture Team"
---

# Product Context

> **Responsibility**: Manage product catalog, categories, and product information

## Overview

The Product Context manages the catalog of items available for sale. It handles product details, categorization, specifications, and status. It serves as the source of product information for the entire platform.

## Domain Model

**Core Aggregate**: `Product`

**Key Entities**:
- `Product` (Aggregate Root)
- `ProductCategory`
- `ProductSpecification`

**Key Value Objects**:
- `ProductId`
- `ProductName`
- `ProductDescription`
- `Price`
- `SKU`
- `ProductStatus` (ACTIVE, INACTIVE, DISCONTINUED)
- `CategoryId`

### Domain Model Diagram

```mermaid
classDiagram
    class Product {
        +ProductId id
        +ProductName name
        +ProductDescription description
        +Price price
        +SKU sku
        +ProductStatus status
        +updatePrice()
        +discontinue()
    }
    class ProductCategory {
        +CategoryId id
        +String name
        +String parentId
    }
    class ProductSpecification {
        +String key
        +String value
    }

    Product "*" --> "1" ProductCategory
    Product "1" --> "*" ProductSpecification
```

## Events

### Event Flow

```mermaid
sequenceDiagram
    participant A as Admin
    participant P as Product Context
    participant C as Shopping Cart Context

    A->>P: Update Price
    P->>P: Price Changed
    P-->>C: ProductPriceChangedEvent

    C->>C: Recalculate Cart Totals
```

**Domain Events Published**:
- `ProductCreatedEvent`
- `ProductUpdatedEvent`
- `ProductPriceChangedEvent`
- `ProductStatusChangedEvent`
- `ProductDiscontinuedEvent`

**Domain Events Consumed**:
- `ReviewSubmittedEvent` (from Review Context) → Update product rating
- `InventoryDepletedEvent` (from Inventory Context) → Mark as out of stock

## API Interface

**REST API Endpoints**:
- `GET /api/v1/products` - List products with filtering
- `GET /api/v1/products/{id}` - Get product details
- `POST /api/v1/products` - Create new product (admin)
- `PUT /api/v1/products/{id}` - Update product (admin)
- `GET /api/v1/products/search?q={query}` - Search products
- `GET /api/v1/products/categories` - List categories

## Business Rules

- SKU must be unique across all products
- Price must be positive
- Product cannot be deleted if referenced in active orders
- Discontinued products cannot be added to new orders
