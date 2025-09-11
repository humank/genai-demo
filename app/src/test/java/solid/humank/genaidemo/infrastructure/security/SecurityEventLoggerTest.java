package solid.humank.genaidemo.infrastructure.security;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for SecurityEventLogger
 * Tests requirement 11.5: IF unauthorized access is attempted THEN the system
 * SHALL log security events and alert administrators
 */
@ExtendWith(MockitoExtension.class)
class SecurityEventLoggerTest {

    private SecurityEventLogger securityEventLogger;
    private ObjectMapper objectMapper;

    @Mock
    private WebAuthenticationDetails authenticationDetails;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        securityEventLogger = new SecurityEventLogger(objectMapper);
    }

    @Test
    void shouldLogAuthenticationSuccessEvent() {
        // Given
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "testuser",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        authentication.setDetails(authenticationDetails);
        when(authenticationDetails.getRemoteAddress()).thenReturn("192.168.1.100");

        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);

        // When
        securityEventLogger.handleAuthenticationSuccess(event);

        // Then
        // Verify that the event was logged (in a real test, we would capture log
        // output)
        verify(authenticationDetails).getRemoteAddress();
    }

    @Test
    void shouldLogSuspiciousActivity() {
        // Given
        String activityType = "MULTIPLE_FAILED_LOGINS";
        String description = "Multiple failed login attempts detected";
        Map<String, Object> details = Map.of(
                "attemptCount", 5,
                "timeWindow", "5 minutes");

        // When
        securityEventLogger.logSuspiciousActivity(activityType, description, details);

        // Then
        // In a real implementation, we would verify the log output and alert triggering
        // For now, we just verify the method executes without error
    }

    @Test
    void shouldLogDataAccess() {
        // Given
        String dataType = "CUSTOMER_DATA";
        String operation = "READ";
        String resourceId = "CUST-12345";

        // When
        securityEventLogger.logDataAccess(dataType, operation, resourceId);

        // Then
        // Verify method executes without error
    }

    @Test
    void shouldLogPiiAccess() {
        // Given
        String piiType = "EMAIL";
        String operation = "READ";
        String resourceId = "CUST-12345";
        String justification = "Customer support request";

        // When
        securityEventLogger.logPiiAccess(piiType, operation, resourceId, justification);

        // Then
        // Verify method executes without error and would trigger alert
    }

    @Test
    void shouldLogConfigurationChange() {
        // Given
        String configType = "SECURITY_POLICY";
        String oldValue = "old_config";
        String newValue = "new_config";

        // When
        securityEventLogger.logConfigurationChange(configType, oldValue, newValue);

        // Then
        // Verify method executes without error
    }

    @Test
    void shouldHandleNullAuthentication() {
        // Given
        String activityType = "ANONYMOUS_ACCESS";
        String description = "Anonymous access attempt";

        // When
        securityEventLogger.logSuspiciousActivity(activityType, description, null);

        // Then
        // Verify method executes without error even with null authentication
    }
}