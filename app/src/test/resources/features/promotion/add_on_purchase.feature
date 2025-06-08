Feature: Add-on Purchase Promotions
  As an e-commerce platform
  I want to offer add-on purchase options
  So that customers can buy related products at special prices

  Background:
    Given the customer is browsing the online store

  # 加價購優惠
  Scenario: Customer uses add-on purchase
    Given the customer has added a main product "55-inch TV" priced at $1200 to the cart
    And the store offers an add-on product "Soundbar" at a special price of $99 instead of $299
    When the customer adds the "Soundbar" to the cart
    Then the "Soundbar" should be priced at $99

  Scenario: Customer attempts to use add-on purchase without buying the main product
    Given the store offers an add-on product "Soundbar" at a special price of $99 with purchase of "55-inch TV"
    When the customer adds only the "Soundbar" to the cart without the required main product
    Then the "Soundbar" should be priced at the regular price of $299