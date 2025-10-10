#!/bin/bash

# Deploy CloudWatch Container Insights to EKS Cluster
# Usage: ./deploy-container-insights.sh <environment> <cluster-name>

set -e

# Configuration
REGION="ap-northeast-1"
PROFILE="default"

# Parse arguments
ENVIRONMENT=${1:-"staging"}
CLUSTER_NAME=${2:-"genai-demo-cluster"}

echo "🚀 Deploying CloudWatch Container Insights"
echo "Environment: $ENVIRONMENT"
echo "Cluster: $CLUSTER_NAME"
echo "Region: $REGION"
echo ""

# Check if kubectl is configured
if ! kubectl cluster-info > /dev/null 2>&1; then
    echo "❌ Error: kubectl not configured or cluster not accessible"
    echo "Please configure kubectl to access the EKS cluster"
    exit 1
fi

# Check if AWS CLI is configured
if ! aws sts get-caller-identity --profile $PROFILE > /dev/null 2>&1; then
    echo "❌ Error: AWS CLI not configured or invalid profile"
    exit 1
fi

echo "📋 Step 1: Enable Container Insights on EKS Cluster"
echo "===================================================="

# Enable Container Insights on the cluster
echo "Enabling Container Insights..."
aws eks update-cluster-config \
    --name $CLUSTER_NAME \
    --region $REGION \
    --profile $PROFILE \
    --logging '{"clusterLogging":[{"types":["api","audit","authenticator","controllerManager","scheduler"],"enabled":true}]}' \
    > /dev/null 2>&1 || echo "⚠️  Cluster logging may already be enabled"

echo "✅ Container Insights enabled on cluster"

echo ""
echo "📋 Step 2: Create IAM Policy for Container Insights"
echo "====================================================="

# Create IAM policy for CloudWatch Agent
POLICY_NAME="CloudWatchAgentServerPolicy-${CLUSTER_NAME}"
POLICY_ARN="arn:aws:iam::$(aws sts get-caller-identity --query Account --output text --profile $PROFILE):policy/${POLICY_NAME}"

# Check if policy already exists
if aws iam get-policy --policy-arn $POLICY_ARN --profile $PROFILE > /dev/null 2>&1; then
    echo "✅ IAM policy already exists: $POLICY_NAME"
else
    echo "Creating IAM policy..."
    
    cat > /tmp/cloudwatch-agent-policy.json <<EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "cloudwatch:PutMetricData",
                "ec2:DescribeVolumes",
                "ec2:DescribeTags",
                "logs:PutLogEvents",
                "logs:DescribeLogStreams",
                "logs:DescribeLogGroups",
                "logs:CreateLogStream",
                "logs:CreateLogGroup"
            ],
            "Resource": "*"
        },
        {
            "Effect": "Allow",
            "Action": [
                "ssm:GetParameter"
            ],
            "Resource": "arn:aws:ssm:*:*:parameter/AmazonCloudWatch-*"
        }
    ]
}
EOF
    
    aws iam create-policy \
        --policy-name $POLICY_NAME \
        --policy-document file:///tmp/cloudwatch-agent-policy.json \
        --profile $PROFILE \
        > /dev/null
    
    echo "✅ IAM policy created: $POLICY_NAME"
    rm /tmp/cloudwatch-agent-policy.json
fi

echo ""
echo "📋 Step 3: Create IAM Role for Service Account (IRSA)"
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
cat > /tmp/trust-policy.json <<EOF
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
          "${OIDC_PROVIDER}:sub": "system:serviceaccount:amazon-cloudwatch:cloudwatch-agent",
          "${OIDC_PROVIDER}:aud": "sts.amazonaws.com"
        }
      }
    }
  ]
}
EOF

# Create IAM role
ROLE_NAME="CloudWatchAgentRole-${CLUSTER_NAME}"

if aws iam get-role --role-name $ROLE_NAME --profile $PROFILE > /dev/null 2>&1; then
    echo "✅ IAM role already exists: $ROLE_NAME"
else
    echo "Creating IAM role..."
    
    aws iam create-role \
        --role-name $ROLE_NAME \
        --assume-role-policy-document file:///tmp/trust-policy.json \
        --profile $PROFILE \
        > /dev/null
    
    # Attach policy to role
    aws iam attach-role-policy \
        --role-name $ROLE_NAME \
        --policy-arn $POLICY_ARN \
        --profile $PROFILE
    
    echo "✅ IAM role created and policy attached: $ROLE_NAME"
