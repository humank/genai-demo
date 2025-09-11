# Multi-Region Infrastructure Implementation

This document describes the complete implementation of multi-region infrastructure for the GenAI Demo project, providing active-active disaster recovery capabilities across Taiwan (ap-east-2) and Tokyo (ap-northeast-1) regions.

## Overview

The multi-region infrastructure implements:

1. **Aurora Global Database** - Zero data loss cross-region database replication
2. **MSK Cross-Region Replication** - Bidirectional event streaming with MirrorMaker 2.0
3. **Route 53 Health Checks and Failover** - Automatic DNS failover with health monitoring
4. **Cross-Region Observability** - Unified monitoring and logging across regions
5. **Infrastructure as Code** - Complete CDK-based deployment automation

## Architecture Components

### 1. Aurora Global Database

**Primary Region (Taiwan - ap-east-2):**

- Aurora PostgreSQL cluster with writer and reader instances
- Global cluster identifier: `genai-demo-production-global`
- Automated backups with 30-day retention
- Performance Insights enabled

**Secondary Region (Tokyo - ap-northeast-1):**

- Aurora PostgreSQL cluster as global cluster member
- Read-only replicas with automatic failover capability
- Cross-region replication lag < 1 second
- Automated promotion on primary region failure

**Key Features:**

- **RPO (Recovery Point Objective):** 0 seconds (zero data loss)
- **RTO (Recovery Time Objective):** < 60 seconds
- Automated failover and failback
- Global cluster management through CDK

### 2. MSK Cross-Region Replication

**MirrorMaker 2.0 Configuration:**

- Bidirectional replication between regions
- Topic pattern: `genai-demo.production.*`
- Replication factor: 3
- Tasks per connector: 2

**Deployment Architecture:**

- ECS Fargate service running MirrorMaker 2.0
- Application Load Balancer for health checks
- IAM authentication for MSK clusters
- CloudWatch monitoring and alerting

**Replicated Topics:**

- `genai-demo.production.customer.created`
- `genai-demo.production.customer.updated`
- `genai-demo.production.order.created`
- `genai-demo.production.order.confirmed`
- `genai-demo.production.payment.processed`
- `genai-demo.production.inventory.reserved`

### 3. Route 53 Health Checks and Failover

**Health Check Configuration:**

- Endpoint: `/actuator/health`
- Protocol: HTTPS (port 443)
- Interval: 30 seconds
- Failure threshold: 3 consecutive failures
- Latency measurement enabled

**DNS Routing Policies:**

- **Failover Routing:** `api.kimkao.io` (PRIMARY/SECONDARY)
- **Latency-based Routing:** `api-latency.kimkao.io`
- **Direct Access:** `api-dr.kimkao.io` (secondary region)

**Failover Behavior:**

1. Primary region healthy → Traffic to Taiwan
2. Primary region fails → Automatic failover to Tokyo
3. Primary region recovers → Automatic failback to Taiwan

### 4. Cross-Region Observability

**Log Replication:**

- CloudWatch Logs → S3 cross-region replication
- Log lifecycle: Hot (7 days) → Warm (30 days) → Cold (365+ days)
- Unified search across regions

**Metrics Aggregation:**

- Cross-region CloudWatch dashboards
- Unified monitoring for both regions
- Replication lag monitoring
- Health status correlation

**Alerting:**

- SNS topics in both regions
- Cross-region alert correlation
- Failover event notifications
- Replication lag alerts

## Deployment Guide

### Prerequisites

1. **AWS CLI configured** with appropriate permissions
2. **AWS CDK installed** (`npm install -g aws-cdk`)
3. **Node.js 18+** for CDK application
4. **Domain configured** in Route 53 (optional but recommended)

### Quick Deployment

```bash
# Clone the repository
git clone <repository-url>
cd genai-demo/infrastructure

# Install dependencies
npm install

# Deploy multi-region infrastructure
./scripts/deploy-multi-region.sh --environment production --domain kimkao.io
```

### Custom Deployment

```bash
# Deploy with custom regions
./scripts/deploy-multi-region.sh \
  --environment production \
  --domain example.com \
  --primary-region us-east-1 \
  --secondary-region us-west-2

# Dry run to see what would be deployed
./scripts/deploy-multi-region.sh --dry-run --domain example.com

# Deploy without confirmation prompts
./scripts/deploy-multi-region.sh --deploy-all --domain example.com
```

### Manual CDK Deployment

