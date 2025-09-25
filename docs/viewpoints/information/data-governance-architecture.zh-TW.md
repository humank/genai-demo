# 資料治理架構 - AWS Glue Data Catalog 整合

## 概述

本文檔描述 GenAI Demo 應用程式的資料治理架構，重點說明 AWS Glue Data Catalog 如何提供自動化的 schema 發現和統一的資料目錄管理。

## 資料治理目標

### 主要目標
1. **自動化 Schema 發現**: 即時檢測和記錄資料庫 schema 變更
2. **統一資料目錄**: 跨 13 個 bounded context 的統一資料視圖
3. **資料血緣追蹤**: 追蹤資料流動和轉換過程
4. **合規性管理**: 確保資料使用符合 GDPR 和其他法規要求
5. **資料品質監控**: 持續監控資料完整性和一致性

### 業務價值
- **降低維護成本**: 自動化減少 80% 的手動資料目錄維護工作
- **提升資料發現效率**: 開發團隊可快速找到所需資料
- **增強合規性**: 自動化合規檢查和報告
- **支援 AI/ML**: 為 GenBI 和 RAG 系統提供結構化資料目錄

## 架構組件

### AWS Glue Data Catalog
```
┌─────────────────────────────────────────────────────────────────┐
│                    AWS Glue Data Catalog                        │
├─────────────────────────────────────────────────────────────────┤
│  Database: genai_demo_catalog                                   │
│  ├── Customer Management Tables                                 │
│  │   ├── customers (id, name, email, membership_level)         │
│  │   ├── customer_profiles (customer_id, preferences)          │
│  │   └── customer_preferences (id, settings)                   │
│  ├── Order Processing Tables                                    │
│  │   ├── orders (id, customer_id, status, total_amount)        │
│  │   ├── order_items (order_id, product_id, quantity)          │
│  │   └── order_status_history (order_id, status, timestamp)    │
│  ├── Product Catalog Tables                                     │
│  │   ├── products (id, name, price, category_id)               │
│  │   ├── categories (id, name, parent_id)                      │
│  │   └── product_variants (product_id, variant_name, price)    │
│  └── ... (其他 10 個 bounded contexts)                         │
└─────────────────────────────────────────────────────────────────┘
```

### 自動化發現流程
```
Aurora PostgreSQL ──┐
                   │
┌──────────────────▼──────────────────┐
│        AWS Glue Crawler             │
│  ┌─────────────────────────────────┐ │
│  │     Daily Schedule (2 AM)       │ │
│  │  ┌─────────────────────────────┐│ │
│  │  │   Full Database Scan       ││ │
│  │  │   - All 13 Bounded Contexts││ │
│  │  │   - Schema Change Detection ││ │
│  │  │   - New Table Discovery     ││ │
│  │  └─────────────────────────────┘│ │
│  └─────────────────────────────────┘ │
│                                     │
│  ┌─────────────────────────────────┐ │
│  │    Real-time Triggers           │ │
│  │  ┌─────────────────────────────┐│ │
│  │  │   RDS Event Detection       ││ │
│  │  │   - Schema Changes          ││ │
│  │  │   - Table Creation/Deletion ││ │
│  │  │   - Index Modifications     ││ │
│  │  └─────────────────────────────┘│ │
│  └─────────────────────────────────┘ │
└─────────────────┬───────────────────┘
                 │
                 ▼
┌─────────────────────────────────────┐
│      Glue Data Catalog              │
│  ┌─────────────────────────────────┐ │
│  │    Metadata Storage             │ │
│  │  - Table Schemas                │ │
│  │  - Column Definitions           │ │
│  │  - Data Types                   │ │
│  │  - Relationships                │ │
│  │  - Business Context Tags        │ │
│  └─────────────────────────────────┘ │
└─────────────────┬───────────────────┘
                 │
                 ▼
┌─────────────────────────────────────┐
│     Consumer Services               │
│  ┌─────────────────────────────────┐ │
│  │  GenBI Text-to-SQL Engine       │ │
│  │  RAG Conversation System        │ │
│  │  Amazon Athena Queries          │ │
│  │  Amazon QuickSight Dashboards   │ │
│  │  Data Lineage Tracking          │ │
│  └─────────────────────────────────┘ │
└─────────────────────────────────────┘
```

## 13 個 Bounded Context 資料對映

### 核心業務領域
1. **Customer Management** (客戶管理)
   - 主要實體: Customer, CustomerProfile, CustomerPreferences
   - 資料特性: 個人識別資訊 (PII), GDPR 敏感資料
   - 治理要求: 資料匿名化, 存取控制, 保留政策

2. **Order Processing** (訂單處理)
   - 主要實體: Order, OrderItem, OrderStatusHistory
   - 資料特性: 交易資料, 財務記錄
   - 治理要求: 稽核追蹤, 完整性檢查, 備份策略

