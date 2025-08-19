Feature: Order Aggregate Root
  As a developer
  I want to test the Order aggregate root functionality
  So that I can ensure the domain model works correctly

  Scenario: Create a new order
    When create a new order with customer ID "customer-123" and shipping address "台北市信義區"
    Then order should be successfully created
    And order status should be "CREATED"
    And order total amount should be 0

  Scenario: Add items to an order
    Given an order has been created with customer ID "customer-123" and shipping address "台北市信義區"
    When add product "iPhone 15" to order with quantity 2 and unit price 35000
    And add product "AirPods Pro" to order with quantity 1 and unit price 7500
    Then order total amount should be 77500
    And order item count should be 2

  Scenario: Submit an order
    Given an order has been created with customer ID "customer-123" and shipping address "台北市信義區"
    And add product "iPhone 15" to order with quantity 1 and unit price 35000
    When submit order
    Then order status should be "PENDING"

  Scenario: Cancel an order
    Given an order has been created with customer ID "customer-123" and shipping address "台北市信義區"
    And add product "iPhone 15" to order with quantity 1 and unit price 35000
    When cancel order
    Then order status should be "CANCELLED"

  Scenario: Apply discount to an order
    Given an order has been created with customer ID "customer-123" and shipping address "台北市信義區"
    And add product "MacBook Pro" to order with quantity 1 and unit price 58000
    When apply fixed amount discount 5000 to order
    Then order total amount should be 53000
    And order discount amount should be 5000

  Scenario: Validate order with no items
    Given an order has been created with customer ID "customer-123" and shipping address "台北市信義區"
    When submit order
    Then should throw exception with error message "Cannot submit an order with no items"

  Scenario: Validate order with excessive total amount
    Given an order has been created with customer ID "customer-123" and shipping address "台北市信義區"
    When add product "超貴產品" to order with quantity 1 and unit price 1000000
    And submit order
    Then should throw exception with error message "訂單總金額超過允許的最大值"
