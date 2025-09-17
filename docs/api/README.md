# API 文檔

本目錄包含 API 相關的文檔。

## 文檔列表

- [API 版本策略](API_VERSIONING_STRATEGY.md) - API 版本管理策略
- [前端 API 整合](frontend-integration.md) - 前端 API 整合指南
- [可觀測性 API](observability-api.md) - 可觀測性系統 API 文檔

## API 端點

### 核心服務

- **後端 API**: <http://localhost:8080>
- **Swagger UI**: <http://localhost:8080/swagger-ui/index.html>
- **健康檢查**: <http://localhost:8080/actuator/health>

### 可觀測性端點

- **分析事件**: `POST /api/analytics/events`
- **效能指標**: `POST /api/analytics/performance`
- **統計查詢**: `GET /api/analytics/stats`
- **WebSocket**: `WS /ws/analytics`
- **監控事件**: `POST /api/monitoring/events`

## 適用對象

- API 使用者
- 前端開發者
- 整合開發者

## 相關連結

- [架構文檔](../architecture/) - 系統架構
- [開發指南](../development/) - 開發相關文檔
