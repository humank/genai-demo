---
title: "Use Cases"
type: "functional-viewpoint"
category: "functional"
stakeholders: ["business-analysts", "product-managers", "developers", "qa-engineers"]
last_updated: "2025-10-22"
version: "1.0"
status: "active"
owner: "Product Team"
related_docs:
  - "viewpoints/functional/overview.md"
  - "viewpoints/functional/bounded-contexts.md"
  - "development/testing/bdd-testing.md"
tags: ["use-cases", "user-journeys", "business-processes", "bdd"]
---

# Use Cases

> **Status**: ✅ Active  
> **Last Updated**: 2025-10-22  
> **Owner**: Product Team

## Overview

This document describes the key use cases and user journeys in the Enterprise E-Commerce Platform. Each use case represents a specific way users interact with the system to achieve their goals.

Use cases are organized by user role and are implemented using **Behavior-Driven Development (BDD)** with Cucumber scenarios in `src/test/resources/features/`.

## User Roles

### Primary Roles
- **Customer**: End user who browses products and places orders
- **Seller**: Merchant who lists and sells products
- **Administrator**: System admin who manages the platform
- **Guest**: Unauthenticated visitor

### System Roles
- **Payment Gateway**: External payment processing system
- **Logistics Provider**: External delivery service
- **Notification Service**: Internal notification system

---

## Customer Use Cases

### UC-001: Customer Registration

**Actor**: Guest

**Goal**: Create a new customer account

**Preconditions**: 
- User is not logged in
- Email address is not already registered

**Main Flow**:
1. Guest navigates to registration page
2. Guest enters personal information (name, email, password, phone)
3. System validates input data
4. System creates customer account
5. System sends welcome email
6. System redirects to customer dashboard

**Postconditions**:
- Customer account is created with STANDARD membership
- Welcome email is sent
- Customer is logged in

**Alternative Flows**:
- **3a. Invalid email format**: System shows validation error
- **3b. Email already exists**: System shows "Email already registered" error
- **3c. Weak password**: System shows password requirements

**Business Rules**:
- Email must be unique
- Password must be at least 8 characters with uppercase, lowercase, number, and special character
- Default membership level is STANDARD
- Initial reward points balance is 0

**Related BDD Scenario**: `features/customer/registration.feature`

**Bounded Contexts Involved**:
- Customer Context (primary)
- Notification Context

---

### UC-002: Product Search and Browse

**Actor**: Customer, Guest

**Goal**: Find products matching search criteria

**Preconditions**: None

**Main Flow**:
1. User enters search query or selects category
2. System retrieves matching products
3. System displays product list with filters
4. User applies filters (price range, brand, rating)
5. System updates product list
6. User views product details

**Postconditions**:
- Product list is displayed
- Filters are applied
- Search history is recorded (for logged-in customers)

**Alternative Flows**:
- **2a. No products found**: System shows "No products found" message with suggestions
- **6a. Product out of stock**: System shows "Out of Stock" badge

**Business Rules**:
- Search results sorted by relevance by default
- Discontinued products not shown in search results
- Out of stock products shown but marked clearly

**Related BDD Scenario**: `features/product/search.feature`

**Bounded Contexts Involved**:
- Product Context (primary)
- Inventory Context

---

### UC-003: Add Product to Shopping Cart

**Actor**: Customer

**Goal**: Add selected product to shopping cart for later purchase

**Preconditions**:
- Customer is logged in
- Product is available

**Main Flow**:
1. Customer views product details
2. Customer selects quantity
3. Customer clicks "Add to Cart"
4. System validates product availability
5. System adds item to shopping cart
6. System updates cart total
7. System shows confirmation message

**Postconditions**:
- Product is added to cart
- Cart total is updated
- Cart item count is updated

**Alternative Flows**:
- **4a. Insufficient inventory**: System shows "Only X items available" message
- **4b. Product discontinued**: System shows "Product no longer available" error
- **5a. Item already in cart**: System updates quantity instead of adding duplicate

**Business Rules**:
- Quantity cannot exceed available inventory
- Cart items expire after 24 hours
- Maximum 99 items per product in cart

**Related BDD Scenario**: `features/cart/add-to-cart.feature`

**Bounded Contexts Involved**:
- Shopping Cart Context (primary)
- Product Context
- Inventory Context

---

### UC-004: Checkout and Place Order

**Actor**: Customer

**Goal**: Complete purchase of items in shopping cart

**Preconditions**:
- Customer is logged in
- Shopping cart has at least one item
- Customer has delivery address

