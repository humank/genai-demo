package solid.humank.genaidemo.examples.order.acl;

import solid.humank.genaidemo.ddd.annotations.ValueObject;
import solid.humank.genaidemo.examples.order.OrderId;

/**
 * 配送訂單值物件
 * 在防腐層中用於表示物流系統中的配送訂單
 */
@ValueObject
public record DeliveryOrder(
    OrderId orderId,
    DeliveryStatus status
) {
    public DeliveryOrder {
        if (orderId == null) {
            throw new IllegalArgumentException("訂單ID不能為空");
        }
        if (status == null) {
            throw new IllegalArgumentException("配送狀態不能為空");
        }
    }
}
