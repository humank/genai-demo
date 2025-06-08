package solid.humank.genaidemo.domain.workflow.service;

import solid.humank.genaidemo.domain.common.annotations.DomainService;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationChannel;
import solid.humank.genaidemo.domain.notification.service.NotificationService;
import solid.humank.genaidemo.domain.workflow.model.aggregate.OrderWorkflow;
import solid.humank.genaidemo.domain.workflow.model.valueobject.WorkflowId;
import solid.humank.genaidemo.domain.workflow.model.valueobject.WorkflowStatus;
import solid.humank.genaidemo.domain.workflow.repository.OrderWorkflowRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 訂單工作流服務
 * 負責處理訂單工作流的創建、狀態轉換和完成
 */
@DomainService
public class OrderWorkflowService {
    private final OrderWorkflowRepository orderWorkflowRepository;
    private final NotificationService notificationService;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final DeliveryService deliveryService;

    public OrderWorkflowService(
            OrderWorkflowRepository orderWorkflowRepository,
            NotificationService notificationService,
            InventoryService inventoryService,
            PaymentService paymentService,
            DeliveryService deliveryService) {
        this.orderWorkflowRepository = orderWorkflowRepository;
        this.notificationService = notificationService;
        this.inventoryService = inventoryService;
        this.paymentService = paymentService;
        this.deliveryService = deliveryService;
    }

    /**
     * 創建訂單工作流
     *
     * @param orderId 訂單ID
     * @param productIds 產品ID列表
     * @return 工作流ID
     */
    public WorkflowId createOrderWorkflow(OrderId orderId, List<String> productIds) {
        OrderWorkflow workflow = new OrderWorkflow(orderId);
        
        // 添加產品
        for (String productId : productIds) {
            workflow.addProduct(productId);
        }
        
        // 保存工作流
        orderWorkflowRepository.save(workflow);
        
        return workflow.getId();
    }

    /**
     * 提交訂單
     *
     * @param workflowId 工作流ID
     * @return 是否提交成功
     */
    public boolean submitOrder(WorkflowId workflowId) {
        Optional<OrderWorkflow> optionalWorkflow = orderWorkflowRepository.findById(workflowId);
        if (optionalWorkflow.isEmpty()) {
            return false;
        }
        
        OrderWorkflow workflow = optionalWorkflow.get();
        
        try {
            workflow.submitOrder();
            orderWorkflowRepository.save(workflow);
            return true;
        } catch (IllegalStateException e) {
            // 處理提交失敗
            return false;
        }
    }

    /**
     * 驗證訂單
     *
     * @param workflowId 工作流ID
     * @return 訂單是否有效
     */
    public boolean validateOrder(WorkflowId workflowId) {
        Optional<OrderWorkflow> optionalWorkflow = orderWorkflowRepository.findById(workflowId);
        if (optionalWorkflow.isEmpty()) {
            return false;
        }
        
        OrderWorkflow workflow = optionalWorkflow.get();
        return workflow.validateOrder();
    }

    /**
     * 檢查庫存
     *
     * @param workflowId 工作流ID
     * @return 庫存是否充足
     */
    public boolean checkInventory(WorkflowId workflowId) {
        Optional<OrderWorkflow> optionalWorkflow = orderWorkflowRepository.findById(workflowId);
        if (optionalWorkflow.isEmpty()) {
            return false;
        }
        
        OrderWorkflow workflow = optionalWorkflow.get();
        
        // 調用庫存服務檢查庫存
        boolean isSufficient = inventoryService.checkInventory(workflow.getOrderId(), workflow.getProductIds());
        
        // 更新工作流狀態
        workflow.checkInventory(isSufficient);
        orderWorkflowRepository.save(workflow);
        
        // 如果庫存不足，發送通知
        if (!isSufficient) {
            sendInsufficientInventoryNotification(workflow);
        }
        
        return isSufficient;
    }

    /**
     * 處理支付
     *
     * @param workflowId 工作流ID
     * @param paymentMethod 支付方式
     * @param paymentDetails 支付詳情
     * @return 支付是否成功
     */
    public boolean processPayment(WorkflowId workflowId, String paymentMethod, Map<String, Object> paymentDetails) {
        Optional<OrderWorkflow> optionalWorkflow = orderWorkflowRepository.findById(workflowId);
        if (optionalWorkflow.isEmpty()) {
            return false;
        }
        
        OrderWorkflow workflow = optionalWorkflow.get();
        
        // 設置支付方式
        workflow.setPaymentMethod(paymentMethod);
        
        // 調用支付服務處理支付
        boolean isSuccessful = paymentService.processPayment(workflow.getOrderId(), paymentMethod, paymentDetails);
        
        // 更新工作流狀態
        workflow.processPayment(isSuccessful);
        orderWorkflowRepository.save(workflow);
        
        // 如果支付成功，發送訂單確認通知
        if (isSuccessful) {
            sendOrderConfirmationNotification(workflow);
        } else {
            // 如果支付失敗，發送支付失敗通知
            sendPaymentFailureNotification(workflow, (String) paymentDetails.get("failureReason"));
        }
        
        return isSuccessful;
    }

