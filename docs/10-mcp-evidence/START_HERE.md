# Exercise 10 — Required MCP Evidence

<span class="badge badge--code-along">Code-Along</span> <span class="badge">Part 2 · 10 minutes</span>

**Persona:** Riley — SRE lead<br>
**Continue in:** the running `part2/lab/` application, **Planning / MCP** mode.<br>
**Files to edit:** `src/main/java/com/incidentmanagement/agentic/workflow/PlanExecuteFlow.java` and `src/main/resources/application.properties`.

## Enterprise context and goal

Extend the plan you just built with a required deployment lookup. Fetch evidence through real MCP over Streamable HTTP, validate its service and fields, and stop before planning if it is unavailable. Keep all earlier edits and results.

<div class="compact-diagram" markdown="1">

```mermaid
%%{init: {'look':'handDrawn','theme':'base','themeVariables': {'darkMode':false,'background':'#FFFCF5','primaryTextColor':'#1F1F1F','secondaryTextColor':'#1F1F1F','tertiaryTextColor':'#1F1F1F','textColor':'#1F1F1F','lineColor':'#4A4035','edgeLabelBackground':'#FFFCF5','clusterBkg':'#F5F5F0','clusterBorder':'#6B5B45','fontSize':'16px'},'flowchart': {'nodeSpacing':35,'rankSpacing':55,'padding':18}}}%%
flowchart TD
    MCP(["Required MCP evidence"])
    VALID{"Evidence valid?"}
    STOP["EVIDENCE_UNAVAILABLE"]
    PLAN(["Plan"])
    CHECK["Java prerequisite check"]
    EXEC(["Specialist recommendations"])
    REVIEW{"Review"}
    HUMAN(["Operator verification<br/>still required"])

    MCP --> VALID
    VALID -->|No| STOP
    VALID -->|Yes| PLAN
    PLAN --> CHECK --> EXEC --> REVIEW
    REVIEW -->|"Feedback, at most 3 iterations"| PLAN
    REVIEW -->|Ready| HUMAN

    style MCP fill:#D4E6F1,stroke:#2E6B8A,color:#1F1F1F,stroke-width:2px
    style VALID fill:#FFE4CC,stroke:#B87333,color:#1F1F1F,stroke-width:2px
    style STOP fill:#F8D7DA,stroke:#A94442,color:#1F1F1F,stroke-width:2px
    style PLAN fill:#D4E6F1,stroke:#2E6B8A,color:#1F1F1F,stroke-width:2px
    style CHECK fill:#FFF8DC,stroke:#C4A000,color:#1F1F1F,stroke-width:2px
    style EXEC fill:#D8F0D8,stroke:#3D7A3D,color:#1F1F1F,stroke-width:2px
    style REVIEW fill:#FFE4CC,stroke:#B87333,color:#1F1F1F,stroke-width:2px
    style HUMAN fill:#E8DCC4,stroke:#6B5B45,color:#1F1F1F,stroke-width:2px
```

</div>

## Step 1 — Connect the lookup (3 min)

In `PlanExecuteFlow.resolve(...)`, replace the explicit `not_requested` baseline with:

```java
var evidence = evidenceClient.fetch(incident.system);
```

Save, then make a request to trigger hot reload. The supplied `DeploymentEvidenceClient` uses a named `McpClient` to call `getRecentDeployments`, validates the result, and returns an unavailable outcome on transport, tool, or validation failure. A failed lookup never falls back to the evidence-free baseline.

This lookup is mandatory, so workflow code calls it before the planner. `@McpToolBox` instead gives a model tools to select. A2A, from Exercise 7, delegates work to another agent. Here MCP supplies data used by our own agents.

## Step 2 — Compare with the baseline (3 min)

Process incident **#2** again with the same operator report. The evidence section now contains `workshop-release-42` and the fixture observation about a reduced memory limit. Inspect the plan and diagnosis for their use of that evidence.

These are **fixed workshop data**, not live infrastructure observations or proof of causation. You are making a new planning run; earlier runs are not replayed automatically. `/incident-plan/2` also starts a new diagnostic run if called with POST.

## Step 3 — Make evidence unavailable without restarting (4 min)

In `application.properties`, change:

```properties
workshop.evidence.scenario=unavailable
```

Leave dev mode running. Save and submit #2 again to trigger automatic reload. Expect `EVIDENCE_UNAVAILABLE` and zero planning iterations. The request still crosses the MCP boundary, but the returned fixture cannot satisfy the evidence requirement.

Change the same property back to `available`, save, and make another request. This is a normal code/configuration edit picked up by hot reload—not a terminal restart or data reset. Existing incident records remain available.

## Done when

- [ ] You observed real MCP evidence replacing the explicit no-lookup baseline.
- [ ] Unavailable evidence stopped the run before any model call.
- [ ] You restored `available` by editing the property while keeping dev mode running.

**Keep the same folder and terminal.** Switch the dashboard mode for [Exercise 11 — Memory Boundaries](../11-memory-tiering/START_HERE.md). If stuck, compare `PlanExecuteFlow.java` with the shared solution; do not overwrite the whole project.
