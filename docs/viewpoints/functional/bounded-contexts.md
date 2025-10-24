---
title: "Bounded Contexts"
type: "functional-viewpoint"
category: "functional"
stakeholders: ["architects", "developers", "business-analysts"]
last_updated: "2025-10-22"
version: "1.0"
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
> **Last Updated**: 2025-10-22  
> **Owner**: Architecture Team

## Overview

This document describes all 13 bounded contexts in the Enterprise E-Commerce Platform. Each bounded context represents a distinct business capability with clear boundaries, its own domain model, and specific responsibilities.

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

## Bounded Contexts

### 1. Customer Context

**Responsibility**: Manage customer lifecycle, profiles, preferences, and membership

**Core Aggregate**: `Customer`

**Key Entities**:
- `Customer` (Aggregate Root)
- `CustomerPreferences`
- `DeliveryAddress`
- `PaymentMethod`

**Key Value Objects**:
- `CustomerId`
- `CustomerName`
- `Email`
- `Phone`
- `Address`
- `MembershipLevel` (STANDARD, SILVER, GOLD, PLATINUM)
- `CustomerStatus` (ACTIVE, INACTIVE, SUSPENDED)
- `RewardPoints`
- `NotificationPreferences`

**Domain Events Published**:
- `CustomerCreatedEvent`
- `CustomerProfileUpdatedEvent`
- `CustomerStatusChangedEvent`
- `MembershipLevelUpgradedEvent`
- `CustomerVipUpgradedEvent`
- `DeliveryAddressAddedEvent`
- `DeliveryAddressRemovedEvent`
- `NotificationPreferencesUpdatedEvent`
- `RewardPointsEarnedEvent`
- `RewardPointsRedeemedEvent`
- `CustomerSpendingUpdatedEvent`

**Domain Events Consumed**:
- `OrderCompletedEvent` (from Order Context) → Update spending, reward points
- `PaymentCompletedEvent` (from Payment Context) → Update customer statistics

**REST API Endpoints**:
- `POST /api/v1/customers` - Register new customer
- `GET /api/v1/customers/{id}` - Get customer details
- `PUT /api/v1/customers/{id}` - Update customer profile
- `POST /api/v1/customers/{id}/addresses` - Add delivery address
- `PUT /api/v1/customers/{id}/preferences` - Update preferences
- `GET /api/v1/customers/{id}/reward-points` - Get reward points balance

**Business Rules**:
- Email must be unique across all customers
- Membership level upgrades based on spending thresholds
- Reward points earned: 1 point per $10 spent
- VIP status requires PLATINUM membership + $10,000 annual spending

---

### 2. Order Context

**Responsibility**: Manage order lifecycle from creation to completion

**Core Aggregate**: `Order`

**Key Entities**:
- `Order` (Aggregate Root)
- `OrderItem`
- `OrderHistory`

**Key Value Objects**:
- `OrderId`
- `OrderStatus` (CREATED, PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)
- `OrderItem`
- `ShippingAddress`
- `Money`
- `Quantity`

**Domain Events Published**:
- `OrderCreatedEvent`
- `OrderSubmittedEvent`
- `OrderConfirmedEvent`
- `OrderCancelledEvent`
- `OrderShippedEvent`
- `OrderDeliveredEvent`
- `OrderCompletedEvent`
- `OrderItemAddedEvent`
- `OrderItemRemovedEvent`

**Domain Events Consumed**:
- `InventoryReservedEvent` (from Inventory Context) → Confirm order
- `PaymentCompletedEvent` (from Payment Context) → Proceed with fulfillment
- `PaymentFailedEvent` (from Payment Context) → Cancel order
- `DeliveryScheduledEvent` (from Delivery Context) → Update order status

**REST API Endpoints**:
- `POST /api/v1/orders` - Create new order
- `GET /api/v1/orders/{id}` - Get order details
- `POST /api/v1/orders/{id}/submit` - Submit order for processing
- `POST /api/v1/orders/{id}/cancel` - Cancel order
- `GET /api/v1/orders?customerId={id}` - List customer orders
- `GET /api/v1/orders/{id}/history` - Get order history

**Business Rules**:
- Order must have at least one item
- Order can only be cancelled if status is CREATED or PENDING
- Total amount must match sum of item prices plus shipping
- Order confirmation requires successful inventory reservation and payment

---

### 3. Product Context

**Responsibility**: Manage product catalog, categories, and product information

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

