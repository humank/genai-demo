Feature: Gift with Purchase Promotions
  As an e-commerce platform
  I want to offer free gifts with qualifying purchases
  So that customers are incentivized to spend more

  Background:
    Given the customer is browsing the online store

  # 滿額贈禮
  Scenario: Customer qualifies for a gift with purchase
    Given the store offers a free "Bluetooth Speaker" (valued at $50) for purchases over $2000
    When the customer's cart total reaches $2100
    Then the customer should automatically receive the "Bluetooth Speaker" in their cart
    And the gift item should be marked as $0

  Scenario: Customer qualifies for multiple gifts with large purchase
    Given the store offers one free "Bluetooth Speaker" for every $2000 spent, up to 3 speakers
    When the customer's cart total reaches $6500
    Then the customer should receive 3 "Bluetooth Speaker" items in their cart
    And all gift items should be marked as $0