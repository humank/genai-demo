package solid.humank.genaidemo.domain.payment.model.aggregate;

import java.time.LocalDateTime;
import java.util.Objects;

import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;

/** 支付方式聚合根 */
@AggregateRoot(name = "PaymentMethod", description = "支付方式聚合根，管理客戶的支付方式配置", boundedContext = "Payment", version = "1.0")
public class PaymentMethod extends solid.humank.genaidemo.domain.common.aggregate.AggregateRoot {

    private final String paymentMethodId;
    private final CustomerId customerId;
    private final String type; // CREDIT_CARD, BANK_TRANSFER, DIGITAL_WALLET, etc.
    private String displayName;
    private String provider; // Visa, MasterCard, PayPal, etc.
    private String maskedNumber; // **** **** **** 1234
    private String expiryDate; // MM/YY for cards
    private boolean isDefault;
    private boolean isActive;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PaymentMethod(String paymentMethodId, CustomerId customerId, String type,
            String displayName, String provider, String maskedNumber) {
        this.paymentMethodId = Objects.requireNonNull(paymentMethodId, "支付方式ID不能為空");
        this.customerId = Objects.requireNonNull(customerId, "客戶ID不能為空");
        this.type = Objects.requireNonNull(type, "支付方式類型不能為空");
        this.displayName = Objects.requireNonNull(displayName, "顯示名稱不能為空");
        this.provider = provider;
        this.maskedNumber = maskedNumber;
        this.isDefault = false;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    // Business methods
    public void setAsDefault() {
        this.isDefault = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void unsetAsDefault() {
        this.isDefault = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.isActive = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.isActive = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateDisplayName(String displayName) {
        this.displayName = Objects.requireNonNull(displayName, "顯示名稱不能為空");
        this.updatedAt = LocalDateTime.now();
    }

    public void updateExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    public String getPaymentMethodId() {
        return paymentMethodId;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public String getType() {
        return type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getProvider() {
        return provider;
    }

    public String getMaskedNumber() {
        return maskedNumber;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean isActive() {
        return isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PaymentMethod that = (PaymentMethod) o;
        return Objects.equals(paymentMethodId, that.paymentMethodId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paymentMethodId);
    }
}