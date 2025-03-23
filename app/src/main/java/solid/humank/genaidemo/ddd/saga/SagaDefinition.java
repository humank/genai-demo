package solid.humank.genaidemo.ddd.saga;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Saga 定義
 * 用於協調長時間運行的業務流程
 * @param <T> Saga 的上下文類型
 */
public class SagaDefinition<T> {
    private final List<SagaStep<T>> steps;
    private final T context;

    public SagaDefinition(T context) {
        this.steps = new ArrayList<>();
        this.context = context;
    }

    /**
     * 添加一個 Saga 步驟
     */
    public SagaDefinition<T> step(String name, Consumer<T> action, Consumer<T> compensation) {
        steps.add(new SagaStep<>(name, action, compensation));
        return this;
    }

    /**
     * 執行 Saga
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
     */
    private void compensate(List<SagaStep<T>> completedSteps) {
        // 反向執行補償操作
        for (int i = completedSteps.size() - 1; i >= 0; i--) {
            try {
                completedSteps.get(i).compensate(context);
            } catch (Exception e) {
                // 記錄補償操作失敗，但繼續執行其他補償
                
                System.err.println("Compensation failed for step: " + 
                    completedSteps.get(i).getName() + ": " + e.getMessage());
            }
        }
    }
}

/**
 * Saga 步驟定義
 */
class SagaStep<T> {
    private final String name;
    private final Consumer<T> action;
    private final Consumer<T> compensation;

    public SagaStep(String name, Consumer<T> action, Consumer<T> compensation) {
        this.name = name;
        this.action = action;
        this.compensation = compensation;
    }

    public void execute(T context) {
        action.accept(context);
    }

    public void compensate(T context) {
        if (compensation != null) {
            compensation.accept(context);
        }
    }

    public String getName() {
        return name;
    }
}

/**
 * Saga 執行異常
 */
class SagaExecutionException extends RuntimeException {
    public SagaExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
