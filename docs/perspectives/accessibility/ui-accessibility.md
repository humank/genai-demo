# UI Accessibility

> **Last Updated**: 2025-10-24  
> **Status**: Active  
> **Owner**: UX & Frontend Team

## Overview

This document provides detailed guidelines for implementing accessible user interfaces that comply with WCAG 2.1 Level AA standards. All frontend developers must follow these guidelines when building or modifying UI components.

## WCAG 2.1 AA Compliance

### Perceivable

#### 1.1 Text Alternatives

**Requirement**: Provide text alternatives for non-text content.

```tsx
// ✅ Good: Descriptive alt text
<img 
  src="/products/laptop.jpg" 
  alt="Silver laptop with 15-inch display, open at 90-degree angle"
/>

// ❌ Bad: Generic or missing alt text
<img src="/products/laptop.jpg" alt="image" />
<img src="/products/laptop.jpg" />

// ✅ Good: Decorative images
<img src="/decorative-line.svg" alt="" role="presentation" />

// ✅ Good: Complex images with detailed description
<img 
  src="/sales-chart.png" 
  alt="Sales chart showing 25% increase in Q4"
  aria-describedby="chart-description"
/>
<div id="chart-description" className="sr-only">
  Detailed description: Sales increased from $1M in Q3 to $1.25M in Q4...
</div>
```

#### 1.3 Adaptable

**Requirement**: Create content that can be presented in different ways without losing information.

```tsx
// ✅ Good: Semantic HTML
<nav aria-label="Main navigation">
  <ul>
    <li><a href="/products">Products</a></li>
    <li><a href="/cart">Cart</a></li>
  </ul>
</nav>

<main>
  <h1>Product Catalog</h1>
  <section>
    <h2>Featured Products</h2>
    <article>
      <h3>Laptop Pro</h3>
      <p>High-performance laptop...</p>
    </article>
  </section>
</main>

// ❌ Bad: Non-semantic HTML
<div class="nav">
  <div class="nav-item">Products</div>
  <div class="nav-item">Cart</div>
</div>
```

#### 1.4 Distinguishable

**Color Contrast Requirements**:
- Normal text: 4.5:1 minimum
- Large text (18pt+): 3:1 minimum
- UI components: 3:1 minimum

```css
/* ✅ Good: Sufficient contrast */
.button-primary {
  background-color: #0066CC; /* Blue */
  color: #FFFFFF; /* White */
  /* Contrast ratio: 7.7:1 */
}

.text-body {
  color: #333333; /* Dark gray */
  background-color: #FFFFFF; /* White */
  /* Contrast ratio: 12.6:1 */
}

/* ❌ Bad: Insufficient contrast */
.button-secondary {
  background-color: #CCCCCC; /* Light gray */
  color: #FFFFFF; /* White */
  /* Contrast ratio: 1.6:1 - FAILS */
}
```

### Operable

#### 2.1 Keyboard Accessible

**Requirement**: All functionality available via keyboard.

```tsx
// ✅ Good: Keyboard accessible dropdown
function Dropdown() {
  const [isOpen, setIsOpen] = useState(false);
  const [focusedIndex, setFocusedIndex] = useState(-1);
  
  const handleKeyDown = (e: KeyboardEvent) => {
    switch (e.key) {
      case 'Enter':
      case ' ':
        setIsOpen(!isOpen);
        break;
      case 'Escape':
        setIsOpen(false);
        break;
      case 'ArrowDown':
        e.preventDefault();
        setFocusedIndex(prev => Math.min(prev + 1, items.length - 1));
        break;
      case 'ArrowUp':
        e.preventDefault();
        setFocusedIndex(prev => Math.max(prev - 1, 0));
        break;
    }
  };
  
  return (
    <div className="dropdown">
      <button
        onClick={() => setIsOpen(!isOpen)}
        onKeyDown={handleKeyDown}
        aria-expanded={isOpen}
        aria-haspopup="listbox"
      >
        Select Option
      </button>
      {isOpen && (
        <ul role="listbox" aria-label="Options">
          {items.map((item, index) => (
            <li
              key={item.id}
              role="option"
              aria-selected={index === focusedIndex}
              tabIndex={0}
            >
              {item.label}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
```

