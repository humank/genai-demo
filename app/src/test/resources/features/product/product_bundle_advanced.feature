# 進階產品組合銷售 - Advanced Product Bundle Sales
Feature: Advanced Product Bundle Sales
  As an e-commerce platform
  I want to provide diverse product bundle sales solutions
  So that I can increase customer purchase value and platform revenue

  Background:
    # 假設系統中存在以下商品 - Given the following products exist in the system
    Given the following products exist in the system:
      | Product ID | Product Name       | Category  | Price | Stock |
      | PROD-001   | iPhone 15 Pro Max  | Phone     | 35900 |    50 |
      | PROD-002   | AirPods Pro        | Headset   |  8990 |   100 |
      | PROD-003   | iPhone Case        | Accessory |   990 |   200 |
      | PROD-004   | Wireless Charger   | Accessory |  1990 |   150 |
      | PROD-005   | MacBook Pro M3     | Laptop    | 58000 |    20 |
      | PROD-006   | Magic Mouse        | Accessory |  2990 |    80 |
      | PROD-007   | Laptop Bag         | Accessory |  1500 |   100 |
      | PROD-008   | Samsung Galaxy S24 | Phone     | 28900 |    40 |
  # 場景: 固定組合套餐 - Scenario: Fixed bundle package

  Scenario: Fixed bundle package
    # 假設存在 "iPhone 完整套餐" 組合 - Given "iPhone Complete Package" bundle exists
    Given "iPhone Complete Package" bundle exists:
      | Product ID | Quantity | Original Price |
      | PROD-001   |        1 |          35900 |
      | PROD-002   |        1 |           8990 |
      | PROD-003   |        1 |            990 |
      | PROD-004   |        1 |           1990 |
    # 並且組合價格為 45000 元 (原價 47870 元) - And bundle price is 45000 (original 47870)
    And bundle price is 45000 with original price 47870
    # 當客戶購買 "iPhone 完整套餐" - When customer purchases "iPhone Complete Package"
    When customer purchases "iPhone Complete Package"
    # 那麼購物車應該包含所有組合商品 - Then cart should contain all bundle products
    Then cart should contain all bundle products
    # 並且總價格應該為 45000 元 - And total price should be 45000
    And total price should be 45000
    # 並且節省金額應該為 2870 元 - And savings amount should be 2870
    And savings amount should be 2870
  # 場景: 任選組合優惠 - 滿足條件 - Scenario: Pick any bundle discount - meets criteria

  Scenario: Pick any bundle discount - meets criteria
    # 假設存在 "任選 3 件配件 8 折" 的促銷活動 - Given "Pick any 3 accessories 20% off" promotion exists
    Given "Pick any 3 accessories 20% off" promotion exists
    # 並且適用於分類 "Accessory" 的所有商品 - And applies to all products in "Accessory" category
    And applies to all products in "Accessory" category
    # 當客戶選擇以下配件 - When customer selects the following accessories
    When customer selects the following accessories:
      | Product ID | Quantity | Unit Price |
      | PROD-003   |        1 |        990 |
      | PROD-004   |        1 |       1990 |
      | PROD-006   |        1 |       2990 |
    # 那麼應該享受 8 折優惠 - Then should receive 20% discount
    Then should receive 20% discount
    # 並且原價總計應該為 5970 元 - And original total should be 5970
    And original total should be 5970
    # 並且優惠後價格應該為 4776 元 - And discounted price should be 4776
    And discounted price should be 4776
    # 並且節省金額應該為 1194 元 - And savings amount should be 1194
    And savings amount should be 1194
  # 場景: 任選組合優惠 - 超過數量 - Scenario: Pick any bundle discount - exceeds quantity

  Scenario: Pick any bundle discount - exceeds quantity
    # 假設存在 "任選 3 件配件 8 折" 的促銷活動 - Given "Pick any 3 accessories 20% off" promotion exists
    Given "Pick any 3 accessories 20% off" promotion exists
    # 當客戶選擇 5 件配件商品 - When customer selects 5 accessory products
    When customer selects 5 accessory products
    # 那麼優惠應該應用於價格最高的 3 件商品 - Then discount should apply to highest priced 3 items
    Then discount should apply to highest priced 3 items
    # 並且其餘 2 件商品按原價計算 - And remaining 2 items should be charged at original price
    And remaining 2 items should be charged at original price
    # 並且系統應該顯示優惠詳情 - And system should display discount details
    And system should display discount details
  # 場景: 階梯式組合優惠 - Scenario: Tiered bundle discount

  Scenario: Tiered bundle discount
    # 假設存在以下階梯優惠 - Given the following tiered discount exists
    Given the following tiered discount exists:
      | Quantity | Discount |
      |        2 |      10% |
      |        3 |      20% |
      |        5 |      30% |
    # 並且適用於 "Phone" 分類 - And applies to "Phone" category
    And applies to "Phone" category
    # 當客戶購買 3 支手機，總價 103700 元 - When customer purchases 3 phones with total 103700
    When customer purchases 3 phones with total 103700
    # 那麼應該享受 8 折優惠 - Then should receive 20% discount
    Then should receive 20% discount
    # 並且優惠後價格應該為 82960 元 - And discounted price should be 82960
    And discounted price should be 82960
  # 場景: 主商品搭配優惠 - Scenario: Main product accessory discount

  Scenario: Main product accessory discount
    # 假設購買 "PROD-001" (iPhone) 時，配件享受特價 - Given when purchasing "PROD-001" (iPhone), accessories get special price
    Given when purchasing "PROD-001" accessories get special price:
      | Accessory ID | Original Price | Special Price |
      | PROD-002     |           8990 |          7990 |
      | PROD-003     |            990 |           690 |
      | PROD-004     |           1990 |          1490 |
    # 當客戶購買 1 個 "PROD-001" 和 1 個 "PROD-002" - When customer purchases 1 "PROD-001" and 1 "PROD-002"
    When customer purchases 1 "PROD-001" and 1 "PROD-002"
    # 那麼 "PROD-001" 應該按原價 35900 元計算 - Then "PROD-001" should be charged at original price 35900
    Then "PROD-001" should be charged at original price 35900
    # 並且 "PROD-002" 應該按特價 7990 元計算 - And "PROD-002" should be charged at special price 7990
    And "PROD-002" should be charged at special price 7990
    # 並且總價格應該為 43890 元 - And total price should be 43890
    And total price should be 43890
  # 場景: 滿額贈品組合 - Scenario: Spend threshold gift bundle

  Scenario: Spend threshold gift bundle
    # 假設購買滿 50000 元贈送 "PROD-003" (保護殼) - Given spend 50000 or more gets free "PROD-003" (Case)
    Given spend 50000 or more gets free "PROD-003"
    # 當客戶購買總價 52000 元的商品 - When customer purchases products totaling 52000
    When customer purchases products totaling 52000
    # 那麼應該自動添加贈品 "PROD-003" 到購物車 - Then should automatically add free gift "PROD-003" to cart
    Then should automatically add free gift "PROD-003" to cart
    # 並且贈品價格應該為 0 元 - And gift price should be 0
    And gift price should be 0
    # 並且應該顯示 "滿額贈品" 標籤 - And should display "Free Gift" label
    And should display "Free Gift" label
  # 場景: 買一送一組合 - Scenario: Buy one get one bundle

  Scenario: Buy one get one bundle
    # 假設 "PROD-003" (保護殼) 有買一送一活動 - Given "PROD-003" (Case) has buy one get one promotion
    Given "PROD-003" has buy one get one promotion
    # 當客戶購買 1 個 "PROD-003" - When customer purchases 1 "PROD-003"
    When customer purchases 1 "PROD-003"
    # 那麼應該自動添加 1 個相同商品作為贈品 - Then should automatically add 1 same product as free gift
    Then should automatically add 1 same product as free gift
    # 並且購物車應該顯示 2 個 "PROD-003" - And cart should display 2 "PROD-003"
    And cart should display 2 "PROD-003"
    # 並且只收取 1 個的費用 - And should only charge for 1 unit
    And should only charge for 1 unit
  # 場景: 交叉銷售推薦 - Scenario: Cross-selling recommendations

  Scenario: Cross-selling recommendations
    # 假設客戶購買了 "PROD-001" (iPhone) - Given customer purchased "PROD-001" (iPhone)
    Given customer purchased "PROD-001"
    # 當客戶查看購物車 - When customer views cart
    When customer views cart
    # 那麼系統應該推薦相關配件 - Then system should recommend related accessories
    Then system should recommend related accessories:
      | Product ID | Reason                     | Special Price |
      | PROD-002   | Perfect match headphones   |          7990 |
      | PROD-003   | Protect your investment    |           690 |
      | PROD-004   | Convenient wireless charge |          1490 |
  # 場景: 組合庫存檢查 - Scenario: Bundle stock validation

  Scenario: Bundle stock validation
    # 假設 "iPhone 完整套餐" 需要 - Given "iPhone Complete Package" requires
    Given "iPhone Complete Package" requires:
      | Product ID | Required Qty | Available Stock |
      | PROD-001   |            1 |              50 |
      | PROD-002   |            1 |             100 |
      | PROD-003   |            1 |               0 |
      | PROD-004   |            1 |             150 |
    # 當客戶嘗試購買 "iPhone 完整套餐" - When customer tries to purchase "iPhone Complete Package"
    When customer tries to purchase "iPhone Complete Package"
    # 那麼系統應該顯示 "PROD-003 庫存不足，無法購買此組合" - Then system should display "PROD-003 out of stock, cannot purchase this bundle"
    Then system should display "PROD-003 out of stock, cannot purchase this bundle"
    # 並且建議替代方案或單獨購買其他商品 - And should suggest alternatives or individual purchase
    And should suggest alternatives or individual purchase
  # 場景: 動態組合定價 - Scenario: Dynamic bundle pricing

  Scenario: Dynamic bundle pricing
    # 假設存在 "自選手機套餐" 允許客戶選擇 - Given "Custom Phone Package" allows customer to select
    Given "Custom Phone Package" allows customer to select:
      # - 1 支手機 (必選) - 1 phone (required)
      # - 2-4 件配件 (可選) - 2-4 accessories (optional)
    # 並且配件享受 15% 折扣 - And accessories get 15% discount
    And accessories get 15% discount
    # 當客戶選擇 1 支 "PROD-001" 和 3 件配件 - When customer selects 1 "PROD-001" and 3 accessories
    When customer selects 1 "PROD-001" and 3 accessories
    # 那麼手機按原價計算 - Then phone should be charged at original price
    Then phone should be charged at original price
    # 並且配件總價應該享受 15% 折扣 - And accessories total should get 15% discount
    And accessories total should get 15% discount
    # 並且系統應該顯示組合優惠詳情 - And system should display bundle discount details
    And system should display bundle discount details