**Main Flow**:
1. Customer navigates to checkout
2. System displays order summary
3. Customer selects delivery address
4. Customer selects payment method
5. Customer applies coupon code (optional)
6. System calculates final price (items + shipping - discounts + tax)
7. Customer confirms order
8. System creates order
9. System reserves inventory
10. System processes payment
11. System confirms order
12. System sends order confirmation email
13. System clears shopping cart

**Postconditions**:
- Order is created with status CONFIRMED
- Inventory is reserved
- Payment is completed
- Shopping cart is empty
- Order confirmation email is sent

**Alternative Flows**:
- **5a. Invalid coupon**: System shows "Invalid or expired coupon" error
- **9a. Insufficient inventory**: System shows "Some items are no longer available" and removes them
- **10a. Payment failed**: System cancels order, releases inventory, shows payment error
- **10b. Payment timeout**: System retries payment up to 3 times

**Business Rules**:
- Free shipping for orders > $100
- Tax calculated based on delivery address
- Promotions and coupons can be combined unless restricted
- Payment must complete within 15 minutes or order is cancelled

**Related BDD Scenario**: `features/order/checkout.feature`

**Bounded Contexts Involved**:
- Order Context (primary)
- Shopping Cart Context
- Inventory Context
- Payment Context
- Pricing Context
- Promotion Context
- Notification Context

---

### UC-005: Track Order Delivery

**Actor**: Customer

**Goal**: Check the status and location of ordered items

**Preconditions**:
- Customer is logged in
- Customer has at least one order

**Main Flow**:
1. Customer navigates to order history
2. System displays list of orders
3. Customer selects an order
4. System displays order details and delivery status
5. Customer clicks "Track Delivery"
6. System displays tracking information with timeline

**Postconditions**:
- Delivery status is displayed
- Tracking timeline is shown

**Alternative Flows**:
- **4a. Order not yet shipped**: System shows "Order is being prepared"
- **6a. Delivery failed**: System shows failure reason and next steps

**Business Rules**:
- Tracking available only for confirmed orders
- Tracking updates every 4 hours
- Estimated delivery: 3-5 business days

**Related BDD Scenario**: `features/delivery/tracking.feature`

**Bounded Contexts Involved**:
- Order Context (primary)
- Delivery Context

---

### UC-006: Submit Product Review

**Actor**: Customer

**Goal**: Share feedback about purchased product

**Preconditions**:
- Customer is logged in
- Customer has received the product
- Customer has not reviewed this product before

**Main Flow**:
1. Customer navigates to order history
2. Customer selects delivered order
3. Customer clicks "Write Review" for a product
4. Customer enters rating (1-5 stars) and review text
5. Customer submits review
6. System validates review
7. System creates review with PENDING status
8. System notifies admin for moderation
9. System shows "Review submitted for moderation" message

**Postconditions**:
- Review is created with PENDING status
- Admin is notified for moderation
- Customer sees confirmation message

**Alternative Flows**:
- **6a. Review contains inappropriate content**: System flags for manual review
- **6b. Customer already reviewed**: System shows "You have already reviewed this product"

**Business Rules**:
- Only customers who purchased the product can review
- One review per customer per product
- Reviews require admin approval before publication
- Rating must be 1-5 stars

**Related BDD Scenario**: `features/review/submit-review.feature`

**Bounded Contexts Involved**:
- Review Context (primary)
- Order Context
- Product Context

---

### UC-007: Manage Delivery Addresses

**Actor**: Customer

**Goal**: Add, update, or remove delivery addresses

**Preconditions**:
- Customer is logged in

**Main Flow**:
1. Customer navigates to account settings
2. Customer selects "Delivery Addresses"
3. System displays list of saved addresses
4. Customer clicks "Add New Address"
5. Customer enters address details
6. Customer marks as default (optional)
7. Customer saves address
8. System validates address
9. System saves address
10. System shows confirmation message

**Postconditions**:
- New address is saved
- Address is marked as default (if selected)

**Alternative Flows**:
- **8a. Invalid address**: System shows validation error
- **3a. Update existing address**: Customer edits and saves
- **3b. Delete address**: Customer confirms deletion, system removes address

**Business Rules**:
- Customer can have multiple addresses
- At least one address must be marked as default
- Cannot delete default address without setting another as default

**Related BDD Scenario**: `features/customer/manage-addresses.feature`

**Bounded Contexts Involved**:
- Customer Context (primary)

---

## Seller Use Cases

### UC-101: Seller Registration

**Actor**: Guest

**Goal**: Register as a seller to list products

**Preconditions**:
- User is not logged in
- Business information is available

