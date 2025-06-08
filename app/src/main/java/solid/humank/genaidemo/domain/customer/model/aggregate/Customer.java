package solid.humank.genaidemo.domain.customer.model.aggregate;

import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerId;
import solid.humank.genaidemo.domain.customer.model.valueobject.MembershipLevel;

import java.time.LocalDate;

@AggregateRoot
public class Customer {
    private CustomerId customerId;
    private String name;
    private String email;
    private LocalDate birthDate;
    private LocalDate registrationDate;
    private MembershipLevel membershipLevel;
    private int rewardPoints;
    private boolean isActive;

    // Private constructor for JPA
    private Customer() {
    }

    public Customer(CustomerId customerId, String name, String email, LocalDate birthDate) {
        this.customerId = customerId;
        this.name = name;
        this.email = email;
        this.birthDate = birthDate;
        this.registrationDate = LocalDate.now();
        this.membershipLevel = MembershipLevel.STANDARD;
        this.rewardPoints = 0;
        this.isActive = true;
    }

    public boolean isBirthdayMonth() {
        LocalDate today = LocalDate.now();
        return birthDate != null && today.getMonth() == birthDate.getMonth();
    }

    public boolean isNewMember() {
        LocalDate today = LocalDate.now();
        return registrationDate != null && registrationDate.plusMonths(1).isAfter(today);
    }

    public void addRewardPoints(int points) {
        this.rewardPoints += points;
    }

    public boolean useRewardPoints(int points) {
        if (points <= this.rewardPoints) {
            this.rewardPoints -= points;
            return true;
        }
        return false;
    }

    public void upgradeMembership(MembershipLevel newLevel) {
        this.membershipLevel = newLevel;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    public MembershipLevel getMembershipLevel() {
        return membershipLevel;
    }

    public int getRewardPoints() {
        return rewardPoints;
    }

    public boolean isActive() {
        return isActive;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }
}