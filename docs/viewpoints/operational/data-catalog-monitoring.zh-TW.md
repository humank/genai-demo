# 資料目錄監控和運營指南

## 概述

本文檔描述 AWS Glue Data Catalog 的監控、告警和日常運營程序，確保自動化 schema 發現系統的穩定運行和高可用性。

## 監控架構

### 監控層級結構
```
┌─────────────────────────────────────────────────────────────────┐
│                    監控層級架構                                    │
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

### 核心監控指標

#### 基礎設施指標
```yaml
infrastructure_metrics:
  glue_crawler:
    - name: "CrawlerState"
      description: "Crawler 當前狀態 (READY, RUNNING, STOPPING)"
      threshold: "非 READY 狀態超過 30 分鐘"
      severity: "WARNING"
    
    - name: "CrawlerRunDuration"
      description: "Crawler 執行時間"
      threshold: "> 60 分鐘"
      severity: "WARNING"
    
    - name: "CrawlerFailureCount"
      description: "Crawler 失敗次數"
      threshold: "> 0 in 24 hours"
      severity: "CRITICAL"
  
  lambda_function:
    - name: "FunctionDuration"
      description: "Lambda 函數執行時間"
      threshold: "> 30 秒"
      severity: "WARNING"
    
    - name: "FunctionErrors"
      description: "Lambda 函數錯誤率"
      threshold: "> 1%"
      severity: "CRITICAL"
    
    - name: "FunctionThrottles"
      description: "Lambda 函數限流次數"
      threshold: "> 0"
      severity: "WARNING"
  
  eventbridge:
    - name: "RuleInvocations"
      description: "EventBridge 規則觸發次數"
      threshold: "< 1 in 48 hours"
      severity: "WARNING"
    
    - name: "FailedInvocations"
      description: "EventBridge 失敗觸發次數"
      threshold: "> 0"
      severity: "CRITICAL"
```

#### 應用程式指標
```yaml
application_metrics:
  schema_discovery:
    - name: "TablesDiscovered"
      description: "發現的表格總數"
      expected_range: "50-100 tables"
      threshold: "< 40 or > 120"
      severity: "WARNING"
    
    - name: "SchemaChangesDetected"
      description: "檢測到的 schema 變更"
      threshold: "> 10 per day"
      severity: "INFO"
    
    - name: "NewTablesAdded"
      description: "新增的表格數量"
      threshold: "> 5 per day"
      severity: "INFO"
  
  data_quality:
    - name: "TableCompletenessScore"
      description: "表格完整性分數"
      threshold: "< 95%"
      severity: "WARNING"
    
    - name: "SchemaConsistencyScore"
      description: "Schema 一致性分數"
      threshold: "< 98%"
      severity: "CRITICAL"
    
    - name: "DataFreshnessScore"
      description: "資料新鮮度分數"
      threshold: "< 90%"
      severity: "WARNING"
```

#### 業務指標
```yaml
business_metrics:
  coverage:
    - name: "BoundedContextCoverage"
      description: "Bounded Context 覆蓋率"
      expected: "13 contexts"
      threshold: "< 13"
      severity: "CRITICAL"
    
    - name: "TableCoveragePerContext"
      description: "每個 Context 的表格覆蓋率"
      threshold: "< 80%"
      severity: "WARNING"
  
  compliance:
    - name: "GDPRComplianceScore"
      description: "GDPR 合規分數"
      threshold: "< 100%"
      severity: "CRITICAL"
    
    - name: "DataRetentionCompliance"
      description: "資料保留政策合規性"
      threshold: "< 95%"
      severity: "WARNING"
  
  adoption:
    - name: "CatalogQueryCount"
      description: "目錄查詢次數"
      threshold: "< 100 per day"
      severity: "INFO"
    
    - name: "GenBIIntegrationUsage"
      description: "GenBI 整合使用率"
      threshold: "< 50 queries per day"
      severity: "INFO"
