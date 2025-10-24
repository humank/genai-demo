# Value Objects - Detailed Examples

## Principle Overview

**Value Objects** are immutable objects that represent descriptive aspects of the domain with no conceptual identity. They are defined by their attributes, not by an ID.

## Key Concepts

- **Immutability**: Cannot be changed after creation
- **Value Equality**: Two value objects are equal if all attributes are equal
- **Self-Validation**: Validate in constructor
- **No Identity**: Defined by attributes, not ID

**Related Standards**: [DDD Tactical Patterns](../../steering/ddd-tactical-patterns.md)

---

## Basic Value Object Pattern

### Using Java Records (Production Code)

This is the actual Email value object from our production codebase:

```java
@ValueObject
public record Email(String value) {
    
    /**
     * Compact constructor - validates parameters
     */
    public Email {
        if (value == null || !isValidEmail(value)) {
            throw new IllegalArgumentException("Invalid email format");
        }
        // Normalize to lowercase
        value = value.toLowerCase();
    }
    
    /**
     * Validate email format
     */
    private static boolean isValidEmail(String email) {
        return email.contains("@") && email.contains(".");
    }
    
    /**
     * Get email (backward compatible method)
     */
    public String getEmail() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
}
```

---

## Complete Value Object Examples (Production Code)

### Example 1: Money (Production Code)

This is the actual Money value object from our production codebase:

```java
@ValueObject
public record Money(BigDecimal amount, Currency currency) {
    
    public static final Money ZERO = new Money(BigDecimal.ZERO, Currency.getInstance("TWD"));
    
    /**
     * Compact constructor - validates parameters
     */
    public Money {
        Objects.requireNonNull(amount, "金額不能為空");
        Objects.requireNonNull(currency, "貨幣不能為空");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("金額不能為負數");
        }
    }
    
    /**
     * Create Money value object
     */
    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, Currency.getInstance(currencyCode));
    }
    
    /**
     * Create Money value object
     */
    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }
    
    /**
     * Create Money value object with default currency TWD
     */
    public static Money of(BigDecimal amount) {
        return new Money(amount, Currency.getInstance("TWD"));
    }
    
    /**
     * Create Money value object
     */
    public static Money of(double amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.getInstance("TWD"));
    }
    
    /**
     * Create Money value object
     */
    public static Money of(double amount, String currencyCode) {
        return new Money(BigDecimal.valueOf(amount), Currency.getInstance(currencyCode));
    }
    
    /**
     * Create TWD Money value object
     */
    public static Money twd(double amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.getInstance("TWD"));
    }
    
    /**
     * Create TWD Money value object
     */
    public static Money twd(int amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.getInstance("TWD"));
    }
    
    /**
     * Create zero amount Money value object
     */
    public static Money zero() {
        return ZERO;
    }
    
    /**
     * Create zero amount Money value object with specified currency
     */
    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }
    
    // Domain operations
    public Money add(Money money) {
        requireSameCurrency(money);
        return new Money(this.amount.add(money.amount), this.currency);
    }
    
    public Money plus(Money money) {
        return add(money);
    }
    
    public Money subtract(Money money) {
        requireSameCurrency(money);
        return new Money(this.amount.subtract(money.amount), this.currency);
    }
    
    public Money multiply(int multiplier) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)), this.currency);
    }
    
    public Money multiply(double multiplier) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)), this.currency);
    }
    
    public Money divide(int divisor) {
        return new Money(this.amount.divide(BigDecimal.valueOf(divisor), RoundingMode.HALF_UP), this.currency);
    }
    
    /**
     * Get amount (backward compatible method)
     */
    public BigDecimal getAmount() {
        return amount;
    }
    
    /**
     * Get currency (backward compatible method)
     */
    public Currency getCurrency() {
        return currency;
    }
    
    /**
     * Compare if amount is greater than another amount
     */
    public boolean isGreaterThan(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }
    
    /**
     * Compare if amount is less than another amount
     */
    public boolean isLessThan(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }
    
    /**
     * Compare if amount is equal to another amount
     */
    public boolean isEqualTo(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) == 0;
    }
    
    /**
     * Check if amount is zero
     */
    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }
    
    /**
     * Check if amount is positive
     */
    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Validate currency is the same
     */
    private void requireSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                "Cannot operate on money with different currencies: " + 
                this.currency.getCurrencyCode() + " vs " + other.currency.getCurrencyCode());
        }
    }
    
    @Override
    public String toString() {
        return amount + " " + currency.getCurrencyCode();
    }
}
```

