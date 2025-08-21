# 聚合根狀態變更與 Domain Event 對應分析

## 分析概述

本文檔分析了所有聚合根的狀態變更方法，並建立了與 Domain Event 的對應關係。

## 聚合根分析結果

### 1. ShoppingCart 聚合根 ✅ 完整實作

**位置**: `solid.humank.genaidemo.domain.shoppingcart.model.aggregate.ShoppingCart`

**狀態變更方法與事件對應**:

| 狀態變更方法              | Domain Event                   | 實作狀態    |
| ------------------------- | ------------------------------ | ----------- |
| `ShoppingCart()` 構造函數 | `CartCreatedEvent`             | ✅ 已實作   |
| `addItem()`               | `CartItemAddedEvent`           | ✅ 已實作   |
| `updateItemQuantity()`    | `CartItemQuantityUpdatedEvent` | ✅ 已實作   |
| `removeItem()`            | `CartItemRemovedEvent`         | ✅ 已實作   |
| `clear()`                 | `CartClearedEvent`             | ✅ 已實作   |
| `updateStatus()`          | `CartStatusChangedEvent`       | ❌ 缺少事件 |

**評估**: ShoppingCart 聚合根的事件發布機制最完整，使用了新的 `collectEvent()` 方法。

### 2. Customer 聚合根 ❌ 需要實作事件發布

**位置**: `solid.humank.genaidemo.domain.customer.model.aggregate.Customer`

**狀態變更方法與事件對應**:

| 狀態變更方法                      | Domain Event                          | 實作狀態        |
| --------------------------------- | ------------------------------------- | --------------- |
| `Customer()` 構造函數             | `CustomerCreatedEvent`                | ❌ 需要實作     |
| `updateProfile()`                 | `CustomerProfileUpdatedEvent`         | ❌ 有 TODO 註解 |
| `addRewardPoints()`               | `RewardPointsEarnedEvent`             | ❌ 有 TODO 註解 |
| `redeemPoints()`                  | `RewardPointsRedeemedEvent`           | ❌ 有 TODO 註解 |
| `upgradeMembershipLevel()`        | `MembershipLevelUpgradedEvent`        | ❌ 需要實作     |
| `updateNotificationPreferences()` | `NotificationPreferencesUpdatedEvent` | ❌ 有 TODO 註解 |
| `updateStatus()`                  | `CustomerStatusChangedEvent`          | ❌ 需要實作     |
| `addDeliveryAddress()`            | `DeliveryAddressAddedEvent`           | ❌ 需要實作     |
| `removeDeliveryAddress()`         | `DeliveryAddressRemovedEvent`         | ❌ 需要實作     |

**問題**: Customer 聚合根沒有繼承 `AggregateRoot` 基類，無法使用事件收集機制。

### 3. Product 聚合根 ❌ 不可變設計，需要重構

**位置**: `solid.humank.genaidemo.domain.product.model.aggregate.Product`

**問題**: Product 聚合根是不可變的（所有屬性都是 final），沒有狀態變更方法。

**需要添加的狀態變更方法**:

| 需要添加的方法        | Domain Event                     | 實作狀態    |
| --------------------- | -------------------------------- | ----------- |
| `Product()` 構造函數  | `ProductCreatedEvent`            | ❌ 需要實作 |
| `updatePrice()`       | `ProductPriceUpdatedEvent`       | ❌ 需要實作 |
| `updateStock()`       | `ProductStockUpdatedEvent`       | ❌ 需要實作 |
| `discontinue()`       | `ProductDiscontinuedEvent`       | ❌ 需要實作 |
| `activate()`          | `ProductActivatedEvent`          | ❌ 需要實作 |
| `updateDescription()` | `ProductDescriptionUpdatedEvent` | ❌ 需要實作 |

### 4. Order 聚合根 ⚠️ 使用舊的事件發布機制

**位置**: `solid.humank.genaidemo.domain.order.model.aggregate.Order`

**狀態變更方法與事件對應**:

| 狀態變更方法       | Domain Event          | 實作狀態                          |
| ------------------ | --------------------- | --------------------------------- |
| `Order()` 構造函數 | `OrderCreatedEvent`   | ⚠️ 使用 `AggregateLifecycleAware` |
| `addItem()`        | `OrderItemAddedEvent` | ⚠️ 使用 `AggregateLifecycleAware` |
| `submit()`         | `OrderSubmittedEvent` | ⚠️ 使用 `AggregateLifecycleAware` |
| `confirm()`        | `OrderConfirmedEvent` | ❌ 缺少事件                       |
| `markAsPaid()`     | `OrderPaidEvent`      | ❌ 缺少事件                       |
| `ship()`           | `OrderShippedEvent`   | ❌ 缺少事件                       |
| `deliver()`        | `OrderDeliveredEvent` | ❌ 缺少事件                       |
| `cancel()`         | `OrderCancelledEvent` | ❌ 缺少事件                       |

**問題**: Order 聚合根使用舊的 `AggregateLifecycleAware.apply()` 方法，需要遷移到新的事件收集機制。

### 5. Inventory 聚合根 ⚠️ 使用舊的事件發布機制

**位置**: `solid.humank.genaidemo.domain.inventory.model.aggregate.Inventory`

**狀態變更方法與事件對應**:

