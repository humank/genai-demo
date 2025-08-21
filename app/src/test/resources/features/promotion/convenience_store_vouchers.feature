Feature: Convenience Store Vouchers
  As an e-commerce platform
  I want to offer convenience store vouchers and combos
  So that customers can purchase physical goods for redemption at convenience stores

  Background:
    Given the customer is browsing the online store

  # 超商線上優惠組合
  Scenario: Customer buys a value meal voucher
    Given the store offers a "45元超值餐" voucher for $45
    When the customer purchases the voucher online
    Then the customer receives a digital voucher with the following details
      | Voucher Type | Price | Valid Period | Redemption Location | Contents                    |
      | 超值餐       | $45   | 90 days      | Any 7-11 in Taiwan  | 御飯糰 + 大杯咖啡          |

  Scenario: Customer buys a multi-cup beverage combo
    Given the store offers a "7-cup coffee combo" at $199 instead of the regular $280
    When the customer purchases the combo
    Then the customer receives 7 beverage vouchers valid for 90 days
    And each voucher has a unique redemption code
    And vouchers can be redeemed for any medium-sized coffee at 7-11

  Scenario: Customer reports a lost voucher
    Given the customer has purchased a "7-cup coffee combo"
    When the customer reports a lost voucher through the customer service
    And provides the original purchase receipt
    Then the lost voucher is invalidated
    And a replacement voucher is issued with a new redemption code
    And the replacement voucher inherits the original expiration date