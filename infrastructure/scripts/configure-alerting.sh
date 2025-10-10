#!/bin/bash

# Configure Alerting and Notifications for Unified Operations
# Usage: ./configure-alerting.sh <environment> <email>

set -e

# Configuration
REGION="ap-northeast-1"
PROFILE="default"

# Parse arguments
ENVIRONMENT=${1:-"staging"}
ALERT_EMAIL=${2:-"devops@genai-demo.com"}

echo "🚀 Configuring Alerting and Notifications"
echo "Environment: $ENVIRONMENT"
echo "Alert Email: $ALERT_EMAIL"
echo "Region: $REGION"
echo ""

# Check if AWS CLI is configured
if ! aws sts get-caller-identity --profile $PROFILE > /dev/null 2>&1; then
    echo "❌ Error: AWS CLI not configured"
    exit 1
fi

echo "📋 Step 1: Create SNS Topics"
echo "============================="

# Create critical alerts topic
CRITICAL_TOPIC_ARN=$(aws sns create-topic \
    --name "genai-demo-critical-alerts-${ENVIRONMENT}" \
    --region $REGION \
    --profile $PROFILE \
    --output text \
    --query 'TopicArn' 2>/dev/null || \
    aws sns list-topics \
    --region $REGION \
    --profile $PROFILE \
    --query "Topics[?contains(TopicArn, 'genai-demo-critical-alerts-${ENVIRONMENT}')].TopicArn" \
    --output text)

echo "✅ Critical alerts topic: $CRITICAL_TOPIC_ARN"

# Create warning alerts topic
WARNING_TOPIC_ARN=$(aws sns create-topic \
    --name "genai-demo-warning-alerts-${ENVIRONMENT}" \
    --region $REGION \
    --profile $PROFILE \
    --output text \
    --query 'TopicArn' 2>/dev/null || \
    aws sns list-topics \
    --region $REGION \
    --profile $PROFILE \
    --query "Topics[?contains(TopicArn, 'genai-demo-warning-alerts-${ENVIRONMENT}')].TopicArn" \
    --output text)

echo "✅ Warning alerts topic: $WARNING_TOPIC_ARN"

# Create info alerts topic
INFO_TOPIC_ARN=$(aws sns create-topic \
    --name "genai-demo-info-alerts-${ENVIRONMENT}" \
    --region $REGION \
    --profile $PROFILE \
    --output text \
    --query 'TopicArn' 2>/dev/null || \
    aws sns list-topics \
    --region $REGION \
    --profile $PROFILE \
    --query "Topics[?contains(TopicArn, 'genai-demo-info-alerts-${ENVIRONMENT}')].TopicArn" \
    --output text)

echo "✅ Info alerts topic: $INFO_TOPIC_ARN"

echo ""
echo "📋 Step 2: Subscribe Email to Topics"
echo "====================================="

# Subscribe to critical alerts
aws sns subscribe \
    --topic-arn "$CRITICAL_TOPIC_ARN" \
    --protocol email \
    --notification-endpoint "$ALERT_EMAIL" \
    --region $REGION \
    --profile $PROFILE \
    2>/dev/null || echo "⚠️  Email already subscribed to critical alerts"

echo "✅ Subscribed $ALERT_EMAIL to critical alerts"

# Subscribe to warning alerts
aws sns subscribe \
    --topic-arn "$WARNING_TOPIC_ARN" \
    --protocol email \
    --notification-endpoint "$ALERT_EMAIL" \
    --region $REGION \
    --profile $PROFILE \
    2>/dev/null || echo "⚠️  Email already subscribed to warning alerts"

echo "✅ Subscribed $ALERT_EMAIL to warning alerts"

echo ""
echo "📋 Step 3: Configure CloudWatch Alarms with SNS"
echo "================================================"

# High error rate alarm (Critical)
aws cloudwatch put-metric-alarm \
    --alarm-name "GenAI-Demo-Critical-High-Error-Rate" \
    --alarm-description "Critical: Error rate exceeds 10%" \
    --metric-name "http.server.requests.error" \
    --namespace "GenAIDemo/Application" \
    --statistic Average \
    --period 300 \
    --evaluation-periods 2 \
    --threshold 0.10 \
    --comparison-operator GreaterThanThreshold \
    --alarm-actions "$CRITICAL_TOPIC_ARN" \
    --region $REGION \
    --profile $PROFILE \
    2>/dev/null || echo "⚠️  Alarm already exists or metric not available"

echo "✅ Critical error rate alarm configured"

