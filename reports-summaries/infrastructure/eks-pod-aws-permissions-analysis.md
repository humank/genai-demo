# EKS Pod AWS 權限配置分析報告

**分析時間**: 2025年9月24日 下午4:43 (台北時間)  
**分析範圍**: EKS Pod 應用程式 AWS 資源存取權限  
**狀態**: 🚨 **發現多項權限缺失**

## 🔍 應用程式 AWS 服務使用分析

### 📋 應用程式會使用的 AWS 服務

根據配置文件分析，應用程式會存取以下 AWS 服務：

#### 1. **CloudWatch (指標和日誌)**
```yaml
用途:
  - 自定義指標發布 (Thread Pool, JVM, HTTP)
  - 應用程式日誌寫入
  - Container Insights 整合
  - KEDA 自動擴展指標

配置:
  - Namespace: GenAIDemo/Production, GenAIDemo/Staging
  - 指標類型: 執行緒池、JVM記憶體、HTTP請求
  - 日誌群組: /aws/genai-demo/application
```

#### 2. **X-Ray (分散式追蹤)**
```yaml
用途:
  - 分散式請求追蹤
  - 效能瓶頸分析
  - 服務地圖生成
  - 錯誤追蹤和分析

配置:
  - 服務名稱: genai-demo-production/staging
  - 採樣率: 0.05 (production), 0.1 (staging)
  - 插件: EC2Plugin, ECSPlugin, EKSPlugin
```

#### 3. **ElastiCache Redis (分散式鎖)**
```yaml
用途:
  - 分散式鎖實現
  - 快取資料存取
  - 高可用性故障轉移

配置:
  - 模式: CLUSTER
  - 連線: ${REDIS_CLUSTER_NODES}
  - 故障轉移: 啟用
```

#### 4. **MSK Kafka (事件發布)**
```yaml
用途:
  - 領域事件發布
  - 非同步訊息處理
  - 事件驅動架構

配置:
  - Bootstrap Servers: ${KAFKA_BOOTSTRAP_SERVERS}
  - Topic: genai-demo-events-production/staging
  - 壓縮: lz4
```

#### 5. **RDS/Aurora (資料庫)**
```yaml
用途:
  - 主要資料儲存
  - 交易處理
  - 資料持久化

配置:
  - 引擎: PostgreSQL 15.4
  - 連線池: HikariCP
  - SSL/TLS 加密
```

#### 6. **Parameter Store/Secrets Manager**
```yaml
用途:
  - 配置參數讀取
  - 敏感資料存取 (資料庫密碼等)
  - 動態配置更新

配置:
  - 參數路徑: /genai-demo/{environment}/
  - 加密: KMS
```

#### 7. **S3 (可能的檔案儲存)**
```yaml
用途:
  - 檔案上傳/下載
  - 靜態資源存取
  - 備份和歸檔

配置:
  - 加密: AES-256/KMS
  - 版本控制: 啟用
```

## 🚨 權限缺失分析

### ❌ **嚴重缺失 - 應用程式 Service Account**

**問題**: 目前 EKS Stack 只配置了 Cluster Autoscaler 的 Service Account，**完全沒有為應用程式 Pod 配置 Service Account 和 IAM 權限**。

#### 當前配置狀況
```typescript
// ❌ 只有 Cluster Autoscaler Service Account
const clusterAutoscalerServiceAccount = this.cluster.addServiceAccount('ClusterAutoscalerServiceAccount', {
    name: 'cluster-autoscaler',
    namespace: 'kube-system',
});

// ❌ 完全缺少應用程式 Service Account
// 應用程式 Pod 無法存取任何 AWS 服務
```

### 📋 **缺失的權限清單**

#### 1. **CloudWatch 權限** ❌ **缺失**
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "cloudwatch:PutMetricData",
        "cloudwatch:GetMetricStatistics",
        "cloudwatch:ListMetrics",
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents",
        "logs:DescribeLogStreams",
        "logs:DescribeLogGroups"
      ],
      "Resource": "*"
    }
  ]
}
```

#### 2. **X-Ray 權限** ❌ **缺失**
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "xray:PutTraceSegments",
        "xray:PutTelemetryRecords",
        "xray:GetSamplingRules",
        "xray:GetSamplingTargets",
        "xray:GetSamplingStatisticSummaries"
      ],
      "Resource": "*"
    }
  ]
}
```

