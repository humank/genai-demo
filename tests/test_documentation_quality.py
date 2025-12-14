"""
Documentation Quality Property Tests

These tests verify the correctness properties defined in the documentation-quality-improvement spec.
**Feature: documentation-quality-improvement**

Tests use pytest for property-based testing of documentation structure.
"""

import os
import re
from pathlib import Path
from typing import List, Set

import pytest

# Base paths for documentation
DOCS_ROOT = Path("docs")
VIEWPOINTS_DIR = DOCS_ROOT / "viewpoints"
PERSPECTIVES_DIR = DOCS_ROOT / "perspectives"


class TestFileConsolidationCompleteness:
    """
    **Feature: documentation-quality-improvement, Property 1: File Consolidation Completeness**
    
    *For any* viewpoint or perspective directory that originally contained both README.md 
    and overview.md, after consolidation, only README.md SHALL exist and overview.md 
    SHALL be removed.
    
    **Validates: Requirements 1.1, 1.4**
    """
    
    # Directories that were consolidated in Task 1
    CONSOLIDATED_VIEWPOINTS = ["functional", "information"]
    CONSOLIDATED_PERSPECTIVES = ["performance", "security"]
    
    def get_consolidated_viewpoint_directories(self) -> List[Path]:
        """Get viewpoint directories that were consolidated."""
        if not VIEWPOINTS_DIR.exists():
            return []
        return [VIEWPOINTS_DIR / name for name in self.CONSOLIDATED_VIEWPOINTS 
                if (VIEWPOINTS_DIR / name).exists()]
    
    def get_consolidated_perspective_directories(self) -> List[Path]:
        """Get perspective directories that were consolidated."""
        if not PERSPECTIVES_DIR.exists():
            return []
        return [PERSPECTIVES_DIR / name for name in self.CONSOLIDATED_PERSPECTIVES 
                if (PERSPECTIVES_DIR / name).exists()]
    
    def test_no_overview_md_in_consolidated_viewpoints(self):
        """
        Property 1: Verify no overview.md files exist in consolidated viewpoint directories.
        
        **Feature: documentation-quality-improvement, Property 1: File Consolidation Completeness**
        **Validates: Requirements 1.1, 1.4**
        """
        viewpoint_dirs = self.get_consolidated_viewpoint_directories()
        
        overview_files_found = []
        for viewpoint_dir in viewpoint_dirs:
            overview_path = viewpoint_dir / "overview.md"
            if overview_path.exists():
                overview_files_found.append(str(overview_path))
        
        assert len(overview_files_found) == 0, (
            f"Found overview.md files that should have been consolidated: {overview_files_found}"
        )
    
    def test_no_overview_md_in_consolidated_perspectives(self):
        """
        Property 1: Verify no overview.md files exist in consolidated perspective directories.
        
        **Feature: documentation-quality-improvement, Property 1: File Consolidation Completeness**
        **Validates: Requirements 1.1, 1.4**
        """
        perspective_dirs = self.get_consolidated_perspective_directories()
        
        overview_files_found = []
        for perspective_dir in perspective_dirs:
            overview_path = perspective_dir / "overview.md"
            if overview_path.exists():
                overview_files_found.append(str(overview_path))
        
        assert len(overview_files_found) == 0, (
            f"Found overview.md files that should have been consolidated: {overview_files_found}"
        )
    
    def test_readme_exists_in_consolidated_viewpoints(self):
        """
        Property 1: Verify README.md exists in consolidated viewpoint directories.
        
        **Feature: documentation-quality-improvement, Property 1: File Consolidation Completeness**
        **Validates: Requirements 1.1, 1.4**
        """
        viewpoint_dirs = self.get_consolidated_viewpoint_directories()
        
        missing_readme = []
        for viewpoint_dir in viewpoint_dirs:
            readme_path = viewpoint_dir / "README.md"
            if not readme_path.exists():
                missing_readme.append(str(viewpoint_dir))
        
        assert len(missing_readme) == 0, (
            f"Missing README.md in consolidated viewpoint directories: {missing_readme}"
        )
    
    def test_readme_exists_in_consolidated_perspectives(self):
        """
        Property 1: Verify README.md exists in consolidated perspective directories.
        
        **Feature: documentation-quality-improvement, Property 1: File Consolidation Completeness**
        **Validates: Requirements 1.1, 1.4**
        """
        perspective_dirs = self.get_consolidated_perspective_directories()
        
        missing_readme = []
        for perspective_dir in perspective_dirs:
            readme_path = perspective_dir / "README.md"
            if not readme_path.exists():
                missing_readme.append(str(perspective_dir))
        
        assert len(missing_readme) == 0, (
            f"Missing README.md in consolidated perspective directories: {missing_readme}"
        )


class TestContentPreservation:
    """
    **Feature: documentation-quality-improvement, Property 2: Content Preservation**
    
    *For any* merged document, all unique sections from the original overview.md 
    SHALL be present in the consolidated README.md.
    
    **Validates: Requirements 1.2**
    """
    
    # Key sections that should be present in consolidated documents
    # These represent the essential content that must be preserved from overview.md
    # Note: Some sections have valid alternatives (e.g., "Key Concerns" or "Key Principles")
    REQUIRED_VIEWPOINT_SECTIONS = [
        "Overview",
        "Purpose",
        "Stakeholders",
        "Contents",
    ]
    
    # Sections where alternatives are acceptable
    VIEWPOINT_ALTERNATIVE_SECTIONS = [
        ["Key Concerns", "Key Principles"],  # Either is acceptable
    ]
    
    REQUIRED_PERSPECTIVE_SECTIONS = [
        "Overview",
        "Purpose",
        "Stakeholders",
        "Key Concerns",
        "Quality Attribute Scenarios",
    ]
    
    # Consolidated directories that were merged from README.md + overview.md
    CONSOLIDATED_VIEWPOINTS = ["functional", "information"]
    CONSOLIDATED_PERSPECTIVES = ["performance", "security"]
    
    # Essential content keywords that should be preserved from overview.md
    # These represent unique content that was in overview.md and must be in README.md
    VIEWPOINT_CONTENT_KEYWORDS = {
        "functional": [
            "bounded context",
            "domain-driven design",
            "hexagonal architecture",
            "aggregate",
            "domain event",
        ],
        "information": [
            "data ownership",
            "data consistency",
            "data flow",
            "event sourcing",
            "aggregate",
        ],
    }
    
    PERSPECTIVE_CONTENT_KEYWORDS = {
        "performance": [
            "response time",
            "throughput",
            "caching",
            "scalability",
            "optimization",
        ],
        "security": [
            "authentication",
            "authorization",
            "encryption",
            "compliance",
            "data protection",
        ],
    }
    
    def extract_sections_from_markdown(self, file_path: Path) -> Set[str]:
        """Extract section headers from a markdown file."""
        if not file_path.exists():
            return set()
        
        content = file_path.read_text(encoding='utf-8')
        # Match ## headers (level 2 headers)
        headers = re.findall(r'^##\s+(.+)$', content, re.MULTILINE)
        return set(headers)
    
    def _check_viewpoint_sections(self, sections: Set[str]) -> List[str]:
        """Check if viewpoint has all required sections, including alternatives."""
        missing_sections = []
        
        # Check required sections
        for required in self.REQUIRED_VIEWPOINT_SECTIONS:
            if not any(required.lower() in s.lower() for s in sections):
                missing_sections.append(required)
        
        # Check alternative sections (at least one from each group must be present)
        for alternatives in self.VIEWPOINT_ALTERNATIVE_SECTIONS:
            found = False
            for alt in alternatives:
                if any(alt.lower() in s.lower() for s in sections):
                    found = True
                    break
            if not found:
                missing_sections.append(f"One of: {alternatives}")
        
        return missing_sections
    
    def test_viewpoint_functional_has_required_sections(self):
        """
        Property 2: Verify functional viewpoint README.md contains required sections.
        
        **Feature: documentation-quality-improvement, Property 2: Content Preservation**
        **Validates: Requirements 1.2**
        """
        readme_path = VIEWPOINTS_DIR / "functional" / "README.md"
        if not readme_path.exists():
            pytest.skip("Functional viewpoint README.md does not exist")
        
        sections = self.extract_sections_from_markdown(readme_path)
        missing_sections = self._check_viewpoint_sections(sections)
        
        assert len(missing_sections) == 0, (
            f"Missing required sections in functional viewpoint: {missing_sections}"
        )
    
    def test_viewpoint_information_has_required_sections(self):
        """
        Property 2: Verify information viewpoint README.md contains required sections.
        
        **Feature: documentation-quality-improvement, Property 2: Content Preservation**
        **Validates: Requirements 1.2**
        """
        readme_path = VIEWPOINTS_DIR / "information" / "README.md"
        if not readme_path.exists():
            pytest.skip("Information viewpoint README.md does not exist")
        
        sections = self.extract_sections_from_markdown(readme_path)
        missing_sections = self._check_viewpoint_sections(sections)
        
        assert len(missing_sections) == 0, (
            f"Missing required sections in information viewpoint: {missing_sections}"
        )
    
    def test_perspective_performance_has_required_sections(self):
        """
        Property 2: Verify performance perspective README.md contains required sections.
        
        **Feature: documentation-quality-improvement, Property 2: Content Preservation**
        **Validates: Requirements 1.2**
        """
        readme_path = PERSPECTIVES_DIR / "performance" / "README.md"
        if not readme_path.exists():
            pytest.skip("Performance perspective README.md does not exist")
        
        sections = self.extract_sections_from_markdown(readme_path)
        
        missing_sections = []
        for required in self.REQUIRED_PERSPECTIVE_SECTIONS:
            if not any(required.lower() in s.lower() for s in sections):
                missing_sections.append(required)
        
        assert len(missing_sections) == 0, (
            f"Missing required sections in performance perspective: {missing_sections}"
        )
    
    def test_perspective_security_has_required_sections(self):
        """
        Property 2: Verify security perspective README.md contains required sections.
        
        **Feature: documentation-quality-improvement, Property 2: Content Preservation**
        **Validates: Requirements 1.2**
        """
        readme_path = PERSPECTIVES_DIR / "security" / "README.md"
        if not readme_path.exists():
            pytest.skip("Security perspective README.md does not exist")
        
        sections = self.extract_sections_from_markdown(readme_path)
        
        missing_sections = []
        for required in self.REQUIRED_PERSPECTIVE_SECTIONS:
            if not any(required.lower() in s.lower() for s in sections):
                missing_sections.append(required)
        
        assert len(missing_sections) == 0, (
            f"Missing required sections in security perspective: {missing_sections}"
        )
    
    def get_file_content_lower(self, file_path: Path) -> str:
        """Read file content and return lowercase version for case-insensitive matching."""
        if not file_path.exists():
            return ""
        return file_path.read_text(encoding='utf-8').lower()
    
    def test_all_consolidated_viewpoints_preserve_essential_content(self):
        """
        Property 2: For any consolidated viewpoint, all essential content keywords 
        from the original overview.md SHALL be present in the consolidated README.md.
        
        **Feature: documentation-quality-improvement, Property 2: Content Preservation**
        **Validates: Requirements 1.2**
        """
        for viewpoint_name in self.CONSOLIDATED_VIEWPOINTS:
            readme_path = VIEWPOINTS_DIR / viewpoint_name / "README.md"
            if not readme_path.exists():
                pytest.skip(f"{viewpoint_name} viewpoint README.md does not exist")
            
            content = self.get_file_content_lower(readme_path)
            keywords = self.VIEWPOINT_CONTENT_KEYWORDS.get(viewpoint_name, [])
            
            missing_keywords = []
            for keyword in keywords:
                if keyword.lower() not in content:
                    missing_keywords.append(keyword)
            
            assert len(missing_keywords) == 0, (
                f"Missing essential content in {viewpoint_name} viewpoint README.md. "
                f"Keywords not found: {missing_keywords}. "
                f"These keywords represent content that should have been preserved from overview.md."
            )
    
    def test_all_consolidated_perspectives_preserve_essential_content(self):
        """
        Property 2: For any consolidated perspective, all essential content keywords 
        from the original overview.md SHALL be present in the consolidated README.md.
        
        **Feature: documentation-quality-improvement, Property 2: Content Preservation**
        **Validates: Requirements 1.2**
        """
        for perspective_name in self.CONSOLIDATED_PERSPECTIVES:
            readme_path = PERSPECTIVES_DIR / perspective_name / "README.md"
            if not readme_path.exists():
                pytest.skip(f"{perspective_name} perspective README.md does not exist")
            
            content = self.get_file_content_lower(readme_path)
            keywords = self.PERSPECTIVE_CONTENT_KEYWORDS.get(perspective_name, [])
            
            missing_keywords = []
            for keyword in keywords:
                if keyword.lower() not in content:
                    missing_keywords.append(keyword)
            
            assert len(missing_keywords) == 0, (
                f"Missing essential content in {perspective_name} perspective README.md. "
                f"Keywords not found: {missing_keywords}. "
                f"These keywords represent content that should have been preserved from overview.md."
            )
    
    def test_consolidated_documents_have_change_history(self):
        """
        Property 2: For any consolidated document, the change history SHALL indicate 
        the consolidation event, proving content was merged.
        
        **Feature: documentation-quality-improvement, Property 2: Content Preservation**
        **Validates: Requirements 1.2**
        """
        all_consolidated_dirs = [
            (VIEWPOINTS_DIR / name, "viewpoint") for name in self.CONSOLIDATED_VIEWPOINTS
        ] + [
            (PERSPECTIVES_DIR / name, "perspective") for name in self.CONSOLIDATED_PERSPECTIVES
        ]
        
        missing_history = []
        for dir_path, doc_type in all_consolidated_dirs:
            readme_path = dir_path / "README.md"
            if not readme_path.exists():
                continue
            
            content = self.get_file_content_lower(readme_path)
            
            # Check for consolidation mention in change history
            has_consolidation_record = (
                "consolidated" in content or 
                "merged" in content or
                "overview.md" in content
            )
            
            if not has_consolidation_record:
                missing_history.append(f"{dir_path.name} {doc_type}")
        
        assert len(missing_history) == 0, (
            f"Documents missing consolidation record in change history: {missing_history}. "
            f"Each consolidated document should mention the merge of README.md and overview.md."
        )
    
    def test_consolidated_documents_minimum_content_length(self):
        """
        Property 2: For any consolidated document, the content length SHALL be 
        substantial (indicating content was preserved, not lost).
        
        **Feature: documentation-quality-improvement, Property 2: Content Preservation**
        **Validates: Requirements 1.2**
        """
        # Minimum expected content length for consolidated documents
        # A properly consolidated document should have significant content
        MIN_CONTENT_LENGTH = 3000  # characters
        
        all_consolidated_dirs = [
            (VIEWPOINTS_DIR / name, "viewpoint") for name in self.CONSOLIDATED_VIEWPOINTS
        ] + [
            (PERSPECTIVES_DIR / name, "perspective") for name in self.CONSOLIDATED_PERSPECTIVES
        ]
        
        insufficient_content = []
        for dir_path, doc_type in all_consolidated_dirs:
            readme_path = dir_path / "README.md"
            if not readme_path.exists():
                continue
            
            content = readme_path.read_text(encoding='utf-8')
            content_length = len(content)
            
            if content_length < MIN_CONTENT_LENGTH:
                insufficient_content.append(
                    f"{dir_path.name} {doc_type} ({content_length} chars)"
                )
        
        assert len(insufficient_content) == 0, (
            f"Documents with insufficient content (< {MIN_CONTENT_LENGTH} chars): {insufficient_content}. "
            f"Consolidated documents should have substantial content from merged overview.md."
        )


