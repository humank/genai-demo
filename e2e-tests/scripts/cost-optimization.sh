#!/bin/bash
# Cost Optimization Script for Staging Environment
# Requirements: 12.14, 12.15

set -e

AWS_REGION="${AWS_REGION:-ap-northeast-1}"
ENVIRONMENT="staging"

# Stop resources during non-business hours (8 PM - 8 AM)
schedule_resource_shutdown() {
    echo "Scheduling resource shutdown for non-business hours..."
    ./manage-staging-resources.sh stop
}

# Start resources during business hours
schedule_resource_startup() {
    echo "Scheduling resource startup for business hours..."
    ./manage-staging-resources.sh start
}

# Generate cost report
generate_cost_report() {
    echo "Generating cost optimization report..."
    python3 ../monitoring/cost_analysis.py --generate-report
}

# Main
case "${1:-}" in
    shutdown) schedule_resource_shutdown ;;
    startup) schedule_resource_startup ;;
    report) generate_cost_report ;;
    *) echo "Usage: $0 {shutdown|startup|report}" ;;
esac
