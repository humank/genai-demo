package solid.humank.genaidemo.infrastructure.analytics;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Configuration properties for analytics pipeline integration.
 * 
 * These properties are populated from application configuration files and
 * CDK stack outputs to configure the analytics pipeline integration.
 */
@ConfigurationProperties(prefix = "analytics")
@Validated
public record AnalyticsProperties(
        /**
         * Whether analytics is enabled for this environment.
         */
        @NotNull Boolean enabled,

        /**
         * Kinesis Data Firehose configuration.
         */
        @NotNull FirehoseConfig firehose,

        /**
         * Data lake configuration.
         */
        @NotNull DataLakeConfig dataLake,

        /**
         * AWS Glue configuration.
         */
        @NotNull GlueConfig glue,

        /**
         * QuickSight configuration.
         */
        @NotNull QuickSightConfig quicksight) {

    /**
     * Kinesis Data Firehose configuration.
     */
    public record FirehoseConfig(
            /**
             * Name of the Firehose delivery stream for domain events.
             */
            @NotBlank String streamName,

            /**
             * Batch size for sending events to Firehose.
             */
            Integer batchSize,

            /**
             * Maximum wait time in milliseconds before sending a batch.
             */
            Integer maxWaitTimeMs) {
        public FirehoseConfig {
            // Set defaults if not provided
            if (batchSize == null) {
                batchSize = 100;
            }
            if (maxWaitTimeMs == null) {
                maxWaitTimeMs = 5000;
            }
        }
    }

    /**
     * Data lake configuration.
     */
    public record DataLakeConfig(
            /**
             * Name of the S3 bucket used as data lake.
             */
            @NotBlank String bucketName,

            /**
             * Prefix for domain events in the data lake.
             */
            String eventPrefix) {
        public DataLakeConfig {
            if (eventPrefix == null) {
                eventPrefix = "domain-events/";
            }
        }
    }

    /**
     * AWS Glue configuration.
     */
    public record GlueConfig(
            /**
             * Name of the Glue database.
             */
            @NotBlank String databaseName,

            /**
             * Name of the Glue crawler.
             */
            String crawlerName) {
    }

    /**
     * QuickSight configuration.
     */
    public record QuickSightConfig(
            /**
             * QuickSight data source ID.
             */
            @NotBlank String dataSourceId,

            /**
             * Executive dashboard ID.
             */
            String executiveDashboardId,

            /**
             * Operations dashboard ID.
             */
            String operationsDashboardId) {
    }
}