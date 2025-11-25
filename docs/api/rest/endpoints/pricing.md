# Pricing API

> **Package**: `solid.humank.genaidemo.interfaces.web.pricing`
> **Base URL**: `/api/pricing`

The Pricing API manages pricing rules, commission rates, and product pricing strategies.

## 1. Pricing Rules

### Create Pricing Rule
**POST** `/api/pricing/rules`

Creates a new pricing rule for a product or category.

**Request Body**: `CreatePricingRuleRequest`
- `productId`: Target product ID
- `productCategory`: Target category (e.g., ELECTRONICS, FASHION)
- `finalPrice`: Final price
- `discountPercentage`: Discount percentage
- `effectiveFrom`: Start date
- `effectiveTo`: End date

### Get Product Rules
**GET** `/api/pricing/rules/product/{productId}`

Retrieves all pricing rules for a specific product.

### Get Category Rules
**GET** `/api/pricing/rules/category/{category}`

Retrieves all pricing rules for a specific product category.

**Allowed Categories**:
- ELECTRONICS, FASHION, GROCERIES, HOME_APPLIANCES, BEAUTY, SPORTS, BOOKS, TOYS, AUTOMOTIVE, HEALTH, GENERAL

## 2. Commission Management

### Update Commission Rate
**PUT** `/api/pricing/rules/{priceId}/commission`

Updates the commission rate for a specific pricing rule.

**Request Body**: `UpdateCommissionRateRequest`
- `normalRate`: Standard commission rate
- `eventRate`: Special event commission rate
