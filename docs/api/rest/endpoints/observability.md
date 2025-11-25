# Observability & Monitoring API

> **Package**: `solid.humank.genaidemo.interfaces.web.monitoring` & `solid.humank.genaidemo.interfaces.web`
> **Base URL**: `/api/monitoring` & `/api/database`

This API provides endpoints for system monitoring, health checks, and database configuration.

## 1. Event Monitoring

### System Health
**GET** `/api/monitoring/events/health`

Returns the overall health status of the event processing system.

### Processing Statistics
**GET** `/api/monitoring/events/statistics/processing`

Returns detailed statistics about event processing.

### Retry Statistics
**GET** `/api/monitoring/events/statistics/retry`

Returns statistics about event retries.

### Backpressure Status
**GET** `/api/monitoring/events/backpressure/status`

Returns current backpressure status and load information.

### Sequence Statistics
**GET** `/api/monitoring/events/statistics/sequence`

Returns statistics about event sequence tracking.

### Reset Operations
- **POST** `/api/monitoring/events/statistics/processing/reset`: Reset processing stats.
- **POST** `/api/monitoring/events/sequence/{aggregateId}/reset`: Reset sequence for an aggregate.
- **POST** `/api/monitoring/events/sequence/{aggregateId}/update`: Force update sequence for an aggregate.

## 2. Database Health

### Database Health Check
**GET** `/api/database/health`

Checks database connection status and basic health information.

**Response**: `200 OK` or `503 Service Unavailable`

### Database Configuration
**GET** `/api/database/config`

Retrieves current database configuration and connection parameters.

### Validation Report
**GET** `/api/database/validation`

Retrieves a full validation report of database structure and configuration.
