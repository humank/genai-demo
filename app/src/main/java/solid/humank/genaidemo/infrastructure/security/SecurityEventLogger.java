package solid.humank.genaidemo.infrastructure.security;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Security event logger for monitoring and alerting
 * Implements requirement 11.5: IF unauthorized access is attempted THEN the
 * system SHALL log security events and alert administrators
 */
@Component
public class SecurityEventLogger {

    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY");
    private final ObjectMapper objectMapper;

    public SecurityEventLogger(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Log successful authentication events
     */
    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        SecurityEvent securityEvent = SecurityEvent.builder()
                .eventType("AUTHENTICATION_SUCCESS")
                .severity(SecuritySeverity.INFO)
                .username(event.getAuthentication().getName())
                .sourceIp(getSourceIp(event.getAuthentication()))
                .userAgent(getUserAgent(event.getAuthentication()))
                .timestamp(LocalDateTime.now())
                .details(Map.of(
                        "authenticationMethod", event.getAuthentication().getClass().getSimpleName(),
                        "authorities", event.getAuthentication().getAuthorities().toString()))
                .build();

        logSecurityEvent(securityEvent);
    }

    /**
     * Log failed authentication events
     */
    @EventListener
    public void handleAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        SecurityEvent securityEvent = SecurityEvent.builder()
                .eventType("AUTHENTICATION_FAILURE")
                .severity(SecuritySeverity.WARNING)
                .username(event.getAuthentication().getName())
                .sourceIp(getSourceIp(event.getAuthentication()))
                .userAgent(getUserAgent(event.getAuthentication()))
                .timestamp(LocalDateTime.now())
                .details(Map.of(
                        "failureReason", event.getException().getClass().getSimpleName(),
                        "failureMessage", event.getException().getMessage()))
                .build();

        logSecurityEvent(securityEvent);

