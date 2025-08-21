Feature: Product Bundle Pricing
  As an e-commerce platform
  I want to offer various product bundle pricing options
  So that customers can save money when buying related products together

  Background:
    Given the customer is browsing the online store

  # 捆綁銷售與組合優惠
  Scenario: Customer buys a designated bundle at a discounted price
    Given the store offers a bundle with the following details
      | Bundle Name        | Items                                      | Regular Total | Bundle Price |
      | Home Appliance Set | Refrigerator, Washing Machine, Microwave   | $5000         | $4200        |
    When the customer adds the "Home Appliance Set" to the cart
    Then the total price should be $4200 instead of $5000

  Scenario: Customer selects items for a "pick any" bundle discount
    Given the store offers "Pick any 3 items from Category A for 12% off"
    When the customer adds the following eligible items from Category A to the cart
      | Product Name | Regular Price |
      | Item A       | $100          |
      | Item B       | $150          |
      | Item C       | $200          |
    Then the total price should be $396 instead of $450

  Scenario: Customer selects more than required items for a "pick any" bundle discount
    Given the store offers "Pick any 3 items from Category A for 12% off"
    When the customer adds 5 eligible items from Category A to the cart
    Then the discount should apply only to the 3 highest priced items
    And the other 2 items should be charged at regular price