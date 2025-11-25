---
title: "Payment Context"
type: "functional-viewpoint"
category: "functional"
status: "active"
last_updated: "2025-10-22"
owner: "Architecture Team"
---

# Payment Context

> **Responsibility**: Process payments and manage payment transactions

## Overview

The Payment Context handles all financial transactions. It integrates with external payment gateways, manages payment status, and handles refunds. It ensures secure and reliable processing of customer payments.

## Domain Model

**Core Aggregate**: `Payment`

**Key Entities**:
- `Payment` (Aggregate Root)
- `PaymentTransaction`
- `Refund`

**Key Value Objects**:
- `PaymentId`
- `OrderId`
- `Money`
- `PaymentMethod` (CREDIT_CARD, DEBIT_CARD, PAYPAL, BANK_TRANSFER)
- `PaymentStatus` (PENDING, COMPLETED, FAILED, REFUNDED)
- `TransactionId`

### Domain Model Diagram

```mermaid
classDiagram
    class Payment {
        +PaymentId id
        +OrderId orderId
        +Money amount
        +PaymentStatus status
        +PaymentMethod method
        +process()
        +refund()
    }
    class PaymentTransaction {
        +TransactionId id
        +DateTime timestamp
        +String gatewayReference
        +TransactionType type
        +Money amount
    }
    class Refund {
        +RefundId id
        +Money amount
        +String reason
        +RefundStatus status
    }

    Payment "1" --> "*" PaymentTransaction
    Payment "1" --> "*" Refund
```

## Events

### Event Flow

```mermaid
sequenceDiagram
    participant O as Order Context
    participant P as Payment Context
    participant G as Payment Gateway

    O-->>P: OrderSubmittedEvent
    P->>P: Create Payment
    P->>G: Process Charge

    alt Success
        G-->>P: Charge Successful
        P-->>O: PaymentCompletedEvent
    else Failure
        G-->>P: Charge Failed
        P-->>O: PaymentFailedEvent
    end
```

**Domain Events Published**:
- `PaymentInitiatedEvent`
- `PaymentCompletedEvent`
- `PaymentFailedEvent`
- `RefundProcessedEvent`

**Domain Events Consumed**:
- `OrderSubmittedEvent` (from Order Context) → Initiate payment
- `OrderCancelledEvent` (from Order Context) → Process refund

## API Interface

**REST API Endpoints**:
- `POST /api/v1/payments` - Initiate payment
- `GET /api/v1/payments/{id}` - Get payment status
- `POST /api/v1/payments/{id}/refund` - Process refund (admin)

## Business Rules

- Payment amount must match order total
- Failed payments trigger automatic retry (max 3 attempts)
- Refunds can only be processed for completed payments
- Partial refunds allowed for order cancellations
