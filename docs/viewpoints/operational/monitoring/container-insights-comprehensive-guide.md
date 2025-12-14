# Container Insights Comprehensive Monitoring Guide

**Last Updated**: 2025-10-22  
**Status**: ✅ Fully Implemented  
**Requirements**: 13.1, 13.2, 13.3

## Overview

This guide provides comprehensive documentation for CloudWatch Container Insights monitoring deployed on the EKS cluster. Container Insights provides detailed metrics and logs for all containers, pods, and nodes in the cluster, with automated anomaly detection and root cause analysis.

## Architecture

### Components

```
┌─────────────────────────────────────────────────────────────┐
│                    EKS Cluster                              │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ CloudWatch   │  │  Fluent Bit  │  │ Application  │     │
│  │ Agent        │  │  DaemonSet   │  │    Pods      │     │
│  │ DaemonSet    │  │              │  │              │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│              CloudWatch Container Insights                  │
├─────────────────────────────────────────────────────────────┤
│  • Performance Metrics                                      │
│  • Application Logs                                         │
│  • Dataplane Logs                                          │
│  • Host Logs                                               │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│           Automated Analysis & Alerting                     │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ Pod Resource │  │  Container   │  │   Network    │     │
│  │   Anomaly    │  │   Restart    │  │    Error     │     │
│  │  Detection   │  │   Analysis   │  │  Detection   │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

## Features

### 1. Comprehensive Metrics Collection (Requirement 13.1)

Container Insights collects the following metrics:

#### Pod-Level Metrics
- **CPU Utilization**: `pod_cpu_utilization` - Percentage of CPU used by pod
- **Memory Utilization**: `pod_memory_utilization` - Percentage of memory used by pod
- **Network RX/TX**: `pod_network_rx_bytes`, `pod_network_tx_bytes` - Network traffic
- **Network Errors**: `pod_network_rx_errors`, `pod_network_tx_errors` - Network error counts
- **Container Restarts**: `pod_number_of_container_restarts` - Number of container restarts

#### Node-Level Metrics
- **CPU Utilization**: `node_cpu_utilization` - Node CPU usage
- **Memory Utilization**: `node_memory_utilization` - Node memory usage
- **Disk Usage**: `node_filesystem_utilization` - Disk space usage
- **Network Performance**: `node_network_total_bytes` - Total network throughput

#### Cluster-Level Metrics
- **Node Count**: `cluster_node_count` - Total number of nodes
- **Failed Node Count**: `cluster_failed_node_count` - Number of failed nodes
- **Pod Count**: `cluster_number_of_running_pods` - Total running pods

### 2. Automated Anomaly Detection (Requirement 13.2)

CloudWatch alarms automatically detect and alert on resource anomalies:

#### High CPU Utilization Alarm
- **Metric**: `pod_cpu_utilization`
- **Threshold**: 80%
- **Evaluation**: 2 consecutive periods of 5 minutes
- **Action**: SNS notification to critical alert topic

#### High Memory Utilization Alarm
- **Metric**: `pod_memory_utilization`
- **Threshold**: 85%
- **Evaluation**: 2 consecutive periods of 5 minutes
- **Action**: SNS notification to critical alert topic

#### Network Error Alarm
- **Metric**: `pod_network_rx_errors`
- **Threshold**: 10 errors in 5 minutes
- **Evaluation**: 1 period
- **Action**: SNS notification to warning alert topic

#### Container Restart Rate Alarm
- **Metric**: `pod_number_of_container_restarts`
- **Threshold**: 5 restarts in 10 minutes
- **Evaluation**: 1 period
- **Action**: SNS notification to critical alert topic

### 3. Automated Container Restart Analysis (Requirement 13.3)

A Lambda function automatically analyzes container restart events every 15 minutes:

#### Analysis Process
1. **Query CloudWatch Logs Insights** for restart-related events:
   - OOMKilled events
   - CrashLoopBackOff events
   - Application errors and exceptions

2. **Root Cause Determination**:
   - **Out of Memory**: Container exceeded memory limits
   - **Application Crash**: Application-level errors
   - **Configuration Issues**: Missing environment variables or config

3. **Event Chain Recording**:
   - Pod name and namespace
   - Container name
   - Timestamp of restart
   - Root cause analysis
   - Log snippet for context

4. **Metrics Publishing**:
   - Custom metric `ContainerRestartEvents` published to CloudWatch
   - Enables trending and historical analysis

## Log Groups

Container Insights creates the following log groups:

### Performance Metrics
- **Log Group**: `/aws/containerinsights/{environment}-genai-demo/performance`
- **Retention**: 14 days
- **Content**: Pod and node performance metrics

### Application Logs
- **Log Group**: `/aws/containerinsights/{environment}-genai-demo/application`
- **Retention**: 14 days
- **Content**: Application stdout/stderr logs

### Dataplane Logs
- **Log Group**: `/aws/containerinsights/{environment}-genai-demo/dataplane`
- **Retention**: 14 days
- **Content**: Kubernetes dataplane logs

### Host Logs
- **Log Group**: `/aws/containerinsights/{environment}-genai-demo/host`
- **Retention**: 14 days
- **Content**: Node-level system logs

## Deployment

### Prerequisites
- EKS cluster running
- IAM roles configured with Container Insights permissions
- CloudWatch Agent and Fluent Bit DaemonSets deployed

### Deployment Script
```bash
# Deploy Container Insights
./infrastructure/scripts/deploy-container-insights.sh <environment> <cluster-name>

