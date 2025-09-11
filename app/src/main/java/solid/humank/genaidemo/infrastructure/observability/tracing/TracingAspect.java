package solid.humank.genaidemo.infrastructure.observability.tracing;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

/**
 * AOP aspect for automatic distributed tracing instrumentation
 * Creates spans for application services, domain services, and repository
 * operations
 */
@Aspect
@Component

@ConditionalOnProperty(name = "tracing.enabled", havingValue = "true", matchIfMissing = true)
public class TracingAspect {

    private static final Logger log = LoggerFactory.getLogger(TracingAspect.class);

    private final OpenTelemetry openTelemetry;
    private final TraceContextManager traceContextManager;
    private final Tracer tracer;

    public TracingAspect(OpenTelemetry openTelemetry, TraceContextManager traceContextManager) {
        this.openTelemetry = openTelemetry;
        this.traceContextManager = traceContextManager;
        this.tracer = openTelemetry.getTracer("genai-demo-tracer", "1.0.0");
    }

    /**
     * Traces application service methods
     */
    @Around("execution(* solid.humank.genaidemo.application..*(..))")
    public Object traceApplicationService(ProceedingJoinPoint joinPoint) throws Throwable {
        return createSpanAndExecute(joinPoint, "application-service", SpanKind.INTERNAL);
    }

    /**
     * Traces domain service methods
     */
    @Around("execution(* solid.humank.genaidemo.domain..service..*(..))")
    public Object traceDomainService(ProceedingJoinPoint joinPoint) throws Throwable {
        return createSpanAndExecute(joinPoint, "domain-service", SpanKind.INTERNAL);
    }

    /**
     * Traces repository operations
     */
    @Around("execution(* solid.humank.genaidemo.infrastructure..repository..*(..))")
    public Object traceRepository(ProceedingJoinPoint joinPoint) throws Throwable {
        return createSpanAndExecute(joinPoint, "repository", SpanKind.CLIENT);
    }

    /**
     * Traces REST controller methods
     */
    @Around("execution(* solid.humank.genaidemo.interfaces.web..*(..))")
    public Object traceController(ProceedingJoinPoint joinPoint) throws Throwable {
        return createSpanAndExecute(joinPoint, "http-controller", SpanKind.SERVER);
    }

    /**
     * Creates a span and executes the method within the span context
     */
    private Object createSpanAndExecute(ProceedingJoinPoint joinPoint, String operationType, SpanKind spanKind)
            throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        String spanName = String.format("%s.%s", className, methodName);

        Span span = tracer.spanBuilder(spanName)
                .setSpanKind(spanKind)
                .setAttribute("operation.type", operationType)
                .setAttribute("class.name", className)
                .setAttribute("method.name", methodName)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            // Update MDC with current trace context
            traceContextManager.updateMDCWithTraceContext();

            // Add method parameters as span attributes (be careful with sensitive data)
            addMethodParametersToSpan(span, joinPoint);

            // Record business operation context
            traceContextManager.recordBusinessOperation(operationType, methodName, extractEntityId(joinPoint));

            log.debug("Started span: {} for {}.{}", span.getSpanContext().getSpanId(), className, methodName);

            // Execute the actual method
            Object result = joinPoint.proceed();

            // Add result information to span if applicable
            addResultToSpan(span, result);

            span.setAttribute("success", true);
            return result;

        } catch (Throwable throwable) {
            // Record error in span and trace context
            span.setAttribute("success", false);
            traceContextManager.recordError(throwable, throwable.getMessage());

            log.debug("Error in span: {} for {}.{} - {}",
                    span.getSpanContext().getSpanId(), className, methodName, throwable.getMessage());

            throw throwable;
        } finally {
            span.end();
            log.debug("Ended span: {} for {}.{}", span.getSpanContext().getSpanId(), className, methodName);
        }
    }

    /**
     * Adds method parameters to span attributes (excluding sensitive data)
     */
    private void addMethodParametersToSpan(Span span, ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        String[] paramNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();

        if (args != null && paramNames != null && args.length == paramNames.length) {
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null && !isSensitiveParameter(paramNames[i])) {
                    String paramValue = args[i].toString();
                    // Limit parameter value length to avoid large spans
                    if (paramValue.length() > 100) {
                        paramValue = paramValue.substring(0, 100) + "...";
                    }
                    span.setAttribute("param." + paramNames[i], paramValue);
                }
            }
        }
    }

    /**
     * Checks if a parameter name indicates sensitive data
     */
    private boolean isSensitiveParameter(String paramName) {
        String lowerName = paramName.toLowerCase();
        return lowerName.contains("password") ||
                lowerName.contains("secret") ||
                lowerName.contains("token") ||
                lowerName.contains("key") ||
                lowerName.contains("credential");
    }

    /**
     * Adds result information to span
     */
    private void addResultToSpan(Span span, Object result) {
        if (result != null) {
            span.setAttribute("result.type", result.getClass().getSimpleName());

            // Add specific result information for common types
            if (result instanceof String) {
                String resultStr = (String) result;
                if (resultStr.length() <= 50) {
                    span.setAttribute("result.value", resultStr);
                }
            } else if (result instanceof Number) {
                span.setAttribute("result.value", result.toString());
            } else if (result instanceof Boolean) {
                span.setAttribute("result.value", result.toString());
            }
        }
    }

    /**
     * Extracts entity ID from method parameters for business context
     */
    private String extractEntityId(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        String[] paramNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();

        if (args != null && paramNames != null && args.length == paramNames.length) {
            for (int i = 0; i < args.length; i++) {
                String paramName = paramNames[i].toLowerCase();
                if (paramName.contains("id") && args[i] != null) {
                    return args[i].toString();
                }
            }
        }

        return null;
    }
}