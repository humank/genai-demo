---
inclusion: manual
---

# Documentation Translation Guide

## Translation Trigger Conditions

Automatic translation will be triggered in the following situations:

1. **File Edit Trigger** (Primary method):
   - Automatically triggered when editing `README.md` or `docs/**/*.md` files
   - Only processes files containing Chinese characters
   - Excludes English files in `docs/en/` directory

2. **Manual Trigger** (Backup method):
   - Include `[translate]` or `[en]` in commit message
   - Mention translation requirements in chat

## Directory Structure Standards

### Original Structure (Traditional Chinese)

```
├── README.md
├── aws-eks-architecture.md
├── microservices-refactoring-plan.md
├── shared-kernel-refactoring.md
└── docs/
    ├── architecture-overview.md
    ├── DesignPrinciple.md
    ├── releases/
    │   ├── README.md
    │   └── *.md
    ├── uml/
    │   ├── README.md
    │   └── *.md
    └── requirements/
        └── promotion-pricing/
            └── *.md
```

### Post-Translation Structure

```
├── README.md (Keep bilingual or point to language versions)
├── docs/
    ├── zh-tw/          # Traditional Chinese version
    │   ├── README.md
    │   ├── aws-eks-architecture.md
    │   ├── microservices-refactoring-plan.md
    │   ├── shared-kernel-refactoring.md
    │   ├── architecture-overview.md
    │   ├── DesignPrinciple.md
    │   ├── releases/
    │   ├── uml/
    │   └── requirements/
    └── en/             # English version (auto-generated)
        ├── README.md
        ├── aws-eks-architecture.md
        ├── microservices-refactoring-plan.md
        ├── shared-kernel-refactoring.md
        ├── architecture-overview.md
        ├── DesignPrinciple.md
        ├── releases/
        ├── uml/
        └── requirements/
```

## Link Conversion Rules

### 1. Relative Path Links

```markdown
<!-- Chinese version -->
[Architecture Overview](docs/architecture-overview.md)
[Design Guidelines](./DesignGuideline.MD)
[Release Notes](releases/README.md)

<!-- Convert to English version -->
[Architecture Overview](docs/en/architecture-overview.md)
[Design Guidelines](./DesignGuideline.MD)
[Release Notes](releases/README.md)
```

### 2. Anchor Link Conversion

```markdown
<!-- Chinese version -->
[Tell, Don't Ask Principle](DesignGuideline.MD#tell-dont-ask-原則)
[Project Architecture](#專案架構)

<!-- Convert to English version -->
[Tell, Don't Ask Principle](DesignGuideline.MD#tell-dont-ask-principle)
[Project Architecture](#project-architecture)
```

### 3. Image Link Handling

```markdown
<!-- Chinese version -->
![Class Diagram](./class-diagram.svg)
![Hexagonal Architecture Diagram](../images/hexagonal-architecture.png)

<!-- English version (path unchanged, only translate alt text) -->
![Class Diagram](./class-diagram.svg)
![Hexagonal Architecture Diagram](../images/hexagonal-architecture.png)
```

## Translation Quality Requirements

### 1. Technical Term Consistency

- Domain-Driven Design (DDD) → 領域驅動設計
- Hexagonal Architecture → 六角形架構
- Aggregate Root → 聚合根
- Value Object → 值對象
- Repository → 儲存庫
- Specification → 規格

### 2. Code Blocks

- Keep code unchanged
- Translate comments and strings
- Maintain English variable names

### 3. File Names

- Keep file names unchanged (e.g., DesignGuideline.MD)
- Only translate content, not file paths

## Execution Process

1. **Detect Changed Files**: Use `git diff --cached --name-only` to find .md files in current commit
2. **Create Directory Structure**: Ensure `docs/en/` directory structure exists
3. **Translate Content**: Translate each file while converting internal links
4. **Verify Links**: Ensure translated links point to correct English files
5. **Add to Commit**: Automatically add translated files to the same commit

## Special Handling

### 1. PlantUML Files

- Chinese comments in `.puml` files need translation
- Maintain PlantUML syntax structure

### 2. Table Content

- Translate Chinese content in tables
- Maintain table formatting

### 3. Code Comments

```java
// Chinese comment → English comment
/* Multi-line Chinese comment → Multi-line English comment */
```

## Error Handling

1. If target file exists and is newer, ask whether to overwrite
2. If link points to non-existent file, log warning but continue processing
3. If translation fails, log error and skip that file