| 狀態變更方法           | Domain Event                 | 實作狀態                          |
| ---------------------- | ---------------------------- | --------------------------------- |
| `Inventory()` 構造函數 | `InventoryCreatedEvent`      | ⚠️ 使用 `AggregateLifecycleAware` |
| `reserve()`            | `StockReservedEvent`         | ⚠️ 使用 `AggregateLifecycleAware` |
| `addStock()`           | `StockAddedEvent`            | ⚠️ 使用 `AggregateLifecycleAware` |
| `releaseReservation()` | `StockReleasedEvent`         | ❌ 缺少事件                       |
| `confirmReservation()` | `StockConfirmedEvent`        | ❌ 缺少事件                       |
| `setThreshold()`       | `ThresholdUpdatedEvent`      | ❌ 缺少事件                       |
| `synchronize()`        | `InventorySynchronizedEvent` | ❌ 缺少事件                       |

**問題**: Inventory 聚合根使用舊的 `AggregateLifecycleAware.apply()` 方法，需要遷移到新的事件收集機制。

### 6. Promotion 聚合根 ❌ 沒有事件發布機制

**位置**: `solid.humank.genaidemo.domain.promotion.model.aggregate.Promotion`

**狀態變更方法與事件對應**:

| 狀態變更方法           | Domain Event                  | 實作狀態    |
| ---------------------- | ----------------------------- | ----------- |
| `Promotion()` 構造函數 | `PromotionCreatedEvent`       | ❌ 需要實作 |
| `use()`                | `PromotionUsedEvent`          | ❌ 需要實作 |
| `updateStatus()`       | `PromotionStatusChangedEvent` | ❌ 需要實作 |
| `updateInfo()`         | `PromotionInfoUpdatedEvent`   | ❌ 需要實作 |

**問題**: Promotion 聚合根沒有繼承 `AggregateRoot` 基類，也沒有事件發布機制。

### 7. Payment 聚合根 ⚠️ 使用舊的事件發布機制

**位置**: `solid.humank.genaidemo.domain.payment.model.aggregate.Payment`

**狀態變更方法與事件對應**:

| 狀態變更方法         | Domain Event            | 實作狀態                          |
| -------------------- | ----------------------- | --------------------------------- |
| `Payment()` 構造函數 | `PaymentCreatedEvent`   | ⚠️ 使用 `AggregateLifecycleAware` |
| `complete()`         | `PaymentCompletedEvent` | ⚠️ 使用 `AggregateLifecycleAware` |
| `fail()`             | `PaymentFailedEvent`    | ⚠️ 使用 `AggregateLifecycleAware` |
| `request()`          | `PaymentRequestedEvent` | ❌ 需要檢查                       |
| `refund()`           | `PaymentRefundedEvent`  | ❌ 需要實作                       |

**問題**: Payment 聚合根使用舊的 `AggregateLifecycleAware.apply()` 方法，需要遷移到新的事件收集機制。

### 8. Notification 聚合根 ⚠️ 使用舊的事件發布機制

**位置**: `solid.humank.genaidemo.domain.notification.model.aggregate.Notification`

**狀態變更方法與事件對應**:

| 狀態變更方法              | Domain Event                     | 實作狀態                          |
| ------------------------- | -------------------------------- | --------------------------------- |
| `Notification()` 構造函數 | `NotificationCreatedEvent`       | ⚠️ 使用 `AggregateLifecycleAware` |
| `updateStatus()`          | `NotificationStatusChangedEvent` | ⚠️ 使用 `AggregateLifecycleAware` |
| `send()`                  | `NotificationSentEvent`          | ❌ 需要實作                       |
| `deliver()`               | `NotificationDeliveredEvent`     | ❌ 需要實作                       |
| `markAsRead()`            | `NotificationReadEvent`          | ❌ 需要實作                       |
| `fail()`                  | `NotificationFailedEvent`        | ❌ 需要實作                       |

**問題**: Notification 聚合根使用舊的 `AggregateLifecycleAware.apply()` 方法，需要遷移到新的事件收集機制。

## 總結

### 事件發布機制狀態

1. **✅ 完整實作**: ShoppingCart（使用新的 `collectEvent()` 機制）
2. **⚠️ 部分實作**: Order、Inventory、Payment、Notification（使用舊的 `AggregateLifecycleAware` 機制）
3. **❌ 需要實作**: Customer、Product、Promotion

### 主要問題

1. **架構不一致**: 不同聚合根使用不同的事件發布機制
2. **缺少基類繼承**: Customer、Promotion 等聚合根沒有繼承 `AggregateRoot` 基類
3. **不可變設計**: Product 聚合根是不可變的，需要重構
4. **事件缺失**: 許多狀態變更方法沒有對應的 Domain Event

### 修復優先級

1. **高優先級**:

   - 修復 Customer 聚合根（繼承 AggregateRoot，實作事件發布）
   - 重構 Product 聚合根（添加狀態變更方法）
   - 遷移 Order、Inventory、Payment、Notification 到新的事件發布機制

2. **中優先級**:

   - 實作 Promotion 聚合根的事件發布

3. **低優先級**:
   - 補充缺失的 Domain Event
   - 統一事件發布機制

## 下一步行動

1. 修復 Customer 聚合根的事件發布機制
2. 重構 Product 聚合根添加狀態變更方法
3. 遷移 Order、Inventory、Payment、Notification 到新的事件收集機制
4. 創建缺失的 Domain Event 類別
5. 實作完整的測試覆蓋
