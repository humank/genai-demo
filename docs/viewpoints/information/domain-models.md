---
title: "Domain Models"
viewpoint: "Information"
status: "Active"
last_updated: "2025-10-23"
related_documents:

  - "overview.md"
  - "data-ownership.md"
  - "data-flow.md"

---

# Domain Models

## Overview

This document describes the domain models for each bounded context in the system. Each model includes entities, value objects, and aggregates that represent the core business concepts and their relationships.

## Model Organization

Each bounded context maintains its own domain model with:

- **Aggregate Roots**: Top-level entities that define consistency boundaries
- **Entities**: Objects with unique identity within an aggregate
- **Value Objects**: Immutable objects defined by their attributes
- **Domain Events**: State change notifications

---

## Customer Context

### Purpose

Manages customer profiles, authentication, and preferences.

### Aggregate: Customer

**Aggregate Root**: `Customer`

#### Entities

- **Customer** (Root)
  - `CustomerId` (Identity)
  - `CustomerName` (Value Object)
  - `Email` (Value Object)
  - `Phone` (Value Object)
  - `MembershipLevel` (Enum: STANDARD, PREMIUM, VIP)
  - `RegistrationDate` (LocalDate)
  - `Status` (Enum: ACTIVE, SUSPENDED, DELETED)

- **CustomerAddress**
  - `AddressId` (Identity)
  - `Address` (Value Object)
  - `AddressType` (Enum: BILLING, SHIPPING)
  - `IsDefault` (Boolean)

#### Value Objects

- **CustomerName**
  - `firstName: String`
  - `lastName: String`
  - Validation: Non-empty, max 100 characters

- **Email**
  - `value: String`
  - Validation: Valid email format, unique

- **Phone**
  - `value: String`
  - Validation: Valid phone format

- **Address**
  - `street: String`
  - `city: String`
  - `state: String`
  - `postalCode: String`
  - `country: String`

#### Domain Events

- `CustomerRegisteredEvent`
- `CustomerProfileUpdatedEvent`
- `CustomerAddressAddedEvent`
- `CustomerMembershipUpgradedEvent`
- `CustomerSuspendedEvent`

#### Relationships

```text
Customer (1) ----< (0..*) CustomerAddress
```

---

## Order Context

### Purpose

Manages order lifecycle from creation to fulfillment.

### Aggregate: Order

**Aggregate Root**: `Order`

#### Entities

- **Order** (Root)
  - `OrderId` (Identity)
  - `CustomerId` (Reference to Customer Context)
  - `OrderDate` (LocalDateTime)
  - `Status` (Enum: CREATED, PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)
  - `TotalAmount` (Money Value Object)
  - `ShippingAddress` (Address Value Object)
  - `BillingAddress` (Address Value Object)

- **OrderItem**
  - `OrderItemId` (Identity)
  - `ProductId` (Reference to Product Context)
  - `ProductName` (String - snapshot)
  - `Quantity` (Integer)
  - `UnitPrice` (Money Value Object)
  - `Subtotal` (Money Value Object)

#### Value Objects

- **Money**
  - `amount: BigDecimal`
  - `currency: Currency`
  - Operations: add, subtract, multiply
  - Validation: Non-negative, max 2 decimal places

- **OrderId**
  - `value: String`
  - Format: "ORD-{UUID}"

#### Domain Events

- `OrderCreatedEvent`
- `OrderSubmittedEvent`
- `OrderConfirmedEvent`
- `OrderShippedEvent`
- `OrderDeliveredEvent`
- `OrderCancelledEvent`
- `OrderItemAddedEvent`
- `OrderItemRemovedEvent`

#### Relationships

```text
Order (1) ----< (1..*) OrderItem
Order (1) ---- (1) ShippingAddress
Order (1) ---- (1) BillingAddress
```

#### Business Rules

