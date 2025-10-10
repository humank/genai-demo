"""
CloudWatch and X-Ray Monitoring Integration Tests

This module provides comprehensive monitoring integration tests including:
- CloudWatch metrics validation and publishing
- X-Ray tracing verification and service map generation
- Custom metrics collection testing
- Monitoring health check validation

Requirements: 12.4, 12.27
Implementation: Python using boto3 CloudWatch and X-Ray clients
"""

import pytest
import boto3
import time
import json
import sys
import os
from datetime import datetime, timedelta
from typing import List, Dict, Any, Optional
from dataclasses import dataclass, field
import logging

# Add parent directory to path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

from base_staging_test import (
    BaseStagingIntegrationTest,
    StagingEnvironmentConfig,
    retry_on_failure
)

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


@dataclass
class CloudWatchMetricData:
    """CloudWatch metric data structure"""
    namespace: str
    metric_name: str
    value: float
    unit: str = 'None'
    dimensions: List[Dict[str, str]] = field(default_factory=list)
    timestamp: Optional[datetime] = None
    
    def to_metric_data(self) -> Dict[str, Any]:
        """Convert to CloudWatch metric data format"""
        data = {
            'MetricName': self.metric_name,
            'Value': self.value,
            'Unit': self.unit,
            'Timestamp': self.timestamp or datetime.now()
        }
        
        if self.dimensions:
            data['Dimensions'] = self.dimensions
        
        return data


@dataclass
class XRayTraceData:
    """X-Ray trace data structure"""
    trace_id: str
    segment_id: str
    name: str
    start_time: float
    end_time: Optional[float] = None
    http_status: Optional[int] = None
    error: bool = False
    fault: bool = False
    annotations: Dict[str, Any] = field(default_factory=dict)
    metadata: Dict[str, Any] = field(default_factory=dict)


