# AWS Glue 服務配置架構

## 概述

本文檔描述 GenAI Demo 應用程式中 AWS Glue 服務的基礎設施配置，包括 Data Catalog、Crawler、網路連接和安全設定的詳細架構。

## 基礎設施架構

### 整體架構圖
```
┌─────────────────────────────────────────────────────────────────┐
│                    AWS Glue Infrastructure                      │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐    ┌─────────────────┐    ┌──────────────┐ │
│  │   Glue Catalog  │    │  Glue Crawler   │    │   Lambda     │ │
│  │                 │    │                 │    │   Trigger    │ │
│  │ ┌─────────────┐ │    │ ┌─────────────┐ │    │              │ │
│  │ │  Database   │ │    │ │  Scheduler  │ │    │ ┌──────────┐ │ │
│  │ │ genai_demo_ │ │    │ │  Daily 2AM  │ │    │ │ RDS Event│ │ │
│  │ │  catalog    │ │    │ └─────────────┘ │    │ │ Handler  │ │ │
│  │ └─────────────┘ │    │                 │    │ └──────────┘ │ │
│  │                 │    │ ┌─────────────┐ │    │              │ │
│  │ ┌─────────────┐ │    │ │ JDBC Target │ │    │ ┌──────────┐ │ │
│  │ │   Tables    │ │◄───┤ │   Aurora    │ │    │ │ Crawler  │ │ │
│  │ │  Metadata   │ │    │ │ PostgreSQL  │ │    │ │ Trigger  │ │ │
│  │ └─────────────┘ │    │ └─────────────┘ │    │ └──────────┘ │ │
│  └─────────────────┘    └─────────────────┘    └──────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Network Infrastructure                       │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐    ┌─────────────────┐    ┌──────────────┐ │
│  │   VPC Network   │    │ Security Groups │    │ IAM Roles    │ │
│  │                 │    │                 │    │              │ │
│  │ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌──────────┐ │ │
│  │ │ Private     │ │    │ │   Aurora    │ │    │ │  Glue    │ │ │
│  │ │ Subnets     │ │    │ │ Security    │ │    │ │ Crawler  │ │ │
│  │ │ Multi-AZ    │ │    │ │   Group     │ │    │ │   Role   │ │ │
│  │ └─────────────┘ │    │ └─────────────┘ │    │ └──────────┘ │ │
│  │                 │    │                 │    │              │ │
│  │ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌──────────┐ │ │
│  │ │   NAT       │ │    │ │   Glue      │ │    │ │ Lambda   │ │ │
│  │ │ Gateways    │ │    │ │ Connection  │ │    │ │ Execution│ │ │
│  │ │             │ │    │ │ Security    │ │    │ │   Role   │ │ │
│  │ └─────────────┘ │    │ └─────────────┘ │    │ └──────────┘ │ │
│  └─────────────────┘    └─────────────────┘    └──────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                  Monitoring Infrastructure                      │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐    ┌─────────────────┐    ┌──────────────┐ │
│  │   CloudWatch    │    │   EventBridge   │    │     SNS      │ │
│  │                 │    │                 │    │              │ │
│  │ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌──────────┐ │ │
│  │ │ Dashboard   │ │    │ │ RDS Events  │ │    │ │ Critical │ │ │
│  │ │ Monitoring  │ │    │ │    Rule     │ │    │ │  Alerts  │ │ │
│  │ └─────────────┘ │    │ └─────────────┘ │    │ └──────────┘ │ │
│  │                 │    │                 │    │              │ │
│  │ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌──────────┐ │ │
│  │ │   Alarms    │ │    │ │ Lambda      │ │    │ │ Warning  │ │ │
│  │ │ & Metrics   │ │    │ │ Targets     │ │    │ │  Alerts  │ │ │
│  │ └─────────────┘ │    │ └─────────────┘ │    │ └──────────┘ │ │
│  └─────────────────┘    └─────────────────┘    └──────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

## CDK Stack 架構

### DataCatalogStack 組件
```typescript
// infrastructure/src/stacks/data-catalog-stack.ts
export class DataCatalogStack extends Stack {
  // 核心組件
  public readonly glueDatabase: glue.CfnDatabase;
  public readonly glueCrawler: glue.CfnCrawler;
  public readonly monitoringDashboard: cloudwatch.Dashboard;
  public readonly alertTopic: sns.Topic;
  
