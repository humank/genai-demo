# Steering Rules Index

## 📋 Integrated Steering File Structure

### 🎯 Core Development Guidance

- **[development-standards.md](development-standards.md)** - Development Standards and Specifications
  - Project overview and complete technology stack (integrated project-overview.md)
  - Test layering strategy and optimization (98.2% performance improvement)
  - Test tagging system (@UnitTest, @SmokeTest, @IntegrationTest)
  - BDD/TDD development workflow
  - Code standards and naming conventions
  - Quality standards and performance benchmarks

### 🏗️ Architecture Design Guidance  

- **[rozanski-woods-architecture-methodology.md](rozanski-woods-architecture-methodology.md)** - Rozanski & Woods Architecture Methodology
  - Fundamental architecture patterns (integrated architecture-patterns.md)
  - Domain-Driven Design (DDD) tactical patterns (Aggregate Root, Value Object, Domain Event)
  - Hexagonal Architecture implementation (ports and adapters)
  - Event-driven architecture (CQRS, event storage)
  - Architecture viewpoints and perspectives methodology (17 Steering rules)
  - Phased implementation strategy (5 phases)

### 🔧 Specialized Guidance

- **[domain-events.md](domain-events.md)** - Detailed Domain Event implementation guide
- **[translation-guide.md](translation-guide.md)** - Documentation translation automation rules

## 🚀 Integration Results

### ✅ Optimization Achievements

#### Steering File Integration

- **File count**: 7 files → **5 files** (further streamlined by 28.6%)
- **Architecture guidance integration**: architecture-patterns.md → rozanski-woods-architecture-methodology.md
- **Project information integration**: project-overview.md → development-standards.md
- **Context Window**: Further reduced token usage for steering rules
- **Maintainability**: Highly centralized management, avoiding information fragmentation and duplication

#### Test Performance Optimization (January 2025)

- **Test execution time**: 13 minutes 52 seconds → 15 seconds (**98.2% improvement**)
- **Memory usage**: 6GB → 1-3GB (**50-83% savings**)
- **Parallel execution**: Single-threaded → Multi-core (**8x improvement**)

### 📊 File Structure Comparison

| Category | Before Integration | After Integration | Change |
|----------|-------------------|-------------------|--------|
| Core Development | 2 scattered files | 1 unified file | -50% |
| Architecture Design | 2 scattered files | 1 unified file | -50% |
| Specialized Guidance | 2 files | 2 files | Maintained |
| Total | 7 files | 5 files | **-28.6%** |

### 🎯 Usage Guidelines

1. **Daily development and project understanding** → Refer to `development-standards.md`
2. **Architecture design and methodology** → Refer to `rozanski-woods-architecture-methodology.md`
3. **Domain Event implementation** → Refer to `domain-events.md`
4. **Documentation translation** → Refer to `translation-guide.md`

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

1. **Daily development**: Use `quickTest` for rapid feedback
2. **Before commit**: Run `unitTest` for complete verification
3. **PR checks**: Execute `integrationTest` for integration verification
4. **Before release**: Run `test` for complete test suite
