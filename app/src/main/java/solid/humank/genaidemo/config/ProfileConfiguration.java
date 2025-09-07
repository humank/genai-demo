package solid.humank.genaidemo.config;

import java.util.Arrays;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

/**
 * Profile Configuration and Validation
 * Validates active profiles and performs startup checks
 */
@Configuration
@EnableConfigurationProperties(ProfileConfigurationProperties.class)
public class ProfileConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ProfileConfiguration.class);
    private static final Set<String> VALID_PROFILES = Set.of("dev", "production", "test");
    private static final String DEFAULT_PROFILE = "dev";

    private final Environment environment;
    private final String applicationName;
    private final String applicationVersion;

    public ProfileConfiguration(Environment environment,
            @Value("${spring.application.name:genai-demo}") String applicationName,
            @Value("${info.app.version:2.0.0}") String applicationVersion) {
        this.environment = environment;
        this.applicationName = applicationName;
        this.applicationVersion = applicationVersion;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateProfilesOnStartup() {
        String[] activeProfiles = environment.getActiveProfiles();

        log.info("=== Application Startup Information ===");
        log.info("Application Name: {}", applicationName);
        log.info("Application Version: {}", applicationVersion);
        log.info("Active Profiles: {}", Arrays.toString(activeProfiles));

        if (activeProfiles.length == 0) {
            log.warn("No active profiles found, using default profile: {}", DEFAULT_PROFILE);
            return;
        }

        // Validate profiles
        for (String profile : activeProfiles) {
            if (!VALID_PROFILES.contains(profile) && !"openapi".equals(profile)) {
                throw new IllegalStateException(
                        String.format("Invalid profile '%s'. Valid profiles are: %s",
                                profile, VALID_PROFILES));
            }
        }

        log.info("Profile validation completed successfully");
        log.info("=======================================");
    }

    /**
     * Get the current active profile for conditional bean creation
     */
    public boolean isProductionProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("production");
    }

    public boolean isDevelopmentProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev") ||
                environment.getActiveProfiles().length == 0;
    }

    public boolean isTestProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("test");
    }
}