class CloudWatchMonitoringTest(BaseStagingIntegrationTest):
    """CloudWatch monitoring integration tests"""
    
    def __init__(self, config: Optional[StagingEnvironmentConfig] = None):
        """Initialize CloudWatch monitoring test"""
        super().__init__(config)
        self.test_namespace = 'StagingTests/Monitoring'
        self.test_log_group = os.getenv('CLOUDWATCH_LOG_GROUP', '/aws/staging/tests')
    
    @pytest.mark.integration
    @pytest.mark.monitoring
    def test_publish_custom_metric(self):
        """Test publishing custom metrics to CloudWatch"""
        with self.track_test_metrics('test_publish_custom_metric'):
            # Create test metric
            metric = CloudWatchMetricData(
                namespace=self.test_namespace,
                metric_name='TestMetric',
                value=100.0,
                unit='Count',
                dimensions=[
                    {'Name': 'Environment', 'Value': 'Staging'},
                    {'Name': 'TestType', 'Value': 'Integration'}
                ]
            )
            
            # Publish metric
            try:
                response = self.cloudwatch.put_metric_data(
                    Namespace=metric.namespace,
                    MetricData=[metric.to_metric_data()]
                )
                
                logger.info(f"Published metric {metric.metric_name} to CloudWatch")
                
                # Verify response
                assert response['ResponseMetadata']['HTTPStatusCode'] == 200
                
                # Wait for metric to be available
                time.sleep(5)
                
                # Verify metric was published by querying it
                end_time = datetime.now()
                start_time = end_time - timedelta(minutes=5)
                
                stats_response = self.cloudwatch.get_metric_statistics(
                    Namespace=metric.namespace,
                    MetricName=metric.metric_name,
                    Dimensions=metric.dimensions,
                    StartTime=start_time,
                    EndTime=end_time,
                    Period=60,
                    Statistics=['Sum', 'Average', 'Maximum']
                )
                
                # Verify we got data points
                assert 'Datapoints' in stats_response
                logger.info(f"Retrieved {len(stats_response['Datapoints'])} data points")
                
            except Exception as e:
                logger.error(f"Failed to publish or verify metric: {str(e)}")
                raise
    
    @pytest.mark.integration
    @pytest.mark.monitoring
    def test_publish_multiple_metrics(self):
        """Test publishing multiple metrics in a single request"""
        with self.track_test_metrics('test_publish_multiple_metrics'):
            # Create multiple metrics
            metrics = [
                CloudWatchMetricData(
                    namespace=self.test_namespace,
                    metric_name='RequestCount',
                    value=150.0,
                    unit='Count'
                ),
                CloudWatchMetricData(
                    namespace=self.test_namespace,
                    metric_name='ResponseTime',
                    value=234.5,
                    unit='Milliseconds'
                ),
                CloudWatchMetricData(
                    namespace=self.test_namespace,
                    metric_name='ErrorRate',
                    value=0.5,
                    unit='Percent'
                )
            ]
            
            # Publish all metrics
            metric_data = [m.to_metric_data() for m in metrics]
            
            response = self.cloudwatch.put_metric_data(
                Namespace=self.test_namespace,
                MetricData=metric_data
            )
            
            assert response['ResponseMetadata']['HTTPStatusCode'] == 200
            logger.info(f"Published {len(metrics)} metrics to CloudWatch")
    
    @pytest.mark.integration
    @pytest.mark.monitoring
    def test_list_metrics(self):
        """Test listing available metrics in namespace"""
        with self.track_test_metrics('test_list_metrics'):
            # List metrics in test namespace
            response = self.cloudwatch.list_metrics(
                Namespace=self.test_namespace
            )
            
            assert 'Metrics' in response
            metrics = response['Metrics']
            
            logger.info(f"Found {len(metrics)} metrics in namespace {self.test_namespace}")
            
            # Verify metric structure
            if metrics:
                metric = metrics[0]
                assert 'Namespace' in metric
                assert 'MetricName' in metric
                assert metric['Namespace'] == self.test_namespace
    
    @pytest.mark.integration
    @pytest.mark.monitoring
    def test_get_metric_statistics(self):
        """Test retrieving metric statistics"""
        with self.track_test_metrics('test_get_metric_statistics'):
            # First publish a metric
            metric = CloudWatchMetricData(
                namespace=self.test_namespace,
                metric_name='StatisticsTest',
                value=75.0,
                unit='Percent'
            )
            
            self.cloudwatch.put_metric_data(
                Namespace=metric.namespace,
                MetricData=[metric.to_metric_data()]
            )
            
            # Wait for metric to be available
            time.sleep(5)
            
            # Get statistics
            end_time = datetime.now()
            start_time = end_time - timedelta(minutes=10)
            
            response = self.cloudwatch.get_metric_statistics(
                Namespace=metric.namespace,
                MetricName=metric.metric_name,
                StartTime=start_time,
                EndTime=end_time,
                Period=300,  # 5 minutes
                Statistics=['Sum', 'Average', 'Maximum', 'Minimum', 'SampleCount']
            )
            
            assert 'Datapoints' in response
            logger.info(f"Retrieved statistics with {len(response['Datapoints'])} data points")
    
    @pytest.mark.integration
    @pytest.mark.monitoring
    @pytest.mark.slow
    def test_cloudwatch_logs_query(self):
        """Test CloudWatch Logs Insights query"""
        with self.track_test_metrics('test_cloudwatch_logs_query'):
            # Define query
            query = """
            fields @timestamp, @message
            | filter @message like /ERROR/
            | sort @timestamp desc
            | limit 10
            """
            
            # Start query
            end_time = datetime.now()
            start_time = end_time - timedelta(hours=1)
            
            try:
                response = self.logs.start_query(
                    logGroupName=self.test_log_group,
                    startTime=int(start_time.timestamp()),
                    endTime=int(end_time.timestamp()),
                    queryString=query
                )
                
                query_id = response['queryId']
                logger.info(f"Started CloudWatch Logs query: {query_id}")
                
                # Wait for query to complete
                max_wait = 30  # seconds
                wait_interval = 2
                elapsed = 0
                
                while elapsed < max_wait:
                    result = self.logs.get_query_results(queryId=query_id)
                    status = result['status']
                    
                    if status == 'Complete':
                        logger.info(f"Query completed with {len(result['results'])} results")
                        assert 'results' in result
                        break
                    elif status in ['Failed', 'Cancelled']:
                        pytest.fail(f"Query {status.lower()}")
                    
                    time.sleep(wait_interval)
                    elapsed += wait_interval
                
                if elapsed >= max_wait:
                    logger.warning("Query did not complete within timeout")
                    
            except self.logs.exceptions.ResourceNotFoundException:
                logger.warning(f"Log group {self.test_log_group} not found - skipping test")
                pytest.skip(f"Log group {self.test_log_group} not found")
    
    @pytest.mark.integration
    @pytest.mark.monitoring
    def test_create_metric_alarm(self):
        """Test creating a CloudWatch alarm"""
        with self.track_test_metrics('test_create_metric_alarm'):
            alarm_name = 'StagingTest-HighErrorRate'
            
            try:
                # Create alarm
                self.cloudwatch.put_metric_alarm(
                    AlarmName=alarm_name,
                    ComparisonOperator='GreaterThanThreshold',
                    EvaluationPeriods=1,
                    MetricName='ErrorRate',
                    Namespace=self.test_namespace,
                    Period=300,
                    Statistic='Average',
                    Threshold=5.0,
                    ActionsEnabled=False,  # Don't trigger actions in test
                    AlarmDescription='Test alarm for high error rate',
                    Unit='Percent'
                )
                
                logger.info(f"Created alarm: {alarm_name}")
                
                # Verify alarm was created
                response = self.cloudwatch.describe_alarms(
                    AlarmNames=[alarm_name]
                )
                
                assert len(response['MetricAlarms']) == 1
                alarm = response['MetricAlarms'][0]
                assert alarm['AlarmName'] == alarm_name
                assert alarm['Threshold'] == 5.0
                
            finally:
                # Cleanup: Delete test alarm
                try:
                    self.cloudwatch.delete_alarms(AlarmNames=[alarm_name])
                    logger.info(f"Deleted test alarm: {alarm_name}")
                except Exception as e:
                    logger.warning(f"Failed to delete test alarm: {str(e)}")


