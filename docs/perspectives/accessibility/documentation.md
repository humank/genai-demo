# Documentation Clarity

> **Last Updated**: 2025-10-24  
> **Status**: Active  
> **Owner**: Documentation & Technical Writing Team

## Overview

This document defines the standards for creating clear, accessible, and effective documentation for the Enterprise E-Commerce Platform. Good documentation is essential for user adoption, developer productivity, and reducing support burden.

## Documentation Principles

### Core Principles

1. **Clarity**: Write in plain language, avoid jargon
2. **Completeness**: Cover all necessary information
3. **Accuracy**: Keep information up-to-date and correct
4. **Accessibility**: Make documentation usable by everyone
5. **Discoverability**: Make information easy to find
6. **Actionability**: Provide clear next steps

### Documentation Goals

| Goal | Target | Measurement |
|------|--------|-------------|
| **Documentation Satisfaction** | > 4.5/5.0 | User survey |
| **Time to Find Information** | < 2 minutes | User testing |
| **Documentation Coverage** | 100% of features | Content audit |
| **Documentation Freshness** | < 30 days old | Last updated date |
| **Search Success Rate** | > 90% | Search analytics |

---

## Writing Style Guide

### Plain Language

**Use simple, clear language**:

```text
✅ GOOD:
"Click the Save button to save your changes."
"The API returns an error if the email format is invalid."
"You can filter products by price, category, or brand."

❌ BAD:
"Utilize the persistence mechanism to commit modifications."
"The API will respond with an error condition in the event that the electronic mail address format fails validation."
"Product filtration can be accomplished via price point, categorical classification, or manufacturer designation."
```

### Active Voice

**Use active voice instead of passive voice**:

```text
✅ GOOD (Active):
"The system sends a confirmation email."
"You can update your profile at any time."
"The API validates the request before processing."

❌ BAD (Passive):
"A confirmation email is sent by the system."
"Your profile can be updated at any time."
"The request is validated before being processed."
```

### Present Tense

**Use present tense for current actions**:

```text
✅ GOOD:
"The button opens a dialog."
"The API returns a JSON response."
"The system validates the input."

❌ BAD:
"The button will open a dialog."
"The API will return a JSON response."
"The system will validate the input."
```

### Second Person

**Address the reader directly**:

```text
✅ GOOD:
"You can create an order by sending a POST request."
"To update your profile, click the Edit button."
"Your API key is displayed in the dashboard."

❌ BAD:
"One can create an order by sending a POST request."
"To update the profile, the user should click the Edit button."
"The API key is displayed in the dashboard."
```

---

## Document Structure

### Standard Document Template

```markdown
# [Document Title]

> **Last Updated**: YYYY-MM-DD  
> **Status**: [Draft | Active | Deprecated]  
> **Owner**: [Team Name]

## Overview

[Brief description of what this document covers - 2-3 sentences]

## Prerequisites

[What the reader needs to know or have before reading this document]

- Prerequisite 1
- Prerequisite 2

## [Main Content Sections]

### [Section 1]

[Content with examples]

### [Section 2]

[Content with examples]

## Examples

### Example 1: [Scenario]

[Complete, working example]

### Example 2: [Scenario]

[Complete, working example]

## Troubleshooting

### Problem 1

**Symptoms**: [What the user sees]
**Cause**: [Why it happens]
**Solution**: [How to fix it]

## Related Documentation

- [Link to related doc 1]
- [Link to related doc 2]

## Changelog

| Date | Change | Author |
|------|--------|--------|
| YYYY-MM-DD | Initial version | Name |
```

### Heading Hierarchy

**Use proper heading levels**:

```markdown
# H1: Document Title (only one per document)

## H2: Major Sections

### H3: Subsections

#### H4: Sub-subsections

##### H5: Rarely needed

###### H6: Almost never needed
```

**Don't skip levels**:

```markdown
✅ GOOD:
# Title
## Section
### Subsection

❌ BAD:
# Title
### Subsection (skipped H2)
```

---

## Content Guidelines

### Introductions

**Start with a clear overview**:

```markdown
✅ GOOD:
# Authentication

This guide explains how to authenticate API requests using Bearer tokens. 
You'll learn how to obtain a token, include it in requests, and handle 
authentication errors.

## What You'll Learn

- How to obtain an API token
- How to include the token in requests
- How to refresh expired tokens
- How to handle authentication errors

❌ BAD:
# Authentication

Authentication is important for security.
```