**Domain Events Published**:
- `ProductCreatedEvent`
- `ProductUpdatedEvent`
- `ProductPriceChangedEvent`
- `ProductStatusChangedEvent`
- `ProductDiscontinuedEvent`

**Domain Events Consumed**:
- `ReviewSubmittedEvent` (from Review Context) → Update product rating
- `InventoryDepletedEvent` (from Inventory Context) → Mark as out of stock

**REST API Endpoints**:
- `GET /api/v1/products` - List products with filtering
- `GET /api/v1/products/{id}` - Get product details
- `POST /api/v1/products` - Create new product (admin)
- `PUT /api/v1/products/{id}` - Update product (admin)
- `GET /api/v1/products/search?q={query}` - Search products
- `GET /api/v1/products/categories` - List categories

**Business Rules**:
- SKU must be unique across all products
- Price must be positive
- Product cannot be deleted if referenced in active orders
- Discontinued products cannot be added to new orders

---

### 4. Inventory Context

**Responsibility**: Manage product stock levels and inventory operations

**Core Aggregate**: `InventoryItem`

**Key Entities**:
- `InventoryItem` (Aggregate Root)
- `StockMovement`
- `Reservation`

**Key Value Objects**:
- `InventoryItemId`
- `ProductId`
- `Quantity`
- `ReservationId`
- `WarehouseLocation`

**Domain Events Published**:
- `InventoryReservedEvent`
- `InventoryReleasedEvent`
- `InventoryReplenishedEvent`
- `InventoryDepletedEvent`
- `StockLevelChangedEvent`

**Domain Events Consumed**:
- `OrderSubmittedEvent` (from Order Context) → Reserve inventory
- `OrderCancelledEvent` (from Order Context) → Release reservation
- `OrderDeliveredEvent` (from Order Context) → Commit reservation

**REST API Endpoints**:
- `GET /api/v1/inventory/{productId}` - Get stock level
- `POST /api/v1/inventory/{productId}/replenish` - Add stock (admin)
- `GET /api/v1/inventory/low-stock` - List low stock items (admin)

**Business Rules**:
- Stock level cannot be negative
- Reservations expire after 15 minutes if order not confirmed
- Low stock alert when quantity < 10
- Automatic reorder when quantity < 5

---

### 5. Payment Context

**Responsibility**: Process payments and manage payment transactions

**Core Aggregate**: `Payment`

**Key Entities**:
- `Payment` (Aggregate Root)
- `PaymentTransaction`
- `Refund`

**Key Value Objects**:
- `PaymentId`
- `OrderId`
- `Money`
- `PaymentMethod` (CREDIT_CARD, DEBIT_CARD, PAYPAL, BANK_TRANSFER)
- `PaymentStatus` (PENDING, COMPLETED, FAILED, REFUNDED)
- `TransactionId`

**Domain Events Published**:
- `PaymentInitiatedEvent`
- `PaymentCompletedEvent`
- `PaymentFailedEvent`
- `RefundProcessedEvent`

**Domain Events Consumed**:
- `OrderSubmittedEvent` (from Order Context) → Initiate payment
- `OrderCancelledEvent` (from Order Context) → Process refund

**REST API Endpoints**:
- `POST /api/v1/payments` - Initiate payment
- `GET /api/v1/payments/{id}` - Get payment status
- `POST /api/v1/payments/{id}/refund` - Process refund (admin)

**Business Rules**:
- Payment amount must match order total
- Failed payments trigger automatic retry (max 3 attempts)
- Refunds can only be processed for completed payments
- Partial refunds allowed for order cancellations

---

### 6. Delivery Context

**Responsibility**: Manage order delivery and logistics

**Core Aggregate**: `Delivery`

**Key Entities**:
- `Delivery` (Aggregate Root)
- `DeliveryRoute`
- `TrackingEvent`

**Key Value Objects**:
- `DeliveryId`
- `OrderId`
- `TrackingNumber`
- `DeliveryStatus` (SCHEDULED, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, FAILED)
- `DeliveryAddress`
- `EstimatedDeliveryDate`

**Domain Events Published**:
- `DeliveryScheduledEvent`
- `DeliveryDispatchedEvent`
- `DeliveryInTransitEvent`
- `DeliveryDeliveredEvent`
- `DeliveryFailedEvent`

**Domain Events Consumed**:
- `OrderConfirmedEvent` (from Order Context) → Schedule delivery
- `PaymentCompletedEvent` (from Payment Context) → Confirm delivery

