package solid.humank.genaidemo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Profile-specific configuration
 * Provides profile-specific beans and configuration properties
 */
@Configuration
public class ProfileConfiguration {

    private final Environment environment;

    public ProfileConfiguration(Environment environment) {
        this.environment = environment;
    }

    // Profile configuration properties are now handled by
    // ProfileConfigurationResolver
    // to avoid bean conflicts and provide better conflict resolution

    /**
     * Profile validation configuration methods
     */
    public boolean isTestProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("test".equals(profile)) {
                return true;
            }
        }
        return false;
    }

    public boolean isDevelopmentProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("dev".equals(profile) || "development".equals(profile)) {
                return true;
            }
        }
        return false;
    }

    public boolean isProductionProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("production".equals(profile) || "prod".equals(profile)) {
                return true;
            }
        }
        return false;
    }
}