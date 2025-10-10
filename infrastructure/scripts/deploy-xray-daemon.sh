#!/bin/bash

# Deploy AWS X-Ray Daemon to EKS Cluster
# Usage: ./deploy-xray-daemon.sh <environment> <cluster-name>

set -e

# Configuration
REGION="ap-northeast-1"
PROFILE="default"

# Parse arguments
ENVIRONMENT=${1:-"staging"}
CLUSTER_NAME=${2:-"genai-demo-cluster"}

echo "🚀 Deploying AWS X-Ray Daemon"
echo "Environment: $ENVIRONMENT"
echo "Cluster: $CLUSTER_NAME"
echo "Region: $REGION"
echo ""

# Check if kubectl is configured
if ! kubectl cluster-info > /dev/null 2>&1; then
    echo "❌ Error: kubectl not configured or cluster not accessible"
    exit 1
fi

# Check if AWS CLI is configured
if ! aws sts get-caller-identity --profile $PROFILE > /dev/null 2>&1; then
    echo "❌ Error: AWS CLI not configured or invalid profile"
    exit 1
fi

echo "📋 Step 1: Create IAM Policy for X-Ray"
echo "======================================="

# Create IAM policy for X-Ray daemon
POLICY_NAME="XRayDaemonPolicy-${CLUSTER_NAME}"
POLICY_ARN="arn:aws:iam::$(aws sts get-caller-identity --query Account --output text --profile $PROFILE):policy/${POLICY_NAME}"

# Check if policy already exists
if aws iam get-policy --policy-arn $POLICY_ARN --profile $PROFILE > /dev/null 2>&1; then
    echo "✅ IAM policy already exists: $POLICY_NAME"
else
    echo "Creating IAM policy..."
    
    cat > /tmp/xray-daemon-policy.json <<EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "xray:PutTraceSegments",
                "xray:PutTelemetryRecords",
                "xray:GetSamplingRules",
                "xray:GetSamplingTargets",
                "xray:GetSamplingStatisticSummaries"
            ],
            "Resource": "*"
        }
    ]
}
EOF
    
    aws iam create-policy \
        --policy-name $POLICY_NAME \
        --policy-document file:///tmp/xray-daemon-policy.json \
        --profile $PROFILE \
        > /dev/null
    
    echo "✅ IAM policy created: $POLICY_NAME"
    rm /tmp/xray-daemon-policy.json
fi

echo ""
echo "📋 Step 2: Create IAM Role for Service Account (IRSA)"
echo "======================================================"

# Get OIDC provider for the cluster
OIDC_PROVIDER=$(aws eks describe-cluster \
    --name $CLUSTER_NAME \
    --region $REGION \
    --profile $PROFILE \
    --query "cluster.identity.oidc.issuer" \
    --output text | sed -e "s/^https:\/\///")

echo "OIDC Provider: $OIDC_PROVIDER"

# Create trust policy
cat > /tmp/xray-trust-policy.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::$(aws sts get-caller-identity --query Account --output text --profile $PROFILE):oidc-provider/${OIDC_PROVIDER}"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "${OIDC_PROVIDER}:sub": "system:serviceaccount:amazon-xray:xray-daemon",
          "${OIDC_PROVIDER}:aud": "sts.amazonaws.com"
        }
      }
    }
  ]
}
EOF

# Create IAM role
ROLE_NAME="XRayDaemonRole-${CLUSTER_NAME}"

if aws iam get-role --role-name $ROLE_NAME --profile $PROFILE > /dev/null 2>&1; then
    echo "✅ IAM role already exists: $ROLE_NAME"
else
    echo "Creating IAM role..."
    
    aws iam create-role \
        --role-name $ROLE_NAME \
        --assume-role-policy-document file:///tmp/xray-trust-policy.json \
        --profile $PROFILE \
        > /dev/null
    
    # Attach policy to role
    aws iam attach-role-policy \
        --role-name $ROLE_NAME \
        --policy-arn $POLICY_ARN \
        --profile $PROFILE
    
    echo "✅ IAM role created and policy attached: $ROLE_NAME"
fi

rm /tmp/xray-trust-policy.json

echo ""
echo "📋 Step 3: Create X-Ray Sampling Rules"
echo "========================================"

