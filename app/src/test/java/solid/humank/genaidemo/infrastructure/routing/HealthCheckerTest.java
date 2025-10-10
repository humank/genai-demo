package solid.humank.genaidemo.infrastructure.routing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HealthChecker.
 * 
 * Tests health checking functionality with mocked DataSources.
 */
@ExtendWith(MockitoExtension.class)
class HealthCheckerTest {
    
    @Mock
    private DataSource mockDataSource;
    
    @Mock
    private Connection mockConnection;
    
    private HealthChecker healthChecker;
    
    @BeforeEach
    void setUp() {
        healthChecker = new HealthChecker();
    }
    
    @Test
    void should_register_datasource_successfully() {
        // When
        healthChecker.registerDataSource("test-db", mockDataSource);
        
        // Then
        // No exception thrown, registration successful
        assertThat(healthChecker.getAllHealth()).isEmpty(); // No health checks performed yet
    }
    
    @Test
    void should_report_healthy_when_connection_valid() throws SQLException {
        // Given
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.isValid(2)).thenReturn(true);
        healthChecker.registerDataSource("test-db", mockDataSource);
        
        // When
        healthChecker.performHealthChecks();
        
        // Then
        assertThat(healthChecker.isHealthy("test-db")).isTrue();
        assertThat(healthChecker.getLatency("test-db")).isGreaterThanOrEqualTo(0);
        
        verify(mockConnection).close();
    }
    
    @Test
    void should_report_unhealthy_when_connection_invalid() throws SQLException {
        // Given
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.isValid(2)).thenReturn(false);
        healthChecker.registerDataSource("test-db", mockDataSource);
        
        // When
        healthChecker.performHealthChecks();
        
        // Then
        assertThat(healthChecker.isHealthy("test-db")).isFalse();
        
        verify(mockConnection).close();
    }
    
    @Test
    void should_report_unhealthy_when_connection_fails() throws SQLException {
        // Given
        when(mockDataSource.getConnection()).thenThrow(new SQLException("Connection failed"));
        healthChecker.registerDataSource("test-db", mockDataSource);
        
        // When
        healthChecker.performHealthChecks();
        
        // Then
        assertThat(healthChecker.isHealthy("test-db")).isFalse();
        EndpointHealth health = healthChecker.getHealth("test-db");
        assertThat(health.errorMessage()).contains("Connection failed");
    }
    
    @Test
    void should_return_unhealthy_for_unknown_endpoint() {
        // When
        EndpointHealth health = healthChecker.getHealth("unknown-endpoint");
        
        // Then
        assertThat(health.isHealthy()).isFalse();
        assertThat(health.errorMessage()).contains("No health data available");
    }
    
    @Test
    void should_track_multiple_endpoints() throws SQLException {
        // Given
        DataSource mockDataSource2 = mock(DataSource.class);
        Connection mockConnection2 = mock(Connection.class);
        
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.isValid(2)).thenReturn(true);
        
        when(mockDataSource2.getConnection()).thenReturn(mockConnection2);
        when(mockConnection2.isValid(2)).thenReturn(true);
        
        healthChecker.registerDataSource("taiwan-db", mockDataSource);
        healthChecker.registerDataSource("japan-db", mockDataSource2);
        
        // When
        healthChecker.performHealthChecks();
        
        // Then
        Map<String, EndpointHealth> allHealth = healthChecker.getAllHealth();
        assertThat(allHealth).hasSize(2);
        assertThat(allHealth).containsKeys("taiwan-db", "japan-db");
        assertThat(healthChecker.isHealthy("taiwan-db")).isTrue();
        assertThat(healthChecker.isHealthy("japan-db")).isTrue();
    }
    
    @Test
    void should_return_negative_latency_for_unknown_endpoint() {
        // When
        long latency = healthChecker.getLatency("unknown-endpoint");
        
        // Then
        assertThat(latency).isEqualTo(-1);
    }
}
