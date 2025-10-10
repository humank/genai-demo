#!/bin/bash

# Configure Amazon Managed Grafana Workspace
# Usage: ./configure-managed-grafana.sh <environment> <workspace-id>

set -e

# Configuration
REGION="ap-northeast-1"
PROFILE="default"

# Parse arguments
ENVIRONMENT=${1:-"staging"}
WORKSPACE_ID=${2}

if [ -z "$WORKSPACE_ID" ]; then
    echo "❌ Error: Workspace ID is required"
    echo "Usage: ./configure-managed-grafana.sh <environment> <workspace-id>"
    echo ""
    echo "To get workspace ID, run:"
    echo "aws grafana list-workspaces --region $REGION --profile $PROFILE"
    exit 1
fi

echo "🚀 Configuring Amazon Managed Grafana"
echo "Environment: $ENVIRONMENT"
echo "Workspace ID: $WORKSPACE_ID"
echo "Region: $REGION"
echo ""

# Check if AWS CLI is configured
if ! aws sts get-caller-identity --profile $PROFILE > /dev/null 2>&1; then
    echo "❌ Error: AWS CLI not configured"
    exit 1
fi

echo "📋 Step 1: Get Workspace Details"
echo "================================="

# Get workspace details
WORKSPACE_DETAILS=$(aws grafana describe-workspace \
    --workspace-id "$WORKSPACE_ID" \
    --region $REGION \
    --profile $PROFILE)

WORKSPACE_ENDPOINT=$(echo "$WORKSPACE_DETAILS" | jq -r '.workspace.endpoint')
WORKSPACE_STATUS=$(echo "$WORKSPACE_DETAILS" | jq -r '.workspace.status')

echo "Workspace Endpoint: $WORKSPACE_ENDPOINT"
echo "Workspace Status: $WORKSPACE_STATUS"

if [ "$WORKSPACE_STATUS" != "ACTIVE" ]; then
    echo "⚠️  Warning: Workspace is not active. Current status: $WORKSPACE_STATUS"
    echo "Please wait for workspace to become active before continuing."
    exit 1
fi

echo "✅ Workspace is active"

echo ""
echo "📋 Step 2: Configure Data Sources"
echo "=================================="

# CloudWatch data source is automatically configured
echo "✅ CloudWatch data source: Automatically configured"

# X-Ray data source is automatically configured
echo "✅ X-Ray data source: Automatically configured"

# Prometheus data source needs to be configured
echo "🔄 Configuring Prometheus data source..."

# Get Prometheus workspace endpoint
PROMETHEUS_WORKSPACES=$(aws amp list-workspaces \
    --region $REGION \
    --profile $PROFILE \
    --query "workspaces[?alias=='genai-demo-${ENVIRONMENT}-prometheus'].workspaceId" \
    --output text)

if [ -n "$PROMETHEUS_WORKSPACES" ]; then
    PROMETHEUS_WORKSPACE_ID=$(echo "$PROMETHEUS_WORKSPACES" | head -1)
    PROMETHEUS_ENDPOINT=$(aws amp describe-workspace \
        --workspace-id "$PROMETHEUS_WORKSPACE_ID" \
        --region $REGION \
        --profile $PROFILE \
        --query 'workspace.prometheusEndpoint' \
        --output text)
    
    echo "✅ Prometheus workspace found: $PROMETHEUS_WORKSPACE_ID"
    echo "   Endpoint: $PROMETHEUS_ENDPOINT"
    echo ""
    echo "   Note: Configure Prometheus data source in Grafana UI:"
    echo "   1. Go to Configuration → Data Sources"
    echo "   2. Add Prometheus data source"
    echo "   3. URL: $PROMETHEUS_ENDPOINT"
    echo "   4. Auth: SigV4 auth enabled"
    echo "   5. Region: $REGION"
else
    echo "⚠️  Prometheus workspace not found"
    echo "   Create Prometheus workspace first using CDK stack"
