# Data Catalog Monitoring and Operations Guide

## Overview

This document describes the monitoring, alerting, and daily operational procedures for AWS Glue Data Catalog, ensuring stable operation and high availability of the automated schema discovery system.

## Monitoring Architecture

### Monitoring Hierarchy Structure
```
┌─────────────────────────────────────────────────────────────────┐
│                    Monitoring Hierarchy Architecture            │
├─────────────────────────────────────────────────────────────────┤
│  Level 1: Infrastructure Monitoring                            │
│  ├── AWS Glue Crawler Status                                   │
│  ├── Lambda Function Health                                    │
│  ├── EventBridge Rule Status                                   │
│  └── IAM Role Permissions                                      │
├─────────────────────────────────────────────────────────────────┤
│  Level 2: Application Monitoring                               │
│  ├── Schema Discovery Success Rate                             │
│  ├── Table Update Frequency                                    │
│  ├── Data Quality Metrics                                      │
│  └── Crawler Execution Time                                    │
├─────────────────────────────────────────────────────────────────┤
│  Level 3: Business Monitoring                                  │
│  ├── Bounded Context Coverage                                  │
│  ├── Data Governance Compliance                                │
│  ├── Cost Optimization Metrics                                 │
│  └── User Adoption Metrics                                     │
└─────────────────────────────────────────────────────────────────┘
```

### Core Monitoring Metrics

#### Infrastructure Metrics
```yaml
infrastructure_metrics:
  glue_crawler:
    - name: "CrawlerState"
      description: "Current Crawler state (READY, RUNNING, STOPPING)"
      threshold: "Non-READY state for more than 30 minutes"
      severity: "WARNING"
    
    - name: "CrawlerRunDuration"
      description: "Crawler execution time"
      threshold: "> 60 minutes"
      severity: "WARNING"
    
    - name: "CrawlerFailureCount"
      description: "Crawler failure count"
      threshold: "> 0 in 24 hours"
      severity: "CRITICAL"
  
  lambda_function:
    - name: "FunctionDuration"
      description: "Lambda function execution time"
      threshold: "> 30 seconds"
      severity: "WARNING"
    
    - name: "FunctionErrors"
      description: "Lambda function error rate"
      threshold: "> 1%"
      severity: "CRITICAL"
    
    - name: "FunctionThrottles"
      description: "Lambda function throttle count"
      threshold: "> 0"
      severity: "WARNING"
  
  eventbridge:
    - name: "RuleInvocations"
      description: "EventBridge rule invocation count"
      threshold: "< 1 in 48 hours"
      severity: "WARNING"
    
    - name: "FailedInvocations"
      description: "EventBridge failed invocation count"
      threshold: "> 0"
      severity: "CRITICAL"
```

#### Application Metrics
```yaml
application_metrics:
  schema_discovery:
    - name: "TablesDiscovered"
      description: "Total number of tables discovered"
      expected_range: "50-100 tables"
      threshold: "< 40 or > 120"
      severity: "WARNING"
    
    - name: "SchemaChangesDetected"
      description: "Schema changes detected"
      threshold: "> 10 per day"
      severity: "INFO"
    
    - name: "NewTablesAdded"
      description: "Number of new tables added"
      threshold: "> 5 per day"
      severity: "INFO"
  
  data_quality:
    - name: "TableCompletenessScore"
      description: "Table completeness score"
      threshold: "< 95%"
      severity: "WARNING"
    
    - name: "SchemaConsistencyScore"
      description: "Schema consistency score"
      threshold: "< 98%"
      severity: "CRITICAL"
    
    - name: "DataFreshnessScore"
      description: "Data freshness score"
      threshold: "< 90%"
      severity: "WARNING"
```

