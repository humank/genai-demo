---
adr_number: 020
title: "Database Migration Strategy with Flyway"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [019, 021, 025]
affected_viewpoints: ["information", "deployment", "operational"]
affected_perspectives: ["availability", "evolution"]
---

# ADR-020: Database Migration Strategy with Flyway

## Status

**Accepted** - 2025-10-25

## Context

### Problem Statement

The Enterprise E-Commerce Platform requires a robust database migration strategy to:

**Business Requirements**:
- **Zero Downtime**: Database changes must not cause service interruptions
- **Version Control**: All schema changes must be tracked and versioned
- **Rollback Safety**: Ability to rollback failed migrations
- **Multi-Environment**: Consistent migrations across dev, staging, production
- **Audit Trail**: Complete history of all database changes
- **Team Collaboration**: Multiple developers making schema changes

**Technical Challenges**:
- Complex schema with 13 bounded contexts
- High transaction volume (10,000+ TPS)
- Multiple database instances (RDS primary + read replicas)
- Concurrent deployments across regions
- Schema evolution with backward compatibility
- Data migration for large tables
- Coordination with application deployments

**Current State**:
- Manual SQL scripts
- No version control for schema
- Inconsistent schemas across environments
- Risky manual migrations
- No rollback strategy
- Difficult to track changes

### Business Context

**Business Drivers**:
- Enable continuous delivery
- Reduce deployment risk
- Improve database reliability
- Support rapid feature development
- Ensure data integrity
- Meet compliance requirements

**Constraints**:
- Budget: $30,000 for implementation
- Timeline: 6 weeks
- Team: 2 database engineers, 3 developers
- Must work with existing PostgreSQL RDS
- Cannot cause downtime
- Must support multi-region

### Technical Context

**Current Database**:
- PostgreSQL 15 on AWS RDS
- Primary + 2 read replicas per region
- 500GB data size
- 10,000+ TPS peak load

**Target State**:
- Automated schema migrations
- Version-controlled migrations
- Backward compatible changes
- Automated rollback capability
- Multi-environment consistency

## Decision Drivers

1. **Reliability**: Ensure migrations succeed consistently
2. **Safety**: Prevent data loss or corruption
3. **Automation**: Minimize manual intervention
4. **Versioning**: Track all schema changes
5. **Rollback**: Quick recovery from failed migrations
6. **Performance**: Minimize migration impact on production
7. **Simplicity**: Easy for developers to use
8. **Integration**: Work with existing CI/CD pipeline

## Considered Options

### Option 1: Flyway (Recommended)

**Description**: Industry-standard database migration tool with version control and automation

**Migration Structure**:

```
src/main/resources/db/migration/
├── V1__initial_schema.sql
├── V2__add_customer_email_index.sql
├── V3__add_order_status_column.sql
├── V4__create_audit_log_table.sql
├── V5__add_customer_membership_level.sql
└── R__create_customer_summary_view.sql  # Repeatable migration
```

**Flyway Configuration**:

```yaml
# application.yml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    baseline-version: 0
    locations: classpath:db/migration
    schemas: public
    table: flyway_schema_history
    validate-on-migrate: true
    out-of-order: false
    placeholder-replacement: true
    placeholders:
      environment: ${ENVIRONMENT:development}
    
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 10
      connection-timeout: 30000
```

**Migration Naming Convention**:

```
V{version}__{description}.sql
├── V: Versioned migration (runs once)
├── U: Undo migration (rollback)
├── R: Repeatable migration (runs on checksum change)

Examples:
- V1__initial_schema.sql
- V2__add_customer_email_index.sql
- V3.1__add_order_status_column.sql
- V3.2__migrate_order_status_data.sql
- U3__rollback_order_status_column.sql
- R__create_customer_summary_view.sql
```

**Backward Compatible Migration Pattern**:

