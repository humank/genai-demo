#!/usr/bin/env python3
"""
DDD Code Analyzer and PlantUML Generator

This script analyzes Java code with DDD annotations and generates PlantUML diagrams:
- Domain model class diagrams
- Aggregate root detailed design diagrams
- Domain event flow diagrams
"""

import os
import re
import json
from pathlib import Path
from typing import Dict, List, Set, Optional, Tuple
from dataclasses import dataclass, field
from datetime import datetime

@dataclass
class DDDAnnotation:
    """Represents a DDD annotation with its properties"""
    type: str  # AggregateRoot, Entity, ValueObject
    name: str
    description: str
    bounded_context: str = ""
    version: str = ""

@dataclass
class JavaClass:
    """Represents a Java class with DDD annotations"""
    name: str
    package: str
    file_path: str
    annotation: Optional[DDDAnnotation] = None
    fields: List[str] = field(default_factory=list)
    methods: List[str] = field(default_factory=list)
    relationships: List[str] = field(default_factory=list)
    extends: Optional[str] = None
    implements: List[str] = field(default_factory=list)

@dataclass
class DomainEvent:
    """Represents a domain event"""
    name: str
    package: str
    file_path: str
    fields: List[str] = field(default_factory=list)
    aggregate_id_field: str = ""
    event_type: str = ""

