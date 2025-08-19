package solid.humank.genaidemo.domain.promotion.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import solid.humank.genaidemo.domain.promotion.model.aggregate.Voucher;

/** 優惠券倉儲接口 */
@solid.humank.genaidemo.domain.common.annotations.Repository(name = "VoucherRepository", description = "優惠券聚合根儲存庫")
public interface VoucherRepository
        extends solid.humank.genaidemo.domain.common.repository.BaseRepository<Voucher, String> {

    /**
     * 根據ID查找優惠券
     *
     * @param voucherId 優惠券ID
     * @return 優惠券，如果不存在則返回空
     */
    @Override
    Optional<Voucher> findById(String voucherId);

    /**
     * 根據兌換碼查找優惠券
     *
     * @param redemptionCode 兌換碼
     * @return 優惠券，如果不存在則返回空
     */
    Optional<Voucher> findByRedemptionCode(String redemptionCode);

    /**
     * 查找客戶的所有優惠券
     *
     * @param customerId 客戶ID
     * @return 優惠券列表
     */
    List<Voucher> findByCustomerId(String customerId);

    /**
     * 查找有效的優惠券
     *
     * @param expirationDate 到期日期
     * @return 有效的優惠券列表
     */
    List<Voucher> findValidVouchers(LocalDateTime expirationDate);

    /**
     * 保存優惠券
     *
     * @param voucher 優惠券
     * @return 保存後的優惠券
     */
    @Override
    Voucher save(Voucher voucher);

    /**
     * 刪除優惠券
     *
     * @param voucherId 優惠券ID
     */
    void delete(String voucherId);
}