# Create sampling rules in X-Ray
echo "Creating X-Ray sampling rules..."

# Check if sampling rules file exists
if [ -f "infrastructure/k8s/monitoring/xray-sampling-rules.json" ]; then
    # Extract rules from JSON and create them
    jq -c '.rules[]' infrastructure/k8s/monitoring/xray-sampling-rules.json | while read rule; do
        rule_name=$(echo $rule | jq -r '.description')
        
        # Check if rule already exists
        existing_rule=$(aws xray get-sampling-rules \
            --region $REGION \
            --profile $PROFILE \
            --query "SamplingRuleRecords[?SamplingRule.RuleName=='$rule_name'].SamplingRule.RuleName" \
            --output text 2>/dev/null)
        
        if [ -z "$existing_rule" ]; then
            echo "Creating sampling rule: $rule_name"
            
            # Create the rule
            aws xray create-sampling-rule \
                --region $REGION \
                --profile $PROFILE \
                --sampling-rule "$rule" \
                > /dev/null 2>&1 || echo "⚠️  Failed to create rule: $rule_name (may already exist)"
        else
            echo "✅ Sampling rule already exists: $rule_name"
        fi
    done
    
    echo "✅ Sampling rules configured"
else
    echo "⚠️  Sampling rules file not found, using default sampling"
fi

echo ""
echo "📋 Step 4: Deploy X-Ray Daemon DaemonSet"
echo "=========================================="

# Deploy X-Ray daemon
kubectl apply -f infrastructure/k8s/monitoring/xray-daemon-daemonset.yaml

echo "✅ X-Ray Daemon DaemonSet deployed"

echo ""
echo "📋 Step 5: Annotate Service Account with IAM Role"
echo "=================================================="

# Wait for service account to be created
sleep 5

# Annotate service account
kubectl annotate serviceaccount xray-daemon \
    -n amazon-xray \
    eks.amazonaws.com/role-arn=arn:aws:iam::$(aws sts get-caller-identity --query Account --output text --profile $PROFILE):role/${ROLE_NAME} \
    --overwrite

echo "✅ Service account annotated with IAM role"

echo ""
echo "📋 Step 6: Verify Deployment"
echo "============================="

echo "Waiting for pods to be ready..."
sleep 10

# Check X-Ray daemon pods
echo ""
echo "X-Ray Daemon pods:"
kubectl get pods -n amazon-xray -l app=xray-daemon

# Check if pods are running
DAEMON_READY=$(kubectl get pods -n amazon-xray -l app=xray-daemon --field-selector=status.phase=Running --no-headers | wc -l)

echo ""
if [ "$DAEMON_READY" -gt 0 ]; then
    echo "✅ X-Ray Daemon deployment successful!"
    echo ""
    echo "📊 X-Ray will start collecting traces immediately"
    echo ""
    echo "📋 X-Ray Console URLs:"
    echo "- Service Map: https://${REGION}.console.aws.amazon.com/xray/home?region=${REGION}#/service-map"
    echo "- Traces: https://${REGION}.console.aws.amazon.com/xray/home?region=${REGION}#/traces"
    echo "- Analytics: https://${REGION}.console.aws.amazon.com/xray/home?region=${REGION}#/analytics"
    echo ""
    echo "🔍 Test X-Ray:"
    echo "1. Make some requests to your application"
    echo "2. Wait 1-2 minutes for traces to appear"
    echo "3. View traces in X-Ray console"
    echo ""
    echo "📚 Query traces:"
    echo "aws xray get-trace-summaries \\"
    echo "  --start-time \$(date -u -d '10 minutes ago' +%s) \\"
    echo "  --end-time \$(date -u +%s) \\"
    echo "  --region ${REGION}"
else
    echo "⚠️  X-Ray Daemon pods are not ready yet"
    echo "X-Ray Daemon ready: $DAEMON_READY"
    echo ""
    echo "Check pod status:"
    echo "kubectl get pods -n amazon-xray"
    echo ""
    echo "Check pod logs:"
    echo "kubectl logs -n amazon-xray -l app=xray-daemon"
fi

echo ""
echo "🎉 X-Ray Daemon deployment completed!"