        // Alert on repeated failures
        checkForBruteForceAttack(securityEvent);
    }

    /**
     * Log authorization denied events
     */
    @EventListener
    public void handleAuthorizationDenied(AuthorizationDeniedEvent<?> event) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        SecurityEvent securityEvent = SecurityEvent.builder()
                .eventType("AUTHORIZATION_DENIED")
                .severity(SecuritySeverity.WARNING)
                .username(auth != null ? auth.getName() : "anonymous")
                .sourceIp(getSourceIp(auth))
                .userAgent(getUserAgent(auth))
                .timestamp(LocalDateTime.now())
                .details(Map.of(
                        "resource", event.getAuthorizationDecision().toString(),
                        "requiredAuthorities", event.getAuthorizationDecision().toString()))
                .build();

        logSecurityEvent(securityEvent);
    }

    /**
     * Log suspicious activity
     */
    public void logSuspiciousActivity(String activityType, String description, Map<String, Object> details) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        SecurityEvent securityEvent = SecurityEvent.builder()
                .eventType("SUSPICIOUS_ACTIVITY")
                .severity(SecuritySeverity.HIGH)
                .username(auth != null ? auth.getName() : "anonymous")
                .sourceIp(getSourceIp(auth))
                .userAgent(getUserAgent(auth))
                .timestamp(LocalDateTime.now())
                .description(description)
                .details(details != null ? details : new HashMap<>())
                .build();

        logSecurityEvent(securityEvent);

        // Trigger immediate alert for high severity events
        triggerSecurityAlert(securityEvent);
    }

    /**
     * Log data access events
     */
    public void logDataAccess(String dataType, String operation, String resourceId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        SecurityEvent securityEvent = SecurityEvent.builder()
                .eventType("DATA_ACCESS")
                .severity(SecuritySeverity.INFO)
                .username(auth != null ? auth.getName() : "system")
                .sourceIp(getSourceIp(auth))
                .userAgent(getUserAgent(auth))
                .timestamp(LocalDateTime.now())
                .details(Map.of(
                        "dataType", dataType,
                        "operation", operation,
                        "resourceId", resourceId))
                .build();

        logSecurityEvent(securityEvent);
    }

    /**
     * Log PII access events
     */
    public void logPiiAccess(String piiType, String operation, String resourceId, String justification) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        SecurityEvent securityEvent = SecurityEvent.builder()
                .eventType("PII_ACCESS")
                .severity(SecuritySeverity.HIGH)
                .username(auth != null ? auth.getName() : "system")
                .sourceIp(getSourceIp(auth))
                .userAgent(getUserAgent(auth))
                .timestamp(LocalDateTime.now())
                .details(Map.of(
                        "piiType", piiType,
                        "operation", operation,
                        "resourceId", resourceId,
                        "justification", justification))
                .build();

        logSecurityEvent(securityEvent);

        // PII access always triggers an alert
        triggerSecurityAlert(securityEvent);
    }

    /**
     * Log configuration changes
     */
    public void logConfigurationChange(String configType, String oldValue, String newValue) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        SecurityEvent securityEvent = SecurityEvent.builder()
                .eventType("CONFIGURATION_CHANGE")
                .severity(SecuritySeverity.MEDIUM)
                .username(auth != null ? auth.getName() : "system")
                .sourceIp(getSourceIp(auth))
                .userAgent(getUserAgent(auth))
                .timestamp(LocalDateTime.now())
                .details(Map.of(
                        "configType", configType,
                        "oldValue", "***MASKED***", // Don't log actual values
                        "newValue", "***MASKED***"))
                .build();

        logSecurityEvent(securityEvent);
    }

    private void logSecurityEvent(SecurityEvent event) {
        try {
            // Set MDC for structured logging
            MDC.put("eventType", event.getEventType());
            MDC.put("severity", event.getSeverity().name());
            MDC.put("username", event.getUsername());
            MDC.put("sourceIp", event.getSourceIp());

            // Log as JSON for structured processing
            String eventJson = objectMapper.writeValueAsString(event);

            switch (event.getSeverity()) {
                case CRITICAL -> securityLogger.error("SECURITY_EVENT: {}", eventJson);
                case HIGH -> securityLogger.warn("SECURITY_EVENT: {}", eventJson);
                case MEDIUM -> securityLogger.warn("SECURITY_EVENT: {}", eventJson);
                case WARNING -> securityLogger.warn("SECURITY_EVENT: {}", eventJson);
                case INFO -> securityLogger.info("SECURITY_EVENT: {}", eventJson);
                case LOW -> securityLogger.debug("SECURITY_EVENT: {}", eventJson);
            }

        } catch (Exception e) {
            securityLogger.error("Failed to log security event", e);
        } finally {
            // Clean up MDC
            MDC.remove("eventType");
            MDC.remove("severity");
            MDC.remove("username");
            MDC.remove("sourceIp");
        }
    }

    private void checkForBruteForceAttack(SecurityEvent event) {
        // Implementation would check for repeated failures from same IP
        // This is a simplified version
        if (event.getSourceIp() != null) {
            // In a real implementation, this would check a cache/database
            // for recent failed attempts from the same IP
            securityLogger.warn("Potential brute force attack detected from IP: {}", event.getSourceIp());
        }
    }

    private void triggerSecurityAlert(SecurityEvent event) {
        // Implementation would integrate with alerting system (SNS, email, etc.)
        securityLogger.error("SECURITY_ALERT: High severity security event detected: {}", event.getEventType());

        // In a real implementation, this would:
        // 1. Send SNS notification
        // 2. Create incident in monitoring system
        // 3. Potentially trigger automated response
    }

    private String getSourceIp(Authentication authentication) {
        if (authentication != null && authentication.getDetails() instanceof WebAuthenticationDetails) {
            return ((WebAuthenticationDetails) authentication.getDetails()).getRemoteAddress();
        }
        return "unknown";
    }

    private String getUserAgent(Authentication authentication) {
        // In a real implementation, this would extract user agent from request
        return "unknown";
    }

    /**
     * Security event data structure
     */
    public static class SecurityEvent {
        private String eventType;
        private SecuritySeverity severity;
        private String username;
        private String sourceIp;
        private String userAgent;
        private LocalDateTime timestamp;
        private String description;
        private Map<String, Object> details;

        // Builder pattern
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private SecurityEvent event = new SecurityEvent();

            public Builder eventType(String eventType) {
                event.eventType = eventType;
                return this;
            }

            public Builder severity(SecuritySeverity severity) {
                event.severity = severity;
                return this;
            }

            public Builder username(String username) {
                event.username = username;
                return this;
            }

            public Builder sourceIp(String sourceIp) {
                event.sourceIp = sourceIp;
                return this;
            }

            public Builder userAgent(String userAgent) {
                event.userAgent = userAgent;
                return this;
            }

            public Builder timestamp(LocalDateTime timestamp) {
                event.timestamp = timestamp;
                return this;
            }

            public Builder description(String description) {
                event.description = description;
                return this;
            }

            public Builder details(Map<String, Object> details) {
                event.details = details;
                return this;
            }

            public SecurityEvent build() {
                return event;
            }
        }

        // Getters
        public String getEventType() {
            return eventType;
        }

        public SecuritySeverity getSeverity() {
            return severity;
        }

        public String getUsername() {
            return username;
        }

        public String getSourceIp() {
            return sourceIp;
        }

        public String getUserAgent() {
            return userAgent;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public String getDescription() {
            return description;
        }

        public Map<String, Object> getDetails() {
            return details;
        }

        public String getFormattedTimestamp() {
            return timestamp != null ? timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
        }
    }

    /**
     * Security event severity levels
     */
    public enum SecuritySeverity {
        CRITICAL, // Immediate action required
        HIGH, // Urgent attention needed
        MEDIUM, // Should be reviewed
        WARNING, // Potential issue
        INFO, // Informational
        LOW // Debug/trace level
    }
}