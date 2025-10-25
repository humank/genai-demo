# Notification API

## Overview

The Notification API provides endpoints for managing system notifications, including email, SMS, and in-app notifications. This API handles notification preferences, delivery status, and notification history.

**Base Path**: `/api/v1/notifications`

**Authentication**: Required for all endpoints

## Endpoints

### Get User Notifications

Retrieve notifications for the authenticated user.

**Endpoint**: `GET /api/v1/notifications/me`

**Authentication**: Required

**Query Parameters**:
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)
- `type`: Filter by type (EMAIL, SMS, IN_APP, PUSH)
- `status`: Filter by status (UNREAD, READ, ARCHIVED)
- `category`: Filter by category (ORDER, PROMOTION, SYSTEM, ACCOUNT)

**Success Response** (200 OK):
```json
{
  "data": {
    "content": [
      {
        "id": "notif-123",
        "type": "IN_APP",
        "category": "ORDER",
        "title": "Order Shipped",
        "message": "Your order #ORD-2025-001 has been shipped",
        "status": "UNREAD",
        "priority": "NORMAL",
        "data": {
          "orderId": "order-456",
          "trackingNumber": "TW1234567890"
        },
        "actionUrl": "/orders/order-456",
        "createdAt": "2025-10-25T14:00:00Z",
        "readAt": null
      },
      {
        "id": "notif-124",
        "type": "EMAIL",
        "category": "PROMOTION",
        "title": "Special Offer: 20% Off",
        "message": "Get 20% off on all electronics this weekend",
        "status": "READ",
        "priority": "LOW",
        "createdAt": "2025-10-24T10:00:00Z",
        "readAt": "2025-10-24T11:30:00Z"
      }
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 45,
      "totalPages": 3
    },
    "unreadCount": 12
  }
}
```

**curl Example**:
```bash
curl -X GET "https://api.ecommerce.com/api/v1/notifications/me?status=UNREAD" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Get Notification by ID

Retrieve a specific notification.

**Endpoint**: `GET /api/v1/notifications/{id}`

**Authentication**: Required

**Authorization**: User can only access own notifications

**Path Parameters**:
- `id`: Notification ID

**Success Response** (200 OK):
```json
{
  "data": {
    "id": "notif-123",
    "type": "IN_APP",
    "category": "ORDER",
    "title": "Order Shipped",
    "message": "Your order #ORD-2025-001 has been shipped. Track your package using tracking number TW1234567890.",
    "status": "UNREAD",
    "priority": "NORMAL",
    "data": {
      "orderId": "order-456",
      "trackingNumber": "TW1234567890",
      "carrier": "CHUNGHWA_POST",
      "estimatedDelivery": "2025-10-28T00:00:00Z"
    },
    "actionUrl": "/orders/order-456",
    "actionLabel": "Track Order",
    "createdAt": "2025-10-25T14:00:00Z",
    "readAt": null,
    "expiresAt": "2025-11-25T14:00:00Z"
  }
}
```

**Error Responses**:
- `403 Forbidden`: Cannot access other user's notifications
- `404 Not Found`: Notification not found

**curl Example**:
```bash
curl -X GET https://api.ecommerce.com/api/v1/notifications/notif-123 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Mark Notification as Read

Mark a notification as read.

**Endpoint**: `PATCH /api/v1/notifications/{id}/read`

**Authentication**: Required

**Path Parameters**:
- `id`: Notification ID

**Success Response** (200 OK):
```json
{
  "data": {
    "id": "notif-123",
    "status": "READ",
    "readAt": "2025-10-25T15:00:00Z"
  }
}
```

**curl Example**:
```bash
curl -X PATCH https://api.ecommerce.com/api/v1/notifications/notif-123/read \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Mark All Notifications as Read

Mark all notifications as read for the authenticated user.

**Endpoint**: `POST /api/v1/notifications/me/read-all`

**Authentication**: Required

**Success Response** (200 OK):
```json
{
  "data": {
    "markedCount": 12,
    "message": "All notifications marked as read"
  }
}
```

**curl Example**:
```bash
curl -X POST https://api.ecommerce.com/api/v1/notifications/me/read-all \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Delete Notification

Delete a notification (soft delete/archive).

**Endpoint**: `DELETE /api/v1/notifications/{id}`

**Authentication**: Required

**Path Parameters**:
- `id`: Notification ID

**Success Response** (204 No Content)

**curl Example**:
```bash
curl -X DELETE https://api.ecommerce.com/api/v1/notifications/notif-123 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Get Unread Count

Get count of unread notifications.

**Endpoint**: `GET /api/v1/notifications/me/unread-count`

**Authentication**: Required

**Success Response** (200 OK):
```json
{
  "data": {
    "total": 12,
    "byCategory": {
      "ORDER": 5,
      "PROMOTION": 3,
      "SYSTEM": 2,
      "ACCOUNT": 2
    }
  }
}
```

**curl Example**:
```bash
curl -X GET https://api.ecommerce.com/api/v1/notifications/me/unread-count \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Get Notification Preferences

Get notification preferences for the authenticated user.

