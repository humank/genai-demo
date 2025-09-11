package solid.humank.genaidemo.infrastructure.security;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Custom Logback pattern layout that masks PII information in log messages
 * Implements requirement 11.1: WHEN logs contain sensitive data THEN the system
 * SHALL mask or encrypt PII information
 */
@Component
public class PiiMaskingPatternLayout extends PatternLayout {

    // PII patterns to mask
    private static final Pattern EMAIL_PATTERN = Pattern.compile("([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "\\b\\d{3}-\\d{3}-\\d{4}\\b|\\b\\d{10}\\b|\\b\\+\\d{1,3}[\\s-]?\\d{3,4}[\\s-]?\\d{3,4}[\\s-]?\\d{3,4}\\b");
    private static final Pattern CREDIT_CARD_PATTERN = Pattern
            .compile("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b");
    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern ADDRESS_PATTERN = Pattern.compile(
            "\\b\\d+\\s+[A-Za-z\\s]+(?:Street|St|Avenue|Ave|Road|Rd|Boulevard|Blvd|Lane|Ln|Drive|Dr|Court|Ct|Place|Pl)\\b",
            Pattern.CASE_INSENSITIVE);

    // Sensitive field names in JSON logs
    private static final Pattern SENSITIVE_JSON_FIELDS = Pattern.compile(
            "\"(password|creditCard|ssn|socialSecurityNumber|bankAccount|routingNumber)\"\\s*:\\s*\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE);

    @Override
    public String doLayout(ILoggingEvent event) {
        String originalMessage = super.doLayout(event);
        return maskPiiInformation(originalMessage);
    }

    /**
     * Masks PII information in log messages
     */
    private String maskPiiInformation(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        String maskedMessage = message;

        // Mask email addresses
        maskedMessage = maskPattern(maskedMessage, EMAIL_PATTERN, this::maskEmail);

        // Mask phone numbers
        maskedMessage = maskPattern(maskedMessage, PHONE_PATTERN, this::maskPhone);

        // Mask credit card numbers
        maskedMessage = maskPattern(maskedMessage, CREDIT_CARD_PATTERN, this::maskCreditCard);

        // Mask SSN
        maskedMessage = maskPattern(maskedMessage, SSN_PATTERN, this::maskSsn);

        // Mask addresses
        maskedMessage = maskPattern(maskedMessage, ADDRESS_PATTERN, this::maskAddress);

        // Mask sensitive JSON fields
        maskedMessage = maskSensitiveJsonFields(maskedMessage);

        return maskedMessage;
    }

    private String maskPattern(String message, Pattern pattern, MaskingFunction maskingFunction) {
        Matcher matcher = pattern.matcher(message);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String replacement = maskingFunction.mask(matcher.group());
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex > 0) {
            String username = email.substring(0, atIndex);
            String domain = email.substring(atIndex);

            if (username.length() <= 2) {
                return "**" + domain;
            } else {
                return username.substring(0, 2) + "***" + domain;
            }
        }
        return "***@***.***";
    }

    private String maskPhone(String phone) {
        // Keep first 3 digits, mask the rest
        String digitsOnly = phone.replaceAll("[^\\d]", "");
        if (digitsOnly.length() >= 10) {
            return digitsOnly.substring(0, 3) + "-***-****";
        }
        return "***-***-****";
    }

    private String maskCreditCard(String creditCard) {
        String digitsOnly = creditCard.replaceAll("[^\\d]", "");
        if (digitsOnly.length() >= 4) {
            return "****-****-****-" + digitsOnly.substring(digitsOnly.length() - 4);
        }
        return "****-****-****-****";
    }

    private String maskSsn(String ssn) {
        return "***-**-****";
    }

    private String maskAddress(String address) {
        // Keep first word (house number), mask the rest
        String[] parts = address.split("\\s+", 2);
        if (parts.length > 1) {
            return parts[0] + " *** *** ***";
        }
        return "*** *** ***";
    }

    private String maskSensitiveJsonFields(String message) {
        Matcher matcher = SENSITIVE_JSON_FIELDS.matcher(message);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String fieldName = matcher.group(1);
            String replacement = "\"" + fieldName + "\":\"***MASKED***\"";
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    @FunctionalInterface
    private interface MaskingFunction {
        String mask(String input);
    }
}