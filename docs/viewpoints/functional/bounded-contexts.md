---
title: "Bounded Contexts"
type: "functional-viewpoint"
category: "functional"
stakeholders: ["architects", "developers", "business-analysts"]
last_updated: "2025-11-25"
version: "2.0"
status: "active"
owner: "Architecture Team"
related_docs:
  - "viewpoints/functional/overview.md"
  - "viewpoints/information/overview.md"
  - "perspectives/evolution/overview.md"
tags: ["ddd", "bounded-contexts", "domain-model", "context-map"]
---

# Bounded Contexts

> **Status**: ✅ Active
> **Last Updated**: 2025-11-25
> **Owner**: Architecture Team

## Overview

This document serves as an index for all 13 bounded contexts in the Enterprise E-Commerce Platform. Each bounded context represents a distinct business capability with clear boundaries, its own domain model, and specific responsibilities.

The contexts are organized following **Domain-Driven Design (DDD)** strategic design principles, with each context being independently deployable and maintainable.

## Context Map

![Bounded Contexts Overview](../../diagrams/generated/functional/bounded-contexts-overview.png)

### Context Relationships

- **Customer ↔ Order**: Customer places orders (via events)
- **Order ↔ Inventory**: Order reserves inventory (via events)
- **Order ↔ Payment**: Order triggers payment processing (via events)
- **Order ↔ Delivery**: Order initiates delivery (via events)
- **Order ↔ Pricing**: Order calculates prices (via service call)
- **Product ↔ Inventory**: Product stock managed by inventory (via events)
- **Shopping Cart ↔ Product**: Cart contains products (via service call)
- **Promotion ↔ Pricing**: Promotions affect pricing (via events)
- **Review ↔ Product**: Reviews associated with products (via events)
- **Notification**: Listens to events from all contexts

## Context Index

### Core Business Contexts

| Context | Responsibility | Documentation |
|---------|----------------|---------------|
| **[Customer](contexts/customer.md)** | Manage customer lifecycle, profiles, preferences, and membership | [View Details](contexts/customer.md) |
| **[Order](contexts/order.md)** | Manage order lifecycle from creation to completion | [View Details](contexts/order.md) |
| **[Product](contexts/product.md)** | Manage product catalog, categories, and product information | [View Details](contexts/product.md) |
| **[Inventory](contexts/inventory.md)** | Manage product stock levels and inventory operations | [View Details](contexts/inventory.md) |
| **[Payment](contexts/payment.md)** | Process payments and manage payment transactions | [View Details](contexts/payment.md) |
| **[Delivery](contexts/delivery.md)** | Manage order delivery and logistics | [View Details](contexts/delivery.md) |

### Supporting Contexts

| Context | Responsibility | Documentation |
|---------|----------------|---------------|
| **[Promotion](contexts/promotion.md)** | Manage promotional campaigns and discount rules | [View Details](contexts/promotion.md) |
| **[Notification](contexts/notification.md)** | Send notifications to customers via multiple channels | [View Details](contexts/notification.md) |
| **[Review](contexts/review.md)** | Manage product reviews and ratings | [View Details](contexts/review.md) |
| **[Shopping Cart](contexts/shopping-cart.md)** | Manage customer shopping carts and cart operations | [View Details](contexts/shopping-cart.md) |
| **[Pricing](contexts/pricing.md)** | Calculate prices with discounts, taxes, and shipping | [View Details](contexts/pricing.md) |
| **[Seller](contexts/seller.md)** | Manage seller accounts and seller operations | [View Details](contexts/seller.md) |

### Cross-Cutting Contexts

| Context | Responsibility | Documentation |
|---------|----------------|---------------|
| **[Observability](contexts/observability.md)** | Collect and aggregate system metrics, logs, and traces | [View Details](contexts/observability.md) |

## Context Integration Patterns

### Synchronous Communication (REST API)

Used for real-time queries where immediate response is required:

- Shopping Cart → Product (get product details)
- Order → Pricing (calculate order total)
- Customer → Order (query order status)

### Asynchronous Communication (Domain Events)

Used for cross-context workflows and eventual consistency:

- Order → Inventory (reserve stock)
- Order → Payment (process payment)
- Order → Delivery (schedule delivery)
- All contexts → Notification (send notifications)

### Shared Kernel

Minimal shared value objects in `domain/shared/`:

- `Money`
- `CustomerId`
- `ProductId`
- `OrderId`

## Context Boundaries

### Clear Ownership

- Each context owns its data exclusively
- No direct database access between contexts
- Each context has its own database schema/tables

### Anti-Corruption Layer

- Each context translates external data to its own domain model
- External IDs are wrapped in context-specific value objects
- External events are transformed to internal domain events

## Evolution Strategy

### Adding New Contexts

1. Identify new business capability
2. Define context boundaries and responsibilities
3. Design domain model and events
4. Implement infrastructure
5. Integrate with existing contexts via events

### Splitting Contexts

1. Identify subdomain within existing context
2. Extract domain model and events
3. Create new bounded context
4. Migrate data and update integrations
5. Deprecate old context gradually

### Merging Contexts

1. Identify overlapping responsibilities
2. Design unified domain model
3. Merge events and APIs
4. Migrate data from both contexts
5. Update all integrations

## Quick Links

- [Back to Functional Viewpoint](overview.md)
- [Use Cases](use-cases.md)
- [Functional Interfaces](interfaces.md)
- [Information Viewpoint](../information/overview.md)
- [Main Documentation](../../README.md)

## Change History

| Date | Version | Author | Changes |
|------|---------|--------|---------|
| 2025-11-25 | 2.0 | Architecture Team | Refactored into individual context files |
| 2025-10-22 | 1.0 | Architecture Team | Initial version with all 13 bounded contexts |
