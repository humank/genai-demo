# 購物車管理 - Shopping Cart Management
Feature: Shopping Cart Management
  As a consumer
  I want to manage my shopping cart
  So that I can adjust products and quantities before purchase

  Background:
    # 假設系統中存在以下商品 - Given the following products exist in the system
    Given the following products exist in the system:
      | Product ID | Product Name       | Price | Stock |
      | PROD-001   | iPhone 15 Pro Max  | 35900 |    50 |
      | PROD-002   | Samsung Galaxy S24 | 28900 |    40 |
      | PROD-003   | MacBook Pro M3     | 58000 |    20 |
      | PROD-004   | AirPods Pro        |  8990 |   100 |
    # 並且我是已登入的客戶 "張小明" - And I am logged in as customer "John"
    And I am logged in as customer "John"
  # 場景: 添加商品到購物車 - Scenario: Add product to cart

  Scenario: Add product to cart
    # 當我將 2 個 "PROD-001" 添加到購物車 - When I add 2 units of "PROD-001" to cart
    When I add 2 units of "PROD-001" to cart
    # 那麼我的購物車應該包含 1 種商品 - Then my cart should contain 1 product type
    Then my cart should contain 1 product type
    # 並且商品 "PROD-001" 的數量應該為 2 - And product "PROD-001" quantity should be 2
    And product "PROD-001" quantity should be 2
    # 並且購物車總金額應該為 71800 元 - And cart total amount should be 71800
    And cart total amount should be 71800
  # 場景: 更新購物車商品數量 - Scenario: Update cart item quantity

  Scenario: Update cart item quantity
    # 假設我的購物車中已有 2 個 "PROD-001" - Given my cart already contains 2 units of "PROD-001"
    Given my cart already contains 2 units of "PROD-001"
    # 當我將商品 "PROD-001" 的數量更新為 3 - When I update product "PROD-001" quantity to 3
    When I update product "PROD-001" quantity to 3
    # 那麼商品 "PROD-001" 的數量應該為 3 - Then product "PROD-001" quantity should be 3
    Then product "PROD-001" quantity should be 3
    # 並且購物車總金額應該為 107700 元 - And cart total amount should be 107700
    And cart total amount should be 107700
  # 場景: 從購物車移除商品 - Scenario: Remove product from cart

  Scenario: Remove product from cart
    # 假設我的購物車中有以下商品 - Given my cart contains the following products
    Given my cart contains the following products:
      | Product ID | Quantity |
      | PROD-001   |        2 |
      | PROD-004   |        1 |
    # 當我從購物車移除商品 "PROD-001" - When I remove product "PROD-001" from cart
    When I remove product "PROD-001" from cart
    # 那麼我的購物車應該包含 1 種商品 - Then my cart should contain 1 product type
    Then my cart should contain 1 product type
    # 並且只有商品 "PROD-004" 在購物車中 - And only product "PROD-004" should be in cart
    And only product "PROD-004" should be in cart
    # 並且購物車總金額應該為 590 元 - And cart total amount should be 590
    And cart total amount should be 590
  # 場景: 清空購物車 - Scenario: Clear cart

  Scenario: Clear cart
    # 假設我的購物車中有以下商品 - Given my cart contains the following products
    Given my cart contains the following products:
      | Product ID | Quantity |
      | PROD-001   |        1 |
      | PROD-002   |        2 |
      | PROD-004   |        1 |
    # 當我清空購物車 - When I clear my cart
    When I clear my cart
    # 那麼我的購物車應該為空 - Then my cart should be empty
    Then my cart should be empty
    # 並且購物車總金額應該為 0 元 - And cart total amount should be 0
    And cart total amount should be 0
  # 場景: 購物車商品庫存驗證 - Scenario: Cart item stock validation

  Scenario: Cart item stock validation
    # 假設商品 "PROD-003" 的庫存只有 5 個 - Given product "PROD-003" has only 5 units in stock
    Given product "PROD-003" has only 5 units in stock
    # 並且我的購物車中已有 3 個 "PROD-003" - And my cart already contains 3 units of "PROD-003"
    And my cart already contains 3 units of "PROD-003"
    # 當我嘗試將商品 "PROD-003" 的數量更新為 8 - When I try to update product "PROD-003" quantity to 8
    When I try to update product "PROD-003" quantity to 8
    # 那麼我應該收到庫存不足的錯誤訊息 - Then I should receive an insufficient stock error message
    Then I should receive an insufficient stock error message
    # 並且商品 "PROD-003" 的數量應該保持為 3 - And product "PROD-003" quantity should remain 3
    And product "PROD-003" quantity should remain 3
  # 場景: 購物車持久化 - Scenario: Cart persistence

  Scenario: Cart persistence
    # 假設我將 1 個 "PROD-001" 添加到購物車 - Given I add 1 unit of "PROD-001" to cart
    Given I add 1 unit of "PROD-001" to cart
    # 當我登出並重新登入 - When I logout and login again
    When I logout and login again
    # 那麼我的購物車應該仍然包含 1 個 "PROD-001" - Then my cart should still contain 1 unit of "PROD-001"
    Then my cart should still contain 1 unit of "PROD-001"
  # 場景: 購物車商品價格更新 - Scenario: Cart item price update

  Scenario: Cart item price update
    # 假設我的購物車中有 2 個 "PROD-001" - Given my cart contains 2 units of "PROD-001"
    Given my cart contains 2 units of "PROD-001"
    # 並且商品 "PROD-001" 的價格從 35900 元更新為 33900 元 - And product "PROD-001" price is updated from 35900 to 33900
    And product "PROD-001" price is updated from 35900 to 33900
    # 當我查看購物車 - When I view my cart
    When I view my cart
    # 那麼購物車應該顯示更新後的價格 - Then cart should display the updated price
    Then cart should display the updated price
    # 並且購物車總金額應該為 67800 元 - And cart total amount should be 67800
    And cart total amount should be 67800
  # 場景: 購物車商品下架處理 - Scenario: Handle discontinued product in cart

  Scenario: Handle discontinued product in cart
    # 假設我的購物車中有 1 個 "PROD-002" - Given my cart contains 1 unit of "PROD-002"
    Given my cart contains 1 unit of "PROD-002"
    # 當商品 "PROD-002" 被下架 - When product "PROD-002" is discontinued
    When product "PROD-002" is discontinued
    # 並且我查看購物車 - And I view my cart
    And I view my cart
    # 那麼我應該收到商品已下架的通知 - Then I should receive a product discontinued notification
    Then I should receive a product discontinued notification
    # 並且該商品應該從購物車中移除 - And the product should be removed from cart
    And the product should be removed from cart
  # 場景: 購物車數量限制 - Scenario: Cart quantity limit

  Scenario: Cart quantity limit
    # 當我嘗試將超過 99 個 "PROD-004" 添加到購物車 - When I try to add more than 99 units of "PROD-004" to cart
    When I try to add more than 99 units of "PROD-004" to cart
    # 那麼我應該收到數量超過限制的錯誤訊息 - Then I should receive a quantity limit exceeded error message
    Then I should receive a quantity limit exceeded error message
    # 並且購物車中該商品數量不應超過 99 個 - And cart should not contain more than 99 units of the product
    And cart should not contain more than 99 units of the product
  # 場景: 匿名用戶購物車轉移 - Scenario: Anonymous cart transfer

  Scenario: Anonymous cart transfer
    # 假設我是匿名用戶並將 1 個 "PROD-001" 添加到購物車 - Given I am an anonymous user and add 1 unit of "PROD-001" to cart
    Given I am an anonymous user and add 1 unit of "PROD-001" to cart
    # 當我註冊並登入帳戶 - When I register and login to an account
    When I register and login to an account
    # 那麼匿名購物車的商品應該合併到我的帳戶購物車中 - Then anonymous cart items should be merged into my account cart
    Then anonymous cart items should be merged into my account cart
    # 並且我的購物車應該包含 1 個 "PROD-001" - And my cart should contain 1 unit of "PROD-001"
    And my cart should contain 1 unit of "PROD-001"
