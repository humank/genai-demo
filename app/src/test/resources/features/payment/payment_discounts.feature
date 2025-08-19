# 支付方式折扣 - Payment Method Discounts
Feature: Payment Method Discounts
  As an e-commerce platform
  I want to provide discounts based on different payment methods
  So that I can encourage customers to use specific payment methods

  Background:
    # 假設系統支援以下支付方式和折扣 - Given the system supports the following payment methods and discounts
    Given the system supports the following payment methods and discounts:
      | Payment Method | Discount Type | Discount Value | Min Spend | Max Discount | Status |
      | Credit Card    | None          |             0% |         0 |            0 | Active |
      | Digital Wallet | Percentage    |             5% |      1000 |          500 | Active |
      | Bank Transfer  | Fixed Amount  |            100 |      2000 |          100 | Active |
      | Cash on Del    | None          |             0% |         0 |            0 | Active |
      | Apple Pay      | Percentage    |             3% |       500 |          300 | Active |
      | Google Pay     | Percentage    |             3% |       500 |          300 | Active |
      | Line Pay       | Percentage    |             8% |      1500 |          800 | Active |
    # 並且客戶 "張小明" 的購物車總金額為 5000 元 - And customer "John" cart total is 5000
    And customer "John" cart total is 5000
  # 場景: 數位錢包支付折扣 - Scenario: Digital wallet payment discount

  Scenario: Digital wallet payment discount
    # 當客戶選擇 "Digital Wallet" 支付方式 - When customer selects "Digital Wallet" payment method
    When customer selects "Digital Wallet" payment method
    # 並且購物車金額為 5000 元（滿足最低消費1000元） - And cart amount is 5000 (meets minimum spend 1000)
    And cart amount is 5000 which meets minimum spend 1000
    # 那麼應該享受 5% 折扣 - Then should receive 5% payment discount
    Then should receive 5% payment discount
    # 並且折扣金額應該為 250 元 - And payment discount amount should be 250
    And payment discount amount should be 250
    # 並且最終支付金額應該為 4750 元 - And final payment amount should be 4750
    And final payment amount should be 4750
  # 場景: 數位錢包折扣上限 - Scenario: Digital wallet discount cap

  Scenario: Digital wallet discount cap
    # 當客戶選擇 "Digital Wallet" 支付方式 - When customer selects "Digital Wallet" payment method
    When customer selects "Digital Wallet" payment method
    # 並且購物車金額為 15000 元 - And cart amount is 15000
    And cart amount is 15000
    # 那麼折扣金額應該為 500 元（達到最高折扣限制） - Then discount amount should be 500 (reaches maximum discount limit)
    Then discount amount should be 500 due to maximum limit
    # 並且最終支付金額應該為 14500 元 - And final payment amount should be 14500
    And final payment amount should be 14500
    # 並且應該顯示 "已享受最高折扣優惠" - And payment system should display "Maximum discount applied"
    And payment system should display "Maximum discount applied"
  # 場景: 銀行轉帳固定折扣 - Scenario: Bank transfer fixed discount

  Scenario: Bank transfer fixed discount
    # 當客戶選擇 "Bank Transfer" 支付方式 - When customer selects "Bank Transfer" payment method
    When customer selects "Bank Transfer" payment method
    # 並且購物車金額為 3000 元（滿足最低消費2000元） - And cart amount is 3000 (meets minimum spend 2000)
    And cart amount is 3000 which meets minimum spend 2000
    # 那麼應該享受 100 元固定折扣 - Then should receive 100 fixed discount
    Then should receive 100 fixed discount
    # 並且最終支付金額應該為 2900 元 - And final payment amount should be 2900
    And final payment amount should be 2900
  # 場景: 不滿足最低消費條件 - Scenario: Does not meet minimum spend requirement

  Scenario: Does not meet minimum spend requirement
    # 當客戶選擇 "Digital Wallet" 支付方式 - When customer selects "Digital Wallet" payment method
    When customer selects "Digital Wallet" payment method
    # 並且購物車金額為 800 元（不滿足最低消費1000元） - And cart amount is 800 (does not meet minimum spend 1000)
    And cart amount is 800 which does not meet minimum spend 1000
    # 那麼不應該享受支付折扣 - Then should not receive payment discount
    Then should not receive payment discount
    # 並且最終支付金額應該為 800 元 - And final payment amount should be 800
    And final payment amount should be 800
    # 並且應該顯示 "滿1000元可享受數位錢包5%折扣" - And payment system should display "Spend 1000 or more for 5% digital wallet discount"
    And payment system should display "Spend 1000 or more for 5% digital wallet discount"
  # 場景: Line Pay 高折扣優惠 - Scenario: Line Pay high discount offer

  Scenario: Line Pay high discount offer
    # 當客戶選擇 "Line Pay" 支付方式 - When customer selects "Line Pay" payment method
    When customer selects "Line Pay" payment method
    # 並且購物車金額為 8000 元 - And cart amount is 8000
    And cart amount is 8000
    # 那麼應該享受 8% 折扣 - Then should receive 8% payment discount
    Then should receive 8% payment discount
    # 並且折扣金額應該為 640 元 - And payment discount amount should be 640
    And payment discount amount should be 640
    # 並且最終支付金額應該為 7360 元 - And final payment amount should be 7360
    And final payment amount should be 7360
  # 場景: 支付方式比較 - Scenario: Payment method comparison

  Scenario: Payment method comparison
    # 當客戶在結帳頁面查看支付方式選項 - When customer views payment method options on checkout page
    When customer views payment method options on checkout page
    # 那麼應該顯示各支付方式的折扣資訊 - Then should display payment method comparison
    Then should display payment method comparison:
      | Payment Method | Final Amount | Savings | Recommended |
      | Credit Card    |         5000 |       0 |             |
      | Digital Wallet |         4750 |     250 |             |
      | Bank Transfer  |         4900 |     100 |             |
      | Line Pay       |         4600 |     400 | Best Deal   |
  # 場景: 支付方式折扣與其他優惠疊加 - Scenario: Payment discount stacking with other offers

  Scenario: Payment discount stacking with other offers
    # 假設客戶已應用會員折扣 200 元 - Given customer has already applied member discount of 200
    Given customer has already applied member discount of 200
    # 並且購物車原價為 5000 元，會員折扣後為 4800 元 - And cart original price is 5000, after member discount is 4800
    And cart original price is 5000, after member discount is 4800
    # 當客戶選擇 "Digital Wallet" 支付 - When customer selects "Digital Wallet" payment
    When customer selects "Digital Wallet" payment
    # 那麼支付折扣應該基於會員折扣後的金額計算 - Then payment discount should be calculated based on post-member-discount amount
    Then payment discount should be calculated based on amount after member discount
    # 並且數位錢包折扣為 240 元（4800 × 5%） - And digital wallet discount is 240 (4800 × 5%)
    And digital wallet discount is 240
    # 並且最終支付金額為 4560 元 - And final payment amount is 4560
    And final payment amount is 4560
  # 場景: 特定銀行信用卡額外優惠 - Scenario: Specific bank credit card additional offer

  Scenario: Specific bank credit card additional offer
    # 假設與 "台新銀行" 合作提供額外 2% 折扣 - Given partnership with "Taishin Bank" provides additional 2% discount
    Given partnership with "Taishin Bank" provides additional 2% discount
    # 當客戶選擇 "Taishin Bank Credit Card" 支付 - When customer selects "Taishin Bank Credit Card" payment
    When customer selects "Taishin Bank Credit Card" payment
    # 並且購物車金額為 10000 元 - And cart amount is 10000
    And cart amount is 10000
    # 那麼應該享受 2% 銀行合作折扣 - Then should receive 2% bank partnership discount
    Then should receive 2% bank partnership discount
    # 並且折扣金額應該為 200 元 - And payment discount amount should be 200
    And payment discount amount should be 200
    # 並且最終支付金額應該為 9800 元 - And final payment amount should be 9800
    And final payment amount should be 9800
  # 場景: 分期付款手續費 - Scenario: Installment payment fees

  Scenario: Installment payment fees
    # 假設信用卡分期付款有以下費率 - Given credit card installment has the following rates
    Given credit card installment has the following rates:
      | Installments | Total Fee Rate | Monthly Fee Rate |
      |            3 |           1.5% |             0.5% |
      |            6 |           3.0% |             0.5% |
      |           12 |           6.0% |             0.5% |
    # 當客戶選擇 6 期分期付款，金額 12000 元 - When customer selects 6 installments for amount 12000
    When customer selects 6 installments for amount 12000
    # 那麼總手續費應該為 360 元（12000 × 3%） - Then total fee should be 360 (12000 × 3%)
    Then total fee should be 360
    # 並且每期應付金額應該為 2060 元（(12000+360)/6） - And monthly payment should be 2060 ((12000+360)/6)
    And monthly payment should be 2060
  # 場景: 支付方式限制 - Scenario: Payment method restrictions

  Scenario: Payment method restrictions
    # 假設 "Cash on Delivery" 僅限購物車金額 3000 元以下 - Given "Cash on Delivery" is limited to cart amount under 3000
    Given "Cash on Delivery" is limited to cart amount under 3000
    # 當客戶購物車金額為 5000 元 - When customer cart amount is 5000
    When customer cart amount is 5000
    # 並且嘗試選擇 "Cash on Delivery" - And tries to select "Cash on Delivery"
    And tries to select "Cash on Delivery"
    # 那麼系統應該顯示 "此支付方式限購物車金額3000元以下" - Then payment system should display "This payment method is limited to cart amount under 3000"
    Then payment system should display "This payment method is limited to cart amount under 3000"
    # 並且不允許選擇此支付方式 - And should not allow selecting this payment method
    And should not allow selecting this payment method
