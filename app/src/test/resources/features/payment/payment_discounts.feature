Feature: Payment Method Discounts and Cashback
  As an e-commerce platform
  I want to offer discounts and cashback for specific payment methods
  So that customers are incentivized to use preferred payment options

  Background:
    Given the customer is browsing the online store

  # 信用卡與支付回饋
  Scenario: Customer pays with a designated credit card for extra cashback
    Given the store offers 6% cashback for payments with "ABC Bank Card"
    And the maximum cashback per transaction is $500
    When the customer checks out a $10,000 order using "ABC Bank Card"
    Then the customer receives $500 cashback credited within 30 days

  Scenario: Customer pays with a designated credit card for normal cashback
    Given the store offers 6% cashback for payments with "ABC Bank Card"
    And the maximum cashback per transaction is $500
    When the customer checks out a $1,000 order using "ABC Bank Card"
    Then the customer receives $60 cashback credited within 30 days

  Scenario: Customer uses a mobile wallet for instant discount
    Given the store offers a $50 discount for using "Pi Wallet" on orders over $500
    When the customer selects "Pi Wallet" at checkout for a $600 order
    Then $50 is immediately deducted from the total price
    And the final amount charged is $550

  Scenario: Customer splits payment between multiple methods
    Given the store offers a $50 discount for using "Pi Wallet"
    And 6% cashback for payments with "ABC Bank Card"
    When the customer pays $500 with "Pi Wallet" and the remaining $700 with "ABC Bank Card"
    Then $50 is deducted from the total price
    And the customer receives $42 cashback (6% of $700) credited within 30 days