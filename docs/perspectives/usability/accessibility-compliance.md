# Accessibility Compliance

> **Last Updated**: 2025-11-17  
> **Status**: 🚧 In Progress

## Overview

This document defines the accessibility standards, testing procedures, and remediation strategies for the Enterprise E-Commerce Platform to achieve WCAG 2.1 Level AA compliance. Accessibility ensures that all users, including those with disabilities, can effectively use our platform.

## WCAG 2.1 Level AA Compliance

### Four Principles (POUR)

#### 1. Perceivable

**Information and user interface components must be presentable to users in ways they can perceive.**

**Requirements**:
- Text alternatives for non-text content
- Captions and alternatives for multimedia
- Adaptable content that can be presented in different ways
- Distinguishable content (color contrast, text sizing)

#### 2. Operable

**User interface components and navigation must be operable.**

**Requirements**:
- Keyboard accessible functionality
- Sufficient time to read and use content
- No content that causes seizures
- Navigable and findable content
- Multiple input modalities

#### 3. Understandable

**Information and operation of user interface must be understandable.**

**Requirements**:
- Readable and understandable text
- Predictable functionality
- Input assistance and error prevention
- Clear error messages

#### 4. Robust

**Content must be robust enough to be interpreted by a wide variety of user agents, including assistive technologies.**

**Requirements**:
- Compatible with current and future user agents
- Valid HTML/CSS
- Proper ARIA usage
- Assistive technology support

## Accessibility Standards

### Level A (Must Have)

| Guideline | Requirement | Implementation |
|-----------|-------------|----------------|
| **1.1.1** | Non-text Content | Alt text for all images |
| **1.3.1** | Info and Relationships | Semantic HTML, proper headings |
| **1.3.2** | Meaningful Sequence | Logical reading order |
| **1.3.3** | Sensory Characteristics | Don't rely on shape/color alone |
| **2.1.1** | Keyboard | All functionality via keyboard |
| **2.1.2** | No Keyboard Trap | Users can navigate away |
| **2.2.1** | Timing Adjustable | User control over time limits |
| **2.2.2** | Pause, Stop, Hide | Control over moving content |
| **2.3.1** | Three Flashes | No flashing content |
| **2.4.1** | Bypass Blocks | Skip navigation links |
| **2.4.2** | Page Titled | Descriptive page titles |
| **2.4.3** | Focus Order | Logical focus order |
| **2.4.4** | Link Purpose | Clear link text |
| **3.1.1** | Language of Page | HTML lang attribute |
| **3.2.1** | On Focus | No unexpected changes |
| **3.2.2** | On Input | Predictable behavior |
| **3.3.1** | Error Identification | Clear error messages |
| **3.3.2** | Labels or Instructions | Form labels present |
| **4.1.1** | Parsing | Valid HTML |
| **4.1.2** | Name, Role, Value | Proper ARIA usage |

### Level AA (Target)

| Guideline | Requirement | Implementation |
|-----------|-------------|----------------|
| **1.2.4** | Captions (Live) | Live captions for audio |
| **1.2.5** | Audio Description | Audio description for video |
| **1.4.3** | Contrast (Minimum) | 4.5:1 for normal text, 3:1 for large |
| **1.4.4** | Resize Text | Text resizable to 200% |
| **1.4.5** | Images of Text | Use actual text, not images |
| **2.4.5** | Multiple Ways | Multiple navigation methods |
| **2.4.6** | Headings and Labels | Descriptive headings |
| **2.4.7** | Focus Visible | Visible keyboard focus |
| **3.1.2** | Language of Parts | Identify language changes |
| **3.2.3** | Consistent Navigation | Consistent nav across pages |
| **3.2.4** | Consistent Identification | Consistent component identification |
| **3.3.3** | Error Suggestion | Provide error correction suggestions |
| **3.3.4** | Error Prevention | Confirmation for legal/financial |

## Automated Testing Framework

### Tools and Integration

