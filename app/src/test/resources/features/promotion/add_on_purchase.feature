# 加購優惠 - Add-on Purchase Offers
Feature: Add-on Purchase Offers
  As an e-commerce platform
  I want to provide add-on purchase offers
  So that I can increase customer purchase value and platform revenue

  Background:
    # 假設系統中存在以下商品 - Given the following products exist for promotion testing
    Given the following products exist for promotion testing:
      | Product ID | Product Name      | Category  | Price | Stock |
      | PROD-001   | iPhone 15 Pro Max | Phone     | 35900 |    50 |
      | PROD-002   | AirPods Pro       | Headset   |  8990 |   100 |
      | PROD-003   | iPhone Case       | Accessory |   990 |   200 |
      | PROD-004   | Wireless Charger  | Accessory |  1990 |   150 |
      | PROD-005   | Screen Protector  | Accessory |   590 |   300 |
      | PROD-006   | MacBook Pro M3    | Laptop    | 58000 |    20 |
      | PROD-007   | Magic Mouse       | Accessory |  2990 |    80 |
    # 並且設定以下加購優惠規則 - And the following add-on offer rules are configured
    And the following add-on offer rules are configured:
      | Main Product | Add-on Product | Add-on Price | Original Price | Limit Qty |
      | PROD-001     | PROD-002       |         7990 |           8990 |         1 |
      | PROD-001     | PROD-003       |          690 |            990 |         2 |
      | PROD-001     | PROD-004       |         1490 |           1990 |         1 |
      | PROD-001     | PROD-005       |          390 |            590 |         3 |
      | PROD-006     | PROD-007       |         2490 |           2990 |         1 |
  # 場景: 顯示加購優惠選項 - Scenario: Display add-on offer options

  Scenario: Display add-on offer options
    # 假設客戶將 "PROD-001" (iPhone) 加入購物車 - Given customer adds "PROD-001" (iPhone) to cart
    Given customer adds "PROD-001" to cart
    # 當客戶查看購物車或商品頁面 - When customer views cart or product page
    When customer views cart or product page
    # 那麼應該顯示以下加購優惠選項 - Then should display the following add-on options
    Then should display the following add-on options:
      | Add-on Product   | Original Price | Add-on Price | Savings | Limit Qty |
      | AirPods Pro      |           8990 |         7990 |    1000 |         1 |
      | iPhone Case      |            990 |          690 |     300 |         2 |
      | Wireless Charger |           1990 |         1490 |     500 |         1 |
      | Screen Protector |            590 |          390 |     200 |         3 |
  # 場景: 成功加購商品 - Scenario: Successfully add add-on product

  Scenario: Successfully add add-on product
    # 假設客戶購物車中有 "PROD-001" (iPhone) - Given customer cart contains "PROD-001" (iPhone)
    Given customer cart contains "PROD-001"
    # 當客戶選擇加購 "PROD-002" (AirPods Pro) - When customer selects add-on "PROD-002" (AirPods Pro)
    When customer selects add-on "PROD-002"
    # 那麼 "PROD-002" 應該以加購價 7990 元加入購物車 - Then "PROD-002" should be added to cart at add-on price 7990
    Then "PROD-002" should be added to cart at add-on price 7990
    # 並且應該標記為 "加購商品" - And should be marked as "Add-on Product"
    And should be marked as "Add-on Product"
    # 並且購物車總金額應該為 43890 元 (35900 + 7990) - And cart total should be 43890 (35900 + 7990)
    And cart total should be 43890
  # 場景: 加購數量限制 - Scenario: Add-on quantity limit

  Scenario: Add-on quantity limit
    # 假設客戶購物車中有 "PROD-001" (iPhone) - Given customer cart contains "PROD-001" (iPhone)
    Given customer cart contains "PROD-001"
    # 並且已加購 1 個 "PROD-002" (AirPods Pro) - And has already added 1 "PROD-002" (AirPods Pro) as add-on
    And has already added 1 "PROD-002" as add-on
    # 當客戶嘗試再加購 1 個 "PROD-002" - When customer tries to add another "PROD-002" as add-on
    When customer tries to add another "PROD-002" as add-on
    # 那麼系統應該顯示 "此商品加購數量已達上限" - Then system should display "Add-on quantity limit reached for this product"
    Then system should display "Add-on quantity limit reached for this product"
    # 並且不允許再次加購 - And should not allow additional add-on
    And should not allow additional add-on
  # 場景: 多數量加購 - Scenario: Multiple quantity add-on

  Scenario: Multiple quantity add-on
    # 假設客戶購物車中有 "PROD-001" (iPhone) - Given customer cart contains "PROD-001" (iPhone)
    Given customer cart contains "PROD-001"
    # 當客戶選擇加購 2 個 "PROD-003" (保護殼) - When customer selects 2 "PROD-003" (Case) as add-on
    When customer selects 2 "PROD-003" as add-on
    # 那麼 2 個 "PROD-003" 都應該以加購價 690 元計算 - Then both "PROD-003" should be priced at add-on price 690 each
    Then both "PROD-003" should be priced at add-on price 690 each
    # 並且購物車應該顯示 "加購商品 x2" - And cart should display "Add-on Product x2"
    And cart should display "Add-on Product x2"
    # 並且總加購金額應該為 1380 元 - And total add-on amount should be 1380
    And total add-on amount should be 1380
  # 場景: 移除主商品影響加購商品 - Scenario: Removing main product affects add-on products

  Scenario: Removing main product affects add-on products
    # 假設客戶購物車中有 - Given customer cart contains
    Given customer cart contains:
      | Product ID | Type   | Price |
      | PROD-001   | Main   | 35900 |
      | PROD-002   | Add-on |  7990 |
      | PROD-003   | Add-on |   690 |
    # 當客戶移除主商品 "PROD-001" - When customer removes main product "PROD-001"
    When customer removes main product "PROD-001"
    # 那麼加購商品應該恢復原價或被移除 - Then add-on products should revert to original price or be removed
    Then add-on products should revert to original price or be removed
    # 並且系統應該提示 "移除主商品將影響加購優惠" - And system should prompt "Removing main product will affect add-on offers"
    And system should prompt "Removing main product will affect add-on offers"
    # 並且詢問客戶是否繼續 - And ask customer whether to continue
    And ask customer whether to continue
  # 場景: 加購商品價格恢復 - Scenario: Add-on product price restoration

  Scenario: Add-on product price restoration
    # 假設客戶移除了主商品但選擇保留加購商品 - Given customer removed main product but chose to keep add-on products
    Given customer removed main product but chose to keep add-on products
    # 當加購商品 "PROD-002" 失去加購資格 - When add-on product "PROD-002" loses add-on eligibility
    When add-on product "PROD-002" loses add-on eligibility
    # 那麼價格應該恢復為原價 8990 元 - Then price should revert to original price 8990
    Then price should revert to original price 8990
    # 並且移除 "加購商品" 標籤 - And remove "Add-on Product" label
    And remove "Add-on Product" label
    # 並且重新計算購物車總金額 - And recalculate cart total amount
    And recalculate cart total amount
  # 場景: 多主商品加購優惠 - Scenario: Multiple main products add-on offers

  Scenario: Multiple main products add-on offers
    # 假設客戶購物車中有 2 個 "PROD-001" (iPhone) - Given customer cart contains 2 "PROD-001" (iPhone)
    Given customer cart contains 2 "PROD-001"
    # 當客戶查看加購選項 - When customer views add-on options
    When customer views add-on options
    # 那麼每個主商品都應該有獨立的加購額度 - Then each main product should have independent add-on quota
    Then each main product should have independent add-on quota
    # 並且 "PROD-002" 的加購數量上限應該為 2 (1×2) - And "PROD-002" add-on quantity limit should be 2 (1×2)
    And "PROD-002" add-on quantity limit should be 2
    # 並且 "PROD-003" 的加購數量上限應該為 4 (2×2) - And "PROD-003" add-on quantity limit should be 4 (2×2)
    And "PROD-003" add-on quantity limit should be 4
  # 場景: 加購優惠與其他優惠疊加 - Scenario: Add-on offers stacking with other discounts

  Scenario: Add-on offers stacking with other discounts
    # 假設客戶是 VIP 會員享受 5% 折扣 - Given customer is VIP member with 5% discount
    Given customer is VIP member with 5% discount
    # 並且購物車中有主商品和加購商品 - And cart contains main product and add-on products
    And cart contains main product and add-on products
    # 當計算最終價格時 - When calculating final price
    When calculating final price
    # 那麼會員折扣應該應用於加購價格 - Then member discount should apply to add-on prices
    Then member discount should apply to add-on prices
    # 並且 "PROD-002" 的最終價格應該為 7590.5 元 (7990 × 0.95) - And "PROD-002" final price should be 7590.5 (7990 × 0.95)
    And "PROD-002" final price should be 7590.5
  # 場景: 加購商品庫存不足 - Scenario: Add-on product out of stock

  Scenario: Add-on product out of stock
    # 假設 "PROD-002" (AirPods Pro) 的庫存只剩 1 個 - Given "PROD-002" (AirPods Pro) has only 1 unit in stock
    Given "PROD-002" has only 1 unit in stock
    # 並且已有其他客戶將其加入購物車 - And another customer has added it to cart
    And another customer has added it to cart
    # 當客戶嘗試加購 "PROD-002" - When customer tries to add "PROD-002" as add-on
    When customer tries to add "PROD-002" as add-on
    # 那麼系統應該顯示 "加購商品庫存不足" - Then system should display "Add-on product out of stock"
    Then system should display "Add-on product out of stock"
    # 並且建議其他可用的加購選項 - And suggest other available add-on options
    And suggest other available add-on options
