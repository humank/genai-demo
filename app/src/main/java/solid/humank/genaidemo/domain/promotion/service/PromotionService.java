package solid.humank.genaidemo.domain.promotion.service;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import solid.humank.genaidemo.domain.common.annotations.DomainService;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderItem;
import solid.humank.genaidemo.domain.customer.model.aggregate.Customer;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.product.model.aggregate.Product;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.promotion.model.aggregate.Promotion;
import solid.humank.genaidemo.domain.promotion.model.entity.Voucher;
import solid.humank.genaidemo.domain.promotion.model.specification.PromotionContext;
import solid.humank.genaidemo.domain.promotion.model.valueobject.AddOnPurchaseRule;
import solid.humank.genaidemo.domain.promotion.model.valueobject.ConvenienceStoreVoucherRule;
import solid.humank.genaidemo.domain.promotion.model.valueobject.FlashSaleRule;
import solid.humank.genaidemo.domain.promotion.model.valueobject.GiftWithPurchaseRule;
import solid.humank.genaidemo.domain.promotion.model.valueobject.LimitedQuantityRule;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionType;
import solid.humank.genaidemo.domain.promotion.repository.PromotionRepository;
import solid.humank.genaidemo.domain.promotion.repository.VoucherRepository;

/** 促銷服務 處理促銷規則的應用和優惠券的創建 */
@DomainService
public class PromotionService {
    private final PromotionRepository promotionRepository;
    private final VoucherRepository voucherRepository;
    private final Map<String, Integer> promotionInventory = new HashMap<>();
    private final Map<String, Product> productCache = new HashMap<>();

    public PromotionService(
            PromotionRepository promotionRepository, VoucherRepository voucherRepository) {
        this.promotionRepository = promotionRepository;
        this.voucherRepository = voucherRepository;
    }

    /** 應用加價購規則 */
    public Order applyAddOnPurchaseRules(Order order, Customer customer) {
        List<Promotion> addOnPromotions =
                promotionRepository.findByType(PromotionType.ADD_ON_PURCHASE);

        for (Promotion promotion : addOnPromotions) {
            Optional<AddOnPurchaseRule> ruleOpt = promotion.getAddOnPurchaseRule();
            if (ruleOpt.isPresent()) {
                AddOnPurchaseRule rule = ruleOpt.get();
                if (isAddOnPurchaseApplicable(order, rule)) {
                    order = updateAddOnProductPrice(order, rule);
                }
            }
        }

        return order;
    }

    /** 應用限時特價規則 */
    public Order applyFlashSaleRules(Order order, Customer customer) {
        List<Promotion> flashSalePromotions =
                promotionRepository.findByType(PromotionType.FLASH_SALE);

        for (Promotion promotion : flashSalePromotions) {
            Optional<FlashSaleRule> ruleOpt = promotion.getFlashSaleRule();
            if (ruleOpt.isPresent()) {
                FlashSaleRule rule = ruleOpt.get();
                if (isFlashSaleApplicable(order, rule)) {
                    order = updateFlashSaleProductPrice(order, rule);
                }
            }
        }

        return order;
    }

    /** 應用限量特價規則 */
    public Order applyLimitedQuantityRules(Order order, Customer customer) {
        List<Promotion> limitedQuantityPromotions =
                promotionRepository.findByType(PromotionType.LIMITED_QUANTITY);

        for (Promotion promotion : limitedQuantityPromotions) {
            Optional<LimitedQuantityRule> ruleOpt = promotion.getLimitedQuantityRule();
            if (ruleOpt.isPresent()) {
                LimitedQuantityRule rule = ruleOpt.get();
                if (isLimitedQuantityApplicable(order, rule)) {
                    order = updateLimitedQuantityProductPrice(order, rule);
                    decrementPromotionInventory(rule.getPromotionId());
                }
            }
        }

        return order;
    }

