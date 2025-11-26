package solid.humank.genaidemo.infrastructure.observability.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Test class for CorrelationIdFilter to verify proper correlation ID handling
 * in HTTP requests and MDC context management.
 */
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private CorrelationIdFilter correlationIdFilter;

    @BeforeEach
    void setUp() {
        correlationIdFilter = new CorrelationIdFilter();
        MDC.clear(); // Clean MDC before each test
    }

    @Test
    @DisplayName("Should use existing correlation ID from request header")
    void shouldUseExistingCorrelationIdFromHeader() throws ServletException, IOException {
        // Given
        String existingCorrelationId = UUID.randomUUID().toString();
        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .thenReturn(existingCorrelationId);

        // When
        correlationIdFilter.doFilterInternal(java.util.Objects.requireNonNull(request),
                java.util.Objects.requireNonNull(response), java.util.Objects.requireNonNull(filterChain));

        // Then
        verify(response).setHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, existingCorrelationId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should generate new correlation ID when not provided in header")
    void shouldGenerateNewCorrelationIdWhenNotProvided() throws ServletException, IOException {
        // Given
        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .thenReturn(null);

        // When
        correlationIdFilter.doFilterInternal(java.util.Objects.requireNonNull(request),
                java.util.Objects.requireNonNull(response), java.util.Objects.requireNonNull(filterChain));

        // Then
        verify(response).setHeader(eq(CorrelationIdFilter.CORRELATION_ID_HEADER), any(String.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should generate new correlation ID when header is empty")
    void shouldGenerateNewCorrelationIdWhenHeaderIsEmpty() throws ServletException, IOException {
        // Given
        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .thenReturn("");

        // When
        correlationIdFilter.doFilterInternal(java.util.Objects.requireNonNull(request),
                java.util.Objects.requireNonNull(response), java.util.Objects.requireNonNull(filterChain));

        // Then
        verify(response).setHeader(eq(CorrelationIdFilter.CORRELATION_ID_HEADER), any(String.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should generate new correlation ID when header is whitespace")
    void shouldGenerateNewCorrelationIdWhenHeaderIsWhitespace() throws ServletException, IOException {
        // Given
        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .thenReturn("   ");

        // When
        correlationIdFilter.doFilterInternal(java.util.Objects.requireNonNull(request),
                java.util.Objects.requireNonNull(response), java.util.Objects.requireNonNull(filterChain));

        // Then
        verify(response).setHeader(eq(CorrelationIdFilter.CORRELATION_ID_HEADER), any(String.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should set correlation ID in MDC during request processing")
    void shouldSetCorrelationIdInMdcDuringRequestProcessing() throws ServletException, IOException {
        // Given
        String correlationId = UUID.randomUUID().toString();
        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .thenReturn(correlationId);

        // Mock filter chain to capture MDC state during processing
        doAnswer(invocation -> {
            // Verify MDC is set during request processing
            assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY))
                    .isEqualTo(correlationId);
            return null;
        }).when(filterChain).doFilter(request, response);

        // When
        correlationIdFilter.doFilterInternal(java.util.Objects.requireNonNull(request),
                java.util.Objects.requireNonNull(response), java.util.Objects.requireNonNull(filterChain));

        // Then
        verify(filterChain).doFilter(request, response);
        // MDC should be cleared after request processing
        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("Should clear MDC even when exception occurs")
    void shouldClearMdcEvenWhenExceptionOccurs() throws ServletException, IOException {
        // Given
        String correlationId = UUID.randomUUID().toString();
        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .thenReturn(correlationId);

        // Mock filter chain to throw exception
        doThrow(new ServletException("Test exception"))
                .when(filterChain).doFilter(request, response);

        // When & Then
        try {
            correlationIdFilter.doFilterInternal(java.util.Objects.requireNonNull(request),
                    java.util.Objects.requireNonNull(response), java.util.Objects.requireNonNull(filterChain));
        } catch (ServletException e) {
            // Expected exception
        }

        // MDC should be cleared even after exception
        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("Should handle multiple concurrent requests with different correlation IDs")
    void shouldHandleMultipleConcurrentRequests() throws ServletException, IOException {
        // This test simulates concurrent request processing
        String correlationId1 = "correlation-1";
        String correlationId2 = "correlation-2";

        // Create separate filter instances to simulate concurrent processing
        CorrelationIdFilter filter1 = new CorrelationIdFilter();
        CorrelationIdFilter filter2 = new CorrelationIdFilter();

        HttpServletRequest request1 = mock(HttpServletRequest.class);
        HttpServletResponse response1 = mock(HttpServletResponse.class);
        FilterChain filterChain1 = mock(FilterChain.class);

        HttpServletRequest request2 = mock(HttpServletRequest.class);
        HttpServletResponse response2 = mock(HttpServletResponse.class);
        FilterChain filterChain2 = mock(FilterChain.class);

        when(request1.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .thenReturn(correlationId1);
        when(request2.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .thenReturn(correlationId2);

        // When - simulate concurrent processing
        Thread thread1 = new Thread(() -> {
            try {
                filter1.doFilterInternal(java.util.Objects.requireNonNull(request1),
                        java.util.Objects.requireNonNull(response1), java.util.Objects.requireNonNull(filterChain1));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                filter2.doFilterInternal(java.util.Objects.requireNonNull(request2),
                        java.util.Objects.requireNonNull(response2), java.util.Objects.requireNonNull(filterChain2));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        thread1.start();
        thread2.start();

        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Test interrupted", e);
        }

        // Then
        verify(response1).setHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId1);
        verify(response2).setHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId2);
        verify(filterChain1).doFilter(request1, response1);
        verify(filterChain2).doFilter(request2, response2);
    }
}