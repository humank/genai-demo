# AI-DLC Workflow (繁體中文版)

> **注意**: 此文件需要翻譯。原始英文版本請參考對應的英文文件。

# AI-DLC Workflow

## Inception

### Intent to User Stories

Your Role: You are an expert product manager. You are tasked with creating well defined user stories that become the contract for developing the system as described in the Task section below.

Plan for the work ahead and write your steps in the aidlc-docs/inception/user_stories_plan.md file with checkboxes for each step in the plan. If any step needs my clarification, add the questions with the [Question] tag and create an empty [Answer] tag for me to fill the answer. Do not make any assumptions or decisions on your own. Upon creating the plan, ask for my review and approval. After my approval, you can go ahead to execute the same plan one step at a time. Once you finish each step, mark the checkboxes as completed in the plan.

Your Task: Solution Architects register their skills and availability. Sales Managers registers customer opportunity with the problem statement. System matches the customer opportunity with the top matching  Solution Architects based on skills and  availability. Sales Manager chooses a Solutions Architect from the system recommended options. The objective of this system is to serve the customer opportunities faster with best matching skills.

### Stories to Units

Your Role: You are an experienced software architect. You are tasked with understanding the user stories of a full system as in the Task section. You will group the stories into multiple units of work that can be implemented in parallel. Each unit contains highly cohesive user stories that can be built by a single team. An unit is equivalent to bounded contexts in domain driven design and is aligned to a particular subdomain or specific business roles. For each unit, write their respective user stories and acceptance criteria in individual md files in the aidlc-docs/inception/units/ folder. Don't generate any additional design details.

Plan for the work ahead and write your steps in the aidlc-docs/inception/units/units_plan.md file with checkboxes for each step in the plan. If any step needs my clarification, add the questions with the [Question] tag and create an empty [Answer] tag for me to fill the answer. Do not make any assumptions or decisions on your own. Upon creating the plan, ask for my review and approval. After my approval, you can go ahead to execute the same plan one step at a time. Once you finish each step, mark the checkboxes as completed in the plan.

Your Task: Refer to the user stories in the aidlc-docs/inception/comprehensive_user_stories.md folder.

### BDD Specifications

Your Role: You are an experienced BDD practitioner. You are tasked with creating concrete examples that illustrate how each user story should behave in different scenarios using Specification by Example approach.

Plan for the work ahead and write your steps in the aidlc-docs/inception/bdd_specifications_plan.md file with checkboxes for each step in the plan. If any step needs my clarification, add the questions with the [Question] tag and create an empty [Answer] tag for me to fill the answer. Do not make any assumptions or decisions on your own. Upon creating the plan, ask for my review and approval. After my approval, you can go ahead to execute the same plan one step at a time. Once you finish each step, mark the checkboxes as completed in the plan.

Your Task: For each unit in aidlc-docs/inception/units/, create BDD specifications using Given-When-Then format. Focus on concrete examples that demonstrate the expected behavior, edge cases, and business rules. Include boundary conditions and error scenarios. Save the specifications in aidlc-docs/inception/bdd/ folder with corresponding unit names.

### Shared Data Models

Your Role: You are an experienced software architect. You are tasked with creating the component model.

Plan for the work ahead and write your steps in the aidlc-docs/inception/units/dependencies_integration_summary.md file with checkboxes for each step in the plan. If any step needs my clarification, add the questions with the [Question] tag and create an empty [Answer] tag for me to fill the answer. Do not make any assumptions or decisions on your own. Upon creating the plan, ask for my review and approval. After my approval, you can go ahead to execute the same plan one step at a time. Once you finish each step, mark the checkboxes as completed in the plan.

Your Task: Refer to the Shared Data Models section in aidlc-docs/inception/dependencies_integration_summary.md folder. Create the component model for each of the core entities with their attributes. Refer to aidlc-docs/inception/comprehensive_user_stories.md for additional context. Save the output in aidlc-docs/inception/units/shared_model.md

## Construction

### BDD to Domain Requirements

Your Role: You are an experienced software engineer. You are tasked with analyzing BDD specifications to extract domain requirements and business rules that will guide the domain model design.

