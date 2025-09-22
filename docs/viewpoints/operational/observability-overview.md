# 可觀測性系統概覽

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
curl http://localhost:8080/../api/cost-optimization/recommendations
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

- **[生產環境可觀測性測試指南](production-observability-testing-guide.md)** - 完整的生產環境測試策略和最佳實踐

### 📚 前端後端整合文檔

- **[配置指南](configuration-guide.md)** - 環境差異化配置和 MSK 主題設定
- **[故障排除指南](../../troubleshooting/observability-troubleshooting.md)** - 常見問題診斷和解決方案
- **[部署指南](../deployment/observability-deployment.md)** - 完整的部署流程和驗證
- **API 文檔** - 可觀測性 API 端點詳細說明

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

## 系統架構

### 可觀測性技術棧

```
┌─────────────────────────────────────────────────────────────┐
│                    可觀測性系統架構                          │
├─────────────────────────────────────────────────────────────┤
│  應用層                                                      │
│  ├── Spring Boot Actuator (健康檢查、指標)                   │
│  ├── Micrometer (指標收集)                                   │
│  ├── OpenTelemetry (分散式追蹤)                              │
│  └── Logback + MDC (結構化日誌)                              │
├─────────────────────────────────────────────────────────────┤
│  AWS 可觀測性服務                                            │
│  ├── CloudWatch (指標、日誌、告警)                           │
│  ├── X-Ray (分散式追蹤)                                      │
│  ├── CloudWatch Insights (日誌分析)                          │
│  └── CloudWatch Dashboards (可視化)                          │
├─────────────────────────────────────────────────────────────┤
│  第三方工具 (可選)                                           │
│  ├── Prometheus + Grafana                                   │
│  ├── ELK Stack (Elasticsearch, Logstash, Kibana)           │
│  └── Jaeger (追蹤可視化)                                     │
└─────────────────────────────────────────────────────────────┘
```

## 監控策略

### 三大支柱

1. **指標 (Metrics)**
   - 業務指標: 訂單數量、收入、轉換率
   - 系統指標: CPU、記憶體、網路、磁碟
   - 應用指標: 響應時間、錯誤率、吞吐量

2. **日誌 (Logs)**
   - 結構化日誌: JSON 格式、統一標準
   - 關聯 ID: 請求追蹤和問題定位
   - 敏感資料遮罩: PII 和機密資訊保護

3. **追蹤 (Traces)**
   - 分散式追蹤: 跨服務請求追蹤
   - 性能分析: 瓶頸識別和優化
   - 錯誤分析: 異常傳播和根因分析

### SLI/SLO 管理

- **可用性**: 99.9% 系統正常運行時間
- **延遲**: 95% 的請求在 2 秒內完成
- **錯誤率**: 小於 0.1% 的請求失敗
- **吞吐量**: 支援每秒 1000 個請求

## 成本優化

### 採樣策略

- **智能採樣**: 錯誤請求 100% 採樣，正常請求 10% 採樣
- **成本控制**: 追蹤成本控制在運營成本的 2% 以內
- **數據保留**: 自動清理過期數據以降低存儲成本

### 資源優化

- **右調建議**: 基於實際使用情況的資源建議
- **成本監控**: 即時成本追蹤和告警
- **預算管理**: 設定成本預算和自動控制

## 安全和合規

### 數據保護

- **PII 遮罩**: 自動遮罩個人識別資訊
- **敏感資料**: 密碼、API 金鑰等不記錄在日誌中
- **存取控制**: 基於角色的存取控制
- **數據加密**: 傳輸和靜態數據加密

### 合規性

- **審計日誌**: 完整的操作審計記錄
- **數據保留**: 符合法規的數據保留政策
- **隱私保護**: 符合 GDPR 等隱私法規
- **合規報告**: 自動生成合規報告

## 相關圖表

- \1
- \1
- \1

## 與其他視點的關聯

- **[部署視點](../deployment/README.md)**: 部署過程中的監控整合
- **[開發視點](../development/README.md)**: 開發階段的可觀測性實踐
- **[並發視點](../concurrency/README.md)**: 並發系統的監控和調優
- **[功能視點](../functional/README.md)**: 業務功能的監控指標

## 相關文檔

- [配置指南](configuration-guide.md) - 詳細的配置說明
- [生產環境測試指南](production-observability-testing-guide.md) - 生產環境測試策略
- [故障排除指南](../../troubleshooting/observability-troubleshooting.md) - 問題診斷和解決
- [部署指南](../deployment/observability-deployment.md) - 部署流程和驗證