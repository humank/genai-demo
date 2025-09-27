# 資料庫配置對照表

## 📊 **完整的資料庫配置矩陣**

### **資料庫配置總覽**

| 配置項目 | Local | Test | Staging | Production |
|----------|-------|------|---------|------------|
| **資料庫類型** | H2 記憶體 | H2 記憶體 | PostgreSQL (RDS) | PostgreSQL (RDS Multi-AZ) |
| **連線方式** | Embedded | Embedded | JDBC | JDBC |
| **Schema 管理** | create-drop | create-drop | validate | validate |
| **Migration** | 禁用 | 禁用 | Flyway 啟用 | Flyway 啟用 |
| **連線池** | 10 max, 2 min | 5 max, 1 min | 20 max, 5 min | 30 max, 10 min |
| **SQL 日誌** | 啟用 (DEBUG) | 禁用 | 禁用 | 禁用 |
| **快取** | 無 | 無 | 無 | 二級快取啟用 |
| **批次處理** | 預設 | 預設 | 20 | 25 |

## 🔧 **詳細配置分析**

### **1. Local Profile - H2 記憶體資料庫**

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:genaidemo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password: ""
    hikari:
      maximum-pool-size: 10      # 適合本機開發
      minimum-idle: 2            # 最小連線數
      connection-timeout: 20000  # 20秒連線超時
      idle-timeout: 300000       # 5分鐘閒置超時
      max-lifetime: 1200000      # 20分鐘最大生命週期
      leak-detection-threshold: 60000  # 1分鐘洩漏檢測

  jpa:
    hibernate:
      ddl-auto: create-drop      # 每次啟動重建 schema
    show-sql: true               # 顯示 SQL (開發除錯)
    properties:
      hibernate:
        format_sql: true         # 格式化 SQL 輸出
        use_sql_comments: true   # 顯示 SQL 註解
        dialect: org.hibernate.dialect.H2Dialect
    defer-datasource-initialization: true  # 延遲初始化

  h2:
    console:
      enabled: true              # 啟用 H2 Console
      path: /h2-console          # Console 路徑
      settings:
        web-allow-others: true   # 允許遠端存取

  flyway:
    enabled: false               # 禁用 (避免循環依賴)
    locations: classpath:db/migration/h2
```

**特性：**
- ✅ 快速啟動 (< 5 秒)
- ✅ 無需外部資料庫
- ✅ 支援 SQL 除錯
- ✅ H2 Console 可視化管理
- ❌ 資料不持久化
- ❌ 不支援複雜 PostgreSQL 功能

### **2. Test Profile - H2 記憶體資料庫 (最小化)**

```yaml
spring:
  main:
    lazy-initialization: true    # 延遲初始化 (加速測試)

  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password: ""
    hikari:
      maximum-pool-size: 5       # 最小連線池
      minimum-idle: 1            # 最小閒置連線

  jpa:
    hibernate:
      ddl-auto: create-drop      # 每次測試重建
    show-sql: false              # 禁用 SQL 日誌 (加速測試)
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect

  flyway:
    enabled: false               # 禁用 (加速測試啟動)
```

**特性：**
- ✅ 最快啟動 (< 2 秒)
- ✅ 完全隔離的測試環境
- ✅ 自動清理
- ✅ 最小資源消耗
- ❌ 功能有限
- ❌ 不適合整合測試

### **3. Staging Profile - PostgreSQL (RDS)**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT:5432}/${DB_NAME}
    driver-class-name: org.postgresql.Driver
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20      # 適合中等負載
      minimum-idle: 5            # 保持基本連線
      connection-timeout: 30000  # 30秒 (考慮網路延遲)
      idle-timeout: 600000       # 10分鐘
      max-lifetime: 1800000      # 30分鐘
      leak-detection-threshold: 60000

  jpa:
    hibernate:
      ddl-auto: validate         # 嚴格驗證 schema
    show-sql: false              # 生產環境不顯示 SQL
    properties:
      hibernate:
        format_sql: false
        use_sql_comments: false
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          batch_size: 20         # 批次處理優化
        order_inserts: true      # 優化插入順序
        order_updates: true      # 優化更新順序

  flyway:
    enabled: true                # 啟用資料庫遷移
    locations: classpath:db/migration/postgresql
    baseline-on-migrate: false  # 不允許基線遷移
    validate-on-migrate: true   # 驗證遷移腳本
```

**環境變數：**
```bash
DB_HOST=genai-demo-staging.cluster-xxx.ap-northeast-1.rds.amazonaws.com
DB_PORT=5432
DB_NAME=genaidemo_staging
DB_USERNAME=genaidemo_user
DB_PASSWORD=${STAGING_DB_PASSWORD}  # 從 Secrets Manager 獲取
```

**特性：**
- ✅ 真實的 PostgreSQL 環境
- ✅ RDS 管理服務 (自動備份、監控)
- ✅ 支援完整的 PostgreSQL 功能
- ✅ 適合整合測試
- ❌ 需要網路連線
- ❌ 啟動較慢

