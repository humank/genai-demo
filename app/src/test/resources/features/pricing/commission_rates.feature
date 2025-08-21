Feature: Platform Commission Rates
  As an e-commerce platform
  I want to apply different commission rates based on product categories and events
  So that the platform can generate appropriate revenue while incentivizing sellers

  Background:
    Given the seller is registered on the platform

  # 平台手續費與活動檔期差異
  Scenario: Seller participates in a promotional event with higher commission
    Given the normal commission rate for electronics is 3%
    And during the "Summer Sale" event the commission increases to 5%
    When the seller's product is sold during the "Summer Sale" event
    Then the platform deducts 5% commission from the sale price
    And the seller is notified of the higher commission rate 7 days before the event

  Scenario: Different commission rates for product categories
    Given the commission rates for different categories are as follows
      | Category    | Normal Rate | Event Rate |
      | Electronics | 3%          | 5%         |
      | Fashion     | 5%          | 8%         |
      | Groceries   | 2%          | 3%         |
    When a seller's "Fashion" product is sold during a promotional event
    Then the platform deducts 8% commission from the sale price