#### 3. **Parameter Store/Secrets Manager 權限** ❌ **缺失**
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ssm:GetParameter",
        "ssm:GetParameters",
        "ssm:GetParametersByPath",
        "secretsmanager:GetSecretValue",
        "secretsmanager:DescribeSecret"
      ],
      "Resource": [
        "arn:aws:ssm:*:*:parameter/genai-demo/*",
        "arn:aws:secretsmanager:*:*:secret:genai-demo/*"
      ]
    }
  ]
}
```

#### 4. **KMS 權限** ❌ **缺失**
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "kms:Decrypt",
        "kms:GenerateDataKey"
      ],
      "Resource": [
        "arn:aws:kms:*:*:key/*"
      ],
      "Condition": {
        "StringEquals": {
          "kms:ViaService": [
            "secretsmanager.*.amazonaws.com",
            "ssm.*.amazonaws.com"
          ]
        }
      }
    }
  ]
}
```

#### 5. **S3 權限** ❌ **缺失** (如果需要)
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::genai-demo-*",
        "arn:aws:s3:::genai-demo-*/*"
      ]
    }
  ]
}
```

## 🔧 修復建議

### 1. **立即修復 - 創建應用程式 Service Account**

在 EKS Stack 中添加應用程式 Service Account：

```typescript
// 在 EKS Stack 中添加
private createApplicationServiceAccount(projectName: string, environment: string): eks.ServiceAccount {
    // 創建應用程式 Service Account
    const appServiceAccount = this.cluster.addServiceAccount('ApplicationServiceAccount', {
        name: 'genai-demo-app',
        namespace: 'default',
    });

    // CloudWatch 權限
    appServiceAccount.addToPrincipalPolicy(new iam.PolicyStatement({
        effect: iam.Effect.ALLOW,
        actions: [
            'cloudwatch:PutMetricData',
            'cloudwatch:GetMetricStatistics',
            'cloudwatch:ListMetrics',
            'logs:CreateLogGroup',
            'logs:CreateLogStream',
            'logs:PutLogEvents',
            'logs:DescribeLogStreams',
            'logs:DescribeLogGroups'
        ],
        resources: ['*'],
    }));

    // X-Ray 權限
    appServiceAccount.addToPrincipalPolicy(new iam.PolicyStatement({
        effect: iam.Effect.ALLOW,
        actions: [
            'xray:PutTraceSegments',
            'xray:PutTelemetryRecords',
            'xray:GetSamplingRules',
            'xray:GetSamplingTargets',
            'xray:GetSamplingStatisticSummaries'
        ],
        resources: ['*'],
    }));

    // Parameter Store 權限
    appServiceAccount.addToPrincipalPolicy(new iam.PolicyStatement({
        effect: iam.Effect.ALLOW,
        actions: [
            'ssm:GetParameter',
            'ssm:GetParameters',
            'ssm:GetParametersByPath'
        ],
        resources: [
            `arn:aws:ssm:*:*:parameter/genai-demo/${environment}/*`
        ],
    }));

    // Secrets Manager 權限
    appServiceAccount.addToPrincipalPolicy(new iam.PolicyStatement({
        effect: iam.Effect.ALLOW,
        actions: [
            'secretsmanager:GetSecretValue',
            'secretsmanager:DescribeSecret'
        ],
        resources: [
            `arn:aws:secretsmanager:*:*:secret:genai-demo/${environment}/*`
        ],
    }));

    return appServiceAccount;
}
```

### 2. **更新 Kubernetes Deployment**

更新應用程式 Deployment 使用 Service Account：

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: genai-demo-app
  namespace: default
spec:
  template:
    spec:
      serviceAccountName: genai-demo-app  # ✅ 添加這行
      containers:
      - name: genai-demo
        image: genai-demo:latest
        env:
        - name: AWS_REGION
          value: "ap-east-2"
        - name: AWS_ROLE_ARN
          value: "arn:aws:iam::ACCOUNT:role/eksctl-genai-demo-addon-iamserviceaccount-Role"
        # ... 其他配置
```

### 3. **環境變數配置**

確保應用程式 Pod 有正確的環境變數：

```yaml
env:
- name: AWS_REGION
  value: "ap-east-2"
- name: AWS_DEFAULT_REGION  
  value: "ap-east-2"
- name: SPRING_PROFILES_ACTIVE
  value: "production"  # 或 staging
- name: REDIS_CLUSTER_NODES
  valueFrom:
    secretKeyRef:
      name: redis-config
      key: cluster-nodes
- name: KAFKA_BOOTSTRAP_SERVERS
  valueFrom:
    secretKeyRef:
      name: kafka-config
      key: bootstrap-servers
```

