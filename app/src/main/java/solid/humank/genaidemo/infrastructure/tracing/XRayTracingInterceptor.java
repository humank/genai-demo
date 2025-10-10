package solid.humank.genaidemo.infrastructure.tracing;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * X-Ray Tracing Interceptor for RestTemplate
 * 
 * This interceptor automatically creates subsegments for outgoing HTTP requests
 * made via RestTemplate, enabling distributed tracing across services.
 * 
 * Features:
 * - Automatic subsegment creation for HTTP calls
 * - Captures request/response metadata
 * - Propagates trace context via HTTP headers
 * - Records errors and exceptions
 * 
 * @author GenAI Demo Team
 * @since 1.0
 */
@Component
public class XRayTracingInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {
        
        // Create subsegment for this HTTP call
        Subsegment subsegment = AWSXRay.beginSubsegment(request.getURI().getHost());
        
        try {
            // Add request metadata
            subsegment.putHttp("request", Map.of(
                "method", request.getMethod().name(),
                "url", request.getURI().toString()
            ));
            
            // Add trace header to propagate context
            String traceHeader = AWSXRay.getTraceEntity().getTraceId().toString();
            request.getHeaders().add("X-Amzn-Trace-Id", traceHeader);
            
            // Execute the request
            ClientHttpResponse response = execution.execute(request, body);
            
            // Add response metadata
            subsegment.putHttp("response", Map.of(
                "status", response.getStatusCode().value(),
                "content_length", response.getHeaders().getContentLength()
            ));
            
            // Mark as error if status >= 400
            if (response.getStatusCode().is4xxClientError() || 
                response.getStatusCode().is5xxServerError()) {
                subsegment.setError(true);
                if (response.getStatusCode().is5xxServerError()) {
                    subsegment.setFault(true);
                }
            }
            
            return response;
            
        } catch (IOException e) {
            // Record exception
            subsegment.addException(e);
            subsegment.setError(true);
            subsegment.setFault(true);
            throw e;
        } finally {
            // Always end the subsegment
            AWSXRay.endSubsegment();
        }
    }
}
