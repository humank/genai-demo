package solid.humank.genaidemo.ddd.context;

/**
 * 界限上下文之間的關係類型
 */
public enum RelationType {
    /**
     * 上游和下游關係
     * 表示一個上下文為另一個提供服務
     */
    UPSTREAM_DOWNSTREAM,

    /**
     * 共享核心
     * 表示多個上下文共享某些核心概念或組件
     */
    SHARED_KERNEL,

    /**
     * 防腐層
     * 表示通過轉換層來隔離不同上下文
     */
    ANTI_CORRUPTION_LAYER
}
