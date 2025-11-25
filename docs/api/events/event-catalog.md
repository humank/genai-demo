# Domain Events Catalog

## Overview

This document provides a comprehensive catalog of all domain events in the e-commerce platform. Events are organized by bounded context and include descriptions, payload information, and related events.

**Last Updated**: 2025-10-25

---

## Event Naming Convention

All events follow these naming conventions:

- **Past Tense**: Events use past tense verbs (e.g., `CustomerCreated`, not `CreateCustomer`)
- **Context Prefix**: Events include the aggregate name (e.g., `OrderSubmitted`, `PaymentProcessed`)
- **Descriptive**: Event names clearly describe what happened

---

## Customer Context Events

| Event Name | Description | Aggregate | Triggers |
|------------|-------------|-----------|----------|
| `CustomerCreatedEvent` | A new customer account was created | Customer | Customer registration |
| `CustomerProfileUpdatedEvent` | Customer profile information was updated | Customer | Profile edit |
| `CustomerEmailVerifiedEvent` | Customer email address was verified | Customer | Email verification |
| `CustomerPasswordChangedEvent` | Customer password was changed | Customer | Password change |
| `CustomerDeactivatedEvent` | Customer account was deactivated | Customer | Account closure |
| `CustomerReactivatedEvent` | Customer account was reactivated | Customer | Account reactivation |
| `CustomerMembershipUpgradedEvent` | Customer membership level was upgraded | Customer | Membership upgrade |

**Total**: 7 events

**Details**: See [Customer Context Documentation](../../viewpoints/functional/contexts/customer.md)

---

## Order Context Events

| Event Name | Description | Aggregate | Triggers |
|------------|-------------|-----------|----------|
| `OrderCreatedEvent` | A new order was created from shopping cart | Order | Checkout initiation |
| `OrderSubmittedEvent` | Order was submitted for processing | Order | Order submission |
| `OrderConfirmedEvent` | Order was confirmed after payment | Order | Payment success |
| `OrderCancelledEvent` | Order was cancelled by customer or system | Order | Order cancellation |
| `OrderShippedEvent` | Order was shipped to customer | Order | Shipping dispatch |
| `OrderDeliveredEvent` | Order was delivered to customer | Order | Delivery confirmation |
| `OrderReturnedEvent` | Order was returned by customer | Order | Return request |
| `OrderRefundedEvent` | Order was refunded to customer | Order | Refund processing |
| `OrderItemAddedEvent` | Item was added to order | Order | Item addition |
| `OrderItemRemovedEvent` | Item was removed from order | Order | Item removal |
| `OrderItemQuantityChangedEvent` | Item quantity was changed in order | Order | Quantity update |

**Total**: 11 events

**Details**: See [Order Context Documentation](../../viewpoints/functional/contexts/order.md)

---

## Product Context Events

| Event Name | Description | Aggregate | Triggers |
|------------|-------------|-----------|----------|
| `ProductCreatedEvent` | A new product was added to catalog | Product | Product creation |
| `ProductUpdatedEvent` | Product information was updated | Product | Product edit |
| `ProductPriceChangedEvent` | Product price was changed | Product | Price update |
| `ProductStockUpdatedEvent` | Product stock level was updated | Product | Stock adjustment |
| `ProductDeactivatedEvent` | Product was removed from catalog | Product | Product deactivation |
| `ProductReactivatedEvent` | Product was restored to catalog | Product | Product reactivation |
| `ProductCategoryChangedEvent` | Product category was changed | Product | Category update |

**Total**: 7 events

**Details**: See [Product Context Documentation](../../viewpoints/functional/contexts/product.md)

---

## Payment Context Events

| Event Name | Description | Aggregate | Triggers |
|------------|-------------|-----------|----------|
| `PaymentInitiatedEvent` | Payment process was initiated | Payment | Payment start |
| `PaymentProcessedEvent` | Payment was successfully processed | Payment | Payment success |
| `PaymentFailedEvent` | Payment processing failed | Payment | Payment failure |
| `PaymentRefundedEvent` | Payment was refunded to customer | Payment | Refund processing |
| `PaymentCancelledEvent` | Payment was cancelled | Payment | Payment cancellation |

**Total**: 5 events

**Details**: See [Payment Context Documentation](../../viewpoints/functional/contexts/payment.md)

---

## Inventory Context Events

