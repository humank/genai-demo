package solid.humank.genaidemo.infrastructure.saga.definition;

/**
 * Saga 定義接口
 * 定義 Saga 的執行和補償邏輯
 */
public interface SagaDefinition<T> {
    /**
     * 執行 Saga
     */
    void execute(T context);
    
    /**
     * 補償 Saga
     */
    void compensate(T context, Exception exception);
}