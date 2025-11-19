# 🚨 DOCUMENTATION DATE REQUIREMENTS 🚨

**CRITICAL - READ THIS BEFORE EVERY DOCUMENTATION UPDATE**

---

## ⚠️ MANDATORY FIRST STEP

**BEFORE creating or updating ANY documentation file, ALWAYS:**

```bash
# STEP 1: Get the ACTUAL current date
date +%Y-%m-%d

# STEP 2: Use that EXACT date in ALL date fields
```

---

## 🔴 ABSOLUTE REQUIREMENTS

### 1. ALWAYS Get Current Date First

```bash
# Execute this command FIRST
CURRENT_DATE=$(date +%Y-%m-%d)

# Example output: 2025-11-19
```

### 2. NEVER Use These

❌ **FORBIDDEN**:
- Placeholder dates: `YYYY-MM-DD`, `2025-XX-XX`
- Hardcoded dates: `2024-11-19` (when it's actually 2025)
- Copied dates from other files
- Remembered dates from memory
- Old dates from previous commits

### 3. ALWAYS Update These Fields

✅ **REQUIRED** in every documentation update:

```markdown
---
last_updated: "2025-11-19"  # ✅ Actual current date
---

> **Last Updated**: 2025-11-19  # ✅ Actual current date

**Date**: 2025-11-19  # ✅ Actual current date

| Date | Change |
|------|--------|
| 2025-11-19 | Updated content |  # ✅ Actual current date
```

---

## 📋 CHECKLIST - Use This Every Time

Before committing any documentation:

- [ ] **Step 1**: Executed `date +%Y-%m-%d` to get current date
- [ ] **Step 2**: Updated `last_updated` field in frontmatter
- [ ] **Step 3**: Updated `Last Updated` in document header
- [ ] **Step 4**: Updated date in change history table
- [ ] **Step 5**: Verified all dates match the ACTUAL current date
- [ ] **Step 6**: No placeholder dates (YYYY-MM-DD) remain
- [ ] **Step 7**: No hardcoded old dates remain

---

## ✅ CORRECT EXAMPLES

### Example 1: Frontmatter

```markdown
---
title: "Feature Documentation"
last_updated: "2025-11-19"  # ✅ Got from `date +%Y-%m-%d`
version: "1.0"
---
```

### Example 2: Document Header

```markdown
# Feature Documentation

**Last Updated**: 2025-11-19  # ✅ Got from `date +%Y-%m-%d`
**Version**: 1.0
```

### Example 3: Change History

```markdown
## Change History

| Date | Version | Changes |
|------|---------|---------|
| 2025-11-19 | 1.1 | Updated examples |  # ✅ Got from `date +%Y-%m-%d`
| 2025-11-15 | 1.0 | Initial version |
```

### Example 4: ADR

```markdown
# ADR-001: Architecture Decision

**Date**: 2025-11-19  # ✅ Got from `date +%Y-%m-%d`
**Status**: Accepted
```

---

## ❌ INCORRECT EXAMPLES

### Example 1: Placeholder Date

```markdown
---
last_updated: "YYYY-MM-DD"  # ❌ FORBIDDEN - Placeholder
---
```

### Example 2: Hardcoded Old Date

```markdown
> **Last Updated**: 2024-11-19  # ❌ FORBIDDEN - Wrong year
```

### Example 3: Copied Date

```markdown
# Copied from another file
**Date**: 2025-01-15  # ❌ FORBIDDEN - Not current date
```

---

## 🎯 WHY THIS MATTERS

### Problems Caused by Wrong Dates

1. **Confusion**: Users don't know if content is current
2. **Maintenance**: Can't identify outdated documentation
3. **Audit**: Compliance and tracking issues
4. **Trust**: Unprofessional appearance
5. **Coordination**: Team can't track changes properly

### Benefits of Correct Dates

1. ✅ **Clarity**: Everyone knows content freshness
2. ✅ **Tracking**: Easy to identify outdated content
3. ✅ **Compliance**: Proper audit trails
4. ✅ **Professionalism**: Shows attention to detail
5. ✅ **Coordination**: Team knows what's current

---

## 🔧 AUTOMATION HELPERS

### Git Pre-commit Hook

Add this to `.git/hooks/pre-commit`:

```bash
#!/bin/bash

# Check for placeholder dates in staged markdown files
if git diff --cached --name-only | grep -q '\.md$'; then
    if git diff --cached | grep -q 'YYYY-MM-DD'; then
        echo "❌ ERROR: Found placeholder date 'YYYY-MM-DD' in documentation"
        echo "Please use actual current date from: date +%Y-%m-%d"
        exit 1
    fi
fi
```

### VS Code Snippet

Add to `.vscode/markdown.json`:

```json
{
  "Current Date": {
    "prefix": "date",
    "body": ["$CURRENT_YEAR-$CURRENT_MONTH-$CURRENT_DATE"],
    "description": "Insert current date"
  }
}
```

### Shell Alias

Add to `~/.bashrc` or `~/.zshrc`:

```bash
alias today='date +%Y-%m-%d'
```

---

## 📝 WORKFLOW

### Creating New Documentation

```bash
# 1. Get current date
CURRENT_DATE=$(date +%Y-%m-%d)
echo "Today is: $CURRENT_DATE"

# 2. Create file with current date
cat > new-doc.md << EOF
---
title: "New Documentation"
last_updated: "$CURRENT_DATE"
---

# New Documentation

**Last Updated**: $CURRENT_DATE

## Content

...
EOF

# 3. Verify date is correct
grep -E "last_updated|Last Updated" new-doc.md
```

### Updating Existing Documentation

```bash
# 1. Get current date
CURRENT_DATE=$(date +%Y-%m-%d)
echo "Today is: $CURRENT_DATE"

# 2. Edit file and update dates manually

# 3. Before commit, verify all dates
grep -E "last_updated|Last Updated|Date:" file.md

# 4. Confirm all dates match current date
```

---

## 🚨 COMMON MISTAKES TO AVOID

### Mistake 1: Using Memory

```markdown
# ❌ WRONG - Using remembered date
**Last Updated**: 2025-11-15  # I think it was last week...

# ✅ CORRECT - Using actual current date
**Last Updated**: 2025-11-19  # From `date +%Y-%m-%d`
```

### Mistake 2: Copying from Other Files

```markdown
# ❌ WRONG - Copied from another file
**Last Updated**: 2025-10-22  # Copied from other-doc.md

# ✅ CORRECT - Using actual current date
**Last Updated**: 2025-11-19  # From `date +%Y-%m-%d`
```

### Mistake 3: Using Wrong Year

```markdown
# ❌ WRONG - Wrong year
**Last Updated**: 2024-11-19  # It's actually 2025!

# ✅ CORRECT - Correct year
**Last Updated**: 2025-11-19  # From `date +%Y-%m-%d`
```

### Mistake 4: Leaving Placeholders

```markdown
# ❌ WRONG - Placeholder not replaced
**Last Updated**: YYYY-MM-DD

# ✅ CORRECT - Actual date
**Last Updated**: 2025-11-19  # From `date +%Y-%m-%d`
```

---

## 📊 VERIFICATION

### Before Committing

Run these checks:

```bash
# Check for placeholder dates
grep -r "YYYY-MM-DD" docs/ .kiro/

# Check for 2024 dates (if current year is 2025)
grep -r "2024-" docs/ .kiro/ | grep -E "last_updated|Last Updated"

# Verify current date
date +%Y-%m-%d
```

### During Code Review

Reviewers should check:

- [ ] All dates are actual current dates
- [ ] No placeholder dates (YYYY-MM-DD)
- [ ] No old hardcoded dates
- [ ] Dates match commit date

---

## 🎓 TRAINING

### For New Contributors

1. **Read this file first** before any documentation work
2. **Bookmark** this file for quick reference
3. **Practice** getting current date: `date +%Y-%m-%d`
4. **Verify** dates before every commit

### For Reviewers

1. **Check dates** in every documentation PR
2. **Reject** PRs with wrong dates
3. **Educate** contributors about date requirements
4. **Reference** this file in review comments

---

## 📞 QUESTIONS?

If you're unsure about dates:

1. **Always** run `date +%Y-%m-%d` first
2. **Never** guess or use memory
3. **Ask** if you're confused
4. **Reference** this file

---

## 🔗 RELATED STANDARDS

- [Development Standards](development-standards.md) - Full development guidelines
- [Code Quality Checklist](code-quality-checklist.md) - Quality checks
- [Code Review Standards](code-review-standards.md) - Review process

---

**Document Version**: 1.0  
**Last Updated**: 2025-11-19  
**Owner**: Documentation Team  
**Status**: ✅ MANDATORY - Must be followed by ALL contributors

---

## 💡 REMEMBER

> **🚨 BEFORE ANY DOCUMENTATION WORK:**
> 
> ```bash
> date +%Y-%m-%d
> ```
> 
> **Use that EXACT date in ALL date fields!**

---

**This is not optional. This is MANDATORY.**