| Event Name | Description | Aggregate | Triggers |
|------------|-------------|-----------|----------|
| `InventoryReservedEvent` | Inventory was reserved for order | Inventory | Order submission |
| `InventoryReleasedEvent` | Reserved inventory was released | Inventory | Order cancellation |
| `InventoryAdjustedEvent` | Inventory level was manually adjusted | Inventory | Stock adjustment |
| `InventoryLowStockAlertEvent` | Inventory fell below threshold | Inventory | Low stock detection |

**Total**: 4 events

**Details**: See [Inventory Context Documentation](../../viewpoints/functional/contexts/inventory.md)

---

## Shipping Context Events

| Event Name | Description | Aggregate | Triggers |
|------------|-------------|-----------|----------|
| `ShippingScheduledEvent` | Shipping was scheduled for order | Shipping | Order confirmation |
| `ShippingLabelCreatedEvent` | Shipping label was generated | Shipping | Label generation |
| `ShippingDispatchedEvent` | Package was dispatched | Shipping | Package pickup |
| `ShippingInTransitEvent` | Package is in transit | Shipping | Transit update |
| `ShippingDeliveredEvent` | Package was delivered | Shipping | Delivery confirmation |
| `ShippingFailedEvent` | Delivery attempt failed | Shipping | Delivery failure |

**Total**: 6 events

**Details**: See [Delivery Context Documentation](../../viewpoints/functional/contexts/delivery.md)

---

## Promotion Context Events

| Event Name | Description | Aggregate | Triggers |
|------------|-------------|-----------|----------|
| `PromotionCreatedEvent` | A new promotion was created | Promotion | Promotion setup |
| `PromotionActivatedEvent` | Promotion was activated | Promotion | Promotion start |
| `PromotionDeactivatedEvent` | Promotion was deactivated | Promotion | Promotion end |
| `PromotionAppliedEvent` | Promotion was applied to order | Promotion | Discount application |

**Total**: 4 events

**Details**: See [Promotion Context Documentation](../../viewpoints/functional/contexts/promotion.md)

---

## Notification Context Events

| Event Name | Description | Aggregate | Triggers |
|------------|-------------|-----------|----------|
| `NotificationSentEvent` | Notification was sent to customer | Notification | Event trigger |
| `NotificationFailedEvent` | Notification sending failed | Notification | Send failure |
| `NotificationReadEvent` | Notification was read by customer | Notification | Read action |

**Total**: 3 events

**Details**: See [Notification Context Documentation](../../viewpoints/functional/contexts/notification.md)

---

## Review Context Events

| Event Name | Description | Aggregate | Triggers |
|------------|-------------|-----------|----------|
| `ReviewCreatedEvent` | Customer created a product review | Review | Review submission |
| `ReviewUpdatedEvent` | Customer updated their review | Review | Review edit |
| `ReviewDeletedEvent` | Review was deleted | Review | Review deletion |
| `ReviewApprovedEvent` | Review was approved by moderator | Review | Moderation approval |

**Total**: 4 events

**Details**: See [Review Context Documentation](../../viewpoints/functional/contexts/review.md)

---

## Shopping Cart Context Events

| Event Name | Description | Aggregate | Triggers |
|------------|-------------|-----------|----------|
| `CartCreatedEvent` | A new shopping cart was created | ShoppingCart | Cart initialization |
| `ItemAddedToCartEvent` | Item was added to cart | ShoppingCart | Add to cart |
| `ItemRemovedFromCartEvent` | Item was removed from cart | ShoppingCart | Remove from cart |
| `CartItemQuantityChangedEvent` | Item quantity was changed in cart | ShoppingCart | Quantity update |
| `CartCheckedOutEvent` | Cart was checked out | ShoppingCart | Checkout |

**Total**: 5 events

**Details**: See [Shopping Cart Context Documentation](../../viewpoints/functional/contexts/shopping-cart.md)

---

## Pricing Context Events

| Event Name | Description | Aggregate | Triggers |
|------------|-------------|-----------|----------|
| `PriceCalculatedEvent` | Price was calculated for order | Pricing | Price calculation |
| `DiscountAppliedEvent` | Discount was applied to order | Pricing | Discount application |
| `TaxCalculatedEvent` | Tax was calculated for order | Pricing | Tax calculation |

**Total**: 3 events

**Details**: See [Pricing Context Documentation](../../viewpoints/functional/contexts/pricing.md)

---

## Seller Context Events

