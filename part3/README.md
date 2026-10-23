# Part 3 — Reliable Response and Evaluation

Six cumulative exercises, one shared Quarkus project, **75 minutes of core labs + 15 minutes of introduction, transitions, and troubleshooting**. Part 3 fits its own 90-minute event slot. Timing is a teaching budget; preflight downloads before the session.

Choose `lab/` for code-along or `solution/` for the completed run-and-read route. Keep that folder for all six exercises. Supplied infrastructure, tests, dashboard and evaluation plumbing keep each learner task focused. Part 3 uses the same Apex incident scenario but has its own database; it does not require copying state from Part 2.

| Exercise | Time | Workspace |
|----------|------|-----------|
| [**15. Runtime Skills**](../docs/15-runtime-skills/START_HERE.md) | 10 min | Skills & guardrails |
| [**16. Runtime Output Guardrails**](../docs/16-output-guardrails/START_HERE.md) | 10 min | Skills & guardrails |
| [**17. Asynchronous Approval**](../docs/17-async-approval/START_HERE.md) | 15 min | Approval & recovery |
| [**18. Restart Recovery**](../docs/18-restart-recovery/START_HERE.md) | 15 min | Approval & recovery |
| [**19. Deterministic Regression Tests**](../docs/19-regression-tests/START_HERE.md) | 10 min | Tests & evaluation |
| [**20. Quality Evaluation**](../docs/20-quality-evaluation/START_HERE.md) | 15 min | Tests & evaluation |

From the repository root:

```bash
cd part3/lab
export OPENAI_API_KEY=your-key
./dev.sh
```

Open [localhost:8080](http://localhost:8080). Use `part3/solution` instead for completed code. Java 25 and a running Docker or Podman engine are required. Use macOS/Linux or WSL on Windows for the Bash launcher. Stop the previous workshop app on port 8080 first.

The launcher provisions **PostgreSQL 17.11** with a named volume before starting `./mvnw quarkus:dev`. Lab and solution use separate containers/volumes and ports (55435 and 55436). Use only one application at a time. Keep the database during the exercise. Normal edits use hot reload; **Exercise 18 deliberately restarts the JVM once** to verify that a pending proposal survives. No resets, project copies, profile switches or per-exercise infrastructure setup are needed.

Before the event, run `./dev.sh --prepare` in your chosen folder to pre-download Maven dependencies and the PostgreSQL image. This does not need a real model API key and does not run paid calls. Maven test runs use their own disposable PostgreSQL Dev Services database, so also run `./mvnw test` once before the session. Container engines must expose their Docker-compatible API to Testcontainers (configure this normally for your Docker/Podman installation).

Tests in a second terminal in the same folder:

```bash
./mvnw test
```

All automated model responses come from a local scripted OpenAI-compatible server. Live recommendations and the dashboard evaluation use your configured paid model. Evaluation makes at least six calls per three-case run, plus skill tool turns and repairs.

## Implementation boundaries

- AI logic uses generated declarative `@Agent` interfaces; native `@Skills` supplies runbook instructions. Supplied `@SequenceAgent` entry points give each recommendation and judge call a fresh agentic scope.
- Structural guardrails allow bounded repairs; exact report excerpts are checked separately.
- A supplied database-backed state machine stores requests, proposals, hashes, attempts and audit events. Model calls do not hold database transactions open; approval is a later independent HTTP request.
- This is one local app/worker, not a Quarkus Flow checkpoint engine or an exactly-once external-action guarantee. No infrastructure remediation is executed. Authentication/authorization and multi-instance coordination are outside the core lab.
- A pending approval survives a JVM restart. An abandoned `PLANNING` attempt becomes `INTERRUPTED` after its lease and needs an explicit retry. Late results cannot complete a different attempt.
- Evaluation saves dataset and skill hashes, outputs, deterministic checks, model-judge scores/reasons, and elapsed time. It does not treat model scores as proof of safety or claim statistically significant improvement.

## Facilitator pacing

Use 5 minutes to introduce the scenario, 5 for transitions, and retain 5 for troubleshooting. Move lengthy container downloads to preflight. If a live model is slow, inspect the latest saved result and use scripted tests to show contract/recovery failures. Do not present scripted outputs as live quality measurements. Optional fault injection, larger datasets and production deployment are outside the 75-minute core.

Starter checkpoints: Ex15 enriches the runbook; Ex16 changes immediate rejection to bounded repairs; Ex17 connects the supplied worker; Ex18 completes the interrupted-attempt policy; Ex19 adds a grounding regression; Ex20 compares one runbook candidate against a fixed baseline. Later exercises reuse the same source and saved response.

[Begin Exercise 15](../docs/15-runtime-skills/START_HERE.md).

[Implementation verification notes](VERIFICATION.md).
