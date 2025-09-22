# 聚合根設計指南

## 概述

本指南基於專案中的實際實作，提供聚合根的設計模式、事件收集機制和最佳實踐。

## @AggregateRoot 註解使用

### 基本配置

```java
@AggregateRoot(
    name = "Customer",                    // 聚合根名稱
    description = "客戶聚合根",            // 描述
    boundedContext = "Customer",          // 所屬限界上下文
    version = "2.0",                     // 版本號
    enableEventCollection = true         // 是否啟用事件收集
)
public class Customer implements AggregateRootInterface {
    // 實作內容
}
```

### 註解屬性說明

- **name**: 聚合根的業務名稱，用於識別和文檔
- **description**: 聚合根的詳細描述
- **boundedContext**: 所屬的限界上下文
- **version**: 版本號，用於演進管理
- **enableEventCollection**: 控制是否啟用事件收集功能

## AggregateRootInterface 介面

### 核心方法

```java
public interface AggregateRootInterface {
    
    // 事件收集
    default void collectEvent(DomainEvent event);
    
    // 獲取未提交事件
    default List<DomainEvent> getUncommittedEvents();
    
    // 標記事件為已提交
    default void markEventsAsCommitted();
    
    // 檢查是否有未提交事件
    default boolean hasUncommittedEvents();
    
    // 清除事件
    default void clearEvents();
    
    // 獲取聚合根元數據
    default String getAggregateRootName();
    default String getBoundedContext();
    default String getVersion();
}
```

## 事件收集最佳實踐

### 在業務操作中收集事件

```java
public void updateProfile(CustomerName newName, Email newEmail, Phone newPhone) {
    // 1. 執行業務邏輯驗證
    validateProfileUpdate(newName, newEmail, newPhone);
    
    // 2. 更新狀態
    this.name = newName;
    this.email = newEmail;
    this.phone = newPhone;
    
    // 3. 收集領域事件
    collectEvent(CustomerProfileUpdatedEvent.create(this.id, newName, newEmail, newPhone));
}
```

### 事件收集時機

1. **狀態變更後**: 在聚合根狀態變更後立即收集事件
2. **業務操作完成**: 確保業務邏輯執行成功後再收集事件
3. **驗證通過**: 所有業務規則驗證通過後收集事件

## 應用服務整合

### 事件發布流程

```java
@Service
@Transactional
public class CustomerApplicationService {
    
    public void updateCustomerProfile(UpdateProfileCommand command) {
        // 1. 載入聚合根
        Customer customer = customerRepository.findById(command.customerId())
            .orElseThrow(() -> new CustomerNotFoundException(command.customerId()));
        
        // 2. 執行業務操作（事件被收集）
        customer.updateProfile(command.name(), command.email(), command.phone());
        
        // 3. 保存聚合根
        customerRepository.save(customer);
        
        // 4. 發布收集的事件
        domainEventService.publishEventsFromAggregate(customer);
    }
}
```

這種設計確保了事件的一致性和可靠性。