# Exercise 20 — Quality Evaluation

<span class="badge badge--code-along">Code-Along</span> <span class="badge">Part 3 · 15 minutes</span>

**Persona:** Sam — NOC analyst<br>
**Continue in:** `part3/lab/` (or the completed `part3/solution/`), **Tests & evaluation** workspace.<br>
**Files to edit:**

- `src/main/resources/skills/incident-response/SKILL.md`

## Enterprise context and goal

Sam, the NOC analyst, needs to decide whether a revised incident-response runbook gives operators more useful verification guidance. Contract tests catch disallowed actions and broken state transitions, but they cannot establish that the suggested checks are actionable. Sam needs to compare the current and revised skill on the same incidents before recommending adoption.

Save a baseline evaluation, make one focused skill edit, and run a candidate evaluation with the same dataset, model configuration, and judge rubric. Compare deterministic checks, individual recommendations, judge reasons, and elapsed time, then decide whether to keep or revise the change. This small comparison supports a workshop adoption decision; it is not statistical proof of improvement, and judge scores remain fallible.

## Step 1 — Read the fixed experiment (3 min)

Select **Tests & evaluation**. Inspect `src/main/resources/evaluation/cases.json` and the supplied declarative `QualityJudge`:

- Three fixed cases: checkout errors, a P1 authentication outage, and instruction injection in the report.
- Deterministic checks: response contract, exact evidence excerpts, expected action.
- Judge rubric `apex-v1`: groundedness and actionability, each scored 1–5 with a concrete reason.
- Persisted run identity: label, model, dataset hash, skill revision hash, outputs, checks, judge reasons, and total elapsed time.

An evidence excerpt match proves the quote exists in the report, not that the report itself is true or the reasoning is correct. Judge scores are a separate, fallible assessment; deterministic failures remain failures regardless of high judge scores.

## Step 2 — Save a baseline (4 min)

Keep the label **baseline**, then click **Run live evaluation · 3 cases**. This makes **at least six paid model calls**, plus skill tool turns and any guardrail repairs. Leave the page open while it runs.

Inspect every case's output and judge reason, including a low score or failed expected action. Runs are saved only when all cases and valid rubric scores complete. A model outage is an error, not a fabricated score or a passing evaluation.

## Step 3 — Make one candidate change (4 min)

In the skill file you have used since Exercise 15, add:

```markdown
Make each verification check observable: identify the signal, comparison window,
and criterion the operator should check. When the report lacks a target or window,
ask the operator to choose one instead of inventing a measured value.
For checkout errors, ask for before/after release error rates and a representative
failed request ID so the operator can correlate logs with the release timeline.
```

Save, wait about three seconds for the dev-mode change scanner, then label the next run **candidate** and run the same evaluation again. Hot reload must pick up the skill edit. Keep the **dataset, model configuration, and judge prompt/rubric unchanged**. Check that the dataset hash matches and the skill revision hash differs.

For the completed run-and-read route, this is still an experiment you perform in your chosen `solution/` folder; do not replace the whole skill with a different project snapshot.

## Step 4 — Decide from the evidence (4 min)

Compare baseline and candidate per case. Did the operator checks become more precise? Did the injected request remain constrained? Did any case lose correct escalation or exact evidence? Read the actual outputs alongside scores and elapsed time.

A three-case dataset and one judge run are a workshop-sized smoke evaluation, not statistical proof of improvement. A candidate may tie or regress; keep or revise it based on the evidence. Latency is measured for the complete run; token cost is not estimated from invented prices or fixture usage.

The supplied test `fixedDatasetEvaluationPersistsSeparateChecksAndJudgeScores` checks evaluation plumbing with scripted outputs, including a failed expected action. It does not replace the live comparison.

<div class="done-when" markdown>

## :material-check-circle: Done when

- [ ] Two completed runs share the same dataset hash, rubric and model but differ in skill hash.
- [ ] You compared deterministic failures, individual outputs, and judge reasons separately.
- [ ] You made an adoption decision without assuming the candidate must score higher.

</div>

The 75-minute core is complete. Keep the same folder and saved records for later practice. A broader dataset, repeated evaluation, independent judges, production authentication, and engine-based distributed durability are optional follow-up work outside this 90-minute event slot.
