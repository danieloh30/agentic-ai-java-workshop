# Exercise 14 — Persistent Outcomes and Replay

<span class="badge badge--code-along">Code-Along</span> <span class="badge">Part 2 · 15 minutes</span>

**Persona:** Jordan — Java platform engineer<br>
**Continue in:** the running `part2/lab/` application, **Events / Replay** mode.<br>
**File to edit:** `src/main/java/com/incidentmanagement/messaging/ReplayPolicy.java`.

## Enterprise context and goal

Continue from the completed request in Exercise 13. Persisting duplicate outcomes safely is useful, but completed redeliveries should also avoid unnecessary model calls. Enable reuse, trigger hot reload, and replay the same request without resetting anything.

## Step 1 — Inspect the saved record (2 min)

Refresh [the dashboard](http://localhost:8080). At the top of the page, open the **Workflow** dropdown and choose **Events / Replay (Ex13–14)**. Reopen incident **#2** and inspect its persisted processing outcome. Open [#2's result](http://localhost:8080/incident-events/resolutions/2) and copy the `requestId`. Keep this exact value; clicking **Submit event** again creates a different request.

The supplied `ResolutionStore` uses one `ProcessingRequest` row per request ID. Its transaction commits before the sink returns and messaging acknowledges the outcome. The first terminal outcome is retained, and the latest-request lookup protects the UI from older results.

## Step 2 — Reuse completed work (3 min)

In `ReplayPolicy.reuseCompleted()`, change:

```java
return false;
```

to:

```java
return true;
```

Save. Leave dev mode running. The next HTTP request triggers automatic reload. The `update` schema strategy and idempotent startup seed preserve your incident and processing rows. The consumer now checks for a stored terminal outcome before invoking the agents.

## Step 3 — Replay the same request (5 min)

In another terminal, use the completed request ID copied above:

```bash
curl -sS -X POST http://localhost:8080/incident-events/replay/REQUEST_ID
```

Expect `Reusing completed request for incident #2` in the original dev-mode terminal. Reload [the result](http://localhost:8080/incident-events/resolutions/2). The request ID and recommendation are unchanged, and the database still has one row for that request. No additional model call is needed for this completed replay.

This also verifies continuity through a real source-code hot reload: the request from Exercise 13 remains available after editing the replay policy. No reset command, copied project, or restarted terminal is involved.

## Step 4 — Distinguish a replay from new work (3 min)

Submit another report through the dashboard. It receives a new request ID and runs the agents again. Replaying the old request cannot replace the latest request displayed for the incident.

Processing status and incident status are separate: `PUBLISHED` means pending work, `AWAITING_REVIEW` means a recommendation is ready for an operator, and `FAILED` means operator attention is required. None of these proves recovery.

## Step 5 — State the guarantee (2 min)

Completed replays reuse persisted outcomes, and duplicate outcome writes are idempotent. This is **not exactly-once model execution**: a crash before persistence may cause another model call. Registration and Kafka publication also have a crash gap; an outbox is a later extension. Retaining a waiting workflow across full application shutdown is a separate topic from this dev-mode continuity exercise.

## Done when

- [ ] The request from Exercise 13 survived the replay-policy hot reload.
- [ ] A completed replay reused its outcome without another model invocation.
- [ ] New work received a different request ID without overwriting the old record.
- [ ] You can distinguish duplicate-safe outcomes from exactly-once execution or workflow recovery.

This completes **75 minutes of Part 2 labs**. Stop dev mode when the session is finished. Optional challenges and automated test runs are outside the 75-minute core; `./mvnw test` in either shared project uses scripted responses rather than paid model calls.