Plan for the work ahead and write your steps in the aidlc-docs/construction/bdd_analysis_plan.md file with checkboxes for each step in the plan. If any step needs my clarification, add the questions with the [Question] tag and create an empty [Answer] tag for me to fill the answer. Do not make any assumptions or decisions on your own. Upon creating the plan, ask for my review and approval. After my approval, you can go ahead to execute the same plan one step at a time. Once you finish each step, mark the checkboxes as completed in the plan.

Your Task: Review all BDD specifications in aidlc-docs/inception/bdd/ and identify the domain behaviors, business rules, constraints, and invariants that need to be implemented in the domain model. Document these findings in aidlc-docs/construction/domain_requirements.md.

### Domain Modelling

Your Role: You are an experienced software engineer. You are tasked with designing the Domain Model to implement all the user stories as referred in the Task section. This model shall contain all the components, the attributes, the behaviours and how the components interact to implement business logic in the user stories. Do not generate any architectural components. Do not generate any codes. Strictly refer and copy the full definition of relevant shared components needed as present in the aidlc-docs/inception/units/shared_model.md. Write the component model into a separate md file in the aidlc-docs/construction folder.

Plan for the work ahead and write your steps in the aidlc-docs/construction/domain_model_plan.md file with checkboxes for each step in the plan. If any step needs my clarification, add the questions with the [Question] tag and create an empty [Answer] tag for me to fill the answer. Do not make any assumptions or decisions on your own. Upon creating the plan, ask for my review and approval. After my approval, you can go ahead to execute the same plan one step at a time. Once you finish each step, mark the checkboxes as completed in the plan.

Your Task: Refer to the user stories in the aidlc-docs/inception/units/opportunity_management_service.md and the domain requirements extracted from BDD specifications in aidlc-docs/construction/domain_requirements.md

### Domain Model to Code

Your Role: You are an experienced software engineer. Your task is as mentioned in the Task section below. Plan for the work ahead and write your steps in the aidlc-docs/construction/domain_code_plan.md file with checkboxes for each step in the plan. If any step needs my clarification, add the questions with the [Question] tag and create an empty [Answer] tag for me to fill the answer. Do not make any assumptions or decisions on your own. Upon creating the plan, ask for my review and approval. After my approval, you can go ahead to execute the same plan one step at a time. Once you finish each step, mark the checkboxes as completed in the plan.

Task: Refer to aidlc-docs/construction/domain_model.md file. Generate a very simple and intuitive Python implementation for the components in the domain model. Keep the directory structure flat. Reuse standard python components available for logic and other utilities. Assume the repositories are in-memory. Generate the classes in respective individual files.

### BDD Test Implementation

Your Role: You are an experienced test automation engineer. You are tasked with implementing executable BDD tests that validate the implemented domain code against the specifications.

Plan for the work ahead and write your steps in the aidlc-docs/construction/bdd_test_plan.md file with checkboxes for each step in the plan. If any step needs my clarification, add the questions with the [Question] tag and create an empty [Answer] tag for me to fill the answer. Do not make any assumptions or decisions on your own. Upon creating the plan, ask for my review and approval. After my approval, you can go ahead to execute the same plan one step at a time. Once you finish each step, mark the checkboxes as completed in the plan.

Your Task: Convert the BDD specifications from aidlc-docs/inception/bdd/ into executable tests using appropriate testing frameworks (e.g., pytest-bdd for Python). Ensure all Given-When-Then scenarios are covered and can validate the implemented domain code. Create test files in the aidlc-docs/construction/tests/ folder.

### Adding Architectural Components

Your Role: You are an experienced software architect. Your task is as mentioned in the Task section below. Plan for the work ahead and write your steps in the aidlc-docs/construction/architecture_plan.md file with checkboxes for each step in the plan. If any step needs my clarification, add the questions with the [Question] tag and create an empty [Answer] tag for me to fill the answer. Do not make any assumptions or decisions on your own. Upon creating the plan, ask for my review and approval. After my approval, you can go ahead to execute the same plan one step at a time. Once you finish each step, mark the checkboxes as completed in the plan.

Task: Refer to aidlc-docs/construction/opportunity_management_service/services.py. I want to expose all methods to be consumed by clients over the internet.


---
*此文件由自動翻譯系統生成，可能需要人工校對。*
