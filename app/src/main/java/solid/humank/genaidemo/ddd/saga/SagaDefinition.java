package solid.humank.genaidemo.ddd.saga;

import java.util.ArrayList;
import java.util.List;
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

    public SagaDefinition(T context) {
        this.steps = new ArrayList<>();
        this.context = context;
    }

    /**
     * 添加一個 Saga 步驟
     * 
     * @param name 步驟名稱
     * @param action 執行動作
     * @param compensation 補償動作
     * @return 當前 Saga 定義實例，用於鏈式調用
     */
    public SagaDefinition<T> step(String name, Consumer<T> action, Consumer<T> compensation) {
        steps.add(new SagaStep<>(name, action, compensation));
        return this;
    }

    /**
     * 執行 Saga
     * 
     * @throws SagaExecutionException 如果執行過程中發生錯誤
     */
    public void execute() {
        List<SagaStep<T>> completedSteps = new ArrayList<>();

        try {
            // 執行所有步驟
            for (SagaStep<T> step : steps) {
                step.execute(context);
                completedSteps.add(step);
            }
        } catch (Exception e) {
            // 如果有任何步驟失敗，執行補償操作
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
        // 反向執行補償操作
        for (int i = completedSteps.size() - 1; i >= 0; i--) {
            try {
                completedSteps.get(i).compensate(context);
            } catch (Exception e) {
                // 記錄補償操作失敗，但繼續執行其他補償
                LOGGER.log(Level.SEVERE, 
                    "Compensation failed for step: " + completedSteps.get(i).getName(), e);
            }
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
     */
    public SagaStep(String name, Consumer<T> action, Consumer<T> compensation) {
        this.name = name;
        this.action = action;
        this.compensation = compensation;
    }

    /**
     * 執行步驟
     * 
     * @param context 上下文
     */
    public void execute(T context) {
        action.accept(context);
    }

    /**
     * 執行補償操作
     * 
     * @param context 上下文
     */
    public void compensate(T context) {
        if (compensation != null) {
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
