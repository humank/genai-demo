# Original language: zh-TW
Feature: Order Workflow
  As an order system
  I need to manage the complete order lifecycle
  From creation to completion or cancellation

  Background:
    # 假設系統中有可用的商品
    # 並且客戶已經登入系統
    Given there are products available for workflow
    And the customer is logged into the system

  Scenario: Complete order flow - from creation to completion
    # 創建訂單
    # 當客戶瀏覽商品目錄
    # 並且客戶選擇商品 "iPhone 15"
    # 並且客戶將商品添加到訂單中
    # 並且客戶提交訂單
    # 
    # 訂單驗證
    # 那麼系統應該驗證訂單
    # 並且訂單應該有效
    # 
    # 庫存檢查
    # 當系統檢查庫存
    # 並且庫存充足
    # 
    # 支付處理
    # 並且客戶選擇信用卡支付方式
    # 並且客戶輸入有效的信用卡信息
    # 並且支付系統處理支付
    # 並且支付成功
    # 
    # 訂單確認
    # 那麼系統應該確認訂單
    # 並且訂單狀態應該更新為 "已確認"
    # 並且客戶應該收到訂單確認通知
    # 
    # 配送處理
    # 當系統安排配送
    # 並且物流系統建立配送單
    # 並且物流系統分配配送資源
    # 並且物流系統執行配送
    # 
    # 訂單完成
    # 並且客戶收到訂單
    # 並且客戶確認收貨
    # 那麼訂單狀態應該更新為 "已完成"
    # 並且客戶應該能夠評價訂單
    # Order Creation
    When the customer browses the product catalog
    And the customer selects product "iPhone 15"
    And the customer adds the product to the order
    And the customer submits the order
    # Order Validation
    Then the system should validate the order
    And the order should be valid
    # Inventory Check
    When the order system checks inventory
    And the inventory is sufficient
    # Payment Processing
    And the customer selects credit card payment method
    And the customer enters valid credit card information
    And the payment system processes the payment
    And the payment is successful
    # Order Confirmation
    Then the system should confirm the order
    And the workflow order status should be updated to "CONFIRMED"
    And the customer should receive an order confirmation notification
    # Delivery Processing
    When the system arranges workflow delivery
    And the logistics system creates a workflow delivery order
    And the logistics system allocates workflow delivery resources
    And the logistics system executes workflow delivery
    # Order Completion
    And the customer receives the order
    And the customer confirms workflow receipt
    Then the workflow order status should be updated to "COMPLETED"
    And the customer should be able to rate the order

  Scenario: Order flow - cancellation due to insufficient inventory
    # 創建訂單
    # 當客戶創建一個包含商品 "限量版手機" 的訂單
    # 並且客戶提交訂單
    # 
    # 訂單驗證
    # 那麼系統應該驗證訂單
    # 並且訂單應該有效
    # 
    # 庫存檢查
    # 當系統檢查庫存
    # 並且庫存不足
    # 
    # 訂單取消
    # 那麼系統應該取消訂單
    # 並且訂單狀態應該更新為 "已取消"
    # 並且取消原因應該為 "庫存不足"
    # 並且客戶應該收到庫存不足通知
    # Order Creation
    When the customer creates an order containing product "Limited Edition Phone"
    And the customer submits the order
    # Order Validation
    Then the system should validate the order
    And the order should be valid
    # Inventory Check
    When the order system checks inventory
    And the inventory is insufficient
    # Order Cancellation
    Then the system should cancel the workflow order
    And the workflow order status should be updated to "CANCELLED"
    And the cancellation reason should be "Insufficient inventory"
    And the customer should receive an insufficient inventory notification

  Scenario: Order flow - cancellation due to payment failure
    # 創建訂單
    # 當客戶創建一個包含有效商品的訂單
    # 並且客戶提交訂單
    # 
    # 訂單驗證和庫存檢查
    # 那麼系統應該驗證訂單
    # 並且訂單應該有效
    # 當系統檢查庫存
    # 並且庫存充足
    # 
    # 支付處理
    # 並且客戶選擇信用卡支付方式
    # 並且客戶輸入無效的信用卡信息
    # 並且支付系統處理支付
    # 並且支付失敗
    # 
    # 訂單取消
    # 那麼系統應該取消訂單
    # 並且訂單狀態應該更新為 "已取消"
    # 並且取消原因應該為 "支付失敗"
    # 並且客戶應該收到支付失敗通知
    # Order Creation
    When the customer creates an order with valid products
    And the customer submits the order
    # Order Validation and Inventory Check
    Then the system should validate the order
    And the order should be valid
    When the order system checks inventory
    And the inventory is sufficient
    # Payment Processing
    And the customer selects credit card payment method
    And the customer enters invalid credit card information
    And the payment system processes the payment
    And the payment fails
    # Order Cancellation
    Then the system should cancel the workflow order
    And the workflow order status should be updated to "CANCELLED"
    And the cancellation reason should be "Payment failure"
    And the customer should receive a payment failure notification

  Scenario: Order flow - customer initiated cancellation
    # 創建訂單
    # 當客戶創建一個包含有效商品的訂單
    # 並且訂單狀態為 "待支付"
    # 
    # 客戶取消
    # 並且客戶請求取消訂單
    # 並且客戶提供取消原因 "找到更好的選擇"
    # 
    # 訂單取消
    # 那麼系統應該取消訂單
    # 並且訂單狀態應該更新為 "已取消"
    # 並且取消原因應該為 "客戶請求"
    # 並且系統應該釋放預留的庫存
    # 並且客戶應該收到訂單取消確認通知
    # Order Creation
    When the customer creates an order with valid products
    And the workflow order status is "PENDING_PAYMENT"
    # Customer Cancellation
    And the customer requests to cancel the order
    And the customer provides cancellation reason "Found a better option"
    # Order Cancellation
    Then the system should cancel the workflow order
    And the workflow order status should be updated to "CANCELLED"
    And the cancellation reason should be "Customer request"
    And the order system should release the reserved inventory
    And the customer should receive an order cancellation confirmation notification
