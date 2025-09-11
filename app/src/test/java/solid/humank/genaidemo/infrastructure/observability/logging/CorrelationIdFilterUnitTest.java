package solid.humank.genaidemo.infrastructure.observability.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 輕量級單元測試 - CorrelationIdFilter
 * 
 * 記憶體使用：~5MB (vs @SpringBootTest ~500MB)
 * 執行時間：~100ms (vs @SpringBootTest ~2s)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Correlation ID Filter Unit Tests")
class CorrelationIdFilterUnitTest {

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
        correlationIdFilter.doFilterInternal(request, response, filterChain);

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
        correlationIdFilter.doFilterInternal(request, response, filterChain);

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
        correlationIdFilter.doFilterInternal(request, response, filterChain);

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
        correlationIdFilter.doFilterInternal(request, response, filterChain);

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
        correlationIdFilter.doFilterInternal(request, response, filterChain);

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
            correlationIdFilter.doFilterInternal(request, response, filterChain);
        } catch (ServletException e) {
            // Expected exception
        }

        // MDC should be cleared even after exception
        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("Should handle null correlation ID gracefully")
    void shouldHandleNullCorrelationIdGracefully() throws ServletException, IOException {
        // Given
        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .thenReturn(null);

        // When
        correlationIdFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader(eq(CorrelationIdFilter.CORRELATION_ID_HEADER), any(String.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should handle invalid correlation ID format")
    void shouldHandleInvalidCorrelationIdFormat() throws ServletException, IOException {
        // Given
        String invalidCorrelationId = "invalid-format";
        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .thenReturn(invalidCorrelationId);

        // When
        correlationIdFilter.doFilterInternal(request, response, filterChain);

        // Then - 應該使用提供的值，即使格式無效
        verify(response).setHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, invalidCorrelationId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should validate filter behavior with different header values")
    void shouldValidateFilterBehaviorWithDifferentHeaderValues() throws ServletException, IOException {
        // Test case 1: Valid UUID
        String validUuid = UUID.randomUUID().toString();
        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .thenReturn(validUuid);

        correlationIdFilter.doFilterInternal(request, response, filterChain);
        verify(response).setHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, validUuid);

        // Reset mocks for next test
        org.mockito.Mockito.reset(response, filterChain);

        // Test case 2: Custom string
        String customString = "custom-correlation-123";
        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .thenReturn(customString);

        correlationIdFilter.doFilterInternal(request, response, filterChain);
        verify(response).setHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, customString);
    }

    @Test
    @DisplayName("Should verify MDC cleanup in all scenarios")
    void shouldVerifyMdcCleanupInAllScenarios() throws ServletException, IOException {
        // Given
        String correlationId = "test-correlation-cleanup";
        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .thenReturn(correlationId);

        // When
        correlationIdFilter.doFilterInternal(request, response, filterChain);

        // Then - MDC should be clean after processing
        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isNull();
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }
}