| Event Name | Description | Aggregate | Triggers |
|------------|-------------|-----------|----------|
| `SellerRegisteredEvent` | A new seller was registered | Seller | Seller registration |
| `SellerVerifiedEvent` | Seller was verified | Seller | Verification completion |
| `SellerSuspendedEvent` | Seller account was suspended | Seller | Account suspension |
| `SellerReactivatedEvent` | Seller account was reactivated | Seller | Account reactivation |
| `SellerProductListedEvent` | Seller listed a new product | Seller | Product listing |

**Total**: 5 events

**Details**: See [Seller Context Documentation](../../viewpoints/functional/contexts/seller.md)

---

## Delivery Context Events

| Event Name | Description | Aggregate | Triggers |
|------------|-------------|-----------|----------|
| `DeliveryScheduledEvent` | Delivery was scheduled | Delivery | Delivery scheduling |
| `DeliveryAssignedEvent` | Delivery was assigned to driver | Delivery | Driver assignment |
| `DeliveryInProgressEvent` | Delivery is in progress | Delivery | Delivery start |
| `DeliveryCompletedEvent` | Delivery was completed | Delivery | Delivery completion |
| `DeliveryFailedEvent` | Delivery attempt failed | Delivery | Delivery failure |

**Total**: 5 events

**Details**: See [Delivery Context Documentation](../../viewpoints/functional/contexts/delivery.md)

---

## Event Statistics

### By Context

| Context | Event Count | Percentage |
|---------|-------------|------------|
| Order | 11 | 16.9% |
| Customer | 7 | 10.8% |
| Product | 7 | 10.8% |
| Shipping | 6 | 9.2% |
| Payment | 5 | 7.7% |
| Shopping Cart | 5 | 7.7% |
| Seller | 5 | 7.7% |
| Delivery | 5 | 7.7% |
| Promotion | 4 | 6.2% |
| Inventory | 4 | 6.2% |
| Review | 4 | 6.2% |
| Notification | 3 | 4.6% |
| Pricing | 3 | 4.6% |
| **Total** | **65** | **100%** |

### By Category

| Category | Event Count | Description |
|----------|-------------|-------------|
| Lifecycle | 25 | Created, Updated, Deleted events |
| State Transition | 20 | Status change events |
| Business Action | 15 | Business operation events |
| System Event | 5 | System-generated events |

---

## Event Dependencies

### Common Event Flows

#### Order Processing Flow

```text
CartCheckedOutEvent
    → OrderCreatedEvent
    → OrderSubmittedEvent
    → PaymentInitiatedEvent
    → PaymentProcessedEvent
    → OrderConfirmedEvent
    → InventoryReservedEvent
    → ShippingScheduledEvent
    → ShippingDispatchedEvent
    → ShippingDeliveredEvent
    → OrderDeliveredEvent
```

#### Customer Registration Flow

```text
CustomerCreatedEvent
    → NotificationSentEvent (Welcome Email)
    → CustomerEmailVerifiedEvent
```

#### Product Purchase Flow

```text
ItemAddedToCartEvent
    → CartCheckedOutEvent
    → OrderCreatedEvent
    → ProductStockUpdatedEvent
    → InventoryReservedEvent
```

---

## Event Versioning

### Current Versions

All events are currently at version 1.0. Future versions will be documented here.

### Deprecated Events

No events are currently deprecated.

---

## Related Documentation

- **Event Schemas**: See [schemas/](schemas/) directory
- **Event Handling**: See `.kiro/steering/domain-events.md`
- **Architecture**: See `docs/viewpoints/information/data-flow.md`
- **API Documentation**: See [README.md](README.md)

---

## Quick Navigation

- [Customer Events](../../viewpoints/functional/contexts/customer.md)
- [Order Events](../../viewpoints/functional/contexts/order.md)
- [Product Events](../../viewpoints/functional/contexts/product.md)
- [Payment Events](../../viewpoints/functional/contexts/payment.md)
- [Inventory Events](../../viewpoints/functional/contexts/inventory.md)
- [Delivery Events](../../viewpoints/functional/contexts/delivery.md)
- [Promotion Events](../../viewpoints/functional/contexts/promotion.md)
- [Notification Events](../../viewpoints/functional/contexts/notification.md)
- [Review Events](../../viewpoints/functional/contexts/review.md)
- [Shopping Cart Events](../../viewpoints/functional/contexts/shopping-cart.md)
- [Pricing Events](../../viewpoints/functional/contexts/pricing.md)
- [Seller Events](../../viewpoints/functional/contexts/seller.md)

---

**Document Version**: 1.0
**Last Updated**: 2025-10-25
**Owner**: Architecture Team
