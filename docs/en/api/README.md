
# API 文檔

本目錄包含 API 相關的文檔。

## 文檔列表

- [API 版本Policy](API_VERSIONING_STRATEGY.md) - API 版本管理Policy
- [前端 API 整合](frontend-integration.md) - 前端 API 整合指南
- [Observability API](observability-api.md) - Observability系統 API 文檔

## API 端點

### 核心服務

- **後端 API**: <http://localhost:8080>
- **Swagger UI**: <http://localhost:8080/swagger-ui/index.html>
- **Health Check**: <http://localhost:8080/actuator/health>

### Observability端點

- **分析事件**: `POST /../api/analytics/events`
- **效能Metrics**: `POST /api/analytics/performance`
- **統計查詢**: `GET /api/analytics/stats`
- **WebSocket**: `WS /ws/analytics`
- **Monitoring事件**: `POST /api/monitoring/events`

## 適用對象

- API User
- 前端Developer
- 整合Developer

## related links

- [架構文檔](../architecture/) - 系統架構
- [開發指南](../development/) - 開發相關文檔
