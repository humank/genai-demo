# language: en
# Original language: zh-TW
Feature: Payment Processing
  # 作為一個客戶
  # 我希望能夠為我的訂單進行支付
  # 以便完成購買流程
  As a customer
  I want to make payments for my orders
  So that I can complete the purchase process

  Background:
    # 假設我已經創建了一個訂單
    # 並且訂單包含有效的商品
    # 並且訂單已提交
    Given I have created an order
    And the order contains valid products
    And the order has been submitted

  Scenario: Successfully process credit card payment
    # 當系統確認庫存充足
    # 並且我選擇信用卡支付方式
    # 並且我輸入有效的信用卡信息
    #   | 卡號             | 有效期   | 安全碼 |
    #   | 4111111111111111 | 12/2025 | 123    |
    # 並且我確認支付
    # 那麼支付系統應該驗證支付信息
    # 並且支付系統應該執行支付交易
    # 並且支付狀態應該更新為 "已完成"
    # 並且訂單狀態應該更新為 "已確認"
    # 並且我應該收到支付成功的通知
    When the system confirms sufficient inventory
    And I select credit card payment method
    And I enter valid credit card information
      | Card Number      | Expiry Date | CVV |
      | 4111111111111111 | 12/2025     | 123 |
    And I confirm payment
    Then the payment system should validate payment information
    And the payment system should execute payment transaction
    And the payment status should be updated to "COMPLETED"
    And the order status should be updated to "CONFIRMED"
    And I should receive a payment success notification

  Scenario: Credit card payment failure - Insufficient funds
    # 當系統確認庫存充足
    # 並且我選擇信用卡支付方式
    # 並且我輸入餘額不足的信用卡信息
    # 並且我確認支付
    # 那麼支付系統應該驗證支付信息
    # 並且支付系統應該拒絕支付交易
    # 並且支付狀態應該更新為 "失敗"
    # 並且訂單狀態應該更新為 "失敗"
    # 並且我應該收到支付失敗的通知
    # 並且通知應該包含錯誤信息 "信用卡餘額不足"
    When the system confirms sufficient inventory
    And I select credit card payment method
    And I enter credit card information with insufficient funds
    And I confirm payment
    Then the payment system should validate payment information
    And the payment system should reject the payment transaction
    And the payment status should be updated to "FAILED"
    And the order status should be updated to "FAILED"
    And I should receive a payment failure notification
    And the notification should contain error message "Insufficient credit card funds"

  Scenario: Credit card payment failure - Invalid card number
    # 當系統確認庫存充足
    # 並且我選擇信用卡支付方式
    # 並且我輸入無效的信用卡號
    # 並且我確認支付
    # 那麼支付系統應該驗證支付信息
    # 並且支付系統應該拒絕支付交易
    # 並且支付狀態應該更新為 "失敗"
    # 並且訂單狀態應該更新為 "失敗"
    # 並且我應該收到支付失敗的通知
    # 並且通知應該包含錯誤信息 "無效的信用卡號"
    When the system confirms sufficient inventory
    And I select credit card payment method
    And I enter an invalid credit card number
    And I confirm payment
    Then the payment system should validate payment information
    And the payment system should reject the payment transaction
    And the payment status should be updated to "FAILED"
    And the order status should be updated to "FAILED"
    And I should receive a payment failure notification
    And the notification should contain error message "Invalid credit card number"

  Scenario: Payment timeout
    # 當系統確認庫存充足
    # 並且我選擇信用卡支付方式
    # 並且支付網關響應超時
    # 那麼系統應該在等待30秒後取消支付請求
    # 並且支付狀態應該更新為 "失敗"
    # 並且訂單狀態應該更新為 "待支付"
    # 並且我應該收到支付超時的通知
    # 並且我應該能夠重新嘗試支付
    When the system confirms sufficient inventory
    And I select credit card payment method
    And the payment gateway response times out
    Then the system should cancel the payment request after waiting 30 seconds
    And the payment status should be updated to "FAILED"
    And the order status should be updated to "PENDING_PAYMENT"
    And I should receive a payment timeout notification
    And I should be able to retry payment

  Scenario: Successfully process bank transfer payment
    # 當系統確認庫存充足
    # 並且我選擇銀行轉賬支付方式
    # 並且我完成銀行轉賬
    # 並且銀行確認轉賬成功
    # 那麼支付系統應該驗證轉賬信息
    # 並且支付狀態應該更新為 "已完成"
    # 並且訂單狀態應該更新為 "已確認"
    # 並且我應該收到支付成功的通知
    When the system confirms sufficient inventory
    And I select bank transfer payment method
    And I complete the bank transfer
    And the bank confirms successful transfer
    Then the payment system should validate transfer information
    And the payment status should be updated to "COMPLETED"
    And the order status should be updated to "CONFIRMED"
    And I should receive a payment success notification

  Scenario: Request refund after successful payment
    # 假設我已經成功支付了訂單
    # 當我申請退款
    # 並且我提供有效的退款理由
    # 那麼系統應該創建退款請求
    # 並且支付系統應該處理退款
    # 並且退款狀態應該更新為 "處理中"
    # 並且我應該收到退款申請已受理的通知
    Given I have successfully paid for an order
    When I request a refund
    And I provide a valid refund reason
    Then the system should create a refund request
    And the payment system should process the refund
    And the refund status should be updated to "PROCESSING"
    And I should receive a refund request acknowledgment notification