**Endpoint**: `GET /api/v1/notifications/me/preferences`

**Authentication**: Required

**Success Response** (200 OK):
```json
{
  "data": {
    "email": {
      "enabled": true,
      "categories": {
        "ORDER": true,
        "PROMOTION": true,
        "SYSTEM": true,
        "ACCOUNT": true
      }
    },
    "sms": {
      "enabled": true,
      "categories": {
        "ORDER": true,
        "PROMOTION": false,
        "SYSTEM": true,
        "ACCOUNT": true
      }
    },
    "inApp": {
      "enabled": true,
      "categories": {
        "ORDER": true,
        "PROMOTION": true,
        "SYSTEM": true,
        "ACCOUNT": true
      }
    },
    "push": {
      "enabled": false,
      "categories": {
        "ORDER": false,
        "PROMOTION": false,
        "SYSTEM": false,
        "ACCOUNT": false
      }
    },
    "quietHours": {
      "enabled": true,
      "startTime": "22:00",
      "endTime": "08:00",
      "timezone": "Asia/Taipei"
    }
  }
}
```

**curl Example**:
```bash
curl -X GET https://api.ecommerce.com/api/v1/notifications/me/preferences \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Update Notification Preferences

Update notification preferences.

**Endpoint**: `PUT /api/v1/notifications/me/preferences`

**Authentication**: Required

**Request Body**:
```json
{
  "email": {
    "enabled": true,
    "categories": {
      "ORDER": true,
      "PROMOTION": false,
      "SYSTEM": true,
      "ACCOUNT": true
    }
  },
  "sms": {
    "enabled": true,
    "categories": {
      "ORDER": true,
      "PROMOTION": false,
      "SYSTEM": true,
      "ACCOUNT": true
    }
  },
  "quietHours": {
    "enabled": true,
    "startTime": "23:00",
    "endTime": "07:00",
    "timezone": "Asia/Taipei"
  }
}
```

**Success Response** (200 OK):
```json
{
  "data": {
    "email": {
      "enabled": true,
      "categories": {
        "ORDER": true,
        "PROMOTION": false,
        "SYSTEM": true,
        "ACCOUNT": true
      }
    },
    "sms": {
      "enabled": true,
      "categories": {
        "ORDER": true,
        "PROMOTION": false,
        "SYSTEM": true,
        "ACCOUNT": true
      }
    },
    "quietHours": {
      "enabled": true,
      "startTime": "23:00",
      "endTime": "07:00",
      "timezone": "Asia/Taipei"
    },
    "updatedAt": "2025-10-25T16:00:00Z"
  }
}
```

**curl Example**:
```bash
curl -X PUT https://api.ecommerce.com/api/v1/notifications/me/preferences \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "email": {
      "enabled": true,
      "categories": {
        "PROMOTION": false
      }
    }
  }'
```

---

### Send Notification (Admin)

Send a notification to users (admin only).

**Endpoint**: `POST /api/v1/notifications/send`

**Authentication**: Required

**Authorization**: ADMIN or MARKETING role required

**Request Body**:
```json
{
  "recipients": {
    "type": "ALL_USERS",
    "filters": {
      "membershipLevel": ["PREMIUM", "VIP"],
      "registeredAfter": "2025-01-01T00:00:00Z"
    }
  },
  "notification": {
    "type": "EMAIL",
    "category": "PROMOTION",
    "title": "Exclusive Offer for Premium Members",
    "message": "Get 30% off on your next purchase",
    "priority": "NORMAL",
    "actionUrl": "/promotions/premium-offer",
    "actionLabel": "Shop Now",
    "expiresAt": "2025-11-01T00:00:00Z"
  },
  "schedule": {
    "sendAt": "2025-10-26T10:00:00Z",
    "timezone": "Asia/Taipei"
  }
}
```

**Recipient Types**:
- `ALL_USERS`: All registered users
- `SPECIFIC_USERS`: List of user IDs
- `SEGMENT`: User segment based on filters

**Success Response** (202 Accepted):
```json
{
  "data": {
    "campaignId": "camp-123",
    "status": "SCHEDULED",
    "estimatedRecipients": 1250,
    "scheduledAt": "2025-10-26T10:00:00Z",
    "createdAt": "2025-10-25T16:30:00Z"
  }
}
```

**Error Responses**:
- `400 Bad Request`: Validation errors
- `403 Forbidden`: Insufficient permissions

**curl Example**:
```bash
curl -X POST https://api.ecommerce.com/api/v1/notifications/send \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "recipients": {
      "type": "ALL_USERS"
    },
    "notification": {
      "type": "EMAIL",
      "category": "PROMOTION",
      "title": "Special Offer",
      "message": "Get 20% off today"
    }
  }'