```bash
# Set CDK context
export CDK_CONTEXT="--context genai-demo:environment=production --context genai-demo:domain=kimkao.io"

# Bootstrap both regions
cdk bootstrap aws://ACCOUNT/ap-east-2
cdk bootstrap aws://ACCOUNT/ap-northeast-1

# Deploy all stacks
cdk deploy $CDK_CONTEXT --app "node bin/multi-region-deployment.js" --all
```

## Configuration

### Environment Variables

```bash
# Required
export CDK_DEFAULT_ACCOUNT="123456789012"
export AWS_DEFAULT_REGION="ap-east-2"

# Optional
export GENAI_DEMO_DOMAIN="kimkao.io"
export GENAI_DEMO_ENVIRONMENT="production"
```

### CDK Context Configuration

The deployment uses the existing `cdk.context.json` configuration:

```json
{
  "genai-demo:regions": {
    "primary": "ap-east-2",
    "secondary": "ap-northeast-1"
  },
  "genai-demo:multi-region": {
    "enable-dr": true,
    "enable-cross-region-replication": true,
    "failover-rto-minutes": 1,
    "failover-rpo-minutes": 0,
    "health-check-interval": 30,
    "health-check-failure-threshold": 3
  }
}
```

## Monitoring and Alerting

### CloudWatch Dashboards

1. **Unified Observability Dashboard**
   - Cross-region application health
   - Database performance metrics
   - MSK throughput and lag
   - Replication status

2. **Failover Monitoring Dashboard**
   - Route 53 health check status
   - DNS query patterns
   - Failover events timeline

3. **Replication Monitoring Dashboard**
   - MirrorMaker service health
   - Replication lag metrics
   - Throughput statistics

### Key Metrics

- **Database Replication Lag:** < 1000ms (alert if > 5000ms)
- **MSK Replication Lag:** < 300000ms (alert if > 300000ms)
- **Health Check Response Time:** < 2000ms (alert if > 5000ms)
- **Application Response Time:** < 500ms (alert if > 2000ms)

### Alerting Thresholds

```yaml
Critical Alerts:
  - Both regions unhealthy
  - Database replication stopped
  - MSK replication lag > 5 minutes
  - Application errors > 5%

Warning Alerts:
  - Single region unhealthy
  - Replication lag > 1 minute
  - High resource utilization
  - Certificate expiration < 30 days
```

## Disaster Recovery Procedures

### Automated Failover

The system automatically handles:

1. **DNS Failover:** Route 53 health checks trigger automatic failover
2. **Database Failover:** Aurora Global Database promotes secondary cluster
3. **Application Traffic:** Load balancers route to healthy region
4. **Event Processing:** MirrorMaker continues bidirectional replication

### Manual Failover Testing

```bash
# Test primary region failure
aws route53 change-resource-record-sets --hosted-zone-id Z123456789 \
  --change-batch file://failover-test.json

# Monitor failover process
aws logs tail /aws/route53/healthchecks --follow

# Restore primary region
aws route53 change-resource-record-sets --hosted-zone-id Z123456789 \
  --change-batch file://failback-test.json
```

### Recovery Procedures

1. **Identify Failed Components**
   - Check CloudWatch dashboards
   - Review health check status
   - Analyze application logs

2. **Assess Data Consistency**
   - Verify Aurora replication status
   - Check MSK topic lag
   - Validate application state

3. **Execute Recovery**
   - Fix infrastructure issues
   - Restore failed services
   - Verify health checks pass

4. **Validate Recovery**
   - Test application functionality
   - Verify data consistency
   - Monitor for issues

## Cost Optimization

### Resource Sizing

**Primary Region (Taiwan):**

- Aurora: db.r6g.large (writer + reader)
- MSK: 3 x kafka.m5.large brokers
- EKS: 2-10 m6g.large nodes
- MirrorMaker: 1 vCPU, 2GB RAM

**Secondary Region (Tokyo):**

- Aurora: db.r6g.large (reader only)
- MSK: 3 x kafka.m5.large brokers
- EKS: 1-8 m6g.large nodes
- MirrorMaker: 1 vCPU, 2GB RAM

### Estimated Monthly Costs

```yaml
Primary Region (Taiwan):
  Aurora Global Database: ~$400
  MSK Cluster: ~$300
  EKS Cluster: ~$200
  Data Transfer: ~$100
  Total: ~$1,000

Secondary Region (Tokyo):
  Aurora Global Database: ~$200
  MSK Cluster: ~$300
  EKS Cluster: ~$150
  Data Transfer: ~$100
  Total: ~$750

Cross-Region Services:
  Route 53 Health Checks: ~$10
  Data Transfer: ~$50
  CloudWatch: ~$40
  Total: ~$100

Grand Total: ~$1,850/month
```

