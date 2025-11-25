# Cost and Usage Reports (CUR) Insights

**Last Updated**: 2025-10-22  
**Status**: Implemented  
**Requirements**: 13.22, 13.23, 13.24

## Overview

The Cost and Usage Reports (CUR) Insights system provides detailed cost breakdown and attribution reporting with automated anomaly detection and budget overspend risk early warning.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   AWS Cost and Usage Reports                 │
│                                                              │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐ │
│  │ Hourly CUR   │───▶│  S3 Bucket   │───▶│ Glue Crawler │ │
│  │  Generation  │    │  (Parquet)   │    │  (Daily)     │ │
│  └──────────────┘    └──────────────┘    └──────────────┘ │
│                                                   │          │
│                                                   ▼          │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐ │
│  │   Athena     │◀───│ Glue Data    │    │  Lambda      │ │
│  │  Workgroup   │    │  Catalog     │    │  Anomaly     │ │
│  └──────────────┘    └──────────────┘    │  Detection   │ │
│         │                                 └──────────────┘ │
│         │                                        │          │
│         ▼                                        ▼          │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐ │
│  │ CloudWatch   │    │     SNS      │    │  CloudWatch  │ │
│  │  Dashboard   │    │    Alerts    │    │   Alarms     │ │
│  └──────────────┘    └──────────────┘    └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## Key Features

### 1. Detailed Cost Breakdown (Requirement 13.22)

- **Hourly Granularity**: Cost data collected every hour for precise analysis
- **Resource-Level Attribution**: Track costs down to individual resources
- **Service-Level Breakdown**: Understand costs by AWS service
- **Parquet Format**: Efficient storage and querying with columnar format
- **1-Year Retention**: Historical data for trend analysis

### 2. Cost Anomaly Detection with Root Cause Analysis (Requirement 13.23)

- **Automated Detection**: Daily analysis at 3 AM
- **Statistical Analysis**: Identifies cost increases >20%
- **Root Cause Analysis**:
  - Service-level cost breakdown
  - Resource-level attribution
  - Configuration change correlation
  - Usage spike identification
- **CloudWatch Metrics**: Real-time anomaly tracking
- **SNS Notifications**: Immediate alerts for anomalies

### 3. Budget Overspend Risk Early Warning (Requirement 13.24)

- **Predictive Analysis**: Forecasts end-of-month costs
- **Risk Levels**: Low, Medium, High risk classification
- **Days to Overspend**: Calculates when budget will be exceeded
- **Proactive Alerts**: Warnings before budget is exceeded
- **Trend Analysis**: Identifies spending acceleration

## Deployment

### Prerequisites

- AWS CDK installed and configured
- AWS account with billing access
- Existing cost management stack (from task 22)

### Deploy the Stack

```bash
cd infrastructure

# Deploy Cost and Usage Reports stack
cdk deploy CostUsageReportsStack

# Note: CUR reports may take 24 hours to start generating data
```

### Configuration

1. **Enable Cost and Usage Reports**:
   - Reports are automatically configured with hourly granularity
   - Parquet format for efficient querying
   - Resource-level details included

2. **Configure SNS Notifications**:
   ```bash
   # Subscribe email to cost alert topic
   aws sns subscribe \
     --topic-arn $(aws cloudformation describe-stacks \
       --stack-name CostUsageReportsStack \
       --query 'Stacks[0].Outputs[?OutputKey==`CostAlertTopicArn`].OutputValue' \
       --output text) \
     --protocol email \
     --notification-endpoint devops@example.com
   ```

3. **Verify Glue Crawler**:
   ```bash
   # Run Glue crawler manually for first-time setup
   aws glue start-crawler --name cost-usage-reports-crawler
   ```

## Usage

### Query Cost Data with Athena

#### 1. Daily Cost Trend Analysis

```sql
-- Query daily costs for the last 30 days
SELECT 
    DATE(line_item_usage_start_date) as usage_date,
    SUM(line_item_unblended_cost) as daily_cost
FROM cost_usage_reports.cost_usage_reports
WHERE line_item_usage_start_date >= date_add('day', -30, current_date)
GROUP BY DATE(line_item_usage_start_date)
ORDER BY usage_date DESC;
```

#### 2. Service-Level Cost Breakdown

```sql
-- Top 10 services by cost (last 7 days)
SELECT 
    product_servicename,
    SUM(line_item_unblended_cost) as service_cost,
    COUNT(DISTINCT line_item_resource_id) as resource_count
FROM cost_usage_reports.cost_usage_reports
WHERE line_item_usage_start_date >= date_add('day', -7, current_date)
GROUP BY product_servicename
ORDER BY service_cost DESC
LIMIT 10;
```