    /** 應用滿額贈禮規則 */
    public Order applyGiftWithPurchaseRules(Order order, Customer customer) {
        List<Promotion> giftPromotions =
                promotionRepository.findByType(PromotionType.GIFT_WITH_PURCHASE);

        for (Promotion promotion : giftPromotions) {
            Optional<GiftWithPurchaseRule> ruleOpt = promotion.getGiftWithPurchaseRule();
            if (ruleOpt.isPresent()) {
                GiftWithPurchaseRule rule = ruleOpt.get();
                if (isGiftWithPurchaseApplicable(order, rule)) {
                    int giftQuantity = calculateGiftQuantity(order, rule);
                    order = addGiftsToOrder(order, rule, giftQuantity);
                }
            }
        }

        return order;
    }

    /** 創建超商優惠券 */
    public List<Voucher> createConvenienceStoreVouchers(ConvenienceStoreVoucherRule voucherRule) {
        List<Voucher> vouchers = new ArrayList<>();

        int validDays =
                Period.between(
                                LocalDateTime.now().toLocalDate(),
                                LocalDateTime.now()
                                        .plus(voucherRule.getValidPeriod())
                                        .toLocalDate())
                        .getDays();

        Money voucherValue = voucherRule.getPrice();

        for (int i = 0; i < voucherRule.getQuantity(); i++) {
            Voucher voucher =
                    new Voucher(
                            voucherRule.getVoucherName(),
                            voucherValue,
                            validDays,
                            voucherRule.getRedemptionLocation(),
                            voucherRule.getContents());

            voucherRepository.save(voucher);
            vouchers.add(voucher);
        }

        return vouchers;
    }

    /** 處理遺失的優惠券 */
    public Optional<Voucher> handleLostVoucher(String voucherId) {
        Optional<Voucher> voucherOpt = voucherRepository.findById(voucherId);

        if (voucherOpt.isPresent()) {
            Voucher voucher = voucherOpt.get();

            if (voucher.isValid() && !voucher.isUsed()) {
                Voucher replacement =
                        new Voucher(
                                voucher.getName(),
                                voucher.getValue(),
                                voucher.getValidDays(),
                                voucher.getRedemptionLocation(),
                                voucher.getContents());

                voucherRepository.save(replacement);
                voucherRepository.save(voucher);

                return Optional.of(replacement);
            }
        }

        return Optional.empty();
    }

    // 私有輔助方法

    private boolean isAddOnPurchaseApplicable(Order order, AddOnPurchaseRule rule) {
        // 檢查訂單中是否包含主要商品
        return order.getItems().stream()
                .anyMatch(item -> item.getProductId().equals(rule.getMainProductId()));
    }

    private boolean isFlashSaleApplicable(Order order, FlashSaleRule rule) {
        // 檢查是否在閃購時間內且包含目標商品
        LocalDateTime now = LocalDateTime.now();
        boolean inTimeRange = rule.flashSalePeriod().contains(now);
        boolean hasTargetProduct =
                order.getItems().stream()
                        .anyMatch(item -> item.getProductId().equals(rule.targetProductId()));

        return inTimeRange && hasTargetProduct;
    }

    private boolean isLimitedQuantityApplicable(Order order, LimitedQuantityRule rule) {
        // 檢查是否包含目標商品且庫存充足
        boolean hasTargetProduct =
                order.getItems().stream()
                        .anyMatch(item -> item.getProductId().equals(rule.getProductId()));

        int remainingQuantity =
                getPromotionInventory()
                        .getOrDefault(rule.getPromotionId(), rule.getTotalQuantity());

        return hasTargetProduct && remainingQuantity > 0;
    }

    private boolean isGiftWithPurchaseApplicable(Order order, GiftWithPurchaseRule rule) {
        // 檢查訂單總額是否達到最低消費金額
        Money orderTotal = order.getTotalAmount();
        return orderTotal.getAmount().compareTo(rule.getMinimumPurchaseAmount().getAmount()) >= 0;
    }