```

## CloudWatch 儀表板配置

### 主要儀表板: GenAI-Demo-Data-Catalog-Monitoring

#### Widget 1: Crawler 執行狀態
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

#### Widget 2: 表格發現趨勢
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

#### Widget 3: 資料品質指標
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

#### Widget 4: 成本監控
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

### 次要儀表板: Data-Catalog-Operational-Details

#### Widget 1: Lambda 函數效能
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

#### Widget 2: EventBridge 規則活動
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

## 告警配置

### 關鍵告警 (CRITICAL)

#### 1. Crawler 執行失敗告警
```yaml
alarm_name: "DataCatalog-CrawlerExecutionFailure"
description: "Glue Crawler 執行失敗，需要立即處理"
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

#### 2. Schema 一致性嚴重下降告警
```yaml
alarm_name: "DataCatalog-SchemaConsistencyDegraded"
description: "Schema 一致性分數嚴重下降，可能影響資料品質"
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

#### 3. Bounded Context 覆蓋不完整告警
```yaml
alarm_name: "DataCatalog-BoundedContextIncomplete"
description: "Bounded Context 覆蓋不完整，可能有表格未被發現"
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

### 警告告警 (WARNING)

#### 1. Crawler 執行時間過長告警
```yaml
alarm_name: "DataCatalog-CrawlerExecutionSlow"
description: "Glue Crawler 執行時間超過預期，可能需要優化"
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

#### 2. 資料完整性下降告警
```yaml
alarm_name: "DataCatalog-DataCompletenessDegraded"
description: "資料完整性分數下降，需要檢查資料品質"
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

### 資訊告警 (INFO)

#### 1. 新表格發現告警
```yaml
alarm_name: "DataCatalog-NewTablesDiscovered"
description: "發現新表格，請檢查是否需要更新資料治理政策"
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

## 運營程序

### 日常運營檢查清單

#### 每日檢查 (自動化)
```bash
#!/bin/bash
# daily-data-catalog-check.sh

echo "=== 每日資料目錄健康檢查 ==="
echo "執行時間: $(date)"

# 1. 檢查 Crawler 狀態
echo "1. 檢查 Glue Crawler 狀態..."
CRAWLER_STATE=$(aws glue get-crawler --name genai-demo-aurora-auto-discovery --query 'Crawler.State' --output text)
echo "   Crawler 狀態: $CRAWLER_STATE"

if [ "$CRAWLER_STATE" != "READY" ]; then
    echo "   ⚠️  警告: Crawler 不在 READY 狀態"
fi

# 2. 檢查表格數量
echo "2. 檢查發現的表格數量..."
TABLE_COUNT=$(aws glue get-tables --database-name genai_demo_catalog --query 'length(TableList)' --output text)
echo "   發現的表格數量: $TABLE_COUNT"

if [ "$TABLE_COUNT" -lt 40 ]; then
    echo "   ⚠️  警告: 表格數量少於預期 (< 40)"
fi

# 3. 檢查最近的 Crawler 執行
echo "3. 檢查最近的 Crawler 執行..."
LAST_RUN=$(aws glue get-crawler-metrics --crawler-name-list genai-demo-aurora-auto-discovery --query 'CrawlerMetricsList[0].LastRuntimeSeconds' --output text)
echo "   最後執行時間: ${LAST_RUN} 秒"

if [ "$LAST_RUN" -gt 3600 ]; then
    echo "   ⚠️  警告: 最後執行時間超過 1 小時"
fi

# 4. 檢查 Lambda 函數錯誤
echo "4. 檢查 Lambda 函數錯誤..."
LAMBDA_ERRORS=$(aws logs filter-log-events \
    --log-group-name /aws/lambda/DataCatalogStack-TriggerCrawlerFunction \
    --start-time $(date -d '24 hours ago' +%s)000 \
    --filter-pattern "ERROR" \
    --query 'length(events)' --output text)
echo "   24小時內 Lambda 錯誤數: $LAMBDA_ERRORS"

if [ "$LAMBDA_ERRORS" -gt 0 ]; then
    echo "   ⚠️  警告: Lambda 函數有錯誤發生"
