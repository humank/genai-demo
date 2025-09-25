# 資料模型設計

## 自動化資料發現與治理

### AWS Glue Data Catalog 整合

GenAI Demo 應用程式採用 AWS Glue Data Catalog 進行自動化 schema 發現和資料治理，確保跨 13 個 bounded contexts 的資料模型一致性和可追溯性。

#### 自動發現機制
- **每日排程掃描**: 每天凌晨 2 點自動掃描 Aurora 資料庫
- **即時變更檢測**: RDS 事件觸發即時 schema 發現
- **智能排除**: 自動排除系統表和管理表
- **跨區域一致性**: 支援 Aurora Global Database 的多區域部署

#### 資料目錄結構
```
genai_demo_catalog/
├── customer_tables/     # 客戶 bounded context
├── order_tables/        # 訂單 bounded context  
├── product_tables/      # 產品 bounded context
├── inventory_tables/    # 庫存 bounded context
├── payment_tables/      # 支付 bounded context
├── delivery_tables/     # 配送 bounded context
├── shoppingcart_tables/ # 購物車 bounded context
├── pricing_tables/      # 定價 bounded context
├── promotion_tables/    # 促銷 bounded context
├── seller_tables/       # 賣家 bounded context
├── review_tables/       # 評價 bounded context
├── notification_tables/ # 通知 bounded context
└── observability_tables/# 可觀測性 bounded context
```

詳細的資料治理架構請參考 [Data Governance Architecture](data-governance-architecture.md)。

## 領域模型

### 聚合根設計
- 客戶聚合 (Customer Aggregate)
- 訂單聚合 (Order Aggregate)
- 產品聚合 (Product Aggregate)

### 值物件設計
```java
public record CustomerId(String value) {
    public CustomerId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Customer ID cannot be null or blank");
        }
    }
}
```

## 資料持久化

### JPA 實體映射
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

### 資料庫設計
- 正規化設計原則
- 索引策略規劃
- 資料完整性約束

## 資料遷移

### Flyway 腳本
```sql
-- V1__Create_customer_table.sql
CREATE TABLE customers (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```
