package solid.humank.genaidemo.domain.product.service;

import solid.humank.genaidemo.domain.common.annotations.DomainService;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.product.model.aggregate.Product;
import solid.humank.genaidemo.domain.product.model.entity.Bundle;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 捆綁銷售服務
 */
@DomainService(description = "捆綁銷售服務，處理產品捆綁銷售的業務邏輯和折扣計算")
public class BundleService {
    
    /**
     * 獲取指定名稱的捆綁銷售
     */
    public Bundle getBundle(String bundleName) {
        // 實際實現中，這裡應該從資料庫或配置中獲取捆綁銷售
        return null;
    }
    
    /**
     * 獲取捆綁銷售中的產品
     */
    public List<Product> getBundleProducts(Bundle bundle) {
        // 實際實現中，這裡應該從資料庫或配置中獲取捆綁銷售的產品
        return null;
    }
    
    /**
     * 計算產品的原始總價
     */
    public Money calculateRegularPrice(List<Product> products) {
        Money total = Money.twd(0);
        for (Product product : products) {
            total = total.add(product.getPrice());
        }
        return total;
    }
    
    /**
     * 計算捆綁銷售的折扣價格
     */
    public Money calculateDiscountedPrice(Bundle bundle, List<Product> products) {
        Money regularPrice = calculateRegularPrice(products);
        
        if (bundle.getDiscount().isFixedPrice()) {
            return bundle.getDiscount().getDiscountedPrice();
        } else {
            return regularPrice.subtract(bundle.getDiscount().calculateDiscount(regularPrice));
        }
    }
    
    /**
     * 應用捆綁銷售折扣到訂單
     */
    public Money applyBundleDiscount(Order order, Bundle bundle) {
        if (bundle.isFixedBundle()) {
            return bundle.getDiscount().getDiscountedPrice();
        } else {
            return order.getTotalAmount().subtract(bundle.getDiscount().calculateDiscount(order.getTotalAmount()));
        }
    }
    
    /**
     * 應用自選捆綁折扣到訂單
     */
    public Money applyPickAnyBundleDiscount(Order order, Bundle bundle, List<Product> products) {
        // 獲取原始總價
        Money regularPrice = calculateRegularPrice(products);
        
        // 如果產品數量小於等於要求的數量，直接應用折扣
        if (products.size() <= bundle.getRequiredItemCount()) {
            return regularPrice.subtract(bundle.getDiscount().calculateDiscount(regularPrice));
        }
        
        // 如果產品數量大於要求的數量，只對最貴的N個產品應用折扣
        List<Product> sortedProducts = products.stream()
            .sorted(Comparator.comparing(p -> p.getPrice().getAmount(), Comparator.reverseOrder()))
            .collect(Collectors.toList());
        
        // 計算折扣金額
        Money discountAmount = Money.twd(0);
        for (int i = 0; i < bundle.getRequiredItemCount(); i++) {
            Product product = sortedProducts.get(i);
            Money productPrice = product.getPrice();
            discountAmount = discountAmount.add(bundle.getDiscount().calculateDiscount(productPrice));
        }
        
        return regularPrice.subtract(discountAmount);
    }
}