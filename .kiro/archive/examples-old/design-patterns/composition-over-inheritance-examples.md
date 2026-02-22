# Composition Over Inheritance - Comprehensive Examples

## Principle Overview

**"Favor object composition over class inheritance"** is one of the most important principles in object-oriented design. This principle suggests that you should achieve code reuse through composition (has-a relationships) rather than inheritance (is-a relationships), unless there is a genuine subtype relationship.

## Table of Contents

1. [When to Use Inheritance vs Composition](#when-to-use-inheritance-vs-composition)
2. [Example 1: Discount System](#example-1-discount-system)
3. [Example 2: Notification System](#example-2-notification-system)
4. [Example 3: Payment Processing](#example-3-payment-processing)
5. [Example 4: Logging and Monitoring](#example-4-logging-and-monitoring)
6. [Example 5: Data Validation](#example-5-data-validation)
7. [Design Patterns Using Composition](#design-patterns-using-composition)
8. [Common Pitfalls](#common-pitfalls)
9. [Refactoring from Inheritance to Composition](#refactoring-from-inheritance-to-composition)

---

## When to Use Inheritance vs Composition

### Use Inheritance When:

1. **True "is-a" relationship exists**
   - A Car IS-A Vehicle
   - A Manager IS-A Employee
   - A SavingsAccount IS-A BankAccount

2. **Liskov Substitution Principle applies**
   - Subtype can replace parent type without breaking functionality
   - Subtype doesn't violate parent's contracts

3. **Shallow hierarchy (1-2 levels maximum)**
   - Deep hierarchies become fragile and hard to maintain

4. **Behavior is fundamental to the type**
   - Not just adding features, but defining what the type IS

### Use Composition When:

1. **Need code reuse without "is-a" relationship**
   - A Car HAS-A Engine (not IS-A Engine)
   - An Order HAS-A DiscountStrategy

2. **Want to change behavior at runtime**
   - Switch strategies dynamically
   - Add/remove capabilities on the fly

3. **Need multiple behaviors combined**
   - Avoid combinatorial explosion of subclasses
   - Mix and match capabilities

4. **Want to avoid fragile base class problem**
   - Changes to base class don't break composed objects
   - Better encapsulation

---

## Example 1: Discount System

### Problem: E-commerce Discount Management

We need to support various discount types: percentage discounts, fixed amount discounts, seasonal discounts, member discounts, and combinations thereof.


### ❌ Bad Approach: Inheritance Hierarchy

```java
// Base class
public abstract class Order {
    protected OrderId id;
    protected CustomerId customerId;
    protected List<OrderItem> items;
    protected Money subtotal;
    
    public abstract Money calculateTotal();
    
    protected Money calculateSubtotal() {
        return items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(Money.ZERO, Money::add);
    }
}

// First level: Basic discount types
public class StandardOrder extends Order {
    @Override
    public Money calculateTotal() {
        return calculateSubtotal();
    }
}

public class PercentageDiscountOrder extends Order {
    private final double discountPercentage;
    
    public PercentageDiscountOrder(double discountPercentage) {
        this.discountPercentage = discountPercentage;
    }
    
    @Override
    public Money calculateTotal() {
        Money subtotal = calculateSubtotal();
        Money discount = subtotal.multiply(discountPercentage);
        return subtotal.subtract(discount);
    }
}

public class FixedDiscountOrder extends Order {
    private final Money discountAmount;
    
    public FixedDiscountOrder(Money discountAmount) {
        this.discountAmount = discountAmount;
    }
    
    @Override
    public Money calculateTotal() {
        Money subtotal = calculateSubtotal();
        return subtotal.subtract(discountAmount);
    }
}

// Second level: Member-specific discounts
public class PremiumMemberOrder extends PercentageDiscountOrder {
    public PremiumMemberOrder() {
        super(0.10); // 10% discount
    }
}

public class GoldMemberOrder extends PercentageDiscountOrder {
    public GoldMemberOrder() {
        super(0.15); // 15% discount
    }
}

// Third level: Seasonal discounts
public class PremiumMemberSeasonalOrder extends PremiumMemberOrder {
    private final double seasonalDiscount;
    
    public PremiumMemberSeasonalOrder(double seasonalDiscount) {
        this.seasonalDiscount = seasonalDiscount;
    }
    
    @Override
    public Money calculateTotal() {
        Money afterMemberDiscount = super.calculateTotal();
        Money seasonalDiscountAmount = afterMemberDiscount.multiply(seasonalDiscount);
        return afterMemberDiscount.subtract(seasonalDiscountAmount);
    }
}

// Combinatorial explosion!
// Need: GoldMemberSeasonalOrder, PremiumMemberFixedDiscountOrder, etc.
// With N discount types and M member levels, need N × M classes!
```

**Problems with this approach:**

1. **Combinatorial Explosion**: Need a new class for every combination
2. **Cannot change at runtime**: Once created, discount type is fixed
3. **Fragile Base Class**: Changes to Order affect all subclasses
4. **Violates Open/Closed**: Must modify code to add new discount types
5. **Code Duplication**: Similar logic repeated across classes
6. **Hard to Test**: Must test every combination class
7. **Poor Flexibility**: Cannot apply multiple discounts dynamically


### ✅ Good Approach: Composition with Strategy Pattern

```java
// Single Order class with composed discount strategy
@AggregateRoot
public class Order {
    private final OrderId id;
    private final CustomerId customerId;
    private final List<OrderItem> items;
    private DiscountStrategy discountStrategy;
    
    public Order(OrderId id, CustomerId customerId, DiscountStrategy discountStrategy) {
        this.id = id;
        this.customerId = customerId;
        this.items = new ArrayList<>();
        this.discountStrategy = discountStrategy != null ? discountStrategy : new NoDiscount();
    }
    
    public Money calculateTotal() {
        Money subtotal = calculateSubtotal();
        return discountStrategy.apply(subtotal);
    }
    
    // Can change discount strategy at runtime
    public void applyDiscount(DiscountStrategy newStrategy) {
        this.discountStrategy = newStrategy;
    }
    
    private Money calculateSubtotal() {
        return items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(Money.ZERO, Money::add);
    }
    
    // Other order methods...
}

// Strategy interface
public interface DiscountStrategy {
    Money apply(Money amount);
    String getDescription();
}

// Concrete strategies
public class NoDiscount implements DiscountStrategy {
    @Override
    public Money apply(Money amount) {
        return amount;
    }
    
    @Override
    public String getDescription() {
        return "No discount";
    }
}

public class PercentageDiscount implements DiscountStrategy {
    private final double percentage;
    private final String description;
    
    public PercentageDiscount(double percentage, String description) {
        if (percentage < 0 || percentage > 1) {
            throw new IllegalArgumentException("Percentage must be between 0 and 1");
        }
        this.percentage = percentage;
        this.description = description;
    }
    
    @Override
    public Money apply(Money amount) {
        Money discount = amount.multiply(percentage);
        return amount.subtract(discount);
    }
    
    @Override
    public String getDescription() {
        return String.format("%s (%.0f%% off)", description, percentage * 100);
    }
}

public class FixedAmountDiscount implements DiscountStrategy {
    private final Money discountAmount;
    private final String description;
    
    public FixedAmountDiscount(Money discountAmount, String description) {
        if (discountAmount.isNegative()) {
            throw new IllegalArgumentException("Discount amount cannot be negative");
        }
        this.discountAmount = discountAmount;
        this.description = description;
    }
    
    @Override
    public Money apply(Money amount) {
        Money result = amount.subtract(discountAmount);
        return result.isNegative() ? Money.ZERO : result;
    }
    
    @Override
    public String getDescription() {
        return String.format("%s (%s off)", description, discountAmount);
    }
}

public class MinimumAmountDiscount implements DiscountStrategy {
    private final Money minimumAmount;
    private final DiscountStrategy actualDiscount;
    
    public MinimumAmountDiscount(Money minimumAmount, DiscountStrategy actualDiscount) {
        this.minimumAmount = minimumAmount;
        this.actualDiscount = actualDiscount;
    }
    
    @Override
    public Money apply(Money amount) {
        if (amount.isGreaterThanOrEqual(minimumAmount)) {
            return actualDiscount.apply(amount);
        }
        return amount;
    }
    
    @Override
    public String getDescription() {
        return String.format("Minimum %s required for %s", 
            minimumAmount, actualDiscount.getDescription());
    }
}

// Composite strategy - combine multiple discounts
public class CompositeDiscount implements DiscountStrategy {
    private final List<DiscountStrategy> strategies;
    private final String description;
    
    public CompositeDiscount(String description, DiscountStrategy... strategies) {
        this.strategies = Arrays.asList(strategies);
        this.description = description;
    }
    
    @Override
    public Money apply(Money amount) {
        Money result = amount;
        for (DiscountStrategy strategy : strategies) {
            result = strategy.apply(result);
        }
        return result;
    }
    
    @Override
    public String getDescription() {
        return description + ": " + strategies.stream()
            .map(DiscountStrategy::getDescription)
            .collect(Collectors.joining(", "));
    }
}

// Maximum discount cap
public class CappedDiscount implements DiscountStrategy {
    private final DiscountStrategy baseDiscount;
    private final Money maximumDiscount;
    
    public CappedDiscount(DiscountStrategy baseDiscount, Money maximumDiscount) {
        this.baseDiscount = baseDiscount;
        this.maximumDiscount = maximumDiscount;
    }
    
    @Override
    public Money apply(Money amount) {
        Money discountedAmount = baseDiscount.apply(amount);
        Money actualDiscount = amount.subtract(discountedAmount);
        
        if (actualDiscount.isGreaterThan(maximumDiscount)) {
            return amount.subtract(maximumDiscount);
        }
        
        return discountedAmount;
    }
    
    @Override
    public String getDescription() {
        return baseDiscount.getDescription() + " (max " + maximumDiscount + ")";
    }
}
```


### Usage Examples

```java
// Example 1: Simple percentage discount
DiscountStrategy memberDiscount = new PercentageDiscount(0.10, "Premium Member");
Order order = new Order(orderId, customerId, memberDiscount);
Money total = order.calculateTotal();

// Example 2: Fixed amount discount
DiscountStrategy couponDiscount = new FixedAmountDiscount(
    Money.of(20, "USD"), 
    "Welcome Coupon"
);
order.applyDiscount(couponDiscount);

// Example 3: Minimum purchase requirement
DiscountStrategy conditionalDiscount = new MinimumAmountDiscount(
    Money.of(100, "USD"),
    new PercentageDiscount(0.15, "Bulk Purchase")
);
order.applyDiscount(conditionalDiscount);

// Example 4: Combine multiple discounts
DiscountStrategy combinedDiscount = new CompositeDiscount(
    "Special Promotion",
    new PercentageDiscount(0.10, "Member Discount"),
    new FixedAmountDiscount(Money.of(5, "USD"), "Seasonal Bonus")
);
order.applyDiscount(combinedDiscount);

// Example 5: Capped discount
DiscountStrategy cappedDiscount = new CappedDiscount(
    new PercentageDiscount(0.20, "Flash Sale"),
    Money.of(50, "USD")
);
order.applyDiscount(cappedDiscount);

// Example 6: Complex combination
DiscountStrategy complexDiscount = new CompositeDiscount(
    "VIP Package",
    new MinimumAmountDiscount(
        Money.of(200, "USD"),
        new CappedDiscount(
            new PercentageDiscount(0.25, "VIP Discount"),
            Money.of(100, "USD")
        )
    ),
    new FixedAmountDiscount(Money.of(10, "USD"), "Loyalty Bonus")
);
order.applyDiscount(complexDiscount);
```

### Factory for Common Discounts

```java
@Component
public class DiscountFactory {
    
    public DiscountStrategy createMemberDiscount(MembershipLevel level) {
        return switch (level) {
            case STANDARD -> new NoDiscount();
            case PREMIUM -> new PercentageDiscount(0.10, "Premium Member");
            case GOLD -> new PercentageDiscount(0.15, "Gold Member");
            case PLATINUM -> new CappedDiscount(
                new PercentageDiscount(0.20, "Platinum Member"),
                Money.of(100, "USD")
            );
        };
    }
    
    public DiscountStrategy createSeasonalDiscount(Season season) {
        return switch (season) {
            case SPRING -> new PercentageDiscount(0.05, "Spring Sale");
            case SUMMER -> new PercentageDiscount(0.10, "Summer Clearance");
            case FALL -> new PercentageDiscount(0.08, "Fall Festival");
            case WINTER -> new CompositeDiscount(
                "Winter Special",
                new PercentageDiscount(0.15, "Holiday Sale"),
                new MinimumAmountDiscount(
                    Money.of(150, "USD"),
                    new FixedAmountDiscount(Money.of(20, "USD"), "Bonus")
                )
            );
        };
    }
    
    public DiscountStrategy createCombinedDiscount(
            MembershipLevel memberLevel, 
            Season season,
            boolean hasLoyaltyPoints) {
        
        List<DiscountStrategy> strategies = new ArrayList<>();
        
        // Add member discount
        strategies.add(createMemberDiscount(memberLevel));
        
        // Add seasonal discount
        strategies.add(createSeasonalDiscount(season));
        
        // Add loyalty bonus if applicable
        if (hasLoyaltyPoints) {
            strategies.add(new FixedAmountDiscount(
                Money.of(5, "USD"), 
                "Loyalty Points"
            ));
        }
        
        return new CompositeDiscount(
            "Combined Discount",
            strategies.toArray(new DiscountStrategy[0])
        );
    }
}
```

### Benefits of Composition Approach

1. **No Class Explosion**: Single Order class handles all cases
2. **Runtime Flexibility**: Change discount strategy dynamically
3. **Easy to Extend**: Add new strategies without modifying Order
4. **Composable**: Combine strategies in any way
5. **Testable**: Test each strategy independently
6. **Maintainable**: Changes to one strategy don't affect others
7. **Follows SOLID**: Open/Closed, Single Responsibility, Dependency Inversion



---

## Example 2: Notification System

### Problem: Multi-Channel Notification System

We need to send notifications through multiple channels (Email, SMS, Push, Slack) and support any combination of these channels.

### ❌ Bad Approach: Inheritance Explosion

```java
// Base class
public abstract class Notifier {
    protected String message;
    
    public abstract void send(String recipient);
}

// Single channel notifiers
public class EmailNotifier extends Notifier {
    private final EmailService emailService;
    
    public EmailNotifier(EmailService emailService) {
        this.emailService = emailService;
    }
    
    @Override
    public void send(String recipient) {
        emailService.sendEmail(recipient, message);
    }
}

public class SmsNotifier extends Notifier {
    private final SmsService smsService;
    
    public SmsNotifier(SmsService smsService) {
        this.smsService = smsService;
    }
    
    @Override
    public void send(String recipient) {
        smsService.sendSms(recipient, message);
    }
}

public class PushNotifier extends Notifier {
    private final PushService pushService;
    
    public PushNotifier(PushService pushService) {
        this.pushService = pushService;
    }
    
    @Override
    public void send(String recipient) {
        pushService.sendPush(recipient, message);
    }
}

// Combination classes - this is where it gets ugly
public class EmailAndSmsNotifier extends Notifier {
    private final EmailService emailService;
    private final SmsService smsService;
    
    public EmailAndSmsNotifier(EmailService emailService, SmsService smsService) {
        this.emailService = emailService;
        this.smsService = smsService;
    }
    
    @Override
    public void send(String recipient) {
        emailService.sendEmail(recipient, message);
        smsService.sendSms(recipient, message);
    }
}

public class EmailSmsAndPushNotifier extends Notifier {
    private final EmailService emailService;
    private final SmsService smsService;
    private final PushService pushService;
    
    public EmailSmsAndPushNotifier(EmailService emailService, 
                                   SmsService smsService, 
                                   PushService pushService) {
        this.emailService = emailService;
        this.smsService = smsService;
        this.pushService = pushService;
    }
    
    @Override
    public void send(String recipient) {
        emailService.sendEmail(recipient, message);
        smsService.sendSms(recipient, message);
        pushService.sendPush(recipient, message);
    }
}

// Need EmailAndPushNotifier, SmsAndPushNotifier, EmailAndSlackNotifier...
// With 4 channels, need 2^4 - 1 = 15 different classes!
// Adding a 5th channel? Need 31 classes!
```

**Problems:**
- **Combinatorial Explosion**: 2^N - 1 classes for N channels
- **Code Duplication**: Same logic repeated in every combination
- **Cannot Add Channels at Runtime**: Fixed at construction
- **Hard to Maintain**: Change to one channel affects multiple classes
- **Violates DRY**: Notification logic duplicated everywhere

### ✅ Good Approach: Composition with Decorator Pattern

```java
// Base interface
public interface Notifier {
    void send(String recipient, String message);
    List<String> getChannels();
}

// Base implementation (optional, can start with empty)
public class BaseNotifier implements Notifier {
    @Override
    public void send(String recipient, String message) {
        // Base implementation does nothing or logs
        System.out.println("Notification prepared: " + message);
    }
    
    @Override
    public List<String> getChannels() {
        return new ArrayList<>();
    }
}

// Abstract decorator
public abstract class NotifierDecorator implements Notifier {
    protected final Notifier wrapped;
    
    protected NotifierDecorator(Notifier wrapped) {
        this.wrapped = wrapped;
    }
    
    @Override
    public void send(String recipient, String message) {
        wrapped.send(recipient, message);
    }
    
    @Override
    public List<String> getChannels() {
        return new ArrayList<>(wrapped.getChannels());
    }
}

// Concrete decorators
public class EmailNotifier extends NotifierDecorator {
    private final EmailService emailService;
    
    public EmailNotifier(Notifier wrapped, EmailService emailService) {
        super(wrapped);
        this.emailService = emailService;
    }
    
    @Override
    public void send(String recipient, String message) {
        // First, delegate to wrapped notifier
        super.send(recipient, message);
        
        // Then, add email notification
        try {
            emailService.sendEmail(recipient, message);
            System.out.println("Email sent to: " + recipient);
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
            // Don't throw - allow other channels to proceed
        }
    }
    
    @Override
    public List<String> getChannels() {
        List<String> channels = super.getChannels();
        channels.add("EMAIL");
        return channels;
    }
}

public class SmsNotifier extends NotifierDecorator {
    private final SmsService smsService;
    
    public SmsNotifier(Notifier wrapped, SmsService smsService) {
        super(wrapped);
        this.smsService = smsService;
    }
    
    @Override
    public void send(String recipient, String message) {
        super.send(recipient, message);
        
        try {
            // Truncate message for SMS if needed
            String smsMessage = truncateForSms(message);
            smsService.sendSms(recipient, smsMessage);
            System.out.println("SMS sent to: " + recipient);
        } catch (Exception e) {
            System.err.println("Failed to send SMS: " + e.getMessage());
        }
    }
    
    private String truncateForSms(String message) {
        return message.length() > 160 ? message.substring(0, 157) + "..." : message;
    }
    
    @Override
    public List<String> getChannels() {
        List<String> channels = super.getChannels();
        channels.add("SMS");
        return channels;
    }
}

public class PushNotifier extends NotifierDecorator {
    private final PushService pushService;
    
    public PushNotifier(Notifier wrapped, PushService pushService) {
        super(wrapped);
        this.pushService = pushService;
    }
    
    @Override
    public void send(String recipient, String message) {
        super.send(recipient, message);
        
        try {
            pushService.sendPush(recipient, message);
            System.out.println("Push notification sent to: " + recipient);
        } catch (Exception e) {
            System.err.println("Failed to send push: " + e.getMessage());
        }
    }
    
    @Override
    public List<String> getChannels() {
        List<String> channels = super.getChannels();
        channels.add("PUSH");
        return channels;
    }
}

public class SlackNotifier extends NotifierDecorator {
    private final SlackService slackService;
    private final String channelName;
    
    public SlackNotifier(Notifier wrapped, SlackService slackService, String channelName) {
        super(wrapped);
        this.slackService = slackService;
        this.channelName = channelName;
    }
    
    @Override
    public void send(String recipient, String message) {
        super.send(recipient, message);
        
        try {
            slackService.postMessage(channelName, message);
            System.out.println("Slack message posted to: " + channelName);
        } catch (Exception e) {
            System.err.println("Failed to post to Slack: " + e.getMessage());
        }
    }
    
    @Override
    public List<String> getChannels() {
        List<String> channels = super.getChannels();
        channels.add("SLACK");
        return channels;
    }
}

// Advanced decorators
public class RetryNotifier extends NotifierDecorator {
    private final int maxRetries;
    private final long retryDelayMs;
    
    public RetryNotifier(Notifier wrapped, int maxRetries, long retryDelayMs) {
        super(wrapped);
        this.maxRetries = maxRetries;
        this.retryDelayMs = retryDelayMs;
    }
    
    @Override
    public void send(String recipient, String message) {
        int attempts = 0;
        Exception lastException = null;
        
        while (attempts < maxRetries) {
            try {
                super.send(recipient, message);
                return; // Success
            } catch (Exception e) {
                lastException = e;
                attempts++;
                if (attempts < maxRetries) {
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                }
            }
        }
        
        throw new RuntimeException("Failed after " + maxRetries + " attempts", lastException);
    }
}

public class LoggingNotifier extends NotifierDecorator {
    private static final Logger logger = LoggerFactory.getLogger(LoggingNotifier.class);
    
    public LoggingNotifier(Notifier wrapped) {
        super(wrapped);
    }
    
    @Override
    public void send(String recipient, String message) {
        logger.info("Sending notification to {} via channels: {}", 
            recipient, getChannels());
        
        long startTime = System.currentTimeMillis();
        
        try {
            super.send(recipient, message);
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Notification sent successfully in {}ms", duration);
        } catch (Exception e) {
            logger.error("Notification failed: {}", e.getMessage(), e);
            throw e;
        }
    }
}

public class RateLimitedNotifier extends NotifierDecorator {
    private final RateLimiter rateLimiter;
    
    public RateLimitedNotifier(Notifier wrapped, int permitsPerSecond) {
        super(wrapped);
        this.rateLimiter = RateLimiter.create(permitsPerSecond);
    }
    
    @Override
    public void send(String recipient, String message) {
        rateLimiter.acquire();
        super.send(recipient, message);
    }
}
```



### Usage Examples

```java
// Example 1: Email only
Notifier emailOnly = new EmailNotifier(
    new BaseNotifier(),
    emailService
);
emailOnly.send("user@example.com", "Welcome!");

// Example 2: Email + SMS
Notifier emailAndSms = new SmsNotifier(
    new EmailNotifier(
        new BaseNotifier(),
        emailService
    ),
    smsService
);
emailAndSms.send("user@example.com", "Order confirmed!");

// Example 3: All channels
Notifier allChannels = new SlackNotifier(
    new PushNotifier(
        new SmsNotifier(
            new EmailNotifier(
                new BaseNotifier(),
                emailService
            ),
            smsService
        ),
        pushService
    ),
    slackService,
    "#notifications"
);
allChannels.send("user@example.com", "Critical alert!");

// Example 4: With logging and retry
Notifier robustNotifier = new LoggingNotifier(
    new RetryNotifier(
        new EmailNotifier(
            new BaseNotifier(),
            emailService
        ),
        3,  // max retries
        1000  // 1 second delay
    )
);
robustNotifier.send("user@example.com", "Important message");

// Example 5: With rate limiting
Notifier rateLimitedNotifier = new RateLimitedNotifier(
    new SmsNotifier(
        new BaseNotifier(),
        smsService
    ),
    10  // 10 SMS per second max
);
rateLimitedNotifier.send("+1234567890", "Notification");

// Example 6: Dynamic composition based on user preferences
public class NotificationService {
    
    public Notifier createNotifierForUser(User user) {
        Notifier notifier = new BaseNotifier();
        
        if (user.getPreferences().isEmailEnabled()) {
            notifier = new EmailNotifier(notifier, emailService);
        }
        
        if (user.getPreferences().isSmsEnabled()) {
            notifier = new SmsNotifier(notifier, smsService);
        }
        
        if (user.getPreferences().isPushEnabled()) {
            notifier = new PushNotifier(notifier, pushService);
        }
        
        // Always add logging
        notifier = new LoggingNotifier(notifier);
        
        // Add retry for critical notifications
        if (user.getPreferences().isCriticalNotificationsEnabled()) {
            notifier = new RetryNotifier(notifier, 3, 1000);
        }
        
        return notifier;
    }
    
    public void sendNotification(User user, String message) {
        Notifier notifier = createNotifierForUser(user);
        notifier.send(user.getEmail(), message);
    }
}
```

### Builder Pattern for Easier Construction

```java
public class NotifierBuilder {
    private Notifier notifier = new BaseNotifier();
    private final EmailService emailService;
    private final SmsService smsService;
    private final PushService pushService;
    private final SlackService slackService;
    
    public NotifierBuilder(EmailService emailService, 
                          SmsService smsService,
                          PushService pushService,
                          SlackService slackService) {
        this.emailService = emailService;
        this.smsService = smsService;
        this.pushService = pushService;
        this.slackService = slackService;
    }
    
    public NotifierBuilder withEmail() {
        notifier = new EmailNotifier(notifier, emailService);
        return this;
    }
    
    public NotifierBuilder withSms() {
        notifier = new SmsNotifier(notifier, smsService);
        return this;
    }
    
    public NotifierBuilder withPush() {
        notifier = new PushNotifier(notifier, pushService);
        return this;
    }
    
    public NotifierBuilder withSlack(String channel) {
        notifier = new SlackNotifier(notifier, slackService, channel);
        return this;
    }
    
    public NotifierBuilder withLogging() {
        notifier = new LoggingNotifier(notifier);
        return this;
    }
    
    public NotifierBuilder withRetry(int maxRetries, long delayMs) {
        notifier = new RetryNotifier(notifier, maxRetries, delayMs);
        return this;
    }
    
    public NotifierBuilder withRateLimit(int permitsPerSecond) {
        notifier = new RateLimitedNotifier(notifier, permitsPerSecond);
        return this;
    }
    
    public Notifier build() {
        return notifier;
    }
}

// Usage with builder
Notifier notifier = new NotifierBuilder(emailService, smsService, pushService, slackService)
    .withEmail()
    .withSms()
    .withPush()
    .withLogging()
    .withRetry(3, 1000)
    .build();

notifier.send("user@example.com", "Hello!");
```

### Benefits of Decorator Pattern

1. **No Class Explosion**: Add N channels with N classes, not 2^N
2. **Runtime Composition**: Build any combination dynamically
3. **Easy to Extend**: Add new channels without modifying existing code
4. **Single Responsibility**: Each decorator has one job
5. **Open/Closed Principle**: Open for extension, closed for modification
6. **Flexible**: Can add cross-cutting concerns (logging, retry, rate limiting)

---

## Example 3: Payment Processing

### Problem: Multiple Payment Methods with Different Processing Logic

Support credit cards, debit cards, PayPal, bank transfers, cryptocurrency, and gift cards.

### ❌ Bad Approach: Inheritance for Payment Types

```java
public abstract class Payment {
    protected Money amount;
    protected String transactionId;
    
    public abstract PaymentResult process();
    public abstract boolean validate();
}

public class CreditCardPayment extends Payment {
    private String cardNumber;
    private String cvv;
    private LocalDate expiryDate;
    
    @Override
    public boolean validate() {
        return validateCardNumber() && 
               validateCvv() && 
               validateExpiry();
    }
    
    @Override
    public PaymentResult process() {
        // Credit card specific processing
        return creditCardGateway.charge(cardNumber, amount);
    }
}

public class PayPalPayment extends Payment {
    private String email;
    private String token;
    
    @Override
    public boolean validate() {
        return validateEmail() && validateToken();
    }
    
    @Override
    public PaymentResult process() {
        return paypalGateway.charge(email, token, amount);
    }
}

// Now need: CreditCardWithRewardsPayment, PayPalWithInsurancePayment, etc.
// Combinatorial explosion again!
```



### ✅ Good Approach: Composition with Strategy + Decorator

```java
// Payment method interface (Strategy)
public interface PaymentMethod {
    PaymentResult process(Money amount);
    boolean validate();
    PaymentMethodType getType();
    String getDisplayName();
}

// Concrete payment methods
public class CreditCardPaymentMethod implements PaymentMethod {
    private final String cardNumber;
    private final String cvv;
    private final LocalDate expiryDate;
    private final String cardholderName;
    private final CreditCardGateway gateway;
    
    public CreditCardPaymentMethod(String cardNumber, String cvv, 
                                   LocalDate expiryDate, String cardholderName,
                                   CreditCardGateway gateway) {
        this.cardNumber = cardNumber;
        this.cvv = cvv;
        this.expiryDate = expiryDate;
        this.cardholderName = cardholderName;
        this.gateway = gateway;
    }
    
    @Override
    public boolean validate() {
        if (!isValidCardNumber(cardNumber)) {
            return false;
        }
        if (!isValidCvv(cvv)) {
            return false;
        }
        if (expiryDate.isBefore(LocalDate.now())) {
            return false;
        }
        return true;
    }
    
    @Override
    public PaymentResult process(Money amount) {
        if (!validate()) {
            return PaymentResult.failed("Invalid credit card details");
        }
        
        return gateway.charge(cardNumber, cvv, amount, cardholderName);
    }
    
    @Override
    public PaymentMethodType getType() {
        return PaymentMethodType.CREDIT_CARD;
    }
    
    @Override
    public String getDisplayName() {
        return "Credit Card ending in " + cardNumber.substring(cardNumber.length() - 4);
    }
    
    private boolean isValidCardNumber(String number) {
        // Luhn algorithm implementation
        return number != null && number.matches("\\d{13,19}");
    }
    
    private boolean isValidCvv(String cvv) {
        return cvv != null && cvv.matches("\\d{3,4}");
    }
}

public class PayPalPaymentMethod implements PaymentMethod {
    private final String email;
    private final String token;
    private final PayPalGateway gateway;
    
    public PayPalPaymentMethod(String email, String token, PayPalGateway gateway) {
        this.email = email;
        this.token = token;
        this.gateway = gateway;
    }
    
    @Override
    public boolean validate() {
        return email != null && email.contains("@") && token != null;
    }
    
    @Override
    public PaymentResult process(Money amount) {
        if (!validate()) {
            return PaymentResult.failed("Invalid PayPal credentials");
        }
        
        return gateway.charge(email, token, amount);
    }
    
    @Override
    public PaymentMethodType getType() {
        return PaymentMethodType.PAYPAL;
    }
    
    @Override
    public String getDisplayName() {
        return "PayPal (" + email + ")";
    }
}

public class BankTransferPaymentMethod implements PaymentMethod {
    private final String accountNumber;
    private final String routingNumber;
    private final String accountHolderName;
    private final BankTransferGateway gateway;
    
    public BankTransferPaymentMethod(String accountNumber, String routingNumber,
                                    String accountHolderName, BankTransferGateway gateway) {
        this.accountNumber = accountNumber;
        this.routingNumber = routingNumber;
        this.accountHolderName = accountHolderName;
        this.gateway = gateway;
    }
    
    @Override
    public boolean validate() {
        return accountNumber != null && 
               routingNumber != null && 
               accountHolderName != null;
    }
    
    @Override
    public PaymentResult process(Money amount) {
        if (!validate()) {
            return PaymentResult.failed("Invalid bank account details");
        }
        
        return gateway.initiateTransfer(accountNumber, routingNumber, amount);
    }
    
    @Override
    public PaymentMethodType getType() {
        return PaymentMethodType.BANK_TRANSFER;
    }
    
    @Override
    public String getDisplayName() {
        return "Bank Transfer (" + accountNumber.substring(accountNumber.length() - 4) + ")";
    }
}

// Payment processor with composed features (Decorator)
public abstract class PaymentProcessor implements PaymentMethod {
    protected final PaymentMethod wrapped;
    
    protected PaymentProcessor(PaymentMethod wrapped) {
        this.wrapped = wrapped;
    }
    
    @Override
    public boolean validate() {
        return wrapped.validate();
    }
    
    @Override
    public PaymentResult process(Money amount) {
        return wrapped.process(amount);
    }
    
    @Override
    public PaymentMethodType getType() {
        return wrapped.getType();
    }
    
    @Override
    public String getDisplayName() {
        return wrapped.getDisplayName();
    }
}

public class FraudDetectionPaymentProcessor extends PaymentProcessor {
    private final FraudDetectionService fraudService;
    
    public FraudDetectionPaymentProcessor(PaymentMethod wrapped, 
                                         FraudDetectionService fraudService) {
        super(wrapped);
        this.fraudService = fraudService;
    }
    
    @Override
    public PaymentResult process(Money amount) {
        // Check for fraud before processing
        FraudCheckResult fraudCheck = fraudService.checkTransaction(
            wrapped.getType(),
            amount,
            getCurrentUserId()
        );
        
        if (fraudCheck.isSuspicious()) {
            return PaymentResult.failed("Transaction flagged as suspicious");
        }
        
        return super.process(amount);
    }
}

public class RewardsPaymentProcessor extends PaymentProcessor {
    private final RewardsService rewardsService;
    private final double rewardsPercentage;
    
    public RewardsPaymentProcessor(PaymentMethod wrapped, 
                                  RewardsService rewardsService,
                                  double rewardsPercentage) {
        super(wrapped);
        this.rewardsService = rewardsService;
        this.rewardsPercentage = rewardsPercentage;
    }
    
    @Override
    public PaymentResult process(Money amount) {
        PaymentResult result = super.process(amount);
        
        if (result.isSuccessful()) {
            // Award rewards points after successful payment
            int points = calculateRewardsPoints(amount);
            rewardsService.awardPoints(getCurrentUserId(), points);
        }
        
        return result;
    }
    
    private int calculateRewardsPoints(Money amount) {
        return (int) (amount.getAmount().doubleValue() * rewardsPercentage);
    }
}

public class InsurancePaymentProcessor extends PaymentProcessor {
    private final InsuranceService insuranceService;
    private final Money insuranceFee;
    
    public InsurancePaymentProcessor(PaymentMethod wrapped, 
                                    InsuranceService insuranceService,
                                    Money insuranceFee) {
        super(wrapped);
        this.insuranceService = insuranceService;
        this.insuranceFee = insuranceFee;
    }
    
    @Override
    public PaymentResult process(Money amount) {
        // Add insurance fee to total
        Money totalWithInsurance = amount.add(insuranceFee);
        
        PaymentResult result = wrapped.process(totalWithInsurance);
        
        if (result.isSuccessful()) {
            // Register insurance policy
            insuranceService.createPolicy(
                getCurrentUserId(),
                result.getTransactionId(),
                amount
            );
        }
        
        return result;
    }
}

public class PaymentAuditProcessor extends PaymentProcessor {
    private final AuditService auditService;
    
    public PaymentAuditProcessor(PaymentMethod wrapped, AuditService auditService) {
        super(wrapped);
        this.auditService = auditService;
    }
    
    @Override
    public PaymentResult process(Money amount) {
        String auditId = auditService.startAudit(
            "PAYMENT_PROCESSING",
            Map.of(
                "paymentMethod", wrapped.getType(),
                "amount", amount,
                "userId", getCurrentUserId()
            )
        );
        
        try {
            PaymentResult result = super.process(amount);
            
            auditService.completeAudit(auditId, result.isSuccessful(), 
                Map.of("transactionId", result.getTransactionId()));
            
            return result;
        } catch (Exception e) {
            auditService.failAudit(auditId, e.getMessage());
            throw e;
        }
    }
}
```

### Complete Usage Example

```java
@Service
public class PaymentService {
    private final CreditCardGateway creditCardGateway;
    private final PayPalGateway paypalGateway;
    private final FraudDetectionService fraudService;
    private final RewardsService rewardsService;
    private final InsuranceService insuranceService;
    private final AuditService auditService;
    
    public PaymentResult processPayment(Order order, PaymentMethodRequest request) {
        // 1. Create base payment method based on type
        PaymentMethod baseMethod = createBasePaymentMethod(request);
        
        // 2. Compose with features based on order and customer
        PaymentMethod processor = composePaymentProcessor(baseMethod, order);
        
        // 3. Process payment
        return processor.process(order.getTotalAmount());
    }
    
    private PaymentMethod createBasePaymentMethod(PaymentMethodRequest request) {
        return switch (request.getType()) {
            case CREDIT_CARD -> new CreditCardPaymentMethod(
                request.getCardNumber(),
                request.getCvv(),
                request.getExpiryDate(),
                request.getCardholderName(),
                creditCardGateway
            );
            case PAYPAL -> new PayPalPaymentMethod(
                request.getEmail(),
                request.getToken(),
                paypalGateway
            );
            case BANK_TRANSFER -> new BankTransferPaymentMethod(
                request.getAccountNumber(),
                request.getRoutingNumber(),
                request.getAccountHolderName(),
                bankTransferGateway
            );
            default -> throw new UnsupportedPaymentMethodException(request.getType());
        };
    }
    
    private PaymentMethod composePaymentProcessor(PaymentMethod baseMethod, Order order) {
        PaymentMethod processor = baseMethod;
        
        // Always add fraud detection
        processor = new FraudDetectionPaymentProcessor(processor, fraudService);
        
        // Add rewards for eligible customers
        Customer customer = order.getCustomer();
        if (customer.isEligibleForRewards()) {
            double rewardsRate = customer.getRewardsRate();
            processor = new RewardsPaymentProcessor(processor, rewardsService, rewardsRate);
        }
        
        // Add insurance if requested
        if (order.hasInsuranceRequested()) {
            Money insuranceFee = calculateInsuranceFee(order.getTotalAmount());
            processor = new InsurancePaymentProcessor(processor, insuranceService, insuranceFee);
        }
        
        // Always add audit trail
        processor = new PaymentAuditProcessor(processor, auditService);
        
        return processor;
    }
    
    private Money calculateInsuranceFee(Money amount) {
        // 2% of order amount
        return amount.multiply(0.02);
    }
}
```

### Testing with Composition

```java
@ExtendWith(MockitoExtension.class)
class PaymentProcessorTest {
    
    @Mock
    private PaymentMethod basePaymentMethod;
    
    @Mock
    private FraudDetectionService fraudService;
    
    @Mock
    private RewardsService rewardsService;
    
    @Test
    void should_detect_fraud_before_processing_payment() {
        // Given
        Money amount = Money.of(1000, "USD");
        when(fraudService.checkTransaction(any(), eq(amount), any()))
            .thenReturn(FraudCheckResult.suspicious("High amount transaction"));
        
        PaymentMethod processor = new FraudDetectionPaymentProcessor(
            basePaymentMethod,
            fraudService
        );
        
        // When
        PaymentResult result = processor.process(amount);
        
        // Then
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getFailureReason()).contains("suspicious");
        verify(basePaymentMethod, never()).process(any());
    }
    
    @Test
    void should_award_rewards_after_successful_payment() {
        // Given
        Money amount = Money.of(100, "USD");
        when(basePaymentMethod.process(amount))
            .thenReturn(PaymentResult.successful("TXN-123"));
        
        PaymentMethod processor = new RewardsPaymentProcessor(
            basePaymentMethod,
            rewardsService,
            0.05  // 5% rewards
        );
        
        // When
        PaymentResult result = processor.process(amount);
        
        // Then
        assertThat(result.isSuccessful()).isTrue();
        verify(rewardsService).awardPoints(any(), eq(5));  // 5 points
    }
    
    @Test
    void should_compose_multiple_processors() {
        // Given
        Money amount = Money.of(200, "USD");
        when(fraudService.checkTransaction(any(), any(), any()))
            .thenReturn(FraudCheckResult.safe());
        when(basePaymentMethod.process(any()))
            .thenReturn(PaymentResult.successful("TXN-456"));
        
        // Compose: Fraud Detection -> Rewards -> Base Payment
        PaymentMethod processor = new RewardsPaymentProcessor(
            new FraudDetectionPaymentProcessor(
                basePaymentMethod,
                fraudService
            ),
            rewardsService,
            0.05
        );
        
        // When
        PaymentResult result = processor.process(amount);
        
        // Then
        assertThat(result.isSuccessful()).isTrue();
        verify(fraudService).checkTransaction(any(), eq(amount), any());
        verify(basePaymentMethod).process(amount);
        verify(rewardsService).awardPoints(any(), eq(10));
    }
}
```

---

## Example 4: Logging and Monitoring

### Problem: Add Logging, Metrics, and Tracing to Services

We need to add cross-cutting concerns to various services without modifying their core logic.

### ❌ Bad Approach: Inheritance for Cross-Cutting Concerns

```java
public abstract class BaseService {
    protected void logMethodEntry(String methodName, Object... params) {
        // Logging logic
    }
    
    protected void logMethodExit(String methodName, Object result) {
        // Logging logic
    }
    
    protected void recordMetric(String metricName, long value) {
        // Metrics logic
    }
}

public class CustomerService extends BaseService {
    public Customer createCustomer(CreateCustomerCommand command) {
        logMethodEntry("createCustomer", command);
        
        // Business logic
        Customer customer = new Customer(command);
        customerRepository.save(customer);
        
        recordMetric("customers.created", 1);
        logMethodExit("createCustomer", customer);
        
        return customer;
    }
}

// Problems:
// - Logging code mixed with business logic
// - Every service must extend BaseService
// - Cannot compose different monitoring strategies
// - Hard to test business logic separately
```

### ✅ Good Approach: Composition with Decorator

```java
// Service interface
public interface CustomerService {
    Customer createCustomer(CreateCustomerCommand command);
    Customer updateCustomer(UpdateCustomerCommand command);
    void deleteCustomer(CustomerId customerId);
    Optional<Customer> findById(CustomerId customerId);
}

// Core implementation - pure business logic
@Service
public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepository customerRepository;
    private final DomainEventApplicationService eventService;
    
    @Override
    @Transactional
    public Customer createCustomer(CreateCustomerCommand command) {
        // Pure business logic, no logging/metrics
        Customer customer = new Customer(
            CustomerId.generate(),
            command.name(),
            command.email(),
            command.membershipLevel()
        );
        
        customerRepository.save(customer);
        eventService.publishEventsFromAggregate(customer);
        
        return customer;
    }
    
    @Override
    @Transactional
    public Customer updateCustomer(UpdateCustomerCommand command) {
        Customer customer = customerRepository.findById(command.customerId())
            .orElseThrow(() -> new CustomerNotFoundException(command.customerId()));
        
        customer.updateProfile(command.name(), command.email(), command.phone());
        
        customerRepository.save(customer);
        eventService.publishEventsFromAggregate(customer);
        
        return customer;
    }
    
    @Override
    @Transactional
    public void deleteCustomer(CustomerId customerId) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));
        
        customerRepository.delete(customer);
    }
    
    @Override
    public Optional<Customer> findById(CustomerId customerId) {
        return customerRepository.findById(customerId);
    }
}

// Logging decorator
public class LoggingCustomerService implements CustomerService {
    private static final Logger logger = LoggerFactory.getLogger(LoggingCustomerService.class);
    private final CustomerService wrapped;
    
    public LoggingCustomerService(CustomerService wrapped) {
        this.wrapped = wrapped;
    }
    
    @Override
    public Customer createCustomer(CreateCustomerCommand command) {
        logger.info("Creating customer", 
            kv("email", command.email()),
            kv("membershipLevel", command.membershipLevel()));
        
        try {
            Customer customer = wrapped.createCustomer(command);
            logger.info("Customer created successfully", 
                kv("customerId", customer.getId()));
            return customer;
        } catch (Exception e) {
            logger.error("Failed to create customer", 
                kv("email", command.email()), e);
            throw e;
        }
    }
    
    @Override
    public Customer updateCustomer(UpdateCustomerCommand command) {
        logger.info("Updating customer", kv("customerId", command.customerId()));
        
        try {
            Customer customer = wrapped.updateCustomer(command);
            logger.info("Customer updated successfully", 
                kv("customerId", customer.getId()));
            return customer;
        } catch (Exception e) {
            logger.error("Failed to update customer", 
                kv("customerId", command.customerId()), e);
            throw e;
        }
    }
    
    @Override
    public void deleteCustomer(CustomerId customerId) {
        logger.info("Deleting customer", kv("customerId", customerId));
        
        try {
            wrapped.deleteCustomer(customerId);
            logger.info("Customer deleted successfully", kv("customerId", customerId));
        } catch (Exception e) {
            logger.error("Failed to delete customer", kv("customerId", customerId), e);
            throw e;
        }
    }
    
    @Override
    public Optional<Customer> findById(CustomerId customerId) {
        logger.debug("Finding customer", kv("customerId", customerId));
        return wrapped.findById(customerId);
    }
}

// Metrics decorator
public class MetricsCustomerService implements CustomerService {
    private final CustomerService wrapped;
    private final MeterRegistry meterRegistry;
    private final Counter customersCreated;
    private final Counter customersUpdated;
    private final Counter customersDeleted;
    private final Timer operationTimer;
    
    public MetricsCustomerService(CustomerService wrapped, MeterRegistry meterRegistry) {
        this.wrapped = wrapped;
        this.meterRegistry = meterRegistry;
        this.customersCreated = Counter.builder("customers.created")
            .description("Total customers created")
            .register(meterRegistry);
        this.customersUpdated = Counter.builder("customers.updated")
            .description("Total customers updated")
            .register(meterRegistry);
        this.customersDeleted = Counter.builder("customers.deleted")
            .description("Total customers deleted")
            .register(meterRegistry);
        this.operationTimer = Timer.builder("customers.operation.duration")
            .description("Customer operation duration")
            .register(meterRegistry);
    }
    
    @Override
    public Customer createCustomer(CreateCustomerCommand command) {
        return operationTimer.record(() -> {
            Customer customer = wrapped.createCustomer(command);
            customersCreated.increment();
            return customer;
        });
    }
    
    @Override
    public Customer updateCustomer(UpdateCustomerCommand command) {
        return operationTimer.record(() -> {
            Customer customer = wrapped.updateCustomer(command);
            customersUpdated.increment();
            return customer;
        });
    }
    
    @Override
    public void deleteCustomer(CustomerId customerId) {
        operationTimer.record(() -> {
            wrapped.deleteCustomer(customerId);
            customersDeleted.increment();
            return null;
        });
    }
    
    @Override
    public Optional<Customer> findById(CustomerId customerId) {
        return wrapped.findById(customerId);
    }
}

// Tracing decorator
public class TracingCustomerService implements CustomerService {
    private final CustomerService wrapped;
    private final Tracer tracer;
    
    public TracingCustomerService(CustomerService wrapped, Tracer tracer) {
        this.wrapped = wrapped;
        this.tracer = tracer;
    }
    
    @Override
    public Customer createCustomer(CreateCustomerCommand command) {
        Span span = tracer.nextSpan().name("create-customer").start();
        
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            span.tag("email", command.email().getValue());
            span.tag("membershipLevel", command.membershipLevel().name());
            
            Customer customer = wrapped.createCustomer(command);
            
            span.tag("customerId", customer.getId().getValue());
            span.event("customer.created");
            
            return customer;
        } catch (Exception e) {
            span.tag("error", e.getMessage());
            span.error(e);
            throw e;
        } finally {
            span.end();
        }
    }
    
    @Override
    public Customer updateCustomer(UpdateCustomerCommand command) {
        Span span = tracer.nextSpan().name("update-customer").start();
        
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            span.tag("customerId", command.customerId().getValue());
            
            Customer customer = wrapped.updateCustomer(command);
            span.event("customer.updated");
            
            return customer;
        } catch (Exception e) {
            span.error(e);
            throw e;
        } finally {
            span.end();
        }
    }
    
    @Override
    public void deleteCustomer(CustomerId customerId) {
        Span span = tracer.nextSpan().name("delete-customer").start();
        
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            span.tag("customerId", customerId.getValue());
            wrapped.deleteCustomer(customerId);
            span.event("customer.deleted");
        } catch (Exception e) {
            span.error(e);
            throw e;
        } finally {
            span.end();
        }
    }
    
    @Override
    public Optional<Customer> findById(CustomerId customerId) {
        return wrapped.findById(customerId);
    }
}
```



### Configuration and Usage

```java
@Configuration
public class CustomerServiceConfiguration {
    
    @Bean
    public CustomerService customerService(
            CustomerRepository customerRepository,
            DomainEventApplicationService eventService,
            MeterRegistry meterRegistry,
            Tracer tracer) {
        
        // Start with core implementation
        CustomerService service = new CustomerServiceImpl(customerRepository, eventService);
        
        // Add logging
        service = new LoggingCustomerService(service);
        
        // Add metrics
        service = new MetricsCustomerService(service, meterRegistry);
        
        // Add tracing
        service = new TracingCustomerService(service, tracer);
        
        return service;
    }
}
```

### Benefits

1. **Separation of Concerns**: Business logic separate from monitoring
2. **Easy to Test**: Test business logic without decorators
3. **Flexible**: Add/remove monitoring features easily
4. **Reusable**: Same decorators work for any service
5. **No Code Duplication**: Monitoring logic in one place

---

## Key Benefits of Composition

### 1. Runtime Flexibility

```java
// Can change behavior at runtime
Order order = new Order(orderId, customerId, new NoDiscount());
order.applyDiscount(new PercentageDiscount(0.15, "Flash Sale"));
```

### 2. Easy Testing

```java
// Test each component independently
@Test
void testPercentageDiscount() {
    DiscountStrategy discount = new PercentageDiscount(0.10, "Test");
    Money result = discount.apply(Money.of(100, "USD"));
    assertThat(result).isEqualTo(Money.of(90, "USD"));
}
```

### 3. No Class Explosion

- With inheritance: N × M × P classes for N discounts, M members, P seasons
- With composition: N + M + P classes total

### 4. Follows SOLID Principles

- **Single Responsibility**: Each strategy has one job
- **Open/Closed**: Add new strategies without modifying existing code
- **Liskov Substitution**: All strategies are interchangeable
- **Interface Segregation**: Clean, focused interfaces
- **Dependency Inversion**: Depend on abstractions, not concretions

---

## Common Pitfalls and Solutions

### ❌ Pitfall 1: Over-Engineering

```java
// Don't create strategies for everything
public interface StringFormatter {
    String format(String input);
}

// This is overkill for simple formatting
public class UpperCaseFormatter implements StringFormatter {
    public String format(String input) {
        return input.toUpperCase();
    }
}
```

### ✅ Solution: Use composition for complex, varying behavior

```java
// Good use case: complex business rules that change
public interface PricingStrategy {
    Money calculatePrice(Product product, Customer customer, LocalDateTime time);
}
```

### ❌ Pitfall 2: Ignoring True IS-A Relationships

```java
// Don't use composition when inheritance makes sense
public class Car {
    private Vehicle vehicle; // Wrong! Car IS-A Vehicle
}
```

### ✅ Solution: Use inheritance for true subtypes

```java
// Correct: Car IS-A Vehicle
public class Car extends Vehicle {
    private Engine engine; // Correct: Car HAS-A Engine
}
```

### ❌ Pitfall 3: Too Many Layers

```java
// Avoid excessive wrapping
Notifier notifier = new LoggingNotifier(
    new RetryNotifier(
        new RateLimitedNotifier(
            new AsyncNotifier(
                new FilteredNotifier(
                    new ValidationNotifier(
                        new MetricsNotifier(
                            new TracingNotifier(
                                new EmailNotifier(
                                    new BaseNotifier()
                                )
                            )
                        )
                    )
                )
            )
        )
    )
);
```

### ✅ Solution: Use builder or factory for complex compositions

```java
Notifier notifier = NotifierBuilder.create()
    .withEmail()
    .withLogging()
    .withRetry(3, 1000)
    .withMetrics()
    .build();
```

---

## Refactoring from Inheritance to Composition

### Step 1: Identify the Varying Behavior

```java
// Before: Multiple subclasses for different behaviors
public class PremiumOrder extends Order { /* discount logic */ }
public class SeasonalOrder extends Order { /* seasonal logic */ }
```

### Step 2: Extract Strategy Interface

```java
public interface DiscountStrategy {
    Money apply(Money amount);
}
```

### Step 3: Create Concrete Strategies

```java
public class PremiumDiscount implements DiscountStrategy { /* ... */ }
public class SeasonalDiscount implements DiscountStrategy { /* ... */ }
```

### Step 4: Compose in Main Class

```java
public class Order {
    private DiscountStrategy discountStrategy;
    
    public Money calculateTotal() {
        return discountStrategy.apply(calculateSubtotal());
    }
}
```

### Step 5: Migrate Existing Code

```java
// Old code
Order order = new PremiumOrder();

// New code
Order order = new Order(orderId, customerId, new PremiumDiscount());
```

---

## Design Patterns Using Composition

### Strategy Pattern
- Encapsulate algorithms
- Make them interchangeable
- Example: Discount strategies, payment methods

### Decorator Pattern
- Add responsibilities dynamically
- Wrap objects with additional behavior
- Example: Notification channels, logging

### Composite Pattern
- Treat individual objects and compositions uniformly
- Tree structures
- Example: UI components, file systems

### Bridge Pattern
- Separate abstraction from implementation
- Both can vary independently
- Example: Database drivers, UI themes

---

## When to Use Each Approach

### Use Inheritance When:

✅ True "is-a" relationship exists
✅ Liskov Substitution Principle applies
✅ Shallow hierarchy (1-2 levels)
✅ Behavior is fundamental to the type

### Use Composition When:

✅ Need code reuse without "is-a"
✅ Want runtime flexibility
✅ Need multiple behaviors combined
✅ Want to avoid fragile base class
✅ Following Strategy or Decorator patterns

---

## Related Patterns

- **Strategy Pattern**: Encapsulate algorithms
- **Decorator Pattern**: Add responsibilities dynamically
- **Factory Pattern**: Create composed objects
- **Dependency Injection**: Inject composed dependencies
- **Template Method**: Use with inheritance (sparingly)

---

## Further Reading

- [Design Principles](../../steering/design-principles.md)
- [Dependency Injection Examples](dependency-injection-examples.md)
- [Tell, Don't Ask Examples](tell-dont-ask-examples.md)
- [Gang of Four Design Patterns](https://en.wikipedia.org/wiki/Design_Patterns)
- [Effective Java by Joshua Bloch](https://www.oreilly.com/library/view/effective-java/9780134686097/)

---

## Summary

**Composition over inheritance** is a powerful principle that leads to:

- More flexible designs
- Better testability
- Easier maintenance
- Reduced coupling
- Runtime configurability

Remember: **Favor composition, but don't fear inheritance when it's the right tool for the job.**
