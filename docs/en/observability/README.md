
# Observability系統文檔

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
curl http://localhost:8080/../api/cost-optimization/recommendations
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

- **[生產EnvironmentObservability測試指南](production-observability-testing-guide.md)** - 完整的生產Environment測試Policy和Best Practice (67頁)

### 📚 前端後端整合文檔

- **[配置指南](configuration-guide.md)** - Environment差異化配置和 MSK 主題設定
- **[故障排除指南](../troubleshooting/observability-troubleshooting.md)** - 常見問題診斷和解決方案
- **[Deployment指南](../deployment/observability-deployment.md)** - 完整的Deployment流程和驗證
- **[API 文檔](../../api/observability-api.md)** - Observability API 端點詳細說明

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
