#!/usr/bin/env python3
"""
Cross-Region Data Consistency Test

This test validates data consistency across multiple AWS regions in an Active-Active architecture.
It verifies that data written to one region is properly replicated to other regions within
acceptable time limits.

Test Scenarios:
1. Write data to Region A, verify replication to Region B
2. Concurrent writes to multiple regions
3. Conflict resolution validation
4. Data integrity checks across regions
"""

import asyncio
import time
import json
import logging
from typing import Dict, List, Optional
from dataclasses import dataclass
from datetime import datetime, timedelta

import boto3
import requests
from botocore.exceptions import ClientError

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


@dataclass
class TestConfig:
    """Configuration for cross-region data consistency tests"""
    primary_region: str = "us-east-1"
    secondary_regions: List[str] = None
    api_endpoints: Dict[str, str] = None
    max_replication_delay_ms: int = 100  # P99 target
    test_timeout_seconds: int = 300
    
    def __post_init__(self):
        if self.secondary_regions is None:
            self.secondary_regions = ["us-west-2", "eu-west-1"]
        if self.api_endpoints is None:
            self.api_endpoints = {
                "us-east-1": "https://api-us-east-1.example.com",
                "us-west-2": "https://api-us-west-2.example.com",
                "eu-west-1": "https://api-eu-west-1.example.com"
            }


@dataclass
class TestResult:
    """Result of a data consistency test"""
    test_name: str
    success: bool
    replication_delay_ms: Optional[float]
    error_message: Optional[str] = None
    details: Optional[Dict] = None


