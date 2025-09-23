# 資料模型設計

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
