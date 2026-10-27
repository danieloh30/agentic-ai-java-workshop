# Exercise 12 — Voting and Disagreement

<span class="badge badge--code-along">Code-Along</span> <span class="badge">Part 2 · 15 minutes</span>

**Persona:** Priya — IT service manager<br>
**Continue in:** the running `part2/lab/` application.<br>
**Files to edit:**

- `src/main/java/com/incidentmanagement/agentic/agents/CostVoter.java`
- `src/main/java/com/incidentmanagement/agentic/workflow/ConsensusPolicy.java`

## Enterprise context and goal

Compare reliability, customer-impact, and operational-risk judgments. Three persona `@Agent` voters run through `@ParallelAgent`; a supplied `@Output` delegates to a Java policy. Require a strict majority of valid ballots, or request operator review.

## Step 1 — Select Voting (2 min)

At the top of [the dashboard](http://localhost:8080), open the **Workflow** dropdown and choose **Voting (Ex12)**. The incident action is **Run vote**, which requests specialist ballots. This workflow also offers links to fixed agreement/disagreement fixtures. The initial conservative policy requires unanimity. Earlier incident descriptions, conversations, and application state remain in the same database.

## Step 2 — Strengthen the cost perspective (5 min)

Replace the short system prompt in `CostVoter.java` with the following, keeping its existing `@Agent`, output key, and user message:

```java
@SystemMessage("""
            You are a platform engineer voting on how to remediate an incident, weighing cost
            and operational risk. You are conservative: intrusive actions (ROLLBACK, RESTART)
            and permanent spend (SCALE_UP) all carry cost and risk, so you endorse them only
            when the evidence clearly justifies it. When the impact is limited or the cause is
            still unclear, you prefer MONITOR — the cheapest safe move — over acting blindly.

            Vote for exactly ONE action: ROLLBACK, SCALE_UP, RESTART, or MONITOR.
            Give a confidence from 0 to 100 and a single-sentence rationale.
            """)
```

These are different perspectives using the same model, not independent proof of correctness. Self-reported confidence is not calibrated probability.

## Step 3 — Accept sufficient agreement (3 min)

In `ConsensusPolicy.java`, change:

```java
boolean accepted = agreement == 3;
```

to:

```java
boolean accepted = agreement >= 2;
```

Ballot validation and counting are supplied. All three voters must return an action, confidence in range, and a nonblank rationale. Missing/invalid ballots require review. Confidence cannot break a three-way split.

## Step 4 — Verify fixed outcomes (3 min)

Open the dashboard's fixture links, or use:

- [Agreement](http://localhost:8080/incident-consensus/fixtures/agreement): two ROLLBACK ballots and one MONITOR. Expect `decision=ROLLBACK`, `agreement=2`, `requiresReview=false`.
- [Disagreement](http://localhost:8080/incident-consensus/fixtures/disagreement): three different actions. Expect `decision=null` and `requiresReview=true`.

The fixtures exercise the real policy without model calls or incident writes. The first request after your edit triggers hot reload; existing data is retained.

## Step 5 — Inspect one live vote (2 min)

Open incident **#2**, submit `Login failures began immediately after a deployment`, and read the result and ballots. Actual actions can vary; use fixed fixtures to reliably demonstrate disagreement. This mode recommends an action, never executes it, and keeps the incident In Progress.

## Done when

- [ ] Two of three valid ballots can produce a recommendation.
- [ ] A three-way split requests review and preserves all rationales.
- [ ] You distinguish a deterministic policy from model-generated ballots.

**Keep the same application running.** In the dashboard’s **Workflow** dropdown, choose **Events / Replay (Ex13–14)** for [Exercise 13](../13-event-driven/START_HERE.md). No reset, build command, or new broker startup is required.