    /**
     * 安排配送
     *
     * @param workflowId 工作流ID
     * @return 是否安排成功
     */
    public boolean arrangeDelivery(WorkflowId workflowId) {
        Optional<OrderWorkflow> optionalWorkflow = orderWorkflowRepository.findById(workflowId);
        if (optionalWorkflow.isEmpty()) {
            return false;
        }
        
        OrderWorkflow workflow = optionalWorkflow.get();
        
        try {
            workflow.arrangeDelivery();
            orderWorkflowRepository.save(workflow);
            
            // 調用配送服務安排配送
            deliveryService.arrangeDelivery(workflow.getOrderId());
            
            // 發送配送狀態更新通知
            sendDeliveryStatusUpdateNotification(workflow);
            
            return true;
        } catch (IllegalStateException e) {
            // 處理安排配送失敗
            return false;
        }
    }

    /**
     * 完成訂單
     *
     * @param workflowId 工作流ID
     * @return 是否完成成功
     */
    public boolean completeOrder(WorkflowId workflowId) {
        Optional<OrderWorkflow> optionalWorkflow = orderWorkflowRepository.findById(workflowId);
        if (optionalWorkflow.isEmpty()) {
            return false;
        }
        
        OrderWorkflow workflow = optionalWorkflow.get();
        
        try {
            workflow.completeOrder();
            orderWorkflowRepository.save(workflow);
            
            // 發送訂單完成通知
            sendOrderCompletionNotification(workflow);
            
            return true;
        } catch (IllegalStateException e) {
            // 處理完成訂單失敗
            return false;
        }
    }

    /**
     * 取消訂單
     *
     * @param workflowId 工作流ID
     * @param reason 取消原因
     * @return 是否取消成功
     */
    public boolean cancelOrder(WorkflowId workflowId, String reason) {
        Optional<OrderWorkflow> optionalWorkflow = orderWorkflowRepository.findById(workflowId);
        if (optionalWorkflow.isEmpty()) {
            return false;
        }
        
        OrderWorkflow workflow = optionalWorkflow.get();
        
        try {
            workflow.cancelOrder(reason);
            orderWorkflowRepository.save(workflow);
            
            // 如果已經預留了庫存，釋放庫存
            if (workflow.isInventoryChecked() && workflow.isInventorySufficient()) {
                inventoryService.releaseInventory(workflow.getOrderId());
            }
            
            // 發送訂單取消通知
            sendOrderCancellationNotification(workflow);
            
            return true;
        } catch (IllegalStateException e) {
            // 處理取消訂單失敗
            return false;
        }
    }

    /**
     * 獲取工作流狀態
     *
     * @param workflowId 工作流ID
     * @return 工作流狀態
     */
    public WorkflowStatus getWorkflowStatus(WorkflowId workflowId) {
        Optional<OrderWorkflow> optionalWorkflow = orderWorkflowRepository.findById(workflowId);
        return optionalWorkflow.map(OrderWorkflow::getStatus).orElse(null);
    }

    /**
     * 獲取取消原因
     *
     * @param workflowId 工作流ID
     * @return 取消原因
     */
    public String getCancellationReason(WorkflowId workflowId) {
        Optional<OrderWorkflow> optionalWorkflow = orderWorkflowRepository.findById(workflowId);
        return optionalWorkflow.map(OrderWorkflow::getCancellationReason).orElse(null);
    }

    /**
     * 發送訂單確認通知
     *
     * @param workflow 訂單工作流
     */
    private void sendOrderConfirmationNotification(OrderWorkflow workflow) {
        // 獲取客戶ID
        String customerId = getCustomerId(workflow.getOrderId());
        
        // 獲取訂單詳情
        Map<String, Object> orderDetails = getOrderDetails(workflow.getOrderId());
        
        // 獲取預計配送時間
        LocalDateTime estimatedDeliveryTime = LocalDateTime.now().plusDays(3);
        
        // 獲取客戶偏好的通知渠道
        List<NotificationChannel> channels = getCustomerPreferredChannels(customerId);
        
        // 發送通知
        notificationService.sendOrderConfirmationNotification(
                customerId,
                workflow.getOrderId(),
                orderDetails,
                estimatedDeliveryTime,
                channels
        );
    }

    /**
     * 發送支付失敗通知
     *
     * @param workflow 訂單工作流
     * @param failureReason 失敗原因
     */
    private void sendPaymentFailureNotification(OrderWorkflow workflow, String failureReason) {
        // 獲取客戶ID
        String customerId = getCustomerId(workflow.getOrderId());
        
        // 生成重新支付鏈接
        String retryPaymentLink = generateRetryPaymentLink(workflow.getOrderId());
        
        // 獲取客戶偏好的通知渠道
        List<NotificationChannel> channels = getCustomerPreferredChannels(customerId);
        
        // 發送通知
        notificationService.sendPaymentFailureNotification(
                customerId,
                workflow.getOrderId(),
                failureReason,
                retryPaymentLink,
                channels
        );
    }