### **4. Production Profile - PostgreSQL (RDS Multi-AZ)**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT:5432}/${DB_NAME}
    driver-class-name: org.postgresql.Driver
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 30      # 更高的連線池 (生產負載)
      minimum-idle: 10           # 更高的最小連線
      connection-timeout: 30000
      idle-timeout: 600000       # 10分鐘
      max-lifetime: 1800000      # 30分鐘
      leak-detection-threshold: 60000
      # 生產環境特定優化
      connection-init-sql: "SET application_name = 'genai-demo-prod'"
      validation-timeout: 5000
      initialization-fail-timeout: 1
      isolate-internal-queries: false
      allow-pool-suspension: true
      read-only: false
      register-mbeans: true      # 啟用 JMX 監控

  jpa:
    hibernate:
      ddl-auto: validate         # 絕不自動修改 schema
    show-sql: false
    properties:
      hibernate:
        format_sql: false
        use_sql_comments: false
        jdbc:
          batch_size: 25         # 更大的批次處理
          fetch_size: 100        # 優化查詢效能
        cache:
          use_second_level_cache: true    # 啟用二級快取
          use_query_cache: true           # 啟用查詢快取
          region:
            factory_class: org.hibernate.cache.jcache.JCacheRegionFactory

  flyway:
    enabled: true
    locations: classpath:db/migration/postgresql
    baseline-on-migrate: false
    validate-on-migrate: true
    clean-disabled: true         # 絕不允許清理生產資料
    baseline-version: 1.0.0
    baseline-description: "Production baseline"
    out-of-order: false          # 嚴格順序執行
    ignore-missing-migrations: false
    ignore-ignored-migrations: false
    ignore-pending-migrations: false
    ignore-future-migrations: false
    validate-migration-naming: true
    # 生產環境特定設定
    batch: true                  # 批次執行
    mixed: false                 # 不允許混合 SQL/Java 遷移
    group: false                 # 不分組執行
    installed-by: "flyway-production"
```

**環境變數：**
```bash
DB_HOST=genai-demo-prod.cluster-xxx.ap-northeast-1.rds.amazonaws.com
DB_PORT=5432
DB_NAME=genaidemo_production
DB_USERNAME=genaidemo_prod_user
DB_PASSWORD=${PROD_DB_PASSWORD}  # 從 Secrets Manager 獲取
```

**特性：**
- ✅ RDS Multi-AZ (高可用性)
- ✅ 自動故障轉移
- ✅ 自動備份和快照
- ✅ 效能監控和優化
- ✅ 二級快取提升效能
- ✅ 嚴格的 schema 管理
- ❌ 成本較高
- ❌ 複雜的配置管理

## 🗄️ **資料庫 Schema 管理策略**

### **Migration 腳本組織**

```
src/main/resources/db/migration/
├── h2/                          # H2 專用腳本 (local)
│   ├── V1__Initial_schema.sql
│   ├── V2__Add_customer_table.sql
│   └── V3__Add_order_table.sql
├── postgresql/                  # PostgreSQL 腳本 (staging/production)
│   ├── V1__Initial_schema.sql
│   ├── V2__Add_customer_table.sql
│   ├── V3__Add_order_table.sql
│   ├── V4__Add_indexes.sql
│   └── V5__Add_constraints.sql
└── common/                      # 通用腳本 (如果需要)
```

### **Schema 演進策略**

| 環境 | DDL Auto | Flyway | Schema 來源 | 變更方式 |
|------|----------|--------|-------------|----------|
| **Local** | create-drop | 禁用 | JPA 自動生成 | 重啟重建 |
| **Test** | create-drop | 禁用 | JPA 自動生成 | 每次測試重建 |
| **Staging** | validate | 啟用 | Flyway 腳本 | 版本化遷移 |
| **Production** | validate | 啟用 | Flyway 腳本 | 嚴格版本控制 |

## 📊 **效能對比**

### **連線池配置對比**

| 指標 | Local | Test | Staging | Production |
|------|-------|------|---------|------------|
| **最大連線數** | 10 | 5 | 20 | 30 |
| **最小閒置** | 2 | 1 | 5 | 10 |
| **連線超時** | 20s | 20s | 30s | 30s |
| **閒置超時** | 5min | 5min | 10min | 10min |
| **洩漏檢測** | 1min | 1min | 1min | 1min |

### **JPA 效能配置對比**

| 功能 | Local | Test | Staging | Production |
|------|-------|------|---------|------------|
| **批次大小** | 預設 | 預設 | 20 | 25 |
| **抓取大小** | 預設 | 預設 | 預設 | 100 |
| **二級快取** | 無 | 無 | 無 | 啟用 |
| **查詢快取** | 無 | 無 | 無 | 啟用 |
| **SQL 日誌** | 啟用 | 禁用 | 禁用 | 禁用 |

## 🔧 **實際使用指南**

### **本機開發**
```bash
# 啟動應用 (自動使用 H2)
export SPRING_PROFILES_ACTIVE=local
./gradlew bootRun

# 存取 H2 Console
open http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:genaidemo
# Username: sa
# Password: (空白)
```

### **Staging 部署**
```bash
# 設定資料庫環境變數
export DB_HOST=your-staging-rds-endpoint
export DB_NAME=genaidemo_staging
export DB_USERNAME=genaidemo_user
export DB_PASSWORD=your-staging-password

# 部署應用
export SPRING_PROFILES_ACTIVE=staging
./gradlew bootRun
```

### **Production 部署**
```bash
# 使用 Secrets Manager 或 K8s Secrets
export SPRING_PROFILES_ACTIVE=production
# 資料庫連線資訊通過 K8s ConfigMap/Secret 注入
```

## 🚨 **注意事項**

### **安全考量**
- **Local/Test**: 無安全限制 (開發便利)
- **Staging**: 基本安全設定
- **Production**: 完整安全措施 (加密、存取控制、稽核)

### **資料持久性**
- **Local/Test**: 資料不持久化 (重啟即清空)
- **Staging**: 資料持久化 (測試資料保留)
- **Production**: 完整備份策略 (自動備份、快照、災難恢復)

### **效能考量**
- **Local**: 最快啟動，適合開發
- **Test**: 最小資源，適合 CI/CD
- **Staging**: 平衡效能，適合整合測試
- **Production**: 最佳效能，適合生產負載

---

**更新日期**: 2025年9月24日 上午9:15 (台北時間)  
**維護者**: 開發團隊  
**版本**: 2.0.0
