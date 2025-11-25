# Analytics API

> **Package**: `solid.humank.genaidemo.interfaces.web.observability` & `solid.humank.genaidemo.interfaces.observability`
> **Base URL**: `/api/analytics` & `/api/v1/analytics/query`

The Analytics API provides endpoints for collecting user behavior events and querying analytical data.

## 1. Event Collection

### Receive Analytics Events Batch
**POST** `/api/analytics/events`

Receives a batch of user behavior analytics events from the frontend.

**Headers**:
- `X-Trace-Id` (Required): Trace ID for end-to-end tracing
- `X-Session-Id` (Required): Session ID
- `X-User-Id` (Optional): User ID

**Request Body**: `List<AnalyticsEventDto>`
```json
[
  {
    "eventType": "PAGE_VIEW",
    "timestamp": "2024-11-25T10:00:00Z",
    "pageUrl": "/home",
    "metadata": {
      "referrer": "google.com"
    }
  }
]
```

### Receive Performance Metrics Batch
**POST** `/api/analytics/performance`

Receives a batch of performance metrics (Core Web Vitals) from the frontend.

**Headers**: Same as above.

**Request Body**: `List<PerformanceMetricDto>`
```json
[
  {
    "metricName": "LCP",
    "value": 1200.5,
    "rating": "GOOD"
  }
]
```

### Health Check
**POST** `/api/analytics/health`

Checks the health status of the analytics service.

## 2. Analytics Query

### User Behavior Stats
**GET** `/api/v1/analytics/query/user-behavior`

Queries user behavior statistics.

**Parameters**:
- `userId` (Required): User ID
- `startTime` (Required): ISO Date Time
- `endTime` (Required): ISO Date Time

### Page Performance Stats
**GET** `/api/v1/analytics/query/page-performance`

Queries page performance statistics.

**Parameters**:
- `page` (Required): Page path
- `startTime` (Required): ISO Date Time
- `endTime` (Required): ISO Date Time

### Business Metrics Stats
**GET** `/api/v1/analytics/query/business-metrics`

Queries business metrics statistics.

**Parameters**:
- `startTime` (Required): ISO Date Time
- `endTime` (Required): ISO Date Time

### Session Stats
**GET** `/api/v1/analytics/query/session-stats`

Queries session statistics.

**Parameters**:
- `startTime` (Required): ISO Date Time
- `endTime` (Required): ISO Date Time

### Get Analytics Events
**GET** `/api/v1/analytics/query/events`

Paginated query for all analytics events.

### Get Events By Type
**GET** `/api/v1/analytics/query/events/by-type`

Queries events by type.

**Parameters**:
- `eventType` (Required): Event type
- `startTime` (Required): ISO Date Time
- `endTime` (Required): ISO Date Time

### Popular Pages
**GET** `/api/v1/analytics/query/popular-pages`

Queries popular pages ranking.

**Parameters**:
- `startTime` (Required): ISO Date Time
- `endTime` (Required): ISO Date Time
- `limit` (Optional, default 10): Limit results

### Activity Trends
**GET** `/api/v1/analytics/query/activity-trends`

Queries user activity trends.

**Parameters**:
- `startTime` (Required): ISO Date Time
- `endTime` (Required): ISO Date Time
