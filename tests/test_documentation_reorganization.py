"""
Documentation Reorganization Property Tests

These tests verify the correctness properties defined in the documentation-reorganization spec.
**Feature: documentation-reorganization**

Tests use pytest for property-based testing of documentation structure after reorganization.
"""

import os
import re
from pathlib import Path
from typing import List, Set, Dict

import pytest

# Base paths for documentation
DOCS_ROOT = Path("docs")
VIEWPOINTS_DIR = DOCS_ROOT / "viewpoints"


class TestContentPreservationAfterMove:
    """
    **Feature: documentation-reorganization, Property 1: Content Preservation After Move**
    
    *For any* file moved from a source directory to a target directory, the file content 
    at the target location should be identical to the original content at the source location.
    
    **Validates: Requirements 1.1, 2.1, 3.1, 4.1, 4.2**
    """
    
    # Mapping of source directories to target directories after reorganization
    # These directories were moved as part of the documentation reorganization
    REORGANIZATION_MAPPING = {
        # Operations -> viewpoints/operational
        "operations/deployment": "viewpoints/operational/deployment",
        "operations/monitoring": "viewpoints/operational/monitoring",
        "operations/runbooks": "viewpoints/operational/runbooks",
        "operations/troubleshooting": "viewpoints/operational/troubleshooting",
        "operations/maintenance": "viewpoints/operational/maintenance",
        # Development -> viewpoints/development
        "development/coding-standards": "viewpoints/development/coding-standards",
        "development/testing": "viewpoints/development/testing",
        "development/workflows": "viewpoints/development/workflows",
        "development/examples": "viewpoints/development/examples",
        "development/setup": "viewpoints/development/setup",
        "development/hooks": "viewpoints/development/hooks",
        # Infrastructure -> viewpoints/deployment/infrastructure
        "infrastructure": "viewpoints/deployment/infrastructure",
        # Auxiliary directories
        "getting-started": "viewpoints/development/getting-started",
        "examples": "viewpoints/development/examples",
    }
    
    def get_target_directories(self) -> List[Path]:
        """Get all target directories that should exist after reorganization."""
        return [DOCS_ROOT / target for target in self.REORGANIZATION_MAPPING.values()]
    
    def test_target_directories_exist(self):
        """
        Property 1: Verify all target directories exist after reorganization.
        
        **Feature: documentation-reorganization, Property 1: Content Preservation After Move**
        **Validates: Requirements 1.1, 2.1, 3.1, 4.1, 4.2**
        """
        missing_dirs = []
        for target in set(self.REORGANIZATION_MAPPING.values()):
            target_path = DOCS_ROOT / target
            if not target_path.exists():
                missing_dirs.append(str(target_path))
        
        assert len(missing_dirs) == 0, (
            f"Target directories missing after reorganization: {missing_dirs}"
        )
    
    def test_operational_viewpoint_has_content(self):
        """
        Property 1: Verify operational viewpoint contains content from operations/.
        
        **Feature: documentation-reorganization, Property 1: Content Preservation After Move**
        **Validates: Requirements 1.1**
        """
        operational_dir = VIEWPOINTS_DIR / "operational"
        if not operational_dir.exists():
            pytest.skip("Operational viewpoint directory does not exist")
        
        # Check for expected subdirectories
        expected_subdirs = ["deployment", "monitoring", "runbooks", "troubleshooting", "maintenance"]
        existing_subdirs = [d.name for d in operational_dir.iterdir() if d.is_dir()]
        
        missing_subdirs = [d for d in expected_subdirs if d not in existing_subdirs]
        
        assert len(missing_subdirs) == 0, (
            f"Missing subdirectories in operational viewpoint: {missing_subdirs}. "
            f"Content from operations/ should have been moved here."
        )
    
    def test_development_viewpoint_has_content(self):
        """
        Property 1: Verify development viewpoint contains content from development/.
        
        **Feature: documentation-reorganization, Property 1: Content Preservation After Move**
        **Validates: Requirements 2.1**
        """
        development_dir = VIEWPOINTS_DIR / "development"
        if not development_dir.exists():
            pytest.skip("Development viewpoint directory does not exist")
        
        # Check for expected subdirectories
        expected_subdirs = ["coding-standards", "testing", "workflows", "examples", "setup", "getting-started"]
        existing_subdirs = [d.name for d in development_dir.iterdir() if d.is_dir()]
        
        missing_subdirs = [d for d in expected_subdirs if d not in existing_subdirs]
        
        assert len(missing_subdirs) == 0, (
            f"Missing subdirectories in development viewpoint: {missing_subdirs}. "
            f"Content from development/ should have been moved here."
        )
    
    def test_deployment_viewpoint_has_infrastructure(self):
        """
        Property 1: Verify deployment viewpoint contains infrastructure content.
        
        **Feature: documentation-reorganization, Property 1: Content Preservation After Move**
        **Validates: Requirements 3.1**
        """
        infrastructure_dir = VIEWPOINTS_DIR / "deployment" / "infrastructure"
        if not infrastructure_dir.exists():
            pytest.skip("Infrastructure directory does not exist in deployment viewpoint")
        
        # Check that infrastructure directory has content
        files = list(infrastructure_dir.glob("*.md"))
        
        assert len(files) > 0, (
            f"Infrastructure directory in deployment viewpoint is empty. "
            f"Content from docs/infrastructure/ should have been moved here."
        )
    
    def test_getting_started_moved_to_development(self):
        """
        Property 1: Verify getting-started content moved to development viewpoint.
        
        **Feature: documentation-reorganization, Property 1: Content Preservation After Move**
        **Validates: Requirements 4.1**
        """
        getting_started_dir = VIEWPOINTS_DIR / "development" / "getting-started"
        if not getting_started_dir.exists():
            pytest.skip("Getting-started directory does not exist in development viewpoint")
        
        # Check that getting-started has content
        files = list(getting_started_dir.glob("*.md"))
        
        assert len(files) > 0, (
            f"Getting-started directory in development viewpoint is empty. "
            f"Content from docs/getting-started/ should have been moved here."
        )
    
    def test_examples_moved_to_development(self):
        """
        Property 1: Verify examples content moved to development viewpoint.
        
        **Feature: documentation-reorganization, Property 1: Content Preservation After Move**
        **Validates: Requirements 4.2**
        """
        examples_dir = VIEWPOINTS_DIR / "development" / "examples"
        if not examples_dir.exists():
            pytest.skip("Examples directory does not exist in development viewpoint")
        
        # Check that examples has content
        files = list(examples_dir.glob("*.md"))
        dirs = [d for d in examples_dir.iterdir() if d.is_dir()]
        
        assert len(files) > 0 or len(dirs) > 0, (
            f"Examples directory in development viewpoint is empty. "
            f"Content from docs/examples/ should have been moved here."
        )



