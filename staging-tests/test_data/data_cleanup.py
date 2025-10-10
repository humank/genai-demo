"""
Test Data Cleanup and Management

This module provides utilities for cleaning up test data from staging environment,
including database cleanup, S3 cleanup, and resource management.

Features:
- Automated test data cleanup
- Database table truncation
- S3 bucket cleanup
- Resource tagging and identification
- Cleanup scheduling and automation

Requirements: 12.12, 12.13
Implementation: Python using boto3 and psycopg2
"""

import boto3
import psycopg2
import logging
from typing import List, Dict, Optional, Set
from datetime import datetime, timedelta
from contextlib import contextmanager
import os


# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class DatabaseCleanup:
    """
    Database cleanup utilities for test data management.
    
    Provides methods for:
    - Table truncation
    - Test data deletion
    - Foreign key constraint handling
    - Transaction management
    """
    
    def __init__(self, connection_params: Dict[str, str]):
        """
        Initialize database cleanup with connection parameters.
        
        Args:
            connection_params: Database connection parameters
        """
        self.connection_params = connection_params
    
    @contextmanager
    def get_connection(self):
        """Context manager for database connections."""
        conn = psycopg2.connect(**self.connection_params)
        try:
            yield conn
            conn.commit()
        except Exception as e:
            conn.rollback()
            logger.error(f"Database operation failed: {str(e)}")
            raise
        finally:
            conn.close()
    
    def truncate_tables(self, tables: List[str], cascade: bool = True):
        """
        Truncate specified tables.
        
        Args:
            tables: List of table names to truncate
            cascade: Whether to cascade truncate to related tables
        """
        logger.info(f"Truncating tables: {', '.join(tables)}")
        
        with self.get_connection() as conn:
            cursor = conn.cursor()
            
            try:
                # Disable foreign key checks temporarily
                cursor.execute("SET session_replication_role = 'replica';")
                
                for table in tables:
                    cascade_clause = "CASCADE" if cascade else ""
                    cursor.execute(f"TRUNCATE TABLE {table} {cascade_clause};")
                    logger.info(f"✓ Truncated table: {table}")
                
                # Re-enable foreign key checks
                cursor.execute("SET session_replication_role = 'origin';")
                
                logger.info(f"Successfully truncated {len(tables)} tables")
                
            except psycopg2.Error as e:
                logger.error(f"Failed to truncate tables: {str(e)}")
                raise
            finally:
                cursor.close()
    
    def delete_test_data_by_pattern(self, table: str, column: str, pattern: str):
        """
        Delete test data matching a specific pattern.
        
        Args:
            table: Table name
            column: Column to match pattern
            pattern: SQL LIKE pattern
        """
        logger.info(f"Deleting test data from {table} where {column} LIKE '{pattern}'")
        
        with self.get_connection() as conn:
            cursor = conn.cursor()
            
            try:
                # Count records before deletion
                cursor.execute(f"SELECT COUNT(*) FROM {table} WHERE {column} LIKE %s;", (pattern,))
                count_before = cursor.fetchone()[0]
                
                # Delete records
                cursor.execute(f"DELETE FROM {table} WHERE {column} LIKE %s;", (pattern,))
                
                logger.info(f"✓ Deleted {count_before} records from {table}")
                
            except psycopg2.Error as e:
                logger.error(f"Failed to delete test data: {str(e)}")
                raise
            finally:
                cursor.close()
    
    def delete_old_test_data(self, table: str, date_column: str, days_old: int = 7):
        """
        Delete test data older than specified days.
        
        Args:
            table: Table name
            date_column: Date column to check
            days_old: Delete data older than this many days
        """
        logger.info(f"Deleting test data from {table} older than {days_old} days")
        
        cutoff_date = datetime.utcnow() - timedelta(days=days_old)
        
        with self.get_connection() as conn:
            cursor = conn.cursor()
            
            try:
                # Count records before deletion
                cursor.execute(
                    f"SELECT COUNT(*) FROM {table} WHERE {date_column} < %s;",
                    (cutoff_date,)
                )
                count_before = cursor.fetchone()[0]
                
                # Delete old records
                cursor.execute(
                    f"DELETE FROM {table} WHERE {date_column} < %s;",
                    (cutoff_date,)
                )
                
                logger.info(f"✓ Deleted {count_before} old records from {table}")
                
            except psycopg2.Error as e:
                logger.error(f"Failed to delete old test data: {str(e)}")
                raise
            finally:
                cursor.close()
    
    def get_table_row_counts(self, tables: List[str]) -> Dict[str, int]:
        """
        Get row counts for specified tables.
        
        Args:
            tables: List of table names
        
        Returns:
            Dictionary mapping table names to row counts
        """
        logger.info(f"Getting row counts for {len(tables)} tables")
        
        row_counts = {}
        
        with self.get_connection() as conn:
            cursor = conn.cursor()
            
            try:
                for table in tables:
                    cursor.execute(f"SELECT COUNT(*) FROM {table};")
                    count = cursor.fetchone()[0]
                    row_counts[table] = count
                
                logger.info(f"✓ Retrieved row counts for {len(tables)} tables")
                
            except psycopg2.Error as e:
                logger.error(f"Failed to get row counts: {str(e)}")
                raise
            finally:
                cursor.close()
        
        return row_counts


