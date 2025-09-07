package solid.humank.genaidemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
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

    /**
     * Test profile configuration properties
     */
    @Bean
    @Profile("test")
    public ProfileConfigurationProperties testProfileConfigurationProperties() {
        return ProfileConfigurationProperties.testDefaults();
    }

    /**
     * Development profile configuration properties
     */
    @Bean
    @Profile({"dev", "development"})
    public ProfileConfigurationProperties developmentProfileConfigurationProperties() {
        return ProfileConfigurationProperties.developmentDefaults();
    }

    /**
     * Production profile configuration properties
     */
    @Bean
    @Profile({"prod", "production"})
    public ProfileConfigurationProperties productionProfileConfigurationProperties() {
        return ProfileConfigurationProperties.productionDefaults();
    }

    /**
     * Default profile configuration properties (fallback)
     */
    @Bean
    @Profile("default")
    public ProfileConfigurationProperties defaultProfileConfigurationProperties() {
        return ProfileConfigurationProperties.developmentDefaults();
    }

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