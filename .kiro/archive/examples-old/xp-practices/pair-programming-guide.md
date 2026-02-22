# Pair Programming Guide

## Overview

Practical guide for effective pair programming in our development team.

**Related Standards**: [Design Principles](../../steering/design-principles.md)

---

## What is Pair Programming?

Two developers working together at one workstation:
- **Driver**: Writes the code
- **Navigator**: Reviews, thinks ahead, suggests improvements

---

## Benefits

### Code Quality
- Fewer bugs (15-20% reduction)
- Better design decisions
- Immediate code review
- Knowledge sharing

### Team Benefits
- Faster onboarding
- Collective code ownership
- Reduced knowledge silos
- Improved communication

---

## Pairing Styles

### 1. Driver-Navigator (Classic)

**When**: General development, learning

```
Driver:    Types code, focuses on syntax
Navigator: Reviews, thinks strategically, suggests improvements

Switch roles every 25-30 minutes
```

### 2. Ping-Pong Pairing

**When**: TDD development

```
Developer A: Writes failing test (Red)
Developer B: Makes test pass (Green)
Developer A: Refactors (Refactor)
Developer B: Writes next test (Red)
... continue switching
```

**Example**:
```java
// Developer A writes test
@Test
void should_calculate_total_with_discount() {
    Order order = anOrder().withTotal(100).build();
    Customer customer = aPremiumCustomer().build();
    
    BigDecimal total = calculator.calculate(order, customer);
    
    assertThat(total).isEqualByComparingTo("90.00");
}

// Developer B implements
public BigDecimal calculate(Order order, Customer customer) {
    BigDecimal total = order.getTotal();
    if (customer.isPremium()) {
        return total.multiply(new BigDecimal("0.9"));
    }
    return total;
}

// Developer A refactors
public BigDecimal calculate(Order order, Customer customer) {
    return customer.applyDiscount(order.getTotal());
}
```

### 3. Strong-Style Pairing

**When**: Teaching, knowledge transfer

```
Rule: "For an idea to go from your head into the computer,
       it must go through someone else's hands"

Expert:   Navigates, explains concepts
Learner:  Drives, asks questions
```

---

## Best Practices

### Before Pairing

- [ ] Clear goal for the session
- [ ] Comfortable workspace setup
- [ ] Agreed time duration
- [ ] Minimize distractions

### During Pairing

- [ ] Switch roles regularly (25-30 min)
- [ ] Take breaks (5-10 min every hour)
- [ ] Communicate constantly
- [ ] Be respectful and patient
- [ ] Ask questions
- [ ] Explain your thinking

### After Pairing

- [ ] Reflect on what worked
- [ ] Discuss improvements
- [ ] Document decisions
- [ ] Commit code together

---

## Communication Tips

### As Driver

```java
// ✅ GOOD: Think aloud
"I'm going to extract this validation logic into a separate method..."
"Let me add a test for the edge case where the order is empty..."
"I think we should use a value object here for the email..."

// ❌ BAD: Silent coding
// Just typing without explaining
```

### As Navigator

```java
// ✅ GOOD: Constructive suggestions
"What if we extract that into a value object?"
"Should we add a test for the null case?"
"I notice some duplication here, maybe we can refactor?"

// ❌ BAD: Micromanaging
"No, use a different variable name"
"That's wrong, do it this way"
"You're typing too slow"
```

---

## Common Challenges

### Challenge 1: Skill Gap

**Solution**: Use Strong-Style Pairing
- Expert navigates, explains concepts
- Junior drives, learns by doing
- Focus on learning, not speed

### Challenge 2: Personality Conflicts

**Solution**: Set ground rules
- Respect different approaches
- Focus on code, not person
- Take breaks if tension rises
- Rotate pairs regularly

### Challenge 3: Remote Pairing

**Solution**: Use proper tools
- Screen sharing (VS Code Live Share)
- Video call (clear audio/video)
- Shared terminal access
- Regular breaks (more frequent)

---

## Remote Pairing Setup

### Tools

