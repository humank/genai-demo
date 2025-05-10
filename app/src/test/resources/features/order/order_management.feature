# language: en
# Original language: zh-TW
Feature: Order Management
  # 作為一個客戶
  # 我希望能夠創建和管理訂單
  # 以便購買商品並追蹤訂單狀態
  As a customer
  I want to create and manage orders
  So that I can purchase products and track order status

  Background:
    # 假設系統中有可用的商品
    # 並且我已經登入系統
    Given there are available products in the system
    And I am logged into the system

  Scenario: Successfully create an order
    # 當我瀏覽商品目錄
    # 並且我選擇一個商品
    # 並且我將商品添加到訂單中
    # 並且我提交訂單
    # 那麼系統應該創建一個新訂單
    # 並且我應該收到訂單創建成功的通知
    When I browse the product catalog
    And I select a product
    And I add the product to my order
    And I submit the order
    Then the system should create a new order
    And I should receive an order creation confirmation notification

  Scenario: Add multiple order items
    # 當我創建一個新訂單
    # 並且我添加商品 "iPhone 15" 數量為 1 單價為 30000 元
    # 並且我添加商品 "AirPods Pro" 數量為 2 單價為 5000 元
    # 那麼訂單總金額應該為 40000 元
    # 並且訂單應該包含 2 個項目
    When I create a new order
    And I add product "iPhone 15" with quantity 1 at price 30000
    And I add product "AirPods Pro" with quantity 2 at price 5000
    Then the order total amount should be 40000
    And the order should contain 2 items

  Scenario: Order validation failure
    # 當我創建一個新訂單
    # 並且我沒有添加任何商品
    # 並且我提交訂單
    # 那麼系統應該拒絕訂單
    # 並且我應該收到訂單無效的通知
    # 並且通知應該包含錯誤信息 "訂單必須包含至少一個商品"
    When I create a new order
    And I don't add any products
    And I submit the order
    Then the system should reject the order
    And I should receive an invalid order notification
    And the notification should contain error message "Order must contain at least one product"

  Scenario: Insufficient inventory
    # 當我創建一個新訂單
    # 並且我添加庫存不足的商品 "限量版手機" 數量為 1
    # 並且我提交訂單
    # 那麼系統應該檢查庫存
    # 並且系統應該取消訂單
    # 並且我應該收到庫存不足的通知
    When I create a new order
    And I add a product "Limited Edition Phone" with insufficient inventory with quantity 1
    And I submit the order
    Then the system should check inventory
    And the system should cancel the order
    And I should receive an insufficient inventory notification

  Scenario: Successful payment and delivery arrangement
    # 當我創建一個包含有效商品的訂單
    # 並且我提交訂單
    # 並且系統確認庫存充足
    # 並且我使用有效的信用卡進行支付
    # 那麼支付系統應該處理支付
    # 並且訂單狀態應該更新為 "已確認"
    # 並且系統應該安排配送
    # 並且我應該收到訂單確認通知
    When I create an order with valid products
    And I submit the order
    And the system confirms sufficient inventory
    And I pay with a valid credit card
    Then the payment system should process the payment
    And the order status should be updated to "CONFIRMED"
    And the system should arrange delivery
    And I should receive an order confirmation notification

  Scenario: Payment failure
    # 當我創建一個包含有效商品的訂單
    # 並且我提交訂單
    # 並且系統確認庫存充足
    # 並且我使用無效的信用卡進行支付
    # 那麼支付系統應該拒絕支付
    # 並且訂單狀態應該更新為 "失敗"
    # 並且我應該收到支付失敗通知
    When I create an order with valid products
    And I submit the order
    And the system confirms sufficient inventory
    And I pay with an invalid credit card
    Then the payment system should reject the payment
    And the order status should be updated to "FAILED"
    And I should receive a payment failure notification

  Scenario: Order delivery and completion
    # 假設我已經創建並支付了一個訂單
    # 當物流系統建立配送單
    # 並且物流系統分配配送資源
    # 並且物流系統執行配送
    # 並且我收到訂單
    # 並且我確認收貨
    # 那麼訂單狀態應該更新為 "已完成"
    # 並且我應該能夠評價訂單
    Given I have created and paid for an order
    When the logistics system creates a delivery order
    And the logistics system allocates delivery resources
    And the logistics system executes delivery
    And I receive the order
    And I confirm receipt
    Then the order status should be updated to "COMPLETED"
    And I should be able to rate the order

  Scenario Outline: Order total calculation for different product combinations
    # 當我創建一個新訂單
    # 並且我添加商品 "<商品1>" 數量為 <數量1> 單價為 <單價1> 元
    # 並且我添加商品 "<商品2>" 數量為 <數量2> 單價為 <單價2> 元
    # 那麼訂單總金額應該為 <總金額> 元
    When I create a new order
    And I add product "<product1>" with quantity <quantity1> at price <price1>
    And I add product "<product2>" with quantity <quantity2> at price <price2>
    Then the order total amount should be <total>

    Examples:
      | product1    | quantity1 | price1 | product2      | quantity2 | price2 | total |
      | iPhone 15   | 1         | 30000  | AirPods Pro   | 1         | 5000   | 35000 |
      | MacBook Pro | 1         | 45000  | Magic Mouse   | 1         | 2500   | 47500 |
      | iPad Pro    | 2         | 25000  | Apple Pencil  | 2         | 3500   | 57000 |