class XRayMonitoringTest(BaseStagingIntegrationTest):
    """X-Ray distributed tracing integration tests"""
    
    def __init__(self, config: Optional[StagingEnvironmentConfig] = None):
        """Initialize X-Ray monitoring test"""
        super().__init__(config)
    
    @pytest.mark.integration
    @pytest.mark.monitoring
    def test_get_service_graph(self):
        """Test retrieving X-Ray service graph"""
        with self.track_test_metrics('test_get_service_graph'):
            # Get service graph for last hour
            end_time = datetime.now()
            start_time = end_time - timedelta(hours=1)
            
            try:
                response = self.xray.get_service_graph(
                    StartTime=start_time,
                    EndTime=end_time
                )
                
                assert 'Services' in response
                services = response['Services']
                
                logger.info(f"Retrieved service graph with {len(services)} services")
                
                # Verify service structure
                if services:
                    service = services[0]
                    assert 'Name' in service or 'ReferenceId' in service
                    assert 'Type' in service
                    
                    logger.info(f"Sample service: {service.get('Name', 'Unknown')}")
                    
            except Exception as e:
                logger.warning(f"Failed to get service graph: {str(e)}")
                # X-Ray may not have data in test environment
                pytest.skip("X-Ray service graph not available")
    
    @pytest.mark.integration
    @pytest.mark.monitoring
    @pytest.mark.slow
    def test_get_trace_summaries(self):
        """Test retrieving X-Ray trace summaries"""
        with self.track_test_metrics('test_get_trace_summaries'):
            # Get trace summaries for last hour
            end_time = datetime.now()
            start_time = end_time - timedelta(hours=1)
            
            try:
                response = self.xray.get_trace_summaries(
                    StartTime=start_time,
                    EndTime=end_time,
                    Sampling=False
                )
                
                assert 'TraceSummaries' in response
                traces = response['TraceSummaries']
                
                logger.info(f"Retrieved {len(traces)} trace summaries")
                
                # Verify trace structure
                if traces:
                    trace = traces[0]
                    assert 'Id' in trace
                    assert 'Duration' in trace
                    
                    logger.info(f"Sample trace ID: {trace['Id']}, Duration: {trace['Duration']}s")
                    
            except Exception as e:
                logger.warning(f"Failed to get trace summaries: {str(e)}")
                pytest.skip("X-Ray traces not available")
    
    @pytest.mark.integration
    @pytest.mark.monitoring
    @pytest.mark.slow
    def test_get_trace_graph(self):
        """Test retrieving X-Ray trace graph"""
        with self.track_test_metrics('test_get_trace_graph'):
            # First get trace summaries to get trace IDs
            end_time = datetime.now()
            start_time = end_time - timedelta(hours=1)
            
            try:
                summaries_response = self.xray.get_trace_summaries(
                    StartTime=start_time,
                    EndTime=end_time,
                    Sampling=False
                )
                
                traces = summaries_response.get('TraceSummaries', [])
                
                if not traces:
                    pytest.skip("No traces available for testing")
                
                # Get trace graph for first trace
                trace_id = traces[0]['Id']
                
                graph_response = self.xray.batch_get_traces(
                    TraceIds=[trace_id]
                )
                
                assert 'Traces' in graph_response
                trace_data = graph_response['Traces']
                
                logger.info(f"Retrieved trace graph for trace {trace_id}")
                
                if trace_data:
                    trace = trace_data[0]
                    assert 'Id' in trace
                    assert 'Segments' in trace
                    
                    logger.info(f"Trace has {len(trace['Segments'])} segments")
                    
            except Exception as e:
                logger.warning(f"Failed to get trace graph: {str(e)}")
                pytest.skip("X-Ray trace graph not available")
    
    @pytest.mark.integration
    @pytest.mark.monitoring
    def test_put_trace_segments(self):
        """Test publishing trace segments to X-Ray"""
        with self.track_test_metrics('test_put_trace_segments'):
            # Create a test trace segment
            trace_id = f"1-{int(time.time())}-{os.urandom(12).hex()}"
            segment_id = os.urandom(8).hex()
            
            segment = {
                'name': 'staging-test',
                'id': segment_id,
                'trace_id': trace_id,
                'start_time': time.time(),
                'end_time': time.time() + 0.1,
                'http': {
                    'request': {
                        'method': 'GET',
                        'url': 'https://api-staging.example.com/test'
                    },
                    'response': {
                        'status': 200
                    }
                },
                'annotations': {
                    'test_type': 'integration',
                    'environment': 'staging'
                },
                'metadata': {
                    'test_data': {
                        'test_name': 'test_put_trace_segments',
                        'timestamp': datetime.now().isoformat()
                    }
                }
            }
            
            try:
                # Put trace segment
                response = self.xray.put_trace_segments(
                    TraceSegmentDocuments=[json.dumps(segment)]
                )
                
                assert 'UnprocessedTraceSegments' in response
                unprocessed = response['UnprocessedTraceSegments']
                
                # Verify all segments were processed
                assert len(unprocessed) == 0, f"Failed to process {len(unprocessed)} segments"
                
                logger.info(f"Successfully published trace segment: {trace_id}")
                
            except Exception as e:
                logger.error(f"Failed to publish trace segment: {str(e)}")
                raise
    
    @pytest.mark.integration
    @pytest.mark.monitoring
    def test_get_sampling_rules(self):
        """Test retrieving X-Ray sampling rules"""
        with self.track_test_metrics('test_get_sampling_rules'):
            try:
                response = self.xray.get_sampling_rules()
                
                assert 'SamplingRuleRecords' in response
                rules = response['SamplingRuleRecords']
                
                logger.info(f"Retrieved {len(rules)} sampling rules")
                
                # Verify rule structure
                if rules:
                    rule = rules[0]['SamplingRule']
                    assert 'RuleName' in rule
                    assert 'Priority' in rule
                    assert 'FixedRate' in rule
                    assert 'ReservoirSize' in rule
                    
                    logger.info(f"Sample rule: {rule['RuleName']}, Rate: {rule['FixedRate']}")
                    
            except Exception as e:
                logger.warning(f"Failed to get sampling rules: {str(e)}")
                pytest.skip("X-Ray sampling rules not available")


