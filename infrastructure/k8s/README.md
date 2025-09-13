# Kubernetes Manifests for GenAI Demo

This directory contains Kubernetes manifests for deploying the GenAI Demo application to Amazon EKS.

## Structure

```
k8s/
├── base/                    # Base Kubernetes manifests
│   ├── namespace.yaml       # Application namespaces
│   ├── deployment.yaml      # Application deployment
│   ├── service.yaml         # Kubernetes services
│   ├── ingress.yaml         # ALB ingress configuration
│   ├── hpa.yaml            # Horizontal Pod Autoscaler
│   └── serviceaccount.yaml  # Service accounts and RBAC
├── overlays/               # Environment-specific configurations
│   ├── development/        # Development environment
│   │   ├── kustomization.yaml
│   │   ├── deployment-patch.yaml
│   │   ├── hpa-patch.yaml
│   │   └── ingress-patch.yaml
│   └── production/         # Production environment
│       ├── kustomization.yaml
│       ├── deployment-patch.yaml
│       └── ingress-patch.yaml
└── README.md              # This file
```

## Features

### Base Configuration

- **Namespace**: Separate namespaces for application and system components
- **Deployment**: Spring Boot application with comprehensive health checks
- **Service**: ClusterIP service with headless service for service discovery
- **Ingress**: AWS Load Balancer Controller integration with SSL termination
- **HPA**: Horizontal Pod Autoscaler based on CPU and memory metrics
- **ServiceAccount**: IRSA-enabled service accounts for AWS integration

### Security Features

- Non-root container execution
- Read-only root filesystem
- Security context with dropped capabilities
- Pod security standards compliance
- Network policies (to be added)

### Observability

- Prometheus metrics scraping annotations
- Comprehensive health checks (liveness, readiness, startup)
- Structured logging with correlation IDs
- Distributed tracing integration

### High Availability

- Pod anti-affinity rules
- Multiple replicas in production
- Rolling update strategy
- Resource requests and limits

## Environment Differences

### Development

- Single replica
- x86_64 architecture (t3.medium instances)
- Reduced resource requirements
- Development Spring profile

### Production

- Multiple replicas (3)
- ARM64 architecture (Graviton3 m6g.large instances)
- Higher resource allocation
- Production Spring profile
- WAF integration
- Enhanced monitoring

## Deployment

### Prerequisites

1. EKS cluster with AWS Load Balancer Controller installed
2. Metrics Server for HPA
3. Cluster Autoscaler for node scaling
4. IRSA roles configured for service accounts

### Using Kustomize

```bash
# Development deployment
kubectl apply -k overlays/development/

# Production deployment
kubectl apply -k overlays/production/
```

### Using Helm (Alternative)

The manifests can also be templated using Helm for more dynamic configuration.

## Configuration

### Environment Variables

- `SPRING_PROFILES_ACTIVE`: Spring Boot profile (dev/prod)
- `ENVIRONMENT`: Deployment environment
- `POD_NAME`, `POD_IP`, `NODE_NAME`: Kubernetes metadata

### Resource Requirements

| Environment | CPU Request | Memory Request | CPU Limit | Memory Limit |
|-------------|-------------|----------------|-----------|--------------|
| Development | 250m        | 512Mi          | 1         | 1Gi          |
| Production  | 500m        | 1Gi            | 2         | 2Gi          |

### Health Checks

- **Startup Probe**: `/actuator/health` (30s initial delay, 30 failures max)
- **Liveness Probe**: `/actuator/health/liveness` (60s initial delay)
- **Readiness Probe**: `/actuator/health/readiness` (30s initial delay)

## Monitoring and Observability

### Metrics

- Prometheus scraping enabled on port 8080 at `/actuator/prometheus`
- Custom business metrics exposed via Micrometer
- JVM and system metrics included

### Logging

- Structured JSON logging to stdout
- Log aggregation via Fluent Bit to CloudWatch
- Correlation IDs for request tracing

### Tracing

- OpenTelemetry integration
- AWS X-Ray for distributed tracing
- Jaeger for local development

## Scaling

### Horizontal Pod Autoscaler

- CPU target: 70% utilization
- Memory target: 80% utilization
- Scale up: 100% increase every 15s (max)
- Scale down: 10% decrease every 60s (max)

### Cluster Autoscaler

- Automatic node scaling based on pod resource requests
- Graviton3 instances for cost optimization
- Spot instances for development environments

## Security

### RBAC

- Least privilege service accounts
- IRSA for AWS service access
- No cluster-admin permissions

### Network Security

- Private subnets for worker nodes
- Security groups for traffic control
- WAF protection for production ingress

### Container Security

- Non-root user execution
- Read-only root filesystem
- Minimal base images
- Regular security scanning

## Troubleshooting

### Common Issues

1. **Pod not starting**

   ```bash
   kubectl describe pod -l app=genai-demo -n genai-demo
   kubectl logs -l app=genai-demo -n genai-demo
   ```

2. **Health check failures**

   ```bash
   kubectl get events -n genai-demo
   kubectl logs -l app=genai-demo -n genai-demo --previous
   ```

3. **Ingress not working**

   ```bash
   kubectl describe ingress genai-demo-ingress -n genai-demo
   kubectl logs -n kube-system -l app.kubernetes.io/name=aws-load-balancer-controller
   ```

4. **HPA not scaling**

   ```bash
   kubectl describe hpa genai-demo-hpa -n genai-demo
   kubectl top pods -n genai-demo
   ```

### Useful Commands

```bash
# Check application status
kubectl get all -n genai-demo

# View application logs
kubectl logs -f deployment/genai-demo-deployment -n genai-demo

# Port forward for local access
kubectl port-forward service/genai-demo-service 8080:80 -n genai-demo

# Execute into pod
kubectl exec -it deployment/genai-demo-deployment -n genai-demo -- /bin/sh

# Check resource usage
kubectl top pods -n genai-demo
kubectl top nodes
```

## Integration with CI/CD

The manifests are designed to work with GitOps workflows:

1. **Image Updates**: Kustomize handles image tag updates
2. **Configuration Changes**: Environment-specific patches
3. **Rollback**: Kubernetes native rollback capabilities
4. **Blue-Green Deployments**: Can be implemented with additional services

## Future Enhancements

- [ ] Network policies for micro-segmentation
- [ ] Pod Disruption Budgets for availability
- [ ] Vertical Pod Autoscaler integration
- [ ] Service mesh integration (Istio/App Mesh)
- [ ] GitOps with ArgoCD
- [ ] Chaos engineering with Chaos Mesh
