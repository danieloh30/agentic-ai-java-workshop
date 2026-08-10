# Reference Solutions

Each folder is a **finished** Quarkus project you can run with `./mvnw quarkus:dev`.
**Only open these if you get stuck on a step** — the exercise guides link to the relevant solution at the top.

| Exercise | Solution folder | What it shows |
|----------|----------------|--------------|
| Ex 1 | [`01-first-agent/`](01-first-agent/) | `TriageAgent` + `TriageTool` |
| Ex 2 | [`02-maintenance-agent/`](02-maintenance-agent/) | `DiagnosticAgent` + `@SequenceAgent` chain |
| Ex 3 | [`03-parallel-workflow/`](03-parallel-workflow/) | `IncidentAnalysisWorkflow` + `@ParallelMapperAgent` |
| Ex 4 | [`04-supervisor/`](04-supervisor/) | Complete multi-agent supervisor pipeline |
| Ex 5 | [`05-ai-governance/`](05-ai-governance/) | `AGENTS.md` + OpenCode CLI governance |
| Ex 6 | [`06-hitl-observability/`](06-hitl-observability/) | `@HumanInTheLoop` + OTel |
| Ex 7 | [`07-a2a/`](07-a2a/) (`multi-agent-system` + `remote-a2a-agent`) | `@A2AClientAgent` |
| Ex 8 (bonus) | [`08-quarkus-flow/`](08-quarkus-flow/) | Programmatic loop with `AgenticServices.loopBuilder()` + Quarkus Flow |

### Additional reference projects

| Folder | What it shows |
|--------|--------------|
| [`05-ai-governance/weather-mcp-server`](05-ai-governance/weather-mcp-server) | MCP SSE weather server (port 8081) |
| [`06-hitl-observability/observability-reference`](06-hitl-observability/observability-reference) | Prompt injection guard + observability |
| [`08-quarkus-flow/lab/`](08-quarkus-flow/lab/) | Bonus exercise starter project (TODO stubs) |

## Versions

All projects target:
- **Java 25+** (`maven.compiler.release`)
- **Quarkus** (`quarkus.platform.version` — kept current via Dependabot)
- **Quarkus LangChain4j** (`quarkus-langchain4j.version`)
