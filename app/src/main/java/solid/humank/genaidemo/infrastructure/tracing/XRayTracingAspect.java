package solid.humank.genaidemo.infrastructure.tracing;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * X-Ray Tracing Aspect for Service Layer
 * 
 * This aspect automatically creates subsegments for service methods,
 * enabling detailed tracing of business logic execution.
 * 
 * Features:
 * - Automatic subsegment creation for @Service methods
 * - Captures method parameters and return values
 * - Records execution time
 * - Captures exceptions
 * 
 * Usage:
 * - Annotate service classes with @Service
 * - Methods will be automatically traced
 * 
 * @author GenAI Demo Team
 * @since 1.0
 */
@Aspect
@Component
public class XRayTracingAspect {

    /**
     * Trace all methods in classes annotated with @Service
     */
    @Around("@within(org.springframework.stereotype.Service)")
    public Object traceServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        
        // Create subsegment for this service method
        Subsegment subsegment = AWSXRay.beginSubsegment(className + "." + methodName);
        
        try {
            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("class", className);
            metadata.put("method", methodName);
            
            // Add parameter names and values (be careful with sensitive data)
            String[] paramNames = signature.getParameterNames();
            Object[] paramValues = joinPoint.getArgs();
            
            if (paramNames != null && paramNames.length > 0) {
                Map<String, String> params = new HashMap<>();
                for (int i = 0; i < paramNames.length; i++) {
                    // Only add non-sensitive parameters
                    if (!isSensitiveParameter(paramNames[i])) {
                        params.put(paramNames[i], String.valueOf(paramValues[i]));
                    }
                }
                metadata.put("parameters", params);
            }
            
            subsegment.putMetadata("service", "method_info", metadata);
            
            // Execute the method
            long startTime = System.currentTimeMillis();
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Add execution time
            subsegment.putAnnotation("execution_time_ms", executionTime);
            
            // Add result type (not the actual value to avoid large traces)
            if (result != null) {
                subsegment.putAnnotation("result_type", result.getClass().getSimpleName());
            }
            
            return result;
            
        } catch (Throwable throwable) {
            // Record exception
            subsegment.addException(throwable);
            subsegment.setError(true);
            subsegment.setFault(true);
            throw throwable;
        } finally {
            // Always end the subsegment
            AWSXRay.endSubsegment();
        }
    }

    /**
     * Check if a parameter name indicates sensitive data
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
     * Trace repository methods for database operations
     */
    @Around("@within(org.springframework.stereotype.Repository)")
    public Object traceRepositoryMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        
        // Create subsegment for this repository method
        Subsegment subsegment = AWSXRay.beginSubsegment("Database::" + className + "." + methodName);
        subsegment.setNamespace("remote");  // Mark as remote call
        
        try {
            // Add metadata
            subsegment.putAnnotation("repository", className);
            subsegment.putAnnotation("operation", methodName);
            
            // Execute the method
            long startTime = System.currentTimeMillis();
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Add execution time
            subsegment.putAnnotation("query_time_ms", executionTime);
            
            // Warn if query is slow
            if (executionTime > 1000) {
                subsegment.putAnnotation("slow_query", true);
            }
            
            return result;
            
        } catch (Throwable throwable) {
            // Record exception
            subsegment.addException(throwable);
            subsegment.setError(true);
            subsegment.setFault(true);
            throw throwable;
        } finally {
            // Always end the subsegment
            AWSXRay.endSubsegment();
        }
    }
}
