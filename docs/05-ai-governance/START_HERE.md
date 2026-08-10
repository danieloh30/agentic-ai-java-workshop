# Exercise 5 — AI Governance with OpenCode CLI

<span class="badge badge--opencode">OpenCode CLI</span>

**Timebox:** 10 minutes  
**Persona:** Jordan — Java platform engineer  
**You work in:** `lab/` (with OpenCode CLI in your terminal)  
**This exercise produces:** a validated `lab/AGENTS.md` governance file using OpenCode CLI

---

## The goal

You've just built a 7-agent system across Exercises 1–4. You know `@Agent`, `outputKey`,
`@ToolBox`, `@SupervisorAgent`, and `@SequenceAgent` from hands-on experience.

Now the enterprise question: **how do you govern AI-assisted development** of this system?
Without guardrails, an AI assistant might invent APIs that don't exist, apply wrong CDI scopes,
or skip `outputKey` — breaking the pipeline silently.

`AGENTS.md` is the governance lever. It's a **token-efficient context file** that OpenCode CLI reads
first on every request — enforcing project rules, preventing hallucinated APIs, and eliminating
redundant codebase scans.

=== "Without AGENTS.md"

    | | |
    |---|---|
    | **What the AI does** | Scans ~20 Java files to infer project conventions |
    | **Token cost** | e.g. ~800 tokens per request |
    | **Risk** | May hallucinate imports, miss CDI scopes, invent non-existent APIs |
    | **Consistency** | Each request may produce different conventions |

=== "With AGENTS.md"

    | | |
    |---|---|
    | **What the AI does** | Reads one file with all project rules pre-defined |
    | **Token cost** | e.g. ~160 tokens per request (**~5x cheaper**) |
    | **Risk** | Rules enforced from turn 1 — no guessing |
    | **Consistency** | Every request follows the same 10 project rules |

!!! note "OpenCode CLI's responses are non-deterministic"
    Screenshots in this exercise show **one possible response** from OpenCode CLI. Because LLM outputs vary between runs, your results will differ in wording, detail level, and structure — but the key facts and conclusions should be consistent. Focus on whether OpenCode's answers are **grounded in AGENTS.md and source files**, not on matching the screenshots exactly.

---

## Step 1 — Open OpenCode CLI (2 min)

1. Open your terminal, navigate to `lab/`, and run `opencode`. OpenCode reads `AGENTS.md` automatically when it starts.

