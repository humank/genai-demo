package solid.humank.genaidemo.infrastructure.order.persistence.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import solid.humank.genaidemo.infrastructure.order.persistence.entity.JpaOrderEntity;

/** JPA 訂單儲存庫 用於與資料庫交互的 Spring Data JPA 儲存庫 */
@Repository
public interface JpaOrderRepository extends JpaRepository<JpaOrderEntity, String> {

    /**
     * 根據客戶ID查詢訂單
     *
     * @param customerId 客戶ID
     * @return 訂單列表
     */
    List<JpaOrderEntity> findByCustomerId(String customerId);

    /**
     * 根據訂單ID查詢訂單
     *
     * @param id 訂單ID
     * @return 訂單
     */
    @Override
    @org.springframework.lang.NonNull
    Optional<JpaOrderEntity> findById(@org.springframework.lang.NonNull String id);

    // ========== 統計查詢方法 ==========

    /** 統計所有訂單項目數量 */
    @Query("SELECT COUNT(oi) FROM JpaOrderEntity o JOIN o.items oi")
    long countAllOrderItems();

    /** 根據狀態統計訂單總金額 */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM JpaOrderEntity o WHERE o.status = :status")
    BigDecimal sumTotalAmountByStatus(
            @Param("status") solid.humank.genaidemo.domain.common.valueobject.OrderStatus status);

    /** 統計不重複客戶數量 */
    @Query("SELECT COUNT(DISTINCT o.customerId) FROM JpaOrderEntity o")
    long countDistinctCustomers();

    /** 按狀態分組統計訂單數量 */
    @Query("SELECT o.status, COUNT(o) FROM JpaOrderEntity o GROUP BY o.status")
    List<Object[]> countByStatusGrouped();

    /** 根據客戶ID列表查詢訂單 */
    @Query("SELECT DISTINCT o.customerId FROM JpaOrderEntity o ORDER BY o.customerId")
    List<String> findDistinctCustomerIds();

    /** 檢查客戶是否存在訂單 */
    boolean existsByCustomerId(String customerId);

    /** 統計指定客戶的訂單數量 */
    long countByCustomerId(String customerId);
}