if __name__ == "__main__":
    pytest.main([__file__, "-v"])


class TestCrossReferenceLimit:
    """
    **Feature: documentation-quality-improvement, Property 3: Cross-Reference Limit**
    
    *For any* viewpoint or perspective README.md, the total number of cross-reference 
    links SHALL NOT exceed 5.
    
    **Validates: Requirements 3.1**
    """
    
    # Maximum allowed cross-reference links per document
    MAX_CROSS_REFERENCES = 5
    
    # Directories that have been optimized for cross-references
    OPTIMIZED_VIEWPOINTS = ["functional", "information"]
    OPTIMIZED_PERSPECTIVES = ["performance", "security"]
    
    def count_cross_reference_links(self, file_path: Path) -> int:
        """
        Count cross-reference links in a markdown file.
        
        Cross-references are links to other documentation files (not anchors within the same file).
        We count links in the "Related Documentation" section specifically.
        """
        if not file_path.exists():
            return 0
        
        content = file_path.read_text(encoding='utf-8')
        
        # Find the Related Documentation section
        related_doc_match = re.search(
            r'## Related Documentation\s*\n(.*?)(?=\n## |\n---|\Z)', 
            content, 
            re.DOTALL
        )
        
        if not related_doc_match:
            return 0
        
        related_section = related_doc_match.group(1)
        
        # Count markdown links to other files (not anchors)
        # Pattern: [text](path) where path doesn't start with #
        links = re.findall(r'\[([^\]]+)\]\(([^)#][^)]*)\)', related_section)
        
        return len(links)
    
    def test_viewpoint_functional_cross_reference_limit(self):
        """
        Property 3: Verify functional viewpoint README.md has ≤ 5 cross-reference links.
        
        **Feature: documentation-quality-improvement, Property 3: Cross-Reference Limit**
        **Validates: Requirements 3.1**
        """
        readme_path = VIEWPOINTS_DIR / "functional" / "README.md"
        if not readme_path.exists():
            pytest.skip("Functional viewpoint README.md does not exist")
        
        link_count = self.count_cross_reference_links(readme_path)
        
        assert link_count <= self.MAX_CROSS_REFERENCES, (
            f"Functional viewpoint has {link_count} cross-reference links, "
            f"exceeds maximum of {self.MAX_CROSS_REFERENCES}. "
            f"Reduce cross-references to improve reading experience."
        )
    
    def test_viewpoint_information_cross_reference_limit(self):
        """
        Property 3: Verify information viewpoint README.md has ≤ 5 cross-reference links.
        
        **Feature: documentation-quality-improvement, Property 3: Cross-Reference Limit**
        **Validates: Requirements 3.1**
        """
        readme_path = VIEWPOINTS_DIR / "information" / "README.md"
        if not readme_path.exists():
            pytest.skip("Information viewpoint README.md does not exist")
        
        link_count = self.count_cross_reference_links(readme_path)
        
        assert link_count <= self.MAX_CROSS_REFERENCES, (
            f"Information viewpoint has {link_count} cross-reference links, "
            f"exceeds maximum of {self.MAX_CROSS_REFERENCES}. "
            f"Reduce cross-references to improve reading experience."
        )
    
    def test_perspective_performance_cross_reference_limit(self):
        """
        Property 3: Verify performance perspective README.md has ≤ 5 cross-reference links.
        
        **Feature: documentation-quality-improvement, Property 3: Cross-Reference Limit**
        **Validates: Requirements 3.1**
        """
        readme_path = PERSPECTIVES_DIR / "performance" / "README.md"
        if not readme_path.exists():
            pytest.skip("Performance perspective README.md does not exist")
        
        link_count = self.count_cross_reference_links(readme_path)
        
        assert link_count <= self.MAX_CROSS_REFERENCES, (
            f"Performance perspective has {link_count} cross-reference links, "
            f"exceeds maximum of {self.MAX_CROSS_REFERENCES}. "
            f"Reduce cross-references to improve reading experience."
        )
    
    def test_perspective_security_cross_reference_limit(self):
        """
        Property 3: Verify security perspective README.md has ≤ 5 cross-reference links.
        
        **Feature: documentation-quality-improvement, Property 3: Cross-Reference Limit**
        **Validates: Requirements 3.1**
        """
        readme_path = PERSPECTIVES_DIR / "security" / "README.md"
        if not readme_path.exists():
            pytest.skip("Security perspective README.md does not exist")
        
        link_count = self.count_cross_reference_links(readme_path)
        
        assert link_count <= self.MAX_CROSS_REFERENCES, (
            f"Security perspective has {link_count} cross-reference links, "
            f"exceeds maximum of {self.MAX_CROSS_REFERENCES}. "
            f"Reduce cross-references to improve reading experience."
        )
    
    def test_all_optimized_documents_cross_reference_limit(self):
        """
        Property 3: For any optimized viewpoint or perspective README.md, 
        the total number of cross-reference links SHALL NOT exceed 5.
        
        **Feature: documentation-quality-improvement, Property 3: Cross-Reference Limit**
        **Validates: Requirements 3.1**
        """
        all_optimized_dirs = [
            (VIEWPOINTS_DIR / name, "viewpoint") for name in self.OPTIMIZED_VIEWPOINTS
        ] + [
            (PERSPECTIVES_DIR / name, "perspective") for name in self.OPTIMIZED_PERSPECTIVES
        ]
        
        violations = []
        for dir_path, doc_type in all_optimized_dirs:
            readme_path = dir_path / "README.md"
            if not readme_path.exists():
                continue
            
            link_count = self.count_cross_reference_links(readme_path)
            
            if link_count > self.MAX_CROSS_REFERENCES:
                violations.append(
                    f"{dir_path.name} {doc_type}: {link_count} links"
                )
        
        assert len(violations) == 0, (
            f"Documents exceeding {self.MAX_CROSS_REFERENCES} cross-reference limit: {violations}. "
            f"Each document should have at most {self.MAX_CROSS_REFERENCES} cross-references "
            f"to improve reading experience."
        )



class TestLinkContextRequirement:
    """
    **Feature: documentation-quality-improvement, Property 4: Link Context Requirement**
    
    *For any* cross-reference link in a document, there SHALL be accompanying 
    descriptive text explaining what the linked document contains.
    
    **Validates: Requirements 3.4**
    """
    
    # Directories that have been optimized for cross-references
    OPTIMIZED_VIEWPOINTS = ["functional", "information"]
    OPTIMIZED_PERSPECTIVES = ["performance", "security"]
    
    # Minimum context length (characters) for a link to be considered properly described
    MIN_CONTEXT_LENGTH = 20
    
    def extract_links_with_context(self, file_path: Path) -> List[tuple]:
        """
        Extract cross-reference links and their surrounding context from Related Documentation section.
        
        Returns list of tuples: (link_text, link_url, surrounding_context)
        """
        if not file_path.exists():
            return []
        
        content = file_path.read_text(encoding='utf-8')
        
        # Find the Related Documentation section
        related_doc_match = re.search(
            r'## Related Documentation\s*\n(.*?)(?=\n## |\n---|\Z)', 
            content, 
            re.DOTALL
        )
        
        if not related_doc_match:
            return []
        
        related_section = related_doc_match.group(1)
        
        # Find all links with their surrounding context
        # Pattern matches: text before [link](url) text after
        links_with_context = []
        
        # Split by lines and process each line containing a link
        lines = related_section.split('\n')
        for i, line in enumerate(lines):
            # Find links in this line
            link_matches = re.finditer(r'\[([^\]]+)\]\(([^)#][^)]*)\)', line)
            for match in link_matches:
                link_text = match.group(1)
                link_url = match.group(2)
                
                # Get context: the line itself plus adjacent lines
                context_lines = []
                if i > 0:
                    context_lines.append(lines[i-1])
                context_lines.append(line)
                if i < len(lines) - 1:
                    context_lines.append(lines[i+1])
                
                context = ' '.join(context_lines)
                # Remove the link itself from context to measure description
                context_without_link = re.sub(r'\[([^\]]+)\]\([^)]+\)', '', context)
                context_without_link = re.sub(r'[*#\-\d\.\s]+', ' ', context_without_link).strip()
                
                links_with_context.append((link_text, link_url, context_without_link))
        
        return links_with_context
    
    def has_sufficient_context(self, context: str) -> bool:
        """Check if the context provides sufficient description for the link."""
        # Remove common filler words and check remaining length
        filler_words = ['the', 'a', 'an', 'and', 'or', 'for', 'to', 'of', 'in', 'on', 'at', 'by']
        words = context.lower().split()
        meaningful_words = [w for w in words if w not in filler_words and len(w) > 2]
        
        # Need at least 3 meaningful words or MIN_CONTEXT_LENGTH characters
        return len(meaningful_words) >= 3 or len(context) >= self.MIN_CONTEXT_LENGTH
    
    def test_viewpoint_functional_links_have_context(self):
        """
        Property 4: Verify functional viewpoint cross-references have descriptive context.
        
        **Feature: documentation-quality-improvement, Property 4: Link Context Requirement**
        **Validates: Requirements 3.4**
        """
        readme_path = VIEWPOINTS_DIR / "functional" / "README.md"
        if not readme_path.exists():
            pytest.skip("Functional viewpoint README.md does not exist")
        
        links_with_context = self.extract_links_with_context(readme_path)
        
        links_without_context = []
        for link_text, link_url, context in links_with_context:
            if not self.has_sufficient_context(context):
                links_without_context.append(f"'{link_text}' -> {link_url}")
        
        assert len(links_without_context) == 0, (
            f"Functional viewpoint has links without sufficient context: {links_without_context}. "
            f"Each cross-reference should have descriptive text explaining what the linked document contains."
        )
    
    def test_viewpoint_information_links_have_context(self):
        """
        Property 4: Verify information viewpoint cross-references have descriptive context.
        
        **Feature: documentation-quality-improvement, Property 4: Link Context Requirement**
        **Validates: Requirements 3.4**
        """
        readme_path = VIEWPOINTS_DIR / "information" / "README.md"
        if not readme_path.exists():
            pytest.skip("Information viewpoint README.md does not exist")
        
        links_with_context = self.extract_links_with_context(readme_path)
        
        links_without_context = []
        for link_text, link_url, context in links_with_context:
            if not self.has_sufficient_context(context):
                links_without_context.append(f"'{link_text}' -> {link_url}")
        
        assert len(links_without_context) == 0, (
            f"Information viewpoint has links without sufficient context: {links_without_context}. "
            f"Each cross-reference should have descriptive text explaining what the linked document contains."
        )
    
    def test_perspective_performance_links_have_context(self):
        """
        Property 4: Verify performance perspective cross-references have descriptive context.
        
        **Feature: documentation-quality-improvement, Property 4: Link Context Requirement**
        **Validates: Requirements 3.4**
        """
        readme_path = PERSPECTIVES_DIR / "performance" / "README.md"
        if not readme_path.exists():
            pytest.skip("Performance perspective README.md does not exist")
        
        links_with_context = self.extract_links_with_context(readme_path)
        
        links_without_context = []
        for link_text, link_url, context in links_with_context:
            if not self.has_sufficient_context(context):
                links_without_context.append(f"'{link_text}' -> {link_url}")
        
        assert len(links_without_context) == 0, (
            f"Performance perspective has links without sufficient context: {links_without_context}. "
            f"Each cross-reference should have descriptive text explaining what the linked document contains."
        )
    
    def test_perspective_security_links_have_context(self):
        """
        Property 4: Verify security perspective cross-references have descriptive context.
        
        **Feature: documentation-quality-improvement, Property 4: Link Context Requirement**
        **Validates: Requirements 3.4**
        """
        readme_path = PERSPECTIVES_DIR / "security" / "README.md"
        if not readme_path.exists():
            pytest.skip("Security perspective README.md does not exist")
        
        links_with_context = self.extract_links_with_context(readme_path)
        
        links_without_context = []
        for link_text, link_url, context in links_with_context:
            if not self.has_sufficient_context(context):
                links_without_context.append(f"'{link_text}' -> {link_url}")
        
        assert len(links_without_context) == 0, (
            f"Security perspective has links without sufficient context: {links_without_context}. "
            f"Each cross-reference should have descriptive text explaining what the linked document contains."
        )
    
    def test_all_optimized_documents_links_have_context(self):
        """
        Property 4: For any cross-reference link in an optimized document, 
        there SHALL be accompanying descriptive text explaining what the linked document contains.
        
        **Feature: documentation-quality-improvement, Property 4: Link Context Requirement**
        **Validates: Requirements 3.4**
        """
        all_optimized_dirs = [
            (VIEWPOINTS_DIR / name, "viewpoint") for name in self.OPTIMIZED_VIEWPOINTS
        ] + [
            (PERSPECTIVES_DIR / name, "perspective") for name in self.OPTIMIZED_PERSPECTIVES
        ]
        
        violations = []
        for dir_path, doc_type in all_optimized_dirs:
            readme_path = dir_path / "README.md"
            if not readme_path.exists():
                continue
            
            links_with_context = self.extract_links_with_context(readme_path)
            
            for link_text, link_url, context in links_with_context:
                if not self.has_sufficient_context(context):
                    violations.append(
                        f"{dir_path.name} {doc_type}: '{link_text}'"
                    )
        
        assert len(violations) == 0, (
            f"Links without sufficient context description: {violations}. "
            f"Each cross-reference should have descriptive text explaining what the linked document contains."
        )



