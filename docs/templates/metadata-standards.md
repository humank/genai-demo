# Document Metadata Standard Format

## Overview

This document defines the Front Matter metadata standard format for all documents in the project, ensuring consistency, traceability, and automated processing capabilities.

<!-- 
Note: Mermaid Diagram Format Update
- Old format: .mmd file references
- New format: .md files with ```mermaid code blocks
- Reason: GitHub native support, better readability and maintainability
-->

## Viewpoint Document Metadata Standards

### Basic Format

```yaml
---
title: "Document Title"
viewpoint: "functional|information|concurrency|development|deployment|operational|context"
perspective: ["security", "performance", "availability", "evolution", "usability", "regulation", "location", "cost"]
stakeholders: ["architect", "developer", "operator", "security-engineer", "business-analyst", "product-manager", "end-user"]
related_viewpoints: ["viewpoint1", "viewpoint2"]
related_documents: ["doc1.md", "doc2.md"]
diagrams: ["diagram1.md", "diagram2.puml", "diagram3.excalidraw"]  # Note: Now using .md files with Mermaid code blocks
last_updated: "YYYY-MM-DD"
version: "1.0"
author: "Architecture Team"
review_status: "draft|reviewed|approved"
complexity: "low|medium|high"
priority: "low|medium|high|critical"
tags: ["tag1", "tag2", "tag3"]
---
```

### Field Descriptions

#### Required Fields

- **title**: Document title, should clearly describe the document content
- **viewpoint**: The architectural viewpoint, must be one of the seven viewpoints
- **last_updated**: Last update date, format YYYY-MM-DD
- **version**: Document version number, using semantic versioning
- **author**: Document author or responsible team

#### Optional Fields

- **perspective**: Related architectural perspectives array, can be multiple
- **stakeholders**: Related stakeholders array
- **related_viewpoints**: Related other viewpoints
- **related_documents**: Relative paths to related documents
- **diagrams**: Relative paths to related diagram files
- **review_status**: Document review status
- **complexity**: Document complexity
- **priority**: Document priority
- **tags**: Free tags for classification and search

### Example

```yaml
---
title: "Functional Viewpoint - Domain Model Design"
viewpoint: "functional"
perspective: ["security", "performance", "evolution"]
stakeholders: ["architect", "developer", "business-analyst"]
related_viewpoints: ["information", "development"]
related_documents: ["../information/domain-events.md", "../development/testing-strategy.md"]
diagrams: ["../diagrams/viewpoints/functional/domain-model.md", "../diagrams/viewpoints/functional/bounded-contexts.puml"]  # Note: Now using .md files with Mermaid code blocks
last_updated: "2025-01-21"
version: "2.1"
author: "Architecture Team"
review_status: "approved"
complexity: "high"
priority: "critical"
tags: ["ddd", "domain-model", "aggregates", "bounded-context"]
---
```

## Perspective Document Metadata Standards

### Basic Format

```yaml
---
title: "Perspective Title"
perspective_type: "security|performance|availability|evolution|usability|regulation|location|cost"
applicable_viewpoints: ["functional", "information", "concurrency", "development", "deployment", "operational", "context"]
quality_attributes: ["attribute1", "attribute2", "attribute3"]
stakeholders: ["architect", "developer", "operator", "security-engineer", "business-analyst"]
related_perspectives: ["perspective1", "perspective2"]
related_documents: ["doc1.md", "doc2.md"]
patterns: ["pattern1", "pattern2"]
tools: ["tool1", "tool2"]
metrics: ["metric1", "metric2"]
last_updated: "YYYY-MM-DD"
version: "1.0"
author: "Architecture Team"
review_status: "draft|reviewed|approved"
impact_level: "low|medium|high|critical"
implementation_difficulty: "easy|medium|hard|very-hard"
tags: ["tag1", "tag2", "tag3"]
---
```

### Field Descriptions

#### Required Fields

- **title**: Perspective title
- **perspective_type**: Perspective type, must be one of the eight perspectives
- **applicable_viewpoints**: Array of applicable viewpoints
- **quality_attributes**: Related quality attributes
- **last_updated**: Last update date
- **version**: Document version number
- **author**: Document author

#### Optional Fields

- **stakeholders**: Related stakeholders
- **related_perspectives**: Related other perspectives
- **related_documents**: Related documents
- **patterns**: Related design patterns
- **tools**: Related tools and technologies
- **metrics**: Related metrics
- **review_status**: Review status
- **impact_level**: Impact level
- **implementation_difficulty**: Implementation difficulty
- **tags**: Tags

### Example

```yaml
---
title: "Security Perspective - Authentication and Authorization"
perspective_type: "security"
applicable_viewpoints: ["functional", "information", "development", "deployment", "operational"]
quality_attributes: ["confidentiality", "integrity", "availability", "accountability"]
stakeholders: ["security-engineer", "architect", "developer", "operator"]
related_perspectives: ["performance", "usability", "regulation"]
related_documents: ["../viewpoints/functional/user-management.md", "../viewpoints/deployment/security-configuration.md"]
patterns: ["oauth2", "jwt", "rbac", "zero-trust"]
tools: ["spring-security", "keycloak", "vault"]
metrics: ["authentication-success-rate", "authorization-latency", "security-incidents"]
last_updated: "2025-01-21"
version: "1.3"
author: "Security Team"
review_status: "approved"
impact_level: "critical"
implementation_difficulty: "medium"
tags: ["authentication", "authorization", "oauth2", "jwt", "security"]
---
```

## Diagram Metadata Standards

### Basic Format

```yaml
---
title: "Diagram Title"
type: "mermaid|plantuml|excalidraw"
format: "md|puml|excalidraw|png|svg"
viewpoint: "functional|information|concurrency|development|deployment|operational|context"
perspective: ["security", "performance", "availability"]
diagram_level: "overview|detailed|conceptual"
target_audience: ["architect", "developer", "stakeholder"]
description: "Diagram description and purpose"
related_documents: ["doc1.md", "doc2.md"]
source_file: "diagram-source.md"  # Note: Now using .md files with Mermaid code blocks
generated_files: ["diagram.svg"]
last_updated: "YYYY-MM-DD"
version: "1.0"
author: "Architecture Team"
auto_generated: true|false
generation_source: "code|manual|template"
update_frequency: "on-demand|weekly|monthly"
complexity: "low|medium|high"
maintenance_notes: "Maintenance instructions"
tags: ["tag1", "tag2"]
---
```

### Field Descriptions

#### Required Fields

- **title**: Diagram title
- **type**: Diagram type (mermaid, plantuml, excalidraw)
- **format**: File format
- **description**: Diagram description
- **last_updated**: Last update date
- **version**: Version number
- **author**: Author

#### Optional Fields

- **viewpoint**: Associated viewpoint
- **perspective**: Related perspectives
- **diagram_level**: Diagram detail level
- **target_audience**: Target audience
- **related_documents**: Related documents
- **source_file**: Source file
- **generated_files**: Generated files
- **auto_generated**: Whether automatically generated
- **generation_source**: Generation source
- **update_frequency**: Update frequency
- **complexity**: Complexity
- **maintenance_notes**: Maintenance notes
- **tags**: Tags

### Example

```yaml
---
title: "Domain Model Class Diagram - Customer Aggregate"
type: "plantuml"
format: "puml"
viewpoint: "functional"
perspective: ["security", "evolution"]
diagram_level: "detailed"
target_audience: ["architect", "developer"]
description: "Shows detailed design of customer aggregate root, including entities, value objects and domain services"
related_documents: ["../viewpoints/functional/domain-model.md", "../viewpoints/information/domain-events.md"]
source_file: "customer-aggregate.puml"
generated_files: ["customer-aggregate.svg"]
last_updated: "2025-01-21"
version: "2.0"
author: "Architecture Team"
auto_generated: false
generation_source: "manual"
update_frequency: "on-demand"
complexity: "high"
maintenance_notes: "Update this diagram when customer aggregate root changes"
tags: ["domain-model", "aggregate", "customer", "ddd"]
---
```

## General Document Metadata Standards

### Basic Format

```yaml
---
title: "Document Title"
document_type: "guide|reference|tutorial|specification|report|template"
category: ["architecture", "development", "deployment", "operations"]
audience: ["developer", "architect", "operator", "business"]
difficulty_level: "beginner|intermediate|advanced|expert"
estimated_reading_time: "5 minutes"
prerequisites: ["prerequisite1", "prerequisite2"]
related_documents: ["doc1.md", "doc2.md"]
external_links: ["https://example.com"]
last_updated: "YYYY-MM-DD"
version: "1.0"
author: "Team Name"
reviewers: ["reviewer1", "reviewer2"]
review_status: "draft|reviewed|approved|deprecated"
language: "zh-TW|en"
translation_status: "original|translated|needs-update"
translation_source: "original-doc.md"
keywords: ["keyword1", "keyword2"]
tags: ["tag1", "tag2"]
---
```

### Field Descriptions

#### Required Fields

- **title**: Document title
- **document_type**: Document type
- **last_updated**: Last update date
- **version**: Version number
- **author**: Author
- **language**: Language

#### Optional Fields

- **category**: Document category
- **audience**: Target audience
- **difficulty_level**: Difficulty level
- **estimated_reading_time**: Estimated reading time
- **prerequisites**: Prerequisites
- **related_documents**: Related documents
- **external_links**: External links
- **reviewers**: Reviewers
- **review_status**: Review status
- **translation_status**: Translation status
- **translation_source**: Translation source
- **keywords**: Keywords
- **tags**: Tags

## Metadata Validation Rules

### Required Field Validation

```yaml
# Validation rules example
validation_rules:
  title:
    required: true
    type: string
    min_length: 5
    max_length: 100
  
  viewpoint:
    required: true
    type: string
    allowed_values: ["functional", "information", "concurrency", "development", "deployment", "operational", "context"]
  
  last_updated:
    required: true
    type: date
    format: "YYYY-MM-DD"
  
  version:
    required: true
    type: string
    pattern: "^\\d+\\.\\d+(\\.\\d+)?$"
  
  perspective:
    required: false
    type: array
    allowed_values: ["security", "performance", "availability", "evolution", "usability", "regulation", "location", "cost"]
