# language: en
# Original language: zh-TW
@notification
Feature: Notification Service
  As an order system
  I need to send various notifications to customers
  So that customers are informed about the latest status of their orders

  Background:
    # 假設通知系統正常運行
    # 並且客戶已設置接收通知
    Given the notification system is functioning properly
    And the customer has set up notification preferences

  Scenario: Send order creation notification
    # 當客戶創建新訂單
    # 並且訂單ID為 "ORD-20240510-001"
    # 那麼系統應該發送訂單創建通知
    # 並且通知應該包含訂單ID "ORD-20240510-001"
    # 並且通知應該包含訂單創建時間
    # 並且通知應該發送到客戶的郵箱和手機
    When a customer creates a new order
    And the order ID is "ORD-20240510-001"
    Then the system should send an order creation notification
    And the notification should include order ID "ORD-20240510-001"
    And the notification should include the order creation time
    And the notification should be sent to the customer's email and phone

  Scenario: Send order confirmation notification
    # 當訂單支付成功
    # 並且訂單狀態更新為 "已確認"
    # 那麼系統應該發送訂單確認通知
    # 並且通知應該包含訂單詳情
    # 並且通知應該包含預計配送時間
    When an order payment is successful
    And the order status is updated to "CONFIRMED"
    Then the system should send an order confirmation notification
    And the notification should include order details
    And the notification should include estimated delivery time

  Scenario: Send payment failure notification
    # 當訂單支付失敗
    # 並且失敗原因為 "信用卡餘額不足"
    # 那麼系統應該發送支付失敗通知
    # 並且通知應該包含失敗原因
    # 並且通知應該包含重新支付的鏈接
    When an order payment fails
    And the failure reason is "Insufficient credit card balance"
    Then the system should send a payment failure notification
    And the notification should include the failure reason
    And the notification should include a link to retry payment

  Scenario: Send insufficient inventory notification
    # 當訂單中的商品庫存不足
    # 並且無法完成訂單
    # 那麼系統應該發送庫存不足通知
    # 並且通知應該包含庫存不足的商品信息
    # 並且通知應該包含替代商品建議
    When products in an order have insufficient inventory
    And the order cannot be fulfilled
    Then the system should send an insufficient inventory notification
    And the notification should include information about the out-of-stock products
    And the notification should include alternative product suggestions

  Scenario: Send delivery status update notification
    # 當配送狀態更新為 "配送中"
    # 並且有預計送達時間
    # 那麼系統應該發送配送狀態更新通知
    # 並且通知應該包含當前配送狀態
    # 並且通知應該包含預計送達時間
    # 並且通知應該包含配送追蹤鏈接
    When the delivery status is updated to "IN_TRANSIT"
    And there is an estimated delivery time
    Then the system should send a delivery status update notification
    And the notification should include the current delivery status
    And the notification should include the estimated delivery time
    And the notification should include a delivery tracking link

  Scenario: Send order completion notification
    # 當客戶確認收貨
    # 並且訂單狀態更新為 "已完成"
    # 那麼系統應該發送訂單完成通知
    # 並且通知應該包含訂單評價鏈接
    # 並且通知應該包含相關商品推薦
    When a customer confirms receipt
    And the order status is updated to "COMPLETED"
    Then the system should send an order completion notification
    And the notification should include an order rating link
    And the notification should include related product recommendations

  Scenario: Handle notification delivery failure
    # 假設客戶的郵箱地址無效
    # 當系統嘗試發送通知到客戶郵箱
    # 並且發送失敗
    # 那麼系統應該記錄發送失敗事件
    # 並且系統應該嘗試通過其他渠道發送通知
    # 並且系統應該在 24 小時內重試發送
    Given the customer's email address is invalid
    When the system attempts to send a notification to the customer's email
    And the delivery fails
    Then the system should log the delivery failure event
    And the system should attempt to send the notification through other channels
    And the system should retry delivery within 24 hours

  Scenario: Customer notification preference settings
    # 當客戶更新通知偏好設置
    # 並且選擇只接收 "訂單狀態變更" 和 "配送狀態" 通知
    # 並且選擇通過 "短信" 接收通知
    # 那麼系統應該更新客戶的通知偏好
    # 並且客戶應該只收到所選類型的通知
    # 並且通知應該只通過短信發送
    When a customer updates notification preferences
    And selects to receive only "Order Status Change" and "Delivery Status" notifications
    And chooses to receive notifications via "SMS"
    Then the system should update the customer's notification preferences
    And the customer should only receive notifications of the selected types
    And notifications should only be sent via SMS