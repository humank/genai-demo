# Flyway Migration 管理指南

## 🎯 **JPA + Flyway 整合策略**

### **為什麼需要 Flyway？**

在生產環境中，JPA 的 `ddl-auto` 有以下問題：

| ddl-auto 選項 | 風險 | 適用環境 |
|---------------|------|----------|
| `create` | 🚨 刪除所有資料 | 絕不使用 |
| `create-drop` | 🚨 啟動時刪除資料 | 僅測試環境 |
| `update` | ⚠️ 可能破壞 schema | 僅開發環境 |
| `validate` | ✅ 只驗證不修改 | 生產環境 |
| `none` | ✅ 完全不管理 | 生產環境 |

### **正確的配置策略**

```yaml
# 各環境的正確配置
Local:    ddl-auto: create-drop + flyway: disabled
Test:     ddl-auto: create-drop + flyway: disabled  
Staging:  ddl-auto: validate    + flyway: enabled
Production: ddl-auto: validate  + flyway: enabled
```

## 📁 **Migration 腳本結構**

### **目錄組織**
```
src/main/resources/db/migration/
├── postgresql/                    # 生產環境腳本
│   ├── V1__Initial_schema.sql
│   ├── V2__Add_domain_events_table.sql
│   ├── V3__Add_performance_indexes.sql
│   ├── V4__Add_audit_and_security.sql
│   └── V5__Add_new_feature.sql
└── h2/                           # 開發環境腳本 (如果需要)
    ├── V1__Initial_schema.sql
    └── V2__Add_test_data.sql
```

### **命名規範**
```
V{版本號}__{描述}.sql

範例：
V1__Initial_schema.sql           # 初始 schema
V2__Add_customer_table.sql       # 新增客戶表格
V3__Modify_order_status.sql      # 修改訂單狀態
V4__Add_performance_indexes.sql  # 新增效能索引
V5__Remove_deprecated_columns.sql # 移除廢棄欄位
```

## 🔄 **開發工作流程**

### **1. 開發階段 (Local Profile)**

```bash
# 1. 修改 JPA Entity
@Entity
public class Customer {
    @Id
    private String id;
    
    @Column(name = "email", unique = true)
    private String email;
    
    // 新增欄位
    @Column(name = "phone")
    private String phone;  // 新欄位
}

# 2. 啟動應用 (H2 自動建立 schema)
export SPRING_PROFILES_ACTIVE=local
./gradlew bootRun

# 3. 測試功能正常
curl http://localhost:8080/api/customers
```

### **2. 建立 Migration 腳本**

```bash
# 建立新的 migration 腳本
touch src/main/resources/db/migration/postgresql/V5__Add_customer_phone.sql
```

```sql
-- V5__Add_customer_phone.sql
-- 新增客戶電話欄位

-- 新增欄位
ALTER TABLE customers 
ADD COLUMN phone VARCHAR(50);

-- 新增索引 (如果需要)
CREATE INDEX IF NOT EXISTS idx_customers_phone ON customers(phone);

-- 更新現有資料 (如果需要)
-- UPDATE customers SET phone = 'N/A' WHERE phone IS NULL;
```

### **3. Staging 環境驗證**

```bash
# 部署到 staging
export SPRING_PROFILES_ACTIVE=staging
export DB_HOST=staging-rds-endpoint
export DB_NAME=genaidemo_staging

# Flyway 會自動執行新的 migration
./gradlew bootRun

# 檢查 migration 狀態
./gradlew flywayInfo
```

### **4. Production 部署**

```bash
# 生產環境部署
export SPRING_PROFILES_ACTIVE=production

# 先檢查 migration 狀態
./gradlew flywayInfo

# 部署應用 (Flyway 自動執行)
./gradlew bootRun
```

## 🛠️ **Flyway 管理命令**

### **Gradle 整合**

```gradle
// build.gradle
plugins {
    id 'org.flywaydb.flyway' version '9.22.3'
}

flyway {
    url = project.findProperty('flyway.url') ?: 'jdbc:postgresql://localhost:5432/genaidemo'
    user = project.findProperty('flyway.user') ?: 'genaidemo'
    password = project.findProperty('flyway.password') ?: 'password'
    locations = ['classpath:db/migration/postgresql']
    baselineOnMigrate = false
    validateOnMigrate = true
    cleanDisabled = true  // 生產安全
}
```

### **常用命令**

```bash
# 查看 migration 狀態
./gradlew flywayInfo

# 手動執行 migration
./gradlew flywayMigrate

# 驗證 migration
./gradlew flywayValidate

# 查看 migration 歷史
./gradlew flywayHistory

# 修復 checksum 錯誤 (小心使用)
./gradlew flywayRepair
```

## 📊 **Migration 腳本範例**

### **V1: 初始 Schema**
```sql
-- V1__Initial_schema.sql
CREATE TABLE customers (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_customers_email ON customers(email);
```