```sql
-- V10__add_customer_phone_column.sql
-- Phase 1: Add new column (backward compatible)

-- Add new column as nullable
ALTER TABLE customers 
ADD COLUMN phone_number VARCHAR(20);

-- Add index for performance
CREATE INDEX idx_customers_phone 
ON customers(phone_number) 
WHERE phone_number IS NOT NULL;

-- Add comment for documentation
COMMENT ON COLUMN customers.phone_number IS 
'Customer phone number - Added in V10';

-- Update application_version tracking
INSERT INTO schema_versions (version, description, applied_at)
VALUES ('V10', 'Add customer phone number column', NOW());
```

```sql
-- V11__migrate_customer_phone_data.sql
-- Phase 2: Migrate data (can run in background)

-- Migrate data in batches to avoid locks
DO $$
DECLARE
    batch_size INTEGER := 1000;
    offset_val INTEGER := 0;
    rows_updated INTEGER;
BEGIN
    LOOP
        -- Update batch
        WITH batch AS (
            SELECT customer_id, contact_phone
            FROM customers
            WHERE phone_number IS NULL
              AND contact_phone IS NOT NULL
            LIMIT batch_size
            OFFSET offset_val
        )
        UPDATE customers c
        SET phone_number = b.contact_phone
        FROM batch b
        WHERE c.customer_id = b.customer_id;
        
        GET DIAGNOSTICS rows_updated = ROW_COUNT;
        
        -- Exit if no more rows
        EXIT WHEN rows_updated = 0;
        
        -- Increment offset
        offset_val := offset_val + batch_size;
        
        -- Commit and pause to avoid long locks
        COMMIT;
        PERFORM pg_sleep(0.1);
    END LOOP;
END $$;
```

```sql
-- V12__make_customer_phone_required.sql
-- Phase 3: Make column required (after validation)

-- Add NOT NULL constraint
ALTER TABLE customers 
ALTER COLUMN phone_number SET NOT NULL;

-- Add validation constraint
ALTER TABLE customers
ADD CONSTRAINT chk_phone_format 
CHECK (phone_number ~ '^\+?[1-9]\d{1,14}$');

-- Drop old column (after grace period)
-- ALTER TABLE customers DROP COLUMN contact_phone;
```

**Rollback Strategy**:

```sql
-- U12__rollback_customer_phone_required.sql
-- Rollback Phase 3

-- Remove NOT NULL constraint
ALTER TABLE customers 
ALTER COLUMN phone_number DROP NOT NULL;

-- Remove validation constraint
ALTER TABLE customers
DROP CONSTRAINT IF EXISTS chk_phone_format;
```

**Flyway Gradle Integration**:

```gradle
// build.gradle
plugins {
    id 'org.flywaydb.flyway' version '9.22.0'
}

flyway {
    url = System.getenv('DB_URL') ?: 'jdbc:postgresql://localhost:5432/ecommerce'
    user = System.getenv('DB_USER') ?: 'postgres'
    password = System.getenv('DB_PASSWORD') ?: 'postgres'
    schemas = ['public']
    locations = ['filesystem:src/main/resources/db/migration']
    baselineOnMigrate = true
    validateOnMigrate = true
    outOfOrder = false
    table = 'flyway_schema_history'
}

tasks.register('flywayMigrateWithValidation') {
    dependsOn 'flywayValidate'
    finalizedBy 'flywayMigrate'
}
```

**CI/CD Integration**:

