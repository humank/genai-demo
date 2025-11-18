# Archived Availability Documentation

**Archive Date**: 2024-11-19  
**Reason**: Consolidated into comprehensive high availability design guide

## Archived Files

These files have been consolidated into a single comprehensive document:
**[high-availability-design.md](../high-availability-design.md)**

### Original Files (3,599 lines total):

1. **high-availability.md** (792 lines)
   - Multi-AZ deployment architecture
   - Load balancing strategies
   - Health check configurations

2. **fault-tolerance.md** (713 lines)
   - Circuit breaker patterns
   - Retry mechanisms
   - Bulkhead isolation

3. **automated-failover.md** (779 lines)
   - Database failover procedures
   - Application failover
   - Cache failover

4. **multi-region-architecture.md** (582 lines)
   - Global distribution strategy
   - Cross-region replication
   - Traffic routing

5. **chaos-engineering.md** (723 lines)
   - Chaos testing framework
   - Experiment procedures
   - Testing schedule

## Why Consolidated?

**Problems with Original Structure**:
- ❌ Over-fragmented (5 files for related HA content)
- ❌ Significant overlap between files
- ❌ Difficult to get complete HA picture
- ❌ Maintenance burden across multiple files

**Benefits of Consolidated Guide**:
- ✅ Complete HA architecture in one place
- ✅ Logical flow from architecture to testing
- ✅ Easier to maintain and update
- ✅ Better for understanding overall strategy

## Files Kept Separate

- **disaster-recovery.md**: DR-specific runbooks and procedures
- **requirements.md**: Detailed SLA and requirement specifications
- **overview.md**: High-level perspective overview

These remain separate as they serve distinct purposes and are referenced independently.

---

**Consolidation performed as part of**: Documentation Quality Improvement Initiative (Phase 1)  
**Reference**: [DOCUMENTATION-ANALYSIS-REPORT.md](../../../DOCUMENTATION-ANALYSIS-REPORT.md)
