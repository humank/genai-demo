package solid.humank.genaidemo.domain.workflow.repository;

import solid.humank.genaidemo.domain.common.repository.Repository;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.workflow.model.aggregate.OrderWorkflow;
import solid.humank.genaidemo.domain.workflow.model.valueobject.WorkflowId;
import solid.humank.genaidemo.domain.workflow.model.valueobject.WorkflowStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 訂單工作流儲存庫接口
 */
public interface OrderWorkflowRepository extends Repository<OrderWorkflow, WorkflowId> {
    
    /**
     * 保存訂單工作流
     * 
     * @param orderWorkflow 訂單工作流
     * @return 保存後的訂單工作流
     */
    @Override
    OrderWorkflow save(OrderWorkflow orderWorkflow);
    
    /**
     * 根據ID查詢訂單工作流
     * 
     * @param id 工作流ID
     * @return 訂單工作流
     */
    Optional<OrderWorkflow> findById(WorkflowId id);
    
    /**
     * 根據訂單ID查詢訂單工作流
     * 
     * @param orderId 訂單ID
     * @return 訂單工作流
     */
    Optional<OrderWorkflow> findByOrderId(OrderId orderId);
    
    /**
     * 根據狀態查詢訂單工作流列表
     * 
     * @param status 工作流狀態
     * @return 訂單工作流列表
     */
    List<OrderWorkflow> findByStatus(WorkflowStatus status);
    
    /**
     * 查詢指定時間之後創建的訂單工作流列表
     * 
     * @param time 時間
     * @return 訂單工作流列表
     */
    List<OrderWorkflow> findByCreatedAtAfter(LocalDateTime time);
    
    /**
     * 查詢指定時間之後更新的訂單工作流列表
     * 
     * @param time 時間
     * @return 訂單工作流列表
     */
    List<OrderWorkflow> findByUpdatedAtAfter(LocalDateTime time);
    
    /**
     * 查詢取消的訂單工作流列表
     * 
     * @param reason 取消原因
     * @return 訂單工作流列表
     */
    List<OrderWorkflow> findCancelledWorkflows(String reason);
}