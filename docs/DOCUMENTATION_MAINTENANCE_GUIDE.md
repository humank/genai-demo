# Documentation Maintenance Guide

> **Architecture Documentation Maintenance Best Practices Based on Rozanski & Woods Methodology**

## Overview

This guide provides complete guidance for maintaining architecture documentation structure based on Rozanski & Woods Viewpoints & Perspectives, ensuring document quality, consistency, and usability.

## Documentation Structure Overview

### Core Structure

```
docs/
├── README.md                    # Documentation center navigation
├── viewpoints/                  # Seven architecture viewpoints
│   ├── functional/             # Functional viewpoint
│   ├── information/            # Information viewpoint
│   ├── concurrency/            # Concurrency viewpoint
│   ├── development/            # Development viewpoint
│   ├── deployment/             # Deployment viewpoint
│   └── operational/            # Operational viewpoint
├── perspectives/               # Eight architecture perspectives
│   ├── security/              # Security perspective
│   ├── performance/           # Performance perspective
│   ├── availability/          # Availability perspective
│   ├── evolution/             # Evolution perspective
│   ├── usability/             # Usability perspective
│   ├── regulation/            # Regulation perspective
│   ├── location/              # Location perspective
│   └── cost/                  # Cost perspective
├── diagrams/                   # Diagram resources
│   ├── viewpoints/            # Viewpoint-related diagrams
│   └── perspectives/          # Perspective-related diagrams
├── templates/                  # Document templates
└── en/                        # English versions (auto-generated)
```

## Maintenance Workflow

### 1. Daily Maintenance Tasks

#### Weekly Checklist
- [ ] Check translation quality and consistency
- [ ] Verify internal link validity
- [ ] Update outdated technical information
- [ ] Check diagram and document synchronization status

#### Monthly Checklist
- [ ] Execute complete document quality checks
- [ ] Update professional terminology dictionary
- [ ] Check accuracy of cross-viewpoint references
- [ ] Verify user experience paths

#### Quarterly Checklist
- [ ] Evaluate effectiveness of document structure
- [ ] Collect user feedback and improve
- [ ] Update maintenance guides and best practices
- [ ] Execute architecture document compliance reviews

### 2. Automated Maintenance Tools

#### Translation System
```bash
# Test translation system status
./scripts/test-translation-system.sh

# Check translation quality
./scripts/check-translation-quality.sh

# Fix translation quality issues
python3 scripts/fix-translation-quality.py
```

#### Document Quality Checks
```bash
# Check document quality
./scripts/check-documentation-quality.sh

# Verify link validity
node scripts/check-links-advanced.js

# Validate diagram metadata
python3 scripts/validate-diagrams.py
```

#### User Experience Testing
```bash
# Execute user experience testing
python3 scripts/test-user-experience.py

# View test reports
cat reports-summaries/testing/user-experience-test-report.md
```

## Content Maintenance Standards

### 1. Viewpoint Document Maintenance

#### Standard Structure Checks
Each Viewpoint document must include:
- [ ] **Overview**: Clearly define the viewpoint's purpose and scope
- [ ] **Stakeholders**: Explicitly list primary and secondary stakeholders
- [ ] **Concerns**: Detail the architectural issues this viewpoint addresses
- [ ] **Architecture Elements**: Describe relevant architectural components and relationships
- [ ] **Quality Attribute Considerations**: Explain how each Perspective applies to this viewpoint
- [ ] **Related Diagrams**: Link to relevant visualization resources
- [ ] **Relationships with Other Viewpoints**: Describe cross-viewpoint relationships
- [ ] **Implementation Guidelines**: Provide specific implementation recommendations
- [ ] **Validation Standards**: Define how to validate the implementation quality of this viewpoint

#### Quality Check Standards
- **Consistency**: Terminology usage conforms to professional dictionary definitions
- **Completeness**: All necessary sections are completed
- **Accuracy**: Technical information aligns with actual implementation
- **Readability**: Clear structure with concise and clear language
- **Relevance**: Accurate references to other viewpoints and perspectives

### 2. Perspective Document Maintenance

#### Standard Structure Checks
Each Perspective document must include:
- [ ] **Overview**: Define quality attributes and importance
- [ ] **Quality Attributes**: Clearly define primary and secondary quality attributes
- [ ] **Cross-Viewpoint Application**: Explain how to manifest in various viewpoints
- [ ] **Design Strategies**: Design methods for implementing this perspective
- [ ] **Implementation Technologies**: Supporting technologies and tools
- [ ] **Testing and Validation**: Validation methods and standards
- [ ] **Monitoring and Metrics**: Related monitoring indicators

