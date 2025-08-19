package solid.humank.genaidemo.infrastructure.order.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import solid.humank.genaidemo.infrastructure.order.persistence.entity.JpaOrderWorkflowEntity;

/**
 * 訂單工作流JPA儲存庫
 */
@Repository
public interface JpaOrderWorkflowRepository extends JpaRepository<JpaOrderWorkflowEntity, String> {

    /**
     * 根據訂單ID查詢訂單工作流
     */
    Optional<JpaOrderWorkflowEntity> findByOrderId(String orderId);

    /**
     * 根據狀態查詢訂單工作流列表
     */
    List<JpaOrderWorkflowEntity> findByStatus(String status);

    /**
     * 查詢指定時間之後創建的訂單工作流列表
     */
    List<JpaOrderWorkflowEntity> findByCreatedAtAfter(LocalDateTime time);

    /**
     * 查詢指定時間之後更新的訂單工作流列表
     */
    List<JpaOrderWorkflowEntity> findByUpdatedAtAfter(LocalDateTime time);

    /**
     * 查詢取消的訂單工作流列表
     */
    @Query("SELECT w FROM JpaOrderWorkflowEntity w WHERE w.status = 'CANCELLED' AND (:reason IS NULL OR w.cancellationReason LIKE %:reason%)")
    List<JpaOrderWorkflowEntity> findCancelledWorkflows(@Param("reason") String reason);
}