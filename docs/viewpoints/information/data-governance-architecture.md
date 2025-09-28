# Data Governance Architecture - AWS Glue Data Catalog Integration

## Overview

This document describes the data governance architecture for the GenAI Demo application, focusing on how AWS Glue Data Catalog provides automated schema discovery and unified data catalog management.

## Data Governance Objectives

### Primary Objectives
1. **Automated Schema Discovery**: Real-time detection and recording of database schema changes
2. **Unified Data Catalog**: Unified data view across 13 bounded contexts
3. **Data Lineage Tracking**: Track data flow and transformation processes
4. **Compliance Management**: Ensure data usage complies with GDPR and other regulatory requirements
5. **Data Quality Monitoring**: Continuous monitoring of data integrity and consistency

### Business Value
- **Reduced Maintenance Costs**: Automation reduces 80% of manual data catalog maintenance work
- **Enhanced Data Discovery Efficiency**: Development teams can quickly find required data
- **Improved Compliance**: Automated compliance checks and reporting
- **AI/ML Support**: Provides structured data catalog for GenBI and RAG systems

## Architecture Components

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
│  └── ... (other 10 bounded contexts)                           │
└─────────────────────────────────────────────────────────────────┘
```

### Automated Discovery Process
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

## 13 Bounded Context Data Mapping

### Core Business Domains
1. **Customer Management**
   - Primary Entities: Customer, CustomerProfile, CustomerPreferences
   - Data Characteristics: Personally Identifiable Information (PII), GDPR sensitive data
   - Governance Requirements: Data anonymization, access control, retention policies

2. **Order Processing**
   - Primary Entities: Order, OrderItem, OrderStatusHistory
   - Data Characteristics: Transaction data, financial records
   - Governance Requirements: Audit trails, integrity checks, backup strategies

3. **Product Catalog**
   - Primary Entities: Product, Category, ProductVariant
   - Data Characteristics: Master data, reference data
   - Governance Requirements: Version control, change tracking, consistency maintenance

4. **Inventory Management**
   - Primary Entities: Inventory, StockMovement, WarehouseLocation
   - Data Characteristics: Real-time data, high-frequency updates
   - Governance Requirements: Real-time synchronization, conflict resolution, accuracy monitoring

### Supporting Service Domains
5. **Payment Processing**
   - Primary Entities: Payment, PaymentMethod, Transaction
   - Data Characteristics: Financial data, PCI DSS compliance
   - Governance Requirements: Encrypted storage, access logging, compliance auditing

6. **Shipping & Logistics**
   - Primary Entities: Shipment, DeliveryAddress, TrackingEvent
   - Data Characteristics: Geographic data, time-series data
   - Governance Requirements: Location privacy, data retention, third-party integration

7. **Notification System**
   - Primary Entities: Notification, NotificationTemplate, DeliveryLog
   - Data Characteristics: Communication data, preference settings
   - Governance Requirements: Consent management, frequency control, unsubscribe mechanisms

8. **User Management**
   - Primary Entities: User, Role, Permission, UserSession
   - Data Characteristics: Identity data, access control
   - Governance Requirements: Authentication, authorization management, session security

### Analytics and Reporting Domains
9. **Analytics & Reporting**
   - Primary Entities: AnalyticsEvent, ReportDefinition, DashboardConfig
   - Data Characteristics: Aggregated data, historical trends
   - Governance Requirements: Data lineage, calculation logic, accuracy verification

10. **Audit & Compliance**
    - Primary Entities: AuditLog, ComplianceReport, DataLineage
    - Data Characteristics: Audit trails, compliance evidence
    - Governance Requirements: Immutability, long-term preservation, regulatory compliance

11. **Marketing & Campaigns**
    - Primary Entities: Campaign, CustomerSegment, MarketingEvent
    - Data Characteristics: Behavioral data, segmentation information
    - Governance Requirements: Consent management, personalization limits, effectiveness tracking

### Technical Foundation Domains
12. **Configuration Management**
    - Primary Entities: ApplicationConfig, FeatureFlag, EnvironmentSetting
    - Data Characteristics: Configuration data, system parameters
    - Governance Requirements: Version control, change approval, rollback mechanisms

13. **Integration & API**
    - Primary Entities: ApiKey, IntegrationLog, WebhookConfig
    - Data Characteristics: Integration data, API usage records
    - Governance Requirements: Access control, usage monitoring, rate limiting

## Data Governance Policies

### Automated Classification Tags
```yaml
# Glue Crawler automatic tagging configuration
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

