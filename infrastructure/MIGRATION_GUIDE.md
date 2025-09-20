# Migration Guide: Separate Stacks → Consolidated Deployment

## Overview

This guide helps you migrate from the previous separate CDK applications to the new consolidated deployment approach.

## 🎯 Migration Benefits

### Before: 3 Separate Applications

- **Main Infrastructure**: 4 stacks in `bin/infrastructure.ts`
- **Analytics Pipeline**: 1 stack in `bin/analytics.ts`
- **Multi-Region Setup**: 15+ stacks in `bin/multi-region-deployment.ts`

### After: 1 Unified Application

- **Consolidated Main**: 5-6 stacks in `bin/infrastructure.ts`
- **Specialized Multi-Region**: Remains separate for complex DR scenarios
- **Deprecated Analytics**: Functionality moved to main application

## 📋 Pre-Migration Checklist

### 1. Backup Current Infrastructure

```bash
# Export current stack outputs
aws cloudformation describe-stacks --region us-east-1 > current-stacks-backup.json

# List all existing stacks
cdk list > current-cdk-stacks.txt
```

### 2. Verify CDK Version

```bash
# Check current CDK version
cdk --version

# Should show: 2.208.0 or higher
# If not, upgrade: npm install -g aws-cdk@latest
```

### 3. Review Current Configuration

```bash
# Check current context settings
cat cdk.context.json

# Review current deployment parameters
grep -r "context" bin/
```

## 🔄 Migration Scenarios

### Scenario 1: Currently Using `bin/infrastructure.ts` Only

**Status**: ✅ **No Migration Needed**

Your deployment will continue to work. Optional improvements:

```bash
# Add analytics capability
cdk deploy --all --context enableAnalytics=true

# Add alerting
cdk deploy --all --context alertEmail=your-email@company.com
```

### Scenario 2: Currently Using `bin/analytics.ts` Separately

**Migration Required**: Move to consolidated deployment

#### Step 1: Destroy Existing Analytics Stack

```bash
# If you have existing analytics deployment
cdk destroy -a "npx ts-node bin/analytics.ts"
```

#### Step 2: Deploy Consolidated Infrastructure

```bash
# Deploy with analytics enabled
./deploy-consolidated.sh production us-east-1 true true
```

#### Step 3: Verify Analytics Components

```bash
# Check analytics stack deployment
aws cloudformation describe-stacks --stack-name GenAIDemo-Prod-AnalyticsStack

# Verify S3 data lake
aws s3 ls | grep data-lake

# Check Kinesis Firehose
aws firehose list-delivery-streams
```

### Scenario 3: Using Both Main + Analytics Separately

**Migration Required**: Consolidate into single deployment

#### Step 1: Export Current Configuration

```bash
# Save current outputs
aws cloudformation describe-stacks --stack-name YourAnalyticsStack > analytics-outputs.json
aws cloudformation describe-stacks --stack-name YourMainStack > main-outputs.json
```

#### Step 2: Plan Migration

```bash
# Test consolidated deployment in development first
cdk deploy --all \
  --context environment=development \
  --context enableAnalytics=true \
  --context region=us-east-1
```

#### Step 3: Migrate Production

```bash
# Destroy separate analytics stack
cdk destroy YourAnalyticsStack

# Deploy consolidated infrastructure
./deploy-consolidated.sh production us-east-1 true true
```

### Scenario 4: Using Multi-Region Deployment

**Status**: ✅ **No Migration Needed**

Multi-region deployment remains separate for specialized DR scenarios:

```bash
# Continue using multi-region deployment
cdk deploy --all -a "npx ts-node bin/multi-region-deployment.ts"
```

**Optional**: Use consolidated deployment for single-region environments:

```bash
# Development/staging with consolidated approach
./deploy-consolidated.sh development us-east-1 true

# Production with multi-region approach
cdk deploy --all -a "npx ts-node bin/multi-region-deployment.ts"
```

## 🛠️ Migration Steps

### Step 1: Update Dependencies

```bash
cd infrastructure
npm install
npm run build
```

### Step 2: Test in Development

```bash
# Deploy to development environment first
./deploy-consolidated.sh development us-east-1 true false
```

### Step 3: Validate Functionality

```bash
# Check all stacks deployed successfully
cdk list

# Verify stack outputs
aws cloudformation describe-stacks --stack-name GenAIDemo-Dev-CoreInfrastructureStack
```