3. **Product Catalog** (產品目錄)
   - 主要實體: Product, Category, ProductVariant
   - 資料特性: 主資料, 參考資料
   - 治理要求: 版本控制, 變更追蹤, 一致性維護

4. **Inventory Management** (庫存管理)
   - 主要實體: Inventory, StockMovement, WarehouseLocation
   - 資料特性: 即時資料, 高頻更新
   - 治理要求: 即時同步, 衝突解決, 準確性監控

### 支援服務領域
5. **Payment Processing** (支付處理)
   - 主要實體: Payment, PaymentMethod, Transaction
   - 資料特性: 金融資料, PCI DSS 合規
   - 治理要求: 加密儲存, 存取日誌, 合規稽核

6. **Shipping & Logistics** (物流配送)
   - 主要實體: Shipment, DeliveryAddress, TrackingEvent
   - 資料特性: 地理資料, 時序資料
   - 治理要求: 位置隱私, 資料保留, 第三方整合

7. **Notification System** (通知系統)
   - 主要實體: Notification, NotificationTemplate, DeliveryLog
   - 資料特性: 通訊資料, 偏好設定
   - 治理要求: 同意管理, 頻率控制, 退訂機制

8. **User Management** (使用者管理)
   - 主要實體: User, Role, Permission, UserSession
   - 資料特性: 身份資料, 存取控制
   - 治理要求: 身份驗證, 授權管理, 會話安全

### 分析和報告領域
9. **Analytics & Reporting** (分析報告)
   - 主要實體: AnalyticsEvent, ReportDefinition, DashboardConfig
   - 資料特性: 聚合資料, 歷史趨勢
   - 治理要求: 資料血緣, 計算邏輯, 準確性驗證

10. **Audit & Compliance** (稽核合規)
    - 主要實體: AuditLog, ComplianceReport, DataLineage
    - 資料特性: 稽核軌跡, 合規證據
    - 治理要求: 不可變性, 長期保存, 法規遵循

11. **Marketing & Campaigns** (行銷活動)
    - 主要實體: Campaign, CustomerSegment, MarketingEvent
    - 資料特性: 行為資料, 分群資訊
    - 治理要求: 同意管理, 個人化限制, 效果追蹤

### 技術基礎領域
12. **Configuration Management** (配置管理)
    - 主要實體: ApplicationConfig, FeatureFlag, EnvironmentSetting
    - 資料特性: 配置資料, 系統參數
    - 治理要求: 版本控制, 變更審批, 回滾機制

13. **Integration & API** (整合介面)
    - 主要實體: ApiKey, IntegrationLog, WebhookConfig
    - 資料特性: 整合資料, API 使用記錄
    - 治理要求: 存取控制, 使用監控, 限流管理

## 資料治理政策

### 自動化分類標籤
```yaml
# Glue Crawler 自動標籤配置
classification_rules:
  - pattern: "customers|users|profiles"
    tags:
      - "DataClassification: PII"
      - "GDPRScope: Yes"
      - "RetentionPeriod: 7years"
      - "AccessLevel: Restricted"
  
  - pattern: "orders|payments|transactions"
    tags:
      - "DataClassification: Financial"
      - "ComplianceScope: PCI-DSS"
      - "RetentionPeriod: 10years"
      - "AccessLevel: Confidential"
  
  - pattern: "products|categories|inventory"
    tags:
      - "DataClassification: Business"
      - "ComplianceScope: None"
      - "RetentionPeriod: 5years"
      - "AccessLevel: Internal"
  
  - pattern: "audit_logs|compliance_reports"
    tags:
      - "DataClassification: Audit"
      - "ComplianceScope: SOX"
      - "RetentionPeriod: Permanent"
      - "AccessLevel: Restricted"
```

### 存取控制矩陣
| 角色 | Customer Data | Order Data | Product Data | Audit Data |
|------|---------------|------------|--------------|------------|
| **Customer Service** | Read/Write | Read | Read | None |
| **Order Manager** | Read | Read/Write | Read | None |
| **Product Manager** | None | Read | Read/Write | None |
| **Data Analyst** | Anonymized | Read | Read | Read |
| **Compliance Officer** | Masked | Read | Read | Read/Write |
| **System Admin** | None | None | None | Read |

