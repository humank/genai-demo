# Quick Deployment Guide - GenAI Demo Infrastructure

**Updated**: September 29, 2025 12:49 PM (Taipei Time)  
**Task 10 Status**: ✅ **COMPLETED** - IAM Fine-grained Access Control

## 🚀 Quick Start Commands

### 1. Complete Infrastructure Deployment

```bash
# Development environment (recommended for first deployment)
./deploy-unified.sh full -e development -r ap-east-2

# Staging environment with analytics
./deploy-unified.sh full -e staging --enable-analytics -a your-email@company.com

# Production environment with multi-region
./deploy-unified.sh full -e production --enable-multi-region --enable-analytics
```

### 2. Component-Specific Deployments

```bash
# Foundation only (network, security, IAM)
./deploy-unified.sh foundation -e development

# Security components (IAM, SSO, IRSA) - Task 10 ✅
./deploy-unified.sh security -e development

# Data layer (RDS, ElastiCache, MSK)
./deploy-unified.sh data -e development

# Compute layer (EKS)
./deploy-unified.sh compute -e development

# Observability (monitoring, alerting)
./deploy-unified.sh observability -e development
```

### 3. IAM Security Deployment (Task 10 Specific)

```bash
# Deploy IAM fine-grained access control
./deploy-iam-security.sh development ap-east-2

# Deploy with SSO integration
./deploy-iam-security.sh production ap-east-2 arn:aws:sso:::instance/ssoins-xxxxxxxxx
```

## 📋 Pre-deployment Checklist

### Prerequisites ✅
- [ ] AWS CLI configured (`aws sts get-caller-identity`)
- [ ] Node.js 18+ installed (`node --version`)
- [ ] AWS CDK CLI installed (`npm install -g aws-cdk`)
- [ ] Proper AWS permissions for CDK deployment

### Environment Setup ✅
```bash
# 1. Install dependencies
npm install

# 2. Build project
npm run build

# 3. Run tests
npm test

# 4. Check status
./status-check.sh
```

## 🔍 Deployment Status Monitoring

### Check Overall Status
```bash
# Check all stacks status
./deploy-unified.sh --status -e development -r ap-east-2

# Quick infrastructure health check
./status-check.sh
```

### Verify Specific Components
```bash
# Check IAM roles (Task 10)
aws iam list-roles --query 'Roles[?contains(RoleName, `genai-demo-development`)].RoleName' --output table

# Check EKS service accounts
kubectl get serviceaccounts -A | grep genai-demo

# Check CloudFormation stacks
aws cloudformation list-stacks --stack-status-filter CREATE_COMPLETE UPDATE_COMPLETE
```

## 🛠️ Troubleshooting Quick Fixes

### Common Issues and Solutions

#### 1. CDK Bootstrap Required
```bash
# Bootstrap CDK in your region
cdk bootstrap --region ap-east-2
```

#### 2. Build Failures
```bash
# Clean and rebuild
npm run clean
npm install
npm run build
```

#### 3. Test Failures
```bash
# Run specific test suites
npm run test:unit
npm run test:integration
```

#### 4. Stack Dependencies
```bash
# Deploy in correct order
./deploy-unified.sh foundation -e development
./deploy-unified.sh data -e development
./deploy-unified.sh compute -e development
./deploy-unified.sh security -e development
```

## 🔐 Security Deployment Verification

### Task 10 - IAM Fine-grained Access Control ✅

#### 1. Verify IAM Stacks
```bash
# Check IAM stack
aws cloudformation describe-stacks --stack-name genai-demo-development-iam --region ap-east-2

# Check SSO stack (if deployed)
aws cloudformation describe-stacks --stack-name genai-demo-development-sso --region ap-east-2

# Check EKS IRSA stack
aws cloudformation describe-stacks --stack-name genai-demo-development-eks-irsa --region ap-east-2
```

