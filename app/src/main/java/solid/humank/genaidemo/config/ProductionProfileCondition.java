package solid.humank.genaidemo.config;

import java.util.Arrays;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.lang.NonNull;

/**
 * Condition for Production Profile Bean Creation
 *
 * This condition ensures that production profile beans are only created when
 * the production profile is active and there are no conflicting profiles.
 */
public class ProductionProfileCondition implements Condition {

    @Override
    public boolean matches(@NonNull ConditionContext context, @NonNull AnnotatedTypeMetadata metadata) {
        String[] activeProfiles = context.getEnvironment().getActiveProfiles();

        // Check if production profile is active
        boolean productionProfileActive = Arrays.stream(activeProfiles)
                .anyMatch(profile -> "prod".equals(profile) || "production".equals(profile));

        if (!productionProfileActive) {
            return false;
        }

        // Check for conflicting profiles - production should be isolated
        boolean hasTestProfile = Arrays.asList(activeProfiles).contains("test");
        boolean hasDevelopmentProfile = Arrays.stream(activeProfiles)
                .anyMatch(profile -> "dev".equals(profile) || "development".equals(profile));

        // Production cannot be combined with test or development profiles
        return !(hasTestProfile || hasDevelopmentProfile);
    }
}