**Main Flow**:
1. Guest navigates to seller registration page
2. Guest enters business information (company name, tax ID, contact details)
3. Guest uploads business documents
4. Guest submits registration
5. System validates information
6. System creates seller account with PENDING status
7. System notifies admin for approval
8. System sends confirmation email

**Postconditions**:
- Seller account created with PENDING status
- Admin notified for approval
- Confirmation email sent

**Alternative Flows**:
- **5a. Invalid tax ID**: System shows validation error
- **5b. Missing documents**: System shows required documents list

**Business Rules**:
- Seller must provide valid business registration
- Admin approval required before listing products
- Default commission rate: 15%

**Related BDD Scenario**: `features/seller/registration.feature`

**Bounded Contexts Involved**:
- Seller Context (primary)
- Notification Context

---

### UC-102: List New Product

**Actor**: Seller

**Goal**: Add a new product to the catalog

**Preconditions**:
- Seller is logged in
- Seller account is APPROVED

**Main Flow**:
1. Seller navigates to product management
2. Seller clicks "Add New Product"
3. Seller enters product details (name, description, price, category)
4. Seller uploads product images
5. Seller sets initial inventory quantity
6. Seller submits product
7. System validates product information
8. System creates product with ACTIVE status
9. System creates inventory record
10. System shows confirmation message

**Postconditions**:
- Product is created and visible in catalog
- Inventory record is created
- Product is searchable

**Alternative Flows**:
- **7a. Invalid price**: System shows "Price must be positive" error
- **7b. Missing required fields**: System highlights missing fields
- **7c. Duplicate SKU**: System shows "SKU already exists" error

**Business Rules**:
- SKU must be unique
- Price must be positive
- At least one product image required
- Product category must be valid

**Related BDD Scenario**: `features/seller/list-product.feature`

**Bounded Contexts Involved**:
- Product Context (primary)
- Inventory Context
- Seller Context

---

## Administrator Use Cases

### UC-201: Approve Seller Registration

**Actor**: Administrator

**Goal**: Review and approve seller registration

**Preconditions**:
- Admin is logged in
- Seller registration is PENDING

**Main Flow**:
1. Admin navigates to pending seller registrations
2. System displays list of pending sellers
3. Admin selects a seller
4. System displays seller details and documents
5. Admin reviews information
6. Admin clicks "Approve"
7. System updates seller status to APPROVED
8. System sends approval email to seller
9. System shows confirmation message

**Postconditions**:
- Seller status is APPROVED
- Seller can now list products
- Approval email sent

**Alternative Flows**:
- **6a. Reject seller**: Admin provides reason, system sends rejection email

**Business Rules**:
- Only admins can approve sellers
- Approval reason is optional
- Rejection reason is mandatory

**Related BDD Scenario**: `features/admin/approve-seller.feature`

**Bounded Contexts Involved**:
- Seller Context (primary)
- Notification Context

---

### UC-202: Moderate Product Review

**Actor**: Administrator

**Goal**: Review and approve/reject product reviews

**Preconditions**:
- Admin is logged in
- Review is in PENDING status

**Main Flow**:
1. Admin navigates to pending reviews
2. System displays list of pending reviews
3. Admin selects a review
4. System displays review details and product context
5. Admin reviews content
6. Admin clicks "Approve"
7. System updates review status to APPROVED
8. System publishes review on product page
9. System updates product rating
10. System shows confirmation message

**Postconditions**:
- Review status is APPROVED
- Review is visible on product page
- Product rating is updated

**Alternative Flows**:
- **6a. Reject review**: Admin provides reason, system notifies customer

**Business Rules**:
- Only admins can moderate reviews
- Approved reviews are immediately visible
- Rejected reviews are not visible to customers

**Related BDD Scenario**: `features/admin/moderate-review.feature`

**Bounded Contexts Involved**:
- Review Context (primary)
- Product Context
- Notification Context

---

### UC-203: Create Promotion Campaign

**Actor**: Administrator

**Goal**: Create a new promotional campaign

**Preconditions**:
- Admin is logged in

**Main Flow**:
1. Admin navigates to promotion management
2. Admin clicks "Create Promotion"
3. Admin enters promotion details (name, type, discount rate, validity period)
4. Admin sets promotion rules (minimum order, applicable categories)
5. Admin generates coupon codes (optional)
6. Admin activates promotion
7. System validates promotion
8. System creates promotion
9. System activates promotion
10. System shows confirmation message

**Postconditions**:
- Promotion is created and active
- Coupon codes are generated (if applicable)
- Promotion is applicable to orders

**Alternative Flows**:
- **7a. Invalid date range**: System shows "End date must be after start date" error
- **7b. Conflicting promotion**: System warns about overlapping promotions