#### 2.4 Navigable

**Skip Links**:
```tsx
// ✅ Good: Skip to main content
<a href="#main-content" className="skip-link">
  Skip to main content
</a>

<style>{`
  .skip-link {
    position: absolute;
    top: -40px;
    left: 0;
    background: #000;
    color: #fff;
    padding: 8px;
    z-index: 100;
  }
  
  .skip-link:focus {
    top: 0;
  }
`}</style>
```

**Focus Management**:
```tsx
// ✅ Good: Focus management in modal
function Modal({ isOpen, onClose, children }) {
  const modalRef = useRef<HTMLDivElement>(null);
  const previousFocusRef = useRef<HTMLElement | null>(null);
  
  useEffect(() => {
    if (isOpen) {
      // Save current focus
      previousFocusRef.current = document.activeElement as HTMLElement;
      
      // Focus modal
      modalRef.current?.focus();
      
      // Trap focus within modal
      const handleTab = (e: KeyboardEvent) => {
        if (e.key === 'Tab') {
          const focusableElements = modalRef.current?.querySelectorAll(
            'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
          );
          // Focus trap logic...
        }
      };
      
      document.addEventListener('keydown', handleTab);
      return () => document.removeEventListener('keydown', handleTab);
    } else {
      // Restore focus
      previousFocusRef.current?.focus();
    }
  }, [isOpen]);
  
  return isOpen ? (
    <div
      ref={modalRef}
      role="dialog"
      aria-modal="true"
      aria-labelledby="modal-title"
      tabIndex={-1}
    >
      {children}
    </div>
  ) : null;
}
```

### Understandable

#### 3.2 Predictable

**Consistent Navigation**:
```tsx
// ✅ Good: Consistent navigation across pages
<nav aria-label="Main navigation">
  <ul>
    <li><Link href="/">Home</Link></li>
    <li><Link href="/products">Products</Link></li>
    <li><Link href="/cart">Cart</Link></li>
    <li><Link href="/account">Account</Link></li>
  </ul>
</nav>
```

#### 3.3 Input Assistance

**Form Validation**:
```tsx
// ✅ Good: Accessible form with validation
function CheckoutForm() {
  const [errors, setErrors] = useState<Record<string, string>>({});
  
  return (
    <form onSubmit={handleSubmit} noValidate>
      <div className="form-group">
        <label htmlFor="email">
          Email Address <span aria-label="required">*</span>
        </label>
        <input
          id="email"
          type="email"
          required
          aria-required="true"
          aria-invalid={!!errors.email}
          aria-describedby={errors.email ? "email-error" : undefined}
        />
        {errors.email && (
          <div id="email-error" className="error" role="alert">
            <span aria-hidden="true">⚠️</span> {errors.email}
          </div>
        )}
      </div>
      
      <button type="submit">
        Complete Purchase
      </button>
    </form>
  );
}
```

### Robust

#### 4.1 Compatible

**Valid HTML and ARIA**:
```tsx
// ✅ Good: Proper ARIA usage
<button
  aria-label="Add to cart"
  aria-pressed={isInCart}
  onClick={handleAddToCart}
>
  <ShoppingCartIcon aria-hidden="true" />
  {isInCart ? 'In Cart' : 'Add to Cart'}
</button>

// ✅ Good: Custom component with proper roles
<div
  role="tablist"
  aria-label="Product information"
>
  <button
    role="tab"
    aria-selected={activeTab === 'description'}
    aria-controls="description-panel"
    id="description-tab"
  >
    Description
  </button>
  <div
    role="tabpanel"
    id="description-panel"
    aria-labelledby="description-tab"
    hidden={activeTab !== 'description'}
  >
    Product description content...
  </div>
</div>
```

