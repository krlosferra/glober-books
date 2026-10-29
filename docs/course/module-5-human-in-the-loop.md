Module 5: The Human in Spec-Driven Development

1. The Paradigm Shift: From "Typist" to "Architect"

Concept Explanation
In traditional development, the developer manually translates intent into syntax. In SDD, the "Source
of Truth" shifts from the code to the Specification. The code becomes a downstream
artifact—"Ambient Code"—that can be regenerated, disposed of, or refactored by AI agents based
on the spec.
The human developer transitions from a "writer of code" to a Specification Control Plane operator.
The primary goal is no longer to write the function but to define the constraints, behaviors, and
policies that govern how the AI writes the function.

Human Intervention & Role

• The Conductor: Just as a conductor does not play every instrument but ensures the
orchestra stays in tempo and key, the developer defines the "Score" (the specification) and
intervenes only when the AI (the orchestra) deviates from the intent.

• The Arbiter of Truth: When there is a discrepancy between the spec and the code, the
human must decide if the code is buggy or if the spec is outdated. In SDD, the default action
is to update the spec and regenerate the code, ensuring the spec remains the single
source of truth.

--------------------------------------------------------------------------------

2. The Core Workflow: Human Interventions at Every Phase
The SDD workflow is typically divided into four phases. Human oversight is required at the "handoff"
of each phase to prevent errors from compounding.

Phase 1: Specify (Defining Intent)

• Concept: This phase focuses on the "What" and "Why," encompassing user journeys,
goals, and success metrics without getting bogged down in implementation details.

• Human Intervention:

    ◦ Context Engineering: The human must curate the "context"—providing the AI
with the right background information (legacy constraints, business rules, design
systems) to prevent hallucinations.

    ◦ The "Clarify" Loop: The human does not just issue a command; they invite the
AI to ask clarifying questions (e.g., "How should pagination be handled?"). The
human must answer these to resolve ambiguity before generation begins.

    ◦ Defining the "Constitution": The human establishes immutable rules (e.g.,
"Never commit secrets," "Always use TypeScript") that the AI must follow across all
tasks.

Phase 2: Plan (Architectural Design)

• Concept: The AI proposes a technical approach (tech stack, API boundaries, data
schemas) based on the spec.

• Human Intervention:

    ◦ Feasibility Review: The human reviews the plan for architectural soundness.
Does the proposed database schema match the organization's data governance
policies? Is the security model robust?

    ◦ Constraint Injection: The human injects specific technical constraints (e.g.,
"Use existing Auth0 integration, do not build custom auth").

Phase 3: Tasks (Decomposition)

