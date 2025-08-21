package solid.humank.genaidemo.infrastructure.aop;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import solid.humank.genaidemo.application.common.service.DomainEventApplicationService;
import solid.humank.genaidemo.domain.common.aggregate.AggregateRootInterface;

/** 領域事件發布 AOP 攔截器 在 Application Service 方法執行成功後自動發布領域事件 確保事件發布在事務提交之前完成 */
@Aspect
@Component
@Order(100) // 確保在事務攔截器之後執行
public class DomainEventPublishingAspect {

    private static final Logger logger = LoggerFactory.getLogger(DomainEventPublishingAspect.class);

    private final DomainEventApplicationService domainEventApplicationService;

    public DomainEventPublishingAspect(
            DomainEventApplicationService domainEventApplicationService) {
        this.domainEventApplicationService = domainEventApplicationService;
    }

    /** 攔截所有 Application Service 的 @Transactional 方法 在方法成功執行後自動發布領域事件 */
    @AfterReturning(pointcut = "@annotation(org.springframework.transaction.annotation.Transactional) && "
            + "execution(* solid.humank.genaidemo.application.*.service.*.*(..))", returning = "result")
    public void publishDomainEventsAfterTransactionalMethod(JoinPoint joinPoint, Object result) {
        try {
            Method method = getMethod(joinPoint);
            String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();

            logger.debug("檢查方法 {} 是否需要發布領域事件", methodName);

            // 檢查方法是否標記為只讀事務
            Transactional transactional = method.getAnnotation(Transactional.class);
            if (transactional != null && transactional.readOnly()) {
                logger.debug("方法 {} 是只讀事務，跳過事件發布", methodName);
                return;
            }

            // 從方法參數和返回值中收集聚合根
            List<AggregateRootInterface> aggregateRoots = collectAggregateRoots(joinPoint.getArgs(), result);

            if (!aggregateRoots.isEmpty()) {
                logger.debug("從方法 {} 收集到 {} 個聚合根，準備發布事件", methodName, aggregateRoots.size());
                domainEventApplicationService.publishEventsFromAggregates(aggregateRoots);
                logger.debug("完成方法 {} 的事件發布", methodName);
            } else {
                logger.debug("方法 {} 沒有找到聚合根，無需發布事件", methodName);
            }

        } catch (Exception e) {
            logger.error("自動發布領域事件時發生錯誤: {}", e.getMessage(), e);
            // 不重新拋出異常，避免影響業務流程
        }
    }

    /** 從方法參數和返回值中收集聚合根實例 */
    private List<AggregateRootInterface> collectAggregateRoots(Object[] args, Object result) {
        List<AggregateRootInterface> aggregateRoots = new ArrayList<>();

        // 從方法參數中收集聚合根
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof AggregateRootInterface) {
                    aggregateRoots.add((AggregateRootInterface) arg);
                }
            }
        }

        // 從返回值中收集聚合根
        if (result instanceof AggregateRootInterface) {
            aggregateRoots.add((AggregateRootInterface) result);
        }

        return aggregateRoots;
    }

    /** 獲取被攔截的方法 */
    private Method getMethod(JoinPoint joinPoint) {
        try {
            String methodName = joinPoint.getSignature().getName();
            Class<?>[] parameterTypes = ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature())
                    .getParameterTypes();
            return joinPoint.getTarget().getClass().getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("無法獲取被攔截的方法", e);
        }
    }
}
