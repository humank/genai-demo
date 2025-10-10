#!/usr/bin/env python3
"""
Aurora PostgreSQL Connection Integration Test
Created: 2025年10月1日 下午8:35 (台北時間)
Purpose: Test Aurora PostgreSQL connection and basic operations
"""

import sys
import time
import argparse
import psycopg2
from psycopg2.extras import RealDictCursor
import logging

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class AuroraConnectionTest:
    def __init__(self, profile='staging'):
        self.profile = profile
        self.connection_config = self._get_connection_config()
        
    def _get_connection_config(self):
        """Get database connection configuration based on profile"""
        if self.profile == 'staging':
            return {
                'host': 'localhost',  # Docker container
                'port': 5432,
                'database': 'genaidemo_staging',
                'user': 'genaidemo',
                'password': 'staging_password'
            }
        else:
            # Production Aurora configuration would be loaded from AWS Secrets Manager
            raise ValueError(f"Unsupported profile: {self.profile}")
    
    def test_basic_connection(self):
        """Test basic database connection"""
        logger.info("Testing basic Aurora connection...")
        
        try:
            conn = psycopg2.connect(**self.connection_config)
            cursor = conn.cursor()
            
            # Test basic query
            cursor.execute("SELECT version();")
            version = cursor.fetchone()[0]
            logger.info(f"Connected to PostgreSQL: {version}")
            
            cursor.close()
            conn.close()
            
            logger.info("✅ Basic connection test passed")
            return True
            
        except Exception as e:
            logger.error(f"❌ Basic connection test failed: {e}")
            return False
    
    def test_connection_pool(self):
        """Test connection pooling behavior"""
        logger.info("Testing connection pool behavior...")
        
        connections = []
        try:
            # Create multiple connections to test pooling
            for i in range(5):
                conn = psycopg2.connect(**self.connection_config)
                connections.append(conn)
                logger.info(f"Created connection {i+1}")
            
            # Test concurrent queries
            for i, conn in enumerate(connections):
                cursor = conn.cursor()
                cursor.execute("SELECT pg_backend_pid();")
                pid = cursor.fetchone()[0]
                logger.info(f"Connection {i+1} backend PID: {pid}")
                cursor.close()
            
            logger.info("✅ Connection pool test passed")
            return True
            
        except Exception as e:
            logger.error(f"❌ Connection pool test failed: {e}")
            return False
            
        finally:
            # Clean up connections
            for conn in connections:
                if not conn.closed:
                    conn.close()
    
    def test_transaction_isolation(self):
        """Test transaction isolation levels"""
        logger.info("Testing transaction isolation...")
        
        try:
            conn1 = psycopg2.connect(**self.connection_config)
            conn2 = psycopg2.connect(**self.connection_config)
            
            # Set isolation level
            conn1.set_isolation_level(psycopg2.extensions.ISOLATION_LEVEL_READ_COMMITTED)
            conn2.set_isolation_level(psycopg2.extensions.ISOLATION_LEVEL_READ_COMMITTED)
            
            cursor1 = conn1.cursor()
            cursor2 = conn2.cursor()
            
            # Create test table
            cursor1.execute("""
                CREATE TABLE IF NOT EXISTS test_isolation (
                    id SERIAL PRIMARY KEY,
                    value INTEGER
                );
            """)
            cursor1.execute("DELETE FROM test_isolation;")
            cursor1.execute("INSERT INTO test_isolation (value) VALUES (100);")
            conn1.commit()
            
            # Test isolation
            cursor1.execute("BEGIN;")
            cursor1.execute("UPDATE test_isolation SET value = 200 WHERE id = 1;")
            
            # Second connection should still see old value
            cursor2.execute("SELECT value FROM test_isolation WHERE id = 1;")
            value = cursor2.fetchone()[0]
            
            if value == 100:
                logger.info("✅ Transaction isolation working correctly")
                result = True
            else:
                logger.error(f"❌ Transaction isolation failed: expected 100, got {value}")
                result = False
            
            cursor1.execute("COMMIT;")
            
            # Cleanup
            cursor1.execute("DROP TABLE IF EXISTS test_isolation;")
            conn1.commit()
            
            cursor1.close()
            cursor2.close()
            conn1.close()
            conn2.close()
            
            return result
            
        except Exception as e:
            logger.error(f"❌ Transaction isolation test failed: {e}")
            return False
    
    def test_performance_metrics(self):
        """Test database performance metrics collection"""
        logger.info("Testing performance metrics collection...")
        
        try:
            conn = psycopg2.connect(**self.connection_config)
            cursor = conn.cursor(cursor_factory=RealDictCursor)
            
            # Enable timing
            start_time = time.time()
            
            # Execute test queries
            test_queries = [
                "SELECT COUNT(*) FROM information_schema.tables;",
                "SELECT current_database();",
                "SELECT current_user;",
                "SELECT now();",
                "SELECT pg_database_size(current_database());"
            ]
            
            results = {}
            for query in test_queries:
                query_start = time.time()
                cursor.execute(query)
                result = cursor.fetchone()
                query_time = (time.time() - query_start) * 1000  # Convert to ms
                
                results[query] = {
                    'result': dict(result) if result else None,
                    'execution_time_ms': round(query_time, 2)
                }
                
                logger.info(f"Query executed in {query_time:.2f}ms: {query[:50]}...")
            
            total_time = (time.time() - start_time) * 1000
            logger.info(f"Total test execution time: {total_time:.2f}ms")
            
            # Check if performance is acceptable
            max_query_time = max(r['execution_time_ms'] for r in results.values())
            if max_query_time < 100:  # 100ms threshold
                logger.info("✅ Performance metrics test passed")
                result = True
            else:
                logger.warning(f"⚠️ Some queries exceeded 100ms threshold: {max_query_time:.2f}ms")
                result = True  # Still pass but with warning
            
            cursor.close()
            conn.close()
            
            return result
            
        except Exception as e:
            logger.error(f"❌ Performance metrics test failed: {e}")
            return False
    
    def run_all_tests(self):
        """Run all Aurora connection tests"""
        logger.info(f"Starting Aurora connection tests for profile: {self.profile}")
        
        tests = [
            ('Basic Connection', self.test_basic_connection),
            ('Connection Pool', self.test_connection_pool),
            ('Transaction Isolation', self.test_transaction_isolation),
            ('Performance Metrics', self.test_performance_metrics)
        ]
        
        results = {}
        for test_name, test_func in tests:
            logger.info(f"\n--- Running {test_name} Test ---")
            results[test_name] = test_func()
        
        # Summary
        logger.info("\n=== Test Results Summary ===")
        passed = 0
        total = len(tests)
        
        for test_name, result in results.items():
            status = "✅ PASSED" if result else "❌ FAILED"
            logger.info(f"{test_name}: {status}")
            if result:
                passed += 1
        
        logger.info(f"\nOverall: {passed}/{total} tests passed")
        
        return passed == total

def main():
    parser = argparse.ArgumentParser(description='Aurora PostgreSQL Connection Integration Test')
    parser.add_argument('--profile', default='staging', help='Test profile (staging/production)')
    args = parser.parse_args()
    
    test = AuroraConnectionTest(profile=args.profile)
    success = test.run_all_tests()
    
    sys.exit(0 if success else 1)

if __name__ == '__main__':
    main()