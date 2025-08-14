package solid.humank.genaidemo.domain.common.context;

/** 上下文之間的關係定義 用於描述不同界限上下文之間的關係 */
public record ContextRelation(
        String sourceContext, String targetContext, RelationType type, String description) {
    /**
     * 建立一個新的上下文關係
     *
     * @param sourceContext 來源上下文
     * @param targetContext 目標上下文
     * @param type 關係類型
     * @param description 關係描述
     */
    public ContextRelation {
        if (sourceContext == null || sourceContext.isBlank()) {
            throw new IllegalArgumentException("來源上下文不能為空");
        }
        if (targetContext == null || targetContext.isBlank()) {
            throw new IllegalArgumentException("目標上下文不能為空");
        }
        if (type == null) {
            throw new IllegalArgumentException("關係類型不能為空");
        }
    }

    /**
     * 建立一個新的上下文關係（不帶描述）
     *
     * @param sourceContext 來源上下文
     * @param targetContext 目標上下文
     * @param type 關係類型
     */
    public ContextRelation(String sourceContext, String targetContext, RelationType type) {
        this(sourceContext, targetContext, type, "");
    }

    @Override
    public String toString() {
        String relation =
                switch (type) {
                    case UPSTREAM_DOWNSTREAM -> sourceContext + " 是 " + targetContext + " 的上游";
                    case SHARED_KERNEL -> sourceContext + " 和 " + targetContext + " 共享核心元件";
                    case ANTI_CORRUPTION_LAYER, ANTICORRUPTION_LAYER ->
                            sourceContext + " 通過防腐層使用 " + targetContext;
                    case CUSTOMER_SUPPLIER -> sourceContext + " 是 " + targetContext + " 的供應商";
                    case CONFORMIST -> sourceContext + " 遵從 " + targetContext + " 的模型";
                };

        if (description != null && !description.isBlank()) {
            relation += " - " + description;
        }

        return relation;
    }
}