#### Business Metrics
```yaml
business_metrics:
  coverage:
    - name: "BoundedContextCoverage"
      description: "Bounded Context coverage rate"
      expected: "13 contexts"
      threshold: "< 13"
      severity: "CRITICAL"
    
    - name: "TableCoveragePerContext"
      description: "Table coverage rate per Context"
      threshold: "< 80%"
      severity: "WARNING"
  
  compliance:
    - name: "GDPRComplianceScore"
      description: "GDPR compliance score"
      threshold: "< 100%"
      severity: "CRITICAL"
    
    - name: "DataRetentionCompliance"
      description: "Data retention policy compliance"
      threshold: "< 95%"
      severity: "WARNING"
  
  adoption:
    - name: "CatalogQueryCount"
      description: "Catalog query count"
      threshold: "< 100 per day"
      severity: "INFO"
    
    - name: "GenBIIntegrationUsage"
      description: "GenBI integration usage rate"
      threshold: "< 50 queries per day"
      severity: "INFO"
```

## CloudWatch Dashboard Configuration

### Main Dashboard: GenAI-Demo-Data-Catalog-Monitoring

#### Widget 1: Crawler Execution Status
```json
{
  "type": "metric",
  "properties": {
    "metrics": [
      ["AWS/Glue", "glue.driver.aggregate.numCompletedTasks", "JobName", "genai-demo-aurora-auto-discovery"],
      [".", "glue.driver.aggregate.numFailedTasks", ".", "."],
      [".", "glue.driver.jvm.heap.usage", ".", "."]
    ],
    "period": 300,
    "stat": "Sum",
    "region": "ap-northeast-1",
    "title": "Glue Crawler Execution Status",
    "yAxis": {
      "left": {
        "min": 0
      }
    }
  }
}
```

#### Widget 2: Table Discovery Trends
```json
{
  "type": "metric",
  "properties": {
    "metrics": [
      ["Custom/DataCatalog", "TablesDiscovered", "Database", "genai_demo_catalog"],
      [".", "NewTablesAdded", ".", "."],
      [".", "SchemaChangesDetected", ".", "."]
    ],
    "period": 3600,
    "stat": "Sum",
    "region": "ap-northeast-1",
    "title": "Table Discovery Trends",
    "view": "timeSeries"
  }
}
```

#### Widget 3: Data Quality Metrics
```json
{
  "type": "metric",
  "properties": {
    "metrics": [
      ["Custom/DataGovernance", "CompletenessScore", "Database", "genai_demo_catalog"],
      [".", "ConsistencyScore", ".", "."],
      [".", "FreshnessScore", ".", "."]
    ],
    "period": 3600,
    "stat": "Average",
    "region": "ap-northeast-1",
    "title": "Data Quality Metrics",
    "yAxis": {
      "left": {
        "min": 0,
        "max": 100
      }
    }
  }
}
```

#### Widget 4: Cost Monitoring
```json
{
  "type": "metric",
  "properties": {
    "metrics": [
      ["AWS/Billing", "EstimatedCharges", "ServiceName", "AWSGlue", "Currency", "USD"],
      [".", ".", ".", "AWSLambda", ".", "."],
      [".", ".", ".", "AmazonCloudWatch", ".", "."]
    ],
    "period": 86400,
    "stat": "Maximum",
    "region": "us-east-1",
    "title": "Daily Cost Breakdown"
  }
}
```

### Secondary Dashboard: Data-Catalog-Operational-Details

#### Widget 1: Lambda Function Performance
```json
{
  "type": "metric",
  "properties": {
    "metrics": [
      ["AWS/Lambda", "Duration", "FunctionName", "DataCatalogStack-TriggerCrawlerFunction"],
      [".", "Errors", ".", "."],
      [".", "Throttles", ".", "."],
      [".", "Invocations", ".", "."]
    ],
    "period": 300,
    "stat": "Average",
    "region": "ap-northeast-1",
    "title": "Lambda Function Performance"
  }
}
```

#### Widget 2: EventBridge Rule Activity
```json
{
  "type": "metric",
  "properties": {
    "metrics": [
      ["AWS/Events", "Invocations", "RuleName", "DataCatalogStack-RDSSchemaChangeRule"],
      [".", "FailedInvocations", ".", "."],
      [".", "MatchedEvents", ".", "."]
    ],
    "period": 300,
    "stat": "Sum",
    "region": "ap-northeast-1",
    "title": "EventBridge Rule Activity"
  }
}
```

