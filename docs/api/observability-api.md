# Observability API Documentation

## Overview

This document describes the API endpoints for the frontend-backend observability integration system, including user behavior tracking, performance monitoring, and business analytics functionality.

## New API Endpoints

### Analytics Events API

#### POST /api/analytics/events

Receives user behavior analytics events sent from the frontend.

**Request Headers**

```
Content-Type: application/json
X-Trace-Id: string (required) - Trace ID for end-to-end tracking
X-Session-Id: string (required) - Session ID
X-Correlation-Id: string (optional) - Correlation ID
```

**Request Body**

```json
[
  {
    "eventId": "string",
    "eventType": "page_view|user_action|business_event",
    "sessionId": "string",
    "userId": "string (optional)",
    "traceId": "string",
    "timestamp": 1640995200000,
    "data": {
      "page": "/products",
      "action": "click",
      "element": "add-to-cart-button",
      "productId": "PROD-123",
      "category": "electronics"
    }
  }
]
```

**Response**

- **200 OK**: Events successfully received
- **400 Bad Request**: Invalid request format
- **500 Internal Server Error**: Internal server error

**Example Request**

```bash
curl -X POST http://localhost:8080/api/analytics/events \
  -H "Content-Type: application/json" \
  -H "X-Trace-Id: trace-1640995200000-abc123" \
  -H "X-Session-Id: session-xyz789" \
  -d '[
    {
      "eventId": "event-001",
      "eventType": "page_view",
      "sessionId": "session-xyz789",
      "traceId": "trace-1640995200000-abc123",
      "timestamp": 1640995200000,
      "data": {
        "page": "/products",
        "referrer": "/home",
        "userAgent": "Mozilla/5.0..."
      }
    }
  ]'
```

#### POST /api/analytics/performance

Receives performance metrics data sent from the frontend.

**Request Headers**

```
Content-Type: application/json
X-Trace-Id: string (required) - Trace ID
X-Session-Id: string (required) - Session ID
```

**Request Body**

```json
[
  {
    "metricId": "string",
    "metricType": "lcp|fid|cls|ttfb|page_load",
    "value": 1500.5,
    "page": "/products",
    "sessionId": "string",
    "traceId": "string",
    "timestamp": 1640995200000,
    "metadata": {
      "viewport": "1920x1080",
      "connection": "4g",
      "device": "desktop"
    }
  }
]
```

**Response**

- **200 OK**: Metrics successfully received
- **400 Bad Request**: Invalid request format
- **500 Internal Server Error**: Internal server error

**Example Request**

```bash
curl -X POST http://localhost:8080/api/analytics/performance \
  -H "Content-Type: application/json" \
  -H "X-Trace-Id: trace-1640995200000-abc123" \
  -H "X-Session-Id: session-xyz789" \
  -d '[
    {
      "metricId": "metric-001",
      "metricType": "lcp",
      "value": 1200.5,
      "page": "/products",
      "sessionId": "session-xyz789",
      "traceId": "trace-1640995200000-abc123",
      "timestamp": 1640995200000
    }
  ]'
```

#### GET /api/analytics/stats

Query analytics statistics data.

**Request Parameters**

- `timeRange` (required): Time range (1h, 24h, 7d, 30d)
- `filter` (optional): Filter conditions
- `page` (optional): Page number, default 0
- `size` (optional): Page size, default 20

**Request Headers**

```
X-Trace-Id: string (optional) - Trace ID
```

**Response**

```json
{
  "timeRange": "24h",
  "totalEvents": 15420,
  "uniqueUsers": 1250,
  "pageViews": 8900,
  "userActions": 6520,
  "averageSessionDuration": 180000,
  "topPages": [
    {
      "page": "/products",
      "views": 3200,
      "uniqueUsers": 890
    }
  ],
  "performanceMetrics": {
    "averageLCP": 1200.5,
    "averageFID": 85.2,
    "averageCLS": 0.05
  }
}
```

### Monitoring Events API (Existing Endpoint Extension)

#### POST /api/monitoring/events

Receives frontend JavaScript errors and monitoring events.

**Request Body**

```json
{
  "eventType": "javascript_error|api_error|network_error",
  "message": "string",
  "stack": "string (optional)",
  "url": "string (optional)",
  "userAgent": "string (optional)",
  "timestamp": 1640995200000,
  "sessionId": "string",
  "traceId": "string",
  "metadata": {
    "component": "ProductList",
    "action": "loadProducts",
    "errorCode": "NETWORK_TIMEOUT"
  }
}
```

### WebSocket Real-time Analytics API

#### WS /ws/analytics

Establish WebSocket connection to receive real-time analytics updates.

**Connection Parameters**

- `sessionId`: Session ID
- `channels`: List of subscribed channels (user-behavior, performance, business-metrics)

**Message Format**

```json
{
  "type": "analytics_update",
  "channel": "user-behavior",
  "data": {
    "eventType": "page_view",
    "count": 1,
    "timestamp": 1640995200000,
    "metadata": {
      "page": "/products",
      "totalViews": 3201
    }
  }
}
```

