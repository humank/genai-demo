#!/usr/bin/env python3
"""
CDN Performance Test

This test validates CloudFront CDN performance including cache hit rates,
edge location latency, and content delivery speed.
"""

import asyncio
import time
import logging
from typing import Dict, List
from dataclasses import dataclass, field
import statistics
import aiohttp

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)


@dataclass
class CDNPerformanceConfig:
    """Configuration for CDN performance tests"""
    cdn_endpoint: str = "https://cdn.example.com"
    test_paths: List[str] = field(default_factory=lambda: [
        "/static/app.js",
        "/static/styles.css",
        "/images/logo.png",
        "/api/health"
    ])
    target_cache_hit_rate: float = 90.0  # 90% target
    max_latency_ms: int = 100  # P95 target
    samples_per_path: int = 100


class CDNPerformanceTest:
    """Test suite for CDN performance"""
    
    def __init__(self, config: CDNPerformanceConfig):
        self.config = config
        
    async def test_cache_hit_rate(self) -> Dict:
        """Test CDN cache hit rate"""
        logger.info("Testing CDN cache hit rate...")
        
        cache_hits = 0
        cache_misses = 0
        
        for path in self.config.test_paths:
            for _ in range(self.config.samples_per_path):
                async with aiohttp.ClientSession() as session:
                    async with session.get(f"{self.config.cdn_endpoint}{path}") as response:
                        cache_status = response.headers.get('X-Cache', 'Miss')
                        if 'Hit' in cache_status:
                            cache_hits += 1
                        else:
                            cache_misses += 1
                
                await asyncio.sleep(0.01)
        
        total_requests = cache_hits + cache_misses
        hit_rate = (cache_hits / total_requests * 100) if total_requests > 0 else 0
        
        success = hit_rate >= self.config.target_cache_hit_rate
        
        logger.info(f"Cache hit rate: {hit_rate:.2f}% (Target: {self.config.target_cache_hit_rate}%)")
        
        return {
            "test_name": "cache_hit_rate",
            "success": success,
            "cache_hit_rate": hit_rate,
            "cache_hits": cache_hits,
            "cache_misses": cache_misses,
            "total_requests": total_requests
        }
    
    async def test_edge_latency(self) -> Dict:
        """Test CDN edge location latency"""
        logger.info("Testing CDN edge latency...")
        
        latencies = []
        
        for path in self.config.test_paths:
            for _ in range(self.config.samples_per_path):
                start_time = time.time()
                
                async with aiohttp.ClientSession() as session:
                    async with session.get(f"{self.config.cdn_endpoint}{path}") as response:
                        if response.status == 200:
                            latency = (time.time() - start_time) * 1000
                            latencies.append(latency)
                
                await asyncio.sleep(0.01)
        
        avg_latency = statistics.mean(latencies) if latencies else 0
        sorted_latencies = sorted(latencies)
        p95_latency = sorted_latencies[int(len(sorted_latencies) * 0.95)] if latencies else 0
        
        success = p95_latency <= self.config.max_latency_ms
        
        logger.info(f"CDN latency - Avg: {avg_latency:.2f}ms, P95: {p95_latency:.2f}ms")
        
        return {
            "test_name": "edge_latency",
            "success": success,
            "avg_latency_ms": avg_latency,
            "p95_latency_ms": p95_latency,
            "samples": len(latencies)
        }
    
    async def run_all_tests(self) -> List[Dict]:
        """Run all CDN performance tests"""
        logger.info("Starting CDN performance test suite")
        
        results = await asyncio.gather(
            self.test_cache_hit_rate(),
            self.test_edge_latency()
        )
        
        passed = sum(1 for r in results if r.get("success"))
        logger.info(f"\nSummary: {passed}/{len(results)} tests passed\n")
        
        return results


async def main():
    config = CDNPerformanceConfig()
    test_suite = CDNPerformanceTest(config)
    results = await test_suite.run_all_tests()
    exit(0 if all(r.get("success") for r in results) else 1)


if __name__ == "__main__":
    asyncio.run(main())