```

### Consistency Checks

```yaml
consistency_checks:
  # Check if related documents exist
  related_documents:
    check: file_exists
    base_path: "docs/"
  
  # Check if diagram files exist
  diagrams:
    check: file_exists
    base_path: "docs/diagrams/"
  
  # Check viewpoint and perspective consistency
  viewpoint_perspective_consistency:
    check: logical_consistency
    rules:
      - if_viewpoint: "security"
        then_perspective_should_include: ["security"]
```

## Automated Processing

### Metadata Extraction

```bash
#!/bin/bash
# Extract metadata from all files
find docs/ -name "*.md" -exec grep -l "^---$" {} \; | while read file; do
    echo "Processing: $file"
    # Extract Front Matter
    sed -n '/^---$/,/^---$/p' "$file" | head -n -1 | tail -n +2
done
```

### Metadata Validation

```python
# Python script example
import yaml
import os
from pathlib import Path

def validate_metadata(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Extract Front Matter
    if content.startswith('---\n'):
        end_index = content.find('\n---\n', 4)
        if end_index != -1:
            front_matter = content[4:end_index]
            try:
                metadata = yaml.safe_load(front_matter)
                return validate_fields(metadata)
            except yaml.YAMLError as e:
                return f"YAML parsing error: {e}"
    
    return "No valid front matter found"

def validate_fields(metadata):
    errors = []
    
    # Check required fields
    required_fields = ['title', 'last_updated', 'version', 'author']
    for field in required_fields:
        if field not in metadata:
            errors.append(f"Missing required field: {field}")
    
    # Check date format
    if 'last_updated' in metadata:
        try:
            from datetime import datetime
            datetime.strptime(metadata['last_updated'], '%Y-%m-%d')
        except ValueError:
            errors.append("Invalid date format for last_updated")
    
    return errors if errors else "Valid"
```

### Metadata Index Generation

```python
# Generate document index
def generate_document_index():
    index = {
        'viewpoints': {},
        'perspectives': {},
        'documents': [],
        'diagrams': []
    }
    
    for md_file in Path('docs').rglob('*.md'):
        metadata = extract_metadata(md_file)
        if metadata:
            doc_info = {
                'path': str(md_file),
                'metadata': metadata
            }
            
            if 'viewpoint' in metadata:
                viewpoint = metadata['viewpoint']
                if viewpoint not in index['viewpoints']:
                    index['viewpoints'][viewpoint] = []
                index['viewpoints'][viewpoint].append(doc_info)
            
            if 'perspective_type' in metadata:
                perspective = metadata['perspective_type']
                if perspective not in index['perspectives']:
                    index['perspectives'][perspective] = []
                index['perspectives'][perspective].append(doc_info)
            
            index['documents'].append(doc_info)
    
    return index
```

## Best Practices

### Metadata Writing Guidelines

1. **Maintain Consistency**: Use standardized field names and values
2. **Timely Updates**: Update metadata synchronously when documents change
3. **Detailed Descriptions**: Provide sufficient descriptive information
4. **Correct Classification**: Accurately set viewpoints, perspectives, and tags
5. **Relationships**: Correctly set links to related documents and diagrams

### Maintenance Recommendations

1. **Regular Checks**: Regularly validate metadata correctness
2. **Automated Validation**: Use CI/CD processes to automatically validate metadata
3. **Version Control**: Track metadata change history
4. **Documentation**: Document changes to metadata standards

### Tool Integration

1. **Editor Support**: Configure editor YAML syntax highlighting and validation
2. **Git Hooks**: Use Git hooks to validate metadata before commits
3. **CI/CD Integration**: Include metadata validation in build processes
4. **Document Generation**: Automatically generate indexes and navigation based on metadata

This standard format ensures document consistency and maintainability while supporting automated processing and analysis."