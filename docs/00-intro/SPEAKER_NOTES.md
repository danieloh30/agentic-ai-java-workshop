# Lab Overview

---

## The scenario: Apex Systems

**Apex Systems** is a mid-size enterprise IT services company managing infrastructure across data centers and cloud regions. The NOC (Network Operations Center) receives free-text incident reports from monitoring tools, tickets, and on-call engineers — but today those reports live in chat threads, email chains, and tribal knowledge.

Last quarter's post-mortem told the story:

- Two P1 outages (auth failure, API gateway) lasted **4+ hours because manual triage classified them as P3** — $150k combined revenue loss
- A payment-gateway incident was **routed to the networking team instead of payments**, burning 20 engineer-hours before correction
- NOC analysts cannot explain *why* an incident was routed or whether escalation was appropriate — no audit trail exists

Leadership mandate: **automate incident triage and routing with AI agents — without losing enterprise control.**

---

## Why agentic AI (not "just a chatbot")

| Chatbot | Agent |
|---------|-------|
| Answers questions | Takes actions |
| One LLM call | Multi-step reasoning + tool calls |
| No side effects | Mutates state (e.g., `IncidentStatus → TRIAGING`) |
| Stateless | Shares context via `AgenticScope` across workflow |
| Single model | Composed specialists (triage, diagnostic, impact) |

Apex Systems needs systems that **reason** over messy natural-language incident reports, **act** by calling enterprise tools, **collaborate** across specialized roles, **pause** for humans on high-stakes outcomes, and **scale** across teams via open protocols.

---

## What you will build

A production-shaped **agentic incident management platform** on Quarkus — from a single agent to a full supervisor orchestration with human oversight and distributed services.

```mermaid
%%{init: {'look':'handDrawn','theme':'neutral','themeVariables': {'lineColor':'#4A4035'}}}%%
flowchart TD
    IR([Incident Report])
    RIA(["':8888 — Remote Impact Assessment'"])

    subgraph main["Apex Systems Incident Management · Quarkus :8080"]
        IPW(["IncidentProcessingWorkflow<br/>@SequenceAgent"])
        IAW(["IncidentAnalysisWorkflow<br/>@ParallelMapperAgent x3"])
        ISA(["IncidentSupervisorAgent<br/>@SupervisorAgent"])
        RA([ResolutionAgent])
        TA(["TriageAgent<br/>@ToolBox"])
        DA([DiagnosticAgent])
        EA(["EscalationAgent<br/>+ HITL gate"])
        IA([ImpactAgent])
    end

    IR --> IPW
    IPW --> IAW
    IPW --> ISA
    IPW --> RA

    ISA --> TA
    ISA --> DA
    ISA --> EA
    ISA --> IA

    IA -->|A2A| RIA

    style IR fill:#E8DCC4,stroke:#6B5B45
    style IPW fill:#D4E6F1,stroke:#2E6B8A
    style IAW fill:#D4E6F1,stroke:#2E6B8A
    style ISA fill:#FFE4CC,stroke:#B87333
    style RA fill:#D8F0D8,stroke:#3D7A3D
    style TA fill:#FFF8DC,stroke:#C4A000
    style DA fill:#D8F0D8,stroke:#3D7A3D
    style EA fill:#D8F0D8,stroke:#3D7A3D
    style IA fill:#FFF8DC,stroke:#C4A000
    style RIA fill:#F5D0D0,stroke:#A04040
    style main fill:#F5F5F0,stroke:#8B8070
```

---

## Your learning path

Each exercise adds a new capability, guided by a persona facing a real IT operations problem:

