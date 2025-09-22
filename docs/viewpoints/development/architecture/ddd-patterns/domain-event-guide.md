# 領域事件實作指南

## 概述

本指南基於專案中的領域事件實作，提供事件設計、處理和最佳實踐。

## Record 實作模式

### 基本事件結構

```java
public record CustomerCreatedEvent(
        CustomerId customerId,
        CustomerName customerName,
        Email email,
        MembershipLevel membershipLevel,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     */
    public static CustomerCreatedEvent create(
            CustomerId customerId, 
            CustomerName customerName, 
            Email email,
            MembershipLevel membershipLevel) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new CustomerCreatedEvent(
            customerId, customerName, email, membershipLevel,
            metadata.eventId(), metadata.occurredOn()
        );
    }

    @Override
    public String getEventType() {
        return DomainEvent.getEventTypeFromClass(this.getClass());
    }

    @Override
    public String getAggregateId() {
        return customerId.getValue();
    }
}
```

### 業務邏輯增強事件

```java
public record PerformanceMetricReceivedEvent(
        String metricId,
        String metricType,
        double value,
        String page,
        String sessionId,
        String traceId,
        LocalDateTime receivedAt,
        UUID domainEventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 檢查是否為核心網頁指標
     */
    public boolean isCoreWebVital() {
        return "lcp".equals(metricType) ||
                "fid".equals(metricType) ||
                "cls".equals(metricType);
    }

    /**
     * 檢查指標是否超過建議閾值
     */
    public boolean exceedsRecommendedThreshold() {
        return switch (metricType) {
            case "lcp" -> value > 2500;
            case "fid" -> value > 100;
            case "cls" -> value > 0.1;
            default -> false;
        };
    }
}
```

## 事件處理機制

### 抽象事件處理器

```java
public abstract class AbstractDomainEventHandler<T extends DomainEvent> 
    implements DomainEventHandler<T> {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Order(100)
    public void onDomainEvent(DomainEventPublisherAdapter.DomainEventWrapper wrapper) {
        DomainEvent event = wrapper.getSource();

        if (getSupportedEventType().isInstance(event)) {
            @SuppressWarnings("unchecked")
            T typedEvent = (T) event;

            if (shouldHandle(typedEvent)) {
                try {
                    handle(typedEvent);
                } catch (Exception e) {
                    throw new DomainEventProcessingException(
                            "Failed to process event: " + event.getClass().getSimpleName(), e);
                }
            }
        }
    }

    protected abstract void handle(T event);
    protected abstract Class<T> getSupportedEventType();
}
```

### 具體事件處理器

```java
@Component
public class CustomerCreatedEventHandler extends AbstractDomainEventHandler<CustomerCreatedEvent> {
    
    private final EmailService emailService;
    private final CustomerStatsService customerStatsService;
    
    @Override
    protected void handle(CustomerCreatedEvent event) {
        // 發送歡迎郵件
        emailService.sendWelcomeEmail(event.email(), event.customerName());
        
        // 更新客戶統計
        customerStatsService.createStatsRecord(event.customerId());
    }
    
    @Override
    protected Class<CustomerCreatedEvent> getSupportedEventType() {
        return CustomerCreatedEvent.class;
    }
}
```

## 事件設計原則

1. **不可變性**: 使用 Record 確保事件不可變
2. **完整上下文**: 包含處理事件所需的所有資訊
3. **業務語言**: 使用領域專家理解的術語
4. **工廠方法**: 提供靜態工廠方法自動設定元數據
5. **業務邏輯**: 在事件中包含相關的業務判斷方法

## 事務管理

使用 `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` 確保：
- 事件在事務成功提交後才被處理
- 避免事務回滾時的不一致狀態
- 提供可靠的事件處理機制