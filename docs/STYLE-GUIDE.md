# Documentation Style Guide

> **Last Updated**: 2024-11-09  
> **Owner**: Documentation Team  
> **Status**: Active

## Overview

This style guide defines the writing standards, formatting conventions, and best practices for all Enterprise E-Commerce Platform documentation. Consistent style ensures documentation is clear, professional, and easy to understand.

## Style Philosophy

- **Clarity First**: Clear communication over clever writing
- **Consistency**: Uniform style across all documentation
- **Accessibility**: Understandable by all stakeholders
- **Conciseness**: Say more with fewer words
- **Accuracy**: Technical precision without jargon

---

## Writing Style

### Voice and Tone

#### Active Voice

**Use active voice** for clarity and directness.

✅ **Good**:
- "The system validates the input"
- "Deploy the application using this command"
- "The service processes requests asynchronously"

❌ **Avoid**:
- "The input is validated by the system"
- "The application should be deployed using this command"
- "Requests are processed asynchronously by the service"

#### Present Tense

**Use present tense** for current functionality.

✅ **Good**:
- "The API returns a JSON response"
- "The system stores data in PostgreSQL"
- "Users authenticate with JWT tokens"

❌ **Avoid**:
- "The API will return a JSON response"
- "The system would store data in PostgreSQL"
- "Users would authenticate with JWT tokens"

#### Second Person

**Address the reader directly** using "you".

✅ **Good**:
- "You can configure the timeout value"
- "To deploy the application, you need to..."
- "You should validate input before processing"

❌ **Avoid**:
- "One can configure the timeout value"
- "The developer needs to..."
- "It is recommended to validate input"

### Clarity and Conciseness

#### Be Direct

Get to the point quickly.

✅ **Good**:
- "Use this command to deploy"
- "The API requires authentication"
- "Configure the database connection"

❌ **Avoid**:
- "In order to deploy the application, you should use this command"
- "It is necessary for the API to have authentication"
- "You need to make sure to configure the database connection"

#### Avoid Jargon

Use clear language; explain technical terms when necessary.

✅ **Good**:
- "The system uses optimistic locking to handle concurrent updates"
- "JWT (JSON Web Token) provides stateless authentication"

❌ **Avoid**:
- "The system leverages OCC for concurrency control"
- "We use JWT for auth"

#### One Idea Per Sentence

Keep sentences focused and simple.

✅ **Good**:
- "The API validates input. It returns an error if validation fails."

❌ **Avoid**:
- "The API validates input and returns an error if validation fails, which helps prevent invalid data from entering the system."

---

## Formatting Standards

### Headings

#### Hierarchy

Use heading levels consistently:

```markdown
# Document Title (H1) - One per document

## Major Section (H2)

### Subsection (H3)

#### Detail Section (H4)

##### Minor Detail (H5) - Use sparingly
```

#### Capitalization

Use **sentence case** for headings.

✅ **Good**:
- "Getting started with the API"
- "Database configuration options"
- "Troubleshooting common issues"

❌ **Avoid**:
- "Getting Started With The API"
- "Database Configuration Options"
- "Troubleshooting Common Issues"

### Lists

#### Bulleted Lists

Use for unordered items:

```markdown
- First item
- Second item
- Third item
  - Nested item
  - Another nested item
```

#### Numbered Lists

Use for sequential steps or ordered items:

```markdown
1. First step
2. Second step
3. Third step
   1. Sub-step
   2. Another sub-step
```

#### Checklist

Use for tasks or requirements:

```markdown
- [ ] Incomplete task
- [x] Completed task
```

### Code Formatting

#### Inline Code

Use backticks for inline code, commands, file names, and technical terms:

```markdown
Use the `kubectl apply` command to deploy.
Edit the `application.yml` file.
The `CustomerService` class handles business logic.
```

#### Code Blocks

Use fenced code blocks with language specification:

````markdown
```java
public class Customer {
    private String id;
    private String name;
}
```

```bash
./gradlew build
./gradlew test
```

```yaml
server:
  port: 8080
  context-path: /api
```
````

#### Command Examples

Show commands with expected output:

```markdown
```bash
$ kubectl get pods
NAME                     READY   STATUS    RESTARTS   AGE
app-deployment-abc123    1/1     Running   0          5m
```
```

### Links

#### Internal Links

Use relative paths for internal documentation:

```markdown
```

#### External Links

Use descriptive link text:

✅ **Good**:
```markdown
See the [Spring Boot documentation](https://spring.io/projects/spring-boot) for more information.
```

❌ **Avoid**:
```markdown
Click [here](https://spring.io/projects/spring-boot) for more information.
See https://spring.io/projects/spring-boot
```

### Tables

Use tables for structured data:

```markdown
| Column 1 | Column 2 | Column 3 |
|----------|----------|----------|
| Data 1   | Data 2   | Data 3   |
| Data 4   | Data 5   | Data 6   |
```

**Alignment**:
- Left-align text columns
- Right-align number columns
- Center-align status indicators

```markdown
| Name | Count | Status |
|------|------:|:------:|
| Item 1 | 100 | ✅ |
| Item 2 | 250 | 🟡 |
```

