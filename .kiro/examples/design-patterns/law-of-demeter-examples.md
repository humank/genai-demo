# Law of Demeter - Detailed Examples

## Principle Overview

The **Law of Demeter** (Principle of Least Knowledge) states that an object should only talk to its immediate friends, not to strangers. This reduces coupling and makes code more maintainable.

## The "One Dot" Rule

A method should only call methods on:
1. The object itself (`this`)
2. Objects passed as parameters
3. Objects created locally
4. Direct component objects (fields)

**Exception**: Fluent APIs and builders are acceptable (e.g., `builder.withName().withEmail().build()`)

---

## Example 1: Address Information

### ❌ Bad: Talking to Strangers

```java
@RestController
public class OrderController {
    
    @GetMapping("/orders/{id}/shipping-city")
    public String getShippingCity(@PathVariable String id) {
        Order order = orderRepository.findById(id).orElseThrow();
        
        // Violation: Chaining through multiple objects
        return order.getCustomer().getAddress().getCity().getName();
        //     ↑ friend    ↑ stranger  ↑ stranger  ↑ stranger
    }
}
```

**Problems:**
- Controller knows about Order's internal structure
- Changes to Customer or Address break the controller
- High coupling across multiple classes
- Hard to test and mock

### ✅ Good: Asking Friends Only

```java
@RestController
public class OrderController {
    
    @GetMapping("/orders/{id}/shipping-city")
    public String getShippingCity(@PathVariable String id) {
        Order order = orderRepository.findById(id).orElseThrow();
        
        // Tell order to get the shipping city
        return order.getShippingCityName();
    }
}

// Order provides a direct method
@AggregateRoot
public class Order {
    private Customer customer;
    private Address shippingAddress;
    
    public String getShippingCityName() {
        return shippingAddress.getCityName();
    }
}

// Address provides a direct method
public class Address {
    private City city;
    
    public String getCityName() {
        return city.getName();
    }
}
```

**Benefits:**
- Controller only talks to Order
- Order encapsulates how to get city name
- Easy to change internal structure
- Low coupling

---

## Example 2: Price Calculation

### ❌ Bad: Deep Navigation

```java
@Service
public class InvoiceService {
    
    public Money calculateInvoiceTotal(Invoice invoice) {
        Money total = Money.ZERO;
        
        // Violation: Navigating deep into object structure
        for (InvoiceLine line : invoice.getLines()) {
            Product product = line.getProduct();
            Money unitPrice = product.getPricing().getCurrentPrice().getAmount();
            int quantity = line.getQuantity();
            
            total = total.add(unitPrice.multiply(quantity));
        }
        
        return total;
    }
}
```

**Problems:**
- Service knows about Product's pricing structure
- Breaks if pricing structure changes
- Calculation logic is external

### ✅ Good: Delegating to Objects

```java
@Service
public class InvoiceService {
    
    public Money calculateInvoiceTotal(Invoice invoice) {
        // Tell invoice to calculate its own total
        return invoice.calculateTotal();
    }
}

// Invoice calculates its own total
public class Invoice {
    private List<InvoiceLine> lines;
    
    public Money calculateTotal() {
        return lines.stream()
            .map(InvoiceLine::calculateSubtotal)
            .reduce(Money.ZERO, Money::add);
    }
}

// InvoiceLine calculates its own subtotal
public class InvoiceLine {
    private Product product;
    private int quantity;
    
    public Money calculateSubtotal() {
        Money unitPrice = product.getCurrentPrice();
        return unitPrice.multiply(quantity);
    }
}

// Product provides a simple interface
public class Product {
    private Pricing pricing;
    
    public Money getCurrentPrice() {
        return pricing.getCurrentAmount();
    }
}

// Pricing encapsulates its structure
public class Pricing {
    private Price currentPrice;
    
    public Money getCurrentAmount() {
        return currentPrice.getAmount();
    }
}
```

**Benefits:**
- Each object handles its own calculations
- Changes to pricing structure don't affect callers
- Clear responsibility chain

---

## Example 3: Notification Preferences

### ❌ Bad: Reaching Through Objects

