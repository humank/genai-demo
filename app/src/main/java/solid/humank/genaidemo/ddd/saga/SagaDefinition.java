package solid.humank.genaidemo.ddd.saga;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Saga 定義
 * 用於協調長時間運行的業務流程
 * @param <T> Saga 的上下文類型
 */
public class SagaDefinition<T> {
    private static final Logger LOGGER = Logger.getLogger(SagaDefinition.class.getName());
    
    private final List<SagaStep<T>> steps;
    private final T context;

    /**
     * 創建一個 Saga 定義
     * 
     * @param context Saga 上下文
     * @throws IllegalArgumentException 如果上下文為 null
     */
    public SagaDefinition(T context) {
        this.context = Objects.requireNonNull(context, "Saga context cannot be null");
        this.steps = new ArrayList<>();
    }

    /**
     * 添加一個 Saga 步驟
     * 
     * @param name 步驟名稱
     * @param action 執行動作
     * @param compensation 補償動作
     * @return 當前 Saga 定義實例，用於鏈式調用
     * @throws IllegalArgumentException 如果名稱或執行動作為 null
     */
    public SagaDefinition<T> step(String name, Consumer<T> action, Consumer<T> compensation) {
        Objects.requireNonNull(name, "Step name cannot be null");
        Objects.requireNonNull(action, "Step action cannot be null");
        
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(String.format("Adding step: %s", name));
        }
        
        steps.add(new SagaStep<>(name, action, compensation));
        return this;
    }

    /**
     * 執行 Saga
     * 
     * @throws SagaExecutionException 如果執行過程中發生錯誤
     */
    public void execute() {
        if (steps.isEmpty()) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning("Executing Saga with no steps defined");
            }
            return;
        }
        
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info(String.format("Executing Saga with %d steps", steps.size()));
        }
        
        List<SagaStep<T>> completedSteps = new ArrayList<>();

        try {
            // 執行所有步驟
            for (SagaStep<T> step : steps) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(String.format("Executing step: %s", step.getName()));
                }
                
                step.execute(context);
                completedSteps.add(step);
                
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(String.format("Step completed: %s", step.getName()));
                }
            }
            
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.info("Saga execution completed successfully");
            }
        } catch (Exception e) {
            // 如果有任何步驟失敗，執行補償操作
            LOGGER.log(Level.SEVERE, String.format("Saga execution failed: %s", e.getMessage()), e);
            compensate(completedSteps);
            throw new SagaExecutionException("Saga execution failed", e);
        }
    }

    /**
     * 執行補償操作
     * 
     * @param completedSteps 已完成的步驟列表
     */
    private void compensate(List<SagaStep<T>> completedSteps) {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info(String.format("Starting compensation for %d steps", completedSteps.size()));
        }
        
        // 反向執行補償操作
        for (int i = completedSteps.size() - 1; i >= 0; i--) {
            SagaStep<T> step = completedSteps.get(i);
            String stepName = step.getName();
            
            try {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(String.format("Compensating step: %s", stepName));
                }
                
                step.compensate(context);
                
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(String.format("Compensation completed for step: %s", stepName));
                }
            } catch (Exception e) {
                // 記錄補償操作失敗，但繼續執行其他補償
                LOGGER.log(Level.SEVERE, String.format("Compensation failed for step: %s", stepName), e);
            }
        }
        
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("Compensation completed");
        }
    }
}

/**
 * Saga 步驟定義
 * 
 * @param <T> 上下文類型
 */
class SagaStep<T> {
    private final String name;
    private final Consumer<T> action;
    private final Consumer<T> compensation;

    /**
     * 創建一個 Saga 步驟
     * 
     * @param name 步驟名稱
     * @param action 執行動作
     * @param compensation 補償動作
     * @throws IllegalArgumentException 如果名稱或執行動作為 null
     */
    public SagaStep(String name, Consumer<T> action, Consumer<T> compensation) {
        this.name = Objects.requireNonNull(name, "Step name cannot be null");
        this.action = Objects.requireNonNull(action, "Step action cannot be null");
        this.compensation = compensation; // 補償動作可以為 null
    }

    /**
     * 執行步驟
     * 
     * @param context 上下文
     * @throws NullPointerException 如果上下文為 null
     */
    public void execute(T context) {
        Objects.requireNonNull(context, "Context cannot be null");
        action.accept(context);
    }

    /**
     * 執行補償操作
     * 
     * @param context 上下文
     * @throws NullPointerException 如果上下文為 null 且補償動作不為 null
     */
    public void compensate(T context) {
        if (compensation != null) {
            Objects.requireNonNull(context, "Context cannot be null");
            compensation.accept(context);
        }
    }

    /**
     * 獲取步驟名稱
     * 
     * @return 步驟名稱
     */
    public String getName() {
        return name;
    }
}

/**
 * Saga 執行異常
 */
class SagaExecutionException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    /**
     * 創建一個 Saga 執行異常
     * 
     * @param message 錯誤訊息
     * @param cause 原因
     */
    public SagaExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