# High latency alarm (Warning)
aws cloudwatch put-metric-alarm \
    --alarm-name "GenAI-Demo-Warning-High-Latency" \
    --alarm-description "Warning: P95 latency exceeds 2 seconds" \
    --metric-name "http.server.requests.duration" \
    --namespace "GenAIDemo/Application" \
    --statistic Average \
    --period 300 \
    --evaluation-periods 2 \
    --threshold 2000 \
    --comparison-operator GreaterThanThreshold \
    --alarm-actions "$WARNING_TOPIC_ARN" \
    --region $REGION \
    --profile $PROFILE \
    2>/dev/null || echo "⚠️  Alarm already exists or metric not available"

echo "✅ High latency alarm configured"

# Database CPU alarm (Critical)
aws cloudwatch put-metric-alarm \
    --alarm-name "GenAI-Demo-Critical-DB-CPU" \
    --alarm-description "Critical: Database CPU exceeds 80%" \
    --metric-name "CPUUtilization" \
    --namespace "AWS/RDS" \
    --statistic Average \
    --period 300 \
    --evaluation-periods 3 \
    --threshold 80 \
    --comparison-operator GreaterThanThreshold \
    --alarm-actions "$CRITICAL_TOPIC_ARN" \
    --region $REGION \
    --profile $PROFILE \
    2>/dev/null || echo "⚠️  Alarm already exists or RDS not available"

echo "✅ Database CPU alarm configured"

# Redis memory alarm (Warning)
aws cloudwatch put-metric-alarm \
    --alarm-name "GenAI-Demo-Warning-Redis-Memory" \
    --alarm-description "Warning: Redis memory usage exceeds 80%" \
    --metric-name "DatabaseMemoryUsagePercentage" \
    --namespace "AWS/ElastiCache" \
    --statistic Average \
    --period 300 \
    --evaluation-periods 2 \
    --threshold 80 \
    --comparison-operator GreaterThanThreshold \
    --alarm-actions "$WARNING_TOPIC_ARN" \
    --region $REGION \
    --profile $PROFILE \
    2>/dev/null || echo "⚠️  Alarm already exists or ElastiCache not available"

echo "✅ Redis memory alarm configured"

# MSK consumer lag alarm (Warning)
aws cloudwatch put-metric-alarm \
    --alarm-name "GenAI-Demo-Warning-MSK-Consumer-Lag" \
    --alarm-description "Warning: MSK consumer lag exceeds 1000 messages" \
    --metric-name "EstimatedMaxTimeLag" \
    --namespace "AWS/Kafka" \
    --statistic Maximum \
    --period 300 \
    --evaluation-periods 2 \
    --threshold 1000 \
    --comparison-operator GreaterThanThreshold \
    --alarm-actions "$WARNING_TOPIC_ARN" \
    --region $REGION \
    --profile $PROFILE \
    2>/dev/null || echo "⚠️  Alarm already exists or MSK not available"

echo "✅ MSK consumer lag alarm configured"

# Pod crash looping alarm (Critical)
aws cloudwatch put-metric-alarm \
    --alarm-name "GenAI-Demo-Critical-Pod-Crash-Looping" \
    --alarm-description "Critical: Pod is crash looping" \
    --metric-name "pod_number_of_container_restarts" \
    --namespace "ContainerInsights" \
    --statistic Sum \
    --period 300 \
    --evaluation-periods 2 \
    --threshold 3 \
    --comparison-operator GreaterThanThreshold \
    --alarm-actions "$CRITICAL_TOPIC_ARN" \
    --region $REGION \
    --profile $PROFILE \
    2>/dev/null || echo "⚠️  Alarm already exists or Container Insights not available"

echo "✅ Pod crash looping alarm configured"

echo ""
echo "📋 Step 4: Configure Grafana Alerting"
echo "======================================"

echo "Grafana alerting configuration requires manual setup:"
echo "1. Access Grafana UI"
echo "2. Navigate to Alerting → Notification channels"
echo "3. Add SNS notification channel with topic ARNs:"
echo "   - Critical: $CRITICAL_TOPIC_ARN"
echo "   - Warning: $WARNING_TOPIC_ARN"
echo "   - Info: $INFO_TOPIC_ARN"
echo "4. Configure alert rules in dashboards"

echo ""
echo "📋 Step 5: Create Alert Documentation"
echo "======================================"

cat > /tmp/alert-runbook.md <<EOF
# Alert Runbook - GenAI Demo

## Alert Severity Levels

### Critical Alerts
- **Response Time**: Immediate (< 5 minutes)
- **Notification**: Email + PagerDuty
- **Escalation**: On-call engineer

