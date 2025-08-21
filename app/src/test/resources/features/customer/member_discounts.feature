Feature: Member Discounts
  As an e-commerce platform
  I want to offer special discounts to members
  So that customers are incentivized to join our membership program

  Background:
    Given the customer is browsing the online store

  # 會員優惠
  Scenario: New member receives a first purchase discount
    Given the customer registered within the last 30 days
    And has not made any previous purchases
    When the customer makes their first purchase
    Then a 15% discount should be applied to the order total
    And the discount should be labeled as "New Member Discount"

  Scenario: Member receives a birthday month discount
    Given the customer is a member with birthdate in the current month
    When the customer makes a purchase
    Then a 10% birthday discount should be applied to the order
    And the discount should be labeled as "Birthday Month Discount"
    And the discount should be capped at $100

  Scenario: Member with multiple eligible discounts
    Given the customer is eligible for both a 10% birthday discount and a 15% new member discount
    When the customer makes a purchase
    Then only the higher discount of 15% should be applied
    And the discount should be labeled as "New Member Discount"