### Instructions

**Write clear, numbered steps**:

```markdown
✅ GOOD:
## Creating Your First Order

Follow these steps to create an order:

1. **Obtain an API token**
   
   Send a POST request to `/auth/token` with your credentials:
   
   ```bash
   curl -X POST https://api.example.com/auth/token \
     -H "Content-Type: application/json" \
     -d '{"username": "your-username", "password": "your-password"}'
   ```

2. **Prepare the order data**

   Create a JSON object with the order details:

   ```json
   {
     "customerId": "cust_123",
     "items": [
       {
         "productId": "prod_001",
         "quantity": 1
       }
     ]
   }
   ```

3. **Send the create order request**

   Use the token from step 1 to authenticate:

   ```bash
   curl -X POST https://api.example.com/api/v1/orders \
     -H "Authorization: Bearer YOUR_TOKEN" \
     -H "Content-Type: application/json" \
     -d @order.json
   ```

4. **Verify the response**

   You should receive a 201 Created response with the order details.

❌ BAD:

## Creating Your First Order

You need to get a token and then create an order with it.

```

### Code Examples

**Provide complete, working examples**:

```markdown
✅ GOOD:
### Example: Creating an Order

```javascript
const axios = require('axios');

async function createOrder() {
  try {
    // Authenticate
    const authResponse = await axios.post(
      'https://api.example.com/auth/token',
      {
        username: 'your-username',
        password: 'your-password'
      }
    );
    
    const token = authResponse.data.token;
    
    // Create order
    const orderResponse = await axios.post(
      'https://api.example.com/api/v1/orders',
      {
        customerId: 'cust_123',
        items: [
          {
            productId: 'prod_001',
            quantity: 1
          }
        ]
      },
      {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      }
    );
    
    console.log('Order created:', orderResponse.data);
    return orderResponse.data;
  } catch (error) {
    console.error('Error:', error.response?.data || error.message);
    throw error;
  }
}

// Run the function
createOrder();
```text

**Expected Output**:

```json
{
  "data": {
    "id": "ord_123456",
    "status": "PENDING",
    "totalAmount": {
      "amount": 99.99,
      "currency": "USD"
    }
  }
}
```text

❌ BAD:

### Example

```javascript
// Create order
api.createOrder(data);
```text

```

### Tables

**Use tables for structured data**:

```markdown
✅ GOOD:
## HTTP Status Codes

| Status Code | Meaning | When Used |
|-------------|---------|-----------|
| 200 OK | Success | GET, PUT, PATCH successful |
| 201 Created | Resource created | POST successful |
| 400 Bad Request | Invalid request | Validation error |
| 401 Unauthorized | Authentication required | Missing or invalid token |
| 404 Not Found | Resource not found | Invalid resource ID |

❌ BAD:
## HTTP Status Codes

200 means OK, 201 means created, 400 means bad request, etc.
```

### Lists

**Use lists for related items**:

```markdown
✅ GOOD:
## Prerequisites

Before you begin, ensure you have:

- A valid API key (obtain from the dashboard)
- Node.js 18 or higher installed
- Basic knowledge of REST APIs
- A text editor or IDE

## Features

The API supports:

- **Product Management**: Create, read, update, and delete products
- **Order Processing**: Handle customer orders from creation to fulfillment
- **Inventory Tracking**: Real-time inventory updates
- **Customer Management**: Manage customer profiles and preferences

❌ BAD:
## Prerequisites

You need an API key and Node.js and knowledge of REST APIs.
```

---

## Visual Elements

### Diagrams

**Include diagrams for complex concepts**:

```markdown
## Authentication Flow

The following diagram shows the OAuth 2.0 authentication flow:

```mermaid
sequenceDiagram
    participant Client
    participant API
    participant AuthServer
    
    Client->>AuthServer: 1. Request token
    AuthServer->>Client: 2. Return token
    Client->>API: 3. API request with token
    API->>AuthServer: 4. Validate token
    AuthServer->>API: 5. Token valid
    API->>Client: 6. Return response
```text

**Steps**:

1. Client requests a token from the auth server
2. Auth server returns a JWT token
3. Client includes token in API request
4. API validates token with auth server
5. Auth server confirms token is valid
6. API returns the requested data

```

