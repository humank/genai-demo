package solid.humank.genaidemo.infrastructure.event.monitor;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.common.event.DomainEvent;

/** 領域事件監控切面 用於監控和記錄事件處理的性能和狀態 */
@Aspect
@Component
public class DomainEventMonitor {
    private static final Logger LOGGER = Logger.getLogger(DomainEventMonitor.class.getName());

    /**
     * 監控使用@EventSubscriber註解的方法
     *
     * @param joinPoint 切入點
     * @return 方法執行結果
     * @throws Throwable 如果方法執行失敗
     */
    @Around("@annotation(solid.humank.genaidemo.domain.common.event.EventSubscriber)")
    public Object monitorEventHandler(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if (args.length == 0 || !(args[0] instanceof DomainEvent)) {
            return joinPoint.proceed();
        }

        DomainEvent event = (DomainEvent) args[0];
        String eventType = event.getEventType();
        String handlerName = joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "."
                + joinPoint.getSignature().getName();

        LOGGER.info(() -> String.format("開始處理事件: %s, 處理器: %s", eventType, handlerName));

        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            LOGGER.info(
                    () -> String.format(
                            "事件處理完成: %s, 處理器: %s, 耗時: %d ms",
                            eventType, handlerName, duration));

            return result;
        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - startTime;
            LOGGER.log(
                    Level.SEVERE,
                    String.format(
                            "事件處理失敗: %s, 處理器: %s, 耗時: %d ms, 錯誤: %s",
                            eventType, handlerName, duration, e.getMessage()),
                    e);
            throw e;
        }
    }
}