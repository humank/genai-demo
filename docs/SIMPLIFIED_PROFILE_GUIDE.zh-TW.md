# 簡化的 Profile 架構指南

## 🎯 **實用的 3-Profile 設計**

基於實際工作流程，我們採用簡化但實用的 profile 架構：

### **Profile 架構總覽**

| Profile | 環境 | 用途 | 資料庫 | Redis | Kafka | 部署位置 |
|---------|------|------|--------|-------|-------|----------|
| `local` | 本機 | 開發+測試 | H2 | Single/Sentinel | 禁用 | 本機 |
| `staging` | AWS | 整合測試 | RDS | ElastiCache | MSK | AWS Tokyo |
| `production` | AWS | 生產環境 | RDS | ElastiCache Cluster | MSK | AWS Tokyo |

## 🚀 **實際工作流程**

### **1. 本機開發階段**
```bash
# 日常開發
export SPRING_PROFILES_ACTIVE=local
./gradlew bootRun

# 本機測試 (使用相同 profile)
./gradlew test  # 自動使用 test profile (最小化配置)
```

**特性：**
- H2 記憶體資料庫 (快速重啟)
- 可選 Redis (單機或 HA 測試)
- 記憶體事件處理
- 完整的開發工具 (H2 Console, OpenAPI)

### **2. AWS Staging 階段**
```bash
# 部署到 AWS 進行整合測試
export SPRING_PROFILES_ACTIVE=staging
```

**特性：**
- 真實的 AWS 服務 (RDS, ElastiCache, MSK)
- 完整的監控和追蹤
- 生產環境的完整模擬
- UAT 和整合測試

### **3. AWS Production 階段**
```bash
# 生產部署
export SPRING_PROFILES_ACTIVE=production
```

**特性：**
- 高可用性配置
- 完整的安全性設定
- 生產級監控和告警
- 災難恢復機制

## 💡 **為什麼這樣設計？**

### **合併 Development + Test 的理由**

1. **實際工作流程**: 你在本機同時進行開發和測試
2. **資源效率**: 避免維護多個相似的配置
3. **簡化管理**: 減少 profile 切換的複雜性
4. **快速反饋**: 本機環境統一，問題更容易重現

### **保留 Test Profile 的理由**

1. **CI/CD 需求**: 自動化測試需要最小化配置
2. **隔離性**: 測試不應該依賴外部服務
3. **速度**: 測試環境需要快速啟動和關閉

## 🔧 **配置檔案結構**

```
app/src/main/resources/
├── application.yml              # 基礎配置
├── application-local.yml        # 本機開發+測試
├── application-staging.yml      # AWS 預發布
├── application-production.yml   # AWS 生產
├── application-msk.yml         # MSK 專用配置
└── application-openapi.yml     # OpenAPI 配置

app/src/test/resources/
└── application-test.yml        # CI/CD 測試專用 (最小化)
```

## 🎮 **使用範例**

### **本機開發**
```bash
# 啟動 Redis
./scripts/redis-dev.sh start-single

# 設定環境
export SPRING_PROFILES_ACTIVE=local
export REDIS_MODE=SINGLE

# 啟動應用
./gradlew bootRun

# 執行測試
./gradlew test  # 自動使用 test profile
```

### **Redis HA 測試**
```bash
# 啟動 HA 環境
./scripts/redis-dev.sh start-ha

# 設定環境
export SPRING_PROFILES_ACTIVE=local
export REDIS_MODE=SENTINEL
export REDIS_SENTINEL_NODES=localhost:26379,localhost:26380,localhost:26381

# 啟動應用並測試 failover
./gradlew bootRun
```

### **AWS 部署**
```bash
# Staging 部署
export SPRING_PROFILES_ACTIVE=staging
export REDIS_MODE=CLUSTER
export REDIS_CLUSTER_NODES=your-staging-cluster

# Production 部署
export SPRING_PROFILES_ACTIVE=production
export REDIS_MODE=CLUSTER
export REDIS_CLUSTER_NODES=your-production-cluster
```

## 🔍 **Profile 選擇決策樹**

```
你在哪裡？
├── 本機
│   ├── 開發新功能 → local
│   ├── 本機測試 → local
│   └── 單元測試 → test (自動)
├── AWS 環境
│   ├── 整合測試 → staging
│   ├── UAT 測試 → staging
│   └── 生產部署 → production
```

## 📊 **Profile 比較**

| 功能 | local | test | staging | production |
|------|-------|------|---------|------------|
| 資料庫 | H2 | H2 | RDS | RDS |
| Redis | 可選 | 禁用 | ElastiCache | ElastiCache Cluster |
| Kafka | 禁用 | 禁用 | MSK | MSK |
| 監控 | 基本 | 禁用 | 完整 | 完整 |
| 安全性 | 寬鬆 | 禁用 | 嚴格 | 最嚴格 |
| 啟動速度 | 快 | 最快 | 中等 | 慢 |

## 🎯 **最佳實踐**

### **本機開發**
- 使用 `local` profile 進行所有本機工作
- 根據需要啟用/禁用 Redis
- 利用 H2 Console 進行資料庫除錯

### **測試策略**
- 單元測試自動使用 `test` profile
- 整合測試可以使用 `local` 或 `staging`
- E2E 測試建議使用 `staging`

### **部署策略**
- 先部署到 `staging` 進行驗證
- 通過所有測試後部署到 `production`
- 使用環境變數管理不同環境的配置

---

**更新日期**: 2025年9月24日 上午8:57 (台北時間)  
**維護者**: 開發團隊  
**版本**: 2.0.0 (簡化版)