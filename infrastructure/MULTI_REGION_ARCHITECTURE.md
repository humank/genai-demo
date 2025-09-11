# Multi-Region Infrastructure Architecture

This document describes the multi-region disaster recovery infrastructure implementation for the GenAI Demo project.

## Overview

The multi-region infrastructure provides active-active disaster recovery capabilities across Taiwan (ap-east-2) and Tokyo (ap-northeast-1) regions, ensuring business continuity with minimal downtime and zero data loss.

## Architecture Components

### 1. Multi-Region Stack (`MultiRegionStack`)

**Purpose**: Coordinates cross-region infrastructure and failover mechanisms.

**Key Features**:

- Cross-region VPC peering for Taiwan-Tokyo connectivity
- Route 53 health checks for multi-region failover
- Cross-region certificate replication strategy
- CloudFormation stack dependencies between regions

**Configuration**:

```typescript
// Enabled only for production environment
if (environment === 'production' && multiRegionConfig['enable-dr']) {
  // Deploy multi-region coordination
}
```

### 2. Disaster Recovery Stack (`DisasterRecoveryStack`)

**Purpose**: Deploys complete infrastructure in secondary region (Tokyo).

**Key Features**:

- Complete network infrastructure (VPC, subnets, security groups)
- Application Load Balancer with SSL termination
- ACM certificates for DR domain (`dr.kimkao.io`)
- CloudWatch monitoring dashboard
- Systems Manager parameter store for configuration
- Cross-region replication setup

**Resource Sizing**:

- DR region uses 50% of primary region's minimum nodes
- DR region uses 80% of primary region's maximum nodes
- Always enables Multi-AZ for RDS in DR region

### 3. Route 53 Failover Stack (`Route53FailoverStack`)

**Purpose**: Manages DNS failover and health checks.

**Key Features**:

- Health checks for both primary and secondary regions
- Failover DNS records (PRIMARY/SECONDARY)
- Latency-based routing for optimal performance
- CloudWatch alarms for health check failures
- SNS notifications for failover events

**Health Check Configuration**:

- Endpoint: `/actuator/health`
- Protocol: HTTPS (port 443)
- Interval: 30 seconds (configurable)
- Failure threshold: 3 consecutive failures (configurable)

## Configuration

### CDK Context Configuration

```json
{
  "genai-demo:regions": {
    "primary": "ap-east-2",
    "secondary": "ap-northeast-1",
    "regions": {
      "ap-east-2": {
        "name": "Taiwan",
        "type": "primary",
        "cost-optimization": {
          "spot-instances": false,
          "reserved-instances": true
        },
        "backup-retention": {
          "rds": 30,
          "logs": 90
        }
      },
      "ap-northeast-1": {
        "name": "Tokyo",
        "type": "secondary",
        "cost-optimization": {
          "spot-instances": false,
          "reserved-instances": true
        },
        "backup-retention": {
          "rds": 30,
          "logs": 90
        }
      }
    }
  },
  "genai-demo:multi-region": {
    "enable-dr": true,
    "enable-cross-region-peering": true,
    "enable-cross-region-replication": true,
    "failover-rto-minutes": 1,
    "failover-rpo-minutes": 0,
    "health-check-interval": 30,
    "health-check-failure-threshold": 3
  }
}
```

### Environment-Specific Configuration

```json
{
  "genai-demo:environments": {
    "production": {
      "vpc-cidr": "10.2.0.0/16",
      "nat-gateways": 3,
      "eks-node-type": "m6g.large",
      "eks-min-nodes": 2,
      "eks-max-nodes": 10
    },
    "production-dr": {
      "vpc-cidr": "10.3.0.0/16",
      "nat-gateways": 3,
      "eks-node-type": "m6g.large",
      "eks-min-nodes": 1,
      "eks-max-nodes": 8
    }
  }
}
```

## Deployment Strategy

### Conditional Deployment

Multi-region infrastructure is deployed only when:

1. Environment is `production`
2. `genai-demo:multi-region.enable-dr` is `true`

### Stack Dependencies

```
Primary Stack (ap-east-2)
    ↓
DR Stack (ap-northeast-1)
    ↓
Multi-Region Stack (ap-east-2)
```

### Deployment Command

```bash
# Deploy to production with multi-region DR
cdk deploy --context genai-demo:environment=production \
           --context genai-demo:domain=kimkao.io \
           --all
```

## DNS and Failover Configuration

### Primary Endpoints

- `api.kimkao.io` - Failover endpoint (PRIMARY)
- `api-latency.kimkao.io` - Latency-based routing

### DR Endpoints

- `api.kimkao.io` - Failover endpoint (SECONDARY)
- `dr.kimkao.io` - Direct DR access
- `api-dr.kimkao.io` - DR-specific endpoint

### Failover Behavior

