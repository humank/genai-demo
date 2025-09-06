<!-- This document needs manual translation from Chinese to English -->
<!-- 此文檔需要從中文手動翻譯為英文 -->

```gherkin
Feature: E-commerce Product Pricing and Promotion Rules

  Background:
    Given the customer is browsing the online store

  # 1. 捆綁銷售與組合優惠
  # 明確定義捆綁商品內容、原價與折扣價格
  Scenario: Customer buys a designated bundle at a discounted price
    Given the store offers a bundle with the following details
      | Bundle Name        | Items                                      | Regular Total | Bundle Price |
      | Home Appliance Set | Refrigerator, Washing Machine, Microwave   | $5000         | $4200        |
    When the customer adds the "Home Appliance Set" to the cart
    Then the total price should be $4200 instead of $5000

  # 明確定義「任選N件」的處理邏輯
  Scenario: Customer selects items for a "pick any" bundle discount
    Given the store offers "Pick any 3 items from Category A for 12% off"
    When the customer adds the following eligible items from Category A to the cart
      | Product Name | Regular Price |
      | Item A       | $100          |
      | Item B       | $150          |
      | Item C       | $200          |
    Then the total price should be $396 instead of $450

  # 明確定義超過指定數量的處理方式
  Scenario: Customer selects more than required items for a "pick any" bundle discount
    Given the store offers "Pick any 3 items from Category A for 12% off"
    When the customer adds 5 eligible items from Category A to the cart
    Then the discount should apply only to the 3 highest priced items
    And the other 2 items should be charged at regular price

  # 明確定義加價購的條件與價格
  Scenario: Customer uses add-on purchase (加價購)
    Given the customer has added a main product "55-inch TV" priced at $1200 to the cart
    And the store offers an add-on product "Soundbar" at a special price of $99 instead of $299
    When the customer adds the "Soundbar" to the cart
    Then the "Soundbar" should be priced at $99

  # 明確定義加價購的限制條件
  Scenario: Customer attempts to use add-on purchase without buying the main product
    Given the store offers an add-on product "Soundbar" at a special price of $99 with purchase of "55-inch TV"
    When the customer adds only the "Soundbar" to the cart without the required main product
    Then the "Soundbar" should be priced at the regular price of $299

  # 明確定義滿額贈禮的具體內容
  Scenario: Customer qualifies for a gift with purchase
    Given the store offers a free "Bluetooth Speaker" (valued at $50) for purchases over $2000
    When the customer's cart total reaches $2100
    Then the customer should automatically receive the "Bluetooth Speaker" in their cart
    And the gift item should be marked as $0

  # 明確定義滿額贈禮的數量限制
  Scenario: Customer qualifies for multiple gifts with large purchase
    Given the store offers one free "Bluetooth Speaker" for every $2000 spent, up to 3 speakers
    When the customer's cart total reaches $6500
    Then the customer should receive 3 "Bluetooth Speaker" items in their cart
    And all gift items should be marked as $0

  # 2. 限時限量與閃購活動
  # 明確定義時區與時間處理
  Scenario: Customer purchases during a flash sale
    Given a product "Wireless Earbuds" is on flash sale from 12:00 to 14:00 (GMT+8) at $79 instead of $129
    When the customer checks out at 12:30 (GMT+8)
    Then the "Wireless Earbuds" should be priced at $79

  # 明確定義時間邊界條件
  Scenario: Customer attempts to purchase just before a flash sale starts
    Given a product "Wireless Earbuds" is on flash sale from 12:00 to 14:00 (GMT+8) at $79 instead of $129
    When the customer checks out at 11:59 (GMT+8)
    Then the "Wireless Earbuds" should be priced at the regular price of $129

  # 明確定義限量商品的判定機制
  Scenario: Customer attempts to buy a limited-quantity deal
    Given a product "Power Bank" has a limited quantity of 100 units at a special price of $19 instead of $39
    When the customer completes payment and is the 75th customer to do so
    Then the customer receives the special price of $19

  # 明確定義限量商品售罄後的處理
  Scenario: Customer attempts to buy a limited-quantity deal after it's sold out
    Given a product "Power Bank" has a limited quantity of 100 units at a special price of $19
    And 100 units have already been sold at the special price
    When the customer attempts to check out with the "Power Bank"
    Then the customer is informed the deal is no longer available
    And the "Power Bank" is offered at the regular price of $39

  # 3. 會員與紅利點數優惠
  # 明確定義點數兌換規則與部分兌換
  Scenario: Customer uses reward points for a discount
    Given the customer has 1000 reward points
    And points can be redeemed at a rate of 10 points = $1
    When the customer chooses to redeem all 1000 points at checkout
    Then $100 should be deducted from the total price
    And the customer should have 0 points remaining

  # 明確定義部分點數兌換
  Scenario: Customer partially redeems reward points
    Given the customer has 1000 reward points
    And points can be redeemed at a rate of 10 points = $1
    When the customer chooses to redeem 500 points at checkout
    Then $50 should be deducted from the total price
    And the customer should have 500 points remaining

  # 明確定義點數不足的處理
  Scenario: Customer attempts to redeem more points than available
    Given the customer has 300 reward points
    When the customer attempts to redeem 500 points at checkout
    Then the system should display an error message
    And no points should be deducted

  # 明確定義「新會員」的標準
  Scenario: New member receives a first purchase discount
    Given the customer registered within the last 30 days
    And has not made any previous purchases
    When the customer makes their first purchase
    Then a 15% discount should be applied to the order total
    And the discount should be labeled as "New Member Discount"

  # 明確定義生日優惠的折扣幅度與條件
  Scenario: Member receives a birthday month discount
    Given the customer is a member with birthdate in the current month
    When the customer makes a purchase
    Then a 10% birthday discount should be applied to the order
    And the discount should be labeled as "Birthday Month Discount"
    And the discount should be capped at $100

  # 明確定義優惠疊加規則
  Scenario: Member with multiple eligible discounts
    Given the customer is eligible for both a 10% birthday discount and a 15% new member discount
    When the customer makes a purchase
    Then only the higher discount of 15% should be applied
    And the discount should be labeled as "New Member Discount"

  # 4. 平台手續費與活動檔期差異
  # 明確定義手續費率差異與通知機制
  Scenario: Seller participates in a promotional event with higher commission
    Given the normal commission rate for electronics is 3%
    And during the "Summer Sale" event the commission increases to 5%
    When the seller's product is sold during the "Summer Sale" event
    Then the platform deducts 5% commission from the sale price
    And the seller is notified of the higher commission rate 7 days before the event

  # 明確定義不同類別的手續費率
  Scenario: Different commission rates for product categories
    Given the commission rates for different categories are as follows
      | Category    | Normal Rate | Event Rate |
      | Electronics | 3%          | 5%         |
      | Fashion     | 5%          | 8%         |
      | Groceries   | 2%          | 3%         |
    When a seller's "Fashion" product is sold during a promotional event
    Then the platform deducts 8% commission from the sale price

  # 5. 超商線上優惠組合（以7-11為例）
  # 明確定義兌換券的有效期限與內容
  Scenario: Customer buys a value meal voucher
    Given the store offers a "45元超值餐" voucher for $45
    When the customer purchases the voucher online
    Then the customer receives a digital voucher with the following details
      | Voucher Type | Price | Valid Period | Redemption Location | Contents                    |
      | 超值餐       | $45   | 90 days      | Any 7-11 in Taiwan  | 御飯糰 + 大杯咖啡          |

  # 明確定義多杯組合的價格與有效期
  Scenario: Customer buys a multi-cup beverage combo
    Given the store offers a "7-cup coffee combo" at $199 instead of the regular $280
    When the customer purchases the combo
    Then the customer receives 7 beverage vouchers valid for 90 days
    And each voucher has a unique redemption code
    And vouchers can be redeemed for any medium-sized coffee at 7-11

  # 明確定義兌換券遺失的處理方式
  Scenario: Customer reports a lost voucher
    Given the customer has purchased a "7-cup coffee combo"
    When the customer reports a lost voucher through the customer service
    And provides the original purchase receipt
    Then the lost voucher is invalidated
    And a replacement voucher is issued with a new redemption code
    And the replacement voucher inherits the original expiration date

  # 6. 信用卡與支付回饋
  # 明確定義現金回饋的結算時間與上限
  Scenario: Customer pays with a designated credit card for extra cashback
    Given the store offers 6% cashback for payments with "ABC Bank Card"
    And the maximum cashback per transaction is $500
    When the customer checks out a $10,000 order using "ABC Bank Card"
    Then the customer receives $500 cashback credited within 30 days

  # 明確定義一般交易的回饋計算
  Scenario: Customer pays with a designated credit card for normal cashback
    Given the store offers 6% cashback for payments with "ABC Bank Card"
    And the maximum cashback per transaction is $500
    When the customer checks out a $1,000 order using "ABC Bank Card"
    Then the customer receives $60 cashback credited within 30 days

  # 明確定義行動支付的即時折扣
  Scenario: Customer uses a mobile wallet for instant discount
    Given the store offers a $50 discount for using "Pi Wallet" on orders over $500
    When the customer selects "Pi Wallet" at checkout for a $600 order
    Then $50 is immediately deducted from the total price
    And the final amount charged is $550

  # 明確定義多種支付方式組合的處理邏輯
  Scenario: Customer splits payment between multiple methods
    Given the store offers a $50 discount for using "Pi Wallet"
    And 6% cashback for payments with "ABC Bank Card"
    When the customer pays $500 with "Pi Wallet" and the remaining $700 with "ABC Bank Card"
    Then $50 is deducted from the total price
    And the customer receives $42 cashback (6% of $700) credited within 30 days
```
這份更新後的 Gherkin feature file 以 BDD 方式涵蓋了台灣電商常見的價格組合與促銷規則，並明確定義了各種場景的具體參數和邊界條件，方便用於自動化測試或需求溝通。

Sources