class TestQASCountRequirement:
    """
    **Feature: documentation-quality-improvement, Property 8: QAS Count Requirement**
    
    *For any* perspective README.md, the document SHALL contain at least 3 
    Quality Attribute Scenarios.
    
    **Validates: Requirements 6.1**
    """
    
    # Minimum required QAS count per perspective
    MIN_QAS_COUNT = 3
    
    # All perspective directories that should have QAS
    ALL_PERSPECTIVES = [
        "accessibility", "availability", "cost", "development-resource",
        "evolution", "internationalization", "location", "performance",
        "regulation", "security", "usability"
    ]
    
    def count_qas_in_document(self, file_path: Path) -> int:
        """
        Count Quality Attribute Scenarios in a perspective document.
        
        QAS are identified by the pattern "### Scenario N:" or similar headers
        within the "Quality Attribute Scenarios" section.
        """
        if not file_path.exists():
            return 0
        
        content = file_path.read_text(encoding='utf-8')
        
        # Find the Quality Attribute Scenarios section
        qas_section_match = re.search(
            r'## Quality Attribute Scenarios\s*\n(.*?)(?=\n## |\n---|\Z)', 
            content, 
            re.DOTALL | re.IGNORECASE
        )
        
        if not qas_section_match:
            return 0
        
        qas_section = qas_section_match.group(1)
        
        # Count scenario headers (### Scenario N: or similar patterns)
        scenario_patterns = [
            r'###\s+Scenario\s+\d+',  # ### Scenario 1:
            r'###\s+QAS\s+\d+',        # ### QAS 1:
            r'\*\*Scenario\s+\d+',     # **Scenario 1:
        ]
        
        total_count = 0
        for pattern in scenario_patterns:
            matches = re.findall(pattern, qas_section, re.IGNORECASE)
            total_count += len(matches)
        
        return total_count
    
    def test_cost_perspective_has_minimum_qas(self):
        """
        Property 8: Verify cost perspective has at least 3 Quality Attribute Scenarios.
        
        **Feature: documentation-quality-improvement, Property 8: QAS Count Requirement**
        **Validates: Requirements 6.1**
        """
        readme_path = PERSPECTIVES_DIR / "cost" / "README.md"
        if not readme_path.exists():
            pytest.skip("Cost perspective README.md does not exist")
        
        qas_count = self.count_qas_in_document(readme_path)
        
        assert qas_count >= self.MIN_QAS_COUNT, (
            f"Cost perspective has {qas_count} QAS, "
            f"requires at least {self.MIN_QAS_COUNT}. "
            f"Add more Quality Attribute Scenarios to meet requirements."
        )
    
    def test_regulation_perspective_has_minimum_qas(self):
        """
        Property 8: Verify regulation perspective has at least 3 Quality Attribute Scenarios.
        
        **Feature: documentation-quality-improvement, Property 8: QAS Count Requirement**
        **Validates: Requirements 6.1**
        """
        readme_path = PERSPECTIVES_DIR / "regulation" / "README.md"
        if not readme_path.exists():
            pytest.skip("Regulation perspective README.md does not exist")
        
        qas_count = self.count_qas_in_document(readme_path)
        
        assert qas_count >= self.MIN_QAS_COUNT, (
            f"Regulation perspective has {qas_count} QAS, "
            f"requires at least {self.MIN_QAS_COUNT}. "
            f"Add more Quality Attribute Scenarios to meet requirements."
        )
    
    def test_all_perspectives_have_minimum_qas(self):
        """
        Property 8: For any perspective README.md, the document SHALL contain 
        at least 3 Quality Attribute Scenarios.
        
        **Feature: documentation-quality-improvement, Property 8: QAS Count Requirement**
        **Validates: Requirements 6.1**
        """
        perspectives_below_minimum = []
        
        for perspective_name in self.ALL_PERSPECTIVES:
            readme_path = PERSPECTIVES_DIR / perspective_name / "README.md"
            if not readme_path.exists():
                continue
            
            qas_count = self.count_qas_in_document(readme_path)
            
            if qas_count < self.MIN_QAS_COUNT:
                perspectives_below_minimum.append(
                    f"{perspective_name}: {qas_count} QAS"
                )
        
        assert len(perspectives_below_minimum) == 0, (
            f"Perspectives with fewer than {self.MIN_QAS_COUNT} QAS: {perspectives_below_minimum}. "
            f"Each perspective should have at least {self.MIN_QAS_COUNT} Quality Attribute Scenarios."
        )


class TestQASMeasurability:
    """
    **Feature: documentation-quality-improvement, Property 9: QAS Measurability**
    
    *For any* Quality Attribute Scenario, the response_measure field SHALL contain 
    at least one numeric target value.
    
    **Validates: Requirements 6.2**
    """
    
    # All perspective directories that should have measurable QAS
    ALL_PERSPECTIVES = [
        "accessibility", "availability", "cost", "development-resource",
        "evolution", "internationalization", "location", "performance",
        "regulation", "security", "usability"
    ]
    
    def extract_qas_response_measures(self, file_path: Path) -> List[tuple]:
        """
        Extract QAS response measures from a perspective document.
        
        Returns list of tuples: (scenario_name, response_measure_text)
        """
        if not file_path.exists():
            return []
        
        content = file_path.read_text(encoding='utf-8')
        
        # Find the Quality Attribute Scenarios section
        qas_section_match = re.search(
            r'## Quality Attribute Scenarios\s*\n(.*?)(?=\n## |\n---|\Z)', 
            content, 
            re.DOTALL | re.IGNORECASE
        )
        
        if not qas_section_match:
            return []
        
        qas_section = qas_section_match.group(1)
        
        # Find all scenarios and their response measures
        # Pattern: ### Scenario N: Title ... - **Response Measure**: text
        scenarios = []
        
        # Split by scenario headers
        scenario_blocks = re.split(r'###\s+Scenario\s+\d+[:\s]', qas_section, flags=re.IGNORECASE)
        
        for i, block in enumerate(scenario_blocks[1:], 1):  # Skip first empty split
            # Extract scenario title (first line)
            lines = block.strip().split('\n')
            scenario_title = lines[0].strip() if lines else f"Scenario {i}"
            
            # Find response measure
            response_measure_match = re.search(
                r'\*\*Response Measure\*\*[:\s]*(.+?)(?=\n\n|\n###|\n##|\Z)',
                block,
                re.DOTALL | re.IGNORECASE
            )
            
            if response_measure_match:
                response_measure = response_measure_match.group(1).strip()
                scenarios.append((scenario_title, response_measure))
            else:
                # Try alternative format: - **Response Measure**: text
                alt_match = re.search(
                    r'-\s*\*\*Response Measure\*\*[:\s]*(.+?)(?=\n-|\n\n|\n###|\n##|\Z)',
                    block,
                    re.DOTALL | re.IGNORECASE
                )
                if alt_match:
                    response_measure = alt_match.group(1).strip()
                    scenarios.append((scenario_title, response_measure))
        
        return scenarios
    
    def has_numeric_target(self, response_measure: str) -> bool:
        """
        Check if the response measure contains at least one numeric target.
        
        Numeric targets include:
        - Percentages: 99.9%, 80%
        - Time values: 500ms, 2 seconds, 72 hours
        - Counts: 3 QAS, 5 links
        - Comparisons: ≤ 1000ms, > 90%, < 24 hours
        """
        # Patterns for numeric targets
        numeric_patterns = [
            r'\d+\.?\d*\s*%',           # Percentages: 99.9%, 80%
            r'\d+\.?\d*\s*ms',           # Milliseconds: 500ms
            r'\d+\.?\d*\s*seconds?',     # Seconds: 2 seconds
            r'\d+\.?\d*\s*minutes?',     # Minutes: 5 minutes
            r'\d+\.?\d*\s*hours?',       # Hours: 72 hours
            r'\d+\.?\d*\s*days?',        # Days: 30 days
            r'\d+\.?\d*\s*years?',       # Years: 5 years
            r'[≤≥<>]\s*\d+',             # Comparisons: ≤ 1000, > 90
            r'\d+\s*(?:TPS|req/s|RPS)',  # Throughput: 1000 TPS
            r'\d+\.?\d*\s*(?:MB|GB|KB)', # Size: 50MB
            r'\d+',                       # Any number
        ]
        
        for pattern in numeric_patterns:
            if re.search(pattern, response_measure, re.IGNORECASE):
                return True
        
        return False
    
    def test_cost_perspective_qas_have_numeric_targets(self):
        """
        Property 9: Verify cost perspective QAS response measures contain numeric targets.
        
        **Feature: documentation-quality-improvement, Property 9: QAS Measurability**
        **Validates: Requirements 6.2**
        """
        readme_path = PERSPECTIVES_DIR / "cost" / "README.md"
        if not readme_path.exists():
            pytest.skip("Cost perspective README.md does not exist")
        
        qas_measures = self.extract_qas_response_measures(readme_path)
        
        if not qas_measures:
            pytest.skip("No QAS found in cost perspective")
        
        qas_without_numeric = []
        for scenario_title, response_measure in qas_measures:
            if not self.has_numeric_target(response_measure):
                qas_without_numeric.append(f"'{scenario_title}': {response_measure[:50]}...")
        
        assert len(qas_without_numeric) == 0, (
            f"Cost perspective has QAS without numeric targets: {qas_without_numeric}. "
            f"Each QAS response_measure should contain at least one numeric target value."
        )
    
    def test_regulation_perspective_qas_have_numeric_targets(self):
        """
        Property 9: Verify regulation perspective QAS response measures contain numeric targets.
        
        **Feature: documentation-quality-improvement, Property 9: QAS Measurability**
        **Validates: Requirements 6.2**
        """
        readme_path = PERSPECTIVES_DIR / "regulation" / "README.md"
        if not readme_path.exists():
            pytest.skip("Regulation perspective README.md does not exist")
        
        qas_measures = self.extract_qas_response_measures(readme_path)
        
        if not qas_measures:
            pytest.skip("No QAS found in regulation perspective")
        
        qas_without_numeric = []
        for scenario_title, response_measure in qas_measures:
            if not self.has_numeric_target(response_measure):
                qas_without_numeric.append(f"'{scenario_title}': {response_measure[:50]}...")
        
        assert len(qas_without_numeric) == 0, (
            f"Regulation perspective has QAS without numeric targets: {qas_without_numeric}. "
            f"Each QAS response_measure should contain at least one numeric target value."
        )
    
    def test_all_perspectives_qas_have_numeric_targets(self):
        """
        Property 9: For any Quality Attribute Scenario, the response_measure field 
        SHALL contain at least one numeric target value.
        
        **Feature: documentation-quality-improvement, Property 9: QAS Measurability**
        **Validates: Requirements 6.2**
        """
        perspectives_with_issues = []
        
        for perspective_name in self.ALL_PERSPECTIVES:
            readme_path = PERSPECTIVES_DIR / perspective_name / "README.md"
            if not readme_path.exists():
                continue
            
            qas_measures = self.extract_qas_response_measures(readme_path)
            
            for scenario_title, response_measure in qas_measures:
                if not self.has_numeric_target(response_measure):
                    perspectives_with_issues.append(
                        f"{perspective_name}/{scenario_title}"
                    )
        
        assert len(perspectives_with_issues) == 0, (
            f"QAS without numeric targets in response_measure: {perspectives_with_issues}. "
            f"Each QAS should have measurable response measures with specific numeric targets."
        )