    private int calculateGiftQuantity(Order order, GiftWithPurchaseRule rule) {
        if (!rule.isMultipleGiftsAllowed()) {
            return 1;
        }

        // 根據消費金額計算贈品數量
        Money orderTotal = order.getTotalAmount();
        Money minimumAmount = rule.getMinimumPurchaseAmount();

        int multiplier = orderTotal.getAmount().divide(minimumAmount.getAmount()).intValue();
        return Math.min(multiplier, rule.getMaxGiftsPerOrder());
    }

    private Order updateAddOnProductPrice(Order order, AddOnPurchaseRule rule) {
        // 為加價購商品設定特價
        List<OrderItem> updatedItems = new ArrayList<>();

        for (OrderItem item : order.getItems()) {
            if (item.getProductId().equals(rule.getAddOnProductId())) {
                // 更新加價購商品的價格
                OrderItem updatedItem =
                        new OrderItem(
                                item.getProductId(),
                                item.getProductName(),
                                item.getQuantity(),
                                rule.getSpecialPrice());
                updatedItems.add(updatedItem);
            } else {
                updatedItems.add(item);
            }
        }

        return order.updateItems(updatedItems);
    }

    private Order updateFlashSaleProductPrice(Order order, FlashSaleRule rule) {
        List<OrderItem> updatedItems = new ArrayList<>();

        for (OrderItem item : order.getItems()) {
            if (item.getProductId().equals(rule.targetProductId())) {
                // 應用數量限制
                int applicableQuantity = Math.min(item.getQuantity(), rule.quantityLimit());

                if (applicableQuantity > 0) {
                    // 特價商品
                    OrderItem discountedItem =
                            new OrderItem(
                                    item.getProductId(),
                                    item.getProductName(),
                                    applicableQuantity,
                                    rule.specialPrice());
                    updatedItems.add(discountedItem);

                    // 剩餘商品保持原價
                    if (item.getQuantity() > applicableQuantity) {
                        OrderItem remainingItem =
                                new OrderItem(
                                        item.getProductId(),
                                        item.getProductName(),
                                        item.getQuantity() - applicableQuantity,
                                        item.getUnitPrice());
                        updatedItems.add(remainingItem);
                    }
                } else {
                    updatedItems.add(item);
                }
            } else {
                updatedItems.add(item);
            }
        }

        return order.updateItems(updatedItems);
    }

    private Order updateLimitedQuantityProductPrice(Order order, LimitedQuantityRule rule) {
        List<OrderItem> updatedItems = new ArrayList<>();

        for (OrderItem item : order.getItems()) {
            if (item.getProductId().equals(rule.getProductId())) {
                OrderItem updatedItem =
                        new OrderItem(
                                item.getProductId(),
                                item.getProductName(),
                                item.getQuantity(),
                                rule.getSpecialPrice());
                updatedItems.add(updatedItem);
            } else {
                updatedItems.add(item);
            }
        }

        return order.updateItems(updatedItems);
    }

    private Order addGiftsToOrder(Order order, GiftWithPurchaseRule rule, int quantity) {
        List<OrderItem> updatedItems = new ArrayList<>(order.getItems());

        // 添加贈品項目
        OrderItem giftItem =
                new OrderItem(
                        rule.getGiftProductId().getId(),
                        "贈品 - " + getProductName(rule.getGiftProductId()),
                        quantity,
                        Money.twd(0)); // 贈品價格為0

        updatedItems.add(giftItem);
        return order.updateItems(updatedItems);
    }

    private void decrementPromotionInventory(String promotionId) {
        int currentQuantity = promotionInventory.getOrDefault(promotionId, 0);
        if (currentQuantity > 0) {
            promotionInventory.put(promotionId, currentQuantity - 1);
        }
    }

    private Map<String, Integer> getPromotionInventory() {
        return promotionInventory;
    }

    private String getProductName(ProductId productId) {
        // 簡化實現：返回預設商品名稱
        return productCache.containsKey(productId.getId())
                ? productCache.get(productId.getId()).getName().getName()
                : "商品 " + productId.getId();
    }

    private PromotionContext createPromotionContext(Order order, Customer customer) {
        return new PromotionContext(
                order, customer, LocalDateTime.now(), getPromotionInventory(), new HashMap<>());
    }
}
