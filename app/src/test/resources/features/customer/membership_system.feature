# 會員系統管理 - Membership System Management
Feature: Membership System Management
  As an e-commerce platform
  I want to provide a comprehensive membership system
  So that I can improve customer loyalty and purchase experience

  Background:
    # 假設系統中存在以下會員等級 - Given the following membership levels exist in the system
    Given the following membership levels exist in the system:
      | Level    | Min Spending | Discount Rate | Loyalty Rate | Birthday Discount |
      | BRONZE   |            0 |            0% |           1% |                5% |
      | SILVER   |        50000 |            3% |           2% |                8% |
      | GOLD     |       150000 |            5% |           3% |               10% |
      | PLATINUM |       300000 |            8% |           5% |               15% |
    # 並且系統中存在客戶 "張小明"，會員等級為 "SILVER"，累計消費 80000 元，紅利點數 1600 點
    # And customer "John" exists with SILVER level, total spending 80000, loyalty points 1600
    And customer "John" exists with SILVER level, total spending 80000, loyalty points 1600
  # 場景: 會員等級自動升級 - Scenario: Automatic membership level upgrade

  Scenario: Automatic membership level upgrade
    # 假設客戶 "張小明" 當前等級為 "SILVER"，累計消費 140000 元 - Given customer "John" current level is "SILVER" with total spending 140000
    Given customer "John" current level is "SILVER" with total spending 140000
    # 當客戶 "張小明" 完成一筆 15000 元的訂單 - When customer "John" completes an order of 15000
    When customer "John" completes an order of 15000
    # 那麼客戶的累計消費應該更新為 155000 元 - Then customer's total spending should be updated to 155000
    Then customer's total spending should be updated to 155000
    # 並且客戶的會員等級應該自動升級為 "GOLD" - And customer's membership level should be automatically upgraded to "GOLD"
    And customer's membership level should be automatically upgraded to "GOLD"
    # 並且系統應該發送等級升級通知 - And system should send level upgrade notification
    And system should send level upgrade notification
  # 場景: 會員折扣計算 - Scenario: Member discount calculation

  Scenario: Member discount calculation
    # 假設客戶 "張小明" 是 "SILVER" 會員 - Given customer "John" is a "SILVER" member
    Given customer "John" is a "SILVER" member
    # 當客戶購買總金額為 10000 元的商品 - When customer purchases products totaling 10000
    When customer purchases products totaling 10000
    # 那麼應該享受 3% 的會員折扣 - Then should receive 3% member discount
    Then should receive 3% member discount
    # 並且折扣金額應該為 300 元 - And discount amount should be 300
    And discount amount should be 300
    # 並且最終付款金額應該為 9700 元 - And final payment amount should be 9700
    And final payment amount should be 9700
  # 場景: 紅利點數累積 - Scenario: Loyalty points accumulation

  Scenario: Loyalty points accumulation
    # 假設客戶 "張小明" 是 "SILVER" 會員，當前紅利點數為 1600 點 - Given customer "John" is "SILVER" member with 1600 loyalty points
    Given customer "John" is "SILVER" member with 1600 loyalty points
    # 當客戶完成一筆 5000 元的訂單 - When customer completes an order of 5000
    When customer completes an order of 5000
    # 那麼應該獲得 100 點紅利點數 (5000 * 2%) - Then should earn 100 loyalty points (5000 * 2%)
    Then should earn 100 loyalty points
    # 並且總紅利點數應該更新為 1700 點 - And total loyalty points should be updated to 1700
    And total loyalty points should be updated to 1700
  # 場景: 紅利點數兌換 - Scenario: Loyalty points redemption

  Scenario: Loyalty points redemption
    # 假設客戶 "張小明" 有 2000 點紅利點數 - Given customer "John" has 2000 loyalty points
    Given customer "John" has 2000 loyalty points
    # 並且兌換比例為 100 點 = 10 元 - And redemption rate is 100 points = 10 dollars
    And redemption rate is 100 points = 10 dollars
    # 當客戶選擇兌換 1000 點 - When customer chooses to redeem 1000 points
    When customer chooses to redeem 1000 points
    # 那麼應該獲得 100 元的購物金 - Then should receive 100 dollars shopping credit
    Then should receive 100 dollars shopping credit
    # 並且剩餘紅利點數應該為 1000 點 - And remaining loyalty points should be 1000
    And remaining loyalty points should be 1000
  # 場景: 生日月份折扣 - Scenario: Birthday month discount

  Scenario: Birthday month discount
    # 假設客戶 "張小明" 是 "SILVER" 會員 - Given customer "John" is a "SILVER" member
    Given customer "John" is a "SILVER" member
    # 並且當前月份是客戶的生日月份 - And current month is customer's birthday month
    And current month is customer's birthday month
    # 當客戶購買總金額為 5000 元的商品 - When customer purchases products totaling 5000
    When customer purchases products totaling 5000
    # 那麼應該享受 8% 的生日折扣 - Then should receive 8% birthday discount
    Then should receive 8% birthday discount
    # 並且折扣金額應該為 400 元 - And discount amount should be 400
    And discount amount should be 400
    # 並且最終付款金額應該為 4600 元 - And final payment amount should be 4600
    And final payment amount should be 4600
  # 場景: 多重折扣優先級 - Scenario: Multiple discount priority

  Scenario: Multiple discount priority
    # 假設客戶 "張小明" 是 "GOLD" 會員 - Given customer "John" is a "GOLD" member
    Given customer "John" is a "GOLD" member
    # 並且當前月份是客戶的生日月份 - And current month is customer's birthday month
    And current month is customer's birthday month
    # 並且客戶有一張 15% 折扣的優惠券 - And customer has a 15% discount coupon
    And customer has a 15% discount coupon
    # 當客戶購買總金額為 10000 元的商品 - When customer purchases products totaling 10000
    When customer purchases products totaling 10000
    # 那麼系統應該選擇最優惠的折扣 (15% 優惠券) - Then system should apply the best discount (15% coupon)
    Then system should apply the best discount of 15%
    # 並且折扣金額應該為 1500 元 - And discount amount should be 1500
    And discount amount should be 1500
    # 並且最終付款金額應該為 8500 元 - And final payment amount should be 8500
    And final payment amount should be 8500
  # 場景: 會員專屬商品 - Scenario: Member exclusive products

  Scenario: Member exclusive products
    # 假設存在會員專屬商品 "限量版手機"，僅限 "GOLD" 以上會員購買 - Given member exclusive product "Limited Edition Phone" exists for GOLD+ members only
    Given member exclusive product "Limited Edition Phone" exists for GOLD+ members only
    # 當 "SILVER" 會員嘗試購買該商品 - When "SILVER" member tries to purchase the product
    When "SILVER" member tries to purchase the product
    # 那麼系統應該顯示 "此商品僅限 GOLD 以上會員購買" - Then system should display "This product is only available for GOLD+ members"
    Then system should display "This product is only available for GOLD+ members"
    # 並且無法將商品加入購物車 - And should not allow adding product to cart
    And should not allow adding product to cart
    # 當 "GOLD" 會員查看該商品 - When "GOLD" member views the product
    When "GOLD" member views the product
    # 那麼應該能夠正常購買 - Then should be able to purchase normally
    Then should be able to purchase normally
  # 場景: 會員積分過期 - Scenario: Loyalty points expiration

  Scenario: Loyalty points expiration
    # 假設客戶 "張小明" 有 500 點即將在 30 天內過期的紅利點數 - Given customer "John" has 500 loyalty points expiring in 30 days
    Given customer "John" has 500 loyalty points expiring in 30 days
    # 當系統檢查積分過期狀態 - When system checks points expiration status
    When system checks points expiration status
    # 那麼應該發送積分即將過期的提醒通知 - Then should send points expiration reminder notification
    Then should send points expiration reminder notification
    # 並且建議客戶儘快使用積分 - And should suggest customer to use points soon
    And should suggest customer to use points soon
  # 場景: 會員等級保級 - Scenario: Membership level maintenance

  Scenario: Membership level maintenance
    # 假設客戶 "張小明" 是 "GOLD" 會員，但過去 12 個月消費不足 150000 元 - Given customer "John" is "GOLD" member but spent less than 150000 in past 12 months
    Given customer "John" is "GOLD" member but spent less than 150000 in past 12 months
    # 當系統進行年度會員等級審核 - When system performs annual membership level review
    When system performs annual membership level review
    # 那麼客戶等級應該降級為 "SILVER" - Then customer level should be downgraded to "SILVER"
    Then customer level should be downgraded to "SILVER"
    # 並且系統應該發送等級變更通知 - And system should send level change notification
    And system should send level change notification
  # 場景: 新會員註冊獎勵 - Scenario: New member registration reward

  Scenario: New member registration reward
    # 當新客戶 "李小華" 完成註冊 - When new customer "Alice" completes registration
    When new customer "Alice" completes registration
    # 那麼應該自動獲得 "BRONZE" 會員等級 - Then should automatically receive "BRONZE" membership level
    Then should automatically receive "BRONZE" membership level
    # 並且應該獲得 100 點歡迎紅利點數 - And should receive 100 welcome loyalty points
    And should receive 100 welcome loyalty points
    # 並且應該獲得新會員專屬優惠券 - And should receive new member exclusive coupon
    And should receive new member exclusive coupon
  # 場景: 會員推薦獎勵 - Scenario: Member referral reward

  Scenario: Member referral reward
    # 假設客戶 "張小明" 推薦新客戶 "王小美" 註冊 - Given customer "John" refers new customer "Mary" to register
    Given customer "John" refers new customer "Mary" to register
    # 當 "王小美" 完成首次購買 - When "Mary" completes first purchase
    When "Mary" completes first purchase
    # 那麼 "張小明" 應該獲得 200 點推薦獎勵 - Then "John" should receive 200 referral reward points
    Then "John" should receive 200 referral reward points
    # 並且 "王小美" 應該獲得 100 點新會員獎勵 - And "Mary" should receive 100 new member reward points
    And "Mary" should receive 100 new member reward points
  # 場景: 會員資料更新 - Scenario: Member profile update

  Scenario: Member profile update
    # 當客戶 "張小明" 更新生日資訊 - When customer "John" updates birthday information
    When customer "John" updates birthday information
    # 那麼系統應該記錄新的生日日期 - Then system should record new birthday date
    Then system should record new birthday date
    # 並且在生日月份時自動啟用生日折扣 - And should automatically enable birthday discount in birthday month
    And should automatically enable birthday discount in birthday month
  # 場景: 會員消費統計 - Scenario: Member spending statistics

  Scenario: Member spending statistics
    # 假設客戶 "張小明" 在過去 12 個月的消費記錄如下 - Given customer "John" has following spending records in past 12 months
    Given customer "John" has following spending records in past 12 months:
      | Month | Amount |
      | Jan   |   5000 |
      | Feb   |   8000 |
      | Mar   |  12000 |
    # 當查詢客戶的消費統計 - When querying customer's spending statistics
    When querying customer's spending statistics
    # 那麼應該顯示總消費金額為 25000 元 - Then should display total spending amount as 25000
    Then should display total spending amount as 25000
    # 並且應該顯示平均月消費為 8333 元 - And should display average monthly spending as 8333
    And should display average monthly spending as 8333
    # 並且應該顯示消費趨勢圖表 - And should display spending trend chart
    And should display spending trend chart