1. **Normal Operation**: Traffic routes to primary region (Taiwan)
2. **Primary Failure**: Route 53 detects health check failure
3. **Automatic Failover**: Traffic automatically routes to secondary region (Tokyo)
4. **Recovery**: When primary region recovers, traffic routes back automatically

### RTO/RPO Targets

- **RTO (Recovery Time Objective)**: 1 minute
- **RPO (Recovery Point Objective)**: 0 minutes (zero data loss)

## Monitoring and Alerting

### CloudWatch Dashboards

1. **Multi-Region Monitoring Dashboard**
   - Health check status for both regions
   - VPC peering connection status
   - Cross-region replication metrics

2. **Failover Monitoring Dashboard**
   - Health check response times
   - Failover events timeline
   - DNS query patterns

3. **DR Monitoring Dashboard**
   - DR region infrastructure health
   - Application Load Balancer metrics
   - Certificate validation status

### CloudWatch Alarms

1. **Primary Health Check Failure**
   - Triggers failover to secondary region
   - Sends SNS notifications

2. **Secondary Health Check Failure**
   - Indicates both regions are down
   - Sends critical alerts

3. **VPC Peering Connection Status**
   - Monitors cross-region connectivity
   - Alerts on connection failures

### SNS Topics

1. **Multi-Region Alerts**: `genai-demo-production-primary-multi-region-alerts`
2. **Failover Alerts**: `genai-demo-production-failover-alerts`
3. **DR Replication Notifications**: `genai-demo-production-dr-replication-notifications`

## Security Considerations

### Network Security

- **VPC Peering**: Secure cross-region communication
- **Security Groups**: Least privilege access between regions
- **Private Subnets**: Database and application tiers isolated

### Certificate Management

- **ACM Certificates**: Automatic renewal and validation
- **Cross-Region Replication**: Certificates available in both regions
- **DNS Validation**: Secure certificate validation process

### Access Control

- **IAM Roles**: Region-specific access controls
- **Systems Manager**: Secure parameter storage
- **CloudTrail**: Audit trail for all operations

## Cost Optimization

### Resource Sizing

- **DR Region**: 50-80% of primary region capacity
- **Reserved Instances**: Cost optimization for production workloads
- **Spot Instances**: Disabled for production reliability

### Data Transfer

- **VPC Peering**: Reduced data transfer costs
- **Regional Optimization**: Minimize cross-region data transfer

## Operational Procedures

### Deployment

1. Deploy primary region infrastructure
2. Deploy DR region infrastructure
3. Configure multi-region coordination
4. Validate health checks and failover

### Monitoring

1. Monitor health check status
2. Review failover metrics
3. Validate cross-region replication
4. Test disaster recovery procedures

### Maintenance

1. Coordinate maintenance windows across regions
2. Test failover procedures monthly
3. Update certificates before expiration
4. Review and update RTO/RPO targets

## Testing and Validation

### Health Check Testing

```bash
# Test primary region health check
curl -k https://api.kimkao.io/actuator/health

# Test DR region health check
curl -k https://api-dr.kimkao.io/actuator/health
```

### Failover Testing

1. **Planned Failover**: Disable primary region health check
2. **Validate Failover**: Confirm traffic routes to DR region
3. **Recovery Testing**: Re-enable primary region
4. **Validate Recovery**: Confirm traffic routes back to primary

### DNS Resolution Testing

```bash
# Test failover DNS resolution
dig api.kimkao.io

# Test latency-based DNS resolution
dig api-latency.kimkao.io
```

## Troubleshooting

### Common Issues

1. **Health Check Failures**
   - Verify application health endpoint
   - Check security group rules
   - Validate certificate configuration

2. **DNS Resolution Issues**
   - Check Route 53 health check status
   - Verify DNS record configuration
   - Validate TTL settings

3. **Cross-Region Connectivity**
   - Verify VPC peering connection status
   - Check route table configuration
   - Validate security group rules

### Monitoring Commands

```bash
# Check health check status
aws route53 get-health-check --health-check-id <health-check-id>

# Check VPC peering status
aws ec2 describe-vpc-peering-connections --vpc-peering-connection-ids <peering-id>

# Check CloudWatch alarms
aws cloudwatch describe-alarms --alarm-names <alarm-name>
```

## Future Enhancements

### Planned Improvements

1. **Automated Failover Testing**: Monthly automated DR tests
2. **Enhanced Monitoring**: Additional metrics and dashboards
3. **Cost Optimization**: Dynamic resource scaling based on demand
4. **Security Enhancements**: Additional security controls and monitoring

### Scalability Considerations

1. **Additional Regions**: Support for more than two regions
2. **Global Load Balancing**: Enhanced traffic distribution
3. **Edge Locations**: CloudFront integration for global performance
4. **Database Replication**: Enhanced cross-region database replication

This multi-region architecture provides a robust foundation for disaster recovery while maintaining high availability and performance across geographic regions.
