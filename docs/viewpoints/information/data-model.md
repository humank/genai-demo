# Data Model Design

## Automated Data Discovery and Governance

### AWS Glue Data Catalog Integration

The GenAI Demo application adopts AWS Glue Data Catalog for automated schema discovery and data governance, ensuring data model consistency and traceability across 13 bounded contexts.

#### Automated Discovery Mechanism
- **Daily Scheduled Scanning**: Automatically scans Aurora database at 2 AM daily
- **Real-time Change Detection**: RDS events trigger immediate schema discovery
- **Intelligent Exclusion**: Automatically excludes system tables and management tables
- **Cross-Region Consistency**: Supports multi-region deployment of Aurora Global Database

#### Data Catalog Structure
```
genai_demo_catalog/
├── customer_tables/     # Customer bounded context
├── order_tables/        # Order bounded context  
├── product_tables/      # Product bounded context
├── inventory_tables/    # Inventory bounded context
├── payment_tables/      # Payment bounded context
├── delivery_tables/     # Delivery bounded context
├── shoppingcart_tables/ # Shopping cart bounded context
├── pricing_tables/      # Pricing bounded context
├── promotion_tables/    # Promotion bounded context
├── seller_tables/       # Seller bounded context
├── review_tables/       # Review bounded context
├── notification_tables/ # Notification bounded context
└── observability_tables/# Observability bounded context
```

For detailed data governance architecture, please refer to [Data Governance Architecture](data-governance-architecture.md).

## Domain Model

### Aggregate Root Design
- Customer Aggregate
- Order Aggregate
- Product Aggregate

### Value Object Design
```java
public record CustomerId(String value) {
    public CustomerId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Customer ID cannot be null or blank");
        }
    }
}
```

## Data Persistence

### JPA Entity Mapping
```java
@Entity
@Table(name = "customers")
public class Customer {
    @Id
    private String id;
    
    @Embedded
    private CustomerName name;
    
    @Embedded
    private Email email;
}
```

### Database Design
- Normalization design principles
- Index strategy planning
- Data integrity constraints

## Data Migration

### Flyway Scripts
```sql
-- V1__Create_customer_table.sql
CREATE TABLE customers (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```