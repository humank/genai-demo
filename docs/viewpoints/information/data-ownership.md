---
title: "Data Ownership"
viewpoint: "Information"
status: "Active"
last_updated: "2025-10-23"
related_documents:
  - "overview.md"
  - "domain-models.md"
  - "data-flow.md"
---

# Data Ownership

## Overview

This document defines which bounded context owns which data in the system. Clear data ownership is essential for maintaining consistency, avoiding conflicts, and enabling independent evolution of bounded contexts.

## Ownership Principles

### 1. Single Source of Truth

Each piece of data has exactly one authoritative source:

- **One Owner**: Only one bounded context can create, update, or delete a piece of data
- **Read-Only Access**: Other contexts can read data via events or APIs, but cannot modify it
- **No Shared Tables**: Contexts do not share database tables or schemas

### 2. Bounded Context Autonomy

Each context is responsible for its own data:

- **Independent Databases**: Each context has its own database schema
- **No Direct Database Access**: Contexts do not query other contexts' databases
- **Event-Driven Integration**: Contexts communicate via domain events
- **Data Duplication**: Contexts may cache data from other contexts for performance

### 3. Eventual Consistency

Cross-context consistency is achieved asynchronously:

- **Domain Events**: State changes are communicated via events
- **Event Handlers**: Contexts update their local data based on events
- **Compensation**: Failed operations are compensated, not rolled back
- **Idempotency**: Event handlers are idempotent to handle duplicate events

## Data Ownership Matrix

| Data Type | Owner Context | Read Access | Write Access | Notes |
|-----------|--------------|-------------|--------------|-------|
| **Customer Profile** | Customer | All contexts | Customer only | Other contexts cache customer name/email |
| **Customer Address** | Customer | Order, Shipping | Customer only | Shipping context caches for delivery |
| **Customer Preferences** | Customer | Notification, Promotion | Customer only | Used for personalization |
| **Order Details** | Order | Customer, Shipping, Payment | Order only | Order status shared via events |
| **Order Items** | Order | Inventory, Shipping | Order only | Product details cached from Product context |
| **Product Catalog** | Product | All contexts | Product, Seller | Other contexts cache product name/price |
| **Product Specifications** | Product | Review, Order | Product only | Cached for display purposes |
| **Inventory Levels** | Inventory | Product, Order | Inventory only | Stock levels shared via events |
| **Inventory Reservations** | Inventory | Order | Inventory only | Created when order is submitted |
| **Payment Transactions** | Payment | Order, Customer | Payment only | Payment status shared via events |
| **Payment Methods** | Payment | Customer | Payment only | Stored securely in Payment context |
| **Shipment Tracking** | Shipping | Order, Customer | Shipping only | Tracking updates shared via events |
| **Delivery Routes** | Delivery | Shipping | Delivery only | Optimized routes for delivery |
| **Promotions** | Promotion | Order, Pricing | Promotion only | Active promotions shared via events |
| **Coupon Codes** | Promotion | Order | Promotion only | Validated during order submission |
| **Product Reviews** | Review | Product, Customer | Review only | Review summaries shared via events |
| **Review Ratings** | Review | Product | Review only | Aggregated ratings shared via events |
| **Shopping Cart** | Shopping Cart | Customer, Order | Shopping Cart only | Converted to order on checkout |
| **Cart Items** | Shopping Cart | Product | Shopping Cart only | Product details cached |
| **Pricing Rules** | Pricing | Order, Product | Pricing only | Price calculations on demand |
| **Seller Profiles** | Seller | Product, Order | Seller only | Seller info cached in Product context |
| **Notification Templates** | Notification | All contexts | Notification only | Templates used for all notifications |
| **Notification Logs** | Notification | Customer | Notification only | Audit trail of sent notifications |

## Context-Specific Ownership Details

### Customer Context

**Owns**:
- Customer profiles (name, email, phone)
- Customer addresses (billing, shipping)
- Customer preferences (language, currency, notifications)
- Customer membership levels
- Customer authentication credentials

**Responsibilities**:
- Validate customer information
- Manage customer lifecycle (registration, suspension, deletion)
- Publish customer events for other contexts
- Maintain customer audit trail

**Data Shared Via Events**:
- `CustomerRegisteredEvent` → All contexts
- `CustomerProfileUpdatedEvent` → Order, Notification
- `CustomerAddressAddedEvent` → Order, Shipping
- `CustomerMembershipUpgradedEvent` → Promotion, Pricing

**Data Cached by Other Contexts**:
- Customer name (Order, Review, Notification)
- Customer email (Notification, Payment)
- Shipping address (Order, Shipping)

---

### Order Context

**Owns**:
- Order details (order date, status, totals)
- Order items (product references, quantities, prices)
- Order addresses (shipping, billing - snapshots)
- Order history and audit trail

**Responsibilities**:
- Validate order business rules
- Calculate order totals
- Manage order lifecycle (creation, submission, fulfillment, cancellation)
- Coordinate order processing workflow
- Publish order events for other contexts

