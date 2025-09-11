#!/bin/bash

# Script to create Kafka topics for domain events in MSK cluster
# Usage: ./create-kafka-topics.sh <environment> <region>

set -e

ENVIRONMENT=${1:-development}
REGION=${2:-ap-northeast-1}
PROJECT_NAME="genai-demo"

echo "Creating Kafka topics for ${PROJECT_NAME} ${ENVIRONMENT} environment in ${REGION}"

# Get MSK cluster information from CloudFormation outputs
STACK_NAME="${PROJECT_NAME}-${ENVIRONMENT}-MSKStack"
BOOTSTRAP_SERVERS=$(aws cloudformation describe-stacks \
    --stack-name ${STACK_NAME} \
    --region ${REGION} \
    --query 'Stacks[0].Outputs[?OutputKey==`MSKBootstrapServersIAM`].OutputValue' \
    --output text)

if [ -z "$BOOTSTRAP_SERVERS" ]; then
    echo "Error: Could not retrieve MSK bootstrap servers from CloudFormation stack ${STACK_NAME}"
    exit 1
fi

echo "Using MSK bootstrap servers: ${BOOTSTRAP_SERVERS}"

# Domain event topics configuration
TOPIC_PREFIX="${PROJECT_NAME}.${ENVIRONMENT}"
PARTITIONS=6
REPLICATION_FACTOR=3

# Adjust replication factor for development environment
if [ "$ENVIRONMENT" = "development" ]; then
    REPLICATION_FACTOR=1
fi

# List of domain event topics
TOPICS=(
    "customer.created"
    "customer.updated"
    "customer.profile.updated"
    "customer.spending.updated"
    "reward.points.earned"
    "reward.points.redeemed"
    "order.created"
    "order.confirmed"
    "order.cancelled"
    "order.shipped"
    "order.delivered"
    "order.discount.applied"
    "payment.processed"
    "payment.failed"
    "payment.refunded"
    "inventory.reserved"
    "inventory.released"
    "inventory.updated"
    "product.created"
    "product.updated"
    "product.price.changed"
    "promotion.created"
    "promotion.activated"
    "promotion.expired"
    "notification.sent"
    "audit.event.logged"
)

# Function to create a topic
create_topic() {
    local topic_name="$1"
    local full_topic_name="${TOPIC_PREFIX}.${topic_name}"
    
    echo "Creating topic: ${full_topic_name}"
    
    # Use kafka-topics.sh with IAM authentication
    kafka-topics.sh \
        --bootstrap-server ${BOOTSTRAP_SERVERS} \
        --command-config /tmp/kafka-client.properties \
        --create \
        --topic ${full_topic_name} \
        --partitions ${PARTITIONS} \
        --replication-factor ${REPLICATION_FACTOR} \
        --config cleanup.policy=delete \
        --config retention.ms=604800000 \
        --config compression.type=snappy \
        --config min.insync.replicas=1 \
        --if-not-exists
    
    if [ $? -eq 0 ]; then
        echo "✓ Successfully created topic: ${full_topic_name}"
    else
        echo "✗ Failed to create topic: ${full_topic_name}"
    fi
}

# Create Kafka client configuration for IAM authentication
create_client_config() {
    cat > /tmp/kafka-client.properties << EOF
security.protocol=SASL_SSL
sasl.mechanism=AWS_MSK_IAM
sasl.jaas.config=software.amazon.msk.auth.iam.IAMLoginModule required;
sasl.client.callback.handler.class=software.amazon.msk.auth.iam.IAMClientCallbackHandler
EOF
}

# Function to list existing topics
list_topics() {
    echo "Listing existing topics:"
    kafka-topics.sh \
        --bootstrap-server ${BOOTSTRAP_SERVERS} \
        --command-config /tmp/kafka-client.properties \
        --list | grep "^${TOPIC_PREFIX}\." || echo "No topics found with prefix ${TOPIC_PREFIX}"
}

