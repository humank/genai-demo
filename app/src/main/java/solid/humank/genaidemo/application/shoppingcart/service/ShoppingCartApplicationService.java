package solid.humank.genaidemo.application.shoppingcart.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import solid.humank.genaidemo.application.shoppingcart.dto.CartItemDto;
import solid.humank.genaidemo.application.shoppingcart.dto.ShoppingCartDto;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerId;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.shoppingcart.model.aggregate.ShoppingCart;
import solid.humank.genaidemo.domain.shoppingcart.model.valueobject.CartItem;
import solid.humank.genaidemo.domain.shoppingcart.model.valueobject.ShoppingCartId;
import solid.humank.genaidemo.domain.shoppingcart.port.ShoppingCartRepository;

/** 購物車應用服務 */
@Service
@Transactional
public class ShoppingCartApplicationService {

    private final ShoppingCartRepository shoppingCartRepository;

    public ShoppingCartApplicationService(ShoppingCartRepository shoppingCartRepository) {
        this.shoppingCartRepository = shoppingCartRepository;
    }

    /** 創建購物車 */
    public ShoppingCartDto createShoppingCart(String customerId) {
        ShoppingCart cart = new ShoppingCart(ShoppingCartId.generate(), new CustomerId(customerId));

        ShoppingCart savedCart = shoppingCartRepository.save(cart);
        return toDto(savedCart);
    }

    /** 添加商品到購物車 */
    public ShoppingCartDto addItemToCart(
            String cartId, String productId, int quantity, double unitPrice) {
        Optional<ShoppingCart> cartOpt = shoppingCartRepository.findById(ShoppingCartId.of(cartId));
        if (cartOpt.isEmpty()) {
            throw new IllegalArgumentException("購物車不存在: " + cartId);
        }

        ShoppingCart cart = cartOpt.get();
        cart.addItem(
                new ProductId(productId),
                quantity,
                solid.humank.genaidemo.domain.common.valueobject.Money.twd(unitPrice));

        ShoppingCart savedCart = shoppingCartRepository.save(cart);
        return toDto(savedCart);
    }

    /** 更新購物車商品數量 */
    public ShoppingCartDto updateItemQuantity(String cartId, String productId, int newQuantity) {
        Optional<ShoppingCart> cartOpt = shoppingCartRepository.findById(ShoppingCartId.of(cartId));
        if (cartOpt.isEmpty()) {
            throw new IllegalArgumentException("購物車不存在: " + cartId);
        }

        ShoppingCart cart = cartOpt.get();
        cart.updateItemQuantity(new ProductId(productId), newQuantity);

        ShoppingCart savedCart = shoppingCartRepository.save(cart);
        return toDto(savedCart);
    }

    /** 從購物車移除商品 */
    public ShoppingCartDto removeItemFromCart(String cartId, String productId) {
        Optional<ShoppingCart> cartOpt = shoppingCartRepository.findById(ShoppingCartId.of(cartId));
        if (cartOpt.isEmpty()) {
            throw new IllegalArgumentException("購物車不存在: " + cartId);
        }

        ShoppingCart cart = cartOpt.get();
        cart.removeItem(new ProductId(productId));

        ShoppingCart savedCart = shoppingCartRepository.save(cart);
        return toDto(savedCart);
    }

    /** 獲取購物車 */
    @Transactional(readOnly = true)
    public Optional<ShoppingCartDto> getShoppingCart(String cartId) {
        return shoppingCartRepository.findById(ShoppingCartId.of(cartId)).map(this::toDto);
    }

    /** 根據客戶ID獲取購物車 */
    @Transactional(readOnly = true)
    public Optional<ShoppingCartDto> getShoppingCartByCustomerId(String customerId) {
        return shoppingCartRepository.findByCustomerId(new CustomerId(customerId)).map(this::toDto);
    }

    /** 清空購物車 */
    public ShoppingCartDto clearCart(String cartId) {
        Optional<ShoppingCart> cartOpt = shoppingCartRepository.findById(ShoppingCartId.of(cartId));
        if (cartOpt.isEmpty()) {
            throw new IllegalArgumentException("購物車不存在: " + cartId);
        }

        ShoppingCart cart = cartOpt.get();
        cart.clear();

        ShoppingCart savedCart = shoppingCartRepository.save(cart);
        return toDto(savedCart);
    }

    private ShoppingCartDto toDto(ShoppingCart cart) {
        List<CartItemDto> itemDtos =
                cart.getItems().stream().map(this::toCartItemDto).collect(Collectors.toList());

        // 計算折扣金額（簡化實現）
        java.math.BigDecimal discountAmount = calculateDiscountAmount(cart);

        return new ShoppingCartDto(
                cart.getId().value(),
                cart.getConsumerId().getId(),
                itemDtos,
                cart.getTotalAmount().getAmount(),
                discountAmount,
                cart.getStatus(),
                cart.getCreatedAt(),
                cart.getUpdatedAt());
    }

    private CartItemDto toCartItemDto(CartItem item) {
        return new CartItemDto(
                item.productId().getId(),
                getProductName(item.productId().getId()),
                item.quantity(),
                item.unitPrice().getAmount(),
                item.totalPrice().getAmount());
    }

    private String getProductName(String productId) {
        // 簡化實現：根據產品ID生成商品名稱
        // 實際應該從產品服務獲取
        switch (productId) {
            case "PROD001":
                return "精選咖啡豆";
            case "PROD002":
                return "有機綠茶";
            case "PROD003":
                return "手工餅乾";
            default:
                return "商品 " + productId;
        }
    }

    private java.math.BigDecimal calculateDiscountAmount(ShoppingCart cart) {
        // 簡化實現：計算折扣金額
        // 實際應該整合促銷服務來計算
        java.math.BigDecimal totalAmount = cart.getTotalAmount().getAmount();

        // 假設滿1000元打9折
        if (totalAmount.compareTo(java.math.BigDecimal.valueOf(1000)) >= 0) {
            return totalAmount.multiply(java.math.BigDecimal.valueOf(0.1));
        }

        return java.math.BigDecimal.ZERO;
    }
}