  // 網路組件
  private readonly auroraConnection: glue.CfnConnection;
  private readonly crawlerRole: iam.Role;
  
  // 監控組件
  private readonly triggerFunction: lambda.Function;
  private readonly rdsEventRule: events.Rule;
}
```

### 資源依賴關係
```
VpcStack ──┐
           ├─► DataCatalogStack
DatabaseStack ──┘
           │
           ├─► glueDatabase
           ├─► auroraConnection
           ├─► glueCrawler
           ├─► triggerFunction
           ├─► rdsEventRule
           ├─► monitoringDashboard
           └─► alertTopic
```

## 網路配置

### VPC 連接架構
```yaml
vpc_configuration:
  vpc_id: "vpc-xxxxxxxxx"  # 從 VpcStack 繼承
  
  subnets:
    private_subnets:
      - subnet_id: "subnet-xxxxxxxxx"  # AZ-1a
        availability_zone: "ap-northeast-1a"
        cidr_block: "10.0.1.0/24"
      - subnet_id: "subnet-yyyyyyyyy"  # AZ-1c
        availability_zone: "ap-northeast-1c"
        cidr_block: "10.0.2.0/24"
    
    database_subnets:
      - subnet_id: "subnet-zzzzzzzzz"  # AZ-1a
        availability_zone: "ap-northeast-1a"
        cidr_block: "10.0.11.0/24"
      - subnet_id: "subnet-aaaaaaaaa"  # AZ-1c
        availability_zone: "ap-northeast-1c"
        cidr_block: "10.0.12.0/24"
  
  nat_gateways:
    - nat_gateway_id: "nat-xxxxxxxxx"
      subnet_id: "subnet-public-1a"
      elastic_ip: "eip-xxxxxxxxx"
```

### 安全群組配置
```yaml
security_groups:
  aurora_security_group:
    group_id: "sg-aurora-xxxxxxxxx"
    description: "Aurora PostgreSQL cluster security group"
    ingress_rules:
      - protocol: "tcp"
        port: 5432
        source: "10.0.0.0/16"  # VPC CIDR
        description: "PostgreSQL access from VPC"
      - protocol: "tcp"
        port: 5432
        source_security_group: "sg-glue-xxxxxxxxx"
        description: "PostgreSQL access from Glue Crawler"
    
    egress_rules:
      - protocol: "all"
        destination: "0.0.0.0/0"
        description: "All outbound traffic"
  
  glue_connection_security_group:
    group_id: "sg-glue-xxxxxxxxx"
    description: "Glue Crawler connection security group"
    ingress_rules: []  # No inbound rules needed
    
    egress_rules:
      - protocol: "tcp"
        port: 5432
        destination_security_group: "sg-aurora-xxxxxxxxx"
        description: "PostgreSQL access to Aurora"
      - protocol: "tcp"
        port: 443
        destination: "0.0.0.0/0"
        description: "HTTPS for AWS API calls"
      - protocol: "tcp"
        port: 80
        destination: "0.0.0.0/0"
        description: "HTTP for package downloads"
```

## Glue 服務配置

### Glue Database 配置
```yaml
glue_database:
  name: "genai_demo_catalog"
  description: "Auto-discovered schema catalog for GenAI Demo application across 13 bounded contexts"
  
  parameters:
    classification: "postgresql"
    typeOfData: "relational"
    created_by: "aws-glue-crawler"
    auto_discovery: "true"
    bounded_contexts: "13"
    source_database: "genai_demo"
    discovery_frequency: "daily"
    real_time_triggers: "enabled"
  
  location_uri: null  # Catalog database, no physical location
  
  tags:
    Project: "GenAI-Demo"
    Component: "DataCatalog"
    Environment: "${environment}"
    ManagedBy: "CDK"