# Function to describe a topic
describe_topic() {
    local topic_name="$1"
    local full_topic_name="${TOPIC_PREFIX}.${topic_name}"
    
    echo "Describing topic: ${full_topic_name}"
    kafka-topics.sh \
        --bootstrap-server ${BOOTSTRAP_SERVERS} \
        --command-config /tmp/kafka-client.properties \
        --describe \
        --topic ${full_topic_name}
}

# Main execution
main() {
    echo "========================================="
    echo "MSK Domain Events Topic Creation Script"
    echo "========================================="
    echo "Environment: ${ENVIRONMENT}"
    echo "Region: ${REGION}"
    echo "Project: ${PROJECT_NAME}"
    echo "Topic Prefix: ${TOPIC_PREFIX}"
    echo "Partitions: ${PARTITIONS}"
    echo "Replication Factor: ${REPLICATION_FACTOR}"
    echo "========================================="
    
    # Check if kafka-topics.sh is available
    if ! command -v kafka-topics.sh &> /dev/null; then
        echo "Error: kafka-topics.sh not found. Please install Apache Kafka client tools."
        echo "You can download them from: https://kafka.apache.org/downloads"
        exit 1
    fi
    
    # Create client configuration
    create_client_config
    
    # List existing topics first
    echo ""
    echo "Current topics:"
    list_topics
    echo ""
    
    # Create all domain event topics
    echo "Creating domain event topics..."
    for topic in "${TOPICS[@]}"; do
        create_topic "$topic"
        sleep 1  # Small delay between topic creations
    done
    
    echo ""
    echo "Topic creation completed!"
    echo ""
    
    # List topics again to show what was created
    echo "Final topic list:"
    list_topics
    
    # Clean up temporary files
    rm -f /tmp/kafka-client.properties
    
    echo ""
    echo "========================================="
    echo "Domain event topics are ready for use!"
    echo "========================================="
    echo ""
    echo "Spring Boot configuration:"
    echo "spring.kafka.bootstrap-servers=${BOOTSTRAP_SERVERS}"
    echo "spring.kafka.security.protocol=SASL_SSL"
    echo "spring.kafka.sasl.mechanism=AWS_MSK_IAM"
    echo ""
}

# Handle command line arguments
case "${1:-}" in
    --help|-h)
        echo "Usage: $0 [environment] [region]"
        echo ""
        echo "Arguments:"
        echo "  environment  Target environment (default: development)"
        echo "  region       AWS region (default: ap-northeast-1)"
        echo ""
        echo "Examples:"
        echo "  $0 development ap-northeast-1"
        echo "  $0 production ap-east-2"
        echo ""
        echo "Prerequisites:"
        echo "  - AWS CLI configured with appropriate permissions"
        echo "  - Apache Kafka client tools installed"
        echo "  - MSK cluster deployed via CDK"
        exit 0
        ;;
    --list)
        create_client_config
        list_topics
        rm -f /tmp/kafka-client.properties
        exit 0
        ;;
    --describe)
        if [ -z "$2" ]; then
            echo "Error: Topic name required for --describe"
            echo "Usage: $0 --describe <topic-name> [environment] [region]"
            exit 1
        fi
        TOPIC_NAME="$2"
        ENVIRONMENT="${3:-development}"
        REGION="${4:-ap-northeast-1}"
        
        # Get bootstrap servers
        STACK_NAME="${PROJECT_NAME}-${ENVIRONMENT}-MSKStack"
        BOOTSTRAP_SERVERS=$(aws cloudformation describe-stacks \
            --stack-name ${STACK_NAME} \
            --region ${REGION} \
            --query 'Stacks[0].Outputs[?OutputKey==`MSKBootstrapServersIAM`].OutputValue' \
            --output text)
        
        create_client_config
        describe_topic "$TOPIC_NAME"
        rm -f /tmp/kafka-client.properties
        exit 0
        ;;
    *)
        main
        ;;
esac