## Screen Reader Support

### ARIA Live Regions

```tsx
// ✅ Good: Announce dynamic content changes
function ShoppingCart() {
  const [itemCount, setItemCount] = useState(0);
  const [announcement, setAnnouncement] = useState('');
  
  const addItem = (item: Product) => {
    setItemCount(prev => prev + 1);
    setAnnouncement(`${item.name} added to cart. Cart now has ${itemCount + 1} items.`);
  };
  
  return (
    <>
      <div aria-live="polite" aria-atomic="true" className="sr-only">
        {announcement}
      </div>
      
      <button onClick={() => addItem(product)}>
        Add to Cart
      </button>
      
      <div aria-label="Shopping cart">
        <span aria-live="polite">{itemCount} items</span>
      </div>
    </>
  );
}
```

### Screen Reader Only Content

```css
/* Screen reader only class */
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border-width: 0;
}

.sr-only-focusable:focus {
  position: static;
  width: auto;
  height: auto;
  padding: inherit;
  margin: inherit;
  overflow: visible;
  clip: auto;
  white-space: normal;
}
```

## Keyboard Navigation

### Focus Indicators

```css
/* ✅ Good: Visible focus indicators */
*:focus {
  outline: 2px solid #0066CC;
  outline-offset: 2px;
}

button:focus {
  outline: 2px solid #0066CC;
  outline-offset: 2px;
  box-shadow: 0 0 0 4px rgba(0, 102, 204, 0.2);
}

/* ❌ Bad: Removing focus indicators */
*:focus {
  outline: none; /* NEVER DO THIS */
}
```

### Tab Order

```tsx
// ✅ Good: Logical tab order
<form>
  <input type="text" name="firstName" tabIndex={1} />
  <input type="text" name="lastName" tabIndex={2} />
  <input type="email" name="email" tabIndex={3} />
  <button type="submit" tabIndex={4}>Submit</button>
</form>

// ✅ Better: Natural tab order (no tabIndex needed)
<form>
  <input type="text" name="firstName" />
  <input type="text" name="lastName" />
  <input type="email" name="email" />
  <button type="submit">Submit</button>
</form>
```

## Responsive and Adaptive Design

### Text Resizing

```css
/* ✅ Good: Relative units */
html {
  font-size: 16px; /* Base size */
}

body {
  font-size: 1rem; /* 16px */
}

h1 {
  font-size: 2.5rem; /* 40px */
}

.button {
  padding: 0.75rem 1.5rem;
  font-size: 1rem;
}

/* ❌ Bad: Fixed pixel sizes */
body {
  font-size: 14px;
}

h1 {
  font-size: 32px;
}
```

### Responsive Breakpoints

```css
/* Mobile-first approach */
.container {
  padding: 1rem;
}

@media (min-width: 768px) {
  .container {
    padding: 2rem;
  }
}

@media (min-width: 1024px) {
  .container {
    padding: 3rem;
  }
}
```

## Testing Checklist

### Automated Testing

```typescript
// Example: axe-core integration
import { axe, toHaveNoViolations } from 'jest-axe';

expect.extend(toHaveNoViolations);

describe('ProductCard', () => {
  it('should have no accessibility violations', async () => {
    const { container } = render(<ProductCard product={mockProduct} />);
    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });
});
```

### Manual Testing Checklist

- [ ] All interactive elements are keyboard accessible
- [ ] Focus indicators are visible
- [ ] Skip links work correctly
- [ ] Screen reader announces content correctly
- [ ] Color contrast meets WCAG AA standards
- [ ] Text can be resized to 200%
- [ ] Forms have proper labels and error messages
- [ ] Images have appropriate alt text
- [ ] Videos have captions
- [ ] No keyboard traps exist

---

**Related Documents**:
- [Overview](overview.md) - Accessibility perspective introduction
- [API Usability](api-usability.md) - API accessibility
- [Documentation](documentation.md) - Documentation clarity