```

### Glue Crawler 配置
```yaml
glue_crawler:
  name: "genai-demo-aurora-auto-discovery"
  description: "Automatically discovers and catalogs all tables in GenAI Demo Aurora database across 13 bounded contexts"
  
  role: "arn:aws:iam::ACCOUNT:role/DataCatalogStack-GlueCrawlerRole"
  database_name: "genai_demo_catalog"
  
  targets:
    jdbc_targets:
      - connection_name: "genai-demo-aurora-connection"
        path: "genai_demo/%"  # Scan all tables in genai_demo database
        exclusions:
          # System tables
          - "genai_demo/flyway_schema_history"
          - "genai_demo/information_schema/%"
          - "genai_demo/pg_catalog/%"
          - "genai_demo/pg_stat_%"
          - "genai_demo/pg_settings"
          # Temporary tables
          - "genai_demo/tmp_%"
          - "genai_demo/temp_%"
  
  schedule:
    schedule_expression: "cron(0 2 * * ? *)"  # Daily at 2 AM JST
  
  schema_change_policy:
    update_behavior: "UPDATE_IN_DATABASE"  # Update existing schemas
    delete_behavior: "LOG"  # Log deleted tables but keep in catalog
  
  recrawl_policy:
    recrawl_behavior: "CRAWL_EVERYTHING"  # Full scan for accuracy
  
  configuration:
    Version: 1.0
    CrawlerOutput:
      Partitions:
        AddOrUpdateBehavior: "InheritFromTable"
      Tables:
        AddOrUpdateBehavior: "MergeNewColumns"
    Grouping:
      TableGroupingPolicy: "CombineCompatibleSchemas"
    PostgreSQL:
      SampleSize: 1000
      MaxConcurrentConnections: 2
      ConnectionTimeout: 30
```

### JDBC 連接配置
```yaml
jdbc_connection:
  name: "genai-demo-aurora-connection"
  description: "JDBC connection to GenAI Demo Aurora PostgreSQL Global Database"
  
  connection_type: "JDBC"
  
  connection_properties:
    JDBC_CONNECTION_URL: "jdbc:postgresql://aurora-cluster-endpoint:5432/genai_demo"
    USERNAME: "${aurora_username}"  # From Secrets Manager
    PASSWORD: "${aurora_password}"  # From Secrets Manager
  
  physical_connection_requirements:
    availability_zone: "ap-northeast-1a"
    security_group_id_list:
      - "sg-glue-xxxxxxxxx"
    subnet_id: "subnet-xxxxxxxxx"  # Private subnet
  
  connection_properties_encryption:
    enabled: true
    kms_key_id: "arn:aws:kms:ap-northeast-1:ACCOUNT:key/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
```

## IAM 權限配置

### Glue Crawler Role
```yaml
glue_crawler_role:
  role_name: "DataCatalogStack-GlueCrawlerRole"
  description: "IAM role for GenAI Demo Glue Crawler with Aurora access"
  
  assume_role_policy:
    Version: "2012-10-17"
    Statement:
      - Effect: "Allow"
        Principal:
          Service: "glue.amazonaws.com"
        Action: "sts:AssumeRole"
  
  managed_policies:
    - "arn:aws:iam::aws:policy/service-role/AWSGlueServiceRole"
  
  inline_policies:
    AuroraDataCatalogAccess:
      Version: "2012-10-17"
      Statement:
        # Aurora cluster access
        - Effect: "Allow"
          Action:
            - "rds:DescribeDBClusters"
            - "rds:DescribeDBInstances"
            - "rds:DescribeDBSubnetGroups"
            - "rds:ListTagsForResource"
          Resource: "*"
        
        # VPC and networking access
        - Effect: "Allow"
          Action:
            - "ec2:CreateNetworkInterface"
            - "ec2:DeleteNetworkInterface"
            - "ec2:DescribeNetworkInterfaces"
            - "ec2:DescribeVpcs"
            - "ec2:DescribeSubnets"
            - "ec2:DescribeSecurityGroups"
          Resource: "*"
        
        # CloudWatch logging
        - Effect: "Allow"
          Action:
            - "logs:CreateLogGroup"
            - "logs:CreateLogStream"
            - "logs:PutLogEvents"
          Resource: "arn:aws:logs:ap-northeast-1:ACCOUNT:log-group:/aws-glue/*"
        
        # Secrets Manager access
        - Effect: "Allow"
          Action:
            - "secretsmanager:GetSecretValue"
            - "secretsmanager:DescribeSecret"
          Resource: "arn:aws:secretsmanager:ap-northeast-1:ACCOUNT:secret:aurora-credentials-*"
