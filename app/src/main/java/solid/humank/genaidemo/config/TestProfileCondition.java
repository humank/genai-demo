package solid.humank.genaidemo.config;

import java.util.Arrays;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition for Test Profile Bean Creation
 * 
 * This condition ensures that test profile beans are only created when
 * the test profile is active and there are no conflicting profiles.
 */
public class TestProfileCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String[] activeProfiles = context.getEnvironment().getActiveProfiles();

        // Check if test profile is active
        boolean testProfileActive = Arrays.asList(activeProfiles).contains("test");

        if (!testProfileActive) {
            return false;
        }

        // Check for conflicting profiles
        boolean hasProductionProfile = Arrays.stream(activeProfiles)
                .anyMatch(profile -> "prod".equals(profile) || "production".equals(profile));

        if (hasProductionProfile) {
            // Test + Production is not allowed
            return false;
        }

        // Test profile is active and no conflicts detected
        return true;
    }
}