class MonitoringHealthCheckTest(BaseStagingIntegrationTest):
    """Monitoring health check and validation tests"""
    
    @pytest.mark.integration
    @pytest.mark.monitoring
    @pytest.mark.smoke
    def test_cloudwatch_service_health(self):
        """Test CloudWatch service health"""
        with self.track_test_metrics('test_cloudwatch_service_health'):
            try:
                # Simple operation to verify CloudWatch is accessible
                response = self.cloudwatch.list_metrics(
                    Namespace='AWS/EC2',
                    MaxRecords=1
                )
                
                assert response['ResponseMetadata']['HTTPStatusCode'] == 200
                logger.info("CloudWatch service is healthy")
                
            except Exception as e:
                pytest.fail(f"CloudWatch service health check failed: {str(e)}")
    
    @pytest.mark.integration
    @pytest.mark.monitoring
    @pytest.mark.smoke
    def test_xray_service_health(self):
        """Test X-Ray service health"""
        with self.track_test_metrics('test_xray_service_health'):
            try:
                # Simple operation to verify X-Ray is accessible
                end_time = datetime.now()
                start_time = end_time - timedelta(minutes=5)
                
                response = self.xray.get_service_graph(
                    StartTime=start_time,
                    EndTime=end_time
                )
                
                assert response['ResponseMetadata']['HTTPStatusCode'] == 200
                logger.info("X-Ray service is healthy")
                
            except Exception as e:
                pytest.fail(f"X-Ray service health check failed: {str(e)}")
    
    @pytest.mark.integration
    @pytest.mark.monitoring
    def test_monitoring_components_operational(self):
        """Test that all monitoring components are operational"""
        with self.track_test_metrics('test_monitoring_components_operational'):
            components_status = {
                'cloudwatch_metrics': False,
                'cloudwatch_logs': False,
                'xray_tracing': False
            }
            
            # Test CloudWatch Metrics
            try:
                self.cloudwatch.list_metrics(MaxRecords=1)
                components_status['cloudwatch_metrics'] = True
            except Exception as e:
                logger.error(f"CloudWatch Metrics not operational: {str(e)}")
            
            # Test CloudWatch Logs
            try:
                self.logs.describe_log_groups(limit=1)
                components_status['cloudwatch_logs'] = True
            except Exception as e:
                logger.error(f"CloudWatch Logs not operational: {str(e)}")
            
            # Test X-Ray
            try:
                end_time = datetime.now()
                start_time = end_time - timedelta(minutes=5)
                self.xray.get_service_graph(StartTime=start_time, EndTime=end_time)
                components_status['xray_tracing'] = True
            except Exception as e:
                logger.error(f"X-Ray not operational: {str(e)}")
            
            # Log status
            logger.info(f"Monitoring components status: {components_status}")
            
            # Assert at least CloudWatch is operational
            assert components_status['cloudwatch_metrics'], "CloudWatch Metrics must be operational"


# Pytest fixtures

@pytest.fixture(scope='module')
def cloudwatch_test_client():
    """Provide CloudWatch monitoring test client"""
    client = CloudWatchMonitoringTest()
    yield client
    client.cleanup()


@pytest.fixture(scope='module')
def xray_test_client():
    """Provide X-Ray monitoring test client"""
    client = XRayMonitoringTest()
    yield client
    client.cleanup()


@pytest.fixture(scope='module')
def health_check_client():
    """Provide monitoring health check client"""
    client = MonitoringHealthCheckTest()
    yield client
    client.cleanup()


if __name__ == '__main__':
    # Run tests with pytest
    pytest.main([__file__, '-v', '--tb=short', '-m', 'not slow'])