**Example JavaScript Client**

```javascript
const socket = new WebSocket('ws://localhost:8080/ws/analytics?sessionId=session-xyz789&channels=user-behavior,performance');

socket.onmessage = function(event) {
  const data = JSON.parse(event.data);
  console.log('Real-time update:', data);
};
```

## Data Models

### AnalyticsEventDto

```java
public record AnalyticsEventDto(
    @NotBlank String eventId,
    @NotBlank String eventType,
    @NotBlank String sessionId,
    String userId,
    @NotBlank String traceId,
    @NotNull Long timestamp,
    @NotNull Map<String, Object> data
) {}
```

### PerformanceMetricDto

```java
public record PerformanceMetricDto(
    @NotBlank String metricId,
    @NotBlank String metricType,
    @NotNull Double value,
    @NotBlank String page,
    @NotBlank String sessionId,
    @NotBlank String traceId,
    @NotNull Long timestamp,
    Map<String, Object> metadata
) {}
```

### AnalyticsStatsDto

```java
public record AnalyticsStatsDto(
    String timeRange,
    Long totalEvents,
    Long uniqueUsers,
    Long pageViews,
    Long userActions,
    Long averageSessionDuration,
    List<PageStatsDto> topPages,
    PerformanceStatsDto performanceMetrics
) {}
```

## Error Handling

### Error Response Format

```json
{
  "errorCode": "ANALYTICS_VALIDATION_ERROR",
  "message": "Invalid event data format",
  "timestamp": "2024-01-01T12:00:00Z",
  "traceId": "trace-1640995200000-abc123",
  "details": {
    "field": "eventType",
    "rejectedValue": "invalid_type",
    "allowedValues": ["page_view", "user_action", "business_event"]
  }
}
```

### Common Error Codes

- `ANALYTICS_VALIDATION_ERROR`: Data validation failed
- `ANALYTICS_PROCESSING_ERROR`: Event processing failed
- `ANALYTICS_RATE_LIMIT_EXCEEDED`: Request rate limit exceeded
- `ANALYTICS_TRACE_ID_MISSING`: Missing trace ID
- `ANALYTICS_SESSION_INVALID`: Invalid session ID

## Security Considerations

### Authentication and Authorization

- All API endpoints require valid sessions
- Trace IDs are used for request correlation and contain no sensitive information
- User data is encrypted during transmission and storage

### Data Privacy

- PII data is automatically masked
- User behavior data is anonymized
- Complies with GDPR and other privacy regulations

### Rate Limiting

- Maximum 1000 events per session per minute
- Batch requests can contain up to 100 events
- WebSocket connections limited to 5 per session

## Performance Considerations

### Batch Processing

- Frontend recommended to send every 30 seconds or after accumulating 50 events
- Backend supports batch processing for improved performance
- Critical events (like purchases) can be sent immediately

### Caching Strategy

- Statistics query results cached for 5 minutes
- Real-time metrics cached for 30 seconds
- Uses Redis for distributed caching

### Monitoring Metrics

- API response time < 200ms (95th percentile)
- Event processing latency < 100ms
- WebSocket connection stability > 99.9%

## Testing

### Unit Tests

```bash
# Run API controller tests
./gradlew test --tests "*AnalyticsController*"

# Run service layer tests
./gradlew test --tests "*ObservabilityEventService*"
```

### Integration Tests

```bash
# Run complete observability integration tests
./gradlew test --tests "*ObservabilityIntegration*"
```

### Load Testing

```bash
# Use K6 for load testing
k6 run scripts/load-test-analytics.js
```

## Deployment Configuration

### Development Environment

```yaml
genai-demo:
  events:
    publisher: in-memory
  observability:
    analytics:
      enabled: true
      batch-size: 10
      flush-interval: 10s
```

### Production Environment

```yaml
genai-demo:
  events:
    publisher: kafka
  observability:
    analytics:
      enabled: true
      batch-size: 100
      flush-interval: 30s
      retention-days: 90
```

## Troubleshooting

### Common Issues

1. **Events not being processed**
   - Check trace ID format
   - Verify session ID validity
   - Review application logs

2. **WebSocket connection failures**
   - Check network connectivity
   - Verify session parameters
   - Review browser console errors

3. **Performance metrics anomalies**
   - Check metric type spelling
   - Verify value ranges
   - Confirm page URL format

### Log Queries

```bash
# View analytics event processing logs
kubectl logs -f deployment/genai-demo-backend | grep "analytics"

# View error logs
kubectl logs -f deployment/genai-demo-backend | grep "ERROR.*observability"
```

## Related Documentation

- [Observability Configuration Guide](../observability/configuration-guide.md)
- [Observability Overview](../viewpoints/operational/observability-overview.md)
- [Troubleshooting Guide](../troubleshooting/observability-troubleshooting.md)
- [Deployment Guide](../deployment/observability-deployment.md)
