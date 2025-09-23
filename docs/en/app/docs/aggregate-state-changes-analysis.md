
# Aggregate Root狀態變更與 Domain Event 對應分析

## 分析概述

This document分析了所有Aggregate Root的狀態變更方法，並建立了與 Domain Event 的對應關係。

## Aggregate Root分析結果

### 1. ShoppingCart Aggregate Root ✅ 完整實作

**位置**: `solid.humank.genaidemo.domain.shoppingcart.model.aggregate.ShoppingCart`

**狀態變更方法與事件對應**:

| 狀態變更方法              | Domain Event                   | 實作狀態    |
| ------------------------- | ------------------------------ | ----------- |
| `ShoppingCart()` Construct函數 | `CartCreatedEvent`             | ✅ 已實作   |
| `addItem()`               | `CartItemAddedEvent`           | ✅ 已實作   |
| `updateItemQuantity()`    | `CartItemQuantityUpdatedEvent` | ✅ 已實作   |
| `removeItem()`            | `CartItemRemovedEvent`         | ✅ 已實作   |
| `clear()`                 | `CartClearedEvent`             | ✅ 已實作   |
| `updateStatus()`          | `CartStatusChangedEvent`       | ❌ 缺少事件 |

**評估**: ShoppingCart Aggregate Root的事件發布機制最完整，使用了新的 `collectEvent()` 方法。

### 2. Customer Aggregate Root ❌ 需要實作事件發布

**位置**: `solid.humank.genaidemo.domain.customer.model.aggregate.Customer`

**狀態變更方法與事件對應**:

| 狀態變更方法                      | Domain Event                          | 實作狀態        |
| --------------------------------- | ------------------------------------- | --------------- |
| `Customer()` Construct函數             | `CustomerCreatedEvent`                | ❌ 需要實作     |
| `updateProfile()`                 | `CustomerProfileUpdatedEvent`         | ❌ 有 TODO 註解 |
| `addRewardPoints()`               | `RewardPointsEarnedEvent`             | ❌ 有 TODO 註解 |
| `redeemPoints()`                  | `RewardPointsRedeemedEvent`           | ❌ 有 TODO 註解 |
| `upgradeMembershipLevel()`        | `MembershipLevelUpgradedEvent`        | ❌ 需要實作     |
| `updateNotificationPreferences()` | `NotificationPreferencesUpdatedEvent` | ❌ 有 TODO 註解 |
| `updateStatus()`                  | `CustomerStatusChangedEvent`          | ❌ 需要實作     |
| `addDeliveryAddress()`            | `DeliveryAddressAddedEvent`           | ❌ 需要實作     |
| `removeDeliveryAddress()`         | `DeliveryAddressRemovedEvent`         | ❌ 需要實作     |

**問題**: Customer Aggregate Root沒有繼承 `AggregateRoot` 基類，無法使用事件收集機制。

### Design

**位置**: `solid.humank.genaidemo.domain.product.model.aggregate.Product`

**問題**: Product Aggregate Root是不可變的（所有屬性都是 final），沒有狀態變更方法。

**需要添加的狀態變更方法**:

| 需要添加的方法        | Domain Event                     | 實作狀態    |
| --------------------- | -------------------------------- | ----------- |
| `Product()` Construct函數  | `ProductCreatedEvent`            | ❌ 需要實作 |
| `updatePrice()`       | `ProductPriceUpdatedEvent`       | ❌ 需要實作 |
| `updateStock()`       | `ProductStockUpdatedEvent`       | ❌ 需要實作 |
| `discontinue()`       | `ProductDiscontinuedEvent`       | ❌ 需要實作 |
| `activate()`          | `ProductActivatedEvent`          | ❌ 需要實作 |
| `updateDescription()` | `ProductDescriptionUpdatedEvent` | ❌ 需要實作 |

### 4. Order Aggregate Root ⚠️ 使用舊的事件發布機制

**位置**: `solid.humank.genaidemo.domain.order.model.aggregate.Order`

**狀態變更方法與事件對應**:

| 狀態變更方法       | Domain Event          | 實作狀態                          |
| ------------------ | --------------------- | --------------------------------- |
| `Order()` Construct函數 | `OrderCreatedEvent`   | ⚠️ 使用 `AggregateLifecycleAware` |
| `addItem()`        | `OrderItemAddedEvent` | ⚠️ 使用 `AggregateLifecycleAware` |
| `submit()`         | `OrderSubmittedEvent` | ⚠️ 使用 `AggregateLifecycleAware` |
| `confirm()`        | `OrderConfirmedEvent` | ❌ 缺少事件                       |
| `markAsPaid()`     | `OrderPaidEvent`      | ❌ 缺少事件                       |
| `ship()`           | `OrderShippedEvent`   | ❌ 缺少事件                       |
| `deliver()`        | `OrderDeliveredEvent` | ❌ 缺少事件                       |
| `cancel()`         | `OrderCancelledEvent` | ❌ 缺少事件                       |

**問題**: Order Aggregate Root使用舊的 `AggregateLifecycleAware.apply()` 方法，需要遷移到新的事件收集機制。

### 5. Inventory Aggregate Root ⚠️ 使用舊的事件發布機制

**位置**: `solid.humank.genaidemo.domain.inventory.model.aggregate.Inventory`

**狀態變更方法與事件對應**:

