// Matomo 用戶行為追蹤服務
// 創建日期: 2025年9月23日 下午4:13 (台北時間)

import { Injectable } from '@angular/core';
import { MatomoTracker } from '@ngx-matomo/tracker';

export interface UserTrackingData {
  userId?: string;
  membershipLevel?: 'BRONZE' | 'SILVER' | 'GOLD' | 'PLATINUM';
  totalSpending?: number;
  loyaltyPoints?: number;
}

export interface ProductTrackingData {
  productId: string;
  productName: string;
  category: string;
  price: number;
  quantity?: number;
}

export interface OrderTrackingData {
  orderId: string;
  totalAmount: number;
  discountAmount?: number;
  discountType?: string;
  paymentMethod: string;
  items: ProductTrackingData[];
}

@Injectable({
  providedIn: 'root'
})
export class MatomoTrackingService {

  constructor(private matomoTracker: MatomoTracker) {}

  // === 用戶身份追蹤 ===
  
  /**
   * 設定用戶身份信息
   */
  setUserData(userData: UserTrackingData): void {
    if (userData.userId) {
      this.matomoTracker.setUserId(userData.userId);
    }
    
    // 設定自定義變數
    if (userData.membershipLevel) {
      this.matomoTracker.setCustomVariable(1, 'membershipLevel', userData.membershipLevel, 'visit');
    }
    
    if (userData.totalSpending) {
      this.matomoTracker.setCustomVariable(2, 'totalSpending', userData.totalSpending.toString(), 'visit');
    }
    
    if (userData.loyaltyPoints) {
      this.matomoTracker.setCustomVariable(3, 'loyaltyPoints', userData.loyaltyPoints.toString(), 'visit');
    }
  }

  /**
   * 追蹤用戶註冊
   */
  trackUserRegistration(membershipLevel: string = 'BRONZE'): void {
    this.matomoTracker.trackEvent('User', 'Registration', 'New Member', undefined, {
      membershipLevel: membershipLevel,
      timestamp: new Date().toISOString()
    });
  }

  /**
   * 追蹤用戶登入
   */
  trackUserLogin(userId: string, membershipLevel: string): void {
    this.matomoTracker.trackEvent('User', 'Login', 'Member Login', undefined, {
      userId: userId,
      membershipLevel: membershipLevel,
      timestamp: new Date().toISOString()
    });
  }

  // === 會員系統追蹤 ===

  /**
   * 追蹤會員等級升級
   */
  trackMembershipUpgrade(fromLevel: string, toLevel: string, totalSpending: number): void {
    this.matomoTracker.trackEvent('Membership', 'Level Upgrade', `${fromLevel} to ${toLevel}`, totalSpending, {
      fromLevel: fromLevel,
      toLevel: toLevel,
      totalSpending: totalSpending,
      timestamp: new Date().toISOString()
    });
  }

  /**
   * 追蹤會員折扣使用
   */
  trackMemberDiscount(discountType: string, discountRate: number, discountAmount: number, orderAmount: number): void {
    this.matomoTracker.trackEvent('Membership', 'Discount Used', discountType, discountAmount, {
      discountType: discountType,
      discountRate: discountRate,
      discountAmount: discountAmount,
      orderAmount: orderAmount,
      timestamp: new Date().toISOString()
    });
  }

  /**
   * 追蹤紅利點數累積
   */
  trackLoyaltyPointsEarned(pointsEarned: number, orderAmount: number, currentTotal: number): void {
    this.matomoTracker.trackEvent('Loyalty', 'Points Earned', 'Purchase Reward', pointsEarned, {
      pointsEarned: pointsEarned,
      orderAmount: orderAmount,
      currentTotal: currentTotal,
      timestamp: new Date().toISOString()
    });
  }

  /**
   * 追蹤紅利點數兌換
   */
  trackLoyaltyPointsRedemption(pointsRedeemed: number, cashValue: number, remainingPoints: number): void {
    this.matomoTracker.trackEvent('Loyalty', 'Points Redeemed', 'Shopping Credit', pointsRedeemed, {
      pointsRedeemed: pointsRedeemed,
      cashValue: cashValue,
      remainingPoints: remainingPoints,
      timestamp: new Date().toISOString()
    });
  }

  /**
   * 追蹤生日折扣使用
   */
  trackBirthdayDiscount(discountAmount: number, orderAmount: number, membershipLevel: string): void {
    this.matomoTracker.trackEvent('Membership', 'Birthday Discount', membershipLevel, discountAmount, {
      discountAmount: discountAmount,
      orderAmount: orderAmount,
      membershipLevel: membershipLevel,
      timestamp: new Date().toISOString()
    });
  }

  // === 購物流程追蹤 ===

  /**
   * 追蹤商品瀏覽
   */
  trackProductView(product: ProductTrackingData): void {
    this.matomoTracker.trackEvent('Product', 'View', product.productName, product.price, {
      productId: product.productId,
      category: product.category,
      price: product.price,
      timestamp: new Date().toISOString()
    });
  }

  /**
   * 追蹤加入購物車
   */
  trackAddToCart(product: ProductTrackingData): void {
    this.matomoTracker.trackEvent('Cart', 'Add Product', product.productName, product.price, {
      productId: product.productId,
      category: product.category,
      price: product.price,
      quantity: product.quantity || 1,
      timestamp: new Date().toISOString()
    });
  }

