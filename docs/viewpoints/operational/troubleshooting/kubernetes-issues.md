# Kubernetes Troubleshooting Guide

## Overview

This document provides comprehensive troubleshooting procedures for Kubernetes-specific issues in the Enterprise E-Commerce Platform. It covers pod scheduling, persistent volumes, ConfigMaps, Secrets, service discovery, autoscaling, node issues, and etcd health.

**Target Audience**: DevOps engineers, SREs, Kubernetes administrators  
**Prerequisites**: Access to Kubernetes cluster, kubectl, cluster admin permissions  
**Related Documents**:

- [Common Issues](common-issues.md)
- [Application Debugging Guide](application-debugging.md)
- [Network and Connectivity Guide](network-connectivity.md)

---

## Table of Contents

1. [Pod Scheduling Failures](#pod-scheduling-failures)
2. [Persistent Volume Claim Issues](#persistent-volume-claim-issues)
3. [ConfigMap and Secret Mounting Problems](#configmap-and-secret-mounting-problems)
4. [Service Discovery Failures](#service-discovery-failures)
5. [Horizontal Pod Autoscaler Troubleshooting](#horizontal-pod-autoscaler-troubleshooting)
6. [Cluster Autoscaler Issues](#cluster-autoscaler-issues)
7. [Node NotReady Troubleshooting](#node-notready-troubleshooting)
8. [etcd Performance and Health Issues](#etcd-performance-and-health-issues)

---

## Pod Scheduling Failures

### Overview

Pod scheduling failures occur when the Kubernetes scheduler cannot find a suitable node to run a pod. This can be due to resource constraints, node affinity rules, taints/tolerations, or other scheduling policies.

### Symptoms

- Pods stuck in `Pending` state
- Events showing "FailedScheduling"
- "Insufficient cpu/memory" messages
- "No nodes available" errors
- Pods not starting after deployment

### Diagnostic Procedures

#### Step 1: Check Pod Status and Events

```bash
# Get pod status
kubectl get pods -n production -l app=ecommerce-backend

# Describe pod to see events
kubectl describe pod ${POD_NAME} -n production

# Get scheduling events
kubectl get events -n production --field-selector involvedObject.name=${POD_NAME} --sort-by='.lastTimestamp'

# Filter for scheduling failures
kubectl get events -n production --field-selector reason=FailedScheduling
```

**Common Event Messages**:

- `0/3 nodes are available: 3 Insufficient cpu.`
- `0/3 nodes are available: 3 node(s) didn't match node selector.`
- `0/3 nodes are available: 3 node(s) had taint {key: value}, that the pod didn't tolerate.`
- `0/3 nodes are available: 3 node(s) didn't match pod affinity rules.`

#### Step 2: Check Resource Requests and Limits

```bash
# Check pod resource requests
kubectl get pod ${POD_NAME} -n production -o jsonpath='{.spec.containers[*].resources}'

# Check node available resources
kubectl describe nodes | grep -A 5 "Allocated resources"

# Get node capacity and allocatable resources
kubectl get nodes -o custom-columns=NAME:.metadata.name,CPU-CAPACITY:.status.capacity.cpu,CPU-ALLOCATABLE:.status.allocatable.cpu,MEMORY-CAPACITY:.status.capacity.memory,MEMORY-ALLOCATABLE:.status.allocatable.memory
```

#### Step 3: Check Node Affinity and Selectors

```bash
# Check pod node selector
kubectl get pod ${POD_NAME} -n production -o jsonpath='{.spec.nodeSelector}'

# Check pod affinity rules
kubectl get pod ${POD_NAME} -n production -o jsonpath='{.spec.affinity}'

# List nodes with labels
kubectl get nodes --show-labels

# Check if nodes match selector
kubectl get nodes -l environment=production
```

#### Step 4: Check Taints and Tolerations

```bash
# Check node taints
kubectl describe nodes | grep -A 3 "Taints:"

# Check pod tolerations
kubectl get pod ${POD_NAME} -n production -o jsonpath='{.spec.tolerations}'

# List all node taints
kubectl get nodes -o custom-columns=NAME:.metadata.name,TAINTS:.spec.taints
```

#### Step 5: Check Pod Priority and Preemption

```bash
# Check pod priority class
kubectl get pod ${POD_NAME} -n production -o jsonpath='{.spec.priorityClassName}'

# List priority classes
kubectl get priorityclasses

# Check if pod was preempted
kubectl get events -n production --field-selector reason=Preempted
```

### Common Scheduling Issues and Solutions

#### Issue 1: Insufficient CPU/Memory

**Problem**: Nodes don't have enough resources to schedule the pod

**Diagnosis**:

```bash
# Check total cluster resources
kubectl top nodes

# Check pod resource requests
kubectl describe pod ${POD_NAME} -n production | grep -A 5 "Requests:"

# Calculate total requested resources
kubectl describe nodes | grep -A 5 "Allocated resources:" | grep -E "cpu|memory"
```

**Solutions**:

**Option 1: Reduce Resource Requests**

```yaml
# deployment.yaml
resources:
  requests:
    cpu: 500m      # Reduced from 1000m
    memory: 512Mi  # Reduced from 1Gi
  limits:
    cpu: 1000m
    memory: 1Gi
```

**Option 2: Add More Nodes**

```bash
# For EKS with cluster autoscaler
# Increase max size in node group configuration
aws eks update-nodegroup-config \
  --cluster-name ecommerce-cluster \
  --nodegroup-name ecommerce-nodes \
  --scaling-config minSize=3,maxSize=10,desiredSize=5

# For manual scaling
kubectl scale deployment cluster-autoscaler \
  --replicas=1 -n kube-system
```

**Option 3: Use Cluster Autoscaler**

```yaml
# Enable cluster autoscaler
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cluster-autoscaler
  namespace: kube-system
spec:
  template:
    spec:
      containers:

      - name: cluster-autoscaler

        image: k8s.gcr.io/autoscaling/cluster-autoscaler:v1.27.0
        command:

          - ./cluster-autoscaler
          - --cloud-provider=aws
          - --namespace=kube-system
          - --node-group-auto-discovery=asg:tag=k8s.io/cluster-autoscaler/enabled,k8s.io/cluster-autoscaler/ecommerce-cluster

```

#### Issue 2: Node Selector Mismatch

**Problem**: Pod requires specific node labels that don't exist

**Diagnosis**:

```bash
# Check pod node selector
kubectl get pod ${POD_NAME} -n production -o yaml | grep -A 5 nodeSelector

# Check available node labels
kubectl get nodes --show-labels | grep -i "environment\|workload"
```

**Solutions**:

**Option 1: Add Labels to Nodes**

```bash
# Add missing label to node
kubectl label nodes ${NODE_NAME} environment=production

# Add multiple labels
kubectl label nodes ${NODE_NAME} workload-type=compute-intensive tier=backend
```

**Option 2: Remove or Modify Node Selector**

```yaml
# deployment.yaml - Remove node selector
spec:
  template:
    spec:
      # nodeSelector:  # Commented out
      #   environment: production
      containers:

      - name: app

```

**Option 3: Use Node Affinity (More Flexible)**

```yaml
spec:
  template:
    spec:
      affinity:
        nodeAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:  # Soft requirement

          - weight: 100

            preference:
              matchExpressions:

              - key: environment

                operator: In
                values:

                - production

```

#### Issue 3: Taint/Toleration Mismatch

**Problem**: Nodes have taints that pods don't tolerate

**Diagnosis**:

```bash
# Check node taints
kubectl describe node ${NODE_NAME} | grep -A 3 "Taints:"

# Check pod tolerations
kubectl get pod ${POD_NAME} -n production -o yaml | grep -A 10 tolerations
```

**Solutions**:

**Option 1: Add Toleration to Pod**

```yaml
# deployment.yaml
spec:
  template:
    spec:
      tolerations:

      - key: "dedicated"

        operator: "Equal"
        value: "backend"
        effect: "NoSchedule"

      - key: "node.kubernetes.io/not-ready"

        operator: "Exists"
        effect: "NoExecute"
        tolerationSeconds: 300
```

**Option 2: Remove Taint from Node**

```bash
# Remove specific taint
kubectl taint nodes ${NODE_NAME} dedicated=backend:NoSchedule-

# Remove all taints
kubectl taint nodes ${NODE_NAME} dedicated-
```

**Option 3: Use Taint-Based Eviction**

```yaml
# For temporary taints during maintenance
tolerations:

- key: "node.kubernetes.io/unreachable"

  operator: "Exists"
  effect: "NoExecute"
  tolerationSeconds: 30
```

#### Issue 4: Pod Affinity/Anti-Affinity Rules

**Problem**: Pod affinity rules prevent scheduling

**Diagnosis**:

```bash
# Check pod affinity rules
kubectl get pod ${POD_NAME} -n production -o yaml | grep -A 20 affinity

# Check existing pod distribution
kubectl get pods -n production -o wide --show-labels
```

**Solutions**:

**Option 1: Use Preferred Instead of Required**

```yaml
# Change from required to preferred
spec:
  template:
    spec:
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:  # Soft rule

          - weight: 100

            podAffinityTerm:
              labelSelector:
                matchExpressions:

                - key: app

                  operator: In
                  values:

                  - ecommerce-backend

              topologyKey: kubernetes.io/hostname
```

**Option 2: Adjust Topology Key**

```yaml
# Use zone instead of hostname for more flexibility
affinity:
  podAntiAffinity:
    requiredDuringSchedulingIgnoredDuringExecution:

    - labelSelector:

        matchExpressions:

        - key: app

          operator: In
          values:

          - ecommerce-backend

      topologyKey: topology.kubernetes.io/zone  # More flexible
```

**Option 3: Increase Replica Count**

```bash
# If anti-affinity requires more nodes than available
kubectl scale deployment ecommerce-backend --replicas=3 -n production
```

### Prevention and Monitoring

**Set up Scheduling Alerts**:

```yaml
# Prometheus alert rule

- alert: PodsPendingTooLong

  expr: kube_pod_status_phase{phase="Pending"} > 0
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "Pods pending for more than 5 minutes"
    description: "Pod {{ $labels.pod }} in namespace {{ $labels.namespace }} has been pending for more than 5 minutes"

- alert: FailedScheduling

  expr: increase(kube_pod_failed_scheduling_total[5m]) > 0
  labels:
    severity: critical
  annotations:
    summary: "Pod scheduling failures detected"
```

**Resource Quota Monitoring**:

```bash
# Check namespace resource quotas
kubectl get resourcequota -n production

# Check limit ranges
kubectl get limitrange -n production

# Monitor resource usage
kubectl top pods -n production --sort-by=memory
```

---

## Persistent Volume Claim Issues

### Overview

PersistentVolumeClaim (PVC) issues prevent pods from accessing persistent storage, causing application failures or data loss.

### Symptoms

- Pods stuck in `ContainerCreating` or `Pending` state
- Events showing "FailedMount" or "FailedAttachVolume"
- "Volume not found" errors
- "Multi-Attach error" for volumes
- Slow pod startup times

### Diagnostic Procedures

#### Step 1: Check PVC Status

```bash
# List PVCs
kubectl get pvc -n production

# Describe PVC
kubectl describe pvc ${PVC_NAME} -n production

# Check PVC events
kubectl get events -n production --field-selector involvedObject.name=${PVC_NAME}

# Check bound PV
kubectl get pv | grep ${PVC_NAME}
```

**PVC Status States**:

- `Pending`: Waiting for PV to be created or bound
- `Bound`: Successfully bound to a PV
- `Lost`: PV no longer exists

#### Step 2: Check Storage Class

```bash
# List storage classes
kubectl get storageclass

# Describe storage class
kubectl describe storageclass ${STORAGE_CLASS_NAME}

# Check default storage class
kubectl get storageclass -o jsonpath='{.items[?(@.metadata.annotations.storageclass\.kubernetes\.io/is-default-class=="true")].metadata.name}'

# Check provisioner
kubectl get storageclass ${STORAGE_CLASS_NAME} -o jsonpath='{.provisioner}'
```

#### Step 3: Check Volume Attachment

```bash
# Check volume attachments
kubectl get volumeattachment

# Describe volume attachment
kubectl describe volumeattachment ${ATTACHMENT_NAME}

# Check CSI driver
kubectl get csidrivers

# Check CSI nodes
kubectl get csinodes
```

#### Step 4: Check Pod Volume Mounts

```bash
# Check pod volume mounts
kubectl get pod ${POD_NAME} -n production -o jsonpath='{.spec.volumes}'

# Check volume mount status
kubectl describe pod ${POD_NAME} -n production | grep -A 10 "Volumes:"

# Check mount errors
kubectl get events -n production --field-selector involvedObject.name=${POD_NAME},reason=FailedMount
```

### Common PVC Issues and Solutions

#### Issue 1: PVC Stuck in Pending

**Problem**: PVC cannot find or create a suitable PV

**Diagnosis**:

```bash
# Check PVC status
kubectl describe pvc ${PVC_NAME} -n production

# Check available PVs
kubectl get pv

# Check storage class
kubectl get storageclass
```

**Common Causes and Solutions**:

**Cause 1: No Storage Class**

```yaml
# Add storage class to PVC
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: data-pvc
spec:
  accessModes:

    - ReadWriteOnce

  storageClassName: gp3  # Add this
  resources:
    requests:
      storage: 10Gi
```

**Cause 2: No Matching PV**

```bash
# Create PV manually (if not using dynamic provisioning)
kubectl apply -f - <<EOF
apiVersion: v1
kind: PersistentVolume
metadata:
  name: pv-manual
spec:
  capacity:
    storage: 10Gi
  accessModes:

    - ReadWriteOnce

  persistentVolumeReclaimPolicy: Retain
  storageClassName: manual
  hostPath:
    path: /mnt/data
EOF
```

**Cause 3: Insufficient Storage**

```bash
# Check available storage
kubectl get pv -o custom-columns=NAME:.metadata.name,CAPACITY:.spec.capacity.storage,STATUS:.status.phase

# Increase PVC size (if storage class supports expansion)
kubectl patch pvc ${PVC_NAME} -n production -p '{"spec":{"resources":{"requests":{"storage":"20Gi"}}}}'
```

#### Issue 2: Multi-Attach Error

**Problem**: Volume cannot be attached to multiple nodes (ReadWriteOnce)

**Diagnosis**:

```bash
# Check volume attachment
kubectl get volumeattachment | grep ${PV_NAME}

# Check which node has the volume
kubectl get volumeattachment -o jsonpath='{.items[?(@.spec.source.persistentVolumeName=="'${PV_NAME}'")].spec.nodeName}'

# Check pod node placement
kubectl get pod ${POD_NAME} -n production -o jsonpath='{.spec.nodeName}'
```

**Solutions**:

**Option 1: Use ReadWriteMany (if supported)**

```yaml
# Change access mode
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: shared-data
spec:
  accessModes:

    - ReadWriteMany  # Changed from ReadWriteOnce

  storageClassName: efs  # Use EFS for RWX
  resources:
    requests:
      storage: 10Gi
```

**Option 2: Force Detach and Reattach**

```bash
# Delete the pod to force detach
kubectl delete pod ${POD_NAME} -n production --grace-period=0 --force

# Wait for volume to detach
kubectl get volumeattachment --watch

# Pod will be recreated by deployment
```

**Option 3: Use Pod Affinity to Same Node**

```yaml
# Ensure pods using same volume run on same node
spec:
  template:
    spec:
      affinity:
        podAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:

          - labelSelector:

              matchExpressions:

              - key: app

                operator: In
                values:

                - ecommerce-backend

            topologyKey: kubernetes.io/hostname
```

#### Issue 3: Volume Mount Failures

**Problem**: Volume cannot be mounted to pod

**Diagnosis**:

```bash
# Check mount errors
kubectl describe pod ${POD_NAME} -n production | grep -A 20 "Events:"

# Check volume plugin
kubectl get pod ${POD_NAME} -n production -o jsonpath='{.spec.volumes[*].persistentVolumeClaim}'

# Check CSI driver logs
kubectl logs -n kube-system -l app=ebs-csi-controller
```

**Solutions**:

**Option 1: Fix Permissions**

```yaml
# Add security context
spec:
  template:
    spec:
      securityContext:
        fsGroup: 1000
        runAsUser: 1000
      containers:

      - name: app

        volumeMounts:

        - name: data

          mountPath: /data
```

**Option 2: Recreate PVC**

```bash
# Backup data if needed
kubectl exec ${POD_NAME} -n production -- tar czf /tmp/backup.tar.gz /data

# Delete and recreate PVC
kubectl delete pvc ${PVC_NAME} -n production
kubectl apply -f pvc.yaml

# Restore data
kubectl exec ${POD_NAME} -n production -- tar xzf /tmp/backup.tar.gz -C /
```

**Option 3: Check Node Kubelet**

```bash
# Check kubelet logs on node
ssh ${NODE_IP}
journalctl -u kubelet -f | grep -i volume

# Restart kubelet if needed
systemctl restart kubelet
```

### Volume Expansion

**Enable Volume Expansion**:

```yaml
# Storage class with expansion enabled
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: gp3-expandable
provisioner: ebs.csi.aws.com
allowVolumeExpansion: true  # Enable expansion
parameters:
  type: gp3
  iops: "3000"
  throughput: "125"
```

**Expand PVC**:

```bash
# Edit PVC to increase size
kubectl patch pvc ${PVC_NAME} -n production -p '{"spec":{"resources":{"requests":{"storage":"20Gi"}}}}'

# Check expansion status
kubectl describe pvc ${PVC_NAME} -n production | grep -A 5 "Conditions:"

# For file system expansion, restart pod
kubectl rollout restart deployment/${DEPLOYMENT_NAME} -n production
```

---

## ConfigMap and Secret Mounting Problems

### Overview

ConfigMaps and Secrets provide configuration and sensitive data to pods. Mounting issues can cause application failures or security vulnerabilities.

### Symptoms

- Pods stuck in `ContainerCreating` state
- "ConfigMap not found" or "Secret not found" errors
- Application configuration not loading
- Environment variables missing
- File mounts empty or incorrect

### Diagnostic Procedures

#### Step 1: Check ConfigMap/Secret Existence

```bash
# List ConfigMaps
kubectl get configmap -n production

# Describe ConfigMap
kubectl describe configmap ${CONFIGMAP_NAME} -n production

# View ConfigMap data
kubectl get configmap ${CONFIGMAP_NAME} -n production -o yaml

# List Secrets
kubectl get secret -n production

# Describe Secret (data is base64 encoded)
kubectl describe secret ${SECRET_NAME} -n production

# Decode Secret data
kubectl get secret ${SECRET_NAME} -n production -o jsonpath='{.data.password}' | base64 -d
```

#### Step 2: Check Pod Configuration

```bash
# Check ConfigMap references in pod
kubectl get pod ${POD_NAME} -n production -o yaml | grep -A 10 configMapRef

# Check Secret references
kubectl get pod ${POD_NAME} -n production -o yaml | grep -A 10 secretRef

# Check volume mounts
kubectl get pod ${POD_NAME} -n production -o yaml | grep -A 20 volumeMounts

# Check environment variables
kubectl exec ${POD_NAME} -n production -- env | grep -i config
```

#### Step 3: Check Mount Status

```bash
# Check if files are mounted
kubectl exec ${POD_NAME} -n production -- ls -la /etc/config

# Check file contents
kubectl exec ${POD_NAME} -n production -- cat /etc/config/application.yml

# Check environment variables
kubectl exec ${POD_NAME} -n production -- printenv | sort
```

### Common ConfigMap/Secret Issues and Solutions

#### Issue 1: ConfigMap/Secret Not Found

**Problem**: Referenced ConfigMap or Secret doesn't exist

**Diagnosis**:

```bash
# Check if ConfigMap exists
kubectl get configmap ${CONFIGMAP_NAME} -n production

# Check pod events
kubectl describe pod ${POD_NAME} -n production | grep -i "configmap\|secret"
```

**Solutions**:

**Option 1: Create Missing ConfigMap**

```bash
# Create from literal values
kubectl create configmap app-config -n production \
  --from-literal=database.host=postgres.production.svc.cluster.local \
  --from-literal=database.port=5432

# Create from file
kubectl create configmap app-config -n production \
  --from-file=application.yml

# Create from directory
kubectl create configmap app-config -n production \
  --from-file=config/
```

**Option 2: Create Missing Secret**

```bash
# Create from literal values
kubectl create secret generic db-credentials -n production \
  --from-literal=username=admin \
  --from-literal=password=secretpassword

# Create from file
kubectl create secret generic tls-cert -n production \
  --from-file=tls.crt=cert.pem \
  --from-file=tls.key=key.pem

# Create docker registry secret
kubectl create secret docker-registry ecr-secret -n production \
  --docker-server=${ECR_REGISTRY} \
  --docker-username=AWS \
  --docker-password=$(aws ecr get-login-password)
```

**Option 3: Fix Reference in Deployment**

```yaml
# Correct the ConfigMap name
spec:
  template:
    spec:
      containers:

      - name: app

        envFrom:

        - configMapRef:

            name: app-config  # Ensure this matches actual ConfigMap name
```

#### Issue 2: ConfigMap/Secret Not Updating

**Problem**: Changes to ConfigMap/Secret not reflected in running pods

**Diagnosis**:

```bash
# Check ConfigMap version
kubectl get configmap ${CONFIGMAP_NAME} -n production -o yaml | grep resourceVersion

# Check when pod was started
kubectl get pod ${POD_NAME} -n production -o jsonpath='{.status.startTime}'

# Check if using subPath (prevents updates)
kubectl get pod ${POD_NAME} -n production -o yaml | grep subPath
```

**Solutions**:

**Option 1: Restart Pods**

```bash
# Rolling restart
kubectl rollout restart deployment/${DEPLOYMENT_NAME} -n production

# Force delete pods
kubectl delete pod -l app=ecommerce-backend -n production

# Scale down and up
kubectl scale deployment/${DEPLOYMENT_NAME} --replicas=0 -n production
kubectl scale deployment/${DEPLOYMENT_NAME} --replicas=3 -n production
```

**Option 2: Use Reloader (Automatic Restart)**

```bash
# Install Reloader
kubectl apply -f https://raw.githubusercontent.com/stakater/Reloader/master/deployments/kubernetes/reloader.yaml

# Add annotation to deployment
kubectl annotate deployment ${DEPLOYMENT_NAME} -n production \
  reloader.stakater.com/auto="true"
```

**Option 3: Avoid subPath**

```yaml
# Instead of subPath, mount entire ConfigMap
volumeMounts:

- name: config

  mountPath: /etc/config
  # Don't use subPath - it prevents updates
```

**Option 4: Use Immutable ConfigMaps**

```yaml
# Create new ConfigMap with version
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config-v2  # Versioned name
immutable: true
data:
  application.yml: |
    ...

# Update deployment to use new version
spec:
  template:
    spec:
      volumes:

      - name: config

        configMap:
          name: app-config-v2  # Updated reference
```

#### Issue 3: Permission Denied on Mounted Files

**Problem**: Application cannot read mounted ConfigMap/Secret files

**Diagnosis**:

```bash
# Check file permissions
kubectl exec ${POD_NAME} -n production -- ls -la /etc/config

# Check pod security context
kubectl get pod ${POD_NAME} -n production -o jsonpath='{.spec.securityContext}'

# Check container user
kubectl exec ${POD_NAME} -n production -- id
```

**Solutions**:

**Option 1: Set Default Mode**

```yaml
# Set file permissions
volumes:

- name: config

  configMap:
    name: app-config
    defaultMode: 0644  # rw-r--r--

- name: secret

  secret:
    secretName: db-credentials
    defaultMode: 0400  # r--------
```

**Option 2: Set Security Context**

```yaml
# Run as specific user
spec:
  template:
    spec:
      securityContext:
        runAsUser: 1000
        runAsGroup: 1000
        fsGroup: 1000
      containers:

      - name: app

```

**Option 3: Use Init Container to Fix Permissions**

```yaml
# Copy and fix permissions
initContainers:

- name: fix-permissions

  image: busybox
  command: ['sh', '-c', 'cp /tmp/config/* /etc/config/ && chmod 644 /etc/config/*']
  volumeMounts:

  - name: config-source

    mountPath: /tmp/config

  - name: config-writable

    mountPath: /etc/config
```

### Best Practices

**ConfigMap Management**:

```yaml
# Use labels for versioning
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
  labels:
    app: ecommerce-backend
    version: "1.2.0"
    environment: production
data:
  application.yml: |
    ...
```

**Secret Management**:

```bash
# Use external secret management
# Install External Secrets Operator
kubectl apply -f https://raw.githubusercontent.com/external-secrets/external-secrets/main/deploy/crds/bundle.yaml

# Create SecretStore
kubectl apply -f - <<EOF
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: aws-secrets-manager
  namespace: production
spec:
  provider:
    aws:
      service: SecretsManager
      region: us-east-1
EOF

# Create ExternalSecret
kubectl apply -f - <<EOF
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: db-credentials
  namespace: production
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: aws-secrets-manager
    kind: SecretStore
  target:
    name: db-credentials
  data:

  - secretKey: password

    remoteRef:
      key: prod/database/password
EOF
```

---

## Service Discovery Failures

### Overview

Service discovery issues prevent pods from communicating with each other through Kubernetes Services, causing application failures and network connectivity problems.

### Symptoms

- "Connection refused" errors between services
- DNS resolution failures
- "Service not found" errors
- Intermittent connectivity issues
- Load balancing not working

### Diagnostic Procedures

#### Step 1: Check Service Configuration

```bash
# List services
kubectl get svc -n production

# Describe service
kubectl describe svc ${SERVICE_NAME} -n production

# Check service endpoints
kubectl get endpoints ${SERVICE_NAME} -n production

# Check service selector
kubectl get svc ${SERVICE_NAME} -n production -o jsonpath='{.spec.selector}'
```

#### Step 2: Verify Pod Labels Match Service Selector

```bash
# Check pod labels
kubectl get pods -n production --show-labels

# Check if pods match service selector
kubectl get pods -n production -l app=ecommerce-backend

# Compare with service selector
kubectl get svc ecommerce-backend -n production -o jsonpath='{.spec.selector}'
```

#### Step 3: Test DNS Resolution

```bash
# Test DNS from within cluster
kubectl run -it --rm debug --image=busybox --restart=Never -- nslookup ${SERVICE_NAME}.production.svc.cluster.local

# Test service connectivity
kubectl run -it --rm debug --image=curlimages/curl --restart=Never -- curl http://${SERVICE_NAME}.production.svc.cluster.local:8080/actuator/health

# Check CoreDNS
kubectl get pods -n kube-system -l k8s-app=kube-dns
kubectl logs -n kube-system -l k8s-app=kube-dns
```

### Common Service Discovery Issues

#### Issue 1: No Endpoints

**Problem**: Service has no endpoints (no pods match selector)

**Solution**:

```bash
# Fix pod labels to match service selector
kubectl label pods -l app=backend app=ecommerce-backend -n production --overwrite

# Or update service selector
kubectl patch svc ${SERVICE_NAME} -n production -p '{"spec":{"selector":{"app":"backend"}}}'
```

#### Issue 2: DNS Resolution Failures

**Problem**: Cannot resolve service names

**Solution**:

```bash
# Restart CoreDNS
kubectl rollout restart deployment/coredns -n kube-system

# Check CoreDNS ConfigMap
kubectl get configmap coredns -n kube-system -o yaml

# Test DNS
kubectl exec ${POD_NAME} -n production -- nslookup kubernetes.default
```

---

## Horizontal Pod Autoscaler Troubleshooting

### Overview

HPA automatically scales pods based on metrics. Issues can cause under or over-provisioning.

### Symptoms

- Pods not scaling despite high load
- Excessive scaling (flapping)
- "unable to get metrics" errors
- HPA showing "unknown" status

### Diagnostic Procedures

```bash
# Check HPA status
kubectl get hpa -n production

# Describe HPA
kubectl describe hpa ${HPA_NAME} -n production

# Check metrics server
kubectl get deployment metrics-server -n kube-system
kubectl logs -n kube-system -l k8s-app=metrics-server

# Check current metrics
kubectl top pods -n production
kubectl top nodes

# Check HPA events
kubectl get events -n production --field-selector involvedObject.name=${HPA_NAME}
```

### Common HPA Issues

#### Issue 1: Metrics Server Not Available

**Solution**:

```bash
# Install metrics server
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

# For development (insecure)
kubectl patch deployment metrics-server -n kube-system --type='json' -p='[{"op": "add", "path": "/spec/template/spec/containers/0/args/-", "value": "--kubelet-insecure-tls"}]'
```

#### Issue 2: Resource Requests Not Set

**Solution**:

```yaml
# Add resource requests (required for HPA)
spec:
  template:
    spec:
      containers:

      - name: app

        resources:
          requests:
            cpu: 500m
            memory: 512Mi
          limits:
            cpu: 1000m
            memory: 1Gi
```

#### Issue 3: HPA Flapping

**Solution**:

```yaml
# Adjust HPA behavior
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ecommerce-backend
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ecommerce-backend
  minReplicas: 3
  maxReplicas: 10
  metrics:

  - type: Resource

    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300  # Wait 5 min before scaling down
      policies:

      - type: Percent

        value: 50
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:

      - type: Percent

        value: 100
        periodSeconds: 15
```

---

## Cluster Autoscaler Issues

### Overview

Cluster Autoscaler automatically adjusts the number of nodes. Issues can cause resource shortages or waste.

### Symptoms

- Pods pending despite autoscaler enabled
- Nodes not scaling up
- Empty nodes not scaling down
- "scale up failed" errors

### Diagnostic Procedures

```bash
# Check cluster autoscaler status
kubectl get deployment cluster-autoscaler -n kube-system

# Check logs
kubectl logs -n kube-system -l app=cluster-autoscaler --tail=100

# Check autoscaler ConfigMap
kubectl get configmap cluster-autoscaler-status -n kube-system -o yaml

# Check node groups
kubectl get nodes -o custom-columns=NAME:.metadata.name,INSTANCE-TYPE:.metadata.labels.node\\.kubernetes\\.io/instance-type,ZONE:.metadata.labels.topology\\.kubernetes\\.io/zone
```

### Common Cluster Autoscaler Issues

#### Issue 1: Autoscaler Not Scaling Up

**Diagnosis**:

```bash
# Check autoscaler logs for scale-up decisions
kubectl logs -n kube-system -l app=cluster-autoscaler | grep -i "scale up"

# Check node group limits
aws autoscaling describe-auto-scaling-groups --auto-scaling-group-names ${ASG_NAME}
```

**Solutions**:

```bash
# Increase max size
aws autoscaling update-auto-scaling-group \
  --auto-scaling-group-name ${ASG_NAME} \
  --max-size 20

# Check autoscaler configuration
kubectl edit deployment cluster-autoscaler -n kube-system
# Ensure --max-nodes-total is sufficient
```

#### Issue 2: Nodes Not Scaling Down

**Diagnosis**:

```bash
# Check why nodes aren't scaling down
kubectl logs -n kube-system -l app=cluster-autoscaler | grep -i "scale down"

# Check node annotations
kubectl describe node ${NODE_NAME} | grep -i "scale-down"
```

**Solutions**:

```bash
# Remove scale-down prevention annotation
kubectl annotate node ${NODE_NAME} cluster-autoscaler.kubernetes.io/scale-down-disabled-

# Check for pods preventing scale-down
kubectl get pods --all-namespaces -o wide --field-selector spec.nodeName=${NODE_NAME}

# Add PodDisruptionBudget to allow eviction
kubectl apply -f - <<EOF
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: ecommerce-backend-pdb
  namespace: production
spec:
  minAvailable: 2
  selector:
    matchLabels:
      app: ecommerce-backend
EOF
```

---

## Node NotReady Troubleshooting

### Overview

Nodes in NotReady state cannot run pods, reducing cluster capacity and potentially causing outages.

### Symptoms

- Nodes showing NotReady status
- Pods evicted from nodes
- "node not found" errors
- Kubelet not responding

### Diagnostic Procedures

```bash
# Check node status
kubectl get nodes

# Describe node
kubectl describe node ${NODE_NAME}

# Check node conditions
kubectl get node ${NODE_NAME} -o jsonpath='{.status.conditions[*].type}{"\n"}{.status.conditions[*].status}'

# Check kubelet status (SSH to node)
ssh ${NODE_IP}
systemctl status kubelet
journalctl -u kubelet -f

# Check node resources
kubectl top node ${NODE_NAME}
```

### Common Node Issues

#### Issue 1: Disk Pressure

**Diagnosis**:

```bash
# Check disk usage on node
ssh ${NODE_IP}
df -h
du -sh /var/lib/docker/* | sort -rh | head -10
du -sh /var/lib/kubelet/* | sort -rh | head -10
```

**Solutions**:

```bash
# Clean up Docker images
docker system prune -a -f

# Clean up unused volumes
docker volume prune -f

# Clean up kubelet
kubectl delete pod --field-selector=status.phase==Succeeded -A
kubectl delete pod --field-selector=status.phase==Failed -A

# Increase disk size (AWS EBS)
aws ec2 modify-volume --volume-id ${VOLUME_ID} --size 100
```

#### Issue 2: Memory Pressure

**Diagnosis**:

```bash
# Check memory usage
ssh ${NODE_IP}
free -h
top -o %MEM

# Check for OOM kills
dmesg | grep -i "out of memory"
```

**Solutions**:

```bash
# Add more nodes
kubectl scale deployment cluster-autoscaler --replicas=1 -n kube-system

# Reduce pod resource requests
kubectl set resources deployment/${DEPLOYMENT_NAME} -n production --requests=memory=256Mi

# Evict pods from node
kubectl drain ${NODE_NAME} --ignore-daemonsets --delete-emptydir-data
```

#### Issue 3: Kubelet Not Running

**Diagnosis**:

```bash
# Check kubelet status
ssh ${NODE_IP}
systemctl status kubelet
journalctl -u kubelet --no-pager | tail -100
```

**Solutions**:

```bash
# Restart kubelet
systemctl restart kubelet

# Check kubelet configuration
cat /var/lib/kubelet/config.yaml

# Check certificates
ls -la /var/lib/kubelet/pki/

# Regenerate certificates if expired
kubeadm alpha certs renew all
systemctl restart kubelet
```

---

## etcd Performance and Health Issues

### Overview

etcd is the key-value store for Kubernetes cluster state. Performance issues can affect the entire cluster.

### Symptoms

- Slow API server responses
- "etcdserver: request timed out" errors
- High etcd latency
- Cluster state inconsistencies
- Leader election failures

### Diagnostic Procedures

```bash
# Check etcd pods
kubectl get pods -n kube-system -l component=etcd

# Check etcd logs
kubectl logs -n kube-system etcd-${MASTER_NODE} --tail=100

# Check etcd health
kubectl exec -n kube-system etcd-${MASTER_NODE} -- etcdctl \
  --endpoints=https://127.0.0.1:2379 \
  --cacert=/etc/kubernetes/pki/etcd/ca.crt \
  --cert=/etc/kubernetes/pki/etcd/server.crt \
  --key=/etc/kubernetes/pki/etcd/server.key \
  endpoint health

# Check etcd metrics
kubectl exec -n kube-system etcd-${MASTER_NODE} -- etcdctl \
  --endpoints=https://127.0.0.1:2379 \
  --cacert=/etc/kubernetes/pki/etcd/ca.crt \
  --cert=/etc/kubernetes/pki/etcd/server.crt \
  --key=/etc/kubernetes/pki/etcd/server.key \
  endpoint status --write-out=table
```

### Common etcd Issues

#### Issue 1: High Latency

**Diagnosis**:

```bash
# Check etcd latency
kubectl exec -n kube-system etcd-${MASTER_NODE} -- etcdctl \
  --endpoints=https://127.0.0.1:2379 \
  --cacert=/etc/kubernetes/pki/etcd/ca.crt \
  --cert=/etc/kubernetes/pki/etcd/server.crt \
  --key=/etc/kubernetes/pki/etcd/server.key \
  check perf

# Check disk I/O
ssh ${MASTER_IP}
iostat -x 1 10
```

**Solutions**:

```bash
# Use faster disks (SSD/NVMe)
# For AWS, use io2 or gp3 volumes

# Defragment etcd
kubectl exec -n kube-system etcd-${MASTER_NODE} -- etcdctl \
  --endpoints=https://127.0.0.1:2379 \
  --cacert=/etc/kubernetes/pki/etcd/ca.crt \
  --cert=/etc/kubernetes/pki/etcd/server.crt \
  --key=/etc/kubernetes/pki/etcd/server.key \
  defrag

# Compact etcd history
kubectl exec -n kube-system etcd-${MASTER_NODE} -- etcdctl \
  --endpoints=https://127.0.0.1:2379 \
  --cacert=/etc/kubernetes/pki/etcd/ca.crt \
  --cert=/etc/kubernetes/pki/etcd/server.crt \
  --key=/etc/kubernetes/pki/etcd/server.key \
  compact $(kubectl exec -n kube-system etcd-${MASTER_NODE} -- etcdctl \
    --endpoints=https://127.0.0.1:2379 \
    --cacert=/etc/kubernetes/pki/etcd/ca.crt \
    --cert=/etc/kubernetes/pki/etcd/server.crt \
    --key=/etc/kubernetes/pki/etcd/server.key \
    endpoint status --write-out="json" | jq -r '.[0].Status.header.revision')
```

#### Issue 2: Database Size Too Large

**Diagnosis**:

```bash
# Check etcd database size
kubectl exec -n kube-system etcd-${MASTER_NODE} -- etcdctl \
  --endpoints=https://127.0.0.1:2379 \
  --cacert=/etc/kubernetes/pki/etcd/ca.crt \
  --cert=/etc/kubernetes/pki/etcd/server.crt \
  --key=/etc/kubernetes/pki/etcd/server.key \
  endpoint status --write-out=table

# Check for large keys
kubectl exec -n kube-system etcd-${MASTER_NODE} -- etcdctl \
  --endpoints=https://127.0.0.1:2379 \
  --cacert=/etc/kubernetes/pki/etcd/ca.crt \
  --cert=/etc/kubernetes/pki/etcd/server.crt \
  --key=/etc/kubernetes/pki/etcd/server.key \
  get --prefix --keys-only / | head -100
```

**Solutions**:

```bash
# Enable automatic compaction
kubectl edit pod etcd-${MASTER_NODE} -n kube-system
# Add: --auto-compaction-retention=1

# Clean up old events
kubectl delete events --all -A

# Clean up completed pods
kubectl delete pod --field-selector=status.phase==Succeeded -A
kubectl delete pod --field-selector=status.phase==Failed -A

# Backup and restore to reduce size
ETCDCTL_API=3 etcdctl snapshot save /tmp/etcd-backup.db
ETCDCTL_API=3 etcdctl snapshot restore /tmp/etcd-backup.db --data-dir=/var/lib/etcd-new
```

#### Issue 3: Split Brain / Quorum Loss

**Diagnosis**:

```bash
# Check member list
kubectl exec -n kube-system etcd-${MASTER_NODE} -- etcdctl \
  --endpoints=https://127.0.0.1:2379 \
  --cacert=/etc/kubernetes/pki/etcd/ca.crt \
  --cert=/etc/kubernetes/pki/etcd/server.crt \
  --key=/etc/kubernetes/pki/etcd/server.key \
  member list

# Check cluster health
kubectl exec -n kube-system etcd-${MASTER_NODE} -- etcdctl \
  --endpoints=https://127.0.0.1:2379 \
  --cacert=/etc/kubernetes/pki/etcd/ca.crt \
  --cert=/etc/kubernetes/pki/etcd/server.crt \
  --key=/etc/kubernetes/pki/etcd/server.key \
  endpoint health --cluster
```

**Solutions**:

```bash
# Remove unhealthy member
kubectl exec -n kube-system etcd-${MASTER_NODE} -- etcdctl \
  --endpoints=https://127.0.0.1:2379 \
  --cacert=/etc/kubernetes/pki/etcd/ca.crt \
  --cert=/etc/kubernetes/pki/etcd/server.crt \
  --key=/etc/kubernetes/pki/etcd/server.key \
  member remove ${MEMBER_ID}

# Add new member
kubectl exec -n kube-system etcd-${MASTER_NODE} -- etcdctl \
  --endpoints=https://127.0.0.1:2379 \
  --cacert=/etc/kubernetes/pki/etcd/ca.crt \
  --cert=/etc/kubernetes/pki/etcd/server.crt \
  --key=/etc/kubernetes/pki/etcd/server.key \
  member add ${NEW_MEMBER_NAME} --peer-urls=https://${NEW_MEMBER_IP}:2380
```

### etcd Backup and Restore

**Backup**:

```bash
# Create snapshot
kubectl exec -n kube-system etcd-${MASTER_NODE} -- etcdctl \
  --endpoints=https://127.0.0.1:2379 \
  --cacert=/etc/kubernetes/pki/etcd/ca.crt \
  --cert=/etc/kubernetes/pki/etcd/server.crt \
  --key=/etc/kubernetes/pki/etcd/server.key \
  snapshot save /tmp/etcd-backup-$(date +%Y%m%d-%H%M%S).db

# Copy backup
kubectl cp kube-system/etcd-${MASTER_NODE}:/tmp/etcd-backup-*.db ./etcd-backup.db
```

**Restore**:

```bash
# Stop API server
systemctl stop kube-apiserver

# Restore snapshot
ETCDCTL_API=3 etcdctl snapshot restore etcd-backup.db \
  --data-dir=/var/lib/etcd-restored \
  --name=${MEMBER_NAME} \
  --initial-cluster=${INITIAL_CLUSTER} \
  --initial-advertise-peer-urls=https://${MEMBER_IP}:2380

# Update etcd data directory
mv /var/lib/etcd /var/lib/etcd-old
mv /var/lib/etcd-restored /var/lib/etcd

# Start API server
systemctl start kube-apiserver
```

---

## Monitoring and Alerts

### Kubernetes-Specific Alerts

```yaml
# Prometheus alert rules
groups:

- name: kubernetes-alerts

  rules:

  - alert: PodsPendingTooLong

    expr: kube_pod_status_phase{phase="Pending"} > 0
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "Pods pending for more than 5 minutes"

  - alert: NodeNotReady

    expr: kube_node_status_condition{condition="Ready",status="true"} == 0
    for: 5m
    labels:
      severity: critical
    annotations:
      summary: "Node {{ $labels.node }} is not ready"

  - alert: PVCPending

    expr: kube_persistentvolumeclaim_status_phase{phase="Pending"} > 0
    for: 10m
    labels:
      severity: warning
    annotations:
      summary: "PVC {{ $labels.persistentvolumeclaim }} pending for 10+ minutes"

  - alert: HPAMaxedOut

    expr: kube_horizontalpodautoscaler_status_current_replicas >= kube_horizontalpodautoscaler_spec_max_replicas
    for: 15m
    labels:
      severity: warning
    annotations:
      summary: "HPA {{ $labels.horizontalpodautoscaler }} at max replicas"

  - alert: etcdHighLatency

    expr: histogram_quantile(0.99, rate(etcd_disk_wal_fsync_duration_seconds_bucket[5m])) > 0.5
    for: 10m
    labels:
      severity: critical
    annotations:
      summary: "etcd high latency detected"
```

---

## Related Documentation

- [Common Issues](common-issues.md) - Quick solutions for common problems
- [Application Debugging Guide](application-debugging.md) - Application-level debugging
- [Database Troubleshooting](database-issues.md) - Database-specific issues
- [Network and Connectivity](network-connectivity.md) - Network troubleshooting
- [Deployment Process](../deployment/deployment-process.md) - Deployment procedures
- [Monitoring Strategy](../monitoring/monitoring-strategy.md) - Monitoring setup

---

**Last Updated**: 2025-10-25  
**Owner**: DevOps Team  
**Review Cycle**: Monthly
