package solid.humank.genaidemo.domain.common.context;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 界限上下文映射 用於定義和管理不同界限上下文之間的關係 */
public class BoundedContextMap {    private static final Logger logger = LoggerFactory.getLogger(BoundedContextMap.class);
    
    private final Map<String, Map<String, ContextRelation>> contextRelations;

    public BoundedContextMap() {
        this.contextRelations = new HashMap<>();
        initializeContextMap();
    }

    private void initializeContextMap() {
        // Define all bounded contexts
        String[] contexts = {
            "Order",
            "Payment",
            "Inventory",
            "Delivery",
            "Notification",
            "Workflow",
            "Product",
            "Promotion",
            "Pricing",
            "Customer"
        };

        // Initialize the map for each context
        for (String context : contexts) {
            contextRelations.put(context, new HashMap<>());
        }

        // Define relationships between contexts
        addRelation(
                "Product",
                "Order",
                RelationType.UPSTREAM_DOWNSTREAM,
                "Product provides catalog information to Order");
        addRelation(
                "Product",
                "Inventory",
                RelationType.SHARED_KERNEL,
                "Product and Inventory share product definitions");
        addRelation(
                "Product",
                "Promotion",
                RelationType.UPSTREAM_DOWNSTREAM,
                "Product provides catalog information to Promotion");
        addRelation(
                "Product",
                "Pricing",
                RelationType.UPSTREAM_DOWNSTREAM,
                "Product provides base prices to Pricing");

        addRelation(
                "Promotion",
                "Pricing",
                RelationType.UPSTREAM_DOWNSTREAM,
                "Promotion provides discount rules to Pricing");
        addRelation(
                "Promotion",
                "Order",
                RelationType.UPSTREAM_DOWNSTREAM,
                "Promotion provides active promotions to Order");
        addRelation(
                "Promotion",
                "Customer",
                RelationType.CUSTOMER_SUPPLIER,
                "Promotion uses customer data for targeted promotions");

        addRelation(
                "Pricing",
                "Order",
                RelationType.UPSTREAM_DOWNSTREAM,
                "Pricing calculates final prices for Order");
        addRelation(
                "Pricing",
                "Payment",
                RelationType.UPSTREAM_DOWNSTREAM,
                "Pricing provides amount to be paid to Payment");

        addRelation(
                "Customer",
                "Order",
                RelationType.UPSTREAM_DOWNSTREAM,
                "Customer provides customer information to Order");
        addRelation(
                "Customer",
                "Payment",
                RelationType.UPSTREAM_DOWNSTREAM,
                "Customer provides payment preferences to Payment");

        addRelation(
                "Order",
                "Payment",
                RelationType.CUSTOMER_SUPPLIER,
                "Order requests payment processing from Payment");
        addRelation(
                "Order",
                "Inventory",
                RelationType.CUSTOMER_SUPPLIER,
                "Order requests inventory allocation from Inventory");
        addRelation(
                "Order",
                "Delivery",
                RelationType.CUSTOMER_SUPPLIER,
                "Order requests delivery from Delivery");

        addRelation(
                "Workflow",
                "Order",
                RelationType.CONFORMIST,
                "Workflow conforms to Order's domain model");
        addRelation(
                "Workflow",
                "Payment",
                RelationType.CONFORMIST,
                "Workflow conforms to Payment's domain model");
        addRelation(
                "Workflow",
                "Inventory",
                RelationType.CONFORMIST,
                "Workflow conforms to Inventory's domain model");
        addRelation(
                "Workflow",
                "Delivery",
                RelationType.CONFORMIST,
                "Workflow conforms to Delivery's domain model");

        addRelation(
                "Notification",
                "Order",
                RelationType.ANTI_CORRUPTION_LAYER,
                "Notification uses ACL to integrate with Order");
        addRelation(
                "Notification",
                "Customer",
                RelationType.ANTI_CORRUPTION_LAYER,
                "Notification uses ACL to integrate with Customer");
    }

    private void addRelation(
            String sourceContext, String targetContext, RelationType type, String description) {
        contextRelations
                .get(sourceContext)
                .put(
                        targetContext,
                        new ContextRelation(sourceContext, targetContext, type, description));
    }

    public ContextRelation getRelation(String sourceContext, String targetContext) {
        Map<String, ContextRelation> relations = contextRelations.get(sourceContext);
        if (relations != null) {
            return relations.get(targetContext);
        }
        return null;
    }

    public Map<String, ContextRelation> getRelationsForContext(String context) {
        return contextRelations.getOrDefault(context, new HashMap<>());
    }

    /**
     * 獲取所有上下文
     *
     * @return 所有上下文的名稱
     */
    public String[] getAllContexts() {
        return contextRelations.keySet().toArray(new String[0]);
    }

    /** 打印所有上下文關係 */
    public void printAllRelations() {
        for (String sourceContext : contextRelations.keySet()) {
            Map<String, ContextRelation> relations = contextRelations.get(sourceContext);
            for (ContextRelation relation : relations.values()) {
                logger.info("{}", relation);
            }
        }
    }
}