#### 1. axe-core (Primary Tool)

**Installation**:
```bash
# Consumer Frontend (Angular)
npm install --save-dev @axe-core/cli axe-core

# Management Frontend (Next.js)
npm install --save-dev @axe-core/react axe-core
```

**Angular Integration**:
```typescript
// src/app/app.component.ts
import { Component, OnInit } from '@angular/core';
import * as axe from 'axe-core';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html'
})
export class AppComponent implements OnInit {
  ngOnInit(): void {
    if (!environment.production) {
      this.runAccessibilityAudit();
    }
  }

  private async runAccessibilityAudit(): Promise<void> {
    try {
      const results = await axe.run();
      
      if (results.violations.length > 0) {
        console.group('Accessibility Violations');
        results.violations.forEach(violation => {
          console.error(violation.description);
          console.log('Help:', violation.helpUrl);
          console.log('Nodes:', violation.nodes);
        });
        console.groupEnd();
      }
    } catch (error) {
      console.error('Accessibility audit failed:', error);
    }
  }
}
```

**Next.js Integration**:
```typescript
// src/app/layout.tsx
'use client';

import { useEffect } from 'react';

export default function RootLayout({ children }: { children: React.ReactNode }) {
  useEffect(() => {
    if (process.env.NODE_ENV !== 'production') {
      import('@axe-core/react').then((axe) => {
        axe.default(React, ReactDOM, 1000);
      });
    }
  }, []);

  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
```

#### 2. Pa11y (CI/CD Integration)

**Installation**:
```bash
npm install --save-dev pa11y pa11y-ci
```

**Configuration** (`pa11y-ci.json`):
```json
{
  "defaults": {
    "standard": "WCAG2AA",
    "timeout": 30000,
    "wait": 1000,
    "chromeLaunchConfig": {
      "args": ["--no-sandbox"]
    }
  },
  "urls": [
    "http://localhost:4200/",
    "http://localhost:4200/products",
    "http://localhost:4200/cart",
    "http://localhost:4200/checkout",
    "http://localhost:4200/account"
  ]
}
```

**GitHub Actions Integration**:
```yaml
# .github/workflows/accessibility-tests.yml
name: Accessibility Tests

on:
  pull_request:
    branches: [main, develop]
  push:
    branches: [main]

jobs:
  accessibility:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
          
      - name: Install dependencies
        run: npm ci
        
      - name: Build application
        run: npm run build
        
      - name: Start application
        run: npm start &
        
      - name: Wait for application
        run: npx wait-on http://localhost:4200
        
      - name: Run Pa11y tests
        run: npx pa11y-ci
        
      - name: Upload results
        if: failure()
        uses: actions/upload-artifact@v3
        with:
          name: accessibility-results
          path: pa11y-results/
```

#### 3. Lighthouse CI

**Configuration** (`.lighthouserc.json`):
```json
{
  "ci": {
    "collect": {
      "url": [
        "http://localhost:4200/",
        "http://localhost:4200/products",
        "http://localhost:4200/checkout"
      ],
      "numberOfRuns": 3
    },
    "assert": {
      "preset": "lighthouse:recommended",
      "assertions": {
        "categories:accessibility": ["error", {"minScore": 0.9}],
        "categories:performance": ["warn", {"minScore": 0.85}],
        "categories:best-practices": ["warn", {"minScore": 0.9}]
      }
    },
    "upload": {
      "target": "temporary-public-storage"
    }
  }
}
```

## Manual Testing Procedures

### Screen Reader Testing

#### NVDA (Windows) - Free

**Test Scenarios**:
1. **Navigation**
   - Tab through all interactive elements
   - Verify focus order is logical
   - Ensure all elements are announced

2. **Forms**
   - Verify all form labels are read
   - Test error message announcements
   - Validate required field indicators

3. **Dynamic Content**
   - Test ARIA live regions
   - Verify modal dialog announcements
   - Test loading state announcements

