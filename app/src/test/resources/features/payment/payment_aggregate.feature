@payment
Feature: Payment Aggregate Root
  As a developer
  I want to test the Payment aggregate root functionality
  So that I can ensure the domain model works correctly

  Background:
    Given create new order ID

  Scenario: Create new payment - verify initial state
    When create payment
      | Amount | 30000 |
    Then payment should be successfully created
    And payment status should be "PENDING"
    And payment amount should be 30000

  Scenario: Set payment method - credit card
    When create payment
      | Amount | 30000 |
    And set payment method to "CREDIT_CARD"
    Then payment method should be "CREDIT_CARD"

  Scenario: Complete payment process - success
    When create payment
      | Amount | 30000 |
    And set payment method to "CREDIT_CARD"
    And complete payment processing
      | Transaction ID | txn-123456 |
    Then payment status should be "COMPLETED"
    And transaction ID should be "txn-123456"

  Scenario: Payment failure handling - insufficient funds
    When create payment
      | Amount | 30000 |
    And set payment method to "CREDIT_CARD"
    And payment processing fails
      | Failure Reason | Insufficient funds |
    Then payment status should be "FAILED"
    And failure reason should be "Insufficient funds"

  Scenario: Refund processing - completed payment
    When create payment
      | Amount | 30000 |
    And complete payment processing
      | Transaction ID | txn-123456 |
    And request refund
    Then payment status should be "REFUNDED"

  Scenario: Refund processing - uncompleted payment
    When create payment
      | Amount | 30000 |
    And request refund
    Then should throw payment exception "Payment must be in COMPLETED state to refund"

  Scenario: Payment timeout handling
    When create payment
      | Amount | 30000 |
    And payment processing times out
    Then payment status should be "FAILED"
    And failure reason should be "Payment gateway timeout"
    And payment should be retryable

  Scenario: Retry failed payment
    When create payment
      | Amount | 30000 |
    And payment processing fails
      | Failure Reason | Insufficient funds |
    And retry payment
    Then payment status should be "PENDING"

  Scenario: Complete non-pending payment
    When create payment
      | Amount | 30000 |
    And payment processing fails
      | Failure Reason | Insufficient funds |
    And complete payment processing
      | Transaction ID | txn-123456 |
    Then should throw payment exception "Payment must be in PENDING state to complete"
