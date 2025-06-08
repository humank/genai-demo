package solid.humank.genaidemo.domain.seller.model.aggregate;

import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.seller.model.valueobject.SellerId;

/**
 * 賣家聚合根
 */
@AggregateRoot
public class Seller {
    private SellerId sellerId;
    private String name;
    private String email;
    private String phone;
    private boolean isActive;

    // Private constructor for JPA
    private Seller() {
    }

    public Seller(SellerId sellerId, String name, String email, String phone) {
        this.sellerId = sellerId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.isActive = true;
    }

    public SellerId getSellerId() {
        return sellerId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public boolean isActive() {
        return isActive;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void updateContactInfo(String email, String phone) {
        this.email = email;
        this.phone = phone;
    }
}