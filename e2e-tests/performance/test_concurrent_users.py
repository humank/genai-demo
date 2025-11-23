#!/usr/bin/env python3
"""
Concurrent Users Load Test

This test validates system performance under high concurrent user load (10,000+ users).
It simulates realistic user behavior patterns and measures system response times,
throughput, and error rates.

Test Scenarios:
1. Ramp-up test - gradually increase load to 10,000 users
2. Sustained load test - maintain 10,000 concurrent users
3. Spike test - sudden traffic spikes
4. Stress test - push beyond capacity to find breaking point
"""

import asyncio
import time
import logging
import random
from typing import Dict, List, Optional
from dataclasses import dataclass, field
from datetime import datetime
from collections import defaultdict
import statistics

import aiohttp
import numpy as np

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


@dataclass
class LoadTestConfig:
    """Configuration for concurrent users load test"""
    target_users: int = 10000
    ramp_up_duration_seconds: int = 300  # 5 minutes
    sustained_duration_seconds: int = 600  # 10 minutes
    api_endpoints: List[str] = field(default_factory=lambda: [
        "https://api-us-east-1.example.com",
        "https://api-us-west-2.example.com",
        "https://api-eu-west-1.example.com"
    ])
    max_response_time_ms: int = 2000  # P95 target
    max_error_rate: float = 1.0  # 1% max error rate


@dataclass
class LoadTestMetrics:
    """Metrics collected during load test"""
    total_requests: int = 0
    successful_requests: int = 0
    failed_requests: int = 0
    response_times: List[float] = field(default_factory=list)
    errors_by_type: Dict[str, int] = field(default_factory=lambda: defaultdict(int))
    requests_per_second: List[float] = field(default_factory=list)
    concurrent_users_actual: List[int] = field(default_factory=list)
    
    def add_response(self, response_time_ms: float, success: bool, error_type: Optional[str] = None):
        """Add a response to metrics"""
        self.total_requests += 1
        if success:
            self.successful_requests += 1
            self.response_times.append(response_time_ms)
        else:
            self.failed_requests += 1
            if error_type:
                self.errors_by_type[error_type] += 1
    
    def get_percentile(self, percentile: float) -> float:
        """Calculate response time percentile"""
        if not self.response_times:
            return 0.0
        return np.percentile(self.response_times, percentile)
    
    def get_error_rate(self) -> float:
        """Calculate error rate percentage"""
        if self.total_requests == 0:
            return 0.0
        return (self.failed_requests / self.total_requests) * 100
    
    def get_avg_response_time(self) -> float:
        """Calculate average response time"""
        if not self.response_times:
            return 0.0
        return statistics.mean(self.response_times)