#### Cross-Viewpoint Consistency Checks
- **Functional Viewpoint**: Quality attribute manifestation at business logic level
- **Information Viewpoint**: Quality considerations for data and information flow
- **Concurrency Viewpoint**: Quality impact of concurrent processing
- **Development Viewpoint**: Quality assurance in development process
- **Deployment Viewpoint**: Quality requirements for deployment environment
- **Operational Viewpoint**: Quality monitoring in operational phase

### 3. Diagram Maintenance

#### Diagram Types and Usage
- **Mermaid (.mmd)**: System architecture diagrams, flowcharts, sequence diagrams
- **PlantUML (.puml)**: Detailed UML diagrams, complex class diagrams, design documents
- **Excalidraw (.excalidraw)**: Conceptual design, brainstorming, hand-drawn style diagrams

#### Diagram Maintenance Checks
- [ ] **Synchronization**: Diagram content consistent with document descriptions
- [ ] **Readability**: Diagrams clear and understandable with complete labels
- [ ] **Standardization**: Use unified colors, fonts, and styles
- [ ] **Version Control**: Diagram changes have appropriate version records
- [ ] **Format Conversion**: SVG versions synchronized with source files

#### Automated Diagram Generation
```bash
# Generate all diagrams
./scripts/generate-all-diagrams.sh

# Validate diagram syntax
python3 scripts/validate-diagrams.py

# Synchronize diagram and document references
python3 scripts/sync-diagram-references.py
```

## Content Maintenance

### 1. Terminology Consistency

#### Professional Terminology Management
Ensure consistent use of professional terminology in documents:
- **Rozanski & Woods Viewpoint Terms**: Functional viewpoint, Information viewpoint, etc.
- **Rozanski & Woods Perspective Terms**: Security perspective, Performance perspective, etc.
- **DDD Strategic Pattern Terms**: Bounded context, Aggregate root, etc.
- **DDD Tactical Pattern Terms**: Entity, Value object, etc.
- **Stakeholder Terms**: Architect, Developer, etc.
- **Design Strategy Terms**: Design patterns, Architecture patterns, etc.

#### Terminology Update Process
1. **Identify New Terms**: Discover new professional terminology in documents
2. **Research Standard Translation**: Find industry standard usage
3. **Establish Standards**: Establish unified usage within project
4. **Verify Consistency**: Execute content quality checks
5. **Apply Corrections**: Use correction scripts to update all documents

### 2. Content Quality Assurance

#### Automated Quality Checks
- **Kiro Hook**: Automatically trigger quality checks when documents change
- **Manual Trigger**: Use translation scripts for batch translation
- **Quality Correction**: Use quality correction scripts to improve translation

#### Translation Validation Process
```bash
# 1. Check translation completeness
./scripts/check-translation-quality.sh

# 2. Fix terminology consistency
python3 scripts/fix-translation-quality.py

# 3. Verify link validity
node scripts/check-links-advanced.js docs/en/
```

## Quality Assurance Process

### 1. Content Review Standards

#### Technical Accuracy
- [ ] Code examples are executable and correct
- [ ] Architecture descriptions align with actual implementation
- [ ] Technical specifications comply with latest standards
- [ ] External links are valid and relevant

#### Structural Integrity
- [ ] All necessary sections are completed
- [ ] Logical relationships between sections are clear
- [ ] Cross-references are accurate
- [ ] Navigation paths are complete and usable

#### Language Quality
- [ ] Grammar is correct with clear expression
- [ ] Terminology usage is consistent
- [ ] Tone is appropriate for target audience
- [ ] Translation is accurate and natural

### 2. Automated Quality Checks

#### GitHub Actions Workflow
Location: `.github/workflows/documentation-quality.yml`

Check Items:
- Link validity verification
- Markdown syntax checking
- Terminology consistency verification
- Diagram synchronization checking
- Translation completeness verification

#### Local Quality Checks
```bash
# Execute complete quality check
./scripts/check-documentation-quality.sh

# Check specific types of issues
./scripts/check-translation-quality.sh
node scripts/check-links-advanced.js
python3 scripts/validate-diagrams.py
python3 scripts/validate-metadata.py
```

## User Experience Optimization

### 1. Navigation Experience

#### Multi-level Navigation Design
- **Main Navigation**: Documentation center provides overview and quick entry points
- **Category Navigation**: Viewpoints and perspectives each have dedicated navigation pages
- **Cross Navigation**: Viewpoint-perspective matrix provides cross-references
- **Role Navigation**: Dedicated navigation paths for different roles