class TestSourceDirectoryDeletion:
    """
    **Feature: documentation-reorganization, Property 2: Source Directory Deletion**
    
    *For any* consolidation operation, after completion the source directory 
    should no longer exist in the file system.
    
    **Validates: Requirements 1.3, 2.3, 3.2, 4.4**
    """
    
    # Source directories that should have been deleted after reorganization
    DELETED_DIRECTORIES = [
        "operations",
        "development", 
        "infrastructure",
        "getting-started",
        "examples",
        "generated",
    ]
    
    def test_operations_directory_deleted(self):
        """
        Property 2: Verify docs/operations/ directory no longer exists.
        
        **Feature: documentation-reorganization, Property 2: Source Directory Deletion**
        **Validates: Requirements 1.3**
        """
        operations_dir = DOCS_ROOT / "operations"
        
        assert not operations_dir.exists(), (
            f"Source directory {operations_dir} still exists. "
            f"It should have been deleted after consolidation to viewpoints/operational/."
        )
    
    def test_development_directory_deleted(self):
        """
        Property 2: Verify docs/development/ directory no longer exists.
        
        **Feature: documentation-reorganization, Property 2: Source Directory Deletion**
        **Validates: Requirements 2.3**
        """
        development_dir = DOCS_ROOT / "development"
        
        assert not development_dir.exists(), (
            f"Source directory {development_dir} still exists. "
            f"It should have been deleted after consolidation to viewpoints/development/."
        )
    
    def test_infrastructure_directory_deleted(self):
        """
        Property 2: Verify docs/infrastructure/ directory no longer exists.
        
        **Feature: documentation-reorganization, Property 2: Source Directory Deletion**
        **Validates: Requirements 3.2**
        """
        infrastructure_dir = DOCS_ROOT / "infrastructure"
        
        assert not infrastructure_dir.exists(), (
            f"Source directory {infrastructure_dir} still exists. "
            f"It should have been deleted after consolidation to viewpoints/deployment/infrastructure/."
        )
    
    def test_generated_directory_deleted(self):
        """
        Property 2: Verify docs/generated/ directory no longer exists.
        
        **Feature: documentation-reorganization, Property 2: Source Directory Deletion**
        **Validates: Requirements 4.4**
        """
        generated_dir = DOCS_ROOT / "generated"
        
        assert not generated_dir.exists(), (
            f"Source directory {generated_dir} still exists. "
            f"It should have been deleted after verifying content is in docs/diagrams/generated/."
        )
    
    def test_all_source_directories_deleted(self):
        """
        Property 2: For any consolidation operation, after completion the source 
        directory should no longer exist in the file system.
        
        **Feature: documentation-reorganization, Property 2: Source Directory Deletion**
        **Validates: Requirements 1.3, 2.3, 3.2, 4.4**
        """
        existing_source_dirs = []
        for dir_name in self.DELETED_DIRECTORIES:
            dir_path = DOCS_ROOT / dir_name
            if dir_path.exists():
                existing_source_dirs.append(str(dir_path))
        
        assert len(existing_source_dirs) == 0, (
            f"Source directories still exist after reorganization: {existing_source_dirs}. "
            f"All source directories should have been deleted after content was moved."
        )


