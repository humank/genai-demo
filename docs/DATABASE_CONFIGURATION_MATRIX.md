# Database Configuration Matrix

## 📊 **Complete Database Configuration Matrix**

### **Database Configuration Overview**

| Configuration Item | Local | Test | Staging | Production |
|-------------------|-------|------|---------|------------|
| **Database Type** | H2 In-Memory | H2 In-Memory | PostgreSQL (RDS) | PostgreSQL (RDS Multi-AZ) |
| **Connection Method** | Embedded | Embedded | JDBC | JDBC |
| **Schema Management** | create-drop | create-drop | validate | validate |
| **Migration** | Disabled | Disabled | Flyway Enabled | Flyway Enabled |
| **Connection Pool** | 10 max, 2 min | 5 max, 1 min | 20 max, 5 min | 30 max, 10 min |
| **SQL Logging** | Enabled (DEBUG) | Disabled | Disabled | Disabled |
| **Cache** | None | None | None | Second Level Cache Enabled |
| **Batch Processing** | Default | Default | 20 | 25 |

## 🔧 **Detailed Configuration Analysis**

### **1. Local Profile - H2 In-Memory Database**

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:genaidemo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password: ""
    hikari:
      maximum-pool-size: 10      # Suitable for local development
      minimum-idle: 2            # Minimum connections
      connection-timeout: 20000  # 20 second connection timeout
      idle-timeout: 300000       # 5 minute idle timeout
      max-lifetime: 1200000      # 20 minute maximum lifetime
      leak-detection-threshold: 60000  # 1 minute leak detection

  jpa:
    hibernate:
      ddl-auto: create-drop      # Rebuild schema on each startup
    show-sql: true               # Show SQL (development debugging)
    properties:
      hibernate:
        format_sql: true         # Format SQL output
        use_sql_comments: true   # Show SQL comments
        dialect: org.hibernate.dialect.H2Dialect
    defer-datasource-initialization: true  # Defer initialization

  h2:
    console:
      enabled: true              # Enable H2 Console
      path: /h2-console          # Console path
      settings:
        web-allow-others: true   # Allow remote access

  flyway:
    enabled: false               # Disabled (avoid circular dependencies)
    locations: classpath:db/migration/h2
```

**Features:**
- ✅ Fast startup (< 5 seconds)
- ✅ No external database required
- ✅ SQL debugging support
- ✅ H2 Console visual management
- ❌ Data not persistent
- ❌ No support for complex PostgreSQL features

### **2. Test Profile - H2 In-Memory Database (Minimized)**

```yaml
spring:
  main:
    lazy-initialization: true    # Lazy initialization (speed up tests)

  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password: ""
    hikari:
      maximum-pool-size: 5       # Minimal connection pool
      minimum-idle: 1            # Minimal idle connections

  jpa:
    hibernate:
      ddl-auto: create-drop      # Rebuild for each test
    show-sql: false              # Disable SQL logging (speed up tests)
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect

  flyway:
    enabled: false               # Disabled (speed up test startup)
```

**Features:**
- ✅ Fastest startup (< 2 seconds)
- ✅ Completely isolated test environment
- ✅ Automatic cleanup
- ✅ Minimal resource consumption
- ❌ Limited functionality
- ❌ Not suitable for integration tests

### **3. Staging Profile - PostgreSQL (RDS)**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT:5432}/${DB_NAME}
    driver-class-name: org.postgresql.Driver
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20      # Suitable for medium load
      minimum-idle: 5            # Maintain basic connections
      connection-timeout: 30000  # 30 seconds (consider network latency)
      idle-timeout: 600000       # 10 minutes
      max-lifetime: 1800000      # 30 minutes
      leak-detection-threshold: 60000

  jpa:
    hibernate:
      ddl-auto: validate         # Strict schema validation
    show-sql: false              # Don't show SQL in production environment
    properties:
      hibernate:
        format_sql: false
        use_sql_comments: false
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          batch_size: 20         # Batch processing optimization
        order_inserts: true      # Optimize insert order
        order_updates: true      # Optimize update order

  flyway:
    enabled: true                # Enable database migration
    locations: classpath:db/migration/postgresql
    baseline-on-migrate: false  # Don't allow baseline migration
    validate-on-migrate: true   # Validate migration scripts
```

**Environment Variables:**
```bash
DB_HOST=genai-demo-staging.cluster-xxx.ap-northeast-1.rds.amazonaws.com
DB_PORT=5432
DB_NAME=genaidemo_staging
DB_USERNAME=genaidemo_user
DB_PASSWORD=${STAGING_DB_PASSWORD}  # Retrieved from Secrets Manager
```

**Features:**
- ✅ Real PostgreSQL environment
- ✅ RDS managed service (automatic backup, monitoring)
- ✅ Support for complete PostgreSQL features
- ✅ Suitable for integration testing
- ❌ Requires network connection
- ❌ Slower startup

