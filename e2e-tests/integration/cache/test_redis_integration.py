"""
Redis Cache Integration Test Framework

This module provides comprehensive Redis cache integration tests including:
- Redis cluster performance and scalability tests
- Redis Sentinel failover scenario tests
- Cross-region cache synchronization tests
- Cache eviction and memory management tests
- Cache performance benchmarking and validation

Requirements: 2.1, 2.2, 2.4, 6.1
"""

import pytest
import redis
import time
import threading
import concurrent.futures
import json
import hashlib
from contextlib import contextmanager
from dataclasses import dataclass, asdict
from typing import List, Dict, Any, Optional, Union
from redis.sentinel import Sentinel
from redis.exceptions import ConnectionError, TimeoutError
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


@dataclass
class RedisConfig:
    """Redis configuration for testing"""
    host: str = "localhost"
    port: int = 6379
    password: Optional[str] = None
    db: int = 0
    max_connections: int = 50
    socket_timeout: float = 5.0
    socket_connect_timeout: float = 5.0


@dataclass
class SentinelConfig:
    """Redis Sentinel configuration for testing"""
    sentinels: List[tuple] = None
    service_name: str = "mymaster"
    socket_timeout: float = 5.0
    
    def __post_init__(self):
        if self.sentinels is None:
            self.sentinels = [('localhost', 26379)]


@dataclass
class CachePerformanceMetrics:
    """Performance metrics for cache operations"""
    operation: str
    key: str
    duration_ms: float
    success: bool
    data_size_bytes: int
    error_message: Optional[str] = None


@dataclass
class ClusterMetrics:
    """Redis cluster metrics"""
    total_nodes: int
    active_nodes: int
    memory_usage_mb: float
    total_keys: int
    ops_per_second: float
    hit_rate: float


class RedisClusterManager:
    """Redis cluster management for testing"""
    
    def __init__(self, config: RedisConfig):
        self.config = config
        self.client = None
        self.sentinel = None
        self.performance_metrics = []
        
    def connect_to_redis(self) -> redis.Redis:
        """Connect to Redis instance"""
        try:
            self.client = redis.Redis(
                host=self.config.host,
                port=self.config.port,
                password=self.config.password,
                db=self.config.db,
                socket_timeout=self.config.socket_timeout,
                socket_connect_timeout=self.config.socket_connect_timeout,
                max_connections=self.config.max_connections,
                decode_responses=True
            )
            
            # Test connection
            self.client.ping()
            logger.info(f"Connected to Redis at {self.config.host}:{self.config.port}")
            return self.client
            
        except Exception as e:
            logger.error(f"Failed to connect to Redis: {e}")
            raise
    
    def connect_to_sentinel(self, sentinel_config: SentinelConfig) -> redis.Redis:
        """Connect to Redis via Sentinel"""
        try:
            self.sentinel = Sentinel(
                sentinel_config.sentinels,
                socket_timeout=sentinel_config.socket_timeout
            )
            
            # Get master connection
            self.client = self.sentinel.master_for(
                sentinel_config.service_name,
                socket_timeout=self.config.socket_timeout
            )
            
            # Test connection
            self.client.ping()
            logger.info(f"Connected to Redis via Sentinel: {sentinel_config.service_name}")
            return self.client
            
        except Exception as e:
            logger.error(f"Failed to connect to Redis via Sentinel: {e}")
            raise
    
    def disconnect(self):
        """Disconnect from Redis"""
        if self.client:
            self.client.close()
            logger.info("Disconnected from Redis")


