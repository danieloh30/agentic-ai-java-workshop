# Exercise 8 — Programmatic Orchestration: Quality Loop (Bonus)

<span class="badge badge--code-along">Code-Along</span> <span class="badge" style="background:#6B5B8A;color:white;">Bonus</span>

**Timebox:** 15 minutes (self-paced)  
**Persona:** Jordan — Java platform engineer  
**You work in:** `solutions/08-quarkus-flow/lab/`  
**Files to edit:**

- `src/.../agentic/agents/ReportDrafterAgent.java`
- `src/.../agentic/agents/ReportCriticAgent.java`
- `src/.../agentic/workflow/IncidentReportFlow.java`

!!! tip "Full solution"
    If stuck, the completed files are in [`solutions/08-quarkus-flow/`](https://github.com/danieloh30/agentic-ai-java-workshop/tree/main/solutions/08-quarkus-flow){:target="_blank"} — copy them over your TODO files and restart.

---

## Why programmatic orchestration?

In Exercises 1–4 you used **declarative annotations** — `@SequenceAgent`, `@ParallelMapperAgent`, `@SupervisorAgent` — to wire agents. These are powerful but have a hard limitation:

!!! failure "Declarative annotations can't loop"
    A `@SequenceAgent` runs its sub-agents **once** and stops. A `@SupervisorAgent` routes via LLM reasoning, but there's no deterministic **"retry until quality passes."**

This exercise introduces **programmatic orchestration** using LangChain4j's builder API backed by [Quarkus Flow](https://docs.quarkiverse.io/quarkus-flow/dev/index.html){:target="_blank"} — a CNCF Serverless Workflow engine. The builder pattern lets you express **loops, conditions, and exit criteria** in plain Java:

```java
AgenticServices.loopBuilder()
    .maxIterations(3)
    .exitCondition((scope, iteration) -> scope.readState("score", 0) >= 7)
    .subAgents(draftAction, critiqueAction)
    .build();
```

---

## The scenario

After an incident is resolved, someone needs to write the **post-incident report (PIR)**. You'll build a workflow that **generates, critiques, and iteratively refines** the report until quality passes a threshold:

```mermaid
%%{init: {'look':'handDrawn','theme':'neutral','themeVariables': {'lineColor':'#4A4035'}}}%%
flowchart TD
    START(["Incident from DB"])
    DRAFT(["ReportDrafterAgent"])
    CRITIQUE(["ReportCriticAgent"])
    CHECK{score >= 7?}
    DONE(["Final Report"])

    START --> DRAFT
    DRAFT --> CRITIQUE
    CRITIQUE --> CHECK
    CHECK -->|No, iterate| DRAFT
    CHECK -->|Yes| DONE

    style START fill:#E8DCC4,stroke:#6B5B45
    style DRAFT fill:#D4E6F1,stroke:#2E6B8A
    style CRITIQUE fill:#FFE4CC,stroke:#B87333
    style CHECK fill:#FFF8DC,stroke:#C4A000
    style DONE fill:#D8F0D8,stroke:#3D7A3D
```

**This is impossible with declarative annotations.** There is no `@LoopAgent` — the loop exists only in the programmatic builder API.

---

## Step 0 — Start the project (2 min)

Stop any running Quarkus process (`Ctrl+C`), then:

!!! warning "Different `lab/` folder"
    This exercise uses `solutions/08-quarkus-flow/lab/` — **not** the root-level `lab/` folder from Exercises 1–4. Make sure you `cd` to the correct location.

```bash
cd solutions/08-quarkus-flow/lab
./mvnw quarkus:dev
```

Wait for `Listening on: http://localhost:8080`.

!!! info "New dependency"
    Check `pom.xml` — this project adds `quarkus-flow-langchain4j`, which provides the `AgenticServices.loopBuilder()` runtime backed by the CNCF Serverless Workflow engine.

---

## Step 1 — Implement the agents (5 min)

### Step 1a — ReportDrafterAgent

Open `src/.../agentic/agents/ReportDrafterAgent.java`. Replace the placeholder `@UserMessage` and `// TODO` comment with the full annotations:

```java
    @SystemMessage("""
            You are a post-incident report writer for an IT incident management system.
            Write a clear, structured post-incident report (PIR) covering:
            1. Incident Summary (system, service, priority, what happened)
            2. Timeline (when detected, key milestones)
            3. Root Cause Analysis
            4. Impact Assessment
            5. Resolution Steps Taken
            6. Preventive Measures / Action Items

            If you receive reviewer feedback from a previous draft, incorporate the feedback
            to improve the report. Keep the report concise (under 500 words).
            Output ONLY the report text.
            """)
    @UserMessage("""
            Write a post-incident report for:
            - System: {system}
            - Service: {service}
            - Priority: {priority}
            - Description: {description}
            - Status: {status}

            Reviewer feedback from previous draft:
            {feedback}
            """)
```

The `@SystemMessage` defines the agent's role; the `@UserMessage` template maps each method parameter to a placeholder. The key difference from earlier exercises: the `feedback` parameter lets the loop feed critic feedback back into the next draft.

### Step 1b — ReportCriticAgent

Open `src/.../agentic/agents/ReportCriticAgent.java`. Replace the placeholder `@UserMessage` and `// TODO` comment with the full annotations:

```java
    @SystemMessage("""
            You are a quality reviewer for post-incident reports (PIRs).
            Evaluate the report on these criteria:
            - Completeness: Does it cover summary, timeline, root cause, impact, resolution, and action items?
            - Clarity: Is it clear and free of unnecessary jargon?
            - Actionability: Are the preventive measures specific and assignable?
            - Accuracy: Does the report match the incident details provided?

            Scoring guide:
            - 1-3: Missing major sections or factual errors
            - 4-6: Incomplete sections or vague action items
            - 7-8: Solid report with minor improvements possible
            - 9-10: Exemplary, ready for stakeholder distribution
            """)
    @UserMessage("""
            Evaluate this post-incident report for an incident on {system}/{service} ({priority}):

            --- REPORT START ---
            {report}
            --- REPORT END ---
            """)
```

Notice the return type is `ReportCritique` — a Java record with `score` and `feedback` fields. LangChain4j automatically instructs the LLM to respond in JSON and deserializes it.

---

## Step 2 — Build the loop workflow (5 min)

Open `src/.../agentic/workflow/IncidentReportFlow.java`. This is where the **programmatic orchestration** happens — no annotations, just Java code.

### Step 2a — Draft agent action

Uncomment the code block under `// TODO Exercise 08 — Step 2a` — remove the `//` prefixes so it becomes:

```java
        var draftAction = AgenticServices.agentAction(scope -> {
            String feedback = scope.readState("feedback", "No previous feedback. Write the first draft.");
            int iteration = scope.readState("iteration", 0) + 1;
            scope.writeState("iteration", iteration);
            Log.infof("Report Draft — iteration %d", iteration);

            String report = drafterAgent.draftReport(
                    incident.system, incident.service, incident.priority,
                    incident.description != null ? incident.description : "",
                    incident.status.toString(), feedback);
            scope.writeState("report", report);
        });
```

**Key concept: `AgenticScope`** — a shared state bag that passes data between agents in the loop. Each agent reads what it needs and writes its output. On the next iteration, the drafter picks up the critic's `feedback` from scope.

### Step 2b — Critique agent action

Uncomment the code block under `// TODO Exercise 08 — Step 2b`:

```java
        var critiqueAction = AgenticServices.agentAction(scope -> {
            String report = scope.readState("report", "");
            int iteration = scope.readState("iteration", 1);

            ReportCritique critique = criticAgent.critiqueReport(
                    incident.system, incident.service, incident.priority, report);

            scope.writeState("score", critique.score());
            scope.writeState("feedback", critique.feedback());
            Log.infof("Report Critique — iteration %d: score=%d, feedback=%s",
                    iteration, critique.score(), critique.feedback());
        });
```

### Step 2c — The loop

Uncomment the code block under `// TODO Exercise 08 — Step 2c` and **delete the `return Map.of();`** placeholder:

```java
        UntypedAgent workflow = AgenticServices.loopBuilder()
                .name("incident-report-quality-loop")
                .maxIterations(3)
                .exitCondition((scope, iteration) -> {
                    int score = scope.readState("score", 0);
                    return score >= 7;
                })
                .subAgents(draftAction, critiqueAction)
                .build();

        var result = workflow.invokeWithAgenticScope(Map.of());
        return result.agenticScope().state();
```

**This is the "aha" moment.** `loopBuilder()` creates a workflow that:

1. Runs `draftAction` then `critiqueAction` (one iteration)
2. Checks the exit condition: is `score >= 7`?
3. If yes → returns the final state (report + score + feedback)
4. If no → loops back to `draftAction` with the critic's feedback in scope
5. Stops after at most 3 iterations regardless

Compare this with `@SequenceAgent` from Exercise 4 — that runs its sub-agents exactly once. There is no way to express "retry until quality passes" with annotations alone.

---

## Step 3 — Test it (3 min)

Hot reload should have picked up your changes. Open [http://localhost:8080](http://localhost:8080){:target="_blank"} — click a **Resolved** or **Escalated** incident (e.g., #1 or #2), then click **Generate Report**.

The button triggers the quality loop. While it runs, **watch the terminal logs** — you should see the loop in action:

```
Report Draft — iteration 1
Report Critique — iteration 1: score=9, feedback=The report is solid...
Report quality loop completed for incident #1 — final score: 9
```

When the loop finishes, the dashboard shows the generated report with its **score** and **iteration count**.

<img src="../../images/generate_report.png" alt="Generate Report result in dashboard" style="width:100%;max-width:480px;display:block;margin:1rem auto;border-radius:8px;box-shadow:0 2px 12px rgba(0,0,0,0.15);">

!!! info "How many iterations?"
    The LLM often produces a good first draft (score >= 7), so the loop may exit after just 1 iteration. Try different incidents — some may need 2-3 rounds to reach the quality threshold.

Try several incidents to see how the loop adapts — P1 incidents with more complexity tend to need more iterations than P4s.

---

## Annotations vs programmatic builders — when to use each

| | Declarative annotations | Programmatic builders |
|--|---|---|
| **Loops** | `@LoopAgent` (with [Quarkus Flow](https://docs.quarkiverse.io/quarkus-flow/dev/langchain4j.html){:target="_blank"} extension) | `loopBuilder().exitCondition(...)` |
| **Conditionals** | `@ConditionalAgent` (single gate) | `conditionalBuilder()` (multi-branch) |
| **Data flow** | Automatic via `AgenticScope` keys | Explicit `scope.readState/writeState` |
| **Orchestration** | `@SequenceAgent`, `@ParallelMapperAgent`, `@SupervisorAgent` | `sequenceBuilder()`, `parallelBuilder()`, `loopBuilder()` |
| **Best for** | Standard agent pipelines | Complex control flow, retry logic |
| **Observability** | Full topology in Dev UI | Loop progress visible in terminal logs |

!!! tip "Start declarative, go programmatic when you need loops or complex branching"
    Annotations handle 80% of use cases. Switch to builders when you need deterministic loops, multi-branch conditionals, or mixed orchestration (agents + HTTP calls + events in one workflow).

---

<div class="done-when" markdown>

## :material-check-circle: Done when

- [ ] Report quality loop completed at least 1 iteration (logs show "Report Draft" and "Report Critique")
- [ ] Final report has score >= 7
- [ ] You can explain why `@SequenceAgent` cannot loop
- [ ] You can describe the `AgenticScope` read/write pattern for inter-agent communication

</div>
