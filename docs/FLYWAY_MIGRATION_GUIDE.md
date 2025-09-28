# Flyway Migration Management Guide

## 🎯 **JPA + Flyway Integration Strategy**

### **Why Do We Need Flyway?**

In production environments, JPA's `ddl-auto` has the following issues:

| ddl-auto Option | Risk | Suitable Environment |
|-----------------|------|---------------------|
| `create` | 🚨 Deletes all data | Never use |
| `create-drop` | 🚨 Deletes data on startup | Test environment only |
| `update` | ⚠️ May break schema | Development environment only |
| `validate` | ✅ Only validates, no modifications | Production environment |
| `none` | ✅ No management at all | Production environment |

### **Correct Configuration Strategy**

```yaml
# Correct configuration for each environment
Local:      ddl-auto: create-drop + flyway: disabled
Test:       ddl-auto: create-drop + flyway: disabled  
Staging:    ddl-auto: validate    + flyway: enabled
Production: ddl-auto: validate    + flyway: enabled
```

## 📁 **Migration Script Structure**

### **Directory Organization**

```text
src/main/resources/db/migration/
├── postgresql/                    # Production environment scripts
│   ├── V1__Initial_schema.sql
│   ├── V2__Add_domain_events_table.sql
│   ├── V3__Add_performance_indexes.sql
│   ├── V4__Add_audit_and_security.sql
│   └── V5__Add_new_feature.sql
└── h2/                           # Development environment scripts (if needed)
    ├── V1__Initial_schema.sql
    └── V2__Add_test_data.sql
```

### **Naming Convention**

```text
V{version_number}__{description}.sql

Examples:
V1__Initial_schema.sql           # Initial schema
V2__Add_customer_table.sql       # Add customer table
V3__Modify_order_status.sql      # Modify order status
V4__Add_performance_indexes.sql  # Add performance indexes
V5__Remove_deprecated_columns.sql # Remove deprecated columns
```

## 🔄 **Development Workflow**

### **1. Development Phase (Local Profile)**

```bash
# 1. Modify JPA Entity
@Entity
public class Customer {
    @Id
    private String id;
    
    @Column(name = "email", unique = true)
    private String email;
    
    // Add new field
    @Column(name = "phone")
    private String phone;  // New field
}

# 2. Start application (H2 automatically creates schema)
export SPRING_PROFILES_ACTIVE=local
./gradlew bootRun

# 3. Test functionality works
curl http://localhost:8080/api/customers
```

### **2. Create Migration Script**

```bash
# Create new migration script
touch src/main/resources/db/migration/postgresql/V5__Add_customer_phone.sql
```

```sql
-- V5__Add_customer_phone.sql
-- Add customer phone field

-- Add column
ALTER TABLE customers 
ADD COLUMN phone VARCHAR(50);

-- Add index (if needed)
CREATE INDEX IF NOT EXISTS idx_customers_phone ON customers(phone);

-- Update existing data (if needed)
-- UPDATE customers SET phone = 'N/A' WHERE phone IS NULL;
```

### **3. Staging Environment Validation**

```bash
# Deploy to staging
export SPRING_PROFILES_ACTIVE=staging
export DB_HOST=staging-rds-endpoint
export DB_NAME=genaidemo_staging

# Flyway will automatically execute new migration
./gradlew bootRun

# Check migration status
./gradlew flywayInfo
```

### **4. Production Deployment**

```bash
# Production environment deployment
export SPRING_PROFILES_ACTIVE=production

# First check migration status
./gradlew flywayInfo

# Deploy application (Flyway executes automatically)
./gradlew bootRun
```

## 🛠️ **Flyway Management Commands**

### **Gradle Integration**

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
    cleanDisabled = true  // Production safety
}
```

### **Common Commands**

```bash
# View migration status
./gradlew flywayInfo

# Manually execute migration
./gradlew flywayMigrate

# Validate migration
./gradlew flywayValidate

# View migration history
./gradlew flywayHistory

# Fix checksum errors (use carefully)
./gradlew flywayRepair
```

## 📊 **Migration Script Examples**

### **V1: Initial Schema**

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

### **V2: Add Column**

```sql
-- V2__Add_customer_phone.sql
ALTER TABLE customers 
ADD COLUMN phone VARCHAR(50);

