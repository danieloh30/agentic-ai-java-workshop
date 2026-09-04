# AGENTS.md — Project Context for AI Assistants

> **Purpose:** This file gives AI coding assistants (OpenCode CLI, Claude Code, or any
> AGENTS.md-compatible tool) targeted context about this project, enforcing the `@Agent`
> declarative programming model **upfront** so the assistant never needs to scan the full
> codebase on every request.
>
> Keeping this file accurate is the primary mechanism for **token-efficient, cost-controlled
> AI-assisted development**. Do not run persistent background MCP-adapter scans or rely on
> the assistant to re-discover project patterns via tool-call waterfalls.

---

## Project identity

| Field | Value |
|-------|-------|
| Name | `agentic-ai-java-workshop` |
| Company | Apex Systems (fictional IT services company) |
| Workshop | Enterprise Agentic AI — 90-minute hands-on |
| Runtime | Quarkus / Java 25+ |
| AI extension | `quarkus-langchain4j` (`io.quarkiverse.langchain4j`) |
| LLM | OpenAI `gpt-4o` via `${OPENAI_API_KEY}`; temperature = 0 |
| Build tool | Maven (wrapper `./mvnw`) |
| Main package | `com.incidentmanagement` |

---

## Mandatory programming model: `@Agent` declarative interfaces

**All AI agents in this project are declared as Java interfaces**, not classes.
AI assistants must follow this model exclusively. Do not suggest:
- `AiService`-only patterns without `@Agent`  
- Spring beans, `@RestController`, or JAX-RS endpoints for agent logic  
- Mutable agent state via instance fields  
- Hardcoded prompt strings outside `@SystemMessage` / `@UserMessage`

### Agent anatomy (canonical template)

```java
// 1. Plain Java interface — no extends, no CDI bean annotation
public interface WidgetAgent {

    @SystemMessage("""
        Role / persona / hard rules.
        Exactly one capability described here.
        """)
    @UserMessage("""
        Structured inputs via {templateVariable} placeholders.
        One method parameter per placeholder.
        """)
    @Agent(description = "One sentence: what this agent does.",
           outputKey = "widgetResult")          // required for workflow agents
    @ToolBox(WidgetTool.class)                   // omit if no tools
    String processWidget(String param1, int param2);
}
```

### Tool anatomy

```java
@ApplicationScoped                              // CDI scope — always @ApplicationScoped
public class WidgetTool {

    @Tool("Description the LLM reads to decide whether to call this.")
    @Transactional                              // if touching JPA entities
    public String doSomething(String input) {
        // 1. Look up JPA entity by ID
        // 2. Mutate status
        // 3. Return a structured summary string (not JSON, not void)
    }
}
```

---

## Workflow patterns

| Annotation | Interface location | Use when |
|------------|--------------------|----------|
| `@SequenceAgent` | workflow interface | Step B needs output of A |
| `@ParallelMapperAgent` | workflow interface | Same agent, different task inputs, concurrent |
| `@SupervisorAgent` | agent interface | LLM-driven orchestration over sub-agents |
| `@SupervisorRequest` | static method in supervisor interface | Build the supervisor prompt from scope data |
| `@ConditionalAgent` | workflow interface | Runtime branching by data |
| `@LoopAgent` | workflow interface | Refine until quality threshold |
| `@Output` | static method | Transform `AgenticScope` → typed return value |
| `@A2AClientAgent` | agent interface | Delegate to a remote A2A service |
| `@McpToolBox` | AI service method | Attach remote MCP tools |

---

## Project structure (exercises)

```
solutions/
├── 01-first-agent/                   TriageAgent + TriageTool          [port 8080]
├── 02-maintenance-agent/             @SequenceAgent chain               [port 8080]
├── 03-parallel-workflow/             IncidentSupervisorAgent             [port 8080]
├── 04-supervisor/                    full supervisor stack               [port 8080]
├── 05-ai-governance/                 docs-only placeholder               [no runtime]
├── 06-hitl-observability/            HITL + OTel + LGTM                  [port 8080]
├── 07-a2a/
│   ├── multi-agent-system            A2A client + supervisor             [port 8080]
│   └── remote-a2a-agent              A2A impact assessment service       [port 8888]
└── 08-quarkus-flow/
    ├── (root)                        Programmatic loop solution           [port 8080]
    └── lab/                          Bonus exercise starter (TODO stubs)  [port 8080]
```

