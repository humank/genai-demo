package solid.humank.genaidemo.application.notification.port.incoming;

/** 通知用例接口 - 主要輸入端口 定義系統對外提供的所有通知相關操作 */
public interface NotificationUseCase {
    /**
     * 發送訂單確認通知
     *
     * @param orderId 訂單ID
     * @param message 通知消息
     * @return 是否發送成功
     */
    boolean sendOrderConfirmation(String orderId, String message);

    /**
     * 發送支付確認通知
     *
     * @param orderId 訂單ID
     * @param message 通知消息
     * @return 是否發送成功
     */
    boolean sendPaymentConfirmation(String orderId, String message);

    /**
     * 發送配送通知
     *
     * @param orderId 訂單ID
     * @param message 通知消息
     * @return 是否發送成功
     */
    boolean sendDeliveryNotification(String orderId, String message);

    /**
     * 發送錯誤通知
     *
     * @param orderId 訂單ID
     * @param errorMessage 錯誤消息
     * @return 是否發送成功
     */
    boolean sendErrorNotification(String orderId, String errorMessage);
}
