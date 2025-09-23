#!/usr/bin/env python3
"""
BDD Feature Analyzer and PlantUML Generator

This script analyzes Gherkin Feature files and generates PlantUML diagrams:
- Event Storming Big Picture diagrams
- Process Level Event Storming diagrams
- Business Process Flow diagrams
- User Journey diagrams
"""

import os
import re
import json
from pathlib import Path
from typing import Dict, List, Set, Optional, Tuple
from dataclasses import dataclass, field
from datetime import datetime

@dataclass
class GherkinStep:
    """Represents a Gherkin step (Given/When/Then)"""
    type: str  # Given, When, Then, And, But
    text: str
    line_number: int

@dataclass
class GherkinScenario:
    """Represents a Gherkin scenario"""
    name: str
    description: str
    steps: List[GherkinStep] = field(default_factory=list)
    tags: List[str] = field(default_factory=list)
    line_number: int = 0

@dataclass
class GherkinFeature:
    """Represents a Gherkin feature file"""
    name: str
    description: str
    file_path: str
    scenarios: List[GherkinScenario] = field(default_factory=list)
    tags: List[str] = field(default_factory=list)
    background_steps: List[GherkinStep] = field(default_factory=list)

@dataclass
class BusinessEvent:
    """Represents a business event extracted from scenarios"""
    name: str
    trigger: str
    context: str
    actors: List[str] = field(default_factory=list)
    aggregates: List[str] = field(default_factory=list)
    commands: List[str] = field(default_factory=list)

@dataclass
class UserJourney:
    """Represents a user journey extracted from scenarios"""
    name: str
    actor: str
    steps: List[str] = field(default_factory=list)
    touchpoints: List[str] = field(default_factory=list)
    outcomes: List[str] = field(default_factory=list)

