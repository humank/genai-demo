package solid.humank.genaidemo.infrastructure.security;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Web Security Configuration
 * 
 * Configures security settings for different environments:
 * - Development/Test: Permissive settings for Swagger UI and actuator endpoints
 * - Production: Secure settings with proper authentication
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfiguration {

        /**
         * Security configuration for development and test environments
         * Allows access to Swagger UI, actuator endpoints, and H2 console
         */
        @Bean
        @Profile({ "development", "test", "test-minimal" })
        @Order(1)
        public SecurityFilterChain developmentSecurityFilterChain(HttpSecurity http) throws Exception {
                return http
                                .securityMatcher("/**")
                                .authorizeHttpRequests(authz -> authz
                                                // Allow access to Swagger UI and OpenAPI docs
                                                .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                                                "/v3/api-docs/**")
                                                .permitAll()
                                                // Allow access to actuator endpoints
                                                .requestMatchers(EndpointRequest.toAnyEndpoint()).permitAll()
                                                // Allow access to H2 console
                                                .requestMatchers("/h2-console/**").permitAll()
                                                // Allow access to static resources
                                                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**")
                                                .permitAll()
                                                // Allow access to error pages
                                                .requestMatchers("/error").permitAll()
                                                // Allow access to API endpoints for testing
                                                .requestMatchers("/api/**").permitAll()
                                                // Require authentication for all other requests
                                                .anyRequest().authenticated())
                                .csrf(AbstractHttpConfigurer::disable)
                                .headers(headers -> headers.frameOptions().sameOrigin()) // For H2 console
                                .build();
        }

        /**
         * Security configuration for production environment
         * Requires authentication for most endpoints
         */
        @Bean
        @Profile("production")
        @Order(2)
        public SecurityFilterChain productionSecurityFilterChain(HttpSecurity http) throws Exception {
                return http
                                .securityMatcher("/**")
                                .authorizeHttpRequests(authz -> authz
                                                // Allow access to health check endpoints (for load balancers)
                                                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                                                // Allow access to Prometheus metrics (should be secured at network
                                                // level)
                                                .requestMatchers("/actuator/prometheus").permitAll()
                                                // Allow access to static resources
                                                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**")
                                                .permitAll()
                                                // Allow access to error pages
                                                .requestMatchers("/error").permitAll()
                                                // Require authentication for Swagger UI in production
                                                .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                                                "/v3/api-docs/**")
                                                .authenticated()
                                                // Require authentication for sensitive actuator endpoints
                                                .requestMatchers(EndpointRequest.toAnyEndpoint()).authenticated()
                                                // Require authentication for API endpoints
                                                .requestMatchers("/api/**").authenticated()
                                                // Require authentication for all other requests
                                                .anyRequest().authenticated())
                                .httpBasic(httpBasic -> {
                                }) // Enable basic authentication for production
                                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for API endpoints
                                .build();
        }

        /**
         * Default security configuration (fallback)
         * Used when no specific profile is active
         */
        @Bean
        @Order(3)
        public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
                return http
                                .authorizeHttpRequests(authz -> authz
                                                // Allow access to health check endpoints
                                                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                                                // Allow access to static resources
                                                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**")
                                                .permitAll()
                                                // Allow access to error pages
                                                .requestMatchers("/error").permitAll()
                                                // Require authentication for all other requests
                                                .anyRequest().authenticated())
                                .httpBasic(httpBasic -> {
                                }) // Enable basic authentication
                                .csrf(AbstractHttpConfigurer::disable)
                                .build();
        }
}