**REST API Endpoints**:
- `GET /api/v1/deliveries/{orderId}` - Get delivery status
- `GET /api/v1/deliveries/track/{trackingNumber}` - Track delivery
- `POST /api/v1/deliveries/{id}/update-status` - Update status (logistics)

**Business Rules**:
- Delivery can only be scheduled for confirmed orders
- Tracking number must be unique
- Estimated delivery: 3-5 business days for standard shipping
- Failed deliveries trigger customer notification

---

### 7. Promotion Context

**Responsibility**: Manage promotional campaigns and discount rules

**Core Aggregate**: `Promotion`

**Key Entities**:
- `Promotion` (Aggregate Root)
- `PromotionRule`
- `DiscountCoupon`

**Key Value Objects**:
- `PromotionId`
- `PromotionType` (PERCENTAGE, FIXED_AMOUNT, BUY_X_GET_Y)
- `DiscountRate`
- `ValidityPeriod`
- `CouponCode`

**Domain Events Published**:
- `PromotionCreatedEvent`
- `PromotionActivatedEvent`
- `PromotionExpiredEvent`
- `CouponAppliedEvent`
- `CouponRedeemedEvent`

**Domain Events Consumed**:
- `OrderSubmittedEvent` (from Order Context) → Apply promotions

**REST API Endpoints**:
- `GET /api/v1/promotions/active` - List active promotions
- `POST /api/v1/promotions/validate-coupon` - Validate coupon code
- `POST /api/v1/promotions` - Create promotion (admin)

**Business Rules**:
- Promotions have start and end dates
- Coupons can have usage limits (per customer or total)
- Multiple promotions can be combined unless explicitly restricted
- Expired promotions cannot be applied

---

### 8. Notification Context

**Responsibility**: Send notifications to customers via multiple channels

**Core Aggregate**: `Notification`

**Key Entities**:
- `Notification` (Aggregate Root)
- `NotificationTemplate`

**Key Value Objects**:
- `NotificationId`
- `NotificationType` (EMAIL, SMS, PUSH)
- `NotificationStatus` (PENDING, SENT, FAILED)
- `RecipientId`

**Domain Events Published**:
- `NotificationSentEvent`
- `NotificationFailedEvent`

**Domain Events Consumed**:
- `CustomerCreatedEvent` → Send welcome email
- `OrderConfirmedEvent` → Send order confirmation
- `OrderShippedEvent` → Send shipping notification
- `PaymentCompletedEvent` → Send payment receipt
- `DeliveryDeliveredEvent` → Send delivery confirmation

**REST API Endpoints**:
- `GET /api/v1/notifications/{customerId}` - Get customer notifications
- `POST /api/v1/notifications/send` - Send notification (internal)

**Business Rules**:
- Respect customer notification preferences
- Failed notifications retry up to 3 times
- Email notifications include unsubscribe link
- Critical notifications (payment, security) cannot be disabled

---

### 9. Review Context

**Responsibility**: Manage product reviews and ratings

**Core Aggregate**: `Review`

**Key Entities**:
- `Review` (Aggregate Root)
- `ReviewComment`

**Key Value Objects**:
- `ReviewId`
- `ProductId`
- `CustomerId`
- `Rating` (1-5 stars)
- `ReviewStatus` (PENDING, APPROVED, REJECTED)

**Domain Events Published**:
- `ReviewSubmittedEvent`
- `ReviewApprovedEvent`
- `ReviewRejectedEvent`
- `ReviewUpdatedEvent`

**Domain Events Consumed**:
- `OrderDeliveredEvent` (from Order Context) → Enable review submission

**REST API Endpoints**:
- `GET /api/v1/reviews?productId={id}` - Get product reviews
- `POST /api/v1/reviews` - Submit review
- `PUT /api/v1/reviews/{id}` - Update review
- `POST /api/v1/reviews/{id}/approve` - Approve review (admin)

**Business Rules**:
- Customer can only review products they purchased
- One review per customer per product
- Reviews require moderation before publication
- Rating must be between 1 and 5 stars

---

### 10. Shopping Cart Context

**Responsibility**: Manage customer shopping carts and cart operations

**Core Aggregate**: `ShoppingCart`

**Key Entities**:
- `ShoppingCart` (Aggregate Root)
- `CartItem`

**Key Value Objects**:
- `CartId`
- `CustomerId`
- `ProductId`
- `Quantity`
- `CartStatus` (ACTIVE, ABANDONED, CONVERTED)