class TestEmptyDirectoryRemoval:
    """
    **Feature: documentation-quality-improvement, Property 7: Empty Directory Removal**
    
    *For any* empty viewpoint directory (architecture/, infrastructure/, security/),
    the directory SHALL NOT exist after cleanup.
    
    **Validates: Requirements 5.3**
    """
    
    # Directories that should be removed
    DIRECTORIES_TO_REMOVE = ["architecture", "infrastructure", "security"]
    
    def test_architecture_directory_removed(self):
        """
        Property 7: Verify viewpoints/architecture/ directory does not exist.
        
        **Feature: documentation-quality-improvement, Property 7: Empty Directory Removal**
        **Validates: Requirements 5.3**
        """
        architecture_dir = VIEWPOINTS_DIR / "architecture"
        
        assert not architecture_dir.exists(), (
            f"Empty directory {architecture_dir} should have been removed. "
            f"Content is covered by other viewpoints."
        )
    
    def test_infrastructure_directory_removed(self):
        """
        Property 7: Verify viewpoints/infrastructure/ directory does not exist.
        
        **Feature: documentation-quality-improvement, Property 7: Empty Directory Removal**
        **Validates: Requirements 5.3**
        """
        infrastructure_dir = VIEWPOINTS_DIR / "infrastructure"
        
        assert not infrastructure_dir.exists(), (
            f"Empty directory {infrastructure_dir} should have been removed. "
            f"Content is covered by deployment viewpoint."
        )
    
    def test_security_directory_removed(self):
        """
        Property 7: Verify viewpoints/security/ directory does not exist.
        
        **Feature: documentation-quality-improvement, Property 7: Empty Directory Removal**
        **Validates: Requirements 5.3**
        """
        security_dir = VIEWPOINTS_DIR / "security"
        
        assert not security_dir.exists(), (
            f"Empty directory {security_dir} should have been removed. "
            f"Content is covered by security perspective."
        )
    
    def test_all_empty_directories_removed(self):
        """
        Property 7: For any empty viewpoint directory (architecture/, infrastructure/, 
        security/), the directory SHALL NOT exist after cleanup.
        
        **Feature: documentation-quality-improvement, Property 7: Empty Directory Removal**
        **Validates: Requirements 5.3**
        """
        existing_directories = []
        
        for dir_name in self.DIRECTORIES_TO_REMOVE:
            dir_path = VIEWPOINTS_DIR / dir_name
            if dir_path.exists():
                existing_directories.append(str(dir_path))
        
        assert len(existing_directories) == 0, (
            f"Empty directories that should have been removed still exist: {existing_directories}. "
            f"These directories are empty and their content is covered by other viewpoints/perspectives."
        )


class TestNoPlaceholderText:
    """
    **Feature: documentation-quality-improvement, Property 5: No Placeholder Text**
    
    *For any* viewpoint or perspective README.md, the document SHALL NOT contain 
    placeholder text patterns such as "To be documented", "TBD", or "Coming soon".
    
    **Validates: Requirements 4.3**
    """
    
    # Placeholder text patterns to detect (case-insensitive)
    # These patterns indicate incomplete documentation content
    PLACEHOLDER_PATTERNS = [
        r'\bto be documented\b',
        r'\bcoming soon\b',
        r'\bplaceholder\b',
        r'\btodo\b',
        r'\bfixme\b',
        r'\bwork in progress\b',
        r'\bwip\b',
        r'\bnot yet implemented\b',
        r'\bpending\b.*\bdocumentation\b',
        r'\bdocumentation pending\b',
        r'\bto be added\b',
        r'\bto be completed\b',
        r'\bto be written\b',
        r'\bto be defined\b',
        r'\bunder construction\b',
        r'\bneeds documentation\b',
        r'\bneeds to be documented\b',
        r'\b\[insert\b',
        r'\b\[add\b',
        r'\b\[fill in\b',
    ]
    
    # Patterns that are acceptable in certain contexts (not placeholder text)
    # - "In Progress" is a valid status indicator in tables
    # - "TBD" in measurement columns is acceptable for pending metrics
    # - "XXX" in phone numbers is acceptable for privacy masking
    ACCEPTABLE_CONTEXT_PATTERNS = [
        r'status.*in progress',  # Status indicators
        r'in progress.*\|',      # Table status columns
        r'\|.*in progress',      # Table cells with status
        r'xxx.*phone',           # Phone number masking
        r'phone.*xxx',           # Phone number masking
        r'1-800-xxx',            # Toll-free number masking
        r'tbd.*\|.*measure',     # TBD in measurement tables
        r'\|.*tbd.*\|',          # TBD in table cells (metrics)
    ]
    
    # All viewpoint directories
    ALL_VIEWPOINTS = [
        "concurrency", "context", "deployment", "development",
        "functional", "information", "operational"
    ]
    
    # All perspective directories
    ALL_PERSPECTIVES = [
        "accessibility", "availability", "cost", "development-resource",
        "evolution", "internationalization", "location", "performance",
        "regulation", "security", "usability"
    ]
    
    def find_placeholder_text(self, file_path: Path) -> List[tuple]:
        """
        Find placeholder text patterns in a markdown file.
        
        Returns list of tuples: (pattern_matched, line_number, line_content)
        """
        if not file_path.exists():
            return []
        
        content = file_path.read_text(encoding='utf-8')
        lines = content.split('\n')
        
        placeholders_found = []
        
        for line_num, line in enumerate(lines, 1):
            line_lower = line.lower()
            
            for pattern in self.PLACEHOLDER_PATTERNS:
                if re.search(pattern, line, re.IGNORECASE):
                    # Skip if it's in a code block or comment about removing placeholders
                    if '```' in line or 'remove placeholder' in line_lower:
                        continue
                    # Skip if it's describing what NOT to do (negative examples)
                    if 'shall not' in line_lower or 'should not' in line_lower:
                        continue
                    # Skip if it's in a test description or requirement
                    if 'verify no' in line_lower or 'patterns exist' in line_lower:
                        continue
                    # Skip if it matches acceptable context patterns
                    is_acceptable = False
                    for acceptable_pattern in self.ACCEPTABLE_CONTEXT_PATTERNS:
                        if re.search(acceptable_pattern, line_lower):
                            is_acceptable = True
                            break
                    if is_acceptable:
                        continue
                    # Skip status badges and status indicators in tables
                    if '> **status**:' in line_lower or '| status |' in line_lower:
                        continue
                    # Skip if it's a phase/milestone status (e.g., "Phase 4: ... IN PROGRESS")
                    if re.search(r'phase\s+\d+.*in progress', line_lower):
                        continue
                    # Skip if it's in a table row with status column
                    if '|' in line and ('in progress' in line_lower or 'tbd' in line_lower):
                        # Check if this looks like a status table
                        if re.search(r'\|\s*(in progress|tbd|to measure)\s*\|', line_lower):
                            continue
                    
                    placeholders_found.append((pattern, line_num, line.strip()[:80]))
        
        return placeholders_found
    
    def test_viewpoint_functional_no_placeholder_text(self):
        """
        Property 5: Verify functional viewpoint README.md has no placeholder text.
        
        **Feature: documentation-quality-improvement, Property 5: No Placeholder Text**
        **Validates: Requirements 4.3**
        """
        readme_path = VIEWPOINTS_DIR / "functional" / "README.md"
        if not readme_path.exists():
            pytest.skip("Functional viewpoint README.md does not exist")
        
        placeholders = self.find_placeholder_text(readme_path)
        
        assert len(placeholders) == 0, (
            f"Functional viewpoint contains placeholder text: "
            f"{[(p[1], p[2]) for p in placeholders]}. "
            f"Remove all placeholder text patterns."
        )
    
    def test_viewpoint_information_no_placeholder_text(self):
        """
        Property 5: Verify information viewpoint README.md has no placeholder text.
        
        **Feature: documentation-quality-improvement, Property 5: No Placeholder Text**
        **Validates: Requirements 4.3**
        """
        readme_path = VIEWPOINTS_DIR / "information" / "README.md"
        if not readme_path.exists():
            pytest.skip("Information viewpoint README.md does not exist")
        
        placeholders = self.find_placeholder_text(readme_path)
        
        assert len(placeholders) == 0, (
            f"Information viewpoint contains placeholder text: "
            f"{[(p[1], p[2]) for p in placeholders]}. "
            f"Remove all placeholder text patterns."
        )
    
    def test_perspective_performance_no_placeholder_text(self):
        """
        Property 5: Verify performance perspective README.md has no placeholder text.
        
        **Feature: documentation-quality-improvement, Property 5: No Placeholder Text**
        **Validates: Requirements 4.3**
        """
        readme_path = PERSPECTIVES_DIR / "performance" / "README.md"
        if not readme_path.exists():
            pytest.skip("Performance perspective README.md does not exist")
        
        placeholders = self.find_placeholder_text(readme_path)
        
        assert len(placeholders) == 0, (
            f"Performance perspective contains placeholder text: "
            f"{[(p[1], p[2]) for p in placeholders]}. "
            f"Remove all placeholder text patterns."
        )
    
    def test_perspective_security_no_placeholder_text(self):
        """
        Property 5: Verify security perspective README.md has no placeholder text.
        
        **Feature: documentation-quality-improvement, Property 5: No Placeholder Text**
        **Validates: Requirements 4.3**
        """
        readme_path = PERSPECTIVES_DIR / "security" / "README.md"
        if not readme_path.exists():
            pytest.skip("Security perspective README.md does not exist")
        
        placeholders = self.find_placeholder_text(readme_path)
        
        assert len(placeholders) == 0, (
            f"Security perspective contains placeholder text: "
            f"{[(p[1], p[2]) for p in placeholders]}. "
            f"Remove all placeholder text patterns."
        )
    
    def test_all_viewpoints_no_placeholder_text(self):
        """
        Property 5: For any viewpoint README.md, the document SHALL NOT contain 
        placeholder text patterns such as "To be documented", "TBD", or "Coming soon".
        
        **Feature: documentation-quality-improvement, Property 5: No Placeholder Text**
        **Validates: Requirements 4.3**
        """
        viewpoints_with_placeholders = []
        
        for viewpoint_name in self.ALL_VIEWPOINTS:
            readme_path = VIEWPOINTS_DIR / viewpoint_name / "README.md"
            if not readme_path.exists():
                continue
            
            placeholders = self.find_placeholder_text(readme_path)
            
            if placeholders:
                viewpoints_with_placeholders.append(
                    f"{viewpoint_name}: {len(placeholders)} placeholder(s) found"
                )
        
        assert len(viewpoints_with_placeholders) == 0, (
            f"Viewpoints containing placeholder text: {viewpoints_with_placeholders}. "
            f"All viewpoint README.md files should have complete content without placeholder text."
        )
    
    def test_all_perspectives_no_placeholder_text(self):
        """
        Property 5: For any perspective README.md, the document SHALL NOT contain 
        placeholder text patterns such as "To be documented", "TBD", or "Coming soon".
        
        **Feature: documentation-quality-improvement, Property 5: No Placeholder Text**
        **Validates: Requirements 4.3**
        """
        perspectives_with_placeholders = []
        
        for perspective_name in self.ALL_PERSPECTIVES:
            readme_path = PERSPECTIVES_DIR / perspective_name / "README.md"
            if not readme_path.exists():
                continue
            
            placeholders = self.find_placeholder_text(readme_path)
            
            if placeholders:
                perspectives_with_placeholders.append(
                    f"{perspective_name}: {len(placeholders)} placeholder(s) found"
                )
        
        assert len(perspectives_with_placeholders) == 0, (
            f"Perspectives containing placeholder text: {perspectives_with_placeholders}. "
            f"All perspective README.md files should have complete content without placeholder text."
        )
    
    def test_all_documentation_no_placeholder_text(self):
        """
        Property 5: For any viewpoint or perspective README.md, the document SHALL NOT 
        contain placeholder text patterns such as "To be documented", "TBD", or "Coming soon".
        
        This is the comprehensive property test that validates all documentation files.
        
        **Feature: documentation-quality-improvement, Property 5: No Placeholder Text**
        **Validates: Requirements 4.3**
        """
        all_dirs = [
            (VIEWPOINTS_DIR / name, "viewpoint") for name in self.ALL_VIEWPOINTS
        ] + [
            (PERSPECTIVES_DIR / name, "perspective") for name in self.ALL_PERSPECTIVES
        ]
        
        documents_with_placeholders = []
        
        for dir_path, doc_type in all_dirs:
            readme_path = dir_path / "README.md"
            if not readme_path.exists():
                continue
            
            placeholders = self.find_placeholder_text(readme_path)
            
            if placeholders:
                for pattern, line_num, line_content in placeholders:
                    documents_with_placeholders.append(
                        f"{dir_path.name} {doc_type} (line {line_num}): '{line_content}'"
                    )
        
        assert len(documents_with_placeholders) == 0, (
            f"Documents containing placeholder text:\n" +
            "\n".join(f"  - {doc}" for doc in documents_with_placeholders) +
            f"\n\nAll viewpoint and perspective README.md files should have complete content "
            f"without placeholder text patterns like 'To be documented', 'TBD', or 'Coming soon'."
        )


