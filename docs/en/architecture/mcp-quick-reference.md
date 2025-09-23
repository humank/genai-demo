
# Reference

## 🚀 快速開始

### 一鍵Command

```bash
# 完整評估 (推薦)
npm run assessment:complete

# Testing
npm run mcp:test                    # Testing
npm run well-architected:assessment # WA 框架評估
npm run architecture:assess         # 架構分析
npm run monitoring:continuous       # 持續Monitoring
```

## 📊 當前狀態Dashboard

### 整體健康度

```
🎯 Well-Architected 總分: 90/100 (優秀)
🧪 MCP 整合狀態: 100% 通過
💰 月度成本: $999 (已優化)
🔒 安全評分: 100/100 (完美)
```

### 六大支柱快速檢視

```
運營卓越: 75/100  🟡 需改進
Security:   100/100 🟢 優秀
Reliability:   100/100 🟢 優秀  
Performance效率: 100/100 🟢 優秀
成本優化: 85/100  🟢 良好
可持續性: 100/100 🟢 優秀
```

## Tools

### 已配置的 MCP 服務器

| 服務器 | 用途 | 狀態 | 主要功能 |
|--------|------|------|----------|
| `aws-docs` | 文檔查詢 | ✅ | 搜索 AWS 官方文檔 |
| `aws-cdk` | CDK 指導 | ✅ | CDK Best Practice檢查 |
| `aws-pricing` | 成本分析 | ✅ | 實時成本估算 |
| `aws-iam` | 安全審查 | ✅ | IAM 政策分析 |
| `aws-core` | WA 審查 | ✅ | 架構框架評估 |

### 在 Kiro IDE 中使用

```
詢問 Kiro:
"搜索 EKS Best Practice的 AWS 文檔"
"分析我的 CDK 項目成本"
"檢查 IAM 政策的Security"
"解釋 CDK Nag 規則 AwsSolutions-IAM4"
```

## 📄 重要報告位置

### 主要報告文件

```
../../../infrastructure/docs/
├── 📊 well-architected-assessment.md      # 詳細 WA 審查
├── 🤖 automated-architecture-assessment.md # 自動架構分析  
├── 📈 continuous-improvement-report.md     # 持續改進Monitoring
├── 👔 ../../../reports-summaries/infrastructure/executive-summary.md                 # 高層執行摘要
├── 🧪 ../../../../../reports-summaries/../../../infrastructure/mcp-integration-test-report.md      # Testing
└── 📋 assessment-summary/                  # 綜合評估摘要
```

### 快速查看Command

```bash
# 查看最新評估結果
cat ../../../infrastructure/docs/../../../reports-summaries/infrastructure/executive-summary.md

# Testing
cat ../../../infrastructure/docs/../../../../../reports-summaries/../../../infrastructure/mcp-integration-test-report.md

# 查看成本分析
jq '.costAnalysis' ../../../infrastructure/docs/architecture-assessment-summary.json
```

## 🚨 告警與閾值

### 關鍵Metrics閾值

```
🔴 緊急 (立即處理):
- 安全評分 < 80%
- Availability < 99.9%
- 高嚴重性漏洞 > 0

🟡 警告 (24小時內):
- 成本增加 > 20%
- Performance下降 > 15%
- WA 總分 < 85%

🟢 正常:
- 所有Metrics在目標範圍內
```

### 告警處理流程

```
1. 檢查告警詳情
2. 查看相關報告
3. 執行recommendations的修復措施
4. 重新運行評估驗證
5. 更新文檔和流程
```

## Maintenance

### 每日任務 (5 分鐘)

```bash
# 檢查 MCP 狀態
npm run mcp:test

# 查看關鍵Metrics
cat ../../../infrastructure/docs/../../../reports-summaries/infrastructure/executive-summary.md | head -20
```

### 每週任務 (30 分鐘)

```bash
# 完整 WA 評估
npm run well-architected:assessment

# 檢查趨勢變化
npm run monitoring:continuous
```

### 每月任務 (2 小時)

```bash
# 完整評估套件
npm run assessment:complete

# Implementation
# 更新文檔和流程
# 團隊培訓和知識分享
```

## Implementation

### Implementation

- [ ] 設置成本告警
- [ ] 啟用詳細Monitoring
- [ ] 更新Resource標籤

### Implementation

- [ ] 優化 IAM 政策
- [ ] 實施快取Policy
- [ ] 加強Health Check

### Implementation

- [ ] 多 AZ Deployment優化
- [ ] Auto Scaling調優
- [ ] 災難恢復測試

## Troubleshooting

### 常見問題

```
❌ MCP 服務器連接失敗
→ 檢查: uv --version && aws sts get-caller-identity

❌ 評估報告生成失敗  
→ 檢查: npm run mcp:test && 查看錯誤Logging

❌ 成本數據不準確
→ 檢查: AWS 憑證和區域設置

❌ 權限被拒絕
→ 檢查: IAM 政策和 AWS_PROFILE Environment變數
```

### 緊急聯繫

- **技術支援**: DevOps 團隊
- **架構諮詢**: 架構團隊  
- **安全問題**: 安全團隊

## Resources

### 必讀文檔

1. [MCP 整合重要性說明](mcp-integration-importance.md)
2. [任務 22 執行摘要](../../../reports-summaries/infrastructure/executive-summary.md)
3. [完整 MCP 整合指南](../../../infrastructure/docs/MCP_INTEGRATION_GUIDE.md)

### Resources

- [AWS Well-Architected Framework](https://aws.amazon.com/architecture/well-architected/)
- [MCP 協議規範](https://modelcontextprotocol.io/)
- [AWS CDK Best Practice](https://docs.aws.amazon.com/cdk/v2/guide/best-practices.html)

---

## 🏆 成功MetricsTracing

```
當前狀態 vs 目標:
✅ WA 總分: 90% (目標: ≥85%)
✅ 安全評分: 100% (目標: ≥90%)  
✅ 成本優化: 85% (目標: ≥80%)
✅ 自動化率: 95% (目標: ≥90%)
✅ Availability: 99.95% (目標: ≥99.9%)
```

**🎉 所有關鍵Metrics均已達到或超越目標！**

---

*📅 最後更新: 2025-09-11*  
*🔄 下次更新: 每週自動更新*  
*📞 支援: DevOps 團隊*
