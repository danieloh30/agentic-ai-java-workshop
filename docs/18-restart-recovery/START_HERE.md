# Exercise 18 — Restart Recovery

<span class="badge badge--code-along">Code-Along</span> <span class="badge">Part 3 · 15 minutes</span>

**Persona:** Jordan — Java platform engineer<br>
**Continue in:** `part3/lab/` (or the completed `part3/solution/`), **Approval & recovery** workspace.<br>
**Files to edit:**

- `src/main/java/com/incidentmanagement/policy/RecoveryPolicy.java`
- `src/test/java/com/incidentmanagement/ResponseTest.java`

## Why this exercise

Jordan, the platform engineer, needs to distinguish saved results from a response workflow that can wait across application shutdown. Exercise 14 reused completed processing outcomes through hot reload; here the human decision is still pending when the JVM stops.

## Step 1 — Define interrupted work (4 min)

In `RecoveryPolicy.abandoned`, replace the starter's `false` with:

```java
return "PLANNING".equals(status) && updatedAt.isBefore(cutoff);
```

The supplied worker scans on its next tick. Only planning work older than the configured 120-second lease becomes `INTERRUPTED`; it is never silently replanned. A model invocation might have finished externally while its local result was lost, so an operator must explicitly retry it. Requests already waiting for approval keep their exact proposal.

The supplied store also checks the planning attempt number before accepting a result. A late result from a previous attempt cannot overwrite a new attempt.

## Step 2 — Restart the JVM with a pending approval (5 min)

In **Approval & recovery**, find the request left waiting in Exercise 17. Copy its ID, proposal hash and planning attempt count.

1. Stop Quarkus with **Ctrl+C** in its dev-mode terminal.
2. In that **same project folder**, start it again:

    ```bash
    ./dev.sh
    ```

3. Refresh the dashboard and select **Approval & recovery**.
4. Find the **same request ID**. Expect `WAITING_APPROVAL`, the **same hash**, and the **same attempt count**. Its audit must not show another `PLANNING` entry.

This deliberate restart is the experiment. All other exercises use normal dev-mode hot reload. Do not stop/delete the dedicated database container, remove its named volume, change folders, or reset the schema. The launcher reuses the database and idempotent seeding preserves existing incidents.

## Step 3 — Finish the saved response (3 min)

Approve or reject the recovered proposal. Refresh again to confirm the terminal decision is saved. No new model call is required for this decision.

The persisted states are the workflow's recovery boundary. They do not resume a Java stack or an in-progress network request. This makes the limitations and retry policy visible.

## Step 4 — Verify the interrupted path (3 min)

Add this test inside `ResponseTest` (the solution already contains it):

```java
@Test
void abandonedPlanningRequiresExplicitRetry() {
    var r = store.create(id(), 1L, REPORT);
    store.claim();
    store.recover(Instant.now().plusSeconds(1));
    assertEquals("INTERRUPTED", store.get(r.id).status);
    assertEquals("QUEUED", store.retry(r.id).status);
    store.claim();
    store.fail(r.id, 2);
    assertEquals(2, store.get(r.id).attempts);
}
```

Run `./mvnw -Dtest=ResponseTest#abandonedPlanningRequiresExplicitRetry test` in the second terminal. The test advances the cutoff directly instead of spending two event minutes waiting for the lease. It verifies interruption policy; the actual JVM restart above proves persistence across process shutdown.

## Done when

- [ ] A real JVM restart preserved a pending proposal, its hash, and attempt count.
- [ ] The recovered request accepted a decision without replanning.
- [ ] Abandoned planning requires an explicit retry rather than automatic duplicate work.

[Next: Exercise 19 — Deterministic Regression Tests](../19-regression-tests/START_HERE.md).