#### 3. Resource-Level Cost Attribution

```sql
-- Most expensive resources (last 7 days)
SELECT 
    line_item_resource_id,
    product_servicename,
    SUM(line_item_unblended_cost) as resource_cost,
    line_item_usage_type
FROM cost_usage_reports.cost_usage_reports
WHERE line_item_usage_start_date >= date_add('day', -7, current_date)
    AND line_item_resource_id IS NOT NULL
GROUP BY line_item_resource_id, product_servicename, line_item_usage_type
ORDER BY resource_cost DESC
LIMIT 20;
```

#### 4. Cost Anomaly Detection Query

```sql
-- Compare this week vs last week costs
WITH this_week AS (
    SELECT 
        product_servicename,
        SUM(line_item_unblended_cost) as cost
    FROM cost_usage_reports.cost_usage_reports
    WHERE line_item_usage_start_date >= date_add('day', -7, current_date)
    GROUP BY product_servicename
),
last_week AS (
    SELECT 
        product_servicename,
        SUM(line_item_unblended_cost) as cost
    FROM cost_usage_reports.cost_usage_reports
    WHERE line_item_usage_start_date >= date_add('day', -14, current_date)
        AND line_item_usage_start_date < date_add('day', -7, current_date)
    GROUP BY product_servicename
)
SELECT 
    tw.product_servicename,
    tw.cost as this_week_cost,
    lw.cost as last_week_cost,
    ((tw.cost - lw.cost) / lw.cost * 100) as percent_change
FROM this_week tw
JOIN last_week lw ON tw.product_servicename = lw.product_servicename
WHERE ((tw.cost - lw.cost) / lw.cost * 100) > 20
ORDER BY percent_change DESC;
```

#### 5. Budget Forecast Query

```sql
-- Calculate daily average and forecast month-end cost
WITH daily_costs AS (
    SELECT 
        DATE(line_item_usage_start_date) as usage_date,
        SUM(line_item_unblended_cost) as daily_cost
    FROM cost_usage_reports.cost_usage_reports
    WHERE MONTH(line_item_usage_start_date) = MONTH(current_date)
        AND YEAR(line_item_usage_start_date) = YEAR(current_date)
    GROUP BY DATE(line_item_usage_start_date)
)
SELECT 
    AVG(daily_cost) as avg_daily_cost,
    SUM(daily_cost) as month_to_date_cost,
    AVG(daily_cost) * DAY(LAST_DAY(current_date)) as projected_monthly_cost,
    DAY(current_date) as days_elapsed,
    DAY(LAST_DAY(current_date)) - DAY(current_date) as days_remaining
FROM daily_costs;
```

### CloudWatch Dashboard

Access the Cost Insights Dashboard:

```bash
# Get dashboard URL
aws cloudformation describe-stacks \
  --stack-name CostUsageReportsStack \
  --query 'Stacks[0].Outputs[?OutputKey==`CostInsightsDashboardURL`].OutputValue' \
  --output text
```

Dashboard includes:
- **Cost Anomalies Detected**: Daily count of detected anomalies
- **Budget Overspend Risk**: Risk level indicator
- **Lambda Execution Metrics**: Anomaly detection function performance

### Automated Anomaly Detection

The Lambda function runs daily at 3 AM and:

1. **Analyzes Cost Trends**: Compares last 7 days vs previous 7 days
2. **Detects Anomalies**: Identifies cost increases >20%
3. **Performs Root Cause Analysis**:
   - Service-level breakdown
   - Resource-level attribution
   - Usage pattern analysis
4. **Checks Budget Risk**: Forecasts end-of-month costs
5. **Sends Alerts**: SNS notifications for anomalies and high risk

### Manual Trigger

```bash
# Manually trigger anomaly detection
aws lambda invoke \
  --function-name cost-anomaly-detection \
  --payload '{}' \
  response.json

cat response.json
```

## Integration with Existing Cost Management

This CUR Insights system complements the existing cost management from task 22:

| Feature | Task 22 (Cost Management) | Task 62 (CUR Insights) |
|---------|---------------------------|------------------------|
| **Granularity** | Daily (Cost Explorer) | Hourly (CUR) |
| **Data Format** | API-based | Parquet files |
| **Query Method** | AWS CLI / MCP Tools | Athena SQL |
| **Anomaly Detection** | AWS Cost Anomaly Detection | Custom Lambda analysis |
| **Budgets** | AWS Budgets service | Forecast-based risk analysis |
| **Attribution** | Service-level | Resource-level |
| **Retention** | 12 months (Cost Explorer) | 1 year (configurable) |
| **Cost** | Free (Cost Explorer) | S3 + Athena query costs |

