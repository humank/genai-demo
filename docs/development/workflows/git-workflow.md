# Git Workflow Guide

> **Last Updated**: 2025-10-25

## Overview

This document defines the Git workflow and branching strategy for the Enterprise E-Commerce Platform. Following these guidelines ensures consistent collaboration and code quality across the team.

## Branching Strategy

### Branch Types

We use a simplified Git Flow model with the following branch types:

#### 1. Main Branch (`main`)

**Purpose**: Production-ready code

**Characteristics:**

- Always deployable
- Protected branch (requires PR and reviews)
- Automatically deployed to production
- Never commit directly to main

**Protection Rules:**

- Require pull request reviews (minimum 2 approvals)
- Require status checks to pass
- Require branches to be up to date
- Restrict who can push to matching branches

#### 2. Development Branch (`develop`)

**Purpose**: Integration branch for features

**Characteristics:**

- Latest development changes
- Protected branch (requires PR)
- Automatically deployed to staging
- Base branch for feature branches

#### 3. Feature Branches (`feature/*`)

**Purpose**: New features or enhancements

**Naming Convention**: `feature/TICKET-ID-short-description`

**Examples:**

- `feature/JIRA-123-customer-registration`
- `feature/JIRA-456-order-discount-calculation`

**Lifecycle:**

1. Branch from `develop`
2. Develop feature
3. Create PR to `develop`
4. Delete after merge

#### 4. Bugfix Branches (`bugfix/*`)

**Purpose**: Bug fixes for development

**Naming Convention**: `bugfix/TICKET-ID-short-description`

**Examples:**

- `bugfix/JIRA-789-fix-email-validation`
- `bugfix/JIRA-101-correct-price-calculation`

**Lifecycle:**

1. Branch from `develop`
2. Fix bug
3. Create PR to `develop`
4. Delete after merge

#### 5. Hotfix Branches (`hotfix/*`)

**Purpose**: Critical fixes for production

**Naming Convention**: `hotfix/TICKET-ID-short-description`

**Examples:**

- `hotfix/JIRA-999-fix-payment-gateway-error`
- `hotfix/JIRA-888-security-vulnerability-patch`

**Lifecycle:**

1. Branch from `main`
2. Fix critical issue
3. Create PR to `main` AND `develop`
4. Delete after merge

#### 6. Release Branches (`release/*`)

**Purpose**: Prepare for production release

**Naming Convention**: `release/v{major}.{minor}.{patch}`

**Examples:**

- `release/v1.2.0`
- `release/v2.0.0`

**Lifecycle:**

1. Branch from `develop`
2. Final testing and bug fixes
3. Merge to `main` and tag
4. Merge back to `develop`
5. Delete after merge

## Workflow Diagrams

### Feature Development Flow

```text
develop
  │
  ├─── feature/JIRA-123-new-feature
  │         │
  │         │ (development)
  │         │
  │         │ (PR created)
  │         │
  │    ┌────┘
  │◄───┤ (merge after review)
  │    └────
  │
```

### Hotfix Flow

```text
main
  │
  ├─── hotfix/JIRA-999-critical-fix
  │         │
  │         │ (fix applied)
  │         │
  │    ┌────┘
  │◄───┤ (merge to main)
  │    └────
  │
develop
  │
  │    ┌──── (also merge to develop)
  │◄───┤
  │    └────
```

### Release Flow

```text
develop
  │
  ├─── release/v1.2.0
  │         │
  │         │ (testing & bug fixes)
  │         │
  │    ┌────┘
  │◄───┤ (merge back to develop)
  │    └────
  │
main
  │
  │    ┌──── (merge to main & tag)
  │◄───┤
  │    └──── v1.2.0
```

## Commit Message Conventions

### Format

```text
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- **feat**: New feature
- **fix**: Bug fix
- **docs**: Documentation changes
- **style**: Code style changes (formatting, missing semicolons, etc.)
- **refactor**: Code refactoring
- **test**: Adding or updating tests
- **chore**: Maintenance tasks (build, dependencies, etc.)
- **perf**: Performance improvements
- **ci**: CI/CD changes

### Scope

The scope should indicate the affected area:

- `customer`: Customer-related changes
- `order`: Order-related changes
- `product`: Product-related changes
- `payment`: Payment-related changes
- `api`: API changes
- `db`: Database changes
- `config`: Configuration changes

### Subject

- Use imperative mood ("add" not "added" or "adds")
- Don't capitalize first letter
- No period at the end
- Maximum 50 characters

### Body

- Explain what and why, not how
- Wrap at 72 characters
- Separate from subject with blank line

### Footer

- Reference issues: `Closes #123`, `Fixes #456`
- Breaking changes: `BREAKING CHANGE: description`