```yaml
# .github/workflows/database-migration.yml
name: Database Migration

on:
  push:
    branches: [main]
    paths:
      - 'src/main/resources/db/migration/**'

jobs:
  validate-migrations:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          java-version: '21'
      
      - name: Validate Migration Scripts
        run: |
          # Check naming convention
          ./scripts/validate-migration-names.sh
          
          # Check SQL syntax
          ./scripts/validate-sql-syntax.sh
          
          # Flyway validate
          ./gradlew flywayValidate
      
      - name: Test Migrations on Staging
        run: |
          # Run migrations on staging database
          ./gradlew flywayMigrate \
            -Dflyway.url=${{ secrets.STAGING_DB_URL }} \
            -Dflyway.user=${{ secrets.STAGING_DB_USER }} \
            -Dflyway.password=${{ secrets.STAGING_DB_PASSWORD }}
      
      - name: Run Integration Tests
        run: ./gradlew integrationTest
      
      - name: Approve for Production
        if: success()
        run: |
          # Create approval request
          gh pr comment ${{ github.event.pull_request.number }} \
            --body "✅ Migrations validated. Ready for production."

  deploy-to-production:
    needs: validate-migrations
    runs-on: ubuntu-latest
    environment: production
    steps:
      - uses: actions/checkout@v3
      
      - name: Backup Database
        run: |
          # Create backup before migration
          aws rds create-db-snapshot \
            --db-instance-identifier ecommerce-prod \
            --db-snapshot-identifier pre-migration-$(date +%Y%m%d-%H%M%S)
      
      - name: Run Production Migrations
        run: |
          ./gradlew flywayMigrate \
            -Dflyway.url=${{ secrets.PROD_DB_URL }} \
            -Dflyway.user=${{ secrets.PROD_DB_USER }} \
            -Dflyway.password=${{ secrets.PROD_DB_PASSWORD }}
      
      - name: Verify Migration
        run: |
          # Check migration status
          ./gradlew flywayInfo
          
          # Run smoke tests
          ./scripts/database-smoke-tests.sh
      
      - name: Rollback on Failure
        if: failure()
        run: |
          # Restore from backup
          ./scripts/restore-database-backup.sh
          
          # Notify team
          curl -X POST ${{ secrets.SLACK_WEBHOOK }} \
            -d '{"text":"❌ Database migration failed and rolled back"}'
```

**Migration Testing Strategy**:

```java
@SpringBootTest
@Testcontainers
class DatabaseMigrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");
    
    @Autowired
    private Flyway flyway;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Test
    void should_apply_all_migrations_successfully() {
        // Given: Clean database
        flyway.clean();
        
        // When: Apply migrations
        MigrateResult result = flyway.migrate();
        
        // Then: All migrations applied
        assertThat(result.migrationsExecuted).isGreaterThan(0);
        assertThat(result.success).isTrue();
    }
    
    @Test
    void should_maintain_backward_compatibility() {
        // Given: Database at version N
        flyway.migrate();
        MigrationInfo currentVersion = flyway.info().current();
        
        // When: Insert data with old schema
        jdbcTemplate.update(
            "INSERT INTO customers (customer_id, name, email) VALUES (?, ?, ?)",
            "CUST-001", "John Doe", "john@example.com"
        );
        
        // Then: Data inserted successfully
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM customers WHERE customer_id = ?",
            Integer.class,
            "CUST-001"
        );
        assertThat(count).isEqualTo(1);
    }
    
    @Test
    void should_rollback_failed_migration() {
        // Given: Migration that will fail
        String failingMigration = """
            CREATE TABLE test_table (
                id SERIAL PRIMARY KEY,
                invalid_syntax  -- Missing type
            );
            """;
        
        // When: Apply failing migration
        assertThatThrownBy(() -> {
            flyway.migrate();
        }).isInstanceOf(FlywayException.class);
        
        // Then: Database state unchanged
        MigrationInfo current = flyway.info().current();
        assertThat(current.getState()).isNotEqualTo(MigrationState.FAILED);
    }
}
```

**Monitoring and Alerting**:

```java
@Component
public class FlywayMigrationMonitor {
    
    private final MeterRegistry meterRegistry;
    
    @EventListener
    public void onMigrationSuccess(MigrationSuccessEvent event) {
        Counter.builder("flyway.migrations.success")
            .tag("version", event.getVersion())
            .register(meterRegistry)
            .increment();
        
        logger.info("Migration {} completed successfully in {}ms",
            event.getVersion(), event.getDuration());
    }
    
    @EventListener
    public void onMigrationFailure(MigrationFailureEvent event) {
        Counter.builder("flyway.migrations.failure")
            .tag("version", event.getVersion())
            .tag("error", event.getError().getClass().getSimpleName())
            .register(meterRegistry)
            .increment();
        
        logger.error("Migration {} failed: {}",
            event.getVersion(), event.getError().getMessage());
        
        // Send alert
        alertService.sendCriticalAlert(
            "Database Migration Failed",
            String.format("Migration %s failed: %s",
                event.getVersion(), event.getError().getMessage())
        );
    }
}
```

