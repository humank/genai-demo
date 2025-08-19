package solid.humank.genaidemo.domain.payment.repository;

import java.util.List;
import java.util.Optional;

import solid.humank.genaidemo.domain.payment.model.aggregate.PaymentMethod;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;

/** 支付方式儲存庫接口 */
@solid.humank.genaidemo.domain.common.annotations.Repository(name = "PaymentMethodRepository", description = "支付方式聚合根儲存庫")
public interface PaymentMethodRepository
        extends solid.humank.genaidemo.domain.common.repository.BaseRepository<PaymentMethod, String> {

    /**
     * 根據客戶ID查詢支付方式
     * 
     * @param customerId 客戶ID
     * @return 支付方式列表
     */
    List<PaymentMethod> findByCustomerId(CustomerId customerId);

    /**
     * 根據客戶ID查詢預設支付方式
     * 
     * @param customerId 客戶ID
     * @return 預設支付方式（如果存在）
     */
    Optional<PaymentMethod> findDefaultByCustomerId(CustomerId customerId);

    /**
     * 根據客戶ID和類型查詢支付方式
     * 
     * @param customerId 客戶ID
     * @param type       支付方式類型
     * @return 支付方式列表
     */
    List<PaymentMethod> findByCustomerIdAndType(CustomerId customerId, String type);

    /**
     * 根據客戶ID查詢活躍的支付方式
     * 
     * @param customerId 客戶ID
     * @return 活躍支付方式列表
     */
    List<PaymentMethod> findActiveByCustomerId(CustomerId customerId);

    /**
     * 根據提供商查詢支付方式
     * 
     * @param provider 支付提供商
     * @return 支付方式列表
     */
    List<PaymentMethod> findByProvider(String provider);
}