**Domain Events Published**:
- `CartCreatedEvent`
- `ItemAddedToCartEvent`
- `ItemRemovedFromCartEvent`
- `CartAbandonedEvent`
- `CartConvertedToOrderEvent`

**Domain Events Consumed**:
- `ProductPriceChangedEvent` (from Product Context) → Update cart prices
- `InventoryDepletedEvent` (from Inventory Context) → Remove unavailable items

**REST API Endpoints**:
- `GET /api/v1/carts/{customerId}` - Get customer cart
- `POST /api/v1/carts/{customerId}/items` - Add item to cart
- `DELETE /api/v1/carts/{customerId}/items/{productId}` - Remove item
- `PUT /api/v1/carts/{customerId}/items/{productId}` - Update quantity
- `POST /api/v1/carts/{customerId}/checkout` - Convert cart to order

**Business Rules**:
- Cart items expire after 24 hours
- Quantity cannot exceed available inventory
- Cart total recalculated when prices change
- Abandoned carts trigger reminder email after 1 hour

---

### 11. Pricing Context

**Responsibility**: Calculate prices with discounts, taxes, and shipping

**Core Aggregate**: `PriceCalculation`

**Key Entities**:
- `PriceCalculation` (Aggregate Root)
- `PricingRule`

**Key Value Objects**:
- `BasePrice`
- `DiscountAmount`
- `TaxAmount`
- `ShippingCost`
- `FinalPrice`
- `PricingStrategy`

**Domain Events Published**:
- `PriceCalculatedEvent`
- `PricingRuleAppliedEvent`

**Domain Events Consumed**:
- `PromotionActivatedEvent` (from Promotion Context) → Update pricing rules
- `OrderSubmittedEvent` (from Order Context) → Calculate final price

**REST API Endpoints**:
- `POST /api/v1/pricing/calculate` - Calculate price for order
- `GET /api/v1/pricing/shipping-cost` - Get shipping cost estimate

**Business Rules**:
- Tax rate varies by delivery location
- Free shipping for orders > $100
- Volume discounts for bulk purchases
- Member discounts based on membership level

---

### 12. Seller Context

**Responsibility**: Manage seller accounts and seller operations

**Core Aggregate**: `Seller`

**Key Entities**:
- `Seller` (Aggregate Root)
- `SellerProfile`
- `SellerRating`

**Key Value Objects**:
- `SellerId`
- `SellerName`
- `SellerStatus` (ACTIVE, SUSPENDED, INACTIVE)
- `CommissionRate`

**Domain Events Published**:
- `SellerRegisteredEvent`
- `SellerApprovedEvent`
- `SellerSuspendedEvent`
- `SellerRatingUpdatedEvent`

**Domain Events Consumed**:
- `OrderCompletedEvent` (from Order Context) → Update seller statistics
- `ReviewSubmittedEvent` (from Review Context) → Update seller rating

**REST API Endpoints**:
- `POST /api/v1/sellers` - Register seller
- `GET /api/v1/sellers/{id}` - Get seller profile
- `PUT /api/v1/sellers/{id}` - Update seller profile
- `GET /api/v1/sellers/{id}/products` - List seller products

**Business Rules**:
- Sellers must be approved before listing products
- Commission rate: 10-20% based on seller tier
- Suspended sellers cannot list new products
- Seller rating based on customer reviews and order fulfillment

---

### 13. Observability Context (Cross-Cutting)

**Responsibility**: Collect and aggregate system metrics, logs, and traces

**Core Aggregate**: `MetricRecord`

**Key Entities**:
- `MetricRecord` (Aggregate Root)
- `LogEntry`
- `TraceSpan`

**Key Value Objects**:
- `MetricName`
- `MetricValue`
- `Timestamp`
- `TraceId`
- `SpanId`

**Domain Events Published**:
- `MetricRecordedEvent`
- `AlertTriggeredEvent`

**Domain Events Consumed**:
- All domain events from all contexts → Record metrics

**REST API Endpoints**:
- `GET /api/v1/metrics` - Get system metrics (admin)
- `GET /api/v1/health` - Health check endpoint

**Business Rules**:
- Metrics retained for 90 days
- Logs retained for 30 days
- Traces retained for 7 days
- Alerts triggered based on threshold rules

---

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
| 2025-10-22 | 1.0 | Architecture Team | Initial version with all 13 bounded contexts |

---

**Document Version**: 1.0  
**Last Updated**: 2025-10-22
