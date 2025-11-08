# Localization Strategy

> **Last Updated**: 2025-10-24  
> **Status**: Active  
> **Owner**: Product & Engineering Team

## Overview

This document details the localization (L10n) strategy for the Enterprise E-Commerce Platform, covering content adaptation, formatting standards, and locale-specific implementations for different markets.

## Localization vs Internationalization

**Internationalization (i18n)**: Designing the system to support multiple locales (done once)  
**Localization (L10n)**: Adapting content and functionality for specific locales (done per market)

This document focuses on the localization aspects - how we adapt our platform for each target market.

## Content Localization Strategy

### Content Categories

#### 1. User Interface Content

**Scope**: All UI labels, buttons, messages, navigation

**Localization Approach**:

- Professional translation
- Native speaker review
- Context-aware translation
- Consistent terminology

**Update Frequency**: Every release

**Example**:

```json
// en-US
{
  "nav.home": "Home",
  "nav.products": "Products",
  "nav.cart": "Shopping Cart",
  "nav.account": "My Account"
}

// zh-TW
{
  "nav.home": "首頁",
  "nav.products": "商品",
  "nav.cart": "購物車",
  "nav.account": "我的帳戶"
}
```

---

#### 2. Product Content

**Scope**: Product names, descriptions, specifications

**Localization Approach**:

- **Product Names**:
  - Keep original brand names
  - Add local phonetic translation if needed
  - Example: "iPhone" → "iPhone" (same) + "愛鳳" (phonetic, optional)
  
- **Descriptions**:
  - Professional translation
  - Adapt to local preferences
  - Maintain SEO keywords
  
- **Specifications**:
  - Translate attribute names
  - Convert units (inches ↔ cm, lbs ↔ kg)
  - Keep technical terms consistent

**Update Frequency**: Weekly batch updates

**Example**:

```text
Product: Wireless Headphones

en-US:
Name: Premium Wireless Headphones
Description: Experience crystal-clear sound with our premium wireless headphones. 
Features active noise cancellation and 30-hour battery life.
Specs: Weight: 8.5 oz, Bluetooth 5.0

zh-TW:
Name: 高級無線耳機
Description: 體驗我們高級無線耳機的清晰音質。
具備主動降噪功能和 30 小時電池續航力。
Specs: 重量: 240 克, 藍牙 5.0
```

---

#### 3. Marketing Content

**Scope**: Banners, promotions, campaigns, email marketing

**Localization Approach**:

- Transcreation (creative adaptation)
- Cultural relevance
- Local holidays and events
- A/B testing for effectiveness

**Update Frequency**: Campaign-based

**Cultural Adaptation Examples**:

| Holiday/Event | US | Taiwan | China |
|---------------|----|----|-------|
| **New Year** | January 1 | Lunar New Year (春節) | Spring Festival (春节) |
| **Major Sales** | Black Friday, Cyber Monday | Double 11 (11/11) | Singles' Day (11/11), 618 |
| **Gift Giving** | Christmas, Valentine's | Moon Festival, Dragon Boat | Mid-Autumn, Qixi |

---

#### 4. Legal Content

**Scope**: Terms of service, privacy policy, return policy

**Localization Approach**:

- Legal translation by certified translators
- Local legal review
- Compliance with local regulations
- Version control for legal changes

**Update Frequency**: As needed (legal changes)

**Requirements by Region**:

| Region | Key Requirements |
|--------|------------------|
| **US** | CCPA compliance, ADA accessibility |
| **Taiwan** | Personal Data Protection Act |
| **China** | Cybersecurity Law, ICP license |
| **EU** | GDPR compliance (future) |

---

#### 5. Help & Support Content

**Scope**: FAQs, help articles, tutorials, error messages

**Localization Approach**:

- Professional translation
- Local customer service review
- Include local examples
- Localized screenshots

**Update Frequency**: Monthly

**Error Message Localization**:

```java
// Backend error messages
messages_en_US.properties:
error.payment.declined=Your payment was declined. Please try another payment method.
error.product.outofstock=This product is currently out of stock.

messages_zh_TW.properties:
error.payment.declined=您的付款被拒絕。請嘗試其他付款方式。
error.product.outofstock=此商品目前缺貨。
```

## Formatting Standards

### Date and Time Formatting

#### Date Formats by Locale