```

---

### Get Notification Campaign Status (Admin)

Get status of a notification campaign.

**Endpoint**: `GET /api/v1/notifications/campaigns/{campaignId}`

**Authentication**: Required

**Authorization**: ADMIN or MARKETING role required

**Path Parameters**:
- `campaignId`: Campaign ID

**Success Response** (200 OK):
```json
{
  "data": {
    "campaignId": "camp-123",
    "status": "COMPLETED",
    "notification": {
      "type": "EMAIL",
      "category": "PROMOTION",
      "title": "Exclusive Offer for Premium Members"
    },
    "statistics": {
      "totalRecipients": 1250,
      "sent": 1245,
      "delivered": 1230,
      "failed": 15,
      "opened": 850,
      "clicked": 320,
      "unsubscribed": 5
    },
    "scheduledAt": "2025-10-26T10:00:00Z",
    "startedAt": "2025-10-26T10:00:05Z",
    "completedAt": "2025-10-26T10:15:30Z"
  }
}
```

**curl Example**:
```bash
curl -X GET https://api.ecommerce.com/api/v1/notifications/campaigns/camp-123 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Get Notification Templates (Admin)

Get available notification templates.

**Endpoint**: `GET /api/v1/notifications/templates`

**Authentication**: Required

**Authorization**: ADMIN or MARKETING role required

**Query Parameters**:
- `type`: Filter by type (EMAIL, SMS)
- `category`: Filter by category

**Success Response** (200 OK):
```json
{
  "data": [
    {
      "id": "tmpl-order-confirmation",
      "name": "Order Confirmation",
      "type": "EMAIL",
      "category": "ORDER",
      "subject": "Order Confirmation - {{orderNumber}}",
      "body": "Thank you for your order {{orderNumber}}...",
      "variables": ["orderNumber", "customerName", "totalAmount"],
      "active": true
    },
    {
      "id": "tmpl-shipping-notification",
      "name": "Shipping Notification",
      "type": "EMAIL",
      "category": "ORDER",
      "subject": "Your order has been shipped",
      "body": "Your order {{orderNumber}} has been shipped...",
      "variables": ["orderNumber", "trackingNumber", "carrier"],
      "active": true
    }
  ]
}
```

**curl Example**:
```bash
curl -X GET "https://api.ecommerce.com/api/v1/notifications/templates?category=ORDER" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## Data Models

### Notification Object

```json
{
  "id": "string",
  "userId": "string",
  "type": "EMAIL | SMS | IN_APP | PUSH",
  "category": "ORDER | PROMOTION | SYSTEM | ACCOUNT",
  "title": "string",
  "message": "string",
  "status": "UNREAD | READ | ARCHIVED",
  "priority": "HIGH | NORMAL | LOW",
  "data": "object",
  "actionUrl": "string",
  "actionLabel": "string",
  "createdAt": "string (ISO 8601)",
  "readAt": "string (ISO 8601)",
  "expiresAt": "string (ISO 8601)"
}
```

### Notification Types

| Type | Description | Delivery Method |
|------|-------------|-----------------|
| EMAIL | Email notification | SMTP |
| SMS | SMS notification | SMS Gateway |
| IN_APP | In-app notification | WebSocket/Polling |
| PUSH | Push notification | FCM/APNS |

### Notification Categories

| Category | Description | Examples |
|----------|-------------|----------|
| ORDER | Order-related | Order confirmation, shipping updates |
| PROMOTION | Marketing | Special offers, discounts |
| SYSTEM | System messages | Maintenance, updates |
| ACCOUNT | Account-related | Password reset, profile updates |

## Business Rules

1. **Quiet Hours**: Notifications respect user's quiet hours settings
2. **Preference Override**: Critical notifications (e.g., security) ignore preferences
3. **Expiration**: Notifications expire after 30 days
4. **Delivery Retry**: Failed deliveries retry up to 3 times
5. **Rate Limiting**: Maximum 10 notifications per user per hour
6. **Unsubscribe**: Users can unsubscribe from promotional notifications
7. **Read Status**: In-app notifications marked as read when viewed

## Error Codes

| Code | Description | Solution |
|------|-------------|----------|
| `NOTIFICATION_NOT_FOUND` | Notification not found | Check notification ID |
| `NOTIFICATION_ACCESS_DENIED` | Cannot access notification | Check ownership |
| `NOTIFICATION_RATE_LIMIT_EXCEEDED` | Too many notifications | Wait before sending more |
| `NOTIFICATION_INVALID_RECIPIENT` | Invalid recipient | Check user ID or email |
| `NOTIFICATION_TEMPLATE_NOT_FOUND` | Template not found | Check template ID |
| `NOTIFICATION_DELIVERY_FAILED` | Delivery failed | Check delivery settings |

## Webhook Events

The system can send webhook events for notification status updates:

```json
{
  "event": "notification.delivered",
  "timestamp": "2025-10-25T14:00:00Z",
  "data": {
    "notificationId": "notif-123",
    "userId": "cust-123",
    "type": "EMAIL",
    "status": "DELIVERED"
  }
}
```

**Event Types**:
- `notification.sent`: Notification sent
- `notification.delivered`: Notification delivered
- `notification.failed`: Delivery failed
- `notification.opened`: Email/push opened
- `notification.clicked`: Action clicked

## Related Documentation

- [Customer API](customers.md) - Customer preferences
- [Order API](orders.md) - Order notifications
- [Logistics API](logistics.md) - Shipping notifications

---

**Last Updated**: 2025-10-25  
**API Version**: v1
