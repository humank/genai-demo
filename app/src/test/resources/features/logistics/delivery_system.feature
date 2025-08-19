# 物流配送系統 (Logistics Delivery System)
Feature: Logistics Delivery System
  As an e-commerce platform
  I want to provide comprehensive logistics delivery services
  So that customers can receive their products on time

  Background:
    Given the following delivery methods exist in the system:
      | Delivery Method | Delivery Fee | Estimated Days | Weight Limit | Region Limit        |
      | Standard        |          100 |            3-5 |         30kg | All Taiwan          |
      | Express         |          200 |            1-2 |         10kg | North/Central/South |
      | Store Pickup    |           60 |            2-3 |          5kg | All Taiwan          |
      | Same Day        |          300 |              1 |          3kg | Taipei City         |
    And customer "John" has default delivery address "100 Xinyi Road, Xinyi District, Taipei City"

  Scenario: Select delivery method
    # 選擇配送方式
    Given customer's cart weight is 2kg and delivery address is Taipei City
    When customer views available delivery methods
    Then should display all applicable delivery methods:
      | Delivery Method | Fee | Estimated Days |
      | Standard        | 100 |            3-5 |
      | Express         | 200 |            1-2 |
      | Store Pickup    |  60 |            2-3 |
      | Same Day        | 300 |              1 |

  Scenario: Delivery method restrictions - Weight limit exceeded
    # 配送方式限制 - 重量超限
    Given customer's cart weight is 15kg
    When customer views available delivery methods
    Then should not display "Store Pickup" (limited to 5kg)
    And should not display "Same Day" (limited to 3kg)
    And should display weight limit explanation

  Scenario: Delivery method restrictions - Region restrictions
    # 配送方式限制 - 地區限制
    Given customer's delivery address is "Hualien City, Hualien County"
    When customer views available delivery methods
    Then should not display "Express" (limited to North/Central/South)
    And should not display "Same Day" (limited to Taipei City)
    And should display region restriction explanation

  Scenario: Free shipping threshold
    # 免運費門檻
    Given standard delivery has free shipping over 1000
    And customer's cart total amount is 1200
    When customer selects standard delivery
    Then delivery fee should be 0
    And should display "Free shipping" label

  Scenario: Delivery address management
    # 配送地址管理
    When customer adds new delivery address
      | Recipient | Phone      | Address                             |
      | Lisa Chen | 0912345678 | 100 Taiwan Boulevard, Taichung City |
    Then address should be successfully saved
    And customer can select this address during checkout

  Scenario: Delivery time appointment
    # 配送時間預約
    Given customer selects "Express" delivery
    When customer schedules delivery time for "tomorrow afternoon 2-6 PM"
    Then system should confirm time availability
    And save delivery time preference
    And notify logistics provider during delivery

  Scenario: Delivery status tracking
    # 配送狀態追蹤
    Given order "550e8400-e29b-41d4-a716-446655440001" has been shipped with tracking number "SF123456789"
    When customer queries delivery status
    Then should display the following delivery progress:
      | Time             | Status           | Location         | Description           |
      | 2024-01-15 10:00 | Shipped          | Taipei Warehouse | Item dispatched       |
      | 2024-01-15 14:30 | In Transit       | Taipei Hub       | In transit            |
      | 2024-01-16 09:15 | Out for Delivery | Xinyi District   | Courier has picked up |

  Scenario: Delivery exception handling - Address error
    # 配送異常處理 - 地址錯誤
    Given delivery person arrives at delivery address but cannot find recipient address
    When delivery person reports "Address not found"
    Then delivery status should be updated to "Delivery Exception"
    And system should notify customer to confirm address
    And pause delivery until address confirmation

  Scenario: Delivery exception handling - No one to receive
    # 配送異常處理 - 無人收件
    Given delivery person arrives at delivery address but no one to receive
    When delivery person attempts delivery 3 times with no one to receive
    Then delivery status should be updated to "Delivery Failed"
    And item should be returned to warehouse
    And customer should receive redelivery arrangement notification

  Scenario: Store pickup process
    # 超商取貨流程
    Given customer selects "7-ELEVEN Xinyi Store" for pickup
    When item arrives at designated store
    Then customer should receive pickup notification SMS
    And SMS should contain pickup code "A12345"
    And item storage period should be 7 days

  Scenario: Store pickup overdue handling
    # 超商取貨逾期處理
    Given customer's item has been stored at convenience store for over 7 days
    When system checks overdue items
    Then item should be returned to warehouse
    And customer should receive overdue notification
    And can choose redelivery or refund

  Scenario: Same day delivery service
    # 當日配送服務
    Given customer places order at 11:00 AM selecting same day delivery
    And delivery address is in Taipei City
    When system confirms same day delivery feasibility
    Then should arrange afternoon delivery
    And customer should receive estimated delivery time notification

  Scenario: Delivery fee calculation - Multiple items
    # 配送費用計算 - 多件商品
    Given cart contains the following items:
      | Item    | Weight | Volume |
      | Phone   |  0.2kg | Small  |
      | Laptop  |  2.5kg | Medium |
      | Monitor |    8kg | Large  |
    When customer selects standard delivery
    Then should calculate delivery fee based on total weight 10.7kg
    And consider packaging requirements for largest volume item

  Scenario: Split delivery
    # 分批配送
    Given order contains in-stock and pre-order items:
      | Item      | Status    | Expected Ship Time |
      | Phone     | In Stock  | Immediate          |
      | Earphones | In Stock  | Immediate          |
      | Watch     | Pre-order |       7 days later |
    When customer selects "Split Delivery"
    Then in-stock items should ship first
    And pre-order items should ship after arrival
    And each shipment should have independent tracking number

  Scenario: Delivery insurance
    # 配送保險
    Given customer purchases items worth 50000
    When customer selects delivery insurance
    Then insurance fee should be 0.5% of item value
    And insurance fee should be 250
    And full compensation available for delivery damage

  Scenario: Delivery rating
    # 配送評價
    Given customer has received items
    When customer rates delivery service
    And gives 5-star rating with comment "Fast delivery, perfect packaging"
    Then rating should be recorded
    And affect delivery person's service score

  Scenario: Emergency delivery
    # 緊急配送
    Given customer needs emergency delivery for medical supplies
    When customer selects "Emergency Delivery" with reason
    Then system should prioritize this order
    And arrange fastest delivery method
    And customer should receive real-time delivery tracking
