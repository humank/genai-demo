---
inclusion: always
---

# Chinese Conversation with English Documentation Standards

## Overview

This steering rule defines the language usage policy for different interaction contexts within Kiro. It allows for Chinese conversation during vibe and spec interactions while maintaining English documentation standards for generated files.

## Language Usage Policy

### 🗣️ Conversation Language (Chinese)

#### When to Use Chinese
- **Vibe conversations**: All casual discussions and brainstorming sessions
- **Spec development**: Requirements gathering, design discussions, and planning
- **Interactive Q&A**: Questions, clarifications, and explanations during development
- **Problem-solving discussions**: Debugging, troubleshooting, and solution exploration
- **Code review discussions**: Informal feedback and suggestions during review process

#### Chinese Conversation Guidelines
- Use Traditional Chinese (繁體中文) for all conversational interactions
- Maintain technical accuracy while using natural, conversational tone
- Include English technical terms when necessary for clarity
- Use Chinese for explanations, reasoning, and thought processes

### 📝 Documentation Language (English)

#### When to Use English
- **Generated documentation**: All markdown files, reports, and formal documents
- **Code comments**: Inline code documentation and API documentation
- **Commit messages**: Git commit messages and pull request descriptions
- **Architecture documents**: ADRs, design documents, and technical specifications
- **User-facing content**: README files, installation guides, and user manuals

#### English Documentation Guidelines
- Follow [English Documentation Standards](english-documentation-standards.md)
- Use clear, professional technical writing
- Maintain consistency with existing documentation
- Include proper grammar and technical terminology

## Implementation Rules

### Context Detection

#### Conversation Context (Use Chinese)
- When user asks questions or seeks explanations
- During spec requirement discussions
- When providing feedback or suggestions
- During problem analysis and solution brainstorming
- When explaining code logic or architecture decisions

#### Documentation Context (Use English)
- When creating or modifying .md files
- When generating reports or summaries
- When writing code documentation
- When creating architectural diagrams with text
- When updating README or configuration files

### Language Switching Protocol

#### Automatic Language Detection
```
IF context == "conversation" OR context == "vibe" OR context == "spec_discussion"
    THEN use_language = "Chinese (Traditional)"
ELSE IF context == "file_generation" OR context == "documentation" OR context == "code_comments"
    THEN use_language = "English"
```

#### Manual Override
- User can explicitly request language change with phrases like:
  - "請用中文回答" (Please respond in Chinese)
  - "Please respond in English"
  - "用英文寫文件" (Write documentation in English)

## Practical Examples

### ✅ Correct Usage

#### Chinese Conversation Example
```
User: 我想了解這個 domain event 的設計模式
Kiro: 這個 domain event 設計模式主要是基於 DDD 的概念。讓我解釋一下主要的組成部分：

1. **事件收集**: Aggregate Root 負責收集領域事件
2. **事件發布**: Application Service 負責發布收集到的事件
3. **事件處理**: Infrastructure Layer 處理事件的技術層面

你想要了解哪個部分的詳細實作？
```

#### English Documentation Generation
```
When generating documentation file:

# Domain Event Design Pattern

## Overview
This document describes the domain event design pattern implementation based on DDD principles.

## Components
1. **Event Collection**: Aggregate roots collect domain events
2. **Event Publishing**: Application services publish collected events
3. **Event Handling**: Infrastructure layer handles technical aspects
```

### ❌ Incorrect Usage

#### Wrong: English in Conversation
```
User: 我想了解這個 domain event 的設計模式
Kiro: This domain event design pattern is based on DDD concepts... (Should be Chinese)
```

#### Wrong: Chinese in Documentation
```
When generating documentation file:

# 領域事件設計模式

## 概覽
本文檔描述基於 DDD 原則的領域事件設計模式實作... (Should be English)
```

## Integration with Existing Standards

### Compatibility with English Documentation Standards
- This rule works in conjunction with [English Documentation Standards](english-documentation-standards.md)
- Conversation language does not override documentation requirements
- All generated files must still comply with English-only documentation policy

### Spec and Vibe Integration
- Spec discussions can be conducted in Chinese for better understanding
- Final spec documentation must be generated in English
- Vibe conversations are always in Chinese for natural communication

### Code Review Process
- Review discussions can be in Chinese for clarity
- Final review comments in pull requests should be in English
- Code documentation must remain in English

## Quality Assurance

### Validation Checklist
- [ ] Conversations are conducted in Chinese for better understanding
- [ ] All generated documentation is in English
- [ ] Technical terms are used consistently across languages
- [ ] Context switching is handled appropriately

### Monitoring and Compliance
- Regular review of language usage patterns
- Feedback collection on communication effectiveness
- Adjustment of rules based on team preferences

## Benefits

### Enhanced Communication
- **Natural Discussion**: Chinese allows for more natural technical discussions
- **Better Understanding**: Complex concepts can be explained more clearly in native language
- **Faster Problem Solving**: Quicker communication leads to faster issue resolution

### Maintained Standards
- **Professional Documentation**: English documentation maintains professional standards
- **Global Accessibility**: English documentation is accessible to international team members
- **Consistency**: Uniform documentation language across the project

## Implementation Timeline

### Immediate (Current)
- [x] Create steering rule for language usage
- [x] Define context detection criteria
- [x] Establish switching protocols

### Ongoing
- [ ] Monitor usage patterns and effectiveness
- [ ] Collect team feedback on communication quality
- [ ] Refine rules based on practical experience

---

**Effective Date**: Current  
**Review Date**: Quarterly  
**Owner**: Development Team  
**Status**: Active
