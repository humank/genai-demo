# Application Debugging Guide

## Overview

This guide provides comprehensive debugging workflows, diagnostic procedures, and analysis techniques for troubleshooting application issues in the Enterprise E-Commerce Platform.

**Purpose**: Detailed step-by-step debugging procedures for complex application issues  
**Audience**: DevOps engineers, SREs, backend developers  
**Prerequisites**: Access to production environment, kubectl, JDK tools

---

## Table of Contents

1. [Debugging Workflow Overview](#debugging-workflow-overview)
2. [Memory Issues](#memory-issues)
3. [Thread Issues](#thread-issues)
4. [Performance Issues](#performance-issues)
5. [JVM Tuning](#jvm-tuning)
6. [Profiling Techniques](#profiling-techniques)
7. [Diagnostic Tools Reference](#diagnostic-tools-reference)

---

## Debugging Workflow Overview

### General Debugging Decision Tree

```
Issue Detected
    ↓
Is it a memory issue? → YES → [Memory Issues Section](#memory-issues)
    ↓ NO
Is it a thread issue? → YES → [Thread Issues Section](#thread-issues)
    ↓ NO
Is it a performance issue? → YES → [Performance Issues Section](#performance-issues)
    ↓ NO
Check application logs and metrics → [Common Issues](common-issues.md)
```

### Initial Diagnostic Steps

**Step 1: Gather Basic Information**
```bash
# Get pod status
kubectl get pods -n production -l app=ecommerce-backend

# Check recent events
kubectl get events -n production --sort-by='.lastTimestamp' | tail -20

# View pod resource usage
kubectl top pod -n production -l app=ecommerce-backend
```

**Step 2: Check Application Health**
```bash
# Health endpoint
POD_NAME=$(kubectl get pod -n production -l app=ecommerce-backend -o jsonpath='{.items[0].metadata.name}')
kubectl exec -n production ${POD_NAME} -- curl -s http://localhost:8080/actuator/health | jq

# Metrics endpoint
kubectl exec -n production ${POD_NAME} -- curl -s http://localhost:8080/actuator/metrics | jq
```

**Step 3: Review Application Logs**
```bash
# Recent logs
kubectl logs -n production ${POD_NAME} --tail=100

# Follow logs in real-time
kubectl logs -n production ${POD_NAME} -f

# Previous container logs (if pod restarted)
kubectl logs -n production ${POD_NAME} --previous
```

---

## Memory Issues

### Memory Issue Decision Tree

```
High Memory Usage Detected
    ↓
Is memory increasing continuously? → YES → Memory Leak Investigation
    ↓ NO
Is memory spiking periodically? → YES → GC Pressure Investigation
    ↓ NO
Is memory usage stable but high? → YES → Heap Size Optimization
```

### Memory Leak Detection and Resolution

#### Step 1: Confirm Memory Leak

**Monitor Memory Trend**
```bash
# Watch memory usage over time
watch -n 5 'kubectl top pod -n production ${POD_NAME}'

# Check heap usage via JMX
kubectl exec -n production ${POD_NAME} -- jcmd 1 VM.native_memory summary
```

**Check for OOMKilled Events**
```bash
kubectl get events -n production --field-selector reason=OOMKilled
```

#### Step 2: Generate Heap Dump

**Using jmap (Recommended)**
```bash
# Get Java process ID
kubectl exec -n production ${POD_NAME} -- jps -l

# Generate heap dump
kubectl exec -n production ${POD_NAME} -- jmap -dump:live,format=b,file=/tmp/heap.hprof 1

# Copy heap dump locally
kubectl cp production/${POD_NAME}:/tmp/heap.hprof ./heap-$(date +%Y%m%d-%H%M%S).hprof
```

**Automatic Heap Dump on OOM**
```bash
# Add JVM flags to deployment
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/tmp/heapdump.hprof
```

**Using jcmd (Alternative)**
```bash
kubectl exec -n production ${POD_NAME} -- jcmd 1 GC.heap_dump /tmp/heap.hprof
```

#### Step 3: Analyze Heap Dump

**Using jhat (Built-in)**
```bash
# Start jhat server
jhat -J-Xmx4g heap.hprof

# Access web interface
open http://localhost:7000
```

**Key Analysis Points in jhat**:
1. **Histogram**: Shows object counts and sizes
   - Look for unexpected large object counts
   - Identify objects consuming most memory
2. **Object Query Language (OQL)**: Query heap objects
3. **Reference Chains**: Find what's holding references

**Using Eclipse MAT (Recommended)**
```bash
# Download Eclipse Memory Analyzer
# https://www.eclipse.org/mat/

# Open heap dump in MAT
# File → Open Heap Dump → Select heap.hprof
```

**MAT Analysis Steps**:
1. **Leak Suspects Report**: Automatic leak detection
2. **Dominator Tree**: Shows retained heap by object
3. **Histogram**: Object instance counts
4. **Thread Overview**: Memory per thread
5. **OQL Console**: Advanced queries

**Common Memory Leak Patterns**:
```java
// Pattern 1: Static collections growing unbounded
public class CacheManager {
    private static Map<String, Object> cache = new HashMap<>(); // LEAK!
    // Solution: Use bounded cache with eviction policy
}

// Pattern 2: Unclosed resources
public void processData() {
    Connection conn = dataSource.getConnection(); // LEAK if not closed!
    // Solution: Use try-with-resources
}

// Pattern 3: ThreadLocal not cleaned
public class RequestContext {
    private static ThreadLocal<User> currentUser = new ThreadLocal<>(); // LEAK!
    // Solution: Call remove() in finally block
}

// Pattern 4: Event listeners not removed
button.addActionListener(listener); // LEAK if not removed!
// Solution: Remove listeners when done
```

#### Step 4: Identify Root Cause

**Using VisualVM**
```bash
# Install VisualVM
# https://visualvm.github.io/

# Connect to remote JVM
# Add JMX flags to deployment:
-Dcom.sun.management.jmxremote
-Dcom.sun.management.jmxremote.port=9010
-Dcom.sun.management.jmxremote.authenticate=false
-Dcom.sun.management.jmxremote.ssl=false

# Port forward JMX port
kubectl port-forward -n production ${POD_NAME} 9010:9010

# Connect VisualVM to localhost:9010
```

**VisualVM Analysis**:
1. **Monitor Tab**: Real-time heap, threads, classes
2. **Sampler**: CPU and memory sampling
3. **Profiler**: Detailed profiling (use cautiously in production)
4. **Heap Dump**: Load and analyze heap dumps

**OQL Queries for Common Leaks**:
```javascript
// Find all instances of a class
select s from java.lang.String s

// Find large collections
select c from java.util.HashMap c where c.size > 1000

// Find objects with many references
select referrers(s) from java.lang.String s where s.value.length > 100

// Find ThreadLocal leaks
select t from java.lang.ThreadLocal t
```

#### Step 5: Fix and Verify

**Common Fixes**:
1. **Add resource cleanup**
   ```java
   try (Connection conn = dataSource.getConnection()) {
       // Use connection
   } // Automatically closed
   ```

2. **Implement cache eviction**
   ```java
   @Bean
   public CacheManager cacheManager() {
       return CacheBuilder.newBuilder()
           .maximumSize(10000)
           .expireAfterWrite(10, TimeUnit.MINUTES)
           .build();
   }
   ```

3. **Clean ThreadLocal**
   ```java
   try {
       threadLocal.set(value);
       // Use value
   } finally {
       threadLocal.remove(); // Critical!
   }
   ```

**Verification**:
```bash
# Monitor memory after fix
kubectl top pod -n production ${POD_NAME} --watch

# Check for OOM events
kubectl get events -n production --field-selector reason=OOMKilled --watch
```

### Garbage Collection Analysis

#### Step 1: Enable GC Logging

**Add GC Logging Flags**
```bash
# For Java 11+
-Xlog:gc*:file=/tmp/gc.log:time,uptime,level,tags
-Xlog:gc+heap=debug

# For Java 8
-XX:+PrintGCDetails
-XX:+PrintGCDateStamps
-XX:+PrintGCTimeStamps
-Xloggc:/tmp/gc.log
```

#### Step 2: Collect GC Logs

```bash
# Copy GC logs from pod
kubectl cp production/${POD_NAME}:/tmp/gc.log ./gc-$(date +%Y%m%d-%H%M%S).log

# Or view in real-time
kubectl exec -n production ${POD_NAME} -- tail -f /tmp/gc.log
```

#### Step 3: Analyze GC Logs

**Using GCViewer**
```bash
# Download GCViewer
# https://github.com/chewiebug/GCViewer

# Open GC log
java -jar gcviewer.jar gc.log
```

**Key Metrics to Check**:
- **GC Pause Time**: Should be < 100ms for young GC, < 1s for full GC
- **GC Frequency**: Full GC should be rare (< 1 per hour)
- **Heap Usage After GC**: Should drop significantly
- **Promotion Rate**: Objects moving to old generation

**GC Log Analysis Patterns**:
```
# Good pattern - Young GC
[GC (Allocation Failure) 512M->64M(1024M), 0.0234567 secs]
# Heap dropped from 512M to 64M in 23ms - GOOD

# Bad pattern - Full GC frequent
[Full GC (Ergonomics) 900M->850M(1024M), 2.3456789 secs]
# Heap only dropped 50M after 2.3s Full GC - BAD (memory leak?)

# Bad pattern - Long pause
[GC (Allocation Failure) 512M->64M(1024M), 1.2345678 secs]
# 1.2s pause for young GC - BAD (heap too large or GC tuning needed)
```

#### Step 4: Tune GC

**G1GC Tuning (Recommended for Java 11+)**
```bash
# Basic G1GC configuration
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:G1HeapRegionSize=16m
-XX:InitiatingHeapOccupancyPercent=45
-XX:G1ReservePercent=10
-XX:G1NewSizePercent=30
-XX:G1MaxNewSizePercent=40
```

**ZGC Tuning (For low-latency requirements)**
```bash
# ZGC configuration (Java 15+)
-XX:+UseZGC
-XX:ZCollectionInterval=5
-XX:ZAllocationSpikeTolerance=2
```

**Common GC Issues and Solutions**:

| Issue | Symptom | Solution |
|-------|---------|----------|
| Frequent Full GC | Full GC every few minutes | Increase heap size or fix memory leak |
| Long GC pauses | Pauses > 1s | Reduce heap size or tune GC |
| High promotion rate | Old gen filling quickly | Increase young gen size |
| Allocation failures | Frequent young GC | Increase young gen or heap size |

---

## Thread Issues

### Thread Issue Decision Tree

```
Thread Issue Detected
    ↓
Are threads blocked? → YES → Deadlock Investigation
    ↓ NO
Are threads waiting? → YES → Contention Investigation
    ↓ NO
Too many threads? → YES → Thread Leak Investigation
    ↓ NO
High CPU usage? → YES → CPU Profiling
```

### Thread Dump Analysis

#### Step 1: Generate Thread Dump

**Using jstack**
```bash
# Generate thread dump
kubectl exec -n production ${POD_NAME} -- jstack 1 > thread-dump-$(date +%Y%m%d-%H%M%S).txt

# Generate multiple dumps (for comparison)
for i in {1..3}; do
  kubectl exec -n production ${POD_NAME} -- jstack 1 > thread-dump-$i.txt
  sleep 10
done
```

**Using jcmd**
```bash
kubectl exec -n production ${POD_NAME} -- jcmd 1 Thread.print > thread-dump.txt
```

**Using kill signal (if tools unavailable)**
```bash
kubectl exec -n production ${POD_NAME} -- kill -3 1
# Thread dump will appear in application logs
kubectl logs -n production ${POD_NAME} | tail -1000 > thread-dump.txt
```

#### Step 2: Analyze Thread States

**Thread State Reference**:
- **RUNNABLE**: Thread is executing or ready to execute
- **BLOCKED**: Thread is blocked waiting for a monitor lock
- **WAITING**: Thread is waiting indefinitely for another thread
- **TIMED_WAITING**: Thread is waiting for a specified time
- **NEW**: Thread has not yet started
- **TERMINATED**: Thread has completed execution

**Thread Dump Format**:
```
"http-nio-8080-exec-1" #23 daemon prio=5 os_prio=0 tid=0x00007f8c4c001000 nid=0x1234 waiting on condition [0x00007f8c3ffff000]
   java.lang.Thread.State: TIMED_WAITING (parking)
        at sun.misc.Unsafe.park(Native Method)
        - parking to wait for  <0x00000000e0a12345> (a java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject)
        at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:215)
        at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.awaitNanos(AbstractQueuedSynchronizer.java:2078)
```

#### Step 3: Identify Issues

**Deadlock Detection**
```bash
# jstack automatically detects deadlocks
grep -A 20 "Found one Java-level deadlock" thread-dump.txt
```

**Deadlock Example**:
```
Found one Java-level deadlock:
=============================
"Thread-1":
  waiting to lock monitor 0x00007f8c4c002000 (object 0x00000000e0a12345, a java.lang.Object),
  which is held by "Thread-2"
"Thread-2":
  waiting to lock monitor 0x00007f8c4c003000 (object 0x00000000e0a67890, a java.lang.Object),
  which is held by "Thread-1"
```

**Blocked Threads Analysis**
```bash
# Count threads by state
grep "java.lang.Thread.State:" thread-dump.txt | sort | uniq -c

# Find blocked threads
grep -B 5 "State: BLOCKED" thread-dump.txt

# Find what they're waiting for
grep -A 10 "State: BLOCKED" thread-dump.txt | grep "waiting to lock"
```

**Thread Contention Hotspots**
```bash
# Find most contended locks
grep "waiting to lock" thread-dump.txt | sort | uniq -c | sort -rn | head -10

# Find threads holding locks
grep "locked <" thread-dump.txt | sort | uniq -c | sort -rn | head -10
```

#### Step 4: Resolve Thread Issues

**Deadlock Resolution**:
1. **Identify lock ordering**: Ensure consistent lock acquisition order
2. **Use timeout**: Use `tryLock(timeout)` instead of `lock()`
3. **Reduce lock scope**: Hold locks for minimal time
4. **Use higher-level concurrency utilities**: `ConcurrentHashMap`, `AtomicInteger`

**Example Fix**:
```java
// BAD: Inconsistent lock ordering
synchronized(lockA) {
    synchronized(lockB) { ... }
}
// Thread 2 does opposite
synchronized(lockB) {
    synchronized(lockA) { ... }
}

// GOOD: Consistent lock ordering
synchronized(lockA) {
    synchronized(lockB) { ... }
}
// Thread 2 uses same order
synchronized(lockA) {
    synchronized(lockB) { ... }
}
```

**Thread Pool Exhaustion**:
```bash
# Check thread pool metrics
kubectl exec -n production ${POD_NAME} -- curl -s http://localhost:8080/actuator/metrics/executor.active | jq
kubectl exec -n production ${POD_NAME} -- curl -s http://localhost:8080/actuator/metrics/executor.pool.size | jq
```

**Thread Pool Tuning**:
```yaml
# application.yml
server:
  tomcat:
    threads:
      max: 200        # Maximum threads
      min-spare: 10   # Minimum idle threads
    accept-count: 100 # Queue size
    max-connections: 10000
```

### Thread Leak Investigation

**Symptoms**:
- Thread count continuously increasing
- Eventually hitting OS thread limit
- Application becomes unresponsive

**Detection**:
```bash
# Monitor thread count
kubectl exec -n production ${POD_NAME} -- jcmd 1 Thread.print | grep "^\"" | wc -l

# Watch thread count over time
watch -n 5 'kubectl exec -n production ${POD_NAME} -- jcmd 1 Thread.print | grep "^\"" | wc -l'
```

**Common Causes**:
1. **Executor not shutdown**: Thread pools not properly closed
2. **Timer threads**: Timers created but not cancelled
3. **Thread creation in loops**: Creating threads without pooling

**Example Fixes**:
```java
// BAD: Thread leak
public void processData() {
    ExecutorService executor = Executors.newFixedThreadPool(10);
    executor.submit(() -> doWork());
    // LEAK: executor never shutdown
}

// GOOD: Proper cleanup
public void processData() {
    ExecutorService executor = Executors.newFixedThreadPool(10);
    try {
        executor.submit(() -> doWork());
    } finally {
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
    }
}
```

---

## Performance Issues

### Performance Debugging Decision Tree

```
Slow Performance Detected
    ↓
Is CPU high? → YES → CPU Profiling
    ↓ NO
Is I/O high? → YES → I/O Analysis
    ↓ NO
Is database slow? → YES → Database Profiling
    ↓ NO
Is network slow? → YES → Network Analysis
```

### CPU Profiling

#### Step 1: Identify High CPU Usage

```bash
# Check pod CPU usage
kubectl top pod -n production ${POD_NAME}

# Check per-thread CPU usage
kubectl exec -n production ${POD_NAME} -- top -H -b -n 1 | head -20

# Get Java thread CPU usage
kubectl exec -n production ${POD_NAME} -- jcmd 1 Thread.print | grep "cpu="
```

#### Step 2: Generate CPU Profile

**Using async-profiler (Recommended)**
```bash
# Download async-profiler
wget https://github.com/jvm-profiling-tools/async-profiler/releases/download/v2.9/async-profiler-2.9-linux-x64.tar.gz
tar -xzf async-profiler-2.9-linux-x64.tar.gz

# Copy to pod
kubectl cp async-profiler-2.9-linux-x64 production/${POD_NAME}:/tmp/async-profiler

# Start profiling (60 seconds)
kubectl exec -n production ${POD_NAME} -- /tmp/async-profiler/profiler.sh -d 60 -f /tmp/cpu-profile.html 1

# Copy profile back
kubectl cp production/${POD_NAME}:/tmp/cpu-profile.html ./cpu-profile-$(date +%Y%m%d-%H%M%S).html
```

**Using JProfiler**
```bash
# Add JProfiler agent to JVM
-agentpath:/path/to/libjprofilerti.so=port=8849,nowait

# Port forward
kubectl port-forward -n production ${POD_NAME} 8849:8849

# Connect JProfiler GUI to localhost:8849
```

**Using YourKit**
```bash
# Add YourKit agent
-agentpath:/path/to/libyjpagent.so=port=10001

# Port forward
kubectl port-forward -n production ${POD_NAME} 10001:10001

# Connect YourKit GUI
```

#### Step 3: Analyze CPU Profile

**Flame Graph Analysis**:
- **Width**: Represents time spent in function
- **Height**: Call stack depth
- **Color**: Different colors for different packages

**Key Areas to Check**:
1. **Hot methods**: Methods consuming most CPU
2. **Unexpected loops**: Tight loops or recursion
3. **Regex compilation**: Repeated pattern compilation
4. **String operations**: Excessive string concatenation
5. **Reflection**: Heavy reflection usage

**Common CPU Hotspots**:
```java
// Pattern 1: Regex in loop
for (String s : list) {
    if (s.matches("\\d+")) { ... } // Compiles regex each time!
}
// Fix: Compile pattern once
Pattern pattern = Pattern.compile("\\d+");
for (String s : list) {
    if (pattern.matcher(s).matches()) { ... }
}

// Pattern 2: String concatenation in loop
String result = "";
for (String s : list) {
    result += s; // Creates new string each time!
}
// Fix: Use StringBuilder
StringBuilder sb = new StringBuilder();
for (String s : list) {
    sb.append(s);
}

// Pattern 3: Unnecessary object creation
for (int i = 0; i < 1000000; i++) {
    Integer obj = new Integer(i); // Boxing overhead!
}
// Fix: Use primitives
for (int i = 0; i < 1000000; i++) {
    int value = i;
}
```

### Database Performance Analysis

#### Step 1: Enable Query Logging

```yaml
# application.yml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

#### Step 2: Identify Slow Queries

```bash
# Check slow query log
kubectl exec -n production ${POD_NAME} -- grep "execution time" /var/log/app.log | sort -t: -k4 -rn | head -20

# Check database slow query log
psql -c "SELECT query, calls, mean_time, max_time FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 10;"
```

#### Step 3: Analyze Query Performance

**N+1 Query Detection**:
```bash
# Look for repeated similar queries
kubectl logs -n production ${POD_NAME} | grep "SELECT" | sort | uniq -c | sort -rn | head -20
```

**N+1 Query Example**:
```sql
-- Initial query
SELECT * FROM orders WHERE customer_id = 123;

-- Then N queries (one per order)
SELECT * FROM order_items WHERE order_id = 1;
SELECT * FROM order_items WHERE order_id = 2;
SELECT * FROM order_items WHERE order_id = 3;
...
```

**Fix with JOIN FETCH**:
```java
// BAD: N+1 query
@Query("SELECT o FROM Order o WHERE o.customerId = :customerId")
List<Order> findByCustomerId(@Param("customerId") String customerId);

// GOOD: Single query with JOIN FETCH
@Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.customerId = :customerId")
List<Order> findByCustomerIdWithItems(@Param("customerId") String customerId);
```

**Missing Index Detection**:
```sql
-- Check query execution plan
EXPLAIN ANALYZE SELECT * FROM orders WHERE customer_id = 123;

-- Look for "Seq Scan" (bad) vs "Index Scan" (good)
-- If Seq Scan, add index:
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
```

---

## JVM Tuning

### Heap Size Tuning

**Initial Sizing Guidelines**:
```bash
# For applications with < 2GB memory requirement
-Xms512m -Xmx1g

# For applications with 2-4GB memory requirement
-Xms1g -Xmx2g

# For applications with 4-8GB memory requirement
-Xms2g -Xmx4g

# For applications with > 8GB memory requirement
-Xms4g -Xmx8g
```

**Heap Size Decision Matrix**:

| Symptom | Current Heap | Action |
|---------|--------------|--------|
| Frequent Full GC | Any | Increase heap or fix memory leak |
| Long GC pauses | > 4GB | Decrease heap or use ZGC |
| OOMError | Any | Increase heap or fix memory leak |
| Low heap usage | Any | Decrease heap to save resources |

**Heap Ratio Tuning**:
```bash
# Young generation sizing
-XX:NewRatio=2              # Old:Young = 2:1
-XX:SurvivorRatio=8         # Eden:Survivor = 8:1

# Or explicit sizing
-XX:NewSize=512m
-XX:MaxNewSize=1g
```

### GC Algorithm Selection

**Algorithm Comparison**:

| GC | Latency | Throughput | Heap Size | Use Case |
|----|---------|------------|-----------|----------|
| Serial GC | High | Low | < 100MB | Single CPU, small heap |
| Parallel GC | Medium | High | Any | Batch processing |
| G1GC | Low | Medium | > 4GB | General purpose (default) |
| ZGC | Very Low | Medium | > 8GB | Low-latency requirements |
| Shenandoah | Very Low | Medium | > 8GB | Low-latency requirements |

**G1GC Tuning (Recommended)**:
```bash
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200           # Target pause time
-XX:G1HeapRegionSize=16m           # Region size (1-32MB)
-XX:InitiatingHeapOccupancyPercent=45  # When to start concurrent marking
-XX:G1ReservePercent=10            # Reserve heap for evacuation
-XX:G1NewSizePercent=30            # Min young gen size
-XX:G1MaxNewSizePercent=40         # Max young gen size
-XX:ParallelGCThreads=8            # Parallel GC threads
-XX:ConcGCThreads=2                # Concurrent GC threads
```

**ZGC Tuning (Low Latency)**:
```bash
-XX:+UseZGC
-XX:+ZGenerational                 # Enable generational ZGC (Java 21+)
-XX:ZCollectionInterval=5          # Collection interval in seconds
-XX:ZAllocationSpikeTolerance=2    # Allocation spike tolerance
-XX:SoftMaxHeapSize=6g            # Soft heap limit
```

### JVM Flags for Production

**Essential Production Flags**:
```bash
# Heap settings
-Xms2g -Xmx2g                      # Set min=max for predictable behavior

# GC settings
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200

# GC logging
-Xlog:gc*:file=/tmp/gc.log:time,uptime,level,tags
-Xlog:gc+heap=debug

# Heap dump on OOM
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/tmp/heapdump.hprof

# JMX for monitoring
-Dcom.sun.management.jmxremote
-Dcom.sun.management.jmxremote.port=9010
-Dcom.sun.management.jmxremote.authenticate=false
-Dcom.sun.management.jmxremote.ssl=false

# Performance tuning
-XX:+UseStringDeduplication       # Reduce string memory usage
-XX:+AlwaysPreTouch              # Touch memory pages at startup
-XX:+DisableExplicitGC           # Ignore System.gc() calls
```

**Complete Production Configuration**:
```yaml
# deployment.yaml
env:
  - name: JAVA_OPTS
    value: >-
      -Xms2g -Xmx2g
      -XX:+UseG1GC
      -XX:MaxGCPauseMillis=200
      -XX:G1HeapRegionSize=16m
      -XX:InitiatingHeapOccupancyPercent=45
      -Xlog:gc*:file=/tmp/gc.log:time,uptime,level,tags
      -XX:+HeapDumpOnOutOfMemoryError
      -XX:HeapDumpPath=/tmp/heapdump.hprof
      -Dcom.sun.management.jmxremote
      -Dcom.sun.management.jmxremote.port=9010
      -Dcom.sun.management.jmxremote.authenticate=false
      -Dcom.sun.management.jmxremote.ssl=false
      -XX:+UseStringDeduplication
      -XX:+AlwaysPreTouch
      -XX:+DisableExplicitGC
```

### Container-Specific Tuning

**Container Memory Awareness**:
```bash
# Java 11+ automatically detects container limits
# No flags needed

# Java 8 (requires flags)
-XX:+UnlockExperimentalVMOptions
-XX:+UseCGroupMemoryLimitForHeap
-XX:MaxRAMFraction=2              # Use 50% of container memory
```

**CPU Awareness**:
```bash
# Java 11+ automatically detects container CPU limits
# No flags needed

# Java 8 (requires flags)
-XX:ActiveProcessorCount=2        # Set explicitly
```

---

## Profiling Techniques

### Sampling vs Instrumentation

**Sampling Profiling**:
- **How**: Periodically samples thread stacks
- **Overhead**: Low (1-5%)
- **Accuracy**: Statistical approximation
- **Use**: Production environments
- **Tools**: async-profiler, VisualVM sampler

**Instrumentation Profiling**:
- **How**: Instruments bytecode to track every method call
- **Overhead**: High (10-50%)
- **Accuracy**: Exact
- **Use**: Development/staging only
- **Tools**: JProfiler, YourKit profiler

### async-profiler (Recommended for Production)

**Installation**:
```bash
# Download
wget https://github.com/jvm-profiling-tools/async-profiler/releases/download/v2.9/async-profiler-2.9-linux-x64.tar.gz
tar -xzf async-profiler-2.9-linux-x64.tar.gz

# Copy to pod
kubectl cp async-profiler-2.9-linux-x64 production/${POD_NAME}:/tmp/async-profiler
```

**CPU Profiling**:
```bash
# Profile for 60 seconds, generate flame graph
kubectl exec -n production ${POD_NAME} -- \
  /tmp/async-profiler/profiler.sh -d 60 -f /tmp/cpu-flamegraph.html 1

# Profile specific event (cpu, alloc, lock)
kubectl exec -n production ${POD_NAME} -- \
  /tmp/async-profiler/profiler.sh -e cpu -d 60 -f /tmp/cpu-profile.html 1
```

**Allocation Profiling**:
```bash
# Profile memory allocations
kubectl exec -n production ${POD_NAME} -- \
  /tmp/async-profiler/profiler.sh -e alloc -d 60 -f /tmp/alloc-flamegraph.html 1
```

**Lock Profiling**:
```bash
# Profile lock contention
kubectl exec -n production ${POD_NAME} -- \
  /tmp/async-profiler/profiler.sh -e lock -d 60 -f /tmp/lock-flamegraph.html 1
```

### JProfiler (Development/Staging)

**Setup**:
```bash
# Add JProfiler agent to JVM
-agentpath:/path/to/libjprofilerti.so=port=8849,nowait,config=/path/to/config.xml

# Port forward
kubectl port-forward -n staging ${POD_NAME} 8849:8849
```

**Profiling Sessions**:
1. **CPU Profiling**: Identify hot methods
2. **Memory Profiling**: Track object allocations
3. **Thread Profiling**: Analyze thread states and contention
4. **Database Profiling**: Monitor JDBC calls

**Key Features**:
- **Call Tree**: Hierarchical view of method calls
- **Hot Spots**: Methods consuming most resources
- **Call Graph**: Visual representation of call relationships
- **Telemetries**: Real-time monitoring

### YourKit (Development/Staging)

**Setup**:
```bash
# Add YourKit agent
-agentpath:/path/to/libyjpagent.so=port=10001,listen=all

# Port forward
kubectl port-forward -n staging ${POD_NAME} 10001:10001
```

**Profiling Modes**:
1. **Sampling**: Low overhead, statistical
2. **Tracing**: High overhead, exact
3. **Adaptive**: Automatic mode switching

**Analysis Views**:
- **CPU**: Method execution time
- **Memory**: Object allocation and retention
- **Threads**: Thread states and synchronization
- **Exceptions**: Exception tracking

### VisualVM (Free Alternative)

**Setup**:
```bash
# Add JMX flags
-Dcom.sun.management.jmxremote
-Dcom.sun.management.jmxremote.port=9010
-Dcom.sun.management.jmxremote.authenticate=false
-Dcom.sun.management.jmxremote.ssl=false

# Port forward
kubectl port-forward -n production ${POD_NAME} 9010:9010

# Connect VisualVM to localhost:9010
```

**Features**:
- **Monitor**: Real-time CPU, memory, threads, classes
- **Sampler**: CPU and memory sampling (low overhead)
- **Profiler**: CPU and memory profiling (high overhead)
- **Heap Dump**: Capture and analyze heap dumps
- **Thread Dump**: Capture and analyze thread dumps

---

## Diagnostic Tools Reference

### JDK Tools

| Tool | Purpose | Usage |
|------|---------|-------|
| jps | List Java processes | `jps -l` |
| jstack | Thread dump | `jstack <pid>` |
| jmap | Heap dump | `jmap -dump:live,format=b,file=heap.hprof <pid>` |
| jstat | GC statistics | `jstat -gc <pid> 1000` |
| jcmd | Multi-purpose | `jcmd <pid> <command>` |
| jhat | Heap analysis | `jhat heap.hprof` |
| jinfo | JVM configuration | `jinfo <pid>` |

### jcmd Commands

```bash
# List available commands
jcmd <pid> help

# Thread dump
jcmd <pid> Thread.print

# Heap dump
jcmd <pid> GC.heap_dump /tmp/heap.hprof

# GC statistics
jcmd <pid> GC.class_histogram

# JVM flags
jcmd <pid> VM.flags

# System properties
jcmd <pid> VM.system_properties

# Native memory tracking
jcmd <pid> VM.native_memory summary

# Force GC
jcmd <pid> GC.run
```

### jstat Examples

```bash
# GC statistics every second
jstat -gc <pid> 1000

# GC summary
jstat -gcutil <pid>

# Heap capacity
jstat -gccapacity <pid>

# New generation statistics
jstat -gcnew <pid>

# Old generation statistics
jstat -gcold <pid>
```

### Native Memory Tracking

**Enable NMT**:
```bash
-XX:NativeMemoryTracking=summary  # or =detail for more info
```

**Check Native Memory**:
```bash
# Summary
jcmd <pid> VM.native_memory summary

# Detailed breakdown
jcmd <pid> VM.native_memory detail

# Diff from baseline
jcmd <pid> VM.native_memory baseline
# ... wait some time ...
jcmd <pid> VM.native_memory summary.diff
```

### Kubernetes Debugging Tools

```bash
# Pod logs
kubectl logs -n production ${POD_NAME} --tail=100 -f

# Previous container logs
kubectl logs -n production ${POD_NAME} --previous

# Execute command in pod
kubectl exec -n production ${POD_NAME} -- <command>

# Interactive shell
kubectl exec -it -n production ${POD_NAME} -- /bin/bash

# Port forwarding
kubectl port-forward -n production ${POD_NAME} 8080:8080

# Copy files
kubectl cp production/${POD_NAME}:/tmp/file.txt ./file.txt

# Pod events
kubectl get events -n production --field-selector involvedObject.name=${POD_NAME}

# Pod resource usage
kubectl top pod -n production ${POD_NAME}

# Pod description
kubectl describe pod -n production ${POD_NAME}
```

---

## Troubleshooting Workflows

### Complete Memory Leak Investigation Workflow

```bash
#!/bin/bash
# memory-leak-investigation.sh

POD_NAME=$1
NAMESPACE=${2:-production}

echo "=== Memory Leak Investigation ==="
echo "Pod: ${POD_NAME}"
echo "Namespace: ${NAMESPACE}"
echo ""

# Step 1: Confirm memory leak
echo "Step 1: Monitoring memory usage..."
kubectl top pod -n ${NAMESPACE} ${POD_NAME}
echo "Watching for 5 minutes (Ctrl+C to stop)..."
watch -n 30 "kubectl top pod -n ${NAMESPACE} ${POD_NAME}"

# Step 2: Generate heap dump
echo "Step 2: Generating heap dump..."
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
kubectl exec -n ${NAMESPACE} ${POD_NAME} -- jmap -dump:live,format=b,file=/tmp/heap.hprof 1
kubectl cp ${NAMESPACE}/${POD_NAME}:/tmp/heap.hprof ./heap-${TIMESTAMP}.hprof
echo "Heap dump saved to: heap-${TIMESTAMP}.hprof"

# Step 3: Generate thread dump
echo "Step 3: Generating thread dump..."
kubectl exec -n ${NAMESPACE} ${POD_NAME} -- jstack 1 > thread-dump-${TIMESTAMP}.txt
echo "Thread dump saved to: thread-dump-${TIMESTAMP}.txt"

# Step 4: Collect GC logs
echo "Step 4: Collecting GC logs..."
kubectl cp ${NAMESPACE}/${POD_NAME}:/tmp/gc.log ./gc-${TIMESTAMP}.log
echo "GC log saved to: gc-${TIMESTAMP}.log"

# Step 5: Check for common issues
echo "Step 5: Checking for common issues..."
echo "Thread count:"
kubectl exec -n ${NAMESPACE} ${POD_NAME} -- jcmd 1 Thread.print | grep "^\"" | wc -l

echo "Database connections:"
kubectl exec -n ${NAMESPACE} ${POD_NAME} -- curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.active | jq

echo "Cache size:"
kubectl exec -n ${NAMESPACE} ${POD_NAME} -- curl -s http://localhost:8080/actuator/metrics/cache.size | jq

echo ""
echo "=== Investigation Complete ==="
echo "Next steps:"
echo "1. Analyze heap dump with Eclipse MAT or jhat"
echo "2. Review thread dump for blocked threads"
echo "3. Analyze GC log with GCViewer"
echo "4. Check application logs for errors"
```

### Complete Performance Investigation Workflow

```bash
#!/bin/bash
# performance-investigation.sh

POD_NAME=$1
NAMESPACE=${2:-production}
DURATION=${3:-60}

echo "=== Performance Investigation ==="
echo "Pod: ${POD_NAME}"
echo "Namespace: ${NAMESPACE}"
echo "Duration: ${DURATION}s"
echo ""

# Step 1: Check current performance
echo "Step 1: Checking current performance..."
kubectl top pod -n ${NAMESPACE} ${POD_NAME}

# Step 2: Check response times
echo "Step 2: Checking response times..."
kubectl exec -n ${NAMESPACE} ${POD_NAME} -- \
  curl -s http://localhost:8080/actuator/metrics/http.server.requests | jq

# Step 3: Profile CPU
echo "Step 3: Profiling CPU for ${DURATION}s..."
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
kubectl exec -n ${NAMESPACE} ${POD_NAME} -- \
  /tmp/async-profiler/profiler.sh -d ${DURATION} -f /tmp/cpu-profile.html 1
kubectl cp ${NAMESPACE}/${POD_NAME}:/tmp/cpu-profile.html ./cpu-profile-${TIMESTAMP}.html
echo "CPU profile saved to: cpu-profile-${TIMESTAMP}.html"

# Step 4: Profile allocations
echo "Step 4: Profiling allocations for ${DURATION}s..."
kubectl exec -n ${NAMESPACE} ${POD_NAME} -- \
  /tmp/async-profiler/profiler.sh -e alloc -d ${DURATION} -f /tmp/alloc-profile.html 1
kubectl cp ${NAMESPACE}/${POD_NAME}:/tmp/alloc-profile.html ./alloc-profile-${TIMESTAMP}.html
echo "Allocation profile saved to: alloc-profile-${TIMESTAMP}.html"

# Step 5: Check database performance
echo "Step 5: Checking database performance..."
kubectl exec -n ${NAMESPACE} ${POD_NAME} -- \
  curl -s http://localhost:8080/actuator/metrics/spring.data.repository.invocations | jq

# Step 6: Generate thread dump
echo "Step 6: Generating thread dump..."
kubectl exec -n ${NAMESPACE} ${POD_NAME} -- jstack 1 > thread-dump-${TIMESTAMP}.txt
echo "Thread dump saved to: thread-dump-${TIMESTAMP}.txt"

echo ""
echo "=== Investigation Complete ==="
echo "Next steps:"
echo "1. Open cpu-profile-${TIMESTAMP}.html in browser"
echo "2. Open alloc-profile-${TIMESTAMP}.html in browser"
echo "3. Review thread-dump-${TIMESTAMP}.txt for blocked threads"
echo "4. Check database slow query log"
```

---

## Best Practices

### Production Debugging Guidelines

1. **Minimize Impact**:
   - Use sampling profilers (async-profiler) instead of instrumentation
   - Limit profiling duration to 60-120 seconds
   - Profile during low-traffic periods when possible
   - Use read-only operations when possible

2. **Collect Evidence**:
   - Always collect multiple data points (heap dumps, thread dumps, profiles)
   - Take snapshots before and after changes
   - Document all steps and findings
   - Save all diagnostic files with timestamps

3. **Safety First**:
   - Test diagnostic commands in staging first
   - Have rollback plan ready
   - Monitor application during diagnostics
   - Coordinate with team before production debugging

4. **Follow Up**:
   - Document root cause and fix
   - Update runbooks with findings
   - Share knowledge with team
   - Implement preventive measures

### Common Pitfalls to Avoid

1. **Don't**:
   - Run instrumentation profilers in production
   - Generate heap dumps during peak traffic
   - Make JVM changes without testing
   - Debug without proper monitoring
   - Ignore warning signs (gradual memory increase, etc.)

2. **Do**:
   - Use appropriate tools for the environment
   - Collect baseline metrics before issues occur
   - Test fixes in staging first
   - Document all changes
   - Monitor after fixes are deployed

---

## Related Documentation

- [Common Issues](common-issues.md) - Quick solutions for common problems
- [High Memory Usage Runbook](../runbooks/high-memory-usage.md) - Memory issue procedures
- [Slow API Responses Runbook](../runbooks/slow-api-responses.md) - Performance issue procedures
- [Monitoring Strategy](../monitoring/monitoring-strategy.md) - Monitoring and alerting setup

---

**Last Updated**: 2025-10-25  
**Owner**: DevOps Team  
**Review Cycle**: Quarterly  
**Contributors**: Backend Engineering Team, SRE Team