fi

rm /tmp/trust-policy.json

echo ""
echo "📋 Step 4: Deploy CloudWatch Agent DaemonSet"
echo "=============================================="

# Update ConfigMap with cluster name
sed "s/genai-demo-cluster/${CLUSTER_NAME}/g" \
    infrastructure/k8s/monitoring/cloudwatch-agent-daemonset.yaml | \
    kubectl apply -f -

echo "✅ CloudWatch Agent DaemonSet deployed"

echo ""
echo "📋 Step 5: Annotate Service Account with IAM Role"
echo "=================================================="

# Annotate service account
kubectl annotate serviceaccount cloudwatch-agent \
    -n amazon-cloudwatch \
    eks.amazonaws.com/role-arn=arn:aws:iam::$(aws sts get-caller-identity --query Account --output text --profile $PROFILE):role/${ROLE_NAME} \
    --overwrite

echo "✅ Service account annotated with IAM role"

echo ""
echo "📋 Step 6: Deploy Fluent Bit DaemonSet"
echo "========================================"

# Update Fluent Bit ConfigMap with cluster name and region
sed -e "s/genai-demo-cluster/${CLUSTER_NAME}/g" \
    -e "s/ap-northeast-1/${REGION}/g" \
    infrastructure/k8s/monitoring/fluent-bit-daemonset.yaml | \
    kubectl apply -f -

echo "✅ Fluent Bit DaemonSet deployed"

echo ""
echo "📋 Step 7: Verify Deployment"
echo "============================="

echo "Waiting for pods to be ready..."
sleep 10

# Check CloudWatch Agent pods
echo ""
echo "CloudWatch Agent pods:"
kubectl get pods -n amazon-cloudwatch -l name=cloudwatch-agent

# Check Fluent Bit pods
echo ""
echo "Fluent Bit pods:"
kubectl get pods -n amazon-cloudwatch -l k8s-app=fluent-bit

# Check if pods are running
AGENT_READY=$(kubectl get pods -n amazon-cloudwatch -l name=cloudwatch-agent --field-selector=status.phase=Running --no-headers | wc -l)
FLUENT_READY=$(kubectl get pods -n amazon-cloudwatch -l k8s-app=fluent-bit --field-selector=status.phase=Running --no-headers | wc -l)

echo ""
if [ "$AGENT_READY" -gt 0 ] && [ "$FLUENT_READY" -gt 0 ]; then
    echo "✅ Container Insights deployment successful!"
    echo ""
    echo "📊 Metrics and logs will be available in CloudWatch within 5-10 minutes"
    echo ""
    echo "📋 CloudWatch Console URLs:"
    echo "- Container Insights: https://${REGION}.console.aws.amazon.com/cloudwatch/home?region=${REGION}#container-insights:infrastructure"
    echo "- Log Groups: https://${REGION}.console.aws.amazon.com/cloudwatch/home?region=${REGION}#logsV2:log-groups"
    echo ""
    echo "📚 Log Groups Created:"
    echo "- /aws/containerinsights/${CLUSTER_NAME}/application"
    echo "- /aws/containerinsights/${CLUSTER_NAME}/dataplane"
    echo "- /aws/containerinsights/${CLUSTER_NAME}/host"
    echo "- /aws/containerinsights/${CLUSTER_NAME}/performance"
    echo ""
    echo "🔍 Verify metrics:"
    echo "aws cloudwatch list-metrics --namespace ContainerInsights --region ${REGION}"
else
    echo "⚠️  Some pods are not ready yet"
    echo "CloudWatch Agent ready: $AGENT_READY"
    echo "Fluent Bit ready: $FLUENT_READY"
    echo ""
    echo "Check pod status:"
    echo "kubectl get pods -n amazon-cloudwatch"
    echo ""
    echo "Check pod logs:"
    echo "kubectl logs -n amazon-cloudwatch -l name=cloudwatch-agent"
    echo "kubectl logs -n amazon-cloudwatch -l k8s-app=fluent-bit"
fi

echo ""
echo "🎉 Container Insights deployment completed!"
