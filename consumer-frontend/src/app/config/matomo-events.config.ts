// Matomo 事件追蹤配置 - 基於 BDD Feature Files
// 創建日期: 2025年9月23日 下午4:13 (台北時間)
// 基於: membership_system.feature, member_discounts.feature, reward_points.feature

export interface MatomoEventConfig {
  category: string;
  action: string;
  name?: string;
  value?: number;
  customData?: Record<string, any>;
}

/**
 * 基於 membership_system.feature 的追蹤事件配置
 */
export const MEMBERSHIP_EVENTS = {
  // 會員等級升級事件 (Scenario: Automatic membership level upgrade)
  LEVEL_UPGRADE: {
    BRONZE_TO_SILVER: {
      category: 'Membership',
      action: 'Level Upgrade',
      name: 'BRONZE to SILVER',
      threshold: 50000
    },
    SILVER_TO_GOLD: {
      category: 'Membership',
      action: 'Level Upgrade', 
      name: 'SILVER to GOLD',
      threshold: 150000
    },
    GOLD_TO_PLATINUM: {
      category: 'Membership',
      action: 'Level Upgrade',
      name: 'GOLD to PLATINUM', 
      threshold: 300000
    }
  },

  // 會員折扣使用事件 (Scenario: Member discount calculation)
  DISCOUNT_USAGE: {
    BRONZE_DISCOUNT: {
      category: 'Membership',
      action: 'Discount Applied',
      name: 'BRONZE Member Discount',
      rate: 0 // 0%
    },
    SILVER_DISCOUNT: {
      category: 'Membership', 
      action: 'Discount Applied',
      name: 'SILVER Member Discount',
      rate: 0.03 // 3%
    },
    GOLD_DISCOUNT: {
      category: 'Membership',
      action: 'Discount Applied', 
      name: 'GOLD Member Discount',
      rate: 0.05 // 5%
    },
    PLATINUM_DISCOUNT: {
      category: 'Membership',
      action: 'Discount Applied',
      name: 'PLATINUM Member Discount', 
      rate: 0.08 // 8%
    }
  },

  // 紅利點數累積事件 (Scenario: Loyalty points accumulation)
  POINTS_ACCUMULATION: {
    BRONZE_RATE: {
      category: 'Loyalty',
      action: 'Points Earned',
      name: 'BRONZE Rate',
      rate: 0.01 // 1%
    },
    SILVER_RATE: {
      category: 'Loyalty',
      action: 'Points Earned', 
      name: 'SILVER Rate',
      rate: 0.02 // 2%
    },
    GOLD_RATE: {
      category: 'Loyalty',
      action: 'Points Earned',
      name: 'GOLD Rate', 
      rate: 0.03 // 3%
    },
    PLATINUM_RATE: {
      category: 'Loyalty',
      action: 'Points Earned',
      name: 'PLATINUM Rate',
      rate: 0.05 // 5%
    }
  },

  // 紅利點數兌換事件 (Scenario: Loyalty points redemption)
  POINTS_REDEMPTION: {
    FULL_REDEMPTION: {
      category: 'Loyalty',
      action: 'Points Redeemed',
      name: 'Full Redemption',
      conversionRate: 0.1 // 100 points = 10 dollars
    },
    PARTIAL_REDEMPTION: {
      category: 'Loyalty', 
      action: 'Points Redeemed',
      name: 'Partial Redemption',
      conversionRate: 0.1
    }
  },

  // 生日折扣事件 (Scenario: Birthday month discount)
  BIRTHDAY_DISCOUNT: {
    BRONZE_BIRTHDAY: {
      category: 'Membership',
      action: 'Birthday Discount',
      name: 'BRONZE Birthday',
      rate: 0.05 // 5%
    },
    SILVER_BIRTHDAY: {
      category: 'Membership',
      action: 'Birthday Discount', 
      name: 'SILVER Birthday',
      rate: 0.08 // 8%
    },
    GOLD_BIRTHDAY: {
      category: 'Membership',
      action: 'Birthday Discount',
      name: 'GOLD Birthday',
      rate: 0.10 // 10%
    },
    PLATINUM_BIRTHDAY: {
      category: 'Membership',
      action: 'Birthday Discount',
      name: 'PLATINUM Birthday', 
      rate: 0.15 // 15%
    }
  },

  // 會員專屬商品事件 (Scenario: Member exclusive products)
  EXCLUSIVE_PRODUCTS: {
    ACCESS_DENIED: {
      category: 'Exclusive',
      action: 'Access Denied',
      name: 'Insufficient Level'
    },
    ACCESS_GRANTED: {
      category: 'Exclusive',
      action: 'Access Granted', 
      name: 'Eligible Member'
    },
    PURCHASE_ATTEMPT: {
      category: 'Exclusive',
      action: 'Purchase Attempt',
      name: 'Exclusive Product'
    }
  },

  // 會員推薦事件 (Scenario: Member referral reward)
  REFERRAL_SYSTEM: {
    REFERRAL_SENT: {
      category: 'Referral',
      action: 'Invitation Sent',
      name: 'Member Referral'
    },
    REFERRAL_REGISTERED: {
      category: 'Referral',
      action: 'Registration Complete',
      name: 'Referred Member'
    },
    REFERRAL_REWARD: {
      category: 'Referral',
      action: 'Reward Earned',
      name: 'Referrer Bonus',
      points: 200
    },
    NEW_MEMBER_BONUS: {
      category: 'Referral',
      action: 'Reward Earned',
      name: 'New Member Bonus',
      points: 100
    }
  }
};