### Example 2: CustomerName (Production Code)

This is the actual CustomerName value object from our production codebase:

```java
@ValueObject
public record CustomerName(String value) {
    
    /**
     * Compact constructor - validates parameters
     */
    public CustomerName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Customer name cannot be null or empty");
        }
        // Normalize: trim whitespace
        value = value.trim();
    }
    
    /**
     * Get name (backward compatible method)
     */
    public String getName() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
}
```

### Example 3: Phone Number (Production Code)

This is the actual Phone value object from our production codebase:

```java
@ValueObject
public record Phone(String value) {
    
    /**
     * Compact constructor - validates parameters
     */
    public Phone {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Phone number cannot be empty");
        }
        // Remove formatting for validation
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.length() < 10 || digits.length() > 15) {
            throw new IllegalArgumentException("Invalid phone number length");
        }
    }
    
    public static Phone of(String value) {
        return new Phone(value);
    }
    
    public String getDigitsOnly() {
        return value.replaceAll("[^0-9]", "");
    }
    
    public String getFormatted() {
        String digits = getDigitsOnly();
        if (digits.length() == 10) {
            return String.format("(%s) %s-%s",
                digits.substring(0, 3),
                digits.substring(3, 6),
                digits.substring(6));
        }
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
}
```

### Example 4: Address (Production Code)

This is the actual Address value object from our production codebase:

```java
@ValueObject
public record Address(
    String street,
    String city,
    String postalCode
) {
    
    /**
     * Compact constructor - validates parameters
     */
    public Address {
        if (street == null || street.isBlank()) {
            throw new IllegalArgumentException("Street is required");
        }
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City is required");
        }
        if (postalCode == null || postalCode.isBlank()) {
            throw new IllegalArgumentException("Postal code is required");
        }
    }
    
    public static Address of(String street, String city, String postalCode) {
        return new Address(street, city, postalCode);
    }
    
    public String getFullAddress() {
        return String.format("%s, %s %s", street, city, postalCode);
    }
    
    @Override
    public String toString() {
        return getFullAddress();
    }
}
```

### Example 5: Entity IDs (Production Code)

These are the actual ID value objects from our production codebase:

```java
@ValueObject
public record CustomerId(String value) {
    
    /**
     * Compact constructor - validates parameters
     */
    public CustomerId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Customer ID cannot be empty");
        }
    }
    
    public static CustomerId generate() {
        return new CustomerId("CUST-" + UUID.randomUUID().toString());
    }
    
    public static CustomerId of(String value) {
        return new CustomerId(value);
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
}

@ValueObject
public record OrderId(String value) {
    
    /**
     * Compact constructor - validates parameters
     */
    public OrderId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Order ID cannot be empty");
        }
    }
    
    public static OrderId generate() {
        return new OrderId("ORD-" + UUID.randomUUID().toString());
    }
    
    public static OrderId of(String value) {
        return new OrderId(value);
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
}
```

### Example 6: RewardPoints (Production Code)

This is the actual RewardPoints value object from our production codebase:

```java
@ValueObject
public record RewardPoints(int balance) {
    
    /**
     * Compact constructor - validates parameters
     */
    public RewardPoints {
        if (balance < 0) {
            throw new IllegalArgumentException("Reward points balance cannot be negative");
        }
    }
    
    public static RewardPoints empty() {
        return new RewardPoints(0);
    }
    
    public static RewardPoints of(int balance) {
        return new RewardPoints(balance);
    }
    
    public RewardPoints add(int points) {
        if (points < 0) {
            throw new IllegalArgumentException("Cannot add negative points");
        }
        return new RewardPoints(balance + points);
    }
    
    public RewardPoints redeem(int points) {
        if (points < 0) {
            throw new IllegalArgumentException("Cannot redeem negative points");
        }
        if (points > balance) {
            throw new IllegalArgumentException(
                String.format("Insufficient points. Balance: %d, Requested: %d", balance, points));
        }
        return new RewardPoints(balance - points);
    }
    
    public boolean canRedeem(int points) {
        return points > 0 && points <= balance;
    }
    
    public boolean isEmpty() {
        return balance == 0;
    }
    
    @Override
    public String toString() {
        return String.valueOf(balance);
    }
}
```

---

## Usage in Aggregates (Production Code)