class DDDCodeAnalyzer:
    """Analyzes Java code for DDD patterns and generates PlantUML diagrams"""
    
    def __init__(self, source_dir: str, output_dir: str):
        self.source_dir = Path(source_dir)
        self.output_dir = Path(output_dir)
        self.classes: List[JavaClass] = []
        self.events: List[DomainEvent] = []
        self.bounded_contexts: Set[str] = set()
        self.application_services: List[JavaClass] = []
        self.repositories: List[JavaClass] = []
        self.controllers: List[JavaClass] = []
        
    def analyze(self):
        """Main analysis method"""
        print("🔍 Analyzing DDD code structure...")
        self._scan_java_files()
        self._analyze_relationships()
        self._generate_diagrams()
        print("✅ DDD code analysis completed!")
        
    def _scan_java_files(self):
        """Scan all Java files for DDD annotations"""
        java_files = list(self.source_dir.rglob("*.java"))
        print(f"📁 Found {len(java_files)} Java files")
        
        for file_path in java_files:
            self._analyze_java_file(file_path)
            
    def _analyze_java_file(self, file_path: Path):
        """Analyze a single Java file"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
                
            # Check for DDD annotations
            if self._has_ddd_annotation(content):
                java_class = self._parse_java_class(file_path, content)
                if java_class:
                    self.classes.append(java_class)
                    if java_class.annotation and java_class.annotation.bounded_context:
                        self.bounded_contexts.add(java_class.annotation.bounded_context)
                        
            # Check for domain events
            if "DomainEvent" in content and "record" in content:
                event = self._parse_domain_event(file_path, content)
                if event:
                    self.events.append(event)
                    
            # Check for application services
            if self._is_application_service(content):
                java_class = self._parse_application_service(file_path, content)
                if java_class:
                    self.application_services.append(java_class)
                    
            # Check for repositories
            if self._is_repository(content):
                java_class = self._parse_repository(file_path, content)
                if java_class:
                    self.repositories.append(java_class)
                    
            # Check for controllers
            if self._is_controller(content):
                java_class = self._parse_controller(file_path, content)
                if java_class:
                    self.controllers.append(java_class)
                    
        except Exception as e:
            print(f"⚠️  Error analyzing {file_path}: {e}")
            
    def _has_ddd_annotation(self, content: str) -> bool:
        """Check if content has DDD annotations"""
        ddd_patterns = [
            r'@AggregateRoot',
            r'@Entity\(',
            r'@ValueObject'
        ]
        return any(re.search(pattern, content) for pattern in ddd_patterns)
        
    def _is_application_service(self, content: str) -> bool:
        """Check if content is an application service"""
        return (
            "@Service" in content and 
            ("ApplicationService" in content or "application" in content.lower()) and
            "@Transactional" in content
        )
        
    def _is_repository(self, content: str) -> bool:
        """Check if content is a repository"""
        return (
            "@Repository" in content or 
            "Repository" in content or
            "extends JpaRepository" in content or
            "extends CrudRepository" in content
        )
        
    def _is_controller(self, content: str) -> bool:
        """Check if content is a controller"""
        return (
            "@RestController" in content or 
            "@Controller" in content
        )
        
    def _parse_java_class(self, file_path: Path, content: str) -> Optional[JavaClass]:
        """Parse Java class with DDD annotations"""
        try:
            # Extract package
            package_match = re.search(r'package\s+([\w.]+);', content)
            package = package_match.group(1) if package_match else ""
            
            # Extract class name
            class_match = re.search(r'public\s+(?:class|record|enum)\s+(\w+)', content)
            if not class_match:
                return None
            class_name = class_match.group(1)
            
            # Parse DDD annotation
            annotation = self._parse_ddd_annotation(content)
            
            # Extract fields
            fields = self._extract_fields(content)
            
            # Extract methods
            methods = self._extract_methods(content)
            
            # Extract inheritance
            extends = self._extract_extends(content)
            implements = self._extract_implements(content)
            
            return JavaClass(
                name=class_name,
                package=package,
                file_path=str(file_path),
                annotation=annotation,
                fields=fields,
                methods=methods,
                extends=extends,
                implements=implements
            )
            
        except Exception as e:
            print(f"⚠️  Error parsing class in {file_path}: {e}")
            return None
            
    def _parse_ddd_annotation(self, content: str) -> Optional[DDDAnnotation]:
        """Parse DDD annotation from content"""
        # AggregateRoot annotation
        aggregate_match = re.search(
            r'@AggregateRoot\(([^)]+)\)', content, re.DOTALL
        )
        if aggregate_match:
            params = aggregate_match.group(1)
            return DDDAnnotation(
                type="AggregateRoot",
                name=self._extract_param(params, "name"),
                description=self._extract_param(params, "description"),
                bounded_context=self._extract_param(params, "boundedContext"),
                version=self._extract_param(params, "version")
            )
            
        # Entity annotation
        entity_match = re.search(
            r'@Entity\(([^)]+)\)', content, re.DOTALL
        )
        if entity_match:
            params = entity_match.group(1)
            return DDDAnnotation(
                type="Entity",
                name=self._extract_param(params, "name"),
                description=self._extract_param(params, "description")
            )
            
        # ValueObject annotation
        value_object_match = re.search(
            r'@ValueObject(?:\(([^)]+)\))?', content, re.DOTALL
        )
        if value_object_match:
            params = value_object_match.group(1) if value_object_match.group(1) else ""
            return DDDAnnotation(
                type="ValueObject",
                name=self._extract_param(params, "name") if params else "",
                description=self._extract_param(params, "description") if params else ""
            )
            
        return None
        
    def _parse_application_service(self, file_path: Path, content: str) -> Optional[JavaClass]:
        """Parse application service"""
        try:
            package_match = re.search(r'package\s+([\w.]+);', content)
            package = package_match.group(1) if package_match else ""
            
            class_match = re.search(r'public\s+class\s+(\w+)', content)
            if not class_match:
                return None
            class_name = class_match.group(1)
            
            # Extract methods
            methods = self._extract_public_methods(content)
            
            return JavaClass(
                name=class_name,
                package=package,
                file_path=str(file_path),
                annotation=DDDAnnotation(type="ApplicationService", name=class_name, description="Application Service"),
                methods=methods
            )
        except Exception as e:
            print(f"⚠️  Error parsing application service in {file_path}: {e}")
            return None
            
    def _parse_repository(self, file_path: Path, content: str) -> Optional[JavaClass]:
        """Parse repository"""
        try:
            package_match = re.search(r'package\s+([\w.]+);', content)
            package = package_match.group(1) if package_match else ""
            
            # Handle both interface and class repositories
            class_match = re.search(r'public\s+(?:interface|class)\s+(\w+)', content)
            if not class_match:
                return None
            class_name = class_match.group(1)
            
            # Extract methods
            methods = self._extract_repository_methods(content)
            
            return JavaClass(
                name=class_name,
                package=package,
                file_path=str(file_path),
                annotation=DDDAnnotation(type="Repository", name=class_name, description="Repository"),
                methods=methods
            )
        except Exception as e:
            print(f"⚠️  Error parsing repository in {file_path}: {e}")
            return None
            
    def _parse_controller(self, file_path: Path, content: str) -> Optional[JavaClass]:
        """Parse controller"""
        try:
            package_match = re.search(r'package\s+([\w.]+);', content)
            package = package_match.group(1) if package_match else ""
            
            class_match = re.search(r'public\s+class\s+(\w+)', content)
            if not class_match:
                return None
            class_name = class_match.group(1)
            
            # Extract REST endpoints
            methods = self._extract_rest_endpoints(content)
            
            return JavaClass(
                name=class_name,
                package=package,
                file_path=str(file_path),
                annotation=DDDAnnotation(type="Controller", name=class_name, description="REST Controller"),
                methods=methods
            )
        except Exception as e:
            print(f"⚠️  Error parsing controller in {file_path}: {e}")
            return None
        
    def _extract_param(self, params: str, param_name: str) -> str:
        """Extract parameter value from annotation parameters"""
        pattern = rf'{param_name}\s*=\s*"([^"]*)"'
        match = re.search(pattern, params)
        return match.group(1) if match else ""
        
    def _extract_fields(self, content: str) -> List[str]:
        """Extract field declarations"""
        fields = []
        
        # Private fields
        field_patterns = [
            r'private\s+(?:final\s+)?(\w+(?:<[^>]+>)?)\s+(\w+);',
            r'private\s+(?:final\s+)?(\w+(?:<[^>]+>)?)\s+(\w+)\s*=',
        ]
        
        for pattern in field_patterns:
            matches = re.findall(pattern, content)
            for match in matches:
                field_type, field_name = match
                fields.append(f"{field_name}: {field_type}")
                
        # Record fields
        record_match = re.search(r'record\s+\w+\(([^)]+)\)', content)
        if record_match:
            record_params = record_match.group(1)
            param_matches = re.findall(r'(\w+(?:<[^>]+>)?)\s+(\w+)', record_params)
            for param_type, param_name in param_matches:
                fields.append(f"{param_name}: {param_type}")
                
        return fields
        
    def _extract_methods(self, content: str) -> List[str]:
        """Extract method declarations"""
        methods = []
        
        # Public methods
        method_pattern = r'public\s+(?:static\s+)?(\w+(?:<[^>]+>)?)\s+(\w+)\s*\([^)]*\)'
        matches = re.findall(method_pattern, content)
        
        for return_type, method_name in matches:
            if method_name not in ['equals', 'hashCode', 'toString']:
                methods.append(f"{method_name}(): {return_type}")
                
        return methods
        
    def _extract_public_methods(self, content: str) -> List[str]:
        """Extract public methods for application services"""
        methods = []
        method_pattern = r'public\s+(?:(?:@\w+\s+)*)?(\w+(?:<[^>]+>)?)\s+(\w+)\s*\([^)]*\)'
        matches = re.findall(method_pattern, content)
        
        for return_type, method_name in matches:
            if method_name not in ['equals', 'hashCode', 'toString']:
                methods.append(f"{method_name}(): {return_type}")
                
        return methods
        
    def _extract_repository_methods(self, content: str) -> List[str]:
        """Extract repository methods"""
        methods = []
        
        # JPA query methods
        query_pattern = r'@Query\([^)]+\)\s*(?:public\s+)?(\w+(?:<[^>]+>)?)\s+(\w+)\s*\([^)]*\)'
        query_matches = re.findall(query_pattern, content, re.DOTALL)
        for return_type, method_name in query_matches:
            methods.append(f"{method_name}(): {return_type} [Query]")
            
        # Standard repository methods
        method_pattern = r'(?:public\s+)?(\w+(?:<[^>]+>)?)\s+(\w+)\s*\([^)]*\)'
        method_matches = re.findall(method_pattern, content)
        for return_type, method_name in method_matches:
            if method_name.startswith(('find', 'save', 'delete', 'exists', 'count')):
                methods.append(f"{method_name}(): {return_type}")
                
        return methods
        
    def _extract_rest_endpoints(self, content: str) -> List[str]:
        """Extract REST endpoints from controllers"""
        methods = []
        
        # REST mapping annotations
        rest_patterns = [
            r'@GetMapping\([^)]*\)\s*(?:public\s+)?(\w+(?:<[^>]+>)?)\s+(\w+)\s*\([^)]*\)',
            r'@PostMapping\([^)]*\)\s*(?:public\s+)?(\w+(?:<[^>]+>)?)\s+(\w+)\s*\([^)]*\)',
            r'@PutMapping\([^)]*\)\s*(?:public\s+)?(\w+(?:<[^>]+>)?)\s+(\w+)\s*\([^)]*\)',
            r'@DeleteMapping\([^)]*\)\s*(?:public\s+)?(\w+(?:<[^>]+>)?)\s+(\w+)\s*\([^)]*\)',
            r'@RequestMapping\([^)]*\)\s*(?:public\s+)?(\w+(?:<[^>]+>)?)\s+(\w+)\s*\([^)]*\)'
        ]
        
        for pattern in rest_patterns:
            matches = re.findall(pattern, content, re.DOTALL)
            for return_type, method_name in matches:
                http_method = self._extract_http_method(pattern)
                methods.append(f"{method_name}(): {return_type} [{http_method}]")
                
        return methods
        
    def _extract_http_method(self, pattern: str) -> str:
        """Extract HTTP method from pattern"""
        if "GetMapping" in pattern:
            return "GET"
        elif "PostMapping" in pattern:
            return "POST"
        elif "PutMapping" in pattern:
            return "PUT"
        elif "DeleteMapping" in pattern:
            return "DELETE"
        else:
            return "REQUEST"
        
    def _extract_extends(self, content: str) -> Optional[str]:
        """Extract extends clause"""
        extends_match = re.search(r'extends\s+([^\s{]+)', content)
        return extends_match.group(1) if extends_match else None
        
    def _extract_implements(self, content: str) -> List[str]:
        """Extract implements clause"""
        implements_match = re.search(r'implements\s+([^{]+)', content)
        if implements_match:
            interfaces = implements_match.group(1).strip()
            return [iface.strip() for iface in interfaces.split(',')]
        return []
        
    def _parse_domain_event(self, file_path: Path, content: str) -> Optional[DomainEvent]:
        """Parse domain event from content"""
        try:
            # Extract package
            package_match = re.search(r'package\s+([\w.]+);', content)
            package = package_match.group(1) if package_match else ""
            
            # Extract event name
            event_match = re.search(r'public\s+record\s+(\w+Event)', content)
            if not event_match:
                return None
            event_name = event_match.group(1)
            
            # Extract fields from record
            fields = []
            record_match = re.search(rf'record\s+{event_name}\s*\(([^)]+)\)', content, re.DOTALL)
            if record_match:
                record_params = record_match.group(1)
                param_matches = re.findall(r'(\w+(?:<[^>]+>)?)\s+(\w+)', record_params)
                for param_type, param_name in param_matches:
                    fields.append(f"{param_name}: {param_type}")
                    
            # Find aggregate ID field
            aggregate_id_field = ""
            for field in fields:
                if "Id" in field and ("customer" in field.lower() or "order" in field.lower() or "product" in field.lower()):
                    aggregate_id_field = field.split(":")[0].strip()
                    break
                    
            return DomainEvent(
                name=event_name,
                package=package,
                file_path=str(file_path),
                fields=fields,
                aggregate_id_field=aggregate_id_field,
                event_type=event_name
            )
            
        except Exception as e:
            print(f"⚠️  Error parsing event in {file_path}: {e}")
            return None
            
    def _analyze_relationships(self):
        """Analyze relationships between classes"""
        print("🔗 Analyzing relationships...")
        
        for java_class in self.classes:
            # Analyze field types for relationships
            for field in java_class.fields:
                field_type = field.split(":")[1].strip()
                
                # Remove generics
                field_type = re.sub(r'<[^>]+>', '', field_type)
                field_type = re.sub(r'List|Set|Map', '', field_type).strip()
                
                # Find related classes
                for other_class in self.classes:
                    if other_class.name in field_type and other_class.name != java_class.name:
                        relationship = f"{java_class.name} --> {other_class.name}"
                        if relationship not in java_class.relationships:
                            java_class.relationships.append(relationship)
                            
    def _generate_diagrams(self):
        """Generate all PlantUML diagrams"""
        print("📊 Generating PlantUML diagrams...")
        
        self.output_dir.mkdir(parents=True, exist_ok=True)
        
        # Generate domain model overview
        self._generate_domain_model_overview()
        
        # Generate aggregate root details for each bounded context
        for context in self.bounded_contexts:
            self._generate_aggregate_details(context)
            
        # Generate domain events diagram
        self._generate_domain_events_diagram()
        
        # Generate bounded context diagram
        self._generate_bounded_context_diagram()
        
        # Generate hexagonal architecture diagram
        self._generate_hexagonal_architecture_diagram()
        
        # Generate application services diagram
        self._generate_application_services_diagram()
        
        # Generate infrastructure layer diagram
        self._generate_infrastructure_diagram()
        
    def _generate_domain_model_overview(self):
        """Generate domain model overview diagram"""
        output_file = self.output_dir / "domain-model-overview.puml"
        
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write("@startuml Domain Model Overview\n")
            f.write("!theme plain\n")
            f.write("skinparam classAttributeIconSize 0\n")
            f.write("skinparam classFontStyle bold\n\n")
            
            f.write("title Domain Model Overview\\n")
            f.write(f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\\n\\n\n\n")
            
            # Group by bounded context
            contexts = {}
            for java_class in self.classes:
                if java_class.annotation:
                    context = java_class.annotation.bounded_context or "Common"
                    if context not in contexts:
                        contexts[context] = []
                    contexts[context].append(java_class)
                    
            # Generate packages for each bounded context
            for context, classes in contexts.items():
                f.write(f"package \"{context} Context\" {{\n")
                
                for java_class in classes:
                    self._write_class_definition(f, java_class, summary=True)
                    
                f.write("}\n\n")
                
            # Add relationships
            f.write("' Relationships\n")
            for java_class in self.classes:
                for relationship in java_class.relationships:
                    f.write(f"{relationship}\n")
                    
            f.write("\n@enduml\n")
            
        print(f"✅ Generated domain model overview: {output_file}")
        
    def _generate_aggregate_details(self, bounded_context: str):
        """Generate detailed diagram for a specific bounded context"""
        aggregates = [c for c in self.classes 
                     if c.annotation and c.annotation.type == "AggregateRoot" 
                     and c.annotation.bounded_context == bounded_context]
        
        if not aggregates:
            return
            
        output_file = self.output_dir / f"{bounded_context.lower()}-aggregate-details.puml"
        
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write(f"@startuml {bounded_context} Aggregate Details\n")
            f.write("!theme plain\n")
            f.write("skinparam classAttributeIconSize 0\n")
            f.write("skinparam classFontStyle bold\n\n")
            
            f.write(f"title {bounded_context} Bounded Context - Aggregate Details\\n")
            f.write(f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\\n\\n\n\n")
            
            # Get all related classes in this context
            context_classes = [c for c in self.classes 
                             if c.annotation and c.annotation.bounded_context == bounded_context]
            
            for java_class in context_classes:
                self._write_class_definition(f, java_class, summary=False)
                
            # Add relationships within context
            f.write("\n' Relationships\n")
            for java_class in context_classes:
                for relationship in java_class.relationships:
                    # Only include relationships within this context
                    target_class = relationship.split(" --> ")[1]
                    if any(c.name == target_class and c.annotation and 
                          c.annotation.bounded_context == bounded_context 
                          for c in self.classes):
                        f.write(f"{relationship}\n")
                        
            f.write("\n@enduml\n")
            
        print(f"✅ Generated {bounded_context} aggregate details: {output_file}")
        
    def _write_class_definition(self, f, java_class: JavaClass, summary: bool = False):
        """Write class definition to PlantUML file"""
        annotation = java_class.annotation
        if not annotation:
            return
            
        # Determine stereotype
        stereotype = f"<<{annotation.type}>>"
        
        # Class header
        f.write(f"class {java_class.name} {stereotype} {{\n")
        
        if not summary:
            # Add fields
            if java_class.fields:
                for field in java_class.fields[:5]:  # Limit fields for readability
                    f.write(f"  -{field}\n")
                if len(java_class.fields) > 5:
                    f.write(f"  -... ({len(java_class.fields) - 5} more fields)\n")
                f.write("  --\n")
                
            # Add key methods
            if java_class.methods:
                for method in java_class.methods[:3]:  # Limit methods for readability
                    f.write(f"  +{method}\n")
                if len(java_class.methods) > 3:
                    f.write(f"  +... ({len(java_class.methods) - 3} more methods)\n")
        else:
            # Summary view - just show it's a class
            f.write(f"  {annotation.description[:50]}...\n" if annotation.description else "")
            
        f.write("}\n\n")
        
        # Add note with description
        if annotation.description and not summary:
            f.write(f"note right of {java_class.name}\n")
            f.write(f"  {annotation.description}\n")
            if annotation.version:
                f.write(f"  Version: {annotation.version}\n")
            f.write("end note\n\n")
            
    def _generate_domain_events_diagram(self):
        """Generate domain events flow diagram"""
        if not self.events:
            return
            
        output_file = self.output_dir / "domain-events-flow.puml"
        
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write("@startuml Domain Events Flow\n")
            f.write("!theme plain\n")
            f.write("skinparam classAttributeIconSize 0\n\n")
            
            f.write("title Domain Events Flow\\n")
            f.write(f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\\n\\n\n\n")
            
            # Group events by aggregate
            events_by_aggregate = {}
            for event in self.events:
                # Extract aggregate from package or event name
                aggregate = self._extract_aggregate_from_event(event)
                if aggregate not in events_by_aggregate:
                    events_by_aggregate[aggregate] = []
                events_by_aggregate[aggregate].append(event)
                
            # Generate event classes
            for aggregate, events in events_by_aggregate.items():
                f.write(f"package \"{aggregate} Events\" {{\n")
                
                for event in events:
                    f.write(f"  class {event.name} <<DomainEvent>> {{\n")
                    for field in event.fields[:3]:  # Limit fields
                        f.write(f"    +{field}\n")
                    if len(event.fields) > 3:
                        f.write(f"    +... ({len(event.fields) - 3} more)\n")
                    f.write("  }\n\n")
                    
                f.write("}\n\n")
                
            # Add event flow relationships
            f.write("' Event Flow\n")
            for aggregate, events in events_by_aggregate.items():
                if len(events) > 1:
                    for i in range(len(events) - 1):
                        f.write(f"{events[i].name} ..> {events[i+1].name} : triggers\n")
                        
            f.write("\n@enduml\n")
            
        print(f"✅ Generated domain events flow: {output_file}")
        
    def _extract_aggregate_from_event(self, event: DomainEvent) -> str:
        """Extract aggregate name from event"""
        # Try to extract from package
        package_parts = event.package.split('.')
        for part in package_parts:
            if part in ['customer', 'order', 'product', 'inventory', 'payment']:
                return part.capitalize()
                
        # Try to extract from event name
        event_name = event.name.replace('Event', '')
        if 'Customer' in event_name:
            return 'Customer'
        elif 'Order' in event_name:
            return 'Order'
        elif 'Product' in event_name:
            return 'Product'
        elif 'Inventory' in event_name:
            return 'Inventory'
        elif 'Payment' in event_name:
            return 'Payment'
            
        return 'Common'
        
    def _generate_bounded_context_diagram(self):
        """Generate bounded context overview diagram"""
        if not self.bounded_contexts:
            return
            
        output_file = self.output_dir / "bounded-contexts-overview.puml"
        
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write("@startuml Bounded Contexts Overview\n")
            f.write("!theme plain\n\n")
            
            f.write("title Bounded Contexts Overview\\n")
            f.write(f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\\n\\n\n\n")
            
            # Create bounded context boxes
            for context in sorted(self.bounded_contexts):
                aggregates = [c for c in self.classes 
                             if c.annotation and c.annotation.type == "AggregateRoot" 
                             and c.annotation.bounded_context == context]
                
                f.write(f"rectangle \"{context} Context\" as {context} {{\n")
                
                for aggregate in aggregates:
                    f.write(f"  [<b>{aggregate.name}</b>\\n{aggregate.annotation.description[:30]}...] as {aggregate.name}\n")
                    
                f.write("}\n\n")
                
            # Add context relationships (simplified)
            contexts_list = list(sorted(self.bounded_contexts))
            for i, context in enumerate(contexts_list):
                if i < len(contexts_list) - 1:
                    next_context = contexts_list[i + 1]
                    f.write(f"{context} ..> {next_context} : integrates\n")
                    
            f.write("\n@enduml\n")
            
        print(f"✅ Generated bounded contexts overview: {output_file}")
        
    def _generate_hexagonal_architecture_diagram(self):
        """Generate hexagonal architecture overview diagram"""
        output_file = self.output_dir / "hexagonal-architecture-overview.puml"
        
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write("@startuml Hexagonal Architecture Overview\n")
            f.write("!theme plain\n")
            f.write("skinparam packageStyle rectangle\n\n")
            
            f.write("title Hexagonal Architecture Overview\\n")
            f.write(f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\\n\\n\n\n")
            
            # Domain Core (center)
            f.write("package \"Domain Core\" {\n")
            for context in sorted(self.bounded_contexts):
                aggregates = [c for c in self.classes 
                             if c.annotation and c.annotation.type == "AggregateRoot" 
                             and c.annotation.bounded_context == context]
                if aggregates:
                    f.write(f"  package \"{context}\" {{\n")
                    for aggregate in aggregates[:2]:  # Limit for readability
                        f.write(f"    [<b>{aggregate.name}</b>] as {aggregate.name}\n")
                    if len(aggregates) > 2:
                        f.write(f"    [... {len(aggregates) - 2} more] as {context}More\n")
                    f.write("  }\n")
            f.write("}\n\n")
            
            # Application Layer
            f.write("package \"Application Layer\" {\n")
            app_services_by_context = {}
            for service in self.application_services:
                context = self._extract_context_from_package(service.package)
                if context not in app_services_by_context:
                    app_services_by_context[context] = []
                app_services_by_context[context].append(service)
                
            for context, services in app_services_by_context.items():
                f.write(f"  package \"{context} Application\" {{\n")
                for service in services[:3]:  # Limit for readability
                    f.write(f"    [<b>{service.name}</b>] as {service.name}\n")
                if len(services) > 3:
                    f.write(f"    [... {len(services) - 3} more] as {context}AppMore\n")
                f.write("  }\n")
            f.write("}\n\n")
            
            # Infrastructure Layer
            f.write("package \"Infrastructure Layer\" {\n")
            repos_by_context = {}
            for repo in self.repositories:
                context = self._extract_context_from_package(repo.package)
                if context not in repos_by_context:
                    repos_by_context[context] = []
                repos_by_context[context].append(repo)
                
            for context, repos in repos_by_context.items():
                f.write(f"  package \"{context} Infrastructure\" {{\n")
                for repo in repos[:2]:  # Limit for readability
                    f.write(f"    [<b>{repo.name}</b>] as {repo.name}\n")
                if len(repos) > 2:
                    f.write(f"    [... {len(repos) - 2} more] as {context}InfraMore\n")
                f.write("  }\n")
            f.write("}\n\n")
            
            # Interface Layer
            f.write("package \"Interface Layer\" {\n")
            for controller in self.controllers[:5]:  # Limit for readability
                f.write(f"  [<b>{controller.name}</b>] as {controller.name}\n")
            if len(self.controllers) > 5:
                f.write(f"  [... {len(self.controllers) - 5} more] as MoreControllers\n")
            f.write("}\n\n")
            
            # Add basic relationships
            f.write("' Layer Dependencies\n")
            if self.controllers and self.application_services:
                f.write(f"{self.controllers[0].name} --> {self.application_services[0].name}\n")
            if self.application_services and self.classes:
                domain_aggregate = next((c for c in self.classes if c.annotation and c.annotation.type == "AggregateRoot"), None)
                if domain_aggregate:
                    f.write(f"{self.application_services[0].name} --> {domain_aggregate.name}\n")
            if self.repositories and self.classes:
                domain_aggregate = next((c for c in self.classes if c.annotation and c.annotation.type == "AggregateRoot"), None)
                if domain_aggregate:
                    f.write(f"{self.repositories[0].name} --> {domain_aggregate.name}\n")
                    
            f.write("\n@enduml\n")
            
        print(f"✅ Generated hexagonal architecture overview: {output_file}")
        
    def _generate_application_services_diagram(self):
        """Generate application services diagram"""
        if not self.application_services:
            return
            
        output_file = self.output_dir / "application-services-overview.puml"
        
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write("@startuml Application Services Overview\n")
            f.write("!theme plain\n")
            f.write("skinparam classAttributeIconSize 0\n\n")
            
            f.write("title Application Services Overview\\n")
            f.write(f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\\n\\n\n\n")
            
            # Group by context
            services_by_context = {}
            for service in self.application_services:
                context = self._extract_context_from_package(service.package)
                if context not in services_by_context:
                    services_by_context[context] = []
                services_by_context[context].append(service)
                
            for context, services in services_by_context.items():
                f.write(f"package \"{context} Application Services\" {{\n")
                
                for service in services:
                    f.write(f"  class {service.name} <<ApplicationService>> {{\n")
                    for method in service.methods[:5]:  # Limit methods
                        f.write(f"    +{method}\n")
                    if len(service.methods) > 5:
                        f.write(f"    +... ({len(service.methods) - 5} more methods)\n")
                    f.write("  }\n\n")
                    
                f.write("}\n\n")
                
            f.write("\n@enduml\n")
            
        print(f"✅ Generated application services overview: {output_file}")
        
    def _generate_infrastructure_diagram(self):
        """Generate infrastructure layer diagram"""
        if not self.repositories:
            return
            
        output_file = self.output_dir / "infrastructure-layer-overview.puml"
        
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write("@startuml Infrastructure Layer Overview\n")
            f.write("!theme plain\n")
            f.write("skinparam classAttributeIconSize 0\n\n")
            
            f.write("title Infrastructure Layer Overview\\n")
            f.write(f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\\n\\n\n\n")
            
            # Group by context
            repos_by_context = {}
            for repo in self.repositories:
                context = self._extract_context_from_package(repo.package)
                if context not in repos_by_context:
                    repos_by_context[context] = []
                repos_by_context[context].append(repo)
                
            for context, repos in repos_by_context.items():
                f.write(f"package \"{context} Infrastructure\" {{\n")
                
                for repo in repos:
                    f.write(f"  interface {repo.name} <<Repository>> {{\n")
                    for method in repo.methods[:5]:  # Limit methods
                        f.write(f"    +{method}\n")
                    if len(repo.methods) > 5:
                        f.write(f"    +... ({len(repo.methods) - 5} more methods)\n")
                    f.write("  }\n\n")
                    
                f.write("}\n\n")
                
            f.write("\n@enduml\n")
            
        print(f"✅ Generated infrastructure layer overview: {output_file}")
        
    def _extract_context_from_package(self, package: str) -> str:
        """Extract bounded context from package name"""
        parts = package.split('.')
        for part in parts:
            if part in ['customer', 'order', 'product', 'inventory', 'payment', 'notification', 'delivery', 'promotion', 'review', 'seller', 'shoppingcart', 'pricing', 'observability']:
                return part.capitalize()
        return 'Common'

def main():
    """Main function"""
    import sys
    
    if len(sys.argv) != 3:
        print("Usage: python analyze-ddd-code.py <source_dir> <output_dir>")
        sys.exit(1)
        
    source_dir = sys.argv[1]
    output_dir = sys.argv[2]
    
    if not os.path.exists(source_dir):
        print(f"❌ Source directory not found: {source_dir}")
        sys.exit(1)
        
    analyzer = DDDCodeAnalyzer(source_dir, output_dir)
    analyzer.analyze()
    
    # Print summary
    print(f"\n📊 Analysis Summary:")
    print(f"   • Domain classes analyzed: {len(analyzer.classes)}")
    print(f"   • Application services found: {len(analyzer.application_services)}")
    print(f"   • Repositories found: {len(analyzer.repositories)}")
    print(f"   • Controllers found: {len(analyzer.controllers)}")
    print(f"   • Domain events found: {len(analyzer.events)}")
    print(f"   • Bounded contexts: {len(analyzer.bounded_contexts)}")
    print(f"   • Bounded contexts: {', '.join(sorted(analyzer.bounded_contexts))}")
    
    # Generate JSON summary
    summary_file = Path(output_dir) / "analysis-summary.json"
    summary = {
        "timestamp": datetime.now().isoformat(),
        "domain_classes_count": len(analyzer.classes),
        "application_services_count": len(analyzer.application_services),
        "repositories_count": len(analyzer.repositories),
        "controllers_count": len(analyzer.controllers),
        "events_count": len(analyzer.events),
        "bounded_contexts": list(sorted(analyzer.bounded_contexts)),
        "domain_classes": [
            {
                "name": c.name,
                "type": c.annotation.type if c.annotation else "Unknown",
                "bounded_context": c.annotation.bounded_context if c.annotation else "",
                "package": c.package
            }
            for c in analyzer.classes
        ],
        "application_services": [
            {
                "name": s.name,
                "package": s.package,
                "methods_count": len(s.methods)
            }
            for s in analyzer.application_services
        ],
        "repositories": [
            {
                "name": r.name,
                "package": r.package,
                "methods_count": len(r.methods)
            }
            for r in analyzer.repositories
        ],
        "controllers": [
            {
                "name": c.name,
                "package": c.package,
                "endpoints_count": len(c.methods)
            }
            for c in analyzer.controllers
        ],
        "events": [
            {
                "name": e.name,
                "package": e.package,
                "aggregate_id_field": e.aggregate_id_field
            }
            for e in analyzer.events
        ]
    }
    
    with open(summary_file, 'w', encoding='utf-8') as f:
        json.dump(summary, f, indent=2, ensure_ascii=False)
        
    print(f"   • Summary saved to: {summary_file}")

if __name__ == "__main__":
    main()