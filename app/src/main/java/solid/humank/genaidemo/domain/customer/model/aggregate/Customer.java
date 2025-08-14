package solid.humank.genaidemo.domain.customer.model.aggregate;

import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerId;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerName;
import solid.humank.genaidemo.domain.customer.model.valueobject.Email;
import solid.humank.genaidemo.domain.customer.model.valueobject.Phone;
import solid.humank.genaidemo.domain.customer.model.valueobject.Address;
import solid.humank.genaidemo.domain.customer.model.valueobject.MembershipLevel;

/**
 * 客戶聚合根
 */
@AggregateRoot(name = "Customer", description = "客戶聚合根，管理客戶的基本信息和會員等級")
public class Customer {
    
    private final CustomerId id;
    private final CustomerName name;
    private final Email email;
    private final Phone phone;
    private final Address address;
    private final MembershipLevel membershipLevel;
    
    public Customer(CustomerId id, CustomerName name, Email email, 
                   Phone phone, Address address, MembershipLevel membershipLevel) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.membershipLevel = membershipLevel;
    }
    
    public CustomerId getId() {
        return id;
    }
    
    public CustomerName getName() {
        return name;
    }
    
    public Email getEmail() {
        return email;
    }
    
    public Phone getPhone() {
        return phone;
    }
    
    public Address getAddress() {
        return address;
    }
    
    public MembershipLevel getMembershipLevel() {
        return membershipLevel;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Customer customer = (Customer) obj;
        return id.equals(customer.id);
    }
    
    /**
     * 檢查是否為新會員
     */
    public boolean isNewMember() {
        // 簡單實現：假設所有客戶都不是新會員
        // 實際實現應該檢查註冊日期
        return false;
    }
    
    /**
     * 檢查是否為生日月
     */
    public boolean isBirthdayMonth() {
        // 簡單實現：假設都不是生日月
        // 實際實現應該檢查生日日期
        return false;
    }
    
    /**
     * 使用獎勵積分
     */
    public boolean useRewardPoints(int points) {
        // 簡單實現：假設總是成功
        // 實際實現應該檢查積分餘額
        return true;
    }
    
    /**
     * 添加獎勵積分
     */
    public void addRewardPoints(int points) {
        // 簡單實現：什麼都不做
        // 實際實現應該更新積分餘額
    }
    

}