```java
@AggregateRoot(
    name = "Customer", 
    description = "增強的客戶聚合根，支援完整的消費者功能", 
    boundedContext = "Customer", 
    version = "2.0"
)
public class Customer implements AggregateRootInterface {
    
    private final CustomerId id;           // Value Object
    private CustomerName name;             // Value Object
    private Email email;                   // Value Object
    private Phone phone;                   // Value Object
    private Address address;               // Value Object
    private MembershipLevel membershipLevel; // Enum (also a value object)
    private RewardPoints rewardPoints;     // Value Object
    private Money totalSpending;           // Value Object
    
    public Customer(
            CustomerId id,
            CustomerName name,
            Email email,
            Phone phone,
            Address address,
            MembershipLevel membershipLevel,
            LocalDate birthDate,
            LocalDateTime registrationDate) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.membershipLevel = membershipLevel;
        this.rewardPoints = RewardPoints.empty();
        this.totalSpending = Money.twd(0);
        
        collectEvent(CustomerCreatedEvent.create(id, name, email, membershipLevel));
    }
    
    public void updateProfile(CustomerName newName, Email newEmail, Phone newPhone) {
        validateProfileUpdate(newName, newEmail, newPhone);
        
        this.name = newName;
        this.email = newEmail;
        this.phone = newPhone;
        
        collectEvent(CustomerProfileUpdatedEvent.create(id, newName, newEmail, newPhone));
    }
    
    public void addRewardPoints(int points, String reason) {
        this.rewardPoints = this.rewardPoints.add(points);
        
        collectEvent(RewardPointsEarnedEvent.create(this.id, points, this.rewardPoints.balance(), reason));
    }
    
    public void redeemPoints(int points, String reason) {
        this.rewardPoints = this.rewardPoints.redeem(points);
        
        collectEvent(RewardPointsRedeemedEvent.create(
            this.id, points, this.rewardPoints.balance(), reason));
    }
}
```

---

## Best Practices

### ✅ DO

1. **Validate in constructor** - Use compact constructor in Records
2. **Use Records for immutability** - Automatic immutability and equals/hashCode
3. **Provide factory methods** - `of()`, `generate()`, `empty()` methods
4. **Include domain behavior** - Business logic belongs in value objects
5. **Use descriptive names** - `Email` instead of `String`, `Money` instead of `BigDecimal`
6. **Normalize values** - Lowercase emails, trim whitespace
7. **Use @ValueObject annotation** - Mark value objects for documentation

### ❌ DON'T

1. **Don't use setters** - Value objects are immutable
2. **Don't use identity-based equality** - Use value-based equality (automatic with Records)
3. **Don't make fields mutable** - All fields should be final (automatic with Records)
4. **Don't use primitive obsession** - Use value objects instead of primitives
5. **Don't skip validation** - Always validate in constructor
6. **Don't allow invalid states** - Throw exceptions for invalid values

---

## Key Patterns

### 1. Compact Constructor Validation

```java
public record Email(String value) {
    public Email {
        if (value == null || !isValidEmail(value)) {
            throw new IllegalArgumentException("Invalid email format");
        }
        value = value.toLowerCase(); // Normalization
    }
}
```

### 2. Factory Methods

```java
public record Money(BigDecimal amount, Currency currency) {
    public static Money of(double amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.getInstance("TWD"));
    }
    
    public static Money twd(int amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.getInstance("TWD"));
    }
    
    public static Money zero() {
        return new Money(BigDecimal.ZERO, Currency.getInstance("TWD"));
    }
}
```

### 3. Domain Operations

```java
public record Money(BigDecimal amount, Currency currency) {
    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }
    
    public boolean isGreaterThan(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }
}
```

### 4. Backward Compatibility

```java
public record Email(String value) {
    // Backward compatible getter
    public String getEmail() {
        return value;
    }
}
```

---

## Summary

Value Objects provide:
- **Type safety** - `Email` vs `String`, `Money` vs `BigDecimal`
- **Validation** - Always valid, cannot create invalid instances
- **Domain behavior** - Business logic in the right place
- **Immutability** - Thread-safe, predictable
- **Self-documentation** - Code is more readable and maintainable

---

**Related Documentation**:
- [DDD Tactical Patterns](../../steering/ddd-tactical-patterns.md)
- [Aggregate Root Examples](aggregate-root-examples.md)
- [Domain Events Examples](domain-events-examples.md)
