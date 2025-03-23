package solid.humank.genaidemo.ddd.context;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Bounded Context Map
 * 用於定義和管理不同界限上下文之間的關係
 */
public class BoundedContextMap {
    private final Map<String, ContextRelation> contextRelations;

    public BoundedContextMap() {
        this.contextRelations = new HashMap<>();
    }

    /**
     * 添加上游下游關係
     * @param upstream 上游上下文
     * @param downstream 下游上下文
     */
    public void addUpstreamDownstream(String upstream, String downstream) {
        contextRelations.put(
            getRelationKey(upstream, downstream),
            new ContextRelation(upstream, downstream, RelationType.UPSTREAM_DOWNSTREAM)
        );
    }

    /**
     * 添加共享核心關係
     * @param context1 第一個上下文
     * @param context2 第二個上下文
     */
    public void addSharedKernel(String context1, String context2) {
        contextRelations.put(
            getRelationKey(context1, context2),
            new ContextRelation(context1, context2, RelationType.SHARED_KERNEL)
        );
    }

    /**
     * 添加防腐層關係
     * @param consumer 消費者上下文
     * @param supplier 供應者上下文
     */
    public void addAntiCorruptionLayer(String consumer, String supplier) {
        contextRelations.put(
            getRelationKey(consumer, supplier),
            new ContextRelation(consumer, supplier, RelationType.ANTI_CORRUPTION_LAYER)
        );
    }

    /**
     * 獲取兩個上下文之間的關係
     * @param context1 第一個上下文
     * @param context2 第二個上下文
     * @return 關係定義，如果不存在則返回空
     */
    public Optional<ContextRelation> getRelation(String context1, String context2) {
        return Optional.ofNullable(contextRelations.get(getRelationKey(context1, context2)));
    }

    private String getRelationKey(String context1, String context2) {
        return context1 + "-" + context2;
    }
}
