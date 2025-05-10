# language: en
# Original language: zh-TW
Feature: Delivery Management
  # 作為一個客戶
  # 我希望能夠追蹤我的訂單配送狀態
  # 以便知道何時能收到商品
  As a customer
  I want to track the delivery status of my order
  So that I know when I will receive my products

  Background:
    # 假設我已經創建了一個訂單
    # 並且我已經成功支付了訂單
    # 並且訂單狀態為 "已確認"
    Given I have created an order
    And I have successfully paid for the order
    And the order status is "CONFIRMED"

  Scenario: Successfully arrange delivery
    # 當系統安排配送
    # 並且物流系統建立配送單
    # 那麼配送狀態應該更新為 "待發貨"
    # 並且我應該收到配送安排的通知
    # 並且通知應該包含預計配送時間
    When the system arranges delivery
    And the logistics system creates a delivery order
    Then the delivery status should be updated to "PENDING_SHIPMENT"
    And I should receive a delivery arrangement notification
    And the notification should include estimated delivery time

  Scenario: Delivery resource allocation
    # 假設物流系統已建立配送單
    # 當物流系統分配配送資源
    # 並且配送員接受配送任務
    # 那麼配送狀態應該更新為 "配送中"
    # 並且我應該能夠查看配送員信息
    # 並且我應該能夠追蹤配送實時位置
    Given the logistics system has created a delivery order
    When the logistics system allocates delivery resources
    And the delivery person accepts the delivery task
    Then the delivery status should be updated to "IN_TRANSIT"
    And I should be able to view delivery person information
    And I should be able to track the delivery real-time location

  Scenario: Successfully complete delivery
    # 假設配送狀態為 "配送中"
    # 當配送員送達商品
    # 並且我簽收商品
    # 那麼配送狀態應該更新為 "已送達"
    # 並且訂單狀態應該更新為 "已完成"
    # 並且我應該收到配送完成的通知
    Given the delivery status is "IN_TRANSIT"
    When the delivery person delivers the products
    And I sign for the products
    Then the delivery status should be updated to "DELIVERED"
    And the order status should be updated to "COMPLETED"
    And I should receive a delivery completion notification

  Scenario: Delivery delay
    # 假設配送狀態為 "配送中"
    # 當配送過程中遇到延遲
    # 並且配送員更新延遲原因
    # 那麼配送狀態應該更新為 "延遲"
    # 並且我應該收到配送延遲的通知
    # 並且通知應該包含延遲原因和新的預計送達時間
    Given the delivery status is "IN_TRANSIT"
    When there is a delay during delivery
    And the delivery person updates the delay reason
    Then the delivery status should be updated to "DELAYED"
    And I should receive a delivery delay notification
    And the notification should include the delay reason and new estimated delivery time

  Scenario: Update delivery address
    # 假設配送狀態為 "待發貨"
    # 當我請求更新配送地址
    # 並且我提供新的有效地址
    # 那麼系統應該更新配送地址
    # 並且物流系統應該更新配送單信息
    # 並且我應該收到地址更新成功的通知
    Given the delivery status is "PENDING_SHIPMENT"
    When I request to update the delivery address
    And I provide a new valid address
    Then the system should update the delivery address
    And the logistics system should update the delivery order information
    And I should receive an address update success notification

  Scenario: Delivery failure - No one to sign
    # 假設配送狀態為 "配送中"
    # 當配送員到達配送地址
    # 並且無人簽收
    # 那麼配送員應該記錄配送失敗
    # 並且配送狀態應該更新為 "配送失敗"
    # 並且系統應該安排重新配送
    # 並且我應該收到配送失敗的通知
    # 並且通知應該包含重新配送的信息
    Given the delivery status is "IN_TRANSIT"
    When the delivery person arrives at the delivery address
    And there is no one to sign for the delivery
    Then the delivery person should record the delivery failure
    And the delivery status should be updated to "DELIVERY_FAILED"
    And the system should arrange for redelivery
    And I should receive a delivery failure notification
    And the notification should include redelivery information

  Scenario: Customer refuses delivery
    # 假設配送狀態為 "配送中"
    # 當配送員到達配送地址
    # 並且我拒絕簽收
    # 並且我提供拒收原因
    # 那麼配送員應該記錄拒收信息
    # 並且配送狀態應該更新為 "已拒收"
    # 並且系統應該創建退貨流程
    # 並且我應該收到退貨流程已啟動的通知
    Given the delivery status is "IN_TRANSIT"
    When the delivery person arrives at the delivery address
    And I refuse to sign for the delivery
    And I provide a reason for refusal
    Then the delivery person should record the refusal information
    And the delivery status should be updated to "REFUSED"
    And the system should create a return process
    And I should receive a notification that the return process has been initiated