| Locale | Format | Example | Implementation |
|--------|--------|---------|----------------|
| **en-US** | MM/DD/YYYY | 10/24/2025 | `new Intl.DateTimeFormat('en-US')` |
| **zh-TW** | YYYY/MM/DD | 2025/10/24 | `new Intl.DateTimeFormat('zh-TW')` |
| **zh-CN** | YYYY年MM月DD日 | 2025年10月24日 | `new Intl.DateTimeFormat('zh-CN')` |
| **ja-JP** | YYYY年MM月DD日 | 2025年10月24日 | `new Intl.DateTimeFormat('ja-JP')` |
| **ko-KR** | YYYY.MM.DD | 2025.10.24 | `new Intl.DateTimeFormat('ko-KR')` |

#### Time Formats by Locale

| Locale | Format | Example | Notes |
|--------|--------|---------|-------|
| **en-US** | 12-hour (AM/PM) | 2:30 PM | Include AM/PM |
| **zh-TW** | 24-hour | 14:30 | Use 下午 prefix for afternoon |
| **zh-CN** | 24-hour | 14:30 | Use 下午 prefix for afternoon |
| **ja-JP** | 24-hour | 14:30 | Use 午後 prefix for afternoon |
| **ko-KR** | 12-hour (오전/오후) | 오후 2:30 | Use 오전/오후 |

#### Implementation Example

**Backend (Java)**:

```java
@Service
public class DateTimeFormattingService {
    
    public String formatDateTime(LocalDateTime dateTime, Locale locale) {
        DateTimeFormatter formatter = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withLocale(locale);
        return dateTime.format(formatter);
    }
    
    public String formatDate(LocalDate date, Locale locale) {
        DateTimeFormatter formatter = DateTimeFormatter
            .ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(locale);
        return date.format(formatter);
    }
}
```

**Frontend (TypeScript)**:

```typescript
export const formatDate = (date: Date, locale: string): string => {
  return new Intl.DateTimeFormat(locale, {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  }).format(date);
};

export const formatDateTime = (date: Date, locale: string): string => {
  return new Intl.DateTimeFormat(locale, {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date);
};

// Usage
formatDate(new Date(), 'en-US');  // "October 24, 2025"
formatDate(new Date(), 'zh-TW');  // "2025年10月24日"
```

### Number Formatting

#### Number Formats by Locale

| Locale | Decimal Separator | Thousands Separator | Example |
|--------|-------------------|---------------------|---------|
| **en-US** | . (period) | , (comma) | 1,234.56 |
| **zh-TW** | . (period) | , (comma) | 1,234.56 |
| **zh-CN** | . (period) | , (comma) | 1,234.56 |
| **ja-JP** | . (period) | , (comma) | 1,234.56 |
| **ko-KR** | . (period) | , (comma) | 1,234.56 |
| **de-DE** | , (comma) | . (period) | 1.234,56 |
| **fr-FR** | , (comma) | (space) | 1 234,56 |

#### Implementation

```typescript
export const formatNumber = (num: number, locale: string): string => {
  return new Intl.NumberFormat(locale).format(num);
};

// Usage
formatNumber(1234.56, 'en-US');  // "1,234.56"
formatNumber(1234.56, 'de-DE');  // "1.234,56"
```

### Currency Formatting

#### Currency by Region

| Region | Currency | Symbol | Code | Example |
|--------|----------|--------|------|---------|
| **US** | US Dollar | $ | USD | $1,234.56 |
| **Taiwan** | New Taiwan Dollar | NT$ | TWD | NT$1,234.56 |
| **China** | Chinese Yuan | ¥ | CNY | ¥1,234.56 |
| **Japan** | Japanese Yen | ¥ | JPY | ¥1,235 |
| **Korea** | Korean Won | ₩ | KRW | ₩1,235 |
| **Eurozone** | Euro | € | EUR | €1,234.56 |

#### Currency Formatting Rules

**Decimal Places**:

- Most currencies: 2 decimal places
- Japanese Yen (JPY): 0 decimal places
- Korean Won (KRW): 0 decimal places

**Symbol Position**:

- US: Symbol before amount ($100)
- Taiwan: Symbol before amount (NT$100)
- China: Symbol before amount (¥100)
- Europe: Symbol after amount (100€) - varies by country

#### Implementation

**Backend (Java)**:

