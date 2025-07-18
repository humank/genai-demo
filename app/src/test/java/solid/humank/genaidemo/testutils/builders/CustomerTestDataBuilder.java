package solid.humank.genaidemo.testutils.builders;

import solid.humank.genaidemo.domain.customer.model.aggregate.Customer;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 客戶測試資料建構器
 * 使用Builder模式來簡化測試中的客戶資料創建
 */
public class CustomerTestDataBuilder {
    
    private String customerId = "customer-" + UUID.randomUUID().toString().substring(0, 8);
    private String name = "測試客戶";
    private String email = "test@example.com";
    private LocalDate birthDate = LocalDate.of(1990, 1, 15);
    private LocalDate registrationDate = LocalDate.now().minusDays(30);
    private int rewardPoints = 0;
    
    /**
     * 創建新的客戶建構器
     */
    public static CustomerTestDataBuilder aCustomer() {
        return new CustomerTestDataBuilder();
    }
    
    /**
     * 設置客戶ID
     */
    public CustomerTestDataBuilder withId(String customerId) {
        this.customerId = customerId;
        return this;
    }
    
    /**
     * 設置客戶姓名
     */
    public CustomerTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }
    
    /**
     * 設置客戶電子郵件
     */
    public CustomerTestDataBuilder withEmail(String email) {
        this.email = email;
        return this;
    }
    
    /**
     * 設置生日
     */
    public CustomerTestDataBuilder withBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
        return this;
    }
    
    /**
     * 設置為生日月份（當前月份）
     */
    public CustomerTestDataBuilder withBirthdayInCurrentMonth() {
        LocalDate now = LocalDate.now();
        this.birthDate = LocalDate.of(1990, now.getMonth(), 15);
        return this;
    }
    
    /**
     * 設置註冊日期
     */
    public CustomerTestDataBuilder withRegistrationDate(LocalDate registrationDate) {
        this.registrationDate = registrationDate;
        return this;
    }
    
    /**
     * 設置為新會員（最近註冊）
     */
    public CustomerTestDataBuilder asNewMember() {
        this.registrationDate = LocalDate.now().minusDays(7); // 7天內註冊
        return this;
    }
    
    /**
     * 設置為老會員
     */
    public CustomerTestDataBuilder asOldMember() {
        this.registrationDate = LocalDate.now().minusYears(2);
        return this;
    }
    
    /**
     * 設置獎勵積點
     */
    public CustomerTestDataBuilder withRewardPoints(int rewardPoints) {
        this.rewardPoints = rewardPoints;
        return this;
    }
    
    /**
     * 設置為高積點客戶
     */
    public CustomerTestDataBuilder withHighRewardPoints() {
        this.rewardPoints = 5000;
        return this;
    }
    
    /**
     * 設置為低積點客戶
     */
    public CustomerTestDataBuilder withLowRewardPoints() {
        this.rewardPoints = 100;
        return this;
    }
    
    /**
     * 建構Customer領域物件
     */
    public Customer build() {
        Customer customer = new Customer(customerId, name, email, birthDate);
        customer.setRegistrationDate(registrationDate);
        customer.setRewardPoints(rewardPoints);
        return customer;
    }
    
    /**
     * 建構VIP客戶
     */
    public Customer buildVipCustomer() {
        return this.withHighRewardPoints()
                  .asOldMember()
                  .build();
    }
    
    /**
     * 建構新會員客戶
     */
    public Customer buildNewMember() {
        return this.asNewMember()
                  .withLowRewardPoints()
                  .build();
    }
    
    /**
     * 建構生日月份客戶
     */
    public Customer buildBirthdayCustomer() {
        return this.withBirthdayInCurrentMonth()
                  .build();
    }
}