### Examples

**Simple commit:**

```text
feat(customer): add email validation for customer registration

Implement email format validation using regex pattern to ensure
valid email addresses during customer registration process.

Closes #123
```

**Bug fix:**

```text
fix(order): correct total calculation for discounted items

Fixed an issue where discount was not properly applied to order
total when multiple items had different discount rates.

Fixes #456
```

**Breaking change:**

```text
refactor(api): change customer API response format

BREAKING CHANGE: Customer API now returns nested address object
instead of flat structure. Clients need to update their code.

Before:
{
  "id": "123",
  "name": "John",
  "street": "Main St",
  "city": "NYC"
}

After:
{
  "id": "123",
  "name": "John",
  "address": {
    "street": "Main St",
    "city": "NYC"
  }
}

Closes #789
```

## Pull Request Process

### Creating a Pull Request

#### 1. Prepare Your Branch

```bash
# Ensure your branch is up to date
git checkout develop
git pull origin develop

# Rebase your feature branch
git checkout feature/JIRA-123-new-feature
git rebase develop

# Run tests
./gradlew test

# Push your changes
git push origin feature/JIRA-123-new-feature
```

#### 2. Create PR on GitHub

**PR Title Format:**

```json
[TYPE] TICKET-ID: Brief description
```

**Examples:**

- `[FEAT] JIRA-123: Add customer registration feature`
- `[FIX] JIRA-456: Fix order total calculation`
- `[REFACTOR] JIRA-789: Improve payment service architecture`

**PR Description Template:**

```markdown
## Description
Brief description of the changes

## Type of Change

- [ ] New feature
- [ ] Bug fix
- [ ] Breaking change
- [ ] Documentation update

## Related Issues
Closes #123

## Changes Made

- Change 1
- Change 2
- Change 3

## Testing

- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing completed

## Screenshots (if applicable)
[Add screenshots for UI changes]

## Checklist

- [ ] Code follows project coding standards
- [ ] Self-review completed
- [ ] Comments added for complex logic
- [ ] Documentation updated
- [ ] No new warnings generated
- [ ] Tests pass locally
- [ ] Dependent changes merged

```

#### 3. Request Reviews

- Assign at least 2 reviewers
- Add relevant labels
- Link related issues
- Add to project board

### Reviewing Pull Requests

#### Review Checklist

**Code Quality:**

- [ ] Code follows coding standards
- [ ] No code duplication
- [ ] Proper error handling
- [ ] Appropriate logging
- [ ] No security vulnerabilities

**Testing:**

- [ ] Tests are included
- [ ] Tests are meaningful
- [ ] Tests pass
- [ ] Coverage is adequate

**Documentation:**

- [ ] Code is self-documenting
- [ ] Complex logic is commented
- [ ] API documentation updated
- [ ] README updated if needed

**Architecture:**

- [ ] Follows project architecture
- [ ] No architectural violations
- [ ] Proper layer separation
- [ ] DDD patterns followed

#### Review Comments

**Use Conventional Comments:**

- `nit:` Minor issue, not blocking
- `question:` Asking for clarification
- `suggestion:` Suggesting an improvement
- `issue:` Blocking issue that must be fixed
- `praise:` Positive feedback

**Examples:**

```yaml
nit: Consider using a more descriptive variable name here

question: Why did you choose this approach over X?

suggestion: This could be simplified using Java streams

issue: This will cause a NullPointerException if customer is null

praise: Great use of the builder pattern here!
```

### Merging Pull Requests

#### Merge Strategies

**1. Squash and Merge (Preferred)**

- Combines all commits into one
- Keeps history clean
- Use for feature branches

```bash
# GitHub will do this automatically when you select "Squash and merge"
```

**2. Rebase and Merge**

- Maintains individual commits
- Linear history
- Use for small, well-organized commits

**3. Merge Commit**

- Creates a merge commit
- Preserves all commits
- Use for release branches

#### After Merge

```bash
# Delete remote branch (done automatically by GitHub)

# Delete local branch
git checkout develop
git pull origin develop
git branch -d feature/JIRA-123-new-feature

# Clean up remote tracking branches
git fetch --prune
```

## Common Git Commands

### Daily Workflow