### Step 4: Migrate Staging

```bash
# Deploy to staging
./deploy-consolidated.sh staging us-east-1 true true
```

### Step 5: Migrate Production

```bash
# Deploy to production (with approval)
cdk deploy --all \
  --context environment=production \
  --context enableAnalytics=true \
  --context enableCdkNag=true \
  --context alertEmail=ops@company.com \
  --require-approval broadening
```

## 🔧 Configuration Mapping

### Old Analytics Configuration → New Consolidated

| Old Parameter | New Parameter | Notes |
|---------------|---------------|-------|
| `--context vpc-id=vpc-xxx` | Automatic | VPC created by NetworkStack |
| `--context kms-key-id=key-xxx` | Automatic | KMS key created by SecurityStack |
| `--context msk-cluster-arn=arn:xxx` | Mock cluster | Real MSK integration available |
| `--context alerting-topic-arn=arn:xxx` | Automatic | SNS topics created by AlertingStack |

### Context Parameter Migration

```bash
# Old analytics deployment
cdk deploy -a "npx ts-node bin/analytics.ts" \
  --context vpc-id=vpc-12345 \
  --context kms-key-id=key-67890

# New consolidated deployment  
cdk deploy --all \
  --context environment=production \
  --context enableAnalytics=true
```

## 🚨 Troubleshooting

### Issue: Stack Dependencies

**Problem**: Dependency conflicts during migration

**Solution**:

```bash
# Deploy stacks in correct order
cdk deploy GenAIDemo-Prod-NetworkStack
cdk deploy GenAIDemo-Prod-SecurityStack  
cdk deploy GenAIDemo-Prod-AlertingStack
cdk deploy GenAIDemo-Prod-CoreInfrastructureStack
cdk deploy GenAIDemo-Prod-ObservabilityStack
cdk deploy GenAIDemo-Prod-AnalyticsStack
```

### Issue: Resource Name Conflicts

**Problem**: Resource names conflict between old and new stacks

**Solution**:

```bash
# Use different stack prefixes
cdk deploy --all --context stackPrefix=GenAIDemoV2-
```

### Issue: Missing Permissions

**Problem**: IAM permissions missing after migration

**Solution**:

```bash
# Redeploy security stack first
cdk deploy GenAIDemo-Prod-SecurityStack
cdk deploy --all
```

## ✅ Post-Migration Validation

### 1. Verify All Stacks

```bash
# List deployed stacks
cdk list

# Expected output:
# GenAIDemo-Prod-NetworkStack
# GenAIDemo-Prod-SecurityStack
# GenAIDemo-Prod-AlertingStack
# GenAIDemo-Prod-CoreInfrastructureStack
# GenAIDemo-Prod-ObservabilityStack
# GenAIDemo-Prod-AnalyticsStack (if enabled)
```

### 2. Test Analytics Pipeline (if enabled)

```bash
# Check S3 data lake
aws s3 ls s3://genai-demo-production-data-lake-ACCOUNT/

# Test Kinesis Firehose
aws firehose put-record \
  --delivery-stream-name genai-demo-production-domain-events-firehose \
  --record '{"Data": "{\"test\": \"data\"}"}'
```

### 3. Verify Monitoring

```bash
# Check CloudWatch dashboards
aws cloudwatch list-dashboards

# Verify SNS topics
aws sns list-topics | grep genai-demo
```

## 📚 Next Steps

### 1. Update CI/CD Pipelines

```yaml
# Update your CI/CD to use consolidated deployment
deploy:
  script:
    - cd infrastructure
    - npm run deploy:prod
```

### 2. Update Documentation

- Update deployment runbooks
- Update team documentation
- Update monitoring procedures

### 3. Clean Up Old Resources

```bash
# Remove deprecated analytics deployment files (optional)
# git rm bin/analytics.ts

# Update .gitignore if needed
echo "# Deprecated deployment files" >> .gitignore
echo "bin/analytics.js" >> .gitignore
```

## 🎉 Migration Complete

After successful migration, you'll have:

✅ **Unified Infrastructure**: Single deployment command  
✅ **Better Dependencies**: Proper stack dependency management  
✅ **Shared Resources**: Efficient resource utilization  
✅ **Consistent Configuration**: Single configuration system  
✅ **Enhanced Monitoring**: Integrated alerting and observability  

Your infrastructure is now consolidated and ready for production! 🚀
