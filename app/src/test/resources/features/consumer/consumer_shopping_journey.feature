# Feature: Consumer Shopping Journey
#   As a consumer
#   I want to complete the full shopping process
#   So that I can purchase the products I need
  # Background:
  #   Given the following products exist in the system:
  #     | Product ID | Product Name        | Price | Stock |
  #     | PROD-001   | iPhone 15 Pro Max   | 35900 |    50 |
  #     | PROD-002   | Samsung Galaxy S24  | 28900 |    40 |
  #     | PROD-005   | AirPods Pro 3rd Gen |  8990 |   100 |
  #   And customer "John Doe" exists with ID "660e8400-e29b-41d4-a716-446655440001"
  #   And customer "John Doe" has membership level "STANDARD"
  #   And customer "John Doe" has reward points 1500
  # Scenario: Complete shopping flow
  #   Given I am customer "John Doe"
  #   When I browse the product catalog
  #   Then I should see the available product list
  #   When I search for "iPhone"
  #   Then I should see search results containing "iPhone 15 Pro Max"
  #   When I view product "PROD-001" details
  #   Then I should see product name as "iPhone 15 Pro Max"
  #   And I should see price as 35900
  #   When I add 1 "PROD-001" to cart
  #   Then my cart should contain 1 item
  #   And cart total should be 35900
  #   When I add 1 "PROD-005" to cart
  #   Then my cart should contain 2 items
  #   When I view cart
  #   Then I should see the following items:
  #     | Product ID | Quantity | Unit Price | Subtotal |
  #     | PROD-001   |        1 |      35900 |    35900 |
  #     | PROD-005   |        1 |       8990 |     8990 |
