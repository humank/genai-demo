package solid.humank.genaidemo.domain.customer.service;

import solid.humank.genaidemo.domain.common.annotations.DomainService;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.customer.model.aggregate.Customer;

/** 紅利點數服務 */
@DomainService(description = "紅利點數服務，處理客戶點數的兌換和累積邏輯")
public class RewardPointsService {

    private static final int POINTS_REDEMPTION_RATE = 10; // 10點 = $1

    /** 獲取點數兌換率 */
    public int getPointsRedemptionRate() {
        return POINTS_REDEMPTION_RATE;
    }

    /** 計算點數可兌換的金額 */
    public Money calculateRedemptionAmount(int points) {
        return Money.twd(points / POINTS_REDEMPTION_RATE);
    }

    /**
     * 兌換點數
     *
     * @return 是否兌換成功
     */
    public boolean redeemPoints(Customer customer, int points) {
        return customer.useRewardPoints(points);
    }

    /** 計算訂單可獲得的點數 */
    public int calculateEarnedPoints(Money orderAmount) {
        // 例如：每消費100元獲得1點
        return orderAmount.getAmount().intValue() / 100;
    }

    /** 添加點數到客戶帳戶 */
    public void addPointsToCustomer(Customer customer, int points) {
        customer.addRewardPoints(points, "Service reward");
    }
}
