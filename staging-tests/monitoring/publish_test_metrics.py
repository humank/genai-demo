"""
Test Metrics Publisher

This module publishes test execution metrics to CloudWatch for monitoring and alerting.

Features:
- Test execution metrics publishing
- Test failure rate tracking
- Performance metrics collection
- Custom CloudWatch metrics
- Integration with test framework

Requirements: 12.26, 12.27
Implementation: Python boto3 + CloudWatch
"""

import boto3
import json
import logging
from datetime import datetime
from typing import Dict, List, Optional
from dataclasses import dataclass, asdict
from enum import Enum


# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class TestStatus(Enum):
    """Test execution status."""
    PASSED = "passed"
    FAILED = "failed"
    SKIPPED = "skipped"
    ERROR = "error"


@dataclass
class TestMetrics:
    """Test execution metrics data class."""
    test_suite: str
    test_name: str
    status: TestStatus
    duration_seconds: float
    timestamp: datetime
    error_message: Optional[str] = None
    test_type: str = "integration"  # integration, security, performance, dr
    region: str = "ap-northeast-1"
    environment: str = "staging"


class CloudWatchMetricsPublisher:
    """
    Publisher for test metrics to CloudWatch.
    
    Publishes metrics including:
    - Test execution count
    - Test failure rate
    - Test duration
    - Test success rate
    """
    
    def __init__(self, region: str = 'ap-northeast-1', namespace: str = 'StagingTests'):
        """
        Initialize CloudWatch metrics publisher.
        
        Args:
            region: AWS region
            namespace: CloudWatch namespace for metrics
        """
        self.cloudwatch = boto3.client('cloudwatch', region_name=region)
        self.namespace = namespace
        self.region = region
    
    def publish_test_metrics(self, metrics: TestMetrics):
        """
        Publish test execution metrics to CloudWatch.
        
        Args:
            metrics: Test metrics to publish
        """
        try:
            metric_data = [
                # Test execution count
                {
                    'MetricName': 'TestExecutionCount',
                    'Dimensions': [
                        {'Name': 'TestSuite', 'Value': metrics.test_suite},
                        {'Name': 'TestType', 'Value': metrics.test_type},
                        {'Name': 'Environment', 'Value': metrics.environment}
                    ],
                    'Value': 1,
                    'Unit': 'Count',
                    'Timestamp': metrics.timestamp
                },
                # Test duration
                {
                    'MetricName': 'TestDuration',
                    'Dimensions': [
                        {'Name': 'TestSuite', 'Value': metrics.test_suite},
                        {'Name': 'TestType', 'Value': metrics.test_type},
                        {'Name': 'Environment', 'Value': metrics.environment}
                    ],
                    'Value': metrics.duration_seconds,
                    'Unit': 'Seconds',
                    'Timestamp': metrics.timestamp
                }
            ]
            
            # Add status-specific metrics
            if metrics.status == TestStatus.FAILED:
                metric_data.append({
                    'MetricName': 'TestFailureCount',
                    'Dimensions': [
                        {'Name': 'TestSuite', 'Value': metrics.test_suite},
                        {'Name': 'TestType', 'Value': metrics.test_type},
                        {'Name': 'Environment', 'Value': metrics.environment}
                    ],
                    'Value': 1,
                    'Unit': 'Count',
                    'Timestamp': metrics.timestamp
                })
            elif metrics.status == TestStatus.PASSED:
                metric_data.append({
                    'MetricName': 'TestSuccessCount',
                    'Dimensions': [
                        {'Name': 'TestSuite', 'Value': metrics.test_suite},
                        {'Name': 'TestType', 'Value': metrics.test_type},
                        {'Name': 'Environment', 'Value': metrics.environment}
                    ],
                    'Value': 1,
                    'Unit': 'Count',
                    'Timestamp': metrics.timestamp
                })
            
            # Publish metrics
            response = self.cloudwatch.put_metric_data(
                Namespace=self.namespace,
                MetricData=metric_data
            )
            
            logger.info(f"✓ Published metrics for test: {metrics.test_name}")
            
        except Exception as e:
            logger.error(f"Failed to publish metrics: {str(e)}")
            raise
    
    def publish_test_suite_summary(self, test_suite: str, summary: Dict):
        """
        Publish test suite summary metrics.
        
        Args:
            test_suite: Test suite name
            summary: Summary statistics
        """
        try:
            metric_data = [
                {
                    'MetricName': 'TestSuiteSuccessRate',
                    'Dimensions': [
                        {'Name': 'TestSuite', 'Value': test_suite},
                        {'Name': 'Environment', 'Value': summary.get('environment', 'staging')}
                    ],
                    'Value': summary.get('success_rate', 0),
                    'Unit': 'Percent',
                    'Timestamp': datetime.utcnow()
                },
                {
                    'MetricName': 'TestSuiteTotalDuration',
                    'Dimensions': [
                        {'Name': 'TestSuite', 'Value': test_suite},
                        {'Name': 'Environment', 'Value': summary.get('environment', 'staging')}
                    ],
                    'Value': summary.get('total_duration', 0),
                    'Unit': 'Seconds',
                    'Timestamp': datetime.utcnow()
                },
                {
                    'MetricName': 'TestSuiteTotalTests',
                    'Dimensions': [
                        {'Name': 'TestSuite', 'Value': test_suite},
                        {'Name': 'Environment', 'Value': summary.get('environment', 'staging')}
                    ],
                    'Value': summary.get('total_tests', 0),
                    'Unit': 'Count',
                    'Timestamp': datetime.utcnow()
                }
            ]
            
            response = self.cloudwatch.put_metric_data(
                Namespace=self.namespace,
                MetricData=metric_data
            )
            
            logger.info(f"✓ Published summary metrics for test suite: {test_suite}")
            
        except Exception as e:
            logger.error(f"Failed to publish summary metrics: {str(e)}")
            raise