```

### Lambda Execution Role
```yaml
lambda_execution_role:
  role_name: "DataCatalogStack-LambdaExecutionRole"
  description: "IAM role for Lambda function to trigger Glue Crawler"
  
  assume_role_policy:
    Version: "2012-10-17"
    Statement:
      - Effect: "Allow"
        Principal:
          Service: "lambda.amazonaws.com"
        Action: "sts:AssumeRole"
  
  managed_policies:
    - "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
  
  inline_policies:
    GlueCrawlerAccess:
      Version: "2012-10-17"
      Statement:
        - Effect: "Allow"
          Action:
            - "glue:GetCrawler"
            - "glue:StartCrawler"
            - "glue:GetCrawlerMetrics"
          Resource: "arn:aws:glue:ap-northeast-1:ACCOUNT:crawler/genai-demo-aurora-auto-discovery"
```

## Lambda 函數配置

### 即時觸發函數
```yaml
trigger_crawler_function:
  function_name: "DataCatalogStack-TriggerCrawlerFunction"
  description: "Triggers Glue Crawler when Aurora schema changes are detected"
  
  runtime: "python3.9"
  handler: "trigger_crawler.handler"
  timeout: 120  # 2 minutes
  memory_size: 128  # MB
  
  environment_variables:
    CRAWLER_NAME: "genai-demo-aurora-auto-discovery"
    LOG_LEVEL: "INFO"
  
  vpc_config:
    subnet_ids: []  # No VPC access needed
    security_group_ids: []
  
  dead_letter_config:
    target_arn: "arn:aws:sqs:ap-northeast-1:ACCOUNT:data-catalog-dlq"
  
  reserved_concurrency: 5  # Limit concurrent executions
  
  tags:
    Project: "GenAI-Demo"
    Component: "DataCatalog"
    Function: "CrawlerTrigger"
```

### Lambda 函數程式碼
```python
# trigger_crawler.py
import boto3
import json
import logging
from datetime import datetime

logger = logging.getLogger()
logger.setLevel(logging.INFO)

def handler(event, context):
    """
    Triggers Glue Crawler when Aurora schema changes are detected
    """
    glue = boto3.client('glue')
    crawler_name = 'genai-demo-aurora-auto-discovery'
    
    try:
        # Log the incoming event
        logger.info(f"Received RDS event: {json.dumps(event)}")
        
        # Check if this is a relevant schema change event
        if not is_schema_change_event(event):
            logger.info("Event is not a schema change, skipping crawler trigger")
            return {
                'statusCode': 200,
                'body': json.dumps('Event ignored - not a schema change')
            }
        
        # Check crawler status
        response = glue.get_crawler(Name=crawler_name)
        state = response['Crawler']['State']
        
        if state == 'READY':
            # Start the crawler
            glue.start_crawler(Name=crawler_name)
            logger.info(f"Started crawler {crawler_name} due to schema change")
            
            return {
                'statusCode': 200,
                'body': json.dumps({
                    'message': f'Crawler {crawler_name} started successfully',
                    'trigger_time': datetime.utcnow().isoformat(),
                    'event_source': event.get('source', 'unknown')
                })
            }
        else:
            logger.warning(f"Crawler {crawler_name} is in state {state}, cannot start")
            return {
                'statusCode': 200,
                'body': json.dumps({
                    'message': f'Crawler {crawler_name} is busy, state: {state}',
                    'current_state': state
                })
            }
            
    except Exception as e:
        logger.error(f"Error triggering crawler: {str(e)}")
        return {
            'statusCode': 500,
            'body': json.dumps({
                'error': str(e),
                'crawler_name': crawler_name
            })
        }

