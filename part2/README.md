# Part 2 — Six exercises, one dev-mode session

Choose `lab/` for code-along or `solution/` for the completed run-and-read route. Keep that folder and the same Quarkus dev-mode terminal throughout Exercises 9–14.

1. Structured planning — 15 minutes.
2. Required MCP evidence — 10 minutes.
3. Memory boundaries — 10 minutes.
4. Voting and disagreement — 15 minutes.
5. Event-driven processing — 10 minutes.
6. Persistent outcomes and replay — 15 minutes.

The 75-minute core leaves 15 minutes in a 90-minute event slot. PostgreSQL, Kafka, and the bundled MCP fixture start up front. Later exercises use the existing work and data; no reset commands or project switches are part of the walkthrough.

From the repository root:

```bash
cd part2/lab
export OPENAI_API_KEY=your-key
./mvnw quarkus:dev
```

[Exercise 9 guide](../docs/09-plan-execute/START_HERE.md). Older `solutions/09-*` through `solutions/12-*` folders remain isolated reference snapshots; the current event path uses this shared project.