    /**
     * 發送庫存不足通知
     *
     * @param workflow 訂單工作流
     */
    private void sendInsufficientInventoryNotification(OrderWorkflow workflow) {
        // 獲取客戶ID
        String customerId = getCustomerId(workflow.getOrderId());
        
        // 獲取庫存不足的商品
        List<String> outOfStockProducts = inventoryService.getOutOfStockProducts(workflow.getOrderId());
        
        // 獲取替代商品建議
        Map<String, String> alternativeProducts = inventoryService.getAlternativeProducts(outOfStockProducts);
        
        // 獲取客戶偏好的通知渠道
        List<NotificationChannel> channels = getCustomerPreferredChannels(customerId);
        
        // 發送通知
        notificationService.sendInsufficientInventoryNotification(
                customerId,
                workflow.getOrderId(),
                outOfStockProducts,
                alternativeProducts,
                channels
        );
    }

    /**
     * 發送配送狀態更新通知
     *
     * @param workflow 訂單工作流
     */
    private void sendDeliveryStatusUpdateNotification(OrderWorkflow workflow) {
        // 獲取客戶ID
        String customerId = getCustomerId(workflow.getOrderId());
        
        // 獲取配送狀態
        String deliveryStatus = "IN_TRANSIT";
        
        // 獲取預計配送時間
        LocalDateTime estimatedDeliveryTime = LocalDateTime.now().plusDays(3);
        
        // 生成配送追蹤鏈接
        String trackingLink = generateTrackingLink(workflow.getOrderId());
        
        // 獲取客戶偏好的通知渠道
        List<NotificationChannel> channels = getCustomerPreferredChannels(customerId);
        
        // 發送通知
        notificationService.sendDeliveryStatusUpdateNotification(
                customerId,
                workflow.getOrderId(),
                deliveryStatus,
                estimatedDeliveryTime,
                trackingLink,
                channels
        );
    }

    /**
     * 發送訂單完成通知
     *
     * @param workflow 訂單工作流
     */
    private void sendOrderCompletionNotification(OrderWorkflow workflow) {
        // 獲取客戶ID
        String customerId = getCustomerId(workflow.getOrderId());
        
        // 生成評價鏈接
        String ratingLink = generateRatingLink(workflow.getOrderId());
        
        // 獲取相關商品推薦
        List<String> recommendations = getProductRecommendations(workflow.getProductIds());
        
        // 獲取客戶偏好的通知渠道
        List<NotificationChannel> channels = getCustomerPreferredChannels(customerId);
        
        // 發送通知
        notificationService.sendOrderCompletionNotification(
                customerId,
                workflow.getOrderId(),
                ratingLink,
                recommendations,
                channels
        );
    }

    /**
     * 發送訂單取消通知
     *
     * @param workflow 訂單工作流
     */
    private void sendOrderCancellationNotification(OrderWorkflow workflow) {
        // 獲取客戶ID
        String customerId = getCustomerId(workflow.getOrderId());
        
        // 獲取取消原因
        String cancellationReason = workflow.getCancellationReason();
        
        // 獲取客戶偏好的通知渠道
        List<NotificationChannel> channels = getCustomerPreferredChannels(customerId);
        
        // 發送通知
        notificationService.sendOrderCancellationNotification(
                customerId,
                workflow.getOrderId(),
                cancellationReason,
                channels
        );
    }

    // 以下是輔助方法，在實際應用中需要實現
    
    private String getCustomerId(OrderId orderId) {
        // 在實際應用中，這裡會查詢訂單獲取客戶ID
        return "customer-123";
    }
    
    private Map<String, Object> getOrderDetails(OrderId orderId) {
        // 在實際應用中，這裡會查詢訂單獲取詳情
        Map<String, Object> details = new HashMap<>();
        details.put("訂單ID", orderId.toString());
        details.put("訂單金額", "100.00");
        details.put("商品數量", "2");
        return details;
    }
    
    private List<NotificationChannel> getCustomerPreferredChannels(String customerId) {
        // 在實際應用中，這裡會查詢客戶偏好
        return List.of(NotificationChannel.EMAIL, NotificationChannel.SMS);
    }
    
    private String generateRetryPaymentLink(OrderId orderId) {
        // 在實際應用中，這裡會生成重新支付鏈接
        return "https://example.com/payment/retry?orderId=" + orderId.toString();
    }
    
    private String generateTrackingLink(OrderId orderId) {
        // 在實際應用中，這裡會生成配送追蹤鏈接
        return "https://example.com/delivery/track?orderId=" + orderId.toString();
    }
    
    private String generateRatingLink(OrderId orderId) {
        // 在實際應用中，這裡會生成評價鏈接
        return "https://example.com/order/rate?orderId=" + orderId.toString();
    }
    
    private List<String> getProductRecommendations(List<String> productIds) {
        // 在實際應用中，這裡會根據購買的商品生成推薦
        return List.of("推薦商品1", "推薦商品2", "推薦商品3");
    }
}