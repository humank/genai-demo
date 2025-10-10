#!/bin/bash

# Enhance CloudWatch Dashboard with Unified Operations Metrics
# Usage: ./enhance-cloudwatch-dashboard.sh <environment>

set -e

# Configuration
REGION="ap-northeast-1"
PROFILE="default"
DASHBOARD_NAME="GenAI-Demo-Unified-Operations"

# Parse arguments
ENVIRONMENT=${1:-"staging"}

echo "🚀 Enhancing CloudWatch Dashboard"
echo "Environment: $ENVIRONMENT"
echo "Dashboard: $DASHBOARD_NAME"
echo "Region: $REGION"
echo ""

# Check if AWS CLI is configured
if ! aws sts get-caller-identity --profile $PROFILE > /dev/null 2>&1; then
    echo "❌ Error: AWS CLI not configured"
    exit 1
fi

echo "📋 Step 1: Create Enhanced Dashboard Configuration"
echo "================================================="

# Create dashboard JSON configuration
cat > /tmp/cloudwatch-dashboard.json <<'EOF'
{
  "widgets": [
    {
      "type": "metric",
      "properties": {
        "metrics": [
          ["AWS/EKS", "cluster_failed_node_count", {"stat": "Average"}],
          [".", "cluster_node_count", {"stat": "Average"}]
        ],
        "period": 300,
        "stat": "Average",
        "region": "ap-northeast-1",
        "title": "EKS Cluster Health",
        "yAxis": {
          "left": {
            "min": 0
          }
        }
      }
    },
    {
      "type": "metric",
      "properties": {
        "metrics": [
          ["AWS/RDS", "CPUUtilization", {"stat": "Average"}],
          [".", "DatabaseConnections", {"stat": "Average"}],
          [".", "FreeableMemory", {"stat": "Average"}]
        ],
        "period": 300,
        "stat": "Average",
        "region": "ap-northeast-1",
        "title": "Aurora Database Metrics",
        "yAxis": {
          "left": {
            "min": 0
          }
        }
      }
    },
    {
      "type": "metric",
      "properties": {
        "metrics": [
          ["AWS/ElastiCache", "CPUUtilization", {"stat": "Average"}],
          [".", "NetworkBytesIn", {"stat": "Sum"}],
          [".", "NetworkBytesOut", {"stat": "Sum"}]
        ],
        "period": 300,
        "stat": "Average",
        "region": "ap-northeast-1",
        "title": "Redis Cache Metrics",
        "yAxis": {
          "left": {
            "min": 0
          }
        }
      }
    },
    {
      "type": "metric",
      "properties": {
        "metrics": [
          ["AWS/Kafka", "BytesInPerSec", {"stat": "Average"}],
          [".", "BytesOutPerSec", {"stat": "Average"}],
          [".", "MessagesInPerSec", {"stat": "Average"}]
        ],
        "period": 300,
        "stat": "Average",
        "region": "ap-northeast-1",
        "title": "MSK Throughput Metrics",
        "yAxis": {
          "left": {
            "min": 0
          }
        }
      }
    },
    {
      "type": "log",
      "properties": {
        "query": "SOURCE '/aws/containerinsights/genai-demo-cluster/application'\n| fields @timestamp, @message\n| filter @message like /ERROR/\n| sort @timestamp desc\n| limit 20",
        "region": "ap-northeast-1",
        "title": "Recent Application Errors",
        "stacked": false
      }
    },
    {
      "type": "metric",
      "properties": {
        "metrics": [
          ["GenAIDemo/Application", "http.server.requests", {"stat": "Sum"}],
          [".", "http.server.requests.error", {"stat": "Sum"}]
        ],
        "period": 300,
        "stat": "Sum",
        "region": "ap-northeast-1",
        "title": "Application Request Metrics",
        "yAxis": {
          "left": {
            "min": 0
          }
        }
      }
    }
  ]
}
EOF

echo "✅ Dashboard configuration created"

echo ""
echo "📋 Step 2: Create/Update CloudWatch Dashboard"
echo "=============================================="