### Screenshots

**Use screenshots with annotations**:

```markdown
## Finding Your API Key

1. Navigate to the **Settings** page
2. Click on the **API Keys** tab
3. Your API key is displayed in the **Active Keys** section

*Figure 1: The API Keys dashboard showing active keys*

**Note**: Keep your API key secure. Never share it publicly or commit it to version control.
```

### Callouts

**Use callouts for important information**:

```markdown
> **💡 Tip**: Use environment variables to store your API key instead of hardcoding it.

> **⚠️ Warning**: Rate limits apply to all API requests. Exceeding the limit will result in 429 errors.

> **❌ Important**: Never expose your API key in client-side code or public repositories.

> **✅ Best Practice**: Implement exponential backoff when retrying failed requests.

> **📝 Note**: The sandbox environment resets data every 24 hours.
```

---

## Accessibility in Documentation

### Alt Text for Images

```markdown
✅ GOOD:

❌ BAD:
```

### Link Text

```markdown
✅ GOOD:

❌ BAD:
```

### Code Block Labels

```markdown
✅ GOOD:
**Request**:
```bash
curl -X GET https://api.example.com/api/v1/products
```text

**Response**:

```json
{
  "data": [...]
}
```text

❌ BAD:

```bash
curl -X GET https://api.example.com/api/v1/products
```text

```json
{
  "data": [...]
}
```text

```

---

## Documentation Types

### Getting Started Guide

**Purpose**: Help new users get up and running quickly

**Structure**:
```markdown
# Getting Started

## Overview
[What the user will accomplish]

## Prerequisites
[What they need before starting]

## Step 1: [First Step]
[Clear instructions with examples]

## Step 2: [Second Step]
[Clear instructions with examples]

## Step 3: [Third Step]
[Clear instructions with examples]

## Next Steps
[Where to go from here]
```

### API Reference

**Purpose**: Comprehensive reference for all API endpoints

**Structure**:

```markdown
# API Reference

## Endpoints

### Create Order

Creates a new order for the authenticated customer.

**Endpoint**: `POST /api/v1/orders`

**Authentication**: Required (Bearer token)

**Request Headers**:
| Header | Required | Description |
|--------|----------|-------------|
| Authorization | Yes | Bearer token |
| Content-Type | Yes | application/json |
| X-Idempotency-Key | No | Unique key for idempotent requests |

**Request Body**:
```json
{
  "customerId": "string",
  "items": [
    {
      "productId": "string",
      "quantity": "number"
    }
  ]
}
```text

**Response**: `201 Created`

```json
{
  "data": {
    "id": "string",
    "status": "string"
  }
}
```text

**Errors**:

- `400 Bad Request`: Invalid request data
- `401 Unauthorized`: Missing or invalid token
- `409 Conflict`: Insufficient inventory

```

### Tutorial

**Purpose**: Teach a specific task or concept

**Structure**:
```markdown
# Tutorial: Building a Shopping Cart

## What You'll Build
[Description of the end result]

## What You'll Learn

- Concept 1
- Concept 2
- Concept 3

## Prerequisites
[Required knowledge and tools]

## Step 1: [Setup]
[Instructions]

## Step 2: [Core Functionality]
[Instructions]

## Step 3: [Enhancement]
[Instructions]

## Conclusion
[Summary and next steps]

## Full Code
[Complete working code]
```

### How-To Guide

**Purpose**: Solve a specific problem

**Structure**:

```markdown
# How to Handle Rate Limiting

## Problem
[Description of the problem]

## Solution
[Step-by-step solution]

## Example
[Working code example]

## Explanation
[Why this solution works]

## Alternative Approaches
[Other ways to solve the problem]
```

### Troubleshooting Guide

**Purpose**: Help users diagnose and fix problems

**Structure**:

```markdown
# Troubleshooting

## Authentication Errors

### Error: "Invalid token"

**Symptoms**:

- API returns 401 Unauthorized
- Error message: "Invalid token"

**Possible Causes**:

1. Token has expired
2. Token is malformed
3. Token was revoked

**Solutions**:

**If token expired**:

1. Request a new token
2. Implement automatic token refresh

**If token malformed**:

1. Check token format (should be JWT)
2. Ensure no extra spaces or characters

**If token revoked**:

1. Generate a new API key
2. Update your application configuration

**Prevention**:

- Implement token refresh before expiration
- Store tokens securely
- Monitor token expiration times

```

