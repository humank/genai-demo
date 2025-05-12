@payment
Feature: Payment Aggregate Root
  As a developer
  I want to test the Payment aggregate root functionality
  So that I can ensure the domain model works correctly

  Background:
    Given 建立新的訂單ID

  Scenario: 建立新支付 - 驗證初始狀態
    When 建立支付
      | 金額 | 30000 |
    Then 支付應該成功建立
    And 支付狀態應為 "PENDING"
    And 支付金額應為 30000

  Scenario: 設定支付方式 - 信用卡
    When 建立支付
      | 金額 | 30000 |
    And 設定支付方式為 "CREDIT_CARD"
    Then 支付方式應為 "CREDIT_CARD"

  Scenario: 完成支付流程 - 成功
    When 建立支付
      | 金額 | 30000 |
    And 設定支付方式為 "CREDIT_CARD"
    And 完成支付處理
      | 交易ID | txn-123456 |
    Then 支付狀態應為 "COMPLETED"
    And 交易ID應為 "txn-123456"

  Scenario: 支付失敗處理 - 餘額不足
    When 建立支付
      | 金額 | 30000 |
    And 設定支付方式為 "CREDIT_CARD"
    And 支付處理失敗
      | 失敗原因 | Insufficient funds |
    Then 支付狀態應為 "FAILED"
    And 失敗原因應為 "Insufficient funds"

  Scenario: 退款處理 - 已完成支付
    When 建立支付
      | 金額 | 30000 |
    And 完成支付處理
      | 交易ID | txn-123456 |
    And 申請退款
    Then 支付狀態應為 "REFUNDED"

  Scenario: 退款處理 - 未完成支付
    When 建立支付
      | 金額 | 30000 |
    And 申請退款
    Then 應拋出支付相關異常 "Payment must be in COMPLETED state to refund"

  Scenario: 支付超時處理
    When 建立支付
      | 金額 | 30000 |
    And 支付處理超時
    Then 支付狀態應為 "FAILED"
    And 失敗原因應為 "Payment gateway timeout"
    And 支付應可重試

  Scenario: 重試失敗的支付
    When 建立支付
      | 金額 | 30000 |
    And 支付處理失敗
      | 失敗原因 | Insufficient funds |
    And 重試支付
    Then 支付狀態應為 "PENDING"

  Scenario: 完成非待處理狀態的支付
    When 建立支付
      | 金額 | 30000 |
    And 支付處理失敗
      | 失敗原因 | Insufficient funds |
    And 完成支付處理
      | 交易ID | txn-123456 |
    Then 應拋出支付相關異常 "Payment must be in PENDING state to complete"