# English Documentation Standards

## Overview

This document establishes the mandatory requirement for all new and modified Markdown documentation to be written in English. This standard ensures consistency, accessibility, and maintainability across the entire project.

## Mandatory Requirements

### 📝 English-Only Documentation Rule

**All new and modified Markdown (.md) files MUST be written in English.**

This applies to:
- All documentation files (*.md)
- README files
- Architecture documentation
- Technical specifications
- User guides
- API documentation
- Release notes
- Change logs
- Project reports
- Meeting notes
- Design documents

### 🚫 Prohibited Languages

- Traditional Chinese (繁體中文)
- Simplified Chinese (简体中文)
- Any other non-English languages

### ✅ Exceptions

The following are the ONLY exceptions to this rule:

1. **Legacy Documentation**: Existing Chinese documentation that is being gradually migrated
2. **Code Comments**: Comments within source code files (not Markdown)
3. **User-Facing Content**: Content specifically intended for Chinese-speaking end users
4. **Localization Files**: Translation files for internationalization purposes

## Implementation Guidelines

### For New Documentation

1. **Always start in English**: Write all new documentation directly in English
2. **Use clear, professional English**: Follow technical writing best practices
3. **Avoid machine translation**: Write naturally, don't rely on translation tools
4. **Use consistent terminology**: Follow established technical terms and conventions

### For Existing Documentation

1. **Gradual Migration**: Existing Chinese documentation should be migrated to English over time
2. **Priority Order**: 
   - Critical architecture documents first
   - Development standards and guidelines
   - User-facing documentation
   - Historical documents last

### Quality Standards

#### Language Requirements

- **Grammar**: Use proper English grammar and syntax
- **Clarity**: Write clear, concise sentences
- **Consistency**: Use consistent terminology throughout
- **Professional Tone**: Maintain a professional, technical writing style

#### Technical Writing Best Practices

- Use active voice when possible
- Keep sentences concise and clear
- Use bullet points and lists for better readability
- Include code examples with proper formatting
- Provide clear headings and structure

## Enforcement Mechanisms

### Automated Checks

- **Kiro Hooks**: Automated detection of non-English content in new/modified files
- **Language Detection**: Automatic scanning for Chinese characters in Markdown files
- **Pre-commit Validation**: Prevent commits containing non-English documentation

### Review Process

- **Code Review**: All documentation changes must be reviewed for English compliance
- **Quality Gates**: Non-English documentation will be rejected during review
- **Feedback Loop**: Provide guidance for converting non-English content

## Migration Strategy

### Phase 1: New Content (Immediate)
- All new documentation MUST be in English
- No exceptions for new files

### Phase 2: Critical Updates (Ongoing)
- When updating existing Chinese documentation, convert to English
- Prioritize frequently accessed documents

### Phase 3: Systematic Migration (Long-term)
- Gradual conversion of remaining Chinese documentation
- Archive or remove obsolete Chinese documents

## Tools and Resources

### Writing Tools

- **Grammar Checkers**: Grammarly, ProWritingAid
- **Style Guides**: Google Developer Documentation Style Guide
- **Technical Writing**: Microsoft Writing Style Guide

### Validation Tools

- **Language Detection**: Automated scanning for non-English content
- **Markdown Linting**: Ensure proper Markdown formatting
- **Link Validation**: Verify all links work correctly

## Compliance Monitoring

### Metrics

- **Compliance Rate**: Percentage of documentation in English
- **Migration Progress**: Number of files converted from Chinese to English
- **Quality Score**: Assessment of English writing quality

### Reporting

- **Monthly Reports**: Track progress on English documentation adoption
- **Quality Reviews**: Regular assessment of documentation quality
- **Compliance Dashboard**: Real-time monitoring of language compliance

## Support and Training

### Resources for Team Members

- **English Writing Guidelines**: Internal style guide for technical writing
- **Template Library**: Standard templates for common document types
- **Review Checklist**: Quality checklist for English documentation

### Training Programs

- **Technical Writing Workshops**: Improve English technical writing skills
- **Style Guide Training**: Learn project-specific writing conventions
- **Tool Training**: How to use grammar checkers and writing tools effectively

## Violation Handling

### Detection

- **Automated Scanning**: Kiro hooks detect non-English content
- **Manual Review**: Code reviewers check for compliance
- **Quality Audits**: Regular audits of documentation quality

### Response Process

1. **Immediate Feedback**: Notify author of non-compliance
2. **Guidance Provided**: Offer specific suggestions for improvement
3. **Revision Required**: Request English version before approval
4. **Support Offered**: Provide assistance with English writing if needed

### Escalation

- **Repeated Violations**: Additional training or support
- **Quality Issues**: Style guide review and improvement
- **Systemic Problems**: Process improvement initiatives

## Benefits and Rationale

### Accessibility

- **Global Team**: Enables participation from international team members
- **Knowledge Sharing**: Facilitates knowledge transfer across teams
- **Documentation Reuse**: Allows reuse of documentation in other projects

### Maintainability

- **Single Source**: Eliminates need to maintain multiple language versions
- **Consistency**: Ensures consistent terminology and style
- **Tool Compatibility**: Better integration with documentation tools

### Professional Standards

- **Industry Standard**: English is the standard language for technical documentation
- **Client Requirements**: Many clients expect English documentation
- **Open Source**: Facilitates contribution to open source projects

## Implementation Timeline

### Immediate (Week 1)
- [ ] Implement automated language detection
- [ ] Update code review checklist
- [ ] Communicate new requirements to team

### Short-term (Month 1)
- [ ] Convert critical architecture documents
- [ ] Establish quality review process
- [ ] Create English writing templates

### Long-term (Quarter 1)
- [ ] Complete migration of frequently used documents
- [ ] Establish training programs
- [ ] Implement compliance monitoring

## Related Standards

- [Development Standards](development-standards.md) - Code and documentation quality requirements
- [Code Review Standards](code-review-standards.md) - Review process including documentation
- [Reports Organization Standards](reports-organization-standards.md) - File organization and naming

---

**Effective Date**: September 24, 2025  
**Review Date**: December 24, 2025  
**Owner**: Development Team  
**Status**: Active and Mandatory