**Pros**:
- ✅ Industry standard with large community
- ✅ Excellent Spring Boot integration
- ✅ Version control for schema changes
- ✅ Automatic migration on startup
- ✅ Rollback support with undo migrations
- ✅ Repeatable migrations for views/procedures
- ✅ Comprehensive validation
- ✅ Multi-environment support
- ✅ Good documentation

**Cons**:
- ⚠️ Learning curve for team
- ⚠️ Requires discipline in migration writing
- ⚠️ Undo migrations need manual creation
- ⚠️ Large data migrations can be slow

**Cost**: $30,000 implementation + $2,000/year (support)

**Risk**: **Low** - Proven, widely adopted solution

### Option 2: Liquibase

**Description**: XML/YAML-based database migration tool

**Pros**:
- ✅ Database-agnostic
- ✅ XML/YAML format
- ✅ Automatic rollback generation
- ✅ Change set tracking

**Cons**:
- ❌ More complex configuration
- ❌ XML verbosity
- ❌ Steeper learning curve
- ❌ Less intuitive than SQL

**Cost**: $35,000 implementation + $3,000/year

**Risk**: **Medium** - More complex

### Option 3: Custom Migration Scripts

**Description**: Manual SQL scripts with custom versioning

**Pros**:
- ✅ Full control
- ✅ No external dependencies
- ✅ Simple for small projects

**Cons**:
- ❌ No automation
- ❌ Error-prone
- ❌ No version tracking
- ❌ Difficult to maintain
- ❌ No rollback support

**Cost**: $10,000 implementation

**Risk**: **High** - Unreliable, not scalable

## Decision Outcome

**Chosen Option**: **Flyway (Option 1)**

### Rationale

Flyway provides the optimal balance of simplicity, reliability, and features for database migration management, with excellent Spring Boot integration and industry-proven track record.

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | Medium | New migration workflow | Training, templates, documentation |
| Database Team | High | New migration process | Training, runbooks, automation |
| DevOps Team | Medium | CI/CD integration | Automated pipelines, monitoring |
| QA Team | Low | Test database consistency | Automated test migrations |
| Operations Team | Low | Monitoring migrations | Dashboards, alerts |

### Impact Radius Assessment

**Selected Impact Radius**: **System**

Affects:
- All database schema changes
- Application deployments
- CI/CD pipeline
- Development workflow
- Testing procedures

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Migration failures | Low | Critical | Validation, testing, rollback |
| Data loss | Very Low | Critical | Backups, backward compatibility |
| Downtime | Low | High | Zero-downtime migrations |
| Team adoption | Medium | Medium | Training, documentation |
| Performance impact | Low | Medium | Batch processing, off-peak |

**Overall Risk Level**: **Low**

## Implementation Plan

### Phase 1: Setup and Configuration (Week 1-2)

**Tasks**:
- [ ] Add Flyway dependency to project
- [ ] Configure Flyway in application.yml
- [ ] Set up migration directory structure
- [ ] Create baseline migration
- [ ] Configure CI/CD integration
- [ ] Set up monitoring

**Success Criteria**:
- Flyway configured and working
- Baseline migration applied
- CI/CD pipeline updated

### Phase 2: Migration Templates and Guidelines (Week 3)

**Tasks**:
- [ ] Create migration templates
- [ ] Document naming conventions
- [ ] Write migration guidelines
- [ ] Create rollback templates
- [ ] Set up validation scripts

**Success Criteria**:
- Templates available
- Guidelines documented
- Validation working

### Phase 3: Team Training (Week 4)

**Tasks**:
- [ ] Conduct training sessions
- [ ] Create example migrations
- [ ] Practice rollback procedures
- [ ] Review best practices

**Success Criteria**:
- Team trained
- Examples created
- Procedures documented

### Phase 4: Production Rollout (Week 5-6)