fi

echo "=== 每日檢查完成 ==="
```

#### 每週檢查 (手動)
- [ ] 檢查所有 13 個 bounded context 的表格覆蓋率
- [ ] 驗證資料品質指標趨勢
- [ ] 檢查成本使用情況和預算
- [ ] 檢查告警配置是否需要調整
- [ ] 檢查 IAM 權限和安全設定

#### 每月檢查 (手動)
- [ ] 檢查 Glue Catalog 資料準確性
- [ ] 更新資料治理政策和標籤
- [ ] 檢查合規性報告
- [ ] 檢查災難恢復程序
- [ ] 檢查效能優化機會

### 故障排除程序

#### Crawler 執行失敗處理
```bash
#!/bin/bash
# troubleshoot-crawler-failure.sh

echo "=== Crawler 故障排除程序 ==="

# 1. 獲取 Crawler 詳細狀態
echo "1. 獲取 Crawler 詳細資訊..."
aws glue get-crawler --name genai-demo-aurora-auto-discovery

# 2. 檢查最近的執行日誌
echo "2. 檢查 Crawler 執行日誌..."
aws logs filter-log-events \
    --log-group-name /aws-glue/crawlers \
    --start-time $(date -d '2 hours ago' +%s)000 \
    --filter-pattern "genai-demo-aurora-auto-discovery"

# 3. 檢查資料庫連線
echo "3. 測試資料庫連線..."
aws glue get-connection --name genai-demo-aurora-connection

# 4. 檢查 IAM 權限
echo "4. 檢查 IAM 角色權限..."
aws iam get-role --role-name DataCatalogStack-GlueCrawlerRole

# 5. 嘗試手動啟動 Crawler
echo "5. 嘗試手動啟動 Crawler..."
aws glue start-crawler --name genai-demo-aurora-auto-discovery

echo "=== 故障排除完成 ==="
```

#### Lambda 函數錯誤處理
```bash
#!/bin/bash
# troubleshoot-lambda-errors.sh

echo "=== Lambda 函數故障排除程序 ==="

# 1. 檢查函數配置
echo "1. 檢查 Lambda 函數配置..."
aws lambda get-function --function-name DataCatalogStack-TriggerCrawlerFunction

# 2. 檢查最近的錯誤日誌
echo "2. 檢查 Lambda 錯誤日誌..."
aws logs filter-log-events \
    --log-group-name /aws/lambda/DataCatalogStack-TriggerCrawlerFunction \
    --start-time $(date -d '1 hour ago' +%s)000 \
    --filter-pattern "ERROR"

# 3. 檢查函數指標
echo "3. 檢查 Lambda 函數指標..."
aws cloudwatch get-metric-statistics \
    --namespace AWS/Lambda \
    --metric-name Errors \
    --dimensions Name=FunctionName,Value=DataCatalogStack-TriggerCrawlerFunction \
    --start-time $(date -d '1 hour ago' --iso-8601) \
    --end-time $(date --iso-8601) \
    --period 300 \
    --statistics Sum

# 4. 測試函數執行
echo "4. 測試 Lambda 函數..."
aws lambda invoke \
    --function-name DataCatalogStack-TriggerCrawlerFunction \
    --payload '{"source":"manual-test","detail":{"EventCategories":["configuration change"]}}' \
    response.json

echo "=== Lambda 故障排除完成 ==="
```

### 效能優化程序

#### Crawler 效能調優
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
    primary_schedule: "cron(0 2 * * ? *)"  # 每日 2 AM
    incremental_schedule: "cron(0 */6 * * ? *)"  # 每 6 小時增量掃描
```

#### 成本優化策略
```yaml
# cost-optimization.yml
cost_control_measures:
  crawler_optimization:
    - "使用增量掃描減少執行時間"
    - "優化排除規則避免掃描不必要的表格"
    - "調整取樣大小平衡準確性和成本"
  
  logging_optimization:
    - "設定合理的日誌保留期限 (7 天)"
    - "使用日誌過濾減少儲存成本"
    - "啟用日誌壓縮"
  
  monitoring_optimization:
    - "使用標準解析度指標"
    - "合併相似的告警規則"
    - "優化儀表板查詢頻率"
  
  estimated_monthly_savings: "$1.20"
```

