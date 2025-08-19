# 商品搜尋 - Product Search
Feature: Product Search
  As a consumer
  I want to search for products
  So that I can quickly find the products I need

  Background:
    # 假設系統中存在以下商品 - Given the following products exist in the system
    Given the following products exist in the system:
      | Product ID | Product Name       | Category | Brand     | Price | Stock | Tags                       |
      | PROD-001   | iPhone 15 Pro Max  | Phone    | Apple     | 35900 |    50 | smartphone,5G,premium      |
      | PROD-002   | Samsung Galaxy S24 | Phone    | Samsung   | 28900 |    40 | smartphone,5G,Android      |
      | PROD-003   | MacBook Pro M3     | Laptop   | Apple     | 58000 |    20 | laptop,M3chip,professional |
      | PROD-004   | AirPods Pro        | Headset  | Apple     |  8990 |   100 | wireless,noise-canceling   |
      | PROD-005   | iPad Air M2        | Tablet   | Apple     | 19900 |    30 | tablet,M2chip,lightweight  |
      | PROD-006   | Surface Pro 9      | Tablet   | Microsoft | 32900 |    15 | tablet,Windows,stylus      |
  # 場景: 基本關鍵字搜尋 - Scenario: Basic keyword search

  Scenario: Basic keyword search
    # 當我搜尋 "iPhone" - When I search for "iPhone"
    When I search for "iPhone"
    # 那麼搜尋結果應該包含 1 個商品 - Then search results should contain 1 product
    Then search results should contain 1 product
    # 並且搜尋結果應該包含商品 "PROD-001" - And search results should include product "PROD-001"
    And search results should include product "PROD-001"
  # 場景: 多關鍵字搜尋 - Scenario: Multiple keywords search

  Scenario: Multiple keywords search
    # 當我搜尋 "Apple laptop" - When I search for "Apple laptop"
    When I search for "Apple laptop"
    # 那麼搜尋結果應該包含 1 個商品 - Then search results should contain 1 product
    Then search results should contain 1 product
    # 並且搜尋結果應該包含商品 "PROD-003" - And search results should include product "PROD-003"
    And search results should include product "PROD-003"
  # 場景: 分類搜尋 - Scenario: Category search

  Scenario: Category search
    # 當我在分類 "Phone" 中搜尋 - When I search within category "Phone"
    When I search within category "Phone"
    # 那麼搜尋結果應該包含 2 個商品 - Then search results should contain 2 products
    Then search results should contain 2 products
    # 並且搜尋結果應該包含商品 "PROD-001" 和 "PROD-002" - And search results should include products "PROD-001" and "PROD-002"
    And search results should include products "PROD-001" and "PROD-002"
  # 場景: 品牌篩選搜尋 - Scenario: Brand filter search

  Scenario: Brand filter search
    # 當我搜尋品牌為 "Apple" 的商品 - When I search for products with brand "Apple"
    When I search for products with brand "Apple"
    # 那麼搜尋結果應該包含 4 個商品 - Then search results should contain 4 products
    Then search results should contain 4 products
    # 並且所有結果的品牌都應該是 "Apple" - And all results should have brand "Apple"
    And all results should have brand "Apple"
  # 場景: 價格範圍搜尋 - Scenario: Price range search

  Scenario: Price range search
    # 當我搜尋價格在 20000 到 40000 元之間的商品 - When I search for products with price between 20000 and 40000
    When I search for products with price between 20000 and 40000
    # 那麼搜尋結果應該包含 3 個商品 - Then search results should contain 3 products
    Then search results should contain 3 products
    # 並且搜尋結果應該包含商品 "PROD-002", "PROD-005", "PROD-006" - And search results should include products "PROD-002", "PROD-005", "PROD-006"
    And search results should include products "PROD-002", "PROD-005", "PROD-006"
  # 場景: 組合條件搜尋 - Scenario: Combined criteria search

  Scenario: Combined criteria search
    # 當我搜尋分類為 "Tablet" 且品牌為 "Apple" 的商品 - When I search for products with category "Tablet" and brand "Apple"
    When I search for products with category "Tablet" and brand "Apple"
    # 那麼搜尋結果應該包含 1 個商品 - Then search results should contain 1 product
    Then search results should contain 1 product
    # 並且搜尋結果應該包含商品 "PROD-005" - And search results should include product "PROD-005"
    And search results should include product "PROD-005"
  # 場景: 標籤搜尋 - Scenario: Tag search

  Scenario: Tag search
    # 當我搜尋標籤包含 "wireless" 的商品 - When I search for products with tag "wireless"
    When I search for products with tag "wireless"
    # 那麼搜尋結果應該包含 1 個商品 - Then search results should contain 1 product
    Then search results should contain 1 product
    # 並且搜尋結果應該包含商品 "PROD-004" - And search results should include product "PROD-004"
    And search results should include product "PROD-004"
  # 場景: 模糊搜尋 - Scenario: Fuzzy search

  Scenario: Fuzzy search
    # 當我搜尋 "iPhon" (拼寫錯誤) - When I search for "iPhon" (typo)
    When I search for "iPhon"
    # 那麼搜尋結果應該包含 1 個商品 - Then search results should contain 1 product
    Then search results should contain 1 product
    # 並且搜尋結果應該包含商品 "PROD-001" - And search results should include product "PROD-001"
    And search results should include product "PROD-001"
    # 並且系統應該顯示 "您是否要搜尋: iPhone" - And system should display "Did you mean: iPhone"
    And system should display "Did you mean: iPhone"
  # 場景: 搜尋結果排序 - 價格由低到高 - Scenario: Search results sorting - Price low to high

  Scenario: Search results sorting - Price low to high
    # 當我搜尋 "Apple" 並按價格由低到高排序 - When I search for "Apple" and sort by price low to high
    When I search for "Apple" and sort by price low to high
    # 那麼搜尋結果應該按以下順序顯示 - Then search results should be displayed in the following order
    Then search results should be displayed in the following order:
      | Product ID | Product Name      | Price |
      | PROD-004   | AirPods Pro       |  8990 |
      | PROD-005   | iPad Air M2       | 19900 |
      | PROD-001   | iPhone 15 Pro Max | 35900 |
      | PROD-003   | MacBook Pro M3    | 58000 |
  # 場景: 搜尋結果排序 - 相關性 - Scenario: Search results sorting - Relevance

  Scenario: Search results sorting - Relevance
    # 當我搜尋 "Pro" 並按相關性排序 - When I search for "Pro" and sort by relevance
    When I search for "Pro" and sort by relevance
    # 那麼搜尋結果應該優先顯示名稱中包含 "Pro" 的商品 - Then search results should prioritize products with "Pro" in name
    Then search results should prioritize products with "Pro" in name
    # 並且 "PROD-001", "PROD-003", "PROD-004", "PROD-006" 應該在結果中 - And "PROD-001", "PROD-003", "PROD-004", "PROD-006" should be in results
    And "PROD-001", "PROD-003", "PROD-004", "PROD-006" should be in results
  # 場景: 空搜尋結果 - Scenario: Empty search results

  Scenario: Empty search results
    # 當我搜尋 "nonexistent product" - When I search for "nonexistent product"
    When I search for "nonexistent product"
    # 那麼搜尋結果應該為空 - Then search results should be empty
    Then search results should be empty
    # 並且系統應該顯示 "沒有找到相關商品" - And system should display "No products found"
    And system should display "No products found"
    # 並且系統應該建議 "請嘗試其他關鍵字或瀏覽分類" - And system should suggest "Try different keywords or browse categories"
    And system should suggest "Try different keywords or browse categories"
  # 場景: 搜尋歷史記錄 - Scenario: Search history

  Scenario: Search history
    # 假設我之前搜尋過 "iPhone", "MacBook", "AirPods" - Given I previously searched for "iPhone", "MacBook", "AirPods"
    Given I previously searched for "iPhone", "MacBook", "AirPods"
    # 當我點擊搜尋框 - When I click on search box
    When I click on search box
    # 那麼系統應該顯示我的搜尋歷史 - Then system should display my search history
    Then system should display my search history
    # 並且歷史記錄應該包含 "iPhone", "MacBook", "AirPods" - And history should include "iPhone", "MacBook", "AirPods"
    And history should include "iPhone", "MacBook", "AirPods"
  # 場景: 熱門搜尋建議 - Scenario: Popular search suggestions

  Scenario: Popular search suggestions
    # 當我點擊搜尋框但未輸入任何內容 - When I click on search box without entering any content
    When I click on search box without entering any content
    # 那麼系統應該顯示熱門搜尋關鍵字 - Then system should display popular search keywords
    Then system should display popular search keywords
    # 並且熱門搜尋應該包含 "iPhone", "MacBook", "iPad" 等 - And popular searches should include "iPhone", "MacBook", "iPad"
    And popular searches should include "iPhone", "MacBook", "iPad"
  # 場景: 自動完成建議 - Scenario: Auto-complete suggestions

  Scenario: Auto-complete suggestions
    # 當我輸入 "iP" - When I type "iP"
    When I type "iP"
    # 那麼系統應該顯示自動完成建議 - Then system should display auto-complete suggestions
    Then system should display auto-complete suggestions
    # 並且建議應該包含 "iPhone", "iPad" - And suggestions should include "iPhone", "iPad"
    And suggestions should include "iPhone", "iPad"
  # 場景: 搜尋結果分頁 - Scenario: Search results pagination

  Scenario: Search results pagination
    # 假設搜尋 "Apple" 有 20 個結果 - Given searching for "Apple" returns 20 results
    Given searching for "Apple" returns 20 results
    # 當我查看第 1 頁，每頁顯示 10 個商品 - When I view page 1 with 10 products per page
    When I view page 1 with 10 products per page
    # 那麼應該顯示前 10 個商品 - Then should display first 10 products
    Then should display first 10 products
    # 並且應該有分頁導航 - And should have pagination navigation
    And should have pagination navigation
    # 當我點擊第 2 頁 - When I click page 2
    When I click page 2
    # 那麼應該顯示剩餘的 10 個商品 - Then should display remaining 10 products
    Then should display remaining 10 products
  # 場景: 庫存狀態篩選 - Scenario: Stock status filter

  Scenario: Stock status filter
    # 當我搜尋 "Apple" 並篩選 "有庫存" 的商品 - When I search for "Apple" and filter for "in stock" products
    When I search for "Apple" and filter for "in stock" products
    # 那麼搜尋結果應該只包含庫存大於 0 的商品 - Then search results should only include products with stock > 0
    Then search results should only include products with stock > 0
    # 並且每個商品都應該顯示庫存狀態 - And each product should display stock status
    And each product should display stock status