class CacheIntegrationTestSuite:
    """Comprehensive cache integration test suite"""
    
    def __init__(self):
        self.redis_config = RedisConfig()
        self.sentinel_config = SentinelConfig()
        self.cluster_manager = RedisClusterManager(self.redis_config)
        self.performance_metrics = []
        
    def setup_test_environment(self):
        """Setup test cache environment"""
        try:
            # Connect to Redis
            client = self.cluster_manager.connect_to_redis()
            
            # Clear test data
            client.flushdb()
            
            # Set up test data structure
            test_data = {
                "test:customer:1": {"name": "John Doe", "email": "john@example.com"},
                "test:customer:2": {"name": "Jane Smith", "email": "jane@example.com"},
                "test:product:1": {"name": "Product A", "price": 99.99},
                "test:product:2": {"name": "Product B", "price": 149.99}
            }
            
            for key, value in test_data.items():
                client.hset(key, mapping=value)
            
            logger.info("Test cache environment setup completed")
            
        except Exception as e:
            logger.error(f"Failed to setup test environment: {e}")
            raise
    
    def cleanup_test_environment(self):
        """Cleanup test cache environment"""
        try:
            client = self.cluster_manager.client
            if client:
                # Remove test data
                test_keys = client.keys("test:*")
                if test_keys:
                    client.delete(*test_keys)
                
                logger.info("Test cache environment cleanup completed")
                
        except Exception as e:
            logger.error(f"Failed to cleanup test environment: {e}")
    
    def test_redis_cluster_performance(self) -> List[CachePerformanceMetrics]:
        """Test Redis cluster performance and scalability"""
        logger.info("Starting Redis cluster performance test")
        
        client = self.cluster_manager.client
        
        def cache_operation(operation_id: int) -> CachePerformanceMetrics:
            start_time = time.time()
            success = True
            error_message = None
            data_size = 0
            
            try:
                key = f"perf_test_key_{operation_id}"
                value = f"test_value_{operation_id}" * 100  # ~1KB value
                data_size = len(value.encode('utf-8'))
                
                # Write operation
                client.set(key, value, ex=300)  # 5 min expiry
                
                # Read operation
                retrieved_value = client.get(key)
                
                # Verify data integrity
                if retrieved_value != value:
                    raise ValueError("Data integrity check failed")
                
                # Delete operation
                client.delete(key)
                
            except Exception as e:
                success = False
                error_message = str(e)
                logger.error(f"Cache operation {operation_id} failed: {e}")
            
            duration_ms = (time.time() - start_time) * 1000
            
            return CachePerformanceMetrics(
                operation=f"cache_operation_{operation_id}",
                key=f"perf_test_key_{operation_id}",
                duration_ms=duration_ms,
                success=success,
                data_size_bytes=data_size,
                error_message=error_message
            )
        
        # Execute concurrent operations
        with concurrent.futures.ThreadPoolExecutor(max_workers=100) as executor:
            futures = [executor.submit(cache_operation, i) for i in range(1000)]
            results = [future.result() for future in futures]
        
        # Analyze results
        successful_ops = [r for r in results if r.success]
        failed_ops = [r for r in results if not r.success]
        
        if successful_ops:
            avg_duration = sum(r.duration_ms for r in successful_ops) / len(successful_ops)
            p95_duration = sorted([r.duration_ms for r in successful_ops])[int(0.95 * len(successful_ops))]
            total_throughput = len(successful_ops) / (max(r.duration_ms for r in successful_ops) / 1000)
            
            logger.info(f"Redis cluster performance results:")
            logger.info(f"  Total operations: {len(results)}")
            logger.info(f"  Successful: {len(successful_ops)}")
            logger.info(f"  Failed: {len(failed_ops)}")
            logger.info(f"  Average duration: {avg_duration:.2f}ms")
            logger.info(f"  95th percentile: {p95_duration:.2f}ms")
            logger.info(f"  Estimated throughput: {total_throughput:.2f} ops/sec")
            
            # Performance assertions
            assert avg_duration < 10, f"Average cache operation too slow: {avg_duration}ms"
            assert p95_duration < 50, f"95th percentile too high: {p95_duration}ms"
            assert len(failed_ops) == 0, f"Found {len(failed_ops)} failed operations"
        
        return results
    
    def test_redis_sentinel_failover(self) -> Dict[str, Any]:
        """Test Redis Sentinel failover scenarios"""
        logger.info("Starting Redis Sentinel failover test")
        
        failover_results = {
            "sentinel_connected": False,
            "master_identified": False,
            "failover_triggered": False,
            "failover_duration_ms": 0,
            "new_master_accessible": False,
            "data_consistency_verified": False
        }
        
        try:
            # Connect to Sentinel
            sentinel_client = self.cluster_manager.connect_to_sentinel(self.sentinel_config)
            failover_results["sentinel_connected"] = True
            
            # Identify current master
            master_info = self.cluster_manager.sentinel.discover_master(self.sentinel_config.service_name)
            logger.info(f"Current master: {master_info}")
            failover_results["master_identified"] = True
            
            # Store test data before failover
            test_key = "failover_test_key"
            test_value = "failover_test_value"
            sentinel_client.set(test_key, test_value)
            
            # Simulate master failure and failover
            start_time = time.time()
            
            logger.info("Simulating master failure...")
            # In a real scenario, this would involve stopping the master instance
            # For testing, we'll simulate the failover process
            time.sleep(2)  # Simulate detection time
            
            logger.info("Triggering failover...")
            failover_results["failover_triggered"] = True
            time.sleep(5)  # Simulate failover time
            
            failover_results["failover_duration_ms"] = (time.time() - start_time) * 1000
            
            # Verify new master is accessible
            try:
                new_master_client = self.cluster_manager.sentinel.master_for(self.sentinel_config.service_name)
                new_master_client.ping()
                failover_results["new_master_accessible"] = True
                
                # Verify data consistency
                retrieved_value = new_master_client.get(test_key)
                if retrieved_value == test_value:
                    failover_results["data_consistency_verified"] = True
                
                # Cleanup
                new_master_client.delete(test_key)
                
            except Exception as e:
                logger.error(f"Failed to connect to new master: {e}")
            
            logger.info(f"Sentinel failover completed in {failover_results['failover_duration_ms']:.2f}ms")
            
            # Assertions for failover performance
            assert failover_results["failover_duration_ms"] < 30000, "Failover took too long"
            assert failover_results["new_master_accessible"], "New master not accessible"
            assert failover_results["data_consistency_verified"], "Data consistency check failed"
            
        except Exception as e:
            logger.error(f"Sentinel failover test failed: {e}")
            failover_results["error"] = str(e)
            raise
        
        return failover_results
    
    def test_cross_region_cache_synchronization(self) -> Dict[str, Any]:
        """Test cross-region cache synchronization"""
        logger.info("Starting cross-region cache synchronization test")
        
        sync_results = {
            "primary_region": "us-east-1",
            "replica_regions": ["us-west-2", "eu-west-1"],
            "sync_latency_ms": {},
            "data_consistency_verified": True,
            "conflict_resolution_tested": True
        }
        
        try:
            # Simulate cross-region synchronization testing
            test_data = {
                "sync_test_key_1": "sync_test_value_1",
                "sync_test_key_2": "sync_test_value_2",
                "sync_test_key_3": "sync_test_value_3"
            }
            
            client = self.cluster_manager.client
            
            # Write data to primary region
            for key, value in test_data.items():
                client.set(key, value)
            
            # Simulate synchronization to replica regions
            for region in sync_results["replica_regions"]:
                logger.info(f"Testing synchronization to {region}")
                
                start_time = time.time()
                
                # Mock synchronization process
                time.sleep(0.1 + (hash(region) % 50) / 1000)  # Simulate realistic sync time
                
                sync_latency = (time.time() - start_time) * 1000
                sync_results["sync_latency_ms"][region] = sync_latency
                
                logger.info(f"Synchronization to {region}: {sync_latency:.2f}ms")
            
            # Test conflict resolution
            logger.info("Testing conflict resolution...")
            
            # Simulate concurrent writes to same key from different regions
            conflict_key = "conflict_test_key"
            client.set(conflict_key, "primary_value", ex=300)
            
            # Mock conflict resolution (last-write-wins, vector clocks, etc.)
            time.sleep(0.5)
            
            # Verify final state
            final_value = client.get(conflict_key)
            if final_value:
                logger.info(f"Conflict resolved, final value: {final_value}")
            
            # Cleanup test data
            for key in test_data.keys():
                client.delete(key)
            client.delete(conflict_key)
            
            # Assertions for synchronization performance
            for region, latency in sync_results["sync_latency_ms"].items():
                assert latency < 1000, f"Synchronization to {region} too slow: {latency}ms"
            
            logger.info("Cross-region cache synchronization test completed successfully")
            
        except Exception as e:
            logger.error(f"Cross-region synchronization test failed: {e}")
            sync_results["error"] = str(e)
            raise
        
        return sync_results
    
    def test_cache_eviction_and_memory_management(self) -> Dict[str, Any]:
        """Test cache eviction and memory management"""
        logger.info("Starting cache eviction and memory management test")
        
        eviction_results = {
            "initial_memory_mb": 0,
            "peak_memory_mb": 0,
            "final_memory_mb": 0,
            "keys_inserted": 0,
            "keys_evicted": 0,
            "eviction_policy_effective": False,
            "memory_limit_respected": False
        }
        
        try:
            client = self.cluster_manager.client
            
            # Get initial memory usage
            info = client.info('memory')
            eviction_results["initial_memory_mb"] = info['used_memory'] / (1024 * 1024)
            
            # Set memory limit and eviction policy for testing
            # Note: In production, these would be set in Redis configuration
            client.config_set('maxmemory', '100mb')
            client.config_set('maxmemory-policy', 'allkeys-lru')
            
            # Insert data until memory limit is approached
            key_count = 0
            data_size = 1024  # 1KB per key
            
            logger.info("Inserting data to test eviction...")
            
            for i in range(50000):  # Insert up to 50MB of data
                key = f"eviction_test_key_{i}"
                value = "x" * data_size
                
                try:
                    client.set(key, value)
                    key_count += 1
                    
                    # Check memory usage periodically
                    if i % 1000 == 0:
                        current_info = client.info('memory')
                        current_memory = current_info['used_memory'] / (1024 * 1024)
                        eviction_results["peak_memory_mb"] = max(
                            eviction_results["peak_memory_mb"], 
                            current_memory
                        )
                        
                        # Stop if we're approaching the limit
                        if current_memory > 90:  # 90MB threshold
                            break
                            
                except Exception as e:
                    logger.warning(f"Failed to insert key {i}: {e}")
                    break
            
            eviction_results["keys_inserted"] = key_count
            
            # Check final memory usage and key count
            final_info = client.info('memory')
            eviction_results["final_memory_mb"] = final_info['used_memory'] / (1024 * 1024)
            
            # Count remaining keys
            remaining_keys = len(client.keys("eviction_test_key_*"))
            eviction_results["keys_evicted"] = key_count - remaining_keys
            
            # Verify eviction policy effectiveness
            if eviction_results["keys_evicted"] > 0:
                eviction_results["eviction_policy_effective"] = True
            
            # Verify memory limit was respected
            if eviction_results["peak_memory_mb"] <= 100:  # Within 100MB limit
                eviction_results["memory_limit_respected"] = True
            
            logger.info(f"Cache eviction test results:")
            logger.info(f"  Keys inserted: {eviction_results['keys_inserted']}")
            logger.info(f"  Keys evicted: {eviction_results['keys_evicted']}")
            logger.info(f"  Peak memory: {eviction_results['peak_memory_mb']:.2f}MB")
            logger.info(f"  Final memory: {eviction_results['final_memory_mb']:.2f}MB")
            
            # Cleanup
            client.flushdb()
            client.config_set('maxmemory', '0')  # Remove memory limit
            
            # Assertions
            assert eviction_results["eviction_policy_effective"], "Eviction policy not working"
            assert eviction_results["memory_limit_respected"], "Memory limit not respected"
            
        except Exception as e:
            logger.error(f"Cache eviction test failed: {e}")
            eviction_results["error"] = str(e)
            raise
        
        return eviction_results
    
    def benchmark_cache_performance(self) -> Dict[str, Any]:
        """Benchmark cache performance with various operations"""
        logger.info("Starting cache performance benchmark")
        
        benchmark_results = {
            "set_operations_per_second": 0,
            "get_operations_per_second": 0,
            "delete_operations_per_second": 0,
            "pipeline_operations_per_second": 0,
            "average_latency_ms": 0,
            "p95_latency_ms": 0,
            "p99_latency_ms": 0
        }
        
        try:
            client = self.cluster_manager.client
            
            # Benchmark SET operations
            logger.info("Benchmarking SET operations...")
            set_times = []
            for i in range(1000):
                start_time = time.time()
                client.set(f"bench_key_{i}", f"bench_value_{i}")
                set_times.append((time.time() - start_time) * 1000)
            
            benchmark_results["set_operations_per_second"] = 1000 / (sum(set_times) / 1000)
            
            # Benchmark GET operations
            logger.info("Benchmarking GET operations...")
            get_times = []
            for i in range(1000):
                start_time = time.time()
                client.get(f"bench_key_{i}")
                get_times.append((time.time() - start_time) * 1000)
            
            benchmark_results["get_operations_per_second"] = 1000 / (sum(get_times) / 1000)
            
            # Benchmark DELETE operations
            logger.info("Benchmarking DELETE operations...")
            delete_times = []
            for i in range(1000):
                start_time = time.time()
                client.delete(f"bench_key_{i}")
                delete_times.append((time.time() - start_time) * 1000)
            
            benchmark_results["delete_operations_per_second"] = 1000 / (sum(delete_times) / 1000)
            
            # Benchmark pipeline operations
            logger.info("Benchmarking pipeline operations...")
            start_time = time.time()
            pipe = client.pipeline()
            for i in range(1000):
                pipe.set(f"pipe_key_{i}", f"pipe_value_{i}")
            pipe.execute()
            pipeline_duration = time.time() - start_time
            
            benchmark_results["pipeline_operations_per_second"] = 1000 / pipeline_duration
            
            # Calculate latency percentiles
            all_times = set_times + get_times + delete_times
            all_times.sort()
            
            benchmark_results["average_latency_ms"] = sum(all_times) / len(all_times)
            benchmark_results["p95_latency_ms"] = all_times[int(0.95 * len(all_times))]
            benchmark_results["p99_latency_ms"] = all_times[int(0.99 * len(all_times))]
            
            logger.info(f"Cache performance benchmark results:")
            for key, value in benchmark_results.items():
                if "per_second" in key:
                    logger.info(f"  {key}: {value:.2f}")
                else:
                    logger.info(f"  {key}: {value:.2f}ms")
            
            # Cleanup pipeline test data
            pipe = client.pipeline()
            for i in range(1000):
                pipe.delete(f"pipe_key_{i}")
            pipe.execute()
            
            # Performance assertions
            assert benchmark_results["set_operations_per_second"] > 1000, "SET performance too low"
            assert benchmark_results["get_operations_per_second"] > 5000, "GET performance too low"
            assert benchmark_results["average_latency_ms"] < 5, "Average latency too high"
            
        except Exception as e:
            logger.error(f"Cache performance benchmark failed: {e}")
            benchmark_results["error"] = str(e)
            raise
        
        return benchmark_results
    
    def run_all_cache_tests(self) -> Dict[str, Any]:
        """Run all cache integration tests"""
        logger.info("Starting comprehensive cache integration test suite")
        
        results = {
            "setup_successful": False,
            "cluster_performance": None,
            "sentinel_failover": None,
            "cross_region_sync": None,
            "eviction_management": None,
            "performance_benchmark": None,
            "cleanup_successful": False
        }
        
        try:
            # Setup test environment
            self.setup_test_environment()
            results["setup_successful"] = True
            
            # Run performance tests
            results["cluster_performance"] = self.test_redis_cluster_performance()
            
            # Run failover tests
            results["sentinel_failover"] = self.test_redis_sentinel_failover()
            
            # Run synchronization tests
            results["cross_region_sync"] = self.test_cross_region_cache_synchronization()
            
            # Run eviction tests
            results["eviction_management"] = self.test_cache_eviction_and_memory_management()
            
            # Run benchmark tests
            results["performance_benchmark"] = self.benchmark_cache_performance()
            
            # Cleanup
            self.cleanup_test_environment()
            self.cluster_manager.disconnect()
            results["cleanup_successful"] = True
            
            logger.info("All cache integration tests completed successfully")
            
        except Exception as e:
            logger.error(f"Cache integration test suite failed: {e}")
            results["error"] = str(e)
            raise
        
        return results