```java
@Service
public class CurrencyFormattingService {
    
    public String formatCurrency(BigDecimal amount, String currencyCode, Locale locale) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
        formatter.setCurrency(Currency.getInstance(currencyCode));
        return formatter.format(amount);
    }
    
    public Money convertCurrency(Money amount, String targetCurrency) {
        // Integration with currency exchange service
        BigDecimal rate = exchangeRateService.getRate(
            amount.getCurrency(), 
            targetCurrency
        );
        BigDecimal convertedAmount = amount.getAmount().multiply(rate);
        return new Money(convertedAmount, Currency.getInstance(targetCurrency));
    }
}
```

**Frontend (TypeScript)**:

```typescript
export const formatCurrency = (
  amount: number, 
  locale: string, 
  currency: string
): string => {
  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency: currency,
    minimumFractionDigits: getCurrencyDecimals(currency),
    maximumFractionDigits: getCurrencyDecimals(currency)
  }).format(amount);
};

const getCurrencyDecimals = (currency: string): number => {
  const noDeci malCurrencies = ['JPY', 'KRW', 'VND'];
  return noDecimalCurrencies.includes(currency) ? 0 : 2;
};

// Usage
formatCurrency(1234.56, 'en-US', 'USD');  // "$1,234.56"
formatCurrency(1234.56, 'zh-TW', 'TWD');  // "NT$1,234.56"
formatCurrency(1234.56, 'ja-JP', 'JPY');  // "¥1,235"
```

### Address Formatting

#### Address Formats by Region

**United States**:

```json
[Name]
[Street Address]
[City], [State] [ZIP Code]
[Country]

Example:
John Doe
123 Main Street
San Francisco, CA 94102
United States
```

**Taiwan**:

```json
[Postal Code]
[City][District]
[Street Address]
[Name]

Example:
100
台北市中正區
中山南路1號
王小明
```

**China**:

```json
[Name]
[Province][City][District]
[Street Address]
[Postal Code]

Example:
张三
北京市朝阳区
建国路1号
100000
```

**Japan**:

```text
〒[Postal Code]
[Prefecture][City][District]
[Street Address]
[Name]様

Example:
〒100-0001
東京都千代田区千代田
1-1-1
山田太郎様
```

#### Implementation

```typescript
interface Address {
  name: string;
  street: string;
  city: string;
  state?: string;
  postalCode: string;
  country: string;
}

export const formatAddress = (address: Address, locale: string): string => {
  switch (locale) {
    case 'en-US':
      return `${address.name}\n${address.street}\n${address.city}, ${address.state} ${address.postalCode}\n${address.country}`;
    
    case 'zh-TW':
    case 'zh-CN':
      return `${address.postalCode}\n${address.city}${address.state}\n${address.street}\n${address.name}`;
    
    case 'ja-JP':
      return `〒${address.postalCode}\n${address.state}${address.city}\n${address.street}\n${address.name}様`;
    
    default:
      return `${address.name}\n${address.street}\n${address.city}, ${address.postalCode}\n${address.country}`;
  }
};
```

### Phone Number Formatting

#### Phone Formats by Region

| Region | Format | Example | Country Code |
|--------|--------|---------|--------------|
| **US** | (XXX) XXX-XXXX | (415) 555-0123 | +1 |
| **Taiwan** | XXXX-XXXXXX | 0912-345678 | +886 |
| **China** | XXX-XXXX-XXXX | 138-0013-8000 | +86 |
| **Japan** | XXX-XXXX-XXXX | 090-1234-5678 | +81 |
| **Korea** | XXX-XXXX-XXXX | 010-1234-5678 | +82 |

#### Implementation

```typescript
export const formatPhoneNumber = (phone: string, locale: string): string => {
  // Remove all non-numeric characters
  const cleaned = phone.replace(/\D/g, '');
  
  switch (locale) {
    case 'en-US':
      return cleaned.replace(/(\d{3})(\d{3})(\d{4})/, '($1) $2-$3');
    
    case 'zh-TW':
      return cleaned.replace(/(\d{4})(\d{6})/, '$1-$2');
    
    case 'zh-CN':
      return cleaned.replace(/(\d{3})(\d{4})(\d{4})/, '$1-$2-$3');
    
    case 'ja-JP':
    case 'ko-KR':
      return cleaned.replace(/(\d{3})(\d{4})(\d{4})/, '$1-$2-$3');
    
    default:
      return phone;
  }
};
```

