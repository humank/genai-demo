Feature: Coupon System
  As an e-commerce platform
  I want to provide diverse coupons
  So that I can promote sales and improve customer satisfaction

  Background:
    Given the following coupons exist in the system:
      | Coupon Code | Type       | Discount Value | Min Spend | Usage Limit | Valid Until | Status  |
      | WELCOME10   | Percentage |            10% |         0 |           1 |  2024-12-31 | Active  |
      | SAVE500     | Fixed      |            500 |      5000 |           1 |  2024-12-31 | Active  |
      | VIP20       | Percentage |            20% |     10000 |           3 |  2024-12-31 | Active  |
      | EXPIRED     | Percentage |            15% |         0 |           1 |  2024-01-01 | Expired |
      | CATEGORY50  | Fixed      |             50 |       500 |           5 |  2024-12-31 | Active  |
    And customer "John Doe" has received coupons "WELCOME10" and "SAVE500"

  Scenario: Successfully apply percentage coupon
    Given customer "John Doe" cart total is 2000
    When customer applies coupon "WELCOME10"
    Then coupon discount amount should be 200
    And coupon final amount should be 1800
    And coupon usage count should decrease by 1

  Scenario: Successfully apply fixed amount coupon
    Given customer "John Doe" cart total is 6000
    When customer applies coupon "SAVE500"
    Then coupon discount amount should be 500
    And coupon final amount should be 5500

  Scenario: Coupon minimum spend requirement
    假設客戶 "張小明" 的購物車總金額為 3000 元
    當客戶嘗試應用優惠券 "SAVE500"
    那麼系統應該顯示錯誤訊息 "此優惠券需要最低消費 5000 元"
    並且優惠券不應該被應用

  場景: 優惠券已過期
    當客戶嘗試應用優惠券 "EXPIRED"
    那麼系統應該顯示錯誤訊息 "此優惠券已過期"
    並且優惠券不應該被應用

  場景: 優惠券使用次數已用完
    假設優惠券 "WELCOME10" 的剩餘使用次數為 0
    當客戶嘗試應用優惠券 "WELCOME10"
    那麼系統應該顯示錯誤訊息 "此優惠券已使用完畢"
    並且優惠券不應該被應用

  場景: 優惠券代碼不存在
    當客戶嘗試應用優惠券 "NOTEXIST"
    那麼系統應該顯示錯誤訊息 "優惠券代碼不存在"
    並且優惠券不應該被應用

  場景: 特定分類商品優惠券
    假設優惠券 "CATEGORY50" 僅適用於 "電子產品" 分類
    並且購物車中有電子產品 1000 元和服飾 500 元
    當客戶應用優惠券 "CATEGORY50"
    那麼折扣應該只應用於電子產品
    並且折扣金額應該為 50 元
    並且最終金額應該為 1450 元

  場景: 會員專屬優惠券
    假設優惠券 "VIP20" 僅限 "GOLD" 以上會員使用
    並且客戶 "張小明" 是 "SILVER" 會員
    當客戶嘗試應用優惠券 "VIP20"
    那麼系統應該顯示錯誤訊息 "此優惠券僅限 GOLD 以上會員使用"
    並且優惠券不應該被應用

  場景: 優惠券疊加限制
    假設客戶已應用優惠券 "WELCOME10"
    當客戶嘗試再應用優惠券 "SAVE500"
    那麼系統應該顯示錯誤訊息 "每筆訂單僅能使用一張優惠券"
    並且第二張優惠券不應該被應用

  場景: 移除已應用的優惠券
    假設客戶已應用優惠券 "WELCOME10"，折扣金額為 200 元
    當客戶選擇移除優惠券
    那麼折扣金額應該變為 0 元
    並且總金額應該恢復為原始金額
    並且優惠券使用次數應該恢復

  場景: 自動應用最優優惠券
    假設客戶擁有以下優惠券:
      | 優惠券代碼 | 折扣值 | 最低消費 |
      | SAVE100    |  100元 |   1000元 |
      | SAVE200    |  200元 |   2000元 |
      | PERCENT15  |    15% |   1500元 |
    並且購物車總金額為 3000 元
    當客戶選擇 "自動應用最優優惠券"
    那麼系統應該應用折扣最大的優惠券 "PERCENT15"
    並且折扣金額應該為 450 元

  場景: 優惠券發放
    當系統發放新會員優惠券給客戶 "李小華"
    那麼客戶應該收到優惠券 "NEWUSER15"
    並且優惠券應該出現在客戶的優惠券列表中
    並且客戶應該收到優惠券發放通知

  場景: 優惠券分享
    假設客戶 "張小明" 有分享型優惠券 "SHARE100"
    當客戶分享優惠券給 3 位朋友
    並且其中 2 位朋友使用了優惠券
    那麼客戶 "張小明" 應該獲得分享獎勵
    並且應該獲得額外的紅利點數

  場景: 批量優惠券管理
    當管理員批量發放生日優惠券給本月生日的會員
    那麼所有符合條件的會員都應該收到優惠券
    並且系統應該記錄發放日誌
    並且應該發送生日祝福通知

  場景: 優惠券使用統計
    假設優惠券 "WELCOME10" 已被使用 100 次
    當查詢優惠券使用統計
    那麼應該顯示使用次數為 100 次
    並且應該顯示總折扣金額
    並且應該顯示使用率和轉換率

  場景: 優惠券到期提醒
    假設客戶 "張小明" 有優惠券將在 3 天後到期
    當系統執行到期提醒任務
    那麼客戶應該收到優惠券即將到期的通知
    並且通知中應該包含優惠券詳情和使用建議
