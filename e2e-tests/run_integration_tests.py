#!/usr/bin/env python3
"""
Comprehensive Integration Test Suite Runner

This script runs all integration tests including:
- Database integration tests
- Cache integration tests  
- Messaging integration tests
- Cross-region and disaster recovery tests

Usage:
    python run_integration_tests.py [--test-type TYPE] [--verbose]
    
Test Types:
    all (default) - Run all integration tests
    database      - Run only database integration tests
    cache         - Run only cache integration tests
    messaging     - Run only messaging integration tests
    disaster      - Run only disaster recovery tests
"""

import sys
import os
import argparse
import logging
import time
from pathlib import Path

# Add the staging-tests directory to Python path
sys.path.insert(0, str(Path(__file__).parent))

# Import test suites
from integration.database.test_database_integration import DatabaseIntegrationTestSuite
from integration.cache.test_redis_integration import CacheIntegrationTestSuite
from integration.messaging.test_kafka_integration import MessagingIntegrationTestSuite
from cross-region.test_disaster_recovery import DisasterRecoveryTestSuite

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class IntegrationTestRunner:
    """Main integration test runner"""
    
    def __init__(self, verbose: bool = False):
        self.verbose = verbose
        self.results = {}
        
        if verbose:
            logging.getLogger().setLevel(logging.DEBUG)
    
    def run_database_tests(self) -> dict:
        """Run database integration tests"""
        logger.info("=" * 60)
        logger.info("RUNNING DATABASE INTEGRATION TESTS")
        logger.info("=" * 60)
        
        try:
            test_suite = DatabaseIntegrationTestSuite()
            results = test_suite.run_all_database_tests()
            
            logger.info("Database integration tests completed successfully")
            return {"success": True, "results": results}
            
        except Exception as e:
            logger.error(f"Database integration tests failed: {e}")
            return {"success": False, "error": str(e)}
    
    def run_cache_tests(self) -> dict:
        """Run cache integration tests"""
        logger.info("=" * 60)
        logger.info("RUNNING CACHE INTEGRATION TESTS")
        logger.info("=" * 60)
        
        try:
            test_suite = CacheIntegrationTestSuite()
            results = test_suite.run_all_cache_tests()
            
            logger.info("Cache integration tests completed successfully")
            return {"success": True, "results": results}
            
        except Exception as e:
            logger.error(f"Cache integration tests failed: {e}")
            return {"success": False, "error": str(e)}
    
    def run_messaging_tests(self) -> dict:
        """Run messaging integration tests"""
        logger.info("=" * 60)
        logger.info("RUNNING MESSAGING INTEGRATION TESTS")
        logger.info("=" * 60)
        
        try:
            test_suite = MessagingIntegrationTestSuite()
            results = test_suite.run_all_messaging_tests()
            
            logger.info("Messaging integration tests completed successfully")
            return {"success": True, "results": results}
            
        except Exception as e:
            logger.error(f"Messaging integration tests failed: {e}")
            return {"success": False, "error": str(e)}
    
    def run_disaster_recovery_tests(self) -> dict:
        """Run disaster recovery tests"""
        logger.info("=" * 60)
        logger.info("RUNNING DISASTER RECOVERY TESTS")
        logger.info("=" * 60)
        
        try:
            test_suite = DisasterRecoveryTestSuite()
            results = test_suite.run_all_disaster_recovery_tests()
            
            logger.info("Disaster recovery tests completed successfully")
            return {"success": True, "results": results}
            
        except Exception as e:
            logger.error(f"Disaster recovery tests failed: {e}")
            return {"success": False, "error": str(e)}
    
    def run_all_tests(self) -> dict:
        """Run all integration tests"""
        logger.info("=" * 80)
        logger.info("STARTING COMPREHENSIVE INTEGRATION TEST SUITE")
        logger.info("=" * 80)
        
        start_time = time.time()
        
        # Run all test suites
        self.results["database"] = self.run_database_tests()
        self.results["cache"] = self.run_cache_tests()
        self.results["messaging"] = self.run_messaging_tests()
        self.results["disaster_recovery"] = self.run_disaster_recovery_tests()
        
        # Calculate overall results
        total_time = time.time() - start_time
        successful_suites = sum(1 for result in self.results.values() if result["success"])
        total_suites = len(self.results)
        
        overall_success = successful_suites == total_suites
        
        # Generate summary
        summary = {
            "overall_success": overall_success,
            "successful_suites": successful_suites,
            "total_suites": total_suites,
            "total_execution_time_seconds": total_time,
            "suite_results": self.results
        }
        
        logger.info("=" * 80)
        logger.info("INTEGRATION TEST SUITE SUMMARY")
        logger.info("=" * 80)
        logger.info(f"Overall Success: {overall_success}")
        logger.info(f"Successful Suites: {successful_suites}/{total_suites}")
        logger.info(f"Total Execution Time: {total_time:.2f} seconds")
        
        for suite_name, result in self.results.items():
            status = "✓ PASSED" if result["success"] else "✗ FAILED"
            logger.info(f"  {suite_name.upper()}: {status}")
            
            if not result["success"]:
                logger.error(f"    Error: {result.get('error', 'Unknown error')}")
        
        logger.info("=" * 80)
        
        return summary
    
    def run_specific_test(self, test_type: str) -> dict:
        """Run a specific type of test"""
        if test_type == "database":
            return self.run_database_tests()
        elif test_type == "cache":
            return self.run_cache_tests()
        elif test_type == "messaging":
            return self.run_messaging_tests()
        elif test_type == "disaster":
            return self.run_disaster_recovery_tests()
        elif test_type == "all":
            return self.run_all_tests()
        else:
            raise ValueError(f"Unknown test type: {test_type}")


def main():
    """Main entry point"""
    parser = argparse.ArgumentParser(
        description="Run comprehensive integration test suite",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python run_integration_tests.py                    # Run all tests
  python run_integration_tests.py --test-type database  # Run only database tests
  python run_integration_tests.py --verbose          # Run with verbose logging
        """
    )
    
    parser.add_argument(
        "--test-type",
        choices=["all", "database", "cache", "messaging", "disaster"],
        default="all",
        help="Type of tests to run (default: all)"
    )
    
    parser.add_argument(
        "--verbose",
        action="store_true",
        help="Enable verbose logging"
    )
    
    args = parser.parse_args()
    
    # Create test runner
    runner = IntegrationTestRunner(verbose=args.verbose)
    
    try:
        # Run tests
        results = runner.run_specific_test(args.test_type)
        
        # Exit with appropriate code
        if results.get("overall_success", results.get("success", False)):
            logger.info("All tests completed successfully!")
            sys.exit(0)
        else:
            logger.error("Some tests failed!")
            sys.exit(1)
            
    except Exception as e:
        logger.error(f"Test execution failed: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()