class TestAlertManager:
    """
    Manager for test-related CloudWatch alarms.
    
    Creates and manages alarms for:
    - High test failure rate
    - Long test duration
    - Test execution failures
    """
    
    def __init__(self, region: str = 'ap-northeast-1'):
        """
        Initialize alert manager.
        
        Args:
            region: AWS region
        """
        self.cloudwatch = boto3.client('cloudwatch', region_name=region)
        self.sns = boto3.client('sns', region_name=region)
        self.region = region
    
    def create_test_failure_alarm(
        self,
        test_suite: str,
        sns_topic_arn: str,
        threshold: float = 20.0,
        evaluation_periods: int = 2
    ):
        """
        Create alarm for high test failure rate.
        
        Args:
            test_suite: Test suite name
            sns_topic_arn: SNS topic ARN for notifications
            threshold: Failure rate threshold (percentage)
            evaluation_periods: Number of periods to evaluate
        """
        try:
            alarm_name = f"StagingTests-{test_suite}-HighFailureRate"
            
            self.cloudwatch.put_metric_alarm(
                AlarmName=alarm_name,
                ComparisonOperator='GreaterThanThreshold',
                EvaluationPeriods=evaluation_periods,
                MetricName='TestFailureCount',
                Namespace='StagingTests',
                Period=300,  # 5 minutes
                Statistic='Sum',
                Threshold=threshold,
                ActionsEnabled=True,
                AlarmActions=[sns_topic_arn],
                AlarmDescription=f'Alert when {test_suite} test failure rate exceeds {threshold}%',
                Dimensions=[
                    {'Name': 'TestSuite', 'Value': test_suite},
                    {'Name': 'Environment', 'Value': 'staging'}
                ],
                TreatMissingData='notBreaching'
            )
            
            logger.info(f"✓ Created alarm: {alarm_name}")
            
        except Exception as e:
            logger.error(f"Failed to create alarm: {str(e)}")
            raise
    
    def create_test_duration_alarm(
        self,
        test_suite: str,
        sns_topic_arn: str,
        threshold_seconds: float = 600.0,
        evaluation_periods: int = 1
    ):
        """
        Create alarm for long test duration.
        
        Args:
            test_suite: Test suite name
            sns_topic_arn: SNS topic ARN for notifications
            threshold_seconds: Duration threshold in seconds
            evaluation_periods: Number of periods to evaluate
        """
        try:
            alarm_name = f"StagingTests-{test_suite}-LongDuration"
            
            self.cloudwatch.put_metric_alarm(
                AlarmName=alarm_name,
                ComparisonOperator='GreaterThanThreshold',
                EvaluationPeriods=evaluation_periods,
                MetricName='TestSuiteTotalDuration',
                Namespace='StagingTests',
                Period=300,  # 5 minutes
                Statistic='Average',
                Threshold=threshold_seconds,
                ActionsEnabled=True,
                AlarmActions=[sns_topic_arn],
                AlarmDescription=f'Alert when {test_suite} duration exceeds {threshold_seconds}s',
                Dimensions=[
                    {'Name': 'TestSuite', 'Value': test_suite},
                    {'Name': 'Environment', 'Value': 'staging'}
                ],
                TreatMissingData='notBreaching'
            )
            
            logger.info(f"✓ Created alarm: {alarm_name}")
            
        except Exception as e:
            logger.error(f"Failed to create alarm: {str(e)}")
            raise
    
    def setup_all_alarms(self, sns_topic_arn: str):
        """
        Set up all test monitoring alarms.
        
        Args:
            sns_topic_arn: SNS topic ARN for notifications
        """
        test_suites = ['integration', 'security', 'performance', 'disaster-recovery']
        
        for suite in test_suites:
            # Create failure rate alarm
            self.create_test_failure_alarm(suite, sns_topic_arn)
            
            # Create duration alarm
            duration_thresholds = {
                'integration': 600,  # 10 minutes
                'security': 900,     # 15 minutes
                'performance': 1800, # 30 minutes
                'disaster-recovery': 1200  # 20 minutes
            }
            self.create_test_duration_alarm(
                suite,
                sns_topic_arn,
                threshold_seconds=duration_thresholds[suite]
            )
        
        logger.info("✓ All test monitoring alarms created")