- Order must have at least one item
- Order total must equal sum of item subtotals
- Order can only be cancelled if status is CREATED or PENDING
- Order items cannot be modified after order is CONFIRMED

---

## Product Context

### Purpose

Manages product catalog, specifications, and inventory information.

### Aggregate: Product

**Aggregate Root**: `Product`

#### Entities

- **Product** (Root)
  - `ProductId` (Identity)
  - `ProductName` (String)
  - `Description` (String)
  - `Category` (Category Value Object)
  - `Price` (Money Value Object)
  - `Status` (Enum: ACTIVE, DISCONTINUED, OUT_OF_STOCK)
  - `SellerId` (Reference to Seller Context)

- **ProductSpecification**
  - `SpecificationId` (Identity)
  - `AttributeName` (String)
  - `AttributeValue` (String)

- **ProductImage**
  - `ImageId` (Identity)
  - `ImageUrl` (String)
  - `IsPrimary` (Boolean)
  - `DisplayOrder` (Integer)

#### Value Objects

- **Category**
  - `categoryId: String`
  - `categoryName: String`
  - `parentCategoryId: String` (optional)

- **ProductId**
  - `value: String`
  - Format: "PRD-{UUID}"

#### Domain Events

- `ProductCreatedEvent`
- `ProductUpdatedEvent`
- `ProductPriceChangedEvent`
- `ProductDiscontinuedEvent`
- `ProductImageAddedEvent`

#### Relationships

```text
Product (1) ----< (0..*) ProductSpecification
Product (1) ----< (0..*) ProductImage
Product (1) ---- (1) Category
```

---

## Inventory Context

### Purpose

Manages stock levels, reservations, and inventory movements.

### Aggregate: InventoryItem

**Aggregate Root**: `InventoryItem`

#### Entities

- **InventoryItem** (Root)
  - `InventoryItemId` (Identity)
  - `ProductId` (Reference to Product Context)
  - `WarehouseId` (String)
  - `QuantityOnHand` (Integer)
  - `QuantityReserved` (Integer)
  - `QuantityAvailable` (Integer - calculated)
  - `ReorderPoint` (Integer)
  - `ReorderQuantity` (Integer)

- **InventoryReservation**
  - `ReservationId` (Identity)
  - `OrderId` (Reference to Order Context)
  - `Quantity` (Integer)
  - `ReservedAt` (LocalDateTime)
  - `ExpiresAt` (LocalDateTime)
  - `Status` (Enum: ACTIVE, FULFILLED, EXPIRED, CANCELLED)

#### Value Objects

- **StockLevel**
  - `onHand: Integer`
  - `reserved: Integer`
  - `available: Integer` (calculated: onHand - reserved)
  - Validation: onHand >= 0, reserved >= 0

#### Domain Events

- `InventoryReceivedEvent`
- `InventoryReservedEvent`
- `InventoryReservationExpiredEvent`
- `InventoryReservationCancelledEvent`
- `InventoryFulfilledEvent`
- `LowStockAlertEvent`

#### Relationships

```text
InventoryItem (1) ----< (0..*) InventoryReservation
```

#### Business Rules

- Available quantity = On-hand quantity - Reserved quantity
- Reservations expire after 15 minutes if not fulfilled
- Cannot reserve more than available quantity
- Low stock alert when available < reorder point

---

## Payment Context

### Purpose

Manages payment processing, transactions, and payment methods.

### Aggregate: Payment

**Aggregate Root**: `Payment`

#### Entities

- **Payment** (Root)
  - `PaymentId` (Identity)
  - `OrderId` (Reference to Order Context)
  - `CustomerId` (Reference to Customer Context)
  - `Amount` (Money Value Object)
  - `PaymentMethod` (PaymentMethod Value Object)
  - `Status` (Enum: PENDING, AUTHORIZED, CAPTURED, FAILED, REFUNDED)
  - `TransactionId` (String - from payment gateway)
  - `CreatedAt` (LocalDateTime)
  - `ProcessedAt` (LocalDateTime)

