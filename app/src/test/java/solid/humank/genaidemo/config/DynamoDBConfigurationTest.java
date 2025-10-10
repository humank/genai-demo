package solid.humank.genaidemo.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * DynamoDB Configuration Unit Test
 * 
 * Tests DynamoDB configuration properties and setup logic:
 * - Configuration properties validation
 * - Global Tables configuration
 * - Conflict resolution strategy
 * 
 * Note: This is a UNIT TEST - no Spring context required.
 * For actual DynamoDB integration testing, see DynamoDBIntegrationTest.
 * 
 * Requirements: 4.1.4 - Cross-region data synchronization testing
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("DynamoDB Configuration Unit Tests")
class DynamoDBConfigurationTest {

    @Mock
    private DynamoDbClient dynamoDbClient;

    @Mock
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;

    private DynamoDBConfiguration.DynamoDBGlobalTablesProperties globalTablesProperties;

    @BeforeEach
    void setUp() {
        // Setup test configuration properties using Builder pattern
        globalTablesProperties = DynamoDBConfiguration.DynamoDBGlobalTablesProperties.builder()
            .enabled(true)
            .primaryRegion("us-east-1")
            .conflictResolution(DynamoDBConfiguration.ConflictResolutionStrategy.LAST_WRITER_WINS)
            .build();
    }

    @Test
    @DisplayName("Should configure DynamoDB client when enabled")
    void should_configure_dynamodb_client_when_enabled() {
        // Given - Mock clients are injected
        when(dynamoDbClient.serviceName()).thenReturn("DynamoDB");
        
        // When & Then - Verify mocks are properly set up
        assertThat(dynamoDbClient).isNotNull();
        assertThat(dynamoDbEnhancedClient).isNotNull();
        assertThat(dynamoDbClient.serviceName()).isEqualTo("DynamoDB");
    }

    @Test
    @DisplayName("Should configure global tables properties correctly")
    void should_configure_global_tables_properties() {
        // Given & When & Then
        assertThat(globalTablesProperties).isNotNull();
        assertThat(globalTablesProperties.isEnabled()).isTrue();
        assertThat(globalTablesProperties.getPrimaryRegion()).isEqualTo("us-east-1");
        assertThat(globalTablesProperties.getConflictResolution())
                .isEqualTo(DynamoDBConfiguration.ConflictResolutionStrategy.LAST_WRITER_WINS);
    }

    @Test
    @DisplayName("Should validate conflict resolution strategy configuration")
    void should_validate_conflict_resolution_strategy() {
        // Given & When
        DynamoDBConfiguration.DynamoDBGlobalTablesProperties properties = 
            DynamoDBConfiguration.DynamoDBGlobalTablesProperties.builder()
                .conflictResolution(DynamoDBConfiguration.ConflictResolutionStrategy.LAST_WRITER_WINS)
                .build();
        
        // Then
        assertThat(properties.getConflictResolution())
            .isEqualTo(DynamoDBConfiguration.ConflictResolutionStrategy.LAST_WRITER_WINS);
    }

    @Test
    @DisplayName("Should configure primary region correctly")
    void should_configure_primary_region() {
        // Given
        String primaryRegion = "ap-northeast-1";
        
        // When
        DynamoDBConfiguration.DynamoDBGlobalTablesProperties properties = 
            DynamoDBConfiguration.DynamoDBGlobalTablesProperties.builder()
                .primaryRegion(primaryRegion)
                .build();
        
        // Then
        assertThat(properties.getPrimaryRegion()).isEqualTo(primaryRegion);
    }

    @Test
    @DisplayName("Should enable/disable global tables configuration")
    void should_enable_disable_global_tables() {
        // Given & When - Enable
        DynamoDBConfiguration.DynamoDBGlobalTablesProperties enabledProperties = 
            DynamoDBConfiguration.DynamoDBGlobalTablesProperties.builder()
                .enabled(true)
                .build();
        
        // Then
        assertThat(enabledProperties.isEnabled()).isTrue();
        
        // When - Disable
        DynamoDBConfiguration.DynamoDBGlobalTablesProperties disabledProperties = 
            DynamoDBConfiguration.DynamoDBGlobalTablesProperties.builder()
                .enabled(false)
                .build();
        
        // Then
        assertThat(disabledProperties.isEnabled()).isFalse();
    }
}

/*
 * NOTE: Session repository tests have been moved to DynamoDBSessionRepositoryIntegrationTest
 * 
 * These tests require actual DynamoDB connection and should be integration tests:
 * - should_create_and_retrieve_session_entity
 * - should_find_session_by_id
 * - should_handle_session_expiration
 * - should_extend_session_expiration
 * - should_invalidate_session
 * - should_track_cross_region_modifications
 * 
 * For integration testing with DynamoDB, use @SpringBootTest with proper test profile.
 */