#### 2. Test IAM Roles
```bash
# List application roles
aws iam get-role --role-name genai-demo-development-app-role

# Check role policies
aws iam list-attached-role-policies --role-name genai-demo-development-app-role
```

#### 3. Verify EKS Integration
```bash
# Update kubeconfig
aws eks update-kubeconfig --region ap-east-2 --name genai-demo-development-cluster

# Check service accounts
kubectl get serviceaccounts -n application
kubectl describe serviceaccount genai-demo-app-sa -n application
```

## 📊 Deployment Options Matrix

| Environment | Command | Features | Use Case |
|-------------|---------|----------|----------|
| Development | `./deploy-unified.sh full -e development` | Basic setup, fast deployment | Daily development |
| Staging | `./deploy-unified.sh full -e staging --enable-analytics` | Analytics enabled, testing | Pre-production validation |
| Production | `./deploy-unified.sh full -e production --enable-multi-region --enable-analytics` | Full features, multi-region | Production workloads |

## 🎯 NPM Script Shortcuts

```bash
# Quick deployment commands
npm run deploy:dev          # Development environment
npm run deploy:staging      # Staging with analytics
npm run deploy:prod         # Production with multi-region

# Component deployments
npm run deploy:foundation   # Network, security, IAM
npm run deploy:security     # IAM, SSO, IRSA (Task 10)
npm run deploy:data         # RDS, ElastiCache, MSK
npm run deploy:compute      # EKS cluster

# Utility commands
npm run status              # Check deployment status
npm run destroy:dev         # Destroy development environment
```

## 🔄 Deployment Workflow

### Standard Deployment Process
1. **Preparation** ✅
   ```bash
   ./status-check.sh
   npm run build
   npm test
   ```

2. **Foundation Deployment** ✅
   ```bash
   ./deploy-unified.sh foundation -e development
   ```

3. **Security Deployment** ✅ (Task 10)
   ```bash
   ./deploy-unified.sh security -e development
   ```

4. **Data Layer Deployment** ✅
   ```bash
   ./deploy-unified.sh data -e development
   ```

5. **Compute Deployment** ✅
   ```bash
   ./deploy-unified.sh compute -e development
   ```

6. **Observability Deployment** ✅
   ```bash
   ./deploy-unified.sh observability -e development
   ```

7. **Verification** ✅
   ```bash
   ./deploy-unified.sh --status -e development
   ```

## 🚨 Emergency Procedures

### Rollback Deployment
```bash
# Destroy specific environment
./deploy-unified.sh --destroy -e development

# Destroy specific component
cdk destroy genai-demo-development-iam --region ap-east-2
```

### Quick Recovery
```bash
# Redeploy foundation
./deploy-unified.sh foundation -e development

# Redeploy security (Task 10)
./deploy-iam-security.sh development ap-east-2
```

## 📞 Support Resources

### Documentation
- [Complete Infrastructure Guide](README.md)
- [Deployment Scripts Summary](DEPLOYMENT_SCRIPTS_SUMMARY.md)
- [Security Implementation](SECURITY_IMPLEMENTATION.md)
- [Troubleshooting Guide](TROUBLESHOOTING.md)

### Quick Help
```bash
# Get deployment help
./deploy-unified.sh --help

# Get IAM security help
./deploy-iam-security.sh --help

# Check infrastructure status
./status-check.sh
```

### AWS Console Links
- [CloudFormation Stacks](https://console.aws.amazon.com/cloudformation/)
- [IAM Roles](https://console.aws.amazon.com/iam/home#/roles)
- [EKS Clusters](https://console.aws.amazon.com/eks/home)
- [SSO Console](https://console.aws.amazon.com/singlesignon/)

---

**Status**: ✅ **Task 10 Completed - IAM Fine-grained Access Control**  
**Infrastructure**: ✅ **Ready for Production Deployment**  
**Security**: ✅ **Fully Implemented and Tested**