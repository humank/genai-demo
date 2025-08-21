Feature: Flash Sale and Limited Quantity Promotions
  As an e-commerce platform
  I want to offer time-limited and quantity-limited promotions
  So that customers are motivated to make quick purchase decisions

  Background:
    Given the customer is browsing the online store

  # 限時特價
  Scenario: Customer purchases during a flash sale
    Given a product "Wireless Earbuds" is on flash sale from 12:00 to 14:00 (GMT+8) at $79 instead of $129
    When the customer checks out at 12:30 (GMT+8)
    Then the "Wireless Earbuds" should be priced at $79

  Scenario: Customer attempts to purchase just before a flash sale starts
    Given a product "Wireless Earbuds" is on flash sale from 12:00 to 14:00 (GMT+8) at $79 instead of $129
    When the customer checks out at 11:59 (GMT+8)
    Then the "Wireless Earbuds" should be priced at the regular price of $129

  # 限量特價
  Scenario: Customer attempts to buy a limited-quantity deal
    Given a product "Power Bank" has a limited quantity of 100 units at a special price of $19 instead of $39
    When the customer completes payment and is the 75th customer to do so
    Then the customer receives the special price of $19

  Scenario: Customer attempts to buy a limited-quantity deal after it's sold out
    Given a product "Power Bank" has a limited quantity of 100 units at a special price of $19
    And 100 units have already been sold at the special price
    When the customer attempts to check out with the "Power Bank"
    Then the customer is informed the deal is no longer available
    And the "Power Bank" is offered at the regular price of $39