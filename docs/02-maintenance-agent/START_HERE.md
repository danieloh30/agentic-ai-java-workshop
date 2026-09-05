# Exercise 2 — Policy as Prompt

<span class="badge badge--code-along">Code-Along</span>

**Timebox:** 10 minutes  
**Persona:** Chris — Ops lead  
**You work in:** `lab/` (keep Quarkus running)  
**Files to edit:** `lab/src/main/java/com/incidentmanagement/agentic/agents/DiagnosticAgent.java`

!!! tip "Solution fallback"
    [`solutions/02-maintenance-agent`](https://github.com/danieloh30/agentic-ai-java-workshop/tree/main/solutions/02-maintenance-agent){:target="_blank"} — includes `DiagnosticAgent` wired into a composed workflow.

---

## The goal

Discover that `@SystemMessage` **is** the policy — not code logic, not conditional branches. You'll add `DiagnosticAgent` (a text-only agent with no tool), then run a live tuning experiment: edit a single string in `@SystemMessage`, hot-reload, and watch the same incident produce completely different agent behavior.

This is the key insight: **changing a prompt changes the policy**. No redeploy, no `if/else`, no feature flag.

---

## Step 1 — Implement `DiagnosticAgent` (4 min)

Open [`DiagnosticAgent.java`](https://github.com/danieloh30/agentic-ai-java-workshop/blob/main/lab/src/main/java/com/incidentmanagement/agentic/agents/DiagnosticAgent.java){:target="_blank"}.

The method signature is already declared. Add these annotations **above** it:

```java
@SystemMessage("""
    You handle intake for the diagnostic department of an IT incident management system.
    Based on the incident report, determine what specific diagnostic actions are needed
    and provide a detailed root cause analysis plan.
    Be specific about what actions are needed based on the incident report.

    Available diagnostic actions include:
    - Log analysis
    - Service restart
    - Config rollback
    - Dependency check
    - Performance profiling
    - Network trace (packet capture, DNS, connectivity)

    For infrastructure issues like connectivity or DNS problems, include network trace in your plan.

    Provide your response as a structured diagnostic plan listing the specific actions needed.
    If no diagnostic action is needed based on the request, respond with "DIAGNOSTIC_NOT_REQUIRED".
    """)
@UserMessage("""
    Incident Information:
    System: {incidentInfo.system}
    Service: {incidentInfo.service}
    Priority: P{incidentInfo.priority}
    Incident Number: {incidentNumber}

    Diagnostic Request:
    {diagnosticRequest}
    """)
@Agent(description = "Incident diagnostic specialist. Using incident information and request, determines what diagnostic actions are needed.",
       outputKey = "analysisResult")
```

??? info "Why no `@ToolBox` here?"
    `DiagnosticAgent` returns a *plan* as text — it does not write to the database. The supervisor in Exercise 4 reads this text plan and decides whether to escalate. Text-only agents are faster and cheaper: no tool-call round-trips to the LLM.

    **Compare with `TriageAgent`:** `TriageAgent` must call `TriageTool.requestTriage()` to actually mutate `IncidentStatus`. `DiagnosticAgent` only produces a recommendation. The supervisor decides what happens next.

Save the file. Quarkus hot-reloads. Check the terminal for any compile errors.

---

## Step 2 — Verify the compile (1 min)

`DiagnosticAgent` wires into the supervisor orchestration in Exercise 4, so it can't be tested end-to-end yet. But you can confirm it compiled correctly:

1. Check the Quarkus terminal — no red stack traces after hot reload
2. Open the [Agentic Dev UI](http://localhost:8080/q/dev-ui/quarkus-langchain4j-agentic/agents){:target="_blank"} — confirm `DiagnosticAgent` appears in the agent list with its `outputKey` and description

If you see `Unsatisfied dependency` or `AmbiguousResolutionException`, double-check: `DiagnosticAgent` must be an **interface** (not a class), with **no** `@ApplicationScoped` annotation.

---

## Step 3 — @SystemMessage tuning experiment (5 min)

This is one of the most important insights in this lab: **`@SystemMessage` is a policy declaration, not code logic**.

The full workflow isn't wired in `lab/` yet (that happens in Exercise 4), so you'll run this experiment from the **solution project**.

Stop `lab/` first (`Ctrl+C`), then start the solution:

```bash
cd ../solutions/02-maintenance-agent
./mvnw quarkus:dev
```

Open `TriageFeedbackAgent.java` in the solution project and find this line in the `@SystemMessage`:

```
If no triage actions are needed based on the report, respond with "TRIAGE_NOT_REQUIRED".
```

**Replace** that line with the strict version:

```
Only request triage for CRITICAL issues: complete service outages,
data loss or corruption, security breaches, or cascading failures affecting multiple systems.
For intermittent errors, slow responses, or single-user complaints, respond with "TRIAGE_NOT_REQUIRED".
```

!!! info "Why `TriageFeedbackAgent` and not `TriageAgent`?"
    `TriageFeedbackAgent` is the agent that *decides* whether triage is needed — its output (`triageRequest`) feeds into the `@Output` method that picks `MONITOR` vs `TRIAGE`. `TriageAgent` only *executes* triage actions after the decision is already made. Changing the wrong agent's prompt would have no visible effect.

Quarkus hot-reloads in ~1 second. Open **[http://localhost:8080](http://localhost:8080){:target="_blank"}**, click **View** on Incident **#7** (monitoring/alerting-api), and process it with:

```
Alert threshold slightly too sensitive, causing a few extra notifications
```

**How to confirm:** Check the Quarkus terminal logs and the incident status in the UI.

**Expected terminal logs (minor report):**
```
ReportAnalysisWorkflow executing...
  ├─ TriageFeedbackAgent analyzing...
  └─ DiagnosticFeedbackAgent analyzing...
IncidentAssignmentWorkflow evaluating conditions...
ResolutionAgent updating...
  └─ Incident #7 action: MONITOR
```

The key line is `Incident #7 action: MONITOR` or `Incident #7 action: RESOLVE` — the agents decided neither triage nor diagnostic escalation is needed for a minor alert sensitivity issue. Status stays `OPEN` or changes to `RESOLVED`. (LLM responses are non-deterministic — the exact action may vary between runs.)

Now press `s` to restart (reset the database), then **reload the browser** to see incident #7 back to `OPEN`. Process it with a critical report:

```
Complete monitoring blackout — zero alerts firing, all dashboards showing stale data, on-call has no visibility
```

**Expected terminal logs (critical report):**
```
ReportAnalysisWorkflow executing...
  ├─ TriageFeedbackAgent analyzing...
  └─ DiagnosticFeedbackAgent analyzing...
IncidentAssignmentWorkflow evaluating conditions...
ResolutionAgent updating...
  └─ Incident #7 action: INVESTIGATE
```

The key difference: the action should be `INVESTIGATE` or `TRIAGE` — a more aggressive response than the minor report. The `DiagnosticFeedbackAgent` flagged this as needing root cause analysis. Incident status changes to `IN_PROGRESS` or `TRIAGING` in the UI.

!!! warning "Key insight"
    You changed agent *behavior* by editing a string — no conditional logic, no redeploy cycle beyond hot reload. The `@SystemMessage` is the policy. This is what "declarative AI engineering" means.

**Revert** `TriageFeedbackAgent.java`'s `@SystemMessage` back to the original (simpler) version.

Stop the solution (`Ctrl+C`) and restart the lab for the next exercise:

```bash
cd ../../lab
./mvnw quarkus:dev
```

---

<div class="done-when" markdown>

## :material-check-circle: Done when

- [ ] `DiagnosticAgent.java` compiles — no errors (interface, `outputKey="analysisResult"`, no CDI scope, no `@ToolBox`)
- [ ] `@SystemMessage` threshold experiment completed — strict vs lenient behavior observed
- [ ] You can articulate: when does an agent need `@ToolBox`? When is text-only output correct?

</div>
