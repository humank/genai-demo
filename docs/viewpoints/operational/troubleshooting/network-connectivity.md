# Network and Connectivity Troubleshooting Guide

## Overview

This document provides comprehensive troubleshooting procedures for network and connectivity issues in the Enterprise E-Commerce Platform. It covers DNS resolution, service mesh debugging, load balancer issues, ingress controllers, TLS/certificate problems, network policies, cross-region connectivity, and VPC networking.

**Target Audience**: DevOps engineers, SREs, network administrators  
**Prerequisites**: Access to AWS Console, kubectl, network debugging tools  
**Related Documents**:

- [Deployment Architecture](../../viewpoints/deployment/physical-architecture.md)
- [Network Architecture](../../viewpoints/deployment/network-architecture.md)
- [Service Outage Runbook](../runbooks/service-outage.md)

---

## Table of Contents

1. [DNS Resolution Troubleshooting](#dns-resolution-troubleshooting)
2. [Service Mesh Debugging](#service-mesh-debugging)
3. [Load Balancer Health Check Failures](#load-balancer-health-check-failures)
4. [Ingress Controller Troubleshooting](#ingress-controller-troubleshooting)
5. [Certificate and TLS Issues](#certificate-and-tls-issues)
6. [Network Policy Debugging](#network-policy-debugging)
7. [Cross-Region Connectivity](#cross-region-connectivity)
8. [VPC Peering and Transit Gateway](#vpc-peering-and-transit-gateway)
9. [Common Network Commands](#common-network-commands)

---

## DNS Resolution Troubleshooting

### Overview

DNS resolution issues can cause service discovery failures, external API call failures, and intermittent connectivity problems.

### Symptoms

- "Name or service not known" errors
- "Could not resolve host" errors
- Intermittent connection failures
- Service discovery failures
- External API timeouts

### Diagnostic Procedures

#### Step 1: Test DNS Resolution from Pod

```bash
# Test internal service DNS
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- nslookup kubernetes.default

# Test external DNS
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- nslookup google.com

# Test specific service
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- nslookup ecommerce-backend.production.svc.cluster.local
```

#### Step 2: Check CoreDNS Status

```bash
# Check CoreDNS pods
kubectl get pods -n kube-system -l k8s-app=kube-dns

# Check CoreDNS logs
kubectl logs -n kube-system -l k8s-app=kube-dns --tail=100

# Check CoreDNS configuration
kubectl get configmap coredns -n kube-system -o yaml
```

#### Step 3: Verify DNS Service

```bash
# Check kube-dns service
kubectl get svc -n kube-system kube-dns

# Verify endpoints
kubectl get endpoints -n kube-system kube-dns

# Test DNS service directly
kubectl run -it --rm debug --image=busybox --restart=Never -- nslookup kubernetes.default 10.100.0.10
```

#### Step 4: Check Pod DNS Configuration

```bash
# Check pod's resolv.conf
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- cat /etc/resolv.conf

# Expected output:
# nameserver 10.100.0.10
# search production.svc.cluster.local svc.cluster.local cluster.local
# options ndots:5
```

### Common Issues and Solutions

#### Issue: CoreDNS Pods Not Running

**Diagnosis**:

```bash
kubectl describe pods -n kube-system -l k8s-app=kube-dns
```

**Solution**:

```bash
# Restart CoreDNS
kubectl rollout restart deployment/coredns -n kube-system

# If persistent, check node resources
kubectl top nodes
```

#### Issue: DNS Queries Timing Out

**Diagnosis**:

```bash
# Check CoreDNS metrics
kubectl port-forward -n kube-system svc/kube-dns 9153:9153
curl http://localhost:9153/metrics | grep coredns_dns_request_duration_seconds
```

**Solution**:

```bash
# Scale CoreDNS replicas
kubectl scale deployment/coredns -n kube-system --replicas=3

# Adjust CoreDNS cache settings
kubectl edit configmap coredns -n kube-system
# Add: cache 30
```

#### Issue: External DNS Resolution Failures

**Diagnosis**:

```bash
# Check if external DNS is configured
kubectl exec -it ${POD_NAME} -- cat /etc/resolv.conf | grep nameserver

# Test with different DNS servers
kubectl exec -it ${POD_NAME} -- nslookup google.com 8.8.8.8
```

**Solution**:

```yaml
# Update CoreDNS ConfigMap to forward external queries
apiVersion: v1
kind: ConfigMap
metadata:
  name: coredns
  namespace: kube-system
data:
  Corefile: |
    .:53 {
        errors
        health
        kubernetes cluster.local in-addr.arpa ip6.arpa {
          pods insecure
          fallthrough in-addr.arpa ip6.arpa
        }
        prometheus :9153
        forward . 8.8.8.8 8.8.4.4
        cache 30
        loop
        reload
        loadbalance
    }
```

#### Issue: Service DNS Not Resolving

**Diagnosis**:

```bash
# Check service exists
kubectl get svc ${SERVICE_NAME} -n ${NAMESPACE}

# Check service has endpoints
kubectl get endpoints ${SERVICE_NAME} -n ${NAMESPACE}

# Verify DNS entry
kubectl exec -it ${POD_NAME} -- nslookup ${SERVICE_NAME}.${NAMESPACE}.svc.cluster.local
```

**Solution**:

```bash
# If no endpoints, check pod labels
kubectl get pods -n ${NAMESPACE} --show-labels

# Verify service selector matches pod labels
kubectl get svc ${SERVICE_NAME} -n ${NAMESPACE} -o yaml | grep selector -A 5
```

### DNS Performance Optimization

```yaml
# Optimize pod DNS configuration
apiVersion: v1
kind: Pod
metadata:
  name: optimized-pod
spec:
  dnsPolicy: ClusterFirst
  dnsConfig:
    options:

      - name: ndots

        value: "2"  # Reduce from default 5

      - name: timeout

        value: "2"

      - name: attempts

        value: "2"
```

---

## Service Mesh Debugging

### Overview

Service mesh issues can cause traffic routing problems, authentication failures, and observability gaps. This section covers Istio/Linkerd debugging.

### Symptoms

- Requests failing with 503 errors
- mTLS authentication failures
- Traffic not routing correctly
- Missing distributed traces
- Circuit breaker not working

### Diagnostic Procedures (Istio)

#### Step 1: Check Istio Installation

```bash
# Verify Istio components
kubectl get pods -n istio-system

# Check Istio version
istioctl version

# Verify proxy injection
kubectl get namespace ${NAMESPACE} -o yaml | grep istio-injection
```

#### Step 2: Analyze Envoy Proxy Configuration

```bash
# Get proxy configuration
istioctl proxy-config cluster ${POD_NAME}.${NAMESPACE}

# Check routes
istioctl proxy-config route ${POD_NAME}.${NAMESPACE}

# Verify listeners
istioctl proxy-config listener ${POD_NAME}.${NAMESPACE}

# Check endpoints
istioctl proxy-config endpoint ${POD_NAME}.${NAMESPACE}
```

#### Step 3: Check Envoy Logs

```bash
# View Envoy sidecar logs
kubectl logs ${POD_NAME} -n ${NAMESPACE} -c istio-proxy

# Filter for errors
kubectl logs ${POD_NAME} -n ${NAMESPACE} -c istio-proxy | grep -i error

# Check access logs
kubectl logs ${POD_NAME} -n ${NAMESPACE} -c istio-proxy --tail=100 | grep "HTTP"
```

#### Step 4: Validate Service Mesh Configuration

```bash
# Analyze configuration
istioctl analyze -n ${NAMESPACE}

# Check virtual services
kubectl get virtualservice -n ${NAMESPACE}

# Check destination rules
kubectl get destinationrule -n ${NAMESPACE}

# Verify gateway configuration
kubectl get gateway -n ${NAMESPACE}
```

### Common Service Mesh Issues

#### Issue: mTLS Authentication Failures

**Symptoms**: 503 errors, "upstream connect error or disconnect/reset before headers"

**Diagnosis**:

```bash
# Check mTLS status
istioctl authn tls-check ${POD_NAME}.${NAMESPACE}

# Verify peer authentication
kubectl get peerauthentication -n ${NAMESPACE}
```

**Solution**:

```yaml
# Enable permissive mode temporarily
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: ${NAMESPACE}
spec:
  mtls:
    mode: PERMISSIVE  # Change from STRICT
```

#### Issue: Traffic Not Routing Correctly

**Diagnosis**:

```bash
# Check virtual service configuration
kubectl describe virtualservice ${VS_NAME} -n ${NAMESPACE}

# Verify destination rule
kubectl describe destinationrule ${DR_NAME} -n ${NAMESPACE}

# Test routing
istioctl proxy-config route ${POD_NAME}.${NAMESPACE} -o json | jq '.[] | select(.name | contains("${SERVICE_NAME}"))'
```

**Solution**:

```yaml
# Fix virtual service routing
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: ecommerce-backend
spec:
  hosts:

  - ecommerce-backend

  http:

  - match:
    - headers:

        version:
          exact: v2
    route:

    - destination:

        host: ecommerce-backend
        subset: v2

  - route:
    - destination:

        host: ecommerce-backend
        subset: v1
```

#### Issue: Circuit Breaker Not Working

**Diagnosis**:

```bash
# Check destination rule outlier detection
kubectl get destinationrule ${DR_NAME} -n ${NAMESPACE} -o yaml | grep -A 10 outlierDetection
```

**Solution**:

```yaml
# Configure circuit breaker
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: ecommerce-backend
spec:
  host: ecommerce-backend
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 100
      http:
        http1MaxPendingRequests: 50
        http2MaxRequests: 100
        maxRequestsPerConnection: 2
    outlierDetection:
      consecutiveErrors: 5
      interval: 30s
      baseEjectionTime: 30s
      maxEjectionPercent: 50
```

### Linkerd Debugging

```bash
# Check Linkerd installation
linkerd check

# View proxy metrics
linkerd stat deploy -n ${NAMESPACE}

# Check routes
linkerd routes svc/${SERVICE_NAME} -n ${NAMESPACE}

# View tap (live traffic)
linkerd tap deploy/${DEPLOYMENT_NAME} -n ${NAMESPACE}

# Check service profile
kubectl get serviceprofile -n ${NAMESPACE}
```

---

## Load Balancer Health Check Failures

### Overview

Load balancer health check failures can cause traffic routing issues and service unavailability.

### Symptoms

- Targets marked as unhealthy in ALB/NLB
- Intermittent 503 errors
- Traffic not reaching pods
- ELB showing OutOfService instances

### Diagnostic Procedures

#### Step 1: Check Load Balancer Status

```bash
# List load balancers
aws elbv2 describe-load-balancers --region ${REGION}

# Check target group health
aws elbv2 describe-target-health \
  --target-group-arn ${TARGET_GROUP_ARN} \
  --region ${REGION}

# View health check configuration
aws elbv2 describe-target-groups \
  --target-group-arns ${TARGET_GROUP_ARN} \
  --region ${REGION} \
  --query 'TargetGroups[0].HealthCheckPath'
```

#### Step 2: Test Health Check Endpoint

```bash
# Test from within cluster
kubectl run -it --rm debug --image=curlimages/curl --restart=Never -- \
  curl -v http://${SERVICE_NAME}.${NAMESPACE}.svc.cluster.local:8080/actuator/health

# Test from pod
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- \
  curl -v http://localhost:8080/actuator/health

# Check response time
time curl http://${SERVICE_NAME}.${NAMESPACE}.svc.cluster.local:8080/actuator/health
```

#### Step 3: Check Security Groups and Network ACLs

```bash
# Describe security group
aws ec2 describe-security-groups \
  --group-ids ${SECURITY_GROUP_ID} \
  --region ${REGION}

# Check if health check port is allowed
aws ec2 describe-security-groups \
  --group-ids ${SECURITY_GROUP_ID} \
  --region ${REGION} \
  --query 'SecurityGroups[0].IpPermissions[?FromPort==`8080`]'

# Check network ACLs
aws ec2 describe-network-acls \
  --filters "Name=association.subnet-id,Values=${SUBNET_ID}" \
  --region ${REGION}
```

### Common Load Balancer Issues

#### Issue: Health Checks Timing Out

**Diagnosis**:

```bash
# Check health check timeout settings
aws elbv2 describe-target-groups \
  --target-group-arns ${TARGET_GROUP_ARN} \
  --query 'TargetGroups[0].[HealthCheckTimeoutSeconds,HealthCheckIntervalSeconds]'

# Test endpoint response time
time curl -v http://${POD_IP}:8080/actuator/health
```

**Solution**:

```bash
# Increase health check timeout
aws elbv2 modify-target-group \
  --target-group-arn ${TARGET_GROUP_ARN} \
  --health-check-timeout-seconds 10 \
  --health-check-interval-seconds 30

# Or optimize health check endpoint
# Ensure /actuator/health responds in < 5 seconds
```

#### Issue: Wrong Health Check Path

**Diagnosis**:

```bash
# Check configured path
aws elbv2 describe-target-groups \
  --target-group-arns ${TARGET_GROUP_ARN} \
  --query 'TargetGroups[0].HealthCheckPath'

# Test the path
curl -v http://${POD_IP}:8080/health
```

**Solution**:

```bash
# Update health check path
aws elbv2 modify-target-group \
  --target-group-arn ${TARGET_GROUP_ARN} \
  --health-check-path /actuator/health
```

#### Issue: Unhealthy Threshold Too Low

**Diagnosis**:

```bash
# Check thresholds
aws elbv2 describe-target-groups \
  --target-group-arns ${TARGET_GROUP_ARN} \
  --query 'TargetGroups[0].[HealthyThresholdCount,UnhealthyThresholdCount]'
```

**Solution**:

```bash
# Adjust thresholds
aws elbv2 modify-target-group \
  --target-group-arn ${TARGET_GROUP_ARN} \
  --healthy-threshold-count 3 \
  --unhealthy-threshold-count 3
```

#### Issue: Deregistration Delay Too Long

**Diagnosis**:

```bash
# Check deregistration delay
aws elbv2 describe-target-group-attributes \
  --target-group-arn ${TARGET_GROUP_ARN} \
  --query 'Attributes[?Key==`deregistration_delay.timeout_seconds`]'
```

**Solution**:

```bash
# Reduce deregistration delay
aws elbv2 modify-target-group-attributes \
  --target-group-arn ${TARGET_GROUP_ARN} \
  --attributes Key=deregistration_delay.timeout_seconds,Value=30
```

---

## Ingress Controller Troubleshooting

### Overview

Ingress controller issues can prevent external traffic from reaching services.

### Symptoms

- 404 errors on ingress URLs
- 502/503 gateway errors
- SSL/TLS handshake failures
- Ingress not getting external IP

### Diagnostic Procedures

#### Step 1: Check Ingress Controller Status

```bash
# Check ingress controller pods
kubectl get pods -n ingress-nginx

# Check ingress controller logs
kubectl logs -n ingress-nginx -l app.kubernetes.io/name=ingress-nginx --tail=100

# Check ingress controller service
kubectl get svc -n ingress-nginx ingress-nginx-controller
```

#### Step 2: Verify Ingress Configuration

```bash
# List ingresses
kubectl get ingress -n ${NAMESPACE}

# Describe ingress
kubectl describe ingress ${INGRESS_NAME} -n ${NAMESPACE}

# Check ingress class
kubectl get ingressclass
```

#### Step 3: Test Backend Service

```bash
# Check service endpoints
kubectl get endpoints ${SERVICE_NAME} -n ${NAMESPACE}

# Test service directly
kubectl run -it --rm debug --image=curlimages/curl --restart=Never -- \
  curl -v http://${SERVICE_NAME}.${NAMESPACE}.svc.cluster.local:8080
```

### Common Ingress Issues

#### Issue: Ingress Not Getting External IP

**Diagnosis**:

```bash
# Check ingress status
kubectl get ingress ${INGRESS_NAME} -n ${NAMESPACE}

# Check load balancer service
kubectl get svc -n ingress-nginx ingress-nginx-controller

# Check AWS load balancer
aws elbv2 describe-load-balancers --region ${REGION}
```

**Solution**:

```bash
# Verify service type is LoadBalancer
kubectl get svc -n ingress-nginx ingress-nginx-controller -o yaml | grep type

# Check for service annotations
kubectl describe svc -n ingress-nginx ingress-nginx-controller | grep Annotations -A 10
```

#### Issue: 404 Not Found Errors

**Diagnosis**:

```bash
# Check ingress rules
kubectl get ingress ${INGRESS_NAME} -n ${NAMESPACE} -o yaml

# Verify path configuration
kubectl describe ingress ${INGRESS_NAME} -n ${NAMESPACE} | grep -A 10 Rules

# Check nginx configuration
kubectl exec -n ingress-nginx ${NGINX_POD} -- cat /etc/nginx/nginx.conf | grep -A 20 "server_name ${HOST}"
```

**Solution**:

```yaml
# Fix ingress path configuration
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ecommerce-ingress
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  ingressClassName: nginx
  rules:

  - host: api.ecommerce.com

    http:
      paths:

      - path: /api

        pathType: Prefix
        backend:
          service:
            name: ecommerce-backend
            port:
              number: 8080
```

#### Issue: 502 Bad Gateway Errors

**Diagnosis**:

```bash
# Check backend service health
kubectl get endpoints ${SERVICE_NAME} -n ${NAMESPACE}

# Test backend directly
kubectl port-forward svc/${SERVICE_NAME} 8080:8080 -n ${NAMESPACE}
curl http://localhost:8080

# Check ingress controller logs
kubectl logs -n ingress-nginx -l app.kubernetes.io/name=ingress-nginx | grep "upstream"
```

**Solution**:

```bash
# Verify service selector matches pods
kubectl get svc ${SERVICE_NAME} -n ${NAMESPACE} -o yaml | grep selector -A 5
kubectl get pods -n ${NAMESPACE} --show-labels

# Check pod readiness
kubectl get pods -n ${NAMESPACE} -o wide
```

#### Issue: SSL/TLS Certificate Problems

**Diagnosis**:

```bash
# Check TLS secret
kubectl get secret ${TLS_SECRET_NAME} -n ${NAMESPACE}

# Verify certificate
kubectl get secret ${TLS_SECRET_NAME} -n ${NAMESPACE} -o jsonpath='{.data.tls\.crt}' | base64 -d | openssl x509 -text -noout

# Check certificate expiration
kubectl get secret ${TLS_SECRET_NAME} -n ${NAMESPACE} -o jsonpath='{.data.tls\.crt}' | base64 -d | openssl x509 -enddate -noout
```

**Solution**:

```bash
# Renew certificate with cert-manager
kubectl delete certificaterequest ${CERT_REQUEST_NAME} -n ${NAMESPACE}
kubectl annotate certificate ${CERT_NAME} -n ${NAMESPACE} cert-manager.io/issue-temporary-certificate="true"

# Or manually update secret
kubectl create secret tls ${TLS_SECRET_NAME} \
  --cert=path/to/tls.crt \
  --key=path/to/tls.key \
  -n ${NAMESPACE} \
  --dry-run=client -o yaml | kubectl apply -f -
```

---

## Certificate and TLS Issues

### Overview

Certificate and TLS issues can cause secure connection failures and authentication problems.

### Symptoms

- SSL handshake failures
- Certificate verification errors
- "Certificate has expired" errors
- "Certificate name mismatch" errors
- mTLS authentication failures

### Diagnostic Procedures

#### Step 1: Check Certificate Status

```bash
# Check certificate in Kubernetes
kubectl get certificate -n ${NAMESPACE}

# Describe certificate
kubectl describe certificate ${CERT_NAME} -n ${NAMESPACE}

# Check certificate secret
kubectl get secret ${TLS_SECRET_NAME} -n ${NAMESPACE} -o yaml
```

#### Step 2: Verify Certificate Details

```bash
# Extract and view certificate
kubectl get secret ${TLS_SECRET_NAME} -n ${NAMESPACE} \
  -o jsonpath='{.data.tls\.crt}' | base64 -d | openssl x509 -text -noout

# Check certificate chain
kubectl get secret ${TLS_SECRET_NAME} -n ${NAMESPACE} \
  -o jsonpath='{.data.tls\.crt}' | base64 -d | openssl x509 -noout -subject -issuer

# Verify certificate expiration
kubectl get secret ${TLS_SECRET_NAME} -n ${NAMESPACE} \
  -o jsonpath='{.data.tls\.crt}' | base64 -d | openssl x509 -noout -dates
```

#### Step 3: Test TLS Connection

```bash
# Test TLS handshake
openssl s_client -connect ${HOST}:443 -servername ${HOST}

# Test with specific TLS version
openssl s_client -connect ${HOST}:443 -tls1_2

# Check certificate chain
openssl s_client -connect ${HOST}:443 -showcerts
```

### Common Certificate Issues

#### Issue: Certificate Expired

**Diagnosis**:

```bash
# Check expiration date
kubectl get secret ${TLS_SECRET_NAME} -n ${NAMESPACE} \
  -o jsonpath='{.data.tls\.crt}' | base64 -d | openssl x509 -noout -enddate

# Check cert-manager certificate status
kubectl describe certificate ${CERT_NAME} -n ${NAMESPACE} | grep "Not After"
```

**Solution**:

```bash
# Trigger certificate renewal with cert-manager
kubectl delete certificaterequest -n ${NAMESPACE} -l cert-manager.io/certificate-name=${CERT_NAME}

# Force renewal
kubectl annotate certificate ${CERT_NAME} -n ${NAMESPACE} \
  cert-manager.io/issue-temporary-certificate="true" --overwrite

# Check renewal status
kubectl get certificaterequest -n ${NAMESPACE} -w
```

#### Issue: Certificate Name Mismatch

**Diagnosis**:

```bash
# Check certificate CN and SANs
kubectl get secret ${TLS_SECRET_NAME} -n ${NAMESPACE} \
  -o jsonpath='{.data.tls\.crt}' | base64 -d | \
  openssl x509 -noout -text | grep -A 1 "Subject Alternative Name"

# Compare with ingress host
kubectl get ingress ${INGRESS_NAME} -n ${NAMESPACE} -o yaml | grep host
```

**Solution**:

```yaml
# Update certificate with correct SANs
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: ecommerce-tls
  namespace: production
spec:
  secretName: ecommerce-tls-secret
  dnsNames:

  - api.ecommerce.com
  - www.ecommerce.com
  - ecommerce.com

  issuerRef:
    name: letsencrypt-prod
    kind: ClusterIssuer
```

#### Issue: Certificate Chain Incomplete

**Diagnosis**:

```bash
# Check certificate chain
openssl s_client -connect ${HOST}:443 -showcerts | grep "Certificate chain"

# Verify with SSL Labs
# https://www.ssllabs.com/ssltest/analyze.html?d=${HOST}
```

**Solution**:

```bash
# Ensure cert-manager includes full chain
kubectl get clusterissuer letsencrypt-prod -o yaml | grep -A 5 "acme"

# Manually add intermediate certificates if needed
cat server.crt intermediate.crt > fullchain.crt
kubectl create secret tls ${TLS_SECRET_NAME} \
  --cert=fullchain.crt \
  --key=server.key \
  -n ${NAMESPACE}
```

#### Issue: mTLS Certificate Validation Failures

**Diagnosis**:

```bash
# Check Istio mTLS configuration
kubectl get peerauthentication -n ${NAMESPACE}

# Verify certificates
istioctl proxy-config secret ${POD_NAME}.${NAMESPACE}

# Check certificate rotation
kubectl logs ${POD_NAME} -n ${NAMESPACE} -c istio-proxy | grep certificate
```

**Solution**:

```yaml
# Configure mTLS properly
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: production
spec:
  mtls:
    mode: STRICT
---
apiVersion: security.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: default
  namespace: production
spec:
  host: "*.production.svc.cluster.local"
  trafficPolicy:
    tls:
      mode: ISTIO_MUTUAL
```

---

## Network Policy Debugging

### Overview

Network policies control traffic flow between pods and can cause connectivity issues if misconfigured.

### Symptoms

- Connection refused between services
- Timeout errors between pods
- External traffic blocked
- Egress traffic failures

### Diagnostic Procedures

#### Step 1: Check Network Policies

```bash
# List network policies
kubectl get networkpolicy -n ${NAMESPACE}

# Describe network policy
kubectl describe networkpolicy ${POLICY_NAME} -n ${NAMESPACE}

# Check if namespace has any policies
kubectl get networkpolicy -n ${NAMESPACE} --no-headers | wc -l
```

#### Step 2: Test Connectivity

```bash
# Test pod-to-pod connectivity
kubectl exec -it ${SOURCE_POD} -n ${NAMESPACE} -- \
  curl -v http://${TARGET_POD_IP}:8080

# Test service connectivity
kubectl exec -it ${SOURCE_POD} -n ${NAMESPACE} -- \
  curl -v http://${SERVICE_NAME}.${TARGET_NAMESPACE}.svc.cluster.local:8080

# Test external connectivity
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- \
  curl -v https://api.external.com
```

#### Step 3: Analyze Policy Rules

```bash
# Get policy in YAML format
kubectl get networkpolicy ${POLICY_NAME} -n ${NAMESPACE} -o yaml

# Check ingress rules
kubectl get networkpolicy ${POLICY_NAME} -n ${NAMESPACE} \
  -o jsonpath='{.spec.ingress[*]}'

# Check egress rules
kubectl get networkpolicy ${POLICY_NAME} -n ${NAMESPACE} \
  -o jsonpath='{.spec.egress[*]}'
```

### Common Network Policy Issues

#### Issue: Default Deny Blocking Traffic

**Diagnosis**:

```bash
# Check for default deny policies
kubectl get networkpolicy -n ${NAMESPACE} -o yaml | grep "podSelector: {}"

# Test if traffic is blocked
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- \
  curl -v --max-time 5 http://${TARGET_SERVICE}:8080
```

**Solution**:

```yaml
# Add explicit allow policy
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-ecommerce-backend
  namespace: production
spec:
  podSelector:
    matchLabels:
      app: ecommerce-backend
  policyTypes:

  - Ingress
  - Egress

  ingress:

  - from:
    - podSelector:

        matchLabels:
          app: ecommerce-frontend
    ports:

    - protocol: TCP

      port: 8080
  egress:

  - to:
    - podSelector:

        matchLabels:
          app: postgres
    ports:

    - protocol: TCP

      port: 5432

  - to:  # Allow DNS
    - namespaceSelector:

        matchLabels:
          name: kube-system
    ports:

    - protocol: UDP

      port: 53
```

#### Issue: Missing DNS Egress Rule

**Diagnosis**:

```bash
# Test DNS resolution
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- nslookup kubernetes.default

# Check egress rules for DNS
kubectl get networkpolicy -n ${NAMESPACE} -o yaml | grep -A 10 "port: 53"
```

**Solution**:

```yaml
# Add DNS egress rule
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-dns-egress
  namespace: production
spec:
  podSelector: {}
  policyTypes:

  - Egress

  egress:

  - to:
    - namespaceSelector:

        matchLabels:
          name: kube-system
    ports:

    - protocol: UDP

      port: 53

    - protocol: TCP

      port: 53
```

#### Issue: Namespace Selector Not Matching

**Diagnosis**:

```bash
# Check namespace labels
kubectl get namespace ${NAMESPACE} --show-labels

# Check policy namespace selector
kubectl get networkpolicy ${POLICY_NAME} -n ${NAMESPACE} \
  -o jsonpath='{.spec.ingress[*].from[*].namespaceSelector}'
```

**Solution**:

```bash
# Add required label to namespace
kubectl label namespace ${NAMESPACE} name=${NAMESPACE}

# Or update policy to match existing labels
kubectl edit networkpolicy ${POLICY_NAME} -n ${NAMESPACE}
```

#### Issue: Pod Selector Not Matching

**Diagnosis**:

```bash
# Check pod labels
kubectl get pods -n ${NAMESPACE} --show-labels

# Check policy pod selector
kubectl get networkpolicy ${POLICY_NAME} -n ${NAMESPACE} \
  -o jsonpath='{.spec.podSelector}'

# Find pods matching selector
kubectl get pods -n ${NAMESPACE} -l app=ecommerce-backend
```

**Solution**:

```bash
# Add required labels to pods
kubectl label pod ${POD_NAME} -n ${NAMESPACE} app=ecommerce-backend

# Or update deployment to include labels
kubectl patch deployment ${DEPLOYMENT_NAME} -n ${NAMESPACE} \
  -p '{"spec":{"template":{"metadata":{"labels":{"app":"ecommerce-backend"}}}}}'
```

### Network Policy Testing Tools

```bash
# Install network policy testing tool
kubectl apply -f https://raw.githubusercontent.com/ahmetb/kubectl-plugins/master/net-forward/net-forward.yaml

# Test connectivity matrix
kubectl run -it --rm netshoot --image=nicolaka/netshoot -- bash
# Inside container:
curl -v http://${TARGET_SERVICE}:${PORT}
nc -zv ${TARGET_SERVICE} ${PORT}
```

---

## Cross-Region Connectivity

### Overview

Cross-region connectivity issues can affect disaster recovery, data replication, and multi-region deployments.

### Symptoms

- High latency between regions
- Connection timeouts across regions
- Replication lag
- Data sync failures
- Cross-region API calls failing

### Diagnostic Procedures

#### Step 1: Test Cross-Region Connectivity

```bash
# Test from one region to another
aws ec2 describe-instances \
  --region us-east-1 \
  --filters "Name=tag:Name,Values=ecommerce-backend" \
  --query 'Reservations[*].Instances[*].[PrivateIpAddress]' \
  --output text

# Test connectivity
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- \
  curl -v --max-time 10 http://${REMOTE_REGION_ENDPOINT}
```

#### Step 2: Check VPC Peering Status

```bash
# List VPC peering connections
aws ec2 describe-vpc-peering-connections \
  --region ${REGION} \
  --filters "Name=status-code,Values=active"

# Check peering connection details
aws ec2 describe-vpc-peering-connections \
  --vpc-peering-connection-ids ${PEERING_ID} \
  --region ${REGION}

# Verify route tables
aws ec2 describe-route-tables \
  --filters "Name=vpc-id,Values=${VPC_ID}" \
  --region ${REGION}
```

#### Step 3: Test Latency and Throughput

```bash
# Measure latency
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- \
  ping -c 10 ${REMOTE_REGION_IP}

# Test throughput
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- \
  iperf3 -c ${REMOTE_REGION_IP} -t 30

# Check network performance
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- \
  curl -w "@curl-format.txt" -o /dev/null -s http://${REMOTE_ENDPOINT}
```

### Common Cross-Region Issues

#### Issue: High Latency Between Regions

**Diagnosis**:

```bash
# Measure round-trip time
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- \
  ping -c 100 ${REMOTE_REGION_IP} | tail -1

# Check route
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- \
  traceroute ${REMOTE_REGION_IP}

# Test with different packet sizes
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- \
  ping -c 10 -s 1400 ${REMOTE_REGION_IP}
```

**Solution**:

```bash
# Use AWS Global Accelerator for optimized routing
aws globalaccelerator create-accelerator \
  --name ecommerce-accelerator \
  --ip-address-type IPV4 \
  --enabled

# Or use CloudFront for content delivery
aws cloudfront create-distribution \
  --origin-domain-name ${ORIGIN_DOMAIN} \
  --default-root-object index.html
```

#### Issue: VPC Peering Route Not Propagating

**Diagnosis**:

```bash
# Check route table entries
aws ec2 describe-route-tables \
  --route-table-ids ${ROUTE_TABLE_ID} \
  --region ${REGION} \
  --query 'RouteTables[*].Routes'

# Verify peering connection is active
aws ec2 describe-vpc-peering-connections \
  --vpc-peering-connection-ids ${PEERING_ID} \
  --query 'VpcPeeringConnections[*].Status'
```

**Solution**:

```bash
# Add route to peering connection
aws ec2 create-route \
  --route-table-id ${ROUTE_TABLE_ID} \
  --destination-cidr-block ${REMOTE_VPC_CIDR} \
  --vpc-peering-connection-id ${PEERING_ID} \
  --region ${REGION}

# Verify route was added
aws ec2 describe-route-tables \
  --route-table-ids ${ROUTE_TABLE_ID} \
  --region ${REGION}
```

#### Issue: Security Groups Blocking Cross-Region Traffic

**Diagnosis**:

```bash
# Check security group rules
aws ec2 describe-security-groups \
  --group-ids ${SECURITY_GROUP_ID} \
  --region ${REGION} \
  --query 'SecurityGroups[*].IpPermissions'

# Test connectivity
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- \
  telnet ${REMOTE_REGION_IP} ${PORT}
```

**Solution**:

```bash
# Add ingress rule for remote VPC CIDR
aws ec2 authorize-security-group-ingress \
  --group-id ${SECURITY_GROUP_ID} \
  --protocol tcp \
  --port ${PORT} \
  --cidr ${REMOTE_VPC_CIDR} \
  --region ${REGION}
```

#### Issue: Database Replication Lag

**Diagnosis**:

```bash
# Check replication lag
aws rds describe-db-instances \
  --db-instance-identifier ${DB_INSTANCE_ID} \
  --region ${REGION} \
  --query 'DBInstances[*].ReadReplicaDBInstanceIdentifiers'

# Monitor replication lag metric
aws cloudwatch get-metric-statistics \
  --namespace AWS/RDS \
  --metric-name ReplicaLag \
  --dimensions Name=DBInstanceIdentifier,Value=${REPLICA_ID} \
  --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 300 \
  --statistics Average \
  --region ${REGION}
```

**Solution**:

```bash
# Increase replica instance size
aws rds modify-db-instance \
  --db-instance-identifier ${REPLICA_ID} \
  --db-instance-class db.r5.2xlarge \
  --apply-immediately \
  --region ${REGION}

# Or reduce write load on primary
# Implement read-write splitting in application
```

---

## VPC Peering and Transit Gateway

### Overview

VPC peering and Transit Gateway issues can affect multi-VPC and multi-region connectivity.

### Symptoms

- Cannot reach resources in peered VPC
- Transit Gateway routes not working
- Asymmetric routing issues
- Overlapping CIDR blocks

### Diagnostic Procedures

#### Step 1: Verify VPC Peering Configuration

```bash
# Check peering connection status
aws ec2 describe-vpc-peering-connections \
  --filters "Name=status-code,Values=active" \
  --region ${REGION}

# Verify both sides accepted
aws ec2 describe-vpc-peering-connections \
  --vpc-peering-connection-ids ${PEERING_ID} \
  --query 'VpcPeeringConnections[*].[Status.Code,AccepterVpcInfo.VpcId,RequesterVpcInfo.VpcId]'
```

#### Step 2: Check Transit Gateway Attachments

```bash
# List Transit Gateway attachments
aws ec2 describe-transit-gateway-attachments \
  --filters "Name=transit-gateway-id,Values=${TGW_ID}" \
  --region ${REGION}

# Check attachment state
aws ec2 describe-transit-gateway-vpc-attachments \
  --transit-gateway-attachment-ids ${ATTACHMENT_ID} \
  --region ${REGION}

# Verify route tables
aws ec2 describe-transit-gateway-route-tables \
  --transit-gateway-route-table-ids ${TGW_ROUTE_TABLE_ID} \
  --region ${REGION}
```

#### Step 3: Verify Route Propagation

```bash
# Check VPC route tables
aws ec2 describe-route-tables \
  --filters "Name=vpc-id,Values=${VPC_ID}" \
  --region ${REGION} \
  --query 'RouteTables[*].Routes'

# Check Transit Gateway route table
aws ec2 search-transit-gateway-routes \
  --transit-gateway-route-table-id ${TGW_ROUTE_TABLE_ID} \
  --filters "Name=state,Values=active" \
  --region ${REGION}
```

### Common VPC Peering Issues

#### Issue: Peering Connection Pending Acceptance

**Diagnosis**:

```bash
# Check peering status
aws ec2 describe-vpc-peering-connections \
  --vpc-peering-connection-ids ${PEERING_ID} \
  --query 'VpcPeeringConnections[*].Status'
```

**Solution**:

```bash
# Accept peering connection (from accepter account)
aws ec2 accept-vpc-peering-connection \
  --vpc-peering-connection-id ${PEERING_ID} \
  --region ${ACCEPTER_REGION}
```

#### Issue: Overlapping CIDR Blocks

**Diagnosis**:

```bash
# Check VPC CIDR blocks
aws ec2 describe-vpcs \
  --vpc-ids ${VPC_ID_1} ${VPC_ID_2} \
  --query 'Vpcs[*].[VpcId,CidrBlock]'
```

**Solution**:

```text
Cannot peer VPCs with overlapping CIDR blocks.
Options:

1. Use Transit Gateway with NAT
2. Create new VPC with non-overlapping CIDR
3. Use AWS PrivateLink for specific services

```

#### Issue: Routes Not Propagating to VPC Route Tables

**Diagnosis**:

```bash
# Check if routes exist
aws ec2 describe-route-tables \
  --route-table-ids ${ROUTE_TABLE_ID} \
  --region ${REGION} \
  --query 'RouteTables[*].Routes[?VpcPeeringConnectionId==`'${PEERING_ID}'`]'
```

**Solution**:

```bash
# Add routes to both VPC route tables
# In VPC A
aws ec2 create-route \
  --route-table-id ${VPC_A_ROUTE_TABLE_ID} \
  --destination-cidr-block ${VPC_B_CIDR} \
  --vpc-peering-connection-id ${PEERING_ID} \
  --region ${REGION_A}

# In VPC B
aws ec2 create-route \
  --route-table-id ${VPC_B_ROUTE_TABLE_ID} \
  --destination-cidr-block ${VPC_A_CIDR} \
  --vpc-peering-connection-id ${PEERING_ID} \
  --region ${REGION_B}
```

### Common Transit Gateway Issues

#### Issue: Transit Gateway Attachment Not Available

**Diagnosis**:

```bash
# Check attachment state
aws ec2 describe-transit-gateway-vpc-attachments \
  --transit-gateway-attachment-ids ${ATTACHMENT_ID} \
  --query 'TransitGatewayVpcAttachments[*].[State,VpcId]'

# Check for errors
aws ec2 describe-transit-gateway-vpc-attachments \
  --transit-gateway-attachment-ids ${ATTACHMENT_ID} \
  --query 'TransitGatewayVpcAttachments[*].Tags[?Key==`Error`]'
```

**Solution**:

```bash
# Delete and recreate attachment if stuck
aws ec2 delete-transit-gateway-vpc-attachment \
  --transit-gateway-attachment-id ${ATTACHMENT_ID} \
  --region ${REGION}

# Create new attachment
aws ec2 create-transit-gateway-vpc-attachment \
  --transit-gateway-id ${TGW_ID} \
  --vpc-id ${VPC_ID} \
  --subnet-ids ${SUBNET_ID_1} ${SUBNET_ID_2} \
  --region ${REGION}
```

#### Issue: Transit Gateway Routes Not Propagating

**Diagnosis**:

```bash
# Check route propagation
aws ec2 get-transit-gateway-route-table-propagations \
  --transit-gateway-route-table-id ${TGW_ROUTE_TABLE_ID} \
  --region ${REGION}

# Check route table associations
aws ec2 get-transit-gateway-route-table-associations \
  --transit-gateway-route-table-id ${TGW_ROUTE_TABLE_ID} \
  --region ${REGION}
```

**Solution**:

```bash
# Enable route propagation
aws ec2 enable-transit-gateway-route-table-propagation \
  --transit-gateway-route-table-id ${TGW_ROUTE_TABLE_ID} \
  --transit-gateway-attachment-id ${ATTACHMENT_ID} \
  --region ${REGION}

# Associate attachment with route table
aws ec2 associate-transit-gateway-route-table \
  --transit-gateway-route-table-id ${TGW_ROUTE_TABLE_ID} \
  --transit-gateway-attachment-id ${ATTACHMENT_ID} \
  --region ${REGION}
```

#### Issue: Blackhole Routes in Transit Gateway

**Diagnosis**:

```bash
# Search for blackhole routes
aws ec2 search-transit-gateway-routes \
  --transit-gateway-route-table-id ${TGW_ROUTE_TABLE_ID} \
  --filters "Name=state,Values=blackhole" \
  --region ${REGION}
```

**Solution**:

```bash
# Delete blackhole route
aws ec2 delete-transit-gateway-route \
  --transit-gateway-route-table-id ${TGW_ROUTE_TABLE_ID} \
  --destination-cidr-block ${CIDR_BLOCK} \
  --region ${REGION}

# Verify attachment is available
aws ec2 describe-transit-gateway-vpc-attachments \
  --transit-gateway-attachment-ids ${ATTACHMENT_ID} \
  --query 'TransitGatewayVpcAttachments[*].State'
```

---

## Common Network Commands

### Connectivity Testing

```bash
# Basic connectivity test
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- curl -v http://${TARGET}

# Test with timeout
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- curl -v --max-time 5 http://${TARGET}

# Test specific port
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- nc -zv ${HOST} ${PORT}

# Trace route
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- traceroute ${HOST}

# DNS lookup
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- nslookup ${HOST}

# Ping test
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- ping -c 5 ${HOST}
```

### Network Debugging Tools

```bash
# Run debug pod with network tools
kubectl run -it --rm debug --image=nicolaka/netshoot --restart=Never -- bash

# Inside debug pod:
# - curl: HTTP testing
# - nc (netcat): Port testing
# - nslookup/dig: DNS testing
# - traceroute: Route tracing
# - tcpdump: Packet capture
# - iperf3: Bandwidth testing
# - mtr: Network diagnostics
```

### Service and Endpoint Checks

```bash
# List services
kubectl get svc -n ${NAMESPACE}

# Check service endpoints
kubectl get endpoints ${SERVICE_NAME} -n ${NAMESPACE}

# Describe service
kubectl describe svc ${SERVICE_NAME} -n ${NAMESPACE}

# Test service from within cluster
kubectl run -it --rm debug --image=curlimages/curl --restart=Never -- \
  curl http://${SERVICE_NAME}.${NAMESPACE}.svc.cluster.local:${PORT}
```

### Network Policy Checks

```bash
# List network policies
kubectl get networkpolicy -n ${NAMESPACE}

# Describe policy
kubectl describe networkpolicy ${POLICY_NAME} -n ${NAMESPACE}

# Get policy YAML
kubectl get networkpolicy ${POLICY_NAME} -n ${NAMESPACE} -o yaml
```

### Load Balancer and Ingress

```bash
# Check ingress
kubectl get ingress -n ${NAMESPACE}

# Describe ingress
kubectl describe ingress ${INGRESS_NAME} -n ${NAMESPACE}

# Check ingress controller logs
kubectl logs -n ingress-nginx -l app.kubernetes.io/name=ingress-nginx

# Test load balancer
curl -v http://${LOAD_BALANCER_DNS}
```

### Certificate Checks

```bash
# Check certificate
kubectl get certificate -n ${NAMESPACE}

# Describe certificate
kubectl describe certificate ${CERT_NAME} -n ${NAMESPACE}

# View certificate details
kubectl get secret ${TLS_SECRET_NAME} -n ${NAMESPACE} \
  -o jsonpath='{.data.tls\.crt}' | base64 -d | openssl x509 -text -noout

# Test TLS connection
openssl s_client -connect ${HOST}:443 -servername ${HOST}
```

---

## Quick Reference

### Network Troubleshooting Checklist

- [ ] Check DNS resolution (CoreDNS status, resolv.conf)
- [ ] Verify service endpoints exist
- [ ] Test pod-to-pod connectivity
- [ ] Check network policies
- [ ] Verify security groups and NACLs
- [ ] Test load balancer health checks
- [ ] Check ingress configuration
- [ ] Verify TLS certificates
- [ ] Test cross-region connectivity
- [ ] Check VPC peering/Transit Gateway routes

### Common Error Messages

| Error | Likely Cause | Quick Fix |
|-------|--------------|-----------|
| "Name or service not known" | DNS resolution failure | Check CoreDNS, verify service exists |
| "Connection refused" | Service not listening | Check pod status, verify port |
| "Connection timeout" | Network policy or firewall | Check network policies, security groups |
| "503 Service Unavailable" | No healthy backends | Check pod health, endpoints |
| "Certificate has expired" | Expired TLS certificate | Renew certificate with cert-manager |
| "SSL handshake failed" | TLS configuration issue | Check certificate chain, TLS version |
| "Upstream connect error" | Backend service down | Check service health, logs |

---

## Related Documentation

- [Service Outage Runbook](../runbooks/service-outage.md) - Service outage response procedures
- [Deployment Architecture](../../viewpoints/deployment/physical-architecture.md) - Infrastructure architecture
- [Network Architecture](../../viewpoints/deployment/network-architecture.md) - Network topology and design
- [Monitoring Strategy](../monitoring/monitoring-strategy.md) - Network monitoring setup
- [Common Issues](common-issues.md) - Quick solutions for common problems

---

**Last Updated**: 2025-10-25  
**Owner**: DevOps Team  
**Review Cycle**: Quarterly