### 資料品質規則
```sql
-- 自動化資料品質檢查
-- 1. 完整性檢查
SELECT 
  table_name,
  column_name,
  COUNT(*) as total_rows,
  COUNT(column_name) as non_null_rows,
  (COUNT(column_name) * 100.0 / COUNT(*)) as completeness_percentage
FROM information_schema.columns c
JOIN (SELECT table_name, COUNT(*) as row_count FROM all_tables) t
  ON c.table_name = t.table_name
WHERE c.is_nullable = 'NO'
GROUP BY table_name, column_name
HAVING completeness_percentage < 95;

-- 2. 一致性檢查
SELECT 
  'customers' as source_table,
  'orders' as target_table,
  COUNT(DISTINCT o.customer_id) as referenced_customers,
  COUNT(DISTINCT c.id) as existing_customers,
  COUNT(DISTINCT o.customer_id) - COUNT(DISTINCT c.id) as orphaned_references
FROM orders o
LEFT JOIN customers c ON o.customer_id = c.id;

-- 3. 唯一性檢查
SELECT 
  table_name,
  column_name,
  COUNT(*) as total_values,
  COUNT(DISTINCT column_name) as unique_values,
  CASE 
    WHEN COUNT(*) = COUNT(DISTINCT column_name) THEN 'PASS'
    ELSE 'FAIL'
  END as uniqueness_check
FROM information_schema.columns
WHERE column_name IN ('id', 'email', 'order_number')
GROUP BY table_name, column_name;
```

## 監控和告警

### 資料治理儀表板
```yaml
# CloudWatch Dashboard 配置
dashboard_widgets:
  - name: "Schema Discovery Status"
    metrics:
      - "AWS/Glue.Crawler.TablesCreated"
      - "AWS/Glue.Crawler.TablesUpdated"
      - "AWS/Glue.Crawler.TablesDeleted"
  
  - name: "Data Quality Metrics"
    metrics:
      - "Custom/DataGovernance.CompletenessScore"
      - "Custom/DataGovernance.ConsistencyScore"
      - "Custom/DataGovernance.UniquenessScore"
  
  - name: "Compliance Status"
    metrics:
      - "Custom/DataGovernance.GDPRCompliance"
      - "Custom/DataGovernance.PCIDSSCompliance"
      - "Custom/DataGovernance.SOXCompliance"
  
  - name: "Access Patterns"
    metrics:
      - "Custom/DataGovernance.UnauthorizedAccess"
      - "Custom/DataGovernance.DataExportEvents"
      - "Custom/DataGovernance.PIIAccessEvents"
```

### 自動化告警
```yaml
# CloudWatch Alarms 配置
alarms:
  - name: "SchemaChangeDetected"
    metric: "AWS/Glue.Crawler.TablesUpdated"
    threshold: 1
    comparison: "GreaterThanThreshold"
    action: "SNS:DataGovernanceTeam"
  
  - name: "DataQualityDegraded"
    metric: "Custom/DataGovernance.CompletenessScore"
    threshold: 95
    comparison: "LessThanThreshold"
    action: "SNS:DataQualityTeam"
  
  - name: "ComplianceViolation"
    metric: "Custom/DataGovernance.GDPRCompliance"
    threshold: 100
    comparison: "LessThanThreshold"
    action: "SNS:ComplianceTeam"
  
  - name: "UnauthorizedDataAccess"
    metric: "Custom/DataGovernance.UnauthorizedAccess"
    threshold: 0
    comparison: "GreaterThanThreshold"
    action: "SNS:SecurityTeam"
```

## 整合點

### GenBI Text-to-SQL 整合
```python
# GenBI 使用 Glue Catalog 進行 SQL 生成
import boto3

class GenBIDataCatalogIntegration:
    def __init__(self):
        self.glue_client = boto3.client('glue')
        self.catalog_database = 'genai_demo_catalog'
    
    def get_table_schema(self, table_name: str) -> dict:
        """獲取表格 schema 用於 SQL 生成"""
        response = self.glue_client.get_table(
            DatabaseName=self.catalog_database,
            Name=table_name
        )
        return response['Table']['StorageDescriptor']['Columns']
    
    def get_available_tables(self) -> list:
        """獲取所有可用表格列表"""
        response = self.glue_client.get_tables(
            DatabaseName=self.catalog_database
        )
        return [table['Name'] for table in response['TableList']]
    
    def generate_context_for_llm(self, user_query: str) -> str:
        """為 LLM 生成包含 schema 資訊的上下文"""
        relevant_tables = self.identify_relevant_tables(user_query)
        context = "Available database schema:\n"
        
        for table_name in relevant_tables:
            schema = self.get_table_schema(table_name)
            context += f"\nTable: {table_name}\n"
            for column in schema:
                context += f"  - {column['Name']}: {column['Type']}\n"
        
        return context
```

