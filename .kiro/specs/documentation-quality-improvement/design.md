# Design Document

## Overview

This design document outlines the approach for improving documentation quality in the `docs/viewpoints/` and `docs/perspectives/` directories. The improvement focuses on three key areas:

1. **File Consolidation**: Merging README.md and overview.md files to eliminate duplication
2. **Cross-Reference Reduction**: Limiting cross-references to improve reading experience
3. **Content Completion**: Filling empty directories and completing stub documents

The implementation follows a systematic approach that preserves all meaningful content while improving document organization and readability.

## Project Technology Stack Analysis

### Current Implementation Components

| Component | Technology | Location | Documentation Status |
|-----------|------------|----------|---------------------|
| Backend API | Java 21 + Spring Boot 3.4.5 | `app/` | ✅ Documented in `docs/development/` |
| Infrastructure | TypeScript CDK (40+ stacks) | `infrastructure/` | ⚠️ Partial - needs stack documentation |
| CMC Frontend | Next.js 14 + React 18 | `cmc-frontend/` | ❌ Missing |
| Consumer Frontend | Angular 18 | `consumer-frontend/` | ❌ Missing |
| E2E Tests | Python + pytest | `e2e-tests/` | ✅ Has README.md |
| Scripts | Python + Bash | `scripts/` | ⚠️ Partial |

### Documentation Gaps Identified

**Empty Documentation Directories:**
- `docs/cicd/` - CI/CD pipeline documentation
- `docs/deployment/` - Deployment procedures
- `docs/mcp/` - MCP server configuration
- `docs/observability/` - Monitoring and tracing
- `docs/project-management/` - Project management docs
- `docs/releases/` - Release notes
- `docs/setup/` - Environment setup
- `docs/testing/` - Testing documentation (may overlap with development/testing)
- `docs/troubleshooting/` - Troubleshooting guides
- `docs/implementation-guides/` - Implementation guides

**Note**: This spec focuses on `viewpoints/` and `perspectives/` directories. The empty directories listed above are out of scope but should be addressed in a separate improvement effort.

## Architecture

### Documentation Structure (Before)

```
docs/
├── viewpoints/
│   ├── functional/
│   │   ├── README.md (100 lines, navigation)
│   │   ├── overview.md (400 lines, detailed content)
│   │   └── *.md (sub-documents)
│   ├── architecture/ (empty)
│   ├── infrastructure/ (empty)
│   └── security/ (empty)
└── perspectives/
    ├── performance/
    │   ├── README.md (50 lines, navigation)
    │   └── overview.md (500 lines, detailed content)
    ├── cost/ (empty)
    └── regulation/ (empty)
```

### Documentation Structure (After)

```
docs/
├── viewpoints/
│   ├── functional/
│   │   ├── README.md (consolidated, ~450 lines)
│   │   └── *.md (sub-documents)
│   ├── information/
│   │   ├── README.md (consolidated)
│   │   └── *.md (sub-documents)
│   └── ... (other viewpoints)
└── perspectives/
    ├── performance/
    │   ├── README.md (consolidated, ~500 lines)
    │   └── *.md (sub-documents)
    ├── cost/
    │   └── README.md (new content)
    ├── regulation/
    │   └── README.md (new content)
    └── ... (other perspectives)
```

## Components and Interfaces

### Component 1: File Consolidation Module

**Purpose**: Merge README.md and overview.md files into a single comprehensive README.md

**Process**:
1. Identify directories with both README.md and overview.md
2. Analyze content overlap between files
3. Merge unique content from overview.md into README.md
4. Remove redundant overview.md files
5. Update any internal references

**Affected Directories**:
- `viewpoints/functional/`
- `viewpoints/information/`
- `perspectives/performance/`
- `perspectives/security/`

### Component 2: Cross-Reference Optimizer

**Purpose**: Reduce cross-references to improve reading experience

**Process**:
1. Scan documents for cross-reference links
2. Categorize links as essential vs redundant
3. Inline essential information where appropriate
4. Remove or consolidate redundant links
5. Ensure maximum 5 cross-references per document

**Link Categories**:
- **Essential**: Links to sub-documents within same directory
- **Contextual**: Links to directly related viewpoints/perspectives
- **Redundant**: Links to general documentation, repeated links

### Component 3: Content Generator

**Purpose**: Create content for empty directories

**New Content Required**:
- `perspectives/cost/README.md` - Cost analysis and optimization
- `perspectives/regulation/README.md` - Regulatory compliance (GDPR, PDPA, APPI)

**Directories to Remove**:
- `viewpoints/architecture/` - Content covered by other viewpoints
- `viewpoints/infrastructure/` - Content covered by deployment viewpoint
- `viewpoints/security/` - Content covered by security perspective

## Data Models

### Document Structure Model

