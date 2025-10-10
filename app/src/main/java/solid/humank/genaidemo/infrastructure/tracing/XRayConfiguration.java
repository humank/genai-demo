package solid.humank.genaidemo.infrastructure.tracing;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.jakarta.servlet.AWSXRayServletFilter;
import com.amazonaws.xray.plugins.EKSPlugin;
import com.amazonaws.xray.strategy.sampling.LocalizedSamplingStrategy;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;

import java.net.URL;

/**
 * AWS X-Ray Configuration for Distributed Tracing
 * 
 * This configuration enables X-Ray tracing for the GenAI Demo application.
 * It integrates with the X-Ray daemon running as a DaemonSet in the EKS cluster.
 * 
 * Features:
 * - Automatic trace collection for HTTP requests
 * - EKS plugin for container metadata
 * - Custom sampling rules for cost optimization
 * - Integration with Spring Boot Actuator
 * 
 * @author GenAI Demo Team
 * @since 1.0
 */
@Configuration
@Profile({"staging", "production"})
public class XRayConfiguration {

    /**
     * Configure AWS X-Ray Recorder with EKS plugin and custom sampling rules
     */
    @Bean
    public AWSXRayRecorderBuilder xrayRecorderBuilder() {
        // Load sampling rules from classpath or use default
        URL samplingRulesUrl = getClass().getClassLoader().getResource("xray-sampling-rules.json");
        
        AWSXRayRecorderBuilder builder = AWSXRayRecorderBuilder.standard()
            .withPlugin(new EKSPlugin());  // Add EKS metadata to traces
        
        if (samplingRulesUrl != null) {
            builder.withSamplingStrategy(new LocalizedSamplingStrategy(samplingRulesUrl));
        }
        
        // Build and set as global recorder
        AWSXRay.setGlobalRecorder(builder.build());
        
        return builder;
    }

    /**
     * Register X-Ray Servlet Filter to trace HTTP requests
     * 
     * This filter automatically creates segments for incoming HTTP requests
     * and captures request/response metadata.
     */
    @Bean
    public FilterRegistrationBean<AWSXRayServletFilter> xrayServletFilter() {
        FilterRegistrationBean<AWSXRayServletFilter> registrationBean = new FilterRegistrationBean<>();
        
        // Create filter with segment naming strategy
        AWSXRayServletFilter filter = new AWSXRayServletFilter("genai-demo-backend");
        
        registrationBean.setFilter(filter);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registrationBean.setName("XRayServletFilter");
        
        return registrationBean;
    }
}
