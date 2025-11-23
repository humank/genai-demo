package solid.humank.genaidemo.infrastructure.common.persistence;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import solid.humank.genaidemo.infrastructure.common.persistence.AuroraReadWriteConfiguration.DataSourceRouter;

/**
 * 只讀操作切面
 * 
 * 自動處理 @ReadOnlyOperation 註解的方法，將其路由到讀取數據源
 * 
 * 處理邏輯：
 * 1. 檢查方法是否標記為只讀操作
 * 2. 根據事務狀態決定是否使用讀取數據源
 * 3. 執行操作並恢復原始數據源設定
 * 4. 記錄數據源路由信息
 * 
 * 建立日期: 2025年9月24日 上午10:18 (台北時間)
 * 需求: 1.1 - 並發控制機制全面重構
 * 
 * @author Kiro AI Assistant
 * @since 1.0
 */
@Aspect
@Component
@Order(1) // 確保在事務切面之前執行
@ConditionalOnProperty(name = "aurora.read-write-separation.enabled", havingValue = "true", matchIfMissing = false)
public class ReadOnlyOperationAspect {    private static final Logger logger = LoggerFactory.getLogger(ReadOnlyOperationAspect.class);

    /**
     * 處理標記為 @ReadOnlyOperation 的方法
     * 
     * @param joinPoint 連接點
     * @param readOnlyOperation 只讀操作註解
     * @return 方法執行結果
     * @throws Throwable 方法執行異常
     */
    @Around("@annotation(readOnlyOperation)")
    public Object handleReadOnlyOperation(ProceedingJoinPoint joinPoint, ReadOnlyOperation readOnlyOperation) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        
        // 檢查是否應該使用讀取數據源
        boolean shouldUseReadDataSource = shouldUseReadDataSource(readOnlyOperation);
        
        if (shouldUseReadDataSource) {
            logger.debug("Routing method {} to read data source", methodName);
            return DataSourceRouter.executeWithReadDataSource(() -> {
                try {
                    return joinPoint.proceed();
                } catch (Throwable throwable) {
                    throw new RuntimeException("Error executing read-only operation", throwable);
                }
            });
        } else {
            logger.debug("Method {} will use default data source routing", methodName);
            return joinPoint.proceed();
        }
    }

    /**
     * 處理類級別的 @ReadOnlyOperation 註解
     * 
     * @param joinPoint 連接點
     * @param readOnlyOperation 只讀操作註解
     * @return 方法執行結果
     * @throws Throwable 方法執行異常
     */
    @Around("@within(readOnlyOperation) && !@annotation(solid.humank.genaidemo.infrastructure.common.persistence.ReadOnlyOperation)")
    public Object handleClassLevelReadOnlyOperation(ProceedingJoinPoint joinPoint, ReadOnlyOperation readOnlyOperation) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        
        // 檢查是否應該使用讀取數據源
        boolean shouldUseReadDataSource = shouldUseReadDataSource(readOnlyOperation);
        
        if (shouldUseReadDataSource) {
            logger.debug("Routing class-level read-only method {} to read data source", methodName);
            return DataSourceRouter.executeWithReadDataSource(() -> {
                try {
                    return joinPoint.proceed();
                } catch (Throwable throwable) {
                    throw new RuntimeException("Error executing read-only operation", throwable);
                }
            });
        } else {
            logger.debug("Class-level read-only method {} will use default data source routing", methodName);
            return joinPoint.proceed();
        }
    }

    /**
     * 判斷是否應該使用讀取數據源
     * 
     * @param readOnlyOperation 只讀操作註解
     * @return 是否應該使用讀取數據源
     */
    private boolean shouldUseReadDataSource(ReadOnlyOperation readOnlyOperation) {
        // 如果強制使用讀取數據源，直接返回 true
        if (readOnlyOperation.forceReadDataSource()) {
            logger.debug("Force using read data source as specified in annotation");
            return true;
        }

        // 如果在活動事務中，不使用讀取數據源（保持一致性）
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            logger.debug("Active transaction detected, will not use read data source");
            return false;
        }

        // 如果是只讀事務，可以使用讀取數據源
        if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            logger.debug("Read-only transaction detected, using read data source");
            return true;
        }

        // 默認情況下，只讀操作使用讀取數據源
        logger.debug("No active transaction, using read data source for read-only operation");
        return true;
    }
}