def is_schema_change_event(event):
    """
    Determines if the RDS event indicates a schema change
    """
    detail = event.get('detail', {})
    event_categories = detail.get('EventCategories', [])
    
    # Schema change indicators
    schema_change_categories = [
        'configuration change',
        'creation',
        'deletion',
        'maintenance',
        'backup'
    ]
    
    return any(category in schema_change_categories for category in event_categories)
```

## EventBridge 配置

### RDS 事件規則
```yaml
rds_event_rule:
  rule_name: "DataCatalogStack-RDSSchemaChangeRule"
  description: "Trigger Glue Crawler when Aurora schema changes detected"
  
  event_pattern:
    source: ["aws.rds"]
    detail-type: 
      - "RDS DB Instance Event"
      - "RDS DB Cluster Event"
    detail:
      EventCategories:
        - "configuration change"
        - "creation"
        - "deletion"
        - "maintenance"
      SourceId: ["${aurora_cluster_identifier}"]
  
  state: "ENABLED"
  
  targets:
    - id: "TriggerCrawlerLambda"
      arn: "arn:aws:lambda:ap-northeast-1:ACCOUNT:function:DataCatalogStack-TriggerCrawlerFunction"
      input_transformer:
        input_paths_map:
          source: "$.source"
          detail-type: "$.detail-type"
          detail: "$.detail"
        input_template: |
          {
            "source": "<source>",
            "detail-type": "<detail-type>",
            "detail": <detail>
          }
```

## 監控基礎設施

### CloudWatch Dashboard
```yaml
cloudwatch_dashboard:
  dashboard_name: "GenAI-Demo-Data-Catalog-Monitoring"
  dashboard_body:
    widgets:
      - type: "metric"
        properties:
          metrics:
            - ["AWS/Glue", "glue.driver.aggregate.numCompletedTasks", "JobName", "genai-demo-aurora-auto-discovery"]
            - [".", "glue.driver.aggregate.numFailedTasks", ".", "."]
            - [".", "glue.driver.jvm.heap.usage", ".", "."]
          period: 300
          stat: "Sum"
          region: "ap-northeast-1"
          title: "Glue Crawler Execution Status"
      
      - type: "metric"
        properties:
          metrics:
            - ["AWS/Lambda", "Duration", "FunctionName", "DataCatalogStack-TriggerCrawlerFunction"]
            - [".", "Errors", ".", "."]
            - [".", "Invocations", ".", "."]
          period: 300
          stat: "Average"
          region: "ap-northeast-1"
          title: "Lambda Function Performance"
      
      - type: "log"
        properties:
          query: |
            SOURCE '/aws-glue/crawlers'
            | fields @timestamp, @message
            | filter @message like /genai-demo-aurora-auto-discovery/
            | sort @timestamp desc
            | limit 20
          region: "ap-northeast-1"
          title: "Recent Crawler Logs"
```

### CloudWatch Alarms
```yaml
cloudwatch_alarms:
  crawler_failure_alarm:
    alarm_name: "DataCatalog-CrawlerExecutionFailure"
    alarm_description: "Glue Crawler failed to complete successfully"
    
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
    
    alarm_actions:
      - "arn:aws:sns:ap-northeast-1:ACCOUNT:DataCatalogCriticalAlerts"
  
  crawler_not_running_alarm:
    alarm_name: "DataCatalog-CrawlerNotRunning"
    alarm_description: "Glue Crawler has not run successfully in the last 25 hours"
    
    metric_name: "glue.driver.aggregate.numCompletedTasks"
    namespace: "AWS/Glue"
    dimensions:
      JobName: "genai-demo-aurora-auto-discovery"
    
    statistic: "Sum"
    period: 90000  # 25 hours
    evaluation_periods: 1
    threshold: 1
    comparison_operator: "LessThanThreshold"
    treat_missing_data: "breaching"
    
    alarm_actions:
      - "arn:aws:sns:ap-northeast-1:ACCOUNT:DataCatalogWarningAlerts"