## Content Management

### Translation Memory

**Purpose**: Reuse previous translations for consistency and efficiency

**Implementation**:

- Use Translation Management System (TMS) with translation memory
- Store approved translations
- Suggest matches for similar content
- Maintain glossary of terms

**Benefits**:

- Consistency across platform
- Faster translation
- Cost reduction
- Quality improvement

### Glossary Management

**Key Terms to Standardize**:

| English | zh-TW | zh-CN | Notes |
|---------|-------|-------|-------|
| Shopping Cart | 購物車 | 购物车 | Consistent across platform |
| Checkout | 結帳 | 结账 | Payment process |
| Order | 訂單 | 订单 | Purchase order |
| Product | 商品 | 商品 | Item for sale |
| Account | 帳戶 | 账户 | User account |
| Shipping | 運送 | 运送 | Delivery |
| Payment | 付款 | 付款 | Payment method |
| Review | 評價 | 评价 | Product review |

### Content Versioning

**Version Control for Translations**:

```text
translations/
├── en-US/
│   ├── v1.0/
│   ├── v1.1/
│   └── v2.0/
├── zh-TW/
│   ├── v1.0/
│   ├── v1.1/
│   └── v2.0/
└── zh-CN/
    ├── v1.0/
    └── v1.1/
```

**Benefits**:

- Track translation changes
- Rollback if needed
- A/B testing different translations
- Audit trail

## Quality Assurance

### Linguistic QA

**Checklist**:

- [ ] Grammar and spelling correct
- [ ] Terminology consistent
- [ ] Tone appropriate for audience
- [ ] Cultural sensitivity verified
- [ ] No literal translations
- [ ] Context appropriate

### Functional QA

**Checklist**:

- [ ] All strings translated
- [ ] No truncated text
- [ ] UI layout correct
- [ ] Date/time formatting correct
- [ ] Currency formatting correct
- [ ] Number formatting correct
- [ ] Links work in all languages
- [ ] Images appropriate for locale

### Visual QA

**Checklist**:

- [ ] Text fits in UI elements
- [ ] No text overflow
- [ ] Proper line breaks
- [ ] Correct font rendering
- [ ] Icons culturally appropriate
- [ ] Colors culturally appropriate
- [ ] Layout direction correct (LTR/RTL)

## Performance Optimization

### Translation Loading Strategy

**Lazy Loading**:

```typescript
// Load only needed translations
const loadTranslations = async (locale: string, namespace: string) => {
  const translations = await import(`./locales/${locale}/${namespace}.json`);
  return translations.default;
};

// Usage
await loadTranslations('zh-TW', 'checkout');
```

**Caching**:

```typescript
// Cache translations in browser
const CACHE_KEY = 'translations';
const CACHE_VERSION = 'v1.0';

const cacheTranslations = (locale: string, data: any) => {
  localStorage.setItem(
    `${CACHE_KEY}_${locale}_${CACHE_VERSION}`,
    JSON.stringify(data)
  );
};

const getCachedTranslations = (locale: string) => {
  const cached = localStorage.getItem(
    `${CACHE_KEY}_${locale}_${CACHE_VERSION}`
  );
  return cached ? JSON.parse(cached) : null;
};
```

## Metrics and Monitoring

### Localization Metrics

| Metric | Target | Current | Trend |
|--------|--------|---------|-------|
| **Translation Coverage** | 100% | 95% | ↗️ |
| **Translation Accuracy** | > 95% | 93% | ↗️ |
| **Time to Localize** | < 5 days | 7 days | ↘️ |
| **User Satisfaction (Localized)** | > 4.0/5.0 | 3.8 | ↗️ |
| **Locale-Specific Conversion Rate** | Varies | Tracking | - |

### Quality Metrics

- **Translation Quality Score**: Native speaker ratings
- **Error Rate**: Reported translation errors per 1000 strings
- **Consistency Score**: Terminology consistency across platform
- **Completeness**: Percentage of translated strings

## Related Documentation

- [Overview](overview.md) - Internationalization Perspective overview
- [Language Support](language-support.md) - Supported languages and translation workflow
- [Cultural Adaptation](cultural-adaptation.md) - Region-specific customization

---

**Next Steps**: Review [Cultural Adaptation](cultural-adaptation.md) for region-specific customization strategies.
