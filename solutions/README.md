# Reference Solutions

Most folders are **finished** Quarkus projects you can run with `./mvnw quarkus:dev`.
Exercise 5 is documentation-only and keeps a README placeholder instead. **Only open solutions if you get stuck on a step** — the exercise guides link to the relevant folder at the top.

| Exercise | Solution folder | What it shows |
|----------|----------------|--------------|
| Ex 1 | [`01-first-agent/`](01-first-agent/) | `TriageAgent` + `TriageTool` |
| Ex 2 | [`02-maintenance-agent/`](02-maintenance-agent/) | `DiagnosticAgent` + `@SequenceAgent` chain |
| Ex 3 | [`03-parallel-workflow/`](03-parallel-workflow/) | `IncidentAnalysisWorkflow` + `@ParallelMapperAgent` |
| Ex 4 | [`04-supervisor/`](04-supervisor/) | Complete multi-agent supervisor pipeline |
| Ex 5 | [`05-ai-governance/`](05-ai-governance/) | Documentation-only placeholder; exercise uses `lab/AGENTS.md` + OpenCode CLI |
| Ex 6 | [`06-hitl-observability/`](06-hitl-observability/) | `@HumanInTheLoop` + OTel |
| Ex 7 | [`07-a2a/`](07-a2a/) (`multi-agent-system` + `remote-a2a-agent`) | `@A2AClientAgent` |
| Ex 8 (bonus) | [`08-quarkus-flow/`](08-quarkus-flow/) | Programmatic loop with `AgenticServices.loopBuilder()` + Quarkus Flow |

### Part 2 — Shared application for six exercises

The current event path uses [`../part2/lab/`](../part2/lab/) or [`../part2/solution/`](../part2/solution/) for **all Exercises 9–14**. Start dev mode once and keep that folder; no reset/copy/restart is part of the walkthrough. The timeboxes are 15 + 10 + 10 + 15 + 10 + 15 = 75 minutes within a 90-minute slot.

- Ex 9: structured planning.
- Ex 10: required MCP evidence.
- Ex 11: memory boundaries.
- Ex 12: voting and disagreement.
- Ex 13: event-driven processing.
- Ex 14: persistent outcomes and completed replay.

The older `09-plan-execute/`, `10-memory-tiering/`, `11-consensus-voting/`, and `12-event-driven/` folders remain isolated historical snapshots. Their numbering predates the six-exercise path. Use the shared application for the current guides.

### Additional reference project

[`06-hitl-observability/observability-reference`](06-hitl-observability/observability-reference) demonstrates a prompt injection guard and observability. [`08-quarkus-flow/lab/`](08-quarkus-flow/lab/) remains the optional Part 1 quality-loop starter.

## Versions

All projects target:
- **Java 25+** (`maven.compiler.release`)
- **Quarkus** (`quarkus.platform.version` — kept current via Dependabot)
- **Quarkus LangChain4j** (`quarkus-langchain4j.version`)

### Part 3 — Reliable Response and Evaluation

Exercises 15–20 use one shared [`part3/lab/`](../part3/lab/) starter or [`part3/solution/`](../part3/solution/) completed application. Start `./dev.sh` once and keep the same folder; Exercise 18 deliberately restarts the JVM with a retained database volume. See the [Part 3 guide](../part3/README.md) for the 75-minute core and setup.
