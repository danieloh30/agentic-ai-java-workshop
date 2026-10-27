# Exercise 11 — Memory Boundaries

<span class="badge badge--code-along">Code-Along</span> <span class="badge">Part 2 · 10 minutes</span>

**Persona:** Sam — NOC analyst<br>
**Continue in:** the running `part2/lab/` application.<br>
**File to edit:** `src/main/java/com/incidentmanagement/agentic/agents/IncidentAssistant.java`.

## Enterprise context and goal

Sam needs follow-up recall without mixing incident conversations or sending an unlimited transcript to the model. Compare a bounded working window with its PostgreSQL backing store. **The store persists the retained window; evicted messages leave the database too.**

## Step 1 — Switch capability, not project (1 min)

At the top of [the dashboard](http://localhost:8080), open the **Workflow** dropdown and choose **Memory (Ex11)**. Keep Quarkus running and keep the planner/MCP edits you already made. This mode sends your messages directly to the incident assistant; it does not replay saved planning descriptions into the conversation.

## Step 2 — Inspect and shrink the window (2 min)

In `IncidentAssistant.java`, find `@MemoryId`, which selects the incident conversation, and the static `@ChatMemoryProviderSupplier`, which builds its memory over the supplied transactional PostgreSQL store.

Change:

```java
.maxMessages(6)
```

to:

```java
.maxMessages(3)
```

Save before starting the conversation. The system message remains, leaving room for one user/AI pair. Messages are not turns, and a message cap is not a token cap.

## Step 3 — Observe recall and eviction (5 min)

1. Open incident **#2**. Copy this text into the message field and click **Send message**. Wait for the reply before continuing.

    ```text
    The pods are OOMKilled every five minutes. Acknowledge briefly.
    ```

2. In the same incident, send this follow-up using **Send message** and wait for the reply:

    ```text
    I am checking the dashboard now. Acknowledge briefly.
    ```

3. Inspect [#2’s history](http://localhost:8080/incident-assistant/2/history) and the `chatmemoryentity` row in the [Dev UI database view](http://localhost:8080/q/dev-ui/quarkus-agroal/database-view). The original user message has left the three-message window and the database.
4. Still in incident **#2**, send:

    ```text
    What symptom did I first report?
    ```

    A reply might still repeat a detail from a retained assistant message. Judge eviction from stored messages, not model wording alone.

5. Open incident **#4**, paste the following text, and click **Send message**:

    ```text
    What symptom did I report earlier in this conversation?
    ```

    Its stored conversation must not contain #2’s messages. Each incident ID has its own `@MemoryId`.

## Step 4 — Explain what survives (2 min)

Change the cap back to six if desired. Increasing it does not recover evicted messages. The database retains the current window across the subsequent hot reloads in this session. The UI only caches the latest displayed reply in browser memory; use the history endpoint to inspect the server's retained messages after refresh.

This setup demonstrates continuity during dev-mode reloads. Recovery after a full application stop additionally needs a retained database and is a later topic.

## Done when

- [ ] You inspected a message that was evicted from both working memory and storage.
- [ ] You verified separate conversations for #2 and #4.
- [ ] You can explain why increasing the window is not historical retrieval.

**Keep Quarkus running.** In the dashboard’s **Workflow** dropdown, choose **Voting (Ex12)** for [Exercise 12](../12-consensus-voting/START_HERE.md). No reset or project switch is needed.