### Emphasis

#### Bold

Use **bold** for:
- Important terms on first use
- UI elements
- Emphasis

```markdown
Click the **Save** button.
The **aggregate root** is responsible for consistency.
This is **critical** for security.
```

#### Italic

Use *italic* for:
- Technical terms
- Variable names
- Subtle emphasis

```markdown
Replace *username* with your actual username.
The *customerId* parameter is required.
```

#### Code Formatting

Use `code formatting` for:
- Code elements
- Commands
- File names
- API endpoints

```markdown
The `CustomerService` class
Run `./gradlew build`
Edit `application.yml`
Call `/api/v1/customers`
```

---

## Document Structure

### Standard Document Template

Every documentation file should follow this structure:

```markdown
# Document Title

> **Last Updated**: YYYY-MM-DD  
> **Owner**: Team Name  
> **Status**: Active|Draft|Deprecated

## Overview

Brief description of the document's purpose and scope.

## [Main Content Sections]

### Section 1

Content...

### Section 2

Content...

## Change History

| Date | Version | Changes | Author |
|------|---------|---------|--------|
| YYYY-MM-DD | 1.0 | Initial creation | Name |

---

**Document Version**: 1.0  
**Last Updated**: YYYY-MM-DD  
**Next Review**: YYYY-MM-DD
```

### Frontmatter

Include metadata at the top of each document:

```markdown
> **Last Updated**: 2024-11-09  
> **Owner**: Documentation Team  
> **Status**: Active  
> **Reviewers**: Architecture Team, Security Team
```

### Overview Section

Every document should start with an overview:

```markdown
## Overview

This document describes [purpose]. It covers [scope] and is intended for [audience].

Key topics include:
- Topic 1
- Topic 2
- Topic 3
```

### Related Documentation

Link to related documents at the end:

```markdown```

---

## Diagram Standards

### When to Use Diagrams

Use diagrams to:
- Illustrate architecture
- Show process flows
- Explain relationships
- Visualize data structures

### Diagram Types

#### PlantUML

Use for:
- Complex UML diagrams
- Detailed class diagrams
- Sequence diagrams
- Component diagrams

**Location**: `docs/diagrams/viewpoints/{viewpoint}/`

**Format**: PNG (primary), SVG (supplementary)

#### Mermaid

Use for:
- Simple flowcharts
- Basic sequence diagrams
- State diagrams
- Gantt charts

**Location**: Inline in markdown or `docs/diagrams/mermaid/`

#### ASCII Art

Use for:
- Very simple diagrams
- Quick sketches
- Terminal-friendly diagrams

### Diagram Quality Standards

- [ ] Clear and readable
- [ ] Properly labeled
- [ ] Consistent style
- [ ] Appropriate level of detail
- [ ] Current and accurate
- [ ] Alt text provided

### Diagram References

Always provide alt text and captions:

```markdown
*Figure 1: High-level system architecture showing major components and their interactions*
```

---

## Technical Writing Guidelines

### API Documentation

#### Endpoint Documentation

```markdown
### Create Customer

**Endpoint**: `POST /api/v1/customers`

**Description**: Creates a new customer account.

**Request**:
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "phone": "+1234567890"
}
```

**Response** (201 Created):
```json
{
  "id": "cust-123",
  "name": "John Doe",
  "email": "john@example.com",
  "createdAt": "2024-11-09T10:00:00Z"
}
```

**Error Responses**:
- `400 Bad Request`: Invalid input
- `409 Conflict`: Email already exists

**Example**:
```bash
curl -X POST https://api.example.com/api/v1/customers \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"john@example.com"}'
```
```

#### Parameter Documentation

```markdown
**Parameters**:

| Name | Type | Required | Description |
|------|------|----------|-------------|
| name | string | Yes | Customer full name (2-100 characters) |
| email | string | Yes | Valid email address |
| phone | string | No | Phone number in E.164 format |
```

### Configuration Documentation

```markdown
### Database Configuration

Configure the database connection in `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

**Configuration Options**:

| Property | Default | Description |
|----------|---------|-------------|
| `url` | - | Database connection URL |
| `username` | - | Database username |
| `password` | - | Database password |
| `pool-size` | 10 | Connection pool size |
```

### Procedure Documentation

Use numbered steps for procedures:

```markdown
## Deploying the Application

1. **Build the application**:
   ```bash
   ./gradlew build
   ```

2. **Create Docker image**:
   ```bash
   docker build -t myapp:latest .
   ```

3. **Deploy to Kubernetes**:
   ```bash
   kubectl apply -f deployment.yaml
   ```

4. **Verify deployment**:
   ```bash
   kubectl get pods
   ```
```

---

## Language and Grammar

### Spelling

- Use American English spelling
- Run spell check before committing
- Add technical terms to custom dictionary

### Grammar

- Use complete sentences
- Avoid sentence fragments
- Use proper punctuation
- Check grammar before committing

### Capitalization

#### Product Names

Capitalize product and service names:
- Spring Boot
- PostgreSQL
- Amazon Web Services (AWS)
- Kubernetes

