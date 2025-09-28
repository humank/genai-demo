# AWS Glue Data Catalog Stack 操作手冊

## 概述

AWS Glue Data Catalog Stack 為 GenAI Demo 應用程式提供自動化的 schema 發現和資料治理功能。該系統能夠自動發現 Aurora PostgreSQL 資料庫中的所有表格結構，並在 schema 變更時即時更新目錄。

## 架構組件

### 核心服務
- **AWS Glue Database**: `genai_demo_catalog` - 統一的資料目錄
- **AWS Glue Crawler**: `genai-demo-aurora-auto-discovery` - 自動化 schema 發現
- **AWS Lambda**: 即時觸發器，監聽 RDS 事件
- **Amazon EventBridge**: RDS 事件路由
- **Amazon CloudWatch**: 監控和告警
- **Amazon SNS**: 通知系統

### 自動化功能
1. **每日排程掃描**: 每天凌晨 2 點自動執行完整掃描
2. **即時變更檢測**: RDS 事件觸發即時 schema 更新
3. **智能排除**: 自動排除系統表格和臨時表格
4. **監控告警**: 失敗自動通知，執行狀態監控

## 部署指南

### 前置條件
確保以下 Stack 已部署：
- NetworkStack (VPC 和子網路)
- SecurityStack (安全群組)
- RdsStack (Aurora 叢集)
- AlertingStack (SNS 主題)

### 部署命令
```bash
# 開發環境部署
cd infrastructure
./scripts/deploy-data-catalog.sh development ap-northeast-1

# 生產環境部署
./scripts/deploy-data-catalog.sh production ap-northeast-1 true
```

### 驗證部署
```bash
# 檢查 Glue Crawler 狀態
aws glue get-crawler --name genai-demo-aurora-auto-discovery

# 檢查資料庫目錄
aws glue get-database --name genai_demo_catalog

# 檢查已發現的表格
aws glue get-tables --database-name genai_demo_catalog
```

## 操作指南

### 手動觸發 Crawler
```bash
# 啟動 Crawler
aws glue start-crawler --name genai-demo-aurora-auto-discovery

# 檢查執行狀態
aws glue get-crawler-metrics --crawler-name-list genai-demo-aurora-auto-discovery
```

### 查看發現的表格
```bash
# 列出所有表格
aws glue get-tables --database-name genai_demo_catalog --output table

# 查看特定表格 schema
aws glue get-table --database-name genai_demo_catalog --name customers
```

### 監控 Crawler 執行
```bash
# 查看 Crawler 執行歷史
aws logs filter-log-events \
  --log-group-name /aws-glue/crawlers \
  --start-time $(date -d '1 hour ago' +%s)000
```

## 13 個 Bounded Context 覆蓋

系統自動發現以下 bounded context 的所有表格：

### 核心業務 Context
1. **Customer Management** - 客戶管理
   - `customers`, `customer_profiles`, `customer_preferences`
2. **Order Processing** - 訂單處理
   - `orders`, `order_items`, `order_status_history`
3. **Product Catalog** - 產品目錄
   - `products`, `categories`, `product_variants`
4. **Inventory Management** - 庫存管理
   - `inventory`, `stock_movements`, `warehouse_locations`

### 支援服務 Context
5. **Payment Processing** - 支付處理
   - `payments`, `payment_methods`, `transactions`
6. **Shipping & Logistics** - 物流配送
   - `shipments`, `delivery_addresses`, `tracking_events`
7. **Notification System** - 通知系統
   - `notifications`, `notification_templates`, `delivery_logs`
8. **User Management** - 使用者管理
   - `users`, `roles`, `permissions`, `user_sessions`

### 分析和報告 Context
9. **Analytics & Reporting** - 分析報告
   - `analytics_events`, `report_definitions`, `dashboard_configs`
10. **Audit & Compliance** - 稽核合規
    - `audit_logs`, `compliance_reports`, `data_lineage`
11. **Marketing & Campaigns** - 行銷活動
    - `campaigns`, `customer_segments`, `marketing_events`

### 技術基礎 Context
12. **Configuration Management** - 配置管理
    - `application_configs`, `feature_flags`, `environment_settings`
13. **Integration & API** - 整合介面
    - `api_keys`, `integration_logs`, `webhook_configs`

## 監控和告警

### CloudWatch 儀表板
訪問 `GenAI-Demo-Data-Catalog-Monitoring` 儀表板查看：
- Crawler 執行狀態
- 發現的表格數量
- 執行時間趨勢
- 錯誤率統計

### 告警配置
系統配置了以下告警：
- **Crawler 失敗告警**: 執行失敗時立即通知
- **Crawler 未執行告警**: 25 小時內未執行時通知
- **資源使用告警**: 記憶體或連線數過高時通知

### SNS 通知
告警會發送到 `GenAI Demo Data Catalog Alerts` SNS 主題，可訂閱：
```bash
# 訂閱 Email 通知
aws sns subscribe \
  --topic-arn arn:aws:sns:ap-northeast-1:ACCOUNT:GenAI-Demo-Data-Catalog-Alerts \
  --protocol email \
  --notification-endpoint your-email@example.com
```

