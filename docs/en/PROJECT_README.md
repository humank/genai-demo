# Steering Rules Index

## 📋 Integrated Steering File Structure

### 🎯 Core Development Guidance

- **[development-standards.md](development-standards.md)** - Development Standards and Specifications
  - Test layering strategy and optimization (98.2% performance improvement)
  - Test tagging system (@UnitTest, @SmokeTest, @IntegrationTest)
  - BDD/TDD development workflow
  - Code standards and naming conventions
  - Quality standards and performance benchmarks

### 🏗️ Architecture Design Guidance  

- **[architecture-patterns.md](architecture-patterns.md)** - Architecture Patterns and Design Principles
  - DDD tactical patterns (Aggregate Root, Value Object, Domain Event)
  - Hexagonal Architecture implementation (Ports and Adapters)
  - Event-driven architecture (CQRS, Event Store)

### 📚 Project Information

- **[project-overview.md](project-overview.md)** - Project Overview and Technology Stack
  - Project introduction and core features
  - Complete technology stack information
  - Project structure and quick start

### 🔧 Specialized Guidance

- **[domain-events.md](domain-events.md)** - Detailed Domain Event Implementation Guide
- **[translation-guide.md](translation-guide.md)** - Documentation Translation Automation Rules

## 🚀 Integration Results

### ✅ Optimization Achievements

#### Steering File Integration

- **File Count**: 7 files → **6 files** (further streamlined)
- **Test Content Integration**: Independent test guides → development-standards.md
- **Context Window**: Significantly reduced token usage for steering rules
- **Maintainability**: Centralized management, avoiding information fragmentation

#### Test Performance Optimization (January 2025)

- **Test Execution Time**: 13min 52sec → 15sec (**98.2% improvement**)
- **Memory Usage**: 6GB → 1-3GB (**50-83% savings**)
- **Parallel Execution**: Single-threaded → Multi-core (**8x improvement**)

### 📊 File Structure Comparison

| Category | Before Integration | After Integration | Change |
|----------|-------------------|-------------------|---------|
| Test-related | 2 independent files | Integrated into development-standards.md | -100% |
| Architecture-related | 1 file | 1 file | Maintained |
| Project information | 1 file | 1 file | Maintained |
| Specialized guidance | 2 files | 2 files | Maintained |

### 🎯 Usage Guidance

1. **Daily Development** → Refer to `development-standards.md`
2. **Architecture Design** → Refer to `architecture-patterns.md`  
3. **Project Understanding** → Refer to `project-overview.md`
4. **Domain Events** → Refer to `domain-events.md`
5. **Documentation Translation** → Refer to `translation-guide.md`

## 📖 Detailed Documentation References

Complete implementation guides and detailed documentation have been moved to:

- `docs/architecture/` - Architecture design documentation  
- `docs/development/` - Development tool configuration
- `app/build/reports/jacoco/test/html/index.html` - Test coverage reports

## 🎯 Quick Start

### Test Execution (Optimized)

```bash
./gradlew quickTest      # Quick check (2 seconds)
./gradlew unitTest       # Unit tests (11 seconds)  
./gradlew integrationTest # Integration tests
./gradlew test           # Complete test suite
```

### Development Workflow

1. **Daily Development**: Use `quickTest` for quick feedback
2. **Before Commit**: Run `unitTest` for complete verification
3. **PR Check**: Execute `integrationTest` for integration verification
4. **Before Release**: Run `test` for complete test suite