**Data Shared Via Events**:
- `OrderCreatedEvent` → Customer, Inventory
- `OrderSubmittedEvent` → Inventory, Payment, Notification
- `OrderConfirmedEvent` → Shipping, Customer
- `OrderCancelledEvent` → Inventory, Payment, Customer

**Data Cached from Other Contexts**:
- Product name and price (from Product context)
- Customer name and email (from Customer context)
- Promotion details (from Promotion context)

**Data Synchronization**:
- Product prices are snapshotted at order creation time
- Customer addresses are snapshotted at order submission time
- Inventory reservations are created via events

---

### Product Context

**Owns**:
- Product catalog (name, description, category)
- Product specifications and attributes
- Product images and media
- Product pricing (base prices)
- Product status (active, discontinued)

**Responsibilities**:
- Maintain product catalog
- Validate product information
- Manage product lifecycle
- Publish product events for other contexts
- Provide product search and filtering

**Data Shared Via Events**:
- `ProductCreatedEvent` → Inventory, Pricing
- `ProductUpdatedEvent` → Order, Shopping Cart
- `ProductPriceChangedEvent` → Pricing, Order
- `ProductDiscontinuedEvent` → Inventory, Order

**Data Cached by Other Contexts**:
- Product name (Order, Shopping Cart, Review)
- Product price (Order, Shopping Cart, Pricing)
- Product images (Order, Shopping Cart)

**Data Synchronization**:
- Product availability depends on Inventory context
- Product reviews are managed by Review context
- Product pricing rules are managed by Pricing context

---

### Inventory Context

**Owns**:
- Inventory levels (on-hand, reserved, available)
- Inventory reservations (for pending orders)
- Warehouse locations
- Reorder points and quantities
- Inventory movements (receipts, adjustments)

**Responsibilities**:
- Track stock levels across warehouses
- Reserve inventory for orders
- Release expired reservations
- Trigger low stock alerts
- Manage inventory replenishment

**Data Shared Via Events**:
- `InventoryReservedEvent` → Order
- `InventoryReservationExpiredEvent` → Order
- `InventoryFulfilledEvent` → Order, Shipping
- `LowStockAlertEvent` → Product, Seller

**Data Cached by Other Contexts**:
- Available quantity (Product context for display)
- Reservation status (Order context for tracking)

**Data Synchronization**:
- Reservations are created when orders are submitted
- Reservations expire after 15 minutes if not fulfilled
- Inventory is decremented when orders are shipped

---

### Payment Context

**Owns**:
- Payment transactions (authorizations, captures, refunds)
- Payment methods (credit cards, PayPal, etc.)
- Payment status and history
- Payment gateway integration details
- Sensitive payment data (PCI-compliant storage)

**Responsibilities**:
- Process payments securely
- Manage payment methods
- Handle payment failures and retries
- Integrate with payment gateways
- Maintain PCI-DSS compliance

**Data Shared Via Events**:
- `PaymentAuthorizedEvent` → Order
- `PaymentCapturedEvent` → Order, Customer
- `PaymentFailedEvent` → Order, Notification
- `PaymentRefundedEvent` → Order, Customer

**Data Cached by Other Contexts**:
- Payment status (Order context)
- Last 4 digits of card (Customer context for display)

**Data Synchronization**:
- Payment is initiated when order is submitted
- Payment is captured when order is confirmed
- Refunds are processed when orders are cancelled

**Security Considerations**:
- Full card numbers are never stored
- Payment tokens are used for recurring payments
- All payment data is encrypted at rest and in transit

---

### Shopping Cart Context

**Owns**:
- Active shopping carts
- Cart items (product references, quantities)
- Cart expiration and abandonment tracking
- Cart conversion to orders

**Responsibilities**:
- Manage cart lifecycle
- Calculate cart totals
- Handle cart abandonment
- Convert carts to orders
- Track cart analytics

**Data Shared Via Events**:
- `ItemAddedToCartEvent` → Product, Pricing
- `CartAbandonedEvent` → Notification, Promotion
- `CartConvertedToOrderEvent` → Order

**Data Cached from Other Contexts**:
- Product name and price (from Product context)
- Customer information (from Customer context)
- Promotion details (from Promotion context)

**Data Synchronization**:
- Product prices are refreshed on cart load
- Carts expire after 7 days of inactivity
- Cart is deleted after conversion to order

---

### Promotion Context

**Owns**:
- Promotion campaigns and rules
- Discount calculations
- Coupon codes and usage tracking
- Promotion eligibility rules
- Promotion analytics

**Responsibilities**:
- Define promotion rules
- Validate coupon codes
- Calculate discounts
- Track promotion usage
- Manage promotion lifecycle

**Data Shared Via Events**:
- `PromotionActivatedEvent` → Order, Pricing
- `PromotionExpiredEvent` → Order, Pricing
- `PromotionAppliedEvent` → Order, Customer

**Data Cached by Other Contexts**:
- Active promotions (Order, Pricing contexts)
- Coupon code validity (Order context)