## Alert Configuration

### Critical Alerts (CRITICAL)

#### 1. Crawler Execution Failure Alert
```yaml
alarm_name: "DataCatalog-CrawlerExecutionFailure"
description: "Glue Crawler execution failed, requires immediate attention"
metric_name: "glue.driver.aggregate.numFailedTasks"
namespace: "AWS/Glue"
dimensions:
  JobName: "genai-demo-aurora-auto-discovery"
statistic: "Sum"
period: 300
evaluation_periods: 1
threshold: 1
comparison_operator: "GreaterThanOrEqualToThreshold"
treat_missing_data: "notBreaching"
actions:
  - "arn:aws:sns:ap-northeast-1:ACCOUNT:DataCatalogCriticalAlerts"
```

#### 2. Schema Consistency Severely Degraded Alert
```yaml
alarm_name: "DataCatalog-SchemaConsistencyDegraded"
description: "Schema consistency score severely degraded, may affect data quality"
metric_name: "ConsistencyScore"
namespace: "Custom/DataGovernance"
dimensions:
  Database: "genai_demo_catalog"
statistic: "Average"
period: 3600
evaluation_periods: 2
threshold: 95
comparison_operator: "LessThanThreshold"
treat_missing_data: "breaching"
actions:
  - "arn:aws:sns:ap-northeast-1:ACCOUNT:DataCatalogCriticalAlerts"
```

#### 3. Bounded Context Coverage Incomplete Alert
```yaml
alarm_name: "DataCatalog-BoundedContextIncomplete"
description: "Bounded Context coverage incomplete, some tables may not be discovered"
metric_name: "BoundedContextCoverage"
namespace: "Custom/DataCatalog"
dimensions:
  Database: "genai_demo_catalog"
statistic: "Maximum"
period: 3600
evaluation_periods: 1
threshold: 13
comparison_operator: "LessThanThreshold"
treat_missing_data: "breaching"
actions:
  - "arn:aws:sns:ap-northeast-1:ACCOUNT:DataCatalogCriticalAlerts"
```

### Warning Alerts (WARNING)

#### 1. Crawler Execution Slow Alert
```yaml
alarm_name: "DataCatalog-CrawlerExecutionSlow"
description: "Glue Crawler execution time exceeds expected, may need optimization"
metric_name: "glue.driver.aggregate.elapsedTime"
namespace: "AWS/Glue"
dimensions:
  JobName: "genai-demo-aurora-auto-discovery"
statistic: "Maximum"
period: 300
evaluation_periods: 1
threshold: 3600  # 60 minutes
comparison_operator: "GreaterThanThreshold"
treat_missing_data: "notBreaching"
actions:
  - "arn:aws:sns:ap-northeast-1:ACCOUNT:DataCatalogWarningAlerts"
```

#### 2. Data Completeness Degraded Alert
```yaml
alarm_name: "DataCatalog-DataCompletenessDegraded"
description: "Data completeness score degraded, need to check data quality"
metric_name: "CompletenessScore"
namespace: "Custom/DataGovernance"
dimensions:
  Database: "genai_demo_catalog"
statistic: "Average"
period: 3600
evaluation_periods: 3
threshold: 90
comparison_operator: "LessThanThreshold"
treat_missing_data: "notBreaching"
actions:
  - "arn:aws:sns:ap-northeast-1:ACCOUNT:DataCatalogWarningAlerts"
```

### Info Alerts (INFO)

#### 1. New Tables Discovered Alert
```yaml
alarm_name: "DataCatalog-NewTablesDiscovered"
description: "New tables discovered, please check if data governance policies need updates"
metric_name: "NewTablesAdded"
namespace: "Custom/DataCatalog"
dimensions:
  Database: "genai_demo_catalog"
statistic: "Sum"
period: 86400  # 24 hours
evaluation_periods: 1
threshold: 1
comparison_operator: "GreaterThanOrEqualToThreshold"
treat_missing_data: "notBreaching"
actions:
  - "arn:aws:sns:ap-northeast-1:ACCOUNT:DataCatalogInfoAlerts"
```