- **PaymentTransaction**
  - `TransactionId` (Identity)
  - `TransactionType` (Enum: AUTHORIZATION, CAPTURE, REFUND)
  - `Amount` (Money Value Object)
  - `Status` (Enum: SUCCESS, FAILED)
  - `GatewayResponse` (String)
  - `ProcessedAt` (LocalDateTime)

#### Value Objects

- **PaymentMethod**
  - `type: PaymentMethodType` (Enum: CREDIT_CARD, DEBIT_CARD, PAYPAL, BANK_TRANSFER)
  - `last4Digits: String` (for cards)
  - `expiryDate: YearMonth` (for cards)
  - `cardBrand: String` (for cards)

- **PaymentId**
  - `value: String`
  - Format: "PAY-{UUID}"

#### Domain Events

- `PaymentInitiatedEvent`
- `PaymentAuthorizedEvent`
- `PaymentCapturedEvent`
- `PaymentFailedEvent`
- `PaymentRefundedEvent`

#### Relationships

```text
Payment (1) ----< (1..*) PaymentTransaction
Payment (1) ---- (1) PaymentMethod
```

#### Business Rules

- Payment must be authorized before capture
- Refund amount cannot exceed captured amount
- Failed payments can be retried up to 3 times
- Payment expires after 24 hours if not captured

---

## Shopping Cart Context

### Purpose

Manages active shopping carts and cart items before order creation.

### Aggregate: ShoppingCart

**Aggregate Root**: `ShoppingCart`

#### Entities

- **ShoppingCart** (Root)
  - `CartId` (Identity)
  - `CustomerId` (Reference to Customer Context)
  - `Status` (Enum: ACTIVE, ABANDONED, CONVERTED)
  - `CreatedAt` (LocalDateTime)
  - `UpdatedAt` (LocalDateTime)
  - `ExpiresAt` (LocalDateTime)

- **CartItem**
  - `CartItemId` (Identity)
  - `ProductId` (Reference to Product Context)
  - `ProductName` (String - snapshot)
  - `Quantity` (Integer)
  - `UnitPrice` (Money Value Object)
  - `Subtotal` (Money Value Object)
  - `AddedAt` (LocalDateTime)

#### Value Objects

- **CartId**
  - `value: String`
  - Format: "CART-{UUID}"

#### Domain Events

- `CartCreatedEvent`
- `ItemAddedToCartEvent`
- `ItemRemovedFromCartEvent`
- `ItemQuantityUpdatedEvent`
- `CartAbandonedEvent`
- `CartConvertedToOrderEvent`

#### Relationships

```text
ShoppingCart (1) ----< (0..*) CartItem
```

#### Business Rules

- Cart expires after 7 days of inactivity
- Cart item quantity must be > 0
- Cart total = sum of all item subtotals
- Cart can only be converted to order if it has items

---

## Promotion Context

### Purpose

Manages discount rules, promotional campaigns, and coupon codes.

### Aggregate: Promotion

**Aggregate Root**: `Promotion`

#### Entities

- **Promotion** (Root)
  - `PromotionId` (Identity)
  - `PromotionName` (String)
  - `Description` (String)
  - `DiscountType` (Enum: PERCENTAGE, FIXED_AMOUNT, BUY_X_GET_Y)
  - `DiscountValue` (BigDecimal)
  - `MinimumPurchaseAmount` (Money Value Object)
  - `StartDate` (LocalDateTime)
  - `EndDate` (LocalDateTime)
  - `Status` (Enum: DRAFT, ACTIVE, EXPIRED, CANCELLED)
  - `UsageLimit` (Integer)
  - `UsageCount` (Integer)

- **PromotionRule**
  - `RuleId` (Identity)
  - `RuleType` (Enum: PRODUCT_CATEGORY, CUSTOMER_SEGMENT, ORDER_AMOUNT)
  - `RuleCondition` (String - JSON)

