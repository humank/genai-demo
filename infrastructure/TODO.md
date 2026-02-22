# Infrastructure TODO — CDK 待辦事項清單

> **建立日期**: 2026-02-22
> **負責人**: Infrastructure Team
> **狀態**: 進行中

---

## 優先級說明

| 等級 | 說明 | 時程建議 |
|------|------|----------|
| P0 | 阻塞部署或有安全風險，必須立即處理 | 本週內 |
| P1 | 功能不完整但不阻塞核心部署 | 2 週內 |
| P2 | 增強功能，提升可靠性與可觀測性 | 1 個月內 |
| P3 | 非必要但有價值的額外 Stack 啟用 | 依需求排程 |

---

## P0 — 關鍵阻塞項目

### 1. 替換 Secrets Stack 中的 Placeholder 值

**檔案**: `src/stacks/secrets-stack.ts` (行 1053-1056, 1080-1083)

**問題**: API 金鑰和應用程式密鑰使用明文 placeholder，部署到任何環境都會暴露不安全的預設值。

**需替換的值**:
- `PLACEHOLDER_OPENAI_KEY` → 實際 OpenAI API Key
- `PLACEHOLDER_ANTHROPIC_KEY` → 實際 Anthropic API Key
- `PLACEHOLDER_BEDROCK_KEY` → 實際 Bedrock Access Key
- `PLACEHOLDER_EXTERNAL_KEY` → 實際外部服務金鑰
- `PLACEHOLDER_JWT_SECRET` → 隨機產生的 JWT 簽名密鑰
- `PLACEHOLDER_ENCRYPTION_KEY` → 隨機產生的加密金鑰
- `PLACEHOLDER_SESSION_SECRET` → 隨機產生的 Session 密鑰
- `PLACEHOLDER_WEBHOOK_SECRET` → 隨機產生的 Webhook 密鑰

**建議做法**:
- 不要在程式碼中硬編碼真實金鑰
- 使用 `cdk.SecretValue.secretsManager()` 或 `cdk.SecretValue.ssmSecure()` 從外部讀取
- 或在部署後透過 AWS Console / CLI 手動更新 Secret 值
- 考慮使用 `aws secretsmanager put-secret-value` 在 CI/CD pipeline 中注入

---

### 2. 修正 Network Stack 跨區域 VPC Peering

**檔案**: `src/stacks/network-stack.ts` (行 203)

**問題**: 跨區域 VPC Peering 使用假的 VPC ID `vpc-${peerRegion}`，無法建立實際的 peering 連線。

**目前程式碼**:
```typescript
peerVpcId: `vpc-${peerRegion}`, // This should be replaced with actual VPC ID
```

**建議做法**:
- 方案 A: 使用 cross-stack reference，從 DR 區域的 NetworkStack 取得 VPC ID
- 方案 B: 使用 SSM Parameter Store 儲存各區域的 VPC ID，部署時讀取
- 方案 C: 透過 `deploy.config.ts` 加入各區域的 VPC ID 設定
- 注意: 跨區域 peering 需要在兩端都接受連線請求

---

## P1 — 功能補完項目

### 3. 完善 Alerting Stack 的 Slack 整合

**檔案**: `src/stacks/alerting-stack.ts` (行 737-742)

**問題**: Slack 通知目前只是將 webhook URL 作為 SNS URL subscription，缺乏格式化和錯誤處理。

**目前程式碼**:
```typescript
// Note: In a real implementation, you would use AWS Chatbot or Lambda for Slack integration
// For now, we'll create a placeholder for the webhook URL
topic.addSubscription(new snsSubscriptions.UrlSubscription(config.slackWebhookUrl));
```

**建議做法**:
- 方案 A (推薦): 使用 AWS Chatbot 整合 Slack，支援互動式通知
- 方案 B: 建立 Lambda 函數格式化 SNS 訊息後發送到 Slack webhook
- 需要設定 Slack workspace 和 channel 的 OAuth 權限

---