/**
 * 基於 member_discounts.feature 的追蹤事件配置
 */
export const DISCOUNT_EVENTS = {
  // 新會員首購折扣 (Scenario: New member receives a first purchase discount)
  NEW_MEMBER_DISCOUNT: {
    FIRST_PURCHASE: {
      category: 'Discount',
      action: 'New Member Discount',
      name: 'First Purchase 15%',
      rate: 0.15,
      eligibilityDays: 30
    }
  },

  // 生日月份折扣 (Scenario: Member receives a birthday month discount)
  BIRTHDAY_MONTH_DISCOUNT: {
    BIRTHDAY_DISCOUNT: {
      category: 'Discount',
      action: 'Birthday Discount',
      name: 'Birthday Month 10%',
      rate: 0.10,
      maxDiscount: 100
    }
  },

  // 多重折扣優先級 (Scenario: Member with multiple eligible discounts)
  MULTIPLE_DISCOUNTS: {
    BEST_DISCOUNT_APPLIED: {
      category: 'Discount',
      action: 'Best Discount Applied',
      name: 'Highest Value Selected'
    },
    DISCOUNT_COMPARISON: {
      category: 'Discount',
      action: 'Discount Comparison',
      name: 'Multiple Options Available'
    }
  }
};

/**
 * 基於 reward_points.feature 的追蹤事件配置
 */
export const REWARD_POINTS_EVENTS = {
  // 點數兌換成功 (Scenario: Customer uses reward points for a discount)
  POINTS_REDEMPTION_SUCCESS: {
    FULL_REDEMPTION: {
      category: 'Loyalty',
      action: 'Points Redeemed',
      name: 'Full Amount',
      conversionRate: 0.1 // 10 points = $1
    },
    PARTIAL_REDEMPTION: {
      category: 'Loyalty', 
      action: 'Points Redeemed',
      name: 'Partial Amount',
      conversionRate: 0.1
    }
  },

  // 點數不足錯誤 (Scenario: Customer attempts to redeem more points than available)
  INSUFFICIENT_POINTS_ERROR: {
    REDEMPTION_FAILED: {
      category: 'Error',
      action: 'Insufficient Points',
      name: 'Redemption Failed'
    },
    ERROR_MESSAGE_SHOWN: {
      category: 'Error',
      action: 'Error Message',
      name: 'Points Insufficient'
    }
  }
};

/**
 * 廣泛用戶行為追蹤事件配置
 */
