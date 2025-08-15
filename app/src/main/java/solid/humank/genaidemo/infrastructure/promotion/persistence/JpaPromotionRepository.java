package solid.humank.genaidemo.infrastructure.promotion.persistence;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import solid.humank.genaidemo.infrastructure.entity.JpaPromotionEntity;

/** 促銷 JPA Repository */
@Repository
public interface JpaPromotionRepository extends JpaRepository<JpaPromotionEntity, String> {

    /** 根據類型查找促銷 */
    List<JpaPromotionEntity> findByType(String type);

    /** 根據狀態查找促銷 */
    List<JpaPromotionEntity> findByStatus(String status);

    /** 查找活躍的促銷 */
    @Query(
            "SELECT p FROM JpaPromotionEntity p WHERE p.status = 'ACTIVE' AND p.startDate <= :now"
                    + " AND p.endDate >= :now")
    List<JpaPromotionEntity> findActivePromotions(@Param("now") LocalDateTime now);

    /** 根據類型和狀態查找促銷 */
    List<JpaPromotionEntity> findByTypeAndStatus(String type, String status);
}