### 4. 記錄 Incident Manager 手動設定步驟

**檔案**: `src/stacks/incident-manager-stack.ts` (行 90-103)

**問題**: AWS CDK 尚未支援 `CfnContact`，聯絡人必須手動建立，但缺乏操作文件。

**目前狀態**:
```typescript
// Return empty array - contacts must be created manually
// until AWS CDK adds CfnContact support
console.warn('SSM Incidents Contacts must be created manually in AWS Console');
```

**需要做的事**:
- [ ] 撰寫操作手冊 (runbook)，記錄如何在 AWS Console 建立 Incident Contacts
- [ ] 記錄需要建立的聯絡人清單 (L1/L2/L3 on-call)
- [ ] 記錄 escalation plan 的設定步驟
- [ ] 在 `incident-manager-stack.ts` 中加入 CfnOutput 提示操作人員
- [ ] 定期追蹤 AWS CDK 是否已支援 CfnContact

---

### 5. 完成 GitOps Monitoring 的 ArgoCD 指標收集

**檔案**: `src/stacks/gitops-monitoring-stack.ts` (行 240-312)

**問題**: ArgoCD 和 Argo Rollouts 的指標收集目前回傳硬編碼的假資料。

**目前狀態**:
```python
# Placeholder for ArgoCD metrics collection
# For now, return placeholder metrics
```

**建議做法**:
- [ ] 設定 Prometheus ServiceMonitor 抓取 ArgoCD metrics endpoint
- [ ] 透過 Kubernetes API 查詢 ArgoCD Application CRD 狀態
- [ ] 設定 Argo Rollouts metrics 的 Prometheus scrape config
- [ ] 將 Lambda 中的 placeholder 替換為實際的 Prometheus 查詢
- [ ] 或改用 CloudWatch Container Insights 收集 ArgoCD 指標

---

### 6. 替換 MSK Cross-Region 的 Placeholder Bootstrap Servers

**檔案**: `src/stacks/msk-cross-region-stack.ts` (行 341)

**問題**: 當沒有提供 secondary MSK cluster 時，MirrorMaker 2.0 使用 `'placeholder-secondary-servers'` 作為 bootstrap servers。

**目前程式碼**:
```typescript
const secondaryBootstrapServers = secondaryMskCluster
    ? cdk.Fn.getAtt(secondaryMskCluster.logicalId, 'BootstrapBrokerStringSaslIam').toString()
    : 'placeholder-secondary-servers';
```

**建議做法**:
- 確保啟用此 Stack 時一定傳入 `secondaryMskCluster` 參數
- 或使用 SSM Parameter Store 儲存 secondary cluster 的 bootstrap servers
- 加入驗證邏輯：若無 secondary cluster 則跳過 MirrorMaker 部署

---

## P2 — 增強與啟用項目

### 7. 啟用優先 Stack 到 bin/infrastructure.ts

**檔案**: `bin/infrastructure.ts`

**問題**: 33+ 個 Stack 已實作但未在主程式中實例化。以下為建議優先啟用的 Stack：

| Stack | 用途 | 依賴 |
|-------|------|------|
| SecretsStack | 集中管理密鑰與自動輪換 | NetworkStack, SecurityStack |
| IAMStack | 細粒度 IAM 角色與策略 | SecurityStack |
| NetworkSecurityStack | WAF, Shield, 網路防護 | NetworkStack |
| DisasterRecoveryStack | 跨區域災難復原自動化 | NetworkStack, RdsStack |
| Route53FailoverStack | DNS 故障轉移 | NetworkStack |
| CostOptimizationStack | 成本監控與優化建議 | AlertingStack |
| IncidentManagerStack | 事件管理與 on-call 排程 | AlertingStack |
| GitOpsMonitoringStack | CI/CD 與 GitOps 監控 | EKSStack, AlertingStack |

**注意**: 啟用前需確認各 Stack 的 props 介面，並正確傳入依賴的資源引用。

---

### 8. 取得新域名並啟用 DNS 相關設定