**Business Rules**:
- Promotion must have valid date range
- Discount rate: 1-90%
- Coupon codes must be unique
- Multiple promotions can be active simultaneously

**Related BDD Scenario**: `features/admin/create-promotion.feature`

**Bounded Contexts Involved**:
- Promotion Context (primary)
- Pricing Context

---

## Cross-Cutting Use Cases

### UC-301: System Health Monitoring

**Actor**: System, Administrator

**Goal**: Monitor system health and performance

**Preconditions**: None

**Main Flow**:
1. System continuously collects metrics
2. System evaluates metrics against thresholds
3. System detects anomaly (high CPU, slow response time)
4. System triggers alert
5. System sends notification to admin
6. Admin investigates issue
7. Admin takes corrective action

**Postconditions**:
- Alert is logged
- Admin is notified
- Issue is tracked

**Alternative Flows**:
- **3a. No anomaly detected**: System continues monitoring

**Business Rules**:
- Metrics collected every 60 seconds
- Alerts triggered based on threshold rules
- Critical alerts sent immediately
- Warning alerts batched every 5 minutes

**Related BDD Scenario**: `features/observability/monitoring.feature`

**Bounded Contexts Involved**:
- Observability Context (primary)
- Notification Context

---

## Use Case Relationships

### Primary User Journeys

#### Journey 1: First-Time Purchase
```
UC-001 (Register) 
  → UC-002 (Search Products) 
  → UC-003 (Add to Cart) 
  → UC-004 (Checkout) 
  → UC-005 (Track Delivery) 
  → UC-006 (Submit Review)
```

#### Journey 2: Repeat Purchase
```
UC-002 (Search Products) 
  → UC-003 (Add to Cart) 
  → UC-004 (Checkout) 
  → UC-005 (Track Delivery)
```

#### Journey 3: Seller Onboarding
```
UC-101 (Seller Registration) 
  → UC-201 (Admin Approval) 
  → UC-102 (List Product)
```

### Use Case Dependencies

- **UC-004** depends on **UC-003** (must have items in cart)
- **UC-005** depends on **UC-004** (must have placed order)
- **UC-006** depends on **UC-005** (must have received product)
- **UC-102** depends on **UC-201** (seller must be approved)

## Implementation Status

| Use Case | Status | BDD Scenario | Implementation |
|----------|--------|--------------|----------------|
| UC-001 | ✅ Complete | ✅ Implemented | ✅ Complete |
| UC-002 | ✅ Complete | ✅ Implemented | ✅ Complete |
| UC-003 | ✅ Complete | ✅ Implemented | ✅ Complete |
| UC-004 | ✅ Complete | ✅ Implemented | ✅ Complete |
| UC-005 | ✅ Complete | ✅ Implemented | ✅ Complete |
| UC-006 | ✅ Complete | ✅ Implemented | ✅ Complete |
| UC-007 | ✅ Complete | ✅ Implemented | ✅ Complete |
| UC-101 | ✅ Complete | ✅ Implemented | ✅ Complete |
| UC-102 | ✅ Complete | ✅ Implemented | ✅ Complete |
| UC-201 | ✅ Complete | ✅ Implemented | ✅ Complete |
| UC-202 | ✅ Complete | ✅ Implemented | ✅ Complete |
| UC-203 | ✅ Complete | ✅ Implemented | ✅ Complete |
| UC-301 | ✅ Complete | ✅ Implemented | ✅ Complete |

## Testing Strategy

### BDD Testing with Cucumber

All use cases are implemented as BDD scenarios using Cucumber:

**Location**: `app/src/test/resources/features/`

**Example Scenario Structure**:
```gherkin
Feature: Customer Registration
  As a guest user
  I want to register for an account
  So that I can place orders

  Scenario: Successful customer registration
    Given I am on the registration page
    When I enter valid registration details
    And I submit the registration form
    Then my account should be created
    And I should receive a welcome email
    And I should be redirected to the dashboard
```

### Test Coverage

- **Unit Tests**: Domain logic within each bounded context
- **Integration Tests**: Cross-context workflows via events
- **BDD Tests**: End-to-end user journeys
- **API Tests**: REST endpoint validation

## Quick Links

- [Back to Functional Viewpoint](overview.md)
- [Bounded Contexts](bounded-contexts.md)
- [Functional Interfaces](interfaces.md)
- [BDD Testing Guide](../../development/testing/bdd-testing.md)
- [Main Documentation](../../README.md)

## Change History

| Date | Version | Author | Changes |
|------|---------|--------|---------|
| 2025-10-22 | 1.0 | Product Team | Initial version with 13 use cases |

---

**Document Version**: 1.0  
**Last Updated**: 2025-10-22