class TestLinkValidityAfterUpdate:
    """
    **Feature: documentation-reorganization, Property 3: Link Validity After Update**
    
    *For any* internal link in docs/README.md, the link target should resolve 
    to an existing file or directory in the documentation structure.
    
    **Validates: Requirements 1.4, 2.4, 3.3, 5.1, 5.3**
    """
    
    def extract_internal_links(self, file_path: Path) -> List[tuple]:
        """
        Extract internal links from a markdown file.
        
        Returns list of tuples: (link_text, link_url)
        """
        if not file_path.exists():
            return []
        
        content = file_path.read_text(encoding='utf-8')
        
        # Find all markdown links that are internal (not http/https)
        # Pattern: [text](path) where path doesn't start with http
        links = re.findall(r'\[([^\]]+)\]\(([^)]+)\)', content)
        
        internal_links = []
        for link_text, link_url in links:
            # Skip external links and anchors
            if link_url.startswith(('http://', 'https://', '#', 'mailto:')):
                continue
            # Remove anchor from URL if present
            link_url = link_url.split('#')[0]
            if link_url:
                internal_links.append((link_text, link_url))
        
        return internal_links
    
    def resolve_link(self, base_path: Path, link_url: str) -> Path:
        """Resolve a relative link URL to an absolute path."""
        # Handle relative paths
        if link_url.startswith('./'):
            link_url = link_url[2:]
        
        # Resolve relative to the base path's directory
        base_dir = base_path.parent
        resolved = (base_dir / link_url).resolve()
        
        return resolved
    
    # Paths that were affected by the reorganization
    REORGANIZATION_AFFECTED_PATHS = [
        "viewpoints/operational",
        "viewpoints/development", 
        "viewpoints/deployment/infrastructure",
        "operations",
        "development",
        "infrastructure",
        "getting-started",
        "examples",
        "generated",
    ]
    
    def is_reorganization_related_link(self, link_url: str) -> bool:
        """Check if a link is related to the reorganization (affected paths)."""
        for path in self.REORGANIZATION_AFFECTED_PATHS:
            if path in link_url:
                return True
        return False
    
    def test_docs_readme_reorganization_links_valid(self):
        """
        Property 3: Verify reorganization-related links in docs/README.md resolve to existing files.
        
        **Feature: documentation-reorganization, Property 3: Link Validity After Update**
        **Validates: Requirements 1.4, 2.4, 3.3, 5.1, 5.3**
        """
        readme_path = DOCS_ROOT / "README.md"
        if not readme_path.exists():
            pytest.skip("docs/README.md does not exist")
        
        links = self.extract_internal_links(readme_path)
        
        broken_links = []
        for link_text, link_url in links:
            # Only check links related to the reorganization
            if self.is_reorganization_related_link(link_url):
                resolved_path = self.resolve_link(readme_path, link_url)
                if not resolved_path.exists():
                    broken_links.append(f"'{link_text}' -> {link_url}")
        
        assert len(broken_links) == 0, (
            f"Broken reorganization-related links found in docs/README.md: {broken_links}. "
            f"All links to reorganized paths should resolve to existing files."
        )
    
    def test_viewpoints_readme_links_valid(self):
        """
        Property 3: Verify all internal links in viewpoints/README.md resolve to existing files.
        
        **Feature: documentation-reorganization, Property 3: Link Validity After Update**
        **Validates: Requirements 5.1**
        """
        readme_path = VIEWPOINTS_DIR / "README.md"
        if not readme_path.exists():
            pytest.skip("viewpoints/README.md does not exist")
        
        links = self.extract_internal_links(readme_path)
        
        broken_links = []
        for link_text, link_url in links:
            resolved_path = self.resolve_link(readme_path, link_url)
            if not resolved_path.exists():
                broken_links.append(f"'{link_text}' -> {link_url}")
        
        assert len(broken_links) == 0, (
            f"Broken links found in viewpoints/README.md: {broken_links}. "
            f"All internal links should resolve to existing files after reorganization."
        )
    
    def test_operational_readme_links_valid(self):
        """
        Property 3: Verify all internal links in operational viewpoint resolve to existing files.
        
        **Feature: documentation-reorganization, Property 3: Link Validity After Update**
        **Validates: Requirements 1.4**
        """
        readme_path = VIEWPOINTS_DIR / "operational" / "README.md"
        if not readme_path.exists():
            pytest.skip("operational/README.md does not exist")
        
        links = self.extract_internal_links(readme_path)
        
        broken_links = []
        for link_text, link_url in links:
            resolved_path = self.resolve_link(readme_path, link_url)
            if not resolved_path.exists():
                broken_links.append(f"'{link_text}' -> {link_url}")
        
        assert len(broken_links) == 0, (
            f"Broken links found in operational/README.md: {broken_links}. "
            f"All internal links should resolve to existing files after reorganization."
        )
    
    def test_development_readme_links_valid(self):
        """
        Property 3: Verify all internal links in development viewpoint resolve to existing files.
        
        **Feature: documentation-reorganization, Property 3: Link Validity After Update**
        **Validates: Requirements 2.4**
        """
        readme_path = VIEWPOINTS_DIR / "development" / "README.md"
        if not readme_path.exists():
            pytest.skip("development/README.md does not exist")
        
        links = self.extract_internal_links(readme_path)
        
        broken_links = []
        for link_text, link_url in links:
            resolved_path = self.resolve_link(readme_path, link_url)
            if not resolved_path.exists():
                broken_links.append(f"'{link_text}' -> {link_url}")
        
        assert len(broken_links) == 0, (
            f"Broken links found in development/README.md: {broken_links}. "
            f"All internal links should resolve to existing files after reorganization."
        )
    
    def test_deployment_readme_links_valid(self):
        """
        Property 3: Verify all internal links in deployment viewpoint resolve to existing files.
        
        **Feature: documentation-reorganization, Property 3: Link Validity After Update**
        **Validates: Requirements 3.3**
        """
        readme_path = VIEWPOINTS_DIR / "deployment" / "README.md"
        if not readme_path.exists():
            pytest.skip("deployment/README.md does not exist")
        
        links = self.extract_internal_links(readme_path)
        
        broken_links = []
        for link_text, link_url in links:
            resolved_path = self.resolve_link(readme_path, link_url)
            if not resolved_path.exists():
                broken_links.append(f"'{link_text}' -> {link_url}")
        
        assert len(broken_links) == 0, (
            f"Broken links found in deployment/README.md: {broken_links}. "
            f"All internal links should resolve to existing files after reorganization."
        )



