# API Documentation

This directory contains API-related documentation.

## Documentation List

- [API Versioning Strategy](API_VERSIONING_STRATEGY.md) - API version management strategy
- [Frontend API Integration](frontend-integration.md) - Frontend API integration guide
- [Observability API](observability-api.md) - Observability system API documentation

## API Endpoints

### Core Services

- **Backend API**: <http://localhost:8080>
- **Swagger UI**: <http://localhost:8080/swagger-ui/index.html>
- **Health Check**: <http://localhost:8080/actuator/health>

### Observability Endpoints

- **Analytics Events**: `POST /api/analytics/events`
- **Performance Metrics**: `POST /api/analytics/performance`
- **Statistics Query**: `GET /api/analytics/stats`
- **WebSocket**: `WS /ws/analytics`
- **Monitoring Events**: `POST /api/monitoring/events`

## Target Audience

- API users
- Frontend developers
- Integration developers

## Related Links

- [Architecture Documentation](../architecture/) - System architecture
- [Development Guide](../development/) - Development-related documentation
