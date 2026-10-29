Module 3: The Project Constitution in Spec-Driven Development
(SDD)

1. Definition and Core Philosophy

●  Concept Explanation

In Spec-Driven Development, an AI agent acts like a highly capable but
literal-minded pair programmer. To prevent the AI from making incorrect assumptions
about your project's architecture, security, or conventions, it requires an
unambiguous set of ground rules.
The Project Constitution is a foundational "memory bank" or steering document
that establishes the high-level, immutable principles that must be applied to every
single change made in the codebase. While functional specifications define what a
specific feature should do, the Constitution defines the universal rules of the game
for how the AI is allowed to operate within your repository.

●  Why it is Essential

Without a Constitution, critical requirements—such as security policies, compliance
rules, and design system constraints—often exist only in human developers' heads
or scattered across inaccessible wikis. By codifying these into a version-controlled
document (such as a constitution.md file used in tools like GitHub Spec Kit), the
AI agent automatically bakes these requirements into every technical plan and
generated code artifact from day one.

--------------------------------------------------------------------------------

2. The "Three-Tier Boundary" System
To effectively govern AI agents, the Constitution should structure operational boundaries into three
distinct tiers. This nuance ensures the AI acts safely without requiring human intervention for every
minor decision.

Tier 1: Always Do (Mandates)
These are the routine practices and standards the AI must execute autonomously for every
task.

●  Examples: "Always run tests before commits," "Always follow specific naming
conventions," or "Always include explanatory docstrings for public interfaces".

Tier 2: Ask First (Human Approval Gates)
These constraints catch high-impact architectural or structural changes that might be
technically valid but require explicit human authorization before the AI proceeds.

●  Examples: "Ask first before modifying database schemas," "Ask first before adding

new third-party dependencies," or "Ask first before altering CI/CD pipeline
configurations".

Tier 3: Never Do (Hard Stops)
These are categorical prohibitions. If the AI encounters a scenario requiring these actions, it
must halt execution entirely.

●  Examples: "Never commit secrets or API keys" (noted as the most commonly helpful

constraint in AI coding studies), "Never edit the node_modules/ or vendor/
directories," or "Never remove a failing test without explicit approval".

--------------------------------------------------------------------------------

3. Key Components of a Complete Constitution
A well-crafted Constitution acts as a professional blueprint. According to analyses of effective agent
configurations, a robust Constitution should explicitly define the following core areas:

1.  Tech Stack and Tooling: Explicitly define the frameworks, versions, and dependencies

(e.g., "Use React 18+ with TypeScript, Vite, and Tailwind CSS").

2.  Project Structure: Give the AI a map of where things belong to avoid disorganized code
sprawl. Specify exact directories (e.g., "src/ for application code, tests/ for unit tests,
docs/ for documentation").

3.  Executable Commands: Provide the exact CLI commands with flags that the agent will
need to use frequently (e.g., npm run build, pytest -v, npm run lint --fix).
4.  Testing Strategy: Detail the testing framework used, where test files live, and coverage

expectations.

5.  Code Style and Git Workflow: Provide a brief, real code snippet demonstrating your

preferred style, and outline rules for branch naming, commit message formatting, and Pull
Request requirements.

--------------------------------------------------------------------------------

4. The Constitution in the SDD Workflow
The Constitution sits at the very beginning of the SDD lifecycle. In frameworks like GitHub Spec Kit,
the formal workflow is defined as: Constitution → Specify → Plan → Tasks → Implement.

●  The Initialization: When a project begins, a file like .specify/constitution.md is

●

initialized in the repository.
Influencing the Plan: During the Plan phase, when the AI creates the technical architecture
for a new feature, it cross-references the functional specification with the Constitution. If the
feature spec requires storing user data, the Constitution's mandate to "Never commit
secrets" and "Use specific database ORMs" will dictate how the AI plans that data storage.
●  Checklists and Validation: The workflow heavily utilizes AI-interpreted checklists to track
whether the principles in the Constitution have been violated during the generation of tasks
or code.

--------------------------------------------------------------------------------

5. Human Responsibility and Governance
While the AI generates the code, the human developer assumes the role of an "architectural
guardian" and quality controller.

●  Authoring the Rules: The human team is strictly responsible for debating, defining, and
writing the Constitution. Machines cannot own legal, ethical, or high-level business risk
boundaries.

●  Reviewing Against the Constitution: When the AI presents its generated code, the

human's primary job during Code Review is to validate adherence. Did the AI respect the
boundary to not add new dependencies? Did it follow the exact project structure mandated?

●  Handling "Over-Eagerness": Humans must be vigilant during reviews, as AI agents can

sometimes ignore instructions or go overboard by too eagerly following a specific
constitutional article. If the AI consistently disobeys a rule, the human must refine the
wording in the Constitution to remove ambiguity.

--------------------------------------------------------------------------------

Summary Checklist for the Team
When adopting the Project Constitution approach, remember these key takeaways:

1.  Treat the Constitution as Code: It must be version-controlled, collaboratively edited, and

treated as a living document that evolves as organizational standards change.

2.  Be Specific, Not Vague: Vague prompts yield wrong results. Say "Use PostgreSQL and

Prisma ORM" rather than just "Use a database".

3.  Enforce the Boundaries: Use the Always Do / Ask First / Never Do framework to give the

AI safe operational freedom while protecting critical infrastructure