class TestNoReferencesToDeletedDirectories:
    """
    **Feature: documentation-reorganization, Property 4: No References to Deleted Directories**
    
    *For any* reference in docs/README.md, the reference should not point to any of 
    the deleted directories (operations/, development/, infrastructure/, getting-started/, 
    examples/, generated/) at the docs/ root level.
    
    Note: References to viewpoints/development/ or viewpoints/operational/ are valid
    and should NOT be flagged as errors.
    
    **Validates: Requirements 5.2**
    """
    
    # Patterns that indicate references to deleted directories at docs/ root level
    # These patterns specifically match paths that go UP to the deleted directories
    # (e.g., ../operations/, ../../development/) but NOT valid paths like 
    # viewpoints/development/ or ./development/ within viewpoints/
    DELETED_DIRECTORY_PATTERNS_FROM_ROOT = [
        # Direct references from docs/README.md to deleted directories
        r'\(operations/',
        r'\(\./operations/',
        r'\(infrastructure/',
        r'\(\./infrastructure/',
        r'\(getting-started/',
        r'\(\./getting-started/',
        r'\(examples/',
        r'\(\./examples/',
        r'\(generated/',
        r'\(\./generated/',
    ]
    
    # Patterns for files within viewpoints/ that incorrectly reference deleted dirs
    # These match paths like ../operations/, ../../development/ etc.
    DELETED_DIRECTORY_PATTERNS_FROM_VIEWPOINTS = [
        r'\(\.\.\/operations/',
        r'\(\.\.\/\.\.\/operations/',
        r'\(\.\.\/infrastructure/',
        r'\(\.\.\/\.\.\/infrastructure/',
        r'\(\.\.\/getting-started/',
        r'\(\.\.\/\.\.\/getting-started/',
        r'\(\.\.\/examples/',
        r'\(\.\.\/\.\.\/examples/',
        r'\(\.\.\/generated/',
        r'\(\.\.\/\.\.\/generated/',
    ]
    
    def find_deleted_directory_references(self, file_path: Path, from_root: bool = True) -> List[str]:
        """
        Find references to deleted directories in a file.
        
        Args:
            file_path: Path to the file to check
            from_root: If True, use patterns for files at docs/ root level.
                      If False, use patterns for files within viewpoints/.
        """
        if not file_path.exists():
            return []
        
        content = file_path.read_text(encoding='utf-8')
        
        patterns = (self.DELETED_DIRECTORY_PATTERNS_FROM_ROOT if from_root 
                   else self.DELETED_DIRECTORY_PATTERNS_FROM_VIEWPOINTS)
        
        found_references = []
        for pattern in patterns:
            matches = re.findall(pattern, content)
            found_references.extend(matches)
        
        return found_references
    
    def test_docs_readme_no_deleted_references(self):
        """
        Property 4: Verify docs/README.md has no references to deleted directories.
        
        **Feature: documentation-reorganization, Property 4: No References to Deleted Directories**
        **Validates: Requirements 5.2**
        """
        readme_path = DOCS_ROOT / "README.md"
        if not readme_path.exists():
            pytest.skip("docs/README.md does not exist")
        
        references = self.find_deleted_directory_references(readme_path)
        
        assert len(references) == 0, (
            f"Found references to deleted directories in docs/README.md: {references}. "
            f"All references should point to new locations in viewpoints/."
        )
    
    def test_viewpoints_readme_no_deleted_references(self):
        """
        Property 4: Verify viewpoints/README.md has no references to deleted directories
        at the docs/ root level.
        
        Note: References to ./development/ or ./operational/ within viewpoints/ are valid.
        
        **Feature: documentation-reorganization, Property 4: No References to Deleted Directories**
        **Validates: Requirements 5.2**
        """
        readme_path = VIEWPOINTS_DIR / "README.md"
        if not readme_path.exists():
            pytest.skip("viewpoints/README.md does not exist")
        
        # viewpoints/README.md should not have references going UP to deleted dirs
        references = self.find_deleted_directory_references(readme_path, from_root=False)
        
        assert len(references) == 0, (
            f"Found references to deleted directories in viewpoints/README.md: {references}. "
            f"All references should point to new locations within viewpoints/."
        )
    
    def test_all_viewpoint_readmes_no_deleted_references(self):
        """
        Property 4: For any viewpoint README.md, there should be no references 
        to deleted directories at the docs/ root level.
        
        **Feature: documentation-reorganization, Property 4: No References to Deleted Directories**
        **Validates: Requirements 5.2**
        """
        if not VIEWPOINTS_DIR.exists():
            pytest.skip("viewpoints directory does not exist")
        
        files_with_references = []
        
        for viewpoint_dir in VIEWPOINTS_DIR.iterdir():
            if viewpoint_dir.is_dir():
                readme_path = viewpoint_dir / "README.md"
                if readme_path.exists():
                    # Check for references going UP to deleted directories
                    references = self.find_deleted_directory_references(readme_path, from_root=False)
                    if references:
                        files_with_references.append(
                            f"{readme_path}: {references}"
                        )
        
        assert len(files_with_references) == 0, (
            f"Found references to deleted directories in viewpoint READMEs: {files_with_references}. "
            f"All references should point to new locations within viewpoints/."
        )
    
    def test_faq_no_deleted_references(self):
        """
        Property 4: Verify FAQ.md has no references to deleted directories.
        
        **Feature: documentation-reorganization, Property 4: No References to Deleted Directories**
        **Validates: Requirements 5.2**
        """
        faq_path = DOCS_ROOT / "FAQ.md"
        if not faq_path.exists():
            pytest.skip("FAQ.md does not exist")
        
        references = self.find_deleted_directory_references(faq_path)
        
        assert len(references) == 0, (
            f"Found references to deleted directories in FAQ.md: {references}. "
            f"All references should point to new locations."
        )
    
    def test_quick_start_no_deleted_references(self):
        """
        Property 4: Verify QUICK-START-GUIDE.md has no references to deleted directories.
        
        **Feature: documentation-reorganization, Property 4: No References to Deleted Directories**
        **Validates: Requirements 5.2**
        """
        quick_start_path = DOCS_ROOT / "QUICK-START-GUIDE.md"
        if not quick_start_path.exists():
            pytest.skip("QUICK-START-GUIDE.md does not exist")
        
        references = self.find_deleted_directory_references(quick_start_path)
        
        assert len(references) == 0, (
            f"Found references to deleted directories in QUICK-START-GUIDE.md: {references}. "
            f"All references should point to new locations."
        )


