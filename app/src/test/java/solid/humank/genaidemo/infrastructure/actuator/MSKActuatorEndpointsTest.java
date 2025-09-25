package solid.humank.genaidemo.infrastructure.actuator;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.DescribeConsumerGroupsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupsResult;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.Status;
import org.springframework.test.util.ReflectionTestUtils;
import solid.humank.genaidemo.infrastructure.messaging.MSKDataFlowTrackingService;
import solid.humank.genaidemo.infrastructure.tracing.MSKXRayTracingService;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

/**
 * MSK Actuator Endpoints Test Suite
 * 
 * Comprehensive tests for MSK Spring Boot Actuator endpoints including
 * health checks, metrics collection, performance monitoring, and error analysis.
 * 
 * @author Architecture Team
 * @since 2025-09-24
 */
@ExtendWith(MockitoExtension.class)
class MSKActuatorEndpointsTest {

    @Mock
    private AdminClient kafkaAdminClient;

    @Mock
    private KafkaProducer<String, Object> kafkaProducer;

    @Mock
    private KafkaConsumer<String, Object> kafkaConsumer;

    @Mock
    private MSKDataFlowTrackingService dataFlowTrackingService;

    @Mock
    private MSKXRayTracingService xrayTracingService;

    private MeterRegistry meterRegistry;
    private MSKActuatorEndpoints actuatorEndpoints;
    private MSKActuatorEndpoints.MSKHealthEndpoint healthEndpoint;
    private MSKActuatorEndpoints.MSKMetricsEndpoint metricsEndpoint;
    private MSKActuatorEndpoints.MSKFlowEndpoint flowEndpoint;
    private MSKActuatorEndpoints.MSKPerformanceEndpoint performanceEndpoint;
    private MSKActuatorEndpoints.MSKErrorsEndpoint errorsEndpoint;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        actuatorEndpoints = new MSKActuatorEndpoints(meterRegistry);
        
        // Inject mocked dependencies
        ReflectionTestUtils.setField(actuatorEndpoints, "kafkaAdminClient", kafkaAdminClient);
        ReflectionTestUtils.setField(actuatorEndpoints, "kafkaProducer", kafkaProducer);
        ReflectionTestUtils.setField(actuatorEndpoints, "kafkaConsumer", kafkaConsumer);
        ReflectionTestUtils.setField(actuatorEndpoints, "dataFlowTrackingService", dataFlowTrackingService);
        ReflectionTestUtils.setField(actuatorEndpoints, "xrayTracingService", xrayTracingService);

