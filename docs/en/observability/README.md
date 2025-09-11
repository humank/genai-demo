# Observability System Documentation

## Overview

This project implements a complete enterprise-grade observability system, including distributed tracing, structured logging, business metrics collection, and cost optimization analysis.

## Core Components

### 🔍 Distributed Tracing

- **AWS X-Ray**: Cross-service request tracing
- **Jaeger**: Local development environment tracing
- **Correlation ID**: Unified request tracking identifier

### 📝 Structured Logging

- **Logback**: Unified log format
- **PII Masking**: Sensitive data protection
- **CloudWatch**: Log aggregation and analysis

### 📊 Business Metrics

- **Micrometer**: Metrics collection framework
- **CloudWatch**: Custom business metrics
- **Prometheus**: Metrics exposure endpoint

### 💰 Cost Optimization

- **Resource Right-sizing**: Automated resource analysis
- **Cost Tracking**: Real-time cost monitoring
- **Optimization Recommendations**: Intelligent cost suggestions

## Quick Start

### Enable Observability Features

```bash
# Start application (automatically enables observability)
./gradlew bootRun

# Check health status
curl http://localhost:8080/actuator/health

# View application metrics
curl http://localhost:8080/actuator/metrics

# Get cost optimization recommendations
curl http://localhost:8080/api/cost-optimization/recommendations
```

### Configure Environment Variables

```bash
# AWS X-Ray configuration
export AWS_XRAY_TRACING_NAME=genai-demo
export AWS_XRAY_CONTEXT_MISSING=LOG_ERROR

# CloudWatch configuration
export CLOUDWATCH_NAMESPACE=GenAI/Demo
export CLOUDWATCH_REGION=us-east-1
```

## Detailed Documentation

- [Distributed Tracing Implementation](../../app/docs/DISTRIBUTED_TRACING_IMPLEMENTATION.md)
- [Structured Logging Implementation](../../app/docs/STRUCTURED_LOGGING_IMPLEMENTATION.md)
- [Metrics Implementation](../../app/docs/METRICS_IMPLEMENTATION.md)