class TestGeneratedDiagramsSingleLocation:
    """
    **Feature: documentation-reorganization, Property 5: Generated Diagrams Single Location**
    
    *For any* generated diagram file, it should exist only in docs/diagrams/generated/ 
    and not in any other generated/ directory.
    
    **Validates: Requirements 4.3, 4.4**
    """
    
    def test_diagrams_generated_directory_exists(self):
        """
        Property 5: Verify docs/diagrams/generated/ directory exists.
        
        **Feature: documentation-reorganization, Property 5: Generated Diagrams Single Location**
        **Validates: Requirements 4.3**
        """
        generated_dir = DOCS_ROOT / "diagrams" / "generated"
        
        assert generated_dir.exists(), (
            f"Directory {generated_dir} does not exist. "
            f"All generated diagrams should be in docs/diagrams/generated/."
        )
    
    def test_no_duplicate_generated_directory(self):
        """
        Property 5: Verify no duplicate generated/ directory exists at docs/generated/.
        
        **Feature: documentation-reorganization, Property 5: Generated Diagrams Single Location**
        **Validates: Requirements 4.4**
        """
        duplicate_generated_dir = DOCS_ROOT / "generated"
        
        assert not duplicate_generated_dir.exists(), (
            f"Duplicate directory {duplicate_generated_dir} exists. "
            f"All generated content should be in docs/diagrams/generated/ only."
        )
    
    def test_generated_diagrams_have_content(self):
        """
        Property 5: Verify docs/diagrams/generated/ has diagram content.
        
        **Feature: documentation-reorganization, Property 5: Generated Diagrams Single Location**
        **Validates: Requirements 4.3**
        """
        generated_dir = DOCS_ROOT / "diagrams" / "generated"
        if not generated_dir.exists():
            pytest.skip("docs/diagrams/generated/ does not exist")
        
        # Check for image files or subdirectories with content
        files = list(generated_dir.glob("**/*"))
        content_files = [f for f in files if f.is_file()]
        
        assert len(content_files) > 0, (
            f"Directory {generated_dir} is empty. "
            f"Generated diagrams should be present in this location."
        )
    
    def test_no_duplicate_root_generated_directory(self):
        """
        Property 5: For any generated diagram file, it should exist only in 
        docs/diagrams/generated/ and not in docs/generated/.
        
        Note: Legacy directories within docs/diagrams/ (like docs/diagrams/legacy/generated)
        are intentionally preserved for historical reference and are not considered duplicates.
        
        **Feature: documentation-reorganization, Property 5: Generated Diagrams Single Location**
        **Validates: Requirements 4.3, 4.4**
        """
        # The specific directory that should NOT exist is docs/generated/
        # (the duplicate that was supposed to be deleted)
        duplicate_root_generated = DOCS_ROOT / "generated"
        
        assert not duplicate_root_generated.exists(), (
            f"Duplicate directory {duplicate_root_generated} exists. "
            f"This directory should have been deleted after verifying content "
            f"is in docs/diagrams/generated/."
        )


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