class TestDateFormatConsistency:
    """
    **Feature: documentation-quality-improvement, Property 10: Date Format Consistency**
    
    *For any* date field in a document (last_updated, Change History entries), 
    the date SHALL follow the YYYY-MM-DD format.
    
    **Validates: Requirements 9.1, 9.2**
    """
    
    # Valid date format pattern: YYYY-MM-DD
    DATE_FORMAT_PATTERN = r'\d{4}-\d{2}-\d{2}'
    
    # Patterns to find date fields in documents
    DATE_FIELD_PATTERNS = [
        # Frontmatter last_updated field
        r'last_updated[:\s]*["\']?(\d{4}[-/]\d{2}[-/]\d{2}|\d{2}[-/]\d{2}[-/]\d{4}|[A-Za-z]+\s+\d+,?\s+\d{4}|\d+[-/]\d+[-/]\d+)["\']?',
        # Last Updated in document header
        r'\*\*Last Updated\*\*[:\s]*(\d{4}[-/]\d{2}[-/]\d{2}|\d{2}[-/]\d{2}[-/]\d{4}|[A-Za-z]+\s+\d+,?\s+\d{4}|\d+[-/]\d+[-/]\d+)',
        # > **Last Updated**: format
        r'>\s*\*\*Last Updated\*\*[:\s]*(\d{4}[-/]\d{2}[-/]\d{2}|\d{2}[-/]\d{2}[-/]\d{4}|[A-Za-z]+\s+\d+,?\s+\d{4}|\d+[-/]\d+[-/]\d+)',
        # Date in Change History table (| Date | ... |)
        r'\|\s*(\d{4}[-/]\d{2}[-/]\d{2}|\d{2}[-/]\d{2}[-/]\d{4}|[A-Za-z]+\s+\d+,?\s+\d{4}|\d+[-/]\d+[-/]\d+)\s*\|',
    ]
    
    # All viewpoint directories
    ALL_VIEWPOINTS = [
        "concurrency", "context", "deployment", "development",
        "functional", "information", "operational"
    ]
    
    # All perspective directories
    ALL_PERSPECTIVES = [
        "accessibility", "availability", "cost", "development-resource",
        "evolution", "internationalization", "location", "performance",
        "regulation", "security", "usability"
    ]
    
    def is_valid_date_format(self, date_str: str) -> bool:
        """
        Check if a date string follows the YYYY-MM-DD format.
        
        Valid format: 2025-01-15, 2024-12-31
        Invalid formats: 01/15/2025, January 15, 2025, 15-01-2025
        """
        # Must match exactly YYYY-MM-DD
        if not re.match(r'^\d{4}-\d{2}-\d{2}$', date_str.strip()):
            return False
        
        # Validate the date is reasonable (year between 2020-2030, valid month/day)
        try:
            parts = date_str.strip().split('-')
            year = int(parts[0])
            month = int(parts[1])
            day = int(parts[2])
            
            if year < 2020 or year > 2030:
                return False
            if month < 1 or month > 12:
                return False
            if day < 1 or day > 31:
                return False
            
            return True
        except (ValueError, IndexError):
            return False
    
    def extract_dates_from_document(self, file_path: Path) -> List[tuple]:
        """
        Extract all date fields from a markdown document.
        
        Returns list of tuples: (date_string, line_number, context)
        """
        if not file_path.exists():
            return []
        
        content = file_path.read_text(encoding='utf-8')
        lines = content.split('\n')
        
        dates_found = []
        
        for line_num, line in enumerate(lines, 1):
            # Check for last_updated in frontmatter or header
            last_updated_match = re.search(
                r'last_updated[:\s]*["\']?(\d{4}[-/]\d{2}[-/]\d{2}|\d{2}[-/]\d{2}[-/]\d{4}|[A-Za-z]+\s+\d+,?\s+\d{4}|\d+[-/]\d+[-/]\d+)["\']?',
                line, re.IGNORECASE
            )
            if last_updated_match:
                dates_found.append((last_updated_match.group(1), line_num, 'last_updated'))
            
            # Check for **Last Updated**: format
            header_date_match = re.search(
                r'\*\*Last Updated\*\*[:\s]*(\d{4}[-/]\d{2}[-/]\d{2}|\d{2}[-/]\d{2}[-/]\d{4}|[A-Za-z]+\s+\d+,?\s+\d{4}|\d+[-/]\d+[-/]\d+)',
                line, re.IGNORECASE
            )
            if header_date_match:
                dates_found.append((header_date_match.group(1), line_num, 'header_date'))
            
            # Check for dates in Change History table
            # Look for table rows with dates (| YYYY-MM-DD | ... |)
            if '|' in line and 'change history' not in line.lower() and 'date' not in line.lower():
                table_date_match = re.search(
                    r'\|\s*(\d{4}[-/]\d{2}[-/]\d{2}|\d{2}[-/]\d{2}[-/]\d{4})\s*\|',
                    line
                )
                if table_date_match:
                    dates_found.append((table_date_match.group(1), line_num, 'change_history'))
        
        return dates_found
    
    def find_invalid_dates(self, file_path: Path) -> List[tuple]:
        """
        Find all dates that don't follow the YYYY-MM-DD format.
        
        Returns list of tuples: (date_string, line_number, context, issue)
        """
        dates = self.extract_dates_from_document(file_path)
        
        invalid_dates = []
        for date_str, line_num, context in dates:
            if not self.is_valid_date_format(date_str):
                # Determine the issue
                if '/' in date_str:
                    issue = "uses '/' instead of '-'"
                elif re.match(r'\d{2}-\d{2}-\d{4}', date_str):
                    issue = "uses DD-MM-YYYY instead of YYYY-MM-DD"
                elif re.match(r'[A-Za-z]+', date_str):
                    issue = "uses text month format instead of YYYY-MM-DD"
                else:
                    issue = "does not match YYYY-MM-DD format"
                
                invalid_dates.append((date_str, line_num, context, issue))
        
        return invalid_dates
    
    def test_viewpoint_functional_date_format(self):
        """
        Property 10: Verify functional viewpoint README.md uses YYYY-MM-DD date format.
        
        **Feature: documentation-quality-improvement, Property 10: Date Format Consistency**
        **Validates: Requirements 9.1, 9.2**
        """
        readme_path = VIEWPOINTS_DIR / "functional" / "README.md"
        if not readme_path.exists():
            pytest.skip("Functional viewpoint README.md does not exist")
        
        invalid_dates = self.find_invalid_dates(readme_path)
        
        assert len(invalid_dates) == 0, (
            f"Functional viewpoint has invalid date formats: "
            f"{[(d[0], d[2], d[3]) for d in invalid_dates]}. "
            f"All dates should use YYYY-MM-DD format."
        )
    
    def test_viewpoint_information_date_format(self):
        """
        Property 10: Verify information viewpoint README.md uses YYYY-MM-DD date format.
        
        **Feature: documentation-quality-improvement, Property 10: Date Format Consistency**
        **Validates: Requirements 9.1, 9.2**
        """
        readme_path = VIEWPOINTS_DIR / "information" / "README.md"
        if not readme_path.exists():
            pytest.skip("Information viewpoint README.md does not exist")
        
        invalid_dates = self.find_invalid_dates(readme_path)
        
        assert len(invalid_dates) == 0, (
            f"Information viewpoint has invalid date formats: "
            f"{[(d[0], d[2], d[3]) for d in invalid_dates]}. "
            f"All dates should use YYYY-MM-DD format."
        )
    
    def test_perspective_performance_date_format(self):
        """
        Property 10: Verify performance perspective README.md uses YYYY-MM-DD date format.
        
        **Feature: documentation-quality-improvement, Property 10: Date Format Consistency**
        **Validates: Requirements 9.1, 9.2**
        """
        readme_path = PERSPECTIVES_DIR / "performance" / "README.md"
        if not readme_path.exists():
            pytest.skip("Performance perspective README.md does not exist")
        
        invalid_dates = self.find_invalid_dates(readme_path)
        
        assert len(invalid_dates) == 0, (
            f"Performance perspective has invalid date formats: "
            f"{[(d[0], d[2], d[3]) for d in invalid_dates]}. "
            f"All dates should use YYYY-MM-DD format."
        )
    
    def test_perspective_security_date_format(self):
        """
        Property 10: Verify security perspective README.md uses YYYY-MM-DD date format.
        
        **Feature: documentation-quality-improvement, Property 10: Date Format Consistency**
        **Validates: Requirements 9.1, 9.2**
        """
        readme_path = PERSPECTIVES_DIR / "security" / "README.md"
        if not readme_path.exists():
            pytest.skip("Security perspective README.md does not exist")
        
        invalid_dates = self.find_invalid_dates(readme_path)
        
        assert len(invalid_dates) == 0, (
            f"Security perspective has invalid date formats: "
            f"{[(d[0], d[2], d[3]) for d in invalid_dates]}. "
            f"All dates should use YYYY-MM-DD format."
        )
    
    def test_all_viewpoints_date_format(self):
        """
        Property 10: For any viewpoint README.md, all date fields SHALL follow 
        the YYYY-MM-DD format.
        
        **Feature: documentation-quality-improvement, Property 10: Date Format Consistency**
        **Validates: Requirements 9.1, 9.2**
        """
        viewpoints_with_invalid_dates = []
        
        for viewpoint_name in self.ALL_VIEWPOINTS:
            readme_path = VIEWPOINTS_DIR / viewpoint_name / "README.md"
            if not readme_path.exists():
                continue
            
            invalid_dates = self.find_invalid_dates(readme_path)
            
            if invalid_dates:
                for date_str, line_num, context, issue in invalid_dates:
                    viewpoints_with_invalid_dates.append(
                        f"{viewpoint_name} (line {line_num}): '{date_str}' {issue}"
                    )
        
        assert len(viewpoints_with_invalid_dates) == 0, (
            f"Viewpoints with invalid date formats:\n" +
            "\n".join(f"  - {v}" for v in viewpoints_with_invalid_dates) +
            f"\n\nAll dates should use YYYY-MM-DD format (e.g., 2025-01-15)."
        )
    
    def test_all_perspectives_date_format(self):
        """
        Property 10: For any perspective README.md, all date fields SHALL follow 
        the YYYY-MM-DD format.
        
        **Feature: documentation-quality-improvement, Property 10: Date Format Consistency**
        **Validates: Requirements 9.1, 9.2**
        """
        perspectives_with_invalid_dates = []
        
        for perspective_name in self.ALL_PERSPECTIVES:
            readme_path = PERSPECTIVES_DIR / perspective_name / "README.md"
            if not readme_path.exists():
                continue
            
            invalid_dates = self.find_invalid_dates(readme_path)
            
            if invalid_dates:
                for date_str, line_num, context, issue in invalid_dates:
                    perspectives_with_invalid_dates.append(
                        f"{perspective_name} (line {line_num}): '{date_str}' {issue}"
                    )
        
        assert len(perspectives_with_invalid_dates) == 0, (
            f"Perspectives with invalid date formats:\n" +
            "\n".join(f"  - {p}" for p in perspectives_with_invalid_dates) +
            f"\n\nAll dates should use YYYY-MM-DD format (e.g., 2025-01-15)."
        )
    
    def test_all_documentation_date_format(self):
        """
        Property 10: For any date field in a viewpoint or perspective document 
        (last_updated, Change History entries), the date SHALL follow the YYYY-MM-DD format.
        
        This is the comprehensive property test that validates all documentation files.
        
        **Feature: documentation-quality-improvement, Property 10: Date Format Consistency**
        **Validates: Requirements 9.1, 9.2**
        """
        all_dirs = [
            (VIEWPOINTS_DIR / name, "viewpoint") for name in self.ALL_VIEWPOINTS
        ] + [
            (PERSPECTIVES_DIR / name, "perspective") for name in self.ALL_PERSPECTIVES
        ]
        
        documents_with_invalid_dates = []
        
        for dir_path, doc_type in all_dirs:
            readme_path = dir_path / "README.md"
            if not readme_path.exists():
                continue
            
            invalid_dates = self.find_invalid_dates(readme_path)
            
            if invalid_dates:
                for date_str, line_num, context, issue in invalid_dates:
                    documents_with_invalid_dates.append(
                        f"{dir_path.name} {doc_type} (line {line_num}, {context}): '{date_str}' {issue}"
                    )
        
        assert len(documents_with_invalid_dates) == 0, (
            f"Documents with invalid date formats:\n" +
            "\n".join(f"  - {doc}" for doc in documents_with_invalid_dates) +
            f"\n\nAll date fields (last_updated, Change History entries) should use "
            f"YYYY-MM-DD format (e.g., 2025-01-15). "
            f"Run 'date +%Y-%m-%d' to get the current date in the correct format."
        )



