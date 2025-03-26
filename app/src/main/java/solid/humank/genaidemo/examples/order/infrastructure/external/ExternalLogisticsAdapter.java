package solid.humank.genaidemo.examples.order.infrastructure.external;

import org.springframework.stereotype.Service;
import solid.humank.genaidemo.examples.order.OrderId;
import solid.humank.genaidemo.examples.order.acl.DeliveryOrder;
import solid.humank.genaidemo.examples.order.acl.DeliveryStatus;
import solid.humank.genaidemo.examples.order.application.port.outgoing.LogisticsServicePort;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 外部物流服務適配器
 * 實現 LogisticsServicePort 接口
 * 注意：這是一個簡化的模擬實現，實際應用中會與真實的物流系統整合
 */
@Service
public class ExternalLogisticsAdapter implements LogisticsServicePort {

    // 模擬物流訂單存儲
    private final Map<String, DeliveryOrder> deliveryOrders = new ConcurrentHashMap<>();
    
    // 訂單操作鎖映射，提供訂單級別的同步
    private final Map<String, Lock> orderLocks = new ConcurrentHashMap<>();

    /**
     * 獲取訂單的鎖，如果不存在則創建新的鎖
     */
    private Lock getOrderLock(String orderIdStr) {
        return orderLocks.computeIfAbsent(orderIdStr, k -> new ReentrantLock());
    }

    @Override
    public DeliveryOrder createDeliveryOrder(OrderId orderId) {
        String orderIdStr = orderId.toString();
        Lock lock = getOrderLock(orderIdStr);
        lock.lock();
        try {
            // 創建新的物流訂單
            DeliveryOrder deliveryOrder = new DeliveryOrder(orderId, DeliveryStatus.CREATED);
            
            // 保存到模擬存儲
            deliveryOrders.put(orderIdStr, deliveryOrder);
            
            return deliveryOrder;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean cancelDeliveryOrder(OrderId orderId) {
        String orderIdStr = orderId.toString();
        Lock lock = getOrderLock(orderIdStr);
        lock.lock();
        try {
            // 檢查訂單是否存在
            if (!deliveryOrders.containsKey(orderIdStr)) {
                return false;
            }
            
            // 移除訂單
            deliveryOrders.remove(orderIdStr);
            orderLocks.remove(orderIdStr);  // 移除對應的鎖
            
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public DeliveryStatus getDeliveryStatus(OrderId orderId) {
        String orderIdStr = orderId.toString();
        Lock lock = getOrderLock(orderIdStr);
        lock.lock();
        try {
            // 獲取物流訂單
            DeliveryOrder deliveryOrder = deliveryOrders.get(orderIdStr);
            
            // 如果不存在，返回未知狀態
            if (deliveryOrder == null) {
                return DeliveryStatus.UNKNOWN;
            }
            
            return deliveryOrder.status();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean updateDeliveryAddress(OrderId orderId, String address) {
        String orderIdStr = orderId.toString();
        Lock lock = getOrderLock(orderIdStr);
        lock.lock();
        try {
            // 實際應用中，這會更新物流系統中的地址
            // 在這個簡化實現中，我們只檢查訂單是否存在
            return deliveryOrders.containsKey(orderIdStr);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean confirmDelivery(OrderId orderId) {
        String orderIdStr = orderId.toString();
        Lock lock = getOrderLock(orderIdStr);
        lock.lock();
        try {
            // 獲取物流訂單
            DeliveryOrder deliveryOrder = deliveryOrders.get(orderIdStr);
            
            // 如果不存在或不是運送中狀態，無法確認送達
            if (deliveryOrder == null || deliveryOrder.status() != DeliveryStatus.IN_TRANSIT) {
                return false;
            }
            
            // 更新為已送達狀態
            DeliveryOrder updatedOrder = new DeliveryOrder(orderId, DeliveryStatus.DELIVERED);
            deliveryOrders.put(orderIdStr, updatedOrder);
            
            return true;
        } finally {
            lock.unlock();
        }
    }

    // 模擬物流狀態變更的方法（在實際應用中，這些可能由外部系統觸發）
    
    /**
     * 模擬開始運送
     */
    public boolean startShipping(OrderId orderId) {
        String orderIdStr = orderId.toString();
        Lock lock = getOrderLock(orderIdStr);
        lock.lock();
        try {
            DeliveryOrder deliveryOrder = deliveryOrders.get(orderIdStr);
            
            if (deliveryOrder == null || deliveryOrder.status() != DeliveryStatus.CREATED) {
                return false;
            }
            
            DeliveryOrder updatedOrder = new DeliveryOrder(orderId, DeliveryStatus.IN_TRANSIT);
            deliveryOrders.put(orderIdStr, updatedOrder);
            
            return true;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 模擬配送失敗
     */
    public boolean failDelivery(OrderId orderId, String reason) {
        String orderIdStr = orderId.toString();
        Lock lock = getOrderLock(orderIdStr);
        lock.lock();
        try {
            DeliveryOrder deliveryOrder = deliveryOrders.get(orderIdStr);
            
            if (deliveryOrder == null) {
                return false;
            }
            
            DeliveryOrder updatedOrder = new DeliveryOrder(orderId, DeliveryStatus.FAILED);
            deliveryOrders.put(orderIdStr, updatedOrder);
            
            // 實際應用中，這里可能還需要記錄失敗原因
            
            return true;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 清理資源方法
     * 在實際應用中，這可以作為系統關閉時的清理操作
     */
    public void cleanup() {
        deliveryOrders.clear();
        orderLocks.clear();
    }
}
