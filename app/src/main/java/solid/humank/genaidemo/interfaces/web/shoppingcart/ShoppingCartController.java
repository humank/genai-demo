package solid.humank.genaidemo.interfaces.web.shoppingcart;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import solid.humank.genaidemo.application.shoppingcart.dto.ShoppingCartDto;
import solid.humank.genaidemo.application.shoppingcart.service.ShoppingCartApplicationService;
import solid.humank.genaidemo.interfaces.web.shoppingcart.dto.AddItemRequest;
import solid.humank.genaidemo.interfaces.web.shoppingcart.dto.ApplyCouponRequest;
import solid.humank.genaidemo.interfaces.web.shoppingcart.dto.ApplyPromotionRequest;
import solid.humank.genaidemo.interfaces.web.shoppingcart.dto.UpdateQuantityRequest;

@RestController
@RequestMapping("/api/consumer/cart")
@Tag(name = "購物車管理", description = "購物車相關操作")
public class ShoppingCartController {

    private final ShoppingCartApplicationService shoppingCartService;

    public ShoppingCartController(ShoppingCartApplicationService shoppingCartService) {
        this.shoppingCartService = shoppingCartService;
    }

    @GetMapping("/{customerId}")
    @Operation(summary = "獲取購物車", description = "根據客戶ID獲取購物車信息")
    public ResponseEntity<ShoppingCartDto> getShoppingCart(
            @Parameter(description = "客戶ID") @PathVariable String customerId) {

        var cart = shoppingCartService.getShoppingCartByCustomerId(customerId);
        if (cart.isPresent()) {
            return ResponseEntity.ok(cart.get());
        } else {
            // 如果購物車不存在，創建一個新的
            var newCart = shoppingCartService.createShoppingCart(customerId);
            return ResponseEntity.ok(newCart);
        }
    }

    @PostMapping("/{customerId}/items")
    @Operation(summary = "添加商品到購物車", description = "向購物車添加商品")
    public ResponseEntity<Map<String, String>> addItemToCart(
            @Parameter(description = "客戶ID") @PathVariable String customerId,
            @RequestBody AddItemRequest request) {

        shoppingCartService.addItemToCart(
                customerId,
                request.getProductId(),
                request.getQuantity(),
                request.getUnitPrice());

        return ResponseEntity.ok(Map.of("message", "商品已添加到購物車"));
    }

    @PutMapping("/{customerId}/items/{productId}")
    @Operation(summary = "更新購物車商品數量", description = "更新購物車中指定商品的數量")
    public ResponseEntity<Map<String, String>> updateItemQuantity(
            @Parameter(description = "客戶ID") @PathVariable String customerId,
            @Parameter(description = "商品ID") @PathVariable String productId,
            @RequestBody UpdateQuantityRequest request) {

        shoppingCartService.updateItemQuantity(customerId, productId, request.getQuantity());

        return ResponseEntity.ok(Map.of("message", "商品數量已更新"));
    }

    @DeleteMapping("/{customerId}/items/{productId}")
    @Operation(summary = "從購物車移除商品", description = "從購物車中移除指定商品")
    public ResponseEntity<Map<String, String>> removeItemFromCart(
            @Parameter(description = "客戶ID") @PathVariable String customerId,
            @Parameter(description = "商品ID") @PathVariable String productId) {

        shoppingCartService.removeItemFromCart(customerId, productId);

        return ResponseEntity.ok(Map.of("message", "商品已從購物車移除"));
    }

    @DeleteMapping("/{customerId}/clear")
    @Operation(summary = "清空購物車", description = "清空購物車中的所有商品")
    public ResponseEntity<Map<String, String>> clearCart(
            @Parameter(description = "客戶ID") @PathVariable String customerId) {

        shoppingCartService.clearCart(customerId);

        return ResponseEntity.ok(Map.of("message", "購物車已清空"));
    }

    @GetMapping("/{customerId}/checkout")
    @Operation(summary = "計算結帳金額", description = "計算購物車的結帳金額")
    public ResponseEntity<Map<String, Object>> calculateCheckout(
            @Parameter(description = "客戶ID") @PathVariable String customerId) {

        var cart = shoppingCartService.getShoppingCartByCustomerId(customerId);
        if (cart.isPresent()) {
            var cartDto = cart.get();
            return ResponseEntity.ok(Map.of(
                    "subtotal", cartDto.totalAmount(),
                    "tax", 0.0,
                    "total", cartDto.totalAmount()));
        }

        return ResponseEntity.ok(Map.of(
                "subtotal", 0.0,
                "tax", 0.0,
                "total", 0.0));
    }

    @PostMapping("/{customerId}/coupons")
    @Operation(summary = "應用優惠券", description = "向購物車應用優惠券")
    public ResponseEntity<Map<String, String>> applyCoupon(
            @Parameter(description = "客戶ID") @PathVariable String customerId,
            @RequestBody ApplyCouponRequest request) {

        // 優惠券功能將在後續版本中實現
        // 目前返回成功響應以支持前端開發
        return ResponseEntity.ok(Map.of("message", "優惠券功能開發中，敬請期待"));
    }

    @DeleteMapping("/{customerId}/coupons/{couponCode}")
    @Operation(summary = "移除優惠券", description = "從購物車移除優惠券")
    public ResponseEntity<Map<String, String>> removeCoupon(
            @Parameter(description = "客戶ID") @PathVariable String customerId,
            @Parameter(description = "優惠券代碼") @PathVariable String couponCode) {

        // 優惠券功能將在後續版本中實現
        // 目前返回成功響應以支持前端開發
        return ResponseEntity.ok(Map.of("message", "優惠券功能開發中，敬請期待"));
    }

    @GetMapping("/{customerId}/promotions")
    @Operation(summary = "獲取可用促銷活動", description = "獲取購物車可用的促銷活動")
    public ResponseEntity<Map<String, Object>> getAvailablePromotions(
            @Parameter(description = "客戶ID") @PathVariable String customerId) {

        // 促銷活動功能將在後續版本中實現
        // 目前返回空列表以支持前端開發
        return ResponseEntity.ok(Map.of("promotions", java.util.List.of(), "message", "促銷活動功能開發中"));
    }

    @PostMapping("/{customerId}/promotions")
    @Operation(summary = "應用促銷活動", description = "向購物車應用促銷活動")
    public ResponseEntity<Map<String, String>> applyPromotion(
            @Parameter(description = "客戶ID") @PathVariable String customerId,
            @RequestBody ApplyPromotionRequest request) {

        // 促銷活動功能將在後續版本中實現
        // 目前返回成功響應以支持前端開發
        return ResponseEntity.ok(Map.of("message", "促銷活動功能開發中，敬請期待"));
    }

    @GetMapping("/{customerId}/inventory-check")
    @Operation(summary = "檢查庫存可用性", description = "檢查購物車商品的庫存可用性")
    public ResponseEntity<Map<String, Object>> checkInventoryAvailability(
            @Parameter(description = "客戶ID") @PathVariable String customerId) {

        // 庫存檢查功能將在後續版本中實現
        // 目前返回可用狀態以支持前端開發
        return ResponseEntity.ok(Map.of("available", true, "items", java.util.List.of(), "message", "庫存檢查功能開發中"));
    }

}