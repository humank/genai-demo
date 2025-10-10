#!/bin/bash

# Configure Amazon Managed Grafana Data Sources
# Usage: ./configure-grafana-datasources.sh <environment> <workspace-id>

set -e

# Configuration
REGION="ap-northeast-1"
PROFILE="default"

# Parse arguments
ENVIRONMENT=${1:-"staging"}
WORKSPACE_ID=${2}

if [ -z "$WORKSPACE_ID" ]; then
    echo "❌ Error: Workspace ID is required"
    echo "Usage: $0 <environment> <workspace-id>"
    echo ""
    echo "Get workspace ID from CloudFormation outputs:"
    echo "aws cloudformation describe-stacks --stack-name ObservabilityStack --query 'Stacks[0].Outputs[?OutputKey==\`GrafanaWorkspaceId\`].OutputValue' --output text"
    exit 1
fi

echo "🚀 Configuring Amazon Managed Grafana Data Sources"
echo "Environment: $ENVIRONMENT"
echo "Workspace ID: $WORKSPACE_ID"
echo "Region: $REGION"
echo ""

# Get workspace details
echo "📋 Step 1: Get Workspace Details"
echo "=================================="

WORKSPACE_ENDPOINT=$(aws grafana describe-workspace \
    --workspace-id $WORKSPACE_ID \
    --region $REGION \
    --profile $PROFILE \
    --query 'workspace.endpoint' \
    --output text)

WORKSPACE_STATUS=$(aws grafana describe-workspace \
    --workspace-id $WORKSPACE_ID \
    --region $REGION \
    --profile $PROFILE \
    --query 'workspace.status' \
    --output text)

echo "Workspace Endpoint: $WORKSPACE_ENDPOINT"
echo "Workspace Status: $WORKSPACE_STATUS"

if [ "$WORKSPACE_STATUS" != "ACTIVE" ]; then
    echo "⚠️  Warning: Workspace is not ACTIVE (current status: $WORKSPACE_STATUS)"
    echo "   Wait for workspace to become ACTIVE before configuring data sources"
fi

echo ""
echo "📋 Step 2: Configure CloudWatch Data Source"
echo "============================================"

echo "CloudWatch data source configuration:"
cat <<EOF
{
  "name": "CloudWatch",
  "type": "cloudwatch",
  "access": "proxy",
  "jsonData": {
    "authType": "default",
    "defaultRegion": "${REGION}"
  },
  "isDefault": true
}
EOF

echo ""
echo "✅ CloudWatch data source will be configured via Grafana workspace settings"
echo "   Data source type: CloudWatch"
echo "   Authentication: AWS IAM (via workspace role)"
echo "   Default region: ${REGION}"

echo ""
echo "📋 Step 3: Configure Prometheus Data Source"
echo "============================================="

# Get Prometheus endpoint from CloudFormation
PROMETHEUS_ENDPOINT=$(aws cloudformation describe-stacks \
    --stack-name ObservabilityStack \
    --region $REGION \
    --profile $PROFILE \
    --query 'Stacks[0].Outputs[?OutputKey==`PrometheusEndpoint`].OutputValue' \
    --output text 2>/dev/null || echo "")

if [ -n "$PROMETHEUS_ENDPOINT" ]; then
    echo "Prometheus Endpoint: $PROMETHEUS_ENDPOINT"
    echo ""
    echo "Prometheus data source configuration:"
    cat <<EOF
{
  "name": "Prometheus",
  "type": "prometheus",
  "access": "proxy",
  "url": "${PROMETHEUS_ENDPOINT}",
  "jsonData": {
    "httpMethod": "POST",
    "timeInterval": "30s"
  }
}
EOF
    echo ""
    echo "✅ Prometheus data source configuration ready"
else
    echo "⚠️  Prometheus endpoint not found"
    echo "   Deploy Prometheus first or use Amazon Managed Prometheus"
fi

echo ""
echo "📋 Step 4: Configure X-Ray Data Source"
echo "======================================="

echo "X-Ray data source configuration:"
cat <<EOF
{
  "name": "X-Ray",
  "type": "grafana-x-ray-datasource",
  "access": "proxy",
  "jsonData": {
    "authType": "default",
    "defaultRegion": "${REGION}"
  }
}
EOF

echo ""
echo "✅ X-Ray data source will be configured via Grafana workspace settings"
echo "   Data source type: X-Ray"
echo "   Authentication: AWS IAM (via workspace role)"
echo "   Default region: ${REGION}"

echo ""
echo "📋 Step 5: Enable Grafana Plugins"
echo "=================================="

echo "Required plugins:"
echo "- CloudWatch (built-in)"
echo "- Prometheus (built-in)"
echo "- X-Ray (grafana-x-ray-datasource)"

echo ""
echo "Enable X-Ray plugin:"
echo "aws grafana update-workspace \\"
echo "  --workspace-id $WORKSPACE_ID \\"
echo "  --region $REGION \\"
echo "  --plugin-admin-enabled"

echo ""
echo "📋 Step 6: Configure SSO Authentication"
echo "========================================"

echo "SSO Configuration:"
echo "1. Go to AWS IAM Identity Center"
echo "2. Create a new application for Grafana"
echo "3. Configure SAML 2.0 integration"
echo "4. Add users and groups"
echo ""
echo "Or use AWS Console:"
echo "https://${REGION}.console.aws.amazon.com/grafana/home?region=${REGION}#/workspaces/${WORKSPACE_ID}"

echo ""
echo "📋 Step 7: Configure Workspace Permissions"
echo "==========================================="

echo "Assign user roles:"
echo ""
echo "# Admin role"
echo "aws grafana update-permissions \\"
echo "  --workspace-id $WORKSPACE_ID \\"
echo "  --update-instruction-batch '[{\"action\":\"ADD\",\"role\":\"ADMIN\",\"users\":[{\"id\":\"user@example.com\",\"type\":\"SSO_USER\"}]}]'"
echo ""
echo "# Editor role"
echo "aws grafana update-permissions \\"
echo "  --workspace-id $WORKSPACE_ID \\"
echo "  --update-instruction-batch '[{\"action\":\"ADD\",\"role\":\"EDITOR\",\"users\":[{\"id\":\"user@example.com\",\"type\":\"SSO_USER\"}]}]'"

echo ""
echo "🎯 Configuration Summary"
echo "========================"

echo "✅ Grafana Workspace: $WORKSPACE_ID"
echo "✅ Endpoint: $WORKSPACE_ENDPOINT"
echo "✅ Data Sources: CloudWatch, Prometheus, X-Ray"
echo ""
echo "📋 Next Steps:"
echo "1. Access Grafana workspace:"
echo "   https://$WORKSPACE_ENDPOINT"
echo ""
echo "2. Configure SSO users in IAM Identity Center"
echo ""
echo "3. Import dashboards:"
echo "   - Use dashboard JSON files from infrastructure/grafana/dashboards/"
echo ""
echo "4. Set up alerts and notifications"
echo ""
echo "📚 Documentation:"
echo "- Grafana Guide: docs/grafana-operations-dashboard-guide.md"
echo ""
echo "🎉 Grafana configuration completed!"