### Access Control Matrix
| Role | Customer Data | Order Data | Product Data | Audit Data |
|------|---------------|------------|--------------|------------|
| **Customer Service** | Read/Write | Read | Read | None |
| **Order Manager** | Read | Read/Write | Read | None |
| **Product Manager** | None | Read | Read/Write | None |
| **Data Analyst** | Anonymized | Read | Read | Read |
| **Compliance Officer** | Masked | Read | Read | Read/Write |
| **System Admin** | None | None | None | Read |

### Data Quality Rules
```sql
-- Automated data quality checks
-- 1. Completeness check
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

-- 2. Consistency check
SELECT 
  'customers' as source_table,
  'orders' as target_table,
  COUNT(DISTINCT o.customer_id) as referenced_customers,
  COUNT(DISTINCT c.id) as existing_customers,
  COUNT(DISTINCT o.customer_id) - COUNT(DISTINCT c.id) as orphaned_references
FROM orders o
LEFT JOIN customers c ON o.customer_id = c.id;

-- 3. Uniqueness check
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

## Monitoring and Alerting

### Data Governance Dashboard
```yaml
# CloudWatch Dashboard configuration
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

### Automated Alerts
```yaml
# CloudWatch Alarms configuration
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

## Integration Points

### GenBI Text-to-SQL Integration
```python
# GenBI uses Glue Catalog for SQL generation
import boto3

class GenBIDataCatalogIntegration:
    def __init__(self):
        self.glue_client = boto3.client('glue')
        self.catalog_database = 'genai_demo_catalog'
    
    def get_table_schema(self, table_name: str) -> dict:
        """Get table schema for SQL generation"""
        response = self.glue_client.get_table(
            DatabaseName=self.catalog_database,
            Name=table_name
        )
        return response['Table']['StorageDescriptor']['Columns']
    
    def get_available_tables(self) -> list:
        """Get list of all available tables"""
        response = self.glue_client.get_tables(
            DatabaseName=self.catalog_database
        )
        return [table['Name'] for table in response['TableList']]
    
    def generate_context_for_llm(self, user_query: str) -> str:
        """Generate context with schema information for LLM"""
        relevant_tables = self.identify_relevant_tables(user_query)
        context = "Available database schema:\n"
        
        for table_name in relevant_tables:
            schema = self.get_table_schema(table_name)
            context += f"\nTable: {table_name}\n"
            for column in schema:
                context += f"  - {column['Name']}: {column['Type']}\n"
        
        return context
```

### RAG System Integration
```python
# RAG system uses Glue Catalog for knowledge base construction
class RAGDataCatalogIntegration:
    def __init__(self):
        self.glue_client = boto3.client('glue')
        self.bedrock_client = boto3.client('bedrock-runtime')
    
    def build_schema_knowledge_base(self) -> str:
        """Build knowledge base containing database schema"""
        tables = self.glue_client.get_tables(
            DatabaseName='genai_demo_catalog'
        )
        
        knowledge_base = ""
        for table in tables['TableList']:
            table_name = table['Name']
            columns = table['StorageDescriptor']['Columns']
            
            # Build natural language description
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

## Compliance and Security

### GDPR Compliance
```yaml
# GDPR data processing records
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

### Data Encryption and Access Control
```yaml
# Data security configuration
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

## Cost Optimization

### Resource Usage Monitoring
```yaml
# Cost monitoring configuration
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

### Performance Optimization
```yaml
# Performance optimization strategies
performance_optimization:
  crawler_configuration:
    sample_size: 1000  # Limit sample size to improve speed
    max_concurrent_connections: 2  # Avoid database overload
    connection_timeout: 30  # Reasonable connection timeout
  
  caching_strategy:
    metadata_cache: "Redis with 1-hour TTL"
    query_result_cache: "ElastiCache with 15-minute TTL"
    schema_cache: "Application-level with 24-hour TTL"
  
  query_optimization:
    partition_pruning: "Automatic based on date columns"
    column_pruning: "Select only required columns"
    predicate_pushdown: "Filter at source level"
```

## Future Development Plan

### Short-term Goals (3 months)
- [ ] Complete automated discovery for all 13 bounded contexts
- [ ] Establish comprehensive data quality monitoring system
- [ ] Integrate GenBI and RAG systems
- [ ] Implement basic GDPR compliance checks

### Medium-term Goals (6 months)
- [ ] Implement advanced data lineage tracking
- [ ] Build automated data quality remediation mechanisms
- [ ] Integrate Amazon DataZone for data governance
- [ ] Implement machine learning-driven anomaly detection

### Long-term Goals (12 months)
- [ ] Build cross-region data governance system
- [ ] Implement real-time data quality monitoring
- [ ] Integrate third-party data sources
- [ ] Build predictive data governance models

---

**Document Version**: 1.0  
**Created Date**: September 28, 2025 11:12 PM (Taipei Time)  
**Responsible Team**: Data Architecture Team  
**Reviewer**: Chief Architect  
**Next Review**: December 28, 2025