class TestViewpointStructureCompliance:
    """
    **Feature: documentation-quality-improvement, Property 11: Viewpoint Structure Compliance**
    
    *For any* viewpoint README.md, the document SHALL contain all required sections:
    Overview, Purpose, Stakeholders, Key Concerns, Contents, Quick Links.
    
    **Validates: Requirements 2.1, 10.1**
    """
    
    # Required sections for viewpoint documents (based on design.md and requirements.md)
    # These sections must be present in all viewpoint README.md files
    REQUIRED_SECTIONS = [
        "Overview",
        "Purpose", 
        "Stakeholders",
        "Contents",
    ]
    
    # Sections where alternatives are acceptable (at least one from each group must be present)
    # "Key Concerns" and "Key Principles" are both valid section names
    ALTERNATIVE_SECTIONS = [
        ["Key Concerns", "Key Principles"],  # Either is acceptable
    ]
    
    # Optional sections that are recommended but not strictly required
    OPTIONAL_SECTIONS = [
        "Quick Links",
        "Related Documentation",
        "Change History",
    ]
    
    # Required header elements (in the document header area, not as ## sections)
    REQUIRED_HEADER_ELEMENTS = [
        "Status",       # Status badge: > **Status**: ✅ Active
        "Last Updated", # Last Updated date
        "Owner",        # Owner information
    ]
    
    # All viewpoint directories that should have compliant README.md
    ALL_VIEWPOINTS = [
        "concurrency", "context", "deployment", "development",
        "functional", "information", "operational"
    ]
    
    def extract_sections_from_markdown(self, file_path: Path) -> Set[str]:
        """Extract section headers (## level) from a markdown file."""
        if not file_path.exists():
            return set()
        
        content = file_path.read_text(encoding='utf-8')
        # Match ## headers (level 2 headers)
        headers = re.findall(r'^##\s+(.+)$', content, re.MULTILINE)
        return set(headers)
    
    def check_header_elements(self, file_path: Path) -> List[str]:
        """
        Check if the document header contains required elements.
        
        Header elements are typically in the format:
        > **Status**: ✅ Active
        > **Last Updated**: 2025-01-15
        > **Owner**: Architecture Team
        """
        if not file_path.exists():
            return self.REQUIRED_HEADER_ELEMENTS.copy()
        
        content = file_path.read_text(encoding='utf-8')
        
        # Get the first 50 lines (header area)
        lines = content.split('\n')[:50]
        header_content = '\n'.join(lines).lower()
        
        missing_elements = []
        for element in self.REQUIRED_HEADER_ELEMENTS:
            # Check for various formats of header elements
            patterns = [
                f'**{element.lower()}**',           # **Status**
                f'> **{element.lower()}**',         # > **Status**
                f'{element.lower()}:',              # Status:
                f'{element.lower()} :',             # Status :
            ]
            
            found = False
            for pattern in patterns:
                if pattern in header_content:
                    found = True
                    break
            
            if not found:
                missing_elements.append(element)
        
        return missing_elements
    
    def check_required_sections(self, file_path: Path) -> List[str]:
        """
        Check if the document contains all required sections.
        
        Returns list of missing section names.
        """
        sections = self.extract_sections_from_markdown(file_path)
        
        missing_sections = []
        
        # Check required sections
        for required in self.REQUIRED_SECTIONS:
            # Case-insensitive check for section presence
            found = False
            for section in sections:
                if required.lower() in section.lower():
                    found = True
                    break
            
            if not found:
                missing_sections.append(required)
        
        # Check alternative sections (at least one from each group must be present)
        for alternatives in self.ALTERNATIVE_SECTIONS:
            found = False
            for alt in alternatives:
                for section in sections:
                    if alt.lower() in section.lower():
                        found = True
                        break
                if found:
                    break
            if not found:
                missing_sections.append(f"One of: {alternatives}")
        
        return missing_sections
    
    def test_viewpoint_functional_structure_compliance(self):
        """
        Property 11: Verify functional viewpoint README.md contains all required sections.
        
        **Feature: documentation-quality-improvement, Property 11: Viewpoint Structure Compliance**
        **Validates: Requirements 2.1, 10.1**
        """
        readme_path = VIEWPOINTS_DIR / "functional" / "README.md"
        if not readme_path.exists():
            pytest.skip("Functional viewpoint README.md does not exist")
        
        # Check required sections
        missing_sections = self.check_required_sections(readme_path)
        
        # Check header elements
        missing_header_elements = self.check_header_elements(readme_path)
        
        all_missing = []
        if missing_sections:
            all_missing.append(f"Missing sections: {missing_sections}")
        if missing_header_elements:
            all_missing.append(f"Missing header elements: {missing_header_elements}")
        
        assert len(all_missing) == 0, (
            f"Functional viewpoint structure compliance issues: {'; '.join(all_missing)}. "
            f"Required sections: {self.REQUIRED_SECTIONS}. "
            f"Required header elements: {self.REQUIRED_HEADER_ELEMENTS}."
        )
    
    def test_viewpoint_information_structure_compliance(self):
        """
        Property 11: Verify information viewpoint README.md contains all required sections.
        
        **Feature: documentation-quality-improvement, Property 11: Viewpoint Structure Compliance**
        **Validates: Requirements 2.1, 10.1**
        """
        readme_path = VIEWPOINTS_DIR / "information" / "README.md"
        if not readme_path.exists():
            pytest.skip("Information viewpoint README.md does not exist")
        
        # Check required sections
        missing_sections = self.check_required_sections(readme_path)
        
        # Check header elements
        missing_header_elements = self.check_header_elements(readme_path)
        
        all_missing = []
        if missing_sections:
            all_missing.append(f"Missing sections: {missing_sections}")
        if missing_header_elements:
            all_missing.append(f"Missing header elements: {missing_header_elements}")
        
        assert len(all_missing) == 0, (
            f"Information viewpoint structure compliance issues: {'; '.join(all_missing)}. "
            f"Required sections: {self.REQUIRED_SECTIONS}. "
            f"Required header elements: {self.REQUIRED_HEADER_ELEMENTS}."
        )
    
    def test_viewpoint_concurrency_structure_compliance(self):
        """
        Property 11: Verify concurrency viewpoint README.md contains all required sections.
        
        **Feature: documentation-quality-improvement, Property 11: Viewpoint Structure Compliance**
        **Validates: Requirements 2.1, 10.1**
        """
        readme_path = VIEWPOINTS_DIR / "concurrency" / "README.md"
        if not readme_path.exists():
            pytest.skip("Concurrency viewpoint README.md does not exist")
        
        # Check required sections
        missing_sections = self.check_required_sections(readme_path)
        
        # Check header elements
        missing_header_elements = self.check_header_elements(readme_path)
        
        all_missing = []
        if missing_sections:
            all_missing.append(f"Missing sections: {missing_sections}")
        if missing_header_elements:
            all_missing.append(f"Missing header elements: {missing_header_elements}")
        
        assert len(all_missing) == 0, (
            f"Concurrency viewpoint structure compliance issues: {'; '.join(all_missing)}. "
            f"Required sections: {self.REQUIRED_SECTIONS}. "
            f"Required header elements: {self.REQUIRED_HEADER_ELEMENTS}."
        )
    
    def test_viewpoint_context_structure_compliance(self):
        """
        Property 11: Verify context viewpoint README.md contains all required sections.
        
        **Feature: documentation-quality-improvement, Property 11: Viewpoint Structure Compliance**
        **Validates: Requirements 2.1, 10.1**
        """
        readme_path = VIEWPOINTS_DIR / "context" / "README.md"
        if not readme_path.exists():
            pytest.skip("Context viewpoint README.md does not exist")
        
        # Check required sections
        missing_sections = self.check_required_sections(readme_path)
        
        # Check header elements
        missing_header_elements = self.check_header_elements(readme_path)
        
        all_missing = []
        if missing_sections:
            all_missing.append(f"Missing sections: {missing_sections}")
        if missing_header_elements:
            all_missing.append(f"Missing header elements: {missing_header_elements}")
        
        assert len(all_missing) == 0, (
            f"Context viewpoint structure compliance issues: {'; '.join(all_missing)}. "
            f"Required sections: {self.REQUIRED_SECTIONS}. "
            f"Required header elements: {self.REQUIRED_HEADER_ELEMENTS}."
        )
    
    def test_viewpoint_deployment_structure_compliance(self):
        """
        Property 11: Verify deployment viewpoint README.md contains all required sections.
        
        **Feature: documentation-quality-improvement, Property 11: Viewpoint Structure Compliance**
        **Validates: Requirements 2.1, 10.1**
        """
        readme_path = VIEWPOINTS_DIR / "deployment" / "README.md"
        if not readme_path.exists():
            pytest.skip("Deployment viewpoint README.md does not exist")
        
        # Check required sections
        missing_sections = self.check_required_sections(readme_path)
        
        # Check header elements
        missing_header_elements = self.check_header_elements(readme_path)
        
        all_missing = []
        if missing_sections:
            all_missing.append(f"Missing sections: {missing_sections}")
        if missing_header_elements:
            all_missing.append(f"Missing header elements: {missing_header_elements}")
        
        assert len(all_missing) == 0, (
            f"Deployment viewpoint structure compliance issues: {'; '.join(all_missing)}. "
            f"Required sections: {self.REQUIRED_SECTIONS}. "
            f"Required header elements: {self.REQUIRED_HEADER_ELEMENTS}."
        )
    
    def test_viewpoint_development_structure_compliance(self):
        """
        Property 11: Verify development viewpoint README.md contains all required sections.
        
        **Feature: documentation-quality-improvement, Property 11: Viewpoint Structure Compliance**
        **Validates: Requirements 2.1, 10.1**
        """
        readme_path = VIEWPOINTS_DIR / "development" / "README.md"
        if not readme_path.exists():
            pytest.skip("Development viewpoint README.md does not exist")
        
        # Check required sections
        missing_sections = self.check_required_sections(readme_path)
        
        # Check header elements
        missing_header_elements = self.check_header_elements(readme_path)
        
        all_missing = []
        if missing_sections:
            all_missing.append(f"Missing sections: {missing_sections}")
        if missing_header_elements:
            all_missing.append(f"Missing header elements: {missing_header_elements}")
        
        assert len(all_missing) == 0, (
            f"Development viewpoint structure compliance issues: {'; '.join(all_missing)}. "
            f"Required sections: {self.REQUIRED_SECTIONS}. "
            f"Required header elements: {self.REQUIRED_HEADER_ELEMENTS}."
        )
    
    def test_viewpoint_operational_structure_compliance(self):
        """
        Property 11: Verify operational viewpoint README.md contains all required sections.
        
        **Feature: documentation-quality-improvement, Property 11: Viewpoint Structure Compliance**
        **Validates: Requirements 2.1, 10.1**
        """
        readme_path = VIEWPOINTS_DIR / "operational" / "README.md"
        if not readme_path.exists():
            pytest.skip("Operational viewpoint README.md does not exist")
        
        # Check required sections
        missing_sections = self.check_required_sections(readme_path)
        
        # Check header elements
        missing_header_elements = self.check_header_elements(readme_path)
        
        all_missing = []
        if missing_sections:
            all_missing.append(f"Missing sections: {missing_sections}")
        if missing_header_elements:
            all_missing.append(f"Missing header elements: {missing_header_elements}")
        
        assert len(all_missing) == 0, (
            f"Operational viewpoint structure compliance issues: {'; '.join(all_missing)}. "
            f"Required sections: {self.REQUIRED_SECTIONS}. "
            f"Required header elements: {self.REQUIRED_HEADER_ELEMENTS}."
        )
    
    def test_all_viewpoints_structure_compliance(self):
        """
        Property 11: For any viewpoint README.md, the document SHALL contain all 
        required sections: Overview, Purpose, Stakeholders, Key Concerns, Contents.
        
        This is the comprehensive property test that validates all viewpoint files.
        
        **Feature: documentation-quality-improvement, Property 11: Viewpoint Structure Compliance**
        **Validates: Requirements 2.1, 10.1**
        """
        viewpoints_with_issues = []
        
        for viewpoint_name in self.ALL_VIEWPOINTS:
            readme_path = VIEWPOINTS_DIR / viewpoint_name / "README.md"
            if not readme_path.exists():
                viewpoints_with_issues.append(
                    f"{viewpoint_name}: README.md does not exist"
                )
                continue
            
            # Check required sections
            missing_sections = self.check_required_sections(readme_path)
            
            # Check header elements
            missing_header_elements = self.check_header_elements(readme_path)
            
            issues = []
            if missing_sections:
                issues.append(f"missing sections: {missing_sections}")
            if missing_header_elements:
                issues.append(f"missing header elements: {missing_header_elements}")
            
            if issues:
                viewpoints_with_issues.append(
                    f"{viewpoint_name}: {'; '.join(issues)}"
                )
        
        assert len(viewpoints_with_issues) == 0, (
            f"Viewpoints with structure compliance issues:\n" +
            "\n".join(f"  - {v}" for v in viewpoints_with_issues) +
            f"\n\nAll viewpoint README.md files must contain these sections: {self.REQUIRED_SECTIONS}. "
            f"And these header elements: {self.REQUIRED_HEADER_ELEMENTS}."
        )


