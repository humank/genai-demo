
# Overview

## Overview

This project實現了完整的企業級Observability系統，包含分散式Tracing、結構化Logging、業務Metrics收集和成本優化分析。

## 核心組件

### 🔍 分散式Tracing

- **AWS X-Ray**: 跨服務請求Tracing
- **Jaeger**: 本地開發EnvironmentTracing
- **關聯 ID**: 統一的請求Tracing標識

### 📝 結構化Logging

- **Logback**: 統一Logging格式
- **PII 遮罩**: 敏感資料保護
- **CloudWatch**: LoggingAggregate和分析

### 📊 業務Metrics

- **Micrometer**: Metrics收集框架
- **CloudWatch**: 自定義業務Metrics
- **Prometheus**: Metrics暴露端點

### 💰 成本優化

- **Resource右調**: 自動化Resource分析
- **成本Tracing**: 即時成本Monitoring
- **優化recommendations**: 智能成本recommendations

## 快速開始

### 啟用Observability功能

```bash
# 啟動應用 (自動啟用Observability)
./gradlew bootRun

# 檢查健康狀態
curl http://localhost:8080/actuator/health

# 查看應用Metrics
curl http://localhost:8080/actuator/metrics

# 獲取成本優化recommendations
curl http://localhost:8080/api/cost-optimization/recommendations
```

### 配置Environment變數

```bash
# AWS X-Ray 配置
export AWS_XRAY_TRACING_NAME=genai-demo
export AWS_XRAY_CONTEXT_MISSING=LOG_ERROR

# CloudWatch 配置
export CLOUDWATCH_NAMESPACE=GenAI/Demo
export CLOUDWATCH_REGION=us-east-1
```

## 詳細文檔

### Guidelines

- **[生產EnvironmentObservability測試指南](production-observability-testing-guide.md)** - 完整的生產Environment測試Policy和Best Practice

### 📚 前端後端整合文檔

- **[配置指南](configuration-guide.md)** - Environment差異化配置和 MSK 主題設定
- **[故障排除指南](docs/troubleshooting/observability-troubleshooting.md)** - 常見問題診斷和解決方案
- **[Deployment指南](../deployment/observability-deployment.md)** - 完整的Deployment流程和驗證
- **[API 文檔](docs/api/observability-api.md)** - Observability API 端點詳細說明

### 📚 實現文檔

- \1
- \1
- \1

### Testing

- **開發階段**: Java 集成測試和Unit Test
- **CI/CD 階段**: 腳本化驗證和 SLI/SLO 檢查
- **生產階段**: Synthetic Monitoring 和 Chaos Engineering
- **持續改進**: 自動化報告和手動分析

### Best Practices

- Bash/Python 腳本測試
- K6 Load Test
- Terraform 基礎設施測試
- DataDog Synthetic Tests
- Chaos Monkey Resilience測試

## 系統架構

### Observability技術棧

```
┌─────────────────────────────────────────────────────────────┐
│                    Observability系統架構                          │
├─────────────────────────────────────────────────────────────┤
│  Application Layer                                                      │
│  ├── Spring Boot Actuator (Health Check、Metrics)                   │
│  ├── Micrometer (Metrics收集)                                   │
│  ├── OpenTelemetry (分散式Tracing)                              │
│  └── Logback + MDC (結構化Logging)                              │
├─────────────────────────────────────────────────────────────┤
│  AWS Observability服務                                            │
│  ├── CloudWatch (Metrics、Logging、告警)                           │
│  ├── X-Ray (分散式Tracing)                                      │
│  ├── CloudWatch Insights (Logging分析)                          │
│  └── CloudWatch Dashboards (可視化)                          │
├─────────────────────────────────────────────────────────────┤
│  第三方工具 (可選)                                           │
│  ├── Prometheus + Grafana                                   │
│  ├── ELK Stack (Elasticsearch, Logstash, Kibana)           │
│  └── Jaeger (Tracing可視化)                                     │
└─────────────────────────────────────────────────────────────┘
```

## MonitoringPolicy

### 三大支柱

1. **Metrics (Metrics)**
   - 業務Metrics: 訂單數量、收入、轉換率
   - 系統Metrics: CPU、記憶體、網路、磁碟
   - 應用Metrics: 響應時間、錯誤率、吞吐量

2. **Logging (Logs)**
   - 結構化Logging: JSON 格式、統一標準
   - 關聯 ID: 請求Tracing和問題定位
   - 敏感資料遮罩: PII 和機密資訊保護

3. **Tracing (Traces)**
   - 分散式Tracing: 跨服務請求Tracing
   - Performance分析: 瓶頸識別和優化
   - 錯誤分析: 異常傳播和根因分析

### SLI/SLO 管理

- **Availability**: 99.9% 系統正常運行時間
- **延遲**: 95% 的請求在 2 秒內完成
- **錯誤率**: 小於 0.1% 的請求失敗
- **吞吐量**: 支援每秒 1000 個請求

## 成本優化

### 採樣Policy

- **智能採樣**: 錯誤請求 100% 採樣，正常請求 10% 採樣
- **成本控制**: Tracing成本控制在運營成本的 2% 以內
- **數據保留**: 自動清理過期數據以降低存儲成本

### Resources

- **右調recommendations**: 基於實際使用情況的Resourcerecommendations
- **成本Monitoring**: 即時成本Tracing和告警
- **預算管理**: 設定成本預算和自動控制

## 安全和合規

### 數據保護

- **PII 遮罩**: 自動遮罩個人識別資訊
- **敏感資料**: 密碼、API 金鑰等不記錄在Logging中
- **存取控制**: 基於角色的存取控制
- **數據加密**: 傳輸和靜態數據加密

### 合規性

- **審計Logging**: 完整的操作審計記錄
- **數據保留**: 符合法規的數據保留政策
- **隱私保護**: 符合 GDPR 等隱私法規
- **合規報告**: 自動生成合規報告

## Related Diagrams

- \1
- \1
- \1

## Relationships with Other Viewpoints

- **[Deployment Viewpoint](../deployment/README.md)**: Deployment過程中的Monitoring整合
- **[Development Viewpoint](../development/README.md)**: 開發階段的Observability實踐
- **[Concurrency Viewpoint](../concurrency/README.md)**: 並發系統的Monitoring和調優
- **[Functional Viewpoint](../functional/README.md)**: 業務功能的MonitoringMetrics

## 相關文檔

- [配置指南](configuration-guide.md) - 詳細的配置說明
- [生產Environment測試指南](production-observability-testing-guide.md) - 生產Environment測試Policy
- [故障排除指南](docs/troubleshooting/observability-troubleshooting.md) - 問題診斷和解決
- [Deployment指南](../deployment/observability-deployment.md) - Deployment流程和驗證