#### Navigation Optimization Checks
- [ ] All major entry points are accessible
- [ ] Navigation paths are logically clear
- [ ] Breadcrumb navigation is accurate
- [ ] Related content recommendations are effective

### 2. Search Experience

#### Content Discoverability
- **Title Structure**: Use clear title hierarchy
- **Keyword Optimization**: Use key terminology in important positions
- **Tag System**: Use consistent tags and categories
- **Summary Descriptions**: Provide clear section summaries

#### Search Optimization Recommendations
- Provide clear summaries at the beginning of documents
- Use standardized terminology and keywords
- Provide synonyms with multiple expressions
- Establish complete cross-reference system

### 3. Accessibility

#### Accessible Design
- **Structured Headings**: Use correct heading hierarchy (H1-H6)
- **Alternative Text**: Provide descriptive text for all images
- **Link Descriptions**: Link text clearly describes target content
- **Color Contrast**: Ensure sufficient color contrast

#### Multi-language Support
- **Parallel Structure**: Chinese and English versions maintain same structure
- **Cultural Adaptation**: Consider understanding differences across cultural backgrounds
- **Localization**: Adapt to expression habits of different regions

## Troubleshooting

### 1. Common Issues and Solutions

#### Translation Issues
**Issue**: Inconsistent terminology translation
**Solution**:
```bash
# 1. Update terminology dictionary
vim docs/.terminology.json

# 2. Execute terminology correction
python3 scripts/fix-translation-quality.py

# 3. Verify correction results
./scripts/check-translation-quality.sh
```

**Issue**: Poor automatic translation quality
**Solution**:
- Check Amazon Q CLI configuration
- Update translation prompt templates
- Manually adjust translation of important files

#### Link Issues
**Issue**: Internal links broken
**Solution**:
```bash
# 1. Check link status
node scripts/check-links-advanced.js

# 2. Fix link redirects
python3 scripts/create-link-redirects.py

# 3. Update link references
# Manual correction or use text replacement tools
```

#### Diagram Issues
**Issue**: Diagrams out of sync with documents
**Solution**:
```bash
# 1. Regenerate diagrams
./scripts/generate-all-diagrams.sh

# 2. Synchronize diagram references
python3 scripts/sync-diagram-references.py

# 3. Validate diagram syntax
python3 scripts/validate-diagrams.py
```

### 2. Emergency Fix Process

#### Critical Issue Handling
1. **Immediate Assessment**: Determine the impact scope of the issue
2. **Quick Fix**: Implement temporary solution
3. **Root Analysis**: Find the root cause of the issue
4. **Permanent Fix**: Implement long-term solution
5. **Prevention Measures**: Update check process to prevent recurrence

#### Rollback Procedure
```bash
# 1. Check Git history
git log --oneline docs/

# 2. Rollback to stable version
git checkout <stable-commit> -- docs/

# 3. Re-execute quality checks
./scripts/check-documentation-quality.sh

# 4. Commit fix
git add docs/
git commit -m "Rollback documentation to stable state"
```

## Continuous Improvement

### 1. Performance Monitoring

#### Maintenance Metrics
- **Translation Completeness**: English version coverage rate
- **Link Health**: Valid link percentage
- **Content Freshness**: Last update time distribution
- **User Satisfaction**: Feedback and ratings

#### Regular Evaluation
- **Monthly Reports**: Maintenance activities and issue statistics
- **Quarterly Reviews**: Document structure and process evaluation
- **Annual Planning**: Major improvements and upgrade plans

### 2. Tool and Process Improvement

#### Automation Enhancement
- Expand automated check scope
- Improve translation quality and speed
- Enhance diagram generation capabilities
- Optimize user experience testing

#### Process Optimization
- Simplify maintenance workflows
- Reduce manual operation requirements
- Improve issue detection speed
- Enhance repair efficiency

## Contact and Support

### Maintenance Team
- **Architecture Documentation Lead**: Responsible for overall document strategy and quality
- **Translation Coordinator**: Responsible for translation quality and terminology consistency
- **Technical Writer**: Responsible for content creation and editing
- **Tool Maintainer**: Responsible for automation tools and scripts

### Issue Reporting
- **GitHub Issues**: For tracking and managing document issues
- **Internal Communication**: Quick communication and coordination within team
- **User Feedback**: Collect and process user suggestions and issues

---

**Last Updated**: September 27, 2025  
**Version**: 1.0  
**Maintainer**: Architecture Documentation Team