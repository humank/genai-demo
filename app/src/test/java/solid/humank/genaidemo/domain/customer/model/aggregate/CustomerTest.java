package solid.humank.genaidemo.domain.customer.model.aggregate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import solid.humank.genaidemo.domain.customer.model.valueobject.Address;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerId;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerName;
import solid.humank.genaidemo.domain.customer.model.valueobject.Email;
import solid.humank.genaidemo.domain.customer.model.valueobject.MembershipLevel;
import solid.humank.genaidemo.domain.customer.model.valueobject.NotificationPreferences;
import solid.humank.genaidemo.domain.customer.model.valueobject.Phone;

/** Customer 聚合根單元測試 測試客戶聚合根的業務邏輯和領域事件 */
@DisplayName("Customer 聚合根測試")
class CustomerTest {

    private Customer customer;
    private CustomerId customerId;
    private CustomerName customerName;
    private Email email;
    private Phone phone;
    private Address address;
    private MembershipLevel membershipLevel;

    @BeforeEach
    void setUp() {
        customerId = new CustomerId("customer-001");
        customerName = new CustomerName("張小明");
        email = new Email("zhang.xiaoming@example.com");
        phone = new Phone("0912345678");
        address = new Address("台北市", "信義區", "信義路五段7號", "110");
        membershipLevel = MembershipLevel.STANDARD;

        customer = new Customer(customerId, customerName, email, phone, address, membershipLevel);
    }

    @Test
    @DisplayName("應該能夠創建客戶")
    void shouldCreateCustomer() {
        // Then
        assertThat(customer.getId()).isEqualTo(customerId);
        assertThat(customer.getName()).isEqualTo(customerName);
        assertThat(customer.getEmail()).isEqualTo(email);
        assertThat(customer.getPhone()).isEqualTo(phone);
        assertThat(customer.getAddress()).isEqualTo(address);
        assertThat(customer.getMembershipLevel()).isEqualTo(membershipLevel);
    }

    @Test
    @DisplayName("應該能夠更新客戶資料")
    void shouldUpdateCustomerProfile() {
        // Given
        LocalDate newBirthDate = LocalDate.of(1990, 5, 15);
        NotificationPreferences newPreferences = NotificationPreferences.defaultPreferences();

        // When
        customer.updateBirthDate(newBirthDate);
        customer.updateNotificationPreferences(newPreferences);

        // Then
        assertThat(customer.getBirthDate()).isEqualTo(newBirthDate);
        assertThat(customer.getNotificationPreferences()).isEqualTo(newPreferences);
    }

    @Test
    @DisplayName("應該能夠累積獎勵積點")
    void shouldAccumulateRewardPoints() {
        // Given
        int pointsToAdd = 500;

        // When
        customer.addRewardPoints(pointsToAdd, "購買商品獲得");

        // Then
        assertThat(customer.getRewardPoints().balance()).isEqualTo(pointsToAdd);
        assertThat(customer.getRewardPoints().lastUpdated()).isNotNull();
    }

    @Test
    @DisplayName("應該能夠兌換獎勵積點")
    void shouldRedeemRewardPoints() {
        // Given
        customer.addRewardPoints(1000, "初始積點");
        int pointsToRedeem = 300;

        // When
        customer.redeemRewardPoints(pointsToRedeem, "兌換商品");

        // Then
        assertThat(customer.getRewardPoints().balance()).isEqualTo(700);
    }

    @Test
    @DisplayName("當積點不足時應該拋出異常")
    void shouldThrowExceptionWhenInsufficientPoints() {
        // Given
        customer.addRewardPoints(100, "少量積點");
        int pointsToRedeem = 200;

        // When & Then
        assertThatThrownBy(() -> customer.redeemRewardPoints(pointsToRedeem, "嘗試兌換"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("積點不足");
    }

    @Test
    @DisplayName("應該能夠升級會員等級")
    void shouldUpgradeMembershipLevel() {
        // Given
        MembershipLevel newLevel = MembershipLevel.GOLD;

        // When
        customer.upgradeMembershipLevel(newLevel);

        // Then
        assertThat(customer.getMembershipLevel()).isEqualTo(newLevel);
    }

    @Test
    @DisplayName("不應該能夠降級會員等級")
    void shouldNotDowngradeMembershipLevel() {
        // Given
        customer.upgradeMembershipLevel(MembershipLevel.VIP);

        // When & Then
        assertThatThrownBy(() -> customer.upgradeMembershipLevel(MembershipLevel.STANDARD))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不能降級");
    }

    @Test
    @DisplayName("應該能夠檢查是否為生日月份")
    void shouldCheckIfBirthdayMonth() {
        // Given
        LocalDate birthDate = LocalDate.of(1990, LocalDate.now().getMonthValue(), 15);
        customer.updateBirthDate(birthDate);

        // When
        boolean isBirthdayMonth = customer.isBirthdayMonth();

        // Then
        assertThat(isBirthdayMonth).isTrue();
    }

    @Test
    @DisplayName("應該能夠檢查是否為新會員")
    void shouldCheckIfNewMember() {
        // Given
        LocalDate recentRegistration = LocalDate.now().minusDays(5);
        customer.updateRegistrationDate(recentRegistration);

        // When
        boolean isNewMember = customer.isNewMember();

        // Then
        assertThat(isNewMember).isTrue();
    }

    @Test
    @DisplayName("應該能夠檢查會員折扣資格")
    void shouldCheckMemberDiscountEligibility() {
        // Given
        customer.upgradeMembershipLevel(MembershipLevel.VIP);

        // When
        boolean isEligible = customer.isEligibleForMemberDiscount();

        // Then
        assertThat(isEligible).isTrue();
    }

    @Test
    @DisplayName("應該能夠檢查生日折扣資格")
    void shouldCheckBirthdayDiscountEligibility() {
        // Given
        LocalDate birthDate = LocalDate.of(1990, LocalDate.now().getMonthValue(), 15);
        customer.updateBirthDate(birthDate);

        // When
        boolean isEligible = customer.isEligibleForBirthdayDiscount();

        // Then
        assertThat(isEligible).isTrue();
    }

    @Test
    @DisplayName("應該能夠更新地址")
    void shouldUpdateAddress() {
        // Given
        Address newAddress = new Address("高雄市", "前金區", "中正四路211號", "801");

        // When
        customer.updateAddress(newAddress);

        // Then
        assertThat(customer.getAddress()).isEqualTo(newAddress);
    }

    @Test
    @DisplayName("應該能夠更新聯絡資訊")
    void shouldUpdateContactInfo() {
        // Given
        Email newEmail = new Email("new.email@example.com");
        Phone newPhone = new Phone("0987654321");

        // When
        customer.updateEmail(newEmail);
        customer.updatePhone(newPhone);

        // Then
        assertThat(customer.getEmail()).isEqualTo(newEmail);
        assertThat(customer.getPhone()).isEqualTo(newPhone);
    }
}
