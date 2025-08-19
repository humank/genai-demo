package solid.humank.genaidemo.infrastructure.customer.persistence.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import solid.humank.genaidemo.infrastructure.customer.persistence.entity.JpaCustomerEntity;

/**
 * 客戶 JPA 儲存庫介面
 * 提供客戶實體的資料庫操作方法
 */
@Repository
public interface JpaCustomerRepository extends JpaRepository<JpaCustomerEntity, String> {

    /**
     * 根據電子郵件查找客戶
     */
    Optional<JpaCustomerEntity> findByEmail(String email);

    /**
     * 根據會員等級查找客戶
     */
    List<JpaCustomerEntity> findByMembershipLevel(String membershipLevel);

    /**
     * 根據生日月份查找客戶
     */
    @Query("SELECT c FROM JpaCustomerEntity c WHERE MONTH(c.birthDate) = :month")
    List<JpaCustomerEntity> findByBirthMonth(@Param("month") int month);

    /**
     * 查找指定日期之後註冊的新會員
     */
    @Query("SELECT c FROM JpaCustomerEntity c WHERE c.registrationDate >= :since")
    List<JpaCustomerEntity> findNewMembersSince(@Param("since") LocalDate since);

    /**
     * 分頁查詢客戶ID
     */
    @Query(value = "SELECT c.id FROM JpaCustomerEntity c ORDER BY c.registrationDate DESC")
    List<String> findCustomerIds(int offset, int limit);

    /**
     * 計算客戶總數
     */
    @Query("SELECT COUNT(c) FROM JpaCustomerEntity c")
    long countCustomers();
}