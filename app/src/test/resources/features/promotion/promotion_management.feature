# 促銷活動管理 - Promotion Management
Feature: Promotion Management
  As an e-commerce platform
  I want to create and manage various promotional activities
  So that I can boost sales performance and customer engagement

  Background:
    # 假設系統中存在以下商品 - Given the following products exist for promotion testing
    Given the following products exist for promotion testing:
      | Product ID | Product Name       | Category | Price | Stock |
      | PROD-001   | iPhone 15 Pro Max  | Phone    | 35900 |    50 |
      | PROD-002   | Samsung Galaxy S24 | Phone    | 28900 |    40 |
      | PROD-003   | AirPods Pro        | Headset  |  8990 |   100 |
      | PROD-004   | MacBook Pro M3     | Laptop   | 58000 |    20 |
    # 並且當前時間為 2024-01-15 10:00 - And current time is 2024-01-15 10:00
    And current time is 2024-01-15 10:00
  # 場景: 創建限時特價活動 - Scenario: Create flash sale promotion

  Scenario: Create flash sale promotion
    # 當管理員創建限時特價活動 - When admin creates flash sale promotion
    When admin creates flash sale promotion:
      | Name         | Product ID | Sale Price | Start Time       | End Time         |
      | Weekend Sale | PROD-001   |      32900 | 2024-01-20 00:00 | 2024-01-21 23:59 |
    # 那麼活動應該被成功創建 - Then promotion should be created successfully
    Then promotion should be created successfully
    # 並且活動狀態應該為 "待開始" - And promotion status should be "Pending"
    And promotion status should be "Pending"
    # 並且系統應該在開始時間自動啟用活動 - And system should automatically activate promotion at start time
    And system should automatically activate promotion at start time
  # 場景: 限時特價活動生效 - Scenario: Flash sale promotion becomes active

  Scenario: Flash sale promotion becomes active
    # 假設存在以下限時特價活動 - Given the following flash sale promotion exists
    Given the following flash sale promotion exists:
      | Name     | Product ID | Sale Price | Start Time       | End Time         |
      | New Year | PROD-001   |      29900 | 2024-01-15 00:00 | 2024-01-15 23:59 |
    # 當客戶在活動期間查看商品 "PROD-001" - When customer views product "PROD-001" during promotion period
    When customer views product "PROD-001" during promotion period
    # 那麼商品價格應該顯示為 29900 元 - Then product price should display as 29900
    Then product price should display as 29900
    # 並且應該顯示原價 35900 元（劃線） - And should display original price 35900 (strikethrough)
    And should display original price 35900 with strikethrough
    # 並且應該顯示節省金額 6000 元 - And should display savings amount 6000
    And should display savings amount 6000
    # 並且應該顯示活動倒數計時 - And should display promotion countdown timer
    And should display promotion countdown timer
  # 場景: 限量特價活動 - Scenario: Limited quantity promotion

  Scenario: Limited quantity promotion
    # 假設創建限量特價活動 - Given limited quantity promotion is created
    Given limited quantity promotion is created:
      | Name       | Product ID | Sale Price | Quantity Limit | Sold Count |
      | Flash Deal | PROD-003   |       6990 |             20 |         15 |
    # 當客戶查看商品 "PROD-003" - When customer views product "PROD-003"
    When customer views product "PROD-003"
    # 那麼應該顯示特價 6990 元 - Then should display sale price 6990
    Then should display sale price 6990
    # 並且應該顯示 "限量20個，剩餘5個" - And should display "Limited 20 units, 5 remaining"
    And should display "Limited 20 units, 5 remaining"
    # 並且應該顯示搶購進度條 - And should display purchase progress bar
    And should display purchase progress bar
  # 場景: 限量特價售完 - Scenario: Limited quantity promotion sold out

  Scenario: Limited quantity promotion sold out
    # 假設限量特價活動已售完 - Given limited quantity promotion is sold out
    Given limited quantity promotion is sold out:
      | Name       | Product ID | Sale Price | Quantity Limit | Sold Count |
      | Flash Deal | PROD-003   |       6990 |             20 |         20 |
    # 當客戶查看商品 "PROD-003" - When customer views product "PROD-003"
    When customer views product "PROD-003"
    # 那麼應該顯示原價 8990 元 - Then should display original price 8990
    Then should display original price 8990
    # 並且應該顯示 "限量特價已售完" - And should display "Limited sale sold out"
    And should display "Limited sale sold out"
    # 並且不應該顯示特價資訊 - And should not display sale price information
    And should not display sale price information
  # 場景: 滿額折扣活動 - Scenario: Spend threshold discount promotion

  Scenario: Spend threshold discount promotion
    # 假設創建滿額折扣活動 - Given spend threshold discount promotion is created
    Given spend threshold discount promotion is created:
      | Name         | Min Amount | Discount Type | Discount Value | Category |
      | Spend & Save |      10000 | Percentage    |            10% | All      |
    # 當客戶購物車總金額為 12000 元 - When customer cart total is 12000
    When customer cart total is 12000
    # 那麼應該享受 10% 折扣 - Then should receive 10% discount
    Then should receive 10% discount
    # 並且折扣金額應該為 1200 元 - And discount amount should be 1200
    And discount amount should be 1200
    # 並且最終金額應該為 10800 元 - And final amount should be 10800
    And final amount should be 10800
  # 場景: 階梯式滿額優惠 - Scenario: Tiered spend discount promotion

  Scenario: Tiered spend discount promotion
    # 假設創建階梯式滿額優惠 - Given tiered spend discount promotion is created
    Given tiered spend discount promotion is created:
      | Min Amount | Discount Amount |
      |       5000 |             200 |
      |      10000 |             500 |
      |      20000 |            1200 |
    # 當客戶購物車總金額為 15000 元 - When customer cart total is 15000
    When customer cart total is 15000
    # 那麼應該享受 500 元折扣（滿10000的優惠） - Then should receive 500 discount (for 10000 tier)
    Then should receive 500 discount for 10000 tier
    # 並且應該提示 "再購買5000元可享受1200元優惠" - And should prompt "Spend 5000 more to get 1200 discount"
    And should prompt "Spend 5000 more to get 1200 discount"
  # 場景: 買N送N活動 - Scenario: Buy N Get N promotion

  Scenario: Buy N Get N promotion
    # 假設創建買2送1活動適用於 "PROD-003" - Given Buy 2 Get 1 promotion is created for "PROD-003"
    Given Buy 2 Get 1 promotion is created for "PROD-003"
    # 當客戶購買 3 個 "PROD-003" - When customer purchases 3 units of "PROD-003"
    When customer purchases 3 units of "PROD-003"
    # 那麼應該只收取 2 個的費用 - Then should only charge for 2 units
    Then should only charge for 2 units
    # 並且第 3 個應該標記為贈品 - And 3rd unit should be marked as free gift
    And 3rd unit should be marked as free gift
    # 並且總價格應該為 17980 元（2 × 8990） - And total price should be 17980 (2 × 8990)
    And total price should be 17980
  # 場景: 第二件半價活動 - Scenario: Second item half price promotion

  Scenario: Second item half price promotion
    # 假設創建第二件半價活動適用於手機分類 - Given second item half price promotion is created for phone category
    Given second item half price promotion is created for phone category
    # 當客戶購買 2 支手機 - When customer purchases 2 phones
    When customer purchases 2 phones:
      | Product ID | Price |
      | PROD-001   | 35900 |
      | PROD-002   | 28900 |
    # 那麼價格較低的商品應該享受半價 - Then lower priced item should get half price
    Then lower priced item should get half price
    # 並且 "PROD-002" 的價格應該為 14450 元 - And "PROD-002" price should be 14450
    And "PROD-002" price should be 14450
    # 並且總價格應該為 50350 元 - And total price should be 50350
    And total price should be 50350
