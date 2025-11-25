# Profiles & Activity API

> **Package**: `solid.humank.genaidemo.interfaces.web` & `solid.humank.genaidemo.interfaces.web.activity`
> **Base URL**: `/api/profile` & `/api/activities`

This API provides access to user profiles, system configuration, and activity logs.

## 1. Profile Information

### Get Profile Info
**GET** `/api/profile/info`

Retrieves current active profiles and configuration details.

**Response**:
```json
{
  "activeProfiles": ["prod", "aws"],
  "defaultProfiles": ["default"],
  "profileName": "Production",
  "profileDescription": "Production environment configuration",
  "features": {
    "h2Console": false,
    "debugLogging": false,
    "kafkaEvents": true
  },
  "database": {
    "url": "jdbc:postgresql://...",
    "driverClass": "org.postgresql.Driver"
  },
  "eventing": {
    "kafkaEnabled": true
  }
}
```

## 2. Activity Logs

### Get Activities
**GET** `/api/activities`

Retrieves a list of system activities (e.g., order updates, payments).

**Parameters**:
- `limit` (Optional, default 10): Limit the number of returned activities.

**Response**:
```json
{
  "success": true,
  "data": [
    {
      "id": "order-123",
      "type": "order",
      "title": "Order Status Update",
      "description": "Order #123 updated",
      "timestamp": "1 day ago",
      "status": "info"
    }
  ]
}
```