```yaml
ViewpointDocument:
  required_sections:
    - status_badge
    - last_updated
    - owner
    - overview
    - purpose
    - stakeholders
    - key_concerns
    - contents
    - quick_links
  max_cross_references: 5
  date_format: "YYYY-MM-DD"

PerspectiveDocument:
  required_sections:
    - status_badge
    - last_updated
    - owner
    - overview
    - key_concerns
    - quality_attribute_scenarios
    - affected_viewpoints
    - quick_links
  min_qas_count: 3
  max_cross_references: 5
  date_format: "YYYY-MM-DD"
```

### Quality Attribute Scenario Format

```yaml
QualityAttributeScenario:
  source: string          # Who/what generates the stimulus
  stimulus: string        # The condition that affects the system
  environment: string     # System state when stimulus occurs
  artifact: string        # The part of the system affected
  response: string        # How the system responds
  response_measure: string # Measurable metric with numeric target
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

Based on the prework analysis, the following correctness properties have been identified:

### Property 1: File Consolidation Completeness
*For any* viewpoint or perspective directory that originally contained both README.md and overview.md, after consolidation, only README.md SHALL exist and overview.md SHALL be removed.
**Validates: Requirements 1.1, 1.4**

### Property 2: Content Preservation
*For any* merged document, all unique sections from the original overview.md SHALL be present in the consolidated README.md.
**Validates: Requirements 1.2**

### Property 3: Cross-Reference Limit
*For any* viewpoint or perspective README.md, the total number of cross-reference links SHALL NOT exceed 5.
**Validates: Requirements 3.1**

### Property 4: Link Context Requirement
*For any* cross-reference link in a document, there SHALL be accompanying descriptive text explaining what the linked document contains.
**Validates: Requirements 3.4**

### Property 5: No Placeholder Text
*For any* viewpoint or perspective README.md, the document SHALL NOT contain placeholder text patterns such as "To be documented", "TBD", or "Coming soon".
**Validates: Requirements 4.3**

### Property 6: Link Validity
*For any* cross-reference link in a document, the target file SHALL exist at the specified path.
**Validates: Requirements 4.4, 7.1**

### Property 7: Empty Directory Removal
*For any* originally empty viewpoint directory (architecture/, infrastructure/, security/), the directory SHALL be removed from the structure.
**Validates: Requirements 5.3**

### Property 8: QAS Count Requirement
*For any* perspective README.md, the document SHALL contain at least 3 Quality Attribute Scenarios.
**Validates: Requirements 6.1**

### Property 9: QAS Measurability
*For any* Quality Attribute Scenario, the response_measure field SHALL contain at least one numeric target value.
**Validates: Requirements 6.2**

### Property 10: Date Format Consistency
*For any* date field in a document (last_updated, Change History entries), the date SHALL follow the YYYY-MM-DD format.
**Validates: Requirements 9.1, 9.2**

### Property 11: Viewpoint Structure Compliance
*For any* viewpoint README.md, the document SHALL contain all required sections: Overview, Purpose, Stakeholders, Key Concerns, Contents, Quick Links.
**Validates: Requirements 10.1**

### Property 12: Perspective Structure Compliance
*For any* perspective README.md, the document SHALL contain all required sections: Overview, Key Concerns, Quality Attribute Scenarios, Affected Viewpoints, Quick Links.
**Validates: Requirements 10.2**

## Error Handling

### File Operation Errors
- **File not found**: Log warning and skip file
- **Permission denied**: Log error and abort operation
- **Merge conflict**: Preserve both versions and flag for manual review

### Content Validation Errors
- **Missing required section**: Add placeholder section with TODO marker
- **Invalid date format**: Correct to YYYY-MM-DD format
- **Broken link**: Log warning and remove link

## Testing Strategy

### Dual Testing Approach

This implementation uses both unit tests and property-based tests:

1. **Unit Tests**: Verify specific examples and edge cases
2. **Property-Based Tests**: Verify universal properties across all documents

### Testing Framework

**Framework**: Python `pytest` with custom validation functions

**Approach**: 
- Each property is validated against ALL relevant files in the documentation structure
- For example, "Cross-Reference Limit" property is tested on every README.md file
- Tests are deterministic (not random) since we're validating a fixed set of files

### Test Categories

#### File Structure Tests
- Verify no overview.md files exist after consolidation
- Verify README.md exists in all viewpoint/perspective directories
- Verify empty directories are removed

#### Content Validation Tests
- Verify required sections exist in all documents
- Verify cross-reference count <= 5
- Verify no placeholder text exists
- Verify date format compliance

#### Link Validation Tests
- Verify all cross-reference links point to existing files
- Verify all diagram references point to existing files

### Test Execution

```bash
# Run all documentation quality tests
python -m pytest tests/documentation/ -v

# Run property-based tests only
python -m pytest tests/documentation/ -v -m "property"

# Run with coverage
python -m pytest tests/documentation/ --cov=scripts/doc_quality
```
