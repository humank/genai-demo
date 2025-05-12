Feature: Order Aggregate Root
  As a developer
  I want to test the Order aggregate root functionality
  So that I can ensure the domain model works correctly

  
  Scenario: Create a new order
    When 創建一個新訂單，客戶ID為 "customer-123"，配送地址為 "台北市信義區"
    Then 訂單應該被成功創建
    And 訂單狀態應為 "CREATED"
    And 訂單總金額應為 0

  
  Scenario: Add items to an order
    Given 已創建一個訂單，客戶ID為 "customer-123"，配送地址為 "台北市信義區"
    When 添加產品 "iPhone 15" 到訂單，數量為 2，單價為 35000
    And 添加產品 "AirPods Pro" 到訂單，數量為 1，單價為 7500
    Then 訂單總金額應為 77500
    And 訂單項目數量應為 2

  
  Scenario: Submit an order
    Given 已創建一個訂單，客戶ID為 "customer-123"，配送地址為 "台北市信義區"
    And 添加產品 "iPhone 15" 到訂單，數量為 1，單價為 35000
    When 提交訂單
    Then 訂單狀態應為 "PENDING"

  
  Scenario: Cancel an order
    Given 已創建一個訂單，客戶ID為 "customer-123"，配送地址為 "台北市信義區"
    And 添加產品 "iPhone 15" 到訂單，數量為 1，單價為 35000
    When 取消訂單
    Then 訂單狀態應為 "CANCELLED"

  
  Scenario: Apply discount to an order
    Given 已創建一個訂單，客戶ID為 "customer-123"，配送地址為 "台北市信義區"
    And 添加產品 "MacBook Pro" 到訂單，數量為 1，單價為 58000
    When 應用固定金額折扣 5000 到訂單
    Then 訂單總金額應為 53000
    And 訂單折扣金額應為 5000

  
  Scenario: Validate order with no items
    Given 已創建一個訂單，客戶ID為 "customer-123"，配送地址為 "台北市信義區"
    When 提交訂單
    Then 應拋出異常，錯誤信息為 "Cannot submit an order with no items"

  
  Scenario: Validate order with excessive total amount
    Given 已創建一個訂單，客戶ID為 "customer-123"，配送地址為 "台北市信義區"
    When 添加產品 "超貴產品" 到訂單，數量為 1，單價為 1000000
    And 提交訂單
    Then 應拋出異常，錯誤信息為 "訂單總金額超過允許的最大值"