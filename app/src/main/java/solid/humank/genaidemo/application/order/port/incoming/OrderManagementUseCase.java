package solid.humank.genaidemo.application.order.port.incoming;

import solid.humank.genaidemo.interfaces.web.order.dto.CreateOrderRequest;
import solid.humank.genaidemo.interfaces.web.order.dto.AddOrderItemRequest;
import solid.humank.genaidemo.application.order.dto.OrderResponse;

/**
 * 訂單管理用例接口 - 主要輸入端口
 * 定義系統對外提供的所有訂單相關操作
 */
public interface OrderManagementUseCase {
    /**
     * 創建新訂單
     */
    OrderResponse createOrder(CreateOrderRequest request);

    /**
     * 添加訂單項目
     */
    OrderResponse addOrderItem(AddOrderItemRequest request);

    /**
     * 提交訂單
     */
    OrderResponse submitOrder(String orderId);

    /**
     * 取消訂單
     */
    OrderResponse cancelOrder(String orderId);

    /**
     * 查詢訂單
     */
    OrderResponse getOrder(String orderId);
}