**Tasks**:
- [ ] Apply baseline to production
- [ ] Migrate existing changes to Flyway
- [ ] Test rollback procedures
- [ ] Monitor first migrations
- [ ] Gather feedback

**Success Criteria**:
- Production using Flyway
- All migrations tracked
- Team comfortable with process

### Rollback Strategy

**Trigger Conditions**:
- Migration failures
- Data corruption
- Performance issues

**Rollback Steps**:
1. Stop application
2. Restore database backup
3. Fix migration script
4. Test in staging
5. Reapply migration

**Rollback Time**: < 30 minutes

## Monitoring and Success Criteria

### Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Migration Success Rate | > 99% | Flyway metrics |
| Migration Time | < 5 minutes | Flyway logs |
| Rollback Time | < 30 minutes | Incident metrics |
| Schema Consistency | 100% | Validation checks |
| Zero Downtime | 100% | Application metrics |

### Review Schedule

- **Weekly**: Migration metrics review
- **Monthly**: Process optimization
- **Quarterly**: Strategy review

## Consequences

### Positive Consequences

- ✅ **Version Control**: All schema changes tracked
- ✅ **Automation**: Migrations run automatically
- ✅ **Consistency**: Same schema across environments
- ✅ **Rollback**: Quick recovery from failures
- ✅ **Audit Trail**: Complete migration history
- ✅ **Safety**: Validation before production
- ✅ **Collaboration**: Multiple developers can work safely

### Negative Consequences

- ⚠️ **Learning Curve**: Team needs training
- ⚠️ **Discipline Required**: Must follow conventions
- ⚠️ **Migration Complexity**: Large data migrations need care
- ⚠️ **Undo Migrations**: Manual creation required

### Technical Debt

**Identified Debt**:
1. Existing schema not in Flyway
2. No automated rollback testing
3. Manual undo migration creation
4. Limited large data migration patterns

**Debt Repayment Plan**:
- **Q1 2026**: Baseline all existing schemas
- **Q2 2026**: Automated rollback testing
- **Q3 2026**: Large data migration framework
- **Q4 2026**: Automated undo generation

## Related Decisions

- [ADR-019: Progressive Deployment Strategy](019-progressive-deployment-strategy.md)
- [ADR-021: Event Sourcing for Critical Aggregates](021-event-sourcing-critical-aggregates.md)
- [ADR-025: Saga Pattern for Distributed Transactions](025-saga-pattern-distributed-transactions.md)

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-25  
**Next Review**: 2026-01-25 (Quarterly)

## Notes

### Migration Best Practices

**DO**:
- ✅ Use backward compatible changes
- ✅ Test migrations in staging first
- ✅ Create backups before production
- ✅ Use descriptive migration names
- ✅ Keep migrations small and focused
- ✅ Document complex migrations
- ✅ Use transactions for safety

**DON'T**:
- ❌ Modify existing migrations
- ❌ Skip version numbers
- ❌ Use out-of-order migrations
- ❌ Make breaking changes without grace period
- ❌ Run migrations manually in production
- ❌ Ignore validation errors

### Common Migration Patterns

**Add Column**:
```sql
-- Phase 1: Add nullable column
ALTER TABLE table_name ADD COLUMN new_column TYPE;

-- Phase 2: Populate data
UPDATE table_name SET new_column = default_value;

-- Phase 3: Make required
ALTER TABLE table_name ALTER COLUMN new_column SET NOT NULL;
```

**Rename Column**:
```sql
-- Phase 1: Add new column
ALTER TABLE table_name ADD COLUMN new_name TYPE;

-- Phase 2: Copy data
UPDATE table_name SET new_name = old_name;

-- Phase 3: Drop old column (after grace period)
ALTER TABLE table_name DROP COLUMN old_name;
```

**Split Table**:
```sql
-- Phase 1: Create new table
CREATE TABLE new_table (...);

-- Phase 2: Copy data
INSERT INTO new_table SELECT ... FROM old_table;

-- Phase 3: Drop old table (after validation)
DROP TABLE old_table;
```
