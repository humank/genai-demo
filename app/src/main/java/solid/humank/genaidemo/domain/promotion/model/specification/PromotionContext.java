package solid.humank.genaidemo.domain.promotion.model.specification;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import solid.humank.genaidemo.domain.common.specification.Specification;
import solid.humank.genaidemo.domain.customer.model.aggregate.Customer;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.product.model.aggregate.Product;

/** 促銷上下文 包含評估促銷條件所需的所有信息 */
public class PromotionContext implements Specification<Object> {
    private final Order order;
    private final Customer customer;
    private final LocalDateTime currentTime;
    private final Map<String, Integer> promotionInventory;
    private final Map<String, Product> products;

    public PromotionContext(
            Order order,
            Customer customer,
            LocalDateTime currentTime,
            Map<String, Integer> promotionInventory,
            Map<String, Product> products) {
        this.order = order;
        this.customer = customer;
        this.currentTime = currentTime;
        this.promotionInventory = promotionInventory;
        this.products = products;
    }

    public Order getOrder() {
        return order;
    }

    public Customer getCustomer() {
        return customer;
    }

    public LocalDateTime getCurrentTime() {
        return currentTime;
    }

    public Optional<Integer> getRemainingQuantity(String promotionId) {
        return Optional.ofNullable(promotionInventory.get(promotionId));
    }

    public void decrementRemainingQuantity(String promotionId) {
        if (promotionInventory.containsKey(promotionId)) {
            int currentQuantity = promotionInventory.get(promotionId);
            if (currentQuantity > 0) {
                promotionInventory.put(promotionId, currentQuantity - 1);
            }
        }
    }

    public Optional<Product> getProduct(String productId) {
        return Optional.ofNullable(products.get(productId));
    }

    @Override
    public boolean isSatisfiedBy(Object entity) {
        // 由於PromotionContext是上下文對象，不是真正的規格，
        // 這裡提供一個默認實現，實際使用時應該由具體的促銷規格來實現
        return true;
    }
}
