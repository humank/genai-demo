package solid.humank.genaidemo.domain.promotion.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import solid.humank.genaidemo.domain.common.annotations.Repository;
import solid.humank.genaidemo.domain.common.repository.BaseRepository;
import solid.humank.genaidemo.domain.promotion.model.aggregate.Promotion;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionId;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionType;

/** 促銷倉儲接口 */
@Repository(name = "PromotionRepository", description = "促銷聚合根儲存庫")
public interface PromotionRepository extends BaseRepository<Promotion, PromotionId> {

    /**
     * 根據ID查找促銷
     *
     * @param promotionId 促銷ID
     * @return 促銷，如果不存在則返回空
     */
    @Override
    Optional<Promotion> findById(PromotionId promotionId);

    /**
     * 根據類型查找促銷
     *
     * @param type 促銷類型
     * @return 促銷列表
     */
    List<Promotion> findByType(PromotionType type);

    /**
     * 查找活躍的促銷
     *
     * @return 活躍的促銷列表
     */
    List<Promotion> findActivePromotions();

    /**
     * 查找在指定時間有效的促銷
     *
     * @param dateTime 時間
     * @return 有效的促銷列表
     */
    List<Promotion> findPromotionsValidAt(LocalDateTime dateTime);

    /**
     * 保存促銷
     *
     * @param promotion 促銷
     * @return 保存後的促銷
     */
    @Override
    Promotion save(Promotion promotion);

    /**
     * 刪除促銷
     *
     * @param promotionId 促銷ID
     */
    void delete(PromotionId promotionId);
}
