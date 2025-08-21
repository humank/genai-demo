Feature: Gift with Purchase
  As an e-commerce platform
  I want to provide gift with purchase promotions
  So that I can encourage customers to increase their purchase amount
  # 功能: 滿額贈品
  # 作為一個電商平台
  # 我希望提供滿額贈品活動
  # 以便鼓勵客戶增加購買金額

  Background:
    # 背景:
    Given the following products exist in the system:
      | Product ID | Product Name      | Category  | Price | Stock |
      | PROD-001   | iPhone 15 Pro Max | Phone     | 35900 |    50 |
      | PROD-002   | AirPods Pro       | Headset   |  8990 |   100 |
      | PROD-003   | Brand T-shirt     | Clothing  |   890 |   500 |
      | PROD-004   | Thermal Cup       | Lifestyle |   590 |   200 |
      | PROD-005   | Wireless Charger  | Accessory |  1990 |   150 |
    # 假設系統中存在以下商品:
    And the following gift with purchase activities are configured:
      | Activity Name     | Min Spend | Gift Product ID | Gift Name        | Gift Quantity | Gift Stock |
      | Spend 1K Get Cup  |      1000 | PROD-004        | Thermal Cup      |             1 |        200 |
      | Spend 5K Get Tee  |      5000 | PROD-003        | Brand T-shirt    |             1 |        500 |
      | Spend 10K Charger |     10000 | PROD-005        | Wireless Charger |             1 |        150 |
    # 並且設定以下滿額贈品活動:

  Scenario: Meeting gift with purchase conditions
    # 場景: 達到滿額贈品條件
    Given customer cart total is 6000
    # 假設客戶購物車總金額為 6000 元
    When system checks gift with purchase conditions
    # 當系統檢查滿額贈品條件
    Then customer should qualify for the following gift activities:
      | Activity Name     | Gift             | Status        |
      | Spend 1K Get Cup  | Thermal Cup      | Qualified     |
      | Spend 5K Get Tee  | Brand T-shirt    | Qualified     |
      | Spend 10K Charger | Wireless Charger | Not Qualified |
    # 那麼客戶應該符合以下贈品活動:

  Scenario: Automatically add qualifying gifts
    # 場景: 自動添加滿額贈品
    Given customer cart total is 5500
    # 假設客戶購物車總金額為 5500 元
    When customer enters checkout page
    # 當客戶進入結帳頁面
    Then system should automatically add qualifying gifts:
      | Gift Product  | Quantity | Price | Label |
      | Thermal Cup   |        1 |     0 | Gift  |
      | Brand T-shirt |        1 |     0 | Gift  |
    # 那麼系統應該自動添加符合條件的贈品:
    And cart total should remain 5500
    # 並且購物車總金額保持 5500 元

  Scenario: Manual gift selection
    # 場景: 手動選擇滿額贈品
    Given gift with purchase activity offers multiple choices:
      | Min Spend | Available Gifts                   |
      |      3000 | Thermal Cup, Brand T-shirt, Stand |
    # 假設滿額贈品活動提供多選一選項:
    And customer cart total is 3500
    # 並且客戶購物車總金額為 3500 元
    When customer views gift options
    # 當客戶查看贈品選項
    Then should display "Please select your gift"
    # 那麼應該顯示 "請選擇您的贈品"
    And customer can select one of the gifts
    # 並且客戶可以選擇其中一項贈品

  Scenario: Gift out of stock
    # 場景: 贈品庫存不足
    Given "Thermal Cup" gift stock is 0
    # 假設"保溫杯" 贈品庫存為 0
    And customer cart total is 1500
    # 並且客戶購物車總金額為 1500 元
    When system checks gift with purchase
    # 當系統檢查滿額贈品
    Then should display "Sorry, gift is out of stock"
    # 那麼應該顯示 "很抱歉，贈品已送完"
    And provide alternative gift or compensation
    # 並且提供替代贈品或其他補償

  Scenario: Cart amount change affects gifts
    # 場景: 購物金額變動影響贈品
    Given customer cart contains gift "Brand T-shirt"
    # 假設客戶購物車中有贈品 "品牌T恤"
    And cart total is 5200
    # 並且購物車總金額為 5200 元
    When customer removes items reducing total to 4500
    # 當客戶移除商品使總金額降至 4500 元
    Then system should prompt "Insufficient amount, removing gift"
    # 那麼系統應該提示 "購物金額不足，將移除贈品"
    And automatically remove unqualified gifts
    # 並且自動移除不符合條件的贈品

  Scenario: Tiered gift with purchase
    # 場景: 階梯式滿額贈品
    Given customer cart total is 12000
    # 假設客戶購物車總金額為 12000 元
    When system calculates gift with purchase
    # 當系統計算滿額贈品
    Then customer should receive all qualifying gifts:
      | Gift             | Condition | Status   |
      | Thermal Cup      | Spend 1K  | Received |
      | Brand T-shirt    | Spend 5K  | Received |
      | Wireless Charger | Spend 10K | Received |
    # 那麼客戶應該獲得所有符合條件的贈品:

  Scenario: Gift size and color selection
    # 場景: 贈品尺寸或顏色選擇
    Given gift "Brand T-shirt" has multiple sizes and colors:
      | Size | Color | Stock |
      | S    | Black |    50 |
      | M    | White |    80 |
      | L    | Blue  |    60 |
    # 假設贈品 "品牌T恤" 有多種尺寸和顏色:
    When customer qualifies for gift
    # 當客戶符合贈品條件
    Then should display size and color selection options
    # 那麼應該顯示尺寸和顏色選擇選項
    And customer must select before proceeding to checkout
    # 並且客戶必須選擇後才能繼續結帳

  Scenario: VIP exclusive gift with purchase
    # 場景: 會員專屬滿額贈品
    Given VIP members have exclusive gift with purchase activities:
      | Member Level | Min Spend | Gift         |
      | VIP          |      3000 | Premium Cup  |
      | GOLD         |      5000 | Limited Tote |
    # 假設VIP 會員有專屬滿額贈品活動:
    And customer "John" is VIP member
    # 並且客戶 "張小明" 是 VIP 會員
    When customer cart total is 3500
    # 當客戶購物車總金額為 3500 元
    Then should receive VIP exclusive gift "Premium Cup"
    # 那麼應該獲得 VIP 專屬贈品 "高級保溫杯"

  Scenario: Gift with purchase and coupon combination
    # 場景: 滿額贈品與優惠券疊加
    Given customer used 500 discount coupon
    # 假設客戶使用了 500 元優惠券
    And cart original price is 5500, discounted to 5000
    # 並且購物車原價為 5500 元，優惠後為 5000 元
    When system checks gift with purchase conditions
    # 當系統檢查滿額贈品條件
    Then should base on discounted amount 5000 for judgment
    # 那麼應該基於優惠後金額 5000 元判斷
    And customer should receive "Spend 5K Get Tee" gift
    # 並且客戶應該獲得 "滿五千送T恤" 的贈品

  Scenario: Gift return handling
    # 場景: 贈品退貨處理
    Given customer purchased items and received gift with purchase
    # 假設客戶購買了商品並獲得滿額贈品
    When customer requests partial return
    # 當客戶申請退貨部分商品
    And return amount no longer meets gift conditions
    # 並且退貨後金額不滿足贈品條件
    Then customer must return gift as well
    # 那麼客戶必須同時退回贈品

  Scenario: Gift activity expiration
    # 場景: 贈品活動時效
    Given gift with purchase activity expires on 2024-01-31
    # 假設滿額贈品活動有效期至 2024-01-31
    And current time is 2024-02-01
    # 並且當前時間為 2024-02-01
    When customer cart total meets conditions
    # 當客戶購物車總金額達到條件
    Then should not display expired gift activities
    # 那麼不應該顯示已過期的贈品活動
    And display "Activity ended" message
    # 並且顯示 "活動已結束" 提示

  Scenario: Gift preview and reminder
    # 場景: 贈品預告和提醒
    Given customer cart total is 4500
    # 假設客戶購物車總金額為 4500 元
    And needs 500 more for next gift condition
    # 並且距離下一個贈品條件還差 500 元
    When customer views cart
    # 當客戶查看購物車
    Then should display "Spend 500 more to get Brand T-shirt"
    # 那麼應該顯示 "再購買500元即可獲得品牌T恤"
    And recommend products with similar price
    # 並且推薦價格接近的商品

  Scenario: Limited time gift with purchase
    # 場景: 限時滿額贈品
    Given limited time gift with purchase activities:
      | Activity Time     | Min Spend | Gift        |
      | Daily 10:00-12:00 |      2000 | Limited Cup |
      | Weekend All Day   |      3000 | Weekend Bag |
    # 假設有限時滿額贈品活動:
    And current time is Saturday 11:00
    # 並且當前時間為週六 11:00
    When customer cart total is 2500
    # 當客戶購物車總金額為 2500 元
    Then should qualify for both limited time activities
    # 那麼應該同時符合兩個限時活動
    And can receive both gifts
    # 並且可以獲得兩個贈品

  Scenario: Gift quantity limit per person
    # 場景: 贈品數量限制
    Given each person limited to one gift with purchase
    # 假設每人限領一次滿額贈品
    And customer "John" previously received "Brand T-shirt"
    # 並且客戶 "張小明" 之前已領取過 "品牌T恤"
    When customer meets gift conditions again
    # 當客戶再次達到滿額條件
    Then should not receive same gift again
    # 那麼不應該再次獲得相同贈品
    And display "You have already received this gift"
    # 並且顯示 "您已領取過此贈品"

  Scenario: Gift activity statistics
    # 場景: 贈品活動統計
    Given past 30 days gift with purchase data:
      | Activity Name     | Participants | Gifts Distributed | Avg Amount Increase |
      | Spend 1K Get Cup  |         2500 |              2500 |                 150 |
      | Spend 5K Get Tee  |          800 |               800 |                 800 |
      | Spend 10K Charger |          200 |               200 |                1200 |
    # 假設過去 30 天的滿額贈品數據:
    When querying activity effectiveness report
    # 當查詢活動效果報告
    Then should display activity impact on sales increase
    # 那麼應該顯示活動對銷售額的提升效果
    And analyze customer behavior changes
    # 並且分析客戶行為變化

  Scenario: Personalized gift recommendations
    # 場景: 個人化贈品推薦
    Given customer "John" purchase history shows preference for tech products
    # 假設客戶 "張小明" 的購買歷史顯示偏好科技產品
    And reaches gift with purchase conditions
    # 並且達到滿額贈品條件
    When system provides gift options
    # 當系統提供贈品選項時
    Then should prioritize tech category gifts
    # 那麼應該優先推薦科技類贈品
    And display "Recommended based on your preferences"
    # 並且顯示 "基於您的購買偏好推薦"

  Scenario: Gift packaging and shipping
    # 場景: 贈品包裝和配送
    Given customer receives gift with purchase
    # 假設客戶獲得滿額贈品
    When order is ready for shipment
    # 當訂單準備出貨時
    Then gift should have special packaging
    # 那麼贈品應該有專門的包裝
    And packaging should have label "Gift with purchase, thank you for your support"
    # 並且包裝上標註 "滿額贈品，感謝您的支持"
    And ship together with purchased items
    # 並且與購買商品一起配送

  Scenario: Gift quality assurance
    # 場景: 贈品品質保證
    Given customer receives defective gift
    # 假設客戶收到的贈品有品質問題
    When customer requests gift replacement
    # 當客戶申請贈品更換
    Then should provide free replacement service
    # 那麼應該免費提供更換服務
    And not affect original purchase warranty
    # 並且不影響原購買商品的保固
    And record quality issues for gift selection improvement
    # 並且記錄品質問題以改善贈品選擇