## Cost Optimization

### CUR Storage Costs

- **S3 Storage**: ~$0.023/GB/month (Standard tier)
- **Lifecycle Policies**: Automatic transition to Glacier after 90 days
- **Estimated Monthly Cost**: $5-10 for typical usage

### Athena Query Costs

- **Per Query**: $5 per TB scanned
- **Parquet Format**: 10x more efficient than CSV
- **Partition Pruning**: Reduces data scanned
- **Estimated Monthly Cost**: $10-20 for regular analysis

### Lambda Execution Costs

- **Daily Execution**: 1 invocation/day
- **Duration**: ~30 seconds
- **Memory**: 512 MB
- **Estimated Monthly Cost**: <$1

**Total Estimated Cost**: $15-30/month

## Monitoring and Alerts

### CloudWatch Alarms

1. **Cost Anomaly Detected**:
   - Threshold: ≥1 anomaly detected
   - Action: SNS notification
   - Evaluation: Daily

2. **Budget Overspend Risk High**:
   - Threshold: Risk level = High
   - Action: SNS notification
   - Evaluation: Daily

### SNS Notifications

Alert format:
```
Cost Anomaly Detection Alert

Anomalies Detected: 2
Budget Risk Level: medium
Projected Monthly Cost: $4500
Budget Limit: $5000

Root Causes:
- EKS node scaling increased by 30%
- RDS instance upgraded to larger size

Action Required:
- Review cost breakdown in CloudWatch dashboard
- Check Athena queries for detailed analysis
- Implement recommended optimizations
```

## Troubleshooting

### CUR Reports Not Generating

**Issue**: No data in S3 bucket after 24 hours

**Solution**:
```bash
# Check CUR report status
aws cur describe-report-definitions

# Verify S3 bucket permissions
aws s3api get-bucket-policy --bucket genai-demo-cur-ACCOUNT-REGION
```

### Glue Crawler Failing

**Issue**: Crawler fails to discover schema

**Solution**:
```bash
# Check crawler logs
aws glue get-crawler --name cost-usage-reports-crawler

# Manually run crawler
aws glue start-crawler --name cost-usage-reports-crawler

# Check crawler status
aws glue get-crawler-metrics --crawler-name-list cost-usage-reports-crawler
```

### Athena Query Errors

**Issue**: "Table not found" error

**Solution**:
```bash
# Verify Glue database exists
aws glue get-database --name cost_usage_reports

# Check tables in database
aws glue get-tables --database-name cost_usage_reports

# Wait for Glue crawler to complete
aws glue get-crawler --name cost-usage-reports-crawler
```

### Lambda Timeout

**Issue**: Anomaly detection Lambda times out

**Solution**:
- Increase Lambda timeout (currently 5 minutes)
- Optimize Athena queries with partitioning
- Use Step Functions for long-running analysis

## Best Practices

1. **Query Optimization**:
   - Use date partitions to reduce data scanned
   - Leverage Parquet columnar format
   - Create views for common queries

2. **Cost Control**:
   - Set Athena query byte limits
   - Use workgroup query result reuse
   - Archive old reports to Glacier

3. **Anomaly Detection**:
   - Tune threshold based on business patterns
   - Exclude expected cost increases (e.g., planned scaling)
   - Review false positives weekly

4. **Budget Management**:
   - Update budget limits quarterly
   - Adjust risk thresholds based on business needs
   - Review forecast accuracy monthly

## Related Documentation

- [AWS Cost and Usage Reports Documentation](https://docs.aws.amazon.com/cur/latest/userguide/what-is-cur.html)

## Success Criteria

- [x] Detailed cost breakdown enabled with hourly granularity
- [x] Resource-level cost attribution implemented
- [x] Automated cost anomaly detection with >20% threshold
- [x] Root cause analysis for detected anomalies
- [x] Budget overspend risk early warning system
- [x] CloudWatch dashboard for cost insights
- [x] SNS notifications for anomalies and high risk
- [x] Athena workgroup for SQL-based cost analysis
- [x] Glue Data Catalog for cost data querying
- [x] Integration with existing cost management (task 22)

## Completion Status

**Status**: ✅ **COMPLETED**  
**Date**: 2025-10-22  
**Requirements Met**: 13.22, 13.23, 13.24

**Key Achievements**:
- Hourly cost data collection with resource-level details
- Automated anomaly detection with root cause analysis
- Budget overspend risk prediction and early warning
- SQL-based cost analysis with Athena
- Integration with existing cost management infrastructure
