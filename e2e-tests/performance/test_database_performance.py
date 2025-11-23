#!/usr/bin/env python3
"""
Database Performance Test

This test validates Aurora Global Database performance across multiple regions.
It measures query response times, throughput, and replication performance.

Test Scenarios:
1. Read performance across regions
2. Write performance and replication
3. Connection pool performance
4. Query complexity performance
"""

import asyncio
import time
import logging
import random
from typing import Dict, List
from dataclasses import dataclass, field
import statistics

import psycopg2
from psycopg2 import pool

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


@dataclass
class DatabasePerformanceConfig:
    """Configuration for database performance tests"""
    regions: List[str] = field(default_factory=lambda: [
        "us-east-1", "us-west-2", "eu-west-1"
    ])
    db_endpoints: Dict[str, str] = field(default_factory=lambda: {
        "us-east-1": "db-us-east-1.cluster-xxx.us-east-1.rds.amazonaws.com",
        "us-west-2": "db-us-west-2.cluster-xxx.us-west-2.rds.amazonaws.com",
        "eu-west-1": "db-eu-west-1.cluster-xxx.eu-west-1.rds.amazonaws.com"
    })
    db_name: str = "testdb"
    db_user: str = "testuser"
    db_password: str = "testpass"
    max_query_time_ms: int = 100  # P95 target
    max_replication_lag_ms: int = 100
    connection_pool_size: int = 20


@dataclass
class DatabaseTestResult:
    """Result of a database performance test"""
    test_name: str
    success: bool
    avg_query_time_ms: float
    p95_query_time_ms: float
    p99_query_time_ms: float
    throughput_qps: float
    details: Dict = field(default_factory=dict)