#### Technical Terms

Follow standard capitalization:
- API (not Api or api)
- JSON (not Json or json)
- HTTP (not Http or http)
- REST (not Rest or rest)

### Abbreviations

#### First Use

Spell out abbreviations on first use:

```markdown
The Application Programming Interface (API) provides...
Use JSON Web Tokens (JWT) for authentication...
```

#### Common Abbreviations

These don't need to be spelled out:
- API
- HTTP/HTTPS
- JSON
- XML
- SQL
- URL
- ID

### Numbers

- Spell out numbers one through nine
- Use numerals for 10 and above
- Use numerals for technical values (5 MB, 3 seconds)
- Use commas for thousands (1,000 not 1000)

---

## Accessibility

### Alt Text

Provide descriptive alt text for all images:

```markdown
```

### Link Text

Use descriptive link text:

✅ **Good**:
```markdown
```

❌ **Avoid**:
```markdown
```

### Color

Don't rely solely on color to convey information:

✅ **Good**:
```markdown
- ✅ Complete
- 🟡 In Progress
- 🔴 Blocked
```

❌ **Avoid**:
```markdown
- 🟢 (no text)
- 🟡 (no text)
- 🔴 (no text)
```

---

## Version Control

### Commit Messages

Use clear, descriptive commit messages:

```
docs: add API authentication guide
docs: update deployment instructions
docs: fix broken links in architecture section
```

### File Naming

Use lowercase with hyphens:

✅ **Good**:
- `getting-started.md`
- `api-authentication.md`
- `deployment-guide.md`

❌ **Avoid**:
- `GettingStarted.md`
- `API_Authentication.md`
- `deployment guide.md`

---

## Quality Checklist

Before submitting documentation:

### Content

- [ ] Accurate and complete
- [ ] Clear and concise
- [ ] Appropriate level of detail
- [ ] Examples provided
- [ ] Error cases covered

### Style

- [ ] Active voice used
- [ ] Present tense used
- [ ] Consistent terminology
- [ ] Proper capitalization
- [ ] No jargon or explained

### Formatting

- [ ] Proper heading hierarchy
- [ ] Code blocks formatted correctly
- [ ] Links work correctly
- [ ] Tables formatted properly
- [ ] Lists formatted consistently

### Technical

- [ ] Code examples tested
- [ ] Commands verified
- [ ] Configuration validated
- [ ] Diagrams current
- [ ] Cross-references correct

### Quality

- [ ] Spelling checked
- [ ] Grammar checked
- [ ] Links validated
- [ ] Diagrams validated
- [ ] Metadata complete

---

## Tools and Resources

### Recommended Tools

- **Markdown Editor**: VS Code with Markdown extensions
- **Spell Check**: `cspell` or VS Code spell checker
- **Link Checker**: `markdown-link-check`
- **Diagram Tools**: PlantUML, Mermaid, draw.io
- **Grammar Check**: Grammarly or LanguageTool

### Validation Scripts

```bash
# Spell check
./scripts/check-spelling.sh

# Link validation
./scripts/validate-links.sh

# Diagram validation
./scripts/validate-diagrams.sh

# Complete validation
./scripts/validate-docs.sh
```

### Style Resources

- [Microsoft Writing Style Guide](https://docs.microsoft.com/en-us/style-guide/)
- [Google Developer Documentation Style Guide](https://developers.google.com/style)
- [Markdown Guide](https://www.markdownguide.org/)

---

## Examples

### Good Documentation Example

```markdown
# Customer API

## Overview

The Customer API provides endpoints for managing customer accounts. It supports creating, reading, updating, and deleting customer records.

## Authentication

All endpoints require JWT authentication. Include the token in the Authorization header:

```bash
Authorization: Bearer <your-token>
```

## Create Customer

**Endpoint**: `POST /api/v1/customers`

Creates a new customer account.

**Request**:
```json
{
  "name": "John Doe",
  "email": "john@example.com"
}
```

**Response** (201 Created):
```json
{
  "id": "cust-123",
  "name": "John Doe",
  "email": "john@example.com",
  "createdAt": "2024-11-09T10:00:00Z"
}
```

**Example**:
```bash
curl -X POST https://api.example.com/api/v1/customers \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"john@example.com"}'
```
```

---

## Contact and Support

### Style Guide Questions

- **Channel**: #documentation Slack channel
- **Email**: docs-team@example.com
- **Issues**: GitHub documentation repository

### Requesting Changes

Submit style guide change requests via:
1. GitHub issue in documentation repository
2. Discussion in #documentation channel
3. Email to documentation team

---

## Appendix

### Related Documents

- [Documentation Maintenance Guide](MAINTENANCE.md)
- [Documentation Metrics](METRICS.md)
- [Diagram Generation Standards](diagrams/README.md)

### Change History

| Date | Version | Changes | Author |
|------|---------|---------|--------|
| 2024-11-09 | 1.0 | Initial creation | Documentation Team |

---

**Document Version**: 1.0  
**Last Updated**: 2024-11-09  
**Next Review**: 2024-12-09
