# Given-When-Then 模式指南

## 概述

Given-When-Then 是 BDD (行為驅動開發) 的核心模式，用於以自然語言描述系統行為。本指南詳細說明如何在專案中正確使用這個模式。

## 🔤 Given-When-Then 結構

### 基本語法

```gherkin
Feature: Customer Discount Calculation
  Scenario: Premium customer receives discount
    Given a premium customer with membership level "PREMIUM"
    And the customer has been active for more than 1 year
    When the customer makes a purchase of $100
    Then the customer should receive a 10% discount
    And the final amount should be $90
```

### 語法元素說明

#### Given (前置條件)
- **目的**: 設定測試的初始狀態
- **內容**: 系統狀態、資料準備、環境設定
- **關鍵字**: Given, And, But

#### When (觸發動作)
- **目的**: 描述被測試的行為或事件
- **內容**: 用戶操作、系統事件、API 呼叫
- **關鍵字**: When, And, But

#### Then (預期結果)
- **目的**: 驗證系統的回應或狀態變化
- **內容**: 預期輸出、狀態變化、副作用
- **關鍵字**: Then, And, But

## 📝 實作範例

### Java Step Definitions

```java
@CucumberTest
public class CustomerDiscountSteps {
    
    private Customer customer;
    private Order order;
    private BigDecimal discount;
    private BigDecimal finalAmount;
    
    @Given("a premium customer with membership level {string}")
    public void a_premium_customer_with_membership_level(String membershipLevel) {
        customer = CustomerTestBuilder.aCustomer()
            .withMembershipLevel(MembershipLevel.valueOf(membershipLevel))
            .build();
    }
    
    @Given("the customer has been active for more than {int} year")
    public void the_customer_has_been_active_for_more_than_year(int years) {
        LocalDate registrationDate = LocalDate.now().minusYears(years + 1);
        customer = customer.withRegistrationDate(registrationDate);
    }
    
    @When("the customer makes a purchase of ${double}")
    public void the_customer_makes_a_purchase_of(double amount) {
        order = OrderTestBuilder.anOrder()
            .withTotal(new BigDecimal(amount))
            .withCustomer(customer)
            .build();
        
        discount = customerService.calculateDiscount(customer, order);
        finalAmount = order.getTotal().subtract(discount);
    }
    
    @Then("the customer should receive a {int}% discount")
    public void the_customer_should_receive_a_discount(int expectedPercentage) {
        BigDecimal expectedDiscount = order.getTotal()
            .multiply(new BigDecimal(expectedPercentage))
            .divide(new BigDecimal(100));
        
        assertThat(discount).isEqualTo(expectedDiscount);
    }
    
    @Then("the final amount should be ${double}")
    public void the_final_amount_should_be(double expectedAmount) {
        assertThat(finalAmount).isEqualTo(new BigDecimal(expectedAmount));
    }
}
```##
 🎯 最佳實踐

### 1. Given 最佳實踐

#### ✅ 好的 Given 範例
```gherkin
Given a customer with email "john@example.com"
And the customer has a premium membership
And the customer's account balance is $500
```

#### ❌ 避免的 Given 範例
```gherkin
Given I login to the system
And I navigate to the customer page
And I click on the premium customer
# 太多實作細節，應該專注於狀態而非步驟
```

### 2. When 最佳實踐

#### ✅ 好的 When 範例
```gherkin
When the customer places an order for $100
```

#### ❌ 避免的 When 範例
```gherkin
When I click the order button
And I enter $100 in the amount field
And I click submit
# 太多 UI 互動細節
```

### 3. Then 最佳實踐

#### ✅ 好的 Then 範例
```gherkin
Then the order should be created successfully
And the customer should receive a confirmation email
And the inventory should be updated
```

#### ❌ 避免的 Then 範例
```gherkin
Then I should see "Order created" message
And the page should redirect to order confirmation
# 太多 UI 細節，應該專注於業務結果
```

## 📊 複雜場景範例

### 場景大綱 (Scenario Outline)

```gherkin
Feature: Customer Membership Discounts

  Scenario Outline: Different membership levels receive different discounts
    Given a customer with membership level "<membership>"
    And the customer has been active for <years> years
    When the customer makes a purchase of $<amount>
    Then the customer should receive a <discount>% discount
    And the final amount should be $<final_amount>

    Examples:
      | membership | years | amount | discount | final_amount |
      | STANDARD   | 1     | 100    | 0        | 100.00       |
      | PREMIUM    | 1     | 100    | 10       | 90.00        |
      | VIP        | 2     | 100    | 15       | 85.00        |
      | VIP        | 5     | 100    | 20       | 80.00        |
```

### 背景 (Background)

```gherkin
Feature: Order Processing

  Background:
    Given the system is running
    And the inventory service is available
    And the payment service is available

  Scenario: Successful order processing
    Given a customer with sufficient balance
    When the customer places an order
    Then the order should be processed successfully

  Scenario: Insufficient inventory
    Given a customer with sufficient balance
    But the requested item is out of stock
    When the customer places an order
    Then the order should be rejected
    And the customer should be notified about insufficient inventory
```

## 🔗 相關資源

- [Gherkin 語法指南](gherkin-guidelines.md)
- [Feature 文件編寫](feature-writing.md)
- [場景設計最佳實踐](scenario-design.md)

---

**最後更新**: 2025年1月21日  
**維護者**: QA Team  
**版本**: 1.0