class BDDFeatureAnalyzer:
    """Analyzes BDD Feature files and generates PlantUML diagrams"""
    
    def __init__(self, features_dir: str, output_dir: str):
        self.features_dir = Path(features_dir)
        self.output_dir = Path(output_dir)
        self.features: List[GherkinFeature] = []
        self.business_events: List[BusinessEvent] = []
        self.user_journeys: List[UserJourney] = []
        self.bounded_contexts: Set[str] = set()
        self.actors: Set[str] = set()
        
    def analyze(self):
        """Main analysis method"""
        print("🔍 Analyzing BDD Feature files...")
        self._scan_feature_files()
        self._extract_business_events()
        self._extract_user_journeys()
        self._generate_diagrams()
        print("✅ BDD Feature analysis completed!")
        
    def _scan_feature_files(self):
        """Scan all Feature files"""
        feature_files = list(self.features_dir.rglob("*.feature"))
        print(f"📁 Found {len(feature_files)} Feature files")
        
        for file_path in feature_files:
            self._analyze_feature_file(file_path)
            
    def _analyze_feature_file(self, file_path: Path):
        """Analyze a single Feature file"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
                
            feature = self._parse_feature_file(file_path, content)
            if feature:
                self.features.append(feature)
                
                # Extract bounded context from file path
                context = self._extract_context_from_path(file_path)
                if context:
                    self.bounded_contexts.add(context)
                    
        except Exception as e:
            print(f"⚠️  Error analyzing {file_path}: {e}")
            
    def _parse_feature_file(self, file_path: Path, content: str) -> Optional[GherkinFeature]:
        """Parse Gherkin feature file"""
        try:
            lines = content.split('\n')
            feature = None
            current_scenario = None
            current_step_type = None
            in_background = False
            
            for i, line in enumerate(lines):
                line = line.strip()
                if not line or line.startswith('#'):
                    continue
                    
                # Feature declaration
                if line.startswith('Feature:'):
                    feature_name = line.replace('Feature:', '').strip()
                    feature_desc = self._extract_feature_description(lines, i)
                    feature = GherkinFeature(
                        name=feature_name,
                        description=feature_desc,
                        file_path=str(file_path)
                    )
                    
                # Tags
                elif line.startswith('@'):
                    tags = [tag.strip() for tag in line.split() if tag.startswith('@')]
                    if current_scenario:
                        current_scenario.tags.extend(tags)
                    elif feature:
                        feature.tags.extend(tags)
                        
                # Background
                elif line.startswith('Background:'):
                    in_background = True
                    current_scenario = None
                    
                # Scenario
                elif line.startswith('Scenario:') or line.startswith('Scenario Outline:'):
                    in_background = False
                    scenario_name = re.sub(r'^Scenario(?:\s+Outline)?:', '', line).strip()
                    current_scenario = GherkinScenario(
                        name=scenario_name,
                        description="",
                        line_number=i + 1
                    )
                    if feature:
                        feature.scenarios.append(current_scenario)
                        
                # Steps
                elif re.match(r'^\s*(Given|When|Then|And|But)', line):
                    step_match = re.match(r'^\s*(Given|When|Then|And|But)\s+(.+)', line)
                    if step_match:
                        step_type = step_match.group(1)
                        step_text = step_match.group(2)
                        
                        # Handle And/But by using previous step type
                        if step_type in ['And', 'But'] and current_step_type:
                            step_type = current_step_type
                        else:
                            current_step_type = step_type
                            
                        step = GherkinStep(
                            type=step_type,
                            text=step_text,
                            line_number=i + 1
                        )
                        
                        if in_background and feature:
                            feature.background_steps.append(step)
                        elif current_scenario:
                            current_scenario.steps.append(step)
                            
            return feature
            
        except Exception as e:
            print(f"⚠️  Error parsing feature file {file_path}: {e}")
            return None
            
    def _extract_feature_description(self, lines: List[str], start_index: int) -> str:
        """Extract feature description from lines following Feature declaration"""
        description_lines = []
        for i in range(start_index + 1, len(lines)):
            line = lines[i].strip()
            if not line or line.startswith('#'):
                continue
            if line.startswith('@') or line.startswith('Background:') or line.startswith('Scenario'):
                break
            description_lines.append(line)
        return ' '.join(description_lines)
        
    def _extract_context_from_path(self, file_path: Path) -> Optional[str]:
        """Extract bounded context from file path"""
        parts = file_path.parts
        for part in parts:
            if part in ['customer', 'order', 'product', 'inventory', 'payment', 
                       'notification', 'delivery', 'promotion', 'consumer', 
                       'logistics', 'pricing', 'workflow']:
                return part.capitalize()
        return None
        
    def _extract_business_events(self):
        """Extract business events from scenarios"""
        print("🔍 Extracting business events...")
        
        for feature in self.features:
            for scenario in feature.scenarios:
                events = self._analyze_scenario_for_events(scenario, feature)
                self.business_events.extend(events)
                
    def _analyze_scenario_for_events(self, scenario: GherkinScenario, feature: GherkinFeature) -> List[BusinessEvent]:
        """Analyze a scenario to extract business events"""
        events = []
        
        # Extract actors from Given steps
        actors = set()
        for step in scenario.steps:
            if step.type == "Given":
                actor = self._extract_actor_from_step(step.text)
                if actor:
                    actors.add(actor)
                    self.actors.add(actor)
                    
        # Extract events from When steps
        for step in scenario.steps:
            if step.type == "When":
                event_name = self._extract_event_from_when_step(step.text)
                if event_name:
                    event = BusinessEvent(
                        name=event_name,
                        trigger=step.text,
                        context=feature.name,
                        actors=list(actors),
                        aggregates=self._extract_aggregates_from_scenario(scenario),
                        commands=self._extract_commands_from_scenario(scenario)
                    )
                    events.append(event)
                    
        return events
        
    def _extract_actor_from_step(self, step_text: str) -> Optional[str]:
        """Extract actor from Given step"""
        # Common patterns for actors
        patterns = [
            r'(?:a|an|the)\s+(\w+(?:\s+\w+)?)\s+(?:is|has|exists|wants|needs)',
            r'(\w+(?:\s+\w+)?)\s+(?:is|has|exists|wants|needs)',
            r'I am (?:a|an|the)\s+(\w+(?:\s+\w+)?)',
            r'As (?:a|an|the)\s+(\w+(?:\s+\w+)?)'
        ]
        
        for pattern in patterns:
            match = re.search(pattern, step_text, re.IGNORECASE)
            if match:
                actor = match.group(1).strip()
                # Filter out common non-actor words
                if actor.lower() not in ['system', 'application', 'service', 'database', 'api']:
                    return actor.title()
        return None
        
    def _extract_event_from_when_step(self, step_text: str) -> Optional[str]:
        """Extract event name from When step"""
        # Convert When step to event name
        # Remove common prefixes and convert to event format
        text = step_text.lower()
        text = re.sub(r'^(?:the\s+)?(?:user|customer|admin|system)\s+', '', text)
        text = re.sub(r'^(?:i\s+)', '', text)
        
        # Convert verb to past tense event
        event_patterns = [
            (r'creates?\s+(?:a|an|the)?\s*(.+)', r'\1 Created'),
            (r'updates?\s+(?:a|an|the)?\s*(.+)', r'\1 Updated'),
            (r'deletes?\s+(?:a|an|the)?\s*(.+)', r'\1 Deleted'),
            (r'adds?\s+(?:a|an|the)?\s*(.+)', r'\1 Added'),
            (r'removes?\s+(?:a|an|the)?\s*(.+)', r'\1 Removed'),
            (r'submits?\s+(?:a|an|the)?\s*(.+)', r'\1 Submitted'),
            (r'cancels?\s+(?:a|an|the)?\s*(.+)', r'\1 Cancelled'),
            (r'processes?\s+(?:a|an|the)?\s*(.+)', r'\1 Processed'),
            (r'applies?\s+(?:a|an|the)?\s*(.+)', r'\1 Applied'),
            (r'activates?\s+(?:a|an|the)?\s*(.+)', r'\1 Activated'),
        ]
        
        for pattern, replacement in event_patterns:
            match = re.search(pattern, text)
            if match:
                entity = match.group(1).strip()
                event_name = re.sub(pattern, replacement, text)
                return ' '.join(word.capitalize() for word in event_name.split())
                
        return None
        
    def _extract_aggregates_from_scenario(self, scenario: GherkinScenario) -> List[str]:
        """Extract aggregate names from scenario steps"""
        aggregates = set()
        
        # Common aggregate patterns
        aggregate_patterns = [
            r'\b(order|customer|product|inventory|payment|cart|promotion|voucher|delivery|notification)\b'
        ]
        
        for step in scenario.steps:
            for pattern in aggregate_patterns:
                matches = re.findall(pattern, step.text, re.IGNORECASE)
                for match in matches:
                    aggregates.add(match.capitalize())
                    
        return list(aggregates)
        
    def _extract_commands_from_scenario(self, scenario: GherkinScenario) -> List[str]:
        """Extract command names from When steps"""
        commands = []
        
        for step in scenario.steps:
            if step.type == "When":
                command = self._convert_when_to_command(step.text)
                if command:
                    commands.append(command)
                    
        return commands
        
    def _convert_when_to_command(self, when_text: str) -> Optional[str]:
        """Convert When step to command name"""
        text = when_text.lower()
        text = re.sub(r'^(?:the\s+)?(?:user|customer|admin|system)\s+', '', text)
        text = re.sub(r'^(?:i\s+)', '', text)
        
        # Convert to command format
        command_patterns = [
            (r'creates?\s+(?:a|an|the)?\s*(.+)', r'Create \1'),
            (r'updates?\s+(?:a|an|the)?\s*(.+)', r'Update \1'),
            (r'deletes?\s+(?:a|an|the)?\s*(.+)', r'Delete \1'),
            (r'adds?\s+(?:a|an|the)?\s*(.+)', r'Add \1'),
            (r'removes?\s+(?:a|an|the)?\s*(.+)', r'Remove \1'),
            (r'submits?\s+(?:a|an|the)?\s*(.+)', r'Submit \1'),
            (r'cancels?\s+(?:a|an|the)?\s*(.+)', r'Cancel \1'),
            (r'processes?\s+(?:a|an|the)?\s*(.+)', r'Process \1'),
        ]
        
        for pattern, replacement in command_patterns:
            if re.search(pattern, text):
                command = re.sub(pattern, replacement, text)
                return ' '.join(word.capitalize() for word in command.split())
                
        return None
        
    def _extract_user_journeys(self):
        """Extract user journeys from scenarios"""
        print("🔍 Extracting user journeys...")
        
        for feature in self.features:
            for scenario in feature.scenarios:
                journey = self._analyze_scenario_for_journey(scenario, feature)
                if journey:
                    self.user_journeys.append(journey)
                    
    def _analyze_scenario_for_journey(self, scenario: GherkinScenario, feature: GherkinFeature) -> Optional[UserJourney]:
        """Analyze a scenario to extract user journey"""
        # Find the main actor
        actor = None
        for step in scenario.steps:
            if step.type == "Given":
                actor = self._extract_actor_from_step(step.text)
                if actor:
                    break
                    
        if not actor:
            return None
            
        # Extract journey steps
        journey_steps = []
        touchpoints = set()
        outcomes = []
        
        for step in scenario.steps:
            if step.type == "When":
                journey_steps.append(step.text)
                # Extract touchpoints (UI, API, etc.)
                touchpoint = self._extract_touchpoint_from_step(step.text)
                if touchpoint:
                    touchpoints.add(touchpoint)
            elif step.type == "Then":
                outcomes.append(step.text)
                
        return UserJourney(
            name=scenario.name,
            actor=actor,
            steps=journey_steps,
            touchpoints=list(touchpoints),
            outcomes=outcomes
        )
        
    def _extract_touchpoint_from_step(self, step_text: str) -> Optional[str]:
        """Extract touchpoint from step text"""
        touchpoint_patterns = [
            r'\b(web|mobile|api|email|sms|notification)\b',
            r'\b(website|app|application|portal|dashboard)\b',
            r'\b(ui|interface|screen|page|form)\b'
        ]
        
        for pattern in touchpoint_patterns:
            match = re.search(pattern, step_text, re.IGNORECASE)
            if match:
                return match.group(1).capitalize()
        return None
        
    def _generate_diagrams(self):
        """Generate all PlantUML diagrams"""
        print("📊 Generating PlantUML diagrams...")
        
        self.output_dir.mkdir(parents=True, exist_ok=True)
        
        # Generate Event Storming diagrams
        self._generate_event_storming_big_picture()
        self._generate_event_storming_process_level()
        
        # Generate business process diagrams
        self._generate_business_process_flows()
        
        # Generate user journey diagrams
        self._generate_user_journey_diagrams()
        
        # Generate feature overview
        self._generate_feature_overview()
        
    def _generate_event_storming_big_picture(self):
        """Generate Event Storming Big Picture diagram"""
        output_file = self.output_dir / "event-storming-big-picture.puml"
        
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write("@startuml Event Storming Big Picture\n")
            f.write("!theme plain\n")
            f.write("skinparam backgroundColor #FFFEF7\n")
            f.write("skinparam defaultFontName Arial\n\n")
            
            f.write("title Event Storming Big Picture\\n")
            f.write(f"Generated from BDD Features: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\\n\\n\n\n")
            
            # Group events by bounded context
            events_by_context = {}
            for event in self.business_events:
                context = self._extract_context_from_event(event)
                if context not in events_by_context:
                    events_by_context[context] = []
                events_by_context[context].append(event)
                
            # Generate event flow
            for context, events in events_by_context.items():
                f.write(f"package \"{context} Context\" {{\n")
                
                for event in events:
                    # Event (Orange)
                    f.write(f"  rectangle \"{event.name}\" as {self._sanitize_id(event.name)} #FF8C00\n")
                    
                    # Actors (Yellow)
                    for actor in event.actors:
                        actor_id = self._sanitize_id(f"{actor}_{context}")
                        f.write(f"  actor \"{actor}\" as {actor_id} #FFFF99\n")
                        f.write(f"  {actor_id} --> {self._sanitize_id(event.name)}\n")
                        
                    # Commands (Blue)
                    for command in event.commands:
                        command_id = self._sanitize_id(f"{command}_{context}")
                        f.write(f"  rectangle \"{command}\" as {command_id} #87CEEB\n")
                        f.write(f"  {command_id} --> {self._sanitize_id(event.name)}\n")
                        
                f.write("}\n\n")
                
            # Add context relationships
            contexts = list(events_by_context.keys())
            for i, context in enumerate(contexts):
                if i < len(contexts) - 1:
                    next_context = contexts[i + 1]
                    f.write(f"note as N{i}\n")
                    f.write(f"  Integration between\n")
                    f.write(f"  {context} and {next_context}\n")
                    f.write("end note\n\n")
                    
            f.write("\n@enduml\n")
            
        print(f"✅ Generated Event Storming Big Picture: {output_file}")
        
    def _generate_event_storming_process_level(self):
        """Generate Event Storming Process Level diagram"""
        output_file = self.output_dir / "event-storming-process-level.puml"
        
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write("@startuml Event Storming Process Level\n")
            f.write("!theme plain\n")
            f.write("skinparam backgroundColor #FFFEF7\n")
            f.write("skinparam defaultFontName Arial\n\n")
            
            f.write("title Event Storming Process Level\\n")
            f.write(f"Generated from BDD Features: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\\n\\n\n\n")
            
            # Focus on main business processes
            main_processes = self._identify_main_processes()
            
            for process_name, events in main_processes.items():
                f.write(f"== {process_name} Process ==\n\n")
                
                prev_event_id = None
                for i, event in enumerate(events):
                    event_id = self._sanitize_id(f"{event.name}_{i}")
                    
                    # Command (Blue)
                    for command in event.commands:
                        command_id = self._sanitize_id(f"{command}_{i}")
                        f.write(f"rectangle \"{command}\" as {command_id} #87CEEB\n")
                        
                    # Event (Orange)
                    f.write(f"rectangle \"{event.name}\" as {event_id} #FF8C00\n")
                    
                    # Aggregates (Yellow)
                    for aggregate in event.aggregates:
                        aggregate_id = self._sanitize_id(f"{aggregate}_{i}")
                        f.write(f"rectangle \"{aggregate}\" as {aggregate_id} #FFFF99\n")
                        f.write(f"{event_id} --> {aggregate_id}\n")
                        
                    # Connect commands to events
                    for command in event.commands:
                        command_id = self._sanitize_id(f"{command}_{i}")
                        f.write(f"{command_id} --> {event_id}\n")
                        
                    # Connect events in sequence
                    if prev_event_id:
                        f.write(f"{prev_event_id} --> {event_id}\n")
                    prev_event_id = event_id
                    
                f.write("\n")
                
            f.write("\n@enduml\n")
            
        print(f"✅ Generated Event Storming Process Level: {output_file}")
        
    def _generate_business_process_flows(self):
        """Generate business process flow diagrams"""
        output_file = self.output_dir / "business-process-flows.puml"
        
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write("@startuml Business Process Flows\n")
            f.write("!theme plain\n")
            f.write("skinparam activityBackgroundColor #E1F5FE\n")
            f.write("skinparam activityBorderColor #0277BD\n\n")
            
            f.write("title Business Process Flows\\n")
            f.write(f"Generated from BDD Features: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\\n\\n\n\n")
            
            # Generate activity diagram for each main journey
            for journey in self.user_journeys[:5]:  # Limit to top 5 journeys
                f.write(f"== {journey.name} ({journey.actor}) ==\n\n")
                f.write("start\n")
                
                for i, step in enumerate(journey.steps):
                    step_text = step.replace('"', '\\"')  # Escape quotes
                    f.write(f":{step_text};\n")
                    
                    # Add decision points for complex flows
                    if "if" in step.lower() or "when" in step.lower():
                        f.write("if (condition met?) then (yes)\n")
                        f.write("  :continue process;\n")
                        f.write("else (no)\n")
                        f.write("  :handle exception;\n")
                        f.write("endif\n")
                        
                # Add outcomes
                for outcome in journey.outcomes:
                    outcome_text = outcome.replace('"', '\\"')
                    f.write(f":{outcome_text};\n")
                    
                f.write("stop\n\n")
                
            f.write("\n@enduml\n")
            
        print(f"✅ Generated Business Process Flows: {output_file}")
        
    def _generate_user_journey_diagrams(self):
        """Generate user journey diagrams"""
        output_file = self.output_dir / "user-journey-overview.puml"
        
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write("@startuml User Journey Overview\n")
            f.write("!theme plain\n")
            f.write("skinparam usecase {\n")
            f.write("  BackgroundColor #E8F5E8\n")
            f.write("  BorderColor #4CAF50\n")
            f.write("}\n\n")
            
            f.write("title User Journey Overview\\n")
            f.write(f"Generated from BDD Features: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\\n\\n\n\n")
            
            # Group journeys by actor
            journeys_by_actor = {}
            for journey in self.user_journeys:
                if journey.actor not in journeys_by_actor:
                    journeys_by_actor[journey.actor] = []
                journeys_by_actor[journey.actor].append(journey)
                
            # Generate use case diagram
            for actor, journeys in journeys_by_actor.items():
                actor_id = self._sanitize_id(actor)
                f.write(f"actor \"{actor}\" as {actor_id}\n")
                
                for journey in journeys:
                    journey_id = self._sanitize_id(journey.name)
                    f.write(f"usecase \"{journey.name}\" as {journey_id}\n")
                    f.write(f"{actor_id} --> {journey_id}\n")
                    
                    # Add touchpoints as system boundaries
                    for touchpoint in journey.touchpoints:
                        touchpoint_id = self._sanitize_id(touchpoint)
                        f.write(f"rectangle \"{touchpoint}\" as {touchpoint_id}\n")
                        f.write(f"{journey_id} --> {touchpoint_id}\n")
                        
                f.write("\n")
                
            f.write("\n@enduml\n")
            
        print(f"✅ Generated User Journey Overview: {output_file}")
        
    def _generate_feature_overview(self):
        """Generate feature overview diagram"""
        output_file = self.output_dir / "bdd-features-overview.puml"
        
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write("@startuml BDD Features Overview\n")
            f.write("!theme plain\n")
            f.write("skinparam packageStyle rectangle\n\n")
            
            f.write("title BDD Features Overview\\n")
            f.write(f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\\n\\n\n\n")
            
            # Group features by bounded context
            features_by_context = {}
            for feature in self.features:
                context = self._extract_context_from_path(Path(feature.file_path))
                if context not in features_by_context:
                    features_by_context[context] = []
                features_by_context[context].append(feature)
                
            for context, features in features_by_context.items():
                f.write(f"package \"{context} Features\" {{\n")
                
                for feature in features:
                    feature_id = self._sanitize_id(feature.name)
                    f.write(f"  rectangle \"{feature.name}\\n({len(feature.scenarios)} scenarios)\" as {feature_id}\n")
                    
                    # Add tags as notes
                    if feature.tags:
                        f.write(f"  note right of {feature_id}\n")
                        f.write(f"    Tags: {', '.join(feature.tags)}\n")
                        f.write("  end note\n")
                        
                f.write("}\n\n")
                
            f.write("\n@enduml\n")
            
        print(f"✅ Generated BDD Features Overview: {output_file}")
        
    def _identify_main_processes(self) -> Dict[str, List[BusinessEvent]]:
        """Identify main business processes from events"""
        processes = {}
        
        # Group events by common patterns
        for event in self.business_events:
            process_name = self._determine_process_name(event)
            if process_name not in processes:
                processes[process_name] = []
            processes[process_name].append(event)
            
        # Sort events within each process
        for process_name in processes:
            processes[process_name] = sorted(
                processes[process_name], 
                key=lambda e: self._get_event_order_priority(e.name)
            )
            
        return processes
        
    def _determine_process_name(self, event: BusinessEvent) -> str:
        """Determine process name from event"""
        event_name = event.name.lower()
        
        if any(word in event_name for word in ['order', 'purchase', 'buy']):
            return "Order Management"
        elif any(word in event_name for word in ['customer', 'user', 'member']):
            return "Customer Management"
        elif any(word in event_name for word in ['product', 'inventory', 'stock']):
            return "Product Management"
        elif any(word in event_name for word in ['payment', 'billing', 'charge']):
            return "Payment Processing"
        elif any(word in event_name for word in ['promotion', 'discount', 'voucher']):
            return "Promotion Management"
        else:
            return "General Process"
            
    def _get_event_order_priority(self, event_name: str) -> int:
        """Get priority order for events in process"""
        event_name = event_name.lower()
        
        # Define typical event order
        order_patterns = [
            ('created', 1),
            ('added', 2),
            ('updated', 3),
            ('submitted', 4),
            ('processed', 5),
            ('completed', 6),
            ('cancelled', 7),
            ('deleted', 8)
        ]
        
        for pattern, priority in order_patterns:
            if pattern in event_name:
                return priority
                
        return 99  # Default priority
        
    def _extract_context_from_event(self, event: BusinessEvent) -> str:
        """Extract bounded context from event"""
        context = event.context.lower()
        
        # Map feature names to bounded contexts
        context_mapping = {
            'customer': 'Customer',
            'order': 'Order',
            'product': 'Product',
            'inventory': 'Inventory',
            'payment': 'Payment',
            'promotion': 'Promotion',
            'notification': 'Notification',
            'delivery': 'Delivery',
            'consumer': 'Customer',
            'logistics': 'Delivery',
            'pricing': 'Pricing',
            'workflow': 'Order'
        }
        
        for key, value in context_mapping.items():
            if key in context:
                return value
                
        return 'Common'
        
    def _sanitize_id(self, text: str) -> str:
        """Sanitize text for use as PlantUML identifier"""
        # Remove special characters and spaces
        sanitized = re.sub(r'[^\w\s]', '', text)
        sanitized = re.sub(r'\s+', '_', sanitized)
        return sanitized

def main():
    """Main function"""
    import sys
    
    if len(sys.argv) != 3:
        print("Usage: python analyze-bdd-features.py <features_dir> <output_dir>")
        sys.exit(1)
        
    features_dir = sys.argv[1]
    output_dir = sys.argv[2]
    
    if not os.path.exists(features_dir):
        print(f"❌ Features directory not found: {features_dir}")
        sys.exit(1)
        
    analyzer = BDDFeatureAnalyzer(features_dir, output_dir)
    analyzer.analyze()
    
    # Print summary
    print(f"\n📊 Analysis Summary:")
    print(f"   • Features analyzed: {len(analyzer.features)}")
    print(f"   • Scenarios found: {sum(len(f.scenarios) for f in analyzer.features)}")
    print(f"   • Business events extracted: {len(analyzer.business_events)}")
    print(f"   • User journeys identified: {len(analyzer.user_journeys)}")
    print(f"   • Bounded contexts: {len(analyzer.bounded_contexts)}")
    print(f"   • Actors identified: {len(analyzer.actors)}")
    print(f"   • Bounded contexts: {', '.join(sorted(analyzer.bounded_contexts))}")
    print(f"   • Actors: {', '.join(sorted(analyzer.actors))}")
    
    # Generate JSON summary
    summary_file = Path(output_dir) / "bdd-analysis-summary.json"
    summary = {
        "timestamp": datetime.now().isoformat(),
        "features_count": len(analyzer.features),
        "scenarios_count": sum(len(f.scenarios) for f in analyzer.features),
        "business_events_count": len(analyzer.business_events),
        "user_journeys_count": len(analyzer.user_journeys),
        "bounded_contexts": list(sorted(analyzer.bounded_contexts)),
        "actors": list(sorted(analyzer.actors)),
        "features": [
            {
                "name": f.name,
                "description": f.description,
                "scenarios_count": len(f.scenarios),
                "file_path": f.file_path,
                "tags": f.tags
            }
            for f in analyzer.features
        ],
        "business_events": [
            {
                "name": e.name,
                "trigger": e.trigger,
                "context": e.context,
                "actors": e.actors,
                "aggregates": e.aggregates,
                "commands": e.commands
            }
            for e in analyzer.business_events
        ],
        "user_journeys": [
            {
                "name": j.name,
                "actor": j.actor,
                "steps_count": len(j.steps),
                "touchpoints": j.touchpoints,
                "outcomes_count": len(j.outcomes)
            }
            for j in analyzer.user_journeys
        ]
    }
    
    with open(summary_file, 'w', encoding='utf-8') as f:
        json.dump(summary, f, indent=2, ensure_ascii=False)
        
    print(f"   • Summary saved to: {summary_file}")

if __name__ == "__main__":
    main()