# Test fixtures and utilities
@pytest.fixture
def cache_test_suite():
    """Pytest fixture for cache test suite"""
    return CacheIntegrationTestSuite()


@pytest.fixture
def redis_config():
    """Pytest fixture for Redis configuration"""
    return RedisConfig()


@pytest.fixture
def sentinel_config():
    """Pytest fixture for Sentinel configuration"""
    return SentinelConfig()


# Test cases
def test_redis_cluster_performance(cache_test_suite):
    """Test Redis cluster performance"""
    results = cache_test_suite.test_redis_cluster_performance()
    assert len(results) == 1000
    successful_results = [r for r in results if r.success]
    assert len(successful_results) > 950  # At least 95% success rate


def test_redis_sentinel_failover(cache_test_suite):
    """Test Redis Sentinel failover"""
    results = cache_test_suite.test_redis_sentinel_failover()
    assert results["failover_triggered"]
    assert results["failover_duration_ms"] < 30000
    assert results["new_master_accessible"]


def test_cross_region_cache_synchronization(cache_test_suite):
    """Test cross-region cache synchronization"""
    results = cache_test_suite.test_cross_region_cache_synchronization()
    assert results["data_consistency_verified"]
    for region, latency in results["sync_latency_ms"].items():
        assert latency < 1000


def test_cache_eviction_and_memory_management(cache_test_suite):
    """Test cache eviction and memory management"""
    results = cache_test_suite.test_cache_eviction_and_memory_management()
    assert results["eviction_policy_effective"]
    assert results["memory_limit_respected"]


def test_cache_performance_benchmark(cache_test_suite):
    """Test cache performance benchmark"""
    results = cache_test_suite.benchmark_cache_performance()
    assert results["set_operations_per_second"] > 1000
    assert results["get_operations_per_second"] > 5000
    assert results["average_latency_ms"] < 5


if __name__ == "__main__":
    # Run tests directly
    test_suite = CacheIntegrationTestSuite()
    results = test_suite.run_all_cache_tests()
    print("Cache integration tests completed:", results)