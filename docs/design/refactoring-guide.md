# Refactoring Guide: Based on "Refactoring: Improving the Design of Existing Code"

This document is compiled from Martin Fowler's classic work "Refactoring: Improving the Design of Existing Code", 
providing an overview of code smells, refactoring techniques, and related design principles to help developers 
improve code quality.

## Table of Contents

- [Code Smells](#code-smells)
- [Refactoring Techniques](#refactoring-techniques)
- [Design Principles](#design-principles)
- [Refactoring Process and Best Practices](#refactoring-process-and-best-practices)

## Code Smells

Code smells are signs of potential problems in code that usually indicate the need for refactoring.

### Major Code Direction Issues

- **Duplicated Code**
  - Same or similar code segments appear in multiple places
  - Violates DRY (Don't Repeat Yourself) principle
  - Solutions: Extract Method, Pull Up Method, Form Template Method

- **Long Method**
  - Methods that are too lengthy, difficult to understand and maintain
  - Solutions: Extract Method, Replace Temp with Query, Introduce Parameter Object

- **Large Class**
  - A class takes on too many responsibilities, having too many fields and methods
  - Solutions: Extract Class, Extract Subclass, Extract Interface

- **Long Parameter List**
  - Too many function parameters, making calls and understanding difficult
  - Solutions: Introduce Parameter Object, Preserve Whole Object, Replace Parameter with Method

- **Divergent Change**
  - Whenever a certain change occurs, multiple parts of the same class need to be modified
  - Solutions: Extract Class

- **Shotgun Surgery**
  - One change causes the need to modify multiple different classes
  - Solutions: Move Method, Move Field, Inline Class

- **Feature Envy**
  - A method is more interested in other classes than its own class
  - Solutions: Move Method, Extract Method

- **Data Clumps**
  - The same combination of data items appears in multiple places
  - Solutions: Extract Class, Introduce Parameter Object, Preserve Whole Object

- **Primitive Obsession**
  - Overuse of primitive types instead of small objects to represent simple concepts
  - Solutions: Replace Data Value with Object, Replace Type Code with Class, Replace Type Code with Subclasses

- **Switch Statements**
  - The same switch statement scattered in multiple places
  - Solutions: Replace Conditional with Polymorphism, Replace Type Code with Subclasses, Replace Type Code with State/Strategy

- **Parallel Inheritance Hierarchies**
  - Whenever adding a subclass to one inheritance hierarchy, you must also add a corresponding subclass to 
    another inheritance hierarchy
  - Solutions: Move Method, Move Field

- **Lazy Class**
  - A class whose usefulness is insufficient to justify its existence
  - Solutions: Inline Class, Collapse Hierarchy

- **Speculative Generality**
  - Abstractions and flexibility designed prematurely for future needs that may arise
  - Solutions: Collapse Hierarchy, Inline Class, Remove Parameter, Rename Method

### Specific Code Issues

- **Temporary Field**
  - A field that is meaningful only in specific situations
  - Solutions: Extract Class, Introduce Special Case

- **Message Chains**
  - Client requests an object, then requests another object, forming a chain of associations
  - Solutions: Hide Delegate, Extract Method, Move Method

- **Middle Man**
  - A class whose main function is to delegate to other classes
  - Solutions: Remove Middle Man, Inline Method, Replace Delegation with Inheritance

- **Inappropriate Intimacy**
  - One class knows too much about the internal details of another class
  - Solutions: Move Method, Move Field, Change Bidirectional Association to Unidirectional, Extract Class, Hide Delegate

- **Alternative Classes with Different Interfaces**
  - Different classes have methods that perform similar functions but have different signatures
  - Solutions: Rename Method, Move Method, Extract Superclass

- **Incomplete Library Class**
  - Library class doesn't provide needed functionality
  - Solutions: Introduce Foreign Method, Introduce Local Extension

- **Data Class**
  - Classes that contain only fields and access methods, no behavior
  - Solutions: Move Method, Encapsulate Field, Encapsulate Collection

- **Refused Bequest**
  - Subclasses don't need or want to inherit some methods or properties from parent classes
  - Solutions: Replace Inheritance with Delegation, Extract Superclass, Extract Interface

- **Comments**
  - Too many comments may indicate insufficient code design clarity
  - Solutions: Extract Method, Rename Method, Introduce Assertion

## Refactoring Techniques

### Composing Methods

- **Extract Method**
  - Extract code fragments into reusable functions
  - Improve readability and avoid duplication

- **Inline Method**
  - Replace function calls with the actual content of the function
  - Use when function body is clearer than function name

- **Extract Variable**
  - Store expression results in variables
  - Make complex expressions easier to understand

- **Inline Temp**
  - Replace temporary variables with their values
  - Simplify unnecessary indirection

- **Replace Temp with Query**
  - Extract expressions into separate methods
  - Make code more readable and reusable

- **Split Temporary Variable**
  - Create independent variables for each assignment
  - Avoid variables taking on multiple responsibilities

- **Remove Assignments to Parameters**
  - Use local variables instead of modifying parameters
  - Avoid side effects and confusion

- **Replace Method with Method Object**
  - Transform complex methods into a new class
  - Facilitate decomposition of long methods

- **Substitute Algorithm**
  - Replace existing implementation with clearer algorithms
  - Improve readability and performance

### Moving Features

- **Move Method**
  - Move methods to more appropriate classes
  - Improve cohesion and reduce coupling

- **Move Field**
  - Move fields to more appropriate classes
  - Improve data and behavior configuration

- **Extract Class**
  - Separate responsibilities from one class
  - Create two more cohesive classes

- **Inline Class**
  - Merge class content into another class
  - When a class has lost its reason for existence

- **Hide Delegate**
  - Create methods in delegate class
  - Reduce client knowledge of delegation relationships

- **Remove Middle Man**
  - Interact directly with the real object
  - When delegation relationships are overly simple

- **Introduce Foreign Method**
  - Add function in client class and pass service class instance
  - When service class cannot be modified

- **Introduce Local Extension**
  - Create subclass or wrapper class of service class
  - Add needed functionality

### Organizing Data

- **Self Encapsulate Field**
  - Access fields through accessor methods rather than directly
  - Increase subclass flexibility

- **Replace Data Value with Object**
  - Transform simple data into objects
  - Add behavior or more structure

- **Change Value to Reference**
  - Transform multiple copies of the same object into references
  - Ensure consistency

- **Change Reference to Value**
  - Transform reference objects into immutable value objects
  - Simplify code and avoid side effects

- **Replace Array with Object**
  - Replace arrays of different element types with objects
  - Improve code clarity

- **Duplicate Observed Data**
  - Separate domain data from UI objects
  - Improve code structure and testability

- **Change Unidirectional Association to Bidirectional**
  - Add reverse references
  - Facilitate bidirectional navigation

- **Change Bidirectional Association to Unidirectional**
  - Remove unnecessary association directions
  - Reduce coupling

- **Replace Magic Number with Symbolic Constant**
  - Replace hard-coded values with named constants
  - Improve readability and maintainability

- **Encapsulate Field**
  - Make public fields private and provide accessors
  - Control access and add behavior

- **Encapsulate Collection**
  - Provide add/remove methods instead of directly returning collections
  - Control collection modifications

- **Replace Type Code with Class**
  - Replace enums or constants with classes
  - Increase type safety and extensibility

- **Replace Type Code with Subclasses**
  - Create subclasses for each type
  - Utilize polymorphism

- **Replace Type Code with State/Strategy**
  - Move type-related behavior to state or strategy classes
  - More flexible behavior changes

- **Replace Subclass with Fields**
  - Move subclass-specific behavior to parent class fields
  - Simplify unnecessary inheritance

### Simplifying Conditional Expressions

- **Decompose Conditional**
  - Extract complex conditions into named methods
  - Make conditions easier to understand

- **Consolidate Conditional Expression**
  - Combine multiple checks with the same result
  - Highlight the intent of the check

- **Consolidate Duplicate Conditional Fragments**
  - Move duplicate code from conditional branches outside the condition
  - Reduce duplication

- **Remove Control Flag**
  - Use break or return instead of control flags
  - Simplify loop or conditional structures

- **Replace Nested Conditional with Guard Clauses**
  - Use early returns to handle special cases
  - Reduce nesting and highlight main logic

- **Replace Conditional with Polymorphism**
  - Move conditional logic to class hierarchies
  - Use polymorphism for dynamic behavior

- **Introduce Null Object**
  - Replace null checks with special objects
  - Avoid proliferation of null checks

- **Introduce Assertion**
  - Add assertions to state assumptions
  - Clearly express code assumptions

### Simplifying Method Calls

- **Rename Method**
  - Make method names clearly express their intent
  - Improve self-documentation

- **Add Parameter**
  - Add parameters to method signatures
  - Provide more information

- **Remove Parameter**
  - Delete unused parameters
  - Simplify method signatures

- **Separate Query from Modifier**
  - Separate read and modify operations
  - Improve safety

- **Parameterize Method**
  - Combine multiple similar methods into a single parameterized method
  - Reduce duplicate code

- **Introduce Parameter Object**
  - Organize multiple parameters into an object
  - Simplify parameter lists

- **Preserve Whole Object**
  - Pass entire objects instead of multiple individual properties
  - Simplify parameter passing

- **Replace Parameter with Explicit Methods**
  - Create independent methods for different behaviors
  - Simplify client code

- **Introduce Named Parameter**
  - Make parameter meanings more explicit
  - Improve readability

- **Hide Method**
  - Make methods that don't need external calls private
  - Reduce public interface

- **Replace Constructor with Factory Method**
  - Use factory methods to create objects
  - Provide more flexibility

- **Encapsulate Downcast**
  - Move casting inside methods
  - Have methods return correct types

- **Replace Error Code with Exception**
  - Use exceptions instead of returning error codes
  - Handle error situations more explicitly

- **Replace Exception with Test**
  - Replace exceptions with conditional tests when possible
  - Avoid performance overhead of exception handling

### Dealing with Generalization

- **Pull Up Field**
  - Move same fields to parent class
  - Eliminate duplication

- **Pull Up Method**
  - Move same methods to parent class
  - Eliminate duplication

- **Pull Up Constructor Body**
  - Move common parts of subclass constructors to parent class
  - Reuse initialization code

- **Push Down Method**
  - Move methods to specific subclasses
  - When methods are only relevant to certain subclasses

- **Push Down Field**
  - Move fields to specific subclasses
  - When fields are only relevant to certain subclasses

- **Extract Subclass**
  - Create subclasses for special behavior
  - Separate special behavior from main class

- **Extract Superclass**
  - Extract common features from classes to create parent class
  - Reduce duplication

- **Extract Interface**
  - Extract common method signatures as interfaces
  - Clarify class capabilities

- **Collapse Hierarchy**
  - Merge subclasses and parent classes
  - When they don't differ much

- **Form Template Method**
  - Define algorithm skeleton in parent class, implement specific steps in subclasses
  - Reuse algorithm structure

- **Replace Inheritance with Delegation**
  - Use composition instead of inheritance relationships
  - Avoid problems caused by inappropriate inheritance

- **Replace Delegation with Inheritance**
  - Use inheritance when a class completely delegates to another class
  - Simplify code

## Design Principles

### SOLID Principles

- **Single Responsibility Principle (SRP)**
  - A class should have only one reason to change
  - Each class should be responsible for only one responsibility

- **Open-Closed Principle (OCP)**
  - Open for extension, closed for modification
  - New functionality should be implemented through extension, not by modifying existing code

- **Liskov Substitution Principle (LSP)**
  - Subclasses should be able to replace their parent classes
  - Ensure correct use of inheritance

- **Interface Segregation Principle (ISP)**
  - Clients should not be forced to depend on methods they don't use
  - Interfaces should be small and focused

- **Dependency Inversion Principle (DIP)**
  - High-level modules should not depend on low-level modules; both should depend on abstractions
  - Abstractions should not depend on details; details should depend on abstractions

### Other Important Principles

- **Law of Demeter/Principle of Least Knowledge**
  - An object should know as little as possible about other objects
  - Reduce coupling between objects

- **Composition Over Inheritance**
  - Prefer object composition over inheritance
  - Improve flexibility and reduce coupling

- **High Cohesion, Low Coupling**
  - Related functionality should be concentrated in the same module
  - Dependencies between different modules should be minimized

- **DRY Principle (Don't Repeat Yourself)**
  - Avoid duplicate code and knowledge
  - Each piece of knowledge should have a clear representation in the system

- **YAGNI Principle (You Aren't Gonna Need It)**
  - Don't develop functionality for future possible needs
  - Focus on current definite requirements

- **Separation of Concerns**
  - Different functions and responsibilities should be separated
  - A component should only focus on its main responsibility

## Refactoring Process and Best Practices

### Refactoring Process

- **Ensure Test Coverage**
  - Ensure adequate testing before refactoring
  - Run tests after each small refactoring step

- **Small Steps Forward**
  - Make small and safe changes
  - Test frequently to ensure no errors are introduced

- **Solve One Problem at a Time**
  - Avoid multiple refactorings simultaneously
  - Keep changes manageable

- **Separate Refactoring from New Feature Development**
  - Don't refactor while adding new features
  - Clearly distinguish between these two activities

### When to Refactor

- **When Adding Features**
  - Make code easier to understand and modify
  - Prepare for new functionality

- **When Fixing Bugs**
  - Improve code structure to make problems more obvious
  - Prevent similar future problems

- **During Code Reviews**
  - Make improvements based on team feedback
  - Unify code style and structure

- **Regular Maintenance Activities**
  - Dedicated refactoring sessions
  - Address technical debt

### Implementation Considerations

- **Team Collaboration**
  - Discuss important refactorings with team members
  - Share refactoring strategies and learning

- **Documentation and Communication**
  - Document reasons and methods for major refactorings
  - Inform relevant team members

- **Version Control**
  - Commit small changes frequently
  - Use meaningful commit messages

- **Monitor Performance and Resource Usage**
  - Ensure refactoring doesn't introduce performance issues
  - Test performance under different scenarios

## Summary

Refactoring is an important practice in software development that helps us continuously improve code quality, 
making systems easier to understand, maintain, and adapt to changes. By identifying code smells, applying 
appropriate refactoring techniques, and following good design principles, we can create robust, flexible, 
and sustainable software systems.

Refactoring is not a one-time activity but a continuous practice in the development process. Reasonable 
refactoring can reduce technical debt, improve team efficiency, and lay the foundation for rapid implementation 
of business requirements.