fi

echo ""
echo "📋 Step 3: Enable Grafana Plugins"
echo "=================================="

echo "The following plugins are enabled by default:"
echo "✅ CloudWatch plugin"
echo "✅ X-Ray plugin"
echo "✅ Prometheus plugin"

echo ""
echo "Additional recommended plugins:"
echo "- grafana-piechart-panel"
echo "- grafana-worldmap-panel"
echo "- grafana-clock-panel"
echo ""
echo "Note: Plugins can be enabled in Grafana UI under Configuration → Plugins"

echo ""
echo "📋 Step 4: Configure SSO Authentication"
echo "========================================"

echo "Amazon Managed Grafana uses AWS IAM Identity Center (SSO) for authentication."
echo ""
echo "To configure SSO:"
echo "1. Ensure AWS IAM Identity Center is enabled in your account"
echo "2. Go to AWS IAM Identity Center console"
echo "3. Add users or groups that should have access to Grafana"
echo "4. Assign users to the Grafana workspace"
echo ""
echo "User roles in Grafana:"
echo "- ADMIN: Full administrative access"
echo "- EDITOR: Can create and edit dashboards"
echo "- VIEWER: Read-only access"

# Check if SSO is configured
SSO_CONFIG=$(echo "$WORKSPACE_DETAILS" | jq -r '.workspace.authentication.providers[]' | grep -c "AWS_SSO" || true)

if [ "$SSO_CONFIG" -gt 0 ]; then
    echo "✅ AWS SSO authentication is enabled"
else
    echo "⚠️  AWS SSO authentication not configured"
    echo "   Update workspace to enable SSO:"
    echo "   aws grafana update-workspace-authentication \\"
    echo "       --workspace-id $WORKSPACE_ID \\"
    echo "       --authentication-providers AWS_SSO \\"
    echo "       --region $REGION"
fi

echo ""
echo "📋 Step 5: Configure Workspace Permissions"
echo "==========================================="

echo "To assign users to the workspace:"
echo ""
echo "# List available users from IAM Identity Center"
echo "aws sso-admin list-instances --region $REGION"
echo ""
echo "# Assign user to workspace"
echo "aws grafana update-permissions \\"
echo "    --workspace-id $WORKSPACE_ID \\"
echo "    --update-instruction-batch '[{\"action\":\"ADD\",\"role\":\"ADMIN\",\"users\":[{\"id\":\"USER_ID\",\"type\":\"SSO_USER\"}]}]' \\"
echo "    --region $REGION"

echo ""
echo "📋 Step 6: Create Initial Dashboards"
echo "====================================="

cat > /tmp/grafana-dashboard-config.json <<'EOF'
{
  "dashboard": {
    "title": "GenAI Demo - Unified Operations",
    "tags": ["genai-demo", "operations"],
    "timezone": "browser",
    "panels": [
      {
        "id": 1,
        "title": "EKS Cluster CPU",
        "type": "graph",
        "datasource": "CloudWatch",
        "targets": [
          {
            "namespace": "ContainerInsights",
            "metricName": "cluster_cpu_utilization",
            "dimensions": {
              "ClusterName": "genai-demo-staging"
            },
            "statistics": ["Average"],
            "period": 300
          }
        ]
      },
      {
        "id": 2,
        "title": "Application Response Time",
        "type": "graph",
        "datasource": "CloudWatch",
        "targets": [
          {
            "namespace": "GenAIDemo/HTTP",
            "metricName": "http.server.requests.duration",
            "statistics": ["Average", "p95", "p99"],
            "period": 300
          }
        ]
      }
    ]
  }
}
EOF

echo "✅ Dashboard configuration template created at /tmp/grafana-dashboard-config.json"
echo ""
echo "To import dashboard:"
echo "1. Access Grafana UI: https://$WORKSPACE_ENDPOINT"
echo "2. Go to Dashboards → Import"
echo "3. Upload the dashboard JSON file"
echo "4. Select appropriate data sources"

