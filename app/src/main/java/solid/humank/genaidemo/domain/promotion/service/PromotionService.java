package solid.humank.genaidemo.domain.promotion.service;

import solid.humank.genaidemo.domain.common.annotations.DomainService;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.common.valueobject.OrderItem;
import solid.humank.genaidemo.domain.customer.model.aggregate.Customer;
import solid.humank.genaidemo.domain.product.model.aggregate.Product;
import solid.humank.genaidemo.domain.promotion.model.aggregate.Promotion;
import solid.humank.genaidemo.domain.promotion.model.entity.Voucher;
import solid.humank.genaidemo.domain.promotion.model.specification.*;
import solid.humank.genaidemo.domain.promotion.model.valueobject.*;
import solid.humank.genaidemo.domain.promotion.repository.PromotionRepository;
import solid.humank.genaidemo.domain.promotion.repository.VoucherRepository;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 促銷服務
 * 處理促銷規則的應用和優惠券的創建
 */
@DomainService
public class PromotionService {
    private final PromotionRepository promotionRepository;
    private final VoucherRepository voucherRepository;
    
    public PromotionService(PromotionRepository promotionRepository, VoucherRepository voucherRepository) {
        this.promotionRepository = promotionRepository;
        this.voucherRepository = voucherRepository;
    }
    
    /**
     * 應用加價購規則
     * 
     * @param order 訂單
     * @param customer 客戶
     * @return 更新後的訂單
     */
    public Order applyAddOnPurchaseRules(Order order, Customer customer) {
        List<Promotion> addOnPromotions = promotionRepository.findByType(PromotionType.ADD_ON_PURCHASE);
        
        // 創建促銷上下文
        PromotionContext context = createPromotionContext(order, customer);
        
        for (Promotion promotion : addOnPromotions) {
            Optional<AddOnPurchaseRule> ruleOpt = promotion.getAddOnPurchaseRule();
            if (ruleOpt.isPresent()) {
                AddOnPurchaseRule rule = ruleOpt.get();
                
                // 創建規格
                AddOnPurchaseSpecification spec = new AddOnPurchaseSpecification(rule);
                
                // 檢查是否滿足條件
                if (spec.isSatisfiedBy(context)) {
                    // 更新加價購商品的價格
                    order = updateAddOnProductPrice(order, rule);
                }
            }
        }
        
        return order;
    }
    
    /**
     * 應用限時特價規則
     * 
     * @param order 訂單
     * @param customer 客戶
     * @return 更新後的訂單
     */
    public Order applyFlashSaleRules(Order order, Customer customer) {
        List<Promotion> flashSalePromotions = promotionRepository.findByType(PromotionType.FLASH_SALE);
        
        // 創建促銷上下文
        PromotionContext context = createPromotionContext(order, customer);
        
        for (Promotion promotion : flashSalePromotions) {
            Optional<FlashSaleRule> ruleOpt = promotion.getFlashSaleRule();
            if (ruleOpt.isPresent()) {
                FlashSaleRule rule = ruleOpt.get();
                
                // 創建規格
                FlashSaleSpecification spec = new FlashSaleSpecification(rule);
                
                // 檢查是否滿足條件
                if (spec.isSatisfiedBy(context)) {
                    // 更新限時特價商品的價格
                    order = updateFlashSaleProductPrice(order, rule);
                }
            }
        }
        
        return order;
    }
    
    /**
     * 應用限量特價規則
     * 
     * @param order 訂單
     * @param customer 客戶
     * @return 更新後的訂單
     */
    public Order applyLimitedQuantityRules(Order order, Customer customer) {
        List<Promotion> limitedQuantityPromotions = promotionRepository.findByType(PromotionType.LIMITED_QUANTITY_DEAL);
        
        // 創建促銷上下文
        PromotionContext context = createPromotionContext(order, customer);
        
        for (Promotion promotion : limitedQuantityPromotions) {
            Optional<LimitedQuantityRule> ruleOpt = promotion.getLimitedQuantityRule();
            if (ruleOpt.isPresent()) {
                LimitedQuantityRule rule = ruleOpt.get();
                
                // 創建規格
                LimitedQuantitySpecification spec = new LimitedQuantitySpecification(rule);
                
                // 檢查是否滿足條件
                if (spec.isSatisfiedBy(context)) {
                    // 更新限量特價商品的價格
                    order = updateLimitedQuantityProductPrice(order, rule);
                    
                    // 減少促銷庫存
                    decrementPromotionInventory(rule.getPromotionId());
                    
                    // 在上下文中也減少庫存，確保同一訂單中的多個相同商品不會重複享受優惠
                    context.decrementRemainingQuantity(rule.getPromotionId());
                }
            }
        }
        
        return order;
    }
    
