# 值對象設計指南

## 概述

本指南基於專案中 50+ 個值對象的實作經驗，提供值對象的設計模式和最佳實踐。

## Record 實作模式 (推薦)

### 基本值對象

```java
@ValueObject(name = "CustomerId", description = "客戶唯一標識符")
public record CustomerId(String value) {
    
    public CustomerId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
    }
    
    public static CustomerId generate() {
        return new CustomerId(UUID.randomUUID().toString());
    }
    
    public static CustomerId of(String value) {
        return new CustomerId(value);
    }
}
```

### 複合值對象

```java
@ValueObject(name = "RewardPoints", description = "紅利點數值對象")
public record RewardPoints(int balance, LocalDateTime lastUpdated) {
    
    public RewardPoints {
        if (balance < 0) {
            throw new IllegalArgumentException("Reward points balance cannot be negative");
        }
        if (lastUpdated == null) {
            lastUpdated = LocalDateTime.now();
        }
    }
    
    public static RewardPoints empty() {
        return new RewardPoints(0, LocalDateTime.now());
    }
    
    public RewardPoints add(int points) {
        return new RewardPoints(balance + points, LocalDateTime.now());
    }
    
    public RewardPoints subtract(int points) {
        if (points > balance) {
            throw new IllegalArgumentException("Insufficient reward points");
        }
        return new RewardPoints(balance - points, LocalDateTime.now());
    }
    
    public boolean canRedeem(int points) {
        return balance >= points;
    }
}
```

## 枚舉值對象

### 狀態枚舉

```java
@ValueObject
public enum CustomerStatus {
    ACTIVE("活躍"),
    INACTIVE("非活躍"),
    SUSPENDED("暫停"),
    DELETED("已刪除");
    
    private final String description;
    
    CustomerStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isActive() {
        return this == ACTIVE;
    }
    
    public boolean canPlaceOrder() {
        return this == ACTIVE;
    }
}
```

## Money 值對象

### 貨幣處理

```java
@ValueObject
public record Money(BigDecimal amount, Currency currency) {
    
    public Money {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
    }
    
    public static Money twd(double amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.TWD);
    }
    
    public Money add(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add different currencies");
        }
        return new Money(amount.add(other.amount), currency);
    }
    
    public boolean isGreaterThan(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot compare different currencies");
        }
        return amount.compareTo(other.amount) > 0;
    }
}
```

## 設計原則

1. **不可變性**: 使用 Record 確保不可變性
2. **業務驗證**: 在建構子中進行驗證
3. **工廠方法**: 提供語意清晰的創建方法
4. **業務方法**: 包含相關的業務邏輯方法