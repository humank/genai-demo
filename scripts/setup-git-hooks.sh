#!/bin/bash
# Git Hooks Setup Script
# This script sets up Git pre-commit hooks as alternatives to Kiro hooks

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if we're in a git repository
if [ ! -d ".git" ]; then
    print_error "Not in a git repository. Please run this from the project root."
    exit 1
fi

print_info "Setting up Git hooks as alternatives to Kiro hooks..."

# Create pre-commit hook
PRE_COMMIT_HOOK=".git/hooks/pre-commit"
print_info "Creating pre-commit hook: $PRE_COMMIT_HOOK"

cat > "$PRE_COMMIT_HOOK" << 'EOF'
#!/bin/bash
# Pre-commit hook for diagram validation
# Alternative to diagram-validation.kiro.hook

set -e

echo "🔍 Running pre-commit validation..."

# Check if validation script exists
if [ ! -f "./scripts/validate-diagrams.sh" ]; then
    echo "❌ Validation script not found: ./scripts/validate-diagrams.sh"
    exit 1
fi

# Run diagram validation
echo "📊 Validating diagrams and references..."
if ./scripts/validate-diagrams.sh --check-references; then
    echo "✅ All diagram references are valid"
else
    echo "❌ Diagram validation failed"
    echo "💡 Fix the issues above or use 'git commit --no-verify' to skip validation"
    exit 1
fi

# Check for missing generated diagrams
echo "🔍 Checking for missing generated diagrams..."
if ./scripts/validate-diagrams.sh --check-missing; then
    echo "✅ All diagrams are generated"
else
    echo "⚠️  Some diagrams are missing"
    echo "💡 Run './scripts/generate-diagrams.sh' to generate missing diagrams"
    echo "💡 Or use 'git commit --no-verify' to skip this check"
    # Don't fail for missing diagrams, just warn
fi

echo "🎉 Pre-commit validation completed successfully!"
EOF

# Make pre-commit hook executable
chmod +x "$PRE_COMMIT_HOOK"
print_success "Created executable pre-commit hook"

# Create commit-msg hook for commit message validation
COMMIT_MSG_HOOK=".git/hooks/commit-msg"
print_info "Creating commit-msg hook: $COMMIT_MSG_HOOK"

cat > "$COMMIT_MSG_HOOK" << 'EOF'
#!/bin/bash
# Commit message hook
# Validates commit message format

commit_regex='^(feat|fix|docs|style|refactor|test|chore|perf)(\(.+\))?: .{1,50}'

error_msg="❌ Invalid commit message format!

Expected format: <type>(<scope>): <description>

Types:
  feat:     New feature
  fix:      Bug fix
  docs:     Documentation changes
  style:    Code style changes (formatting, etc.)
  refactor: Code refactoring
  test:     Adding or updating tests
  chore:    Maintenance tasks
  perf:     Performance improvements

Examples:
  feat(auth): add user authentication
  fix(api): resolve timeout issue
  docs(hooks): update hook documentation
  refactor(diagrams): simplify generation logic

Your commit message:
$(cat $1)
"

if ! grep -qE "$commit_regex" "$1"; then
    echo "$error_msg"
    exit 1
fi
EOF

chmod +x "$COMMIT_MSG_HOOK"
print_success "Created executable commit-msg hook"

# Create pre-push hook for additional validation
PRE_PUSH_HOOK=".git/hooks/pre-push"
print_info "Creating pre-push hook: $PRE_PUSH_HOOK"

cat > "$PRE_PUSH_HOOK" << 'EOF'
#!/bin/bash
# Pre-push hook for comprehensive validation
# Runs more thorough checks before pushing

set -e

echo "🚀 Running pre-push validation..."

# Run comprehensive diagram validation
echo "📊 Running comprehensive diagram validation..."
if ./scripts/validate-diagrams.sh; then
    echo "✅ All diagram validations passed"
else
    echo "❌ Diagram validation failed"
    echo "💡 Fix the issues above or use 'git push --no-verify' to skip validation"
    exit 1
fi

# Generate any missing diagrams
echo "🎨 Ensuring all diagrams are generated..."
if ./scripts/generate-diagrams.sh --format=png; then
    echo "✅ All diagrams generated successfully"
else
    echo "❌ Diagram generation failed"
    echo "💡 Fix the issues above or use 'git push --no-verify' to skip validation"
    exit 1
fi

echo "🎉 Pre-push validation completed successfully!"
EOF

chmod +x "$PRE_PUSH_HOOK"
print_success "Created executable pre-push hook"

# Summary
echo ""
print_success "Git hooks setup completed!"
echo ""
print_info "Created hooks:"
echo "  📝 pre-commit:  Validates diagram references"
echo "  💬 commit-msg:  Validates commit message format"
echo "  🚀 pre-push:    Comprehensive validation and generation"
echo ""
print_info "To bypass hooks (if needed):"
echo "  git commit --no-verify"
echo "  git push --no-verify"
echo ""
print_info "To test hooks manually:"
echo "  .git/hooks/pre-commit"
echo "  .git/hooks/pre-push"
echo ""
print_warning "Note: These hooks are local to your repository."
print_warning "Team members need to run this script to set up their own hooks."
