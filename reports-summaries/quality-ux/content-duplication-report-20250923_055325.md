# Content Duplication Detection Report
Generated: Tue Sep 23 05:53:25 CST 2025
Threshold: 80.0%

## Summary
- Total duplicates found: 2
- Full document duplicates: 0
- Section duplicates: 2

## Detailed Findings

### Duplicate 1: Section
**Similarity**: 83.60%
**File 1**: `docs/viewpoints/development/coding-standards/api-design-standards.md`
**File 2**: `docs/viewpoints/development/coding-standards/README.md`

**Section 1 Preview**: #### 基本 CRUD 操作
```
GET    /api/v1/customers                    # 取得客戶列表
GET    /api/v1/customers/{i...
**Section 2 Preview**: # 資源命名：複數名詞
GET    /api/v1/customers           # 取得客戶列表
GET    /api/v1/customers/{id}      # 取得特定客戶
...

---

### Duplicate 2: Section
**Similarity**: 91.06%
**File 1**: `docs/viewpoints/development/tools-and-environment/technology-stack.md`
**File 2**: `docs/viewpoints/development/build-system/build-deployment.md`

**Section 1 Preview**: # Management endpoints
management:
  endpoints:
    web:
      exposure:
        include: health,inf...
**Section 2 Preview**: # application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,met...

---

## Recommendations

1. **Review High Similarity Content**: Examine content with >90% similarity
2. **Consolidate Duplicates**: Merge similar sections where appropriate
3. **Create Cross-References**: Use links instead of duplicating content
4. **Establish Single Source of Truth**: Designate authoritative sources for common topics