export const GENERAL_USER_EVENTS = {
  // 電商轉換漏斗
  CONVERSION_FUNNEL: {
    PRODUCT_VIEW: {
      category: 'Funnel',
      action: 'Product View',
      name: 'Step 1'
    },
    ADD_TO_CART: {
      category: 'Funnel',
      action: 'Add to Cart', 
      name: 'Step 2'
    },
    CHECKOUT_START: {
      category: 'Funnel',
      action: 'Checkout Start',
      name: 'Step 3'
    },
    PAYMENT_INFO: {
      category: 'Funnel',
      action: 'Payment Info',
      name: 'Step 4'
    },
    ORDER_COMPLETE: {
      category: 'Funnel',
      action: 'Order Complete',
      name: 'Step 5'
    }
  },

  // 購物車行為
  CART_BEHAVIOR: {
    CART_ABANDONMENT: {
      category: 'Cart',
      action: 'Abandonment',
      name: 'Cart Left'
    },
    CART_RECOVERY: {
      category: 'Cart',
      action: 'Recovery',
      name: 'Returned to Cart'
    },
    QUANTITY_CHANGE: {
      category: 'Cart',
      action: 'Quantity Change',
      name: 'Item Updated'
    },
    REMOVE_ITEM: {
      category: 'Cart',
      action: 'Remove Item',
      name: 'Item Removed'
    }
  },

  // 搜尋行為
  SEARCH_BEHAVIOR: {
    SEARCH_QUERY: {
      category: 'Search',
      action: 'Query Submitted',
      name: 'Site Search'
    },
    SEARCH_RESULT_CLICK: {
      category: 'Search',
      action: 'Result Click',
      name: 'Search Result'
    },
    NO_RESULTS: {
      category: 'Search',
      action: 'No Results',
      name: 'Empty Results'
    },
    SEARCH_REFINEMENT: {
      category: 'Search',
      action: 'Refinement',
      name: 'Filter Applied'
    }
  },

  // 商品推薦
  PRODUCT_RECOMMENDATIONS: {
    RECOMMENDATION_VIEW: {
      category: 'Recommendation',
      action: 'View',
      name: 'Recommendation Shown'
    },
    RECOMMENDATION_CLICK: {
      category: 'Recommendation',
      action: 'Click',
      name: 'Recommendation Selected'
    },
    CROSS_SELL_CLICK: {
      category: 'Recommendation',
      action: 'Cross-sell Click',
      name: 'Related Product'
    },
    UPSELL_CLICK: {
      category: 'Recommendation',
      action: 'Upsell Click', 
      name: 'Premium Product'
    }
  },

  // 用戶細分
  USER_SEGMENTATION: {
    NEW_USER: {
      category: 'Segment',
      action: 'New User',
      name: 'First Visit'
    },
    RETURNING_USER: {
      category: 'Segment',
      action: 'Returning User',
      name: 'Repeat Visit'
    },
    HIGH_VALUE_USER: {
      category: 'Segment',
      action: 'High Value',
      name: 'Premium Customer'
    },
    FREQUENT_BUYER: {
      category: 'Segment',
      action: 'Frequent Buyer',
      name: 'Regular Customer'
    }
  },

  // 頁面互動
  PAGE_INTERACTIONS: {
    SCROLL_DEPTH: {
      category: 'Engagement',
      action: 'Scroll Depth',
      name: 'Page Scroll'
    },
    TIME_ON_PAGE: {
      category: 'Engagement',
      action: 'Time on Page',
      name: 'Page Duration'
    },
    CLICK_HEATMAP: {
      category: 'Engagement',
      action: 'Click',
      name: 'Element Click'
    },
    FORM_INTERACTION: {
      category: 'Engagement',
      action: 'Form Interaction',
      name: 'Form Field'
    }
  }
};

/**
 * 事件追蹤輔助函數
 */
export class MatomoEventHelper {
  
  /**
   * 根據會員等級獲取對應的折扣事件配置
   */
  static getDiscountEventByLevel(membershipLevel: string): any {
    const levelKey = `${membershipLevel}_DISCOUNT`;
    return MEMBERSHIP_EVENTS.DISCOUNT_USAGE[levelKey as keyof typeof MEMBERSHIP_EVENTS.DISCOUNT_USAGE];
  }

  /**
   * 根據會員等級獲取對應的紅利點數累積事件配置
   */
  static getPointsEventByLevel(membershipLevel: string): any {
    const levelKey = `${membershipLevel}_RATE`;
    return MEMBERSHIP_EVENTS.POINTS_ACCUMULATION[levelKey as keyof typeof MEMBERSHIP_EVENTS.POINTS_ACCUMULATION];
  }

  /**
   * 根據會員等級獲取對應的生日折扣事件配置
   */
  static getBirthdayEventByLevel(membershipLevel: string): any {
    const levelKey = `${membershipLevel}_BIRTHDAY`;
    return MEMBERSHIP_EVENTS.BIRTHDAY_DISCOUNT[levelKey as keyof typeof MEMBERSHIP_EVENTS.BIRTHDAY_DISCOUNT];
  }

  /**
   * 檢查是否符合會員等級升級條件
   */
  static checkLevelUpgradeEligibility(currentLevel: string, totalSpending: number): string | null {
    const upgrades = MEMBERSHIP_EVENTS.LEVEL_UPGRADE;
    
    if (currentLevel === 'BRONZE' && totalSpending >= upgrades.BRONZE_TO_SILVER.threshold) {
      return 'SILVER';
    }
    if (currentLevel === 'SILVER' && totalSpending >= upgrades.SILVER_TO_GOLD.threshold) {
      return 'GOLD';
    }
    if (currentLevel === 'GOLD' && totalSpending >= upgrades.GOLD_TO_PLATINUM.threshold) {
      return 'PLATINUM';
    }
    
    return null;
  }

  /**
   * 計算紅利點數累積
   */
  static calculateLoyaltyPoints(membershipLevel: string, orderAmount: number): number {
    const pointsEvent = this.getPointsEventByLevel(membershipLevel);
    return pointsEvent ? Math.floor(orderAmount * pointsEvent.rate) : 0;
  }

  /**
   * 計算會員折扣金額
   */
  static calculateMemberDiscount(membershipLevel: string, orderAmount: number): number {
    const discountEvent = this.getDiscountEventByLevel(membershipLevel);
    return discountEvent ? orderAmount * discountEvent.rate : 0;
  }
}