**Testing Checklist**:
- [ ] All images have alt text
- [ ] Form labels are associated with inputs
- [ ] Error messages are announced
- [ ] Focus is managed properly
- [ ] ARIA landmarks are used correctly
- [ ] Dynamic content updates are announced

#### JAWS (Windows) - Commercial

**Test Scenarios**:
1. **Complex Interactions**
   - Test custom widgets (date pickers, autocomplete)
   - Verify table navigation
   - Test accordion and tab panels

2. **E-commerce Specific**
   - Product listing navigation
   - Shopping cart updates
   - Checkout process flow

#### VoiceOver (macOS/iOS) - Built-in

**Test Scenarios**:
1. **Mobile Experience**
   - Touch exploration
   - Swipe navigation
   - Custom gestures

2. **Safari Compatibility**
   - Test on macOS Safari
   - Test on iOS Safari
   - Verify consistent behavior

### Keyboard Navigation Testing

**Test Checklist**:
- [ ] All functionality accessible via keyboard
- [ ] Tab order is logical
- [ ] Focus indicators are visible
- [ ] No keyboard traps
- [ ] Skip links work correctly
- [ ] Escape key closes modals
- [ ] Arrow keys work in custom widgets

**Keyboard Shortcuts**:
- `Tab`: Move forward
- `Shift + Tab`: Move backward
- `Enter`: Activate buttons/links
- `Space`: Toggle checkboxes, activate buttons
- `Escape`: Close modals/dialogs
- `Arrow keys`: Navigate within widgets

### Color Contrast Testing

**Tools**:
- Chrome DevTools (Lighthouse)
- WebAIM Contrast Checker
- Colour Contrast Analyser (CCA)

**Requirements**:
- Normal text (< 18pt): 4.5:1 minimum
- Large text (≥ 18pt or 14pt bold): 3:1 minimum
- UI components and graphics: 3:1 minimum

**Test Checklist**:
- [ ] Text on background meets contrast ratio
- [ ] Link text is distinguishable
- [ ] Button text is readable
- [ ] Form inputs have sufficient contrast
- [ ] Icons and graphics meet requirements
- [ ] Focus indicators are visible

## Common Accessibility Issues and Fixes

### Issue 1: Missing Alt Text

**Problem**:
```html
<img src="product.jpg">
```

**Solution**:
```html
<!-- Decorative image -->
<img src="decorative.jpg" alt="" role="presentation">

<!-- Informative image -->
<img src="product.jpg" alt="Wireless Bluetooth Headphones - Black">

<!-- Functional image (button) -->
<button>
  <img src="cart-icon.svg" alt="Add to cart">
</button>
```

### Issue 2: Poor Form Labels

**Problem**:
```html
<input type="text" placeholder="Email">
```

**Solution**:
```html
<!-- Visible label -->
<label for="email">Email Address</label>
<input type="email" id="email" name="email" required>

<!-- Hidden label (if design requires) -->
<label for="search" class="sr-only">Search products</label>
<input type="search" id="search" placeholder="Search...">
```

### Issue 3: Insufficient Color Contrast

**Problem**:
```css
.button {
  background: #6c757d; /* Gray */
  color: #ffffff; /* White */
  /* Contrast ratio: 4.47:1 - Fails for normal text */
}
```

**Solution**:
```css
.button {
  background: #495057; /* Darker gray */
  color: #ffffff; /* White */
  /* Contrast ratio: 7.48:1 - Passes AA */
}
```

### Issue 4: Keyboard Trap

**Problem**:
```typescript
// Modal that traps focus
openModal() {
  this.modalOpen = true;
  // Focus gets stuck in modal
}
```