# Verify deployment
./infrastructure/scripts/verify-container-insights.sh <environment> <cluster-name>
```

### Manual Deployment Steps

1. **Enable Container Insights on EKS Cluster**:
```bash
aws eks update-cluster-config \
    --name <cluster-name> \
    --logging '{"clusterLogging":[{"types":["api","audit","authenticator","controllerManager","scheduler"],"enabled":true}]}'
```

2. **Deploy CloudWatch Agent DaemonSet**:
```bash
kubectl apply -f infrastructure/k8s/monitoring/cloudwatch-agent-daemonset.yaml
```

3. **Deploy Fluent Bit DaemonSet**:
```bash
kubectl apply -f infrastructure/k8s/monitoring/fluent-bit-daemonset.yaml
```

4. **Verify Deployment**:
```bash
kubectl get daemonset -n amazon-cloudwatch
kubectl get pods -n amazon-cloudwatch
```

## Monitoring and Dashboards

### CloudWatch Console

Access Container Insights dashboard:
```
https://console.aws.amazon.com/cloudwatch/home?region=<region>#container-insights:infrastructure
```

### Key Metrics to Monitor

1. **Pod CPU Utilization**:
   - Normal: < 70%
   - Warning: 70-80%
   - Critical: > 80%

2. **Pod Memory Utilization**:
   - Normal: < 75%
   - Warning: 75-85%
   - Critical: > 85%

3. **Container Restart Rate**:
   - Normal: 0-2 restarts/hour
   - Warning: 3-5 restarts/hour
   - Critical: > 5 restarts/hour

4. **Network Errors**:
   - Normal: 0-5 errors/5min
   - Warning: 6-10 errors/5min
   - Critical: > 10 errors/5min

## Troubleshooting

### High CPU Utilization

**Symptoms**:
- Pod CPU utilization > 80%
- Application slowness
- Request timeouts

**Investigation Steps**:
1. Check CloudWatch Logs for application errors
2. Review pod resource requests and limits
3. Analyze CPU-intensive operations in application logs
4. Check for infinite loops or inefficient algorithms

**Resolution**:
- Increase CPU limits in pod specification
- Optimize application code
- Scale horizontally with more pod replicas

### High Memory Utilization

**Symptoms**:
- Pod memory utilization > 85%
- OOMKilled events
- Pod restarts

**Investigation Steps**:
1. Check for memory leaks in application logs
2. Review memory allocation patterns
3. Analyze heap dumps if available
4. Check for large object retention

**Resolution**:
- Increase memory limits in pod specification
- Fix memory leaks in application code
- Implement proper resource cleanup
- Use memory profiling tools

### Container Restart Loop

**Symptoms**:
- Frequent container restarts (> 5 in 10 minutes)
- CrashLoopBackOff status
- Application unavailability

**Investigation Steps**:
1. Check container restart analysis Lambda logs
2. Review application logs for errors
3. Verify environment variables and configuration
4. Check liveness and readiness probe configuration

**Resolution**:
- Fix application startup errors
- Adjust probe timing and thresholds
- Verify configuration and secrets
- Check resource limits

### Network Errors

**Symptoms**:
- High network error rate
- Connection timeouts
- Service unavailability

**Investigation Steps**:
1. Check network error metrics in Container Insights
2. Review VPC Flow Logs
3. Verify security group rules
4. Check DNS resolution

**Resolution**:
- Fix network configuration issues
- Adjust security group rules
- Verify service mesh configuration
- Check load balancer health

## Best Practices

### Resource Limits
- Always set CPU and memory limits for pods
- Use resource requests to guarantee minimum resources
- Monitor actual usage and adjust limits accordingly

### Logging
- Use structured logging (JSON format)
- Include correlation IDs for request tracing
- Log at appropriate levels (ERROR, WARN, INFO, DEBUG)
- Avoid logging sensitive information

### Monitoring
- Set up alerts for critical metrics
- Review Container Insights dashboard regularly
- Analyze restart patterns weekly
- Tune alert thresholds based on actual usage

### Performance Optimization
- Use horizontal pod autoscaling (HPA)
- Implement proper health checks
- Optimize application startup time
- Use readiness probes to prevent premature traffic

## Integration with Other Services

### X-Ray Distributed Tracing
- Container Insights metrics complement X-Ray traces
- Use both for complete observability
- Correlate high CPU/memory with slow traces

### Grafana Dashboards
- Import Container Insights metrics into Grafana
- Create custom dashboards for specific use cases
- Combine with Prometheus metrics for unified view

### AWS Systems Manager
- Use Systems Manager for automated remediation
- Create runbooks for common issues
- Integrate with incident management

## Cost Optimization

### Log Retention
- Default retention: 14 days
- Adjust based on compliance requirements
- Use S3 archival for long-term storage

### Metrics Collection
- Filter unnecessary metrics
- Use metric filters to reduce data volume
- Aggregate metrics at appropriate intervals

### Lambda Function
- Restart analysis runs every 15 minutes
- Adjust frequency based on cluster size
- Monitor Lambda costs and optimize if needed

## Security Considerations

### IAM Permissions
- Use least privilege principle
- Separate roles for different components
- Regularly audit IAM policies

### Log Access
- Restrict access to CloudWatch Logs
- Use IAM policies for fine-grained control
- Enable CloudTrail for audit logging

### Data Protection
- Logs may contain sensitive information
- Implement log scrubbing if needed
- Use encryption at rest for log groups

## Compliance and Auditing

### Audit Trail
- All Container Insights actions logged to CloudTrail
- Log access tracked and auditable
- Alarm state changes recorded

### Compliance Requirements
- Meets SOC 2 monitoring requirements
- Supports ISO 27001 compliance
- Enables GDPR data protection monitoring

## References

- [AWS Container Insights Documentation](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/ContainerInsights.html)
- [EKS Best Practices Guide](https://aws.github.io/aws-eks-best-practices/)
- [CloudWatch Logs Insights Query Syntax](https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/CWL_QuerySyntax.html)

## Support

For issues or questions:
1. Check CloudWatch Logs for error messages
2. Review this guide for troubleshooting steps
3. Contact DevOps team via Slack #devops-support
4. Create incident ticket for critical issues

---

**Document Owner**: DevOps Team  
**Review Cycle**: Quarterly  
**Next Review**: 2026-01-22