### RAG 系統整合
```python
# RAG 系統使用 Glue Catalog 進行知識庫建構
class RAGDataCatalogIntegration:
    def __init__(self):
        self.glue_client = boto3.client('glue')
        self.bedrock_client = boto3.client('bedrock-runtime')
    
    def build_schema_knowledge_base(self) -> str:
        """建構包含資料庫 schema 的知識庫"""
        tables = self.glue_client.get_tables(
            DatabaseName='genai_demo_catalog'
        )
        
        knowledge_base = ""
        for table in tables['TableList']:
            table_name = table['Name']
            columns = table['StorageDescriptor']['Columns']
            
            # 建構自然語言描述
            description = f"The {table_name} table contains information about "
            
            if 'customer' in table_name.lower():
                description += "customer data including personal information and preferences."
            elif 'order' in table_name.lower():
                description += "order transactions and purchase history."
            elif 'product' in table_name.lower():
                description += "product catalog and inventory information."
            
            description += f" It has {len(columns)} columns: "
            description += ", ".join([col['Name'] for col in columns])
            
            knowledge_base += description + "\n\n"
        
        return knowledge_base
```

## 合規性和安全

### GDPR 合規
```yaml
# GDPR 資料處理記錄
gdpr_compliance:
  data_categories:
    - category: "Personal Identifiers"
      tables: ["customers", "users", "customer_profiles"]
      retention_period: "7 years"
      lawful_basis: "Contract performance"
      
    - category: "Financial Data"
      tables: ["orders", "payments", "transactions"]
      retention_period: "10 years"
      lawful_basis: "Legal obligation"
      
    - category: "Marketing Data"
      tables: ["campaigns", "customer_segments"]
      retention_period: "2 years"
      lawful_basis: "Legitimate interest"
  
  data_subject_rights:
    - right: "Access"
      implementation: "Automated data export via Glue Catalog queries"
    - right: "Rectification"
      implementation: "Direct database updates with audit trail"
    - right: "Erasure"
      implementation: "Automated anonymization scripts"
    - right: "Portability"
      implementation: "Structured data export in JSON format"
```

### 資料加密和存取控制
```yaml
# 資料安全配置
security_controls:
  encryption:
    at_rest: "AWS KMS with customer-managed keys"
    in_transit: "TLS 1.2+ for all connections"
    application_level: "Field-level encryption for PII"
  
  access_control:
    authentication: "AWS IAM with MFA"
    authorization: "Resource-based policies"
    audit: "CloudTrail + CloudWatch Logs"
  
  data_masking:
    development: "Automated PII masking for non-prod environments"
    testing: "Synthetic data generation"
    analytics: "Differential privacy for aggregated queries"
```

## 成本優化

### 資源使用監控
```yaml
# 成本監控配置
cost_monitoring:
  glue_crawler:
    schedule: "Daily at 2 AM (off-peak hours)"
    estimated_cost: "$0.44/month"
    optimization: "Incremental crawling for large tables"
  
  data_catalog:
    storage: "Metadata only, minimal cost"
    api_calls: "Cached responses to reduce API costs"
    estimated_cost: "$0.10/month"
  
  monitoring:
    cloudwatch_logs: "7-day retention for cost control"
    cloudwatch_metrics: "Standard resolution"
    estimated_cost: "$3.00/month"
  
  total_estimated_cost: "$3.54/month"
```

### 效能優化
```yaml
# 效能優化策略
performance_optimization:
  crawler_configuration:
    sample_size: 1000  # 限制樣本大小以提升速度
    max_concurrent_connections: 2  # 避免資料庫負載過高
    connection_timeout: 30  # 合理的連線逾時設定
  
  caching_strategy:
    metadata_cache: "Redis with 1-hour TTL"
    query_result_cache: "ElastiCache with 15-minute TTL"
    schema_cache: "Application-level with 24-hour TTL"
  
  query_optimization:
    partition_pruning: "Automatic based on date columns"
    column_pruning: "Select only required columns"
    predicate_pushdown: "Filter at source level"
```

## 未來發展規劃

### 短期目標 (3個月)
- [ ] 完成所有 13 個 bounded context 的自動發現
- [ ] 建立完整的資料品質監控體系
- [ ] 整合 GenBI 和 RAG 系統
- [ ] 實施基本的 GDPR 合規檢查

### 中期目標 (6個月)
- [ ] 實施進階資料血緣追蹤
- [ ] 建立自動化資料品質修復機制
- [ ] 整合 Amazon DataZone 進行資料治理
- [ ] 實施機器學習驅動的異常檢測

### 長期目標 (12個月)
- [ ] 建立跨區域資料治理體系
- [ ] 實施即時資料品質監控
- [ ] 整合第三方資料來源
- [ ] 建立預測性資料治理模型

---

**文檔版本**: 1.0  
**建立日期**: 2025年9月24日 下午3:25 (台北時間)  
**負責團隊**: 資料架構團隊  
**審核者**: 首席架構師  
**下次審核**: 2025年12月24日