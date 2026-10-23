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

### Part 2 — From Patterns to Production

**90-minute event slot:** 20 + 15 + 20 + 20 minutes of labs, plus 15 minutes for introduction, transitions, and troubleshooting. Part 2 starters are runnable; each guide limits required edits and supplies infrastructure.

| Exercise | Solution folder | What it shows |
|----------|----------------|--------------|
| Ex 9 | [`09-plan-execute/`](09-plan-execute/) | Required MCP evidence precedes a validated `ExecutionPlan`, bounded re-planning, and recommendation review |
| Ex 10 | [`10-memory-tiering/`](10-memory-tiering/) | A conversational `@Agent` with two-tier memory — a `MessageWindowChatMemory` (working memory) over a durable `ChatMemoryStore` backed by PostgreSQL, wired via `@ChatMemoryProviderSupplier` |
| Ex 11 | [`11-consensus-voting/`](11-consensus-voting/) | Three persona voter `@Agent`s run in parallel via `@ParallelAgent`, each returning a structured `Vote`; an `@Output` method applies validated majority-or-review policy |
| Ex 12 | [`12-event-driven/`](12-event-driven/) | Capstone — a `@SequenceAgent` triage→resolve pipeline wired between Kafka topics (`@Incoming`/`@Outgoing`, Kafka via Dev Services); correlated recommendations persisted by a downstream sink, with completed replay |

### Additional reference projects

| Folder | What it shows |
|--------|--------------|
| [`06-hitl-observability/observability-reference`](06-hitl-observability/observability-reference) | Prompt injection guard + observability |
| [`08-quarkus-flow/lab/`](08-quarkus-flow/lab/) | Bonus exercise runnable starter project (focused TODOs) |
| [`09-plan-execute/lab/`](09-plan-execute/lab/) | Exercise 9 runnable starter project (focused TODOs) |
| [`10-memory-tiering/lab/`](10-memory-tiering/lab/) | Exercise 10 runnable starter project (focused TODOs) |
| [`11-consensus-voting/lab/`](11-consensus-voting/lab/) | Exercise 11 runnable starter project (focused TODOs) |
| [`12-event-driven/lab/`](12-event-driven/lab/) | Exercise 12 runnable starter project (focused TODOs) |

## Versions

All projects target:
- **Java 25+** (`maven.compiler.release`)
- **Quarkus** (`quarkus.platform.version` — kept current via Dependabot)
- **Quarkus LangChain4j** (`quarkus-langchain4j.version`)