| Exercise | Persona | Problem | Pattern you learn |
|----------|---------|---------|-------------------|
| **1 — Agent + tool** | **Sam** — NOC analyst | Free-text reports pile up; triage is manual | `@Agent` + `@ToolBox` |
| **2 — Policy as prompt** | **Chris** — Ops lead | Diagnostic decisions need policy, not code | `@SystemMessage` as policy declaration |
| **3 — Parallel agents** | **Chris** — Ops lead | Three analyses must run concurrently | `@ParallelMapperAgent` + `@Output` |
| **4 — Supervisor orchestration** | **Priya** — IT service mgr | Critical incidents need adaptive escalation | `@SupervisorAgent` orchestration |
| **5 — AI governance** | **Jordan** — Platform engineer | Must ship governed code; copilots hallucinate | `AGENTS.md` + OpenCode CLI |
| **6 — Human gate + tracing** | **Alex** — Compliance officer | P1 incidents need approval + audit trail | HITL + OpenTelemetry |
| **7 — Remote agents (A2A)** | **Riley** — SRE lead | Impact assessment is a separate team | A2A remote impact agent |
| **8 — Quality loop (bonus)** | **Jordan** — Platform engineer | Post-incident reports need iterative refinement | `AgenticServices.loopBuilder()` + Quarkus Flow |

**Exercises 1–4** are hands-on code-along — you type agent code into stub files with hot reload.  
**Exercise 5** uses OpenCode CLI to govern and validate the system you built.  
**Exercise 6** codes `EscalationProposalAgent` in `lab/`, then tests the full HITL flow and OTel tracing from the solution.  
**Exercise 7** runs a pre-built A2A solution to explore remote agent patterns.  
**Exercise 8 (bonus)** is a self-paced code-along in a standalone project — builds a programmatic quality loop with Quarkus Flow.

---

## The technology stack

| Layer | Component | Role |
|-------|-----------|------|
| Runtime | Quarkus | Build-time agent validation, fast startup |
| AI extension | Quarkus LangChain4j | Declarative agents, workflows, A2A |
| Dev tooling | OpenCode CLI | SDLC partner: plan → code → test → secure |
| Context efficiency | `AGENTS.md` | Targeted AI assistant context — avoids token-bloat scans |

---

## Learning outcomes

After this lab you will be able to:

- Declare agents with `@Agent`, `@SystemMessage`, `@ToolBox` and explain what Quarkus generates at build time
- Compose multi-agent workflows: sequence, parallel, and supervisor orchestration
- Use `@SupervisorAgent` for adaptive AI routing vs hardcoded conditional logic
- Author an `AGENTS.md` to make OpenCode CLI cost-efficient on agentic projects
- Distribute agents across services with **A2A** (`@A2AClientAgent`)
- Add **human-in-the-loop** gates and read **OpenTelemetry** spans for compliance and FinOps
- Use **programmatic orchestration** (`AgenticServices.loopBuilder()`) for control flow that annotations can't express

---

## The seeded incidents

When you start the app in Exercise 1, you'll see 8 incidents in the Incident Dashboard:

| Incident # | System | Service | Priority | Initial Status |
|------------|--------|---------|----------|----------------|
| 1 | payment-gateway | checkout-api | P2 | OPEN |
| 2 | auth-service | user-login | P1 | IN_PROGRESS |
| 3 | inventory-db | stock-sync | P3 | OPEN |
| 4 | cdn-edge | static-assets | P4 | TRIAGING |
| 5 | email-service | notification-api | P2 | OPEN |
| 6 | search-engine | product-search | P3 | OPEN |
| 7 | monitoring | alerting-api | P2 | OPEN |
| 8 | api-gateway | rate-limiter | P1 | IN_PROGRESS |

You'll use these incidents throughout the exercises — processing them with different reports to trigger different agent behaviors.

---

## Patterns cheat sheet

Keep this handy as you work through the exercises:

```
Goal: single specialist agent    → @Agent + @ToolBox               (Ex 1)
Goal: policy as prompt           → @SystemMessage                  (Ex 2)
Goal: step-by-step pipeline      → @SequenceAgent                  (Ex 2–4)
Goal: concurrent work            → @ParallelMapperAgent            (Ex 3)
Goal: data-driven branching      → @ConditionalAgent               (Ex 2)
Goal: adaptive multi-agent       → @SupervisorAgent                (Ex 4)
Goal: AI governance              → AGENTS.md + OpenCode CLI             (Ex 5)
Goal: human approval             → @HumanInTheLoop                 (Ex 6)
Goal: delegate to remote agent   → A2A + @A2AClientAgent           (Ex 7)
Goal: programmatic quality loop  → AgenticServices.loopBuilder()   (Ex 8)
```
