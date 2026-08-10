# Exercise 4 — Supervisor Orchestration

<span class="badge badge--code-along">Code-Along</span>

**Timebox:** 15 minutes  
**Personas:** Priya (IT service mgr), Riley (SRE lead), Sam (NOC analyst)  
**You work in:** `lab/` (keep Quarkus running)  
**Files to edit:**

- `lab/src/main/java/com/incidentmanagement/agentic/agents/ImpactAgent.java`
- `lab/src/main/java/com/incidentmanagement/agentic/agents/EscalationAgent.java`
- `lab/src/main/java/com/incidentmanagement/agentic/agents/ResolutionAgent.java`
- `lab/src/main/java/com/incidentmanagement/agentic/agents/IncidentSupervisorAgent.java`
- `lab/src/main/java/com/incidentmanagement/agentic/workflow/IncidentProcessingWorkflow.java`

!!! tip "Solution fallback"
    [`solutions/04-supervisor`](https://github.com/danieloh30/agentic-ai-java-workshop/tree/main/solutions/04-supervisor){:target="_blank"} — open if stuck.

---

## The goal

Complete the full multi-agent pipeline. After this exercise, a single `POST /incident-management/process/{id}` triggers:

```mermaid
%%{init: {'look':'handDrawn','theme':'neutral','themeVariables': {'lineColor':'#4A4035'}}}%%
flowchart TD
    IPW(["IncidentProcessingWorkflow<br/>@SequenceAgent"])
    IAW(["IncidentAnalysisWorkflow<br/>@ParallelMapperAgent x3"])
    ISA(["IncidentSupervisorAgent<br/>@SupervisorAgent"])
    RA([ResolutionAgent])
    IA([ImpactAgent])
    EA([EscalationAgent])
    DA([DiagnosticAgent])
    TA([TriageAgent])
    IMS([IncidentManagementService])

    IPW --> IAW
    IPW --> ISA
    IPW --> RA

    IAW -->|IncidentAnalysisResults| ISA

    ISA -->|LLM decides| IA
    ISA -->|LLM decides| EA
    ISA -->|LLM decides| DA
    ISA -->|LLM decides| TA

    RA -->|IncidentOutcome| IMS

    style IPW fill:#D4E6F1,stroke:#2E6B8A
    style IAW fill:#D4E6F1,stroke:#2E6B8A
    style ISA fill:#FFE4CC,stroke:#B87333
    style RA fill:#D8F0D8,stroke:#3D7A3D
    style IA fill:#D8F0D8,stroke:#3D7A3D
    style EA fill:#D8F0D8,stroke:#3D7A3D
    style DA fill:#D8F0D8,stroke:#3D7A3D
    style TA fill:#FFF8DC,stroke:#C4A000
    style IMS fill:#E8DCC4,stroke:#6B5B45
```

!!! note "Where is `IncidentAnalysisAgent`?"
    `IncidentAnalysisWorkflow` (the "x3" box) internally fans out three parallel calls to the `IncidentAnalysisAgent` you built in Exercise 3. It's not shown as a separate node because it's encapsulated inside the workflow — the supervisor only sees the aggregated `IncidentAnalysisResults`.

---

## Step 1 — `ImpactAgent` (2 min)

Open [`ImpactAgent.java`](https://github.com/danieloh30/agentic-ai-java-workshop/blob/main/lab/src/main/java/com/incidentmanagement/agentic/agents/ImpactAgent.java){:target="_blank"}.

Replace the `// TODO` block:

```java
@SystemMessage("""
    You are a business impact assessment specialist with expertise in SLA and revenue analysis.

    Today is {current_date}. Use this to calculate SLA breach windows.

    Use these impact guidelines:

    System Criticality Tiers (hourly revenue impact):
    - Tier 1 — Revenue-critical (payment, checkout): $50,000-$100,000/hr
    - Tier 2 — Customer-facing (auth, search, email): $10,000-$50,000/hr
    - Tier 3 — Internal operations (monitoring, inventory): $1,000-$10,000/hr
    - Tier 4 — Non-critical (CDN edge, static assets): <$1,000/hr

    Priority Multipliers:
    - P1 (critical): 4x base impact
    - P2 (high): 2x base impact
    - P3 (medium): 1x base impact
    - P4 (low): 0.5x base impact

    SLA Breach Penalties:
    - P1 > 1 hour unresolved: $25,000 penalty
    - P2 > 4 hours unresolved: $10,000 penalty
    - P3 > 24 hours: $5,000 penalty

    Format your response as:
    Business Impact: HIGH/MEDIUM/LOW
    Estimated Revenue Loss: $XX,XXX/hr
    Justification: [reasoning including system tier and priority]
    """)
@UserMessage("""
    Assess the business impact of this incident:
    - System: {system}
    - Service: {service}
    - Priority: P{priority}
    - Description: {incidentDescription}
    """)
@Agent(outputKey = "businessImpact",
       description = "Impact assessment specialist that estimates business impact based on system criticality, priority, and SLA risk")
String assessImpact(String system, String service, Integer priority, String incidentDescription);
```

!!! note
    `{current_date}` is a built-in Quarkus LangChain4j template variable — it resolves to today's date automatically. This lets the agent compute SLA breach windows without hardcoding a date.

---

## Step 2 — `EscalationAgent` (2 min)

Open [`EscalationAgent.java`](https://github.com/danieloh30/agentic-ai-java-workshop/blob/main/lab/src/main/java/com/incidentmanagement/agentic/agents/EscalationAgent.java){:target="_blank"}.

Replace the `// TODO` block:

```java
@SystemMessage("""
    You are an incident escalation specialist for an IT incident management system.
    Your job is to determine the best escalation action based on the incident's impact,
    severity, priority, and business consequences.

    Escalation Options:
    - ESCALATE_P1: Critical incident requiring VP/exec attention and war room
    - ASSIGN_TEAM: Route to specific engineering team for resolution
    - WORKAROUND: Apply temporary mitigation while root cause is investigated
    - CLOSE: Incident resolved or no action needed

    Decision Criteria:
    - If estimated revenue loss > $10,000/hr: Consider ESCALATE_P1 or ASSIGN_TEAM
    - If P1 with cascading failures: ESCALATE_P1
    - If P2 with contained impact: ASSIGN_TEAM
    - If workaround available and impact is temporary: WORKAROUND
    - If false alarm or already resolved: CLOSE

    Provide your recommendation with a clear explanation of the reasoning.
    """)
@UserMessage("""
    Determine the escalation action for this incident:
    - System: {system}
    - Service: {service}
    - Priority: P{priority}
    - Incident Number: {incidentNumber}
    - Description: {incidentDescription}
    - Business Impact Assessment: {businessImpact}
    - Incident Report: {report}

    Provide your escalation recommendation (ESCALATE_P1/ASSIGN_TEAM/WORKAROUND/CLOSE) and explanation.
    """)
@Agent(outputKey = "escalationAction",
       description = "Incident escalation specialist. Determines escalation path based on impact and severity.")
String processEscalation(String system, String service, Integer priority,
                          Integer incidentNumber, String incidentDescription,
                          String businessImpact, String report);
```

!!! note
    The `{businessImpact}` placeholder means the supervisor passes the `ImpactAgent` output (e.g., `"Business Impact: HIGH\nRevenue Loss: $50,000/hr"`) directly into this `@UserMessage`. `AgenticScope` wires it — no Java code needed to thread values between agents.

---

## Step 3 — `ResolutionAgent` (2 min)

Open [`ResolutionAgent.java`](https://github.com/danieloh30/agentic-ai-java-workshop/blob/main/lab/src/main/java/com/incidentmanagement/agentic/agents/ResolutionAgent.java){:target="_blank"}.

Replace the `// TODO` block:

```java
@SystemMessage("""
    Analyze incident processing results and output a JSON summary.

    Output format:
    {
      "resolution": "concise description (max 200 chars)",
      "incidentAction": "ESCALATE|INVESTIGATE|TRIAGE|MONITOR|RESOLVE"
    }

    Rules:
    - Check the ACTUAL EscalationAgent decision in supervisorDecision, not just the analysis
    - If supervisorDecision mentions ESCALATE_P1/ASSIGN_TEAM (but NOT CLOSE) → ESCALATE
    - Else if resolutionAnalysis ≠ "ESCALATION_NOT_REQUIRED" → INVESTIGATE
    - Else if severityAnalysis ≠ "SEVERITY_LOW" → TRIAGE
    - Else if severityAnalysis = "SEVERITY_LOW" and no escalation and no triage needed → MONITOR
    - Else → RESOLVE
    - IMPORTANT: If EscalationAgent decided CLOSE, do NOT assign ESCALATE — check diagnostic/triage instead
    - resolution: Summarize the action and reason in plain language
    """)
@UserMessage("""
    Incident: P{incidentInfo.priority} {incidentInfo.system}/{incidentInfo.service} (#{incidentNumber})

    Supervisor Decision: {supervisorDecision}

    Incident Analysis Results:
    - Resolution: {incidentAnalysisResults.resolutionAnalysis}
    - Impact: {incidentAnalysisResults.impactAnalysis}
    - Severity: {incidentAnalysisResults.severityAnalysis}
    """)
@Agent(description = "Final incident resolution analyzer. Determines the incident's outcome and action based on all analysis.",
       outputKey = "incidentOutcome")
IncidentOutcome analyzeForResolution(IncidentInfo incidentInfo, Integer incidentNumber,
                                      IncidentAnalysisResults incidentAnalysisResults,
                                      String supervisorDecision);
```

??? info "Why return `IncidentOutcome` instead of `String`?"
    Quarkus LangChain4j deserializes the LLM's JSON output directly into the `IncidentOutcome` record. The `@SystemMessage` instructs the LLM to output well-formed JSON; the framework handles parsing. If parsing fails, an exception is thrown at runtime — which is far better than silently accepting a malformed string downstream.

---

## Step 4 — `IncidentSupervisorAgent` (4 min)

Open [`IncidentSupervisorAgent.java`](https://github.com/danieloh30/agentic-ai-java-workshop/blob/main/lab/src/main/java/com/incidentmanagement/agentic/agents/IncidentSupervisorAgent.java){:target="_blank"}.

This is the most important agent in the system. It has **two annotated members**.

**4a — `@SupervisorAgent` method** (replace the first `// TODO`):

```java
@SupervisorAgent(
        outputKey = "supervisorDecision",
        subAgents = {
                ImpactAgent.class,
                EscalationAgent.class,
                DiagnosticAgent.class,
                TriageAgent.class
        })
String superviseIncidentProcessing(IncidentInfo incidentInfo, Integer incidentNumber,
                                    IncidentAnalysisResults incidentAnalysisResults);
```

**4b — `@SupervisorRequest` static method** (replace the second `// TODO`):

```java
@SupervisorRequest
static String request(IncidentInfo incidentInfo, Integer incidentNumber,
                      IncidentAnalysisResults incidentAnalysisResults) {

    boolean escalationRequired = incidentAnalysisResults.resolutionAnalysis() != null &&
            incidentAnalysisResults.resolutionAnalysis().toUpperCase().contains("ESCALATION_REQUIRED");

    String noEscalationMessage = """
            No escalation has been requested.

            INSTRUCTIONS:
            - DO NOT invoke ImpactAgent
            - DO NOT invoke EscalationAgent
            - Only invoke DiagnosticAgent if root cause analysis needed
            - Only invoke TriageAgent if re-triage needed
            """;

    String escalationMessage = """
            The incident requires escalation.

            STEP 1: Invoke ImpactAgent to assess business impact
            STEP 2: Invoke EscalationAgent to decide escalation action (ESCALATE_P1/ASSIGN_TEAM/WORKAROUND/CLOSE)
            STEP 3: If EscalationAgent decides CLOSE:
                    - Invoke DiagnosticAgent if root cause analysis needed
                    - Invoke TriageAgent if re-triage needed

            IMPORTANT: When invoking EscalationAgent:
            - Pass businessImpact as a STRING with the full assessment
            - Use the EXACT format from ImpactAgent's response

            Follow the decision logic in your system message carefully.
            """;

    return """
            You are an incident supervisor for an IT incident management system. You coordinate action agents based on incident analysis.

            The incident has already been analyzed and you have these inputs:
            - severityAnalysis: Severity classification (or "SEVERITY_LOW")
            - impactAnalysis: Business impact assessment (or "IMPACT_MINIMAL")
            - resolutionAnalysis: Whether critical issues require escalation (or "ESCALATION_NOT_REQUIRED")

            Your job is to invoke the appropriate ACTION agents for this incident.

            Incident: P""" + incidentInfo.priority + " " + incidentInfo.system + "/" + incidentInfo.service
            + " (#" + incidentNumber + ")\n"
            + "Current Description: " + incidentInfo.description + "\n\n"
            + "Severity Analysis: " + incidentAnalysisResults.severityAnalysis() + "\n"
            + "Impact Analysis: " + incidentAnalysisResults.impactAnalysis() + "\n\n"
            + "In particular, you have to follow these steps:\n\n"
            + (escalationRequired ? escalationMessage : noEscalationMessage);
}
```

??? info "Why policy lives in `@SupervisorRequest`"
    The boolean `escalationRequired` is the only Java logic here — it checks whether the earlier `IncidentAnalysisWorkflow` step flagged an escalation case. Everything else is natural-language instructions. To change the supervisor's behavior (add a new sub-agent, change escalation rules), you edit this string. No `if/else` chains, no enum routing tables.

??? info "Why declare sub-agents in `@SupervisorAgent` but not call them explicitly?"
    The LLM reads the `@SupervisorRequest` prompt and decides which sub-agents to call, in what order, with what parameters. `subAgents` is the capability declaration — it tells the framework what tools to expose. The LLM decides the invocation strategy.

---

## Step 5 — `IncidentProcessingWorkflow` (2 min)

Open [`IncidentProcessingWorkflow.java`](https://github.com/danieloh30/agentic-ai-java-workshop/blob/main/lab/src/main/java/com/incidentmanagement/agentic/workflow/IncidentProcessingWorkflow.java){:target="_blank"}.

First, add `extends MonitoredAgent` to the interface declaration — this enables the **topology view** in the Dev UI. The framework registers any `MonitoredAgent` workflow as a root agent, allowing it to visualize the full agent tree:

```java
public interface IncidentProcessingWorkflow extends MonitoredAgent {
```

Add the import:

```java
import dev.langchain4j.agentic.observability.MonitoredAgent;
```

The method signature is already declared. Add the `@SequenceAgent` annotation above it:

**5a — `@SequenceAgent` annotation:**

```java
@SequenceAgent(outputKey = "incidentProcessingAgentResult",
        subAgents = { IncidentAnalysisWorkflow.class,
                      IncidentSupervisorAgent.class,
                      ResolutionAgent.class })
```

**5b — `@Output` static method:**

```java
@Output
static IncidentOutcome output(IncidentOutcome incidentOutcome) {
    Log.debug("IncidentOutcome: " + incidentOutcome.resolution()
              + " → " + incidentOutcome.incidentAction());
    return incidentOutcome;
}
```

Save. Quarkus hot-reloads.

---

## Step 6 — Test all three paths (3 min)

!!! tip "Agentic Dev UI"
    Before testing, open the [topology view](http://localhost:8080/q/dev-ui/quarkus-langchain4j-agentic/topology){:target="_blank"} to see the full pipeline: `IncidentProcessingWorkflow` → `IncidentAnalysisWorkflow` + `IncidentSupervisorAgent` (with its 4 sub-agents) + `ResolutionAgent`. This is the first exercise where a **root agent** appears — the topology visualizes `IncidentProcessingWorkflow` as the entry point with all sub-agents branching out beneath it. You'll see this topology grow as you add HITL (Exercise 6) and A2A (Exercise 7).

    You can also check the [Agents tab](http://localhost:8080/q/dev-ui/quarkus-langchain4j-agentic/agents){:target="_blank"} for each agent's type, `outputKey`, and description — a quick sanity check that everything wired correctly after hot reload.

Open **[http://localhost:8080](http://localhost:8080){:target="_blank"}** to test all three paths. After each, check the **Quarkus terminal logs** for tool calls and the **UI** for status changes.

**Path 1 — Minor incident:**  
Click **View** on Incident **#6** (search-engine/product-search) and process with:

```
False alarm, relevance restored after cache refresh
```

**How to confirm:** UI status stays `OPEN` or changes to `RESOLVED`. Terminal logs show `Action: MONITOR` or `RESOLVE`. (LLM responses are non-deterministic — the exact action may vary.)

Now press `s` to restart (reset the database), then **reload the browser**.

**Path 2 — Needs triage:**  
Click **View** on Incident **#5** (email-service/notification-api) and process with:

```
SMTP timeout for 30% of outbound emails, queue growing
```

**How to confirm:** UI status changes to `TRIAGING` or `IN_PROGRESS`. Terminal logs show `TriageTool activated for incident #5` — the supervisor delegated to `TriageAgent`.

Now press `s` to restart (reset the database), then **reload the browser**.

**Path 3 — Critical incident (full supervisor path):**  
Click **View** on Incident **#1** (payment-gateway/checkout-api, P2) and process with:

```
Complete checkout failure; all payment processing down; customers seeing 500 errors; revenue loss estimated at $50k/hr
```

**How to confirm:** UI status changes to `ESCALATED`. Terminal logs show `Action: ESCALATE`, with `ImpactAgent` and `EscalationAgent` invoked by the supervisor.

---

## Supervisor vs conditional routing

| | `@ConditionalAgent` (not used in this lab) | `@SupervisorAgent` |
|---|---|---|
| **Decision logic** | Hardcoded Java predicates | LLM reasoning on natural-language prompt |
| **Policy change** | Code change + test + redeploy | Edit `@SupervisorRequest` string + hot reload |
| **Sub-agent selection** | Compile-time routing table | Runtime multi-factor reasoning |
| **When to use** | Stable, binary, well-defined rules | Multi-factor, evolving, nuanced policy |

---

<div class="done-when" markdown>

## :material-check-circle: Done when

- [ ] Full pipeline runs end-to-end: `POST /incident-management/process/{id}` produces correct `IncidentStatus`
- [ ] Supervisor chose `ESCALATE` with `ImpactAgent` + `EscalationAgent` for critical incident (Path 3)
- [ ] Supervisor invoked `TriageAgent` for email issues — `TriageTool activated` in logs (Path 2)
- [ ] You can draw the full agent chain from ProcessingWorkflow → IncidentStatus from memory
- [ ] You can explain why policy lives in `@SupervisorRequest` and not in Java `if/else`

</div>

!!! note
    **Keep Quarkus running** — Exercise 5 uses OpenCode CLI with your current `lab/` project.