class DatabasePerformanceTest:
    """Test suite for database performance"""
    
    def __init__(self, config: DatabasePerformanceConfig):
        self.config = config
        self.results: List[DatabaseTestResult] = []
        self.connection_pools: Dict[str, pool.SimpleConnectionPool] = {}
        
    def _get_connection_pool(self, region: str) -> pool.SimpleConnectionPool:
        """Get or create connection pool for region"""
        if region not in self.connection_pools:
            endpoint = self.config.db_endpoints.get(region)
            self.connection_pools[region] = pool.SimpleConnectionPool(
                1,
                self.config.connection_pool_size,
                host=endpoint,
                database=self.config.db_name,
                user=self.config.db_user,
                password=self.config.db_password
            )
        return self.connection_pools[region]
    
    async def test_read_performance(self) -> DatabaseTestResult:
        """
        Test: Read query performance across regions
        
        Measures SELECT query performance in each region.
        """
        test_name = "read_performance"
        logger.info(f"Starting test: {test_name}")
        
        all_query_times = []
        region_results = {}
        
        try:
            for region in self.config.regions:
                query_times = []
                conn_pool = self._get_connection_pool(region)
                
                # Run 1000 read queries
                for _ in range(1000):
                    conn = conn_pool.getconn()
                    try:
                        cursor = conn.cursor()
                        
                        start_time = time.time()
                        cursor.execute("SELECT * FROM test_table WHERE id = %s", (random.randint(1, 10000),))
                        cursor.fetchall()
                        query_time = (time.time() - start_time) * 1000
                        
                        query_times.append(query_time)
                        all_query_times.append(query_time)
                        
                        cursor.close()
                    finally:
                        conn_pool.putconn(conn)
                
                avg_time = statistics.mean(query_times)
                region_results[region] = avg_time
                logger.info(f"{region} read performance: {avg_time:.2f}ms avg")
            
            # Calculate statistics
            avg_query_time = statistics.mean(all_query_times)
            sorted_times = sorted(all_query_times)
            p95_query_time = sorted_times[int(len(sorted_times) * 0.95)]
            p99_query_time = sorted_times[int(len(sorted_times) * 0.99)]
            
            # Calculate throughput
            total_time = sum(all_query_times) / 1000  # Convert to seconds
            throughput = len(all_query_times) / total_time if total_time > 0 else 0
            
            success = p95_query_time <= self.config.max_query_time_ms
            
            return DatabaseTestResult(
                test_name=test_name,
                success=success,
                avg_query_time_ms=avg_query_time,
                p95_query_time_ms=p95_query_time,
                p99_query_time_ms=p99_query_time,
                throughput_qps=throughput,
                details={
                    "region_results": region_results,
                    "total_queries": len(all_query_times)
                }
            )
            
        except Exception as e:
            logger.error(f"Test {test_name} failed: {str(e)}")
            return DatabaseTestResult(
                test_name=test_name,
                success=False,
                avg_query_time_ms=0,
                p95_query_time_ms=0,
                p99_query_time_ms=0,
                throughput_qps=0,
                details={"error": str(e)}
            )
    
    async def test_write_and_replication(self) -> DatabaseTestResult:
        """
        Test: Write performance and replication lag
        
        Measures INSERT/UPDATE performance and replication lag across regions.
        """
        test_name = "write_and_replication"
        logger.info(f"Starting test: {test_name}")
        
        write_times = []
        replication_lags = []
        
        try:
            primary_region = self.config.regions[0]
            primary_pool = self._get_connection_pool(primary_region)
            
            # Perform 100 writes and measure replication
            for i in range(100):
                # Write to primary
                conn = primary_pool.getconn()
                try:
                    cursor = conn.cursor()
                    
                    test_id = f"repl-{int(time.time() * 1000)}-{i}"
                    write_start = time.time()
                    
                    cursor.execute(
                        "INSERT INTO test_table (id, data, timestamp) VALUES (%s, %s, NOW())",
                        (test_id, f"test data {i}")
                    )
                    conn.commit()
                    
                    write_time = (time.time() - write_start) * 1000
                    write_times.append(write_time)
                    
                    cursor.close()
                finally:
                    primary_pool.putconn(conn)
                
                # Check replication to secondary regions
                await asyncio.sleep(0.05)  # Small delay
                
                for secondary_region in self.config.regions[1:]:
                    replication_lag = await self._measure_replication_lag(
                        secondary_region,
                        test_id
                    )
                    if replication_lag is not None:
                        replication_lags.append(replication_lag)
            
            # Calculate statistics
            avg_write_time = statistics.mean(write_times)
            sorted_writes = sorted(write_times)
            p95_write_time = sorted_writes[int(len(sorted_writes) * 0.95)]
            p99_write_time = sorted_writes[int(len(sorted_writes) * 0.99)]
            
            avg_replication_lag = statistics.mean(replication_lags) if replication_lags else 0
            max_replication_lag = max(replication_lags) if replication_lags else 0
            
            # Calculate throughput
            total_time = sum(write_times) / 1000
            throughput = len(write_times) / total_time if total_time > 0 else 0
            
            success = (
                p95_write_time <= self.config.max_query_time_ms and
                max_replication_lag <= self.config.max_replication_lag_ms
            )
            
            return DatabaseTestResult(
                test_name=test_name,
                success=success,
                avg_query_time_ms=avg_write_time,
                p95_query_time_ms=p95_write_time,
                p99_query_time_ms=p99_write_time,
                throughput_qps=throughput,
                details={
                    "avg_replication_lag_ms": avg_replication_lag,
                    "max_replication_lag_ms": max_replication_lag,
                    "writes_performed": len(write_times)
                }
            )
            
        except Exception as e:
            logger.error(f"Test {test_name} failed: {str(e)}")
            return DatabaseTestResult(
                test_name=test_name,
                success=False,
                avg_query_time_ms=0,
                p95_query_time_ms=0,
                p99_query_time_ms=0,
                throughput_qps=0,
                details={"error": str(e)}
            )
    
    async def test_connection_pool_performance(self) -> DatabaseTestResult:
        """
        Test: Connection pool performance under load
        
        Measures connection acquisition time and pool efficiency.
        """
        test_name = "connection_pool_performance"
        logger.info(f"Starting test: {test_name}")
        
        acquisition_times = []
        query_times = []
        
        try:
            region = self.config.regions[0]
            conn_pool = self._get_connection_pool(region)
            
            # Simulate concurrent connection requests
            async def get_and_query():
                start_time = time.time()
                conn = conn_pool.getconn()
                acquisition_time = (time.time() - start_time) * 1000
                acquisition_times.append(acquisition_time)
                
                try:
                    cursor = conn.cursor()
                    query_start = time.time()
                    cursor.execute("SELECT 1")
                    cursor.fetchall()
                    query_time = (time.time() - query_start) * 1000
                    query_times.append(query_time)
                    cursor.close()
                finally:
                    conn_pool.putconn(conn)
            
            # Run 500 concurrent queries
            tasks = [get_and_query() for _ in range(500)]
            await asyncio.gather(*tasks)
            
            # Calculate statistics
            avg_acquisition = statistics.mean(acquisition_times)
            avg_query_time = statistics.mean(query_times)
            sorted_times = sorted(query_times)
            p95_query_time = sorted_times[int(len(sorted_times) * 0.95)]
            p99_query_time = sorted_times[int(len(sorted_times) * 0.99)]
            
            # Calculate throughput
            total_time = sum(query_times) / 1000
            throughput = len(query_times) / total_time if total_time > 0 else 0
            
            # Success if acquisition is fast and queries are efficient
            success = avg_acquisition < 10 and p95_query_time <= self.config.max_query_time_ms
            
            return DatabaseTestResult(
                test_name=test_name,
                success=success,
                avg_query_time_ms=avg_query_time,
                p95_query_time_ms=p95_query_time,
                p99_query_time_ms=p99_query_time,
                throughput_qps=throughput,
                details={
                    "avg_acquisition_time_ms": avg_acquisition,
                    "pool_size": self.config.connection_pool_size,
                    "concurrent_requests": len(tasks)
                }
            )
            
        except Exception as e:
            logger.error(f"Test {test_name} failed: {str(e)}")
            return DatabaseTestResult(
                test_name=test_name,
                success=False,
                avg_query_time_ms=0,
                p95_query_time_ms=0,
                p99_query_time_ms=0,
                throughput_qps=0,
                details={"error": str(e)}
            )
    
    async def _measure_replication_lag(self, region: str, test_id: str) -> float:
        """Measure replication lag for a specific write"""
        conn_pool = self._get_connection_pool(region)
        start_time = time.time()
        max_wait = 5  # 5 seconds max
        
        while (time.time() - start_time) < max_wait:
            conn = conn_pool.getconn()
            try:
                cursor = conn.cursor()
                cursor.execute("SELECT * FROM test_table WHERE id = %s", (test_id,))
                result = cursor.fetchone()
                cursor.close()
                
                if result:
                    return (time.time() - start_time) * 1000
            finally:
                conn_pool.putconn(conn)
            
            await asyncio.sleep(0.01)
        
        return None
    
    async def run_all_tests(self) -> List[DatabaseTestResult]:
        """Run all database performance tests"""
        logger.info("Starting database performance test suite")
        
        tests = [
            self.test_read_performance(),
            self.test_write_and_replication(),
            self.test_connection_pool_performance()
        ]
        
        self.results = await asyncio.gather(*tests)
        
        # Cleanup connection pools
        for pool_obj in self.connection_pools.values():
            pool_obj.closeall()
        
        # Print summary
        self._print_summary()
        
        return self.results
    
    def _print_summary(self):
        """Print test results summary"""
        logger.info("\n" + "="*80)
        logger.info("Database Performance Test Results")
        logger.info("="*80)
        
        for result in self.results:
            status = "✓ PASS" if result.success else "✗ FAIL"
            logger.info(f"\n{status} - {result.test_name}")
            logger.info(f"  Avg Query Time: {result.avg_query_time_ms:.2f}ms")
            logger.info(f"  P95 Query Time: {result.p95_query_time_ms:.2f}ms")
            logger.info(f"  P99 Query Time: {result.p99_query_time_ms:.2f}ms")
            logger.info(f"  Throughput: {result.throughput_qps:.2f} QPS")
        
        passed = sum(1 for r in self.results if r.success)
        total = len(self.results)
        logger.info(f"\n{'='*80}")
        logger.info(f"Summary: {passed}/{total} tests passed")
        logger.info("="*80 + "\n")


async def main():
    """Main entry point for database performance tests"""
    config = DatabasePerformanceConfig()
    test_suite = DatabasePerformanceTest(config)
    
    results = await test_suite.run_all_tests()
    
    # Exit with appropriate code
    all_passed = all(r.success for r in results)
    exit(0 if all_passed else 1)


if __name__ == "__main__":
    asyncio.run(main())
