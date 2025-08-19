package solid.humank.genaidemo.domain.notification.service;

import java.util.List;
import solid.humank.genaidemo.domain.common.annotations.DomainService;
import solid.humank.genaidemo.domain.notification.model.aggregate.Notification;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationChannel;

/** 通知發送器接口 定義通知發送的行為 */
@DomainService
public interface NotificationSender {

    /**
     * 發送通知
     *
     * @param notification 通知
     * @return 是否發送成功
     */
    boolean send(Notification notification);

    /**
     * 通過指定渠道發送通知
     *
     * @param notification 通知
     * @param channel 通知渠道
     * @return 是否發送成功
     */
    boolean sendThroughChannel(Notification notification, NotificationChannel channel);

    /**
     * 通過替代渠道發送通知
     *
     * @param notification 通知
     * @return 是否發送成功
     */
    boolean sendThroughAlternativeChannels(Notification notification);

    /**
     * 批量發送通知
     *
     * @param notifications 通知列表
     * @return 成功發送的通知數量
     */
    int sendBatch(List<Notification> notifications);
}