### Warning Alerts
- **Response Time**: Within 30 minutes
- **Notification**: Email + Slack
- **Escalation**: Team lead if unresolved in 1 hour

### Info Alerts
- **Response Time**: Next business day
- **Notification**: Email
- **Escalation**: None

## Alert Response Procedures

### High Error Rate (Critical)
**Symptoms**: Error rate > 10% for 10 minutes

**Investigation Steps**:
1. Check application logs in CloudWatch Logs Insights
2. Review recent deployments
3. Check database connectivity
4. Verify external service dependencies

**Resolution**:
- If deployment related: Rollback to previous version
- If database related: Check connection pool and query performance
- If external service: Enable circuit breaker or fallback

### High Latency (Warning)
**Symptoms**: P95 latency > 2 seconds for 10 minutes

**Investigation Steps**:
1. Check database query performance
2. Review cache hit rates
3. Check external API response times
4. Verify resource utilization (CPU, memory)

**Resolution**:
- Optimize slow queries
- Increase cache TTL
- Scale up resources if needed
- Enable request throttling

### Database CPU High (Critical)
**Symptoms**: Database CPU > 80% for 15 minutes

**Investigation Steps**:
1. Check Performance Insights for slow queries
2. Review active connections
3. Check for lock contention
4. Verify backup/maintenance windows

**Resolution**:
- Kill long-running queries
- Optimize slow queries
- Scale up database instance
- Enable read replicas for read-heavy workloads

### Redis Memory High (Warning)
**Symptoms**: Redis memory > 80% for 10 minutes

**Investigation Steps**:
1. Check key distribution
2. Review TTL settings
3. Identify large keys
4. Check eviction policy

**Resolution**:
- Reduce TTL for non-critical keys
- Remove unused keys
- Scale up Redis instance
- Enable cluster mode for horizontal scaling

### MSK Consumer Lag (Warning)
**Symptoms**: Consumer lag > 1000 messages for 10 minutes

**Investigation Steps**:
1. Check consumer health
2. Review processing time per message
3. Verify partition distribution
4. Check for consumer rebalancing

**Resolution**:
- Scale up consumer instances
- Optimize message processing
- Increase partition count
- Enable parallel processing

### Pod Crash Looping (Critical)
**Symptoms**: Pod restarts > 3 times in 15 minutes

**Investigation Steps**:
1. Check pod logs
2. Review resource limits
3. Verify liveness/readiness probes
4. Check for OOM kills

**Resolution**:
- Fix application errors
- Increase resource limits
- Adjust probe settings
- Enable HPA for auto-scaling

## Contact Information

- **On-Call Engineer**: [PagerDuty rotation]
- **Team Lead**: devops-lead@genai-demo.com
- **DevOps Team**: devops@genai-demo.com
- **Slack Channel**: #genai-demo-alerts

## Escalation Path

1. **Level 1**: On-call engineer (0-30 minutes)
2. **Level 2**: Team lead (30-60 minutes)
3. **Level 3**: Engineering manager (60+ minutes)
4. **Level 4**: CTO (Critical production outage)

EOF

echo "✅ Alert runbook created at /tmp/alert-runbook.md"

echo ""
echo "📊 Alerting Configuration Summary"
echo "=================================="

echo ""
echo "SNS Topics:"
echo "- Critical: $CRITICAL_TOPIC_ARN"
echo "- Warning: $WARNING_TOPIC_ARN"
echo "- Info: $INFO_TOPIC_ARN"

echo ""
echo "Email Subscriptions:"
echo "- $ALERT_EMAIL (pending confirmation)"

echo ""
echo "CloudWatch Alarms:"
echo "- GenAI-Demo-Critical-High-Error-Rate"
echo "- GenAI-Demo-Warning-High-Latency"
echo "- GenAI-Demo-Critical-DB-CPU"
echo "- GenAI-Demo-Warning-Redis-Memory"
echo "- GenAI-Demo-Warning-MSK-Consumer-Lag"
echo "- GenAI-Demo-Critical-Pod-Crash-Looping"

echo ""
echo "📋 Next Steps:"
echo "1. Confirm email subscription (check inbox)"
echo "2. Configure Grafana notification channels"
echo "3. Set up PagerDuty integration for critical alerts"
echo "4. Configure Slack webhook for team notifications"
echo "5. Review and customize alert thresholds"
echo "6. Test alert notifications"
echo "7. Document alert response procedures"

echo ""
echo "🎉 Alerting and notifications configuration completed!"