Exercise 5 is documentation-only and uses `lab/AGENTS.md`; its solution folder contains only a README.

---

## Existing agents (04-supervisor — the richest exercise)

| Interface | File | Role | outputKey |
|-----------|------|------|-----------|
| `TriageAgent` | `agents/TriageAgent.java` | Requests triage via `TriageTool` | `analysisResult` |
| `DiagnosticAgent` | `agents/DiagnosticAgent.java` | Plans diagnostic actions (no tool, returns plan) | `analysisResult` |
| `EscalationAgent` | `agents/EscalationAgent.java` | Decides ESCALATE_P1 / ASSIGN_TEAM / WORKAROUND / CLOSE | `escalationAction` |
| `ImpactAgent` | `agents/ImpactAgent.java` | Estimates business impact and SLA cost | `businessImpact` |
| `IncidentAnalysisAgent` | `agents/IncidentAnalysisAgent.java` | Classifies incident by task type | `incidentAnalysis` |
| `ResolutionAgent` | `agents/ResolutionAgent.java` | Sets final incident action + resolution | `incidentOutcome` |
| `IncidentSupervisorAgent` | `agents/IncidentSupervisorAgent.java` | `@SupervisorAgent` — orchestrates sub-agents | `supervisorDecision` |

### Workflows

| Interface | Annotation | Sub-agents |
|-----------|------------|------------|
| `IncidentAnalysisWorkflow` | `@ParallelMapperAgent` | `IncidentAnalysisAgent` × 3 tasks |
| `IncidentProcessingWorkflow` | `@SequenceAgent` | `IncidentAnalysisWorkflow` → `IncidentSupervisorAgent` → `ResolutionAgent` |

---

## Domain model (shared across exercises)

| Class | Key fields |
|-------|-----------|
| `IncidentInfo` (Panache entity) | `id` (Long, auto), `system` (column: system_name), `service`, `priority`, `status` (`IncidentStatus`), `description` |
| `IncidentStatus` (enum) | `OPEN`, `TRIAGING`, `IN_PROGRESS`, `ESCALATED`, `RESOLVED` |
| `IncidentAction` (enum) | `ESCALATE`, `INVESTIGATE`, `TRIAGE`, `RESOLVE` |
| `IncidentAnalysisResults` (record) | `severityAnalysis`, `impactAnalysis`, `resolutionAnalysis` |
| `AnalysisTask` (record) | `analysisType` (`AnalysisType`), `systemInstructions` |
| `IncidentOutcome` (record) | `resolution`, `incidentAction` |

---

## Rules AI assistants must follow in this project

1. **Never create a class that implements an agent interface.** Quarkus generates the implementation.
2. **Never add `@ApplicationScoped` / `@Singleton` to an agent interface.** CDI scope is managed by the framework.
3. **`@ToolBox` and `@Tool` only.** Do not reference tools via constructor injection in agent interfaces.
4. **`outputKey` is mandatory on any agent used inside a workflow.** Omitting it breaks `AgenticScope` resolution.
5. **All tools that mutate JPA entities must be `@Transactional`.**
6. **Do not log full incident report strings** at INFO level — log only incident number and status transition.
7. **No secrets in `@SystemMessage` / `@UserMessage`.** Use environment variable references in `application.properties`.
8. **Approval gate first:** The AI assistant must produce a diff/plan and wait for human approval before writing files.
9. **Tests are part of definition of done** — any agent or tool change requires a matching `@QuarkusTest`.
10. **Do not invent third-party APIs** not present in `pom.xml` or `application.properties`.

---

## Token-efficiency notice

This `AGENTS.md` file is the **single authoritative context source** for AI assistants in this project.
When the assistant reads this file first, it does not need to:
- Run `quarkus_searchTools` to re-discover agent patterns
- Execute codebase scans to find class names
- Perform repeated MCP round-trips to resolve domain types

**Estimated savings:** e.g. 2,000–5,000 tokens per complex request in an exercise with a full
supervisor + workflow stack. Front-load context once, spend fewer tokens per interaction.
