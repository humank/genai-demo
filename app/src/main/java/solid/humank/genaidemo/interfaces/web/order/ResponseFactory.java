package solid.humank.genaidemo.interfaces.web.order;

import java.util.List;
import java.util.stream.Collectors;
import java.math.BigDecimal;

import solid.humank.genaidemo.application.order.dto.response.OrderResponse;
import solid.humank.genaidemo.application.order.dto.response.OrderItemResponse;
import solid.humank.genaidemo.application.common.valueobject.Money;
import solid.humank.genaidemo.application.common.valueobject.OrderStatus;
import solid.humank.genaidemo.interfaces.web.order.dto.OrderResponse.WebOrderItemResponse;

/**
 * 響應工廠類，負責將應用層響應轉換為介面層響應
 */
public class ResponseFactory {

    /**
     * 將應用層訂單響應轉換為介面層訂單響應
     */
    public static solid.humank.genaidemo.interfaces.web.order.dto.OrderResponse toWebResponse(OrderResponse appResponse) {
        // 使用 OrderResponse 的靜態工廠方法創建介面層響應
        return solid.humank.genaidemo.interfaces.web.order.dto.OrderResponse.fromApplicationResponse(appResponse);
    }
}