| 狀態變更方法           | Domain Event                 | 實作狀態                          |
| ---------------------- | ---------------------------- | --------------------------------- |
| `Inventory()` Construct函數 | `InventoryCreatedEvent`      | ⚠️ 使用 `AggregateLifecycleAware` |
| `reserve()`            | `StockReservedEvent`         | ⚠️ 使用 `AggregateLifecycleAware` |
| `addStock()`           | `StockAddedEvent`            | ⚠️ 使用 `AggregateLifecycleAware` |
| `releaseReservation()` | `StockReleasedEvent`         | ❌ 缺少事件                       |
| `confirmReservation()` | `StockConfirmedEvent`        | ❌ 缺少事件                       |
| `setThreshold()`       | `ThresholdUpdatedEvent`      | ❌ 缺少事件                       |
| `synchronize()`        | `InventorySynchronizedEvent` | ❌ 缺少事件                       |

**問題**: Inventory Aggregate Root使用舊的 `AggregateLifecycleAware.apply()` 方法，需要遷移到新的事件收集機制。

### 6. Promotion Aggregate Root ❌ 沒有事件發布機制

**位置**: `solid.humank.genaidemo.domain.promotion.model.aggregate.Promotion`

**狀態變更方法與事件對應**:

| 狀態變更方法           | Domain Event                  | 實作狀態    |
| ---------------------- | ----------------------------- | ----------- |
| `Promotion()` Construct函數 | `PromotionCreatedEvent`       | ❌ 需要實作 |
| `use()`                | `PromotionUsedEvent`          | ❌ 需要實作 |
| `updateStatus()`       | `PromotionStatusChangedEvent` | ❌ 需要實作 |
| `updateInfo()`         | `PromotionInfoUpdatedEvent`   | ❌ 需要實作 |

**問題**: Promotion Aggregate Root沒有繼承 `AggregateRoot` 基類，也沒有事件發布機制。

### 7. Payment Aggregate Root ⚠️ 使用舊的事件發布機制

**位置**: `solid.humank.genaidemo.domain.payment.model.aggregate.Payment`

**狀態變更方法與事件對應**:

| 狀態變更方法         | Domain Event            | 實作狀態                          |
| -------------------- | ----------------------- | --------------------------------- |
| `Payment()` Construct函數 | `PaymentCreatedEvent`   | ⚠️ 使用 `AggregateLifecycleAware` |
| `complete()`         | `PaymentCompletedEvent` | ⚠️ 使用 `AggregateLifecycleAware` |
| `fail()`             | `PaymentFailedEvent`    | ⚠️ 使用 `AggregateLifecycleAware` |
| `request()`          | `PaymentRequestedEvent` | ❌ 需要檢查                       |
| `refund()`           | `PaymentRefundedEvent`  | ❌ 需要實作                       |

**問題**: Payment Aggregate Root使用舊的 `AggregateLifecycleAware.apply()` 方法，需要遷移到新的事件收集機制。

### 8. Notification Aggregate Root ⚠️ 使用舊的事件發布機制

**位置**: `solid.humank.genaidemo.domain.notification.model.aggregate.Notification`

**狀態變更方法與事件對應**:

| 狀態變更方法              | Domain Event                     | 實作狀態                          |
| ------------------------- | -------------------------------- | --------------------------------- |
| `Notification()` Construct函數 | `NotificationCreatedEvent`       | ⚠️ 使用 `AggregateLifecycleAware` |
| `updateStatus()`          | `NotificationStatusChangedEvent` | ⚠️ 使用 `AggregateLifecycleAware` |
| `send()`                  | `NotificationSentEvent`          | ❌ 需要實作                       |
| `deliver()`               | `NotificationDeliveredEvent`     | ❌ 需要實作                       |
| `markAsRead()`            | `NotificationReadEvent`          | ❌ 需要實作                       |
| `fail()`                  | `NotificationFailedEvent`        | ❌ 需要實作                       |

**問題**: Notification Aggregate Root使用舊的 `AggregateLifecycleAware.apply()` 方法，需要遷移到新的事件收集機制。

## summary

### 事件發布機制狀態

1. **✅ 完整實作**: ShoppingCart（使用新的 `collectEvent()` 機制）
2. **⚠️ 部分實作**: Order、Inventory、Payment、Notification（使用舊的 `AggregateLifecycleAware` 機制）
3. **❌ 需要實作**: Customer、Product、Promotion

### 主要問題

1. **架構不一致**: 不同Aggregate Root使用不同的事件發布機制
2. **缺少基類繼承**: Customer、Promotion 等Aggregate Root沒有繼承 `AggregateRoot` 基類
3. **不可變設計**: Product Aggregate Root是不可變的，需要Refactoring
4. **事件缺失**: 許多狀態變更方法沒有對應的 Domain Event

### 修復優先級

1. **高優先級**:

   - 修復 Customer Aggregate Root（繼承 AggregateRoot，實作事件發布）
   - Refactoring Product Aggregate Root（添加狀態變更方法）
   - 遷移 Order、Inventory、Payment、Notification 到新的事件發布機制

2. **中優先級**:

   - 實作 Promotion Aggregate Root的事件發布

3. **低優先級**:
   - 補充缺失的 Domain Event
   - 統一事件發布機制

## 下一步行動

1. 修復 Customer Aggregate Root的事件發布機制
2. Refactoring Product Aggregate Root添加狀態變更方法
3. 遷移 Order、Inventory、Payment、Notification 到新的事件收集機制
4. 創建缺失的 Domain Event 類別
5. 實作完整的測試覆蓋
