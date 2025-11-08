# Language Support

> **Last Updated**: 2025-10-24  
> **Status**: Active  
> **Owner**: Product & Engineering Team

## Overview

This document details the implementation of multi-language support in the Enterprise E-Commerce Platform, including supported languages, translation workflow, and technical implementation.

## Supported Languages

### Current Languages (Phase 1)

#### English (United States) - en-US

- **Status**: ✅ Active (Default)
- **Launch Date**: 2024-01-01
- **Coverage**: 100%
- **Market**: Primary market
- **Notes**: Base language for all translations

#### Traditional Chinese (Taiwan) - zh-TW

- **Status**: ✅ Active
- **Launch Date**: 2024-06-01
- **Coverage**: 98%
- **Market**: Taiwan, Hong Kong, Macau
- **Notes**: Uses Traditional Chinese characters (繁體中文)
- **Special Considerations**:
  - Vertical text support for certain contexts
  - Traditional character set
  - Taiwan-specific terminology

#### Simplified Chinese (China) - zh-CN

- **Status**: 🚧 In Progress
- **Target Launch**: 2025-Q2
- **Coverage**: 75%
- **Market**: Mainland China
- **Notes**: Uses Simplified Chinese characters (简体中文)
- **Special Considerations**:
  - Content filtering for compliance
  - ICP license required
  - Alipay/WeChat Pay integration
  - Data localization requirements

### Planned Languages (Phase 2-3)

| Language | Locale | Priority | Target | Market Size | Complexity |
|----------|--------|----------|--------|-------------|------------|
| **Japanese** | ja-JP | High | 2025-Q3 | 125M | High (Kanji, Hiragana, Katakana) |
| **Korean** | ko-KR | High | 2025-Q4 | 51M | Medium (Hangul) |
| **Spanish (Spain)** | es-ES | Medium | 2026-Q1 | 47M | Low |
| **Spanish (Latin America)** | es-MX | Medium | 2026-Q2 | 460M | Low |
| **French** | fr-FR | Medium | 2026-Q3 | 67M | Low |
| **German** | de-DE | Low | 2026-Q4 | 83M | Low |

## Translation Workflow

### 1. String Extraction

**Backend (Spring Boot)**:

```bash
# Extract messages from code
./gradlew extractMessages

# Output: src/main/resources/i18n/messages.properties
```

**Frontend (React)**:

```bash
# Extract translation keys
npm run i18n:extract

# Output: public/locales/en-US/*.json
```

### 2. Translation Management

**Using Crowdin (Recommended)**:

1. **Setup**:

   ```bash
   # Install Crowdin CLI
   npm install -g @crowdin/cli
   
   # Initialize project
   crowdin init
   ```

2. **Upload Source Files**:

   ```bash
   crowdin upload sources
   ```

3. **Download Translations**:

   ```bash
   crowdin download
   ```

**Configuration** (`crowdin.yml`):

```yaml
project_id: "your-project-id"
api_token_env: CROWDIN_API_TOKEN

files:

  - source: /public/locales/en-US/**/*.json

    translation: /public/locales/%locale%/**/%original_file_name%
  
  - source: /src/main/resources/i18n/messages.properties

    translation: /src/main/resources/i18n/messages_%locale%.properties
```

### 3. Translation Process

```text
Developer → Add String → Extract → Upload to TMS
                                         ↓
                                    Translator
                                         ↓
                                    Review (QA)
                                         ↓
                                    Download
                                         ↓
                                    Commit → Deploy
```

**Timeline**:

- **Extraction**: Automated (daily)
- **Translation**: 2-3 days
- **Review**: 1-2 days
- **Integration**: Same day
- **Total**: 3-5 days

## Technical Implementation

### Backend Implementation (Spring Boot)

**Message Source Configuration**:

```java
@Configuration
public class I18nConfiguration {
    
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = 
            new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setCacheSeconds(3600);
        return messageSource;
    }
    
    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver();
        resolver.setDefaultLocale(Locale.US);
        resolver.setCookieName("locale");
        resolver.setCookieMaxAge(31536000); // 1 year
        return resolver;
    }
    
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }
}
```

**Usage in Services**:

```java
@Service
public class NotificationService {
    
    private final MessageSource messageSource;
    
    public void sendOrderConfirmation(Order order, Locale locale) {
        String subject = messageSource.getMessage(
            "email.order.confirmation.subject",
            new Object[]{order.getId()},
            locale
        );
        
        String body = messageSource.getMessage(
            "email.order.confirmation.body",
            new Object[]{
                order.getCustomerName(),
                order.getId(),
                formatCurrency(order.getTotalAmount(), locale)
            },
            locale
        );
        
        emailService.send(order.getCustomerEmail(), subject, body);
    }
}
```

### Frontend Implementation (React + i18next)

**i18n Configuration**:

```typescript
// src/i18n/config.ts
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import Backend from 'i18next-http-backend';
import LanguageDetector from 'i18next-browser-languagedetector';

i18n
  .use(Backend)
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    fallbackLng: 'en-US',
    supportedLngs: ['en-US', 'zh-TW', 'zh-CN', 'ja-JP', 'ko-KR'],
    
    detection: {
      order: ['cookie', 'localStorage', 'navigator'],
      caches: ['cookie', 'localStorage'],
    },
    
    backend: {
      loadPath: '/locales/{{lng}}/{{ns}}.json',
    },
    
    interpolation: {
      escapeValue: false,
    },
    
    react: {
      useSuspense: true,
    },
  });

export default i18n;
```