        // Create endpoint instances
        healthEndpoint = new MSKActuatorEndpoints.MSKHealthEndpoint(actuatorEndpoints);
        metricsEndpoint = new MSKActuatorEndpoints.MSKMetricsEndpoint(actuatorEndpoints);
        flowEndpoint = new MSKActuatorEndpoints.MSKFlowEndpoint(actuatorEndpoints);
        performanceEndpoint = new MSKActuatorEndpoints.MSKPerformanceEndpoint(actuatorEndpoints);
        errorsEndpoint = new MSKActuatorEndpoints.MSKErrorsEndpoint(actuatorEndpoints);
    }

    @Test
    void should_return_healthy_status_when_all_components_are_working() {
        // Given
        setupHealthyKafkaComponents();

        // When
        Map<String, Object> healthStatus = healthEndpoint.mskHealth();

        // Then
        assertThat(healthStatus).isNotNull();
        assertThat(healthStatus.get("overall_healthy")).isEqualTo(true);
        assertThat(healthStatus.get("status")).isEqualTo("UP");
        assertThat(healthStatus.get("admin_client_healthy")).isEqualTo(true);
        assertThat(healthStatus.get("producer_healthy")).isEqualTo(true);
        assertThat(healthStatus.get("consumer_healthy")).isEqualTo(true);
        assertThat(healthStatus).containsKey("timestamp");
        assertThat(healthStatus).containsKey("consumer_groups");
        assertThat(healthStatus).containsKey("connection_details");
    }

    @Test
    void should_return_unhealthy_status_when_admin_client_fails() {
        // Given
        when(kafkaAdminClient.listTopics()).thenThrow(new RuntimeException("Connection failed"));
        when(kafkaProducer.metrics()).thenReturn(Collections.emptyMap());
        when(kafkaConsumer.assignment()).thenReturn(Collections.emptySet());

        // When
        Map<String, Object> healthStatus = healthEndpoint.mskHealth();

        // Then
        assertThat(healthStatus.get("overall_healthy")).isEqualTo(false);
        assertThat(healthStatus.get("status")).isEqualTo("DOWN");
        assertThat(healthStatus.get("admin_client_healthy")).isEqualTo(false);
    }

    @Test
    void should_return_spring_boot_health_indicator_status() {
        // Given
        setupHealthyKafkaComponents();

        // When
        Health health = healthEndpoint.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("overall_healthy");
        assertThat(health.getDetails()).containsKey("admin_client_healthy");
        assertThat(health.getDetails()).containsKey("producer_healthy");
        assertThat(health.getDetails()).containsKey("consumer_healthy");
    }

    @Test
    void should_collect_business_metrics_successfully() {
        // Given
        setupMockMetrics();

        // When
        Map<String, Object> metrics = metricsEndpoint.mskMetrics();

        // Then
        assertThat(metrics).isNotNull();
        assertThat(metrics).containsKey("timestamp");
        assertThat(metrics).containsKey("business_events");
        assertThat(metrics).containsKey("processing_stats");
        assertThat(metrics).containsKey("topic_metrics");
        assertThat(metrics).containsKey("consumer_group_metrics");
        assertThat(metrics).containsKey("throughput");

        @SuppressWarnings("unchecked")
        Map<String, Object> businessEvents = (Map<String, Object>) metrics.get("business_events");
        assertThat(businessEvents).containsKey("order_events_per_minute");
        assertThat(businessEvents).containsKey("customer_events_per_minute");
        assertThat(businessEvents).containsKey("payment_events_per_minute");
        assertThat(businessEvents).containsKey("inventory_events_per_minute");
    }

    @Test
    void should_provide_data_flow_information() {
        // Given
        Map<String, Object> mockFlowStatus = Map.of(
            "active_flows", 5,
            "total_events_processed", 1000,
            "current_throughput", 50.0
        );
        when(dataFlowTrackingService.getCurrentFlowStatus()).thenReturn(mockFlowStatus);

        Map<String, Object> mockTraceStats = Map.of(
            "active_traces", 10,
            "service_name", "genai-demo",
            "tracing_name", "genai-demo-msk"
        );
        when(xrayTracingService.getTraceStatistics()).thenReturn(mockTraceStats);

        // When
        Map<String, Object> dataFlow = flowEndpoint.mskFlow();

        // Then
        assertThat(dataFlow).isNotNull();
        assertThat(dataFlow).containsKey("timestamp");
        assertThat(dataFlow).containsKey("real_time_flow");
        assertThat(dataFlow).containsKey("event_lineage");
        assertThat(dataFlow).containsKey("flow_patterns");
        assertThat(dataFlow).containsKey("service_dependencies");
        assertThat(dataFlow).containsKey("visualization");

        @SuppressWarnings("unchecked")
        Map<String, Object> realTimeFlow = (Map<String, Object>) dataFlow.get("real_time_flow");
        assertThat(realTimeFlow).isEqualTo(mockFlowStatus);

        @SuppressWarnings("unchecked")
        Map<String, Object> serviceDependencies = (Map<String, Object>) dataFlow.get("service_dependencies");
        assertThat(serviceDependencies).isEqualTo(mockTraceStats);
    }

    @Test
    void should_provide_performance_metrics() {
        // Given
        setupMockMetrics();

        // When
        Map<String, Object> performance = performanceEndpoint.mskPerformance();

        // Then
        assertThat(performance).isNotNull();
        assertThat(performance).containsKey("timestamp");
        assertThat(performance).containsKey("latency");
        assertThat(performance).containsKey("throughput_analysis");
        assertThat(performance).containsKey("resource_utilization");
        assertThat(performance).containsKey("trends");
        assertThat(performance).containsKey("bottlenecks");

        @SuppressWarnings("unchecked")
        Map<String, Object> latency = (Map<String, Object>) performance.get("latency");
        assertThat(latency).containsKey("producer_latency_p95");
        assertThat(latency).containsKey("consumer_latency_p95");
        assertThat(latency).containsKey("end_to_end_latency_p95");
    }

    @Test
    void should_provide_error_analysis() {
        // Given
        setupMockMetrics();

        // When
        Map<String, Object> errors = errorsEndpoint.mskErrors();

        // Then
        assertThat(errors).isNotNull();
        assertThat(errors).containsKey("timestamp");
        assertThat(errors).containsKey("error_stats");
        assertThat(errors).containsKey("error_patterns");
        assertThat(errors).containsKey("recovery_status");
        assertThat(errors).containsKey("dlq_analysis");
        assertThat(errors).containsKey("error_trends");

        @SuppressWarnings("unchecked")
        Map<String, Object> errorStats = (Map<String, Object>) errors.get("error_stats");
        assertThat(errorStats).containsKey("total_errors");
        assertThat(errorStats).containsKey("error_rate");
        assertThat(errorStats).containsKey("producer_errors");
        assertThat(errorStats).containsKey("consumer_errors");
    }

    @Test
    void should_handle_consumer_group_health_check() {
        // Given
        setupConsumerGroupMocks();

        // When
        Map<String, Object> healthStatus = healthEndpoint.mskHealth();

        // Then
        assertThat(healthStatus).containsKey("consumer_groups");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> consumerGroups = (Map<String, Object>) healthStatus.get("consumer_groups");
        assertThat(consumerGroups.get("status")).isEqualTo("available");
        assertThat(consumerGroups.get("total_groups")).isEqualTo(2);
        assertThat(consumerGroups.get("healthy_groups")).isEqualTo(1);
        assertThat(consumerGroups).containsKey("groups");
    }

    @Test
    void should_handle_kafka_admin_client_timeout() {
        // Given
        ListConsumerGroupsResult mockListResult = mock(ListConsumerGroupsResult.class);
        when(kafkaAdminClient.listTopics()).thenReturn(null);
        when(kafkaAdminClient.listConsumerGroups()).thenReturn(mockListResult);
        when(mockListResult.all()).thenReturn(CompletableFuture.failedFuture(
            new RuntimeException("Timeout")));

        // When
        Map<String, Object> healthStatus = healthEndpoint.mskHealth();

        // Then
        assertThat(healthStatus.get("admin_client_healthy")).isEqualTo(false);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> consumerGroups = (Map<String, Object>) healthStatus.get("consumer_groups");
        assertThat(consumerGroups.get("status")).isEqualTo("error");
        assertThat(consumerGroups).containsKey("error");
    }

    @Test
    void should_handle_null_kafka_components_gracefully() {
        // Given
        ReflectionTestUtils.setField(actuatorEndpoints, "kafkaAdminClient", null);
        ReflectionTestUtils.setField(actuatorEndpoints, "kafkaProducer", null);
        ReflectionTestUtils.setField(actuatorEndpoints, "kafkaConsumer", null);

        // When
        Map<String, Object> healthStatus = healthEndpoint.mskHealth();

        // Then
        assertThat(healthStatus.get("overall_healthy")).isEqualTo(false);
        assertThat(healthStatus.get("status")).isEqualTo("DOWN");
        assertThat(healthStatus.get("admin_client_healthy")).isEqualTo(false);
        assertThat(healthStatus.get("producer_healthy")).isEqualTo(false);
        assertThat(healthStatus.get("consumer_healthy")).isEqualTo(false);

        @SuppressWarnings("unchecked")
        Map<String, Object> connectionDetails = (Map<String, Object>) healthStatus.get("connection_details");
        assertThat(connectionDetails.get("admin_client_available")).isEqualTo(false);
        assertThat(connectionDetails.get("producer_available")).isEqualTo(false);
        assertThat(connectionDetails.get("consumer_available")).isEqualTo(false);
    }

    @Test
    void should_increment_health_check_metrics() {
        // Given
        setupHealthyKafkaComponents();
        double initialCount = meterRegistry.find("msk.health.checks").counter().count();

        // When
        healthEndpoint.mskHealth();

        // Then
        double finalCount = meterRegistry.find("msk.health.checks").counter().count();
        assertThat(finalCount).isEqualTo(initialCount + 1);
    }

    @Test
    void should_record_health_check_duration() {
        // Given
        setupHealthyKafkaComponents();

        // When
        healthEndpoint.mskHealth();

        // Then
        assertThat(meterRegistry.find("msk.health.check.duration").timer()).isNotNull();
        assertThat(meterRegistry.find("msk.health.check.duration").timer().count()).isEqualTo(1);
    }

    @Test
    void should_handle_exception_in_health_check() {
        // Given
        when(kafkaAdminClient.listTopics()).thenThrow(new RuntimeException("Unexpected error"));

        // When
        Map<String, Object> healthStatus = healthEndpoint.mskHealth();

        // Then
        assertThat(healthStatus.get("overall_healthy")).isEqualTo(false);
        assertThat(healthStatus.get("status")).isEqualTo("DOWN");
        assertThat(healthStatus).containsKey("error");
        assertThat(healthStatus).containsKey("timestamp");
    }

    @Test
    void should_handle_exception_in_metrics_collection() {
        // Given
        when(dataFlowTrackingService.getCurrentFlowStatus())
            .thenThrow(new RuntimeException("Service unavailable"));

        // When
        Map<String, Object> dataFlow = flowEndpoint.mskFlow();

        // Then
        assertThat(dataFlow).containsKey("error");
        assertThat(dataFlow).containsKey("timestamp");
    }

    // Helper methods

    private void setupHealthyKafkaComponents() {
        // Mock successful admin client
        ListConsumerGroupsResult mockListResult = mock(ListConsumerGroupsResult.class);
        when(kafkaAdminClient.listTopics()).thenReturn(null); // Simplified mock
        when(kafkaAdminClient.listConsumerGroups()).thenReturn(mockListResult);
        when(mockListResult.all()).thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));

        // Mock successful producer
        when(kafkaProducer.metrics()).thenReturn(Collections.emptyMap());

        // Mock successful consumer
        when(kafkaConsumer.assignment()).thenReturn(Collections.emptySet());
    }

    private void setupConsumerGroupMocks() {
        // Mock consumer group listing
        ListConsumerGroupsResult mockListResult = mock(ListConsumerGroupsResult.class);
        when(kafkaAdminClient.listConsumerGroups()).thenReturn(mockListResult);
        
        // Create mock consumer group listings
        org.apache.kafka.clients.admin.ConsumerGroupListing group1 = 
            mock(org.apache.kafka.clients.admin.ConsumerGroupListing.class);
        org.apache.kafka.clients.admin.ConsumerGroupListing group2 = 
            mock(org.apache.kafka.clients.admin.ConsumerGroupListing.class);
        
        when(group1.groupId()).thenReturn("group1");
        when(group2.groupId()).thenReturn("group2");
        
        List<org.apache.kafka.clients.admin.ConsumerGroupListing> groupListings = 
            Arrays.asList(group1, group2);
        when(mockListResult.all()).thenReturn(CompletableFuture.completedFuture(groupListings));

        // Mock consumer group descriptions
        DescribeConsumerGroupsResult mockDescribeResult = mock(DescribeConsumerGroupsResult.class);
        when(kafkaAdminClient.describeConsumerGroups(anySet())).thenReturn(mockDescribeResult);

        ConsumerGroupDescription desc1 = mock(ConsumerGroupDescription.class);
        ConsumerGroupDescription desc2 = mock(ConsumerGroupDescription.class);
        
        when(desc1.state()).thenReturn(ConsumerGroupState.STABLE);
        when(desc1.members()).thenReturn(Collections.emptyList());
        when(desc1.coordinator()).thenReturn(new Node(1, "broker1", 9092));
        
        when(desc2.state()).thenReturn(ConsumerGroupState.REBALANCING);
        when(desc2.members()).thenReturn(Collections.emptyList());
        when(desc2.coordinator()).thenReturn(new Node(2, "broker2", 9092));

        Map<String, ConsumerGroupDescription> descriptions = Map.of(
            "group1", desc1,
            "group2", desc2
        );
        when(mockDescribeResult.all()).thenReturn(CompletableFuture.completedFuture(descriptions));
    }

    private void setupMockMetrics() {
        // Register some test metrics
        meterRegistry.gauge("order.events.rate", 10.0);
        meterRegistry.gauge("customer.events.rate", 5.0);
        meterRegistry.gauge("payment.events.rate", 8.0);
        meterRegistry.gauge("inventory.events.rate", 3.0);
        
        meterRegistry.gauge("events.processed.total", 1000.0);
        meterRegistry.gauge("events.processing.rate", 50.0);
        meterRegistry.gauge("events.processing.time.avg", 25.0);
        meterRegistry.gauge("events.processing.success.rate", 0.99);
        
        meterRegistry.gauge("kafka.messages.rate", 100.0);
        meterRegistry.gauge("kafka.bytes.rate", 1024.0);
        meterRegistry.gauge("kafka.throughput.peak", 200.0);
        
        meterRegistry.gauge("kafka.producer.latency.p95", 50.0);
        meterRegistry.gauge("kafka.consumer.latency.p95", 30.0);
        meterRegistry.gauge("kafka.e2e.latency.p95", 80.0);
        
        meterRegistry.gauge("kafka.errors.total", 5.0);
        meterRegistry.gauge("kafka.errors.rate", 0.01);
        meterRegistry.gauge("kafka.producer.errors", 2.0);
        meterRegistry.gauge("kafka.consumer.errors", 3.0);
    }
}