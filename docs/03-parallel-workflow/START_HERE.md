# Exercise 3 — Parallel Agents: @ParallelMapperAgent

<span class="badge badge--code-along">Code-Along</span>

**Timebox:** 10 minutes  
**Persona:** Chris — Ops lead  
**You work in:** `lab/` (keep Quarkus running)  
**Files to edit:**

- `lab/src/main/java/com/incidentmanagement/agentic/agents/IncidentAnalysisAgent.java`
- `lab/src/main/java/com/incidentmanagement/agentic/workflow/IncidentAnalysisWorkflow.java`

!!! tip "Solution fallback"
    [`solutions/03-parallel-workflow`](https://github.com/danieloh30/agentic-ai-java-workshop/tree/main/solutions/03-parallel-workflow){:target="_blank"} — open if stuck.

---

## The goal

Run three **parallel agents** — severity, impact, and resolution analysis — concurrently instead of sequentially. Wall-clock time ≈ slowest single call (not the sum). One interface declaration, three concurrent LLM calls, each with a different `@SystemMessage` injected at runtime.

This introduces two concepts: **dynamic `@SystemMessage`** (same agent interface, different prompt per task) and **`@Output`** (aggregating parallel results into a typed record before downstream agents read scope).

---

## How `AgenticScope` connects agents

```mermaid
%%{init: {'look':'handDrawn','theme':'neutral','themeVariables': {'lineColor':'#4A4035'}}}%%
flowchart TD
    A([IncidentAnalysisAgent x3])
    B([Output → List of String])
    C([IncidentAnalysisResults])
    D([IncidentSupervisorAgent])

    A --> B --> C --> D

    subgraph parallel["Concurrent — outputKey = incidentAnalysis"]
        A1([Severity analyst])
        A2([Impact analyst])
        A3([Resolution analyst])
    end

    A1 --> B
    A2 --> B
    A3 --> B

    style A fill:#D4E6F1,stroke:#2E6B8A
    style B fill:#E8E0F0,stroke:#6B5B8A
    style C fill:#E8E0F0,stroke:#6B5B8A
    style D fill:#E8DCC4,stroke:#6B5B45
    style A1 fill:#D8F0D8,stroke:#3D7A3D
    style A2 fill:#D8F0D8,stroke:#3D7A3D
    style A3 fill:#D8F0D8,stroke:#3D7A3D
    style parallel fill:#F5F5F0,stroke:#8B8070
```

Each parallel invocation of `IncidentAnalysisAgent` writes its result under `"incidentAnalysis"`. The `@ParallelMapperAgent` framework collects these into a `List<String>` and passes it to the `@Output` method, which maps them positionally into `IncidentAnalysisResults`.

!!! warning "Rule"
    Every agent in a workflow **must** declare `outputKey`. Without it, the result is silently dropped from scope and the next agent finds nothing.

---

## Step 1 — Implement `IncidentAnalysisAgent` (4 min)

Open [`IncidentAnalysisAgent.java`](https://github.com/danieloh30/agentic-ai-java-workshop/blob/main/lab/src/main/java/com/incidentmanagement/agentic/agents/IncidentAnalysisAgent.java){:target="_blank"}.

The method signature is already declared. Add these annotations **above** it:

```java
@SystemMessage("{task.systemInstructions}")
@UserMessage("""
    Incident Information:
    System: {incidentInfo.system}
    Service: {incidentInfo.service}
    Priority: P{incidentInfo.priority}
    Current Description: {incidentInfo.description}

    Report: {report}
    """)
@Agent(description = "Incident analyzer. Using report, determines if action is needed based on task type.",
       outputKey = "incidentAnalysis")
```

??? info "Dynamic `@SystemMessage` — one interface, three roles"
    The `{task.systemInstructions}` placeholder is resolved from the `AnalysisTask` parameter at runtime, not from a compile-time constant. The same interface handles three completely different analysis tasks:

    | Task index | `systemInstructions` content |
    |------------|------------------------------|
    | 0 | "You are a severity analyzer. Classify as P1-P4 or SEVERITY_LOW." |
    | 1 | "You are a business impact analyzer. Assess revenue/SLA impact or IMPACT_MINIMAL." |
    | 2 | "You are a resolution analyzer. If critical, ESCALATION_REQUIRED. Else ESCALATION_NOT_REQUIRED." |

    One interface declaration powers three concurrent LLM calls with different roles.

---

## Step 2 — Implement `IncidentAnalysisWorkflow` (3 min)

Open [`IncidentAnalysisWorkflow.java`](https://github.com/danieloh30/agentic-ai-java-workshop/blob/main/lab/src/main/java/com/incidentmanagement/agentic/workflow/IncidentAnalysisWorkflow.java){:target="_blank"}.

Replace both `// TODO` blocks with the following **two members**:

**2a — `@ParallelMapperAgent` method:**

```java
@ParallelMapperAgent(
        description = "Analyzes incident reports in parallel for severity, impact, and resolution needs",
        outputKey = "incidentAnalysisResults",
        subAgent = IncidentAnalysisAgent.class,
        itemsProvider = "tasks")
IncidentAnalysisResults analyzeIncident(List<AnalysisTask> tasks,
                                        IncidentInfo incidentInfo,
                                        Integer incidentNumber,
                                        String report);
```

**2b — `@Output` static method:**

```java
@Output
static IncidentAnalysisResults output(AgenticScope scope,
                                      List<String> incidentAnalysisResults) {
    return new IncidentAnalysisResults(
            incidentAnalysisResults.get(0),  // severityAnalysis
            incidentAnalysisResults.get(1),  // impactAnalysis
            incidentAnalysisResults.get(2)   // resolutionAnalysis
    );
}
```

??? info "Why `itemsProvider = \"tasks\"`?"
    `@ParallelMapperAgent` needs to know which method parameter holds the list of items to fan out over. `itemsProvider = "tasks"` names the `tasks` parameter — the framework fans out one `IncidentAnalysisAgent` call per element in `tasks`. The three calls run concurrently in separate virtual threads.

??? info "Why a static `@Output` method?"
    `@Output` is a post-processing step, not an LLM call. It receives the collected results from all parallel invocations and transforms them into a typed record. It runs synchronously after all parallel calls complete. Marking it `static` makes it clear it is a pure transformation with no injected dependencies.

Save both files. Quarkus hot-reloads.

---

## Step 3 — Verify in the Dev UI (1 min)

Open the [Agentic Dev UI](http://localhost:8080/q/dev-ui/quarkus-langchain4j-agentic/agents){:target="_blank"} to see the agent graph. Look for these two entries:

| Agent | Type | Key detail |
|-------|------|------------|
| IncidentAnalysisWorkflow | ParallelMapperAgent | subAgent = IncidentAnalysisAgent, itemsProvider = "tasks" |
| IncidentAnalysisAgent | Agent | outputKey = "incidentAnalysis" |

This page visualizes every agent's type, `outputKey`, and sub-agent wiring — use it after every exercise to confirm your changes compiled and registered correctly.

??? question "Why not a Java `for` loop instead of `itemsProvider`?"
    `AgenticScope` must be injected per-invocation so each parallel run gets its own scope context, result slot, and `outputKey` entry. A Java loop over LLM calls would run sequentially in the same thread with a shared scope — defeating both the parallelism and the scope isolation.

---

## Step 4 — Test parallel execution (2 min)

The full workflow isn't wired in `lab/` yet (that happens in Exercise 4), so you'll run this test from the **solution project**.

Stop `lab/` first (`Ctrl+C`), then start the solution:

```bash
cd ../solutions/03-parallel-workflow
./mvnw quarkus:dev
```

Open the [topology](http://localhost:8080/q/dev-ui/quarkus-langchain4j-agentic/topology){:target="_blank"} — this solution has `IncidentProcessingWorkflow` fully wired, so the full agent tree is visible.

Open **[http://localhost:8080](http://localhost:8080){:target="_blank"}**, click **View** on Incident **#5** (email-service/notification-api), and process with:

```
SMTP timeout for 30% of outbound emails, queue growing
```

**Expected terminal logs:**
```
ResolutionAgent updating...
  └─ Action: INVESTIGATE → SMTP timeout affecting 30% of outbound emails...
```

The action should be `INVESTIGATE` or `TRIAGE` — the parallel analysis flagged this as needing attention. Check the UI — incident status changes to `IN_PROGRESS` or `TRIAGING`. (LLM responses are non-deterministic — the exact action may vary.)

!!! tip "Parallelism proof"
    Note the wall-clock time from when you clicked "Process" to when `ResolutionAgent updating...` appears (~3–5 seconds). Three parallel LLM calls complete in roughly the time of one. If they ran sequentially, it would take ~3x longer.

Now press `s` to restart (reset the database), then **reload the browser**. Process Incident **#6** (search-engine/product-search) with:

```
False alarm, relevance restored after cache refresh
```

**Expected:** Action is `MONITOR` or `RESOLVE` — the parallel analysis determined this is a low-severity, low-impact false alarm. Status stays `OPEN` or changes to `RESOLVED`.

Stop the solution (`Ctrl+C`) and restart `lab/`:

```bash
cd ../../lab
./mvnw quarkus:dev
```

---

<div class="done-when" markdown>

## :material-check-circle: Done when

- [ ] `IncidentAnalysisAgent` and `IncidentAnalysisWorkflow` appear in the [Agentic Dev UI](http://localhost:8080/q/dev-ui/quarkus-langchain4j-agentic/agents){:target="_blank"}
- [ ] No compile errors in the Quarkus terminal after hot reload
- [ ] Parallel execution tested via solution — wall-clock time confirms concurrency
- [ ] You can explain `outputKey` and `@Output` from memory
- [ ] You can explain how dynamic SystemMessage enables one interface for 3 tasks

</div>