### **V2: 新增欄位**
```sql
-- V2__Add_customer_phone.sql
ALTER TABLE customers 
ADD COLUMN phone VARCHAR(50);

CREATE INDEX idx_customers_phone ON customers(phone);
```

### **V3: 修改欄位**
```sql
-- V3__Modify_customer_email_length.sql
-- 增加 email 欄位長度
ALTER TABLE customers 
ALTER COLUMN email TYPE VARCHAR(320);  -- RFC 5321 標準
```

### **V4: 新增表格**
```sql
-- V4__Add_orders_table.sql
CREATE TABLE orders (
    id VARCHAR(255) PRIMARY KEY,
    customer_id VARCHAR(255) NOT NULL,
    total_amount DECIMAL(19,2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_orders_customer 
        FOREIGN KEY (customer_id) REFERENCES customers(id)
);

CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
```

### **V5: 資料遷移**
```sql
-- V5__Migrate_customer_data.sql
-- 資料遷移範例

-- 更新現有客戶的預設值
UPDATE customers 
SET phone = 'N/A' 
WHERE phone IS NULL;

-- 新增預設客戶
INSERT INTO customers (id, name, email, phone) 
VALUES ('system', 'System User', 'system@genaidemo.com', 'N/A')
ON CONFLICT (id) DO NOTHING;
```

## 🚨 **最佳實踐和注意事項**

### **✅ 最佳實踐**

1. **向後相容性**
   ```sql
   -- ✅ 好：新增欄位時設定預設值
   ALTER TABLE customers ADD COLUMN phone VARCHAR(50) DEFAULT 'N/A';
   
   -- ❌ 壞：新增 NOT NULL 欄位沒有預設值
   ALTER TABLE customers ADD COLUMN phone VARCHAR(50) NOT NULL;
   ```

2. **索引管理**
   ```sql
   -- ✅ 好：使用 IF NOT EXISTS
   CREATE INDEX IF NOT EXISTS idx_customers_phone ON customers(phone);
   
   -- ❌ 壞：可能重複建立
   CREATE INDEX idx_customers_phone ON customers(phone);
   ```

3. **資料遷移**
   ```sql
   -- ✅ 好：分批處理大量資料
   UPDATE customers SET status = 'ACTIVE' 
   WHERE status IS NULL AND id IN (
       SELECT id FROM customers WHERE status IS NULL LIMIT 1000
   );
   
   -- ❌ 壞：一次更新所有資料 (可能鎖表)
   UPDATE customers SET status = 'ACTIVE' WHERE status IS NULL;
   ```

### **🚨 注意事項**

1. **絕不在生產環境使用的命令**
   ```bash
   # 🚨 危險：會刪除所有資料
   ./gradlew flywayClean
   
   # 🚨 危險：會重建整個資料庫
   ./gradlew flywayClean flywayMigrate
   ```

2. **Migration 腳本規則**
   - ✅ 一旦部署就不能修改
   - ✅ 必須向後相容
   - ✅ 必須可重複執行
   - ❌ 不能包含 DROP TABLE (除非確定)
   - ❌ 不能修改已存在的 migration

3. **回滾策略**
   ```sql
   -- Flyway 不支援自動回滾，需要手動建立回滾腳本
   -- V6__Rollback_customer_phone.sql
   ALTER TABLE customers DROP COLUMN IF EXISTS phone;
   DROP INDEX IF EXISTS idx_customers_phone;
   ```

## 🔍 **故障排除**

### **常見問題**

1. **Checksum 錯誤**
   ```bash
   # 問題：Migration checksum mismatch
   # 解決：修復 checksum (小心使用)
   ./gradlew flywayRepair
   ```

2. **Migration 失敗**
   ```bash
   # 問題：Migration 執行失敗
   # 解決：檢查失敗的 migration，手動修復後重新執行
   ./gradlew flywayInfo  # 查看狀態
   # 手動修復資料庫
   ./gradlew flywayMigrate  # 重新執行
   ```

3. **JPA Entity 與 Schema 不一致**
   ```bash
   # 問題：Validation failed
   # 解決：檢查 Entity 定義與資料庫 schema 是否一致
   
   # 檢查 Entity
   @Entity
   @Table(name = "customers")
   public class Customer {
       @Column(name = "phone")  // 確保欄位名稱一致
       private String phone;
   }
   ```

## 📋 **檢查清單**

### **開發階段**
- [ ] JPA Entity 修改完成
- [ ] 本機測試通過
- [ ] Migration 腳本建立
- [ ] Migration 腳本測試

### **部署前**
- [ ] Migration 腳本 review
- [ ] 向後相容性確認
- [ ] Staging 環境測試
- [ ] 回滾計劃準備

### **部署後**
- [ ] Migration 執行成功
- [ ] 應用程式啟動正常
- [ ] 功能測試通過
- [ ] 效能監控正常

---

**更新日期**: 2025年9月24日 上午9:30 (台北時間)  
**維護者**: 開發團隊  
**版本**: 2.0.0