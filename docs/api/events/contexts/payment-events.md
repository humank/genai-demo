# Payment Context Events

## Overview

This document describes all domain events published by the Payment bounded context. These events capture payment processing, refunds, and transaction management.

**Last Updated**: 2025-10-25

---

## Event List

| Event Name | Trigger | Frequency | Priority |
|------------|---------|-----------|----------|
| `PaymentInitiatedEvent` | Payment start | Very High | P0 |
| `PaymentProcessedEvent` | Payment success | Very High | P0 |
| `PaymentFailedEvent` | Payment failure | Medium | P0 |
| `PaymentRefundedEvent` | Refund processing | Low | P1 |
| `PaymentCancelledEvent` | Payment cancellation | Low | P1 |

---

## PaymentInitiatedEvent

### Event Structure

```java
public record PaymentInitiatedEvent(
    PaymentId paymentId,
    OrderId orderId,
    CustomerId customerId,
    Money amount,
    PaymentMethod paymentMethod,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

### Example JSON

```json
{
  "eventType": "PaymentInitiated",
  "eventId": "300e8400-e29b-41d4-a716-446655440000",
  "occurredOn": "2025-10-25T10:05:30Z",
  "paymentId": "PAY-2025-001",
  "orderId": "ORD-2025-001",
  "customerId": "CUST-001",
  "amount": {
    "amount": 37695,
    "currency": "TWD"
  },
  "paymentMethod": "CREDIT_CARD"
}
```

### Event Handlers

- `PaymentGatewayHandler`: Process payment with gateway
- `PaymentTimeoutHandler`: Set payment timeout
- `FraudDetectionHandler`: Check for fraud

---

## PaymentProcessedEvent

### Event Structure

```java
public record PaymentProcessedEvent(
    PaymentId paymentId,
    OrderId orderId,
    CustomerId customerId,
    Money paidAmount,
    String transactionId,
    PaymentMethod paymentMethod,
    LocalDateTime processedAt,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

### Example JSON

```json
{
  "eventType": "PaymentProcessed",
  "eventId": "310e8400-e29b-41d4-a716-446655440001",
  "occurredOn": "2025-10-25T10:06:00Z",
  "paymentId": "PAY-2025-001",
  "orderId": "ORD-2025-001",
  "customerId": "CUST-001",
  "paidAmount": {
    "amount": 37695,
    "currency": "TWD"
  },
  "transactionId": "TXN-20251025-001",
  "paymentMethod": "CREDIT_CARD",
  "processedAt": "2025-10-25T10:06:00Z"
}
```

### Event Handlers

- `OrderConfirmationHandler`: Confirm order
- `PaymentReceiptHandler`: Generate receipt
- `AccountingHandler`: Record transaction

---

## PaymentFailedEvent

### Event Structure

```java
public record PaymentFailedEvent(
    PaymentId paymentId,
    OrderId orderId,
    CustomerId customerId,
    Money attemptedAmount,
    String failureReason,
    String errorCode,
    LocalDateTime failedAt,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

### Example JSON

```json
{
  "eventType": "PaymentFailed",
  "eventId": "320e8400-e29b-41d4-a716-446655440002",
  "occurredOn": "2025-10-25T10:06:00Z",
  "paymentId": "PAY-2025-002",
  "orderId": "ORD-2025-002",
  "customerId": "CUST-002",
  "attemptedAmount": {
    "amount": 15000,
    "currency": "TWD"
  },
  "failureReason": "信用卡餘額不足",
  "errorCode": "INSUFFICIENT_FUNDS",
  "failedAt": "2025-10-25T10:06:00Z"
}
```

### Event Handlers

- `OrderCancellationHandler`: Cancel order
- `PaymentRetryHandler`: Schedule retry if applicable
- `CustomerNotificationHandler`: Notify customer

---

## PaymentRefundedEvent

### Event Structure

```java
public record PaymentRefundedEvent(
    RefundId refundId,
    PaymentId originalPaymentId,
    OrderId orderId,
    CustomerId customerId,
    Money refundAmount,
    String refundReason,
    LocalDateTime refundedAt,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

### Example JSON

```json
{
  "eventType": "PaymentRefunded",
  "eventId": "330e8400-e29b-41d4-a716-446655440003",
  "occurredOn": "2025-10-29T15:00:00Z",
  "refundId": "REF-2025-001",
  "originalPaymentId": "PAY-2025-001",
  "orderId": "ORD-2025-001",
  "customerId": "CUST-001",
  "refundAmount": {
    "amount": 37695,
    "currency": "TWD"
  },
  "refundReason": "產品瑕疵退貨",
  "refundedAt": "2025-10-29T15:00:00Z"
}
```

### Event Handlers

- `RefundNotificationHandler`: Notify customer
- `AccountingHandler`: Record refund transaction
- `OrderStatusHandler`: Update order status

---

**Document Version**: 1.0  
**Last Updated**: 2025-10-25  
**Owner**: Payment Domain Team