### Cost Optimization Strategies

1. **Reserved Instances:** 70% savings on compute
2. **Spot Instances:** Not recommended for production
3. **Right-sizing:** Monitor and adjust instance sizes
4. **Data Lifecycle:** Automated log archival to reduce storage costs

## Security Considerations

### Network Security

- **VPC Isolation:** Private subnets for databases and MSK
- **Security Groups:** Least privilege access rules
- **VPC Peering:** Secure cross-region communication
- **TLS Encryption:** All data in transit encrypted

### Data Encryption

- **At Rest:** KMS encryption for all data stores
- **In Transit:** TLS 1.2+ for all communications
- **Key Management:** Separate KMS keys per region
- **Rotation:** Automatic key rotation enabled

### Access Control

- **IAM Roles:** Service-specific roles with minimal permissions
- **MSK Authentication:** IAM-based authentication
- **Database Access:** Secrets Manager for credentials
- **API Access:** Certificate-based authentication

## Troubleshooting

### Common Issues

1. **Health Check Failures**

   ```bash
   # Check application health
   curl -k https://api.kimkao.io/actuator/health
   
   # Check Route 53 health check status
   aws route53 get-health-check --health-check-id HCHECKID
   ```

2. **Database Replication Lag**

   ```bash
   # Check Aurora replication status
   aws rds describe-db-clusters --db-cluster-identifier genai-demo-production-primary-aurora
   
   # Monitor replication metrics
   aws cloudwatch get-metric-statistics --namespace AWS/RDS --metric-name AuroraReplicaLag
   ```

3. **MSK Replication Issues**

   ```bash
   # Check MirrorMaker service status
   aws ecs describe-services --cluster genai-demo-production-mirrormaker
   
   # View MirrorMaker logs
   aws logs tail /aws/ecs/mirrormaker/genai-demo-production --follow
   ```

### Debug Commands

```bash
# Check stack status
cdk list --app "node bin/multi-region-deployment.js"

# View stack outputs
aws cloudformation describe-stacks --stack-name genai-demo-production-primary-core

# Test connectivity
telnet api.kimkao.io 443
telnet api-dr.kimkao.io 443

# Monitor failover
watch -n 5 'dig api.kimkao.io'
```

## Maintenance

### Regular Tasks

1. **Monthly Failover Testing**
   - Schedule maintenance windows
   - Test automated failover
   - Validate recovery procedures
   - Document any issues

2. **Certificate Management**
   - Monitor certificate expiration
   - Validate DNS validation
   - Test certificate renewal

3. **Performance Monitoring**
   - Review replication lag trends
   - Analyze health check metrics
   - Optimize resource allocation

4. **Security Updates**
   - Update ECS task definitions
   - Patch Aurora clusters
   - Review IAM policies

### Upgrade Procedures

1. **Aurora Version Upgrades**
   - Test in secondary region first
   - Use blue-green deployment
   - Monitor replication during upgrade

2. **MSK Version Upgrades**
   - Upgrade secondary cluster first
   - Validate MirrorMaker compatibility
   - Monitor replication lag

3. **Application Deployments**
   - Deploy to secondary region first
   - Validate health checks pass
   - Deploy to primary region
   - Monitor for issues

## Support and Documentation

### Additional Resources

- [AWS CDK Documentation](https://docs.aws.amazon.com/cdk/)
- [Aurora Global Database Guide](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/aurora-global-database.html)
- [MSK MirrorMaker 2.0 Guide](https://docs.aws.amazon.com/msk/latest/developerguide/msk-connect.html)
- [Route 53 Health Checks](https://docs.aws.amazon.com/Route53/latest/DeveloperGuide/health-checks-creating.html)

### Getting Help

1. **Check CloudWatch Logs** for detailed error messages
2. **Review CDK Synthesis** output for configuration issues
3. **Validate AWS Permissions** for deployment failures
4. **Test Network Connectivity** for communication issues

### Contributing

To contribute improvements to the multi-region infrastructure:

1. Fork the repository
2. Create a feature branch
3. Test changes thoroughly
4. Submit a pull request with detailed description
5. Include documentation updates

---

This implementation provides a robust, scalable, and cost-effective multi-region infrastructure that meets the requirements for zero data loss and minimal downtime disaster recovery.