class S3Cleanup:
    """
    S3 cleanup utilities for test data management.
    
    Provides methods for:
    - Test file deletion
    - Bucket cleanup
    - Prefix-based cleanup
    - Old file removal
    """
    
    def __init__(self, region: str = 'ap-northeast-1'):
        """
        Initialize S3 cleanup with AWS region.
        
        Args:
            region: AWS region
        """
        self.s3_client = boto3.client('s3', region_name=region)
        self.region = region
    
    def delete_objects_by_prefix(self, bucket: str, prefix: str):
        """
        Delete all objects with specified prefix.
        
        Args:
            bucket: S3 bucket name
            prefix: Object key prefix
        """
        logger.info(f"Deleting objects from s3://{bucket}/{prefix}")
        
        try:
            # List objects with prefix
            paginator = self.s3_client.get_paginator('list_objects_v2')
            pages = paginator.paginate(Bucket=bucket, Prefix=prefix)
            
            delete_count = 0
            
            for page in pages:
                if 'Contents' not in page:
                    continue
                
                # Prepare delete request
                objects_to_delete = [{'Key': obj['Key']} for obj in page['Contents']]
                
                if objects_to_delete:
                    response = self.s3_client.delete_objects(
                        Bucket=bucket,
                        Delete={'Objects': objects_to_delete}
                    )
                    
                    deleted = len(response.get('Deleted', []))
                    delete_count += deleted
            
            logger.info(f"✓ Deleted {delete_count} objects from s3://{bucket}/{prefix}")
            
        except Exception as e:
            logger.error(f"Failed to delete S3 objects: {str(e)}")
            raise
    
    def delete_old_objects(self, bucket: str, prefix: str, days_old: int = 7):
        """
        Delete objects older than specified days.
        
        Args:
            bucket: S3 bucket name
            prefix: Object key prefix
            days_old: Delete objects older than this many days
        """
        logger.info(f"Deleting objects from s3://{bucket}/{prefix} older than {days_old} days")
        
        cutoff_date = datetime.utcnow() - timedelta(days=days_old)
        
        try:
            # List objects with prefix
            paginator = self.s3_client.get_paginator('list_objects_v2')
            pages = paginator.paginate(Bucket=bucket, Prefix=prefix)
            
            delete_count = 0
            
            for page in pages:
                if 'Contents' not in page:
                    continue
                
                # Filter old objects
                old_objects = [
                    {'Key': obj['Key']}
                    for obj in page['Contents']
                    if obj['LastModified'].replace(tzinfo=None) < cutoff_date
                ]
                
                if old_objects:
                    response = self.s3_client.delete_objects(
                        Bucket=bucket,
                        Delete={'Objects': old_objects}
                    )
                    
                    deleted = len(response.get('Deleted', []))
                    delete_count += deleted
            
            logger.info(f"✓ Deleted {delete_count} old objects from s3://{bucket}/{prefix}")
            
        except Exception as e:
            logger.error(f"Failed to delete old S3 objects: {str(e)}")
            raise
    
    def get_bucket_size(self, bucket: str, prefix: str = '') -> Dict[str, any]:
        """
        Get size and object count for bucket/prefix.
        
        Args:
            bucket: S3 bucket name
            prefix: Object key prefix (optional)
        
        Returns:
            Dictionary with size and count information
        """
        logger.info(f"Getting size for s3://{bucket}/{prefix}")
        
        try:
            paginator = self.s3_client.get_paginator('list_objects_v2')
            pages = paginator.paginate(Bucket=bucket, Prefix=prefix)
            
            total_size = 0
            object_count = 0
            
            for page in pages:
                if 'Contents' not in page:
                    continue
                
                for obj in page['Contents']:
                    total_size += obj['Size']
                    object_count += 1
            
            size_info = {
                'bucket': bucket,
                'prefix': prefix,
                'total_size_bytes': total_size,
                'total_size_mb': round(total_size / (1024 * 1024), 2),
                'object_count': object_count
            }
            
            logger.info(f"✓ Bucket size: {size_info['total_size_mb']} MB, {object_count} objects")
            
            return size_info
            
        except Exception as e:
            logger.error(f"Failed to get bucket size: {str(e)}")
            raise