```bash
# Start new feature
git checkout develop
git pull origin develop
git checkout -b feature/JIRA-123-new-feature

# Make changes and commit
git add .
git commit -m "feat(customer): add email validation"

# Push to remote
git push origin feature/JIRA-123-new-feature

# Update from develop
git checkout develop
git pull origin develop
git checkout feature/JIRA-123-new-feature
git rebase develop

# Interactive rebase to clean up commits
git rebase -i develop
```

### Useful Commands

```bash
# View commit history
git log --oneline --graph --all

# View changes
git diff
git diff --staged

# Stash changes
git stash
git stash pop
git stash list

# Undo last commit (keep changes)
git reset --soft HEAD~1

# Undo last commit (discard changes)
git reset --hard HEAD~1

# Amend last commit
git commit --amend

# Cherry-pick a commit
git cherry-pick <commit-hash>

# View branch information
git branch -a
git branch -vv

# Clean up local branches
git branch --merged | grep -v "\*" | xargs -n 1 git branch -d
```

## Conflict Resolution

### Resolving Merge Conflicts

```bash
# Update your branch
git checkout feature/JIRA-123-new-feature
git fetch origin
git rebase origin/develop

# If conflicts occur
# 1. Open conflicted files
# 2. Resolve conflicts manually
# 3. Mark as resolved
git add <resolved-file>

# Continue rebase
git rebase --continue

# Or abort if needed
git rebase --abort
```

### Conflict Markers

```java
<<<<<<< HEAD
// Your changes
public void processOrder(Order order) {
    validateOrder(order);
    calculateTotal(order);
}
=======
// Incoming changes
public void processOrder(Order order) {
    validateOrder(order);
    applyDiscounts(order);
    calculateTotal(order);
}
>>>>>>> develop
```

**Resolution:**

```java
// Resolved version
public void processOrder(Order order) {
    validateOrder(order);
    applyDiscounts(order);
    calculateTotal(order);
}
```

## Best Practices

### Do's

✅ **Commit Often**: Make small, logical commits
✅ **Write Clear Messages**: Follow commit message conventions
✅ **Keep Branches Updated**: Regularly rebase from develop
✅ **Review Your Own Code**: Self-review before creating PR
✅ **Test Before Pushing**: Run tests locally
✅ **Clean Up Branches**: Delete merged branches
✅ **Use Descriptive Branch Names**: Include ticket ID and description

### Don'ts

❌ **Don't Commit to Main**: Always use feature branches
❌ **Don't Force Push**: Unless you're sure no one else is using the branch
❌ **Don't Commit Large Files**: Use Git LFS for large files
❌ **Don't Commit Secrets**: Use environment variables
❌ **Don't Mix Concerns**: One feature/fix per branch
❌ **Don't Skip Tests**: Always run tests before pushing
❌ **Don't Ignore Conflicts**: Resolve conflicts properly

## Git Configuration

### Recommended Git Config

```bash
# Set your identity
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"

# Set default branch name
git config --global init.defaultBranch main

# Enable color output
git config --global color.ui auto

# Set default editor
git config --global core.editor "code --wait"

# Enable credential helper
git config --global credential.helper osxkeychain

# Set pull strategy
git config --global pull.rebase true

# Enable auto-stash during rebase
git config --global rebase.autoStash true

# Set default merge strategy
git config --global merge.ff false

# Enable rerere (reuse recorded resolution)
git config --global rerere.enabled true
```

### Git Aliases

```bash
# Add useful aliases
git config --global alias.co checkout
git config --global alias.br branch
git config --global alias.ci commit
git config --global alias.st status
git config --global alias.unstage 'reset HEAD --'
git config --global alias.last 'log -1 HEAD'
git config --global alias.visual 'log --oneline --graph --all'
git config --global alias.amend 'commit --amend --no-edit'
```

## Troubleshooting

### Common Issues

**Issue: Accidentally committed to wrong branch**

```bash
# Move commits to correct branch
git checkout correct-branch
git cherry-pick <commit-hash>

# Remove from wrong branch
git checkout wrong-branch
git reset --hard HEAD~1
```

**Issue: Need to undo last commit**

```bash
# Keep changes
git reset --soft HEAD~1

# Discard changes
git reset --hard HEAD~1
```

**Issue: Pushed sensitive data**

```bash
# Remove from history (use with caution)
git filter-branch --force --index-filter \
  "git rm --cached --ignore-unmatch path/to/sensitive/file" \
  --prune-empty --tag-name-filter cat -- --all

# Force push (coordinate with team)
git push origin --force --all
```


**Document Version**: 1.0  
**Last Updated**: 2025-10-25  
**Maintained By**: Development Team
