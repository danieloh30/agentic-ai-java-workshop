# Exercise 10 — Required MCP Evidence

<span class="badge badge--code-along">Code-Along</span> <span class="badge">Part 2 · 10 minutes</span>

**Persona:** Riley — SRE lead<br>
**Continue in:** the running `part2/lab/` application, **Planning / MCP** mode.<br>
**Files to edit:** `src/main/java/com/incidentmanagement/agentic/workflow/PlanExecuteFlow.java` and `src/main/resources/application.properties`.

## Enterprise context and goal

Riley, the SRE lead, needs deployment context before recommending how to handle the authentication incident. An incident report alone cannot establish what changed in the latest release, and a failed lookup must not become an invented deployment history. Riley wants the team to see which evidence informed the plan and when the required evidence could not be obtained.

Extend the plan you just built with a required deployment lookup. Fetch evidence through real MCP over Streamable HTTP, validate its service and fields, and stop before planning if it is unavailable. Keep all earlier edits and results. The supplied workshop evidence informs recommendations; it does not prove a cause or confirm production recovery.

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

1. Open [the dashboard](http://localhost:8080) in your running Part 2 application and select **9–10 · Planning / MCP**.
2. Open incident **#2** — **auth-service / user-login**. In the **Process Incident** report field, paste:

    ```text
    Auth pods are OOMKilled every five minutes
    ```

3. Click **Process Incident** and wait for the planning run to finish. Reopen incident **#2** if its detail panel closes.
4. In **Planning result**, check the **Deployment evidence** card. It should show `available`, release `workshop-release-42`, and the fixture observation about a reduced memory limit.
5. Expand **Diagnosis** under **Specialist recommendations** and the rounds under **Review timeline**. Inspect how the diagnosis and plan use the deployment evidence.

This is the same operator report used in Exercise 9. Its evidence-free baseline showed **Not fetched** (`not_requested`); this run should include the MCP evidence. If you skipped that baseline, you can still complete this step: check the evidence card and recommendations described above.

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
