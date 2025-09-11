# AWS CDK Infrastructure Troubleshooting Guide

This guide addresses common issues encountered during AWS CDK infrastructure deployment and development.

## 🔧 Common Issues and Solutions

### 1. Route53 Health Check Configuration Issues

**Problem**: Route53 health checks failing due to missing API properties or incorrect configuration.

**Symptoms**:

- Health checks showing as "Failure" in Route53 console
- Failover not working as expected
- Missing alarm integration

**Solution**:

```bash
# The Route53 health check configuration has been updated with:
# - searchString: '"status":"UP"' - looks for Spring Boot Actuator health response
# - insufficientDataHealthStatus: 'Failure' - treats missing data as failure
# - alarmIdentifier - integrates with CloudWatch alarms
# - Additional tags for better resource management
```

**Verification**:

1. Check Route53 console for health check status
2. Verify the health endpoint returns `{"status":"UP"}`
3. Monitor CloudWatch alarms for health check metrics

### 2. OpenSearch Multi-AZ Configuration Issues

**Problem**: OpenSearch domain fails to deploy with Multi-AZ when using t3.small.search instances.

**Symptoms**:

- CDK deployment fails with "UnsupportedOperation" error
- Error message about instance type not supporting Multi-AZ

**Solution**:
The configuration has been updated to use environment-specific instance types:

```typescript
// Development: Single-AZ with t3.small.search
"development": {
  "instance-type": "t3.small.search",
  "instance-count": 1,
  "multi-az": false,
  "volume-size": 20
}

// Staging/Production: Multi-AZ with m6g instances
"production": {
  "instance-type": "m6g.large.search",
  "instance-count": 3,
  "multi-az": true,
  "volume-size": 100
}
```

**Verification**:

```bash
# Check OpenSearch domain configuration
aws opensearch describe-domain --domain-name genai-demo-logs-production
```

### 3. TypeScript/ts-node Cache Issues

**Problem**: CDK synthesis fails due to stale TypeScript compilation cache or ts-node cache.

**Symptoms**:

- "Cannot find module" errors
- Outdated type definitions being used
- Inconsistent compilation results

**Solutions**:

#### Quick Cache Cleanup

```bash
npm run clean:cache
npm run build
npm run synth
```

#### Deep Cleanup

```bash
./scripts/cleanup-cache.sh --deep
npm run build
```

#### Complete Reset

```bash
./scripts/cleanup-cache.sh --reinstall
npm run build
```

#### Manual Cleanup

```bash
# Remove TypeScript build info
rm -f tsconfig.tsbuildinfo

# Remove ts-node cache
rm -rf node_modules/.cache/ts-node/

# Remove Jest cache
rm -rf .jest-cache/

# Remove CDK output
rm -rf cdk.out/

# Remove compiled files
find . -name "*.js" -not -path "./node_modules/*" -not -path "./scripts/*" -delete
find . -name "*.d.ts" -not -path "./node_modules/*" -delete
```

## 🚀 Deployment Best Practices

### Pre-deployment Checklist

1. **Clean Build Environment**:

   ```bash
   npm run clean:cache
   npm run build
   ```

2. **Validate Configuration**:

   ```bash
   npm run synth:validate
   ```

3. **Run Tests**:

   ```bash
   npm run test:ci
   ```

4. **Security Scan**:

   ```bash
   npm run security:scan
   ```

### Environment-Specific Deployment

#### Development Environment

```bash
# Clean deployment for development
npm run deploy:clean -- --context environment=development
```

#### Production Environment

```bash
# Validate before production deployment
npm run validate:comprehensive
npm run deploy -- --context environment=production --require-approval broadening
```

## 🔍 Debugging Commands

### CDK Debugging

```bash
# Synthesize with verbose output
cdk synth --verbose

# Show differences
cdk diff --context environment=production

# List all stacks
cdk list

# Show stack dependencies
cdk synth --json | jq '.[] | select(.type=="aws:cdk:tree") | .metadata'
```

### Infrastructure Validation

```bash
# Validate all templates
npm run validate:templates

# Check for drift
npm run drift:detect

# Analyze performance
npm run performance:analyze
```

## 📊 Monitoring and Alerting

### Health Check Monitoring

- **Primary Health Check**: Monitor via CloudWatch alarm `genai-demo-production-primary-health-alarm`
- **Secondary Health Check**: Monitor via CloudWatch alarm `genai-demo-production-secondary-health-alarm`
- **Dashboard**: Access failover monitoring dashboard via CDK output URL

### OpenSearch Monitoring

- **Domain Health**: Check OpenSearch domain status in AWS console
- **Cluster Metrics**: Monitor CPU, memory, and storage utilization
- **Index Health**: Verify log ingestion and search performance

### Log Analysis

```bash
# Check application logs
aws logs describe-log-groups --log-group-name-prefix "/aws/containerinsights/genai-demo-cluster"

# Query recent errors
aws logs filter-log-events --log-group-name "/aws/containerinsights/genai-demo-cluster/application" --filter-pattern "ERROR"
```

## 🛠️ Advanced Troubleshooting

### CDK Context Issues

```bash
# Clear CDK context cache
cdk context --clear

# Reset specific context values
cdk context --reset "availability-zones:account=ACCOUNT:region=REGION"
```

### Node.js Memory Issues

```bash
# Increase Node.js memory limit
export NODE_OPTIONS="--max-old-space-size=4096"
npm run build
```

### AWS Credentials Issues

```bash
# Verify AWS credentials
aws sts get-caller-identity

# Check CDK bootstrap status
cdk bootstrap --show-template
```

## 📞 Getting Help

### Log Collection for Support

```bash
# Collect comprehensive logs
npm run troubleshoot > troubleshoot-output.log 2>&1

# Generate deployment report
npm run docs:generate
npm run cost:estimate
```

### Common Error Patterns

1. **"Cannot assume role"**: Check IAM permissions and trust relationships
2. **"Resource already exists"**: Check for naming conflicts or incomplete cleanup
3. **"Insufficient capacity"**: Verify instance types are available in target AZs
4. **"Invalid parameter"**: Validate configuration values in cdk.context.json

### Support Resources

- [AWS CDK Documentation](https://docs.aws.amazon.com/cdk/)
- [AWS CDK GitHub Issues](https://github.com/aws/aws-cdk/issues)
- [AWS Support Center](https://console.aws.amazon.com/support/)

## 🔄 Recovery Procedures

### Rollback Deployment

```bash
# Rollback to previous version
cdk deploy --rollback

# Destroy and redeploy
cdk destroy --force
npm run deploy:clean
```

### Emergency Procedures

1. **Route53 Failover**: Manually update DNS records if automated failover fails
2. **OpenSearch Recovery**: Restore from automated snapshots
3. **Complete Infrastructure Reset**: Use disaster recovery procedures in DR documentation