```bash
# VS Code Live Share
# - Real-time collaboration
# - Shared debugging
# - Shared terminal

# Tuple / Pop
# - Low-latency screen sharing
# - Designed for pair programming

# tmux + SSH
# - Terminal-based pairing
# - Lightweight, fast
```

### Best Practices

- [ ] Good microphone and camera
- [ ] Stable internet connection
- [ ] Clear screen sharing
- [ ] More frequent breaks
- [ ] Over-communicate

---

## Pairing Schedule

### Full-Time Pairing

```
09:00 - 10:30  Pair Session 1
10:30 - 10:45  Break
10:45 - 12:15  Pair Session 2
12:15 - 13:15  Lunch
13:15 - 14:45  Pair Session 3
14:45 - 15:00  Break
15:00 - 16:30  Pair Session 4
16:30 - 17:00  Solo time (email, admin)
```

### Part-Time Pairing

```
Morning:   Solo work (research, design)
Afternoon: Pair programming (implementation)

Or rotate:
Mon/Wed/Fri: Pairing
Tue/Thu:     Solo work
```

---

## When to Pair

### Always Pair
- [ ] Complex features
- [ ] Critical bug fixes
- [ ] Onboarding new team members
- [ ] Learning new technology
- [ ] Refactoring legacy code

### Consider Pairing
- [ ] Medium complexity features
- [ ] Code reviews (live)
- [ ] Architecture decisions
- [ ] Performance optimization

### Solo Work
- [ ] Simple, well-understood tasks
- [ ] Research and exploration
- [ ] Documentation
- [ ] Administrative tasks

---

## Measuring Success

### Qualitative Metrics
- Team satisfaction
- Knowledge sharing
- Code quality perception
- Onboarding speed

### Quantitative Metrics
- Defect rate
- Code review time (reduced)
- Time to production
- Test coverage

---

## Example Pairing Session

### Session: Implement Order Submission

**Goal**: Implement order submission with validation

**Duration**: 2 hours

**Approach**: Ping-Pong TDD

```java
// Round 1: Developer A writes test
@Test
void should_submit_order_successfully() {
    Order order = anOrder()
        .withCustomer(aCustomer())
        .withItem(aProduct(), quantity(1))
        .build();
    
    orderService.submitOrder(order.getId());
    
    Order submitted = orderRepository.findById(order.getId()).get();
    assertThat(submitted.getStatus()).isEqualTo(OrderStatus.SUBMITTED);
}

// Developer B implements (minimal)
public void submitOrder(OrderId orderId) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new OrderNotFoundException(orderId));
    order.setStatus(OrderStatus.SUBMITTED);
    orderRepository.save(order);
}

// Round 2: Developer B writes test
@Test
void should_throw_exception_when_order_is_empty() {
    Order order = anOrder()
        .withCustomer(aCustomer())
        .withNoItems()
        .build();
    
    assertThatThrownBy(() -> orderService.submitOrder(order.getId()))
        .isInstanceOf(BusinessRuleViolationException.class)
        .hasMessageContaining("empty order");
}

// Developer A implements
public void submitOrder(OrderId orderId) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new OrderNotFoundException(orderId));
    
    if (order.getItems().isEmpty()) {
        throw new BusinessRuleViolationException("Cannot submit empty order");
    }
    
    order.setStatus(OrderStatus.SUBMITTED);
    orderRepository.save(order);
}

// Developer A refactors
public void submitOrder(OrderId orderId) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new OrderNotFoundException(orderId));
    
    order.submit(); // Move logic to aggregate
    orderRepository.save(order);
}

// Continue with more tests and implementation...
```

---

## Summary

Effective pair programming requires:

1. **Clear roles** - Driver and Navigator
2. **Regular switching** - Every 25-30 minutes
3. **Good communication** - Think aloud, ask questions
4. **Mutual respect** - Different approaches are valid
5. **Proper setup** - Comfortable workspace, good tools

Remember: **Pairing is a skill** - it improves with practice.

---

**Related Documentation**:
- [Simple Design Examples](simple-design-examples.md)
- [Refactoring Guide](refactoring-guide.md)
- [Design Principles](../../steering/design-principles.md)
