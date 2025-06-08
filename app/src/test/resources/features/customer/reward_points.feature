Feature: Customer Reward Points
  As an e-commerce platform
  I want to offer a reward points system
  So that customers are incentivized to make repeat purchases

  Background:
    Given the customer is browsing the online store

  # 紅利點數優惠
  Scenario: Customer uses reward points for a discount
    Given the customer has 1000 reward points
    And points can be redeemed at a rate of 10 points = $1
    When the customer chooses to redeem all 1000 points at checkout
    Then $100 should be deducted from the total price
    And the customer should have 0 points remaining

  Scenario: Customer partially redeems reward points
    Given the customer has 1000 reward points
    And points can be redeemed at a rate of 10 points = $1
    When the customer chooses to redeem 500 points at checkout
    Then $50 should be deducted from the total price
    And the customer should have 500 points remaining

  Scenario: Customer attempts to redeem more points than available
    Given the customer has 300 reward points
    When the customer attempts to redeem 500 points at checkout
    Then the system should display an error message
    And no points should be deducted