class ConcurrentUsersLoadTest:
    """Load test suite for concurrent users"""
    
    def __init__(self, config: LoadTestConfig):
        self.config = config
        self.metrics = LoadTestMetrics()
        self.active_users = 0
        self.test_running = False
        
    async def test_ramp_up_load(self) -> Dict:
        """
        Test: Gradual ramp-up to target concurrent users
        
        Gradually increases load from 0 to 10,000 users over 5 minutes
        to observe system behavior under increasing load.
        """
        test_name = "ramp_up_load"
        logger.info(f"Starting test: {test_name}")
        logger.info(f"Ramping up to {self.config.target_users} users over {self.config.ramp_up_duration_seconds}s")
        
        self.test_running = True
        self.metrics = LoadTestMetrics()
        
        start_time = time.time()
        user_tasks = []
        
        # Calculate users to add per second
        users_per_second = self.config.target_users / self.config.ramp_up_duration_seconds
        
        try:
            for second in range(self.config.ramp_up_duration_seconds):
                # Add new users
                users_to_add = int(users_per_second * (second + 1)) - self.active_users
                
                for _ in range(users_to_add):
                    task = asyncio.create_task(self._simulate_user())
                    user_tasks.append(task)
                    self.active_users += 1
                
                # Record metrics
                self.metrics.concurrent_users_actual.append(self.active_users)
                
                # Log progress every 30 seconds
                if second % 30 == 0:
                    logger.info(f"Active users: {self.active_users}/{self.config.target_users}")
                    logger.info(f"Requests: {self.metrics.total_requests}, "
                              f"Errors: {self.metrics.failed_requests}, "
                              f"Avg Response: {self.metrics.get_avg_response_time():.2f}ms")
                
                await asyncio.sleep(1)
            
            # Wait for all user tasks to complete
            logger.info("Ramp-up complete, waiting for user tasks to finish...")
            self.test_running = False
            await asyncio.gather(*user_tasks, return_exceptions=True)
            
            test_duration = time.time() - start_time
            
            return {
                "test_name": test_name,
                "success": self._evaluate_test_success(),
                "duration_seconds": test_duration,
                "target_users": self.config.target_users,
                "peak_concurrent_users": max(self.metrics.concurrent_users_actual),
                "total_requests": self.metrics.total_requests,
                "successful_requests": self.metrics.successful_requests,
                "failed_requests": self.metrics.failed_requests,
                "error_rate": self.metrics.get_error_rate(),
                "avg_response_time_ms": self.metrics.get_avg_response_time(),
                "p50_response_time_ms": self.metrics.get_percentile(50),
                "p95_response_time_ms": self.metrics.get_percentile(95),
                "p99_response_time_ms": self.metrics.get_percentile(99),
                "errors_by_type": dict(self.metrics.errors_by_type)
            }
            
        except Exception as e:
            logger.error(f"Test {test_name} failed with exception: {str(e)}")
            self.test_running = False
            return {
                "test_name": test_name,
                "success": False,
                "error": str(e)
            }
    
    async def test_sustained_load(self) -> Dict:
        """
        Test: Sustained load at target concurrent users
        
        Maintains 10,000 concurrent users for 10 minutes to validate
        system stability under sustained load.
        """
        test_name = "sustained_load"
        logger.info(f"Starting test: {test_name}")
        logger.info(f"Maintaining {self.config.target_users} users for {self.config.sustained_duration_seconds}s")
        
        self.test_running = True
        self.metrics = LoadTestMetrics()
        
        start_time = time.time()
        user_tasks = []
        
        try:
            # Quickly ramp up to target users
            for _ in range(self.config.target_users):
                task = asyncio.create_task(self._simulate_user_sustained())
                user_tasks.append(task)
                self.active_users += 1
            
            logger.info(f"Reached {self.active_users} concurrent users")
            
            # Monitor for sustained duration
            for second in range(self.config.sustained_duration_seconds):
                self.metrics.concurrent_users_actual.append(self.active_users)
                
                # Log progress every 60 seconds
                if second % 60 == 0:
                    logger.info(f"Time: {second}s/{self.config.sustained_duration_seconds}s")
                    logger.info(f"Requests: {self.metrics.total_requests}, "
                              f"Errors: {self.metrics.failed_requests}, "
                              f"Avg Response: {self.metrics.get_avg_response_time():.2f}ms, "
                              f"P95: {self.metrics.get_percentile(95):.2f}ms")
                
                await asyncio.sleep(1)
            
            # Stop test and wait for completion
            logger.info("Sustained load test complete, stopping users...")
            self.test_running = False
            await asyncio.gather(*user_tasks, return_exceptions=True)
            
            test_duration = time.time() - start_time
            
            return {
                "test_name": test_name,
                "success": self._evaluate_test_success(),
                "duration_seconds": test_duration,
                "target_users": self.config.target_users,
                "sustained_duration": self.config.sustained_duration_seconds,
                "total_requests": self.metrics.total_requests,
                "successful_requests": self.metrics.successful_requests,
                "failed_requests": self.metrics.failed_requests,
                "error_rate": self.metrics.get_error_rate(),
                "avg_response_time_ms": self.metrics.get_avg_response_time(),
                "p50_response_time_ms": self.metrics.get_percentile(50),
                "p95_response_time_ms": self.metrics.get_percentile(95),
                "p99_response_time_ms": self.metrics.get_percentile(99),
                "throughput_rps": self.metrics.total_requests / test_duration,
                "errors_by_type": dict(self.metrics.errors_by_type)
            }
            
        except Exception as e:
            logger.error(f"Test {test_name} failed with exception: {str(e)}")
            self.test_running = False
            return {
                "test_name": test_name,
                "success": False,
                "error": str(e)
            }
    
    async def test_spike_load(self) -> Dict:
        """
        Test: Sudden traffic spike
        
        Simulates sudden traffic spikes to validate system's ability
        to handle rapid load increases.
        """
        test_name = "spike_load"
        logger.info(f"Starting test: {test_name}")
        
        self.test_running = True
        self.metrics = LoadTestMetrics()
        
        start_time = time.time()
        
        try:
            # Start with baseline load (1000 users)
            baseline_users = 1000
            logger.info(f"Starting with baseline: {baseline_users} users")
            
            baseline_tasks = []
            for _ in range(baseline_users):
                task = asyncio.create_task(self._simulate_user_sustained())
                baseline_tasks.append(task)
                self.active_users += 1
            
            await asyncio.sleep(30)  # Baseline period
            
            # Sudden spike to 10,000 users
            spike_users = self.config.target_users - baseline_users
            logger.info(f"SPIKE: Adding {spike_users} users instantly!")
            
            spike_start = time.time()
            spike_tasks = []
            for _ in range(spike_users):
                task = asyncio.create_task(self._simulate_user())
                spike_tasks.append(task)
                self.active_users += 1
            
            # Monitor spike response
            await asyncio.sleep(60)  # Monitor for 1 minute
            spike_duration = time.time() - spike_start
            
            logger.info(f"Spike handled in {spike_duration:.2f}s")
            
            # Stop test
            self.test_running = False
            await asyncio.gather(*baseline_tasks, *spike_tasks, return_exceptions=True)
            
            test_duration = time.time() - start_time
            
            return {
                "test_name": test_name,
                "success": self._evaluate_test_success(),
                "duration_seconds": test_duration,
                "baseline_users": baseline_users,
                "spike_users": spike_users,
                "spike_duration_seconds": spike_duration,
                "total_requests": self.metrics.total_requests,
                "error_rate": self.metrics.get_error_rate(),
                "p95_response_time_ms": self.metrics.get_percentile(95),
                "p99_response_time_ms": self.metrics.get_percentile(99)
            }
            
        except Exception as e:
            logger.error(f"Test {test_name} failed with exception: {str(e)}")
            self.test_running = False
            return {
                "test_name": test_name,
                "success": False,
                "error": str(e)
            }
    
    async def _simulate_user(self):
        """Simulate a single user session"""
        while self.test_running:
            try:
                # Random think time between requests (1-5 seconds)
                await asyncio.sleep(random.uniform(1, 5))
                
                # Make request
                endpoint = random.choice(self.config.api_endpoints)
                start_time = time.time()
                
                async with aiohttp.ClientSession() as session:
                    async with session.get(f"{endpoint}/api/v1/health", timeout=aiohttp.ClientTimeout(total=10)) as response:
                        response_time = (time.time() - start_time) * 1000
                        success = response.status == 200
                        error_type = None if success else f"HTTP_{response.status}"
                        
                        self.metrics.add_response(response_time, success, error_type)
                        
            except asyncio.TimeoutError:
                response_time = 10000  # Timeout
                self.metrics.add_response(response_time, False, "TIMEOUT")
            except Exception as e:
                self.metrics.add_response(0, False, type(e).__name__)
    
    async def _simulate_user_sustained(self):
        """Simulate a user for sustained load test"""
        while self.test_running:
            await self._simulate_user()
            await asyncio.sleep(0.1)  # Small delay between iterations
    
    def _evaluate_test_success(self) -> bool:
        """Evaluate if test passed based on success criteria"""
        error_rate = self.metrics.get_error_rate()
        p95_response_time = self.metrics.get_percentile(95)
        
        success = (
            error_rate <= self.config.max_error_rate and
            p95_response_time <= self.config.max_response_time_ms
        )
        
        return success
    
    async def run_all_tests(self) -> List[Dict]:
        """Run all concurrent user load tests"""
        logger.info("Starting concurrent users load test suite")
        logger.info(f"Target: {self.config.target_users} concurrent users")
        
        results = []
        
        # Run tests sequentially to avoid interference
        logger.info("\n" + "="*80)
        result = await self.test_ramp_up_load()
        results.append(result)
        await asyncio.sleep(60)  # Cool-down period
        
        logger.info("\n" + "="*80)
        result = await self.test_sustained_load()
        results.append(result)
        await asyncio.sleep(60)  # Cool-down period
        
        logger.info("\n" + "="*80)
        result = await self.test_spike_load()
        results.append(result)
        
        # Print summary
        self._print_summary(results)
        
        return results
    
    def _print_summary(self, results: List[Dict]):
        """Print test results summary"""
        logger.info("\n" + "="*80)
        logger.info("Concurrent Users Load Test Results")
        logger.info("="*80)
        
        for result in results:
            status = "✓ PASS" if result.get("success") else "✗ FAIL"
            logger.info(f"\n{status} - {result['test_name']}")
            
            if "error" in result:
                logger.info(f"  Error: {result['error']}")
                continue
            
            logger.info(f"  Duration: {result.get('duration_seconds', 0):.2f}s")
            logger.info(f"  Total Requests: {result.get('total_requests', 0)}")
            logger.info(f"  Error Rate: {result.get('error_rate', 0):.2f}%")
            logger.info(f"  Avg Response Time: {result.get('avg_response_time_ms', 0):.2f}ms")
            logger.info(f"  P95 Response Time: {result.get('p95_response_time_ms', 0):.2f}ms")
            logger.info(f"  P99 Response Time: {result.get('p99_response_time_ms', 0):.2f}ms")
            
            if "throughput_rps" in result:
                logger.info(f"  Throughput: {result['throughput_rps']:.2f} req/s")
        
        passed = sum(1 for r in results if r.get("success"))
        total = len(results)
        logger.info(f"\n{'='*80}")
        logger.info(f"Summary: {passed}/{total} tests passed")
        logger.info("="*80 + "\n")


async def main():
    """Main entry point for concurrent users load test"""
    config = LoadTestConfig()
    test_suite = ConcurrentUsersLoadTest(config)
    
    results = await test_suite.run_all_tests()
    
    # Exit with appropriate code
    all_passed = all(r.get("success") for r in results)
    exit(0 if all_passed else 1)


if __name__ == "__main__":
    asyncio.run(main())