**Solution**:
```typescript
openModal() {
  this.modalOpen = true;
  this.trapFocus();
  
  // Store last focused element
  this.lastFocusedElement = document.activeElement;
}

closeModal() {
  this.modalOpen = false;
  
  // Return focus to last focused element
  if (this.lastFocusedElement) {
    this.lastFocusedElement.focus();
  }
}

trapFocus() {
  const modal = document.querySelector('.modal');
  const focusableElements = modal.querySelectorAll(
    'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
  );
  
  const firstElement = focusableElements[0];
  const lastElement = focusableElements[focusableElements.length - 1];
  
  modal.addEventListener('keydown', (e) => {
    if (e.key === 'Tab') {
      if (e.shiftKey && document.activeElement === firstElement) {
        e.preventDefault();
        lastElement.focus();
      } else if (!e.shiftKey && document.activeElement === lastElement) {
        e.preventDefault();
        firstElement.focus();
      }
    }
    
    if (e.key === 'Escape') {
      this.closeModal();
    }
  });
}
```

### Issue 5: Missing ARIA Labels

**Problem**:
```html
<button>
  <svg><!-- Icon --></svg>
</button>
```

**Solution**:
```html
<!-- Option 1: aria-label -->
<button aria-label="Close dialog">
  <svg aria-hidden="true"><!-- Icon --></svg>
</button>

<!-- Option 2: aria-labelledby -->
<button aria-labelledby="close-label">
  <svg aria-hidden="true"><!-- Icon --></svg>
  <span id="close-label" class="sr-only">Close dialog</span>
</button>
```

## ARIA Best Practices

### ARIA Landmarks

```html
<header role="banner">
  <nav role="navigation" aria-label="Main navigation">
    <!-- Navigation links -->
  </nav>
</header>

<main role="main">
  <article role="article">
    <!-- Main content -->
  </article>
  
  <aside role="complementary">
    <!-- Sidebar content -->
  </aside>
</main>

<footer role="contentinfo">
  <!-- Footer content -->
</footer>
```

### ARIA Live Regions

```html
<!-- Polite: Announce when user is idle -->
<div role="status" aria-live="polite" aria-atomic="true">
  Item added to cart
</div>

<!-- Assertive: Announce immediately -->
<div role="alert" aria-live="assertive" aria-atomic="true">
  Error: Payment failed. Please try again.
</div>

<!-- Off: Don't announce -->
<div aria-live="off">
  <!-- Content that shouldn't be announced -->
</div>
```

### ARIA States and Properties

```html
<!-- Expanded/Collapsed -->
<button aria-expanded="false" aria-controls="menu">
  Menu
</button>
<div id="menu" hidden>
  <!-- Menu content -->
</div>

<!-- Selected -->
<div role="tablist">
  <button role="tab" aria-selected="true" aria-controls="panel1">
    Tab 1
  </button>
  <button role="tab" aria-selected="false" aria-controls="panel2">
    Tab 2
  </button>
</div>

<!-- Disabled -->
<button aria-disabled="true">
  Submit (disabled)
</button>

<!-- Required -->
<input type="text" aria-required="true" aria-invalid="false">

<!-- Busy -->
<div aria-busy="true">
  Loading...
</div>
```

## Accessibility Testing Checklist

### Pre-Development

- [ ] Review WCAG 2.1 AA guidelines
- [ ] Use accessible component library
- [ ] Plan keyboard navigation
- [ ] Design with sufficient color contrast
- [ ] Include accessibility in user stories

### During Development

- [ ] Use semantic HTML
- [ ] Add proper ARIA attributes
- [ ] Implement keyboard navigation
- [ ] Test with screen reader
- [ ] Run automated tests (axe-core)
- [ ] Verify color contrast
- [ ] Test focus management

### Pre-Release

- [ ] Complete manual accessibility audit
- [ ] Test with multiple screen readers
- [ ] Verify keyboard-only navigation
- [ ] Run Pa11y CI tests
- [ ] Check Lighthouse accessibility score
- [ ] Document known issues
- [ ] Create remediation plan

### Post-Release

- [ ] Monitor accessibility metrics
- [ ] Collect user feedback
- [ ] Address reported issues
- [ ] Conduct quarterly audits
- [ ] Update accessibility documentation
- [ ] Train team on new patterns

