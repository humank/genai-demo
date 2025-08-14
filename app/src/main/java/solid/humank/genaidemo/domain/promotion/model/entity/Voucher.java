package solid.humank.genaidemo.domain.promotion.model.entity;

import java.time.LocalDate;
import java.util.UUID;
import solid.humank.genaidemo.domain.common.annotations.Entity;
import solid.humank.genaidemo.domain.common.valueobject.Money;

/** 優惠券實體 */
@Entity
public class Voucher {
    private final String id;
    private final String name;
    private final Money value;
    private final String redemptionCode;
    private final LocalDate issueDate;
    private final LocalDate expirationDate;
    private final String redemptionLocation;
    private final String contents;
    private boolean isUsed;
    private boolean isInvalidated;

    public Voucher(
            String name, Money value, int validDays, String redemptionLocation, String contents) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.value = value;
        this.redemptionCode = generateRedemptionCode();
        this.issueDate = LocalDate.now();
        this.expirationDate = issueDate.plusDays(validDays);
        this.redemptionLocation = redemptionLocation;
        this.contents = contents;
        this.isUsed = false;
        this.isInvalidated = false;
    }

    private String generateRedemptionCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Money getValue() {
        return value;
    }

    public String getRedemptionCode() {
        return redemptionCode;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public int getValidDays() {
        return (int) (expirationDate.toEpochDay() - issueDate.toEpochDay());
    }

    public String getRedemptionLocation() {
        return redemptionLocation;
    }

    public String getContents() {
        return contents;
    }

    public boolean isUsed() {
        return isUsed;
    }

    public boolean isInvalidated() {
        return isInvalidated;
    }

    public void use() {
        if (isUsed || isInvalidated) {
            throw new IllegalStateException("Voucher is already used or invalidated");
        }
        this.isUsed = true;
    }

    public void invalidate() {
        if (isUsed) {
            throw new IllegalStateException("Cannot invalidate a used voucher");
        }
        this.isInvalidated = true;
    }

    public boolean isValid() {
        return !isUsed && !isInvalidated && LocalDate.now().isBefore(expirationDate);
    }
}
