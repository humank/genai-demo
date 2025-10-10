# Route53 Global Routing Stack Integration Guide

## Overview

The `Route53GlobalRoutingStack` extends the existing Route53 failover capabilities to provide true 
Active-Active multi-region DNS routing with intelligent traffic distribution, real-time health monitoring, 
and A/B testing capabilities.

## Key Features

### 🌍 **Geolocation-Based Routing**

- Routes traffic based on user's geographic location
- Optimizes performance by directing users to nearest region
- Supports continent-level routing (North America, Europe, Asia)
- Includes default fallback for unmatched locations

### ⚖️ **Weighted Routing for A/B Testing**

- Configurable traffic distribution across regions
- Default split: Primary (70%), Secondary (20%), Tertiary (10%)
- Enables controlled rollouts and feature testing
- Real-time traffic distribution monitoring

### 🚀 **Latency-Based Routing**

- Routes to region with lowest latency for optimal performance
- Automatic performance optimization based on real-time measurements
- Fallback mechanism when regions become unavailable

### 🏥 **Enhanced Health Monitoring**

- 30-second health check intervals (configurable)
- HTTPS health checks on `/actuator/health` endpoint
- Multi-region health check execution for reliability
- Latency measurement and SNI support

## Integration with Existing Infrastructure

### Based on Existing Route53 Failover Stack

```typescript
// Extends existing route53-failover-stack.ts functionality
// Maintains backward compatibility with current failover setup
// Adds new routing types while preserving existing health checks
```

### Certificate Stack Integration

```typescript
// Uses existing SSL certificates from certificate-stack.ts
// Leverages existing hosted zone configuration
// Maintains certificate validation and monitoring
```

### Core Infrastructure Integration

```typescript
// Integrates with existing ALB from core-infrastructure-stack.ts
// Uses existing load balancer health check endpoints
// Maintains existing monitoring and alerting systems
```

## Usage Example

```typescript
import { Route53GlobalRoutingStack } from '../src/stacks/route53-global-routing-stack';

// Create global routing with existing infrastructure
const globalRouting = new Route53GlobalRoutingStack(this, 'GlobalRouting', {
    environment: 'production',
    projectName: 'genai-demo',
    domain: 'api.genai-demo.com',
    hostedZone: certificateStack.hostedZone,
    certificate: certificateStack.certificate,
    regions: {
        primary: {
            region: 'us-east-1',
            loadBalancer: primaryInfraStack.loadBalancer,
            weight: 70 // 70% traffic for A/B testing
        },
        secondary: {
            region: 'eu-west-1',
            loadBalancer: secondaryInfraStack.loadBalancer,
            weight: 20 // 20% traffic for A/B testing
        },
        tertiary: {
            region: 'ap-southeast-1',
            loadBalancer: tertiaryInfraStack.loadBalancer,
            weight: 10 // 10% traffic for A/B testing
        }
    },
    monitoringConfig: {
        healthCheckInterval: 30, // 30-second intervals
        failureThreshold: 3,     // 3 failures trigger failover
        enableABTesting: true,   // Enable weighted routing
        enableGeolocationRouting: true // Enable geo routing
    }
});
```

## DNS Endpoints Created

### 1. Geolocation Routing

- **Endpoint**: `api-geo.{domain}`
- **Purpose**: Routes based on user's geographic location
- **Routing Logic**:
  - North America → Primary region
  - Europe → Secondary region  
  - Asia → Tertiary region
  - Default → Primary region

### 2. Weighted Routing (A/B Testing)

- **Endpoint**: `api-weighted.{domain}`
- **Purpose**: Distributes traffic based on configured weights
- **Default Distribution**:
  - Primary: 70%
  - Secondary: 20%
  - Tertiary: 10%

### 3. Latency-Based Routing

- **Endpoint**: `api-latency.{domain}`
- **Purpose**: Routes to region with lowest latency
- **Benefits**: Optimal performance for each user

## Monitoring and Alerting

### CloudWatch Dashboard

- Health check status for all regions
- DNS query metrics by routing type
- Latency comparison across regions
- Traffic distribution visualization

### Automated Alerts

- Health check failure notifications
- High latency warnings (>2 seconds)
- Global system health composite alarms
- DNS query rate monitoring (DDoS detection)

### SNS Integration

- Centralized alerting topic for all routing events
- Integration with existing notification systems
- Escalation policies for critical failures

## Configuration Options

### Health Check Configuration

```typescript
monitoringConfig: {
    healthCheckInterval: 30,    // Seconds between checks
    failureThreshold: 3,        // Failures before failover
    enableABTesting: true,      // Enable weighted routing
    enableGeolocationRouting: true // Enable geo routing
}
```

### Traffic Distribution

```typescript
regions: {
    primary: { weight: 70 },    // 70% of traffic
    secondary: { weight: 20 },  // 20% of traffic
    tertiary: { weight: 10 }    // 10% of traffic
}
```

## Requirements Fulfilled

### ✅ Requirement 4.1.3 - Global Routing

- ✅ Geolocation-based intelligent routing
- ✅ SSL certificate integration from existing Certificate Stack
- ✅ Real-time health checks with 30-second intervals
- ✅ Weighted routing for A/B testing support
- ✅ Integration with existing monitoring systems

## Deployment Considerations

### Prerequisites

1. Existing Certificate Stack deployed with SSL certificates
2. Core Infrastructure Stack with Application Load Balancers
3. Multi-region deployment with healthy endpoints
4. Route53 hosted zone configured and accessible

### Deployment Order

1. Deploy Certificate Stack (existing)
2. Deploy Core Infrastructure Stacks in all regions (existing)
3. Deploy Route53 Global Routing Stack (new)
4. Verify health checks and DNS resolution
5. Test traffic distribution and failover scenarios

### Testing Checklist

- [ ] Health checks pass for all regions
- [ ] DNS resolution works for all endpoint types
- [ ] Geolocation routing directs traffic correctly
- [ ] Weighted routing distributes traffic as configured
- [ ] Latency routing selects optimal region
- [ ] Failover works when regions become unhealthy
- [ ] Monitoring dashboard shows accurate metrics
- [ ] Alerts trigger correctly for failure scenarios

## Troubleshooting

### Common Issues

1. **Health checks failing**: Verify `/actuator/health` endpoint accessibility
2. **DNS not resolving**: Check hosted zone configuration and propagation
3. **Incorrect routing**: Verify region configuration and weights
4. **Missing metrics**: Ensure CloudWatch permissions are configured

### Debug Commands

```bash
# Test DNS resolution
dig api-geo.genai-demo.com
dig api-weighted.genai-demo.com
dig api-latency.genai-demo.com

# Check health check status
aws route53 get-health-check --health-check-id <health-check-id>

# Monitor CloudWatch metrics
aws cloudwatch get-metric-statistics --namespace AWS/Route53 --metric-name HealthCheckStatus
```

## Performance Targets

- **Global P95 Response Time**: < 200ms
- **Health Check Interval**: 30 seconds
- **Failover Time**: < 2 minutes (RTO)
- **Data Loss**: < 1 second (RPO)
- **System Availability**: ≥ 99.99%

This implementation provides the foundation for true Active-Active multi-region architecture with 
intelligent DNS routing, comprehensive monitoring, and automated failover capabilities.