## 災難恢復

### 備份策略
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

### 恢復程序
```bash
#!/bin/bash
# disaster-recovery-procedure.sh

echo "=== 資料目錄災難恢復程序 ==="

# 1. 檢查備份可用性
echo "1. 檢查最新備份..."
aws s3 ls s3://genai-demo-backup/glue-catalog/ --recursive | tail -5

# 2. 恢復 Glue Database
echo "2. 恢復 Glue Database..."
aws glue create-database --database-input file://backup/glue-database.json

# 3. 恢復表格定義
echo "3. 恢復表格定義..."
for table_file in backup/tables/*.json; do
    aws glue create-table --database-name genai_demo_catalog --table-input file://$table_file
done

# 4. 重新部署 Crawler
echo "4. 重新部署 Crawler..."
cd infrastructure
npx cdk deploy DataCatalogStack --require-approval never

# 5. 驗證恢復結果
echo "5. 驗證恢復結果..."
TABLE_COUNT=$(aws glue get-tables --database-name genai_demo_catalog --query 'length(TableList)' --output text)
echo "   恢復的表格數量: $TABLE_COUNT"

echo "=== 災難恢復完成 ==="
```

## 合規性監控

### GDPR 合規檢查
```sql
-- gdpr-compliance-check.sql
-- 檢查 PII 資料的標籤和分類

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
    -- 檢查是否有適當的標籤
    CASE 
        WHEN table_comment ILIKE '%gdpr%' THEN 'Tagged'
        ELSE 'Missing GDPR Tag'
    END as gdpr_compliance_status
FROM information_schema.columns 
WHERE table_schema = 'public'
ORDER BY data_classification, table_name;
```

### 資料保留政策檢查
```sql
-- data-retention-check.sql
-- 檢查資料保留政策合規性

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

## 整合測試

### 端到端測試腳本
```bash
#!/bin/bash
# end-to-end-test.sh

echo "=== 資料目錄端到端測試 ==="

# 1. 測試 Crawler 手動觸發
echo "1. 測試 Crawler 手動觸發..."
aws glue start-crawler --name genai-demo-aurora-auto-discovery
sleep 10

# 2. 測試 Lambda 函數觸發
echo "2. 測試 Lambda 函數觸發..."
aws lambda invoke \
    --function-name DataCatalogStack-TriggerCrawlerFunction \
    --payload '{"source":"aws.rds","detail-type":"RDS DB Instance Event","detail":{"EventCategories":["configuration change"],"SourceId":"genai-demo-cluster"}}' \
    response.json

# 3. 測試資料目錄查詢
echo "3. 測試資料目錄查詢..."
aws glue get-tables --database-name genai_demo_catalog --max-items 5

# 4. 測試 GenBI 整合
echo "4. 測試 GenBI 整合..."
python3 << EOF
import boto3
glue = boto3.client('glue')
tables = glue.get_tables(DatabaseName='genai_demo_catalog')
print(f"GenBI 可用表格數量: {len(tables['TableList'])}")
for table in tables['TableList'][:3]:
    print(f"  - {table['Name']}: {len(table['StorageDescriptor']['Columns'])} columns")
EOF

# 5. 測試監控指標
echo "5. 測試監控指標..."
aws cloudwatch put-metric-data \
    --namespace Custom/DataCatalog \
    --metric-data MetricName=TestMetric,Value=1,Unit=Count

echo "=== 端到端測試完成 ==="
```

---

**文檔版本**: 1.0  
**建立日期**: 2025年9月24日 下午3:35 (台北時間)  
**負責團隊**: DevOps 和 SRE 團隊  
**審核者**: 運營主管  
**下次審核**: 2025年12月24日