CREATE INDEX idx_customers_phone ON customers(phone);
```

### **V3: Modify Column**

```sql
-- V3__Modify_customer_email_length.sql
-- Increase email field length
ALTER TABLE customers 
ALTER COLUMN email TYPE VARCHAR(320);  -- RFC 5321 standard
```

### **V4: Add Table**

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

### **V5: Data Migration**

```sql
-- V5__Migrate_customer_data.sql
-- Data migration example

-- Update default values for existing customers
UPDATE customers 
SET phone = 'N/A' 
WHERE phone IS NULL;

-- Add default customer
INSERT INTO customers (id, name, email, phone) 
VALUES ('system', 'System User', 'system@genaidemo.com', 'N/A')
ON CONFLICT (id) DO NOTHING;
```

## 🚨 **Best Practices and Precautions**

### **✅ Best Practices**

1. **Backward Compatibility**
   ```sql
   -- ✅ Good: Set default value when adding column
   ALTER TABLE customers ADD COLUMN phone VARCHAR(50) DEFAULT 'N/A';
   
   -- ❌ Bad: Add NOT NULL column without default value
   ALTER TABLE customers ADD COLUMN phone VARCHAR(50) NOT NULL;
   ```

2. **Index Management**
   ```sql
   -- ✅ Good: Use IF NOT EXISTS
   CREATE INDEX IF NOT EXISTS idx_customers_phone ON customers(phone);
   
   -- ❌ Bad: May create duplicates
   CREATE INDEX idx_customers_phone ON customers(phone);
   ```

3. **Data Migration**
   ```sql
   -- ✅ Good: Process large data in batches
   UPDATE customers SET status = 'ACTIVE' 
   WHERE status IS NULL AND id IN (
       SELECT id FROM customers WHERE status IS NULL LIMIT 1000
   );
   
   -- ❌ Bad: Update all data at once (may lock table)
   UPDATE customers SET status = 'ACTIVE' WHERE status IS NULL;
   ```

### **🚨 Precautions**

1. **Commands Never to Use in Production**
   ```bash
   # 🚨 Dangerous: Will delete all data
   ./gradlew flywayClean
   
   # 🚨 Dangerous: Will rebuild entire database
   ./gradlew flywayClean flywayMigrate
   ```

2. **Migration Script Rules**
   - ✅ Cannot be modified once deployed
   - ✅ Must be backward compatible
   - ✅ Must be repeatable
   - ❌ Cannot include DROP TABLE (unless certain)
   - ❌ Cannot modify existing migrations

3. **Rollback Strategy**
   ```sql
   -- Flyway doesn't support automatic rollback, need manual rollback scripts
   -- V6__Rollback_customer_phone.sql
   ALTER TABLE customers DROP COLUMN IF EXISTS phone;
   DROP INDEX IF EXISTS idx_customers_phone;
   ```

## 🔍 **Troubleshooting**

### **Common Issues**

1. **Checksum Error**
   ```bash
   # Problem: Migration checksum mismatch
   # Solution: Repair checksum (use carefully)
   ./gradlew flywayRepair
   ```

2. **Migration Failure**
   ```bash
   # Problem: Migration execution failed
   # Solution: Check failed migration, manually fix then re-execute
   ./gradlew flywayInfo  # Check status
   # Manually fix database
   ./gradlew flywayMigrate  # Re-execute
   ```

3. **JPA Entity and Schema Inconsistency**
   ```bash
   # Problem: Validation failed
   # Solution: Check if Entity definition matches database schema
   
   # Check Entity
   @Entity
   @Table(name = "customers")
   public class Customer {
       @Column(name = "phone")  // Ensure column name matches
       private String phone;
   }
   ```

## 📋 **Checklist**

### **Development Phase**

- [ ] JPA Entity modifications completed
- [ ] Local testing passed
- [ ] Migration script created
- [ ] Migration script tested

### **Pre-deployment**

- [ ] Migration script reviewed
- [ ] Backward compatibility confirmed
- [ ] Staging environment tested
- [ ] Rollback plan prepared

### **Post-deployment**

- [ ] Migration executed successfully
- [ ] Application started normally
- [ ] Functional testing passed
- [ ] Performance monitoring normal

---

**Updated**: September 27, 2025 5:50 PM (Taipei Time)  
**Maintainer**: Development Team  
**Version**: 2.0.0