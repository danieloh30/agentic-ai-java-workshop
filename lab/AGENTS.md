# AGENTS.md — Apex Systems Incident Management

> **Exercise 5 task:** You will validate this file with OpenCode CLI before writing any Java code.
> Your AI assistant reads this file on every request — it replaces codebase scanning and saves
> tokens per complex task. Keep it accurate as you add agents through the exercises.
>
> See the root [`AGENTS.md`](../AGENTS.md) for the complete reference. This file is your
> project-local working copy; update it after each exercise as you add new agents.

---

## Project identity

| Field | Value |
|-------|-------|
| Name | `agentic-ai-java-workshop` |
| Story | Apex Systems incident management (Workshop) |
| Runtime | Quarkus / Java 25+ |
| AI extension | `quarkus-langchain4j` |
| LLM | OpenAI `gpt-4o` via `${OPENAI_API_KEY}`; temperature = 0 |
| Build | Maven (`./mvnw`) |
| Main package | `com.incidentmanagement` |
| Port | 8080 |

---

## Mandatory programming model

All AI agents are **Java interfaces** with these annotations:

```java
public interface MyAgent {

    @SystemMessage("Role and hard rules for the LLM.")
    @UserMessage("Per-call prompt with {placeholder} substitution.")
    @Agent(description = "One sentence.", outputKey = "myKey")  // outputKey required for workflows
    @ToolBox(MyTool.class)   // omit if no tools
    String process(String param);
}
```

Tools are `@ApplicationScoped` CDI beans:

```java
@ApplicationScoped
public class MyTool {
    @Tool("Description the LLM reads.")
    @Transactional   // required if calling entity.persist()
    public String doAction(String input) { ... }
}
```

---

## Project rules (AI assistants must follow all 10)

1. **Never create a class that implements an agent interface** — Quarkus generates it.
2. **Never add CDI scope annotations** (`@ApplicationScoped`, `@Singleton`) to agent interfaces.
3. **`@ToolBox` only** — no constructor injection of tools into agent interfaces.
4. **`outputKey` is mandatory** on every agent used inside `@SequenceAgent` or `@SupervisorAgent`.
5. **`@Transactional` is mandatory** on any tool method that calls `entity.persist()`.
6. **Never log full incident report strings** — PII risk. Log only incident number and status.
7. **No secrets in `@SystemMessage` / `@UserMessage`** — use `application.properties` vars.
8. **Approval gate first** — propose diff and wait for approval before writing files.
9. **Tests are required** — every agent/tool change needs a `@QuarkusTest`.
10. **Never invent third-party APIs** not present in `pom.xml` or this file.

---

## Domain model

| Type | Fields / Values |
|------|----------------|
| `IncidentInfo` (Panache entity) | `id` (Long), `system` (column: system_name), `service`, `priority`, `description`, `status` |
| `IncidentStatus` (enum) | `OPEN`, `TRIAGING`, `IN_PROGRESS`, `ESCALATED`, `RESOLVED` |
| `IncidentAction` (enum) | `ESCALATE`, `INVESTIGATE`, `TRIAGE`, `RESOLVE` |
| `AnalysisTask` (record) | `analysisType` (`AnalysisType`), `systemInstructions` — factory methods: `AnalysisTask.severity()`, `.impact()`, `.resolution()` |
| `AnalysisType` (enum) | `SEVERITY`, `IMPACT`, `RESOLUTION` |
| `IncidentAnalysisResults` (record) | `severityAnalysis`, `impactAnalysis`, `resolutionAnalysis` |
| `IncidentOutcome` (record) | `resolution` (String), `incidentAction` (IncidentAction) |

---

## API endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/incident-management/process/{incidentNumber}` | Trigger incident processing workflow |
| `GET` | `/incidents` | List all incidents |
| `GET` | `/incidents/{id}` | Single incident by ID |

---

## Seeded incidents (import.sql)

| # | System | Service | Priority | Initial status |
|---|--------|---------|----------|---------------|
| 1 | payment-gateway | checkout-api | P2 | OPEN |
| 2 | auth-service | user-login | P1 | IN_PROGRESS |
| 3 | inventory-db | stock-sync | P3 | OPEN |
| 4 | cdn-edge | static-assets | P4 | TRIAGING |
| 5 | email-service | notification-api | P2 | OPEN |
| 6 | search-engine | product-search | P3 | OPEN |
| 7 | monitoring | alerting-api | P2 | OPEN |
| 8 | api-gateway | rate-limiter | P1 | IN_PROGRESS |

---

## Agents (update this table as you complete each exercise)

| Interface | File | Role | outputKey | Exercise |
|-----------|------|------|-----------|---------|
| `TriageAgent` | `agents/TriageAgent.java` | Requests triage via `TriageTool` | `analysisResult` | Ex 1 |
| `DiagnosticAgent` | `agents/DiagnosticAgent.java` | Returns diagnostic plan (no tool) | `analysisResult` | Ex 2 |
| `IncidentAnalysisAgent` | `agents/IncidentAnalysisAgent.java` | Parameterized; classifies by task type | `incidentAnalysis` | Ex 3 |
| `ImpactAgent` | `agents/ImpactAgent.java` | Estimates business impact and SLA cost | `businessImpact` | Ex 4 |
| `EscalationAgent` | `agents/EscalationAgent.java` | Decides ESCALATE_P1/ASSIGN_TEAM/WORKAROUND/CLOSE | `escalationAction` | Ex 4 |
| `ResolutionAgent` | `agents/ResolutionAgent.java` | Final IncidentOutcome (typed return) | `incidentOutcome` | Ex 4 |
| `IncidentSupervisorAgent` | `agents/IncidentSupervisorAgent.java` | `@SupervisorAgent` orchestrator | `supervisorDecision` | Ex 4 |

---

## Workflows

| Interface | Annotation | Sub-agents | Exercise |
|-----------|------------|------------|---------|
| `IncidentAnalysisWorkflow` | `@ParallelMapperAgent` | `IncidentAnalysisAgent` × 3 tasks | Ex 3 |
| `IncidentProcessingWorkflow` | `@SequenceAgent` | `IncidentAnalysisWorkflow` → `IncidentSupervisorAgent` → `ResolutionAgent` | Ex 4 |
