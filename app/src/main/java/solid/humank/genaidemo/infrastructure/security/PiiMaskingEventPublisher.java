package solid.humank.genaidemo.infrastructure.security;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import solid.humank.genaidemo.domain.common.event.DomainEvent;

/**
 * Component responsible for masking PII information in domain events before
 * publishing
 * Implements requirement 11.1: WHEN logs contain sensitive data THEN the system
 * SHALL mask or encrypt PII information
 */
@Component
public class PiiMaskingEventPublisher {

    private final ObjectMapper objectMapper;

    // Sensitive field names that should be masked
    private static final Set<String> SENSITIVE_FIELDS = new HashSet<>(Arrays.asList(
            "password", "creditCard", "ssn", "socialSecurityNumber", "bankAccount",
            "routingNumber", "pin", "cvv", "securityCode", "personalId", "passport"));

    // Email pattern for masking
    private static final Pattern EMAIL_PATTERN = Pattern.compile("([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");

    public PiiMaskingEventPublisher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Masks PII information in domain events before publishing
     */
    public DomainEvent maskPiiInEvent(DomainEvent event) {
        try {
            // Convert event to JSON
            String eventJson = objectMapper.writeValueAsString(event);
            JsonNode eventNode = objectMapper.readTree(eventJson);

            // Mask sensitive fields
            JsonNode maskedNode = maskSensitiveFields((ObjectNode) eventNode);

            // Convert back to event object
            return objectMapper.treeToValue(maskedNode, event.getClass());

        } catch (Exception e) {
            // If masking fails, log error but don't break the flow
            // Return original event to maintain system stability
            return event;
        }
    }

    private JsonNode maskSensitiveFields(ObjectNode node) {
        node.fieldNames().forEachRemaining(fieldName -> {
            JsonNode fieldValue = node.get(fieldName);

            if (fieldValue.isTextual()) {
                String textValue = fieldValue.asText();

                // Check if field name is sensitive
                if (isSensitiveField(fieldName)) {
                    node.put(fieldName, "***MASKED***");
                }
                // Check if value contains email pattern
                else if (EMAIL_PATTERN.matcher(textValue).find()) {
                    node.put(fieldName, maskEmail(textValue));
                }
                // Check if value looks like a phone number
                else if (isPhoneNumber(textValue)) {
                    node.put(fieldName, maskPhoneNumber(textValue));
                }
            } else if (fieldValue.isObject()) {
                // Recursively mask nested objects
                maskSensitiveFields((ObjectNode) fieldValue);
            } else if (fieldValue.isArray()) {
                // Handle arrays
                for (int i = 0; i < fieldValue.size(); i++) {
                    JsonNode arrayElement = fieldValue.get(i);
                    if (arrayElement.isObject()) {
                        maskSensitiveFields((ObjectNode) arrayElement);
                    }
                }
            }
        });

        return node;
    }

    private boolean isSensitiveField(String fieldName) {
        return SENSITIVE_FIELDS.stream()
                .anyMatch(sensitiveField -> fieldName.toLowerCase().contains(sensitiveField.toLowerCase()));
    }

    private String maskEmail(String email) {
        return EMAIL_PATTERN.matcher(email).replaceAll(matchResult -> {
            String username = matchResult.group(1);
            String domain = matchResult.group(2);

            if (username.length() <= 2) {
                return "**@" + domain;
            } else {
                return username.substring(0, 2) + "***@" + domain;
            }
        });
    }

    private boolean isPhoneNumber(String value) {
        // Simple phone number detection
        String digitsOnly = value.replaceAll("[^\\d]", "");
        return digitsOnly.length() >= 10 && digitsOnly.length() <= 15;
    }

    private String maskPhoneNumber(String phone) {
        String digitsOnly = phone.replaceAll("[^\\d]", "");
        if (digitsOnly.length() >= 10) {
            return digitsOnly.substring(0, 3) + "-***-****";
        }
        return "***-***-****";
    }
}