**Data Synchronization**:
- Promotions are applied during order submission
- Usage counts are updated when promotions are applied
- Expired promotions are automatically deactivated

---

### Review Context

**Owns**:
- Product reviews and ratings
- Review comments and replies
- Review moderation status
- Review images and media
- Review analytics (average ratings, counts)

**Responsibilities**:
- Manage review lifecycle
- Moderate reviews
- Calculate aggregate ratings
- Publish review events
- Prevent review fraud

**Data Shared Via Events**:
- `ReviewSubmittedEvent` → Product, Customer
- `ReviewApprovedEvent` → Product
- `ReviewRejectedEvent` → Customer

**Data Cached by Other Contexts**:
- Average rating (Product context)
- Review count (Product context)

**Data Synchronization**:
- Reviews are linked to verified purchases
- Aggregate ratings are updated when reviews are approved
- Review summaries are cached in Product context

---

### Notification Context

**Owns**:
- Notification templates
- Notification logs and history
- Notification preferences
- Delivery status tracking
- Notification analytics

**Responsibilities**:
- Send notifications (email, SMS, push)
- Manage notification templates
- Track delivery status
- Handle notification failures
- Respect user preferences

**Data Shared Via Events**:
- `NotificationSentEvent` → Customer
- `NotificationFailedEvent` → Customer

**Data Cached from Other Contexts**:
- Customer email and phone (from Customer context)
- Customer notification preferences (from Customer context)
- Order details (from Order context)

**Data Synchronization**:
- Notifications are triggered by events from other contexts
- Delivery status is tracked asynchronously
- Failed notifications are retried with exponential backoff

---

## Data Duplication Strategy

### When to Duplicate Data

Data duplication is acceptable when:

1. **Performance**: Avoiding cross-context queries improves response time
2. **Availability**: Local data ensures context remains available if others fail
3. **Autonomy**: Contexts can operate independently
4. **Read-Heavy**: Data is read frequently but updated rarely

### What to Duplicate

Commonly duplicated data:

- **Reference Data**: Customer name, product name (for display)
- **Snapshots**: Order items with product details at order time
- **Aggregates**: Review ratings, order counts (for analytics)
- **Lookup Data**: Category names, status labels

### What NOT to Duplicate

Never duplicate:

- **Sensitive Data**: Payment details, passwords
- **Frequently Changing Data**: Inventory levels, prices (use events instead)
- **Large Data**: Product images, documents (use references)
- **Transactional Data**: Payment transactions, order history

### Synchronization Patterns

#### 1. Event-Driven Synchronization

```
Context A → State Change → Domain Event → Context B Event Handler → Update Local Cache
```

- **Use Case**: Product price changes, customer profile updates
- **Consistency**: Eventual consistency
- **Latency**: Seconds to minutes

#### 2. Snapshot Pattern

```
Context A → Create Snapshot → Store in Context B → Never Update
```

- **Use Case**: Order items with product details
- **Consistency**: Point-in-time consistency
- **Latency**: Immediate (no synchronization needed)

#### 3. Query Pattern

```
Context A → Query Context B API → Get Latest Data → Cache Locally (optional)
```

- **Use Case**: Real-time product availability check
- **Consistency**: Strong consistency
- **Latency**: Milliseconds (synchronous call)

## Conflict Resolution

### Conflict Types

1. **Write Conflicts**: Two contexts try to modify the same data
   - **Resolution**: Only owner context can write (prevented by design)

2. **Read Conflicts**: Cached data is stale
   - **Resolution**: Accept eventual consistency or query owner context

3. **Event Ordering**: Events arrive out of order
   - **Resolution**: Use event timestamps and version numbers

### Conflict Prevention

- **Clear Ownership**: Only owner context can modify data
- **Event Sourcing**: Events provide audit trail and ordering
- **Idempotent Handlers**: Duplicate events don't cause issues
- **Optimistic Locking**: Version numbers prevent concurrent updates

## Data Governance

### Data Quality

- **Owner Responsibility**: Owner context ensures data quality
- **Validation**: Input validation at API and domain layers
- **Audit Trail**: Domain events provide complete history
- **Data Cleansing**: Periodic cleanup of stale cached data

### Data Privacy

- **GDPR Compliance**: Customer context handles data subject requests
- **Data Minimization**: Only cache necessary data
- **Data Retention**: Define retention policies per context
- **Data Deletion**: Cascade deletions via events

### Data Security

- **Encryption**: Sensitive data encrypted at rest and in transit
- **Access Control**: Context boundaries enforce access control
- **Audit Logging**: All data access is logged
- **PCI Compliance**: Payment context maintains PCI-DSS compliance

## Related Documentation

- [Information Viewpoint Overview](overview.md)
- [Domain Models](domain-models.md)
- [Data Flow](data-flow.md)
- [Security Perspective](../../perspectives/security/overview.md)

---

**Document Status**: Active  
**Last Review**: 2025-10-23  
**Next Review**: 2026-01-23  
**Owner**: Architecture Team