---

## Documentation Maintenance

### Keeping Documentation Current

**Update triggers**:

- Code changes that affect behavior
- New features added
- Bugs fixed that change expected behavior
- API changes
- Deprecations

**Review schedule**:

- **Weekly**: Check for broken links
- **Monthly**: Review most-viewed pages
- **Quarterly**: Comprehensive content audit
- **Annually**: Major documentation refresh

### Version Control

**Track changes**:

```markdown
## Changelog

### 2025-10-24

- Added section on rate limiting
- Updated authentication examples
- Fixed broken links to API reference

### 2025-10-01

- Initial version

```

### Deprecation Notices

```markdown
> **⚠️ Deprecated**: This endpoint is deprecated and will be removed on 2025-12-31.
> 
> 
```

---

## Search and Navigation

### Search Optimization

**Use descriptive titles**:

```markdown
✅ GOOD:
# How to Authenticate API Requests with Bearer Tokens

❌ BAD:
# Authentication
```

**Include keywords**:

```markdown
✅ GOOD:
# Creating Orders: POST /api/v1/orders

This guide explains how to create orders using the REST API. You'll learn
about order creation, order validation, and handling order errors.

❌ BAD:
# Creating Orders

This guide explains the process.
```

### Navigation Structure

**Logical hierarchy**:

```text
Documentation/
├── Getting Started/
│   ├── Quickstart
│   ├── Authentication
│   └── Your First API Call
├── Guides/
│   ├── Orders
│   ├── Products
│   └── Customers
├── API Reference/
│   ├── Orders API
│   ├── Products API
│   └── Customers API
├── Tutorials/
│   ├── Building a Shopping Cart
│   └── Implementing Webhooks
└── Support/
    ├── Troubleshooting
    ├── FAQ
    └── Contact Support
```

---

## Documentation Quality Checklist

### Content Quality

- [ ] Written in plain language
- [ ] Uses active voice
- [ ] Uses present tense
- [ ] Addresses reader directly (you)
- [ ] Free of jargon or jargon is explained
- [ ] Grammar and spelling checked
- [ ] Consistent terminology
- [ ] Accurate and up-to-date

### Structure Quality

- [ ] Clear title and overview
- [ ] Logical heading hierarchy
- [ ] Proper use of lists and tables
- [ ] Code examples are complete and working
- [ ] Images have descriptive alt text
- [ ] Links are descriptive and working
- [ ] Includes troubleshooting section
- [ ] Has related documentation links

### Accessibility Quality

- [ ] Proper heading structure (H1 → H2 → H3)
- [ ] Alt text for all images
- [ ] Descriptive link text
- [ ] Color not used as only indicator
- [ ] Code blocks have language labels
- [ ] Tables have headers
- [ ] Callouts are clearly marked

### Usability Quality

- [ ] Easy to scan (headings, lists, bold)
- [ ] Examples are realistic
- [ ] Prerequisites clearly stated
- [ ] Next steps provided
- [ ] Search-friendly titles and content
- [ ] Mobile-friendly formatting
- [ ] Print-friendly layout

---

## Documentation Tools

### Recommended Tools

| Tool | Purpose | Usage |
|------|---------|-------|
| **Markdown** | Writing format | All documentation |
| **Mermaid** | Diagrams | Flowcharts, sequences |
| **Vale** | Style linting | Automated checks |
| **markdownlint** | Markdown linting | Formatting checks |
| **markdown-link-check** | Link validation | CI/CD pipeline |
| **Grammarly** | Grammar checking | Manual review |

### Automation

```yaml
# .github/workflows/docs-validation.yml
name: Documentation Validation

on:
  pull_request:
    paths:

      - 'docs/**'

jobs:
  validate:
    runs-on: ubuntu-latest
    steps:

      - uses: actions/checkout@v3
      
      - name: Check links

        uses: gaurav-nelson/github-action-markdown-link-check@v1
      
      - name: Lint markdown

        uses: avto-dev/markdown-lint@v1
      
      - name: Check spelling

        uses: rojopolis/spellcheck-github-actions@v0
```

---

**Related Documents**:

- [Overview](overview.md) - Accessibility perspective introduction
- [UI Accessibility](ui-accessibility.md) - User interface accessibility
- [API Usability](api-usability.md) - API design and usability
