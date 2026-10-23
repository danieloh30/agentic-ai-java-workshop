# Part 2 — Completed solution

Use this single project for **all six exercises (9–14)**. Start `./mvnw quarkus:dev` once with `OPENAI_API_KEY` set and Docker or Podman running. Choose Planning / MCP, Memory, Voting, or Events / Replay in the dashboard at http://localhost:8080.

All exercise changes are already complete; use this folder for the run-and-read route or compare individual files with the starter.

Do not copy the whole solution, reset files, switch folders, change Git branches, or restart dev mode between exercises. Dev-mode hot reload preserves the existing PostgreSQL records through an update schema strategy and an idempotent startup seed. The bundled MCP fixture and Kafka Dev Services start with this application.

**Timeboxes:** 15 + 10 + 10 + 15 + 10 + 15 = 75 minutes, plus 15 minutes of session overhead.

[Start with Exercise 9](https://danieloh30.github.io/agentic-ai-java-workshop/09-plan-execute/START_HERE/). Run `./mvnw test` outside the core timebox to use PostgreSQL/Kafka and scripted model replies without paid model calls. Keeping data through a full stop/start additionally requires retained database/broker configuration; this is separate from hot reload.