**Usage in Components**:

```typescript
import { useTranslation } from 'react-i18next';

function CheckoutPage() {
  const { t, i18n } = useTranslation('checkout');
  
  const changeLanguage = (lng: string) => {
    i18n.changeLanguage(lng);
  };
  
  return (
    <div>
      <h1>{t('title')}</h1>
      <p>{t('description')}</p>
      
      <select onChange={(e) => changeLanguage(e.target.value)}>
        <option value="en-US">English</option>
        <option value="zh-TW">繁體中文</option>
        <option value="zh-CN">简体中文</option>
      </select>
      
      <button>{t('placeOrder')}</button>
    </div>
  );
}
```

**Translation Files** (`public/locales/en-US/checkout.json`):

```json
{
  "title": "Checkout",
  "description": "Review your order and complete your purchase",
  "placeOrder": "Place Order",
  "orderSummary": "Order Summary",
  "subtotal": "Subtotal",
  "shipping": "Shipping",
  "tax": "Tax",
  "total": "Total",
  "paymentMethod": "Payment Method",
  "shippingAddress": "Shipping Address"
}
```

### Frontend Implementation (Angular)

**Configuration**:

```typescript
// app.module.ts
import { LOCALE_ID, NgModule } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeEn from '@angular/common/locales/en';
import localeZhTW from '@angular/common/locales/zh-Hant';
import localeZhCN from '@angular/common/locales/zh';

registerLocaleData(localeEn, 'en-US');
registerLocaleData(localeZhTW, 'zh-TW');
registerLocaleData(localeZhCN, 'zh-CN');

@NgModule({
  providers: [
    {
      provide: LOCALE_ID,
      useFactory: () => localStorage.getItem('locale') || 'en-US'
    }
  ]
})
export class AppModule { }
```

## Translation Quality Assurance

### Quality Checks

1. **Automated Checks**:
   - Missing translations
   - Placeholder consistency
   - Variable interpolation
   - Character encoding
   - String length (UI fit)

2. **Manual Review**:
   - Native speaker review
   - Context appropriateness
   - Cultural sensitivity
   - Tone and style consistency

3. **Functional Testing**:
   - UI layout in each language
   - Text truncation
   - RTL support (future)
   - Date/number formatting

### Translation Guidelines

**Style Guide**:

- **Tone**: Professional but friendly
- **Formality**: Appropriate to culture
- **Terminology**: Consistent across platform
- **Length**: Keep translations concise
- **Context**: Provide context for translators

**Common Pitfalls to Avoid**:

- ❌ Literal translations
- ❌ Ignoring cultural context
- ❌ Inconsistent terminology
- ❌ Machine translation without review
- ❌ Missing pluralization rules

## Pluralization Rules

Different languages have different pluralization rules:

**English** (2 forms):

```json
{
  "items": {
    "one": "{{count}} item",
    "other": "{{count}} items"
  }
}
```

**Chinese** (1 form):

```json
{
  "items": "{{count}} 個商品"
}
```

**Implementation**:

```typescript
t('items', { count: 1 });  // "1 item" (en-US) / "1 個商品" (zh-TW)
t('items', { count: 5 });  // "5 items" (en-US) / "5 個商品" (zh-TW)
```

## Performance Optimization

### Lazy Loading

**Load translations on demand**:

```typescript
i18n.init({
  backend: {
    loadPath: '/locales/{{lng}}/{{ns}}.json',
  },
  ns: ['common'], // Load only common namespace initially
  defaultNS: 'common',
});

// Load additional namespaces when needed
i18n.loadNamespaces(['checkout', 'product']);
```

### Caching

**Browser caching**:

```typescript
i18n.init({
  backend: {
    loadPath: '/locales/{{lng}}/{{ns}}.json',
    requestOptions: {
      cache: 'default', // Use browser cache
    },
  },
});
```

**CDN caching**:

- Serve translation files from CDN
- Set appropriate cache headers
- Version translation files for cache busting

## Metrics and Monitoring

### Translation Metrics

| Metric | Target | Current | Trend |
|--------|--------|---------|-------|
| **Translation Coverage (en-US)** | 100% | 100% | → |
| **Translation Coverage (zh-TW)** | 100% | 98% | ↗️ |
| **Translation Coverage (zh-CN)** | 90% | 75% | ↗️ |
| **Average Translation Time** | < 5 days | 7 days | ↘️ |
| **Translation Quality Score** | > 4.5/5.0 | 4.3 | ↗️ |
| **Missing Translation Errors** | 0 | 12 | ↘️ |

### Monitoring

**Track missing translations**:

```typescript
i18n.on('missingKey', (lngs, namespace, key, res) => {
  // Log to monitoring system
  logger.warn('Missing translation', {
    languages: lngs,
    namespace,
    key,
    fallback: res
  });
});
```

## Related Documentation

- [Overview](overview.md) - Internationalization Perspective overview
- [Localization Strategy](localization.md) - Content localization approach
- [Cultural Adaptation](cultural-adaptation.md) - Region-specific customization

---

**Next Steps**: Review [Localization Strategy](localization.md) for content localization and formatting guidelines.