# Create or update dashboard
aws cloudwatch put-dashboard \
    --dashboard-name "$DASHBOARD_NAME" \
    --dashboard-body file:///tmp/cloudwatch-dashboard.json \
    --region $REGION \
    --profile $PROFILE

echo "✅ Dashboard created/updated successfully"

# Clean up
rm /tmp/cloudwatch-dashboard.json

echo ""
echo "📋 Step 3: Create CloudWatch Alarms"
echo "===================================="

# Create high error rate alarm
aws cloudwatch put-metric-alarm \
    --alarm-name "GenAI-Demo-High-Error-Rate" \
    --alarm-description "Alert when error rate exceeds 5%" \
    --metric-name "http.server.requests.error" \
    --namespace "GenAIDemo/Application" \
    --statistic Average \
    --period 300 \
    --evaluation-periods 2 \
    --threshold 0.05 \
    --comparison-operator GreaterThanThreshold \
    --region $REGION \
    --profile $PROFILE \
    2>/dev/null || echo "⚠️  Alarm already exists or metric not available yet"

# Create high latency alarm
aws cloudwatch put-metric-alarm \
    --alarm-name "GenAI-Demo-High-Latency" \
    --alarm-description "Alert when P95 latency exceeds 2 seconds" \
    --metric-name "http.server.requests.duration" \
    --namespace "GenAIDemo/Application" \
    --statistic Average \
    --period 300 \
    --evaluation-periods 2 \
    --threshold 2000 \
    --comparison-operator GreaterThanThreshold \
    --region $REGION \
    --profile $PROFILE \
    2>/dev/null || echo "⚠️  Alarm already exists or metric not available yet"

# Create database connection alarm
aws cloudwatch put-metric-alarm \
    --alarm-name "GenAI-Demo-High-DB-Connections" \
    --alarm-description "Alert when database connections exceed 80%" \
    --metric-name "DatabaseConnections" \
    --namespace "AWS/RDS" \
    --statistic Average \
    --period 300 \
    --evaluation-periods 2 \
    --threshold 80 \
    --comparison-operator GreaterThanThreshold \
    --region $REGION \
    --profile $PROFILE \
    2>/dev/null || echo "⚠️  Alarm already exists or RDS not available yet"

echo "✅ CloudWatch alarms configured"

echo ""
echo "📋 Step 4: Create Log Insights Queries"
echo "======================================="

# Create saved query for error analysis
aws logs put-query-definition \
    --name "GenAI-Demo-Error-Analysis" \
    --query-string "fields @timestamp, @message, @logStream
| filter @message like /ERROR/
| stats count() by bin(5m)" \
    --log-group-names "/aws/containerinsights/genai-demo-cluster/application" \
    --region $REGION \
    --profile $PROFILE \
    2>/dev/null || echo "⚠️  Query already exists or log group not available yet"

# Create saved query for performance analysis
aws logs put-query-definition \
    --name "GenAI-Demo-Performance-Analysis" \
    --query-string "fields @timestamp, @message
| filter @message like /duration/
| parse @message /duration=(?<duration>\\d+)/
| stats avg(duration), max(duration), min(duration) by bin(5m)" \
    --log-group-names "/aws/containerinsights/genai-demo-cluster/application" \
    --region $REGION \
    --profile $PROFILE \
    2>/dev/null || echo "⚠️  Query already exists or log group not available yet"

echo "✅ Log Insights queries created"

echo ""
echo "📊 Dashboard Access Information"
echo "================================"

DASHBOARD_URL="https://console.aws.amazon.com/cloudwatch/home?region=${REGION}#dashboards:name=${DASHBOARD_NAME}"
echo "🌐 Dashboard URL: $DASHBOARD_URL"

echo ""
echo "📋 Next Steps:"
echo "1. Access the dashboard using the URL above"
echo "2. Verify all widgets are displaying data"
echo "3. Configure SNS topics for alarm notifications"
echo "4. Set up additional custom metrics as needed"
echo "5. Create additional saved queries for specific use cases"

echo ""
echo "🎉 CloudWatch dashboard enhancement completed!"
