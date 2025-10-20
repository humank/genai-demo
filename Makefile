# Project Makefile
# Convenient commands for development workflow

.PHONY: help validate generate diagrams pre-commit setup-hooks clean-hooks

# Default target
help:
	@echo "Available commands:"
	@echo ""
	@echo "📊 Diagram Commands:"
	@echo "  make validate     - Validate all diagrams and references"
	@echo "  make generate     - Generate all diagrams from PlantUML sources"
	@echo "  make diagrams     - Validate and generate diagrams"
	@echo ""
	@echo "🔧 Development Commands:"
	@echo "  make pre-commit   - Run pre-commit checks (validate + generate)"
	@echo "  make setup-hooks  - Set up Git hooks for validation"
	@echo "  make clean-hooks  - Remove Git hooks"
	@echo ""
	@echo "📝 Documentation Commands:"
	@echo "  make docs         - Generate documentation (future)"
	@echo ""
	@echo "💡 Examples:"
	@echo "  make validate     # Check all diagram references"
	@echo "  make generate     # Generate PNG diagrams"
	@echo "  make pre-commit   # Full pre-commit validation"

# Diagram validation
validate:
	@echo "🔍 Validating diagrams and references..."
	@./scripts/validate-diagrams.sh

# Diagram generation
generate:
	@echo "🎨 Generating diagrams..."
	@./scripts/generate-diagrams.sh --format=png

# Combined diagram operations
diagrams: validate generate
	@echo "✅ Diagram validation and generation completed"

# Pre-commit checks (what should run before committing)
pre-commit: validate generate
	@echo "✅ Pre-commit checks completed successfully"
	@echo "💡 Ready to commit!"

# Set up Git hooks
setup-hooks:
	@echo "🔧 Setting up Git hooks..."
	@./scripts/setup-git-hooks.sh

# Clean Git hooks
clean-hooks:
	@echo "🧹 Removing Git hooks..."
	@rm -f .git/hooks/pre-commit
	@rm -f .git/hooks/commit-msg
	@rm -f .git/hooks/pre-push
	@echo "✅ Git hooks removed"

# Documentation generation (placeholder for future)
docs:
	@echo "📚 Documentation generation not implemented yet"
	@echo "💡 Future: Generate API docs, architecture docs, etc."

# Validate specific diagram
validate-diagram:
	@if [ -z "$(FILE)" ]; then \
		echo "❌ Please specify a file: make validate-diagram FILE=path/to/diagram.puml"; \
		exit 1; \
	fi
	@echo "🔍 Validating specific diagram: $(FILE)"
	@./scripts/validate-diagrams.sh --check-syntax $(FILE)

# Generate specific diagram
generate-diagram:
	@if [ -z "$(FILE)" ]; then \
		echo "❌ Please specify a file: make generate-diagram FILE=path/to/diagram.puml"; \
		exit 1; \
	fi
	@echo "🎨 Generating specific diagram: $(FILE)"
	@./scripts/generate-diagrams.sh --format=png $(FILE)

# Quick status check
status:
	@echo "📊 Project Status:"
	@echo ""
	@echo "🔧 Hooks:"
	@if [ -f ".git/hooks/pre-commit" ]; then \
		echo "  ✅ pre-commit hook installed"; \
	else \
		echo "  ❌ pre-commit hook not installed (run 'make setup-hooks')"; \
	fi
	@if [ -f ".git/hooks/pre-push" ]; then \
		echo "  ✅ pre-push hook installed"; \
	else \
		echo "  ❌ pre-push hook not installed (run 'make setup-hooks')"; \
	fi
	@echo ""
	@echo "📊 Diagrams:"
	@puml_count=$$(find docs/diagrams -name "*.puml" 2>/dev/null | wc -l); \
	png_count=$$(find docs/diagrams -name "*.png" 2>/dev/null | wc -l); \
	echo "  📝 PlantUML sources: $$puml_count"; \
	echo "  🖼️  Generated PNGs: $$png_count"
	@echo ""
	@echo "🎯 Quick Actions:"
	@echo "  make validate     # Check everything"
	@echo "  make generate     # Generate missing diagrams"
	@echo "  make pre-commit   # Full pre-commit check"

# Development workflow helpers
dev-setup: setup-hooks
	@echo "🚀 Development environment setup completed!"
	@echo ""
	@echo "📋 Next steps:"
	@echo "  1. Edit PlantUML files in docs/diagrams/"
	@echo "  2. Kiro hook will auto-generate PNG files"
	@echo "  3. Run 'make validate' before committing"
	@echo "  4. Git hooks will validate on commit/push"

# Clean generated files (use with caution)
clean-generated:
	@echo "⚠️  This will delete all generated diagram files!"
	@read -p "Are you sure? (y/N): " confirm && [ "$$confirm" = "y" ]
	@echo "🧹 Cleaning generated diagrams..."
	@find docs/diagrams -name "*.png" -delete 2>/dev/null || true
	@find docs/diagrams -name "*.svg" -delete 2>/dev/null || true
	@echo "✅ Generated diagrams cleaned"
	@echo "💡 Run 'make generate' to regenerate them"