class TestDashboardCreator:
    """
    Creator for CloudWatch dashboards for test monitoring.
    
    Creates dashboards showing:
    - Test execution trends
    - Failure rates
    - Duration metrics
    - Success rates
    """
    
    def __init__(self, region: str = 'ap-northeast-1'):
        """
        Initialize dashboard creator.
        
        Args:
            region: AWS region
        """
        self.cloudwatch = boto3.client('cloudwatch', region_name=region)
        self.region = region
    
    def create_test_monitoring_dashboard(self, dashboard_name: str = 'StagingTestMonitoring'):
        """
        Create comprehensive test monitoring dashboard.
        
        Args:
            dashboard_name: Dashboard name
        """
        try:
            dashboard_body = {
                "widgets": [
                    # Test Execution Count
                    {
                        "type": "metric",
                        "properties": {
                            "metrics": [
                                ["StagingTests", "TestExecutionCount", {"stat": "Sum"}]
                            ],
                            "period": 300,
                            "stat": "Sum",
                            "region": self.region,
                            "title": "Test Execution Count",
                            "yAxis": {"left": {"min": 0}}
                        }
                    },
                    # Test Success Rate
                    {
                        "type": "metric",
                        "properties": {
                            "metrics": [
                                ["StagingTests", "TestSuiteSuccessRate", {"stat": "Average"}]
                            ],
                            "period": 300,
                            "stat": "Average",
                            "region": self.region,
                            "title": "Test Success Rate (%)",
                            "yAxis": {"left": {"min": 0, "max": 100}}
                        }
                    },
                    # Test Failure Count
                    {
                        "type": "metric",
                        "properties": {
                            "metrics": [
                                ["StagingTests", "TestFailureCount", {"stat": "Sum"}]
                            ],
                            "period": 300,
                            "stat": "Sum",
                            "region": self.region,
                            "title": "Test Failure Count",
                            "yAxis": {"left": {"min": 0}}
                        }
                    },
                    # Test Duration
                    {
                        "type": "metric",
                        "properties": {
                            "metrics": [
                                ["StagingTests", "TestSuiteTotalDuration", {"stat": "Average"}]
                            ],
                            "period": 300,
                            "stat": "Average",
                            "region": self.region,
                            "title": "Test Suite Duration (seconds)",
                            "yAxis": {"left": {"min": 0}}
                        }
                    }
                ]
            }
            
            self.cloudwatch.put_dashboard(
                DashboardName=dashboard_name,
                DashboardBody=json.dumps(dashboard_body)
            )
            
            logger.info(f"✓ Created dashboard: {dashboard_name}")
            
        except Exception as e:
            logger.error(f"Failed to create dashboard: {str(e)}")
            raise


# Pytest plugin for automatic metrics publishing
class PytestMetricsPlugin:
    """Pytest plugin for automatic test metrics publishing."""
    
    def __init__(self):
        self.publisher = CloudWatchMetricsPublisher()
        self.test_results = []
    
    def pytest_runtest_makereport(self, item, call):
        """Hook called when test report is created."""
        if call.when == 'call':
            test_metrics = TestMetrics(
                test_suite=item.module.__name__,
                test_name=item.name,
                status=TestStatus.PASSED if call.excinfo is None else TestStatus.FAILED,
                duration_seconds=call.duration,
                timestamp=datetime.utcnow(),
                error_message=str(call.excinfo) if call.excinfo else None
            )
            
            self.test_results.append(test_metrics)
            self.publisher.publish_test_metrics(test_metrics)
    
    def pytest_sessionfinish(self, session):
        """Hook called when test session finishes."""
        # Publish summary metrics
        total_tests = len(self.test_results)
        passed_tests = sum(1 for t in self.test_results if t.status == TestStatus.PASSED)
        success_rate = (passed_tests / total_tests * 100) if total_tests > 0 else 0
        total_duration = sum(t.duration_seconds for t in self.test_results)
        
        summary = {
            'total_tests': total_tests,
            'success_rate': success_rate,
            'total_duration': total_duration,
            'environment': 'staging'
        }
        
        self.publisher.publish_test_suite_summary('all', summary)


if __name__ == "__main__":
    # Example usage
    import argparse
    
    parser = argparse.ArgumentParser(description='Test Metrics Publisher')
    parser.add_argument('--setup-alarms', action='store_true', help='Setup CloudWatch alarms')
    parser.add_argument('--create-dashboard', action='store_true', help='Create CloudWatch dashboard')
    parser.add_argument('--sns-topic-arn', type=str, help='SNS topic ARN for alarms')
    
    args = parser.parse_args()
    
    if args.setup_alarms:
        if not args.sns_topic_arn:
            print("Error: --sns-topic-arn required for alarm setup")
            exit(1)
        
        alert_manager = TestAlertManager()
        alert_manager.setup_all_alarms(args.sns_topic_arn)
        print("✓ Alarms setup complete")
    
    if args.create_dashboard:
        dashboard_creator = TestDashboardCreator()
        dashboard_creator.create_test_monitoring_dashboard()
        print("✓ Dashboard created")