echo ""
echo "📋 Step 7: Configure Alerting"
echo "=============================="

echo "Amazon Managed Grafana supports alerting with SNS integration."
echo ""
echo "To configure alerting:"
echo "1. Create SNS topic for alerts"
echo "2. In Grafana UI, go to Alerting → Notification channels"
echo "3. Add SNS notification channel"
echo "4. Configure alert rules in dashboards"

# Create SNS topic for Grafana alerts
GRAFANA_ALERT_TOPIC="genai-demo-grafana-alerts-${ENVIRONMENT}"

TOPIC_ARN=$(aws sns create-topic \
    --name "$GRAFANA_ALERT_TOPIC" \
    --region $REGION \
    --profile $PROFILE \
    --output text \
    --query 'TopicArn' 2>/dev/null || \
    aws sns list-topics \
    --region $REGION \
    --profile $PROFILE \
    --query "Topics[?contains(TopicArn, '${GRAFANA_ALERT_TOPIC}')].TopicArn" \
    --output text)

echo "✅ SNS topic for Grafana alerts: $TOPIC_ARN"

# Grant Grafana permission to publish to SNS
aws sns set-topic-attributes \
    --topic-arn "$TOPIC_ARN" \
    --attribute-name Policy \
    --attribute-value "{
        \"Version\": \"2012-10-17\",
        \"Statement\": [{
            \"Effect\": \"Allow\",
            \"Principal\": {\"Service\": \"grafana.amazonaws.com\"},
            \"Action\": \"SNS:Publish\",
            \"Resource\": \"$TOPIC_ARN\",
            \"Condition\": {
                \"StringEquals\": {
                    \"aws:SourceAccount\": \"$(aws sts get-caller-identity --query Account --output text --profile $PROFILE)\"
                }
            }
        }]
    }" \
    --region $REGION \
    --profile $PROFILE \
    2>/dev/null || echo "⚠️  Failed to update SNS topic policy"

echo ""
echo "📊 Configuration Summary"
echo "========================"

echo ""
echo "Workspace Information:"
echo "- Workspace ID: $WORKSPACE_ID"
echo "- Endpoint: https://$WORKSPACE_ENDPOINT"
echo "- Status: $WORKSPACE_STATUS"
echo "- Region: $REGION"

echo ""
echo "Data Sources:"
echo "- ✅ CloudWatch (automatic)"
echo "- ✅ X-Ray (automatic)"
if [ -n "$PROMETHEUS_ENDPOINT" ]; then
    echo "- ✅ Prometheus: $PROMETHEUS_ENDPOINT"
else
    echo "- ⚠️  Prometheus: Not configured"
fi

echo ""
echo "Authentication:"
echo "- Method: AWS IAM Identity Center (SSO)"
echo "- Status: $([ "$SSO_CONFIG" -gt 0 ] && echo "Enabled" || echo "Not configured")"

echo ""
echo "Alerting:"
echo "- SNS Topic: $TOPIC_ARN"

echo ""
echo "📋 Next Steps:"
echo "1. Access Grafana: https://$WORKSPACE_ENDPOINT"
echo "2. Sign in with AWS SSO credentials"
echo "3. Configure Prometheus data source (if available)"
echo "4. Import dashboards from /tmp/grafana-dashboard-config.json"
echo "5. Configure alert notification channels"
echo "6. Assign users and set permissions"
echo "7. Create custom dashboards for your use cases"

echo ""
echo "📚 Documentation:"
echo "- Amazon Managed Grafana: https://docs.aws.amazon.com/grafana/"
echo "- Grafana Documentation: https://grafana.com/docs/"
echo "- CloudWatch Data Source: https://grafana.com/docs/grafana/latest/datasources/cloudwatch/"

echo ""
echo "🎉 Amazon Managed Grafana configuration completed!"