### **4. Production Profile - PostgreSQL (RDS Multi-AZ)**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT:5432}/${DB_NAME}
    driver-class-name: org.postgresql.Driver
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 30      # Higher connection pool (production load)
      minimum-idle: 10           # Higher minimum connections
      connection-timeout: 30000
      idle-timeout: 600000       # 10 minutes
      max-lifetime: 1800000      # 30 minutes
      leak-detection-threshold: 60000
      # Production-specific optimizations
      connection-init-sql: "SET application_name = 'genai-demo-prod'"
      validation-timeout: 5000
      initialization-fail-timeout: 1
      isolate-internal-queries: false
      allow-pool-suspension: true
      read-only: false
      register-mbeans: true      # Enable JMX monitoring

  jpa:
    hibernate:
      ddl-auto: validate         # Never automatically modify schema
    show-sql: false
    properties:
      hibernate:
        format_sql: false
        use_sql_comments: false
        jdbc:
          batch_size: 25         # Larger batch processing
          fetch_size: 100        # Optimize query performance
        cache:
          use_second_level_cache: true    # Enable second level cache
          use_query_cache: true           # Enable query cache
          region:
            factory_class: org.hibernate.cache.jcache.JCacheRegionFactory

  flyway:
    enabled: true
    locations: classpath:db/migration/postgresql
    baseline-on-migrate: false
    validate-on-migrate: true
    clean-disabled: true         # Never allow cleaning production data
    baseline-version: 1.0.0
    baseline-description: "Production baseline"
    out-of-order: false          # Strict order execution
    ignore-missing-migrations: false
    ignore-ignored-migrations: false
    ignore-pending-migrations: false
    ignore-future-migrations: false
    validate-migration-naming: true
    # Production-specific settings
    batch: true                  # Batch execution
    mixed: false                 # Don't allow mixed SQL/Java migrations
    group: false                 # Don't group execution
    installed-by: "flyway-production"
```

**Environment Variables:**
```bash
DB_HOST=genai-demo-prod.cluster-xxx.ap-northeast-1.rds.amazonaws.com
DB_PORT=5432
DB_NAME=genaidemo_production
DB_USERNAME=genaidemo_prod_user
DB_PASSWORD=${PROD_DB_PASSWORD}  # Retrieved from Secrets Manager
```

**Features:**
- ✅ RDS Multi-AZ (high availability)
- ✅ Automatic failover
- ✅ Automatic backup and snapshots
- ✅ Performance monitoring and optimization
- ✅ Second level cache improves performance
- ✅ Strict schema management
- ❌ Higher cost
- ❌ Complex configuration management

## 🗄️ **Database Schema Management Strategy**

### **Migration Script Organization**

```
src/main/resources/db/migration/
├── h2/                          # H2-specific scripts (local)
│   ├── V1__Initial_schema.sql
│   ├── V2__Add_customer_table.sql
│   └── V3__Add_order_table.sql
├── postgresql/                  # PostgreSQL scripts (staging/production)
│   ├── V1__Initial_schema.sql
│   ├── V2__Add_customer_table.sql
│   ├── V3__Add_order_table.sql
│   ├── V4__Add_indexes.sql
│   └── V5__Add_constraints.sql
└── common/                      # Common scripts (if needed)
```

### **Schema Evolution Strategy**

| Environment | DDL Auto | Flyway | Schema Source | Change Method |
|-------------|----------|--------|---------------|---------------|
| **Local** | create-drop | Disabled | JPA auto-generated | Rebuild on restart |
| **Test** | create-drop | Disabled | JPA auto-generated | Rebuild each test |
| **Staging** | validate | Enabled | Flyway scripts | Versioned migration |
| **Production** | validate | Enabled | Flyway scripts | Strict version control |

## 📊 **Performance Comparison**

### **Connection Pool Configuration Comparison**

| Metric | Local | Test | Staging | Production |
|--------|-------|------|---------|------------|
| **Max Connections** | 10 | 5 | 20 | 30 |
| **Min Idle** | 2 | 1 | 5 | 10 |
| **Connection Timeout** | 20s | 20s | 30s | 30s |
| **Idle Timeout** | 5min | 5min | 10min | 10min |
| **Leak Detection** | 1min | 1min | 1min | 1min |

### **JPA Performance Configuration Comparison**

| Feature | Local | Test | Staging | Production |
|---------|-------|------|---------|------------|
| **Batch Size** | Default | Default | 20 | 25 |
| **Fetch Size** | Default | Default | Default | 100 |
| **Second Level Cache** | None | None | None | Enabled |
| **Query Cache** | None | None | None | Enabled |
| **SQL Logging** | Enabled | Disabled | Disabled | Disabled |

## 🔧 **Practical Usage Guide**

### **Local Development**
```bash
# Start application (automatically uses H2)
export SPRING_PROFILES_ACTIVE=local
./gradlew bootRun

# Access H2 Console
open http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:genaidemo
# Username: sa
# Password: (blank)
```

### **Staging Deployment**
```bash
# Set database environment variables
export DB_HOST=your-staging-rds-endpoint
export DB_NAME=genaidemo_staging
export DB_USERNAME=genaidemo_user
export DB_PASSWORD=your-staging-password

# Deploy application
export SPRING_PROFILES_ACTIVE=staging
./gradlew bootRun
```

### **Production Deployment**
```bash
# Use Secrets Manager or K8s Secrets
export SPRING_PROFILES_ACTIVE=production
# Database connection info injected via K8s ConfigMap/Secret
```

## 🚨 **Important Notes**

### **Security Considerations**
- **Local/Test**: No security restrictions (development convenience)
- **Staging**: Basic security settings
- **Production**: Complete security measures (encryption, access control, auditing)

### **Data Persistence**
- **Local/Test**: Data not persistent (cleared on restart)
- **Staging**: Data persistent (test data retained)
- **Production**: Complete backup strategy (automatic backup, snapshots, disaster recovery)

### **Performance Considerations**
- **Local**: Fastest startup, suitable for development
- **Test**: Minimal resources, suitable for CI/CD
- **Staging**: Balanced performance, suitable for integration testing
- **Production**: Best performance, suitable for production load

---

**Updated**: September 27, 2025 5:50 PM (Taipei Time)  
**Maintainer**: Development Team  
**Version**: 2.0.0