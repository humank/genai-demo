# 通知管理系統 - Notification Management System
Feature: Notification Management System
  As an e-commerce platform
  I want to provide comprehensive notification management functionality
  So that I can maintain good communication with customers

  Background:
    # 假設系統支援以下通知渠道 - Given the system supports the following notification channels
    Given the system supports the following notification channels:
      | Channel Type | Status | Cost | Delivery Rate |
      | SMS          | Active |    3 |           95% |
      | Email        | Active |    0 |           85% |
      | Push         | Active |    0 |           70% |
      | In-App       | Active |    0 |           90% |
    # 並且客戶 "張小明" 的通知偏好設定為 - And customer "John" notification preferences are set as
    And customer "John" notification preferences are set as:
      | Notification Type | SMS | Email | Push | In-App |
      | Order Status      | Yes | Yes   | Yes  | Yes    |
      | Promotions        | No  | Yes   | Yes  | No     |
      | Delivery          | Yes | No    | Yes  | Yes    |
      | System Alerts     | No  | Yes   | No   | Yes    |
  # 場景: 訂單狀態通知 - Scenario: Order status notification

  Scenario: Order status notification
    # 假設客戶 "張小明" 的訂單狀態從 "處理中" 更新為 "已出貨"
    Given customer "John" order "550e8400-e29b-41d4-a716-446655440001" status updates from "Processing" to "Shipped"
    # 當系統觸發訂單狀態通知 - When system triggers order status notification
    When system triggers order status notification
    # 那麼應該發送以下通知 - Then should send the following notifications
    Then should send the following notifications:
      | Channel | Content                                                             |
      | SMS     | Your order 550e8400-e29b-41d4-a716-446655440001 shipped, ETA 2 days |
      | Email   | Detailed shipping email with tracking link                          |
      | Push    | Order shipped! Click to track progress                              |
      | In-App  | Shipping notification with full order info                          |
  # 場景: 通知偏好設定 - Scenario: Notification preference settings

  Scenario: Notification preference settings
    # 當客戶 "張小明" 更新通知偏好 - When customer "John" updates notification preferences
    When customer "John" updates notification preferences
    # 並且關閉促銷活動的推播通知 - And disables push notifications for promotions
    And disables push notifications for promotions
    # 並且開啟促銷活動的簡訊通知 - And enables SMS notifications for promotions
    And enables SMS notifications for promotions
    # 那麼系統應該更新客戶的通知偏好 - Then system should update customer notification preferences
    Then system should update customer notification preferences
    # 並且未來的促銷通知應該按新設定發送 - And future promotion notifications should follow new settings
    And future promotion notifications should follow new settings
  # 場景: 通知發送失敗處理 - Scenario: Notification delivery failure handling

  Scenario: Notification delivery failure handling
    # 假設客戶的手機號碼已停用 - Given customer phone number is deactivated
    Given customer phone number is deactivated
    # 當系統嘗試發送簡訊通知 - When system attempts to send SMS notification
    When system attempts to send SMS notification
    # 並且簡訊發送失敗 - And SMS delivery fails
    And SMS delivery fails
    # 那麼系統應該記錄發送失敗 - Then system should log delivery failure
    Then system should log delivery failure
    # 並且嘗試透過其他可用渠道發送 - And attempt to send via other available channels
    And attempt to send via other available channels
    # 並且標記該手機號碼為無效 - And mark phone number as invalid
    And mark phone number as invalid
  # 場景: 通知重試機制 - Scenario: Notification retry mechanism

  Scenario: Notification retry mechanism
    # 假設電子郵件服務暫時不可用 - Given email service is temporarily unavailable
    Given email service is temporarily unavailable
    # 當系統嘗試發送郵件通知失敗 - When system attempts to send email notification and fails
    When system attempts to send email notification and fails
    # 那麼系統應該在 5 分鐘後重試 - Then system should retry after 5 minutes
    Then system should retry after 5 minutes
    # 並且最多重試 3 次 - And retry maximum 3 times
    And retry maximum 3 times
    # 並且如果仍然失敗則記錄到錯誤日誌 - And log to error log if still fails
    And log to error log if still fails
  # 場景: 批量通知發送 - Scenario: Bulk notification sending

  Scenario: Bulk notification sending
    # 假設需要向 10000 名會員發送促銷通知 - Given need to send promotion notification to 10000 members
    Given need to send promotion notification to 10000 members
    # 當系統執行批量通知任務 - When system executes bulk notification task
    When system executes bulk notification task
    # 那麼應該分批發送，每批 100 個 - Then should send in batches of 100
    Then should send in batches of 100
    # 並且控制發送頻率避免超過服務商限制 - And control sending rate to avoid service provider limits
    And control sending rate to avoid service provider limits
    # 並且記錄發送進度和統計 - And record sending progress and statistics
    And record sending progress and statistics
  # 場景: 個人化通知內容 - Scenario: Personalized notification content

  Scenario: Personalized notification content
    # 假設客戶 "張小明" 是 "GOLD" 會員，最近購買了 iPhone - Given customer "John" is "GOLD" member who recently purchased iPhone
    Given customer "John" is "GOLD" member who recently purchased iPhone
    # 當系統發送促銷通知 - When system sends promotion notification
    When system sends promotion notification
    # 那麼通知內容應該個人化 - Then notification content should be personalized
    Then notification content should be personalized:
      | Element          | Content                              |
      | Greeting         | Dear GOLD member Mr. John            |
      | Product Rec      | iPhone Accessories Section           |
      | Exclusive Offer  | GOLD member exclusive 20% off        |
      | Purchase History | Based on your recent iPhone purchase |
  # 場景: 通知時間控制 - Scenario: Notification time control

  Scenario: Notification time control
    # 假設客戶設定不要在晚上 10 點到早上 8 點接收非緊急通知 - Given customer set no non-urgent notifications between 10 PM and 8 AM
    Given customer set no non-urgent notifications between 10 PM and 8 AM
    # 當系統在晚上 11 點嘗試發送促銷通知 - When system attempts to send promotion notification at 11 PM
    When system attempts to send promotion notification at 11 PM
    # 那麼通知應該被延遲到早上 8 點發送 - Then notification should be delayed until 8 AM
    Then notification should be delayed until 8 AM
    # 並且緊急通知（如安全警告）不受時間限制 - And urgent notifications (like security alerts) are not time-restricted
    And urgent notifications are not time-restricted
  # 場景: 通知頻率限制 - Scenario: Notification frequency limit

  Scenario: Notification frequency limit
    # 假設客戶在一天內已收到 5 則促銷通知 - Given customer received 5 promotion notifications in one day
    Given customer received 5 promotion notifications in one day
    # 當系統嘗試發送第 6 則促銷通知 - When system attempts to send 6th promotion notification
    When system attempts to send 6th promotion notification
    # 那麼系統應該檢查頻率限制 - Then system should check frequency limit
    Then system should check frequency limit
    # 並且延遲發送到隔天 - And delay sending to next day
    And delay sending to next day
    # 並且記錄頻率限制日誌 - And log frequency limit record
    And log frequency limit record