#### Value Objects

- **CouponCode**
  - `code: String`
  - `promotionId: PromotionId`
  - `usageLimit: Integer`
  - `usageCount: Integer`
  - Validation: Unique, alphanumeric

#### Domain Events

- `PromotionCreatedEvent`
- `PromotionActivatedEvent`
- `PromotionExpiredEvent`
- `PromotionAppliedEvent`
- `CouponCodeGeneratedEvent`

#### Relationships

```text
Promotion (1) ----< (0..*) PromotionRule
Promotion (1) ----< (0..*) CouponCode
```

---

## Review Context

### Purpose

Manages product reviews and ratings from customers.

### Aggregate: Review

**Aggregate Root**: `Review`

#### Entities

- **Review** (Root)
  - `ReviewId` (Identity)
  - `ProductId` (Reference to Product Context)
  - `CustomerId` (Reference to Customer Context)
  - `OrderId` (Reference to Order Context)
  - `Rating` (Integer: 1-5)
  - `Title` (String)
  - `Content` (String)
  - `Status` (Enum: PENDING, APPROVED, REJECTED)
  - `CreatedAt` (LocalDateTime)
  - `UpdatedAt` (LocalDateTime)

- **ReviewImage**
  - `ImageId` (Identity)
  - `ImageUrl` (String)
  - `DisplayOrder` (Integer)

- **ReviewComment**
  - `CommentId` (Identity)
  - `UserId` (String)
  - `Content` (String)
  - `CreatedAt` (LocalDateTime)

#### Domain Events

- `ReviewSubmittedEvent`
- `ReviewApprovedEvent`
- `ReviewRejectedEvent`
- `ReviewUpdatedEvent`
- `ReviewCommentAddedEvent`

#### Relationships

```text
Review (1) ----< (0..*) ReviewImage
Review (1) ----< (0..*) ReviewComment
```

#### Business Rules

- Customer can only review products they have purchased
- One review per customer per product
- Rating must be between 1 and 5
- Review requires moderation before publication

---

## Shared Value Objects

These value objects are used across multiple bounded contexts:

### Money

```java
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be non-negative");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency is required");
        }
    }
    
    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }
    
    public Money multiply(int factor) {
        return new Money(amount.multiply(BigDecimal.valueOf(factor)), currency);
    }
}
```

### Address

```java
public record Address(
    String street,
    String city,
    String state,
    String postalCode,
    String country
) {
    public Address {
        if (street == null || street.isBlank()) {
            throw new IllegalArgumentException("Street is required");
        }
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City is required");
        }
        if (postalCode == null || postalCode.isBlank()) {
            throw new IllegalArgumentException("Postal code is required");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Country is required");
        }
    }
}
```

### Email

```java
public record Email(String value) {
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    
    public Email {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
    }
}
```

## Model Diagrams

### Entity Relationship Diagrams

- [Customer Context ER Diagram](../../diagrams/generated/information/customer-context-er.png)
- [Order Context ER Diagram](../../diagrams/generated/information/order-context-er.png)
- [Product Context ER Diagram](../../diagrams/generated/information/product-context-er.png)
- [Inventory Context ER Diagram](../../diagrams/generated/information/inventory-context-er.png)
- [Payment Context ER Diagram](../../diagrams/generated/information/payment-context-er.png)
- [Shopping Cart Context ER Diagram](../../diagrams/generated/information/shopping-cart-context-er.png)

### Aggregate Diagrams

- [Order Aggregate](../../diagrams/generated/information/order-aggregate.png)
- [Customer Aggregate](../../diagrams/generated/information/customer-aggregate.png)
- [Product Aggregate](../../diagrams/generated/information/product-aggregate.png)


**Document Status**: Active  
**Last Review**: 2025-10-23  
**Next Review**: 2026-01-23  
**Owner**: Architecture Team