## Operational Procedures

### Daily Operations Checklist

#### Daily Checks (Automated)
```bash
#!/bin/bash
# daily-data-catalog-check.sh

echo "=== Daily Data Catalog Health Check ==="
echo "Execution time: $(date)"

# 1. Check Crawler status
echo "1. Checking Glue Crawler status..."
CRAWLER_STATE=$(aws glue get-crawler --name genai-demo-aurora-auto-discovery --query 'Crawler.State' --output text)
echo "   Crawler state: $CRAWLER_STATE"

if [ "$CRAWLER_STATE" != "READY" ]; then
    echo "   ⚠️  Warning: Crawler is not in READY state"
fi

# 2. Check table count
echo "2. Checking discovered table count..."
TABLE_COUNT=$(aws glue get-tables --database-name genai_demo_catalog --query 'length(TableList)' --output text)
echo "   Discovered table count: $TABLE_COUNT"

if [ "$TABLE_COUNT" -lt 40 ]; then
    echo "   ⚠️  Warning: Table count is less than expected (< 40)"
fi

# 3. Check recent Crawler execution
echo "3. Checking recent Crawler execution..."
LAST_RUN=$(aws glue get-crawler-metrics --crawler-name-list genai-demo-aurora-auto-discovery --query 'CrawlerMetricsList[0].LastRuntimeSeconds' --output text)
echo "   Last execution time: ${LAST_RUN} seconds"

if [ "$LAST_RUN" -gt 3600 ]; then
    echo "   ⚠️  Warning: Last execution time exceeds 1 hour"
fi

# 4. Check Lambda function errors
echo "4. Checking Lambda function errors..."
LAMBDA_ERRORS=$(aws logs filter-log-events \
    --log-group-name /aws/lambda/DataCatalogStack-TriggerCrawlerFunction \
    --start-time $(date -d '24 hours ago' +%s)000 \
    --filter-pattern "ERROR" \
    --query 'length(events)' --output text)
echo "   Lambda errors in 24 hours: $LAMBDA_ERRORS"

if [ "$LAMBDA_ERRORS" -gt 0 ]; then
    echo "   ⚠️  Warning: Lambda function has errors"
fi

echo "=== Daily check completed ==="
```

#### Weekly Checks (Manual)
- [ ] Check table coverage rate for all 13 bounded contexts
- [ ] Verify data quality metrics trends
- [ ] Check cost usage and budget
- [ ] Check if alert configurations need adjustment
- [ ] Check IAM permissions and security settings

#### Monthly Checks (Manual)
- [ ] Check Glue Catalog data accuracy
- [ ] Update data governance policies and tags
- [ ] Check compliance reports
- [ ] Check disaster recovery procedures
- [ ] Check performance optimization opportunities

### Troubleshooting Procedures

#### Crawler Execution Failure Handling
```bash
#!/bin/bash
# troubleshoot-crawler-failure.sh

echo "=== Crawler Troubleshooting Procedure ==="

# 1. Get Crawler detailed status
echo "1. Getting Crawler detailed information..."
aws glue get-crawler --name genai-demo-aurora-auto-discovery

# 2. Check recent execution logs
echo "2. Checking Crawler execution logs..."
aws logs filter-log-events \
    --log-group-name /aws-glue/crawlers \
    --start-time $(date -d '2 hours ago' +%s)000 \
    --filter-pattern "genai-demo-aurora-auto-discovery"

# 3. Check database connection
echo "3. Testing database connection..."
aws glue get-connection --name genai-demo-aurora-connection

# 4. Check IAM permissions
echo "4. Checking IAM role permissions..."
aws iam get-role --role-name DataCatalogStack-GlueCrawlerRole

# 5. Try manual Crawler start
echo "5. Attempting manual Crawler start..."
aws glue start-crawler --name genai-demo-aurora-auto-discovery

echo "=== Troubleshooting completed ==="
```