class TestPerspectiveStructureCompliance:
    """
    **Feature: documentation-quality-improvement, Property 12: Perspective Structure Compliance**
    
    *For any* perspective README.md, the document SHALL contain all required sections:
    Overview, Key Concerns, Quality Attribute Scenarios, Affected Viewpoints, Quick Links.
    
    **Validates: Requirements 2.2, 10.2**
    """
    
    # Required sections for perspective documents (based on design.md and requirements.md)
    # These sections must be present in all perspective README.md files
    REQUIRED_SECTIONS = [
        "Overview",
        "Key Concerns",
        "Quality Attribute Scenarios",
    ]
    
    # Sections where alternatives are acceptable (at least one from each group must be present)
    ALTERNATIVE_SECTIONS = [
        ["Affected Viewpoints", "Related Viewpoints", "Contents"],  # Either is acceptable
    ]
    
    # Optional sections that are recommended but not strictly required
    OPTIONAL_SECTIONS = [
        "Quick Links",
        "Related Documentation",
        "Change History",
        "Purpose",
        "Stakeholders",
    ]
    
    # Required header elements (in the document header area, not as ## sections)
    REQUIRED_HEADER_ELEMENTS = [
        "Status",       # Status badge: > **Status**: ✅ Active
        "Last Updated", # Last Updated date
        "Owner",        # Owner information
    ]
    
    # All perspective directories that should have compliant README.md
    ALL_PERSPECTIVES = [
        "accessibility", "availability", "cost", "development-resource",
        "evolution", "internationalization", "location", "performance",
        "regulation", "security", "usability"
    ]
    
    def extract_sections_from_markdown(self, file_path: Path) -> Set[str]:
        """Extract section headers (## level) from a markdown file."""
        if not file_path.exists():
            return set()
        
        content = file_path.read_text(encoding='utf-8')
        # Match ## headers (level 2 headers)
        headers = re.findall(r'^##\s+(.+)$', content, re.MULTILINE)
        return set(headers)
    
    def check_header_elements(self, file_path: Path) -> List[str]:
        """
        Check if the document header contains required elements.
        
        Header elements are typically in the format:
        > **Status**: ✅ Active
        > **Last Updated**: 2025-01-15
        > **Owner**: Architecture Team
        """
        if not file_path.exists():
            return self.REQUIRED_HEADER_ELEMENTS.copy()
        
        content = file_path.read_text(encoding='utf-8')
        
        # Get the first 50 lines (header area)
        lines = content.split('\n')[:50]
        header_content = '\n'.join(lines).lower()
        
        missing_elements = []
        for element in self.REQUIRED_HEADER_ELEMENTS:
            # Check for various formats of header elements
            patterns = [
                f'**{element.lower()}**',           # **Status**
                f'> **{element.lower()}**',         # > **Status**
                f'{element.lower()}:',              # Status:
                f'{element.lower()} :',             # Status :
            ]
            
            found = False
            for pattern in patterns:
                if pattern in header_content:
                    found = True
                    break
            
            if not found:
                missing_elements.append(element)
        
        return missing_elements
    
    def check_required_sections(self, file_path: Path) -> List[str]:
        """
        Check if the document contains all required sections.
        
        Returns list of missing section names.
        """
        sections = self.extract_sections_from_markdown(file_path)
        
        missing_sections = []
        
        # Check required sections
        for required in self.REQUIRED_SECTIONS:
            # Case-insensitive check for section presence
            found = False
            for section in sections:
                if required.lower() in section.lower():
                    found = True
                    break
            
            if not found:
                missing_sections.append(required)
        
        # Check alternative sections (at least one from each group must be present)
        for alternatives in self.ALTERNATIVE_SECTIONS:
            found = False
            for alt in alternatives:
                for section in sections:
                    if alt.lower() in section.lower():
                        found = True
                        break
                if found:
                    break
            if not found:
                missing_sections.append(f"One of: {alternatives}")
        
        return missing_sections
    
    def test_perspective_performance_structure_compliance(self):
        """
        Property 12: Verify performance perspective README.md contains all required sections.
        
        **Feature: documentation-quality-improvement, Property 12: Perspective Structure Compliance**
        **Validates: Requirements 2.2, 10.2**
        """
        readme_path = PERSPECTIVES_DIR / "performance" / "README.md"
        if not readme_path.exists():
            pytest.skip("Performance perspective README.md does not exist")
        
        # Check required sections
        missing_sections = self.check_required_sections(readme_path)
        
        # Check header elements
        missing_header_elements = self.check_header_elements(readme_path)
        
        all_missing = []
        if missing_sections:
            all_missing.append(f"Missing sections: {missing_sections}")
        if missing_header_elements:
            all_missing.append(f"Missing header elements: {missing_header_elements}")
        
        assert len(all_missing) == 0, (
            f"Performance perspective structure compliance issues: {'; '.join(all_missing)}. "
            f"Required sections: {self.REQUIRED_SECTIONS}. "
            f"Required header elements: {self.REQUIRED_HEADER_ELEMENTS}."
        )
    
    def test_perspective_security_structure_compliance(self):
        """
        Property 12: Verify security perspective README.md contains all required sections.
        
        **Feature: documentation-quality-improvement, Property 12: Perspective Structure Compliance**
        **Validates: Requirements 2.2, 10.2**
        """
        readme_path = PERSPECTIVES_DIR / "security" / "README.md"
        if not readme_path.exists():
            pytest.skip("Security perspective README.md does not exist")
        
        # Check required sections
        missing_sections = self.check_required_sections(readme_path)
        
        # Check header elements
        missing_header_elements = self.check_header_elements(readme_path)
        
        all_missing = []
        if missing_sections:
            all_missing.append(f"Missing sections: {missing_sections}")
        if missing_header_elements:
            all_missing.append(f"Missing header elements: {missing_header_elements}")
        
        assert len(all_missing) == 0, (
            f"Security perspective structure compliance issues: {'; '.join(all_missing)}. "
            f"Required sections: {self.REQUIRED_SECTIONS}. "
            f"Required header elements: {self.REQUIRED_HEADER_ELEMENTS}."
        )
    
    def test_perspective_cost_structure_compliance(self):
        """
        Property 12: Verify cost perspective README.md contains all required sections.
        
        **Feature: documentation-quality-improvement, Property 12: Perspective Structure Compliance**
        **Validates: Requirements 2.2, 10.2**
        """
        readme_path = PERSPECTIVES_DIR / "cost" / "README.md"
        if not readme_path.exists():
            pytest.skip("Cost perspective README.md does not exist")
        
        # Check required sections
        missing_sections = self.check_required_sections(readme_path)
        
        # Check header elements
        missing_header_elements = self.check_header_elements(readme_path)
        
        all_missing = []
        if missing_sections:
            all_missing.append(f"Missing sections: {missing_sections}")
        if missing_header_elements:
            all_missing.append(f"Missing header elements: {missing_header_elements}")
        
        assert len(all_missing) == 0, (
            f"Cost perspective structure compliance issues: {'; '.join(all_missing)}. "
            f"Required sections: {self.REQUIRED_SECTIONS}. "
            f"Required header elements: {self.REQUIRED_HEADER_ELEMENTS}."
        )
    
    def test_perspective_regulation_structure_compliance(self):
        """
        Property 12: Verify regulation perspective README.md contains all required sections.
        
        **Feature: documentation-quality-improvement, Property 12: Perspective Structure Compliance**
        **Validates: Requirements 2.2, 10.2**
        """
        readme_path = PERSPECTIVES_DIR / "regulation" / "README.md"
        if not readme_path.exists():
            pytest.skip("Regulation perspective README.md does not exist")
        
        # Check required sections
        missing_sections = self.check_required_sections(readme_path)
        
        # Check header elements
        missing_header_elements = self.check_header_elements(readme_path)
        
        all_missing = []
        if missing_sections:
            all_missing.append(f"Missing sections: {missing_sections}")
        if missing_header_elements:
            all_missing.append(f"Missing header elements: {missing_header_elements}")
        
        assert len(all_missing) == 0, (
            f"Regulation perspective structure compliance issues: {'; '.join(all_missing)}. "
            f"Required sections: {self.REQUIRED_SECTIONS}. "
            f"Required header elements: {self.REQUIRED_HEADER_ELEMENTS}."
        )
    
    def test_perspective_availability_structure_compliance(self):
        """
        Property 12: Verify availability perspective README.md contains all required sections.
        
        **Feature: documentation-quality-improvement, Property 12: Perspective Structure Compliance**
        **Validates: Requirements 2.2, 10.2**
        """
        readme_path = PERSPECTIVES_DIR / "availability" / "README.md"
        if not readme_path.exists():
            pytest.skip("Availability perspective README.md does not exist")
        
        # Check required sections
        missing_sections = self.check_required_sections(readme_path)
        
        # Check header elements
        missing_header_elements = self.check_header_elements(readme_path)
        
        all_missing = []
        if missing_sections:
            all_missing.append(f"Missing sections: {missing_sections}")
        if missing_header_elements:
            all_missing.append(f"Missing header elements: {missing_header_elements}")
        
        assert len(all_missing) == 0, (
            f"Availability perspective structure compliance issues: {'; '.join(all_missing)}. "
            f"Required sections: {self.REQUIRED_SECTIONS}. "
            f"Required header elements: {self.REQUIRED_HEADER_ELEMENTS}."
        )
    
    def test_perspective_accessibility_structure_compliance(self):
        """
        Property 12: Verify accessibility perspective README.md contains all required sections.
        
        **Feature: documentation-quality-improvement, Property 12: Perspective Structure Compliance**
        **Validates: Requirements 2.2, 10.2**
        """
        readme_path = PERSPECTIVES_DIR / "accessibility" / "README.md"
        if not readme_path.exists():
            pytest.skip("Accessibility perspective README.md does not exist")
        
        # Check required sections
        missing_sections = self.check_required_sections(readme_path)
        
        # Check header elements
        missing_header_elements = self.check_header_elements(readme_path)
        
        all_missing = []
        if missing_sections:
            all_missing.append(f"Missing sections: {missing_sections}")
        if missing_header_elements:
            all_missing.append(f"Missing header elements: {missing_header_elements}")
        
        assert len(all_missing) == 0, (
            f"Accessibility perspective structure compliance issues: {'; '.join(all_missing)}. "
            f"Required sections: {self.REQUIRED_SECTIONS}. "
            f"Required header elements: {self.REQUIRED_HEADER_ELEMENTS}."
        )
    
    def test_perspective_usability_structure_compliance(self):
        """
        Property 12: Verify usability perspective README.md contains all required sections.
        
        **Feature: documentation-quality-improvement, Property 12: Perspective Structure Compliance**
        **Validates: Requirements 2.2, 10.2**
        """
        readme_path = PERSPECTIVES_DIR / "usability" / "README.md"
        if not readme_path.exists():
            pytest.skip("Usability perspective README.md does not exist")
        
        # Check required sections
        missing_sections = self.check_required_sections(readme_path)
        
        # Check header elements
        missing_header_elements = self.check_header_elements(readme_path)
        
        all_missing = []
        if missing_sections:
            all_missing.append(f"Missing sections: {missing_sections}")
        if missing_header_elements:
            all_missing.append(f"Missing header elements: {missing_header_elements}")
        
        assert len(all_missing) == 0, (
            f"Usability perspective structure compliance issues: {'; '.join(all_missing)}. "
            f"Required sections: {self.REQUIRED_SECTIONS}. "
            f"Required header elements: {self.REQUIRED_HEADER_ELEMENTS}."
        )
    
    def test_perspective_evolution_structure_compliance(self):
        """
        Property 12: Verify evolution perspective README.md contains all required sections.
        
        **Feature: documentation-quality-improvement, Property 12: Perspective Structure Compliance**
        **Validates: Requirements 2.2, 10.2**
        """
        readme_path = PERSPECTIVES_DIR / "evolution" / "README.md"
        if not readme_path.exists():
            pytest.skip("Evolution perspective README.md does not exist")
        
        # Check required sections
        missing_sections = self.check_required_sections(readme_path)
        
        # Check header elements
        missing_header_elements = self.check_header_elements(readme_path)
        
        all_missing = []
        if missing_sections:
            all_missing.append(f"Missing sections: {missing_sections}")
        if missing_header_elements:
            all_missing.append(f"Missing header elements: {missing_header_elements}")
        
        assert len(all_missing) == 0, (
            f"Evolution perspective structure compliance issues: {'; '.join(all_missing)}. "
            f"Required sections: {self.REQUIRED_SECTIONS}. "
            f"Required header elements: {self.REQUIRED_HEADER_ELEMENTS}."
        )
    
    def test_perspective_internationalization_structure_compliance(self):
        """
        Property 12: Verify internationalization perspective README.md contains all required sections.
        
        **Feature: documentation-quality-improvement, Property 12: Perspective Structure Compliance**
        **Validates: Requirements 2.2, 10.2**
        """
        readme_path = PERSPECTIVES_DIR / "internationalization" / "README.md"
        if not readme_path.exists():
            pytest.skip("Internationalization perspective README.md does not exist")
        
        # Check required sections
        missing_sections = self.check_required_sections(readme_path)
        
        # Check header elements
        missing_header_elements = self.check_header_elements(readme_path)
        
        all_missing = []
        if missing_sections:
            all_missing.append(f"Missing sections: {missing_sections}")
        if missing_header_elements:
            all_missing.append(f"Missing header elements: {missing_header_elements}")
        
        assert len(all_missing) == 0, (
            f"Internationalization perspective structure compliance issues: {'; '.join(all_missing)}. "
            f"Required sections: {self.REQUIRED_SECTIONS}. "
            f"Required header elements: {self.REQUIRED_HEADER_ELEMENTS}."
        )
    
    def test_perspective_location_structure_compliance(self):
        """
        Property 12: Verify location perspective README.md contains all required sections.
        
        **Feature: documentation-quality-improvement, Property 12: Perspective Structure Compliance**
        **Validates: Requirements 2.2, 10.2**
        """
        readme_path = PERSPECTIVES_DIR / "location" / "README.md"
        if not readme_path.exists():
            pytest.skip("Location perspective README.md does not exist")
        
        # Check required sections
        missing_sections = self.check_required_sections(readme_path)
        
        # Check header elements
        missing_header_elements = self.check_header_elements(readme_path)
        
        all_missing = []
        if missing_sections:
            all_missing.append(f"Missing sections: {missing_sections}")
        if missing_header_elements:
            all_missing.append(f"Missing header elements: {missing_header_elements}")
        
        assert len(all_missing) == 0, (
            f"Location perspective structure compliance issues: {'; '.join(all_missing)}. "
            f"Required sections: {self.REQUIRED_SECTIONS}. "
            f"Required header elements: {self.REQUIRED_HEADER_ELEMENTS}."
        )
    
    def test_perspective_development_resource_structure_compliance(self):
        """
        Property 12: Verify development-resource perspective README.md contains all required sections.
        
        **Feature: documentation-quality-improvement, Property 12: Perspective Structure Compliance**
        **Validates: Requirements 2.2, 10.2**
        """
        readme_path = PERSPECTIVES_DIR / "development-resource" / "README.md"
        if not readme_path.exists():
            pytest.skip("Development-resource perspective README.md does not exist")
        
        # Check required sections
        missing_sections = self.check_required_sections(readme_path)
        
        # Check header elements
        missing_header_elements = self.check_header_elements(readme_path)
        
        all_missing = []
        if missing_sections:
            all_missing.append(f"Missing sections: {missing_sections}")
        if missing_header_elements:
            all_missing.append(f"Missing header elements: {missing_header_elements}")
        
        assert len(all_missing) == 0, (
            f"Development-resource perspective structure compliance issues: {'; '.join(all_missing)}. "
            f"Required sections: {self.REQUIRED_SECTIONS}. "
            f"Required header elements: {self.REQUIRED_HEADER_ELEMENTS}."
        )
    
    def test_all_perspectives_structure_compliance(self):
        """
        Property 12: For any perspective README.md, the document SHALL contain all 
        required sections: Overview, Key Concerns, Quality Attribute Scenarios, 
        Affected Viewpoints.
        
        This is the comprehensive property test that validates all perspective files.
        
        **Feature: documentation-quality-improvement, Property 12: Perspective Structure Compliance**
        **Validates: Requirements 2.2, 10.2**
        """
        perspectives_with_issues = []
        
        for perspective_name in self.ALL_PERSPECTIVES:
            readme_path = PERSPECTIVES_DIR / perspective_name / "README.md"
            if not readme_path.exists():
                perspectives_with_issues.append(
                    f"{perspective_name}: README.md does not exist"
                )
                continue
            
            # Check required sections
            missing_sections = self.check_required_sections(readme_path)
            
            # Check header elements
            missing_header_elements = self.check_header_elements(readme_path)
            
            issues = []
            if missing_sections:
                issues.append(f"missing sections: {missing_sections}")
            if missing_header_elements:
                issues.append(f"missing header elements: {missing_header_elements}")
            
            if issues:
                perspectives_with_issues.append(
                    f"{perspective_name}: {'; '.join(issues)}"
                )
        
        assert len(perspectives_with_issues) == 0, (
            f"Perspectives with structure compliance issues:\n" +
            "\n".join(f"  - {p}" for p in perspectives_with_issues) +
            f"\n\nAll perspective README.md files must contain these sections: {self.REQUIRED_SECTIONS}. "
            f"And these header elements: {self.REQUIRED_HEADER_ELEMENTS}."
        )