```

## 成本優化配置

### 資源標籤策略
```yaml
resource_tagging:
  mandatory_tags:
    Project: "GenAI-Demo"
    Component: "DataCatalog"
    Environment: "${environment}"
    CostCenter: "DataGovernance"
    Owner: "DataArchitectureTeam"
    ManagedBy: "CDK"
    AutoShutdown: "false"
  
  cost_allocation_tags:
    BillingGroup: "Infrastructure"
    Service: "DataGovernance"
    Team: "Platform"
    Application: "GenAI-Demo"
```

### 成本控制措施
```yaml
cost_optimization:
  glue_crawler:
    schedule_optimization: "Daily at 2 AM (off-peak)"
    sample_size_limit: 1000
    connection_limit: 2
    timeout_setting: 30
  
  lambda_function:
    memory_optimization: 128  # MB (minimum for cost)
    timeout_optimization: 120  # seconds
    reserved_concurrency: 5  # Prevent runaway costs
  
  cloudwatch:
    log_retention: 7  # days
    metric_resolution: "standard"  # Not high-resolution
    dashboard_refresh: 300  # 5 minutes
  
  estimated_monthly_cost:
    glue_crawler: "$0.44"
    lambda_function: "$0.20"
    cloudwatch: "$3.00"
    total: "$3.64"
```

## 災難恢復配置

### 備份策略
```yaml
backup_configuration:
  glue_catalog_backup:
    method: "CloudFormation template export"
    frequency: "daily"
    retention: "30 days"
    storage_location: "s3://genai-demo-backup/glue-catalog/"
  
  configuration_backup:
    method: "CDK source code"
    frequency: "on change"
    retention: "indefinite"
    storage_location: "git repository"
  
  cross_region_replication:
    enabled: false  # Single region deployment
    target_region: "ap-southeast-1"  # Future consideration
```

### 恢復程序
```yaml
disaster_recovery:
  rto_target: "4 hours"  # Recovery Time Objective
  rpo_target: "24 hours"  # Recovery Point Objective
  
  recovery_steps:
    1: "Restore VPC and networking infrastructure"
    2: "Restore Aurora database cluster"
    3: "Deploy DataCatalogStack via CDK"
    4: "Restore Glue Catalog metadata from backup"
    5: "Verify crawler functionality"
    6: "Test end-to-end data discovery"
  
  testing_schedule: "quarterly"
  documentation: "infrastructure/docs/DISASTER_RECOVERY.md"
```

## 安全配置

### 網路安全
```yaml
network_security:
  vpc_isolation:
    - "Glue Crawler runs in private subnets only"
    - "No direct internet access for Glue resources"
    - "NAT Gateway for outbound AWS API calls"
  
  security_groups:
    - "Minimal ingress rules (none for Glue)"
    - "Specific egress rules for required services"
    - "No SSH or RDP access"
  
  network_acls:
    - "Default VPC NACLs (allow all)"
    - "Additional restrictions via security groups"
```

### 資料安全
```yaml
data_security:
  encryption:
    at_rest: "AWS managed keys (SSE-S3)"
    in_transit: "TLS 1.2+ for all connections"
    catalog_metadata: "Encrypted by default"
  
  access_control:
    authentication: "AWS IAM"
    authorization: "Resource-based policies"
    audit_trail: "CloudTrail logging"
  
  secrets_management:
    database_credentials: "AWS Secrets Manager"
    rotation_schedule: "90 days"
    encryption_key: "Customer managed KMS key"
```

---

**文檔版本**: 1.0  
**建立日期**: 2025年9月24日 下午3:45 (台北時間)  
**負責團隊**: 基礎設施團隊  
**審核者**: 雲端架構師  
**下次審核**: 2025年12月24日