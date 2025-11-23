package solid.humank.genaidemo.interfaces.web;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import solid.humank.genaidemo.config.ProfileConfigurationProperties;

/**
 * REST controller for exposing profile information
 */
@RestController
@RequestMapping("/api/profile")
@Tag(name = "Profile", description = "Profile information and configuration")
public class ProfileInfoController {

    private final Environment environment;    private final ProfileConfigurationProperties profileProperties;

    public ProfileInfoController(Environment environment,
            ProfileConfigurationProperties profileProperties) {
        this.environment = environment;
        this.profileProperties = profileProperties;
    }

    @GetMapping("/info")
    @Operation(summary = "Get profile information", description = "Retrieve current active profiles and configuration details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile information retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ProfileInfoResponse getProfileInfo() {
        return new ProfileInfoResponse(
                Arrays.asList(environment.getActiveProfiles()),
                Arrays.asList(environment.getDefaultProfiles()),
                profileProperties.name(),
                profileProperties.description(),
                createFeaturesMap(),
                createDatabaseInfo(),
                createEventingInfo());
    }

    private Map<String, Boolean> createFeaturesMap() {
        Map<String, Boolean> features = new HashMap<>();
        ProfileConfigurationProperties.ProfileFeatures profileFeatures = profileProperties.features();
        features.put("h2Console", profileFeatures.h2Console());
        features.put("debugLogging", profileFeatures.debugLogging());
        features.put("inMemoryEvents", profileFeatures.inMemoryEvents());
        features.put("kafkaEvents", profileFeatures.kafkaEvents());
        return features;
    }

    private DatabaseInfo createDatabaseInfo() {
        String url = environment.getProperty("spring.datasource.url");
        String driverClass = environment.getProperty("spring.datasource.driver-class-name");
        String flywayLocations = environment.getProperty("spring.flyway.locations");

        return new DatabaseInfo(url, driverClass, flywayLocations);
    }

    private EventingInfo createEventingInfo() {
        String kafkaBootstrapServers = environment.getProperty("spring.kafka.bootstrap-servers");
        boolean kafkaEnabled = kafkaBootstrapServers != null && !kafkaBootstrapServers.trim().isEmpty();

        return new EventingInfo(
                profileProperties.features().inMemoryEvents(),
                kafkaEnabled,
                kafkaBootstrapServers);
    }

    public record ProfileInfoResponse(
            java.util.List<String> activeProfiles,
            java.util.List<String> defaultProfiles,
            String profileName,
            String profileDescription,
            Map<String, Boolean> features,
            DatabaseInfo database,
            EventingInfo eventing) {
    }

    public record DatabaseInfo(
            String url,
            String driverClass,
            String flywayLocations) {
    }

    public record EventingInfo(
            boolean inMemoryEvents,
            boolean kafkaEnabled,
            String kafkaBootstrapServers) {
    }
}