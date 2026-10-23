# Exercise 11 — Consensus, Voting, and Disagreement

<span class="badge badge--code-along">Code-Along</span> <span class="badge">Part 2 · 20 minutes</span>

**Persona:** Priya — IT service manager<br>
**You work in:** `solutions/11-consensus-voting/lab/`<br>
**Files to edit:** `agentic/agents/CostVoter.java` and `agentic/workflow/ConsensusPolicy.java`, under `src/main/java/com/incidentmanagement/`.

## Enterprise context and goal

Priya needs to compare reliability, customer-impact, and operational-risk judgments before choosing a response. Three fluent answers can disagree; a high confidence score is not an extra vote.

Run three persona `@Agent` voters with `@ParallelAgent`. The supplied `@Output` reads their `Vote` records and delegates to deterministic Java. Implement the acceptance boundary: **at least two of three valid ballots must agree; otherwise request operator review**. The workflow recommends work and never executes remediation.

## Step 0 — Start green (2 min)

Stop Exercise 10. From the repository root:

```bash
cd solutions/11-consensus-voting/lab
export OPENAI_API_KEY=your-key
./mvnw quarkus:dev
```

Open [the dashboard](http://localhost:8080). The starter already builds. Its conservative default requires unanimity; you will change that to a strict majority. All three agent interfaces, their unique output keys, the parallel workflow, and ballot validation are supplied.

## Step 1 — Strengthen the third perspective (6 min)

Open `CostVoter.java`. Keep `@Agent`, its `outputKey`, and `@UserMessage`. Replace its short system prompt with:

```java
@SystemMessage("""
        You assess IT incident remediation from a cost and operational-risk perspective.
        Choose one action: ROLLBACK, SCALE_UP, RESTART, or MONITOR.
        Consider disruption, capacity cost, rollback compatibility, and risk to recent data.
        Prefer the least disruptive action consistent with the reported impact.
        Do not invent metrics, deployments, transaction loss, or actions already performed.
        Return an action, confidence from 0 to 100, and a one-line rationale.
        """)
```

Compare it with `ReliabilityVoter` and `CustomerImpactVoter`. They use the same model with different prompts. This provides different perspectives, **not independent proof of correctness**. Confidence is model-reported, not calibrated probability.

## Step 2 — Define when agreement is sufficient (5 min)

Open `ConsensusPolicy.java`. The supplied code checks that all three ballots have an action, confidence in range, and a nonblank rationale. Missing or invalid ballots return `requiresReview=true`.

Replace the conservative default:

```java
boolean accepted = agreement == 3;
```

with:

```java
boolean accepted = agreement >= 2;
```

The rest is supplied: count votes, preserve the ballots, return a majority recommendation or a null decision with a review reason. When three agents choose three different actions, even a confidence of 99 cannot manufacture a majority.

## Step 3 — Verify predictable agreement and disagreement (4 min)

Open these fixtures in browser tabs:

- [Agreement fixture](http://localhost:8080/incident-consensus/fixtures/agreement): two ROLLBACK ballots and one MONITOR ballot. Expect `decision=ROLLBACK`, `agreement=2`, `requiresReview=false`.
- [Disagreement fixture](http://localhost:8080/incident-consensus/fixtures/disagreement): ROLLBACK, SCALE_UP, and MONITOR. Expect `decision=null`, `requiresReview=true`, and the review reason.

These workshop-only endpoints invoke the same Java policy with fixed ballots. They make no LLM calls and do not alter incidents. A failure after editing points to the policy rather than model variation.

## Step 4 — Compare with real voters (3 min)

Open incident **#2**, enter `Login failures began immediately after the latest deployment`, and click **Process Incident**. Read the result and all ballots. The exact actions can vary; use the fixtures to demonstrate disagreement reliably.

The incident remains **In Progress**. The summary shows either a recommendation or `NEEDS_OPERATOR_REVIEW`. Reopening the panel preserves the result while the page stays open; a browser refresh clears the displayed result. Structured results are also available through `POST /incident-consensus/2`, which starts a new vote rather than retrieving the UI run.

## Done when

- [ ] The agreement fixture accepts two of three ballots.
- [ ] The disagreement fixture requests review and preserves all three rationales.
- [ ] You can explain why confidence does not break a three-way split.
- [ ] You inspected one live vote without confusing consensus with verified correctness.

## Supplied verification and optional challenges

`./mvnw test` covers missing/invalid ballots, fixed disagreement, and the declarative parallel workflow using scripted model replies. The completed solution additionally checks the two-of-three acceptance boundary. PostgreSQL Dev Services is required; no paid model calls are made.

Outside the core timebox: add a bounded revision loop, compare models, or persist the ballot history. Those should not expand this 20-minute lab.

**Full solution:** copy the two edited files from the parent project. Stop this application before [Exercise 12](../12-event-driven/START_HERE.md).