**檔案**: `deploy.config.ts`, `src/stacks/certificate-stack.ts`, `src/stacks/route53-*.ts`

**問題**: 原域名 `kimkao.io` 已停用，所有域名相關設定已被註解。

**需要做的事**:
- [ ] 取得新域名並在 Route53 建立 Hosted Zone
- [ ] 更新 `deploy.config.ts` 中各環境的 `domain`、`certificateArn`、`hostedZoneId`
- [ ] 啟用 CertificateStack (ACM 憑證)
- [ ] 啟用 Route53GlobalRoutingStack 或 Route53FailoverStack
- [ ] 考慮啟用 CloudFrontGlobalCdnStack 作為 CDN

---

### 9. 執行完整 CDK Nag 合規檢查

**指令**: `npx cdk synth -c enableCdkNag=true`

**需要做的事**:
- [ ] 執行 AwsSolutionsChecks 並記錄所有違規項目
- [ ] 修正 Critical 和 High 等級的違規
- [ ] 對於無法修正的項目，加入合理的 NagSuppression 並附上說明
- [ ] 建立 CI/CD pipeline 中的自動 CDK Nag 檢查

---

## P3 — 額外 Stack 啟用 (依需求)

### 10. 啟用剩餘的輔助 Stack

以下 Stack 已實作但尚未啟用，可依業務需求逐步啟用：

**安全類**:
- SecurityHubStack — AWS Security Hub 整合
- SSOStack — AWS SSO 單一登入
- SecurityMonitoringStack — 安全事件監控

**可觀測性類**:
- CrossRegionObservabilityStack — 跨區域監控整合
- DeploymentMonitoringStack — 部署監控
- CloudWatchMSKDashboardStack — MSK CloudWatch 儀表板
- GrafanaMSKDashboardStack — MSK Grafana 儀表板
- MSKAlertingStack — MSK 專用告警

**成本類**:
- CostDashboardStack — 成本儀表板
- CostManagementStack — 成本管理
- CostUsageReportsStack — 成本使用報告

**資料與合規類**:
- DataRetentionStack — 資料保留策略
- WellArchitectedStack — Well-Architected 審查
- ConfigInsightsStack — AWS Config 洞察

**基礎設施類**:
- ALBHealthCheckStack — ALB 健康檢查增強
- CloudFrontGlobalCdnStack — 全球 CDN
- CrossRegionSyncStack — 跨區域同步
- DeadlockMonitoringStack — 資料庫死鎖監控
- EKSIRSAStack — EKS IRSA 角色
- KmsStack — 額外 KMS 金鑰管理
- MultiRegionStack — 多區域協調

---

## 未在 index.ts 中匯出的 Stack

以下 Stack 檔案存在但未在 `src/stacks/index.ts` 中匯出，啟用前需先加入 export：

- `alb-health-check-stack.ts`
- `cloudfront-global-cdn-stack.ts`
- `cloudwatch-msk-dashboard-stack.ts`
- `config-insights-stack.ts`
- `cost-dashboard-stack.ts`
- `cost-management-stack.ts`
- `cost-usage-reports-stack.ts`
- `cross-region-sync-stack.ts`
- `deadlock-monitoring-stack.ts`
- `deployment-monitoring-stack.ts`
- `gitops-monitoring-stack.ts`
- `grafana-msk-dashboard-stack.ts`
- `incident-manager-stack.ts`
- `msk-alerting-stack.ts`
- `security-hub-stack.ts`
- `security-monitoring-stack.ts`
- `well-architected-stack.ts`

---

## 快速參考指令

```bash
cd infrastructure

# 合成 CloudFormation 模板
npx cdk synth

# 合成並啟用 CDK Nag 檢查
npx cdk synth -c enableCdkNag=true

# 查看變更差異
npx cdk diff

# 部署所有 Stack
npx cdk deploy --all

# 部署特定 Stack
npx cdk deploy development-NetworkStack

# 執行測試
npm test
```

---

**Last Updated**: 2026-02-22