```java
@Service
public class NotificationService {
    
    public void sendOrderConfirmation(Order order) {
        // Violation: Reaching through multiple objects
        Customer customer = order.getCustomer();
        ContactInfo contactInfo = customer.getContactInfo();
        NotificationPreferences prefs = contactInfo.getNotificationPreferences();
        
        if (prefs.isEmailEnabled()) {
            String email = contactInfo.getEmail().getValue();
            emailService.send(email, "Order Confirmation", buildMessage(order));
        }
        
        if (prefs.isSmsEnabled()) {
            String phone = contactInfo.getPhone().getValue();
            smsService.send(phone, buildShortMessage(order));
        }
    }
}
```

**Problems:**
- Service navigates deep object graph
- Knows about internal structure
- Hard to change notification logic

### ✅ Good: Using Facade Methods

```java
@Service
public class NotificationService {
    
    public void sendOrderConfirmation(Order order) {
        Customer customer = order.getCustomer();
        
        // Ask customer for notification channels
        if (customer.prefersEmailNotifications()) {
            String email = customer.getEmailAddress();
            emailService.send(email, "Order Confirmation", buildMessage(order));
        }
        
        if (customer.prefersSmsNotifications()) {
            String phone = customer.getPhoneNumber();
            smsService.send(phone, buildShortMessage(order));
        }
    }
}

// Customer provides facade methods
@AggregateRoot
public class Customer {
    private ContactInfo contactInfo;
    
    public boolean prefersEmailNotifications() {
        return contactInfo.isEmailNotificationEnabled();
    }
    
    public boolean prefersSmsNotifications() {
        return contactInfo.isSmsNotificationEnabled();
    }
    
    public String getEmailAddress() {
        return contactInfo.getEmailValue();
    }
    
    public String getPhoneNumber() {
        return contactInfo.getPhoneValue();
    }
}

// ContactInfo encapsulates its structure
public class ContactInfo {
    private Email email;
    private Phone phone;
    private NotificationPreferences preferences;
    
    public boolean isEmailNotificationEnabled() {
        return preferences.isEmailEnabled();
    }
    
    public boolean isSmsNotificationEnabled() {
        return preferences.isSmsEnabled();
    }
    
    public String getEmailValue() {
        return email.getValue();
    }
    
    public String getPhoneValue() {
        return phone.getValue();
    }
}
```

**Benefits:**
- Service only talks to Customer
- Customer encapsulates contact details
- Easy to change notification logic

---

## Example 4: Order Validation

### ❌ Bad: Inspecting Internal State

```java
@Service
public class OrderValidationService {
    
    public void validateOrder(Order order) {
        // Violation: Inspecting deep internal state
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            Inventory inventory = product.getInventory();
            
            if (inventory.getAvailableQuantity() < item.getQuantity()) {
                throw new InsufficientInventoryException(
                    "Not enough inventory for: " + product.getName()
                );
            }
            
            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new InactiveProductException(
                    "Product is not active: " + product.getName()
                );
            }
        }
    }
}
```

**Problems:**
- Service knows about Product and Inventory internals
- Validation logic is scattered
- Hard to maintain

### ✅ Good: Delegating Validation

```java
@Service
public class OrderValidationService {
    
    public void validateOrder(Order order) {
        // Tell order to validate itself
        order.validate();
    }
}

// Order validates itself
@AggregateRoot
public class Order {
    private List<OrderItem> items;
    
    public void validate() {
        if (items.isEmpty()) {
            throw new BusinessRuleViolationException("Order must have at least one item");
        }
        
        items.forEach(OrderItem::validate);
    }
}

// OrderItem validates itself
public class OrderItem {
    private Product product;
    private int quantity;
    
    public void validate() {
        product.validateAvailability(quantity);
        product.validateActive();
    }
}

// Product provides validation methods
public class Product {
    private Inventory inventory;
    private ProductStatus status;
    
    public void validateAvailability(int requestedQuantity) {
        if (!inventory.hasAvailableQuantity(requestedQuantity)) {
            throw new InsufficientInventoryException(
                String.format("Not enough inventory for: %s. Requested: %d, Available: %d",
                    name, requestedQuantity, inventory.getAvailableQuantity())
            );
        }
    }
    
    public void validateActive() {
        if (status != ProductStatus.ACTIVE) {
            throw new InactiveProductException("Product is not active: " + name);
        }
    }
}

// Inventory provides query method
public class Inventory {
    private int availableQuantity;
    
    public boolean hasAvailableQuantity(int requested) {
        return availableQuantity >= requested;
    }
    
    public int getAvailableQuantity() {
        return availableQuantity;
    }
}
```

