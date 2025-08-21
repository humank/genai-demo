# language: en
# Original language: zh-TW
Feature: Inventory Management
  As an order system
  I need to check and manage product inventory
  So that products in orders have sufficient stock for delivery

  Background:
    # 假設系統中有商品目錄
    # 並且庫存系統正常運行
    Given there is a product catalog in the system
    And the inventory system is functioning properly

  Scenario: Successfully check sufficient inventory
    # 假設商品 "iPhone 15" 的庫存數量為 10
    # 當訂單包含商品 "iPhone 15" 數量為 2
    # 並且系統檢查庫存
    # 那麼庫存檢查結果應該為 "充足"
    # 並且系統應該預留 2 個 "iPhone 15" 的庫存
    # 並且可用庫存數量應該更新為 8
    Given the product "iPhone 15" has an inventory quantity of 10
    When the order contains product "iPhone 15" with quantity 2
    And the inventory system checks inventory
    Then the inventory check result should be "SUFFICIENT"
    And the system should reserve 2 units of "iPhone 15" inventory
    And the available inventory quantity should be updated to 8

  Scenario: Insufficient inventory
    # 假設商品 "限量版手機" 的庫存數量為 1
    # 當訂單包含商品 "限量版手機" 數量為 2
    # 並且系統檢查庫存
    # 那麼庫存檢查結果應該為 "不足"
    # 並且系統不應該預留任何庫存
    # 並且系統應該通知訂單系統庫存不足
    Given the product "Limited Edition Phone" has an inventory quantity of 1
    When the order contains product "Limited Edition Phone" with quantity 2
    And the inventory system checks inventory
    Then the inventory check result should be "INSUFFICIENT"
    And the system should not reserve any inventory
    And the system should notify the order system of insufficient inventory

  Scenario: Multiple product inventory check
    # 假設商品 "iPhone 15" 的庫存數量為 5
    # 並且商品 "AirPods Pro" 的庫存數量為 10
    # 當訂單包含以下商品:
    #   | 商品名稱      | 數量 |
    #   | iPhone 15     | 2    |
    #   | AirPods Pro   | 3    |
    # 並且系統檢查庫存
    # 那麼庫存檢查結果應該為 "充足"
    # 並且系統應該預留所有訂單商品的庫存
    # 並且 "iPhone 15" 的可用庫存數量應該更新為 3
    # 並且 "AirPods Pro" 的可用庫存數量應該更新為 7
    Given the product "iPhone 15" has an inventory quantity of 5
    And the product "AirPods Pro" has an inventory quantity of 10
    When the order contains the following products:
      | Product Name  | Quantity |
      | iPhone 15     | 2        |
      | AirPods Pro   | 3        |
    And the inventory system checks inventory
    Then the inventory check result should be "SUFFICIENT"
    And the system should reserve inventory for all order products
    And the available inventory quantity for "iPhone 15" should be updated to 3
    And the available inventory quantity for "AirPods Pro" should be updated to 7

  Scenario: Inventory reservation timeout
    # 假設商品 "iPhone 15" 的庫存數量為 5
    # 並且系統已為訂單預留 2 個 "iPhone 15"
    # 當預留時間超過 30 分鐘
    # 並且訂單仍未支付
    # 那麼系統應該釋放預留的庫存
    # 並且 "iPhone 15" 的可用庫存數量應該更新為 5
    Given the product "iPhone 15" has an inventory quantity of 5
    And the system has reserved 2 units of "iPhone 15" for an order
    When the reservation time exceeds 30 minutes
    And the order is still not paid
    Then the system should release the reserved inventory
    And the available inventory quantity for "iPhone 15" should be updated to 5

  Scenario: Release inventory after order cancellation
    # 假設商品 "iPhone 15" 的庫存數量為 10
    # 並且系統已為訂單預留 2 個 "iPhone 15"
    # 並且可用庫存數量為 8
    # 當訂單被取消
    # 那麼系統應該釋放預留的庫存
    # 並且 "iPhone 15" 的可用庫存數量應該更新為 10
    Given the product "iPhone 15" has an inventory quantity of 10
    And the system has reserved 2 units of "iPhone 15" for an order
    And the available inventory quantity is 8
    When the order is canceled
    Then the system should release the reserved inventory
    And the available inventory quantity for "iPhone 15" should be updated to 10

  Scenario: Inventory threshold warning
    # 假設商品 "iPhone 15" 的庫存閾值設置為 5
    # 當商品 "iPhone 15" 的庫存數量降至 5 以下
    # 那麼系統應該生成庫存警告
    # 並且庫存管理員應該收到補貨通知
    Given the inventory threshold for product "iPhone 15" is set to 5
    When the inventory quantity for "iPhone 15" drops below 5
    Then the system should generate an inventory warning
    And the inventory manager should receive a restocking notification

  Scenario: Inventory synchronization
    # 假設外部倉庫系統更新了商品庫存
    # 當庫存同步任務執行
    # 那麼系統應該從外部倉庫系統獲取最新庫存數據
    # 並且系統應該更新本地庫存記錄
    # 並且庫存歷史記錄應該包含同步事件
    Given the external warehouse system has updated product inventory
    When the inventory synchronization task runs
    Then the system should fetch the latest inventory data from the external warehouse system
    And the system should update the local inventory records
    And the inventory history should include the synchronization event