## Accessibility Metrics

### Automated Test Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| axe-core Pass Rate | 100% | Automated tests |
| Pa11y Pass Rate | 100% | CI/CD pipeline |
| Lighthouse Accessibility Score | ≥ 90 | Lighthouse CI |
| Critical Violations | 0 | axe-core |
| Serious Violations | 0 | axe-core |

### Manual Test Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| WCAG 2.1 AA Compliance | 100% | Manual audit |
| Screen Reader Compatibility | 100% | NVDA, JAWS, VoiceOver |
| Keyboard Navigation | 100% functional | Manual testing |
| Color Contrast Compliance | 100% | Manual verification |

### User Experience Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Assistive Technology User Satisfaction | > 4.0/5 | User surveys |
| Task Completion Rate (AT users) | > 90% | User testing |
| Support Tickets (Accessibility) | < 5/month | Support system |

## Remediation Process

### Priority Levels

**Critical (P0)** - Fix immediately:
- Blocks core functionality
- Prevents task completion
- Legal compliance risk

**High (P1)** - Fix within 1 week:
- Significant usability impact
- Affects multiple users
- WCAG Level A violation

**Medium (P2)** - Fix within 1 month:
- Moderate usability impact
- WCAG Level AA violation
- Workaround available

**Low (P3)** - Fix in next release:
- Minor usability impact
- Enhancement opportunity
- Best practice improvement

### Remediation Workflow

1. **Identify**: Automated or manual testing
2. **Triage**: Assign priority level
3. **Document**: Create detailed issue report
4. **Assign**: Assign to development team
5. **Fix**: Implement solution
6. **Verify**: Re-test with original method
7. **Deploy**: Release to production
8. **Monitor**: Track for regression

## Training and Resources

### Team Training

**Developers**:
- WCAG 2.1 guidelines overview
- Semantic HTML and ARIA
- Keyboard navigation patterns
- Screen reader basics
- Automated testing tools

**Designers**:
- Accessible design principles
- Color contrast requirements
- Focus indicator design
- Accessible typography
- Inclusive design patterns

**QA Engineers**:
- Manual accessibility testing
- Screen reader testing
- Keyboard navigation testing
- Automated testing tools
- Issue documentation

### Resources

**Guidelines and Standards**:
- [WCAG 2.1 Quick Reference](https://www.w3.org/WAI/WCAG21/quickref/)
- [ARIA Authoring Practices Guide](https://www.w3.org/WAI/ARIA/apg/)
- [WebAIM Resources](https://webaim.org/resources/)

**Tools**:
- [axe DevTools](https://www.deque.com/axe/devtools/)
- [WAVE Browser Extension](https://wave.webaim.org/extension/)
- [Colour Contrast Analyser](https://www.tpgi.com/color-contrast-checker/)

**Testing**:
- [NVDA Screen Reader](https://www.nvaccess.org/)
- [Pa11y](https://pa11y.org/)
- [Lighthouse](https://developers.google.com/web/tools/lighthouse)

## Related Documentation

- [Overview](overview.md) - Usability Perspective overview
- [Real User Monitoring](real-user-monitoring.md) - RUM implementation
- [User Journey Optimization](user-journey-optimization.md) - Journey mapping
- [Accessibility Perspective](../accessibility/README.md) - Detailed accessibility guidelines

## References

- [WCAG 2.1 Guidelines](https://www.w3.org/WAI/WCAG21/Understanding/)
- [Section 508 Standards](https://www.section508.gov/)
- [EN 301 549 (EU)](https://www.etsi.org/deliver/etsi_en/301500_301599/301549/03.02.01_60/en_301549v030201p.pdf)

---

**Implementation Status**: 🚧 Specification Complete - Implementation Pending  
**Target Completion**: 2025-12-08  
**WCAG Compliance Target**: Level AA (100%)