#### Lambda Function Error Handling
```bash
#!/bin/bash
# troubleshoot-lambda-errors.sh

echo "=== Lambda Function Troubleshooting Procedure ==="

# 1. Check function configuration
echo "1. Checking Lambda function configuration..."
aws lambda get-function --function-name DataCatalogStack-TriggerCrawlerFunction

# 2. Check recent error logs
echo "2. Checking Lambda error logs..."
aws logs filter-log-events \
    --log-group-name /aws/lambda/DataCatalogStack-TriggerCrawlerFunction \
    --start-time $(date -d '1 hour ago' +%s)000 \
    --filter-pattern "ERROR"

# 3. Check function metrics
echo "3. Checking Lambda function metrics..."
aws cloudwatch get-metric-statistics \
    --namespace AWS/Lambda \
    --metric-name Errors \
    --dimensions Name=FunctionName,Value=DataCatalogStack-TriggerCrawlerFunction \
    --start-time $(date -d '1 hour ago' --iso-8601) \
    --end-time $(date --iso-8601) \
    --period 300 \
    --statistics Sum

# 4. Test function execution
echo "4. Testing Lambda function..."
aws lambda invoke \
    --function-name DataCatalogStack-TriggerCrawlerFunction \
    --payload '{"source":"manual-test","detail":{"EventCategories":["configuration change"]}}' \
    response.json

echo "=== Lambda troubleshooting completed ==="
```

### Performance Optimization Procedures

#### Crawler Performance Tuning
```yaml
# crawler-performance-tuning.yml
optimization_strategies:
  connection_pooling:
    max_connections: 2
    connection_timeout: 30
    idle_timeout: 300
  
  sampling_strategy:
    sample_size: 1000
    sampling_method: "random"
    full_scan_frequency: "weekly"
  
  exclusion_optimization:
    system_tables:
      - "information_schema/%"
      - "pg_catalog/%"
      - "pg_stat_%"
    temporary_tables:
      - "tmp_%"
      - "temp_%"
    audit_tables:
      - "flyway_schema_history"
  
  schedule_optimization:
    primary_schedule: "cron(0 2 * * ? *)"  # Daily at 2 AM
    incremental_schedule: "cron(0 */6 * * ? *)"  # Every 6 hours incremental scan
```

#### Cost Optimization Strategy
```yaml
# cost-optimization.yml
cost_control_measures:
  crawler_optimization:
    - "Use incremental scanning to reduce execution time"
    - "Optimize exclusion rules to avoid scanning unnecessary tables"
    - "Adjust sampling size to balance accuracy and cost"
  
  logging_optimization:
    - "Set reasonable log retention period (7 days)"
    - "Use log filtering to reduce storage costs"
    - "Enable log compression"
  
  monitoring_optimization:
    - "Use standard resolution metrics"
    - "Consolidate similar alert rules"
    - "Optimize dashboard query frequency"
  
  estimated_monthly_savings: "$1.20"
```

## Disaster Recovery

### Backup Strategy
```yaml
backup_strategy:
  glue_catalog_metadata:
    method: "AWS Glue ETL job export"
    frequency: "daily"
    retention: "30 days"
    storage: "S3 with versioning"
  
  crawler_configuration:
    method: "CloudFormation template backup"
    frequency: "on change"
    retention: "indefinite"
    storage: "Git repository"
  
  monitoring_configuration:
    method: "Terraform state backup"
    frequency: "on change"
    retention: "90 days"
    storage: "S3 with cross-region replication"
```

### Recovery Procedures
```bash
#!/bin/bash
# disaster-recovery-procedure.sh

echo "=== Data Catalog Disaster Recovery Procedure ==="

# 1. Check backup availability
echo "1. Checking latest backup..."
aws s3 ls s3://genai-demo-backup/glue-catalog/ --recursive | tail -5

# 2. Restore Glue Database
echo "2. Restoring Glue Database..."
aws glue create-database --database-input file://backup/glue-database.json

# 3. Restore table definitions
echo "3. Restoring table definitions..."
for table_file in backup/tables/*.json; do
    aws glue create-table --database-name genai_demo_catalog --table-input file://$table_file
done

# 4. Redeploy Crawler
echo "4. Redeploying Crawler..."
cd infrastructure
npx cdk deploy DataCatalogStack --require-approval never

# 5. Verify recovery results
echo "5. Verifying recovery results..."
TABLE_COUNT=$(aws glue get-tables --database-name genai_demo_catalog --query 'length(TableList)' --output text)
echo "   Recovered table count: $TABLE_COUNT"

echo "=== Disaster recovery completed ==="
```