• Concept: Breaking the plan into atomic, reviewable units of work (e.g., "Create API
Endpoint" vs. "Build Backend").

• Human Intervention:

    ◦ Reviewing Granularity: The human ensures tasks are small enough to be
verified. If a task is too large ("Build the payment system"), the human must force the
AI to break it down further to minimize context drift.

    ◦ Dependency Management: Ensuring the order of operations makes logical
sense (e.g., database migration must happen before API implementation).

Phase 4: Implement & Validate (Execution)

• Concept: The AI generates the code. The human validates that the code meets the spec.

• Human Intervention:

    ◦ Intent Validation: Unlike a traditional code review (checking for syntax errors),
the human checks for Spec Adherence. Did the AI implement the feature exactly as
specified?

    ◦ Drift Detection: If the generated code works but violates the spec (e.g., added
an undocumented API field), the human must reject it or update the spec to reflect
the new reality.

--------------------------------------------------------------------------------

3. Critical Responsibility: Context Engineering & Governance

Context Engineering
This is the new "coding." It involves delivering the right information, in the right format, at the right
time to the LLM.

• Steering Documents: Humans are responsible for creating and maintaining high-level
"Steering Docs" (or "Memory Banks") that describe the project structure, tech stack, and
coding standards. The AI uses these as a reference to ensure consistency.

• Information Retrieval: The human must ensure the AI has access to necessary
documentation (e.g., via RAG or "Reference" files). If the AI hallucinates a library that doesn't
exist, it is a failure of Context Engineering.
Safety & Security Governance
The AI cannot make moral or legal judgments. This remains a strictly human domain.

• Guardrails: Humans must define and enforce "Three-Tier Boundaries":
    1. Always Do: (e.g., "Write tests").
    2. Ask First: (e.g., "Modify database schema").
    3. Never Do: (e.g., "Hardcode API keys").

• Compliance: For regulated industries (e.g., GDPR, HIPAA), the human must audit the
spec and the generated code to ensure no sensitive data handling violations occur. The AI
may technically solve the problem but legally violate a constraint.

--------------------------------------------------------------------------------

4. Drift Management: The "Self-Healing" Loop

Concept Explanation
"Drift" occurs when the implementation diverges from the specification. In traditional development,
this leads to outdated documentation. In SDD, this is a system failure.

Human Intervention

• The Feedback Loop: When "Drift" is detected (e.g., a test fails because the code output
doesn't match the spec), the human must perform Root Cause Analysis:

    ◦ Scenario A (Bad Code): The AI failed to follow instructions. Action: Refine the
prompt/context and regenerate.

    ◦ Scenario B (Bad Spec): The spec was ambiguous or incorrect. Action: Update
the Spec, then regenerate the code.

• Preventing "Vibe Coding": The human must resist the urge to manually patch the code to
"just make it work." Doing so breaks the link between Spec and Code. The discipline is to fix
the Spec so the AI can generate the correct code.

--------------------------------------------------------------------------------

5. Required Skills for the Human-in-the-Loop

To effectively operate the Specification Control Plane and manage AI agents, developers must
cultivate a "T-shaped" skill set: deep knowledge of system architecture and broad competence in AI
orchestration and compliance.

A. Core Technical Competencies

1. Context Engineering

This is the ability to curate and structure the information provided to the AI to ensure
accurate execution. It goes beyond simple prompt engineering.

• Definition: Delivering the right information, in the right format, at the right time to
the LLM.

• Skill Application: You must be able to assemble dynamic, structured contexts that
include long-term memory (docs), short-term state (current task), and available tools,
optimizing system-level performance rather than just a single prompt.

• Why it matters: Without precise context engineering, AI agents suffer from "context
drift," fixing one bug while breaking three other files they didn't "see".

2. Specification & Schema Literacy

Developers must be fluent in the languages of intent rather than just languages of
implementation.

• API Design Principles: Proficiency in REST, GraphQL, and async messaging
patterns is required to define boundaries clearly.

• Format Expertise: Mastery of specification formats like OpenAPI, AsyncAPI, and
JSON Schema is essential, as these become the primary artifacts managed by the
human.

• Declarative Thinking: The ability to express intent declaratively (what the system
should do) rather than procedurally (how it should do it).

3. Architectural & "Invariant" Thinking

Because AI handles the boilerplate, humans must focus on the immutable rules of the
system.

• Thinking in Invariants: Instead of reasoning about individual features, humans
must reason about invariants—conditions that must always be true regardless of the
implementation details.

• Deep System Design: Focus shifts to deep architecture, security boundary setting,
and defining complex data models, tasks that AI currently struggles to perform
autonomously.

• Compatibility Reasoning: Humans must possess the foresight to reason about
backward and forward compatibility to prevent breaking changes during AI-driven
refactoring.

B. Validation & Governance Skills

1. Intent Validation & Semantic Auditing

The human must act as a high-level auditor, verifying that the generated output aligns with
business goals and domain logic.

• Semantic Review: Unlike code review (checking for syntax), this skill involves
checking if the generated artifacts capture the actual user need and business intent.

• Domain Expertise: Humans must handle business domain complexity, identifying
logic errors that are syntactically correct but functionally wrong (e.g., a discount
applied before tax instead of after).

• Security Awareness: Reviewers need acute security awareness to spot
vulnerabilities in AI-generated code, particularly regarding authentication and
authorization patterns.

2. Automated Quality Gate Configuration

The human must build the "safety net" that allows the AI to operate.

• CI/CD Configuration: Skills in configuring pipelines that trigger specification
validation, automated testing, and drift detection are critical.

• Observability & Monitoring: Setting up monitoring for production systems to
detect when the AI-generated implementation begins to drift from the specification.

C. Cognitive & Collaborative Skills

1. AI Orchestration & Role Management

The developer acts as a manager for a team of AI agents.

• Decomposition: The ability to break complex ambiguity into atomic, reviewable
steps (Specification → Plan → Task) so the AI does not get lost,.

• Role Assignment: Skill in assigning specific "personas" to agents (e.g., assigning
a "QA Agent" to review the work of a "Coding Agent") to create adversarial validity
checks.

2. Stakeholder Alignment & Emotional Intelligence

AI lacks the social context required to negotiate requirements.

• Ambiguity Resolution: Humans must possess the emotional intelligence to
navigate stakeholder disagreements and clarify ambiguous requirements before they
reach the specification stage.

• Risk & Ethical Judgment: Humans act as the custodians of risk tolerance, safety
envelopes, and ethical frameworks, making judgment calls on acceptable trade-offs
that machines cannot calculate.

Summary of Skill Shift

Traditional Developer Skill

SDD / Human-in-the-Loop Skill

Writing Boilerplate Code

Context Engineering & Prompting

Debugging Syntax

Validating Intent & Semantics

Manual Refactoring

Defining Architectural Invariants

Implementing Features

Decomposing Specifications

Code Review

Auditing Security & Compliance

--------------------------------------------------------------------------------

6. Summary Checklist for the Human-in-the-Loop
To effectively operate in an SDD environment, your team members must adopt these specific
behaviors:

1. Write clear, unambiguous specifications focusing on user behavior and outcomes, not
just technical tasks.

2. Verify the Plan before the Code: Spend more time reviewing the architectural plan and
task list than debugging the syntax.

3. Treat the Spec as the Source of Truth: Never allow the code to become the "real"
version of the system while the spec rots. Update the spec first.

4. Manage the Context: Curate the information fed to the AI (Constitution, Steering Docs) to
ensure it acts as a specialized employee, not a generic chatbot.

5. Audit for Safety: Maintain absolute authority over security credentials, PII (Personally
Identifiable Information), and compliance boundaries.

