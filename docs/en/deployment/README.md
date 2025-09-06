<!-- 
此文件需要手動翻譯
原文件: deployment/README.md
翻譯日期: Thu Aug 21 22:14:37 CST 2025

請將以下中文內容翻譯為英文，保持 Markdown 格式不變
-->

# Deployment 目錄

此目錄包含專案的部署相關檔案。

## 檔案說明

### Kubernetes 部署

- `k8s/` - Kubernetes 部署配置檔案
  - `configmap.yaml` - 配置映射
  - `deployment.yaml` - 部署配置

### AWS EKS 部署

- `deploy-to-eks.sh` - EKS 部署腳本
- `aws-eks-architecture.md` - AWS EKS 架構說明

## 使用方式

```bash
# 部署到 EKS
./deployment/deploy-to-eks.sh

# 使用 kubectl 部署
kubectl apply -f deployment/k8s/
```

**注意**: EKS 部署需要先配置 AWS CLI 和 kubectl。


<!-- 翻譯完成後請刪除此註釋 -->