## Compliance Monitoring

### GDPR Compliance Check
```sql
-- gdpr-compliance-check.sql
-- Check PII data tagging and classification

SELECT 
    table_name,
    column_name,
    data_type,
    CASE 
        WHEN column_name ILIKE '%email%' OR 
             column_name ILIKE '%phone%' OR 
             column_name ILIKE '%address%' THEN 'PII'
        WHEN column_name ILIKE '%payment%' OR 
             column_name ILIKE '%card%' OR 
             column_name ILIKE '%account%' THEN 'Financial'
        ELSE 'Non-sensitive'
    END as data_classification,
    -- Check for appropriate tags
    CASE 
        WHEN table_comment ILIKE '%gdpr%' THEN 'Tagged'
        ELSE 'Missing GDPR Tag'
    END as gdpr_compliance_status
FROM information_schema.columns 
WHERE table_schema = 'public'
ORDER BY data_classification, table_name;
```

### Data Retention Policy Check
```sql
-- data-retention-check.sql
-- Check data retention policy compliance

WITH table_age AS (
    SELECT 
        schemaname,
        tablename,
        EXTRACT(DAYS FROM (CURRENT_DATE - 
            (SELECT MIN(created_date) FROM pg_stat_user_tables 
             WHERE schemaname = t.schemaname AND relname = t.tablename)
        )) as days_old
    FROM pg_tables t
    WHERE schemaname = 'public'
)
SELECT 
    tablename,
    days_old,
    CASE 
        WHEN tablename ILIKE '%customer%' AND days_old > 2555 THEN 'Retention Violation (>7 years)'
        WHEN tablename ILIKE '%order%' AND days_old > 3650 THEN 'Retention Violation (>10 years)'
        WHEN tablename ILIKE '%audit%' THEN 'Permanent Retention'
        ELSE 'Compliant'
    END as retention_status
FROM table_age
ORDER BY days_old DESC;
```

## Integration Testing

### End-to-End Test Script
```bash
#!/bin/bash
# end-to-end-test.sh

echo "=== Data Catalog End-to-End Test ==="

# 1. Test manual Crawler trigger
echo "1. Testing manual Crawler trigger..."
aws glue start-crawler --name genai-demo-aurora-auto-discovery
sleep 10

# 2. Test Lambda function trigger
echo "2. Testing Lambda function trigger..."
aws lambda invoke \
    --function-name DataCatalogStack-TriggerCrawlerFunction \
    --payload '{"source":"aws.rds","detail-type":"RDS DB Instance Event","detail":{"EventCategories":["configuration change"],"SourceId":"genai-demo-cluster"}}' \
    response.json

# 3. Test data catalog query
echo "3. Testing data catalog query..."
aws glue get-tables --database-name genai_demo_catalog --max-items 5

# 4. Test GenBI integration
echo "4. Testing GenBI integration..."
python3 << EOF
import boto3
glue = boto3.client('glue')
tables = glue.get_tables(DatabaseName='genai_demo_catalog')
print(f"GenBI available table count: {len(tables['TableList'])}")
for table in tables['TableList'][:3]:
    print(f"  - {table['Name']}: {len(table['StorageDescriptor']['Columns'])} columns")
EOF

# 5. Test monitoring metrics
echo "5. Testing monitoring metrics..."
aws cloudwatch put-metric-data \
    --namespace Custom/DataCatalog \
    --metric-data MetricName=TestMetric,Value=1,Unit=Count

echo "=== End-to-end test completed ==="
```

---

**Document Version**: 1.0  
**Created Date**: September 24, 2025 3:35 PM (Taipei Time)  
**Responsible Team**: DevOps and SRE Team  
**Reviewer**: Operations Manager  
**Next Review**: December 24, 2025