class CrossRegionDataConsistencyTest:
    """Test suite for cross-region data consistency"""
    
    def __init__(self, config: TestConfig):
        self.config = config
        self.results: List[TestResult] = []
        
    async def test_write_and_replicate(self) -> TestResult:
        """
        Test: Write data to primary region and verify replication to secondary regions
        
        Steps:
        1. Write test data to primary region
        2. Wait for replication
        3. Verify data exists in all secondary regions
        4. Measure replication delay
        """
        test_name = "write_and_replicate"
        logger.info(f"Starting test: {test_name}")
        
        try:
            # Generate unique test data
            test_id = f"test-{int(time.time() * 1000)}"
            test_data = {
                "id": test_id,
                "timestamp": datetime.utcnow().isoformat(),
                "data": "Cross-region test data",
                "region": self.config.primary_region
            }
            
            # Write to primary region
            write_start = time.time()
            primary_response = await self._write_data(
                self.config.primary_region,
                test_data
            )
            write_time = (time.time() - write_start) * 1000
            
            if not primary_response:
                return TestResult(
                    test_name=test_name,
                    success=False,
                    replication_delay_ms=None,
                    error_message="Failed to write to primary region"
                )
            
            logger.info(f"Data written to primary region in {write_time:.2f}ms")
            
            # Wait and verify replication to secondary regions
            replication_delays = []
            for region in self.config.secondary_regions:
                delay = await self._wait_for_replication(region, test_id)
                if delay is None:
                    return TestResult(
                        test_name=test_name,
                        success=False,
                        replication_delay_ms=None,
                        error_message=f"Replication to {region} failed or timed out"
                    )
                replication_delays.append(delay)
                logger.info(f"Replication to {region}: {delay:.2f}ms")
            
            # Calculate P99 replication delay
            max_delay = max(replication_delays)
            avg_delay = sum(replication_delays) / len(replication_delays)
            
            success = max_delay <= self.config.max_replication_delay_ms
            
            return TestResult(
                test_name=test_name,
                success=success,
                replication_delay_ms=max_delay,
                details={
                    "write_time_ms": write_time,
                    "avg_replication_delay_ms": avg_delay,
                    "max_replication_delay_ms": max_delay,
                    "replication_delays": dict(zip(self.config.secondary_regions, replication_delays))
                }
            )
            
        except Exception as e:
            logger.error(f"Test {test_name} failed with exception: {str(e)}")
            return TestResult(
                test_name=test_name,
                success=False,
                replication_delay_ms=None,
                error_message=str(e)
            )
    
    async def test_concurrent_writes(self) -> TestResult:
        """
        Test: Concurrent writes to multiple regions
        
        Validates that concurrent writes to different regions are properly handled
        and eventually consistent across all regions.
        """
        test_name = "concurrent_writes"
        logger.info(f"Starting test: {test_name}")
        
        try:
            # Create concurrent write tasks
            test_id = f"concurrent-{int(time.time() * 1000)}"
            write_tasks = []
            
            for i, region in enumerate([self.config.primary_region] + self.config.secondary_regions):
                test_data = {
                    "id": test_id,
                    "timestamp": datetime.utcnow().isoformat(),
                    "data": f"Concurrent write from {region}",
                    "region": region,
                    "sequence": i
                }
                write_tasks.append(self._write_data(region, test_data))
            
            # Execute concurrent writes
            start_time = time.time()
            results = await asyncio.gather(*write_tasks, return_exceptions=True)
            write_duration = (time.time() - start_time) * 1000
            
            # Check for write failures
            failures = [r for r in results if isinstance(r, Exception) or not r]
            if failures:
                return TestResult(
                    test_name=test_name,
                    success=False,
                    replication_delay_ms=None,
                    error_message=f"{len(failures)} concurrent writes failed"
                )
            
            # Wait for eventual consistency
            await asyncio.sleep(2)  # Allow time for conflict resolution
            
            # Verify data consistency across all regions
            consistency_check = await self._verify_consistency(test_id)
            
            return TestResult(
                test_name=test_name,
                success=consistency_check,
                replication_delay_ms=write_duration,
                details={
                    "concurrent_writes": len(write_tasks),
                    "write_duration_ms": write_duration,
                    "consistency_achieved": consistency_check
                }
            )
            
        except Exception as e:
            logger.error(f"Test {test_name} failed with exception: {str(e)}")
            return TestResult(
                test_name=test_name,
                success=False,
                replication_delay_ms=None,
                error_message=str(e)
            )
    
    async def test_conflict_resolution(self) -> TestResult:
        """
        Test: Conflict resolution mechanism
        
        Validates that the Last-Write-Wins (LWW) conflict resolution strategy
        works correctly when concurrent updates occur.
        """
        test_name = "conflict_resolution"
        logger.info(f"Starting test: {test_name}")
        
        try:
            test_id = f"conflict-{int(time.time() * 1000)}"
            
            # Create conflicting writes with different timestamps
            write_tasks = []
            for i, region in enumerate([self.config.primary_region] + self.config.secondary_regions):
                test_data = {
                    "id": test_id,
                    "timestamp": (datetime.utcnow() + timedelta(milliseconds=i*10)).isoformat(),
                    "data": f"Update {i} from {region}",
                    "region": region,
                    "version": i
                }
                write_tasks.append(self._write_data(region, test_data))
            
            # Execute conflicting writes
            await asyncio.gather(*write_tasks)
            
            # Wait for conflict resolution
            await asyncio.sleep(3)
            
            # Verify that the latest write won across all regions
            final_values = []
            for region in [self.config.primary_region] + self.config.secondary_regions:
                data = await self._read_data(region, test_id)
                if data:
                    final_values.append(data.get('version'))
            
            # All regions should have the same final value (highest version)
            expected_version = len(write_tasks) - 1
            success = all(v == expected_version for v in final_values)
            
            return TestResult(
                test_name=test_name,
                success=success,
                replication_delay_ms=None,
                details={
                    "expected_version": expected_version,
                    "final_versions": final_values,
                    "consistency_achieved": success
                }
            )
            
        except Exception as e:
            logger.error(f"Test {test_name} failed with exception: {str(e)}")
            return TestResult(
                test_name=test_name,
                success=False,
                replication_delay_ms=None,
                error_message=str(e)
            )
    
    async def _write_data(self, region: str, data: Dict) -> bool:
        """Write data to specified region"""
        try:
            endpoint = self.config.api_endpoints.get(region)
            if not endpoint:
                logger.error(f"No endpoint configured for region {region}")
                return False
            
            response = requests.post(
                f"{endpoint}/api/v1/test-data",
                json=data,
                timeout=10
            )
            return response.status_code in [200, 201]
        except Exception as e:
            logger.error(f"Failed to write data to {region}: {str(e)}")
            return False
    
    async def _read_data(self, region: str, test_id: str) -> Optional[Dict]:
        """Read data from specified region"""
        try:
            endpoint = self.config.api_endpoints.get(region)
            if not endpoint:
                return None
            
            response = requests.get(
                f"{endpoint}/api/v1/test-data/{test_id}",
                timeout=10
            )
            if response.status_code == 200:
                return response.json()
            return None
        except Exception as e:
            logger.error(f"Failed to read data from {region}: {str(e)}")
            return None
    
    async def _wait_for_replication(self, region: str, test_id: str, max_wait_ms: int = 5000) -> Optional[float]:
        """Wait for data to replicate to specified region and measure delay"""
        start_time = time.time()
        max_wait_seconds = max_wait_ms / 1000
        
        while (time.time() - start_time) < max_wait_seconds:
            data = await self._read_data(region, test_id)
            if data:
                delay_ms = (time.time() - start_time) * 1000
                return delay_ms
            await asyncio.sleep(0.1)  # Poll every 100ms
        
        return None  # Timeout
    
    async def _verify_consistency(self, test_id: str) -> bool:
        """Verify data consistency across all regions"""
        try:
            data_values = []
            for region in [self.config.primary_region] + self.config.secondary_regions:
                data = await self._read_data(region, test_id)
                if data:
                    data_values.append(json.dumps(data, sort_keys=True))
            
            # All regions should have the same data
            return len(set(data_values)) == 1
        except Exception as e:
            logger.error(f"Consistency verification failed: {str(e)}")
            return False
    
    async def run_all_tests(self) -> List[TestResult]:
        """Run all data consistency tests"""
        logger.info("Starting cross-region data consistency test suite")
        
        tests = [
            self.test_write_and_replicate(),
            self.test_concurrent_writes(),
            self.test_conflict_resolution()
        ]
        
        self.results = await asyncio.gather(*tests)
        
        # Print summary
        self._print_summary()
        
        return self.results
    
    def _print_summary(self):
        """Print test results summary"""
        logger.info("\n" + "="*80)
        logger.info("Cross-Region Data Consistency Test Results")
        logger.info("="*80)
        
        for result in self.results:
            status = "✓ PASS" if result.success else "✗ FAIL"
            logger.info(f"\n{status} - {result.test_name}")
            
            if result.replication_delay_ms is not None:
                logger.info(f"  Replication Delay: {result.replication_delay_ms:.2f}ms")
            
            if result.error_message:
                logger.info(f"  Error: {result.error_message}")
            
            if result.details:
                logger.info(f"  Details: {json.dumps(result.details, indent=2)}")
        
        passed = sum(1 for r in self.results if r.success)
        total = len(self.results)
        logger.info(f"\n{'='*80}")
        logger.info(f"Summary: {passed}/{total} tests passed")
        logger.info("="*80 + "\n")


async def main():
    """Main entry point for cross-region data consistency tests"""
    config = TestConfig()
    test_suite = CrossRegionDataConsistencyTest(config)
    
    results = await test_suite.run_all_tests()
    
    # Exit with appropriate code
    all_passed = all(r.success for r in results)
    exit(0 if all_passed else 1)


if __name__ == "__main__":
    asyncio.run(main())
