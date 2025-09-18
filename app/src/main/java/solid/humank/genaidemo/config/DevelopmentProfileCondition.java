package solid.humank.genaidemo.config;

import java.util.Arrays;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition for Development Profile Bean Creation
 * 
 * This condition ensures that development profile beans are only created when
 * the development profile is active and there are no conflicting profiles.
 */
public class DevelopmentProfileCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String[] activeProfiles = context.getEnvironment().getActiveProfiles();

        // Check if development profile is active
        boolean developmentProfileActive = Arrays.stream(activeProfiles)
                .anyMatch(profile -> "dev".equals(profile) || "development".equals(profile));

        if (!developmentProfileActive) {
            return false;
        }

        // Check for conflicting profiles
        boolean hasProductionProfile = Arrays.stream(activeProfiles)
                .anyMatch(profile -> "prod".equals(profile) || "production".equals(profile));

        boolean hasTestProfile = Arrays.asList(activeProfiles).contains("test");

        if (hasProductionProfile) {
            // Development + Production is not allowed
            return false;
        }

        if (hasTestProfile) {
            // Development + Test is allowed but test takes precedence
            // So we don't create development beans when test is active
            return false;
        }

        // Development profile is active and no conflicts detected
        return true;
    }
}