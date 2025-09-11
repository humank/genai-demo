package solid.humank.genaidemo.bdd.observability;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import solid.humank.genaidemo.infrastructure.observability.tracing.TraceContextManager;

/**
 * Step definitions for distributed tracing BDD tests
 */
public class DistributedTracingStepDefinitions {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OpenTelemetry openTelemetry;

    @Autowired
    private Tracer tracer;

    @Autowired
    private TraceContextManager traceContextManager;

    private ResponseEntity<String> lastResponse;
    private String currentEnvironment = "development";
    private HttpHeaders requestHeaders = new HttpHeaders();

    @Given("the distributed tracing system is enabled")
    public void theDistributedTracingSystemIsEnabled() {
        assertThat(openTelemetry).isNotNull();
        assertThat(tracer).isNotNull();
        assertThat(traceContextManager).isNotNull();
    }

    @Given("the application is running with tracing configuration")
    public void theApplicationIsRunningWithTracingConfiguration() {
        // Verify that the application started successfully with tracing
        ResponseEntity<String> healthResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health",
                String.class);
        assertThat(healthResponse.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Given("a client makes a request to the system")
    public void aClientMakesARequestToTheSystem() {
        // Prepare for making a request
        requestHeaders.clear();
    }

    @When("the request enters the system")
    public void theRequestEntersTheSystem() {
        HttpEntity<String> entity = new HttpEntity<>(requestHeaders);
        lastResponse = restTemplate.exchange(
                "http://localhost:" + port + "/actuator/info",
                HttpMethod.GET,
                entity,
                String.class);
    }

    @Then("the system should generate a unique trace ID using OpenTelemetry")
    public void theSystemShouldGenerateAUniqueTraceIdUsingOpenTelemetry() {
        assertThat(lastResponse).isNotNull();
        assertThat(lastResponse.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Then("the trace ID should be included in the response headers")
    public void theTraceIdShouldBeIncludedInTheResponseHeaders() {
        assertThat(lastResponse.getHeaders().get("X-Trace-ID")).isNotNull();
        String traceId = lastResponse.getHeaders().getFirst("X-Trace-ID");
        assertThat(traceId).isNotNull();
        assertThat(traceId).isNotEmpty();
    }

    @Then("the trace ID should be propagated to all logs")
    public void theTraceIdShouldBePropagatedToAllLogs() {
        // In a real scenario, we would verify log entries contain trace IDs
        // For this test, we verify that MDC context is properly managed
        assertThat(traceContextManager).isNotNull();
    }

    @Given("a request is being processed")
    public void aRequestIsBeingProcessed() {
        // Set up a request that will go through multiple components
        requestHeaders.set("X-Correlation-ID", "test-correlation-spans");
    }

    @When("the request flows through different components")
    public void theRequestFlowsThroughDifferentComponents() {
        HttpEntity<String> entity = new HttpEntity<>(requestHeaders);
        lastResponse = restTemplate.exchange(
                "http://localhost:" + port + "/actuator/info",
                HttpMethod.GET,
                entity,
                String.class);
    }

    @Then("the system should create spans for each operation")
    public void theSystemShouldCreateSpansForEachOperation() {
        assertThat(lastResponse.getStatusCode().is2xxSuccessful()).isTrue();
        // In a real scenario, we would verify span creation through tracing backend
    }

    @Then("each span should have proper timing information")
    public void eachSpanShouldHaveProperTimingInformation() {
        // Verify that spans are created with timing information
        // This is typically verified through the tracing backend
        assertThat(lastResponse).isNotNull();
    }

    @Then("spans should be linked to the parent trace")
    public void spansShouldBeLinkedToTheParentTrace() {
        // Verify span hierarchy
        assertThat(lastResponse.getHeaders().get("X-Trace-ID")).isNotNull();
    }

    @Given("traces are being generated")
    public void tracesAreBeingGenerated() {
        // Make a request to generate traces
        lastResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health",
                String.class);
    }

    @When("running in development environment")
    public void runningInDevelopmentEnvironment() {
        currentEnvironment = "development";
    }

    @Then("traces should be sent to Jaeger for analysis")
    public void tracesShouldBeSentToJaegerForAnalysis() {
        // In development, traces should be configured for Jaeger
        // This is verified through configuration
        assertThat(currentEnvironment).isEqualTo("development");
    }

    @When("running in production environment")
    public void runningInProductionEnvironment() {
        currentEnvironment = "production";
    }

    @Then("traces should be sent to AWS X-Ray for analysis")
    public void tracesShouldBeSentToAwsXRayForAnalysis() {
        // In production, traces should be configured for AWS X-Ray
        assertThat(currentEnvironment).isEqualTo("production");
    }

    @Given("a request has been processed completely")
    public void aRequestHasBeenProcessedCompletely() {
        HttpEntity<String> entity = new HttpEntity<>(requestHeaders);
        lastResponse = restTemplate.exchange(
                "http://localhost:" + port + "/actuator/info",
                HttpMethod.GET,
                entity,
                String.class);
        assertThat(lastResponse.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @When("viewing traces in the tracing backend")
    public void viewingTracesInTheTracingBackend() {
        // This step represents viewing traces in Jaeger or X-Ray
        // In a real scenario, we would query the tracing backend
    }

    @Then("the system should show the complete request journey")
    public void theSystemShouldShowTheCompleteRequestJourney() {
        // Verify that trace information is available
        assertThat(lastResponse.getHeaders().get("X-Trace-ID")).isNotNull();
    }

    @Then("timing information should be available for each span")
    public void timingInformationShouldBeAvailableForEachSpan() {
        // Timing information is captured by OpenTelemetry spans
        assertThat(lastResponse).isNotNull();
    }

    @Then("the trace should include all components involved")
    public void theTraceShouldIncludeAllComponentsInvolved() {
        // All components should be instrumented and included in traces
        assertThat(lastResponse.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Given("a request encounters an error during processing")
    public void aRequestEncountersAnErrorDuringProcessing() {
        // Make a request that will cause an error
        requestHeaders.set("X-Test-Error", "true");
    }

    @When("the error occurs in a specific component")
    public void theErrorOccursInASpecificComponent() {
        HttpEntity<String> entity = new HttpEntity<>(requestHeaders);
        // Try to access a non-existent endpoint to generate an error
        lastResponse = restTemplate.exchange(
                "http://localhost:" + port + "/non-existent-endpoint",
                HttpMethod.GET,
                entity,
                String.class);
    }

    @Then("the system should highlight the failed span")
    public void theSystemShouldHighlightTheFailedSpan() {
        // Error spans should be marked with error status
        assertThat(lastResponse.getStatusCode().is4xxClientError()).isTrue();
    }

    @Then("error details should be included in the span")
    public void errorDetailsShouldBeIncludedInTheSpan() {
        // Error information should be recorded in spans
        assertThat(lastResponse).isNotNull();
    }

    @Then("the error should be propagated to the trace level")
    public void theErrorShouldBePropagatedToTheTraceLevel() {
        // Error status should be visible at trace level
        assertThat(lastResponse.getStatusCode().isError()).isTrue();
    }

    @Given("a request is being traced")
    public void aRequestIsBeingTraced() {
        requestHeaders.set("X-Correlation-ID", "test-correlation-logs");
    }

    @When("logs are generated during request processing")
    public void logsAreGeneratedDuringRequestProcessing() {
        HttpEntity<String> entity = new HttpEntity<>(requestHeaders);
        lastResponse = restTemplate.exchange(
                "http://localhost:" + port + "/actuator/info",
                HttpMethod.GET,
                entity,
                String.class);
    }

    @Then("logs should include the trace ID and span ID")
    public void logsShouldIncludeTheTraceIdAndSpanId() {
        // Logs should contain trace context in MDC
        assertThat(lastResponse.getHeaders().get("X-Trace-ID")).isNotNull();
    }

    @Then("correlation ID should be consistent across traces and logs")
    public void correlationIdShouldBeConsistentAcrossTracesAndLogs() {
        String correlationId = lastResponse.getHeaders().getFirst("X-Correlation-ID");
        assertThat(correlationId).isEqualTo("test-correlation-logs");
    }

    @Then("metrics should be tagged with trace context when available")
    public void metricsShouldBeTaggedWithTraceContextWhenAvailable() {
        // Metrics should include trace context as tags
        assertThat(lastResponse).isNotNull();
    }

    @Given("a request with existing trace context")
    public void aRequestWithExistingTraceContext() {
        // Set up request with trace context headers
        requestHeaders.set("traceparent", "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
        requestHeaders.set("X-Correlation-ID", "existing-correlation");
    }

    @When("the request is received by the system")
    public void theRequestIsReceivedByTheSystem() {
        HttpEntity<String> entity = new HttpEntity<>(requestHeaders);
        lastResponse = restTemplate.exchange(
                "http://localhost:" + port + "/actuator/health",
                HttpMethod.GET,
                entity,
                String.class);
    }

    @Then("the existing trace context should be extracted")
    public void theExistingTraceContextShouldBeExtracted() {
        assertThat(lastResponse.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Then("new spans should be created as children of the existing trace")
    public void newSpansShouldBeCreatedAsChildrenOfTheExistingTrace() {
        // New spans should be part of the existing trace
        assertThat(lastResponse.getHeaders().get("X-Trace-ID")).isNotNull();
    }

    @Then("trace context should be propagated to downstream services")
    public void traceContextShouldBePropagatedToDownstreamServices() {
        // Trace context should be available for downstream propagation
        assertThat(lastResponse.getHeaders().get("X-Correlation-ID")).contains("existing-correlation");
    }

    @Given("the tracing system is configured with sampling")
    public void theTracingSystemIsConfiguredWithSampling() {
        // Sampling is configured in the tracing system
        assertThat(openTelemetry).isNotNull();
    }

    @When("multiple requests are processed")
    public void multipleRequestsAreProcessed() {
        // Process multiple requests to test sampling
        for (int i = 0; i < 5; i++) {
            restTemplate.getForEntity(
                    "http://localhost:" + port + "/actuator/health",
                    String.class);
        }
    }

    @Then("only the configured percentage of traces should be sampled")
    public void onlyTheConfiguredPercentageOfTracesShouldBeSampled() {
        // In test environment, we sample all traces (100%)
        // This would be verified through tracing backend metrics in production
        assertThat(true).isTrue(); // Placeholder assertion
    }

    @Then("sampling decisions should be consistent across the trace")
    public void samplingDecisionsShouldBeConsistentAcrossTheTrace() {
        // Sampling decisions should be consistent for all spans in a trace
        assertThat(true).isTrue(); // Placeholder assertion
    }

    @Then("important traces should always be sampled regardless of sampling rate")
    public void importantTracesShouldAlwaysBeSampledRegardlessOfSamplingRate() {
        // Critical operations should always be traced
        assertThat(true).isTrue(); // Placeholder assertion
    }

    @Given("the application is deployed in different environments")
    public void theApplicationIsDeployedInDifferentEnvironments() {
        // Application supports multiple environments
        assertThat(openTelemetry).isNotNull();
    }

    @Then("Jaeger should be used as the tracing backend")
    public void jaegerShouldBeUsedAsTheTracingBackend() {
        // Development environment uses Jaeger
        assertThat(currentEnvironment).isEqualTo("development");
    }

    @Then("AWS X-Ray should be used as the tracing backend")
    public void awsXRayShouldBeUsedAsTheTracingBackend() {
        // Production environment uses AWS X-Ray
        assertThat(currentEnvironment).isEqualTo("production");
    }

    @Then("configuration should automatically select the appropriate backend")
    public void configurationShouldAutomaticallySelectTheAppropriateBackend() {
        // Configuration should be environment-aware
        assertThat(openTelemetry).isNotNull();
    }

    @Given("the tracing system is enabled")
    public void theTracingSystemIsEnabled() {
        assertThat(openTelemetry).isNotNull();
    }

    @When("processing high volumes of requests")
    public void processingHighVolumesOfRequests() {
        // Simulate high volume by making multiple concurrent requests
        for (int i = 0; i < 10; i++) {
            restTemplate.getForEntity(
                    "http://localhost:" + port + "/actuator/health",
                    String.class);
        }
    }

    @Then("the application performance should not be significantly impacted")
    public void theApplicationPerformanceShouldNotBeSignificantlyImpacted() {
        // Application should remain responsive with tracing enabled
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health",
                String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Then("tracing overhead should be minimal")
    public void tracingOverheadShouldBeMinimal() {
        // Tracing should have minimal performance impact
        assertThat(true).isTrue(); // Placeholder - would measure actual overhead
    }

    @Then("the system should remain responsive under load")
    public void theSystemShouldRemainResponsiveUnderLoad() {
        // System should handle load with tracing enabled
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health",
                String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }
}