class TestDataCleanupManager:
    """
    Comprehensive test data cleanup manager.
    
    Coordinates cleanup across multiple services:
    - Database cleanup
    - S3 cleanup
    - Resource tagging
    - Cleanup reporting
    """
    
    def __init__(self, config: Dict[str, any]):
        """
        Initialize cleanup manager with configuration.
        
        Args:
            config: Configuration dictionary
        """
        self.config = config
        self.db_cleanup = DatabaseCleanup(config.get('database', {}))
        self.s3_cleanup = S3Cleanup(config.get('region', 'ap-northeast-1'))
    
    def cleanup_all_test_data(self):
        """Perform comprehensive cleanup of all test data."""
        logger.info("Starting comprehensive test data cleanup")
        
        cleanup_summary = {
            'start_time': datetime.utcnow().isoformat(),
            'database': {},
            's3': {},
            'errors': []
        }
        
        try:
            # Database cleanup
            logger.info("Cleaning up database test data")
            
            test_tables = self.config.get('test_tables', [])
            if test_tables:
                # Get row counts before cleanup
                before_counts = self.db_cleanup.get_table_row_counts(test_tables)
                cleanup_summary['database']['before'] = before_counts
                
                # Truncate test tables
                self.db_cleanup.truncate_tables(test_tables, cascade=True)
                
                # Get row counts after cleanup
                after_counts = self.db_cleanup.get_table_row_counts(test_tables)
                cleanup_summary['database']['after'] = after_counts
            
            # S3 cleanup
            logger.info("Cleaning up S3 test data")
            
            test_buckets = self.config.get('test_buckets', [])
            for bucket_config in test_buckets:
                bucket = bucket_config['bucket']
                prefix = bucket_config.get('prefix', 'test-data/')
                
                # Get size before cleanup
                before_size = self.s3_cleanup.get_bucket_size(bucket, prefix)
                
                # Delete test objects
                self.s3_cleanup.delete_objects_by_prefix(bucket, prefix)
                
                # Get size after cleanup
                after_size = self.s3_cleanup.get_bucket_size(bucket, prefix)
                
                cleanup_summary['s3'][bucket] = {
                    'before': before_size,
                    'after': after_size
                }
            
            cleanup_summary['end_time'] = datetime.utcnow().isoformat()
            cleanup_summary['status'] = 'SUCCESS'
            
            logger.info("✓ Comprehensive test data cleanup completed successfully")
            
        except Exception as e:
            cleanup_summary['status'] = 'FAILED'
            cleanup_summary['errors'].append(str(e))
            logger.error(f"Test data cleanup failed: {str(e)}")
            raise
        
        return cleanup_summary
    
    def cleanup_old_test_data(self, days_old: int = 7):
        """
        Cleanup test data older than specified days.
        
        Args:
            days_old: Delete data older than this many days
        """
        logger.info(f"Cleaning up test data older than {days_old} days")
        
        try:
            # Database cleanup
            test_tables = self.config.get('test_tables', [])
            for table in test_tables:
                self.db_cleanup.delete_old_test_data(table, 'created_at', days_old)
            
            # S3 cleanup
            test_buckets = self.config.get('test_buckets', [])
            for bucket_config in test_buckets:
                bucket = bucket_config['bucket']
                prefix = bucket_config.get('prefix', 'test-data/')
                self.s3_cleanup.delete_old_objects(bucket, prefix, days_old)
            
            logger.info(f"✓ Old test data cleanup completed")
            
        except Exception as e:
            logger.error(f"Old test data cleanup failed: {str(e)}")
            raise


def load_cleanup_config() -> Dict[str, any]:
    """Load cleanup configuration from environment variables."""
    return {
        'region': os.getenv('AWS_REGION', 'ap-northeast-1'),
        'database': {
            'host': os.getenv('DB_HOST', 'localhost'),
            'port': int(os.getenv('DB_PORT', '5432')),
            'database': os.getenv('DB_NAME', 'genai_demo'),
            'user': os.getenv('DB_USER', 'postgres'),
            'password': os.getenv('DB_PASSWORD', '')
        },
        'test_tables': [
            'customers',
            'orders',
            'order_items',
            'products',
            'payments'
        ],
        'test_buckets': [
            {
                'bucket': os.getenv('TEST_BUCKET', 'genai-demo-test-data'),
                'prefix': 'test-data/'
            }
        ]
    }


if __name__ == "__main__":
    # Example usage
    config = load_cleanup_config()
    cleanup_manager = TestDataCleanupManager(config)
    
    # Perform cleanup
    summary = cleanup_manager.cleanup_all_test_data()
    
    print("\nCleanup Summary:")
    print(f"Status: {summary['status']}")
    print(f"Start Time: {summary['start_time']}")
    print(f"End Time: {summary['end_time']}")
    
    if summary['database']:
        print("\nDatabase Cleanup:")
        for table, count in summary['database'].get('before', {}).items():
            print(f"  {table}: {count} rows deleted")
    
    if summary['s3']:
        print("\nS3 Cleanup:")
        for bucket, info in summary['s3'].items():
            before_mb = info['before']['total_size_mb']
            after_mb = info['after']['total_size_mb']
            print(f"  {bucket}: {before_mb} MB -> {after_mb} MB")
