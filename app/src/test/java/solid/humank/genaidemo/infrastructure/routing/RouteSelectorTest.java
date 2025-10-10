package solid.humank.genaidemo.infrastructure.routing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RouteSelector.
 * 
 * Tests intelligent endpoint selection logic with various health scenarios.
 */
@ExtendWith(MockitoExtension.class)
class RouteSelectorTest {
    
    @Mock
    private RegionDetector mockRegionDetector;
    
    @Mock
    private HealthChecker mockHealthChecker;
    
    private RouteSelector routeSelector;
    
    @BeforeEach
    void setUp() {
        routeSelector = new RouteSelector(mockRegionDetector, mockHealthChecker);
    }
    
    @Test
    void should_select_local_healthy_endpoint_when_available() {
        // Given
        when(mockRegionDetector.detectRegion()).thenReturn("ap-northeast-1");
        when(mockHealthChecker.isHealthy("taiwan-db")).thenReturn(true);
        
        List<String> endpoints = List.of("taiwan-db", "japan-db");
        
        // When
        Optional<String> selected = routeSelector.selectEndpoint(endpoints);
        
        // Then
        assertThat(selected).isPresent();
        assertThat(selected.get()).isEqualTo("taiwan-db"); // Local region preferred
    }
    
    @Test
    void should_failover_to_remote_when_local_unhealthy() {
        // Given
        when(mockRegionDetector.detectRegion()).thenReturn("ap-northeast-1");
        when(mockHealthChecker.isHealthy("taiwan-db")).thenReturn(false);
        when(mockHealthChecker.isHealthy("japan-db")).thenReturn(true);
        // Note: getLatency is not needed when only one healthy endpoint exists
        
        List<String> endpoints = List.of("taiwan-db", "japan-db");
        
        // When
        Optional<String> selected = routeSelector.selectEndpoint(endpoints);
        
        // Then
        assertThat(selected).isPresent();
        assertThat(selected.get()).isEqualTo("japan-db"); // Failover to healthy remote
    }
    
    @Test
    void should_select_endpoint_with_lower_latency() {
        // Given
        when(mockRegionDetector.detectRegion()).thenReturn("ap-northeast-1");
        when(mockHealthChecker.isHealthy("taiwan-db")).thenReturn(false);
        when(mockHealthChecker.isHealthy("japan-db")).thenReturn(true);
        when(mockHealthChecker.isHealthy("singapore-db")).thenReturn(true);
        when(mockHealthChecker.getLatency("japan-db")).thenReturn(100L);
        when(mockHealthChecker.getLatency("singapore-db")).thenReturn(50L);
        
        List<String> endpoints = List.of("taiwan-db", "japan-db", "singapore-db");
        
        // When
        Optional<String> selected = routeSelector.selectEndpoint(endpoints);
        
        // Then
        assertThat(selected).isPresent();
        assertThat(selected.get()).isEqualTo("singapore-db"); // Lower latency preferred
    }
    
    @Test
    void should_return_first_endpoint_when_all_unhealthy() {
        // Given
        when(mockRegionDetector.detectRegion()).thenReturn("ap-northeast-1");
        when(mockHealthChecker.isHealthy(anyString())).thenReturn(false);
        
        List<String> endpoints = List.of("taiwan-db", "japan-db");
        
        // When
        Optional<String> selected = routeSelector.selectEndpoint(endpoints);
        
        // Then
        assertThat(selected).isPresent();
        assertThat(selected.get()).isEqualTo("taiwan-db"); // Last resort
    }
    
    @Test
    void should_return_empty_when_no_endpoints_available() {
        // Given
        List<String> endpoints = List.of();
        
        // When
        Optional<String> selected = routeSelector.selectEndpoint(endpoints);
        
        // Then
        assertThat(selected).isEmpty();
    }
    
    @Test
    void should_select_database_endpoint_correctly() {
        // Given
        when(mockRegionDetector.detectRegion()).thenReturn("ap-northeast-1");
        when(mockHealthChecker.isHealthy("taiwan-db")).thenReturn(true);
        
        // When
        String selected = routeSelector.selectDatabaseEndpoint();
        
        // Then
        assertThat(selected).isEqualTo("taiwan-db");
    }
    
    @Test
    void should_select_redis_endpoint_correctly() {
        // Given
        when(mockRegionDetector.detectRegion()).thenReturn("ap-northeast-1");
        when(mockHealthChecker.isHealthy("taiwan-redis")).thenReturn(true);
        
        // When
        String selected = routeSelector.selectRedisEndpoint();
        
        // Then
        assertThat(selected).isEqualTo("taiwan-redis");
    }
    
    @Test
    void should_select_kafka_endpoint_correctly() {
        // Given
        when(mockRegionDetector.detectRegion()).thenReturn("ap-northeast-1");
        when(mockHealthChecker.isHealthy("taiwan-kafka")).thenReturn(true);
        
        // When
        String selected = routeSelector.selectKafkaEndpoint();
        
        // Then
        assertThat(selected).isEqualTo("taiwan-kafka");
    }
}