## 成本優化

### 預估成本 (每月)
- **AWS Glue Crawler**: ~$0.44 (每日執行 + 即時觸發)
- **AWS Lambda**: ~$0.20 (即時觸發函數)
- **CloudWatch**: ~$3.00 (日誌和指標)
- **總計**: ~$3.64/月

### 成本控制措施
1. **排程優化**: 僅在凌晨 2 點執行完整掃描
2. **智能觸發**: 只在真正的 schema 變更時觸發
3. **日誌保留**: 設定合理的日誌保留期限
4. **資源標籤**: 完整的成本追蹤標籤

## 故障排除

### 常見問題

#### Crawler 執行失敗
```bash
# 檢查 Crawler 日誌
aws logs filter-log-events \
  --log-group-name /aws-glue/crawlers \
  --filter-pattern "ERROR"

# 檢查 IAM 權限
aws iam simulate-principal-policy \
  --policy-source-arn arn:aws:iam::ACCOUNT:role/DataCatalogStack-GlueCrawlerRole \
  --action-names glue:GetConnection rds:DescribeDBClusters
```

#### 連線問題
```bash
# 測試 VPC 連線
aws glue get-connection --name genai-demo-aurora-connection

# 檢查安全群組規則
aws ec2 describe-security-groups \
  --group-ids sg-xxxxxxxxx \
  --query 'SecurityGroups[0].IpPermissions'
```

#### 表格未發現
```bash
# 檢查排除規則
aws glue get-crawler --name genai-demo-aurora-auto-discovery \
  --query 'Crawler.Targets.JdbcTargets[0].Exclusions'

# 手動測試連線
psql -h aurora-cluster-endpoint -U postgres -d genai_demo -c "\dt"
```

### 效能調優

#### Crawler 效能優化
```json
{
  "PostgreSQL": {
    "SampleSize": 1000,
    "MaxConcurrentConnections": 2,
    "ConnectionTimeout": 30
  }
}
```

#### 資料庫連線優化
- 使用連線池限制並發連線數
- 設定適當的連線逾時時間
- 監控 Aurora 連線使用率

## 安全考量

### 網路安全
- Crawler 在私有子網路中執行
- 使用專用安全群組限制存取
- 僅允許必要的 PostgreSQL 連接埠 (5432)

### 存取控制
- 使用最小權限原則的 IAM 角色
- 資料庫認證透過 AWS Secrets Manager
- 所有 API 呼叫都有完整的 CloudTrail 記錄

### 資料保護
- 傳輸中加密 (TLS 1.2+)
- 靜態資料加密 (Aurora 和 Glue)
- 敏感資料遮罩和匿名化

## 整合指南

### 與其他服務整合

#### Amazon Athena 查詢
```sql
-- 使用 Glue Catalog 進行 Athena 查詢
SELECT table_name, column_name, data_type 
FROM information_schema.columns 
WHERE table_catalog = 'genai_demo_catalog'
```

#### AWS Lake Formation
```bash
# 註冊 Glue Catalog 到 Lake Formation
aws lakeformation register-resource \
  --resource-arn arn:aws:glue:region:account:catalog \
  --use-service-linked-role
```

#### Amazon QuickSight
- 直接連接到 Glue Data Catalog
- 自動發現所有已註冊的表格
- 支援即時查詢和視覺化

### API 整合
```python
# Python SDK 範例
import boto3

glue = boto3.client('glue')

# 獲取所有表格
response = glue.get_tables(DatabaseName='genai_demo_catalog')
tables = response['TableList']

# 獲取表格 schema
table_schema = glue.get_table(
    DatabaseName='genai_demo_catalog',
    Name='customers'
)
```

## 維護指南

### 定期維護任務

#### 每週檢查
- [ ] 檢查 Crawler 執行狀態
- [ ] 驗證新表格是否被正確發現
- [ ] 檢查告警配置是否正常

#### 每月檢查
- [ ] 檢查成本使用情況
- [ ] 更新排除規則 (如有需要)
- [ ] 檢查 IAM 權限是否仍然適當

#### 每季檢查
- [ ] 檢查 Glue Catalog 資料品質
- [ ] 更新監控儀表板
- [ ] 檢查災難恢復程序

### 升級指南
```bash
# 更新 CDK Stack
cd infrastructure
npm run build
npx cdk diff DataCatalogStack
npx cdk deploy DataCatalogStack
```

## 參考資源

### AWS 文檔
- AWS Glue Developer Guide
- AWS Glue Crawler Best Practices
- Aurora PostgreSQL Integration

### 內部文檔
- [Architecture Design Document](../../.kiro/specs/architecture-viewpoints-enhancement/design.md)
- [Requirements Document](../../.kiro/specs/architecture-viewpoints-enhancement/requirements.md)
- [Development Standards](../../.kiro/steering/development-standards.md)

---

**文檔版本**: 1.0  
**最後更新**: 2025年9月24日 下午3:15 (台北時間)  
**維護者**: 架構團隊  
**審核者**: DevOps 團隊
