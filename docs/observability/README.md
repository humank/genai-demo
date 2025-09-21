# 可觀測性系統文檔

## 概覽

本專案實現了完整的企業級可觀測性系統，包含分散式追蹤、結構化日誌、業務指標收集和成本優化分析。

## 核心組件

### 🔍 分散式追蹤

- **AWS X-Ray**: 跨服務請求追蹤
- **Jaeger**: 本地開發環境追蹤
- **關聯 ID**: 統一的請求追蹤標識

### 📝 結構化日誌

- **Logback**: 統一日誌格式
- **PII 遮罩**: 敏感資料保護
- **CloudWatch**: 日誌聚合和分析

### 📊 業務指標

- **Micrometer**: 指標收集框架
- **CloudWatch**: 自定義業務指標
- **Prometheus**: 指標暴露端點

### 💰 成本優化

- **資源右調**: 自動化資源分析
- **成本追蹤**: 即時成本監控
- **優化建議**: 智能成本建議

## 快速開始

### 啟用可觀測性功能

```bash
# 啟動應用 (自動啟用可觀測性)
./gradlew bootRun

# 檢查健康狀態
curl http://localhost:8080/actuator/health

# 查看應用指標
curl http://localhost:8080/actuator/metrics

# 獲取成本優化建議
curl http://localhost:8080/api/cost-optimization/recommendations
```

### 配置環境變數

```bash
# AWS X-Ray 配置
export AWS_XRAY_TRACING_NAME=genai-demo
export AWS_XRAY_CONTEXT_MISSING=LOG_ERROR

# CloudWatch 配置
export CLOUDWATCH_NAMESPACE=GenAI/Demo
export CLOUDWATCH_REGION=us-east-1
```

## 詳細文檔

### 🎯 生產環境指南

- **[生產環境可觀測性測試指南](docs/en/observability/production-observability-testing-guide.md)** - 完整的生產環境測試策略和最佳實踐 (67頁)

### 📚 前端後端整合文檔

- **[配置指南](configuration-guide.md)** - 環境差異化配置和 MSK 主題設定
- **[故障排除指南](../troubleshooting/observability-troubleshooting.md)** - 常見問題診斷和解決方案
- **[部署指南](../deployment/observability-deployment.md)** - 完整的部署流程和驗證
- **[API 文檔](../api/observability-api.md)** - 可觀測性 API 端點詳細說明

### 📚 實現文檔

- \1
- \1
- \1

### 🔧 測試策略

- **開發階段**: Java 集成測試和單元測試
- **CI/CD 階段**: 腳本化驗證和 SLI/SLO 檢查
- **生產階段**: Synthetic Monitoring 和 Chaos Engineering
- **持續改進**: 自動化報告和手動分析

### 🌟 業界最佳實踐

- Bash/Python 腳本測試
- K6 負載測試
- Terraform 基礎設施測試
- DataDog Synthetic Tests
- Chaos Monkey 韌性測試