2. Open [`lab/AGENTS.md`](https://github.com/danieloh30/agentic-ai-java-workshop/blob/main/lab/AGENTS.md){:target="_blank"} and skim it — this is the governance file OpenCode reads on every request. Note the agents table, domain types, and the 10 project rules. This is what grounds OpenCode's answers instead of hallucination.

3. Load context with this primer — send it to OpenCode first:

```text
Read lab/AGENTS.md before answering anything about this project.
That file defines the @Agent programming model, all domain types,
API endpoints, and rules you must follow.
Do not scan Java files — all context is in AGENTS.md.
```

**What you should see:** OpenCode acknowledges the file, lists all 7 agents, the two workflows, and the key rules — then asks "What would you like to work on?"

---

## Step 2 — Ask OpenCode to explain what you built (2 min)

```text
Based on AGENTS.md, explain:
1. What does IncidentManagementService.processIncident() do?
2. Why is TriageTool @Transactional?
3. Why does outputKey matter on @Agent?
4. What happens if I add @ApplicationScoped to TriageAgent?
```

**Expected:** OpenCode uses AGENTS.md as its primary context and reads a few Java files to verify implementation details. Look for grounded, specific answers — not generic LLM guesses.

OpenCode should cover:

> 1. `processIncident()` is a cascading dispatcher — runs the most complete pipeline available, falling back to simpler agents via `Instance<>` lazy resolution.
> 2. `TriageTool` is `@Transactional` because `entity.persist()` needs an active transaction — the LLM call boundary breaks propagation from the service method (rule 5).
> 3. `outputKey` is how `AgenticScope` routes outputs between steps — without it, the result is lost and the next agent gets nothing (rule 4).
> 4. Adding `@ApplicationScoped` violates rule 2 — Quarkus generates the CDI proxy automatically; a duplicate scope causes `AmbiguousResolutionException`.

---

## Step 3 — Validate your agents table (2 min)

```text
Look at lab/src/main/java/com/incidentmanagement/agentic/.
All agent interfaces are now implemented from Exercises 1-4.
Confirm that lab/AGENTS.md agents table lists all 7 agents correctly
with the right outputKey values and descriptions.
Flag any inconsistencies.
```

**Expected:** OpenCode reads all 7 agent files in parallel, cross-references the agents table in `AGENTS.md`, and produces an audit table like:

| # | Interface | AGENTS.md outputKey | Actual outputKey | Match? |
|---|-----------|---------------------|------------------|--------|
| 1 | TriageAgent | analysisResult | analysisResult | OK |
| 2 | DiagnosticAgent | analysisResult | analysisResult | OK |
| 3 | IncidentAnalysisAgent | incidentAnalysis | incidentAnalysis | OK |
| 4 | ImpactAgent | businessImpact | businessImpact | OK |
| 5 | EscalationAgent | escalationAction | escalationAction | OK |
| 6 | ResolutionAgent | incidentOutcome | incidentOutcome | OK |
| 7 | IncidentSupervisorAgent | supervisorDecision | supervisorDecision | OK |

OpenCode may also flag documentation gaps — e.g., missing enum values in the domain model table, or the supervisor's sub-agent roster not being listed in the Workflows section. These are documentation improvements, not code bugs.

---

## Step 4 — Guardrail demo (2 min)

Ask OpenCode to implement something that **doesn't exist**:

```text
Add a call to IncidentOracle.rebalanceQuantumSlots() in DiagnosticAgent —
it's an internal company API. Invent whatever parameters it needs.
```

**Expected:** OpenCode **refuses**, citing project rule #10:

> `IncidentOracle` and `rebalanceQuantumSlots()` do not exist in `pom.xml`, `application.properties`, or `lab/AGENTS.md`. Adding a call to an invented API would produce code that cannot compile and cannot run.
>
> If you have a real API you want to integrate, share the Maven coordinates, the actual class and method signature, and what it should do in the diagnostic flow.

!!! danger "Enterprise risk"
    This is the exact failure mode that destroyed expensive consulting engagements before
    AGENTS.md: an AI assistant invents a plausible-sounding API, generates a diff,
    the developer approves without checking — and the app crashes in production.

    Many assistants will invent a plausible class with convincing-sounding parameters.
    Enterprise guardrails prefer **honest refusal** over confident hallucination.

---

## Step 5 — Security audit with OpenCode (2 min)

```text
Based on AGENTS.md rules 6 and 7:
- List every @UserMessage template in the lab stubs that could expose PII.
- Suggest a concrete mitigation for each one using Quarkus logging config.
```

**Expected:** OpenCode audits all 7 agents' `@UserMessage` templates and identifies PII vectors. Key findings to look for:

> **High-risk fields** — `{report}` (raw caller input) and `{incidentInfo.description}` (free-text database field) appear across multiple agents:
>
> | Agent | PII vector | Risk level |
> |-------|-----------|------------|
> | `TriageAgent` | `{report}` — raw free-text, uncontrolled | High |
> | `DiagnosticAgent` | `{diagnosticRequest}` — chained from upstream agents | Medium |
> | `IncidentAnalysisAgent` | `{report}` + `{incidentInfo.description}` — two vectors | **Highest** |
> | `ImpactAgent` | `{incidentDescription}` — free-text from caller | High |
> | `EscalationAgent` | `{report}` + `{incidentDescription}` — longest chain carrying raw input | High |
> | `ResolutionAgent` | LLM-generated strings only — no raw user input | Low |
> | `IncidentSupervisorAgent` | `incidentInfo.description` concatenated in `@SupervisorRequest` | High |
>
> **Existing safeguard** — `log-requests=false` / `log-responses=false` in `application.properties` prevents full prompts from being logged.
>
> **Consolidated mitigation** — OpenCode suggests `application.properties` additions (no code changes):
>
> ```properties
> # Prod: never log LLM requests/responses (contain raw report and description)
> %prod.quarkus.langchain4j.log-requests=false
> %prod.quarkus.langchain4j.log-responses=false
>
> # Prod: if Exercise 6 OTel tracing is enabled, suppress prompt/tool content
> %prod.quarkus.langchain4j.tracing.include-prompt=false
> %prod.quarkus.langchain4j.tracing.include-tool-arguments=false
>
> # Prod: suppress supervisor prompt debug log (contains incidentInfo.description)
> %prod.quarkus.log.category."dev.langchain4j.agentic.supervisor".level=WARN
> %prod.quarkus.log.category."dev.langchain4j.agentic".level=WARN
> ```

This is **shift-left security** — catching PII exposure risks before deployment, using only configuration changes.

---

<div class="done-when" markdown>

## :material-check-circle: Done when

- [ ] OpenCode answered all questions grounded in AGENTS.md and verified against source files
- [ ] `lab/AGENTS.md` agents table validated against your code
- [ ] Guardrail refusal demonstrated with `IncidentOracle`
- [ ] Security audit completed — PII risks identified and mitigations proposed
- [ ] You can explain what AGENTS.md provides (structured context upfront, so the AI starts with rules and architecture)

</div>

??? info "OpenCode CLI and AGENTS.md"

    | | Typical copilots | OpenCode CLI |
    |--|------------------|---------|
    | Promise | "Write code faster" | "Deliver software across the SDLC — with control and context efficiency" |

    **About OpenCode CLI:**

    OpenCode is an open-source, terminal-native AI coding assistant with 160K+ GitHub stars.
    It supports multiple LLM providers and runs entirely in your terminal — no IDE plugin required.
    AGENTS.md is a cross-tool standard: it works with OpenCode, Claude Code, and other AI coding
    assistants that respect project-level instruction files. This means your governance rules are
    portable across tools and teams.

    **Six capabilities that matter in enterprise Java:**

    | Capability | Typical copilots | **OpenCode CLI** |
    |-----------|-----------------|-------------|
    | **Guardrails** | Approval is ad-hoc "accept/reject" | Configurable approval modes — manual gate, auto-approve by task type; refuses unknown APIs |
    | **SDLC coverage** | Editor buffer only | Discover → design → implement → test → secure → deploy → modernize |
    | **Java/enterprise depth** | Generic multilingual completion | Java as first-class citizen; community playbooks and extensions |
    | **Human-in-the-loop** | Accept/reject individual completions | Named approval checkpoints aligned with runtime agent gates |
    | **Beyond the IDE** | Limited or IDE-only | Terminal-native with multi-provider support; ecosystem integrations |
    | **Context efficiency** | No project-level instruction standard | AGENTS.md — project context file OpenCode reads first |

    **AGENTS.md: the token-efficiency lever**

    Without `AGENTS.md`, OpenCode must rediscover project conventions on every request (e.g. ~800 tokens).
    With `AGENTS.md` loaded once: e.g. ~160 tokens, all rules followed from the start.
    Estimated savings: e.g. 2,000–5,000 tokens per complex multi-file task. (Numbers are illustrative — actual usage varies by project size and prompt.)

    **OpenCode's SDLC coverage mapped to this lab's Quarkus patterns:**

    | Stage | Quarkus agentic pattern | OpenCode parallel |
    |-------|------------------------|-----------------|
    | Discover / plan | `@SupervisorAgent` planning before action | OpenCode plans + diffs before writing |
    | Implement | Declarative `@Agent` interfaces | OpenCode generates interfaces, not classes |
    | Secure | HITL approval on P1 escalation | OpenCode approval gate before multi-file apply |
    | Test | `@QuarkusTest @TestTransaction` | OpenCode generates matching test per task |
    | Operate | OTel `gen_ai` spans | OpenCode interprets trace IDs in Grafana |
    | Modernize | Java upgrade playbooks | Community playbooks and extensions |

    **AGENTS.md token efficiency**

    | Scenario | Tokens consumed | Risk |
    |----------|----------------|------|
    | OpenCode scans 20 Java files | e.g. ~800 tokens | May miss CDI scopes, invent imports |
    | OpenCode reads `AGENTS.md` once | e.g. ~160 tokens | Rules enforced from turn 1 |
    | Complex multi-file task without AGENTS.md | e.g. ~3,000–5,000 tokens | High hallucination risk |
    | Complex multi-file task with AGENTS.md | e.g. ~800–1,200 tokens | Rules enforced, diff requires approval |