**Benefits:**
- Each object validates its own state
- Validation logic is encapsulated
- Easy to add new validation rules

---

## Example 5: Fluent API (Acceptable Exception)

### ✅ Acceptable: Builder Pattern

```java
// This is acceptable because it's a fluent API
Customer customer = Customer.builder()
    .withName("John Doe")
    .withEmail("john@example.com")
    .withAddress(Address.builder()
        .withStreet("123 Main St")
        .withCity("New York")
        .withZipCode("10001")
        .build())
    .build();

// This is also acceptable
String result = stringBuilder
    .append("Hello")
    .append(" ")
    .append("World")
    .toString();
```

**Why it's acceptable:**
- Builder returns itself for chaining
- It's a deliberate API design pattern
- Doesn't expose internal structure
- Improves readability

---

## Refactoring Strategies

### Strategy 1: Add Facade Methods

```java
// Before
String city = order.getCustomer().getAddress().getCity();

// After
String city = order.getCustomerCity();

// Implementation
public class Order {
    public String getCustomerCity() {
        return customer.getAddressCity();
    }
}

public class Customer {
    public String getAddressCity() {
        return address.getCity();
    }
}
```

### Strategy 2: Move Behavior to Owner

```java
// Before
if (order.getCustomer().getMembershipLevel() == MembershipLevel.PREMIUM) {
    applyPremiumDiscount();
}

// After
if (order.hasCustomerWithPremiumMembership()) {
    applyPremiumDiscount();
}

// Implementation
public class Order {
    public boolean hasCustomerWithPremiumMembership() {
        return customer.isPremiumMember();
    }
}

public class Customer {
    public boolean isPremiumMember() {
        return membershipLevel == MembershipLevel.PREMIUM;
    }
}
```

### Strategy 3: Use Value Objects

```java
// Before
String fullAddress = customer.getAddress().getStreet() + ", " +
                    customer.getAddress().getCity() + ", " +
                    customer.getAddress().getZipCode();

// After
String fullAddress = customer.getFormattedAddress();

// Implementation
public class Customer {
    private Address address;
    
    public String getFormattedAddress() {
        return address.format();
    }
}

public class Address {
    public String format() {
        return String.format("%s, %s, %s", street, city, zipCode);
    }
}
```

---

## Common Violations and Fixes

### Violation 1: Configuration Access

```java
// ❌ Bad
int maxRetries = config.getRetrySettings().getMaxAttempts();

// ✅ Good
int maxRetries = config.getMaxRetryAttempts();
```

### Violation 2: Collection Navigation

```java
// ❌ Bad
Money total = order.getItems().stream()
    .map(item -> item.getPrice())
    .reduce(Money.ZERO, Money::add);

// ✅ Good
Money total = order.calculateTotal();
```

### Violation 3: Nested Conditionals

```java
// ❌ Bad
if (order.getCustomer().getAddress().getCountry().equals("US")) {
    applyUSTax();
}

// ✅ Good
if (order.isShippingToUS()) {
    applyUSTax();
}
```

---

## Quick Reference

### Allowed Method Calls

```java
public class Example {
    private Dependency dependency;  // Field
    
    public void method(Parameter param) {  // Parameter
        // ✅ Allowed
        this.doSomething();           // this
        param.doSomething();          // parameter
        dependency.doSomething();     // field
        
        Local local = new Local();    // local
        local.doSomething();          // local
        
        // ❌ Not allowed
        dependency.getOther().doSomething();  // stranger
        param.getChild().doSomething();       // stranger
    }
}
```

### Benefits

- **Low Coupling**: Objects don't depend on internal structures
- **High Cohesion**: Behavior stays with data
- **Easy Refactoring**: Internal changes don't break clients
- **Better Testing**: Fewer mocks needed

### Trade-offs

- **More Methods**: Need facade methods
- **Potential Duplication**: Similar methods in different classes
- **Learning Curve**: Requires thinking about object responsibilities

---

## Related Principles

- **Tell, Don't Ask**: Tell objects what to do
- **Information Hiding**: Hide internal structure
- **Encapsulation**: Protect object state

## Further Reading

- [Tell, Don't Ask Examples](tell-dont-ask-examples.md)
- [Design Principles](../../steering/design-principles.md)