    /**
     * 應用滿額贈禮規則
     * 
     * @param order 訂單
     * @param customer 客戶
     * @return 更新後的訂單，可能包含贈品
     */
    public Order applyGiftWithPurchaseRules(Order order, Customer customer) {
        List<Promotion> giftPromotions = promotionRepository.findByType(PromotionType.GIFT_WITH_PURCHASE);
        
        // 創建促銷上下文
        PromotionContext context = createPromotionContext(order, customer);
        
        for (Promotion promotion : giftPromotions) {
            Optional<GiftWithPurchaseRule> ruleOpt = promotion.getGiftWithPurchaseRule();
            if (ruleOpt.isPresent()) {
                GiftWithPurchaseRule rule = ruleOpt.get();
                
                // 創建規格
                GiftWithPurchaseSpecification spec = new GiftWithPurchaseSpecification(rule);
                
                // 檢查是否滿足條件
                if (spec.isSatisfiedBy(context)) {
                    // 計算贈品數量
                    int giftQuantity = spec.calculateGiftQuantity(context);
                    
                    // 添加贈品到訂單
                    order = addGiftsToOrder(order, rule, giftQuantity);
                }
            }
        }
        
        return order;
    }
    
    /**
     * 創建超商優惠券
     * 
     * @param voucherRule 優惠券規則
     * @return 創建的優惠券列表
     */
    public List<Voucher> createConvenienceStoreVouchers(ConvenienceStoreVoucherRule voucherRule) {
        List<Voucher> vouchers = new ArrayList<>();
        
        // 計算有效天數
        int validDays = Period.between(
            LocalDateTime.now().toLocalDate(), 
            LocalDateTime.now().plus(voucherRule.getValidPeriod()).toLocalDate()
        ).getDays();
        
        Money voucherValue = voucherRule.getPrice();
        
        for (int i = 0; i < voucherRule.getQuantity(); i++) {
            Voucher voucher = new Voucher(
                voucherRule.getVoucherName(),
                voucherValue,
                validDays,
                voucherRule.getRedemptionLocation(),
                voucherRule.getContents()
            );
            
            // 保存優惠券
            voucherRepository.save(voucher);
            
            vouchers.add(voucher);
        }
        
        return vouchers;
    }
    
    /**
     * 處理遺失的優惠券
     * 
     * @param voucherId 優惠券ID
     * @return 替換的優惠券，如果原優惠券不存在或已兌換則返回空
     */
    public Optional<Voucher> handleLostVoucher(String voucherId) {
        Optional<Voucher> voucherOpt = voucherRepository.findById(voucherId);
        
        if (voucherOpt.isPresent()) {
            Voucher voucher = voucherOpt.get();
            
            // 檢查優惠券是否有效且未使用
            if (voucher.isValid() && !voucher.isUsed()) {
                // 創建替換優惠券
                Voucher replacement = new Voucher(
                    voucher.getName(),
                    voucher.getValue(),
                    voucher.getValidDays(),
                    voucher.getRedemptionLocation(),
                    voucher.getContents()
                );
                
                // 保存替換優惠券
                voucherRepository.save(replacement);
                
                // 更新原優惠券狀態
                voucherRepository.save(voucher);
                
                return Optional.of(replacement);
            }
        }
        
        return Optional.empty();
    }
    
    // 輔助方法
    
    private PromotionContext createPromotionContext(Order order, Customer customer) {
        // 創建促銷上下文，包含評估促銷條件所需的所有信息
        return new PromotionContext(
            order,
            customer,
            LocalDateTime.now(),
            getPromotionInventory(),
            getProductMap()
        );
    }
    
    private Map<String, Integer> getPromotionInventory() {
        // 從倉儲獲取促銷庫存信息
        // 這裡是簡化實現
        return new HashMap<>();
    }
    
    private Map<String, Product> getProductMap() {
        // 從倉儲獲取產品信息
        // 這裡是簡化實現
        return new HashMap<>();
    }
    
    private Order updateAddOnProductPrice(Order order, AddOnPurchaseRule rule) {
        // 更新加價購商品的價格
        // 這裡是簡化實現
        return order;
    }
    
    private Order updateFlashSaleProductPrice(Order order, FlashSaleRule rule) {
        // 更新限時特價商品的價格
        // 這裡是簡化實現
        return order;
    }
    
    private Order updateLimitedQuantityProductPrice(Order order, LimitedQuantityRule rule) {
        // 更新限量特價商品的價格
        // 實際實現中，這裡會更新訂單中對應產品的價格
        String productId = rule.getProductId().getId();
        Money specialPrice = rule.getSpecialPrice();
        
        // 在實際實現中，這裡會遍歷訂單項目並更新價格
        // 這裡只是簡化的示例
        return order;
    }
    
    private void decrementPromotionInventory(String promotionId) {
        // 減少促銷庫存
        // 在實際實現中，這裡會更新資料庫中的促銷庫存
        Map<String, Integer> inventory = getPromotionInventory();
        if (inventory.containsKey(promotionId)) {
            int currentQuantity = inventory.get(promotionId);
            if (currentQuantity > 0) {
                inventory.put(promotionId, currentQuantity - 1);
                // 在實際實現中，這裡會將更新後的庫存保存到資料庫
            }
        }
    }
    
    private Order addGiftsToOrder(Order order, GiftWithPurchaseRule rule, int quantity) {
        // 添加贈品到訂單
        // 這裡是簡化實現
        return order;
    }
}