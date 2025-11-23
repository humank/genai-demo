package solid.humank.genaidemo.infrastructure.observability.logging;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AOP aspect that automatically adds business context to logging for domain
 * operations.
 * This aspect intercepts method calls in application services and domain
 * services
 * to automatically set appropriate MDC context.
 */
@Aspect
@Component
public class BusinessLoggingAspect {    private static final Logger logger = LoggerFactory.getLogger(BusinessLoggingAspect.class);
    private final LoggingContextManager loggingContextManager;

    public BusinessLoggingAspect(LoggingContextManager loggingContextManager) {
        this.loggingContextManager = loggingContextManager;
    }

    /**
     * Pointcut for all application service methods
     */
    @Pointcut("execution(* solid.humank.genaidemo.application..*(..))")
    public void applicationServiceMethods() {
    }

    /**
     * Pointcut for all domain service methods
     */
    @Pointcut("execution(* solid.humank.genaidemo.domain..service..*(..))")
    public void domainServiceMethods() {
    }

    /**
     * Around advice for application service methods to add operation context
     */
    @Around("applicationServiceMethods()")
    public Object aroundApplicationService(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String operation = className + "." + methodName;

        // Set operation context
        loggingContextManager.setOperation(operation);
        loggingContextManager.setComponent("application-service");

        long startTime = System.currentTimeMillis();

        try {
            logger.debug("Starting operation: {}", operation);
            Object result = joinPoint.proceed();

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Operation completed successfully: {} (duration: {}ms)", operation, duration);

            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Operation failed: {} (duration: {}ms) - Error: {}",
                    operation, duration, e.getMessage(), e);
            throw new RuntimeException("Operation failed", e);
        }
    }

    /**
     * Around advice for domain service methods to add domain context
     */
    @Around("domainServiceMethods()")
    public Object aroundDomainService(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String operation = className + "." + methodName;

        // Set domain context
        loggingContextManager.setOperation(operation);
        loggingContextManager.setComponent("domain-service");

        long startTime = System.currentTimeMillis();

        try {
            logger.debug("Starting domain operation: {}", operation);
            Object result = joinPoint.proceed();

            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Domain operation completed: {} (duration: {}ms)", operation, duration);

            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Domain operation failed: {} (duration: {}ms) - Error: {}",
                    operation, duration, e.getMessage(), e);
            throw new RuntimeException("Operation failed", e);
        }
    }

    /**
     * After throwing advice to log exceptions with context
     */
    @AfterThrowing(pointcut = "applicationServiceMethods() || domainServiceMethods()", throwing = "ex")
    public void logException(JoinPoint joinPoint, Throwable ex) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        logger.error("Exception in {}.{}: {} - Context: {}",
                className, methodName, ex.getMessage(),
                loggingContextManager.getCurrentContext(), ex);
    }
}