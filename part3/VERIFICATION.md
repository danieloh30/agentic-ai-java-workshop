# Part 3 verification — 2026-10-23

- Completed application: **17 @QuarkusTest checks passed**.
- Untouched starter: **12 checks passed**.
- Cumulative starter with the guide changes and test snippets applied: **15 checks passed**.
- Automated calls used a local scripted OpenAI-compatible server and isolated PostgreSQL test databases. Tests cover the native skill tool round trip, contract repair/budget, exact evidence, independent incident histories, asynchronous submission, idempotent requests/decisions, stale hashes, interrupted attempts, late completions, and evaluation plumbing.
- Desktop (1440 px) and mobile (390 px) browser checks passed across the three workspaces. No JavaScript errors or horizontal mobile overflow. The hand-drawn workflow diagram rendered and expanded successfully.
- A real JVM stop/start retained a WAITING_APPROVAL record with identical proposal JSON, SHA-256 hash and attempt count. A decision after restart completed without another model call.
- A saved skill edit reached the model through the native tool after hot reload. Baseline/candidate runs retained the same dataset hash and rubric, with different skill hashes. This check used scripted outputs to verify the mechanism, not live model quality.
- `mkdocs build --strict`, launcher shell syntax, and Git whitespace checks passed.

The 75-minute core is a facilitator time budget, not a timed participant study. Download/container preparation belongs before the event. Live model quality comparison remains the participant experiment in Exercise 20; no paid model evaluation was used to claim an improvement during implementation.