## 🔒 安全最佳實踐

### 1. **最小權限原則**
```typescript
// ✅ 使用具體的資源 ARN，避免 "*"
resources: [
    `arn:aws:ssm:${region}:${account}:parameter/genai-demo/${environment}/*`
],

// ✅ 添加條件限制
conditions: {
    StringEquals: {
        'aws:RequestedRegion': region
    }
}
```

### 2. **資源標籤和命名**
```typescript
// ✅ 使用一致的命名和標籤
const serviceAccountName = `${projectName}-${environment}-app`;
const roleName = `${projectName}-${environment}-app-role`;

// 添加標籤
cdk.Tags.of(appServiceAccount).add('Application', projectName);
cdk.Tags.of(appServiceAccount).add('Environment', environment);
cdk.Tags.of(appServiceAccount).add('Component', 'Application');
```

### 3. **權限分離**
```typescript
// ✅ 為不同功能創建不同的 Service Account
const appServiceAccount = this.createApplicationServiceAccount();
const monitoringServiceAccount = this.createMonitoringServiceAccount();
const eventProcessingServiceAccount = this.createEventProcessingServiceAccount();
```

## 📊 影響評估

### 🚨 **當前狀況**
```bash
應用程式 Pod 狀態: ❌ 無法存取 AWS 服務
CloudWatch 指標: ❌ 無法發布
X-Ray 追蹤: ❌ 無法寫入
配置讀取: ❌ 無法存取 Parameter Store
事件發布: ❌ 可能無法存取 MSK (取決於網路配置)
```

### ✅ **修復後狀況**
```bash
應用程式 Pod 狀態: ✅ 完整 AWS 服務存取
CloudWatch 指標: ✅ 正常發布和監控
X-Ray 追蹤: ✅ 完整分散式追蹤
配置讀取: ✅ 動態配置和密鑰存取
KEDA 自動擴展: ✅ 基於 CloudWatch 指標
```

## 🚀 實施計劃

### Phase 1: 緊急修復 (立即)
1. ✅ 在 EKS Stack 中添加應用程式 Service Account
2. ✅ 配置基本的 CloudWatch 和 X-Ray 權限
3. ✅ 更新 Kubernetes Deployment

### Phase 2: 完整權限 (1-2天)
1. ✅ 添加 Parameter Store/Secrets Manager 權限
2. ✅ 配置 KMS 解密權限
3. ✅ 測試所有 AWS 服務存取

### Phase 3: 安全優化 (1週)
1. ✅ 實施最小權限原則
2. ✅ 添加條件限制和資源範圍
3. ✅ 設置權限監控和告警

## 📋 檢查清單

### ✅ **必須修復的項目**
- [ ] 創建應用程式 Service Account
- [ ] 配置 CloudWatch 權限 (指標 + 日誌)
- [ ] 配置 X-Ray 權限 (追蹤)
- [ ] 配置 Parameter Store 權限 (配置)
- [ ] 配置 Secrets Manager 權限 (密鑰)
- [ ] 配置 KMS 權限 (解密)
- [ ] 更新 Kubernetes Deployment
- [ ] 測試所有 AWS 服務存取

### 🔍 **可選但建議的項目**
- [ ] S3 權限 (如果需要檔案存取)
- [ ] SQS 權限 (如果使用佇列)
- [ ] SNS 權限 (如果需要通知)
- [ ] 權限監控和告警設置
- [ ] 定期權限審查機制

## 🎯 預期效益

### 📈 **功能改善**
- **監控**: 完整的 CloudWatch 指標和 X-Ray 追蹤
- **自動擴展**: KEDA 基於實際指標自動擴展
- **配置管理**: 動態配置和密鑰管理
- **安全性**: 加密和存取控制

### 🔧 **運維改善**
- **可觀測性**: 全面的應用程式監控
- **故障排除**: 分散式追蹤和詳細日誌
- **自動化**: 基於指標的自動擴展
- **合規性**: 符合 AWS 安全最佳實踐

---

**🚨 緊急建議**: 立即修復應用程式 Service Account 權限缺失問題  
**優先級**: P0 (最高優先級)  
**預估修復時間**: 2-4 小時  
**影響範圍**: 所有 EKS Pod 的 AWS 服務存取