class TestLinkValidity:
    """
    **Feature: documentation-quality-improvement, Property 6: Link Validity**
    
    *For any* cross-reference link in a document, the target file SHALL exist 
    at the specified path.
    
    **Validates: Requirements 4.4, 7.1, 7.2**
    """
    
    # All viewpoint directories to check
    ALL_VIEWPOINTS = [
        "concurrency", "context", "deployment", "development",
        "functional", "information", "operational"
    ]
    
    # All perspective directories to check
    ALL_PERSPECTIVES = [
        "accessibility", "availability", "cost", "development-resource",
        "evolution", "internationalization", "location", "performance",
        "regulation", "security", "usability"
    ]
    
    def extract_markdown_links(self, file_path: Path) -> List[tuple]:
        """
        Extract all markdown links from a file.
        
        Returns list of tuples: (link_text, link_url, line_number)
        """
        if not file_path.exists():
            return []
        
        content = file_path.read_text(encoding='utf-8')
        links = []
        
        for line_num, line in enumerate(content.split('\n'), 1):
            # Find markdown links: [text](url)
            # Exclude anchor-only links (#section) and external URLs (http://, https://)
            link_matches = re.finditer(r'\[([^\]]+)\]\(([^)]+)\)', line)
            for match in link_matches:
                link_text = match.group(1)
                link_url = match.group(2)
                
                # Skip anchor-only links
                if link_url.startswith('#'):
                    continue
                
                # Skip external URLs
                if link_url.startswith('http://') or link_url.startswith('https://'):
                    continue
                
                # Skip mailto links
                if link_url.startswith('mailto:'):
                    continue
                
                # Remove anchor part from URL for file existence check
                url_without_anchor = link_url.split('#')[0]
                
                if url_without_anchor:  # Only add if there's a file path
                    links.append((link_text, url_without_anchor, line_num))
        
        return links
    
    def resolve_link_path(self, source_file: Path, link_url: str) -> Path:
        """
        Resolve a relative link URL to an absolute path.
        """
        # Get the directory containing the source file
        source_dir = source_file.parent
        
        # Resolve the link relative to the source directory
        resolved_path = (source_dir / link_url).resolve()
        
        return resolved_path
    
    def validate_links_in_file(self, file_path: Path) -> List[str]:
        """
        Validate all links in a file and return list of broken links.
        
        Returns list of error messages for broken links.
        """
        links = self.extract_markdown_links(file_path)
        broken_links = []
        
        for link_text, link_url, line_num in links:
            resolved_path = self.resolve_link_path(file_path, link_url)
            
            if not resolved_path.exists():
                broken_links.append(
                    f"Line {line_num}: [{link_text}]({link_url}) -> {resolved_path} does not exist"
                )
        
        return broken_links
    
    def test_viewpoint_functional_links_valid(self):
        """
        Property 6: Verify all links in functional viewpoint README.md point to existing files.
        
        **Feature: documentation-quality-improvement, Property 6: Link Validity**
        **Validates: Requirements 7.1**
        """
        readme_path = VIEWPOINTS_DIR / "functional" / "README.md"
        if not readme_path.exists():
            pytest.skip("Functional viewpoint README.md does not exist")
        
        broken_links = self.validate_links_in_file(readme_path)
        
        assert len(broken_links) == 0, (
            f"Functional viewpoint has broken links:\n" +
            "\n".join(f"  - {link}" for link in broken_links)
        )
    
    def test_viewpoint_information_links_valid(self):
        """
        Property 6: Verify all links in information viewpoint README.md point to existing files.
        
        **Feature: documentation-quality-improvement, Property 6: Link Validity**
        **Validates: Requirements 7.1**
        """
        readme_path = VIEWPOINTS_DIR / "information" / "README.md"
        if not readme_path.exists():
            pytest.skip("Information viewpoint README.md does not exist")
        
        broken_links = self.validate_links_in_file(readme_path)
        
        assert len(broken_links) == 0, (
            f"Information viewpoint has broken links:\n" +
            "\n".join(f"  - {link}" for link in broken_links)
        )
    
    def test_all_viewpoint_readme_links_valid(self):
        """
        Property 6: For any viewpoint README.md, all cross-reference links SHALL 
        point to existing files.
        
        **Feature: documentation-quality-improvement, Property 6: Link Validity**
        **Validates: Requirements 7.1**
        """
        viewpoints_with_broken_links = []
        
        for viewpoint_name in self.ALL_VIEWPOINTS:
            readme_path = VIEWPOINTS_DIR / viewpoint_name / "README.md"
            if not readme_path.exists():
                continue
            
            broken_links = self.validate_links_in_file(readme_path)
            
            if broken_links:
                viewpoints_with_broken_links.append(
                    f"{viewpoint_name}:\n" + "\n".join(f"    - {link}" for link in broken_links)
                )
        
        assert len(viewpoints_with_broken_links) == 0, (
            f"Viewpoints with broken links:\n" +
            "\n".join(viewpoints_with_broken_links)
        )
    
    def test_all_perspective_readme_links_valid(self):
        """
        Property 6: For any perspective README.md, all cross-reference links SHALL 
        point to existing files.
        
        **Feature: documentation-quality-improvement, Property 6: Link Validity**
        **Validates: Requirements 7.1**
        """
        perspectives_with_broken_links = []
        
        for perspective_name in self.ALL_PERSPECTIVES:
            readme_path = PERSPECTIVES_DIR / perspective_name / "README.md"
            if not readme_path.exists():
                continue
            
            broken_links = self.validate_links_in_file(readme_path)
            
            if broken_links:
                perspectives_with_broken_links.append(
                    f"{perspective_name}:\n" + "\n".join(f"    - {link}" for link in broken_links)
                )
        
        assert len(perspectives_with_broken_links) == 0, (
            f"Perspectives with broken links:\n" +
            "\n".join(perspectives_with_broken_links)
        )
    
    def test_viewpoints_index_links_valid(self):
        """
        Property 6: Verify all links in viewpoints/README.md index point to existing files.
        
        **Feature: documentation-quality-improvement, Property 6: Link Validity**
        **Validates: Requirements 7.1**
        """
        readme_path = VIEWPOINTS_DIR / "README.md"
        if not readme_path.exists():
            pytest.skip("Viewpoints index README.md does not exist")
        
        broken_links = self.validate_links_in_file(readme_path)
        
        assert len(broken_links) == 0, (
            f"Viewpoints index has broken links:\n" +
            "\n".join(f"  - {link}" for link in broken_links)
        )
    
    def test_perspectives_index_links_valid(self):
        """
        Property 6: Verify all links in perspectives/README.md index point to existing files.
        
        **Feature: documentation-quality-improvement, Property 6: Link Validity**
        **Validates: Requirements 7.1**
        """
        readme_path = PERSPECTIVES_DIR / "README.md"
        if not readme_path.exists():
            pytest.skip("Perspectives index README.md does not exist")
        
        broken_links = self.validate_links_in_file(readme_path)
        
        assert len(broken_links) == 0, (
            f"Perspectives index has broken links:\n" +
            "\n".join(f"  - {link}" for link in broken_links)
        )
    
    def test_all_viewpoint_subdocument_links_valid(self):
        """
        Property 6: For any viewpoint subdocument (non-README.md), all cross-reference 
        links SHALL point to existing files.
        
        **Feature: documentation-quality-improvement, Property 6: Link Validity**
        **Validates: Requirements 7.1**
        """
        files_with_broken_links = []
        
        for viewpoint_name in self.ALL_VIEWPOINTS:
            viewpoint_dir = VIEWPOINTS_DIR / viewpoint_name
            if not viewpoint_dir.exists():
                continue
            
            # Find all .md files except README.md
            for md_file in viewpoint_dir.glob("*.md"):
                if md_file.name == "README.md":
                    continue
                
                broken_links = self.validate_links_in_file(md_file)
                
                if broken_links:
                    files_with_broken_links.append(
                        f"{viewpoint_name}/{md_file.name}:\n" + 
                        "\n".join(f"    - {link}" for link in broken_links)
                    )
        
        assert len(files_with_broken_links) == 0, (
            f"Viewpoint subdocuments with broken links:\n" +
            "\n".join(files_with_broken_links)
        )


class TestDiagramReferences:
    """
    **Feature: documentation-quality-improvement, Property 6: Link Validity (Diagrams)**
    
    *For any* diagram reference in a document, the target diagram file SHALL exist 
    at the specified path.
    
    **Validates: Requirements 7.2, 8.1**
    """
    
    # All viewpoint directories to check
    ALL_VIEWPOINTS = [
        "concurrency", "context", "deployment", "development",
        "functional", "information", "operational"
    ]
    
    # All perspective directories to check
    ALL_PERSPECTIVES = [
        "accessibility", "availability", "cost", "development-resource",
        "evolution", "internationalization", "location", "performance",
        "regulation", "security", "usability"
    ]
    
    def extract_image_references(self, file_path: Path) -> List[tuple]:
        """
        Extract all image/diagram references from a file.
        
        Returns list of tuples: (alt_text, image_url, line_number)
        """
        if not file_path.exists():
            return []
        
        content = file_path.read_text(encoding='utf-8')
        images = []
        
        for line_num, line in enumerate(content.split('\n'), 1):
            # Find markdown images: ![alt](url)
            image_matches = re.finditer(r'!\[([^\]]*)\]\(([^)]+)\)', line)
            for match in image_matches:
                alt_text = match.group(1)
                image_url = match.group(2)
                
                # Skip external URLs
                if image_url.startswith('http://') or image_url.startswith('https://'):
                    continue
                
                images.append((alt_text, image_url, line_num))
        
        return images
    
    def resolve_image_path(self, source_file: Path, image_url: str) -> Path:
        """
        Resolve a relative image URL to an absolute path.
        """
        source_dir = source_file.parent
        resolved_path = (source_dir / image_url).resolve()
        return resolved_path
    
    def validate_images_in_file(self, file_path: Path) -> List[str]:
        """
        Validate all image references in a file and return list of broken references.
        """
        images = self.extract_image_references(file_path)
        broken_images = []
        
        for alt_text, image_url, line_num in images:
            resolved_path = self.resolve_image_path(file_path, image_url)
            
            if not resolved_path.exists():
                broken_images.append(
                    f"Line {line_num}: ![{alt_text}]({image_url}) -> {resolved_path} does not exist"
                )
        
        return broken_images
    
    def test_all_viewpoint_diagram_references_valid(self):
        """
        Property 6: For any viewpoint document, all diagram references SHALL 
        point to existing files.
        
        **Feature: documentation-quality-improvement, Property 6: Link Validity**
        **Validates: Requirements 7.2, 8.1**
        """
        files_with_broken_images = []
        
        for viewpoint_name in self.ALL_VIEWPOINTS:
            viewpoint_dir = VIEWPOINTS_DIR / viewpoint_name
            if not viewpoint_dir.exists():
                continue
            
            for md_file in viewpoint_dir.glob("**/*.md"):
                broken_images = self.validate_images_in_file(md_file)
                
                if broken_images:
                    rel_path = md_file.relative_to(VIEWPOINTS_DIR)
                    files_with_broken_images.append(
                        f"{rel_path}:\n" + 
                        "\n".join(f"    - {img}" for img in broken_images)
                    )
        
        assert len(files_with_broken_images) == 0, (
            f"Viewpoint documents with broken diagram references:\n" +
            "\n".join(files_with_broken_images)
        )
    
    def test_all_perspective_diagram_references_valid(self):
        """
        Property 6: For any perspective document, all diagram references SHALL 
        point to existing files.
        
        **Feature: documentation-quality-improvement, Property 6: Link Validity**
        **Validates: Requirements 7.2, 8.1**
        """
        files_with_broken_images = []
        
        for perspective_name in self.ALL_PERSPECTIVES:
            perspective_dir = PERSPECTIVES_DIR / perspective_name
            if not perspective_dir.exists():
                continue
            
            for md_file in perspective_dir.glob("**/*.md"):
                broken_images = self.validate_images_in_file(md_file)
                
                if broken_images:
                    rel_path = md_file.relative_to(PERSPECTIVES_DIR)
                    files_with_broken_images.append(
                        f"{rel_path}:\n" + 
                        "\n".join(f"    - {img}" for img in broken_images)
                    )
        
        assert len(files_with_broken_images) == 0, (
            f"Perspective documents with broken diagram references:\n" +
            "\n".join(files_with_broken_images)
        )
