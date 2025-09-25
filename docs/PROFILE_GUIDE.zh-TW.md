# Spring Boot Profile 使用指南

## 📋 **標準化 Profile 架構**

### **Profile 命名標準**

根據 Spring Boot 最佳實踐，我們使用以下標準 profile：

| Profile | 用途 | 環境 | 資料庫 | Redis | Kafka/MSK |
|---------|------|------|--------|-------|-----------|
| `development` | 本地開發 | Local | H2 | Single/Sentinel | 禁用 |
| `test` | 自動化測試 | CI/CD | H2 | 禁用 | 禁用 |
| `staging` | 預發布環境 | Kubernetes | PostgreSQL | ElastiCache/EKS | MSK |
| `production` | 生產環境 | AWS | PostgreSQL | ElastiCache Cluster | MSK |

### **Profile 組合策略**

Spring Boot 支援 profile 組合，我們的配置如下：

```yaml
spring:
  profiles:
    group:
      development: "development,openapi"
      test: "test,openapi"
      staging: "staging,openapi,msk"
      production: "production,openapi,msk"
```

## 🚀 **使用方式**

### **1. 本地開發環境**

```bash
# 基本開發環境
export SPRING_PROFILES_ACTIVE=development
./gradlew bootRun

# 或者直接指定
./gradlew bootRun --args='--spring.profiles.active=development'
```

**特性：**
- H2 記憶體資料庫
- 單一 Redis 實例
- 記憶體事件處理
- OpenAPI 文檔啟用
- 詳細日誌輸出

### **2. 測試環境**

```bash
# 執行測試
./gradlew test

# 手動指定測試 profile
./gradlew test -Dspring.profiles.active=test
```

**特性：**
- H2 記憶體資料庫
- Redis 完全禁用
- 記憶體事件處理
- 最小化日誌輸出
- 快速啟動

### **3. 預發布環境 (Staging)**

```bash
# Kubernetes 部署
export SPRING_PROFILES_ACTIVE=staging
# 配合 Kubernetes ConfigMap 和 Secret
```

**特性：**
- PostgreSQL 資料庫
- ElastiCache 或 EKS Redis
- MSK Kafka 整合
- AWS X-Ray 追蹤
- 生產級監控

### **4. 生產環境**

```bash
# 生產部署
export SPRING_PROFILES_ACTIVE=production
```

**特性：**
- PostgreSQL 資料庫
- ElastiCache Cluster
- MSK Kafka 整合
- 完整可觀測性
- 安全性強化

## 🔧 **Redis 配置策略**

### **Development Profile**

```bash
# 單一 Redis (預設)
export REDIS_MODE=SINGLE
./scripts/redis-dev.sh start-single

# HA 測試
export REDIS_MODE=SENTINEL
export REDIS_SENTINEL_NODES=localhost:26379,localhost:26380,localhost:26381
./scripts/redis-dev.sh start-ha
```

### **Staging/Production Profile**

```bash
# ElastiCache Cluster
export REDIS_MODE=CLUSTER
export REDIS_CLUSTER_NODES=your-cluster-endpoint

# EKS Redis Sentinel
export REDIS_MODE=SENTINEL
export REDIS_SENTINEL_NODES=sentinel-1:26379,sentinel-2:26379,sentinel-3:26379
```

## 📁 **配置檔案結構**

```
app/src/main/resources/
├── application.yml                 # 基礎配置
├── application-development.yml     # 開發環境
├── application-test.yml           # 測試環境
├── application-staging.yml        # 預發布環境
├── application-production.yml     # 生產環境
├── application-msk.yml           # MSK 專用配置
└── application-openapi.yml       # OpenAPI 配置

app/src/test/resources/
└── application-test.yml          # 測試專用配置
```

## 🎯 **最佳實踐**

### **1. Profile 選擇原則**

- **開發階段**: 使用 `development`
- **單元測試**: 自動使用 `test`
- **整合測試**: 使用 `staging` 或 `test`
- **生產部署**: 使用 `production`

### **2. 環境變數管理**

```bash
# .env 檔案 (本地開發)
SPRING_PROFILES_ACTIVE=development
REDIS_MODE=SINGLE

# Kubernetes ConfigMap (staging/production)
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
data:
  SPRING_PROFILES_ACTIVE: "staging"
  REDIS_MODE: "CLUSTER"
```

### **3. 條件式 Bean 配置**

```java
@Component
@Profile("development")
public class DevelopmentService {
    // 僅在開發環境啟用
}

@Component
@Profile({"staging", "production"})
public class ProductionService {
    // 僅在預發布和生產環境啟用
}
```

## 🔍 **故障排除**

### **常見問題**

#### 1. Profile 未正確載入
```bash
# 檢查當前 profile
curl http://localhost:8080/actuator/env | jq '.activeProfiles'

# 或查看日誌
grep "The following profiles are active" logs/application.log
```

#### 2. Redis 連線失敗
```bash
# 檢查 Redis 狀態
./scripts/redis-dev.sh status

# 測試連線
./scripts/redis-dev.sh test
```

#### 3. 配置衝突
```bash
# 檢查配置屬性
curl http://localhost:8080/actuator/configprops
```

## 📊 **Profile 驗證清單**

### **開發環境檢查**
- [ ] H2 Console 可存取: http://localhost:8080/h2-console
- [ ] Redis 連線正常
- [ ] OpenAPI 文檔可用: http://localhost:8080/swagger-ui.html
- [ ] 健康檢查通過: http://localhost:8080/actuator/health

### **測試環境檢查**
- [ ] 所有測試通過
- [ ] 無外部依賴
- [ ] 快速啟動 (< 30 秒)
- [ ] 記憶體使用合理

### **生產環境檢查**
- [ ] 資料庫連線正常
- [ ] Redis Cluster 連線正常
- [ ] MSK Kafka 連線正常
- [ ] 監控指標正常
- [ ] 安全配置啟用

---

**更新日期**: 2025年9月24日 上午8:40 (台北時間)  
**維護者**: 開發團隊  
**版本**: 2.0.0