  /**
   * 追蹤從購物車移除商品
   */
  trackRemoveFromCart(product: ProductTrackingData): void {
    this.matomoTracker.trackEvent('Cart', 'Remove Product', product.productName, product.price, {
      productId: product.productId,
      category: product.category,
      price: product.price,
      quantity: product.quantity || 1,
      timestamp: new Date().toISOString()
    });
  }

  /**
   * 追蹤開始結帳流程
   */
  trackCheckoutStart(cartValue: number, itemCount: number): void {
    this.matomoTracker.trackEvent('Checkout', 'Start', 'Begin Checkout', cartValue, {
      cartValue: cartValue,
      itemCount: itemCount,
      timestamp: new Date().toISOString()
    });
  }

  /**
   * 追蹤結帳步驟
   */
  trackCheckoutStep(step: string, stepNumber: number, cartValue: number): void {
    this.matomoTracker.trackEvent('Checkout', 'Step', step, stepNumber, {
      step: step,
      stepNumber: stepNumber,
      cartValue: cartValue,
      timestamp: new Date().toISOString()
    });
  }

  /**
   * 追蹤訂單完成
   */
  trackOrderComplete(order: OrderTrackingData): void {
    // 追蹤電商轉換
    this.matomoTracker.trackEcommerceOrder(
      order.orderId,
      order.totalAmount,
      undefined, // 子總計
      undefined, // 稅額
      undefined, // 運費
      order.discountAmount || 0
    );

    // 追蹤訂單事件
    this.matomoTracker.trackEvent('Order', 'Complete', order.orderId, order.totalAmount, {
      orderId: order.orderId,
      totalAmount: order.totalAmount,
      discountAmount: order.discountAmount || 0,
      discountType: order.discountType || 'none',
      paymentMethod: order.paymentMethod,
      itemCount: order.items.length,
      timestamp: new Date().toISOString()
    });

    // 追蹤每個商品
    order.items.forEach(item => {
      this.matomoTracker.addEcommerceItem(
        item.productId,
        item.productName,
        item.category,
        item.price,
        item.quantity || 1
      );
    });
  }

  // === 搜尋和推薦追蹤 ===

  /**
   * 追蹤站內搜尋
   */
  trackSiteSearch(keyword: string, category?: string, resultCount?: number): void {
    this.matomoTracker.trackSiteSearch(keyword, category, resultCount);
    
    this.matomoTracker.trackEvent('Search', 'Query', keyword, resultCount, {
      keyword: keyword,
      category: category || 'all',
      resultCount: resultCount || 0,
      timestamp: new Date().toISOString()
    });
  }

  /**
   * 追蹤推薦商品點擊
   */
  trackRecommendationClick(productId: string, recommendationType: string, position: number): void {
    this.matomoTracker.trackEvent('Recommendation', 'Click', recommendationType, position, {
      productId: productId,
      recommendationType: recommendationType,
      position: position,
      timestamp: new Date().toISOString()
    });
  }

  // === 會員專屬功能追蹤 ===

  /**
   * 追蹤會員專屬商品瀏覽
   */
  trackExclusiveProductView(productId: string, membershipLevel: string, hasAccess: boolean): void {
    this.matomoTracker.trackEvent('Exclusive', 'Product View', productId, hasAccess ? 1 : 0, {
      productId: productId,
      membershipLevel: membershipLevel,
      hasAccess: hasAccess,
      timestamp: new Date().toISOString()
    });
  }

  /**
   * 追蹤會員推薦
   */
  trackMemberReferral(referrerId: string, refereeId?: string): void {
    this.matomoTracker.trackEvent('Referral', 'Member Invite', referrerId, undefined, {
      referrerId: referrerId,
      refereeId: refereeId || 'pending',
      timestamp: new Date().toISOString()
    });
  }

  /**
   * 追蹤推薦獎勵
   */
  trackReferralReward(referrerId: string, refereeId: string, rewardPoints: number): void {
    this.matomoTracker.trackEvent('Referral', 'Reward Earned', referrerId, rewardPoints, {
      referrerId: referrerId,
      refereeId: refereeId,
      rewardPoints: rewardPoints,
      timestamp: new Date().toISOString()
    });
  }

  // === 錯誤和異常追蹤 ===

  /**
   * 追蹤應用程式錯誤
   */
  trackError(errorType: string, errorMessage: string, page: string): void {
    this.matomoTracker.trackEvent('Error', errorType, errorMessage, undefined, {
      errorType: errorType,
      errorMessage: errorMessage,
      page: page,
      timestamp: new Date().toISOString()
    });
  }

  /**
   * 追蹤點數不足錯誤
   */
  trackInsufficientPointsError(attemptedPoints: number, availablePoints: number): void {
    this.matomoTracker.trackEvent('Error', 'Insufficient Points', 'Redemption Failed', attemptedPoints, {
      attemptedPoints: attemptedPoints,
      availablePoints: availablePoints,
      timestamp: new Date().toISOString()
    });
  }

  // === 頁面效能追蹤 ===

  /**
   * 追蹤頁面載入時間
   */
  trackPageLoadTime(page: string, loadTime: number): void {
    this.matomoTracker.trackEvent('Performance', 'Page Load', page, loadTime, {
      page: page,
      loadTime: loadTime,
      timestamp: new Date().toISOString()
    });
  }

  /**
   * 追蹤 API 回應時間
   */
  trackApiResponseTime(endpoint: string, responseTime: number, status: number): void {
    this.matomoTracker.trackEvent('Performance', 'API Response', endpoint, responseTime, {
      endpoint: endpoint,
      responseTime: responseTime,
      status: status,
      timestamp: new Date().toISOString()
    });
  }
}